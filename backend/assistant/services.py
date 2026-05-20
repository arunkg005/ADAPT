import json
import os
import re
from datetime import datetime

import google.generativeai as genai

from tasks.models import CareItem

PLACEHOLDER_KEY = "replace-with-your-gemini-api-key"
ACTION_PATTERN = re.compile(r"<ACTION_JSON>(.*?)</ACTION_JSON>", re.DOTALL)


def _query_is_patient_specific(text: str) -> bool:
    text = (text or "").lower()
    keywords = [
        "patient",
        "medication",
        "allergy",
        "disease",
        "diagnosis",
        "bp",
        "sugar",
        "diabetes",
        "prescription",
        "routine",
        "schedule",
        "task",
    ]
    return any(word in text for word in keywords)


def _build_context_block(session, selected_patient, previous_summaries):
    patient_block = "No patient selected."
    if selected_patient:
        patient_block = (
            f"Patient: {selected_patient.name}\n"
            f"Summary: {selected_patient.ai_summary or 'No summary yet'}\n"
            f"Allergies: {selected_patient.allergies}\n"
            f"Disease history: {', '.join(selected_patient.history_diseases)}\n"
        )

    prev_summary_block = "\n".join([f"- {s}" for s in previous_summaries]) or "No previous summaries."

    history = []
    for msg in session.messages.all().order_by("created_at")[:30]:
        history.append(f"{msg.role.upper()}: {msg.content}")

    return (
        "SYSTEM INSTRUCTIONS:\n"
        "- You are ADAPT caregiver assistant.\n"
        "- If no patient selected and query is patient-specific, answer generally and add a suggestion to select a patient.\n"
        "- If user asks to create/update/delete task/routine/schedule, include one action proposal in this exact tag format:\n"
        "<ACTION_JSON>{...valid json...}</ACTION_JSON>\n"
        "- Action JSON schema keys: operation(create|update|delete), item_type(task|routine|schedule), title, description, notes, status, priority, due_at, recurrence_mode, recurrence_weekdays, recurrence_dates, reminder_enabled, reminder_minutes_before, item_id(optional).\n"
        "- Do not execute actions yourself; only propose.\n\n"
        f"CURRENT SESSION SOURCE: {session.source}\n"
        f"PATIENT CONTEXT:\n{patient_block}\n"
        f"PREVIOUS SESSION SUMMARIES:\n{prev_summary_block}\n"
        "CHAT HISTORY:\n"
        + "\n".join(history)
    )


def _call_model(prompt):
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not api_key or api_key == PLACEHOLDER_KEY:
        return "AI is in fallback mode because Gemini API key is missing."

    genai.configure(api_key=api_key)
    model_name = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")
    model = genai.GenerativeModel(model_name=model_name)
    response = model.generate_content(prompt)
    return (getattr(response, "text", "") or "").strip()


def generate_assistant_reply(session, selected_patient, user_text, previous_summaries):
    context = _build_context_block(session, selected_patient, previous_summaries)
    prompt = f"{context}\n\nLATEST USER QUESTION:\n{user_text}\n"

    try:
        raw = _call_model(prompt)
    except Exception:
        raw = "Assistant is temporarily unavailable. Please try again."

    action_data = None
    match = ACTION_PATTERN.search(raw)
    if match:
        action_text = match.group(1).strip()
        try:
            action_data = json.loads(action_text)
        except Exception:
            action_data = None
        raw = ACTION_PATTERN.sub("", raw).strip()

    if selected_patient is None and _query_is_patient_specific(user_text):
        raw += "\n\nTip: Select a patient for better patient-specific guidance."

    return raw, action_data


def generate_session_summary(session, selected_patient):
    patient_hint = selected_patient.name if selected_patient else "No patient selected"
    convo = "\n".join([f"{m.role.upper()}: {m.content}" for m in session.messages.all().order_by("created_at")])
    prompt = (
        "Create a concise session summary for caregiver handoff with sections: key questions, key guidance, pending actions, safety notes.\n"
        f"Patient context: {patient_hint}\n"
        f"Conversation:\n{convo}\n"
    )
    try:
        return _call_model(prompt)
    except Exception:
        return "Session ended. Summary generation unavailable."


def apply_action_proposal(patient, action_payload):
    operation = (action_payload.get("operation") or "create").lower()
    item_type = action_payload.get("item_type") or CareItem.ItemType.TASK

    if operation == "delete":
        item_id = action_payload.get("item_id")
        if item_id:
            CareItem.objects.filter(id=item_id, patient=patient, item_type=item_type).delete()
        return "delete"

    if operation == "update":
        item_id = action_payload.get("item_id")
        item = CareItem.objects.filter(id=item_id, patient=patient, item_type=item_type).first()
        if not item:
            return "skipped"
    else:
        item = CareItem(patient=patient, item_type=item_type)

    item.title = action_payload.get("title") or "AI proposed item"
    item.description = action_payload.get("description") or ""
    item.notes = action_payload.get("notes") or ""
    item.status = action_payload.get("status") or CareItem.Status.PENDING
    item.priority = action_payload.get("priority") or CareItem.Priority.MEDIUM

    due_at_text = action_payload.get("due_at")
    if due_at_text:
        try:
            item.due_at = datetime.fromisoformat(due_at_text)
        except Exception:
            pass

    item.recurrence_mode = action_payload.get("recurrence_mode") or CareItem.RecurrenceMode.NONE
    weekdays = action_payload.get("recurrence_weekdays")
    if isinstance(weekdays, list):
        item.recurrence_weekdays = ",".join(weekdays)
    elif isinstance(weekdays, str):
        item.recurrence_weekdays = weekdays

    dates = action_payload.get("recurrence_dates")
    if isinstance(dates, list):
        item.recurrence_dates = ",".join(str(x) for x in dates)
    elif isinstance(dates, str):
        item.recurrence_dates = dates

    item.reminder_enabled = bool(action_payload.get("reminder_enabled"))
    item.reminder_minutes_before = action_payload.get("reminder_minutes_before") if item.reminder_enabled else None
    item.save()

    return operation

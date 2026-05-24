import json
import os
import re
from datetime import datetime

from google import genai
from google.genai import types

from tasks.models import CareItem

PLACEHOLDER_KEY = "replace-with-your-gemini-api-key"
ACTION_PATTERN = re.compile(r"<ACTION_JSON>(.*?)</ACTION_JSON>", re.DOTALL)

# ---------------------------------------------------------------------------
# Module-level client — reads GEMINI_API_KEY from the environment automatically.
# A single Client instance is reused across all requests (thread-safe).
# ---------------------------------------------------------------------------
_api_key = os.getenv("GEMINI_API_KEY", "").strip()
client = genai.Client(api_key=_api_key) if (_api_key and _api_key != PLACEHOLDER_KEY) else None

# ---------------------------------------------------------------------------
# Emergency keywords that immediately bypass the LLM pipeline.
# ---------------------------------------------------------------------------
_EMERGENCY_KEYWORDS = (
    "unresponsive",
    "choking",
    "seizure",
    "unconscious",
    "bleeding",
    "heart attack",
    "stroke",
)

_EMERGENCY_RESPONSE = (
    "🚨 EMERGENCY DETECTED 🚨\n\n"
    "This situation requires IMMEDIATE emergency services. Please follow these steps right now:\n"
    "1. Call your local emergency number (911 / 112 / 999) immediately.\n"
    "2. Do NOT leave the patient alone.\n"
    "3. Stay on the line with the dispatcher — they will guide you.\n"
    "4. Unlock the front door so responders can enter.\n"
    "5. Do not administer food, water, or medication unless directed by the dispatcher.\n\n"
    "This AI assistant cannot replace emergency services. Call for help NOW."
)


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
        diseases = selected_patient.disease_entries.all()
        medications = selected_patient.medication_entries.all()
        sensitivities = selected_patient.sensitivity_entries.all()

        disease_list = "\n".join([f"- {d.name} (Notes: {d.notes or 'None'}, Source: {d.get_source_display()})" for d in diseases]) or "None"
        med_list = "\n".join([f"- {m.name} {m.dosage} (Instructions: {m.instructions or 'None'}, Notes: {m.notes or 'None'}, Source: {m.get_source_display()})" for m in medications]) or "None"
        sens_list = "\n".join([f"- {s.name} ({s.get_kind_display()}, Severity: {s.severity or 'Not specified'}, Notes: {s.notes or 'None'}, Source: {s.get_source_display()})" for s in sensitivities]) or "None"

        patient_block = (
            f"Patient Name: {selected_patient.name}\n"
            f"Age: {selected_patient.age}\n"
            f"Gender: {selected_patient.get_gender_display()}\n"
            f"Current Scenario: {selected_patient.current_scenario_description or 'None'}\n"
            f"Doctor Guidelines: {selected_patient.doctor_guidelines or 'None'}\n"
            f"Current AI Summary: {selected_patient.ai_summary or 'No summary yet'}\n"
            f"Diseases:\n{disease_list}\n"
            f"Active Medications:\n{med_list}\n"
            f"Sensitivities & Allergies:\n{sens_list}\n"
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
        "- If user asks you to generate, write, update, or rewrite the patient summary, include an action proposal in this format:\n"
        "<ACTION_JSON>{\"operation\": \"update\", \"item_type\": \"summary\", \"title\": \"Update Patient Summary\", \"summary_text\": \"...detailed, practical patient summary...\"}</ACTION_JSON>\n"
        "- Do not execute actions yourself; only propose.\n\n"
        f"CURRENT SESSION SOURCE: {session.source}\n"
        f"PATIENT CONTEXT:\n{patient_block}\n"
        f"PREVIOUS SESSION SUMMARIES:\n{prev_summary_block}\n"
        "CHAT HISTORY:\n"
        + "\n".join(history)
    )


def _call_model(prompt: str) -> str:
    """Send *prompt* to the Gemini model and return the text response.

    Uses the module-level ``client`` (google-genai SDK).  Falls back to a
    static message when the API key is absent or is still the placeholder.
    Temperature is fixed at 0.1 to keep responses strictly factual.
    """
    if client is None:
        return "AI is in fallback mode because Gemini API key is missing."

    model_name = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
    response = client.models.generate_content(
        model=model_name,
        contents=prompt,
        config=types.GenerateContentConfig(
            temperature=0.1,
        ),
    )
    return (getattr(response, "text", "") or "").strip()


def generate_assistant_reply(session, selected_patient, user_text, previous_summaries):
    # ------------------------------------------------------------------
    # EMERGENCY BYPASS BLOCK — evaluated before any other logic.
    # If the user message contains a life-threatening keyword the LLM
    # pipeline is skipped entirely and a hardcoded directive is returned.
    # ------------------------------------------------------------------
    _text_lower = (user_text or "").lower()
    if any(kw in _text_lower for kw in _EMERGENCY_KEYWORDS):
        return _EMERGENCY_RESPONSE, None

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

    if item_type == "summary":
        summary_text = action_payload.get("summary_text") or action_payload.get("description") or action_payload.get("notes") or ""
        patient.ai_summary = summary_text.strip()
        patient.save(update_fields=["ai_summary", "updated_at"])
        return "update"

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

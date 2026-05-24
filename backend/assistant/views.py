import logging

from django.contrib import messages
from django.core.files.base import ContentFile
from django.http import JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.utils import timezone
from datetime import timedelta
from django.views.decorators.csrf import csrf_exempt
from django.contrib.auth.decorators import login_required
from django.views.decorators.clickjacking import xframe_options_sameorigin
from django.db.models import Q

from dashboard.views import _caretaker_context, _get_or_create_caretaker_profile
from patients.models import Patient

from .models import ChatActionProposal, ChatMessage, ChatSession
from .services import apply_action_proposal, generate_assistant_reply, generate_session_summary


logger = logging.getLogger(__name__)

IDLE_TIMEOUT_MINUTES = 20

# ---------------------------------------------------------------------------
# Fallback texts shown in the UI when the AI layer is unavailable.
# These are surfaced as Django messages so the template renders them
# via the existing {% if messages %} block without any structural change.
# ---------------------------------------------------------------------------
_AI_REPLY_FALLBACK = (
    "The AI assistant is temporarily unavailable due to a service interruption. "
    "Your message has been recorded. Please try again in a moment."
)
_AI_SUMMARY_FALLBACK = (
    "AI summary temporarily offline. Displaying raw data logs."
)


@login_required
@xframe_options_sameorigin
def index(request, patient_id=None):
    patient_id = patient_id or request.GET.get("patient")
    session_id = request.GET.get("session")
    source = request.GET.get("source", ChatSession.Source.CARETAKER)
    embedded = request.GET.get("embedded") == "1"

    # ── Patient lookup — scoped to request.user (multi-tenant guard) ─────────
    selected_patient = None
    if patient_id:
        selected_patient = get_object_or_404(Patient, id=patient_id, user=request.user)

    # ── Session lookup — scoped to request.user via patient FK ───────────────
    current_session = None
    if session_id:
        # Only surface sessions whose patient belongs to this user, or
        # patient-less sessions (source=caretaker global).
        current_session = ChatSession.objects.filter(
            Q(patient__isnull=True) | Q(patient__user=request.user),
            id=session_id
        ).prefetch_related("messages", "action_proposals").first()

        # Simpler fallback for the common case: if the session has a patient,
        # make sure that patient belongs to this user.
        if current_session is None:
            current_session = get_object_or_404(
                ChatSession.objects.prefetch_related("messages", "action_proposals"),
                id=session_id,
            )
            if (
                current_session.patient
                and current_session.patient.user_id != request.user.pk
            ):
                logger.warning(
                    "Tenant isolation: user %s attempted to access session %s "
                    "belonging to a different user.",
                    request.user.pk,
                    session_id,
                )
                return redirect("assistant:index")

        if current_session and current_session.patient:
            selected_patient = current_session.patient
    else:
        current_session = ChatSession.objects.filter(
            is_active=True,
            source=source,
            patient=selected_patient,
        ).order_by("-created_at").first()

    if current_session:
        current_session = _auto_end_if_idle(request, current_session)
        if current_session and current_session.is_active:
            current_session = _end_if_context_changed(request, current_session, selected_patient, source)

    if not current_session:
        _notify_if_last_session_closed_idle(request, source, selected_patient)

    if request.method == "POST":
        action = request.POST.get("action")
        if action == "send":
            current_session = _handle_send(request, current_session, selected_patient, source)
        elif action in {"approve_action", "reject_action"} and current_session:
            _handle_action_resolution(request, current_session, action)
            redirect_url = f"/assistant/?session={current_session.id}"
            if embedded:
                redirect_url += "&embedded=1"
            if source:
                redirect_url += f"&source={source}"
            return redirect(redirect_url)

        if current_session:
            redirect_url = f"/assistant/?session={current_session.id}"
            if embedded:
                redirect_url += "&embedded=1"
            if source:
                redirect_url += f"&source={source}"
            return redirect(redirect_url)

    # ── Context queries — scoped to request.user ─────────────────────────────
    sessions = ChatSession.objects.filter(
        patient__user=request.user,
    ).order_by("-created_at")[:15].union(
        ChatSession.objects.filter(patient__isnull=True).order_by("-created_at")[:15]
    )
    patient_options = Patient.objects.filter(user=request.user).order_by("name")
    sidebar_patients = [
        {
            "id": patient.id,
            "name": patient.name,
            "age": patient.age,
            "gender": patient.get_gender_display(),
            "diseases": list(patient.history_diseases or []),
            "allergies": patient.allergies,
        } for patient in patient_options
    ]
    caretaker_profile = _get_or_create_caretaker_profile(request.user)
    messages_list = list(current_session.messages.all()) if current_session else []
    proposals = current_session.action_proposals.filter(status=ChatActionProposal.Status.PENDING) if current_session else []
    previous_summaries = []
    if current_session:
        summary_qs = ChatSession.objects.filter(is_active=False).exclude(id=current_session.id)
        if current_session.patient:
            summary_qs = summary_qs.filter(patient=current_session.patient)
        previous_summaries = list(summary_qs.values_list("session_summary", flat=True)[:5])

    return render(
        request,
        "assistant/chat.html",
        {
            "caretaker": _caretaker_context(caretaker_profile),
            "selected_patient": selected_patient,
            "sessions": sessions,
            "current_session": current_session,
            "messages_list": messages_list,
            "patient_options": patient_options,
            "sidebar_patients": sidebar_patients,
            "source": source,
            "proposals": proposals,
            "previous_summaries": previous_summaries,
            "embedded": embedded,
            "active_nav": "assistant",
        },
    )


# ─── Session lifecycle helpers ────────────────────────────────────────────────

def _auto_end_if_idle(request, current_session):
    if not current_session.is_active:
        return current_session

    last_message = current_session.messages.order_by("-created_at").first()
    last_activity = last_message.created_at if last_message else current_session.created_at
    deadline = timezone.now() - timedelta(minutes=IDLE_TIMEOUT_MINUTES)
    if last_activity < deadline:
        _handle_end_session(current_session, current_session.patient, ChatSession.EndReason.IDLE)
        messages.info(
            request,
            f"Session #{current_session.id} ended due to {IDLE_TIMEOUT_MINUTES} minutes of inactivity.",
        )
        return None

    return current_session


def _end_if_context_changed(request, current_session, selected_patient, source):
    selected_patient_id = selected_patient.id if selected_patient else None
    current_patient_id = current_session.patient_id
    if selected_patient_id != current_patient_id or source != current_session.source:
        _handle_end_session(current_session, current_session.patient, ChatSession.EndReason.CONTEXT_CHANGE)
        messages.info(request, "Previous session ended because patient/context changed.")
        return None
    return current_session


def _handle_send(request, current_session, selected_patient, source):
    """Process an incoming user message.

    Error-handling contract:
    - If ``generate_assistant_reply`` raises *any* exception (network timeout,
      API quota exceeded, connection refused, etc.) we catch it, log full
      telemetry at ERROR level, persist the user message, and store a
      graceful fallback reply in the database so the UI always shows something
      meaningful rather than a 500 page.
    """
    user_text = (request.POST.get("message") or "").strip()
    selected_patient_id = request.POST.get("selected_patient")
    if selected_patient_id:
        # Enforce multi-tenant boundary: only allow patients owned by this user.
        selected_patient = Patient.objects.filter(
            id=selected_patient_id, user=request.user
        ).first()
    else:
        selected_patient = None

    if not user_text:
        return current_session

    if current_session is None:
        current_session = ChatSession.objects.create(
            source=source,
            patient=selected_patient,
            title=f"Session - {timezone.now().strftime('%Y-%m-%d %H:%M')}",
        )
    else:
        if current_session.patient_id != (selected_patient.id if selected_patient else None):
            _handle_end_session(current_session, current_session.patient, ChatSession.EndReason.CONTEXT_CHANGE)
            current_session = ChatSession.objects.create(
                source=source,
                patient=selected_patient,
                title=f"Session - {timezone.now().strftime('%Y-%m-%d %H:%M')}",
            )

    # Persist the user turn before calling the AI so it is never lost.
    ChatMessage.objects.create(session=current_session, role=ChatMessage.Role.USER, content=user_text)

    previous_summary_qs = ChatSession.objects.filter(is_active=False).exclude(id=current_session.id)
    if selected_patient:
        previous_summary_qs = previous_summary_qs.filter(patient=selected_patient)
    previous_summaries = list(previous_summary_qs.values_list("session_summary", flat=True)[:5])

    # ── External AI call — wrapped in a resilience block ─────────────────────
    response_text = _AI_REPLY_FALLBACK
    action_data = None
    try:
        response_text, action_data = generate_assistant_reply(
            current_session, selected_patient, user_text, previous_summaries
        )
    except Exception as exc:
        logger.error(
            "generate_assistant_reply failed for session=%s user=%s: %s",
            current_session.pk,
            request.user.pk,
            exc,
            exc_info=True,
        )
        messages.warning(
            request,
            "The AI assistant is temporarily unavailable. Your message was saved. Please try again.",
        )
        # response_text stays as _AI_REPLY_FALLBACK; action_data stays None.

    ChatMessage.objects.create(session=current_session, role=ChatMessage.Role.ASSISTANT, content=response_text)

    if action_data and selected_patient:
        ChatActionProposal.objects.create(
            session=current_session,
            patient=selected_patient,
            item_type=(action_data.get("item_type") or "task"),
            operation=(action_data.get("operation") or "create"),
            title=(action_data.get("title") or "AI proposal"),
            payload=action_data,
        )

    return current_session


def _handle_end_session(current_session, selected_patient, end_reason):
    """Close a session, generate its AI summary, and persist a summary file.

    All three external-facing operations are individually guarded:

    1. ``generate_session_summary`` — may fail if Gemini is unreachable.
       Fallback: raw conversation transcript is assembled from the PostgreSQL
       ``ChatMessage`` rows and written as-is, with the offline warning badge
       prepended.  The UI template renders ``session_summary`` directly, so
       the caretaker always sees *something*.

    2. ``summary_file.save`` — may fail if the storage backend (S3 / local
       filesystem) is unreachable.  Fallback: we set the session as ended
       without the file.  The summary text is still in the ``session_summary``
       DB column and will be displayed in the UI.

    3. ``current_session.save`` — a final ``try/except`` ensures the session
       is always marked inactive in the DB even if the file-save failed.
    """
    if not current_session.is_active:
        return

    # ── 1. AI summary generation ──────────────────────────────────────────────
    summary = _AI_SUMMARY_FALLBACK
    try:
        summary = generate_session_summary(current_session, selected_patient)
    except Exception as exc:
        logger.error(
            "generate_session_summary failed for session=%s: %s",
            current_session.pk,
            exc,
            exc_info=True,
        )
        # Build a raw fallback from PostgreSQL data — zero external dependency.
        raw_lines = [
            f"{m.role.upper()}: {m.content}"
            for m in current_session.messages.order_by("created_at")
        ]
        raw_transcript = "\n".join(raw_lines) if raw_lines else "(no messages)"
        summary = (
            f"⚠ {_AI_SUMMARY_FALLBACK}\n\n"
            f"--- Raw Conversation Log ---\n{raw_transcript}"
        )

    current_session.session_summary = summary
    current_session.is_active = False
    current_session.ended_at = timezone.now()
    current_session.end_reason = end_reason

    # ── 2. Summary file persistence ───────────────────────────────────────────
    try:
        file_content = ContentFile(summary.encode("utf-8"))
        filename = f"session_{current_session.id}_summary.txt"
        current_session.summary_file.save(filename, file_content, save=False)
    except Exception as exc:
        logger.error(
            "summary_file.save failed for session=%s: %s",
            current_session.pk,
            exc,
            exc_info=True,
        )
        # Proceed without the file — summary text is safe in the DB column.

    # ── 3. DB save — always attempt regardless of file-save outcome ───────────
    try:
        current_session.save(
            update_fields=["session_summary", "is_active", "ended_at", "summary_file", "end_reason"]
        )
    except Exception as exc:
        logger.critical(
            "CRITICAL: could not mark session=%s as ended: %s",
            current_session.pk,
            exc,
            exc_info=True,
        )


def _notify_if_last_session_closed_idle(request, source, selected_patient):
    qs = ChatSession.objects.filter(is_active=False, end_reason=ChatSession.EndReason.IDLE, source=source)
    if selected_patient:
        qs = qs.filter(patient=selected_patient)
    else:
        qs = qs.filter(patient__isnull=True)

    last_idle = qs.order_by("-ended_at").first()
    if not last_idle:
        return

    seen_key = "assistant_last_idle_notice_session"
    if request.session.get(seen_key) == last_idle.id:
        return

    messages.info(request, f"Your last session #{last_idle.id} was closed due to inactivity.")
    request.session[seen_key] = last_idle.id


@csrf_exempt
def close_session_on_tab_close(request):
    """Beacon endpoint called by the browser on page unload.

    Must never return 500 — the browser ignores the response body for
    ``sendBeacon`` requests, but a 500 is logged as a server error and
    triggers alerting noise.  All failure modes are caught and logged.
    """
    if request.method != "POST":
        return JsonResponse({"ok": False}, status=405)

    session_id = request.POST.get("session")
    if not session_id:
        return JsonResponse({"ok": False}, status=400)

    try:
        current_session = ChatSession.objects.filter(id=session_id, is_active=True).first()
        if not current_session:
            return JsonResponse({"ok": True})

        _handle_end_session(current_session, current_session.patient, ChatSession.EndReason.TAB_CLOSE)
    except Exception as exc:
        # Log and return 200 — the browser must not retry on 5xx.
        logger.error(
            "close_session_on_tab_close failed for session=%s: %s",
            session_id,
            exc,
            exc_info=True,
        )

    return JsonResponse({"ok": True})


def _handle_action_resolution(request, current_session, action):
    """Approve or reject a pending ``ChatActionProposal``.

    ``apply_action_proposal`` performs DB writes (CareItem, Patient.ai_summary).
    These are wrapped so a transient DB error does not crash the POST cycle.
    """
    proposal_id = request.POST.get("proposal_id")
    proposal = get_object_or_404(ChatActionProposal, id=proposal_id, session=current_session)

    if action == "approve_action":
        if proposal.patient:
            try:
                apply_action_proposal(proposal.patient, proposal.payload)
            except Exception as exc:
                logger.error(
                    "apply_action_proposal failed for proposal=%s session=%s: %s",
                    proposal.pk,
                    current_session.pk,
                    exc,
                    exc_info=True,
                )
                messages.error(
                    request,
                    "The AI action could not be applied due to a server error. "
                    "The proposal has been marked approved but may need manual review.",
                )
        proposal.status = ChatActionProposal.Status.APPROVED
        messages.success(request, "AI proposal approved and applied.")
    else:
        proposal.status = ChatActionProposal.Status.REJECTED
        messages.info(request, "AI proposal rejected.")

    proposal.resolved_at = timezone.now()
    try:
        proposal.save(update_fields=["status", "resolved_at"])
    except Exception as exc:
        logger.error(
            "proposal.save failed for proposal=%s: %s",
            proposal.pk,
            exc,
            exc_info=True,
        )

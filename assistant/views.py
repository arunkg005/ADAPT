from django.contrib import messages
from django.core.files.base import ContentFile
from django.http import JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.utils import timezone
from datetime import timedelta
from django.views.decorators.csrf import csrf_exempt

from dashboard.views import _caretaker_context, _get_or_create_caretaker_profile
from patients.models import Patient

from .models import ChatActionProposal, ChatMessage, ChatSession
from .services import apply_action_proposal, generate_assistant_reply, generate_session_summary


IDLE_TIMEOUT_MINUTES = 20

def index(request, patient_id=None):
	patient_id = patient_id or request.GET.get("patient")
	session_id = request.GET.get("session")
	source = request.GET.get("source", ChatSession.Source.CARETAKER)
	embedded = request.GET.get("embedded") == "1"

	selected_patient = None
	if patient_id:
		selected_patient = get_object_or_404(Patient, id=patient_id)

	current_session = None
	if session_id:
		current_session = get_object_or_404(ChatSession.objects.prefetch_related("messages", "action_proposals"), id=session_id)
		if current_session.patient:
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
			return redirect(f"/assistant/?session={current_session.id}")

		if current_session:
			return redirect(f"/assistant/?session={current_session.id}")

	sessions = ChatSession.objects.order_by("-created_at")[:15]
	patient_options = Patient.objects.order_by("name")
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
	caretaker_profile = _get_or_create_caretaker_profile()
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
	user_text = (request.POST.get("message") or "").strip()
	selected_patient_id = request.POST.get("selected_patient")
	if selected_patient_id:
		selected_patient = Patient.objects.filter(id=selected_patient_id).first()
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

	ChatMessage.objects.create(session=current_session, role=ChatMessage.Role.USER, content=user_text)

	previous_summary_qs = ChatSession.objects.filter(is_active=False).exclude(id=current_session.id)
	if selected_patient:
		previous_summary_qs = previous_summary_qs.filter(patient=selected_patient)
	previous_summaries = list(previous_summary_qs.values_list("session_summary", flat=True)[:5])

	response_text, action_data = generate_assistant_reply(current_session, selected_patient, user_text, previous_summaries)
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
	if not current_session.is_active:
		return
	summary = generate_session_summary(current_session, selected_patient)
	current_session.session_summary = summary
	current_session.is_active = False
	current_session.ended_at = timezone.now()
	current_session.end_reason = end_reason

	file_content = ContentFile(summary.encode("utf-8"))
	filename = f"session_{current_session.id}_summary.txt"
	current_session.summary_file.save(filename, file_content, save=False)
	current_session.save(update_fields=["session_summary", "is_active", "ended_at", "summary_file", "end_reason"])


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
	if request.method != "POST":
		return JsonResponse({"ok": False}, status=405)

	session_id = request.POST.get("session")
	if not session_id:
		return JsonResponse({"ok": False}, status=400)

	current_session = ChatSession.objects.filter(id=session_id, is_active=True).first()
	if not current_session:
		return JsonResponse({"ok": True})

	_handle_end_session(current_session, current_session.patient, ChatSession.EndReason.TAB_CLOSE)
	return JsonResponse({"ok": True})


def _handle_action_resolution(request, current_session, action):
	proposal_id = request.POST.get("proposal_id")
	proposal = get_object_or_404(ChatActionProposal, id=proposal_id, session=current_session)

	if action == "approve_action":
		if proposal.patient:
			apply_action_proposal(proposal.patient, proposal.payload)
		proposal.status = ChatActionProposal.Status.APPROVED
		messages.success(request, "AI proposal approved and applied.")
	else:
		proposal.status = ChatActionProposal.Status.REJECTED
		messages.info(request, "AI proposal rejected.")

	proposal.resolved_at = timezone.now()
	proposal.save(update_fields=["status", "resolved_at"])


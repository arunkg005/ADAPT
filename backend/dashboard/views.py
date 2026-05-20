from django.http import Http404
from django.contrib import messages
from django.shortcuts import redirect, render
from django.utils import timezone
from django.contrib.auth.decorators import login_required
from django.contrib.auth import login
from django.contrib.auth.forms import UserCreationForm

from dashboard.forms import CaretakerProfileForm
from dashboard.models import CaretakerProfile
from patients.forms import PatientSettingsForm
from patients.models import Patient, PatientSensitivity
from tasks.forms import CareItemForm
from tasks.models import CareItem


CARETAKER_DEFAULTS = {
	"full_name": "Neha Sharma",
	"role": "Primary Caretaker",
	"timezone": "Asia/Kolkata",
}


def _dashboard_context(request, profile, active_nav, extra=None):
	context = {
		"caretaker": _caretaker_context(profile),
		"patients": _all_patients_for_dashboard(request.user),
		"active_nav": active_nav,
	}
	if extra:
		context.update(extra)
	return context


def _selected_patient_instance_from_request(request):
	patient_id = (request.GET.get("patient_id") or request.POST.get("patient_id") or "").strip()
	if not patient_id.isdigit():
		return None
	try:
		return _resolve_patient_instance(int(patient_id), request.user)
	except Http404:
		return None


def _render_patient_selection(request, caretaker_profile, active_nav, heading):
	return render(
		request,
		"dashboard/select_patient_for_routine.html",
		_dashboard_context(request,
			caretaker_profile,
			active_nav,
			{
				"heading": heading,
			},
		),
	)


LEGACY_DEMO_ID_NAME_MAP = {
	10001: "Asha Verma (Demo)",
	10002: "Raghav Singh (Demo)",
}


def _ensure_demo_patients(user):
	Patient.objects.get_or_create(
		name="Asha Verma (Demo)",
		user=user,
		defaults={
			"age": 68,
			"gender": Patient.Gender.FEMALE,
			"allergies": "Penicillin",
			"history_diseases": ["Type 2 Diabetes", "Hypertension"],
			"current_scenario_description": "Mild fatigue in evenings; needs strict medication timing.",
			"doctor_guidelines": "Monitor BP and sugar twice daily.",
			"routine_reminder_enabled": True,
			"routine_reminder_minutes_before": 20,
			"ai_summary": "Demo patient summary: Focus on sugar control, BP monitoring, hydration, and safe activity pacing.",
		},
	)
	Patient.objects.get_or_create(
		name="Raghav Singh (Demo)",
		user=user,
		defaults={
			"age": 74,
			"gender": Patient.Gender.MALE,
			"allergies": "No known drug allergies",
			"history_diseases": ["Parkinsonism", "Arthritis"],
			"current_scenario_description": "Mobility stiffness in mornings; delayed afternoon medication occasionally.",
			"doctor_guidelines": "Support mobility drills and maintain hydration schedule.",
			"routine_reminder_enabled": True,
			"routine_reminder_minutes_before": 30,
			"ai_summary": "Demo patient summary: Emphasize mobility support, timely meds, posture, and fall risk prevention.",
		},
	)

	for patient in Patient.objects.filter(user=user, name__in=["Asha Verma (Demo)", "Raghav Singh (Demo)"]):
		CareItem.objects.get_or_create(
			patient=patient,
			item_type=CareItem.ItemType.ROUTINE,
			title="Morning vitals and hygiene support",
			defaults={
				"description": "Daily routine: BP/sugar check and assisted morning hygiene.",
				"notes": "Step 1: check BP; Step 2: check sugar; Step 3: log values.",
				"status": CareItem.Status.PENDING,
				"priority": CareItem.Priority.MEDIUM,
				"recurrence_mode": CareItem.RecurrenceMode.DAILY,
				"reminder_enabled": patient.routine_reminder_enabled,
				"reminder_minutes_before": patient.routine_reminder_minutes_before,
			},
		)
		CareItem.objects.get_or_create(
			patient=patient,
			item_type=CareItem.ItemType.SCHEDULE,
			title="Follow-up consultation",
			defaults={
				"description": "Occasional appointment task with set time/deadline.",
				"notes": "Carry previous report and medication list.",
				"status": CareItem.Status.PENDING,
				"priority": CareItem.Priority.HIGH,
				"recurrence_mode": CareItem.RecurrenceMode.SPECIFIC_DATES,
				"recurrence_dates": "2026-04-20, 2026-05-20",
				"reminder_enabled": True,
				"reminder_minutes_before": 120,
			},
		)
		CareItem.objects.get_or_create(
			patient=patient,
			item_type=CareItem.ItemType.TASK,
			title="General wellness check-in",
			defaults={
				"description": "General task that can be repetitive or non-repetitive.",
				"notes": "Review hydration, appetite, and mobility comfort.",
				"status": CareItem.Status.IN_PROGRESS,
				"priority": CareItem.Priority.MEDIUM,
				"recurrence_mode": CareItem.RecurrenceMode.SELECTED_WEEKDAYS,
				"recurrence_weekdays": "mon,wed,fri",
				"reminder_enabled": True,
				"reminder_minutes_before": 45,
			},
		)


def _serialize_patient(patient):
	medication_entries = patient.medication_entries.all()
	disease_entries = patient.disease_entries.all()
	allergy_entries = patient.sensitivity_entries.filter(kind=PatientSensitivity.Kind.ALLERGY)
	intolerance_entries = patient.sensitivity_entries.filter(kind=PatientSensitivity.Kind.INTOLERANCE)
	items = CareItem.objects.filter(patient=patient)
	task_items = items.filter(item_type=CareItem.ItemType.TASK)
	routine_items = items.filter(item_type=CareItem.ItemType.ROUTINE)
	schedule_items = items.filter(item_type=CareItem.ItemType.SCHEDULE)
	total_today = task_items.count()
	completed_today = task_items.filter(status=CareItem.Status.DONE).count()
	pending_today = task_items.exclude(status=CareItem.Status.DONE).count()
	overdue = task_items.filter(status__in=[CareItem.Status.PENDING, CareItem.Status.IN_PROGRESS], due_at__isnull=False).count()
	completion_percent = int((completed_today / total_today) * 100) if total_today else 0
	return {
		"id": patient.id,
		"name": patient.name,
		"age": patient.age,
		"gender": patient.get_gender_display(),
		"diseases": [d.name for d in disease_entries] or patient.history_diseases,
		"allergies": ", ".join([a.name for a in allergy_entries]) or patient.allergies,
		"intolerances": ", ".join([i.name for i in intolerance_entries]) or patient.intolerances,
		"current_scenario": patient.current_scenario_description,
		"doctor_guidelines": patient.doctor_guidelines,
		"ai_summary": patient.ai_summary,
		"medication_details": [
			{
				"name": medication.name,
				"dosage": medication.dosage,
				"instructions": medication.instructions,
				"notes": medication.notes,
				"display": " - ".join(filter(None, [medication.name, medication.dosage])) if medication.dosage else medication.name,
			}
			for medication in medication_entries
		],
		"medications": [f"{m.name} {('- ' + m.dosage) if m.dosage else ''}".strip() for m in medication_entries] or ["No medications listed"],
		"tasks": [f"{item.title} ({item.get_status_display()})" for item in task_items] or ["Task list is empty."],
		"routine": [f"{item.title} ({item.notes or 'No notes'})" for item in routine_items] or ["Routine tasks are not configured yet."],
		"schedule": [f"{item.title} ({item.notes or 'No notes'})" for item in schedule_items] or ["Scheduled tasks are not configured yet."],
		"reminder_preference": {
			"enabled": patient.routine_reminder_enabled,
			"minutes_before": patient.routine_reminder_minutes_before,
		},
		"task_progress": {
			"total_today": total_today,
			"completed_today": completed_today,
			"pending_today": pending_today,
			"overdue": overdue,
			"completion_percent": completion_percent,
			"avg_delay_min": 0,
		},
		"analysis": [
			"Use Task/Routine/Schedule buttons for full CRUD operations.",
		],
	}


def _task_lab_insight(patient_data, task_items):
	if not patient_data:
		return "Select a patient to begin building a task draft."

	overdue_tasks = task_items.filter(
		status__in=[CareItem.Status.PENDING, CareItem.Status.IN_PROGRESS],
		due_at__lt=timezone.now(),
	)
	if overdue_tasks.exists():
		return f"{overdue_tasks.count()} task(s) are overdue. Start with the oldest timed item."

	high_priority_tasks = task_items.filter(
		status__in=[CareItem.Status.PENDING, CareItem.Status.IN_PROGRESS],
		priority=CareItem.Priority.HIGH,
	)
	if high_priority_tasks.exists():
		return f"{high_priority_tasks.count()} high-priority task(s) still need attention."

	if patient_data["reminder_preference"]["enabled"]:
		return "Reminder timing is already active for this patient. Use it when scheduling the next task."

	return "Add a task draft, keep the steps short, and schedule it from this workspace."


def _task_lab_initials(patient_data):
	if not patient_data:
		return "TL"
	parts = [part for part in patient_data["name"].split() if part]
	initials = "".join(part[0] for part in parts[:2]).upper()
	return initials or "TL"


def _sync_task_to_routine(task_item):
	routine_item, created = CareItem.objects.get_or_create(
		patient=task_item.patient,
		item_type=CareItem.ItemType.ROUTINE,
		title=task_item.title,
		defaults={
			"description": task_item.description,
			"notes": task_item.notes,
			"status": CareItem.Status.PENDING,
			"priority": task_item.priority,
			"due_at": task_item.due_at,
			"recurrence_mode": task_item.recurrence_mode,
			"recurrence_weekdays": task_item.recurrence_weekdays,
			"recurrence_dates": task_item.recurrence_dates,
			"reminder_enabled": task_item.reminder_enabled,
			"reminder_minutes_before": task_item.reminder_minutes_before,
		},
	)
	if not created:
		routine_item.description = task_item.description
		routine_item.notes = task_item.notes
		routine_item.status = CareItem.Status.PENDING
		routine_item.priority = task_item.priority
		routine_item.due_at = task_item.due_at
		routine_item.recurrence_mode = task_item.recurrence_mode
		routine_item.recurrence_weekdays = task_item.recurrence_weekdays
		routine_item.recurrence_dates = task_item.recurrence_dates
		routine_item.reminder_enabled = task_item.reminder_enabled
		routine_item.reminder_minutes_before = task_item.reminder_minutes_before
		routine_item.save()
	return routine_item


def _routine_workspace_insight(patient_data, routine_items, schedule_items):
	if not patient_data:
		return "Select a patient to begin building a routine."

	overdue_routines = routine_items.filter(
		status__in=[CareItem.Status.PENDING, CareItem.Status.IN_PROGRESS],
		due_at__lt=timezone.now(),
	)
	if overdue_routines.exists():
		return f"{overdue_routines.count()} routine item(s) are overdue. Start with the oldest timed entry."

	high_priority_routines = routine_items.filter(
		status__in=[CareItem.Status.PENDING, CareItem.Status.IN_PROGRESS],
		priority=CareItem.Priority.HIGH,
	)
	if high_priority_routines.exists():
		return f"{high_priority_routines.count()} high-priority routine item(s) still need attention."

	if schedule_items.exists():
		return f"{schedule_items.count()} schedule item(s) are already attached to this patient."

	if patient_data["reminder_preference"]["enabled"]:
		return "Patient-level reminders are enabled. Keep the next routine aligned to that lead time."

	return "Build a short routine, keep the steps focused, and schedule it from this workspace."


def _routine_workspace_payload(request, selected_patient_instance):
	patients = _all_patients_for_dashboard(request.user)
	selected_patient = _serialize_patient(selected_patient_instance) if selected_patient_instance else None
	routine_items = CareItem.objects.none()
	schedule_items = CareItem.objects.none()
	routine_form = None
	task_items = CareItem.objects.none()
	if selected_patient_instance:
		routine_items = CareItem.objects.filter(
			patient=selected_patient_instance,
			item_type=CareItem.ItemType.ROUTINE,
		).order_by("-created_at")
		schedule_items = CareItem.objects.filter(
			patient=selected_patient_instance,
			item_type=CareItem.ItemType.SCHEDULE,
		).order_by("-created_at")
		task_items = CareItem.objects.filter(
			patient=selected_patient_instance,
			item_type=CareItem.ItemType.TASK,
		).order_by("-created_at")
		if request.method == "POST":
			routine_form = CareItemForm(request.POST, item_type=CareItem.ItemType.ROUTINE, patient=selected_patient_instance)
			if routine_form.is_valid():
				item = routine_form.save(commit=False)
				item.patient = selected_patient_instance
				item.item_type = CareItem.ItemType.ROUTINE
				item.save()
				messages.success(request, "Routine saved.")
				return {"saved": True, "patient_id": selected_patient_instance.id}
		else:
			routine_form = CareItemForm(item_type=CareItem.ItemType.ROUTINE, patient=selected_patient_instance)

		if routine_form:
			routine_form.fields["status"].initial = CareItem.Status.PENDING
			routine_form.fields["priority"].initial = CareItem.Priority.MEDIUM
			routine_form.fields["recurrence_mode"].initial = CareItem.RecurrenceMode.DAILY

	routine_metrics = {
		"routine_count": routine_items.count() if selected_patient_instance else 0,
		"schedule_count": schedule_items.count() if selected_patient_instance else 0,
		"pending_routine": routine_items.filter(status=CareItem.Status.PENDING).count() if selected_patient_instance else 0,
		"active_routine": routine_items.exclude(status=CareItem.Status.DONE).count() if selected_patient_instance else 0,
		"reminder_enabled": selected_patient["reminder_preference"]["enabled"] if selected_patient else False,
	}
	routine_summary_minutes = (routine_metrics["routine_count"] * 30) if selected_patient_instance else 0

	return {
		"patients": patients,
		"selected_patient": selected_patient,
		"routine_form": routine_form,
		"routine_items": routine_items,
		"schedule_items": schedule_items,
		"routine_metrics": routine_metrics,
		"routine_summary_minutes": routine_summary_minutes,
		"routine_insight": _routine_workspace_insight(selected_patient, routine_items, schedule_items),
		"routine_initials": _task_lab_initials(selected_patient),
		"task_items": task_items,
		"workspace_action_name": "dashboard:schedule_routine",
	}


def _schedule_workspace_payload(user, selected_patient_instance):
	patients = _all_patients_for_dashboard(user)
	selected_patient = _serialize_patient(selected_patient_instance) if selected_patient_instance else None
	routine_items = CareItem.objects.none()
	schedule_items = CareItem.objects.none()
	if selected_patient_instance:
		routine_items = CareItem.objects.filter(
			patient=selected_patient_instance,
			item_type=CareItem.ItemType.ROUTINE,
		).order_by("due_at", "-created_at")
		schedule_items = CareItem.objects.filter(
			patient=selected_patient_instance,
			item_type=CareItem.ItemType.SCHEDULE,
		).order_by("due_at", "-created_at")

	def _describe_item(item, fallback_title, fallback_description):
		if not item:
			return {
				"title": fallback_title,
				"description": fallback_description,
				"time": "12:30 PM",
			}
		item_time = timezone.localtime(item.due_at).strftime("%I:%M %p").lstrip("0") if item.due_at else "12:30 PM"
		return {
			"title": item.title,
			"description": item.description or item.notes or fallback_description,
			"time": item_time,
		}

	schedule_sources = list(schedule_items[:3]) + list(routine_items[:1]) if selected_patient_instance else []
	high_priority_item = next((item for item in schedule_items if item.priority == CareItem.Priority.HIGH), None)
	urgent_item = high_priority_item or (schedule_sources[0] if schedule_sources else None)
	routine_item = routine_items.first() if selected_patient_instance else None
	medical_item = schedule_items.first() if selected_patient_instance else None

	schedule_events = [
		{
			"day": "MON",
			"date": "09",
			"slot": "09:00",
			"position_class": "schedule-event--one",
			"variant": "primary",
			**_describe_item(schedule_sources[0] if len(schedule_sources) > 0 else None, "Morning Meds", "Aricept 10mg"),
		},
		{
			"day": "TUE",
			"date": "10",
			"slot": "09:00",
			"position_class": "schedule-event--two",
			"variant": "secondary",
			**_describe_item(routine_item or (schedule_sources[1] if len(schedule_sources) > 1 else None), "Morning Walk", "30 mins with physical therapist"),
		},
		{
			"day": "WED",
			"date": "11",
			"slot": "11:00",
			"position_class": "schedule-event--three",
			"variant": "primary",
			**_describe_item(schedule_sources[2] if len(schedule_sources) > 2 else None, "Cognitive Ex.", "Memory matching game"),
		},
	]

	primary_description = selected_patient["current_scenario"] if selected_patient else "No patient selected yet."
	secondary_description = selected_patient["doctor_guidelines"] if selected_patient else ""
	pending_count = schedule_items.filter(status=CareItem.Status.PENDING).count() if selected_patient_instance else 0
	schedule_next_steps = [
		{
			"kind": "urgent",
			"label": "HIGH PRIORITY",
			"time": urgent_item and urgent_item.due_at and timezone.localtime(urgent_item.due_at).strftime("%I:%M %p").lstrip("0") or "12:30 PM",
			"title": urgent_item.title if urgent_item else "Diabetes Check",
			"description": urgent_item.description or urgent_item.notes or "Insulin injection and blood sugar logging.",
			"button": "Log Now",
		},
		{
			"kind": "routine",
			"label": "ROUTINE",
			"time": routine_item and routine_item.due_at and timezone.localtime(routine_item.due_at).strftime("%I:%M %p").lstrip("0") or "02:00 PM",
			"title": routine_item.title if routine_item else "Hydration Reminder",
			"description": routine_item.description or routine_item.notes or "Ensure 250ml water intake.",
			"progress": "2/3 Cups",
			"progress_value": 66,
			"progress_value_class": "schedule-progress-fill-66",
		},
		{
			"kind": "medical",
			"label": "MEDICAL",
			"time": medical_item and medical_item.due_at and timezone.localtime(medical_item.due_at).strftime("%b %d").lstrip("0") or "Tomorrow",
			"title": medical_item.title if medical_item else "Dr. Aris (Neurology)",
			"description": medical_item.description or medical_item.notes or "St. Jude Medical Center, Rm 402",
			"button": "View Details",
		},
	]

	return {
		"patients": patients,
		"selected_patient": selected_patient,
		"schedule_items": schedule_items,
		"routine_items": routine_items,
		"schedule_metrics": {
			"pending": pending_count,
			"routine": routine_items.count() if selected_patient_instance else 0,
			"total": schedule_items.count() if selected_patient_instance else 0,
		},
		"schedule_month_label": "September 2024",
		"schedule_range_label": "Sept 9 — 15, 2024",
		"schedule_day_headers": [
			{"label": "MON", "date": "09", "active": False},
			{"label": "TUE", "date": "10", "active": True},
			{"label": "WED", "date": "11", "active": False},
			{"label": "THU", "date": "12", "active": False},
			{"label": "FRI", "date": "13", "active": False},
			{"label": "SAT", "date": "14", "active": False},
			{"label": "SUN", "date": "15", "active": False},
		],
		"schedule_time_labels": ["08:00", "09:00", "10:00", "11:00", "12:00"],
		"schedule_events": schedule_events,
		"schedule_next_steps": schedule_next_steps,
		"schedule_note_title": "Caretaker Note",
		"schedule_note_body": primary_description,
		"schedule_note_followup": secondary_description or "Recommended sensory activity for later.",
		"schedule_pending_label": f"{pending_count} PENDING",
	}


def _get_or_create_caretaker_profile(user):
	profile = CaretakerProfile.objects.filter(user=user).first()
	if profile:
		return profile
	return CaretakerProfile.objects.create(user=user, **CARETAKER_DEFAULTS)


def _caretaker_context(profile):
	return {
		"name": profile.full_name,
		"role": profile.role,
	}


def _resolve_patient_instance(patient_id, user):
	_ensure_demo_patients(user)
	try:
		return Patient.objects.prefetch_related("documents").get(id=patient_id, user=user)
	except Patient.DoesNotExist as exc:
		legacy_name = LEGACY_DEMO_ID_NAME_MAP.get(patient_id)
		if not legacy_name:
			raise Http404("Patient not found") from exc
		try:
			return Patient.objects.prefetch_related("documents").get(name=legacy_name, user=user)
		except Patient.DoesNotExist as mapped_exc:
			raise Http404("Patient not found") from mapped_exc


def _find_patient_or_404(patient_id, user):
	patient = _resolve_patient_instance(patient_id, user)
	return _serialize_patient(patient)


def _selected_patient_from_request(request):
	patient_id = request.GET.get("patient_id", "").strip()
	if not patient_id.isdigit():
		return None
	try:
		return _find_patient_or_404(int(patient_id), request.user)
	except Http404:
		return None


def _all_patients_for_dashboard(user):
	_ensure_demo_patients(user)
	patients_qs = Patient.objects.prefetch_related("documents").filter(user=user).order_by("name")
	return [_serialize_patient(patient) for patient in patients_qs]


@login_required
def home(request):
	patients = _all_patients_for_dashboard(request.user)
	caretaker_profile = _get_or_create_caretaker_profile(request.user)
	return render(
		request,
		"dashboard/index.html",
		_dashboard_context(request,
			caretaker_profile,
			"overview",
			{
				"patients": patients,
				"featured_patient": patients[0] if patients else None,
			},
		),
	)


@login_required
def patient_dashboard(request, patient_id):
	patient = _find_patient_or_404(patient_id, request.user)
	patients = _all_patients_for_dashboard(request.user)
	caretaker_profile = _get_or_create_caretaker_profile(request.user)
	return render(
		request,
		"dashboard/patient_dashboard.html",
		_dashboard_context(request,
			caretaker_profile,
			"overview",
			{
				"selected_patient": patient,
				"patients": patients,
			},
		),
	)

@login_required
def patient_settings(request, patient_id):
	patient = _resolve_patient_instance(patient_id, request.user)
	if request.method == "POST" and request.POST.get("action") == "delete":
		patient_name = patient.name
		patient.delete()
		messages.success(request, f"Patient '{patient_name}' deleted.")
		return redirect("dashboard:home")

	if request.method == "POST":
		form = PatientSettingsForm(request.POST, instance=patient)
		if form.is_valid():
			form.save()
			messages.success(request, "Patient settings updated.")
			return redirect("dashboard:patient_settings", patient_id=patient.id)
	else:
		form = PatientSettingsForm(instance=patient)

	caretaker_profile = _get_or_create_caretaker_profile(request.user)
	return render(
		request,
		"dashboard/patient_settings.html",
		_dashboard_context(request,
			caretaker_profile,
			"overview",
			{
				"patient": patient,
				"form": form,
			},
		),
	)


@login_required
def task_lab(request):
	caretaker_profile = _get_or_create_caretaker_profile(request.user)
	patients = _all_patients_for_dashboard(request.user)
	selected_patient_instance = _selected_patient_instance_from_request(request)

	selected_patient = _serialize_patient(selected_patient_instance) if selected_patient_instance else None
	task_items = CareItem.objects.none()
	task_form = None
	task_form_open = False
	if selected_patient_instance:
		task_items = CareItem.objects.filter(
			patient=selected_patient_instance,
			item_type=CareItem.ItemType.TASK,
		).order_by("-created_at")
		task_form = CareItemForm(item_type=CareItem.ItemType.TASK, patient=selected_patient_instance)
		if request.method == "POST":
			task_form = CareItemForm(request.POST, item_type=CareItem.ItemType.TASK, patient=selected_patient_instance)
			if task_form.is_valid():
				item = task_form.save(commit=False)
				item.patient = selected_patient_instance
				item.item_type = CareItem.ItemType.TASK
				item.save()
				_sync_task_to_routine(item)
				messages.success(request, "Task saved and synced to the routine.")
				return redirect(f"/dashboard/task-lab/?patient_id={selected_patient_instance.id}")
			else:
				task_form_open = True
		elif request.GET.get("compose") == "1":
			task_form_open = True

	task_metrics = {
		"total": task_items.count() if selected_patient_instance else 0,
		"pending": task_items.filter(status=CareItem.Status.PENDING).count() if selected_patient_instance else 0,
		"in_progress": task_items.filter(status=CareItem.Status.IN_PROGRESS).count() if selected_patient_instance else 0,
		"done": task_items.filter(status=CareItem.Status.DONE).count() if selected_patient_instance else 0,
		"overdue": task_items.filter(
			status__in=[CareItem.Status.PENDING, CareItem.Status.IN_PROGRESS],
			due_at__lt=timezone.now(),
		).count() if selected_patient_instance else 0,
	}
	return render(
		request,
		"dashboard/task_lab.html",
		_dashboard_context(request,
			caretaker_profile,
			"task-lab",
			{
				"patients": patients,
				"selected_patient": selected_patient,
				"task_form": task_form,
				"task_form_open": task_form_open,
				"task_items": task_items,
				"task_metrics": task_metrics,
				"task_insight": _task_lab_insight(selected_patient, task_items),
				"task_lab_initials": _task_lab_initials(selected_patient),
				"routine_items": CareItem.objects.filter(
					patient=selected_patient_instance,
					item_type=CareItem.ItemType.ROUTINE
				).order_by("due_at") if selected_patient_instance else CareItem.objects.none(),
			},
		),
	)


@login_required
def schedule_routine(request):
		caretaker_profile = _get_or_create_caretaker_profile(request.user)
		selected_patient_instance = _selected_patient_instance_from_request(request)
		if not selected_patient_instance:
			return _render_patient_selection(request, caretaker_profile, "routine", "Select a patient")
		workspace = _routine_workspace_payload(request, selected_patient_instance)
		if workspace.get("saved"):
			return redirect(f"/dashboard/schedule-routine/?patient_id={workspace['patient_id']}")
		return render(
			request,
			"dashboard/schedule_routine.html",
			_dashboard_context(request,
			caretaker_profile,
				"schedule-routine",
				{
					**workspace,
					"workspace_mode": "routine",
					"workspace_title": "Routine Builder",
					"workspace_action_name": "dashboard:schedule_routine",
				},
			),
		)


@login_required
def schedule_window(request):
	caretaker_profile = _get_or_create_caretaker_profile(request.user)
	selected_patient_instance = _selected_patient_instance_from_request(request)
	if not selected_patient_instance:
		return _render_patient_selection(request, caretaker_profile, "schedule", "Select a patient")
	workspace = _schedule_workspace_payload(request.user, selected_patient_instance)
	return render(
		request,
		"dashboard/schedule.html",
		_dashboard_context(request,
			caretaker_profile,
			"schedule",
			{
				**workspace,
			},
		),
	)


@login_required
def progress(request):
	caretaker_profile = _get_or_create_caretaker_profile(request.user)
	selected_patient_instance = _selected_patient_instance_from_request(request)
	if not selected_patient_instance:
		return _render_patient_selection(request, caretaker_profile, "progress", "Select a patient")
	patients = _all_patients_for_dashboard(request.user)
	selected_patient = _serialize_patient(selected_patient_instance)
	routine_items = CareItem.objects.none()
	schedule_items = CareItem.objects.none()
	task_items = CareItem.objects.none()
	analysis_metrics = {
		"tasks_completed_percent": 0,
		"missed_tasks": 0,
		"assistance_triggered": 0,
		"active_routines": 0,
	}
	analysis_trend = [22, 28, 40, 44, 35, 52, 68]
	assistance_breakdown = [
		{"label": "Cognitive Support", "value": 75, "color": "primary"},
		{"label": "Physical Aid", "value": 25, "color": "secondary-fixed-dim"},
	]
	activity_bars = [
		{"label": "AM", "value": 40},
		{"label": "NOON", "value": 85},
		{"label": "PM", "value": 60},
		{"label": "EVE", "value": 30},
		{"label": "NIGHT", "value": 15},
	]
	clinical_insight = "Select a patient to review the latest performance window."
	analysis_note = "Peak activity recorded at 11:45 AM during 'Physio Garden Walk'."
	tasks_qs = CareItem.objects.filter(patient=selected_patient_instance, item_type=CareItem.ItemType.TASK)
	routine_items = CareItem.objects.filter(patient=selected_patient_instance, item_type=CareItem.ItemType.ROUTINE)
	schedule_items = CareItem.objects.filter(patient=selected_patient_instance, item_type=CareItem.ItemType.SCHEDULE)
	task_items = tasks_qs
	total_tasks = tasks_qs.count()
	completed_tasks = tasks_qs.filter(status=CareItem.Status.DONE).count()
	missed_tasks = tasks_qs.filter(status__in=[CareItem.Status.PENDING, CareItem.Status.IN_PROGRESS], due_at__lt=timezone.now()).count()
	analysis_metrics = {
		"tasks_completed_percent": int((completed_tasks / total_tasks) * 100) if total_tasks else 0,
		"missed_tasks": missed_tasks,
		"assistance_triggered": max(missed_tasks, routine_items.count() // 2),
		"active_routines": routine_items.count(),
	}
	clinical_insight = f"Most common difficulty: {selected_patient['diseases'][0] if selected_patient['diseases'] else 'care coordination'} during morning meds."
	analysis_note = selected_patient["current_scenario"] or analysis_note
	return render(
		request,
		"dashboard/analysis.html",
		_dashboard_context(request,
			caretaker_profile,
			"progress",
			{
				"patients": patients,
				"selected_patient": selected_patient,
				"analysis_metrics": analysis_metrics,
				"analysis_trend": analysis_trend,
				"assistance_breakdown": assistance_breakdown,
				"activity_bars": activity_bars,
				"analysis_note": analysis_note,
				"clinical_insight": clinical_insight,
				"routine_items": routine_items,
				"schedule_items": schedule_items,
				"task_items": task_items,
			},
		),
	)


analysis = progress


@login_required
def profile_settings(request):
	profile = _get_or_create_caretaker_profile(request.user)
	if request.method == "POST" and request.POST.get("action") == "delete":
		profile.delete()
		messages.success(request, "Caretaker profile removed and reset to default.")
		return redirect("dashboard:profile_settings")

	if request.method == "POST":
		form = CaretakerProfileForm(request.POST, instance=profile)
		if form.is_valid():
			form.save()
			messages.success(request, "Profile settings updated.")
			return redirect("dashboard:profile_settings")
	else:
		form = CaretakerProfileForm(instance=profile)

	return render(
		request,
		"dashboard/profile_settings.html",
		_dashboard_context(request,
			profile,
			"preferences",
			{
				"form": form,
			},
		),
	)

from django.contrib.auth import logout

@login_required
def signout(request):
	logout(request)
	return redirect("home")

def register(request):
	if request.method == "POST":
		form = UserCreationForm(request.POST)
		if form.is_valid():
			user = form.save()
			login(request, user)
			return redirect("dashboard:home")
	else:
		form = UserCreationForm()
	return render(request, "registration/register.html", {"form": form})

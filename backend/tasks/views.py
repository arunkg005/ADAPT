from django.contrib.auth.decorators import login_required
from django.http import Http404
from django.shortcuts import get_object_or_404, redirect, render

from patients.models import Patient

from .forms import CareItemForm
from .models import CareItem


def _shell_context(request, extra=None):
	"""Build base context for tasks pages so they render inside the app shell."""
	from dashboard.views import _caretaker_context, _get_or_create_caretaker_profile
	profile = _get_or_create_caretaker_profile(request.user)
	context = {
		"caretaker": _caretaker_context(profile),
		"active_nav": "task-lab",
	}
	if extra:
		context.update(extra)
	return context


TYPE_META = {
	"task": {
		"label": "Tasks",
		"description": "General tasks with priorities, optional recurrence, and per-item reminders.",
	},
	"routine": {
		"label": "Routine",
		"description": "Daily timetable items with recurrence by daily or selected weekdays.",
	},
	"schedule": {
		"label": "Schedule",
		"description": "Occasional/deadline items with recurrence by dates, days, weekly, or monthly dates.",
	},
}


def _get_type_meta(item_type):
	if item_type not in TYPE_META:
		raise Http404("Invalid section")
	return TYPE_META[item_type]


@login_required
def index(request):
	return redirect("dashboard:home")


@login_required
def item_list(request, patient_id, item_type):
	meta = _get_type_meta(item_type)
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)

	if request.method == "POST" and item_type == CareItem.ItemType.ROUTINE:
		patient.routine_reminder_enabled = request.POST.get("routine_reminder_enabled") == "on"
		minutes = request.POST.get("routine_reminder_minutes_before", "").strip()
		if minutes.isdigit():
			patient.routine_reminder_minutes_before = int(minutes)
		patient.save(update_fields=["routine_reminder_enabled", "routine_reminder_minutes_before", "updated_at"])
		for item in CareItem.objects.filter(patient=patient, item_type=CareItem.ItemType.ROUTINE):
			item.reminder_enabled = patient.routine_reminder_enabled
			item.reminder_minutes_before = patient.routine_reminder_minutes_before if patient.routine_reminder_enabled else None
			item.save(update_fields=["reminder_enabled", "reminder_minutes_before", "updated_at"])
		return redirect("tasks:item_list", patient_id=patient.id, item_type=item_type)

	items = CareItem.objects.filter(patient=patient, item_type=item_type)
	return render(
		request,
		"tasks/item_list.html",
		_shell_context(request, {
			"patient": patient,
			"items": items,
			"item_type": item_type,
			"meta": meta,
			"routine_reminder_enabled": patient.routine_reminder_enabled,
			"routine_reminder_minutes_before": patient.routine_reminder_minutes_before,
		}),
	)


@login_required
def item_create(request, patient_id, item_type):
	meta = _get_type_meta(item_type)
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	if request.method == "POST":
		form = CareItemForm(request.POST, item_type=item_type, patient=patient)
		if form.is_valid():
			item = form.save(commit=False)
			item.patient = patient
			item.item_type = item_type
			if item_type == CareItem.ItemType.ROUTINE:
				item.reminder_enabled = patient.routine_reminder_enabled
				item.reminder_minutes_before = patient.routine_reminder_minutes_before if patient.routine_reminder_enabled else None
			item.save()
			return redirect("tasks:item_list", patient_id=patient.id, item_type=item_type)
	else:
		form = CareItemForm(item_type=item_type, patient=patient)

	return render(
		request,
		"tasks/item_form.html",
		_shell_context(request, {"patient": patient, "form": form, "item_type": item_type, "meta": meta, "mode": "create"}),
	)


@login_required
def item_edit(request, patient_id, item_type, item_id):
	meta = _get_type_meta(item_type)
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	item = get_object_or_404(CareItem, id=item_id, patient=patient, item_type=item_type)

	if request.method == "POST":
		form = CareItemForm(request.POST, instance=item, item_type=item_type, patient=patient)
		if form.is_valid():
			saved = form.save(commit=False)
			if item_type == CareItem.ItemType.ROUTINE:
				saved.reminder_enabled = patient.routine_reminder_enabled
				saved.reminder_minutes_before = patient.routine_reminder_minutes_before if patient.routine_reminder_enabled else None
			saved.save()
			return redirect("tasks:item_list", patient_id=patient.id, item_type=item_type)
	else:
		form = CareItemForm(instance=item, item_type=item_type, patient=patient)

	return render(
		request,
		"tasks/item_form.html",
		_shell_context(request, {
			"patient": patient,
			"form": form,
			"item_type": item_type,
			"meta": meta,
			"mode": "edit",
			"item": item,
		}),
	)


@login_required
def item_delete(request, patient_id, item_type, item_id):
	meta = _get_type_meta(item_type)
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	item = get_object_or_404(CareItem, id=item_id, patient=patient, item_type=item_type)

	if request.method == "POST":
		item.delete()
		return redirect("tasks:item_list", patient_id=patient.id, item_type=item_type)

	return render(
		request,
		"tasks/item_confirm_delete.html",
		_shell_context(request, {"patient": patient, "item": item, "item_type": item_type, "meta": meta}),
	)


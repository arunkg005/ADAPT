from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.db import transaction
from django.shortcuts import get_object_or_404, redirect, render

from dashboard.views import _caretaker_context, _get_or_create_caretaker_profile
from .forms import PatientCreateForm, PatientDiseaseForm, PatientMedicationForm, PatientSensitivityForm
from .models import ClinicalSource, Patient, PatientDisease, PatientDocument, PatientMedication, PatientSensitivity
from .services import extract_structured_clinical_data, generate_patient_summary


def _sidebar_patient_cards(user):
	patients = Patient.objects.filter(user=user).order_by("name")
	return [
		{
			"id": patient.id,
			"name": patient.name,
			"age": patient.age,
			"gender": patient.get_gender_display(),
			"diseases": list(patient.history_diseases or []),
			"allergies": patient.allergies,
		} for patient in patients
	]


def _shell_context(request, extra=None):
	profile = _get_or_create_caretaker_profile(request.user)
	context = {
		"caretaker": _caretaker_context(profile),
		"sidebar_patients": _sidebar_patient_cards(request.user),
		"active_nav": "patients",
	}
	if extra:
		context.update(extra)
	return context

@login_required
def index(request):
	patients = Patient.objects.filter(user=request.user).order_by("-created_at")
	return render(request, "patients/index.html", _shell_context(request, {"patients": patients}))


@login_required
def add_patient(request):
	if request.method == "POST":
		form = PatientCreateForm(request.POST, request.FILES)
		if form.is_valid():
			with transaction.atomic():
				patient = form.save(commit=False)
				patient.user = request.user
				patient.history_diseases = form.get_disease_history()
				patient.save()
				_sync_manual_clinical_entries(patient)

				category_map = {
					"report_docs": PatientDocument.Category.REPORT,
					"prescription_docs": PatientDocument.Category.PRESCRIPTION,
					"medication_docs": PatientDocument.Category.MEDICATION,
				}
				for field_name, category in category_map.items():
					for uploaded_file in request.FILES.getlist(field_name):
						PatientDocument.objects.create(
							patient=patient,
							category=category,
							file=uploaded_file,
						)

				documents = list(patient.documents.all())
				patient.ai_summary = generate_patient_summary(patient, documents)
				_save_extracted_clinical_data(patient, extract_structured_clinical_data(patient, documents))
				patient.save(update_fields=["ai_summary", "updated_at", "history_diseases", "allergies", "intolerances"])

			messages.success(request, "Patient profile created and AI summary saved.")
			return redirect("dashboard:patient_dashboard", patient_id=patient.id)
	else:
		form = PatientCreateForm()

	return render(request, "patients/add_patient.html", _shell_context(request, {"form": form}))


@login_required
def disease_management(request, patient_id):
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	disease_form = PatientDiseaseForm(prefix="disease")
	sensitivity_form = PatientSensitivityForm(prefix="sensitivity")

	if request.method == "POST":
		action = request.POST.get("action")
		if action == "add_disease":
			disease_form = PatientDiseaseForm(request.POST, prefix="disease")
			if disease_form.is_valid():
				item = disease_form.save(commit=False)
				item.patient = patient
				item.source = ClinicalSource.MANUAL
				item.save()
				_sync_patient_summary_fields_from_entries(patient)
				messages.success(request, "Disease added.")
				return redirect("patients:disease_management", patient_id=patient.id)

		if action == "add_sensitivity":
			sensitivity_form = PatientSensitivityForm(request.POST, prefix="sensitivity")
			if sensitivity_form.is_valid():
				item = sensitivity_form.save(commit=False)
				item.patient = patient
				item.source = ClinicalSource.MANUAL
				item.save()
				_sync_patient_summary_fields_from_entries(patient)
				messages.success(request, "Allergy/Intolerance added.")
				return redirect("patients:disease_management", patient_id=patient.id)

		if action == "analyze_disease_docs":
			data = extract_structured_clinical_data(patient, list(patient.documents.all()))
			_save_extracted_clinical_data(patient, data)
			messages.success(request, "AI analysis completed for diseases/allergies/intolerances.")
			return redirect("patients:disease_management", patient_id=patient.id)

	return render(
		request,
		"patients/disease_management.html",
		_shell_context(request, {
			"patient": patient,
			"disease_form": disease_form,
			"sensitivity_form": sensitivity_form,
			"diseases": patient.disease_entries.all(),
			"sensitivities": patient.sensitivity_entries.all(),
		}),
	)


@login_required
def medication_management(request, patient_id):
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	medication_form = PatientMedicationForm(prefix="med")

	if request.method == "POST":
		action = request.POST.get("action")
		if action == "add_medication":
			medication_form = PatientMedicationForm(request.POST, prefix="med")
			if medication_form.is_valid():
				item = medication_form.save(commit=False)
				item.patient = patient
				item.source = ClinicalSource.MANUAL
				item.save()
				messages.success(request, "Medication added.")
				return redirect("patients:medication_management", patient_id=patient.id)

		if action == "analyze_medication_docs":
			data = extract_structured_clinical_data(patient, list(patient.documents.all()))
			_save_extracted_clinical_data(patient, data)
			messages.success(request, "AI analysis completed for medications.")
			return redirect("patients:medication_management", patient_id=patient.id)

	return render(
		request,
		"patients/medication_management.html",
		_shell_context(request, {
			"patient": patient,
			"medication_form": medication_form,
			"medications": patient.medication_entries.all(),
		}),
	)


@login_required
def disease_edit(request, patient_id, disease_id):
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	disease = get_object_or_404(PatientDisease, id=disease_id, patient=patient)

	if request.method == "POST":
		form = PatientDiseaseForm(request.POST, instance=disease)
		if form.is_valid():
			form.save()
			_sync_patient_summary_fields_from_entries(patient)
			messages.success(request, "Disease updated.")
			return redirect("patients:disease_management", patient_id=patient.id)
	else:
		form = PatientDiseaseForm(instance=disease)

	return render(
		request,
		"patients/clinical_item_form.html",
		_shell_context(request, {"patient": patient, "form": form, "title": "Edit Disease", "cancel_url": "patients:disease_management"}),
	)


@login_required
def disease_delete(request, patient_id, disease_id):
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	disease = get_object_or_404(PatientDisease, id=disease_id, patient=patient)

	if request.method == "POST":
		disease.delete()
		_sync_patient_summary_fields_from_entries(patient)
		messages.success(request, "Disease deleted.")
		return redirect("patients:disease_management", patient_id=patient.id)

	return render(
		request,
		"patients/clinical_item_delete.html",
		_shell_context(request, {"patient": patient, "item": disease, "title": "Delete Disease", "cancel_url": "patients:disease_management"}),
	)


@login_required
def sensitivity_edit(request, patient_id, sensitivity_id):
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	sensitivity = get_object_or_404(PatientSensitivity, id=sensitivity_id, patient=patient)

	if request.method == "POST":
		form = PatientSensitivityForm(request.POST, instance=sensitivity)
		if form.is_valid():
			form.save()
			_sync_patient_summary_fields_from_entries(patient)
			messages.success(request, "Allergy/Intolerance updated.")
			return redirect("patients:disease_management", patient_id=patient.id)
	else:
		form = PatientSensitivityForm(instance=sensitivity)

	return render(
		request,
		"patients/clinical_item_form.html",
		_shell_context(request, {"patient": patient, "form": form, "title": "Edit Allergy/Intolerance", "cancel_url": "patients:disease_management"}),
	)


@login_required
def sensitivity_delete(request, patient_id, sensitivity_id):
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	sensitivity = get_object_or_404(PatientSensitivity, id=sensitivity_id, patient=patient)

	if request.method == "POST":
		sensitivity.delete()
		_sync_patient_summary_fields_from_entries(patient)
		messages.success(request, "Allergy/Intolerance deleted.")
		return redirect("patients:disease_management", patient_id=patient.id)

	return render(
		request,
		"patients/clinical_item_delete.html",
		_shell_context(request, {"patient": patient, "item": sensitivity, "title": "Delete Allergy/Intolerance", "cancel_url": "patients:disease_management"}),
	)


@login_required
def medication_edit(request, patient_id, medication_id):
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	medication = get_object_or_404(PatientMedication, id=medication_id, patient=patient)

	if request.method == "POST":
		form = PatientMedicationForm(request.POST, instance=medication)
		if form.is_valid():
			form.save()
			messages.success(request, "Medication updated.")
			return redirect("patients:medication_management", patient_id=patient.id)
	else:
		form = PatientMedicationForm(instance=medication)

	return render(
		request,
		"patients/clinical_item_form.html",
		_shell_context(request, {"patient": patient, "form": form, "title": "Edit Medication", "cancel_url": "patients:medication_management"}),
	)


@login_required
def medication_delete(request, patient_id, medication_id):
	patient = get_object_or_404(Patient, id=patient_id, user=request.user)
	medication = get_object_or_404(PatientMedication, id=medication_id, patient=patient)

	if request.method == "POST":
		medication.delete()
		messages.success(request, "Medication deleted.")
		return redirect("patients:medication_management", patient_id=patient.id)

	return render(
		request,
		"patients/clinical_item_delete.html",
		_shell_context(request, {"patient": patient, "item": medication, "title": "Delete Medication", "cancel_url": "patients:medication_management"}),
	)


def _sync_manual_clinical_entries(patient):
	for disease in patient.history_diseases:
		PatientDisease.objects.get_or_create(
			patient=patient,
			name=disease,
			defaults={"source": ClinicalSource.MANUAL},
		)

	for allergy in [x.strip() for x in (patient.allergies or "").split(",") if x.strip()]:
		PatientSensitivity.objects.get_or_create(
			patient=patient,
			kind=PatientSensitivity.Kind.ALLERGY,
			name=allergy,
			defaults={"source": ClinicalSource.MANUAL},
		)

	for intolerance in [x.strip() for x in (patient.intolerances or "").split(",") if x.strip()]:
		PatientSensitivity.objects.get_or_create(
			patient=patient,
			kind=PatientSensitivity.Kind.INTOLERANCE,
			name=intolerance,
			defaults={"source": ClinicalSource.MANUAL},
		)


def _save_extracted_clinical_data(patient, data):
	for disease in data.get("diseases", []):
		name = str(disease).strip()
		if not name:
			continue
		PatientDisease.objects.get_or_create(
			patient=patient,
			name=name,
			defaults={"source": ClinicalSource.AI_IMAGE},
		)

	for med in data.get("medications", []):
		if isinstance(med, dict):
			name = str(med.get("name") or "").strip()
			dosage = str(med.get("dosage") or "").strip()
			instructions = str(med.get("instructions") or "").strip()
		else:
			name = str(med).strip()
			dosage = ""
			instructions = ""

		if not name:
			continue
		PatientMedication.objects.get_or_create(
			patient=patient,
			name=name,
			dosage=dosage,
			defaults={"instructions": instructions, "source": ClinicalSource.AI_IMAGE},
		)

	for allergy in data.get("allergies", []):
		name = str(allergy).strip()
		if not name:
			continue
		PatientSensitivity.objects.get_or_create(
			patient=patient,
			kind=PatientSensitivity.Kind.ALLERGY,
			name=name,
			defaults={"source": ClinicalSource.AI_IMAGE},
		)

	for intolerance in data.get("intolerances", []):
		name = str(intolerance).strip()
		if not name:
			continue
		PatientSensitivity.objects.get_or_create(
			patient=patient,
			kind=PatientSensitivity.Kind.INTOLERANCE,
			name=name,
			defaults={"source": ClinicalSource.AI_IMAGE},
		)

	_sync_patient_summary_fields_from_entries(patient)


def _sync_patient_summary_fields_from_entries(patient):
	patient.history_diseases = list(patient.disease_entries.values_list("name", flat=True).distinct())
	patient.allergies = ", ".join(
		patient.sensitivity_entries.filter(kind=PatientSensitivity.Kind.ALLERGY).values_list("name", flat=True)
	)
	patient.intolerances = ", ".join(
		patient.sensitivity_entries.filter(kind=PatientSensitivity.Kind.INTOLERANCE).values_list("name", flat=True)
	)
	patient.save(update_fields=["history_diseases", "allergies", "intolerances", "updated_at"])


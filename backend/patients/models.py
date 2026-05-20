from django.db import models
from django.conf import settings


class Patient(models.Model):
	class Gender(models.TextChoices):
		MALE = "male", "Male"
		FEMALE = "female", "Female"
		OTHER = "other", "Other"

	name = models.CharField(max_length=150)
	user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="patients", null=True, blank=True)
	age = models.PositiveIntegerField()
	gender = models.CharField(max_length=10, choices=Gender.choices)
	allergies = models.TextField()
	intolerances = models.TextField(blank=True)
	history_diseases = models.JSONField(default=list)
	current_scenario_description = models.TextField()
	doctor_guidelines = models.TextField(blank=True)
	routine_reminder_enabled = models.BooleanField(default=False)
	routine_reminder_minutes_before = models.PositiveIntegerField(default=30)
	ai_summary = models.TextField(blank=True)
	created_at = models.DateTimeField(auto_now_add=True)
	updated_at = models.DateTimeField(auto_now=True)

	def __str__(self):
		return self.name


class ClinicalSource(models.TextChoices):
	MANUAL = "manual", "Manual"
	AI_IMAGE = "ai_image", "AI Image Analysis"


class PatientDisease(models.Model):
	patient = models.ForeignKey(Patient, on_delete=models.CASCADE, related_name="disease_entries")
	name = models.CharField(max_length=200)
	notes = models.TextField(blank=True)
	source = models.CharField(max_length=20, choices=ClinicalSource.choices, default=ClinicalSource.MANUAL)
	created_at = models.DateTimeField(auto_now_add=True)

	class Meta:
		ordering = ["name"]
		constraints = [
			models.UniqueConstraint(fields=["patient", "name"], name="uq_patient_disease_name"),
		]


class PatientMedication(models.Model):
	patient = models.ForeignKey(Patient, on_delete=models.CASCADE, related_name="medication_entries")
	name = models.CharField(max_length=200)
	dosage = models.CharField(max_length=120, blank=True)
	instructions = models.CharField(max_length=200, blank=True)
	notes = models.TextField(blank=True)
	source = models.CharField(max_length=20, choices=ClinicalSource.choices, default=ClinicalSource.MANUAL)
	created_at = models.DateTimeField(auto_now_add=True)

	class Meta:
		ordering = ["name"]
		constraints = [
			models.UniqueConstraint(fields=["patient", "name", "dosage"], name="uq_patient_medication_name_dosage"),
		]


class PatientSensitivity(models.Model):
	class Kind(models.TextChoices):
		ALLERGY = "allergy", "Allergy"
		INTOLERANCE = "intolerance", "Intolerance"

	patient = models.ForeignKey(Patient, on_delete=models.CASCADE, related_name="sensitivity_entries")
	name = models.CharField(max_length=200)
	kind = models.CharField(max_length=20, choices=Kind.choices)
	severity = models.CharField(max_length=80, blank=True)
	notes = models.TextField(blank=True)
	source = models.CharField(max_length=20, choices=ClinicalSource.choices, default=ClinicalSource.MANUAL)
	created_at = models.DateTimeField(auto_now_add=True)

	class Meta:
		ordering = ["kind", "name"]
		constraints = [
			models.UniqueConstraint(fields=["patient", "kind", "name"], name="uq_patient_sensitivity_kind_name"),
		]


class PatientDocument(models.Model):
	class Category(models.TextChoices):
		REPORT = "report", "Report"
		PRESCRIPTION = "prescription", "Prescription"
		MEDICATION = "medication", "Medication"

	patient = models.ForeignKey(Patient, on_delete=models.CASCADE, related_name="documents")
	category = models.CharField(max_length=20, choices=Category.choices)
	file = models.FileField(upload_to="patient_docs/%Y/%m/%d")
	uploaded_at = models.DateTimeField(auto_now_add=True)

	def __str__(self):
		return f"{self.patient.name} - {self.category}"

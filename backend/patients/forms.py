from django import forms

from .models import Patient, PatientDisease, PatientMedication, PatientSensitivity


COMMON_DISEASE_OPTIONS = [
    ("Hypertension", "Hypertension (BP)"),
    ("Type 2 Diabetes", "Diabetes"),
    ("Asthma", "Asthma"),
    ("Arthritis", "Arthritis"),
    ("Thyroid Disorder", "Thyroid Disorder"),
    ("Parkinsonism", "Parkinsonism"),
]


class MultipleFileInput(forms.ClearableFileInput):
    allow_multiple_selected = True


class MultipleFileField(forms.FileField):
    widget = MultipleFileInput

    def clean(self, data, initial=None):
        single_file_clean = super().clean
        if not data:
            return []
        if isinstance(data, (list, tuple)):
            return [single_file_clean(d, initial) for d in data]
        return [single_file_clean(data, initial)]


class PatientCreateForm(forms.ModelForm):
    common_diseases = forms.MultipleChoiceField(
        choices=COMMON_DISEASE_OPTIONS,
        required=False,
        widget=forms.CheckboxSelectMultiple,
        label="Common disease history",
    )
    custom_diseases = forms.CharField(
        required=False,
        help_text="Add other diseases separated by commas.",
        label="Other disease history",
    )
    report_docs = MultipleFileField(required=True, label="Reports (images/docs)")
    prescription_docs = MultipleFileField(required=True, label="Prescriptions (images/docs)")
    medication_docs = MultipleFileField(required=True, label="Ongoing medication docs")

    class Meta:
        model = Patient
        fields = [
            "name",
            "age",
            "gender",
            "allergies",
            "intolerances",
            "current_scenario_description",
            "doctor_guidelines",
        ]
        labels = {
            "current_scenario_description": "Current scenario description",
            "doctor_guidelines": "Doctor guidelines (optional)",
            "intolerances": "Intolerances (optional)",
        }

    def clean(self):
        cleaned_data = super().clean()
        common = cleaned_data.get("common_diseases") or []
        custom_raw = cleaned_data.get("custom_diseases", "")
        custom = [item.strip() for item in custom_raw.split(",") if item.strip()]

        if not common and not custom:
            self.add_error(
                "custom_diseases",
                "Disease history is mandatory. Select from list and/or add custom diseases.",
            )

        files_to_check = ["report_docs", "prescription_docs", "medication_docs"]
        for field_name in files_to_check:
            files = self.files.getlist(field_name)
            if not files:
                self.add_error(field_name, "This upload is mandatory.")

        return cleaned_data

    def get_disease_history(self):
        common = self.cleaned_data.get("common_diseases") or []
        custom_raw = self.cleaned_data.get("custom_diseases", "")
        custom = [item.strip() for item in custom_raw.split(",") if item.strip()]
        merged = common + [item for item in custom if item not in common]
        return merged


class PatientDiseaseForm(forms.ModelForm):
    class Meta:
        model = PatientDisease
        fields = ["name", "notes"]


class PatientMedicationForm(forms.ModelForm):
    class Meta:
        model = PatientMedication
        fields = ["name", "dosage", "instructions", "notes"]


class PatientSensitivityForm(forms.ModelForm):
    class Meta:
        model = PatientSensitivity
        fields = ["name", "kind", "severity", "notes"]


class PatientSettingsForm(forms.ModelForm):
    history_diseases_csv = forms.CharField(
        required=False,
        label="Disease history",
        help_text="Comma-separated diseases/conditions.",
    )

    class Meta:
        model = Patient
        fields = [
            "name",
            "age",
            "gender",
            "allergies",
            "intolerances",
            "current_scenario_description",
            "doctor_guidelines",
            "routine_reminder_enabled",
            "routine_reminder_minutes_before",
            "ai_summary",
        ]

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        if self.instance and self.instance.pk:
            self.fields["history_diseases_csv"].initial = ", ".join(self.instance.history_diseases or [])

    def clean_history_diseases_csv(self):
        raw = self.cleaned_data.get("history_diseases_csv", "")
        return [item.strip() for item in raw.split(",") if item.strip()]

    def save(self, commit=True):
        patient = super().save(commit=False)
        patient.history_diseases = self.cleaned_data.get("history_diseases_csv", [])
        if commit:
            patient.save()
        return patient

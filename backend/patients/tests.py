from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import TestCase

from .models import Patient
from .services import extract_structured_clinical_data, generate_patient_summary


User = get_user_model()


class PatientServicesFallbackTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(username="caretaker", password="StrongPass!99")
        self.patient = Patient.objects.create(
            name="Fallback Patient",
            user=self.user,
            age=67,
            gender=Patient.Gender.FEMALE,
            allergies="Penicillin",
            intolerances="Lactose",
            history_diseases=["Hypertension"],
            current_scenario_description="Needs routine monitoring.",
        )

    @patch("patients.services.genai", None)
    @patch.dict("os.environ", {"GEMINI_API_KEY": "test-key"}, clear=False)
    def test_generate_patient_summary_falls_back_when_sdk_unavailable(self):
        summary = generate_patient_summary(self.patient, [])
        self.assertIn("Gemini is unavailable", summary)
        self.assertIn("Fallback Patient", summary)

    @patch("patients.services.genai", None)
    @patch.dict("os.environ", {"GEMINI_API_KEY": "test-key"}, clear=False)
    def test_extract_structured_clinical_data_falls_back_when_sdk_unavailable(self):
        data = extract_structured_clinical_data(self.patient, [])
        self.assertEqual(data["diseases"], ["Hypertension"])
        self.assertEqual(data["allergies"], ["Penicillin"])
        self.assertEqual(data["intolerances"], ["Lactose"])

from django.test import TestCase
from django.urls import reverse
from django.contrib.auth import get_user_model
from django.utils import timezone

from patients.models import Patient, PatientDisease, PatientMedication, PatientSensitivity
from assistant.models import ChatSession, ChatMessage, ChatActionProposal
from assistant.services import _build_context_block, apply_action_proposal


class AssistantViewTests(TestCase):
    def setUp(self):
        self.User = get_user_model()
        self.user = self.User.objects.create_user(username='testcare', password='pwd123password')
        self.patient = Patient.objects.create(
            name="John Doe",
            age=45,
            gender=Patient.Gender.MALE,
            allergies="Peanuts",
            current_scenario_description="Recovering from hip surgery",
            user=self.user
        )

    def test_assistant_index_requires_login(self):
        # Unauthenticated access should redirect to login
        response = self.client.get(reverse('assistant:index'))
        self.assertEqual(response.status_code, 302)
        self.assertTrue(response.url.startswith('/accounts/login/'))

    def test_assistant_index_authenticated_success(self):
        # Authenticated access should render successfully
        self.client.login(username='testcare', password='pwd123password')
        response = self.client.get(reverse('assistant:index'))
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, "assistant/chat.html")

    def test_build_context_block_patient_details(self):
        # Create diseases, medications, sensitivities for the patient
        PatientDisease.objects.create(patient=self.patient, name="Hypertension", notes="Under control")
        PatientMedication.objects.create(patient=self.patient, name="Lisinopril", dosage="10mg", instructions="Daily")
        PatientSensitivity.objects.create(patient=self.patient, name="Penicillin", kind=PatientSensitivity.Kind.ALLERGY)

        session = ChatSession.objects.create(source=ChatSession.Source.CARETAKER, patient=self.patient)
        
        context = _build_context_block(session, self.patient, [])
        self.assertIn("John Doe", context)
        self.assertIn("Hypertension", context)
        self.assertIn("Lisinopril", context)
        self.assertIn("Penicillin", context)
        self.assertIn("Recovering from hip surgery", context)

    def test_apply_action_proposal_summary(self):
        payload = {
            "operation": "update",
            "item_type": "summary",
            "summary_text": "This is a newly generated AI summary."
        }
        res = apply_action_proposal(self.patient, payload)
        self.assertEqual(res, "update")
        
        self.patient.refresh_from_db()
        self.assertEqual(self.patient.ai_summary, "This is a newly generated AI summary.")

    def test_proposal_resolution_flow_for_summary(self):
        self.client.login(username='testcare', password='pwd123password')
        session = ChatSession.objects.create(source=ChatSession.Source.CARETAKER, patient=self.patient)
        proposal = ChatActionProposal.objects.create(
            session=session,
            patient=self.patient,
            item_type="summary",
            operation="update",
            title="Update Patient Summary",
            payload={
                "operation": "update",
                "item_type": "summary",
                "summary_text": "Approved AI summary text"
            }
        )

        url = reverse('assistant:index') + f"?session={session.id}"
        response = self.client.post(url, {
            "action": "approve_action",
            "proposal_id": proposal.id
        })
        self.assertEqual(response.status_code, 302)

        # Verify proposal status changed to approved
        proposal.refresh_from_db()
        self.assertEqual(proposal.status, ChatActionProposal.Status.APPROVED)

        # Verify summary was applied to patient
        self.patient.refresh_from_db()
        self.assertEqual(self.patient.ai_summary, "Approved AI summary text")



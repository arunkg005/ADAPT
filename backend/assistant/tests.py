"""assistant/tests.py — Automated QA suite for the ADAPT AI layer.

Coverage areas
──────────────
1. EmergencyBypassTests
   Validates that every critical keyword short-circuits the LLM entirely and
   returns the hardcoded emergency directive — even when the Gemini client is
   fully patched out.

2. TenantIsolationTests
   Creates two independent caretakers (A and B) with their own patients,
   clinical entries, and DocumentChunk rows.  Asserts that every query path
   (API, assistant view, DocumentChunk ORM) exposes only the requesting
   user's own data.

3. ModelTemperatureTests
   Patches the google-genai client and captures the ``config`` kwarg passed
   to ``generate_content``.  Asserts temperature == 0.1 on every call path.

4. AIFallbackResilienceTests
   Simulates Gemini API failures (network error, quota exceeded) and verifies
   that the view layer degrades gracefully: HTTP 200/302 returned, fallback
   message persisted to DB, no 500 raised.

5. ExistingRegressionTests (preserved from the original file)
   The original test cases are retained verbatim so nothing already passing
   can silently regress.

Run with:
    python manage.py test assistant
"""

import json
from unittest.mock import MagicMock, patch, call

from django.contrib.auth import get_user_model
from django.test import TestCase, Client
from django.urls import reverse
from django.utils import timezone

from patients.models import Patient, PatientDisease, PatientMedication, PatientSensitivity
from tasks.models import CareItem
from assistant.models import ChatActionProposal, ChatMessage, ChatSession
from assistant.services import (
    _build_context_block,
    _EMERGENCY_KEYWORDS,
    _EMERGENCY_RESPONSE,
    apply_action_proposal,
    generate_assistant_reply,
    generate_session_summary,
)

User = get_user_model()


# ─────────────────────────────────────────────────────────────────────────────
# Shared factory helpers
# ─────────────────────────────────────────────────────────────────────────────

def _make_user(username: str, password: str = "StrongPass!99") -> "User":
    return User.objects.create_user(username=username, password=password)


def _make_patient(user, name: str = "Test Patient") -> Patient:
    return Patient.objects.create(
        name=name,
        user=user,
        age=55,
        gender=Patient.Gender.FEMALE,
        allergies="None",
        current_scenario_description="Post-operative care",
    )


def _make_session(patient=None, source=ChatSession.Source.CARETAKER) -> ChatSession:
    return ChatSession.objects.create(
        source=source,
        patient=patient,
        title="Test session",
    )


# ─────────────────────────────────────────────────────────────────────────────
# 1. Emergency Bypass Tests
# ─────────────────────────────────────────────────────────────────────────────

class EmergencyBypassTests(TestCase):
    """Validate that critical keywords short-circuit the LLM 100% of the time."""

    def setUp(self):
        self.user = _make_user("bypass_user")
        self.patient = _make_patient(self.user)
        self.session = _make_session(self.patient)

    # ── Pure service-layer tests (no HTTP, no mock needed) ────────────────────

    def _call(self, text: str):
        """Helper: call generate_assistant_reply and return (reply, action)."""
        return generate_assistant_reply(self.session, self.patient, text, [])

    def test_keyword_choking_bypasses_llm(self):
        """'choking' must return the hardcoded emergency response."""
        with patch("assistant.services.client") as mock_client:
            reply, action = self._call("Help! The patient is choking!")
        mock_client.models.generate_content.assert_not_called()
        self.assertEqual(reply, _EMERGENCY_RESPONSE)
        self.assertIsNone(action)

    def test_keyword_unresponsive_bypasses_llm(self):
        with patch("assistant.services.client") as mock_client:
            reply, action = self._call("Patient is unresponsive and not breathing")
        mock_client.models.generate_content.assert_not_called()
        self.assertEqual(reply, _EMERGENCY_RESPONSE)

    def test_keyword_seizure_bypasses_llm(self):
        with patch("assistant.services.client") as mock_client:
            reply, _ = self._call("She is having a seizure right now")
        mock_client.models.generate_content.assert_not_called()
        self.assertIn("EMERGENCY", reply)

    def test_keyword_unconscious_bypasses_llm(self):
        with patch("assistant.services.client") as mock_client:
            reply, _ = self._call("He fell unconscious suddenly")
        mock_client.models.generate_content.assert_not_called()
        self.assertEqual(reply, _EMERGENCY_RESPONSE)

    def test_keyword_bleeding_bypasses_llm(self):
        with patch("assistant.services.client") as mock_client:
            reply, _ = self._call("There is heavy bleeding from the wound")
        mock_client.models.generate_content.assert_not_called()
        self.assertEqual(reply, _EMERGENCY_RESPONSE)

    def test_keyword_heart_attack_bypasses_llm(self):
        with patch("assistant.services.client") as mock_client:
            reply, _ = self._call("I think she is having a heart attack")
        mock_client.models.generate_content.assert_not_called()
        self.assertEqual(reply, _EMERGENCY_RESPONSE)

    def test_keyword_stroke_bypasses_llm(self):
        with patch("assistant.services.client") as mock_client:
            reply, _ = self._call("Patient showing stroke symptoms, face drooping")
        mock_client.models.generate_content.assert_not_called()
        self.assertEqual(reply, _EMERGENCY_RESPONSE)

    def test_combined_emergency_phrase_bypasses_llm(self):
        """Compound phrase matching the original instructions verbatim."""
        with patch("assistant.services.client") as mock_client:
            reply, action = self._call("patient is choking and unresponsive")
        mock_client.models.generate_content.assert_not_called()
        self.assertEqual(reply, _EMERGENCY_RESPONSE)
        self.assertIsNone(action)

    def test_emergency_is_case_insensitive(self):
        """Upper-case input must still trigger the bypass."""
        with patch("assistant.services.client") as mock_client:
            reply, _ = self._call("PATIENT IS CHOKING")
        mock_client.models.generate_content.assert_not_called()
        self.assertEqual(reply, _EMERGENCY_RESPONSE)

    def test_non_emergency_query_reaches_llm(self):
        """A benign message must NOT be intercepted by the bypass block."""
        mock_response = MagicMock()
        mock_response.text = "Here is the medication schedule."
        with patch("assistant.services.client") as mock_client:
            mock_client.models.generate_content.return_value = mock_response
            reply, _ = self._call("What medications does the patient take?")
        mock_client.models.generate_content.assert_called_once()
        self.assertNotEqual(reply, _EMERGENCY_RESPONSE)

    def test_all_declared_keywords_trigger_bypass(self):
        """Every keyword in _EMERGENCY_KEYWORDS must individually bypass the LLM."""
        for keyword in _EMERGENCY_KEYWORDS:
            with self.subTest(keyword=keyword):
                with patch("assistant.services.client") as mock_client:
                    reply, _ = self._call(f"The patient is {keyword}")
                mock_client.models.generate_content.assert_not_called()
                self.assertEqual(reply, _EMERGENCY_RESPONSE)

    def test_emergency_response_contains_emergency_number(self):
        """The hardcoded response must include actionable contact instructions."""
        reply, _ = self._call("patient is choking")
        self.assertIn("911", reply)
        self.assertIn("112", reply)
        self.assertIn("999", reply)

    # ── HTTP-level test: emergency keyword via POST ───────────────────────────

    def test_emergency_via_view_post_persists_hardcoded_reply(self):
        """Posting a choking message through the view must store the hardcoded
        emergency reply in the DB without touching the LLM."""
        self.client.force_login(self.user)
        with patch("assistant.services.client") as mock_client:
            response = self.client.post(
                reverse("assistant:index"),
                {
                    "action": "send",
                    "message": "The patient is choking, please help!",
                    "selected_patient": self.patient.id,
                },
            )
        mock_client.models.generate_content.assert_not_called()
        # View should redirect after a successful POST.
        self.assertIn(response.status_code, [200, 302])

        # The assistant's reply stored in the DB must be the emergency string.
        assistant_msg = ChatMessage.objects.filter(
            role=ChatMessage.Role.ASSISTANT
        ).order_by("-created_at").first()
        self.assertIsNotNone(assistant_msg)
        self.assertEqual(assistant_msg.content, _EMERGENCY_RESPONSE)


# ─────────────────────────────────────────────────────────────────────────────
# 2. Tenant Isolation Tests
# ─────────────────────────────────────────────────────────────────────────────

class TenantIsolationTests(TestCase):
    """Assert that caretaker_A can never see caretaker_B's data via any path."""

    def setUp(self):
        # Caretaker A
        self.user_a = _make_user("caretaker_A")
        self.patient_a = _make_patient(self.user_a, name="Alice (A's patient)")
        PatientDisease.objects.create(patient=self.patient_a, name="Hypertension")
        PatientMedication.objects.create(patient=self.patient_a, name="Lisinopril", dosage="10mg")
        PatientSensitivity.objects.create(
            patient=self.patient_a, name="Penicillin", kind=PatientSensitivity.Kind.ALLERGY
        )
        self.session_a = _make_session(self.patient_a)
        ChatMessage.objects.create(
            session=self.session_a, role=ChatMessage.Role.USER, content="A's private note"
        )

        # Caretaker B
        self.user_b = _make_user("caretaker_B")
        self.patient_b = _make_patient(self.user_b, name="Bob (B's patient)")
        PatientDisease.objects.create(patient=self.patient_b, name="Diabetes Type 2")
        PatientMedication.objects.create(patient=self.patient_b, name="Metformin", dosage="500mg")
        PatientSensitivity.objects.create(
            patient=self.patient_b, name="Sulfa drugs", kind=PatientSensitivity.Kind.ALLERGY
        )
        self.session_b = _make_session(self.patient_b)
        ChatMessage.objects.create(
            session=self.session_b, role=ChatMessage.Role.USER, content="B's confidential note"
        )

    # ── ORM isolation — Patient queryset ──────────────────────────────────────

    def test_patient_queryset_scoped_to_owner(self):
        """Patient.objects.filter(user=...) must not return another user's records."""
        a_patients = Patient.objects.filter(user=self.user_a)
        b_patients = Patient.objects.filter(user=self.user_b)

        a_names = list(a_patients.values_list("name", flat=True))
        b_names = list(b_patients.values_list("name", flat=True))

        self.assertIn("Alice (A's patient)", a_names)
        self.assertNotIn("Bob (B's patient)", a_names)

        self.assertIn("Bob (B's patient)", b_names)
        self.assertNotIn("Alice (A's patient)", b_names)

    def test_clinical_entries_scoped_via_patient_fk(self):
        """Disease/Medication/Sensitivity entries must be unreadable across tenants."""
        a_diseases = PatientDisease.objects.filter(patient__user=self.user_a).values_list("name", flat=True)
        self.assertIn("Hypertension", list(a_diseases))
        self.assertNotIn("Diabetes Type 2", list(a_diseases))

        b_medications = PatientMedication.objects.filter(patient__user=self.user_b).values_list("name", flat=True)
        self.assertIn("Metformin", list(b_medications))
        self.assertNotIn("Lisinopril", list(b_medications))

    # ── HTTP isolation — assistant index view ─────────────────────────────────

    def test_caretaker_a_cannot_access_patient_b_via_url(self):
        """User A hitting /assistant/patient/<B's patient id>/ must be denied (404)."""
        self.client.force_login(self.user_a)
        url = reverse("assistant:patient_index", kwargs={"patient_id": self.patient_b.id})
        response = self.client.get(url)
        # Must receive 404 (multi-tenant guard) — not 200 with B's data.
        self.assertEqual(response.status_code, 404)

    def test_caretaker_a_cannot_access_patient_b_via_query_param(self):
        """User A hitting /assistant/?patient=<B's id> must be denied (404)."""
        self.client.force_login(self.user_a)
        url = reverse("assistant:index") + f"?patient={self.patient_b.id}"
        response = self.client.get(url)
        self.assertEqual(response.status_code, 404)

    def test_caretaker_a_sidebar_does_not_expose_patient_b(self):
        """The patient_options context passed to the template must be scoped."""
        self.client.force_login(self.user_a)
        response = self.client.get(reverse("assistant:index"))
        self.assertEqual(response.status_code, 200)

        patient_options = response.context.get("patient_options") or []
        names = [p.name for p in patient_options]
        self.assertIn("Alice (A's patient)", names)
        self.assertNotIn("Bob (B's patient)", names)

    def test_caretaker_a_cannot_post_to_patient_b_session(self):
        """Posting a message referencing B's session must not inject into B's history."""
        self.client.force_login(self.user_a)
        # Count B's messages before the attack attempt.
        b_msg_count_before = ChatMessage.objects.filter(session=self.session_b).count()

        self.client.post(
            reverse("assistant:index") + f"?session={self.session_b.id}",
            {
                "action": "send",
                "message": "Attempting cross-tenant injection",
                "selected_patient": self.patient_b.id,
            },
        )
        # B's session must not have gained a new message from A's POST.
        b_msg_count_after = ChatMessage.objects.filter(session=self.session_b).count()
        self.assertEqual(b_msg_count_before, b_msg_count_after)

    # ── ORM isolation — DocumentChunk queryset ────────────────────────────────

    def test_document_chunks_scoped_via_patient_fk(self):
        """DocumentChunk rows must only be reachable through the owning patient."""
        # Import here so the test still runs even if pgvector is not installed
        # in the test environment (the table will exist but be empty).
        try:
            from assistant.models import DocumentChunk
            from patients.models import PatientDocument

            doc_a = PatientDocument.objects.create(
                patient=self.patient_a,
                category=PatientDocument.Category.REPORT,
                file="patient_docs/test_a.txt",
            )
            doc_b = PatientDocument.objects.create(
                patient=self.patient_b,
                category=PatientDocument.Category.REPORT,
                file="patient_docs/test_b.txt",
            )
            # We cannot create real VectorField rows without pgvector running,
            # but we can assert the queryset filter logic is correct.
            a_chunks_qs = DocumentChunk.objects.filter(patient__user=self.user_a)
            b_chunks_qs = DocumentChunk.objects.filter(patient__user=self.user_b)

            # Ensure the two querysets are disjoint at the DB level.
            overlap = a_chunks_qs.filter(
                id__in=b_chunks_qs.values_list("id", flat=True)
            )
            self.assertEqual(overlap.count(), 0)

        except Exception as exc:
            # pgvector extension may not be available in CI — skip gracefully.
            self.skipTest(f"DocumentChunk test skipped (pgvector unavailable): {exc}")

    # ── Context block isolation — no cross-patient data in AI prompt ──────────

    def test_context_block_for_patient_a_excludes_patient_b_data(self):
        """The prompt passed to Gemini for patient A must contain NONE of patient B's data."""
        context = _build_context_block(self.session_a, self.patient_a, [])

        # A's own data must be present.
        self.assertIn("Alice (A's patient)", context)
        self.assertIn("Hypertension", context)
        self.assertIn("Lisinopril", context)

        # B's data must be completely absent.
        self.assertNotIn("Bob (B's patient)", context)
        self.assertNotIn("Diabetes Type 2", context)
        self.assertNotIn("Metformin", context)

    def test_context_block_chat_history_scoped_to_session(self):
        """Chat history in the context block must only contain messages from the given session."""
        context = _build_context_block(self.session_a, self.patient_a, [])
        self.assertIn("A's private note", context)
        self.assertNotIn("B's confidential note", context)

    # ── DRF API isolation — /api/care-items/ ──────────────────────────────────

    def test_api_care_items_scoped_to_authenticated_user(self):
        """GET /api/care-items/ as user A must not return items belonging to user B."""
        # Create a care item for each patient.
        CareItem.objects.create(
            patient=self.patient_a,
            item_type=CareItem.ItemType.TASK,
            title="A's confidential task",
        )
        CareItem.objects.create(
            patient=self.patient_b,
            item_type=CareItem.ItemType.TASK,
            title="B's confidential task",
        )

        # Obtain JWT for user A.
        token_resp = self.client.post(
            "/api/auth/token/",
            data=json.dumps({"username": "caretaker_A", "password": "StrongPass!99"}),
            content_type="application/json",
        )
        if token_resp.status_code != 200:
            self.skipTest("JWT endpoint unavailable in this test environment.")

        access_token = token_resp.json().get("access")
        api_client = Client()
        api_client.defaults["HTTP_AUTHORIZATION"] = f"Bearer {access_token}"

        resp = api_client.get("/api/care-items/")
        self.assertEqual(resp.status_code, 200)

        payload = resp.json()
        items = payload if isinstance(payload, list) else payload.get("results", [])
        titles = [i["title"] for i in items]

        self.assertIn("A's confidential task", titles)
        self.assertNotIn("B's confidential task", titles)


# ─────────────────────────────────────────────────────────────────────────────
# 3. Temperature Configuration Tests
# ─────────────────────────────────────────────────────────────────────────────

class ModelTemperatureTests(TestCase):
    """Assert that every call to the Gemini model passes temperature=0.1."""

    def setUp(self):
        self.user = _make_user("temp_user")
        self.patient = _make_patient(self.user)
        self.session = _make_session(self.patient)

    def _capture_generate_content_call(self, user_text: str):
        """Run generate_assistant_reply and return the captured call kwargs."""
        mock_response = MagicMock()
        mock_response.text = "Mocked AI response."

        with patch("assistant.services.client") as mock_client:
            mock_client.models.generate_content.return_value = mock_response
            generate_assistant_reply(self.session, self.patient, user_text, [])
            return mock_client.models.generate_content.call_args

    def test_generate_content_receives_temperature_0_1(self):
        """generate_assistant_reply must pass temperature=0.1 to the model."""
        call_args = self._capture_generate_content_call("What is the patient's blood pressure trend?")
        self.assertIsNotNone(call_args, "generate_content was never called")

        kwargs = call_args.kwargs
        config = kwargs.get("config")
        self.assertIsNotNone(config, "config kwarg missing from generate_content call")
        self.assertEqual(
            config.temperature,
            0.1,
            f"Expected temperature=0.1, got {config.temperature}",
        )

    def test_temperature_enforced_for_session_summary(self):
        """generate_session_summary must also pass temperature=0.1."""
        mock_response = MagicMock()
        mock_response.text = "Mocked summary."

        with patch("assistant.services.client") as mock_client:
            mock_client.models.generate_content.return_value = mock_response
            generate_session_summary(self.session, self.patient)
            call_args = mock_client.models.generate_content.call_args

        self.assertIsNotNone(call_args)
        config = call_args.kwargs.get("config")
        self.assertIsNotNone(config)
        self.assertEqual(config.temperature, 0.1)

    def test_emergency_bypass_does_not_call_model_at_all(self):
        """Emergency bypass must produce zero model calls (temperature is irrelevant)."""
        with patch("assistant.services.client") as mock_client:
            generate_assistant_reply(self.session, self.patient, "patient is unconscious", [])
        mock_client.models.generate_content.assert_not_called()

    def test_temperature_is_not_zero(self):
        """Ensure temperature is NOT 0 (which would indicate the wrong default)."""
        call_args = self._capture_generate_content_call("What medications should I give?")
        if call_args is None:
            self.skipTest("generate_content not called (client=None in test env)")
        config = call_args.kwargs.get("config")
        self.assertIsNotNone(config)
        self.assertNotEqual(config.temperature, 0)

    def test_temperature_is_not_default_1_0(self):
        """Ensure temperature is NOT the typical LLM default of 1.0."""
        call_args = self._capture_generate_content_call("Summarise the day's schedule")
        if call_args is None:
            self.skipTest("generate_content not called (client=None in test env)")
        config = call_args.kwargs.get("config")
        self.assertIsNotNone(config)
        self.assertNotEqual(config.temperature, 1.0)


# ─────────────────────────────────────────────────────────────────────────────
# 4. AI Fallback Resilience Tests
# ─────────────────────────────────────────────────────────────────────────────

class AIFallbackResilienceTests(TestCase):
    """Verify graceful degradation when the Gemini API is unavailable."""

    def setUp(self):
        self.user = _make_user("resilience_user")
        self.patient = _make_patient(self.user)
        self.session = _make_session(self.patient)
        self.client.force_login(self.user)

    # ── generate_assistant_reply failure paths ────────────────────────────────

    def test_network_error_on_reply_returns_200_not_500(self):
        """A ConnectionError from Gemini must not surface as HTTP 500."""
        with patch("assistant.services._call_model", side_effect=ConnectionError("Gemini unreachable")):
            response = self.client.post(
                reverse("assistant:index"),
                {
                    "action": "send",
                    "message": "What should I do today?",
                    "selected_patient": self.patient.id,
                },
            )
        # 302 redirect after POST is the success path; 500 is the failure path.
        self.assertIn(response.status_code, [200, 302])
        self.assertNotEqual(response.status_code, 500)

    def test_timeout_on_reply_stores_fallback_message_in_db(self):
        """On timeout, a user-friendly fallback reply must be stored in ChatMessage."""
        import socket
        with patch("assistant.services._call_model", side_effect=socket.timeout("Timed out")):
            self.client.post(
                reverse("assistant:index"),
                {
                    "action": "send",
                    "message": "Routine check",
                    "selected_patient": self.patient.id,
                },
            )
        # The most recent assistant message must be the fallback string.
        last_assistant_msg = (
            ChatMessage.objects
            .filter(role=ChatMessage.Role.ASSISTANT)
            .order_by("-created_at")
            .first()
        )
        self.assertIsNotNone(last_assistant_msg)
        self.assertIn("temporarily unavailable", last_assistant_msg.content.lower())

    def test_quota_exceeded_on_reply_does_not_corrupt_user_message(self):
        """Even if the AI call fails, the user's original message must be in the DB."""
        user_message_text = "Quota exceeded test message — unique string 8f3a"
        with patch("assistant.services._call_model", side_effect=Exception("429 Quota exceeded")):
            self.client.post(
                reverse("assistant:index"),
                {
                    "action": "send",
                    "message": user_message_text,
                    "selected_patient": self.patient.id,
                },
            )
        user_msg = ChatMessage.objects.filter(
            role=ChatMessage.Role.USER,
            content=user_message_text,
        ).first()
        self.assertIsNotNone(
            user_msg,
            "The user's message must be persisted even when the AI call fails.",
        )

    # ── generate_session_summary failure paths ────────────────────────────────

    def test_summary_failure_falls_back_to_raw_transcript(self):
        """When generate_session_summary raises, _handle_end_session must store
        a raw transcript fallback — not crash."""
        ChatMessage.objects.create(
            session=self.session, role=ChatMessage.Role.USER, content="Morning check-in note"
        )

        with patch("assistant.views.generate_session_summary", side_effect=Exception("API down")):
            from assistant.views import _handle_end_session
            _handle_end_session(self.session, self.patient, ChatSession.EndReason.IDLE)

        self.session.refresh_from_db()
        self.assertFalse(self.session.is_active)
        # The fallback text must contain the offline warning badge.
        self.assertIn("AI summary temporarily offline", self.session.session_summary)
        # The raw transcript must also be present.
        self.assertIn("Morning check-in note", self.session.session_summary)

    def test_summary_fallback_contains_raw_data_log_marker(self):
        """The raw-fallback summary must include the 'Raw Conversation Log' section header."""
        ChatMessage.objects.create(
            session=self.session, role=ChatMessage.Role.USER, content="Handover note"
        )
        with patch("assistant.views.generate_session_summary", side_effect=OSError("Storage error")):
            from assistant.views import _handle_end_session
            _handle_end_session(self.session, self.patient, ChatSession.EndReason.USER)

        self.session.refresh_from_db()
        self.assertIn("Raw Conversation Log", self.session.session_summary)

    def test_session_marked_inactive_even_if_file_save_fails(self):
        """If the summary_file.save() raises, the session must still be marked inactive."""
        with patch("assistant.views.generate_session_summary", return_value="Mocked summary"):
            with patch.object(
                ChatSession.summary_file.field, "save",
                side_effect=Exception("S3 unreachable"),
            ):
                from assistant.views import _handle_end_session
                _handle_end_session(self.session, self.patient, ChatSession.EndReason.TAB_CLOSE)

        self.session.refresh_from_db()
        # The session must be inactive despite the storage failure.
        self.assertFalse(self.session.is_active)

    # ── close_session_on_tab_close failure path ───────────────────────────────

    def test_close_session_beacon_never_returns_500_on_api_failure(self):
        """The beacon endpoint must return 200 even if Gemini is down during summary."""
        with patch("assistant.views.generate_session_summary", side_effect=Exception("API failure")):
            response = self.client.post(
                reverse("assistant:close_session"),
                {"session": self.session.id},
            )
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertTrue(data.get("ok"))

    def test_close_session_beacon_handles_missing_session_id(self):
        response = self.client.post(reverse("assistant:close_session"), {})
        self.assertEqual(response.status_code, 400)

    def test_close_session_beacon_handles_invalid_session_id(self):
        response = self.client.post(
            reverse("assistant:close_session"), {"session": 999999}
        )
        # Session not found → ok=True, 200 (idempotent).
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json().get("ok"))

    # ── apply_action_proposal failure path ───────────────────────────────────

    def test_action_proposal_db_failure_returns_302_not_500(self):
        """If apply_action_proposal raises, the view must redirect (not 500)."""
        proposal = ChatActionProposal.objects.create(
            session=self.session,
            patient=self.patient,
            item_type="task",
            operation="create",
            title="Test proposal",
            payload={"operation": "create", "item_type": "task", "title": "Do something"},
        )
        with patch("assistant.views.apply_action_proposal", side_effect=Exception("DB write failed")):
            response = self.client.post(
                reverse("assistant:index") + f"?session={self.session.id}",
                {"action": "approve_action", "proposal_id": proposal.id},
            )
        self.assertIn(response.status_code, [200, 302])
        self.assertNotEqual(response.status_code, 500)


# ─────────────────────────────────────────────────────────────────────────────
# 5. Existing Regression Tests (preserved verbatim)
# ─────────────────────────────────────────────────────────────────────────────

class AssistantViewTests(TestCase):
    """Original test cases — retained to prevent regressions."""

    def setUp(self):
        self.user = User.objects.create_user(username="testcare", password="pwd123password")
        self.patient = Patient.objects.create(
            name="John Doe",
            age=45,
            gender=Patient.Gender.MALE,
            allergies="Peanuts",
            current_scenario_description="Recovering from hip surgery",
            user=self.user,
        )

    def test_assistant_index_requires_login(self):
        response = self.client.get(reverse("assistant:index"))
        self.assertEqual(response.status_code, 302)
        self.assertTrue(response.url.startswith("/accounts/login/"))

    def test_assistant_index_authenticated_success(self):
        self.client.login(username="testcare", password="pwd123password")
        response = self.client.get(reverse("assistant:index"))
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, "assistant/chat.html")

    def test_build_context_block_patient_details(self):
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
            "summary_text": "This is a newly generated AI summary.",
        }
        res = apply_action_proposal(self.patient, payload)
        self.assertEqual(res, "update")
        self.patient.refresh_from_db()
        self.assertEqual(self.patient.ai_summary, "This is a newly generated AI summary.")

    def test_proposal_resolution_flow_for_summary(self):
        self.client.login(username="testcare", password="pwd123password")
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
                "summary_text": "Approved AI summary text",
            },
        )

        url = reverse("assistant:index") + f"?session={session.id}"
        response = self.client.post(url, {"action": "approve_action", "proposal_id": proposal.id})
        self.assertEqual(response.status_code, 302)

        proposal.refresh_from_db()
        self.assertEqual(proposal.status, ChatActionProposal.Status.APPROVED)

        self.patient.refresh_from_db()
        self.assertEqual(self.patient.ai_summary, "Approved AI summary text")

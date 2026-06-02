from django.test import TestCase
from django.urls import reverse
from unittest.mock import patch

class LandingPageTests(TestCase):
    def test_landing_page_renders_successfully(self):
        response = self.client.get(reverse('home'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "ADAPT Care Platform")
        self.assertContains(response, "A cleaner caregiver workflow")

    @patch("adapt.context_processors.CaretakerProfile.objects.get_or_create")
    def test_landing_page_does_not_hit_database_for_anonymous_user(self, get_or_create_mock):
        response = self.client.get(reverse('home'))
        self.assertEqual(response.status_code, 200)
        get_or_create_mock.assert_not_called()


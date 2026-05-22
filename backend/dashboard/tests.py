from django.test import TestCase
from django.urls import reverse

class LandingPageTests(TestCase):
    def test_landing_page_renders_successfully(self):
        response = self.client.get(reverse('home'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "ADAPT Care Platform")
        self.assertContains(response, "A cleaner caregiver workflow")


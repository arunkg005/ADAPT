from django.test import TestCase
from django.urls import reverse
from django.contrib.auth import get_user_model

class AssistantViewTests(TestCase):
    def setUp(self):
        self.User = get_user_model()
        self.user = self.User.objects.create_user(username='testcare', password='pwd123password')

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


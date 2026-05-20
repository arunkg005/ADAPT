from django.db import models
from django.conf import settings


class CaretakerProfile(models.Model):
	user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="caretaker_profile", null=True, blank=True)
	full_name = models.CharField(max_length=150)
	role = models.CharField(max_length=120, default="Primary Caretaker")
	email = models.EmailField(blank=True)
	phone = models.CharField(max_length=40, blank=True)
	alternate_phone = models.CharField(max_length=40, blank=True)
	timezone = models.CharField(max_length=80, default="Asia/Kolkata")
	emergency_contact_name = models.CharField(max_length=150, blank=True)
	emergency_contact_phone = models.CharField(max_length=40, blank=True)
	address = models.TextField(blank=True)
	bio = models.TextField(blank=True)
	email_notifications = models.BooleanField(default=True)
	sms_notifications = models.BooleanField(default=False)
	routine_reminders_enabled = models.BooleanField(default=True)
	daily_digest_enabled = models.BooleanField(default=True)
	created_at = models.DateTimeField(auto_now_add=True)
	updated_at = models.DateTimeField(auto_now=True)

	def __str__(self):
		return self.full_name

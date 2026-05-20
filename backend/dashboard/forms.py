from django import forms

from .models import CaretakerProfile


class CaretakerProfileForm(forms.ModelForm):
    class Meta:
        model = CaretakerProfile
        fields = [
            "full_name",
            "role",
            "email",
            "phone",
            "alternate_phone",
            "timezone",
            "emergency_contact_name",
            "emergency_contact_phone",
            "address",
            "bio",
            "email_notifications",
            "sms_notifications",
            "routine_reminders_enabled",
            "daily_digest_enabled",
        ]

"""
Shared context processor that injects caretaker profile and navigation
context into every template render. This eliminates the need for each view
to manually build shell context.
"""
from dashboard.models import CaretakerProfile


CARETAKER_DEFAULTS = {
	"full_name": "Neha Sharma",
	"role": "Primary Caretaker",
	"timezone": "Asia/Kolkata",
}


def _get_or_create_caretaker_profile():
	profile = CaretakerProfile.objects.order_by("id").first()
	if profile:
		return profile
	return CaretakerProfile.objects.create(**CARETAKER_DEFAULTS)


def caretaker_context(request):
	"""Injects ``caretaker`` dict into template context automatically."""
	profile = _get_or_create_caretaker_profile()
	return {
		"caretaker": {
			"name": profile.full_name,
			"role": profile.role,
		},
	}

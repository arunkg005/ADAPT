"""
Shared context processor that injects caretaker profile and navigation
context into every template render. This eliminates the need for each view
to manually build shell context.
"""
from django.db import DatabaseError

from dashboard.models import CaretakerProfile


CARETAKER_DEFAULTS = {
    "full_name": "Neha Sharma",
    "role": "Primary Caretaker",
    "timezone": "Asia/Kolkata",
}


def _default_caretaker_context():
    return {
        "caretaker": {
            "name": CARETAKER_DEFAULTS["full_name"],
            "role": CARETAKER_DEFAULTS["role"],
        },
    }


def _get_or_create_caretaker_profile(request):
    user = getattr(request, "user", None)
    if not getattr(user, "is_authenticated", False):
        return None

    try:
        profile, _ = CaretakerProfile.objects.get_or_create(
            user=user,
            defaults=CARETAKER_DEFAULTS,
        )
        return profile
    except DatabaseError:
        return None


def caretaker_context(request):
    """Injects ``caretaker`` dict into template context automatically."""
    profile = _get_or_create_caretaker_profile(request)
    if profile is None:
        return _default_caretaker_context()

    return {
        "caretaker": {
            "name": profile.full_name,
            "role": profile.role,
        },
    }

"""
Celery configuration for the ADAPT project.

Handles background AI tasks (Gemini analysis), routine reminders,
and any async processing.
"""

import os

from celery import Celery

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "adapt.settings")

app = Celery("adapt")

# Pull config values from Django settings, prefixed with CELERY_
app.config_from_object("django.conf:settings", namespace="CELERY")

# Auto-discover tasks.py in every installed app
app.autodiscover_tasks()


@app.task(bind=True, ignore_result=True)
def debug_task(self):
    """Simple sanity-check task for verifying the worker is online."""
    print(f"Request: {self.request!r}")

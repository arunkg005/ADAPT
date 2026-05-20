from django.db import models

from patients.models import Patient


class CareItem(models.Model):
	class ItemType(models.TextChoices):
		TASK = "task", "Task"
		ROUTINE = "routine", "Routine"
		SCHEDULE = "schedule", "Schedule"

	class Status(models.TextChoices):
		PENDING = "pending", "Pending"
		IN_PROGRESS = "in_progress", "In Progress"
		DONE = "done", "Done"

	class Priority(models.TextChoices):
		LOW = "low", "Low"
		MEDIUM = "medium", "Medium"
		HIGH = "high", "High"

	class RecurrenceMode(models.TextChoices):
		NONE = "none", "No recurrence"
		DAILY = "daily", "Daily"
		SELECTED_WEEKDAYS = "selected_weekdays", "Selected weekdays"
		WEEKLY = "weekly", "Weekly"
		MONTHLY_DATES = "monthly_dates", "Monthly dates"
		SPECIFIC_DATES = "specific_dates", "Specific dates"

	patient = models.ForeignKey(Patient, on_delete=models.CASCADE, related_name="care_items")
	item_type = models.CharField(max_length=20, choices=ItemType.choices)
	title = models.CharField(max_length=200)
	description = models.TextField(blank=True)
	notes = models.TextField(blank=True)
	status = models.CharField(max_length=20, choices=Status.choices, default=Status.PENDING)
	priority = models.CharField(max_length=10, choices=Priority.choices, default=Priority.MEDIUM)
	due_at = models.DateTimeField(null=True, blank=True)
	recurrence_mode = models.CharField(max_length=30, choices=RecurrenceMode.choices, default=RecurrenceMode.NONE)
	recurrence_weekdays = models.CharField(max_length=50, blank=True)
	recurrence_dates = models.CharField(max_length=300, blank=True)
	reminder_enabled = models.BooleanField(default=False)
	reminder_minutes_before = models.PositiveIntegerField(null=True, blank=True)
	created_at = models.DateTimeField(auto_now_add=True)
	updated_at = models.DateTimeField(auto_now=True)

	class Meta:
		ordering = ["-created_at"]

	def __str__(self):
		return f"{self.get_item_type_display()}: {self.title}"

from django import forms

from .models import CareItem


WEEKDAY_CHOICES = [
    ("mon", "Mon"),
    ("tue", "Tue"),
    ("wed", "Wed"),
    ("thu", "Thu"),
    ("fri", "Fri"),
    ("sat", "Sat"),
    ("sun", "Sun"),
]


class CareItemForm(forms.ModelForm):
    recurrence_weekday_choices = forms.MultipleChoiceField(
        choices=WEEKDAY_CHOICES,
        required=False,
        widget=forms.CheckboxSelectMultiple,
        label="Recurrence days",
    )

    class Meta:
        model = CareItem
        fields = [
            "title",
            "description",
            "notes",
            "status",
            "priority",
            "due_at",
            "recurrence_mode",
            "recurrence_dates",
            "reminder_enabled",
            "reminder_minutes_before",
        ]
        widgets = {
            "due_at": forms.DateTimeInput(attrs={"type": "datetime-local"}),
            "description": forms.Textarea(attrs={"rows": 3}),
            "notes": forms.Textarea(attrs={"rows": 3}),
            "recurrence_dates": forms.Textarea(
                attrs={"rows": 2, "placeholder": "e.g. 2026-05-01, 2026-05-15 or 1,15,28"}
            ),
        }

    def __init__(self, *args, item_type=None, patient=None, **kwargs):
        super().__init__(*args, **kwargs)
        self.item_type = item_type
        self.patient = patient

        if self.instance and self.instance.pk and self.instance.recurrence_weekdays:
            self.fields["recurrence_weekday_choices"].initial = self.instance.recurrence_weekdays.split(",")

        if self.item_type == CareItem.ItemType.ROUTINE:
            self.fields["priority"].required = False
            self.fields["priority"].help_text = "Routine priority is optional."
            self.fields["recurrence_mode"].choices = [
                (CareItem.RecurrenceMode.DAILY, "Daily"),
                (CareItem.RecurrenceMode.SELECTED_WEEKDAYS, "Particular days of week"),
            ]
            self.fields["reminder_enabled"].help_text = "Routine reminder uses patient-level common setting."
            self.fields["reminder_enabled"].disabled = True
            self.fields["reminder_minutes_before"].disabled = True

        if self.item_type == CareItem.ItemType.SCHEDULE:
            self.fields["recurrence_mode"].choices = [
                (CareItem.RecurrenceMode.SPECIFIC_DATES, "Specific dates"),
                (CareItem.RecurrenceMode.SELECTED_WEEKDAYS, "Selected weekdays"),
                (CareItem.RecurrenceMode.WEEKLY, "Weekly"),
                (CareItem.RecurrenceMode.MONTHLY_DATES, "Monthly dates"),
            ]

        if self.item_type == CareItem.ItemType.TASK:
            self.fields["recurrence_mode"].choices = [
                (CareItem.RecurrenceMode.NONE, "No recurrence"),
                (CareItem.RecurrenceMode.DAILY, "Daily"),
                (CareItem.RecurrenceMode.SELECTED_WEEKDAYS, "Selected weekdays"),
                (CareItem.RecurrenceMode.WEEKLY, "Weekly"),
                (CareItem.RecurrenceMode.MONTHLY_DATES, "Monthly dates"),
                (CareItem.RecurrenceMode.SPECIFIC_DATES, "Specific dates"),
            ]

    def clean(self):
        cleaned_data = super().clean()
        recurrence_mode = cleaned_data.get("recurrence_mode")
        weekdays = cleaned_data.get("recurrence_weekday_choices") or []
        dates_text = (cleaned_data.get("recurrence_dates") or "").strip()
        reminder_enabled = cleaned_data.get("reminder_enabled")
        reminder_minutes = cleaned_data.get("reminder_minutes_before")

        if recurrence_mode == CareItem.RecurrenceMode.SELECTED_WEEKDAYS and not weekdays:
            self.add_error("recurrence_weekday_choices", "Select at least one weekday.")

        if recurrence_mode in {
            CareItem.RecurrenceMode.SPECIFIC_DATES,
            CareItem.RecurrenceMode.MONTHLY_DATES,
        } and not dates_text:
            self.add_error("recurrence_dates", "Enter the required dates or day numbers.")

        if reminder_enabled and not reminder_minutes:
            self.add_error("reminder_minutes_before", "Set reminder minutes before due time.")

        if reminder_minutes is not None and reminder_minutes < 0:
            self.add_error("reminder_minutes_before", "Reminder minutes cannot be negative.")

        cleaned_data["recurrence_weekdays"] = ",".join(weekdays)

        if self.item_type == CareItem.ItemType.ROUTINE:
            cleaned_data["reminder_enabled"] = False
            cleaned_data["reminder_minutes_before"] = None

        return cleaned_data

    def save(self, commit=True):
        instance = super().save(commit=False)
        instance.recurrence_weekdays = self.cleaned_data.get("recurrence_weekdays", "")
        if not self.cleaned_data.get("reminder_enabled"):
            instance.reminder_minutes_before = None

        if commit:
            instance.save()
        return instance

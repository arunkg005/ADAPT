from django.db import models

from patients.models import Patient


class ChatSession(models.Model):
	class Source(models.TextChoices):
		CARETAKER = "caretaker", "Caretaker Dashboard"
		PATIENT = "patient", "Patient Dashboard"

	class EndReason(models.TextChoices):
		USER = "user", "User ended"
		IDLE = "idle", "Idle timeout"
		TAB_CLOSE = "tab_close", "Tab/browser close"
		CONTEXT_CHANGE = "context_change", "Patient/context changed"

	source = models.CharField(max_length=20, choices=Source.choices, default=Source.CARETAKER)
	patient = models.ForeignKey(Patient, null=True, blank=True, on_delete=models.SET_NULL, related_name="chat_sessions")
	title = models.CharField(max_length=200, blank=True)
	is_active = models.BooleanField(default=True)
	session_summary = models.TextField(blank=True)
	summary_file = models.FileField(upload_to="session_summaries/%Y/%m/%d", blank=True, null=True)
	end_reason = models.CharField(max_length=30, choices=EndReason.choices, blank=True)
	created_at = models.DateTimeField(auto_now_add=True)
	ended_at = models.DateTimeField(null=True, blank=True)

	class Meta:
		ordering = ["-created_at"]

	def __str__(self):
		return self.title or f"Session {self.id}"


class ChatMessage(models.Model):
	class Role(models.TextChoices):
		USER = "user", "User"
		ASSISTANT = "assistant", "Assistant"
		SYSTEM = "system", "System"

	session = models.ForeignKey(ChatSession, on_delete=models.CASCADE, related_name="messages")
	role = models.CharField(max_length=20, choices=Role.choices)
	content = models.TextField()
	created_at = models.DateTimeField(auto_now_add=True)

	class Meta:
		ordering = ["created_at"]


class ChatActionProposal(models.Model):
	class Status(models.TextChoices):
		PENDING = "pending", "Pending"
		APPROVED = "approved", "Approved"
		REJECTED = "rejected", "Rejected"

	session = models.ForeignKey(ChatSession, on_delete=models.CASCADE, related_name="action_proposals")
	patient = models.ForeignKey(Patient, null=True, blank=True, on_delete=models.SET_NULL)
	item_type = models.CharField(max_length=20)
	operation = models.CharField(max_length=20)
	title = models.CharField(max_length=200, blank=True)
	payload = models.JSONField(default=dict)
	status = models.CharField(max_length=20, choices=Status.choices, default=Status.PENDING)
	created_at = models.DateTimeField(auto_now_add=True)
	resolved_at = models.DateTimeField(null=True, blank=True)

	class Meta:
		ordering = ["-created_at"]

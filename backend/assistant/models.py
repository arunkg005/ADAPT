from django.db import models
from pgvector.django import VectorField

from patients.models import Patient, PatientDocument


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


class DocumentChunk(models.Model):
	"""A single text chunk of a PatientDocument together with its embedding vector.

	Embedding dimensionality is 768, which is the exact output shape produced
	by Google's ``text-embedding-004`` model.  Both FK columns are indexed by
	Django automatically (ForeignKey always creates a DB index).

	The ``(document, chunk_index)`` unique constraint makes re-indexing runs
	idempotent: a second ``index_document_embeddings_async`` call for the same
	document can safely ``bulk_create(update_conflicts=True)`` without
	creating phantom duplicate rows.
	"""

	document = models.ForeignKey(
		PatientDocument,
		on_delete=models.CASCADE,
		related_name="chunks",
	)
	patient = models.ForeignKey(
		Patient,
		on_delete=models.CASCADE,
		related_name="document_chunks",
	)
	chunk_index = models.PositiveIntegerField(
		help_text="Zero-based position of this chunk within the source document.",
	)
	text_content = models.TextField(
		help_text="Raw extracted text for this chunk.",
	)
	embedding = VectorField(
		dimensions=768,
		help_text="text-embedding-004 semantic vector (768-dim).",
	)
	created_at = models.DateTimeField(auto_now_add=True)

	class Meta:
		ordering = ["document", "chunk_index"]
		constraints = [
			models.UniqueConstraint(
				fields=["document", "chunk_index"],
				name="uq_documentchunk_document_index",
			),
		]
		indexes = [
			# Speeds up per-patient similarity searches without joining through document.
			models.Index(fields=["patient"], name="idx_documentchunk_patient"),
		]

	def __str__(self):
		return f"Chunk {self.chunk_index} of {self.document}"

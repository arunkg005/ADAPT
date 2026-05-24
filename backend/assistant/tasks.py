"""assistant/tasks.py — Celery tasks for the RAG document-embedding pipeline.

Workflow for ``index_document_embeddings_async``:
  1. Fetch the ``PatientDocument`` record (and its parent ``Patient``).
  2. Open the stored file and extract plain text.
     - Plain-text / Markdown files are read directly.
     - PDF files are parsed page-by-page with ``pypdf``.
  3. Chunk the full text into overlapping windows of ``CHUNK_SIZE`` characters
     with a ``CHUNK_OVERLAP`` stride so semantic context is not lost at
     boundaries.
  4. Embed every chunk via Google's ``text-embedding-004`` model using the
     shared ``client`` instance from ``assistant.services``.
  5. Persist all ``DocumentChunk`` rows in a single ``bulk_create`` call.
     The ``update_conflicts`` flag keeps re-indexing idempotent: running the
     task twice for the same document updates the embedding in-place rather
     than raising an IntegrityError.
"""

from __future__ import annotations

import io
import logging
import textwrap

from celery import shared_task
from google.genai import types

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Chunking parameters
# ---------------------------------------------------------------------------
CHUNK_SIZE: int = 1_500      # characters per chunk
CHUNK_OVERLAP: int = 200     # overlap between consecutive chunks
EMBED_MODEL: str = "text-embedding-004"
EMBED_DIMENSIONS: int = 768  # exact output shape for text-embedding-004


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _extract_text_from_file(document) -> str:
    """Return the full plain-text content of *document*.file.

    Supports:
    - Plain-text files (.txt, .md, .csv, and any other non-binary format).
    - PDF files parsed page-by-page via ``pypdf``.

    Falls back gracefully to an empty string if extraction fails.
    """
    file_field = document.file
    file_name = file_field.name.lower()

    try:
        with file_field.open("rb") as fh:
            raw_bytes = fh.read()
    except Exception as exc:  # noqa: BLE001
        logger.error(
            "DocumentChunk indexing: could not read file for document %s — %s",
            document.pk,
            exc,
        )
        return ""

    # ---- PDF ----------------------------------------------------------------
    if file_name.endswith(".pdf"):
        try:
            import pypdf  # optional dependency; fail loudly if missing

            reader = pypdf.PdfReader(io.BytesIO(raw_bytes))
            pages = [page.extract_text() or "" for page in reader.pages]
            return "\n\n".join(pages)
        except Exception as exc:  # noqa: BLE001
            logger.error(
                "DocumentChunk indexing: PDF extraction failed for document %s — %s",
                document.pk,
                exc,
            )
            return ""

    # ---- Plain text (best-effort UTF-8 decode) ------------------------------
    try:
        return raw_bytes.decode("utf-8", errors="replace")
    except Exception as exc:  # noqa: BLE001
        logger.error(
            "DocumentChunk indexing: text decode failed for document %s — %s",
            document.pk,
            exc,
        )
        return ""


def _chunk_text(text: str, size: int = CHUNK_SIZE, overlap: int = CHUNK_OVERLAP) -> list[str]:
    """Split *text* into overlapping windows of *size* characters.

    Returns an empty list when the input is blank so the caller can short-
    circuit without creating any DB rows.
    """
    text = text.strip()
    if not text:
        return []

    chunks: list[str] = []
    start = 0
    while start < len(text):
        end = start + size
        chunks.append(text[start:end])
        start += size - overlap  # advance by (size - overlap) = stride

    return chunks


def _embed_chunks(chunks: list[str]) -> list[list[float]]:
    """Call the ``text-embedding-004`` model and return one vector per chunk.

    Uses the module-level ``client`` from ``assistant.services`` so we have a
    single, shared, authenticated ``genai.Client`` instance across the whole
    application.  If the client is ``None`` (missing API key) an empty list is
    returned for every chunk so the caller can skip persistence.
    """
    # Import lazily to avoid a circular import at module load time.
    from assistant.services import client  # noqa: PLC0415

    if client is None:
        logger.warning(
            "DocumentChunk indexing: Gemini client not initialised (missing API key). "
            "Skipping embedding generation."
        )
        return [[] for _ in chunks]

    embeddings: list[list[float]] = []
    for chunk_text in chunks:
        try:
            response = client.models.embed_content(
                model=EMBED_MODEL,
                contents=chunk_text,
                config=types.EmbedContentConfig(
                    output_dimensionality=EMBED_DIMENSIONS,
                ),
            )
            # The SDK returns a list of ContentEmbedding objects; we want the
            # first (and only) one's .values attribute.
            vector = response.embeddings[0].values
            embeddings.append(list(vector))
        except Exception as exc:  # noqa: BLE001
            logger.error(
                "DocumentChunk indexing: embedding API call failed for a chunk — %s", exc
            )
            embeddings.append([])  # placeholder so indices stay aligned

    return embeddings


# ---------------------------------------------------------------------------
# Celery task
# ---------------------------------------------------------------------------

@shared_task(
    bind=True,
    name="assistant.tasks.index_document_embeddings_async",
    max_retries=3,
    default_retry_delay=60,  # seconds
    acks_late=True,
)
def index_document_embeddings_async(self, document_id: int) -> dict:
    """Chunk, embed, and index a single ``PatientDocument`` into ``DocumentChunk``.

    Args:
        document_id: Primary key of the ``PatientDocument`` to process.

    Returns:
        A summary dict with ``{"document_id": ..., "chunks_created": ...}``
        that Celery stores in the result backend for observability.

    Raises:
        self.retry: Automatically retried up to ``max_retries`` times on any
        transient exception (network error, API rate limit, etc.).
    """
    # Import inside the task body to prevent Django app-registry issues when
    # the worker process boots before ``django.setup()`` completes.
    from patients.models import PatientDocument  # noqa: PLC0415
    from .models import DocumentChunk  # noqa: PLC0415

    logger.info("DocumentChunk indexing: starting for document_id=%s", document_id)

    # ------------------------------------------------------------------
    # 1. Fetch the document record
    # ------------------------------------------------------------------
    try:
        document = PatientDocument.objects.select_related("patient").get(pk=document_id)
    except PatientDocument.DoesNotExist:
        logger.error(
            "DocumentChunk indexing: PatientDocument(id=%s) not found. Aborting.",
            document_id,
        )
        return {"document_id": document_id, "chunks_created": 0, "error": "document_not_found"}

    patient = document.patient

    # ------------------------------------------------------------------
    # 2. Extract text from the stored file
    # ------------------------------------------------------------------
    full_text = _extract_text_from_file(document)
    if not full_text.strip():
        logger.warning(
            "DocumentChunk indexing: no text extracted from document_id=%s. "
            "Nothing to index.",
            document_id,
        )
        return {"document_id": document_id, "chunks_created": 0, "error": "no_text_extracted"}

    # ------------------------------------------------------------------
    # 3. Chunk the text
    # ------------------------------------------------------------------
    chunks = _chunk_text(full_text)
    logger.info(
        "DocumentChunk indexing: document_id=%s produced %d chunks.", document_id, len(chunks)
    )

    # ------------------------------------------------------------------
    # 4. Embed every chunk via text-embedding-004
    # ------------------------------------------------------------------
    try:
        embeddings = _embed_chunks(chunks)
    except Exception as exc:  # noqa: BLE001
        logger.exception(
            "DocumentChunk indexing: embedding batch failed for document_id=%s.", document_id
        )
        raise self.retry(exc=exc)

    # ------------------------------------------------------------------
    # 5. Bulk-persist DocumentChunk rows (idempotent via update_conflicts)
    # ------------------------------------------------------------------
    chunk_objects: list[DocumentChunk] = []
    for idx, (text, vector) in enumerate(zip(chunks, embeddings)):
        if not vector:
            # Skip chunks whose embedding call failed (logged in _embed_chunks).
            logger.warning(
                "DocumentChunk indexing: skipping chunk %d for document_id=%s "
                "(empty embedding).",
                idx,
                document_id,
            )
            continue
        chunk_objects.append(
            DocumentChunk(
                document=document,
                patient=patient,
                chunk_index=idx,
                text_content=text,
                embedding=vector,
            )
        )

    if not chunk_objects:
        logger.warning(
            "DocumentChunk indexing: all chunks were skipped for document_id=%s. "
            "No rows written.",
            document_id,
        )
        return {"document_id": document_id, "chunks_created": 0, "error": "all_chunks_skipped"}

    try:
        created = DocumentChunk.objects.bulk_create(
            chunk_objects,
            update_conflicts=True,
            unique_fields=["document", "chunk_index"],
            update_fields=["text_content", "embedding"],
        )
    except Exception as exc:  # noqa: BLE001
        logger.exception(
            "DocumentChunk indexing: bulk_create failed for document_id=%s.", document_id
        )
        raise self.retry(exc=exc)

    count = len(created)
    logger.info(
        "DocumentChunk indexing: completed for document_id=%s — %d rows written.",
        document_id,
        count,
    )
    return {"document_id": document_id, "chunks_created": count}

# ============================================================
# ADAPT — Production Dockerfile
# Multi-stage build for the Django/Gunicorn backend
# ============================================================

# ---------- Stage 1: Build dependencies ----------
FROM python:3.13-slim AS builder

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /build

# Install system deps required to compile psycopg / Pillow
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential \
        libpq-dev \
        libjpeg62-turbo-dev \
        zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

COPY ./backend/requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

# ---------- Stage 2: Runtime ----------
FROM python:3.13-slim AS runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    DJANGO_SETTINGS_MODULE=adapt.settings

WORKDIR /app

# Runtime-only system libraries
RUN apt-get update && apt-get install -y --no-install-recommends \
        libpq5 \
        libjpeg62-turbo \
    && rm -rf /var/lib/apt/lists/*

# Copy pip packages from builder
COPY --from=builder /install /usr/local

# Copy project source
COPY ./backend .
COPY ./web /web

# Collect static files (requires SECRET_KEY at build time)
ARG DJANGO_SECRET_KEY=build-placeholder
RUN python manage.py collectstatic --noinput

# Create non-root user
RUN addgroup --system adapt && adduser --system --ingroup adapt adapt \
    && chown -R adapt:adapt /app
USER adapt

EXPOSE 8000

# Default entrypoint — can be overridden in docker-compose
CMD ["gunicorn", "adapt.wsgi:application", "--config", "gunicorn.conf.py"]

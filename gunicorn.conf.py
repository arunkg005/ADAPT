"""
Gunicorn configuration for ADAPT production deployment.
"""

import os
import multiprocessing

# ---------- Server socket ----------
bind = f"0.0.0.0:{os.getenv('PORT', '8000')}"
backlog = 2048

# ---------- Workers ----------
# gthread workers handle concurrent I/O (DB, Gemini API calls) efficiently
workers = multiprocessing.cpu_count() * 2 + 1
worker_class = "gthread"
threads = 4
worker_connections = 1000

# ---------- Timeouts ----------
# Extended timeout for AI-heavy requests (Gemini analysis)
timeout = 120
graceful_timeout = 30
keepalive = 5

# ---------- Logging ----------
accesslog = "-"  # stdout
errorlog = "-"   # stderr
loglevel = "info"

# ---------- Process naming ----------
proc_name = "adapt-web"

# ---------- Security ----------
limit_request_line = 8190
limit_request_fields = 100
limit_request_field_size = 8190

#!/bin/sh
set -e

ENGINE_CMD="node /app/engine-service/dist/index.js"
BACKEND_CMD="node /app/backend/dist/db/migrate.js && node /app/backend/dist/index.js"
FRONTEND_CMD="npm --prefix /app/frontend run start -- --hostname 0.0.0.0 --port 3000"

ENGINE_PID=""
BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
  for pid in "$ENGINE_PID" "$BACKEND_PID" "$FRONTEND_PID"; do
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done

  wait || true
}

trap cleanup INT TERM

echo "Starting ADAPT single-image stack..."

eval "$ENGINE_CMD" &
ENGINE_PID=$!

eval "$BACKEND_CMD" &
BACKEND_PID=$!

eval "$FRONTEND_CMD" &
FRONTEND_PID=$!

while true; do
  for pid in "$ENGINE_PID" "$BACKEND_PID" "$FRONTEND_PID"; do
    if ! kill -0 "$pid" 2>/dev/null; then
      wait "$pid" || true
      cleanup
      exit 1
    fi
  done

  sleep 2
done

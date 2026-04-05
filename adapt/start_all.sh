#!/bin/bash
# ADAPT Project - Complete Startup Script for Linux/Mac
# This script starts all four services in separate terminal windows

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                                                            ║"
echo "║        🧠 ADAPT Project - Multi-Service Startup 🧠        ║"
echo "║                                                            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "⚠️  Docker not found. Please install Docker before running this script."
    exit 1
fi

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "⚠️  Node.js not found. Please install Node.js before running this script."
    exit 1
fi

echo "✅ Prerequisites found (Docker, Node.js)"
echo ""
echo "Starting services..."
echo ""

# Terminal 1 - PostgreSQL with Docker
echo "[1/4] Starting PostgreSQL..."
docker-compose up &
PG_PID=$!

# Wait for PostgreSQL to be ready
sleep 5

# Terminal 2 - Engine Service
echo "[2/4] Starting Engine Service..."
(cd engine-service && npm run dev) &
ENGINE_PID=$!

# Wait for Engine Service to start
sleep 3

# Terminal 3 - Backend Service
echo "[3/4] Starting Backend Service..."
(cd backend && npm run migrate && npm run dev) &
BACKEND_PID=$!

# Wait for Backend Service to start
sleep 3

# Terminal 4 - Frontend
echo "[4/4] Starting Frontend..."
(cd frontend && npm run dev) &
FRONTEND_PID=$!

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                                                            ║"
echo "║              🚀 All services started! 🚀                  ║"
echo "║                                                            ║"
echo "║  Frontend:      http://localhost:3000                     ║"
echo "║  Backend API:   http://localhost:3001/api/docs            ║"
echo "║  Engine API:    http://localhost:4001/docs                ║"
echo "║  PostgreSQL:    localhost:5432                            ║"
echo "║                                                            ║"
echo "║  Process IDs:                                              ║"
echo "║  - PostgreSQL: $PG_PID                                     ║"
echo "║  - Engine: $ENGINE_PID                                     ║"
echo "║  - Backend: $BACKEND_PID                                   ║"
echo "║  - Frontend: $FRONTEND_PID                                 ║"
echo "║                                                            ║"
echo "║  Press Ctrl+C to stop all services                        ║"
echo "║                                                            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Wait for all background processes
wait

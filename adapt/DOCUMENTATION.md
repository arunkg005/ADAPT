# ADAPT Documentation

## Overview
ADAPT is a cognitive assistance platform designed to support elderly and cognitively vulnerable users through guided routines, telemetry analysis, and caregiver-facing monitoring.

The repository contains three main apps:
- `frontend` (Next.js app for landing page, login, and dashboard)
- `backend` (Express + TypeScript API with PostgreSQL)
- `engine-service` (independent cognitive evaluation service)

## Architecture

### High-Level Flow
1. User and caregiver interfaces run in the `frontend` app.
2. `frontend` calls `backend` APIs for auth, patients, caregivers, devices, alerts, and telemetry.
3. `backend` stores and reads domain data from PostgreSQL.
4. `backend` sends telemetry/evaluation requests to `engine-service`.
5. `engine-service` returns cognitive evaluation output used for alerts and guidance.

### Backend Boundaries
- Routes are in `backend/src/routes`.
- Business logic is in `backend/src/services`.
- Database connection and scripts are in `backend/src/db`.
- Auth and error middleware are in `backend/src/middleware`.

### Frontend Boundaries
- App Router entry points live in `frontend/app`.
- Active main routes are:
  - `frontend/app/page.tsx`
  - `frontend/app/login/page.tsx`
  - `frontend/app/dashboard/page.tsx`

### Engine Service
- Main runtime files are in `engine-service/src`.
- It runs as a separate process and is called by backend over HTTP.

## Prerequisites
- Node.js 18+
- npm
- Docker Desktop (optional, for DB/app containers)

## Environment Setup

Create `backend/.env` with values similar to:

```env
NODE_ENV=development
PORT=3001
API_BASE_URL=http://localhost:3001

DB_HOST=localhost
DB_PORT=5432
DB_NAME=adapt_db
DB_USER=adapt_user
DB_PASSWORD=adapt_password

JWT_SECRET=change-this-secret
JWT_EXPIRY=7d

ENGINE_SERVICE_URL=http://localhost:4001
```

## Install Dependencies

Preferred (from `adapt/package.json` helper scripts):

```bash
npm run install:all
```

Manual alternative:

```bash
cd backend && npm install
cd ../frontend && npm install
cd ../engine-service && npm install
```

## Run Locally

### Option A: Start each service manually

Terminal 1 (database):
```bash
docker compose up -d
```

Terminal 2 (engine):
```bash
cd engine-service
npm run dev
```

Terminal 3 (backend):
```bash
cd backend
npm run migrate
npm run seed
npm run dev
```

Terminal 4 (frontend):
```bash
cd frontend
npm run dev
```

### Option B: Helper scripts
- Windows: `START_ALL.bat` or `START_ALL_NO_DOCKER.bat`
- Linux/Mac: `start_all.sh`

### Option C: Single app image (frontend + backend + engine)

From `adapt/`:

```bash
docker compose --profile single-image up -d --build
```

Useful commands:

```bash
docker compose --profile single-image ps
docker compose --profile single-image logs -f adapt-app
docker compose --profile single-image down
```

Notes:
- This runs a single app container (`adapt_app`) plus the existing PostgreSQL container.
- `docker compose up -d` still starts only PostgreSQL, so DB-only workflows are unchanged.
- This setup only uses files under `adapt/` and does not affect `Adapt_mobile_app/` or Android development.
- Docker is optional for local development if your team already has a local PostgreSQL instance.

## Commands

### Backend (`backend/package.json`)
- `npm run dev` - start backend in development mode
- `npm run build` - compile TypeScript to `dist`
- `npm run start` - run compiled backend
- `npm run typecheck` - TypeScript checks without emit
- `npm run migrate` - run DB schema creation
- `npm run seed` - seed sample data

### Frontend (`frontend/package.json`)
- `npm run dev` - start Next.js dev server
- `npm run build` - production build
- `npm run start` - run production build
- `npm run lint` - run ESLint

### Engine (`engine-service/package.json`)
- `npm run dev` - start engine in development mode
- `npm run build` - compile TypeScript
- `npm run start` - run compiled engine
- `npm test` - placeholder script (currently exits with error)

## Default Local Ports
- Frontend: `3000`
- Backend: `3001`
- Engine service: `4001`
- PostgreSQL: `5432`

## Database

The backend migration/seed scripts create and initialize core tables for:
- users
- patients
- caregivers
- patient-caregiver links
- devices
- alerts
- telemetry

Run order for a fresh setup:
1. Start PostgreSQL.
2. Run `npm run migrate` in `backend`.
3. Run `npm run seed` in `backend`.

## Auth
- Backend uses JWT-based authentication.
- Passwords are hashed with bcrypt.
- Role model includes ADMIN, CAREGIVER, and PATIENT.

## API Areas
Main backend API groups:
- `/api/auth`
- `/api/patients`
- `/api/caregivers`
- `/api/devices`
- `/api/alerts`
- `/api/telemetry`

Health endpoint:
- `/health`

## Common Issues
- Missing `backend/.env` causes backend startup failures or incorrect service URLs.
- PostgreSQL not running causes migration/seed failures.
- Port conflicts on 3000/3001/4001/5432 prevent services from starting.
- Engine service unavailable can degrade telemetry evaluation behavior.

## Validation Checklist
1. `backend`: run `npm run typecheck`.
2. `frontend`: run `npm run lint` and `npm run build`.
3. `engine-service`: run `npm run build`.
4. Verify frontend loads on `http://localhost:3000`.
5. Verify backend health on `http://localhost:3001/health`.

## Notes
- Repository includes several historical UI variant files (`*-new.tsx`, `*-old.tsx`, `*-redesigned.tsx`).
- Treat `frontend/app/page.tsx`, `frontend/app/login/page.tsx`, and `frontend/app/dashboard/page.tsx` as primary routes unless intentionally switching variants.
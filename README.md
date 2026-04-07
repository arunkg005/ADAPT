# ADAPT Project

ADAPT is a cross-platform cognitive assistance platform with shared backend services for caregiver workflows, telemetry analysis, and assist-mode recommendations.

This README is the current delivery snapshot and runbook.

## Current Work Done

Core functionality completed in this repository:

- Shared backend modules delivered and integrated:
	- AI endpoints
	- Analysis endpoints
	- Task Lab endpoints
	- Cognitive endpoints
- Backend hardening added:
	- Helmet security headers
	- Global and auth-specific rate limiting
	- Configurable request body limits
	- Role-based route guards
- Database migration expanded for compatibility and new features:
	- Backward-compatible telemetry columns and backfill
	- Task Lab tables (`task_plans`, `task_plan_steps`)
- Web caregiver console delivered (Next.js):
	- Caregiver/admin auth flow
	- Task Lab template and AI draft workflows
	- Analysis overview and patient summary views
	- AI assistant chat panel
- Mobile application convergence delivered:
	- Updated caregiver-facing flows
	- Monitoring and reminder/escalation support
	- Assist mode enhancements
	- Task Lab and AI assistant wiring
- CI release hardening workflow added:
	- Workspace final gate
	- Backend security audit
	- Mobile artifact generation
	- Signed readiness job (when secrets are present)

## Repository Structure

```text
Project_ADAPT/
|- adapt/
|  |- backend/         # Express + TypeScript API
|  |- engine-service/  # Cognitive engine service
|  |- frontend/        # Next.js caregiver web app
|  |- scripts/         # Smart validation tooling
|  \- docker-compose.yml
|- Adapt_mobile_app/   # Native Android app (Gradle)
\- .github/workflows/  # CI pipelines
```

## Tech Stack

- Frontend: Next.js, React, TypeScript
- Backend: Node.js, Express, TypeScript, PostgreSQL
- Engine: Node.js, TypeScript
- Mobile: Android (Gradle)

## Prerequisites

- Node.js 18+
- npm
- Docker Desktop (optional but recommended for local PostgreSQL)
- Java 17 + Android Studio (for mobile)

## Quick Start (Web + API + Engine)

### 1) Install dependencies

```bash
cd adapt
npm run install:all
```

### 2) Configure backend environment

Create `adapt/backend/.env` from `adapt/backend/.env.example`.

### 3) Start PostgreSQL

```bash
cd adapt
npm run db:up
```

### 4) Start all services

```bash
cd adapt
npm run dev
```

Default local endpoints:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:3001`
- Backend health: `http://localhost:3001/health`
- Engine health: `http://localhost:4001/health`

## Validation Commands

From `adapt/`:

- Fast targeted check:
	- `npm run check:smart`
- Fast check with DB-aware migration step:
	- `npm run check:smart:db`
- Final release gate:
	- `npm run check:final`

`check:final` runs:

1. Full workspace build (backend + engine + frontend)
2. Backend migration
3. Backend seed

## Mobile Build Commands

From `Adapt_mobile_app/`:

- Debug build:
	- Windows: `gradlew.bat assembleDebug`
	- macOS/Linux: `./gradlew assembleDebug`
- Release readiness (after signing config):
	- Windows: `gradlew.bat checkReleaseReadiness`
	- macOS/Linux: `./gradlew checkReleaseReadiness`

## CI Release Hardening

Workflow: `.github/workflows/release-hardening.yml`

Jobs:

- Workspace final check
- Backend production dependency audit
- Mobile unsigned artifact generation
- Mobile signed readiness (conditional)

Required GitHub secrets for signed mobile readiness:

- `ADAPT_RELEASE_STORE_FILE_BASE64`
- `ADAPT_RELEASE_STORE_PASSWORD`
- `ADAPT_RELEASE_KEY_ALIAS`
- `ADAPT_RELEASE_KEY_PASSWORD`

## Current Pending Before Full Production Sign-off

- Configure signing secrets and pass signed mobile readiness job.
- Complete provider token verification hardening for social login flow.

## Seed Credentials (Local Development)

After running `npm run check:final` or `npm run db:prepare`, sample credentials are:

- `admin@adapt.local` / `password123`
- `caregiver1@adapt.local` / `password123`

## Notes

- Keep secrets in local env/CI secret stores only.
- Do not commit large binary files.

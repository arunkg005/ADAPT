# ADAPT Architecture Reference

Quick map of where core behavior lives.

## Workspace Components

- `backend/`: Express + TypeScript API + PostgreSQL access.
- `engine-service/`: rule evaluation service.
- `frontend/`: Next.js UI.
- `docker-compose.yml`: local PostgreSQL and single-image profile.
- `scripts/dev-runner.mjs`: launches engine/backend/frontend together.

## Backend Quick Index

- App bootstrap: `backend/src/index.ts`
- Env config: `backend/src/config.ts`
- DB pool: `backend/src/db/index.ts`
- DB migrate: `backend/src/db/migrate.ts`
- DB seed: `backend/src/db/seed.ts`
- Auth middleware + role guard: `backend/src/middleware/auth.ts`
- API router mount + docs: `backend/src/routes/index.ts`

### Backend Domain Routes

- Auth: `backend/src/routes/auth.ts`
- Patients: `backend/src/routes/patients.ts`
- Caregivers: `backend/src/routes/caregivers.ts`
- Devices: `backend/src/routes/devices.ts`
- Alerts: `backend/src/routes/alerts.ts`
- Telemetry: `backend/src/routes/telemetry.ts`

### Backend Services

- Auth service: `backend/src/services/authService.ts`
- Patient service: `backend/src/services/patientService.ts`
- Caregiver service: `backend/src/services/caregiverService.ts`
- Device service: `backend/src/services/deviceService.ts`
- Alert service: `backend/src/services/alertService.ts`
- Telemetry service: `backend/src/services/telemetryService.ts`
- Engine client service: `backend/src/services/engineService.ts`

## Engine Service Quick Index

- HTTP entry: `engine-service/src/index.ts`
- Core rule logic: `engine-service/src/engine.ts`
- Input/output types: `engine-service/src/types.ts`

## Frontend Quick Index

- Root layout: `frontend/app/layout.tsx`
- Global styles: `frontend/app/globals.css`
- Home route: `frontend/app/page.tsx`

## Mobile Quick Index

- API base URL config: `Adapt_mobile_app/app/build.gradle.kts`
- Network client: `Adapt_mobile_app/app/src/main/java/com/example/adapt/data/network/NetworkClient.java`
- Auth API interface: `Adapt_mobile_app/app/src/main/java/com/example/adapt/data/network/auth/AuthApiService.java`
- Login screen flow: `Adapt_mobile_app/app/src/main/java/com/example/adapt/ui/login/LoginActivity.java`
- Register screen flow: `Adapt_mobile_app/app/src/main/java/com/example/adapt/ui/login/RegisterActivity.java`
- Local repository layer: `Adapt_mobile_app/app/src/main/java/com/example/adapt/data/repository/AppRepository.java`

## Current Policy Decisions

- Registration model: hybrid
  - Public route for caregiver/patient self-signup
  - Admin-only route for provisioning any role

- Alert acknowledgment identity: caregivers-based
  - Acknowledgment writes caregiver id
  - Authenticated user is mapped to caregiver profile

## Fast Start Commands

From `adapt/`:

- Install deps: `npm run install:all`
- Start DB: `npm run db:up`
- Start all app services: `npm run dev`
- Quick validation: `npm run check:smart`
- Final validation: `npm run check:final`

## Keep This Doc Fresh

Update this reference whenever:

- an endpoint moves
- a service file is renamed
- auth/role policy changes
- startup or validation scripts change

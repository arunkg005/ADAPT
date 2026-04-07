# ADAPT Complete Documentation Snapshot

Last updated: 2026-04-07

## 1. Project Overview

ADAPT is a cross-platform cognitive assistance solution with shared backend services and multiple clients:

- Backend API: Node.js + Express + TypeScript + PostgreSQL
- Cognitive Engine: independent TypeScript service
- Web Console: Next.js caregiver-focused interface
- Mobile App: Android caregiver and assist-mode workflows

Primary objective: keep web and mobile aligned on one shared backend contract while preserving platform-specific UX.

## 2. Repository Structure

Top-level areas:

- `adapt/`: backend, engine service, frontend, scripts, and workspace tooling
- `Adapt_mobile_app/`: Android application
- `.github/workflows/`: CI workflows

Within `adapt/`:

- `backend/`: API routes, services, DB migration/seed
- `engine-service/`: evaluation logic and engine HTTP API
- `frontend/`: caregiver web app
- `scripts/`: smart validation tooling

## 3. Backend Capabilities

Implemented and available in current codebase:

- Authentication and role-aware access control
- Patients, caregivers, devices, alerts, telemetry APIs
- AI routes for caregiver chat and Task Lab draft generation
- Analysis routes for overview and patient summaries
- Cognitive routes for evaluation and assist-mode next action
- Task Lab routes for templates, plans, statuses, and step completion

Security and operational hardening included:

- Helmet headers
- Global and auth-specific rate limiting
- Configurable JSON request body limits
- Environment-based configuration safeguards

Database support includes:

- Core schema for users/patients/caregivers/devices/alerts/telemetry
- Backward-compatible telemetry schema patching
- Task Lab tables (`task_plans`, `task_plan_steps`)

## 4. Web Frontend (Caregiver Console)

Current web app behavior:

- Caregiver/admin login and registration flow
- Task Lab template creation and AI draft generation
- Plan publication/status updates
- Analysis overview and patient summary views
- AI assistant chat panel

Technical state:

- Lint clean
- Production build passing

## 5. Mobile Application

Major mobile modules integrated:

- Caregiver-first auth UI and flow updates
- Monitoring service with cloud telemetry publishing
- Reminder/Task Guardian worker with escalation behavior
- Assist mode enhancements (voice guidance, large text, restrictions)
- AI assistant activity and Task Lab integration hooks
- Patient workspace support in dashboard components
- Additional network DTO/contracts aligned with backend

Technical state:

- `assembleDebug` passing

## 6. CI/CD and Validation

Automated/available checks:

- Smart checks: component-targeted validation (`check:smart`, `check:smart:db`)
- Final workspace gate: full build + DB prepare (`check:final`)
- Release hardening workflow:
  - Workspace final check
  - Backend dependency audit
  - Mobile unsigned release artifact generation
  - Mobile signed release readiness when secrets exist

## 7. Release Notes and Current Readiness

Validated in this cycle:

- Backend build passing
- Engine build passing
- Frontend lint/build passing
- DB migration and seed passing
- Mobile debug build passing

Current known release dependency:

- Signed mobile release readiness requires configured signing secrets.

## 8. Required Secrets for Signed Mobile Readiness

Repository secrets expected by CI:

- `ADAPT_RELEASE_STORE_FILE_BASE64`
- `ADAPT_RELEASE_STORE_PASSWORD`
- `ADAPT_RELEASE_KEY_ALIAS`
- `ADAPT_RELEASE_KEY_PASSWORD`

## 9. Recommended Runbook

From `adapt/`:

1. `npm run db:up`
2. `npm run check:smart`
3. `npm run check:smart:db` (when DB schema/data changes)
4. `npm run check:final` before merge/release

For mobile:

1. `cd Adapt_mobile_app`
2. `gradlew.bat assembleDebug`
3. `gradlew.bat checkReleaseReadiness` (after signing secrets/config)

## 10. Next Priorities

Suggested short-term priorities:

- Finalize social login provider token verification hardening
- Finalize signed mobile release readiness in CI
- Continue UX validation passes for in-progress mobile modules
- Keep convergence smoke tests in release checklist

---

This file is intended to provide one consolidated, current-state documentation snapshot for handoff, validation, and release planning.

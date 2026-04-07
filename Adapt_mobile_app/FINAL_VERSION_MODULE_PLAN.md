# ADAPT Final Version Plan (Cloud-First)

This roadmap follows a cloud-first strategy now, then adds offline and rule-based fallback in later versions.

## Guiding Principle

- Build end-to-end cloud features first so all core workflows work reliably.
- Keep fallback hooks in architecture, but do not delay release for offline complexity.

## Module 1 (Completed): App Shell + Command Center

Goal:
- Stabilize navigation and top-level UX controls needed for all upcoming features.

Scope:
- Center Task Lab action in main shell.
- Floating AI assistant launcher above bottom navigation.
- Drawer controls for Assist Mode, AI assistant toggle, dark mode toggle, and About info.
- Persist settings in preferences.
- Add Task Lab and AI Assistant entry screens.

Definition of done:
- App navigates to all shell modules with no crashes.
- Drawer toggles persist and apply correctly.

## Module 2 (Completed): Cloud Telemetry + Diagnostic Pipeline

Goal:
- Continuously collect mobile sensor and connected IoT data and push to backend engine.

Scope:
- Expand background MonitoringService into scheduled telemetry producer.
- Normalize and send telemetry to backend endpoints.
- Trigger engine evaluation cycles and store diagnostic summaries.
- Surface key diagnostics in Status dashboard cards.

Definition of done:
- Sensor and IoT streams visible in backend and dashboard metrics.

## Module 3: Task Guardian Service (Routines, Notifications, Alerts)

Status:
- Completed

Goal:
- Keep tasks and routines active, supervised, and escalated.

Scope:
- Add reminder schedule orchestration for tasks/routines.
- Add missed-step detection and caregiver escalation alerts.
- Keep logs aligned with reminders and alerts.

Definition of done:
- Reminder and alert lifecycle works from creation to acknowledgment.

## Module 4: Caregiver Hero Dashboard + Patient Workspace

Status:
- In progress (hero dashboard, patient cards, and onboarding flow implemented; final UX validation pending)

Goal:
- Build a modern caregiver home with patient onboarding and live status.

Scope:
- Welcome card with profile avatar and personalized suggestions.
- Empty-state stencil card for add-patient flow.
- Patient cards with live sensor snapshots and performance analysis action.
- Add-patient form with IoT/sensor linkage.

Definition of done:
- Caregiver can onboard patient and monitor live summary from one screen.

## Module 5: Task Lab + AI Decision Assistant

Status:
- Completed (shared backend Task Lab APIs, AI draft generation endpoint, caregiver web Task Lab console, and mobile cloud sync hooks implemented)

Goal:
- Give caregivers a productive planner for task and routine generation.

Scope:
- Template-based task/routine builders.
- Prompt-to-plan AI assistant workflow.
- Task tab split into Task and Schedule sub-sections with redirects to Lab.

Definition of done:
- Caregiver can generate and publish tasks/routines quickly with AI support.

## Module 6: Logs Intelligence

Status:
- In progress (live search, patient grouping, and category filters implemented; UX tuning and validation pending)

Goal:
- Improve observability and filtering across patients and event types.

Scope:
- Search bar with live filtering.
- Patient-wise hierarchical logs.
- Categories: task/routine, alert, IoT/sensor.

Definition of done:
- Log retrieval and filtering is fast and accurate.

## Module 7: Auth UX and Social Sign-In

Status:
- In progress (caregiver-first login/signup redesign and social-login backend/mobile wiring implemented; provider SDK token verification hardening pending)

Goal:
- Modernize login/signup and make caregiver-first onboarding.

Scope:
- New login/signup UI.
- Remove caretaker/patient role prompt from signup.
- Add Google/Facebook auth integration.

Definition of done:
- Caregiver can authenticate via email or social providers.

## Module 8: Assist Mode 2.0

Status:
- Completed (assist profile setup, adaptive prompts, large-text mode, and routine-specific task restriction controls implemented and build-validated)

Goal:
- Deliver configurable, low-friction patient assistance in real time.

Scope:
- Pre-handover assist profile setup.
- Task-specific restriction controls.
- Voice-first and enlarged low-friction assist UI.
- Adaptive attention recovery prompts.

Definition of done:
- Assist sessions are configurable and usable in real caregiving scenarios.

## Module 9: Release Hardening and Deployment

Status:
- In progress (crash telemetry, security hardening, dependency audit cleanup, release artifact generation, and full workspace check:final validation completed; remaining blocker is signed-release secrets for verifyReleaseSigning/checkReleaseReadiness)
- Pending secrets: ADAPT_RELEASE_STORE_FILE, ADAPT_RELEASE_STORE_PASSWORD, ADAPT_RELEASE_KEY_ALIAS, ADAPT_RELEASE_KEY_PASSWORD.
- CI automation added: release-hardening workflow now validates workspace final gate, backend security audit, mobile unsigned artifacts, and signed readiness when secrets are configured.

Goal:
- Final production readiness.

Scope:
- Security and build validation.
- Monitoring and crash telemetry.
- Signed release artifacts and deployment checklist.

Definition of done:
- Deployable, test-validated release build.

## Module 10 (Completed): Cross-Platform Convergence Sprint

Goal:
- Keep web and mobile on one shared backend contract while preserving platform-specific capabilities.

Scope delivered:
- Caregiver-only web auth enforcement via platform-aware backend auth rules.
- Shared backend modules for AI, Analysis, Task Lab, and Cognitive APIs.
- Web caregiver console now supports:
	- caretaker-only login/register flow,
	- AI task draft generation,
	- template-based sophisticated todo/task-plan generation,
	- patient-level analysis summaries and overview metrics.
- Mobile wiring now supports:
	- cloud-first AI draft generation in AI Assistant with local fallback,
	- Task Lab cloud sync when templates/drafts are published,
	- Assist Mode cognitive next-action calls with local fallback hints.

Definition of done:
- Backend TypeScript build passes.
- Frontend production build passes.
- Android debug build passes.

## Fallback and Offline (Post-Release Version 2)

Planned later:
- Local rule-based assistant fallback if cloud AI is unavailable.
- Offline telemetry queue and delayed sync.
- Degraded-mode routine reminders without backend.

Reason for deferral:
- Keeps current release scope focused and stable while delivering all primary cloud workflows first.

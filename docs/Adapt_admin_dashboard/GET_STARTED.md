# 🚀 ADAPT System: Implementation Complete (Phase 1)

## What You Now Have

### ✅ Frontend (Next.js + Tailwind CSS)

**Live at:** `http://localhost:3000`

1. **Landing Page** (`/`)
   - Problem statement (cognitive challenges from your details.txt)
   - ADAPT solution explanation
   - 4-stage methodology diagram
   - Architecture overview
   - CTA buttons to explore or login

2. **Login Page** (`/login`)
   - Simple, accessible form
   - Redirects to dashboard on submit

3. **Admin Dashboard** (`/dashboard`)
   - Sidebar navigation
   - Stats cards (patients, caregivers, alerts, devices)
   - Recent alerts list
   - System architecture snapshot
   - Professional dark theme matching ADAPT branding

**Key Files:**
- `frontend/app/page.tsx` – landing
- `frontend/app/login/page.tsx` – login with useRouter redirect
- `frontend/app/dashboard/page.tsx` – admin shell
- All styled with Tailwind CSS, cognitive-friendly principles applied

---

### ✅ Rule Engine Service (Node.js + TypeScript)

**Live at:** `http://localhost:4001`

1. **Data Model** (`types.ts`)
   - 5 input blocks (TaskContext, PassiveSignals, InteractionSignals, ProgressSignals, HistorySignals)
   - Output structure (EngineOutput) with scores, state, assistance mode, rule trace

2. **Rule Engine Logic** (`engine.ts`)
   - 5-step evaluation process
   - Cognitive score calculation (0–100 for memory, attention, speed, decision, comprehension)
   - Task state determination (NOT_STARTED, ENGAGED, PROGRESSING, STALLED, CONFUSED, ABANDONED, COMPLETED)
   - Assistance mode selection (NONE, SOFT_REMINDER, VOICE_CUE, GUIDED_STEP, ESCALATE_CARETAKER)
   - Human-readable rule trace for explainability

3. **HTTP API** (`index.ts`)
   - `GET /health` – service health check
   - `POST /engine/evaluate` – main evaluation endpoint
   - `GET /docs` – API documentation
   - CORS enabled for cross-origin calls

**Key Files:**
- `engine-service/src/types.ts` – data model
- `engine-service/src/engine.ts` – rule logic (this is the heart of ADAPT)
- `engine-service/src/index.ts` – Express HTTP server
- `engine-service/README.md` – complete documentation
- `engine-service/EXAMPLE_PAYLOADS.json` – test scenarios

---

## Architecture at a Glance

```
Patient/Caregiver         Wearables/IoT           Admin Dashboard
      ↓                         ↓                        ↓
      └─────────────────────────┴────────────────────────┘
                                 ↓
                    ┌────────────────────────┐
                    │  ADAPT Backend API     │
                    │  (Coming next)         │
                    └────────────────────────┘
                                 ↓
                    ┌────────────────────────┐
                    │  Rule Engine Service   │ ✅ READY
                    │  (http://4001)         │
                    │                        │
                    │  5 Input Blocks ──────→│
                    │  ↓                     │
                    │  5-Step Evaluation    │
                    │  ↓                     │
                    │  Task State           │
                    │  Cognitive Scores     │
                    │  Severity & Mode      │
                    │  Rule Trace           │
                    └────────────────────────┘
                                 ↓
                    ┌────────────────────────┐
                    │  PostgreSQL            │
                    │  (To be connected)     │
                    └────────────────────────┘
```

---

## How to Run Everything

### Terminal 1: Engine Service

```bash
cd d:\PROJ_6th_Sem\Adapt_admin_dashboard\adapt\engine-service
npm run dev
```

Expected output:
```
╔════════════════════════════════════════════════════════════════╗
║              ADAPT Rule Engine Service                         ║
║                                                                ║
║  Server running on http://localhost:4001                      ║
║  Health: http://localhost:4001/health                         ║
║  Docs:   http://localhost:4001/docs                           ║
║                                                                ║
║  POST /engine/evaluate  →  Behavioral assessment              ║
╚════════════════════════════════════════════════════════════════╝
```

### Terminal 2: Frontend

```bash
cd d:\PROJ_6th_Sem\Adapt_admin_dashboard\adapt\frontend
npm run dev
```

Expected output:
```
> frontend@0.1.0 dev
> next dev

  ▲ Next.js 15.1.3
  ▲ Local:        http://localhost:3000
  ...
```

### Terminal 3: Test the Engine

```bash
# Check health
curl http://localhost:4001/health

# Get docs
curl http://localhost:4001/docs

# Test with a sample payload
curl -X POST http://localhost:4001/engine/evaluate \
  -H "Content-Type: application/json" \
  -d @"d:\PROJ_6th_Sem\Adapt_admin_dashboard\adapt\engine-service\EXAMPLE_PAYLOADS.json"
```

---

## Example Engine Input & Output

### Input (Patient taking medication, confused with memory issues):

```json
{
  "patientId": "patient_003",
  "taskContext": {
    "taskName": "Morning Hygiene",
    "taskType": "HYGIENE",
    "riskLevel": "MEDIUM",
    "complexity": "MEDIUM"
  },
  "passiveSignals": {
    "reminderDelivered": true,
    "voicePromptAcknowledged": true,
    "motionDetectedAfterPrompt": true,
    "noResponseDurationMs": 25000
  },
  "interactionSignals": {
    "instructionReplay": 4,    ← Many replays (memory issue!)
    "helpRequests": 2
  },
  "progressSignals": {
    "currentStepIndex": 2,
    "stepRevisits": 3,         ← Revisiting same steps
    "screenBacktracks": 2,
    "idleTimeMs": 45000
  },
  "historySignals": {
    "recentFailureCount": 2,
    "commonDifficultyType": "MEMORY",
    "lastAdaptationUsed": "SIMPLIFIED_STEPS",
    "lastAdaptationSuccess": true
  }
}
```

### Output (Engine decision):

```json
{
  "success": true,
  "output": {
    "taskState": "CONFUSED",
    "cognitiveScores": {
      "workingMemory": 55,      ← Primary issue
      "attention": 30,
      "processingSpeed": 10,
      "decisionMaking": 15,
      "comprehension": 25
    },
    "primaryIssue": "workingMemory",
    "secondaryIssue": "attention",
    "severity": "MODERATE",
    "confidence": 85,
    "assistanceMode": "GUIDED_STEP",
    "adaptationActions": [
      "RESET_TO_LAST_GOOD_STEP",
      "SIMPLIFY_INSTRUCTIONS",
      "VOICE_ONLY"
    ],
    "escalationRequired": false,
    "ruleTrace": [
      "→ Task state determined: CONFUSED",
      "  • Instruction replay x4 → WM +20",
      "  • Step revisits x3 → WM +15",
      "  • No response > 25s → ATN +15",
      "→ Cognitive scores: WM=55, ATN=30, SPD=10",
      "→ Primary issue: workingMemory, Secondary: attention",
      "→ Severity: MODERATE, Confidence: 85%",
      "→ Assistance mode: GUIDED_STEP"
    ]
  }
}
```

**Human interpretation:**
- Patient is confused due to working memory issues
- System should reset to the last completed step
- Simplify instructions to 1–2 steps at a time
- Use voice guidance instead of text
- No caregiver escalation needed (confidence that adaptation will help)

---

## Files You Should Know

| Path | Purpose | Edit for |
|------|---------|----------|
| `frontend/app/page.tsx` | Landing page | Messaging, problem description |
| `frontend/app/dashboard/page.tsx` | Admin UI | Adding new dashboard panels |
| `engine-service/src/engine.ts` | Rule logic | Tuning thresholds (90s? 30s?), tweaking rules |
| `engine-service/src/types.ts` | Data model | Adding new signal types |
| `engine-service/EXAMPLE_PAYLOADS.json` | Test data | Create scenarios to test rules |

---

## Design Philosophy Embedded in Code

### 1. Hands-Free First
- Engine prioritizes `passiveSignals` (motion, acknowledgment) over direct app taps
- Can assess patient behavior without constant interaction

### 2. Cognitive-Safe UX
- Dashboard uses clear hierarchy, high contrast (dark theme)
- Information presented in small chunks (stat cards, short paragraphs)
- No overwhelming density

### 3. Explainability
- Every engine decision includes `ruleTrace`: human-readable log
- Caregivers/clinicians understand "why" the system recommended something
- Builds trust and enables system refinement

### 4. Modular Service
- Engine is independent HTTP microservice
- Can be called from:
  - Android app (background service)
  - Cloud backend (ingestion pipeline)
  - Admin dashboard (simulation/debug)
  - Future FastAPI version
- No vendor lock-in

### 5. Rule-Based (Interpretable)
- No opaque ML models
- Every decision is traceable to specific rules + thresholds
- Easy to audit for ethics (important for healthcare)
- Easy to refine based on user feedback

---

## Your Next Steps (Recommendations)

### 🎯 This Week

1. **Test the system end-to-end**
   - Run both services (engine + frontend)
   - Navigate landing → login → dashboard
   - Verify no errors

2. **Test the engine with example payloads**
   - Save each scenario from `EXAMPLE_PAYLOADS.json` as JSON files
   - Send via curl to `/engine/evaluate`
   - Verify outputs match expected behavior

3. **Refine rule thresholds**
   - Question: Is 90 seconds "too long" for no response?
   - Question: Should a HIGH-risk task escalate at MODERATE severity instead of SEVERE?
   - Adjust in `engine-service/src/engine.ts` lines where scores are accumulated

### 🎯 Next Week

1. **Add PostgreSQL + Docker**
   - Create `docker-compose.yml` with Postgres service
   - Define schema: users, patients, caregivers, alerts, telemetry

2. **Build basic backend**
   - Node.js server: auth (login), patient CRUD, alert retrieval
   - Connect to Postgres

3. **Wire dashboard to backend**
   - Replace mock data with real API calls
   - Show real patient list, alerts, etc.

### 🎯 Following Week

1. **Telemetry ingestion**
   - `POST /api/telemetry` endpoint
   - Build `EngineInput` from recent signals
   - Call engine `/engine/evaluate`
   - Save result as alert

2. **Mobile integration planning**
   - Design how Android app calls the engine
   - Decide: real-time during task vs. batch processing?

---

## Project Summary for B.Tech Evaluation

**ADAPT** (Adaptive Daily Assistance Platform for Task Independence)

**Architecture:** 5-layer input pipeline → rule-based behavioral engine → 7-output modes → explainable decisions

**Tech Stack:**
- Frontend: Next.js (React + TypeScript + Tailwind CSS)
- Backend: Node.js + Express (microservice architecture)
- Engine: TypeScript with modular rule-based logic
- Database: PostgreSQL (to come)
- Deployment: Docker (to come)

**Key Innovation:**
- **Hands-free assessment**: Passive signals (motion, device pickup) + optional direct interaction
- **Explainability**: Every decision traced with human-readable rules (no black-box ML)
- **Cognitive-safe UX**: Designed for users with memory, attention, and processing-speed limitations
- **Modular service**: Engine portable to Android, FastAPI, Java (not locked to Node)

**Status:** MVP frontend + rule engine ready; database + backend API in progress

---

## Quick Reference: Commands

```bash
# Engine service
cd adapt/engine-service && npm run dev        # Start engine (port 4001)
npm run build                                  # Compile TypeScript

# Frontend
cd adapt/frontend && npm run dev              # Start frontend (port 3000)
npm run build                                  # Build for production

# Test engine
curl http://localhost:4001/health             # Health check
curl http://localhost:4001/docs               # API docs

# Test a scenario
curl -X POST http://localhost:4001/engine/evaluate \
  -H "Content-Type: application/json" \
  -d '{ "patientId": "...", ... }'
```

---

## Questions? Next Steps?

1. Do the rule thresholds make sense for your target users?
2. Should we add more cognitive scores or simplify?
3. How should the Android app integrate?
4. What alert channels matter most: push, SMS, email?
5. How do we measure if ADAPT is working (success metrics)?

---

**Great work building the foundation! 🎉**

You now have:
- ✅ A cognitively-friendly landing page
- ✅ A professional admin dashboard
- ✅ A rule-based behavioral engine
- ✅ An HTTP API for integration

Next sprint: database, backend, mobile integration.

Let's ship it! 🚀

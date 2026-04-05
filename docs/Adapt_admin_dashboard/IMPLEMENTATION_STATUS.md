# ADAPT Project: Complete Architecture & Implementation Status

## Project Structure

```
d:\PROJ_6th_Sem\Adapt_admin_dashboard\
  ADAPT_synopsis.txt           ← Project overview
  detials.txt                  ← Cognitive challenges document
  New Text Document.txt        ← Architecture diagram
  
  adapt/
    frontend/                  ✅ DONE
      app/
        page.tsx               → Landing page (problem → ADAPT → CTA)
        login/page.tsx         → Login form
        dashboard/page.tsx     → Admin dashboard shell
      package.json
    
    engine-service/            ✅ DONE (THIS SPRINT)
      src/
        types.ts               → Data model (EngineInput, EngineOutput)
        engine.ts              → Rule logic (5-step evaluation)
        index.ts               → Express HTTP server
      package.json
      README.md                → Complete documentation
```

## What We've Built This Sprint

### 1. Frontend (Next.js + Tailwind)

**Landing Page** (`/`)
- Hero with problem statement
- Gradual reveal: problem → ADAPT solution → methodology → architecture
- Call-to-action: explore or login

**Login Page** (`/login`)
- Simple email/password form
- Client-side redirect to `/dashboard` on submit (no real auth yet)

**Admin Dashboard** (`/dashboard`)
- Sidebar with navigation
- Top bar with page title
- Stats cards: patients, caregivers, alerts, devices
- Recent alerts list (prototype data)
- System architecture snapshot
- Cognitive-friendly UI (clear hierarchy, limited info density)

### 2. Rule Engine Service (Node.js + TypeScript)

**Data Model** (`types.ts`)
- 5 input blocks:
  - `TaskContext`: what task?
  - `PassiveSignals`: hands-free observation
  - `InteractionSignals`: app interactions (optional)
  - `ProgressSignals`: task advancement
  - `HistorySignals`: repeating patterns?
- Output: `EngineOutput` with scores, state, severity, assistance mode, rule trace

**Rule Engine Logic** (`engine.ts`)
- 5-step evaluation:
  1. Determine task state (NOT_STARTED, ENGAGED, PROGRESSING, STALLED, CONFUSED, ABANDONED, COMPLETED)
  2. Score cognitive difficulty (0–100 for each: memory, attention, speed, decision, comprehension)
  3. Identify primary & secondary issues
  4. Assess severity & confidence
  5. Select assistance mode (NONE, SOFT_REMINDER, VOICE_CUE, GUIDED_STEP, ESCALATE_CARETAKER)
- Generates human-readable rule trace (explainability)

**HTTP Server** (`index.ts`)
- `GET /health` → service status
- `POST /engine/evaluate` → main evaluation endpoint
- `GET /docs` → API documentation
- CORS enabled for cross-origin calls

## How to Run

### Frontend (Next.js)

```bash
cd adapt/frontend
npm run dev
```

Open `http://localhost:3000`
- `/` → landing page
- `/login` → login form (try any email/password)
- `/dashboard` → admin dashboard

### Engine Service

```bash
cd adapt/engine-service
npm install
npm run dev
```

Engine runs on `http://localhost:4001`

Test with curl:

```bash
curl http://localhost:4001/health

curl -X POST http://localhost:4001/engine/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient_001",
    "taskContext": {...},
    "passiveSignals": {...},
    ...
  }'
```

## Design Principles Applied

### 1. Hands-Free First
- Passive signals (device pickup, motion) > direct app input
- Engine doesn't depend only on button taps
- Works with background observation

### 2. Cognitive-Friendly UX
- Clear visual hierarchy
- Limited simultaneous information
- Short, interpretable descriptions
- Accessible design (large text, high contrast)

### 3. Explainability (for B.Tech & caregivers)
- Every decision traced with human-readable rules
- Caregivers/clinicians can understand "why"
- Builds trust in the system

### 4. Modularity
- Engine is independent service (can be ported to Android, FastAPI, Java)
- Can be called from different clients:
  - Patient app (background service)
  - Backend ingestion pipeline
  - Admin dashboard (simulation/debug)

### 5. Rule-Based (not ML)
- No black boxes
- Thresholds are tunable
- Rules are interpretable
- Easy to audit for ethics

---

## Next Steps (Recommended Order)

### Phase 1: Connect Frontend to Engine (This Week)

- [ ] Add "Test Engine" button to dashboard
- [ ] Build a form to manually input `EngineInput` JSON
- [ ] Call `/engine/evaluate` endpoint
- [ ] Display `EngineOutput` and rule trace on dashboard
- [ ] Use this as a **debugging & demo tool**

**Why?** Lets you test the engine without mobile/backend integration yet.

### Phase 2: Database + Simple Backend (Next Week)

- [ ] Add PostgreSQL via Docker
- [ ] Create tables: `users`, `patients`, `caregivers`, `devices`, `alerts`
- [ ] Build simple Node backend:
  - `POST /api/auth/login` → JWT
  - `GET /api/patients` → list
  - `POST /api/patients` → create
  - etc.
- [ ] Connect dashboard to backend (replace mock data)

### Phase 3: IoT Ingestion Pipeline (Week After)

- [ ] Build `/api/telemetry` endpoint to receive sensor data
- [ ] Queue incoming telemetry
- [ ] Build telemetry processor:
  - Build `EngineInput` from recent telemetry + patient history
  - Call engine `/engine/evaluate`
  - Save `EngineOutput` as alert in DB
  - Notify caregiver if needed
  
### Phase 4: Mobile Integration

- [ ] Android app calls engine during task execution
- [ ] Passes real-time signals as user performs task
- [ ] Engine provides guidance + escalation

### Phase 5: Advanced Features

- [ ] Real auth (password hashing, JWT refresh)
- [ ] FastAPI microservice for future ML models
- [ ] WebSocket for real-time alerts
- [ ] Analytics dashboard (trends, risk heatmaps)

---

## Key Files to Know

| File | Purpose |
|------|---------|
| `frontend/app/page.tsx` | Landing page (edit to refine messaging) |
| `frontend/app/login/page.tsx` | Login form |
| `frontend/app/dashboard/page.tsx` | Admin dashboard (edit to add features) |
| `engine-service/src/types.ts` | Data model (study these interfaces) |
| `engine-service/src/engine.ts` | Rule logic (tune thresholds here) |
| `engine-service/src/index.ts` | HTTP server (add new endpoints here) |

---

## Testing the System End-to-End (Now)

### 1. Open two terminals

**Terminal 1: Engine service**
```bash
cd adapt/engine-service
npm run dev
```

**Terminal 2: Frontend**
```bash
cd adapt/frontend
npm run dev
```

### 2. Open browser to `http://localhost:3000`

- See the landing page ✓
- Click "go to the dashboard login" → `/login`
- Enter any email/password, click "Continue"
- Should redirect to `/dashboard` and show dashboard shell

### 3. In a third terminal, test the engine

```bash
curl http://localhost:4001/health
```

You should see the service is up.

### 4. (Coming next) Add a "Test Engine" panel to the dashboard

Then you can interactively test the rule engine from the UI.

---

## Philosophy Summary

**ADAPT** is built around one core idea:

> Don't expect humans to adapt to systems. Build systems that adapt to humans.

This manifests as:

1. **For patients**: clear step-by-step guidance, hands-free when possible, minimal cognitive load
2. **For caregivers**: explainable alerts, human-readable rule traces, actionable recommendations
3. **For clinicians**: objective functional measurement (not just tests), longitudinal tracking
4. **For developers**: modular, rule-based, portable, testable

---

## Questions to Consider for Next Sprint

1. **Rule tuning**: What thresholds make sense for YOUR patient population?
   - How long is "too long" to not respond (90s? 30s?)?
   - What's a normal response time for your users?
   - How many replays before confusion?

2. **Adaptation actions**: What should the system actually DO?
   - Speak a simplified version?
   - Call caregiver automatically?
   - Offer a different approach?

3. **Mobile integration**: How will Android app call the engine?
   - HTTP to backend-hosted service?
   - Embedded logic?
   - Hybrid?

4. **Escalation**: Who/how should caregiver be alerted?
   - Push notification?
   - SMS?
   - Email?
   - In-app alert?

---

**Status**: ✅ Architecture designed, ✅ Engine logic implemented, ✅ Frontend UI created

**Ready for**: Integration testing, rule refinement, real data

Let's build the next layer! 🚀

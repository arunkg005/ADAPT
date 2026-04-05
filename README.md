# ADAPT Project

ADAPT is a multi-component platform for cognitive assistance and caregiver support. This repository includes:

- Web dashboard and API stack under adapt/
- Android mobile application under Adapt_mobile_app/
- Project documentation under docs/

## Repository Structure

```text
Project_ADAPT/
|- adapt/
|  |- backend/         # Express + TypeScript API
|  |- engine-service/  # Rule engine service
|  |- frontend/        # Next.js application
|  |- docker-compose.yml
|  |- setup_database.sql
|  \- start_all.sh
|- Adapt_mobile_app/   # Native Android app (Gradle)
|- docs/               # Supporting docs and summaries
\- .gitignore
```

## Tech Stack

- Frontend: Next.js, React, TypeScript
- Backend: Node.js, Express, TypeScript, PostgreSQL
- Engine: Node.js, Express, TypeScript
- Mobile: Android (Java/Kotlin + Gradle)

## Prerequisites

Install the following before running locally:

- Node.js 18 or newer
- npm
- Docker Desktop (optional, for containerized DB/app runs)
- Java 17 and Android Studio (for mobile app)

## Quick Start (Web + API + Engine)

### 1) Configure backend environment

From adapt/backend, create .env from .env.example and adjust values if needed.

Example values:

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
FRONTEND_URL=http://localhost:3000
```

### 2) Install dependencies

Preferred (single command from dependency manager file):

```bash
cd adapt
npm run install:all
```

Manual alternative:

```bash
cd adapt/backend && npm install
cd ../engine-service && npm install
cd ../frontend && npm install
```

### 3) Start PostgreSQL

If you have local PostgreSQL installed, you can use that and skip Docker here.

```bash
cd ..
docker compose up -d
```

### 4) Start services (separate terminals)

Terminal A (engine):

```bash
cd adapt/engine-service
npm run dev
```

Terminal B (backend):

```bash
cd adapt/backend
npm run migrate
npm run seed
npm run dev
```

Terminal C (frontend):

```bash
cd adapt/frontend
npm run dev
```

### 5) Verify local endpoints

- Frontend: http://localhost:3000
- Backend health: http://localhost:3001/health
- Engine health: http://localhost:4001/health

## Single Docker Image Stack (Frontend + Backend + Engine in one image)

From `adapt/`, run:

```bash
docker compose --profile single-image up -d --build
```

Verify:

```bash
docker compose --profile single-image ps
```

View logs:

```bash
docker compose --profile single-image logs -f adapt-app
```

Stop:

```bash
docker compose --profile single-image down
```

Notes:
- This mode runs one app container (`adapt_app`) plus the existing PostgreSQL container.
- `docker compose up -d` still starts only PostgreSQL (DB behavior unchanged).
- This Docker setup is scoped to `adapt/` and does not touch `Adapt_mobile_app/` or Android Studio/Gradle workflows.
- Docker is optional for development; it is not a mandatory core requirement if your team uses a local PostgreSQL setup.

## Useful Scripts

### adapt/backend

- npm run dev
- npm run build
- npm run start
- npm run typecheck
- npm run migrate
- npm run seed

### adapt/engine-service

- npm run dev
- npm run build
- npm run start

### adapt/frontend

- npm run dev
- npm run build
- npm run start
- npm run lint

## Android App (Adapt_mobile_app)

1. Open Adapt_mobile_app/ in Android Studio.
2. Let Gradle sync complete.
3. Select an emulator or connected device.
4. Run the app module.

CLI build option:

```bash
cd Adapt_mobile_app
./gradlew assembleDebug
```

On Windows cmd:

```bat
cd Adapt_mobile_app
gradlew.bat assembleDebug
```

## Documentation

- Main web stack notes: adapt/DOCUMENTATION.md
- Admin dashboard notes: docs/Adapt_admin_dashboard/GET_STARTED.md

## Notes

- Do not commit large binary archives (GitHub blocks files larger than 100 MB).
- Keep secrets out of git; use local .env files for credentials.

# ADAPT Web App Local Setup Guide

This guide helps a teammate run the web stack from a fresh clone.

## 1) Prerequisites

- Node.js 20 or newer
- npm 10 or newer
- One database option:
  - Local PostgreSQL, or
  - Docker Desktop

## 2) Go To Project Folder

From the repository root:

Windows Command Prompt (cmd):

```bat
cd adapt
```

macOS/Linux Terminal:

```bash
cd adapt
```

## 3) Install Dependencies

Preferred single command:

Windows Command Prompt (cmd):

```bat
npm run install:all
```

macOS/Linux Terminal:

```bash
npm run install:all
```

This installs dependencies for:
- backend
- engine-service
- frontend

## 4) Configure Backend Environment

Create backend/.env from backend/.env.example.

Minimum values:

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

## 5) Start Database

Option A (recommended for onboarding): Docker database

Windows Command Prompt (cmd):

```bat
docker compose up -d
```

macOS/Linux Terminal:

```bash
docker compose up -d
```

Option B: local PostgreSQL
- Create database/user that match backend/.env.
- You can use adapt/setup_database.sql as a base script.

## 6) Run Services In Separate Terminals

Terminal 1 (engine-service):

Windows Command Prompt (cmd):

```bat
npm --prefix engine-service run dev
```

macOS/Linux Terminal:

```bash
npm --prefix engine-service run dev
```

Terminal 2 (backend):

Windows Command Prompt (cmd):

```bat
npm --prefix backend run migrate
npm --prefix backend run seed
npm --prefix backend run dev
```

macOS/Linux Terminal:

```bash
npm --prefix backend run migrate
npm --prefix backend run seed
npm --prefix backend run dev
```

Terminal 3 (frontend):

Windows Command Prompt (cmd):

```bat
npm --prefix frontend run dev
```

macOS/Linux Terminal:

```bash
npm --prefix frontend run dev
```

## 7) Verify Endpoints

- Frontend: http://localhost:3000
- Backend health: http://localhost:3001/health
- Engine health: http://localhost:4001/health

## 8) Optional: Run Single Image App Container

If you want app services containerized:

Windows Command Prompt (cmd):

```bat
npm run single-image:up
```

macOS/Linux Terminal:

```bash
npm run single-image:up
```

Stop it:

Windows Command Prompt (cmd):

```bat
npm run single-image:down
```

macOS/Linux Terminal:

```bash
npm run single-image:down
```

## 9) Useful Notes

- Docker is optional for development. The key requirement is a reachable PostgreSQL database.
- If ports 3000/3001/4001/5432 are already used, stop the conflicting process first.
- Do not commit backend/.env.

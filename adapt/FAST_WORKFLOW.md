# ADAPT Fast Workflow

This guide is optimized for speed during day-to-day development.

## Goal

- Avoid full builds on every small edit.
- Run only the checks needed for files that changed.
- Keep full validation for milestone/end checkpoints.

## Fast Commands

Run these from the adapt workspace root.

- `npm run dev`
  - Starts engine, backend, and frontend together.

- `npm run check:smart`
  - Runs targeted checks based on changed files in `adapt/`.
  - Backend changes -> backend build.
  - Engine changes -> engine build.
  - Frontend changes -> frontend lint.
  - Workspace infra changes (scripts/docker/root package) -> all three checks.

- `npm run check:smart:db`
  - Same as `check:smart`, plus backend migration if DB-related files changed.

- `npm run check:final`
  - Full gate at the end of a milestone:
    - build all components
    - run backend migration + seed (`npm run db:prepare`)

- `npm run db:prepare`
  - Deterministically prepares backend database state:
    - run migration
    - run seed

## Recommended Validation Cadence

### 1) Inner Loop (very fast)

- Make a small logical batch of edits.
- Run `npm run check:smart`.
- Continue coding.

### 2) DB or schema updates

- After DB-related edits, run `npm run check:smart:db`.

### 3) Before merge / handoff

- Run `npm run check:final` once.

## Notes

- `check:smart` detects changes from `git status --porcelain`.
- If you want to force checks for all components, run:
  - `node scripts/smart-check.mjs --all`
- If you want all checks plus DB migration regardless of diff, run:
  - `node scripts/smart-check.mjs --all --with-db`

## Why this is faster

- Most edits affect one component, not all three.
- Targeted checks reduce repeated wait time.
- Full checks happen only at the right checkpoint.

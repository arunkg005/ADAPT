import { spawnSync } from "node:child_process";
import process from "node:process";

const args = new Set(process.argv.slice(2));
const runAll = args.has("--all");
const withDb = args.has("--with-db");

function runCommand(command, commandArgs, cwd) {
  const result = spawnSync(command, commandArgs, {
    cwd,
    stdio: "inherit",
    env: process.env,
  });

  return result.status ?? 1;
}

function runNpm(npmArgs, cwd) {
  if (process.platform === "win32") {
    return runCommand("cmd", ["/c", "npm", ...npmArgs], cwd);
  }

  return runCommand("npm", npmArgs, cwd);
}

function capture(command, commandArgs, cwd) {
  const result = spawnSync(command, commandArgs, {
    cwd,
    stdio: ["ignore", "pipe", "pipe"],
    encoding: "utf8",
    env: process.env,
  });

  return {
    ok: result.status === 0,
    stdout: result.stdout || "",
    stderr: result.stderr || "",
  };
}

function parseGitStatusLine(line) {
  if (!line || line.length < 4) {
    return null;
  }

  let path = line.slice(3).trim();
  const renameIndex = path.indexOf(" -> ");
  if (renameIndex >= 0) {
    path = path.slice(renameIndex + 4).trim();
  }

  return path.replace(/\\/g, "/");
}

function toAdaptRelative(repoPath) {
  if (!repoPath.startsWith("adapt/")) {
    return null;
  }

  return repoPath.slice("adapt/".length);
}

function getTouchedScopes(changedPaths) {
  const touched = {
    backend: false,
    engine: false,
    frontend: false,
    db: false,
    workspaceInfra: false,
  };

  for (const path of changedPaths) {
    if (path.startsWith("backend/")) {
      touched.backend = true;
    }

    if (path.startsWith("engine-service/")) {
      touched.engine = true;
    }

    if (path.startsWith("frontend/")) {
      touched.frontend = true;
    }

    if (path.startsWith("backend/src/db/") || path === "setup_database.sql") {
      touched.db = true;
      touched.backend = true;
    }

    if (
      path === "package.json" ||
      path === "docker-compose.yml" ||
      path === "Dockerfile.single" ||
      path.startsWith("docker/") ||
      path.startsWith("scripts/")
    ) {
      touched.workspaceInfra = true;
    }
  }

  if (touched.workspaceInfra) {
    touched.backend = true;
    touched.engine = true;
    touched.frontend = true;
  }

  return touched;
}

function printHeader(message) {
  console.log("\n============================================================");
  console.log(message);
  console.log("============================================================");
}

const adaptRoot = process.cwd();
const repoRootResult = capture("git", ["rev-parse", "--show-toplevel"], adaptRoot);

if (!repoRootResult.ok) {
  console.error("[smart-check] Unable to locate git repository root.");
  process.exit(1);
}

const repoRoot = repoRootResult.stdout.trim();
const statusResult = capture("git", ["-C", repoRoot, "status", "--porcelain"], adaptRoot);

if (!statusResult.ok) {
  console.error("[smart-check] Unable to read git status.");
  if (statusResult.stderr.trim()) {
    console.error(statusResult.stderr.trim());
  }
  process.exit(1);
}

const repoChangedPaths = statusResult.stdout
  .split(/\r?\n/)
  .map(parseGitStatusLine)
  .filter(Boolean);

const adaptChangedPaths = repoChangedPaths
  .map(toAdaptRelative)
  .filter(Boolean);

const touched = runAll
  ? {
      backend: true,
      engine: true,
      frontend: true,
      db: withDb,
      workspaceInfra: true,
    }
  : getTouchedScopes(adaptChangedPaths);

if (!touched.backend && !touched.engine && !touched.frontend) {
  printHeader("Smart Check: no changed files under adapt/ requiring code validation");
  console.log("Tip: use --all to force checks for all components.");
  process.exit(0);
}

const steps = [];

if (touched.backend) {
  steps.push({
    label: "Backend build",
    run: () => runNpm(["--prefix", "backend", "run", "build"], adaptRoot),
  });
}

if (touched.engine) {
  steps.push({
    label: "Engine build",
    run: () => runNpm(["--prefix", "engine-service", "run", "build"], adaptRoot),
  });
}

if (touched.frontend) {
  steps.push({
    label: "Frontend lint",
    run: () => runNpm(["--prefix", "frontend", "run", "lint"], adaptRoot),
  });
}

if (withDb && touched.db) {
  steps.push({
    label: "Backend migration",
    run: () => runNpm(["--prefix", "backend", "run", "migrate"], adaptRoot),
  });
}

printHeader("Smart Check: starting targeted validation");
console.log("Changed files considered:");
for (const path of adaptChangedPaths) {
  console.log(` - ${path}`);
}

for (const step of steps) {
  console.log(`\n[smart-check] Running: ${step.label}`);
  const exitCode = step.run();
  if (exitCode !== 0) {
    console.error(`[smart-check] Failed: ${step.label}`);
    process.exit(exitCode);
  }
}

printHeader("Smart Check: completed successfully");
if (withDb && !touched.db) {
  console.log("DB check requested, but no DB-related files were changed.");
}

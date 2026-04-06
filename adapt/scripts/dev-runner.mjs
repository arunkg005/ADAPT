import { spawn } from "node:child_process";
import process from "node:process";

const services = [
  { name: "engine", path: "engine-service", color: "\x1b[36m" },
  { name: "backend", path: "backend", color: "\x1b[32m" },
  { name: "frontend", path: "frontend", color: "\x1b[33m" }
];

const reset = "\x1b[0m";
const children = [];
let shuttingDown = false;
let exitCode = 0;
let remaining = services.length;

function writePrefixed(color, name, chunk, target) {
  const text = chunk.toString();
  const lines = text.split(/\r?\n/);

  for (const line of lines) {
    if (!line) {
      continue;
    }

    target.write(`${color}[${name}]${reset} ${line}\n`);
  }
}

function shutdown() {
  if (shuttingDown) {
    return;
  }

  shuttingDown = true;

  for (const child of children) {
    if (child.exitCode !== null || !child.pid) {
      continue;
    }

    if (process.platform === "win32") {
      spawn("taskkill", ["/pid", String(child.pid), "/t", "/f"], {
        stdio: "ignore"
      });
    } else {
      child.kill("SIGTERM");
    }
  }
}

for (const service of services) {
  const child =
    process.platform === "win32"
      ? spawn(`npm --prefix ${service.path} run dev`, {
          shell: true,
          env: process.env,
          windowsHide: false
        })
      : spawn("npm", ["--prefix", service.path, "run", "dev"], {
          env: process.env,
          windowsHide: false
        });

  children.push(child);

  child.stdout?.on("data", (chunk) => {
    writePrefixed(service.color, service.name, chunk, process.stdout);
  });

  child.stderr?.on("data", (chunk) => {
    writePrefixed(service.color, service.name, chunk, process.stderr);
  });

  child.on("exit", (code, signal) => {
    remaining -= 1;

    if (!shuttingDown && code !== 0) {
      exitCode = code ?? 1;
      console.error(
        `\n[${service.name}] exited with code ${code ?? "unknown"}${
          signal ? ` (signal ${signal})` : ""
        }. Stopping all services...`
      );
      shutdown();
    }

    if (shuttingDown && remaining === 0) {
      process.exit(exitCode);
    }
  });
}

process.on("SIGINT", () => {
  shutdown();
});

process.on("SIGTERM", () => {
  shutdown();
});

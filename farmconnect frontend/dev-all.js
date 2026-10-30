import { spawn } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const isWindows = process.platform === "win32";
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const backendDir = path.resolve(__dirname, "..", "farmconnect backend");
const frontendDir = __dirname;

const frontendCommand = isWindows ? "cmd.exe" : "npm";
const frontendArgs = isWindows
  ? ["/d", "/s", "/c", "npm.cmd run dev:web -- --host 0.0.0.0"]
  : ["run", "dev:web", "--", "--host", "0.0.0.0"];

const backendCommand = isWindows ? "cmd.exe" : "./mvnw";
const backendArgs = isWindows
  ? ["/d", "/s", "/c", "mvnw.cmd spring-boot:run"]
  : ["spring-boot:run"];

const children = [
  spawn(backendCommand, backendArgs, { cwd: backendDir, stdio: "inherit" }),
  spawn(frontendCommand, frontendArgs, { cwd: frontendDir, stdio: "inherit" }),
];

let stopping = false;
function stop(code = 0) {
  if (stopping) return;
  stopping = true;
  for (const child of children) if (child.exitCode === null) child.kill();
  process.exitCode = code;
}

for (const child of children) child.on("exit", (code) => stop(code ?? 1));
for (const signal of ["SIGINT", "SIGTERM"]) process.on(signal, () => stop(0));

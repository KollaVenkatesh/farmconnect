import { spawn } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const isWindows = process.platform === "win32";
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const backendDir = path.resolve(__dirname, "..", "farmconnect backend");
const command = isWindows ? "cmd.exe" : "./mvnw";
const args = isWindows
  ? ["/d", "/s", "/c", "mvnw.cmd spring-boot:run"]
  : ["spring-boot:run"];
const child = spawn(command, args, {
  cwd: backendDir,
  stdio: "inherit",
});

child.on("exit", (code) => {
  process.exitCode = code ?? 1;
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => child.kill(signal));
}

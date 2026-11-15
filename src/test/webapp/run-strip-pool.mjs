import { execFile } from "node:child_process";
import { createReadStream, existsSync } from "node:fs";
import { mkdtemp, rm } from "node:fs/promises";
import { createServer } from "node:http";
import { tmpdir } from "node:os";
import { dirname, extname, join, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "../../..");
const browser = process.env.CHROME_BIN || join(
    process.env["ProgramFiles(x86)"] || "C:\\Program Files (x86)",
    "Microsoft", "Edge", "Application", "msedge.exe"
);
if (!existsSync(browser)) {
    throw new Error("Set CHROME_BIN to a Chromium-compatible browser executable.");
}

const mimeTypes = {
    ".html": "text/html",
    ".js": "text/javascript",
    ".css": "text/css",
    ".svg": "image/svg+xml"
};
const server = createServer((request, response) => {
    const pathname = new URL(request.url, "http://localhost").pathname;
    if (!pathname.startsWith("/src/main/webapp/") && !pathname.startsWith("/src/test/webapp/")) {
        response.writeHead(404).end();
        return;
    }
    const file = resolve(root, `.${decodeURIComponent(pathname)}`);
    if (!file.startsWith(root + sep)) {
        response.writeHead(403).end();
        return;
    }
    const stream = createReadStream(file);
    stream.on("error", error => {
        if (error.code !== "ENOENT") console.error("Test server:", error);
        response.writeHead(error.code === "ENOENT" ? 404 : 500).end();
    });
    response.setHeader("Content-Type", mimeTypes[extname(file)] || "application/octet-stream");
    stream.pipe(response);
});

const profile = await mkdtemp(join(tmpdir(), "mixer-strip-test-"));
try {
    await new Promise((resolveListen, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", resolveListen);
    });
    for (const test of ["strip-pool.html", "detail-layout.html"]) {
        const url = `http://127.0.0.1:${server.address().port}/src/test/webapp/${test}`;
        const { stdout } = await promisify(execFile)(browser, [
            "--headless", "--disable-gpu", "--no-first-run", "--disable-extensions",
            `--user-data-dir=${profile}`, "--virtual-time-budget=10000", "--dump-dom", url
        ], { timeout: 30000, maxBuffer: 4 * 1024 * 1024 });
        const result = stdout.match(/<pre id="result">([\s\S]*?)<\/pre>/)?.[1];
        if (result !== "PASS") throw new Error(`${test} failed: ${result || "no result"}`);
        console.log(`${test}: PASS`);
    }
} finally {
    if (server.listening) await new Promise((resolveClose, reject) =>
        server.close(error => error ? reject(error) : resolveClose()));
    await rm(profile, { recursive: true, force: true, maxRetries: 3, retryDelay: 200 });
}

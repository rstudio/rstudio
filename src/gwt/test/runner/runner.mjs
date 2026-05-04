// Headless runner for the standalone HTML test pages under src/gwt/test/.
//
// Each target page is expected to populate `#passed` and `#failed` spans,
// the same convention the existing pages use for in-browser display.
// We serve src/gwt/ over a local HTTP server (the browser's same-origin
// policy blocks cross-directory `<script src>` loads under file://),
// navigate Playwright's bundled chromium to each page, wait until the
// counts are populated, and exit non-zero on any failure, uncaught page
// error, or failed request.

import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import { dirname, extname, isAbsolute, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";

const __dirname = dirname(fileURLToPath(import.meta.url));
const GWT_ROOT = resolve(__dirname, "..", "..");

const PAGES = [
   "test/autoindent_test_cpp.html",
   "test/autoindent_test_r.html",
   "test/highlight_test_cpp.html",
   "test/highlight_test_sql.html",
];

const MIME = {
   ".html": "text/html; charset=utf-8",
   ".js": "application/javascript; charset=utf-8",
   ".mjs": "application/javascript; charset=utf-8",
   ".css": "text/css; charset=utf-8",
   ".json": "application/json; charset=utf-8",
   ".svg": "image/svg+xml",
   ".png": "image/png",
};

function startServer(root) {
   const server = createServer(async (req, res) => {
      try {
         const url = new URL(req.url, "http://localhost");
         const requested = decodeURIComponent(url.pathname);
         const safe = resolve(join(root, requested));
         const rel = relative(root, safe);
         if (rel.startsWith("..") || isAbsolute(rel)) {
            res.writeHead(403).end("forbidden");
            return;
         }
         const info = await stat(safe).catch((err) => {
            if (err.code === "ENOENT") return null;
            throw err;
         });
         if (!info || !info.isFile()) {
            res.writeHead(404).end("not found");
            return;
         }
         const body = await readFile(safe);
         res.writeHead(200, {
            "content-type": MIME[extname(safe).toLowerCase()] || "application/octet-stream",
            "content-length": body.length,
         });
         res.end(body);
      } catch (err) {
         console.error("server error:", err);
         res.writeHead(500).end("internal error");
      }
   });
   return new Promise((resolveServer) => {
      server.listen(0, "127.0.0.1", () => resolveServer(server));
   });
}

async function runPage(page, baseUrl, relativeUrl) {
   const consoleErrors = [];
   const pageErrors = [];
   const failedRequests = [];
   page.on("console", (msg) => {
      if (msg.type() === "error") consoleErrors.push(msg.text());
   });
   page.on("pageerror", (err) => pageErrors.push(err.message));
   page.on("requestfailed", (req) => {
      failedRequests.push(`${req.url()} (${req.failure()?.errorText || "unknown"})`);
   });
   page.on("response", (res) => {
      if (res.status() >= 400) failedRequests.push(`${res.url()} (HTTP ${res.status()})`);
   });

   const url = `${baseUrl}/${relativeUrl}`;
   await page.goto(url, { waitUntil: "load" });

   // Wait for the test harness to populate #passed -- empty means it
   // has not finished (or never started, in which case we time out).
   try {
      await page.waitForFunction(() => {
         const el = document.getElementById("passed");
         return el && el.textContent && el.textContent.trim() !== "";
      }, null, { timeout: 30000 });
   } catch {
      return {
         url: relativeUrl,
         status: "timeout",
         passed: 0,
         failed: 0,
         consoleErrors,
         pageErrors,
         failedRequests,
      };
   }

   const counts = await page.evaluate(() => ({
      passed: parseInt(document.getElementById("passed")?.textContent || "0", 10),
      failed: parseInt(document.getElementById("failed")?.textContent || "0", 10),
   }));

   return {
      url: relativeUrl,
      status: counts.failed === 0 && pageErrors.length === 0 ? "ok" : "fail",
      passed: counts.passed,
      failed: counts.failed,
      consoleErrors,
      pageErrors,
      failedRequests,
   };
}

async function main() {
   const server = await startServer(GWT_ROOT);
   const { port } = server.address();
   const baseUrl = `http://127.0.0.1:${port}`;

   const browser = await chromium.launch();
   const results = [];
   try {
      const context = await browser.newContext();
      for (const relativeUrl of PAGES) {
         const page = await context.newPage();
         try {
            results.push(await runPage(page, baseUrl, relativeUrl));
         } catch (err) {
            results.push({
               url: relativeUrl,
               status: "fail",
               passed: 0,
               failed: 0,
               consoleErrors: [],
               pageErrors: [String(err)],
               failedRequests: [],
            });
         } finally {
            await page.close();
         }
      }
   } finally {
      await browser.close().catch((err) => console.error("browser.close():", err));
      await new Promise((res) => server.close(res));
   }

   let totalPassed = 0;
   let totalFailed = 0;
   let anyFail = false;
   for (const r of results) {
      const tag = r.status === "ok" ? "PASS" : r.status === "timeout" ? "TIMEOUT" : "FAIL";
      console.log(`[${tag}] ${r.url} -- passed=${r.passed} failed=${r.failed}`);
      for (const msg of r.pageErrors) console.log(`        page error: ${msg}`);
      for (const msg of r.consoleErrors) console.log(`        console: ${msg}`);
      for (const msg of (r.failedRequests || [])) console.log(`        request failed: ${msg}`);
      totalPassed += r.passed;
      totalFailed += r.failed;
      if (r.status !== "ok") anyFail = true;
   }
   console.log(`\nTotal: ${totalPassed} passed, ${totalFailed} failed across ${results.length} pages.`);
   process.exit(anyFail ? 1 : 0);
}

main().catch((err) => {
   console.error(err);
   process.exit(1);
});

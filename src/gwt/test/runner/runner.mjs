// Headless runner for the standalone HTML test pages under src/gwt/test/.
//
// Each target page is expected to populate `#passed` and `#failed` spans,
// the same convention the existing pages use for in-browser display.
// We serve the pages and the files they reference over a local HTTP
// server (the browser's same-origin policy blocks cross-directory
// `<script src>` loads under file://), navigate Playwright's bundled
// chromium to each page, wait until the counts are populated, and exit
// non-zero on any failure, uncaught page error, or failed request.

import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { dirname, extname, posix, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";

const __dirname = dirname(fileURLToPath(import.meta.url));
const GWT_ROOT = resolve(__dirname, "..", "..");

const PAGES = [
   "test/git_diff_test.html",
   "test/autoindent_test_cpp.html",
   "test/autoindent_test_r.html",
   "test/context_test_rmd.html",
   "test/git_diff_split_test.html",
   "test/highlight_test_cpp.html",
   "test/highlight_test_markdown.html",
   "test/highlight_test_r.html",
   "test/highlight_test_sql.html",
   "test/outline_test_cpp.html",
   "test/outline_test_r.html",
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

// Load each page and, transitively, the local scripts and stylesheets it
// references, keyed by the URL path the browser will request. The request
// handler serves only from this map, so nothing request-derived ever
// reaches the file system -- which the Snyk Code scan otherwise reports
// as path traversal and as unthrottled file operations, even for a
// loopback server that lives for a single test run. A request for
// anything else is a 404, which fails the page like any other failed
// request: a reference this scan misses (a single-quoted src, a script
// injected from JS, a CSS url()) must not pass silently.
async function loadAssets(root, pages) {
   const assets = new Map();
   const queue = [...pages];
   while (queue.length > 0) {
      const urlPath = queue.shift();
      if (assets.has(urlPath))
         continue;

      const file = resolve(root, ...urlPath.split("/"));
      const body = await readFile(file).catch((err) => {
         if (err.code === "ENOENT" || err.code === "EISDIR")
            return null;
         throw err;
      });
      if (body === null)
         continue;
      assets.set(urlPath, body);

      if (extname(urlPath).toLowerCase() === ".html") {
         for (const ref of localReferences(body.toString("utf8")))
            queue.push(posix.normalize(posix.join(posix.dirname(urlPath), ref)));
      }
   }
   return assets;
}

// Relative src/href targets in a page, as written, minus query and fragment.
function localReferences(html) {
   const refs = [];
   for (const match of html.matchAll(/\b(?:src|href)\s*=\s*"([^"]+)"/g)) {
      const ref = match[1].replace(/[?#].*$/, "");
      if (ref === "" || /^[a-z][a-z0-9+.-]*:/i.test(ref) || ref.startsWith("/"))
         continue;
      refs.push(ref);
   }
   return refs;
}

function startServer(assets) {
   const server = createServer((req, res) => {
      try {
         const url = new URL(req.url, "http://localhost");
         const urlPath = posix.normalize(decodeURIComponent(url.pathname)).replace(/^\/+/, "");
         const body = assets.get(urlPath);
         if (body === undefined) {
            res.writeHead(404).end("not found");
            return;
         }
         res.writeHead(200, {
            "content-type": MIME[extname(urlPath).toLowerCase()] || "application/octet-stream",
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

   // The failed assertions, as the pages that log one per line report them
   // (a "[FAIL] ..." list item or log line); the counts alone say nothing
   // about which assertion failed.
   const counts = await page.evaluate(() => ({
      passed: parseInt(document.getElementById("passed")?.textContent || "0", 10),
      failed: parseInt(document.getElementById("failed")?.textContent || "0", 10),
      failures: (document.body.innerText || "")
         .split("\n")
         .filter((line) => /^\[?FAIL\]?\s/.test(line.trim()))
         .map((line) => line.trim()),
   }));

   return {
      url: relativeUrl,
      status: counts.failed === 0 && pageErrors.length === 0 && failedRequests.length === 0
                 ? "ok" : "fail",
      passed: counts.passed,
      failed: counts.failed,
      failures: counts.failures,
      consoleErrors,
      pageErrors,
      failedRequests,
   };
}

async function main() {
   const server = await startServer(await loadAssets(GWT_ROOT, PAGES));
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
      for (const msg of (r.failures || [])) console.log(`        ${msg}`);
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

import { test as base, type Page, type TestInfo } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { launchRStudio, shutdownRStudio } from './desktop.fixture';
import { launchServer, shutdownServer } from './server.fixture';
import { setAuthStateEnv, type AiAuthOption } from '../utils/auth';
import { getEnvironmentVersions, clearConsole } from '../pages/console_pane.page';
import { drainClientExceptions } from '../utils/commands';
import { resetForNextTest } from '../utils/test-reset';
import { waitForUserConsoleInput } from '../utils/debug';

type Mode = 'desktop' | 'server';

/** One buffered browser-side diagnostic line, captured during a test. */
interface ConsoleLine {
  ts: number;
  kind: 'console' | 'pageerror';
  type?: string; // console message type ('error' | 'warning'); unset for pageerror
  text: string;
}

/**
 * Worker-scoped session context shared by the page and the per-test reset.
 *
 * `consoleBuffer` accumulates browser console errors/warnings and uncaught
 * page errors for the page; the per-test fixture clears it at the start of
 * each test and attaches it to the report when that test fails. `logDir` is
 * the directory rsession writes its log files to (desktop only -- see
 * DesktopSession.dataHome), read on failure to attach backend logs.
 */
interface SessionContext {
  page: Page;
  consoleBuffer: ConsoleLine[];
  logDir?: string;
}

/** Capture R/RStudio versions once per worker and log them. */
async function logVersions(page: Page): Promise<void> {
  const versions = await getEnvironmentVersions(page);
  console.log(`R: ${versions.r}, RStudio: ${versions.rstudio}`);
  await clearConsole(page);
}

/**
 * Buffer browser console errors/warnings and uncaught page errors into
 * `buffer`. The page persists across a worker's tests, so the listeners are
 * attached once here and the per-test fixture scopes capture by clearing the
 * buffer between tests. Only error/warning console messages are kept -- the
 * full info/log stream is noisy and rarely diagnostic on failure.
 */
function attachConsoleCapture(page: Page, buffer: ConsoleLine[]): void {
  page.on('console', (msg) => {
    const type = msg.type();
    if (type === 'error' || type === 'warning') {
      buffer.push({ ts: Date.now(), kind: 'console', type, text: msg.text() });
    }
  });
  page.on('pageerror', (err) => {
    buffer.push({ ts: Date.now(), kind: 'pageerror', text: err.stack || err.message });
  });
}

/** Attach the buffered browser console/page errors to a failing test. */
async function attachBrowserConsole(testInfo: TestInfo, buffer: ConsoleLine[]): Promise<void> {
  if (buffer.length === 0) return;
  const t0 = buffer[0].ts;
  const body = buffer
    .map((line) => {
      const rel = `+${(line.ts - t0).toString().padStart(5, ' ')}ms`;
      const tag = line.kind === 'pageerror' ? 'pageerror' : `console.${line.type}`;
      return `${rel} [${tag}] ${line.text}`;
    })
    .join('\n');
  await testInfo.attach('browser-console.log', { body, contentType: 'text/plain' });
}

/**
 * Record the current byte length of every `.log` file under `logDir`, so a
 * failing test can later attach only the bytes appended while it ran (the
 * rsession log is append-only and shared across a worker's tests). Returns an
 * empty map when no log dir is known (server mode) or it doesn't exist yet.
 */
function snapshotLogSizes(logDir?: string): Map<string, number> {
  const sizes = new Map<string, number>();
  if (!logDir) return sizes;
  let entries: string[];
  try {
    entries = fs.readdirSync(logDir);
  } catch {
    return sizes; // log dir not created yet
  }
  for (const name of entries) {
    if (!name.endsWith('.log')) continue;
    const filePath = path.join(logDir, name);
    try {
      sizes.set(filePath, fs.statSync(filePath).size);
    } catch {
      // File vanished between readdir and stat; skip it.
    }
  }
  return sizes;
}

/**
 * Attach the slice of each rsession log file written while a failing test
 * ran. `baseline` is the per-test snapshot from snapshotLogSizes(); we read
 * from that offset to the current end. If a file shrank (log rotation), read
 * it from the start instead. Empty slices are skipped.
 */
async function attachSessionLogs(
  testInfo: TestInfo,
  logDir: string | undefined,
  baseline: Map<string, number>,
): Promise<void> {
  if (!logDir) return;
  let entries: string[];
  try {
    entries = fs.readdirSync(logDir);
  } catch {
    return;
  }
  for (const name of entries) {
    if (!name.endsWith('.log')) continue;
    const filePath = path.join(logDir, name);
    let slice: string;
    try {
      const size = fs.statSync(filePath).size;
      const start = baseline.get(filePath) ?? 0;
      const from = size >= start ? start : 0;
      const length = size - from;
      if (length <= 0) continue;
      const buf = Buffer.alloc(length);
      const fd = fs.openSync(filePath, 'r');
      try {
        fs.readSync(fd, buf, 0, length, from);
      } finally {
        fs.closeSync(fd);
      }
      slice = buf.toString('utf8');
    } catch {
      continue;
    }
    if (slice.trim().length === 0) continue;
    await testInfo.attach(name, { body: slice, contentType: 'text/plain' });
  }
}

/**
 * Unified Playwright Test fixture that provides a shared RStudio page.
 *
 * The `mode` option is set per-project in playwright.config.ts; select with
 * `--project=desktop` (default) or `--project=server`.
 *
 * The `aiAuth` option declares which AI providers this file's RStudio should
 * be signed OUT of, e.g. `test.use({ aiAuth: { positai: 'none' } })` at file
 * level. It's worker-scoped, so Playwright rejects it inside a describe block
 * (that would force a new worker). Omitted providers stay authenticated (the default `{}`
 * keeps today's fully-authenticated behavior). It's worker-scoped, so tests
 * with a different aiAuth run in their own worker with a fresh RStudio launch
 * against a credential-stripped copy of the user home -- the running IDE only
 * reads credentials at launch, so a per-test toggle without a relaunch would
 * be fiction. Group same-state tests in one file to avoid relaunch churn.
 * Only sessions launched by this fixture honor it; tests that call
 * launchRStudio() themselves get the default authenticated home.
 */
export const test = base.extend<
  { perTestReset: void },
  { mode: Mode; aiAuth: AiAuthOption; rstudioSession: SessionContext; rstudioPage: Page }
>({
  mode: ['desktop', { option: true, scope: 'worker' }],
  aiAuth: [{}, { option: true, scope: 'worker' }],
  rstudioSession: [async ({ mode, aiAuth }, use) => {
    // Publish the per-worker auth state before launching; launchRStudio /
    // launchServer resolve their HOME through userHomeForAuthState, which
    // reads it back.
    setAuthStateEnv(aiAuth);
    const consoleBuffer: ConsoleLine[] = [];
    if (mode === 'server') {
      const session = await launchServer();
      // Server mode doesn't expose a per-session log dir (the spawned rserver
      // shares a data home across workers); see the issue's desktop-only note.
      attachConsoleCapture(session.page, consoleBuffer);
      await logVersions(session.page);
      await use({ page: session.page, consoleBuffer });
      // Debug-only: keep the session alive after the last test so you can
      // keep inspecting; press Enter in the Console to quit. No-op otherwise.
      await waitForUserConsoleInput(session.page, 'quit RStudio');
      await shutdownServer(session);
    } else {
      const session = await launchRStudio();
      attachConsoleCapture(session.page, consoleBuffer);
      await logVersions(session.page);
      await use({ page: session.page, consoleBuffer, logDir: session.logDir });
      // Debug-only: keep the session alive after the last test so you can
      // keep inspecting; press Enter in the Console to quit. No-op otherwise.
      await waitForUserConsoleInput(session.page, 'quit RStudio');
      await shutdownRStudio(session);
    }
  }, { scope: 'worker' }],

  rstudioPage: [async ({ rstudioSession }, use) => {
    await use(rstudioSession.page);
  }, { scope: 'worker' }],

  // Reset the IDE to a clean per-test starting state. See utils/test-reset.ts
  // for what's covered and what's deliberately not. Each step short-circuits
  // when its trigger isn't present, so on a clean session this is cheap.
  //
  // This is an auto FIXTURE, not a module-scope test.beforeEach, very much on
  // purpose. Hooks registered at the top level of this (imported) module are
  // only attached to the suite of the FIRST spec file that loads the module
  // in each worker process -- Node caches the module, so its top-level
  // statements never re-run for the next spec file, and every later file in
  // the worker silently ran without any per-test reset. That is exactly how a
  // leaked pane maximize from one spec (an R Notebook preview maximizing the
  // Viewer on a short display) survived into the next spec's first test and
  // hid the Environment tab (#17952). Auto fixtures are part of the test type
  // itself, so they run for every test in every file regardless of module
  // caching.
  perTestReset: [async ({ rstudioSession }, use, testInfo) => {
    const page = rstudioSession.page;

    // Drain exceptions that arrived BEFORE this test (a previous test's
    // teardown, the gap between specs). They can't be attributed to the
    // upcoming test, so log them rather than fail it.
    const leftovers = await drainClientExceptions(page);
    for (const e of leftovers) {
      console.warn(
        `[client-exception] recorded between tests (not attributed): ${e.message}\n${e.stack}`,
      );
    }

    // Scope failure diagnostics to this test: start with an empty browser
    // console buffer and a snapshot of the current rsession log sizes, so on
    // failure we attach only what this test produced.
    rstudioSession.consoleBuffer.length = 0;
    const logBaseline = snapshotLogSizes(rstudioSession.logDir);

    await resetForNextTest(page);

    // Debug-only: park the test (IDE clean and idle) so a human can arm
    // DevTools before the test body drives its scenario. Prompts in the
    // RStudio Console pane. No-op unless PW_DEBUG is set. See utils/debug.ts.
    await waitForUserConsoleInput(page, `run: ${testInfo.title}`);

    await use();

    // Any uncaught client exception raised while this test ran fails the
    // test, with the recorded stack in the failure output. The product
    // swallows these behind an "Error" dialog (message only), which the
    // next reset would silently dismiss -- a real product bug (like the
    // Plots-pane ImageFrame TypeError on short displays) could otherwise
    // hide behind passing tests indefinitely. PW_IGNORE_CLIENT_EXCEPTIONS=1
    // downgrades to a warning if a known benign exception must be tolerated
    // while a fix lands.
    const raised = await drainClientExceptions(page);
    const ignoreClientExceptions = ['1', 'true'].includes(
      (process.env.PW_IGNORE_CLIENT_EXCEPTIONS ?? '').toLowerCase(),
    );

    // Attach browser console + rsession logs whenever the test is failing --
    // either the body already failed, or a client exception is about to fail
    // it below. The console output captures symptoms that never reach a
    // failure assertion (a console error, an uncaught pageerror), and the
    // rsession log captures backend errors that never reach the browser.
    const willFail =
      testInfo.status !== testInfo.expectedStatus ||
      (raised.length > 0 && !ignoreClientExceptions);
    if (willFail) {
      await attachBrowserConsole(testInfo, rstudioSession.consoleBuffer);
      await attachSessionLogs(testInfo, rstudioSession.logDir, logBaseline);
    }

    if (raised.length > 0) {
      const detail = raised.map((e) => `${e.message}\n${e.stack}`).join('\n---\n');
      if (ignoreClientExceptions) {
        console.warn(`[client-exception] during "${testInfo.title}" (ignored by env):\n${detail}`);
      } else {
        throw new Error(
          `${raised.length} uncaught client exception(s) during "${testInfo.title}":\n${detail}`,
        );
      }
    }
  }, { auto: true }],
});

export { expect } from '@playwright/test';

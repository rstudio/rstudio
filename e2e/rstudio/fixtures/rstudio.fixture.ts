import { test as base, type Page, type TestInfo } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { launchRStudio, shutdownRStudio, type DesktopSession } from './desktop.fixture';
import { launchServer, shutdownServer, externalServerUrl } from './server.fixture';
import { setAuthStateEnv, type AiAuthOption } from '../utils/auth';
import { clearConsole } from '../pages/console_pane.page';
import {
  collectRunVersions,
  formatRunVersions,
  publishRunVersions,
  writeJobSummary,
} from '../utils/versions';
import { drainClientExceptions, getPref, setPref } from '../utils/commands';
import { withDeadline, DeadlineError } from '../utils/deadline';
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
  // Config root of this worker's own Desktop launch (desktop only). The
  // sandbox layout test uses it to scope assertions that only hold while
  // the launching instance is alive (see #18475).
  configRoot?: string;
}

/** Tags whose tests intentionally run with an AI assistant active. */
const ASSISTANT_TEST_TAGS = ['@ai', '@chat'];

/**
 * Set the AI assistant provider prefs back to "none" if a previous suite left
 * them on.
 *
 * The `assistant` and `chat_provider` prefs default to "posit", so once the
 * Posit Assistant backend is present in a worker's data home (installed by a
 * @chat suite settling the install prompt, or seeded via PW_SEED_PAI), every
 * later session start in that worker launches the chat backend + NES agent,
 * and every project open/close/restart shuts them down again seconds later.
 * That churn is the window for the session-shutdown wedge that kills whole CI
 * shards (#18394), and it adds agent start/stop overhead to restart-heavy
 * suites. base-prefs.jsonc starts desktop workers with both prefs at "none";
 * this guard restores that state after an @ai/@chat suite has run.
 *
 * The guard runs with the per-test fixtures, which Playwright orders after a
 * suite's beforeAll hooks: a non-@ai suite whose beforeAll restarts the
 * session or opens a project still does that work under any leaked provider
 * prefs; the guard then normalizes state before its first test runs.
 *
 * Desktop and spawned-server workers get a per-worker config home, so
 * flipping the prefs there is always isolated. An external
 * PW_RSTUDIO_SERVER_URL server has a single config shared by every worker,
 * where flipping the pref would race a @chat suite running concurrently in
 * another worker -- so external servers are only covered when the run has a
 * single worker (which cannot race itself). That includes the CI server
 * shards: one worker against a job-local rserver at localhost:8787. The
 * single-worker coverage exists because suite-local hygiene is not enough
 * on those shards: an @ai suite whose afterAll never runs (crash, timeout,
 * interrupt) leaves the provider prefs on server-side, where they survive
 * worker restarts; run 31833520057 showed a leaked live provider swallowing
 * an Escape keypress in multiselect_recovery and displacing the injected
 * suggestion in edit_suggestions. (What server workers always share is the
 * data home -- the installed backend -- not the prefs.) Spawned-server
 * workers were originally excluded too, on the assumption that the #18417
 * shutdown hardening bounded the restarts product-side -- but the wedge
 * kept firing on the server shards (#18394), where the projects region
 * restarts sessions ~30 times with a live agent, so the guard now covers
 * them.
 */
async function disableLeakedAssistant(page: Page): Promise<void> {
  const [assistant, chatProvider] = await Promise.all([
    getPref(page, 'assistant'),
    getPref(page, 'chat_provider'),
  ]);

  // getPref returns null when the pref entry is missing from the bridge's
  // map. That should not happen here (the reset fixture has already waited
  // for readiness), but a protective guard must not fail silent: warn so a
  // skipped check is visible in the test output.
  if (assistant == null || chatProvider == null) {
    console.warn(
      '[test-reset] assistant prefs unreadable ' +
        `(assistant=${assistant}, chat_provider=${chatProvider}); leak guard skipped`,
    );
  }

  const leakedOn = (value: unknown) => value != null && value !== 'none';
  if (!leakedOn(assistant) && !leakedOn(chatProvider)) return;

  console.log(
    '[test-reset] disabling AI assistant left on by a previous suite ' +
      `(assistant=${assistant}, chat_provider=${chatProvider})`,
  );
  if (leakedOn(assistant)) await setPref(page, 'assistant', 'none');
  if (leakedOn(chatProvider)) await setPref(page, 'chat_provider', 'none');
}

/**
 * Capture what this worker is running against, log it, publish it for the
 * reporter to put at the top of the Playwright report, and write it to the top
 * of the GitHub Actions run summary. The last two are per-run, not per-worker --
 * see the guards in utils/versions.ts.
 */
async function recordVersions(page: Page, mode: Mode): Promise<void> {
  const versions = await collectRunVersions(page, mode);
  console.log(`Run under test: ${formatRunVersions(versions)} · ${versions.os}`);
  publishRunVersions(versions);
  writeJobSummary(versions);
  await clearConsole(page);
}

/**
 * Log a line that GitHub also surfaces as a run annotation, so it is readable
 * without scrolling a collapsed step. Under CI only: locally the `::notice::`
 * marker is just noise. Must be stdout -- that is where GitHub parses workflow
 * commands from.
 */
function logCiNotice(message: string): void {
  console.log(process.env.GITHUB_ACTIONS ? `::notice::${message}` : message);
}

/**
 * If this worker's launch requested the Posit Assistant pre-release (test)
 * manifest (via PW_RSTUDIO_PREFS_OVERRIDE -- see desktop.fixture.ts), confirm
 * the live session actually applied it. A misapplied override (wrong prefs
 * file, timing) would otherwise silently fall back to the released Assistant
 * while every test still passes -- the run would report green having tested
 * the wrong build. Skipped entirely, with no output, on every ordinary run
 * that didn't request the test manifest.
 *
 * Desktop only, because PW_RSTUDIO_PREFS_OVERRIDE is: server.fixture.ts has no
 * prefs-override mechanism (rstudio/rstudio#17520), so a Server engine cannot
 * be gated this way -- the override would be ignored AND this check skipped.
 */
async function verifyTestManifestIfRequested(session: DesktopSession): Promise<void> {
  if (!session.requestedTestManifest) return;
  const actual = await getPref(session.page, 'posit_assistant_test_manifest');
  if (actual !== true) {
    throw new Error(
      'Posit Assistant test manifest was requested for this run, but the live session reports ' +
      `posit_assistant_test_manifest=${actual}. This run would silently test the released ` +
      'Assistant instead of the pre-release candidate -- refusing to continue.',
    );
  }
  logCiNotice('Confirmed: Posit Assistant pre-release (test) manifest is active for this worker.');
}

/**
 * Record which Posit Assistant build this worker exercised. A read-back of what
 * is on disk, not an assertion -- but absence IS reported, because by this point
 * the run has declared it is testing a pre-release candidate, and no install
 * means the subject under test was never there (Copilot-based @ai tests would
 * still pass regardless). Gated on requestedTestManifest, so an ordinary run
 * prints nothing. Under PW_SEED_PAI this reports the seeded local build, which
 * nothing downloaded.
 */
async function logPositAssistantVersionIfInstalled(session: DesktopSession): Promise<void> {
  if (!session.requestedTestManifest) return;
  const packageJsonPath = path.join(session.dataHome, 'pai', 'bin', 'package.json');
  if (!fs.existsSync(packageJsonPath)) {
    console.warn(
      `WARNING: this run requested the Posit Assistant test manifest, but no install exists at ` +
      `${packageJsonPath} -- this worker exercised no Assistant build.`,
    );
    return;
  }
  try {
    const { version } = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
    if (!version) {
      console.warn(`WARNING: no version field in ${packageJsonPath}.`);
      return;
    }
    logCiNotice(`Posit Assistant version under test: ${version}`);
  } catch (err) {
    console.warn(`WARNING: could not read Posit Assistant version from ${packageJsonPath}: ${err}`);
  }
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
 * Deadline for the per-test drain evaluates. A healthy page answers in
 * milliseconds; a page still settling a session transition answers within a
 * few seconds. 30s is comfortably past both while staying well under the
 * 120s test timeout, leaving room to diagnose and report within the test's
 * own budget.
 */
const WEDGE_PROBE_MS = 30_000;

/**
 * A page that stops answering evaluates is the signature of the #18394
 * wedge: a session shutdown that never completes leaves the browser parked
 * on a transition that never finishes (server: the proxied request to the
 * zombie session has no deadline; desktop: the relaunch is driven by a child
 * exit event that never fires), so the frame has no JS context and every
 * bridge call blocks forever. Untreated, that stacks fixture stages into
 * 300s of output silence and the CI heartbeat kills the whole shard with no
 * report.
 *
 * Capture what needs no JS context -- page.url() is tracked protocol-side
 * and tells a stuck navigation apart from a frozen renderer, and a
 * screenshot is a protocol-level capture -- then hand back a descriptive
 * error for the caller to throw. Failing the test fast makes Playwright
 * discard this worker and run the remaining tests in a fresh one, so the
 * shard survives with its report intact.
 */
async function attachWedgeDiagnostics(page: Page, testInfo: TestInfo, cause: Error): Promise<Error> {
  const url = page.url();

  let screenshotNote = 'screenshot unavailable';
  try {
    const shot = await page.screenshot({ timeout: 5000 });
    await testInfo.attach('wedged-page.png', { body: shot, contentType: 'image/png' });
    screenshotNote = 'screenshot attached as wedged-page.png';
  } catch {
    // A frozen renderer can block even protocol-level capture; the URL
    // alone still distinguishes the stuck-navigation case.
  }

  console.error(`[wedged-page] ${cause.message}; page URL: ${url} (${screenshotNote})`);
  return new Error(
    `page is unresponsive (${cause.message}); URL at detection: ${url}. ` +
    'This is the session-transition wedge signature (#18394); failing fast ' +
    'so the worker restarts with a fresh session.',
  );
}

/**
 * Unified Playwright Test fixture that provides a shared RStudio page.
 *
 * The `mode` option is set per-project in playwright.config.ts; select with
 * `--project=desktop` (default) or `--project=server`.
 *
 * The `aiAuth` option declares which AI providers this file's RStudio should
 * be signed OUT of, e.g. `test.use({ aiAuth: { positai: 'none' } })` at file
 * level. Omitted providers stay authenticated (the default `{}` is the
 * fully-authenticated behavior). It's worker-scoped: Playwright rejects it
 * inside a describe block (that would force a new worker), and tests with a
 * different aiAuth run in their own worker with a fresh RStudio launch
 * against a credential-stripped copy of the user home (the running IDE only
 * reads credentials at launch, so a per-test toggle without a relaunch would
 * be fiction). Group same-state tests in one file to avoid relaunch churn.
 * The fixture publishes the state worker-wide before launching, so a test
 * that also calls launchRStudio() itself inherits the worker's declared
 * state; only workers where this fixture never runs launch against the
 * default authenticated home.
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
      await recordVersions(session.page, 'server');
      await use({ page: session.page, consoleBuffer });
      // Debug-only: keep the session alive after the last test so you can
      // keep inspecting; press Enter in the Console to quit. Does nothing otherwise.
      await waitForUserConsoleInput(session.page, 'quit RStudio');
      await shutdownServer(session);
    } else {
      const session = await launchRStudio();
      attachConsoleCapture(session.page, consoleBuffer);
      await recordVersions(session.page, 'desktop');
      await verifyTestManifestIfRequested(session);
      await use({
        page: session.page,
        consoleBuffer,
        logDir: session.logDir,
        configRoot: session.configRoot,
      });
      await logPositAssistantVersionIfInstalled(session);
      // Debug-only: keep the session alive after the last test so you can
      // keep inspecting; press Enter in the Console to quit. Does nothing otherwise.
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
  perTestReset: [async ({ rstudioSession, mode }, use, testInfo) => {
    const page = rstudioSession.page;

    // Drain exceptions that arrived BEFORE this test (a previous test's
    // teardown, the gap between specs). They can't be attributed to the
    // upcoming test, so log them rather than fail it. The drain is the first
    // page.evaluate of the test, so it is also where a wedged page (#18394)
    // surfaces: bound it and fail fast with diagnostics rather than hang the
    // whole 120s test timeout doing nothing.
    let leftovers: Awaited<ReturnType<typeof drainClientExceptions>>;
    try {
      leftovers = await withDeadline(
        drainClientExceptions(page), WEDGE_PROBE_MS, 'pre-test client-exception drain');
    } catch (err) {
      if (!(err instanceof DeadlineError))
        throw err;
      throw await attachWedgeDiagnostics(page, testInfo, err);
    }
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

    // Keep the AI assistant off for tests that don't opt in via @ai/@chat --
    // see disableLeakedAssistant (also for why multi-worker runs against an
    // external server are excluded). Runs after resetForNextTest so the
    // bridge readiness gate has already been cleared.
    const prefsAreWorkerScoped = mode === 'desktop' || externalServerUrl() === null;
    const cannotRaceAnotherWorker = testInfo.config.workers === 1;
    if ((prefsAreWorkerScoped || cannotRaceAnotherWorker) &&
        !testInfo.tags.some((tag) => ASSISTANT_TEST_TAGS.includes(tag)))
      await disableLeakedAssistant(page);

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
    //
    // The drain is bounded for the same reason as in setup: a test body that
    // wedged the page (#18394) already burned the 120s test timeout, and an
    // unbounded evaluate here would silently burn the teardown budget too,
    // pushing total output silence past the CI heartbeat's kill window.
    let raised: Awaited<ReturnType<typeof drainClientExceptions>> = [];
    let wedge: Error | null = null;
    try {
      raised = await withDeadline(
        drainClientExceptions(page), WEDGE_PROBE_MS, 'post-test client-exception drain');
    } catch (err) {
      if (!(err instanceof DeadlineError))
        throw err;
      wedge = await attachWedgeDiagnostics(page, testInfo, err);
    }
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
      wedge !== null ||
      (raised.length > 0 && !ignoreClientExceptions);
    if (willFail) {
      await attachBrowserConsole(testInfo, rstudioSession.consoleBuffer);
      await attachSessionLogs(testInfo, rstudioSession.logDir, logBaseline);
    }

    if (wedge)
      throw wedge;

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

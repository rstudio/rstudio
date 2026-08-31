import type { Page } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser } from 'playwright';
import { spawn, spawnSync, execSync } from 'child_process';
import type { ChildProcess, SpawnOptions } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import stripJsonComments from 'strip-json-comments';
import { TIMEOUTS, RSTUDIO_EXTRA_ARGS, sleep } from '../utils/constants';
import { CONSOLE_INPUT, executeInConsole } from '../pages/console_pane.page';
import { dismissAllModals, documentCloseAllNoSave, executeCommand } from '../utils/commands';
import { withDeadline } from '../utils/deadline';
import { workerRLibsUser } from './r-libs-setup';
import { trackForReaping } from './process-reaper';
import { isDebugMode } from '../utils/debug';
import { userHomeForAuthState } from '../utils/auth';

const BASE_PREFS_PATH = path.join(__dirname, 'base-prefs.jsonc');
const OVERRIDE_PREFS_ENV = 'PW_RSTUDIO_PREFS_OVERRIDE';

// PW_SANDBOX is exported by the globalSetup hook in fixtures/sandbox-setup.ts
// before any worker spawns. Resolve lazily so importing this module (for
// --list, type-checking, etc.) doesn't require the env var -- the assertion
// fires only when a test actually launches a session.
function sandboxRoot(): string {
  const s = process.env.PW_SANDBOX;
  if (!s) {
    throw new Error(
      'PW_SANDBOX is not set; fixtures/sandbox-setup.ts should populate it before any worker spawns',
    );
  }
  return s;
}
// Sandbox-level data-home: NOT used as RSTUDIO_DATA_HOME for Desktop launches
// (each launch gets its own data home under its config root -- see
// createTempConfig), only as the source of the seeded Posit Assistant build
// (data-home/pai, populated by sandbox-setup.ts when PW_SEED_PAI is set).
const sandboxDataHome = () => path.join(sandboxRoot(), 'data-home');

// HOME / USERPROFILE for the current worker. Single-worker runs (the default)
// use the seeded template home directly -- byte-for-byte the historical
// behavior. Parallel runs give every worker its own copy of the template,
// keyed on the stable parallel index, so concurrent workers never write the
// same HOME (RStudio user state, command history, AI credentials, ...). The
// copy is lazy and idempotent; the template carries the seeded AI credentials
// and Windows AppData scaffold, so each worker's copy starts authenticated.
function workerUserHome(): string {
  const template = path.join(sandboxRoot(), 'user-home');

  const totalWorkers = Number(process.env.PW_TOTAL_WORKERS ?? '1');
  if (!Number.isFinite(totalWorkers) || totalWorkers <= 1) {
    return template;
  }

  const idx = Number(process.env.TEST_PARALLEL_INDEX ?? '0') || 0;
  const home = path.join(sandboxRoot(), `user-home-${idx}`);
  if (!fs.existsSync(home)) {
    // Copy into a temp sibling and atomically rename, so a crash mid-copy can't
    // leave a partial HOME that later reads as complete (missing seeded AI creds
    // / Windows AppData scaffold). The parallel index is exclusive to one worker
    // process at a time, so the temp name only needs to be unique against a
    // prior aborted attempt.
    const tmp = `${home}.partial`;
    fs.rmSync(tmp, { recursive: true, force: true });
    fs.cpSync(template, tmp, { recursive: true });
    fs.renameSync(tmp, home);
  }
  return home;
}

function readPrefsFile(filePath: string, sourceLabel: string): Record<string, unknown> {
  let raw: string;
  try {
    raw = fs.readFileSync(filePath, 'utf8');
  } catch (err) {
    throw new Error(`Failed to read RStudio prefs (${sourceLabel}) at ${filePath}: ${(err as Error).message}`);
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(stripJsonComments(raw));
  } catch (err) {
    throw new Error(`Failed to parse RStudio prefs (${sourceLabel}) at ${filePath}: ${(err as Error).message}`);
  }
  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    const got = Array.isArray(parsed) ? 'array' : typeof parsed;
    throw new Error(`RStudio prefs (${sourceLabel}) at ${filePath} must be a JSON object, got ${got}`);
  }
  return parsed as Record<string, unknown>;
}

// Constants
export const RSTUDIO_PATH = process.platform === 'win32'
  ? 'C:\\Program Files\\RStudio\\rstudio.exe'
  : process.platform === 'darwin'
    ? '/Applications/RStudio.app/Contents/MacOS/RStudio'
    : '/usr/bin/rstudio';
// Deterministic per-worker CDP port: each parallel worker gets its own fixed
// port (base + checkout offset + parallel index) so concurrent workers never
// collide. A random port would, with a handful of workers, occasionally have
// two workers draw the same value -- and the per-launch `lsof :PORT | kill`
// cleanup would then kill another worker's RStudio. The checkout offset (a
// stable hash of this checkout's path) keeps concurrent runs from different
// checkouts/worktrees on the same machine in disjoint bands, so one run's
// cleanup can't reclaim another run's live instance (rstudio#18135). The
// dev/logger ports derive from this (+1000/+2000); the offset is folded into
// [0, 900) so with < 100 workers all three bands stay disjoint. PW_CDP_PORT
// overrides for single-instance debugging.
const CDP_PORT_BASE = 9231;
function checkoutPortOffset(): number {
  // FNV-1a over the checkout path (this file's directory is stable and
  // unique per checkout/worktree).
  let hash = 0x811c9dc5;
  for (let i = 0; i < __dirname.length; i++) {
    hash ^= __dirname.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  return (hash >>> 0) % 900;
}
function defaultCdpPort(): number {
  const idx = Number(process.env.TEST_PARALLEL_INDEX ?? '0') || 0;
  return CDP_PORT_BASE + checkoutPortOffset() + idx;
}
export const CDP_PORT = Number(process.env.PW_CDP_PORT) || defaultCdpPort();
// Connect over IPv4 explicitly. Electron's --remote-debugging-port listens on
// 127.0.0.1 only, but "localhost" resolves to IPv6 ::1 first on some Linux
// distros (e.g. Fedora), so connectOverCDP dials ::1 and gets ECONNREFUSED even
// though RStudio is up on 127.0.0.1. Using the literal 127.0.0.1 removes the
// resolution ambiguity and is correct on every platform.
export const CDP_URL = `http://127.0.0.1:${CDP_PORT}`;

// PW_RSTUDIO_DEV=1 launches the in-tree dev build via `npm run start`
// (electron-forge) in src/node/desktop, instead of the installed RStudio
// binary at RSTUDIO_PATH. Assumes the dev build is already compiled --
// see e2e/rstudio/README.md.
const DEV_MODE = (() => {
  const v = process.env.PW_RSTUDIO_DEV?.toLowerCase();
  return v === '1' || v === 'true';
})();
const DEV_DESKTOP_DIR = path.resolve(__dirname, '../../../src/node/desktop');
// First-run webpack compile can take a couple of minutes; subsequent
// starts are much faster but still slower than launching the installed
// binary, so give dev-mode startup more headroom than installed mode.
const DEV_STARTUP_TIMEOUT_MS = 180000;

/**
 * Kill the rstudio child process and (in dev mode) its descendants.
 *
 * Default mode spawns the RStudio binary directly, so `proc.kill()` is
 * enough. In dev mode, `proc` is the npm/cmd.exe wrapper -- SIGTERM to it
 * doesn't reliably reach electron-forge, webpack-dev-server, or Electron,
 * so we tear down the whole tree: by process group on POSIX (set up via
 * `detached: true` at spawn time) and via `taskkill /F /T` on Windows.
 */
function killProcessTree(proc: ChildProcess): void {
  const pid = proc.pid;
  if (pid === undefined) return;
  try {
    if (DEV_MODE) {
      if (process.platform === 'win32') {
        spawnSync('taskkill', ['/F', '/T', '/PID', String(pid)], { stdio: 'pipe' });
      } else {
        process.kill(-pid, 'SIGTERM');
      }
    } else {
      proc.kill();
    }
  } catch {
    // Process tree may already be gone
  }
}

export interface DesktopSession {
  page: Page;
  browser: Browser;
  rstudioProcess: ChildProcess;
  configRoot: string;
  // Directory rsession writes its log files to for this launch:
  // RSTUDIO_DATA_HOME/log (see core/system/Xdg.cpp userLogDir). The per-test
  // fixture reads them from here to attach backend logs on a failure.
  logDir: string;
  // RSTUDIO_DATA_HOME for this launch. Owned here because this file owns the
  // config-tree layout; callers needing something under it (e.g. the installed
  // Posit Assistant at data-home/pai) should build from this, not re-derive it.
  dataHome: string;
  // Whether this launch's prefs asked for the Posit Assistant pre-release
  // (test) manifest -- see TempConfig.requestedTestManifest.
  requestedTestManifest: boolean;
}

// Gated diagnostic: when PW_DEBUG_PAGES=1 is set, attach listeners to every
// existing and future page in every context, so we can see the navigation /
// load / console / error sequence that produces the renderer's "double load"
// behavior during startup. Output is prefixed `[debug-launch]` with relative
// timestamps. Leave the flag unset for normal runs.
//
// Deliberately a separate flag from PW_DEBUG_LAUNCH (the `[launch-timing]`
// phase timeline CI keeps enabled): these listeners stay attached for the
// whole run and would otherwise trace every popup and secondary window --
// plot zoom, presentations, etc. -- long after launch.
function attachLaunchDebug(browser: Browser): void {
  if (process.env.PW_DEBUG_PAGES !== '1' && process.env.PW_DEBUG_PAGES !== 'true') {
    return;
  }

  const t0 = Date.now();
  const stamp = () => `+${(Date.now() - t0).toString().padStart(5, ' ')}ms`;
  let pageSeq = 0;

  const attach = (p: Page): void => {
    const label = `p${pageSeq++}`;
    console.log(`[debug-launch] ${stamp()} ${label}: page created, url=${p.url()}`);

    p.on('framenavigated', (frame) => {
      if (frame === p.mainFrame()) {
        console.log(`[debug-launch] ${stamp()} ${label}: navigated -> ${frame.url()}`);
      }
    });
    p.on('load', () => {
      console.log(`[debug-launch] ${stamp()} ${label}: load (url=${p.url()})`);
    });
    p.on('domcontentloaded', () => {
      console.log(`[debug-launch] ${stamp()} ${label}: domcontentloaded (url=${p.url()})`);
    });
    p.on('console', (msg) => {
      const t = msg.type();
      if (t === 'error' || t === 'warning' || t === 'info') {
        console.log(`[debug-launch] ${stamp()} ${label}: console.${t}: ${msg.text()}`);
      }
    });
    p.on('pageerror', (err) => {
      console.log(`[debug-launch] ${stamp()} ${label}: pageerror: ${err.message}`);
    });
    p.on('close', () => {
      console.log(`[debug-launch] ${stamp()} ${label}: closed`);
    });
  };

  for (const ctx of browser.contexts()) {
    ctx.on('page', attach);
    for (const existing of ctx.pages()) {
      attach(existing);
    }
  }
}

interface TempConfig {
  root: string;
  configHome: string;
  configDir: string;
  electronUserData: string;
  dataHome: string;
  // Whether this launch's prefs ask for the Posit Assistant pre-release (test)
  // manifest. Derived from the merged base+override prefs on a fresh config,
  // and re-read from the reused prefs file on the relaunch path -- a relaunched
  // session reads that same file, so it can genuinely be on the test manifest.
  requestedTestManifest: boolean;
}

/**
 * Create an isolated RStudio config directory tree with a prefs file
 * built by merging fixtures/base-prefs.jsonc with an optional override
 * from PW_RSTUDIO_PREFS_OVERRIDE. Plumbed into RStudio via
 * RSTUDIO_CONFIG_* env vars at spawn time so the user's real profile
 * is untouched. Also carries a per-spec data home (RSTUDIO_DATA_HOME)
 * so persisted client state -- window layout, pane sizes, source docs --
 * can't leak between specs or workers.
 *
 * Desktop only -- Server mode doesn't spawn RStudio, so this mechanism
 * doesn't apply directly. See https://github.com/rstudio/rstudio/issues/17520
 * for tracking parity with Server mode.
 */
function createTempConfig(): TempConfig {
  const root = fs.mkdtempSync(path.join(sandboxRoot(), 'config_'));
  const configHome = path.join(root, 'config-home');
  const configDir = path.join(root, 'config-dir');
  const electronUserData = path.join(root, 'electron-userdata');
  const dataHome = path.join(root, 'data-home');
  for (const d of [configHome, configDir, electronUserData, dataHome]) {
    fs.mkdirSync(d, { recursive: true });
  }
  seedPaiIntoDataHome(dataHome);

  const basePrefs = readPrefsFile(BASE_PREFS_PATH, 'base');
  const overridePath = process.env[OVERRIDE_PREFS_ENV];
  const overridePrefs = overridePath ? readPrefsFile(overridePath, OVERRIDE_PREFS_ENV) : {};
  const prefs = { ...basePrefs, ...overridePrefs };

  fs.writeFileSync(
    path.join(configHome, 'rstudio-prefs.json'),
    JSON.stringify(prefs, null, 2),
  );

  const requestedTestManifest = prefs.posit_assistant_test_manifest === true;

  // Pre-seed electron-store's config.json with explicit windowBounds, pinned
  // to ONE geometry that both local machines and CI render identically:
  // 1024x645, the macOS GH Actions runner's display workArea. Asking for
  // anything taller is silently clamped to that on CI (macOS constrains
  // windows to the visible frame regardless of the requested rect), so the
  // previous 1400x900 request meant local runs tested a different layout
  // than CI -- and geometry-sensitive bugs (the Viewer's ensure-height
  // escalating to a pane maximize, the Plots-pane ImageFrame TypeError)
  // surfaced only on CI. 800x600 was evaluated and rejected: it genuinely
  // cramps layouts (fewer data-viewer columns visible, the Import Dataset
  // dialog doesn't fit) and broke 7 tests.
  fs.writeFileSync(
    path.join(electronUserData, 'config.json'),
    JSON.stringify(
      { view: { windowBounds: { x: 0, y: 0, width: 1024, height: 645, maximized: false } } },
      null,
      2,
    ),
  );

  return { root, configHome, configDir, electronUserData, dataHome, requestedTestManifest };
}

/**
 * Link the seeded Posit Assistant build (sandbox data-home/pai, populated by
 * sandbox-setup.ts when PW_SEED_PAI is set) into a per-spec data home so the
 * session under test finds it at RSTUDIO_DATA_HOME/pai. A symlink (junction
 * on Windows, which needs no elevation) avoids copying the install once per
 * spec. The uninstall flow deletes pai via boost::filesystem::remove_all,
 * which removes the link itself without following it, so an uninstall test
 * can't destroy the shared seed. Writes into pai (e.g. manifest-check.json)
 * do go through the link to the seed -- same exposure as the previous fully
 * shared data home, now scoped to pai only. No-op when nothing was seeded or
 * the link already exists (config-root reuse across a restart).
 */
function seedPaiIntoDataHome(dataHome: string): void {
  const seed = path.join(sandboxDataHome(), 'pai');
  const dest = path.join(dataHome, 'pai');
  if (!fs.existsSync(seed) || fs.existsSync(dest)) {
    return;
  }
  fs.symlinkSync(seed, dest, process.platform === 'win32' ? 'junction' : 'dir');
}

// Cold CI runners can take longer than a developer machine to clear the
// GWT-ready check (JS download/parse, R session boot, ApplicationAutomation
// init, DeferredInitCompletedEvent). PW_GWT_READY_TIMEOUT_MS overrides
// explicitly; otherwise default to 60s under CI and the previous 30s locally.
const PAGE_READY_TIMEOUT_MS =
  Number(process.env.PW_GWT_READY_TIMEOUT_MS) ||
  (process.env.CI ? 60000 : 30000);

// Shard-level circuit breaker (rstudio#18522): when the environment is broken
// (e.g. an orphaned process holding the worker's CDP port), every launch in
// the shard fails the same way, each burning the better part of a minute
// across its attempts -- and the retry loop's own logging keeps the
// run-with-heartbeat watchdog fed, so a shard making no progress still looks
// alive. Left alone, that occupied a Windows runner for the full 2h job
// timeout without recording a single test result. Track the consecutive
// launch-failure streak in a file (Playwright replaces the worker process
// after a test failure, so module state would reset) and refuse to launch
// once it crosses the threshold, failing the remaining tests immediately.
const LAUNCH_FAILURE_STREAK_LIMIT = Math.max(1, Number(process.env.PW_LAUNCH_FAILURE_STREAK_LIMIT) || 3);

interface LaunchFailureStreak {
  count: number;
  lastError?: string;
}

// One streak file per worker slot: parallel workers have independent CDP
// ports, so one worker's poisoned port shouldn't abort its siblings. The
// parallel index is reused by Playwright's replacement workers, and the
// sandbox root is removed at end of run, so the streak spans one worker
// slot's launches within a single run and nothing else.
function launchFailureStreakFile(): string {
  const idx = Number(process.env.TEST_PARALLEL_INDEX ?? '0') || 0;
  return path.join(sandboxRoot(), `launch-failure-streak-${idx}.json`);
}

function readLaunchFailureStreak(): LaunchFailureStreak {
  try {
    const parsed = JSON.parse(fs.readFileSync(launchFailureStreakFile(), 'utf8')) as LaunchFailureStreak;
    return Number.isFinite(parsed.count) && parsed.count > 0 ? parsed : { count: 0 };
  } catch {
    // Missing or unreadable file = no streak
    return { count: 0 };
  }
}

/**
 * Launch RStudio with CDP, connect Playwright, and return the session.
 *
 * `existingConfigRoot` lets relaunchAfterRestart reuse the same config
 * directory across a quit-and-restart so prefs/state persist.
 *
 * Retries the underlying launch once on failure. A cold-cache flake during
 * the GWT-ready phase is the dominant failure mode on CI runners, and the
 * post-CDP catch already tears the process tree down on failure, so a
 * second attempt is safe and cheap. Set PW_LAUNCH_ATTEMPTS to override the
 * attempt count (default 2 -- one retry).
 *
 * Once LAUNCH_FAILURE_STREAK_LIMIT consecutive launches (all attempts
 * exhausted, no success in between) have failed on this worker slot, gives
 * up immediately without spawning anything -- see the circuit-breaker note
 * above. PW_LAUNCH_FAILURE_STREAK_LIMIT overrides the threshold.
 */
export async function launchRStudio(existingConfigRoot?: string): Promise<DesktopSession> {
  const streak = readLaunchFailureStreak();
  if (streak.count >= LAUNCH_FAILURE_STREAK_LIMIT) {
    throw new Error(
      `Launch circuit breaker open: ${streak.count} consecutive RStudio launches have failed on this worker` +
      ` (last: ${streak.lastError ?? 'unknown'}). Refusing further launches so the shard fails fast instead of` +
      ` running out the job timeout (rstudio#18522). Set PW_LAUNCH_FAILURE_STREAK_LIMIT to tune.`,
    );
  }

  const maxAttempts = Math.max(1, Number(process.env.PW_LAUNCH_ATTEMPTS) || 2);
  let lastError: unknown;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      const session = await launchRStudioOnce(existingConfigRoot);
      fs.rmSync(launchFailureStreakFile(), { force: true });
      return session;
    } catch (err) {
      lastError = err;
      const msg = (err as Error)?.message ?? String(err);
      if (attempt < maxAttempts) {
        console.warn(`[launch] attempt ${attempt}/${maxAttempts} failed: ${msg} -- retrying`);
      } else {
        console.warn(`[launch] attempt ${attempt}/${maxAttempts} failed: ${msg} -- giving up`);
      }
    }
  }

  try {
    const lastMessage = (lastError as Error)?.message ?? String(lastError);
    fs.writeFileSync(
      launchFailureStreakFile(),
      JSON.stringify({ count: streak.count + 1, lastError: lastMessage }),
    );
  } catch {
    // Never mask the launch error with a bookkeeping failure
  }
  throw lastError instanceof Error ? lastError : new Error(String(lastError));
}

/**
 * PIDs the TCP stack reports as owning a LISTEN socket on this worker's CDP
 * port, comma-separated ('' when the port is free). The owning PID is
 * recorded at socket creation, so for a socket kept alive only by an
 * inherited handle this can name a process that already exited.
 */
function cdpPortListenerPids(): string {
  try {
    if (process.platform === 'win32') {
      return execSync(
        `powershell.exe -NoProfile -Command "(Get-NetTCPConnection -LocalPort ${CDP_PORT} -State Listen -ErrorAction SilentlyContinue).OwningProcess -join ','"`,
        { encoding: 'utf-8', stdio: 'pipe' },
      ).trim();
    } else {
      const out = execSync(`lsof -ti TCP:${CDP_PORT} -sTCP:LISTEN`, { encoding: 'utf-8', stdio: 'pipe' }).trim();
      return out.split('\n').filter(Boolean).join(',');
    }
  } catch {
    // lsof exits non-zero when there is no listener
    return '';
  }
}

async function launchRStudioOnce(existingConfigRoot?: string): Promise<DesktopSession> {
  // Clean up any existing RStudio on our specific CDP port. The port is fixed
  // per worker (base + checkout offset + parallel index), so this only ever
  // reclaims an orphaned process from a prior interrupted run on this worker's
  // own port -- never a sibling worker's live instance. Match the LISTEN
  // socket only: an unqualified port match also selects the *client* end of
  // established connections to that port, and if the band were ever shared
  // (e.g. PW_CDP_PORT collisions) that would SIGKILL another run's Playwright
  // worker process, not just a leftover RStudio (rstudio#18135).
  console.log(`CDP port: ${CDP_PORT}`);
  console.log(`Cleaning up any RStudio on port ${CDP_PORT}...`);
  try {
    if (process.platform === 'win32') {
      execSync(
        `powershell.exe -NoProfile -Command "(Get-NetTCPConnection -LocalPort ${CDP_PORT} -State Listen -ErrorAction SilentlyContinue).OwningProcess | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }"`,
        { encoding: 'utf-8', stdio: 'pipe' }
      );
    } else {
      execSync(`lsof -ti TCP:${CDP_PORT} -sTCP:LISTEN | xargs kill -9 2>/dev/null`, { stdio: 'ignore' });
    }
  } catch {
    // No process on that port, that's fine
  }

  // Wait for the port to be free. When nothing was ever bound, the first
  // probe reports empty and we break out in microseconds. When we just
  // killed something, the OS usually releases the port within a few hundred
  // ms; the 15s ceiling covers a lingering rsession that inherited the
  // socket handle and exits when it notices its parent is gone. As above,
  // probe the LISTEN socket only -- what matters is whether Electron can
  // bind the port, and matching connected client sockets too would stall
  // the full 15s.
  let portHolders = '';
  const portDeadline = Date.now() + 15000;
  while (Date.now() < portDeadline) {
    portHolders = cdpPortListenerPids();
    if (!portHolders) break;
    await sleep(100);
  }

  // A LISTEN socket that survives the kill above is one the cleanup cannot
  // clear: when a process holds an inherited handle to another process's
  // socket, the TCP table still attributes the listener to the (now dead)
  // creator PID, so killing by owning PID is a no-op. Launching anyway would
  // spawn an RStudio that cannot bind the port and never becomes reachable
  // over CDP -- and did, repeatedly, for the rest of a 2h shard
  // (rstudio#18522). Fail now with the real reason instead.
  if (portHolders) {
    throw new Error(
      `CDP port ${CDP_PORT} still has a LISTEN socket after cleanup (owning PID(s): ${portHolders});` +
      ` an orphaned process is likely holding an inherited handle to it, and a newly launched RStudio` +
      ` would not be able to bind it (rstudio#18522)`,
    );
  }

  // Set up the isolated config directory (or reuse one across a restart)
  let tempConfig: TempConfig;
  if (existingConfigRoot) {
    const reusedPrefsPath = path.join(existingConfigRoot, 'config-home', 'rstudio-prefs.json');
    tempConfig = {
      root: existingConfigRoot,
      configHome: path.join(existingConfigRoot, 'config-home'),
      configDir: path.join(existingConfigRoot, 'config-dir'),
      electronUserData: path.join(existingConfigRoot, 'electron-userdata'),
      dataHome: path.join(existingConfigRoot, 'data-home'),
      // Read back rather than assumed: this path doesn't re-merge, but the
      // prefs file it reuses still carries whatever the first launch wrote.
      requestedTestManifest:
        fs.existsSync(reusedPrefsPath) &&
        readPrefsFile(reusedPrefsPath, 'reused config').posit_assistant_test_manifest === true,
    };
    // Defensively recreate child dirs in case anything cleared them between runs
    for (const d of [tempConfig.configHome, tempConfig.configDir, tempConfig.electronUserData, tempConfig.dataHome]) {
      fs.mkdirSync(d, { recursive: true });
    }
    seedPaiIntoDataHome(tempConfig.dataHome);
  } else {
    tempConfig = createTempConfig();
  }
  const configRoot = tempConfig.root;
  console.log(`[sandbox] this spec's config: ${configRoot}`);

  // Start RStudio with remote debugging enabled. --automation-agent is
  // forwarded to rsession (see session-launcher.ts), which causes
  // ApplicationAutomation to expose `window.rstudio` -- the command,
  // preference, and document helpers our tests drive instead of typing
  // commands through the console.
  const rstudioArgs = [
    `--remote-debugging-port=${CDP_PORT}`,
    `--user-data-dir=${tempConfig.electronUserData}`,
    '--automation-agent',
    ...RSTUDIO_EXTRA_ARGS,
  ];
  // In dev mode, run `npm run start` in src/node/desktop. The package.json
  // script command is `electron-forge start -- --no-sandbox`, and `npm run
  // start -- <args>` appends our args to the script command, producing
  // `electron-forge start -- --no-sandbox <args>`. electron-forge forwards
  // everything after its own `--` to Electron, so our flags arrive alongside
  // `--no-sandbox`.
  //
  // We deliberately avoid `shell: true`: it would re-parse each argv value
  // through the shell, breaking paths that contain spaces (e.g. when
  // PW_SANDBOX_ROOT points somewhere with whitespace) and changing the
  // semantics of PW_RSTUDIO_EXTRA_ARGS from literal argv to shell text.
  // On Windows, npm is npm.cmd, which Node refuses to spawn directly
  // without shell: true -- go through cmd.exe /c instead. Node's normal
  // arg quoting then preserves whitespace and most punctuation, but cmd
  // metacharacters (`&` `|` `<` `>` `^` `%`) still go through cmd's own
  // parser, so paths or PW_RSTUDIO_EXTRA_ARGS values containing those
  // characters are not supported on the Windows dev path.
  let spawnCmd: string;
  let spawnArgs: string[];
  // Resolve the launch HOME through the per-test auth state: normally the
  // worker home unchanged; under aiAuth 'none' declarations, a
  // credential-stripped copy of it (see userHomeForAuthState in utils/auth.ts).
  const launchHome = userHomeForAuthState(workerUserHome());
  const spawnOptions: SpawnOptions = {
    env: {
      ...process.env,
      HOME: launchHome,
      // R expands %p / %v at startup; the resolved path is the same one
      // globalSetup pre-creates and pre-populates in r-libs-setup.ts. Setting
      // this explicitly is necessary because HOME is redirected -- without it,
      // R derives an empty default library inside the per-run sandbox. Under
      // parallel runs this resolves to a per-worker hermetic clone of that
      // library so concurrent installs/removes can't race or leak.
      R_LIBS_USER: workerRLibsUser(),
      RSTUDIO_CONFIG_DIR: tempConfig.configDir,
      RSTUDIO_CONFIG_HOME: tempConfig.configHome,
      RSTUDIO_CONFIG_ROOT: tempConfig.root,
      // Per-spec, not shared: RSTUDIO_DATA_HOME is where the session persists
      // client state (data-home/pcs/*.pper -- window layout, pane sizes,
      // source docs, ...). Sharing it across workers let one spec's leaked
      // state (e.g. a maximized Viewer pane from a notebook preview) poison
      // every later launch in the run, including fresh retry workers. Tied to
      // the config root so a deliberate quit-and-restart (which reuses the
      // config root) still sees its persisted state.
      RSTUDIO_DATA_HOME: tempConfig.dataHome,
      RSTUDIO_DISABLE_WHATS_NEW: '1',
      // Under PW_DEBUG, have the launched app open Chromium DevTools on
      // startup so the renderer's Performance profiler is ready before
      // waitForUserConsoleInput resumes the test.
      ...(isDebugMode() ? { RSTUDIO_OPEN_DEVTOOLS: '1' } : {}),
      // Suppress the Electron splash screen during automation; otherwise CDP
      // can grab the splash window before the main app loads (see the
      // automation-bridge poll loop below).
      RS_NO_SPLASH: '1',
      // Force window closes through even when a page's beforeunload handler
      // prevents unload; the interactive path shows a native "Leave page?"
      // dialog that automation cannot dismiss (rstudio#17439).
      RSTUDIO_DESKTOP_IGNORE_BEFOREUNLOAD: '1',
      // Point the unixODBC driver manager (both the system one and the copy
      // statically linked into the odbc R package) at the sandbox-local ODBC
      // configuration built by sandbox-setup, so the Connections tests see
      // exactly the drivers registered there and the machine's real
      // odbcinst.ini is never read or written. Without this the C++ session
      // falls back to /usr/local/etc (SessionConnections.cpp), i.e. whatever
      // happens to be on the host.
      ...(process.env.PW_ODBC_DIR ? { ODBCSYSINI: process.env.PW_ODBC_DIR } : {}),
      // Windows has no ODBCSYSINI, so the Connections tests instead register
      // their drivers machine-wide, pointing at a sandbox copy of the driver
      // DLL alone (see prepareOdbcSandboxWindows for why the directory is not
      // copied). A DLL loaded by full path does not get its own directory
      // searched for dependencies, so the directory it was copied from is
      // prepended to PATH here -- that is what lets psqlODBC find its bundled
      // libpq and OpenSSL. Prepended rather than appended so the driver's own
      // versioned copies win over anything else on PATH.
      ...(process.env.PW_ODBC_DRIVER_PATHS
        ? {
            PATH: `${process.env.PW_ODBC_DRIVER_PATHS}${path.delimiter}${process.env.PATH ?? ''}`,
          }
        : {}),
      // The bundled Copilot language server stores the master key for its
      // encrypted OAuth token cache in the OS keychain (@github/keytar,
      // service "copilot-language-server", account "oauth-token-key"). Under
      // the redirected HOME, macOS finds no login keychain and blocks the
      // run with a "Keychain Not Found" system modal; on Windows the key
      // would land in the host's real Credential Manager, leaking test state
      // out of the sandbox. Disable the encryption path so the token cache
      // stays inside the sandboxed user-home (#18205).
      GITHUB_COPILOT_AUTH_TOKEN_ENCRYPTION: 'false',
      // Dev mode runs `npm run start` (electron-forge), whose webpack dev-server
      // and logger otherwise bind the fixed defaults 3000 / 9000. Derive both
      // from the per-worker CDP port so concurrent workers -- and a developer's
      // own manually-launched dev instance on the defaults -- don't collide.
      // CDP ports are fixed per worker (base + checkout offset + parallel
      // index), and the offset stays under 900, so with a realistic worker
      // count the CDP / dev (+1000) / logger (+2000) bands stay disjoint and
      // no two workers share a port.
      // forge.config.js reads these; ignored by the installed-binary path.
      ...(DEV_MODE ? {
        RSTUDIO_DESKTOP_DEV_PORT: String(CDP_PORT + 1000),
        RSTUDIO_DESKTOP_LOGGER_PORT: String(CDP_PORT + 2000),
      } : {}),
      USERPROFILE: launchHome,
    },
  };
  if (DEV_MODE) {
    spawnOptions.cwd = DEV_DESKTOP_DIR;
    // Surface webpack / electron-forge output so a compile failure isn't
    // hidden behind a 180s opaque CDP timeout.
    spawnOptions.stdio = 'inherit';
    // POSIX: put the child in its own process group so killProcessTree
    // can take down electron-forge + webpack-dev-server + Electron via
    // a single negative-PID signal. Windows uses taskkill /F /T instead.
    if (process.platform !== 'win32') {
      spawnOptions.detached = true;
    }
    if (process.platform === 'win32') {
      spawnCmd = 'cmd.exe';
      spawnArgs = ['/c', 'npm', 'run', 'start', '--', ...rstudioArgs];
    } else {
      spawnCmd = 'npm';
      spawnArgs = ['run', 'start', '--', ...rstudioArgs];
    }
    console.log(`Starting RStudio dev build via "npm run start" in ${DEV_DESKTOP_DIR} (CDP port ${CDP_PORT})...`);
  } else {
    spawnCmd = RSTUDIO_PATH;
    spawnArgs = rstudioArgs;
    console.log(`Starting RStudio with CDP on port ${CDP_PORT}...`);
  }
  const rstudioProcess = spawn(spawnCmd, spawnArgs, spawnOptions);
  // Backstop: if the worker exits without running shutdownRStudio (e.g. an
  // interrupted run whose graceful teardown was skipped), force-kill the
  // RStudio process tree on the way out so it isn't orphaned. The dev-mode
  // tree is detached into its own group and would otherwise survive the run.
  trackForReaping(rstudioProcess, () => killProcessTree(rstudioProcess));
  const launchTarget = DEV_MODE ? `npm run start (cwd ${DEV_DESKTOP_DIR})` : RSTUDIO_PATH;
  let launchError: Error | undefined;
  rstudioProcess.on('error', (err) => {
    launchError = new Error(`Failed to launch RStudio (${launchTarget}): ${err.message}`);
  });
  // `'error'` only fires on spawn-level failures (ENOENT). An exit with a
  // non-zero code -- missing npm script, webpack abort, electron-forge
  // crash -- would otherwise sit unnoticed for the full CDP-wait timeout.
  // We only treat code !== 0 as an error; code === null means the process
  // was killed by signal (typically our own killProcessTree during
  // teardown), which isn't a launch failure.
  rstudioProcess.on('exit', (code, signal) => {
    if (code !== null && code !== 0) {
      launchError = new Error(
        `RStudio process (${launchTarget}) exited prematurely with code ${code}${signal ? ` (signal ${signal})` : ''}`,
      );
    }
  });
  console.log(`RStudio process started (PID: ${rstudioProcess.pid})`);

  // Poll for CDP availability instead of a fixed sleep. RStudio Desktop
  // typically has CDP up in 3-5s on a developer machine; capping at
  // TIMEOUTS.rstudioStartup keeps the overall safety margin. Dev mode is
  // slower because electron-forge has to run a webpack build before
  // Electron starts, so we extend the deadline only on that path.
  let browser: Browser | undefined;
  const startupTimeout = DEV_MODE ? DEV_STARTUP_TIMEOUT_MS : TIMEOUTS.rstudioStartup;
  const cdpDeadline = Date.now() + startupTimeout;
  let lastConnectErr: unknown;
  while (Date.now() < cdpDeadline) {
    if (launchError) {
      killProcessTree(rstudioProcess);
      throw launchError;
    }
    try {
      browser = await chromium.connectOverCDP(CDP_URL, { timeout: 5000 });
      break;
    } catch (err) {
      lastConnectErr = err;
      await sleep(250);
    }
  }
  if (!browser) {
    killProcessTree(rstudioProcess);
    throw new Error(
      `Failed to connect to CDP at ${CDP_URL} within ${startupTimeout}ms: ${(lastConnectErr as Error)?.message ?? 'unknown'}`,
    );
  }

  attachLaunchDebug(browser);

  // If anything fails after CDP connect, kill the process to avoid orphaning RStudio.
  try {
    // Gated diagnostic for the post-CDP wait. Helps compare what the test
    // fixture is waiting on against what's visible in the UI. Enabled by
    // PW_DEBUG_LAUNCH=1, same flag as attachLaunchDebug above.
    const launchDebug =
      process.env.PW_DEBUG_LAUNCH === '1' || process.env.PW_DEBUG_LAUNCH === 'true';
    const launchT0 = Date.now();
    const logLaunchStep = (label: string): void => {
      if (launchDebug) {
        console.log(`[launch-timing] +${(Date.now() - launchT0).toString().padStart(5, ' ')}ms ${label}`);
      }
    };
    logLaunchStep('CDP connected; polling for window.rstudio.ready');

    // The splash screen and (in GWT super dev mode) a transient "Compiling
    // RStudio" page both flash before the real app loads, and the compiling
    // page briefly exposes window.rstudio. Polling for window.rstudio.ready
    // === true cuts through both: ApplicationAutomation initializes ready to
    // false and only flips it on DeferredInitCompletedEvent, so the transient
    // page never matches and we proceed exactly when R-to-GWT roundtrips are
    // safe (no separate stability window needed).
    const pageDeadline = Date.now() + PAGE_READY_TIMEOUT_MS;
    let page: Page | undefined;
    let bridgeFirstSeen = false;

    while (Date.now() < pageDeadline && !page) {
      for (const ctx of browser.contexts()) {
        for (const candidate of ctx.pages()) {
          if (candidate.isClosed()) continue;
          try {
            const state = await candidate.evaluate(() => {
              const r = window.rstudio;
              return {
                hasBridge: typeof r?.commands?.activateConsole === 'function',
                ready: r?.ready === true,
              };
            });
            if (state.hasBridge && !bridgeFirstSeen) {
              bridgeFirstSeen = true;
              logLaunchStep('window.rstudio bridge installed (ready=false)');
            }
            if (state.hasBridge && state.ready) {
              page = candidate;
              break;
            }
          } catch {
            // Page may be navigating or closing during splash -> main transition.
          }
        }
        if (page) break;
      }
      if (page) break;
      await sleep(250);
    }
    if (!page) {
      throw new Error(
        `GWT app did not finish loading within ${PAGE_READY_TIMEOUT_MS}ms (window.rstudio.ready never became true)`,
      );
    }
    logLaunchStep('window.rstudio.ready === true');

    // Dismiss any "save changes" modal from a previous interrupted run.
    // Use isVisible() (snapshot, no wait) to gate the click -- the prior
    // form passed timeout: 3000 to click(), which spends the full 3s waiting
    // when no dialog exists (the common case with a fresh per-spec sandbox).
    const dontSaveBtn = page.locator(
      "button:has-text('Don\\'t Save'), button:has-text('Do not Save'), #rstudio_dlg_no",
    ).first();
    if (await dontSaveBtn.isVisible()) {
      await dontSaveBtn.click();
      console.log('Dismissed save dialog from previous session');
      await sleep(1000);
    }

    // Dismiss any other modal overlay (e.g. update notification, options dialog).
    const overlay = page.locator('.gwt-PopupPanelGlass').first();
    if (await overlay.isVisible()) {
      await page.keyboard.press('Escape');
      console.log('Dismissed modal overlay during startup');
      await sleep(1000);
    }

    // Activate console (makes it visible without zooming)
    await executeCommand(page, 'activateConsole');
    logLaunchStep('activateConsole dispatched');

    // Wait for the console input to be visible AND R to be idle (no
    // rstudio-console-busy class on #rstudio_console_input). The latter is
    // what GWT sets while R is executing -- visibility alone can occur a
    // beat before R is ready to accept input.
    await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: TIMEOUTS.consoleReady });
    logLaunchStep('console input visible');
    await page.waitForFunction(
      () => {
        const el = document.getElementById('rstudio_console_input');
        return !!el && !el.classList.contains('rstudio-console-busy');
      },
      null,
      { timeout: TIMEOUTS.consoleReady, polling: 100 },
    );
    logLaunchStep('console input not busy');
    console.log('RStudio console is ready');

    // rsession logs land in RSTUDIO_DATA_HOME/log (see core/system/Xdg.cpp).
    const logDir = path.join(tempConfig.dataHome, 'log');
    return {
      page,
      browser,
      rstudioProcess,
      configRoot,
      logDir,
      dataHome: tempConfig.dataHome,
      requestedTestManifest: tempConfig.requestedTestManifest,
    };
  } catch (err) {
    await browser?.close().catch(() => {});
    killProcessTree(rstudioProcess);
    throw err;
  }
}

/**
 * A snapshot of running RStudio processes: pid -> start-time key. Keying on
 * (pid, start time) rather than bare pid means a PID the OS recycled between
 * two snapshots -- routine on Windows -- doesn't read as "the same process".
 */
export type RStudioProcessSnapshot = ReadonlyMap<number, string>;

/**
 * Relaunch RStudio after a full quit+restart (e.g. uninstall Posit Assistant).
 * The doRestart() flow quits Electron entirely and opens a new window without
 * our CDP flag. We wait for the old process to exit, kill the non-CDP restart
 * instance, and launch a fresh CDP-enabled session.
 *
 * `processesBefore` must be captured with snapshotRStudioProcesses() BEFORE
 * the restart is triggered (e.g. before confirming the uninstall dialog).
 * The restart instance is spawned by the old Electron main as part of its
 * quit sequence, so a snapshot taken here would race that spawn: on a loaded
 * runner the restart instance's main process is already running and lands in
 * the "before" set, and the diff below spares it. That mattered far beyond a
 * leaked process: the restart instance inherits the old main's open handles
 * (application-launch.ts spawns it from the dying Electron), including the
 * CDP listen socket on Windows, so the spared orphan kept the worker's CDP
 * port in LISTEN -- unkillable via its owning PID, unbindable by every later
 * launch -- and the shard burned its full 2h job timeout (rstudio#18522).
 * Killing everything not in the caller's snapshot still preserves an RStudio
 * instance a developer had open before the test began.
 */
export async function relaunchAfterRestart(
  session: DesktopSession,
  processesBefore: RStudioProcessSnapshot,
): Promise<DesktopSession> {
  const { browser, rstudioProcess, configRoot } = session;

  console.log(`RStudio PIDs before restart: ${[...processesBefore.keys()].join(', ') || 'none'}`);

  // Wait for the old process to exit
  console.log('Waiting for RStudio process to exit...');
  const exitDeadline = Date.now() + 30000;
  while (Date.now() < exitDeadline && rstudioProcess.exitCode === null) {
    await sleep(500);
  }
  if (rstudioProcess.exitCode === null) {
    console.log('WARNING: old process did not exit within 30s');
    killProcessTree(rstudioProcess);
  }
  console.log(`Old RStudio exited (code ${rstudioProcess.exitCode})`);
  await browser.close().catch(() => {});

  // Wait for the non-CDP restart instance to spawn
  await sleep(5000);

  // Kill every RStudio process that wasn't already running when the caller
  // took its snapshot -- i.e. the non-CDP restart instance.
  const processesAfter = snapshotRStudioProcesses();
  const newPids = [...processesAfter]
    .filter(([pid, start]) => processesBefore.get(pid) !== start)
    .map(([pid]) => pid);
  console.log(`RStudio PIDs after restart: ${[...processesAfter.keys()].join(', ') || 'none'}`);
  console.log(`New PIDs to kill: ${newPids.join(', ') || 'none'}`);

  for (const pid of newPids) {
    try {
      if (process.platform === 'win32') {
        // /T kills the entire process tree
        execSync(`taskkill /F /T /PID ${pid}`, { stdio: 'pipe' });
      } else {
        execSync(`kill -9 ${pid}`, { stdio: 'ignore' });
      }
      console.log(`Killed PID ${pid}`);
    } catch {
      // Process may have already exited
    }
  }
  await sleep(3000);

  return launchRStudio(configRoot);
}

/**
 * Snapshot the RStudio processes currently running, keyed pid -> start time.
 *
 * Matched case-insensitively (-i): the executable is `RStudio` on macOS but
 * `rstudio` on Linux, and a case-sensitive `pgrep -x rstudio` silently matched
 * nothing on macOS -- the `|| true` turns pgrep's no-match exit 1 into empty
 * output, so relaunchAfterRestart saw an empty before/after diff and left the
 * post-restart instance running for the rest of the run.
 */
export function snapshotRStudioProcesses(): RStudioProcessSnapshot {
  const snapshot = new Map<number, string>();
  try {
    if (process.platform === 'win32') {
      // StartTime can throw for a process that exits mid-enumeration or is
      // inaccessible; record those as 'unknown' rather than dropping them --
      // an entry that matches by accident only ever spares a process, while
      // a dropped one could get an unrelated pre-existing instance killed.
      const output = execSync(
        `powershell.exe -NoProfile -Command "Get-Process rstudio -ErrorAction SilentlyContinue | ForEach-Object { $t = 'unknown'; try { $t = $_.StartTime.Ticks } catch { }; Write-Output ($_.Id.ToString() + '=' + $t) }"`,
        { encoding: 'utf-8' }
      ).trim();
      for (const line of output ? output.split('\n') : []) {
        const [pid, start] = line.trim().split('=');
        if (Number.isInteger(Number(pid)) && Number(pid) > 0) {
          snapshot.set(Number(pid), start || 'unknown');
        }
      }
    } else {
      const output = execSync('pgrep -ix rstudio 2>/dev/null || true', { encoding: 'utf-8' }).trim();
      const pids = output ? output.split('\n').map(Number).filter(n => Number.isInteger(n) && n > 0) : [];
      for (const pid of pids) {
        try {
          const start = execSync(`ps -p ${pid} -o lstart=`, { encoding: 'utf-8' }).trim();
          snapshot.set(pid, start || 'unknown');
        } catch {
          // Process exited between pgrep and ps
        }
      }
    }
  } catch (err) {
    console.log(`WARNING: snapshotRStudioProcesses() failed, returning empty snapshot: ${err}`);
  }
  return snapshot;
}

/**
 * Graceful shutdown: q() in console, close browser, kill process if it
 * hasn't exited on its own.
 *
 * `browser.close()` over a CDP connection only disconnects the CDP session
 * -- it does not terminate the underlying Electron process. And `q()`
 * cascading to a full Electron quit is best-effort (a pending modal, a
 * hung renderer, etc. can leave Electron alive after rsession exits). So
 * after attempting graceful shutdown we always verify the process tree
 * actually exited, and force-kill if not.
 *
 * No per-spec config-tree cleanup -- the sandbox-wide globalTeardown
 * removes everything under PW_SANDBOX at end of run.
 */
export async function shutdownRStudio(session: DesktopSession): Promise<void> {
  const { page, browser, rstudioProcess } = session;

  // Dismiss any modal dialogs the test left open. An open GWT modal (Global
  // Options, Import Dataset, ...) blocks the Electron close path: the
  // renderer's quit confirmation prompts queue behind the existing modal and
  // q(save="no") never gets a chance to cascade to a full quit (#17790).
  //
  // Each graceful phase gets a deadline: a page left mid-transition can make
  // `page.evaluate` reject with "context was destroyed", but a transition
  // that never completes (the #18394 wedge) makes it hang instead, and this
  // teardown is on the path of an interrupted run flushing its report. The
  // browser close and force-kill below always run either way.
  try {
    await withDeadline(dismissAllModals(page), 10_000, 'dismiss modals at shutdown');
  } catch {
    // Page context may already be gone or wedged; we still force-kill below.
  }

  // Close all source files without prompting to save.
  try {
    await withDeadline(documentCloseAllNoSave(page), 15_000, 'close documents at shutdown');
    await sleep(1000);
  } catch {
    // Page context may already be gone or wedged; we still force-kill below.
  }

  try {
    await withDeadline(executeInConsole(page, 'q(save = "no")'), 15_000, 'quit R at shutdown');
  } catch {
    // Console may already be unresponsive; we still force-kill below.
  }
  await browser.close().catch(() => {});

  // Wait briefly for Electron to exit on its own, then force-kill if it
  // hasn't. Polling avoids a fixed sleep when the graceful path works.
  const exitDeadline = Date.now() + 5000;
  while (Date.now() < exitDeadline && rstudioProcess.exitCode === null && rstudioProcess.signalCode === null) {
    await sleep(100);
  }
  if (rstudioProcess.exitCode === null && rstudioProcess.signalCode === null) {
    killProcessTree(rstudioProcess);
  }
}


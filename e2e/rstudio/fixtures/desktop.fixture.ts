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
import { rLibsUserTemplate } from './r-libs-setup';

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
const sharedDataHome = () => path.join(sandboxRoot(), 'data-home');
const sharedUserHome = () => path.join(sandboxRoot(), 'user-home');

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
export const CDP_PORT = Number(process.env.PW_CDP_PORT) || (9231 + Math.floor(Math.random() * 69));
export const CDP_URL = `http://localhost:${CDP_PORT}`;

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
}

// Gated diagnostic: when PW_DEBUG_LAUNCH=1 is set, attach listeners to every
// existing and future page in every context, so we can see the navigation /
// load / console / error sequence that produces the renderer's "double load"
// behavior during startup. Output is prefixed `[debug-launch]` with relative
// timestamps. Leave the flag unset for normal runs.
function attachLaunchDebug(browser: Browser): void {
  if (process.env.PW_DEBUG_LAUNCH !== '1' && process.env.PW_DEBUG_LAUNCH !== 'true') {
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
}

/**
 * Create an isolated RStudio config directory tree with a prefs file
 * built by merging fixtures/base-prefs.jsonc with an optional override
 * from PW_RSTUDIO_PREFS_OVERRIDE. Plumbed into RStudio via
 * RSTUDIO_CONFIG_* env vars at spawn time so the user's real profile
 * is untouched.
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
  for (const d of [configHome, configDir, electronUserData]) {
    fs.mkdirSync(d, { recursive: true });
  }

  const basePrefs = readPrefsFile(BASE_PREFS_PATH, 'base');
  const overridePath = process.env[OVERRIDE_PREFS_ENV];
  const overridePrefs = overridePath ? readPrefsFile(overridePath, OVERRIDE_PREFS_ENV) : {};
  const prefs = { ...basePrefs, ...overridePrefs };

  fs.writeFileSync(
    path.join(configHome, 'rstudio-prefs.json'),
    JSON.stringify(prefs, null, 2),
  );

  // Pre-seed electron-store's config.json with explicit windowBounds. The
  // schema default (1200x900) gets clamped by positionAndEnsureVisible to the
  // workArea of whatever display is attached -- on macOS GH Actions runners
  // that's around 1024x645, which is small enough that the left column's
  // source pane drops under DualWindowLayoutPanel's 60px snap threshold and
  // the Console gets promoted to MAXIMIZE state. Locking the renderer at
  // 1400x900 keeps both panes comfortably above the snap threshold.
  // Requested bounds that intersect any display.workArea are used as-is
  // (window-utils.ts:191), so a 1400x900 rect at the origin sticks even on
  // narrower virtual displays.
  fs.writeFileSync(
    path.join(electronUserData, 'config.json'),
    JSON.stringify(
      { view: { windowBounds: { x: 0, y: 0, width: 1400, height: 900, maximized: false } } },
      null,
      2,
    ),
  );

  return { root, configHome, configDir, electronUserData };
}

// Cold CI runners can take longer than a developer machine to clear the
// GWT-ready check (JS download/parse, R session boot, ApplicationAutomation
// init, DeferredInitCompletedEvent). PW_GWT_READY_TIMEOUT_MS overrides
// explicitly; otherwise default to 60s under CI and the previous 30s locally.
const PAGE_READY_TIMEOUT_MS =
  Number(process.env.PW_GWT_READY_TIMEOUT_MS) ||
  (process.env.CI ? 60000 : 30000);

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
 */
export async function launchRStudio(existingConfigRoot?: string): Promise<DesktopSession> {
  const maxAttempts = Math.max(1, Number(process.env.PW_LAUNCH_ATTEMPTS) || 2);
  let lastError: unknown;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await launchRStudioOnce(existingConfigRoot);
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
  throw lastError instanceof Error ? lastError : new Error(String(lastError));
}

async function launchRStudioOnce(existingConfigRoot?: string): Promise<DesktopSession> {
  // Clean up any existing RStudio on our specific CDP port. The port is
  // random per worker (9231-9299), so a collision is rare -- only happens
  // when an orphaned process from a prior interrupted run is still bound.
  console.log(`CDP port: ${CDP_PORT}`);
  console.log(`Cleaning up any RStudio on port ${CDP_PORT}...`);
  try {
    if (process.platform === 'win32') {
      execSync(
        `powershell.exe -NoProfile -Command "(Get-NetTCPConnection -LocalPort ${CDP_PORT} -ErrorAction SilentlyContinue).OwningProcess | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }"`,
        { encoding: 'utf-8', stdio: 'pipe' }
      );
    } else {
      execSync(`lsof -ti :${CDP_PORT} | xargs kill -9 2>/dev/null`, { stdio: 'ignore' });
    }
  } catch {
    // No process on that port, that's fine
  }

  // Wait for the port to be free. When nothing was ever bound, the first
  // probe throws immediately and we break out in microseconds. When we
  // just killed something, the OS usually releases the port within a few
  // hundred ms; the 15s ceiling is purely a safety net.
  const portDeadline = Date.now() + 15000;
  while (Date.now() < portDeadline) {
    try {
      if (process.platform === 'win32') {
        const result = execSync(`powershell.exe -NoProfile -Command "Get-NetTCPConnection -LocalPort ${CDP_PORT} -ErrorAction SilentlyContinue"`, { encoding: 'utf-8' });
        if (!result.trim()) break;
      } else {
        execSync(`lsof -i :${CDP_PORT} -t`, { encoding: 'utf-8' });
        // If lsof succeeds, port is still in use -- keep waiting
      }
    } catch {
      break; // No connections on the port
    }
    await sleep(100);
  }

  // Set up the isolated config directory (or reuse one across a restart)
  let tempConfig: TempConfig;
  if (existingConfigRoot) {
    tempConfig = {
      root: existingConfigRoot,
      configHome: path.join(existingConfigRoot, 'config-home'),
      configDir: path.join(existingConfigRoot, 'config-dir'),
      electronUserData: path.join(existingConfigRoot, 'electron-userdata'),
    };
    // Defensively recreate child dirs in case anything cleared them between runs
    for (const d of [tempConfig.configHome, tempConfig.configDir, tempConfig.electronUserData]) {
      fs.mkdirSync(d, { recursive: true });
    }
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
  const spawnOptions: SpawnOptions = {
    env: {
      ...process.env,
      HOME: sharedUserHome(),
      // R expands %p / %v at startup; the resolved path is the same one
      // globalSetup pre-creates and pre-populates in r-libs-setup.ts. Setting
      // this explicitly is necessary because HOME is redirected -- without it,
      // R derives an empty default library inside the per-run sandbox.
      R_LIBS_USER: rLibsUserTemplate(),
      RSTUDIO_CONFIG_DIR: tempConfig.configDir,
      RSTUDIO_CONFIG_HOME: tempConfig.configHome,
      RSTUDIO_CONFIG_ROOT: tempConfig.root,
      RSTUDIO_DATA_HOME: sharedDataHome(),
      RSTUDIO_DISABLE_WHATS_NEW: '1',
      // Suppress the Electron splash screen during automation; otherwise CDP
      // can grab the splash window before the main app loads (see the
      // automation-bridge poll loop below).
      RS_NO_SPLASH: '1',
      USERPROFILE: sharedUserHome(),
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

    return { page, browser, rstudioProcess, configRoot };
  } catch (err) {
    await browser?.close().catch(() => {});
    killProcessTree(rstudioProcess);
    throw err;
  }
}

/**
 * Relaunch RStudio after a full quit+restart (e.g. uninstall Posit Assistant).
 * The doRestart() flow quits Electron entirely and opens a new window without
 * our CDP flag. We wait for the old process to exit, kill the non-CDP instance,
 * and launch a fresh CDP-enabled session.
 */
export async function relaunchAfterRestart(session: DesktopSession): Promise<DesktopSession> {
  const { browser, rstudioProcess, configRoot } = session;

  // Snapshot all RStudio PIDs before the restart so we can identify new ones
  const pidsBefore = getRStudioPids();
  console.log(`RStudio PIDs before restart: ${[...pidsBefore].join(', ') || 'none'}`);

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

  // Find and kill only the NEW RStudio processes (the non-CDP restart).
  // This preserves any other RStudio instance the user has open.
  const pidsAfter = getRStudioPids();
  const newPids = [...pidsAfter].filter(pid => !pidsBefore.has(pid));
  console.log(`RStudio PIDs after restart: ${[...pidsAfter].join(', ') || 'none'}`);
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

/** Get all RStudio PIDs currently running. */
function getRStudioPids(): Set<number> {
  try {
    if (process.platform === 'win32') {
      const output = execSync(
        `powershell.exe -NoProfile -Command "(Get-Process rstudio -ErrorAction SilentlyContinue).Id -join ','"`,
        { encoding: 'utf-8' }
      ).trim();
      return new Set(output ? output.split(',').map(Number) : []);
    } else {
      const output = execSync('pgrep -x rstudio 2>/dev/null || true', { encoding: 'utf-8' }).trim();
      return new Set(output ? output.split('\n').map(Number).filter(n => Number.isInteger(n) && n > 0) : []);
    }
  } catch (err) {
    console.log(`WARNING: getRStudioPids() failed, returning empty set: ${err}`);
    return new Set();
  }
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
  try {
    await dismissAllModals(page);
  } catch {
    // Page context may already be gone; we still force-kill below.
  }

  // Close all source files without prompting to save. If a test left the page
  // in the middle of a navigation (e.g. opening a project triggers a session
  // restart), `page.evaluate` will reject with "context was destroyed" --
  // we don't care, we're shutting down anyway.
  try {
    await documentCloseAllNoSave(page);
    await sleep(1000);
  } catch {
    // Page context may already be gone; we still force-kill below.
  }

  try {
    await executeInConsole(page, 'q(save = "no")');
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


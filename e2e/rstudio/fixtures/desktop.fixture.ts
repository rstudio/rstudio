import type { Page } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser } from 'playwright';
import { spawn, execSync } from 'child_process';
import type { ChildProcess, SpawnOptions } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import stripJsonComments from 'strip-json-comments';
import { TIMEOUTS, RSTUDIO_EXTRA_ARGS, sleep } from '../utils/constants';
import { CONSOLE_INPUT, typeInConsole } from '../pages/console_pane.page';

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

export interface DesktopSession {
  page: Page;
  browser: Browser;
  rstudioProcess: ChildProcess;
  configRoot: string;
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

  return { root, configHome, configDir, electronUserData };
}

/**
 * Launch RStudio with CDP, connect Playwright, and return the session.
 *
 * `existingConfigRoot` lets relaunchAfterRestart reuse the same config
 * directory across a quit-and-restart so prefs/state persist.
 */
export async function launchRStudio(existingConfigRoot?: string): Promise<DesktopSession> {
  // Clean up any existing RStudio on our specific CDP port
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
    await sleep(5000); // Give RStudio time to shut down gracefully
  } catch {
    // No process on that port, that's fine
  }
  await sleep(TIMEOUTS.processCleanup);

  // Wait for port to be released (up to 15 seconds)
  const portDeadline = Date.now() + 15000;
  while (Date.now() < portDeadline) {
    try {
      if (process.platform === 'win32') {
        const result = execSync(`powershell.exe -NoProfile -Command "Get-NetTCPConnection -LocalPort ${CDP_PORT} -ErrorAction SilentlyContinue"`, { encoding: 'utf-8' });
        if (!result.trim()) break;
      } else {
        execSync(`lsof -i :${CDP_PORT} -t`, { encoding: 'utf-8' });
        // If lsof succeeds, port is still in use — keep waiting
      }
    } catch {
      break; // No connections on the port
    }
    await sleep(1000);
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
  // ApplicationAutomation to expose `window.rstudioCallbacks` -- the
  // command-execution and command-state helpers our tests drive instead of
  // typing commands through the console.
  const rstudioArgs = [
    `--remote-debugging-port=${CDP_PORT}`,
    `--user-data-dir=${tempConfig.electronUserData}`,
    '--automation-agent',
    ...RSTUDIO_EXTRA_ARGS,
  ];
  // In dev mode, run `npm run start` in src/node/desktop. The package.json
  // script already invokes `electron-forge start -- --no-sandbox`, and `npm
  // run start -- <args>` appends our args to the script's existing `--`
  // passthrough, so they reach Electron alongside `--no-sandbox`.
  // We deliberately avoid `shell: true`: it would re-parse each argv value
  // through the shell, breaking paths that contain spaces (e.g. when
  // PW_SANDBOX_ROOT points somewhere with whitespace) and changing the
  // semantics of PW_RSTUDIO_EXTRA_ARGS from literal argv to shell text.
  // On Windows, npm is npm.cmd, which Node refuses to spawn directly
  // without shell: true -- go through cmd.exe /c so Node's normal arg
  // quoting still preserves each value literally.
  let spawnCmd: string;
  let spawnArgs: string[];
  const spawnOptions: SpawnOptions = {
    env: {
      ...process.env,
      HOME: sharedUserHome(),
      RSTUDIO_CONFIG_DIR: tempConfig.configDir,
      RSTUDIO_CONFIG_HOME: tempConfig.configHome,
      RSTUDIO_CONFIG_ROOT: tempConfig.root,
      RSTUDIO_DATA_HOME: sharedDataHome(),
      RSTUDIO_DISABLE_WHATS_NEW: '1',
      // Suppress the Electron splash screen during automation; otherwise CDP
      // can grab the splash window before the main app loads (see the
      // desktopHooks-poll loop below).
      RS_NO_SPLASH: '1',
      USERPROFILE: sharedUserHome(),
    },
  };
  if (DEV_MODE) {
    spawnOptions.cwd = DEV_DESKTOP_DIR;
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
  let launchError: Error | undefined;
  rstudioProcess.on('error', (err) => {
    const target = DEV_MODE ? `npm run start (cwd ${DEV_DESKTOP_DIR})` : RSTUDIO_PATH;
    launchError = new Error(`Failed to launch RStudio (${target}): ${err.message}`);
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
    if (launchError) throw launchError;
    try {
      browser = await chromium.connectOverCDP(CDP_URL, { timeout: 5000 });
      break;
    } catch (err) {
      lastConnectErr = err;
      await sleep(250);
    }
  }
  if (!browser) {
    rstudioProcess.kill();
    throw new Error(
      `Failed to connect to CDP at ${CDP_URL} within ${startupTimeout}ms: ${(lastConnectErr as Error)?.message ?? 'unknown'}`,
    );
  }

  // If anything fails after CDP connect, kill the process to avoid orphaning RStudio.
  try {
    // The splash screen briefly holds its own page that gets replaced by the
    // main GWT window. Poll until we find a live page whose window has
    // desktopHooks installed -- that is the main app.
    const pageDeadline = Date.now() + 30000;
    let page: Page | undefined;
    while (Date.now() < pageDeadline && !page) {
      for (const ctx of browser.contexts()) {
        for (const candidate of ctx.pages()) {
          if (candidate.isClosed()) continue;
          try {
            const hasHooks = await candidate.evaluate(
              'typeof window.desktopHooks?.invokeCommand === "function"',
            );
            if (hasHooks === true) {
              page = candidate;
              break;
            }
          } catch {
            // Page may be navigating or closing during splash -> main transition.
          }
        }
        if (page) break;
      }
      if (!page) await sleep(250);
    }
    if (!page) {
      throw new Error('GWT app did not finish loading within 30s (no page with window.desktopHooks)');
    }

    // Dismiss any "save changes" modal from a previous interrupted run
    try {
      const dontSaveBtn = page.locator("button:has-text('Don\\'t Save'), button:has-text('Do not Save'), #rstudio_dlg_no");
      await dontSaveBtn.click({ timeout: 3000 });
      console.log('Dismissed save dialog from previous session');
      await sleep(1000);
    } catch {
      // No dialog, continue normally
    }

    // Dismiss any other modal overlay (e.g. update notification, options dialog)
    try {
      const overlay = page.locator('.gwt-PopupPanelGlass');
      if (await overlay.isVisible({ timeout: 1000 })) {
        await page.keyboard.press('Escape');
        console.log('Dismissed modal overlay during startup');
        await sleep(1000);
      }
    } catch {
      // No overlay
    }

    // Activate console (makes it visible without zooming)
    await page.evaluate("window.desktopHooks.invokeCommand('activateConsole')");

    // Wait for the console input to be visible AND R to be idle (no
    // rstudio-console-busy class on #rstudio_console_input). The latter is
    // what GWT sets while R is executing -- visibility alone can occur a
    // beat before R is ready to accept input.
    await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: TIMEOUTS.consoleReady });
    await page.waitForFunction(
      () => {
        const el = document.getElementById('rstudio_console_input');
        return !!el && !el.classList.contains('rstudio-console-busy');
      },
      null,
      { timeout: TIMEOUTS.consoleReady, polling: 100 },
    );
    console.log('RStudio console is ready');

    return { page, browser, rstudioProcess, configRoot };
  } catch (err) {
    await browser?.close().catch(() => {});
    rstudioProcess.kill();
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
    rstudioProcess.kill();
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
 * Graceful shutdown: q() in console, close browser, kill process.
 *
 * No per-spec config-tree cleanup -- the sandbox-wide globalTeardown
 * removes everything under PW_SANDBOX at end of run.
 */
export async function shutdownRStudio(session: DesktopSession): Promise<void> {
  const { page, browser, rstudioProcess } = session;

  // Close all source files without prompting to save
  await typeInConsole(page, '.rs.api.closeAllSourceBuffersWithoutSaving()');
  await sleep(1000);

  try {
    await typeInConsole(page, 'q(save = "no")');
    await sleep(5000); // Give RStudio time to shut down and release port
    await browser.close();
  } catch {
    await browser.close().catch(() => {});
    // Only force kill if graceful shutdown failed
    rstudioProcess.kill();
  }
}


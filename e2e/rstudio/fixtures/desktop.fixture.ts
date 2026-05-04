import type { Page } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser, BrowserContext } from 'playwright';
import { spawn, execSync } from 'child_process';
import type { ChildProcess } from 'child_process';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import stripJsonComments from 'strip-json-comments';
import { TIMEOUTS, RSTUDIO_EXTRA_ARGS, sleep } from '../utils/constants';
import { CONSOLE_INPUT, typeInConsole } from '../pages/console_pane.page';

const BASE_PREFS_PATH = path.join(__dirname, 'base-prefs.jsonc');
const OVERRIDE_PREFS_ENV = 'PW_RSTUDIO_PREFS_OVERRIDE';

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
  dataHome: string;
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
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'pw_rstudio_config_'));
  const configHome = path.join(root, 'config-home');
  const configDir = path.join(root, 'config-dir');
  const dataHome = path.join(root, 'data-home');
  for (const d of [configHome, configDir, dataHome]) {
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

  return { root, configHome, configDir, dataHome };
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
      dataHome: path.join(existingConfigRoot, 'data-home'),
    };
    // Defensively recreate child dirs in case anything cleared them between runs
    for (const d of [tempConfig.configHome, tempConfig.configDir, tempConfig.dataHome]) {
      fs.mkdirSync(d, { recursive: true });
    }
  } else {
    tempConfig = createTempConfig();
  }
  const configRoot = tempConfig.root;
  console.log(`RStudio config root: ${configRoot}`);

  // Start RStudio with remote debugging enabled
  console.log(`Starting RStudio with CDP on port ${CDP_PORT}...`);
  const args = [`--remote-debugging-port=${CDP_PORT}`, ...RSTUDIO_EXTRA_ARGS];
  const rstudioProcess = spawn(RSTUDIO_PATH, args, {
    env: {
      ...process.env,
      RSTUDIO_CONFIG_ROOT: tempConfig.root,
      RSTUDIO_CONFIG_HOME: tempConfig.configHome,
      RSTUDIO_CONFIG_DIR: tempConfig.configDir,
      RSTUDIO_DATA_HOME: tempConfig.dataHome,
    },
  });
  let launchError: Error | undefined;
  rstudioProcess.on('error', (err) => {
    launchError = new Error(`Failed to launch RStudio at ${RSTUDIO_PATH}: ${err.message}`);
  });
  console.log(`RStudio process started (PID: ${rstudioProcess.pid})`);

  // Wait for RStudio to start
  await sleep(TIMEOUTS.rstudioStartup);
  if (launchError) throw launchError;

  // Connect to CDP and set up the session.
  // If anything fails after spawn, kill the process to avoid orphaning RStudio.
  let browser: Browser | undefined;
  try {
    browser = await chromium.connectOverCDP(CDP_URL);
    const contexts: BrowserContext[] = browser.contexts();
    if (contexts.length === 0) {
      throw new Error('CDP connected but no browser contexts available — RStudio window may not be ready');
    }
    const pages = contexts[0].pages();
    if (pages.length === 0) {
      throw new Error('CDP context has no pages — RStudio window may not be ready');
    }
    const page: Page = pages[0];

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

    // Wait for RStudio's GWT app to fully initialize
    await page.waitForFunction('typeof window.desktopHooks?.invokeCommand === "function"', null, { timeout: 30000 });

    // Activate console (makes it visible without zooming)
    await page.evaluate("window.desktopHooks.invokeCommand('activateConsole')");
    await sleep(2000);

    // Wait for console to be ready
    await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: TIMEOUTS.consoleReady });
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
 */
export async function shutdownRStudio(session: DesktopSession): Promise<void> {
  const { page, browser, rstudioProcess, configRoot } = session;

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

  try {
    fs.rmSync(configRoot, { recursive: true, force: true });
  } catch {
    // Best effort; OS will clean up the temp dir eventually
  }
}


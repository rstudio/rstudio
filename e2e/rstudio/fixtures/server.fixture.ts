import { chromium } from 'playwright';
import type { Browser, Page } from 'playwright';
import { spawn, type ChildProcess } from 'child_process';
import { randomBytes } from 'crypto';
import { createServer } from 'net';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { CONSOLE_INPUT, executeInConsole } from '../pages/console_pane.page';
import { sleep } from '../utils/constants';
import { setPref, documentCloseAllNoSave } from '../utils/commands';
import { rLibsUserTemplate } from './r-libs-setup';

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
const sharedUserHome = () => path.join(sandboxRoot(), 'user-home');
const sharedDataHome = () => path.join(sandboxRoot(), 'data-home');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..');
const DEFAULT_RSERVER_BIN = path.join(REPO_ROOT, 'build', 'src', 'cpp', 'server', 'rserver');
const DEFAULT_RSERVER_CONF = path.join(REPO_ROOT, 'build', 'src', 'cpp', 'conf', 'rserver-dev.conf');
const DEFAULT_DB_MIGRATIONS = path.join(REPO_ROOT, 'src', 'cpp', 'server', 'db');

export interface ServerSession {
  page: Page;
  browser: Browser;
  rserverProcess?: ChildProcess;
  rserverCleanupDirs?: string[];
}

/** Find a free TCP port by binding to port 0 and reading the assigned one. */
async function pickFreePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const srv = createServer();
    srv.on('error', reject);
    srv.listen(0, '127.0.0.1', () => {
      const addr = srv.address();
      if (addr && typeof addr === 'object') {
        const port = addr.port;
        srv.close(() => resolve(port));
      } else {
        srv.close(() => reject(new Error('Could not read port from server')));
      }
    });
  });
}

interface SpawnedServer {
  process: ChildProcess;
  url: string;
  /** Per-worker dirs created outside PW_SANDBOX (kept short for socket-path limits). Cleaned at shutdown. */
  cleanupDirs: string[];
}

/**
 * Spawn a private rserver-dev process with sandboxed env so each worker gets
 * its own server data dir, R config home, and HOME pointing into PW_SANDBOX.
 * Mirrors the Desktop fixture's per-worker isolation.
 *
 * Returns null when the in-tree rserver binary is unavailable -- the caller
 * then falls back to connecting at PW_RSTUDIO_SERVER_URL.
 */
async function spawnSandboxedRserver(): Promise<SpawnedServer | null> {
  const rserverBin = process.env.PW_RSERVER_BIN || DEFAULT_RSERVER_BIN;
  const rserverConf = process.env.PW_RSERVER_CONF || DEFAULT_RSERVER_CONF;
  if (!fs.existsSync(rserverBin) || !fs.existsSync(rserverConf)) {
    return null;
  }

  const port = await pickFreePort();
  const userHome = sharedUserHome();
  const dataHome = sharedDataHome();
  const serverRoot = fs.mkdtempSync(path.join(sandboxRoot(), 'rserver_'));
  // rserver creates Unix-domain sockets and IPC files under server-data-dir.
  // macOS caps sockaddr_un.sun_path at ~104 chars, and Playwright's sandbox
  // root inside /var/folders/.../T/ is already ~70 chars on its own. Anchor
  // server-data-dir under os.tmpdir() with a short prefix so the resulting
  // socket paths stay below the limit.
  const dataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'rsd-'));
  const secureCookieKey = path.join(serverRoot, 'secure-cookie-key');
  const configHome = path.join(serverRoot, 'config-home');
  for (const d of [configHome, userHome, dataHome]) {
    fs.mkdirSync(d, { recursive: true });
  }

  // rserver refuses to start if the secure-cookie-key file does not exist
  // (and there's no permission to create it under /var/lib/rstudio-server).
  // Give it a fresh per-worker key. Must be at least 256 bits (32 chars)
  // per server_core/http/SecureCookie.cpp ensureKeyStrength().
  fs.writeFileSync(secureCookieKey, randomBytes(32).toString('hex'), { mode: 0o600 });

  const env = {
    ...process.env,
    HOME: userHome,
    USERPROFILE: userHome,
    // Mirror the Desktop fixture -- under the redirected HOME, R would
    // otherwise compute an empty default user library. globalSetup
    // pre-creates and pre-populates this same path.
    R_LIBS_USER: rLibsUserTemplate(),
    RS_DB_MIGRATIONS_PATH: process.env.RS_DB_MIGRATIONS_PATH || DEFAULT_DB_MIGRATIONS,
    RSTUDIO_PROJECT_ROOT: process.env.RSTUDIO_PROJECT_ROOT || REPO_ROOT,
    RSTUDIO_CONFIG_HOME: configHome,
    RSTUDIO_DATA_HOME: dataHome,
  };

  const args = [
    `--server-user=${os.userInfo().username}`,
    `--auth-none=1`,
    `--server-daemonize=0`,
    `--www-port=${port}`,
    `--server-data-dir=${dataDir}`,
    `--secure-cookie-key-file=${secureCookieKey}`,
    `--config-file=${rserverConf}`,
    // Forward --automation-agent to every rsession this server spawns so
    // window.rstudio is exposed to the Playwright command bridge in
    // @utils/commands. Matches what desktop.fixture.ts does directly to its
    // single rsession.
    `--automation-agent=1`,
  ];

  console.log(`[server] spawning ${path.basename(rserverBin)} on port ${port}`);
  console.log(`[server] data dir: ${dataDir}`);
  console.log(`[server] HOME: ${userHome}`);

  const proc = spawn(rserverBin, args, {
    env,
    cwd: path.dirname(path.dirname(rserverBin)), // build/src/cpp -- rserver-dev runs here
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  // Surface server logs prefixed for triage. Captured but not failing the
  // test directly -- the URL probe below decides whether the server is up.
  proc.stdout?.on('data', (b) => process.stdout.write(`[rserver:out] ${b}`));
  proc.stderr?.on('data', (b) => process.stderr.write(`[rserver:err] ${b}`));

  let earlyError: Error | undefined;
  proc.on('error', (err) => {
    earlyError = err;
  });
  let earlyExit: { code: number | null; signal: NodeJS.Signals | null } | undefined;
  proc.on('exit', (code, signal) => {
    earlyExit = { code, signal };
  });

  // Poll the server until it responds. Cap at 30s to surface boot failures
  // (missing R, missing migrations, port collision) quickly.
  const url = `http://localhost:${port}`;
  const deadline = Date.now() + 30_000;
  while (Date.now() < deadline) {
    if (earlyError) throw earlyError;
    if (earlyExit) {
      throw new Error(
        `rserver exited before becoming ready: code=${earlyExit.code} signal=${earlyExit.signal}`,
      );
    }
    try {
      const res = await fetch(`${url}/auth-sign-in`, { redirect: 'manual' });
      // Any HTTP response (200, 302, etc.) means the server is accepting requests.
      if (res.status > 0) {
        return { process: proc, url, cleanupDirs: [dataDir] };
      }
    } catch {
      // not up yet
    }
    await sleep(250);
  }
  proc.kill('SIGINT');
  throw new Error(`rserver did not respond at ${url} within 30s`);
}

/**
 * Connect to RStudio Server, log in, and return a ready session. When
 * PW_RSTUDIO_SERVER_URL is unset, a private rserver-dev is spawned per
 * worker with sandboxed env so HOME / data dirs / config dirs are isolated
 * the same way the Desktop fixture isolates them. Set PW_RSTUDIO_SERVER_URL
 * to point at an external server (e.g. CI) to skip the spawn.
 */
export async function launchServer(): Promise<ServerSession> {
  const externalUrl = process.env.PW_RSTUDIO_SERVER_URL;
  const username = process.env.PW_RSTUDIO_SERVER_USER || '';
  const password = process.env.PW_RSTUDIO_SERVER_PASSWORD || '';

  let rserverProcess: ChildProcess | undefined;
  let rserverCleanupDirs: string[] | undefined;
  let serverUrl: string;
  if (externalUrl) {
    const url = new URL(externalUrl);
    if (process.env.PW_RSTUDIO_SERVER_PORT) {
      url.port = process.env.PW_RSTUDIO_SERVER_PORT;
    }
    serverUrl = url.toString().replace(/\/$/, '');
    console.log(`[server] using external URL ${serverUrl}`);
  } else {
    const spawned = await spawnSandboxedRserver();
    if (!spawned) {
      throw new Error(
        `rserver binary not found at ${DEFAULT_RSERVER_BIN}. Build the server (cmake --build build) or set PW_RSTUDIO_SERVER_URL to point at an existing server.`,
      );
    }
    rserverProcess = spawned.process;
    rserverCleanupDirs = spawned.cleanupDirs;
    serverUrl = spawned.url;
  }

  console.log(`Connecting to RStudio Server at ${serverUrl}...`);

  const browser = await chromium.launch({
    headless: false,
    args: ['--window-size=960,540', '--window-position=100,100'],
  });
  const context = await browser.newContext({ viewport: null });
  const page = await context.newPage();

  await page.goto(serverUrl, { waitUntil: 'domcontentloaded' });

  // Log in if a login form is presented. Servers running with --auth-none
  // (e.g. local rserver-dev, our spawn) skip straight to the IDE, so
  // credentials are only required when the form appears.
  const usernameField = page.locator('#username');
  if (await usernameField.isVisible({ timeout: 5_000 }).catch(() => false)) {
    if (!username || !password) {
      throw new Error(
        'Server presented a login form but PW_RSTUDIO_SERVER_USER / PW_RSTUDIO_SERVER_PASSWORD are not set',
      );
    }
    await usernameField.fill(username);
    await page.locator('#password').fill(password);
    await page.locator('#signinbutton').click();
    console.log(`Logged in as ${username}`);
  } else {
    console.log('No login form detected (auth-none mode)');
  }

  const loginTimeout = Number(process.env.PW_RSTUDIO_SERVER_LOGIN_TIMEOUT) || 60_000;
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: loginTimeout });
  console.log('RStudio console is ready');

  // Dismiss any "save changes" modal from a previous interrupted run.
  // Use isVisible() (snapshot, no wait) to gate the click -- click({ timeout })
  // would spend the full timeout when no dialog exists.
  const dontSaveBtn = page.locator(
    "button:has-text('Don\\'t Save'), button:has-text('Do not Save'), #rstudio_dlg_no",
  ).first();
  if (await dontSaveBtn.isVisible()) {
    await dontSaveBtn.click();
    console.log('Dismissed save dialog from previous session');
    await sleep(500);
  }

  await page.getByRole('tab', { name: 'Files' }).click({ timeout: 120_000 });
  await page.waitForSelector('#rstudio_mb_files_touch_file', { state: 'visible', timeout: 120_000 });
  console.log('Files pane toolbar is ready');

  await setPref(page, 'save_workspace', 'never');
  // Disable UI animations (pane minimize/maximize/zoom) so transitions apply
  // synchronously; the animated path runs an async completion automation can
  // race, leaving the Source pane stuck minimized. Mirrors reduced_motion in
  // fixtures/base-prefs.jsonc (desktop). Animation tests override per-test.
  await setPref(page, 'reduced_motion', true);
  await sleep(1000);

  await page.locator(CONSOLE_INPUT).click({ force: true });
  await sleep(500);
  await page.keyboard.press('Control+l');
  await sleep(500);
  console.log('Console cleared');

  return { page, browser, rserverProcess, rserverCleanupDirs };
}

/**
 * Close the server session: close buffers, sign out, close browser, and
 * stop the spawned rserver (if any).
 */
export async function shutdownServer(session: ServerSession): Promise<void> {
  const { page, browser, rserverProcess } = session;

  try {
    await documentCloseAllNoSave(page);
    await sleep(1000);
    await executeInConsole(page, 'q("no")');
    await sleep(2000);
  } catch {
    // Page may already be closed
  }

  await browser.close();

  if (rserverProcess && !rserverProcess.killed) {
    rserverProcess.kill('SIGINT');
    // Give rserver a chance to clean up child rsession processes, then
    // force-kill if it lingers.
    const exited = await new Promise<boolean>((resolve) => {
      const timer = setTimeout(() => resolve(false), 5000);
      rserverProcess.once('exit', () => {
        clearTimeout(timer);
        resolve(true);
      });
    });
    if (!exited) {
      rserverProcess.kill('SIGKILL');
    }
  }

  for (const dir of session.rserverCleanupDirs ?? []) {
    try {
      fs.rmSync(dir, { recursive: true, force: true });
    } catch (err) {
      console.warn(`Failed to remove ${dir}: ${(err as Error).message}`);
    }
  }
}

import { chromium } from 'playwright';
import type { Browser, Page } from 'playwright';
import { execFileSync, spawn, type ChildProcess } from 'child_process';
import { randomBytes } from 'crypto';
import { createServer } from 'net';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { CONSOLE_INPUT, executeInConsole } from '../pages/console_pane.page';
import { sleep } from '../utils/constants';
import { setPref, documentCloseAllNoSave } from '../utils/commands';
import { rLibsUserTemplate, workerRLibsUser } from './r-libs-setup';
import { trackForReaping } from './process-reaper';
import { userHomeForAuthState, strippedProvidersFromEnv } from '../utils/auth';

// PW_SANDBOX is exported by the globalSetup hook in fixtures/sandbox-setup.ts
// before any worker spawns. Resolve lazily so importing this module (for
// --list, type-checking, etc.) doesn't require the environment variable -- the assertion
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
 * Resolve the real rsession binary the spawned rserver would launch, from the
 * rsession-path setting in the conf in use. rserver-dev.conf carries an
 * absolute path into the build tree; installed confs may omit it or use the
 * default relative form ("rsession", resolved against the installation
 * layout), so relative values are tried against the rserver binary's own
 * directory and its parent. Fails loud when nothing resolves -- a wrapper
 * exec'ing a missing binary would otherwise surface as an opaque
 * session-launch failure.
 */
function resolveRsessionPath(rserverBin: string, rserverConf: string): string {
  const conf = fs.readFileSync(rserverConf, 'utf-8');
  const match = conf.match(/^\s*rsession-path=(.+)$/m);
  const configured = match ? match[1].trim() : 'rsession';
  const candidates = path.isAbsolute(configured)
    ? [configured]
    : [
        path.resolve(path.dirname(rserverBin), configured),
        path.resolve(path.dirname(path.dirname(rserverBin)), configured),
      ];
  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  throw new Error(
    `Cannot resolve the rsession binary for the session-env wrapper: ` +
      `rsession-path=${configured} (from ${rserverConf}) resolved to none of: ${candidates.join(', ')}`,
  );
}

/**
 * On macOS, rserver injects DYLD_INSERT_LIBRARIES=<R_HOME>/lib/libR.dylib
 * into every rsession's environment (ServerSessionManager.cpp,
 * launchAndTrackSession). SIP strips DYLD_* variables across the exec of a
 * protected interpreter like /bin/sh, so the wrapper script must re-export
 * it itself. Compute R_HOME the same way rserver does: from the conf's
 * rsession-which-r (falling back to `R` on PATH).
 */
function macosLibRPath(rserverConf: string): string {
  const conf = fs.readFileSync(rserverConf, 'utf-8');
  const match = conf.match(/^\s*rsession-which-r=(.+)$/m);
  const rBinary = match ? match[1].trim() : 'R';
  const rHome = execFileSync(rBinary, ['RHOME'], { encoding: 'utf-8' }).trim();
  return path.join(rHome, 'lib', 'libR.dylib');
}

/** Shell-quote a path for the generated wrapper script. */
function shQuote(value: string): string {
  return `'${value.replace(/'/g, "'\\''")}'`;
}

/**
 * Generate the per-worker rsession wrapper script that delivers the sandbox
 * environment to the rsession (and thus to the AI backends it spawns).
 * rserver builds each rsession's environment from scratch (runProcess in
 * core/system/PosixSystem.cpp) with HOME taken from the passwd db. Only a
 * short allow-list survives from the rserver process: PATH, MANPATH, LANG,
 * SHELL, the RS_LOG_* family, and the names forwardXdgEnvVars carries (see
 * the env block in spawnSandboxedRserver). HOME and R_LIBS_USER are not among
 * them, so setting those on rserver would accomplish nothing (#18348). The
 * wrapper runs as the session user after that environment is built, so its
 * exports win:
 *  - HOME: the sandbox user-home (honoring aiAuth-stripped variants), where
 *    auth.setup provisioned the Posit AI token store and Copilot's auth.db.
 *  - R_LIBS_USER: under the redirected HOME, R would otherwise compute an
 *    empty default user library and the session would not see the packages
 *    globalSetup pre-populated. Resolved with workerRLibsUser() to match
 *    desktop.fixture.ts -- the shared template on single-worker runs, a
 *    per-worker hermetic clone when running in parallel. The value keeps R's
 *    own %p/%v tokens; R expands them itself.
 *  - GITHUB_COPILOT_AUTH_TOKEN_ENCRYPTION=false: the sandbox auth.db is
 *    written plaintext (#18205; matches desktop.fixture.ts), and under the
 *    redirected HOME macOS has no login keychain.
 *  - XDG_CONFIG_HOME unset: rserver's xdg filter forwards a developer-shell
 *    value to the session, and the copilot-language-server resolves its
 *    config dir from XDG_CONFIG_HOME before HOME -- unset, it falls back to
 *    $HOME/.config/github-copilot, inside the sandbox.
 */
function writeRsessionWrapper(serverRoot: string, userHome: string, rserverBin: string, rserverConf: string): string {
  const rsessionBin = resolveRsessionPath(rserverBin, rserverConf);
  const lines = [
    '#!/bin/sh',
    `export HOME=${shQuote(userHome)}`,
    `export R_LIBS_USER=${shQuote(workerRLibsUser())}`,
    'export GITHUB_COPILOT_AUTH_TOKEN_ENCRYPTION=false',
    'unset XDG_CONFIG_HOME',
  ];
  if (process.platform === 'darwin') {
    lines.push(`export DYLD_INSERT_LIBRARIES=${shQuote(macosLibRPath(rserverConf))}`);
  }
  lines.push(`exec ${shQuote(rsessionBin)} "$@"`, '');
  const wrapperPath = path.join(serverRoot, 'rsession-wrapper.sh');
  fs.writeFileSync(wrapperPath, lines.join('\n'), { mode: 0o755 });
  return wrapperPath;
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
  // Resolve the launch HOME through the per-test auth state: normally the
  // shared home unchanged; under aiAuth 'none' declarations, a
  // credential-stripped copy of it (see userHomeForAuthState in utils/auth.ts).
  const userHome = userHomeForAuthState(sharedUserHome());
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

  // Deliver the sandbox environment to each rsession through a wrapper
  // script passed as --rsession-path (#18348). Most of what is set on the
  // rserver process below does NOT reach its rsessions -- rserver rebuilds
  // their environment from scratch (runProcess in
  // core/system/PosixSystem.cpp), with HOME from the passwd db, copying over
  // only PATH, MANPATH, LANG, SHELL, the RS_LOG_* family, and the names the
  // xdg filter forwards. HOME and R_LIBS_USER are not on that list, so the
  // wrapper is what redirects the rsession (and the AI backends it spawns)
  // into the sandbox. See writeRsessionWrapper for what it carries.
  const rsessionWrapper = writeRsessionWrapper(serverRoot, userHome, rserverBin, rserverConf);

  const env = {
    ...process.env,
    HOME: userHome,
    USERPROFILE: userHome,
    // The rserver process's own copy, which does NOT reach its rsessions (see
    // the comment above). What gives a session its user library is the
    // wrapper's export, resolved per worker; this one only affects R that
    // rserver itself runs. globalSetup pre-creates and pre-populates the path.
    R_LIBS_USER: rLibsUserTemplate(),
    RS_DB_MIGRATIONS_PATH: process.env.RS_DB_MIGRATIONS_PATH || DEFAULT_DB_MIGRATIONS,
    RSTUDIO_PROJECT_ROOT: process.env.RSTUDIO_PROJECT_ROOT || REPO_ROOT,
    // These two DO reach the rsessions: the xdg filter forwards them by name
    // (forwardXdgEnvVars, core/system/Xdg.cpp), which is the whole reason
    // setting them here works when setting HOME here would not.
    RSTUDIO_CONFIG_HOME: configHome,
    RSTUDIO_DATA_HOME: dataHome,
  };
  // Keep developer-shell config dirs out of the picture entirely: the xdg
  // filter forwards each name below to every rsession, where XDG_CONFIG_HOME
  // would win over the wrapper's HOME for the Copilot config dir and point it
  // outside the sandbox. The wrapper also unsets XDG_CONFIG_HOME
  // session-side; this covers the rserver process itself and the rest of the
  // family.
  //
  // The list is everything forwardXdgEnvVars carries (core/system/Xdg.cpp)
  // except the two names set just above. RSTUDIO_CONFIG_DIR and
  // RSTUDIO_DATA_DIR are deleted rather than redirected because they name
  // system-wide dirs with no per-run sandbox equivalent; dropping them falls
  // back to the built-in defaults, which is the isolation we want. Deleting
  // an unset name changes nothing, so this is safe on a clean shell.
  // XDG_CACHE_HOME is absent on purpose: rserver never forwards it, so
  // deleting it here would imply a relationship that isn't there.
  for (const v of [
    'XDG_CONFIG_HOME', 'XDG_CONFIG_DIRS', 'XDG_DATA_HOME', 'XDG_DATA_DIRS', 'XDG_STATE_HOME',
    'RSTUDIO_CONFIG_DIR', 'RSTUDIO_DATA_DIR',
  ]) {
    delete (env as Record<string, string | undefined>)[v];
  }

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
    // Launch rsessions through the sandbox-env wrapper. CLI args override
    // conf-file values (as --auth-none above already relies on).
    `--rsession-path=${rsessionWrapper}`,
  ];

  console.log(`[server] spawning ${path.basename(rserverBin)} on port ${port}`);
  console.log(`[server] data dir: ${dataDir}`);
  console.log(`[server] HOME: ${userHome}`);

  const proc = spawn(rserverBin, args, {
    env,
    cwd: path.dirname(path.dirname(rserverBin)), // build/src/cpp -- rserver-dev runs here
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  // Backstop: if the worker exits without running shutdownServer (e.g. an
  // interrupted run whose graceful teardown was skipped), SIGTERM rserver on
  // the way out so it isn't orphaned. SIGTERM (not SIGKILL) so rserver runs
  // its own shutdown and reaps its rsession children -- a killed rserver would
  // leave those behind.
  trackForReaping(proc, () => proc.kill('SIGTERM'));

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
 * The external server URL this run targets (PW_RSTUDIO_SERVER_URL, with
 * PW_RSTUDIO_SERVER_PORT applied and any trailing slash removed), or null
 * when the run spawns its own in-tree rserver. Shared by launchServer and
 * the remote-provisioning flows (utils/remote-provision.ts) so both resolve
 * the same server.
 */
export function externalServerUrl(): string | null {
  const externalUrl = process.env.PW_RSTUDIO_SERVER_URL;
  if (!externalUrl) return null;
  const url = new URL(externalUrl);
  if (process.env.PW_RSTUDIO_SERVER_PORT) {
    url.port = process.env.PW_RSTUDIO_SERVER_PORT;
  }
  return url.toString().replace(/\/$/, '');
}

/**
 * Complete the RStudio Server login on `page` (already navigated to the
 * server) and wait until the R console is ready. Fills the login form when
 * one is presented (PW_RSTUDIO_SERVER_USER / PW_RSTUDIO_SERVER_PASSWORD);
 * servers running with --auth-none (e.g. our spawn) skip straight to the
 * IDE, so credentials are only required when the form appears. Shared by
 * launchServer and the remote-provisioning flows.
 */
export async function signInToServer(page: Page): Promise<void> {
  const username = process.env.PW_RSTUDIO_SERVER_USER || '';
  const password = process.env.PW_RSTUDIO_SERVER_PASSWORD || '';

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
}

/**
 * Connect to RStudio Server, log in, and return a ready session. When
 * PW_RSTUDIO_SERVER_URL is unset, a private rserver-dev is spawned per
 * worker with sandboxed env so HOME / data dirs / config dirs are isolated
 * the same way the Desktop fixture isolates them. Set PW_RSTUDIO_SERVER_URL
 * to point at an external server (e.g. CI) to skip the spawn.
 */
export async function launchServer(): Promise<ServerSession> {
  const externalUrl = externalServerUrl();

  let rserverProcess: ChildProcess | undefined;
  let rserverCleanupDirs: string[] | undefined;
  let serverUrl: string;
  if (externalUrl) {
    // The signed-out home (userHomeForAuthState) is applied only on the spawn
    // path below. An external server uses its own home, so an aiAuth 'none'
    // declaration can't be honored here -- fail loud rather than silently run a
    // signed-out test against a server that may well be signed in.
    const stripped = strippedProvidersFromEnv();
    if (stripped.length > 0) {
      throw new Error(
        `test.use({ aiAuth }) requests signing out of ${stripped.join(', ')}, but PW_RSTUDIO_SERVER_URL points at an external server whose credentials the harness cannot control. Unset PW_RSTUDIO_SERVER_URL to use a spawned server, or remove the aiAuth declaration.`,
      );
    }
    serverUrl = externalUrl;
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
  await signInToServer(page);

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
  // fixtures/base-prefs.jsonc (desktop). No test currently needs real
  // animations; one could override this per-test if it did.
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
    // The dispatch returns before the async close chain finishes; wait for
    // the file-backed tabs to actually close so the RPCs removing the docs
    // from the source database land before we quit the session. Accept zero
    // tabs or a lone untitled placeholder (one can auto-spawn when the last
    // tab closes). Best-effort: quitting with a straggler tab only risks a
    // save prompt on the next run, which launchServer already dismisses.
    await page.waitForFunction(
      () => {
        const doc = window.rstudio?.documents.active() ?? null;
        if (doc !== null && doc.path !== null) return false;
        const tabs = document.querySelectorAll(
          "[class*='rstudio_source_panel'] [role='tab']",
        );
        return tabs.length <= 1;
      },
      null,
      { timeout: 10000, polling: 50 },
    ).catch(() => {});
    await executeInConsole(page, 'quit(save = "no")');
    // Wait for the "R Session Ended" overlay (ApplicationEndedPopupPanel in
    // QUIT mode) -- the deterministic signal that the rsession has exited --
    // rather than sleeping a fixed interval with the dead tab still open.
    await page
      .locator('[role="alertdialog"]', { hasText: 'R Session Ended' })
      .waitFor({ state: 'visible', timeout: 10000 });
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

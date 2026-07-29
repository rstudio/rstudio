// Shared helpers for the dev-server tasks (rserver-dev, rserver-stop,
// rserver-status).
//
// These run under plain `node` using its built-in TypeScript type stripping,
// so nothing here may rely on a build step or on installed dependencies --
// only the node: builtins. tasks/package.json marks this directory as ESM so
// the repo root keeps its default (CommonJS) module resolution for stray .js
// files elsewhere in the tree.

import { execFileSync, spawnSync } from 'node:child_process';
import { connect, createServer } from 'node:net';
import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';

/** How a dev instance's GWT front end was brought up. */
export type GwtMode = 'devmode' | 'draft' | 'none';

/**
 * On-disk record of a running dev server, written to
 * <checkout>/.rstudio-dev/instance.json. Everything rserver-stop needs to
 * tear the instance down lives here, so stopping never has to guess.
 */
export interface Instance {
  version: 1;
  checkout: string;
  url: string;
  wwwAddress: string;
  port: number;
  rserverPid: number;
  gwt: GwtMode;
  codeServerPort: number | null;
  gwtPid: number | null;
  dataDir: string;
  secureCookieKey: string;
  startedAt: string;
}

export const STATE_DIR_NAME = '.rstudio-dev';

export function stateDir(checkout: string): string {
  return path.join(checkout, STATE_DIR_NAME);
}

export function instanceFile(checkout: string): string {
  return path.join(stateDir(checkout), 'instance.json');
}

export function logFile(checkout: string, name: string): string {
  return path.join(stateDir(checkout), name);
}

export function step(tag: string, msg: string): void {
  console.log(`[${tag}] ${msg}`);
}

export function fail(tag: string, msg: string): never {
  console.error(`[${tag}] error: ${msg}`);
  process.exit(1);
}

/**
 * RStudio Server does not build or run on Windows, so the dev-server tasks are
 * POSIX-only. Bail with an explanation rather than failing obscurely later on
 * a missing rserver binary.
 */
export function requirePosix(tag: string): void {
  if (process.platform === 'win32') {
    fail(tag, 'RStudio Server is not supported on Windows; these tasks are macOS/Linux only.');
  }
}

// --- argument parsing -------------------------------------------------------

export interface ParsedArgs {
  positional: string[];
  flags: Map<string, string | boolean>;
}

/**
 * Parse `--flag`, `--flag=value`, `--no-flag` and bare positional arguments.
 * Deliberately minimal -- these tasks take a handful of options and pulling in
 * a parser would mean adding a dependency to a dependency-free directory.
 */
export function parseArgs(argv: string[]): ParsedArgs {
  const positional: string[] = [];
  const flags = new Map<string, string | boolean>();

  for (const arg of argv) {
    if (!arg.startsWith('--')) {
      positional.push(arg);
      continue;
    }

    const body = arg.slice(2);
    const eq = body.indexOf('=');
    if (eq >= 0) {
      flags.set(body.slice(0, eq), body.slice(eq + 1));
    } else if (body.startsWith('no-')) {
      flags.set(body.slice(3), false);
    } else {
      flags.set(body, true);
    }
  }

  return { positional, flags };
}

/** Reject unrecognized flags up front so a typo can't be silently ignored. */
export function rejectUnknownFlags(tag: string, args: ParsedArgs, known: string[]): void {
  for (const name of args.flags.keys()) {
    if (!known.includes(name)) {
      fail(tag, `unknown option --${name} (known: ${known.map((k) => `--${k}`).join(', ')})`);
    }
  }
}

export function flagNumber(tag: string, args: ParsedArgs, name: string): number | undefined {
  const raw = args.flags.get(name);
  if (raw === undefined) {
    return undefined;
  }

  const value = Number(raw);
  if (typeof raw !== 'string' || !Number.isInteger(value) || value < 0) {
    fail(tag, `--${name} requires a non-negative integer (got ${String(raw)})`);
  }

  return value;
}

// --- checkout resolution ----------------------------------------------------

/**
 * Resolve the RStudio checkout to operate on: an explicit path argument when
 * given (a sibling git worktree, typically), otherwise the checkout these
 * tasks live in. Validated against two files every checkout has, so a wrong
 * path fails here with a clear message instead of deep inside cmake.
 */
export function resolveCheckout(tag: string, explicit?: string): string {
  const raw = explicit ?? path.resolve(import.meta.dirname, '..');
  const dir = path.resolve(raw);

  if (!fs.existsSync(dir)) {
    fail(tag, `no such directory: ${dir}`);
  }

  const markers = [path.join(dir, 'src', 'cpp', 'CMakeLists.txt'), path.join(dir, 'src', 'gwt', 'build.xml')];
  for (const marker of markers) {
    if (!fs.existsSync(marker)) {
      fail(tag, `${dir} does not look like an RStudio checkout (missing ${path.relative(dir, marker)})`);
    }
  }

  // Normalize through realpath so the state directory of a checkout reached by
  // two different paths (e.g. via a symlink) is the same directory either way.
  return fs.realpathSync(dir);
}

// --- instance state ---------------------------------------------------------

export function readInstance(checkout: string): Instance | null {
  const file = instanceFile(checkout);
  if (!fs.existsSync(file)) {
    return null;
  }

  try {
    return JSON.parse(fs.readFileSync(file, 'utf8')) as Instance;
  } catch {
    // A truncated or hand-edited state file should not wedge the tasks; treat
    // it as "no instance" so the next start overwrites it.
    return null;
  }
}

export function writeInstance(instance: Instance): void {
  fs.mkdirSync(stateDir(instance.checkout), { recursive: true });
  fs.writeFileSync(instanceFile(instance.checkout), `${JSON.stringify(instance, null, 2)}\n`);
}

export function clearInstance(checkout: string): void {
  fs.rmSync(instanceFile(checkout), { force: true });
}

// --- process helpers --------------------------------------------------------

export function isPidAlive(pid: number): boolean {
  try {
    process.kill(pid, 0);
    return true;
  } catch (e) {
    // EPERM means the process exists but belongs to another user, which still
    // counts as alive for our purposes.
    return (e as NodeJS.ErrnoException).code === 'EPERM';
  }
}

/** The full command line of a running process, or null if it is gone. */
export function pidCommand(pid: number): string | null {
  try {
    const out = execFileSync('ps', ['-p', String(pid), '-o', 'command='], { encoding: 'utf8' });
    return out.trim() || null;
  } catch {
    return null;
  }
}

/**
 * True if `pid` is alive and its command line mentions `token`.
 *
 * PIDs are recycled, and a stale instance.json could otherwise aim a SIGKILL
 * at an unrelated process. Every kill in rserver-stop is gated on this.
 */
export function pidMatches(pid: number, token: string): boolean {
  const command = pidCommand(pid);
  return command !== null && command.includes(token);
}

export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Signal a process (`toGroup` false) or its whole process group (`toGroup`
 * true). Tasks spawn children detached, which makes each child a process-group
 * leader, so a group signal reaches its descendants too -- necessary for ant,
 * which forks the actual devmode JVM as a child.
 */
export function signalProcess(pid: number, signal: NodeJS.Signals, toGroup: boolean): void {
  try {
    process.kill(toGroup ? -pid : pid, signal);
  } catch {
    // Already gone, or a group that no longer exists.
  }
}

/** Wait for a pid to exit. Resolves true if it did within `timeoutMs`. */
export async function waitForExit(pid: number, timeoutMs: number): Promise<boolean> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (!isPidAlive(pid)) {
      return true;
    }
    await sleep(200);
  }
  return !isPidAlive(pid);
}

// --- ports ------------------------------------------------------------------

function canBind(port: number, address: string): Promise<boolean> {
  return new Promise((resolve) => {
    const server = createServer();
    server.once('error', () => resolve(false));
    server.once('listening', () => server.close(() => resolve(true)));
    server.listen(port, address);
  });
}

/** True if something accepts a TCP connection on the port right now. */
function isAnswering(port: number): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = connect({ port, host: '127.0.0.1' });
    socket.setTimeout(250);
    socket.once('connect', () => {
      socket.destroy();
      resolve(true);
    });
    // A timeout on loopback is odd enough that treating it as occupied is the
    // safe reading; only an outright refusal counts as free.
    socket.once('timeout', () => {
      socket.destroy();
      resolve(true);
    });
    socket.once('error', () => resolve(false));
  });
}

/**
 * A port is usable only if it passes all three checks. One bind test is not
 * enough, because BSD/macOS lets a specific-address bind and a wildcard bind
 * coexist on the same port: binding 127.0.0.1:8787 succeeds while another
 * server holds *:8787 (and vice versa for 9876), after which the more specific
 * socket silently steals localhost traffic from the server that was there
 * first. Testing both binding styles catches each direction, and the connect
 * probe catches anything already answering regardless of how it bound.
 */
async function isPortFree(port: number, address: string): Promise<boolean> {
  if (!(await canBind(port, address))) {
    return false;
  }

  if (address !== '0.0.0.0' && !(await canBind(port, '0.0.0.0'))) {
    return false;
  }

  return !(await isAnswering(port));
}

/**
 * First free port at or above `start`, probed on the address the caller will
 * actually bind. Scanning upward (rather than asking the kernel for any free
 * port) keeps the common case on the familiar 8787 / 9876 while still letting
 * a second worktree come up alongside the first.
 */
export async function findFreePort(
  tag: string,
  start: number,
  address: string,
  span = 100,
): Promise<number> {
  for (let port = start; port < start + span; port++) {
    if (await isPortFree(port, address)) {
      return port;
    }
  }

  fail(tag, `no free port found in ${start}-${start + span - 1} on ${address}`);
}

/**
 * Validate a port the caller asked for explicitly. Without this an explicit
 * --port would skip the scan above and could land on top of a server that is
 * already running (see isPortFree).
 */
export async function requirePortFree(
  tag: string,
  port: number,
  address: string,
  label: string,
): Promise<number> {
  if (!(await isPortFree(port, address))) {
    fail(tag, `${label} port ${port} is already in use; choose another or stop what is using it.`);
  }

  return port;
}

// --- readiness probing ------------------------------------------------------

export interface WaitOptions {
  timeoutMs: number;
  /** Called when the wait has been running a while, for a progress heartbeat. */
  onWaiting?: (elapsedMs: number) => void;
  /** Abort early with this message if it returns a string (e.g. the process died). */
  checkAbort?: () => string | null;
}

/**
 * Poll `url` until it answers with any HTTP status. Any response at all means
 * the listener is up -- a redirect to a sign-in page is as good as a 200.
 */
export async function waitForHttp(url: string, options: WaitOptions): Promise<void> {
  const deadline = Date.now() + options.timeoutMs;
  const started = Date.now();
  let nextHeartbeat = started + 30_000;

  while (Date.now() < deadline) {
    const abort = options.checkAbort?.();
    if (abort) {
      throw new Error(abort);
    }

    try {
      const response = await fetch(url, { redirect: 'manual' });
      if (response.status > 0) {
        return;
      }
    } catch {
      // Not listening yet.
    }

    if (options.onWaiting && Date.now() >= nextHeartbeat) {
      options.onWaiting(Date.now() - started);
      nextHeartbeat = Date.now() + 30_000;
    }

    await sleep(500);
  }

  throw new Error(`timed out after ${Math.round(options.timeoutMs / 1000)}s waiting for ${url}`);
}

/** Last `count` lines of a log file, for including in a failure message. */
export function tailLog(file: string, count = 20): string {
  try {
    const lines = fs.readFileSync(file, 'utf8').split('\n');
    return lines.slice(-count).join('\n').trim();
  } catch {
    return '';
  }
}

// --- build tree -------------------------------------------------------------

export function buildDir(checkout: string): string {
  return path.join(checkout, 'build');
}

export function cppBuildDir(checkout: string): string {
  return path.join(buildDir(checkout), 'src', 'cpp');
}

/**
 * Configure <checkout>/build if it has never been configured. CMakeCache.txt is
 * the authoritative signal: an empty or partial build/ can exist for various
 * reasons, but only a configured tree has the cache.
 */
export function ensureBuildConfigured(tag: string, checkout: string): void {
  const dir = buildDir(checkout);
  if (fs.existsSync(path.join(dir, 'CMakeCache.txt'))) {
    return;
  }

  step(tag, `Configuring ${dir} (first run; this checkout has no build directory yet)...`);
  fs.mkdirSync(dir, { recursive: true });

  const result = spawnSync('cmake', ['-S', checkout, '-B', dir, '-DCMAKE_EXPORT_COMPILE_COMMANDS=1'], {
    stdio: 'inherit',
  });

  if (result.error?.message.includes('ENOENT')) {
    fail(tag, 'cmake was not found on PATH; install it (or run dependencies/install-dependencies) and retry.');
  }
  if (result.status !== 0) {
    fail(tag, `cmake configure failed with exit code ${result.status ?? 'null'}`);
  }
}

/**
 * Incremental build of the whole C++ tree. When nothing changed this costs a
 * few seconds; the first build of a fresh checkout takes considerably longer,
 * which is why the caller announces it before getting here.
 */
export function runCmakeBuild(tag: string, checkout: string): void {
  ensureBuildConfigured(tag, checkout);

  step(tag, 'Building RStudio Server (cmake --build, incremental)...');
  const result = spawnSync('cmake', ['--build', buildDir(checkout)], { stdio: 'inherit' });

  if (result.status !== 0) {
    fail(tag, `cmake --build failed with exit code ${result.status ?? 'null'}`);
  }
}

/**
 * Absolute path to an executable on PATH, or null. Resolved by scanning PATH
 * directly rather than shelling out to `command -v`, which would need
 * shell: true (deprecated for argument-passing since Node 24, DEP0190).
 */
export function which(program: string): string | null {
  for (const dir of (process.env.PATH ?? '').split(path.delimiter)) {
    if (!dir) {
      continue;
    }

    const candidate = path.join(dir, program);
    try {
      fs.accessSync(candidate, fs.constants.X_OK);
      if (fs.statSync(candidate).isFile()) {
        return candidate;
      }
    } catch {
      // Not here, or not executable.
    }
  }

  return null;
}

/** The current login name, used for rserver's --server-user. */
export function currentUser(): string {
  return os.userInfo().username;
}

// --- teardown ---------------------------------------------------------------

/** Prefix for the per-instance server-data-dir; see makeDataDir in rserver-dev. */
export const DATA_DIR_PREFIX = 'rsd-';

/**
 * Remove a per-instance temp directory, but only if it still looks like one we
 * created. A hand-edited or corrupt instance.json must never turn teardown
 * into a recursive delete of an arbitrary path.
 */
function removeInstanceDir(tag: string, dir: string): void {
  if (!dir || !fs.existsSync(dir)) {
    return;
  }

  const parent = path.dirname(dir);
  const looksOurs =
    path.basename(dir).startsWith(DATA_DIR_PREFIX) && fs.realpathSync(os.tmpdir()) === fs.realpathSync(parent);
  if (!looksOurs) {
    step(tag, `note: leaving ${dir} in place (not a recognized dev-server temp directory)`);
    return;
  }

  try {
    fs.rmSync(dir, { recursive: true, force: true });
  } catch (e) {
    step(tag, `note: could not remove ${dir} (${(e as Error).message})`);
  }
}

/**
 * `ant devmode` starts a file watcher for acesupport as a side effect and
 * records its pid in the checkout. Nothing else stops it, so a dev-server
 * teardown that skipped this would leak a node process per start.
 */
function stopAcesupportWatcher(tag: string, checkout: string): void {
  const pidFile = path.join(checkout, 'src', 'gwt', 'tools', 'acesupport-watcher', 'watcher.pid');
  if (!fs.existsSync(pidFile)) {
    return;
  }

  const pid = Number(fs.readFileSync(pidFile, 'utf8').trim());
  if (Number.isInteger(pid) && pid > 0 && pidMatches(pid, 'watch.js')) {
    signalProcess(pid, 'SIGTERM', false);
    step(tag, `Stopped acesupport watcher (pid ${pid}).`);
  }

  fs.rmSync(pidFile, { force: true });
}

/**
 * Stop one recorded instance and clean up after it. Safe to call when some or
 * all of the processes are already gone -- every kill is gated on the pid
 * still being alive AND still looking like the process we started.
 */
export async function stopInstance(tag: string, instance: Instance): Promise<void> {
  // GWT devmode first: it is the noisier process, and rserver can keep serving
  // the (now static) www while it winds down.
  if (instance.gwtPid !== null) {
    if (pidMatches(instance.gwtPid, 'devmode')) {
      // Signal the whole group: `ant` forks the devmode JVM as a separate
      // process, and killing only the launcher would orphan it.
      signalProcess(instance.gwtPid, 'SIGTERM', true);

      if (!(await waitForExit(instance.gwtPid, 10_000))) {
        signalProcess(instance.gwtPid, 'SIGKILL', true);
      }
      step(tag, `Stopped GWT ${instance.gwt} (pid ${instance.gwtPid}).`);
    } else {
      step(tag, `GWT ${instance.gwt} (pid ${instance.gwtPid}) is no longer running.`);
    }

    stopAcesupportWatcher(tag, instance.checkout);
  }

  if (pidMatches(instance.rserverPid, 'rserver')) {
    // SIGINT, not SIGKILL: rserver reaps its rsession children on a graceful
    // shutdown, and a killed rserver would leave them running.
    signalProcess(instance.rserverPid, 'SIGINT', false);

    if (!(await waitForExit(instance.rserverPid, 15_000))) {
      step(tag, 'rserver did not exit within 15s; escalating to SIGKILL.');
      signalProcess(instance.rserverPid, 'SIGKILL', true);
    }
    step(tag, `Stopped rserver (pid ${instance.rserverPid}).`);
  } else {
    step(tag, `rserver (pid ${instance.rserverPid}) is no longer running.`);
  }

  removeInstanceDir(tag, instance.dataDir);
  clearInstance(instance.checkout);
}

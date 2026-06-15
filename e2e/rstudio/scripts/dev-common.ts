// Shared helpers for the dev-mode Playwright runners (test:desktop-dev,
// test:server-dev). These keep the build and codeserver state current so
// that a `npm run test:*-dev` invocation actually exercises the user's
// uncommitted changes.

import { spawn, spawnSync } from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';

export const REPO_ROOT = path.resolve(__dirname, '../../..');
export const BUILD_DIR = path.join(REPO_ROOT, 'build');

export function step(tag: string, msg: string): void {
  console.log(`\n[${tag}] ${msg}`);
}

export function fail(tag: string, msg: string): never {
  console.error(`\n[${tag}] error: ${msg}`);
  process.exit(1);
}

// Configure the build directory if it has never been initialized. Presence
// of CMakeCache.txt is the authoritative signal -- an empty build/ dir can
// exist for various reasons but only a configured tree has the cache file.
function ensureBuildDirConfigured(tag: string): void {
  if (fs.existsSync(path.join(BUILD_DIR, 'CMakeCache.txt'))) {
    return;
  }

  step(tag, 'Configuring build directory (first run)...');
  fs.mkdirSync(BUILD_DIR, { recursive: true });

  const result = spawnSync(
    'cmake',
    ['-S', REPO_ROOT, '-B', BUILD_DIR, '-DCMAKE_EXPORT_COMPILE_COMMANDS=1'],
    { stdio: 'inherit' },
  );

  if (result.status !== 0) {
    fail(tag, `cmake configure failed with exit code ${result.status ?? 'null'}`);
  }
}

// Run an incremental C++ build. When nothing has changed this is a no-op
// of a few seconds; when something has changed it picks up just those
// translation units. Either way it's cheaper than letting tests run
// against a stale session binary.
export function runCmakeBuild(tag: string): void {
  ensureBuildDirConfigured(tag);

  step(tag, 'Running cmake --build (incremental)...');
  const result = spawnSync('cmake', ['--build', BUILD_DIR], { stdio: 'inherit' });

  if (result.status !== 0) {
    fail(tag, `cmake --build failed with exit code ${result.status ?? 'null'}`);
  }
}

// GWT devmode runs as a Java process whose command line ends with
// `org.rstudio.studio.RStudioSuperDevMode`. That token is unique to this
// project, so matching it avoids colliding with other Java processes.
export function isGwtDevmodeRunning(): boolean {
  if (process.platform === 'win32') {
    // wmic is deprecated on recent Windows but still ships; PowerShell's
    // Get-CimInstance is the modern replacement. Try wmic first since it
    // is cheaper; fall back to PowerShell if wmic is unavailable.
    const wmic = spawnSync(
      'wmic',
      ['process', 'where', "name='java.exe'", 'get', 'commandline'],
      { encoding: 'utf8' },
    );

    if (wmic.status === 0) {
      return /RStudioSuperDevMode/.test(wmic.stdout);
    }

    const ps = spawnSync(
      'powershell',
      [
        '-NoProfile',
        '-Command',
        "Get-CimInstance Win32_Process -Filter \"name = 'java.exe'\" | Select-Object -ExpandProperty CommandLine",
      ],
      { encoding: 'utf8' },
    );

    return ps.status === 0 && /RStudioSuperDevMode/.test(ps.stdout);
  }

  const result = spawnSync('pgrep', ['-f', 'RStudioSuperDevMode'], { stdio: 'pipe' });
  return result.status === 0;
}

// True if a precompiled GWT bootstrap exists on disk. `ant draft` writes
// this; presence is enough to know the dev build can serve a working IDE
// without devmode. We don't bother comparing source mtimes -- if the user
// edits Java without re-running `ant draft`, that's on them.
function hasPrecompiledGwt(): boolean {
  return fs.existsSync(path.join(REPO_ROOT, 'src/gwt/www/rstudio/rstudio.nocache.js'));
}

export function checkGwtBuildReady(tag: string): void {
  step(tag, 'Checking GWT build state...');

  if (isGwtDevmodeRunning()) {
    console.log(`[${tag}] GWT devmode is running.`);
    return;
  }

  if (hasPrecompiledGwt()) {
    console.log(`[${tag}] Precompiled GWT bootstrap present (devmode not running).`);
    return;
  }

  console.log(
    `[${tag}] WARNING: no GWT build available. Tests will likely fail until you run one of:\n` +
      '    (cd src/gwt && ant devmode)   # active development\n' +
      '    (cd src/gwt && ant draft)     # one-shot precompile',
  );
}

// Grace period between asking Playwright to stop and force-killing it, so a
// single Ctrl-C reliably ends even a wedged run. Generous because a graceful
// stop has to quit RStudio (shutdownRStudio waits up to ~8s) before the child
// exits on its own -- which usually happens well inside this window, tripping
// child.on('exit') long before the timer fires. Overridable for tuning/tests.
const STOP_GRACE_MS = Number(process.env.PW_STOP_GRACE_MS) || 30000;

// Spawn `npx playwright test ...` with the supplied args appended and the
// supplied env merged on top of process.env. Inherits stdio so the user
// sees the live test output, and propagates the playwright exit code.
//
// Signal handling: a terminal Ctrl-C, or `kill <pid>` on the launcher PID we
// log below, must tear down the whole run -- the worker, the dev-build
// Electron process / in-tree rserver, and their rsession children -- not just
// this launcher. On POSIX the child is spawned in its own process group so the
// launcher is the sole signal coordinator: it forwards a graceful SIGINT to
// the child's group (the only signal Playwright treats as a cancellation),
// then escalates to SIGKILL if the run doesn't wind down within STOP_GRACE_MS.
// We never signal RStudio directly -- SIGINT to an rsession merely interrupts
// R; the actual shutdown is Playwright's worker-fixture teardown, which quits
// RStudio cleanly (q(save="no") then a SIGTERM-based process-tree kill).
export function runPlaywright(
  tag: string,
  extraArgs: string[],
  env: Record<string, string> = {},
): void {
  step(tag, 'Running Playwright tests...');

  // Give each dev run its own timestamped artifact directory. Playwright wipes
  // the --output dir at the start of every run, so with the default
  // (test-results/) a follow-up run deletes the previous run's failure context
  // -- error-context.md, screenshots, traces -- which is exactly what you want
  // to read after a flaky failure. Pointing --output at a per-run subdir keeps
  // earlier runs intact. The run id is printed so you know where to look, and
  // appears in the "Error Context: ..." paths the list reporter prints.
  //
  // Skipped if the caller passed their own --output. test-results/ is
  // gitignored, so these subdirs are too; prune with `rm -rf test-results/*`.
  const hasOutput = extraArgs.some((a) => a === '--output' || a.startsWith('--output='));
  const runId = new Date().toISOString().replace(/[:.]/g, '-');
  const outputArgs = hasOutput ? [] : [`--output=test-results/${runId}`];
  if (!hasOutput) {
    console.log(`[${tag}] artifacts -> test-results/${runId} (prior runs preserved)`);
  }

  const isWindows = process.platform === 'win32';
  const npx = isWindows ? 'npx.cmd' : 'npx';
  const args = ['playwright', 'test', ...outputArgs, ...extraArgs];

  const child = spawn(npx, args, {
    stdio: 'inherit',
    env: { ...process.env, ...env },
    // POSIX: give the child its own process group so a terminal Ctrl-C is
    // delivered only to this launcher, not also straight to the child group.
    // The launcher then forwards a single, well-timed signal (see below)
    // instead of racing the kernel's group delivery. Windows lacks POSIX
    // process groups, so the child stays in ours there and we signal it
    // directly.
    detached: !isWindows,
  });

  if (child.pid !== undefined) {
    console.log(`[${tag}] Playwright launcher PID: ${child.pid} (kill this to abort the run)`);
  }

  // Deliver a signal to the child -- to its whole process group on POSIX (so
  // npx, Playwright, and the test workers all receive it), or directly on
  // Windows.
  const signalChild = (signal: NodeJS.Signals): void => {
    if (child.pid === undefined)
      return;
    try {
      if (!isWindows)
        process.kill(-child.pid, signal);
      else
        child.kill(signal);
    } catch {
      // Child / group already gone.
    }
  };

  // On the first interrupt, forward a graceful SIGINT and arm a hard-kill
  // fallback; on a repeat (impatient second Ctrl-C, or the run wedging), kill
  // immediately. The launcher stays alive throughout so the inherited stdio
  // and foreground pipeline survive until Playwright finishes tearing down --
  // child.on('exit') below is what actually ends the launcher.
  let stopRequested = false;
  const requestStop = (): void => {
    if (!stopRequested) {
      stopRequested = true;
      signalChild('SIGINT');

      const timer = setTimeout(() => signalChild('SIGKILL'), STOP_GRACE_MS);
      timer.unref();
    } else {
      signalChild('SIGKILL');
    }
  };

  process.on('SIGINT', requestStop);
  process.on('SIGTERM', requestStop);
  process.on('SIGHUP', requestStop);

  // stdio:'inherit' keeps Node's event loop alive until the child exits, so
  // we don't need to await here. Propagate the exit status when the child
  // does finish.
  child.on('exit', (code, signal) => {
    process.exit(signal !== null ? 128 + (signalNumber(signal) ?? 1) : (code ?? 1));
  });
}

// Look up a signal name's numeric code via os.constants.signals when
// available. Falls back to a small allow-list so tests that hit SIGTERM /
// SIGINT report a sensible exit code on platforms with a missing table.
function signalNumber(signal: NodeJS.Signals): number | undefined {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const signalsModule = require('node:os').constants.signals as Record<string, number>;
  if (signal in signalsModule) return signalsModule[signal];
  const fallback: Record<string, number> = { SIGTERM: 15, SIGINT: 2, SIGKILL: 9, SIGHUP: 1 };
  return fallback[signal];
}

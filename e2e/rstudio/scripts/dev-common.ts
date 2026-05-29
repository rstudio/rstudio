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

// Spawn `npx playwright test ...` with the supplied args appended and the
// supplied env merged on top of process.env. Inherits stdio so the user
// sees the live test output, and propagates the playwright exit code.
//
// Logs the Playwright launcher PID up front so a stuck run is easy to
// abort -- `kill <pid>` on this PID tears down the launcher, the per-test
// worker, the dev-build Electron process, and the in-tree rsession in
// one shot.
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

  const npx = process.platform === 'win32' ? 'npx.cmd' : 'npx';
  const args = ['playwright', 'test', ...outputArgs, ...extraArgs];

  const child = spawn(npx, args, {
    stdio: 'inherit',
    env: { ...process.env, ...env },
  });

  if (child.pid !== undefined) {
    console.log(`[${tag}] Playwright launcher PID: ${child.pid} (kill this to abort the run)`);
  }

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

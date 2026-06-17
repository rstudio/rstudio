// Shared helpers for the dev-mode Playwright runners (test:desktop-dev,
// test:server-dev). These keep the build and codeserver state current so
// that a `npm run test:*-dev` invocation actually exercises the user's
// uncommitted changes.

import { spawn, spawnSync } from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';

export const REPO_ROOT = path.resolve(__dirname, '../../..');
export const BUILD_DIR = path.join(REPO_ROOT, 'build');

// The e2e/rstudio directory (where the npm scripts run and where Playwright
// writes its reports). Resolved from this script's location so it's correct
// regardless of the caller's cwd.
export const E2E_DIR = path.resolve(__dirname, '..');

// Stable, well-known path that always points at the most recent HTML report.
// We keep it as a symlink into the timestamped per-run report dir (see
// runPlaywright) so re-running tests never clobbers an earlier report, while
// `npm run test:report` / `npx playwright show-report` still find the latest.
export const HTML_REPORT_LINK = path.join(E2E_DIR, 'playwright-report');

export function step(tag: string, msg: string): void {
  console.log(`\n[${tag}] ${msg}`);
}

export function fail(tag: string, msg: string): never {
  console.error(`\n[${tag}] error: ${msg}`);
  process.exit(1);
}

// Point the stable HTML_REPORT_LINK symlink at the run's timestamped report
// dir. Replaces whatever is already there -- an older symlink, or a real
// directory left by the previous (single-folder) scheme. Best-effort: a
// failure here (e.g. Windows without symlink privilege) is reported but never
// fails the run, since the timestamped dir is always printed too.
export function updateReportSymlink(tag: string, reportDir: string): void {
  try {
    let existing: fs.Stats | undefined;
    try {
      existing = fs.lstatSync(HTML_REPORT_LINK);
    } catch {
      // nothing there yet
    }

    if (existing) {
      if (existing.isDirectory() && !existing.isSymbolicLink())
        fs.rmSync(HTML_REPORT_LINK, { recursive: true, force: true });
      else
        fs.unlinkSync(HTML_REPORT_LINK);
    }

    // Junctions on Windows need an absolute target; a relative target keeps the
    // link portable on POSIX (it stays valid if the dir is moved wholesale).
    const isWin = process.platform === 'win32';
    const target = isWin ? reportDir : path.basename(reportDir);
    fs.symlinkSync(target, HTML_REPORT_LINK, isWin ? 'junction' : 'dir');
  } catch (e) {
    console.log(`[${tag}] note: could not update playwright-report symlink (${(e as Error).message})`);
  }
}

// Print where to find the results after a run. The HTML report is the richest
// view (per-test steps, traces, screenshots); `npm run test:report` opens it.
// Printed on every run, pass or fail, since the report is most useful when
// something failed.
export function printReportLocation(tag: string, reportDir: string): void {
  step(tag, 'Test summary (HTML report):');
  console.log(`    ${reportDir}`);
  console.log(`    latest -> ${HTML_REPORT_LINK}`);
  console.log('    open with: npm run test:report   (npx playwright show-report)');
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

  // Same idea for the HTML report: the 'html' reporter wipes its output folder
  // at the start of each run, so a single fixed folder loses the previous
  // report on re-run. Point it at a per-run folder via PLAYWRIGHT_HTML_OUTPUT_DIR
  // and keep the stable playwright-report symlink pointing at the latest (see
  // updateReportSymlink). A caller-set PLAYWRIGHT_HTML_OUTPUT_DIR wins, in which
  // case we leave the symlink alone and just report their folder.
  const userReportDir = env.PLAYWRIGHT_HTML_OUTPUT_DIR ?? process.env.PLAYWRIGHT_HTML_OUTPUT_DIR;
  const manageReportDir = userReportDir === undefined;
  const reportDir = manageReportDir
    ? path.join(E2E_DIR, `playwright-report-${runId}`)
    : path.resolve(userReportDir);
  const reportEnv = manageReportDir ? { PLAYWRIGHT_HTML_OUTPUT_DIR: reportDir } : {};

  const npx = process.platform === 'win32' ? 'npx.cmd' : 'npx';
  const args = ['playwright', 'test', ...outputArgs, ...extraArgs];

  const child = spawn(npx, args, {
    stdio: 'inherit',
    env: { ...process.env, ...reportEnv, ...env },
  });

  if (child.pid !== undefined) {
    console.log(`[${tag}] Playwright launcher PID: ${child.pid} (kill this to abort the run)`);
  }

  // stdio:'inherit' keeps Node's event loop alive until the child exits, so
  // we don't need to await here. Propagate the exit status when the child
  // does finish.
  child.on('exit', (code, signal) => {
    if (manageReportDir)
      updateReportSymlink(tag, reportDir);
    printReportLocation(tag, reportDir);
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

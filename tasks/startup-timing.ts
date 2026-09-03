// Measure RStudio Desktop startup.
//
//     npm run startup-timing -- [<checkout>] [--runs=N] [--app=<binary>]
//
// Launches RStudio Desktop several times with RSTUDIO_STARTUP_TIMING set, so
// the Electron main process, the rsession process and the GWT client all
// append their startup checkpoints to one JSON-lines file per run, then prints
// a merged timeline (medians across runs) and the longest measured spans.
//
// By default each run gets a fresh config/data home and Electron user-data
// directory, so the numbers describe a clean start rather than whatever
// project or documents happened to be open last; --user-config measures the
// real profile instead.

import { type ChildProcess, spawn } from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';
import {
  fail,
  findFreePort,
  flagNumber,
  parseArgs,
  rebuildFromDirectoryListings,
  rejectUnknownFlags,
  requirePosix,
  resolveCheckout,
  signalProcess,
  sleep,
  stateDir,
  step,
  waitForExit,
} from './common.ts';

const TAG = 'startup-timing';

const KNOWN_FLAGS = ['path', 'runs', 'app', 'out', 'report', 'user-config', 'splash', 'extra', 'timeout', 'help'];

const USAGE = `
Usage: npm run startup-timing -- [<checkout>] [options]

Launches RStudio Desktop repeatedly and reports where startup time goes.

Options:
  --path=<dir>       Checkout to launch the dev build from (default: this one)
  --runs=<n>         Number of launches to measure (default 3)
  --app=<binary>     Measure an installed build instead of the dev build,
                     e.g. /Applications/RStudio.app/Contents/MacOS/RStudio
  --out=<dir>        Where per-run files go (default <checkout>/.rstudio-dev/startup-timing)
  --report=<path>    Skip launching; report on an existing .jsonl file or a
                     directory of run-*/ folders produced by an earlier run
  --user-config      Use the real user config, data and Electron profile
                     (default: a fresh temporary profile per run)
  --no-splash        Suppress the splash screen (RS_NO_SPLASH=1)
  --extra=<args>     Extra command-line arguments for RStudio (space separated)
  --timeout=<sec>    Give up on a launch after this long (default 120)
  --help             Show this message
`.trim();

interface Checkpoint {
  tier: string;
  name: string;
  t: number;
  pid?: number;
  dur?: number;
}

interface Run {
  file: string;
  checkpoints: Checkpoint[];
}

// --- launching --------------------------------------------------------------

interface LaunchOptions {
  checkout: string;
  app: string | null;
  outDir: string;
  userConfig: boolean;
  splash: boolean;
  extraArgs: string[];
  timeoutMs: number;
}

function devBuildOutput(checkout: string): string | null {
  const configured = path.join(checkout, 'build', 'CMakeCache.txt');
  if (fs.existsSync(configured)) {
    return path.join(checkout, 'build', 'src', 'cpp');
  }

  // a worktree bootstrapped without its own C++ build (see
  // scripts/bootstrap-worktree.sh) serves the main checkout's rsession
  const shim = path.join(checkout, 'build-dev-shim');
  if (fs.existsSync(path.join(shim, 'conf', 'rdesktop-dev.conf'))) {
    return shim;
  }

  return null;
}

function hasCheckpoint(file: string, name: string): boolean {
  try {
    return fs.readFileSync(file, 'utf8').includes(`"name":"${name}"`);
  } catch {
    return false;
  }
}

async function launchOnce(index: number, options: LaunchOptions): Promise<Run> {
  const runDir = path.join(options.outDir, `run-${String(index + 1).padStart(2, '0')}`);
  fs.rmSync(runDir, { recursive: true, force: true });
  fs.mkdirSync(runDir, { recursive: true });

  const timingFile = path.join(runDir, 'startup-timing.jsonl');
  const env: NodeJS.ProcessEnv = {
    ...process.env,
    RSTUDIO_STARTUP_TIMING: timingFile,
    RSTUDIO_DISABLE_WHATS_NEW: '1',
  };
  if (!options.splash) {
    env.RS_NO_SPLASH = '1';
  }

  const args: string[] = [];
  if (!options.userConfig) {
    const configHome = path.join(runDir, 'config-home');
    const dataHome = path.join(runDir, 'data-home');
    for (const dir of [configHome, dataHome]) {
      fs.mkdirSync(dir, { recursive: true });
    }
    env.RSTUDIO_CONFIG_HOME = configHome;
    env.RSTUDIO_DATA_HOME = dataHome;

    // the Electron profile is shared by the runs of one measurement (and
    // emptied before the first), so run 1 is a first launch and the rest
    // see whatever the desktop remembers between launches
    const userData = path.join(options.outDir, 'electron-userdata');
    if (index === 0) {
      fs.rmSync(userData, { recursive: true, force: true });
    }
    fs.mkdirSync(userData, { recursive: true });
    args.push(`--user-data-dir=${userData}`);
  }
  args.push(...options.extraArgs);

  let child: ChildProcess;
  const logFile = path.join(runDir, 'launch.log');
  const log = fs.openSync(logFile, 'w');
  if (options.app) {
    child = spawn(options.app, args, { env, detached: true, stdio: ['ignore', log, log] });
  } else {
    const buildOutput = devBuildOutput(options.checkout);
    if (buildOutput === null) {
      fail(TAG, `${options.checkout} has no C++ build (build/) or dev shim (build-dev-shim); build it first`);
    }
    env.RSTUDIO_CPP_BUILD_OUTPUT = buildOutput;

    // electron-forge's webpack dev server and logger default to fixed ports
    // (3000 / 9000); pick free ones so a developer's own dev instance, or
    // an e2e run, can coexist with the measurement (forge.config.js reads these)
    env.RSTUDIO_DESKTOP_DEV_PORT = String(await findFreePort(TAG, 3100, '0.0.0.0'));
    env.RSTUDIO_DESKTOP_LOGGER_PORT = String(await findFreePort(TAG, 9100, '0.0.0.0'));
    child = spawn('npm', ['run', 'start', '--', ...args], {
      cwd: path.join(options.checkout, 'src', 'node', 'desktop'),
      env,
      detached: true,
      stdio: ['ignore', log, log],
    });
  }
  fs.closeSync(log);

  const pid = child.pid;
  if (pid === undefined) {
    fail(TAG, 'failed to launch RStudio');
  }

  let exited = false;
  child.on('exit', () => {
    exited = true;
  });

  // the desktop writes 'timing-harvested' once the client reports deferred
  // init complete and the renderer's timeline has been copied into the file
  // (or 'timing-harvest-timeout' if the client never got that far)
  const deadline = Date.now() + options.timeoutMs;
  let complete = false;
  let harvestTimedOut = false;
  while (Date.now() < deadline) {
    if (hasCheckpoint(timingFile, 'timing-harvested')) {
      complete = true;
      break;
    }
    if (hasCheckpoint(timingFile, 'timing-harvest-timeout')) {
      harvestTimedOut = true;
      break;
    }
    if (exited) {
      break;
    }
    await sleep(100);
  }

  // the child is a process-group leader (detached), so this takes down the
  // renderer/GPU helpers and rsession with it; in dev mode, electron-forge too
  signalProcess(pid, 'SIGKILL', true);
  await waitForExit(pid, 5000);

  if (!complete) {
    const reason = harvestTimedOut
      ? 'the client never completed deferred init, so the timeline is incomplete'
      : exited
        ? 'the process exited before the workbench initialized'
        : 'timed out';
    fail(TAG, `run ${index + 1}: ${reason}; see ${logFile}`);
  }

  return readRun(timingFile);
}

// --- reading ----------------------------------------------------------------

function readRun(file: string): Run {
  const checkpoints: Checkpoint[] = [];
  for (const line of fs.readFileSync(file, 'utf8').split('\n')) {
    if (!line.trim()) {
      continue;
    }
    try {
      checkpoints.push(JSON.parse(line) as Checkpoint);
    } catch {
      // a torn line from a process killed mid-write; skip it
    }
  }
  checkpoints.sort((a, b) => a.t - b.t);
  return { file, checkpoints };
}

function readRuns(target: string): Run[] {
  if (fs.statSync(target).isFile()) {
    return [readRun(target)];
  }

  const runs: Run[] = [];
  for (const entry of fs.readdirSync(target).sort()) {
    const file = path.join(target, entry, 'startup-timing.jsonl');
    if (fs.existsSync(file)) {
      runs.push(readRun(file));
    }
  }
  if (runs.length === 0) {
    fail(TAG, `no startup-timing.jsonl files found under ${target}`);
  }
  return runs;
}

// --- reporting --------------------------------------------------------------

function median(values: number[]): number {
  const sorted = [...values].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 === 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
}

function pad(value: string | number, width: number): string {
  return String(value).padStart(width);
}

/** Everything is reported relative to the Electron process creation time. */
function origin(run: Run): number {
  const start = run.checkpoints.find((c) => c.tier === 'desktop' && c.name === 'process-start');
  return start ? start.t : run.checkpoints[0].t;
}

function key(checkpoint: Checkpoint): string {
  return `${checkpoint.tier}:${checkpoint.name}`;
}

// resource entries are numerous and only interesting by duration; the
// navigation timing and client marks tell the story of the page load
function isTimelineEntry(checkpoint: Checkpoint): boolean {
  return checkpoint.dur === undefined && !checkpoint.name.startsWith('resource:');
}

function report(runs: Run[]): void {
  console.log('');
  console.log(`Runs: ${runs.length}`);
  for (const [index, run] of runs.entries()) {
    const t0 = origin(run);
    const summary: string[] = [];
    for (const [tier, name] of [
      ['session', 'first-wait-for-method'],
      ['client', 'workbench-painted'],
      ['session', 'deferred-init-completed'],
    ]) {
      const found = run.checkpoints.find((c) => c.tier === tier && c.name === name);
      if (found) {
        summary.push(`${name} ${Math.round(found.t - t0)} ms`);
      }
    }
    console.log(`  run ${index + 1}: ${summary.join(', ')}  (${run.file})`);
  }

  // timeline: median time of each checkpoint across the runs it appears in
  const times = new Map<string, number[]>();
  const seen = new Map<string, Checkpoint>();
  for (const run of runs) {
    const t0 = origin(run);
    for (const checkpoint of run.checkpoints) {
      if (!isTimelineEntry(checkpoint)) {
        continue;
      }
      const k = key(checkpoint);
      if (!times.has(k)) {
        times.set(k, []);
        seen.set(k, checkpoint);
      }
      times.get(k)!.push(checkpoint.t - t0);
    }
  }

  const rows = [...times.entries()]
    .map(([k, values]) => ({ checkpoint: seen.get(k)!, t: median(values), n: values.length }))
    .sort((a, b) => a.t - b.t);

  console.log('');
  console.log(`Timeline (median ms since Electron process start${runs.length > 1 ? `, ${runs.length} runs` : ''})`);
  console.log('');
  console.log(`  ${pad('t', 7)}  ${pad('+delta', 7)}  ${'tier'.padEnd(8)} checkpoint`);
  let previous = 0;
  for (const row of rows) {
    const delta = row.t - previous;
    previous = row.t;
    const missing = row.n < runs.length ? `  (${row.n}/${runs.length} runs)` : '';
    console.log(
      `  ${pad(Math.round(row.t), 7)}  ${pad(Math.round(delta), 7)}  ${row.checkpoint.tier.padEnd(8)} ${row.checkpoint.name}${missing}`,
    );
  }

  // spans: anything reported with a duration, longest first
  const durations = new Map<string, number[]>();
  for (const run of runs) {
    for (const checkpoint of run.checkpoints) {
      if (checkpoint.dur === undefined) {
        continue;
      }
      const k = key(checkpoint);
      if (!durations.has(k)) {
        durations.set(k, []);
      }
      durations.get(k)!.push(checkpoint.dur);
    }
  }

  const spans = [...durations.entries()]
    .map(([k, values]) => ({ key: k, dur: median(values) }))
    .sort((a, b) => b.dur - a.dur)
    .slice(0, 25);

  if (spans.length > 0) {
    console.log('');
    console.log('Longest spans (median ms)');
    console.log('');
    for (const span of spans) {
      const [tier, ...rest] = span.key.split(':');
      console.log(`  ${pad(Math.round(span.dur), 7)}  ${tier.padEnd(8)} ${rest.join(':')}`);
    }
  }
  console.log('');
}

// --- main -------------------------------------------------------------------

async function main(): Promise<void> {
  const args = parseArgs(process.argv.slice(2));
  if (args.flags.get('help') === true) {
    console.log(USAGE);
    return;
  }
  rejectUnknownFlags(TAG, args, KNOWN_FLAGS);

  const reportTarget = args.flags.get('report');
  if (typeof reportTarget === 'string') {
    const resolved = path.resolve(reportTarget);
    if (!fs.existsSync(resolved)) {
      fail(TAG, `no such file or directory: ${resolved}`);
    }
    report(readRuns(rebuildFromDirectoryListings(TAG, fs.realpathSync(resolved))));
    return;
  }

  requirePosix(TAG);

  const explicit = args.positional[0] ?? (args.flags.get('path') as string | undefined);
  const checkout = resolveCheckout(TAG, explicit);
  const runs = flagNumber(TAG, args, 'runs') ?? 3;
  const timeoutSec = flagNumber(TAG, args, 'timeout') ?? 120;
  const app = args.flags.get('app');
  const out = args.flags.get('out');
  const extra = args.flags.get('extra');

  // like the checkout, paths from the command line are laundered through
  // directory listings before they reach file operations or spawn
  // (see rebuildFromDirectoryListings)
  let appBinary: string | null = null;
  if (typeof app === 'string') {
    const resolved = path.resolve(app);
    if (!fs.existsSync(resolved)) {
      fail(TAG, `no such file: ${resolved}`);
    }
    appBinary = rebuildFromDirectoryListings(TAG, fs.realpathSync(resolved));
  }

  let outDir = path.join(stateDir(checkout), 'startup-timing');
  if (typeof out === 'string') {
    const resolved = path.resolve(out);
    fs.mkdirSync(resolved, { recursive: true });
    outDir = rebuildFromDirectoryListings(TAG, fs.realpathSync(resolved));
  }

  const options: LaunchOptions = {
    checkout,
    app: appBinary,
    outDir,
    userConfig: args.flags.get('user-config') === true,
    splash: args.flags.get('splash') !== false,
    extraArgs: typeof extra === 'string' ? extra.split(' ').filter((a) => a.length > 0) : [],
    timeoutMs: timeoutSec * 1000,
  };

  fs.mkdirSync(options.outDir, { recursive: true });
  step(TAG, `Measuring ${options.app ?? `the dev build in ${checkout}`} (${runs} run${runs === 1 ? '' : 's'})`);
  step(TAG, `Per-run files under ${options.outDir}`);

  const results: Run[] = [];
  for (let i = 0; i < runs; i++) {
    step(TAG, `run ${i + 1}/${runs}...`);
    results.push(await launchOnce(i, options));

    // let the OS reclaim the killed processes before the next launch
    await sleep(1500);
  }

  report(results);
}

await main();

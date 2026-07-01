// Run the Playwright CLI under a no-output watchdog.
//
// A hung e2e run (e.g. the GWT app never reaches ready, so a test blocks with no
// further output) would otherwise idle until the job's timeout-minutes -- ~2
// hours of a wedged runner. This wrapper streams the child's output through and
// resets a timer on every chunk; if nothing is written for the idle window it
// treats the run as hung, kills the whole process tree, and exits non-zero so
// the shard fails fast. The `list` reporter (added for sharded CI in
// playwright.config.ts) emits a line per test, which is the heartbeat this
// relies on.
//
// Everything after `--` is forwarded to the Playwright CLI, e.g.
//   node run-with-heartbeat.mjs -- test --project desktop-macos --shard 1/2
//
// Idle window: PW_HEARTBEAT_TIMEOUT_SECONDS (default 300).

import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import path from 'node:path';

const IDLE_SECONDS = Number(process.env.PW_HEARTBEAT_TIMEOUT_SECONDS || '300');
const IDLE_MS = IDLE_SECONDS * 1000;

const separator = process.argv.indexOf('--');
const cliArgs = separator === -1 ? [] : process.argv.slice(separator + 1);
if (cliArgs.length === 0) {
  console.error('run-with-heartbeat: no Playwright CLI args given after --');
  process.exit(2);
}

// Resolve the Playwright CLI (the same entry `npx playwright` runs) and invoke it
// with the current node binary. Spawning node directly with an argv array -- no
// shell -- keeps argument quoting identical across bash and PowerShell callers.
const require = createRequire(import.meta.url);
const cli = path.join(path.dirname(require.resolve('playwright/package.json')), 'cli.js');

const isWindows = process.platform === 'win32';

// On POSIX, put the child in its own process group so we can signal the entire
// tree (Playwright's workers plus the RStudio/Electron process it launches). On
// Windows we tear the tree down with `taskkill /T` instead.
const child = spawn(process.execPath, [cli, ...cliArgs], {
  stdio: ['ignore', 'pipe', 'pipe'],
  detached: !isWindows,
});

let timer;
let killedForIdle = false;

function killTree() {
  try {
    if (isWindows)
      spawn('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore' });
    else
      process.kill(-child.pid, 'SIGKILL');
  } catch {
    // Child already exited; nothing to kill.
  }
}

function onIdle() {
  killedForIdle = true;
  process.stderr.write(`\n::error::run-with-heartbeat: no output for ${IDLE_SECONDS}s -- treating the run as hung and terminating it.\n`);
  killTree();
}

function resetTimer() {
  if (timer)
    clearTimeout(timer);
  timer = setTimeout(onIdle, IDLE_MS);
}

child.stdout.on('data', (chunk) => { process.stdout.write(chunk); resetTimer(); });
child.stderr.on('data', (chunk) => { process.stderr.write(chunk); resetTimer(); });

// Forward cancellation (job cancelled / timed out) to the child tree too.
for (const signal of ['SIGINT', 'SIGTERM'])
  process.on(signal, killTree);

console.log(`run-with-heartbeat: watchdog armed (idle timeout ${IDLE_SECONDS}s)`);
resetTimer();

child.on('error', (err) => {
  if (timer)
    clearTimeout(timer);
  console.error(`run-with-heartbeat: failed to start Playwright: ${err.message}`);
  process.exit(1);
});

child.on('exit', (code, signal) => {
  if (timer)
    clearTimeout(timer);
  if (killedForIdle)
    process.exit(124); // conventional timeout exit code
  if (signal)
    process.exit(1);
  process.exit(code ?? 1);
});

// A worker-process backstop that force-kills spawned RStudio / rserver
// children if the worker exits without running the normal fixture teardown
// (shutdownRStudio / shutdownServer).
//
// Playwright's graceful interrupt path *does* run worker-fixture teardown:
// on Ctrl-C the main process sends each worker a __stop__ IPC message, and the
// worker tears down its worker-scoped fixtures before exiting. But a few paths
// skip that teardown -- a teardown that wedges past the worker's force-exit
// timeout, a parent that dies and disconnects, or a crash mid-launch before the
// fixture's own try/catch is in place. In dev mode the RStudio tree is spawned
// detached into its own process group (see desktop.fixture.ts), so it never
// receives the terminal's group SIGINT and would survive indefinitely when
// teardown is skipped. This reaper is the last line of defense for those cases.
//
// Note: a hard SIGKILL of the worker itself bypasses this -- Node runs no
// 'exit' handler on SIGKILL. The launcher's graceful-first signal handling
// (scripts/dev-common.ts) is what keeps us out of that case for `npm run
// test:*-dev`; plain `npx playwright test` relies on Playwright's own graceful
// stop, which routes through the worker teardown path above.

import type { ChildProcess } from 'child_process';

type Killer = () => void;

const killers = new Set<Killer>();
let installed = false;

// Synchronously invoke every registered killer. Runs in a Node 'exit' handler,
// so it must do only synchronous work (no awaits, no async I/O).
function reapAll(): void {
  for (const kill of killers) {
    try {
      kill();
    } catch {
      // Best-effort: the process is already exiting, nothing useful to do.
    }
  }
  killers.clear();
}

function ensureInstalled(): void {
  if (installed) {
    return;
  }
  installed = true;

  // 'exit' fires for normal exits and for any process.exit() -- which is how
  // the Playwright worker terminates, including on its graceful-stop and
  // disconnect paths. We deliberately do NOT register SIGINT/SIGTERM handlers
  // here: the Playwright worker installs its own no-op handlers for those so
  // the main process can coordinate shutdown over IPC, and adding ours would
  // race that.
  process.on('exit', reapAll);
}

/**
 * Register a child process for last-resort reaping on worker exit.
 *
 * `kill` must synchronously terminate the process (and its tree, where
 * applicable). The reaper skips it automatically if the process has already
 * exited, so callers don't need to unregister after a clean shutdown.
 */
export function trackForReaping(proc: ChildProcess, kill: Killer): void {
  ensureInstalled();

  killers.add(() => {
    if (proc.exitCode !== null || proc.signalCode !== null) {
      return;
    }
    kill();
  });
}

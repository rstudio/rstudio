// A session can stop serving requests while its page stays perfectly
// responsive: R code dispatched from the idle event loop -- a later::later()
// callback, e.g. shiny::runApp() scheduled that way -- kept rsession's main
// thread from dequeuing anything (#19236). Nothing DOM-based notices: the
// console shows a prompt with no busy class, client events still flow, and
// every bridge call that awaits an RPC simply never settles. Run 39884727549
// lost a shard that way: two hooks sat in pref writes for 120s each, which
// pushed the output silence past the CI heartbeat's kill window, and the
// SIGKILL left no record of what R had been doing.
//
// This module gives the harness a direct read on the main thread, bounded,
// and a way to record what it was doing when it did not answer.

import { execSync } from 'child_process';
import type { Page, TestInfo } from '@playwright/test';
import { withDeadline } from './deadline';

/**
 * Round-trip budget for the liveness probe. A healthy session answers a
 * ping in milliseconds; one still settling a restart answers within a few
 * seconds. 15s is well past both and still leaves the test its own budget
 * once the probe has failed.
 */
export const SESSION_LIVENESS_DEADLINE_MS = 15_000;

/**
 * Round-trip the session's main thread through `window.rstudio.session.ping()`.
 *
 * Resolves 'alive' once the ping is answered, 'unsupported' against a binary
 * whose bridge predates the method, and rejects with a DeadlineError when
 * the main thread does not answer within `deadlineMs`. The bridge check is
 * bounded too: an evaluate against a page with no JS context (the #18394
 * wedge) would otherwise hang here instead of in the drain that precedes it.
 */
export async function probeSessionLiveness(
  page: Page,
  deadlineMs: number = SESSION_LIVENESS_DEADLINE_MS,
): Promise<'alive' | 'unsupported'> {
  const supported = await withDeadline(
    page.evaluate(() => typeof window.rstudio?.session?.ping === 'function'),
    deadlineMs,
    'session liveness probe (bridge check)',
  );
  if (!supported)
    return 'unsupported';

  await withDeadline(
    page.evaluate(() => window.rstudio!.session!.ping()),
    deadlineMs,
    'session liveness probe (ping RPC)',
  );
  return 'alive';
}

/**
 * Caps on the dump attached to a test: a `sample` report runs to ~130KB per
 * session, and a developer machine can have several dev sessions alive
 * (every one is dumped, since nothing ties a pid to this worker's session).
 */
const DUMP_LIMIT_PER_SESSION = 256 * 1024;
const DUMP_LIMIT = 1024 * 1024;

function run(command: string, timeoutMs: number): string {
  try {
    return execSync(command, { encoding: 'utf-8', timeout: timeoutMs, stdio: ['ignore', 'pipe', 'pipe'] });
  } catch (err) {
    const e = err as { stdout?: string; stderr?: string; message?: string };
    return `${e.stdout ?? ''}${e.stderr ?? ''}(command failed: ${e.message ?? String(err)})\n`;
  }
}

/**
 * Best-effort stack dump of every running rsession, as text.
 *
 * macOS: `sample`, which works unprivileged on the caller's own processes and
 * names frames from the binary's own symbols (that is how #19236 was found).
 * Linux: thread states with their kernel wait channels (always readable),
 * then gdb when it is installed and may attach -- Ubuntu's default
 * ptrace_scope refuses a non-child, so the gdb section may just say so; the
 * CI workflows take a privileged dump of their own at the end of a failed
 * shard (scripts/ci/rsession-diagnostics.sh). Windows: not attempted.
 */
export function dumpRSessionStacks(): string {
  if (process.platform === 'win32')
    return '(rsession stack dump not implemented on Windows)\n';

  const pids = run('pgrep -x rsession', 5_000).split('\n').map((s) => s.trim()).filter((s) => /^\d+$/.test(s));
  if (pids.length === 0)
    return '(no rsession process found)\n';

  const clip = (text: string, limit: number): string =>
    text.length > limit ? text.slice(0, limit) + '\n(truncated)\n' : text;

  const sections: string[] = [];
  for (const pid of pids) {
    const parts: string[] = [`=== rsession pid ${pid} ===`];
    if (process.platform === 'darwin') {
      parts.push(run(`ps -o pid,ppid,etime,stat,command -p ${pid}`, 5_000));
      parts.push(run(`sample ${pid} 1`, 30_000));
    } else {
      parts.push(run(`ps -o pid,ppid,etime,stat,wchan:32,cmd -p ${pid}`, 5_000));
      parts.push('--- threads (tid state wchan) ---');
      parts.push(run(`for t in /proc/${pid}/task/*; do echo "$(basename "$t") $(awk '{print $3}' "$t/stat" 2>/dev/null) $(cat "$t/wchan" 2>/dev/null)"; done`, 5_000));
      parts.push('--- gdb: thread apply all bt ---');
      parts.push(
        run(`command -v gdb >/dev/null && timeout 30s gdb -batch -p ${pid} -ex 'set pagination off' -ex 'thread apply all bt' 2>&1 || echo '(gdb not installed, or not permitted to attach)'`, 40_000),
      );
    }
    sections.push(clip(parts.join('\n'), DUMP_LIMIT_PER_SESSION));
  }
  return clip(sections.join('\n'), DUMP_LIMIT);
}

/**
 * Record a session that stopped answering: the rsession stack dump as an
 * attachment, plus a console line (with the caller's note on the page
 * state), so a watchdog kill downstream can no longer erase the evidence.
 * Returns the error for the caller to throw.
 */
export async function attachDeafSessionDiagnostics(
  testInfo: TestInfo,
  cause: Error,
  pageNote: string,
): Promise<Error> {
  const dump = dumpRSessionStacks();
  await testInfo.attach('rsession-stacks.txt', { body: dump, contentType: 'text/plain' });
  console.error(`[deaf-session] ${cause.message}; ${pageNote}; rsession stack dump attached as rsession-stacks.txt`);
  return new Error(
    `session is not serving requests (${cause.message}). The page answers but the ` +
    'session main thread does not: R is most likely running code dispatched from the ' +
    'idle event loop (#19236) -- see the attached rsession-stacks.txt. Failing fast so ' +
    'the worker restarts with a fresh session.',
  );
}

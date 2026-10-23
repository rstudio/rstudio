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
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import type { Page, TestInfo } from '@playwright/test';
import { withDeadline, DeadlineError } from './deadline';

/**
 * Round-trip budget for the liveness probe. A healthy session answers a
 * ping in milliseconds; one still settling a restart answers within a few
 * seconds. 15s is well past both and still leaves the test its own budget
 * once the probe has failed.
 */
export const SESSION_LIVENESS_DEADLINE_MS = 15_000;

/**
 * Backoff between attempts when a ping is answered with an error rather
 * than left hanging: a dropped XHR, or connection_refused from a session
 * that is still shutting down ahead of a relaunch. Short, since a session
 * in transition answers again within a second or two.
 */
const PING_RETRY_DELAYS_MS = [500, 1000];

/**
 * Round-trip the session's main thread through `window.rstudio.session.ping()`.
 *
 * Resolves 'alive' once the ping is answered, 'unsupported' against a binary
 * whose bridge predates the method, and rejects with a DeadlineError when
 * the main thread does not answer within `deadlineMs`. The bridge check is
 * bounded too: an evaluate against a page with no JS context (the #18394
 * wedge) would otherwise hang here instead of in the drain that precedes it.
 *
 * A ping that is answered with an error is not a deaf session: it is retried
 * a couple of times (the same transport flakes withPrefWriteRetry absorbs for
 * pref writes), and only a persistent error rejects, carrying the RPC's own
 * detail. A timeout is never retried -- the deadline is the signal.
 */
export async function probeSessionLiveness(
  page: Page,
  deadlineMs: number = SESSION_LIVENESS_DEADLINE_MS,
): Promise<'alive' | 'unsupported'> {
  for (let attempt = 0; ; attempt++) {
    try {
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
    } catch (err) {
      if (err instanceof DeadlineError || attempt >= PING_RETRY_DELAYS_MS.length)
        throw err;
      const message = err instanceof Error ? err.message : String(err);
      console.warn(`[session-liveness] ping failed (attempt ${attempt + 1}), retrying: ${message}`);
      await new Promise((resolve) => setTimeout(resolve, PING_RETRY_DELAYS_MS[attempt]));
    }
  }
}

/**
 * The CI diagnostics script, which is also the per-test dump: one
 * implementation of "find every rsession and record what it is doing", so
 * the process-name match and the debugger handling cannot drift apart.
 */
const DIAGNOSTICS_SCRIPT = path.resolve(__dirname, '..', 'scripts', 'ci', 'rsession-diagnostics.sh');

/**
 * Bound on the script. macOS samples each session for a second; on Linux
 * gdb is given 30s per session and escalated to SIGKILL if it ignores the
 * timeout, so one wedged session fits comfortably. Whatever the script
 * wrote before this fires is still collected.
 */
const DUMP_TIMEOUT_MS = 60_000;

/**
 * Caps on the dump attached to a test: a `sample` report runs to ~130KB per
 * session, and a developer machine can have several dev sessions alive
 * (every one is dumped, since nothing ties a pid to this worker's session).
 */
const DUMP_LIMIT_PER_SESSION = 256 * 1024;
const DUMP_LIMIT = 1024 * 1024;

/**
 * Best-effort stack dump of every running rsession, as text: the output of
 * scripts/ci/rsession-diagnostics.sh run unprivileged into a temp dir.
 *
 * macOS: `sample`, which works unprivileged on the caller's own processes and
 * names frames from the binary's own symbols (that is how #19236 was found).
 * Linux: thread states with their kernel wait channels (always readable),
 * then gdb when it is installed and may attach -- Ubuntu's default
 * ptrace_scope refuses a non-child, so the gdb section may just say so; the
 * CI workflows run the same script privileged at the end of a failed shard.
 * Windows: not attempted.
 */
export function dumpRSessionStacks(): string {
  if (process.platform === 'win32')
    return '(rsession stack dump not implemented on Windows)\n';

  const clip = (text: string, limit: number): string =>
    text.length > limit ? text.slice(0, limit) + '\n(truncated)\n' : text;

  const outDir = fs.mkdtempSync(path.join(os.tmpdir(), 'rsession-stacks-'));
  try {
    let log: string;
    try {
      log = execSync(`bash "${DIAGNOSTICS_SCRIPT}" "${outDir}"`, {
        encoding: 'utf-8',
        timeout: DUMP_TIMEOUT_MS,
        stdio: ['ignore', 'pipe', 'pipe'],
      });
    } catch (err) {
      const e = err as { stdout?: string; stderr?: string; message?: string };
      log = `${e.stdout ?? ''}${e.stderr ?? ''}(diagnostics script failed: ${e.message ?? String(err)})\n`;
    }

    const sections = fs.readdirSync(outDir)
      .filter((name) => name.startsWith('rsession-') && name.endsWith('.txt'))
      .sort()
      .map((name) => clip(`=== ${name} ===\n${fs.readFileSync(path.join(outDir, name), 'utf-8')}`, DUMP_LIMIT_PER_SESSION));
    return clip([log, ...sections].join('\n'), DUMP_LIMIT);
  } finally {
    fs.rmSync(outDir, { recursive: true, force: true });
  }
}

/**
 * Record a session that stopped answering: the rsession stack dump as an
 * attachment, plus a console line (with the caller's note on the page
 * state), so a watchdog kill downstream can no longer erase the evidence.
 * Returns the error for the caller to throw.
 *
 * `localSessions` says whether the rsessions run on this machine. Against an
 * external server (PW_RSTUDIO_SERVER_URL) they do not, and a dump of the
 * runner's own processes would only misdirect triage, so none is taken and
 * the message says where to look instead.
 */
export async function attachDeafSessionDiagnostics(
  testInfo: TestInfo,
  cause: Error,
  pageNote: string,
  localSessions: boolean,
): Promise<Error> {
  let evidence: string;
  if (localSessions) {
    const dump = dumpRSessionStacks();
    await testInfo.attach('rsession-stacks.txt', { body: dump, contentType: 'text/plain' });
    evidence = 'see the attached rsession-stacks.txt';
    console.error(`[deaf-session] ${cause.message}; ${pageNote}; rsession stack dump attached as rsession-stacks.txt`);
  } else {
    evidence = 'the session runs on the external server, so no local stack dump was taken; ' +
      'sample or attach a debugger to the rsession on that host';
    console.error(`[deaf-session] ${cause.message}; ${pageNote}; sessions are remote, no stack dump taken`);
  }
  return new Error(
    `session is not serving requests (${cause.message}). The page answers but the ` +
    'session main thread does not: R is most likely running code dispatched from the ' +
    `idle event loop (#19236) -- ${evidence}. Failing fast so the worker restarts ` +
    'with a fresh session.',
  );
}

import { test } from '@playwright/test';

/**
 * Paper-trail logging for automation bridge actions.
 *
 * Emits one log line per bridge call, capturing the high-level action label,
 * its arguments, success or failure, and elapsed time. The timestamp is
 * captured at call start (when the action was initiated, not when it
 * returned) so concurrent flows interleave in initiation order.
 *
 * Format:
 *
 *     [<iso8601-timestamp>] [<test-title-or-fallback>] <action> <details> <status> <ms>ms
 *
 * Example:
 *
 *     [2026-05-24T08:50:42.123Z] [shinytest2 toolbar] setPref rainbow_fenced_divs=true ok 23ms
 *     [2026-05-24T08:50:42.146Z] [shinytest2 toolbar] openProject /tmp/sandbox/foo.Rproj ok 1842ms
 *     [2026-05-24T08:50:43.988Z] [shinytest2 toolbar] documentOpen /tmp/sandbox/test.R FAILED 20012ms
 *
 * Only used by the helpers in `commands.ts`. Tests should not call these
 * functions directly; calling a bridge helper produces the log entry.
 */

function activeTestTitle(): string {
  try {
    return test.info().title;
  } catch {
    // Outside the test-runner context (e.g. a helper called from a worker
    // setup hook before any test has started).
    return '[no-test]';
  }
}

function emit(
  startedAt: string,
  action: string,
  details: string,
  status: 'ok' | 'FAILED',
  durationMs: number,
): void {
  const detailPart = details ? ` ${details}` : '';
  // eslint-disable-next-line no-console
  console.log(`[${startedAt}] [${activeTestTitle()}] ${action}${detailPart} ${status} ${durationMs}ms`);
}

/**
 * Run `fn` and emit one paper-trail log line. The line includes the action
 * label, the details string, ok/FAILED status, and elapsed milliseconds.
 *
 * Captures the timestamp at call start. On thrown error the entry is logged
 * before the error is re-thrown so a trace-less failure still produces "the
 * last thing that was tried" in the log.
 */
export async function withBridgeLog<T>(
  action: string,
  details: string,
  fn: () => Promise<T>,
): Promise<T> {
  const startedAt = new Date().toISOString();
  const startMs = Date.now();
  try {
    const result = await fn();
    emit(startedAt, action, details, 'ok', Date.now() - startMs);
    return result;
  } catch (err) {
    emit(startedAt, action, details, 'FAILED', Date.now() - startMs);
    throw err;
  }
}

/**
 * Variant of `withBridgeLog` for read calls that resolve to a value the
 * paper trail should record. The detail string for the log entry is built
 * from the resolved value via `formatResult`, so the log can include the
 * value that was observed (useful for `getPref` etc.).
 */
export async function withBridgeLogResult<T>(
  action: string,
  formatResult: (result: T) => string,
  fn: () => Promise<T>,
): Promise<T> {
  const startedAt = new Date().toISOString();
  const startMs = Date.now();
  try {
    const result = await fn();
    emit(startedAt, action, formatResult(result), 'ok', Date.now() - startMs);
    return result;
  } catch (err) {
    emit(startedAt, action, '<unresolved>', 'FAILED', Date.now() - startMs);
    throw err;
  }
}

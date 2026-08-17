/**
 * Bound an operation that has no timeout of its own.
 *
 * The main customer is `page.evaluate`: unlike locator actions (bounded by
 * the config's actionTimeout) and waitForFunction (explicit timeouts), an
 * evaluate waits for the frame's JS execution context with no limit. A page
 * parked on a navigation that never commits -- the #18394 session-transition
 * wedge -- therefore hangs every bridge call forever, silently stacking
 * fixture setup/teardown stages toward the CI heartbeat kill. Racing such
 * calls against a deadline turns the wedge into a fast, descriptive failure.
 */

export class DeadlineError extends Error {
  constructor(label: string, ms: number) {
    super(`${label} did not complete within ${ms}ms`);
    this.name = 'DeadlineError';
  }
}

/**
 * Resolve with `promise`, or reject with a DeadlineError after `ms`.
 *
 * On deadline the underlying operation is abandoned, not cancelled -- it may
 * still settle later (e.g. an evaluate rejecting once the browser closes), so
 * its eventual rejection is silenced to keep it from surfacing as an
 * unhandled rejection in the worker process.
 */
export async function withDeadline<T>(promise: Promise<T>, ms: number, label: string): Promise<T> {
  let timer: NodeJS.Timeout | undefined;

  const deadline = new Promise<never>((_, reject) => {
    timer = setTimeout(() => reject(new DeadlineError(label, ms)), ms);
  });

  try {
    return await Promise.race([promise, deadline]);
  } catch (err) {
    if (err instanceof DeadlineError)
      promise.catch(() => {});
    throw err;
  } finally {
    clearTimeout(timer);
  }
}

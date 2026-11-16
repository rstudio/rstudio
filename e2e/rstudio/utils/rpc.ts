import { errors, expect, type Page, type Request, type Response } from '@playwright/test';
import { TIMEOUTS } from '@utils/constants';
import { DeadlineError, withDeadline } from '@utils/deadline';

/** A JSON-RPC response body as the session sends it. */
export interface RpcResponseBody {
  result?: unknown;
  error?: unknown;
}

/** The deferred response to the RPC the session acknowledged with `handle`. */
export interface AsyncCompletion {
  handle: string;
  response: unknown;
}

const GET_EVENTS = /\/events\/get_events(?:\?|$)/;

interface ClientEvent {
  type?: unknown;
  data?: { handle?: unknown; response?: unknown };
}

/**
 * The async_completion events carried by `response`: none unless it is a
 * get_events long-poll whose body can be read. Long-polls are routinely cut
 * off, so an unreadable body is not an error.
 */
export async function asyncCompletions(response: Response): Promise<AsyncCompletion[]> {
  if (!GET_EVENTS.test(response.url()))
    return [];
  const body = (await response.json().catch(() => null)) as { result?: unknown } | null;
  if (!Array.isArray(body?.result))
    return [];
  const completions: AsyncCompletion[] = [];
  for (const event of body.result as ClientEvent[]) {
    const handle = event?.data?.handle;
    if (event?.type === 'async_completion' && typeof handle === 'string')
      completions.push({ handle, response: event.data?.response });
  }
  return completions;
}

/**
 * The body of the first `method` response (that `request` accepts) to arrive,
 * once the main thread has handled it: a busy session answers with only an
 * asyncHandle and sends the body later. Call before sending the RPC. Rejects on
 * an RPC error, or when `timeout` passes.
 */
export function waitForRpcResponse(
  page: Page,
  method: string,
  options: { timeout?: number; request?: (request: Request) => boolean } = {},
): Promise<RpcResponseBody> {
  const timeout = options.timeout ?? TIMEOUTS.consoleReady;
  const label = `response of ${method}`;
  const rpc = new RegExp(`/rpc/${method}(?:\\?|$)`);
  const completions = new Map<string, RpcResponseBody>();
  let onCompletion: (() => void) | undefined;
  const onResponse = async (response: Response): Promise<void> => {
    const received = await asyncCompletions(response);
    for (const { handle, response: body } of received)
      completions.set(handle, body as RpcResponseBody);
    if (received.length > 0)
      onCompletion?.();
  };
  page.on('response', onResponse);

  const settle = async (): Promise<RpcResponseBody> => {
    const matches = (response: Response): boolean =>
      rpc.test(response.url()) && (options.request?.(response.request()) ?? true);
    // Playwright's own timeout fires with the deadline; report it the same way.
    const ack = await page.waitForResponse(matches, { timeout }).catch((err: unknown) => {
      throw err instanceof errors.TimeoutError ? new DeadlineError(label, timeout) : err;
    });
    const body = (await ack.json().catch((err: unknown) => {
      throw new Error(
        `${method} answered HTTP ${ack.status()} with a body that is not JSON: ${err}`);
    })) as RpcResponseBody & { asyncHandle?: unknown };
    const handle = body.asyncHandle;
    if (typeof handle !== 'string')
      return body;
    return new Promise<RpcResponseBody>((resolve) => {
      onCompletion = () => {
        const response = completions.get(handle);
        if (response !== undefined)
          resolve(response);
      };
      onCompletion();
    });
  };
  return withDeadline(settle(), timeout, label)
    .then((body) => {
      if (body.error !== undefined)
        throw new Error(`${method} failed: ${JSON.stringify(body.error)}`);
      return body;
    })
    .finally(() => page.off('response', onResponse));
}

const RPC_METHOD = /\/rpc\/([A-Za-z0-9_]+)(?:\?|$)/;

/** Tracks the RPCs the page sends from now on; see `trackRpcs`. */
export interface RpcTracker {
  /** Resolves once every tracked RPC has been answered, async completions included. */
  idle(timeout: number): Promise<void>;
  stop(): void;
}

/**
 * Track every RPC the page sends from now on until each is answered: by its
 * response or, for one acknowledged with an asyncHandle, by its
 * async_completion event. Error answers count as answered.
 */
export function trackRpcs(page: Page): RpcTracker {
  const pending = new Map<Request, string>();
  const awaitingCompletion = new Map<string, string>();
  const completed = new Set<string>();

  const onRequest = (request: Request): void => {
    const method = RPC_METHOD.exec(request.url())?.[1];
    if (method)
      pending.set(request, method);
  };
  const onDone = (request: Request): void => {
    pending.delete(request);
  };
  const onResponse = async (response: Response): Promise<void> => {
    for (const { handle } of await asyncCompletions(response)) {
      if (!awaitingCompletion.delete(handle))
        completed.add(handle);
    }
    const method = pending.get(response.request());
    if (method === undefined)
      return;
    const body = (await response.json().catch(() => null)) as { asyncHandle?: unknown } | null;
    const handle = body?.asyncHandle;
    if (typeof handle === 'string' && !completed.delete(handle))
      awaitingCompletion.set(handle, method);
    pending.delete(response.request());
  };
  page.on('request', onRequest);
  page.on('response', onResponse);
  page.on('requestfailed', onDone);

  return {
    async idle(timeout: number): Promise<void> {
      const unanswered = () => [...pending.values(), ...awaitingCompletion.values()];
      try {
        await expect.poll(() => unanswered().length, { timeout }).toBe(0);
      } catch {
        throw new Error(`RPCs still unanswered after ${timeout}ms: ${unanswered().join(', ')}`);
      }
    },
    stop(): void {
      page.off('request', onRequest);
      page.off('response', onResponse);
      page.off('requestfailed', onDone);
    },
  };
}

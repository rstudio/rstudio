import { errors, type Page, type Response } from '@playwright/test';
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
 * The response body of the next `method` RPC the page sends, once the
 * session's main thread has handled it. Call it before sending the RPC: while
 * that thread is busy, the session acknowledges the RPC with only an
 * asyncHandle and delivers the body later in an async_completion event, which
 * can arrive before the acknowledgement is read. `timeout` bounds the whole
 * wait, reading the acknowledgement's body included. A response carrying an
 * error rejects: a failed RPC still answers HTTP 200.
 */
export function waitForRpcResponse(
  page: Page,
  method: string,
  options: { timeout?: number } = {},
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
    // Playwright's own timeout fires with the deadline; report it the same way.
    const ack = await page.waitForResponse(rpc, { timeout }).catch((err: unknown) => {
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

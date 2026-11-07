import type { Page, Response } from '@playwright/test';
import { TIMEOUTS } from '@utils/constants';
import { withDeadline } from '@utils/deadline';

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
 * can arrive before the acknowledgement is read. `timeout` covers both.
 */
export function waitForRpcResponse(
  page: Page,
  method: string,
  options: { timeout?: number } = {},
): Promise<RpcResponseBody> {
  const timeout = options.timeout ?? TIMEOUTS.consoleReady;
  const deadline = Date.now() + timeout;
  const rpc = new RegExp(`/rpc/${method}(?:\\?|$)`);
  const completions = new Map<string, unknown>();
  let onCompletion: (() => void) | undefined;
  const onResponse = async (response: Response): Promise<void> => {
    const received = await asyncCompletions(response);
    for (const { handle, response: body } of received)
      completions.set(handle, body);
    if (received.length > 0)
      onCompletion?.();
  };
  page.on('response', onResponse);

  const settle = async (): Promise<RpcResponseBody> => {
    const ack = await page.waitForResponse(rpc, { timeout });
    const body = (await ack.json().catch((err: unknown) => {
      throw new Error(`${method} answered HTTP ${ack.status()} with a body that is not JSON: ${err}`);
    })) as RpcResponseBody & { asyncHandle?: unknown };
    const handle = body.asyncHandle;
    if (typeof handle !== 'string')
      return body;
    const completion = new Promise<RpcResponseBody>((resolve, reject) => {
      onCompletion = () => {
        if (!completions.has(handle))
          return;
        const response = completions.get(handle) as RpcResponseBody | undefined;
        if (response === undefined)
          reject(new Error(`async_completion of ${method} (handle ${handle}) carried no response`));
        else
          resolve(response);
      };
      onCompletion();
    });
    return withDeadline(completion, Math.max(0, deadline - Date.now()),
      `async completion of ${method} (handle ${handle})`);
  };
  return settle().finally(() => page.off('response', onResponse));
}

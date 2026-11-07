import { test, expect, type Page, type Route } from '@playwright/test';
import { waitForRpcResponse } from '@utils/rpc';

/**
 * Harness self-test for utils/rpc.ts. waitForRpcResponse() stands for "the
 * session has handled this RPC", so it must not settle on a bare asyncHandle
 * acknowledgement; the page's RPC and event traffic is replayed here against
 * routed fakes.
 */

const ORIGIN = 'http://rstudio.test';

// What each RPC answers, by method; a method without an entry is held until
// the test fulfills it through `held`.
let answers: Record<string, unknown>;
let held: Map<string, Route>;
// Each get_events poll answers with the next batch of events queued here.
let eventBatches: unknown[][];

test.beforeEach(async ({ page }) => {
  answers = {};
  held = new Map();
  eventBatches = [];
  await page.route(`${ORIGIN}/`, (route) =>
    route.fulfill({ contentType: 'text/html', body: '<div></div>' }));
  await page.route(`${ORIGIN}/rpc/*`, (route) => {
    const method = new URL(route.request().url()).pathname.split('/').pop() ?? '';
    if (method in answers)
      return route.fulfill({ json: answers[method] });
    held.set(method, route);
    return Promise.resolve();
  });
  await page.route(`${ORIGIN}/events/get_events`, (route) =>
    route.fulfill({ json: { result: eventBatches.shift() ?? [] } }),
  );
  await page.goto(`${ORIGIN}/`);
});

const completion = (handle: string, response: unknown) => ({
  type: 'async_completion',
  data: { handle, ...(response === undefined ? {} : { response }) },
});

// Send without waiting for the answer, as the IDE does.
const send = (page: Page, path: string) =>
  page.evaluate((url) => {
    void fetch(url, { method: 'POST' }).catch(() => {});
  }, path);

const pollEvents = (page: Page, ...events: unknown[]) => {
  eventBatches.push(events);
  return page.evaluate(async () => {
    await fetch('/events/get_events', { method: 'POST' });
  });
};

test('returns an inline answer from the named RPC only', async ({ page }) => {
  answers = {
    get_recent_history_extra: { result: 'other' },
    get_recent_history: { result: 'mine' },
  };
  const response = waitForRpcResponse(page, 'get_recent_history');
  await send(page, '/rpc/get_recent_history_extra');
  await send(page, '/rpc/get_recent_history');
  expect(await response).toEqual({ result: 'mine' });
});

test('returns the body an async completion delivers after the ack', async ({ page }) => {
  answers = { console_input: { asyncHandle: 'h1', ep: 'false' } };
  const response = waitForRpcResponse(page, 'console_input');
  await send(page, '/rpc/console_input');
  await pollEvents(page, completion('other', { result: 'not mine' }));
  const busy = { type: 'busy', data: { value: true } };
  await pollEvents(page, busy, completion('h1', { result: 'served' }));
  expect(await response).toEqual({ result: 'served' });
});

test('returns the body of a completion that arrived before the ack', async ({ page }) => {
  const response = waitForRpcResponse(page, 'console_input');
  await send(page, '/rpc/console_input');
  await expect.poll(() => held.has('console_input')).toBe(true);
  await pollEvents(page, completion('h2', { result: 'early' }));
  await held.get('console_input')!.fulfill({ json: { asyncHandle: 'h2', ep: 'false' } });
  expect(await response).toEqual({ result: 'early' });
});

test('rejects a completion that carries no response', async ({ page }) => {
  answers = { close_document: { asyncHandle: 'h3', ep: 'false' } };
  const rejected = expect(waitForRpcResponse(page, 'close_document')).rejects.toThrow(
    'async_completion of close_document (handle h3) carried no response');
  await send(page, '/rpc/close_document');
  await pollEvents(page, completion('h3', undefined));
  await rejected;
});

test('rejects when the completion does not arrive in time', async ({ page }) => {
  answers = { set_user_prefs: { asyncHandle: 'h4', ep: 'false' } };
  const rejected = expect(waitForRpcResponse(page, 'set_user_prefs', { timeout: 500 }))
    .rejects.toThrow('response of set_user_prefs did not complete within 500ms');
  await send(page, '/rpc/set_user_prefs');
  await rejected;
});

test('rejects an acknowledgement that is not JSON', async ({ page }) => {
  const rejected = expect(waitForRpcResponse(page, 'ping')).rejects.toThrow(
    'ping answered HTTP 401 with a body that is not JSON');
  await send(page, '/rpc/ping');
  await expect.poll(() => held.has('ping')).toBe(true);
  await held.get('ping')!.fulfill({ status: 401, body: '<html>sign in</html>' });
  await rejected;
});

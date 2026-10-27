import { test, expect, type Page } from '@playwright/test';
import {
  AssistantRequestWatcher,
  SLOW_ASSISTANT_ANSWER_MS,
  slowAssistantReason,
  type AssistantRequestOutcome,
} from '@utils/assistant-requests';

/**
 * Harness self-test for utils/assistant-requests.ts. slowAssistantReason()
 * decides whether a suggestion wait that came up empty is skipped as a slow
 * service instead of failing, so it must not excuse a session that dropped
 * requests on its own; the watcher feeds it from the page's RPC and event
 * traffic, replayed here against routed fakes.
 */

const answered = (elapsedMs: number): AssistantRequestOutcome => ({ cancelled: false, elapsedMs });
const cancelled = (elapsedMs: number): AssistantRequestOutcome => ({ cancelled: true, elapsedMs });

test.describe('slowAssistantReason', () => {
  test('has no reason without both a cancelled and an answered request', () => {
    expect(slowAssistantReason([])).toBeNull();
    // A session that stopped relaying requests cancels every one of them.
    expect(slowAssistantReason([cancelled(10500), cancelled(10800)])).toBeNull();
    expect(slowAssistantReason([answered(9000), answered(9500)])).toBeNull();
  });

  test('has no reason when any answer came back fast', () => {
    expect(slowAssistantReason([answered(1200), answered(10300), cancelled(10500)])).toBeNull();
    expect(slowAssistantReason([answered(SLOW_ASSISTANT_ANSWER_MS - 1), cancelled(10500)])).toBeNull();
  });

  test('names the slow service when every answer was slow and a request was cancelled', () => {
    // Run 40398345386: two next-edit answers after 10.6s and 10.3s, and the
    // re-request cancelled at the session's cutoff.
    const reason = slowAssistantReason([answered(10600), answered(10300), cancelled(10500)]);
    expect(reason).toContain('answered 2 request(s), the fastest after 10.3s');
    expect(reason).toContain('left 1 unanswered');

    expect(slowAssistantReason([answered(SLOW_ASSISTANT_ANSWER_MS), cancelled(10500)])).not.toBeNull();
  });
});

test.describe('AssistantRequestWatcher', () => {
  const ORIGIN = 'http://rstudio.test';

  // Each RPC answers with a handle named after it, as an async RPC does.
  // Each get_events poll answers with the next batch of events queued here.
  let eventBatches: unknown[][];

  test.beforeEach(async ({ page }) => {
    eventBatches = [];
    await page.route(`${ORIGIN}/`, (route) => route.fulfill({ contentType: 'text/html', body: '<div></div>' }));
    await page.route(`${ORIGIN}/rpc/*`, (route) => {
      const method = new URL(route.request().url()).pathname.split('/').pop();
      return route.fulfill({ json: { asyncHandle: `handle-${method}`, ep: 'false' } });
    });
    await page.route(`${ORIGIN}/events/get_events`, (route) =>
      route.fulfill({ json: { result: eventBatches.shift() ?? [] } }),
    );
    await page.goto(`${ORIGIN}/`);
  });

  const completion = (handle: string, result: unknown) => ({
    type: 'async_completion',
    data: { handle, response: { result, ep: 'false' } },
  });

  const post = (page: Page, path: string) =>
    page.evaluate(async (url) => {
      await fetch(url, { method: 'POST' });
    }, path);

  test('settles each assistant RPC with its async completion', async ({ page }) => {
    const watcher = new AssistantRequestWatcher(page);
    await post(page, '/rpc/assistant_next_edit_suggestions');
    await post(page, '/rpc/assistant_generate_completions');
    eventBatches.push([
      completion('handle-assistant_next_edit_suggestions', { cancelled: false }),
      completion('handle-assistant_generate_completions', { cancelled: true }),
    ]);
    await post(page, '/events/get_events');

    await expect.poll(() => watcher.outcomes().length).toBe(2);
    const outcomes = watcher.outcomes();
    expect(outcomes.map((o) => o.cancelled).sort()).toEqual([false, true]);
    for (const outcome of outcomes) {
      expect(outcome.elapsedMs).toBeGreaterThanOrEqual(0);
    }
    watcher.stop();
  });

  test('ignores other RPCs, other events, and completions without a cancelled flag', async ({ page }) => {
    const watcher = new AssistantRequestWatcher(page);
    await post(page, '/rpc/chat_doc_focused');
    await post(page, '/rpc/assistant_status');
    await post(page, '/rpc/assistant_did_show_completion');
    await post(page, '/rpc/assistant_next_edit_suggestions');
    eventBatches.push([
      completion('handle-chat_doc_focused', { cancelled: true }),
      completion('handle-assistant_status', null),
      { type: 'console_output', data: { handle: 'handle-assistant_did_show_completion' } },
      completion('handle-assistant_next_edit_suggestions', { cancelled: true }),
    ]);
    await post(page, '/events/get_events');

    // The one relayed request settling proves the batch has been read.
    await expect.poll(() => watcher.outcomes().length).toBe(1);
    expect(watcher.outcomes()[0].cancelled).toBe(true);
    watcher.stop();
  });

  test('records nothing once stopped', async ({ page }) => {
    const stopped = new AssistantRequestWatcher(page);
    stopped.stop();
    const running = new AssistantRequestWatcher(page);
    await post(page, '/rpc/assistant_generate_completions');
    eventBatches.push([completion('handle-assistant_generate_completions', { cancelled: false })]);
    await post(page, '/events/get_events');

    await expect.poll(() => running.outcomes().length).toBe(1);
    expect(stopped.outcomes()).toEqual([]);
    running.stop();
  });
});

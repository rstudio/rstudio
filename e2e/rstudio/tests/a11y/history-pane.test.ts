import { test, expect } from '@fixtures/a11y.fixture';
import type { Page, Response } from '@playwright/test';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withEnvironmentTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { withDeadline } from '@utils/deadline';

const HISTORY_PANEL = '#rstudio_workbench_panel_history';
const HISTORY_TAB = '#rstudio_workbench_tab_history';
// The History presenter is delay-loaded: the first activation creates it,
// and it fetches the history list then.
const RECENT_HISTORY_RPC = /\/rpc\/get_recent_history(?:\?|$)/;
const GET_EVENTS = /\/events\/get_events(?:\?|$)/;

interface RpcBody {
  result?: { command?: unknown };
  asyncHandle?: unknown;
}

interface AsyncCompletionEvent {
  type?: string;
  data?: { handle?: unknown; response?: RpcBody };
}

/**
 * Run `trigger`, which sends the RPC matched by `rpc`, and return the RPC's
 * response body. The POST carries it only if the session's main thread serves
 * the RPC within asyncRpcTimeoutMs (200ms by default); otherwise the POST
 * answers with an asyncHandle and the body arrives later in an
 * async_completion event on the get_events long-poll. The events listener
 * goes in before `trigger`, as the completion can land before the
 * acknowledgement is read.
 */
async function rpcResponseBody(page: Page, rpc: RegExp, trigger: () => Promise<unknown>): Promise<RpcBody> {
  const completions = new Map<string, RpcBody>();
  let onCompletion: (() => void) | undefined;
  const onResponse = async (response: Response): Promise<void> => {
    if (!GET_EVENTS.test(response.url()))
      return;
    const body = (await response.json().catch(() => null)) as { result?: unknown } | null;
    if (!Array.isArray(body?.result))
      return;
    for (const event of body.result as AsyncCompletionEvent[]) {
      const handle = event?.data?.handle;
      if (event?.type === 'async_completion' && typeof handle === 'string')
        completions.set(handle, event.data?.response ?? {});
    }
    onCompletion?.();
  };
  page.on('response', onResponse);
  try {
    const [ack] = await Promise.all([
      page.waitForResponse(rpc, { timeout: TIMEOUTS.consoleReady }),
      trigger(),
    ]);
    const body = (await ack.json()) as RpcBody;
    const handle = body.asyncHandle;
    if (typeof handle !== 'string')
      return body;
    return await withDeadline(
      new Promise<RpcBody>((resolve) => {
        onCompletion = () => {
          const completed = completions.get(handle);
          if (completed !== undefined)
            resolve(completed);
        };
        onCompletion();
      }),
      TIMEOUTS.consoleReady,
      `async completion of ${rpc.source} (handle ${handle})`,
    );
  } finally {
    page.off('response', onResponse);
  }
}

/**
 * The initial state with the History pane shown in place of Environment
 * (Ctrl+4). Console history outlives the session, so the list holds whatever
 * earlier sessions ran, including the fixture's own version read, and is
 * empty only when none has run.
 */
test('history pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    let history: RpcBody | undefined;
    const results = await withEnvironmentTabsetTab(page, HISTORY_TAB,
      async () => {
        history = await rpcResponseBody(page, RECENT_HISTORY_RPC,
          () => executeCommand(page, 'activateHistory'));
      },
      async () => {
        const commands = history?.result?.command;
        expect(Array.isArray(commands), `get_recent_history result: ${JSON.stringify(history)}`).toBe(true);
        // Matching the count also covers an empty history, whose list has no
        // height and so is never visible.
        await expect(page.locator(HISTORY_PANEL).getByRole('listbox', { name: 'Console History', exact: true })
          .getByRole('option')).toHaveCount((commands as unknown[]).length, { timeout: TIMEOUTS.consoleReady });
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

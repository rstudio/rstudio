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
 * The response body of the next RPC matched by `rpc`. Call before sending it:
 * a busy session answers with just an asyncHandle and delivers the body later
 * in an async_completion event, which can land before the handle is read.
 */
function watchRpcResponse(page: Page, rpc: RegExp): Promise<RpcBody> {
  const completions = new Map<string, AsyncCompletionEvent['data']>();
  let onCompletion: (() => void) | undefined;
  const onResponse = async (response: Response): Promise<void> => {
    if (!GET_EVENTS.test(response.url()))
      return;
    // Long-polls are routinely cut off; a body that can't be read is skipped.
    const body = (await response.json().catch(() => null)) as { result?: unknown } | null;
    if (!Array.isArray(body?.result))
      return;
    for (const event of body.result as AsyncCompletionEvent[]) {
      const handle = event?.data?.handle;
      if (event?.type === 'async_completion' && typeof handle === 'string')
        completions.set(handle, event.data);
    }
    onCompletion?.();
  };
  page.on('response', onResponse);

  const settle = async (): Promise<RpcBody> => {
    const ack = await page.waitForResponse(rpc, { timeout: TIMEOUTS.consoleReady });
    const body = (await ack.json()) as RpcBody;
    const handle = body.asyncHandle;
    if (typeof handle !== 'string')
      return body;
    const completion = new Promise<RpcBody>((resolve, reject) => {
      onCompletion = () => {
        if (!completions.has(handle))
          return;
        const data = completions.get(handle);
        if (data?.response === undefined)
          reject(new Error(`async_completion without a response: ${JSON.stringify(data)}`));
        else
          resolve(data.response);
      };
      onCompletion();
    });
    return withDeadline(completion, TIMEOUTS.consoleReady,
      `async completion of ${rpc.source} (handle ${handle})`);
  };
  return settle().finally(() => page.off('response', onResponse));
}

/**
 * The initial state with the History pane shown in place of Environment
 * (Ctrl+4). Console history outlives the session, so the list holds whatever
 * earlier sessions ran, including the fixture's own version read, and is
 * empty only when none has run.
 */
test('history pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    let history: Promise<RpcBody> | undefined;
    const results = await withEnvironmentTabsetTab(page, HISTORY_TAB,
      async () => {
        history = watchRpcResponse(page, RECENT_HISTORY_RPC);
        // Awaited below; this keeps a failure before then from also
        // surfacing as an unhandled rejection.
        history.catch(() => {});
        await executeCommand(page, 'activateHistory');
      },
      async () => {
        const body = await history!;
        const commands = body.result?.command;
        if (!Array.isArray(commands))
          throw new Error(`get_recent_history result: ${JSON.stringify(body)}`);
        // Matching the count also covers an empty history, whose list has no
        // height and so is never visible.
        const historyList = page.locator(HISTORY_PANEL)
          .getByRole('listbox', { name: 'Console History', exact: true });
        await expect(historyList.getByRole('option'))
          .toHaveCount(commands.length, { timeout: TIMEOUTS.consoleReady });
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

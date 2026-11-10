import { test, expect } from '@fixtures/a11y.fixture';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withEnvironmentTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { waitForRpcResponse, type RpcResponseBody } from '@utils/rpc';

const HISTORY_PANEL = '#rstudio_workbench_panel_history';
const HISTORY_TAB = '#rstudio_workbench_tab_history';

/**
 * The initial state with the History pane shown in place of Environment
 * (Ctrl+4). Console history outlives the session, so the list holds whatever
 * earlier sessions ran, including the fixture's own version read, and is
 * empty only when none has run.
 */
test('history pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    let history: Promise<RpcResponseBody> | undefined;
    const results = await withEnvironmentTabsetTab(page, HISTORY_TAB,
      async () => {
        // The History presenter is delay-loaded: the first activation creates
        // it, and it fetches the history list then.
        history = waitForRpcResponse(page, 'get_recent_history');
        // Awaited below; this keeps a failure before then from also
        // surfacing as an unhandled rejection.
        history.catch(() => {});
        await executeCommand(page, 'activateHistory');
      },
      async () => {
        const body = await history!;
        const commands = (body.result as { command?: unknown } | undefined)?.command;
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

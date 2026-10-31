import { test, expect } from '@fixtures/a11y.fixture';
import type { Response } from '@playwright/test';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withEnvironmentTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const HISTORY_PANEL = '#rstudio_workbench_panel_history';
const HISTORY_TAB = '#rstudio_workbench_tab_history';
// The History presenter is delay-loaded: the first activation creates it,
// and it fetches the history list then.
const RECENT_HISTORY_RPC = /\/rpc\/get_recent_history(?:\?|$)/;

/**
 * The initial state with the History pane shown in place of Environment
 * (Ctrl+4). Console history outlives the session, so the list holds whatever
 * earlier sessions ran, including the fixture's own version read, and is
 * empty only when none has run.
 */
test('history pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    let fetched: Promise<Response> | undefined;
    const results = await withEnvironmentTabsetTab(page, HISTORY_TAB,
      async () => {
        fetched = page.waitForResponse(RECENT_HISTORY_RPC, { timeout: TIMEOUTS.consoleReady });
        // Awaited below; this keeps a failure before then from also
        // surfacing as an unhandled rejection.
        fetched.catch(() => {});
        await executeCommand(page, 'activateHistory');
      },
      async () => {
        const body = await (await fetched!).json();
        const commands = body?.result?.command;
        expect(Array.isArray(commands), `get_recent_history result: ${JSON.stringify(body)}`).toBe(true);
        // Matching the count also covers an empty history, whose list has no
        // height and so is never visible.
        await expect(page.locator(HISTORY_PANEL).getByRole('listbox', { name: 'Console History', exact: true })
          .getByRole('option')).toHaveCount(commands.length, { timeout: TIMEOUTS.consoleReady });
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

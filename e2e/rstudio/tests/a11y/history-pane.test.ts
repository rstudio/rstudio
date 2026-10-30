import { test, expect } from '@fixtures/a11y.fixture';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withEnvironmentTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const HISTORY_PANEL = '#rstudio_workbench_panel_history';
const HISTORY_TAB = '#rstudio_workbench_tab_history';
const SEARCH_INPUT = '#rstudio_sw_history input';

/**
 * The initial state with the History pane shown in place of Environment
 * (Ctrl+4). Console history outlives the session, so the list holds whatever
 * earlier sessions ran, including the fixture's own version read, and is
 * empty only when none has run.
 */
test('history pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const results = await withEnvironmentTabsetTab(page, HISTORY_TAB,
      () => executeCommand(page, 'activateHistory'),
      async () => {
        // An empty list has no height, so it is attached but never visible.
        await expect(page.locator(SEARCH_INPUT)).toBeVisible({ timeout: TIMEOUTS.consoleReady });
        await expect(page.locator(HISTORY_PANEL).getByRole('listbox', { name: 'Console History', exact: true }))
          .toBeAttached();
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

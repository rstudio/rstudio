import { test, expect } from '@fixtures/a11y.fixture';
import { ConnectionsPane, CONNECTIONS_TAB } from '@pages/connections_pane.page';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withEnvironmentTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

/**
 * The initial state with the Connections pane shown in place of Environment,
 * on its connection list. No connection is made, so the list holds only
 * connections earlier sessions left in the user's connection history.
 */
test('connections pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const pane = new ConnectionsPane(page);
    const results = await withEnvironmentTabsetTab(page, CONNECTIONS_TAB,
      () => executeCommand(page, 'activateConnections'),
      async () => {
        await expect(pane.newConnectionBtn).toBeVisible({ timeout: TIMEOUTS.consoleReady });
        await expect(pane.filterConnections).toBeVisible({ timeout: TIMEOUTS.consoleReady });
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

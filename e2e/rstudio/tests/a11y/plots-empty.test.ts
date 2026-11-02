import { test, expect } from '@fixtures/a11y.fixture';
import { PlotsPane, PLOTS_TAB } from '@pages/plots_pane.page';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withFilesTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

/**
 * The initial state with the Plots pane shown in place of Files (Ctrl+6),
 * before anything has been plotted.
 */
test('empty plots pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const pane = new PlotsPane(page);
    const results = await withFilesTabsetTab(page, PLOTS_TAB,
      () => executeCommand(page, 'activatePlots'),
      async () => {
        await expect(pane.plotImage).toBeVisible({ timeout: TIMEOUTS.consoleReady });
        // Clear All is enabled whenever the plot history holds a plot.
        await expect(pane.clearPlotsBtn).toBeDisabled();
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

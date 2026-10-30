import { test, expect } from '@fixtures/a11y.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { PLOT_IMAGE_FRAME, PLOTS_TAB } from '@pages/plots_pane.page';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withFilesTabsetTab } from '@utils/a11y-tabs';
import { TIMEOUTS } from '@utils/constants';

/**
 * The initial state after `plot(1)`, which shows the Plots pane in place of
 * Files. The plot history is session state, so it ends with the session the
 * fixture quits.
 */
test('plots pane showing one plot has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const results = await withFilesTabsetTab(page, PLOTS_TAB,
      () => executeInConsole(page, 'plot(1)'),
      async () => {
        // The alt text names the plot's place in the history, so it also
        // shows no plot was left over from an earlier session.
        const img = page.frameLocator(PLOT_IMAGE_FRAME).locator('#img');
        await expect(img).toHaveAttribute('alt', 'Plot 1 of 1', { timeout: TIMEOUTS.fileOpen });
        await expect.poll(() => img.evaluate((el: HTMLImageElement) => el.complete && el.naturalWidth > 0),
          { timeout: TIMEOUTS.fileOpen }).toBe(true);
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

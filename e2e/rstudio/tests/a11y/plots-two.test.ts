import { test, expect } from '@fixtures/a11y.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { PLOT_IMAGE_FRAME, PLOTS_TAB } from '@pages/plots_pane.page';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withFilesTabsetTab } from '@utils/a11y-tabs';
import { TIMEOUTS } from '@utils/constants';

/**
 * The initial state after `plot(1)` and then `plot(2)`: the Plots pane, in
 * place of Files, on the second of two plots, so it can step back to the
 * first. The plot history ends with the session the fixture quits.
 */
test('plots pane showing a second plot has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const results = await withFilesTabsetTab(page, PLOTS_TAB,
      async () => {
        await executeInConsole(page, 'plot(1)');
        await executeInConsole(page, 'plot(2)');
      },
      async () => {
        const img = page.frameLocator(PLOT_IMAGE_FRAME).locator('#img');
        await expect(img).toHaveAttribute('alt', 'Plot 2 of 2', { timeout: TIMEOUTS.fileOpen });
        await expect.poll(() => img.evaluate((el: HTMLImageElement) => el.complete && el.naturalWidth > 0),
          { timeout: TIMEOUTS.fileOpen }).toBe(true);
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

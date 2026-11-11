import { test, expect } from '@fixtures/a11y.fixture';
import { SourcePane } from '@pages/source_pane.page';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withPlotsInsteadOfFiles } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

/**
 * The initial state plus a new, empty R script (File > New File > R Script),
 * with Plots shown in place of Files. No restore step for the script: the
 * fixture's teardown closes every document unsaved before quitting the
 * session.
 */
test('untitled R script has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const results = await withPlotsInsteadOfFiles(page, async () => {
      // Teardown may leave an untitled tab behind for the next session, so
      // "an untitled doc is active" alone could match before the new one exists.
      const previousId = await page.evaluate(() => window.rstudio?.documents.active()?.id ?? null);
      await executeCommand(page, 'newSourceDoc');
      // The document is created over an RPC and laid out afterwards; wait for
      // it to be active and its editor painted.
      await page.waitForFunction((prevId) => {
        const doc = window.rstudio?.documents.active() ?? null;
        return doc !== null && doc.id !== prevId && doc.path === null;
      }, previousId, { timeout: TIMEOUTS.fileOpen });
      await expect(new SourcePane(page).contentPane).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      return scanA11y(page, testInfo);
    });
    expect(violationSummary(results)).toEqual([]);
  });

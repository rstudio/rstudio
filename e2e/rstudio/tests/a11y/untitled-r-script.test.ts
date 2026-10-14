import { test, expect } from '@fixtures/a11y.fixture';
import { SourcePane } from '@pages/source_pane.page';
import { scanA11y, violationSummary } from '@utils/a11y';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

/**
 * The initial state plus a new, empty R script (File > New File > R Script).
 * No restore step: the fixture's teardown closes every document unsaved
 * before quitting the session.
 */
test('untitled R script has no WCAG 2.2 AA violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    await executeCommand(page, 'newSourceDoc');
    // The document is created over an RPC and laid out afterwards; wait for
    // it to be active and its editor painted.
    await page.waitForFunction(() => {
      const doc = window.rstudio?.documents.active() ?? null;
      return doc !== null && doc.path === null;
    }, null, { timeout: TIMEOUTS.fileOpen });
    await expect(new SourcePane(page).contentPane).toBeVisible({ timeout: TIMEOUTS.fileOpen });

    const results = await scanA11y(page, testInfo);
    expect(violationSummary(results)).toEqual([]);
  });

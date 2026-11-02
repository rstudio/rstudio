import { test, expect } from '@fixtures/a11y.fixture';
import { VIEWER_FRAME, VIEWER_TAB } from '@pages/viewer_pane.page';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withFilesTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

/**
 * The initial state with the Viewer pane shown in place of Files (Ctrl+9),
 * before anything has been viewed, so its frame is blank.
 */
test('empty viewer pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const results = await withFilesTabsetTab(page, VIEWER_TAB,
      () => executeCommand(page, 'activateViewer'),
      async () => {
        const frame = page.locator(VIEWER_FRAME);
        await expect(frame).toBeVisible({ timeout: TIMEOUTS.consoleReady });
        await expect(frame).toHaveAttribute('src', 'about:blank');
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

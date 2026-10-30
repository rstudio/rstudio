import { test, expect } from '@fixtures/a11y.fixture';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withFilesTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

// The Quarto presentation tab (PaneManager's "Presentations"), titled
// Presentation. The legacy R Presentation tab only exists while an .Rpres
// presentation is running.
const PRESENTATION_TAB = '#rstudio_workbench_tab_presentation';
const PRESENTATION_FRAME = "iframe[title='Presentation Preview']";

/**
 * The initial state with the Presentation pane shown in place of Files,
 * before any presentation has been previewed, so its frame is blank.
 */
test('empty presentation pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const results = await withFilesTabsetTab(page, PRESENTATION_TAB,
      () => executeCommand(page, 'activatePresentation2'),
      async () => {
        const frame = page.locator(PRESENTATION_FRAME);
        await expect(frame).toBeVisible({ timeout: TIMEOUTS.consoleReady });
        await expect(frame).toHaveAttribute('src', 'about:blank');
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

import { test, expect } from '@fixtures/a11y.fixture';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withFilesTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const HELP_TAB = '#rstudio_workbench_tab_help';
const HELP_FRAME = '#rstudio_help_frame';
const HOME_PATH = '/help/doc/home/';

/**
 * The initial state with the Help pane shown in place of Files, on its home
 * page (Help > R Help, the pane's house button). The page is help history,
 * which persists, but the session opens on home when that history is empty,
 * so it needs no restore.
 */
test('help home page has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    // helpHome navigates even when the pane is already on home (it reloads
    // then), so waiting for that navigation keeps the scan off a page that is
    // about to be replaced.
    const homeNavigated = page.waitForEvent('framenavigated', {
      predicate: (frame) => frame.parentFrame() === page.mainFrame() && frame.url().includes(HOME_PATH),
      timeout: TIMEOUTS.fileOpen,
    });
    homeNavigated.catch(() => {});
    const results = await withFilesTabsetTab(page, HELP_TAB,
      () => executeCommand(page, 'helpHome'),
      async () => {
        const frame = await homeNavigated;
        await frame.waitForLoadState('load', { timeout: TIMEOUTS.fileOpen });
        await expect(page.frameLocator(HELP_FRAME).getByRole('heading', { name: 'Manuals' }))
          .toBeVisible({ timeout: TIMEOUTS.fileOpen });
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

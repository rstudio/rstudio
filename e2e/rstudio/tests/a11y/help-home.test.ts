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
 * page (Help > R Help, the pane's house button). Showing home adds it to the
 * persisted help history; no other state shows Help, so that needs no restore.
 */
test('help home page has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const results = await withFilesTabsetTab(page, HELP_TAB,
      () => executeCommand(page, 'activateHelp'),
      async () => {
        // Help is built on first use and then shows a page from its history.
        // Let that load first, so the navigation waited on below is helpHome's
        // and the scan isn't run on a page about to be replaced.
        await page.waitForFunction((selector) => {
          const doc = document.querySelector<HTMLIFrameElement>(selector)?.contentDocument;
          return doc?.location.href.includes('/help/') === true && doc.readyState === 'complete';
        }, HELP_FRAME, { timeout: TIMEOUTS.fileOpen });
        // helpHome navigates even when the pane is already on home (it reloads).
        const homeNavigated = page.waitForEvent('framenavigated', {
          predicate: (frame) => frame.parentFrame() === page.mainFrame() && frame.url().includes(HOME_PATH),
          timeout: TIMEOUTS.fileOpen,
        });
        // Awaited below; this keeps a helpHome failure from also surfacing as
        // an unhandled rejection.
        homeNavigated.catch(() => {});
        await executeCommand(page, 'helpHome');
        const frame = await homeNavigated;
        await frame.waitForLoadState('load', { timeout: TIMEOUTS.fileOpen });
        await expect(page.frameLocator(HELP_FRAME).getByRole('heading', { name: 'Manuals' }))
          .toBeVisible({ timeout: TIMEOUTS.fileOpen });
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

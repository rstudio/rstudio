import { test, expect } from '@fixtures/a11y.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { switchToViewerFrame, VIEWER_TAB } from '@pages/viewer_pane.page';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withFilesTabsetTab } from '@utils/a11y-tabs';
import { TIMEOUTS } from '@utils/constants';

// A static page from the session tempdir, shown through the viewer option as
// a package would show one.
const VIEW_HTML_R = [
  'html <- file.path(tempdir(), "hello.html")',
  'writeLines("<h1>Hello, Viewer!</h1><p>Rendered in the Viewer pane.</p>", html)',
  'viewer <- getOption("viewer", utils::browseURL)',
  'viewer(html)',
].join('; ');

/**
 * The initial state with the Viewer pane showing a static HTML page in place
 * of Files. The page lives in the session tempdir and the Viewer's URL is
 * session state, so neither outlives the session the fixture quits.
 */
test('viewer pane showing HTML has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const results = await withFilesTabsetTab(page, VIEWER_TAB,
      () => executeInConsole(page, VIEW_HTML_R),
      async () => {
        await expect(switchToViewerFrame(page).getByRole('heading', { name: 'Hello, Viewer!' }))
          .toBeVisible({ timeout: TIMEOUTS.fileOpen });
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });

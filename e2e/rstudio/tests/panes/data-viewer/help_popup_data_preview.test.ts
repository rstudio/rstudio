// Data-frame preview in the autocompletion help popup -- #17735.
//
// Completing a data frame that is a list member (`object$df`) renders the
// shared grid viewer (grid_resource/gridviewer.html) as a preview in the
// completion help popup: the completion has type DATAFRAME, so HelpStrategy
// routes to getHelpDataFrame, which resolves `object$df` via .rs.getAnywhere
// and embeds the preview. (A bare top-level data-frame symbol does not surface
// this preview, so the test drives the list-member `$` path the feature
// actually uses.)
//
// That host should hide the column-summary sidebar: it is redundant with the
// help text and crowds out the rows the preview exists to surface.
// SessionHelp.R (getHelpDataFrame) now passes show_summary=0 for this host;
// the standalone Data Viewer still honors the data_viewer_show_summary pref.
//
// The grid viewer always renders #sidebarPanel; it carries the "expanded"
// class only while the summary is shown, and #sidebarToggle mirrors that in
// aria-expanded. Asserting the panel is collapsed is the behavioral check for
// the fix -- pre-fix this host defaulted show_summary to true and the sidebar
// was expanded.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { COMPLETION_POPUP } from '@actions/autocomplete.actions';
import { TIMEOUTS } from '@utils/constants';

// No data viewer is open in this test, so the gridviewer iframe matched here
// is unambiguously the help-popup preview.
const GRID_FRAME = 'iframe[src*="grid_resource/gridviewer.html"]';
const LIST_NAME = 'list_17735_preview';

test.describe('Help popup data preview - #17735', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    // executeInConsole replaces the input via setValue, so any partial
    // completion text left in the buffer can't corrupt this cleanup.
    await new ConsolePaneActions(page).executeInConsole(
      `if (exists("${LIST_NAME}", envir = .GlobalEnv)) rm(list = "${LIST_NAME}", envir = .GlobalEnv)`,
      { wait: true },
    );
  });

  test('the autocompletion help popup hides the column-summary sidebar', async ({ rstudioPage: page }) => {
    // A list whose `df` member is a data frame: completing `<list>$df` routes
    // to the DATAFRAME help path (getHelpDataFrame) that embeds the preview.
    await consoleActions.executeInConsole(`${LIST_NAME} <- list(df = head(mtcars))`, { wait: true });

    // Type up to the `$` and trigger member completion. The selected member's
    // help (the data preview) renders alongside the completion popup.
    await consoleActions.typeInConsole(`${LIST_NAME}$`);
    if (!(await page.locator(COMPLETION_POPUP).isVisible())) {
      await page.keyboard.press('Control+Space');
    }
    await expect(page.locator(COMPLETION_POPUP)).toBeVisible({ timeout: TIMEOUTS.fileOpen });

    // The preview iframe is the shared grid viewer in server mode (env/obj).
    const previewFrame = page.frameLocator(GRID_FRAME);

    // Wait for the grid to bootstrap (first data column header). initSidebar
    // runs during bootstrap, so the sidebar's collapsed/expanded state is
    // settled once the header is visible.
    await expect(previewFrame.locator('th[data-col-idx="1"]'))
      .toBeVisible({ timeout: TIMEOUTS.fileOpen });

    // The fix: the summary sidebar is collapsed in this host.
    await expect(previewFrame.locator('#sidebarPanel')).not.toHaveClass(/\bexpanded\b/);
    await expect(previewFrame.locator('#sidebarToggle')).toHaveAttribute('aria-expanded', 'false');

    // Dismiss the completion + help popups.
    await page.keyboard.press('Escape');
    await page.keyboard.press('Escape');
  });
});

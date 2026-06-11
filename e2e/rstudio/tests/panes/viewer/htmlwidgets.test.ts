import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import {
  VIEWER_TAB, VIEWER_FRAME, PUBLISH_BTN_IN_PANEL, CONTAINER,
  switchToViewerFrame,
} from '@pages/viewer_pane.page';

test.describe('Viewer pane htmlwidgets', () => {
  let consoleActions: ConsolePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    // plotly pulls a large dependency tree; allow plenty of install time.
    missingPackages = await consoleActions.ensurePackages(['plotly'], 600_000);
    await consoleActions.clearConsole();
  });

  test('sending htmlwidgets to the viewer pane', async ({ rstudioPage: page }) => {
    // Original: test_desktop_ViewerPane.py::test_sending_htmlwidgets_to_viewer_pane
    test.skip(missingPackages.length > 0, `required R package(s) not available: ${missingPackages.join(', ')}`);

    // Build a plotly scatter, then add a labeled text trace; the second call
    // auto-prints the widget, which sends it to the Viewer pane.
    await consoleActions.executeInConsole(
      "x <- plotly::plot_ly(x = rnorm(100), y = rnorm(100), mode = 'markers', type = 'scatter')",
      { wait: true },
    );
    await consoleActions.executeInConsole(
      "plotly::add_text(p = x, text = 'You missed the point')",
      { wait: true },
    );

    // Widget renders in the Viewer pane.
    await expect(page.locator(VIEWER_TAB)).toBeVisible({ timeout: 20000 });
    await expect(page.locator(VIEWER_FRAME)).toBeVisible({ timeout: 20000 });
    await expect(page.locator(PUBLISH_BTN_IN_PANEL)).toBeVisible({ timeout: 10000 });

    // The text-trace label appears in the rendered widget.
    const viewerFrame = switchToViewerFrame(page);
    await expect(viewerFrame.locator(CONTAINER).first()).toContainText('You missed the point', { timeout: 20000 });
  });
});

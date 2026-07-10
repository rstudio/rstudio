// Regression tests for https://github.com/rstudio/rstudio/issues/17738.
//
// closeAllSourceDocs starts a SerializedCommandQueue of close operations and,
// when the last tab closes, fires LastSourceDocClosedEvent ->
// PaneManager.closeSourceWindow -> WindowStateChangeEvent(HIDE), which kicks
// off a 250ms animation on the source LogicalWindow. A file-open arriving in
// that window would silently drop because WindowFrame.onEnsureVisible only
// fired the WindowStateChangeEvent(NORMAL) recovery when !isVisible() -- and
// during the HIDE animation the WindowFrame is still visible in the DOM.
//
// The fix tracks the LogicalWindow's state on the WindowFrame and also
// recovers when that state is HIDE or MINIMIZE. The tests below assert the
// recovery for both file.edit (Source.onFileEdit) and View() (Source.onShowData)
// since both flow into the same ensureVisible path.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { DataViewerPane } from '@pages/data_viewer.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile } from '@utils/files';
import { executeCommand, documentCloseAllNoSave } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

test.describe('Source pane open during close race', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  test('file.edit immediately after closeAllSourceDocs still opens the tab', async ({ rstudioPage: page }) => {
    const initialFile = `race_initial_${Date.now()}.R`;
    const targetFile = `race_target_${Date.now()}.R`;

    // Open an initial file so closeAllSourceDocs has a tab to close (and the
    // close pipeline fires LastSourceDocClosedEvent on completion).
    await writeAndOpenFile(page, sandbox.dir, initialFile, '# initial');

    // Kick off closeAllSourceDocs and -- without waiting for it to drain --
    // create + open a new file. The interleaving here is exactly what the
    // issue's reproducer describes.
    await executeCommand(page, 'closeAllSourceDocs');
    await consoleActions.executeInConsole(`file.create("${targetFile}")`);
    await consoleActions.executeInConsole(`file.edit("${targetFile}")`);

    const sourcePane = new SourcePane(page);
    await expect(sourcePane.selectedTab).toContainText(targetFile, { timeout: 10000 });
  });

  // The View() / documentCloseAllNoSave variant exercises the same race
  // through Source.onShowData (data viewer iframe) rather than onFileEdit.
  // This was left as a fixme when #17740 landed -- at the time the data
  // viewer iframe still failed to finish loading even with the
  // WindowFrame.onEnsureVisible HIDE-state recovery in place. The
  // data-viewer mount path has since been reworked and the race no longer
  // reproduces; the iframe renders reliably (verified over repeated runs).
  test('View() immediately after documentCloseAllNoSave still renders the data viewer', async ({
    rstudioPage: page,
  }) => {
    const initialFile = `race_view_initial_${Date.now()}.R`;

    // Seed the source pane so the close has something to close (and so
    // LastSourceDocClosedEvent fires when the last tab goes away).
    await writeAndOpenFile(page, sandbox.dir, initialFile, '# initial');

    // Bridge-path close (mirrors what the data_viewer.test.ts afterEach was
    // doing before we switched to resetSourcePaneState). Fire-and-forget --
    // the close pipeline runs async and we want View() to land while the
    // HIDE animation is still in flight.
    await documentCloseAllNoSave(page);
    await consoleActions.executeInConsole('View(mtcars)');

    // The viewer tab opening isn't a strong enough signal -- under #17738
    // the tab can attach to a still-hiding pane and never finish loading.
    // Assert the iframe actually renders its first column header so we
    // exercise the full open + render path.
    const dataViewer = new DataViewerPane(page);
    await expect(dataViewer.frame.locator('th[data-col-idx="1"]'))
      .toBeVisible({ timeout: TIMEOUTS.fileOpen });
  });
});

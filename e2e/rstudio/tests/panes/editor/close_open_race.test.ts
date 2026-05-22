// Regression test for https://github.com/rstudio/rstudio/issues/17738.
//
// closeAllSourceDocs starts a SerializedCommandQueue of close operations and,
// when the last tab closes, fires LastSourceDocClosedEvent ->
// PaneManager.closeSourceWindow -> WindowStateChangeEvent(HIDE), which kicks
// off a 250ms animation on the source LogicalWindow. A file.edit arriving in
// that window would silently drop because WindowFrame.onEnsureVisible only
// fired the WindowStateChangeEvent(NORMAL) recovery when !isVisible() -- and
// during the HIDE animation the WindowFrame is still visible in the DOM.
//
// The fix tracks the LogicalWindow's state on the WindowFrame and also
// recovers when that state is HIDE or MINIMIZE. This test asserts that a
// file.edit issued immediately after closeAllSourceDocs opens its tab.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile } from '@utils/files';
import { executeCommand } from '@utils/commands';

test.describe('Source pane open during close race', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
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
});

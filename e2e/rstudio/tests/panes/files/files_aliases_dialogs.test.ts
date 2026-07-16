// Web file dialogs follow macOS Finder aliases (#18158).
//
// The backend resolves Finder aliases during listing (emitting
// `alias_target`; see files_aliases.test.ts for the Files pane side).
// The GWT file dialogs -- used in server mode; Desktop shows native
// dialogs -- must follow the target in every acceptance path:
//
//   - row commit (double-click / Enter): DirectoryContentsWidget.commitSelection
//   - Open button on a selected item:    OpenFileDialog.shouldAccept /
//                                        FileDialog.getSelectedItem
//   - Open button on a typed name:       FileDialog.navigateIfDirectory
//   - Choose Directory selection:        ChooseFolderDialog2.onSelection
//
// The alias fixture (creation mechanics and rationale) lives in
// @utils/finder-aliases.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand, waitForActiveDocument } from '@utils/commands';
import { openFileDialogAtHome } from '@utils/file-dialogs';
import {
  createFinderAliasFixture,
  removeFinderAliasFixture,
  uniqueFinderAliasFixture,
} from '@utils/finder-aliases';

const fx = uniqueFinderAliasFixture('dlg');

const CHOOSE_DIR_DIALOG = '.gwt-DialogBox[aria-label*="Working Directory"]';

test.describe('Web file dialogs follow macOS Finder aliases @server_only', () => {
  test.skip(process.platform !== 'darwin', 'Finder aliases are macOS-only');

  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await createFinderAliasFixture(page, consoleActions, fx);
  });

  test.afterAll(async () => {
    await removeFinderAliasFixture(consoleActions, fx);
  });

  test('double-clicking a directory alias lists the target folder', async ({ rstudioPage: page }) => {
    const dialog = await openFileDialogAtHome(page);

    await dialog.getByText(fx.dirAliasName, { exact: true }).dblclick();

    // the marker file only exists inside the target directory
    await expect(dialog.getByText(fx.markerName, { exact: true })).toBeVisible({ timeout: 15000 });
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0, { timeout: 15000 });
  });

  test('Open button with a selected directory alias navigates into the target', async ({ rstudioPage: page }) => {
    const dialog = await openFileDialogAtHome(page);

    // single click selects without committing; Open triggers shouldAccept
    await dialog.getByText(fx.dirAliasName, { exact: true }).click();
    await dialog.getByRole('button', { name: 'Open' }).click();
    await expect(dialog.getByText(fx.markerName, { exact: true })).toBeVisible({ timeout: 15000 });
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0, { timeout: 15000 });
  });

  test('Open button with a typed directory alias name navigates into the target', async ({ rstudioPage: page }) => {
    const dialog = await openFileDialogAtHome(page);
    await expect(dialog.getByText(fx.dirAliasName, { exact: true })).toBeVisible({ timeout: 15000 });

    // typed input exercises FileDialog.navigateIfDirectory
    await dialog.locator('input[type="text"]').fill(fx.dirAliasName);
    await dialog.getByRole('button', { name: 'Open' }).click();
    await expect(dialog.getByText(fx.markerName, { exact: true })).toBeVisible({ timeout: 15000 });
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0, { timeout: 15000 });
  });

  test('Open button with a selected file alias opens the target document', async ({ rstudioPage: page }) => {
    const dialog = await openFileDialogAtHome(page);

    await dialog.getByText(fx.fileAliasName, { exact: true }).click();
    await dialog.getByRole('button', { name: 'Open' }).click();
    await waitForActiveDocument(page, `~/${fx.targetDocName}`, 15000);
  });

  test('Choose Directory with a selected directory alias picks the target', async ({ rstudioPage: page }) => {
    // the folder chooser opens at the R working directory
    // (Workbench.onSetWorkingDir), so pin that to ~ first
    await consoleActions.executeInConsole('setwd(path.expand("~"))', { wait: true });
    await executeCommand(page, 'setWorkingDir');
    const dialog = page.locator(CHOOSE_DIR_DIALOG);
    await expect(dialog).toBeVisible({ timeout: 15000 });

    await dialog.getByText(fx.dirAliasName, { exact: true }).click();
    await dialog.getByRole('button', { name: 'Choose' }).click();
    await expect(dialog).toHaveCount(0, { timeout: 15000 });

    // ChooseFolderDialog2.onSelection resolved the alias, so the session's
    // working directory must now be the target, not the bookmark file
    await consoleActions.executeInConsole(
      `writeLines(if (identical(basename(getwd()), "${fx.targetDirName}")) "WD-RESOLVED-OK" else paste("WD-BAD:", getwd()))`,
      { wait: true },
    );
    await expect(page.getByText('WD-RESOLVED-OK', { exact: true })).toBeVisible({ timeout: 15000 });
    await consoleActions.executeInConsole('setwd(path.expand("~"))', { wait: true });
  });
});

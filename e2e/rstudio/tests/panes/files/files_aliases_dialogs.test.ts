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
// Aliases are created via `osascript -l JavaScript` (the ObjC bridge's
// NSURL bookmark API -- the same data Finder writes) rather than Finder
// scripting, which would require automation (TCC) permission. Creation
// runs through the R console so paths resolve against rsession's HOME.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand, waitForActiveDocument } from '@utils/commands';

// JXA script: argv[0] = target path, argv[1] = alias path to create.
// 1024 is NSURLBookmarkCreationSuitableForBookmarkFile (1 << 10); the raw
// value avoids depending on the ObjC bridge exposing the enum constant.
const CREATE_ALIAS_JXA = [
  'ObjC.import("Foundation");',
  'function run(argv) {',
  '  var data = $.NSURL.fileURLWithPath(argv[0])',
  '    .bookmarkDataWithOptionsIncludingResourceValuesForKeysRelativeToURLError(1024, $(), $(), null);',
  '  var ok = $.NSURL.writeBookmarkDataToURLOptionsError(data, $.NSURL.fileURLWithPath(argv[1]), 0, null);',
  '  return String(ok);',
  '}',
].join(' ');

const uniq = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const targetDirName = `pw-dlg-target-${uniq}`;
const markerName = `marker-${uniq}.txt`;
const targetDocName = `pw-dlg-doc-${uniq}.txt`;
const dirAliasName = `pw-dlg-dir-alias-${uniq}`;
const fileAliasName = `pw-dlg-file-alias-${uniq}`;

const OPEN_FILE_DIALOG = '.gwt-DialogBox[aria-label="Open File"]';

test.describe('Web file dialogs follow macOS Finder aliases @server_only', () => {
  test.skip(process.platform !== 'darwin', 'Finder aliases are macOS-only');

  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.executeInConsole(
      [
        'local({',
        'home <- path.expand("~")',
        `dir.create(file.path(home, "${targetDirName}"))`,
        `writeLines("marker", file.path(home, "${targetDirName}", "${markerName}"))`,
        `writeLines("target doc", file.path(home, "${targetDocName}"))`,
        `jxa <- '${CREATE_ALIAS_JXA}'`,
        'f <- tempfile(fileext = ".js")',
        'writeLines(jxa, f)',
        'mk <- function(target, alias) system2("osascript", c("-l", "JavaScript", f, shQuote(target), shQuote(alias)))',
        `mk(file.path(home, "${targetDirName}"), file.path(home, "${dirAliasName}"))`,
        `mk(file.path(home, "${targetDocName}"), file.path(home, "${fileAliasName}"))`,
        '})',
      ].join('; '),
      { wait: true },
    );
  });

  test.afterAll(async () => {
    await consoleActions.executeInConsole(
      `unlink(file.path(path.expand("~"), c("${targetDirName}", "${targetDocName}", "${dirAliasName}", "${fileAliasName}")), recursive = TRUE)`,
      { wait: true },
    );
  });

  test('double-clicking a directory alias lists the target folder', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'openSourceDoc');
    const dialog = page.locator(OPEN_FILE_DIALOG);
    await expect(dialog).toBeVisible({ timeout: 15000 });

    await dialog.getByText(dirAliasName, { exact: true }).dblclick();

    // the marker file only exists inside the target directory
    await expect(dialog.getByText(markerName, { exact: true })).toBeVisible({ timeout: 15000 });
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0, { timeout: 15000 });
  });

  test('Open button with a selected directory alias navigates into the target', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'openSourceDoc');
    const dialog = page.locator(OPEN_FILE_DIALOG);
    await expect(dialog).toBeVisible({ timeout: 15000 });

    // single click selects without committing; Open triggers shouldAccept
    await dialog.getByText(dirAliasName, { exact: true }).click();
    await dialog.getByRole('button', { name: 'Open' }).click();
    await expect(dialog.getByText(markerName, { exact: true })).toBeVisible({ timeout: 15000 });
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0, { timeout: 15000 });
  });

  test('Open button with a typed directory alias name navigates into the target', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'openSourceDoc');
    const dialog = page.locator(OPEN_FILE_DIALOG);
    await expect(dialog).toBeVisible({ timeout: 15000 });
    await expect(dialog.getByText(dirAliasName, { exact: true })).toBeVisible({ timeout: 15000 });

    // typed input exercises FileDialog.navigateIfDirectory
    await dialog.locator('input[type="text"]').fill(dirAliasName);
    await dialog.getByRole('button', { name: 'Open' }).click();
    await expect(dialog.getByText(markerName, { exact: true })).toBeVisible({ timeout: 15000 });
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0, { timeout: 15000 });
  });

  test('Open button with a selected file alias opens the target document', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'openSourceDoc');
    const dialog = page.locator(OPEN_FILE_DIALOG);
    await expect(dialog).toBeVisible({ timeout: 15000 });

    await dialog.getByText(fileAliasName, { exact: true }).click();
    await dialog.getByRole('button', { name: 'Open' }).click();
    await waitForActiveDocument(page, `~/${targetDocName}`, 15000);
  });

  test('Choose Directory with a selected directory alias picks the target', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'setWorkingDir');
    const dialog = page.locator('.gwt-DialogBox[aria-label*="Working Directory"]');
    await expect(dialog).toBeVisible({ timeout: 15000 });

    await dialog.getByText(dirAliasName, { exact: true }).click();
    await dialog.getByRole('button', { name: 'Choose' }).click();
    await expect(dialog).toHaveCount(0, { timeout: 15000 });

    // ChooseFolderDialog2.onSelection resolved the alias, so the session's
    // working directory must now be the target, not the bookmark file
    await consoleActions.executeInConsole(
      `writeLines(if (identical(basename(getwd()), "${targetDirName}")) "WD-RESOLVED-OK" else paste("WD-BAD:", getwd()))`,
      { wait: true },
    );
    await expect(page.getByText('WD-RESOLVED-OK', { exact: true })).toBeVisible({ timeout: 15000 });
    await consoleActions.executeInConsole('setwd(path.expand("~"))', { wait: true });
  });
});

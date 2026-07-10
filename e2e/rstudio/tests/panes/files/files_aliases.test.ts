// Files pane Finder-alias navigation (#18158).
//
// macOS Finder aliases are bookmark files, not symlinks, so the filesystem
// does not resolve them. The backend resolves them during listing
// (createFileSystemItem in SessionModuleContext.cpp emits `alias_target`)
// and the Files pane follows the target: clicking a directory alias
// navigates into the target folder, and clicking or open-command-ing a
// file alias opens the target file.
//
// The alias fixture (creation mechanics and rationale) lives in
// @utils/finder-aliases.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { documentCloseAllNoSave, executeCommand, waitForActiveDocument } from '@utils/commands';
import {
  createFinderAliasFixture,
  removeFinderAliasFixture,
  uniqueFinderAliasFixture,
} from '@utils/finder-aliases';

const fx = uniqueFinderAliasFixture('files');

test.describe('Files pane follows macOS Finder aliases @desktop_only', () => {
  test.skip(process.platform !== 'darwin', 'Finder aliases are macOS-only');

  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await createFinderAliasFixture(page, consoleActions, fx);
  });

  test.afterAll(async () => {
    await removeFinderAliasFixture(consoleActions, fx);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    // Re-home the pane each test (the directory-alias test navigates the
    // pane into the target dir) and bring it to front so the virtualized
    // grid renders rows.
    await consoleActions.executeInConsole(
      '.rs.api.filesPaneNavigate(path.expand("~"))',
      { wait: true },
    );
    await executeCommand(page, 'activateFiles');
  });

  test('clicking a directory alias navigates to the target folder', async ({ rstudioPage: page }) => {
    const aliasRow = page.locator(`div[title="${fx.dirAliasName}"]`);
    await expect(aliasRow).toBeVisible({ timeout: 15000 });
    await aliasRow.click();

    // The marker file only exists inside the target directory, so seeing it
    // proves the pane listed the alias's target rather than erroring on the
    // alias file itself.
    await expect(page.locator(`div[title="${fx.markerName}"]`)).toBeVisible({ timeout: 15000 });
  });

  test('clicking a file alias opens the target file', async ({ rstudioPage: page }) => {
    const aliasRow = page.locator(`div[title="${fx.fileAliasName}"]`);
    await expect(aliasRow).toBeVisible({ timeout: 15000 });
    await aliasRow.click();

    // The editor must land on the resolved target document, not the alias.
    await waitForActiveDocument(page, `~/${fx.targetDocName}`, 15000);
  });

  test('open command on a selected file alias opens the target file', async ({ rstudioPage: page }) => {
    const aliasRow = page.locator(`div[title="${fx.fileAliasName}"]`);
    await expect(aliasRow).toBeVisible({ timeout: 15000 });

    // select via the row checkbox -- clicking the name would navigate instead
    const row = page.locator('tr', { has: aliasRow });
    await row.locator('input[type="checkbox"]').check();
    await executeCommand(page, 'openFilesInSinglePane');

    // the open command must act on the resolved target, not the alias
    await waitForActiveDocument(page, `~/${fx.targetDocName}`, 15000);
  });

  test('opening an alias and its target in columns opens one column', async ({ rstudioPage: page }) => {
    // an alias and its target resolve to the same document; without
    // dedupe by resolved path, columns mode opened an extra, empty
    // source column for the duplicate
    for (const name of [fx.fileAliasName, fx.targetDocName]) {
      const row = page.locator('tr', { has: page.locator(`div[title="${name}"]`) });
      await expect(row).toBeVisible({ timeout: 15000 });
      await row.locator('input[type="checkbox"]').check();
    }
    await executeCommand(page, 'openEachFileInColumns');
    await waitForActiveDocument(page, `~/${fx.targetDocName}`, 15000);

    // exactly the main source pane plus one new column
    await expect
      .poll(
        () => page.evaluate(() => document.querySelectorAll("[class*='rstudio_source_panel']").length),
        { timeout: 15000 },
      )
      .toBe(2);

    // closing the document also removes the added column
    await documentCloseAllNoSave(page);
    await expect
      .poll(
        () => page.evaluate(() => document.querySelectorAll("[class*='rstudio_source_panel']").length),
        { timeout: 15000 },
      )
      .toBe(1);
  });
});

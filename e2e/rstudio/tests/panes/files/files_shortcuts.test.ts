// Files pane Windows .lnk shortcut navigation (#7327).
//
// Windows shortcuts are shell objects, not symlinks, so the filesystem does
// not resolve them. The backend resolves them during listing
// (createFileSystemItem in SessionModuleContext.cpp emits `alias_target`
// plus `is_shortcut`) and the Files pane follows the target: clicking a
// directory shortcut navigates into the target folder, and clicking or
// open-command-ing a file shortcut opens the target file.
//
// This mirrors files_aliases.test.ts (the macOS Finder-alias counterpart);
// the shortcut fixture (creation mechanics and rationale) lives in
// @utils/windows-shortcuts.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { documentCloseAllNoSave, executeCommand, waitForActiveDocument } from '@utils/commands';
import {
  createWindowsShortcutFixture,
  removeWindowsShortcutFixture,
  uniqueWindowsShortcutFixture,
} from '@utils/windows-shortcuts';

const fx = uniqueWindowsShortcutFixture('files');

test.describe('Files pane follows Windows shortcuts @windows_only @desktop_only', () => {
  test.skip(process.platform !== 'win32', 'Windows .lnk shortcuts are Windows-only');

  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await createWindowsShortcutFixture(page, consoleActions, fx);
  });

  test.afterAll(async () => {
    await removeWindowsShortcutFixture(consoleActions, fx);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    // Re-home the pane each test (the directory-shortcut test navigates the
    // pane into the target dir) and bring it to front so the virtualized
    // grid renders rows.
    await consoleActions.executeInConsole(
      '.rs.api.filesPaneNavigate(path.expand("~"))',
      { wait: true },
    );
    await executeCommand(page, 'activateFiles');
  });

  test('clicking a directory shortcut navigates to the target folder', async ({ rstudioPage: page }) => {
    const shortcutRow = page.locator(`div[title="${fx.dirShortcutName}"]`);
    await expect(shortcutRow).toBeVisible({ timeout: 15000 });
    await shortcutRow.click();

    // The marker file only exists inside the target directory, so seeing it
    // proves the pane listed the shortcut's target rather than erroring on
    // the .lnk file itself.
    await expect(page.locator(`div[title="${fx.markerName}"]`)).toBeVisible({ timeout: 15000 });
  });

  test('clicking a file shortcut opens the target file', async ({ rstudioPage: page }) => {
    const shortcutRow = page.locator(`div[title="${fx.fileShortcutName}"]`);
    await expect(shortcutRow).toBeVisible({ timeout: 15000 });
    await shortcutRow.click();

    // The editor must land on the resolved target document, not the .lnk.
    await waitForActiveDocument(page, `~/${fx.targetDocName}`, 15000);
  });

  test('open command on a selected file shortcut opens the target file', async ({ rstudioPage: page }) => {
    const shortcutRow = page.locator(`div[title="${fx.fileShortcutName}"]`);
    await expect(shortcutRow).toBeVisible({ timeout: 15000 });

    // select via the row checkbox -- clicking the name would navigate instead
    const row = page.locator('tr', { has: shortcutRow });
    await row.locator('input[type="checkbox"]').check();
    await executeCommand(page, 'openFilesInSinglePane');

    // the open command must act on the resolved target, not the .lnk
    await waitForActiveDocument(page, `~/${fx.targetDocName}`, 15000);
  });

  test('opening a shortcut and its target in columns opens one column', async ({ rstudioPage: page }) => {
    // a shortcut and its target resolve to the same document; without
    // dedupe by resolved path, columns mode opened an extra, empty
    // source column for the duplicate
    for (const name of [fx.fileShortcutName, fx.targetDocName]) {
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

// Typed directory navigation in the Open File dialog.
//
// FileDialog.navigateIfDirectory resolves a typed name against the browsed
// directory's listing and cd's to the item's absolute path (changed from
// cd'ing the relative name when Finder-alias support landed, #18158). The
// alias variants of this flow are covered by the macOS-only
// files_aliases_dialogs.test.ts; this spec guards the plain-directory path,
// which runs on every platform and therefore in Linux server CI.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { openFileDialogAtHome } from '@utils/file-dialogs';

const uniq = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const dirName = `pw-typed-nav-${uniq}`;
const markerName = `marker-${uniq}.txt`;

test.describe('Open File dialog typed directory navigation @server_only', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.executeInConsole(
      [
        'local({',
        'home <- path.expand("~")',
        `dir.create(file.path(home, "${dirName}"))`,
        `writeLines("marker", file.path(home, "${dirName}", "${markerName}"))`,
        '})',
      ].join('; '),
      { wait: true },
    );
  });

  test.afterAll(async () => {
    await consoleActions.executeInConsole(
      `unlink(file.path(path.expand("~"), "${dirName}"), recursive = TRUE)`,
      { wait: true },
    );
  });

  test('typing a directory name and accepting navigates into it', async ({ rstudioPage: page }) => {
    const dialog = await openFileDialogAtHome(page);
    await expect(dialog.getByText(dirName, { exact: true })).toBeVisible({ timeout: 15000 });

    await dialog.locator('input[type="text"]').fill(dirName);
    await dialog.getByRole('button', { name: 'Open' }).click();

    // the marker file only exists inside the directory
    await expect(dialog.getByText(markerName, { exact: true })).toBeVisible({ timeout: 15000 });
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0, { timeout: 15000 });
  });
});

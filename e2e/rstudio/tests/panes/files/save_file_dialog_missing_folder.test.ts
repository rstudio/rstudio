// A typed `dir/name` whose folder cannot be entered must leave the dialog
// where it is, with the typed path given back, rather than staging `name` in
// a fallback folder (#18959). The suite uses GWT file dialogs on Desktop and
// Server.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { dismissBlockingModals } from '@pages/modals.page';
import { executeCommand, resetSourcePaneState, waitForActiveDocument } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';

test.describe('Save File dialog missing folder', () => {
  const sandbox = useSuiteSandbox();

  for (const relative of [true, false]) {
    test(`a missing ${relative ? 'relative' : 'absolute'} folder reports an error without saving`, async ({ rstudioPage: page }) => {
      await resetSourcePaneState(page);
      await executeCommand(page, 'saveSourceDocAs');
      const filename = page.locator('#file_dialog_name_prompt');
      const dialog = page.getByRole('dialog').filter({ has: filename });
      await expect(dialog.locator('[aria-current="location"]')).toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // Normalize the starting directory before trying a relative path.
      await dialog.getByRole('button', { name: 'Go to directory' }).click();
      const prompt = page.getByRole('dialog', { name: 'Go To Folder' });
      await prompt.locator('#rstudio_text_entry').fill(sandbox.dir);
      await prompt.getByRole('button', { name: 'OK' }).click();
      const leaf = sandbox.dir.split(/[\\/]/).pop()!;
      await expect(dialog.locator('[aria-current="location"]')).toHaveText(leaf, { timeout: TIMEOUTS.fileOpen });

      const basename = `pw-missing-${relative ? 'relative' : 'absolute'}.R`;
      const missing = `missing-${Date.now()}`;
      const typed = relative ? `${missing}/${basename}` : `${sandbox.dir}/${missing}/${basename}`;
      await filename.fill(typed);
      await dialog.getByRole('button', { name: 'Save', exact: true }).click();

      const error = page.getByRole('alertdialog', { name: 'Error', exact: true });
      await expect(error).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await error.getByRole('button', { name: 'OK' }).click();
      await expect(dialog).toBeVisible();
      expect(await page.evaluate(() => window.rstudio!.documents.active()!.path)).toBeNull();

      // The dialog stays put and gives the whole typed path back, so a Save
      // without correcting it cannot land `basename` in the folder shown.
      await expect(dialog.locator('[aria-current="location"]')).toHaveText(leaf);
      await expect(filename).toBeEnabled();
      await expect(filename).toHaveValue(typed);

      // A corrected path still works after the error.
      const corrected = `${sandbox.dir}/${basename}`;
      await filename.fill(corrected);
      await dialog.getByRole('button', { name: 'Save', exact: true }).click();
      await expect(dialog).toBeHidden({ timeout: TIMEOUTS.fileOpen });
      await waitForActiveDocument(page, corrected);
      expect(await new ConsolePaneActions(page).evalRLogical(`file.exists(${rPathLiteral(corrected)})`)).toBe(true);
    });
  }

  test.afterEach(async ({ rstudioPage: page }) => {
    await dismissBlockingModals(page);
  });
});

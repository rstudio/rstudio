// Opening a folder that has no .Rproj from the web Open Project dialog
// creates the .Rproj and opens the project, as the native dialogs do
// (#14985).

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { executeCommand, waitForProjectOpen } from '@utils/commands';
import { closeProjectIfOpen } from '@utils/project';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import type { Locator, Page } from 'playwright';

const OPEN_PROJECT_DIALOG = '.gwt-DialogBox[aria-label="Open Project"]';

test.describe('Open Project dialog', () => {
  const sandbox = useSuiteSandbox();

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
  });

  // Creates `name` under the sandbox without a .Rproj and opens the dialog.
  // A retry finds the folder left by an earlier attempt; its .Rproj is removed
  // so the open has to create one.
  async function openDialogForFolder(page: Page, name: string): Promise<{ dialog: Locator; dir: string; rproj: string }> {
    const dir = `${sandbox.dir.replace(/\\/g, '/')}/${name}`;
    const rproj = `${dir}/${name}.Rproj`;
    await executeInConsole(
      page,
      `{ dir.create(${rPathLiteral(dir)}, showWarnings = FALSE); unlink(${rPathLiteral(rproj)}) }`,
      { wait: true },
    );

    await executeCommand(page, 'openProject');
    const dialog = page.locator(OPEN_PROJECT_DIALOG);
    await expect(dialog).toBeVisible({ timeout: 15000 });
    return { dialog, dir, rproj };
  }

  test('opening a folder without a .Rproj creates and opens one', async ({ rstudioPage: page }) => {
    const name = 'folder_without_rproj';
    const { dialog, dir, rproj } = await openDialogForFolder(page, name);

    // a typed directory path navigates into that directory; the breadcrumb
    // marks the listed directory with aria-current
    await dialog.locator('input[type="text"]').fill(dir);
    await dialog.getByRole('button', { name: 'Open' }).click();
    await expect(dialog.locator('[aria-current="location"]')).toHaveText(name, { timeout: 15000 });

    // Open with no filename and no selection opens the listed directory
    await page.evaluate(() => {
      if (window.rstudio) window.rstudio.ready = false;
    });
    await dialog.getByRole('button', { name: 'Open' }).click();
    await waitForProjectOpen(page, rproj);
  });

  test('a typed folder path ending in a separator opens it straight away', async ({ rstudioPage: page }) => {
    const { dialog, dir, rproj } = await openDialogForFolder(page, 'folder_typed_separator');

    // the trailing separator names a folder to browse; the Open deferred past
    // that navigation finds no name and no selection, so it opens the listed
    // directory as a project
    await page.evaluate(() => {
      if (window.rstudio) window.rstudio.ready = false;
    });
    await dialog.locator('input[type="text"]').fill(`${dir}/`);
    await dialog.getByRole('button', { name: 'Open' }).click();
    await waitForProjectOpen(page, rproj);
  });
});

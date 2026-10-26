// Opening a folder that has no .Rproj from the web Open Project dialog
// creates the .Rproj and opens the project, as the native dialogs do
// (#14985).

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { executeCommand, waitForProjectOpen } from '@utils/commands';
import { closeProjectIfOpen } from '@utils/project';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';

const OPEN_PROJECT_DIALOG = '.gwt-DialogBox[aria-label="Open Project"]';

test.describe('Open Project dialog', () => {
  const sandbox = useSuiteSandbox();

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
  });

  test('opening a folder without a .Rproj creates and opens one', async ({ rstudioPage: page }) => {
    const name = 'folder_without_rproj';
    const dir = `${sandbox.dir.replace(/\\/g, '/')}/${name}`;
    const rproj = `${dir}/${name}.Rproj`;

    // a retry finds the folder left by an earlier attempt; remove its .Rproj
    // so the open has to create one
    await executeInConsole(
      page,
      `{ dir.create(${rPathLiteral(dir)}, showWarnings = FALSE); unlink(${rPathLiteral(rproj)}) }`,
      { wait: true },
    );

    await executeCommand(page, 'openProject');
    const dialog = page.locator(OPEN_PROJECT_DIALOG);
    await expect(dialog).toBeVisible({ timeout: 15000 });

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
});

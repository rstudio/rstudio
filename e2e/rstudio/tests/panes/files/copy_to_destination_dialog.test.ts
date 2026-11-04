// The Choose Destination dialog (Files > More > Copy To...), the GWT save
// dialog. base-prefs.jsonc sets native_file_dialogs=false suite-wide, so
// Desktop shows the GWT dialog too.
//
// #19427: Save with an empty name silently did nothing, and Save with the
// `..` row selected rejected `..` as an illegal file name instead of
// navigating to the parent folder.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import type { Locator, Page } from 'playwright';

const DIALOG = '.gwt-DialogBox[aria-label="Choose Destination"]';
const ERROR_DIALOG = '.gwt-DialogBox[aria-label="Error"]';
const SAVE_BTN = '#rstudio_file_accept_save';
const CURRENT_LOCATION = '[aria-current="location"]';

const BASE_NAME = `pw-copy-to-${Date.now()}`;
const SOURCE = 'source';

function leaf(path: string): string {
  return path.split(/[\\/]/).pop() ?? path;
}

// Selects `SOURCE` in the Files pane (which shows `base`) and opens Copy To.
// The dialog opens in the source's parent, `base`, with an empty name; the
// breadcrumb marks it as the current location once the listing lands.
async function openDestinationDialog(page: Page, base: string): Promise<Locator> {
  const row = page.locator('tr', { has: page.locator(`div[title="${SOURCE}"]`) });
  await expect(row).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  // the row checkbox: clicking the name would navigate into the folder
  await row.locator('input[type="checkbox"]').check();
  await executeCommand(page, 'copyFileTo');

  const dialog = page.locator(DIALOG);
  await expect(dialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  await expect(dialog.locator(CURRENT_LOCATION)).toHaveText(leaf(base), { timeout: TIMEOUTS.fileOpen });
  await expect(dialog.getByRole('textbox', { name: 'File name:' })).toHaveValue('');
  return dialog;
}

test.describe('Copy To destination dialog', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let base = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    base = `${sandbox.dir}/${BASE_NAME}`;
    await consoleActions.executeInConsole(
      `dir.create(file.path(${rPathLiteral(base)}, "${SOURCE}"), recursive = TRUE)`,
      { wait: true },
    );
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    // re-home the pane each test and bring it to front so the grid renders rows
    await consoleActions.executeInConsole(`.rs.api.filesPaneNavigate(${rPathLiteral(base)})`, { wait: true });
    await executeCommand(page, 'activateFiles');
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    const dialog = page.locator(DIALOG);
    if (await dialog.isVisible()) {
      await dialog.getByRole('button', { name: 'Cancel' }).click();
      await expect(dialog).toBeHidden();
    }
  });

  test('Save with an empty name reports that the name is empty', async ({ rstudioPage: page }) => {
    const dialog = await openDestinationDialog(page, base);
    await dialog.locator(SAVE_BTN).click();

    const error = page.locator(ERROR_DIALOG);
    await expect(error).toBeVisible();
    await expect(error).toContainText('Name is empty');
    await error.getByRole('button', { name: 'OK' }).click();
    await expect(error).toBeHidden();

    // the dialog stays open for the user to supply a name
    await expect(dialog).toBeVisible();
  });

  test('Save with a typed path ending in a separator navigates there', async ({ rstudioPage: page }) => {
    const dialog = await openDestinationDialog(page, base);
    await dialog.getByRole('textbox', { name: 'File name:' }).fill(`${sandbox.dir}/`);
    await dialog.locator(SAVE_BTN).click();

    // a folder to browse, not an empty name to accept once there
    await expect(dialog.locator(CURRENT_LOCATION)).toHaveText(leaf(sandbox.dir), { timeout: TIMEOUTS.fileOpen });
    await expect(page.locator(ERROR_DIALOG)).toBeHidden();
    await expect(dialog.getByRole('textbox', { name: 'File name:' })).toHaveValue('');
  });

  test('Save with the `..` row selected navigates to the parent folder', async ({ rstudioPage: page }) => {
    const dialog = await openDestinationDialog(page, base);
    // a single click selects the row without navigating
    await dialog.getByRole('option', { name: 'Folder ..' }).click();
    await dialog.locator(SAVE_BTN).click();

    await expect(dialog.locator(CURRENT_LOCATION)).toHaveText(leaf(sandbox.dir), { timeout: TIMEOUTS.fileOpen });
    await expect(page.locator(ERROR_DIALOG)).toBeHidden();
    await expect(dialog.getByRole('textbox', { name: 'File name:' })).toHaveValue('');

    // the copy then lands in the folder navigated to
    const name = dialog.getByRole('textbox', { name: 'File name:' });
    await name.fill('copied');
    await dialog.locator(SAVE_BTN).click();
    await expect(dialog).toBeHidden({ timeout: TIMEOUTS.fileOpen });
    await expect
      .poll(() => consoleActions.evalRLogical(`dir.exists(file.path(${rPathLiteral(sandbox.dir)}, "copied"))`), {
        timeout: TIMEOUTS.fileOpen,
      })
      .toBe(true);
  });
});

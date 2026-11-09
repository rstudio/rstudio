// The GWT directory chooser (ChooseFolderDialog2), driven through the Find in
// Files "Search in" field. base-prefs.jsonc sets native_file_dialogs=false
// suite-wide, so Desktop shows the GWT chooser too.
//
// Choose returns the selected folder, or the current directory when nothing
// is selected. #18938: a listing's arrival wiped the chooser's path after it
// was set. #18958: selecting the parent row returned the literal path `..`.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { dismissBlockingModals } from '@pages/modals.page';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { FILE_LIST, LIST_FILES_RPC, holdListFiles } from '@utils/file-dialogs';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import type { Locator, Page } from 'playwright';

const FIND_DIALOG = '.gwt-DialogBox[aria-label="Find in Files"]';
const FIND_CANCEL_BTN = '#rstudio_dlg_cancel';
const SEARCH_IN_TEXT = '#rstudio_tbb_text_find_in';
const SEARCH_IN_BROWSE = '#rstudio_tbb_button_find_in';
const CHOOSER = '.gwt-DialogBox[aria-label="Choose Directory"]';
const CHOOSE_BTN = '#rstudio_file_accept_choose';
const NEW_FOLDER_BTN = '#rstudio_file_new_folder';
const TEXT_ENTRY = '#rstudio_text_entry';
const SEARCH_INPUT = '#rstudio_find_files_text';
const FIND_OK_BTN = '#rstudio_dlg_ok';
const CURRENT_LOCATION = '[aria-current="location"]';

const BASE_NAME = `pw-choose-folder-${Date.now()}`;
const SUBFOLDER = 'target';

// Opens Find in Files and its directory chooser, in whatever directory the
// chooser starts in.
async function openChooser(page: Page): Promise<Locator> {
  await executeCommand(page, 'findInFiles');
  await expect(page.locator(FIND_DIALOG)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  await page.locator(SEARCH_IN_BROWSE).click();
  const chooser = page.locator(CHOOSER);
  await expect(chooser).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  return chooser;
}

// Opens Find in Files and its directory chooser, then takes the chooser to
// `dir` with the breadcrumb's Go To Folder prompt: the chooser starts in the
// dialog's last search scope, which depends on earlier specs. `dir` is fresh
// to this spec, so the breadcrumb showing it as the current location means
// that navigation has landed.
async function openChooserAt(page: Page, dir: string): Promise<Locator> {
  const chooser = await openChooser(page);

  // the breadcrumb marks a current location once the initial listing lands;
  // navigating before then would race it, and whichever listing arrived last
  // would decide where the chooser ends up
  await expect(chooser.locator(CURRENT_LOCATION)).toBeVisible({ timeout: TIMEOUTS.fileOpen });

  await chooser.getByRole('button', { name: 'Go to directory' }).click();
  const prompt = page.getByRole('dialog', { name: 'Go To Folder' });
  await expect(prompt).toBeVisible();
  await prompt.locator(TEXT_ENTRY).fill(dir);
  // by role: the Find in Files dialog already owns the OK id
  await prompt.getByRole('button', { name: 'OK' }).click();
  const leaf = dir.split(/[\\/]/).pop() ?? dir;
  await expect(chooser.locator(CURRENT_LOCATION)).toHaveText(leaf, { timeout: TIMEOUTS.fileOpen });
  await expect(chooser.getByText(SUBFOLDER, { exact: true })).toBeVisible();
  return chooser;
}

// Single-clicks the row named `row`, then clicks Choose. A navigation that
// lands would return the same path as choosing the selected row, so list_files
// is held instead: Choose waits on any navigation still in flight, so a click
// that started one would leave the chooser up.
async function chooseRow(page: Page, chooser: Locator, row: string): Promise<void> {
  const listing = await holdListFiles(page);

  try {
    await chooser.getByText(row, { exact: true }).click();
    await chooser.locator(CHOOSE_BTN).click();
    await expect(chooser).toBeHidden();
  } finally {
    await listing.cleanup();
  }
}

test.describe('Choose Directory dialog', () => {
  const sandbox = useSuiteSandbox();
  let base = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    base = `${sandbox.dir}/${BASE_NAME}`;
    await new ConsolePaneActions(page).executeInConsole(
      `dir.create(file.path(${rPathLiteral(base)}, "${SUBFOLDER}"), recursive = TRUE)`,
      { wait: true },
    );
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // after a failure the chooser, or a prompt over it, can still be up, and
    // its glass would block the Cancel click below
    if (await page.locator(CHOOSER).isVisible() ||
        await page.getByRole('alertdialog', { name: 'Error', exact: true }).isVisible())
      await dismissBlockingModals(page);

    const findDialog = page.locator(FIND_DIALOG);
    if (await findDialog.isVisible()) {
      await page.locator(FIND_CANCEL_BTN).click();
      await expect(findDialog).toBeHidden();
    }
  });

  test('choosing after navigating into a folder returns that folder', async ({ rstudioPage: page }) => {
    const chooser = await openChooserAt(page, base);
    await chooser.getByText(SUBFOLDER, { exact: true }).dblclick();
    await expect(chooser.locator(CURRENT_LOCATION)).toHaveText(SUBFOLDER, { timeout: TIMEOUTS.fileOpen });

    await chooser.locator(CHOOSE_BTN).click();
    await expect(chooser).toBeHidden();
    await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(new RegExp(`/${BASE_NAME}/${SUBFOLDER}$`));
  });

  test('choosing a selected parent row returns the parent folder', async ({ rstudioPage: page }) => {
    const chooser = await openChooserAt(page, base);
    await chooser.getByText(SUBFOLDER, { exact: true }).dblclick();
    await expect(chooser.locator(CURRENT_LOCATION)).toHaveText(SUBFOLDER, { timeout: TIMEOUTS.fileOpen });

    // choosing the selected synthetic .. row returned `..` (#18958)
    await chooseRow(page, chooser, '..');
    await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(new RegExp(`/${BASE_NAME}$`));
  });

  test('double-clicking the parent row navigates to the parent folder', async ({ rstudioPage: page }) => {
    const chooser = await openChooserAt(page, base);
    await chooser.getByText(SUBFOLDER, { exact: true }).dblclick();
    await expect(chooser.locator(CURRENT_LOCATION)).toHaveText(SUBFOLDER, { timeout: TIMEOUTS.fileOpen });

    await chooser.getByText('..', { exact: true }).dblclick();
    await expect(chooser.locator(CURRENT_LOCATION)).toHaveText(BASE_NAME, { timeout: TIMEOUTS.fileOpen });
    await expect(chooser.getByText(SUBFOLDER, { exact: true })).toBeVisible();
    await chooser.locator(CHOOSE_BTN).click();
    await expect(chooser).toBeHidden();
    await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(new RegExp(`/${BASE_NAME}$`));
  });

  test('choosing a selected child row returns the child folder', async ({ rstudioPage: page }) => {
    const chooser = await openChooserAt(page, base);
    await chooseRow(page, chooser, SUBFOLDER);
    await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(new RegExp(`/${BASE_NAME}/${SUBFOLDER}$`));
  });

  test('choosing a new folder before it is listed returns that folder', async ({ rstudioPage: page }) => {
    const folder = `new-${Date.now()}`;
    const chooser = await openChooserAt(page, base);

    // holds list_files only; the New Folder prompt and its mkdir run normally
    const listing = await holdListFiles(page);

    try {
      await chooser.locator(NEW_FOLDER_BTN).click();
      const prompt = page.getByRole('dialog', { name: 'New Folder' });
      await expect(prompt).toBeVisible();
      await prompt.locator(TEXT_ENTRY).fill(folder);
      await prompt.getByRole('button', { name: 'OK' }).click();

      // the chooser navigates into the new folder; Choose waits for that
      // listing (other panes' listings may be held too, so match its path)
      await expect.poll(
        () => listing.heldPaths().some((p) => p.endsWith(`/${folder}`)),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);
      await chooser.locator(CHOOSE_BTN).click();
      await expect(chooser).toBeVisible();

      listing.release();
      await expect(chooser).toBeHidden({ timeout: TIMEOUTS.fileOpen });
      await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(new RegExp(`/${BASE_NAME}/${folder}$`));
    } finally {
      await listing.cleanup();
    }
  });

  test('choosing while a missing folder is loading reports an error and stays put', async ({ rstudioPage: page }) => {
    const chooser = await openChooserAt(page, base);
    const original = await page.locator(SEARCH_IN_TEXT).inputValue();
    const shown = await chooser.locator('#file_dialog_name_prompt').inputValue();
    const missing = `missing-${Date.now()}`;
    const listing = await holdListFiles(page);

    try {
      await chooser.getByRole('button', { name: 'Go to directory' }).click();
      const prompt = page.getByRole('dialog', { name: 'Go To Folder' });
      await prompt.locator(TEXT_ENTRY).fill(`${base}/${missing}`);
      await prompt.getByRole('button', { name: 'OK' }).click();
      await expect.poll(
        () => listing.heldPaths().some((p) => p.endsWith(`/${missing}`)),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);
      await chooser.locator(CHOOSE_BTN).click();

      listing.release();
      const error = page.getByRole('alertdialog', { name: 'Error', exact: true });
      await expect(error).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await error.getByRole('button', { name: 'OK' }).click();
      await expect(chooser).toBeVisible();
      await expect(chooser.locator(CURRENT_LOCATION)).toHaveText(BASE_NAME);
      await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(original);

      // Choosing again returns the folder still shown, not a fallback.
      await expect(chooser.locator('#file_dialog_name_prompt')).toHaveValue(shown);
      await chooser.locator(CHOOSE_BTN).click();
      await expect(chooser).toBeHidden();
      await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(new RegExp(`/${BASE_NAME}$`));
    } finally {
      await listing.cleanup();
    }
  });

  test('canceling a missing-folder navigation does not show a late error', async ({ rstudioPage: page }) => {
    const chooser = await openChooserAt(page, base);
    const missing = `missing-${Date.now()}`;
    const listing = await holdListFiles(page, (path) => path.endsWith(`/${missing}`));

    try {
      await chooser.getByRole('button', { name: 'Go to directory' }).click();
      const prompt = page.getByRole('dialog', { name: 'Go To Folder' });
      await prompt.locator(TEXT_ENTRY).fill(`${base}/${missing}`);
      await prompt.getByRole('button', { name: 'OK' }).click();
      await expect.poll(() => listing.held(), { timeout: TIMEOUTS.fileOpen }).toBe(1);
      await chooser.locator(CHOOSE_BTN).click();
      await chooser.getByRole('button', { name: 'Cancel' }).click();
      await expect(chooser).toBeHidden();

      const failedListing = page.waitForResponse((response) =>
        LIST_FILES_RPC.test(response.url()) &&
        String(response.request().postDataJSON()?.params?.[0]).endsWith(`/${missing}`),
      );
      listing.release();
      await (await failedListing).finished();

      // A later round trip (reopening lists the chooser's folder) orders the
      // assertion after the stale response's callback has run.
      await page.locator(SEARCH_IN_BROWSE).click();
      await expect(chooser.locator(CURRENT_LOCATION)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(page.getByRole('alertdialog', { name: 'Error', exact: true })).toHaveCount(0);
    } finally {
      await listing.cleanup();
    }
  });

  test('reopening the chooser ignores a canceled navigation response', async ({ rstudioPage: page }) => {
    const chooser = await openChooserAt(page, base);
    const missing = `missing-${Date.now()}`;
    const listing = await holdListFiles(page, (path) => path.endsWith(`/${missing}`));

    try {
      await chooser.getByRole('button', { name: 'Go to directory' }).click();
      const prompt = page.getByRole('dialog', { name: 'Go To Folder' });
      await prompt.locator(TEXT_ENTRY).fill(`${base}/${missing}`);
      await prompt.getByRole('button', { name: 'OK' }).click();
      await expect.poll(() => listing.held(), { timeout: TIMEOUTS.fileOpen }).toBe(1);
      await chooser.getByRole('button', { name: 'Cancel' }).click();
      await expect(chooser).toBeHidden();

      // The same Browse control reuses its context for the new chooser.
      await page.locator(SEARCH_IN_BROWSE).click();
      await expect(chooser.locator(CURRENT_LOCATION)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await chooser.getByRole('button', { name: 'Go to directory' }).click();
      await prompt.locator(TEXT_ENTRY).fill(`${base}/${SUBFOLDER}`);
      await prompt.getByRole('button', { name: 'OK' }).click();
      await expect(chooser.locator(CURRENT_LOCATION)).toHaveText(SUBFOLDER, { timeout: TIMEOUTS.fileOpen });

      const failedListing = page.waitForResponse((response) =>
        LIST_FILES_RPC.test(response.url()) &&
        String(response.request().postDataJSON()?.params?.[0]).endsWith(`/${missing}`),
      );
      listing.release();
      await (await failedListing).finished();

      // A later round trip orders the assertions after the stale response's
      // callback has run; a leaked error would block it behind an alert.
      await chooser.getByRole('button', { name: 'Go to directory' }).click();
      await prompt.locator(TEXT_ENTRY).fill(base);
      await prompt.getByRole('button', { name: 'OK' }).click();
      await expect(chooser.locator(CURRENT_LOCATION)).toHaveText(BASE_NAME, { timeout: TIMEOUTS.fileOpen });
      await expect(page.getByRole('alertdialog', { name: 'Error', exact: true })).toHaveCount(0);

      await chooser.locator(CHOOSE_BTN).click();
      await expect(chooser).toBeHidden();
      await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(new RegExp(`/${BASE_NAME}$`));
    } finally {
      await listing.cleanup();
    }
  });

  test('a missing starting folder opens the chooser in the fallback folder without an error', async ({ rstudioPage: page }) => {
    // Find in Files reopens with the folder it last searched. Search in a
    // folder, delete it, and the chooser's opening navigation cannot land.
    const goneName = `gone-${Date.now()}`;
    const gone = `${base}/${goneName}`;
    const console = new ConsolePaneActions(page);
    await console.executeInConsole(
      `dir.create(file.path(${rPathLiteral(gone)}, "${SUBFOLDER}"), recursive = TRUE)`,
      { wait: true },
    );
    const chooser = await openChooserAt(page, gone);
    await chooser.locator(CHOOSE_BTN).click();
    await expect(chooser).toBeHidden();
    await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(new RegExp(`/${goneName}$`));
    // Find enables its OK button on key-up, so type rather than fill.
    await page.locator(SEARCH_INPUT).click();
    await page.locator(SEARCH_INPUT).pressSequentially('pw-choose-folder');
    await page.locator(FIND_OK_BTN).click();
    await expect(page.locator(FIND_DIALOG)).toBeHidden();
    await console.executeInConsole(`unlink(${rPathLiteral(gone)}, recursive = TRUE)`, { wait: true });

    await executeCommand(page, 'findInFiles');
    await expect(page.locator(FIND_DIALOG)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(new RegExp(`/${goneName}$`));
    await page.locator(SEARCH_IN_BROWSE).click();
    await expect(chooser).toBeVisible({ timeout: TIMEOUTS.fileOpen });

    // An error would be raised as the opening listing arrives, which is when
    // the breadcrumb gains a current location.
    await expect(chooser.locator(CURRENT_LOCATION)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(page.getByRole('alertdialog', { name: 'Error', exact: true })).toHaveCount(0);
    const fallback = await chooser.locator('#file_dialog_name_prompt').inputValue();
    expect(fallback).not.toBe('');
    expect(fallback.endsWith(goneName)).toBe(false);

    // The fallback is a real folder, so choosing it works.
    await chooser.locator(CHOOSE_BTN).click();
    await expect(chooser).toBeHidden();
    await expect(page.locator(SEARCH_IN_TEXT)).toHaveValue(fallback);
  });

  test('opening the chooser focuses the folder list without selecting a folder', async ({ rstudioPage: page }) => {
    // a selection would change the folder Choose returns (#19315)
    const chooser = await openChooser(page);

    await expect(chooser.locator(FILE_LIST)).toBeFocused({ timeout: TIMEOUTS.fileOpen });
    await expect(chooser.locator(`${FILE_LIST} tr[aria-selected]`)).toHaveCount(0);
  });

  test('a listing that arrives after the chooser is shown still focuses the folder list', async ({ rstudioPage: page }) => {
    // the list cannot take focus until its first listing attaches it, so the
    // chooser's initial focus has to wait for that listing
    const listing = await holdListFiles(page);

    try {
      const chooser = await openChooser(page);
      await expect.poll(() => listing.held(), { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);
      // outlast the dialog's initial focus, 100ms after it is shown; with the
      // list still unattached, nothing observable marks that point
      await page.waitForTimeout(1000);

      listing.release();
      await expect(chooser.locator(FILE_LIST)).toBeFocused({ timeout: TIMEOUTS.fileOpen });
    } finally {
      await listing.cleanup();
    }
  });

  test('a late first listing does not take focus moved elsewhere meanwhile', async ({ rstudioPage: page }) => {
    const listing = await holdListFiles(page);

    try {
      const chooser = await openChooser(page);
      await expect.poll(() => listing.held(), { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);
      // outlast the dialog's initial focus, which leaves a request pending
      await page.waitForTimeout(1000);

      const cancel = chooser.getByRole('button', { name: 'Cancel' });
      await cancel.focus();
      listing.release();

      // the breadcrumb's current location and any focus change land in the
      // same task, so once it shows the focus decision has been made (a
      // folder with no subfolders lists no rows to wait on instead)
      await expect(chooser.locator(CURRENT_LOCATION)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(cancel).toBeFocused();
    } finally {
      await listing.cleanup();
    }
  });
});

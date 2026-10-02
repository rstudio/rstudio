// The GWT directory chooser (ChooseFolderDialog2), driven through the Find in
// Files "Search in" field. base-prefs.jsonc sets native_file_dialogs=false
// suite-wide, so Desktop shows the GWT chooser too.
//
// Choose hands the caller the directory the chooser shows. #18938: a listing's
// arrival wiped the chooser's path after it was set, so Choose returned an
// empty path and blanked the caller's field.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { dismissBlockingModals } from '@pages/modals.page';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { holdListFiles } from '@utils/file-dialogs';
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
const CURRENT_LOCATION = '[aria-current="location"]';

const BASE_NAME = `pw-choose-folder-${Date.now()}`;
const SUBFOLDER = 'target';

// Opens Find in Files and its directory chooser, then takes the chooser to
// `dir` with the breadcrumb's Go To Folder prompt: the chooser starts in the
// dialog's last search scope, which depends on earlier specs. `dir` is fresh
// to this spec, so the breadcrumb showing it as the current location means
// that navigation has landed.
async function openChooserAt(page: Page, dir: string): Promise<Locator> {
  await executeCommand(page, 'findInFiles');
  await expect(page.locator(FIND_DIALOG)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  await page.locator(SEARCH_IN_BROWSE).click();
  const chooser = page.locator(CHOOSER);
  await expect(chooser).toBeVisible({ timeout: TIMEOUTS.fileOpen });

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
    if (await page.locator(CHOOSER).isVisible())
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
});

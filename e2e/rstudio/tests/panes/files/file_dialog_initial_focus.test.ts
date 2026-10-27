// Initial focus in the web file dialogs (#19315).
//
// Two things used to set focus as the Open File dialog opened: the dialog
// focuses its file name box 100ms after it is shown
// (ModalDialogBase.onDialogShown -> FileDialog.focusInitialControl), and
// every directory listing focused the file list (FileBrowserWidget.
// onNavigated). A listing faster than 100ms flashed focus on the list
// before the name box took it; a slower one stole focus from the name box.
// The first listing now leaves focus to the dialog; later navigations
// still bring it to the list.

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { OPEN_FILE_DIALOG, holdListFiles } from '@utils/file-dialogs';

const FILE_LIST = '#rstudio_directory_contents';
const FILE_NAME = '#file_dialog_name_prompt';
// the breadcrumb marks the current location when a listing arrives, in the
// same task as any focus change, and also for a directory with no entries
const CURRENT_LOCATION = '[aria-current="location"]';

test.describe('Web file dialog initial focus @server_only', () => {
  test('a listing that arrives after the dialog is shown leaves focus on the file name', async ({ rstudioPage: page }) => {
    const listing = await holdListFiles(page);

    try {
      await executeCommand(page, 'openSourceDoc');
      const dialog = page.locator(OPEN_FILE_DIALOG);
      await expect(dialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect.poll(() => listing.held(), { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);
      await expect(dialog.locator(FILE_NAME)).toBeFocused({ timeout: TIMEOUTS.fileOpen });

      listing.release();

      await expect(dialog.locator(CURRENT_LOCATION)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dialog.locator(FILE_NAME)).toBeFocused();

      await page.keyboard.press('Escape');
      await expect(dialog).toHaveCount(0, { timeout: TIMEOUTS.fileOpen });
    } finally {
      await listing.cleanup();
    }
  });

  test('opening the dialog never focuses the file list', async ({ rstudioPage: page }) => {
    await page.evaluate(() => {
      const w = window as unknown as { __pwFocusLog: string[]; __pwFocusListener: (e: Event) => void };
      w.__pwFocusLog = [];
      w.__pwFocusListener = (e: Event) => w.__pwFocusLog.push((e.target as Element).id);
      document.addEventListener('focusin', w.__pwFocusListener, true);
    });

    try {
      await executeCommand(page, 'openSourceDoc');
      const dialog = page.locator(OPEN_FILE_DIALOG);
      await expect(dialog.locator(CURRENT_LOCATION)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dialog.locator(FILE_NAME)).toBeFocused({ timeout: TIMEOUTS.fileOpen });

      const focused = await page.evaluate(() =>
        (window as unknown as { __pwFocusLog: string[] }).__pwFocusLog);
      expect(focused).not.toContain(FILE_LIST.slice(1));

      await page.keyboard.press('Escape');
      await expect(dialog).toHaveCount(0, { timeout: TIMEOUTS.fileOpen });
    } finally {
      await page.evaluate(() => {
        const w = window as unknown as { __pwFocusListener: (e: Event) => void };
        document.removeEventListener('focusin', w.__pwFocusListener, true);
      });
    }
  });

  test('navigating after the dialog opens moves focus to the file list', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'openSourceDoc');
    const dialog = page.locator(OPEN_FILE_DIALOG);
    const filename = dialog.locator(FILE_NAME);

    // wait out the dialog's own initial focus, which would otherwise land
    // after a navigation started within its first 100ms
    await expect(filename).toBeFocused({ timeout: TIMEOUTS.fileOpen });

    await filename.fill('~');
    await dialog.getByRole('button', { name: 'Open' }).click();
    // navigating clears the filename box (OpenFileDialog.onBrowserNavigated)
    await expect(filename).toHaveValue('', { timeout: TIMEOUTS.fileOpen });

    await expect(dialog.locator(FILE_LIST)).toBeFocused();

    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0, { timeout: TIMEOUTS.fileOpen });
  });
});

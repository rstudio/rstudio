import type { AxeResults } from 'axe-core';
import { test, expect } from '@fixtures/a11y.fixture';
import type { Locator, Page } from '@playwright/test';
import { scanA11y, violationSummary } from '@utils/a11y';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { OPEN_FILE_DIALOG } from '@utils/file-dialogs';

const FILE_NAME = '#file_dialog_name_prompt';
// The breadcrumb marks the current location once the first listing arrives,
// even for a directory with no entries.
const CURRENT_LOCATION = '[aria-current="location"]';

async function closeDialog(page: Page, dialog: Locator): Promise<void> {
  await page.keyboard.press('Escape');
  await expect(dialog).toHaveCount(0, { timeout: TIMEOUTS.fileOpen });
}

/**
 * The initial state with the Open File dialog (File > Open File) showing the
 * working directory. The dialog is closed again before the assertion, and
 * when the test fails first: the fixture's teardown types into the console,
 * which a modal would block.
 */
test('open file dialog has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const dialog = page.locator(OPEN_FILE_DIALOG);
    let results: AxeResults;
    try {
      await executeCommand(page, 'openSourceDoc');
      await expect(dialog.locator(CURRENT_LOCATION)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      // The dialog focuses its file name box 100ms after it is shown.
      await expect(dialog.locator(FILE_NAME)).toBeFocused({ timeout: TIMEOUTS.fileOpen });
      results = await scanA11y(page, testInfo);
    } catch (err) {
      // Logged only, so a close problem can't hide the original error.
      await closeDialog(page, dialog)
        .catch((closeErr: Error) => console.warn(`Open File dialog not closed: ${closeErr.message}`));
      throw err;
    }
    await closeDialog(page, dialog);
    expect(violationSummary(results)).toEqual([]);
  });

import { test, expect } from '@fixtures/a11y.fixture';
import { scanA11y, violationSummary } from '@utils/a11y';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { OPEN_FILE_DIALOG } from '@utils/file-dialogs';

const FILE_NAME = '#file_dialog_name_prompt';
// The breadcrumb marks the current location once the first listing arrives,
// even for a directory with no entries.
const CURRENT_LOCATION = '[aria-current="location"]';

/**
 * The initial state with the Open File dialog (File > Open File) showing the
 * working directory. The dialog is closed again before the assertion: the
 * fixture's teardown types into the console, which a modal would block.
 */
test('open file dialog has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    await executeCommand(page, 'openSourceDoc');
    const dialog = page.locator(OPEN_FILE_DIALOG);
    await expect(dialog.locator(CURRENT_LOCATION)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    // The dialog focuses its file name box 100ms after it is shown.
    await expect(dialog.locator(FILE_NAME)).toBeFocused({ timeout: TIMEOUTS.fileOpen });

    const results = await scanA11y(page, testInfo);

    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0, { timeout: TIMEOUTS.fileOpen });
    expect(violationSummary(results)).toEqual([]);
  });

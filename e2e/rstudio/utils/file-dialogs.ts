// Helpers for driving the web (GWT) file dialogs, which are used in server
// mode; Desktop shows native dialogs.

import { Locator, Page, expect } from '@playwright/test';
import { executeCommand } from '@utils/commands';

export const OPEN_FILE_DIALOG = '.gwt-DialogBox[aria-label="Open File"]';

// Open the Open File dialog and navigate it to ~. The dialog opens in the
// directory of the last dialog-opened file (WorkbenchContext.
// getDefaultFileDialogDir), falling back to the working directory --
// nondeterministic across specs sharing the session, and possibly outside
// HOME, where the breadcrumb offers no Home crumb. Typing '~' and accepting
// is an explicit navigation FileDialog.shouldAccept supports from any
// location, so normalize through it to keep tests order-independent.
export async function openFileDialogAtHome(page: Page): Promise<Locator> {
  await executeCommand(page, 'openSourceDoc');
  const dialog = page.locator(OPEN_FILE_DIALOG);
  await expect(dialog).toBeVisible({ timeout: 15000 });
  const filename = dialog.locator('input[type="text"]');
  await filename.fill('~');
  await dialog.getByRole('button', { name: 'Open' }).click();
  // navigating clears the filename box (OpenFileDialog.onNavigated)
  await expect(filename).toHaveValue('', { timeout: 15000 });
  return dialog;
}

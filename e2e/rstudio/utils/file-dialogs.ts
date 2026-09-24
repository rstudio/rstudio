// Helpers for driving the web (GWT) file dialogs, which are used in server
// mode; Desktop shows native dialogs.

import { Locator, Page, expect } from '@playwright/test';
import { executeCommand } from '@utils/commands';

export const OPEN_FILE_DIALOG = '.gwt-DialogBox[aria-label="Open File"]';

// The RPC the GWT file dialogs list their directory with.
export const LIST_FILES_RPC = /\/rpc\/list_files(?:\?|$)/;

// Holds every list_files RPC until release() is called, so a dialog can be
// driven before its directory listing arrives. cleanup() releases anything
// still held and removes the route; call it from a finally block.
export async function holdListFiles(page: Page) {
  let release = () => {};
  const released = new Promise<void>((resolve) => (release = resolve));
  let held = 0;
  const continued: Promise<void>[] = [];
  await page.route(LIST_FILES_RPC, async (route) => {
    held++;
    const request = released.then(() => route.continue());
    continued.push(request);
    await request;
  });

  return {
    held: () => held,
    release,
    cleanup: async () => {
      // let held requests through before removing the route, so an unroute
      // doesn't continue them a second time
      release();
      await Promise.allSettled(continued);
      await page.unroute(LIST_FILES_RPC);
    },
  };
}

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

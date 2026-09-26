// Helpers for driving the web (GWT) file dialogs. Server mode always uses
// them, and base-prefs.jsonc sets native_file_dialogs=false, so Desktop does
// too.

import { Locator, Page, Request, Route, expect } from '@playwright/test';
import { executeCommand } from '@utils/commands';

export const OPEN_FILE_DIALOG = '.gwt-DialogBox[aria-label="Open File"]';

// The RPC the GWT file dialogs list their directory with.
export const LIST_FILES_RPC = /\/rpc\/list_files(?:\?|$)/;

// The directory a list_files request asks for ('' if unreadable). Never
// throws: a throw in a route handler would leave the request hanging.
function listFilesPath(request: Request): string {
  try {
    return String(request.postDataJSON()?.params?.[0] ?? '');
  } catch {
    return '';
  }
}

// Holds every list_files RPC until release() is called, so a dialog can be
// driven before its directory listing arrives. cleanup() releases anything
// still held and removes the route; call it from a finally block.
//
// The hold covers the whole page, so held() also counts other panes'
// listings; heldPaths() says which directories the held requests list.
// Requests made after release() pass through and are not counted.
export async function holdListFiles(page: Page) {
  let holding = true;
  let unblock = () => {};
  const released = new Promise<void>((resolve) => (unblock = resolve));
  const release = () => {
    holding = false;
    unblock();
  };

  const paths: string[] = [];
  const continued: Promise<void>[] = [];
  const handler = async (route: Route) => {
    if (!holding)
      return route.fallback();

    paths.push(listFilesPath(route.request()));
    const request = released.then(() => route.continue());
    continued.push(request);
    await request;
  };
  await page.route(LIST_FILES_RPC, handler);

  return {
    held: () => paths.length,
    heldPaths: () => [...paths],
    release,
    cleanup: async () => {
      // let held requests through before removing the route, so an unroute
      // doesn't continue them a second time
      release();
      await Promise.allSettled(continued);
      // by handler: another route on LIST_FILES_RPC stays installed
      await page.unroute(LIST_FILES_RPC, handler);
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
  // navigating clears the filename box (OpenFileDialog.onBrowserNavigated)
  await expect(filename).toHaveValue('', { timeout: 15000 });
  return dialog;
}

// Helpers for driving the web (GWT) file dialogs, which are used in server
// mode; Desktop shows native dialogs.

import { Locator, Page, expect } from '@playwright/test';
import { executeCommand } from '@utils/commands';

export const OPEN_FILE_DIALOG = '.gwt-DialogBox[aria-label="Open File"]';

// The RPC the GWT file dialogs list their directory with.
export const LIST_FILES_RPC = /\/rpc\/list_files(?:\?|$)/;

interface ListFilesHold {
  released: Promise<void>;
  held: number;
  continued: Promise<void>[];
}

interface ListFilesRoute {
  hold: ListFilesHold | null;
}

const listFilesRoutes = new WeakMap<Page, ListFilesRoute>();

// The page's list_files route, installed on first use and never removed.
// Removing a page's last route turns Playwright's request interception off,
// and a request paused during that switch is never continued. That request
// is usually RStudio's get_events long-poll, so the client would stop
// receiving events (console prompts included) for the rest of the page's
// life.
async function listFilesRoute(page: Page): Promise<ListFilesRoute> {
  const existing = listFilesRoutes.get(page);
  if (existing)
    return existing;

  const state: ListFilesRoute = { hold: null };
  await page.route(LIST_FILES_RPC, async (route) => {
    const hold = state.hold;
    if (!hold) {
      await route.fallback();
      return;
    }

    hold.held++;
    const request = hold.released.then(() => route.fallback());
    hold.continued.push(request);
    await request;
  });
  listFilesRoutes.set(page, state);
  return state;
}

// Holds every list_files RPC until release() is called, so a dialog can be
// driven before its directory listing arrives. cleanup() releases anything
// still held and ends the hold; call it from a finally block.
export async function holdListFiles(page: Page) {
  const state = await listFilesRoute(page);

  let release = () => {};
  const hold: ListFilesHold = {
    released: new Promise<void>((resolve) => (release = resolve)),
    held: 0,
    continued: [],
  };
  state.hold = hold;

  return {
    held: () => hold.held,
    release,
    cleanup: async () => {
      release();
      await Promise.allSettled(hold.continued);
      if (state.hold === hold)
        state.hold = null;
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

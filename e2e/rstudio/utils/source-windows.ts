import { expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { TIMEOUTS } from '@utils/constants';

/** Matches the URL of a popped-out source window. */
export const SOURCE_WINDOW_URL = /view=source_window_/;

// How long to keep checking that no source window reopens after a reload. The
// main window reopens them while the workbench initializes, so this only has
// to outlast the delivery of the new window to Playwright.
const REOPEN_WINDOW_MS = 3000;

/** The popped-out source windows open in the main window's context. */
export function sourceWindows(page: Page): Page[] {
  return page.context().pages().filter((p) => SOURCE_WINDOW_URL.test(p.url()));
}

// How long to give a source window to close, and the main window to release
// its documents, before giving up on the orderly path.
const CLOSE_WINDOW_MS = 5000;

/**
 * Close any source windows a test left (or reopened), so they are not handed
 * to the next test.
 *
 * Playwright's plain page.close() skips the page's own teardown, so the main
 * window would never hear that the window is gone, and would reopen it for
 * its documents on the next reload. Close the way the window's close button
 * does instead, and discard any unsaved changes through the prompt. (An
 * evaluate counts as a user gesture, which lets the browser show the prompt;
 * without one the window would hand its unsaved documents to the main window
 * rather than drop them.)
 */
export async function closeSourceWindows(page: Page): Promise<void> {
  for (const satellite of sourceWindows(page)) {
    satellite.on('dialog', (dialog) => {
      dialog.accept().catch(() => {});
    });
    await satellite.evaluate(() => {}).catch(() => {});

    const released = page
      .waitForResponse((response) => response.url().includes('/rpc/close_document'), { timeout: CLOSE_WINDOW_MS })
      .catch(() => undefined);
    const closed = satellite.waitForEvent('close', { timeout: CLOSE_WINDOW_MS }).catch(() => undefined);
    await satellite.close({ runBeforeUnload: true }).catch(() => {});
    await closed;
    if (!satellite.isClosed()) {
      await satellite.close().catch(() => {});
      continue;
    }
    await released;
  }
}

/** Reload the main window and check that it does not reopen a source window. */
export async function expectNoSourceWindowAfterReload(page: Page): Promise<void> {
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, {
    timeout: TIMEOUTS.sessionRestart,
    polling: 50,
  });

  // Nothing reopening is also the starting state, so sample over a window
  // rather than polling for it.
  const deadline = Date.now() + REOPEN_WINDOW_MS;
  while (Date.now() < deadline) {
    expect(sourceWindows(page).map((p) => p.url())).toEqual([]);
    await page.waitForTimeout(TIMEOUTS.layoutSettle);
  }
}

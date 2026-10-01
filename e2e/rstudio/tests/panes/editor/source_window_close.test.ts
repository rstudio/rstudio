// Closing a popped-out source window releases its documents (#18987).
//
// A document shown in its own window records that window's id, and the main
// window reopens every recorded window when it loads. The record is dropped
// (and the document closed) once the satellite tells the main window that it
// is going away. When that notification was lost, a window closed with its own
// close button came back on every reload, and closed windows piled up.
//
// Documents with unsaved changes are not closed: they move back to the main
// window (#19008). On RStudio Server the satellite can only ask the browser to
// prompt, and browsers skip that prompt in a window the user never clicked or
// typed in, so closing those documents would silently discard the edits.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from 'playwright';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS } from '@utils/constants';
import { openFile, seedSandboxFile } from '@utils/files';
import { executeCommand, resetSourcePaneState } from '@utils/commands';

const SELECTED_DOC_TAB = "[class*='rstudio_source_panel'] [class*='PanelTab-selected']";
const DOC_TAB = "[class*='rstudio_source_panel'] .gwt-TabLayoutPanelTab";
const SOURCE_WINDOW_URL = /view=source_window_/;

// How long to keep checking that no source window reopens after a reload. The
// main window reopens them while the workbench initializes, so this only has
// to outlast the delivery of the new window to Playwright.
const REOPEN_WINDOW_MS = 3000;

function sourceWindows(page: Page): Page[] {
  return page.context().pages().filter((p) => SOURCE_WINDOW_URL.test(p.url()));
}

async function popOutActiveDoc(page: Page, caption: string): Promise<Page> {
  await expect(page.locator(SELECTED_DOC_TAB)).toContainText(caption, {
    timeout: TIMEOUTS.fileOpen,
  });

  const detached = page.context().waitForEvent('page');
  await executeCommand(page, 'popoutDoc');
  const satellite = await detached;
  await satellite.waitForURL(SOURCE_WINDOW_URL);

  await expect(satellite.locator(SELECTED_DOC_TAB)).toContainText(caption, {
    timeout: TIMEOUTS.fileOpen,
  });
  return satellite;
}

// Close the window from inside the page, as its close button does, so the
// page runs its own teardown. Playwright's page.close() skips that by default.
async function closeLikeUser(satellite: Page): Promise<void> {
  const closed = satellite.waitForEvent('close');
  await satellite.evaluate(() => {
    setTimeout(() => window.close(), 0);
  });
  await closed;
}

// The main window closes the window's documents once it hears the window is
// gone; on a build that loses the notification this RPC is never sent.
async function closeAndExpectDocumentReleased(page: Page, satellite: Page): Promise<void> {
  const released = page.waitForResponse(
    (response) => response.url().includes('/rpc/close_document'),
    { timeout: TIMEOUTS.consoleReady },
  );
  await closeLikeUser(satellite);

  // A rejected RPC still answers HTTP 200, with an "error" member.
  const response = await released;
  expect(await response.text()).not.toContain('"error"');
}

async function expectNoSourceWindowAfterReload(page: Page): Promise<void> {
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

test.describe('Closing a popped-out source window (#18987)', () => {
  const sandbox = useSuiteSandbox();

  test.afterEach(async ({ rstudioPage: page }) => {
    // A failing run leaves (or reopens) the window; don't hand it to the next test.
    for (const satellite of sourceWindows(page))
      await satellite.close().catch(() => {});
    await resetSourcePaneState(page);
  });

  test('a closed Data Viewer window does not come back on reload', async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    await consoleActions.executeInConsole(
      'source_window_close_df <- data.frame(a = 1); View(source_window_close_df)',
    );

    try {
      const satellite = await popOutActiveDoc(page, 'source_window_close_df');
      await closeAndExpectDocumentReleased(page, satellite);
      await expectNoSourceWindowAfterReload(page);
    } finally {
      await consoleActions.executeInConsole('rm(source_window_close_df)');
    }
  });

  test('a closed editor window does not come back on reload', async ({ rstudioPage: page }) => {
    const fileName = 'source_window_close.R';
    await openFile(page, await seedSandboxFile(page, sandbox.dir, fileName, '# closed\n'));

    const satellite = await popOutActiveDoc(page, fileName);
    await closeAndExpectDocumentReleased(page, satellite);
    await expectNoSourceWindowAfterReload(page);
  });

  test('unsaved changes come back to the main window (#19008)', { tag: ['@server_only'] }, async ({ rstudioPage: page }) => {
    const fileName = 'source_window_close_unsaved.R';
    await openFile(page, await seedSandboxFile(page, sandbox.dir, fileName, '# saved\n'));

    // Edit before popping out, as a user who never touches the new window does.
    await page.evaluate(() => window.rstudio!.documents.activeEditor()!.insert('# unsaved\n'));
    await expect.poll(() => page.evaluate(() => window.rstudio?.documents.active()?.dirty)).toBe(true);

    const satellite = await popOutActiveDoc(page, fileName);

    // Leave the page if the browser asks; the edits must survive either way.
    satellite.on('dialog', (dialog) => dialog.accept());
    await closeLikeUser(satellite);

    const returned = page.locator(DOC_TAB).filter({ hasText: fileName });
    await expect(returned).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await returned.click();

    await expect.poll(() => page.evaluate(() => ({
      dirty: window.rstudio?.documents.active()?.dirty,
      text: window.rstudio?.documents.activeEditor()?.getValue(),
    }))).toEqual({ dirty: true, text: '# unsaved\n# saved\n' });
  });
});

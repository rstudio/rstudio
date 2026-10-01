// Closing a popped-out source window keeps its unsaved documents (#19008).
//
// On RStudio Server a popped-out window with unsaved changes warns through the
// browser's beforeunload prompt. Browsers show that prompt only in a window
// the user has clicked or typed in since it loaded; a window that was popped
// out and then closed without ever being touched just closes, and the main
// window would then close its documents. The window hands its unsaved
// documents back to the main window instead. Desktop is not affected: its
// windows close through RStudio's own Save / Don't Save prompt.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from 'playwright';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS } from '@utils/constants';
import { openFile, seedSandboxFile } from '@utils/files';
import { executeCommand, resetSourcePaneState } from '@utils/commands';

// Playwright's trace snapshots evaluate in every page, and an evaluate counts
// as a user gesture: with tracing on, the popped-out window would get the
// prompt after all and this file would exercise the wrong path.
test.use({ trace: 'off' });

const SELECTED_DOC_TAB = "[class*='rstudio_source_panel'] [class*='PanelTab-selected']";
const DOC_TABS = "[class*='rstudio_source_panel'] [class*='PanelTab']";
const SOURCE_WINDOW_URL = /view=source_window_/;

// How long to keep checking that no source window reopens after a reload (see
// source_window_close.test.ts).
const REOPEN_WINDOW_MS = 3000;

function sourceWindows(page: Page): Page[] {
  return page.context().pages().filter((p) => SOURCE_WINDOW_URL.test(p.url()));
}

test.describe('Closing a popped-out source window without a gesture (#19008)', { tag: ['@server_only'] }, () => {
  const sandbox = useSuiteSandbox();

  test.afterEach(async ({ rstudioPage: page }) => {
    for (const satellite of sourceWindows(page))
      await satellite.close().catch(() => {});
    await resetSourcePaneState(page);
  });

  test('a window closed without a prompt returns its unsaved documents to the main window', async ({
    rstudioPage: page,
  }) => {
    const fileName = 'source_window_close_unsaved.R';
    const edit = '# unsaved edit';
    await openFile(page, await seedSandboxFile(page, sandbox.dir, fileName, '# closed\n'));

    // Edit in the main window, so the popped-out window is never touched.
    const backedUp = page.waitForResponse((response) => response.url().includes('/rpc/save_document_diff'));
    await new SourcePaneActions(page, new ConsolePaneActions(page)).sendText(edit);
    await backedUp;
    await expect(page.locator(SELECTED_DOC_TAB)).toContainText(`${fileName}*`);

    const detached = page.context().waitForEvent('page');
    await executeCommand(page, 'popoutDoc');
    const satellite = await detached;
    await satellite.waitForURL(SOURCE_WINDOW_URL);

    // A prompt means the window was activated after all; record it rather
    // than let Playwright accept it silently.
    const dialogs: string[] = [];
    satellite.on('dialog', (dialog) => {
      dialogs.push(dialog.type());
      void dialog.accept();
    });

    // Wait for the document to show up in the new window through raw CDP;
    // Playwright's evaluate and locators would count as a user gesture.
    const cdp = await page.context().newCDPSession(satellite);
    await expect
      .poll(
        async () => {
          const { result } = await cdp.send('Runtime.evaluate', {
            expression: `Array.from(document.querySelectorAll(${JSON.stringify(DOC_TABS)})).map((e) => e.textContent).join('|')`,
            returnByValue: true,
          });
          return String(result.value ?? '');
        },
        { timeout: TIMEOUTS.fileOpen },
      )
      .toContain(`${fileName}*`);
    await cdp.detach();

    // Close through the page's close path; the browser withholds the prompt.
    const closed = satellite.waitForEvent('close');
    await satellite.close({ runBeforeUnload: true });
    await closed;
    expect(dialogs).toEqual([]);

    // The document is back in the main window, still unsaved.
    const tab = page.locator(DOC_TABS, { hasText: fileName }).first();
    await expect(tab).toContainText(`${fileName}*`, { timeout: TIMEOUTS.fileOpen });
    await tab.click();
    await expect(page.locator(SELECTED_DOC_TAB)).toContainText(fileName);
    expect(await new SourcePaneActions(page, new ConsolePaneActions(page)).getEditorContent()).toContain(edit);

    // And it stays there: no window to reopen, and the edit survives a reload.
    await page.reload();
    await page.waitForFunction(() => window.rstudio?.ready === true, null, {
      timeout: TIMEOUTS.sessionRestart,
      polling: 50,
    });
    const deadline = Date.now() + REOPEN_WINDOW_MS;
    while (Date.now() < deadline) {
      expect(sourceWindows(page).map((p) => p.url())).toEqual([]);
      await page.waitForTimeout(TIMEOUTS.layoutSettle);
    }
    await expect(page.locator(DOC_TABS, { hasText: fileName }).first()).toContainText(`${fileName}*`, {
      timeout: TIMEOUTS.fileOpen,
    });
  });
});

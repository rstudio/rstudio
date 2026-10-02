// Closing a popped-out source window keeps its unsaved documents (#19008).
//
// On RStudio Server a popped-out window with unsaved changes warns through the
// browser's beforeunload prompt. Browsers show that prompt only in a window
// the user has clicked or typed in since it loaded; a window that was popped
// out and then closed without ever being touched just closes, and the main
// window would then close its documents. Such a window reports its unsaved
// documents to the main window as it unloads, and the main window keeps them
// open once it sees the window is really gone (and not merely reloading).
// Desktop is not affected: its windows close through RStudio's own
// Save / Don't Save prompt.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Locator, Page } from 'playwright';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS } from '@utils/constants';
import { openFile, seedSandboxFile } from '@utils/files';
import { executeCommand, resetSourcePaneState } from '@utils/commands';
import { SOURCE_WINDOW_URL, closeSourceWindows, expectNoSourceWindowAfterReload } from '@utils/source-windows';

// Trace snapshots run scripts in every page, and a script run by Playwright
// counts as a user gesture: with snapshots on, the popped-out window would
// get the prompt after all and this file would exercise the wrong path. The
// rest of the trace (actions, network, screenshots) is still worth keeping.
test.use({ trace: { mode: 'retain-on-failure', snapshots: false } });

const SELECTED_DOC_TAB = "[class*='rstudio_source_panel'] [class*='PanelTab-selected']";
const DOC_TABS = "[class*='rstudio_source_panel'] [class*='PanelTab']";

// The main window decides that an unloaded source window has reloaded rather
// than closed once it has seen the window still open for 5 seconds
// (WindowCloseMonitor); only then is it sure not to take the documents.
const RELOAD_DECISION_MS = 6000;

// Open a file, edit it in the main window so the popped-out window is never
// touched, and pop it out. Returns the new window once it shows the document.
async function popOutUnsavedDoc(page: Page, sandboxDir: string, fileName: string, edit: string): Promise<Page> {
  await openFile(page, await seedSandboxFile(page, sandboxDir, fileName, '# closed\n'));

  const backedUp = page.waitForResponse((response) => response.url().includes('/rpc/save_document_diff'));
  await new SourcePaneActions(page, new ConsolePaneActions(page)).sendText(edit);
  await backedUp;
  await expect(page.locator(SELECTED_DOC_TAB)).toContainText(`${fileName}*`);

  const detached = page.context().waitForEvent('page');
  await executeCommand(page, 'popoutDoc');
  const satellite = await detached;
  await satellite.waitForURL(SOURCE_WINDOW_URL);
  await expectSatelliteTabs(page, satellite, `${fileName}*`);
  return satellite;
}

// Wait for the popped-out window's document tabs to include `text`, through
// raw CDP: Playwright's evaluate and locators would count as a user gesture.
async function expectSatelliteTabs(page: Page, satellite: Page, text: string): Promise<void> {
  const cdp = await page.context().newCDPSession(satellite);
  try {
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
      .toContain(text);
  } finally {
    await cdp.detach();
  }
}

// A prompt means the window was activated after all; record it rather than
// let Playwright accept it silently.
function recordDialogs(satellite: Page): string[] {
  const dialogs: string[] = [];
  satellite.on('dialog', (dialog) => {
    dialogs.push(dialog.type());
    dialog.accept().catch(() => {});
  });
  return dialogs;
}

// Close through the page's close path; the browser withholds the prompt from
// a window without a gesture, and `dialogs` confirms it did.
async function closeWithoutPrompt(satellite: Page, dialogs: string[]): Promise<void> {
  const closed = satellite.waitForEvent('close');
  await satellite.close({ runBeforeUnload: true });
  await closed;
  expect(dialogs).toEqual([]);
}

async function expectUnsavedTabInMainWindow(page: Page, fileName: string): Promise<Locator> {
  const tab = page.locator(DOC_TABS, { hasText: fileName }).first();
  await expect(tab).toContainText(`${fileName}*`, { timeout: TIMEOUTS.fileOpen });
  return tab;
}

test.describe('Unloading a popped-out source window without a gesture (#19008)', { tag: ['@server_only'] }, () => {
  const sandbox = useSuiteSandbox();

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeSourceWindows(page);
    await resetSourcePaneState(page);
  });

  test('a window closed without a prompt returns its unsaved documents to the main window', async ({
    rstudioPage: page,
  }) => {
    const fileName = 'source_window_close_unsaved.R';
    const edit = '# unsaved edit';
    const satellite = await popOutUnsavedDoc(page, sandbox.dir, fileName, edit);
    const dialogs = recordDialogs(satellite);

    await closeWithoutPrompt(satellite, dialogs);

    // The document is back in the main window, still unsaved.
    const tab = await expectUnsavedTabInMainWindow(page, fileName);
    await tab.click();
    await expect(page.locator(SELECTED_DOC_TAB)).toContainText(fileName);
    expect(await new SourcePaneActions(page, new ConsolePaneActions(page)).getEditorContent()).toContain(edit);

    // And it stays there: no window to reopen, and the edit survives a reload.
    await expectNoSourceWindowAfterReload(page);
    await expect(page.locator(DOC_TABS, { hasText: fileName }).first()).toContainText(`${fileName}*`, {
      timeout: TIMEOUTS.fileOpen,
    });
  });

  test('a window reloaded without a prompt keeps its unsaved documents', async ({ rstudioPage: page }) => {
    const fileName = 'source_window_reload_unsaved.R';
    const satellite = await popOutUnsavedDoc(page, sandbox.dir, fileName, '# unsaved edit');
    const dialogs = recordDialogs(satellite);

    // A reload unloads the page just like a close does; the window reports
    // its unsaved document either way, and the main window must notice that
    // this window came back.
    await satellite.reload();
    await expectSatelliteTabs(page, satellite, `${fileName}*`);
    expect(dialogs).toEqual([]);

    // The document must not also show up in the main window.
    const deadline = Date.now() + RELOAD_DECISION_MS;
    while (Date.now() < deadline) {
      await expect(page.locator(DOC_TABS, { hasText: fileName })).toHaveCount(0);
      await page.waitForTimeout(TIMEOUTS.layoutSettle);
    }
    await expectSatelliteTabs(page, satellite, `${fileName}*`);

    // Closing the reloaded window still hands the document over.
    await closeWithoutPrompt(satellite, dialogs);
    await expectUnsavedTabInMainWindow(page, fileName);
  });

  test('a window reloaded without a prompt and then closed through the prompt discards its documents', async ({
    rstudioPage: page,
  }) => {
    const fileName = 'source_window_reload_then_leave.R';
    const satellite = await popOutUnsavedDoc(page, sandbox.dir, fileName, '# unsaved edit');
    const dialogs = recordDialogs(satellite);

    // The reload reports the unsaved document, and the main window holds on
    // to that report until it is sure the window is gone.
    await satellite.reload();
    await expectSatelliteTabs(page, satellite, `${fileName}*`);
    expect(dialogs).toEqual([]);

    // Before the main window has decided, click into the window (an evaluate
    // counts as a gesture) and close it, choosing Leave at the prompt. The
    // report left by the reload must not make the main window keep a
    // document the user just chose to discard.
    await satellite.evaluate(() => {});
    const released = page.waitForResponse((response) => response.url().includes('/rpc/close_document'), {
      timeout: RELOAD_DECISION_MS,
    });
    const closed = satellite.waitForEvent('close');
    await satellite.close({ runBeforeUnload: true });
    await closed;
    expect(dialogs).toEqual(['beforeunload']);
    await released;

    // The document is closed, not adopted by the main window.
    const deadline = Date.now() + RELOAD_DECISION_MS;
    while (Date.now() < deadline) {
      await expect(page.locator(DOC_TABS, { hasText: fileName })).toHaveCount(0);
      await page.waitForTimeout(TIMEOUTS.layoutSettle);
    }
  });
});

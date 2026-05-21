// Regression test for https://github.com/rstudio/rstudio/issues/17701
//
// rstudioapi::showDialog() and rstudioapi::showPrompt() silently dropped
// newline characters from the `message` argument. The fix applies
// `white-space: pre-line` to the GWT widgets so caller-supplied "\n"
// characters render as line breaks.
//
// Only the GWT-rendered paths are covered here:
//   - showDialog    -- GWT MessageDialog on both Desktop and Server
//   - showPrompt    -- GWT TextEntryModalDialog on Electron Desktop and Server
//   - showQuestion  -- GWT MessageDialog on Server only (Desktop uses Electron's
//                      native dialog, which is outside Playwright's reach). This
//                      path wasn't broken in #17701, but the @server_only test
//                      guards against future regressions.
//
// The R-side calls block until the dialog is dismissed, so each test must
// click OK/Cancel before issuing the next console command.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';

const DIALOG_OK = '#rstudio_dlg_ok';
const DIALOG_CANCEL = '#rstudio_dlg_cancel';

test.describe('rstudioapi dialog newline handling (#17701)', { tag: ['@parallel_safe'] }, () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.clearConsole();
  });

  // If a test fails with the dialog still up, R stays blocked on
  // showDialogCompleted -- fixture shutdown then triggers the "R session is
  // busy" quit prompt. Force the dialog closed so cleanup is reliable.
  test.afterEach(async ({ rstudioPage: page }) => {
    const cancel = page.locator(`.gwt-DialogBox ${DIALOG_CANCEL}`);
    const ok = page.locator(`.gwt-DialogBox ${DIALOG_OK}`);
    if (await cancel.first().isVisible().catch(() => false)) {
      await cancel.first().click();
    } else if (await ok.first().isVisible().catch(() => false)) {
      await ok.first().click();
    }
  });

  test('showDialog preserves "\\n" as a line break', async ({ rstudioPage: page }) => {
    const title = `showDialog_17701_${Date.now()}`;

    // Calling .rs.api.showDialog blocks the R thread until the client fires
    // showDialogCompleted, so this returns immediately after pressing Enter.
    await consoleActions.executeInConsole(
      `.rs.api.showDialog("${title}", "line one\\nline two")`,
    );

    const dialog = page.locator(`.gwt-DialogBox[aria-label="${title}"]`);
    await expect(dialog).toBeVisible({ timeout: 10000 });

    // GWT HTML widget renders the message in a div.gwt-HTML inside the
    // dialog. With white-space: pre-line applied, innerText reflects the
    // literal "\n" as a rendered newline.
    const messageText = await dialog.locator('.gwt-HTML').innerText();
    expect(messageText).toMatch(/line one\s*\n\s*line two/);

    await dialog.locator(DIALOG_OK).click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });

  test('showPrompt preserves "\\n" as a line break', async ({ rstudioPage: page }) => {
    const title = `showPrompt_17701_${Date.now()}`;

    await consoleActions.executeInConsole(
      `.rs.api.showPrompt("${title}", "line one\\nline two")`,
    );

    const dialog = page.locator(`.gwt-DialogBox[aria-label="${title}"]`);
    await expect(dialog).toBeVisible({ timeout: 10000 });

    // The prompt caption is a FormLabel — the only <label> in the dialog.
    const messageText = await dialog.locator('label').first().innerText();
    expect(messageText).toMatch(/line one\s*\n\s*line two/);

    // Cancel so R's showPrompt returns NULL and the session goes idle.
    await dialog.locator(DIALOG_CANCEL).click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });

  test('showQuestion preserves "\\n" as a line break', { tag: ['@server_only'] }, async ({ rstudioPage: page }) => {
    const title = `showQuestion_17701_${Date.now()}`;

    await consoleActions.executeInConsole(
      `.rs.api.showQuestion("${title}", "line one\\nline two")`,
    );

    const dialog = page.locator(`.gwt-DialogBox[aria-label="${title}"]`);
    await expect(dialog).toBeVisible({ timeout: 10000 });

    // MessageDialog renders the message through MultiLineLabel, which uses
    // DomUtils.textToHtml to convert "\n" into <br/> tags. innerText then
    // reports the rendered newline.
    const messageText = await dialog.locator('.gwt-Label').first().innerText();
    expect(messageText).toMatch(/line one\s*\n\s*line two/);

    await dialog.locator(DIALOG_CANCEL).click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });
});

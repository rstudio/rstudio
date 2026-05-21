// Regression test for https://github.com/rstudio/rstudio/issues/17701
//
// rstudioapi::showDialog() and rstudioapi::showPrompt() silently dropped
// newline characters from the `message` argument. The fix applies
// `white-space: pre-line` to the GWT widgets so caller-supplied "\n"
// characters render as line breaks.
//
// All three calls render through GWT dialogs in the test suite. Desktop
// would normally use Electron's native showMessageBox for showQuestion,
// but fixtures/base-prefs.jsonc sets native_file_dialogs=false so message
// dialogs are GWT too (Playwright can't reach native dialogs). The
// showQuestion case wasn't broken in #17701; the test guards against
// future regressions in the MultiLineLabel rendering path.
//
// The R-side calls block until the dialog is dismissed, so each test must
// click OK/Cancel before issuing the next console command.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';

const DIALOG_OK = '#rstudio_dlg_ok';
const DIALOG_CANCEL = '#rstudio_dlg_cancel';
// showQuestion uses showYesNoMessage, which assigns the yes/no element IDs
// regardless of the caller-supplied labels ("OK"/"Cancel").
const DIALOG_NO = '#rstudio_dlg_no';

// Ordered from least-destructive (Cancel/No) to most (OK), used by the
// afterEach cleanup so a stuck dialog can be dismissed without committing
// the action.
const DISMISS_SELECTORS = [DIALOG_CANCEL, DIALOG_NO, DIALOG_OK];

// Assert that messageText contains the expected lines as distinct rendered
// rows (split on "\n", trimmed, non-empty). Stronger than a regex with \s*
// across the newline because it requires the newline character to actually
// separate the two lines in the rendered output.
function expectRenderedLines(messageText: string, expected: string[]): void {
  const lines = messageText.split('\n').map((s) => s.trim()).filter(Boolean);
  expect(lines).toEqual(expect.arrayContaining(expected));
  expect(lines.length).toBeGreaterThanOrEqual(expected.length);
}

test.describe('rstudioapi dialog newline handling (#17701)', { tag: ['@parallel_safe'] }, () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.clearConsole();
  });

  // If a test fails with the dialog still up, R stays blocked on
  // showDialogCompleted -- fixture shutdown then triggers the "R session is
  // busy" quit prompt. Drain every visible dismiss button (not just the
  // first) in case multiple modals stacked up, then fall back to Escape so
  // a dialog with unexpected selectors still gets dismissed.
  test.afterEach(async ({ rstudioPage: page }) => {
    let dismissed = false;
    for (const sel of DISMISS_SELECTORS) {
      const btn = page.locator(`.gwt-DialogBox ${sel}`).first();
      while (await btn.isVisible().catch(() => false)) {
        await btn.click();
        dismissed = true;
      }
    }
    if (!dismissed && await page.locator('.gwt-DialogBox').first().isVisible().catch(() => false)) {
      await page.keyboard.press('Escape');
    }
  });

  test('showDialog preserves "\\n" as a line break', async ({ rstudioPage: page }) => {
    // Calling .rs.api.showDialog blocks the R thread until the client fires
    // showDialogCompleted, so this returns immediately after pressing Enter.
    await consoleActions.executeInConsole(
      `.rs.api.showDialog("showDialog_17701", "line one\\nline two")`,
    );

    const dialog = page.locator('.gwt-DialogBox[aria-label="showDialog_17701"]');
    await expect(dialog).toBeVisible({ timeout: 10000 });

    // GWT HTML widget renders the message in a div.gwt-HTML inside the
    // dialog. With white-space: pre-line applied, innerText reflects the
    // literal "\n" as a rendered newline.
    const messageText = await dialog.locator('.gwt-HTML').innerText();
    expectRenderedLines(messageText, ['line one', 'line two']);

    await dialog.locator(DIALOG_OK).click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });

  test('showDialog renders HTML alongside newlines', async ({ rstudioPage: page }) => {
    // pre-line preserves "\n" while still honoring HTML tags. Verifies that
    // existing showDialog callers passing markup (e.g. <b>, links) keep
    // working after the fix.
    await consoleActions.executeInConsole(
      `.rs.api.showDialog("showDialog_17701_html", "<b>line one</b>\\nline two")`,
    );

    const dialog = page.locator('.gwt-DialogBox[aria-label="showDialog_17701_html"]');
    await expect(dialog).toBeVisible({ timeout: 10000 });

    const messageBody = dialog.locator('.gwt-HTML');
    const messageText = await messageBody.innerText();
    expectRenderedLines(messageText, ['line one', 'line two']);
    // Literal markup must not bleed into rendered text.
    expect(messageText).not.toContain('<b>');
    // Bold element must actually be a node, not just text.
    await expect(messageBody.locator('b')).toContainText('line one');

    await dialog.locator(DIALOG_OK).click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });

  test('showDialog preserves consecutive newlines', async ({ rstudioPage: page }) => {
    // pre-line collapses consecutive spaces but not consecutive newlines,
    // so a blank line between two text lines should survive.
    await consoleActions.executeInConsole(
      `.rs.api.showDialog("showDialog_17701_blank", "line one\\n\\nline two")`,
    );

    const dialog = page.locator('.gwt-DialogBox[aria-label="showDialog_17701_blank"]');
    await expect(dialog).toBeVisible({ timeout: 10000 });

    const messageText = await dialog.locator('.gwt-HTML').innerText();
    // Two newlines must remain in the rendered text.
    expect(messageText).toMatch(/line one\n\s*\n\s*line two/);

    await dialog.locator(DIALOG_OK).click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });

  test('showPrompt preserves "\\n" as a line break', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      `.rs.api.showPrompt("showPrompt_17701", "line one\\nline two")`,
    );

    const dialog = page.locator('.gwt-DialogBox[aria-label="showPrompt_17701"]');
    await expect(dialog).toBeVisible({ timeout: 10000 });

    // The prompt caption is a FormLabel — the only <label> in the dialog.
    const messageText = await dialog.locator('label').first().innerText();
    expectRenderedLines(messageText, ['line one', 'line two']);

    // Cancel so R's showPrompt returns NULL and the session goes idle.
    await dialog.locator(DIALOG_CANCEL).click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });

  test('showQuestion preserves "\\n" as a line break', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      `.rs.api.showQuestion("showQuestion_17701", "line one\\nline two")`,
    );

    const dialog = page.locator('.gwt-DialogBox[aria-label="showQuestion_17701"]');
    await expect(dialog).toBeVisible({ timeout: 10000 });

    // MessageDialog renders the message through MultiLineLabel, which uses
    // DomUtils.textToHtml to convert "\n" into <br/> tags. The label's CSS
    // class is GWT-obfuscated (setStylePrimaryName replaces gwt-Label with
    // the themeStyles.dialogMessage() class), so read the whole-dialog
    // innerText -- the buttons ("OK", "Cancel") don't introduce extra lines
    // that match the expected text.
    const messageText = await dialog.innerText();
    expectRenderedLines(messageText, ['line one', 'line two']);

    // showYesNoMessage assigns dlg_no to the noLabel button (here labeled
    // "Cancel"); dlg_cancel does not exist on this dialog.
    await dialog.locator(DIALOG_NO).click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });
});

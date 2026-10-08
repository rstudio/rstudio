import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { clearPref, setPref, waitForSourcePaneReset } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { TIMEOUTS } from '@utils/constants';
import { useSuiteSandbox } from '@utils/sandbox';

// Diagnostic messages are displayed as text, not markup (rstudio-pro#12202).
//
// R diagnostics quote identifiers from the file into their messages, so a
// message can contain tag-like text. It must reach the display verbatim.

const FILE = 'diagnostics_message_rendering.R';
const BACKTICK = '`';
const MARKUP = '<b>markup</b>';
const DIAGNOSTICS_POPUP = '[id^="rstudio_popup_diagnostics"]';
// intentionally invalid R code to generate a lint assertion
const CONTENT = `nchar(${BACKTICK}${MARKUP}${BACKTICK} = 1)\n`;

test.describe('Diagnostic message rendering - rstudio-pro#12202', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    await setPref(page, 'show_diagnostics_r', true);
    await setPref(page, 'check_arguments_to_r_function_calls', true);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE]);
    await clearPref(page, 'check_arguments_to_r_function_calls');
    await clearPref(page, 'show_diagnostics_r');
  });

  test('a message containing tag-like text is displayed literally', async ({ rstudioPage: page }) => {
    await waitForSourcePaneReset(page);
    await writeAndOpenFile(page, sandbox.dir, FILE, CONTENT);

    // Background lint runs on edits while the editor is focused; a no-op edit
    // is the established way to trigger it (see confusable_characters.test.ts).
    // Edit the trailing empty line so the cursor stays off the diagnostic's
    // range, which LintManager would otherwise filter out.
    const editor = new AceEditor(page, '');
    const sourceActions = new SourcePaneActions(page, new ConsolePaneActions(page));
    if (!(await editor.isFocused())) {
      await sourceActions.sourcePane.aceTextInput.click({ force: true });
      await expect.poll(() => editor.isFocused()).toBe(true);
    }
    await editor.gotoLine(CONTENT.split('\n').length, 0);
    await page.keyboard.press('End');
    await page.keyboard.type(' ');
    await page.keyboard.press('Backspace');

    // Gate on the diagnostic existing, so a lint failure reports as such
    // rather than as a timeout waiting for the display.
    await expect
      .poll(
        () =>
          page.evaluate(
            () =>
              window.rstudio?.documents
                .activeEditor()
                ?.session.getAnnotations()
                .find((a) => a.text.includes('unmatched argument'))?.text ?? '',
          ),
        { timeout: TIMEOUTS.fileOpen },
      )
      .toContain(MARKUP);

    // Resting the cursor inside the marked range shows the diagnostic popup.
    await editor.gotoLine(1, 3);

    // Scoped to the popup: the editor renders the source line itself, so a
    // document-wide match would pass even when the popup renders markup.
    const popup = page.locator(DIAGNOSTICS_POPUP);
    await expect(popup).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    // Text, not markup -- parsed tags would leave only "markup" behind.
    await expect(popup).toContainText(MARKUP, { timeout: TIMEOUTS.fileOpen });
  });
});

// Switching the spelling dictionary in one step, with immediate effect
// (#12223). Edit > Change Spelling Language... opens a small picker; applying
// it updates the spelling_dictionary_language pref and the realtime spell
// checker re-checks open documents, so "colour" stops being flagged as soon
// as the dictionary becomes British English -- no restart.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { TIMEOUTS } from '@utils/constants';
import { clearPref, executeCommand, getPref, setPref, waitForSourcePaneReset } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';

const LANGUAGE_SELECT = '#rstudio_change_spelling_language_select';
const DIALOG_OK = '#rstudio_dlg_ok';

// "colour" sits at columns 4-10 of the first line
const CONTENT = 'The colour of the sky.\n';
const WORD_ROW = 0;
const WORD_START = 4;
const WORD_END = 10;

// A realtime spelling marker is a front "text" marker covering exactly the
// word (lint markers are added in front; session.getMarkers() alone would only
// return the back markers, e.g. the selected-word highlight).
async function isWordFlagged(page: Page): Promise<boolean> {
  return page.evaluate(
    (word) => {
      const editor = window.rstudio?.documents.activeEditor();
      if (!editor) return false;
      const markers = Object.values(editor.session.getMarkers(true)) as Array<{
        type: string;
        range?: { start: { row: number; column: number }; end: { row: number; column: number } };
      }>;
      return markers.some(
        (m) =>
          m.type === 'text' &&
          m.range !== undefined &&
          m.range.start.row === word.row &&
          m.range.start.column === word.start &&
          m.range.end.column === word.end,
      );
    },
    { row: WORD_ROW, start: WORD_START, end: WORD_END },
  );
}

// Realtime lint runs on document edits while the editor is focused (and after
// a dictionary change), so focus the editor and make a no-op edit before
// watching the markers.
async function expectWordFlagged(
  page: Page,
  sourceActions: SourcePaneActions,
  editor: AceEditor,
  flagged: boolean,
): Promise<void> {
  if (!(await editor.isFocused())) {
    await sourceActions.sourcePane.aceTextInput.click({ force: true });
    await expect.poll(() => editor.isFocused()).toBe(true);
  }
  await editor.gotoLine(2, 0);
  await page.keyboard.type(' ');
  await page.keyboard.press('Backspace');

  await expect.poll(() => isWordFlagged(page), { timeout: TIMEOUTS.fileOpen }).toBe(flagged);
}

async function changeLanguage(page: Page, langId: string): Promise<void> {
  await executeCommand(page, 'changeSpellingLanguage');
  const dialog = page.getByRole('dialog', { name: 'Change Spelling Language', exact: true });
  await expect(dialog).toBeVisible();
  await dialog.locator(LANGUAGE_SELECT).selectOption(langId);
  await dialog.locator(DIALOG_OK).click();
  await expect(dialog).toBeHidden();
  await expect.poll(() => getPref(page, 'spelling_dictionary_language')).toBe(langId);
}

test.describe.serial('Change Spelling Language', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  const fileName = 'spelling_language_switch.Rmd';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await setPref(page, 'real_time_spellchecking', true);
    await setPref(page, 'spelling_dictionary_language', 'en_US');
  });

  // the per-test fixture resets the source pane, so the document is opened
  // per test rather than once for the suite
  test.beforeEach(async ({ rstudioPage: page }) => {
    await waitForSourcePaneReset(page);
    await writeAndOpenFile(page, sandbox.dir, fileName, CONTENT);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'spelling_dictionary_language');
    await clearPref(page, 'real_time_spellchecking');
  });

  test('a British dictionary un-flags "colour" without a restart', async ({ rstudioPage: page }) => {
    const editor = new AceEditor(page, '');
    await expectWordFlagged(page, sourceActions, editor, true);

    await changeLanguage(page, 'en_GB');
    await expectWordFlagged(page, sourceActions, editor, false);
  });

  test('switching back to American English flags it again', async ({ rstudioPage: page }) => {
    const editor = new AceEditor(page, '');
    await changeLanguage(page, 'en_GB');
    await expectWordFlagged(page, sourceActions, editor, false);

    await changeLanguage(page, 'en_US');
    await expectWordFlagged(page, sourceActions, editor, true);
  });
});

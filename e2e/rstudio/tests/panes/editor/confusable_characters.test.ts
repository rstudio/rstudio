// Diagnostics for characters that look like ASCII but are not (#14485).
//
// ConfusableCharacterLinter adds a warning alongside the R diagnostics for
// e.g. a Cyrillic "c" in code, naming the codepoint and the ASCII lookalike.
// Strings and comments are exempt. Gated by the warn_confusable_characters
// pref.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { TIMEOUTS } from '@utils/constants';
import { clearPref, setPref, waitForSourcePaneReset } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';

// U+0441 CYRILLIC SMALL LETTER ES, visually identical to "c"
const CYRILLIC_C = 'с';

// row 0: in code (flagged); row 1: in a string and a comment (exempt);
// row 2: a genuine Cyrillic identifier, which also contains letters with no
// ASCII lookalike (exempt)
const CYRILLIC_WORD = 'сумма';
const CONTENT = `x <- ${CYRILLIC_C}(1, 2, 3)\ny <- "${CYRILLIC_C}" # ${CYRILLIC_C}\n${CYRILLIC_WORD} <- 1\n`;
const EXPECTED_MESSAGE = "Non-ASCII character U+0441 looks like 'c'";

interface AceAnnotation {
  row: number;
  column: number;
  text: string;
  type: string;
}

async function getAnnotations(page: Page): Promise<AceAnnotation[]> {
  return page.evaluate(() => {
    const editor = window.rstudio?.documents.activeEditor();
    if (!editor) return [];
    return (editor.session.getAnnotations() as AceAnnotation[]).map((a) => ({
      row: a.row,
      column: a.column,
      text: a.text,
      type: a.type,
    }));
  });
}

function confusableAnnotations(annotations: AceAnnotation[]): AceAnnotation[] {
  return annotations.filter((a) => a.text.includes(EXPECTED_MESSAGE));
}

// Background lint runs on document edits while the editor is focused, so
// focus the editor and make a no-op edit before watching the annotations.
async function relint(page: Page, sourceActions: SourcePaneActions, editor: AceEditor): Promise<void> {
  if (!(await editor.isFocused())) {
    await sourceActions.sourcePane.aceTextInput.click({ force: true });
    await expect.poll(() => editor.isFocused()).toBe(true);
  }
  await editor.gotoLine(4, 0);
  await page.keyboard.type(' ');
  await page.keyboard.press('Backspace');
}

test.describe.serial('Confusable character diagnostics', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  const fileName = 'confusable_characters.R';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await setPref(page, 'show_diagnostics_r', true);
    await setPref(page, 'warn_confusable_characters', true);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await waitForSourcePaneReset(page);
    await writeAndOpenFile(page, sandbox.dir, fileName, CONTENT);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'warn_confusable_characters');
    await clearPref(page, 'show_diagnostics_r');
  });

  test('a lone Cyrillic letter in code is flagged; strings, comments and Cyrillic words are not', async ({
    rstudioPage: page,
  }) => {
    const editor = new AceEditor(page, '');
    await relint(page, sourceActions, editor);

    let flagged: AceAnnotation[] = [];
    await expect
      .poll(
        async () => {
          flagged = confusableAnnotations(await getAnnotations(page));
          return flagged.length;
        },
        { timeout: TIMEOUTS.fileOpen },
      )
      .toBe(1);

    // row 0, right after "x <- "
    expect(flagged[0].row).toBe(0);
    expect(flagged[0].column).toBe(5);
    expect(flagged[0].type).toBe('warning');
  });

  test('the pref turns the warning off', async ({ rstudioPage: page }) => {
    const editor = new AceEditor(page, '');
    await relint(page, sourceActions, editor);
    await expect
      .poll(async () => confusableAnnotations(await getAnnotations(page)).length, {
        timeout: TIMEOUTS.fileOpen,
      })
      .toBe(1);

    await setPref(page, 'warn_confusable_characters', false);
    try {
      await relint(page, sourceActions, editor);
      await expect
        .poll(async () => confusableAnnotations(await getAnnotations(page)).length, {
          timeout: TIMEOUTS.fileOpen,
        })
        .toBe(0);
    } finally {
      await setPref(page, 'warn_confusable_characters', true);
    }
  });
});

// Diagnostics for characters that look like ASCII but are not (#14485).
//
// ConfusableCharacterLinter adds a warning alongside the R diagnostics for
// e.g. a Cyrillic "c" in code, naming the codepoint and the ASCII lookalike.
// Strings, comments (roxygen included) and, in R Markdown, everything outside
// R chunks are exempt. Gated by the warn_confusable_characters pref.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import type { Ace } from '@utils/ace';
import { TIMEOUTS } from '@utils/constants';
import { clearPref, setPref, waitForSourcePaneReset } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { useSuiteSandbox } from '@utils/sandbox';

// U+0441 CYRILLIC SMALL LETTER ES, visually identical to "c"
const CYRILLIC_C = 'с';
// U+2013 EN DASH, a lookalike for "-"
const EN_DASH = '–';
// a genuine Cyrillic word: also contains letters with no ASCII lookalike
const CYRILLIC_WORD = 'сумма';

const R_FILE = 'confusable_characters.R';
const RMD_FILE = 'confusable_characters.Rmd';

// the lone Cyrillic letter in code, flagged in both documents
const R_CODE_LINE = `x <- ${CYRILLIC_C}(1, 2, 3)`;

// row 0: formatted roxygen prose (exempt; tokenized as constant.numeric, not
// comment); row 1: in code (flagged); row 2: in a string and a comment
// (exempt); row 3: a genuine Cyrillic identifier (exempt)
const R_CONTENT = heredoc`
  #' Use **left${EN_DASH}right** intervals.
  ${R_CODE_LINE}
  y <- "${CYRILLIC_C}" # ${CYRILLIC_C}
  ${CYRILLIC_WORD} <- 1
`;

// only the R chunk body is R code: the prose, the asis chunk and the python
// chunk all contain lookalikes that must not be flagged
const FENCE = '`'.repeat(3);
const RMD_CONTENT = heredoc`
  ---
  title: "Confusables"
  ---

  Prose with a Cyrillic ${CYRILLIC_C} and an en${EN_DASH}dash.

  ${FENCE}{r}
  ${R_CODE_LINE}
  ${FENCE}

  ${FENCE}{asis}
  An asis chunk with a Cyrillic ${CYRILLIC_C} and an en${EN_DASH}dash.
  ${FENCE}

  ${FENCE}{python}
  y = ${CYRILLIC_C}(1)
  print(y ${EN_DASH} 1)
  ${FENCE}
`;
const RMD_R_CODE_ROW = RMD_CONTENT.split('\n').indexOf(R_CODE_LINE);

const MESSAGE_PREFIX = 'Non-ASCII character';
const EXPECTED_MESSAGE = `${MESSAGE_PREFIX} U+0441 looks like 'c'`;

async function getConfusableAnnotations(page: Page): Promise<Ace.Annotation[]> {
  const annotations = await page.evaluate(() => {
    const editor = window.rstudio?.documents.activeEditor();
    if (!editor) return [];
    return editor.session.getAnnotations().map((a) => ({
      row: a.row,
      column: a.column,
      text: a.text,
      type: a.type,
    }));
  });
  return annotations.filter((a) => a.text.includes(MESSAGE_PREFIX));
}

async function pollConfusables(page: Page, expectedCount: number): Promise<Ace.Annotation[]> {
  let flagged: Ace.Annotation[] = [];
  await expect
    .poll(
      async () => {
        flagged = await getConfusableAnnotations(page);
        return flagged.length;
      },
      { timeout: TIMEOUTS.fileOpen },
    )
    .toBe(expectedCount);
  return flagged;
}

// Background lint runs on document edits while the editor is focused, so
// focus the editor and make a no-op edit at the end of the last line before
// watching the annotations.
async function relint(page: Page, sourceActions: SourcePaneActions, editor: AceEditor, content: string): Promise<void> {
  if (!(await editor.isFocused())) {
    await sourceActions.sourcePane.aceTextInput.click({ force: true });
    await expect.poll(() => editor.isFocused()).toBe(true);
  }
  await editor.gotoLine(content.split('\n').length, 0);
  await page.keyboard.press('End');
  await page.keyboard.type(' ');
  await page.keyboard.press('Backspace');
}

test.describe.serial('Confusable character diagnostics', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await setPref(page, 'show_diagnostics_r', true);
    await setPref(page, 'warn_confusable_characters', true);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await waitForSourcePaneReset(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [R_FILE, RMD_FILE]);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'warn_confusable_characters');
    await clearPref(page, 'show_diagnostics_r');
  });

  test('a lone Cyrillic letter in code is flagged; roxygen, strings, comments and Cyrillic words are not', async ({
    rstudioPage: page,
  }) => {
    await writeAndOpenFile(page, sandbox.dir, R_FILE, R_CONTENT);
    const editor = new AceEditor(page, '');
    await relint(page, sourceActions, editor, R_CONTENT);

    const flagged = await pollConfusables(page, 1);

    // row 1, right after "x <- "
    expect(flagged[0].row).toBe(1);
    expect(flagged[0].column).toBe(5);
    expect(flagged[0].text).toBe(EXPECTED_MESSAGE);
    expect(flagged[0].type).toBe('warning');
  });

  test('in R Markdown, only R chunk bodies are checked', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, RMD_FILE, RMD_CONTENT);
    const editor = new AceEditor(page, '');
    await relint(page, sourceActions, editor, RMD_CONTENT);

    const flagged = await pollConfusables(page, 1);

    expect(flagged[0].row).toBe(RMD_R_CODE_ROW);
    expect(flagged[0].column).toBe(5);
    expect(flagged[0].text).toBe(EXPECTED_MESSAGE);
  });

  test('the pref turns the warning off', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, R_FILE, R_CONTENT);
    const editor = new AceEditor(page, '');
    await relint(page, sourceActions, editor, R_CONTENT);
    await pollConfusables(page, 1);

    await setPref(page, 'warn_confusable_characters', false);
    try {
      await relint(page, sourceActions, editor, R_CONTENT);
      await pollConfusables(page, 0);
    } finally {
      await setPref(page, 'warn_confusable_characters', true);
    }
  });
});

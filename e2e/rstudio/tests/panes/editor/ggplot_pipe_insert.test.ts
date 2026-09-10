// Insert Pipe Operator inserts '+' inside a ggplot2 chain (#15261).
//
// AceEditor.insertPipeOperator() walks the current statement backwards from
// the cursor, at the cursor's own nesting level, and inserts '+' instead of a
// pipe when it finds a ggplot2-style call there. The behavior is governed by
// the insert_plus_in_ggplot_chains pref.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { executeCommand, setPref, clearPref } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const PLUS = ' + ';
const PIPE = ' |> ';

interface PipeCase {
  name: string;
  content: string;
  expected: string;
}

const CASES: PipeCase[] = [
  {
    name: 'after a ggplot layer',
    content: 'ggplot(mtcars, aes(wt, mpg)) + geom_point()',
    expected: PLUS,
  },
  {
    name: 'after a namespaced ggplot call',
    content: 'ggplot2::ggplot(mtcars, aes(wt, mpg))',
    expected: PLUS,
  },
  {
    name: 'on a continuation line after a trailing +',
    content: 'p <- ggplot(mtcars, aes(wt, mpg)) +\n  geom_point()',
    expected: PLUS,
  },
  {
    name: 'on a continuation line with a leading +',
    content: 'ggplot(mtcars, aes(wt, mpg))\n  + geom_point()',
    expected: PLUS,
  },
  {
    name: 'in a plain dplyr chain',
    content: 'mtcars |> dplyr::filter(cyl == 4)',
    expected: PIPE,
  },
  {
    name: "inside a layer's argument list",
    content: 'ggplot(mtcars) + geom_point(data = mtcars',
    expected: PIPE,
  },
  {
    name: 'on a new statement after a ggplot statement',
    content: 'p <- ggplot(mtcars)\nmtcars',
    expected: PIPE,
  },
  {
    name: 'after a call that wraps a ggplot chain',
    content: 'ggplotly(ggplot(mtcars) + geom_point())',
    expected: PIPE,
  },
  {
    name: 'after a global theme helper',
    content: 'theme_set(theme_bw())',
    expected: PIPE,
  },
];

// Loads `content` into the active editor, puts the cursor at its end, runs
// Insert Pipe Operator and returns what got inserted.
async function insertPipeAtEnd(page: Page, editor: AceEditor, content: string): Promise<string> {
  await editor.setValue(content);
  const lines = content.split('\n');
  await editor.gotoLine(lines.length, lines[lines.length - 1].length);
  await expect.poll(() => editor.getValue()).toBe(content);

  await executeCommand(page, 'insertPipeOperator');

  let value = '';
  await expect
    .poll(async () => {
      value = await editor.getValue();
      return value;
    })
    .not.toBe(content);
  return value.slice(content.length);
}

test.describe('Insert Pipe Operator in ggplot2 chains', () => {
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await setPref(page, 'insert_native_pipe_operator', true);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    // a single Untitled R script, focused so the command reaches its editor
    await consoleActions.resetSourcePane();
    await sourceActions.sourcePane.aceTextInput.click({ force: true });
    await expect
      .poll(() => new AceEditor(page, '').isFocused(), { timeout: TIMEOUTS.fileOpen })
      .toBe(true);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'insert_native_pipe_operator');
    await clearPref(page, 'insert_plus_in_ggplot_chains');
    await consoleActions.resetSourcePane();
  });

  for (const c of CASES) {
    test(`inserts '${c.expected.trim()}' ${c.name}`, async ({ rstudioPage: page }) => {
      const editor = new AceEditor(page, '');
      expect(await insertPipeAtEnd(page, editor, c.content)).toBe(c.expected);
    });
  }

  test('inserts a pipe in a ggplot chain when the pref is off', async ({ rstudioPage: page }) => {
    await setPref(page, 'insert_plus_in_ggplot_chains', false);
    try {
      const editor = new AceEditor(page, '');
      expect(await insertPipeAtEnd(page, editor, CASES[0].content)).toBe(PIPE);
    } finally {
      await clearPref(page, 'insert_plus_in_ggplot_chains');
    }
  });
});

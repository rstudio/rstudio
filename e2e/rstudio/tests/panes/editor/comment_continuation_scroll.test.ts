// With "Continue comments on new lines" enabled, Return on a comment line is
// handled by RStudio rather than by Ace's insertstring command, so RStudio has
// to scroll the new line into view itself. It used to do so by jumping the
// cursor to near the top of the viewport (#19185); it should scroll by one
// row, exactly as a plain Return does.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { clearPref, setPref, waitForSourcePaneReset } from '@utils/commands';

interface Viewport {
  firstRow: number;
  firstFullyVisibleRow: number;
  lastFullyVisibleRow: number;
  cursorRow: number;
}

async function readViewport(page: Page): Promise<Viewport> {
  return page.evaluate(() => {
    const editor = window.rstudio!.documents.activeEditor()!;
    return {
      firstRow: editor.getFirstVisibleRow(),
      firstFullyVisibleRow: editor.renderer.getFirstFullyVisibleRow(),
      lastFullyVisibleRow: editor.renderer.getLastFullyVisibleRow(),
      cursorRow: editor.getCursorPosition().row,
    };
  });
}

// Fills the editor, scrolls into the middle of it, and parks the cursor at the
// end of `lineText` placed on the last fully visible row, so that a new line
// below the cursor is off screen until something scrolls.
async function parkCursorOnLastVisibleRow(page: Page, lineText: string): Promise<Viewport> {
  await page.evaluate(() => {
    const editor = window.rstudio!.documents.activeEditor()!;
    editor.setValue(Array.from({ length: 300 }, (_, i) => `x${i + 1} <- ${i + 1}`).join('\n'), -1);
    editor.gotoLine(150, 0, false);
  });
  await expect.poll(() => page.evaluate(() =>
    window.rstudio!.documents.activeEditor()!.getFirstVisibleRow()
  )).toBeGreaterThan(0);

  await page.evaluate((text) => {
    const editor = window.rstudio!.documents.activeEditor()!;
    const row = editor.renderer.getLastFullyVisibleRow();
    const end = editor.session.getLine(row).length;
    editor.session.replace({ start: { row, column: 0 }, end: { row, column: end } }, text);
    editor.selection.setRange({ start: { row, column: text.length }, end: { row, column: text.length } });
    editor.focus();
  }, lineText);

  const viewport = await readViewport(page);
  expect(viewport.cursorRow).toBe(viewport.lastFullyVisibleRow);
  return viewport;
}

async function currentLine(page: Page): Promise<string> {
  return page.evaluate(() => {
    const editor = window.rstudio!.documents.activeEditor()!;
    return editor.session.getLine(editor.getCursorPosition().row);
  });
}

const CASES = [
  { title: 'Return continues a # comment', line: '# test', key: 'Enter', newLine: '# ' },
  { title: "Return continues a #' roxygen comment", line: "#' roxygen", key: 'Enter', newLine: "#' " },
  { title: 'Shift+Return inserts a plain newline', line: '# test', key: 'Shift+Enter', newLine: '' },
];

test.describe.serial('Comment continuation keeps the new line in view', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
    await waitForSourcePaneReset(page);
    await setPref(page, 'continue_comments_on_newline', true);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'continue_comments_on_newline');
    await consoleActions.resetSourcePane();
  });

  for (const { title, line, key, newLine } of CASES) {
    test(`${title} and scrolls by one row`, async ({ rstudioPage: page }) => {
      const before = await parkCursorOnLastVisibleRow(page, line);

      await page.keyboard.press(key);
      await expect.poll(() => page.evaluate(() =>
        window.rstudio!.documents.activeEditor()!.getCursorPosition().row
      )).toBe(before.cursorRow + 1);
      expect(await currentLine(page)).toBe(newLine);

      // The scroll lands on Ace's next render, so poll rather than read once.
      await expect.poll(() => readViewport(page).then(v => v.firstRow)).toBe(before.firstRow + 1);
      const after = await readViewport(page);
      expect(after.cursorRow).toBeGreaterThanOrEqual(after.firstFullyVisibleRow);
      expect(after.cursorRow).toBeLessThanOrEqual(after.lastFullyVisibleRow);
    });
  }
});

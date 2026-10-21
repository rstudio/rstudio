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
  scrollTop: number;
  scrollerHeight: number;
  lineHeight: number;
  cursorRow: number;
}

async function readViewport(page: Page): Promise<Viewport> {
  return page.evaluate(() => {
    const editor = window.rstudio!.documents.activeEditor()!;
    const renderer = editor.renderer;
    return {
      scrollTop: renderer.scrollTop,
      scrollerHeight: renderer.$size.scrollerHeight,
      lineHeight: renderer.lineHeight,
      cursorRow: editor.getCursorPosition().row,
    };
  });
}

// Row geometry in pixels. Ace's getLastFullyVisibleRow() is off by one when
// the viewport is not aligned to a row boundary, so compute from these instead.
function rowTop(v: Viewport, row: number): number {
  return row * v.lineHeight;
}

function rowBottom(v: Viewport, row: number): number {
  return (row + 1) * v.lineHeight;
}

function viewportBottom(v: Viewport): number {
  return v.scrollTop + v.scrollerHeight;
}

function isRowFullyVisible(v: Viewport, row: number): boolean {
  return rowTop(v, row) >= v.scrollTop && rowBottom(v, row) <= viewportBottom(v);
}

// Fills the editor, scrolls into the middle of it, and parks the cursor at the
// end of `lineText` placed on the last fully visible row, so that a new line
// below the cursor is at least partly off screen until something scrolls.
async function parkCursorOnLastVisibleRow(page: Page, lineText: string): Promise<Viewport> {
  await page.evaluate(() => {
    const editor = window.rstudio!.documents.activeEditor()!;
    editor.setValue(Array.from({ length: 300 }, (_, i) => `x${i + 1} <- ${i + 1}`).join('\n'), -1);
    editor.gotoLine(150, 0, false);
  });
  await expect.poll(() => page.evaluate(() =>
    window.rstudio!.documents.activeEditor()!.getFirstVisibleRow()
  )).toBeGreaterThan(0);

  // Leave the viewport off a row boundary, as it is after real scrolling, so
  // the test covers the geometry that row-based bookkeeping gets wrong.
  await page.evaluate(() => {
    const editor = window.rstudio!.documents.activeEditor()!;
    editor.session.setScrollTop(editor.renderer.scrollTop + editor.renderer.lineHeight / 3);
  });

  const centered = await readViewport(page);
  const row = Math.floor(viewportBottom(centered) / centered.lineHeight) - 1;
  await page.evaluate(({ row, text }) => {
    const editor = window.rstudio!.documents.activeEditor()!;
    const end = editor.session.getLine(row).length;
    editor.session.replace({ start: { row, column: 0 }, end: { row, column: end } }, text);
    editor.selection.setRange({ start: { row, column: text.length }, end: { row, column: text.length } });
    editor.focus();
  }, { row, text: lineText });

  const viewport = await readViewport(page);
  expect(viewport.cursorRow).toBe(row);
  expect(isRowFullyVisible(viewport, row)).toBe(true);
  expect(isRowFullyVisible(viewport, row + 1)).toBe(false);
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

      // The scroll may animate, so wait for the new line to be fully in view
      // before judging how far the view moved.
      await expect.poll(() => readViewport(page).then(v => isRowFullyVisible(v, v.cursorRow))).toBe(true);

      // A minimal scroll leaves the new line flush with the bottom of the
      // viewport, exactly where a plain Return would put it.
      const after = await readViewport(page);
      expect(after.scrollTop).toBeGreaterThan(before.scrollTop);
      expect(after.scrollTop).toBeCloseTo(rowBottom(after, after.cursorRow) - after.scrollerHeight, 1);
    });
  }
});

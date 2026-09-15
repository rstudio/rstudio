// Ctrl+Shift+C / Ctrl+Shift+V copy and paste in the Terminal pane (#1687).
//
// The terminal handles both keys itself, on every platform: xterm sends
// nothing to the shell for them, and they are the copy/paste keys of most
// terminal emulators. Cmd+C / Cmd+V on macOS keep working through the browser.
//
// Copies land on the system clipboard when the app runs headed (desktop), so a
// local run replaces whatever was on it.

import type { Locator, Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { ConsolePane, focusConsole } from '@pages/console_pane.page';
import { killAllTerminals, openTerminal } from '@utils/terminal';

// Only the DOM renderer puts terminal rows in the DOM, where printed text can
// be located for a double-click; the default renderer paints to a WebGL canvas.
const XTERM_ROWS = '.xterm-rows > div';
const XTERM_SELECTION = '.xterm .xterm-selection div';

/**
 * Text of each rendered terminal row with whitespace stripped (U+00B7 too, as
 * in terminal.test.ts), so a match doesn't depend on where a line wraps.
 */
async function rowTexts(page: Page): Promise<string[]> {
  const texts = await page.locator(XTERM_ROWS).allInnerTexts();
  return texts.map((text) => text.replace(/[\s\u00B7]+/g, ''));
}

async function runInTerminal(page: Page, command: string): Promise<void> {
  await page.keyboard.type(command);
  await page.keyboard.press('Enter');
}

/** Screen point at the centre of cell `column` of a rendered terminal row. */
async function cellPoint(row: Locator, column: number): Promise<{ x: number; y: number }> {
  // a span holds a run of same-styled cells, so locate the span covering the
  // column and interpolate within it
  const spans = row.locator('span');
  const count = await spans.count();
  let start = 0;
  for (let i = 0; i < count; i++) {
    const span = spans.nth(i);
    const length = ((await span.textContent()) ?? '').length;
    if (column < start + length) {
      const box = await span.boundingBox();
      if (!box) break;
      const cellWidth = box.width / Math.max(length, 1);
      return { x: box.x + (column - start + 0.5) * cellWidth, y: box.y + box.height / 2 };
    }
    start += length;
  }
  throw new Error(`terminal row has no cell at column ${column}`);
}

/**
 * Print a unique word on its own line and select it with a double-click.
 * Quotes split the word on the command line, so the only rendered row that
 * contains it whole is the output. Returns the word.
 */
async function printAndSelectWord(page: Page): Promise<string> {
  const stamp = Date.now();
  const word = `copy_me_${stamp}`;
  await runInTerminal(page, `echo copy_"me"_${stamp}`);

  const row = page.locator(XTERM_ROWS, { hasText: word });
  await expect(row).toHaveCount(1, { timeout: TIMEOUTS.consoleReady });
  const text = (await row.textContent()) ?? '';
  const point = await cellPoint(row, text.indexOf(word) + Math.floor(word.length / 2));

  // the raw mouse: xterm's screen element sits over the row spans, so a
  // locator-level click fails its hit-target check
  await page.mouse.dblclick(point.x, point.y);
  await expect(page.locator(XTERM_SELECTION).first()).toBeVisible();
  return word;
}

test.describe.serial('Terminal: Ctrl+Shift+C / Ctrl+Shift+V', () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    await setPref(page, 'terminal_renderer', 'dom');
    // Where the browser has no plain-text paste key (macOS), the terminal
    // reads the clipboard through the async clipboard API, which Chromium
    // gates behind a permission. Electron contexts don't support this call
    // and don't need it: the desktop bridge reads the clipboard there.
    await page
      .context()
      .grantPermissions(['clipboard-read'])
      .catch(() => {});
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    try {
      await clearPref(page, 'terminal_renderer');
    } catch (err) {
      console.warn('[terminal_clipboard] afterAll clearPref failed:', err);
    }
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page);
    await openTerminal(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page).catch(() => {});
    await executeCommand(page, 'activateConsole').catch(() => {});
  });

  test('Ctrl+Shift+C copies the selection', async ({ rstudioPage: page }) => {
    const word = await printAndSelectWord(page);
    await page.keyboard.press('Control+Shift+C');

    // read the clipboard back through a plain paste into the Console input
    await focusConsole(page);
    await page.keyboard.press('ControlOrMeta+V');
    const consolePane = new ConsolePane(page);
    await expect.poll(() => consolePane.consoleInputValue()).toBe(word);
    await page.keyboard.press('Escape');
  });

  test('Ctrl+Shift+V pastes at the prompt', async ({ rstudioPage: page }) => {
    const word = await printAndSelectWord(page);
    await page.keyboard.press('Control+Shift+C');

    await page.keyboard.type('echo P=');
    await page.keyboard.press('Control+Shift+V');

    // where the paste is read asynchronously it lands a moment after the key;
    // join the rows so a command line that wraps still matches
    await expect.poll(async () => (await rowTexts(page)).join('')).toContain(`P=${word}`);
    await page.keyboard.press('Enter');

    // the echoed output starts its own row, so compare it whole: a doubled
    // paste (P=<word><word>) would still pass a substring match
    await expect.poll(() => rowTexts(page)).toContain(`P=${word}`);
  });
});

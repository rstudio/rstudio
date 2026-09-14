// Opening files from paths printed in the Terminal pane with Ctrl/Cmd+Click.
//
// The xterm file-link provider sends every token on the hovered line to the
// session, which resolves them against the terminal's working directory; only
// tokens naming an existing file become links, and a link activates with the
// platform's open-link modifier held (Cmd on macOS, Ctrl elsewhere).

import type { Locator, Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { AceEditor } from '@pages/ace_editor.page';
import { clearPref, documentCloseAllNoSave, executeCommand, setPref, waitForActiveDocument } from '@utils/commands';
import { seedSandboxFile } from '@utils/files';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import { captureResult, focusTerminal, killAllTerminals, openTerminal } from '@utils/terminal';

const FILE_CONTENT = 'first <- 1\nsecond <- 2\nthird <- 3\n';

// Only the DOM renderer puts terminal rows in the DOM where a printed path can
// be located and hovered; the default renderer paints to a WebGL canvas.
const XTERM_ROWS = '.xterm-rows > div';
const LINK_UNDER_POINTER = '.xterm-screen.xterm-cursor-pointer';

async function runInTerminal(page: Page, command: string): Promise<void> {
  await page.keyboard.type(command);
  await page.keyboard.press('Enter');
}

interface Point {
  x: number;
  y: number;
}

/** Screen point at the centre of cell `column` of a rendered terminal row. */
async function cellPoint(row: Locator, column: number): Promise<Point> {
  // a span holds a run of same-styled cells (e.g. every name on an `ls`
  // row), so locate the span covering the column and interpolate within it
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
 * Find the last occurrence of `text` in the rendered terminal and return a
 * point over it. A long path wraps across rows, and where the wrap falls
 * depends on the path's length, so the match is made against each row joined
 * with the one above it and the point lands on the part of the match in the
 * lower row. Searching from the bottom finds the command's output rather than
 * its echo of the command line.
 */
async function locateTerminalText(page: Page, text: string): Promise<Point> {
  const rows = page.locator(XTERM_ROWS);
  const texts = await rows.allTextContents();
  for (let i = texts.length - 1; i >= 0; i--) {
    const above = i > 0 ? texts[i - 1] : '';
    const index = (above + texts[i]).lastIndexOf(text);
    if (index === -1) continue;

    // a match lying wholly in the row above is found on that row's turn
    const end = index + text.length - above.length;
    if (end <= 0) continue;

    const start = Math.max(0, index - above.length);
    return cellPoint(rows.nth(i), Math.floor((start + end) / 2));
  }
  throw new Error(`terminal text not found: ${text}`);
}

/**
 * Hover the terminal text `text` until xterm reports a link under the pointer,
 * and return the point to click. `text` should be the tail of the printed
 * path (the file name), which belongs to a link naming the file wherever the
 * path wraps.
 *
 * The raw mouse is used throughout: xterm's screen element sits over the row
 * spans, so locator-level hover/click fail their hit-target check. Link
 * resolution is asynchronous and the terminal's working directory reaches the
 * session a little after a `cd`, so an early hover can come back empty; xterm
 * only re-queries a row once the pointer has visited another row, hence the
 * hop to the row above before each retry.
 */
async function hoverFileLink(page: Page, text: string): Promise<Point> {
  let point: Point = { x: 0, y: 0 };
  await expect(async () => {
    point = await locateTerminalText(page, text);
    const rowHeight = (await page.locator(XTERM_ROWS).first().boundingBox())?.height ?? 0;
    await page.mouse.move(point.x, point.y - rowHeight);
    await page.mouse.move(point.x, point.y);
    await expect(page.locator(LINK_UNDER_POINTER)).toBeVisible({ timeout: TIMEOUTS.settleDelay });
  }).toPass({ timeout: TIMEOUTS.fileOpen });

  return point;
}

/** Click a hovered link with the platform's open-link modifier held. */
async function clickFileLink(page: Page, point: Point): Promise<void> {
  const modifier = process.platform === 'darwin' ? 'Meta' : 'Control';
  await page.keyboard.down(modifier);
  try {
    await page.mouse.click(point.x, point.y);
  } finally {
    await page.keyboard.up(modifier);
  }
}

const sandbox = useSuiteSandbox();

test.describe.serial('Terminal: file paths open with Ctrl/Cmd+Click', () => {
  let isWindows = false;

  test.beforeAll(async ({ rstudioPage: page }) => {
    isWindows = (await captureResult(page, '.Platform$OS.type')) === 'windows';
    await setPref(page, 'terminal_renderer', 'dom');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'terminal_renderer').catch(() => {});
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await documentCloseAllNoSave(page);
    await killAllTerminals(page);
    await openTerminal(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page).catch(() => {});
    await documentCloseAllNoSave(page).catch(() => {});
    await executeCommand(page, 'activateConsole').catch(() => {});
  });

  test('an absolute path with a :line suffix opens the file at that line', async ({ rstudioPage: page }) => {
    // Against a remote server the seed is written through the R console,
    // which takes focus away from the terminal.
    const fullPath = await seedSandboxFile(page, sandbox.dir, 'link_absolute.R', FILE_CONTENT);
    await focusTerminal(page);

    // On Windows the sandbox path comes back with backslashes, which an
    // unquoted bash `echo` strips (Git Bash is the default shell there); print
    // the forward-slash form, which every shell echoes verbatim and the
    // session resolves just the same.
    await runInTerminal(page, `echo ${fullPath.replace(/\\/g, '/')}:2`);
    const link = await hoverFileLink(page, 'link_absolute.R:2');
    await clickFileLink(page, link);

    await waitForActiveDocument(page, fullPath, TIMEOUTS.fileOpen);
    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getValue()).toBe(FILE_CONTENT);
    await expect.poll(async () => (await editor.getCursorPosition()).row).toBe(1);
  });

  test('a relative path resolves against the terminal working directory', async ({ rstudioPage: page }) => {
    test.skip(isWindows, 'the test drives a POSIX shell (cd + ls)');

    const fileName = 'link_relative.R';
    const terminalDir = `${sandbox.dir}/terminal`;
    const fullPath = await seedSandboxFile(page, sandbox.dir, `terminal/${fileName}`, FILE_CONTENT);

    // R stays in the sandbox root, with a same-named decoy. Resolving against
    // R's working directory must not open that file instead of the terminal's.
    await seedSandboxFile(page, sandbox.dir, fileName, 'stop("wrong working directory")\n');
    expect(await captureResult(page, 'getwd()')).toBe(sandbox.dir);
    await focusTerminal(page);

    await runInTerminal(page, `cd "${terminalDir}"`);
    // Wait for the session's cwd poller before hovering: the decoy is also a
    // valid link while the terminal still reports its previous directory.
    await expect.poll(() => captureResult(page,
      `normalizePath(rstudioapi::terminalContext(rstudioapi::terminalList()[[1]])$working_dir) == normalizePath(${rPathLiteral(terminalDir)})`,
    ), { timeout: TIMEOUTS.fileOpen }).toBe('TRUE');
    await focusTerminal(page);
    await runInTerminal(page, 'ls');
    const link = await hoverFileLink(page, fileName);
    await clickFileLink(page, link);

    await waitForActiveDocument(page, fullPath, TIMEOUTS.fileOpen);
    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getValue()).toBe(FILE_CONTENT);
  });
});

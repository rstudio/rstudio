// Opening files from paths printed in the Terminal pane with Ctrl/Cmd+Click.
//
// The xterm file-link provider sends every token on the hovered line to the
// session, which resolves them against the terminal's working directory; only
// tokens naming an existing file become links, and a link activates with the
// platform's open-link modifier held (Cmd on macOS, Ctrl elsewhere).

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { AceEditor } from '@pages/ace_editor.page';
import { clearPref, documentCloseAllNoSave, executeCommand, setPref, waitForActiveDocument } from '@utils/commands';
import { seedSandboxFile } from '@utils/files';
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

/**
 * Hover the terminal text `text` until xterm reports a link under the pointer,
 * and return the point to click. A long path wraps across rows, so `text`
 * should be its tail (the file name); the last matching span is the command's
 * output rather than its echo of the command line, and either way the row
 * belongs to a link naming the file.
 *
 * The raw mouse is used throughout: xterm's screen element sits over the row
 * spans, so locator-level hover/click fail their hit-target check. Link
 * resolution is asynchronous and the terminal's working directory reaches the
 * session a little after a `cd`, so an early hover can come back empty; xterm
 * only re-queries a row once the pointer has visited another row, hence the
 * hop to the row above before each retry.
 */
async function hoverFileLink(page: Page, text: string): Promise<Point> {
  const link = page.locator(`${XTERM_ROWS} span`).filter({ hasText: text }).last();
  await expect(link).toBeVisible({ timeout: TIMEOUTS.consoleReady });

  let point: Point = { x: 0, y: 0 };
  await expect(async () => {
    const box = await link.boundingBox();
    if (!box) throw new Error('link span has no bounding box');

    // the span holds a whole run of same-styled cells (e.g. every name on an
    // `ls` row), so aim at the middle of `text` rather than of the span
    const content = (await link.textContent()) ?? '';
    const cellWidth = box.width / Math.max(content.length, 1);
    const offset = content.indexOf(text) + text.length / 2;
    point = { x: box.x + offset * cellWidth, y: box.y + box.height / 2 };
    await page.mouse.move(point.x, point.y - box.height);
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

    await runInTerminal(page, `echo ${fullPath}:2`);
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
    const fullPath = await seedSandboxFile(page, sandbox.dir, fileName, FILE_CONTENT);
    await focusTerminal(page);

    await runInTerminal(page, `cd "${sandbox.dir}"`);
    await runInTerminal(page, 'ls');
    const link = await hoverFileLink(page, fileName);
    await clickFileLink(page, link);

    await waitForActiveDocument(page, fullPath, TIMEOUTS.fileOpen);
    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getValue()).toBe(FILE_CONTENT);
  });
});

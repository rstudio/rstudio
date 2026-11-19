import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from 'playwright';
import { TIMEOUTS } from '@utils/constants';
import {
  clearPref, executeCommand, resetLayoutZoom, setPref, waitForActiveDocument,
} from '@utils/commands';
import { closeAndDeleteSandboxFiles, seedSandboxFile } from '@utils/files';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import { killAllTerminals, XTERM_SELECTOR } from '@utils/terminal';
import {
  getConsoleCursorPosition,
  getConsoleScreenRowCount,
  getConsoleSelectedText,
  getSelectionInfo,
  setConsoleInput,
} from '@utils/console';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { waitForConsoleFocus } from '@pages/console_pane.page';

// Focus Last Console Error
const FOCUS_LAST_ERROR = process.platform === 'darwin' ? 'Control+Alt+7' : 'Alt+Shift+7';
const NO_ERROR_ANNOUNCEMENT = '[role="status"]:has-text("No Console error to focus")';
// An error with a traceback, so its box has the Show Traceback button
const NESTED_ERROR = 'g <- function() stop("boom"); f <- function() g(); f()';

test.describe('Console pane', () => {
  let consoleActions: ConsolePaneActions;
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.beforeEach(async () => {
    await consoleActions.clearConsole();
  });

  test('print() auto-prints its argument', async () => {
    const phrase = 'If we shadows have offended, think but this, and all is mended.';
    await consoleActions.executeInConsole(`print("${phrase}")`);
    await expect(consoleActions.consolePane.consoleOutput).toContainText(`[1] "${phrase}"`);
  });

  test('unknown identifier prints object-not-found error', async () => {
    await consoleActions.executeInConsole('fake_command');
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      "Error: object 'fake_command' not found",
    );
  });

  test('arrow keys cycle through previous commands', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole("cat('one')");
    await consoleActions.executeInConsole("cat('two')");
    await consoleActions.executeInConsole("cat('three')");
    await expect(consoleActions.consolePane.consoleOutput).toContainText('three');

    const input = consoleActions.consolePane.consoleInput;
    await input.click({ force: true });
    await waitForConsoleFocus(page);

    const readInput = () => consoleActions.consolePane.consoleInputValue();

    await page.keyboard.press('ArrowUp');
    await expect.poll(readInput).toBe("cat('three')");
    await page.keyboard.press('ArrowUp');
    await expect.poll(readInput).toBe("cat('two')");
    await page.keyboard.press('ArrowUp');
    await expect.poll(readInput).toBe("cat('one')");
    await page.keyboard.press('ArrowDown');
    await expect.poll(readInput).toBe("cat('two')");
    await page.keyboard.press('ArrowDown');
    await expect.poll(readInput).toBe("cat('three')");

    // Escape clears the recalled command; without this, "cat('three')" leaks
    // into the next test's input and becomes a parse error.
    await page.keyboard.press('Escape');
    await expect.poll(readInput).toBe('');
  });

  // The console is a command line, so the ends of a soft-wrapped command --
  // not the ends of a wrapped visual row -- are what Home / End should reach.
  // Ace's stock behavior stranded the cursor at the wrap boundary (#18447).
  test.describe('line navigation over a soft-wrapped command', () => {
    // Long enough to wrap at any plausible console width.
    const command = `x <- c(${Array.from({ length: 80 }, (_, i) => i + 1).join(', ')})`;

    test.beforeEach(async ({ rstudioPage: page }) => {
      await consoleActions.consolePane.consoleInput.click({ force: true });
      await waitForConsoleFocus(page);
      await setConsoleInput(page, command);

      // Everything below is vacuously true unless the input really wrapped.
      expect(await getConsoleScreenRowCount(page)).toBeGreaterThan(1);
    });

    // Key presses go through the console-input locator, not page.keyboard:
    // pressing on the focused element is racy, since focus can shift between
    // setConsoleInput's editor.focus() and the key press (see the same
    // pattern in ConsolePaneActions.executeInConsole).
    test.afterEach(async () => {
      // Leave the input empty; a leftover command would be submitted by the
      // next test's Enter and show up as a parse error.
      await consoleActions.consolePane.consoleInput.press('Escape');
      await expect.poll(() => consoleActions.consolePane.consoleInputValue()).toBe('');
    });

    test('Home and End reach the ends of the whole command', async ({ rstudioPage: page }) => {
      await consoleActions.consolePane.consoleInput.press('Home');
      await expect.poll(() => getConsoleCursorPosition(page)).toEqual({ row: 0, column: 0 });

      await consoleActions.consolePane.consoleInput.press('End');
      await expect
        .poll(() => getConsoleCursorPosition(page))
        .toEqual({ row: 0, column: command.length });
    });

    test('Shift+Home and Shift+End select to the ends of the whole command', async ({
      rstudioPage: page,
    }) => {
      // Cursor starts at the end of the command, so this selects all of it.
      await consoleActions.consolePane.consoleInput.press('Shift+Home');
      await expect.poll(() => getConsoleSelectedText(page)).toBe(command);

      await consoleActions.consolePane.consoleInput.press('Home');
      await consoleActions.consolePane.consoleInput.press('Shift+End');
      await expect.poll(() => getConsoleSelectedText(page)).toBe(command);
    });
  });

  test('timestamp() adds an entry to console history', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('timestamp(quiet = TRUE)');

    // timestamp(quiet = TRUE) produces no console output, so there is no
    // text-based gate signalling that R is idle. Wait for the busy class to
    // clear before recalling history -- otherwise ArrowUp fires while R is
    // still executing and the recalled entry can be stale.
    await page.waitForFunction(
      () => {
        const el = document.getElementById('rstudio_console_input');
        return !!el && !el.classList.contains('rstudio-console-busy');
      },
      null,
      { timeout: TIMEOUTS.consoleReady, polling: 100 },
    );

    await consoleActions.consolePane.consoleInput.click({ force: true });
    await waitForConsoleFocus(page);

    const readInput = () => consoleActions.consolePane.consoleInputValue();

    await page.keyboard.press('ArrowUp');
    await expect.poll(readInput).toMatch(/^##.*##$/);

    await page.keyboard.press('Escape');
    await expect.poll(readInput).toBe('');
  });

  test('writeLines outputs all 10000 lines without truncation', async () => {
    await consoleActions.executeInConsole('long <- as.character(1:1E4)');
    await consoleActions.executeInConsole('writeLines(long)');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('10000', {
      timeout: 60000,
    });
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText(
      '<console output truncated>',
    );
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText(
      'writeLines(long)',
    );
  });

  test('Show Traceback button reveals the stack for nested calls', async () => {
    await consoleActions.executeInConsole('f <- function() stop()');
    await consoleActions.executeInConsole('g <- function() f()');
    await consoleActions.executeInConsole('h <- function() g()');
    await consoleActions.executeInConsole('k <- function() h()');
    await consoleActions.executeInConsole('k()');

    await expect(consoleActions.consolePane.tracebackBtn).toBeVisible({ timeout: 10000 });
    await expect(consoleActions.consolePane.tracebackBtn).toHaveText('Show Traceback');
    await consoleActions.consolePane.tracebackBtn.click();
    await expect(consoleActions.consolePane.stackTrace).toBeVisible();
    await expect(consoleActions.consolePane.tracebackBtn).toHaveText('Hide Traceback');

    const actual = (await consoleActions.consolePane.stackTrace.innerText()).replace(/\s+/g, '');
    const expected = '5.function()stop()4.function()f()3.function()g()2.function()h()1.k()';
    expect(actual).toBe(expected);

    await consoleActions.consolePane.tracebackBtn.click();
    await expect(consoleActions.consolePane.stackTrace).toBeHidden();
    await expect(consoleActions.consolePane.tracebackBtn).toHaveText('Show Traceback');
  });

  test('keyboard reaches an error box and its traceback source links', async ({
    rstudioPage: page,
  }) => {
    const fileName = 'console_error_keyboard.R';
    const filePath = await seedSandboxFile(page, sandbox.dir, fileName,
      'inner <- function() stop("boom")\nouter <- function() inner()\n');
    try {
      await consoleActions.executeInConsole(`source(${rPathLiteral(filePath)})`);
      await consoleActions.executeInConsole('outer()');
      const pane = consoleActions.consolePane;
      await expect(pane.tracebackBtn).toBeVisible({ timeout: 10000 });
      await expect(pane.tracebackBtn).toHaveAttribute('title', /most recent Console error/);

      // A half-typed line must survive the visit: keys meant for the error box
      // must not reach the input (Escape would clear it, Enter would run it)
      await setConsoleInput(page, 'pending <- 1');

      await page.keyboard.press(FOCUS_LAST_ERROR);
      await expect(pane.tracebackBtn).toBeFocused();
      await expect(pane.tracebackBtn).toHaveAttribute('aria-expanded', 'false');

      await page.keyboard.press('Escape');
      await waitForConsoleFocus(page);
      expect(await pane.consoleInputValue()).toBe('pending <- 1');

      await page.keyboard.press(FOCUS_LAST_ERROR);
      await expect(pane.tracebackBtn).toBeFocused();
      await page.keyboard.press('Enter');
      await expect(pane.stackTrace).toBeVisible();
      await expect(pane.tracebackBtn).toHaveAttribute('aria-expanded', 'true');
      await expect(pane.tracebackBtn).toHaveText('Hide Traceback');
      await expect(pane.tracebackBtn).toBeFocused();
      expect(await pane.consoleInputValue()).toBe('pending <- 1');

      // Tab moves through the box even though Tab in the input does completion
      await page.keyboard.press('Tab');
      await expect(pane.rerunBtn).toBeFocused();
      await page.keyboard.press('Tab');
      const sourceLink = pane.stackTrace.getByRole('button', {
        name: /^console_error_keyboard\.R#\d+$/,
      });
      await expect(sourceLink.first()).toBeFocused();

      await page.keyboard.press('Enter');
      await waitForActiveDocument(page, filePath);
    } finally {
      await setConsoleInput(page, '');
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
    }
  });

  test('Focus Last Console Error announces when there is no error', async ({
    rstudioPage: page,
  }) => {
    await page.keyboard.press(FOCUS_LAST_ERROR);
    await expect(page.locator(NO_ERROR_ANNOUNCEMENT)).toBeAttached();
    await waitForConsoleFocus(page);
  });

  test('Focus Last Console Error announces when trimming removed the error\'s buttons', async ({
    rstudioPage: page,
  }) => {
    const announcement = page.locator(NO_ERROR_ANNOUNCEMENT);
    await setPref(page, 'console_max_lines', 20);
    try {
      await consoleActions.executeInConsole(
        'f <- function() stop(paste(1:15, collapse = "\\n")); g <- function() f(); g()');
      const pane = consoleActions.consolePane;
      await expect(pane.tracebackBtn).toBeVisible({ timeout: 10000 });

      // Trim into the error box: its buttons go first, part of its message stays
      await consoleActions.executeInConsole('writeLines(as.character(101:110))');
      await expect(page.locator('.ace_console_error')).toHaveCount(1);
      await expect(pane.tracebackBtn).toHaveCount(0);

      // An earlier test's announcement clears itself after a few seconds
      await expect(announcement).toHaveCount(0, { timeout: 10000 });
      await page.keyboard.press(FOCUS_LAST_ERROR);
      await expect(announcement).toBeAttached();
      await waitForConsoleFocus(page);
    } finally {
      await clearPref(page, 'console_max_lines');
    }
  });

  // Alone in its pane, the Console has no tab panel, and the end of the
  // restore animation focuses the Console's input
  async function setConsoleAlone(page: Page, alone: boolean): Promise<void> {
    const neighbors = ['terminal', 'background_jobs'];
    if (alone) {
      for (const id of neighbors) {
        const close = page.locator(`#rstudio_workbench_tab_${id} img[alt^="Close"]`);
        if (await close.isVisible())
          await close.click();
      }
    } else {
      // Later specs expect both tabs. Reopening Terminal starts a shell; let it
      // finish starting before ending it.
      await executeCommand(page, 'activateBackgroundJobs');
      await executeCommand(page, 'activateTerminal');
      await expect(page.locator(`#rstudio_workbench_panel_terminal ${XTERM_SELECTOR}`))
        .toBeVisible({ timeout: TIMEOUTS.consoleReady });
      await killAllTerminals(page);
      await executeCommand(page, 'activateConsole');
    }
    await expect(page.locator('#rstudio_workbench_tab_console')).toBeVisible({ visible: !alone });
    for (const id of neighbors)
      await expect(page.locator(`#rstudio_workbench_tab_${id}`)).toBeVisible({ visible: !alone });
  }

  for (const hide of ['zoomed out', 'minimized', 'minimized alone in its pane'] as const) {
    for (const reducedMotion of [true, false]) {
      const motion = reducedMotion ? 'reduced motion' : 'animated';
      test(`Focus Last Console Error brings a ${hide} Console forward (${motion})`, async ({
        rstudioPage: page,
      }) => {
        const alone = hide === 'minimized alone in its pane';
        const pane = consoleActions.consolePane;
        try {
          if (alone)
            await setConsoleAlone(page, true);
          await consoleActions.executeInConsole(NESTED_ERROR);
          await expect(pane.tracebackBtn).toBeVisible({ timeout: 10000 });

          await setPref(page, 'reduced_motion', reducedMotion);
          if (hide === 'zoomed out')
            await executeCommand(page, 'layoutZoomRightColumn');
          else
            await page.locator('#rstudio_Console_pane [class*="rstudio_panel_min_btn"]')
              .first().click();
          await expect(page.locator('body')).not.toHaveClass(/rstudio-animating/);
          await expect(pane.tracebackBtn).not.toBeInViewport();

          await page.keyboard.press(FOCUS_LAST_ERROR);
          await expect(pane.tracebackBtn).toBeInViewport();
          await expect(pane.tracebackBtn).toBeFocused();

          // Restoring the pane animates, and its end can move focus; the column
          // animation sets no marker to wait on, so watch past its 300ms
          for (let elapsed = 0; elapsed < 1000; elapsed += 50) {
            expect(await pane.tracebackBtn.evaluate((el) => el === document.activeElement),
              `Show Traceback still focused after ${elapsed}ms`).toBe(true);
            await page.waitForTimeout(50);
          }
        } finally {
          // The fixture's value, not the default: animations would slow every later test
          await setPref(page, 'reduced_motion', true);
          await resetLayoutZoom(page);
          if (!(await page.locator('#rstudio_Console_pane').isVisible()))
            await executeCommand(page, 'activateConsolePane');
          if (alone)
            await setConsoleAlone(page, false);
        }
      });
    }
  }

  test('auto-expanded traceback offers Hide Traceback', async ({ rstudioPage: page }) => {
    await setPref(page, 'auto_expand_error_tracebacks', true);
    try {
      await consoleActions.executeInConsole('g <- function() stop("boom"); f <- function() g(); f()');

      await expect(consoleActions.consolePane.stackTrace).toBeVisible({ timeout: 10000 });
      await expect(consoleActions.consolePane.tracebackBtn).toHaveText('Hide Traceback');

      await consoleActions.consolePane.tracebackBtn.click();
      await expect(consoleActions.consolePane.stackTrace).toBeHidden();
      await expect(consoleActions.consolePane.tracebackBtn).toHaveText('Show Traceback');
    } finally {
      await clearPref(page, 'auto_expand_error_tracebacks');
    }
  });

  test.describe('Find in Console', () => {
    test.beforeEach(async () => {
      await consoleActions.executeInConsole(`a <- "Once more unto the breach, dear friends, once more;"`);
      await consoleActions.executeInConsole(`b <- "Or close the wall up with our English dead."`);
      await consoleActions.executeInConsole(`c <- "In peace there's nothing so becomes a man"`);
      await consoleActions.executeInConsole(`d <- "As modest stillness and humility."`);
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole('writeLines(a)');
      await consoleActions.executeInConsole('writeLines(b)');
      await consoleActions.executeInConsole('writeLines(c)');
      await consoleActions.executeInConsole('writeLines(d)');
      await expect(consoleActions.consolePane.consoleOutput).toContainText(
        'As modest stillness and humility.',
      );

      await consoleActions.consolePane.findBtn.click();
      await expect(consoleActions.consolePane.findBar).toBeVisible();
    });

    test.afterEach(async () => {
      // Guarded: if beforeEach failed before opening the find bar, clicking
      // Close here would error and mask the real root cause.
      if (await consoleActions.consolePane.findBar.isVisible()) {
        await consoleActions.consolePane.findClose.click();
        await expect(consoleActions.consolePane.findBar).not.toBeVisible();
      }
    });

    test('finds matches across multiple lines with "the"', async ({ rstudioPage: page }) => {
      const { findInput, findNext } = consoleActions.consolePane;

      await findInput.fill('the');
      await findInput.press('Enter');

      const first = await getSelectionInfo(page);
      expect(first.text.toLowerCase()).toBe('the');

      await findNext.click();
      const second = await getSelectionInfo(page);
      expect(second.text.toLowerCase()).toBe('the');
      expect(second.pos).not.toBe(first.pos);

      await findNext.click();
      const third = await getSelectionInfo(page);
      expect(third.text.toLowerCase()).toBe('the');
      expect(third.pos).not.toBe(second.pos);
    });

    test('case-insensitive search matches both cases of "once"', async ({ rstudioPage: page }) => {
      const { findInput, findNext, findCaseSensitive } = consoleActions.consolePane;

      if (await findCaseSensitive.isChecked()) {
        await findCaseSensitive.uncheck();
      }
      await expect(findCaseSensitive).not.toBeChecked();

      await findInput.fill('once');
      await findInput.press('Enter');

      const first = await getSelectionInfo(page);
      expect(first.text.toLowerCase()).toBe('once');

      await findNext.click();
      const second = await getSelectionInfo(page);
      expect(second.text.toLowerCase()).toBe('once');
      expect(second.pos).not.toBe(first.pos);
    });

    test('case-sensitive search matches only exact case of "once"', async ({ rstudioPage: page }) => {
      const { findInput, findNext, findCaseSensitive } = consoleActions.consolePane;

      await findCaseSensitive.check();
      await expect(findCaseSensitive).toBeChecked();

      await findInput.fill('once');
      await findInput.press('Enter');

      const first = await getSelectionInfo(page);
      expect(first.text).toBe('once');

      await findNext.click();
      const second = await getSelectionInfo(page);
      expect(second.text).toBe('once');
      expect(second.pos).toBe(first.pos);

      await findCaseSensitive.uncheck();
      await expect(findCaseSensitive).not.toBeChecked();
    });
  });
});

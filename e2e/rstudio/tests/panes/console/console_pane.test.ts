import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import {
  getConsoleCursorPosition,
  getConsoleScreenRowCount,
  getConsoleSelectedText,
  getSelectionInfo,
  setConsoleInput,
} from '@utils/console';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { waitForConsoleFocus } from '@pages/console_pane.page';

test.describe('Console pane', () => {
  let consoleActions: ConsolePaneActions;

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

    test.afterEach(async ({ rstudioPage: page }) => {
      // Leave the input empty; a leftover command would be submitted by the
      // next test's Enter and show up as a parse error.
      await page.keyboard.press('Escape');
      await expect.poll(() => consoleActions.consolePane.consoleInputValue()).toBe('');
    });

    test('Home and End reach the ends of the whole command', async ({ rstudioPage: page }) => {
      await page.keyboard.press('Home');
      await expect.poll(() => getConsoleCursorPosition(page)).toEqual({ row: 0, column: 0 });

      await page.keyboard.press('End');
      await expect
        .poll(() => getConsoleCursorPosition(page))
        .toEqual({ row: 0, column: command.length });
    });

    test('Shift+Home and Shift+End select to the ends of the whole command', async ({
      rstudioPage: page,
    }) => {
      // Cursor starts at the end of the command, so this selects all of it.
      await page.keyboard.press('Shift+Home');
      await expect.poll(() => getConsoleSelectedText(page)).toBe(command);

      await page.keyboard.press('Home');
      await page.keyboard.press('Shift+End');
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
    await consoleActions.consolePane.tracebackBtn.click();
    await expect(consoleActions.consolePane.stackTrace).toBeVisible();

    const actual = (await consoleActions.consolePane.stackTrace.innerText()).replace(/\s+/g, '');
    const expected = '5.function()stop()4.function()f()3.function()g()2.function()h()1.k()';
    expect(actual).toBe(expected);
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

import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { getSelectionInfo } from '@utils/console';
import { ConsolePaneActions } from '@actions/console_pane.actions';

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
    await consoleActions.typeInConsole(`print("${phrase}")`);
    await expect(consoleActions.consolePane.consoleOutput).toContainText(`[1] "${phrase}"`);
  });

  test('unknown identifier prints object-not-found error', async () => {
    await consoleActions.typeInConsole('fake_command');
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      "Error: object 'fake_command' not found",
    );
  });

  test('arrow keys cycle through previous commands', async ({ rstudioPage: page }) => {
    await consoleActions.typeInConsole("cat('one')");
    await consoleActions.typeInConsole("cat('two')");
    await consoleActions.typeInConsole("cat('three')");
    await expect(consoleActions.consolePane.consoleOutput).toContainText('three');

    const input = consoleActions.consolePane.consoleInput;
    await input.click({ force: true });
    await sleep(200);

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

  test('writeLines outputs all 10000 lines without truncation', async () => {
    test.setTimeout(90000);
    await consoleActions.typeInConsole('long <- as.character(1:1E4)');
    await consoleActions.typeInConsole('writeLines(long)');
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
    await consoleActions.typeInConsole('f <- function() stop()');
    await consoleActions.typeInConsole('g <- function() f()');
    await consoleActions.typeInConsole('h <- function() g()');
    await consoleActions.typeInConsole('k <- function() h()');
    await consoleActions.typeInConsole('k()');

    await expect(consoleActions.consolePane.tracebackBtn).toBeVisible({ timeout: 10000 });
    await consoleActions.consolePane.tracebackBtn.click();
    await expect(consoleActions.consolePane.stackTrace).toBeVisible();

    const actual = (await consoleActions.consolePane.stackTrace.innerText()).replace(/\s+/g, '');
    const expected = '5.function()stop()4.function()f()3.function()g()2.function()h()1.k()';
    expect(actual).toBe(expected);
  });

  test.describe('Find in Console', () => {
    test.beforeEach(async () => {
      await consoleActions.typeInConsole(`a <- "Once more unto the breach, dear friends, once more;"`);
      await consoleActions.typeInConsole(`b <- "Or close the wall up with our English dead."`);
      await consoleActions.typeInConsole(`c <- "In peace there's nothing so becomes a man"`);
      await consoleActions.typeInConsole(`d <- "As modest stillness and humility."`);
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole('writeLines(a)');
      await consoleActions.typeInConsole('writeLines(b)');
      await consoleActions.typeInConsole('writeLines(c)');
      await consoleActions.typeInConsole('writeLines(d)');
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

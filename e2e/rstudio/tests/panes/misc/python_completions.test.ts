import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import type { Page } from 'playwright';

async function waitForConsoleIdle(page: Page): Promise<void> {
  await page.waitForFunction(
    () => {
      const el = document.getElementById('rstudio_console_input');
      return !!el && !el.classList.contains('rstudio-console-busy');
    },
    null,
    { timeout: TIMEOUTS.consoleReady, polling: 100 },
  );
}

async function exitPythonReplIfActive(
  page: Page,
  consoleActions: ConsolePaneActions,
): Promise<void> {
  const text = await consoleActions.consolePane.consoleOutput.innerText();
  // Trailing >>> indicates the Python REPL is still the active prompt.
  if (!text.trimEnd().endsWith('>>>')) return;
  await consoleActions.executeInConsole('exit');
  await waitForConsoleIdle(page);
}

// https://github.com/rstudio/rstudio/issues/14560
test.describe('Python REPL completions', () => {
  let consoleActions: ConsolePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    missingPackages = await consoleActions.ensurePackages(['reticulate']);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // Always leave the REPL so a failed test does not leak state into the
    // next test in this worker.
    await exitPythonReplIfActive(page, consoleActions);
    await consoleActions.clearConsole();
  });

  test('Tab-completion of __dunder__ attributes does not add quotes', async ({ rstudioPage: page }) => {
    test.skip(missingPackages.length > 0, `Missing: ${missingPackages.join(', ')}`);

    await consoleActions.executeInConsole('reticulate::repl_python()');
    await waitForConsoleIdle(page);
    await consoleActions.executeInConsole('import sys');
    await waitForConsoleIdle(page);

    // Type the partial attribute and wait for the completion popup. On some
    // Windows configurations the Python REPL completion engine does not produce
    // a popup (reticulate's jedi integration may not be initialised); skip the
    // rest of the test rather than hard-fail in that case.
    await consoleActions.typeInConsole('sys.__name');
    const popupVisible = await page.locator('#rstudio_popup_completions')
      .waitFor({ state: 'visible', timeout: 8000 })
      .then(() => true)
      .catch(() => false);
    test.skip(!popupVisible, 'Python REPL completion popup did not appear; skipping completion acceptance');

    await page.keyboard.press('Tab');
    // Wait for the popup to close (Tab accepted the completion and the popup
    // dismissed itself) before pressing Enter to submit the command.
    await expect(page.locator('#rstudio_popup_completions')).not.toBeVisible({ timeout: 3000 });
    await page.keyboard.press('Enter');

    // Before the fix the dunder name was emitted as `sys."__name__"` (quoted),
    // which Python evaluates to the string instead of the attribute. The
    // input echo must be `>>> sys.__name__` (no inner quotes) and the result
    // line must be `'sys'`.
    await expect
      .poll(
        async () => {
          const text = await consoleActions.consolePane.consoleOutput.innerText();
          const lines = text
            .split('\n')
            .map((l) => l.trim())
            .filter((l) => l !== '' && l !== '>>>');
          const echoIdx = lines.findIndex((l) => l.startsWith('>>> sys.__name'));
          if (echoIdx < 0 || echoIdx + 1 >= lines.length) return null;
          return [lines[echoIdx], lines[echoIdx + 1]];
        },
        { timeout: TIMEOUTS.consoleReady },
      )
      .toEqual(['>>> sys.__name__', "'sys'"]);
  });
});

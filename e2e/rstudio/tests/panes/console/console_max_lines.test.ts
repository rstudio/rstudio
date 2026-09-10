// The console scrollback limit applies immediately (#16386).
//
// console_max_lines (Global Options > Console, "Maximum lines of output to
// keep in the console") used to be read once at session start. Shell now
// follows the pref live, and the session keeps its console-actions capacity
// in sync, so lowering or raising the limit affects the next output.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { TIMEOUTS } from '@utils/constants';
import { clearPref, setPref } from '@utils/commands';

const PRINTED_LINES = 200;

async function consoleLineCount(page: Page): Promise<number> {
  const text = await page.locator(CONSOLE_OUTPUT).innerText();
  return text.split('\n').filter((line) => line.length > 0).length;
}

test.describe.serial('Console scrollback limit', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'console_max_lines');
    await consoleActions.clearConsole();
  });

  test('a lower limit trims the scrollback as soon as new output arrives', async ({
    rstudioPage: page,
  }) => {
    await setPref(page, 'console_max_lines', 50);
    await consoleActions.clearConsole();

    await consoleActions.executeInConsole(`cat(sprintf("line %03d", seq_len(${PRINTED_LINES})), sep = "\\n")`);
    await expect(page.locator(CONSOLE_OUTPUT)).toContainText(`line ${PRINTED_LINES}`, {
      timeout: TIMEOUTS.consoleReady,
    });

    // only the tail survives; allow for the echoed command and prompt lines
    await expect.poll(() => consoleLineCount(page)).toBeLessThanOrEqual(60);
    await expect(page.locator(CONSOLE_OUTPUT)).not.toContainText('line 001');
  });

  test('a higher limit keeps everything, with no restart', async ({ rstudioPage: page }) => {
    await setPref(page, 'console_max_lines', 5000);
    await consoleActions.clearConsole();

    await consoleActions.executeInConsole(`cat(sprintf("line %03d", seq_len(${PRINTED_LINES})), sep = "\\n")`);
    await expect(page.locator(CONSOLE_OUTPUT)).toContainText(`line ${PRINTED_LINES}`, {
      timeout: TIMEOUTS.consoleReady,
    });

    await expect(page.locator(CONSOLE_OUTPUT)).toContainText('line 001');
    await expect.poll(() => consoleLineCount(page)).toBeGreaterThanOrEqual(PRINTED_LINES);
  });
});

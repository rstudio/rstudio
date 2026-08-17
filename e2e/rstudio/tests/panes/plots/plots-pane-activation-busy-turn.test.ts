import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand } from '@utils/commands';
import { PLOTS_TAB } from '@pages/plots_pane.page';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import type { Page } from 'playwright';

// Regression test for https://github.com/rstudio/rstudio/issues/18559.
//
// While R is busy, background processing renders pending plot changes
// incrementally (to support animations) -- without activating the Plots pane.
// That render consumes the graphics device's change flag, so the REPL change
// source fired at end of turn used to find no changes and skip activation
// entirely: a turn like `plot(1:10); Sys.sleep(2)` drew a plot the user never
// saw. The same race made the Posit Assistant plots-pane e2e test (#18037)
// fail on slow runners, where the assistant's post-plot processing keeps the
// turn busy past the background-processing gates.
//
// The console repro is deterministic and AI-free: the Sys.sleep keeps the turn
// busy well past the 50ms polled-events throttle and the 50ms change-age gate,
// guaranteeing the mid-turn render happens before the turn ends.

async function isPlotsTabSelected(page: Page): Promise<boolean> {
  return (await page.locator(PLOTS_TAB).getAttribute('aria-selected')) === 'true';
}

test.describe.serial('Plots pane activation at end of a busy turn', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // Close the device so the plot does not leak into later tests, and leave
    // the tabset on Files so each test starts from the deselected state.
    await consoleActions.executeInConsole('graphics.off()', { wait: true });
    await executeCommand(page, 'activateFiles');
  });

  test('a fast console plot activates the Plots pane', async ({ rstudioPage: page }) => {
    // Precondition: Files shares the Plots tabset, so selecting it guarantees
    // Plots is deselected.
    await executeCommand(page, 'activateFiles');
    await expect.poll(() => isPlotsTabSelected(page)).toBe(false);

    await consoleActions.executeInConsole('plot(1:10)', { wait: true });

    await expect.poll(() => isPlotsTabSelected(page), { timeout: 10000 }).toBe(true);
  });

  test('a console plot followed by a busy tail activates the Plots pane', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'activateFiles');
    await expect.poll(() => isPlotsTabSelected(page)).toBe(false);

    await consoleActions.executeInConsole('plot(1:10); Sys.sleep(2)', { wait: true });

    await expect.poll(() => isPlotsTabSelected(page), { timeout: 10000 }).toBe(true);
  });
});

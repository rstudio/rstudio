// Focus Next Pane (F6) and Focus Previous Pane (Shift+F6).

import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { executeCommand } from '@utils/commands';
import { TERMINAL_TAB, XTERM_SELECTOR, killAllTerminals, openTerminal } from '@utils/terminal';
import type { Page } from 'playwright';

const CONSOLE_PANE = '#rstudio_Console_pane';
const ENVIRONMENT_TAB = '#rstudio_workbench_tab_environment';
const LOAD_WORKSPACE_BUTTON = '#rstudio_tb_loadworkspace';

// Pane order follows the pane layout, so walk backward until focus reaches the
// Console pane. Shift+F6 rather than F6: F6 can stop short of the Console pane
// in some layouts. Five panes plus the sidebar bound the walk.
const MAX_PANE_STEPS = 6;

async function focusIsIn(page: Page, selector: string): Promise<boolean> {
  return page.evaluate(
    sel => document.querySelector(sel)?.contains(document.activeElement) ?? false,
    selector,
  );
}

async function previousPaneUntilConsolePane(page: Page): Promise<void> {
  for (let step = 0; step < MAX_PANE_STEPS; step++) {
    await page.keyboard.press('Shift+F6');
    const reached = await expect
      .poll(() => focusIsIn(page, CONSOLE_PANE), { timeout: 2000 })
      .toBe(true)
      .then(() => true, () => false);
    if (reached)
      return;
  }
  throw new Error(`Shift+F6 did not reach the Console pane in ${MAX_PANE_STEPS} steps`);
}

test.describe.serial('Focus adjacent pane', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page).catch(() => {});
    await executeCommand(page, 'activateConsole').catch(() => {});
  });

  // #19183
  test('keeps the Terminal tab selected and focuses it', async ({ rstudioPage: page }) => {
    await openTerminal(page);

    // Clicking the tab leaves focus in the terminal, so focus a pane control.
    await page.locator(ENVIRONMENT_TAB).click();
    await page.locator(LOAD_WORKSPACE_BUTTON).focus();
    await expect.poll(() => focusIsIn(page, CONSOLE_PANE)).toBe(false);

    await previousPaneUntilConsolePane(page);

    await expect(page.locator(TERMINAL_TAB)).toHaveAttribute('aria-selected', 'true');
    await expect
      .poll(() => focusIsIn(page, XTERM_SELECTOR), { timeout: TIMEOUTS.consoleReady })
      .toBe(true);
  });
});

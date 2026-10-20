// Focus Previous Pane (Shift+F6) into the Console pane (#19183).

import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { executeCommand, resetLayoutZoom } from '@utils/commands';
import { CONSOLE_INPUT, CONSOLE_TAB } from '@pages/console_pane.page';
import {
  TERMINAL_TAB,
  XTERM_SELECTOR,
  captureResult,
  killAllTerminals,
  openTerminal,
} from '@utils/terminal';
import type { Page } from 'playwright';

const CONSOLE_PANE = '#rstudio_Console_pane';
const CONSOLE_MIN_BTN = `${CONSOLE_PANE} .rstudio_panel_min_btn_console`;
const BACKGROUND_JOBS_TAB = '#rstudio_workbench_tab_background_jobs';
const BACKGROUND_JOBS_PANEL = '#rstudio_workbench_panel_background_jobs';
const ENVIRONMENT_TAB = '#rstudio_workbench_tab_environment';
const LOAD_WORKSPACE_BUTTON = '#rstudio_tb_loadworkspace';
const PANES = '[id^="rstudio_"][id$="_pane"]';

// A terminal is created by an RPC round trip; watch this long for one to appear.
const NO_NEW_TERMINAL_WINDOW_MS = 3000;

/** The id of the pane holding focus, or '' when focus is outside every pane. */
async function focusedPane(page: Page): Promise<string> {
  return page.evaluate(
    panes => document.activeElement?.closest(panes)?.id ?? '',
    PANES,
  );
}

async function focusIsIn(page: Page, selector: string): Promise<boolean> {
  return page.evaluate(
    sel => document.querySelector(sel)?.contains(document.activeElement) ?? false,
    selector,
  );
}

async function visibleTerminalCount(page: Page): Promise<number> {
  return page.evaluate(
    sel => [...document.querySelectorAll<HTMLElement>(sel)].filter(el => el.offsetWidth > 0).length,
    XTERM_SELECTOR,
  );
}

/** Move focus out of the Console pane, into the Environment pane. */
async function focusEnvironmentPane(page: Page): Promise<void> {
  // Clicking the tab leaves focus in a terminal, so focus a pane control.
  await page.locator(ENVIRONMENT_TAB).click();
  await page.locator(LOAD_WORKSPACE_BUTTON).focus();
  await expect.poll(() => focusedPane(page)).not.toBe(CONSOLE_PANE.slice(1));
}

/**
 * Press Shift+F6 until focus reaches the Console pane. Pane order follows the
 * layout, so walk rather than assume a step count. Each step must move focus
 * to another pane, so a full cycle takes at most one step per pane.
 */
async function previousPaneUntilConsolePane(page: Page): Promise<void> {
  const paneCount = await page.locator(PANES).count();
  for (let step = 0; step < paneCount; step++) {
    const from = await focusedPane(page);
    await page.keyboard.press('Shift+F6');
    await expect
      .poll(() => focusedPane(page), { message: `Shift+F6 did not move focus from ${from}` })
      .not.toBe(from);
    if (await focusedPane(page) === CONSOLE_PANE.slice(1))
      return;
  }
  throw new Error(`Shift+F6 did not reach the Console pane in ${paneCount} steps`);
}

test.describe.serial('Focus adjacent pane', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await resetLayoutZoom(page);
    await killAllTerminals(page);
    await executeCommand(page, 'activateConsole');
    await expect(page.locator(CONSOLE_PANE)).toBeVisible();
  });

  test('keeps the Terminal tab selected and focuses it', async ({ rstudioPage: page }) => {
    await openTerminal(page);
    await focusEnvironmentPane(page);

    await previousPaneUntilConsolePane(page);

    await expect(page.locator(TERMINAL_TAB)).toHaveAttribute('aria-selected', 'true');
    await expect
      .poll(() => focusIsIn(page, XTERM_SELECTOR), { timeout: TIMEOUTS.consoleReady })
      .toBe(true);
  });

  test('keeps the Console tab selected and focuses its input', async ({ rstudioPage: page }) => {
    await openTerminal(page);
    await page.locator(CONSOLE_TAB).click();
    await focusEnvironmentPane(page);

    await previousPaneUntilConsolePane(page);

    await expect(page.locator(CONSOLE_TAB)).toHaveAttribute('aria-selected', 'true');
    await expect.poll(() => focusIsIn(page, CONSOLE_INPUT)).toBe(true);
  });

  test('keeps the Background Jobs tab selected and focuses it', async ({ rstudioPage: page }) => {
    await page.locator(BACKGROUND_JOBS_TAB).click();
    await focusEnvironmentPane(page);

    await previousPaneUntilConsolePane(page);

    await expect(page.locator(BACKGROUND_JOBS_TAB)).toHaveAttribute('aria-selected', 'true');
    await expect.poll(() => focusIsIn(page, BACKGROUND_JOBS_PANEL)).toBe(true);
  });

  test('restores a minimized Console pane', async ({ rstudioPage: page }) => {
    await openTerminal(page);
    await page.locator(CONSOLE_MIN_BTN).click();
    await expect(page.locator(CONSOLE_PANE)).toBeHidden();
    await focusEnvironmentPane(page);

    await previousPaneUntilConsolePane(page);

    await expect(page.locator(CONSOLE_PANE)).toBeVisible();
    await expect(page.locator(TERMINAL_TAB)).toHaveAttribute('aria-selected', 'true');
    await expect
      .poll(() => focusIsIn(page, XTERM_SELECTOR), { timeout: TIMEOUTS.consoleReady })
      .toBe(true);
  });

  test('moves a zoom to the Console pane', async ({ rstudioPage: page }) => {
    await openTerminal(page);
    await focusEnvironmentPane(page);
    await executeCommand(page, 'layoutZoomEnvironment');
    await expect.poll(() => page.locator(CONSOLE_PANE).evaluate(el => (el as HTMLElement).offsetWidth))
      .toBe(0);
    await page.locator(LOAD_WORKSPACE_BUTTON).focus();

    await previousPaneUntilConsolePane(page);

    await expect.poll(() => page.locator(CONSOLE_PANE).evaluate(el => (el as HTMLElement).offsetWidth))
      .toBeGreaterThan(0);
    await expect(page.locator(TERMINAL_TAB)).toHaveAttribute('aria-selected', 'true');
    await expect
      .poll(() => focusIsIn(page, XTERM_SELECTOR), { timeout: TIMEOUTS.consoleReady })
      .toBe(true);
  });

  test('does not start a terminal when the last one has exited', async ({ rstudioPage: page }) => {
    await openTerminal(page);
    await page.locator(XTERM_SELECTOR).click();
    await page.keyboard.type('exit');
    await page.keyboard.press('Enter');
    await expect.poll(() => visibleTerminalCount(page), { timeout: TIMEOUTS.consoleReady }).toBe(0);
    await expect(page.locator(TERMINAL_TAB)).toHaveAttribute('aria-selected', 'true');
    await focusEnvironmentPane(page);

    await previousPaneUntilConsolePane(page);

    // A new terminal would appear after its creation RPC returns; sample for
    // longer than that and fail on the first one seen.
    const deadline = Date.now() + NO_NEW_TERMINAL_WINDOW_MS;
    while (Date.now() < deadline) {
      expect(await visibleTerminalCount(page)).toBe(0);
      await page.waitForTimeout(250);
    }
    expect(await captureResult(page, 'length(rstudioapi::terminalList())')).toBe('0');
  });
});

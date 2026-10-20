// Focus Previous Pane (Shift+F6) into the Console pane (#19183), and Focus
// Next/Previous Pane (F6/Shift+F6) around the sidebar (#19191).

import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { executeCommand, isCommandChecked, resetLayoutZoom } from '@utils/commands';
import { CONSOLE_INPUT, CONSOLE_TAB } from '@pages/console_pane.page';
import {
  TERMINAL_TAB,
  XTERM_SELECTOR,
  captureResult,
  killAllTerminals,
  openTerminal,
} from '@utils/terminal';
import type { Page } from 'playwright';

const CONSOLE_PANE_ID = 'rstudio_Console_pane';
const CONSOLE_PANE = `#${CONSOLE_PANE_ID}`;
const CONSOLE_MIN_BTN = `${CONSOLE_PANE} .rstudio_panel_min_btn_console`;
const BACKGROUND_JOBS_TAB = '#rstudio_workbench_tab_background_jobs';
const BACKGROUND_JOBS_PANEL = '#rstudio_workbench_panel_background_jobs';
const ENVIRONMENT_TAB = '#rstudio_workbench_tab_environment';
const LOAD_WORKSPACE_BUTTON = '#rstudio_tb_loadworkspace';
const PANES = '[id^="rstudio_"][id$="_pane"]';
const SIDEBAR_PANE_ID = 'rstudio_Sidebar_pane';
const SIDEBAR_PANE = `#${SIDEBAR_PANE_ID}`;
// The sidebar's first tab, or its Configure Panes button when it has none --
// the Workbench default, where Posit Assistant is off.
const SIDEBAR_START =
  `${SIDEBAR_PANE} [role="tab"], ${SIDEBAR_PANE} #rstudio_customize_panes`;
const QUADRANT_PANE_IDS = [
  'rstudio_Source_pane',
  CONSOLE_PANE_ID,
  'rstudio_TabSet1_pane',
  'rstudio_TabSet2_pane',
];

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
  await expect.poll(() => focusedPane(page)).not.toBe(CONSOLE_PANE_ID);
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
    if (await focusedPane(page) === CONSOLE_PANE_ID)
      return;
  }
  throw new Error(`Shift+F6 did not reach the Console pane in ${paneCount} steps`);
}

/**
 * Press key until focus is back in the pane it started in, and return the
 * panes visited in order. Each step must move focus to another pane, and a
 * full cycle takes at most one step per pane.
 */
async function cyclePanes(page: Page, key: string): Promise<string[]> {
  const start = await focusedPane(page);
  const visited: string[] = [];
  const maxSteps = QUADRANT_PANE_IDS.length + 1;
  for (let step = 0; step < maxSteps; step++) {
    const from = await focusedPane(page);
    await page.keyboard.press(key);
    await expect
      .poll(() => focusedPane(page), { message: `${key} did not move focus from ${from}` })
      .not.toBe(from);
    const to = await focusedPane(page);
    if (to === start)
      return visited;
    visited.push(to);
  }
  throw new Error(`${key} did not return to ${start} in ${maxSteps} steps: ${visited.join(', ')}`);
}

/**
 * Show or hide the sidebar. Reads the toggleSidebar command's checked state,
 * which follows the saved preference, because a zoom can shut a sidebar that
 * is showing and make it read as hidden.
 */
async function setSidebarVisible(page: Page, visible: boolean): Promise<void> {
  if (await isCommandChecked(page, 'toggleSidebar') !== visible)
    await executeCommand(page, 'toggleSidebar');
  if (visible)
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible();
  else
    await expect(page.locator(SIDEBAR_PANE)).toBeHidden();
}

test.describe.serial('Focus adjacent pane', () => {
  // Showing or hiding the sidebar saves the preference, so put it back the way
  // the suite found it rather than leave later spec files a hidden sidebar.
  let sidebarInitiallyVisible = false;

  test.beforeAll(async ({ rstudioPage: page }) => {
    sidebarInitiallyVisible = await isCommandChecked(page, 'toggleSidebar');
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await resetLayoutZoom(page);
    await setSidebarVisible(page, sidebarInitiallyVisible);
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

  test('skips a hidden sidebar without showing it', async ({ rstudioPage: page }) => {
    await setSidebarVisible(page, false);
    await focusEnvironmentPane(page);

    const forward = await cyclePanes(page, 'F6');
    const backward = await cyclePanes(page, 'Shift+F6');

    expect(forward).toEqual(['rstudio_TabSet2_pane', 'rstudio_Source_pane', CONSOLE_PANE_ID]);
    expect(backward).toEqual([CONSOLE_PANE_ID, 'rstudio_Source_pane', 'rstudio_TabSet2_pane']);
    await expect(page.locator(SIDEBAR_PANE)).toBeHidden();
  });

  test('cycles into and out of a sidebar shown after load', async ({ rstudioPage: page }) => {
    await setSidebarVisible(page, false);
    await setSidebarVisible(page, true);
    await page.locator(SIDEBAR_START).first().focus();
    await expect.poll(() => focusedPane(page)).toBe(SIDEBAR_PANE_ID);

    const forward = await cyclePanes(page, 'F6');
    const backward = await cyclePanes(page, 'Shift+F6');

    expect(forward).toEqual(QUADRANT_PANE_IDS);
    expect(backward).toEqual([...QUADRANT_PANE_IDS].reverse());
  });

  test('skips a sidebar shut by a zoom', async ({ rstudioPage: page }) => {
    await setSidebarVisible(page, true);
    await focusEnvironmentPane(page);
    await executeCommand(page, 'layoutZoomEnvironment');
    await expect.poll(() => page.locator(SIDEBAR_PANE).evaluate(el => (el as HTMLElement).offsetWidth))
      .toBe(0);
    await page.locator(LOAD_WORKSPACE_BUTTON).focus();

    const forward = await cyclePanes(page, 'F6');

    expect(forward).toEqual(['rstudio_TabSet2_pane', 'rstudio_Source_pane', CONSOLE_PANE_ID]);
  });
});

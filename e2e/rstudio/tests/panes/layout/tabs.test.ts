// Workbench tab visibility, selection, and layoutZoomEnvironment.
// Sidebar-width-when-adding-tabs scenarios are exercised by panes.test.ts.

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand, isCommandChecked, resetSourcePaneState } from '@utils/commands';
import type { Page } from 'playwright';

const WORKBENCH_TABS = [
  '#rstudio_workbench_tab_console',
  '#rstudio_workbench_tab_terminal',
  '#rstudio_workbench_tab_background_jobs',
  '#rstudio_workbench_tab_environment',
  '#rstudio_workbench_tab_history',
  '#rstudio_workbench_tab_files',
  '#rstudio_workbench_tab_plots',
  '#rstudio_workbench_tab_connections',
  '#rstudio_workbench_tab_packages',
  '#rstudio_workbench_tab_help',
  '#rstudio_workbench_tab_tutorial',
  '#rstudio_workbench_tab_viewer',
] as const;

const TAB_ENVIRONMENT = '#rstudio_workbench_tab_environment';
const TAB_HISTORY = '#rstudio_workbench_tab_history';
const TAB_FILES = '#rstudio_workbench_tab_files';
const TAB_PLOTS = '#rstudio_workbench_tab_plots';

const ENV_PANEL = '#rstudio_workbench_panel_environment';
const CONSOLE_PANE = '#rstudio_Console_pane';
const TABSET2_PANE = '#rstudio_TabSet2_pane';

async function getOffsetWidth(page: Page, selector: string): Promise<number> {
  return page.locator(selector).evaluate((el) => (el as HTMLElement).offsetWidth);
}

async function getOffsetHeight(page: Page, selector: string): Promise<number> {
  return page.locator(selector).evaluate((el) => (el as HTMLElement).offsetHeight);
}

async function getAriaSelected(page: Page, selector: string): Promise<boolean> {
  return (await page.locator(selector).getAttribute('aria-selected')) === 'true';
}

test.describe('Workbench tabs', () => {
  test('all core tabs are present and visible', async ({ rstudioPage: page }) => {
    for (const selector of WORKBENCH_TABS) {
      const tab = page.locator(selector);
      await expect(tab, `${selector} should be attached`).toHaveCount(1);
      expect(await getOffsetWidth(page, selector), `${selector} should have non-zero width`).toBeGreaterThan(0);
      expect(await getOffsetHeight(page, selector), `${selector} should have non-zero height`).toBeGreaterThan(0);
    }
  });

  test('clicking a tab updates aria-selected on the destination and the previously selected tab', async ({ rstudioPage: page }) => {
    // Default state: Environment selected in TabSet1, Files selected in TabSet2.
    expect(await getAriaSelected(page, TAB_ENVIRONMENT)).toBe(true);
    expect(await getAriaSelected(page, TAB_HISTORY)).toBe(false);

    await page.locator(TAB_HISTORY).click();
    await expect.poll(() => getAriaSelected(page, TAB_HISTORY)).toBe(true);
    expect(await getAriaSelected(page, TAB_ENVIRONMENT)).toBe(false);

    await page.locator(TAB_ENVIRONMENT).click();
    await expect.poll(() => getAriaSelected(page, TAB_ENVIRONMENT)).toBe(true);

    await page.locator(TAB_PLOTS).click();
    await expect.poll(() => getAriaSelected(page, TAB_PLOTS)).toBe(true);
    expect(await getAriaSelected(page, TAB_FILES)).toBe(false);

    // Restore the Files tab so later tests inherit the default layout.
    await page.locator(TAB_FILES).click();
    await expect.poll(() => getAriaSelected(page, TAB_FILES)).toBe(true);
  });

  // layoutZoomEnvironment is a separate command from layoutZoomLeftColumn /
  // layoutZoomRightColumn (covered by panes.test.ts) -- it expands just the
  // environment panel rather than a whole column.
  test('layoutZoomEnvironment zooms the environment pane and toggles back', async ({ rstudioPage: page }) => {
    const initialEnvWidth = await getOffsetWidth(page, ENV_PANEL);
    const initialEnvHeight = await getOffsetHeight(page, ENV_PANEL);
    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const initialTabSet2Width = await getOffsetWidth(page, TABSET2_PANE);
    const initialTabSet2Height = await getOffsetHeight(page, TABSET2_PANE);

    expect(initialEnvWidth).toBeGreaterThan(0);
    expect(initialEnvHeight).toBeGreaterThan(0);
    expect(initialConsoleWidth).toBeGreaterThan(0);
    expect(initialTabSet2Width).toBeGreaterThan(0);

    await executeCommand(page, 'layoutZoomEnvironment');

    await expect.poll(
      async () => (await getOffsetWidth(page, ENV_PANEL)) > initialEnvWidth * 1.5
        && (await getOffsetWidth(page, CONSOLE_PANE)) < 50,
      { timeout: 5000 },
    ).toBe(true);

    await expect.poll(() => isCommandChecked(page, 'layoutZoomEnvironment')).toBe(true);

    const zoomedEnvHeight = await getOffsetHeight(page, ENV_PANEL);
    expect(zoomedEnvHeight).toBeGreaterThan(initialEnvHeight * 1.5);

    const zoomedTabSet2Width = await getOffsetWidth(page, TABSET2_PANE);
    const zoomedTabSet2Height = await getOffsetHeight(page, TABSET2_PANE);
    expect(zoomedTabSet2Width < 50 || zoomedTabSet2Height < 50).toBe(true);

    await executeCommand(page, 'layoutZoomEnvironment');

    await expect.poll(
      async () => (await getOffsetWidth(page, ENV_PANEL)) < initialEnvWidth * 1.5
        && (await getOffsetWidth(page, CONSOLE_PANE)) > 50,
      { timeout: 5000 },
    ).toBe(true);

    await expect.poll(() => isCommandChecked(page, 'layoutZoomEnvironment')).toBe(false);

    const tol = (actual: number, expected: number) => Math.abs(actual - expected) / expected < 0.1;
    expect(tol(await getOffsetWidth(page, ENV_PANEL), initialEnvWidth)).toBe(true);
    expect(tol(await getOffsetHeight(page, ENV_PANEL), initialEnvHeight)).toBe(true);
    expect(tol(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth)).toBe(true);
    expect(tol(await getOffsetWidth(page, TABSET2_PANE), initialTabSet2Width)).toBe(true);
    expect(tol(await getOffsetHeight(page, TABSET2_PANE), initialTabSet2Height)).toBe(true);

    // layoutZoomEnvironment creates an untitled source doc as a side-effect.
    // resetSourcePaneState (not closeAllSourceDocs) so the pane keeps a
    // single Untitled placeholder rather than draining to zero tabs and
    // triggering the HIDE-animation race (#17738) that breaks the next test.
    await resetSourcePaneState(page);
  });
});

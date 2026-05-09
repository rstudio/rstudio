import { test as base, expect } from '@playwright/test';
import { launchRStudio, shutdownRStudio, type DesktopSession } from '@fixtures/desktop.fixture';
import { sleep } from '@utils/constants';
import { typeInConsole } from '@pages/console_pane.page';
import type { Page } from 'playwright';

// Pane Layout preferences dialog selectors
const PANE_LAYOUT_TAB = '#rstudio_label_pane_layout_options';
const PANE_LAYOUT_PANEL = '#rstudio_label_pane_layout_options_panel';
const OPTIONS_OK = '#rstudio_preferences_confirm';

// Pane layout dialog containers (where tab checkboxes live)
const LAYOUT_SIDEBAR = '#rstudio_pane_layout_sidebar';
const LAYOUT_RIGHT_TOP = '#rstudio_pane_layout_right_top';
const LAYOUT_RIGHT_BOTTOM = '#rstudio_pane_layout_right_bottom';

// Workbench pane containers (the actual panes in the IDE)
const SIDEBAR_PANE = '#rstudio_Sidebar_pane';
const TABSET1_PANE = '#rstudio_TabSet1_pane';
const TABSET2_PANE = '#rstudio_TabSet2_pane';

/**
 * Open Global Options and navigate to the Pane Layout tab.
 */
async function openPaneLayoutOptions(page: Page): Promise<void> {
  await typeInConsole(page, ".rs.api.executeCommand('showOptions')");
  await page.waitForSelector(OPTIONS_OK, { timeout: 15000 });
  await page.locator(PANE_LAYOUT_TAB).click();
  await expect(page.locator(PANE_LAYOUT_PANEL)).toBeVisible({ timeout: 5000 });
  await sleep(1000);
}

/**
 * In the Pane Layout dialog, toggle a tab's checkbox within a container.
 */
async function toggleTabInContainer(page: Page, containerSelector: string, tabLabel: string): Promise<void> {
  const container = page.locator(containerSelector);
  const checkbox = container.locator(`label:has-text("${tabLabel}") input[type="checkbox"], label:has-text("${tabLabel}") >> xpath=../input`);
  const label = container.locator(`text="${tabLabel}"`);
  if (await checkbox.count() > 0) {
    await checkbox.first().click();
  } else {
    await label.click();
  }
  await sleep(500);
}

/**
 * Check if a tab's checkbox is checked in a specific container.
 */
async function isTabCheckedInContainer(page: Page, containerSelector: string, tabLabel: string): Promise<boolean> {
  const container = page.locator(containerSelector);
  const checkbox = container.locator(`label:has-text("${tabLabel}") input[type="checkbox"]`).first();
  if (await checkbox.count() === 0) return false;
  return checkbox.isChecked();
}

/**
 * Determine which workbench pane contains a given tab.
 * Returns "sidebar", "tabSet1", "tabSet2", or "not found".
 */
async function findTabLocation(page: Page, tabSelector: string): Promise<string> {
  for (const [paneSelector, name] of [
    [SIDEBAR_PANE, 'sidebar'],
    [TABSET1_PANE, 'tabSet1'],
    [TABSET2_PANE, 'tabSet2'],
  ] as const) {
    if (await page.locator(`${paneSelector} ${tabSelector}`).count() > 0) return name;
  }
  return 'not found';
}

/**
 * Ensure a tab is checked in the target container and unchecked in the source.
 */
async function moveTab(page: Page, tabLabel: string, fromContainer: string, toContainer: string): Promise<void> {
  if (await isTabCheckedInContainer(page, fromContainer, tabLabel)) {
    await toggleTabInContainer(page, fromContainer, tabLabel);
  }
  if (!(await isTabCheckedInContainer(page, toContainer, tabLabel))) {
    await toggleTabInContainer(page, toContainer, tabLabel);
  }
}

/**
 * Pane location persistence — rstudio/rstudio#17177
 *
 * Verifies that pane assignments persist across RStudio restarts.
 * Moves Posit Assistant from sidebar to a pane AND Help from a pane
 * to the sidebar in a single session, then restarts and checks both
 * persisted.
 */
base.describe('Pane location persistence - #17177', { tag: ['@desktop_only'] }, () => {
  let session: DesktopSession;

  base('Pane assignments persist after restart', async () => {
    // --- Phase 1: Launch and rearrange panes ---
    session = await launchRStudio();
    const page = session.page;

    await openPaneLayoutOptions(page);

    // Reset to defaults so we start from a known state
    await page.locator('#rstudio_pane_layout_reset_link').click();
    await sleep(1000);

    // Ensure sidebar is visible
    const sidebarCheckbox = page.locator('#rstudio_pane_layout_sidebar_visible');
    if (!(await sidebarCheckbox.isChecked())) {
      await sidebarCheckbox.click();
      await sleep(500);
    }

    // Move Posit Assistant into TabSet1 (right-top)
    await moveTab(page, 'Posit Assistant', LAYOUT_SIDEBAR, LAYOUT_RIGHT_TOP);

    // Move Help into sidebar (Help is in TabSet2 / right-bottom by default)
    await moveTab(page, 'Help', LAYOUT_RIGHT_BOTTOM, LAYOUT_SIDEBAR);

    await page.locator(OPTIONS_OK).click();
    await expect(page.locator(OPTIONS_OK)).toBeHidden({ timeout: 15000 });
    await sleep(2000);

    // Verify both moved correctly before restart
    const chatBeforeRestart = await findTabLocation(page, '#rstudio_workbench_tab_posit_assistant');
    const helpBeforeRestart = await findTabLocation(page, '#rstudio_workbench_tab_help');
    console.log(`Before restart — Posit Assistant: ${chatBeforeRestart}, Help: ${helpBeforeRestart}`);
    expect(chatBeforeRestart).toBe('tabSet1');
    expect(helpBeforeRestart).toBe('sidebar');

    // --- Phase 2: Shutdown and relaunch ---
    console.log('Shutting down RStudio...');
    await shutdownRStudio(session);

    console.log('Relaunching RStudio...');
    session = await launchRStudio();
    const page2 = session.page;

    // --- Phase 3: Verify persistence ---
    const chatAfterRestart = await findTabLocation(page2, '#rstudio_workbench_tab_posit_assistant');
    const helpAfterRestart = await findTabLocation(page2, '#rstudio_workbench_tab_help');
    console.log(`After restart — Posit Assistant: ${chatAfterRestart}, Help: ${helpAfterRestart}`);
    expect(chatAfterRestart).toBe('tabSet1');
    expect(helpAfterRestart).toBe('sidebar');

    // --- Cleanup: restore defaults (Posit Assistant → sidebar, Help → TabSet2) ---
    await openPaneLayoutOptions(page2);
    await moveTab(page2, 'Posit Assistant', LAYOUT_RIGHT_TOP, LAYOUT_SIDEBAR);
    await moveTab(page2, 'Help', LAYOUT_SIDEBAR, LAYOUT_RIGHT_BOTTOM);

    await page2.locator(OPTIONS_OK).click();
    await expect(page2.locator(OPTIONS_OK)).toBeHidden({ timeout: 15000 });
    await sleep(1000);

    await shutdownRStudio(session);
  });
});

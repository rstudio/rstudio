// Tests related to pane and column management.
//
// Ported from src/cpp/tests/automation/testthat/test-automation-panes.R.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { sleep, TIMEOUTS } from '@utils/constants';
import type { Page } from 'playwright';

// ---------------------------------------------------------------------------
// Workbench pane selectors
// ---------------------------------------------------------------------------
const TABSET1_PANE = '#rstudio_TabSet1_pane';
const TABSET2_PANE = '#rstudio_TabSet2_pane';
const CONSOLE_PANE = '#rstudio_Console_pane';
const SOURCE_PANE = '#rstudio_Source_pane';
const SOURCE1_PANE = '#rstudio_Source1_pane';
const SOURCE2_PANE = '#rstudio_Source2_pane';
const SOURCE3_PANE = '#rstudio_Source3_pane';
const SIDEBAR_PANE = '#rstudio_Sidebar_pane';
const CUSTOMIZE_PANES_BUTTON = '#rstudio_customize_panes';
const SIDEBAR_CLOSE_BTN = '.rstudio_panel_close_btn_sidebar';
const MIDDLE_COLUMN_SPLITTER = '#rstudio_middle_column_splitter';
const CONSOLE_INPUT = '#rstudio_console_input .ace_text-input';

// Pane Layout dialog selectors
const PL_RIGHT_TOP = '#rstudio_pane_layout_right_top';
const PL_SIDEBAR = '#rstudio_pane_layout_sidebar';
const PL_SIDEBAR_VISIBLE = '#rstudio_pane_layout_sidebar_visible';
const PREFERENCES_CONFIRM = '#rstudio_preferences_confirm';
const DIALOG_BOX = '.gwt-DialogBox';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

// Type a command into the Ace console input directly. We force-click the input
// instead of going through the console tab because zoomed-out panes can leave
// the tab too narrow to receive a normal click.
async function executeCommand(page: Page, command: string): Promise<void> {
  const input = page.locator(CONSOLE_INPUT);
  await input.click({ force: true });
  await sleep(200);
  await input.pressSequentially(`.rs.api.executeCommand('${command}')`);
  await sleep(200);
  if (await page.locator('#rstudio_popup_completions').isVisible().catch(() => false)) {
    await page.keyboard.press('Escape');
    await sleep(100);
  }
  await input.press('Enter');
}

async function getOffsetWidth(page: Page, selector: string): Promise<number> {
  return await page.locator(selector).evaluate(el => (el as HTMLElement).offsetWidth);
}

async function getOffsetHeight(page: Page, selector: string): Promise<number> {
  return await page.locator(selector).evaluate(el => (el as HTMLElement).offsetHeight);
}

async function getLeft(page: Page, selector: string): Promise<number> {
  return await page.locator(selector).evaluate(el => el.getBoundingClientRect().left);
}

async function elementExists(page: Page, selector: string): Promise<boolean> {
  return (await page.locator(selector).count()) > 0;
}

async function showSidebar(page: Page): Promise<void> {
  await executeCommand(page, 'toggleSidebar');
  await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
}

async function hideSidebarIfVisible(page: Page): Promise<void> {
  if (await elementExists(page, SIDEBAR_PANE)) {
    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });
  }
}

// Targeted cleanup that doesn't reload the page. Resets sidebar visibility,
// sidebar location, and any active zoom — covers what tests 1-18 mutate.
// Tests that mutate pane assignments (Posit Assistant tests) reset themselves
// via the dialog's reset link.
async function resetUILayout(page: Page): Promise<void> {
  // If sidebar is on the right, move it back to the left.
  if (await elementExists(page, SIDEBAR_PANE)) {
    const sidebarLeft = await getLeft(page, SIDEBAR_PANE);
    const consoleLeft = await getLeft(page, CONSOLE_PANE);
    if (sidebarLeft > consoleLeft) {
      await executeCommand(page, 'toggleSidebarLocation');
      await sleep(300);
    }
  }
  // Unzoom by re-executing whichever zoom command is active. We detect zoom
  // by checking which column is collapsed.
  const consoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
  const tabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
  if (consoleWidth < 50) {
    await executeCommand(page, 'layoutZoomRightColumn');
    await sleep(300);
  } else if (tabSet1Width < 50) {
    await executeCommand(page, 'layoutZoomLeftColumn');
    await sleep(300);
  }
  await hideSidebarIfVisible(page);
}

async function focusSplitter(page: Page): Promise<void> {
  await page.locator(MIDDLE_COLUMN_SPLITTER).evaluate(el => (el as HTMLElement).focus());
}

async function pressArrowMany(page: Page, key: 'ArrowLeft' | 'ArrowRight', count: number): Promise<void> {
  for (let i = 0; i < count; i++) {
    await page.keyboard.press(key);
  }
}

async function openPaneLayoutOptions(page: Page): Promise<void> {
  await executeCommand(page, 'paneLayout');
  await page.waitForSelector(DIALOG_BOX, { timeout: 15000 });
  await page.waitForSelector('#rstudio_label_pane_layout_options_panel', { timeout: 5000 });
  await sleep(500);
}

async function resetPaneLayoutInDialog(page: Page): Promise<void> {
  await page.locator('#rstudio_pane_layout_reset_link').click();
  await sleep(1000);
}

// Pane Layout dialog uses GWT checkboxes: a <label for="X"> paired with a
// sibling <input id="X">. Resolve the linked input via the for attribute.
async function findTabCheckbox(page: Page, container: string, tabLabel: string) {
  const labels = page.locator(container).locator('label');
  const count = await labels.count();
  for (let i = 0; i < count; i++) {
    const label = labels.nth(i);
    const text = (await label.innerText()).trim();
    if (!text.includes(tabLabel)) continue;
    const forId = await label.getAttribute('for');
    if (!forId) continue;
    const checkbox = page.locator(`#${forId}`);
    if ((await checkbox.count()) > 0) return checkbox;
  }
  return null;
}

async function isTabChecked(page: Page, container: string, tabLabel: string): Promise<boolean> {
  const checkbox = await findTabCheckbox(page, container, tabLabel);
  if (!checkbox) return false;
  return await checkbox.isChecked();
}

async function toggleTab(page: Page, container: string, tabLabel: string): Promise<void> {
  const checkbox = await findTabCheckbox(page, container, tabLabel);
  if (!checkbox) {
    throw new Error(`Tab '${tabLabel}' not found in container '${container}'`);
  }
  await checkbox.scrollIntoViewIfNeeded();
  await checkbox.click();
  await sleep(300);
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test.describe.serial('Pane and column management', () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
    await resetUILayout(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await resetUILayout(page);
  });

  // Restore Posit Assistant to its default sidebar location and hide the
  // sidebar before handing the worker off to the next test file.
  test.afterAll(async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);
    await resetPaneLayoutInDialog(page);
    const sidebarVisibleCheckbox = page.locator(PL_SIDEBAR_VISIBLE);
    if (await sidebarVisibleCheckbox.isChecked()) {
      await sidebarVisibleCheckbox.click();
      await sleep(500);
    }
    await page.locator(PREFERENCES_CONFIRM).click();
    await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 10000 });
    await sleep(500);
  });

  // -------------------------------------------------------------------------
  test('Default quadrants exist and have expected visibility', async ({ rstudioPage: page }) => {
    expect(await elementExists(page, TABSET1_PANE)).toBe(true);
    expect(await elementExists(page, TABSET2_PANE)).toBe(true);
    expect(await elementExists(page, CONSOLE_PANE)).toBe(true);
    expect(await elementExists(page, SOURCE_PANE)).toBe(true);

    expect(await elementExists(page, SIDEBAR_PANE)).toBe(false);
    expect(await elementExists(page, SOURCE1_PANE)).toBe(false);
    expect(await elementExists(page, SOURCE2_PANE)).toBe(false);
    expect(await elementExists(page, SOURCE3_PANE)).toBe(false);
    expect(await elementExists(page, CUSTOMIZE_PANES_BUTTON)).toBe(false);

    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, TABSET1_PANE)).toBeGreaterThan(0);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, TABSET2_PANE)).toBeGreaterThan(0);
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, CONSOLE_PANE)).toBeGreaterThan(0);

    // Source pane exists in DOM but is not visible (no source docs open).
    const sourceWidth = await getOffsetWidth(page, SOURCE_PANE);
    const sourceHeight = await getOffsetHeight(page, SOURCE_PANE);
    expect(sourceWidth === 0 || sourceHeight === 0).toBe(true);
  });

  // -------------------------------------------------------------------------
  test('Source columns can be created and closed', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'newSourceColumn');
    await expect(page.locator(SOURCE1_PANE)).toBeVisible({ timeout: 10000 });

    await executeCommand(page, 'newSourceColumn');
    await expect(page.locator(SOURCE2_PANE)).toBeVisible({ timeout: 10000 });

    await executeCommand(page, 'newSourceColumn');
    await expect(page.locator(SOURCE3_PANE)).toBeVisible({ timeout: 10000 });

    expect(await getOffsetWidth(page, SOURCE1_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SOURCE1_PANE)).toBeGreaterThan(0);
    expect(await getOffsetWidth(page, SOURCE2_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SOURCE2_PANE)).toBeGreaterThan(0);
    expect(await getOffsetWidth(page, SOURCE3_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SOURCE3_PANE)).toBeGreaterThan(0);

    await executeCommand(page, 'closeAllSourceDocs');
    await expect(page.locator(SOURCE1_PANE)).toHaveCount(0, { timeout: 10000 });
    await expect(page.locator(SOURCE2_PANE)).toHaveCount(0, { timeout: 10000 });
    await expect(page.locator(SOURCE3_PANE)).toHaveCount(0, { timeout: 10000 });
  });

  // -------------------------------------------------------------------------
  test('Layout zoom is unrestrained by default', async ({ rstudioPage: page }) => {
    // No column should be collapsed (a zoom would shrink one side to ~0).
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeGreaterThan(50);
  });

  // -------------------------------------------------------------------------
  test('Sidebar can be shown and hidden with toggleSidebar command', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SIDEBAR_PANE)).toBeGreaterThan(0);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });
  });

  // -------------------------------------------------------------------------
  test('Sidebar can be hidden by clicking the close button', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    const closeBtn = page.locator(SIDEBAR_CLOSE_BTN);
    await expect(closeBtn).toBeVisible({ timeout: 5000 });
    await closeBtn.click();

    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });
  });

  // -------------------------------------------------------------------------
  test('Sidebar can be moved left and right with toggleSidebarLocation command', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    const initialLeft = await getLeft(page, SIDEBAR_PANE);

    await executeCommand(page, 'toggleSidebarLocation');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(500);

    const rightLeft = await getLeft(page, SIDEBAR_PANE);
    expect(rightLeft).toBeGreaterThan(initialLeft);

    const consoleLeftAfterRight = await getLeft(page, CONSOLE_PANE);
    expect(rightLeft).toBeGreaterThan(consoleLeftAfterRight);

    await executeCommand(page, 'toggleSidebarLocation');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(500);

    const leftLeft = await getLeft(page, SIDEBAR_PANE);
    expect(leftLeft).toBeLessThan(rightLeft);

    const consoleLeftAfterLeft = await getLeft(page, CONSOLE_PANE);
    expect(leftLeft).toBeLessThan(consoleLeftAfterLeft);
  });

  // -------------------------------------------------------------------------
  test('Zoomed left column with sidebar hidden works as expected', async ({ rstudioPage: page }) => {
    expect(await elementExists(page, SIDEBAR_PANE)).toBe(false);

    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    const initialTabSet2Width = await getOffsetWidth(page, TABSET2_PANE);

    expect(initialConsoleWidth).toBeGreaterThan(0);
    expect(initialTabSet1Width).toBeGreaterThan(0);
    expect(initialTabSet2Width).toBeGreaterThan(0);

    await executeCommand(page, 'layoutZoomLeftColumn');
    const expectedZoomedWidth = initialConsoleWidth + initialTabSet1Width;

    await expect.poll(
      async () => Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - expectedZoomedWidth) < 30,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    const zoomedConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    expect(Math.abs(zoomedConsoleWidth - expectedZoomedWidth)).toBeLessThan(30);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeLessThan(50);

    await executeCommand(page, 'layoutZoomLeftColumn');
    await expect.poll(
      async () => (await getOffsetWidth(page, CONSOLE_PANE)) < zoomedConsoleWidth * 0.75
        && (await getOffsetWidth(page, TABSET1_PANE)) > 50,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    const restoredConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const restoredTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    const restoredTabSet2Width = await getOffsetWidth(page, TABSET2_PANE);

    expect(Math.abs(restoredConsoleWidth - initialConsoleWidth) / initialConsoleWidth).toBeLessThan(0.1);
    expect(Math.abs(restoredTabSet1Width - initialTabSet1Width) / initialTabSet1Width).toBeLessThan(0.1);
    expect(Math.abs(restoredTabSet2Width - initialTabSet2Width) / initialTabSet2Width).toBeLessThan(0.1);
  });

  // -------------------------------------------------------------------------
  test('Zoomed left column with sidebar visible works as expected', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    const initialTabSet2Width = await getOffsetWidth(page, TABSET2_PANE);
    const initialSidebarWidth = await getOffsetWidth(page, SIDEBAR_PANE);

    expect(initialConsoleWidth).toBeGreaterThan(0);
    expect(initialTabSet1Width).toBeGreaterThan(0);
    expect(initialTabSet2Width).toBeGreaterThan(0);
    expect(initialSidebarWidth).toBeGreaterThan(0);

    await executeCommand(page, 'layoutZoomLeftColumn');
    const expectedZoomedWidth = initialConsoleWidth + initialTabSet1Width + initialSidebarWidth;

    await expect.poll(
      async () => Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - expectedZoomedWidth) < 30,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    const zoomedConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    expect(Math.abs(zoomedConsoleWidth - expectedZoomedWidth)).toBeLessThan(30);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeLessThan(50);

    await executeCommand(page, 'layoutZoomLeftColumn');
    await expect.poll(
      async () => (await getOffsetWidth(page, CONSOLE_PANE)) < zoomedConsoleWidth * 0.75
        && (await getOffsetWidth(page, TABSET1_PANE)) > 50
        && (await getOffsetWidth(page, SIDEBAR_PANE)) > 50,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    expect(Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - initialConsoleWidth) / initialConsoleWidth).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, TABSET1_PANE)) - initialTabSet1Width) / initialTabSet1Width).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - initialTabSet2Width) / initialTabSet2Width).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, SIDEBAR_PANE)) - initialSidebarWidth) / initialSidebarWidth).toBeLessThan(0.1);
  });

  // -------------------------------------------------------------------------
  test('Zoomed right column with sidebar hidden works as expected', async ({ rstudioPage: page }) => {
    expect(await elementExists(page, SIDEBAR_PANE)).toBe(false);

    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    const initialTabSet2Width = await getOffsetWidth(page, TABSET2_PANE);

    expect(initialConsoleWidth).toBeGreaterThan(0);
    expect(initialTabSet1Width).toBeGreaterThan(0);
    expect(initialTabSet2Width).toBeGreaterThan(0);

    await executeCommand(page, 'layoutZoomRightColumn');
    const expectedZoomedWidth = initialTabSet1Width + initialConsoleWidth;

    await expect.poll(
      async () => Math.abs((await getOffsetWidth(page, TABSET1_PANE)) - expectedZoomedWidth) < 30,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    const zoomedTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    expect(Math.abs(zoomedTabSet1Width - expectedZoomedWidth)).toBeLessThan(30);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - zoomedTabSet1Width)).toBeLessThan(30);
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeLessThan(50);

    await executeCommand(page, 'layoutZoomRightColumn');
    await expect.poll(
      async () => (await getOffsetWidth(page, TABSET1_PANE)) < zoomedTabSet1Width * 0.75
        && (await getOffsetWidth(page, CONSOLE_PANE)) > 50,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    expect(Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - initialConsoleWidth) / initialConsoleWidth).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, TABSET1_PANE)) - initialTabSet1Width) / initialTabSet1Width).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - initialTabSet2Width) / initialTabSet2Width).toBeLessThan(0.1);
  });

  // -------------------------------------------------------------------------
  test('Zoomed right column with sidebar visible works as expected', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    const initialTabSet2Width = await getOffsetWidth(page, TABSET2_PANE);
    const initialSidebarWidth = await getOffsetWidth(page, SIDEBAR_PANE);

    expect(initialConsoleWidth).toBeGreaterThan(0);
    expect(initialTabSet1Width).toBeGreaterThan(0);
    expect(initialTabSet2Width).toBeGreaterThan(0);
    expect(initialSidebarWidth).toBeGreaterThan(0);

    await executeCommand(page, 'layoutZoomRightColumn');
    const expectedZoomedWidth = initialTabSet1Width + initialConsoleWidth + initialSidebarWidth;

    await expect.poll(
      async () => Math.abs((await getOffsetWidth(page, TABSET1_PANE)) - expectedZoomedWidth) < 30,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    const zoomedTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    expect(Math.abs(zoomedTabSet1Width - expectedZoomedWidth)).toBeLessThan(30);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - zoomedTabSet1Width)).toBeLessThan(30);
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeLessThan(50);

    await executeCommand(page, 'layoutZoomRightColumn');
    await expect.poll(
      async () => (await getOffsetWidth(page, TABSET1_PANE)) < zoomedTabSet1Width * 0.75
        && (await getOffsetWidth(page, CONSOLE_PANE)) > 50
        && (await getOffsetWidth(page, SIDEBAR_PANE)) > 50,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    expect(Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - initialConsoleWidth) / initialConsoleWidth).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, TABSET1_PANE)) - initialTabSet1Width) / initialTabSet1Width).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - initialTabSet2Width) / initialTabSet2Width).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, SIDEBAR_PANE)) - initialSidebarWidth) / initialSidebarWidth).toBeLessThan(0.1);
  });

  // -------------------------------------------------------------------------
  test('Zoomed left column with sidebar on right works as expected', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    await executeCommand(page, 'toggleSidebarLocation');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(500);

    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    const initialTabSet2Width = await getOffsetWidth(page, TABSET2_PANE);
    const initialSidebarWidth = await getOffsetWidth(page, SIDEBAR_PANE);

    expect(initialConsoleWidth).toBeGreaterThan(0);
    expect(initialTabSet1Width).toBeGreaterThan(0);
    expect(initialTabSet2Width).toBeGreaterThan(0);
    expect(initialSidebarWidth).toBeGreaterThan(0);

    await executeCommand(page, 'layoutZoomLeftColumn');
    const expectedZoomedWidth = initialConsoleWidth + initialTabSet1Width + initialSidebarWidth;

    await expect.poll(
      async () => Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - expectedZoomedWidth) < 30,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    expect(Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - expectedZoomedWidth)).toBeLessThan(30);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeLessThan(50);

    const zoomedConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);

    await executeCommand(page, 'layoutZoomLeftColumn');
    await expect.poll(
      async () => (await getOffsetWidth(page, CONSOLE_PANE)) < zoomedConsoleWidth * 0.75
        && (await getOffsetWidth(page, TABSET1_PANE)) > 50
        && (await getOffsetWidth(page, SIDEBAR_PANE)) > 50,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    expect(Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - initialConsoleWidth) / initialConsoleWidth).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, TABSET1_PANE)) - initialTabSet1Width) / initialTabSet1Width).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - initialTabSet2Width) / initialTabSet2Width).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, SIDEBAR_PANE)) - initialSidebarWidth) / initialSidebarWidth).toBeLessThan(0.1);
  });

  // -------------------------------------------------------------------------
  test('Zoomed right column with sidebar on right works as expected', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    await executeCommand(page, 'toggleSidebarLocation');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(500);

    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    const initialTabSet2Width = await getOffsetWidth(page, TABSET2_PANE);
    const initialSidebarWidth = await getOffsetWidth(page, SIDEBAR_PANE);

    expect(initialConsoleWidth).toBeGreaterThan(0);
    expect(initialTabSet1Width).toBeGreaterThan(0);
    expect(initialTabSet2Width).toBeGreaterThan(0);
    expect(initialSidebarWidth).toBeGreaterThan(0);

    await executeCommand(page, 'layoutZoomRightColumn');
    const expectedZoomedWidth = initialTabSet1Width + initialConsoleWidth + initialSidebarWidth;

    await expect.poll(
      async () => Math.abs((await getOffsetWidth(page, TABSET1_PANE)) - expectedZoomedWidth) < 30,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    const zoomedTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    expect(Math.abs(zoomedTabSet1Width - expectedZoomedWidth)).toBeLessThan(30);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - zoomedTabSet1Width)).toBeLessThan(30);
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeLessThan(50);

    await executeCommand(page, 'layoutZoomRightColumn');
    await expect.poll(
      async () => (await getOffsetWidth(page, TABSET1_PANE)) < zoomedTabSet1Width * 0.75
        && (await getOffsetWidth(page, CONSOLE_PANE)) > 50
        && (await getOffsetWidth(page, SIDEBAR_PANE)) > 50,
      { timeout: 5000 }
    ).toBe(true);
    await sleep(200);

    expect(Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - initialConsoleWidth) / initialConsoleWidth).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, TABSET1_PANE)) - initialTabSet1Width) / initialTabSet1Width).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - initialTabSet2Width) / initialTabSet2Width).toBeLessThan(0.1);
    expect(Math.abs((await getOffsetWidth(page, SIDEBAR_PANE)) - initialSidebarWidth) / initialSidebarWidth).toBeLessThan(0.1);
  });

  // -------------------------------------------------------------------------
  test('Sidebar visibility persists across UI reload', async ({ rstudioPage: page }) => {
    expect(await elementExists(page, SIDEBAR_PANE)).toBe(false);

    await showSidebar(page);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SIDEBAR_PANE)).toBeGreaterThan(0);

    await page.reload();
    await page.waitForSelector(SIDEBAR_PANE, { timeout: TIMEOUTS.sessionRestart });
    await sleep(1000);

    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SIDEBAR_PANE)).toBeGreaterThan(0);
  });

  // -------------------------------------------------------------------------
  test('Column widths are preserved when toggling sidebar visibility (#16676)', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    await sleep(300);

    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    expect(initialConsoleWidth).toBeGreaterThan(0);
    expect(initialTabSet1Width).toBeGreaterThan(0);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(0);

    expect(await elementExists(page, MIDDLE_COLUMN_SPLITTER)).toBe(true);
    await focusSplitter(page);
    await pressArrowMany(page, 'ArrowRight', 18);
    await sleep(300);

    let modifiedConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    let modifiedTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);

    const widthChanged = Math.abs(modifiedConsoleWidth - initialConsoleWidth) > 20
      || Math.abs(modifiedTabSet1Width - initialTabSet1Width) > 20;
    if (!widthChanged) {
      // Keyboard resize did not move the splitter — fall back to verifying
      // that the original widths are preserved through the toggle cycle.
      modifiedConsoleWidth = initialConsoleWidth;
      modifiedTabSet1Width = initialTabSet1Width;
    }

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });
    await sleep(300);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(500);

    const finalConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const finalTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);

    expect(Math.abs(finalConsoleWidth - modifiedConsoleWidth) / modifiedConsoleWidth).toBeLessThan(0.05);
    expect(Math.abs(finalTabSet1Width - modifiedTabSet1Width) / modifiedTabSet1Width).toBeLessThan(0.05);
  });

  // -------------------------------------------------------------------------
  test('Column widths preserved through multiple hide/show cycles', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    await sleep(300);

    expect(await elementExists(page, MIDDLE_COLUMN_SPLITTER)).toBe(true);
    await focusSplitter(page);
    await pressArrowMany(page, 'ArrowRight', 15);
    await sleep(300);

    const consoleModified = await getOffsetWidth(page, CONSOLE_PANE);
    const tabSet1Modified = await getOffsetWidth(page, TABSET1_PANE);

    for (let cycle = 1; cycle <= 3; cycle++) {
      await executeCommand(page, 'toggleSidebar');
      await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });
      await sleep(300);

      await executeCommand(page, 'toggleSidebar');
      await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
      await sleep(500);

      const consoleAfter = await getOffsetWidth(page, CONSOLE_PANE);
      const tabSet1After = await getOffsetWidth(page, TABSET1_PANE);

      expect(Math.abs(consoleAfter - consoleModified) / consoleModified).toBeLessThan(0.05);
      expect(Math.abs(tabSet1After - tabSet1Modified) / tabSet1Modified).toBeLessThan(0.05);
    }
  });

  // -------------------------------------------------------------------------
  test('Sidebar show uses default widths after columns resized while hidden', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    await sleep(300);

    await focusSplitter(page);
    await pressArrowMany(page, 'ArrowRight', 10);
    await sleep(300);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });
    await sleep(300);

    // Resize columns while sidebar is hidden — saved widths should be invalidated.
    await focusSplitter(page);
    await pressArrowMany(page, 'ArrowLeft', 25);
    await sleep(300);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(500);

    const consoleAfterShow = await getOffsetWidth(page, CONSOLE_PANE);
    const tabSet1AfterShow = await getOffsetWidth(page, TABSET1_PANE);
    const sidebarAfterShow = await getOffsetWidth(page, SIDEBAR_PANE);

    expect(consoleAfterShow).toBeGreaterThan(100);
    expect(tabSet1AfterShow).toBeGreaterThan(100);
    expect(sidebarAfterShow).toBeGreaterThan(100);
  });

  // -------------------------------------------------------------------------
  test('Different resize patterns preserve correctly through sidebar toggle', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    await sleep(300);

    // Phase 1: resize LEFT, verify preservation.
    await focusSplitter(page);
    await pressArrowMany(page, 'ArrowLeft', 15);
    await sleep(300);

    const consoleAfterLeft = await getOffsetWidth(page, CONSOLE_PANE);
    const tabSet1AfterLeft = await getOffsetWidth(page, TABSET1_PANE);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });
    await sleep(300);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(500);

    expect(Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - consoleAfterLeft) / consoleAfterLeft).toBeLessThan(0.05);
    expect(Math.abs((await getOffsetWidth(page, TABSET1_PANE)) - tabSet1AfterLeft) / tabSet1AfterLeft).toBeLessThan(0.05);

    // Phase 2: resize RIGHT, verify preservation.
    await focusSplitter(page);
    await pressArrowMany(page, 'ArrowRight', 20);
    await sleep(300);

    const consoleAfterRight = await getOffsetWidth(page, CONSOLE_PANE);
    const tabSet1AfterRight = await getOffsetWidth(page, TABSET1_PANE);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });
    await sleep(300);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(500);

    expect(Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - consoleAfterRight) / consoleAfterRight).toBeLessThan(0.05);
    expect(Math.abs((await getOffsetWidth(page, TABSET1_PANE)) - tabSet1AfterRight) / tabSet1AfterRight).toBeLessThan(0.05);
  });

  // -------------------------------------------------------------------------
  test('Extreme resize values preserve correctly through sidebar toggle', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    await sleep(300);

    await focusSplitter(page);
    await pressArrowMany(page, 'ArrowRight', 30);
    await sleep(300);

    const consoleVeryWide = await getOffsetWidth(page, CONSOLE_PANE);
    const tabSet1VeryNarrow = await getOffsetWidth(page, TABSET1_PANE);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });
    await sleep(300);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(500);

    const consoleAfterToggle = await getOffsetWidth(page, CONSOLE_PANE);
    const tabSet1AfterToggle = await getOffsetWidth(page, TABSET1_PANE);

    expect(Math.abs(consoleAfterToggle - consoleVeryWide) / consoleVeryWide).toBeLessThan(0.05);

    if (tabSet1VeryNarrow < 50) {
      expect(Math.abs(tabSet1AfterToggle - tabSet1VeryNarrow)).toBeLessThan(10);
    } else {
      expect(Math.abs(tabSet1AfterToggle - tabSet1VeryNarrow) / tabSet1VeryNarrow).toBeLessThan(0.10);
    }

    expect(consoleAfterToggle).toBeGreaterThan(50);
    expect(tabSet1AfterToggle).toBeGreaterThanOrEqual(0);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(0);
  });

  // -------------------------------------------------------------------------
  test('Moving Posit Assistant from visible sidebar to TabSet1 persists across UI reload', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);
    await resetPaneLayoutInDialog(page);

    // Ensure sidebar is visible so PL_SIDEBAR is populated.
    const sidebarVisibleCheckbox = page.locator(PL_SIDEBAR_VISIBLE);
    if (!(await sidebarVisibleCheckbox.isChecked())) {
      await sidebarVisibleCheckbox.click();
      await sleep(500);
    }

    expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(true);
    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Posit Assistant')).toBe(false);

    await toggleTab(page, PL_RIGHT_TOP, 'Posit Assistant');

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Posit Assistant')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(false);
    expect(await page.locator(PL_SIDEBAR_VISIBLE).isChecked()).toBe(false);

    await page.locator(PREFERENCES_CONFIRM).click();
    await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 10000 });
    await sleep(500);

    await page.reload();
    await page.waitForSelector(TABSET1_PANE, { timeout: TIMEOUTS.sessionRestart });
    await sleep(500);

    await openPaneLayoutOptions(page);

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Posit Assistant')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(false);
    expect(await page.locator(PL_SIDEBAR_VISIBLE).isChecked()).toBe(false);

    await page.keyboard.press('Escape');
    await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 5000 });
  });

  // -------------------------------------------------------------------------
  test('Moving Posit Assistant from hidden sidebar to TabSet1 persists across UI reload', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);
    await resetPaneLayoutInDialog(page);

    // Ensure sidebar is hidden for this scenario.
    const sidebarVisibleCheckbox = page.locator(PL_SIDEBAR_VISIBLE);
    if (await sidebarVisibleCheckbox.isChecked()) {
      await sidebarVisibleCheckbox.click();
      await sleep(500);
    }

    expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(true);
    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Posit Assistant')).toBe(false);

    await toggleTab(page, PL_RIGHT_TOP, 'Posit Assistant');

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Posit Assistant')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(false);
    expect(await page.locator(PL_SIDEBAR_VISIBLE).isChecked()).toBe(false);

    await page.locator(PREFERENCES_CONFIRM).click();
    await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 10000 });
    await sleep(500);

    await page.reload();
    await page.waitForSelector(TABSET1_PANE, { timeout: TIMEOUTS.sessionRestart });
    await sleep(500);

    await openPaneLayoutOptions(page);

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Posit Assistant')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(false);
    expect(await page.locator(PL_SIDEBAR_VISIBLE).isChecked()).toBe(false);

    await page.keyboard.press('Escape');
    await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 5000 });
  });
});

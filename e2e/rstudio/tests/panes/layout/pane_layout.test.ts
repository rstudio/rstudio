import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, TIMEOUTS } from '@utils/constants';
import { executeInConsole, CONSOLE_INPUT } from '@pages/console_pane.page';
import { executeCommand } from '@utils/commands';
import type { Page } from 'playwright';

// Pane layout dialog: quadrant containers
const PL_LEFT_TOP = '#rstudio_pane_layout_left_top';
const PL_LEFT_BOTTOM = '#rstudio_pane_layout_left_bottom';
const PL_RIGHT_TOP = '#rstudio_pane_layout_right_top';
const PL_RIGHT_BOTTOM = '#rstudio_pane_layout_right_bottom';
const PL_SIDEBAR = '#rstudio_pane_layout_sidebar';

// Pane layout dialog: dropdowns
const PL_LEFT_TOP_SELECT = '#rstudio_pane_layout_left_top_select';
const PL_LEFT_BOTTOM_SELECT = '#rstudio_pane_layout_left_bottom_select';
const PL_RIGHT_TOP_SELECT = '#rstudio_pane_layout_right_top_select';
const PL_RIGHT_BOTTOM_SELECT = '#rstudio_pane_layout_right_bottom_select';
const PL_SIDEBAR_SELECT = '#rstudio_pane_layout_sidebar_select';

// Pane layout dialog: other controls
const PL_SIDEBAR_VISIBLE = '#rstudio_pane_layout_sidebar_visible';
const PL_RESET_LINK = '#rstudio_pane_layout_reset_link';
const PL_ADD_COLUMN_BUTTON = '#rstudio_pane_layout_add_column_button';
const PL_OK = '#rstudio_preferences_confirm';
const PL_PANEL = '#rstudio_label_pane_layout_options_panel';
const DIALOG_BOX = '.gwt-DialogBox';

// Workbench panes (after applying changes)
const SIDEBAR_PANE = '#rstudio_Sidebar_pane';
const SOURCE1_PANE = '#rstudio_Source1_pane';
const CONSOLE_PANE = '#rstudio_Console_pane';
const TABSET2_PANE = '#rstudio_TabSet2_pane';

const ALL_TAB_NAMES = [
  'Environment', 'History', 'Connections', 'Build', 'VCS', 'Tutorial',
  'Files', 'Plots', 'Packages', 'Help', 'Viewer', 'Presentations', 'Posit Assistant',
];

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

async function openPaneLayoutOptions(page: Page): Promise<void> {
  await executeCommand(page, 'paneLayout');
  await page.waitForSelector(DIALOG_BOX, { timeout: TIMEOUTS.consoleReady });
  await page.waitForSelector(PL_PANEL, { timeout: 5000 });
  await sleep(500);
}

async function closePaneLayoutOptions(page: Page): Promise<void> {
  await page.keyboard.press('Escape');
  await page.waitForSelector(DIALOG_BOX, { state: 'detached', timeout: 10000 });
}

// Best-effort dialog dismissal for cleanup hooks. Unlike closePaneLayoutOptions,
// this tolerates an already-closed dialog without failing the test.
async function dismissDialogIfOpen(page: Page): Promise<void> {
  if (await page.locator(DIALOG_BOX).count() === 0) return;
  await page.keyboard.press('Escape');
  await page.waitForSelector(DIALOG_BOX, { state: 'detached', timeout: 10000 });
}

// Reload-based reset; the GWT command calls window.reload() once prefs save.
// Uses a window-scoped sentinel so we can detect a successful reload (the new
// document won't have the property) versus a no-op (still present).
async function resetUILayout(page: Page): Promise<void> {
  const sentinel = `__pl_reset_${Date.now()}_${Math.random()}`;
  await page.evaluate((s) => { (window as unknown as Record<string, true>)[s] = true; }, sentinel);
  await executeCommand(page, 'restoreDefaultPaneAndTabLayoutNoPrompt');
  await page.waitForFunction(
    (s) => !(s in (window as unknown as Record<string, unknown>)),
    sentinel,
    { timeout: 30000 },
  );
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: 30000 });
}

async function getQuadrantDropdownText(page: Page, quadrant: string): Promise<string> {
  return page.locator(`${quadrant} select`).evaluate(
    (el) => {
      const sel = el as HTMLSelectElement;
      return sel.options[sel.selectedIndex]?.text ?? '';
    },
  );
}

async function selectDropdownOption(page: Page, quadrant: string, optionText: string): Promise<void> {
  await page.locator(`${quadrant} select`).evaluate(
    (el, text) => {
      const sel = el as HTMLSelectElement;
      for (let i = 0; i < sel.options.length; i++) {
        if (sel.options[i].text === text) {
          sel.selectedIndex = i;
          sel.dispatchEvent(new Event('change'));
          return;
        }
      }
      throw new Error(`Option not found: '${text}'`);
    },
    optionText,
  );
  await sleep(1000);
}

async function getDropdownOptionTexts(page: Page, selector: string): Promise<string[]> {
  return page.locator(selector).evaluate(
    (el) => Array.from((el as HTMLSelectElement).options).map(o => o.text),
  );
}

async function getDropdownSelectedIndex(page: Page, selector: string): Promise<number> {
  return page.locator(selector).evaluate((el) => (el as HTMLSelectElement).selectedIndex);
}

// Find the input id for a tab checkbox by matching its label text inside the quadrant.
async function findTabCheckboxId(page: Page, quadrant: string, tab: string): Promise<string | null> {
  return page.evaluate(
    ({ q, t }) => {
      const root = document.querySelector(q);
      if (!root) return null;
      const labels = Array.from(root.querySelectorAll('label')) as HTMLLabelElement[];
      for (const lbl of labels) {
        if ((lbl.innerText ?? '').includes(t)) {
          return lbl.getAttribute('for');
        }
      }
      return null;
    },
    { q: quadrant, t: tab },
  );
}

async function isTabChecked(page: Page, quadrant: string, tab: string): Promise<boolean> {
  const id = await findTabCheckboxId(page, quadrant, tab);
  if (!id) throw new Error(`Tab '${tab}' not found in quadrant '${quadrant}'`);
  return page.locator(`#${id}`).isChecked();
}

async function getTabCheckedState(page: Page, quadrant: string, tabs: string[]): Promise<Record<string, boolean>> {
  return page.evaluate(
    ({ q, ts }) => {
      const root = document.querySelector(q);
      if (!root) throw new Error(`Quadrant not found: ${q}`);
      const labels = Array.from(root.querySelectorAll('label')) as HTMLLabelElement[];
      const result: Record<string, boolean> = {};
      for (const lbl of labels) {
        const text = lbl.innerText ?? '';
        for (const t of ts) {
          if (t in result) continue;
          if (text.includes(t)) {
            const forId = lbl.getAttribute('for');
            if (!forId) continue;
            const cb = document.getElementById(forId) as HTMLInputElement | null;
            if (cb) result[t] = cb.checked;
          }
        }
      }
      const missing = ts.filter(t => !(t in result));
      if (missing.length) {
        throw new Error(`Tabs not found in '${q}': ${missing.join(', ')}`);
      }
      return result;
    },
    { q: quadrant, ts: tabs },
  );
}

async function toggleTab(page: Page, quadrant: string, tab: string): Promise<void> {
  const id = await findTabCheckboxId(page, quadrant, tab);
  if (!id) throw new Error(`Tab '${tab}' not found in quadrant '${quadrant}'`);
  await page.locator(`#${id}`).scrollIntoViewIfNeeded();
  await sleep(100);
  await page.locator(`#${id}`).click();
  await sleep(500);
}

// Expected tabs are a subset of the dropdown text -- RStudio Pro adds
// extra tabs (e.g. "Databricks") that should not cause failures.
async function verifyQuadrantTabs(page: Page, quadrant: string, expected: string[]): Promise<void> {
  const text = await getQuadrantDropdownText(page, quadrant);
  const actual = text.split(', ');
  for (const tab of expected) {
    expect(actual, `Expected '${tab}' in quadrant '${quadrant}' (got: "${text}")`).toContain(tab);
  }
}

async function verifyDropdownOptions(
  page: Page,
  selector: string,
  expectedTexts: string[],
  expectedSelectedIndex?: number,
): Promise<void> {
  expect(expectedTexts.length, 'expectedTexts must contain at least 1 string').toBeGreaterThan(0);
  const actualTexts = await getDropdownOptionTexts(page, selector);
  expect(actualTexts.length, `Expected ${expectedTexts.length} options but found ${actualTexts.length}`)
    .toBe(expectedTexts.length);
  for (let i = 0; i < expectedTexts.length; i++) {
    expect(actualTexts[i], `Position ${i + 1}: expected '${expectedTexts[i]}' but got '${actualTexts[i]}'`)
      .toContain(expectedTexts[i]);
  }
  if (expectedSelectedIndex != null) {
    const selected = await getDropdownSelectedIndex(page, selector);
    expect(selected, `Selected option index mismatch`).toBe(expectedSelectedIndex - 1);
  }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test.describe.serial('Pane Layout dialog (#test-automation-pane-layout)', { tag: ['@serial'] }, () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    // If a test threw before its own close, dismiss any leftover dialog so the
    // next test in this serial chain starts clean.
    await dismissDialogIfOpen(page);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await dismissDialogIfOpen(page);
    await resetUILayout(page);
  });

  test('default quadrant configuration is correct', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    await expect(page.locator(PL_LEFT_TOP)).toBeAttached();
    await expect(page.locator(PL_LEFT_BOTTOM)).toBeAttached();
    await expect(page.locator(PL_RIGHT_TOP)).toBeAttached();
    await expect(page.locator(PL_RIGHT_BOTTOM)).toBeAttached();
    await expect(page.locator(PL_SIDEBAR)).toBeAttached();

    expect(await getQuadrantDropdownText(page, PL_LEFT_TOP)).toBe('Source');
    expect(await getQuadrantDropdownText(page, PL_LEFT_BOTTOM)).toBe('Console');

    await verifyQuadrantTabs(page, PL_RIGHT_TOP,
      ['Environment', 'History', 'Connections', 'Build', 'VCS', 'Tutorial']);
    await verifyQuadrantTabs(page, PL_RIGHT_BOTTOM,
      ['Files', 'Plots', 'Packages', 'Help', 'Viewer', 'Presentations']);
    await verifyQuadrantTabs(page, PL_SIDEBAR, ['Sidebar on Left']);

    await expect(page.locator(PL_SIDEBAR_VISIBLE)).not.toBeChecked();

    await closePaneLayoutOptions(page);
  });

  test('quadrant dropdowns offer all options with correct selection', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    const quadrantOptions = ['Source', 'Console', 'Environment', 'Files'];
    await verifyDropdownOptions(page, PL_LEFT_TOP_SELECT, quadrantOptions, 1);
    await verifyDropdownOptions(page, PL_LEFT_BOTTOM_SELECT, quadrantOptions, 2);
    await verifyDropdownOptions(page, PL_RIGHT_TOP_SELECT, quadrantOptions, 3);
    await verifyDropdownOptions(page, PL_RIGHT_BOTTOM_SELECT, quadrantOptions, 4);

    await verifyDropdownOptions(page, PL_SIDEBAR_SELECT, ['Sidebar on Left', 'Sidebar on Right'], 1);

    await closePaneLayoutOptions(page);
  });

  test('quadrant swapping via dropdown works correctly', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    const sourceInitial = await getQuadrantDropdownText(page, PL_LEFT_TOP);
    const consoleInitial = await getQuadrantDropdownText(page, PL_LEFT_BOTTOM);
    const upperRightInitial = await getQuadrantDropdownText(page, PL_RIGHT_TOP);
    const lowerRightInitial = await getQuadrantDropdownText(page, PL_RIGHT_BOTTOM);
    const sidebarInitial = await getQuadrantDropdownText(page, PL_SIDEBAR);

    // Swap Source and Console by selecting Console in the Source dropdown
    await selectDropdownOption(page, PL_LEFT_TOP, consoleInitial);
    expect(await getQuadrantDropdownText(page, PL_LEFT_TOP)).toBe(consoleInitial);
    expect(await getQuadrantDropdownText(page, PL_LEFT_BOTTOM)).toBe(sourceInitial);

    // Swap TabSet1 and TabSet2 by selecting TabSet2's text in TabSet1's dropdown
    await selectDropdownOption(page, PL_RIGHT_TOP, lowerRightInitial);
    expect(await getQuadrantDropdownText(page, PL_RIGHT_TOP)).toBe(lowerRightInitial);
    expect(await getQuadrantDropdownText(page, PL_RIGHT_BOTTOM)).toBe(upperRightInitial);

    // Swap lower-left with upper-right (now upperRightAfter == lowerRightInitial)
    const upperRightAfter1 = await getQuadrantDropdownText(page, PL_RIGHT_TOP);
    await selectDropdownOption(page, PL_LEFT_BOTTOM, upperRightAfter1);

    // Swap lower-right with upper-left (upper-left currently holds consoleInitial)
    await selectDropdownOption(page, PL_RIGHT_BOTTOM, consoleInitial);

    expect(await getQuadrantDropdownText(page, PL_RIGHT_TOP)).toBe(sourceInitial);
    expect(await getQuadrantDropdownText(page, PL_RIGHT_BOTTOM)).toBe(consoleInitial);
    expect(await getQuadrantDropdownText(page, PL_LEFT_TOP)).toBe(upperRightInitial);
    expect(await getQuadrantDropdownText(page, PL_LEFT_BOTTOM)).toBe(lowerRightInitial);
    expect(await getQuadrantDropdownText(page, PL_SIDEBAR)).toBe(sidebarInitial);

    await closePaneLayoutOptions(page);
  });

  test('TabSet1 displays correct default checked tabs', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    const states = await getTabCheckedState(page, PL_RIGHT_TOP, ALL_TAB_NAMES);

    for (const tab of ['Environment', 'History', 'Connections', 'Build', 'VCS', 'Tutorial']) {
      expect(states[tab], `${tab} should be checked in TabSet1`).toBe(true);
    }
    for (const tab of ['Files', 'Plots', 'Packages', 'Help', 'Viewer', 'Presentations', 'Posit Assistant']) {
      expect(states[tab], `${tab} should be unchecked in TabSet1`).toBe(false);
    }

    await closePaneLayoutOptions(page);
  });

  test('TabSet2 displays correct default checked tabs', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    const states = await getTabCheckedState(page, PL_RIGHT_BOTTOM, ALL_TAB_NAMES);

    for (const tab of ['Files', 'Plots', 'Packages', 'Help', 'Viewer', 'Presentations']) {
      expect(states[tab], `${tab} should be checked in TabSet2`).toBe(true);
    }
    for (const tab of ['Environment', 'History', 'Connections', 'Build', 'VCS', 'Tutorial', 'Posit Assistant']) {
      expect(states[tab], `${tab} should be unchecked in TabSet2`).toBe(false);
    }

    await closePaneLayoutOptions(page);
  });

  test('Sidebar displays correct default checked tabs', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    const states = await getTabCheckedState(page, PL_SIDEBAR, ALL_TAB_NAMES);

    expect(states['Posit Assistant'], 'Posit Assistant should be checked in Sidebar').toBe(true);
    for (const tab of ALL_TAB_NAMES) {
      if (tab === 'Posit Assistant') continue;
      expect(states[tab], `${tab} should be unchecked in Sidebar`).toBe(false);
    }

    await closePaneLayoutOptions(page);
  });

  test('checking unchecked tab in one TabSet unchecks it in the other', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Files')).toBe(true);

    await toggleTab(page, PL_RIGHT_TOP, 'Files');

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Files')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Files')).toBe(false);

    await toggleTab(page, PL_RIGHT_BOTTOM, 'Files');

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Files')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(false);

    await closePaneLayoutOptions(page);
  });

  test('checking unchecked tab in TabSet1 unchecks it in the Sidebar', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Files')).toBe(true);

    await toggleTab(page, PL_SIDEBAR, 'Files');

    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(true);
    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Files')).toBe(false);

    await toggleTab(page, PL_RIGHT_TOP, 'Files');

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Files')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Files')).toBe(false);

    await toggleTab(page, PL_RIGHT_BOTTOM, 'Files');

    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Files')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Files')).toBe(false);

    await closePaneLayoutOptions(page);
  });

  test('clicking checked tab unchecks it in both TabSets (hides the tab)', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Environment')).toBe(true);
    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Environment')).toBe(false);

    await toggleTab(page, PL_RIGHT_TOP, 'Environment');

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Environment')).toBe(false);
    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Environment')).toBe(false);

    await toggleTab(page, PL_RIGHT_TOP, 'Environment');

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Environment')).toBe(true);
    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Environment')).toBe(false);

    await closePaneLayoutOptions(page);
  });

  test('all tabs can be moved to one TabSet leaving the other empty', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    const initialTabSet1 = ['Environment', 'History', 'Connections', 'Build', 'VCS', 'Tutorial'];
    const initialTabSet2 = ['Files', 'Plots', 'Packages', 'Help', 'Viewer', 'Presentations'];
    await verifyQuadrantTabs(page, PL_RIGHT_TOP, initialTabSet1);
    await verifyQuadrantTabs(page, PL_RIGHT_BOTTOM, initialTabSet2);

    const tabsToMove = [...initialTabSet2];
    for (const tab of tabsToMove) {
      await toggleTab(page, PL_RIGHT_TOP, tab);
    }

    const topState = await getTabCheckedState(page, PL_RIGHT_TOP, tabsToMove);
    const bottomState = await getTabCheckedState(page, PL_RIGHT_BOTTOM, tabsToMove);
    for (const tab of tabsToMove) {
      expect(topState[tab], `${tab} should be checked in TabSet1`).toBe(true);
      expect(bottomState[tab], `${tab} should not be checked in TabSet2`).toBe(false);
    }

    for (const tab of tabsToMove) {
      await toggleTab(page, PL_RIGHT_BOTTOM, tab);
    }

    await verifyQuadrantTabs(page, PL_RIGHT_TOP, initialTabSet1);
    await verifyQuadrantTabs(page, PL_RIGHT_BOTTOM, initialTabSet2);

    await closePaneLayoutOptions(page);
  });

  test('TabSet quadrants swap while keeping tab configurations', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    const tabset1Initial = await getQuadrantDropdownText(page, PL_RIGHT_TOP);
    const tabset2Initial = await getQuadrantDropdownText(page, PL_RIGHT_BOTTOM);

    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Environment')).toBe(true);
    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Files')).toBe(true);

    await selectDropdownOption(page, PL_RIGHT_TOP, tabset2Initial);

    expect(await getQuadrantDropdownText(page, PL_RIGHT_TOP)).toBe(tabset2Initial);
    expect(await getQuadrantDropdownText(page, PL_RIGHT_BOTTOM)).toBe(tabset1Initial);

    expect(await isTabChecked(page, PL_RIGHT_BOTTOM, 'Environment')).toBe(true);
    expect(await isTabChecked(page, PL_RIGHT_TOP, 'Files')).toBe(true);

    await selectDropdownOption(page, PL_RIGHT_TOP, tabset1Initial);

    await closePaneLayoutOptions(page);
  });

  test('reset link restores all pane layout settings to defaults', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    await expect(page.locator(PL_SIDEBAR_VISIBLE)).not.toBeChecked();
    await page.locator(PL_SIDEBAR_VISIBLE).click();
    await expect(page.locator(PL_SIDEBAR_VISIBLE)).toBeChecked();

    await selectDropdownOption(page, PL_SIDEBAR, 'Sidebar on Right');
    expect(await getQuadrantDropdownText(page, PL_SIDEBAR)).toBe('Sidebar on Right');

    await toggleTab(page, PL_SIDEBAR, 'Files');
    await toggleTab(page, PL_SIDEBAR, 'Environment');
    await toggleTab(page, PL_SIDEBAR, 'History');
    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Environment')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'History')).toBe(true);

    await selectDropdownOption(page, PL_LEFT_TOP, 'Console');
    expect(await getQuadrantDropdownText(page, PL_LEFT_TOP)).toBe('Console');

    await expect(page.locator(PL_RESET_LINK)).toBeAttached();
    await page.locator(PL_RESET_LINK).click();
    await sleep(800);

    await expect(page.locator(PL_SIDEBAR_VISIBLE)).not.toBeChecked();
    expect(await getQuadrantDropdownText(page, PL_SIDEBAR)).toBe('Sidebar on Left');

    expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(true);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Environment')).toBe(false);
    expect(await isTabChecked(page, PL_SIDEBAR, 'History')).toBe(false);

    expect(await getQuadrantDropdownText(page, PL_LEFT_TOP)).toBe('Source');
    expect(await getQuadrantDropdownText(page, PL_LEFT_BOTTOM)).toBe('Console');

    await verifyQuadrantTabs(page, PL_RIGHT_TOP,
      ['Environment', 'History', 'Connections', 'Build', 'VCS', 'Tutorial']);
    await verifyQuadrantTabs(page, PL_RIGHT_BOTTOM,
      ['Files', 'Plots', 'Packages', 'Help', 'Viewer', 'Presentations']);

    await closePaneLayoutOptions(page);
  });

  test('sidebar visibility checkbox auto-updates based on tab assignments', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);

    await expect(page.locator(PL_SIDEBAR_VISIBLE)).not.toBeChecked();
    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Environment')).toBe(false);

    await toggleTab(page, PL_SIDEBAR, 'Files');
    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(true);
    await expect(page.locator(PL_SIDEBAR_VISIBLE)).toBeChecked();

    await toggleTab(page, PL_SIDEBAR, 'Environment');
    expect(await isTabChecked(page, PL_SIDEBAR, 'Environment')).toBe(true);
    await expect(page.locator(PL_SIDEBAR_VISIBLE)).toBeChecked();

    await toggleTab(page, PL_SIDEBAR, 'Files');
    expect(await isTabChecked(page, PL_SIDEBAR, 'Files')).toBe(false);
    expect(await isTabChecked(page, PL_SIDEBAR, 'Environment')).toBe(true);
    await expect(page.locator(PL_SIDEBAR_VISIBLE)).toBeChecked();

    await toggleTab(page, PL_SIDEBAR, 'Environment');
    expect(await isTabChecked(page, PL_SIDEBAR, 'Environment')).toBe(false);

    await toggleTab(page, PL_SIDEBAR, 'Posit Assistant');
    expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(false);

    await expect(page.locator(PL_SIDEBAR_VISIBLE)).not.toBeChecked();

    // User can still manually toggle visibility for an empty sidebar.
    await page.locator(PL_SIDEBAR_VISIBLE).click();
    await expect(page.locator(PL_SIDEBAR_VISIBLE)).toBeChecked();
    await page.locator(PL_SIDEBAR_VISIBLE).click();
    await expect(page.locator(PL_SIDEBAR_VISIBLE)).not.toBeChecked();

    await closePaneLayoutOptions(page);
  });

  test('add column preserves layout when sidebar visible on left', async ({ rstudioPage: page }) => {
    await resetUILayout(page);

    // Show the sidebar (left is the default)
    await executeCommand(page, 'toggleSidebar');
    await page.waitForSelector(SIDEBAR_PANE, { timeout: 15000 });

    await openPaneLayoutOptions(page);
    await page.locator(PL_ADD_COLUMN_BUTTON).click();
    await sleep(300);
    await page.locator(PL_OK).click();
    await page.waitForSelector(DIALOG_BOX, { state: 'detached', timeout: 15000 });
    await sleep(300);

    await page.waitForSelector(SOURCE1_PANE, { timeout: 15000 });

    const sidebarWidth = await page.locator(SIDEBAR_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);
    const sourceWidth = await page.locator(SOURCE1_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);
    const consoleWidth = await page.locator(CONSOLE_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);
    const tabSetWidth = await page.locator(TABSET2_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);

    expect(sidebarWidth, `Sidebar width should be >= 100px, got: ${sidebarWidth}`).toBeGreaterThanOrEqual(100);
    expect(sourceWidth, `Source column width should be >= 100px, got: ${sourceWidth}`).toBeGreaterThanOrEqual(100);
    expect(consoleWidth, `Console width should be >= 100px, got: ${consoleWidth}`).toBeGreaterThanOrEqual(100);
    expect(tabSetWidth, `TabSet width should be >= 100px, got: ${tabSetWidth}`).toBeGreaterThanOrEqual(100);

    await resetUILayout(page);
  });

  test('add column preserves layout when sidebar visible on right', async ({ rstudioPage: page }) => {
    await resetUILayout(page);

    await openPaneLayoutOptions(page);
    await page.locator(PL_SIDEBAR_VISIBLE).click();
    await selectDropdownOption(page, PL_SIDEBAR, 'Sidebar on Right');
    await page.locator(PL_ADD_COLUMN_BUTTON).click();
    await sleep(300);
    await page.locator(PL_OK).click();
    await page.waitForSelector(DIALOG_BOX, { state: 'detached', timeout: 15000 });
    await sleep(300);

    await page.waitForSelector(SIDEBAR_PANE, { timeout: 15000 });
    await page.waitForSelector(SOURCE1_PANE, { timeout: 15000 });

    const sidebarWidth = await page.locator(SIDEBAR_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);
    const sourceWidth = await page.locator(SOURCE1_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);
    const consoleWidth = await page.locator(CONSOLE_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);
    const tabSetWidth = await page.locator(TABSET2_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);

    expect(sidebarWidth, `Sidebar width should be >= 100px, got: ${sidebarWidth}`).toBeGreaterThanOrEqual(100);
    expect(sourceWidth, `Source column width should be >= 100px, got: ${sourceWidth}`).toBeGreaterThanOrEqual(100);
    expect(consoleWidth, `Console width should be >= 100px, got: ${consoleWidth}`).toBeGreaterThanOrEqual(100);
    expect(tabSetWidth, `TabSet width should be >= 100px, got: ${tabSetWidth}`).toBeGreaterThanOrEqual(100);

    await resetUILayout(page);
  });

  test('add column preserves layout when sidebar not visible', async ({ rstudioPage: page }) => {
    await resetUILayout(page);

    if ((await page.locator(SIDEBAR_PANE).count()) > 0) {
      await executeCommand(page, 'toggleSidebar');
      await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 10000 });
    }

    await openPaneLayoutOptions(page);
    await page.locator(PL_ADD_COLUMN_BUTTON).click();
    await sleep(300);
    await page.locator(PL_OK).click();
    await page.waitForSelector(DIALOG_BOX, { state: 'detached', timeout: 15000 });
    await sleep(300);

    await page.waitForSelector(SOURCE1_PANE, { timeout: 15000 });

    await expect(page.locator(SIDEBAR_PANE), 'Sidebar should not be visible').toHaveCount(0);
    const sourceWidth = await page.locator(SOURCE1_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);
    const consoleWidth = await page.locator(CONSOLE_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);
    const tabSetWidth = await page.locator(TABSET2_PANE).evaluate((el) => (el as HTMLElement).offsetWidth);

    expect(sourceWidth, `Source column width should be >= 100px, got: ${sourceWidth}`).toBeGreaterThanOrEqual(100);
    expect(consoleWidth, `Console width should be >= 100px, got: ${consoleWidth}`).toBeGreaterThanOrEqual(100);
    expect(tabSetWidth, `TabSet width should be >= 100px, got: ${tabSetWidth}`).toBeGreaterThanOrEqual(100);

    await resetUILayout(page);
  });
});

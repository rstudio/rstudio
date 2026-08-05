// Tests related to pane and column management.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { sleep, TIMEOUTS } from '@utils/constants';
import { executeCommand, isCommandChecked } from '@utils/commands';
import { PLOTS_TAB } from '@pages/plots_pane.page';
import type { Locator, Page } from 'playwright';

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
const SIDEBAR_MAX_BTN = '.rstudio_panel_max_btn_sidebar';
const MIDDLE_COLUMN_SPLITTER = '#rstudio_middle_column_splitter';
// Pane header maximize button (ClassIds.PANEL_MAX_BTN + idSafeString(paneName)),
// which doubles as the "restore" button once the pane is EXCLUSIVE. Scoped to
// the normal frame: MinimizedWindowFrame builds a button with the same class,
// so an unscoped selector matches two elements.
const CONSOLE_MAX_BTN = `${CONSOLE_PANE} .rstudio_panel_max_btn_console`;

// Pane Layout dialog selectors
const PL_RIGHT_TOP = '#rstudio_pane_layout_right_top';
const PL_SIDEBAR = '#rstudio_pane_layout_sidebar';
const PL_SIDEBAR_VISIBLE = '#rstudio_pane_layout_sidebar_visible';
const PREFERENCES_CONFIRM = '#rstudio_preferences_confirm';
const DIALOG_BOX = '.gwt-DialogBox';

// Splitter resizes that don't move the splitter at all should fail loudly —
// otherwise the preservation tests degenerate to no-op cycles.
const RESIZE_MIN_DELTA_PX = 20;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

async function getOffsetWidth(page: Page, selector: string): Promise<number> {
  return await page.locator(selector).evaluate(el => (el as HTMLElement).offsetWidth);
}

async function getOffsetHeight(page: Page, selector: string): Promise<number> {
  return await page.locator(selector).evaluate(el => (el as HTMLElement).offsetHeight);
}

async function getLeft(page: Page, selector: string): Promise<number> {
  return await page.locator(selector).evaluate(el => el.getBoundingClientRect().left);
}

// Wait until a pane's offsetWidth has settled: two consecutive samples agree
// and the width is at least `min`. The offsetWidth observation is the most
// direct DOM signal we have for "GWT has finished laying out this pane" --
// the previous pattern (sleep(layoutSettle) + immediate read) lost races on
// slow CI because relayout regularly outlasts the 300ms wait. Returns the
// settled width.
async function waitForStableWidth(
  page: Page,
  selector: string,
  options: { min?: number; timeout?: number } = {},
): Promise<number> {
  const { min = 1, timeout = 5000 } = options;
  let prev = -1;
  await expect.poll(
    async () => {
      const w = await getOffsetWidth(page, selector);
      const settled = w >= min && w === prev;
      prev = w;
      return settled;
    },
    { timeout, intervals: [50, 100, 150] },
  ).toBe(true);
  return prev;
}

async function elementExists(page: Page, selector: string): Promise<boolean> {
  return (await page.locator(selector).count()) > 0;
}

async function isPlotsTabSelected(page: Page): Promise<boolean> {
  return (await page.locator(PLOTS_TAB).getAttribute('aria-selected')) === 'true';
}

// Asserts that `actual` is within `tolerance` (as a fraction) of `expected`.
// When `expected` is 0 a ratio is undefined, so fall back to checking that
// `actual` is also 0 (e.g., a column hidden state where both widths are 0).
function expectWidthClose(actual: number, expected: number, tolerance: number, label: string): void {
  if (expected === 0) {
    expect(actual, `${label}: expected 0, got ${actual}`).toBe(0);
    return;
  }
  const ratio = Math.abs(actual - expected) / expected;
  expect(ratio, `${label}: expected ~${expected}, got ${actual} (delta ratio ${ratio.toFixed(3)})`).toBeLessThan(tolerance);
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
      await sleep(TIMEOUTS.layoutSettle);
    }
  }
  // Unzoom by re-executing whichever zoom command is active. We detect zoom
  // by checking which column is collapsed.
  const consoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
  const tabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
  if (consoleWidth < 50) {
    await executeCommand(page, 'layoutZoomRightColumn');
    await sleep(TIMEOUTS.layoutSettle);
  } else if (tabSet1Width < 50) {
    await executeCommand(page, 'layoutZoomLeftColumn');
    await sleep(TIMEOUTS.layoutSettle);
  }

  // A failure part-way through a zoom test can leave a quadrant stuck in
  // HIDE/EXCLUSIVE. That state is client-state-persisted, survives a reload, and
  // resetLayoutZoom deliberately won't touch it (HIDE/EXCLUSIVE pairs also
  // encode the empty-source-pane layout) -- so it would poison every later test
  // in the worker. Detect the specifically-wrong case, a Source pane that still
  // holds tabs but has no height, and clear it the way a user would.
  const sourceTabCount = await page.locator(`${SOURCE_PANE} [role="tab"]`).count();
  if (sourceTabCount > 0 && (await getOffsetHeight(page, SOURCE_PANE)) < 50) {
    await executeCommand(page, 'layoutEndZoom');
    await sleep(TIMEOUTS.layoutSettle);
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

// Drag the middle splitter left until the center (Console) column collapses
// below MINIMUM_CENTER_WIDTH (50px in MainSplitPanel). Pressing until the
// width drops -- rather than a fixed count -- keeps the precondition robust if
// the keyboard resize step changes; the cap stops a runaway loop. Returns the
// collapsed width.
async function collapseCenterColumn(page: Page): Promise<number> {
  await focusSplitter(page);
  for (let i = 0; i < 60 && (await getOffsetWidth(page, CONSOLE_PANE)) >= 50; i++) {
    await page.keyboard.press('ArrowLeft');
  }
  return getOffsetWidth(page, CONSOLE_PANE);
}

// Resize the middle splitter and assert it actually moved at least one column.
// Without this guard, a no-op resize would silently turn the preservation
// tests into no-op cycles that can never fail.
async function resizeAndAssertMoved(
  page: Page,
  key: 'ArrowLeft' | 'ArrowRight',
  count: number,
): Promise<{ consoleWidth: number; tabSet1Width: number }> {
  const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
  const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);

  await focusSplitter(page);
  await pressArrowMany(page, key, count);
  await sleep(TIMEOUTS.layoutSettle);

  const consoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
  const tabSet1Width = await getOffsetWidth(page, TABSET1_PANE);

  const consoleDelta = Math.abs(consoleWidth - initialConsoleWidth);
  const tabSet1Delta = Math.abs(tabSet1Width - initialTabSet1Width);
  expect(
    Math.max(consoleDelta, tabSet1Delta),
    `keyboard splitter resize did not move columns (console delta ${consoleDelta}, tabSet1 delta ${tabSet1Delta})`,
  ).toBeGreaterThan(RESIZE_MIN_DELTA_PX);

  return { consoleWidth, tabSet1Width };
}

async function openPaneLayoutOptions(page: Page): Promise<void> {
  await executeCommand(page, 'paneLayout');
  await page.waitForSelector(DIALOG_BOX, { timeout: 15000 });
  await page.waitForSelector('#rstudio_label_pane_layout_options_panel', { timeout: 5000 });
  await sleep(TIMEOUTS.layoutSettle);
}

async function resetPaneLayoutInDialog(page: Page): Promise<void> {
  await page.locator('#rstudio_pane_layout_reset_link').click();
  await sleep(TIMEOUTS.settleDelay);
}

// Pane Layout dialog uses GWT checkboxes: a <label for="X"> paired with a
// sibling <input id="X">. Resolve the linked input via the for attribute.
// Matches the label exactly (after trim) to avoid e.g. "Posit Assistant"
// matching a hypothetical "Posit Assistant Settings".
async function findTabCheckbox(page: Page, container: string, tabLabel: string): Promise<Locator | null> {
  const labels = page.locator(container).locator('label');
  const count = await labels.count();
  for (let i = 0; i < count; i++) {
    const label = labels.nth(i);
    const text = (await label.innerText()).trim();
    if (text !== tabLabel) continue;
    const forId = await label.getAttribute('for');
    if (!forId) continue;
    const checkbox = page.locator(`#${forId}`);
    if ((await checkbox.count()) > 0) return checkbox;
  }
  return null;
}

async function isTabChecked(page: Page, container: string, tabLabel: string): Promise<boolean> {
  const checkbox = await findTabCheckbox(page, container, tabLabel);
  if (!checkbox) {
    throw new Error(`Tab '${tabLabel}' not found in container '${container}'`);
  }
  return await checkbox.isChecked();
}

async function toggleTab(page: Page, container: string, tabLabel: string): Promise<void> {
  const checkbox = await findTabCheckbox(page, container, tabLabel);
  if (!checkbox) {
    throw new Error(`Tab '${tabLabel}' not found in container '${container}'`);
  }
  await checkbox.scrollIntoViewIfNeeded();
  await checkbox.click();
  await sleep(TIMEOUTS.layoutSettle);
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

// Dropped `.describe.serial` here: the per-test `afterEach(resetUILayout)`
// below already restores the layout between tests, and serial mode was
// turning one flaky test failure into a cascade of skipped sibling tests
// (then re-running the whole block on retry). Without serial, only the
// failed test retries -- failures stay actionable instead of producing
// 17-test noise dumps.
test.describe('Pane and column management', () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    // Normalize the source pane to a single Untitled tab instead of
    // trying to empty it. RStudio's session init creates a default
    // Untitled tab asynchronously (not gated on DeferredInitCompletedEvent),
    // so "0 docs at startup" is a state we can't reliably observe -- but
    // "exactly one Untitled" is what documents.resetToUntitled() lands on
    // deterministically, and it's the state every doc-touching test in this
    // file is happy to start from. See SourceColumnManager
    // onDocumentResetToUntitled -- it keeps any existing untitled and
    // closes everything else, or creates a fresh Untitled if none exists.
    await consoleActions.resetSourcePane();
    await expect.poll(
      () => page.locator(`${SOURCE_PANE} [role="tab"]`).count(),
      { timeout: 5000 },
    ).toBe(1);
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
      await sleep(TIMEOUTS.layoutSettle);
    }
    await page.locator(PREFERENCES_CONFIRM).click();
    await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 10000 });
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

    // Source pane has the single Untitled tab beforeAll normalized to.
    // The pane is visible (has dimensions); the asymmetric tab assertion
    // pins the canonical post-reset state -- exactly one tab.
    expect(await getOffsetWidth(page, SOURCE_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SOURCE_PANE)).toBeGreaterThan(0);
    expect(await page.locator(`${SOURCE_PANE} [role="tab"]`).count()).toBe(1);
  });

  // -------------------------------------------------------------------------
  test('Source columns can be created and closed', async ({ rstudioPage: page }) => {
    const dumpState = async (tag: string) => {
      const detail = await page.locator('[id^="rstudio_Source"][id$="_pane"]').evaluateAll(
        (els) =>
          els.map((el) => {
            const tabBar = el.querySelector('[role="tablist"], .gwt-TabLayoutPanelTabs');
            const tabTitles = tabBar
              ? Array.from(tabBar.children).map((c) => (c as HTMLElement).innerText?.trim() ?? '?')
              : [];
            return { id: el.id, tabCount: tabTitles.length, tabs: tabTitles };
          }),
      );
      // eslint-disable-next-line no-console
      console.log(`[panes:250 ${tag}] ${JSON.stringify(detail)}`);
    };

    // Ensure each new source column has at least one editor tab. RStudio
    // removes a source column only when its last tab is closed (via
    // LastSourceDocClosedEvent firing from SourceColumn.closeTabIndex);
    // columns that never had a tab persist through closeAllSourceDocs.
    // newSourceColumn does auto-create an Untitled in the FIRST new column
    // (the "always have a source doc when source view is shown" invariant),
    // but subsequent ones come up empty. Click each pane after creating it
    // to make it the active column, then run newSourceDoc -- now every
    // column has something for closeAllSourceDocs to close, and every
    // column ends up cleaned up.
    //
    // The click-to-activate dependency is undocumented product behavior:
    // clicking the outer pane container happens to focus the column today
    // (SourceColumnManager.setActive is invoked off a focus event chain we
    // don't directly observe). If a future change to focus routing or pane
    // hierarchy breaks this, ensureDoc will silently create the new doc in
    // the wrong column and the toHaveCount(1) assertion below will fail
    // even though newSourceDoc succeeded. The right long-term fix is to
    // expose window.rstudio.source.setActiveColumn(name) and use it here.
    const ensureDoc = async (paneSelector: string) => {
      await page.locator(paneSelector).click();
      const startedEmpty = await page.locator(`${paneSelector} .gwt-TabLayoutPanelTabs > *`).count() === 0;
      if (startedEmpty) {
        await executeCommand(page, 'newSourceDoc');
        await expect(
          page.locator(`${paneSelector} .gwt-TabLayoutPanelTabs > *`),
        ).toHaveCount(1, { timeout: 10000 });
      }
    };

    await dumpState('start');

    await executeCommand(page, 'newSourceColumn');
    await expect(page.locator(SOURCE1_PANE)).toBeVisible({ timeout: 10000 });
    await ensureDoc(SOURCE1_PANE);
    await dumpState('after-newSourceColumn-1');

    await executeCommand(page, 'newSourceColumn');
    await expect(page.locator(SOURCE2_PANE)).toBeVisible({ timeout: 10000 });
    await ensureDoc(SOURCE2_PANE);
    await dumpState('after-newSourceColumn-2');

    await executeCommand(page, 'newSourceColumn');
    await expect(page.locator(SOURCE3_PANE)).toBeVisible({ timeout: 10000 });
    await ensureDoc(SOURCE3_PANE);
    await dumpState('after-newSourceColumn-3');

    expect(await getOffsetWidth(page, SOURCE1_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SOURCE1_PANE)).toBeGreaterThan(0);
    expect(await getOffsetWidth(page, SOURCE2_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SOURCE2_PANE)).toBeGreaterThan(0);
    expect(await getOffsetWidth(page, SOURCE3_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SOURCE3_PANE)).toBeGreaterThan(0);

    // closeAllSourceDocs closes every editor; the LastSourceDocClosedEvent
    // fired by each column's final tab-close then prompts WorkbenchScreen
    // to remove the column container.
    await executeCommand(page, 'closeAllSourceDocs');
    await dumpState('after-closeAllSourceDocs-immediate');
    try {
      await expect(page.locator(SOURCE1_PANE)).toHaveCount(0, { timeout: 10000 });
      await expect(page.locator(SOURCE2_PANE)).toHaveCount(0, { timeout: 10000 });
      await expect(page.locator(SOURCE3_PANE)).toHaveCount(0, { timeout: 10000 });
    } finally {
      await dumpState('final');
    }
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
    // Sidebar reposition recreates the element; wait for it to settle in
    // its new spot before measuring left.
    await sleep(TIMEOUTS.layoutSettle);

    const rightLeft = await getLeft(page, SIDEBAR_PANE);
    expect(rightLeft).toBeGreaterThan(initialLeft);

    const consoleLeftAfterRight = await getLeft(page, CONSOLE_PANE);
    expect(rightLeft).toBeGreaterThan(consoleLeftAfterRight);

    await executeCommand(page, 'toggleSidebarLocation');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(TIMEOUTS.layoutSettle);

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

    const zoomedConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    expect(Math.abs(zoomedConsoleWidth - expectedZoomedWidth)).toBeLessThan(30);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeLessThan(50);

    await executeCommand(page, 'layoutZoomLeftColumn');
    // Wait for the un-zoom relayout to settle on each pane before reading
    // widths -- the previous `> 50 && < 0.75 * zoomed` threshold passed mid-
    // animation, so expectWidthClose would see in-flight values.
    await waitForStableWidth(page, CONSOLE_PANE, { min: 100 });
    await waitForStableWidth(page, TABSET1_PANE, { min: 50 });
    await waitForStableWidth(page, TABSET2_PANE, { min: 50 });

    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth, 0.1, 'restored Console');
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), initialTabSet1Width, 0.1, 'restored TabSet1');
    expectWidthClose(await getOffsetWidth(page, TABSET2_PANE), initialTabSet2Width, 0.1, 'restored TabSet2');
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

    const zoomedConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    expect(Math.abs(zoomedConsoleWidth - expectedZoomedWidth)).toBeLessThan(30);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeLessThan(50);

    await executeCommand(page, 'layoutZoomLeftColumn');
    await waitForStableWidth(page, CONSOLE_PANE, { min: 100 });
    await waitForStableWidth(page, TABSET1_PANE, { min: 50 });
    await waitForStableWidth(page, TABSET2_PANE, { min: 50 });
    await waitForStableWidth(page, SIDEBAR_PANE, { min: 50 });

    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth, 0.1, 'restored Console');
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), initialTabSet1Width, 0.1, 'restored TabSet1');
    expectWidthClose(await getOffsetWidth(page, TABSET2_PANE), initialTabSet2Width, 0.1, 'restored TabSet2');
    expectWidthClose(await getOffsetWidth(page, SIDEBAR_PANE), initialSidebarWidth, 0.1, 'restored Sidebar');
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

    const zoomedTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    expect(Math.abs(zoomedTabSet1Width - expectedZoomedWidth)).toBeLessThan(30);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - zoomedTabSet1Width)).toBeLessThan(30);
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeLessThan(50);

    await executeCommand(page, 'layoutZoomRightColumn');
    await waitForStableWidth(page, CONSOLE_PANE, { min: 50 });
    await waitForStableWidth(page, TABSET1_PANE, { min: 100 });
    await waitForStableWidth(page, TABSET2_PANE, { min: 100 });

    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth, 0.1, 'restored Console');
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), initialTabSet1Width, 0.1, 'restored TabSet1');
    expectWidthClose(await getOffsetWidth(page, TABSET2_PANE), initialTabSet2Width, 0.1, 'restored TabSet2');
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

    const zoomedTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    expect(Math.abs(zoomedTabSet1Width - expectedZoomedWidth)).toBeLessThan(30);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - zoomedTabSet1Width)).toBeLessThan(30);
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeLessThan(50);

    await executeCommand(page, 'layoutZoomRightColumn');
    await waitForStableWidth(page, CONSOLE_PANE, { min: 50 });
    await waitForStableWidth(page, TABSET1_PANE, { min: 100 });
    await waitForStableWidth(page, TABSET2_PANE, { min: 100 });
    await waitForStableWidth(page, SIDEBAR_PANE, { min: 50 });

    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth, 0.1, 'restored Console');
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), initialTabSet1Width, 0.1, 'restored TabSet1');
    expectWidthClose(await getOffsetWidth(page, TABSET2_PANE), initialTabSet2Width, 0.1, 'restored TabSet2');
    expectWidthClose(await getOffsetWidth(page, SIDEBAR_PANE), initialSidebarWidth, 0.1, 'restored Sidebar');
  });

  // -------------------------------------------------------------------------
  test('Zoomed left column with sidebar on right works as expected', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    await executeCommand(page, 'toggleSidebarLocation');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(TIMEOUTS.layoutSettle);

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

    expect(Math.abs((await getOffsetWidth(page, CONSOLE_PANE)) - expectedZoomedWidth)).toBeLessThan(30);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeLessThan(50);

    const zoomedConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);

    await executeCommand(page, 'layoutZoomLeftColumn');
    await waitForStableWidth(page, CONSOLE_PANE, { min: 100 });
    await waitForStableWidth(page, TABSET1_PANE, { min: 50 });
    await waitForStableWidth(page, TABSET2_PANE, { min: 50 });
    await waitForStableWidth(page, SIDEBAR_PANE, { min: 50 });

    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth, 0.1, 'restored Console');
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), initialTabSet1Width, 0.1, 'restored TabSet1');
    expectWidthClose(await getOffsetWidth(page, TABSET2_PANE), initialTabSet2Width, 0.1, 'restored TabSet2');
    expectWidthClose(await getOffsetWidth(page, SIDEBAR_PANE), initialSidebarWidth, 0.1, 'restored Sidebar');
  });

  // -------------------------------------------------------------------------
  test('Zoomed right column with sidebar on right works as expected', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    await executeCommand(page, 'toggleSidebarLocation');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await sleep(TIMEOUTS.layoutSettle);

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

    const zoomedTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    expect(Math.abs(zoomedTabSet1Width - expectedZoomedWidth)).toBeLessThan(30);
    expect(Math.abs((await getOffsetWidth(page, TABSET2_PANE)) - zoomedTabSet1Width)).toBeLessThan(30);
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeLessThan(50);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeLessThan(50);

    await executeCommand(page, 'layoutZoomRightColumn');
    await waitForStableWidth(page, CONSOLE_PANE, { min: 50 });
    await waitForStableWidth(page, TABSET1_PANE, { min: 100 });
    await waitForStableWidth(page, TABSET2_PANE, { min: 100 });
    await waitForStableWidth(page, SIDEBAR_PANE, { min: 50 });

    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth, 0.1, 'restored Console');
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), initialTabSet1Width, 0.1, 'restored TabSet1');
    expectWidthClose(await getOffsetWidth(page, TABSET2_PANE), initialTabSet2Width, 0.1, 'restored TabSet2');
    expectWidthClose(await getOffsetWidth(page, SIDEBAR_PANE), initialSidebarWidth, 0.1, 'restored Sidebar');
  });

  // -------------------------------------------------------------------------
  // #18444: PaneManager's zoom bookkeeping (maximizedWindow_ / maximizedTab_)
  // must never outlive the visible zoom. While it does, the
  // WindowEnsureVisibleEvent handler re-zooms whichever pane raises itself
  // next -- a new plot, a package-list refresh, a render -- which is what
  // makes the four-pane layout collapse over and over.
  //
  // `View > Panes > Zoom Console` is the live readout of that bookkeeping:
  // manageLayoutCommands() drives the checkmark straight off maximizedTab_.
  // A checked item while the layout looks normal IS the stale state.

  // Drive the Console pane header's maximize button, which reads as "restore"
  // once the pane is EXCLUSIVE (WindowFrameButton.updateLabel). This is the
  // button a user reaches for to escape a zoom.
  async function clickConsoleRestoreButton(page: Page): Promise<void> {
    await page.locator(CONSOLE_MAX_BTN).click();
    await sleep(TIMEOUTS.layoutSettle);
  }

  // Zoom the Console pane and wait for the right column to collapse.
  async function zoomConsolePane(page: Page): Promise<void> {
    await executeCommand(page, 'layoutZoomConsole');
    await expect.poll(
      async () => (await getOffsetWidth(page, TABSET1_PANE)) < 50,
      { timeout: 5000 },
    ).toBe(true);
    expect(
      await isCommandChecked(page, 'layoutZoomConsole'),
      'precondition: zoom should be tracked after layoutZoomConsole',
    ).toBe(true);
  }

  test('Restoring a zoomed pane from its header button ends the zoom (#18444)', async ({ rstudioPage: page }) => {
    const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);
    const initialTabSet2Width = await getOffsetWidth(page, TABSET2_PANE);
    expect(initialTabSet1Width).toBeGreaterThan(50);

    await zoomConsolePane(page);
    await clickConsoleRestoreButton(page);

    // The header button restores the vertical split in the Console's own
    // column, so Source comes back...
    await waitForStableWidth(page, SOURCE_PANE, { min: 50 });
    expect(await getOffsetHeight(page, SOURCE_PANE)).toBeGreaterThan(50);

    // ...and the right column must come back too, with the zoom bookkeeping
    // cleared. Before this fix neither happened: the button drove
    // DualWindowLayoutPanel's state machine directly and PaneManager never
    // heard about it, so maximizedWindow_ stayed set and the right column
    // stayed at width 0.
    expect(
      await isCommandChecked(page, 'layoutZoomConsole'),
      'zoom bookkeeping should be cleared after escaping the zoom',
    ).toBe(false);
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), initialTabSet1Width, 0.1, 'restored TabSet1');
    expectWidthClose(await getOffsetWidth(page, TABSET2_PANE), initialTabSet2Width, 0.1, 'restored TabSet2');
  });

  // The maximize button is not the only way to maximize a pane: double-clicking
  // a tabset's tab bar (ModuleTabLayoutPanel) does it too, as does
  // double-clicking the Console title bar (PrimaryWindowFrame). Every route must
  // end a zoom the same way, or the ones that don't leave the same stale
  // bookkeeping behind.
  //
  // Only the tab-bar gesture is covered. The Console title bar is not rendered
  // in the default configuration: whenever a secondary console tab is visible
  // (Terminal is, by default) ConsoleTabPanel.managePanels calls
  // setFillWidget, which drops the frame's header widget entirely. It becomes
  // the live gesture only with show_terminal_tab and the jobs tabs all off.
  test('Restoring a zoomed tabset by double-clicking its tab ends the zoom (#18444)', async ({ rstudioPage: page }) => {
    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);

    await executeCommand(page, 'layoutZoomEnvironment');
    await expect.poll(
      async () => (await getOffsetWidth(page, CONSOLE_PANE)) < 50,
      { timeout: 5000 },
    ).toBe(true);
    expect(
      await isCommandChecked(page, 'layoutZoomEnvironment'),
      'precondition: zoom should be tracked after layoutZoomEnvironment',
    ).toBe(true);

    await page.locator(`${TABSET1_PANE} [role="tab"]`).first().dblclick();
    await sleep(TIMEOUTS.layoutSettle);

    await waitForStableWidth(page, CONSOLE_PANE, { min: 50 });
    expect(
      await isCommandChecked(page, 'layoutZoomEnvironment'),
      'a tab double-click must end the zoom, not just restore the quadrant',
    ).toBe(false);
    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth, 0.1, 'restored Console');
  });

  // The end-to-end guard on the whole reported cascade: zoom, escape the zoom,
  // then let a pane raise itself. Before this fix the escape left
  // maximizedWindow_ set and the raise was turned into a fresh zoom by the
  // WindowEnsureVisibleEvent handler, so the right column took the full panel
  // width and Source/Console collapsed to 0.
  test('A pane raising itself does not collapse the layout (#18444)', async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);

    // Start with Plots deselected so its selection is an unambiguous signal
    // that the pane actually raised itself.
    await executeCommand(page, 'activateFiles');
    await expect.poll(() => isPlotsTabSelected(page), { timeout: 5000 }).toBe(false);

    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);

    await zoomConsolePane(page);
    await clickConsoleRestoreButton(page);
    await waitForStableWidth(page, SOURCE_PANE, { min: 50 });

    // `plot()` sets activatePlots, so Plots calls WorkbenchPane.bringToFront()
    // -> ToolbarPane.bringToFront() -> EnsureVisibleEvent ->
    // WindowFrame.onEnsureVisible -> WindowEnsureVisibleEvent, the handler that
    // used to steal the zoom.
    await consoleActions.executeInConsole('plot(1:10)');

    // Wait for the raise itself, not just for the console to return to a
    // prompt. The session enqueues the prompt event BEFORE running change
    // detection (SessionConsoleInput.cpp), so executeInConsole resolves before
    // the plots event is even queued -- measuring widths at that point would
    // pass without the raise ever happening. Plots gets selected on both a
    // fixed and a broken build (the broken path selects it via
    // fullyMaximizeWindow), so this gate cannot mask the regression.
    await expect.poll(() => isPlotsTabSelected(page), { timeout: 15000 }).toBe(true);
    await sleep(TIMEOUTS.layoutSettle);

    // Nothing should have moved: the raise activates Plots within the existing
    // layout. Tolerance is tight because the expected delta is zero.
    expect(
      await getOffsetWidth(page, CONSOLE_PANE),
      'a pane raising itself must not collapse the Console column',
    ).toBeGreaterThan(50);
    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth, 0.05, 'Console after plot');
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), initialTabSet1Width, 0.05, 'TabSet1 after plot');
    expect(
      await getOffsetHeight(page, TABSET1_PANE),
      'TabSet1 should not be hidden by a pane raising itself',
    ).toBeGreaterThan(50);
  });

  // The refactor routed every maximize gesture through WindowFrame.maximize()
  // and its overridable action; hookPaneMaximize delegates to maximizeDefault()
  // when the pane isn't the zoomed one. Nothing else covers that branch --
  // tabs.test.ts drives maximizeTabSet2, which calls onWindowStateChange
  // directly and never reaches the frame -- so a regression there would stop
  // the header button maximizing anything, silently.
  test('The pane header button still maximizes when nothing is zoomed (#18444)', async ({ rstudioPage: page }) => {
    expect(
      await isCommandChecked(page, 'layoutZoomConsole'),
      'precondition: nothing should be zoomed',
    ).toBe(false);

    const initialSourceHeight = await getOffsetHeight(page, SOURCE_PANE);
    expect(initialSourceHeight).toBeGreaterThan(50);

    await page.locator(CONSOLE_MAX_BTN).click();
    await expect.poll(
      async () => getOffsetHeight(page, SOURCE_PANE),
      { message: 'maximizing Console should shrink Source', timeout: 5000 },
    ).toBeLessThan(initialSourceHeight);

    // A vertical maximize is not a zoom, so no column collapses and the zoom
    // bookkeeping stays clear.
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(50);
    expect(await isCommandChecked(page, 'layoutZoomConsole')).toBe(false);

    // Clicking again restores, since LogicalWindow maps MAXIMIZE onto an
    // already-MAXIMIZE window back to NORMAL.
    await page.locator(CONSOLE_MAX_BTN).click();
    await expect.poll(
      async () => getOffsetHeight(page, SOURCE_PANE),
      { message: 'clicking again should restore Source', timeout: 5000 },
    ).toBeGreaterThan(50);
  });

  test('Toggling the sidebar while zoomed ends the zoom (#18444)', async ({ rstudioPage: page }) => {
    await zoomConsolePane(page);

    // Showing the sidebar re-lays-out the columns, which undraws the zoom.
    // Before this fix that happened without telling PaneManager.
    await showSidebar(page);
    await waitForStableWidth(page, TABSET1_PANE, { min: 50 });

    expect(
      await isCommandChecked(page, 'layoutZoomConsole'),
      'zoom bookkeeping should be cleared once the sidebar re-lays-out the columns',
    ).toBe(false);
    // Assert every column, not just TabSet1: the fix starts a restore animation
    // and then lets setSidebarWidget re-lay-out on top of it, so a bad
    // interleaving could leave a different column collapsed.
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(50);
    expect(
      await getOffsetHeight(page, TABSET1_PANE),
      'TabSet1 should not be left hidden after the zoom is undrawn',
    ).toBeGreaterThan(50);
  });

  // The sidebar maximizes by zooming its column, not its quadrant, so it needs
  // its own zoom-aware dispatch: it can still be the pane-zoomed window, because
  // Zoom Chat goes through zoomTab -> fullyMaximizeWindow, and zoomColumn's
  // un-zoom branch does not clear the pane-zoom bookkeeping. Without that
  // dispatch, escaping a Zoom Chat via the sidebar's own button leaves the same
  // stale state -- and after the maximize gestures were funnelled together, a
  // double-click on a sidebar tab reaches it too.
  test('Restoring a zoomed sidebar from its header button ends the zoom (#18444)', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);

    await executeCommand(page, 'layoutZoomChat');
    await expect.poll(
      async () => (await getOffsetWidth(page, CONSOLE_PANE)) < 50,
      { timeout: 5000 },
    ).toBe(true);
    expect(
      await isCommandChecked(page, 'layoutZoomChat'),
      'precondition: the sidebar pane zoom should be tracked',
    ).toBe(true);

    await page.locator(SIDEBAR_MAX_BTN).click();
    await sleep(TIMEOUTS.layoutSettle);
    await waitForStableWidth(page, CONSOLE_PANE, { min: 50 });

    expect(
      await isCommandChecked(page, 'layoutZoomChat'),
      'escaping a sidebar pane zoom must clear the zoom bookkeeping',
    ).toBe(false);
    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth, 0.1, 'restored Console');
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(50);
  });

  // Same defect as the test above, reached through the menu instead of the
  // frame: View > Panes > Zoom Sidebar (and its shortcut) calls zoomColumn
  // directly, bypassing the frame's maximize action entirely.
  test('Escaping a zoomed sidebar via the Zoom Sidebar command ends the zoom (#18444)', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);

    await executeCommand(page, 'layoutZoomChat');
    await expect.poll(
      async () => (await getOffsetWidth(page, CONSOLE_PANE)) < 50,
      { timeout: 5000 },
    ).toBe(true);
    expect(
      await isCommandChecked(page, 'layoutZoomChat'),
      'precondition: the sidebar pane zoom should be tracked',
    ).toBe(true);

    await executeCommand(page, 'layoutZoomSidebar');
    await waitForStableWidth(page, CONSOLE_PANE, { min: 50 });

    expect(
      await isCommandChecked(page, 'layoutZoomChat'),
      'the Zoom Sidebar command must clear a tracked pane zoom, not just the widths',
    ).toBe(false);
    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), initialConsoleWidth, 0.1, 'restored Console');
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(50);
  });

  // The hide direction is not redundant: refreshSidebar implements the location
  // toggle as showSidebar(false) then showSidebar(true), so the hide branch of
  // the visibilityChanging guard is what carries "move the sidebar while a pane
  // is zoomed".
  test('Hiding the sidebar while zoomed ends the zoom (#18444)', async ({ rstudioPage: page }) => {
    await showSidebar(page);
    await zoomConsolePane(page);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });
    await waitForStableWidth(page, TABSET1_PANE, { min: 50 });

    expect(
      await isCommandChecked(page, 'layoutZoomConsole'),
      'zoom bookkeeping should be cleared when the sidebar is hidden',
    ).toBe(false);
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeGreaterThan(50);
  });

  test('A zoomed pane does not resurrect across a UI reload (#18444)', async ({ rstudioPage: page }) => {
    // Client state is pushed on ClientStateUpdater's passive 5s timer
    // (PushClientStateEvent defaults to active=false), so reloading straight
    // after zooming would outrun the persist and the reload would restore
    // nothing -- the test would pass without exercising the replay at all.
    // Wait for the set_client_state RPC that actually carries the zoom.
    const zoomPersisted = page.waitForResponse(
      (response) =>
        response.url().includes('set_client_state') &&
        (response.request().postData() ?? '').includes('MaximizedTab'),
      { timeout: 15000 },
    );
    await zoomConsolePane(page);
    await zoomPersisted;

    // MainSplitPanel.isZoomedColumnState deliberately discards zoomed column
    // widths on restore (#16688), so the zoom's other half -- the persisted
    // TabZoom in PaneManager and the persisted quadrant state in
    // DualWindowLayoutPanel (topwindowstate HIDE for a Console zoom) -- must not
    // come back either. Otherwise the session reopens with default widths and
    // live zoom bookkeeping.
    await page.reload();
    // TABSET1_PANE attaches at workbench construction, long before client state
    // is applied, so it is not a usable gate for the startup zoom handling.
    // window.rstudio.ready is; the polled assertions below then absorb the
    // remaining 200ms timer in PaneManager's ZoomedTabStateValue.onInit.
    await page.waitForFunction(() => window.rstudio?.ready === true, null, {
      timeout: TIMEOUTS.sessionRestart,
    });
    await sleep(TIMEOUTS.layoutSettle);

    await expect.poll(
      async () => getOffsetWidth(page, TABSET1_PANE),
      {
        message: 'the zoom must not be replayed after a reload that discarded its column widths',
        timeout: 5000,
        intervals: [50, 100, 150],
      },
    ).toBeGreaterThan(50);
    await expect.poll(
      () => isCommandChecked(page, 'layoutZoomConsole'),
      {
        message: 'a zoom whose column widths were not restored must not stay tracked',
        timeout: 5000,
      },
    ).toBe(false);
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(50);
    // The quadrant half of the zoom is persisted separately, by
    // DualWindowLayoutPanel. Zooming Console puts its sibling (Source) in HIDE,
    // so a reload that only skips the column half would still come back with
    // Source hidden.
    expect(
      await getOffsetHeight(page, SOURCE_PANE),
      'the zoomed pane\'s sibling quadrant must not come back hidden',
    ).toBeGreaterThan(50);
  });

  // -------------------------------------------------------------------------
  test('Sidebar visibility persists across UI reload', async ({ rstudioPage: page }) => {
    expect(await elementExists(page, SIDEBAR_PANE)).toBe(false);

    await showSidebar(page);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SIDEBAR_PANE)).toBeGreaterThan(0);

    await page.reload();
    await page.waitForSelector(SIDEBAR_PANE, { timeout: TIMEOUTS.sessionRestart });
    await sleep(TIMEOUTS.settleDelay);

    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(0);
    expect(await getOffsetHeight(page, SIDEBAR_PANE)).toBeGreaterThan(0);
  });

  // -------------------------------------------------------------------------
  test('Column widths are preserved when toggling sidebar visibility (#16676)', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(0);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(0);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(0);
    expect(await elementExists(page, MIDDLE_COLUMN_SPLITTER)).toBe(true);

    const { consoleWidth: modifiedConsoleWidth, tabSet1Width: modifiedTabSet1Width } =
      await resizeAndAssertMoved(page, 'ArrowRight', 18);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    // Settle on the sidebar width: it reveals in the same relayout pass as the
    // columns, so once it stops animating the column widths are final too.
    await waitForStableWidth(page, SIDEBAR_PANE, { min: 100 });

    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), modifiedConsoleWidth, 0.05, 'final Console');
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), modifiedTabSet1Width, 0.05, 'final TabSet1');
  });

  // -------------------------------------------------------------------------
  test('Column widths preserved through multiple hide/show cycles', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    expect(await elementExists(page, MIDDLE_COLUMN_SPLITTER)).toBe(true);
    const { consoleWidth: consoleModified, tabSet1Width: tabSet1Modified } =
      await resizeAndAssertMoved(page, 'ArrowRight', 15);

    for (let cycle = 1; cycle <= 3; cycle++) {
      await executeCommand(page, 'toggleSidebar');
      await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });

      await executeCommand(page, 'toggleSidebar');
      await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
      await waitForStableWidth(page, SIDEBAR_PANE, { min: 100 });

      expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), consoleModified, 0.05, `Console cycle ${cycle}`);
      expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), tabSet1Modified, 0.05, `TabSet1 cycle ${cycle}`);
    }
  });

  // -------------------------------------------------------------------------
  // Exercises MainSplitPanel's involuntary-squeeze "reclaim" branch: the
  // center is healthy when the sidebar is hidden (so savedCenterCollapsed_ is
  // false), then resizing while hidden leaves it near-zero on show -- so the
  // reclaim resets columns to default widths, which the >100px assertions
  // below verify. (The complementary "preserve" branch is covered by
  // "Deliberately collapsed center is preserved across sidebar toggle".)
  test('Sidebar show uses default widths after columns resized while hidden', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    await resizeAndAssertMoved(page, 'ArrowRight', 10);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });

    // Resize columns while sidebar is hidden — saved widths should be invalidated.
    await resizeAndAssertMoved(page, 'ArrowLeft', 25);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });

    // Sidebar reveal animates: toBeVisible passes as soon as the element
    // attaches with non-empty bounding box, but column widths can still be
    // mid-relayout. Poll on each pane's offsetWidth -- a direct DOM signal
    // -- instead of a blind 300ms sleep that loses the race on slow CI.
    await expect.poll(() => getOffsetWidth(page, CONSOLE_PANE),
      { timeout: 5000 }).toBeGreaterThan(100);
    await expect.poll(() => getOffsetWidth(page, TABSET1_PANE),
      { timeout: 5000 }).toBeGreaterThan(100);
    await expect.poll(() => getOffsetWidth(page, SIDEBAR_PANE),
      { timeout: 5000 }).toBeGreaterThan(100);
  });

  // -------------------------------------------------------------------------
  test('Different resize patterns preserve correctly through sidebar toggle', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    // Phase 1: resize LEFT, verify preservation.
    const { consoleWidth: consoleAfterLeft, tabSet1Width: tabSet1AfterLeft } =
      await resizeAndAssertMoved(page, 'ArrowLeft', 15);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await waitForStableWidth(page, SIDEBAR_PANE, { min: 100 });

    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), consoleAfterLeft, 0.05, 'Console after LEFT');
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), tabSet1AfterLeft, 0.05, 'TabSet1 after LEFT');

    // Phase 2: resize RIGHT, verify preservation.
    const { consoleWidth: consoleAfterRight, tabSet1Width: tabSet1AfterRight } =
      await resizeAndAssertMoved(page, 'ArrowRight', 20);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await waitForStableWidth(page, SIDEBAR_PANE, { min: 100 });

    expectWidthClose(await getOffsetWidth(page, CONSOLE_PANE), consoleAfterRight, 0.05, 'Console after RIGHT');
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), tabSet1AfterRight, 0.05, 'TabSet1 after RIGHT');
  });

  // -------------------------------------------------------------------------
  test('Extreme resize values preserve correctly through sidebar toggle', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    const { consoleWidth: consoleVeryWide, tabSet1Width: tabSet1VeryNarrow } =
      await resizeAndAssertMoved(page, 'ArrowRight', 30);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await waitForStableWidth(page, SIDEBAR_PANE, { min: 100 });

    const consoleAfterToggle = await getOffsetWidth(page, CONSOLE_PANE);
    const tabSet1AfterToggle = await getOffsetWidth(page, TABSET1_PANE);

    expectWidthClose(consoleAfterToggle, consoleVeryWide, 0.05, 'Console after extreme resize');

    if (tabSet1VeryNarrow < 50) {
      // Tiny widths use absolute tolerance; ratio amplifies single-pixel differences.
      expect(Math.abs(tabSet1AfterToggle - tabSet1VeryNarrow)).toBeLessThan(10);
    } else {
      expectWidthClose(tabSet1AfterToggle, tabSet1VeryNarrow, 0.10, 'TabSet1 after extreme resize');
    }

    expect(consoleAfterToggle).toBeGreaterThan(50);
    expect(tabSet1AfterToggle).toBeGreaterThanOrEqual(0);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(0);
  });

  // -------------------------------------------------------------------------
  // Guards MainSplitPanel's savedCenterCollapsed_ "preserve" branch: a center
  // the user deliberately collapsed must survive a sidebar hide/show as-is,
  // NOT get reclaimed to a default width. (The complementary "reclaim" branch
  // -- an involuntary squeeze -- is exercised by "Sidebar show uses default
  // widths after columns resized while hidden" above.)
  test('Deliberately collapsed center is preserved across sidebar toggle', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    const collapsedWidth = await collapseCenterColumn(page);
    expect(collapsedWidth, 'precondition: center collapsed below minimum').toBeLessThan(50);

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });

    await executeCommand(page, 'toggleSidebar');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 5000 });
    await waitForStableWidth(page, SIDEBAR_PANE, { min: 100 });

    // With the fix the deliberately-collapsed center is restored as-is; an
    // unconditional reclaim would instead snap it back to a default width well
    // above the minimum (this assertion fails against that earlier behavior).
    expect(
      await getOffsetWidth(page, CONSOLE_PANE),
      'deliberately-collapsed center should be preserved, not reclaimed',
    ).toBeLessThan(50);

    // Restore a healthy center so the shared afterEach -- which reads a <50px
    // Console as a zoomed layout -- doesn't misfire on cleanup. Leaves the
    // layout lopsided the same way the extreme-resize test above does, which
    // resetUILayout already tolerates.
    await focusSplitter(page);
    await pressArrowMany(page, 'ArrowRight', 60);
    await sleep(TIMEOUTS.layoutSettle);
  });

  // -------------------------------------------------------------------------
  // Both Posit Assistant tests verify the same flow; only the initial sidebar
  // visibility differs (visible vs hidden).
  for (const sidebarVisibleAtStart of [true, false] as const) {
    const label = sidebarVisibleAtStart ? 'visible' : 'hidden';
    test(`Moving Posit Assistant from ${label} sidebar to TabSet1 persists across UI reload`, async ({ rstudioPage: page }) => {
      await openPaneLayoutOptions(page);
      await resetPaneLayoutInDialog(page);

      const sidebarVisibleCheckbox = page.locator(PL_SIDEBAR_VISIBLE);
      if ((await sidebarVisibleCheckbox.isChecked()) !== sidebarVisibleAtStart) {
        await sidebarVisibleCheckbox.click();
        await sleep(TIMEOUTS.layoutSettle);
      }

      expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(true);
      expect(await isTabChecked(page, PL_RIGHT_TOP, 'Posit Assistant')).toBe(false);

      await toggleTab(page, PL_RIGHT_TOP, 'Posit Assistant');

      expect(await isTabChecked(page, PL_RIGHT_TOP, 'Posit Assistant')).toBe(true);
      expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(false);
      // Sidebar visibility auto-unchecks once its last tab is removed.
      expect(await sidebarVisibleCheckbox.isChecked()).toBe(false);

      await page.locator(PREFERENCES_CONFIRM).click();
      await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 10000 });

      await page.reload();
      await page.waitForSelector(TABSET1_PANE, { timeout: TIMEOUTS.sessionRestart });
      await sleep(TIMEOUTS.layoutSettle);

      await openPaneLayoutOptions(page);

      expect(await isTabChecked(page, PL_RIGHT_TOP, 'Posit Assistant')).toBe(true);
      expect(await isTabChecked(page, PL_SIDEBAR, 'Posit Assistant')).toBe(false);
      expect(await page.locator(PL_SIDEBAR_VISIBLE).isChecked()).toBe(false);

      // Read-only verification — discard the dialog via Escape rather than
      // committing, since we made no changes here.
      await page.keyboard.press('Escape');
      await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 5000 });
    });
  }
});

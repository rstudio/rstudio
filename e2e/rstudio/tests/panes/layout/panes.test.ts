// Tests related to pane and column management.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { sleep, TIMEOUTS } from '@utils/constants';
import { executeCommand, isCommandChecked } from '@utils/commands';
import { PLOTS_TAB } from '@pages/plots_pane.page';
import { VIEWER_TAB } from '@pages/viewer_pane.page';
import type { Locator, Page } from 'playwright';

// ---------------------------------------------------------------------------
// Workbench pane selectors
// ---------------------------------------------------------------------------
const TABSET1_PANE = '#rstudio_TabSet1_pane';
const TABSET2_PANE = '#rstudio_TabSet2_pane';
const CONSOLE_PANE = '#rstudio_Console_pane';
const SOURCE_PANE = '#rstudio_Source_pane';
const SOURCE1_PANE = '#rstudio_Source1_pane';
// Any additional source column, whatever number its pane id carries; the main
// Source pane (#rstudio_Source_pane) is excluded.
const EXTRA_SOURCE_COLUMN_PANES = '[id^="rstudio_Source"][id$="_pane"]:not(#rstudio_Source_pane)';
const SOURCE2_PANE = '#rstudio_Source2_pane';
const SOURCE3_PANE = '#rstudio_Source3_pane';
const SIDEBAR_PANE = '#rstudio_Sidebar_pane';
const CUSTOMIZE_PANES_BUTTON = '#rstudio_customize_panes';
const SIDEBAR_CLOSE_BTN = '.rstudio_panel_close_btn_sidebar';
const SIDEBAR_MAX_BTN = '.rstudio_panel_max_btn_sidebar';
const MIDDLE_COLUMN_SPLITTER = '#rstudio_middle_column_splitter';
// Pane header maximize button; doubles as "restore" once the pane is EXCLUSIVE.
// Scoped to the normal frame -- MinimizedWindowFrame reuses the same class.
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

// How long a post-reload layout must stay un-zoomed to count as not replayed.
// Must outlast the 200ms Timer in ZoomedTabStateValue.onInit, which runs after
// window.rstudio.ready. 2s leaves room for a draft build on a loaded machine.
const RELOAD_ZOOM_REPLAY_WINDOW_MS = 2000;

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

// Asserts continuously for `durationMs`, failing on the first violation.
// expect.poll cannot express "must not happen": it returns on its first passing
// sample, and here the good state is also the starting state. The window starts
// after the first sample, which is a locator read and can be slow.
async function expectHoldsFor(durationMs: number, assertions: () => Promise<void>): Promise<void> {
  await assertions();
  const deadline = Date.now() + durationMs;
  for (;;) {
    if (Date.now() >= deadline)
      return;
    await sleep(50);
    await assertions();
  }
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

  // A failed zoom test can leave a quadrant stuck in HIDE/EXCLUSIVE, which
  // persists across a reload and breaks every later test. resetLayoutZoom won't
  // touch it, since HIDE/EXCLUSIVE also encodes the empty-source-pane layout.
  // Detect the one wrong case -- Source has tabs but no height -- and clear it.
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
  // must not outlive the visible zoom. While it does, WindowEnsureVisibleEvent
  // re-zooms the next pane that raises itself -- a plot, a package refresh, a
  // render -- collapsing the layout again and again. `View > Panes > Zoom
  // Console` is the live readout: manageLayoutCommands drives its checkmark off
  // maximizedTab_, so a checkmark beside a normal layout is the stale state.

  // The button a user reaches for to escape a zoom: it reads as "restore" once
  // the pane is EXCLUSIVE (WindowFrameButton.updateLabel).
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

    // The button restores the vertical split, so Source comes back...
    await waitForStableWidth(page, SOURCE_PANE, { min: 50 });
    expect(await getOffsetHeight(page, SOURCE_PANE)).toBeGreaterThan(50);

    // ...and the right column too, with the bookkeeping cleared. Before the fix
    // neither happened: the button drove DualWindowLayoutPanel directly and
    // PaneManager never heard about it.
    expect(
      await isCommandChecked(page, 'layoutZoomConsole'),
      'zoom bookkeeping should be cleared after escaping the zoom',
    ).toBe(false);
    expectWidthClose(await getOffsetWidth(page, TABSET1_PANE), initialTabSet1Width, 0.1, 'restored TabSet1');
    expectWidthClose(await getOffsetWidth(page, TABSET2_PANE), initialTabSet2Width, 0.1, 'restored TabSet2');
  });

  // The button is not the only maximize gesture: a tab-bar double-click
  // (ModuleTabLayoutPanel) and a Console title-bar double-click
  // (PrimaryWindowFrame) do it too, and all must end a zoom the same way.
  //
  // Only the tab bar is covered. ConsoleTabPanel.managePanels calls
  // setFillWidget while any secondary console tab is visible (Terminal is, by
  // default), which drops that title bar -- so the gesture is unreachable in
  // the default configuration. Verified by hand instead.
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

  // The end-to-end guard on the reported cascade: zoom, escape, then let a pane
  // raise itself. Before the fix the escape left maximizedWindow_ set, and
  // WindowEnsureVisibleEvent turned the raise into a fresh zoom.
  test('A pane raising itself does not collapse the layout (#18444)', async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);

    // Deselect Plots first, so its selection is a clear signal of the raise.
    await executeCommand(page, 'activateFiles');
    await expect.poll(() => isPlotsTabSelected(page), { timeout: 5000 }).toBe(false);

    const initialConsoleWidth = await getOffsetWidth(page, CONSOLE_PANE);
    const initialTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);

    await zoomConsolePane(page);
    await clickConsoleRestoreButton(page);
    await waitForStableWidth(page, SOURCE_PANE, { min: 50 });

    // `plot()` activates Plots, which ends in WindowEnsureVisibleEvent -- the
    // handler that used to steal the zoom.
    await consoleActions.executeInConsole('plot(1:10)');

    // Wait for the raise, not the prompt: the session queues the prompt and
    // wakes the poller before running change detection (SessionConsoleInput.cpp),
    // so at the prompt the plots event may be unqueued, undelivered, or
    // unrendered. Plots is selected on fixed and broken builds alike, so this
    // gate cannot mask the regression.
    await expect.poll(() => isPlotsTabSelected(page), { timeout: 15000 }).toBe(true);
    await sleep(TIMEOUTS.layoutSettle);

    // Nothing must move: the raise activates Plots inside the existing layout.
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

  // Covers hookPaneMaximize's maximizeDefault() branch, for a pane that is not
  // the zoomed one. Nothing else does: tabs.test.ts drives maximizeTabSet2,
  // which calls onWindowStateChange directly and never reaches the frame. A
  // regression here breaks the header button silently.
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

    // A vertical maximize is not a zoom: no column collapses, bookkeeping clear.
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(50);
    expect(await isCommandChecked(page, 'layoutZoomConsole')).toBe(false);

    // A second click restores: LogicalWindow maps MAXIMIZE on MAXIMIZE to NORMAL.
    await page.locator(CONSOLE_MAX_BTN).click();
    await expect.poll(
      async () => getOffsetHeight(page, SOURCE_PANE),
      { message: 'clicking again should restore Source', timeout: 5000 },
    ).toBeGreaterThan(50);
  });

  test('Toggling the sidebar while zoomed ends the zoom (#18444)', async ({ rstudioPage: page }) => {
    await zoomConsolePane(page);

    // A sidebar show re-lays-out the columns, which undraws the zoom -- before
    // the fix, without telling PaneManager.
    await showSidebar(page);
    await waitForStableWidth(page, TABSET1_PANE, { min: 50 });

    expect(
      await isCommandChecked(page, 'layoutZoomConsole'),
      'zoom bookkeeping should be cleared once the sidebar re-lays-out the columns',
    ).toBe(false);
    // Every column, not just TabSet1: the restore animates while setSidebarWidget
    // lays out on top of it, so a bad interleaving can collapse a different one.
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, SIDEBAR_PANE)).toBeGreaterThan(50);
    expect(
      await getOffsetHeight(page, TABSET1_PANE),
      'TabSet1 should not be left hidden after the zoom is undrawn',
    ).toBeGreaterThan(50);
  });

  // The sidebar maximizes by zooming its column, not its quadrant -- but it can
  // still be the pane-zoomed window (Zoom Chat -> zoomTab -> fullyMaximizeWindow)
  // and zoomColumn's un-zoom branch does not clear pane-zoom bookkeeping. Hence
  // its own zoom-aware dispatch.
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

  // The same defect through the menu: View > Panes > Zoom Sidebar calls
  // zoomColumn directly and never reaches the frame's maximize action.
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

  // Not redundant: refreshSidebar implements the location toggle as
  // showSidebar(false) then showSidebar(true), so the hide branch is what
  // carries "move the sidebar while a pane is zoomed".
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
    // Client state ships on a passive 5s timer (PushClientStateEvent defaults to
    // active=false), so a reload straight after the zoom outruns the save and
    // leaves nothing to replay. Wait for the RPC that carries it.
    const zoomPersisted = page.waitForResponse(
      (response) =>
        response.url().includes('set_client_state') &&
        (response.request().postData() ?? '').includes('MaximizedTab'),
      { timeout: 15000 },
    );
    await zoomConsolePane(page);
    const persistResponse = await zoomPersisted;
    // A rejected RPC still returns HTTP 200 with an error member, so ok() alone
    // does not prove the zoom was stored -- and an unstored zoom would make
    // everything below pass vacuously.
    expect(persistResponse.ok(), 'the set_client_state carrying the zoom must succeed').toBe(true);
    expect(
      await persistResponse.text(),
      'the set_client_state carrying the zoom must not be rejected',
    ).not.toContain('"error"');

    // isZoomedColumnState discards zoomed column widths on restore (#16688), so
    // the zoom's other half must not come back either: PaneManager's TabZoom, and
    // DualWindowLayoutPanel's quadrant state (HIDE for a Console zoom). Otherwise
    // the session reopens with default widths and live zoom bookkeeping.
    await page.reload();
    // TABSET1_PANE attaches at construction, so it gates nothing here.
    // window.rstudio.ready is the right signal but not a state-applied gate:
    // later-task startup work has not run when it flips, and
    // initializeWorkbench() can return early with a ReloadEvent, so it does not
    // even prove the panes exist. The assertion below waits for both.
    await page.waitForFunction(() => window.rstudio?.ready === true, null, {
      timeout: TIMEOUTS.sessionRestart,
    });

    // A must-not-happen assertion against exactly that late work: a replayed
    // zoom lands on onInit's 200ms Timer, after ready. One sample -- or a poll,
    // which returns on its first pass -- goes green on the pre-replay layout.
    await expectHoldsFor(RELOAD_ZOOM_REPLAY_WINDOW_MS, async () => {
      expect(
        await getOffsetWidth(page, TABSET1_PANE),
        'the zoom must not be replayed after a reload that discarded its column widths',
      ).toBeGreaterThan(50);
      expect(
        await isCommandChecked(page, 'layoutZoomConsole'),
        'a zoom whose column widths were not restored must not stay tracked',
      ).toBe(false);
      expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(50);
      // DualWindowLayoutPanel persists the quadrant half separately: a Console
      // zoom puts Source in HIDE, so skipping only the column half is not enough.
      expect(
        await getOffsetHeight(page, SOURCE_PANE),
        'the zoomed pane\'s sibling quadrant must not come back hidden',
      ).toBeGreaterThan(50);
    });
  });

  // -------------------------------------------------------------------------
  // #18448: the remaining routes that re-lay-out the panes without clearing
  // the zoom bookkeeping. Same failure mode as #18444: bookkeeping that
  // outlives the drawn zoom re-zooms the next pane that raises itself.

  test('Zooming a different column while a pane is zoomed ends the pane zoom (#18448)', async ({ rstudioPage: page }) => {
    // Deselect Plots while the layout is still normal, so its selection later
    // is a clear signal that the raise happened.
    await executeCommand(page, 'activateFiles');
    await expect.poll(() => isPlotsTabSelected(page), { timeout: 5000 }).toBe(false);

    await zoomConsolePane(page);

    // Ask for a column other than the one the pane zoom collapsed into. This
    // falls through zoomColumn's per-column branches; before the fix those
    // never cleared the pane-zoom bookkeeping, so Zoom Console and Zoom Right
    // Column ended up check-marked at once.
    await executeCommand(page, 'layoutZoomRightColumn');
    await expect.poll(
      async () =>
        (await getOffsetWidth(page, CONSOLE_PANE)) < 50 &&
        (await getOffsetWidth(page, TABSET1_PANE)) > 200,
      { timeout: 10000 },
    ).toBe(true);

    expect(
      await isCommandChecked(page, 'layoutZoomConsole'),
      'switching to a column zoom must end the tracked pane zoom',
    ).toBe(false);
    expect(
      await isCommandChecked(page, 'layoutZoomRightColumn'),
      'the requested column zoom should be the one active',
    ).toBe(true);

    const zoomedTabSet1Width = await getOffsetWidth(page, TABSET1_PANE);

    // The original #18444 cascade: with stale bookkeeping, a pane raising
    // itself hands the whole window to fullyMaximizeWindow, hiding its
    // sibling. Raise Plots through its activate command rather than plot():
    // typing at the console raises the Console pane, which the column zoom
    // has collapsed to width 0, and an ensure-visible on a zero-width pane
    // legitimately redistributes the columns. Plots lives in the zoomed right
    // column, so on a fixed build nothing needs to move.
    await executeCommand(page, 'activatePlots');
    await expect.poll(() => isPlotsTabSelected(page), { timeout: 10000 }).toBe(true);
    await sleep(TIMEOUTS.layoutSettle);

    // A stale zoom collapses the layout on a later pass than the raise, so hold
    // the assertions over a window instead of sampling once.
    await expectHoldsFor(1000, async () => {
      expect(
        await getOffsetHeight(page, TABSET1_PANE),
        'a pane raising itself must not hide TabSet1 via a stale pane zoom',
      ).toBeGreaterThan(50);
      expectWidthClose(
        await getOffsetWidth(page, TABSET1_PANE),
        zoomedTabSet1Width,
        0.05,
        'zoomed TabSet1 after the raise',
      );
      expect(
        await getOffsetWidth(page, CONSOLE_PANE),
        'the raise must not move the layout: Console stays collapsed by the column zoom',
      ).toBeLessThan(50);
    });
  });

  // The two-click repro from the issue: zoom the sidebar column, then close
  // the sidebar from its header button. removeSidebarWidget hands the freed
  // width to the center column, so without the fix the right column stays at
  // 1px -- Environment / Plots / Help gone, and no zoom check-marked anywhere.
  test('Closing the sidebar while its column is zoomed restores the columns (#18448)', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    await executeCommand(page, 'layoutZoomSidebar');
    await expect.poll(
      async () =>
        (await getOffsetWidth(page, TABSET1_PANE)) < 50 &&
        (await getOffsetWidth(page, SIDEBAR_PANE)) > 200,
      { timeout: 10000 },
    ).toBe(true);
    expect(
      await isCommandChecked(page, 'layoutZoomSidebar'),
      'precondition: the sidebar column zoom should be tracked',
    ).toBe(true);

    await page.locator(SIDEBAR_CLOSE_BTN).click();
    await expect(page.locator(SIDEBAR_PANE)).toHaveCount(0, { timeout: 5000 });

    await waitForStableWidth(page, TABSET1_PANE, { min: 50 });
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(50);
    expect(await getOffsetWidth(page, TABSET2_PANE)).toBeGreaterThan(50);
  });

  // MainSplitPanel.addLeftWidget rebuilds the split panel and re-applies
  // persisted widths, undrawing a zoom. Before the fix the bookkeeping
  // survived: the zoomed pane stayed EXCLUSIVE (its sibling hidden), and the
  // next pane to raise itself was re-zoomed.
  test('Adding a source column while a pane is zoomed ends the zoom (#18448)', async ({ rstudioPage: page }) => {
    await zoomConsolePane(page);

    await executeCommand(page, 'newSourceColumn');
    await expect(page.locator(SOURCE1_PANE)).toBeVisible({ timeout: 10000 });

    try {
      await waitForStableWidth(page, TABSET1_PANE, { min: 50 });
      expect(
        await isCommandChecked(page, 'layoutZoomConsole'),
        'adding a source column must end the tracked pane zoom',
      ).toBe(false);
      expect(
        await getOffsetHeight(page, SOURCE_PANE),
        'the zoomed pane\'s sibling must come back when the zoom is undrawn',
      ).toBeGreaterThan(50);

      const consoleActions = new ConsolePaneActions(page);
      await consoleActions.executeInConsole('plot(1:10)');
      await expect.poll(() => isPlotsTabSelected(page), { timeout: 15000 }).toBe(true);
      await sleep(TIMEOUTS.layoutSettle);

      expect(
        await getOffsetWidth(page, CONSOLE_PANE),
        'a pane raising itself must not collapse the layout via a stale pane zoom',
      ).toBeGreaterThan(50);
    } finally {
      // newSourceColumn auto-creates an Untitled doc in the new column; close
      // it so the column is removed and later tests see the default layout.
      await page.locator(SOURCE1_PANE).click();
      await executeCommand(page, 'closeSourceDoc');
      await expect(page.locator(SOURCE1_PANE)).toHaveCount(0, { timeout: 10000 });
    }
  });

  // The removal half of the same defect: MainSplitPanel.removeLeftWidget does
  // the same clearForRefresh + initialize rebuild as addLeftWidget, so closing
  // a source column undraws a zoom too and must end the bookkeeping with it.
  test('Closing a source column while a pane is zoomed ends the zoom (#18448)', async ({ rstudioPage: page }) => {
    // Match the added column by shape, not by number: SourceColumnManager's
    // column counter only ever climbs within a client session (it restarts at 1
    // on a UI reload), so which SourceN a fresh column gets depends on what ran
    // before this test.
    const extraColumn = page.locator(EXTRA_SOURCE_COLUMN_PANES);
    await expect(extraColumn, 'precondition: no extra source column to start').toHaveCount(0);

    await executeCommand(page, 'newSourceColumn');
    await expect(extraColumn).toHaveCount(1, { timeout: 10000 });

    try {
      // Make the new column the active one before zooming: the zoom collapses
      // it to zero width, and a click on a zero-width pane never lands.
      // closeSourceDoc then targets its auto-created Untitled, whose close
      // removes the column.
      await extraColumn.click();

      await zoomConsolePane(page);

      await executeCommand(page, 'closeSourceDoc');
      await expect(extraColumn).toHaveCount(0, { timeout: 10000 });

      await waitForStableWidth(page, TABSET1_PANE, { min: 50 });
      expect(
        await isCommandChecked(page, 'layoutZoomConsole'),
        'closing a source column must end the tracked pane zoom',
      ).toBe(false);
      expect(
        await getOffsetHeight(page, SOURCE_PANE),
        'the zoomed pane\'s sibling must come back when the zoom is undrawn',
      ).toBeGreaterThan(50);

      const consoleActions = new ConsolePaneActions(page);
      await consoleActions.executeInConsole('plot(1:10)');
      await expect.poll(() => isPlotsTabSelected(page), { timeout: 15000 }).toBe(true);
      await sleep(TIMEOUTS.layoutSettle);

      // A stale zoom re-collapses the layout when Plots raises itself, which
      // can land after the first sample -- hold the assertion over a window.
      await expectHoldsFor(1000, async () => {
        expect(
          await getOffsetWidth(page, CONSOLE_PANE),
          'a pane raising itself must not collapse the layout via a stale pane zoom',
        ).toBeGreaterThan(50);
      });
    } finally {
      // The body closes the column; clean up only when it failed before that.
      if ((await extraColumn.count()) > 0) {
        await extraColumn.click();
        await executeCommand(page, 'closeSourceDoc');
        await expect(extraColumn).toHaveCount(0, { timeout: 10000 });
      }
    }
  });

  // Moving a zoomed sidebar exercises refreshSidebar, which captures the
  // sidebar width before its hide/show cycle ends the zoom and re-applies it
  // after. A zoomed sidebar's captured width is nearly the whole panel, so
  // re-applying it would zoom the sidebar right back (#18448).
  test('Moving a zoomed sidebar to the other side ends the zoom (#18448)', async ({ rstudioPage: page }) => {
    await showSidebar(page);

    await executeCommand(page, 'layoutZoomSidebar');
    await expect.poll(
      async () =>
        (await getOffsetWidth(page, TABSET1_PANE)) < 50 &&
        (await getOffsetWidth(page, SIDEBAR_PANE)) > 200,
      { timeout: 10000 },
    ).toBe(true);
    const zoomedSidebarWidth = await getOffsetWidth(page, SIDEBAR_PANE);

    await executeCommand(page, 'toggleSidebarLocation');
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 10000 });
    await waitForStableWidth(page, TABSET1_PANE, { min: 50 });

    // The relocated sidebar must come back at a normal width, not the
    // captured zoomed one, with every column visible again. The deferred
    // width re-apply lands after the recreation settles, so hold the
    // assertion over a window rather than sampling once.
    await expectHoldsFor(1000, async () => {
      expect(
        await getOffsetWidth(page, SIDEBAR_PANE),
        'the relocated sidebar must not keep its zoomed width',
      ).toBeLessThan(zoomedSidebarWidth / 2);
      expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(50);
      expect(await getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThan(50);
      expect(await getOffsetWidth(page, TABSET2_PANE)).toBeGreaterThan(50);
    });
  });

  // Recreating the sidebar (hide/show, location change) replaces its
  // WorkbenchTabPanel. The selection must survive: the reused
  // SelectedTabStateValue no longer runs onInit's restore, so PaneManager
  // carries the outgoing panel's selected tab to the replacement explicitly.
  test('Sidebar recreation keeps the selected tab (#18448)', async ({ rstudioPage: page }) => {
    // Two tabs are needed to observe a selection reset; put Viewer in the
    // sidebar next to Chat via the Pane Layout dialog.
    await openPaneLayoutOptions(page);
    await toggleTab(page, PL_SIDEBAR, 'Viewer');
    const sidebarVisibleCheckbox = page.locator(PL_SIDEBAR_VISIBLE);
    if (!(await sidebarVisibleCheckbox.isChecked())) {
      await sidebarVisibleCheckbox.click();
      await sleep(TIMEOUTS.layoutSettle);
    }
    await page.locator(PREFERENCES_CONFIRM).click();
    await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 10000 });
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 10000 });

    try {
      const sidebarTabs = page.locator(`${SIDEBAR_PANE} [role="tab"]`);
      await expect(sidebarTabs).toHaveCount(2, { timeout: 10000 });

      // Select the second tab, so a reset-to-first is distinguishable.
      await sidebarTabs.nth(1).click();
      await expect(sidebarTabs.nth(1)).toHaveAttribute('aria-selected', 'true');
      const selectedTabId = await sidebarTabs.nth(1).getAttribute('id');

      await executeCommand(page, 'toggleSidebarLocation');
      await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 10000 });
      await waitForStableWidth(page, SIDEBAR_PANE, { min: 50 });

      // Same tab set, so the same position is the same tab; pin it by id too.
      const tabsAfter = page.locator(`${SIDEBAR_PANE} [role="tab"]`);
      await expect(tabsAfter).toHaveCount(2, { timeout: 10000 });
      expect(await tabsAfter.nth(1).getAttribute('id')).toBe(selectedTabId);
      await expect(
        tabsAfter.nth(1),
        'the selected tab must survive the sidebar recreation',
      ).toHaveAttribute('aria-selected', 'true');
    } finally {
      await openPaneLayoutOptions(page);
      await resetPaneLayoutInDialog(page);
      const cleanupCheckbox = page.locator(PL_SIDEBAR_VISIBLE);
      if (await cleanupCheckbox.isChecked()) {
        await cleanupCheckbox.click();
        await sleep(TIMEOUTS.layoutSettle);
      }
      await page.locator(PREFERENCES_CONFIRM).click();
      await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 10000 });
    }
  });

  // A maximize height request (EnsureHeightEvent.MAXIMIZED) is a vertical
  // operation, but the sidebar's maximize action is a horizontal column zoom
  // (layoutZoomSidebar). Routing the request through the frame's maximize
  // action would collapse every other column, so LogicalWindow keeps the
  // direct conversion for non-zoomed windows. Here rather than in
  // viewer-maximize-zoom.test.ts because moving Viewer into the sidebar needs
  // this file's Pane Layout dialog helpers (the window.rstudio prefs bridge
  // cannot set object-valued prefs like panes).
  test('A viewer in the sidebar keeps its maximize request vertical (#18448)', async ({ rstudioPage: page }) => {
    await openPaneLayoutOptions(page);
    await toggleTab(page, PL_SIDEBAR, 'Viewer');
    const sidebarVisibleCheckbox = page.locator(PL_SIDEBAR_VISIBLE);
    if (!(await sidebarVisibleCheckbox.isChecked())) {
      await sidebarVisibleCheckbox.click();
      await sleep(TIMEOUTS.layoutSettle);
    }
    await page.locator(PREFERENCES_CONFIRM).click();
    await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 10000 });
    await expect(page.locator(SIDEBAR_PANE)).toBeVisible({ timeout: 10000 });
    await expect(page.locator(`${SIDEBAR_PANE} ${VIEWER_TAB}`)).toBeVisible({ timeout: 10000 });

    try {
      // The invariant is relative -- a vertical height request must change no
      // column width -- because the starting widths aren't pristine here: a
      // prior sidebar hide can record a collapsed center that the show path
      // then deliberately preserves (savedCenterCollapsed_).
      await waitForStableWidth(page, SIDEBAR_PANE, { min: 50 });
      await sleep(TIMEOUTS.layoutSettle);

      // Typing at a zero-width console raises it, and an ensure-visible on a
      // zero-width pane legitimately redistributes the columns. Give the
      // center a healthy width first, so the height request is the only
      // remaining actor. Press-until-wide rather than a fixed count: a fixed
      // 60 presses squeezes the right column to zero instead.
      if ((await getOffsetWidth(page, CONSOLE_PANE)) < 200) {
        await focusSplitter(page);
        for (let i = 0; i < 60 && (await getOffsetWidth(page, CONSOLE_PANE)) < 200; i++) {
          await page.keyboard.press('ArrowRight');
        }
        await sleep(TIMEOUTS.layoutSettle);
      }
      const widthsBefore = async () => ({
        console: await getOffsetWidth(page, CONSOLE_PANE),
        tabSet1: await getOffsetWidth(page, TABSET1_PANE),
        tabSet2: await getOffsetWidth(page, TABSET2_PANE),
        sidebar: await getOffsetWidth(page, SIDEBAR_PANE),
      });
      const before = await widthsBefore();
      expect(
        before.tabSet1,
        'precondition: TabSet1 visible before the height request',
      ).toBeGreaterThan(50);

      const consoleActions = new ConsolePaneActions(page);
      await consoleActions.executeInConsole(
        'f <- file.path(tempdir(), "t18448.html"); ' +
        'writeLines("<h1>hi</h1>", f); ' +
        '.rs.api.viewer(f, height = "maximize")',
      );

      // Wait on the effect, not the prompt: the request also brings the
      // Viewer tab to the front, so its selection shows the event landed.
      await expect.poll(
        async () =>
          (await page.locator(`${SIDEBAR_PANE} ${VIEWER_TAB}`).getAttribute('aria-selected')) === 'true',
        { timeout: 10000 },
      ).toBe(true);
      await sleep(TIMEOUTS.layoutSettle);

      // A misrouted height request turns into a column zoom on a later layout
      // pass, so hold the invariant over a window rather than sampling once.
      await expectHoldsFor(1000, async () => {
        const after = await widthsBefore();
        for (const key of Object.keys(before) as (keyof typeof before)[]) {
          expect(
            Math.abs(after[key] - before[key]),
            `a vertical height request must not move the ${key} column (${before[key]} -> ${after[key]})`,
          ).toBeLessThan(10);
        }
        expect(await isCommandChecked(page, 'layoutZoomSidebar')).toBe(false);
      });
    } finally {
      // Put Viewer back in TabSet2 and hide the sidebar via the dialog's
      // reset link, the same cleanup the suite's afterAll uses.
      await openPaneLayoutOptions(page);
      await resetPaneLayoutInDialog(page);
      const cleanupCheckbox = page.locator(PL_SIDEBAR_VISIBLE);
      if (await cleanupCheckbox.isChecked()) {
        await cleanupCheckbox.click();
        await sleep(TIMEOUTS.layoutSettle);
      }
      await page.locator(PREFERENCES_CONFIRM).click();
      await expect(page.locator(DIALOG_BOX)).toHaveCount(0, { timeout: 10000 });
    }
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

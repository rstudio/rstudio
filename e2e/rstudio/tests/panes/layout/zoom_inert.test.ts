// A column that a zoom or a splitter drag collapses to nothing must leave the
// accessibility tree and the Tab order (#19181). Quadrants hidden by a maximize
// or a splitter snap are display:none; they are covered here so they stay so.

import { test, expect } from '@fixtures/rstudio.fixture';
import {
  documentCloseAllNoSave, executeCommand, isCommandChecked, resetLayoutZoom,
} from '@utils/commands';
import { executeInConsole, focusConsole, waitForConsoleIdle } from '@pages/console_pane.page';
import { captureResult, killAllTerminals, XTERM_SELECTOR } from '@utils/terminal';
import { TIMEOUTS } from '@utils/constants';
import type { Locator, Page } from 'playwright';

const CONSOLE_PANE = '#rstudio_Console_pane';
const SOURCE_PANE = '#rstudio_Source_pane';
const TABSET1_PANE = '#rstudio_TabSet1_pane';
const TABSET2_PANE = '#rstudio_TabSet2_pane';
const HISTORY_TAB = '#rstudio_workbench_tab_history';
const SIDEBAR_PANE = '#rstudio_Sidebar_pane';
const MIDDLE_COLUMN_SPLITTER = '#rstudio_middle_column_splitter';
const SIDEBAR_COLUMN_SPLITTER = '#rstudio_sidebar_column_splitter';
const RIGHT_ROW_SPLITTER = '[role="separator"][aria-label="right splitter"]';

function paneRegion(page: Page, pane: string): Locator {
  return page.getByRole('region').and(page.locator(pane));
}

// Whether Tab and clicks can't reach the pane: it sits under an inert element,
// or is not rendered at all. A column collapsed to 0px still renders, so only
// inert keeps its controls out of the Tab order.
async function isUnreachable(page: Page, pane: string): Promise<boolean> {
  return page.locator(pane).evaluate(
    el => el.closest('[inert]') !== null || !el.checkVisibility());
}

async function expectHidden(page: Page, ...panes: string[]): Promise<void> {
  for (const pane of panes) {
    await expect(paneRegion(page, pane), `${pane} in accessibility tree`).toHaveCount(0);
    await expect.poll(() => isUnreachable(page, pane), { message: `${pane} unreachable` })
      .toBe(true);
  }
}

async function expectExposed(page: Page, ...panes: string[]): Promise<void> {
  for (const pane of panes) {
    await expect(paneRegion(page, pane), `${pane} in accessibility tree`).toHaveCount(1);
    await expect.poll(() => isUnreachable(page, pane), { message: `${pane} unreachable` })
      .toBe(false);
  }
}

async function hasFocusWithin(page: Page, pane: string): Promise<boolean> {
  return page.locator(pane).evaluate(el => el.contains(document.activeElement));
}

// Whether focus is on something the user can still reach, rather than dropped
// to the page body or left inside a hidden column.
async function hasReachableFocus(page: Page): Promise<boolean> {
  return page.evaluate(() => {
    const el = document.activeElement;
    return el !== null && el !== document.body &&
      el.closest('[inert]') === null && el.checkVisibility();
  });
}

// Re-focuses on each poll: a terminal that is still starting can take focus
// after the first attempt.
async function focusSelectedTab(page: Page, pane: string): Promise<void> {
  const tab = page.locator(`${pane} [role="tab"][aria-selected="true"]`);
  await expect.poll(async () => {
    await tab.focus();
    return hasFocusWithin(page, pane);
  }).toBe(true);
}

async function getOffsetWidth(page: Page, selector: string): Promise<number> {
  return page.locator(selector).evaluate(el => (el as HTMLElement).offsetWidth);
}

// A value once a resize has finished: two reads 150ms apart agree.
async function getSettled(read: () => Promise<number>): Promise<number> {
  let last = Number.NaN;
  await expect.poll(async () => {
    const value = await read();
    const settled = value === last;
    last = value;
    return settled;
  }, { intervals: [150] }).toBe(true);
  return last;
}

async function getSettledWidth(page: Page, selector: string): Promise<number> {
  return getSettled(() => getOffsetWidth(page, selector));
}

// A window resize lays the columns out over more than one pass, and a drag
// started from a position a later pass moves misses the 7px splitter.
async function waitForSplitterToSettle(page: Page, splitter: string): Promise<void> {
  await getSettled(async () => (await page.locator(splitter).boundingBox())?.x ?? -1);
}

async function dragSplitterTo(page: Page, splitter: string, x: number, y: number): Promise<void> {
  const box = await page.locator(splitter).boundingBox();
  if (!box)
    throw new Error(`Splitter ${splitter} has no bounding box; is it rendered?`);
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(x, y, { steps: 10 });
  await page.mouse.up();
}

async function setSidebarVisible(page: Page, visible: boolean): Promise<void> {
  if ((await page.locator(SIDEBAR_PANE).count() > 0) === visible)
    return;
  await executeCommand(page, 'toggleSidebar');
  await expect(page.locator(SIDEBAR_PANE)).toHaveCount(visible ? 1 : 0);
}

test.describe('Panes hidden by zoom leave the accessibility tree (#19181)', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await resetLayoutZoom(page);
    await setSidebarVisible(page, false);
    await expectExposed(page, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);
  });

  test('zooming the left column hides the right column', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'layoutZoomLeftColumn');
    await expectHidden(page, TABSET1_PANE, TABSET2_PANE);
    await expectExposed(page, CONSOLE_PANE);

    await executeCommand(page, 'layoutZoomLeftColumn');
    await expectExposed(page, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);
  });

  test('zooming the right column hides the left column', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'layoutZoomRightColumn');
    await expectHidden(page, CONSOLE_PANE);
    await expectExposed(page, TABSET1_PANE, TABSET2_PANE);

    await executeCommand(page, 'layoutZoomRightColumn');
    await expectExposed(page, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);
  });

  test('zooming a pane hides the panes around it', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'layoutZoomConsole');
    await expectHidden(page, TABSET1_PANE, TABSET2_PANE);
    await expectExposed(page, CONSOLE_PANE);

    // Moving the zoom to a pane in another column swaps which column is hidden.
    await executeCommand(page, 'layoutZoomFiles');
    await expectHidden(page, CONSOLE_PANE, TABSET1_PANE);
    await expectExposed(page, TABSET2_PANE);

    await executeCommand(page, 'layoutEndZoom');
    await expectExposed(page, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);
  });

  test('zooming the sidebar hides the other columns', async ({ rstudioPage: page }) => {
    await setSidebarVisible(page, true);

    await executeCommand(page, 'layoutZoomSidebar');
    await expectHidden(page, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);
    await expectExposed(page, SIDEBAR_PANE);

    await executeCommand(page, 'layoutZoomSidebar');
    await expectExposed(page, SIDEBAR_PANE, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);

    await executeCommand(page, 'layoutZoomLeftColumn');
    await expectHidden(page, SIDEBAR_PANE, TABSET1_PANE, TABSET2_PANE);
    await expectExposed(page, CONSOLE_PANE);
  });

  test('dragging a column splitter closed hides the column and moves focus out', async ({ rstudioPage: page }) => {
    const splitter = page.locator(MIDDLE_COLUMN_SPLITTER);
    const box = await splitter.boundingBox();
    if (!box)
      throw new Error('Middle column splitter has no bounding box; is it rendered?');
    const windowWidth = await page.evaluate(() => window.innerWidth);

    const splitterX = box.x + box.width / 2;
    const splitterY = box.y + box.height / 2;

    // A custom, unzoomed layout survives resetLayoutZoom(), so put the
    // splitter back however the test ends.
    try {
      // Mouse: drag the right column shut against the window edge, then back.
      // The splitter takes no focus on mousedown, so focus is still in the
      // column it shuts and has to move out.
      await focusSelectedTab(page, TABSET1_PANE);
      await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, windowWidth - 1, splitterY);
      await expect.poll(() => getOffsetWidth(page, TABSET1_PANE)).toBeLessThan(20);
      await expectHidden(page, TABSET1_PANE, TABSET2_PANE);
      await expect.poll(() => hasReachableFocus(page)).toBe(true);

      await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, splitterX, splitterY);
      await expectExposed(page, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);

      // Keyboard: walk the splitter left until the Console column is shut.
      await splitter.focus();
      for (let i = 0; i < 100 && (await getOffsetWidth(page, CONSOLE_PANE)) > 0; i++)
        await page.keyboard.press('ArrowLeft');
      await expectHidden(page, CONSOLE_PANE);

      for (let i = 0; i < 100 && (await getOffsetWidth(page, CONSOLE_PANE)) < 100; i++)
        await page.keyboard.press('ArrowRight');
      await expectExposed(page, CONSOLE_PANE);
    } finally {
      await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, splitterX, splitterY);
      await expect.poll(async () => Math.abs(((await splitter.boundingBox())?.x ?? 0) - box.x))
        .toBeLessThanOrEqual(2);
    }
  });

  // Columns keep their share of the window as it resizes, so a shrink collapses
  // only a column that was already narrow.
  test('shrinking the window past a narrow column hides it and moves focus out', async ({ rstudioPage: page }) => {
    const viewport = await page.evaluate(() => ({ width: innerWidth, height: innerHeight }));
    const box = await page.locator(MIDDLE_COLUMN_SPLITTER).boundingBox();
    if (!box)
      throw new Error('Middle column splitter has no bounding box; is it rendered?');
    const splitterY = box.y + box.height / 2;

    try {
      await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, viewport.width - 40, splitterY);
      await expect.poll(() => getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThanOrEqual(20);
      await expectExposed(page, TABSET1_PANE);
      await focusSelectedTab(page, TABSET1_PANE);

      await page.setViewportSize({ width: Math.round(viewport.width / 3), height: viewport.height });
      await expect.poll(() => getOffsetWidth(page, TABSET1_PANE)).toBeLessThan(20);
      await expectHidden(page, TABSET1_PANE, TABSET2_PANE);
      await expect.poll(() => hasReachableFocus(page)).toBe(true);
    } finally {
      await page.setViewportSize(viewport);
      await waitForSplitterToSettle(page, MIDDLE_COLUMN_SPLITTER);
      await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, box.x + box.width / 2, splitterY);
      await expect.poll(async () => Math.abs(
        ((await page.locator(MIDDLE_COLUMN_SPLITTER).boundingBox())?.x ?? 0) - box.x))
        .toBeLessThanOrEqual(2);
    }
  });

  // A resize first lays out the old column widths at the new window width,
  // which can squeeze the fill column shut until the proportional widths land.
  test('shrinking the window keeps focus in a narrow column that stays open', async ({ rstudioPage: page }) => {
    const viewport = await page.evaluate(() => ({ width: innerWidth, height: innerHeight }));
    const box = await page.locator(MIDDLE_COLUMN_SPLITTER).boundingBox();
    if (!box)
      throw new Error('Middle column splitter has no bounding box; is it rendered?');
    const splitterY = box.y + box.height / 2;

    try {
      await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, 200, splitterY);
      await expect.poll(() => getOffsetWidth(page, CONSOLE_PANE)).toBeLessThan(250);
      await focusConsole(page);

      await page.setViewportSize({ width: Math.round(viewport.width / 2), height: viewport.height });
      await expect.poll(() => getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThanOrEqual(20);
      await expectExposed(page, CONSOLE_PANE);
      // Focus must not move after the layout settles, so sample for a while.
      for (let i = 0; i < 10; i++) {
        expect(await hasFocusWithin(page, CONSOLE_PANE), `focus in Console, sample ${i}`).toBe(true);
        await page.waitForTimeout(50);
      }
    } finally {
      await page.setViewportSize(viewport);
      await waitForSplitterToSettle(page, MIDDLE_COLUMN_SPLITTER);
      await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, box.x + box.width / 2, splitterY);
      await expect.poll(async () => Math.abs(
        ((await page.locator(MIDDLE_COLUMN_SPLITTER).boundingBox())?.x ?? 0) - box.x))
        .toBeLessThanOrEqual(2);
    }
  });

  // The zoom moves to Source while its column is still collapsed, and the new
  // editor must still get focus.
  test('opening a document while a pane is zoomed focuses the editor', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'layoutZoomFiles');
    await expectHidden(page, CONSOLE_PANE, SOURCE_PANE);

    await executeCommand(page, 'newSourceDoc');
    await expectExposed(page, SOURCE_PANE);
    await expectHidden(page, TABSET2_PANE);
    await expect.poll(() => hasFocusWithin(page, SOURCE_PANE)).toBe(true);

    await documentCloseAllNoSave(page);
  });

  // Without this, the browser drops focus to the page body when the element
  // that had it becomes inert or stops being rendered.
  test('a pane zoom that hides the focused column focuses the zoomed pane', async ({ rstudioPage: page }) => {
    await focusConsole(page);

    await executeCommand(page, 'layoutZoomFiles');
    await expectHidden(page, CONSOLE_PANE);
    await expect.poll(() => hasFocusWithin(page, TABSET2_PANE)).toBe(true);
  });

  test('a column zoom that hides the focused column focuses the zoomed column', async ({ rstudioPage: page }) => {
    await focusConsole(page);

    await executeCommand(page, 'layoutZoomRightColumn');
    await expectHidden(page, CONSOLE_PANE);
    await expect.poll(() => hasFocusWithin(page, TABSET1_PANE)).toBe(true);
  });

  test('a pane zoom that hides the focused quadrant focuses the zoomed pane', async ({ rstudioPage: page }) => {
    await page.locator(`${TABSET1_PANE} [role="tab"]`).first().focus();
    await expect.poll(() => hasFocusWithin(page, TABSET1_PANE)).toBe(true);

    await executeCommand(page, 'layoutZoomFiles');
    await expectHidden(page, TABSET1_PANE);
    await expect.poll(() => hasFocusWithin(page, TABSET2_PANE)).toBe(true);
  });

  test('a sidebar pane zoom that hides the focused column focuses the sidebar', async ({ rstudioPage: page }) => {
    await setSidebarVisible(page, true);
    await focusConsole(page);

    await executeCommand(page, 'layoutZoomChat');
    await expectHidden(page, CONSOLE_PANE);
    await expect.poll(() => hasFocusWithin(page, SIDEBAR_PANE)).toBe(true);
    // Focusing the sidebar must not raise another pane and end the zoom.
    expect(await isCommandChecked(page, 'layoutZoomChat')).toBe(true);
  });

  // Zoom Files first selects the Files tab, which hides the Plots content that
  // had focus, and only then zooms.
  test('zooming a tab from another tab in its pane focuses the zoomed pane', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'activatePlots');
    const plotsButtonId = await page.locator(`${TABSET2_PANE} button[id]:not([disabled]):visible`)
      .first().getAttribute('id');
    const plotsButton = page.locator(`#${plotsButtonId}`);
    await plotsButton.focus();
    await expect.poll(() => hasFocusWithin(page, TABSET2_PANE)).toBe(true);

    await executeCommand(page, 'layoutZoomFiles');
    await expectHidden(page, CONSOLE_PANE);
    await expect(plotsButton).toBeHidden();
    await expect.poll(() => hasFocusWithin(page, TABSET2_PANE)).toBe(true);
  });

  // With Files already zoomed, Zoom Plots only switches the tab, which hides
  // the Files content that had focus.
  test('zooming another tab of the zoomed pane keeps focus in the pane', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'layoutZoomFiles');
    await expectHidden(page, CONSOLE_PANE);
    const filesButtonId = await page.locator(`${TABSET2_PANE} button[id]:not([disabled]):visible`)
      .first().getAttribute('id');
    const filesButton = page.locator(`#${filesButtonId}`);
    await filesButton.focus();
    await expect.poll(() => hasFocusWithin(page, TABSET2_PANE)).toBe(true);

    await executeCommand(page, 'layoutZoomPlots');
    await expect(filesButton).toBeHidden();
    expect(await isCommandChecked(page, 'layoutZoomPlots')).toBe(true);
    await expect.poll(() => hasFocusWithin(page, TABSET2_PANE)).toBe(true);
  });

  // Terminal focuses itself only on selection, so its pane needs the fallback
  // to a control; Background Jobs (or whichever tab is last) takes focus.
  for (const [label, tabName] of [['Terminal', 'Terminal'], ['its last', null]] as const) {
    test(`a column zoom that focuses the Console pane keeps ${label} tab selected`, async ({ rstudioPage: page }) => {
      // With no documents open, the Console is the only pane in its column.
      await documentCloseAllNoSave(page);
      await expect(page.locator(SOURCE_PANE)).toBeHidden();
      const tabs = page.locator(`${CONSOLE_PANE} [role="tab"]`);
      const otherTab = tabName ? tabs.filter({ hasText: tabName }) : tabs.last();
      await otherTab.click();
      await expect(otherTab).toHaveAttribute('aria-selected', 'true');
      try {
        // Selecting Terminal on a fresh session starts a shell.
        if (tabName === 'Terminal')
          await expect(page.locator(`${CONSOLE_PANE} ${XTERM_SELECTOR}`)).toBeVisible();

        await focusSelectedTab(page, TABSET1_PANE);

        await executeCommand(page, 'layoutZoomLeftColumn');
        await expectHidden(page, TABSET1_PANE);
        await expect.poll(() => hasFocusWithin(page, CONSOLE_PANE)).toBe(true);
        await expect(otherTab).toHaveAttribute('aria-selected', 'true');
      } finally {
        await resetLayoutZoom(page);
        // Don't leave a shell running for later tests in this worker. The
        // widget can show before the server registers the terminal, so wait
        // for the registration, or the kill finds nothing to kill.
        if (tabName === 'Terminal') {
          await expect.poll(
            () => captureResult(page, 'length(rstudioapi::terminalList())'),
            { timeout: TIMEOUTS.consoleReady },
          ).not.toBe('0');
          await killAllTerminals(page);
          await expect.poll(
            () => captureResult(page, 'length(rstudioapi::terminalList())'),
          ).toBe('0');
        }
        // Brings the Console tab back to the front.
        await focusConsole(page);
      }
    });
  }

  test('a zoom leaves focus alone when the focused pane stays visible', async ({ rstudioPage: page }) => {
    await focusConsole(page);

    await executeCommand(page, 'layoutZoomLeftColumn');
    await expectHidden(page, TABSET1_PANE);
    expect(await hasFocusWithin(page, CONSOLE_PANE)).toBe(true);
  });

  test('maximizing or snapping a quadrant hides its sibling', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'maximizeTabSet1');
    await expectHidden(page, TABSET2_PANE);
    await expectExposed(page, TABSET1_PANE);

    await executeCommand(page, 'maximizeTabSet1');
    await expectExposed(page, TABSET1_PANE, TABSET2_PANE);

    // Dragging the row splitter to the top snaps TabSet1 to its minimized bar.
    const box = await page.locator(RIGHT_ROW_SPLITTER).boundingBox();
    if (!box)
      throw new Error('Right row splitter has no bounding box; is it rendered?');
    const tabSet1Top = (await page.locator(TABSET1_PANE).boundingBox())?.y ?? 0;
    await dragSplitterTo(page, RIGHT_ROW_SPLITTER, box.x + box.width / 2, tabSet1Top);
    await expectHidden(page, TABSET1_PANE);
    await expectExposed(page, TABSET2_PANE);
  });
});

// Show commands select a tab; a column shut by a splitter drag has no zoom
// that will reopen it later, so the command has to (#19184).
test.describe('Show pane commands reopen a column dragged shut (#19184)', () => {
  let splitterHome: { left: number; x: number; y: number };

  test.beforeEach(async ({ rstudioPage: page }) => {
    const box = await page.locator(MIDDLE_COLUMN_SPLITTER).boundingBox();
    if (!box)
      throw new Error('Middle column splitter has no bounding box; is it rendered?');
    splitterHome = { left: box.x, x: box.x + box.width / 2, y: box.y + box.height / 2 };
  });

  // A custom, unzoomed layout survives resetLayoutZoom(), so put the splitter
  // back however the test ends.
  test.afterEach(async ({ rstudioPage: page }) => {
    await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, splitterHome.x, splitterHome.y);
    await expect.poll(async () => Math.abs(
      ((await page.locator(MIDDLE_COLUMN_SPLITTER).boundingBox())?.x ?? 0) - splitterHome.left))
      .toBeLessThanOrEqual(2);
    await expectExposed(page, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);
  });

  async function shutRightColumn(page: Page): Promise<void> {
    const windowWidth = await page.evaluate(() => window.innerWidth);
    await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, windowWidth - 1, splitterHome.y);
    await expectHidden(page, TABSET1_PANE, TABSET2_PANE);
  }

  for (const [command, tabName, preselect] of [
    ['activateFiles', 'Files', false],
    ['activatePlots', 'Plots', true],
  ] as const) {
    test(`${command} reopens the column with ${tabName} showing` +
         `${preselect ? ' when already selected' : ''}`, async ({ rstudioPage: page }) => {
      const otherTab = preselect ? command : 'activatePackages';
      await executeCommand(page, otherTab);
      await shutRightColumn(page);

      await executeCommand(page, command);
      await expect.poll(() => getOffsetWidth(page, TABSET2_PANE)).toBeGreaterThanOrEqual(20);
      await expectExposed(page, CONSOLE_PANE, TABSET2_PANE);
      await expect(page.locator(`${TABSET2_PANE} [role="tab"][aria-selected="true"]`))
        .toContainText(tabName);
    });
  }

  // The zoom records the widths it restores before it selects the tab, so
  // ending it must bring back the shut column the user left.
  test('ending a zoom of a tab in a shut column leaves the column shut', async ({
    rstudioPage: page,
  }) => {
    await shutRightColumn(page);

    await executeCommand(page, 'layoutZoomFiles');
    await expectExposed(page, TABSET2_PANE);

    await executeCommand(page, 'layoutEndZoom');
    await expectHidden(page, TABSET1_PANE, TABSET2_PANE);
  });

  // With the Console column squeezed narrow, the sidebar has the most width to
  // give. A plot raises Plots through two routes in one tick, and the sidebar
  // must still pay only what the reopened column gains. (Console stays open, so
  // focusing it to run the plot doesn't reopen it from the sidebar too.)
  for (const [label, reopen] of [
    ['Show Files', (page: Page) => executeCommand(page, 'activateFiles')],
    ['a plot', (page: Page) => executeInConsole(page, 'plot(1)')],
  ] as const) {
    test(`${label} reopens a column next to a wide sidebar`, async ({ rstudioPage: page }) => {
      await setSidebarVisible(page, true);
      const sidebarSplitter = page.locator(SIDEBAR_COLUMN_SPLITTER);
      const box = await sidebarSplitter.boundingBox();
      if (!box)
        throw new Error('Sidebar column splitter has no bounding box; is it rendered?');
      const splitterY = box.y + box.height / 2;
      try {
        await shutRightColumn(page);
        const windowWidth = await page.evaluate(() => window.innerWidth);
        await dragSplitterTo(page, SIDEBAR_COLUMN_SPLITTER, windowWidth - 120, splitterY);
        await expect.poll(() => getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThanOrEqual(20);
        await expectHidden(page, TABSET2_PANE);
        const sidebarBefore = await getOffsetWidth(page, SIDEBAR_PANE);
        const rightBefore = await getOffsetWidth(page, TABSET2_PANE);

        await reopen(page);
        await expect.poll(() => getOffsetWidth(page, TABSET2_PANE)).toBeGreaterThanOrEqual(20);
        const sidebarAfter = await getSettledWidth(page, SIDEBAR_PANE);
        const gained = (await getOffsetWidth(page, TABSET2_PANE)) - rightBefore;
        expect(Math.abs(sidebarBefore - sidebarAfter - gained), 'sidebar paid only what it gave')
          .toBeLessThanOrEqual(4);
        expect(sidebarAfter).toBeGreaterThanOrEqual(175);
        await expectExposed(page, SIDEBAR_PANE, TABSET2_PANE);
      } finally {
        await executeInConsole(page, 'graphics.off()');
        // Hiding the sidebar saves its width for the next show, so put the
        // splitter back first.
        await dragSplitterTo(page, SIDEBAR_COLUMN_SPLITTER, box.x + box.width / 2, splitterY);
        await expect.poll(async () =>
          Math.abs(((await sidebarSplitter.boundingBox())?.x ?? 0) - box.x))
          .toBeLessThanOrEqual(2);
        await setSidebarVisible(page, false);
      }
    });
  }

  // Console and Source raise themselves rather than going through the tab
  // commands, and reopen their column the same way -- including one dragged
  // nearly but not fully shut.
  for (const [label, x] of [['shut', 1], ['nearly shut', 22]] as const) {
    test(`activateConsole reopens the Console column dragged ${label}`, async ({
      rstudioPage: page,
    }) => {
      await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, x, splitterHome.y);
      await expectHidden(page, CONSOLE_PANE);
      if (label === 'nearly shut')
        expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(0);

      await executeCommand(page, 'activateConsole');
      await expect.poll(() => getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThanOrEqual(20);
      await expectExposed(page, CONSOLE_PANE, TABSET2_PANE);
    });
  }

  // A column zoom can't move to the raised pane as a pane zoom does, so
  // showing the pane ends it -- and the zoom command must then start a fresh
  // zoom rather than "restore" from state left behind.
  for (const [label, show] of [
    ['a plot', (page: Page) => executeInConsole(page, 'plot(1)')],
    ['Show Files', (page: Page) => executeCommand(page, 'activateFiles')],
  ] as const) {
    test(`${label} during a column zoom ends the zoom`, async ({ rstudioPage: page }) => {
      await executeCommand(page, 'layoutZoomLeftColumn');
      await expectHidden(page, TABSET2_PANE);
      try {
        await show(page);
        await expectExposed(page, TABSET2_PANE);
        expect(await isCommandChecked(page, 'layoutZoomLeftColumn')).toBe(false);

        await executeCommand(page, 'layoutZoomLeftColumn');
        await expectHidden(page, TABSET2_PANE);
        expect(await isCommandChecked(page, 'layoutZoomLeftColumn')).toBe(true);
      } finally {
        await resetLayoutZoom(page);
        await executeInConsole(page, 'graphics.off()');
      }
    });
  }

  // A pane zoom shuts the other columns and moves to a pane that raises itself;
  // a Show command for a pane in a shut column moves it the same way.
  test('Show Files during a Console zoom moves the zoom to Files', async ({
    rstudioPage: page,
  }) => {
    try {
      await executeCommand(page, 'layoutZoomConsole');
      await expectHidden(page, TABSET1_PANE, TABSET2_PANE);

      await executeCommand(page, 'activateFiles');
      await expectExposed(page, TABSET2_PANE);
      await expectHidden(page, CONSOLE_PANE, TABSET1_PANE);
      await expect.poll(() => isCommandChecked(page, 'layoutZoomFiles')).toBe(true);

      // Ending the moved zoom restores the layout from before the first one.
      await executeCommand(page, 'layoutEndZoom');
      await expectExposed(page, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);
      await expect.poll(async () => Math.abs(
        ((await page.locator(MIDDLE_COLUMN_SPLITTER).boundingBox())?.x ?? 0) - splitterHome.left))
        .toBeLessThanOrEqual(2);
    } finally {
      await resetLayoutZoom(page);
    }
  });

  // The zoomed pane's partner quadrant is hidden rather than in a shut column,
  // and showing it must move the zoom too, not leave a stale one (#18444).
  test('Show Files while Environment is zoomed moves the zoom to Files', async ({
    rstudioPage: page,
  }) => {
    try {
      await executeCommand(page, 'layoutZoomEnvironment');
      await expectHidden(page, CONSOLE_PANE, TABSET2_PANE);

      await executeCommand(page, 'activateFiles');
      await expectExposed(page, TABSET2_PANE);
      await expectHidden(page, CONSOLE_PANE, TABSET1_PANE);
      await expect.poll(() => isCommandChecked(page, 'layoutZoomFiles')).toBe(true);
      expect(await isCommandChecked(page, 'layoutZoomEnvironment')).toBe(false);

      await executeCommand(page, 'layoutEndZoom');
      await expectExposed(page, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);
      await expect.poll(async () => Math.abs(
        ((await page.locator(MIDDLE_COLUMN_SPLITTER).boundingBox())?.x ?? 0) - splitterHome.left))
        .toBeLessThanOrEqual(2);
    } finally {
      await resetLayoutZoom(page);
    }
  });

  // history() shows History through an ActivatePaneEvent, which code also fires
  // as a side effect (e.g. entering the debugger). It reopens a column dragged
  // shut, but leaves a zoom where the user put it.
  test('history() reopens a column dragged shut', async ({ rstudioPage: page }) => {
    await shutRightColumn(page);

    await executeInConsole(page, 'history()');
    await expect.poll(() => getOffsetWidth(page, TABSET1_PANE)).toBeGreaterThanOrEqual(20);
    await expectExposed(page, TABSET1_PANE);
    await expect(page.locator(HISTORY_TAB)).toHaveAttribute('aria-selected', 'true');
  });

  test('history() during a Console zoom leaves the zoom on Console', async ({
    rstudioPage: page,
  }) => {
    try {
      await executeCommand(page, 'layoutZoomConsole');
      await expectHidden(page, TABSET1_PANE);

      await executeInConsole(page, 'history()');
      await expect(page.locator(HISTORY_TAB)).toHaveAttribute('aria-selected', 'true');
      // A moved zoom would start at once and animate for 300ms, so sample past it.
      for (let i = 0; i < 20; i++) {
        expect(await isCommandChecked(page, 'layoutZoomConsole'), `sample ${i}`).toBe(true);
        expect(await getOffsetWidth(page, TABSET1_PANE), `sample ${i}`).toBe(0);
        await page.waitForTimeout(50);
      }
    } finally {
      await resetLayoutZoom(page);
    }
  });

  // The Help menu's commands raise the Help pane as a help topic does, so they
  // take over a zoom rather than act behind it.
  test('Search R Help during a Console zoom moves the zoom to Help', async ({
    rstudioPage: page,
  }) => {
    try {
      await executeCommand(page, 'layoutZoomConsole');
      await expectHidden(page, TABSET2_PANE);

      await executeCommand(page, 'helpSearch');
      await expectExposed(page, TABSET2_PANE);
      await expectHidden(page, CONSOLE_PANE);
      await expect.poll(() => isCommandChecked(page, 'layoutZoomHelp')).toBe(true);
      await expect.poll(() => hasFocusWithin(page, TABSET2_PANE)).toBe(true);
    } finally {
      await resetLayoutZoom(page);
    }
  });

  // A pane raises itself under its localized title, which PaneManager doesn't
  // know ("Aide" for Help in French), so the moved zoom must still name Help.
  test('Search R Help during a Console zoom moves the zoom to Help in French', async ({
    rstudioPage: page,
  }) => {
    // The workbench syncs the LOCALE cookie the GWT bootstrap reads to the
    // ui_language pref on load, and reloads again if they differed; wait for
    // the language's own Help tab title rather than the first ready.
    const reloadIn = async (helpTitle: string) => {
      await page.reload();
      await expect(page.locator(`${TABSET2_PANE} [role="tab"]`, { hasText: helpTitle }))
        .toHaveCount(1, { timeout: TIMEOUTS.sessionRestart });
      await page.waitForFunction(() => window.rstudio?.ready === true, null, {
        timeout: TIMEOUTS.sessionRestart,
      });
    };
    await page.evaluate(() => window.rstudio!.prefs.uiLanguage.set('fr'));
    try {
      await reloadIn('Aide');

      await executeCommand(page, 'layoutZoomConsole');
      await expectHidden(page, TABSET2_PANE);
      await executeCommand(page, 'helpSearch');
      await expectExposed(page, TABSET2_PANE);
      await expect.poll(() => isCommandChecked(page, 'layoutZoomHelp')).toBe(true);
      expect(await isCommandChecked(page, 'layoutZoomConsole')).toBe(false);
    } finally {
      await resetLayoutZoom(page);
      await page.evaluate(() => window.rstudio!.prefs.uiLanguage.clear());
      await reloadIn('Help');
    }
  });

  // History is the zoomed pane's hidden partner here; showing it would end the
  // zoom without clearing its state (#18444).
  test('history() while Files is zoomed leaves the zoom on Files', async ({
    rstudioPage: page,
  }) => {
    await executeCommand(page, 'activateEnvironment');
    try {
      // Run history() once the zoom has started: running it at the prompt then
      // would focus the Console, which moves the zoom there.
      await executeInConsole(page, 'Sys.sleep(2); history()', { wait: false });
      await executeCommand(page, 'layoutZoomFiles');
      expect(await page.locator(HISTORY_TAB).getAttribute('aria-selected'),
             'history() ran before the zoom started').not.toBe('true');
      await expectHidden(page, TABSET1_PANE);

      await expect(page.locator(HISTORY_TAB)).toHaveAttribute('aria-selected', 'true',
                                                              { timeout: 10000 });
      for (let i = 0; i < 20; i++) {
        expect(await isCommandChecked(page, 'layoutZoomFiles'), `sample ${i}`).toBe(true);
        expect(await isUnreachable(page, TABSET1_PANE), `sample ${i}`).toBe(true);
        await page.waitForTimeout(50);
      }

      await executeCommand(page, 'layoutEndZoom');
      await expectExposed(page, TABSET1_PANE, TABSET2_PANE);
      await expect(page.locator(HISTORY_TAB)).toHaveAttribute('aria-selected', 'true');
    } finally {
      await waitForConsoleIdle(page, 10000);
      await resetLayoutZoom(page);
    }
  });

  // The zoom's widths are set at once but drawn over a 300ms animation, so a
  // pane shown in that window must still end it rather than end up behind it.
  test('Show Files while a column zoom animates ends the zoom', async ({ rstudioPage: page }) => {
    try {
      await page.evaluate(() => {
        const commands = window.rstudio!.commands;
        commands.layoutZoomLeftColumn();
        commands.activateFiles();
      });
      expect(await getSettledWidth(page, TABSET2_PANE)).toBeGreaterThanOrEqual(20);
      await expectExposed(page, TABSET2_PANE);
      expect(await isCommandChecked(page, 'layoutZoomLeftColumn')).toBe(false);
    } finally {
      await resetLayoutZoom(page);
    }
  });

  // A left-column zoom shuts the sidebar too, and ending it leaves the sidebar
  // shut (#19201). Three default columns in a 500px window are each narrower
  // than the sidebar's 175px minimum, so the reopen must aim for the minimum.
  test('Show Chat reopens a sidebar shut by a column zoom in a narrow window', async ({
    rstudioPage: page,
  }) => {
    const viewport = await page.evaluate(() => ({ width: innerWidth, height: innerHeight }));
    await setSidebarVisible(page, true);
    const sidebarSplitter = page.locator(SIDEBAR_COLUMN_SPLITTER);
    const box = await sidebarSplitter.boundingBox();
    if (!box)
      throw new Error('Sidebar column splitter has no bounding box; is it rendered?');
    try {
      await page.setViewportSize({ width: 500, height: viewport.height });
      await executeCommand(page, 'layoutZoomLeftColumn');
      await expectHidden(page, SIDEBAR_PANE, TABSET2_PANE);

      await executeCommand(page, 'activateChat');
      expect(await getSettledWidth(page, SIDEBAR_PANE)).toBeGreaterThanOrEqual(175);
      await expectExposed(page, SIDEBAR_PANE);
    } finally {
      await resetLayoutZoom(page);
      await page.setViewportSize(viewport);
      await waitForSplitterToSettle(page, SIDEBAR_COLUMN_SPLITTER);
      // Hiding the sidebar saves its width for the next show, so put the
      // splitter back first.
      await dragSplitterTo(page, SIDEBAR_COLUMN_SPLITTER, box.x + box.width / 2,
                           box.y + box.height / 2);
      await expect.poll(async () =>
        Math.abs(((await sidebarSplitter.boundingBox())?.x ?? 0) - box.x))
        .toBeLessThanOrEqual(2);
      await setSidebarVisible(page, false);
    }
  });

  test('a narrow column that is still open keeps its width', async ({ rstudioPage: page }) => {
    const windowWidth = await page.evaluate(() => window.innerWidth);
    await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, windowWidth - 60, splitterHome.y);
    await expect.poll(() => getOffsetWidth(page, TABSET2_PANE)).toBeGreaterThanOrEqual(20);
    const width = await getOffsetWidth(page, TABSET2_PANE);

    await executeCommand(page, 'activateFiles');
    // A resize would start at once and animate for 300ms, so sample well past it.
    for (let i = 0; i < 20; i++) {
      expect(await getOffsetWidth(page, TABSET2_PANE), `width, sample ${i}`).toBe(width);
      await page.waitForTimeout(50);
    }
  });
});

// A column that a zoom or a splitter drag collapses to nothing must leave the
// accessibility tree and the Tab order (#19181). Quadrants hidden by a maximize
// or a splitter snap are display:none; they are covered here so they stay so.

import { test, expect } from '@fixtures/rstudio.fixture';
import { documentCloseAllNoSave, executeCommand, resetLayoutZoom } from '@utils/commands';
import type { Locator, Page } from 'playwright';

const CONSOLE_PANE = '#rstudio_Console_pane';
const SOURCE_PANE = '#rstudio_Source_pane';
const TABSET1_PANE = '#rstudio_TabSet1_pane';
const TABSET2_PANE = '#rstudio_TabSet2_pane';
const SIDEBAR_PANE = '#rstudio_Sidebar_pane';
const MIDDLE_COLUMN_SPLITTER = '#rstudio_middle_column_splitter';
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

async function getOffsetWidth(page: Page, selector: string): Promise<number> {
  return page.locator(selector).evaluate(el => (el as HTMLElement).offsetWidth);
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

  test('dragging a column splitter closed hides the column', async ({ rstudioPage: page }) => {
    const splitter = page.locator(MIDDLE_COLUMN_SPLITTER);
    const box = await splitter.boundingBox();
    if (!box)
      throw new Error('Middle column splitter has no bounding box; is it rendered?');
    const windowWidth = await page.evaluate(() => window.innerWidth);

    // Mouse: drag the right column shut against the window edge, then back.
    await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, windowWidth - 1, box.y + box.height / 2);
    await expect.poll(() => getOffsetWidth(page, TABSET1_PANE)).toBeLessThan(20);
    await expectHidden(page, TABSET1_PANE, TABSET2_PANE);

    await dragSplitterTo(page, MIDDLE_COLUMN_SPLITTER, box.x, box.y + box.height / 2);
    await expectExposed(page, CONSOLE_PANE, TABSET1_PANE, TABSET2_PANE);

    // Keyboard: walk the splitter left until the Console column is shut.
    await splitter.focus();
    for (let i = 0; i < 100 && (await getOffsetWidth(page, CONSOLE_PANE)) > 0; i++)
      await page.keyboard.press('ArrowLeft');
    await expectHidden(page, CONSOLE_PANE);

    for (let i = 0; i < 100 && (await getOffsetWidth(page, CONSOLE_PANE)) < 100; i++)
      await page.keyboard.press('ArrowRight');
    await expectExposed(page, CONSOLE_PANE);
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

// The Viewer's "maximize" height request is the one live producer of
// EnsureHeightEvent.MAXIMIZED, which LogicalWindow now absorbs on a zoomed
// (EXCLUSIVE) window instead of firing at the quadrant state machine
// (#18448). Nothing else in the suite drives that event: the tab tests fire
// window state changes directly. These are regression guards on that change --
// the maximize must still reach the quadrant when nothing is zoomed, and must
// not corrupt a zoom when one is active. The sidebar-hosted variant lives in
// panes.test.ts, which owns the Pane Layout dialog helpers it needs.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand, isCommandChecked, resetLayoutZoom } from '@utils/commands';
import { sleep, TIMEOUTS } from '@utils/constants';
import { VIEWER_MAXIMIZE_R } from '@pages/viewer_pane.page';
import type { Page } from 'playwright';

const TABSET1_PANE = '#rstudio_TabSet1_pane';
const TABSET2_PANE = '#rstudio_TabSet2_pane';
const CONSOLE_PANE = '#rstudio_Console_pane';

async function getOffsetHeight(page: Page, selector: string): Promise<number> {
  return await page.locator(selector).evaluate(el => (el as HTMLElement).offsetHeight);
}
async function getOffsetWidth(page: Page, selector: string): Promise<number> {
  return await page.locator(selector).evaluate(el => (el as HTMLElement).offsetWidth);
}

test.describe('Viewer maximize height request', () => {
  // End any zoom and restore any maximized quadrant, whichever a failed test
  // left behind. The bridge reset covers both; the command-based unzoom in
  // panes.test.ts's resetUILayout only covers tracked zooms.
  test.afterEach(async ({ rstudioPage: page }) => {
    await resetLayoutZoom(page);
    await sleep(TIMEOUTS.layoutSettle);
  });

  test('maximizes the quadrant when nothing is zoomed', async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    const initialTabSet2Height = await getOffsetHeight(page, TABSET2_PANE);

    await consoleActions.executeInConsole(VIEWER_MAXIMIZE_R, { wait: true });

    // The quadrant maximizes vertically: TabSet2 grows well past its half.
    await expect.poll(
      async () => getOffsetHeight(page, TABSET2_PANE),
      { timeout: 10000 },
    ).toBeGreaterThan(initialTabSet2Height + 100);

    // A vertical maximize is not a zoom: no column collapses, no zoom tracked.
    expect(await getOffsetWidth(page, CONSOLE_PANE)).toBeGreaterThan(50);
    expect(await isCommandChecked(page, 'layoutZoomViewer')).toBe(false);
  });

  test('leaves consistent zoom state when a pane is zoomed', async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);

    await executeCommand(page, 'layoutZoomConsole');
    await expect.poll(
      async () => (await getOffsetWidth(page, TABSET1_PANE)) < 50,
      { timeout: 5000 },
    ).toBe(true);

    // The viewer raise transfers the zoom to TabSet2 (existing behavior); the
    // trailing MAXIMIZED request on the now-EXCLUSIVE window must be absorbed
    // rather than fired at the quadrant state machine.
    await consoleActions.executeInConsole(VIEWER_MAXIMIZE_R, { wait: true });

    // Wait on the effect, not the prompt: the transferred zoom is what proves
    // the viewer's raise landed, and the session wakes the poller before the
    // event is delivered.
    await expect.poll(
      async () => isCommandChecked(page, 'layoutZoomViewer'),
      { timeout: 10000 },
    ).toBe(true);
    await sleep(TIMEOUTS.layoutSettle);

    // Ending the zoom restores the full four-pane layout.
    await executeCommand(page, 'layoutEndZoom');
    await expect.poll(
      async () =>
        (await getOffsetWidth(page, CONSOLE_PANE)) > 50 &&
        (await getOffsetWidth(page, TABSET1_PANE)) > 50 &&
        (await getOffsetHeight(page, TABSET1_PANE)) > 50 &&
        (await getOffsetHeight(page, TABSET2_PANE)) > 50,
      { timeout: 10000 },
    ).toBe(true);
    expect(await isCommandChecked(page, 'layoutZoomConsole')).toBe(false);
    expect(await isCommandChecked(page, 'layoutZoomViewer')).toBe(false);
  });
});

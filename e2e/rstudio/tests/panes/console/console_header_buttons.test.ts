// The Console pane's buttons stay usable with a long working directory.
//
// When the Console is the only tab in its pane, its header shows the working
// directory and the Console's buttons; Find in Console was missing there
// (#19784). A long working directory drew over those buttons, and with other
// tabs it pushed the Console toolbar's buttons out of the pane (#19785). The
// path is now shortened with an ellipsis instead.

import type { Locator, Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsoleChecked } from '@pages/console_pane.page';
import { clearPref, executeCommand, resetLayoutZoom, setPref } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const CONSOLE_PANE = '#rstudio_Console_pane';
const FIND_BUTTON = "[id^='rstudio_tb_consolefind']";
const GO_TO_WD_BUTTON = "[id^='rstudio_tb_gotoworkingdir']";

// Ends the working directory so the path label can be found by its tooltip,
// which holds the full path.
const LEAF = 'pw-console-header-end';

// About 1200px of path, enough to overflow the pane at CI window sizes, yet
// under Windows' 248-character directory limit once appended to tempdir().
const LONG_DIR_R =
  `file.path(tempdir(), paste(rep("abcdefghijklmnopqrstuvwxyz0123456789", 4), ` +
  `collapse = "/"), "${LEAF}")`;

type Box = { x: number; y: number; width: number; height: number };

async function reloadAndWait(page: Page): Promise<void> {
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, {
    timeout: TIMEOUTS.sessionRestart,
    polling: 50,
  });
}

function visible(locator: Locator): Locator {
  return locator.filter({ visible: true });
}

// In tabbed mode the header's copy of the path is still laid out, but hidden
// from assistive technology; the toolbar's copy is the one shown.
function pathLabel(page: Page): Locator {
  return visible(
    page.locator(CONSOLE_PANE).locator(`[title$="${LEAF}/"]:not([aria-hidden="true"])`),
  );
}

async function boxOf(locator: Locator): Promise<Box> {
  await expect(locator).toHaveCount(1);
  const box = await locator.boundingBox();
  if (!box) throw new Error(`no bounding box for ${locator}`);
  return box;
}

// Each item must sit inside the pane, after the previous one, with no overlap.
async function expectInOrderWithinPane(page: Page, items: Locator[]): Promise<void> {
  const pane = await boxOf(page.locator(CONSOLE_PANE));
  const boxes = await Promise.all(items.map(boxOf));
  for (const [i, box] of boxes.entries()) {
    expect(box.x, `item ${i} starts inside the pane`).toBeGreaterThanOrEqual(pane.x);
    expect(box.x + box.width, `item ${i} ends inside the pane`)
      .toBeLessThanOrEqual(pane.x + pane.width + 0.5);
    if (i > 0) {
      const prev = boxes[i - 1];
      expect(box.x, `item ${i} starts after item ${i - 1} ends`)
        .toBeGreaterThanOrEqual(prev.x + prev.width - 0.5);
    }
  }
}

async function expectShortened(label: Locator): Promise<void> {
  const overflow = await label.evaluate((el) => ({
    shortened: el.scrollWidth > el.clientWidth,
    textOverflow: getComputedStyle(el).textOverflow,
  }));
  expect(overflow).toEqual({ shortened: true, textOverflow: 'ellipsis' });
}

async function consolePaneHeight(page: Page): Promise<number> {
  return (await boxOf(page.locator(CONSOLE_PANE))).height;
}

test.describe.serial('Console pane buttons with a long working directory', () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    await executeInConsoleChecked(
      page,
      `.pw_old_wd <- getwd(); .pw_long_wd <- ${LONG_DIR_R}; ` +
      `dir.create(.pw_long_wd, recursive = TRUE, showWarnings = FALSE); setwd(.pw_long_wd)`,
    );
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'show_terminal_tab');
    await clearPref(page, 'jobs_tab_visibility');
    await reloadAndWait(page);
    await executeInConsoleChecked(page, 'setwd(.pw_old_wd)');
    // Go to Working Directory left the Files pane in the directory removed below.
    await executeCommand(page, 'goToWorkingDir');
    await executeInConsoleChecked(
      page,
      'unlink(file.path(tempdir(), "abcdefghijklmnopqrstuvwxyz0123456789"), recursive = TRUE); ' +
      'rm(.pw_old_wd, .pw_long_wd)',
    );
  });

  test('with other tabs, the toolbar shortens the path and keeps its buttons in view', async ({
    rstudioPage: page,
  }) => {
    // The Terminal tab keeps the Console in tabbed mode.
    await setPref(page, 'show_terminal_tab', true);
    await reloadAndWait(page);
    await expect(page.locator('#rstudio_workbench_tab_terminal')).toBeVisible();
    await page.locator('#rstudio_workbench_tab_console').click();

    const pane = page.locator(CONSOLE_PANE);
    const label = pathLabel(page);
    await expect(label).toBeVisible();
    await expectShortened(label);
    await expectInOrderWithinPane(page, [
      label,
      visible(pane.locator(GO_TO_WD_BUTTON)),
      visible(pane.locator(FIND_BUTTON)),
      visible(pane.getByRole('button', { name: /^Clear console/ })),
    ]);
  });

  test('as the only tab, the header shortens the path and keeps every button in view', async ({
    rstudioPage: page,
  }) => {
    await setPref(page, 'show_terminal_tab', false);
    await setPref(page, 'jobs_tab_visibility', 'closed');
    await reloadAndWait(page);
    // With no other tabs, the Console's own tab is gone too.
    await expect(page.locator('#rstudio_workbench_tab_console')).toBeHidden();

    const pane = page.locator(CONSOLE_PANE);
    const label = pathLabel(page);
    await expect(label).toBeVisible();
    await expectShortened(label);

    // #19784: Find in Console is in the header alongside Clear.
    await expectInOrderWithinPane(page, [
      label,
      visible(pane.locator(GO_TO_WD_BUTTON)),
      visible(pane.locator(FIND_BUTTON)),
      visible(pane.getByRole('button', { name: /^Clear console/ })),
      visible(pane.getByRole('button', { name: 'Minimize Console' })),
      visible(pane.getByRole('button', { name: 'Maximize Console' })),
    ]);
  });

  test('double-clicking a header button does not maximize the pane', async ({
    rstudioPage: page,
  }) => {
    // Continues in console-only mode from the previous test.
    const pane = page.locator(CONSOLE_PANE);
    const goToWd = visible(pane.locator(GO_TO_WD_BUTTON));
    const label = pathLabel(page);
    await expect(goToWd).toBeVisible();

    try {
      // A double click on the header's path does maximize, so the height
      // below is a signal that reads differently when the guard is missing.
      const normal = await consolePaneHeight(page);
      await label.dblclick();
      await expect.poll(() => consolePaneHeight(page)).toBeGreaterThan(normal + 10);
      await resetLayoutZoom(page);
      await expect.poll(() => consolePaneHeight(page)).toBeLessThan(normal + 1);

      await goToWd.dblclick();
      // Maximizing is animated; sample past its end and fail on any growth.
      const deadline = Date.now() + 1500;
      while (Date.now() < deadline) {
        expect(await consolePaneHeight(page)).toBeLessThan(normal + 1);
        await page.waitForTimeout(100);
      }
    } finally {
      await resetLayoutZoom(page);
    }
  });
});

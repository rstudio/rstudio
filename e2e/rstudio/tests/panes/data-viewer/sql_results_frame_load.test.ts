// SQL Results waits for its grid viewer to load -- #19480.
//
// A preview (SQL `-- !preview`, or .rs.previewDataFrame) opens the SQL Results
// pane, whose grid viewer iframe loads grid_resource/gridviewer.html. The pane
// pushes the data into the frame from a 100 ms timer that is meant to wait for
// GridViewerFrame.isReady(). Pre-fix isReady() was always true -- the iframe
// has an about:blank contentWindow before its page loads -- so when the page
// took longer than 100 ms the timer called a setOption() that did not exist
// yet, threw, and the pane stayed empty.
//
// The test holds the frame's page load until that timer has fired, then lets
// it through and expects the previewed rows to appear.

import type { Page, Route } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';

// The SQL Results frame (GridViewerFrame, data_source=data). Match by regex
// per the Electron-CDP interception guidance (globs can miss query strings).
const GRID_PAGE = /\/grid_resource\/gridviewer\.html\?data_source=data/;
const RESULTS_FRAME = 'iframe[title="Data Output Pane"]';
const RESULTS_TAB = '#rstudio_workbench_tab_sql_results';
const TITLE = 'preview_19480';
const CELL = 'cell_19480';

// DataOutputPane.outputCompleted() schedules its first readiness check this
// long after it sets the pane title.
const OUTPUT_TIMER_MS = 100;

async function reloadAndWait(page: Page): Promise<void> {
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, {
    timeout: TIMEOUTS.sessionRestart,
    polling: 50,
  });
}

test.describe('SQL Results grid viewer load - #19480', () => {
  test('previewed rows appear when the grid viewer loads after the first check', async ({
    rstudioPage: page,
  }) => {
    // Hold every load of the results page until released. Installed before
    // the reload so a SQL Results tab restored at startup is held too.
    let release!: () => void;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    let held = 0;
    const holdGridPage = async (route: Route): Promise<void> => {
      held++;
      await gate;
      await route.continue();
    };
    await page.route(GRID_PAGE, holdGridPage);

    try {
      // A fresh UI: a results frame loaded by an earlier spec cannot race.
      await reloadAndWait(page);

      const consoleActions = new ConsolePaneActions(page);
      await consoleActions.executeInConsole(
        `.rs.previewDataFrame(data.frame(value = "${CELL}"), "${TITLE}")`,
        { wait: true },
      );

      // The pane title is set in the same task that schedules the first
      // readiness check, and showing the pane requests the frame's page.
      await expect(page.locator(RESULTS_TAB)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(page.getByText(TITLE, { exact: true })).toBeVisible();
      await expect.poll(() => held, { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);

      // Keep the page held past that first check: a known timer, with margin.
      await page.waitForTimeout(OUTPUT_TIMER_MS * 5);
      release();

      const results = page.frameLocator(RESULTS_FRAME);
      await expect(results.getByText(CELL)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    } finally {
      release();
      await page.unroute(GRID_PAGE, holdGridPage).catch(() => { /* already removed */ });
    }

    // Close the pane so later specs see the default console tabs.
    await page.locator(RESULTS_TAB).getByAltText('Close SQL Results tab').click();
    await expect(page.locator(RESULTS_TAB)).toBeHidden();
  });
});

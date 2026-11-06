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
// The tests hold the frame's page load while previews arrive, then let it
// through and check which rows the pane ends up showing.

import type { Page, Route } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';

// The SQL Results frame (GridViewerFrame, data_source=data). Match by regex
// per the Electron-CDP interception guidance (globs can miss query strings).
const GRID_PAGE = /\/grid_resource\/gridviewer\.html\?data_source=data/;
const RESULTS_FRAME = 'iframe[title="Data Output Pane"]';
const RESULTS_TAB = '#rstudio_workbench_tab_sql_results';

// DataOutputPane.outputCompleted() schedules its first readiness check this
// long after it sets the pane title, then re-checks at READY_POLL_MS.
const OUTPUT_TIMER_MS = 100;
const READY_POLL_MS = 200;

interface GridPageGate {
  release: () => void;
  held: () => number;
  dispose: () => Promise<void>;
}

// Hold every load of the results page until released.
async function holdGridPage(page: Page): Promise<GridPageGate> {
  let release!: () => void;
  const gate = new Promise<void>((resolve) => {
    release = resolve;
  });
  let held = 0;
  const handler = async (route: Route): Promise<void> => {
    held++;
    await gate;
    await route.continue();
  };
  await page.route(GRID_PAGE, handler);
  return {
    release,
    held: () => held,
    dispose: async () => {
      release();
      await page.unroute(GRID_PAGE, handler).catch(() => { /* already removed */ });
    },
  };
}

async function reloadAndWait(page: Page): Promise<void> {
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, {
    timeout: TIMEOUTS.sessionRestart,
    polling: 50,
  });
}

// The pane title is set in the same task that schedules the preview's first
// readiness check.
async function preview(page: Page, title: string, cell: string): Promise<void> {
  await new ConsolePaneActions(page).executeInConsole(
    `.rs.previewDataFrame(data.frame(value = "${cell}"), "${title}")`,
    { wait: true },
  );
  await expect(page.getByText(title, { exact: true })).toBeVisible({ timeout: TIMEOUTS.fileOpen });
}

async function closeResultsTab(page: Page): Promise<void> {
  await page.locator(RESULTS_TAB).getByAltText('Close SQL Results tab').click();
  await expect(page.locator(RESULTS_TAB)).toBeHidden();
}

test.describe('SQL Results grid viewer load - #19480', () => {
  test('previewed rows appear when the grid viewer loads after the first check', async ({
    rstudioPage: page,
  }) => {
    // Installed before the reload so a SQL Results tab restored at startup
    // is held too.
    const gate = await holdGridPage(page);
    try {
      // A fresh UI: a results frame loaded by an earlier spec cannot race.
      await reloadAndWait(page);

      await preview(page, 'preview_19480', 'cell_19480');
      await expect(page.locator(RESULTS_TAB)).toBeVisible();
      await expect.poll(() => gate.held(), { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);

      // Keep the page held past that first check: a known timer, with margin.
      await page.waitForTimeout(OUTPUT_TIMER_MS * 5);
      gate.release();

      const results = page.frameLocator(RESULTS_FRAME);
      await expect(results.getByText('cell_19480')).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    } finally {
      await gate.dispose();
    }

    await closeResultsTab(page);
  });

  test('a newer preview is not overwritten by one still waiting for the grid viewer', async ({
    rstudioPage: page,
  }) => {
    const gate = await holdGridPage(page);
    try {
      await reloadAndWait(page);

      // Both previews arrive while the page is held, so both wait on it.
      await preview(page, 'older_19480', 'older_cell_19480');
      await expect.poll(() => gate.held(), { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);
      await preview(page, 'newer_19480', 'newer_cell_19480');
      gate.release();

      const results = page.frameLocator(RESULTS_FRAME);
      await expect(results.getByText('newer_cell_19480')).toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // A stale preview still polling would land within one more readiness
      // check of the newer one: wait out a few, then the newer rows must
      // remain.
      await page.waitForTimeout(READY_POLL_MS * 5);
      await expect(results.getByText('newer_cell_19480')).toBeVisible();
      await expect(results.getByText('older_cell_19480')).toHaveCount(0);
    } finally {
      await gate.dispose();
    }

    await closeResultsTab(page);
  });
});

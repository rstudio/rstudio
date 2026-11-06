// SQL Results waits for its grid viewer to load -- #19480.
//
// A preview (SQL `-- !preview`, or .rs.previewDataFrame) opens the SQL Results
// pane, whose grid viewer iframe loads grid_resource/gridviewer.html, which
// defines setOption()/setData() in DataViewer.js. The pane pushes the data
// into the frame from a 100 ms timer that is meant to wait for
// GridViewerFrame.isReady(). Pre-fix isReady() was always true -- the iframe
// has a contentWindow before its page has loaded -- so when the page took
// longer than 100 ms the timer called a setOption() that did not exist yet,
// threw, and the pane stayed empty.
//
// The tests hold the frame's DataViewer.js while previews arrive, then let it
// through and check what the pane pushed into the frame.

import type { Frame, Page, Route } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';

// The SQL Results frame (GridViewerFrame, data_source=data) and its script.
// Match by regex per the Electron-CDP interception guidance (globs can miss
// query strings).
const GRID_PAGE = /\/grid_resource\/gridviewer\.html\?data_source=data/;
const GRID_SCRIPT = /\/grid_resource\/DataViewer\.js/;
const RESULTS_FRAME = 'iframe[title="Data Output Pane"]';
const RESULTS_TAB = '#rstudio_workbench_tab_sql_results';

// DataOutputPane.outputCompleted() schedules its first readiness check this
// long after it sets the pane title, then re-checks at READY_POLL_MS.
const OUTPUT_TIMER_MS = 100;
const READY_POLL_MS = 200;

interface GridScriptGate {
  release: () => void;
  // Results frames whose script is (or was) held.
  frames: Frame[];
  dispose: () => Promise<void>;
}

// Hold DataViewer.js in every SQL Results frame until released. Other grid
// viewers (e.g. Data Viewer tabs restored by a reload) load it unhindered.
async function holdGridScript(page: Page): Promise<GridScriptGate> {
  let release!: () => void;
  const gate = new Promise<void>((resolve) => {
    release = resolve;
  });
  const frames: Frame[] = [];
  const handler = async (route: Route): Promise<void> => {
    let frame: Frame | undefined;
    try {
      frame = route.request().frame();
    } catch {
      // not a frame request
    }
    if (frame && GRID_PAGE.test(frame.url())) {
      frames.push(frame);
      await gate;
    }
    await route.continue();
  };
  await page.route(GRID_SCRIPT, handler);
  return {
    release,
    frames,
    dispose: async () => {
      release();
      await page.unroute(GRID_SCRIPT, handler).catch(() => { /* already removed */ });
    },
  };
}

// Record every payload pushed into the frame, so a stale preview is caught
// even when the newer one ends up on top. Install it while the frame's
// script is held: it wraps whatever DataViewer.js then assigns to setData.
async function recordGridData(frame: Frame): Promise<void> {
  await frame.evaluate(() => {
    const w = window as unknown as { payloads19480: string[] };
    w.payloads19480 = [];
    let real: ((data: unknown) => unknown) | undefined;
    Object.defineProperty(window, 'setData', {
      configurable: true,
      set: (fn: (data: unknown) => unknown) => {
        real = fn;
      },
      get: () => real && ((data: unknown) => {
        w.payloads19480.push(JSON.stringify(data));
        return real!(data);
      }),
    });
  });
}

async function recordedGridData(frame: Frame): Promise<string[]> {
  return frame.evaluate(() => (window as unknown as { payloads19480: string[] }).payloads19480);
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
    const gate = await holdGridScript(page);
    try {
      // A fresh UI: a results frame loaded by an earlier spec cannot race.
      await reloadAndWait(page);

      await preview(page, 'preview_19480', 'cell_19480');
      await expect(page.locator(RESULTS_TAB)).toBeVisible();
      await expect.poll(() => gate.frames.length, { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);

      // Keep the script held past that first check: a known timer, with margin.
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
    const gate = await holdGridScript(page);
    try {
      await reloadAndWait(page);

      // Both previews arrive while the script is held, so both wait on it.
      await preview(page, 'older_19480', 'older_cell_19480');
      await expect.poll(() => gate.frames.length, { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);
      const frame = gate.frames[gate.frames.length - 1];
      await recordGridData(frame);
      await preview(page, 'newer_19480', 'newer_cell_19480');
      gate.release();

      const results = page.frameLocator(RESULTS_FRAME);
      await expect(results.getByText('newer_cell_19480')).toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // A stale preview still polling would be pushed within one readiness
      // check of the script loading, whichever order the two land in: wait
      // out a few, then only the newer preview may have been pushed.
      await page.waitForTimeout(READY_POLL_MS * 5);
      const payloads = await recordedGridData(frame);
      expect(payloads).toHaveLength(1);
      expect(payloads[0]).toContain('newer_cell_19480');
      await expect(results.getByText('newer_cell_19480')).toBeVisible();
    } finally {
      await gate.dispose();
    }

    await closeResultsTab(page);
  });
});

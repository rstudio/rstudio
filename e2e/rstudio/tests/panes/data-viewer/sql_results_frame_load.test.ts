// SQL Results waits for its grid viewer to load -- #19480.
//
// A preview (SQL `-- !preview`, or .rs.previewDataFrame) opens the SQL Results
// pane, whose grid viewer iframe loads grid_resource/gridviewer.html, which
// defines setOption()/setData() in DataViewer.js. The pane pushes the data
// into the frame as soon as the preview arrives; GridViewerFrame holds the
// pushes until the page has loaded and replays them from the frame's load
// event. Pre-fix the pane pushed from a 100 ms timer whose readiness check
// was always true -- the iframe has a contentWindow before its page has
// loaded -- so when the page took longer than 100 ms it called a setOption()
// that did not exist yet, threw, and the pane stayed empty.
//
// The frame also reloads (and so empties) whenever the console tab panel is
// rebuilt, which happens when any console tab is shown or hidden; the replay
// covers that too.
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
const MARKERS_TAB = '#rstudio_workbench_tab_markers';

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

// The SQL Results frame's current document, if it is showing gridviewer.html.
function resultsFrame(page: Page): Frame | undefined {
  return page.frames().find((frame) => GRID_PAGE.test(frame.url()));
}

async function reloadAndWait(page: Page): Promise<void> {
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, {
    timeout: TIMEOUTS.sessionRestart,
    polling: 50,
  });
}

// The pane title is set right before the preview is pushed into the frame,
// in the same call, so once it shows the push has been made.
async function preview(page: Page, title: string, cell: string): Promise<void> {
  await new ConsolePaneActions(page).executeInConsole(
    `.rs.previewDataFrame(data.frame(value = "${cell}"), "${title}")`,
    { wait: true },
  );
  await expect(page.getByText(title, { exact: true })).toBeVisible({ timeout: TIMEOUTS.fileOpen });
}

// Best effort, for cleanup: a failed test may not have opened the tab.
async function closeTabIfOpen(page: Page, tab: string, closeAlt: string): Promise<void> {
  const locator = page.locator(tab);
  if (!(await locator.isVisible())) {
    return;
  }
  await locator.getByAltText(closeAlt).click();
  await expect(locator).toBeHidden();
}

async function closeResultsTab(page: Page): Promise<void> {
  await closeTabIfOpen(page, RESULTS_TAB, 'Close SQL Results tab');
}

// Closing the tab also clears the markers on the server, so nothing is
// restored for later specs.
async function closeMarkersTab(page: Page): Promise<void> {
  await closeTabIfOpen(page, MARKERS_TAB, 'Close Markers tab');
}

test.describe('SQL Results grid viewer load - #19480', () => {
  test('previewed rows appear when the grid viewer loads after they are pushed', async ({
    rstudioPage: page,
  }) => {
    // Installed before the reload so a SQL Results tab restored at startup
    // is held too.
    const gate = await holdGridScript(page);
    try {
      // A fresh UI: a results frame loaded by an earlier spec cannot race.
      await reloadAndWait(page);

      // The preview is pushed while the script is still held, so the frame
      // has to hold it until the page loads.
      await preview(page, 'preview_19480', 'cell_19480');
      await expect(page.locator(RESULTS_TAB)).toBeVisible();
      await expect.poll(() => gate.frames.length, { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);
      gate.release();

      const results = page.frameLocator(RESULTS_FRAME);
      await expect(results.getByText('cell_19480')).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    } finally {
      await gate.dispose();
      await closeResultsTab(page);
    }
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

      // Everything held is replayed from the frame's load event, before the
      // grid is drawn, so by now only the newer preview may have been pushed.
      const payloads = await recordedGridData(frame);
      expect(payloads).toHaveLength(1);
      expect(payloads[0]).toContain('newer_cell_19480');
    } finally {
      await gate.dispose();
      await closeResultsTab(page);
    }
  });

  test('previewed rows are pushed again when the grid viewer reloads', async ({
    rstudioPage: page,
  }) => {
    try {
      await reloadAndWait(page);

      await preview(page, 'reload_19480', 'reload_cell_19480');
      const results = page.frameLocator(RESULTS_FRAME);
      await expect(results.getByText('reload_cell_19480')).toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // Mark this document, so a reload can be told apart from no reload.
      const shown = resultsFrame(page);
      expect(shown).toBeDefined();
      await shown!.evaluate(() => {
        (window as unknown as { marked19480: boolean }).marked19480 = true;
      });

      // Showing another console tab rebuilds the console tab panel, which
      // re-inserts the SQL Results iframe and so reloads it empty.
      await new ConsolePaneActions(page).executeInConsole(
        [
          'local({',
          '  f <- tempfile(fileext = ".R"); writeLines("x", f)',
          '  m <- data.frame(type = "info", file = f, line = 1, column = 1,',
          '                  message = "marker_19480", stringsAsFactors = FALSE)',
          '  .rs.api.sourceMarkers("markers_19480", m)',
          '})',
        ].join('\n'),
        { wait: true },
      );
      await expect(page.locator(MARKERS_TAB)).toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // A new document: the mark is gone.
      await expect.poll(async () => {
        const frame = resultsFrame(page);
        if (!frame) {
          return 'detached';
        }
        return frame
          .evaluate(() => (window as unknown as { marked19480?: boolean }).marked19480 === true)
          .then((marked) => (marked ? 'marked' : 'reloaded'))
          .catch(() => 'navigating');
      }, { timeout: TIMEOUTS.fileOpen }).toBe('reloaded');

      // Showing the markers also selected their tab; the reloaded grid is
      // only visible once SQL Results is in front again.
      await page.locator(RESULTS_TAB).click();
      await expect(results.getByText('reload_cell_19480')).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    } finally {
      await closeMarkersTab(page);
      await closeResultsTab(page);
    }
  });
});

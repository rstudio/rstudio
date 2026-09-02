// Data Viewer column visibility (#17787).
//
// Columns can be hidden from the grid through the summary sidebar: an eye
// icon on each entry toggles one column, and an eye in the panel header hides
// or shows them all. Hiding is a client-side layout change -- the column
// leaves the render order and the fetched window is measured in visible
// columns -- so these tests check the grid, the sidebar, the status bar, the
// keyboard path, the wide-frame window math, and persistence.
//
// Grid headers are located by their title ("column N: <type>"), which carries
// the ABSOLUTE column index. Their data-col-idx is the position in the fetched
// column set, which shifts once a hidden column drops out of a fetch.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { DataViewerPane } from '@pages/data_viewer.page';
import { resetSourcePaneState } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const VIEWER_FRAME = '#rstudio_data_viewer_frame';

// Waits for the data viewer iframe to render a column header (any column: on
// a wide frame column 1 may be outside the rendered window).
async function waitForViewer(dataViewer: DataViewerPane): Promise<void> {
  await expect(dataViewer.frame.locator('th[data-col-idx][title]').first())
    .toBeVisible({ timeout: TIMEOUTS.fileOpen });
}

// Absolute index of every rendered column header, left to right, as strings
// ('0' for the rownames header). The frozen pane's #pinned_cols precedes
// #data_cols in the DOM; spacer cells carry no title and are skipped.
async function colOrder(dataViewer: DataViewerPane): Promise<string[]> {
  return dataViewer.frame.locator('#pinned_cols th[title], #data_cols th[title]')
    .evaluateAll((ths) =>
      (ths as HTMLElement[]).map((th) => {
        const m = /^column (\d+):/.exec(th.getAttribute('title') ?? '');
        return m ? m[1] : '0';
      }),
    );
}

// Calls one of the iframe's window-level hooks (refreshData / refreshAndReset).
async function callViewerHook(page: Page, name: string): Promise<void> {
  await page.evaluate(([sel, hook]) => {
    const f = document.querySelector(sel) as HTMLIFrameElement | null;
    const w = f?.contentWindow as unknown as Record<string, (() => void) | undefined> | undefined;
    const fn = w?.[hook];
    if (!fn) throw new Error(`${hook}() not available on data viewer iframe`);
    fn();
  }, [VIEWER_FRAME, name] as const);
}

// The sidebar list is virtualized: scroll it so the entry for absolute column
// `abs` is built (entries are a fixed height; the rowname is not listed, so
// abs N sits at index N-1).
async function scrollSidebarToCol(dataViewer: DataViewerPane, abs: number): Promise<void> {
  await dataViewer.viewport.evaluate((vp, a) => {
    const doc = vp.ownerDocument;
    const content = doc.getElementById('sidebarContent');
    if (!content) return;
    const h = parseInt(
      getComputedStyle(doc.documentElement).getPropertyValue('--sidebar-entry-height'),
      10) || 78;
    content.scrollTop = (a - 1) * h;
  }, abs);
}

test.describe('Data Viewer column visibility', () => {
  let consoleActions: ConsolePaneActions;
  let sourcePane: SourcePane;
  let dataViewer: DataViewerPane;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourcePane = new SourcePane(page);
    dataViewer = new DataViewerPane(page);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // See data_viewer.test.ts: go through resetSourcePaneState so the Source
    // pane never reaches zero tabs (#17738).
    await resetSourcePaneState(page);
    await expect(sourcePane.selectedTab).toContainText('Untitled', { timeout: 5000 });
    // Drop the per-test frame copies (viewMtcarsCopy) so they don't pile up.
    await consoleActions.executeInConsole(
      'rm(list = ls(envir = .GlobalEnv, all.names = TRUE, pattern = "^[.]rs[.]colvis_"), envir = .GlobalEnv)',
    );
  });

  // View() a copy of mtcars named after the current test. Viewer state (pins,
  // sorts, hidden columns) is persisted in localStorage per object name, so a
  // unique object per test keeps one test's state from reaching another
  // regardless of how the previous viewer tab was torn down.
  async function viewMtcarsCopy(): Promise<void> {
    const name = '.rs.colvis_' + test.info().title.replace(/[^A-Za-z0-9]+/g, '_');
    await consoleActions.executeInConsole(`{ ${name} <- mtcars; View(${name}) }`);
  }

  test('sidebar eye icon hides a column from the grid and shows it again', async () => {
    await viewMtcarsCopy();
    await waitForViewer(dataViewer);
    expect((await colOrder(dataViewer)).slice(0, 5)).toEqual(['0', '1', '2', '3', '4']);

    // mtcars row 1 (Mazda RX4): disp 160 (rendered "160.0" -- the column has
    // fractional values), hp 110. Cells carry their display position, so with
    // disp (3) hidden the third data cell is hp.
    const row0 = dataViewer.frame.locator('#gridBody tr[data-row="0"]');
    await expect(row0.locator('td[data-col-pos="3"]')).toHaveText('160.0');

    const entry3 = dataViewer.frame.locator('.sidebar-col[data-col-idx="3"]');
    const eye3 = entry3.locator('.sidebar-eye-icon');
    await expect(eye3).toHaveAttribute('aria-pressed', 'false');
    await eye3.click();

    // The header is gone, the remaining columns close ranks, and the row
    // cells follow the new display order.
    await expect(dataViewer.columnHeader(3)).toHaveCount(0);
    expect((await colOrder(dataViewer)).slice(0, 5)).toEqual(['0', '1', '2', '4', '5']);
    await expect(row0.locator('td[data-col-pos="3"]')).toHaveText(/^110(\.0)?$/);

    // The sidebar entry stays listed, marked hidden, and the counts say so.
    await expect(entry3).toHaveClass(/\bcol-hidden\b/);
    await expect(eye3).toHaveAttribute('aria-pressed', 'true');
    await expect(dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-label'))
      .toContainText('(1 hidden)');
    await expect(dataViewer.gridInfo).toContainText('(1 hidden)');

    // Show it again from the same icon.
    await eye3.click();
    await expect(dataViewer.columnHeader(3)).toBeVisible();
    expect((await colOrder(dataViewer)).slice(0, 5)).toEqual(['0', '1', '2', '3', '4']);
    await expect(row0.locator('td[data-col-pos="3"]')).toHaveText('160.0');
    await expect(entry3).not.toHaveClass(/\bcol-hidden\b/);
    await expect(dataViewer.gridInfo).not.toContainText('hidden');
  });

  test('header eye hides every column and the in-grid hint shows them all', async () => {
    await viewMtcarsCopy();
    await waitForViewer(dataViewer);

    const headerEye = dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-eye');
    const hint = dataViewer.frame.locator('#allColumnsHiddenHint');
    await expect(headerEye).toHaveAttribute('aria-label', 'Hide all columns');
    await expect(hint).toBeHidden();

    await headerEye.click();

    // Only the frozen rownames column remains; the scrollable pane is empty
    // and the hint (with its way back) is up.
    await expect(dataViewer.frame.locator('#data_cols th')).toHaveCount(0);
    await expect(dataViewer.frame.locator('#pinned_cols th[title="row names"]')).toBeVisible();
    await expect(hint).toBeVisible();
    await expect(headerEye).toHaveAttribute('aria-label', 'Show all columns');
    await expect(dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-label'))
      .toContainText('(11 hidden)');
    await expect(dataViewer.gridInfo).toContainText('11 total columns (11 hidden)');

    // Every entry is marked hidden, but stays listed.
    await expect(dataViewer.frame.locator('.sidebar-col[data-col-idx="1"]'))
      .toHaveClass(/\bcol-hidden\b/);

    // The hint's button restores everything. (Only the headers that fit the
    // rendered column window exist, so check the leading columns, not a count.)
    await hint.locator('#allColumnsHiddenShow').click();
    await expect(dataViewer.columnHeader(1)).toBeVisible();
    expect((await colOrder(dataViewer)).slice(0, 4)).toEqual(['0', '1', '2', '3']);
    await expect(hint).toBeHidden();
    await expect(headerEye).toHaveAttribute('aria-label', 'Hide all columns');
    await expect(dataViewer.gridInfo).not.toContainText('hidden');

    // With some (not all) columns hidden the header eye reads as show-all and
    // brings back just the hidden ones.
    await dataViewer.frame.locator('.sidebar-col[data-col-idx="2"] .sidebar-eye-icon').click();
    await expect(dataViewer.columnHeader(2)).toHaveCount(0);
    await expect(headerEye).toHaveAttribute('aria-label', 'Show all columns');
    await headerEye.click();
    await expect(dataViewer.columnHeader(2)).toBeVisible();
    expect((await colOrder(dataViewer)).slice(0, 4)).toEqual(['0', '1', '2', '3']);
    await expect(dataViewer.gridInfo).not.toContainText('hidden');
  });

  test('a hidden column keeps its sort, and clicking its entry shows it again', async () => {
    await viewMtcarsCopy();
    await waitForViewer(dataViewer);

    // Sort mpg (1) descending from the sidebar: Toyota Corolla (33.9 mpg,
    // 4 cyl) leads; unsorted, Mazda RX4 (6 cyl) does.
    const sortIcon1 = dataViewer.frame.locator('.sidebar-col[data-col-idx="1"] .sidebar-sort-icon');
    await sortIcon1.click();
    await sortIcon1.click();
    await expect(sortIcon1).toHaveClass(/sorting_desc/);
    const row0 = dataViewer.frame.locator('#gridBody tr[data-row="0"]');
    await expect(row0.locator('td[data-col-pos="1"]')).toHaveText('33.9');

    // Hide the sort column: the rows stay in mpg order (cyl is now the first
    // data cell), and the status bar still reports the sort.
    const entry1 = dataViewer.frame.locator('.sidebar-col[data-col-idx="1"]');
    await entry1.locator('.sidebar-eye-icon').click();
    await expect(dataViewer.columnHeader(1)).toHaveCount(0);
    await expect(row0.locator('td[data-col-pos="1"]')).toHaveText('4');
    await expect(dataViewer.sortStatus).toContainText('mpg');
    await expect(sortIcon1).toHaveClass(/sorting_desc/);

    // Activating the entry itself (not the eye) is a request to see the
    // column: it comes back, still sorted.
    await entry1.locator('.sidebar-col-name').click();
    await expect(dataViewer.columnHeader(1)).toBeVisible();
    await expect(entry1).not.toHaveClass(/\bcol-hidden\b/);
    await expect(row0.locator('td[data-col-pos="1"]')).toHaveText('33.9');
    await expect(dataViewer.columnHeader(1)).toHaveClass(/sorting_desc/);
  });

  // A hidden sort column drops out of the fetch on the next refresh (or window
  // slide), so the "Sorted by" status must name it from full-frame metadata
  // rather than from the fetched columns -- and its clear button must stay.
  test('a hidden sort column keeps its status and clear button across a refresh', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '{ .rs.colvis_sort_df <- mtcars; View(.rs.colvis_sort_df) }',
    );
    try {
      await waitForViewer(dataViewer);
      const sortIcon1 = dataViewer.frame.locator('.sidebar-col[data-col-idx="1"] .sidebar-sort-icon');
      await sortIcon1.click();
      await sortIcon1.click();
      await expect(sortIcon1).toHaveClass(/sorting_desc/);
      const row0 = dataViewer.frame.locator('#gridBody tr[data-row="0"]');
      await expect(row0.locator('td[data-col-pos="1"]')).toHaveText('33.9');

      await dataViewer.frame.locator('.sidebar-col[data-col-idx="1"] .sidebar-eye-icon').click();
      await expect(dataViewer.columnHeader(1)).toHaveCount(0);

      // After the refresh mpg is no longer fetched at all, yet the rows are
      // still in mpg order (Toyota Corolla's 4 cylinders lead) and the status
      // bar still says so.
      await callViewerHook(page, 'refreshData');
      await expect(dataViewer.columnHeader(2)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.columnHeader(1)).toHaveCount(0);
      await expect(row0.locator('td[data-col-pos="1"]')).toHaveText('4', { timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.sortStatus).toContainText('mpg');
      await expect(dataViewer.clearSortButton).toBeVisible();

      // Clearing the sort from the status bar restores frame order (Mazda RX4,
      // 6 cylinders, leads) with the column still hidden.
      await dataViewer.clearSortButton.click();
      await expect(row0.locator('td[data-col-pos="1"]')).toHaveText('6', { timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.sortStatus).toBeHidden();
      await expect(dataViewer.columnHeader(1)).toHaveCount(0);
    } finally {
      await consoleActions.executeInConsole('rm(".rs.colvis_sort_df", envir = .GlobalEnv)');
    }
  });

  test('a pinned column can be hidden and returns to the pinned pane', async () => {
    await viewMtcarsCopy();
    await waitForViewer(dataViewer);

    const entry3 = dataViewer.frame.locator('.sidebar-col[data-col-idx="3"]');
    const pinnedHeader3 = dataViewer.frame.locator('#pinned_cols th[title^="column 3:"]');
    await entry3.locator('.sidebar-pin-icon').click();
    await expect(pinnedHeader3).toBeVisible();

    await entry3.locator('.sidebar-eye-icon').click();
    await expect(dataViewer.columnHeader(3)).toHaveCount(0);
    expect((await colOrder(dataViewer)).slice(0, 3)).toEqual(['0', '1', '2']);
    // The pin survives hiding: the entry's pin icon still reads pinned.
    await expect(entry3.locator('.sidebar-pin-icon')).toHaveClass(/\bpinned\b/);

    await entry3.locator('.sidebar-eye-icon').click();
    await expect(pinnedHeader3).toBeVisible();
    expect((await colOrder(dataViewer)).slice(0, 3)).toEqual(['0', '3', '1']);
  });

  test('H hides the column under the keyboard cursor and moves the cursor on', async ({ rstudioPage: page }) => {
    await viewMtcarsCopy();
    await waitForViewer(dataViewer);

    // Clicking a header makes it the active header and focuses the grid.
    await dataViewer.columnHeader(2).click();
    await expect(dataViewer.columnHeader(2)).toHaveClass(/activeHeader/);

    await page.keyboard.press('h');

    // cyl (2) is gone; the cursor lands on the column that took its slot.
    await expect(dataViewer.columnHeader(2)).toHaveCount(0);
    await expect(dataViewer.columnHeader(3)).toHaveClass(/activeHeader/);
    await expect(dataViewer.frame.locator('.sidebar-col[data-col-idx="2"]'))
      .toHaveClass(/\bcol-hidden\b/);

    // Show all from the header eye.
    await dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-eye').click();
    await expect(dataViewer.columnHeader(2)).toBeVisible();
  });

  // The fetched column window is measured in visible columns, so hiding
  // almost everything on a frame wider than the window must still fetch and
  // lay out the few columns that remain -- with no blank span standing in for
  // an unfetched column, wherever those columns sit in the frame.
  test('wide frame: the window follows the visible columns', async () => {
    // 300 columns: wider than the fetched window. matrix(1:3000) is
    // column-major with 10 rows, so column k's first row holds (k-1)*10 + 1.
    await consoleActions.executeInConsole(
      '{ .rs.colvis_wide_df <- as.data.frame(matrix(1:3000, nrow = 10, ncol = 300)); View(.rs.colvis_wide_df) }',
    );
    try {
      await waitForViewer(dataViewer);
      await expect(dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-label'))
        .toHaveText('300 columns', { timeout: TIMEOUTS.fileOpen });

      // Hide column 5, then jump to it with Go to column: the jump shows it.
      const entry5 = dataViewer.frame.locator('.sidebar-col[data-col-idx="5"]');
      await entry5.locator('.sidebar-eye-icon').click();
      await expect(dataViewer.columnHeader(5)).toHaveCount(0);
      await expect(entry5).toHaveClass(/\bcol-hidden\b/);
      await dataViewer.goToColumn(5);
      await expect(dataViewer.columnHeader(5)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(entry5).not.toHaveClass(/\bcol-hidden\b/);

      // Hide everything, then show columns 1, 260 and 300 from their entries
      // (scrolling the virtualized list to build the far ones).
      const headerEye = dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-eye');
      await headerEye.click();
      await expect(dataViewer.frame.locator('#data_cols th')).toHaveCount(0);
      await expect(dataViewer.frame.locator('#allColumnsHiddenHint')).toBeVisible();
      await expect(dataViewer.gridInfo).toContainText('(300 hidden)');

      for (const abs of [1, 260, 300]) {
        await scrollSidebarToCol(dataViewer, abs);
        const entry = dataViewer.frame.locator(`.sidebar-col[data-col-idx="${abs}"]`);
        await expect(entry).toHaveCount(1, { timeout: TIMEOUTS.fileOpen });
        await entry.locator('.sidebar-eye-icon').click();
        await expect(entry).not.toHaveClass(/\bcol-hidden\b/);
      }

      // All three are fetched and rendered, in frame order, with real data --
      // and nothing else: no spacer cell stands in for an unfetched column.
      for (const abs of [1, 260, 300]) {
        await expect(dataViewer.columnHeader(abs)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      }
      await expect(dataViewer.frame.locator('#data_cols th[data-col-idx]')).toHaveCount(3);
      await expect(dataViewer.frame.locator('#data_cols th.col-spacer')).toHaveCount(0);
      const row0 = dataViewer.frame.locator('#gridBody tr[data-row="0"]');
      await expect(row0.locator('td[data-col-pos="1"]')).toHaveText('1', { timeout: TIMEOUTS.fileOpen });
      await expect(row0.locator('td[data-col-pos="2"]')).toHaveText('2591');
      await expect(row0.locator('td[data-col-pos="3"]')).toHaveText('2991');
      await expect(dataViewer.gridInfo).toContainText('300 total columns (297 hidden)');
      await expect(dataViewer.frame.locator('#allColumnsHiddenHint')).toBeHidden();

      // The three visible columns fit the viewport, yet the toolbar's Go to
      // column box must stay available while anything is hidden: it is a way
      // back to a hidden column. Jumping to column 150 shows it in place.
      await dataViewer.goToColumn(150);
      await expect(dataViewer.columnHeader(150)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.frame.locator('#data_cols th[data-col-idx]')).toHaveCount(4);
      await expect(row0.locator('td[data-col-pos="2"]')).toHaveText('1491', { timeout: TIMEOUTS.fileOpen });
      await expect(row0.locator('td[data-col-pos="3"]')).toHaveText('2591');
      await expect(dataViewer.gridInfo).toContainText('(296 hidden)');

      // Show all: the frame is wide again and column 1 leads.
      await headerEye.click();
      await expect(dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-label'))
        .toHaveText('300 columns', { timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.gridInfo).not.toContainText('hidden');
    } finally {
      await consoleActions.executeInConsole('rm(".rs.colvis_wide_df", envir = .GlobalEnv)');
    }
  });

  // The toolbar's Go to column box normally appears only when the columns
  // overflow the viewport, but it doubles as a way back to a hidden column, so
  // it must also appear while anything is hidden -- including after hide-all
  // on a frame that never overflowed.
  test('Go to column stays available while columns are hidden on a narrow frame', async () => {
    await consoleActions.executeInConsole(
      '{ .rs.colvis_narrow_df <- data.frame(a = 1:3, b = 4:6, c = 7:9); View(.rs.colvis_narrow_df) }',
    );
    try {
      await waitForViewer(dataViewer);
      await expect(dataViewer.columnHeader(3)).toBeVisible();
      await expect(dataViewer.gotoColumnInput).toBeHidden();

      await dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-eye').click();
      await expect(dataViewer.frame.locator('#data_cols th')).toHaveCount(0);
      await expect(dataViewer.gotoColumnInput).toBeVisible();

      // Jumping to a hidden column through the box shows just that column.
      await dataViewer.goToColumn('b');
      await expect(dataViewer.columnHeader(2)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.columnHeader(1)).toHaveCount(0);
      await expect(dataViewer.gridInfo).toContainText('(2 hidden)');

      // Once nothing is hidden the box goes away again, since the frame fits.
      await dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-eye').click();
      await expect(dataViewer.gridInfo).not.toContainText('hidden');
      await expect(dataViewer.gotoColumnInput).toBeHidden();
    } finally {
      await consoleActions.executeInConsole('rm(".rs.colvis_narrow_df", envir = .GlobalEnv)');
    }
  });

  // Hidden columns are saved with the frame's column fingerprint (like pins),
  // so a structural change invalidates them -- and the refresh that follows
  // must fetch the whole new frame, not a window bounded by the old width.
  test('adding a column discards the hidden set and fetches every column', async () => {
    await consoleActions.executeInConsole(
      '{ .rs.colvis_grow_df <- as.data.frame(matrix(0L, nrow = 5, ncol = 4)); View(.rs.colvis_grow_df) }',
    );
    try {
      await waitForViewer(dataViewer);
      await dataViewer.frame.locator('.sidebar-col[data-col-idx="2"] .sidebar-eye-icon').click();
      await expect(dataViewer.columnHeader(2)).toHaveCount(0);
      await expect(dataViewer.gridInfo).toContainText('(1 hidden)');

      await consoleActions.executeInConsole('.rs.colvis_grow_df$added <- 1L');

      // The refresh reports and renders the new fifth column, and the column
      // hidden against the old frame is back.
      await expect(dataViewer.gridInfo).toContainText('5 total columns', { timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.columnHeader(5)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.columnHeader(2)).toBeVisible();
      expect(await colOrder(dataViewer)).toEqual(['0', '1', '2', '3', '4', '5']);
      await expect(dataViewer.gridInfo).not.toContainText('hidden');
      await expect(dataViewer.frame.locator('.sidebar-col[data-col-idx="2"]'))
        .not.toHaveClass(/\bcol-hidden\b/);
    } finally {
      await consoleActions.executeInConsole('rm(".rs.colvis_grow_df", envir = .GlobalEnv)');
    }
  });

  test('hidden columns survive a refresh and are cleared by Reset View', async ({ rstudioPage: page }) => {
    // A uniquely-named object so this viewer's localStorage entry is its own.
    await consoleActions.executeInConsole(
      '{ .rs.colvis_persist_df <- mtcars; View(.rs.colvis_persist_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      await dataViewer.frame.locator('.sidebar-col[data-col-idx="3"] .sidebar-eye-icon').click();
      await expect(dataViewer.columnHeader(3)).toHaveCount(0);

      // A data refresh rebuilds the grid from saved state: the column stays
      // hidden -- now left out of the fetch altogether -- in the grid and in
      // the rebuilt sidebar.
      await callViewerHook(page, 'refreshData');
      await expect(dataViewer.columnHeader(4)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.columnHeader(3)).toHaveCount(0);
      expect((await colOrder(dataViewer)).slice(0, 4)).toEqual(['0', '1', '2', '4']);
      await expect(dataViewer.frame.locator('.sidebar-col[data-col-idx="3"]'))
        .toHaveClass(/\bcol-hidden\b/);
      await expect(dataViewer.gridInfo).toContainText('(1 hidden)');

      // Reset View discards the saved state, hidden columns included.
      await callViewerHook(page, 'refreshAndReset');
      await expect(dataViewer.columnHeader(3)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.frame.locator('.sidebar-col[data-col-idx="3"]'))
        .not.toHaveClass(/\bcol-hidden\b/);
      await expect(dataViewer.gridInfo).not.toContainText('hidden');
    } finally {
      await consoleActions.executeInConsole('rm(".rs.colvis_persist_df", envir = .GlobalEnv)');
    }
  });
});

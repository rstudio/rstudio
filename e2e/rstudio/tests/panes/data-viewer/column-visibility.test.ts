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
    // Drop the per-test frames (viewAs) so they don't pile up.
    await consoleActions.executeInConsole(
      'rm(list = ls(envir = .GlobalEnv, all.names = TRUE, pattern = "^[.]rs[.]colvis_"), envir = .GlobalEnv)',
    );
  });

  // Assigns the R expression `expr` to an object named after the current test
  // (and retry attempt) and View()s it, returning the name. Viewer state --
  // pins, sorts, filters, hidden columns -- is persisted in localStorage per
  // object name, so a name unique to this attempt keeps one test's state from
  // reaching another regardless of how the previous viewer tab was torn down.
  async function viewAs(expr: string): Promise<string> {
    const info = test.info();
    const name = '.rs.colvis_' + info.title.replace(/[^A-Za-z0-9]+/g, '_') + '_r' + info.retry;
    await consoleActions.executeInConsole(`{ ${name} <- ${expr}; View(${name}) }`);
    return name;
  }

  test('sidebar eye icon hides a column from the grid and shows it again', async () => {
    await viewAs('mtcars');
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
    await viewAs('mtcars');
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
    await viewAs('mtcars');
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
    await viewAs('mtcars');
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
  });

  // An open filter editor holds the header rebuild back (autoSizeColumns
  // defers it so the editor isn't torn down mid-edit); hide/show closes the
  // editor first, otherwise the rows would be rebuilt against a column order
  // the headers don't show yet.
  test('hiding a column while a filter editor is open closes it and keeps headers and rows in step', async ({ rstudioPage: page }) => {
    await viewAs('data.frame(x = 1:20, y = 21:40, z = 41:60)');
    await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    // The info bar renders once the first row batch lands, which is also when
    // the post-load auto-size rebuilds the headers; touching the filter row
    // before that races the rebuild (see column-filters.test.ts).
    await expect(dataViewer.gridInfo).toContainText('of 20', { timeout: TIMEOUTS.fileOpen });
    await page.locator('#data_editing_toolbar').getByText('Filter', { exact: true }).click();
    const colFilter1 = dataViewer.columnHeader(1).locator('.colFilter');
    await expect(colFilter1).toBeVisible({ timeout: TIMEOUTS.fileOpen });

    // Open x's numeric filter popup, then hide y from the sidebar.
    await colFilter1.getByText('All').click();
    const popup = dataViewer.frame.locator('.filterPopup');
    await expect(popup).toBeVisible();
    await dataViewer.frame.locator('.sidebar-col[data-col-idx="2"] .sidebar-eye-icon').click();

    // The editor is closed; y is gone from the headers AND the rows, which
    // agree on the remaining two columns (z's first value now sits second);
    // and the filter row is still up on x.
    await expect(popup).toHaveCount(0);
    await expect(dataViewer.columnHeader(2)).toHaveCount(0);
    await expect(dataViewer.frame.locator('#data_cols th[data-col-idx]')).toHaveCount(2);
    const row0 = dataViewer.frame.locator('#gridBody tr[data-row="0"]');
    await expect(row0.locator('td[data-col-pos]')).toHaveCount(2);
    await expect(row0.locator('td[data-col-pos="2"]')).toHaveText('41');
    await expect(dataViewer.columnHeader(1).locator('.colFilter')).toBeVisible();
  });

  // The inline text filter applies what you type on a debounce. A blur from
  // clicking elsewhere (a sidebar eye included) deliberately discards pending
  // text -- clicking away is a dismissal. A rebuild that reaches a box still
  // holding focus must instead commit the text first: the rebuilt header is
  // created from the stored filter, so the keystrokes would otherwise be lost.
  // Go to column, which does not move focus, showing a hidden column is such
  // a rebuild.
  test('typed filter text is committed when a show rebuild closes the box', async ({ rstudioPage: page }) => {
    await viewAs('data.frame(s = c("ab", "cd", "ef"), y = 1:3, z = 4:6)');
    await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(dataViewer.gridInfo).toContainText('of 3', { timeout: TIMEOUTS.fileOpen });

    // Hide y, then open the filter row and start typing into s's box (real
    // keyup events schedule the debounced apply).
    await dataViewer.frame.locator('.sidebar-col[data-col-idx="2"] .sidebar-eye-icon').click();
    await expect(dataViewer.columnHeader(2)).toHaveCount(0);
    await page.locator('#data_editing_toolbar').getByText('Filter', { exact: true }).click();
    const colFilter1 = dataViewer.columnHeader(1).locator('.colFilter');
    await expect(colFilter1).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await colFilter1.getByText('All').click();
    const filterInput = dataViewer.columnHeader(1).locator('.textFilterBox');
    await expect(filterInput).toBeVisible();
    await filterInput.pressSequentially('cd');

    // Show y through the grid's go-to entry point (what the host toolbar box
    // calls), which rebuilds the headers with the text box still focused.
    await dataViewer.viewport.evaluate((el) => {
      const w = el.ownerDocument.defaultView as unknown as { goToColumn: (c: number) => void };
      w.goToColumn(2);
    });

    // y is back, the recreated box still shows the text, and the grid is
    // filtered by it.
    await expect(dataViewer.columnHeader(2)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(dataViewer.columnHeader(1).locator('.textFilterBox')).toHaveValue('cd');
    await expect(dataViewer.gridInfo)
      .toContainText('of 1 entries (filtered from 3', { timeout: TIMEOUTS.fileOpen });
    await expect(dataViewer.columnHeader(1).locator('.colFilter')).toHaveClass(/filtered/);
  });

  test('a pinned column can be hidden and returns to the pinned pane', async () => {
    await viewAs('mtcars');
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
    await viewAs('mtcars');
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

  // The cursor's successor after a keyboard hide is the next visible column,
  // which is not fetched yet when the hidden column sat at the right edge of
  // the fetched window. The hide slides the window; the cursor must land on
  // that successor once it arrives, not on the left neighbour that happened
  // to be fetched.
  test('H on the last fetched header moves the cursor to the next column after the slide', async ({ rstudioPage: page }) => {
    await viewAs('as.data.frame(matrix(1:3000, nrow = 10, ncol = 300))');
    await waitForViewer(dataViewer);
    await expect(dataViewer.gridInfo).toContainText('of 10', { timeout: TIMEOUTS.fileOpen });

    // Shrink the fetched window to columns 1..5 through the automation hook
    // (the only way to place the window's edge deterministically).
    await dataViewer.viewport.evaluate((el) => {
      const w = el.ownerDocument.defaultView as unknown as {
        setOffsetAndMaxColumns: (offset: number, max: number) => void;
      };
      w.setOffsetAndMaxColumns(0, 5);
    });
    await expect(dataViewer.columnHeader(5)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(dataViewer.columnHeader(6)).toHaveCount(0);

    await dataViewer.columnHeader(5).click();
    await expect(dataViewer.columnHeader(5)).toHaveClass(/activeHeader/);
    await page.keyboard.press('h');

    // V5 is hidden, V6 is fetched to keep the window five visible columns
    // wide, and the cursor is on it.
    await expect(dataViewer.columnHeader(5)).toHaveCount(0);
    await expect(dataViewer.columnHeader(6)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(dataViewer.columnHeader(6)).toHaveClass(/activeHeader/);
    await expect(dataViewer.frame.locator('th.activeHeader')).toHaveCount(1);
  });

  // The fetched column window is measured in visible columns, so hiding
  // almost everything on a frame wider than the window must still fetch and
  // lay out the few columns that remain -- with no blank span standing in for
  // an unfetched column, wherever those columns sit in the frame.
  test('wide frame: the window follows the visible columns', async () => {
    // 300 columns: wider than the fetched window. matrix(1:3000) is
    // column-major with 10 rows, so column k's first row holds (k-1)*10 + 1.
    await viewAs('as.data.frame(matrix(1:3000, nrow = 10, ncol = 300))');
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
  });

  // The toolbar's Go to column box normally appears only when the columns
  // overflow the viewport, but it doubles as a way back to a hidden column, so
  // it must also appear while anything is hidden -- including after hide-all
  // on a frame that never overflowed.
  test('Go to column stays available while columns are hidden on a narrow frame', async () => {
    await viewAs('data.frame(a = 1:3, b = 4:6, c = 7:9)');
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
  });

  // Hidden columns are saved with the frame's column fingerprint (like pins),
  // so a structural change invalidates them -- and the refresh that follows
  // must fetch the whole new frame, not a window bounded by the old width.
  test('adding a column discards the hidden set and fetches every column', async () => {
    const name = await viewAs('as.data.frame(matrix(0L, nrow = 5, ncol = 4))');
    await waitForViewer(dataViewer);
    await dataViewer.frame.locator('.sidebar-col[data-col-idx="2"] .sidebar-eye-icon').click();
    await expect(dataViewer.columnHeader(2)).toHaveCount(0);
    await expect(dataViewer.gridInfo).toContainText('(1 hidden)');

    await consoleActions.executeInConsole(`${name}$added <- 1L`);

    // The refresh reports and renders the new fifth column, and the column
    // hidden against the old frame is back.
    await expect(dataViewer.gridInfo).toContainText('5 total columns', { timeout: TIMEOUTS.fileOpen });
    await expect(dataViewer.columnHeader(5)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(dataViewer.columnHeader(2)).toBeVisible();
    expect(await colOrder(dataViewer)).toEqual(['0', '1', '2', '3', '4', '5']);
    await expect(dataViewer.gridInfo).not.toContainText('hidden');
    await expect(dataViewer.frame.locator('.sidebar-col[data-col-idx="2"]'))
      .not.toHaveClass(/\bcol-hidden\b/);
  });

  test('hidden columns survive a refresh and are cleared by Reset View', async ({ rstudioPage: page }) => {
    await viewAs('mtcars');
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
  });
});

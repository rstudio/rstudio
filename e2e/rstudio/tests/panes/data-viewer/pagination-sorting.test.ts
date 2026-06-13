import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { DataViewerPane } from '@pages/data_viewer.page';
import { resetSourcePaneState } from '@utils/commands';

const VIEWER_FRAME = '#rstudio_data_viewer_frame';

// Tests from: electron-tests/EditorPane/test_desktop_DataViewer.py
// Issues: https://github.com/rstudio/rstudio/issues/13220
//         https://github.com/rstudio/rstudio/issues/13328

test.describe('Data Viewer', () => {
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
    // resetToUntitled (vs closeSourceDoc) leaves a single Untitled tab
    // behind, which keeps the Source pane open across tests. Without that,
    // the pane closes when the last data viewer tab is closed and re-opens
    // when the next test's View(df) lands -- and the toolbar can get stuck
    // hidden in the open-then-close-then-reopen window (#17738-ish).
    await resetSourcePaneState(page);
    await expect(sourcePane.selectedTab).toContainText('Untitled', { timeout: 5000 });
  });

  // -----------------------------------------------------------------------
  // Issue 13220 - Large data frame navigation
  // -----------------------------------------------------------------------
  test('large data frame - go to column jumps anywhere in the frame', async () => {
    await consoleActions.executeInConsole(
      'df <- data.frame(matrix(1:1000000, nrow=100000, ncol=500))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');

    // The go-to-column jump box appears for frames wider than one fetch
    // window (the pagination arrows are gone -- the grid scrolls
    // continuously through every column).
    await expect(dataViewer.gotoColumnInput).toBeVisible();

    // The summary sidebar lists every column of the frame (lazy-loading their
    // stats), so its header reports the frame total -- not the loaded window.
    // It settles on "500 columns" once the complete column index loads.
    await expect(dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-label')).toHaveText(
      '500 columns', { timeout: 15000 },
    );

    // Jump beyond the fetched window, to the last column, and back home.
    await dataViewer.goToColumn(201);
    await expect(dataViewer.columnHeader(201)).toBeVisible({ timeout: 15000 });

    await dataViewer.goToColumn(500);
    await expect(dataViewer.columnHeader(500)).toBeVisible({ timeout: 15000 });

    // Jump by column NAME: the popup matches against the whole frame's
    // names (fetched separately), not just the loaded window.
    await dataViewer.goToColumn('X450');
    await expect(dataViewer.columnHeader(450)).toBeVisible({ timeout: 15000 });

    await dataViewer.goToColumn(1);
    await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: 15000 });

    // Verify grid info inside iframe (visible row count depends on pane
    // height); wide frames report the visible column range there.
    await expect(dataViewer.gridInfo).toContainText(/Showing 1 to [1-9]\d* of 100,000 entries/);
    await expect(dataViewer.gridInfo).toContainText(/columns 1 to \d+ of 500/);
  });

  // -----------------------------------------------------------------------
  // Odd-numbered columns (edge case: jump targets clamp to the frame)
  // -----------------------------------------------------------------------
  test('odd-numbered column count - jump targets clamp to the frame', async () => {
    await consoleActions.executeInConsole(
      'df <- data.frame(matrix(1:1000000, nrow=1000, ncol=227))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');
    await expect(dataViewer.gotoColumnInput).toBeVisible();

    // A target past the end of the frame clamps to the last column.
    await dataViewer.goToColumn(9999);
    await expect(dataViewer.columnHeader(227)).toBeVisible({ timeout: 15000 });

    // Jump back to the first column.
    await dataViewer.goToColumn(1);
    await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: 15000 });

    // Verify grid info (visible row count depends on pane height)
    await expect(dataViewer.gridInfo).toContainText(/Showing 1 to [1-9]\d* of 1,000 entries/);
  });

  // -----------------------------------------------------------------------
  // Continuous column virtualization: the horizontal scrollbar spans the
  // whole frame, and scrolling past the fetched window slides it in place
  // (no pagination clicks required to reach any column).
  // -----------------------------------------------------------------------
  test('horizontal scroll slides the column window to the last column', async () => {
    await consoleActions.executeInConsole(
      'df <- data.frame(matrix(1:50000, nrow=100, ncol=500))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');
    await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: 15000 });

    // Scroll fully right. The viewport lands on unfetched span columns,
    // which triggers a window slide centered on the visible range; the
    // frame's last column arrives without any pagination clicks.
    await dataViewer.viewport.evaluate((el) => { el.scrollLeft = el.scrollWidth; });

    await expect(dataViewer.columnHeader(500)).toBeVisible({ timeout: 15000 });

    // The last column's data is real: V500 holds 49901..50000, so row 0
    // reads 49901. data-col-pos 200 is V500's position within the
    // [301, 500] window (rownames at 0; the slide's recenter offset clamps
    // to the end of the frame).
    await expect(
      dataViewer.frame.locator('#gridBody tr[data-row="0"] td[data-col-pos="200"]'),
    ).toHaveText('49901', { timeout: 15000 });

    // Scrolling back to the far left slides the window home again.
    await dataViewer.viewport.evaluate((el) => { el.scrollLeft = 0; });
    await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: 15000 });
    await expect(
      dataViewer.frame.locator('#gridBody tr[data-row="0"] td[data-col-pos="1"]'),
    ).toHaveText('1', { timeout: 15000 });
  });

  test('very wide frame - scroll-driven slides reach an arbitrary column', async () => {
    // 10,000 columns: the unfetched spans dominate the layout (the fetched
    // window is 2% of the frame), so this exercises the span math and the
    // x -> column mapping at a scale where estimate drift would show.
    await consoleActions.executeInConsole(
      'df <- as.data.frame(matrix(1:100000, nrow=10, ncol=10000))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');
    await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: 15000 });

    // Jump to ~60% of the frame via the scrollbar (a track click in real
    // usage). The window slides to the viewport, several thousand columns
    // away from anything fetched.
    await dataViewer.viewport.evaluate((el) => {
      el.scrollLeft = Math.round((el.scrollWidth - el.clientWidth) * 0.6);
    });

    // The slide recenters the fetched window on the visible columns. The
    // exact landing column depends on measured widths, so assert
    // structurally: poll until the leftmost rendered data column resolves to
    // an absolute index thousands of columns in, then check its first-row
    // value matches matrix(1:100000)'s column-major fill ((k-1)*10 + 1).
    //
    // Resolve the leftmost cell, its header, and the cell's text in a SINGLE
    // DOM read (one evaluate inside the iframe): a window slide can re-render
    // between separate reads, so split reads race the relayout and a stale
    // locator throws out of the poll instead of retrying. Returns abs 0 when
    // not yet settled so the poll keeps trying.
    const leftmostAbs = async (): Promise<{ abs: number; text: string }> => {
      return await dataViewer.viewport.evaluate((vp) => {
        const doc = vp.ownerDocument!;
        const cell = doc.querySelector(
          '#gridBody tr[data-row="0"] td[data-col-pos]:not([data-col-pos="0"])');
        if (!cell) return { abs: 0, text: '' };
        // data-col-pos is a columnOrder position; data-col-idx is a cols-array
        // index. They coincide only because nothing is pinned here (columnOrder
        // is the identity permutation) -- map through columnOrder if a pin is
        // ever added to this test.
        const pos = cell.getAttribute('data-col-pos');
        const th = doc.querySelector(`th[data-col-idx="${pos}"]`);
        const title = th?.getAttribute('title') ?? '';
        const m = title.match(/^column (\d+):/);
        return { abs: m ? parseInt(m[1], 10) : 0, text: cell.textContent ?? '' };
      });
    };

    await expect
      .poll(async () => (await leftmostAbs()).abs, { timeout: 15000 })
      .toBeGreaterThan(4000);

    const { abs, text } = await leftmostAbs();
    expect(abs).toBeGreaterThan(4000);
    expect(text).toBe(String((abs - 1) * 10 + 1));
  });

  test('an off-screen row block survives a column-window round trip', async () => {
    // Regression guard for the blockColsSig round-trip bug. Sliding the column
    // window away (A -> B) and back (B -> A) remaps the row cache by identity,
    // wiping each block's non-overlap cells. A block cached outside the
    // visible+prefetch vertical range during the round trip used to keep its
    // old signature, so on return (colsSig back to A) it read as "current"
    // while holding undefined cells -- rendering a permanently blank band with
    // no refetch. The slide now purges signatures that don't match the new
    // window, forcing such blocks to refetch when scrolled back into view.
    //
    // 1500 rows -> three FETCH_SIZE(500) row-blocks; 500 columns force the
    // window to slide. matrix() fills column-major, so V1's row r (0-based)
    // holds r + 1. Row 1200 sits in the third block [1000, 1499], which is
    // outside row 0's prefetch-ahead (that only reaches block [500, 999]).
    await consoleActions.executeInConsole(
      'df <- data.frame(matrix(1:750000, nrow=1500, ncol=500))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');
    await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: 15000 });

    const ROW = 1200;

    // Scroll to a vertical row and read V1's cell text there, re-applying the
    // scroll on every poll iteration. A single scrollTop set can be undone by
    // the grid's deferred relayout/refetch, and rendering lands a frame later
    // than the set, so we keep forcing the position until the row renders.
    // Returns "" when the row isn't rendered or its data cell is still blank.
    const readV1AtRow = async (row: number): Promise<string> => {
      return await dataViewer.viewport.evaluate((el, r) => {
        el.scrollTop = r * 23;
        const cell = el.ownerDocument!.querySelector(
          `#gridBody tr[data-row="${r}"] td[data-col-pos="1"]`);
        return cell?.textContent ?? '';
      }, row);
    };

    // Cache the lower block in column window A by scrolling it into view.
    await expect.poll(() => readV1AtRow(ROW), { timeout: 20000 }).toBe(String(ROW + 1));

    // Park back at the top so the lower block sits outside the
    // visible+prefetch range and isn't refetched during the slides below.
    await expect.poll(() => readV1AtRow(0), { timeout: 15000 }).toBe('1');

    // Slide the column window away (scroll fully right) and back (fully left).
    await dataViewer.viewport.evaluate((el) => { el.scrollLeft = el.scrollWidth; });
    await expect(dataViewer.columnHeader(500)).toBeVisible({ timeout: 15000 });
    await dataViewer.viewport.evaluate((el) => { el.scrollLeft = 0; });
    await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: 15000 });

    // Scroll the lower block back into view: its cells must be refetched, not
    // rendered blank from the wiped-but-stale-signature cache. Without the
    // blockColsSig purge this stays blank ("") and the poll times out.
    await expect.poll(() => readV1AtRow(ROW), { timeout: 20000 }).toBe(String(ROW + 1));
  });

  // -----------------------------------------------------------------------
  // Sorting
  // -----------------------------------------------------------------------
  test('column sorting - descending order', async () => {
    // This test exercises sorting in a wide frame with the summary panel
    // active, so keep the 500 columns -- but only a handful of rows. A large
    // row count adds no coverage here (sorting touches a single visible
    // column) while making the server-side summary and initial render slower.
    await consoleActions.executeInConsole(
      'df <- data.frame(matrix(1:50000, nrow=100, ncol=500))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');

    // matrix() fills column-major, so X2 holds 101..200. First row cells are
    // [rowNum, X1, X2, X3, ...] -- X2 is td:nth-child(3). tbody starts with a
    // virtual-scroll spacer row; select the first data row by its data-row
    // attribute instead of :first-child.
    const firstRowX2 = dataViewer.frame.locator('#rsGridData tbody tr[data-row="0"] td:nth-child(3)');
    await expect(firstRowX2).toContainText('101');

    // Click column 2 header twice for descending sort
    await dataViewer.columnHeader(2).click();
    await dataViewer.columnHeader(2).click();

    // After descending sort, first row of X2 should be 200 (max value)
    await expect(firstRowX2).toContainText('200');
  });

  // -----------------------------------------------------------------------
  // Clear-sort button in the status bar
  // -----------------------------------------------------------------------
  test('clear-sort button in the status bar resets the sort', async ({ rstudioPage: page }) => {
    // Values chosen so no cell text is a substring of another -- a stale
    // sort can't false-pass a toContainText assertion.
    await consoleActions.executeInConsole(
      'df <- data.frame(a = c(20, 10, 30))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');

    // First row cells are [rowNum, a]; skip the virtual-scroll spacer row.
    const firstRowA = dataViewer.frame.locator('#rsGridData tbody tr[data-row="0"] td:nth-child(2)');
    await expect(firstRowA).toContainText('20');

    // No sort active: the clear button is hidden.
    await expect(dataViewer.clearSortButton).toBeHidden();

    // Sort column 1 descending (click twice).
    await dataViewer.columnHeader(1).click();
    await dataViewer.columnHeader(1).click();
    await expect(firstRowA).toContainText('30');
    await expect(dataViewer.sortStatus).toContainText('Sorted by: a (descending)');
    await expect(dataViewer.clearSortButton).toBeVisible();

    // The summary sidebar mirrors the sort state on its own icon.
    const sidebarSort = dataViewer.frame
      .locator('.sidebar-col[data-col-idx="1"] .sidebar-sort-icon');
    await expect(sidebarSort).toHaveClass(/sorting_desc/);

    // Clicking the clear button restores the natural row order, clears the
    // status text, hides the button, and resets both the header indicator
    // and the sidebar icon.
    await dataViewer.clearSortButton.click();
    await expect(firstRowA).toContainText('20');
    await expect(dataViewer.sortStatus).not.toContainText('Sorted by');
    await expect(dataViewer.clearSortButton).toBeHidden();
    await expect(dataViewer.columnHeader(1)).not.toHaveClass(/sorting_asc|sorting_desc/);
    await expect(dataViewer.columnHeader(1)).toHaveAttribute('aria-sort', 'none');
    await expect(sidebarSort).not.toHaveClass(/sorting_asc|sorting_desc/);

    // The cleared sort must also persist: clearSort() saves sort: null, and a
    // restore-path regression that re-applied the stale sort on reload (the
    // filter analogue was #17950) would pass everything above. refreshData()
    // synchronously clears the grid DOM, then re-fetches and re-applies the
    // persisted state, so the reappearing first row gates the assertions.
    await page.evaluate((sel: string) => {
      const f = document.querySelector(sel) as HTMLIFrameElement | null;
      const w = f?.contentWindow as unknown as { refreshData?: () => void } | undefined;
      if (!w?.refreshData)
        throw new Error('refreshData() not available on data viewer iframe');
      w.refreshData();
    }, VIEWER_FRAME);

    // The rebuilt grid comes back unsorted: natural row order, no status
    // text, no clear button, and reset header / sidebar indicators.
    await expect(firstRowA).toContainText('20');
    await expect(dataViewer.sortStatus).not.toContainText('Sorted by');
    await expect(dataViewer.clearSortButton).toBeHidden();
    await expect(dataViewer.columnHeader(1)).not.toHaveClass(/sorting_asc|sorting_desc/);
    await expect(dataViewer.columnHeader(1)).toHaveAttribute('aria-sort', 'none');
    await expect(sidebarSort).not.toHaveClass(/sorting_asc|sorting_desc/);
  });

  // -----------------------------------------------------------------------
  // Issue 17863 - Clicking a list-column header must not error
  // -----------------------------------------------------------------------
  test('list columns are not sortable and clicking the header does not error (#17863)', async () => {
    // A data frame with a list column (each element an atomic vector). Sorting
    // such a column server-side errors in R ("unimplemented type 'list'"), so
    // the header must be inert rather than triggering a failing fetch.
    await consoleActions.executeInConsole('df <- data.frame(a = 1:20)');
    await consoleActions.executeInConsole(
      'df$b <- replicate(20, as.numeric(1:100), simplify = FALSE)',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');
    await expect(dataViewer.gridInfo).toContainText(/Showing 1 to [1-9]\d* of 20 entries/);

    // The list column reports its R typeof() in the header tooltip.
    const listHeader = dataViewer.columnHeader(2);
    await expect(listHeader).toHaveAttribute('title', 'column 2: list');

    // Clicking it twice (which would cycle asc/desc on a sortable column) must
    // not apply a sort or surface the error overlay.
    await listHeader.click();
    await listHeader.click();

    await expect(listHeader).not.toHaveClass(/sorting_asc|sorting_desc/);
    await expect(dataViewer.frame.locator('#errorWrapper')).toBeHidden();
    await expect(dataViewer.gridInfo).toContainText(/Showing 1 to [1-9]\d* of 20 entries/);
  });

  // -----------------------------------------------------------------------
  // Issue 13328 - Sorting with many columns after navigating
  // -----------------------------------------------------------------------
  test('sorting after navigating to later columns (#13328)', async () => {
    // Create a 10-row x 400-column data frame with shifted values
    await consoleActions.executeInConsole('data <- replicate(400, 0:9, simplify = FALSE)');
    await consoleActions.executeInConsole(
      'for (i in seq_along(data)) { data[[i]] <- (data[[i]] + i) %% 10 }',
    );
    await consoleActions.executeInConsole('names(data) <- paste0("V", 1:400)');
    await consoleActions.executeInConsole('df <- as.data.frame(data)', { wait: true });
    await consoleActions.executeInConsole('View(df)');

    // Jump beyond the fetched window via the go-to-column box.
    await expect(dataViewer.gotoColumnInput).toBeVisible();
    await dataViewer.goToColumn(201);
    await expect(dataViewer.columnHeader(201)).toBeVisible({ timeout: 15000 });

    // Address V201's cell by its columnOrder position (data-col-pos), not
    // DOM position -- rows lead with the rownames cell and a width-bearing
    // spacer for the unfetched span. The jump centers the fetched window on
    // the target, so resolve the position from V201's header (with no pins,
    // a header's data-col-idx equals its cells' data-col-pos).
    const colPos = await dataViewer.columnHeader(201).getAttribute('data-col-idx');
    const firstRowCol201 = dataViewer.frame.locator(
      `#rsGridData tbody tr[data-row="0"] td[data-col-pos="${colPos}"]`);

    // Sort column 201 descending (click twice)
    await dataViewer.columnHeader(201).click();
    await dataViewer.columnHeader(201).click();

    // After descending sort, first row of column 201 should be 9
    await expect(firstRowCol201).toContainText('9');
  });
});

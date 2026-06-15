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
  test('large data frame - navigation arrows and column pagination', async () => {
    await consoleActions.executeInConsole(
      'df <- data.frame(matrix(1:1000000, nrow=100000, ncol=500))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');

    // Verify navigation arrows are visible
    await expect(dataViewer.rightArrow).toBeVisible();
    await expect(dataViewer.leftArrow).toBeVisible();
    await expect(dataViewer.rightDoubleArrow).toBeVisible();
    await expect(dataViewer.leftDoubleArrow).toBeVisible();

    // Check initial column range
    await expect(dataViewer.columnNumberInput).toHaveValue('1 - 200');

    // The summary sidebar header counts the loaded page against the frame
    // total; both sides must exclude the rowname column, so a miscount on
    // either would surface here as an off-by-one (e.g. "201 of 500").
    await expect(dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-label')).toHaveText(
      '200 of 500 columns',
    );

    // Navigate forward one page
    await dataViewer.rightArrow.click();
    await expect(dataViewer.columnNumberInput).toHaveValue('201 - 400');

    // Jump to last page
    await dataViewer.rightDoubleArrow.click();
    await expect(dataViewer.columnNumberInput).toHaveValue('301 - 500');

    // Jump back to first page
    await dataViewer.leftDoubleArrow.click();
    await expect(dataViewer.columnNumberInput).toHaveValue('1 - 200');

    // Verify grid info inside iframe (visible row count depends on pane height)
    await expect(dataViewer.gridInfo).toContainText(/Showing 1 to [1-9]\d* of 100,000 entries/);
  });

  // -----------------------------------------------------------------------
  // Odd-numbered columns (edge case for pagination)
  // -----------------------------------------------------------------------
  test('odd-numbered column count - pagination edge case', async () => {
    await consoleActions.executeInConsole(
      'df <- data.frame(matrix(1:1000000, nrow=1000, ncol=227))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');
    await expect(dataViewer.columnNumberInput).toHaveValue('1 - 200');

    // The viewer's column-number input renders before the navigation
    // arrows do; wait for the specific arrow to be visible before clicking.
    await expect(dataViewer.rightDoubleArrow).toBeVisible();
    await dataViewer.rightDoubleArrow.click();
    await expect(dataViewer.columnNumberInput).toHaveValue('28 - 227');

    // Jump back to first page
    await dataViewer.leftDoubleArrow.click();
    await expect(dataViewer.columnNumberInput).toHaveValue('1 - 200');

    // Verify grid info (visible row count depends on pane height)
    await expect(dataViewer.gridInfo).toContainText(/Showing 1 to [1-9]\d* of 1,000 entries/);
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

    await expect(dataViewer.columnNumberInput).toHaveValue('1 - 200');

    // Wait for the navigation arrow to render before clicking it.
    await expect(dataViewer.rightArrow).toBeVisible();
    await dataViewer.rightArrow.click();
    await expect(dataViewer.columnNumberInput).toHaveValue('201 - 400');

    // Column 201 is the first data column on page 2: td:nth-child(2).
    // Use data-row to skip the virtual-scroll spacer row.
    const firstRowCol201 = dataViewer.frame.locator('#rsGridData tbody tr[data-row="0"] td:nth-child(2)');

    // Sort column 201 descending (click twice)
    await dataViewer.columnHeader(201).click();
    await dataViewer.columnHeader(201).click();

    // After descending sort, first row of column 201 should be 9
    await expect(firstRowCol201).toContainText('9');
  });
});

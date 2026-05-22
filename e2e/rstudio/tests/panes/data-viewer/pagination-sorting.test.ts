import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { DataViewerPane } from '@pages/data_viewer.page';
import { resetSourcePaneState } from '@utils/commands';

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
    await consoleActions.closeAllBuffersWithoutSaving();
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
    await consoleActions.executeInConsole(
      'df <- data.frame(matrix(1:1000000, nrow=100000, ncol=500))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(df)');

    await expect(sourcePane.selectedTab).toContainText('df');

    // First row cells: [rowNum, X1, X2, X3, ...] -- X2 is td:nth-child(3).
    // tbody starts with a virtual-scroll spacer row; select the first data
    // row by its data-row attribute instead of :first-child.
    const firstRowX2 = dataViewer.frame.locator('#rsGridData tbody tr[data-row="0"] td:nth-child(3)');
    await expect(firstRowX2).toContainText('100001');

    // Click column 2 header twice for descending sort
    await dataViewer.columnHeader(2).click();
    await dataViewer.columnHeader(2).click();

    // After descending sort, first row of X2 should be 200000 (max value)
    await expect(firstRowX2).toContainText('200000');
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

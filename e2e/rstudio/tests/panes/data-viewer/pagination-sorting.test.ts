import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { DataViewerPane } from '@pages/data_viewer.page';

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
    // Close the data viewer tab after each test
    await consoleActions.typeInConsole(".rs.api.executeCommand('closeSourceDoc')");
    await sleep(1000);
  });

  // -----------------------------------------------------------------------
  // Issue 13220 — Large data frame navigation
  // -----------------------------------------------------------------------
  test('large data frame - navigation arrows and column pagination', async ({ rstudioPage: page }) => {
    await consoleActions.typeInConsole('df <- data.frame(matrix(1:1000000, nrow=100000, ncol=500))');
    await sleep(2000);
    await consoleActions.typeInConsole('View(df)');
    await sleep(3000);

    await expect(sourcePane.selectedTab).toContainText('df');

    // Verify navigation arrows are visible
    await expect(dataViewer.rightArrow).toBeVisible();
    await expect(dataViewer.leftArrow).toBeVisible();
    await expect(dataViewer.rightDoubleArrow).toBeVisible();
    await expect(dataViewer.leftDoubleArrow).toBeVisible();

    // Check initial column range
    await expect(dataViewer.columnNumberInput).toHaveValue('1 - 50');

    // Navigate forward one page
    await dataViewer.rightArrow.click();
    await sleep(1000);
    await expect(dataViewer.columnNumberInput).toHaveValue('51 - 100');

    // Jump to last page
    await dataViewer.rightDoubleArrow.click();
    await sleep(1000);
    await expect(dataViewer.columnNumberInput).toHaveValue('451 - 500');

    // Jump back to first page
    await dataViewer.leftDoubleArrow.click();
    await sleep(1000);
    await expect(dataViewer.columnNumberInput).toHaveValue('1 - 50');

    // Verify grid info inside iframe
    await expect(dataViewer.gridInfo).toContainText('100,000 entries, 500 total columns');
  });

  // -----------------------------------------------------------------------
  // Odd-numbered columns (edge case for pagination)
  // -----------------------------------------------------------------------
  test('odd-numbered column count - pagination edge case', async ({ rstudioPage: page }) => {
    await consoleActions.typeInConsole('df <- data.frame(matrix(1:1000000, nrow=1000, ncol=127))');
    await sleep(2000);
    await consoleActions.typeInConsole('View(df)');
    await sleep(3000);

    await expect(sourcePane.selectedTab).toContainText('df');
    await expect(dataViewer.columnNumberInput).toHaveValue('1 - 50');

    // Jump to last page — should show remaining columns (not a full 50)
    await dataViewer.rightDoubleArrow.click();
    await sleep(1000);
    await expect(dataViewer.columnNumberInput).toHaveValue('78 - 127');

    // Jump back to first page
    await dataViewer.leftDoubleArrow.click();
    await sleep(1000);
    await expect(dataViewer.columnNumberInput).toHaveValue('1 - 50');

    // Verify grid info
    await expect(dataViewer.gridInfo).toContainText('1,000 entries, 127 total columns');
  });

  // -----------------------------------------------------------------------
  // Sorting
  // -----------------------------------------------------------------------
  test('column sorting - descending order', async ({ rstudioPage: page }) => {
    await consoleActions.typeInConsole('df <- data.frame(matrix(1:1000000, nrow=100000, ncol=500))');
    await sleep(2000);
    await consoleActions.typeInConsole('View(df)');
    await sleep(3000);

    await expect(sourcePane.selectedTab).toContainText('df');

    // First row cells: [rowNum, X1, X2, X3, ...] — X2 is td:nth-child(3)
    const firstRowX2 = dataViewer.frame.locator('#rsGridData tbody tr:first-child td:nth-child(3)');
    await expect(firstRowX2).toContainText('100001');

    // Click column 2 header twice for descending sort
    await dataViewer.columnHeader(2).click();
    await dataViewer.columnHeader(2).click();
    await sleep(2000);

    // After descending sort, first row of X2 should be 200000 (max value)
    await expect(firstRowX2).toContainText('200000');
  });

  // -----------------------------------------------------------------------
  // Issue 13328 — Sorting with many columns after navigating
  // -----------------------------------------------------------------------
  test('sorting after navigating to later columns (#13328)', async ({ rstudioPage: page }) => {
    // Create a 10-row x 100-column data frame with shifted values
    await consoleActions.typeInConsole('data <- replicate(100, 0:9, simplify = FALSE)');
    await sleep(1000);
    await consoleActions.typeInConsole('for (i in seq_along(data)) { data[[i]] <- (data[[i]] + i) %% 10 }');
    await sleep(1000);
    await consoleActions.typeInConsole('names(data) <- paste0("V", 1:100)');
    await sleep(500);
    await consoleActions.typeInConsole('df <- as.data.frame(data)');
    await sleep(500);
    await consoleActions.typeInConsole('View(df)');
    await sleep(3000);

    await expect(dataViewer.columnNumberInput).toHaveValue('1 - 50');

    // Navigate to second page of columns
    await dataViewer.rightArrow.click();
    await sleep(1000);
    await expect(dataViewer.columnNumberInput).toHaveValue('51 - 100');

    // Column 51 is the first data column on page 2: td:nth-child(2)
    const firstRowCol51 = dataViewer.frame.locator('#rsGridData tbody tr:first-child td:nth-child(2)');

    // Sort column 51 descending (click twice)
    await dataViewer.columnHeader(51).click();
    await dataViewer.columnHeader(51).click();
    await sleep(2000);

    // After descending sort, first row of column 51 should be 9
    await expect(firstRowCol51).toContainText('9');
  });
});

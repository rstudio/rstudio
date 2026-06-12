// Per-column filter widget tests for the data viewer.
//
// The viewer builds one of four typed filter UIs per column based on the
// backend's col_search_type (DataViewer.js: createNumericFilterUI,
// createFactorFilterUI, createTextFilterUI, createBooleanFilterUI). The
// text filter is covered in data_viewer.test.ts; these tests cover the
// numeric range filter, the factor level filter (including its display
// restore across column virtualization), the boolean filter, and the
// inert "All" chip shown for column types with no filter support (Date).

import type { Locator, Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { DataViewerPane } from '@pages/data_viewer.page';
import { resetSourcePaneState } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

test.describe('Data Viewer column filters', () => {
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
    await resetSourcePaneState(page);
    await expect(sourcePane.selectedTab).toContainText('Untitled', { timeout: 5000 });
  });

  // Views `expr`, waits for the first data column header and the initial row
  // batch (the info bar only renders once the first fetch lands -- which is
  // also when the post-load column auto-size rebuilds the headers; touching
  // the filter row before that races the rebuild), then reveals the filter
  // row via the latching toolbar button.
  // filterCol is the column whose filter chip gates "the filter row is up" --
  // it must be a column that actually gets a filter UI (see the inert-types
  // test, where column 1 deliberately has none).
  async function openWithFilterRow(
    page: Page,
    expr: string,
    rowCount: number,
    filterCol = 1,
  ): Promise<void> {
    await consoleActions.executeInConsole(expr);
    await expect(dataViewer.frame.locator('th[data-col-idx="1"]'))
      .toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(dataViewer.gridInfo)
      .toContainText(`of ${rowCount}`, { timeout: TIMEOUTS.fileOpen });
    await page.locator('#data_editing_toolbar').getByText('Filter', { exact: true }).click();
    await expect(dataViewer.frame.locator(`th[data-col-idx="${filterCol}"] .colFilter`))
      .toBeVisible({ timeout: TIMEOUTS.fileOpen });
  }

  // Body cells (rownames + values) whose full text is exactly `text`.
  // Complements the info-bar count assertions: the count is server-reported,
  // so also check the rendered rows -- excluded values must actually leave
  // the grid, and surviving values must actually be shown.
  function gridCells(text: string): Locator {
    return dataViewer.frame
      .locator('#gridBody td')
      .filter({ hasText: new RegExp(`^${text}$`) });
  }

  test('numeric filter applies a typed range, narrows to equality, and clears', async ({
    rstudioPage: page,
  }) => {
    await openWithFilterRow(page, 'View(data.frame(x = 1:20))', 20);

    const colFilter = dataViewer.frame.locator('th[data-col-idx="1"] .colFilter');

    // Clicking the "All" chip swaps in the numeric widget and opens the
    // histogram popup (appended to the iframe body, not the header).
    await colFilter.getByText('All').click();
    const popup = dataViewer.frame.locator('.filterPopup');
    await expect(popup).toBeVisible();
    await expect(popup.locator('.numHist')).toBeVisible();

    // Type a range into the value box. The widget listens for the native
    // "change" event (not per-keystroke input), so commit the value
    // explicitly rather than relying on blur ordering against the popup's
    // light-dismiss handlers. Histogram brushing drives the same updateText
    // -> updateView path, so the typed range covers the shared wire format;
    // brush drag mechanics are not exercised here.
    const numBox = popup.locator('.numValueBox');
    await numBox.fill('5 - 10');
    await numBox.dispatchEvent('change');

    // The filter round-trips as "numeric|5_10": inclusive range, 6 rows.
    await expect(dataViewer.gridInfo)
      .toContainText('of 6 entries (filtered from 20', { timeout: TIMEOUTS.fileOpen });

    // The header label renders the active range, an excluded value is gone
    // from the rendered rows, and a surviving value remains.
    await expect(colFilter).toContainText('[5, 10]');
    await expect(gridCells('15')).toHaveCount(0);
    await expect(gridCells('7').first()).toBeVisible();

    // Narrow to a single-value equality filter ("numeric|7").
    await numBox.fill('7');
    await numBox.dispatchEvent('change');
    await expect(dataViewer.gridInfo)
      .toContainText('of 1 entries (filtered from 20', { timeout: TIMEOUTS.fileOpen });
    await expect(colFilter).toContainText('[7]');
    await expect(gridCells('10')).toHaveCount(0);
    await expect(gridCells('7').first()).toBeVisible();

    // Dismiss the popup, then clear the filter from the header.
    await page.keyboard.press('Escape');
    await expect(popup).toHaveCount(0);
    await colFilter.locator('.clearFilter').click();
    await expect(dataViewer.gridInfo).toContainText('of 20 entries');
    await expect(dataViewer.gridInfo).not.toContainText('filtered from');
    await expect(colFilter).toHaveClass(/unfiltered/);
    await expect(gridCells('15').first()).toBeVisible();
  });

  // The numeric value box fires no native "change" until the popup is
  // dismissed, so a typed (not brushed) value is still uncommitted when the
  // Enter keydown runs. Pre-fix, Enter dismissed the popup first: the column
  // reverted to "All", and the value only landed via a debounced apply
  // ~200ms later -- the grid filtered with no visible filter. Enter now
  // commits the typed value synchronously.
  test('numeric filter commits a typed value on Enter, with the header label in sync', async ({
    rstudioPage: page,
  }) => {
    await openWithFilterRow(page, 'View(data.frame(x = 1:20))', 20);

    const colFilter = dataViewer.frame.locator('th[data-col-idx="1"] .colFilter');
    await colFilter.getByText('All').click();
    const popup = dataViewer.frame.locator('.filterPopup');
    await expect(popup).toBeVisible();

    // fill() sets the value without firing "change", mirroring the
    // type-then-Enter user path.
    const numBox = popup.locator('.numValueBox');
    await numBox.fill('5 - 10');
    await numBox.press('Enter');
    await expect(popup).toHaveCount(0);

    // The filter applies AND the header reflects it -- both halves matter:
    // the bug produced a filtered grid behind an "All" header.
    await expect(dataViewer.gridInfo)
      .toContainText('of 6 entries (filtered from 20', { timeout: TIMEOUTS.fileOpen });
    await expect(colFilter).toContainText('[5, 10]');
    await expect(colFilter).toHaveClass(/filtered/);
    await expect(gridCells('15')).toHaveCount(0);
    await expect(gridCells('7').first()).toBeVisible();
  });

  // Escape is a cancel: the typed value must not be applied late by the
  // debounced apply (or by the native "change" the dismissal replays on the
  // dirty input), and the column reverts to its unfiltered chip.
  test('numeric filter Escape cancels a typed value instead of applying it late', async ({
    rstudioPage: page,
  }) => {
    await openWithFilterRow(page, 'View(data.frame(x = 1:20))', 20);

    const colFilter = dataViewer.frame.locator('th[data-col-idx="1"] .colFilter');
    await colFilter.getByText('All').click();
    const popup = dataViewer.frame.locator('.filterPopup');
    await expect(popup).toBeVisible();

    const numBox = popup.locator('.numValueBox');
    await numBox.fill('5 - 10');
    await numBox.press('Escape');
    await expect(popup).toHaveCount(0);

    // No value was ever committed, so the column reverts to "All".
    await expect(colFilter).toHaveClass(/unfiltered/);
    await expect(colFilter).toContainText('All');

    // ...and stays unfiltered past the debounce window. Asserting that
    // nothing happens needs a bounded wait: 400ms comfortably covers
    // TIMING.filterDebounce (200ms), after which a late apply would have
    // already landed.
    await page.waitForTimeout(400);
    await expect(dataViewer.gridInfo).toContainText('of 20 entries');
    await expect(dataViewer.gridInfo).not.toContainText('filtered from');
    await expect(colFilter).toHaveClass(/unfiltered/);
    // A value the cancelled range would have excluded is still rendered.
    await expect(gridCells('15').first()).toBeVisible();
  });

  test('factor filter applies a clicked level and shows it in the filter box', async ({
    rstudioPage: page,
  }) => {
    await openWithFilterRow(
      page,
      'View(data.frame(f = factor(rep(c("alpha", "beta", "gamma"), c(5, 3, 2)))))',
      10,
    );

    const colFilter = dataViewer.frame.locator('th[data-col-idx="1"] .colFilter');
    await colFilter.getByText('All').click();

    // The popup lists the factor levels; clicking one applies "factor|<level
    // index>" and light-dismisses the popup.
    const popup = dataViewer.frame.locator('.filterPopup');
    await expect(popup.locator('.choiceListItem')).toHaveText(['alpha', 'beta', 'gamma']);
    await popup.locator('.choiceListItem', { hasText: 'beta' }).click();

    await expect(dataViewer.gridInfo)
      .toContainText('of 3 entries (filtered from 10', { timeout: TIMEOUTS.fileOpen });
    await expect(popup).toHaveCount(0);

    // The filter box shows the selected level, and the header is latched
    // into its filtered state. Excluded levels leave the rendered rows;
    // the selected one remains.
    await expect(colFilter.locator('.textFilterBox')).toHaveValue('beta');
    await expect(colFilter).toHaveClass(/filtered/);
    await expect(gridCells('alpha')).toHaveCount(0);
    await expect(gridCells('gamma')).toHaveCount(0);
    await expect(gridCells('beta').first()).toBeVisible();
  });

  // Column virtualization destroys and recreates a header as it scrolls out
  // of and back into the rendered window (#17806). The recreated factor
  // filter must show the applied level name -- the filter itself stays
  // active server-side either way, so a blank box would leave an invisible
  // filter.
  test('factor filter display survives a column scrolling out and back', async ({
    rstudioPage: page,
  }) => {
    await openWithFilterRow(
      page,
      '{ df <- as.data.frame(matrix(1:30000, nrow = 100, ncol = 300)); df[[1]] <- factor(rep(c("alpha", "beta"), length.out = 100)); View(df) }',
      100,
    );

    const colFilter = dataViewer.frame.locator('th[data-col-idx="1"] .colFilter');
    await colFilter.getByText('All').click();
    const popup = dataViewer.frame.locator('.filterPopup');
    await popup.locator('.choiceListItem', { hasText: 'beta' }).click();
    await expect(dataViewer.gridInfo)
      .toContainText('filtered from 100', { timeout: TIMEOUTS.fileOpen });
    // The excluded level is gone from the rendered rows (only column 1
    // holds factor values, so "alpha" can't appear anywhere else).
    await expect(gridCells('alpha')).toHaveCount(0);

    // Evict column 1 from the DOM with a full horizontal scroll, then bring
    // it back.
    await dataViewer.viewport.evaluate((el) => { el.scrollLeft = el.scrollWidth; });
    await expect(dataViewer.frame.locator('th[data-col-idx="1"]'))
      .toHaveCount(0, { timeout: TIMEOUTS.fileOpen });
    await dataViewer.viewport.evaluate((el) => { el.scrollLeft = 0; });

    // The recreated header must render the active filter: latched state and
    // the level name in the box, not a blank.
    await expect(colFilter).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(colFilter).toHaveClass(/filtered/);
    await expect(colFilter.locator('.textFilterBox')).toHaveValue('beta');
  });

  test('boolean filter applies a clicked value and clears', async ({ rstudioPage: page }) => {
    await openWithFilterRow(
      page,
      'View(data.frame(b = rep(c(TRUE, FALSE), c(7, 3))))',
      10,
    );

    const colFilter = dataViewer.frame.locator('th[data-col-idx="1"] .colFilter');
    await colFilter.getByText('All').click();

    const popup = dataViewer.frame.locator('.filterPopup');
    await expect(popup.locator('.choiceListItem')).toHaveText(['TRUE', 'FALSE']);
    await popup.locator('.choiceListItem', { hasText: 'TRUE' }).click();

    await expect(dataViewer.gridInfo)
      .toContainText('of 7 entries (filtered from 10', { timeout: TIMEOUTS.fileOpen });
    await expect(colFilter).toContainText('TRUE');
    await expect(gridCells('FALSE')).toHaveCount(0);
    await expect(gridCells('TRUE').first()).toBeVisible();

    await colFilter.locator('.clearFilter').click();
    await expect(dataViewer.gridInfo).toContainText('of 10 entries');
    await expect(dataViewer.gridInfo).not.toContainText('filtered from');
    await expect(gridCells('FALSE').first()).toBeVisible();
  });

  // Date columns get no col_search_type from the backend, so the filter row
  // renders no filter UI for them at all (setFilterUIVisible only builds a
  // widget for the four supported search types). This pins the intended
  // behavior so a future date filter updates the test rather than the gap
  // regressing silently into "filtering looks broken".
  test('columns without filter support get no filter widget', async ({
    rstudioPage: page,
  }) => {
    // Column 1 is a Date (unsupported); gate the filter row on column 2.
    await openWithFilterRow(
      page,
      'View(data.frame(d = as.Date("2026-01-01") + 1:5, x = 1:5))',
      5,
      2,
    );

    // The Date column has no filter chip while its numeric neighbor does.
    await expect(dataViewer.frame.locator('th[data-col-idx="1"] .colFilter'))
      .toHaveCount(0);

    // The numeric column right next to it still filters normally.
    const numFilter = dataViewer.frame.locator('th[data-col-idx="2"] .colFilter');
    await numFilter.getByText('All').click();
    await expect(dataViewer.frame.locator('.filterPopup')).toBeVisible();
  });
});

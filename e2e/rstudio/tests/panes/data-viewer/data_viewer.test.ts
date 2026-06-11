// Data Viewer tests ported from
// src/cpp/tests/automation/testthat/test-automation-data-viewer.R.
//
// Covers the temporary-expression iframe (#14657), the search/filter
// toolbar + viewerLink cell-explorer hop, the three-state column sort
// cycle, the pin-icon column reorder, per-object state persistence
// across a refresh, and HTML-special-character escaping in both cell
// values and column names.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { DataViewerPane } from '@pages/data_viewer.page';
import { resetSourcePaneState, executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const VIEWER_FRAME = '#rstudio_data_viewer_frame';

// Waits for the data viewer iframe to render its first data column header.
// Used as a "viewer is ready" gate before introspection. Returns the
// FrameLocator so callers can chain queries off the same iframe.
async function waitForViewer(dataViewer: DataViewerPane): Promise<void> {
  await expect(dataViewer.frame.locator('th[data-col-idx="1"]'))
    .toBeVisible({ timeout: TIMEOUTS.fileOpen });
}

// Reads the className attribute off the column-1 header inside the iframe.
// Sort and pin state are reflected on the th, so the test asserts on
// substring matches of this string.
async function colHeaderClass(dataViewer: DataViewerPane, colIdx: number): Promise<string> {
  return (await dataViewer.frame.locator(`th[data-col-idx="${colIdx}"]`)
    .getAttribute('class')) ?? '';
}

// Returns the data-col-idx of every column header in left-to-right order.
// Used to assert pin reordering: pinning column 3 should shift it just
// after the rownames column (idx 0).
async function colOrder(dataViewer: DataViewerPane): Promise<string[]> {
  return dataViewer.frame.locator('#data_cols th').evaluateAll((ths) =>
    (ths as HTMLElement[]).map((th) => th.getAttribute('data-col-idx') ?? ''),
  );
}

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
    // Tear down every viewer / cell-explorer tab from this test, but go
    // through resetSourcePaneState (not closeAllSourceDocs) so the Source
    // pane never reaches zero tabs. closeAllSourceDocs would fire
    // LastSourceDocClosedEvent and start a 250ms HIDE animation, which
    // races the next test's View(df) and leaves its iframe in a pane that
    // never finishes loading (#17738). resetToUntitled keeps an Untitled
    // tab alive through the close so the pane stays in its NORMAL state.
    await resetSourcePaneState(page);
    // Assert the Untitled tab is what's selected -- doubles as a wait gate
    // for the reset to land and as a canary for #17738 regressions.
    await expect(sourcePane.selectedTab).toContainText('Untitled', { timeout: 5000 });
  });

  // https://github.com/rstudio/rstudio/pull/14657
  test('viewer opens for a temporary R expression', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('View(subset(mtcars, mpg >= 30))');
    await expect(page.locator(VIEWER_FRAME))
      .toHaveAttribute('src', /gridviewer\.html/, { timeout: TIMEOUTS.fileOpen });
  });

  test('search filter + viewerLink opens an explorer tab for the cell', async ({ rstudioPage: page }) => {
    // data.frame with a list column: the data viewer renders each list
    // entry as a clickable viewerLink that opens the cell explorer.
    await consoleActions.executeInConsole(
      '{ d <- data.frame(x = letters); d$y <- lapply(letters, as.list); row.names(d) <- LETTERS; View(d) }',
    );
    await waitForViewer(dataViewer);

    // The SearchWidget on the data-editing toolbar exposes its <input> via a
    // hidden <label> with text "Search data table". getByLabel reaches the
    // right element regardless of the obfuscated GWT class names.
    const search = page.locator('#data_editing_toolbar').getByLabel('Search data table');
    await search.click();
    await page.keyboard.type('K');

    // The filter is what reduces the list-column to a single viewerLink for
    // row "K"; without it the table still has all 26 rows.
    const viewerLink = dataViewer.frame.locator('.viewerLink');
    await expect(viewerLink).toHaveCount(1, { timeout: TIMEOUTS.fileOpen });
    await viewerLink.click();

    // The cell explorer opens as a new source tab titled `d["K", 2]`.
    await expect(sourcePane.selectedTab).toContainText('d["K", 2]', {
      timeout: TIMEOUTS.fileOpen,
    });

    // The afterEach closes both tabs via documentCloseAllNoSave; no manual
    // cleanup needed here.
  });

  // Column virtualization (#17806) destroys and recreates a column's header
  // when it scrolls out of and back into the rendered window. The per-column
  // filter must restore its applied value on the recreated header rather than
  // reset to "All".
  test('per-column filter display survives a column scrolling out and back (#17806)', async ({ rstudioPage: page }) => {
    // 300 columns so the first data column is virtualized out of the DOM on a
    // full horizontal scroll; column 1 is made character so its filter is a
    // simple text box to drive.
    await consoleActions.executeInConsole(
      '{ df <- as.data.frame(matrix(1:30000, nrow = 100, ncol = 300)); df[[1]] <- rep(letters[1:5], length.out = 100); View(df) }',
    );
    await waitForViewer(dataViewer);

    // Wait for the initial row batch to land before touching the filter. The
    // info bar's row count only appears once the first fetch returns -- which is
    // also when the post-load column auto-size runs. Opening the filter before
    // that races the auto-size's header rebuild, which would tear the editor
    // down mid-type and force the retry loop below to spin (slow + flaky).
    await expect(dataViewer.gridInfo).toContainText('100', { timeout: TIMEOUTS.fileOpen });

    // Reveal the per-column filter row (the latching "Filter" toolbar button).
    await page.locator('#data_editing_toolbar').getByText('Filter', { exact: true }).click();
    const col1Filter = dataViewer.frame.locator('th[data-col-idx="1"] .colFilter');
    await expect(col1Filter).toBeVisible({ timeout: TIMEOUTS.fileOpen });

    // Apply a text filter to column 1. The text input only exists once the
    // filter editor is open (clicking the "All" chip opens it), and the editor
    // auto-dismisses if it blurs while still empty -- so the open-and-type has
    // to land as a unit. Retry the whole thing until the value sticks. Probe
    // for the input with count() (instant) rather than inputValue(), which
    // would block on the action timeout each iteration while the editor is
    // closed. pressSequentially fires real keyup (which the filter listens
    // for); fill() alone would not trigger the search.
    const filterInput = dataViewer.frame.locator('th[data-col-idx="1"] .textFilterBox');
    const allLabel = col1Filter.getByText('All');
    await expect(async () => {
      if ((await filterInput.count()) === 0) {
        await allLabel.click();
        await filterInput.waitFor({ state: 'visible', timeout: 2000 });
      }
      await filterInput.fill('');
      await filterInput.pressSequentially('a');
      await expect(filterInput).toHaveValue('a', { timeout: 2000 });
    }).toPass({ timeout: TIMEOUTS.fileOpen });

    // Confirm the filter actually applied -- the info bar gains the
    // "filtered from" suffix once the debounced search round-trips.
    await expect(dataViewer.gridInfo)
      .toContainText('filtered from 100', { timeout: TIMEOUTS.fileOpen });

    // Scroll the column window fully right: column 1 is evicted from the DOM
    // (this also confirms column virtualization is active).
    await dataViewer.viewport.evaluate((el) => { el.scrollLeft = el.scrollWidth; });
    await expect(dataViewer.frame.locator('th[data-col-idx="1"]'))
      .toHaveCount(0, { timeout: TIMEOUTS.fileOpen });

    // Scroll back: the header is recreated and must show the active filter
    // value, not "All".
    await dataViewer.viewport.evaluate((el) => { el.scrollLeft = 0; });
    await expect(col1Filter).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(col1Filter).toHaveClass(/filtered/);
    // filterInput is a lazy locator, so it re-resolves to the recreated input.
    await expect(filterInput).toHaveValue('a');
  });

  test('sort headers cycle through asc, desc, and unsorted', async () => {
    await consoleActions.executeInConsole('View(mtcars)');
    await waitForViewer(dataViewer);

    const header = dataViewer.frame.locator('th[data-col-idx="1"]');

    // Initial: just "sorting" (unsorted). The DataTables baseline class.
    await expect.poll(() => colHeaderClass(dataViewer, 1))
      .toMatch(/(?:^|\s)sorting(?:\s|$)/);

    await header.click();
    await expect.poll(() => colHeaderClass(dataViewer, 1))
      .toMatch(/sorting_asc/);

    await header.click();
    await expect.poll(() => colHeaderClass(dataViewer, 1))
      .toMatch(/sorting_desc/);

    // Third click drops back to the base class -- the asc/desc modifier
    // is gone but "sorting" remains.
    await header.click();
    await expect.poll(async () => {
      const cls = await colHeaderClass(dataViewer, 1);
      return /sorting/.test(cls) && !/sorting_(asc|desc)/.test(cls);
    }).toBe(true);
  });

  test('pin icon moves a column to the pinned prefix', async () => {
    await consoleActions.executeInConsole('View(mtcars)');
    await waitForViewer(dataViewer);

    // Initial layout: rownames column (0), then mtcars columns 1..n in order.
    expect((await colOrder(dataViewer)).slice(0, 4)).toEqual(['0', '1', '2', '3']);

    // The pin icon is opacity:0 by default; clicking via locator dispatches
    // a real click regardless of visibility.
    const pinCol3 = dataViewer.frame.locator('th[data-col-idx="3"] .pin-icon');
    await pinCol3.click();

    await expect.poll(() => colHeaderClass(dataViewer, 3))
      .toMatch(/\bpinned\b/);

    // Column 3 now sits between rownames and the rest of the unpinned columns.
    expect((await colOrder(dataViewer)).slice(0, 3)).toEqual(['0', '3', '1']);

    // Toggle the pin off; original order returns.
    await pinCol3.click();
    await expect.poll(() => colHeaderClass(dataViewer, 3))
      .not.toMatch(/\bpinned\b/);
    expect((await colOrder(dataViewer)).slice(0, 4)).toEqual(['0', '1', '2', '3']);
  });

  // https://github.com/rstudio/rstudio/issues/17835
  test('a pinned column stays pinned to its original column across column pagination', async ({ rstudioPage: page }) => {
    // 300 columns exceeds the 200-column page size, so server-side column
    // windowing (the "Cols:" navigator) is active and column 1 can be paged
    // out of view. Column 1 carries a sentinel value so we can confirm its
    // data -- not just its header -- follows the pin into a window that does
    // not otherwise include column 1.
    await consoleActions.executeInConsole(
      '{ .rs.pin_paginate_df <- as.data.frame(matrix(1:30000, nrow = 100, ncol = 300)); .rs.pin_paginate_df[[1]] <- rep("PINSENTINEL", 100); View(.rs.pin_paginate_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Pin column 1 (V1), which is in the initial window.
      await dataViewer.frame.locator('th[data-col-idx="1"] .pin-icon').click();
      await expect.poll(() => colHeaderClass(dataViewer, 1)).toMatch(/\bpinned\b/);

      // Page to a window that starts well past column 1 (offset 100 -> columns
      // 101..300). This is the exact action from the bug report: with the bug,
      // the pinned slot started showing column 101 instead of column 1.
      await page.evaluate((sel: string) => {
        const f = document.querySelector(sel) as HTMLIFrameElement | null;
        const w = f?.contentWindow as unknown as
          { setOffsetAndMaxColumns?: (offset: number, max: number) => void } | undefined;
        if (!w?.setOffsetAndMaxColumns)
          throw new Error('setOffsetAndMaxColumns() not available on data viewer iframe');
        w.setOffsetAndMaxColumns(100, 200);
      }, VIEWER_FRAME);

      // The new window loaded: a header for column 101 (outside the old window)
      // is now present.
      await expect(dataViewer.frame.locator('th[title^="column 101:"]'))
        .toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // The pinned slot still tracks the ORIGINAL column 1: it is still pinned,
      // its header reports absolute column 1 (not the window's first column),
      // and a pinned body cell still shows column 1's sentinel value -- so the
      // column's data was fetched even though it lies outside the visible
      // window.
      const pinnedHeader = dataViewer.frame.locator('th[data-col-idx="1"]');
      await expect(pinnedHeader).toHaveClass(/\bpinned\b/);
      await expect(pinnedHeader).toHaveAttribute('title', /^column 1:/);
      await expect(dataViewer.frame.locator('#gridBody td.pinned', { hasText: 'PINSENTINEL' }).first())
        .toBeVisible({ timeout: TIMEOUTS.fileOpen });
    } finally {
      await consoleActions.executeInConsole('rm(".rs.pin_paginate_df", envir = .GlobalEnv)');
    }
  });

  test('per-object state survives a refresh', async ({ rstudioPage: page }) => {
    // Use a uniquely-named object so localStorage for this viewer can't be
    // contaminated by a previous test that happened to View(mtcars).
    await consoleActions.executeInConsole(
      '{ .rs.persist_test_df <- mtcars; View(.rs.persist_test_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Sort col 1 descending (asc -> desc takes two clicks).
      const col1 = dataViewer.frame.locator('th[data-col-idx="1"]');
      await col1.click();
      await expect.poll(() => colHeaderClass(dataViewer, 1)).toMatch(/sorting_asc/);
      await col1.click();
      await expect.poll(() => colHeaderClass(dataViewer, 1)).toMatch(/sorting_desc/);

      // Pin col 3.
      await dataViewer.frame.locator('th[data-col-idx="3"] .pin-icon').click();
      await expect.poll(() => colHeaderClass(dataViewer, 3)).toMatch(/\bpinned\b/);

      // Trigger the iframe-level refresh path. The bootstrap then clears
      // the DOM, re-fetches, and re-applies persisted state from
      // localStorage. Both the sort and the pin should come back.
      await page.evaluate((sel: string) => {
        const f = document.querySelector(sel) as HTMLIFrameElement | null;
        const w = f?.contentWindow as unknown as { refreshData?: () => void } | undefined;
        if (!w?.refreshData) throw new Error('refreshData() not available on data viewer iframe');
        w.refreshData();
      }, VIEWER_FRAME);

      await expect.poll(async () => {
        const c1 = await colHeaderClass(dataViewer, 1);
        const c3 = await colHeaderClass(dataViewer, 3);
        return /sorting_desc/.test(c1) && /\bpinned\b/.test(c3);
      }).toBe(true);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.persist_test_df", envir = .GlobalEnv)',
      );
    }
  });

  // https://github.com/rstudio/rstudio/issues/17830
  // Saved per-object UI state lives in localStorage (shared same-origin between
  // the host page and the data viewer iframe). Explicitly closing the viewer
  // tab must discard it, so reopening the data set starts fresh -- the clear
  // runs host-side because the iframe is already detached by the time the close
  // reaches us.
  //
  // NOTE: the complementary half of this invariant -- that MOVING the tab
  // between source columns must PRESERVE the saved state -- is deliberately not
  // covered here. The gate lives in DataEditingTarget.onDismiss, which clears
  // only on DISMISS_TYPE_CLOSE and not on DISMISS_TYPE_MOVE. A cross-column tab
  // move is a drag-and-drop layout operation with no AppCommand to drive it
  // from the automation bridge, so it can't be exercised reliably from
  // Playwright. If a future refactor drops that dismissType guard (or clears
  // unconditionally), a column move would silently wipe the user's sorts and
  // filters and this suite would stay green -- so guard the invariant at the
  // source, not just here.
  test('explicitly closing the viewer clears its saved sorts and filters (#17830)', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '{ .rs.close_clear_df <- mtcars; View(.rs.close_clear_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Sort col 1 ascending -- this writes per-object state to localStorage.
      const col1 = dataViewer.frame.locator('th[data-col-idx="1"]');
      await col1.click();
      await expect.poll(() => colHeaderClass(dataViewer, 1)).toMatch(/sorting_asc/);

      // The saved-state entry for this object is readable from the host page,
      // which both confirms the write landed and proves the host/iframe share
      // localStorage (the assumption the host-side clear relies on).
      const savedKey = () => page.evaluate(() =>
        Object.keys(window.localStorage).find(
          (k) => k.startsWith('rstudio.dataViewer:') && k.includes('close_clear_df')) ?? null);
      await expect.poll(savedKey, { timeout: TIMEOUTS.fileOpen }).not.toBeNull();

      // Explicitly close the data viewer tab (DISMISS_TYPE_CLOSE).
      await executeCommand(page, 'closeSourceDoc');
      await expect(sourcePane.selectedTab).toContainText('Untitled', { timeout: 5000 });

      // The saved state must be gone, so a later View() of the same object
      // would start unsorted/unfiltered.
      await expect.poll(savedKey, { timeout: TIMEOUTS.fileOpen }).toBeNull();
    } finally {
      await consoleActions.executeInConsole('rm(".rs.close_clear_df", envir = .GlobalEnv)');
    }
  });

  // https://github.com/rstudio/rstudio/issues/17830
  // A saved filter is restored on open, but it must also reveal the filter row
  // so it's visible and editable -- before the fix the rows were filtered with
  // no filter UI shown. A page/iframe reload preserves saved state (it's not a
  // close), so it exercises the restore path.
  test('restored filters reveal the filter row after a reload (#17830)', async ({ rstudioPage: page }) => {
    // A character first column gives a simple text-box filter to drive.
    await consoleActions.executeInConsole(
      '{ .rs.restore_filter_df <- data.frame(x = letters, stringsAsFactors = FALSE); View(.rs.restore_filter_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      const filterToggle = page.locator('#data_editing_toolbar .rstudio_dt_filter_toggle');

      // Reveal the filter row and apply a text filter to column 1. Mirrors the
      // open-and-type retry from the #17806 test (the editor auto-dismisses if
      // it blurs while empty, so the open + type has to land as a unit).
      await page.locator('#data_editing_toolbar').getByText('Filter', { exact: true }).click();
      const col1Filter = dataViewer.frame.locator('th[data-col-idx="1"] .colFilter');
      await expect(col1Filter).toBeVisible({ timeout: TIMEOUTS.fileOpen });

      const filterInput = dataViewer.frame.locator('th[data-col-idx="1"] .textFilterBox');
      const allLabel = col1Filter.getByText('All');
      await expect(async () => {
        if ((await filterInput.count()) === 0) {
          await allLabel.click();
          await filterInput.waitFor({ state: 'visible', timeout: 2000 });
        }
        await filterInput.fill('');
        await filterInput.pressSequentially('a');
        await expect(filterInput).toHaveValue('a', { timeout: 2000 });
      }).toPass({ timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.gridInfo)
        .toContainText('filtered from', { timeout: TIMEOUTS.fileOpen });

      // Reload only the data viewer iframe: a reload preserves saved state (it's
      // not a close), so the filter must come back -- and reveal its row.
      await page.evaluate((sel: string) => {
        const f = document.querySelector(sel) as HTMLIFrameElement | null;
        if (!f?.contentWindow) throw new Error('data viewer iframe not accessible');
        f.contentWindow.location.reload();
      }, VIEWER_FRAME);

      await waitForViewer(dataViewer);

      // The filter row is shown automatically (no manual toolbar click) and
      // still carries the active value -- the core of the bug.
      const col1FilterAfter = dataViewer.frame.locator('th[data-col-idx="1"] .colFilter');
      await expect(col1FilterAfter).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(col1FilterAfter).toHaveClass(/filtered/);
      await expect(dataViewer.frame.locator('th[data-col-idx="1"] .textFilterBox'))
        .toHaveValue('a');

      // The toolbar funnel reflects the restored filter state (aria-pressed is
      // set by the LatchingToolbarButton when filterStateCallback latches it).
      await expect(filterToggle)
        .toHaveAttribute('aria-pressed', 'true', { timeout: TIMEOUTS.fileOpen });

      // ...and the data is still filtered.
      await expect(dataViewer.gridInfo)
        .toContainText('filtered from', { timeout: TIMEOUTS.fileOpen });
    } finally {
      await consoleActions.executeInConsole('rm(".rs.restore_filter_df", envir = .GlobalEnv)');
    }
  });

  // https://github.com/rstudio/rstudio/issues/17861
  // Adding a column refreshes the grid in place. As long as the row count is
  // unchanged the refresh should keep the user where they were scrolled,
  // instead of snapping back to the first row. (Column removal -- another
  // in-place change -- is covered by the next test.)
  test('scroll position is preserved when a column is added (#17861)', async () => {
    await consoleActions.executeInConsole(
      '{ .rs.scroll_pos_df <- as.data.frame(matrix(0L, nrow = 2000, ncol = 5)); View(.rs.scroll_pos_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Scroll well down into the grid and record where we landed (clamped to
      // the scrollable range by the browser).
      const target = await dataViewer.viewport.evaluate((el: HTMLElement) => {
        el.scrollTop = 20000;
        return el.scrollTop;
      });
      expect(target).toBeGreaterThan(0);

      // Add a column: the row count is unchanged, so this is exactly the kind
      // of in-place change whose scroll position should survive. The
      // assignment fires the structure-changed refresh through the backend.
      await consoleActions.executeInConsole('.rs.scroll_pos_df$added <- 1L');

      // The refresh must both report the new column count (5 -> 6 total) and
      // actually bring the new sixth column into view. (The latter regressed
      // when a refresh reused the previous frame's stale totalCols to clamp the
      // requested column window, dropping any column added since.)
      await expect(dataViewer.gridInfo)
        .toContainText('6 total columns', { timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.frame.locator('th[data-col-idx="6"]'))
        .toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // Position should be restored to (about) where it was, not reset to the
      // top. Allow one ROW_HEIGHT (23px) of slack for rounding. On the pre-fix
      // code this would be 0.
      await expect.poll(
        () => dataViewer.viewport.evaluate((el: HTMLElement) => el.scrollTop),
        { message: 'scroll position should be preserved across the refresh' },
      ).toBeGreaterThan(target - 23);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.scroll_pos_df", envir = .GlobalEnv)',
      );
    }
  });

  // https://github.com/rstudio/rstudio/issues/17861
  // When the refresh is for genuinely new data -- signalled by a change in the
  // underlying row count -- the grid resets to the top, since the previous row
  // position no longer maps to anything meaningful.
  test('scroll position resets to the top when the row count changes (#17861)', async () => {
    await consoleActions.executeInConsole(
      '{ .rs.scroll_reset_df <- as.data.frame(matrix(0L, nrow = 2000, ncol = 5)); View(.rs.scroll_reset_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      await dataViewer.viewport.evaluate((el: HTMLElement) => { el.scrollTop = 20000; });
      await expect.poll(
        () => dataViewer.viewport.evaluate((el: HTMLElement) => el.scrollTop),
      ).toBeGreaterThan(0);

      // Halve the rows: still tall enough to scroll, but a different row count,
      // so the refresh should snap back to the top rather than restoring the
      // old position.
      await consoleActions.executeInConsole(
        '.rs.scroll_reset_df <- .rs.scroll_reset_df[1:1000, ]',
      );

      // Wait for the refresh to report the new row count.
      await expect(dataViewer.gridInfo)
        .toContainText('1,000', { timeout: TIMEOUTS.fileOpen });

      await expect.poll(
        () => dataViewer.viewport.evaluate((el: HTMLElement) => el.scrollTop),
        { message: 'scroll position should reset to the top for new data' },
      ).toBe(0);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.scroll_reset_df", envir = .GlobalEnv)',
      );
    }
  });

  // https://github.com/rstudio/rstudio/issues/17861
  // Removing a column is the other in-place change (alongside column add)
  // that leaves the row count unchanged; the scroll position should survive
  // it too. This exercises a different refresh path than the add case.
  test('scroll position is preserved when a column is removed (#17861)', async () => {
    await consoleActions.executeInConsole(
      '{ .rs.scroll_drop_df <- as.data.frame(matrix(0L, nrow = 2000, ncol = 6)); View(.rs.scroll_drop_df) }',
    );
    try {
      await waitForViewer(dataViewer);
      await expect(dataViewer.gridInfo)
        .toContainText('6 total columns', { timeout: TIMEOUTS.fileOpen });

      const target = await dataViewer.viewport.evaluate((el: HTMLElement) => {
        el.scrollTop = 20000;
        return el.scrollTop;
      });
      expect(target).toBeGreaterThan(0);

      // Drop a column: row count unchanged, so the position should be kept.
      await consoleActions.executeInConsole('.rs.scroll_drop_df$V6 <- NULL');

      // The refresh should report the reduced column count (6 -> 5 total)...
      await expect(dataViewer.gridInfo)
        .toContainText('5 total columns', { timeout: TIMEOUTS.fileOpen });

      // ...and keep the user where they were, not snap to the top. Allow one
      // ROW_HEIGHT (23px) of slack for rounding.
      await expect.poll(
        () => dataViewer.viewport.evaluate((el: HTMLElement) => el.scrollTop),
        { message: 'scroll position should be preserved across a column removal' },
      ).toBeGreaterThan(target - 23);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.scroll_drop_df", envir = .GlobalEnv)',
      );
    }
  });

  // https://github.com/rstudio/rstudio/issues/17861
  // "Reset View" rebuilds the grid like an in-place refresh, but deliberately
  // returns to the top and discards the captured scroll position -- even when
  // the row count is unchanged (which would otherwise trigger a restore). This
  // guards the explicit pendingScrollRestore drop in refreshAndReset().
  test('Reset View returns to the top even when the row count is unchanged (#17861)', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '{ .rs.reset_view_df <- as.data.frame(matrix(0L, nrow = 2000, ncol = 5)); View(.rs.reset_view_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      const target = await dataViewer.viewport.evaluate((el: HTMLElement) => {
        el.scrollTop = 20000;
        return el.scrollTop;
      });
      expect(target).toBeGreaterThan(0);

      // Drive the iframe's refreshAndReset() directly -- the same method the
      // toolbar's "Reset View" option invokes.
      await page.evaluate((sel: string) => {
        const f = document.querySelector(sel) as HTMLIFrameElement | null;
        const w = f?.contentWindow as unknown as { refreshAndReset?: () => void } | undefined;
        if (!w?.refreshAndReset) throw new Error('refreshAndReset() not available on data viewer iframe');
        w.refreshAndReset();
      }, VIEWER_FRAME);

      // Wait for the grid to finish rebuilding (headers + a real data cell --
      // body cells carry data-col-pos; the empty colspan spacer row does not --
      // back in the DOM), by which point any erroneous restore would already
      // have run.
      await waitForViewer(dataViewer);
      await expect(dataViewer.frame.locator('#gridBody td[data-col-pos]').first())
        .toBeVisible({ timeout: TIMEOUTS.fileOpen });

      await expect.poll(
        () => dataViewer.viewport.evaluate((el: HTMLElement) => el.scrollTop),
        { message: 'Reset View should return to the top' },
      ).toBe(0);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.reset_view_df", envir = .GlobalEnv)',
      );
    }
  });

  // The scroll position is part of the per-object UI state persisted to
  // localStorage (alongside sorts/filters/pins), so it survives a close/reopen
  // reload the same way they do. A page/iframe reload preserves saved state
  // (it's not a close), so it exercises the restore path -- mirroring the
  // #17830 filter-restore test.
  test('scroll position is restored after a reload', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '{ .rs.scroll_reload_df <- as.data.frame(matrix(0L, nrow = 2000, ncol = 5)); View(.rs.scroll_reload_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Scroll well down, then fire scrollend so the settled position is
      // persisted to localStorage (onScrollEnd -> saveState). Dispatching the
      // event explicitly removes the timing flake of waiting on the browser to
      // emit scrollend for a programmatic scroll.
      const target = await dataViewer.viewport.evaluate((el: HTMLElement) => {
        el.scrollTop = 20000;
        el.dispatchEvent(new Event('scrollend'));
        return el.scrollTop;
      });
      expect(target).toBeGreaterThan(0);

      // Confirm the position landed in the shared (host/iframe) localStorage
      // entry: both a wait gate for the write and proof it was persisted.
      const savedScrollTop = () => page.evaluate(() => {
        const key = Object.keys(window.localStorage).find(
          (k) => k.startsWith('rstudio.dataViewer:') && k.includes('scroll_reload_df'));
        if (!key)
          return null;
        try {
          const state = JSON.parse(window.localStorage.getItem(key) ?? '');
          return state && state.scroll ? state.scroll.top : null;
        } catch (e) {
          return null;
        }
      });
      await expect.poll(savedScrollTop, { timeout: TIMEOUTS.fileOpen })
        .toBeGreaterThan(0);

      // Reload only the data viewer iframe: a reload preserves saved state (it's
      // not a close), so the scroll position must come back.
      await page.evaluate((sel: string) => {
        const f = document.querySelector(sel) as HTMLIFrameElement | null;
        if (!f?.contentWindow) throw new Error('data viewer iframe not accessible');
        f.contentWindow.location.reload();
      }, VIEWER_FRAME);

      await waitForViewer(dataViewer);
      await expect(dataViewer.frame.locator('#gridBody td[data-col-pos]').first())
        .toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // Position is restored to (about) where it was, not reset to the top.
      // Allow one ROW_HEIGHT (23px) of slack for rounding.
      await expect.poll(
        () => dataViewer.viewport.evaluate((el: HTMLElement) => el.scrollTop),
        { message: 'scroll position should be restored after a reload' },
      ).toBeGreaterThan(target - 23);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.scroll_reload_df", envir = .GlobalEnv)',
      );
    }
  });

  test('HTML-special cell values render as text, not markup', async () => {
    // textContent encodes <, >, and & but leaves quotes as plain text.
    // The security property is that nothing user-supplied becomes a real
    // DOM element.
    const setup = `{ .rs.escape_test_df <- data.frame(a = c("<script>x</script>", "tom & jerry", "\\"quoted\\"", "it's"), stringsAsFactors = FALSE); View(.rs.escape_test_df) }`;
    await consoleActions.executeInConsole(setup);
    try {
      await waitForViewer(dataViewer);

      // Nothing user-supplied should have escaped into a real DOM element.
      await expect(dataViewer.frame.locator('#gridBody script')).toHaveCount(0);

      // textContent of the rendered cells matches the original strings
      // exactly, including the un-encoded quotes.
      const cellText = await dataViewer.frame.locator('#gridBody .textCell')
        .evaluateAll((tds) => (tds as HTMLElement[]).map((td) => td.textContent ?? '').join('\n'));
      expect(cellText).toContain('<script>x</script>');
      expect(cellText).toContain('tom & jerry');
      expect(cellText).toContain('"quoted"');
      expect(cellText).toContain("it's");

      // innerHTML still encodes the chars textContent encodes -- guards
      // against a regression that would set innerHTML from a raw value.
      const cellHtml = await dataViewer.frame.locator('#gridBody .textCell')
        .evaluateAll((tds) => (tds as HTMLElement[]).map((td) => td.innerHTML).join('\n'));
      expect(cellHtml).toContain('&lt;script&gt;');
      expect(cellHtml).toContain('tom &amp; jerry');
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.escape_test_df", envir = .GlobalEnv)',
      );
    }
  });

  // https://github.com/rstudio/rstudio/issues/17800
  //
  // The grid intercepts Ctrl/Cmd+C while the viewport is focused to copy the
  // single active cell. Regression: it did so unconditionally, clobbering a
  // multi-cell native text selection -- so after clicking a cell the user
  // could only ever copy that one cell. The fix steps aside when a non-empty
  // selection exists and lets the browser copy it.
  //
  // The test installs a one-shot `copy` listener in the iframe that records
  // window.getSelection().toString(). When the keystroke is correctly handed
  // to the browser, that listener fires with the full selection; with the old
  // behavior the keydown was preventDefault()ed and no `copy` event fired at
  // all, so the recorded text stays null and the poll below times out.
  test('Ctrl+C copies a multi-cell text selection, not just the active cell', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '{ .rs.copy_test_df <- data.frame(a = c("alpha", "bravo"), b = c("charlie", "delta"), stringsAsFactors = FALSE); View(.rs.copy_test_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Click a value cell first: this sets the active cell and focuses the
      // viewport, which is the precondition for the grid's keydown handler to
      // intercept Ctrl+C in the first place (and matches the user's report of
      // "the cell that I initially clicked on").
      await dataViewer.frame.locator('#gridBody tr[data-row="0"] td.textCell').first().click();

      // Select the whole first data row natively, then arm a copy listener.
      await page.evaluate((sel: string) => {
        const f = document.querySelector(sel) as HTMLIFrameElement | null;
        const win = f?.contentWindow as (Window & { __dvCopyText?: string | null }) | null;
        const doc = f?.contentDocument;
        if (!win || !doc) throw new Error('data viewer iframe not accessible');
        const tr = doc.querySelector('#gridBody tr[data-row="0"]');
        if (!tr) throw new Error('first data row not rendered');
        const selection = win.getSelection();
        if (!selection) throw new Error('no Selection object');
        selection.removeAllRanges();
        const range = doc.createRange();
        range.selectNodeContents(tr);
        selection.addRange(range);
        win.__dvCopyText = null;
        doc.addEventListener(
          'copy',
          () => { win.__dvCopyText = win.getSelection()?.toString() ?? ''; },
          { once: true },
        );
      }, VIEWER_FRAME);

      // ControlOrMeta so the platform copy accelerator fires the browser's
      // native copy (Cmd+C on macOS, Ctrl+C elsewhere); the grid handler
      // accepts either modifier too.
      await page.keyboard.press('ControlOrMeta+c');

      // The native copy should have captured both cells from row 0.
      await expect.poll(
        () => page.evaluate((sel: string) => {
          const f = document.querySelector(sel) as HTMLIFrameElement | null;
          const win = f?.contentWindow as (Window & { __dvCopyText?: string | null }) | null;
          return win?.__dvCopyText ?? null;
        }, VIEWER_FRAME),
        { timeout: TIMEOUTS.fileOpen, message: 'native copy of the multi-cell selection never fired' },
      ).toContain('alpha');

      const copied = await page.evaluate((sel: string) => {
        const f = document.querySelector(sel) as HTMLIFrameElement | null;
        const win = f?.contentWindow as (Window & { __dvCopyText?: string | null }) | null;
        return win?.__dvCopyText ?? '';
      }, VIEWER_FRAME);
      expect(copied).toContain('charlie');
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.copy_test_df", envir = .GlobalEnv)',
      );
    }
  });

  // Regression from the column-virtualization work (#17812): getActiveCellTd
  // located the active cell by the rsGridCell_<row>_<col> id, but buildRow
  // assigns that id only to the cell that is *already* active. So clicking (or
  // arrow-keying to) a fresh cell set activeRow/activeCol logically, but the
  // lookup couldn't find the cell to mark and .activeCell was never applied --
  // the highlight only appeared once a later render pass (a scroll) rebuilt the
  // row. Column headers were unaffected (looked up by index). Asserting on the
  // class -- not the painted outline -- mirrors the actual defect and avoids
  // depending on focus/compositing state.
  test('clicking or arrow-keying a cell applies the active-cell highlight immediately', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '{ .rs.active_cell_df <- data.frame(a = c("alpha", "bravo"), b = c("charlie", "delta"), stringsAsFactors = FALSE); View(.rs.active_cell_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Click a value cell in the first data row. The cell is already
      // rendered and on screen, so nothing scrolls -- the regression was that
      // the class only landed after a scroll re-rendered the row.
      const firstRowCell = dataViewer.frame.locator('#gridBody tr[data-row="0"] td.textCell').first();
      await firstRowCell.click();
      await expect(firstRowCell).toHaveClass(/\bactiveCell\b/, { timeout: TIMEOUTS.fileOpen });

      // Arrow-down moves the highlight to the next row's cell and clears it
      // from the first, confirming setActiveCell tracks the live cell on
      // keyboard navigation too (clicking focused the viewport).
      await page.keyboard.press('ArrowDown');
      const secondRowCell = dataViewer.frame.locator('#gridBody tr[data-row="1"] td.textCell').first();
      await expect(secondRowCell).toHaveClass(/\bactiveCell\b/, { timeout: TIMEOUTS.fileOpen });
      await expect(firstRowCell).not.toHaveClass(/\bactiveCell\b/);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.active_cell_df", envir = .GlobalEnv)',
      );
    }
  });

  // https://github.com/rstudio/rstudio/issues/17800
  //
  // The grid uses auto-hide overlay scrollbars that fade after ~1.2s of
  // inactivity and only reappear on a scroll event. Regression: returning to
  // the data viewer tab re-ran layout but never re-showed the bars, so the
  // horizontal scrollbar stayed hidden (and horizontal scrolls rarely happen
  // via the wheel, so it was effectively gone). The fix calls showScrollbars()
  // from the tab-activation hook.
  test('horizontal scrollbar reappears after returning to the data viewer tab', async ({ rstudioPage: page }) => {
    // 60 columns guarantees the grid overflows its viewport horizontally
    // regardless of the source pane width in CI.
    await consoleActions.executeInConsole(
      '{ .rs.wide_test_df <- as.data.frame(matrix(seq_len(300), nrow = 5, ncol = 60)); View(.rs.wide_test_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Precondition: the grid is actually horizontally scrollable.
      await expect.poll(
        () => dataViewer.viewport.evaluate(
          (el: HTMLElement) => el.scrollWidth - el.clientWidth,
        ),
        { message: 'wide data frame should overflow the viewport horizontally' },
      ).toBeGreaterThan(1);

      // Let the initial activity-based show fade out so the next assertion is
      // meaningful: the bar must be hidden before we switch back to it.
      await expect(dataViewer.horizontalScrollbar)
        .not.toHaveClass(/\bvisible\b/, { timeout: 2500 });

      // Switch away to the kept Untitled tab, then back to the viewer tab.
      // Selecting the viewer tab fires the data viewer's onActivate hook.
      const sourceTabs = page.locator("[class*='rstudio_source_panel'] .gwt-TabLayoutPanelTab");
      await sourceTabs.filter({ hasText: 'Untitled' }).first().click();
      await sourceTabs.filter({ hasText: '.rs.wide_test_df' }).first().click();

      // The bar is shown again on activation (it fades after ~1.2s, but
      // toHaveClass polls fast enough to observe the visible window).
      await expect(dataViewer.horizontalScrollbar).toHaveClass(/\bvisible\b/);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.wide_test_df", envir = .GlobalEnv)',
      );
    }
  });

  // Adding a column flips the server-side column fingerprint, which discards
  // the saved per-object UI state (it can no longer be trusted against the new
  // column structure). The live sidebar choice must NOT be discarded with it:
  // before the fix, the structure-changed refresh re-applied the
  // data_viewer_show_summary preference default, silently re-opening a summary
  // sidebar the user had just dismissed.
  test('dismissed summary sidebar stays dismissed when a column is added', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '{ .rs.sidebar_keep_df <- as.data.frame(matrix(0L, nrow = 10, ncol = 5)); View(.rs.sidebar_keep_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      const sidebarPanel = dataViewer.frame.locator('#sidebarPanel');
      const sidebarToggle = page.locator('#data_editing_toolbar .rstudio_dt_sidebar_toggle');

      // The summary sidebar opens by default (data_viewer_show_summary).
      await expect(sidebarPanel).toHaveClass(/\bexpanded\b/, { timeout: TIMEOUTS.fileOpen });

      // Dismiss it via the toolbar toggle; the latch reflects the new state
      // once the iframe fires sidebarStateCallback back at the host.
      await sidebarToggle.click();
      await expect(sidebarPanel).not.toHaveClass(/\bexpanded\b/, { timeout: TIMEOUTS.fileOpen });
      await expect(sidebarToggle).toHaveAttribute('aria-pressed', 'false');

      // Add a column: the structure-changed refresh re-bootstraps the grid
      // with a new column fingerprint, discarding the saved UI state.
      await consoleActions.executeInConsole('.rs.sidebar_keep_df$added <- 1L');
      await expect(dataViewer.gridInfo)
        .toContainText('6 total columns', { timeout: TIMEOUTS.fileOpen });

      // The sidebar stays dismissed -- it must not snap back to the
      // preference default. The host latch agrees.
      await expect(sidebarPanel).not.toHaveClass(/\bexpanded\b/);
      await expect(sidebarToggle).toHaveAttribute('aria-pressed', 'false');
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.sidebar_keep_df", envir = .GlobalEnv)',
      );
    }
  });

  test('HTML-special column names render as text, not markup', async () => {
    await consoleActions.executeInConsole(
      '{ .rs.escape_hdr_df <- data.frame(x = 1, check.names = FALSE); names(.rs.escape_hdr_df) <- "<b>&\\"\'"; View(.rs.escape_hdr_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      const th = dataViewer.frame.locator('th[data-col-idx="1"]');

      // No <b> element should have appeared in the header from the column
      // name alone -- if escaping regressed, this query would find one.
      await expect(th.locator('b')).toHaveCount(0);

      // textContent of the header matches the original column name verbatim.
      expect(await th.textContent()).toContain('<b>&"\'');

      // innerHTML still encodes <, >, and & -- guards against a regression
      // that sets innerHTML directly from a raw column name.
      const html = await th.innerHTML();
      expect(html).toContain('&lt;b&gt;');
      expect(html).toContain('&amp;');
      expect(html).not.toContain('<b>');
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.escape_hdr_df", envir = .GlobalEnv)',
      );
    }
  });
});

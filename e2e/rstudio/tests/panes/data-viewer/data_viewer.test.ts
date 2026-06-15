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

    // The pin icon is opacity:0 by default, which still passes Playwright's
    // actionability visibility check (only display:none / visibility:hidden
    // / an empty box fail it), so a locator click lands.
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

  // The summary sidebar mirrors the grid header's pin and sort affordances:
  // its icons both reflect state changes made in the grid and drive the same
  // toggle/cycle paths themselves.
  test('sidebar pin and sort icons mirror and control column state', async () => {
    await consoleActions.executeInConsole('View(mtcars)');
    await waitForViewer(dataViewer);

    const sidebarSort = dataViewer.frame
      .locator('.sidebar-col[data-col-idx="1"] .sidebar-sort-icon');
    const sidebarPin = dataViewer.frame
      .locator('.sidebar-col[data-col-idx="3"] .sidebar-pin-icon');

    // Sort from the sidebar: the grid header and the sidebar icon both
    // step through asc, then desc.
    await sidebarSort.click();
    await expect.poll(() => colHeaderClass(dataViewer, 1)).toMatch(/sorting_asc/);
    await expect(sidebarSort).toHaveClass(/sorting_asc/);

    await sidebarSort.click();
    await expect.poll(() => colHeaderClass(dataViewer, 1)).toMatch(/sorting_desc/);
    await expect(sidebarSort).toHaveClass(/sorting_desc/);

    // The sort actually re-fetched the rows, not just flipped the arrows:
    // mtcars' maximum mpg leads the grid when descending.
    await expect(dataViewer.frame.locator('#gridBody td[data-col-pos="1"]').first())
      .toHaveText('33.9');

    // Clearing the sort from the grid header (third cycle step) syncs back
    // into the sidebar icon.
    await dataViewer.frame.locator('th[data-col-idx="1"]').click();
    await expect(sidebarSort).not.toHaveClass(/sorting_(asc|desc)/);

    // Pin from the sidebar: the column moves to the pinned prefix and the
    // sidebar icon flips to its pinned state. (The icon is opacity:0 until
    // hover, which still passes Playwright's actionability visibility
    // check -- only display:none / visibility:hidden / an empty box fail it.)
    await sidebarPin.click();
    await expect.poll(() => colHeaderClass(dataViewer, 3)).toMatch(/\bpinned\b/);
    expect((await colOrder(dataViewer)).slice(0, 3)).toEqual(['0', '3', '1']);
    await expect(sidebarPin).toHaveClass(/\bpinned\b/);
    await expect(sidebarPin).toHaveAttribute('aria-pressed', 'true');

    // Unpin from the grid header: the sidebar icon follows.
    await dataViewer.frame.locator('th[data-col-idx="3"] .pin-icon').click();
    await expect.poll(() => colHeaderClass(dataViewer, 3)).not.toMatch(/\bpinned\b/);
    await expect(sidebarPin).not.toHaveClass(/\bpinned\b/);
    await expect(sidebarPin).toHaveAttribute('aria-pressed', 'false');
  });

  // The sidebar sort icon must work for a column whose <th> is virtualized
  // out of the rendered window: handleSortClick keys off column state, not
  // the rendered header (which the pre-sidebar implementation required).
  test('sidebar sorts a column whose header is virtualized out of the grid', async () => {
    // 300 columns so a full horizontal scroll evicts column 1's header from
    // the DOM (same harness as the #17806 filter test). V1 holds descending
    // values so an ascending sort visibly reverses the row order.
    await consoleActions.executeInConsole(
      '{ .rs.sidebar_virt_df <- as.data.frame(matrix(1:30000, nrow = 100, ncol = 300)); .rs.sidebar_virt_df[[1]] <- 100:1; View(.rs.sidebar_virt_df) }',
    );
    try {
      await waitForViewer(dataViewer);
      await expect(dataViewer.gridInfo).toContainText('100', { timeout: TIMEOUTS.fileOpen });

      // Scroll the rendered column window right far enough that column 1's
      // header is evicted from the DOM, but not so far that the viewport
      // leaves the fetched window (which would slide it and re-key the
      // sidebar). Its sidebar entry remains either way.
      await dataViewer.viewport.evaluate((el) => { el.scrollLeft = 2000; });
      await expect(dataViewer.frame.locator('th[data-col-idx="1"]'))
        .toHaveCount(0, { timeout: TIMEOUTS.fileOpen });

      // Anchor the row-order assertions on whichever data column is rendered
      // at the scrolled-to position (the exact column depends on measured
      // widths). With no pins a cell's data-col-pos equals its column index
      // k, and Vk holds (k-1)*100+1 .. k*100 in natural row order.
      const row0 = dataViewer.frame.locator('#gridBody tr[data-row="0"]');
      await expect(row0).toBeVisible();
      const posAttr = await row0
        .locator('td[data-col-pos]:not([data-col-pos="0"])')
        .last()
        .getAttribute('data-col-pos');
      const k = parseInt(posAttr ?? '0', 10);
      expect(k).toBeGreaterThan(1);
      const anchorCell = row0.locator(`td[data-col-pos="${k}"]`);
      await expect(anchorCell).toHaveText(String((k - 1) * 100 + 1));

      // Sort ascending from the sidebar: the icon reflects the new state
      // with no <th> for the column in the document, and the rows actually
      // reorder (V1 descends, so ascending brings the last row to the top).
      const sidebarSort = dataViewer.frame
        .locator('.sidebar-col[data-col-idx="1"] .sidebar-sort-icon');
      await sidebarSort.click();
      await expect(sidebarSort).toHaveClass(/sorting_asc/);
      await expect(anchorCell).toHaveText(String(k * 100), { timeout: TIMEOUTS.fileOpen });

      // Scroll back: the recreated header carries the sort state.
      await dataViewer.viewport.evaluate((el) => { el.scrollLeft = 0; });
      await expect(dataViewer.frame.locator('th[data-col-idx="1"]'))
        .toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect.poll(() => colHeaderClass(dataViewer, 1)).toMatch(/sorting_asc/);
    } finally {
      await consoleActions.executeInConsole('rm(".rs.sidebar_virt_df", envir = .GlobalEnv)');
    }
  });

  // The sidebar footer is uniform across entries: numeric columns show the
  // actual data range on the left (the sparkline has no axis ticks), and
  // every column shows an NA percentage on the right, dimmed (class "zero")
  // when the column has no missing values.
  test('sidebar footer shows the numeric range and a uniform NA stat', async () => {
    await consoleActions.executeInConsole(
      '{ .rs.sidebar_footer_df <- data.frame(num = c(1.5, NA, 4.2, 3), chr = letters[1:4]); View(.rs.sidebar_footer_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      const numEntry = dataViewer.frame.locator('.sidebar-col[data-col-idx="1"]');
      const chrEntry = dataViewer.frame.locator('.sidebar-col[data-col-idx="2"]');

      // Numeric column: finite data range on the left, 25% NA on the right.
      await expect(numEntry.locator('.sidebar-col-summary')).toHaveText('[1.5, 4.2]');
      const numNa = numEntry.locator('.sidebar-col-na');
      await expect(numNa).toHaveText('25% NA');
      await expect(numNa).not.toHaveClass(/\bzero\b/);

      // Character column: distinct-value count on the left, and the NA stat
      // is still rendered (uniform footer), dimmed via the "zero" class.
      await expect(chrEntry.locator('.sidebar-col-summary')).toHaveText('4 unique');
      const chrNa = chrEntry.locator('.sidebar-col-na');
      await expect(chrNa).toHaveText('0% NA');
      await expect(chrNa).toHaveClass(/\bzero\b/);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.sidebar_footer_df", envir = .GlobalEnv)',
      );
    }
  });

  // The sidebar summaries describe the rows currently in view: applying a
  // filter or search recomputes the histograms, ranges, and NA stats over the
  // filtered subset (and a "(filtered)" tag appears), reverting when cleared.
  test('sidebar summaries reflect the filtered rows, not the whole frame', async ({ rstudioPage: page }) => {
    // alpha rows have num 1..4 (no NA); beta rows include the only NA. So a
    // search that keeps only the alpha rows shifts num's range 1..80 -> 1..4
    // and its NA 13% -> 0% -- both visible, both computed over filtered rows.
    await consoleActions.executeInConsole(
      '{ .rs.sidebar_filt_df <- data.frame(' +
        'grp = rep(c("alpha", "beta"), each = 4), ' +
        'num = c(1, 2, 3, 4, 50, 60, NA, 80)); View(.rs.sidebar_filt_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Read the num column's sidebar summary from the LIVE data-browser
      // document via the viewport element. During a View() tab swap two
      // iframes briefly share the "Data Browser" title, and a plain
      // frameLocator can resolve to the outgoing (stale) one; going through
      // the viewport's ownerDocument pins reads to the frame actually in view.
      const readNum = () => dataViewer.viewport.evaluate((vp) => {
        const doc = vp.ownerDocument;
        const sum = doc.querySelector('.sidebar-col[data-col-idx="2"] .sidebar-col-summary');
        const na = doc.querySelector('.sidebar-col[data-col-idx="2"] .sidebar-col-na');
        return {
          summary: sum ? sum.textContent : null,
          na: na ? na.textContent : null,
          naZero: !!(na && na.classList.contains('zero')),
          filtered: !!doc.querySelector('.sidebar-toggle-filtered'),
        };
      });

      // Whole-frame summary to start: full range, NA over all 8 rows, no tag.
      await expect.poll(async () => (await readNum()).summary, { timeout: TIMEOUTS.fileOpen })
        .toBe('[1, 80]');
      await expect.poll(async () => (await readNum()).na, { timeout: TIMEOUTS.fileOpen })
        .toBe('13% NA');
      expect((await readNum()).filtered).toBe(false);

      // Search to the alpha rows. (getByLabel reaches the search input via its
      // hidden "Search data table" label, regardless of GWT class names.)
      const search = page.locator('#data_editing_toolbar').getByLabel('Search data table');
      await search.click();
      await page.keyboard.type('alpha');

      // The grid confirms the filter landed; the sidebar then describes only
      // the 4 matching rows: range 1..4, no missing values, with the tag shown.
      await expect(dataViewer.gridInfo).toContainText('filtered from', { timeout: TIMEOUTS.fileOpen });
      await expect.poll(async () => (await readNum()).summary, { timeout: TIMEOUTS.fileOpen })
        .toBe('[1, 4]');
      const filtered = await readNum();
      expect(filtered.na).toBe('0% NA');
      expect(filtered.naZero).toBe(true);
      expect(filtered.filtered).toBe(true);

      // Clearing the search reverts the summaries to the whole frame.
      for (let i = 0; i < 'alpha'.length; i++)
        await search.press('Backspace');
      await expect.poll(async () => (await readNum()).summary, { timeout: TIMEOUTS.fileOpen })
        .toBe('[1, 80]');
      const reverted = await readNum();
      expect(reverted.na).toBe('13% NA');
      expect(reverted.filtered).toBe(false);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.sidebar_filt_df", envir = .GlobalEnv)',
      );
    }
  });

  // A go-to-column jump scrolls the target's sidebar entry into view (the
  // inverse of clicking a sidebar entry, which scrolls the grid), without
  // moving keyboard focus off the grid.
  test('go to column scrolls the sidebar entry into view, keeping grid focus', async () => {
    // Enough columns that the sidebar list overflows -- a late column's entry
    // starts well below the fold.
    await consoleActions.executeInConsole(
      '{ .rs.sidebar_goto_df <- as.data.frame(matrix(1:600, nrow = 10, ncol = 60)); View(.rs.sidebar_goto_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Fractional position of column `idx`'s sidebar entry center within the
      // scrolled viewport of the (live) sidebar list: < 0 above the fold, > 1
      // below it, ~0.5 centered. Read through the viewport's own document to
      // pin to the frame in view (see the filtered-summary test).
      const entryCenterRatio = (idx: number) => dataViewer.viewport.evaluate((vp, i) => {
        const doc = vp.ownerDocument;
        const content = doc.getElementById('sidebarContent');
        const entry = doc.querySelector(`.sidebar-col[data-col-idx="${i}"]`) as HTMLElement | null;
        if (!content || !entry || content.clientHeight === 0) return null;
        const center = entry.offsetTop + entry.offsetHeight / 2;
        return (center - content.scrollTop) / content.clientHeight;
      }, idx);

      // Column 40 (mid-list) starts below the fold. The sidebar is virtualized,
      // so a below-fold entry isn't built at all -- its absence is how we know
      // it's off-screen (entryCenterRatio returns null when the entry is absent).
      await expect(dataViewer.frame.locator('.sidebar-col[data-col-idx="40"]'))
        .toHaveCount(0, { timeout: TIMEOUTS.fileOpen });

      // Jump to it via the go-to-column box.
      await dataViewer.goToColumn(40);

      // Its sidebar entry scrolls to roughly the center of the list (not flush
      // against the bottom edge, which "nearest" alignment would produce).
      await expect.poll(() => entryCenterRatio(40), { timeout: TIMEOUTS.fileOpen })
        .toBeLessThan(0.7);
      expect(await entryCenterRatio(40)).toBeGreaterThan(0.3);

      // ...but focus stays on the grid viewport (so arrow keys drive the
      // data), not anywhere in the sidebar.
      const focusOnGrid = await dataViewer.viewport.evaluate(
        (vp) => vp.ownerDocument.activeElement === vp);
      expect(focusOnGrid).toBe(true);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.sidebar_goto_df", envir = .GlobalEnv)',
      );
    }
  });

  // Regression: the virtualized sidebar replaces all visible entries on every
  // scroll-driven render. With browser scroll anchoring enabled, that wholesale
  // content shift made the browser re-adjust scrollTop, which re-fired the
  // scroll handler -- a self-sustaining "runaway scroll" that kept paging
  // through columns after the user stopped. #sidebarContent now sets
  // overflow-anchor:none. Only real wheel events reproduce it (a programmatic
  // scrollTop set does not), so drive the wheel and assert the scroll settles.
  test('summary sidebar scroll settles instead of running away', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '{ .rs.sidebar_scroll_df <- as.data.frame(matrix(1:6000, nrow = 10, ncol = 600)); View(.rs.sidebar_scroll_df) }',
    );
    try {
      await waitForViewer(dataViewer);
      const content = dataViewer.frame.locator('#sidebarContent');
      await expect(content).toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // Wheel over the sidebar, then confirm it scrolled.
      const box = await content.boundingBox();
      if (!box) throw new Error('sidebar content has no bounding box');
      await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
      await page.mouse.wheel(0, 600);
      const readTop = () => content.evaluate((el) => el.scrollTop);
      await expect.poll(readTop, { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);

      // It must SETTLE: two reads a few hundred ms apart are identical. With the
      // anchoring loop, scrollTop kept changing on its own and these differ.
      await page.waitForTimeout(500);
      const t1 = await readTop();
      await page.waitForTimeout(400);
      const t2 = await readTop();
      expect(t2).toBe(t1);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.sidebar_scroll_df", envir = .GlobalEnv)',
      );
    }
  });

  // The sidebar is a complete index: it lists every column of a wide frame
  // (not just the fetched window), lazy-loads an off-window column's summary
  // when it scrolls into view, and lets you pin/sort an off-window column
  // straight from its entry.
  test('sidebar lists all columns of a wide frame and acts on off-window ones', async () => {
    // 300 columns: wider than the ~200-column fetched window, so late columns
    // are off-window. Column 250 holds a known range for the summary check.
    await consoleActions.executeInConsole(
      '{ .rs.sidebar_all_df <- as.data.frame(matrix(1:3000, nrow = 10, ncol = 300)); View(.rs.sidebar_all_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // The header reports the frame total once the complete index loads.
      await expect(dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-label'))
        .toHaveText('300 columns', { timeout: TIMEOUTS.fileOpen });

      // The sidebar is virtualized, so a far-off column's entry isn't built
      // until it scrolls into the window. Scroll the list to column 250 (entry
      // height from the CSS var; the rowname is excluded so abs N sits at index
      // N-1), after which its entry exists.
      const scrollSidebarToCol = (abs: number) => dataViewer.viewport.evaluate((vp, a) => {
        const doc = vp.ownerDocument!;
        const content = doc.getElementById('sidebarContent');
        if (!content) return;
        const h = parseInt(
          getComputedStyle(doc.documentElement).getPropertyValue('--sidebar-entry-height'),
          10) || 78;
        content.scrollTop = (a - 1) * h;
      }, abs);
      await scrollSidebarToCol(250);
      const entry250 = dataViewer.frame.locator('.sidebar-col[data-col-idx="250"]');
      await expect(entry250).toHaveCount(1, { timeout: TIMEOUTS.fileOpen });

      // Its summary lazy-loads once the entry is built. matrix(1:3000) is
      // column-major with 10 rows, so column 250 holds 2491..2500.
      await expect(entry250.locator('.sidebar-col-summary'))
        .toHaveText('[2,491, 2,500]', { timeout: TIMEOUTS.fileOpen });

      // Sort descending from the off-window entry (two clicks). Sorting alone
      // doesn't scroll the column into view, so confirm via the entry's icon,
      // then jump to the column and confirm the grid actually reordered: 2500
      // (its max) lands in the first row.
      const sortIcon = entry250.locator('.sidebar-sort-icon');
      await sortIcon.click();
      await sortIcon.click();
      await expect(sortIcon).toHaveClass(/sorting_desc/, { timeout: TIMEOUTS.fileOpen });

      await dataViewer.goToColumn(250);
      await expect(dataViewer.columnHeader(250)).toBeVisible({ timeout: 15000 });
      const headerPos = await dataViewer.columnHeader(250).getAttribute('data-col-idx');
      await expect(
        dataViewer.frame.locator(`#gridBody tr[data-row="0"] td[data-col-pos="${headerPos}"]`),
      ).toHaveText('2500', { timeout: 15000 });

      // Pin the off-window-origin column from its (now rebuilt) entry; it
      // becomes pinned (sticky) and the entry's pin icon reflects it.
      const pinIcon = dataViewer.frame.locator('.sidebar-col[data-col-idx="250"] .sidebar-pin-icon');
      await pinIcon.click();
      await expect(pinIcon).toHaveClass(/pinned/, { timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.frame.locator('th[data-col-idx] .pin-icon.pinned'))
        .toHaveCount(1, { timeout: 15000 });
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.sidebar_all_df", envir = .GlobalEnv)',
      );
    }
  });

  // Low-cardinality factor / character columns get a categorical frequency
  // sparkline (one bar per distinct value, capped server-side at 24 bars);
  // above the cutoff the sidebar falls back to a text summary with the
  // dominant value -- omitted for ID-like columns where every value is
  // distinct and "top" would be noise.
  test('sidebar shows category bars below the cutoff and a text summary above it', async () => {
    await consoleActions.executeInConsole(
      '{ .rs.sidebar_cat_df <- data.frame(' +
        'fct = factor(rep(c("aa", "bb", "cc"), each = 10)), ' +
        'rpt = c(rep("dom", 6), sprintf("v%02d", 1:24)), ' +
        'ids = sprintf("id%02d", 1:30)); ' +
        'View(.rs.sidebar_cat_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      const fctEntry = dataViewer.frame.locator('.sidebar-col[data-col-idx="1"]');
      const rptEntry = dataViewer.frame.locator('.sidebar-col[data-col-idx="2"]');
      const idsEntry = dataViewer.frame.locator('.sidebar-col[data-col-idx="3"]');

      // 3 levels: frequency bars, with the level count kept in the footer.
      await expect(fctEntry.locator('.sidebar-sparkline canvas')).toBeVisible();
      await expect(fctEntry.locator('.sidebar-col-summary')).toHaveText('3 levels');

      // 25 distinct values (> 24): no sparkline; the text summary names the
      // dominant value with its share of all rows (6 of 30). The entry keeps an
      // empty reserved sparkline slot (constant height for the virtualizer), so
      // assert there is no drawn canvas in it rather than no slot at all.
      await expect(rptEntry.locator('.sidebar-sparkline canvas')).toHaveCount(0);
      await expect(rptEntry.locator('.sidebar-col-summary'))
        .toHaveText('25 unique \u00b7 top: dom (20%)');

      // All-distinct (ID-like): cardinality only, no "top".
      await expect(idsEntry.locator('.sidebar-sparkline canvas')).toHaveCount(0);
      await expect(idsEntry.locator('.sidebar-col-summary')).toHaveText('30 unique');
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.sidebar_cat_df", envir = .GlobalEnv)',
      );
    }
  });

  // The sidebar header's "?" opens a dialog explaining the summary entries.
  // Opening it must not collapse the panel (the icon stops propagation to
  // the header toggle, unlike the decorative close glyph), and Escape
  // dismisses it with the panel still expanded.
  test('sidebar help icon opens the explainer dialog without collapsing the panel', async () => {
    await consoleActions.executeInConsole('View(mtcars)');
    await waitForViewer(dataViewer);

    const panel = dataViewer.frame.locator('#sidebarPanel');
    await expect(panel).toHaveClass(/\bexpanded\b/, { timeout: TIMEOUTS.fileOpen });

    const dialog = dataViewer.frame.locator('.sidebar-help-dialog');
    await dataViewer.frame.locator('#sidebarToggle .sidebar-toggle-help').click();
    await expect(dialog).toBeVisible();
    await expect(panel).toHaveClass(/\bexpanded\b/);

    // The dialog took focus on open, so Escape lands on it and dismisses.
    await dialog.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(panel).toHaveClass(/\bexpanded\b/);
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

      // The restored state also re-applies to the rebuilt sidebar (the
      // trailing updateSidebarColumnIndicators call in initSidebar).
      await expect(dataViewer.frame
        .locator('.sidebar-col[data-col-idx="1"] .sidebar-sort-icon'))
        .toHaveClass(/sorting_desc/);
      await expect(dataViewer.frame
        .locator('.sidebar-col[data-col-idx="3"] .sidebar-pin-icon'))
        .toHaveClass(/\bpinned\b/);
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

  // Reset View must also return to the FIRST COLUMN on a wide frame. After a
  // horizontal scroll slides the fetched column window (columnOffset > 0), Reset
  // View clears the scroll position; if it didn't also reset columnOffset, the
  // window would stay parked far right while the viewport sat at scrollLeft 0
  // over the blank left spacer span -- a blank grid until the user scrolled.
  test('Reset View returns to the first column on a wide frame', async ({ rstudioPage: page }) => {
    // 500 columns exceeds the ~200-column fetched window, so scrolling slides it.
    await consoleActions.executeInConsole(
      '{ .rs.reset_view_wide_df <- as.data.frame(matrix(1:5000, nrow = 10, ncol = 500)); View(.rs.reset_view_wide_df) }',
    );
    try {
      await waitForViewer(dataViewer);
      await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: 15000 });

      // Scroll fully right so the window slides far from column 1.
      await dataViewer.viewport.evaluate((el) => { el.scrollLeft = el.scrollWidth; });
      await expect(dataViewer.columnHeader(500)).toBeVisible({ timeout: 15000 });

      // Reset View (the same iframe method the toolbar option invokes).
      await page.evaluate((sel: string) => {
        const f = document.querySelector(sel) as HTMLIFrameElement | null;
        const w = f?.contentWindow as unknown as { refreshAndReset?: () => void } | undefined;
        if (!w?.refreshAndReset) throw new Error('refreshAndReset() not available on data viewer iframe');
        w.refreshAndReset();
      }, VIEWER_FRAME);

      // The grid rebuilds back at column 1 with real data visible -- not a blank
      // left-spacer band. V1's first row holds 1 (matrix is column-major).
      await waitForViewer(dataViewer);
      await expect(dataViewer.columnHeader(1)).toBeVisible({ timeout: 15000 });
      await expect(
        dataViewer.frame.locator('#gridBody tr[data-row="0"] td[data-col-pos="1"]'),
      ).toHaveText('1', { timeout: 15000 });
      await expect.poll(
        () => dataViewer.viewport.evaluate((el: HTMLElement) => el.scrollLeft),
      ).toBe(0);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.reset_view_wide_df", envir = .GlobalEnv)',
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

  // https://github.com/rstudio/rstudio/issues/17958
  //
  // Two regressions from the vanilla-JS grid rewrite: (1) End/Ctrl+End jumped
  // to the last *column* (Excel semantics) instead of scrolling to the bottom
  // of the current column like the pre-rewrite viewer; (2) the bottom-edge
  // math in ensureActiveCellVisible used the raw viewport clientHeight,
  // ignoring the sticky header inside it, so the jump landed a header-height
  // short and the target row stayed hidden below the fold.
  test('End scrolls to the last row of the current column, fully visible (#17958)', async ({ rstudioPage: page }) => {
    // 500 rows forces vertical virtual scrolling; 30 columns force horizontal
    // overflow so a wrong jump-to-last-column would visibly move scrollLeft.
    await consoleActions.executeInConsole(
      '{ .rs.ctrl_end_df <- as.data.frame(matrix(seq_len(500 * 30), nrow = 500)); View(.rs.ctrl_end_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Click the first data cell: sets the active cell to (0, 1) and focuses
      // the viewport so the grid keydown handler receives the End press.
      await dataViewer.frame.locator('#gridBody tr[data-row="0"] td.numberCell').first().click();
      await expect(dataViewer.frame.locator('#rsGridCell_0_1')).toHaveClass(/\bactiveCell\b/);

      await page.keyboard.press('ControlOrMeta+End');

      // The active cell lands on the last row of the *same* column -- not the
      // bottom-right corner -- and the viewport does not scroll horizontally.
      const lastCell = dataViewer.frame.locator('#rsGridCell_499_1');
      await expect(lastCell).toHaveClass(/\bactiveCell\b/, { timeout: TIMEOUTS.fileOpen });
      expect(await dataViewer.viewport.evaluate((el) => el.scrollLeft)).toBe(0);

      // The last row must be *fully* visible; before the fix the scroll
      // stopped a header-height short, leaving it below the bottom edge.
      const viewportBox = await dataViewer.viewport.boundingBox();
      const cellBox = await lastCell.boundingBox();
      expect(viewportBox).not.toBeNull();
      expect(cellBox).not.toBeNull();
      expect(cellBox!.y + cellBox!.height)
        .toBeLessThanOrEqual(viewportBox!.y + viewportBox!.height + 0.5);

      // The info bar's "Showing X to Y" range shares visibleBodyHeight with
      // the scroll math; at max scroll it must count the last row in view.
      await expect(dataViewer.gridInfo)
        .toContainText('to 500 of 500', { timeout: TIMEOUTS.fileOpen });

      // Home returns to the top of the same column.
      await page.keyboard.press('Home');
      await expect(dataViewer.frame.locator('#rsGridCell_0_1'))
        .toHaveClass(/\bactiveCell\b/, { timeout: TIMEOUTS.fileOpen });
      await expect.poll(() => dataViewer.viewport.evaluate((el) => el.scrollTop)).toBe(0);

      // Plain End (no modifier) behaves identically -- it was the keystroke
      // in the original report.
      await page.keyboard.press('End');
      await expect(lastCell).toHaveClass(/\bactiveCell\b/, { timeout: TIMEOUTS.fileOpen });
      expect(await dataViewer.viewport.evaluate((el) => el.scrollLeft)).toBe(0);
    } finally {
      await consoleActions.executeInConsole(
        'rm(".rs.ctrl_end_df", envir = .GlobalEnv)',
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

  // The saved-state fingerprint covers column types and factor levels, not
  // just names. A column changing type with unchanged names must discard the
  // saved per-object state: a numeric filter restored onto a now-character
  // column would otherwise be applied with the wrong semantics server-side
  // (is.finite() on character is all-FALSE), silently showing an empty grid.
  test('a saved numeric filter is discarded when the column type changes', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '{ .rs.type_change_df <- data.frame(x = 1:20); View(.rs.type_change_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      // Reveal the filter row and apply a numeric range filter to column 1.
      await page.locator('#data_editing_toolbar').getByText('Filter', { exact: true }).click();
      const colFilter = dataViewer.frame.locator('th[data-col-idx="1"] .colFilter');
      await expect(colFilter).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await colFilter.getByText('All').click();

      const numBox = dataViewer.frame.locator('.filterPopup .numValueBox');
      await numBox.fill('5 - 10');
      await numBox.dispatchEvent('change');
      await expect(dataViewer.gridInfo)
        .toContainText('of 6 entries (filtered from 20', { timeout: TIMEOUTS.fileOpen });
      const excludedCell = dataViewer.frame
        .locator('#gridBody td')
        .filter({ hasText: /^15$/ });
      await expect(excludedCell).toHaveCount(0);
      await page.keyboard.press('Escape');
      await expect(dataViewer.frame.locator('.filterPopup')).toHaveCount(0);

      // Change the column's type without renaming it, then refresh in place.
      // (The console mutation may also trigger an automatic refresh; the
      // explicit call just makes the re-bootstrap deterministic.)
      await consoleActions.executeInConsole(
        '.rs.type_change_df$x <- as.character(.rs.type_change_df$x)',
      );
      await page.evaluate((sel: string) => {
        const f = document.querySelector(sel) as HTMLIFrameElement | null;
        const w = f?.contentWindow as unknown as { refreshData?: () => void } | undefined;
        if (!w?.refreshData) throw new Error('refreshData() not available on data viewer iframe');
        w.refreshData();
      }, VIEWER_FRAME);

      // The fingerprint mismatch discards the stale filter: all rows return
      // (including the value the range had excluded) and nothing reports as
      // filtered. Pre-fix, the names-only fingerprint validated and the
      // restored filter emptied the grid.
      await expect(dataViewer.gridInfo)
        .toContainText('of 20 entries', { timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.gridInfo).not.toContainText('filtered from');
      await expect(excludedCell.first()).toBeVisible();
    } finally {
      await consoleActions.executeInConsole('rm(".rs.type_change_df", envir = .GlobalEnv)');
    }
  });

  // initSidebar runs on every bootstrap and re-registers the toggle's
  // listeners on the persistent #sidebarToggle element. Pre-fix it added a
  // fresh closure each time, so after one refresh a single click ran two
  // toggles -- a visible no-op. The in-grid toggle (inside the iframe, as
  // opposed to the host toolbar's latching button) must keep working after
  // a data refresh.
  test('the in-grid Summary toggle still works after a data refresh', async () => {
    await consoleActions.executeInConsole(
      '{ .rs.toggle_df <- as.data.frame(matrix(0L, nrow = 10, ncol = 5)); View(.rs.toggle_df) }',
    );
    try {
      await waitForViewer(dataViewer);

      const sidebarPanel = dataViewer.frame.locator('#sidebarPanel');
      const sidebarToggle = dataViewer.frame.locator('#sidebarToggle');
      await expect(sidebarPanel).toHaveClass(/\bexpanded\b/, { timeout: TIMEOUTS.fileOpen });

      // Add a column: the resulting in-place refresh re-bootstraps the grid
      // (re-running initSidebar); the column count is the completion gate.
      await consoleActions.executeInConsole('.rs.toggle_df$added <- 1L');
      await expect(dataViewer.gridInfo)
        .toContainText('6 total columns', { timeout: TIMEOUTS.fileOpen });

      // One click flips the sidebar exactly once...
      await sidebarToggle.click();
      await expect(sidebarPanel).not.toHaveClass(/\bexpanded\b/);
      await expect(sidebarToggle).toHaveAttribute('aria-expanded', 'false');

      // ...and once more brings it back. The collapsed #sidebarPanel is
      // width:0 / overflow:hidden, so the toggle has no hit target at all --
      // a forced click hit-tests to whatever is topmost at the zero-width
      // box's center, which differs between retina (local) and 1x (CI)
      // displays and landed on the grid panel on CI. Dispatch the click
      // synthetically: what's under test is how many times the listener
      // runs, not the hit target (re-expanding from the UI goes through the
      // host toolbar's Summary button, covered elsewhere).
      await sidebarToggle.dispatchEvent('click');
      await expect(sidebarPanel).toHaveClass(/\bexpanded\b/);
      await expect(sidebarToggle).toHaveAttribute('aria-expanded', 'true');
    } finally {
      await consoleActions.executeInConsole('rm(".rs.toggle_df", envir = .GlobalEnv)');
    }
  });

  // Manual (drag-resized) column widths are keyed by absolute column
  // identity, like pins/sort/filters. Pre-fix they were positional within
  // the fetched window, so paging to the next set of columns re-applied a
  // saved width to whatever column occupied the same position -- and the
  // resized column lost its width.
  test('a manually resized width follows its column across column pagination', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '.rs.width_df <- as.data.frame(matrix(1:4000, nrow = 10, ncol = 400))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(.rs.width_df)');
    try {
      await waitForViewer(dataViewer);
      await expect(dataViewer.gotoColumnInput).toBeVisible();
      // Let the post-first-fetch auto-size pass settle before measuring.
      await expect(dataViewer.gridInfo)
        .toContainText('of 10 entries', { timeout: TIMEOUTS.fileOpen });

      const col1 = dataViewer.frame.locator('th[data-col-idx="1"]');
      const before = (await col1.boundingBox())!;

      // Drag column 1's resize handle 150px to the right.
      const handle = col1.locator('.resizer');
      const hb = (await handle.boundingBox())!;
      await page.mouse.move(hb.x + hb.width / 2, hb.y + hb.height / 2);
      await page.mouse.down();
      await page.mouse.move(hb.x + hb.width / 2 + 150, hb.y + hb.height / 2, { steps: 10 });
      await page.mouse.up();

      await expect.poll(async () => (await col1.boundingBox())!.width)
        .toBeGreaterThan(before.width + 100);
      const resizedWidth = (await col1.boundingBox())!.width;

      // Jump forward: the column occupying the same window position (201)
      // must keep its own auto-sized width, not inherit column 1's. Note
      // data-col-idx is a position within the fetched window, so after the
      // jump the header for column 201 is found by its absolute title.
      await expect(dataViewer.gotoColumnInput).toBeVisible();
      await dataViewer.goToColumn(201);
      // The post-slide width-refinement pass rebuilds the header row, which
      // can momentarily detach the <th> between poll iterations -- treat a
      // null boundingBox as "retry", not an error.
      const col201 = dataViewer.columnHeader(201);
      await expect(col201).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect.poll(async () =>
        (await col201.boundingBox())?.width ?? Number.MAX_SAFE_INTEGER)
        .toBeLessThan(resizedWidth - 50);

      // Jump back: column 1 still carries the manual width.
      await dataViewer.goToColumn(1);
      await expect.poll(async () =>
        (await dataViewer.frame.locator('th[data-col-idx="1"]').boundingBox())?.width ?? 0,
      { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(before.width + 100);
    } finally {
      await consoleActions.executeInConsole('rm(".rs.width_df", envir = .GlobalEnv)');
    }
  });
});

// Data Viewer tests ported from
// src/cpp/tests/automation/testthat/test-automation-data-viewer.R.
//
// Covers the temporary-expression iframe (#14657), the search/filter
// toolbar + viewerLink cell-explorer hop, the three-state column sort
// cycle, the pin-icon column reorder, per-object state persistence
// across a refresh, and HTML-special-character escaping in both cell
// values and column names.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from 'playwright';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { DataViewerPane } from '@pages/data_viewer.page';
import { executeCommand } from '@utils/commands';
import { sleep, TIMEOUTS } from '@utils/constants';

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

async function closeViewerTab(page: Page): Promise<void> {
  await executeCommand(page, 'closeSourceDoc');
  await sleep(TIMEOUTS.settleDelay);
}

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
    await closeViewerTab(page);
  });

  // https://github.com/rstudio/rstudio/pull/14657
  test('viewer opens for a temporary R expression', async ({ rstudioPage: page }) => {
    await consoleActions.typeInConsole('View(subset(mtcars, mpg >= 30))');
    await expect(page.locator(VIEWER_FRAME))
      .toHaveAttribute('src', /gridviewer\.html/, { timeout: TIMEOUTS.fileOpen });
  });

  test('search filter + viewerLink opens an explorer tab for the cell', async ({ rstudioPage: page }) => {
    // data.frame with a list column: the data viewer renders each list
    // entry as a clickable viewerLink that opens the cell explorer.
    await consoleActions.typeInConsole(
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

    // Close the cell-explorer tab; the data viewer tab itself is closed by
    // the afterEach.
    await executeCommand(page, 'closeSourceDoc');
    await sleep(TIMEOUTS.settleDelay);
  });

  test('sort headers cycle through asc, desc, and unsorted', async () => {
    await consoleActions.typeInConsole('View(mtcars)');
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
    await consoleActions.typeInConsole('View(mtcars)');
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

  test('per-object state survives a refresh', async ({ rstudioPage: page }) => {
    // Use a uniquely-named object so localStorage for this viewer can't be
    // contaminated by a previous test that happened to View(mtcars).
    await consoleActions.typeInConsole(
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
      await consoleActions.typeInConsole(
        'rm(".rs.persist_test_df", envir = .GlobalEnv)',
      );
    }
  });

  test('HTML-special cell values render as text, not markup', async () => {
    // textContent encodes <, >, and & but leaves quotes as plain text.
    // The security property is that nothing user-supplied becomes a real
    // DOM element.
    const setup = `{ .rs.escape_test_df <- data.frame(a = c("<script>x</script>", "tom & jerry", "\\"quoted\\"", "it's"), stringsAsFactors = FALSE); View(.rs.escape_test_df) }`;
    await consoleActions.typeInConsole(setup);
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
      await consoleActions.typeInConsole(
        'rm(".rs.escape_test_df", envir = .GlobalEnv)',
      );
    }
  });

  test('HTML-special column names render as text, not markup', async () => {
    await consoleActions.typeInConsole(
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
      await consoleActions.typeInConsole(
        'rm(".rs.escape_hdr_df", envir = .GlobalEnv)',
      );
    }
  });
});

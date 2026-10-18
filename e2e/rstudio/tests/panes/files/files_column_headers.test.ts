// Files pane column header semantics (#19170).
//
// Sortable DataGrid headers used to put role="button" on the <th> itself,
// which replaced its columnheader role, so screen readers saw no column
// headers and the sort direction couldn't be exposed. Each sortable <th> now
// stays a columnheader, carries aria-sort while its column is sorted, and holds
// the sort button. The file-type icon column's header is named by visually
// hidden text.

import { test, expect } from '@fixtures/rstudio.fixture';

import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';

function reversed(direction: string): string {
  return direction === 'ascending' ? 'descending' : 'ascending';
}

test.describe('Files pane column headers (#19170)', () => {
  test('sortable headers are column headers that report their sort direction', async ({ rstudioPage: page }) => {
    await new ConsolePaneActions(page).executeInConsole(
      '.rs.api.filesPaneNavigate(path.expand("~"))',
      { wait: true },
    );
    await executeCommand(page, 'activateFiles');

    const filesPanel = page.locator('#rstudio_workbench_panel_files');
    const nameHeader = filesPanel.getByRole('columnheader', { name: 'Name', exact: true });
    await expect(nameHeader).toBeVisible();
    for (const name of ['File type', 'Size', 'Modified'])
      await expect(filesPanel.getByRole('columnheader', { name, exact: true })).toBeVisible();

    // Each click on the sort button sorts by Name: a newly sorted Name column
    // starts ascending, and a click on the sorted one reverses it. Only the
    // sorted column's header carries aria-sort.
    const sortButton = nameHeader.getByRole('button', { name: 'Name', exact: true });
    for (let i = 0; i < 2; i++) {
      const before = await nameHeader.getAttribute('aria-sort');
      await sortButton.click();
      await expect(nameHeader).toHaveAttribute('aria-sort',
        before === null ? 'ascending' : reversed(before));
      await expect(filesPanel.locator('th[aria-sort]')).toHaveCount(1);
    }
  });
});

// DataGrid renders its header and body as separate <table>s so the header can
// stay fixed while the body scrolls, which read as a headers-only table and a
// headerless data table (#19173). They are now exposed as one ARIA table, and
// the rows that only reserve scroll height for unrendered files are not rows.
test.describe('Files pane table structure (#19173)', () => {
  test('headers and file cells form one table', async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    const dir = 'file.path(tempdir(), "a11y-grid")';

    try {
      // Enough files that the virtualized grid pads for the unrendered ones.
      // local() keeps the setup's variable out of the global environment.
      await consoleActions.executeInConsole(
        `local({ d <- ${dir}; dir.create(d, showWarnings = FALSE); ` +
        'invisible(file.create(file.path(d, sprintf("file-%03d.txt", 1:300)))); ' +
        '.rs.api.filesPaneNavigate(d) })',
        { wait: true },
      );
      await executeCommand(page, 'activateFiles');

      const filesPanel = page.locator('#rstudio_workbench_panel_files');
      const table = filesPanel.getByRole('table');
      await expect(table.getByRole('cell', { name: /^file-\d{3}\.txt$/ }).first()).toBeVisible();
      await expect(table).toHaveCount(1);
      await expect(table).toMatchAriaSnapshot(`
        - table:
          - rowgroup:
            - row:
              - columnheader "Selected"
              - columnheader "File type"
              - columnheader "Name"
              - columnheader "Size"
              - columnheader "Modified"
          - rowgroup:
            - row:
              - cell
              - cell
              - cell /file-\\d{3}\\.txt/
      `);

      // Padding rows reserve scroll height but are not rows: every row the
      // table exposes is a header or file row.
      const paddingRows = filesPanel.locator('tbody tr[height]');
      await expect(paddingRows).not.toHaveCount(0);
      for (const row of await paddingRows.all())
        await expect(row).toHaveAttribute('role', 'presentation');
      const contentRows = table.locator('tr:not([height])');
      expect(await table.getByRole('row').count()).toBe(await contentRows.count());
    } finally {
      await consoleActions.executeInConsole(
        `unlink(${dir}, recursive = TRUE); .rs.api.filesPaneNavigate(path.expand("~"))`,
        { wait: true },
      );
    }
  });
});

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

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
import { useSuiteSandbox } from '@utils/sandbox';

// GWT re-renders the header row only when rows are pushed, so an empty folder
// would leave aria-sort stale after a click. Two files give the sort rows to push.
const FILES = ['aaa.txt', 'bbb.txt'];

function reversed(direction: string): string {
  return direction === 'ascending' ? 'descending' : 'ascending';
}

test.describe.serial('Files pane column headers (#19170)', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    const dir = JSON.stringify(sandbox.dir);
    const names = `c(${FILES.map((f) => JSON.stringify(f)).join(', ')})`;
    await consoleActions.executeInConsole(
      `{ invisible(file.create(file.path(${dir}, ${names}))); ` +
      `.rs.api.filesPaneNavigate(${dir}) }`,
      { wait: true },
    );
    await executeCommand(page, 'activateFiles');
    for (const name of FILES)
      await expect(page.locator(`div[title="${name}"]`)).toBeVisible({ timeout: 15000 });
  });

  test.afterAll(async () => {
    await consoleActions.executeInConsole(
      '.rs.api.filesPaneNavigate(path.expand("~"))',
      { wait: true },
    );
  });

  test('sortable headers are column headers that report their sort direction', async ({ rstudioPage: page }) => {
    const filesPanel = page.locator('#rstudio_workbench_panel_files');
    const nameHeader = filesPanel.getByRole('columnheader', { name: 'Name', exact: true });
    await expect(nameHeader).toBeVisible();
    for (const name of ['File type', 'Size', 'Modified'])
      await expect(filesPanel.getByRole('columnheader', { name, exact: true })).toBeVisible();

    // Each click on the sort button sorts by Name, reversing the direction if Name
    // was already the sort column (as it is by default). Only the sorted column's
    // header carries aria-sort.
    const sortButton = nameHeader.getByRole('button', { name: 'Name', exact: true });
    for (let i = 0; i < 2; i++) {
      const before = await nameHeader.getAttribute('aria-sort');
      await sortButton.click();
      await expect(nameHeader).toHaveAttribute('aria-sort',
        before === null ? /^(ascending|descending)$/ : reversed(before));
      await expect(filesPanel.locator('th[aria-sort]')).toHaveCount(1);
    }
  });
});

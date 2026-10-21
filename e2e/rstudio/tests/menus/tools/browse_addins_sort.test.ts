// Tools > Addins > Browse Addins: sort state on an empty table (#19170).
//
// A sortable header reports its sort direction with aria-sort. The table
// redraws its headers only when rows are pushed, and sorting an empty list
// pushes none, so the dialog redraws them itself after each sort.

import { test, expect } from '@fixtures/rstudio.fixture';

import { dismissAllModals, executeCommand } from '@utils/commands';

test.describe('Browse Addins sorting (#19170)', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await dismissAllModals(page);
  });

  test('a sort click updates aria-sort when no addins match the filter', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'browseAddins');
    const dialog = page.getByRole('dialog', { name: 'Addins' });
    await dialog.getByRole('textbox', { name: /Filter addins/ }).pressSequentially('zzz-no-such-addin');
    await expect(dialog.getByText('No addins available')).toBeVisible();

    const nameHeader = dialog.getByRole('columnheader', { name: 'Name', exact: true });
    const sortButton = nameHeader.getByRole('button', { name: 'Name', exact: true });
    await expect(nameHeader).not.toHaveAttribute('aria-sort');

    await sortButton.click();
    await expect(nameHeader).toHaveAttribute('aria-sort', 'ascending');
    await sortButton.click();
    await expect(nameHeader).toHaveAttribute('aria-sort', 'descending');
  });
});

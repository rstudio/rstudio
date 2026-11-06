// The Files pane lists the parent directory as a ".." row with no checkbox,
// but the grid's keyboard handling still toggled that row's selection on
// Space, and Delete then offered (and performed) deletion of the parent
// directory (#19493). The parent row must never be part of a selection.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import type { Page } from 'playwright';

const CONFIRM_DELETE = { name: 'Confirm Delete' };

async function rowNamed(page: Page, name: string) {
  return page.locator('tr', { has: page.locator(`div[title="${name}"]`) });
}

test.describe('Files pane parent row', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let base = '';
  let baseName = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    baseName = `pw-parent-row-${Date.now()}`;
    base = `${sandbox.dir}/${baseName}`;
    await consoleActions.executeInConsole(
      `{ dir.create(file.path(${rPathLiteral(base)}, "child"), recursive = TRUE); ` +
        `writeLines("x", file.path(${rPathLiteral(base)}, "child", "victim.txt")); ` +
        `writeLines("x", file.path(${rPathLiteral(base)}, "marker.txt")) }`,
      { wait: true },
    );
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      `.rs.api.filesPaneNavigate(file.path(${rPathLiteral(base)}, "child"))`,
      { wait: true },
    );
    await executeCommand(page, 'activateFiles');
    await expect(await rowNamed(page, '..')).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  });

  test('Space on the focused grid cannot select the parent row for deletion', async ({ rstudioPage: page }) => {
    // nothing selected: Delete is a no-op
    await executeCommand(page, 'deleteFiles');
    await expect(page.getByRole('alertdialog', CONFIRM_DELETE)).toHaveCount(0);

    // focus the grid on the parent row without activating its link, then
    // Space, which GWT's selection manager turns into a toggle of that row
    const parentRow = await rowNamed(page, '..');
    await parentRow.locator('td').last().click();
    await page.keyboard.press('Space');

    // before the fix this showed "Are you sure you want to ... delete <parent>?"
    await executeCommand(page, 'deleteFiles');
    await expect(page.getByRole('alertdialog', CONFIRM_DELETE)).toHaveCount(0);

    // and the parent directory is untouched
    const marker = `__PARENT_ROW_${Date.now()}__`;
    await consoleActions.executeInConsole(
      `cat("${marker}", file.exists(file.path(${rPathLiteral(base)}, "marker.txt")), "${marker}")`,
    );
    await expect(consoleActions.consolePane.consoleOutput).toContainText(`${marker} TRUE ${marker}`, {
      timeout: TIMEOUTS.consoleReady,
    });
  });

  test('a checked file in the browsed directory is still deleted', async ({ rstudioPage: page }) => {
    const victim = await rowNamed(page, 'victim.txt');
    await expect(victim).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await victim.locator('input[type="checkbox"]').check();

    await executeCommand(page, 'deleteFiles');
    const dialog = page.getByRole('alertdialog', CONFIRM_DELETE);
    await expect(dialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(dialog).toContainText('victim.txt');
    await dialog.getByRole('button', { name: 'Yes' }).click();

    await expect(victim).toHaveCount(0, { timeout: TIMEOUTS.fileOpen });
  });
});

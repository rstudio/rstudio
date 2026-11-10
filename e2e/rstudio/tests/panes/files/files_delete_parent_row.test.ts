// The Files pane lists the parent directory as a ".." row with no checkbox,
// but the grid's keyboard handling still toggled that row's selection on
// Space, and Delete then offered (and performed) deletion of the parent
// directory (#19493). A shift-click range could also sweep it in, since the
// grid's selection manager keeps its anchor across listings. The parent row
// must never be part of a selection.

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

  // The fixtures point HOME at a sandboxed directory, so a row can be added
  // to the home listing (the only listing without a ".." row) for the
  // shift-click test to anchor on.
  const anchorName = `pw-parent-row-anchor-${Date.now()}`;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    baseName = `pw-parent-row-${Date.now()}`;
    base = `${sandbox.dir}/${baseName}`;
    await consoleActions.executeInConsole(
      `{ dir.create(file.path(${rPathLiteral(base)}, "child"), recursive = TRUE); ` +
        `writeLines("x", file.path(${rPathLiteral(base)}, "child", "victim.txt")); ` +
        `writeLines("x", file.path(${rPathLiteral(base)}, "child", "present.txt")); ` +
        `writeLines("x", file.path(${rPathLiteral(base)}, "marker.txt")); ` +
        `dir.create(file.path("~", "${anchorName}")) }`,
      { wait: true },
    );
  });

  test.afterAll(async () => {
    await consoleActions.executeInConsole(`unlink(file.path("~", "${anchorName}"), recursive = TRUE)`, {
      wait: true,
    });
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      `.rs.api.filesPaneNavigate(file.path(${rPathLiteral(base)}, "child"))`,
      { wait: true },
    );
    await executeCommand(page, 'activateFiles');

    // filesPaneNavigate is asynchronous: wait for a row that only the child
    // listing has, so a late-arriving listing cannot clear the selection
    // made below and mask the bug
    await expect(await rowNamed(page, 'present.txt')).toBeVisible({ timeout: TIMEOUTS.fileOpen });
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

  test('a shift-click range anchored in an earlier listing cannot select the parent row', async ({
    rstudioPage: page,
  }) => {
    // the home listing has no ".." row, so its first row is at index 0;
    // checking it leaves the selection manager's shift anchor there
    await consoleActions.executeInConsole(`.rs.api.filesPaneNavigate("~")`, { wait: true });
    await expect(await rowNamed(page, anchorName)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(await rowNamed(page, '..')).toHaveCount(0);
    const firstRow = page
      .locator('#rstudio_workbench_panel_files tr', { has: page.locator('input[type="checkbox"]') })
      .first();
    await firstRow.locator('input[type="checkbox"]').check();

    // back in the child listing, ".." is at index 0 again, and a shift-click
    // on the first file (row 1) selects rows 0..1 from the stale anchor
    await consoleActions.executeInConsole(
      `.rs.api.filesPaneNavigate(file.path(${rPathLiteral(base)}, "child"))`,
      { wait: true },
    );
    await expect(await rowNamed(page, 'present.txt')).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(await rowNamed(page, '..')).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    const present = await rowNamed(page, 'present.txt');
    await present.locator('input[type="checkbox"]').click({ modifiers: ['Shift'] });

    // the grid draws no selection highlight and the parent row has no
    // checkbox, so observe the selection through Rename: it needs exactly
    // one selected item and, unlike Delete, does not drop the parent row.
    // Before the fix the selection held two items and Rename refused.
    await executeCommand(page, 'renameFile');
    const prompt = page.getByText('Please enter the new file name:');
    await expect(prompt).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(page.getByText('Invalid Selection')).toHaveCount(0);
    await page.keyboard.press('Escape');
    await expect(prompt).toHaveCount(0);
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

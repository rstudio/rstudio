// Files pane date/time display format settings (#5908).
//
// By default the Files pane Modified column formats dates according to the
// system region (via the browser's Intl API) in the local time zone. The
// toolbar settings (gear) menu offers two overrides, each backed by a global
// pref consulted by StringUtil.formatDate:
//   - "Use ISO-8601 date-time formatting" (date_time_use_iso8601)
//   - "Use UTC time zone"                 (date_time_use_utc)
//
// Assertions check the *shape* of the formatted value (via regex) rather than
// an absolute timestamp, so they are insensitive to the runner's timezone and
// locale.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Locator, Page } from 'playwright';

import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, getPref, clearPref } from '@utils/commands';

const FILE_NAME = 'modified-format.txt';

// The Modified column is the 5th DataGrid column (selection, icon, name, size,
// modified). The name cell renders as <div title="<name>">, so we find the row
// containing it and read its Modified cell.
function modifiedCell(filesPanel: Locator, page: Page): Locator {
  const row = filesPanel.locator('tr', { has: page.locator(`div[title="${FILE_NAME}"]`) });
  return row.locator('td').nth(4);
}

// Open the Files toolbar settings (gear) menu and click the item with the
// given label.
async function chooseSetting(page: Page, label: string): Promise<void> {
  await page.locator('#rstudio_mb_files_settings').click();
  await page.getByText(label, { exact: true }).click();
}

test.describe.serial('Files pane date/time format settings (#5908)', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);

    // Start from the defaults regardless of prior runs.
    await clearPref(page, 'date_time_use_iso8601');
    await clearPref(page, 'date_time_use_utc');

    // Create a file on the session's filesystem and point the Files pane at it.
    await consoleActions.executeInConsole(
      `{ invisible(file.create(file.path(${JSON.stringify(sandbox.dir)}, ${JSON.stringify(FILE_NAME)}))); .rs.api.filesPaneNavigate(${JSON.stringify(sandbox.dir)}) }`,
      { wait: true },
    );

    await executeCommand(page, 'activateFiles');
    await expect(page.locator(`div[title="${FILE_NAME}"]`)).toBeVisible({ timeout: 15000 });
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'date_time_use_iso8601');
    await clearPref(page, 'date_time_use_utc');
    await consoleActions.executeInConsole(
      `{ unlink(file.path(${JSON.stringify(sandbox.dir)}, ${JSON.stringify(FILE_NAME)})); .rs.api.filesPaneNavigate(path.expand("~")) }`,
      { wait: true },
    );
  });

  test('defaults to a region-formatted (non-ISO, local) value', async ({ rstudioPage: page }) => {
    const filesPanel = page.locator('#rstudio_workbench_panel_files');
    // The locale default (e.g. "Mar 9, 2026, 2:30 PM") must not look like the
    // ISO override (which begins with a yyyy-MM-dd date), and the local time
    // zone carries no " UTC" marker.
    await expect(modifiedCell(filesPanel, page)).not.toHaveText(/^\d{4}-\d{2}-\d{2}/, { timeout: 15000 });
    await expect(modifiedCell(filesPanel, page)).not.toHaveText(/ UTC$/);
  });

  test('the UTC toggle renders in the UTC time zone', async ({ rstudioPage: page }) => {
    const filesPanel = page.locator('#rstudio_workbench_panel_files');

    await chooseSetting(page, 'Use UTC time zone');

    await expect.poll(async () => getPref(page, 'date_time_use_utc'), { timeout: 5000 }).toBe(true);
    // Still the region format (ISO not yet enabled), now marked as UTC.
    await expect(modifiedCell(filesPanel, page)).toHaveText(/ UTC$/, { timeout: 15000 });
    await expect(modifiedCell(filesPanel, page)).not.toHaveText(/^\d{4}-\d{2}-\d{2}/);
  });

  test('the ISO-8601 toggle reformats to yyyy-MM-dd', async ({ rstudioPage: page }) => {
    const filesPanel = page.locator('#rstudio_workbench_panel_files');

    await chooseSetting(page, 'Use ISO-8601 date-time formatting');

    await expect.poll(async () => getPref(page, 'date_time_use_iso8601'), { timeout: 5000 }).toBe(true);
    // ISO-8601 with UTC (enabled in the previous test) ends with a "Z".
    await expect(modifiedCell(filesPanel, page)).toHaveText(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}Z$/, { timeout: 15000 });
  });
});

// Files pane date/time display format settings (#5908).
//
// The Files pane toolbar has a settings (gear) menu that controls how the
// Modified column renders dates: the date component order (month/day/year,
// day/month/year, year/month/day) and whether a 24-hour clock is used. These
// are backed by the global `date_format` and `time_format_24_hour` prefs,
// consulted by StringUtil.formatDate. This test drives the gear menu and
// asserts the Modified column reformats accordingly.
//
// Assertions check the *shape* of the formatted value (via regex), not an
// absolute timestamp, so they are insensitive to the runner's timezone and
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

    // Start from the documented defaults regardless of prior runs.
    await clearPref(page, 'date_format');
    await clearPref(page, 'time_format_24_hour');

    // Create a file on the session's filesystem and point the Files pane at it.
    await consoleActions.executeInConsole(
      `{ invisible(file.create(file.path(${JSON.stringify(sandbox.dir)}, ${JSON.stringify(FILE_NAME)}))); .rs.api.filesPaneNavigate(${JSON.stringify(sandbox.dir)}) }`,
      { wait: true },
    );

    await executeCommand(page, 'activateFiles');
    await expect(page.locator(`div[title="${FILE_NAME}"]`)).toBeVisible({ timeout: 15000 });
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'date_format');
    await clearPref(page, 'time_format_24_hour');
    await consoleActions.executeInConsole(
      `{ unlink(file.path(${JSON.stringify(sandbox.dir)}, ${JSON.stringify(FILE_NAME)})); .rs.api.filesPaneNavigate(path.expand("~")) }`,
      { wait: true },
    );
  });

  test('defaults to month/day/year with a 12-hour clock', async ({ rstudioPage: page }) => {
    const filesPanel = page.locator('#rstudio_workbench_panel_files');
    // e.g. "Jun 29, 2026, 5:00 PM": abbreviated month, day, year, then a
    // 12-hour time with a meridiem (PM or p.m. depending on the locale data).
    await expect(modifiedCell(filesPanel, page)).toHaveText(
      /^[A-Za-z]{3} \d{1,2}, \d{4}, \d{1,2}:\d{2}\s?[AaPp]\.?[Mm]\.?$/,
      { timeout: 15000 },
    );
  });

  test('the 24-hour clock toggle reformats the time', async ({ rstudioPage: page }) => {
    const filesPanel = page.locator('#rstudio_workbench_panel_files');

    await chooseSetting(page, '24-Hour Clock');

    // The pref is written, and the time loses its meridiem in favor of a
    // 24-hour HH:mm. Date order is still month/day/year.
    await expect.poll(async () => getPref(page, 'time_format_24_hour'), { timeout: 5000 }).toBe(true);
    await expect(modifiedCell(filesPanel, page)).toHaveText(
      /^[A-Za-z]{3} \d{1,2}, \d{4}, \d{2}:\d{2}$/,
      { timeout: 15000 },
    );
  });

  test('the date-order setting reformats the date', async ({ rstudioPage: page }) => {
    const filesPanel = page.locator('#rstudio_workbench_panel_files');

    await chooseSetting(page, 'Year, Month, Day');

    // The pref is written, and the date renders ISO-style (yyyy-MM-dd). The
    // 24-hour time from the previous test is retained.
    await expect.poll(async () => getPref(page, 'date_format'), { timeout: 5000 }).toBe('year_month_day');
    await expect(modifiedCell(filesPanel, page)).toHaveText(
      /^\d{4}-\d{2}-\d{2}, \d{2}:\d{2}$/,
      { timeout: 15000 },
    );
  });
});

// Scrolling and keyboard focus in the web file dialogs' file list (RowTable).
//
// Scrolling redraws the list, and the redraw used to re-fire the selection,
// so the dialog copied the selected file's name over one the user had typed
// (#19316). Keyboard focus returning to the list now scrolls the selected
// row wholly into view, rendering it first if virtualization (lists over 500
// rows) had dropped it, so aria-activedescendant can name it (#19325).

import { Locator, Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';
import { FILE_LIST, FILE_NAME, openFileDialogAtHome } from '@utils/file-dialogs';

const uniq = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const dirName = `pw-list-scroll-${uniq}`;
// more than RowTable renders at once (MAX_VISIBLE_ROWS = 500)
const FILE_COUNT = 600;

const OPEN_BUTTON = '#rstudio_file_accept_open';

// row 0 is the '..' entry
const SELECTED_FILE = 'file001.R';

async function openDialogInTestDir(page: Page): Promise<Locator> {
  const dialog = await openFileDialogAtHome(page);
  await dialog.locator(FILE_NAME).fill(dirName);
  await dialog.getByRole('button', { name: 'Open' }).click();
  await expect(fileOption(dialog, SELECTED_FILE)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  return dialog;
}

function fileOption(dialog: Locator, name: string): Locator {
  return dialog.locator(`${FILE_LIST} tr[role="option"]`, { hasText: name });
}

// Sets the list's scroll position; the list scrolls in an element a couple
// of levels above the table, whose class name is obfuscated in release builds.
async function scrollList(dialog: Locator, where: 'bottom' | number): Promise<void> {
  await dialog.locator(FILE_LIST).evaluate((table, where) => {
    let panel = table.parentElement!;
    while (getComputedStyle(panel).overflowY !== 'auto')
      panel = panel.parentElement!;
    panel.scrollTop = where === 'bottom' ? panel.scrollHeight : where;
  }, where);
}

// The option aria-activedescendant names, and whether all of it shows.
async function activeOption(dialog: Locator) {
  return dialog.locator(FILE_LIST).evaluate((table) => {
    const id = table.getAttribute('aria-activedescendant');
    const row = id ? document.getElementById(id) : null;
    if (!row)
      return { text: null, whollyVisible: false };

    let panel = table.parentElement!;
    while (getComputedStyle(panel).overflowY !== 'auto')
      panel = panel.parentElement!;
    const r = row.getBoundingClientRect();
    const p = panel.getBoundingClientRect();
    const top = p.top + panel.clientTop;
    return {
      text: row.textContent,
      whollyVisible: r.top >= top && r.bottom <= top + panel.clientHeight,
    };
  });
}

// Keyboard focus on the list: Shift+Tab from the Open button that follows it.
async function shiftTabToList(dialog: Locator): Promise<void> {
  await dialog.locator(OPEN_BUTTON).focus();
  await dialog.page().keyboard.press('Shift+Tab');
  await expect(dialog.locator(FILE_LIST)).toBeFocused();
}

test.describe('Web file dialog list scrolling', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.executeInConsole(
      [
        'local({',
        `d <- file.path(path.expand("~"), "${dirName}")`,
        'dir.create(d)',
        `invisible(file.create(file.path(d, sprintf("file%03d.R", seq_len(${FILE_COUNT})))))`,
        '})',
      ].join('; '),
      { wait: true },
    );
  });

  test.afterAll(async () => {
    await consoleActions.executeInConsole(
      `unlink(file.path(path.expand("~"), "${dirName}"), recursive = TRUE)`,
      { wait: true },
    );
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await page.keyboard.press('Escape');
  });

  test('scrolling the list keeps a typed file name', async ({ rstudioPage: page }) => {
    const dialog = await openDialogInTestDir(page);
    await fileOption(dialog, SELECTED_FILE).click();
    const fileName = dialog.locator(FILE_NAME);
    await expect(fileName).toHaveValue(SELECTED_FILE);
    await fileName.fill('pw-typed-name.R');

    await fileOption(dialog, SELECTED_FILE).hover();
    await page.mouse.wheel(0, 10000);

    // the redraw that follows a scroll renders a later window of rows
    const firstRendered = dialog.locator(`${FILE_LIST} tr[role="option"]`).first();
    await expect(firstRendered).not.toHaveAttribute('__row', '0', { timeout: TIMEOUTS.fileOpen });
    await expect(fileName).toHaveValue('pw-typed-name.R');
  });

  test('keyboard focus reveals a partly scrolled-off selected file', async ({ rstudioPage: page }) => {
    const dialog = await openDialogInTestDir(page);
    const option = fileOption(dialog, SELECTED_FILE);
    await option.click();
    // leave 2px of the selected row showing at the top of the list
    const bottom = await option.evaluate((row: HTMLElement) => row.offsetTop + row.offsetHeight);
    await scrollList(dialog, bottom - 2);

    await shiftTabToList(dialog);

    await expect.poll(() => activeOption(dialog)).toEqual({
      text: expect.stringContaining(SELECTED_FILE),
      whollyVisible: true,
    });
  });

  test('keyboard focus reveals a selected file virtualization dropped', async ({ rstudioPage: page }) => {
    const dialog = await openDialogInTestDir(page);
    await fileOption(dialog, SELECTED_FILE).click();
    await scrollList(dialog, 'bottom');
    // the redraw no longer renders the selected row
    await expect(fileOption(dialog, SELECTED_FILE)).toHaveCount(0, { timeout: TIMEOUTS.fileOpen });

    await shiftTabToList(dialog);

    await expect.poll(() => activeOption(dialog)).toEqual({
      text: expect.stringContaining(SELECTED_FILE),
      whollyVisible: true,
    });
  });
});

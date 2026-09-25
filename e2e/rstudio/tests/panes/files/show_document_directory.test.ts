// Show Document's Directory (#6781): the document tab context menu item and
// the showActiveDocDirInFiles command navigate the Files pane to the folder
// holding a document -- from the main source column, an extra source column,
// and a popped-out source window.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from 'playwright';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS } from '@utils/constants';
import { openFile, seedSandboxFile } from '@utils/files';
import { rPathLiteral } from '@utils/r';
import {
  clearPref,
  executeCommand,
  isCommandEnabled,
  resetSourcePaneState,
  setPref,
} from '@utils/commands';

const FILES_TAB = '#rstudio_workbench_tab_files';
const FILES_PANEL = '#rstudio_workbench_panel_files';
const SELECTED_DOC_TAB = "[class*='rstudio_source_panel'] [class*='PanelTab-selected']";
const EXTRA_COLUMNS = '[id^="rstudio_Source"][id$="_pane"]:not(#rstudio_Source_pane)';
const PALETTE_LIST = '#rstudio_command_palette_list';
const MENU_RENAME = '#rstudio_tab_rename_file';
const MENU_SET_WD = '#rstudio_tab_set_working_dir';
const MENU_SHOW_DIR = '#rstudio_tab_set_files_pane';
const COMMAND_ID = 'showActiveDocDirInFiles';
const COMMAND_LABEL = "Show Current Document's Directory in Files Pane";

// A per-test directory holding one uniquely named file, so seeing that file
// listed in the Files pane proves the pane is showing that directory.
async function seedDocument(page: Page, root: string, name: string): Promise<string> {
  return seedSandboxFile(page, root, `${name}/${name}.R`, `# ${name}\n`);
}

function filesRow(page: Page, fileName: string) {
  return page.locator(`${FILES_PANEL} div[title="${fileName}"]`);
}

// Point the Files pane at a sentinel directory and switch to the Plots tab, so
// both the navigation and the activation under test start from a known state.
// Waits on the sentinel row rather than the console prompt: the navigation is
// delivered as a client event that can land after the prompt.
async function parkFilesPane(page: Page, root: string): Promise<void> {
  const sentinel = await seedSandboxFile(page, root, 'park/park_sentinel.txt', 'park\n');
  await new ConsolePaneActions(page).executeInConsole(
    `.rs.api.filesPaneNavigate(dirname(${rPathLiteral(sentinel)}))`,
  );
  await executeCommand(page, 'activateFiles');
  await expect(filesRow(page, 'park_sentinel.txt')).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  await executeCommand(page, 'activatePlots');
  await expect(page.locator(FILES_TAB)).toHaveAttribute('aria-selected', 'false');
}

async function expectFilesPaneShows(page: Page, fileName: string): Promise<void> {
  await expect(page.locator(FILES_TAB)).toHaveAttribute('aria-selected', 'true');
  await expect(filesRow(page, `${fileName}.R`)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
}

// The context menu's entries in order, with '---' for each separator.
async function contextMenuLayout(page: Page): Promise<string[]> {
  return page.locator(MENU_RENAME).evaluate((item) => {
    const menu = item.closest('table');
    if (!menu)
      throw new Error('tab context menu table not found');
    const cells = menu.querySelectorAll('td.gwt-MenuItem, td.gwt-MenuItemSeparator');
    return Array.from(cells, (cell) =>
      cell.classList.contains('gwt-MenuItemSeparator') ? '---' : cell.id);
  });
}

async function runFromPalette(page: Page): Promise<void> {
  await page.keyboard.press('ControlOrMeta+Shift+p');
  await expect(page.locator(PALETTE_LIST)).toBeVisible({ timeout: 5000 });
  await page.keyboard.type('Show Current Document');
  const item = page.locator(PALETTE_LIST).getByText(COMMAND_LABEL, { exact: true });
  await expect(item).toBeVisible({ timeout: 5000 });
  await item.click();
}

async function popOutActiveDoc(page: Page, fileName: string): Promise<Page> {
  const detached = page.context().waitForEvent('page');
  await executeCommand(page, 'popoutDoc');
  const satellite = await detached;
  await satellite.waitForURL(/view=source_window_/);
  await expect(satellite.locator(SELECTED_DOC_TAB)).toContainText(fileName, {
    timeout: TIMEOUTS.fileOpen,
  });
  return satellite;
}

test.describe("Show Document's Directory (#6781)", () => {
  const sandbox = useSuiteSandbox();

  test.afterEach(async ({ rstudioPage: page }) => {
    await resetSourcePaneState(page);
    await new ConsolePaneActions(page).executeInConsole(`setwd(${rPathLiteral(sandbox.dir)})`);
  });

  test('tab menu item shows the directory and sits in the expected place', async ({ rstudioPage: page }) => {
    const name = 'showdir_menu';
    await openFile(page, await seedDocument(page, sandbox.dir, name));
    await parkFilesPane(page, sandbox.dir);

    await page.locator(SELECTED_DOC_TAB).click({ button: 'right' });
    expect(await contextMenuLayout(page)).toEqual([
      'rstudio_tab_rename_file',
      '---',
      'rstudio_tab_copy_path',
      'rstudio_tab_set_working_dir',
      'rstudio_tab_set_files_pane',
      '---',
      'rstudio_tab_close',
      'rstudio_tab_close_all',
      'rstudio_tab_close_others',
    ]);
    await expect(page.locator(MENU_SHOW_DIR)).toHaveText("Show Document's Directory");
    await page.locator(MENU_SHOW_DIR).click();

    await expectFilesPaneShows(page, name);
  });

  test('command palette entry shows the directory', async ({ rstudioPage: page }) => {
    const name = 'showdir_palette';
    await openFile(page, await seedDocument(page, sandbox.dir, name));
    await parkFilesPane(page, sandbox.dir);

    await runFromPalette(page);

    await expectFilesPaneShows(page, name);
  });

  test('unsaved documents do not offer the item or the command', async ({ rstudioPage: page }) => {
    // resetSourcePaneState (run after every test, and by the fixture) leaves
    // one Untitled document active.
    await resetSourcePaneState(page);
    expect(await isCommandEnabled(page, COMMAND_ID)).toBe(false);

    await page.locator(SELECTED_DOC_TAB).click({ button: 'right' });
    await expect(page.locator('#rstudio_tab_close')).toBeVisible();
    await expect(page.locator(MENU_SHOW_DIR)).toHaveCount(0);
    await page.keyboard.press('Escape');

    await openFile(page, await seedDocument(page, sandbox.dir, 'showdir_enabled'));
    expect(await isCommandEnabled(page, COMMAND_ID)).toBe(true);
  });

  test('saving an untitled document enables the command', async ({ rstudioPage: page }) => {
    await resetSourcePaneState(page);
    expect(await isCommandEnabled(page, COMMAND_ID)).toBe(false);

    // An Untitled R script saved as .R keeps its file type, so this doesn't
    // depend on the refresh that a file type change triggers.
    await executeCommand(page, 'saveSourceDoc');
    const fileName = page.locator('#file_dialog_name_prompt');
    await expect(fileName).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await fileName.fill(`${sandbox.dir}/showdir_saved.R`);
    await page.locator('#rstudio_file_accept_save').click();
    await expect(page.locator(SELECTED_DOC_TAB)).toContainText('showdir_saved.R', {
      timeout: TIMEOUTS.fileOpen,
    });

    await expect.poll(() => isCommandEnabled(page, COMMAND_ID)).toBe(true);
    expect(await isCommandEnabled(page, 'renameSourceDoc')).toBe(true);
  });

  test('works for a document in an extra source column', async ({ rstudioPage: page }) => {
    const name = 'showdir_column';
    const docPath = await seedDocument(page, sandbox.dir, name);
    await executeCommand(page, 'newSourceColumn');
    await expect(page.locator(`${EXTRA_COLUMNS} .ace_editor`)).toHaveCount(1);
    // openFile's selected-tab wait is ambiguous with two columns, so wait on
    // the new column's tab instead.
    await new ConsolePaneActions(page).executeInConsole(`file.edit(${rPathLiteral(docPath)})`);
    const columnTab = page.locator(`${EXTRA_COLUMNS} ${SELECTED_DOC_TAB}`);
    await expect(columnTab).toContainText(name, { timeout: TIMEOUTS.fileOpen });
    await parkFilesPane(page, sandbox.dir);

    await columnTab.click({ button: 'right' });
    await page.locator(MENU_SHOW_DIR).click();
    await expectFilesPaneShows(page, name);

    await parkFilesPane(page, sandbox.dir);
    await columnTab.click();
    await runFromPalette(page);
    await expectFilesPaneShows(page, name);
  });

  test('works from a popped-out source window', async ({ rstudioPage: page }) => {
    const name = 'showdir_satellite';
    await openFile(page, await seedDocument(page, sandbox.dir, name));
    let satellite: Page | undefined;
    try {
      satellite = await popOutActiveDoc(page, name);
      await parkFilesPane(page, sandbox.dir);

      await satellite.locator(SELECTED_DOC_TAB).click({ button: 'right' });
      await satellite.locator(MENU_SHOW_DIR).click();
      await expectFilesPaneShows(page, name);

      await parkFilesPane(page, sandbox.dir);
      await satellite.bringToFront();
      await satellite.locator(SELECTED_DOC_TAB).click();
      await runFromPalette(satellite);
      await expectFilesPaneShows(page, name);
    } finally {
      await satellite?.close().catch(() => {});
    }
  });

  test('Set Working Directory from a popped-out window also moves the Files pane', async ({ rstudioPage: page }) => {
    const name = 'showdir_setwd';
    const docPath = await seedDocument(page, sandbox.dir, name);
    await openFile(page, docPath);
    const consoleActions = new ConsolePaneActions(page);
    // With syncing on, setwd() alone would move the pane; keep it off so only
    // the menu item's own navigation can.
    await setPref(page, 'sync_files_pane_working_dir', false);
    let satellite: Page | undefined;
    try {
      satellite = await popOutActiveDoc(page, name);
      await parkFilesPane(page, sandbox.dir);

      await satellite.locator(SELECTED_DOC_TAB).click({ button: 'right' });
      await satellite.locator(MENU_SET_WD).click();

      const inDocDir = `identical(normalizePath(getwd()), normalizePath(dirname(${rPathLiteral(docPath)})))`;
      await expect.poll(() => consoleActions.evalRLogical(inDocDir), {
        timeout: TIMEOUTS.consoleReady,
      }).toBe(true);
      // The item navigates without raising the pane, so bring it up to read the
      // listing.
      await executeCommand(page, 'activateFiles');
      await expectFilesPaneShows(page, name);
    } finally {
      await satellite?.close().catch(() => {});
      await clearPref(page, 'sync_files_pane_working_dir');
    }
  });
});

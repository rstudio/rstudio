import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { executeCommand, setPref, getPref, clearPref } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

// -- Selectors ----------------------------------------------------------------

// Command palette search box and result list.
const PALETTE_SEARCH = '#rstudio_command_palette_search';
const PALETTE_LIST = '#rstudio_command_palette_list';
// The "insert native pipe operator" user pref renders as an inline toggle in
// the palette when the filter matches it; the checkbox is the <input> inside.
const NATIVE_PIPE_CHECKBOX = '#rstudio_command_entry_user_pref_insert_native_pipe_operator input';
// Source tabs across all columns (one [role=tab] per open document).
const SOURCE_TABS = "[class*='rstudio_source_panel'] [role='tab']";

test.describe('Command Palette', () => {
  // No useSuiteSandbox() here: this spec only drives commands and prefs and
  // writes nothing to disk, so it needs no per-spec workdir. Source-pane
  // isolation comes from resetSourcePane() in beforeEach.
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
  });

  test.beforeEach(async () => {
    // Start each test from a single Untitled tab so the tab-count assertions
    // and the active editor begin from a known state.
    await consoleActions.resetSourcePane();
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    // Restore the pipe-operator pref to its default and tidy the source pane.
    await clearPref(page, 'insert_native_pipe_operator');
    await consoleActions.resetSourcePane();
  });

  test('filters commands and runs the highlighted one', async ({ rstudioPage: page }) => {
    // Original: test_desktop_Command_Palette.py::test_command_palette
    //
    // Each palette row shows one resolved label (AppCommandPaletteItem: label ->
    // buttonLabel -> desc -> menuLabel, first non-empty). For these commands
    // that resolves to: newSourceDoc -> its label "Create a New R Script";
    // newSqlDoc -> its desc "Create a new SQL script"; newTextDoc -> its desc
    // "Create a new text file". The filter matches against that same label.
    const search = page.locator(PALETTE_SEARCH);

    await executeCommand(page, 'showCommandPalette');
    await expect(search).toBeVisible({ timeout: TIMEOUTS.fileOpen });

    // Escape closes the palette.
    await page.keyboard.press('Escape');
    await expect(search).toBeHidden();

    // Reopen and filter to "script".
    await executeCommand(page, 'showCommandPalette');
    await expect(search).toBeVisible();
    await search.pressSequentially('script');

    // R and SQL script labels contain "script"; the text-file label has no "s"
    // at all, so it can't match and is dropped from the filtered list.
    const list = page.locator(PALETTE_LIST);
    await expect(list).toContainText('Create a New R Script', { timeout: TIMEOUTS.fileOpen });
    await expect(list).toContainText('Create a new SQL script');
    await expect(list).not.toContainText('Create a new text file');

    // Enter runs the highlighted (first) entry. AppCommandPaletteSource
    // front-loads newSourceDoc, so "Create a New R Script" is the first match;
    // running it opens a new R-script tab alongside the reset placeholder.
    await search.press('Enter');
    await expect(page.locator(SOURCE_TABS)).toHaveCount(2, { timeout: TIMEOUTS.fileOpen });
    await expect(sourceActions.sourcePane.selectedTab).toContainText('Untitled');
    await expect(sourceActions.sourcePane.footerTable).toContainText('R Script');
  });

  test('toggling the native pipe pref changes the inserted operator', async ({ rstudioPage: page }) => {
    // Original: test_desktop_Command_Palette.py::
    //   test_command_Palette_Turn_On_Rs_Native_Pipe_Operator_Setting
    // Start with the native pipe off so flipping it on from the palette is what
    // makes the inserted operator "|>" -- proving the palette toggle took effect
    // rather than passing trivially because the pref was already on.
    await setPref(page, 'insert_native_pipe_operator', false);

    await executeCommand(page, 'newSourceDoc');
    await expect(page.locator(SOURCE_TABS)).toHaveCount(2, { timeout: TIMEOUTS.fileOpen });

    const search = page.locator(PALETTE_SEARCH);
    await executeCommand(page, 'showCommandPalette');
    await expect(search).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await search.pressSequentially('pipe');

    // Flip the inline pref toggle on.
    const checkbox = page.locator(NATIVE_PIPE_CHECKBOX);
    await expect(checkbox).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    if (!(await checkbox.isChecked())) {
      await checkbox.click();
    }
    // The toggle persists through the setUserPrefs RPC; wait for the new value
    // to land before inserting so insertPipeOperator reads it.
    await expect
      .poll(() => getPref(page, 'insert_native_pipe_operator'), { timeout: TIMEOUTS.fileOpen })
      .toBe(true);

    await page.keyboard.press('Escape');
    await expect(search).toBeHidden();

    // Insert the pipe into the R script; with the native pipe on it is "|>".
    await sourceActions.sourcePane.aceTextInput.click({ force: true });
    await executeCommand(page, 'insertPipeOperator');
    await expect
      .poll(() => sourceActions.getEditorContent(), { timeout: TIMEOUTS.fileOpen })
      .toContain('|>');
  });
});

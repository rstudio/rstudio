// File / Open File dialog tests ported from
// src/cpp/tests/automation/testthat/test-automation-files.R.
//
// Test 1 and test 2 exercise the Server-side virtualized Open File dialog
// (Desktop uses the native OS picker, so they are tagged @server_only).
// Test 3 (autosave-unchanged-doc, #16329) runs on both fixtures.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS, typeSlowly } from '@utils/constants';
import { executeCommand, documentCloseAllNoSave, setPref, clearPref } from '@utils/commands';

const MODAL_DIALOG = '.rstudio_modal_dialog';
// The Open File dialog renders a virtualized file list as a `<table
// role="listbox" aria-label="Directory Contents">`. RowTable has its own
// prefix-search handler that buffers typed characters and selects the row
// whose key starts with that prefix; clicking the table first puts focus
// there so the keystrokes reach the prefix-search handler instead of the
// path text input.
const DIRECTORY_CONTENTS_TABLE = '[role="listbox"][aria-label="Directory Contents"]';

test.describe('Open File dialog (virtualized)', { tag: ['@server_only'] }, () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // Clean up any stub files and close opened source docs.
    await documentCloseAllNoSave(page);
    await consoleActions.executeInConsole(`unlink(list.files(${JSON.stringify(sandbox.dir)}, pattern = "\\\\.R$", full.names = TRUE))`);
  });

  test('handles a large flat directory (>500 files)', async ({ rstudioPage: page }) => {
    // Create 10000 stub .R files in the suite sandbox -- enough to force the
    // virtualized list path. { wait: true } so the file creation has actually
    // landed before the Open dialog enumerates the directory.
    await consoleActions.executeInConsole(
      `{ setwd(${JSON.stringify(sandbox.dir)}); files <- sprintf("%04i.R", 0:9999); invisible(file.create(files)) }`,
      { wait: true, timeout: TIMEOUTS.consoleReady },
    );

    await executeCommand(page, 'openSourceDoc');
    await expect(page.locator(MODAL_DIALOG)).toBeVisible({ timeout: TIMEOUTS.consoleReady });

    // Focus the file list so the typed prefix reaches RowTable's prefix-search
    // handler (default focus is on the path text input, which would just
    // accumulate characters instead of selecting a row).
    await page.locator(DIRECTORY_CONTENTS_TABLE).click();
    await typeSlowly(page, '5159');
    await page.keyboard.press('Enter');

    const selectedTab = new SourcePane(page).selectedTab;
    await expect(selectedTab).toContainText('5159.R', { timeout: TIMEOUTS.fileOpen });
  });

  test('handles a moderate flat directory (<500 files)', async ({ rstudioPage: page }) => {
    // 451 stub files keeps us below the virtualization threshold; the dialog
    // should still resolve a typed prefix to the only matching file.
    await consoleActions.executeInConsole(
      `{ setwd(${JSON.stringify(sandbox.dir)}); files <- sprintf("%03i.R", 0:450); invisible(file.create(files)) }`,
      { wait: true, timeout: TIMEOUTS.consoleReady },
    );

    await executeCommand(page, 'openSourceDoc');
    await expect(page.locator(MODAL_DIALOG)).toBeVisible({ timeout: TIMEOUTS.consoleReady });

    // No 455.R exists; the dialog selects the closest preceding file (450.R)
    // via RowTable's prefix-search. Focus the table first (see comment above).
    await page.locator(DIRECTORY_CONTENTS_TABLE).click();
    await typeSlowly(page, '455');
    await page.keyboard.press('Enter');

    const selectedTab = new SourcePane(page).selectedTab;
    await expect(selectedTab).toContainText('450.R', { timeout: TIMEOUTS.fileOpen });
  });
});

test.describe('Autosave on blur', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  // https://github.com/rstudio/rstudio/issues/16329
  test('unchanged document is not rewritten when focus leaves the source pane', async ({ rstudioPage: page }) => {
    await setPref(page, 'auto_save_on_blur', true);
    try {
      // Create a file on disk inside the sandbox and open it via file.edit().
      const setupExpr = [
        `{ con <- file.path(${JSON.stringify(sandbox.dir)}, "autosave.R")`,
        'writeLines("# hello world", con = con)',
        'assign(".rs_autosave_path", con, envir = globalenv())',
        'file.edit(con) }',
      ].join('; ');
      await consoleActions.executeInConsole(setupExpr, { wait: true });

      // Record mtime, blur the source pane, and assert it didn't change.
      await consoleActions.executeInConsole(
        '{ .rs_autosave_old <- file.info(.rs_autosave_path)$mtime }',
      );
      await executeCommand(page, 'activateConsole');
      await executeCommand(page, 'activateSource');
      await executeCommand(page, 'activateConsole');

      const unchangedMarker = `__AUTOSAVE_UNCHANGED_${Date.now()}__`;
      await consoleActions.executeInConsole(
        `{ .rs_autosave_new <- file.info(.rs_autosave_path)$mtime; cat("${unchangedMarker}", isTRUE(.rs_autosave_old == .rs_autosave_new), "${unchangedMarker}") }`,
      );
      await expect(consoleActions.consolePane.consoleOutput).toContainText(
        `${unchangedMarker} TRUE ${unchangedMarker}`,
        { timeout: TIMEOUTS.consoleReady },
      );

      // Edit the source doc via the Ace API, blur, and assert the file was rewritten.
      await executeCommand(page, 'activateSource');
      const editor = new AceEditor(page, '# hello world');
      await editor.gotoLine(2);
      await editor.insert('# this is some text');
      await executeCommand(page, 'activateConsole');
      // mtime granularity is one second on some filesystems; wait until the
      // wall clock has moved past the old mtime before re-stating, so a fresh
      // write produces a different timestamp.
      await page.waitForTimeout(1500);

      const changedMarker = `__AUTOSAVE_CHANGED_${Date.now()}__`;
      await consoleActions.executeInConsole(
        `{ .rs_autosave_after <- file.info(.rs_autosave_path)$mtime; cat("${changedMarker}", isTRUE(.rs_autosave_old == .rs_autosave_after), "${changedMarker}") }`,
      );
      await expect(consoleActions.consolePane.consoleOutput).toContainText(
        `${changedMarker} FALSE ${changedMarker}`,
        { timeout: TIMEOUTS.consoleReady },
      );
    } finally {
      await clearPref(page, 'auto_save_on_blur');
      await documentCloseAllNoSave(page);
      await consoleActions.executeInConsole(
        `{ unlink(file.path(${JSON.stringify(sandbox.dir)}, "autosave.R")); rm(list = ls(pattern = "^\\\\.rs_autosave", envir = globalenv()), envir = globalenv()) }`,
      );
    }
  });
});

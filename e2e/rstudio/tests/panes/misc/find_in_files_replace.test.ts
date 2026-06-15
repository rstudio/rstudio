import { test, expect } from '@fixtures/rstudio.fixture';
import * as fs from 'fs';
import * as path from 'path';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';
import { createAndOpenProject, closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS } from '@utils/constants';

// -- Selectors ----------------------------------------------------------------

// Find in Files results pane (workbench_panel_ + idSafeString("Find in Files")).
const FIND_PANE = '#rstudio_workbench_panel_find_in_files';
// The Find in Files dialog and its three search-option checkboxes
// (ElementIds.FIND_FILES_CASE / FIND_FILES_WHOLE_WORD / FIND_FILES_REGEX).
const FIND_DIALOG = 'div.gwt-DialogBox[aria-label="Find in Files"]';
const CASE_CHECKBOX = '#rstudio_find_files_case';
const WHOLE_WORD_CHECKBOX = '#rstudio_find_files_whole_word';
const REGEX_CHECKBOX = '#rstudio_find_files_regex';
// Search-pattern box in the Find in Files dialog (ElementIds.FIND_FILES_TEXT).
const SEARCH_INPUT = '#rstudio_find_files_text';
// Replace-mode toggle, replace box, and Replace All button in the find pane
// (ElementIds.FIND_REPLACE_MODE_TOGGLE / FIND_REPLACE_TEXT / FIND_REPLACE_ALL).
const REPLACE_MODE_TOGGLE = '#rstudio_find_replace_mode_toggle';
const REPLACE_INPUT = '#rstudio_find_replace_text';
const REPLACE_ALL_BTN = '#rstudio_find_replace_all';
// Find pane toolbars and their run/stop controls (aria-labels).
const SEARCH_TOOLBAR = 'div[aria-label="Find Output Tab"]';
const REPLACE_TOOLBAR = 'div[aria-label="Replace"]';
const REFRESH_BTN = 'button[aria-label="Refresh Find in Files results"]';
const STOP_SEARCH_BTN = 'button[aria-label="Stop find in files"]';
const STOP_REPLACE_BTN = 'button[aria-label="Stop replace"]';
// Standard modal buttons (pages/modals.page.ts).
const FIND_OK_BTN = '#rstudio_dlg_ok';
const FIND_CANCEL_BTN = '#rstudio_dlg_cancel';
const CONFIRM_YES_BTN = '#rstudio_dlg_yes';

const sandbox = useSuiteSandbox();

// General Find in Files behavior: the search-options dialog, and a real
// multi-file search with the search/replace toolbar toggle. Cross-mode -- file
// creation goes through the R console and every assertion is on the UI, so
// there's no Node-fs dependency (unlike the disk-reading Replace All test below).
test.describe('Find in Files', () => {
  test.afterAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
  });

  test('dialog exposes the case, whole-word, and regex options', async ({ rstudioPage: page }) => {
    // Original: test_desktop_FindInFiles.py::test_show_modal
    await executeCommand(page, 'findInFiles');
    await expect(page.locator(FIND_DIALOG)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(page.locator(CASE_CHECKBOX)).toBeVisible();
    await expect(page.locator(WHOLE_WORD_CHECKBOX)).toBeVisible();
    await expect(page.locator(REGEX_CHECKBOX)).toBeVisible();

    await page.locator(FIND_CANCEL_BTN).click();
    await expect(page.locator(FIND_DIALOG)).toBeHidden();
  });

  test('searches across multiple files and toggles the replace toolbar', async ({ rstudioPage: page }) => {
    // Original: test_desktop_FindInFiles.py::test_show_pane +
    // test_create_file_and_perform_search (the single-file Selenium versions).
    const consoleActions = new ConsolePaneActions(page);

    // Open a project so the dialog defaults its search scope to the project dir
    // (avoids driving the read-only native directory chooser). Opening the
    // project sets R's working directory to the project root, so the relative
    // writeLines paths below land inside the searched scope.
    await createAndOpenProject(page, sandbox.dir, 'find_multi_file_project');

    // Macbeth's dagger soliloquy (Act 2, Sc 1), one file per fragment. "dagger"
    // appears in the first and third but not the second, so the search must
    // return hits in two files and correctly skip the middle one.
    await consoleActions.executeInConsole(
      `writeLines(c("Is this a dagger which I see before me,", "The handle toward my hand? Come, let me clutch thee."), con = "soliloquy_1.txt")`,
      { wait: true },
    );
    await consoleActions.executeInConsole(
      `writeLines(c("I have thee not, and yet I see thee still.", "Art thou not, fatal vision, sensible", "To feeling as to sight?"), con = "soliloquy_2.txt")`,
      { wait: true },
    );
    await consoleActions.executeInConsole(
      `writeLines(c("Or art thou but", "A dagger of the mind, a false creation", "Proceeding from the heat-oppressed brain?"), con = "soliloquy_3.txt")`,
      { wait: true },
    );

    // Search the project for "dagger".
    await executeCommand(page, 'findInFiles');
    const searchInput = page.locator(SEARCH_INPUT);
    await searchInput.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    await searchInput.click();
    await searchInput.pressSequentially('dagger');
    await page.locator(FIND_OK_BTN).click();

    // Results span the two matching files and exclude the third. Waiting for
    // both matches confirms the search finished before the absence assertion.
    const findPane = page.locator(FIND_PANE);
    await expect(findPane).toContainText('soliloquy_1.txt', { timeout: TIMEOUTS.fileOpen });
    await expect(findPane).toContainText('soliloquy_3.txt');
    await expect(findPane).not.toContainText('soliloquy_2.txt');

    // The pane opens in search mode: search toolbar shown, replace toolbar
    // hidden, refresh available, and the search isn't still running.
    await expect(page.locator(SEARCH_TOOLBAR)).toBeVisible();
    await expect(page.locator(REPLACE_TOOLBAR)).toBeHidden();
    await expect(page.locator(REFRESH_BTN)).toBeVisible();
    await expect(page.locator(STOP_SEARCH_BTN)).toBeHidden();

    // Toggling replace mode reveals the replace toolbar and Replace All, while
    // the search toolbar stays put.
    await page.locator(REPLACE_MODE_TOGGLE).click();
    await expect(page.locator(SEARCH_TOOLBAR)).toBeVisible();
    await expect(page.locator(REPLACE_TOOLBAR)).toBeVisible();
    await expect(page.locator(REPLACE_ALL_BTN)).toBeVisible();
    await expect(page.locator(STOP_REPLACE_BTN)).toBeHidden();
  });

  test('regex replace preview omits matches whose replacement is identical', async ({ rstudioPage: page }) => {
    // A regex replace can produce a replacement that is identical to the matched
    // text (e.g. "cat" matched by "c.t" and replaced with "cat"). Such matches
    // are no-ops and should not be presented as replacement candidates: a line
    // whose every match is a no-op is omitted from the preview entirely.
    const consoleActions = new ConsolePaneActions(page);
    await createAndOpenProject(page, sandbox.dir, 'find_noop_preview_project');

    // Search "c.t" matches cat/cot/cut; replacing with "cat" leaves "cat"
    // unchanged (no-op) but rewrites "cot"/"cut". noop_only.txt therefore has
    // nothing to replace, while real_change.txt does.
    await consoleActions.executeInConsole(
      `writeLines(c("cat", "cat cat"), con = "noop_only.txt")`,
      { wait: true },
    );
    await consoleActions.executeInConsole(
      `writeLines(c("cot", "cut"), con = "real_change.txt")`,
      { wait: true },
    );

    // Regex search for "c.t" -- regex mode is required for the live replace
    // preview (PreviewReplaceEvent only fires when the search was a regex).
    await executeCommand(page, 'findInFiles');
    const searchInput = page.locator(SEARCH_INPUT);
    await searchInput.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    await page.locator(REGEX_CHECKBOX).check();
    await searchInput.click();
    await searchInput.pressSequentially('c.t');
    await page.locator(FIND_OK_BTN).click();

    // In plain search mode both files match and are shown.
    const findPane = page.locator(FIND_PANE);
    await expect(findPane).toContainText('noop_only.txt', { timeout: TIMEOUTS.fileOpen });
    await expect(findPane).toContainText('real_change.txt');

    // Switch to replace mode and enter the replacement; this triggers the regex
    // replace preview (debounced) which re-runs the search server-side.
    await page.locator(REPLACE_MODE_TOGGLE).click();
    const replaceInput = page.locator(REPLACE_INPUT);
    await replaceInput.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    await replaceInput.click();
    await replaceInput.pressSequentially('cat');

    // The preview keeps real_change.txt (its matches change) but drops
    // noop_only.txt entirely, since every match there would be a no-op.
    await expect(findPane).toContainText('real_change.txt', { timeout: TIMEOUTS.fileOpen });
    await expect(findPane).not.toContainText('noop_only.txt');
  });

  test('literal replace preview omits matches whose replacement is identical', async ({ rstudioPage: page }) => {
    // The same no-op omission must also apply to literal (non-regex) searches,
    // whose replace preview is rendered client-side rather than server-side. A
    // case-insensitive search for "cat" matches both "cat" and "Cat"; replacing
    // with "cat" leaves "cat" unchanged (no-op) but rewrites "Cat".
    const consoleActions = new ConsolePaneActions(page);
    await createAndOpenProject(page, sandbox.dir, 'find_literal_noop_project');

    // mixed.txt has one no-op ("cat") and one real change ("Cat") on one line;
    // allnoop.txt has only no-op matches.
    await consoleActions.executeInConsole(
      `writeLines("cat Cat", con = "mixed.txt")`,
      { wait: true },
    );
    await consoleActions.executeInConsole(
      `writeLines(c("cat", "cat cat"), con = "allnoop.txt")`,
      { wait: true },
    );

    // Literal, case-insensitive search for "cat" (regex and case-sensitive both
    // off). Uncheck regex defensively in case a prior test left it on.
    await executeCommand(page, 'findInFiles');
    const searchInput = page.locator(SEARCH_INPUT);
    await searchInput.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    await page.locator(REGEX_CHECKBOX).uncheck();
    await page.locator(CASE_CHECKBOX).uncheck();
    await searchInput.click();
    await searchInput.pressSequentially('cat');
    await page.locator(FIND_OK_BTN).click();

    // In plain search mode both files match and are shown.
    const findPane = page.locator(FIND_PANE);
    await expect(findPane).toContainText('mixed.txt', { timeout: TIMEOUTS.fileOpen });
    await expect(findPane).toContainText('allnoop.txt');

    // Switch to replace mode and enter the replacement; the literal preview is
    // painted client-side from the cached search results.
    await page.locator(REPLACE_MODE_TOGGLE).click();
    const replaceInput = page.locator(REPLACE_INPUT);
    await replaceInput.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    await replaceInput.click();
    await replaceInput.pressSequentially('cat');

    // mixed.txt stays (its "Cat" match changes) but allnoop.txt is dropped
    // entirely. The preview inserts (<ins>) exactly one replacement -- for the
    // "Cat" match -- and not for either no-op "cat".
    await expect(findPane).toContainText('mixed.txt', { timeout: TIMEOUTS.fileOpen });
    await expect(findPane).not.toContainText('allnoop.txt');
    await expect(findPane.locator('ins')).toHaveCount(1);
  });
});

// Regression test for #17845: a Find in Files "Replace All" must rewrite the
// file with every match replaced -- and must not leave it truncated/emptied.
// The disk-full failure mode that caused the truncation can't be reproduced
// here, so this guards the streaming -> buffered-atomic-write refactor by
// driving a real replace end to end and asserting the on-disk result.
//
// @desktop_only: the assertion reads the project file straight off disk via
// Node fs, which only sees the rsession's filesystem when they're co-located.
test.describe('Find in Files: Replace All', { tag: ['@desktop_only'] }, () => {
  test.afterAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
  });

  test('replaces every match and writes the result to disk', async ({ rstudioPage: page }) => {
    // Open a project so the Find in Files dialog defaults its search scope to
    // the project directory (no need to drive the read-only directory chooser).
    const projectDir = await createAndOpenProject(page, sandbox.dir, 'find_replace_project');

    // Create hello.txt on disk inside the project, with three matches.
    const helloPath = path.join(projectDir, 'hello.txt');
    fs.writeFileSync(helloPath, 'hello hello hello\n');

    // Find "hello" across the project.
    await executeCommand(page, 'findInFiles');
    const searchInput = page.locator(SEARCH_INPUT);
    await searchInput.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    await searchInput.click();
    await searchInput.pressSequentially('hello');
    await page.locator(FIND_OK_BTN).click();

    // Wait for the results to include our file before switching to replace.
    const findPane = page.locator(FIND_PANE);
    await expect(findPane).toContainText('hello.txt', { timeout: TIMEOUTS.fileOpen });

    // Switch to Replace mode, enter the replacement, and Replace All.
    await page.locator(REPLACE_MODE_TOGGLE).click();
    const replaceInput = page.locator(REPLACE_INPUT);
    await replaceInput.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    await replaceInput.click();
    await replaceInput.pressSequentially('goodbye');

    await page.locator(REPLACE_ALL_BTN).click();
    await page.locator(CONFIRM_YES_BTN).click();

    // The file is rewritten with every match replaced -- and, crucially, not
    // truncated or emptied (the #17845 regression).
    await expect
      .poll(() => fs.readFileSync(helloPath, 'utf8').trim(), { timeout: TIMEOUTS.fileOpen })
      .toBe('goodbye goodbye goodbye');
  });
});

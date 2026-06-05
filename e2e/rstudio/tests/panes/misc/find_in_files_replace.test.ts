import { test, expect } from '@fixtures/rstudio.fixture';
import * as fs from 'fs';
import * as path from 'path';
import { executeCommand } from '@utils/commands';
import { createAndOpenProject, closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS } from '@utils/constants';

// -- Selectors ----------------------------------------------------------------

// Find in Files results pane (workbench_panel_ + idSafeString("Find in Files")).
const FIND_PANE = '#rstudio_workbench_panel_find_in_files';
// Search-pattern box in the Find in Files dialog (ElementIds.FIND_FILES_TEXT).
const SEARCH_INPUT = '#rstudio_find_files_text';
// Replace-mode toggle, replace box, and Replace All button in the find pane
// (ElementIds.FIND_REPLACE_MODE_TOGGLE / FIND_REPLACE_TEXT / FIND_REPLACE_ALL).
const REPLACE_MODE_TOGGLE = '#rstudio_find_replace_mode_toggle';
const REPLACE_INPUT = '#rstudio_find_replace_text';
const REPLACE_ALL_BTN = '#rstudio_find_replace_all';
// Standard modal buttons (pages/modals.page.ts).
const FIND_OK_BTN = '#rstudio_dlg_ok';
const CONFIRM_YES_BTN = '#rstudio_dlg_yes';

const sandbox = useSuiteSandbox();

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

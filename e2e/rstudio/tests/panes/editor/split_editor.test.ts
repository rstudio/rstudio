import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { executeCommand } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import type { AceEditorElement } from '@utils/ace';
import type { Locator, Page } from '@playwright/test';

// https://github.com/rstudio/rstudio/issues/2129
test.describe('Split editor', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  const FILE = 'split_editor.R';

  // The marker comment identifies the source editor views in the DOM; it is
  // never executed, so it cannot leak into the console's Ace instance.
  const CONTENT = heredoc`
    # split_view_marker
    split_x <- 10
    split_y <- 20
  `;

  // Both views of the document render the marker on their first (visible)
  // row; the primary view precedes the split view in the DOM.
  function sourceViews(page: Page): Locator {
    return page.locator('.ace_editor').filter({ hasText: 'split_view_marker' });
  }

  // The shared document's contents, read through the active editor.
  function documentValue(page: Page): Promise<string> {
    return page.evaluate(() => window.rstudio?.documents.activeEditor()?.getValue() ?? '');
  }

  function viewEditor(view: Locator) {
    return {
      cursorRow: () =>
        view.evaluate((el) => (el as AceEditorElement).env?.editor?.getCursorPosition().row ?? -1),
      gotoLine: (line: number) =>
        view.evaluate((el, ln) => (el as AceEditorElement).env?.editor?.gotoLine(ln, 0), line),
      navigateFileEnd: () =>
        view.evaluate((el) => (el as AceEditorElement).env?.editor?.navigateFileEnd()),
      hasFocus: () => view.evaluate((el) => el.contains(document.activeElement)),
      click: () => view.locator('.ace_content').click({ force: true }),
    };
  }

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    if ((await sourceViews(page).count()) > 1)
      await executeCommand(page, 'removeEditorSplit');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE]);
  });

  test('splitting shows a second synchronized view of the document', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILE, CONTENT);
    const views = sourceViews(page);
    await expect(views).toHaveCount(1);

    await executeCommand(page, 'splitEditorRight');
    await expect(views).toHaveCount(2);

    // Type into the split view; the shared document must pick up the edit.
    const splitView = viewEditor(views.nth(1));
    await splitView.click();
    await splitView.navigateFileEnd();
    await page.keyboard.press('Enter');
    await page.keyboard.type('split_z <- 30');
    await expect.poll(() => documentValue(page)).toContain('split_z <- 30');

    // Undo initiated from the primary view must unwind the split view's
    // edit, since the views share one undo history. Typing produces several
    // undo groups, so undo repeatedly until the edit is fully unwound.
    await viewEditor(views.first()).click();
    await expect
      .poll(async () => {
        await page.keyboard.press('ControlOrMeta+z');
        return documentValue(page);
      })
      .not.toContain('split_z');

    // Removing the split returns to a single view.
    await executeCommand(page, 'removeEditorSplit');
    await expect(views).toHaveCount(1);

    // Toggling restores the most recent split orientation.
    await executeCommand(page, 'toggleEditorSplit');
    await expect(views).toHaveCount(2);
    await executeCommand(page, 'toggleEditorSplit');
    await expect(views).toHaveCount(1);
  });

  test('views have independent cursors and focus toggles between them', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILE, CONTENT);
    const views = sourceViews(page);

    await executeCommand(page, 'splitEditorDown');
    await expect(views).toHaveCount(2);

    const primary = viewEditor(views.first());
    const split = viewEditor(views.nth(1));

    // Place the primary view's cursor on row 1, the split view's on row 2.
    await primary.click();
    await primary.gotoLine(2);
    await split.click();
    await split.gotoLine(3);

    await expect.poll(() => primary.cursorRow()).toBe(1);
    await expect.poll(() => split.cursorRow()).toBe(2);

    // focusOtherEditorSplit moves focus to the view that is not focused.
    await expect.poll(() => split.hasFocus()).toBe(true);
    await executeCommand(page, 'focusOtherEditorSplit');
    await expect.poll(() => split.hasFocus()).toBe(false);
    await executeCommand(page, 'focusOtherEditorSplit');
    await expect.poll(() => split.hasFocus()).toBe(true);

    // activateSource (Ctrl+1) toggles between the views too, when a source
    // editor already holds focus.
    await executeCommand(page, 'activateSource');
    await expect.poll(() => split.hasFocus()).toBe(false);
    await executeCommand(page, 'activateSource');
    await expect.poll(() => split.hasFocus()).toBe(true);
  });

  test('running code follows the focused view', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILE, CONTENT);
    const views = sourceViews(page);

    await executeCommand(page, 'splitEditorRight');
    await expect(views).toHaveCount(2);

    // Clear any leftover state from earlier runs of this test.
    await consoleActions.evalRLogical(
      '{ rm(list = intersect(c("split_x", "split_y"), ls(globalenv())), envir = globalenv()); TRUE }');

    // Cursor on 'split_x <- 10' (row 1) in the primary view, and on
    // 'split_y <- 20' (row 2) in the split view, which keeps focus.
    const primary = viewEditor(views.first());
    const split = viewEditor(views.nth(1));
    await primary.click();
    await primary.gotoLine(2);
    await split.click();
    await split.gotoLine(3);

    // Run Line must execute the focused (split) view's line, not the
    // primary view's. (RStudio binds Run Line to plain Ctrl+Enter on all
    // platforms, including macOS.)
    await page.keyboard.press('Control+Enter');

    await expect
      .poll(() => consoleActions.evalRLogical('exists("split_y") && !exists("split_x")'))
      .toBe(true);
  });
});

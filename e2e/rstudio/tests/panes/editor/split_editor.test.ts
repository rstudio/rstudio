import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { executeCommand, setPref, clearPref } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { heredoc } from '@utils/heredoc';
import type { AceEditorElement } from '@utils/ace';
import type { Locator, Page } from '@playwright/test';

// https://github.com/rstudio/rstudio/issues/2129
test.describe('Split editor', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  const FILE = 'split_editor.R';

  // The marker comment identifies the source editor views in the DOM.
  const CONTENT = heredoc`
    # split_view_marker
    split_x <- 10
    split_y <- 20
  `;

  // Both views of the document render the marker on their first (visible)
  // row; the primary view precedes the split view in the DOM. Scoped to the
  // source panel: on Server, writeAndOpenFile falls back to a console
  // writeLines() whose echoed command leaves the marker sitting in the
  // console's own Ace instance.
  function sourceViews(page: Page): Locator {
    return page
      .locator("[class*='rstudio_source_panel'] .ace_editor")
      .filter({ hasText: 'split_view_marker' });
  }

  // The shared document's contents, read through the active editor. Throws
  // rather than returning a fallback, so content assertions cannot pass
  // vacuously when no editor is active.
  function documentValue(page: Page): Promise<string> {
    return page.evaluate(() => {
      const editor = window.rstudio?.documents.activeEditor();
      if (!editor)
        throw new Error('no active editor');
      return editor.getValue();
    });
  }

  // The two views' bounding boxes, for asserting the split orientation.
  async function viewBoxes(page: Page) {
    const views = sourceViews(page);
    const primary = await views.first().boundingBox();
    const split = await views.nth(1).boundingBox();
    if (!primary || !split)
      throw new Error('expected two visible editor views');
    return { primary, split };
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

    // A right split lays the views out side by side.
    const boxes = await viewBoxes(page);
    expect(boxes.split.x).toBeGreaterThan(boxes.primary.x);
    expect(Math.abs(boxes.split.y - boxes.primary.y)).toBeLessThan(2);

    // Type into the split view; the shared document must pick up the edit.
    const splitView = viewEditor(views.nth(1));
    await splitView.click();
    await splitView.navigateFileEnd();
    await page.keyboard.press('Enter');
    await page.keyboard.type('split_z <- 30');
    await expect.poll(() => documentValue(page)).toContain('split_z <- 30');

    // Undo initiated from the primary view must unwind the split view's
    // edit, since the views share one undo history. Assert the focus move
    // landed, so the undos genuinely originate from the primary view.
    // Typing produces several undo groups, so undo repeatedly until the
    // edit is fully unwound.
    const primaryView = viewEditor(views.first());
    await primaryView.click();
    await expect.poll(() => primaryView.hasFocus()).toBe(true);
    await expect
      .poll(async () => {
        await page.keyboard.press('ControlOrMeta+z');
        return documentValue(page);
      })
      .not.toContain('split_z');

    // Only the split view's edit is unwound; the original content survives,
    // guarding against an over-undo.
    await expect.poll(() => documentValue(page)).toContain('split_x <- 10');

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

    // A down split stacks the views.
    const boxes = await viewBoxes(page);
    expect(boxes.split.y).toBeGreaterThan(boxes.primary.y);
    expect(Math.abs(boxes.split.x - boxes.primary.x)).toBeLessThan(2);

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

    // Toggling after a down split restores the remembered *down*
    // orientation, not the default right split.
    await executeCommand(page, 'removeEditorSplit');
    await expect(views).toHaveCount(1);
    await executeCommand(page, 'toggleEditorSplit');
    await expect(views).toHaveCount(2);
    const restored = await viewBoxes(page);
    expect(restored.split.y).toBeGreaterThan(restored.primary.y);
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

  test('closing a split document releases its editor state', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILE, CONTENT);
    const views = sourceViews(page);

    await executeCommand(page, 'splitEditorRight');
    await expect(views).toHaveCount(2);

    // Close the tab (and delete the file) without removing the split first.
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE]);
    await expect(views).toHaveCount(0);

    // Exercise the tab-close-while-split path (the rest of the suite always
    // removes the split first), then rebroadcast an editor pref. Teardown
    // regressions that leave pref bindings firing against a torn-down split
    // view surface here as recorded client exceptions, which fail the test.
    await setPref(page, 'show_invisibles', true);
    await clearPref(page, 'show_invisibles');
  });

  test('the split is restored when the IDE reloads', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILE, CONTENT);
    const views = sourceViews(page);

    // The split renders only after the editorSplit document property has
    // persisted server-side, so a visible split implies it survives a
    // reload.
    await executeCommand(page, 'splitEditorDown');
    await expect(views).toHaveCount(2);

    await page.reload();
    await page.waitForFunction(() => window.rstudio?.ready === true, null, { timeout: 30000 });

    // The restored split is applied through the deferred-layout path
    // (the property arrives before the editor has been laid out). The
    // document itself restores asynchronously after `ready`, so allow the
    // same budget as a file open.
    await expect(views).toHaveCount(2, { timeout: TIMEOUTS.fileOpen });
    const boxes = await viewBoxes(page);
    expect(boxes.split.y).toBeGreaterThan(boxes.primary.y);
  });
});

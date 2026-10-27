import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { collectClientStatePushes, waitForActiveDocIdPush } from '@utils/client-state';
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
      // the scroller is the visible viewport; .ace_content spans the longest
      // line, so its center can fall outside a narrow view
      click: () => view.locator('.ace_scroller').click({ force: true }),
      select: (range: { start: { row: number; column: number }; end: { row: number; column: number } }) =>
        view.evaluate((el, r) => (el as AceEditorElement).env?.editor?.selection.setRange(r), range),
      selectedText: () =>
        view.evaluate((el) => (el as AceEditorElement).env?.editor?.getSelectedText() ?? ''),
      rangeCount: () =>
        view.evaluate((el) => (el as AceEditorElement).env?.editor?.selection.rangeCount ?? 0),
      foldCount: () =>
        view.evaluate((el) => (el as AceEditorElement).env?.editor?.session.getAllFolds().length ?? -1),
    };
  }

  // Two functions: the primary view's cursor rests in cmd_a and the focused
  // split view selects 'y<-y*2' in cmd_b, so a command routed to the wrong
  // view changes cmd_a instead.
  const COMMAND_CONTENT = heredoc`
    # split_view_marker
    cmd_a <- function(x) {
      x<-x+1
      x
    }
    cmd_b <- function(y) {
      y<-y*2
      y
    }
  `;
  const PRIMARY_LINE = '  x<-x+1';

  async function openWithFocusedSplit(page: Page) {
    await writeAndOpenFile(page, sandbox.dir, FILE, COMMAND_CONTENT);
    const views = sourceViews(page);
    await executeCommand(page, 'splitEditorRight');
    await expect(views).toHaveCount(2);

    const primary = viewEditor(views.first());
    const split = viewEditor(views.nth(1));
    await primary.click();
    await primary.gotoLine(3);
    await split.click();
    await split.select({ start: { row: 6, column: 2 }, end: { row: 6, column: 8 } });
    await expect.poll(() => split.hasFocus()).toBe(true);
    return { primary, split };
  }

  async function documentLine(page: Page, row: number): Promise<string> {
    return (await documentValue(page)).split('\n')[row];
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

  for (const splitHistory of ['no', 'older']) {
    test(`selecting a statement preserves Re-Run Previous with ${splitHistory} split history`, async ({ rstudioPage: page }) => {
      await writeAndOpenFile(page, sandbox.dir, FILE, CONTENT);
      const views = sourceViews(page);

      await executeCommand(page, 'splitEditorRight');
      await expect(views).toHaveCount(2);
      const primary = viewEditor(views.first());
      const split = viewEditor(views.nth(1));

      const clearVariables = () => consoleActions.executeInConsole(
        'rm(list = intersect(c("split_x", "split_y"), ls(globalenv())), envir = globalenv())',
        { wait: true },
      );
      await clearVariables();

      // Cover both an empty execution history and an older execution in the
      // split. Neither may replace the primary view's most recent run.
      if (splitHistory === 'older') {
        await split.click();
        await split.gotoLine(3);
        await executeCommand(page, 'executeCurrentStatement');
        await expect.poll(() => consoleActions.evalRLogical('exists("split_y")')).toBe(true);
      }

      await primary.click();
      await primary.gotoLine(2);
      await executeCommand(page, 'executeCurrentStatement');
      await expect.poll(() => consoleActions.evalRLogical('exists("split_x")')).toBe(true);

      // Removing the results makes it observable which statement is rerun.
      // A console command does not change the source editor's run history.
      await clearVariables();
      await split.click();
      await split.gotoLine(3);
      await expect.poll(() => split.hasFocus()).toBe(true);
      await executeCommand(page, 'selectCurrentStatement');
      await expect.poll(() => views.nth(1).evaluate(
        (el) => (el as AceEditorElement).env?.editor?.getSelectedText(),
      )).toBe('split_y <- 20');

      await executeCommand(page, 'executeLastCode');
      await expect
        .poll(() => consoleActions.evalRLogical('exists("split_x") || exists("split_y")'))
        .toBe(true);
      expect(await consoleActions.evalRLogical('exists("split_x") && !exists("split_y")')).toBe(true);
    });
  }

  // https://github.com/rstudio/rstudio/issues/19298
  const EDITING_CASES = [
    {
      name: 'Comment/Uncomment shortcut',
      run: (page: Page) => page.keyboard.press('ControlOrMeta+Shift+C'),
      expected: /^\s*#\s*y<-y\*2$/,
    },
    {
      name: 'Reformat Selection',
      run: (page: Page) => executeCommand(page, 'reformatCode'),
      expected: /^\s*y <- y \* 2$/,
    },
  ];

  for (const editing of EDITING_CASES) {
    test(`${editing.name} edits the focused view's selection`, async ({ rstudioPage: page }) => {
      await openWithFocusedSplit(page);
      await editing.run(page);
      await expect.poll(() => documentLine(page, 6)).toMatch(editing.expected);
      expect(await documentLine(page, 2)).toBe(PRIMARY_LINE);
    });
  }

  test('folding and scope navigation follow the focused view', async ({ rstudioPage: page }) => {
    const { primary, split } = await openWithFocusedSplit(page);

    // Folds are per view: the split view folds, the primary view does not.
    await executeCommand(page, 'fold');
    await expect.poll(() => split.foldCount()).toBe(1);
    expect(await primary.foldCount()).toBe(0);
    await executeCommand(page, 'unfold');
    await expect.poll(() => split.foldCount()).toBe(0);

    // Go to Start of Scope moves the split view's cursor to cmd_b's opening
    // line, leaving the primary view's cursor inside cmd_a.
    await executeCommand(page, 'goToStartOfCurrentScope');
    await expect.poll(() => split.cursorRow()).toBe(5);
    expect(await primary.cursorRow()).toBe(2);
  });

  test('Rename in Scope selects in the focused view', async ({ rstudioPage: page }) => {
    const { primary, split } = await openWithFocusedSplit(page);

    // Renaming cmd_b's 'y' selects each of its occurrences in the split view.
    await executeCommand(page, 'renameInScope');
    await expect.poll(() => split.rangeCount()).toBe(4);
    expect(await primary.rangeCount()).toBeLessThan(2);
    await page.keyboard.press('Escape');
  });

  test('Use Selection for Find searches for the focused view\'s selection', async ({ rstudioPage: page }) => {
    const { primary } = await openWithFocusedSplit(page);

    // The find bar searches the primary view, so the split view's selection
    // carries over to it as the current match, and focus follows it there.
    await executeCommand(page, 'findFromSelection');
    await expect.poll(() => primary.selectedText()).toBe('y<-y*2');
    expect(await primary.cursorRow()).toBe(6);
    await expect.poll(() => primary.hasFocus()).toBe(true);
    await page.keyboard.press('Escape');
  });

  test('Go to Line moves the focused view and is recorded in navigation history', async ({ rstudioPage: page }) => {
    const { primary, split } = await openWithFocusedSplit(page);

    await executeCommand(page, 'goToLine');
    await expect.poll(() => page.evaluate(() => document.activeElement?.tagName)).toBe('INPUT');
    await page.keyboard.type('2');
    await page.keyboard.press('Enter');
    await expect.poll(() => split.cursorRow()).toBe(1);
    expect(await primary.cursorRow()).toBe(2);

    // Back returns the split view to where Go to Line left from, and Forward
    // repeats the jump; the primary view stays put throughout.
    await executeCommand(page, 'sourceNavigateBack');
    await expect.poll(() => split.cursorRow()).toBe(6);
    await executeCommand(page, 'sourceNavigateForward');
    await expect.poll(() => split.cursorRow()).toBe(1);
    expect(await primary.cursorRow()).toBe(2);
  });

  test('Vim reflow (gqq) reflows the focused view\'s line', async ({ rstudioPage: page }) => {
    const LONG = 'aaa bbb ccc ddd eee fff ggg hhh iii jjj kkk lll mmm nnn ooo ppp qqq rrr sss ttt';
    const PRIMARY_COMMENT = `# primary ${LONG}`;
    const SPLIT_COMMENT = `# split ${LONG}`;
    const NEXT_COMMENT = '# next line in the same comment block';

    try {
      await setPref(page, 'editor_keybindings', 'vim');
      await writeAndOpenFile(page, sandbox.dir, FILE, heredoc`
        # split_view_marker
        ${PRIMARY_COMMENT}
        reflow_x <- 1
        ${SPLIT_COMMENT}
        ${NEXT_COMMENT}
      `);
      const views = sourceViews(page);
      await executeCommand(page, 'splitEditorRight');
      await expect(views).toHaveCount(2);

      const primary = viewEditor(views.first());
      const split = viewEditor(views.nth(1));
      await primary.click();
      await primary.gotoLine(2);
      await split.click();
      await page.keyboard.press('Escape');
      await split.gotoLine(4);
      await expect.poll(() => split.hasFocus()).toBe(true);

      // Only the split view's cursor line is wrapped: the rest of its comment
      // block and the primary view's line are untouched.
      await page.keyboard.type('gqq', { delay: 20 });
      await expect.poll(async () => (await documentValue(page)).split('\n')).not.toContain(SPLIT_COMMENT);
      const lines = (await documentValue(page)).split('\n');
      expect(lines).toContain(PRIMARY_COMMENT);
      expect(lines).toContain(NEXT_COMMENT);
    } finally {
      await clearPref(page, 'editor_keybindings');
    }
  });

  test('rstudioapi replaces the focused view\'s selection', async ({ rstudioPage: page }) => {
    await openWithFocusedSplit(page);

    // The console takes focus to run this; the split view remains the
    // document's active view, as for an addin run from the console.
    await consoleActions.executeInConsole(
      '.rs.api.selectionSet("y <- y * 3", id = .rs.api.getSourceEditorContext()$id)',
      { wait: true },
    );
    await expect.poll(() => documentLine(page, 6)).toBe('  y <- y * 3');
    expect(await documentLine(page, 2)).toBe(PRIMARY_LINE);
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
    const clientStatePushes = collectClientStatePushes(page);

    await writeAndOpenFile(page, sandbox.dir, FILE, CONTENT);
    const views = sourceViews(page);

    // The split renders only after the editorSplit document property has
    // persisted server-side, so a visible split implies it survives a
    // reload.
    await executeCommand(page, 'splitEditorDown');
    await expect(views).toHaveCount(2);

    // The active tab is persisted as the "activeTabDocId" client state
    // value, pushed on a passive ~5s timer. Reloading before that push
    // restores a stale value: the document then comes back as a background
    // tab, which is never laid out, so neither its text nor the split
    // (applied on layout) ever renders and the view count stays at 0.
    const docId = await page.evaluate(() => window.rstudio?.documents.active()?.id ?? null);
    expect(docId).not.toBeNull();
    await waitForActiveDocIdPush(page, clientStatePushes, docId as string);

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

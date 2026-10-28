import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { collectClientStatePushes, waitForActiveDocIdPush } from '@utils/client-state';
import { executeCommand, setPref, clearPref } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { heredoc } from '@utils/heredoc';
import type { AceEditorElement } from '@utils/ace';
import type { Locator, Page, Route } from '@playwright/test';

// https://github.com/rstudio/rstudio/issues/2129
test.describe('Split editor', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  const FILE = 'split_editor.R';
  const RMD_FILE = 'split_editor.Rmd';
  const INSERT_FILE = 'split_editor_insert.txt';

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
      scrollLeft: () =>
        view.evaluate((el) => {
          const renderer = (el as AceEditorElement).env?.editor?.renderer as { scrollLeft?: number } | undefined;
          return renderer?.scrollLeft ?? -1;
        }),
      // the text of each range of a multi-selection (single-line ranges)
      rangeTexts: () =>
        view.evaluate((el) => {
          const editor = (el as AceEditorElement).env?.editor;
          return (editor?.selection.rangeList.ranges ?? []).map((r) =>
            editor?.session.getLine(r.start.row).slice(r.start.column, r.end.column) ?? '');
        }),
      foldCount: () =>
        view.evaluate((el) => (el as AceEditorElement).env?.editor?.session.getAllFolds().length ?? -1),
      hasGhostText: () =>
        view.evaluate((el) => (el as AceEditorElement).env?.editor?.renderer.$ghostText != null),
      rowFullyVisible: (row: number) =>
        view.evaluate((el, r) => (el as AceEditorElement).env?.editor?.isRowFullyVisible(r) ?? false, row),
    };
  }

  type ViewEditor = ReturnType<typeof viewEditor>;

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

  // Runs Go to Line through its prompt, in whichever view has focus.
  async function goToLineViaPrompt(page: Page, line: number) {
    await executeCommand(page, 'goToLine');
    await expect.poll(() => page.evaluate(() => document.activeElement?.tagName)).toBe('INPUT');
    await page.keyboard.type(String(line));
    await page.keyboard.press('Enter');
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
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE, RMD_FILE, INSERT_FILE]);
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

  test('Rename in Scope in another view does not reuse cached chunk ranges', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, RMD_FILE, heredoc`
      ---
      title: "split_view_marker"
      ---

      ${'```'}{r}
      x <- 1
      y <- x + 1
      y + x
      ${'```'}
    `);
    const views = sourceViews(page);
    await executeCommand(page, 'splitEditorRight');
    await expect(views).toHaveCount(2);
    const primary = viewEditor(views.first());
    const split = viewEditor(views.nth(1));

    // Renaming x twice in the primary view caches its chunk ranges for a
    // second; renaming y in the split view within that window must not reuse
    // them. The sequence runs in one synchronous evaluation, so the cache
    // cannot expire part way through and let the test pass without the reset.
    await primary.click();
    await expect.poll(() => primary.hasFocus()).toBe(true);
    const splitElement = await views.nth(1).elementHandle();
    await views.first().evaluate((primaryEl, splitEl) => {
      const primaryEditor = (primaryEl as AceEditorElement).env?.editor;
      const splitEditor = (splitEl as AceEditorElement | null)?.env?.editor;
      const commands = window.rstudio?.commands;
      if (!primaryEditor || !splitEditor || !commands)
        throw new Error('editor views or command bridge unavailable');

      primaryEditor.focus();
      primaryEditor.gotoLine(6, 0);
      commands.renameInScope();
      commands.renameInScope();

      splitEditor.focus();
      splitEditor.gotoLine(7, 0);
      commands.renameInScope();
    }, splitElement);

    await expect.poll(() => split.rangeTexts()).toEqual(['y', 'y']);
    await page.keyboard.press('Escape');
  });

  test('Use Selection for Find searches for the focused view\'s selection', async ({ rstudioPage: page }) => {
    const { primary, split } = await openWithFocusedSplit(page);

    // Fold both functions in the primary view only (folds are per view),
    // then restore the split view's focus and selection.
    await primary.click();
    await executeCommand(page, 'foldAll');
    await expect.poll(() => primary.foldCount()).toBe(2);
    await split.click();
    await split.select({ start: { row: 6, column: 2 }, end: { row: 6, column: 8 } });
    await expect.poll(() => split.hasFocus()).toBe(true);

    // The find bar searches the primary view, so the split view's selection
    // carries over to it as the current match -- unfolding cmd_b to show
    // it -- and focus follows it there.
    await executeCommand(page, 'findFromSelection');
    await expect.poll(() => primary.selectedText()).toBe('y<-y*2');
    expect(await primary.cursorRow()).toBe(6);
    expect(await primary.foldCount()).toBe(1);
    await expect.poll(() => primary.hasFocus()).toBe(true);
    await page.keyboard.press('Escape');
  });

  test('Go to Line moves the focused view and is recorded in navigation history', async ({ rstudioPage: page }) => {
    const { primary, split } = await openWithFocusedSplit(page);

    await goToLineViaPrompt(page, 2);
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

  test('Back reveals a position restored into the other view', async ({ rstudioPage: page }) => {
    const LONG_LINE = `long_line <- "${'a'.repeat(200)}"`;
    await writeAndOpenFile(page, sandbox.dir, FILE, heredoc`
      # split_view_marker
      ${LONG_LINE}
      short_line <- 1
    `);
    const views = sourceViews(page);
    await executeCommand(page, 'splitEditorRight');
    await expect(views).toHaveCount(2);
    const primary = viewEditor(views.first());
    const split = viewEditor(views.nth(1));

    // Leave the end of the long line for the last line in the split view.
    await split.click();
    const lineEnd = { row: 1, column: LONG_LINE.length };
    await split.select({ start: lineEnd, end: lineEnd });
    await goToLineViaPrompt(page, 3);
    await expect.poll(() => split.cursorRow()).toBe(2);

    // Back from the primary view, scrolled to the left, restores the far end
    // of the long line there and must scroll it into sight.
    await primary.click();
    await expect.poll(() => primary.hasFocus()).toBe(true);
    expect(await primary.scrollLeft()).toBe(0);
    await executeCommand(page, 'sourceNavigateBack');
    await expect.poll(() => primary.cursorRow()).toBe(1);
    await expect.poll(() => primary.scrollLeft()).toBeGreaterThan(0);
  });

  test('Vim :read inserts into the view it ran in when focus moves before the file arrives', async ({ rstudioPage: page }) => {
    const GET_FILE_CONTENTS = /\/rpc\/get_file_contents(?:\?|$)/;
    const insertPath = `${sandbox.dir}/${INSERT_FILE}`;

    // Hold the file contents request until focus has moved to the primary view.
    const parked: Route[] = [];
    await page.route(GET_FILE_CONTENTS, (route) => {
      parked.push(route);
    });
    const release = async (): Promise<void> => {
      await page.unroute(GET_FILE_CONTENTS);
      for (const route of parked.splice(0))
        await route.continue().catch(() => {});
    };

    try {
      await setPref(page, 'editor_keybindings', 'vim');
      await consoleActions.executeInConsole(
        `writeLines("INSERTED", ${JSON.stringify(insertPath)})`,
        { wait: true },
      );

      const { primary, split } = await openWithFocusedSplit(page);
      const cursor = { row: 6, column: 0 };
      await split.select({ start: cursor, end: cursor });
      await page.keyboard.press('Escape');
      await page.keyboard.type(`:r ${insertPath}`);
      await page.keyboard.press('Enter');
      await expect.poll(() => parked.length).toBe(1);

      await primary.click();
      await primary.gotoLine(2);
      await expect.poll(() => primary.hasFocus()).toBe(true);
      await release();

      await expect.poll(() => documentLine(page, 6)).toBe('INSERTED');
      expect(await documentLine(page, 1)).toBe('cmd_a <- function(x) {');
    } finally {
      await release();
      await clearPref(page, 'editor_keybindings');
    }
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

  test('Use Selection for Find without a selection leaves the primary view alone', async ({ rstudioPage: page }) => {
    const { primary, split } = await openWithFocusedSplit(page);
    const cursor = { row: 6, column: 2 };
    await split.select({ start: cursor, end: cursor });

    // An empty selection starts no search, so nothing carries over to the
    // primary view and focus stays in the split view. The command runs
    // synchronously, so its effects are settled once it returns.
    await executeCommand(page, 'findFromSelection');
    expect(await primary.cursorRow()).toBe(2);
    expect(await split.hasFocus()).toBe(true);
  });

  test('Toggle Breakpoint uses the focused view\'s cursor row', async ({ rstudioPage: page }) => {
    await openWithFocusedSplit(page);

    // Breakpoints live on the primary editor (the split view draws none), so
    // the marker lands in the primary view's gutter, on the split view's row.
    const markers = sourceViews(page).first().locator(".ace_gutter-cell[class*='breakpoint']");
    await executeCommand(page, 'debugBreakpoint');
    await expect(markers).toHaveCount(1);
    await expect(markers).toHaveText('7');

    await executeCommand(page, 'debugBreakpoint');
    await expect(markers).toHaveCount(0);
  });

  test('Switch Focus between Source/Console leaves the focused split view', async ({ rstudioPage: page }) => {
    const { split } = await openWithFocusedSplit(page);

    await executeCommand(page, 'switchFocusSourceConsole');
    await expect.poll(() =>
      page.evaluate(() => document.activeElement?.closest('#rstudio_console_input') != null)).toBe(true);
    expect(await split.hasFocus()).toBe(false);
  });

  // Whether the active document has unsaved changes.
  function activeDocDirty(page: Page) {
    return page.evaluate(() => window.rstudio?.documents.active()?.dirty ?? null);
  }

  test('autosave on blur saves an edit made in the split view', async ({ rstudioPage: page }) => {
    try {
      await setPref(page, 'auto_save_on_blur', true);
      await openWithFocusedSplit(page);

      await page.keyboard.type('y<-y*5');
      await expect.poll(() => activeDocDirty(page)).toBe(true);

      // leaving the split view for the console saves the document
      await executeCommand(page, 'activateConsole');
      await expect.poll(() => activeDocDirty(page), { timeout: TIMEOUTS.fileOpen }).toBe(false);
    } finally {
      await clearPref(page, 'auto_save_on_blur');
    }
  });

  test('idle autosave keeps trailing whitespace under both views\' cursors', async ({ rstudioPage: page }) => {
    const PRIMARY_LINE_WS = 'keep_primary <- 1  ';
    const OTHER_LINE_WS = 'strip_other <- 2  ';
    const TYPED = 'keep_split';

    try {
      await writeAndOpenFile(page, sandbox.dir, FILE, heredoc`
        # split_view_marker
        ${PRIMARY_LINE_WS}
        ${OTHER_LINE_WS}
        ${TYPED}
      `);
      const views = sourceViews(page);
      await executeCommand(page, 'splitEditorRight');
      await expect(views).toHaveCount(2);
      const primary = viewEditor(views.first());
      const split = viewEditor(views.nth(1));

      // The primary view's cursor rests at the end of its line; the split
      // view types a trailing space onto another line, then idles.
      await primary.click();
      const primaryEnd = { row: 1, column: PRIMARY_LINE_WS.length };
      await primary.select({ start: primaryEnd, end: primaryEnd });
      await split.click();
      const splitEnd = { row: 3, column: TYPED.length };
      await split.select({ start: splitEnd, end: splitEnd });

      // Turn on stripping only once the cursors are in place, so that no
      // autosave runs while they are still being positioned.
      await setPref(page, 'strip_trailing_whitespace', true);
      await setPref(page, 'auto_save_on_idle', 'commit');
      await expect.poll(() => split.hasFocus()).toBe(true);
      await page.keyboard.type(' <- ');
      await expect.poll(() => activeDocDirty(page)).toBe(true);
      await expect.poll(() => activeDocDirty(page), { timeout: TIMEOUTS.fileOpen }).toBe(false);

      // Only the line under neither cursor is stripped.
      expect(await documentLine(page, 1)).toBe(PRIMARY_LINE_WS);
      expect(await documentLine(page, 2)).toBe(OTHER_LINE_WS.trimEnd());
      expect(await documentLine(page, 3)).toBe(`${TYPED} <- `);
    } finally {
      await clearPref(page, 'auto_save_on_idle');
      await clearPref(page, 'strip_trailing_whitespace');
    }
  });

  test('saving with whitespace stripping keeps the split view\'s selection and folds', async ({ rstudioPage: page }) => {
    const STRIP_LINE_WS = 'strip_me <- 1  ';
    const TYPED = 'keep_split';

    try {
      await setPref(page, 'strip_trailing_whitespace', true);
      await writeAndOpenFile(page, sandbox.dir, FILE, heredoc`
        # split_view_marker
        ${STRIP_LINE_WS}
        ${TYPED}
        fold_me <- function() {
          1
        }
        last_line <- 2
        last_line
      `);
      const views = sourceViews(page);
      await executeCommand(page, 'splitEditorRight');
      await expect(views).toHaveCount(2);
      const split = viewEditor(views.nth(1));

      await split.click();
      const typedEnd = { row: 2, column: TYPED.length };
      await split.select({ start: typedEnd, end: typedEnd });
      await expect.poll(() => split.hasFocus()).toBe(true);
      await page.keyboard.type('X');

      // Collapse fold_me in the split view, then select across it.
      await executeCommand(page, 'foldAll');
      await expect.poll(() => split.foldCount()).toBe(1);
      await split.select({ start: { row: 2, column: 0 }, end: { row: 5, column: 1 } });

      // Stripping replaces the whole shared document, which would otherwise
      // move the split view's cursor to the end of the file and drop its
      // folds; restoring the selection must not unfold them either.
      await executeCommand(page, 'saveSourceDoc');
      await expect.poll(() => activeDocDirty(page), { timeout: TIMEOUTS.fileOpen }).toBe(false);
      expect(await documentLine(page, 1)).toBe(STRIP_LINE_WS.trimEnd());
      expect(await split.cursorRow()).toBe(5);
      expect(await split.foldCount()).toBe(1);
    } finally {
      await clearPref(page, 'strip_trailing_whitespace');
    }
  });

  test('Shrink Selection in the split view forgets ranges from before an edit', async ({ rstudioPage: page }) => {
    const { split } = await openWithFocusedSplit(page);

    await executeCommand(page, 'expandSelection');
    await expect.poll(() => split.selectedText()).not.toBe('y<-y*2');

    // Insert a line at the top, shifting the rows the expansion recorded.
    const top = { row: 0, column: 0 };
    await split.select({ start: top, end: top });
    await page.keyboard.press('Enter');
    await expect.poll(() => documentLine(page, 0)).toBe('');

    // With the history cleared, Shrink Selection leaves the empty selection;
    // stale history would select text at the recorded (now shifted) range.
    await executeCommand(page, 'shrinkSelection');
    expect(await split.selectedText()).toBe('');
  });

  test('Expand Selection leaves Ace\'s Editor.prototype without an event registry', async ({ rstudioPage: page }) => {
    await openWithFocusedSplit(page);
    await executeCommand(page, 'expandSelection');

    // A listener registered on the prototype gives it an event registry that
    // every editor created afterwards shares, so focusing a later split view
    // also fired its primary view's focus handler and misrouted commands.
    const prototypeHasRegistry = await page.evaluate(() => {
      const ace = window as unknown as { require: (id: string) => { Editor: { prototype: object } } };
      return Object.prototype.hasOwnProperty.call(ace.require('ace/editor').Editor.prototype, '_eventRegistry');
    });
    expect(prototypeHasRegistry).toBe(false);
  });

  test('Go to End of Scope finds the end of a long function in a new split view', async ({ rstudioPage: page }) => {
    // Longer than the 30 rows past the cursor that a scope lookup builds.
    // Joined at the template's indent, which heredoc then strips.
    const BODY = Array.from({ length: 40 }, (_, i) => `  v${i} <- ${i}`).join('\n      ');
    await writeAndOpenFile(page, sandbox.dir, FILE, heredoc`
      # split_view_marker
      long_fn <- function() {
      ${BODY}
      }
    `);
    const views = sourceViews(page);
    await executeCommand(page, 'splitEditorRight');
    await expect(views).toHaveCount(2);

    // Located by class rather than by the marker text, which scrolls out of
    // the split view once it jumps to the end of the function.
    const split = viewEditor(page.locator(
      "[class*='rstudio_source_panel'] .ace_editor.rstudio_editor_split_view:visible"));

    await split.click();
    await split.gotoLine(3);
    await expect.poll(() => split.hasFocus()).toBe(true);
    await executeCommand(page, 'goToEndOfCurrentScope');
    await expect.poll(() => split.cursorRow()).toBe(42);
    await expect.poll(() => split.rowFullyVisible(42)).toBe(true);
  });

  // rstudioapi writes act on the view getActiveDocumentContext() reads from.
  // The console takes focus to run each call; the split view remains the
  // document's active view, as for an addin run from the console.
  const API_CASES: { name: string; code: string; check: (page: Page, split: ViewEditor) => Promise<void> }[] = [
    {
      name: 'selectionSet() replaces the focused view\'s selection',
      code: '.rs.api.selectionSet("y <- y * 3", id = .rs.api.getSourceEditorContext()$id)',
      check: async (page) => {
        await expect.poll(() => documentLine(page, 6)).toBe('  y <- y * 3');
      },
    },
    {
      name: 'insertText() replaces the focused view\'s selection',
      code: '.rs.api.insertText("y <- y * 4", id = .rs.api.getSourceEditorContext()$id)',
      check: async (page) => {
        await expect.poll(() => documentLine(page, 6)).toBe('  y <- y * 4');
      },
    },
    {
      name: 'setSelectionRanges() selects in the focused view',
      code: '.rs.api.setSelectionRanges(c(8, 3, 8, 4), id = .rs.api.getSourceEditorContext()$id)',
      check: async (_page, split) => {
        await expect.poll(() => split.selectedText()).toBe('y');
        expect(await split.cursorRow()).toBe(7);
      },
    },
    {
      name: 'setGhostText() shows ghost text in the focused view',
      code: '.rs.api.setGhostText("# ghost")',
      check: async (_page, split) => {
        await expect.poll(() => split.hasGhostText()).toBe(true);
      },
    },
  ];

  for (const api of API_CASES) {
    test(`rstudioapi ${api.name}`, async ({ rstudioPage: page }) => {
      const { primary, split } = await openWithFocusedSplit(page);

      await consoleActions.executeInConsole(api.code, { wait: true });
      await api.check(page, split);

      // the primary view's line, cursor, and lack of ghost text are untouched
      expect(await documentLine(page, 2)).toBe(PRIMARY_LINE);
      expect(await primary.cursorRow()).toBe(2);
      expect(await primary.hasGhostText()).toBe(false);
    });
  }

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

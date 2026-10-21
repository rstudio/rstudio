// The editor gutter marks lines that differ from the Git index (#2846): a bar
// beside added and modified lines, and a wedge at the seam where lines were
// removed. The index version is fetched once per document and again when the
// index moves (commit, stage, checkout); the diff against the live buffer runs
// client-side, so the markers follow typing without a save. Next/Previous
// Change step through the markers, Revert Change at Cursor restores the index
// version, and clicking a marker peeks at the previous lines. The repo is built
// R-side so this also works in Server mode, where the rsession may not share a
// filesystem with the test runner.
import { test, expect } from '@fixtures/rstudio.fixture';
import type { Locator, Page, Request, Response, Route } from '@playwright/test';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { CONSOLE_OUTPUT, executeInConsole } from '@pages/console_pane.page';
import { clearPref, documentCloseAllNoSave, executeCommand, isCommandEnabled, openProject, setPref } from '@utils/commands';
import { openFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral, rStringLiteral } from '@utils/r';
import type { AceEditorElement } from '@utils/ace';

const PROJECT_NAME = 'GitGutter';
const PREF = 'show_git_diff_gutter';

// Class names the acesupport GitDiffGutter puts on gutter cells (and on the
// gutter layer itself while attached); see git_diff_gutter.js.
const GUTTER_ATTACHED = 'ace_git-gutter';
const ADDED = 'ace_git-added';
const MODIFIED = 'ace_git-modified';
const DELETED = 'ace_git-deleted';

// The peek widget and its parts; see git_diff_gutter.js.
const PEEK = '.ace_git-peek';
const PEEK_LABEL = '.ace_git-peek-label';
const PEEK_LINE = '.ace_git-peek-line';
const PEEK_REVERT = '.ace_git-peek-revert';

const SEED_LINES = ['a <- 1', 'b <- 2', 'c <- 3', 'd <- 4', 'e <- 5', 'f <- 6', 'g <- 7', 'h <- 8'];

// The working copy after the uncommitted edits: a line inserted after the
// first, the fourth changed, the sixth removed.
const EDITED_LINES = ['a <- 1', 'a2 <- 1.5', 'b <- 2', 'c <- 3', 'd <- 40', 'e <- 5', 'g <- 7', 'h <- 8'];

// Expected markers, by 0-based row of the edited file. The removed `f <- 6`
// leaves nothing to mark, so the wedge sits on the line that now follows it.
const EDITED_MARKERS = { 1: [ADDED], 4: [MODIFIED], 6: [DELETED] };

/**
 * executeInConsole only confirms that R reached a new prompt, which an R error
 * does just as readily as a success; scrape the output this command produced
 * for an error header so a failed setup step fails here, not as a later
 * unrelated-looking timeout.
 */
async function executeInConsoleChecked(
  page: Page,
  command: string,
  opts: { timeout?: number } = {},
): Promise<void> {
  const before = await page.locator(CONSOLE_OUTPUT).innerText();
  await executeInConsole(page, command, { wait: true, timeout: opts.timeout });
  const after = await page.locator(CONSOLE_OUTPUT).innerText();

  const emitted = after.startsWith(before) ? after.slice(before.length) : after;
  const errMatch = emitted.match(/(?:Error in|Error:)[^\n]*(?:\n[ \t]+[^\n]*)*/);
  if (errMatch) {
    throw new Error(
      `R error running "${command.slice(0, 120)}": ${errMatch[0].replace(/\n+/g, ' | ').trim()}`,
    );
  }
}

/**
 * The Git marker classes on each rendered gutter cell of the active editor,
 * keyed by document row; rows without a marker are left out. Only rendered
 * cells are inspected, which covers every row of the short files used here.
 */
async function gutterMarkers(page: Page): Promise<Record<number, string[]>> {
  return page.evaluate(() => {
    type Cell = { row: number; element: HTMLElement };
    type GutterEditor = { renderer: { $gutterLayer: { $lines: { cells: Cell[] } } } };

    const editor = window.rstudio?.documents.activeEditor() as unknown as GutterEditor | null;
    if (!editor) return {};

    const markers: Record<number, string[]> = {};
    for (const cell of editor.renderer.$gutterLayer.$lines.cells) {
      const classes = Array.from(cell.element.classList)
        .filter((cls) => cls.startsWith('ace_git-'))
        .sort();
      if (classes.length > 0) markers[cell.row] = classes;
    }
    return markers;
  });
}

/** 0-based cursor row of the active editor. */
async function cursorRow(page: Page): Promise<number> {
  return new AceEditor(page, '').getCursorPosition().then((pos) => pos.row);
}

/** gutterMarkers, for one editor view's element rather than the active editor. */
async function viewMarkers(view: Locator): Promise<Record<number, string[]>> {
  return view.evaluate((el) => {
    type Cell = { row: number; element: HTMLElement };
    type GutterEditor = { renderer: { $gutterLayer: { $lines: { cells: Cell[] } } } };

    const editor = (el as AceEditorElement).env?.editor as unknown as GutterEditor | undefined;
    if (!editor) return {};

    const markers: Record<number, string[]> = {};
    for (const cell of editor.renderer.$gutterLayer.$lines.cells) {
      const classes = Array.from(cell.element.classList)
        .filter((cls) => cls.startsWith('ace_git-'))
        .sort();
      if (classes.length > 0) markers[cell.row] = classes;
    }
    return markers;
  });
}

/** Whether the active editor's gutter layer has a GitDiffGutter attached. */
async function gutterAttached(page: Page): Promise<boolean> {
  return page.evaluate((attachedClass) => {
    type GutterEditor = { renderer: { $gutterLayer: { element: HTMLElement } } };
    const editor = window.rstudio?.documents.activeEditor() as unknown as GutterEditor | null;
    return editor?.renderer.$gutterLayer.element.classList.contains(attachedClass) ?? false;
  }, GUTTER_ATTACHED);
}

test.describe.serial('Git diff markers in the editor gutter (#2846)', () => {
  const sandbox = useSuiteSandbox();

  const projectDir = () => `${sandbox.dir}/${PROJECT_NAME}`.replace(/\\/g, '/');
  const scriptPath = () => `${projectDir()}/script.R`;

  // CI runners have no global git config, hence the inline -c identity
  const gitC = () => `"-C", shQuote(${rPathLiteral(projectDir())})`;
  const gitWho = `"-c", "user.name=rstudio-e2e", "-c", "user.email=rstudio-e2e@posit.co"`;

  // Commits the given files, or everything when none are given. Tests that
  // seed a file of their own commit just that file, so the uncommitted edits
  // to script.R that later tests rely on stay out of the index.
  async function commitAll(page: Page, message: string, paths: string[] = []): Promise<void> {
    const add = paths.length === 0
      ? '"-A"'
      : `"--", ${paths.map((path) => `shQuote(${rPathLiteral(path)})`).join(', ')}`;
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          s <- c(
            system2("git", c(${gitC()}, "add", ${add})),
            system2("git", c(${gitC()}, ${gitWho}, "commit", "-m", shQuote(${rStringLiteral(message)}), "--quiet"))
          )
          if (any(s != 0))
            stop("git commit failed (exit status: ", paste(s, collapse = "/"), ")")
        }
      `,
      { timeout: 60000 },
    );
  }

  test.beforeAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
    await setPref(page, PREF, true);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await setPref(page, PREF, true).catch(() => undefined);
    await closeProjectIfOpen(page).catch((err) => {
      console.warn(`[git_diff_gutter] project close failed: ${(err as Error).message}`);
    });
  });

  test('marks added, modified, and removed lines against the index', async ({
    rstudioPage: page,
  }) => {
    test.setTimeout(180_000);

    const rprojPath = `${projectDir()}/${PROJECT_NAME}.Rproj`;

    // The repo has to exist before the project opens: RStudio decides whether
    // the project is version controlled at session start.
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          stopifnot(dir.create(${rPathLiteral(projectDir())}, recursive = TRUE))
          writeLines("Version: 1.0", ${rPathLiteral(rprojPath)})
          writeLines(c(${SEED_LINES.map(rStringLiteral).join(', ')}), ${rPathLiteral(scriptPath())})
        }
      `,
    );
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          s <- c(
            system2("git", c(${gitC()}, "init", "--quiet")),
            system2("git", c(${gitC()}, "add", "-A")),
            system2("git", c(${gitC()}, ${gitWho}, "commit", "-m", "seed", "--quiet"))
          )
          if (any(s != 0))
            stop("git seed failed (exit status: ", paste(s, collapse = "/"), ")")
        }
      `,
      { timeout: 60000 },
    );

    // the uncommitted edits the markers should reflect
    await executeInConsoleChecked(
      page,
      `writeLines(c(${EDITED_LINES.map(rStringLiteral).join(', ')}), ${rPathLiteral(scriptPath())})`,
    );

    await openProject(page, rprojPath);
    await openFile(page, scriptPath());

    // The markers arrive after a round trip for the index version (and, for
    // the first document in a session, the diff extension's script load).
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);
  });

  // The fixture closes every document between tests, so each one reopens the
  // script; the project (and the working copy) carry over.
  test('follows edits as they are typed, without a save', async ({ rstudioPage: page }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    const editor = new AceEditor(page, '');
    await editor.gotoLine(EDITED_LINES.length);
    await editor.navigateLineEnd();
    await editor.insert('\ni <- 9');

    // the new last line is an addition; everything above is unchanged
    await expect
      .poll(() => gutterMarkers(page), { timeout: 30000 })
      .toEqual({ ...EDITED_MARKERS, [EDITED_LINES.length]: [ADDED] });
  });

  test('steps through the changes with Next and Previous Change', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    const editor = new AceEditor(page, '');
    await editor.gotoLine(1);

    // forward through the three changes, then around to the first again
    for (const row of [1, 4, 6, 1]) {
      await executeCommand(page, 'vcsNextChange');
      await expect.poll(() => cursorRow(page)).toBe(row);
    }

    // and back, wrapping from the first to the last
    for (const row of [6, 4, 1]) {
      await executeCommand(page, 'vcsPreviousChange');
      await expect.poll(() => cursorRow(page)).toBe(row);
    }
  });

  test('reverts the change at the cursor to the index version', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    const editor = new AceEditor(page, '');

    // the modified line goes back to what the index has
    await editor.gotoLine(5);
    await executeCommand(page, 'vcsRevertChange');
    await expect.poll(() => editor.getLine(4)).toBe('d <- 4');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 1: [ADDED], 6: [DELETED] });

    // the removed line comes back where it was, ahead of the marked line
    await editor.gotoLine(7);
    await executeCommand(page, 'vcsRevertChange');
    await expect.poll(() => editor.getLine(6)).toBe('f <- 6');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 1: [ADDED] });

    // and the added line goes away, leaving the index version exactly
    await editor.gotoLine(2);
    await executeCommand(page, 'vcsRevertChange');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({});
    expect((await editor.getValue()).split('\n')).toEqual([...SEED_LINES, '']);

    // nothing under the cursor is a no-op
    await executeCommand(page, 'vcsRevertChange');
    expect((await editor.getValue()).split('\n')).toEqual([...SEED_LINES, '']);

    // Emptying the buffer is one deletion, and reverting it brings the whole
    // index version back (an empty document counts as no lines, not one).
    await editor.setValue('');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 0: [DELETED] });
    await editor.gotoLine(1);
    await executeCommand(page, 'vcsRevertChange');
    await expect.poll(() => editor.getValue()).toBe(SEED_LINES.join('\n') + '\n');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({});

    // The buffer now matches the index but the working copy doesn't; don't
    // leave that behind for the tests below, which reopen the file.
    await documentCloseAllNoSave(page);
  });

  test('reverts against the current document, not a stale diff', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    // Delete the modified line and revert at once, before the debounced diff
    // has caught up. The revert must act on the deletion now at row 4, not
    // on the modification the stale diff still has there -- which would
    // have swallowed the unchanged `e <- 5` below it.
    const editor = new AceEditor(page, '');
    await editor.gotoLine(5);
    await page.evaluate(() => {
      const ed = window.rstudio!.documents.activeEditor()!;
      ed.execCommand('removeline');
      window.rstudio!.commands.vcsRevertChange();
    });
    await expect.poll(() => editor.getLine(4)).toBe('d <- 4');
    expect(await editor.getLine(5)).toBe('e <- 5');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 1: [ADDED], 6: [DELETED] });

    await documentCloseAllNoSave(page);
  });

  test('peeks at the previous lines when a marker is clicked', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    // The marker is a bar at the right edge of the gutter cell for row 4
    // (the modified line); the left part of the cell toggles breakpoints.
    const cell = page.locator('.ace_gutter-cell.ace_git-modified').first();
    const box = (await cell.boundingBox())!;
    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);

    const peek = page.locator(PEEK);
    await expect(peek).toBeVisible({ timeout: 10000 });
    await expect(peek.locator(PEEK_LABEL)).toHaveText('Changed 1 line; previously:');
    await expect(peek.locator(PEEK_LINE)).toHaveText(['d <- 4']);
    expect(await gutterMarkers(page)).toEqual(EDITED_MARKERS);

    // the second click on the same marker closes the peek again
    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
    await expect(peek).toHaveCount(0);

    // reverting from the peek restores the line and closes the peek
    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
    await expect(peek).toBeVisible({ timeout: 10000 });
    await peek.locator(PEEK_REVERT).click();
    await expect(peek).toHaveCount(0);
    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getLine(4)).toBe('d <- 4');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 1: [ADDED], 6: [DELETED] });

    await documentCloseAllNoSave(page);
  });

  test('shows and hides the change at the cursor from the keyboard', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    // the marker is a mouse target; the command opens the same peek for a
    // keyboard user, so what Revert would restore can be seen first
    const editor = new AceEditor(page, '');
    await editor.gotoLine(5);
    await executeCommand(page, 'vcsShowChange');
    const peek = page.locator(PEEK);
    await expect(peek).toBeVisible({ timeout: 10000 });
    await expect(peek.locator(PEEK_LABEL)).toHaveText('Changed 1 line; previously:');
    await expect(peek.locator(PEEK_LINE)).toHaveText(['d <- 4']);

    // the same command toggles it closed
    await executeCommand(page, 'vcsShowChange');
    await expect(peek).toHaveCount(0);

    // a removal's peek lists the removed lines
    await editor.gotoLine(7);
    await executeCommand(page, 'vcsShowChange');
    await expect(peek).toBeVisible({ timeout: 10000 });
    await expect(peek.locator(PEEK_LABEL)).toHaveText('Removed 1 line');
    await expect(peek.locator(PEEK_LINE)).toHaveText(['f <- 6']);

    // on an unchanged line the command has nothing to show, and leaves an
    // open peek alone
    await editor.gotoLine(3);
    await executeCommand(page, 'vcsShowChange');
    await expect(peek).toHaveCount(1);
    await expect(peek.locator(PEEK_LINE)).toHaveText(['f <- 6']);

    await documentCloseAllNoSave(page);
  });

  test('survives an edit that removes the peek\'s row', async ({ rstudioPage: page }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    const cell = page.locator('.ace_gutter-cell.ace_git-modified').first();
    const box = (await cell.boundingBox())!;
    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
    await expect(page.locator(PEEK)).toBeVisible({ timeout: 10000 });

    // Deleting from the row above the peek's across it takes its widget
    // with it (Ace drops widgets from the row after the deletion's start);
    // as the only widget, that leaves Ace with no widget collection at all.
    // The gutter must keep rendering (the fixture fails the test on a client
    // exception), the peek must be gone, and the markers must follow.
    const collectionAfterRemove = await page.evaluate(() => {
      type Pos = { row: number; column: number };
      const editor = window.rstudio!.documents.activeEditor() as unknown as {
        session: { doc: { remove(range: { start: Pos; end: Pos }): void }; lineWidgets: unknown };
      };
      editor.session.doc.remove({ start: { row: 3, column: 0 }, end: { row: 6, column: 0 } });
      return editor.session.lineWidgets;
    });
    expect(collectionAfterRemove).toBeNull();
    await expect(page.locator(PEEK)).toHaveCount(0, { timeout: 10000 });
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 1: [ADDED], 3: [DELETED] });

    // and the gutter is still live: a further edit (to the last line, whose
    // neighbors are unchanged, so it stays a chunk of its own) is marked
    const editor = new AceEditor(page, '');
    await editor.gotoLine(5);
    await editor.navigateLineEnd();
    await editor.insert('0');
    await expect
      .poll(() => gutterMarkers(page), { timeout: 30000 })
      .toEqual({ 1: [ADDED], 3: [DELETED], 4: [MODIFIED] });

    await documentCloseAllNoSave(page);
  });

  test('does not toggle a fold when the marker of a foldable line is clicked', async ({
    rstudioPage: page,
  }) => {
    // a committed function whose first line is then changed: the marker and
    // Ace's fold control share the right edge of the gutter cell
    const foldPath = `${projectDir()}/fold.R`;
    await executeInConsoleChecked(
      page,
      `writeLines(c("f <- function() {", "  x <- 1", "}"), ${rPathLiteral(foldPath)})`,
    );
    await commitAll(page, 'fold', [foldPath]);
    await executeInConsoleChecked(
      page,
      `writeLines(c("f <- function(y) {", "  x <- 1", "}"), ${rPathLiteral(foldPath)})`,
    );
    await openFile(page, foldPath);
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 0: [MODIFIED] });

    const editor = new AceEditor(page, '');
    const cell = page.locator('.ace_gutter-cell.ace_git-modified').first();
    const box = (await cell.boundingBox())!;
    await page.mouse.dblclick(box.x + box.width - 3, box.y + box.height / 2);

    // a double click is a mousedown/click pair twice: the peek opens and then
    // closes, and the function stays unfolded throughout
    await expect(page.locator(PEEK)).toHaveCount(0);
    expect(await editor.getFoldCount()).toBe(0);

    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
    await expect(page.locator(PEEK)).toBeVisible({ timeout: 10000 });
    expect(await editor.getFoldCount()).toBe(0);

    // The fold arrow itself still folds: it sits just left of the 6px of
    // padding the attached gutter adds, i.e. about 12px in from the edge,
    // and clicking it must not open (or close) a peek.
    await page.mouse.click(box.x + box.width - 12, box.y + box.height / 2);
    await expect.poll(() => editor.getFoldCount()).toBe(1);
    await expect(page.locator(PEEK)).toBeVisible();

    await documentCloseAllNoSave(page);
    await executeInConsoleChecked(page, `invisible(file.remove(${rPathLiteral(foldPath)}))`);
    await commitAll(page, 'remove fold', [foldPath]);
  });

  test('closes cleanly after being folded away', async ({ rstudioPage: page }) => {
    // A fold whose last row carries the peek gets the peek's widget aliased
    // at the fold's start row (that is how Ace shows it while folded), and
    // Ace's removal clears only the widget's own row. Closing the peek has
    // to sweep the alias too, or the next render brings the peek back with
    // nothing left to close it.
    const foldPath = `${projectDir()}/fold.R`;
    await executeInConsoleChecked(
      page,
      `writeLines(c("f <- function() {", "  x <- 1", "}"), ${rPathLiteral(foldPath)})`,
    );
    await commitAll(page, 'fold', [foldPath]);
    await executeInConsoleChecked(
      page,
      `writeLines(c("f <- function() {", "  x <- 1", "} # end"), ${rPathLiteral(foldPath)})`,
    );
    await openFile(page, foldPath);
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 2: [MODIFIED] });

    const cell = page.locator('.ace_gutter-cell.ace_git-modified').first();
    const box = (await cell.boundingBox())!;
    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
    await expect(page.locator(PEEK)).toBeVisible({ timeout: 10000 });

    const editor = new AceEditor(page, '');
    await editor.gotoLine(1);
    await editor.execCommand('toggleFoldWidget');
    await expect.poll(() => editor.getFoldCount()).toBe(1);

    // the peek shows at the fold while folded, and closes from there
    await expect(page.locator(PEEK)).toBeVisible();
    await page.locator(PEEK).locator('.ace_git-peek-close').click();
    await expect(page.locator(PEEK)).toHaveCount(0);

    // nothing of it survives unfolding and a few renders
    await editor.gotoLine(1);
    await editor.execCommand('toggleFoldWidget');
    await expect.poll(() => editor.getFoldCount()).toBe(0);
    await editor.gotoLine(3);
    const deadline = Date.now() + 1500;
    while (Date.now() < deadline) {
      expect(await page.locator(PEEK).count()).toBe(0);
      await page.waitForTimeout(250);
    }
    expect(
      await page.evaluate(() => {
        const ed = window.rstudio!.documents.activeEditor() as unknown as {
          session: { lineWidgets?: ({ el?: HTMLElement } | undefined)[] | null };
        };
        return (ed.session.lineWidgets ?? []).filter((w) => w?.el?.classList.contains('ace_git-peek')).length;
      }),
    ).toBe(0);

    // and a fresh peek still works
    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
    await expect(page.locator(PEEK)).toBeVisible({ timeout: 10000 });

    await documentCloseAllNoSave(page);
    await executeInConsoleChecked(page, `invisible(file.remove(${rPathLiteral(foldPath)}))`);
    await commitAll(page, 'remove fold again', [foldPath]);
  });

  test('matches the editor\'s line-ending normalization and empty files', async ({
    rstudioPage: page,
  }) => {
    // CR-only endings are normalized to LF when a document loads, so the
    // index version must be too, or every line reads as modified
    const crPath = `${projectDir()}/cr.R`;
    const emptyPath = `${projectDir()}/empty.R`;
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          writeBin(charToRaw("a <- 1\rb <- 2\r"), ${rPathLiteral(crPath)})
          writeBin(raw(0), ${rPathLiteral(emptyPath)})
        }
      `,
    );
    await commitAll(page, 'cr and empty', [crPath, emptyPath]);

    await openFile(page, crPath);
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);
    const deadline = Date.now() + 3000;
    while (Date.now() < deadline) {
      expect(await gutterMarkers(page)).toEqual({});
      await page.waitForTimeout(250);
    }

    // typing into a tracked empty file is an addition, not a modification of
    // its (nonexistent) first line
    await openFile(page, emptyPath);
    const editor = new AceEditor(page, '');
    await editor.insert('x <- 1');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 0: [ADDED] });
    await executeCommand(page, 'vcsRevertChange');
    await expect.poll(() => editor.getValue()).toBe('');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({});

    await documentCloseAllNoSave(page);
  });

  test('honors the ignore-whitespace diff preference', async ({ rstudioPage: page }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    // Trailing whitespace on an otherwise unchanged line -- one whose
    // neighbors are unchanged too, since touching changes merge into a
    // single chunk and would turn the added line above into "modified".
    const editor = new AceEditor(page, '');
    await editor.gotoLine(8);
    await editor.navigateLineEnd();
    await editor.insert('   ');
    await expect
      .poll(() => gutterMarkers(page), { timeout: 30000 })
      .toEqual({ ...EDITED_MARKERS, 7: [MODIFIED] });

    try {
      await setPref(page, 'git_diff_ignore_whitespace', true);
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

      // Whitespace inside a line is ignored too, as git diff -w does. This
      // one sits next to the modified line, so without the preference the
      // two would merge into a single two-line modification.
      await editor.gotoLine(4, 1);
      await editor.insert(' ');
      await expect.poll(() => editor.getLine(3)).toBe('c  <- 3');
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

      await setPref(page, 'git_diff_ignore_whitespace', false);
      await expect
        .poll(() => gutterMarkers(page), { timeout: 30000 })
        .toEqual({ ...EDITED_MARKERS, 3: [MODIFIED], 7: [MODIFIED] });
    } finally {
      await setPref(page, 'git_diff_ignore_whitespace', false);
    }

    await documentCloseAllNoSave(page);
  });

  test('leaves a symlinked index entry alone', async ({ rstudioPage: page }) => {
    test.skip(process.platform === 'win32', 'creating symlinks needs extra privileges on Windows');

    // git show gives a symlink's target path as its contents, while the editor
    // shows the target file; diffing the two would mark every line
    const aliasPath = `${projectDir()}/alias.R`;
    await executeInConsoleChecked(
      page,
      `stopifnot(file.symlink("script.R", ${rPathLiteral(aliasPath)}))`,
    );
    await commitAll(page, 'alias', [aliasPath]);

    await openFile(page, aliasPath);
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);
    const deadline = Date.now() + 3000;
    while (Date.now() < deadline) {
      expect(await gutterMarkers(page)).toEqual({});
      await page.waitForTimeout(250);
    }

    await documentCloseAllNoSave(page);

    // A symlink whose name reads as a glob, next to a regular file the glob
    // matches: the index lookup has to be literal and exact, or the regular
    // file's entry would vouch for the symlink.
    const globLinkPath = `${projectDir()}/report[1].R`;
    const globFilePath = `${projectDir()}/report1.R`;
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          writeLines("r <- 1", ${rPathLiteral(globFilePath)})
          stopifnot(file.symlink("script.R", ${rPathLiteral(globLinkPath)}))
        }
      `,
    );
    await commitAll(page, 'glob alias', [globFilePath, globLinkPath]);
    await openFile(page, globLinkPath);
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);
    const globDeadline = Date.now() + 3000;
    while (Date.now() < globDeadline) {
      expect(await gutterMarkers(page)).toEqual({});
      await page.waitForTimeout(250);
    }

    await documentCloseAllNoSave(page);
    await executeInConsoleChecked(
      page,
      `invisible(file.remove(c(${rPathLiteral(aliasPath)}, ${rPathLiteral(globLinkPath)}, ${rPathLiteral(globFilePath)})))`,
    );
    await commitAll(page, 'remove aliases', [aliasPath, globLinkPath, globFilePath]);
  });

  test('reads a file named like a stage selector as itself', async ({ rstudioPage: page }) => {
    test.skip(process.platform === 'win32', 'colons are not allowed in Windows file names');

    // "git show :0:script.R" would mean stage 0 of script.R; a tracked file
    // literally named 0:script.R has to be looked up as that file
    const stagePath = `${projectDir()}/0:script.R`;
    await executeInConsoleChecked(page, `writeLines("s <- 0", ${rPathLiteral(stagePath)})`);
    await commitAll(page, 'stage-like name', [stagePath]);

    await openFile(page, stagePath);
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);
    const deadline = Date.now() + 3000;
    while (Date.now() < deadline) {
      expect(await gutterMarkers(page)).toEqual({});
      await page.waitForTimeout(250);
    }

    // and a revert restores this file's line, not script.R's
    const editor = new AceEditor(page, '');
    await editor.gotoLine(1);
    await editor.navigateLineEnd();
    await editor.insert('1');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 0: [MODIFIED] });
    await executeCommand(page, 'vcsRevertChange');
    await expect.poll(() => editor.getLine(0)).toBe('s <- 0');

    await documentCloseAllNoSave(page);
    await executeInConsoleChecked(page, `invisible(file.remove(${rPathLiteral(stagePath)}))`);
    await commitAll(page, 'remove stage-like name', [stagePath]);
  });

  test('reads the index through the file\'s smudge filter', async ({ rstudioPage: page }) => {
    // The editor shows the smudged working copy, so the base has to come
    // through the same filters (git-crypt, LFS, ident) or every line would
    // read as changed and the peek would show the stored form. A rot13
    // clean/smudge pair stands in for them here.
    const attrsPath = `${projectDir()}/.gitattributes`;
    const cipherPath = `${projectDir()}/cipher.txt`;
    const rot13 = 'tr A-Za-z N-ZA-Mn-za-m';
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          s <- c(
            system2("git", c(${gitC()}, "config", "filter.rot.clean", shQuote(${rStringLiteral(rot13)}))),
            system2("git", c(${gitC()}, "config", "filter.rot.smudge", shQuote(${rStringLiteral(rot13)})))
          )
          if (any(s != 0))
            stop("git config failed (exit status: ", paste(s, collapse = "/"), ")")
          writeLines("cipher.txt filter=rot", ${rPathLiteral(attrsPath)})
          writeLines(c("one <- 1", "two <- 2", "three <- 3"), ${rPathLiteral(cipherPath)})
        }
      `,
    );
    await commitAll(page, 'cipher', [attrsPath, cipherPath]);

    // the stored blob is rot13 of the text, as the filter is in effect
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          stored <- system2("git", c(${gitC()}, "show", ":0:cipher.txt"), stdout = TRUE)
          if (!identical(stored[[1]], "bar <- 1"))
            stop("the clean filter did not apply: ", stored[[1]])
          writeLines(c("one <- 1", "two <- 22", "three <- 3"), ${rPathLiteral(cipherPath)})
        }
      `,
    );

    try {
      await openFile(page, cipherPath);
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 1: [MODIFIED] });

      const editor = new AceEditor(page, '');
      await editor.gotoLine(2);
      await executeCommand(page, 'vcsShowChange');
      const peek = page.locator(PEEK);
      await expect(peek).toBeVisible({ timeout: 10000 });
      await expect(peek.locator(PEEK_LINE)).toHaveText(['two <- 2']);
    } finally {
      await documentCloseAllNoSave(page);
      await executeInConsoleChecked(
        page,
        `invisible(file.remove(c(${rPathLiteral(attrsPath)}, ${rPathLiteral(cipherPath)})))`,
      );
      await commitAll(page, 'remove cipher', [attrsPath, cipherPath]);
    }
  });

  test('keeps control characters the editor keeps', async ({ rstudioPage: page }) => {
    // U+001F survives document loading, so it has to survive the base too,
    // or the line reads as modified and a revert would drop the character.
    // ESC, on the other hand, is stripped when a document loads, so the base
    // must drop it as well (an ANSI-colored log is the usual case).
    const ctrlPath = `${projectDir()}/ctrl.txt`;
    await executeInConsoleChecked(
      page,
      `writeBin(charToRaw("a\u001fb\nc\u001b[31md\n"), ${rPathLiteral(ctrlPath)})`,
    );
    await commitAll(page, 'ctrl', [ctrlPath]);

    await openFile(page, ctrlPath);
    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getLine(0)).toBe('a\u001fb');
    await expect.poll(() => editor.getLine(1)).toBe('c[31md');
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);
    const deadline = Date.now() + 3000;
    while (Date.now() < deadline) {
      expect(await gutterMarkers(page)).toEqual({});
      await page.waitForTimeout(250);
    }

    // and a revert restores it rather than the filtered line
    await editor.gotoLine(1);
    await editor.navigateLineEnd();
    await editor.insert('!');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 0: [MODIFIED] });
    await executeCommand(page, 'vcsRevertChange');
    await expect.poll(() => editor.getLine(0)).toBe('a\u001fb');

    await documentCloseAllNoSave(page);
  });

  test('acts on the editor view holding the cursor when the editor is split', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    await executeCommand(page, 'splitEditorRight');
    const views = page
      .locator("[class*='rstudio_source_panel'] .ace_editor")
      .filter({ hasText: 'a2 <- 1.5' });
    await expect(views).toHaveCount(2);
    const primary = views.first();
    const split = views.nth(1);

    try {
      // both views carry the markers
      await expect.poll(() => viewMarkers(split), { timeout: 30000 }).toEqual(EDITED_MARKERS);
      expect(await viewMarkers(primary)).toEqual(EDITED_MARKERS);

      // primary cursor on the added line, split cursor on the modified one,
      // split focused last: the revert must take the modified line
      await primary.locator('.ace_content').click({ force: true });
      await primary.evaluate((el) => (el as AceEditorElement).env?.editor?.gotoLine(2, 0));
      await split.locator('.ace_content').click({ force: true });
      await split.evaluate((el) => (el as AceEditorElement).env?.editor?.gotoLine(5, 0));
      await expect.poll(() => split.evaluate((el) => el.contains(document.activeElement))).toBe(true);

      await executeCommand(page, 'vcsRevertChange');
      const editor = new AceEditor(page, '');
      await expect.poll(() => editor.getLine(4)).toBe('d <- 4');
      expect(await editor.getLine(1)).toBe('a2 <- 1.5');
      await expect.poll(() => viewMarkers(split), { timeout: 30000 }).toEqual({ 1: [ADDED], 6: [DELETED] });
      await expect.poll(() => viewMarkers(primary), { timeout: 30000 }).toEqual({ 1: [ADDED], 6: [DELETED] });

      // and Next Change moves the split view's cursor, from its own position
      await executeCommand(page, 'vcsNextChange');
      await expect
        .poll(() => split.evaluate((el) => (el as AceEditorElement).env?.editor?.getCursorPosition().row))
        .toBe(6);

      // Clicking into the split view re-fetches the base like the primary
      // does, so an index moved from outside the IDE is picked up there too.
      // Replace the index entry (without touching the working copy) with the
      // buffer's current contents except `d <- 41` for the reverted `d <- 4`.
      await primary.locator('.ace_content').click({ force: true });
      const indexLines = ['a <- 1', 'a2 <- 1.5', 'b <- 2', 'c <- 3', 'd <- 41', 'e <- 5', 'g <- 7', 'h <- 8'];
      await executeInConsoleChecked(
        page,
        heredoc`
          {
            blob <- system2(
              "git", c(${gitC()}, "hash-object", "-w", "--stdin"),
              input = c(${indexLines.map(rStringLiteral).join(', ')}), stdout = TRUE
            )
            s <- system2("git", c(${gitC()}, "update-index", "--cacheinfo",
                                  paste0("100644,", blob, ",script.R")))
            if (s != 0)
              stop("git update-index failed (exit status: ", s, ")")
          }
        `,
        { timeout: 60000 },
      );
      await split.locator('.ace_content').click({ force: true });
      await expect.poll(() => viewMarkers(split), { timeout: 30000 }).toEqual({ 4: [MODIFIED] });
      await expect.poll(() => viewMarkers(primary), { timeout: 30000 }).toEqual({ 4: [MODIFIED] });
    } finally {
      await executeInConsoleChecked(
        page,
        `invisible(system2("git", c(${gitC()}, "reset", "-q", "--", "script.R")))`,
      );
      await executeCommand(page, 'removeEditorSplit');
      await expect(views).toHaveCount(1);
      await documentCloseAllNoSave(page);
    }
  });

  test('disables the commands in visual mode', async ({ rstudioPage: page }) => {
    // the gutter belongs to the source editor, which visual mode hides
    const rmdPath = `${projectDir()}/doc.Rmd`;
    await executeInConsoleChecked(
      page,
      `writeLines(c("---", "title: x", "---", "", "One.", "", "Two."), ${rPathLiteral(rmdPath)})`,
    );
    await commitAll(page, 'rmd', [rmdPath]);
    await executeInConsoleChecked(
      page,
      `writeLines(c("---", "title: x", "---", "", "One!", "", "Two."), ${rPathLiteral(rmdPath)})`,
    );

    await openFile(page, rmdPath);
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 4: [MODIFIED] });
    await expect.poll(() => isCommandEnabled(page, 'vcsRevertChange')).toBe(true);

    const sourceActions = new SourcePaneActions(page, new ConsolePaneActions(page));
    await sourceActions.ensureVisualMode();
    await expect.poll(() => isCommandEnabled(page, 'vcsRevertChange')).toBe(false);
    await expect.poll(() => isCommandEnabled(page, 'vcsNextChange')).toBe(false);

    await sourceActions.ensureSourceMode();
    await expect.poll(() => isCommandEnabled(page, 'vcsRevertChange')).toBe(true);

    await documentCloseAllNoSave(page);
    await executeInConsoleChecked(page, `invisible(file.remove(${rPathLiteral(rmdPath)}))`);
    await commitAll(page, 'remove rmd', [rmdPath]);
  });

  test('re-fetches the base when the document is reopened with another encoding', async ({
    rstudioPage: page,
  }) => {
    // UTF-8 bytes reopened as Latin-1 decode differently in the buffer; the
    // base has to be decoded the same way or every non-ASCII line reads as
    // modified (and a revert would restore the UTF-8 decoding)
    const latinPath = `${projectDir()}/latin.R`;
    await executeInConsoleChecked(
      page,
      `writeBin(charToRaw(enc2utf8("x <- \\"caf\\u00e9\\"\\n")), ${rPathLiteral(latinPath)})`,
    );
    await commitAll(page, 'latin', [latinPath]);

    await openFile(page, latinPath);
    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getLine(0)).toBe('x <- "caf\u00e9"');
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);

    await executeCommand(page, 'reopenSourceDocWithEncoding');
    const encodings = page.locator('select[aria-label="Encodings"]');
    await expect(encodings).toBeVisible({ timeout: 30000 });
    await encodings.selectOption({ label: 'ISO-8859-1' });
    await page.locator('#rstudio_dlg_ok').click();

    // the two UTF-8 bytes of e-acute, read as Latin-1
    await expect.poll(() => editor.getLine(0), { timeout: 30000 }).toBe('x <- "caf\u00c3\u00a9"');
    const deadline = Date.now() + 3000;
    while (Date.now() < deadline) {
      expect(await gutterMarkers(page)).toEqual({});
      await page.waitForTimeout(250);
    }

    await documentCloseAllNoSave(page);
  });

  test('keeps a notebook chunk\'s output widget when a peek lands on its row', async ({
    rstudioPage: page,
  }) => {
    // Ace keeps one line widget per row and the notebook persists chunk
    // output by enumerating them, so a peek placed on a chunk's output row
    // would hide that output from the next save.
    // Inline chunk execution needs rmarkdown loadable, nothing more; the
    // harness's ensurePackages also walks rmarkdown's optional runtime
    // dependencies, which this does not depend on.
    const consoleActions = new ConsolePaneActions(page);
    const loadable = await consoleActions.evalRLogical('requireNamespace("rmarkdown", quietly = TRUE)');
    test.skip(loadable !== true, 'rmarkdown is not installed');

    const nbPath = `${projectDir()}/nb.Rmd`;
    const seed = ['---', 'title: x', '---', '', '```{r}', '1 + 1', '```', 'after', 'tail'];
    await executeInConsoleChecked(
      page,
      `writeLines(c(${seed.map(rStringLiteral).join(', ')}), ${rPathLiteral(nbPath)})`,
    );
    await commitAll(page, 'nb', [nbPath]);
    // the line after the chunk is removed: the deletion seam sits right
    // below the chunk's closing fence, where its output widget lives
    const edited = seed.filter((line) => line !== 'after');
    await executeInConsoleChecked(
      page,
      `writeLines(c(${edited.map(rStringLiteral).join(', ')}), ${rPathLiteral(nbPath)})`,
    );

    await openFile(page, nbPath);
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 7: [DELETED] });

    const widgets = () =>
      page.evaluate(() => {
        type Widget = { row: number; type?: string; el?: HTMLElement };
        const editor = window.rstudio?.documents.activeEditor() as unknown as {
          session: { lineWidgets?: (Widget | undefined)[] };
        } | null;
        return (editor?.session.lineWidgets ?? [])
          .filter((w): w is Widget => !!w)
          .map((w) => ({
            row: w.row,
            type: w.type ?? (w.el?.classList.contains('ace_git-peek') ? 'peek' : '?'),
          }));
      });

    // run the chunk so its output widget occupies the fence row (6)
    const editor = new AceEditor(page, '');
    await editor.gotoLine(6);
    await executeCommand(page, 'executeCurrentChunk');
    await expect.poll(widgets, { timeout: 60000 }).toContainEqual({ row: 6, type: 'ChunkOutput' });

    // The output widget registers before its contents arrive and keeps
    // growing as they do, pushing the rows below it down; click the marker
    // only once the output has rendered and the cell has stopped moving.
    await expect(page.locator('.ace_lineWidgetContainer').filter({ hasText: '[1] 2' })).toBeVisible({
      timeout: 60000,
    });
    const cell = page.locator('.ace_gutter-cell.ace_git-deleted').first();
    let box: { x: number; y: number; width: number; height: number } | null = null;
    await expect
      .poll(async () => {
        // the first sample only seeds the comparison: two reads a polling
        // interval apart have to agree, never two back-to-back ones
        const next = (await cell.boundingBox())!;
        const settled = box !== null && next.y === box.y && next.height === box.height;
        box = next;
        return settled;
      }, { timeout: 30000, intervals: [300] })
      .toBe(true);
    await page.mouse.click(box!.x + box!.width - 3, box!.y + box!.height / 2);
    await expect(page.locator(PEEK)).toBeVisible({ timeout: 10000 });

    // the peek took the row below the seam instead, and the output is intact
    expect(await widgets()).toEqual(
      expect.arrayContaining([{ row: 6, type: 'ChunkOutput' }, { row: 7, type: 'peek' }]),
    );

    // Saving with the peek open, then reopening, still restores the output.
    // Save needs a dirty document, so touch the title (its own chunk, so the
    // peek stays), and wait on the save RPC itself: a notebook stays dirty
    // after a save by design, so the dirty bit is no signal here.
    await editor.gotoLine(2);
    await editor.navigateLineEnd();
    await editor.insert('y');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 1: [MODIFIED], 7: [DELETED] });
    await expect(page.locator(PEEK)).toBeVisible();
    const saved = page.waitForResponse((r) => r.url().includes('save_document'));
    await executeCommand(page, 'saveSourceDoc');
    await saved;
    await documentCloseAllNoSave(page);
    await openFile(page, nbPath);
    await expect.poll(widgets, { timeout: 60000 }).toContainEqual({ row: 6, type: 'ChunkOutput' });

    await documentCloseAllNoSave(page);
    await executeInConsoleChecked(
      page,
      `invisible(system2("git", c(${gitC()}, "checkout", "--", "nb.Rmd")))`,
    );
  });

  test('drops the old base the moment Save As changes the path', async ({ rstudioPage: page }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    // Hold the new path's base fetch, so the window before it lands -- in
    // which the old file's base must already be gone -- is wide enough to
    // act in. The saved copy is untracked, so it never gets a base at all.
    const copyPath = `${projectDir()}/copy.R`;
    let release: () => void = () => undefined;
    const held = new Promise<void>((resolve) => {
      release = resolve;
    });
    // fallback, not continue: the fixture routes RPCs too, and the request
    // still has to pass through its handler
    await page.route('**/git_diff_base_contents', async (route) => {
      await held;
      await route.fallback();
    });

    try {
      await executeCommand(page, 'saveSourceDocAs');
      const fileName = page.locator('#file_dialog_name_prompt');
      await expect(fileName).toBeVisible({ timeout: 30000 });
      await fileName.fill(copyPath);
      await page.locator('#rstudio_file_accept_save').click();
      await expect(
        page.locator("[class*='rstudio_source_panel'] [class*='PanelTab-selected']"),
      ).toContainText('copy.R', { timeout: 30000 });

      // no base: no markers, and Revert has nothing to act on
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({});
      const editor = new AceEditor(page, '');
      await editor.gotoLine(5);
      await executeCommand(page, 'vcsRevertChange');
      expect(await editor.getLine(4)).toBe('d <- 40');
    } finally {
      release();
      await page.unroute('**/git_diff_base_contents');
    }

    // and once the fetch lands, the untracked copy stays unmarked
    const deadline = Date.now() + 3000;
    while (Date.now() < deadline) {
      expect(await gutterMarkers(page)).toEqual({});
      await page.waitForTimeout(250);
    }

    await documentCloseAllNoSave(page);
    await executeInConsoleChecked(page, `invisible(file.remove(${rPathLiteral(copyPath)}))`);
  });

  test('drops the old base when the document is renamed', async ({ rstudioPage: page }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    // a peek open on the modified line: its Revert button is reachable
    // without refocusing the editor, so a stale base would be actionable
    const cell = page.locator('.ace_gutter-cell.ace_git-modified').first();
    const box = (await cell.boundingBox())!;
    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
    await expect(page.locator(PEEK)).toBeVisible({ timeout: 10000 });

    // rename through the tab menu: a path change with no save of its own.
    // The renamed file is untracked, so there is no base for it at all.
    await executeCommand(page, 'renameSourceDoc');
    const prompt = page.locator('#rstudio_text_entry');
    await expect(prompt).toBeVisible({ timeout: 30000 });
    await prompt.fill('renamed.R');
    await page.locator('#rstudio_dlg_ok').click();
    await expect(
      page.locator("[class*='rstudio_source_panel'] [class*='PanelTab-selected']"),
    ).toContainText('renamed.R', { timeout: 30000 });

    await expect(page.locator(PEEK)).toHaveCount(0, { timeout: 30000 });
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({});
    const deadline = Date.now() + 3000;
    while (Date.now() < deadline) {
      expect(await gutterMarkers(page)).toEqual({});
      await page.waitForTimeout(250);
    }
    const editor = new AceEditor(page, '');
    await editor.gotoLine(5);
    await executeCommand(page, 'vcsRevertChange');
    expect(await editor.getLine(4)).toBe('d <- 40');

    // put script.R back, with the working-copy edits the tests below expect
    await documentCloseAllNoSave(page);
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          invisible(file.remove(${rPathLiteral(`${projectDir()}/renamed.R`)}))
          writeLines(c(${EDITED_LINES.map(rStringLiteral).join(', ')}), ${rPathLiteral(scriptPath())})
        }
      `,
    );
  });

  test('gives way to an inline equation preview on its row', async ({ rstudioPage: page }) => {
    // MathJax renders a display equation's preview into a line widget on the
    // closing fence's row, and takes whatever widget it finds there as its
    // own. A peek on that row is not; the preview must still render, the
    // peek closes, and the typesetting queue stays unblocked for the next
    // equation. Previews are off while the peek is opened, so that the row
    // is free for it, and turned on afterwards to force the collision.
    const mathPath = `${projectDir()}/math.Rmd`;
    const seed = ['---', 'title: x', '---', '', '$$', 'a = 1', '$$', '', 'text', '', '$$', 'b = 2', '$$'];
    await executeInConsoleChecked(
      page,
      `writeLines(c(${seed.map(rStringLiteral).join(', ')}), ${rPathLiteral(mathPath)})`,
    );
    await commitAll(page, 'math', [mathPath]);
    // the blank line after the first equation is removed: the deletion's
    // peek goes below the closing fence, where the preview wants to be
    const edited = [...seed.slice(0, 7), ...seed.slice(8)];
    await executeInConsoleChecked(
      page,
      `writeLines(c(${edited.map(rStringLiteral).join(', ')}), ${rPathLiteral(mathPath)})`,
    );

    const visibleMath = page.locator('.rstudio-mathjax-root mjx-container').filter({ visible: true });
    try {
      await setPref(page, 'latexPreviewOnCursorIdle', 'never');
      await openFile(page, mathPath);
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 7: [DELETED] });

      const cell = page.locator('.ace_gutter-cell.ace_git-deleted').first();
      const box = (await cell.boundingBox())!;
      await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
      await expect(page.locator(PEEK)).toBeVisible({ timeout: 10000 });
      expect(await visibleMath.count()).toBe(0);

      // Typing inside the equation re-renders an existing preview after a
      // pause; the peek on the closing fence's row is not one, so with
      // previews off nothing may appear (a popup, in this mode).
      await page.evaluate(() => {
        const editor = window.rstudio!.documents.activeEditor()!;
        editor.focus();
        editor.gotoLine(6, 5);
        editor.insert('1');
      });
      const quiet = Date.now() + 1500;
      while (Date.now() < quiet) {
        expect(await visibleMath.count()).toBe(0);
        await page.waitForTimeout(250);
      }
      await expect(page.locator(PEEK)).toBeVisible();

      await setPref(page, 'latexPreviewOnCursorIdle', 'always');
      await page.evaluate(() => {
        const editor = window.rstudio!.documents.activeEditor()!;
        editor.focus();
        editor.gotoLine(6, 2);
      });

      // the first equation's preview renders on the row the peek held, and
      // the peek is gone
      await expect(visibleMath).toHaveCount(1, { timeout: 60000 });
      await expect(page.locator(PEEK)).toHaveCount(0);

      // and the queue is not stuck: the second equation renders when the
      // cursor idles in it
      await page.evaluate(() => {
        const editor = window.rstudio!.documents.activeEditor()!;
        editor.gotoLine(11, 2);
      });
      await expect(visibleMath).toHaveCount(2, { timeout: 60000 });
    } finally {
      await clearPref(page, 'latexPreviewOnCursorIdle');
    }

    await documentCloseAllNoSave(page);
    await executeInConsoleChecked(page, `invisible(file.remove(${rPathLiteral(mathPath)}))`);
    await commitAll(page, 'remove math', [mathPath]);
  });

  test('re-fetches the base when a marker is clicked in an unfocused editor', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    // Stage the edits from the console (focus stays there), which moves the
    // index under the open document. Clicking the marker is the first
    // interaction with the editor afterwards: it has to pick the new index
    // up, so the peek it opens does not outlive the stale base and no
    // markers remain (the index now matches the buffer).
    await executeInConsoleChecked(
      page,
      `invisible(system2("git", c(${gitC()}, "add", "--", "script.R")))`,
    );
    try {
      const cell = page.locator('.ace_gutter-cell.ace_git-modified').first();
      const box = (await cell.boundingBox())!;
      await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
      await expect(page.locator(PEEK)).toHaveCount(0, { timeout: 30000 });
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({});
    } finally {
      await executeInConsoleChecked(
        page,
        `invisible(system2("git", c(${gitC()}, "reset", "-q", "--", "script.R")))`,
      );
      await documentCloseAllNoSave(page);
    }
  });

  test('reverts from a peek against the index as it is, not as it was', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    const cell = page.locator('.ace_gutter-cell.ace_git-modified').first();
    const box = (await cell.boundingBox())!;
    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
    const peek = page.locator(PEEK);
    await expect(peek).toBeVisible({ timeout: 10000 });
    await expect(peek.locator(PEEK_LINE)).toHaveText(['d <- 4']);

    // Stage the file from the console while the peek is open: the index now
    // matches the buffer, and nothing has told the editor so. Revert must
    // not restore the `d <- 4` the peek shows (undoing the very change just
    // staged); it re-fetches the base first, finds the change gone, and
    // leaves the line alone.
    await executeInConsoleChecked(
      page,
      `invisible(system2("git", c(${gitC()}, "add", "--", "script.R")))`,
    );
    try {
      await peek.locator(PEEK_REVERT).click();
      await expect(peek).toHaveCount(0, { timeout: 30000 });
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({});
      expect(await new AceEditor(page, '').getLine(4)).toBe('d <- 40');
    } finally {
      await executeInConsoleChecked(
        page,
        `invisible(system2("git", c(${gitC()}, "reset", "-q", "--", "script.R")))`,
      );
      await documentCloseAllNoSave(page);
    }
  });

  test('reverts at the cursor against the index as it is, not as it was', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    const editor = new AceEditor(page, '');
    await editor.gotoLine(5);

    // The same, through the command: with focus still in the console after
    // staging, no focus-driven re-fetch is even pending, so the command has
    // to fetch the base itself before acting on the (now stale) chunk.
    await executeInConsoleChecked(
      page,
      `invisible(system2("git", c(${gitC()}, "add", "--", "script.R")))`,
    );
    try {
      await executeCommand(page, 'vcsRevertChange');
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({});
      expect(await editor.getLine(4)).toBe('d <- 40');
    } finally {
      await executeInConsoleChecked(
        page,
        `invisible(system2("git", c(${gitC()}, "reset", "-q", "--", "script.R")))`,
      );
      await documentCloseAllNoSave(page);
    }
  });

  test('drops the base, and the revert, when the base cannot be re-fetched', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    // Open the peek with focus in the console, so that the re-fetch the
    // click's editor focus triggers has come and gone before fetches are
    // made to fail: only the Revert's own fetch is meant to.
    await executeInConsoleChecked(page, 'invisible(NULL)');
    const focusFetch = page.waitForResponse((r) => r.url().includes('git_diff_base_contents'));
    const cell = page.locator('.ace_gutter-cell.ace_git-modified').first();
    const box = (await cell.boundingBox())!;
    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
    const peek = page.locator(PEEK);
    await expect(peek).toBeVisible({ timeout: 10000 });
    await focusFetch;
    await expect(peek).toBeVisible();

    // Answer the re-fetch with an error: with no way of knowing what the
    // index holds, nothing may be reverted, and the markers (and the peek,
    // which shows the base) go until a fetch succeeds again.
    const failFetch = async (route: Route): Promise<void> => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ result: null, error: { code: 100, message: 'git failed' } }),
      });
    };
    await page.route('**/git_diff_base_contents*', failFetch);
    try {
      await peek.locator(PEEK_REVERT).click();
      await expect(peek).toHaveCount(0, { timeout: 30000 });
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({});
      expect(await new AceEditor(page, '').getLine(4)).toBe('d <- 40');
    } finally {
      await page.unroute('**/git_diff_base_contents*', failFetch);
    }

    // the next Git refresh fetches the base again and the markers return
    await executeCommand(page, 'activateVcs');
    await executeCommand(page, 'vcsRefresh');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    await documentCloseAllNoSave(page);
  });

  test('drops a revert that an edit overtakes while the base is re-fetched', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    const editor = new AceEditor(page, '');
    await editor.gotoLine(5);
    await editor.navigateLineEnd();

    // Hold the re-fetch back, type into the very line the revert is for,
    // then let the fetch through. The chunk still spans the same rows, so
    // only the edit itself can tell the late revert to stand down; the
    // typed text must survive.
    const parked: Route[] = [];
    const park = async (route: Route): Promise<void> => {
      parked.push(route);
    };
    await page.route('**/git_diff_base_contents*', park);
    try {
      await executeCommand(page, 'vcsRevertChange');
      await expect.poll(() => parked.length, { timeout: 30000 }).toBeGreaterThan(0);
      await editor.insert('1');
      await expect.poll(() => editor.getLine(4)).toBe('d <- 401');

      // fallback, not continue: the fixture routes RPCs too, and the
      // request still has to pass through its handler
      const fetched = page.waitForResponse((r) => r.url().includes('git_diff_base_contents'));
      for (const route of parked.splice(0)) await route.fallback();
      await fetched;
    } finally {
      await page.unroute('**/git_diff_base_contents*', park);
      for (const route of parked.splice(0)) await route.fallback().catch(() => {});
    }
    expect(await editor.getLine(4)).toBe('d <- 401');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    // asked again, with nothing in the way, the revert goes through
    await executeCommand(page, 'vcsRevertChange');
    await expect.poll(() => editor.getLine(4)).toBe('d <- 4');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 1: [ADDED], 6: [DELETED] });

    await documentCloseAllNoSave(page);
  });

  test('drops a revert that a Save As overtakes while the base is re-fetched', async ({
    rstudioPage: page,
  }) => {
    // A tracked destination whose index version differs from the buffer in
    // the same rows as script.R's does -- `d <- 44` where script.R's index
    // has `d <- 4` -- so the chunk the revert was issued for has a twin with
    // the same coordinates under the new path. It is kept out of the
    // working tree so that Save As raises no overwrite prompt.
    const otherPath = `${projectDir()}/other.R`;
    const otherLines = SEED_LINES.map((line) => (line === 'd <- 4' ? 'd <- 44' : line));
    await executeInConsoleChecked(
      page,
      `writeLines(c(${otherLines.map(rStringLiteral).join(', ')}), ${rPathLiteral(otherPath)})`,
    );
    await commitAll(page, 'other', [otherPath]);
    await executeInConsoleChecked(page, `invisible(file.remove(${rPathLiteral(otherPath)}))`);

    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);
    const editor = new AceEditor(page, '');
    await editor.gotoLine(5);

    // Hold every base fetch: the revert's, then the one Save As triggers.
    // Once released, the revert must not have restored other.R's `d <- 44`.
    const parked: Route[] = [];
    const park = async (route: Route): Promise<void> => {
      parked.push(route);
    };
    await page.route('**/git_diff_base_contents*', park);
    try {
      await executeCommand(page, 'vcsRevertChange');
      await expect.poll(() => parked.length, { timeout: 30000 }).toBeGreaterThan(0);

      await executeCommand(page, 'saveSourceDocAs');
      const fileName = page.locator('#file_dialog_name_prompt');
      await expect(fileName).toBeVisible({ timeout: 30000 });
      await fileName.fill(otherPath);
      await page.locator('#rstudio_file_accept_save').click();
      await expect(
        page.locator("[class*='rstudio_source_panel'] [class*='PanelTab-selected']"),
      ).toContainText('other.R', { timeout: 30000 });
      await expect.poll(() => parked.length, { timeout: 30000 }).toBeGreaterThan(1);

      for (const route of parked.splice(0)) await route.fallback();
    } finally {
      await page.unroute('**/git_diff_base_contents*', park);
      for (const route of parked.splice(0)) await route.fallback().catch(() => {});
    }

    // the new path's base arrives and is diffed against the untouched buffer
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);
    expect(await editor.getLine(4)).toBe('d <- 40');

    await documentCloseAllNoSave(page);
    await executeInConsoleChecked(page, `invisible(file.remove(${rPathLiteral(otherPath)}))`);
    await commitAll(page, 'remove other', [otherPath]);
  });

  test('closes the peek when the index version it shows is replaced', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    const cell = page.locator('.ace_gutter-cell.ace_git-modified').first();
    const box = (await cell.boundingBox())!;
    await page.mouse.click(box.x + box.width - 3, box.y + box.height / 2);
    await expect(page.locator(PEEK)).toBeVisible({ timeout: 10000 });

    // A refresh that fetches the same index version leaves the peek alone;
    // the focus-triggered re-fetch would otherwise close every peek opened
    // within half a second of clicking into the editor.
    await executeCommand(page, 'activateVcs');
    const unchangedBase = page.waitForResponse((r) => r.url().includes('git_diff_base_contents'));
    await executeCommand(page, 'vcsRefresh');
    await unchangedBase;
    await page.waitForTimeout(500);
    await expect(page.locator(PEEK)).toBeVisible();

    // Replace the index entry without touching the working copy: the edited
    // file, but with `d <- 41` where the buffer has `d <- 40`. The chunk
    // keeps its rows, so only its contents change.
    const indexLines = EDITED_LINES.map((line) => (line === 'd <- 40' ? 'd <- 41' : line));
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          blob <- system2(
            "git", c(${gitC()}, "hash-object", "-w", "--stdin"),
            input = c(${indexLines.map(rStringLiteral).join(', ')}), stdout = TRUE
          )
          s <- system2("git", c(${gitC()}, "update-index", "--cacheinfo",
                                paste0("100644,", blob, ",script.R")))
          if (s != 0)
            stop("git update-index failed (exit status: ", s, ")")
        }
      `,
      { timeout: 60000 },
    );
    await executeCommand(page, 'vcsRefresh');

    // the peek showed `d <- 4`, which the index no longer has
    await expect(page.locator(PEEK)).toHaveCount(0, { timeout: 30000 });
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 4: [MODIFIED] });

    // put the index back for the tests below
    await executeInConsoleChecked(
      page,
      `invisible(system2("git", c(${gitC()}, "reset", "-q", "--", "script.R")))`,
    );
    await documentCloseAllNoSave(page);
  });

  test('answers an unchanged base without re-sending it', async ({ rstudioPage: page }) => {
    type BaseResult = { tracked: boolean; id: string; unchanged: boolean; contents: string };
    const baseResult = async (r: Promise<Response>): Promise<BaseResult> =>
      ((await (await r).json()) as { result: BaseResult }).result;

    const firstFetch = page.waitForResponse((r) => r.url().includes('git_diff_base_contents'));
    await openFile(page, scriptPath());
    const first = await baseResult(firstFetch);
    expect(first.tracked).toBe(true);
    expect(first.unchanged).toBe(false);
    expect(first.contents.split('\n').slice(0, 2)).toEqual(SEED_LINES.slice(0, 2));
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    // A refresh names the base held; the index still has it, so the reply
    // carries no contents, and the markers (computed from the held base)
    // stay.
    await executeCommand(page, 'activateVcs');
    const refetch = page.waitForResponse((r) => r.url().includes('git_diff_base_contents'));
    await executeCommand(page, 'vcsRefresh');
    const second = await baseResult(refetch);
    expect(second.tracked).toBe(true);
    expect(second.unchanged).toBe(true);
    expect(second.id).toBe(first.id);
    expect(second.contents).toBe('');
    await page.waitForTimeout(500);
    expect(await gutterMarkers(page)).toEqual(EDITED_MARKERS);

    await documentCloseAllNoSave(page);
  });

  test('fetches a background document\'s base only once it is shown', async ({
    rstudioPage: page,
  }) => {
    const otherPath = `${projectDir()}/other.R`;
    await executeInConsoleChecked(page, `writeLines("o <- 1", ${rPathLiteral(otherPath)})`);
    await commitAll(page, 'other', [otherPath]);

    // which documents a fetch was for, by file name, and when the last came
    const fetched: string[] = [];
    let lastFetchAt = 0;
    const onRequest = (request: Request): void => {
      if (!request.url().includes('git_diff_base_contents')) return;
      const params = (request.postDataJSON() as { params: string[] }).params;
      fetched.push(params[0].replace(/\\/g, '/').split('/').pop()!);
      lastFetchAt = Date.now();
    };
    page.on('request', onRequest);

    // opening a document fetches once on attach and again on focus; wait
    // for those to be over before counting
    const settled = async (): Promise<void> => {
      await expect.poll(() => lastFetchAt > 0 && Date.now() - lastFetchAt > 1000, { timeout: 15000 }).toBe(true);
    };

    try {
      await openFile(page, scriptPath());
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);
      await openFile(page, otherPath);
      await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);
      await expect.poll(() => fetched.includes('other.R'), { timeout: 15000 }).toBe(true);
      await settled();

      // A Git refresh reaches every open document, but only the one shown
      // fetches; the hidden script.R waits.
      fetched.length = 0;
      await executeCommand(page, 'activateVcs');
      await executeCommand(page, 'vcsRefresh');
      await expect.poll(() => fetched.includes('other.R'), { timeout: 15000 }).toBe(true);
      await settled();
      expect(fetched).not.toContain('script.R');

      // and fetches when it is shown again
      await openFile(page, scriptPath());
      await expect.poll(() => fetched.includes('script.R'), { timeout: 15000 }).toBe(true);
      await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);
    } finally {
      page.off('request', onRequest);
      await documentCloseAllNoSave(page);
      await executeInConsoleChecked(page, `invisible(file.remove(${rPathLiteral(otherPath)}))`);
      await commitAll(page, 'remove other', [otherPath]);
    }
  });

  test('clears once the changes are committed, and tracks the new base', async ({
    rstudioPage: page,
  }) => {
    await openFile(page, scriptPath());
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual(EDITED_MARKERS);

    await commitAll(page, 'edits');

    // A commit from outside the IDE is picked up on the next Git refresh,
    // which the Git pane's own refresh (and every git operation RStudio runs
    // itself) triggers. The pane's presenter loads on first show, so the
    // refresh command has no handler until the pane has been activated.
    await executeCommand(page, 'activateVcs');
    await executeCommand(page, 'vcsRefresh');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({});

    // The base moved to the new commit rather than the markers merely being
    // dropped: a fresh edit is diffed against what was just committed.
    const editor = new AceEditor(page, '');
    await editor.gotoLine(1);
    await editor.navigateLineEnd();
    await editor.insert('0');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 0: [MODIFIED] });
  });

  test('is controlled by the preference', async ({ rstudioPage: page }) => {
    // everything is committed now, so make an (unsaved) edit to have a marker
    await openFile(page, scriptPath());
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);
    const editor = new AceEditor(page, '');
    await editor.gotoLine(1);
    await editor.navigateLineEnd();
    await editor.insert('0');
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 0: [MODIFIED] });

    // the commands that act on the markers come and go with them
    await expect.poll(() => isCommandEnabled(page, 'vcsNextChange')).toBe(true);

    await setPref(page, PREF, false);
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(false);
    expect(await gutterMarkers(page)).toEqual({});
    await expect.poll(() => isCommandEnabled(page, 'vcsNextChange')).toBe(false);
    await expect.poll(() => isCommandEnabled(page, 'vcsRevertChange')).toBe(false);

    await setPref(page, PREF, true);
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 0: [MODIFIED] });
    await expect.poll(() => isCommandEnabled(page, 'vcsNextChange')).toBe(true);
  });

  test('leaves an untracked file unmarked', async ({ rstudioPage: page }) => {
    const untrackedPath = `${projectDir()}/untracked.R`;
    await executeInConsoleChecked(page, `writeLines("u <- 1", ${rPathLiteral(untrackedPath)})`);
    await openFile(page, untrackedPath);

    // The gutter attaches (the file is in a Git project) but has no index
    // version to diff against, so nothing is marked -- an untracked file is
    // not "all added". Sample over a window: the empty result is also the
    // starting state, so a single poll would pass before the base arrived.
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);
    const deadline = Date.now() + 3000;
    while (Date.now() < deadline) {
      expect(await gutterMarkers(page)).toEqual({});
      await page.waitForTimeout(250);
    }
  });
});

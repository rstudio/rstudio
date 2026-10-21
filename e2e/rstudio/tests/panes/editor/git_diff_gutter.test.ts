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
import type { Page } from '@playwright/test';
import { AceEditor } from '@pages/ace_editor.page';
import { CONSOLE_OUTPUT, executeInConsole } from '@pages/console_pane.page';
import { documentCloseAllNoSave, executeCommand, openProject, setPref } from '@utils/commands';
import { openFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral, rStringLiteral } from '@utils/r';

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

  async function commitAll(page: Page, message: string): Promise<void> {
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          s <- c(
            system2("git", c(${gitC()}, "add", "-A")),
            system2("git", c(${gitC()}, ${gitWho}, "commit", "-m", ${rStringLiteral(message)}, "--quiet"))
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

    // The buffer now matches the index but the working copy doesn't; don't
    // leave that behind for the tests below, which reopen the file.
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

      await setPref(page, 'git_diff_ignore_whitespace', false);
      await expect
        .poll(() => gutterMarkers(page), { timeout: 30000 })
        .toEqual({ ...EDITED_MARKERS, 7: [MODIFIED] });
    } finally {
      await setPref(page, 'git_diff_ignore_whitespace', false);
    }

    await documentCloseAllNoSave(page);
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

    await setPref(page, PREF, false);
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(false);
    expect(await gutterMarkers(page)).toEqual({});

    await setPref(page, PREF, true);
    await expect.poll(() => gutterAttached(page), { timeout: 30000 }).toBe(true);
    await expect.poll(() => gutterMarkers(page), { timeout: 30000 }).toEqual({ 0: [MODIFIED] });
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

// Edit-suggestion tests ported from
// src/cpp/tests/automation/testthat/test-automation-edit-suggestions.R.
//
// These tests drive .rs.api.showEditSuggestion via the console to inject a
// deterministic ghost-text suggestion into the active editor. Bypassing
// Copilot / Posit Assistant keeps the tests focused on the IDE's ghost-text
// rendering, mutation handling, and accept paths -- and frees them from
// external provider flake. Provider-driven coverage of the same accept and
// dismiss flows already lives in code_suggestions.test.ts.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { SourcePane } from '@pages/source_pane.page';
import { clearPref, getPref, setPref } from '@utils/commands';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { typeSlowly } from '@utils/constants';

const FILE_PREFIX = 'es_';
const FILES = {
  prefix:          `${FILE_PREFIX}prefix.R`,
  mutate:          `${FILE_PREFIX}mutate.R`,
  move:            `${FILE_PREFIX}move.R`,
  clearOldRow:     `${FILE_PREFIX}clear_old_row.R`,
  inline:          `${FILE_PREFIX}inline.R`,
  cursorDismiss:   `${FILE_PREFIX}cursor_dismiss.R`,
  offscreenAbove:  `${FILE_PREFIX}offscreen_above.R`,
  offscreenBelow:  `${FILE_PREFIX}offscreen_below.R`,
  offscreenClick:  `${FILE_PREFIX}offscreen_click.R`,
  wordDiff:        `${FILE_PREFIX}word_diff.R`,
  mixedDiff:       `${FILE_PREFIX}mixed_diff.R`,
  multilineDiff:   `${FILE_PREFIX}multiline_diff.R`,
  charDiff:        `${FILE_PREFIX}char_diff.R`,
  suffixDiff:      `${FILE_PREFIX}suffix_diff.R`,
  truncDiff:       `${FILE_PREFIX}trunc_diff.R`,
  affixDiff:       `${FILE_PREFIX}affix_diff.R`,
} as const;

// A file long enough that the editor must scroll: the suggestion target line
// and the cursor can't both be in the viewport at once.
function longFileContents(): string {
  const lines: string[] = ['# Create a 3D point.'];
  for (let i = 2; i < 100; i++) {
    lines.push(`value_${i} <- ${i}`);
  }
  lines.push('point <- function(x, y, z) {}');
  return lines.join('\n');
}

// Returns the inline diff view's ace_diff-* markers, or null while the view's
// embedded editor has not attached yet (for use with expect.poll). Only the
// expected editor-not-found error keeps the poll going; anything else (page
// closed, missing bridge, a bug in getMarkers) is a real failure and rethrows
// rather than becoming an indefinite poll ending in "Received: null".
async function diffMarkersOrNull(diffEditor: AceEditor) {
  try {
    const markers = await diffEditor.getMarkers();
    return markers.filter((m) => m.clazz.startsWith('ace_diff-'));
  } catch (e) {
    if (e instanceof Error && e.message.includes('No Ace editor found containing marker')) {
      return null;
    }
    throw e;
  }
}

test.describe('Edit suggestions (showEditSuggestion injection)', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let savedAssistantPref: boolean | number | string | null = null;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();

    // These tests inject deterministic suggestions via showEditSuggestion; a
    // live code assistant (e.g. a sign-in leaked from the @ai suites on
    // credentialed shards) races them with real completions that displace the
    // injected ones, so force the assistant off for the suite. Restore the
    // prior value afterwards rather than clearing: the default is "posit",
    // which would re-enable a leaked provider for later suites.
    savedAssistantPref = await getPref(page, 'assistant');
    await setPref(page, 'assistant', 'none');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    if (savedAssistantPref === null)
      await clearPref(page, 'assistant');
    else
      await setPref(page, 'assistant', savedAssistantPref);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, Object.values(FILES));
  });

  // Tagged @ai and deferred: this is a known flake. Pressing Tab can race the
  // ghost-suggestion anchor -- if "he" is typed before the suggestion is
  // active/prefix-matched, Tab does nothing and the line stays "he" instead of
  // completing to "hello". Needs a gate on the suggestion being active (e.g. a
  // poll on a synthetic ghost-text token via AceEditor.getTokens) before Tab.
  test('ghost text suggestions can be prefix-matched', { tag: ['@ai'] }, async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.prefix, '');
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 1, 1, 1), "hello")',
    );

    // Empty-marker AceEditor matches the first non-console Ace editor; with
    // a single source tab open that's the file we just created.
    const editor = new AceEditor(page, '');
    const sourcePane = new SourcePane(page);
    await sourcePane.contentPane.click();
    // Ensure the editor textarea actually owns focus before typing -- clicks
    // dispatch synchronously but Ace's focus shift can lag by a tick.
    await editor.focus();

    await page.keyboard.type('he');
    await expect.poll(() => editor.getLine(0)).toBe('he');

    await page.keyboard.press('Tab');
    await expect.poll(() => editor.getLine(0)).toBe('hello');
  });

  // Two paths into "type characters into the editor while a suggestion is
  // active" land in different broken places, neither of which we have a
  // fix for here:
  //
  //   * page.keyboard.type / typeSlowly: the first char lands in the
  //     editor, then subsequent chars get routed to the console. Cause
  //     not yet identified -- something between editor.focus() and the
  //     second keystroke is moving keyboard focus off the source pane.
  //   * editor.insert() (Ace API): all 3 chars land in the editor, but
  //     the NES anchor doesn't shift. The subsequent gutter-click accept
  //     then overwrites our typed characters with the suggestion at the
  //     stale original range.
  //
  // The other four tests in this file cover ghost-text rendering, anchor
  // shifting on programmatic insert (see the next test), prefix-match
  // accept, and inline insertion-preview.
  test.fixme('ghost text suggestions survive document mutations', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.mutate, '# abc def');
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 3, 1, 6), "ABC")',
    );
    const editor = new AceEditor(page, 'abc def');
    await editor.focus();
    await editor.gotoLine(1, 0);
    await page.keyboard.press('ArrowRight');
    await typeSlowly(page, '123');
    await expect.poll(() => editor.getLine(0)).toBe('#123 abc def');
    await new SourcePane(page).nesGutter.click();
    await expect.poll(() => editor.getLine(0)).toBe('#123 ABC def');
  });

  test('ghost text moves on document edit', async ({ rstudioPage: page }) => {
    // Six newlines map to seven rows once Ace counts the trailing empty line.
    await writeAndOpenFile(page, sandbox.dir, FILES.move, '\n\n\n\n\n\n');
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(3, 1, 3, 1), "Hello world!")',
    );

    const editor = new AceEditor(page, '');
    // Wait for the suggestion to land on its initial row before mutating.
    // Without this the inserts can race the suggestion's first render and
    // the anchor never sees the document deltas.
    await expect.poll(async () => {
      const tokens = await editor.getTokens(2);
      return tokens[0]?.value;
    }).toBe('Hello world!');

    // Inserting two newlines at row 0 shifts everything below down by 2 rows;
    // the suggestion's anchor (Ace row 2) should land on row 4.
    await editor.gotoLine(1);
    await editor.insert('\n');
    await editor.insert('\n');

    await expect.poll(async () => {
      const tokens = await editor.getTokens(4);
      return tokens[0]?.value;
    }).toBe('Hello world!');
  });

  test('ghost text is cleared from old row when newline inserted above', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.clearOldRow, '\n\n\n\n\n\n');
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(3, 1, 3, 1), "Hello world!")',
    );

    const editor = new AceEditor(page, '');

    // Suggestion is on Ace row 2.
    await expect.poll(async () => {
      const tokens = await editor.getTokens(2);
      return tokens[0]?.value;
    }).toBe('Hello world!');

    await editor.gotoLine(1);
    await editor.insert('\n');

    // Suggestion moved to row 3.
    await expect.poll(async () => {
      const tokens = await editor.getTokens(3);
      return tokens[0]?.value;
    }).toBe('Hello world!');

    // Old row 2 must no longer report a synthetic token.
    const oldRowTokens = await editor.getTokens(2);
    const hasSynthetic = oldRowTokens.some((t) => t.synthetic === true);
    expect(hasSynthetic).toBe(false);
  });

  test('edit suggestions render inline when appropriate', async ({ rstudioPage: page }) => {
    const contents = '# Create a 3D point.\npoint <- function(x, y, z) {}\n';
    await writeAndOpenFile(page, sandbox.dir, FILES.inline, contents);
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 12, 1, 14), "4D")',
    );

    const editor = new AceEditor(page, '# Create');

    await expect.poll(async () => {
      const tokens = await editor.getTokens(0);
      return tokens[1]?.type;
    }).toBe('insertion_preview');

    await new SourcePane(page).nesGutter.click();

    await expect.poll(async () => {
      const tokens = await editor.getTokens(0);
      return tokens[0]?.value;
    }).toBe('# Create a 4D point.');
  });

  test('at-cursor ghost text is dismissed when the cursor moves away', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.cursorDismiss, '# abc\n\n\n');

    const editor = new AceEditor(page, '# abc');
    const sourcePane = new SourcePane(page);

    // A zero-width suggestion at the cursor position is treated as an
    // inline (at-cursor) completion, like real provider ghost text.
    await editor.gotoLine(1, 5);
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 6, 1, 6), " def")',
    );

    await expect(sourcePane.ghostText.first()).toBeVisible();

    // Moving the cursor away dismisses the ghost text without inserting it
    // (https://github.com/rstudio/rstudio/issues/17147)
    await editor.gotoLine(3);
    await expect(sourcePane.ghostText).toHaveCount(0);
    expect(await editor.getLine(0)).toBe('# abc');
  });

  // --- Diff granularity (#18437) ---
  //
  // Edit-suggestion previews diff the original against the replacement text
  // to decide what to highlight. The default is a word-level diff, so a
  // variable rename previews as whole-word deletion/insertion pairs rather
  // than interleaved character fragments; the edit_suggestion_diff_granularity
  // preference restores the old character-level behavior.

  test('single-word renames preview as whole-word replacements', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.wordDiff, 'count <- 1');
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 1, 1, 11), "total <- 1")',
    );

    const editor = new AceEditor(page, '');

    // The rename previews in-document as one whole inserted word ("total").
    // A character-level diff of the same edit fragments into several
    // deletion/insertion pairs, which cannot render as a replacement at all
    // and would fall back to the inline diff view.
    await expect.poll(async () => {
      const tokens = await editor.getTokens(0);
      return tokens.filter((t) => t.type === 'insertion_preview').map((t) => t.value);
    }).toEqual(['total']);

    // The deletion highlight likewise covers the whole word being replaced.
    const deletions = (await editor.getMarkers())
      .filter((m) => m.clazz === 'ace_next-edit-suggestion-deletion')
      .map((m) => [m.range?.start.column, m.range?.end.column]);
    expect(deletions).toEqual([[0, 5]]);
  });

  test('renames sharing an affix still preview as whole-word replacements', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.affixDiff, 'total <- 1');
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 1, 1, 11), "count_total <- 1")',
    );

    const editor = new AceEditor(page, '');

    // 'total' is a suffix of 'count_total', so the pair is a candidate for
    // character-level refinement -- but jsdiff fragments it around the extra
    // shared characters (+'coun' ='t' +'_t' ='otal') instead of yielding the
    // single clean insertion of 'count_'. The refinement is only accepted
    // when it collapses to one edit, so the pair must stay a whole-word
    // replacement rather than fragmenting.
    await expect.poll(async () => {
      const tokens = await editor.getTokens(0);
      return tokens.filter((t) => t.type === 'insertion_preview').map((t) => t.value);
    }).toEqual(['count_total']);

    const deletions = (await editor.getMarkers())
      .filter((m) => m.clazz === 'ace_next-edit-suggestion-deletion')
      .map((m) => [m.range?.start.column, m.range?.end.column]);
    expect(deletions).toEqual([[0, 5]]);
  });

  test('the inline diff view highlights whole words', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.mixedDiff, 'count <- count + 1');
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 1, 1, 19), "total <- total + 1")',
    );

    // Two renames on one line render as the inline diff view, whose embedded
    // editor shows the merged text with deletion/insertion markers. The word
    // diff marks each whole word, not interleaved character fragments.
    const diffEditor = new AceEditor(page, 'counttotal');
    await expect.poll(async () => {
      const markers = await diffMarkersOrNull(diffEditor);
      return markers === null ? null : markers
        .map((m) => [m.clazz, m.range?.start.column, m.range?.end.column])
        .sort((a, b) => (a[1] as number) - (b[1] as number));
    }).toEqual([
      ['ace_diff-removed', 0, 5],   // count
      ['ace_diff-added', 5, 10],    // total
      ['ace_diff-removed', 14, 19], // count
      ['ace_diff-added', 19, 24],   // total
    ]);
  });

  test('the inline diff view places markers on the correct rows for multi-line edits', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.multilineDiff, 'first <- 1\nfirst <- first + 1');
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 1, 2, 19), "second <- 1\\nsecond <- second + 1")',
    );

    // A multi-line suggestion exercises the diff view's row/column marker
    // math across newlines: the merged text is "firstsecond <- 1" /
    // "firstsecond <- firstsecond + 1", with each whole-word pair marked on
    // its own row.
    const diffEditor = new AceEditor(page, 'firstsecond');
    await expect.poll(async () => {
      const markers = await diffMarkersOrNull(diffEditor);
      return markers === null ? null : markers
        .map((m) => [m.clazz, m.range?.start.row, m.range?.start.column, m.range?.end.column])
        .sort((a, b) => ((a[1] as number) - (b[1] as number)) || ((a[2] as number) - (b[2] as number)));
    }).toEqual([
      ['ace_diff-removed', 0, 0, 5],   // first
      ['ace_diff-added', 0, 5, 11],    // second
      ['ace_diff-removed', 1, 0, 5],   // first
      ['ace_diff-added', 1, 5, 11],    // second
      ['ace_diff-removed', 1, 15, 20], // first
      ['ace_diff-added', 1, 20, 26],   // second
    ]);
  });

  test('word extensions still preview as pure insertions', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.suffixDiff, 'x <- foo');
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 1, 1, 9), "x <- foobar")',
    );

    const editor = new AceEditor(page, '');

    // 'foo' -> 'foobar' is a word replacement to the word diff, but the
    // refinement pass reduces it to an insertion of 'bar', so nothing is
    // struck out.
    await expect.poll(async () => {
      const tokens = await editor.getTokens(0);
      return tokens.filter((t) => t.type === 'insertion_preview').map((t) => t.value);
    }).toEqual(['bar']);

    const deletions = (await editor.getMarkers())
      .filter((m) => m.clazz === 'ace_next-edit-suggestion-deletion');
    expect(deletions).toEqual([]);
  });

  test('word truncations still preview as pure deletions', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.truncDiff, 'x <- foobar');
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 1, 1, 12), "x <- foo")',
    );

    const editor = new AceEditor(page, '');

    // The truncation direction of the refinement: 'foobar' -> 'foo' reduces
    // to a deletion of 'bar', taking the deletion-only rendering path, so
    // just 'bar' is struck out and nothing is inserted.
    await expect.poll(async () => {
      const markers = await editor.getMarkers();
      return markers
        .filter((m) => m.clazz === 'ace_next-edit-suggestion-deletion')
        .map((m) => [m.range?.start.column, m.range?.end.column]);
    }).toEqual([[8, 11]]);

    const insertions = (await editor.getTokens(0))
      .filter((t) => t.type === 'insertion_preview');
    expect(insertions).toEqual([]);
  });

  // The pref set/clear lives in beforeAll/afterAll rather than try/finally
  // inside the test: rstudioPage is worker-scoped and a test body's finally
  // block does not run when Playwright aborts on a test timeout, which would
  // leak edit_suggestion_diff_granularity=character into every later test in
  // the worker. afterAll runs even after a timed-out test.
  test.describe('with character-level granularity', () => {
    test.beforeAll(async ({ rstudioPage: page }) => {
      await setPref(page, 'edit_suggestion_diff_granularity', 'character');
    });

    test.afterAll(async ({ rstudioPage: page }) => {
      await clearPref(page, 'edit_suggestion_diff_granularity');
    });

    test('character-level previews can be restored via preference', async ({ rstudioPage: page }) => {
      await writeAndOpenFile(page, sandbox.dir, FILES.charDiff, 'count <- 1');
      await consoleActions.executeInConsole(
        '.rs.api.showEditSuggestion(c(1, 1, 1, 11), "total <- 1")',
      );

      // The character-level diff of count -> total keeps the characters the
      // words share, fragmenting the rename into several edits; the
      // suggestion therefore renders in the inline diff view (merged text
      // "ctountal"), with the fragments highlighted, rather than as a
      // whole-word replacement in the document. Assert the fragmentation
      // shape -- several markers, each narrower than the 5-character words --
      // rather than the exact ranges, which depend on which of several
      // equally-valid alignments the bundled jsdiff happens to choose.
      const diffEditor = new AceEditor(page, 'ctountal');
      await expect.poll(async () => {
        const markers = await diffMarkersOrNull(diffEditor);
        return markers === null ? null : markers.length;
      }).toBeGreaterThan(2);

      const fragments = (await diffMarkersOrNull(diffEditor)) ?? [];
      for (const fragment of fragments) {
        const width = (fragment.range?.end.column ?? 0) - (fragment.range?.start.column ?? 0);
        expect(width).toBeGreaterThan(0);
        expect(width).toBeLessThan(5);
      }
    });
  });

  // --- Off-screen suggestion handling (#17147) ---
  //
  // A suggestion whose range is scrolled out of view must not be accepted
  // blindly: while it is off-screen an edge-pinned gutter arrow points toward
  // it, the first accept keypress navigates to it, and only a subsequent
  // accept inserts the edit.

  test('accepting an off-screen pending suggestion navigates first', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.offscreenAbove, longFileContents());

    const editor = new AceEditor(page, '# Create');
    const sourcePane = new SourcePane(page);

    // Move to the bottom of the file so row 0 is scrolled out of view
    await editor.gotoLine(100);
    await expect.poll(() => sourceActions.getFirstVisibleRow()).toBeGreaterThan(50);

    // The suggestion starts before the cursor, so it shows as a pending
    // (gutter-only) suggestion on row 0 -- off-screen.
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 12, 1, 14), "4D")',
    );

    // The edge-pinned indicator points toward the off-screen suggestion
    await expect(sourcePane.nesOffscreenGutter.first()).toBeVisible();

    await sourcePane.contentPane.click();
    await editor.focus();

    // First accept navigates to the suggestion without applying it
    await page.keyboard.press('ControlOrMeta+;');
    await expect.poll(() => sourceActions.getFirstVisibleRow()).toBeLessThan(5);
    await expect(sourcePane.nesOffscreenGutter).toHaveCount(0);
    expect(await editor.getLine(0)).toBe('# Create a 3D point.');

    // Second accept applies it
    await page.keyboard.press('ControlOrMeta+;');
    await expect.poll(() => editor.getLine(0)).toBe('# Create a 4D point.');
  });

  test('accepting an off-screen revealed suggestion navigates first', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.offscreenBelow, longFileContents());

    const editor = new AceEditor(page, '# Create');
    const sourcePane = new SourcePane(page);

    // Keep the cursor at the top; the suggestion lands on the last row,
    // after the cursor, so it autoshows (as ghost text) -- off-screen.
    await editor.gotoLine(1);
    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(100, 1, 100, 1), "# ")',
    );

    await expect(sourcePane.nesOffscreenGutter.first()).toBeVisible();

    await sourcePane.contentPane.click();
    await editor.focus();

    // First accept navigates to the suggestion without applying it
    await page.keyboard.press('ControlOrMeta+;');
    await expect.poll(() => sourceActions.getFirstVisibleRow()).toBeGreaterThan(50);
    await expect(sourcePane.nesOffscreenGutter).toHaveCount(0);
    expect(await editor.getLine(99)).toBe('point <- function(x, y, z) {}');

    // Second accept applies it
    await page.keyboard.press('ControlOrMeta+;');
    await expect.poll(() => editor.getLine(99)).toBe('# point <- function(x, y, z) {}');
  });

  test('clicking the off-screen indicator navigates to the suggestion', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.offscreenClick, longFileContents());

    const editor = new AceEditor(page, '# Create');
    const sourcePane = new SourcePane(page);

    await editor.gotoLine(100);
    await expect.poll(() => sourceActions.getFirstVisibleRow()).toBeGreaterThan(50);

    await consoleActions.executeInConsole(
      '.rs.api.showEditSuggestion(c(1, 12, 1, 14), "4D")',
    );

    await expect(sourcePane.nesOffscreenGutter.first()).toBeVisible();
    await sourcePane.nesOffscreenGutter.first().click({ force: true });

    // Navigates without accepting; the suggestion's own gutter icon is now
    // visible and the document text is unchanged.
    await expect.poll(() => sourceActions.getFirstVisibleRow()).toBeLessThan(5);
    await expect(sourcePane.nesOffscreenGutter).toHaveCount(0);
    await expect(sourcePane.nesGutter.first()).toBeVisible();
    expect(await editor.getLine(0)).toBe('# Create a 3D point.');
  });
});

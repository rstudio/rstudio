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
  offscreenAbove:  `${FILE_PREFIX}offscreen_above.R`,
  offscreenBelow:  `${FILE_PREFIX}offscreen_below.R`,
  offscreenClick:  `${FILE_PREFIX}offscreen_click.R`,
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

test.describe('Edit suggestions (showEditSuggestion injection)', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
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

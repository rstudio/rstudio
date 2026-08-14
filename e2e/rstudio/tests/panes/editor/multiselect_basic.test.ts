// Basic multi-select coverage for the code paths guarded by
// acemixins/multi_select_guard (#13605): forEachSelection (typing with
// multiple cursors or ranges active) and $moveLines (moving lines with
// multiple cursors). The guard wraps both with snapshot/restore semantics
// and a corrupt-state check runs on tab activation and editor focus, so
// these tests pin the everyday behaviors those layers must leave intact,
// driving them through the real keyboard shortcuts rather than the
// automation bridge. Exception and recovery scenarios live in
// multiselect_recovery.test.ts.

import { test, expect } from '@fixtures/rstudio.fixture';
import { Page } from '@playwright/test';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { executeCommand } from '@utils/commands';

const FILE = 'multiselect_basic.R';

// Count occurrences of a single character in the editor value. Asserting an
// exact count catches input landing at more or fewer cursors than expected.
function countChar(value: string, ch: string): number {
  return value.split(ch).length - 1;
}

// Open FILE with `content`, focus the source editor, and put the cursor at
// the start of line 1.
async function openAtLineOne(page: Page, dir: string, content: string): Promise<AceEditor> {
  await writeAndOpenFile(page, dir, FILE, content);

  const editor = new AceEditor(page, '');
  await expect.poll(() => editor.getValue()).toBe(content);

  await executeCommand(page, 'activateSource');
  await editor.gotoLine(1);
  return editor;
}

// Press the addCursorBelow shortcut (Ctrl+Alt+Down on every platform) until
// `count` cursors are active, verifying the multi-select bookkeeping after
// each press. The shortcut dispatches through the GWT ShortcutManager as a
// focused-editor AppCommand, so this also exercises the
// EXECUTION_POLICY_FOCUSED routing to the last-focused editor.
async function addCursorsBelowTo(page: Page, editor: AceEditor, count: number): Promise<void> {
  for (let i = 2; i <= count; i++) {
    await page.keyboard.press('Control+Alt+ArrowDown');
    await expect
      .poll(async () => {
        const state = await editor.getMultiSelectState();
        return { inMultiSelect: state.editorInMultiSelectMode, rangeCount: state.rangeCount };
      })
      .toEqual({ inMultiSelect: true, rangeCount: i });
  }
}

test.describe('Multi-select basics', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    await new ConsolePaneActions(page).resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // leave multi-select mode before the buffer is closed out from under it
    await page.keyboard.press('Escape');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE]);
  });

  test('typing lands at every cursor, and Escape returns to a single cursor', async ({ rstudioPage: page }) => {
    const editor = await openAtLineOne(page, sandbox.dir, 'alpha\nbravo\ncharlie');
    await addCursorsBelowTo(page, editor, 3);

    await page.keyboard.type('X');
    await expect.poll(() => editor.getValue()).toBe('Xalpha\nXbravo\nXcharlie');

    await page.keyboard.press('Escape');
    await expect
      .poll(async () => (await editor.getMultiSelectState()).editorInMultiSelectMode)
      .toBe(false);

    // back in single-select, typing lands at exactly one cursor
    await page.keyboard.type('Z');
    await expect.poll(async () => countChar(await editor.getValue(), 'Z')).toBe(1);
  });

  test('a multi-cursor edit undoes in a single step', async ({ rstudioPage: page }) => {
    const editor = await openAtLineOne(page, sandbox.dir, 'aaa\nbbb\nccc');
    await addCursorsBelowTo(page, editor, 3);

    await page.keyboard.type('X');
    await expect.poll(() => editor.getValue()).toBe('Xaaa\nXbbb\nXccc');

    // the per-cursor inserts form one undo group
    await page.keyboard.press('ControlOrMeta+KeyZ');
    await expect.poll(() => editor.getValue()).toBe('aaa\nbbb\nccc');
  });

  test('splitIntoLines converts a selection into one range per line', async ({ rstudioPage: page }) => {
    const editor = await openAtLineOne(page, sandbox.dir, 'aaa\nbbb\nccc');

    await page.keyboard.press('ControlOrMeta+KeyA');
    await page.keyboard.press('Control+Alt+KeyA');

    await expect
      .poll(async () => {
        const state = await editor.getMultiSelectState();
        return { inMultiSelect: state.editorInMultiSelectMode, rangeCount: state.rangeCount };
      })
      .toEqual({ inMultiSelect: true, rangeCount: 3 });

    expect(await editor.getSelectionRanges()).toEqual([
      { start: { row: 0, column: 0 }, end: { row: 0, column: 3 } },
      { start: { row: 1, column: 0 }, end: { row: 1, column: 3 } },
      { start: { row: 2, column: 0 }, end: { row: 2, column: 3 } },
    ]);

    // typing replaces the selected text at every range
    await page.keyboard.type('X');
    await expect.poll(() => editor.getValue()).toBe('X\nX\nX');
  });

  test('moving lines with multiple cursors moves the selected block', async ({ rstudioPage: page }) => {
    const editor = await openAtLineOne(page, sandbox.dir, 'aaa\nbbb\nccc\nddd');
    await addCursorsBelowTo(page, editor, 3);

    // Alt+Down runs Ace's movelinesdown through the guarded $moveLines;
    // adjacent cursor rows move as one block
    await page.keyboard.press('Alt+ArrowDown');
    await expect.poll(() => editor.getValue()).toBe('ddd\naaa\nbbb\nccc');

    // the multi-select survives the move with no virtual-selection leftovers
    await expect.poll(() => editor.getMultiSelectState()).toEqual({
      editorInMultiSelectMode: true,
      selectionInMultiSelectMode: true,
      inVirtualSelectionMode: false,
      tempSelectionInstalled: false,
      rangeCount: 3,
      rangeListAttached: true,
    });

    await page.keyboard.press('Alt+ArrowUp');
    await expect.poll(() => editor.getValue()).toBe('aaa\nbbb\nccc\nddd');
  });
});

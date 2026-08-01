// Regression tests for multi-select state corruption (#13605).
//
// Ace's forEachSelection() and $moveLines() mutate selection state
// (temporary Selection, silenced event registry, inVirtualSelectionMode,
// detached range list) and restore it with no exception protection, so an
// exception thrown from a document listener during a multi-cursor edit used
// to leave the editor permanently broken: typing silently discarded, mouse
// selection dead, multi-select mode un-exitable. Only reloading the IDE
// recovered. These tests exercise the two layers of defense added for that
// failure mode:
//
//   1. the acesupport wrapper (mixins/multi_select_guard) that restores the
//      mutated state when an exception aborts the operation, and
//   2. the corrupt-state detection/reset that runs when an editor tab is
//      activated or the editor is refocused
//      (AceEditorNative.getMultiSelectCorruptionReason).

import { test, expect } from '@fixtures/rstudio.fixture';
import { Page } from '@playwright/test';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { drainClientExceptions, documentOpen, executeCommand } from '@utils/commands';
import * as path from 'path';

const ERROR_MARKER = '[pw:13605] intentional listener error';

// Count occurrences of a single character in the editor value. Asserting an
// exact count (rather than toContain) catches a stuck multi-select inserting
// the character at every stranded cursor.
function countChar(value: string, ch: string): number {
  return value.split(ch).length - 1;
}

// Drain the intentionally-raised exception so the fixture doesn't fail the
// test for it. How (and whether) the raw throw gets recorded is incidental
// -- the sentinel checked by the tests proves the injection fired -- so the
// marker's presence is not asserted; anything else recorded is a real
// failure.
async function drainInjectedException(page: Page): Promise<void> {
  const errors = await drainClientExceptions(page);
  const unexpected = errors.filter((e) => !e.message.includes(ERROR_MARKER));
  expect(unexpected).toEqual([]);
}

// Place a cursor on each of the first three lines and wait for the
// multi-select bookkeeping to reflect all three ranges.
async function addCursorsOnThreeLines(page: Page, editor: AceEditor): Promise<void> {
  await executeCommand(page, 'activateSource');
  await editor.gotoLine(1);
  await editor.execCommand('addCursorBelow');
  await editor.execCommand('addCursorBelow');
  await expect
    .poll(async () => {
      const state = await editor.getMultiSelectState();
      return { inMultiSelect: state.editorInMultiSelectMode, rangeCount: state.rangeCount };
    })
    .toEqual({ inMultiSelect: true, rangeCount: 3 });
}

test.describe('Multi-select recovery', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [
      'multiselect_recovery.R',
      'multiselect_other.R',
    ]);
  });

  test('editor survives an exception thrown during a multi-cursor edit', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, 'multiselect_recovery.R', 'aaa\nbbb\nccc\n');

    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getValue()).toContain('ccc');
    await addCursorsOnThreeLines(page, editor);

    // arrange for the next document change to throw from a change listener,
    // aborting Ace's forEachSelection mid-iteration, then type with the
    // multiple cursors active to trigger it
    await editor.injectThrowingChangeListener(ERROR_MARKER);
    await page.keyboard.type('X');
    await expect.poll(() => editor.throwingChangeListenerFired()).toBe(true);

    // prove the exception fired mid-iteration: forEachSelection applies the
    // edit per-range from the last range upward, so an abort on the first
    // (bottom-most) insert leaves the other lines untouched
    await expect.poll(() => editor.getValue()).toBe('aaa\nbbb\nXccc\n');

    // the aborted operation must not leave its temporary state behind
    await expect
      .poll(async () => {
        const state = await editor.getMultiSelectState();
        return {
          inVirtualSelectionMode: state.inVirtualSelectionMode,
          tempSelectionInstalled: state.tempSelectionInstalled,
        };
      })
      .toEqual({ inVirtualSelectionMode: false, tempSelectionInstalled: false });

    // multi-select mode must still be exitable
    await page.keyboard.press('Escape');
    await expect
      .poll(async () => (await editor.getMultiSelectState()).editorInMultiSelectMode)
      .toBe(false);

    // and subsequent typing must reach the document at exactly one cursor
    await page.keyboard.type('Z');
    await expect.poll(async () => countChar(await editor.getValue(), 'Z')).toBe(1);

    await drainInjectedException(page);
  });

  test('editor survives an exception thrown while moving lines with multiple cursors', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, 'multiselect_recovery.R', 'aaa\nbbb\nccc\nddd\n');

    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getValue()).toContain('ddd');
    await addCursorsOnThreeLines(page, editor);

    // $moveLines detaches the range list and sets inVirtualSelectionMode
    // while moving; aborting it mid-move must not leave either behind. The
    // command runs via evaluate, so the injected throw propagates back out
    // of the call itself -- expected here.
    await editor.injectThrowingChangeListener(ERROR_MARKER);
    await editor.execCommand('movelinesdown').catch((e) => {
      if (!String(e).includes(ERROR_MARKER))
        throw e;
    });
    await expect.poll(() => editor.throwingChangeListenerFired()).toBe(true);

    await expect
      .poll(async () => {
        const state = await editor.getMultiSelectState();
        return {
          inVirtualSelectionMode: state.inVirtualSelectionMode,
          rangeListAttached: state.rangeListAttached,
        };
      })
      .toEqual({ inVirtualSelectionMode: false, rangeListAttached: true });

    // multi-select mode must still be exitable, and typing must reach the
    // document at exactly one cursor afterwards
    await page.keyboard.press('Escape');
    await expect
      .poll(async () => (await editor.getMultiSelectState()).editorInMultiSelectMode)
      .toBe(false);
    await page.keyboard.type('Z');
    await expect.poll(async () => countChar(await editor.getValue(), 'Z')).toBe(1);

    await drainInjectedException(page);
  });

  test('a healthy multi-cursor selection survives tab switches', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, 'multiselect_recovery.R', 'aaa\nbbb\nccc\n');
    await writeAndOpenFile(page, sandbox.dir, 'multiselect_other.R', 'other tab\n');

    const targetPath = path.join(sandbox.dir, 'multiselect_recovery.R');
    await documentOpen(page, targetPath);

    const editor = new AceEditor(page, '');
    await addCursorsOnThreeLines(page, editor);

    // switching away and back runs the corrupt-state check on activation
    // and again on focus; a healthy selection must not be flagged
    await documentOpen(page, path.join(sandbox.dir, 'multiselect_other.R'));
    await documentOpen(page, targetPath);

    await expect
      .poll(async () => {
        const state = await editor.getMultiSelectState();
        return { inMultiSelect: state.editorInMultiSelectMode, rangeCount: state.rangeCount };
      })
      .toEqual({ inMultiSelect: true, rangeCount: 3 });

    // typing lands at all three cursors
    await executeCommand(page, 'activateSource');
    await page.keyboard.type('W');
    await expect.poll(async () => countChar(await editor.getValue(), 'W')).toBe(3);
  });

  test('corrupt multi-select state is repaired on tab switch', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, 'multiselect_recovery.R', 'recovery target\nsecond\nthird\n');
    await writeAndOpenFile(page, sandbox.dir, 'multiselect_other.R', 'other tab\n');

    const targetPath = path.join(sandbox.dir, 'multiselect_recovery.R');
    await documentOpen(page, targetPath);

    const editor = new AceEditor(page, '');
    await addCursorsOnThreeLines(page, editor);

    // corrupt the live multi-select the way an aborted operation can:
    // detach the range list so it stops tracking document edits (a shape
    // the JS guard cannot repair after the fact)
    await editor.detachRangeList();

    // switch away and back; the corrupt-state check (which runs on both
    // tab activation and refocus) should detect and reset the corruption,
    // dropping the extra cursors
    await documentOpen(page, path.join(sandbox.dir, 'multiselect_other.R'));
    await documentOpen(page, targetPath);

    await expect
      .poll(async () => {
        const state = await editor.getMultiSelectState();
        return {
          editorInMultiSelectMode: state.editorInMultiSelectMode,
          selectionInMultiSelectMode: state.selectionInMultiSelectMode,
          rangeCount: state.rangeCount,
        };
      })
      .toEqual({
        editorInMultiSelectMode: false,
        selectionInMultiSelectMode: false,
        rangeCount: 0,
      });

    // the recovered editor accepts input, at exactly one cursor
    await executeCommand(page, 'activateSource');
    await editor.navigateLineEnd();
    await page.keyboard.type('Q');
    await expect.poll(async () => countChar(await editor.getValue(), 'Q')).toBe(1);
  });

  test('corrupt multi-select state is repaired when the editor is refocused', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, 'multiselect_recovery.R', 'recovery target\n');

    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getValue()).toContain('recovery');
    await executeCommand(page, 'activateSource');

    // simulate a lost 'singleSelect' notification: editor flags set while
    // the selection is still in single-select mode (the flag-mismatch
    // corruption branch)
    await page.evaluate(() => {
      const active = window.rstudio?.documents.activeEditor() as unknown as {
        inMultiSelectMode?: boolean;
        inVirtualSelectionMode?: boolean;
      } | null;
      if (!active)
        throw new Error('no active editor');
      active.inMultiSelectMode = true;
      active.inVirtualSelectionMode = true;
    });

    // no tab switch: move focus to the console and back to the editor; the
    // deferred focus-time check must detect and reset the corrupt state
    await executeCommand(page, 'activateConsole');
    await executeCommand(page, 'activateSource');

    await expect
      .poll(async () => {
        const state = await editor.getMultiSelectState();
        return {
          editorInMultiSelectMode: state.editorInMultiSelectMode,
          inVirtualSelectionMode: state.inVirtualSelectionMode,
        };
      })
      .toEqual({ editorInMultiSelectMode: false, inVirtualSelectionMode: false });

    // the recovered editor accepts input, at exactly one cursor
    await editor.navigateLineEnd();
    await page.keyboard.type('Q');
    await expect.poll(async () => countChar(await editor.getValue(), 'Q')).toBe(1);
  });
});

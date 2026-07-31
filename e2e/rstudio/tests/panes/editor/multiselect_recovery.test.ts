// Regression tests for multi-select state corruption (#13605).
//
// Ace's forEachSelection() mutates selection state (temporary Selection,
// silenced event registry, inVirtualSelectionMode) and restores it with no
// exception protection, so an exception thrown from a document listener
// during a multi-cursor edit used to leave the editor permanently broken:
// typing silently discarded, mouse selection dead, multi-select mode
// un-exitable. Only reloading the IDE recovered. These tests exercise the
// two layers of defense added for that failure mode:
//
//   1. the acesupport wrapper (mixins/multi_select_guard) that restores the
//      mutated state when an exception aborts the operation, and
//   2. the corrupt-state detection/reset that runs when an editor tab is
//      (re)activated (AceEditorNative.isMultiSelectStateCorrupt).

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { drainClientExceptions, documentOpen, executeCommand } from '@utils/commands';
import * as path from 'path';

const ERROR_MARKER = '[pw:13605] intentional listener error';

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

    // place a cursor on each of the three lines
    await executeCommand(page, 'activateSource');
    await editor.gotoLine(1);
    await editor.execCommand('addCursorBelow');
    await editor.execCommand('addCursorBelow');
    await expect
      .poll(async () => (await editor.getMultiSelectState()).editorInMultiSelectMode)
      .toBe(true);

    // arrange for the next document change to throw from a change listener,
    // aborting Ace's forEachSelection mid-iteration, then type with the
    // multiple cursors active to trigger it
    await editor.injectThrowingChangeListener(ERROR_MARKER);
    await page.keyboard.type('X');

    // prove the exception fired mid-iteration: forEachSelection applies the
    // edit per-range from the last range upward, so an abort on the first
    // (bottom-most) insert leaves the other lines untouched
    await expect.poll(() => editor.getValue()).toContain('Xccc');
    expect(await editor.getValue()).not.toContain('Xaaa');

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

    // and subsequent typing must still reach the document
    await page.keyboard.type('Z');
    await expect.poll(() => editor.getValue()).toContain('Z');

    // drain the intentionally-raised exception so the fixture doesn't fail
    // the test for it. The marker must be present -- its absence means the
    // fault injection never fired and the test proved nothing -- and
    // anything else recorded is a real failure.
    const errors = await drainClientExceptions(page);
    expect(errors.some((e) => e.message.includes(ERROR_MARKER))).toBe(true);
    const unexpected = errors.filter((e) => !e.message.includes(ERROR_MARKER));
    expect(unexpected).toEqual([]);
  });

  test('corrupt multi-select state is repaired when the tab is reactivated', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, 'multiselect_recovery.R', 'recovery target\n');
    await writeAndOpenFile(page, sandbox.dir, 'multiselect_other.R', 'other tab\n');

    // activate the first tab and simulate the stuck state left behind by an
    // aborted multi-select operation (editor flags set with no ranges)
    const targetPath = path.join(sandbox.dir, 'multiselect_recovery.R');
    await documentOpen(page, targetPath);

    const editor = new AceEditor(page, '');
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

    // switch away and back; tab activation should detect and reset the
    // corrupt state
    await documentOpen(page, path.join(sandbox.dir, 'multiselect_other.R'));
    await documentOpen(page, targetPath);

    await expect
      .poll(async () => {
        const state = await editor.getMultiSelectState();
        return {
          editorInMultiSelectMode: state.editorInMultiSelectMode,
          inVirtualSelectionMode: state.inVirtualSelectionMode,
        };
      })
      .toEqual({ editorInMultiSelectMode: false, inVirtualSelectionMode: false });

    // the recovered editor accepts input
    await executeCommand(page, 'activateSource');
    await editor.navigateLineEnd();
    await page.keyboard.type('Q');
    await expect.poll(() => editor.getValue()).toContain('Q');
  });
});

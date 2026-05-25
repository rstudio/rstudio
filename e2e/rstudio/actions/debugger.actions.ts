import type { Page } from 'playwright';
import { expect } from '@playwright/test';
import { DebuggerPage } from '../pages/debugger.page';
import { ConsolePaneActions } from './console_pane.actions';
import { TIMEOUTS } from '../utils/constants';

// Click position inside an Ace gutter cell that lands on the breakpoint
// hit area (left edge of the cell, before the line-number text). Pixel
// values are tuned to RStudio's current Ace gutter padding.
const GUTTER_BREAKPOINT_HIT_AREA = { x: 4, y: 8 } as const;

export class DebuggerActions {
  readonly page: Page;
  readonly debuggerPage: DebuggerPage;
  readonly consoleActions: ConsolePaneActions;

  constructor(page: Page, consoleActions: ConsolePaneActions) {
    this.page = page;
    this.debuggerPage = new DebuggerPage(page);
    this.consoleActions = consoleActions;
  }

  /** Click the breakpoint area of the gutter cell for `line` (1-indexed).
   *  Waits until any breakpoint marker (active / pending / inactive) appears
   *  on the cell — the test caller can assert further on which class shows
   *  up if a specific state matters. */
  async setBreakpoint(line: number): Promise<void> {
    const before = await this.debuggerPage.anyBreakpointMarker.count();
    const cell = this.debuggerPage.gutterCellForLine(line);
    await cell.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    await cell.click({ position: GUTTER_BREAKPOINT_HIT_AREA });
    await expect.poll(() => this.debuggerPage.anyBreakpointMarker.count(), {
      timeout: TIMEOUTS.fileOpen,
    }).toBeGreaterThan(before);
  }

  /** Toggle the breakpoint at the cursor's current position via Shift+F9
   *  (the keyboard shortcut bound to debugBreakpoint). Waits until the
   *  total breakpoint marker count changes -- the toggle either added a
   *  new marker (in any of active/pending/inactive states) or removed the
   *  existing one. */
  async toggleBreakpointAtCursor(): Promise<void> {
    const beforeCount = await this.debuggerPage.anyBreakpointMarker.count();
    await this.page.keyboard.press('Shift+F9');
    await expect.poll(
      () => this.debuggerPage.anyBreakpointMarker.count(),
      { timeout: TIMEOUTS.fileOpen },
    ).not.toBe(beforeCount);
  }

  /** Click the toolbar "Next" button (debugStep, F10). See
   *  {@link waitForDebugAdvance} for the wait semantics. */
  async stepOver(): Promise<void> {
    const beforeRow = await this.getActiveDebugLineRow().catch(() => null);
    await this.debuggerPage.stepBtn.click();
    await this.waitForDebugAdvance(beforeRow);
  }

  /** Click the toolbar Step Into button (debugStepInto, Shift+F4). See
   *  {@link waitForDebugAdvance} for the wait semantics. */
  async stepInto(): Promise<void> {
    const beforeRow = await this.getActiveDebugLineRow().catch(() => null);
    await this.debuggerPage.stepIntoBtn.click();
    await this.waitForDebugAdvance(beforeRow);
  }

  /** Click the toolbar Finish button (debugFinish, Shift+F7). See
   *  {@link waitForDebugAdvance} for the wait semantics. */
  async stepOut(): Promise<void> {
    const beforeRow = await this.getActiveDebugLineRow().catch(() => null);
    await this.debuggerPage.finishBtn.click();
    await this.waitForDebugAdvance(beforeRow);
  }

  /** Click the toolbar Continue button (debugContinue, Shift+F5). See
   *  {@link waitForDebugAdvance} for the wait semantics. */
  async continueDebug(): Promise<void> {
    const beforeRow = await this.getActiveDebugLineRow().catch(() => null);
    await this.debuggerPage.continueBtn.click();
    await this.waitForDebugAdvance(beforeRow);
  }

  /** Click the toolbar Stop button (debugStop, Shift+F8). Waits until the
   *  debug toolbar is gone -- R has dispatched the stop and the session is
   *  back at the global prompt. */
  async stopDebug(): Promise<void> {
    await this.debuggerPage.stopBtn.click();
    await expect(this.debuggerPage.debugToolbar).not.toBeVisible({
      timeout: TIMEOUTS.fileOpen,
    });
  }

  /** Wait until the active debug line moves off `beforeRow` (R landed
   *  somewhere new -- next breakpoint, into a callee, back in the caller)
   *  or the debug toolbar disappears (debug mode exited). The two outcomes
   *  cover every well-formed Step/Continue/Finish, so the caller doesn't
   *  need a blind settle on top. */
  private async waitForDebugAdvance(beforeRow: number | null): Promise<void> {
    await expect.poll(async () => {
      const inDebug = await this.debuggerPage.debugToolbar
        .isVisible()
        .catch(() => false);
      if (!inDebug) return true;
      const currentRow = await this.getActiveDebugLineRow().catch(() => null);
      return currentRow !== null && currentRow !== beforeRow;
    }, { timeout: TIMEOUTS.fileOpen }).toBe(true);
  }

  /** Wait until the debug toolbar is visible and the console prompt has
   *  switched to `Browse[N]>`. */
  async waitForDebugMode(): Promise<void> {
    await expect(this.debuggerPage.debugToolbar).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(this.consoleActions.consolePane.consoleOutput).toContainText(
      /Browse\[\d+\]>/,
      { timeout: TIMEOUTS.fileOpen },
    );
  }

  /** Wait until the debug toolbar is gone — i.e., debug mode has exited. */
  async waitForDebugExit(): Promise<void> {
    await expect(this.debuggerPage.debugToolbar).not.toBeVisible({ timeout: TIMEOUTS.fileOpen });
  }

  /** Read the editor row (0-indexed) of the active debug line via Ace's
   *  pixel→screen mapping.
   *
   *  Filters by visibility rather than XPath scope: with multiple editor
   *  tabs open from prior serial-block tests, stale .ace_active_debug_line
   *  elements can linger in hidden tabs; those have a zero-size
   *  getBoundingClientRect because their tab isn't laid out. Pick the
   *  first one that's actually rendered, then walk up to its containing
   *  .ace_editor for the pixel→screen mapping.
   *
   *  Throws if no visibly-rendered active debug line is found — callers
   *  that need to poll should wrap the call in `expect.poll`, which retries
   *  on thrown errors. */
  async getActiveDebugLineRow(): Promise<number> {
    const row = await this.page.evaluate(`(function() {
      var candidates = document.querySelectorAll('.ace_active_debug_line');
      var debugLine = null;
      for (var i = 0; i < candidates.length; i++) {
        var r = candidates[i].getBoundingClientRect();
        if (r.width > 0 && r.height > 0) { debugLine = candidates[i]; break; }
      }
      if (!debugLine) return null;
      var aceEditor = debugLine.closest('.ace_editor');
      if (!aceEditor) return null;
      var env = aceEditor.env;
      if (!env || !env.editor) return null;
      var rect = debugLine.getBoundingClientRect();
      var coords = env.editor.session.renderer.pixelToScreenCoordinates(
        rect.x + rect.width / 2,
        rect.y + rect.height / 2
      );
      return coords.row;
    })()`) as number | null;
    if (row === null) {
      throw new Error('No visible .ace_active_debug_line element found');
    }
    return row;
  }

  /** Read the gutter row label of the cell that currently displays the
   *  executing-line icon. Returns the line number text (e.g., "3"). */
  async getExecutingLineGutterText(): Promise<string> {
    const el = this.debuggerPage.executingLineGutter.first();
    await el.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    return (await el.innerText()).trim();
  }
}

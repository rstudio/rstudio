import type { Page } from 'playwright';
import { expect } from '@playwright/test';
import { DebuggerPage } from '../pages/debugger.page';
import { ConsolePaneActions } from './console_pane.actions';
import { TIMEOUTS, sleep } from '../utils/constants';

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
   *  Waits until the breakpoint count increases. */
  async setBreakpoint(line: number): Promise<void> {
    const before = await this.debuggerPage.breakpoints.count();
    const cell = this.debuggerPage.gutterCellForLine(line);
    await cell.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    // The breakpoint hit area is the leftmost few pixels of the gutter cell,
    // before the line-number text. Click at x=4 to land in it.
    await cell.click({ position: { x: 4, y: 8 } });
    await expect.poll(() => this.debuggerPage.breakpoints.count(), {
      timeout: TIMEOUTS.fileOpen,
    }).toBeGreaterThan(before);
  }

  /** Click the gutter cell for `line` to clear an existing breakpoint. */
  async clearBreakpoint(line: number): Promise<void> {
    const before = await this.debuggerPage.breakpoints.count();
    const cell = this.debuggerPage.gutterCellForLine(line);
    await cell.click({ position: { x: 4, y: 8 } });
    await expect.poll(() => this.debuggerPage.breakpoints.count(), {
      timeout: TIMEOUTS.fileOpen,
    }).toBeLessThan(before);
  }

  /** Fire the debugClearBreakpoints command via the R API. The command
   *  prompts a confirmation dialog in some contexts; we accept it if it
   *  appears, but proceed regardless. */
  async clearAllBreakpoints(): Promise<void> {
    await this.consoleActions.typeInConsole(`.rs.api.executeCommand('debugClearBreakpoints')`);
    await sleep(TIMEOUTS.settleDelay);
  }

  /** Toggle the breakpoint at the cursor's current position via Shift+F9
   *  (the keyboard shortcut bound to debugBreakpoint). */
  async toggleBreakpointAtCursor(): Promise<void> {
    await this.page.keyboard.press('Shift+F9');
    await sleep(TIMEOUTS.settleDelay);
  }

  /** Click the toolbar "Next" button (debugStep, F10). */
  async stepOver(): Promise<void> {
    await this.debuggerPage.stepBtn.click();
    await sleep(TIMEOUTS.settleDelay);
  }

  /** Click the toolbar Step Into button (debugStepInto, Shift+F4). */
  async stepInto(): Promise<void> {
    await this.debuggerPage.stepIntoBtn.click();
    await sleep(TIMEOUTS.settleDelay);
  }

  /** Click the toolbar Finish button (debugFinish, Shift+F7). */
  async stepOut(): Promise<void> {
    await this.debuggerPage.finishBtn.click();
    await sleep(TIMEOUTS.settleDelay);
  }

  /** Click the toolbar Continue button (debugContinue, Shift+F5). */
  async continueDebug(): Promise<void> {
    await this.debuggerPage.continueBtn.click();
    await sleep(TIMEOUTS.settleDelay);
  }

  /** Click the toolbar Stop button (debugStop, Shift+F8). */
  async stopDebug(): Promise<void> {
    await this.debuggerPage.stopBtn.click();
    await sleep(TIMEOUTS.settleDelay);
  }

  /** Wait until the debug toolbar is visible and the console prompt has
   *  switched to `Browse[N]>`. */
  async waitForDebugMode(): Promise<void> {
    await expect(this.debuggerPage.debugToolbar).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(this.debuggerPage.consoleOutput).toContainText(/Browse\[\d+\]>/, {
      timeout: TIMEOUTS.fileOpen,
    });
  }

  /** Wait until the debug toolbar is gone — i.e., debug mode has exited. */
  async waitForDebugExit(): Promise<void> {
    await expect(this.debuggerPage.debugToolbar).not.toBeVisible({ timeout: TIMEOUTS.fileOpen });
  }

  /** Read the editor row (0-indexed) of the active debug line via Ace's
   *  pixel→screen mapping, mirroring BRAT's approach for #15072.
   *
   *  Filters by visibility rather than XPath scope: with multiple editor
   *  tabs open from prior serial-block tests, stale .ace_active_debug_line
   *  elements can linger in hidden tabs; those have a zero-size
   *  getBoundingClientRect because their tab isn't laid out. Pick the
   *  first one that's actually rendered, then walk up to its containing
   *  .ace_editor for the pixel→screen mapping.
   *
   *  Returns -1 if no visibly-rendered active debug line is found. */
  async getActiveDebugLineRow(): Promise<number> {
    return await this.page.evaluate(`(function() {
      var candidates = document.querySelectorAll('.ace_active_debug_line');
      var debugLine = null;
      for (var i = 0; i < candidates.length; i++) {
        var r = candidates[i].getBoundingClientRect();
        if (r.width > 0 && r.height > 0) { debugLine = candidates[i]; break; }
      }
      if (!debugLine) return -1;
      var aceEditor = debugLine.closest('.ace_editor');
      if (!aceEditor) return -1;
      var env = aceEditor.env;
      if (!env || !env.editor) return -1;
      var rect = debugLine.getBoundingClientRect();
      var coords = env.editor.session.renderer.pixelToScreenCoordinates(
        rect.x + rect.width / 2,
        rect.y + rect.height / 2
      );
      return coords.row;
    })()`);
  }

  /** Read the gutter row label of the cell that currently displays the
   *  executing-line icon. Returns the line number text (e.g., "3"). */
  async getExecutingLineGutterText(): Promise<string> {
    const el = this.debuggerPage.executingLineGutter.first();
    await el.waitFor({ state: 'visible', timeout: TIMEOUTS.fileOpen });
    return (await el.innerText()).trim();
  }
}

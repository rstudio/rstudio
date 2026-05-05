import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { DebuggerActions } from '@actions/debugger.actions';
import { EnvironmentPane } from '@pages/environment_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS, sleep } from '@utils/constants';

const sandbox = useSuiteSandbox();

let consoleActions: ConsolePaneActions;
let debuggerActions: DebuggerActions;
let envPane: EnvironmentPane;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function rPath(p: string): string {
  return p.replace(/\\/g, '/');
}

async function writeAndOpen(fileName: string, content: string): Promise<void> {
  const fullPath = `${rPath(sandbox.dir)}/${fileName}`;
  // Build an R-side c("line1", "line2", ...) vector. JSON's double-quoted
  // string-escape rules (\\, \", \n, \t, \uXXXX) match R's, so JSON.stringify
  // each line and join with commas to produce a valid R argument list.
  const lines = content.split('\n').map(l => JSON.stringify(l)).join(', ');
  await consoleActions.typeInConsole(`writeLines(c(${lines}), "${fullPath}")`);
  await sleep(TIMEOUTS.settleDelay);
  await consoleActions.typeInConsole(`file.edit("${fullPath}")`);
  await sleep(TIMEOUTS.fileEditSettle);
}

async function resetAfterTest(): Promise<void> {
  // Per-test cleanup: stop debug if the toolbar is still up, then clear
  // the console. Buffers are not closed here — unique timestamped
  // filenames keep tests from colliding, and the suite-level beforeAll
  // already cleared any leftovers from a prior crashed run. We also
  // deliberately do NOT call debugClearBreakpoints — on Desktop it opens
  // an Electron-native messagebox that Playwright can't dismiss via DOM
  // selectors.
  try {
    if (await debuggerActions.debuggerPage.debugToolbar.isVisible()) {
      await debuggerActions.stopDebug();
      await debuggerActions.waitForDebugExit().catch((err) => {
        console.warn('[debugger.test] waitForDebugExit during cleanup failed:', err);
      });
    }
  } catch (err) {
    console.warn('[debugger.test] resetAfterTest cleanup error:', err);
  }
  await consoleActions.clearConsole();
}

// ---------------------------------------------------------------------------
// Test suite
// ---------------------------------------------------------------------------

test.describe('R debugger', () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    debuggerActions = new DebuggerActions(page, consoleActions);
    envPane = new EnvironmentPane(page);
    // Defensive: clear any leftover open buffers from a prior crashed run.
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  // -------------------------------------------------------------------------
  // Block 1 — Gutter breakpoints
  // -------------------------------------------------------------------------

  test.describe.serial('Gutter breakpoints', { tag: ['@serial'] }, () => {
    test.afterEach(resetAfterTest);

    test('top-level breakpoint pauses when the file is sourced', async () => {
      // Simplest end-to-end check: a top-level breakpoint outside any
      // function. setTopLevelBreakpoint sets STATE_ACTIVE immediately, no
      // function lookup. Sourcing the file fires it on the breakpoint line.
      const fileName = `bp_toplevel_${Date.now()}.R`;
      const content = [
        'x <- 1',
        'y <- 22',
        'z <- x + y',
      ].join('\n');

      await writeAndOpen(fileName, content);

      await debuggerActions.setBreakpoint(2);

      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');

      await debuggerActions.waitForDebugMode();
      await expect.poll(
        () => debuggerActions.getActiveDebugLineRow(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(1); // 0-indexed → line 2
    });

    test('single gutter breakpoint highlights correct row', async () => {
      // Regression coverage for rstudio/rstudio#15072.
      const fileName = `bp_brace_${Date.now()}.R`;
      const content = [
        'brace_fn <- function() {',
        '   1 + 1',
        '   {',
        '      2 + 2',
        '   }',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);

      // Source first so the function is in scope — that way the gutter
      // click produces an ACTIVE (.ace_breakpoint) marker rather than a
      // pending / inactive one that wouldn't satisfy our locator.
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);

      // Set breakpoint on the brace-expression line (line 3).
      await debuggerActions.setBreakpoint(3);

      await consoleActions.typeInConsole('brace_fn()');

      await debuggerActions.waitForDebugMode();

      // Breakpoint on line 3 (the `{` opening the brace block). The active
      // debug line lands on row 2 (= line 3, 0-indexed); the gutter
      // executing-line icon shows "3".
      await expect.poll(
        () => debuggerActions.getActiveDebugLineRow(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(2);

      const gutterText = await debuggerActions.getExecutingLineGutterText();
      expect(gutterText).toBe('3');
    });

    test('Shift+F9 toggles a breakpoint at the cursor', async () => {
      const fileName = `bp_toggle_${Date.now()}.R`;
      const content = [
        'toggle_fn <- function() {',
        '   a <- 1',
        '   b <- 2',
        '   a + b',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      // Source so the toggled breakpoint becomes ACTIVE rather than INACTIVE.
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);

      // Place the cursor on line 2 via the goToLine command.
      await consoleActions.goToLine(2);

      expect(await debuggerActions.debuggerPage.breakpoints.count()).toBe(0);

      await debuggerActions.toggleBreakpointAtCursor();
      await expect.poll(
        () => debuggerActions.debuggerPage.breakpoints.count(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(1);

      await debuggerActions.toggleBreakpointAtCursor();
      await expect.poll(
        () => debuggerActions.debuggerPage.breakpoints.count(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(0);
    });

    // NOTE: a `debugClearBreakpoints` test belongs here in principle, but
    // on Desktop the command opens an Electron-native confirmation
    // messagebox that Playwright can't dismiss via DOM selectors
    // (DesktopDialogBuilderFactory.showModal → Desktop.getFrame()
    // .showMessageBox). Driving Electron natively (electron.launch instead
    // of CDP) would fix it but is out of scope here. The Server-mode build
    // uses a real GWT dialog where #rstudio_dlg_yes is selectable.
  });

  // -------------------------------------------------------------------------
  // Block 2 — Stepping with the debug toolbar
  // -------------------------------------------------------------------------

  test.describe.serial('Stepping with toolbar', { tag: ['@serial'] }, () => {
    test.afterEach(resetAfterTest);

    test('Step Next advances to the following line', async () => {
      const fileName = `step_next_${Date.now()}.R`;
      const content = [
        'step_next_fn <- function() {',
        '   a <- 1',
        '   b <- 2',
        '   c <- 3',
        '   a + b + c',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      await debuggerActions.setBreakpoint(2);
      await consoleActions.typeInConsole('step_next_fn()');

      await debuggerActions.waitForDebugMode();
      const startRow = await debuggerActions.getActiveDebugLineRow();
      expect(startRow).toBeGreaterThanOrEqual(0);

      await debuggerActions.stepOver();
      await expect.poll(
        () => debuggerActions.getActiveDebugLineRow(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(startRow + 1);
    });

    test('Step Into descends into a nested function call', async () => {
      const fileName = `step_into_${Date.now()}.R`;
      const content = [
        'inner <- function() {',
        '   1 + 1',
        '}',
        'outer <- function() {',
        '   inner()',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      // Breakpoint on the line that calls inner() (line 5).
      await debuggerActions.setBreakpoint(5);
      await consoleActions.typeInConsole('outer()');

      await debuggerActions.waitForDebugMode();

      // Breakpoint at line 5 (the `inner()` call) → row 4 at debug entry.
      await expect.poll(
        () => debuggerActions.getActiveDebugLineRow(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(4);

      await debuggerActions.stepInto();

      // Step Into descends into inner(); the exact landing row depends on
      // R's debug state machine (function header vs first executable line),
      // so we assert (a) the marker moved off the inner() call line and
      // (b) it landed inside inner()'s body (rows 0–2). Combined, these
      // distinguish a real descent from "debug exited" or "stepped over".
      await expect.poll(
        () => debuggerActions.getActiveDebugLineRow(),
        { timeout: TIMEOUTS.fileOpen },
      ).not.toBe(4);
      const after = await debuggerActions.getActiveDebugLineRow();
      expect(after).toBeGreaterThanOrEqual(0);
      expect(after).toBeLessThanOrEqual(2);
      await expect(debuggerActions.debuggerPage.debugToolbar).toBeVisible();
    });

    test('Finish executes the remainder of the current function', async () => {
      const fileName = `step_finish_${Date.now()}.R`;
      const content = [
        'step_finish_fn <- function() {',
        '   a <- 1',
        '   a + 1',
        '   a + 2',
        '   a + 3',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      await debuggerActions.setBreakpoint(2);
      await consoleActions.typeInConsole('step_finish_fn()');

      await debuggerActions.waitForDebugMode();
      await debuggerActions.stepOut();

      await debuggerActions.waitForDebugExit();
      await expect(debuggerActions.debuggerPage.activeDebugLine).toHaveCount(0);
    });

    test('Continue jumps to the next breakpoint', async () => {
      // Continue-stepping coverage for rstudio/rstudio#15201 without the
      // package-build setup.
      const fileName = `step_continue_${Date.now()}.R`;
      const content = [
        'step_continue_fn <- function() {',
        '   a <- 1',
        '   b <- 2',
        '   c <- 3',
        '   d <- 4',
        '   a + b + c + d',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      await debuggerActions.setBreakpoint(2);
      await debuggerActions.setBreakpoint(5);
      await consoleActions.typeInConsole('step_continue_fn()');

      await debuggerActions.waitForDebugMode();
      await expect.poll(
        () => debuggerActions.getActiveDebugLineRow(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(1); // 0-indexed → line 2

      await debuggerActions.continueDebug();
      await expect.poll(
        () => debuggerActions.getActiveDebugLineRow(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(4); // 0-indexed → line 5
    });

    test('Stop button cleanly exits debug', async () => {
      const fileName = `step_stop_${Date.now()}.R`;
      const content = [
        'step_stop_fn <- function() {',
        '   a <- 1',
        '   a + 1',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      await debuggerActions.setBreakpoint(2);
      await consoleActions.typeInConsole('step_stop_fn()');

      await debuggerActions.waitForDebugMode();
      await debuggerActions.stopDebug();
      await debuggerActions.waitForDebugExit();

      await expect(debuggerActions.debuggerPage.activeDebugLine).toHaveCount(0);
    });

    test('Continue chains through six breakpoints', async () => {
      const fileName = `continue_chain_${Date.now()}.R`;
      const content = [
        'five_continues_fn <- function() {',
        '   a <- 1',
        '   b <- 2',
        '   c <- 3',
        '   d <- 4',
        '   e <- 5',
        '   f <- 6',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      for (const line of [2, 3, 4, 5, 6, 7]) {
        await debuggerActions.setBreakpoint(line);
      }
      await consoleActions.typeInConsole('five_continues_fn()');

      await debuggerActions.waitForDebugMode();
      await expect.poll(
        () => debuggerActions.getActiveDebugLineRow(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(1); // 0-indexed → line 2

      // Each Continue lands on the next breakpoint, and the prior
      // assignment becomes visible in the Environment pane.
      const stops: Array<{ row: number; name: string; value: string }> = [
        { row: 2, name: 'a', value: '1' },
        { row: 3, name: 'b', value: '2' },
        { row: 4, name: 'c', value: '3' },
        { row: 5, name: 'd', value: '4' },
        { row: 6, name: 'e', value: '5' },
      ];
      for (const { row, name, value } of stops) {
        await debuggerActions.continueDebug();
        await expect.poll(
          () => debuggerActions.getActiveDebugLineRow(),
          { timeout: TIMEOUTS.fileOpen },
        ).toBe(row);
        await expect.poll(
          () => envPane.hasVariable(name, value),
          { timeout: TIMEOUTS.fileOpen },
        ).toBe(true);
      }

      // 6th Continue runs the last line and exits debug cleanly.
      await debuggerActions.continueDebug();
      await debuggerActions.waitForDebugExit();
      await expect(debuggerActions.debuggerPage.activeDebugLine).toHaveCount(0);
    });
  });

  // -------------------------------------------------------------------------
  // Block 3 — browser() entry path
  // -------------------------------------------------------------------------

  test.describe.serial('browser() entry', { tag: ['@serial'] }, () => {
    test.afterEach(resetAfterTest);

    test('browser() in a function enters debug mode', async () => {
      const fileName = `browser_entry_${Date.now()}.R`;
      const content = [
        'browser_entry_fn <- function() {',
        '   browser()',
        '   x <- 1',
        '   x + 1',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      await consoleActions.typeInConsole('browser_entry_fn()');

      await debuggerActions.waitForDebugMode();
      // After browser() on line 2, R pauses on the browser() call itself
      // (row 1) before advancing. Assert the marker lands on row 1 or row
      // 2 — anywhere inside the function body but not on the function
      // declaration (row 0) or past the closing brace.
      const row = await debuggerActions.getActiveDebugLineRow();
      expect(row).toBeGreaterThanOrEqual(1);
      expect(row).toBeLessThanOrEqual(2);
    });

    test('Continue past browser() exits debug', async () => {
      const fileName = `browser_continue_${Date.now()}.R`;
      const content = [
        'browser_continue_fn <- function() {',
        '   browser()',
        '   "done"',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      await consoleActions.typeInConsole('browser_continue_fn()');

      await debuggerActions.waitForDebugMode();
      await debuggerActions.continueDebug();
      await debuggerActions.waitForDebugExit();
      await expect(debuggerActions.debuggerPage.activeDebugLine).toHaveCount(0);
    });
  });

  // -------------------------------------------------------------------------
  // Block 4 — Recover on error
  // -------------------------------------------------------------------------

  test.describe.serial('Recover on error', { tag: ['@serial'] }, () => {
    test.afterEach(resetAfterTest);

    test('Rerun with Debug link reopens the failing call in the debugger', async () => {
      const fileName = `rerun_${Date.now()}.R`;
      const content = [
        'rerun_fn <- function() {',
        '   stop("intentional")',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      await consoleActions.typeInConsole('rerun_fn()');

      // The error widget — and the "Rerun with Debug" link inside it —
      // should appear in the console after the error fires.
      await expect(debuggerActions.debuggerPage.rerunWithDebugLink).toBeVisible({
        timeout: TIMEOUTS.fileOpen,
      });

      await debuggerActions.debuggerPage.rerunWithDebugLink.click();
      await debuggerActions.waitForDebugMode();
      // Confirm the rerun actually paused inside rerun_fn rather than
      // bouncing out: an active debug line should be visible.
      await expect(debuggerActions.debuggerPage.activeDebugLine.first()).toBeVisible({
        timeout: TIMEOUTS.fileOpen,
      });
    });
  });

  // -------------------------------------------------------------------------
  // Block 5 — Environment + Traceback panes during debug
  // -------------------------------------------------------------------------

  test.describe.serial('Environment + Traceback', { tag: ['@serial'] }, () => {
    test.afterEach(resetAfterTest);

    test('Environment pane shows local frame variables', async () => {
      const fileName = `env_locals_${Date.now()}.R`;
      const content = [
        'env_locals_fn <- function() {',
        '   localx <- 42',
        '   browser()',
        '   localx',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      await consoleActions.typeInConsole('env_locals_fn()');

      await debuggerActions.waitForDebugMode();

      // Wait for the Environment pane to render the local frame.
      await expect.poll(
        () => envPane.hasVariable('localx', '42'),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);
    });

    test('Traceback pane lists the call stack', async () => {
      const fileName = `tb_stack_${Date.now()}.R`;
      const content = [
        'tb_h <- function() browser()',
        'tb_g <- function() tb_h()',
        'tb_f <- function() tb_g()',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      await consoleActions.typeInConsole('tb_f()');

      await debuggerActions.waitForDebugMode();

      // The Traceback panel renders each frame's call summary as text;
      // verify all three function names show up. (CSS class names that
      // would let us count rows are GWT-obfuscated, so we match by text.)
      await expect.poll(
        async () => {
          const text = await envPane.getPanelText();
          return ['tb_h', 'tb_g', 'tb_f'].every(name => text.includes(name));
        },
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);
    });

    test('Clicking a call frame switches Environment context', async () => {
      const fileName = `tb_click_${Date.now()}.R`;
      const content = [
        'fr_h <- function() {',
        '   marker_h <- "in_h"',
        '   browser()',
        '   marker_h',
        '}',
        'fr_g <- function() {',
        '   marker_g <- "in_g"',
        '   fr_h()',
        '}',
        'fr_f <- function() {',
        '   marker_f <- "in_f"',
        '   fr_g()',
        '}',
      ].join('\n');

      await writeAndOpen(fileName, content);
      await consoleActions.typeInConsole('.rs.api.executeCommand("sourceActiveDocument")');
      await sleep(TIMEOUTS.settleDelay);
      await consoleActions.typeInConsole('fr_f()');

      await debuggerActions.waitForDebugMode();

      // Initially we're inside fr_h — its local marker should be visible.
      await expect.poll(
        () => envPane.hasVariable('marker_h', 'in_h'),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);

      // Click the "fr_g" frame label. The call frames render inside the
      // Environment workbench panel; we click the first matching text node.
      await envPane.callFrameByText('fr_g').first().click();

      // After switching to fr_g's frame, marker_g becomes the visible local.
      await expect.poll(
        () => envPane.hasVariable('marker_g', 'in_g'),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);
    });
  });
});

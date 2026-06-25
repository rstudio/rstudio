import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { DebuggerActions } from '@actions/debugger.actions';
import { EnvironmentPane } from '@pages/environment_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS } from '@utils/constants';
import { executeCommand } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import { waitForConsoleIdle } from '@pages/console_pane.page';

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

  // wait:true so file.edit doesn't race the writeLines completion.
  await consoleActions.executeInConsole(
    `writeLines(c(${lines}), "${fullPath}")`,
    { wait: true },
  );

  await consoleActions.executeInConsole(`file.edit("${fullPath}")`);
  // file.edit returns quickly on the R side but the editor opens
  // asynchronously on the GWT side. Wait for the file's tab to actually
  // be the selected one -- a deterministic signal that the buffer is
  // ready to drive.
  const selectedTab = consoleActions.page.locator(
    "[class*='rstudio_source_panel'] [class*='PanelTab-selected']",
  );
  await expect(selectedTab).toContainText(fileName, { timeout: TIMEOUTS.fileOpen });
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
    // Per-test cleanup of leftover debug mode + source buffers happens in
    // the shared beforeEach (utils/test-reset.ts via fixtures/rstudio.fixture.ts).
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
      const content = heredoc`
        x <- 1
        y <- 22
        z <- x + y
      `;

      await writeAndOpen(fileName, content);
      await debuggerActions.setBreakpoint(2);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');

      await debuggerActions.waitForDebugMode();
      await debuggerActions.waitForActiveDebugLineRowToBe(1); // 0-indexed → line 2
    });

    test('single gutter breakpoint highlights correct row', async () => {
      // Regression coverage for rstudio/rstudio#15072.
      const fileName = `bp_brace_${Date.now()}.R`;
      const content = heredoc`
        brace_fn <- function() {
           1 + 1
           {
              2 + 2
           }
        }
      `;

      await writeAndOpen(fileName, content);

      // Source first so the function is in scope — that way the gutter
      // click produces an ACTIVE (.ace_breakpoint) marker rather than a
      // pending / inactive one that wouldn't satisfy our locator.
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);

      // Set breakpoint on the brace-expression line (line 3).
      await debuggerActions.setBreakpoint(3);

      await consoleActions.executeInConsole('brace_fn()');

      await debuggerActions.waitForDebugMode();

      // Breakpoint on line 3 (the `{` opening the brace block). The active
      // debug line lands on row 2 (= line 3, 0-indexed); the gutter
      // executing-line icon shows "3".
      await debuggerActions.waitForActiveDebugLineRowToBe(2);

      const gutterText = await debuggerActions.waitForExecutingLineGutterText();
      expect(gutterText).toBe('3');
    });

    test('Shift+F9 toggles a breakpoint at the cursor', async () => {
      const fileName = `bp_toggle_${Date.now()}.R`;
      const content = heredoc`
        toggle_fn <- function() {
           a <- 1
           b <- 2
           a + b
        }
      `;

      await writeAndOpen(fileName, content);
      // Source so the toggled breakpoint becomes ACTIVE rather than INACTIVE.
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);

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
      const content = heredoc`
        step_next_fn <- function() {
           a <- 1
           b <- 2
           c <- 3
           a + b + c
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      await debuggerActions.setBreakpoint(2);
      await consoleActions.executeInConsole('step_next_fn()');

      await debuggerActions.waitForDebugMode();
      const startRow = await debuggerActions.waitForActiveDebugLineRow();
      expect(startRow).toBeGreaterThanOrEqual(0);

      await debuggerActions.stepOver();
      await debuggerActions.waitForActiveDebugLineRowToBe(startRow + 1);
    });

    test('Step Into descends into a nested function call', async () => {
      const fileName = `step_into_${Date.now()}.R`;
      const content = heredoc`
        inner <- function() {
           1 + 1
        }
        outer <- function() {
           inner()
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      // Breakpoint on the line that calls inner() (line 5).
      await debuggerActions.setBreakpoint(5);
      await consoleActions.executeInConsole('outer()');

      await debuggerActions.waitForDebugMode();

      // Breakpoint at line 5 (the `inner()` call) → row 4 at debug entry.
      await debuggerActions.waitForActiveDebugLineRowToBe(4);

      await debuggerActions.stepInto();

      // Step Into descends into inner(); the exact landing row depends on
      // R's debug state machine (function header vs first executable line),
      // so we assert (a) the marker moved off the inner() call line and
      // (b) it landed inside inner()'s body (rows 0–2). Combined, these
      // distinguish a real descent from "debug exited" or "stepped over".
      await expect.poll(
        () => debuggerActions.getActiveDebugLineRow().catch(() => -1),
        { timeout: TIMEOUTS.fileOpen },
      ).not.toBe(4);
      const after = await debuggerActions.waitForActiveDebugLineRow();
      expect(after).toBeGreaterThanOrEqual(0);
      expect(after).toBeLessThanOrEqual(2);
      await expect(debuggerActions.debuggerPage.debugToolbar).toBeVisible();
    });

    test('Finish executes the remainder of the current function', async () => {
      const fileName = `step_finish_${Date.now()}.R`;
      const content = heredoc`
        step_finish_fn <- function() {
           a <- 1
           a + 1
           a + 2
           a + 3
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      await debuggerActions.setBreakpoint(2);
      await consoleActions.executeInConsole('step_finish_fn()');

      await debuggerActions.waitForDebugMode();
      await debuggerActions.stepOut();

      await debuggerActions.waitForDebugExit();
      await expect(debuggerActions.debuggerPage.activeDebugLine).toHaveCount(0);
    });

    test('Continue jumps to the next breakpoint', async () => {
      // Continue-stepping coverage for rstudio/rstudio#15201 without the
      // package-build setup.
      const fileName = `step_continue_${Date.now()}.R`;
      const content = heredoc`
        step_continue_fn <- function() {
           a <- 1
           b <- 2
           c <- 3
           d <- 4
           a + b + c + d
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      await debuggerActions.setBreakpoint(2);
      await debuggerActions.setBreakpoint(5);
      await consoleActions.executeInConsole('step_continue_fn()');

      await debuggerActions.waitForDebugMode();
      await debuggerActions.waitForActiveDebugLineRowToBe(1); // 0-indexed → line 2

      await debuggerActions.continueDebug();
      await debuggerActions.waitForActiveDebugLineRowToBe(4); // 0-indexed → line 5
    });

    test('Stop button cleanly exits debug', async () => {
      const fileName = `step_stop_${Date.now()}.R`;
      const content = heredoc`
        step_stop_fn <- function() {
           a <- 1
           a + 1
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      await debuggerActions.setBreakpoint(2);
      await consoleActions.executeInConsole('step_stop_fn()');

      await debuggerActions.waitForDebugMode();
      await debuggerActions.stopDebug();
      await debuggerActions.waitForDebugExit();

      await expect(debuggerActions.debuggerPage.activeDebugLine).toHaveCount(0);
    });

    test('Continue chains through six breakpoints', async () => {
      const fileName = `continue_chain_${Date.now()}.R`;
      const content = heredoc`
        five_continues_fn <- function() {
           a <- 1
           b <- 2
           c <- 3
           d <- 4
           e <- 5
           f <- 6
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      for (const line of [2, 3, 4, 5, 6, 7]) {
        await debuggerActions.setBreakpoint(line);
      }
      await consoleActions.executeInConsole('five_continues_fn()');

      await debuggerActions.waitForDebugMode();
      await debuggerActions.waitForActiveDebugLineRowToBe(1); // 0-indexed → line 2

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
        await debuggerActions.waitForActiveDebugLineRowToBe(row);
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
      const content = heredoc`
        browser_entry_fn <- function() {
           browser()
           x <- 1
           x + 1
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      await consoleActions.executeInConsole('browser_entry_fn()');

      await debuggerActions.waitForDebugMode();
      // After browser() on line 2, R pauses on the browser() call itself
      // (row 1) before advancing. Assert the marker lands on row 1 or row
      // 2 — anywhere inside the function body but not on the function
      // declaration (row 0) or past the closing brace.
      const row = await debuggerActions.waitForActiveDebugLineRow();
      expect(row).toBeGreaterThanOrEqual(1);
      expect(row).toBeLessThanOrEqual(2);
    });

    test('Continue past browser() exits debug', async () => {
      const fileName = `browser_continue_${Date.now()}.R`;
      const content = heredoc`
        browser_continue_fn <- function() {
           browser()
           "done"
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      await consoleActions.executeInConsole('browser_continue_fn()');

      await debuggerActions.waitForDebugMode();
      await debuggerActions.continueDebug();
      await debuggerActions.waitForDebugExit();
      await expect(debuggerActions.debuggerPage.activeDebugLine).toHaveCount(0);
    });

    test('captures environment for functions loaded via box::use', async () => {
      // Regression coverage for rstudio/rstudio#17743. Box modules live in
      // hermetic namespace:<mod> -> imports:<mod> -> ... chains that bypass
      // the user search path, so a bare-name .rs.captureCurrentEnvironment()
      // lookup from the browser's rho would fail with "could not find
      // function". RStdCallbacks.cpp now resolves it explicitly through
      // as.environment("tools:rstudio"). Without that fix, browserEnv stays
      // NIL, the Environment pane falls back to globalenv, and `localx`
      // would not appear.
      const missing = await consoleActions.ensurePackages(['box']);
      test.skip(missing.length > 0, `Missing: ${missing.join(', ')}`);

      const moduleName = `boxmod${Date.now()}`;
      const fullPath = `${rPath(sandbox.dir)}/${moduleName}.R`;
      const moduleContent = heredoc`
        foo <- function() {
           localx <- 42
           browser()
           localx
        }
      `;
      const lines = moduleContent.split('\n').map(l => JSON.stringify(l)).join(', ');
      await consoleActions.executeInConsole(
        `writeLines(c(${lines}), "${fullPath}")`,
        { wait: true },
      );

      // box::use(./<name>) resolves relative to cwd in interactive R
      // sessions, which is what an RStudio session is. setwd persists
      // beyond the test, but the rest of this file uses absolute sandbox
      // paths so a stale cwd is harmless.
      await consoleActions.executeInConsole(
        `setwd("${rPath(sandbox.dir)}")`,
        { wait: true },
      );
      await consoleActions.executeInConsole(
        `box::use(./${moduleName})`,
        { wait: true },
      );
      await consoleActions.executeInConsole(`${moduleName}$foo()`);

      await debuggerActions.waitForDebugMode();

      await expect.poll(
        () => envPane.hasVariable('localx', '42'),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);
    });
  });

  // -------------------------------------------------------------------------
  // Block 4 — Recover on error
  // -------------------------------------------------------------------------

  test.describe.serial('Recover on error', { tag: ['@serial'] }, () => {
    test.afterEach(resetAfterTest);

    test('Rerun with Debug link reopens the failing call in the debugger', async () => {
      const fileName = `rerun_${Date.now()}.R`;
      const content = heredoc`
        rerun_fn <- function() {
           stop("intentional")
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      await consoleActions.executeInConsole('rerun_fn()');

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
      const content = heredoc`
        env_locals_fn <- function() {
           localx <- 42
           browser()
           localx
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      await consoleActions.executeInConsole('env_locals_fn()');

      await debuggerActions.waitForDebugMode();

      // Wait for the Environment pane to render the local frame.
      await expect.poll(
        () => envPane.hasVariable('localx', '42'),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);
    });

    test('Traceback pane lists the call stack', async () => {
      const fileName = `tb_stack_${Date.now()}.R`;
      const content = heredoc`
        tb_h <- function() browser()
        tb_g <- function() tb_h()
        tb_f <- function() tb_g()
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      await consoleActions.executeInConsole('tb_f()');

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

    // Tracked by #18064 (re-enable on Server).
    // Server-on-Linux: clicking the call-frame label does not switch the
    // Environment context -- the post-click env-pane assertion below
    // (envPane.hasVariable('marker_g', 'in_g')) fails. Working hypothesis:
    // the click resolves a "fr_g" text node other than the call-stack label
    // (e.g. the source editor's breakpoint marker), so the frame never
    // switches; a locator scoped to the call-stack widget is likely needed.
    // Skip on Server until that is sorted.
    test('Clicking a call frame switches Environment context', { tag: ['@desktop_only'] }, async () => {
      const fileName = `tb_click_${Date.now()}.R`;
      const content = heredoc`
        fr_h <- function() {
           marker_h <- "in_h"
           browser()
           marker_h
        }
        fr_g <- function() {
           marker_g <- "in_g"
           fr_h()
        }
        fr_f <- function() {
           marker_f <- "in_f"
           fr_g()
        }
      `;

      await writeAndOpen(fileName, content);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await waitForConsoleIdle(consoleActions.page);
      await consoleActions.executeInConsole('fr_f()');

      await debuggerActions.waitForDebugMode();

      // Initially we're inside fr_h — its local marker should be visible.
      await expect.poll(
        () => envPane.hasVariable('marker_h', 'in_h'),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);

      // Click the "fr_g" frame label. The call frames render inside the
      // Environment workbench panel; we click the first matching text node.
      // The call-stack pane can render the frames a moment after
      // waitForDebugMode resolves, so wait for the specific frame to be
      // visible before clicking. The post-click env assertion below catches
      // any case where the click didn't actually land.
      const frGLabel = envPane.callFrameByText('fr_g').first();
      await expect(frGLabel).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await frGLabel.click();

      // After switching to fr_g's frame, marker_g becomes the visible local.
      await expect.poll(
        () => envPane.hasVariable('marker_g', 'in_g'),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);
    });
  });
});

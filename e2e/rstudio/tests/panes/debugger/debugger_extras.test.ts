// Additional debugger tests covering scenarios not exercised by
// debugger.test.ts:
//
//   - multi-line input at the Browse[N]> prompt preserves browser state
//   - clicking a top-level breakpoint marker toggles it off (#9450)
//   - Clear All Breakpoints removes every marker (#9450, @server_only --
//     on Desktop the confirm dialog is a native Electron messagebox)
//   - breakpoints fire inside S7 method definitions for S3 generics (#16490)
//
// The package-rebuild paths (#15201, #9450 package half) are tracked as
// `test.fixme` placeholders below -- they need a full devtools build
// cycle that fits poorly in Playwright's fixture model.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from 'playwright';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { DebuggerActions } from '@actions/debugger.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile } from '@utils/files';
import { TIMEOUTS, sleep } from '@utils/constants';
import { executeCommand } from '@utils/commands';

const sandbox = useSuiteSandbox();

let consoleActions: ConsolePaneActions;
let debuggerActions: DebuggerActions;

async function resetAfterTest(): Promise<void> {
  // Same shape as debugger.test.ts: bail out of the debugger if still
  // attached, then clear the console.
  try {
    if (await debuggerActions.debuggerPage.debugToolbar.isVisible()) {
      await debuggerActions.stopDebug();
      await debuggerActions.waitForDebugExit().catch(() => {});
    }
  } catch {
    /* best effort */
  }
  await consoleActions.clearConsole();
}

// Insert multi-line text into the console input as one raw insert and
// press Enter to submit. typeInConsole turns each \n into an Enter
// keystroke (Ace's keyboard handler treats it as commit-this-line), which
// would commit prematurely. keyboard.insertText sends the entire text via
// CDP's Input.insertText so R's parser sees it as one input and emits the
// continuation prompts ("+ ") the regression depends on.
async function consoleSubmitMultiline(page: Page, text: string): Promise<void> {
  await page.locator('#rstudio_console_input .ace_text-input').click({ force: true });
  await sleep(TIMEOUTS.layoutSettle);
  await page.keyboard.insertText(text);
  await sleep(TIMEOUTS.settleDelay);
  await page.keyboard.press('Enter');
}

// Poll the console; if R has stepped into a nested debug invocation
// (`debug at #N: ...`) on the way to evaluating our submitted block,
// fire Continue and keep waiting. Returns once `marker` shows up.
async function waitForOutputThroughNestedDebug(
  page: Page,
  consoleActions: ConsolePaneActions,
  debuggerActions: DebuggerActions,
  marker: RegExp,
  timeoutMs: number,
): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const out = await consoleActions.consolePane.consoleOutput.innerText();
    if (marker.test(out)) return;
    if (/(^|\n)debug at #/.test(out.slice(-500))) {
      // Inside a nested debug step -- send Continue and keep polling.
      await debuggerActions.continueDebug().catch(() => {});
    }
    await sleep(300);
  }
  const tail = (await consoleActions.consolePane.consoleOutput.innerText()).slice(-400);
  throw new Error(`waitForOutputThroughNestedDebug: ${marker} not seen. Tail:\n${tail}`);
}

test.describe('R debugger extras', () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    debuggerActions = new DebuggerActions(page, consoleActions);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  // Multi-line input at the Browse[N]> prompt previously triggered
  // continuation prompts ("+ ") that called setBrowserActive(false),
  // clearing the captured browser environment mid-eval and confusing
  // debugger introspection.
  test.describe.serial('Browser prompt', { tag: ['@serial'] }, () => {
    test.afterEach(resetAfterTest);

    test('multi-line input at Browse[N]> preserves browser state', async ({ rstudioPage: page }) => {
      const fileName = `browser_multiline_${Date.now()}.R`;
      const content = [
        'multiline_fn <- function() {',
        '   x <- 1',
        '   browser()',
        '   x + 1',
        '}',
        '',
      ].join('\n');
      await writeAndOpenFile(page, sandbox.dir, fileName, content);

      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await sleep(TIMEOUTS.settleDelay);

      await consoleActions.typeInConsole('multiline_fn()');
      await debuggerActions.waitForDebugMode();
      await expect(debuggerActions.debuggerPage.activeDebugLine.first())
        .toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // Submit a multi-line block at Browse[N]>. The {...} block forces R
      // to parse line-by-line, issuing continuation prompts ("+ ") between
      // lines; pre-fix those continuations cleared the captured browser
      // environment mid-eval.
      const multiline = [
        '{',
        '   status <- .rs.isBrowserActive()',
        '   envIsGlobal <- identical(',
        '      .Call("rs_getBrowserEnv", PACKAGE = "(embedding)"),',
        '      globalenv()',
        '   )',
        '   cat("DBG isBrowserActive:", status, "envIsGlobal:", envIsGlobal, "\\n")',
        '}',
      ].join('\n');
      await consoleSubmitMultiline(page, multiline);

      // R may step into a nested debug invocation while parsing the block
      // (the regression's whole point is that mid-block continuation
      // prompts shouldn't clear the captured browser environment). Walk
      // through that nested step if it shows up.
      await waitForOutputThroughNestedDebug(
        page,
        consoleActions,
        debuggerActions,
        /DBG isBrowserActive:\s*TRUE\s+envIsGlobal:\s*FALSE/,
        TIMEOUTS.consoleReady,
      );

      // Debug highlight should still be visible after the multi-line eval.
      await expect(debuggerActions.debuggerPage.activeDebugLine.first()).toBeVisible();
    });
  });

  // https://github.com/rstudio/rstudio/issues/9450 -- clicking a gutter
  // cell that already has a breakpoint should remove it (the original
  // issue is about "sticky" markers that survive the second click).
  test.describe.serial('Breakpoint toggle via gutter click', { tag: ['@serial'] }, () => {
    test.afterEach(resetAfterTest);

    test('clicking a top-level breakpoint marker toggles it off', async ({ rstudioPage: page }) => {
      const fileName = `bp_gutter_toggle_${Date.now()}.R`;
      const content = [
        'x <- 1',
        'y <- 2',
        'z <- x + y',
        '',
      ].join('\n');
      await writeAndOpenFile(page, sandbox.dir, fileName, content);

      // Set a breakpoint on line 2.
      await debuggerActions.setBreakpoint(2);
      await expect.poll(
        () => debuggerActions.debuggerPage.anyBreakpointMarker.count(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(1);

      // Click the same gutter cell to toggle the breakpoint off.
      await debuggerActions.debuggerPage.gutterCellForLine(2).click({ position: { x: 4, y: 8 } });

      // No marker in any visual state should remain.
      await expect.poll(
        () => debuggerActions.debuggerPage.anyBreakpointMarker.count(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(0);
    });
  });

  // https://github.com/rstudio/rstudio/issues/9450 -- "Clear All
  // Breakpoints" should remove every marker. On Desktop the command opens
  // an Electron-native messagebox that Playwright can't drive; the Server
  // build pops a real GWT dialog with #rstudio_dlg_yes.
  test.describe.serial('Clear All Breakpoints', { tag: ['@serial', '@server_only'] }, () => {
    test.afterEach(resetAfterTest);

    test('removes every breakpoint marker', async ({ rstudioPage: page }) => {
      const fileName = `bp_clear_all_${Date.now()}.R`;
      const content = ['x <- 1', 'y <- 2', 'z <- x + y', ''].join('\n');
      await writeAndOpenFile(page, sandbox.dir, fileName, content);

      // Place a top-level breakpoint on each of the three lines.
      for (const line of [1, 2, 3]) {
        await debuggerActions.setBreakpoint(line);
      }
      await expect.poll(
        () => debuggerActions.debuggerPage.anyBreakpointMarker.count(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(3);

      // Invoke Clear All Breakpoints and confirm the Yes/No dialog.
      await executeCommand(consoleActions.page, 'debugClearBreakpoints');
      const yesBtn = page.locator('#rstudio_dlg_yes');
      await expect(yesBtn).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await yesBtn.click();

      await expect.poll(
        () => debuggerActions.debuggerPage.anyBreakpointMarker.count(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(0);
    });
  });

  // https://github.com/rstudio/rstudio/issues/16490 -- breakpoints inside
  // S7::method() definitions for S3 generics should fire when the generic
  // dispatches to the S7 method.
  test.describe.serial('S7 method breakpoints', { tag: ['@serial'] }, () => {
    let s7Missing: string[] = [];

    test.beforeAll(async () => {
      // S7 installs from source on Linux PPM (or whatever ensurePackages
      // resolves to). Give it room.
      s7Missing = await consoleActions.ensurePackages(['S7'], 180_000);
    });

    test.afterEach(resetAfterTest);

    test('breakpoints fire in S7 methods for S3 generics (#16490)', async ({ rstudioPage: page }) => {
      test.skip(s7Missing.length > 0, `S7 not available: ${s7Missing.join(', ')}`);

      const fileName = `s7_breakpoint_${Date.now()}.R`;
      const content = [
        's7mean <- S7::new_class("s7mean")',
        'S7::method(mean, s7mean) <- function(x, ...) {',
        '   print("This is my S7 method.")',
        '}',
        '',
      ].join('\n');
      await writeAndOpenFile(page, sandbox.dir, fileName, content);

      // Breakpoint on the body of the S7 method (line 3 = the print() call).
      await debuggerActions.setBreakpoint(3);
      await executeCommand(consoleActions.page, 'sourceActiveDocument');
      await sleep(TIMEOUTS.settleDelay);

      // Dispatch through the S3 generic into the S7 method.
      await consoleActions.typeInConsole(
        '{ object <- structure(1:10, class = "s7mean"); mean(object) }',
      );

      await debuggerActions.waitForDebugMode();
      await expect.poll(
        () => debuggerActions.getExecutingLineGutterText(),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe('3');

      // Continue and verify the method actually printed its message.
      await debuggerActions.continueDebug();
      await debuggerActions.waitForDebugExit();
      await expect(consoleActions.consolePane.consoleOutput).toContainText(
        '[1] "This is my S7 method."',
      );
    });
  });

  // --- Package build/reload cycle (TODOs) ---------------------------------
  //
  // Two debugger tests that need a full `devtools::build()` + reload cycle
  // before debugging can be exercised. The build typically takes a minute
  // or more, which doesn't fit the Playwright fixture model. A future port
  // would need: a long timeout, a way to install devtools into the per-spec
  // sandbox without retriggering it across tests, and a project teardown
  // that survives the build cache.

  test.describe.serial('Package debugging across build cycles', { tag: ['@serial'] }, () => {
    test.fixme('package functions can be debugged after build and reload (#15201)', async () => {
      // Sketch:
      //   1. project.create(type = "package")
      //   2. closeAllSourceBuffersWithoutSaving; open R/example.R
      //   3. write a function with several lines, save, buildAll
      //   4. wait for the package's library() line to appear in the console
      //   5. click gutter to set breakpoints on lines 3 and 4
      //   6. assert .rs.isFunctionInSync("example", "R/example.R", pkg) is TRUE
      //   7. example() -> should pause at line 3, continue, pause at line 4
      //   8. close project
    });

    test.fixme('cleared package breakpoints stay cleared across rebuild (#9450)', async () => {
      // Sketch:
      //   1. project.create(type = "package"); add a function in R/
      //   2. buildAll, set breakpoint, then click the breakpoint marker to clear it
      //   3. buildAll again -- the PackageLoadedEvent triggers
      //      BreakpointManager.updatePackageBreakpoints; with the breakpoint
      //      already removed, no marker should reappear
      //   4. assert .ace_breakpoint count is 0 after the second build
    });
  });
});

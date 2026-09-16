import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { DebuggerActions } from '@actions/debugger.actions';
import { EnvironmentPane } from '@pages/environment_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile } from '@utils/files';
import { executeCommand } from '@utils/commands';
import { waitForConsoleIdle } from '@pages/console_pane.page';
import { heredoc } from '@utils/heredoc';

const sandbox = useSuiteSandbox();

test.describe('Source locations inside wrapper calls', () => {
  let consoleActions: ConsolePaneActions;
  let debuggerActions: DebuggerActions;
  let envPane: EnvironmentPane;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    debuggerActions = new DebuggerActions(page, consoleActions);
    envPane = new EnvironmentPane(page);
  });

  test.afterEach(async () => {
    if (await debuggerActions.debuggerPage.debugToolbar.isVisible())
      await debuggerActions.stopDebug();
  });

  for (const [wrapper, handler] of [
    ['tryCatch', ', error = identity'],
    ['withCallingHandlers', ', warning = identity'],
    ['suppressWarnings', ''],
  ]) {
    test(`${wrapper} highlights each statement in the browser frame`, async ({ rstudioPage: page }) => {
      await writeAndOpenFile(page, sandbox.dir, 'wrapped_debug.R', heredoc`
        wrapped_debug <- function() {
          ${wrapper}({
            browser()
            x <- 1
            y <- 2
            x + y
          }${handler})
        }
      ` + '\n');
      await executeCommand(page, 'sourceActiveDocument');
      await waitForConsoleIdle(page);
      await consoleActions.executeInConsole('wrapped_debug()');
      await debuggerActions.waitForDebugMode();

      await debuggerActions.waitForActiveDebugLineRowToBe(2);
      for (const row of [3, 4]) {
        await consoleActions.executeInConsole('n');
        await debuggerActions.waitForActiveDebugLineRowToBe(row);
      }
      await consoleActions.executeInConsole('c');
      await debuggerActions.waitForDebugExit();
    });
  }

  test('selecting an outer frame retains its call site', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, 'outer_debug.R', heredoc`
      inner_debug <- function() {
        tryCatch({
          browser()
          x <- 1
          y <- 2
        }, error = identity)
      }
      outer_debug <- function() {
        inner_debug()
      }
    ` + '\n');
    await executeCommand(page, 'sourceActiveDocument');
    await waitForConsoleIdle(page);
    await consoleActions.executeInConsole('outer_debug()');
    await debuggerActions.waitForDebugMode();
    await debuggerActions.waitForActiveDebugLineRowToBe(2);
    await consoleActions.executeInConsole('n');
    await debuggerActions.waitForActiveDebugLineRowToBe(3);

    await envPane.callFrameByText('outer_debug()').first().click();
    await debuggerActions.waitForActiveDebugLineRowToBe(8);
    await envPane.callFrameByText('inner_debug()').first().click();
    await debuggerActions.waitForActiveDebugLineRowToBe(3);
    await consoleActions.executeInConsole('n');
    await debuggerActions.waitForActiveDebugLineRowToBe(4);
    await consoleActions.executeInConsole('c');
    await debuggerActions.waitForDebugExit();
  });

  test('same-frame steps on a deep stack avoid rebuilding frame descriptors', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, 'deep_debug.R', heredoc`
      deep_debug <- function() {
        browser()
        x <- 1
        y <- 2
      }
    ` + '\n');
    await executeCommand(page, 'sourceActiveDocument');
    await waitForConsoleIdle(page);
    await consoleActions.executeInConsole(
      'descend_debug <- function(n) { if (n > 0) descend_debug(n - 1) else deep_debug() }',
    );
    await consoleActions.executeInConsole('descend_debug(30)');
    await debuggerActions.waitForDebugMode();
    await debuggerActions.waitForActiveDebugLineRowToBe(1);

    // Count full descriptor builds instead of asserting a machine-dependent
    // latency. Stack construction on entry is expected; same-frame steps
    // should resolve only the active frame's source location.
    await consoleActions.executeInConsole('.GlobalEnv$debug_frame_builds <- 0L');
    try {
      await consoleActions.executeInConsole(
        'trace(".rs.callFrames", tracer = quote(.GlobalEnv$debug_frame_builds <- .GlobalEnv$debug_frame_builds + 1L), print = FALSE, where = as.environment("tools:rstudio"))',
      );
      for (const row of [2, 3]) {
        await consoleActions.executeInConsole('n');
        await debuggerActions.waitForActiveDebugLineRowToBe(row);
      }
      await consoleActions.executeInConsole('cat("FRAME_BUILDS:", .GlobalEnv$debug_frame_builds, "\\n")');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('FRAME_BUILDS: 0');
    } finally {
      await consoleActions.executeInConsole(
        'untrace(".rs.callFrames", where = as.environment("tools:rstudio"))',
      );
    }
    await consoleActions.executeInConsole('c');
    await debuggerActions.waitForDebugExit();
  });
});

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { DebuggerActions } from '@actions/debugger.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { seedSandboxFile, writeAndOpenFile } from '@utils/files';
import { executeCommand } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import { rPathLiteral } from '@utils/r';
import { TIMEOUTS } from '@utils/constants';
import path from 'path';

interface DebugSourceCompletedEvent {
  type: string;
  data?: { path?: string; succeeded?: boolean };
}

// debugSource() is what the Source command runs when the document has
// breakpoints. It rewrites the file into a function whose source references
// carry the breakpoints (SessionBreakpoints.R, .rs.makeSourceEquivFunction),
// and cleans the injected browser calls out of the functions the file
// defined once it has run (.rs.executeDebugSource).
//
// Covered here, end to end:
//   - options(keep.source = FALSE) no longer breaks it, and the breakpoints
//     are still hit (#12375)
//   - an empty or comment-only file runs instead of erroring (#12375)
//   - cleanup leaves the bindings the file ended with alone, instead of
//     re-running the statements that defined the breakpointed functions
//
// The unit tests in src/cpp/tests/testthat/test-debugger.R cover the same
// paths at the R level, including the case where every breakpointed
// function is restored; that one is not observable here, because the
// client re-applies its own breakpoints to those functions as soon as the
// file has run.

const sandbox = useSuiteSandbox();

test.describe.serial('debugSource', { tag: ['@serial'] }, () => {
  let consoleActions: ConsolePaneActions;
  let debuggerActions: DebuggerActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    debuggerActions = new DebuggerActions(page, consoleActions);
  });

  test.afterEach(async () => {
    if (await debuggerActions.debuggerPage.debugToolbar.isVisible()) {
      await debuggerActions.stopDebug();
    }
    // keep.source is TRUE in an interactive session; put it back in case a
    // test that turned it off failed before restoring it.
    await consoleActions.executeInConsole('options(keep.source = TRUE)', { wait: true });
    await consoleActions.clearConsole();
  });

  test('hits breakpoints in a file sourced with keep.source off (#12375)', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('options(keep.source = FALSE)', { wait: true });

    // With keep.source off, parse() attaches no source references, which
    // used to make debugSource() fail outright ("attempt to set an attribute
    // on NULL") before running anything.
    await writeAndOpenFile(page, sandbox.dir, 'keepsrc_off.R', heredoc`
      keepsrc_f <- function() {
        x <- 1
        x + 1
      }
      keepsrc_f()
    ` + '\n');
    await debuggerActions.setBreakpoint(2);
    await executeCommand(page, 'sourceActiveDocument');

    await debuggerActions.waitForDebugMode();
    await debuggerActions.waitForActiveDebugLineRowToBe(1);
    await debuggerActions.continueDebug();
    await debuggerActions.waitForDebugExit();

    // The option itself is left as the user set it.
    expect(await consoleActions.evalRLogical('isFALSE(getOption("keep.source"))'), 'keep.source')
      .toBe(true);

    // The file is parsed with source references regardless of the option,
    // so the function it defined can take breakpoints: the one on line 2
    // is now active (the client traced keepsrc_f once the file had run),
    // and toggling it off hands back the function with its srcref.
    await expect(debuggerActions.debuggerPage.breakpoints).toHaveCount(1);
    await debuggerActions.clearBreakpoint(2);
    expect(await consoleActions.evalRLogical('inherits(attr(keepsrc_f, "srcref"), "srcref")'), 'srcref')
      .toBe(true);
  });

  test('runs an empty file and a comment-only file (#12375)', async ({ rstudioPage: page }) => {
    // Nothing to set a breakpoint on, so this is the direct call from the
    // console. Either file used to fail with "subscript out of bounds"
    // while wiring up source references that do not exist. debugSource()
    // returns normally either way: rs_debugSourceFile swallows the error
    // and reports it only through the debug_source_completed event, so
    // that event's 'succeeded' flag is the signal.
    for (const [fileName, content] of [
      ['debugsource_empty.R', ''],
      ['debugsource_comment.R', '# just a comment\n'],
    ]) {
      await seedSandboxFile(page, sandbox.dir, fileName, content);
      const filePath = path.join(sandbox.dir, fileName);

      let completed: DebugSourceCompletedEvent | undefined;
      const completion = page.waitForResponse(
        async response => {
          if (!new URL(response.url()).pathname.endsWith('/events/get_events'))
            return false;
          const body = await response.json().catch(() => null) as
            { result?: DebugSourceCompletedEvent[] } | null;
          const event = body?.result?.find(event =>
            event.type === 'debug_source_completed' &&
            event.data?.path?.replace(/\\/g, '/').split('/').pop() === fileName,
          );
          if (!event)
            return false;
          completed = event;
          return true;
        },
        { timeout: TIMEOUTS.fileOpen },
      );
      await Promise.all([
        completion,
        consoleActions.executeInConsole(`debugSource(${rPathLiteral(filePath)})`, { wait: true }),
      ]);
      expect(completed?.data?.succeeded, fileName).toBe(true);
    }
  });

  test('cleanup leaves the bindings the file ended with alone', async ({ rstudioPage: page }) => {
    // A breakpoint inside a definition the file later replaces, and one
    // inside a statement with a side effect. After the file has run, the
    // injected browser calls are stripped from what it left behind. The
    // old cleanup re-ran the defining statements instead, which put the
    // first cleanup_h back and bumped cleanup_count a second time.
    await writeAndOpenFile(page, sandbox.dir, 'debugsource_cleanup.R', heredoc`
      cleanup_h <- function() {
        1
      }
      cleanup_h <- function() {
        2
      }
      cleanup_count <- 0
      cleanup_made <- (function() {
        cleanup_count <<- cleanup_count + 1
        function() 42
      })()
    ` + '\n');
    await debuggerActions.setBreakpoint(2);
    await debuggerActions.setBreakpoint(9);
    await executeCommand(page, 'sourceActiveDocument');

    // Only the line 9 breakpoint fires: the file never calls cleanup_h.
    await debuggerActions.waitForDebugMode();
    await debuggerActions.waitForActiveDebugLineRowToBe(8);
    await debuggerActions.continueDebug();
    await debuggerActions.waitForDebugExit();

    expect(await consoleActions.evalRLogical(
      'identical(cleanup_h(), 2) && identical(cleanup_count, 1) && identical(cleanup_made(), 42)',
    )).toBe(true);
  });
});

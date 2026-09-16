import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { DebuggerActions } from '@actions/debugger.actions';
import type { AceEditorElement } from '@utils/ace';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile } from '@utils/files';
import { executeCommand } from '@utils/commands';
import { waitForConsoleIdle } from '@pages/console_pane.page';
import { TIMEOUTS } from '@utils/constants';
import { heredoc } from '@utils/heredoc';

const sandbox = useSuiteSandbox();

test.describe('Package function source (#18754)', () => {
  let consoleActions: ConsolePaneActions;
  let debuggerActions: DebuggerActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    debuggerActions = new DebuggerActions(page, consoleActions);
  });

  test.afterEach(async () => {
    if (await debuggerActions.debuggerPage.debugToolbar.isVisible())
      await debuggerActions.stopDebug();
  });

  for (const entry of ['direct', 'debugonce in wrapper', 'debugonce in console wrapper', 'step into wrapper'] as const) {
    test(`${entry} displays and steps through lm source`, async ({ rstudioPage: page }) => {
      if (entry !== 'direct') {
        const wrapper = heredoc`
          fit_lm <- function(data) {
            lm(mpg ~ wt, data = data)
          }
        ` + '\n';
        if (entry === 'debugonce in console wrapper') {
          await consoleActions.executeInConsole(
            `eval(parse(text = ${JSON.stringify(wrapper)}, keep.source = TRUE))`,
          );
        } else {
          await writeAndOpenFile(page, sandbox.dir, 'fit_lm.R', wrapper);
          await executeCommand(page, 'sourceActiveDocument');
          await waitForConsoleIdle(page);
        }
      }

      if (entry === 'step into wrapper') {
        await consoleActions.executeInConsole('debugonce(fit_lm); fit_lm(mtcars)');
        await debuggerActions.waitForDebugMode();
        await consoleActions.executeInConsole('n');
        await debuggerActions.waitForActiveDebugLineRowToBe(1);
        await consoleActions.executeInConsole('s');
      } else {
        const call = entry === 'direct' ? 'lm(mpg ~ wt, data = mtcars)' : 'fit_lm(mtcars)';
        await consoleActions.executeInConsole(`debugonce(lm); ${call}`);
      }

      await debuggerActions.waitForDebugMode();
      // The automation bridge's activeEditor() covers editable documents;
      // the generated function browser also uses Ace, but is read-only.
      const editor = page.locator("[class*='rstudio_source_panel'] .ace_editor:visible");
      const readCode = () => editor.evaluate(el => (el as AceEditorElement).env?.editor?.getValue() ?? '');
      await expect.poll(
        readCode,
        { timeout: TIMEOUTS.fileOpen },
      ).toContain('ret.x <- x');

      // Check actual statements, rather than fixed rows: deparse formatting
      // and the length of lm's function signature vary between R versions.
      const code = await readCode();
      expect(code).not.toContain('fit_lm <- function');
      for (const statement of ['ret.x <- x', 'ret.y <- y', 'cl <- match.call()']) {
        const row = code.split('\n').findIndex(line => line.trim() === statement);
        expect(row).toBeGreaterThanOrEqual(0);
        await consoleActions.executeInConsole('n');
        await debuggerActions.waitForActiveDebugLineRowToBe(row);
      }

      await consoleActions.executeInConsole('c');
      await debuggerActions.waitForDebugExit();
    });
  }

  test('evalq retains source locations in a local environment', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, 'eval_source.R', heredoc`
      run_eval <- function() {
        evalq({
          browser()
          local_x <- 1
          local_y <- 2
          local_x + local_y
        }, new.env())
      }
    ` + '\n');
    await executeCommand(page, 'sourceActiveDocument');
    await waitForConsoleIdle(page);
    await consoleActions.executeInConsole('run_eval()');
    await debuggerActions.waitForDebugMode();

    await debuggerActions.waitForActiveDebugLineRowToBe(2);
    for (const row of [3, 4]) {
      await consoleActions.executeInConsole('n');
      await debuggerActions.waitForActiveDebugLineRowToBe(row);
    }
    await consoleActions.executeInConsole('c');
    await debuggerActions.waitForDebugExit();
  });
});

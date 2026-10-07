/**
 * "Run All Chunks Above" / "Run All Chunks Below" / "Run All" with chunk
 * output sent to the console (not inline) must run Python chunks too,
 * switching the console between R and Python in document order (#17196).
 * Before the fix only R chunks were collected, and they were sent in the
 * language of the chunk under the cursor, so Python chunks were dropped
 * and R code could land in the Python REPL.
 *
 * The inline-output path goes through the notebook queue and was never
 * affected; it is covered by rmarkdown_chunks.test.ts and friends.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from 'playwright';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import { waitForConsoleIdle } from '@pages/console_pane.page';

// The console toolbar's interpreter label: "R 4.x.y" or "Python 3.x.y".
// The console pane is tabbed (Console / Terminal / ...), which is the
// variant that carries the _tabbed suffix; the untabbed label exists in
// the DOM too but is hidden.
function interpreterLabel(page: Page) {
  return page.locator('#rstudio_console_interpreter_version_tabbed');
}

async function exitPythonReplIfActive(page: Page, consoleActions: ConsolePaneActions): Promise<void> {
  const label = await interpreterLabel(page).innerText();
  if (!label.startsWith('Python'))
    return;
  await consoleActions.executeInConsole('exit');
  await expect(interpreterLabel(page)).toContainText(/^R /);
  await waitForConsoleIdle(page);
}

// The chunk fences are interpolated because heredoc reads template parts
// raw -- an escaped backtick would land in the file as a literal
// backslash-backtick.
const FENCE = '```';

test.describe('Run All Chunks with console chunk output', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let missingPackages: string[] = [];
  let file: string;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    missingPackages = await consoleActions.ensurePackages(['reticulate']);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    test.skip(missingPackages.length > 0, `Missing: ${missingPackages.join(', ')}`);
    // reticulate may be installed without a usable interpreter (e.g. Windows
    // CI); py_available() reports that rather than erroring.
    const pyAvailable = await consoleActions.evalRLogical('reticulate::py_available(initialize = TRUE)');
    test.skip(pyAvailable !== true, 'No Python interpreter available');

    await setPref(page, 'rmd_chunk_output_inline', false);
    await consoleActions.clearConsole();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // Leave the REPL so a failed test does not leak Python mode into the
    // next test in this worker, then restore the default output mode.
    await exitPythonReplIfActive(page, consoleActions);
    await clearPref(page, 'rmd_chunk_output_inline');
    if (file)
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [file]);
  });

  test('Run All Chunks Above: an R chunk then Python chunks, ending in the Python REPL', async ({ rstudioPage: page }) => {
    file = 'run_above_r_then_python.qmd';
    await writeAndOpenFile(page, sandbox.dir, file, heredoc`
      ---
      title: run all chunks above
      ---

      ${FENCE}{r setup}
      r_object <- 1
      ${FENCE}

      ${FENCE}{python}
      testing_object = 44
      ${FENCE}

      ${FENCE}{python}
      testing_object = testing_object + 11
      ${FENCE}

      ${FENCE}{r last}
      "cursor goes here"
      ${FENCE}
    `);
    await sourceActions.navigateToChunkByLabel('last');

    await executeCommand(page, 'executePreviousChunks');

    // The last chunk above the cursor is Python, so the console ends up in
    // the Python REPL with both Python chunks run and the R chunk run as R.
    await expect(interpreterLabel(page)).toContainText('Python', { timeout: 60000 });
    await expect(consoleActions.consolePane.consoleOutput).toContainText('testing_object + 11', { timeout: 30000 });
    await consoleActions.executeInConsole('print("probe", testing_object, r.r_object)');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('probe 55 1.0', { timeout: 30000 });
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText('Error');
  });

  test('Run All: a Python chunk ending in an open block, then an R chunk, ending back in R', async ({ rstudioPage: page }) => {
    // The Python chunk ends inside an indented block. The session keeps that
    // block open across the blank line that separates the chunks, so the
    // 'quit' that moves the console back to R reaches reticulate with code
    // still buffered; it must run that code and leave the REPL, not evaluate
    // 'quit' as a Python expression and keep the REPL (and the R chunk) in
    // Python.
    file = 'run_all_python_block_then_r.qmd';
    await writeAndOpenFile(page, sandbox.dir, file, heredoc`
      ---
      title: run all
      ---

      ${FENCE}{python}
      def f():
          return 1
      ${FENCE}

      ${FENCE}{r after}
      r_object <- reticulate::py$f() + 1
      ${FENCE}
    `);

    await executeCommand(page, 'executeAllCode');

    // The R chunk is echoed last, after the console has left the REPL.
    await expect(consoleActions.consolePane.consoleOutput).toContainText('r_object <- reticulate::py$f() + 1', { timeout: 60000 });
    await expect(interpreterLabel(page)).toContainText(/^R /, { timeout: 30000 });
    await waitForConsoleIdle(page);
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText('Error');
    // evalRLogical clears the console first, so the error check sits above it.
    expect(await consoleActions.evalRLogical('identical(r_object, 2)')).toBe(true);
  });

  test('Run All Chunks Below: Python, R, then Python again, ending in the Python REPL', async ({ rstudioPage: page }) => {
    file = 'run_below_python_r_python.qmd';
    await writeAndOpenFile(page, sandbox.dir, file, heredoc`
      ---
      title: run all chunks below
      ---

      ${FENCE}{r first}
      "cursor goes here"
      ${FENCE}

      ${FENCE}{python}
      total = 0
      for i in range(4):
          total += i
      ${FENCE}

      ${FENCE}{r middle}
      r_total <- reticulate::py$total
      ${FENCE}

      ${FENCE}{python}
      done = r.r_total + 1
      ${FENCE}
    `);
    await sourceActions.navigateToChunkByLabel('first');

    await executeCommand(page, 'executeSubsequentChunks');

    await expect(consoleActions.consolePane.consoleOutput).toContainText('done = r.r_total + 1', { timeout: 60000 });
    await expect(interpreterLabel(page)).toContainText('Python', { timeout: 30000 });
    await consoleActions.executeInConsole('print("probe", total, done)');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('probe 6 7', { timeout: 30000 });
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText('Error');
  });
});

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
import { getConsolePromptCount, waitForConsoleIdle } from '@pages/console_pane.page';

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

const CONSOLE_INPUT_RPC = /\/rpc\/console_input(?:\?|$)/;

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

  test('Run All Chunks Below: a final Python chunk ending in an open block is closed', async ({ rstudioPage: page }) => {
    // The REPL reads a line at a time and only closes an indented block on
    // a blank line. A chunk's text has no trailing newline, so a batch that
    // ends inside a block would leave the REPL at its '...' continuation
    // prompt with the block unevaluated.
    file = 'run_below_python_open_block.qmd';
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
    `);
    await sourceActions.navigateToChunkByLabel('first');

    await executeCommand(page, 'executeSubsequentChunks');

    await expect(interpreterLabel(page)).toContainText('Python', { timeout: 60000 });
    // The probe is echoed with the prompt it was submitted at: '>>> ' once
    // the block has been closed and run, '... ' if it is still open.
    await consoleActions.executeInConsole('print("probe", total)');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('>>> print("probe", total)');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('probe 6', { timeout: 30000 });
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText('Error');
  });

  test('a Python chunk run after the batch has left the REPL, with no prompt in between, reaches Python', async ({ rstudioPage: page }) => {
    // The session enqueues the switch to Python itself and then has to
    // follow the batch through the REPL and back out: when the console is
    // busy as the batch is sent, the switch and the batch both queue behind
    // that work and no prompt fires until everything has drained. A Python
    // chunk run once the batch is back in R must see that the console has
    // left the REPL, not the switch the session enqueued earlier.
    file = 'run_above_no_prompt.qmd';
    await writeAndOpenFile(page, sandbox.dir, file, heredoc`
      ---
      title: no prompt
      ---

      ${FENCE}{python first}
      import time
      time.sleep(2)
      stale_probe = 'python'
      ${FENCE}

      ${FENCE}{r slow}
      Sys.sleep(5)
      ${FENCE}

      ${FENCE}{r last}
      "cursor goes here"
      ${FENCE}

      ${FENCE}{python probe}
      print("probe:" + stale_probe)
      ${FENCE}
    `);

    // Keep R busy so the switch and the batch queue behind it, with no
    // prompt between them. The Python sleep keeps the REPL phase long
    // enough for the label to be seen in Python before it returns to R.
    await consoleActions.executeInConsole('Sys.sleep(3)', { wait: false });
    await sourceActions.navigateToChunkByLabel('last');
    await executeCommand(page, 'executePreviousChunks');

    // The interpreter label follows the REPL in and back out; once it is
    // back on R the slow chunk is running and the probe queues behind it.
    await expect(interpreterLabel(page)).toContainText('Python', { timeout: 60000 });
    await expect(interpreterLabel(page)).toContainText(/^R /, { timeout: 30000 });
    await sourceActions.navigateToChunkByLabel('probe');
    await executeCommand(page, 'executeCurrentChunk');

    await expect(consoleActions.consolePane.consoleOutput).toContainText('probe:python', { timeout: 60000 });
    await expect(interpreterLabel(page)).toContainText('Python');
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText('Error');
  });

  test('a Python chunk run while the batch is still draining reaches Python, despite an early prompt', async ({ rstudioPage: page }) => {
    // Run All Chunks Above from an R console with a Python-first batch: the
    // session enqueues reticulate::repl_python() itself, and if the REPL
    // prompts before the batch sent behind it has been buffered, the client
    // sees a Python prompt even though the batch leaves the console in R.
    // A Python chunk run while that batch drains queues behind it, so it
    // needs its own switch; the client must not take the early prompt as
    // proof it is already in Python. Holding the batch's RPC makes the early
    // prompt deterministic.
    //
    // A line typed into the console while the batch drains also queues
    // behind it (so it runs as R), and it reaches the client's tracker
    // without the language check an editor send goes through. It must not
    // make the tracker trust the early prompt's Python either.
    file = 'run_above_early_prompt.qmd';
    await writeAndOpenFile(page, sandbox.dir, file, heredoc`
      ---
      title: early prompt
      ---

      ${FENCE}{python first}
      stale_probe = 'python'
      ${FENCE}

      ${FENCE}{r slow}
      Sys.sleep(5)
      ${FENCE}

      ${FENCE}{r last}
      "cursor goes here"
      ${FENCE}

      ${FENCE}{python probe}
      print("probe:" + stale_probe + ":" + r.typed_probe)
      ${FENCE}
    `);

    let releaseBatch = () => {};
    const batchHeld = new Promise<void>((resolve) => (releaseBatch = resolve));
    await page.route(CONSOLE_INPUT_RPC, async (route) => {
      if (route.request().postData()?.includes("stale_probe = 'python'"))
        await batchHeld;
      await route.continue();
    });

    try {
      await sourceActions.navigateToChunkByLabel('last');
      const promptCountBefore = await getConsolePromptCount(page);
      expect(promptCountBefore).not.toBeNull();
      await executeCommand(page, 'executePreviousChunks');

      // With the batch held, the only prompt that can arrive is the REPL's.
      await expect.poll(() => getConsolePromptCount(page), { timeout: 60000 }).toBeGreaterThan(promptCountBefore!);
      await expect(interpreterLabel(page)).toContainText('Python');
      releaseBatch();

      // Once the R chunk is echoed the batch has left the REPL and R is busy
      // with it, so the typed line and the probe chunk queue behind it.
      await expect(consoleActions.consolePane.consoleOutput).toContainText('Sys.sleep(5)', { timeout: 30000 });
      await consoleActions.executeInConsole('typed_probe <- "r"', { wait: false });
      await sourceActions.navigateToChunkByLabel('probe');
      await executeCommand(page, 'executeCurrentChunk');

      await expect(consoleActions.consolePane.consoleOutput).toContainText('probe:python:r', { timeout: 60000 });
      await expect(interpreterLabel(page)).toContainText('Python');
      await expect(consoleActions.consolePane.consoleOutput).not.toContainText('Error');
    } finally {
      releaseBatch();
      await page.unroute(CONSOLE_INPUT_RPC);
    }
  });
});

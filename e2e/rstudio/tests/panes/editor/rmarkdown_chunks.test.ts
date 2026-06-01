/**
 * R Markdown chunk-execution behavior: chunk widgets visibility, the
 * `warn` option round-trip when running a chunk, error halts
 * (#16006-adjacent), console-history recall after an errored chunk
 * (#16006), patchwork-style auto-printing (#13470), paged-table
 * representation (#16483), suppression of invisibly-returned data.table
 * auto-printing (#17278), and nb.html generation on save.
 *
 * Multiline chunk execution (#17350) is covered by
 * `multiline_chunk_execution.test.ts`; notebook-save-during-execution
 * (#6260) is in `notebook_save_during_execution.test.ts`.
 */

import * as fs from 'fs';
import * as path from 'path';
import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from 'playwright';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, isCommandEnabled } from '@utils/commands';
import { waitForConsoleBusy, waitForConsoleIdle } from '@pages/console_pane.page';

/**
 * Run a labeled chunk and wait for `signal` to appear in the console
 * output. Chunks echo their R code and results to the console as
 * they run; pick a `signal` unique to the chunk's output (typically
 * the printed result like `[1] 2` or the prefix of the error message).
 *
 * This path never types anything into the console, so it's safe to use
 * in tests that walk Up-arrow history afterwards.
 */
async function runChunkByLabelAndWaitForConsoleSignal(
  page: Page,
  consoleActions: ConsolePaneActions,
  sourceActions: SourcePaneActions,
  label: string,
  signal: string | RegExp,
): Promise<void> {
  await sourceActions.navigateToChunkByLabel(label);
  await executeCommand(page, 'executeCurrentChunk');
  await expect(consoleActions.consolePane.consoleOutput).toContainText(signal, { timeout: 15000 });
}

/**
 * Run a labeled chunk that produces no visible output (e.g. invisible
 * assignments or `registerS3method` calls) and wait for it to finish.
 *
 * There's no console signal to wait for, so we wait on R's busy state instead
 * of injecting a synthetic marker. The previous approach cat()'d a marker via
 * the console immediately after dispatching the chunk -- but executeCurrentChunk
 * only dispatches (the chunk feeds the console asynchronously), so for a
 * multi-line chunk the marker landed mid-continuation, corrupting R's input
 * buffer ("Incomplete expression" from NotebookQueue) and silently skipping the
 * chunk body. Bracketing busy->idle has no such race and doesn't pollute
 * console history.
 */
async function runChunkByLabelAndWaitForIdle(
  page: Page,
  sourceActions: SourcePaneActions,
  label: string,
): Promise<void> {
  await sourceActions.navigateToChunkByLabel(label);
  await executeCommand(page, 'executeCurrentChunk');
  // executeCurrentChunk dispatches asynchronously, so R isn't busy on the next
  // tick. Wait for it to enter the busy state before waiting for idle, so we
  // don't catch a still-idle console and return before the chunk runs. Tolerate
  // a chunk fast enough that the busy class is never observed -- idle is then
  // already true.
  await waitForConsoleBusy(page).catch(() => {});
  await waitForConsoleIdle(page);
}

test.describe.serial('R Markdown chunks', { tag: ['@serial'] }, () => {
  // Sets cwd to a per-spec sandbox; relative paths used by
  // createAndOpenFile / closeSourceAndDeleteFile land there.
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    // Every test in this file drives chunks or saves a notebook, both of
    // which need rmarkdown loadable. Without this check, a missing dep
    // surfaces as an opaque chunk/save timeout deep inside a test; capture
    // the failure once here and let test.beforeEach skip with a clear reason.
    missingPackages = await consoleActions.ensurePackages(['rmarkdown'], 180_000);
    await consoleActions.clearConsole();
  });

  test.beforeEach(() => {
    test.skip(
      missingPackages.length > 0,
      `required R package(s) not available: ${missingPackages.join(', ')}`,
    );
  });

  test('the warn option is preserved when running chunks', async ({ rstudioPage: page }) => {
    const fileName = `rmarkdown_warn_${Date.now()}.Rmd`;
    // Sentinel printed as the last line of the chunk so the test can
    // wait for "chunk finished" before checking the global warn value
    // in a *separate* console execution. The cat is independent of the
    // chunk so warn=2 persisting across the chunk boundary is the
    // assertion being tested. executeCurrentChunk dispatches via a
    // different path than executeInConsole, so without an explicit wait
    // a following console command can land before the chunk completes.
    const sentinel = `__CHUNK_DONE_${Date.now()}__`;
    const content = [
      '---',
      'title: Chunk Warnings',
      '---',
      '',
      '```{r warning_chunk, warning=TRUE}',
      '# check current option',
      'getOption("warn")',
      '# setting a global option',
      'options(warn = 2)',
      `cat("${sentinel}\\n")`,
      '```',
    ].join('\n');

    // Reset warn to 0 and verify the baseline. `WARN_BEFORE=0` only
    // appears in the cat *output*, not the (unescaped) input echo --
    // the input has `WARN_BEFORE="` followed by a comma.
    await consoleActions.clearConsole();
    await consoleActions.executeInConsole('options(warn = 0); cat("WARN_BEFORE=", getOption("warn"), "\\n", sep = "")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('WARN_BEFORE=0');

    await sourceActions.createAndOpenFile(fileName, content);

    // Clear before running the chunk so the sentinel wait below cannot
    // match against the writeLines echo from createAndOpenFile (which
    // contains the sentinel string verbatim as part of the chunk body).
    // Use the consoleClear command rather than clearConsole() so we keep
    // source-pane focus -- navigateToChunkByLabel below clicks into the
    // source editor, which fails if focus has shifted to the console.
    await executeCommand(page, 'consoleClear');

    await sourceActions.navigateToChunkByLabel('warning_chunk');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(consoleActions.consolePane.consoleOutput).toContainText(sentinel, { timeout: 30000 });

    await consoleActions.executeInConsole('cat("WARN_AFTER=", getOption("warn"), "\\n", sep = "")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('WARN_AFTER=2', { timeout: 30000 });

    // Reset warn before closing
    await consoleActions.executeInConsole('options(warn = 0)');
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('the expected chunk widgets show for multiple chunks', async ({ rstudioPage: page }) => {
    const fileName = `rmarkdown_widgets_${Date.now()}.Rmd`;
    const content = [
      '---',
      'title: "Chunk widgets"',
      '---',
      '',
      '```{r setup, include=FALSE}',
      'knitr::opts_chunk$set(echo = TRUE)',
      '```',
      '',
      '## R Markdown',
      '',
      'This is an R Markdown document.',
      '',
      '```{r cars}',
      'summary(cars)',
      '```',
      '',
      '## Including Plots',
      '',
      'You can also embed plots, for example:',
      '',
      '```{r pressure, echo=FALSE}',
      'plot(pressure)',
      '```',
      '',
      'The end.',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);

    // All three widget kinds should render once per chunk.
    const optionWidgets = page.locator('.rstudio_modify_chunk');
    const previewWidgets = page.locator('.rstudio_preview_chunk');
    const runWidgets = page.locator('.rstudio_run_chunk');

    await expect(optionWidgets).toHaveCount(3);
    await expect(previewWidgets).toHaveCount(3);
    await expect(runWidgets).toHaveCount(3);

    // The setup chunk's preview widget is hidden (no point previewing a
    // setup chunk that's already run on document open).
    await expect(previewWidgets.nth(0)).toHaveAttribute('aria-hidden', 'true');
    await expect(previewWidgets.nth(0)).toHaveCSS('display', 'none');

    // All other widgets should be visible (aria-hidden absent or false,
    // display not "none").
    for (const locator of [previewWidgets.nth(1), previewWidgets.nth(2)]) {
      const ariaHidden = await locator.getAttribute('aria-hidden');
      expect(ariaHidden === null || ariaHidden === 'false').toBe(true);
      await expect(locator).not.toHaveCSS('display', 'none');
    }
    for (let i = 0; i < 3; i++) {
      const ariaHidden = await optionWidgets.nth(i).getAttribute('aria-hidden');
      expect(ariaHidden === null || ariaHidden === 'false').toBe(true);
      await expect(optionWidgets.nth(i)).not.toHaveCSS('display', 'none');
      const runAriaHidden = await runWidgets.nth(i).getAttribute('aria-hidden');
      expect(runAriaHidden === null || runAriaHidden === 'false').toBe(true);
      await expect(runWidgets.nth(i)).not.toHaveCSS('display', 'none');
    }

    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('errors in notebook chunks halt execution', async ({ rstudioPage: page }) => {
    const fileName = `rmarkdown_halt_${Date.now()}.Rmd`;
    const content = [
      '---',
      'title: Stop on Error',
      '---',
      '',
      '```{r halt}',
      'stop("An error occurred here.")',
      'stop("This line of code should not be executed.")',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);
    await consoleActions.clearConsole();
    await runChunkByLabelAndWaitForConsoleSignal(
      page, consoleActions, sourceActions, 'halt', 'Error: An error occurred here.',
    );
    // The first stop() halts the chunk, so the second message must NOT be
    // present anywhere in the console output.
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText(
      'This line of code should not be executed.',
    );

    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('command line history can be recalled after error (#16006)', async ({ rstudioPage: page }) => {
    const fileName = `rmarkdown_history_${Date.now()}.Rmd`;
    const content = [
      '---',
      'title: Stop on Error',
      '---',
      '',
      '```{r chunk_a}',
      '1 + 1',
      '```',
      '',
      '```{r chunk_b}',
      'stop("Ouch!")',
      '```',
      '',
      '```{r chunk_c}',
      '2 + 2',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);

    // Run each chunk. Use the console-signal wait (executeCommand bridge
    // + watch for the chunk's result in console output) so we never type
    // anything -- otherwise the typed marker / command would land in
    // console history and Up-arrow would walk back through those instead
    // of the chunk code.
    const chunks: Array<{ label: string; signal: string }> = [
      { label: 'chunk_a', signal: '[1] 2' },
      { label: 'chunk_b', signal: 'Error: Ouch!' },
      { label: 'chunk_c', signal: '[1] 4' },
    ];
    for (const { label, signal } of chunks) {
      await runChunkByLabelAndWaitForConsoleSignal(
        page, consoleActions, sourceActions, label, signal,
      );
    }

    // Focus the console without typing (so activateConsole itself doesn't
    // become the freshest history entry). Then walk the history.
    await executeCommand(page, 'activateConsole');
    const consoleInput = page.locator('#rstudio_console_input');

    await page.keyboard.press('ArrowUp');
    await expect(consoleInput).toContainText('2 + 2');

    await page.keyboard.press('ArrowUp');
    await expect(consoleInput).toContainText('stop("Ouch!")');

    await page.keyboard.press('ArrowUp');
    await expect(consoleInput).toContainText('1 + 1');

    await page.keyboard.press('Escape');
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test(`patchwork-style S3 method override does not dump output (#13470)`, async ({ rstudioPage: page }) => {
    const fileName = `rmarkdown_patchwork_${Date.now()}.Rmd`;
    const content = [
      '---',
      'title: S3 Method Overrides',
      '---',
      '',
      '```{r setup}',
      '# simulate the regression without requiring the patchwork package',
      'registerS3method("str", "patchwork", function(object, ...) {',
      '   print(mtcars)',
      '})',
      '```',
      '',
      '```{r buggy}',
      'x <- structure(list(), class = "patchwork")',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);

    // Setup chunk: registerS3method is invisible, so no chunk-output element
    // appears -- wait on R's busy state instead.
    await runChunkByLabelAndWaitForIdle(page, sourceActions, 'setup');

    // Buggy chunk: also invisible (assignment). Run twice -- the
    // first invocation might not surface a stale chunk-output buffer.
    await runChunkByLabelAndWaitForIdle(page, sourceActions, 'buggy');
    await runChunkByLabelAndWaitForIdle(page, sourceActions, 'buggy');

    // After the buggy chunk runs, there should be no chunk output element
    // for it (the S3 override would otherwise dump mtcars into the chunk).
    await expect(page.locator('#rstudio_chunk_output_buggy')).toHaveCount(0);

    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('paged-table representation only used for auto-printed objects (#16483)', async ({ rstudioPage: page }) => {
    // First file: implicit auto-print -- should render as a paged table
    const fileNameAuto = `rmarkdown_paged_auto_${Date.now()}.Rmd`;
    const autoContent = [
      '---',
      'title: Paged Table; Auto-Printing',
      '---',
      '',
      '```{r paged_auto}',
      'mtcars',
      '```',
    ].join('\n');
    await sourceActions.createAndOpenFile(fileNameAuto, autoContent);
    await sourceActions.navigateToChunkByLabel('paged_auto');
    await executeCommand(page, 'executeCurrentChunk');
    // The auto-print path renders mtcars as a paged table; waiting for
    // the locator is itself the signal that R produced output.
    await expect(page.locator('.pagedtable')).toBeVisible({ timeout: 15000 });
    await sourceActions.closeSourceAndDeleteFile(fileNameAuto);

    // Second file: explicit print() -- should NOT render as a paged table
    const fileNamePrint = `rmarkdown_paged_print_${Date.now()}.Rmd`;
    const printContent = [
      '---',
      'title: Paged Table; Explicit Printing',
      '---',
      '',
      '```{r paged_print}',
      'print(mtcars)',
      '```',
    ].join('\n');
    await sourceActions.createAndOpenFile(fileNamePrint, printContent);
    // `print(mtcars)` echoes the mtcars header (e.g. "Mazda RX4") to the
    // console -- wait for that to know R has finished.
    await runChunkByLabelAndWaitForConsoleSignal(
      page, consoleActions, sourceActions, 'paged_print', 'Mazda RX4',
    );
    await expect(page.locator('.pagedtable')).toHaveCount(0);
    await sourceActions.closeSourceAndDeleteFile(fileNamePrint);
  });

  // A data.table returned invisibly from `[.data.table` after a `:=` update
  // must not be auto-printed -- matching the console, knitr, and rmarkdown
  // behavior. The notebook print override regressed in 2026.01: on the
  // suppression signal it fell through to data.table's own print method,
  // re-triggering data.table's stateful shouldPrint() and dumping the table
  // into the chunk. https://github.com/rstudio/rstudio/issues/17278
  test('data.table returned invisibly from := is not auto-printed (#17278)', async ({ rstudioPage: page }) => {
    const missing = await consoleActions.ensurePackages(['data.table'], 180_000);
    test.skip(missing.length > 0, `required R package(s) not available: ${missing.join(', ')}`);

    // Distinctive value for the added column: if the table is (incorrectly)
    // printed, this string lands in the chunk output, which is what makes the
    // assertion below non-vacuous. It cannot leak in from the source editor
    // because the assertion is scoped to the chunk-output element.
    const sentinel = 'NBDTSENTINEL17278';
    const fileName = `rmarkdown_datatable_invisible_${Date.now()}.Rmd`;
    const content = [
      '---',
      'title: data.table Invisible Update',
      '---',
      '',
      '```{r dtinvisible}',
      `data.table::as.data.table(mtcars)[, foo := '${sentinel}']`,
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);

    // The `:=` update returns invisibly, so a correctly-behaving chunk
    // produces no output -- wait on R's idle state. Run twice; the first run
    // might not surface a stale chunk-output buffer (mirrors the #13470 test).
    await runChunkByLabelAndWaitForIdle(page, sourceActions, 'dtinvisible');
    await runChunkByLabelAndWaitForIdle(page, sourceActions, 'dtinvisible');

    // No chunk-output element for this chunk should contain the table. The
    // class is `rstudio_chunk_output_<label>` -- ClassIds.assignClassId adds it
    // as a CSS *class*, so it must be matched with `.`, not `#`. Filtering on
    // the sentinel keeps this robust whether or not an empty output frame
    // exists, and only matches when the table was actually printed.
    await expect(
      page.locator('.rstudio_chunk_output_dtinvisible', { hasText: sentinel }),
    ).toHaveCount(0);

    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('saving an R Notebook creates an nb.html file', async ({ rstudioPage: page }) => {
    const fileName = `rmarkdown_nbhtml_${Date.now()}.Rmd`;
    const nbHtmlPath = path.join(sandbox.dir, fileName.replace(/\.Rmd$/, '.nb.html'));
    const content = [
      '---',
      'title: "Notebook Test"',
      'output: html_notebook',
      '---',
      '',
      '```{r nb}',
      'print("hello from notebook")',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);
    await runChunkByLabelAndWaitForConsoleSignal(
      page, consoleActions, sourceActions, 'nb', 'hello from notebook',
    );

    // The saveSourceDoc command is disabled when the doc is clean
    // (createAndOpenFile leaves the editor matching disk, and running a chunk
    // doesn't dirty it). Add a trailing newline to dirty the doc, then poll
    // until saveSourceDoc is enabled before firing it via the bridge. Going
    // through Ctrl+S here is flaky -- focus may not be on the editor, and
    // the command's enabled flag is recomputed asynchronously after the
    // dirty-state change, so a same-tick keystroke can silently no-op.
    await sourceActions.goToEnd();
    await page.keyboard.press('Enter');
    await expect.poll(() => isCommandEnabled(page, 'saveSourceDoc'), { timeout: 5000 }).toBe(true);

    // Fire-and-forget the save: do NOT use saveDocument(), which waits for the
    // doc to be marked clean. An R Notebook never settles clean after save --
    // notebooks always commit chunk output as "uncommitted" (getCommitMode() ==
    // MODE_UNCOMMITTED), so when the post-save render's chunk output finishes,
    // TextEditingTargetNotebook.setDirtyState() re-marks the doc dirty. That's
    // by design (it lets users re-save to refresh the preview at any time), so
    // the dirty bit is not a valid post-condition here. The nb.html write below
    // is the real signal that the save landed.
    await executeCommand(page, 'saveSourceDoc');

    // RStudio writes nb.html on save; poll the filesystem for it.
    await expect.poll(() => fs.existsSync(nbHtmlPath), { timeout: 30000, intervals: [500] }).toBe(true);

    await sourceActions.closeSourceAndDeleteFile(fileName);
    fs.unlinkSync(nbHtmlPath);
  });
});

// Regression test for https://github.com/rstudio/rstudio/issues/19654
// When chunk output is replayed from the notebook cache, stderr text queued
// before an error with a traceback is rendered first, and the stream then
// switches from text to the error. That switch used to clear the console the
// stream kept writing to, erasing all of the text shown before the error.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { executeCommand } from '@utils/commands';
import { closeAndDeleteSandboxFiles, openFile, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { useSuiteSandbox } from '@utils/sandbox';

const FILE_NAME = 'chunk_output_replay.Rmd';
const OUTPUT = '.rstudio_chunk_output_before_error';
const ERROR_BOX = `${OUTPUT} .ace_console_error`;

function notebookSource(): string {
  // The heredoc reads template parts in raw form, so escaped backticks
  // (\`) would keep their backslashes; interpolate backticks instead.
  const fence = '`'.repeat(3);
  return heredoc`
    ---
    output: html_document
    ---

    ${fence}{r before_error}
    outer19654 <- function() inner19654()
    inner19654 <- function() { cat("note\n", file = stderr()); stop("boom") }
    print("before")
    outer19654()
    ${fence}
  `;
}

test.describe('Notebook chunk output replay', { tag: ['@parallel_safe'] }, () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE_NAME]);
    await consoleActions.executeInConsole(
      'suppressWarnings(rm(list = c("outer19654", "inner19654")))',
      { wait: true },
    );
  });

  test('keeps text shown before an error with a traceback (#19654)', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILE_NAME, notebookSource());
    await sourceActions.ensureSourceMode();

    // Reopening restores output only for chunks whose definitions the server
    // has; they arrive with the document sync that follows the new output.
    const chunkDefsSaved = page.waitForResponse(response =>
      response.url().includes('save_document') &&
      (response.request().postData() ?? '').includes('chunk_id'));

    await sourceActions.navigateToChunkByLabel('before_error');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(page.locator(ERROR_BOX)).toHaveCount(1, { timeout: 30000 });
    const saved = await chunkDefsSaved;
    expect(await saved.text()).not.toContain('"error"');

    // Live output already shows the text: the error arrives after it has been
    // rendered, so the stream never switches away from text with it queued.
    await expect(page.locator(OUTPUT)).toContainText('[1] "before"');
    await expect(page.locator(OUTPUT)).toContainText('note');

    const closed = page.waitForResponse(response => response.url().includes('close_document'));
    await executeCommand(page, 'closeSourceDoc');
    await closed;
    await openFile(page, `${sandbox.dir}/${FILE_NAME}`);
    await expect(page.locator(ERROR_BOX)).toHaveCount(1, { timeout: 30000 });

    // the replayed text sits in the stream's first block, above the error box
    const text = page.locator(`${OUTPUT} pre`).first();
    await expect(text).toContainText('[1] "before"');
    await expect(text).toContainText('note');
  });
});

// Regression tests for https://github.com/rstudio/rstudio/issues/19654
// When chunk output is replayed from the notebook cache, stderr text queued
// before an error with a traceback is rendered first, and the stream then
// switches from text to the error. That switch used to clear the console the
// stream kept writing to, erasing all of the text shown before the error.
//
// The error box now always ends the text block it follows, so the box also has
// to find its message only in that block: an earlier block can hold another
// error's message, and an earlier box carries the same class as a message.

import type { Locator, Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { executeCommand } from '@utils/commands';
import { closeAndDeleteSandboxFiles, openFile, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { useSuiteSandbox } from '@utils/sandbox';

const FILE_NAME = 'chunk_output_replay.Rmd';
const ERRORS_FILE_NAME = 'chunk_output_errors.Rmd';
const MESSAGES_FILE_NAME = 'chunk_output_messages.Rmd';
const OUTPUT = '.rstudio_chunk_output_before_error';
const ERROR_BOX = `${OUTPUT} .ace_console_error`;
const TWO_ERRORS = '.rstudio_chunk_output_two_errors';
const ONE_FRAME_FIRST = '.rstudio_chunk_output_one_frame_first';
const MESSAGE_FORMS = '.rstudio_chunk_output_message_forms';

// The heredoc reads template parts in raw form, so escaped backticks (\`)
// would keep their backslashes; interpolate backticks instead.
const FENCE = '`'.repeat(3);

function notebookSource(): string {
  return heredoc`
    ---
    output: html_document
    ---

    ${FENCE}{r before_error}
    outer19654 <- function() inner19654()
    inner19654 <- function() { cat("note\n", file = stderr()); stop("boom") }
    print("before")
    outer19654()
    ${FENCE}
  `;
}

function errorsSource(): string {
  // The sleeps let each error box arrive before the output that follows it.
  return heredoc`
    ---
    output: html_document
    ---

    ${FENCE}{r two_errors, error = TRUE}
    outerA19654 <- function() innerA19654()
    innerA19654 <- function() stop("boomA")
    outerB19654 <- function() innerB19654()
    innerB19654 <- function() stop("boomB")
    print("a")
    outerA19654()
    Sys.sleep(1)
    print("b")
    outerB19654()
    ${FENCE}

    ${FENCE}{r one_frame_first, error = TRUE}
    outerC19654 <- function() innerC19654()
    innerC19654 <- function() stop("multi frame")
    stop("one frame")
    outerC19654()
    ${FENCE}
  `;
}

function messagesSource(): string {
  // Messages longer than the console's line length limit (2000 by default),
  // both on a line of their own (R wraps a long message after its call) and
  // on the line that starts with the highlighted "Error", and a message with
  // color escapes. R cuts error messages at warning.length (1000 by default),
  // so the chunk raises it. The \033 escapes are written for R to read.
  return heredoc`
    ---
    output: html_document
    ---

    ${FENCE}{r message_forms, error = TRUE}
    options19654 <- options(warning.length = 8170)
    outerD19654 <- function() innerD19654()
    innerD19654 <- function() stop(paste0("ownline19654", strrep("x", 3000)))
    outerE19654 <- function() innerE19654()
    innerE19654 <- function() stop(paste0("errorline19654", strrep("y", 3000)), call. = FALSE)
    outerF19654 <- function() innerF19654()
    innerF19654 <- function() stop("\033[31mcolored19654\033[39m")
    outerD19654()
    Sys.sleep(1)
    outerE19654()
    Sys.sleep(1)
    outerF19654()
    options(options19654)
    ${FENCE}
  `;
}

// Whether `first` comes before `second` in the document.
async function precedes(first: Locator, second: Locator): Promise<boolean> {
  const secondEl = await second.elementHandle();
  return await first.evaluate(
    (a, b) => !!b && !!(a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING),
    secondEl,
  );
}

// Runs a chunk and waits for the document sync that carries its chunk
// definitions: reopening restores output only for chunks the server has.
async function runChunk(page: Page, sourceActions: SourcePaneActions, label: string, boxes: number): Promise<void> {
  const chunkDefsSaved = page.waitForResponse(response =>
    response.url().includes('save_document') &&
    (response.request().postData() ?? '').includes('chunk_id'));

  await sourceActions.navigateToChunkByLabel(label);
  await executeCommand(page, 'executeCurrentChunk');
  await expect(page.locator(`.rstudio_chunk_output_${label} .ace_console_error`)).toHaveCount(boxes, { timeout: 30000 });
  await chunkDefsSaved;
}

async function reopen(page: Page, path: string): Promise<void> {
  const closed = page.waitForResponse(response => response.url().includes('close_document'));
  await executeCommand(page, 'closeSourceDoc');
  await closed;
  await openFile(page, path);
}

async function expectSeparateErrorBoxes(page: Page): Promise<void> {
  const boxes = page.locator(`${TWO_ERRORS} .ace_console_error`);
  await expect(boxes).toHaveCount(2, { timeout: 30000 });
  await expect(page.locator(`${TWO_ERRORS} .ace_console_error .ace_console_error`)).toHaveCount(0);
  await expect(boxes.nth(0)).toContainText('boomA');
  await expect(boxes.nth(1)).toContainText('boomB');
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
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE_NAME, ERRORS_FILE_NAME, MESSAGES_FILE_NAME]);
    await consoleActions.executeInConsole(
      'suppressWarnings(rm(list = ls(pattern = "^(outer|inner|options).*19654$")))',
      { wait: true },
    );
  });

  test('keeps text shown before an error with a traceback (#19654)', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILE_NAME, notebookSource());
    await sourceActions.ensureSourceMode();

    await runChunk(page, sourceActions, 'before_error', 1);

    // Live output already shows the text: the error arrives after it has been
    // rendered, so the stream never switches away from text with it queued.
    await expect(page.locator(OUTPUT)).toContainText('[1] "before"');
    await expect(page.locator(OUTPUT)).toContainText('note');

    await reopen(page, `${sandbox.dir}/${FILE_NAME}`);
    await expect(page.locator(ERROR_BOX)).toHaveCount(1, { timeout: 30000 });
    await expect(page.locator(ERROR_BOX)).toContainText('boom');

    // the replayed text sits in the stream's first block, above the error box
    const text = page.locator(`${OUTPUT} pre`).first();
    await expect(text).toContainText('[1] "before"');
    await expect(text).toContainText('note');
    expect(await precedes(text, page.locator(ERROR_BOX))).toBe(true);
  });

  test('gives each error box its own message, below the text before it', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, ERRORS_FILE_NAME, errorsSource());
    await sourceActions.ensureSourceMode();

    // Text printed after an error box arrived goes below that box.
    await runChunk(page, sourceActions, 'two_errors', 2);
    await expectSeparateErrorBoxes(page);
    const afterFirst = page.locator(`${TWO_ERRORS} pre`, { hasText: '[1] "b"' });
    expect(await precedes(page.locator(`${TWO_ERRORS} .ace_console_error`).first(), afterFirst)).toBe(true);

    // An error without a traceback stays text; the box for the next error
    // takes that error's message, not the earlier one. With nothing between
    // them, the console puts both messages in one group.
    await runChunk(page, sourceActions, 'one_frame_first', 1);
    const box = page.locator(`${ONE_FRAME_FIRST} .ace_console_error`);
    await expect(box).toContainText('multi frame');
    await expect(box).not.toContainText('one frame');
    await expect(page.locator(`${ONE_FRAME_FIRST} pre`).first()).toContainText('one frame');

    // On replay, the error boxes come after all of the text, and the second
    // box used to take the first box as its message.
    await reopen(page, `${sandbox.dir}/${ERRORS_FILE_NAME}`);
    await expectSeparateErrorBoxes(page);
  });

  test('moves a long or colored error message into its box', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, MESSAGES_FILE_NAME, messagesSource());
    await sourceActions.ensureSourceMode();

    // Live, each message is shown as text before its error arrives; the box
    // takes that text, rather than leaving it above and showing its own copy.
    await runChunk(page, sourceActions, 'message_forms', 3);
    const boxes = page.locator(`${MESSAGE_FORMS} .ace_console_error`);
    const markers = ['ownline19654', 'errorline19654', 'colored19654'];
    for (const [i, marker] of markers.entries()) {
      await expect.soft(boxes.nth(i)).toContainText(marker);
      await expect.soft(page.locator(`${MESSAGE_FORMS} pre`, { hasText: marker })).toHaveCount(0);
    }

    // the long messages are cut as the console cuts them
    await expect.soft(boxes.nth(0)).toContainText('<truncated>');
    await expect.soft(boxes.nth(1)).toContainText('<truncated>');
  });
});

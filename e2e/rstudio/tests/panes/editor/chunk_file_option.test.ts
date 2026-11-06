// Interactive execution of chunks whose code comes from knitr's `file` chunk
// option. https://github.com/rstudio/rstudio/issues/19454

import * as path from 'path';
import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand } from '@utils/commands';
import { closeAndDeleteSandboxFiles, seedSandboxFile, writeAndOpenFile } from '@utils/files';
import { waitForConsoleIdle } from '@pages/console_pane.page';

// The console toolbar's interpreter label: "R 4.x.y" or "Python 3.x.y".
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

function qmd(title: string, chunks: string[]): string {
  return ['---', `title: ${title}`, '---', '', ...chunks, ''].join('\n');
}

test.describe('Chunk file option', { tag: ['@parallel_safe'] }, () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [
      'chunk_file_crlf.qmd',
      'chunk_file_crlf.R',
      'docdir/chunk_file_reldir.qmd',
      'docdir/chunk_file_reldir.R',
      'docdir/chunk_file_empty.qmd',
      'docdir/chunk_file_empty.R',
      'chunk_file_engine.qmd',
      'chunk_file_engine.sh',
      'chunk_file_python.qmd',
    ]);
  });

  test('runs code from a file with CRLF line endings (#19454)', async ({ rstudioPage: page }) => {
    // R's parser rejects '\r', so these lines only run if the IDE normalizes
    // line endings when reading the file.
    await seedSandboxFile(page, sandbox.dir, 'chunk_file_crlf.R', 'x <- 41\r\nprint(x + 1)\r\n');
    await writeAndOpenFile(page, sandbox.dir, 'chunk_file_crlf.qmd', qmd('CRLF', [
      '```{r crlf}',
      '#| file: chunk_file_crlf.R',
      '```',
    ]));

    await sourceActions.navigateToChunkByLabel('crlf');
    await executeCommand(page, 'executeCurrentChunk');

    const output = page.locator('.rstudio_chunk_output_crlf');
    await expect(output).toContainText('[1] 42', { timeout: 20000 });
    await expect(output).not.toContainText('invalid token');
  });

  test('resolves relative paths against the document directory', async ({ rstudioPage: page }) => {
    // R's working directory is the sandbox root, not the document's folder,
    // so a lookup against the working directory finds neither file.
    await seedSandboxFile(page, sandbox.dir, 'docdir/chunk_file_reldir.R', 'print("found")\n');
    await writeAndOpenFile(page, sandbox.dir, 'docdir/chunk_file_reldir.qmd', qmd('Relative', [
      '```{r reldir}',
      '#| file: chunk_file_reldir.R',
      '```',
      '',
      '```{r missing}',
      '#| file: chunk_file_missing.R',
      '```',
    ]));

    await sourceActions.navigateToChunkByLabel('reldir');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(page.locator('.rstudio_chunk_output_reldir')).toContainText('[1] "found"', { timeout: 20000 });

    // A file that can't be read is reported in the chunk, not silently skipped.
    await sourceActions.navigateToChunkByLabel('missing');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(page.locator('.rstudio_chunk_output_missing')).toContainText(
      /Error: cannot open file '.*docdir.chunk_file_missing\.R'/,
      { timeout: 20000 },
    );
  });

  test('an empty file leaves the console working directory alone', async ({ rstudioPage: page }) => {
    // Running a chunk switches R into the document's directory until the
    // chunk finishes; an empty file must not leave it switched.
    const emptyFile = await seedSandboxFile(page, sandbox.dir, 'docdir/chunk_file_empty.R', '');
    await writeAndOpenFile(page, sandbox.dir, 'docdir/chunk_file_empty.qmd', qmd('Empty', [
      '```{r empty}',
      `#| file: ${emptyFile.replace(/\\/g, '/')}`,
      '```',
    ]));

    // wait for the server to start (or skip) the chunk before using the console
    const chunkHandled = page.waitForResponse(async (response) =>
      response.url().endsWith('/events/get_events') &&
      (await response.text().catch(() => '')).includes('"chunk_exec_state_changed"'));
    await sourceActions.navigateToChunkByLabel('empty');
    await executeCommand(page, 'executeCurrentChunk');
    await chunkHandled;

    await consoleActions.clearConsole();
    await consoleActions.executeInConsole('cat("[wd:", basename(getwd()), "]", sep = "")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      `[wd:${path.basename(sandbox.dir)}]`,
      { timeout: 20000 },
    );
  });

  test('passes the whole file to an alternate engine', async ({ rstudioPage: page }) => {
    test.skip(process.platform === 'win32', 'bash is not reliably available on Windows');

    await seedSandboxFile(page, sandbox.dir, 'chunk_file_engine.sh', 'echo first-line\necho middle-line\necho last-line\n');
    await writeAndOpenFile(page, sandbox.dir, 'chunk_file_engine.qmd', qmd('Engine', [
      '```{bash shfile}',
      '#| file: chunk_file_engine.sh',
      '```',
    ]));

    await sourceActions.navigateToChunkByLabel('shfile');
    await executeCommand(page, 'executeCurrentChunk');

    const output = page.locator('.rstudio_chunk_output_shfile');
    await expect(output).toContainText('middle-line', { timeout: 20000 });
    await expect(output).toContainText('first-line');
    await expect(output).toContainText('last-line');
  });

  test('reports a missing file in a Python chunk', async ({ rstudioPage: page }) => {
    const missingPackages = await consoleActions.ensurePackages(['reticulate']);
    test.skip(missingPackages.length > 0, `Missing: ${missingPackages.join(', ')}`);
    const pyAvailable = await consoleActions.evalRLogical('reticulate::py_available(initialize = TRUE)');
    test.skip(pyAvailable !== true, 'No Python interpreter available');

    await writeAndOpenFile(page, sandbox.dir, 'chunk_file_python.qmd', qmd('Python', [
      '```{python pymissing}',
      '#| file: chunk_file_missing.py',
      '```',
    ]));

    try {
      await sourceActions.navigateToChunkByLabel('pymissing');
      await executeCommand(page, 'executeCurrentChunk');
      await expect(page.locator('.rstudio_chunk_output_pymissing')).toContainText(
        /FileNotFoundError: cannot open file '.*chunk_file_missing\.py'/,
        { timeout: 30000 },
      );
    } finally {
      await exitPythonReplIfActive(page, consoleActions);
    }
  });
});

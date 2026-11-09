// Interactive execution of chunks whose code comes from knitr's `file` chunk
// option. https://github.com/rstudio/rstudio/issues/19454

import * as path from 'path';
import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { closeAndDeleteSandboxFiles, openFile, seedSandboxFile, writeAndOpenFile } from '@utils/files';
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
      'chunk_file_engine_missing.qmd',
      'chunk_file_python.qmd',
      'rootdir/chunk_file_rootdir.qmd',
      'rootdir/chunk_file_setup.R',
      'rootdir/sub/chunk_file_rootdir.R',
      'chunk_file_home.qmd',
      'prefdir/chunk_file_pref.qmd',
      'chunk_file_cwd.R',
      'chunk_file_vector.qmd',
      'chunk_file_a.R',
      'chunk_file_b.R',
      'chunk_file_partial.qmd',
      'chunk_file_partial.R',
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

  test('reports a missing file in an alternate engine chunk', async ({ rstudioPage: page }) => {
    test.skip(process.platform === 'win32', 'bash is not reliably available on Windows');

    await writeAndOpenFile(page, sandbox.dir, 'chunk_file_engine_missing.qmd', qmd('Engine missing', [
      '```{bash shmissing}',
      '#| file: chunk_file_missing.sh',
      '```',
    ]));

    await sourceActions.navigateToChunkByLabel('shmissing');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(page.locator('.rstudio_chunk_output_shmissing')).toContainText(
      /cannot open file '.*chunk_file_missing\.sh'/,
      { timeout: 20000 },
    );
  });

  test('resolves paths against root.dir, except in the setup chunk', async ({ rstudioPage: page }) => {
    // The setup chunk sets root.dir; a later chunk's file resolves against
    // it. The setup chunk's own file keeps resolving against the document,
    // even once root.dir is set, so that it can be re-run.
    await seedSandboxFile(page, sandbox.dir, 'rootdir/chunk_file_setup.R', [
      'knitr::opts_knit$set(root.dir = "sub")',
      '.e2e_setup_runs <- if (exists(".e2e_setup_runs")) .e2e_setup_runs + 1 else 1',
      'print(paste0("setup-run-", .e2e_setup_runs))',
      '',
    ].join('\n'));
    await seedSandboxFile(page, sandbox.dir, 'rootdir/sub/chunk_file_rootdir.R', 'print("in-sub")\n');
    await writeAndOpenFile(page, sandbox.dir, 'rootdir/chunk_file_rootdir.qmd', qmd('Root dir', [
      '```{r setup}',
      '#| file: chunk_file_setup.R',
      '```',
      '',
      '```{r rootdir}',
      '#| file: chunk_file_rootdir.R',
      '```',
    ]));

    try {
      await sourceActions.navigateToChunkByLabel('setup');
      await executeCommand(page, 'executeCurrentChunk');
      await expect(page.locator('.rstudio_chunk_output_setup')).toContainText('setup-run-1', { timeout: 20000 });

      await sourceActions.navigateToChunkByLabel('rootdir');
      await executeCommand(page, 'executeCurrentChunk');
      await expect(page.locator('.rstudio_chunk_output_rootdir')).toContainText('[1] "in-sub"', { timeout: 20000 });

      await sourceActions.navigateToChunkByLabel('setup');
      await executeCommand(page, 'executeCurrentChunk');
      await expect(page.locator('.rstudio_chunk_output_setup')).toContainText('setup-run-2', { timeout: 20000 });
    } finally {
      await consoleActions.executeInConsole(
        'knitr::opts_knit$set(root.dir = NULL); rm(list = ".e2e_setup_runs", envir = globalenv())',
      );
    }
  });

  test('resolves ~ paths against the home directory', async ({ rstudioPage: page }) => {
    // The sandbox is not under the home directory, so the file is written
    // there through R, which also defines what ~ means on this platform.
    const homeDir = `rstudio-e2e-chunk-file-${path.basename(sandbox.dir)}`;
    const rHomeDir = `file.path(path.expand("~"), "${homeDir}")`;
    await consoleActions.executeInConsole(
      `dir.create(${rHomeDir}); writeLines('print("from-home")', file.path(${rHomeDir}, "chunk_file_home.R"))`,
    );
    try {
      await writeAndOpenFile(page, sandbox.dir, 'chunk_file_home.qmd', qmd('Home', [
        '```{r home}',
        `#| file: ~/${homeDir}/chunk_file_home.R`,
        '```',
      ]));

      await sourceActions.navigateToChunkByLabel('home');
      await executeCommand(page, 'executeCurrentChunk');
      await expect(page.locator('.rstudio_chunk_output_home')).toContainText('[1] "from-home"', { timeout: 20000 });
    } finally {
      await consoleActions.executeInConsole(`unlink(${rHomeDir}, recursive = TRUE)`);
    }
  });

  test('resolves relative paths against the knit directory preference', async ({ rstudioPage: page }) => {
    // With the preference set to the current directory, the file is looked
    // up in R's working directory (the sandbox root), not the document's.
    await seedSandboxFile(page, sandbox.dir, 'chunk_file_cwd.R', 'print("from-cwd")\n');
    await setPref(page, 'knit_working_dir', 'current');
    try {
      const docPath = await seedSandboxFile(page, sandbox.dir, 'prefdir/chunk_file_pref.qmd', qmd('Pref', [
        '```{r prefdir}',
        '#| file: chunk_file_cwd.R',
        '```',
      ]));
      await openFile(page, docPath);

      await sourceActions.navigateToChunkByLabel('prefdir');
      await executeCommand(page, 'executeCurrentChunk');
      await expect(page.locator('.rstudio_chunk_output_prefdir')).toContainText('[1] "from-cwd"', { timeout: 20000 });
    } finally {
      await clearPref(page, 'knit_working_dir');
    }
  });

  test('concatenates a vector of files', async ({ rstudioPage: page }) => {
    await seedSandboxFile(page, sandbox.dir, 'chunk_file_a.R', 'x <- 20\n');
    await seedSandboxFile(page, sandbox.dir, 'chunk_file_b.R', 'print(x + 1)\n');
    await writeAndOpenFile(page, sandbox.dir, 'chunk_file_vector.qmd', qmd('Vector', [
      '```{r vector}',
      '#| file: [chunk_file_a.R, chunk_file_b.R]',
      '```',
    ]));

    await sourceActions.navigateToChunkByLabel('vector');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(page.locator('.rstudio_chunk_output_vector')).toContainText('[1] 21', { timeout: 20000 });
  });

  test('runs only the selection when not running the whole chunk', async ({ rstudioPage: page }) => {
    // Ctrl+Enter on a line of the chunk runs that line, as for read_chunk()
    // code, rather than the whole file.
    await seedSandboxFile(page, sandbox.dir, 'chunk_file_partial.R', 'print("whole-file")\n');
    const bodyLine = 'print("body-line")';
    const contents = qmd('Partial', [
      '```{r partial}',
      '#| file: chunk_file_partial.R',
      bodyLine,
      '```',
    ]);
    await writeAndOpenFile(page, sandbox.dir, 'chunk_file_partial.qmd', contents);

    const lineNumber = contents.split('\n').indexOf(bodyLine) + 1;
    await sourceActions.selectInEditor(bodyLine, lineNumber, 0, bodyLine.length);
    await executeCommand(page, 'executeCode');

    const output = page.locator('.rstudio_chunk_output_partial');
    await expect(output).toContainText('[1] "body-line"', { timeout: 20000 });
    await expect(output).not.toContainText('whole-file');
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

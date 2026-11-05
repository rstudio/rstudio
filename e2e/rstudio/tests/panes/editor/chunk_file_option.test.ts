// Interactive execution of chunks whose code comes from knitr's `file` chunk
// option. https://github.com/rstudio/rstudio/issues/19454

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand } from '@utils/commands';
import { closeAndDeleteSandboxFiles, seedSandboxFile, writeAndOpenFile } from '@utils/files';

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
    ]);
  });

  test('runs code from a file with CRLF line endings (#19454)', async ({ rstudioPage: page }) => {
    // R's parser rejects '\r', so these lines only run if the IDE normalizes
    // line endings when reading the file.
    await seedSandboxFile(page, sandbox.dir, 'chunk_file_crlf.R', 'x <- 41\r\nprint(x + 1)\r\n');
    await writeAndOpenFile(page, sandbox.dir, 'chunk_file_crlf.qmd', [
      '---',
      'title: CRLF',
      '---',
      '',
      '```{r crlf}',
      '#| file: chunk_file_crlf.R',
      '```',
      '',
    ].join('\n'));

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
    await writeAndOpenFile(page, sandbox.dir, 'docdir/chunk_file_reldir.qmd', [
      '---',
      'title: Relative',
      '---',
      '',
      '```{r reldir}',
      '#| file: chunk_file_reldir.R',
      '```',
      '',
      '```{r missing}',
      '#| file: chunk_file_missing.R',
      '```',
      '',
    ].join('\n'));

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
});

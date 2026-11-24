import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { executeCommand } from '@utils/commands';
import { executeInConsole, getConsolePromptText } from '@pages/console_pane.page';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { JULIA_PROMPT, JULIA_START_TIMEOUT, leaveJulia, R_PROMPT, useJulia } from '@utils/julia';
import { useSuiteSandbox } from '@utils/sandbox';

// Inline {julia} chunks run in the console's Julia (through JuliaConnectoR),
// sharing its state, as {python} chunks do with reticulate. Skipped where
// Julia isn't installed.

const FENCE = '```';

const RMD = heredoc`
  ---
  title: probe
  ---

  ${FENCE}{julia jlone}
  x_nb = 41
  println("hello from Julia")
  x_nb + 1
  ${FENCE}

  ${FENCE}{julia jltwo}
  x_nb * 2
  ${FENCE}

  ${FENCE}{r rone}
  cat("R says", 1 + 1, "\n")
  ${FENCE}

  ${FENCE}{julia jlerr}
  error("boom")
  ${FENCE}

  ${FENCE}{julia jlfile, file = "missing.jl"}
  ${FENCE}
`;

const RMD_ALL = heredoc`
  ---
  title: all
  ---

  ${FENCE}{julia jla}
  y_nb = 7
  y_nb * 3
  ${FENCE}

  ${FENCE}{r rb}
  cat("R again", 3 * 3, "\n")
  ${FENCE}

  ${FENCE}{julia jlc}
  y_nb + 1
  ${FENCE}

  ${FENCE}{julia jld}
  error("stop here")
  ${FENCE}
`;

function chunkOutput(page: Page, label: string) {
  return page.locator(`.rstudio_chunk_output_${label}`);
}

test.describe.serial('Julia notebook chunks', () => {
  const sandbox = useSuiteSandbox();
  let sourceActions: SourcePaneActions;
  useJulia();

  test.beforeAll(async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await leaveJulia(page);
    await closeAndDeleteSandboxFiles(page, sandbox.dir, ['jlnb.Rmd', 'jlall.Rmd']);
  });

  test('inline julia chunks run in the console Julia', async ({ rstudioPage: page }) => {
    test.setTimeout(JULIA_START_TIMEOUT + 240_000);
    await writeAndOpenFile(page, sandbox.dir, 'jlnb.Rmd', RMD);

    await sourceActions.navigateToChunkByLabel('jlone');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(chunkOutput(page, 'jlone')).toContainText('hello from Julia', { timeout: JULIA_START_TIMEOUT });
    await expect(chunkOutput(page, 'jlone')).toContainText('42');
    await expect(chunkOutput(page, 'jlone')).not.toContainText('JuliaConnectoR');

    // state carries over to the next chunk
    await sourceActions.navigateToChunkByLabel('jltwo');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(chunkOutput(page, 'jltwo')).toContainText('82', { timeout: 60_000 });

    // and back to R
    await sourceActions.navigateToChunkByLabel('rone');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(chunkOutput(page, 'rone')).toContainText('R says 2', { timeout: 60_000 });
    await expect.poll(() => getConsolePromptText(page), { timeout: 30_000 }).toBe(R_PROMPT);

    // a Julia error shows in its chunk
    await sourceActions.navigateToChunkByLabel('jlerr');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(chunkOutput(page, 'jlerr')).toContainText('boom', { timeout: 60_000 });

    // a file that can't be read is reported as a Julia error
    await sourceActions.navigateToChunkByLabel('jlfile');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(chunkOutput(page, 'jlfile')).toContainText(/cannot open file '.*missing\.jl'/, { timeout: 60_000 });

    // the console's Julia has the chunks' state
    await expect.poll(() => getConsolePromptText(page), { timeout: 30_000 }).toBe(JULIA_PROMPT);
    await executeInConsole(page, 'println("console sees ", x_nb)');
    await expect(page.locator('#rstudio_console_output')).toContainText('console sees 41', { timeout: 30_000 });
  });

  test('Run All runs R and julia chunks inline', async ({ rstudioPage: page }) => {
    test.setTimeout(300_000);

    // the harness opens files with R code at the console
    await leaveJulia(page);

    await writeAndOpenFile(page, sandbox.dir, 'jlall.Rmd', RMD_ALL);
    await executeCommand(page, 'executeAllCode');
    await expect(chunkOutput(page, 'jla')).toContainText('21', { timeout: 120_000 });
    await expect(chunkOutput(page, 'rb')).toContainText('R again 9', { timeout: 60_000 });
    await expect(chunkOutput(page, 'jlc')).toContainText('8', { timeout: 60_000 });
    await expect(chunkOutput(page, 'jld')).toContainText('stop here', { timeout: 60_000 });
  });
});

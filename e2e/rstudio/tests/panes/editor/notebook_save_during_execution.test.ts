// Regression test for https://github.com/rstudio/rstudio/issues/6260
// Saving an R Notebook while a chunk is executing (especially one producing a plot)
// can corrupt the notebook cache, causing a gzfile "cannot open compressed file" error
// and making subsequent chunks unrunnable.

import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';

test.describe('Notebook save during execution', { tag: ['@parallel_safe'] }, () => {
  // Sets cwd to a per-spec sandbox; relative paths used by createAndOpenFile
  // and closeSourceAndDeleteFile land there.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);

    await consoleActions.closeAllBuffersWithoutSaving();
    await consoleActions.clearConsole();

    missingPackages = await consoleActions.ensurePackages(['ggplot2', 'nycflights13'], 120000);
  });

  test('save during chunk execution does not break notebook (#6260)', async ({ rstudioPage: page }) => {
    // https://github.com/rstudio/rstudio/issues/6260
    test.skip(missingPackages.length > 0, `Missing packages: ${missingPackages.join(', ')}`);

    const fileName = `notebook_save_exec_${Date.now()}.Rmd`;

    // Create an Rmd with two chunks:
    // Chunk 1: slow ggplot that takes long enough to save mid-execution
    // Chunk 2: simple output to verify chunks are still runnable after save
    const rmdContent = [
      '---',
      'title: Save During Execution Test',
      'output: html_notebook',
      '---',
      '',
      '```{r slow_plot}',
      'library(ggplot2)',
      'library(nycflights13)',
      'ggplot(flights, aes(x = arr_time, y = dep_delay)) + geom_point()',
      '```',
      '',
      '```{r verify_runnable}',
      "print('The quality of mercy is not strained')",
      "print('Chunks still work.')",
      '```',
    ].join('\\n');

    await sourceActions.createAndOpenFile(fileName, rmdContent);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });

    // Save the file first so it's on disk
    await consoleActions.typeInConsole(".rs.api.executeCommand('saveSourceDoc')");
    await sleep(1000);

    // Make an edit so the file has unsaved changes — Ctrl+S during execution
    // must actually trigger a save for the bug to manifest
    await sourceActions.goToEnd();
    await page.keyboard.press('Enter');
    await page.keyboard.press('Enter');
    await page.keyboard.type('It droppeth as the gentle rain from heaven');
    await sleep(500);

    await consoleActions.clearConsole();

    // Navigate to chunk 1 and execute it
    await sourceActions.navigateToChunkByLabel('slow_plot');
    await consoleActions.typeInConsole(".rs.api.executeCommand('executeCurrentChunk')");

    // While the slow plot is rendering, save the document
    // This is the trigger for the bug — saving during execution corrupts the notebook cache
    await sleep(1000);
    await page.keyboard.press('ControlOrMeta+s');

    // Wait for chunk execution to complete (the interrupt button disappears)
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).not.toBeVisible({ timeout: 120000 });
    await sleep(2000);

    // Verify no gzfile / "cannot open" error in console
    const consoleOutput = consoleActions.consolePane.consoleOutput;
    await expect(consoleOutput).not.toContainText('cannot open compressed file', { timeout: 5000 });
    await expect(consoleOutput).not.toContainText('gzfile', { timeout: 5000 });
    await expect(consoleOutput).not.toContainText('No such file or directory', { timeout: 5000 });

    // Execute chunk 2 to verify chunks are still runnable
    await consoleActions.clearConsole();
    await sourceActions.navigateToChunkByLabel('verify_runnable');
    await consoleActions.typeInConsole(".rs.api.executeCommand('executeCurrentChunk')");
    await sleep(5000);

    // Verify chunk 2 output appeared — proves chunks still work after save-during-execution
    await expect(consoleOutput).toContainText('The quality of mercy is not strained', { timeout: 10000 });
    await expect(consoleOutput).toContainText('Chunks still work.', { timeout: 5000 });

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });
});

// Regression test for https://github.com/rstudio/rstudio/issues/17350
// Multiline R expressions in Rmd/Quarto chunks (e.g. `1 +\n2` or `sum(1,\n2)`)
// were hanging because isAtTopLevel() returned false during continuation prompts,
// causing the notebook queue to defer indefinitely.

import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';

test.describe('Multiline chunk execution', { tag: ['@parallel_safe'] }, () => {
  // Sets cwd to a per-spec sandbox; relative paths used by createAndOpenFile
  // and closeSourceAndDeleteFile land there.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);

    await consoleActions.closeAllBuffersWithoutSaving();
    await consoleActions.clearConsole();
  });

  test('multiline expressions in chunks execute without hanging (#17350)', async ({ rstudioPage: page }) => {
    const fileName = `multiline_chunk_${Date.now()}.Rmd`;

    const rmdContent = [
      '---',
      'title: Multiline Chunks',
      '---',
      '',
      '```{r incomplete_expr}',
      '1 +',
      '2',
      '```',
      '',
      '```{r open_paren}',
      'sum(22,',
      '23)',
      '```',
    ].join('\\n');

    await sourceActions.createAndOpenFile(fileName, rmdContent);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });

    // Save so it's on disk
    await consoleActions.typeInConsole(".rs.api.executeCommand('saveSourceDoc')");
    await sleep(1000);

    // --- Chunk 1: incomplete expression (1 +\n2) ---
    await consoleActions.clearConsole();
    await sourceActions.navigateToChunkByLabel('incomplete_expr');
    await consoleActions.typeInConsole(".rs.api.executeCommand('executeCurrentChunk')");

    // If the bug is present, the chunk hangs and the interrupt button stays visible
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).not.toBeVisible({ timeout: 10000 });
    await sleep(1000);

    await expect(consoleActions.consolePane.consoleOutput).toContainText('[1] 3', { timeout: 10000 });

    // --- Chunk 2: open paren (sum(22,\n23)) ---
    await consoleActions.clearConsole();
    await sourceActions.navigateToChunkByLabel('open_paren');
    await consoleActions.typeInConsole(".rs.api.executeCommand('executeCurrentChunk')");

    await expect(page.locator("[id^='rstudio_tb_interruptr']")).not.toBeVisible({ timeout: 10000 });
    await sleep(1000);

    await expect(consoleActions.consolePane.consoleOutput).toContainText('[1] 45', { timeout: 10000 });

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });
});

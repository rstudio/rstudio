import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { executeCommand } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';

const PAGED_TABLE_CELL = '.pagedtable td:visible';

function notebook(code: string): string {
  return ['---', 'output: html_document', '---', '', '```{r}', code, '```', ''].join('\n');
}

// Inline data frame output renders directly in the editor DOM (not in an
// output iframe), so a font stack of its own diverges from the rest of the
// IDE; on Windows machines whose only "Lucida Sans" face is Demibold that
// rendered every cell in bold (#18874). The table must use the IDE font.
test.describe('Notebook paged table font', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    missingPackages = await consoleActions.ensurePackages(['rmarkdown', 'knitr']);
  });

  test.beforeEach(() => {
    test.skip(missingPackages.length > 0, 'Required R packages are unavailable');
  });

  for (const [name, code] of [
    ['bare data frame', 'data.frame(value = 42)'],
    ['paged_table', 'rmarkdown::paged_table(data.frame(value = 42))'],
  ]) {
    test(`${name} output uses the IDE proportional font`, async ({ rstudioPage: page }) => {
      const fileName = `paged-table-font-${name.replaceAll(' ', '-')}.Rmd`;
      await writeAndOpenFile(page, sandbox.dir, fileName, notebook(code));
      await sourceActions.ensureSourceMode();
      await sourceActions.navigateToChunkByIndex(1);
      await executeCommand(page, 'executeCurrentChunk');

      const cell = page.locator(PAGED_TABLE_CELL).first();
      await expect(cell).toHaveText('42', { timeout: 30000 });
      const bodyFont = await page.locator('body').evaluate(el => getComputedStyle(el).fontFamily);
      expect(bodyFont).not.toBe('');
      await expect(cell).toHaveCSS('font-family', bodyFont);
      await expect(cell).toHaveCSS('font-weight', '400');

      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
    });
  }
});

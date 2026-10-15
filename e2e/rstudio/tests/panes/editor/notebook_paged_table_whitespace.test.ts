import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { executeCommand } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';

const PAGED_TABLE_CELL = '.pagedtable td:visible';

// The value under test: a run of five spaces that the paged table used to
// collapse to one when drawn (and so when copied), see #19117.
const VALUE = 'test     this';

function notebook(code: string): string {
  return ['---', 'output: html_document', '---', '', '```{r}', code, '```', ''].join('\n');
}

// Inline data frame output must show the data as-is: a run of spaces in a
// character value is rendered (and copied) with every space intact, while the
// cell still stays on a single line.
test.describe('Notebook paged table whitespace', () => {
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

  test('runs of spaces in cell values are preserved when rendered', async ({ rstudioPage: page }) => {
    const fileName = 'paged-table-whitespace.Rmd';
    await writeAndOpenFile(page, sandbox.dir, fileName, notebook(`data.frame(text = "${VALUE}")`));
    await sourceActions.ensureSourceMode();
    await sourceActions.navigateToChunkByIndex(1);
    await executeCommand(page, 'executeCurrentChunk');

    // toHaveText() normalizes whitespace, so it only gates on the output
    // having arrived; the real assertion reads innerText, which reflects the
    // rendered (CSS white-space) text and is what a copy of the cell yields.
    const cell = page.locator(PAGED_TABLE_CELL).first();
    await expect(cell).toHaveText(VALUE, { timeout: 30000 });
    expect(await cell.evaluate((el) => (el as HTMLElement).innerText)).toBe(VALUE);

    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });
});

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';

test.describe('Code folding', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('.rs.uiPrefs$hierarchicalSectionFolding$clear()');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, ['code_folding.R']);
  });

  // https://github.com/rstudio/rstudio/issues/16541
  test('hierarchical section folding respects heading depth', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('.rs.uiPrefs$hierarchicalSectionFolding$set(TRUE)');

    const content = `# Section 1 ----
code_1 <- 1
## Section 1.1 ----
code_1_1 <- 2
## Section 1.2 ----
code_1_2 <- 3
# Section 2 ----
code_2 <- 4
`;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_1_2');
    await expect.poll(() => editor.getValue()).toContain('code_1_2');

    // All section headers should be fold starts.
    expect(await editor.getFoldWidget(0)).toBe('start'); // # Section 1
    expect(await editor.getFoldWidget(2)).toBe('start'); // ## Section 1.1
    expect(await editor.getFoldWidget(4)).toBe('start'); // ## Section 1.2
    expect(await editor.getFoldWidget(6)).toBe('start'); // # Section 2

    // '# Section 1' folds through both ## subsections to the line before '# Section 2' (row 5).
    let range = await editor.getFoldWidgetRange(0);
    expect(range?.end.row).toBe(5);

    // '## Section 1.1' folds to the line before '## Section 1.2' (row 3).
    range = await editor.getFoldWidgetRange(2);
    expect(range?.end.row).toBe(3);

    // '## Section 1.2' folds to the line before '# Section 2' (row 5).
    range = await editor.getFoldWidgetRange(4);
    expect(range?.end.row).toBe(5);

    // '# Section 2' is the last section; folds to end of document (row 8).
    range = await editor.getFoldWidgetRange(6);
    expect(range?.end.row).toBe(8);
  });

  // https://github.com/rstudio/rstudio/issues/16541
  test('flat section folding stops at any section header', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('.rs.uiPrefs$hierarchicalSectionFolding$set(FALSE)');

    const content = `# Section 1 ----
code_1 <- 1
## Section 1.1 ----
code_1_1 <- 2
# Section 2 ----
code_2 <- 3
`;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_1_1');
    await expect.poll(() => editor.getValue()).toContain('code_1_1');

    // Flat folding: '# Section 1' stops at the next section header (row 1, before ## Section 1.1).
    let range = await editor.getFoldWidgetRange(0);
    expect(range?.end.row).toBe(1);

    // '## Section 1.1' folds to row 3 (before # Section 2).
    range = await editor.getFoldWidgetRange(2);
    expect(range?.end.row).toBe(3);
  });
});

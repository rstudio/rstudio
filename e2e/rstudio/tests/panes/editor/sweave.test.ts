import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/test_files';

test.describe('Sweave', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [
      'sweave_braces.Rnw',
      'sweave_highlight.Rnw',
    ]);
  });

  test('braces are inserted and highlighted correctly in Sweave documents', async ({ rstudioPage: page }) => {
    const content = `This is a Sweave document.

<<>>=

@
`;

    await writeAndOpenFile(page, sandbox.dir, 'sweave_braces.Rnw', content);

    const editor = new AceEditor(page, 'This is a Sweave document.');
    await expect.poll(() => editor.getValue()).toContain('<<>>=');

    // Position cursor on the empty line inside the chunk (line 4, 1-indexed = row 3, 0-indexed)
    await editor.gotoLine(4, 0);
    await page.keyboard.type('{ 1 + 1 }');

    // Wait until the inserted braces tokenize as expected.
    await expect.poll(async () => {
      const tokens = await editor.getTokens(3);
      return tokens.map((t) => t.value);
    }).toEqual(['{', ' ', '1', ' ', '+', ' ', '1', ' ', '}']);
  });

  // https://github.com/rstudio/rstudio/issues/15574
  test('background chunk highlight in Sweave documents is correct', async ({ rstudioPage: page }) => {
    const content = String.raw`\begin{document}

This is some text.

<<chunk>>=
print(1 + 1)
@

This is some more text.

\end{document}
`;

    await writeAndOpenFile(page, sandbox.dir, 'sweave_highlight.Rnw', content);

    const editor = new AceEditor(page, '<<chunk>>=');
    await expect.poll(() => editor.getValue()).toContain('<<chunk>>=');

    // Three background_highlight markers should land on the chunk lines
    // (<<chunk>>= at row 4, print(1+1) at row 5, @ at row 6).
    await expect.poll(async () => {
      const markers = await editor.getMarkers();
      return markers
        .filter((m) => /background_highlight/.test(m.clazz))
        .map((m) => m.range?.start.row);
    }).toEqual([4, 5, 6]);
  });
});

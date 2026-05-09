import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';

test.describe('Syntax Highlighting', () => {
  // Sets cwd to a per-spec sandbox; relative paths used by createAndOpenFile
  // and closeSourceAndDeleteFile land there.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);

    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test('tokens are correct outside R chunks in Rmd', async ({ rstudioPage: page }) => {
    // Original: test_desktop_SyntaxHighlighting.py::test_syntax_highlight
    const document = [
      '---',
      'title: R Notebook',
      'output: html_notebook',
      '---',
      '',
      '```{r}',
      '#| label: This is a label.',
      '```',
      '',
      '1 + 1',
    ].join('\\n');

    const fileName = `syntax_highlight_test_${Date.now()}.Rmd`;

    // Write file and open it
    await sourceActions.createAndOpenFile(fileName, document);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });
    await sleep(2000);

    // Use Ace API to get tokens at line 9 (0-indexed: row 9 = "1 + 1")
    const actual = await page.evaluate(`(function() {
      var editors = document.querySelectorAll('.ace_editor');
      for (var i = 0; i < editors.length; i++) {
        var env = editors[i].env;
        if (env && env.editor) {
          var editor = env.editor;
          if (editor.getValue().indexOf('1 + 1') !== -1) {
            var tokens = editor.session.getTokens(9);
            return tokens[0].value;
          }
        }
      }
      return null;
    })()`);

    expect(actual).toBe('1 + 1');

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });
});

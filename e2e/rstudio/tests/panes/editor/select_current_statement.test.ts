// Code > Select Current Statement (#16102).
//
// Selects the whole R statement containing the cursor -- the same range
// Execute Current Statement runs -- so a multi-line call can be grabbed with
// one keystroke regardless of where in it the cursor sits.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { TIMEOUTS } from '@utils/constants';
import { executeCommand, waitForSourcePaneReset } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { useSuiteSandbox } from '@utils/sandbox';

const SCRIPT = heredoc`
  library(ggplot2)
  data <- head(mtcars, 30)

  p <- ggplot(data, aes(x = wt, y = mpg)) +
    geom_point() + # Show dots
    geom_text(
      label = rownames(data),
      nudge_x = 0.25, nudge_y = 0.25,
      check_overlap = TRUE
    )

  print(p)
`;

// lines 4-10 of the script (0-based rows 3-9)
const STATEMENT = SCRIPT.split('\n').slice(3, 10).join('\n');

test.describe.serial('Select Current Statement', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await waitForSourcePaneReset(page);
  });

  test.afterAll(async () => {
    await consoleActions.resetSourcePane();
  });

  test('selects a multi-line statement from a cursor in its middle', async ({ rstudioPage: page }) => {
    const fileName = 'select_statement.R';
    await writeAndOpenFile(page, sandbox.dir, fileName, SCRIPT);
    try {
      const editor = new AceEditor(page, '');
      await sourceActions.sourcePane.aceTextInput.click({ force: true });

      // inside "nudge_x = 0.25" on the fifth line of the statement
      await editor.gotoLine(8, 12);
      await executeCommand(page, 'selectCurrentStatement');
      await expect.poll(() => editor.getSelectedText(), { timeout: TIMEOUTS.action }).toBe(STATEMENT);

      // a one-line statement selects just that line
      await editor.gotoLine(2, 5);
      await executeCommand(page, 'selectCurrentStatement');
      await expect.poll(() => editor.getSelectedText(), { timeout: TIMEOUTS.action }).toBe(
        'data <- head(mtcars, 30)',
      );
    } finally {
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
    }
  });

  test('stays within the chunk in an R Markdown document', async ({ rstudioPage: page }) => {
    const fileName = 'select_statement.Rmd';
    const fence = '```';
    const rmd = heredoc`
      ---
      title: "Select"
      ---

      ${fence}{r}
      x <- c(1,
             2,
             3)
      ${fence}

      Prose that must not be selected.
    `;
    await writeAndOpenFile(page, sandbox.dir, fileName, rmd);
    try {
      const editor = new AceEditor(page, '');
      await sourceActions.sourcePane.aceTextInput.click({ force: true });

      await editor.gotoLine(7, 8);
      await executeCommand(page, 'selectCurrentStatement');
      await expect.poll(() => editor.getSelectedText(), { timeout: TIMEOUTS.action }).toBe(
        'x <- c(1,\n       2,\n       3)',
      );
    } finally {
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
    }
  });
});

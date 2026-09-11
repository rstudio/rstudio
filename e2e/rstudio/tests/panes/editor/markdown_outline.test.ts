// Document outline for plain Markdown documents (#13505).
//
// The markdown Ace mode now carries the same code model R Markdown uses for
// its heading outline, and the Markdown file type advertises the outline, so
// a .md document gets the outline toggle and its headings nest like in .Rmd.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { TIMEOUTS } from '@utils/constants';
import { executeCommand, isCommandEnabled } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { useSuiteSandbox } from '@utils/sandbox';

const MARKDOWN = heredoc`
  # One

  Some prose.

  ## Two

  More prose.

  ### Three

  # Four
`;

test.describe.serial('Markdown document outline', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  const fileName = 'markdown_outline.md';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, fileName, MARKDOWN);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  test.afterAll(async () => {
    await consoleActions.resetSourcePane();
  });

  test('a .md document offers the outline and its headings nest by level', async ({
    rstudioPage: page,
  }) => {
    // the command is only offered for file types that can show a scope tree
    await expect
      .poll(() => isCommandEnabled(page, 'toggleDocumentOutline'), { timeout: TIMEOUTS.fileOpen })
      .toBe(true);

    // the scopes the outline is built from
    const editor = new AceEditor(page, '');
    await expect
      .poll(async () => (await editor.getSectionScopes()).map((s) => s.label), {
        timeout: TIMEOUTS.fileOpen,
      })
      .toEqual(['One', 'Two', 'Three', 'Four']);

    const scopes = await editor.getSectionScopes();
    expect(scopes.map((s) => [s.depth, s.parent])).toEqual([
      [1, null],
      [2, 'One'],
      [3, 'Two'],
      [1, null],
    ]);

    // the toggle works for this document type (a client exception here would
    // fail the test through the fixture's error drain)
    await executeCommand(page, 'toggleDocumentOutline');
    await expect.poll(() => isCommandEnabled(page, 'toggleDocumentOutline')).toBe(true);
    await executeCommand(page, 'toggleDocumentOutline');
  });
});

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';

// https://github.com/rstudio/rstudio/issues/18468
//
// Pressing Enter inside a C++ chunk routes indentation through CppCodeModel,
// whose TokenUtils tokenized the document without a tokenizer context; the
// chunk-end highlight rule then dereferenced the missing chunk information
// and threw, leaving the new line unindented. The completed R chunk above
// the Rcpp chunk matters: the crash fired only once tokenization crossed a
// completed chunk fence. The rstudio fixture fails any test that raises a
// client exception, so this test also guards against the TypeError itself.
test.describe('C++ chunk indentation', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, ['cpp_chunk_indent.Rmd']);
  });

  test('Enter inside an Rcpp chunk below a completed chunk indents the new line', async ({ rstudioPage: page }) => {
    const content = `---
title: cpp chunk indent
---

\`\`\`{r}
1 + 1
\`\`\`

\`\`\`{Rcpp}
int add(int x) {
\`\`\`
`;

    await writeAndOpenFile(page, sandbox.dir, 'cpp_chunk_indent.Rmd', content);

    const editor = new AceEditor(page, 'int add');
    await expect.poll(() => editor.getValue()).toContain('int add');

    // place the cursor at the end of 'int add(int x) {' and press Enter
    await editor.gotoLine(10, 16);
    await editor.focus();
    await page.keyboard.press('Enter');

    // the new line should carry the indent for the opened brace
    await expect.poll(() => editor.getLine(10)).toMatch(/^\s+$/);
  });
});

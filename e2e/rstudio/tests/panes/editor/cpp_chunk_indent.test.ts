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
// client exception (unless PW_IGNORE_CLIENT_EXCEPTIONS=1 downgrades that to
// a warning), so this test also guards against the TypeError itself.
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

    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getValue()).toContain('int add');

    // the brace line ('int add(int x) {', row 9, 0-based) must be tokenized
    // as C++ embedded in R Markdown -- this pins the mechanism, not just the
    // resulting indent
    await expect.poll(() => editor.getState(9)).toMatch(/^r-cpp-/);

    // place the cursor at the end of 'int add(int x) {' and press Enter
    // (gotoLine is 1-based, matching the editor gutter: line 10 here is the
    // same line as row 9 above)
    await editor.gotoLine(10, 16);
    await editor.focus();
    await page.keyboard.press('Enter');

    // the newly inserted line (row 10, 0-based) should carry exactly one
    // indent level for the opened brace: the brace line is at column 0
    const tab = await editor.getTabString();
    await expect.poll(() => editor.getLine(10)).toBe(tab);
  });
});

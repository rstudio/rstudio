import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, TIMEOUTS } from '@utils/constants';
import { AceEditor } from '@pages/ace_editor.page';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand } from '@utils/commands';

const RMD_CONTENT = `---
title: Refactoring
---

\`\`\`{r}
# These should all get selected.
variable <- 42
print(variable + variable)
\`\`\`

\`\`\`{r}
# These should also get selected.
print(variable + variable)
\`\`\`

\`\`\`{r}
# These should be ignored.
example <- function(variable) {
   variable
}
\`\`\`
`;

// https://github.com/rstudio/rstudio/issues/4961
test.describe('Rename in scope across Rmd chunks', () => {
  const sandbox = useSuiteSandbox();
  const FILE = 'rename_in_scope.Rmd';

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE]);
  });

  test('renameInScope selects all matching occurrences across sibling chunks', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILE, RMD_CONTENT);

    // Marker text uniquely identifies this editor (avoid colliding with any
    // other "variable" text in the rest of the suite).
    const editor = new AceEditor(page, '# These should all get selected.');

    // Position the cursor on the `variable <- 42` line. `gotoLine` is 1-based.
    await editor.gotoLine(7, 0);
    await sleep(200);

    await executeCommand(page, 'renameInScope');

    await expect.poll(() => editor.getSelectionRanges().then((r) => r.length), {
      timeout: TIMEOUTS.fileEditSettle,
    }).toBe(5);

    const ranges = await editor.getSelectionRanges();
    expect(ranges).toEqual([
      { start: { row: 6, column: 0 }, end: { row: 6, column: 8 } },
      { start: { row: 7, column: 6 }, end: { row: 7, column: 14 } },
      { start: { row: 7, column: 17 }, end: { row: 7, column: 25 } },
      { start: { row: 12, column: 6 }, end: { row: 12, column: 14 } },
      { start: { row: 12, column: 17 }, end: { row: 12, column: 25 } },
    ]);

    // Exit multi-cursor mode so the buffer can be closed cleanly.
    await page.keyboard.press('Escape');
  });
});

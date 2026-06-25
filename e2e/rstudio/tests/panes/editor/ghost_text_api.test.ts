// Ghost-text tests for rstudioapi::setGhostText().
//
// These drive .rs.api.setGhostText via the console to place renderer ghost
// text at the active editor's cursor, then assert it is dismissed -- both
// visually and in internal state -- when the cursor moves or the document is
// edited. This is the renderer's $ghostText mechanism (also used by at-cursor
// completion previews), distinct from the assistant's synthetic ghost-text /
// insertion-preview tokens covered by edit_suggestions.test.ts.
//
// Regression coverage for https://github.com/rstudio/rstudio/issues/18033,
// where the ghost text stopped being dismissed and Tab would still insert it
// after it appeared to be gone.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { SourcePane } from '@pages/source_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';

const FILE_PREFIX = 'gt_api_';
const FILES = {
  cursorDismiss: `${FILE_PREFIX}cursor_dismiss.R`,
  editDismiss:   `${FILE_PREFIX}edit_dismiss.R`,
} as const;

// Trailing blank lines keep the cursor off the last line of the file:
// setGhostText does not render when the cursor is on the final line.
const CONTENTS = '# placeholder\n\n\n';
const MARKER = '# placeholder';

test.describe('Ghost text (rstudioapi::setGhostText injection)', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, Object.values(FILES));
  });

  test('setGhostText is dismissed when the cursor moves away', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.cursorDismiss, CONTENTS);

    const editor = new AceEditor(page, MARKER);
    const sourcePane = new SourcePane(page);

    await editor.gotoLine(1, MARKER.length);
    await consoleActions.executeInConsole('.rs.api.setGhostText("hello world")');

    await expect(sourcePane.ghostText.first()).toBeVisible();
    await expect.poll(() => editor.hasRendererGhostText()).toBe(true);

    // Moving the cursor away dismisses the ghost text and clears the internal
    // state, so a later Tab cannot insert it.
    await editor.gotoLine(3);
    await expect(sourcePane.ghostText).toHaveCount(0);
    await expect.poll(() => editor.hasRendererGhostText()).toBe(false);
    expect(await editor.getLine(0)).toBe(MARKER);
  });

  test('setGhostText is dismissed on a non-matching document edit', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, FILES.editDismiss, CONTENTS);

    const editor = new AceEditor(page, MARKER);
    const sourcePane = new SourcePane(page);

    await editor.gotoLine(1, MARKER.length);
    await consoleActions.executeInConsole('.rs.api.setGhostText("hello world")');

    await expect(sourcePane.ghostText.first()).toBeVisible();
    await expect.poll(() => editor.hasRendererGhostText()).toBe(true);

    // A non-matching edit dismisses the ghost text and clears state; the edit
    // itself lands but the ghost text is not inserted.
    await editor.insert(' ');
    await expect(sourcePane.ghostText).toHaveCount(0);
    await expect.poll(() => editor.hasRendererGhostText()).toBe(false);
    expect(await editor.getLine(0)).toBe(MARKER + ' ');
  });
});

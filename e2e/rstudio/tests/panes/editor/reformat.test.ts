// Code reformatting (built-in + styler). Air formatter cases live in
// air_formatting.test.ts.

import * as fs from 'fs';
import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { SourcePane } from '@pages/source_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import {
  createAndOpenProject,
  closeProjectIfOpen,
  waitForConsoleIdle,
} from '@utils/project';
import { TIMEOUTS } from '@utils/constants';

const FILE_5425 = 'reformat_5425.R';
const FILE_STYLER_DOC = 'reformat_styler_doc.R';
const FILE_STYLER_SEL = 'reformat_styler_selection.R';

async function focusEditor(page: Page): Promise<void> {
  await new SourcePane(page).contentPane.click();
  // Wait for keyboard focus to actually land on a source-pane Ace textarea
  // (not the console). page.click() resolves before the focus event has
  // propagated; without this gate, subsequent keystrokes can fire while
  // focus is still mid-flight and route to the previously focused element.
  await page.waitForFunction(
    () => {
      const el = document.activeElement;
      if (!el) return false;
      if (el.closest('#rstudio_console_input')) return false;
      return el.classList?.contains('ace_text-input') ?? false;
    },
    null,
    { timeout: 2000, polling: 50 },
  );
}

async function selectAllInEditor(page: Page): Promise<void> {
  await focusEditor(page);
  await page.keyboard.press('ControlOrMeta+a');
}

test.describe('Built-in code reformat', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE_5425]);
  });

  // https://github.com/rstudio/rstudio/issues/5425
  test('preserves end-of-line comment when reformatting', async ({ rstudioPage: page }) => {
    const initial = 'c(1 #2\n)';
    const expected = 'c(\n  1 #2\n)';
    await writeAndOpenFile(page, sandbox.dir, FILE_5425, initial);

    // `1 #2` survives the reformat unchanged, so use it as the editor marker.
    // `c(1` would be broken across lines by the reformat and fail to match.
    const editor = new AceEditor(page, '1 #2');
    await expect.poll(() => editor.getValue()).toBe(initial);

    await selectAllInEditor(page);
    await executeCommand(page, 'reformatCode');
    await expect.poll(() => editor.getValue()).toBe(expected);
  });
});

// Reformat-on-save requires an active project that contains the file (see
// TextEditingTarget.maybeFormatOnUserInitiatedSave). Open one for this test.
test.describe.serial('styler reformat on save', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let projectDir: string;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
    missingPackages = await consoleActions.ensurePackages(['styler'], 180_000);

    projectDir = await createAndOpenProject(page, sandbox.dir, 'reformat-styler-project');
    await waitForConsoleIdle(page);

    await setPref(page, 'reformat_on_save', true);
    await setPref(page, 'code_formatter', 'styler');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'reformat_on_save');
    await clearPref(page, 'code_formatter');
    await consoleActions.resetSourcePane();
    await waitForConsoleIdle(page);
    await closeProjectIfOpen(page);
  });

  test('documents are reformatted on save', async ({ rstudioPage: page }) => {
    test.skip(missingPackages.length > 0, `styler not available: ${missingPackages.join(', ')}`);

    // File must live inside the project dir or maybeFormatOnUserInitiatedSave
    // bails out before invoking the formatter. Use writeAndOpenFile so we
    // also wait for the tab to render before driving keystrokes.
    const fileName = 'styler_save.R';
    await writeAndOpenFile(page, projectDir, fileName, '# placeholder');
    const filePath = `${projectDir}/${fileName}`;

    // Empty-marker AceEditor matches the first non-console editor. Using
    // '# placeholder' would break after the select-all-then-type replaces
    // the file's contents, since AceEditor.runOnEditor finds the editor by
    // content-substring match.
    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getValue()).toBe('# placeholder');

    // Replace the placeholder by selecting all and typing the messy code;
    // the buffer becomes dirty, so Ctrl+S triggers reformat-on-save.
    await selectAllInEditor(page);
    await page.keyboard.type('1+1; 2+2');
    // Wait for the editor to actually contain the typed text before saving.
    // keyboard.type returns when keystrokes have been dispatched, not when
    // Ace has applied them all -- a fast Ctrl+S can race the last char.
    await expect.poll(() => editor.getValue()).toBe('1+1; 2+2');

    await page.keyboard.press('ControlOrMeta+s');

    // styler runs async; poll the disk for the formatted file.
    await expect.poll(
      () => fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n'),
      { timeout: TIMEOUTS.consoleReady },
    ).toBe('1 + 1\n2 + 2\n');
  });
});

// https://github.com/rstudio/rstudio/issues/17471
// Windows-only: styler's writeLines writes CRLF on Windows, which the
// formatter callers then read back. Without LineEndingPosix normalization
// the character-level diff against the LF-normalized in-memory document
// produces \r insertions before each \n; on the client, Ace's Document.$split
// regex turns each inserted bare \r into another \n, doubling line breaks.
// On macOS/Linux styler writes LF and the bug doesn't manifest. These tests
// drive the manual-reformat code path, which doesn't require an active
// project (unlike reformat-on-save above).
test.describe('styler reformat #17471 (Windows)', { tag: ['@windows_only'] }, () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
    missingPackages = await consoleActions.ensurePackages(['styler'], 180_000);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await clearPref(page, 'code_formatter');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE_STYLER_DOC, FILE_STYLER_SEL]);
  });

  test('Reformat Document does not add extra newlines', async ({ rstudioPage: page }) => {
    test.skip(missingPackages.length > 0, `styler not available: ${missingPackages.join(', ')}`);

    await setPref(page, 'code_formatter', 'styler');

    const initial = 'print("test")\nprint("test")\n\n';
    const expected = 'print("test")\nprint("test")\n';
    await writeAndOpenFile(page, sandbox.dir, FILE_STYLER_DOC, initial);

    const editor = new AceEditor(page, 'print(');
    await expect.poll(() => editor.getValue()).toBe(initial);

    await executeCommand(page, 'reformatDocument');
    await expect.poll(() => editor.getValue()).toBe(expected);
  });

  test('Reformat Code (selection) does not add extra newlines', async ({ rstudioPage: page }) => {
    test.skip(missingPackages.length > 0, `styler not available: ${missingPackages.join(', ')}`);

    await setPref(page, 'code_formatter', 'styler');

    const initial = '1+1; 2+2\n3+3; 4+4\n';
    const expected = '1 + 1\n2 + 2\n3 + 3\n4 + 4\n';
    await writeAndOpenFile(page, sandbox.dir, FILE_STYLER_SEL, initial);

    // Empty-marker: targets the active editor by bridge lookup, which openFile
    // already confirmed is non-null. More reliable than a content-substring DOM
    // scan when the file was just opened and content may not be in the DOM yet.
    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getValue(), { timeout: 20000 }).toBe(initial);

    await selectAllInEditor(page);
    await executeCommand(page, 'reformatCode');
    // trimEnd: Ace's getValue() on Windows omits the trailing newline from the
    // document model; the substantive assertion is "no extra blank lines", not
    // the presence of exactly one trailing newline.
    await expect.poll(() => editor.getValue().trimEnd()).toBe(expected.trimEnd());
  });
});

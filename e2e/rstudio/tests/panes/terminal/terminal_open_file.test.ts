// Opening files in the source editor from the Terminal pane (#14226).
//
// The `rstudio` script shipped alongside the other postback scripts resolves
// each argument against the terminal's working directory and hands it to the
// session's "openfile" postback handler, which fires the same FileEdit event
// as file.edit(). Not available on Windows (rpostback is not installed there).

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { AceEditor } from '@pages/ace_editor.page';
import { documentCloseAllNoSave, executeCommand, waitForActiveDocument } from '@utils/commands';
import { seedSandboxFile } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';
import { rStringLiteral } from '@utils/r';
import { captureResult, focusTerminal, killAllTerminals, openTerminal } from '@utils/terminal';

const FILE_CONTENT = 'first <- 1\nsecond <- 2\nthird <- 3\n';

async function runInTerminal(page: Page, command: string): Promise<void> {
  await page.keyboard.type(command);
  await page.keyboard.press('Enter');
}

const sandbox = useSuiteSandbox();

test.describe.serial('Terminal: rstudio command opens files', () => {
  let isWindows = false;

  test.beforeAll(async ({ rstudioPage: page }) => {
    isWindows = (await captureResult(page, '.Platform$OS.type')) === 'windows';
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    test.skip(isWindows, 'the rstudio terminal command is not available on Windows');
    await documentCloseAllNoSave(page);
    await killAllTerminals(page);
    await openTerminal(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page).catch(() => {});
    await documentCloseAllNoSave(page).catch(() => {});
    await executeCommand(page, 'activateConsole').catch(() => {});
  });

  test('an absolute path with a :line suffix opens the file at that line', async ({
    rstudioPage: page,
  }) => {
    // Against a remote server the seed is written through the R console,
    // which takes focus away from the terminal.
    const fullPath = await seedSandboxFile(page, sandbox.dir, 'open_from_terminal.R', FILE_CONTENT);
    await focusTerminal(page);

    await runInTerminal(page, `rstudio "${fullPath}:2"`);

    await waitForActiveDocument(page, fullPath, TIMEOUTS.fileOpen);
    const editor = new AceEditor(page, '');
    await expect.poll(() => editor.getValue()).toBe(FILE_CONTENT);
    await expect.poll(async () => (await editor.getCursorPosition()).row).toBe(1);
  });

  test('a relative path resolves against the terminal cwd and a new file is created', async ({
    rstudioPage: page,
  }) => {
    const fileName = 'new_from_terminal.R';
    const fullPath = `${sandbox.dir}/${fileName}`;

    await runInTerminal(page, `cd "${sandbox.dir}"`);
    await runInTerminal(page, `rstudio ${fileName}`);

    await waitForActiveDocument(page, fullPath, TIMEOUTS.fileOpen);
    await expect
      .poll(() => captureResult(page, `file.exists(${rStringLiteral(fullPath)})`))
      .toBe('TRUE');
  });
});

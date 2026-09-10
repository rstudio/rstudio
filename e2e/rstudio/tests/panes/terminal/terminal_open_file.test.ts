// Opening files in the source editor from the Terminal pane (#14226).
//
// The `rstudio` script shipped alongside the other postback scripts resolves
// each argument against the terminal's working directory and hands it to the
// session's "openfile" postback handler, which fires the same FileEdit event
// as file.edit(). Not available on Windows (rpostback is not installed there).

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { AceEditor } from '@pages/ace_editor.page';
import { documentCloseAllNoSave, executeCommand, waitForActiveDocument } from '@utils/commands';
import { seedSandboxFile } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';
import { rStringLiteral } from '@utils/r';

const TERMINAL_TAB = '#rstudio_workbench_tab_terminal';
const XTERM_SELECTOR = '.xterm';

const FILE_CONTENT = 'first <- 1\nsecond <- 2\nthird <- 3\n';

async function captureResult(page: Page, rExpression: string): Promise<string> {
  const marker = `__TERM_OPEN_${Date.now()}__`;
  await executeInConsole(
    page,
    `cat(${rStringLiteral(marker)}, ${rExpression}, ${rStringLiteral(marker)})`,
    { wait: true },
  );

  const pattern = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`, 's');
  const output = await page.locator(CONSOLE_OUTPUT).innerText();
  const match = output.match(pattern);
  if (!match) throw new Error(`captureResult: markers not found for "${rExpression}"`);
  return match[1].trim();
}

async function killAllTerminals(page: Page): Promise<void> {
  await executeInConsole(page, 'rstudioapi::terminalKill(rstudioapi::terminalList())', {
    wait: true,
  });
}

// Creates a terminal and returns once its shell is echoing a prompt, with the
// xterm widget focused for typing (see terminal.test.ts for the rationale).
async function openTerminal(page: Page): Promise<void> {
  await executeInConsole(page, 'rstudioapi::terminalCreate(show = TRUE)');
  await expect(page.locator(XTERM_SELECTOR)).toBeVisible({ timeout: TIMEOUTS.consoleReady });

  await expect
    .poll(
      () =>
        captureResult(
          page,
          '{ ids <- rstudioapi::terminalList(); ' +
            'length(ids) > 0 && any(nzchar(trimws(rstudioapi::terminalBuffer(ids[[1]])))) }',
        ),
      { timeout: TIMEOUTS.consoleReady },
    )
    .toBe('TRUE');

  await page.locator(TERMINAL_TAB).click();
  await expect(page.locator(XTERM_SELECTOR)).toBeVisible({ timeout: TIMEOUTS.consoleReady });
  await page.locator(XTERM_SELECTOR).click();
}

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
    const fullPath = await seedSandboxFile(page, sandbox.dir, 'open_from_terminal.R', FILE_CONTENT);

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

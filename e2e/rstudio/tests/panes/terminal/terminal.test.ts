import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { AceEditor } from '@pages/ace_editor.page';
import { rStringLiteral } from '@utils/r';
import { executeCommand } from '@utils/commands';
import type { Page } from 'playwright';

const TERMINAL_TAB = '#rstudio_workbench_tab_terminal';
const XTERM_SELECTOR = '.xterm';

async function captureResult(page: Page, rExpression: string): Promise<string> {
  const marker = `__TERM_${Date.now()}__`;
  // Gate on R reporting idle so the marker pair has fully written by the time
  // we read the console.
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
  await executeInConsole(
    page,
    'rstudioapi::terminalKill(rstudioapi::terminalList())',
    { wait: true },
  );
}

async function openTerminal(page: Page): Promise<void> {
  await executeInConsole(page, 'rstudioapi::terminalCreate(show = TRUE)');
  await expect(page.locator(XTERM_SELECTOR)).toBeVisible({ timeout: TIMEOUTS.consoleReady });
  await expect(page.locator(TERMINAL_TAB)).toHaveAttribute('aria-selected', 'true', {
    timeout: TIMEOUTS.consoleReady,
  });
}

test.describe.serial('Terminal pane', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page).catch(() => {});
    await executeCommand(page, 'activateConsole').catch(() => {});
  });

  test('a terminal can be created and is visible', async ({ rstudioPage: page }) => {
    await killAllTerminals(page);
    await openTerminal(page);

    const xterm = page.locator(XTERM_SELECTOR);
    const box = await xterm.boundingBox();
    expect(box, 'xterm widget should have a bounding box').not.toBeNull();
    expect(box!.width, 'xterm width').toBeGreaterThan(0);
    expect(box!.height, 'xterm height').toBeGreaterThan(0);
  });

  test('we can run commands in the terminal', async ({ rstudioPage: page }) => {
    await killAllTerminals(page);
    await openTerminal(page);

    // After openTerminal returns, the terminal tab is selected and xterm
    // is focused. Type the command via the page keyboard.
    await page.keyboard.type('expr 1 + 1');
    await page.keyboard.press('Enter');

    // Wait for the terminal buffer to include the result line. Poll via
    // rstudioapi::terminalBuffer -- the xterm canvas isn't directly readable.
    await expect.poll(
      () => captureResult(
        page,
        '{ ids <- rstudioapi::terminalList(); ' +
        'length(ids) > 0 && any(grepl("^2$", rstudioapi::terminalBuffer(ids[[1]]))) }',
      ),
      { timeout: TIMEOUTS.consoleReady },
    ).toBe('TRUE');

    // Send the terminal contents to a new editor tab.
    await executeCommand(page, 'sendTerminalToEditor');

    // Find the new editor by its marker and assert it contains the expected
    // command + output sequence.
    const editor = new AceEditor(page, 'expr 1 + 1');
    await expect.poll(
      async () => {
        try {
          return (await editor.getValue()).replace(/\r?\n+/g, '\n');
        } catch {
          return '';
        }
      },
      { timeout: TIMEOUTS.fileOpen },
    ).toContain('expr 1 + 1\n2\n');
  });
});

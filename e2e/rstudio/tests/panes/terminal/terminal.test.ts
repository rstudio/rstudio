import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { AceEditor } from '@pages/ace_editor.page';
import { rStringLiteral } from '@utils/r';
import { executeCommand } from '@utils/commands';
import { useSuiteSandbox } from '@utils/sandbox';
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

// Sandbox for file-creation test (terminal cwd is its own shell, so we
// pass the absolute sandbox path directly to the shell command).
const sandbox = useSuiteSandbox();

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

    // Find the new editor by its marker and assert it contains both the
    // command and the result "2" on its own line. We can't require the
    // literal substring "expr 1 + 1\n2\n" -- on Server-on-Linux the prompt
    // ($USER@$HOST:$CWD$ ) is long enough that "expr 1 + 1" wraps in xterm,
    // and the captured editor content interleaves a wrap marker (·) and the
    // shell prompt between the command and its output. The semantic check
    // ("editor has the command and the output 2") is what the test is really
    // verifying, and that holds regardless of wrap. The rstudioapi::terminalBuffer
    // ^2$ poll above is the authoritative output check; this assertion only
    // confirms the editor tab received both the command and the result.
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
    ).toMatch(/expr 1 \+ 1[\s\S]*\n2(?:\n|$)/);
  });

  test('toolbar shows next and previous terminal buttons', async ({ rstudioPage: page }) => {
    await killAllTerminals(page);
    await openTerminal(page);

    await expect(page.locator('#rstudio_tb_nextterminal')).toBeVisible();
    await expect(page.locator('#rstudio_tb_previousterminal')).toBeVisible();
  });

  test('R --version runs in the terminal', async ({ rstudioPage: page }) => {
    await killAllTerminals(page);
    await openTerminal(page);

    await page.keyboard.type('R --version');
    await page.keyboard.press('Enter');

    await expect.poll(
      () => captureResult(
        page,
        '{ ids <- rstudioapi::terminalList(); ' +
        'length(ids) > 0 && any(grepl("R version", rstudioapi::terminalBuffer(ids[[1]]))) }',
      ),
      { timeout: TIMEOUTS.consoleReady },
    ).toBe('TRUE');
  });

  test('a file created in the terminal appears in a directory listing', async ({
    rstudioPage: page,
  }) => {
    await killAllTerminals(page);
    await openTerminal(page);

    // cd into the absolute sandbox path first, then create and list the file
    // using a short name. The file is cleaned up by globalTeardown regardless
    // of the terminal's working directory. We deliberately keep the touch/ls
    // arguments short rather than passing the absolute path: a narrow terminal
    // wraps a long path across physical lines, and terminalBuffer() returns one
    // entry per physical line, which would split "ztestfile.txt" across a wrap
    // boundary and break the grep below. Require the filename to appear at least
    // twice in the buffer: once in the echoed touch command and once in the ls
    // output. A single match would pass even if touch failed (the filename
    // appears in the echoed command regardless).
    const sandboxDir = sandbox.dir.replace(/\\/g, '/');
    await page.keyboard.type(`cd ${sandboxDir}`);
    await page.keyboard.press('Enter');
    await page.keyboard.type('touch ztestfile.txt');
    await page.keyboard.press('Enter');
    await page.keyboard.type('ls');
    await page.keyboard.press('Enter');

    await expect.poll(
      () => captureResult(
        page,
        '{ ids <- rstudioapi::terminalList(); ' +
        'buf <- paste(rstudioapi::terminalBuffer(ids[[1]]), collapse = "\\n"); ' +
        'length(ids) > 0 && length(grep("ztestfile.txt", unlist(strsplit(buf, "\\n")))) >= 2 }',
      ),
      { timeout: TIMEOUTS.consoleReady },
    ).toBe('TRUE');
  });

  test(
    'Shift+Backspace deletes the last character before submission',
    { tag: ['@desktop_only'] },
    async ({ rstudioPage: page }) => {
      await killAllTerminals(page);
      await openTerminal(page);

      await page.keyboard.type('echo hello');
      await page.keyboard.press('Shift+Backspace');
      await page.keyboard.press('Enter');

      // "echo hell" must appear AND "echo hello" must not -- if Shift+Backspace
      // had no effect the full word would be present and the test would pass
      // vacuously on the substring match alone.
      await expect.poll(
        () => captureResult(
          page,
          '{ ids <- rstudioapi::terminalList(); ' +
          'buf <- paste(rstudioapi::terminalBuffer(ids[[1]]), collapse = "\\n"); ' +
          'grepl("echo hell", buf) && !grepl("echo hello", buf) }',
        ),
        { timeout: TIMEOUTS.consoleReady },
      ).toBe('TRUE');
    },
  );
});

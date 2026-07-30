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

  // The xterm widget becomes visible before the shell has attached to the pty
  // and echoed its prompt; keystrokes sent before then are dropped (observed
  // on macOS CI: the buffer held only the prompt, every typed line lost).
  // Wait until the buffer has a non-empty line -- proof the shell is echoing.
  await expect.poll(
    () => captureResult(
      page,
      '{ ids <- rstudioapi::terminalList(); ' +
      'length(ids) > 0 && any(nzchar(trimws(rstudioapi::terminalBuffer(ids[[1]])))) }',
    ),
    { timeout: TIMEOUTS.consoleReady },
  ).toBe('TRUE');

  // captureResult drives the R console (it clicks the Console tab and focuses
  // the console input), so reselect the terminal and give it keyboard focus
  // before callers start typing.
  await page.locator(TERMINAL_TAB).click();
  await expect(page.locator(XTERM_SELECTOR)).toBeVisible({ timeout: TIMEOUTS.consoleReady });
  await page.locator(XTERM_SELECTOR).click();
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

    // Find the new editor as the active tab (sendTerminalToEditor activates
    // it) and assert it contains both the command and the result "2" on its
    // own line. We can't require any contiguous substring of the command --
    // on Server-on-Linux the prompt ($USER@$HOST:$CWD$ ) pushes the command
    // toward the xterm wrap column, and depending on the prompt length the
    // hard wrap can land anywhere, even mid-word ("...$ exp" / "r 1 + 1" was
    // observed when the cwd was a long sandbox path). That also rules out
    // locating the editor by content marker. So compare with all whitespace
    // and wrap markers (U+00B7 middle dot) stripped, which is invariant
    // under any wrap position, and check the result "2" (a single character,
    // so it can't wrap) on a raw line of its own. The rstudioapi::terminalBuffer ^2$
    // poll above is the authoritative output check; this assertion only
    // confirms the editor tab received both the command and the result.
    const editor = new AceEditor(page, '');
    await expect.poll(
      async () => {
        try {
          const value = (await editor.getValue()).replace(/\r?\n+/g, '\n');
          const dewrapped = value.replace(/[\s\u00B7]+/g, '');
          return dewrapped.includes('expr1+1') && /(?:^|\n)2(?:\n|$)/.test(value);
        } catch {
          return false;
        }
      },
      { timeout: TIMEOUTS.fileOpen },
    ).toBe(true);
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

      // Verify the deletion through a filesystem side effect rather than the
      // terminal buffer: type a `touch` command with a stray trailing "Q",
      // delete it with Shift+Backspace, and check which file was created.
      // The buffer is unsuitable for this assertion: after the deletion,
      // readline may redraw the whole (wrapped) prompt line instead of
      // erasing in place, and the server-side buffer capture keeps the stale
      // pre-erase command image, so a grep for the original text matches even
      // though the deletion worked. Whether readline erases or redraws is a
      // shell implementation detail -- the grep started failing with Ubuntu
      // 26.04 (bash 5.3 / readline 8.3, which reworked redisplay) while
      // older Ubuntu erased in place -- and also depends on prompt width,
      // which varies per CI runner (hostname and sandbox path lengths).
      const sandboxDir = sandbox.dir.replace(/\\/g, '/');
      await page.keyboard.type(`cd ${sandboxDir}`);
      await page.keyboard.press('Enter');
      await page.keyboard.type('touch term_bs_test.txtQ');
      await page.keyboard.press('Shift+Backspace');
      await page.keyboard.press('Enter');

      // If Shift+Backspace deleted the trailing "Q", the file without the
      // "Q" exists and the file with it does not. If the keystroke had no
      // effect, only "term_bs_test.txtQ" exists and the first condition
      // stays false; if it deleted more than one character, neither file
      // matches. The file is cleaned up with the sandbox by globalTeardown.
      await expect.poll(
        () => captureResult(
          page,
          `{ file.exists(file.path(${rStringLiteral(sandboxDir)}, "term_bs_test.txt")) && ` +
          `!file.exists(file.path(${rStringLiteral(sandboxDir)}, "term_bs_test.txtQ")) }`,
        ),
        { timeout: TIMEOUTS.consoleReady },
      ).toBe('TRUE');
    },
  );
});

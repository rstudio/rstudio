// Helpers for driving the Terminal pane through a real shell.
//
// The xterm widget cannot be read directly, so terminal state is observed via
// rstudioapi::terminalBuffer() in the R console. Every console round trip
// moves focus away from the terminal; use focusTerminal() before typing.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { rStringLiteral } from '@utils/r';

export const TERMINAL_TAB = '#rstudio_workbench_tab_terminal';
export const XTERM_SELECTOR = '.xterm';

/** Evaluate an R expression in the console and return its printed value. */
export async function captureResult(page: Page, rExpression: string): Promise<string> {
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

export async function killAllTerminals(page: Page): Promise<void> {
  await executeInConsole(page, 'rstudioapi::terminalKill(rstudioapi::terminalList())', {
    wait: true,
  });
}

/**
 * Select the Terminal tab and give the xterm widget keyboard focus. Needed
 * after anything that drives the R console (captureResult, seedSandboxFile on
 * a remote server, ...), since the console path clicks the Console tab and
 * focuses the console input.
 */
export async function focusTerminal(page: Page): Promise<void> {
  await page.locator(TERMINAL_TAB).click();
  await expect(page.locator(XTERM_SELECTOR)).toBeVisible({ timeout: TIMEOUTS.consoleReady });
  await page.locator(XTERM_SELECTOR).click();
}

/** Create a terminal and return once its shell is echoing a prompt and focused for typing. */
export async function openTerminal(page: Page): Promise<void> {
  await executeInConsole(page, 'rstudioapi::terminalCreate(show = TRUE)');
  await expect(page.locator(XTERM_SELECTOR)).toBeVisible({ timeout: TIMEOUTS.consoleReady });
  await expect(page.locator(TERMINAL_TAB)).toHaveAttribute('aria-selected', 'true', {
    timeout: TIMEOUTS.consoleReady,
  });

  // The xterm widget becomes visible before the shell has attached to the pty
  // and echoed its prompt; keystrokes sent before then are dropped (observed
  // on macOS CI: the buffer held only the prompt, every typed line lost).
  // Wait until the buffer has a non-empty line -- proof the shell is echoing.
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

  await focusTerminal(page);
}

/**
 * Run `input` -- keystrokes that submit one or more shell commands at the
 * focused terminal -- then `landed`, which must throw until their effect is
 * observable. If it never is, interrupt the prompt and do both once more.
 *
 * A keystroke typed while the pty is being resized can be lost. Seen on
 * Windows CI (ConPTY + MSYS bash): "echo" arrived as "cho" and "expr 1 + 1"
 * as "expr 1+ 1", each within ~50ms of a process_set_size RPC. A resize tends
 * to land just as a test starts typing: a hidden terminal is not re-fit, so
 * a size change that comes up while openTerminal() has the Console tab
 * selected is sent when the Terminal tab is shown again, and switching
 * terminal_renderer re-fits too. Nothing signals that the shell is done
 * handling a resize, so detect the loss rather than trying to wait it out.
 * The size has settled by the second attempt.
 *
 * The commands must be safe to submit twice. A retry is recorded as a test
 * annotation so that lost input stays visible in the report.
 */
export async function retryTerminalInput(
  page: Page,
  input: () => Promise<void>,
  landed: () => Promise<void>,
): Promise<void> {
  await input();

  try {
    await landed();
    return;
  } catch (error) {
    test.info().annotations.push({
      type: 'terminal-input-retry',
      description: error instanceof Error ? error.message.split('\n')[0] : String(error),
    });
  }

  // `landed` may have driven the R console; Ctrl+C discards whatever is left
  // of the first attempt on the command line.
  await focusTerminal(page);
  await page.keyboard.press('Control+C');
  await input();
  await landed();
}

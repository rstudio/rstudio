// Select All in the Terminal pane's desktop right-click menu (#19390).
//
// The browser's own select-all, run over the terminal, only selects xterm's
// hidden helper textarea. The desktop menu instead sends an
// 'rstudio-terminal-select-all' event to that textarea (see
// src/node/desktop/src/main/context-menu.ts), and the terminal answers it by
// selecting its buffer. Electron's native menu can't be driven from here, so
// this test sends the event the way the menu does, on Desktop and Server.
//
// The test copies the selection, so a local Desktop run replaces whatever was
// on the system clipboard.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { executeCommand } from '@utils/commands';
import { killAllTerminals, openTerminal } from '@utils/terminal';

/**
 * The text a copy would put on the clipboard, read from the copy event rather
 * than the clipboard itself. xterm's copy handler fills in the event's data.
 */
async function copiedText(page: Page): Promise<string> {
  return page.evaluate(() => {
    let text = '';
    const onCopy = (event: ClipboardEvent) => {
      text = event.clipboardData?.getData('text/plain') ?? '';
    };
    window.addEventListener('copy', onCopy);
    try {
      document.execCommand('copy');
    } finally {
      window.removeEventListener('copy', onCopy);
    }
    return text;
  });
}

/** Mirrors the script the desktop menu runs in the page. */
async function sendSelectAll(page: Page): Promise<boolean> {
  return page.evaluate(() => {
    const textarea = document.activeElement;
    if (textarea?.classList.contains('xterm-helper-textarea') !== true) {
      return false;
    }
    textarea.dispatchEvent(new Event('rstudio-terminal-select-all'));
    return true;
  });
}

test.describe('Terminal: right-click Select All', () => {
  test.beforeEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page);
    await openTerminal(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page).catch(() => {});
    await executeCommand(page, 'activateConsole').catch(() => {});
  });

  test("selects the terminal's whole buffer", async ({ rstudioPage: page }) => {
    // Quotes split each word on the command line, so only the output has it
    // whole. Two lines, so a selection of one row or word can't pass.
    const stamp = Date.now();
    const first = `first_${stamp}`;
    const second = `second_${stamp}`;
    await page.keyboard.type(`echo first_"${stamp}"; echo second_"${stamp}"`);
    await page.keyboard.press('Enter');

    // Retried until the output has arrived. Without the terminal's listener
    // nothing is selected, the copy carries no text, and this times out.
    await expect
      .poll(
        async () => {
          if (!(await sendSelectAll(page))) {
            return '<terminal does not have focus>';
          }
          return copiedText(page);
        },
        { timeout: TIMEOUTS.consoleReady },
      )
      .toMatch(new RegExp(`${first}\\s+${second}`));
  });
});

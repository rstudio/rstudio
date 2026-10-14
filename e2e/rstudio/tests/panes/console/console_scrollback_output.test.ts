// https://github.com/rstudio/rstudio/issues/19082
// Exercise real stdout/stderr delivery with a full scrollback, including the
// condition groups and cursor edits that change the visible newline count.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { clearPref, setPref } from '@utils/commands';
import { heredoc } from '@utils/heredoc';

const MAX_LINES = 50;

test.describe('Console output with a full scrollback', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await setPref(page, 'console_max_lines', MAX_LINES);
    await setPref(page, 'console_highlight_conditions', 'errors_warnings_messages');
    await setPref(page, 'ansi_console_mode', 'on');
    await consoleActions.clearConsole();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await clearPref(page, 'console_max_lines');
    await clearPref(page, 'console_highlight_conditions');
    await clearPref(page, 'ansi_console_mode');
    await consoleActions.clearConsole();
  });

  test('interleaved messages and stdout retain the exact newest output', async () => {
    await consoleActions.executeInConsole(heredoc`
      local({
        cat(sprintf("fill-%03d\n", seq_len(100)), sep = "")
        for (i in seq_len(100)) {
          message(sprintf("msg-%03d", i))
          cat(sprintf("out-%03d\n", i))
        }
      })
    `);

    const expected = Array.from({ length: MAX_LINES }, (_, i) => {
      const number = String(101 - MAX_LINES + i).padStart(3, '0');
      return [`msg-${number}`, `out-${number}`];
    }).flat();

    // textContent counts actual newline characters, matching the scrollback
    // limit; innerText also inserts line breaks for the block-styled groups.
    // Numbered markers cannot match the command echo's sprintf templates.
    const output = consoleActions.page.locator('#rstudio_console_output');
    await expect.poll(async () => {
      const text = await output.textContent() ?? '';
      return text.match(/(?:msg|out)-\d{3}/g);
    }).toEqual(expected);
    const text = await output.textContent() ?? '';
    expect(text.match(/\n/g)).toHaveLength(MAX_LINES);
    expect(text).not.toContain('fill-');
  });

  test('a colored cursor edit reaches the correct line after front trimming', async () => {
    await consoleActions.executeInConsole(heredoc`
      local({
        for (i in seq_len(200))
          cat(sprintf("history-%03d %s\n", i, strrep("x", 1536)))
        message(paste0("stream-", "boundary"))
        cat("\033[2F\033[32m", paste0("progress-", "done"),
            "\033[K\033[0m\033[2E", paste0("after-", "edit"), "\n", sep = "")
      })
    `);

    // Switching to stderr flushes the large stdout chunk as a separate
    // console event. Its front is therefore trimmed before the cursor edit,
    // even when the backend delivers all events in a single poll response.
    const output = consoleActions.page.locator('#rstudio_console_output');
    await expect(output).toContainText('after-edit');
    const text = await output.textContent() ?? '';
    expect(text.match(/history-\d{3}/g)).toEqual(
      Array.from({ length: 48 }, (_, i) => `history-${152 + i}`),
    );
    expect(text).toMatch(/history-199[^\n]*\nprogress-done *\nstream-boundaryafter-edit\n$/);
    expect(text.match(/\n/g)).toHaveLength(MAX_LINES);
    await expect(output.locator('.xtermColor2').filter({ hasText: 'progress-done' })).toBeVisible();

    // A subsequent command must still produce output after the edit.
    await consoleActions.executeInConsole('cat("next-", "command\\n", sep = "")');
    await expect(output).toContainText('next-command');
  });

  test('a styled rewrite into discarded scrollback does not interrupt later output', async () => {
    await consoleActions.executeInConsole(heredoc`
      local({
        cat(sprintf("retained-%03d\n", seq_len(100)), sep = "")
        message(paste0("stream-", "boundary"))
        cat("\033[999F\033[31mHID\033[0m\033[999E",
            paste0("surviving-", "output"), "\n", sep = "")
      })
    `);

    // The command echo is already discarded when stderr flushes stdout.
    // Rewriting its first line in red must stay offscreen and must not throw.
    const output = consoleActions.page.locator('#rstudio_console_output');
    await expect(output).toContainText('surviving-output');
    const text = await output.textContent() ?? '';
    expect(text).not.toContain('HID');
    expect(text.match(/retained-\d{3}/g)).toEqual(
      Array.from({ length: 49 }, (_, i) => `retained-${String(52 + i).padStart(3, '0')}`),
    );
    expect(text.match(/\n/g)).toHaveLength(MAX_LINES);
  });
});

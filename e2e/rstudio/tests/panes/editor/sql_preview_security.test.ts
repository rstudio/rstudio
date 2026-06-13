import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { YES_BTN, NO_BTN, CONFIRM_BTN } from '@pages/modals.page';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral } from '@utils/r';
import { heredoc } from '@utils/heredoc';
import { resetSourcePaneState, executeCommand } from '@utils/commands';
import { COMPLETION_POPUP } from '@actions/autocomplete.actions';
import { sleep, TIMEOUTS } from '@utils/constants';

// Regression tests for rstudio-pro#10981: a SQL file's `-- !preview conn=`
// header was passed straight to eval(parse(text = conn)), so opening an
// untrusted .sql file and either triggering autocomplete or clicking Preview
// executed arbitrary R code. The connection expression is now statically
// classified; arbitrary code is never run silently (completions) and requires
// explicit consent (preview).

// An R expression that, if evaluated, writes a sentinel file. It is not on the
// connection allow-list, so it must never run without consent.
function payload(sentinelRPath: string): string {
  return `writeLines("pwned", ${rPathLiteral(sentinelRPath)})`;
}

// Ask the R session whether the sentinel file exists. Checking on the rsession
// host (rather than via Node fs) keeps the test correct in Server mode, where
// the session may live on a different machine than the test runner.
async function sentinelExists(page: import('@playwright/test').Page, sentinelRPath: string): Promise<boolean> {
  const marker = `__SQLSENTINEL_${Date.now()}__`;
  await executeInConsole(
    page,
    `cat("${marker}", if (file.exists(${rPathLiteral(sentinelRPath)})) "EXISTS" else "ABSENT", "${marker}")`,
    { wait: true },
  );
  const output = await page.locator(CONSOLE_OUTPUT).innerText();
  const match = output.match(new RegExp(`${marker}\\s+(EXISTS|ABSENT)\\s+${marker}`));
  if (!match) throw new Error(`sentinelExists: marker not found in console output`);
  return match[1] === 'EXISTS';
}

test.describe('SQL Preview security (rstudio-pro#10981)', () => {
  const sandbox = useSuiteSandbox();

  test.beforeEach(async ({ rstudioPage: page }) => {
    await resetSourcePaneState(page);
    // Clear any per-session preview approvals left by a prior test so each
    // test starts from a clean "nothing is permitted" state.
    await executeInConsole(page, `.rs.preview.permittedExprs$items <- NULL`, { wait: true });
  });

  test('autocomplete does not evaluate the connection expression', async ({ rstudioPage: page }) => {
    const sentinel = `${sandbox.dir}/pwned_autocomplete.txt`;
    const fileName = 'injection_autocomplete.sql';
    const content = heredoc`
      -- !preview conn=${payload(sentinel)}
      SELECT 1
    `;

    await writeAndOpenFile(page, sandbox.dir, fileName, content);

    // Reproduce the reported trigger: request SQL completions in the file.
    // The completion path resolves the connection to offer table/field names,
    // which is exactly where the unsanitized eval used to run.
    await page.evaluate(() => {
      const editor = window.rstudio?.documents.activeEditor() ?? null;
      if (!editor) throw new Error('No active source editor');
      editor.focus();
      editor.gotoLine(2, editor.session.getLine(1).length);
    });
    await sleep(300);
    await page.keyboard.press('Control+Space');

    // Give the completion RPC time to round-trip (it may pop a list of
    // keywords; that is fine -- we only care that the payload did not run).
    await sleep(1000);
    if (await page.locator(COMPLETION_POPUP).isVisible()) {
      await page.keyboard.press('Escape');
    }

    expect(await sentinelExists(page, sentinel)).toBe(false);

    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  test('keyword completions are still offered when the connection is untrusted', async ({ rstudioPage: page }) => {
    const sentinel = `${sandbox.dir}/pwned_completion_keyword.txt`;
    const fileName = 'injection_completion_keyword.sql';
    // Untrusted connection header, plus a partial keyword that matches more
    // than one SQL keyword ('SELECT', 'SET', ...). A prefix with a single
    // candidate would be auto-inserted without ever showing the popup.
    const content = heredoc`
      -- !preview conn=${payload(sentinel)}
      SE
    `;

    await writeAndOpenFile(page, sandbox.dir, fileName, content);

    // Position at the end of the partial keyword and request completions.
    await page.evaluate(() => {
      const editor = window.rstudio?.documents.activeEditor() ?? null;
      if (!editor) throw new Error('No active source editor');
      editor.focus();
      editor.gotoLine(2, editor.session.getLine(1).length);
    });
    await sleep(300);
    await page.keyboard.press('Control+Space');

    // Keyword completions are static -- they never resolve the connection -- so
    // 'SELECT' is offered even though the connection expression is untrusted.
    // The point of the hardening is only that the payload never runs; legible
    // editing (keywords, buffer identifiers) must keep working.
    const popup = page.locator(COMPLETION_POPUP);
    await expect(popup).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(popup).toContainText('SELECT', { timeout: TIMEOUTS.fileOpen });
    await page.keyboard.press('Escape');

    // ...and offering keyword completions did not evaluate the connection.
    expect(await sentinelExists(page, sentinel)).toBe(false);

    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  test('preview prompts for consent and does not run the expression when declined', async ({ rstudioPage: page }) => {
    const sentinel = `${sandbox.dir}/pwned_preview_declined.txt`;
    const fileName = 'injection_preview_declined.sql';
    const content = heredoc`
      -- !preview conn=${payload(sentinel)}
      SELECT 1
    `;

    await writeAndOpenFile(page, sandbox.dir, fileName, content);

    await executeCommand(page, 'previewSql');

    // The backend declines to evaluate the unsafe expression and asks the
    // front-end to confirm; verify the warning dialog and decline it.
    const yesBtn = page.locator(YES_BTN);
    await expect(yesBtn).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await page.locator(NO_BTN).click();

    expect(await sentinelExists(page, sentinel)).toBe(false);

    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  test('preview runs the expression after the user consents', async ({ rstudioPage: page }) => {
    const sentinel = `${sandbox.dir}/pwned_preview_allowed.txt`;
    const fileName = 'injection_preview_allowed.sql';
    const content = heredoc`
      -- !preview conn=${payload(sentinel)}
      SELECT 1
    `;

    await writeAndOpenFile(page, sandbox.dir, fileName, content);

    await executeCommand(page, 'previewSql');

    const yesBtn = page.locator(YES_BTN);
    await expect(yesBtn).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await yesBtn.click();

    // Consenting evaluates the expression (which here writes the sentinel and
    // then fails to produce a real connection, surfacing an error dialog we
    // dismiss). The point is that evaluation now happens only after consent.
    // Server can take longer to surface the error dialog because the
    // expression evaluation round-trips through rsession; use a generous
    // wait rather than the file-open default.
    await expect(page.locator(CONFIRM_BTN)).toBeVisible({ timeout: TIMEOUTS.consoleReady });
    await page.locator(CONFIRM_BTN).click();

    expect(await sentinelExists(page, sentinel)).toBe(true);

    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });
});

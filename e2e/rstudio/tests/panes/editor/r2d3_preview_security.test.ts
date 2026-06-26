import { test, expect } from '@fixtures/rstudio.fixture';
import * as os from 'os';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { YES_BTN, NO_BTN, CONFIRM_BTN } from '@pages/modals.page';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral } from '@utils/r';
import { heredoc } from '@utils/heredoc';
import { resetSourcePaneState, executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';

// Security tests for the r2d3 ('// !preview r2d3 ...') file preview, the
// companion to the SQL preview hardening. Unlike SQL, the built
// 'r2d3::r2d3(...)' call is run verbatim in the console (so r2d3 can render the
// widget), which makes it the more dangerous path: the '!preview' header is
// fully attacker-controlled when the .js/.d3 file comes from an untrusted
// source. The backend classifies the command and requires it to be a single
// statement; arbitrary code must never run without the user's explicit consent.

// A d3 script body. The content is irrelevant to the security checks; we only
// need a valid '// !preview r2d3' header for the Preview command to fire.
const D3_BODY = `var bars = svg.selectAll('rect').data(data);`;

// An R expression that, if evaluated, writes a sentinel file. It is not on the
// preview allow-list, so it must never run without consent.
function payload(sentinelRPath: string): string {
  return `writeLines("pwned", ${rPathLiteral(sentinelRPath)})`;
}

// Ask the R session whether the sentinel file exists (checked on the rsession
// host so the test is correct in Server mode too).
async function sentinelExists(page: import('@playwright/test').Page, sentinelRPath: string): Promise<boolean> {
  const marker = `__D3SENTINEL_${Date.now()}__`;
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

test.describe('r2d3 Preview security', () => {
  const sandbox = useSuiteSandbox();
  let missingR2d3: string[] = [];

  // The Preview command is gated behind a withR2D3() dependency check that pops
  // an "install r2d3?" dialog when the package is absent. Install it up front
  // (via the console, no GUI prompt) so the tests exercise the preview/consent
  // flow rather than the dependency installer; skip if it cannot be installed.
  test.beforeAll(async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    missingR2d3 = await consoleActions.ensurePackages(['r2d3']);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    test.skip(missingR2d3.length > 0, `Missing: ${missingR2d3.join(', ')}`);
    await resetSourcePaneState(page);
    // Clear any per-session preview approvals left by a prior test so each
    // test starts from a clean "nothing is permitted" state.
    await executeInConsole(page, `.rs.preview.permittedExprs$items <- NULL`, { wait: true });
  });

  test('preview blocks multi-statement header injection without running it', async ({ rstudioPage: page }) => {
    const sentinel = `${sandbox.dir}/pwned_r2d3_multi.txt`;
    const fileName = 'injection_r2d3_multi.js';
    // Close the r2d3 call early and append a second statement. The front-end
    // runs the built command verbatim, so without the single-statement guard
    // the classifier would inspect only 'r2d3::r2d3(...)' (safe) while the
    // console ran the trailing payload too.
    const content = heredoc`
      // !preview r2d3 height=300); ${payload(sentinel)}
      ${D3_BODY}
    `;

    await writeAndOpenFile(page, sandbox.dir, fileName, content);

    await executeCommand(page, 'previewJS');

    // The backend rejects the multi-statement command and reports an error;
    // dismiss the message dialog.
    await expect(page.locator(CONFIRM_BTN)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await page.locator(CONFIRM_BTN).click();

    // Crucially, the trailing statement never ran.
    expect(await sentinelExists(page, sentinel)).toBe(false);

    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  test('preview prompts for consent and does not run an unsafe argument when declined', async ({ rstudioPage: page }) => {
    const sentinel = `${sandbox.dir}/pwned_r2d3_declined.txt`;
    const fileName = 'injection_r2d3_declined.js';
    // A single, correctly-shaped r2d3 call whose 'data' argument smuggles in
    // an unsafe expression; this must require consent before running.
    const content = heredoc`
      // !preview r2d3 data=${payload(sentinel)}
      ${D3_BODY}
    `;

    await writeAndOpenFile(page, sandbox.dir, fileName, content);

    await executeCommand(page, 'previewJS');

    // The backend declines to classify the unsafe argument as safe and asks the
    // front-end to confirm; verify the warning dialog and decline it.
    const yesBtn = page.locator(YES_BTN);
    await expect(yesBtn).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await page.locator(NO_BTN).click();

    expect(await sentinelExists(page, sentinel)).toBe(false);

    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  test('preview runs the command in the console after the user consents', async ({ rstudioPage: page }) => {
    // GWT null dereference in compiled JS during the consent + render flow
    // on Windows CI and Linux Server (product bug #18065). Marked fixme
    // rather than skipped so it still runs and a future fix lights it up
    // automatically, keeping the failure visible in reports.
    test.fixme(
      (os.platform() === 'win32' && !!process.env.CI)
        || process.env.PW_RSTUDIO_MODE === 'server',
      'GWT null dereference during r2d3 widget render on Windows CI and Linux Server; see #18065',
    );
    const sentinel = `${sandbox.dir}/pwned_r2d3_allowed.txt`;
    const fileName = 'injection_r2d3_allowed.js';
    // The 'data' argument both smuggles in the (unsafe) sentinel-writing call
    // -- so consent is still required -- and yields a valid numeric vector so
    // the d3 widget actually renders. (A NULL/!vector data makes the d3 script
    // throw, and the IDE's reaction to that error races teardown.)
    const unsafeData = `{ ${payload(sentinel)}; c(0.3, 0.6, 0.8) }`;
    const content = heredoc`
      // !preview r2d3 data=${unsafeData}
      ${D3_BODY}
    `;

    await writeAndOpenFile(page, sandbox.dir, fileName, content);

    await executeCommand(page, 'previewJS');

    const yesBtn = page.locator(YES_BTN);
    await expect(yesBtn).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await yesBtn.click();

    // Consenting runs 'r2d3::r2d3(...)' in the console, which forces the
    // (now-permitted) 'data' argument and writes the sentinel. The point is
    // that evaluation happens only after explicit consent.
    await expect.poll(
      async () => sentinelExists(page, sentinel),
      { timeout: TIMEOUTS.fileOpen },
    ).toBe(true);

    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });
});

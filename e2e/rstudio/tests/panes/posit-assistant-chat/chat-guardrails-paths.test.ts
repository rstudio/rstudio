/**
 * Filesystem Guardrails: paths (#17122)
 *
 * Companion to chat-guardrails.test.ts. That file drives the guardrails
 * via natural-language prompts to the Posit Assistant; here we call
 * .rs.chat.injectBindings() / .rs.chat.restoreBindings() directly from
 * the console so every sensitive read/write path can be covered
 * deterministically. The R-side mechanism is the same; this just
 * removes the assistant from the loop.
 *
 * Tests are grouped by category (read denials, read allows, write
 * denials, write allows, connection denials, error message structure,
 * system file denials, path traversal).
 *
 * Each test calls `.rs.test.guardrails(quote(<expr>))` -- a helper installed
 * in beforeAll that injects the guardrail bindings, evaluates the
 * expression, prints any error message, and finally cats a unique
 * "DONE" marker. The marker is computed at runtime so the JS poll
 * can distinguish R's output from the input echo of the very same
 * command. on.exit restores bindings even when the guarded call stop()s.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { createAndOpenProject } from '@utils/project';

const PROJECT_NAME = 'guardrail_paths_project';

// The helper definition installs `.rs.test.guardrails` in the global
// env. The marker is generated at runtime (proc.time() + sample.int),
// so the JS-side regex never matches the input echo of the call --
// only the cat() output that runs after the guarded expression
// completes.
const GUARDRAILS_HELPER_DEF =
  `.rs.test.guardrails <- function(expr) { ` +
    `on.exit(.rs.chat.restoreBindings(), add = TRUE); ` +
    `.rs.chat.injectBindings(); ` +
    `m <- paste0("__GUARDRAILS_", proc.time()[3], "_", sample.int(1e9, 1L), "__"); ` +
    `tryCatch({ ` +
      `eval(expr, envir = parent.frame()); ` +
      `cat("__OK__", m, "\\n") ` +
    `}, error = function(e) { ` +
      `cat(conditionMessage(e), "\\n"); ` +
      `cat("__ERR__", m, "\\n") ` +
    `}) ` +
  `}`;

// Matches the runtime-generated marker (proc.time() seconds.fraction +
// integer). Used to poll for "R is done with the helper call".
const GUARDRAILS_MARKER_RE = /__GUARDRAILS_[\d.]+_\d+__/;

// Whether the helper's tryCatch caught an error (i.e., the guarded call
// stop()ed). Sentinel is printed by the error branch.
const GUARDRAILS_ERR_RE = /__ERR__\s+__GUARDRAILS_[\d.]+_\d+__/;

test.describe.serial('Filesystem Guardrails: paths (#17122)', { tag: ['@serial'] }, () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  // Forward-slash sandbox path for safe interpolation into R double-quoted strings.
  let sandboxR = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sandboxR = sandbox.dir.replace(/\\/g, '/');

    // Open a project so .rs.getProjectDirectory() is non-null and the
    // sandbox parent is reliably "outside" the allowed locations.
    await createAndOpenProject(page, sandboxR, PROJECT_NAME);

    // The session restart invalidates the previous actions wrapper.
    consoleActions = new ConsolePaneActions(page);

    // Install the .rs.test.guardrails helper once for the suite.
    await consoleActions.executeInConsole(GUARDRAILS_HELPER_DEF);
    await consoleActions.clearConsole();
  });

  /**
   * Run rCode under the guardrail bindings, optionally surrounded by
   * pre/post R statements (used to plant and clean up files outside
   * the guardrail wrap, in a single console submission). Returns the
   * post-call console text and whether the guarded call errored.
   */
  async function runWithGuardrails(
    rCode: string,
    opts: { pre?: string; post?: string } = {},
  ): Promise<{ output: string; blocked: boolean }> {
    const consoleOutput = consoleActions.page.locator('#rstudio_console_output');
    const segments = [
      opts.pre,
      `.rs.test.guardrails(quote(${rCode}))`,
      opts.post,
    ].filter(Boolean) as string[];
    await consoleActions.clearConsole();
    await consoleActions.executeInConsole(segments.join('; '));
    // The DONE marker is printed AFTER the guarded call finishes. Polling
    // for it via Playwright's auto-retry replaces the previous blind sleep.
    // (Any `post` cleanup runs after the marker is printed but before R
    // returns to the prompt -- the next test's clearConsole gives it time.)
    await expect(consoleOutput).toContainText(GUARDRAILS_MARKER_RE, { timeout: 15000 });
    const output = await consoleOutput.innerText();
    return { output, blocked: GUARDRAILS_ERR_RE.test(output) };
  }

  /**
   * Assert that running rCode under the guardrails triggered a stop()
   * whose message matches `pattern` (default: contains "blocked").
   * Returns the output so callers can make further assertions on the
   * error structure.
   */
  async function expectBlocked(
    rCode: string,
    pattern: RegExp = /blocked/i,
    opts: { pre?: string; post?: string } = {},
  ): Promise<string> {
    const { output, blocked } = await runWithGuardrails(rCode, opts);
    expect(blocked).toBe(true);
    expect(output).toMatch(pattern);
    return output;
  }

  /**
   * Assert that running rCode under the guardrails did NOT trigger
   * a stop() (no error, no "blocked" message). Used for paths the
   * guardrails should pass through to the original R implementation.
   */
  async function expectAllowed(
    rCode: string,
    opts: { pre?: string; post?: string } = {},
  ): Promise<void> {
    const { output, blocked } = await runWithGuardrails(rCode, opts);
    expect(blocked).toBe(false);
    expect(output.toLowerCase()).not.toContain('blocked');
  }

  // --- Read denials --------------------------------------------------------

  test('reading ~/.aws/credentials is denied', async () => {
    await expectBlocked(`readLines("~/.aws/credentials")`);
  });

  test('reading ~/.ssh/config is denied', async () => {
    await expectBlocked(`readLines("~/.ssh/config")`);
  });

  test('reading ~/.netrc is denied', async () => {
    await expectBlocked(`readLines("~/.netrc")`);
  });

  test('reading SSH private keys is denied', async () => {
    await expectBlocked(`readLines("~/.ssh/id_rsa")`);
  });

  test('reading .env files is denied', async () => {
    const planted = `file.path(tempdir(), ".env")`;
    await expectBlocked(`readLines(${planted})`, /blocked/i, {
      pre: `writeLines("SECRET=abc", ${planted})`,
      post: `unlink(${planted})`,
    });
  });

  test('reading .Renviron files is denied', async () => {
    const planted = `file.path(tempdir(), ".Renviron")`;
    await expectBlocked(`readLines(${planted})`, /blocked/i, {
      pre: `writeLines("SECRET=abc", ${planted})`,
      post: `unlink(${planted})`,
    });
  });

  test('reading .Rprofile is denied', async () => {
    const planted = `file.path(tempdir(), ".Rprofile")`;
    await expectBlocked(`readLines(${planted})`, /blocked/i, {
      pre: `writeLines("# profile", ${planted})`,
      post: `unlink(${planted})`,
    });
  });

  test('reading .env.local variant is denied', async () => {
    const planted = `file.path(tempdir(), ".env.local")`;
    await expectBlocked(`readLines(${planted})`, /blocked/i, {
      pre: `writeLines("SECRET=abc", ${planted})`,
      post: `unlink(${planted})`,
    });
  });

  // --- Read allows ---------------------------------------------------------

  test('reading SSH public key is allowed', async () => {
    const planted = `file.path(tempdir(), "id_rsa.pub")`;
    await expectAllowed(`readLines(${planted})`, {
      pre: `writeLines("ssh-rsa AAAA...", ${planted})`,
      post: `unlink(${planted})`,
    });
  });

  test('reading normal files is allowed', async () => {
    const planted = `file.path(tempdir(), "guardrail-read.txt")`;
    await expectAllowed(`readLines(${planted})`, {
      pre: `writeLines("hello", ${planted})`,
      post: `unlink(${planted})`,
    });
  });

  // --- Write denials -------------------------------------------------------

  test('writing outside project directory is denied', async () => {
    // sandboxR is the parent of the project dir, so a write there is
    // outside the project, the working dir, tempdir, and scratch path.
    await expectBlocked(`writeLines("x", "${sandboxR}/not-in-project.txt")`);
  });

  test('writing to ~/.ssh is denied', async () => {
    await expectBlocked(`writeLines("x", "~/.ssh/guardrail-test")`);
  });

  test('writing to tempdir is allowed', async () => {
    await expectAllowed(`writeLines("hello", tempfile("guardrail-write-"))`);
  });

  test('file.create outside project directory is denied', async () => {
    await expectBlocked(`file.create("${sandboxR}/not-in-project.txt")`);
  });

  test('file.remove outside project directory is denied', async () => {
    await expectBlocked(`file.remove("${sandboxR}/not-in-project.txt")`);
  });

  test('unlink outside project directory is denied', async () => {
    // Plant the file outside the guardrail wrap so the unlink target
    // actually exists; the guardrail should still block the unlink call.
    // The `unlink` in `post` runs without the guardrail bindings active.
    const target = `${sandboxR}/guardrail-unlink-target.txt`;
    await expectBlocked(`unlink("${target}")`, /blocked/i, {
      pre: `file.create("${target}")`,
      post: `unlink("${target}")`,
    });
  });

  test('file.copy to denied path is denied', async () => {
    const src = `file.path(tempdir(), "guardrail-copy-src.txt")`;
    const dest = `${sandboxR}/guardrail-copy-dest.txt`;
    await expectBlocked(`file.copy(${src}, "${dest}")`, /blocked/i, {
      pre: `writeLines("hello", ${src})`,
      post: `unlink(${src})`,
    });
  });

  test('file.rename to denied path is denied', async () => {
    const src = `file.path(tempdir(), "guardrail-rename-src.txt")`;
    const dest = `${sandboxR}/guardrail-rename-dest.txt`;
    await expectBlocked(`file.rename(${src}, "${dest}")`, /blocked/i, {
      pre: `writeLines("hello", ${src})`,
      post: `unlink(${src})`,
    });
  });

  // --- Connection denials --------------------------------------------------

  test('file() connection to sensitive path is denied', async () => {
    await expectBlocked(`file("~/.aws/credentials", open = "r")`);
  });

  test('file() with deferred open to sensitive path is denied', async () => {
    // Deferred-open file() (no `open` argument) is validated as an edit,
    // which applies both the read and edit deny patterns.
    await expectBlocked(`file("~/.ssh/guardrail-test")`);
  });

  // --- Error message structure --------------------------------------------

  test('read denial error includes action, path, and reason', async () => {
    const output = await expectBlocked(
      `readLines("~/.aws/credentials")`,
      /One or more agent file operations were blocked/,
    );
    expect(output).toMatch(/Action:/);
    expect(output).toMatch(/Path:/);
    expect(output).toMatch(/Reason:/);
    expect(output).toMatch(/secret keys or credentials/);
  });

  test('edit denial error includes reason for path outside allowed locations', async () => {
    const output = await expectBlocked(
      `writeLines("x", "${sandboxR}/not-in-project.txt")`,
      /One or more agent file operations were blocked/,
    );
    expect(output).toMatch(/not within the project/);
  });

  test('edit denial on .ssh path includes credentials reason', async () => {
    const output = await expectBlocked(
      `writeLines("x", "~/.ssh/guardrail-test")`,
      /One or more agent file operations were blocked/,
    );
    expect(output).toMatch(/secret keys or credentials/);
  });

  // --- System file denials -------------------------------------------------

  test('reading /etc/passwd is denied', async () => {
    await expectBlocked(`readLines("/etc/passwd")`, /sensitive system information/);
  });

  test('reading /etc/shadow is denied', async () => {
    await expectBlocked(`readLines("/etc/shadow")`, /sensitive system information/);
  });

  test('writing /etc/passwd is denied', async () => {
    await expectBlocked(`writeLines("x", "/etc/passwd")`, /sensitive system information/);
  });

  // --- Path traversal ------------------------------------------------------

  test(`path traversal with '..' is rejected`, async () => {
    await expectBlocked(
      `writeLines("x", file.path(tempdir(), "sub/../../etc/passwd"))`,
      /unresolved|sensitive/,
    );
  });

  // --- Binding lifecycle (TODOs) -------------------------------------------
  //
  // The four tests below came from test-automation-guardrails.R's "Binding
  // lifecycle" block. They exercise `.rs.chat.safeEval()` and the
  // injectBindings/restoreBindings reentrancy guard. They aren't blocked by
  // a real RStudio bug -- they just need a port that doesn't rely on the
  // standard "blocked" console pattern (safeEval catches the stop() and
  // returns it as a condition, so the message never lands in the console).
  // The path-based tests above already cover the user-observable guardrail
  // behavior; these are internal-API checks. The natural ports below are
  // close to what you'd write -- they were left as `test.fixme` so the
  // next person can verify them end-to-end before un-fixme-ing.

  test.fixme('bindings are restored after safeEval', async () => {
    // Sketch: .rs.chat.safeEval(quote(1 + 1)), then a write+unlink inside
    // tempdir should NOT produce a "blocked" message. Try:
    //   await consoleActions.executeInConsole('.rs.chat.safeEval(quote(1 + 1))');
    //   await consoleActions.executeInConsole(
    //     'p <- file.path(tempdir(), "lifecycle.txt"); writeLines("x", p); unlink(p)'
    //   );
    //   // wait for a runtime marker, then assert no "blocked" in console
  });

  test.fixme('safeEval blocks writes outside project directory', async () => {
    // Sketch: .rs.chat.safeEval(quote(writeLines("x", dirname(tempdir())/...)))
    // and verify the file was NOT created. safeEval swallows the error, so
    // the assertion is on file.exists(), not on console output.
  });

  test.fixme('double injection is safe (reentrancy guard)', async () => {
    // Sketch: call .rs.chat.injectBindings() twice, then restoreBindings()
    // once. A normal write in tempdir afterwards should NOT be blocked --
    // verifies that the second inject is a no-op and the bindings come
    // back cleanly.
  });

  test.fixme('safeEval restores bindings when user code errors', async () => {
    // Sketch: .rs.chat.safeEval(quote(stop("user error"))), then verify a
    // normal write in tempdir works (i.e. bindings were restored even
    // though the user code stop()ed inside safeEval).
  });
});

import type { Page } from '@playwright/test';
import * as path from 'path';
import { test } from '../fixtures/rstudio.fixture';
import { executeInConsole, CONSOLE_OUTPUT, waitForConsoleIdle } from '../pages/console_pane.page';
import { sleep, TIMEOUTS } from './constants';
import { assertAbsolutePath } from './paths';

/**
 * Prefix for per-suite R workdir subdirectories created inside the sandbox.
 * Used by createSandbox() to create the workdir and by the two project specs
 * that detect leftover sandbox paths in the saved default-project-location pref
 * (see tests/projects/create_projects.test.ts and project_trust_dialog.test.ts).
 * Exported so the prefix is a single source of truth: if it changes, the specs
 * that compare against it pick up the new value automatically.
 */
export const SANDBOX_DIR_PREFIX = 'workdir_';

/**
 * Per-spec R-side sandbox directory for test artifacts.
 *
 * Creates a unique subdirectory under the per-invocation sandbox root and
 * `setwd()`s into it. The sandbox root is set by fixtures/sandbox-setup.ts
 * via the PW_SANDBOX env var before any worker spawns; this module just
 * reads it. Cleanup of the entire sandbox subtree happens in
 * fixtures/sandbox-teardown.ts at end of run.
 */
function rootExpr(): string {
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) {
    throw new Error(
      'PW_SANDBOX is not set; sandbox-setup.ts should have populated it before any test code runs',
    );
  }
  // Use the runner-side PW_SANDBOX path when it exists on the rsession host
  // (Desktop, or Server pointed at localhost). Fall back to R's own tempdir
  // parent when the rsession runs on a remote host where the runner-side
  // path doesn't exist. The remote-host workdir isn't covered by globalTeardown.
  const lit = JSON.stringify(sandbox);
  return `(function() { p <- path.expand(${lit}); if (dir.exists(p)) p else dirname(tempdir()) })()`;
}

/**
 * Create a fresh workdir subdirectory via R and return its absolute path.
 * The R session's working directory is set to the new subdirectory.
 */
export async function createSandbox(page: Page): Promise<string> {
  const marker = `__SANDBOX_${Date.now()}__`;

  // Gate on R actually being ready to receive input before submitting the
  // marker command. Without this, a createSandbox called right after a
  // session restart (project open/close, restart-R, or just the initial
  // page load) lands while R's startup banner is still rendering -- R
  // drops or queues the input and our cat(marker) is never echoed back,
  // surfacing as an opaque "marker not found within 15000ms" timeout.
  // window.rstudio.ready flips to true on DeferredInitCompletedEvent
  // (workspace restored, deferred-init modules sourced); waitForConsoleIdle
  // then confirms the console isn't mid-execution from anything else. Both
  // checks are no-ops in the common case (the IDE is already ready by the
  // time a suite's beforeAll runs), so this only pays cost on the race.
  await page.waitForFunction(
    () => window.rstudio?.ready === true,
    null,
    { timeout: 30000, polling: 50 },
  ).catch(() => {
    // Bridge not in scope yet -- let the marker poll below surface any
    // real failure rather than blocking the suite on the readiness wait.
  });
  await waitForConsoleIdle(page);

  // Build the R expression on a single line so executeInConsole's single
  // press('Enter') executes it atomically.
  //
  // normalizePath(winslash = "/") forces forward slashes: on Windows tempfile()
  // yields a backslash path (the tmpdir root comes from PW_SANDBOX, a Node
  // path), and backslashes are escape sequences in an R string literal -- so
  // embedding the returned dir into generated R code (e.g.
  // writeLines(..., "<dir>/foo")) mangles it or hard-errors (\p, \w are
  // unrecognized escapes; see #17985). R accepts forward slashes on every
  // platform, so this is the safe canonical form for callers to interpolate.
  // (Defense in depth: callers should still pass paths through rPathLiteral.)
  const rCode = [
    `{ root <- ${rootExpr()}`,
    `d <- tempfile("${SANDBOX_DIR_PREFIX}", tmpdir = root)`,
    `dir.create(d, recursive = TRUE)`,
    `d <- normalizePath(d, winslash = "/", mustWork = FALSE)`,
    `setwd(d)`,
    `cat("${marker}", d, "${marker}") }`,
  ].join('; ');

  await executeInConsole(page, rCode);

  const pattern = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`);
  const start = Date.now();
  while (Date.now() - start < TIMEOUTS.consoleReady) {
    await sleep(TIMEOUTS.pollInterval);
    const output = await page.locator(CONSOLE_OUTPUT).innerText();
    const match = output.match(pattern);
    if (match) {
      const dir = match[1].trim();
      // Fail loudly if R handed back something unusable (empty / relative)
      // rather than letting it flow downstream into a root-relative path.
      assertAbsolutePath(dir, 'createSandbox: R returned workdir');
      // If the workdir's parent isn't PW_SANDBOX, the adaptive rootExpr()
      // chose the dirname(tempdir()) fallback on the rsession host -- meaning
      // PW_SANDBOX doesn't exist there (remote-rsession Server mode). Surface
      // that explicitly so it's clear the workdir won't be auto-cleaned.
      const sandbox = process.env.PW_SANDBOX;
      const normalize = (p: string) => p.replace(/\\/g, '/');
      if (sandbox && !normalize(dir).startsWith(normalize(sandbox) + '/')) {
        console.warn(
          `[sandbox] R workdir ${dir} is not under PW_SANDBOX (${sandbox}); rsession appears to be on a different host. Workdir will not be auto-cleaned.`,
        );
      }
      return dir;
    }
  }
  // Marker timeout. Surface the underlying R error if one is in the buffer,
  // otherwise echo the console tail and the resolved root expression so the
  // failure is actionable. R formats errors as `Error in <call> :\n  <message>`,
  // so capture indented continuation lines along with the header.
  const tail = await page.locator(CONSOLE_OUTPUT).innerText();
  const errMatch = tail.match(/(?:Error in|Error:)[^\n]*(?:\n[ \t]+[^\n]*)*/);
  const detail = errMatch
    ? `R error: ${errMatch[0].replace(/\n+/g, ' | ').trim()}`
    : `console tail: ${tail.slice(-300).replace(/\n+/g, ' | ').trim()}`;
  throw new Error(
    `createSandbox: marker not found within ${TIMEOUTS.consoleReady}ms\n` +
    `  root expr: ${rootExpr()}\n` +
    `  ${detail}`,
  );
}

/**
 * Register a per-suite `beforeAll` hook that creates a workdir inside the
 * sandbox. Returns a ref whose `dir` field is populated before any test in
 * the describe block runs. No afterAll cleanup -- the entire sandbox is
 * removed by globalTeardown at end of run.
 *
 * Usage:
 *   const sandbox = useSuiteSandbox();
 *   test('...', async ({ rstudioPage }) => {
 *     await executeInConsole(rstudioPage, `writeLines("x", "${sandbox.dir}/foo.txt")`);
 *   });
 */
export function useSuiteSandbox(): { dir: string } {
  let value = '';
  let warnedEmpty = false;
  const ref = {
    get dir(): string {
      // Instrumentation: a read while still empty means the path built from it
      // will be rooted at "/". Surface the first such read (with a stack so we
      // can see the caller) instead of silently producing a bad path. The
      // downstream assertAbsolutePath guards still throw; this just pins which
      // read raced the beforeAll.
      if (value === '' && !warnedEmpty) {
        warnedEmpty = true;
        console.warn(
          '[sandbox] useSuiteSandbox: sandbox.dir read while still empty -- ' +
          "the suite's sandbox beforeAll has not populated it (check beforeAll " +
          'ordering / a failed createSandbox). Stack:\n' +
          (new Error().stack ?? '(no stack)'),
        );
      }
      return value;
    },
    set dir(next: string) {
      value = next;
    },
  };
  test.beforeAll(async ({ rstudioPage }) => {
    ref.dir = await createSandbox(rstudioPage);
  });
  return ref;
}

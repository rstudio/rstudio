import type { Page } from '@playwright/test';
import { test } from '../fixtures/rstudio.fixture';
import { typeInConsole, CONSOLE_OUTPUT } from '../pages/console_pane.page';
import { sleep, TIMEOUTS } from './constants';

/**
 * Prefix for per-suite R workdir subdirectories created inside the sandbox.
 * Used by createSandbox() to mint the directory and by the two project specs
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
  return `path.expand(${JSON.stringify(sandbox)})`;
}

/**
 * Create a fresh workdir subdirectory via R and return its absolute path.
 * The R session's working directory is set to the new subdirectory.
 */
export async function createSandbox(page: Page): Promise<string> {
  const marker = `__SANDBOX_${Date.now()}__`;

  // Build the R expression on a single line so typeInConsole's single
  // press('Enter') executes it atomically.
  const rCode = [
    `{ root <- ${rootExpr()}`,
    `d <- tempfile("${SANDBOX_DIR_PREFIX}", tmpdir = root)`,
    `dir.create(d, recursive = TRUE)`,
    `setwd(d)`,
    `cat("${marker}", d, "${marker}") }`,
  ].join('; ');

  await typeInConsole(page, rCode);

  const pattern = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`);
  const start = Date.now();
  while (Date.now() - start < TIMEOUTS.consoleReady) {
    await sleep(TIMEOUTS.pollInterval);
    const output = await page.locator(CONSOLE_OUTPUT).innerText();
    const match = output.match(pattern);
    if (match) {
      const dir = match[1].trim();
      console.log(`Sandbox: ${dir}`);
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
 *     await typeInConsole(rstudioPage, `writeLines("x", "${sandbox.dir}/foo.txt")`);
 *   });
 */
export function useSuiteSandbox(): { dir: string } {
  const ref = { dir: '' };
  test.beforeAll(async ({ rstudioPage }) => {
    ref.dir = await createSandbox(rstudioPage);
  });
  return ref;
}

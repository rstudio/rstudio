import type { Page } from '@playwright/test';
import { test } from '../fixtures/rstudio.fixture';
import { typeInConsole, CONSOLE_OUTPUT } from '../pages/console_pane.page';
import { sleep, TIMEOUTS } from './constants';

/**
 * Per-spec R-side sandbox directory for test artifacts.
 *
 * Creates a unique subdirectory on whichever machine R is running on (local
 * in Desktop mode, remote in Server mode) and `setwd()`s into it. Root
 * defaults to the OS temp parent (`dirname(tempdir())`), matching BRAT's
 * convention. Override with the env vars documented below.
 *
 * Env vars:
 *   RSTUDIO_PW_SANDBOX         Override the sandbox root. Unset uses OS temp parent.
 *   RSTUDIO_PW_SANDBOX_CREATE  When "true"/"1", create an overridden root if missing.
 *                              Default false — fail loud on typos.
 */

const SANDBOX_ROOT_ENV = 'RSTUDIO_PW_SANDBOX';
const SANDBOX_CREATE_ENV = 'RSTUDIO_PW_SANDBOX_CREATE';

function shouldCreateRoot(): boolean {
  const v = process.env[SANDBOX_CREATE_ENV];
  return v === 'true' || v === '1';
}

function rootExpr(): string {
  const override = process.env[SANDBOX_ROOT_ENV];
  return override
    ? `path.expand(${JSON.stringify(override)})`
    : `dirname(tempdir())`;
}

/**
 * Create a fresh sandbox subdirectory via R and return its absolute path.
 * The R session's working directory is set to the new subdirectory.
 */
export async function createSandbox(page: Page): Promise<string> {
  const marker = `__SANDBOX_${Date.now()}__`;
  const createRoot = shouldCreateRoot();

  // Build the R expression on a single line so typeInConsole's single
  // press('Enter') executes it atomically.
  const rCode = [
    `{ root <- ${rootExpr()}`,
    `if (!dir.exists(root)) { if (${createRoot ? 'TRUE' : 'FALSE'}) dir.create(root, recursive = TRUE) else stop(sprintf("Sandbox root does not exist: %s", root)) }`,
    `d <- tempfile("rstudio_pw_", tmpdir = root)`,
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
  throw new Error(`createSandbox: marker not found within ${TIMEOUTS.consoleReady}ms`);
}

/**
 * Remove a sandbox directory. Moves R's cwd to `~` first so Windows can
 * actually release the directory handle — `unlink` on cwd silently no-ops
 * on Windows otherwise.
 */
export async function removeSandbox(page: Page, dir: string): Promise<void> {
  if (!dir) return;
  const escaped = dir.replace(/\\/g, '/');
  await typeInConsole(page, `setwd("~"); unlink("${escaped}", recursive = TRUE)`);
  await sleep(TIMEOUTS.pollInterval);
}

/**
 * Register per-spec `beforeAll`/`afterAll` hooks that create and remove a
 * sandbox directory. Returns a ref whose `dir` field is populated before
 * any test in the describe block runs.
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
  test.afterAll(async ({ rstudioPage }) => {
    await removeSandbox(rstudioPage, ref.dir);
    ref.dir = '';
  });
  return ref;
}

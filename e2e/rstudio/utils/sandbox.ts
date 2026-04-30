import type { Page } from '@playwright/test';
import { test } from '../fixtures/rstudio.fixture';
import { typeInConsole, CONSOLE_INPUT, CONSOLE_OUTPUT } from '../pages/console_pane.page';
import { sleep, TIMEOUTS } from './constants';

const PALETTE_LIST = '#rstudio_command_palette_list';
const CLOSE_PROJECT_LABEL = 'Close Current Project';

/**
 * Prefix for per-suite sandbox subdirectories. Exported so specs that need
 * to detect leftover sandbox paths from a crashed prior run (see
 * `create_projects.test.ts` and `project_trust_dialog.test.ts`) can match
 * against the same value.
 */
export const SANDBOX_DIR_PREFIX = 'pw_rstudio_';

/**
 * Per-spec R-side sandbox directory for test artifacts.
 *
 * Creates a unique subdirectory on whichever machine R is running on (local
 * in Desktop mode, remote in Server mode) and `setwd()`s into it. Root
 * defaults to the OS temp parent (`dirname(tempdir())`), matching BRAT's
 * convention. Override with the env vars documented below.
 *
 * Env vars:
 *   PW_SANDBOX         Override the sandbox root. Unset uses OS temp parent.
 *   PW_SANDBOX_CREATE  When "true"/"1", create an overridden root if missing.
 *                      Default false — fail loud on typos.
 */

const SANDBOX_ROOT_ENV = 'PW_SANDBOX';
const SANDBOX_CREATE_ENV = 'PW_SANDBOX_CREATE';

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
  // Marker timeout. Surface the underlying R error if one is in the buffer
  // (e.g., the override root doesn't exist), otherwise echo the console tail
  // and the resolved root expression so the failure is actionable. R formats
  // errors as `Error in <call> :\n  <message>`, so capture indented
  // continuation lines along with the header.
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
 * Probe an R expression via cat() with markers and return the captured value,
 * or null if the markers don't appear within `timeoutMs`.
 */
async function captureCat(page: Page, rExpression: string, timeoutMs: number): Promise<string | null> {
  const marker = `__SBX_${Date.now()}_${Math.random().toString(36).slice(2, 8)}__`;
  await typeInConsole(page, `cat("${marker}", ${rExpression}, "${marker}")`);
  const pattern = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`);
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    await sleep(TIMEOUTS.pollInterval);
    const output = await page.locator(CONSOLE_OUTPUT).innerText();
    const match = output.match(pattern);
    if (match) return match[1].trim();
  }
  return null;
}

/**
 * Best-effort wait for the R session to become responsive after a navigation
 * or restart. Caller is in afterAll cleanup, so we never throw — failures
 * fall through and the caller decides how to proceed.
 */
async function waitForRSessionIdle(page: Page): Promise<void> {
  // Server reloads via navigation; Desktop reloads in place — best-effort settle.
  await page.waitForLoadState('load', { timeout: TIMEOUTS.sessionRestart }).catch(() => {});
  await sleep(3000);
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: TIMEOUTS.sessionRestart * 2 }).catch(() => {});
  await sleep(2000);
  for (let attempt = 0; attempt < 3; attempt++) {
    const result = await captureCat(page, '"ready"', 5000).catch(() => null);
    if (result === 'ready') return;
    await sleep(2000);
  }
}

/**
 * Close any active project before sandbox teardown. RStudio holds file
 * handles inside `.Rproj.user/` while a project is open, which on Windows
 * causes `unlink(recursive = TRUE)` to partial-delete and leave a tree
 * behind in the OS temp dir. Closing first releases those handles.
 *
 * Best-effort: probe failure or close failure logs a warning and returns
 * so the unlink still runs.
 */
async function closeActiveProjectIfOpen(page: Page): Promise<void> {
  const status = await captureCat(
    page,
    'if (is.null(rstudioapi::getActiveProject())) "NONE" else "OPEN"',
    5000,
  ).catch((err: unknown) => {
    console.warn(`closeActiveProjectIfOpen: probe failed (${err}); skipping close`);
    return null;
  });
  if (status !== 'OPEN') return;

  try {
    // executeCommand("closeProject") blocks the R thread on some platforms,
    // so drive the command palette instead — same approach as the project
    // specs use.
    await page.keyboard.press('ControlOrMeta+Shift+p');
    await sleep(1000);
    await page.keyboard.type(CLOSE_PROJECT_LABEL);
    await sleep(500);
    const item = page.locator(`${PALETTE_LIST} >> text=${CLOSE_PROJECT_LABEL}`);
    await item.waitFor({ state: 'visible', timeout: 5000 });
    await item.click();
    await waitForRSessionIdle(page);
  } catch (err) {
    console.warn(`closeActiveProjectIfOpen: close failed (${err}); sandbox may leak on Windows`);
  }
}

/**
 * Remove a sandbox directory. Closes any active project first so RStudio
 * releases handles inside the tree (otherwise Windows leaves files behind),
 * then moves R's cwd to `~` so `unlink` can remove the directory itself.
 */
export async function removeSandbox(page: Page, dir: string): Promise<void> {
  if (!dir) return;
  await closeActiveProjectIfOpen(page);
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

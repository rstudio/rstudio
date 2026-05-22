import type { Page } from 'playwright';
import type { Locator } from 'playwright';
import { PageObject } from './page_object_base_classes';
import { sleep, TIMEOUTS } from '../utils/constants';
import { documentCloseAllNoSave, executeCommand, getVersion } from '../utils/commands';
import { AceEditorElement } from '../utils/ace';

// ---------------------------------------------------------------------------
// Class-based page object
// ---------------------------------------------------------------------------

export class ConsolePane extends PageObject {
  public consoleInput: Locator;
  public consoleTab: Locator;
  public consoleOutput: Locator;
  public interruptRBtn: Locator;
  public tracebackBtn: Locator;
  public stackTrace: Locator;
  public findBar: Locator;
  public findInput: Locator;
  public findNext: Locator;
  public findClose: Locator;
  public findBtn: Locator;
  public findCaseSensitive: Locator;

  constructor(page: Page) {
    super(page);
    this.consoleInput = page.locator('#rstudio_console_input .ace_text-input');
    this.consoleTab = page.locator('#rstudio_workbench_tab_console');
    this.consoleOutput = page.locator('#rstudio_workbench_panel_console');
    this.interruptRBtn = page.locator("[id^='rstudio_tb_interruptr']");
    this.tracebackBtn = page.locator("[class*='show_traceback_text']");
    this.stackTrace = page.locator("[class*='stack_trace']");

    // Find in Console: #rstudio_find_replace_bar is the inner panel;
    // the Close button is a sibling at the shelf level, so scope it at the console panel.
    const consolePanel = page.locator('#rstudio_workbench_panel_console');
    this.findBar = consolePanel.locator('#rstudio_find_replace_bar');
    this.findInput = this.findBar.locator('input[type="text"]');
    this.findNext = this.findBar.getByRole('button', { name: 'Next' });
    this.findClose = consolePanel.getByRole('button', { name: 'Close' }).first();
    this.findBtn = consolePanel.locator('button[aria-label^="Find in Console"]').first();
    this.findCaseSensitive = this.findBar.getByRole('checkbox', { name: 'Case sensitive' });
  }

  async consoleInputValue(): Promise<string> {
    return this.page.evaluate(() => {
      const el = document.getElementById('rstudio_console_input') as AceEditorElement | null;
      return el?.env?.editor?.getValue() ?? '';
    });
  }
}

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface EnvironmentVersions {
  r: string;
  rstudio: string;
}

// ---------------------------------------------------------------------------
// Backward-compatible exports (used by other tests & desktop.fixture.ts)
// ---------------------------------------------------------------------------

export const CONSOLE_INPUT = '#rstudio_console_input .ace_text-input';
export const CONSOLE_TAB = '#rstudio_workbench_tab_console';
export const CONSOLE_OUTPUT = '#rstudio_workbench_panel_console';
export const INTERRUPT_R_BTN = "[id^='rstudio_tb_interruptr']";

/**
 * Wait for the console input to clear its "busy" class -- i.e. for R to
 * finish processing whatever it's running. Polls the DOM at 100ms intervals.
 * Default timeout is generous (TIMEOUTS.sessionRestart) so it covers project
 * opens and session restarts; pass a smaller value for snappier per-command
 * waits, or a larger one for long-running calls like `install.packages`.
 */
export async function waitForConsoleIdle(
  page: Page,
  timeout: number = TIMEOUTS.sessionRestart,
): Promise<void> {
  await page.waitForFunction(
    () => {
      const el = document.getElementById('rstudio_console_input');
      return !!el && !el.classList.contains('rstudio-console-busy');
    },
    null,
    { timeout, polling: 100 },
  );
}

/** Options accepted by `executeInConsole`. */
export interface ExecuteInConsoleOptions {
  /**
   * If true, wait for R to finish processing the command before returning
   * (via `waitForConsoleIdle`). Defaults to false to preserve the
   * fire-and-forget semantics callers may rely on (e.g. queuing an install
   * then a marker command). Use `wait: true` when the next step depends on
   * R having completed -- it's faster and more reliable than a blind sleep.
   */
  wait?: boolean;
  /**
   * Timeout for the `wait: true` poll, in ms. Defaults to
   * `TIMEOUTS.sessionRestart`. Raise this for slow commands like
   * `install.packages`.
   */
  timeout?: number;
}

/**
 * Submit an R expression to the console. Writes the text directly into the
 * console's Ace editor and presses Enter -- no per-key typing -- so it doesn't
 * race with autocomplete popups, tooltip handlers, or other live-edit UI that
 * can swallow characters or steal focus. Prefer this when you just need code
 * to run; use `typeInConsole` only when a test is exercising actual typing
 * behavior (e.g. autocomplete triggering).
 *
 * Pass `{ wait: true }` when the next step depends on R having finished the
 * command -- it polls the console-busy class instead of sleeping.
 */
export async function executeInConsole(
  page: Page,
  command: string,
  opts: ExecuteInConsoleOptions = {},
): Promise<void> {
  await page.locator(CONSOLE_TAB).click();
  await page.evaluate((text) => {
    const el = document.getElementById('rstudio_console_input') as AceEditorElement | null;
    const editor = el?.env?.editor;
    if (!editor) throw new Error('Console Ace editor not found at #rstudio_console_input');
    editor.setValue(text, 1); // 1 = move cursor to end
    editor.focus();
  }, command);
  // Defensive: a popup may have been left open by previous interaction.
  if (await page.locator('#rstudio_popup_completions').isVisible()) {
    await page.keyboard.press('Escape');
  }
  // Press Enter on the console-input textarea explicitly. `page.keyboard.press`
  // delivers to the focused element; relying on editor.focus() above is racy --
  // focus can shift between the evaluate() returning and the key press, leaving
  // the text in the buffer but never submitted.
  await page.locator(CONSOLE_INPUT).press('Enter');
  if (opts.wait) {
    await waitForConsoleIdle(page, opts.timeout);
  }
}

/**
 * Simulate user typing one keystroke at a time into the console input. Does
 * NOT press Enter -- the caller controls submission. Use this only when a
 * test needs to exercise live-edit behavior that `executeInConsole`'s
 * programmatic write doesn't trigger (e.g. autocomplete popups, parameter
 * tooltips).
 *
 * `delayMs` is the per-keystroke delay; default 50ms is close to typical
 * human typing speed and gives the editor time to dispatch input events and
 * fire completers between chars.
 */
/** Default CRAN mirror for `ensurePackageInstalled`. */
export const DEFAULT_CRAN_REPOS = 'https://cran.r-project.org';

/** Options accepted by `ensurePackageInstalled`. */
export interface EnsurePackageInstalledOptions {
  /** Repos URL; defaults to `DEFAULT_CRAN_REPOS`. */
  repos?: string;
  /** Install type ("binary" / "source"); defaults to `getOption("pkgType")`. */
  type?: string;
  /** Total timeout for the install, ms. Defaults to 120000 (2 min). */
  timeout?: number;
}

/**
 * Install an R package only if it isn't already on disk. Wraps the R-side
 * `.rs.ensurePackageInstalled` helper, which checks the library before
 * downloading -- much faster than a blind `install.packages` when the package
 * is already present, which is the common case on repeated test runs.
 *
 * `repos` defaults to CRAN. Override it (e.g. with Posit Public Package
 * Manager's per-distro endpoint) for faster Linux installs.
 *
 * Don't use this in tests that are exercising `install.packages` behavior
 * itself -- call `executeInConsole(page, 'install.packages(...)', { wait: true })`
 * directly there.
 */
export async function ensurePackageInstalled(
  page: Page,
  packageName: string,
  opts: EnsurePackageInstalledOptions = {},
): Promise<void> {
  const repos = opts.repos ?? DEFAULT_CRAN_REPOS;
  const args: string[] = [
    JSON.stringify(packageName),
    `repos = ${JSON.stringify(repos)}`,
  ];
  if (opts.type !== undefined) args.push(`type = ${JSON.stringify(opts.type)}`);
  await executeInConsole(
    page,
    `.rs.ensurePackageInstalled(${args.join(', ')})`,
    { wait: true, timeout: opts.timeout ?? 120000 },
  );
}

export async function typeInConsole(page: Page, text: string, delayMs: number = 50): Promise<void> {
  await page.locator(CONSOLE_TAB).click();
  await page.locator(CONSOLE_INPUT).click({ force: true });
  await sleep(300);
  await page.locator(CONSOLE_INPUT).pressSequentially(text, { delay: delayMs });
}

export async function clearConsole(page: Page): Promise<void> {
  await page.locator(CONSOLE_TAB).click();
  await page.locator(CONSOLE_INPUT).click({ force: true });
  await sleep(200);
  await page.keyboard.press('Control+l');
  await sleep(500);
}

export async function closeAllBuffersWithoutSaving(page: Page): Promise<void> {
  await documentCloseAllNoSave(page);
  await sleep(1000);
}

export async function getEnvironmentVersions(page: Page): Promise<EnvironmentVersions> {
  return getVersion(page);
}

export async function goToLine(page: Page, line: number): Promise<void> {
  await executeCommand(page, 'goToLine');
  await sleep(500);
  await page.keyboard.type(String(line));
  await page.keyboard.press('Enter');
  await sleep(500);
}


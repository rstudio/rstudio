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

/**
 * Read the automation agent's monotonic console prompt counter, or null if the
 * running binary predates it (`window.rstudio.console.promptCount`, added in
 * ApplicationAutomation). The counter advances by one each time R issues a
 * console prompt awaiting fresh client input -- a submitted command either
 * completing (top-level prompt) or settling at an interactive sub-prompt
 * (browser()/readline()/menu()) -- so it is a race-free completion signal,
 * unlike the busy CSS class which is sampled (and can be read stale in the
 * submit->busy gap, or miss a fast command's busy flash). See
 * waitForConsoleCommandComplete.
 */
export async function getConsolePromptCount(page: Page): Promise<number | null> {
  return page.evaluate(() => {
    const count = window.rstudio?.console?.promptCount;
    return typeof count === 'number' ? count : null;
  });
}

/** Set once we've warned about a missing prompt counter, to avoid log spam. */
let warnedMissingPromptCount = false;

/**
 * Wait for a console command to finish, given the prompt count captured *before*
 * it was submitted (via getConsolePromptCount). Resolves once the count exceeds
 * `promptCountBefore`, i.e. R issued a new prompt awaiting client input. This is
 * the reliable replacement for waitForConsoleIdle when bracketing a submission:
 * it keys off the prompt *event*, not a sampled class, so it can't return
 * spuriously-idle before the command starts.
 *
 * Every new prompt event advances the counter, but a single submission only
 * produces one: the server services a multi-line/multi-statement submission's
 * intermediate continuation (`+`) and inter-statement prompts from its buffered
 * input without firing a client event (see ApplicationAutomation.registerConsole
 * for the full rationale). So this resolves when the command completes (back to
 * the top-level prompt) OR when it settles at an interactive sub-prompt that
 * needs the test to act next -- `browser()`/Browse[N]>, readline(), menu(). The
 * latter is what lets debugger tests submit a breakpoint-triggering call and
 * proceed once R parks at the Browse prompt, rather than hanging until timeout.
 *
 * Falls back to the busy-class wait when the counter is unavailable (a binary
 * built before the counter, or one where registerConsole failed to install it),
 * preserving prior behavior; warns once so a silent revert to the flaky path is
 * visible in the log.
 */
export async function waitForConsoleCommandComplete(
  page: Page,
  promptCountBefore: number | null,
  timeout: number = TIMEOUTS.sessionRestart,
): Promise<void> {
  if (promptCountBefore === null) {
    if (!warnedMissingPromptCount) {
      warnedMissingPromptCount = true;
      console.warn(
        '[console] window.rstudio.console.promptCount unavailable; falling back ' +
        'to the busy-class wait (waitForConsoleIdle). This is the known-flaky ' +
        'path -- expected only against a binary that predates the counter.',
      );
    }
    await waitForConsoleIdle(page, timeout);
    return;
  }

  await page.waitForFunction(
    (before) => {
      const count = window.rstudio?.console?.promptCount;
      return typeof count === 'number' && count > before;
    },
    promptCountBefore,
    { timeout, polling: 50 },
  );
}

/**
 * Wait for the console input to gain its "busy" class -- i.e. for R to start
 * processing submitted code. Pairs with waitForConsoleIdle to bracket an
 * asynchronously-dispatched job (e.g. executeCurrentChunk) whose work doesn't
 * begin on the same tick: wait for busy first so a following waitForConsoleIdle
 * can't observe a spuriously-idle console and return before the job has even
 * started. Callers should tolerate a job fast enough that the busy class is
 * never observed (catch the timeout) -- waitForConsoleIdle is then already
 * satisfied. Polls the DOM at 50ms intervals.
 */
export async function waitForConsoleBusy(page: Page, timeout: number = 2000): Promise<void> {
  await page.waitForFunction(
    () => {
      const el = document.getElementById('rstudio_console_input');
      return !!el && el.classList.contains('rstudio-console-busy');
    },
    null,
    { timeout, polling: 50 },
  );
}

/**
 * Wait until the console input's Ace editor owns the document focus.
 * `activateConsole` schedules the focus shift on the next event-loop tick,
 * so callers that follow it with keystrokes can race the focus change.
 * Polling beats a blind sleep -- the common case settles in tens of
 * milliseconds.
 */
export async function waitForConsoleFocus(page: Page, timeout: number = 5000): Promise<void> {
  await page.waitForFunction(
    () => {
      const el = document.getElementById('rstudio_console_input');
      return el !== null && el.contains(document.activeElement);
    },
    null,
    { timeout, polling: 50 },
  );
}

/**
 * Focus the console input, re-issuing `activateConsole` until focus sticks.
 *
 * A single activateConsole + waitForConsoleFocus is not enough: other UI can
 * take focus *after* the command has run and nothing gives it back, so the
 * wait can never succeed. The known offender is the Posit Assistant pane,
 * whose iframe reload (e.g. the workspace-trust prompt shown after a project
 * open) grabs document focus asynchronously. Re-issuing the command inside
 * the poll loop wins that race deterministically.
 *
 * Use this on "just get me a focused console" paths. Tests that assert the
 * focus behavior of a single activateConsole dispatch should keep using
 * executeCommand + waitForConsoleFocus so a product regression isn't masked
 * by the retry.
 */
export async function focusConsole(page: Page, timeout: number = 10000): Promise<void> {
  const deadline = Date.now() + timeout;
  let attempts = 0;

  for (;;) {
    attempts++;
    await executeCommand(page, 'activateConsole');

    const remaining = deadline - Date.now();
    try {
      await waitForConsoleFocus(page, Math.min(1000, Math.max(remaining, 100)));
      return;
    } catch (err) {
      if (Date.now() >= deadline) {
        throw new Error(
          `focusConsole: console input did not hold focus after ${attempts} ` +
            `activateConsole dispatches over ${timeout}ms; something is ` +
            `stealing focus (e.g. an assistant-pane iframe load or a modal)`,
          { cause: err },
        );
      }
    }
  }
}

/** Options accepted by `executeInConsole`. */
export interface ExecuteInConsoleOptions {
  /**
   * Whether to wait for R to finish processing the command before returning
   * (via `waitForConsoleCommandComplete`, which keys off a new top-level
   * prompt). Defaults to `true`: most callers expect the command to have fully
   * executed before the next step runs. It also matters for correctness -- a
   * command is only added to recall history when R *executes* it, so firing
   * several commands without waiting leaves the later ones queued (not yet in
   * history) and makes history navigation nondeterministic.
   *
   * Pass `wait: false` for the fire-and-forget pattern: submitting a
   * long-running command (e.g. `install.packages`) and then queuing a marker
   * command behind it while R is still busy, polling output for the marker.
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
 * By default this waits for R to finish the command -- it waits for a new
 * console prompt (waitForConsoleCommandComplete) rather than sleeping. Pass
 * `{ wait: false }` for the fire-and-forget pattern (e.g. queuing a marker
 * command behind a long-running install).
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
  // Capture the console prompt count before submitting so the wait below can
  // key off a *new* prompt (command completed) rather than sampling the busy
  // class. Must be read before any Enter press, while R is still at the prior
  // prompt.
  const promptCountBefore = await getConsolePromptCount(page);
  // Try the Enter dispatch a few times. Ace can briefly route the keystroke
  // to an autocomplete / signature-help popup (selecting an entry instead of
  // submitting). The completion wait below can't catch that case on its own:
  // if nothing was submitted, no new prompt arrives, so the wait would just
  // time out (and the busy-class fallback returns immediately, R already being
  // idle) while the command sits unsubmitted in the editor. After each press
  // we verify the editor is now empty (Ace clears the input on submit); if
  // not, dismiss any popup and try again.
  const MAX_ATTEMPTS = 3;
  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
    if (attempt === 1) {
      // First attempt: only clear a completions popup if one happens to be
      // lingering from a prior interaction; setValue() shouldn't have
      // triggered any new overlay.
      if (await page.locator('#rstudio_popup_completions').isVisible()) {
        await page.keyboard.press('Escape');
      }
    } else {
      // Retry: the previous Enter didn't submit, so some overlay (popup,
      // signature help, hover tooltip, ...) almost certainly intercepted
      // it. Send Escape unconditionally to cover the full set of
      // Enter-intercepting overlays, then re-focus the editor since
      // Escape can shift focus off it.
      await page.keyboard.press('Escape');
      await page.evaluate(() => {
        const e = document.getElementById('rstudio_console_input') as AceEditorElement | null;
        e?.env?.editor?.focus();
      });
    }
    // Press Enter on the console-input textarea explicitly.
    // `page.keyboard.press` delivers to the focused element; relying on
    // editor.focus() above is racy -- focus can shift between the evaluate()
    // returning and the key press, leaving the text in the buffer but never
    // submitted.
    await page.locator(CONSOLE_INPUT).press('Enter');

    // Verify the command was actually consumed by polling for an empty
    // editor value. The await-poll completes immediately on the happy path.
    try {
      await page.waitForFunction(
        () => {
          const e = document.getElementById('rstudio_console_input') as AceEditorElement | null;
          return e?.env?.editor?.getValue() === '';
        },
        null,
        { timeout: 1000, polling: 50 },
      );
      break;
    } catch (err) {
      // Only TimeoutError is a legitimate "editor still non-empty" retry
      // case -- other errors (frame teardown, exec-context destroyed, JS
      // evaluation failures) must propagate so root causes aren't masked
      // by the catch-all submission-failure message below.
      const isTimeout = err instanceof Error && err.name === 'TimeoutError';
      if (!isTimeout) throw err;
      if (attempt === MAX_ATTEMPTS) {
        throw new Error(
          `executeInConsole: Enter did not submit "${command.slice(0, 80)}" ` +
          `after ${MAX_ATTEMPTS} attempts; editor still non-empty.`,
        );
      }
    }
  }
  if (opts.wait ?? true) {
    await waitForConsoleCommandComplete(page, promptCountBefore, opts.timeout);
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


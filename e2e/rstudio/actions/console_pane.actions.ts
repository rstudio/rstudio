import type { Page } from 'playwright';
import * as fs from 'fs';
import {
  ConsolePane,
  waitForConsoleBusy,
  waitForConsoleIdle,
  type EnvironmentVersions,
  type ExecuteInConsoleOptions,
} from '../pages/console_pane.page';
import { sleep } from '../utils/constants';
import { documentCloseAllNoSave, executeCommand, getVersion, resetSourcePaneState } from '../utils/commands';
import { AceEditorElement } from '../utils/ace';

let cachedInstallRepos: string | null = null;

// Returns the repo URL to use for install.packages(). On Linux, prefer Posit
// Public Package Manager's per-distro repo so installs don't compile from
// source (which can take minutes for packages with heavy C/C++ deps like
// duckdb); PPM serves precompiled tarballs that R fetches via type="source".
//
// The install *type* is chosen at R runtime via .Platform$pkgType -- see the
// install.packages() call below -- so source-only R builds (Homebrew macOS,
// any Linux) don't fall over on a forced type = "binary". This mirrors the
// pattern used by the global lib pre-population in fixtures/r-libs-setup.ts.
function getInstallRepos(): string {
  if (cachedInstallRepos) return cachedInstallRepos;

  if (process.platform === 'win32' || process.platform === 'darwin') {
    cachedInstallRepos = 'https://cran.r-project.org';
    return cachedInstallRepos;
  }

  const distro = detectLinuxDistro();
  cachedInstallRepos = distro
    ? `https://packagemanager.posit.co/cran/__linux__/${distro}/latest`
    : 'https://cran.r-project.org';
  return cachedInstallRepos;
}

// Returns the PPM distro slug for the current Linux host, or '' if unknown.
// Ubuntu/Debian use VERSION_CODENAME (e.g. "noble", "jammy", "bookworm").
// RHEL family (rhel/rocky/almalinux/centos) has no codename; PPM serves
// them as "rhel<major>" derived from VERSION_ID.
function detectLinuxDistro(): string {
  const osRelease = fs.readFileSync('/etc/os-release', 'utf8');
  const get = (key: string): string =>
    osRelease.match(new RegExp(`^${key}=("?)([^"\\n]+)\\1`, 'm'))?.[2] ?? '';

  const codename = get('VERSION_CODENAME');
  if (codename) return codename;

  const id = get('ID');
  const idLike = get('ID_LIKE').split(/\s+/);
  const rhelFamily = new Set(['rhel', 'rocky', 'almalinux', 'centos']);
  if (rhelFamily.has(id) || idLike.includes('rhel')) {
    const major = get('VERSION_ID').split('.')[0];
    if (major) return `rhel${major}`;
  }
  return '';
}

export class ConsolePaneActions {
  readonly page: Page;
  readonly consolePane: ConsolePane;

  constructor(page: Page) {
    this.page = page;
    this.consolePane = new ConsolePane(page);
  }

  /**
   * Submit an R expression to the console. Writes the text directly into the
   * console's Ace editor and presses Enter -- no per-key typing -- so it
   * doesn't race with autocomplete popups or other live-edit UI that can
   * swallow characters. Prefer this when you just need code to run; use
   * `typeInConsole` only when a test is exercising actual typing behavior.
   */
  /**
   * Wait until the console input's Ace editor owns the document focus.
   * activateConsole schedules the focus shift on the next event loop tick,
   * so callers that hand the console keystrokes immediately after the
   * command can race with the focus change. Polling beats a blind sleep
   * here -- the common case settles in tens of milliseconds.
   */
  private async waitForConsoleFocus(): Promise<void> {
    await this.page.waitForFunction(
      () => {
        const el = document.getElementById('rstudio_console_input');
        return el !== null && el.contains(document.activeElement);
      },
      null,
      { timeout: 5000, polling: 50 },
    );
  }

  async executeInConsole(command: string, opts: ExecuteInConsoleOptions = {}): Promise<void> {
    // Focus the console via the activateConsole command rather than
    // clicking the console tab. The tab-click path was clicking the
    // same element repeatedly across executeInConsole + clearConsole
    // calls; using the command keeps focus changes deterministic and
    // avoids any tab-level click side effects.
    await executeCommand(this.page, 'activateConsole');
    await this.waitForConsoleFocus();
    await this.page.evaluate((text) => {
      const el = document.getElementById('rstudio_console_input') as AceEditorElement | null;
      const editor = el?.env?.editor;
      if (!editor) throw new Error('Console Ace editor not found at #rstudio_console_input');
      editor.setValue(text, 1); // 1 = move cursor to end
      editor.focus();
    }, command);
    if (await this.page.locator('#rstudio_popup_completions').isVisible()) {
      await this.page.keyboard.press('Escape');
    }
    // Press Enter on the console-input textarea explicitly. `page.keyboard.press`
    // delivers to the focused element; relying on editor.focus() above is racy --
    // focus can shift between the evaluate() returning and the key press,
    // leaving the text in the buffer but never submitted.
    await this.consolePane.consoleInput.press('Enter');
    if (opts.wait ?? true) {
      await waitForConsoleIdle(this.page, opts.timeout);
    }
  }

  /**
   * Simulate user typing one keystroke at a time into the console input. Does
   * NOT press Enter -- the caller controls submission. Use only when a test
   * needs to exercise live-edit behavior that `executeInConsole`'s programmatic
   * write doesn't trigger (e.g. autocomplete popups).
   *
   * `delayMs` is the per-keystroke delay; default 50ms is close to typical
   * human typing speed and gives the editor time to dispatch input events and
   * fire completers between chars.
   */
  async typeInConsole(text: string, delayMs: number = 50): Promise<void> {
    await executeCommand(this.page, 'activateConsole');
    await this.waitForConsoleFocus();
    await this.consolePane.consoleInput.pressSequentially(text, { delay: delayMs });
  }

  async clearConsole(): Promise<void> {
    await executeCommand(this.page, 'activateConsole');
    await this.waitForConsoleFocus();
    await this.page.keyboard.press('Control+l');
    await sleep(500);
  }

  async closeAllBuffersWithoutSaving(): Promise<void> {
    // Use the window.rstudio bridge instead of typing `.rs.api.close...`
    // into the console: the R session isn't busy while the close fires, so
    // RStudio's "session is busy" confirmation dialog can't intervene.
    await documentCloseAllNoSave(this.page);
    await sleep(500);
  }

  /**
   * Reset the source pane to a single untitled tab. Prefer this over
   * closeAllBuffersWithoutSaving in test setup where a following file.edit
   * or newDoc is imminent: closing every tab triggers a HIDE animation on
   * the source pane that races the next open (#17738), while resetToUntitled
   * keeps the pane in its NORMAL state throughout.
   */
  async resetSourcePane(): Promise<void> {
    await resetSourcePaneState(this.page);
    await sleep(500);
  }

  async getEnvironmentVersions(): Promise<EnvironmentVersions> {
    return getVersion(this.page);
  }

  async goToLine(line: number): Promise<void> {
    await executeCommand(this.page, 'goToLine');
    await sleep(500);
    await this.page.keyboard.type(String(line));
    await this.page.keyboard.press('Enter');
    await sleep(500);
  }

  /**
   * Evaluate an R expression that yields a single logical and return its value
   * (or null if it couldn't be read). Wraps the expression in parens so R
   * auto-prints the result, then reads the printed `[1] TRUE`/`[1] FALSE` out
   * of the console. Clears the console first and relies on executeInConsole
   * waiting for R to go idle, so the only `[1] ...` line in the output is this
   * result -- no unique marker needed (the echoed input line has no `[1]`
   * prefix, so it can't false-match).
   */
  private async evalRLogical(expr: string): Promise<boolean | null> {
    await this.clearConsole();
    await this.executeInConsole(`(${expr})`);
    const output = await this.consolePane.consoleOutput.innerText();
    const match = output.match(/\[1\]\s+(TRUE|FALSE)/);
    return match ? match[1] === 'TRUE' : null;
  }

  /**
   * Ensure an R package is available, installing it if necessary.
   * Returns true if the package is available after the check, false if installation failed.
   */
  async ensurePackage(pkg: string, timeoutMs = 60000): Promise<boolean> {
    if ((await this.evalRLogical(`requireNamespace("${pkg}", quietly = TRUE)`)) === true) {
      return true;
    }

    // Package not installed -- try to install it
    console.log(`Installing R package: ${pkg}...`);
    await this.clearConsole();
    const repos = getInstallRepos();
    // Pick the install type at R runtime so source-only R builds (Homebrew
    // macOS, all Linux) don't error with "type 'binary' is not supported".
    const typeExpr = `if (identical(.Platform$pkgType, "source")) "source" else "binary"`;
    // install.packages can run for a while, so submit it fire-and-forget and
    // then wait for R to go idle (with a generous timeout) rather than polling
    // output for a done-marker. Confirm R actually picked up the install
    // (busy) first: waitForConsoleIdle keys off the *absence* of the busy
    // class, so without this it could return on the gap between submitting and
    // R starting. A package install reliably stays busy for seconds, but if it
    // somehow finished before we observed busy, idle is already true -- so a
    // missed busy window is harmless.
    await this.executeInConsole(
      `install.packages("${pkg}", repos = "${repos}", type = ${typeExpr})`,
      { wait: false },
    );
    await waitForConsoleBusy(this.page).catch(() => {});
    await waitForConsoleIdle(this.page, timeoutMs);

    // Idle only tells us R is free again, not whether the install succeeded --
    // verify by reading the package's availability back out of the console.
    const installed =
      (await this.evalRLogical(`requireNamespace("${pkg}", quietly = TRUE)`)) === true;
    if (installed) {
      console.log(`Package ${pkg} is now available.`);
    } else {
      console.log(`WARNING: Failed to install package ${pkg}.`);
    }
    return installed;
  }

  /**
   * Ensure multiple R packages are available, installing any that are missing.
   * Returns an array of package names that could not be installed.
   */
  async ensurePackages(packages: string[], timeoutMs = 60000): Promise<string[]> {
    const failed: string[] = [];
    for (const pkg of packages) {
      const ok = await this.ensurePackage(pkg, timeoutMs);
      if (!ok) failed.push(pkg);
    }
    return failed;
  }

  /**
   * Uninstall an R package if currently installed. Unloads the namespace first
   * so downstream `requireNamespace()` checks correctly see it as missing.
   * Returns true if the package is absent (on disk and unloaded) after the call.
   *
   * Avoids `requireNamespace()` for gating -- it has a load side-effect that
   * would leave the namespace cached after remove.packages() wipes the files,
   * masking the uninstall from later checks.
   *
   * `pkg` must be a valid R package name (letters, digits, dots only, must
   * start with a letter) -- R itself enforces this, so interpolation into the
   * double-quoted R strings below is safe.
   */
  async uninstallPackage(pkg: string, timeoutMs = 30000): Promise<boolean> {
    await this.clearConsole();

    // Each executeInConsole waits for R to go idle before returning, so by the
    // time the remove() call resolves the package files are gone -- no
    // done-marker poll needed. Give the remove() call the caller's timeout
    // since uninstall can touch many files.
    await this.executeInConsole(
      `if ("${pkg}" %in% loadedNamespaces()) { try(detach(paste0("package:", "${pkg}"), character.only = TRUE, unload = TRUE), silent = TRUE); try(unloadNamespace("${pkg}"), silent = TRUE) }`,
    );
    await this.executeInConsole(
      `if ("${pkg}" %in% rownames(installed.packages())) remove.packages("${pkg}")`,
      { timeout: timeoutMs },
    );

    // Verify the package is actually gone: true (absent) when it is no longer
    // present in the installed-packages list.
    const present = await this.evalRLogical(
      `"${pkg}" %in% rownames(installed.packages())`,
    );
    return present === false;
  }
}

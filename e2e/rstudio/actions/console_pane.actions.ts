import type { Page } from 'playwright';
import * as fs from 'fs';
import { ConsolePane, type EnvironmentVersions } from '../pages/console_pane.page';
import { sleep } from '../utils/constants';
import { documentCloseAllNoSave, executeCommand } from '../utils/commands';

interface InstallTarget {
  repos: string;
  type: 'binary' | 'source';
}

let cachedInstallTarget: InstallTarget | null = null;

// Returns the repo URL and install type to use for install.packages().
// On Linux, prefer Posit Public Package Manager's per-distro binary repo so
// installs don't compile from source (which can take minutes for packages
// with heavy C/C++ deps like duckdb).
function getInstallTarget(): InstallTarget {
  if (cachedInstallTarget) return cachedInstallTarget;

  if (process.platform === 'win32' || process.platform === 'darwin') {
    cachedInstallTarget = { repos: 'https://cran.r-project.org', type: 'binary' };
    return cachedInstallTarget;
  }

  const distro = detectLinuxDistro();
  // On Linux, R has no formal "binary" type -- PPM's __linux__/<distro>/latest
  // endpoint serves precompiled tarballs that R fetches via type="source".
  // Passing type="binary" here causes R to look for a nonexistent format and
  // silently skip the install.
  cachedInstallTarget = distro
    ? {
        repos: `https://packagemanager.posit.co/cran/__linux__/${distro}/latest`,
        type: 'source',
      }
    : { repos: 'https://cran.r-project.org', type: 'source' };
  return cachedInstallTarget;
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
  async executeInConsole(command: string): Promise<void> {
    await this.consolePane.consoleTab.click();
    await this.page.evaluate((text) => {
      const el = document.getElementById('rstudio_console_input') as
        | (HTMLElement & {
            env?: { editor?: { setValue(s: string, cursorPos?: number): void; focus(): void } };
          })
        | null;
      const editor = el?.env?.editor;
      if (!editor) throw new Error('Console Ace editor not found at #rstudio_console_input');
      editor.setValue(text, 1); // 1 = move cursor to end
      editor.focus();
    }, command);
    if (await this.page.locator('#rstudio_popup_completions').isVisible()) {
      await this.page.keyboard.press('Escape');
    }
    await this.page.keyboard.press('Enter');
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
    await this.consolePane.consoleTab.click();
    await this.consolePane.consoleInput.click({ force: true });
    await sleep(300);
    await this.consolePane.consoleInput.pressSequentially(text, { delay: delayMs });
  }

  async clearConsole(): Promise<void> {
    await this.consolePane.consoleTab.click();
    await this.consolePane.consoleInput.click({ force: true });
    await sleep(200);
    await this.page.keyboard.press('Control+l');
    await sleep(500);
  }

  async closeAllBuffersWithoutSaving(): Promise<void> {
    // Use the rstudioCallbacks bridge instead of typing `.rs.api.close...`
    // into the console: the R session isn't busy while the close fires, so
    // RStudio's "session is busy" confirmation dialog can't intervene.
    await documentCloseAllNoSave(this.page);
    await sleep(500);
  }

  async getEnvironmentVersions(): Promise<EnvironmentVersions> {
    await this.typeInConsole('cat("R:", R.version.string, "\\nRStudio:", RStudio.Version()$long_version)');
    await sleep(2000);

    const output = await this.consolePane.consoleOutput.innerText();
    const rMatch = output.match(/R:\s*(R version [\d.]+[^\n]*)/);
    const rstudioMatch = output.match(/RStudio:\s*([\d.+]+)/);

    return {
      r: rMatch?.[1] ?? 'unknown',
      rstudio: rstudioMatch?.[1] ?? 'unknown',
    };
  }

  async goToLine(line: number): Promise<void> {
    await executeCommand(this.page, 'goToLine');
    await sleep(500);
    await this.page.keyboard.type(String(line));
    await this.page.keyboard.press('Enter');
    await sleep(500);
  }

  /**
   * Ensure an R package is available, installing it if necessary.
   * Returns true if the package is available after the check, false if installation failed.
   *
   * Uses unique timestamp markers (e.g., `__PKG_1234__`) wrapped around R output
   * so we can reliably parse TRUE/FALSE from the console even if other output is present.
   */
  async ensurePackage(pkg: string, timeoutMs = 60000): Promise<boolean> {
    const marker = `__PKG_${Date.now()}__`;

    await this.clearConsole();
    await this.typeInConsole(`cat("${marker}", requireNamespace("${pkg}", quietly = TRUE), "${marker}")`);
    await sleep(1000);

    const output = await this.consolePane.consoleOutput.innerText();
    const match = output.match(new RegExp(`${marker}\\s+(TRUE|FALSE)\\s+${marker}`));

    if (match && match[1] === 'TRUE') {
      return true;
    }

    // Package not installed — try to install it
    console.log(`Installing R package: ${pkg}...`);
    const doneMarker = `__WHATS_DONE_IS_DONE_${Date.now()}__`;
    await this.clearConsole();
    // Run install, then print marker as a separate command.
    // typeInConsole queues input — if R is busy with install, the cat()
    // will execute after install finishes.
    const { repos, type } = getInstallTarget();
    await this.typeInConsole(`install.packages("${pkg}", repos = "${repos}", type = "${type}")`);
    await this.typeInConsole(`cat("${doneMarker}")`);


    // Wait for the done marker to appear (indicates install finished)
    const startTime = Date.now();
    while (Date.now() - startTime < timeoutMs) {
      await sleep(3000);
      const text = await this.consolePane.consoleOutput.innerText();
      if (text.includes(doneMarker)) {
        break;
      }
    }

    // Verify installation succeeded. Poll for the marker -- after a package
    // installs, R may still be doing post-install work (lazy-load DB, help
    // index) when the doneMarker emitted, so the verify cat() can sit
    // queued briefly before R gets to it.
    const verifyMarker = `__PKG_VERIFY_${Date.now()}__`;
    await this.clearConsole();
    await this.typeInConsole(`cat("${verifyMarker}", requireNamespace("${pkg}", quietly = TRUE), "${verifyMarker}")`);

    const verifyPattern = new RegExp(`${verifyMarker}\\s+(TRUE|FALSE)\\s+${verifyMarker}`);
    const verifyDeadline = Date.now() + 15000;
    let installed = false;
    while (Date.now() < verifyDeadline) {
      await sleep(500);
      const verifyOutput = await this.consolePane.consoleOutput.innerText();
      const verifyMatch = verifyOutput.match(verifyPattern);
      if (verifyMatch) {
        installed = verifyMatch[1] === 'TRUE';
        break;
      }
    }

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
   * Avoids `requireNamespace()` for gating — it has a load side-effect that
   * would leave the namespace cached after remove.packages() wipes the files,
   * masking the uninstall from later checks.
   *
   * `pkg` must be a valid R package name (letters, digits, dots only, must
   * start with a letter) — R itself enforces this, so interpolation into the
   * double-quoted R strings below is safe.
   */
  async uninstallPackage(pkg: string, timeoutMs = 30000): Promise<boolean> {
    await this.clearConsole();
    const doneMarker = `__UNINSTALL_DONE_${Date.now()}__`;

    await this.typeInConsole(
      `if ("${pkg}" %in% loadedNamespaces()) { try(detach(paste0("package:", "${pkg}"), character.only = TRUE, unload = TRUE), silent = TRUE); try(unloadNamespace("${pkg}"), silent = TRUE) }`,
    );
    await this.typeInConsole(
      `if ("${pkg}" %in% rownames(installed.packages())) remove.packages("${pkg}")`,
    );
    await this.typeInConsole(`cat("${doneMarker}")`);

    const startTime = Date.now();
    let doneMarkerSeen = false;
    while (Date.now() - startTime < timeoutMs) {
      await sleep(1000);
      const text = await this.consolePane.consoleOutput.innerText();
      if (text.includes(doneMarker)) {
        doneMarkerSeen = true;
        break;
      }
    }
    if (!doneMarkerSeen) return false;

    const verifyMarker = `__UNINSTALL_VERIFY_${Date.now()}__`;
    await this.clearConsole();
    await this.typeInConsole(
      `cat("${verifyMarker}", "${pkg}" %in% rownames(installed.packages()), "${verifyMarker}")`,
    );

    const verifyStart = Date.now();
    const verifyPattern = new RegExp(`${verifyMarker}\\s+(TRUE|FALSE)\\s+${verifyMarker}`);
    while (Date.now() - verifyStart < 15000) {
      await sleep(500);
      const output = await this.consolePane.consoleOutput.innerText();
      const match = output.match(verifyPattern);
      if (match) return match[1] === 'FALSE';
    }
    return false;
  }
}

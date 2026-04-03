import type { Page } from 'playwright';
import { ConsolePane, type EnvironmentVersions } from '../pages/console_pane.page';
import { sleep } from '../utils/constants';

export class ConsolePaneActions {
  readonly page: Page;
  readonly consolePane: ConsolePane;

  constructor(page: Page) {
    this.page = page;
    this.consolePane = new ConsolePane(page);
  }

  async typeInConsole(command: string): Promise<void> {
    await this.consolePane.consoleTab.click();
    await this.consolePane.consoleInput.click({ force: true });
    await sleep(500);
    await this.consolePane.consoleInput.pressSequentially(command);
    await sleep(200);
    if (await this.page.locator('#rstudio_popup_completions').isVisible()) {
      await this.page.keyboard.press('Escape');
      await sleep(100);
    }
    await this.consolePane.consoleInput.press('Enter');
  }

  async clearConsole(): Promise<void> {
    await this.consolePane.consoleTab.click();
    await this.consolePane.consoleInput.click({ force: true });
    await sleep(200);
    await this.page.keyboard.press('Control+l');
    await sleep(500);
  }

  async closeAllBuffersWithoutSaving(): Promise<void> {
    await this.typeInConsole('.rs.api.closeAllSourceBuffersWithoutSaving()');
    await sleep(1000);
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
    await this.typeInConsole(`.rs.api.executeCommand('goToLine')`);
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
    const installType = 'ifelse(.Platform$OS.type == "windows" || Sys.info()["sysname"] == "Darwin", "binary", "source")';
    await this.typeInConsole(`install.packages("${pkg}", repos = "https://cran.r-project.org", type = ${installType})`);
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

    // Verify installation succeeded
    const verifyMarker = `__PKG_VERIFY_${Date.now()}__`;
    await this.clearConsole();
    await this.typeInConsole(`cat("${verifyMarker}", requireNamespace("${pkg}", quietly = TRUE), "${verifyMarker}")`);
    await sleep(1000);

    const verifyOutput = await this.consolePane.consoleOutput.innerText();
    const verifyMatch = verifyOutput.match(new RegExp(`${verifyMarker}\\s+(TRUE|FALSE)\\s+${verifyMarker}`));

    const installed = verifyMatch?.[1] === 'TRUE';
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
}

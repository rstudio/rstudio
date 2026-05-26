// Editor warning bar for files that reference uninstalled R packages.
// Addresses https://github.com/rstudio/rstudio/issues/15830 -- a per-file
// "Don't show for this file" dismissal that can be undone via the
// toggleDetectMissingPackages AppCommand.

import type { Locator, Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { documentOpen, executeCommand, isCommandChecked, saveDocument } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

// InfoBar duplicates its message into a role="status" live-region for screen
// readers (see InfoBar.java setText -- live_ is populated after a short
// AriaLiveService.UI_ANNOUNCEMENT_DELAY). Scope locator queries to the
// visible label so getByText doesn't trip strict mode once the live region
// has caught up.
function bannerLocator(page: Page, pkg: string): Locator {
  return page
    .getByText(`Package ${pkg} required but is not installed.`)
    .and(page.locator(':not([role="status"])'));
}

// Two real CRAN packages that the test session won't already have installed.
// `fortunes` has no Imports/Depends, so it isn't pulled in by REQUIRED_PACKAGES'
// transitive closure. `cowsay` is a small package that depends on `fortunes`
// (so removing `fortunes` from the cache is enough to make both look missing).
const TEST_PKG_A = 'fortunes';
const TEST_PKG_B = 'cowsay';

test.describe('Missing-package banner', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
    // discoverPackageDependencies filters to CRAN packages that aren't on
    // disk, so if a prior run or the shared R_LIBS_USER cache already has
    // the package the banner won't appear. Force a clean slate for both.
    await consoleActions.uninstallPackage(TEST_PKG_A);
    await consoleActions.uninstallPackage(TEST_PKG_B);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [
      'missing_packages.R',
      'missing_packages_a.R',
      'missing_packages_b.R',
    ]);
  });

  test('dismisses per-file and re-appears when toggleDetectMissingPackages is flipped back on',
    async ({ rstudioPage: page }) => {
      await writeAndOpenFile(
        page,
        sandbox.dir,
        'missing_packages.R',
        `library(${TEST_PKG_A})\n`,
      );

      // discoverPackageDependencies fires on save (and on
      // AvailablePackagesReadyEvent). Save forces the path we care about.
      await saveDocument(page);

      const banner = bannerLocator(page, TEST_PKG_A);
      // available.packages() may still be loading the first time the session
      // hits this path; the helper retries on AvailablePackagesReadyEvent,
      // so the visible-banner deadline is intentionally generous.
      await expect(banner).toBeVisible({ timeout: 60000 });
      expect(await isCommandChecked(page, 'toggleDetectMissingPackages')).toBe(true);

      // Per-file dismiss: banner disappears and the AppCommand picks up the
      // property change via the DocUpdateSentinel handler.
      await page.getByText("Don't show for this file").click();
      await expect(banner).toBeHidden({ timeout: 5000 });
      await expect
        .poll(() => isCommandChecked(page, 'toggleDetectMissingPackages'))
        .toBe(false);

      // Re-enable via the AppCommand: clears the suppression property and
      // re-runs discovery, so the banner returns.
      await executeCommand(page, 'toggleDetectMissingPackages');
      await expect(banner).toBeVisible({ timeout: 60000 });
      expect(await isCommandChecked(page, 'toggleDetectMissingPackages')).toBe(true);
    });

  test('dismissal is per-file and the command tracks the active editor',
    async ({ rstudioPage: page }) => {
      const pathA = `${sandbox.dir}/missing_packages_a.R`;
      const pathB = `${sandbox.dir}/missing_packages_b.R`;

      // Open A, save, wait for its banner, then dismiss in A.
      await writeAndOpenFile(page, sandbox.dir, 'missing_packages_a.R', `library(${TEST_PKG_A})\n`);
      await saveDocument(page);
      const bannerA = bannerLocator(page, TEST_PKG_A);
      await expect(bannerA).toBeVisible({ timeout: 60000 });
      await page.getByText("Don't show for this file").click();
      await expect(bannerA).toBeHidden({ timeout: 5000 });
      await expect
        .poll(() => isCommandChecked(page, 'toggleDetectMissingPackages'))
        .toBe(false);

      // Open B (different missing package). The dismissal in A must not
      // suppress B's banner -- the suppression property lives on A's
      // DocUpdateSentinel, not in a global pref.
      await writeAndOpenFile(page, sandbox.dir, 'missing_packages_b.R', `library(${TEST_PKG_B})\n`);
      await saveDocument(page);
      const bannerB = bannerLocator(page, TEST_PKG_B);
      await expect(bannerB).toBeVisible({ timeout: 60000 });
      // B is the active editor; syncDetectMissingPackagesMode() in onActivate
      // restores the command to checked for the newly-active file.
      await expect
        .poll(() => isCommandChecked(page, 'toggleDetectMissingPackages'))
        .toBe(true);

      // Switch back to A: command reflects A's (dismissed) state. The banner
      // stays gone because tab activation doesn't re-run discovery.
      await documentOpen(page, pathA, {}, TIMEOUTS.fileOpen);
      await expect
        .poll(() => isCommandChecked(page, 'toggleDetectMissingPackages'))
        .toBe(false);
      await expect(bannerA).toBeHidden();

      // Switch back to B: command flips to true, B's banner is still visible.
      await documentOpen(page, pathB, {}, TIMEOUTS.fileOpen);
      await expect
        .poll(() => isCommandChecked(page, 'toggleDetectMissingPackages'))
        .toBe(true);
      await expect(bannerB).toBeVisible();
    });
});

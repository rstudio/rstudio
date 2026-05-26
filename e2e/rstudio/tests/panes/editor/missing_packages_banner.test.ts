// Editor warning bar for files that reference uninstalled R packages.
// Addresses https://github.com/rstudio/rstudio/issues/15830 -- a per-file
// "Don't show for this file" dismissal that can be undone via the
// toggleDetectMissingPackages AppCommand.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { executeCommand, isCommandChecked, saveDocument } from '@utils/commands';

// `fortunes` is on CRAN and has no Imports/Depends, so it isn't pulled in by
// any of REQUIRED_PACKAGES' transitive deps -- a reliable "real CRAN package
// the test session doesn't already have installed."
const TEST_PKG = 'fortunes';

test.describe('Missing-package banner', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
    // discoverPackageDependencies filters to CRAN packages that aren't on
    // disk, so if a prior run or the shared R_LIBS_USER cache already has
    // the package the banner won't appear. Force a clean slate.
    await consoleActions.uninstallPackage(TEST_PKG);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, ['missing_packages.R']);
  });

  test('dismisses per-file and re-appears when toggleDetectMissingPackages is flipped back on',
    async ({ rstudioPage: page }) => {
      await writeAndOpenFile(
        page,
        sandbox.dir,
        'missing_packages.R',
        `library(${TEST_PKG})\n`,
      );

      // discoverPackageDependencies fires on save (and on
      // AvailablePackagesReadyEvent). Save forces the path we care about.
      await saveDocument(page);

      const banner = page.getByText(
        `Package ${TEST_PKG} required but is not installed.`,
      );
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
});

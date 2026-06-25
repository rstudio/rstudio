import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';

let consoleActions: ConsolePaneActions;

// Tracked by #18064 (re-enable on Server).
// Server-on-Linux: ConsolePaneActions.restartSession() relies on the
// `__RESTART_<ts>__DONE` marker being printed cleanly via afterRestart's
// cat(). On Server the post-restart console fills with what look like
// terminal-escape-rendered glyphs (long runs of `ה`/`X`) instead of the
// marker, so toContainText(marker) never matches. Likely the IDE's
// post-restart render writes ANSI sequences the helper isn't decoding.
// Skip on Server until the helper handles the post-restart console state
// correctly. Related: package_installation.test.ts uses the same helper.
test.describe.serial('R session restart', { tag: ['@serial', '@desktop_only'] }, () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test(
    'clean=false preserves environment; clean=true clears it',
    async () => {
      const missing = await consoleActions.ensurePackages(['magrittr']);
      test.skip(missing.length > 0, 'magrittr could not be installed');

      // Start from a known clean state
      await consoleActions.restartSession({ clean: true });

      // Load magrittr and set a variable
      await consoleActions.executeInConsole('library(magrittr, logical.return = TRUE)');
      await consoleActions.executeInConsole("mb <- 'ISFP'");

      // Verify state before any restart
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole("isNamespaceLoaded('magrittr')");
      await expect(consoleActions.consolePane.consoleOutput).toContainText('TRUE');
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole('mb');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('ISFP');

      // clean=false: namespace and workspace should be preserved
      await consoleActions.restartSession({ clean: false });
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole("isNamespaceLoaded('magrittr')");
      await expect(consoleActions.consolePane.consoleOutput).toContainText('TRUE');
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole('mb');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('ISFP');

      // clean=true: namespace and workspace should be cleared
      await consoleActions.restartSession({ clean: true });
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole("isNamespaceLoaded('magrittr')");
      await expect(consoleActions.consolePane.consoleOutput).toContainText('FALSE');
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole('mb');
      await expect(consoleActions.consolePane.consoleOutput).toContainText("object 'mb' not found");
    },
  );
});

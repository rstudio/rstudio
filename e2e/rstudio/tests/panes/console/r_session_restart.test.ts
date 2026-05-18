import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';

let consoleActions: ConsolePaneActions;

test.describe.serial('R session restart', { tag: ['@serial'] }, () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test(
    'clean=false preserves environment; clean=true clears it',
    { tag: ['@smoketest'] },
    async () => {
      const missing = await consoleActions.ensurePackages(['magrittr']);
      test.skip(missing.length > 0, 'magrittr could not be installed');

      // Start from a known clean state
      await consoleActions.restartSession({ clean: true });

      // Load magrittr and set a variable
      await consoleActions.typeInConsole('library(magrittr, logical.return = TRUE)');
      await consoleActions.typeInConsole("mb <- 'ISFP'");

      // Verify state before any restart
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole("isNamespaceLoaded('magrittr')");
      await expect(consoleActions.consolePane.consoleOutput).toContainText('TRUE');
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole('mb');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('ISFP');

      // clean=false: namespace and workspace should be preserved
      await consoleActions.restartSession({ clean: false });
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole("isNamespaceLoaded('magrittr')");
      await expect(consoleActions.consolePane.consoleOutput).toContainText('TRUE');
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole('mb');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('ISFP');

      // clean=true: namespace and workspace should be cleared
      await consoleActions.restartSession({ clean: true });
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole("isNamespaceLoaded('magrittr')");
      await expect(consoleActions.consolePane.consoleOutput).toContainText('FALSE');
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole('mb');
      await expect(consoleActions.consolePane.consoleOutput).toContainText("object 'mb' not found");
    },
  );
});

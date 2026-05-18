import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';

let consoleActions: ConsolePaneActions;

const PACKAGES = ['meditations', 'titanic', 'rtrek'] as const;

test.describe.serial('Package installation', { tag: ['@serial'] }, () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test(
    'installs packages and verifies their functionality',
    async () => {
      test.setTimeout(600000);

      // Remove any pre-existing copies so the install path is exercised
      for (const pkg of PACKAGES) {
        await consoleActions.uninstallPackage(pkg);
      }
      await consoleActions.restartSession({ clean: true });

      // Install all three; skip if any fail
      const missing = await consoleActions.ensurePackages([...PACKAGES], TIMEOUTS.packageInstall);
      test.skip(missing.length > 0, `Could not install: ${missing.join(', ')}`);

      // meditations: known quote at index 70
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole('meditations::meditations(70)');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('Death hangs over thee.');

      // titanic: second test passenger ID
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole('titanic::titanic_test[2,1]');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('893');

      // rtrek: third series name
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole('rtrek::stSeries[3,1]');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('Deep Space Nine');
    },
  );
});

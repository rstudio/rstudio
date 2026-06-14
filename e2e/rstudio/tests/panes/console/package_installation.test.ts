import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';

let consoleActions: ConsolePaneActions;

const PACKAGES = ['meditations', 'titanic', 'rtrek'] as const;

// TODO(aza): file a follow-up issue to unblock on Server.
// Server-on-Linux: this suite calls ConsolePaneActions.restartSession(),
// which is broken on Server -- the post-restart console fills with
// escape-sequence glyphs instead of the expected `__RESTART_<ts>__DONE`
// marker, so the helper's toContainText(marker) times out. Same root cause
// as the r_session_restart.test.ts skip; tag this preventively until the
// helper is fixed (we got lucky on the most recent run, but sharding is
// not stable, so leaving it untagged would re-introduce flakiness).
test.describe.serial('Package installation', { tag: ['@serial', '@desktop_only'] }, () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test(
    'installs packages and verifies their functionality',
    async () => {
      test.setTimeout(600000);

      // Remove any pre-existing copies so the install path is exercised.
      // Assert each package is actually absent afterward -- otherwise a silent
      // uninstall failure would leave it present and the install path below
      // would be skipped while the functional checks still passed.
      for (const pkg of PACKAGES) {
        expect(
          await consoleActions.uninstallPackage(pkg),
          `precondition: ${pkg} must be absent before install`,
        ).toBe(true);
      }
      await consoleActions.restartSession({ clean: true });

      // Install all three; skip if any fail
      const missing = await consoleActions.ensurePackages([...PACKAGES], TIMEOUTS.packageInstall);
      test.skip(missing.length > 0, `Could not install: ${missing.join(', ')}`);

      // meditations: known quote at index 70
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole('meditations::meditations(70)');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('Death hangs over thee.');

      // titanic: second test passenger ID
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole('titanic::titanic_test[2,1]');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('893');

      // rtrek: third series name
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole('rtrek::stSeries[3,1]');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('Deep Space Nine');
    },
  );
});

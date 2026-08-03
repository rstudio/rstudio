/**
 * Running commands over an established connection: DBI queries from the
 * console against seeded data, with the connection open in the pane. The
 * queries are engine-specific and come from the target descriptor.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConnectionsPaneActions } from '@actions/connections_pane.actions';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ALL_DB_TARGETS, effectiveTarget } from '@utils/db-targets';
import {
  dbAvailability,
  drainKnownExplorerException,
  driverVisibleInSession,
} from '@utils/connections';

for (const base of ALL_DB_TARGETS) {
  const target = effectiveTarget(base);

  test.describe(`Queries over a pane connection (${target.id})`, () => {
    let actions: ConnectionsPaneActions;
    let consoleActions: ConsolePaneActions;
    let driverVisible = false;
    let seeded = false;

    test.beforeAll(async ({ rstudioPage: page }) => {
      actions = new ConnectionsPaneActions(page);
      consoleActions = new ConsolePaneActions(page);
      driverVisible = await driverVisibleInSession(page, target);
      if (driverVisible && dbAvailability(target).ok) {
        seeded = await actions.seedDatabase(target);
      }
    });

    test.beforeEach(async () => {
      test.skip(
        !driverVisible,
        `session does not see the "${target.driverName}" ODBC driver (sandbox registration unavailable here)`,
      );
      const avail = dbAvailability(target);
      test.skip(!avail.ok, avail.reason);
      test.skip(!seeded, 'seeding the database through DBI failed');
    });

    test('connect via wizard, query seeded data from the console, disconnect', async () => {
      await actions.activatePane();
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.confirmWizardAndWaitConnected();

      // The wizard (Connect from: R Console) assigned `con` in the console.
      // The queries themselves are engine SQL and live in the descriptor.
      for (const expr of target.verifyQueriesR) {
        expect(await consoleActions.evalRLogical(expr), `should be TRUE: ${expr}`).toBe(true);
      }

      await actions.disconnect();
    });

    test.afterEach(async ({ rstudioPage: page }) => {
      expect(
        await drainKnownExplorerException(page),
        'only the known object_types client exception is tolerated',
      ).toEqual([]);
    });
  });
}

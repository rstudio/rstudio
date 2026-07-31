/**
 * Running commands over an established connection: DBI queries from the
 * console against seeded data, with the connection open in the pane.
 *
 * Counts come back from the odbc driver as integer64; comparisons in R are
 * done with as.numeric() so the console text is a plain number (a bare
 * cat() of an integer64 prints its raw bit pattern).
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConnectionsPaneActions } from '@actions/connections_pane.actions';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { POSTGRES, effectiveTarget } from '@utils/db-targets';
import {
  dbAvailability,
  drainKnownExplorerException,
  driverVisibleInSession,
} from '@utils/connections';

const target = effectiveTarget(POSTGRES);

test.describe('Queries over a pane connection', () => {
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
    const rowCount = await consoleActions.evalRLogical(
      'identical(as.numeric(DBI::dbGetQuery(con, "SELECT count(*) AS n FROM sales.orders")$n), 3)',
    );
    expect(rowCount, 'sales.orders should hold the 3 seeded rows').toBe(true);

    const tables = await consoleActions.evalRLogical(
      'all(c("customers", "orders") %in% DBI::dbGetQuery(con,' +
        ' "SELECT tablename FROM pg_tables WHERE schemaname = \'sales\'")$tablename)',
    );
    expect(tables, 'both seeded sales tables should be listed').toBe(true);

    const aggregate = await consoleActions.evalRLogical(
      'identical(as.numeric(DBI::dbGetQuery(con, "SELECT sum(amount) AS s FROM sales.orders")$s), 149.75)',
    );
    expect(aggregate, 'aggregate over seeded rows should compute').toBe(true);

    await actions.disconnect();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    expect(
      await drainKnownExplorerException(page),
      'only the known object_types client exception is tolerated',
    ).toEqual([]);
  });
});

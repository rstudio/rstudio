/**
 * Connection explorer: drilling the object tree (containers per the
 * target's descriptor, e.g. catalog, schema, table for PostgreSQL), the
 * column leaves, filtering by object, and opening a table in the data
 * viewer.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConnectionsPaneActions } from '@actions/connections_pane.actions';
import { ALL_DB_TARGETS, effectiveTarget } from '@utils/db-targets';
import {
  dbAvailability,
  drainKnownExplorerException,
  driverVisibleInSession,
} from '@utils/connections';

for (const base of ALL_DB_TARGETS) {
  const target = effectiveTarget(base);

  test.describe(`Connection explorer (${target.id})`, () => {
    let actions: ConnectionsPaneActions;
    let driverVisible = false;
    let seeded = false;

    test.beforeAll(async ({ rstudioPage: page }) => {
      actions = new ConnectionsPaneActions(page);
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
      await actions.activatePane();
    });

    test('drills to a seeded table, shows its columns, and opens the data viewer', async ({
      rstudioPage: page,
    }) => {
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.confirmWizardAndWaitConnected();

      // Container path from the descriptor (for PostgreSQL: catalog, schema,
      // table).
      await actions.drillExplorer(target);

      // The table's columns render as leaf rows of "name : type" (e.g.
      // "id : int4"); match on the name prefix since the type is the
      // database's business.
      for (const column of target.tableColumns) {
        await expect(
          actions.pane.panel.getByText(new RegExp(`^${column}\\b`)).first(),
        ).toBeVisible({ timeout: 15000 });
      }

      // Filtering by object narrows the tree to matching names. The last
      // path element is the table regardless of the engine's nesting depth.
      const table = target.explorerPath[target.explorerPath.length - 1];
      await actions.pane.filterObjects.fill(table);
      await expect(actions.pane.panel.getByText(table, { exact: true })).toBeVisible();

      // Clear the filter before using the tree again.
      await actions.pane.filterObjects.fill('');

      // View table opens the data viewer on up to 1,000 records, showing
      // the table's actual contents ("Charlie" is seeded only into this
      // table, so its presence pins both the table and the data).
      await actions.pane.viewTableIcon(table).first().click();
      await expect(page.locator('iframe[title="Data Browser"]')).toBeVisible({ timeout: 20000 });
      const dataBrowser = page.frameLocator('iframe[title="Data Browser"]');
      await expect(dataBrowser.getByText('Charlie')).toBeVisible({ timeout: 20000 });

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

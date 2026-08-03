/**
 * Wizard-driven connect: Test reports success, OK connects, the connection
 * appears in the pane, disconnect returns it to "(Not connected)".
 *
 * Requires the registered driver AND a reachable database; each gate skips
 * with its own reason. The specs share one provisioned database per run, so
 * connection state is always cleaned up (disconnect) in afterEach.
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

  test.describe(`Connections pane connect and disconnect (${target.id})`, () => {
    let actions: ConnectionsPaneActions;
    let driverVisible = false;

    test.beforeAll(async ({ rstudioPage: page }) => {
      actions = new ConnectionsPaneActions(page);
      driverVisible = await driverVisibleInSession(page, target);
    });

    test.beforeEach(async () => {
      test.skip(
        !driverVisible,
        `session does not see the "${target.driverName}" ODBC driver (sandbox registration unavailable here)`,
      );
      const avail = dbAvailability(target);
      test.skip(!avail.ok, avail.reason);
      await actions.activatePane();
    });

    test.afterEach(async ({ rstudioPage: page }) => {
      // Leave no live connection or open dialog behind for the next test.
      if (await actions.wizard.dialog.isVisible()) {
        await actions.wizard.cancelBtn.click();
        await actions.wizard.dialog.waitFor({ state: 'hidden', timeout: 5000 });
      }
      if (await actions.pane.disconnectBtn.isVisible()) {
        await actions.disconnect();
      }
      expect(
        await drainKnownExplorerException(page),
        'only the known object_types client exception is tolerated',
      ).toEqual([]);
    });

    test('Test button reports success against the provisioned database', async () => {
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      expect(await actions.testConnection(), 'wizard Test should succeed').toBe(true);
      await actions.wizard.cancelBtn.click();
    });

    test('OK connects and the pane shows the connection as connected', async () => {
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.confirmWizardAndWaitConnected();

      // While connected: disconnect and refresh are offered, remove is not.
      await expect(actions.pane.refreshConnectionBtn).toBeVisible();
      await expect(actions.pane.removeConnectionBtn).not.toBeVisible();

      await actions.disconnect();
      await expect(actions.pane.disconnectBtn).not.toBeVisible();
    });

    test('reconnects a previously disconnected connection from its list entry', async () => {
      // Start from a clean list: engines connecting to the same database
      // share a display name, so leftover entries from other specs (or the
      // sibling engine) would make row selection ambiguous.
      await actions.removeMatchingConnections(target.database);

      // Create a connection and disconnect it; its entry stays listed.
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.confirmWizardAndWaitConnected();
      await actions.disconnect();
      await expect(actions.pane.connectionRow(target.database).first()).toBeVisible();

      // Reconnect through the entry's stored code rather than the wizard:
      // explore the disconnected entry, then Connect from its toolbar.
      await actions.exploreConnection(target.database);
      await actions.reconnectExplored();

      // The reconnected connection is live again, and disconnects cleanly.
      await expect(actions.pane.refreshConnectionBtn).toBeVisible();
      await actions.disconnect();

      // Remove the entry, leaving the list clean for the next iteration.
      await actions.removeMatchingConnections(target.database);
      await expect(actions.pane.connectionRow(target.database)).not.toBeVisible();
    });
  });
}

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
import { POSTGRES, effectiveTarget } from '@utils/db-targets';
import {
  dbAvailability,
  drainKnownExplorerException,
  driverVisibleInSession,
} from '@utils/connections';

const target = effectiveTarget(POSTGRES);

test.describe('Connections pane connect and disconnect', () => {
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
});

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
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { ALL_DB_TARGETS, connectionDisplayName, effectiveTarget, resolveRemoteFileTarget } from '@utils/db-targets';
import {
  dbAvailability,
  drainKnownExplorerException,
  driverVisibleInSession,
  resetConnectionState,
} from '@utils/connections';
import { restartSessionWithSentinel } from '@utils/project';

// The pane rebuilds its connection list from a client event, so membership
// changes (a removal in particular) land some time after the action that
// caused them returns. Every assertion about which rows are listed polls
// with this budget instead of the default, which is shorter than the lag.
const LIST_REFRESH_TIMEOUT = 20000;

for (const base of ALL_DB_TARGETS) {
  const target = effectiveTarget(base);
  const displayName = connectionDisplayName(target);

  // Every test here restarts the R session in beforeEach and then opens the
  // New Connection wizard right after: on Server, that sequence makes the
  // dialog disappear before it can be used (the wizard opens, then vanishes
  // entirely from the page moments later, with the console idle and ready
  // throughout -- not a slow or busy session). Reproduced consistently
  // across both database targets; two independent timing mitigations in the
  // test harness (retrying the open, giving the type-list click a generous
  // wait) did not fix it. Distinct from rstudio/rstudio#18064's tracked
  // Server-restart issues, which are a different symptom (console text
  // unreadable, or window.rstudio.ready not re-arming), though the same
  // general class. Un-skip once this is filed and fixed.
  test.describe(`Connections pane connect and disconnect (${target.id})`, { tag: ['@desktop_only'] }, () => {
    let actions: ConnectionsPaneActions;
    let consoleActions: ConsolePaneActions;
    let driverVisible = false;

    test.beforeAll(async ({ rstudioPage: page }) => {
      resolveRemoteFileTarget(target);
      actions = new ConnectionsPaneActions(page);
      consoleActions = new ConsolePaneActions(page);
      driverVisible = await driverVisibleInSession(page, target);
    });

    test.beforeEach(async ({ rstudioPage: page }) => {
      test.skip(
        !driverVisible,
        `session does not see the "${target.driverName}" ODBC driver (sandbox registration unavailable here)`,
      );
      const avail = dbAvailability(target);
      test.skip(!avail.ok, avail.reason);
      // Specs share one session, so each test starts by clearing this
      // target's state. Two steps, and the order matters:
      //
      //  1. Clear the connection and its history entry (fails loudly if the
      //     removal RPC errors).
      //  2. Restart R. This drops any live connection and any `con` binding
      //     outright, and -- the reason it is worth the seconds -- makes the
      //     pane rebuild its list from the now-clean history at startup,
      //     instead of depending on a client event that
      //     ConnectionHistory::remove never sends.
      await resetConnectionState(page, target);
      await restartSessionWithSentinel(page);
      await actions.activatePane();
      // Asserts nothing is LIVE, not that the list is empty. Closing a live
      // connection during the restart calls ConnectionHistory::update, which
      // re-adds the entry the step above removed, so an empty list is not
      // something the product guarantees here. No live connection is.
      await expect(actions.pane.disconnectBtn).not.toBeVisible();
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

    test('Test button reports failure for a wrong password', async () => {
      test.skip(
        target.kind === 'file',
        `${target.id} authenticates no one (the driver opens a file), so there is no wrong password to send`,
      );
      await actions.openWizard();
      await actions.fillWizardForTarget(target, { Password: 'wrong-password' });
      expect(
        await actions.testConnection(),
        'wizard Test should report failure for bad credentials',
      ).toBe(false);
      await actions.wizard.cancelBtn.click();
    });

    test('Test button reports failure for an unreachable port', async () => {
      test.skip(
        target.kind === 'file',
        `${target.id} has no server or port to make unreachable`,
      );
      await actions.openWizard();
      // Port 1 on loopback: nothing listens there, so the driver gets an
      // immediate refusal (no DNS or timeout waits).
      await actions.fillWizardForTarget(target, { Port: '1' });
      expect(
        await actions.testConnection(),
        'wizard Test should report failure for an unreachable server',
      ).toBe(false);
      await actions.wizard.cancelBtn.click();
    });

    test('Test button reports failure for a nonexistent database path', async () => {
      test.skip(
        target.kind !== 'file',
        `${target.id} has no bare file path to connect to (wrong password and unreachable ` +
          'port above already cover its failure modes)',
      );
      await actions.openWizard();
      await actions.fillWizardForTarget(target, { Database: '/nonexistent/dir/x.db' });
      expect(
        await actions.testConnection(),
        'wizard Test should report failure for a path whose directory does not exist',
      ).toBe(false);
      await actions.wizard.cancelBtn.click();
    });

    test('Connect from New R Script puts the connect code in a new script', async ({
      rstudioPage: page,
    }) => {
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.wizard.connectFromDropdown.selectOption('connect-new-r-script');
      await actions.wizard.okBtn.click();
      await actions.wizard.dialog.waitFor({ state: 'hidden', timeout: 10000 });

      // A new R script holds the generated connect code.
      const editor = new AceEditor(page, '');
      await expect
        .poll(() => editor.getValue(), { timeout: 15000 })
        .toContain(`Driver   = "${target.driverName}"`);

      // Only the R Console destination runs the code (see
      // ConnectionsPresenter.onPerformConnection: the script and notebook
      // destinations create a document and stop), so nothing is connected
      // yet -- the user runs the script when ready.
      await expect(actions.pane.disconnectBtn).not.toBeVisible();

      await consoleActions.resetSourcePane();
    });

    // Parked: verifying the generated script actually connects when run is
    // the natural next assertion, but no reliable way to run it from a test
    // has been found. Submitting the buffer through the executeAllCode
    // command delivers the multi-line call out of order; pasting the text
    // into the console lets the completion wait satisfy on an intermediate
    // line (passes most runs, not all); wrapping it in eval(parse(text=))
    // submits cleanly but the connection never registers with the pane;
    // sourceActiveDocument prompts to save an untitled document. The
    // generated code itself is known good -- the wizard's R Console
    // destination runs the same code and is covered above.
    test.fixme('the generated script connects when run', async ({ rstudioPage: page }) => {
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.wizard.connectFromDropdown.selectOption('connect-new-r-script');
      await actions.wizard.okBtn.click();
      await actions.wizard.dialog.waitFor({ state: 'hidden', timeout: 10000 });

      const editor = new AceEditor(page, '');
      const script = await editor.getValue();
      await consoleActions.executeInConsole(script);
      await actions.pane.disconnectBtn.waitFor({ state: 'visible', timeout: 30000 });
      await actions.disconnect();
      await consoleActions.resetSourcePane();
    });

    test('reconnects a previously disconnected connection from its list entry', async () => {
      // Create a connection and disconnect it; its entry stays listed. Each
      // target uses a distinct database name, so its row is unambiguous even
      // with the sibling engine's connections present.
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.confirmWizardAndWaitConnected();
      await actions.disconnect();
      await expect(actions.pane.connectionRow(displayName)).toHaveCount(1, {
        timeout: LIST_REFRESH_TIMEOUT,
      });

      // Reconnect through the entry's stored code rather than the wizard:
      // explore the disconnected entry, then Connect from its toolbar.
      await actions.exploreConnection(displayName);
      await actions.reconnectExplored();

      // The reconnected connection is live again, and disconnects cleanly.
      await expect(actions.pane.refreshConnectionBtn).toBeVisible();
      await actions.disconnect();

    });

    // Parked on unreliable product behavior: removing a connection updates
    // the pane's list most of the time, but not always (observed failing
    // roughly one run in three, and consistently when the connection had
    // been reconnected first). ConnectionHistory::remove writes the history
    // file but never calls onConnectionsChanged, unlike
    // ConnectionHistory::update which does -- so the list only refreshes
    // when some other event happens to fire and rebuild it from history.
    // The entry IS gone server-side either way; it disappears on restart.
    //
    // fixme rather than fail(): the test passes often enough that fail()
    // would report an unexpected pass most runs. Un-fixme once the missing
    // notification is added.
    test.fixme('removing a disconnected connection drops it from the list', async () => {
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.confirmWizardAndWaitConnected();
      await actions.disconnect();
      await expect(actions.pane.connectionRow(displayName)).toHaveCount(1, {
        timeout: LIST_REFRESH_TIMEOUT,
      });

      await actions.exploreConnection(displayName);
      await actions.removeExploredConnection();
      // Deliberately the default timeout: this assertion is expected to
      // fail, so there is no reason to wait out the longer refresh budget.
      await expect(actions.pane.connectionRow(displayName)).toHaveCount(0);
    });
  });
}

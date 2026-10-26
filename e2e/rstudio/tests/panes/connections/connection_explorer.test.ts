/**
 * Connection explorer: drilling the object tree (containers per the
 * target's descriptor, e.g. catalog, schema, table for PostgreSQL), the
 * column leaves, filtering by object, and opening a table in the data
 * viewer.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConnectionsPaneActions } from '@actions/connections_pane.actions';
import { ALL_DB_TARGETS, effectiveTarget, resolveRemoteFileTarget } from '@utils/db-targets';
import { restartSessionWithSentinel } from '@utils/project';
import {
  dbAvailability,
  drainKnownExplorerException,
  driverVisibleInSession,
  resetConnectionState,
} from '@utils/connections';
import type { Locator } from '@playwright/test';

/**
 * Dispatch an Enter keydown to a tree item's cell. Sending it to the element,
 * rather than through the keyboard, makes it independent of focus, which a
 * refresh of the object tree can take.
 */
async function dispatchEnterKeydown(
  item: Locator,
  init: { shiftKey?: boolean; repeat?: boolean },
): Promise<void> {
  await item.evaluate((el, eventInit) => {
    const event = new KeyboardEvent('keydown', {
      key: 'Enter',
      code: 'Enter',
      bubbles: true,
      cancelable: true,
      ...eventInit,
    });
    // GWT reads the legacy keyCode, which KeyboardEvent's init ignores.
    Object.defineProperty(event, 'keyCode', { get: () => 13 });
    el.dispatchEvent(event);
  }, init);
}

for (const base of ALL_DB_TARGETS) {
  const target = effectiveTarget(base);

  // Restarts the R session in beforeEach and then opens the New Connection
  // wizard: on Server that makes the dialog disappear before use. See the
  // comment on the matching describe in connection_connect.test.ts.
  test.describe(`Connection explorer (${target.id})`, { tag: ['@desktop_only'] }, () => {
    let actions: ConnectionsPaneActions;
    let driverVisible = false;
    let seeded = false;
    let seedFailureDetail = '';

    test.beforeAll(async ({ rstudioPage: page }) => {
      resolveRemoteFileTarget(target);
      actions = new ConnectionsPaneActions(page);
      driverVisible = await driverVisibleInSession(page, target);
      if (driverVisible && dbAvailability(target).ok) {
        const result = await actions.seedDatabase(target);
        seeded = result.ok;
        seedFailureDetail = result.detail;
      }
    });

    test.beforeEach(async ({ rstudioPage: page }) => {
      test.skip(
        !driverVisible,
        `session does not see the "${target.driverName}" ODBC driver (sandbox registration unavailable here)`,
      );
      const avail = dbAvailability(target);
      test.skip(!avail.ok, avail.reason);
      test.skip(!seeded, `seeding the database through DBI failed: ${seedFailureDetail}`);
      // Specs share one session: clear this target's connection and
      // history entry, then restart R so no live connection or `con`
      // binding survives from a prior test. The restart costs a couple
      // of seconds and is what makes these specs deterministic.
      await resetConnectionState(page, target);
      await restartSessionWithSentinel(page);
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
      // Checking only that it stays visible would pass even if the filter
      // did nothing -- drillExplorer already made it visible above -- so
      // also confirm a sibling table (seeded for every target, per
      // db-targets.ts) is visible before filtering and gone after.
      const table = target.explorerPath[target.explorerPath.length - 1];
      const sibling = actions.pane.panel.getByText('customers', { exact: true });
      await expect(sibling).toBeVisible();
      await actions.pane.filterObjects.pressSequentially(table);
      await expect(actions.pane.panel.getByText(table, { exact: true })).toBeVisible();
      await expect(sibling).not.toBeVisible();

      // Clear the filter before using the tree again. fill('') has the same
      // problem as fill(table) above, in reverse: it wouldn't fire the
      // KeyUpHandler either, leaving the tree filtered for whatever runs
      // next. Select the typed text and delete it with a real keystroke.
      await actions.pane.filterObjects.selectText();
      await actions.pane.filterObjects.press('Backspace');
      await expect(sibling).toBeVisible();

      // View table opens the data viewer on up to 1,000 records, showing
      // the table's actual contents ("Charlie" is seeded only into this
      // table, so its presence pins both the table and the data).
      await actions.pane.viewTableIcon(table).click();
      await expect(page.locator('iframe[title="Data Browser"]')).toBeVisible({ timeout: 20000 });
      const dataBrowser = page.frameLocator('iframe[title="Data Browser"]');
      await expect(dataBrowser.getByText('Charlie')).toBeVisible({ timeout: 20000 });

      await actions.disconnect();
    });

    test('opens a table in the data viewer with Enter', async ({ rstudioPage: page }) => {
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.confirmWizardAndWaitConnected();
      await actions.drillExplorer(target);

      // The fixture's per-test reset closes earlier viewers. Check it, so a
      // leftover viewer can't stand in for the one Enter should open.
      const viewers = page.locator('iframe[title="Data Browser"]');
      await expect(viewers).toHaveCount(0);

      // Clicking the table's name (not its View table icon) moves the tree's
      // keyboard focus to that item, which is where Enter is handled. A late
      // refresh of the object tree can drop that focus, so focus the item and
      // press again if the key didn't reach it.
      const table = target.explorerPath[target.explorerPath.length - 1];
      const tableName = actions.pane.panel.getByText(table, { exact: true });
      await expect(async () => {
        await tableName.click();
        await expect(tableName.locator('xpath=..')).toBeFocused({ timeout: 1000 });
        await page.keyboard.press('Enter');
        await expect(viewers).toHaveCount(1, { timeout: 3000 });
      }).toPass({ timeout: 20000 });
      const dataBrowser = page.frameLocator('iframe[title="Data Browser"]');
      await expect(dataBrowser.getByText('Charlie')).toBeVisible({ timeout: 20000 });

      await actions.disconnect();
    });

    test('does not open a table with Shift+Enter or a repeated Enter', async ({
      rstudioPage: page,
    }) => {
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.confirmWizardAndWaitConnected();
      await actions.drillExplorer(target);

      const table = target.explorerPath[target.explorerPath.length - 1];
      const tableItem = actions.pane.panel.getByText(table, { exact: true }).locator('xpath=..');
      const viewers = page.locator('iframe[title="Data Browser"]');
      await expect(viewers).toHaveCount(0);

      // Count preview requests rather than viewers: previewing a table whose
      // viewer is already open reuses that viewer, so a second request can
      // leave the viewer count unchanged.
      const previews: string[] = [];
      const onRequest = (request: { url(): string }) => {
        if (request.url().endsWith('/rpc/connection_preview_object')) previews.push(request.url());
      };
      page.on('request', onRequest);

      try {
        // The key handler sends the preview request as it handles the key, so
        // a request from either ignored key would go out before the plain
        // Enter's, and be counted by the time that one's viewer appears.
        await dispatchEnterKeydown(tableItem, { shiftKey: true });
        await dispatchEnterKeydown(tableItem, { repeat: true });
        await dispatchEnterKeydown(tableItem, {});
        await expect(viewers.first()).toBeVisible({ timeout: 20000 });
        expect(previews, 'Shift+Enter or a repeated Enter requested a preview').toHaveLength(1);
      } finally {
        page.off('request', onRequest);
      }

      await actions.disconnect();
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
  });
}

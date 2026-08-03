/**
 * Running commands over an established connection: DBI queries from the
 * console against seeded data, with the connection open in the pane. The
 * queries are engine-specific and come from the target descriptor.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConnectionsPaneActions } from '@actions/connections_pane.actions';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { YES_BTN } from '@pages/modals.page';
import { executeCommand } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';
import { ALL_DB_TARGETS, effectiveTarget } from '@utils/db-targets';
import {
  dbAvailability,
  drainKnownExplorerException,
  driverVisibleInSession,
} from '@utils/connections';

for (const base of ALL_DB_TARGETS) {
  const target = effectiveTarget(base);

  test.describe(`Queries over a pane connection (${target.id})`, () => {
    const sandbox = useSuiteSandbox();
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

    test('SQL toolbar action opens a wired script whose preview shows seeded rows', async ({
      rstudioPage: page,
    }) => {
      await actions.activatePane();
      await actions.openWizard();
      await actions.fillWizardForTarget(target);
      await actions.confirmWizardAndWaitConnected();

      // The odbc connection contributes a "SQL" action to the explorer
      // toolbar: it opens a new SQL script pre-wired to the connection via
      // a "-- !preview conn=" header (the wizard bound the connection to
      // `con` in the console).
      await actions.pane.sqlActionButton.click();
      const editor = new AceEditor(page, '');
      await expect
        .poll(() => editor.getValue(), { timeout: 15000 })
        .toContain('-- !preview conn=con');
      await consoleActions.resetSourcePane();

      // Preview from a saved file: previewing an untitled document opens a
      // Save File dialog instead. The query targets seeded data; the table
      // reference is the engine's container path (schema-qualified for
      // PostgreSQL, bare for MySQL).
      const fileName = `preview_${target.id}.sql`;
      const tableRef = target.explorerPath.join('.');
      await writeAndOpenFile(
        page,
        sandbox.dir,
        fileName,
        `-- !preview conn=con\nSELECT * FROM ${tableRef}\n`,
      );
      await executeCommand(page, 'previewSql');

      // Previewing a conn= expression can prompt for consent (see
      // sql_preview_security.test.ts); approvals persist per session, so
      // the prompt appears at most once. Approve it if it shows.
      const yes = page.locator(YES_BTN);
      await yes
        .waitFor({ state: 'visible', timeout: 5000 })
        .then(() => yes.click())
        .catch(() => undefined); // no prompt: expression already permitted

      // Results render in the Data Output pane; "Charlie" is seeded only
      // into this table.
      const results = page.frameLocator('iframe[title="Data Output Pane"]');
      await expect(results.getByText('Charlie')).toBeVisible({ timeout: 30000 });

      // Close and remove the SQL file, then drop the connection.
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
      await actions.activatePane();
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

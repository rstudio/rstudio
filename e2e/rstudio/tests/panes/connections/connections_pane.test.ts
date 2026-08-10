/**
 * Connections pane basics, plus the environment probes the rest of the
 * connections suite builds on:
 *
 *  - the sandbox-local ODBC configuration (ODBCSYSINI) reaches the rsession
 *  - the driver registered by the sandbox is visible to the session
 *  - the throwaway database the suite provisioned accepts TCP connections
 *    from the session
 *
 * Probes run inside the rsession, not the test runner: in remote Server mode
 * what the runner can see is irrelevant. A recorded provisioning failure
 * downgrades to a skip that names the problem; anything else that renders
 * nothing still fails.
 *
 * The two @desktop_only tests assert the Desktop fixture's launch-env
 * plumbing specifically. In Server mode rserver rebuilds each rsession's
 * environment from scratch (see the note in fixtures/server.fixture.ts), so
 * spawn-time variables never arrive; server engines instead register drivers
 * machine-wide (CI-enablement phase), and the wizard/connect specs gate on
 * in-session driver visibility rather than on mode.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand } from '@utils/commands';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { CONNECTIONS_PANEL, NEW_CONNECTION_BTN } from '@pages/connections_pane.page';
import { ALL_DB_TARGETS, effectiveTarget } from '@utils/db-targets';
import {
  dbAvailability,
  dbReachableFromSession,
  driverVisibleInSession,
} from '@utils/connections';
import { rStringLiteral } from '@utils/r';

test.describe('Connections pane', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    // Bridge command instead of clicking the tab: avoids actionability
    // failures when another element overlaps the tab in full-suite runs
    // (same rationale as environment_pane.test.ts).
    await executeCommand(page, 'activateConnections');
  });

  test('shows the pane with its New Connection button', async ({ rstudioPage: page }) => {
    await expect(page.locator(CONNECTIONS_PANEL)).toBeVisible();
    await expect(page.locator(NEW_CONNECTION_BTN)).toBeVisible();
  });

  test(
    'sandbox ODBC configuration reaches the session',
    { tag: ['@desktop_only'] },
    async () => {
      const odbcDir = process.env.PW_ODBC_DIR;
      test.skip(!odbcDir, 'no sandbox ODBC dir (no registered driver library on this machine)');
      const matches = await consoleActions.evalRLogical(
        `identical(Sys.getenv("ODBCSYSINI"), ${rStringLiteral(odbcDir!)})`,
      );
      expect(matches, `rsession ODBCSYSINI should be ${odbcDir}`).toBe(true);
    },
  );

});

// Per-target probes: one describe per database engine the suite knows.
for (const base of ALL_DB_TARGETS) {
  const target = effectiveTarget(base);

  test.describe(`Connections pane probes (${target.id})`, () => {
    let consoleActions: ConsolePaneActions;

    test.beforeAll(async ({ rstudioPage: page }) => {
      consoleActions = new ConsolePaneActions(page);
    });

    test.beforeEach(async ({ rstudioPage: page }) => {
      await executeCommand(page, 'activateConnections');
    });

    test(
      'registered driver is visible to the session',
      { tag: ['@desktop_only'] },
      async ({ rstudioPage: page }) => {
        // PW_ODBC_REGISTERED, not PW_ODBC_DIR: on Windows the suite registers
        // drivers in the registry and there is no ODBCSYSINI directory, so
        // gating on the directory would skip this everywhere on that platform
        // even though the driver is registered and visible.
        test.skip(
          !process.env.PW_ODBC_REGISTERED,
          'no ODBC driver registered by the suite (no driver library on this machine)',
        );
        // Belongs to required-packages.txt, so normally preinstalled by
        // globalSetup; a hard failure here means the library seeding broke.
        const failed = await consoleActions.ensurePackages(['odbc']);
        expect(failed, 'odbc R package must be installable').toEqual([]);
        expect(
          await driverVisibleInSession(page, target),
          `odbcListDrivers() should list "${target.driverName}"`,
        ).toBe(true);
      },
    );

    test('provisioned database accepts connections from the session', async ({
      rstudioPage: page,
    }) => {
      const avail = dbAvailability(target);
      test.skip(!avail.ok, avail.reason);
      expect(
        await dbReachableFromSession(page, target),
        `session should reach ${target.id} (status: ${avail.reason})`,
      ).toBe(true);
    });
  });
}

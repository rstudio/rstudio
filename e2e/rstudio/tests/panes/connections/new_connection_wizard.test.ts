/**
 * New Connection wizard: the sandbox-registered driver appears in the type
 * list, its snippet renders labeled parameter fields (the same path
 * professional drivers take), typing updates the generated code, and
 * Back/Cancel navigate as expected. No database needed: nothing here
 * connects.
 *
 * Gate: in-session driver visibility, not mode or platform. Wherever the
 * session can't see the registered driver (e.g. Server until its engine
 * registers drivers machine-wide), these skip with that reason.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConnectionsPaneActions } from '@actions/connections_pane.actions';
import { POSTGRES, effectiveTarget } from '@utils/db-targets';
import { driverVisibleInSession } from '@utils/connections';

const target = effectiveTarget(POSTGRES);

test.describe('New Connection wizard', () => {
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
    await actions.activatePane();
  });

  test.afterEach(async () => {
    // Close any wizard a failed assertion left open, so the next test does
    // not start behind a modal glass overlay.
    if (await actions.wizard.dialog.isVisible()) {
      await actions.wizard.cancelBtn.click();
      await actions.wizard.dialog.waitFor({ state: 'hidden', timeout: 5000 });
    }
  });

  test('lists the registered driver and renders its labeled fields', async () => {
    await actions.openWizard();
    const entry = actions.wizard.typeEntry(target.driverName);
    await expect(entry).toBeVisible({ timeout: 10000 });
    await entry.click();

    // The snippet's placeholders become labeled fields; Server and Port
    // carry defaults from the descriptor, the credentials start blank.
    await expect(actions.wizard.field('Server')).toHaveValue(target.host);
    await expect(actions.wizard.field('Port')).toHaveValue(String(target.port));
    for (const key of Object.keys(target.wizardFields)) {
      await expect(actions.wizard.field(key)).toHaveValue('');
    }

    // The generated code reflects the snippet before any typing.
    expect(await actions.wizard.previewCode()).toContain(`Driver   = "${target.driverName}"`);

    // The "Connect from" dropdown offers the four standard destinations.
    const options = actions.wizard.connectFromDropdown.locator('option');
    await expect(options).toHaveText([
      'R Console',
      'New R Script',
      'New R Notebook',
      'Copy to Clipboard',
    ]);
  });

  test('typing into fields updates the generated code, and Back returns to the list', async () => {
    await actions.openWizard();
    await actions.fillWizardForTarget(target);

    const code = await actions.wizard.previewCode();
    expect(code).toContain(`Database = "${target.database}"`);
    expect(code).toContain(`UID      = "${target.user}"`);

    await actions.wizard.backBtn.click();
    await expect(actions.wizard.typeEntry(target.driverName)).toBeVisible({ timeout: 5000 });

    await actions.wizard.cancelBtn.click();
    await expect(actions.wizard.dialog).not.toBeVisible({ timeout: 5000 });
  });
});

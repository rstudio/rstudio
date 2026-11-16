/**
 * New Connection wizard's ODBC driver install page, fed by a dummy installer
 * file so no real driver download is involved. The read-only license box
 * must be reachable by its "License:" label (#19381).
 *
 * Desktop-only: the session lists ODBC installers only on Desktop
 * (.rs.connectionReadInstallers).
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConnectionsPaneActions } from '@actions/connections_pane.actions';
import { executeInConsole } from '@pages/console_pane.page';
import { rStringLiteral } from '@utils/r';

const DRIVER_NAME = 'Playwright ODBC Installer';
const LICENSE = 'Playwright license text for the ODBC installer page.';

// No Licensed: field, which would hide the entry unless driver licensing is
// supported. The Download URL is never fetched: the test cancels first.
const INSTALLER_DCF = [
  'Version: 1.0',
  `License: ${LICENSE}`,
  'Download: https://example.com/playwright-driver.tar.gz',
];

const installerPath = `file.path(.rs.connectionOdbcInstallerPath(), ${rStringLiteral(
  `${DRIVER_NAME}.dcf`,
)})`;

test.describe('New Connection wizard (ODBC driver installer)', { tag: ['@desktop_only'] }, () => {
  let actions: ConnectionsPaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    actions = new ConnectionsPaneActions(page);
    const lines = INSTALLER_DCF.map(rStringLiteral).join(', ');
    await executeInConsole(
      page,
      'dir.create(.rs.connectionOdbcInstallerPath(), recursive = TRUE, showWarnings = FALSE); ' +
        `writeLines(c(${lines}), ${installerPath})`,
      { wait: true },
    );
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    if (await actions.wizard.dialog.isVisible()) {
      await actions.wizard.cancelBtn.click();
      await actions.wizard.dialog.waitFor({ state: 'hidden', timeout: 5000 });
    }
    await executeInConsole(page, `unlink(${installerPath})`, { wait: true });
  });

  test('labels the license text', async () => {
    await actions.activatePane();
    await actions.openWizard();
    const entry = actions.wizard.typeEntry(DRIVER_NAME);
    await expect(entry).toBeVisible({ timeout: 20000 });
    await entry.click();

    const license = actions.wizard.dialog.getByRole('textbox', { name: 'License:', exact: true });
    await expect(license).toBeVisible();
    await expect(license).toHaveValue(LICENSE);
  });
});

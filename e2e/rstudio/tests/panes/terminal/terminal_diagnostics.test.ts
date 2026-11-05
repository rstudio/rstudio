// Terminal Diagnostics dialog (#19381). Its read-only text box has no
// visible label, so the dialog names it with its own title.

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand } from '@utils/commands';
import { killAllTerminals, openTerminal } from '@utils/terminal';

const DIALOG = '.gwt-DialogBox[aria-label="Terminal Diagnostics"]';

test.describe('Terminal Diagnostics dialog', () => {
  test.beforeEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page);
    await openTerminal(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    const dialog = page.locator(DIALOG);
    if (await dialog.isVisible()) {
      await dialog.getByRole('button', { name: 'Close', exact: true }).click();
      await dialog.waitFor({ state: 'hidden', timeout: 5000 });
    }
    await killAllTerminals(page).catch(() => {});
    await executeCommand(page, 'activateConsole').catch(() => {});
  });

  test('names the diagnostics text box', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'showTerminalInfo');
    const dialog = page.locator(DIALOG);
    await expect(dialog).toBeVisible();

    const box = dialog.getByRole('textbox', { name: 'Terminal Diagnostics', exact: true });
    await expect(box).toBeVisible();
    await expect(box).toHaveValue(/Global Terminal Information/);
  });
});

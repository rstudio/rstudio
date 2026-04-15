import { test, expect } from '@fixtures/rstudio.fixture';
import { CONSOLE_INPUT } from '@pages/console_pane.page';

test.describe('Startup smoke test', () => {
  test('RStudio starts and stays alive for 30 seconds', async ({ rstudioPage: page }) => {
    // Verify console is ready
    await expect(page.locator(CONSOLE_INPUT)).toBeVisible({ timeout: 30000 });

    // Stay alive for 30 seconds
    await page.waitForTimeout(30000);

    // Verify RStudio is still responsive
    await expect(page.locator(CONSOLE_INPUT)).toBeVisible();
  });
});

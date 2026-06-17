import { test, expect } from '@fixtures/rstudio.fixture';
import { CONSOLE_INPUT } from '@pages/console_pane.page';

// Excluded from default runs (see PW_RUN_SMOKE in playwright.config.ts); opt in
// with PW_RUN_SMOKE=1. The 30s idle below needs more than the 30s default test
// timeout -- and full runs cap the global timeout even lower -- so give this
// test its own headroom regardless of the ambient setting.
test.describe('Startup smoke test', { tag: ['@smoke'] }, () => {
  test('RStudio starts and stays alive for 30 seconds', async ({ rstudioPage: page }) => {
    test.setTimeout(60000);

    // Verify console is ready
    await expect(page.locator(CONSOLE_INPUT)).toBeVisible({ timeout: 30000 });

    // Stay alive for 30 seconds
    await page.waitForTimeout(30000);

    // Verify RStudio is still responsive
    await expect(page.locator(CONSOLE_INPUT)).toBeVisible();
  });
});

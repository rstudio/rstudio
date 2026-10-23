import type { Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { getPref, setPref } from '@utils/commands';

// The root lang must name the displayed UI language so screen readers pick the
// right voice. After the pref changes, the first reload still shows the old
// language (GWT reads the LOCALE cookie), then updates the cookie and reloads
// again, so poll across both loads.

const PREF = 'ui_language';

async function reloadAndWaitForLang(page: Page, lang: string): Promise<void> {
  await page.reload();
  await expect.poll(
    () => page.evaluate(() =>
      window.rstudio?.ready === true ? document.documentElement.lang : null),
    { timeout: 60000 },
  ).toBe(lang);
}

test.describe.serial('UI language', () => {
  let originalLanguage: string;

  test.beforeAll(async ({ rstudioPage: page }) => {
    originalLanguage = await getPref(page, PREF) as string;
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await setPref(page, PREF, originalLanguage);
    await reloadAndWaitForLang(page, originalLanguage);
  });

  test('root lang follows the UI language', async ({ rstudioPage: page }) => {
    await setPref(page, PREF, 'fr');
    await reloadAndWaitForLang(page, 'fr');

    await setPref(page, PREF, 'en');
    await reloadAndWaitForLang(page, 'en');
  });
});

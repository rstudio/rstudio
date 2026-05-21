import { test as base, type Page } from '@playwright/test';
import { launchRStudio, shutdownRStudio } from './desktop.fixture';
import { launchServer, shutdownServer } from './server.fixture';
import { getEnvironmentVersions, clearConsole } from '../pages/console_pane.page';

const DONT_SAVE_BTN = "button:has-text('Don\\'t Save'), button:has-text('Do not Save'), #rstudio_dlg_no";

type Mode = 'desktop' | 'server';

/** Capture R/RStudio versions once per worker and log them. */
async function logVersions(page: Page): Promise<void> {
  const versions = await getEnvironmentVersions(page);
  console.log(`R: ${versions.r}, RStudio: ${versions.rstudio}`);
  await clearConsole(page);
}

/**
 * Unified Playwright Test fixture that provides a shared RStudio page.
 *
 * The `mode` option is set per-project in playwright.config.ts; select with
 * `--project=desktop` (default) or `--project=server`.
 */
export const test = base.extend<{}, { mode: Mode; rstudioPage: Page }>({
  mode: ['desktop', { option: true, scope: 'worker' }],
  rstudioPage: [async ({ mode }, use) => {
    if (mode === 'server') {
      const session = await launchServer();
      await logVersions(session.page);
      await use(session.page);
      await shutdownServer(session);
    } else {
      const session = await launchRStudio();
      await logVersions(session.page);
      await use(session.page);
      await shutdownRStudio(session);
    }
  }, { scope: 'worker' }],
});

// Dismiss any leftover save dialogs before each test. A dialog left
// open by a previous test is already visible by the time we get here,
// so isVisible() (which does NOT wait) is enough -- a click with a
// 2-second timeout would poll for the full 2s in the (overwhelmingly
// common) no-dialog case, adding ~2s of dead wait to every test.
// After clicking, wait for the dialog to actually disappear rather
// than sleeping a fixed amount.
test.beforeEach(async ({ rstudioPage: page }) => {
  const dialog = page.locator(DONT_SAVE_BTN);
  if (await dialog.isVisible().catch(() => false)) {
    await dialog.click();
    await dialog.waitFor({ state: 'hidden', timeout: 5000 });
    console.log('Dismissed save dialog before test');
  }
});

export { expect } from '@playwright/test';

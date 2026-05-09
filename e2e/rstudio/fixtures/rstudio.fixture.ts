import { test as base, type Page } from '@playwright/test';
import { launchRStudio, shutdownRStudio } from './desktop.fixture';
import { launchServer, shutdownServer } from './server.fixture';
import { sleep } from '../utils/constants';
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

// Dismiss any leftover save dialogs before each test
test.beforeEach(async ({ rstudioPage: page }) => {
  try {
    await page.locator(DONT_SAVE_BTN).click({ timeout: 2000 });
    console.log('Dismissed save dialog before test');
    await sleep(500);
  } catch {
    // No dialog present
  }
});

export { expect } from '@playwright/test';

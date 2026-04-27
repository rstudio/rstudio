import { test as base, type Page } from '@playwright/test';
import { launchRStudio, shutdownRStudio } from './desktop.fixture';
import { launchServer, shutdownServer } from './server.fixture';
import { sleep } from '../utils/constants';

const mode = (process.env.RSTUDIO_EDITION || 'desktop').toLowerCase();

const DONT_SAVE_BTN = "button:has-text('Don\\'t Save'), button:has-text('Do not Save'), #rstudio_dlg_no";

/**
 * Unified Playwright Test fixture that provides a shared RStudio page.
 *
 * Set RSTUDIO_EDITION=server to connect to RStudio Server instead of Desktop.
 * Server mode requires RSTUDIO_USER, RSTUDIO_PASSWORD, and optionally RSTUDIO_SERVER_URL.
 */
export const test = base.extend<{}, { rstudioPage: Page }>({
  rstudioPage: [async ({}, use) => {
    if (mode === 'server') {
      const session = await launchServer();
      await use(session.page);
      await shutdownServer(session);
    } else {
      const session = await launchRStudio();
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

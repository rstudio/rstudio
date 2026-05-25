import { test as base, type Page } from '@playwright/test';
import { launchRStudio, shutdownRStudio } from './desktop.fixture';
import { launchServer, shutdownServer } from './server.fixture';
import { getEnvironmentVersions, clearConsole } from '../pages/console_pane.page';
import { resetForNextTest } from '../utils/test-reset';

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

// Reset the IDE to a clean per-test starting state. See utils/test-reset.ts
// for what's covered and what's deliberately not. Each step short-circuits
// when its trigger isn't present, so on a clean session this is cheap.
test.beforeEach(async ({ rstudioPage: page }) => {
  await resetForNextTest(page);
});

export { expect } from '@playwright/test';

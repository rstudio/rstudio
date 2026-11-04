import { test as base, type Page } from '@playwright/test';
import { CONSOLE_INPUT } from '../pages/console_pane.page';
import { writeStateMarker } from '../utils/a11y';
import { withDeadline } from '../utils/deadline';
import { collectRunVersions, publishRunVersions } from '../utils/versions';
import {
  openServerSession,
  RECOVER_CMD_TIMEOUT,
  serverLoginTimeout,
  shutdownServer,
} from './server.fixture';

/**
 * Fixture for accessibility scans: a new Server session per test, left as
 * the server delivered it. Deliberately independent of rstudio.fixture.ts --
 * no per-test reset, assistant guard, prefs, or client-exception drain --
 * since each of those would change the state being scanned.
 */

const SETTLE_TIMEOUT = 120_000;
const VERSIONS_TIMEOUT = 30_000;
// The rserver spawn (30 s), browser launch and navigation, the login-form
// probe, and shutdownServer (45 s graceful phase plus process cleanup).
const OVERHEAD = 180_000;

/**
 * Playwright gives a fixture one timeout slot for setup and teardown together.
 * It sums each phase's own bound, so a stuck phase fails with its own message:
 * sign-in, including CI's recovery command between two console waits, then
 * the settle waits and the version read.
 */
const FIXTURE_TIMEOUT = 2 * serverLoginTimeout() + RECOVER_CMD_TIMEOUT
  + 3 * SETTLE_TIMEOUT + VERSIONS_TIMEOUT + OVERHEAD;

/** Wait on concrete signals that the initial UI has settled; no fixed sleeps. */
async function waitForInitialState(page: Page): Promise<void> {
  await page.waitForFunction(() => window.rstudio?.ready === true, null, { timeout: SETTLE_TIMEOUT });
  await page.locator(CONSOLE_INPUT).waitFor({ state: 'visible', timeout: SETTLE_TIMEOUT });
  await page.locator('#rstudio_mb_files_touch_file').waitFor({ state: 'visible', timeout: SETTLE_TIMEOUT });
}

/**
 * Publish the versions for the report header. Runs after the scan because
 * reading the release name types into the console. Best-effort: a missing
 * header line is not worth failing a scan over.
 */
async function recordVersions(page: Page): Promise<void> {
  try {
    publishRunVersions(
      await withDeadline(collectRunVersions(page, 'server'), VERSIONS_TIMEOUT, 'version read'));
  } catch (err) {
    console.warn(`WARNING: could not record run versions: ${(err as Error).message}`);
  }
}

export const test = base.extend<{ a11yPage: Page }>({
  a11yPage: [async ({}, use, testInfo) => {
    writeStateMarker(testInfo);
    const session = await openServerSession();
    try {
      await waitForInitialState(session.page);
      await use(session.page);
      await recordVersions(session.page);
    } finally {
      await shutdownServer(session);
    }
  }, { timeout: FIXTURE_TIMEOUT }],
});

export { expect } from '@playwright/test';

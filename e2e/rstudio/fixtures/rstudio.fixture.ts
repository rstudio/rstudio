import { test as base, type Page } from '@playwright/test';
import { launchRStudio, shutdownRStudio } from './desktop.fixture';
import { launchServer, shutdownServer } from './server.fixture';
import { getEnvironmentVersions, clearConsole } from '../pages/console_pane.page';
import { drainClientExceptions } from '../utils/commands';
import { resetForNextTest } from '../utils/test-reset';
import { waitForUserConsoleInput } from '../utils/debug';

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
export const test = base.extend<{ perTestReset: void }, { mode: Mode; rstudioPage: Page }>({
  mode: ['desktop', { option: true, scope: 'worker' }],
  rstudioPage: [async ({ mode }, use) => {
    if (mode === 'server') {
      const session = await launchServer();
      await logVersions(session.page);
      await use(session.page);
      // Debug-only: keep the session alive after the last test so you can
      // keep inspecting; press Enter in the Console to quit. No-op otherwise.
      await waitForUserConsoleInput(session.page, 'quit RStudio');
      await shutdownServer(session);
    } else {
      const session = await launchRStudio();
      await logVersions(session.page);
      await use(session.page);
      // Debug-only: keep the session alive after the last test so you can
      // keep inspecting; press Enter in the Console to quit. No-op otherwise.
      await waitForUserConsoleInput(session.page, 'quit RStudio');
      await shutdownRStudio(session);
    }
  }, { scope: 'worker' }],

  // Reset the IDE to a clean per-test starting state. See utils/test-reset.ts
  // for what's covered and what's deliberately not. Each step short-circuits
  // when its trigger isn't present, so on a clean session this is cheap.
  //
  // This is an auto FIXTURE, not a module-scope test.beforeEach, very much on
  // purpose. Hooks registered at the top level of this (imported) module are
  // only attached to the suite of the FIRST spec file that loads the module
  // in each worker process -- Node caches the module, so its top-level
  // statements never re-run for the next spec file, and every later file in
  // the worker silently ran without any per-test reset. That is exactly how a
  // leaked pane maximize from one spec (an R Notebook preview maximizing the
  // Viewer on a short display) survived into the next spec's first test and
  // hid the Environment tab (#17952). Auto fixtures are part of the test type
  // itself, so they run for every test in every file regardless of module
  // caching.
  perTestReset: [async ({ rstudioPage: page }, use, testInfo) => {
    // Drain exceptions that arrived BEFORE this test (a previous test's
    // teardown, the gap between specs). They can't be attributed to the
    // upcoming test, so log them rather than fail it.
    const leftovers = await drainClientExceptions(page);
    for (const e of leftovers) {
      console.warn(
        `[client-exception] recorded between tests (not attributed): ${e.message}\n${e.stack}`,
      );
    }

    await resetForNextTest(page);

    // Debug-only: park the test (IDE clean and idle) so a human can arm
    // DevTools before the test body drives its scenario. Prompts in the
    // RStudio Console pane. No-op unless PW_DEBUG is set. See utils/debug.ts.
    await waitForUserConsoleInput(page, `run: ${testInfo.title}`);

    await use();

    // Any uncaught client exception raised while this test ran fails the
    // test, with the recorded stack in the failure output. The product
    // swallows these behind an "Error" dialog (message only), which the
    // next reset would silently dismiss -- a real product bug (like the
    // Plots-pane ImageFrame TypeError on short displays) could otherwise
    // hide behind passing tests indefinitely. PW_IGNORE_CLIENT_EXCEPTIONS=1
    // downgrades to a warning if a known benign exception must be tolerated
    // while a fix lands.
    const raised = await drainClientExceptions(page);
    if (raised.length > 0) {
      const detail = raised.map((e) => `${e.message}\n${e.stack}`).join('\n---\n');
      const ignore = ['1', 'true'].includes(
        (process.env.PW_IGNORE_CLIENT_EXCEPTIONS ?? '').toLowerCase(),
      );
      if (ignore) {
        console.warn(`[client-exception] during "${testInfo.title}" (ignored by env):\n${detail}`);
      } else {
        throw new Error(
          `${raised.length} uncaught client exception(s) during "${testInfo.title}":\n${detail}`,
        );
      }
    }
  }, { auto: true }],
});

export { expect } from '@playwright/test';

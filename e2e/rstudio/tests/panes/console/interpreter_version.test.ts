import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { restartSessionWithSentinel } from '@utils/project';

const INTERPRETER_VERSION = '#rstudio_console_interpreter_version_tabbed';
const RVERSION_RPC = /\/rpc\/get_rversion_info(?:\?|$)/;
const PING_RPC = /\/rpc\/ping(?:\?|$)/;

test.describe('Console interpreter version', () => {
  test('uses session info at startup without requesting the R version', async ({ rstudioPage: page }) => {
    const label = page.locator(INTERPRETER_VERSION);
    await expect(label).toHaveText(/^R \d+\.\d+/);
    const originalVersion = await label.textContent();
    let requests = 0;
    await page.route(RVERSION_RPC, async (route) => {
      requests++;
      await route.abort();
    });

    try {
      await page.reload();
      await page.waitForFunction(() => window.rstudio?.ready === true, null, {
        timeout: TIMEOUTS.sessionRestart,
        polling: 50,
      });

      await expect(label).toBeVisible();
      await expect(label).toHaveText(originalVersion!);
      expect(requests).toBe(0);
    } finally {
      await page.unroute(RVERSION_RPC);
    }
  });

  test('refreshes after restart and retains the refreshed version if the next RPC fails', async ({ rstudioPage: page }) => {
    const label = page.locator(INTERPRETER_VERSION);
    const refreshedVersion = await page.evaluate(() => `R ${window.rstudio!.version.rstudio}`);
    await expect(label).not.toHaveText(refreshedVersion);

    // A restart's completion ping can otherwise hit the old process while it
    // shuts down, preventing ConsoleRestartRCompletedEvent from being fired.
    await page.route(PING_RPC, async (route) => {
      await page.waitForFunction(() => window.rstudio?.ready === true, null, {
        timeout: TIMEOUTS.sessionRestart,
        polling: 50,
      });
      await route.continue();
    });

    let releaseRpc = () => {};
    const rpcHeld = new Promise<void>((resolve) => (releaseRpc = resolve));
    let requests = 0;
    let failRefresh = false;
    await page.route(RVERSION_RPC, async (route) => {
      requests++;
      if (!failRefresh)
        await rpcHeld;
      // Electron can expose route.fulfill() as HTTP status 0. Use real HTTP
      // responses: product info has a distinct version field, and an unknown
      // method gives us a JSON-RPC error without disrupting the connection.
      const method = failRefresh ? 'get_rversion_info_test_missing' : 'get_product_info';
      const request = route.request();
      await route.continue({
        url: request.url().replace('get_rversion_info', method),
        postData: JSON.stringify({ ...request.postDataJSON(), method }),
      });
    });

    try {
      const originalVersion = await label.textContent();
      await restartSessionWithSentinel(page);
      await expect.poll(() => requests).toBeGreaterThan(0);
      await expect(label).toHaveText(originalVersion!);
      releaseRpc();
      await expect(label).toHaveText(refreshedVersion);

      failRefresh = true;
      // Wait for the error callback itself, not just the HTTP response: the
      // label already has this text before the callback runs.
      await Promise.all([
        page.waitForEvent('console', {
          predicate: (message) => message.text().includes('Method not found'),
          timeout: TIMEOUTS.sessionRestart,
        }),
        restartSessionWithSentinel(page),
      ]);
      await expect(label).toHaveText(refreshedVersion);
    } finally {
      releaseRpc();
      await page.unroute(RVERSION_RPC);
      await page.unroute(PING_RPC);
      // Remove the synthetic cached version before the worker's next test.
      await page.reload();
      await page.waitForFunction(() => window.rstudio?.ready === true, null, {
        timeout: TIMEOUTS.sessionRestart,
        polling: 50,
      });
    }
  });
});

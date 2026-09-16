// The Console pane's interpreter label shows the R version straight away.
//
// It used to be created reading "(unknown)" and only picked up the real
// version when the get_rversion_info RPC answered. At startup that RPC waits
// behind whatever R is busy with, so the placeholder was what users saw for
// the first seconds of every session. The label now seeds itself from session
// info, which already carries the version.

import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';

const INTERPRETER_VERSION = '#rstudio_console_interpreter_version_tabbed';

// Match by regex per the Electron-CDP interception guidance (glob patterns
// can miss query strings).
const RVERSION_RPC = /\/rpc\/get_rversion_info/;

test.describe('Console interpreter version', () => {
  test('is labelled before the R version RPC answers', async ({ rstudioPage: page }) => {
    // Hold get_rversion_info open for the whole check, standing in for the
    // busy session that delays it at startup.
    let releaseRpc = () => {};
    const rpcHeld = new Promise<void>((resolve) => (releaseRpc = resolve));
    await page.route(RVERSION_RPC, async (route) => {
      await rpcHeld;
      await route.continue().catch(() => {});
    });

    try {
      await page.reload();
      await page.waitForFunction(() => window.rstudio?.ready === true, null, {
        timeout: TIMEOUTS.sessionRestart,
        polling: 50,
      });

      const label = page.locator(INTERPRETER_VERSION);
      await expect(label).toBeVisible();
      await expect(label).toHaveText(/^R \d+\.\d+/, { timeout: 2000 });
    } finally {
      releaseRpc();
      await page.unroute(RVERSION_RPC);
    }

    // The RPC response, once it lands, agrees with what we already showed.
    await expect(page.locator(INTERPRETER_VERSION)).toHaveText(/^R \d+\.\d+/);
  });
});

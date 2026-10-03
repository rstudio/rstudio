import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { collectClientStatePushes, waitForActiveDocIdPush } from '@utils/client-state';
import { TIMEOUTS } from '@utils/constants';

const HTML_CAPABILITIES_RPC = /\/rpc\/get_html_capabilities(?:\?|$)/;

test.describe('Restored editor prerequisites', () => {
  const sandbox = useSuiteSandbox();

  test('waits for HTML capabilities before checking an R presentation', async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    const knitrAvailable = await consoleActions.evalRLogical(
      'requireNamespace("knitr", quietly = TRUE) && packageVersion("knitr") >= "1.2"',
    );
    test.skip(knitrAvailable !== true, 'Requires knitr >= 1.2');

    const file = `capabilities_restore_${Date.now()}.Rpres`;
    const clientStatePushes = collectClientStatePushes(page);
    await writeAndOpenFile(page, sandbox.dir, file, 'Presentation\n============\n\nSlide\n============\n');
    const docId = await page.evaluate(() => window.rstudio?.documents.active()?.id ?? null);
    expect(docId).not.toBeNull();
    await waitForActiveDocIdPush(page, clientStatePushes, docId!);

    let releaseRpc = () => {};
    const rpcHeld = new Promise<void>((resolve) => (releaseRpc = resolve));
    let requests = 0;
    await page.route(HTML_CAPABILITIES_RPC, async (route) => {
      requests++;
      await rpcHeld;
      await route.continue();
    });

    const warning = page.getByText('R Presentations requires the knitr package', { exact: false });
    try {
      await page.reload();
      await page.waitForFunction(() => window.rstudio?.ready === true, null, {
        timeout: TIMEOUTS.sessionRestart,
        polling: 50,
      });
      await expect.poll(() => requests).toBeGreaterThan(0);
      await expect.poll(() => page.evaluate(() => window.rstudio?.documents.active()?.id))
        .toBe(docId);
      // The editor is restored while the real capability probe is still held.
      await expect(warning).not.toBeVisible();

      const response = page.waitForResponse(HTML_CAPABILITIES_RPC);
      releaseRpc();
      expect((await (await response).json()).result.r_markdown_supported).toBe(true);
      await expect(warning).not.toBeVisible();
    } finally {
      releaseRpc();
      await page.unroute(HTML_CAPABILITIES_RPC);
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [file]);
    }
  });
});

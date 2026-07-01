import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { dismissBlockingModals } from '@pages/modals.page';
import { waitForSessionRestart } from '@utils/project';
import { rStringLiteral } from '@utils/r';
import { executeCommand } from '@utils/commands';
import type { Page } from 'playwright';

async function captureResult(page: Page, rExpression: string): Promise<string> {
  const marker = `__SUSP_${Date.now()}__`;
  // { wait: true } gates on R reporting idle, which is the deterministic
  // post-condition for "cat() has finished writing both markers".
  await executeInConsole(
    page,
    `cat(${rStringLiteral(marker)}, ${rExpression}, ${rStringLiteral(marker)})`,
    { wait: true },
  );

  const pattern = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`, 's');
  const output = await page.locator(CONSOLE_OUTPUT).innerText();
  const match = output.match(pattern);
  if (!match) throw new Error(`captureResult: markers not found for "${rExpression}"`);
  return match[1].trim();
}

async function suspendAndResume(page: Page): Promise<void> {
  await executeCommand(page, 'suspendSession');
  await waitForSessionRestart(page);

  // Defensive: a stray modal that pops up during resume renders a
  // gwt-PopupPanelGlass overlay that then blocks the console tab and wedges the
  // next executeInConsole with an opaque pointer-intercept timeout. The one
  // trigger we know of -- reticulate's unguarded here::here() throwing "No root
  // directory found" from the rootless sandbox HOME (rstudio/reticulate#1909),
  // surfacing as an "Error Listing Packages" dialog on resume -- is prevented at
  // the source by the `.here` marker seeded into the sandbox home (see
  // fixtures/sandbox-setup.ts). This clear stays as a cheap safety net for any
  // other out-of-band dialog, since these tests exercise search-path
  // preservation, not whatever pane raised it.
  await dismissBlockingModals(page);
}

// Suspend/resume relies on the Server reconnection path. Desktop has no
// equivalent flow, so these tests are tagged @server_only.
test.describe.serial('Session suspend/resume', { tag: ['@server_only'] }, () => {
  test('loaded packages are preserved on suspend + resume', async ({ rstudioPage: page }) => {
    await executeInConsole(page, 'library(tools)', { wait: true });

    await suspendAndResume(page);

    const stillLoaded = await captureResult(page, '"tools" %in% loadedNamespaces()');
    expect(stillLoaded, 'tools should still be loaded after resume').toBe('TRUE');
  });

  test('attached datasets are preserved on suspend + resume', async ({ rstudioPage: page }) => {
    await executeInConsole(
      page,
      'attach(list(apple = 1, banana = 2, cherry = 3), name = "my-attached-dataset")',
      { wait: true },
    );

    await suspendAndResume(page);

    try {
      const inSearch = await captureResult(
        page,
        '"my-attached-dataset" %in% search()',
      );
      expect(inSearch, 'attached dataset should remain on the search path').toBe('TRUE');

      const sum = await captureResult(page, 'apple + banana + cherry');
      expect(sum, 'attached values should still resolve').toBe('6');
    } finally {
      await executeInConsole(
        page,
        'try(detach("my-attached-dataset"), silent = TRUE)',
        { wait: true },
      );
    }
  });
});

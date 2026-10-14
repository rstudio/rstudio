import type { Page, Request } from '@playwright/test';
import { test, expect } from '@fixtures/a11y.fixture';
import { scanA11y, violationSummary } from '@utils/a11y';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const PACKAGES_PANEL = '#rstudio_workbench_panel_packages';
const FILES_TOOLBAR_BUTTON = '#rstudio_mb_files_touch_file';
// A pane's selected tab is saved under this client-state group, which
// outlives the session -- see PaneManager.SelectedTabStateValue.
const TAB_STATE = '"workbench-pane"';
// The tab switch only schedules a passive save, 5 s out (ClientStateUpdater).
const TAB_STATE_SAVE_TIMEOUT = 15_000;

const isTabStateSave = (r: Request) =>
  r.url().includes('set_client_state') && (r.postData() ?? '').includes(TAB_STATE);

/**
 * Switch back to Files and make sure the server has it. The client only
 * sends tab state that changed since its last save, so if Packages was never
 * sent, Files is still what's stored. If it was, wait for the save that
 * replaces it: the next state's session would otherwise open on Packages.
 */
async function restoreFilesTab(page: Page, packagesSaved: () => boolean): Promise<void> {
  await executeCommand(page, 'activateFiles');
  await page.locator(FILES_TOOLBAR_BUTTON).waitFor({ state: 'visible', timeout: TIMEOUTS.consoleReady });
  if (!packagesSaved())
    return;
  const request = await page.waitForRequest(isTabStateSave, { timeout: TAB_STATE_SAVE_TIMEOUT });
  const response = await request.response();
  // A rejected RPC still answers HTTP 200, with an error member.
  expect(await response?.text(), 'saving the Files tab selection').not.toContain('"error"');
}

/** The initial state with the Packages pane shown in place of Files (Ctrl+7). */
test('packages pane has no WCAG 2.2 AA violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    let packagesSaved = false;
    const onRequest = (r: Request) => { packagesSaved ||= isTabStateSave(r); };
    page.on('request', onRequest);

    await executeCommand(page, 'activatePackages');
    let results;
    try {
      // stats is always attached, so its checked box means the list has loaded.
      await expect(page.locator(`${PACKAGES_PANEL} input[type='checkbox'][aria-label='stats']`).first())
        .toBeChecked({ timeout: TIMEOUTS.consoleReady });
      results = await scanA11y(page, testInfo);
    } finally {
      page.off('request', onRequest);
      await restoreFilesTab(page, () => packagesSaved);
    }
    expect(violationSummary(results)).toEqual([]);
  });

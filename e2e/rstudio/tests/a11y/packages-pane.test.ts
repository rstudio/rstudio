import type { Page, Request } from '@playwright/test';
import { test, expect } from '@fixtures/a11y.fixture';
import { scanA11y, violationSummary } from '@utils/a11y';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const PACKAGES_PANEL = '#rstudio_workbench_panel_packages';
const FILES_TOOLBAR_BUTTON = '#rstudio_mb_files_touch_file';
// The tab switch only schedules a passive save, 5 s out (ClientStateUpdater).
const TAB_STATE_SAVE_TIMEOUT = 15_000;

/** Selected tab index by tabset, e.g. { TabSet2: 2 }. */
type TabState = Record<string, number>;

/**
 * The tab selections a request saves, if any. A pane's selected tab is
 * project-persistent client state (the third set_client_state parameter)
 * that outlives the session -- see PaneManager.SelectedTabStateValue.
 */
function savedTabState(r: Request): TabState | undefined {
  if (!r.url().includes('set_client_state'))
    return undefined;
  return r.postDataJSON()?.params?.[2]?.['workbench-pane'];
}

/**
 * Switch back to Files and wait for the save that replaces the Packages
 * selection: the next state's session would otherwise open on Packages. The
 * client sends only values that changed, so that save is the next one to
 * carry a different index for a tabset the Packages save set.
 */
async function restoreFilesTab(page: Page, packagesSave: Promise<Request>): Promise<void> {
  const packages = savedTabState(await packagesSave)!;
  const filesSave = page.waitForRequest((r) => {
    const saved = savedTabState(r);
    return saved !== undefined
      && Object.entries(packages).some(([tabset, index]) => tabset in saved && saved[tabset] !== index);
  }, { timeout: TAB_STATE_SAVE_TIMEOUT });
  const [request] = await Promise.all([filesSave, (async () => {
    await executeCommand(page, 'activateFiles');
    await page.locator(FILES_TOOLBAR_BUTTON).waitFor({ state: 'visible', timeout: TIMEOUTS.consoleReady });
  })()]);
  const response = await request.response();
  // A rejected RPC still answers HTTP 200, with an error member.
  expect(await response?.text(), 'saving the Files tab selection').not.toContain('"error"');
}

/** The initial state with the Packages pane shown in place of Files (Ctrl+7). */
test('packages pane has no WCAG 2.2 AA violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    await executeCommand(page, 'activatePackages');
    // The command selects the tab synchronously, so any tab-state save from
    // here on carries Packages. Waiting for one costs up to a timer tick, but
    // tells the restore which value it must see replaced.
    const packagesSave = page.waitForRequest(
      (r) => savedTabState(r) !== undefined, { timeout: TAB_STATE_SAVE_TIMEOUT });
    // Awaited in restoreFilesTab; this keeps a timeout during the scan from
    // surfacing as an unhandled rejection first.
    packagesSave.catch(() => {});
    let results;
    try {
      // stats is always attached, so its checked box means the list has loaded.
      await expect(page.locator(`${PACKAGES_PANEL} input[type='checkbox'][aria-label='stats']`).first())
        .toBeChecked({ timeout: TIMEOUTS.consoleReady });
      results = await scanA11y(page, testInfo);
    } finally {
      await restoreFilesTab(page, packagesSave);
    }
    expect(violationSummary(results)).toEqual([]);
  });

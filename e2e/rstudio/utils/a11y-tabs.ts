import { expect, type Page, type Request } from '@playwright/test';
import { executeCommand } from './commands';
import { TIMEOUTS } from './constants';

const FILES_TOOLBAR_BUTTON = '#rstudio_mb_files_touch_file';
// The tab switch only schedules a passive save, 5 s out (ClientStateUpdater).
const TAB_STATE_SAVE_TIMEOUT = 15_000;

/**
 * Whether a request saves tab selections. A pane's selected tab is
 * project-persistent client state (the third set_client_state parameter)
 * that outlives the session -- see PaneManager.SelectedTabStateValue.
 */
function isTabStateSave(r: Request): boolean {
  return r.url().includes('set_client_state')
    && r.postDataJSON()?.params?.[2]?.['workbench-pane'] !== undefined;
}

/**
 * Switch back to Files and wait for the save that replaces the other tab's
 * selection: the next state's session would otherwise open on that tab. The
 * client sends only values that changed, so once the other tab has been sent,
 * the next tab-state save is the Files one.
 */
async function restoreFilesTab(page: Page, tabSave: Promise<Request>): Promise<void> {
  // The timer sends a pending change within 5 s, so a timeout here means the
  // tab's save went out before the wait began. Restore either way; a stalled
  // timer fails the Files wait below instead (unless its late save of the
  // other tab lands just before Files is activated, and is taken for it).
  await tabSave.catch((err: Error) => console.warn(`Tab selection save not seen: ${err.message}`));
  const filesSave = page.waitForRequest(isTabStateSave, { timeout: TAB_STATE_SAVE_TIMEOUT });
  const [request] = await Promise.all([filesSave, (async () => {
    await executeCommand(page, 'activateFiles');
    await page.locator(FILES_TOOLBAR_BUTTON).waitFor({ state: 'visible', timeout: TIMEOUTS.consoleReady });
  })()]);
  const response = await request.response();
  // A rejected RPC still answers HTTP 200, with an error member.
  expect(await response?.text(), 'saving the Files tab selection').not.toContain('"error"');
}

/**
 * Run `body` with another tab of the Files tabset selected, then select Files
 * again and wait for that selection to be saved. `select` must lead to `tab`
 * (e.g. `#rstudio_workbench_tab_help`) being selected; until it is, nothing
 * has changed, so a failure there needs no restore.
 */
export async function withFilesTabsetTab<T>(
  page: Page,
  tab: string,
  select: () => Promise<void>,
  body: () => Promise<T>,
): Promise<T> {
  await select();
  await expect(page.locator(tab)).toHaveAttribute('aria-selected', 'true', { timeout: TIMEOUTS.consoleReady });
  // Any tab-state save from here on carries the new tab. Waiting for it costs
  // up to a timer tick, but guarantees switching back produces a save to wait
  // for. Awaited in restoreFilesTab; this keeps a timeout during `body` from
  // surfacing as an unhandled rejection first.
  const tabSave = page.waitForRequest(isTabStateSave, { timeout: TAB_STATE_SAVE_TIMEOUT });
  tabSave.catch(() => {});
  try {
    return await body();
  } finally {
    await restoreFilesTab(page, tabSave);
  }
}

import { expect, type Page, type Request } from '@playwright/test';
import { executeCommand } from './commands';
import { TIMEOUTS } from './constants';

/** A tabset's tab in the default layout, and how to select it again. */
interface HomeTab {
  name: string;
  tab: string;
  command: string;
  // Visible once the home tab is showing again.
  ready: string;
}

const FILES_HOME: HomeTab = {
  name: 'Files',
  tab: '#rstudio_workbench_tab_files',
  command: 'activateFiles',
  ready: '#rstudio_mb_files_touch_file',
};

const ENVIRONMENT_HOME: HomeTab = {
  name: 'Environment',
  tab: '#rstudio_workbench_tab_environment',
  command: 'activateEnvironment',
  ready: '#rstudio_workbench_panel_environment',
};

// The tab switch only schedules a passive save, 5 s out (ClientStateUpdater).
const TAB_STATE_SAVE_TIMEOUT = 15_000;

/**
 * Whether a request saves tab selections. A pane's selected tab is
 * project-persistent client state (the third set_client_state parameter)
 * that outlives the session -- see PaneManager.SelectedTabStateValue. Every
 * tabset saves under the same group.
 */
function isTabStateSave(r: Request): boolean {
  return r.url().includes('set_client_state')
    && r.postDataJSON()?.params?.[2]?.['workbench-pane'] !== undefined;
}

/**
 * Switch back to the home tab and wait for the save that replaces the other
 * tab's selection: the next state's session would otherwise open on that tab.
 * The client sends only values that changed, so once the other tab has been
 * sent, the next tab-state save is the home one.
 */
async function restoreHomeTab(page: Page, home: HomeTab, tabSave: Promise<Request>): Promise<void> {
  // The timer sends a pending change within 5 s, so a timeout here means the
  // tab's save went out before the wait began. Restore either way; a stalled
  // timer fails the home wait below instead (unless its late save of the
  // other tab lands just before home is activated, and is taken for it).
  await tabSave.catch((err: Error) => console.warn(`Tab selection save not seen: ${err.message}`));
  const homeSave = page.waitForRequest(isTabStateSave, { timeout: TAB_STATE_SAVE_TIMEOUT });
  const [request] = await Promise.all([homeSave, (async () => {
    await executeCommand(page, home.command);
    await page.locator(home.ready).waitFor({ state: 'visible', timeout: TIMEOUTS.consoleReady });
  })()]);
  const response = await request.response();
  // A rejected RPC still answers HTTP 200, with an error member.
  expect(await response?.text(), `saving the ${home.name} tab selection`).not.toContain('"error"');
}

/**
 * Restore the home tab once the test has already failed. `tabSave` is unset
 * when the other tab was never confirmed selected, in which case home may
 * never have been left.
 */
async function restoreAfterFailure(
  page: Page,
  home: HomeTab,
  tabSave: Promise<Request> | undefined,
): Promise<void> {
  if (tabSave === undefined) {
    const homeSelected = await page.locator(home.tab).getAttribute('aria-selected', { timeout: TIMEOUTS.action });
    if (homeSelected === 'true')
      return;
  }
  await restoreHomeTab(page, home,
    tabSave ?? page.waitForRequest(isTabStateSave, { timeout: TAB_STATE_SAVE_TIMEOUT }));
}

async function withTabsetTab<T>(
  page: Page,
  home: HomeTab,
  tab: string,
  select: () => Promise<void>,
  body: () => Promise<T>,
): Promise<T> {
  let tabSave: Promise<Request> | undefined;
  let result: T;
  try {
    await select();
    await expect(page.locator(tab)).toHaveAttribute('aria-selected', 'true', { timeout: TIMEOUTS.consoleReady });
    // Any tab-state save from here on carries the new tab. Waiting for it
    // costs up to a timer tick, but guarantees switching back produces a save
    // to wait for. Awaited in restoreHomeTab; this keeps a timeout during
    // `body` from surfacing as an unhandled rejection first.
    tabSave = page.waitForRequest(isTabStateSave, { timeout: TAB_STATE_SAVE_TIMEOUT });
    tabSave.catch(() => {});
    result = await body();
  } catch (err) {
    await restoreAfterFailure(page, home, tabSave)
      .catch((restoreErr: Error) => console.warn(`${home.name} tab not restored: ${restoreErr.message}`));
    throw err;
  }
  await restoreHomeTab(page, home, tabSave);
  return result;
}

/**
 * Run `body` with another tab of the Files tabset selected, then select Files
 * again and wait for that selection to be saved. `select` must lead to `tab`
 * (e.g. `#rstudio_workbench_tab_help`) being selected. If the test fails
 * first, a restore problem is only logged, so it can't hide the original error.
 */
export async function withFilesTabsetTab<T>(
  page: Page,
  tab: string,
  select: () => Promise<void>,
  body: () => Promise<T>,
): Promise<T> {
  return withTabsetTab(page, FILES_HOME, tab, select, body);
}

/** As `withFilesTabsetTab`, for a tab of the Environment tabset. */
export async function withEnvironmentTabsetTab<T>(
  page: Page,
  tab: string,
  select: () => Promise<void>,
  body: () => Promise<T>,
): Promise<T> {
  return withTabsetTab(page, ENVIRONMENT_HOME, tab, select, body);
}

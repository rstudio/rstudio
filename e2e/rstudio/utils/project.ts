import type { Page } from 'playwright';
import {
  executeInConsole,
  waitForConsoleIdle,
  CONSOLE_TAB,
  CONSOLE_INPUT,
  CONSOLE_OUTPUT,
} from '../pages/console_pane.page';
import { executeCommand, openProject } from './commands';
import { sleep, TIMEOUTS } from './constants';

// Re-exported from pages/console_pane.page.ts; preserved here so existing
// `import { waitForConsoleIdle } from '@utils/project'` call sites keep working.
export { waitForConsoleIdle };

const PROJECT_MENU = '#rstudio_project_menubutton_toolbar';
const CLOSE_PROJECT_MENU_ITEM = '#rstudio_label_close_project_command';

/**
 * Wait for the page to settle after a session restart: page load, console
 * input visible, rstudioapi defined. Shared by both the sentinel-based and
 * typing-based restart helpers.
 */
async function waitForPostRestartReady(page: Page): Promise<void> {
  await page.waitForLoadState('load', { timeout: 30000 }).catch(() => {});
  await sleep(3000);
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: 60000 });
  await sleep(2000);

  await page.waitForFunction(
    'typeof window.rstudioapi !== "undefined" || typeof window.$RStudio !== "undefined"',
    null,
    { timeout: 15000 }
  ).catch(() => {});
  await sleep(1000);
}

/**
 * Restart the R session and wait for the new session to be fully ready.
 *
 * Goes through the `restartR` AppCommand via the automation bridge -- NOT
 * `.rs.api.restartSession` typed into the console. Earlier versions of this
 * helper used the console route, but a follow-up `console_input` RPC could
 * race the in-flight restart: the server would try to forward the next input
 * to R, find R mid-shutdown, and surface as
 *   Error: Unable to establish connection with R session when executing 'console_input'
 * Driving through the AppCommand keeps the restart out of the console_input
 * pipeline entirely.
 *
 * Readiness is signalled via `window.rstudio.ready`: the GWT bridge resets it
 * to false on `RestartStatusEvent.RESTART_INITIATED` and back to true on the
 * new session's `DeferredInitCompletedEvent`. We pre-reset it ourselves to
 * close the window between executeCommand returning and GWT's own reset
 * landing, so a stale "true" can't sneak through.
 *
 * Use this when the test owns the restart. For project-open and other
 * implicit restarts (where the test can't inject readiness signalling)
 * callers use waitForSessionRestart instead.
 */
export async function restartSessionWithSentinel(page: Page): Promise<void> {
  // Force-reset the readiness flag so the wait below sees a clean transition.
  // RestartStatusEvent.RESTART_INITIATED also resets it, but that fires after
  // the AppCommand handler runs -- without this preemptive reset, a poll that
  // fires between executeCommand returning and the event landing could see
  // the prior session's stale "true".
  await page.evaluate(() => {
    if (window.rstudio) window.rstudio.ready = false;
  });

  await executeCommand(page, 'restartR');

  // window.rstudio.ready flips to true on DeferredInitCompletedEvent, which
  // is GWT's signal that the new session has finished its deferred init
  // (workspace + search-path restore, modules sourced). 60s covers the
  // full restart + package restore on a slow worker.
  await page.waitForFunction(
    () => window.rstudio?.ready === true,
    null,
    { timeout: 60000, polling: 50 },
  );

  // The ready flag tells us GWT's workbench is wired up, but the console-
  // busy class can still be set briefly while the post-restart prompt
  // transition completes. Wait for it to clear so callers immediately
  // following with executeInConsole / executeCommand see an idle session.
  await waitForConsoleIdle(page);
}

/**
 * Wait for the R session to settle after an implicit restart (project switch,
 * restartR IDE command). The IDE reloads and the console becomes briefly
 * unavailable; this best-effort sequence waits for the page to load, the
 * console input to reappear, and R to confirm idle by echoing a unique marker.
 *
 * Logs a warning and returns if R does not confirm idle within three attempts;
 * the caller decides how to proceed.
 *
 * Prefer restartSessionWithSentinel when the test is the one invoking
 * .rs.api.restartSession -- the sentinel-based confirmation avoids the
 * post-restart focus race this helper has to work around.
 */
export async function waitForSessionRestart(page: Page): Promise<void> {
  await waitForPostRestartReady(page);

  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const marker = `[pw:session-restart-ready ${Date.now()}]`;
      await page.locator(CONSOLE_TAB).click();
      await page.locator(CONSOLE_INPUT).click({ force: true });
      await sleep(200);
      await page.locator(CONSOLE_INPUT).pressSequentially(`cat("${marker}")`);
      await page.locator(CONSOLE_INPUT).press('Enter');
      await sleep(1500);
      const output = await page.locator(CONSOLE_OUTPUT).innerText();
      if (output.includes(marker)) return;
    } catch (e) {
      console.warn(`waitForSessionRestart attempt ${attempt}: ${(e as Error).message}`);
    }
    await sleep(2000);
  }
  console.warn('waitForSessionRestart: R session did not confirm idle after 3 attempts');
}

/**
 * Create a fresh project directory inside `parentDir`, write a minimal
 * `.Rproj` file, and open the project in RStudio via the automation bridge.
 *
 * The project file is written R-side via the console so this works in Server
 * mode where the rsession host may not share a filesystem with the test
 * runner. The actual project switch goes through `openProject` (which fires
 * SwitchToProjectEvent on the GWT side), so readiness is signalled by the
 * bridge's `window.rstudio.ready` flag -- no `.Rprofile` sentinel marker
 * needed.
 *
 * Returns the absolute project directory path. Callers must reconstruct any
 * page-action wrappers held over this call; the session restart invalidates
 * them.
 */
export async function createAndOpenProject(
  page: Page,
  parentDir: string,
  name: string,
): Promise<string> {
  const parentDirR = parentDir.replace(/\\/g, '/');
  const projectDir = `${parentDirR}/${name}`;
  const rprojPath = `${projectDir}/${name}.Rproj`;

  // Create the directory and .Rproj in a single console roundtrip so we only
  // wait for one prompt-return before driving the project switch.
  await executeInConsole(
    page,
    `{ dir.create("${projectDir}"); writeLines(c("Version: 1.0", "", "RestoreWorkspace: Default", "SaveWorkspace: Default"), "${rprojPath}") }`
  );
  await waitForConsoleIdle(page);

  await openProject(page, rprojPath);

  return projectDir;
}

/**
 * Close the currently open project, if any, via the project toolbar menu.
 *
 * Drives the close through the UI (toolbar menu click) rather than R-side
 * (`.rs.api.executeCommand('closeProject')` typed into the console). The
 * R-side path leaves the session marked busy while the close runs, which
 * triggers RStudio's "R session is currently busy. Are you sure you want to
 * quit?" confirmation dialog and hangs the test.
 *
 * "Close project" is effectively a workbench rebuild -- the same Electron
 * window swaps out the project-bound page for a fresh no-project page. So we
 * wait not just for the console to come back, but for the new
 * `window.rstudio` automation bridge to finish installing and any modal
 * error from a too-early Files pane query ("Error navigating to ~") to have
 * surfaced and been dismissed. Without this, the caller's next action can
 * land while the new workbench is still mid-init.
 *
 * No-op when no project is open (the toolbar label is "Project: (None)" or
 * the button isn't rendered).
 */
export async function closeProjectIfOpen(page: Page): Promise<void> {
  const menu = page.locator(PROJECT_MENU);
  const label = (await menu.innerText().catch(() => '')).trim();
  if (label.includes('(None)') || label === '')
    return;

  await menu.click();
  await page.locator(CLOSE_PROJECT_MENU_ITEM).click();
  await page.waitForLoadState('load', { timeout: TIMEOUTS.sessionRestart }).catch(() => {});
  await page.waitForSelector(CONSOLE_INPUT, {
    state: 'visible',
    timeout: TIMEOUTS.sessionRestart,
  });
  await waitForConsoleIdle(page);

  // Wait for the new no-project session to finish installing its automation
  // bridge AND report no active project. The bridge re-installs when the
  // workbench rebuilds; `project.isActive()` returns false only once the new
  // SessionInfo has propagated. Polling on both signals catches the case
  // where the bridge is from the previous (project-bound) session.
  await page.waitForFunction(
    () => window.rstudio?.project?.isActive() === false,
    null,
    { timeout: TIMEOUTS.sessionRestart, polling: 100 },
  );

  // Dismiss any modal error dialog that surfaced while the new workbench
  // was initializing -- e.g. a Files-pane refresh racing the rsession's
  // home-dir hand-off can show "Error navigating to ~". Click whatever
  // OK button is on top; absence is fine.
  const okButton = page.locator('button:has-text("OK")').first();
  if (await okButton.isVisible({ timeout: 500 }).catch(() => false))
    await okButton.click();
}

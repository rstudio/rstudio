import type { Page } from 'playwright';
import { typeInConsole, CONSOLE_TAB, CONSOLE_INPUT, CONSOLE_OUTPUT } from '../pages/console_pane.page';
import { sleep, TIMEOUTS } from './constants';

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
 * Restart the R session and wait for the new session to confirm idle by
 * echoing a sentinel string. The sentinel is passed as the `command` argument
 * to .rs.api.restartSession, so R itself prints it as part of startup -- no
 * post-restart typing dance, which would otherwise be subject to Ace focus
 * races.
 *
 * The sentinel appearing IS the readiness signal: no fixed pre-restart sleeps
 * are needed, and polling can be tight.
 *
 * Use this when the test owns the restart. For project-open and "restartR"
 * IDE-command paths, the restart is implicit and the sentinel can't be
 * injected; those callers use waitForSessionRestart instead.
 */
/**
 * Poll the console output for a sentinel string, returning as soon as it
 * appears. Throws if not seen within `timeoutMs`. Used by both the explicit
 * restart helper and createAndOpenProject.
 */
async function pollForMarker(page: Page, marker: string, timeoutMs: number): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      const output = await page.locator(CONSOLE_OUTPUT).innerText();
      if (output.includes(marker)) return;
    } catch { /* console output transiently detached during restart */ }
    await sleep(100);
  }
  throw new Error(`pollForMarker: sentinel "${marker}" not seen within ${timeoutMs}ms`);
}

export async function restartSessionWithSentinel(page: Page): Promise<void> {
  const marker = `__READY_${Date.now()}__`;
  // Deferred (default eager = FALSE): the sentinel fires after workspace and
  // search-path restore complete, so when we observe the marker R is fully
  // initialized and ready for follow-up automation commands. The visible
  // "pause" between restart and marker reflects real package-restore work --
  // we accept it as the price of a reliable readiness signal. See
  // src/cpp/r/session/RSessionState.cpp:920-956 for the dispatch logic.
  await typeInConsole(page, `.rs.api.restartSession(command = "cat('${marker}')")`);
  await pollForMarker(page, marker, 30000);
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
      const marker = `__READY_${Date.now()}__`;
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
 * `.Rproj` file plus a `.Rprofile` that prints a startup sentinel, and open
 * the project in RStudio. The implicit session restart triggered by
 * .rs.api.openProject() can't accept a `command` argument like restartSession
 * can, so the sentinel is delivered via .Rprofile -- R sources it on startup
 * and prints the marker, which the test then polls for. This avoids the
 * post-restart focus race that plagues typing-based readiness probes.
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
  const marker = `__READY_${Date.now()}__`;

  await typeInConsole(page, `dir.create("${projectDir}")`);
  await sleep(500);
  await typeInConsole(
    page,
    `writeLines(c("Version: 1.0", "", "RestoreWorkspace: Default", "SaveWorkspace: Default"), "${projectDir}/${name}.Rproj")`
  );
  await sleep(500);
  await typeInConsole(page, `writeLines("cat('${marker}')", "${projectDir}/.Rprofile")`);
  await sleep(500);

  await typeInConsole(page, `.rs.api.openProject("${projectDir}/${name}.Rproj")`);
  // The page may navigate on Server mode; let it settle before polling.
  await page.waitForLoadState('load', { timeout: 30000 }).catch(() => {});
  await pollForMarker(page, marker, 60000);

  return projectDir;
}

/**
 * Wait for the console input to clear its "busy" class. Use after explicit
 * session restarts and project opens to make sure the next R-side action
 * isn't enqueued behind a long-running startup command.
 */
export async function waitForConsoleIdle(page: Page): Promise<void> {
  await page.waitForFunction(
    () => {
      const el = document.getElementById('rstudio_console_input');
      return !!el && !el.classList.contains('rstudio-console-busy');
    },
    null,
    { timeout: TIMEOUTS.sessionRestart, polling: 100 },
  );
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
 * No-op when no project is open (the toolbar label is "Project: (None)" or
 * the button isn't rendered).
 */
export async function closeProjectIfOpen(page: Page): Promise<void> {
  const menu = page.locator(PROJECT_MENU);
  const label = (await menu.innerText().catch(() => '')).trim();
  if (label.includes('(None)') || label === '') return;

  await menu.click();
  await page.locator(CLOSE_PROJECT_MENU_ITEM).click();
  await page.waitForLoadState('load', { timeout: TIMEOUTS.sessionRestart }).catch(() => {});
  await page.waitForSelector(CONSOLE_INPUT, {
    state: 'visible',
    timeout: TIMEOUTS.sessionRestart,
  });
  await waitForConsoleIdle(page);
}

import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { executeInConsole, waitForConsoleIdle, CONSOLE_INPUT, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { installDepIfPrompted } from '@pages/modals.page';
import { SourcePane } from '@pages/source_pane.page';
import { useSuiteSandbox, SANDBOX_DIR_PREFIX } from '@utils/sandbox';
import { setPref, resetSourcePaneState } from '@utils/commands';
import type { Page } from 'playwright';

// -- Selectors ----------------------------------------------------------------

const PALETTE_LIST = '#rstudio_command_palette_list';
const WIZARD_DIALOG = '.gwt-DialogBox[aria-label="New Project Wizard"]';
const NEW_DIRECTORY_OPTION = '#rstudio_label_new_directory_wizard_page';
const CREATE_PROJECT_BTN = '#rstudio_label_create_project_wizard_confirm';
const PROJECT_MENU = '#rstudio_project_menubutton_toolbar';
const CLOSE_PROJECT_MENU_ITEM = '#rstudio_label_close_project_command';

// -- Project type configs -----------------------------------------------------

type ProjectType = {
  name: string;
  wizardPageId: string;
  dirInputId: string;
  expectedFile: string | null;
};

const NEW_PROJECT: ProjectType = {
  name: 'new_project_test_project',
  wizardPageId: '#rstudio_label_new_project_wizard_page',
  dirInputId: '#rstudio_new_project_directory_name',
  expectedFile: null,
};

// R package names cannot contain underscores (letters/digits/dots only,
// starting with a letter), so this one can't follow the snake_case convention.
const R_PACKAGE: ProjectType = {
  name: 'RPackageTestProject',
  wizardPageId: '#rstudio_label_r_package_wizard_page',
  dirInputId: '#rstudio_r_package_directory_name',
  expectedFile: 'hello.R',
};

const SHINY_APP: ProjectType = {
  name: 'shiny_app_test_project',
  wizardPageId: '#rstudio_label_shiny_application_wizard_page',
  dirInputId: '#rstudio_shiny_application_directory_name',
  expectedFile: 'app.R',
};

const QUARTO_PROJECT: ProjectType = {
  name: 'quarto_project_test_project',
  wizardPageId: '#rstudio_label_quarto_project_wizard_page',
  dirInputId: '#rstudio_quarto_project_directory_name',
  expectedFile: 'quarto_project_test_project.qmd',
};

const QUARTO_WEBSITE: ProjectType = {
  name: 'quarto_website_test_project',
  wizardPageId: '#rstudio_label_quarto_website_wizard_page',
  dirInputId: '#rstudio_quarto_website_directory_name',
  expectedFile: 'index.qmd',
};

const ALL_TYPES = [NEW_PROJECT, R_PACKAGE, SHINY_APP, QUARTO_PROJECT, QUARTO_WEBSITE];

// -- Helpers ------------------------------------------------------------------

// Helpers below interpolate rExpression / projectDir / type.name directly into
// double-quoted R strings. Callers must pass values that do not contain `"` or
// `\` — all current inputs are controlled (hardcoded R expressions, known-safe
// project names, Windows paths normalized to forward slashes). If these
// helpers are ever reused with uncontrolled input, add proper escaping.
async function captureResult(page: Page, rExpression: string): Promise<string> {
  const marker = `__CP_${Date.now()}__`;
  await executeInConsole(
    page,
    `cat("${marker}", ${rExpression}, "${marker}")`,
    { wait: true },
  );

  // executeInConsole's waitForConsoleIdle returns the moment R drops its busy
  // flag, but the console-output client event that renders the cat() text can
  // land a beat later. A single innerText read here therefore races the paint
  // and intermittently misses the markers under full-suite load (the cause of
  // the flaky "markers not found" beforeAll failure). Poll the console text
  // until both markers are present, then extract the value between them.
  const pattern = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`);
  let match: RegExpMatchArray | null = null;
  await expect
    .poll(
      async () => {
        const output = await page.locator(CONSOLE_OUTPUT).innerText();
        match = output.match(pattern);
        return match !== null;
      },
      {
        timeout: 10000,
        message: `captureResult: markers not found for "${rExpression}"`,
      },
    )
    .toBe(true);
  return match![1].trim();
}

async function waitForSessionRestart(page: Page): Promise<void> {
  // Server navigates on project open/close; Desktop reloads in place, so
  // waitForLoadState never fires there -- intentional catch.
  await page.waitForLoadState('load', { timeout: TIMEOUTS.sessionRestart }).catch(() => {});

  // Readiness is signalled by window.rstudio.ready: the GWT bridge resets it
  // to false when the session-ending transition starts (QuitEvent on project
  // open/close) and flips it back to true on the new session's
  // DeferredInitCompletedEvent. Callers pre-reset it to false *before*
  // triggering the restart (see closeCurrentProject / createProjectInNewDir)
  // so this poll observes a clean false->true edge rather than the prior
  // session's stale true.
  //
  // This replaces an older marker-echo loop that fired a `cat()` marker via
  // executeInConsole and scanned console output for it. That approach was
  // flaky: the marker could be submitted into the old console moments before
  // the in-place restart cleared it (so it never reappeared), and each
  // attempt's inner waitForConsoleIdle (default sessionRestart timeout) could
  // consume the whole budget, leaving room for only a couple of retries.
  //
  // 2x sessionRestart covers the full restart + workspace/search-path restore
  // on a slow CI worker.
  await page.waitForFunction(
    () => window.rstudio?.ready === true,
    null,
    { timeout: TIMEOUTS.sessionRestart * 2, polling: 100 },
  );

  // ready=true means GWT's workbench is wired up, but the console input may
  // still be mounting and the console-busy class can linger briefly through
  // the post-restart prompt transition. Wait for both so a caller that
  // immediately issues console actions sees an idle, mounted console.
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: TIMEOUTS.sessionRestart });
  await waitForConsoleIdle(page);
}

async function openNewProjectWizard(page: Page): Promise<void> {
  // Close any open source docs first to avoid "unsaved changes" dialogs
  await resetSourcePaneState(page);

  // executeCommand("newProject") blocks the R thread with an "R session is busy"
  // modal on some platforms, so use the command palette instead.
  await page.keyboard.press('ControlOrMeta+Shift+p');
  // Wait for the palette list to render before typing -- the keystrokes get
  // swallowed if the palette hasn't mounted yet.
  await expect(page.locator(PALETTE_LIST)).toBeVisible({ timeout: 5000 });
  await page.keyboard.type('Create a New Project');

  const item = page.locator(`${PALETTE_LIST} >> text=Create a New Project...`);
  await expect(item).toBeVisible({ timeout: 5000 });
  await item.click();

  await expect(page.locator(WIZARD_DIALOG)).toBeVisible({ timeout: 15000 });
}

async function createProjectInNewDir(
  page: Page,
  type: ProjectType,
  options: { withGit?: boolean } = {},
): Promise<void> {
  await openNewProjectWizard(page);

  const dialog = page.locator(WIZARD_DIALOG);

  // Each step's `expect(...).toBeVisible` is the wait gate for the next
  // panel to mount; click() returns once the event fires, not after the
  // panel transitions.
  const newDir = dialog.locator(NEW_DIRECTORY_OPTION);
  await expect(newDir).toBeVisible({ timeout: 5000 });
  await newDir.click();

  const wizardPage = dialog.locator(type.wizardPageId);
  await expect(wizardPage).toBeVisible({ timeout: 5000 });
  await wizardPage.click();

  const dirInput = dialog.locator(type.dirInputId);
  await expect(dirInput).toBeVisible({ timeout: 5000 });
  await dirInput.click();
  // pressSequentially is required for GWT TextBox to fire keystroke handlers
  // that enable the Create Project button. Wait for the input to actually
  // hold the typed text before clicking Create.
  await dirInput.pressSequentially(type.name);
  await expect(dirInput).toHaveValue(type.name, { timeout: 2000 });

  if (options.withGit !== undefined) {
    const gitCheckbox = dialog.locator('#rstudio_new_project_git_repo input');
    await expect(gitCheckbox).toBeVisible({ timeout: 5000 });
    if (options.withGit) {
      await gitCheckbox.check();
    } else {
      await gitCheckbox.uncheck();
    }
  }

  const createBtn = page.locator(CREATE_PROJECT_BTN);
  // The Quarto project pages fire an async quartoCapabilities() server call in
  // onActivate and surface the wait via indicator.onProgress(); the modal
  // dialog's progress indicator disables ALL dialog buttons (including this
  // one) until the response lands or the page's 30s capabilities timeout
  // fires. Waiting on `toBeVisible` alone races that fetch on slow CI, so
  // wait for the button to actually be enabled before clicking.
  await expect(createBtn).toBeEnabled({ timeout: 30000 });

  // Pre-reset the readiness flag before creating: project creation restarts
  // the session into the new project, and we want waitForSessionRestart to
  // see a clean false->true transition rather than the prior session's stale
  // true. Nothing flips ready back to true until the new session's deferred
  // init, so doing this before the wizard dismisses is safe.
  await page.evaluate(() => {
    if (window.rstudio) window.rstudio.ready = false;
  });
  await createBtn.click();

  // Wizard should dismiss within a few seconds. If it lingers, the name
  // likely failed validation (e.g. invalid R package name).
  await expect(page.locator(WIZARD_DIALOG)).not.toBeVisible({ timeout: 15000 });

  await waitForSessionRestart(page);

  // R Package creation may prompt to install build-tools (devtools/roxygen2).
  await installDepIfPrompted(page, 3000);
}

async function closeCurrentProject(page: Page): Promise<void> {
  const menu = page.locator(PROJECT_MENU);
  await expect(menu).toBeVisible({ timeout: 10000 });
  await menu.click();

  const close = page.locator(CLOSE_PROJECT_MENU_ITEM);
  await expect(close).toBeVisible({ timeout: 5000 });

  // Pre-reset the readiness flag before triggering the close so
  // waitForSessionRestart sees a clean false->true transition. The QuitEvent
  // fired by closing the project also resets it, but that lands only after a
  // server round-trip -- without this preemptive reset a poll could observe
  // the prior session's stale true and return before the restart finishes.
  await page.evaluate(() => {
    if (window.rstudio) window.rstudio.ready = false;
  });
  await close.click();

  await waitForSessionRestart(page);
}

async function cleanupProject(page: Page, projectDir: string): Promise<void> {
  if (!projectDir) return;
  const escaped = projectDir.replace(/\\/g, '/');
  await executeInConsole(page, `unlink("${escaped}", recursive = TRUE)`, { wait: true });
}

// -- Tests --------------------------------------------------------------------

test.describe.serial('Create Projects in New Directory', () => {
  const sandbox = useSuiteSandbox();

  // Tracked per-test so afterEach can clean up on failure
  let currentProjectDir = '';
  let source: SourcePane;
  let originalDefaultProjectLocation = '';

  test.beforeEach(async ({ rstudioPage: page }) => {
    source = new SourcePane(page);
  });

  test.beforeAll(async ({ rstudioPage: page }) => {
    // Dismiss any leftover dialog from a prior failed run. Wait briefly after
    // each Escape for the overlay to detach -- a long wait per loop would
    // just waste time in the common no-dialog case.
    const overlay = page.locator('.gwt-PopupPanelGlass, [role="alertdialog"]').first();
    for (let i = 0; i < 3; i++) {
      if (!(await overlay.isVisible())) break;
      await page.keyboard.press('Escape');
      await overlay.waitFor({ state: 'hidden', timeout: 2000 }).catch(() => {});
    }

    // Close any open project from a prior run
    const hasProject = await captureResult(page, '!is.null(rstudioapi::getActiveProject())');
    if (hasProject === 'TRUE') {
      await closeCurrentProject(page);
    }

    // Redirect the New Project Wizard's parent-directory field into the
    // sandbox by overriding the default_project_location preference. The
    // field itself is read-only, so setting the pref is the only way.
    // If a prior crashed run left our sandbox path in the pref, treat it
    // as leftover state and restore to the schema default on afterAll.
    const current = await captureResult(
      page,
      '.rs.api.readRStudioPreference("default_project_location")',
    );
    const basename = current.split(/[/\\]/).pop() || '';
    originalDefaultProjectLocation = basename.startsWith(SANDBOX_DIR_PREFIX) ? '' : current;

    const escaped = sandbox.dir.replace(/\\/g, '/');
    await setPref(page, 'default_project_location', escaped);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    try {
      await setPref(page, 'default_project_location', originalDefaultProjectLocation);
    } catch (err) {
      console.warn('default_project_location restore failed:', err);
    }
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // Safety net: if a test failed before its own cleanup ran, close any
    // open project and delete the captured directory.
    try {
      const hasProject = await captureResult(page, '!is.null(rstudioapi::getActiveProject())');
      if (hasProject === 'TRUE') {
        await closeCurrentProject(page);
      }
    } catch {
      // best-effort
    }
    if (currentProjectDir) {
      await cleanupProject(page, currentProjectDir).catch(() => {});
      currentProjectDir = '';
    }
  });

  for (const type of ALL_TYPES) {
    test(`create "${type.name}"`, async ({ rstudioPage: page }) => {
      await createProjectInNewDir(page, type);

      // Capture the actual project directory for later cleanup
      currentProjectDir = await captureResult(page, 'getwd()');

      // Project actually created on disk (.Rproj file exists)
      const rprojExists = await captureResult(page,
        `file.exists("${currentProjectDir}/${type.name}.Rproj")`);
      expect(rprojExists, `.Rproj missing for ${type.name} at ${currentProjectDir}`).toBe('TRUE');

      // Project menu reflects the new project
      await expect(page.locator(PROJECT_MENU)).toContainText(type.name, { timeout: 15000 });

      // Expected starter file is open in the source pane
      if (type.expectedFile) {
        await expect(source.selectedTab).toContainText(type.expectedFile, { timeout: 15000 });
      }

      await closeCurrentProject(page);
      await cleanupProject(page, currentProjectDir);
      currentProjectDir = '';
    });
  }

  // Git-enabled new project: assumes git is installed on the test host so
  // the VCS tab appears.
  test('create new project with git enabled', async ({ rstudioPage: page }) => {
    const type = { ...NEW_PROJECT, name: 'git_enabled_test_project' };
    await createProjectInNewDir(page, type, { withGit: true });

    currentProjectDir = await captureResult(page, 'getwd()');

    const rprojExists = await captureResult(page,
      `file.exists("${currentProjectDir}/${type.name}.Rproj")`);
    expect(rprojExists, `.Rproj missing at ${currentProjectDir}`).toBe('TRUE');

    await expect(page.locator(PROJECT_MENU)).toContainText(type.name, { timeout: 15000 });
    await expect(page.locator('#rstudio_workbench_tab_git')).toBeVisible({ timeout: 15000 });

    await closeCurrentProject(page);
    await cleanupProject(page, currentProjectDir);
    currentProjectDir = '';
  });
});

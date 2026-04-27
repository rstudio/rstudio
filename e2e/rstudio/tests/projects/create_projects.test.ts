import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, TIMEOUTS } from '@utils/constants';
import { typeInConsole, CONSOLE_INPUT, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { installDepIfPrompted } from '@pages/modals.page';
import { SourcePane } from '@pages/source_pane.page';
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
  await typeInConsole(page, `cat("${marker}", ${rExpression}, "${marker}")`);

  const pattern = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`);
  const start = Date.now();
  while (Date.now() - start < TIMEOUTS.consoleReady) {
    await sleep(500);
    const output = await page.locator(CONSOLE_OUTPUT).innerText();
    const match = output.match(pattern);
    if (match) return match[1].trim();
  }
  throw new Error(`captureResult: markers not found for "${rExpression}" within ${TIMEOUTS.consoleReady}ms`);
}

async function waitForSessionRestart(page: Page): Promise<void> {
  // Server navigates on project open/close; Desktop reloads in place, so
  // waitForLoadState never fires there — intentional catch.
  await page.waitForLoadState('load', { timeout: TIMEOUTS.sessionRestart }).catch(() => {});
  await sleep(3000);
  // 2× sessionRestart: after navigation settles, the console element may still
  // take extra time to mount on slower Server sessions.
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: TIMEOUTS.sessionRestart * 2 });
  await sleep(2000);

  // Confirm R is idle with retries. Individual attempts may fail while the
  // console is still coming up — intentional catch inside the loop — but if
  // all three fail we surface the timeout instead of silently returning and
  // letting downstream code fail mysteriously.
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const marker = `__READY_${Date.now()}__`;
      await typeInConsole(page, `cat("${marker}")`);
      await sleep(1500);
      const output = await page.locator(CONSOLE_OUTPUT).innerText();
      if (output.includes(marker)) return;
    } catch {
      // console may not be ready yet
    }
    await sleep(2000);
  }
  throw new Error('R session did not become idle within timeout after session restart');
}

async function openNewProjectWizard(page: Page): Promise<void> {
  // Close any open source docs first to avoid "unsaved changes" dialogs
  await typeInConsole(page, '.rs.api.closeAllSourceBuffersWithoutSaving()');
  await sleep(1000);

  // executeCommand("newProject") blocks the R thread with an "R session is busy"
  // modal on some platforms, so use the command palette instead.
  await page.keyboard.press('ControlOrMeta+Shift+p');
  await sleep(1000);
  await page.keyboard.type('Create a New Project');
  await sleep(500);

  const item = page.locator(`${PALETTE_LIST} >> text=Create a New Project...`);
  await expect(item).toBeVisible({ timeout: 5000 });
  await item.click();
  await sleep(2000);

  await expect(page.locator(WIZARD_DIALOG)).toBeVisible({ timeout: 15000 });
}

async function createProjectInNewDir(page: Page, type: ProjectType): Promise<void> {
  await openNewProjectWizard(page);

  const dialog = page.locator(WIZARD_DIALOG);

  const newDir = dialog.locator(NEW_DIRECTORY_OPTION);
  await expect(newDir).toBeVisible({ timeout: 5000 });
  await newDir.click();
  await sleep(1000);

  const wizardPage = dialog.locator(type.wizardPageId);
  await expect(wizardPage).toBeVisible({ timeout: 5000 });
  await wizardPage.click();
  await sleep(1000);

  const dirInput = dialog.locator(type.dirInputId);
  await expect(dirInput).toBeVisible({ timeout: 5000 });
  await dirInput.click();
  // pressSequentially is required for GWT TextBox to fire keystroke handlers
  // that enable the Create Project button.
  await dirInput.pressSequentially(type.name);
  await sleep(500);

  const createBtn = page.locator(CREATE_PROJECT_BTN);
  await expect(createBtn).toBeVisible({ timeout: 5000 });
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
  await close.click();

  await waitForSessionRestart(page);
}

async function cleanupProject(page: Page, projectDir: string): Promise<void> {
  if (!projectDir) return;
  const escaped = projectDir.replace(/\\/g, '/');
  await typeInConsole(page, `unlink("${escaped}", recursive = TRUE)`);
  await sleep(500);
}

// -- Tests --------------------------------------------------------------------

test.describe.serial('Create Projects in New Directory', () => {
  // Tracked per-test so afterEach can clean up on failure
  let currentProjectDir = '';
  let source: SourcePane;

  test.beforeEach(async ({ rstudioPage: page }) => {
    source = new SourcePane(page);
  });

  test.beforeAll(async ({ rstudioPage: page }) => {
    // Dismiss any leftover dialog from a prior failed run
    for (let i = 0; i < 3; i++) {
      const overlay = page.locator('.gwt-PopupPanelGlass, [role="alertdialog"]');
      if (await overlay.first().isVisible({ timeout: 2000 }).catch(() => false)) {
        await page.keyboard.press('Escape');
        await sleep(1000);
      } else {
        break;
      }
    }

    // Close any open project from a prior run
    const hasProject = await captureResult(page, '!is.null(rstudioapi::getActiveProject())');
    if (hasProject === 'TRUE') {
      await closeCurrentProject(page);
    }

    // Return to home and clean up any stale test directories
    await typeInConsole(page, 'setwd("~")');
    await sleep(500);
    for (const type of ALL_TYPES) {
      await typeInConsole(page, `unlink("~/${type.name}", recursive = TRUE)`);
      await sleep(300);
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
});

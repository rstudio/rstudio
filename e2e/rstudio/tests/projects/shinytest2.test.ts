import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { CONFIRM_BTN } from '@pages/modals.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { typeInConsole, CONSOLE_INPUT, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { rPathLiteral } from '@utils/r';
import * as fs from 'fs';
import * as path from 'path';
import type { Page } from 'playwright';

const PROJECT_MENU = '#rstudio_project_menubutton_toolbar';
const CLOSE_PROJECT_MENU_ITEM = '#rstudio_label_close_project_command';
// The project menu button also has a title containing the project path, so a
// loose `title*='shinytest2'` selector hits it when the project name contains
// the word. Match the toolbar button's exact title text instead.
const SHINYTEST_BUTTON = "button[title='Run test using the shinytest2 package']";

async function waitForConsoleIdle(page: Page): Promise<void> {
  await page.waitForFunction(
    () => {
      const el = document.getElementById('rstudio_console_input');
      return !!el && !el.classList.contains('rstudio-console-busy');
    },
    null,
    { timeout: TIMEOUTS.sessionRestart, polling: 100 },
  );
}

async function closeProjectIfOpen(page: Page): Promise<void> {
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

const SHINYTEST2_TEST_CONTENT = `library(shinytest2)

test_that("01_hello produces stable values", {
  app <- AppDriver$new(name = "hello")
  app$set_inputs(bins = 20)
  app$expect_values()
})
`;

async function scaffoldShinytest2Project(
  page: Page,
  consoleActions: ConsolePaneActions,
  projectDir: string,
): Promise<void> {
  const projectName = projectDir.split('/').pop()!;
  const testFilePath = path.join(projectDir, 'tests', 'testthat', 'test-shinytest2.R');

  // Create the project tree and seed files via Node fs before opening the
  // project. Avoids R-string-escape issues with writeLines("...") containing
  // embedded double quotes (test_that("..."), AppDriver$new(name = "..."),
  // etc.) and keeps the test file content readable as a TS template literal.
  fs.mkdirSync(path.dirname(testFilePath), { recursive: true });
  fs.writeFileSync(testFilePath, SHINYTEST2_TEST_CONTENT);

  const projectDirLit = rPathLiteral(projectDir);
  const appPathLit = rPathLiteral(`${projectDir}/app.R`);
  const testPathLit = rPathLiteral(testFilePath);

  await consoleActions.typeInConsole(`.rs.api.initializeProject(${projectDirLit})`);
  await consoleActions.typeInConsole(`.rs.api.openProject(${projectDirLit})`);
  await expect(page.locator(PROJECT_MENU)).toContainText(projectName, {
    timeout: TIMEOUTS.sessionRestart,
  });
  await waitForConsoleIdle(page);

  await consoleActions.typeInConsole(
    `file.copy(file.path(system.file("examples", package = "shiny"), "01_hello", "app.R"), ${appPathLit}, overwrite = TRUE)`,
  );
  await consoleActions.typeInConsole(`.rs.api.documentOpen(${testPathLit})`);
  await sleep(1000);
}

test.describe('shinytest2 integration', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let missingShiny: string[] = [];
  let missingShinytest2: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    missingShiny = await consoleActions.ensurePackages(['shiny']);
    // shinytest2 is optional for the toolbar-button test but required for
    // the snapshot_review stub test.
    missingShinytest2 = await consoleActions.ensurePackages(['shinytest2']);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page).catch(() => {});
  });

  test('shinytest2 file shows Shiny test toolbar button and "no tests yet" info dialog', async ({ rstudioPage: page }) => {
    test.skip(missingShiny.length > 0, `Missing: ${missingShiny.join(', ')}`);

    const projectDir = `${sandbox.dir}/shinytest2-demo-toolbar`.replace(/\\/g, '/');
    await scaffoldShinytest2Project(page, consoleActions, projectDir);

    // Button only appears when getTestType() classifies the file as
    // TestsShinyTest -- exercises the AppDriver$new-before-test_that detection.
    await expect(page.locator(SHINYTEST_BUTTON)).toBeVisible({ timeout: TIMEOUTS.fileOpen });

    await consoleActions.typeInConsole('.rs.api.executeCommand("shinyCompareTest")');

    const okBtn = page.locator(CONFIRM_BTN);
    await expect(okBtn).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await okBtn.click();
    await expect(okBtn).not.toBeVisible({ timeout: TIMEOUTS.fileOpen });
  });

  test('shinyCompareTest calls testthat::snapshot_review with the project tests dir when snaps exist', async ({ rstudioPage: page }) => {
    test.skip(
      missingShiny.length > 0 || missingShinytest2.length > 0,
      `Missing: ${[...missingShiny, ...missingShinytest2].join(', ')}`,
    );

    const projectDir = `${sandbox.dir}/shinytest2-demo-snaps`.replace(/\\/g, '/');
    await scaffoldShinytest2Project(page, consoleActions, projectDir);

    // Seed a pending diff (*.new.*) so has_shinytest2_results returns true,
    // then stub testthat::snapshot_review to capture the path argument
    // instead of actually launching diffviewer.
    const snapDirLit = rPathLiteral(`${projectDir}/tests/testthat/_snaps/hello`);
    const snapFileLit = rPathLiteral(`${projectDir}/tests/testthat/_snaps/hello/snapshot.new.png`);
    await consoleActions.typeInConsole(
      `dir.create(${snapDirLit}, recursive = TRUE, showWarnings = FALSE)`,
    );
    await consoleActions.typeInConsole(`file.create(${snapFileLit})`);

    const stubInstall = [
      'ns <- asNamespace("testthat")',
      '.rs.original.snapshot_review <- ns$snapshot_review',
      'unlockBinding("snapshot_review", ns)',
      'ns$snapshot_review <- function(files = NULL, path = "tests/testthat", ...) { cat("CAPTURED snapshot_review path=", path, "\\n", sep = "") }',
      'lockBinding("snapshot_review", ns)',
    ].join('; ');
    await consoleActions.typeInConsole(stubInstall);

    try {
      await consoleActions.typeInConsole('.rs.api.executeCommand("shinyCompareTest")');

      await expect
        .poll(
          async () => (await page.locator(CONSOLE_OUTPUT).innerText()),
          { timeout: TIMEOUTS.fileOpen },
        )
        .toMatch(/CAPTURED snapshot_review path=.*tests\/testthat$/m);
    } finally {
      const restore = [
        'ns <- asNamespace("testthat")',
        'unlockBinding("snapshot_review", ns)',
        'ns$snapshot_review <- .rs.original.snapshot_review',
        'lockBinding("snapshot_review", ns)',
      ].join('; ');
      await typeInConsole(page, restore).catch(() => {});
    }
  });
});

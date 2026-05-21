import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { installDepIfPrompted } from '@pages/modals.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral, rStringLiteral } from '@utils/r';
import { executeCommand } from '@utils/commands';
import * as fs from 'fs';
import * as path from 'path';
import type { Page } from 'playwright';

const PROJECT_MENU = '#rstudio_project_menubutton_toolbar';

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
  await page.locator('#rstudio_label_close_project_command').click();
  await expect(menu).toContainText('(None)', { timeout: TIMEOUTS.sessionRestart });
  await waitForConsoleIdle(page);
}

test.describe('Build pane', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    // Pre-install devtools so testTestthatFile does not block on the
    // "Using testthat requires an updated version of the devtools package"
    // modal. installDepIfPrompted below stays as a fallback for cases where
    // the installed version is still considered outdated.
    missingPackages = await consoleActions.ensurePackages(['testthat', 'devtools']);
  });

  test('testTestthatFile runs tests and prints "Test complete" with color in the Build pane', async ({ rstudioPage: page }) => {
    test.skip(missingPackages.length > 0, `Missing packages: ${missingPackages.join(', ')}`);

    // R package names must be letters/digits/dots (no underscores) and start
    // with a letter, so build the name from a hex stamp rather than Date.now().
    const projectName = `BuildPaneTest${Math.random().toString(16).slice(2, 10)}`;
    const projectDir = path.join(sandbox.dir, projectName);
    const testFile = path.join(projectDir, 'tests', 'testthat', 'test-example.R');
    const projectNameLit = rStringLiteral(projectName);
    const projectDirLit = rPathLiteral(projectDir);
    const testFileLit = rPathLiteral(testFile);

    test.setTimeout(180000);

    // Create the package skeleton and open the project. openProject restarts R,
    // so wait for both the project label to update and the console to be idle
    // before continuing.
    await consoleActions.executeInConsole(
      `.rs.rpc.package_skeleton(packageName = ${projectNameLit}, packageDirectory = ${projectDirLit}, sourceFiles = character(), usingRcpp = FALSE)`,
    );
    await consoleActions.executeInConsole(`.rs.api.openProject(${projectDirLit})`);
    await expect(page.locator(PROJECT_MENU)).toContainText(projectName, {
      timeout: TIMEOUTS.sessionRestart,
    });
    await waitForConsoleIdle(page);

    // Write a trivial testthat test file via Node fs and open it -- testTestthatFile
    // runs the active document, so the file has to be the current source tab.
    fs.mkdirSync(path.dirname(testFile), { recursive: true });
    fs.writeFileSync(
      testFile,
      'test_that("we can run a test", {\n  expect_equal(2 + 2, 4)\n})\n',
    );
    await consoleActions.executeInConsole(`.rs.api.documentOpen(${testFileLit})`);
    await sleep(1000);

    await executeCommand(page, 'testTestthatFile');

    // testTestthatFile checks devtools is current and prompts to install if
    // not; click Yes (no-op if no prompt appears).
    await installDepIfPrompted(page);

    const buildOutput = page.locator('#rstudio_workbench_panel_build .ace_editor');
    await expect(buildOutput).toContainText('Test complete', { timeout: 90000 });

    // testthat prints results with ANSI colors; RStudio renders them via
    // xtermColor-class spans, so a non-color run would have no xtermColor in
    // the rendered HTML.
    const html = await buildOutput.innerHTML();
    expect(html).toContain('xtermColor');

    await closeProjectIfOpen(page);
  });
});

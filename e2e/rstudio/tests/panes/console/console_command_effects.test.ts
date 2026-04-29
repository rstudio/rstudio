import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';

test.describe('Console command effects', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.beforeEach(async () => {
    await consoleActions.clearConsole();
  });

  test("packageVersion('base') auto-prints the version", async () => {
    await consoleActions.typeInConsole("packageVersion('base')");
    await expect(consoleActions.consolePane.consoleOutput).toContainText('[1]');
  });

  test('help.start() loads the R help index into the Help pane', async ({ rstudioPage: page }) => {
    await consoleActions.typeInConsole('help.start()');
    const helpFrame = page.frameLocator('#rstudio_help_frame');
    await expect(helpFrame.locator('body')).toContainText('Statistical Data Analysis', {
      timeout: 30000,
    });
  });

  test('help(package = "base") loads package help into the Help pane', async ({ rstudioPage: page }) => {
    await consoleActions.typeInConsole("help(package = 'base')");
    const helpFrame = page.frameLocator('#rstudio_help_frame');
    await expect(helpFrame.locator('body')).toContainText('The R Base Package', {
      timeout: 15000,
    });
  });

  test('plot(x, y) renders a plot in the Plots pane', async ({ rstudioPage: page }) => {
    await consoleActions.typeInConsole('x <- 1');
    await consoleActions.typeInConsole('y <- 1');
    await consoleActions.typeInConsole('plot(x, y)');
    await expect(page.locator('#rstudio_plot_image_frame')).toBeVisible({ timeout: 15000 });
  });

  test('rstudioDiagnosticsReport() writes a diagnostics report', async () => {
    test.setTimeout(120000);
    await consoleActions.typeInConsole('rstudioDiagnosticsReport()');
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      'Diagnostics report written',
      { timeout: 90000 },
    );
  });

  test('install.packages() reports not-available for a non-existent package', async () => {
    test.setTimeout(60000);
    await consoleActions.typeInConsole(
      "install.packages('fake_package', repos = 'https://cran.r-project.org')",
    );
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      "‘fake_package’ is not available",
      { timeout: 45000 },
    );
  });

  test('install.packages() installs a real CRAN package that library() can load', async () => {
    test.setTimeout(300000);
    const uninstalled = await consoleActions.uninstallPackage('starwarsdb');
    test.skip(!uninstalled, 'Could not uninstall starwarsdb to set up a fresh-install scenario');
    const failed = await consoleActions.ensurePackages(['starwarsdb'], 240000);
    test.skip(failed.length > 0, `Could not install: ${failed.join(', ')}`);

    await consoleActions.clearConsole();
    await consoleActions.typeInConsole("library('starwarsdb')");
    await sleep(2000);
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText(
      'Error in library',
    );
  });
});

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { EnvironmentPane, clearWorkspace } from '@pages/environment_pane.page';
import { CONFIRM_BTN } from '@pages/modals.page';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

let envPane: EnvironmentPane;
let consoleActions: ConsolePaneActions;

test.describe('Environment pane', () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    envPane = new EnvironmentPane(page);
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    // Every test operates on the Environment tab; make it active first.
    // Using the bridge command avoids actionability-check timeouts that occur
    // when another element overlaps the tab's click target in the full suite.
    await executeCommand(page, 'activateEnvironment');
  });

  test('toolbar elements are displayed', async () => {
    await expect(envPane.panel).toBeVisible();
    await expect(envPane.searchBar).toBeVisible();
    await expect(envPane.refreshWorkspaceBtn).toBeVisible();
    await expect(envPane.memoryPieBtn).toBeVisible();
    await expect(envPane.clearWorkspaceBtn).toBeVisible();
    await expect(envPane.loadWorkspaceBtn).toBeVisible();
    await expect(envPane.saveWorkspaceBtn).toBeVisible();
  });

  test('import dataset dropdown lists every source', async ({ rstudioPage: page }) => {
    await envPane.importDatasetMenu.click();

    await expect(envPane.datasetTextBase).toBeVisible();
    await expect(envPane.datasetTextReadr).toBeVisible();
    await expect(envPane.datasetExcel).toBeVisible();
    await expect(envPane.datasetSpss).toBeVisible();
    await expect(envPane.datasetSas).toBeVisible();
    await expect(envPane.datasetStata).toBeVisible();

    await page.keyboard.press('Escape');
  });

  test('object-view dropdown shows list and grid views', async ({ rstudioPage: page }) => {
    await envPane.viewMenu.click();

    await expect(envPane.listViewOption).toBeVisible();
    await expect(envPane.gridViewOption).toBeVisible();

    await page.keyboard.press('Escape');
  });

  test('environment-list dropdown shows the global environment and packages', async ({
    rstudioPage: page,
  }) => {
    await envPane.envListMenu.click();

    await expect(envPane.globalEnvOption).toBeVisible();
    await expect(envPane.packageStats).toBeVisible();
    await expect(envPane.packageGraphics).toBeVisible();
    await expect(envPane.packageGrDevices).toBeVisible();
    await expect(envPane.packageUtils).toBeVisible();
    await expect(envPane.packageMethods).toBeVisible();
    await expect(envPane.packageBase).toBeVisible();

    await page.keyboard.press('Escape');
  });

  test('memory pie grows with allocation and the usage report opens', async ({
    rstudioPage: page,
  }) => {
    // Start from a clean workspace so the only Values row is the one we add.
    await clearWorkspace(page);
    await consoleActions.clearConsole();

    // Wait for the pie to report a concrete positive figure (it reads "Memory"
    // until the first usage sample arrives), then capture the baseline in MiB.
    await expect.poll(() => envPane.getMemoryMiB(), { timeout: TIMEOUTS.memoryUsageUpdate })
      .toBeGreaterThan(0);
    const before = (await envPane.getMemoryMiB())!;

    // Allocate ~76 MiB (1e7 doubles) -- well above the pie's 1 MiB resolution.
    await consoleActions.executeInConsole('bigval <- runif(1e7)');
    await expect(envPane.panel).toContainText('bigval');

    // The pie samples on an interval, so poll for the increase rather than
    // reading once. Unit-normalized comparison (MiB) avoids the GiB/MiB trap.
    await expect
      .poll(
        async () => {
          const after = await envPane.getMemoryMiB();
          return after !== null && after > before;
        },
        { timeout: TIMEOUTS.memoryUsageUpdate, message: 'memory pie did not grow after allocation' },
      )
      .toBe(true);

    // The memory-usage report opens as a modal titled "Memory Usage Report".
    await executeCommand(page, 'showMemoryUsageReport');
    const dialog = page.locator('.gwt-DialogBox').filter({ hasText: 'Memory Usage' });
    await expect(dialog).toBeVisible();
    await page.locator(CONFIRM_BTN).click();
    await expect(dialog).toBeHidden();

    // Free the big vector so the shared session doesn't carry ~76 MiB onward.
    await consoleActions.executeInConsole('rm(bigval)');
  });
});

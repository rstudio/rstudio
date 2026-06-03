import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { EnvironmentPane } from '@pages/environment_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';

const sandbox = useSuiteSandbox();

let consoleActions: ConsolePaneActions;
let sourceActions: SourcePaneActions;
let sourcePane: SourcePane;
let envPane: EnvironmentPane;

function rPath(p: string): string {
  return p.replace(/\\/g, '/');
}

test.describe('R file execution and environment pane', () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    sourcePane = new SourcePane(page);
    envPane = new EnvironmentPane(page);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test(
    'creates an R file, executes code, and tracks environment pane state',
    { tag: ['@parallel_safe', '@smoketest'] },
    async () => {
      await consoleActions.executeInConsole('rm(list = ls())');

      const fileName = `rTest${Date.now()}.R`;
      const filePath = `${rPath(sandbox.dir)}/${fileName}`;

      await consoleActions.executeInConsole(`writeLines("", "${filePath}")`);
      await consoleActions.executeInConsole(`file.edit("${filePath}")`);
      await expect(sourcePane.selectedTab).toContainText(fileName, { timeout: TIMEOUTS.fileOpen });
      await expect(sourcePane.footerTable).toContainText('R Script', { timeout: TIMEOUTS.fileOpen });

      await sourceActions.sendText('x <- 3\ny <- 10\ny - x');

      await consoleActions.executeInConsole(".rs.api.executeCommand('executeAllCode')");
      await expect(consoleActions.consolePane.consoleOutput).toContainText('[1] 7', {
        timeout: TIMEOUTS.sessionRestart,
      });
      await expect(consoleActions.consolePane.consoleOutput).toContainText('x <- 3');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('y <- 10');
      await expect(consoleActions.consolePane.consoleOutput).toContainText('y - x');

      // Verify initial environment state: x=3, y=10
      await consoleActions.executeInConsole(".rs.api.executeCommand('activateEnvironment')");
      await expect(envPane.panel).toBeVisible();
      await expect.poll(() => envPane.hasVariable('x', '3'), { timeout: 5000 }).toBe(true);
      await expect.poll(() => envPane.hasVariable('y', '10'), { timeout: 5000 }).toBe(true);

      // Change x and verify env pane updates
      await consoleActions.executeInConsole('x <- 22');
      await consoleActions.executeInConsole(".rs.api.executeCommand('activateEnvironment')");
      await expect(envPane.panel).toBeVisible();
      await expect.poll(() => envPane.hasVariable('x', '22'), { timeout: 5000 }).toBe(true);
      await expect.poll(() => envPane.hasVariable('y', '10'), { timeout: 5000 }).toBe(true);

      // Remove x and verify it disappears from the env pane
      await consoleActions.executeInConsole('rm(x)');
      await consoleActions.executeInConsole(".rs.api.executeCommand('activateEnvironment')");
      await expect(envPane.panel).toBeVisible();
      await expect.poll(() => envPane.hasVariable('x', '22'), { timeout: 5000 }).toBe(false);
      await expect.poll(() => envPane.hasVariable('y', '10'), { timeout: 5000 }).toBe(true);
    },
  );
});

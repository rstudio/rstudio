import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { documentCloseAllNoSave, waitForActiveDocument } from '@utils/commands';

test.describe('Run Line button', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
  });

  test('submits queued lines to the console in document order', async ({ rstudioPage: page }) => {
    await consoleActions.clearConsole();
    await documentCloseAllNoSave(page);

    const sandboxR = sandbox.dir.replace(/\\/g, '/');
    const filePath = `${sandboxR}/submit_order_${Date.now()}.R`;
    await consoleActions.executeInConsole(
      `writeLines(c("Sys.sleep(1)", "# 1", "x <- 1", "# 2", "x <- 22", "x"), "${filePath}")`,
      { wait: true },
    );
    await consoleActions.executeInConsole(`file.edit("${filePath}")`);
    await waitForActiveDocument(page, filePath, TIMEOUTS.fileOpen);

    await sourceActions.goToTop();

    const runLineBtn = page.locator("[class*='run_the_current_line_or_selection']").first();
    for (let i = 0; i < 4; i++) {
      await runLineBtn.click();
    }

    await expect(consoleActions.consolePane.consoleOutput).toContainText('[1] 22', {
      timeout: 15000,
    });

    await documentCloseAllNoSave(page);
  });
});

import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';

test.describe('Run Line button', () => {
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
  });

  test('submits queued lines to the console in document order', async ({ rstudioPage: page }) => {
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole('.rs.api.executeCommand("closeAllSourceDocs")');

    const fileName = `submit_order_${Date.now()}.R`;
    await consoleActions.typeInConsole(
      `writeLines(c("Sys.sleep(1)", "# 1", "x <- 1", "# 2", "x <- 22", "x"), "${fileName}")`,
    );
    await consoleActions.typeInConsole(`file.edit("${fileName}")`);
    await sleep(1500);

    await sourceActions.goToTop();

    const runLineBtn = page.locator("[class*='run_the_current_line_or_selection']").first();
    for (let i = 0; i < 4; i++) {
      await runLineBtn.click();
    }

    await expect(consoleActions.consolePane.consoleOutput).toContainText('[1] 22', {
      timeout: 15000,
    });

    await consoleActions.typeInConsole('.rs.api.executeCommand("closeAllSourceDocs")');
    await consoleActions.typeInConsole(`unlink("${fileName}")`);
  });
});

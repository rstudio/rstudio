import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';

// https://github.com/rstudio/rstudio/issues/14636
test.describe('Session restart', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test('variables defined before restart are visible to the after-restart command', async () => {
    await consoleActions.typeInConsole('x <- 1; y <- 2');
    await consoleActions.typeInConsole(".rs.api.restartSession('print(x + y)')");

    await expect(consoleActions.consolePane.consoleOutput).toContainText('[1] 3', {
      timeout: TIMEOUTS.sessionRestart,
    });
  });
});

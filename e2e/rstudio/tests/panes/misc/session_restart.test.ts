import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';

// https://github.com/rstudio/rstudio/issues/14636
// TODO(aza): file a follow-up issue to unblock on Server.
// Server-on-Linux: the afterRestart callback never produces "[1] 3" in the
// console -- likely because the workspace transfer that lets x/y survive a
// restart on Desktop interacts differently with rsession's lifecycle on
// Server (save_workspace=never plus a fresh user-home, etc.). Failures here
// also cascade into the worker's next test by leaving the fixture in a
// half-restarted state. Skip on Server until the restart semantics are
// confirmed for Server mode.
test.describe('Session restart', { tag: ['@desktop_only'] }, () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test('variables defined before restart are visible to the after-restart command', async () => {
    await consoleActions.executeInConsole('x <- 1; y <- 2');
    // Test specifically exercises .rs.api.restartSession's afterRestart
    // parameter -- the restartR AppCommand has no equivalent way to schedule
    // a command for the post-restart session, so this stays in console.
    await consoleActions.executeInConsole(".rs.api.restartSession('print(x + y)')");

    await expect(consoleActions.consolePane.consoleOutput).toContainText('[1] 3', {
      timeout: TIMEOUTS.sessionRestart,
    });
  });
});

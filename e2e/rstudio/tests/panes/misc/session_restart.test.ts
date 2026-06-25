import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';

// https://github.com/rstudio/rstudio/issues/14636
// Tracked by #18064 (re-enable on Server).
// Server-on-Linux: the test reads the after-restart result ("[1] 3") off
// the console. On Server the post-restart console fills with escape-sequence
// glyphs (long runs of `ה`/`X`) instead of clean output, so the
// toContainText assertion never matches. This is the same post-restart
// console-rendering artifact that disables r_session_restart and
// package_installation. (The earlier save_workspace=never theory doesn't
// hold: that pref is set on Desktop too, where this test passes.) Failures
// here also cascade into the worker's next test by leaving the fixture in a
// half-restarted state, so this is a hard skip rather than a test.fixme.
// When re-enabling, confirm the #14636 behavior (x and y survive the
// restart) via a sentinel file or the automation bridge rather than console
// text.
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

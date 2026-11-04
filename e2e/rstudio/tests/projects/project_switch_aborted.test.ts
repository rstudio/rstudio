// A project switch quits the R session, and an error in .Last() refuses that
// quit: the session reports quit_aborted and carries on. The switch it had
// written down for the next session must go with it -- an ordinary quit
// afterwards starts a session without the project the aborted switch named.
//
// Server only: a Desktop quit closes the window, so the next launch cannot be
// watched from here.

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { closeProjectIfOpen } from '@utils/project';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';

const SESSION_ENDED = '[role="alertdialog"]';

test.describe('aborted project switch', () => {
  const sandbox = useSuiteSandbox();

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
  });

  test('an ordinary quit after the abort does not switch to the project', async ({ rstudioPage: page, mode }) => {
    test.skip(mode === 'desktop', 'a Desktop quit closes the window; the next launch is not observable here');

    await closeProjectIfOpen(page);

    const name = 'switch_aborted';
    const dir = `${sandbox.dir.replace(/\\/g, '/')}/${name}`;
    const rproj = `${dir}/${name}.Rproj`;
    await executeInConsole(
      page,
      `{ dir.create(${rPathLiteral(dir)}, showWarnings = FALSE); writeLines("Version: 1.0", ${rPathLiteral(rproj)}) }`,
      { wait: true },
    );

    await executeInConsole(page, '.Last <- function() stop("not quitting")', { wait: true });
    const promptsBefore = await page.evaluate(() => window.rstudio?.console?.promptCount ?? 0);
    await page.evaluate((p) => {
      const r = window.rstudio;
      if (!r)
        throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
      r.project.open(p);
    }, rproj);

    // R refuses the quit from .Last() and returns to its prompt; the bridge
    // is told the quit was aborted and reports the session usable again
    await expect(page.locator('#rstudio_workbench_panel_console')).toContainText(/Error in \.Last\(\)\s*: not quitting/, {
      timeout: 30000,
    });
    await page.waitForFunction(
      (before) => (window.rstudio?.console?.promptCount ?? 0) > before && window.rstudio?.ready === true,
      promptsBefore,
      { timeout: 30000, polling: 50 },
    );
    expect(await page.evaluate(() => window.rstudio?.project.isActive())).toBe(false);

    // an ordinary quit now. Were the aborted switch still written down, the
    // session would report this quit as a project switch and the page would
    // reload into the project instead of ending.
    await executeInConsole(page, 'rm(.Last); quit(save = "no")', { wait: false });
    const ended = page.locator(SESSION_ENDED, { hasText: 'R Session Ended' });
    await ended.waitFor({ state: 'visible', timeout: 30000 });
    await ended.getByRole('button', { name: 'Start New Session' }).click();

    await page.waitForLoadState('load', { timeout: 60000 }).catch(() => {});
    await page.waitForFunction(() => window.rstudio?.ready === true, null, { timeout: 90000, polling: 100 });
    expect(await page.evaluate(() => window.rstudio?.project.isActive())).toBe(false);
    expect(await page.evaluate(() => window.rstudio?.project.path())).toBeNull();
  });
});

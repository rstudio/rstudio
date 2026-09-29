// Closing a Plumber API window stops the API (#18987).
//
// With "Run in Window", the API's window tells the main window when it is
// closed, and the main window then interrupts R to stop the API. When that
// notification was lost, the API kept running (and the console stayed busy)
// after its window was gone.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from '@playwright/test';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ensureConsoleIdle, INTERRUPT_R_BTN } from '@pages/console_pane.page';
import { setPref } from '@utils/commands';
import { seedSandboxFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';

const API_FILE = 'plumber-api-window-close.R';
const API_FRAME = 'iframe[title="Plumber API Panel"]';
const API_WINDOW_URL = /view=plumber/;

const API_SOURCE = heredoc`
  #* @get /ping
  function() {
    "pong"
  }
`;

async function launchApiInWindow(
  page: Page,
  consoleActions: ConsolePaneActions,
  apiPath: string,
): Promise<Page> {
  const satellitePromise = page.context().waitForEvent('page', { timeout: 30000 });
  await consoleActions.executeInConsole(
    `plumber::plumb(file = ${rPathLiteral(apiPath)})$run()`,
    { wait: false },
  );
  const satellitePage = await satellitePromise;
  await satellitePage.waitForURL(API_WINDOW_URL);

  // The window only knows which API it is showing once the main window has
  // handed it over, at which point it points its frame at the API's docs.
  await expect(satellitePage.locator(API_FRAME)).toHaveAttribute('src', /\S/, {
    timeout: 30000,
  });

  return satellitePage;
}

test.describe.serial('plumber api window close (#18987)', () => {
  const sandbox = useSuiteSandbox();
  let plumberAvailable = false;
  let apiPath = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    // 180s covers a cold transitive install on a fresh CI runner.
    const consoleActions = new ConsolePaneActions(page);
    plumberAvailable = await consoleActions.ensurePackage('plumber', 180_000);
    await consoleActions.clearConsole();

    await setPref(page, 'plumber_viewer_type', 'window');
    apiPath = await seedSandboxFile(page, sandbox.dir, API_FILE, API_SOURCE);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // A failing run leaves the API serving; free the console for the next spec.
    await ensureConsoleIdle(page);
    for (const satellite of page.context().pages()) {
      if (API_WINDOW_URL.test(satellite.url()))
        await satellite.close().catch(() => {});
    }
  });

  test('closing the window stops the API', async ({ rstudioPage: page }) => {
    test.skip(!plumberAvailable, 'required R package not available: plumber');

    const consoleActions = new ConsolePaneActions(page);
    const satellitePage = await launchApiInWindow(page, consoleActions, apiPath);
    await expect(page.locator(INTERRUPT_R_BTN)).toBeVisible();

    // Close the window from inside the page, as its close button does, so
    // the page runs its own teardown.
    const closePromise = satellitePage.waitForEvent('close', { timeout: 15000 });
    await satellitePage.evaluate(() => {
      setTimeout(() => window.close(), 0);
    });
    await closePromise;

    // The main window confirms the window is really gone before it stops
    // the API, so allow for that on top of the interrupt itself.
    await expect(page.locator(INTERRUPT_R_BTN)).toBeHidden({ timeout: 15000 });
  });
});

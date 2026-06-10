import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand, setPref } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import type { Page } from '@playwright/test';

// Regression tests for https://github.com/rstudio/rstudio/issues/17439:
// with "Run in Window", the Shiny app window could become impossible to
// close (and permanently orphaned) when the app's page registered a
// 'beforeunload' handler that prevents unload. Desktop-only: the fix lives
// in the Electron main process; on Server the browser handles beforeunload.

const APP_DIR = 'shiny-app-17439';
const APP_MARKER = 'hello shiny 17439';
const APP_FRAME = 'iframe[title="Shiny Application"]';

async function launchShinyAppInWindow(page: Page, consoleActions: ConsolePaneActions) {
  const satellitePromise = page.context().waitForEvent('page', { timeout: 30000 });
  await consoleActions.executeInConsole(`shiny::runApp("${APP_DIR}")`, { wait: false });
  const satellitePage = await satellitePromise;
  await satellitePage.waitForLoadState('domcontentloaded');

  // wait until the app is served and rendered inside the satellite's iframe
  await expect(
    satellitePage.frameLocator(APP_FRAME).locator(`body:has-text("${APP_MARKER}")`),
  ).toBeVisible({ timeout: 30000 });

  return satellitePage;
}

// Interrupting a freshly-launched shiny app is racy: a SIGINT that lands
// while shiny is servicing a request/callback is caught by shiny's error
// handling, which bounces the websocket (so the disconnected client still
// closes the window) without stopping runApp() -- leaving R busy forever and
// starving every later console interaction. Once the app is idle a retried
// interrupt always lands, so nudge until the console frees up.
async function ensureConsoleIdle(page: Page) {
  const interruptButton = page.locator("[id^='rstudio_tb_interruptr']");

  // normal path: the app stopped and the console frees up on its own
  try {
    await expect(interruptButton).toBeHidden({ timeout: 3000 });
    return;
  } catch {
    // still busy; fall through and nudge
  }

  // keep the nudges slow: the client counts interrupt requests while busy
  // and escalates to a blocking "Terminate R" dialog on the third one
  await expect(async () => {
    if (await interruptButton.isVisible()) {
      await executeCommand(page, 'interruptR');
      throw new Error('console still busy; interrupt requested again');
    }
  }).toPass({ timeout: 15000, intervals: [2000] });
}

test.describe.serial('shiny app window close', { tag: ['@desktop_only'] }, () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    // a preceding spec can hand off the worker with a main-window reload
    // still in flight (e.g. a project open/close); let it settle first
    await page.waitForFunction(() => window.rstudio?.ready === true, null, {
      timeout: 30000,
      polling: 50,
    });

    const consoleActions = new ConsolePaneActions(page);
    await setPref(page, 'shiny_viewer_type', 'window');
    await consoleActions.executeInConsole(
      heredoc`
        dir.create("${APP_DIR}", showWarnings = FALSE)
        writeLines(c(
          'library(shiny)',
          'shinyApp(fluidPage("${APP_MARKER}"), function(input, output) {})'
        ), "${APP_DIR}/app.R")
      `,
      { wait: true },
    );
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    // if a test failed mid-app, R may still be busy serving it; free the
    // console before driving it
    await ensureConsoleIdle(page);

    const consoleActions = new ConsolePaneActions(page);
    await consoleActions.executeInConsole(`unlink("${APP_DIR}", recursive = TRUE)`, {
      wait: true,
    });
  });

  test('window closes when the app is stopped', async ({ rstudioPage: page }) => {
    test.setTimeout(120000);
    const consoleActions = new ConsolePaneActions(page);
    const satellitePage = await launchShinyAppInWindow(page, consoleActions);

    // stopping the app disconnects the satellite, which should close itself
    const closePromise = satellitePage.waitForEvent('close', { timeout: 15000 });
    await executeCommand(page, 'interruptR');
    await closePromise;

    // the window closing does not guarantee the app stopped (see
    // ensureConsoleIdle); make sure R is back at the prompt so the next
    // test's runApp doesn't sit queued behind a still-running app
    await ensureConsoleIdle(page);
  });

  test('window closes while the app is running', async ({ rstudioPage: page }) => {
    test.setTimeout(120000);
    const consoleActions = new ConsolePaneActions(page);
    const satellitePage = await launchShinyAppInWindow(page, consoleActions);

    const closePromise = satellitePage.waitForEvent('close', { timeout: 15000 });
    await satellitePage.evaluate(() => window.close());
    await closePromise;

    // closing the window also stops the app, unblocking the console
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).toBeHidden({ timeout: 15000 });
  });

  test('window closes despite a beforeunload handler in the app', async ({
    rstudioPage: page,
  }) => {
    test.setTimeout(120000);
    const consoleActions = new ConsolePaneActions(page);
    const satellitePage = await launchShinyAppInWindow(page, consoleActions);

    // simulate an app that prompts before leaving, as the app in
    // rstudio#17502 does via window.onbeforeunload; the user gesture (click)
    // is required for Chromium to honor the handler
    await satellitePage.frameLocator(APP_FRAME).locator('body').click();
    const appFrame = satellitePage.frames().find((f) => /127\.0\.0\.1:\d+\/?$/.test(f.url()));
    expect(appFrame).toBeTruthy();
    await appFrame!.evaluate(() => {
      window.addEventListener('beforeunload', (e) => {
        e.preventDefault();
        e.returnValue = '';
      });
    });

    // without the will-prevent-unload handling (rstudio#17439), this close
    // is silently cancelled and the window is orphaned; the e2e harness runs
    // with RSTUDIO_DESKTOP_IGNORE_BEFOREUNLOAD=1 so no native dialog shows.
    // the beforeunload confirmation can still surface as a CDP dialog, and
    // Playwright's default dismissal races the closing window -- handle it
    // ourselves and tolerate the window being gone by the time we respond
    satellitePage.on('dialog', (dialog) => {
      void dialog.accept().catch(() => {});
    });
    const closePromise = satellitePage.waitForEvent('close', { timeout: 15000 });
    await satellitePage.evaluate(() => window.close());
    await closePromise;

    await expect(page.locator("[id^='rstudio_tb_interruptr']")).toBeHidden({ timeout: 15000 });
  });
});

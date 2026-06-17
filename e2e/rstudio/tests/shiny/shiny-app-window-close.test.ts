import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand, setPref, stopForegroundShinyApp } from '@utils/commands';
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

  // The marker being visible only proves the first render landed; shiny may
  // still be mid-binding (input/output reactives running their initial pass)
  // and an interrupt that arrives during that window is caught by shiny's
  // error handling instead of stopping runApp(). Wait until shiny is actually
  // idle -- it sets html.shiny-busy while servicing requests and clears it
  // once the queue drains. Caller-side `executeCommand(page, 'interruptR')`
  // then lands cleanly on the first try.
  await waitForShinyIdle(satellitePage);

  return satellitePage;
}

async function waitForShinyIdle(satellitePage: Page) {
  const html = satellitePage.frameLocator(APP_FRAME).locator('html');
  // html.shiny-busy is the canonical idle signal: shiny adds the class while
  // servicing a request/render and removes it once the queue drains. Initial
  // binding flips it on/off as widgets register, so a single "not busy"
  // sample can land in the gap between two render passes -- require N
  // consecutive clear samples before declaring shiny stably idle. This is a
  // measurable-condition poll, not a fixed dwell.
  let stableClearSamples = 0;
  await expect
    .poll(
      async () => {
        const busy = await html.evaluate((el) =>
          el.classList.contains('shiny-busy'),
        );
        stableClearSamples = busy ? 0 : stableClearSamples + 1;
        return stableClearSamples;
      },
      { timeout: 15000, intervals: [100] },
    )
    .toBeGreaterThanOrEqual(5);
}

// Defense in depth after the launcher waits for shiny idle. If a previous
// test left R busy (e.g. interrupt landed mid-callback) the interrupt button
// stays visible -- send one nudge and bail. Re-spamming interruptR every 2s
// makes the IDE stack "Terminate R" confirmation dialogs (one per request
// while R is unresponsive), which is what the historical 30s-toPass loop
// produced when it ran past the third interrupt.
async function ensureConsoleIdle(page: Page) {
  const interruptButton = page.locator("[id^='rstudio_tb_interruptr']");
  try {
    await expect(interruptButton).toBeHidden({ timeout: 3000 });
    return;
  } catch {
    // still busy; fall through and try one more interrupt
  }
  await executeCommand(page, 'interruptR');
  await expect(interruptButton).toBeHidden({ timeout: 10000 });
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
    // console before driving it. Wrap in try/finally so a still-busy console
    // can't leak the temp app dir into the next worker -- the unlink is the
    // important side effect here, not the idle assertion.
    try {
      await ensureConsoleIdle(page);
    } finally {
      const consoleActions = new ConsolePaneActions(page);
      await consoleActions
        .executeInConsole(`unlink("${APP_DIR}", recursive = TRUE)`, { wait: true })
        .catch((err) => {
          console.warn(
            `[shiny-app-window-close] cleanup unlink failed (R may be stuck): ${(err as Error).message}`,
          );
        });
    }
  });

  test('window closes when the app is stopped', async ({ rstudioPage: page }) => {
    test.setTimeout(120000);
    const consoleActions = new ConsolePaneActions(page);
    const satellitePage = await launchShinyAppInWindow(page, consoleActions);

    // stopping the app disconnects the satellite, which should close itself.
    // Use the automation bridge (shiny::stopApp via stop_shiny_app RPC)
    // instead of the interrupt button: interrupt sends CTRL_BREAK_EVENT on
    // Windows and R's R_interrupts_pending flag is only polled at certain
    // points inside runApp's event loop, so the signal can be swallowed --
    // the historical Windows fixme on this test was the symptom. The RPC
    // goes through shiny's normal shutdown on every platform and returns
    // once R has exited runApp.
    const closePromise = satellitePage.waitForEvent('close', { timeout: 15000 });
    await stopForegroundShinyApp(page);
    await closePromise;

    // closePromise resolving means the satellite closed, but the rsession
    // RPC also waits for R to exit runApp -- so the interrupt button (R's
    // busy indicator) should already be hidden. Sanity-check it.
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).toBeHidden({ timeout: 5000 });
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

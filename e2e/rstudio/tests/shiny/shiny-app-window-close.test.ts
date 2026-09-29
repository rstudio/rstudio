import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ensureConsoleIdle } from '@pages/console_pane.page';
import { clearPref, setPref, stopForegroundShinyApp } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import type { Page } from '@playwright/test';

// Regression tests for https://github.com/rstudio/rstudio/issues/17439:
// with "Run in Window", the Shiny app window could become impossible to
// close (and permanently orphaned) when the app's page registered a
// 'beforeunload' handler that prevents unload. That test is Desktop-only:
// the fix lives in the Electron main process; on Server the browser handles
// beforeunload.
//
// The other tests also cover Server, where the window itself has to tell the
// main window that it was closed (#18987). Two of them cover ways for that
// notification to go missing: the window being unable to send it (Desktop
// then falls back on Electron's own report of the closed window), and a close
// that the app's page cancelled before the window was closed for real.

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

// The app window reports its closure by calling notifyShinyAppClosed() on
// the main window. Count those calls, and optionally swallow them to stand in
// for a window that could not make the call.
async function interceptCloseNotification(page: Page, options: { drop: boolean }) {
  await page.evaluate(({ drop }) => {
    const w = window as unknown as Record<string, any>;
    const original = w.notifyShinyAppClosed;
    w.__shinyCloseNotifications = 0;
    w.__restoreShinyCloseNotification = () => {
      w.notifyShinyAppClosed = original;
    };
    w.notifyShinyAppClosed = function (...args: unknown[]) {
      w.__shinyCloseNotifications++;
      if (!drop)
        original.apply(this, args);
    };
  }, options);
}

async function closeNotificationCount(page: Page): Promise<number> {
  return page.evaluate(
    () => (window as unknown as Record<string, any>).__shinyCloseNotifications,
  );
}

async function restoreCloseNotification(page: Page) {
  await page.evaluate(() => {
    const w = window as unknown as Record<string, any>;
    if (w.__restoreShinyCloseNotification)
      w.__restoreShinyCloseNotification();
    delete w.__restoreShinyCloseNotification;
  });
}

test.describe.serial('shiny app window close', () => {
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
    // console (interrupting, and terminating R if the interrupt is caught by
    // shiny mid-callback) before driving it. ensureConsoleIdle never throws,
    // so the unlink below always runs.
    await ensureConsoleIdle(page);

    const consoleActions = new ConsolePaneActions(page);
    await consoleActions
      .executeInConsole(`unlink("${APP_DIR}", recursive = TRUE)`, { wait: true })
      .catch((err) => {
        console.warn(
          `[shiny-app-window-close] cleanup unlink failed (R may be stuck): ${(err as Error).message}`,
        );
      });

    await clearPref(page, 'shiny_viewer_type');
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await restoreCloseNotification(page);
  });

  test('window closes when the app is stopped', async ({ rstudioPage: page }) => {
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
    const consoleActions = new ConsolePaneActions(page);
    const satellitePage = await launchShinyAppInWindow(page, consoleActions);

    const closePromise = satellitePage.waitForEvent('close', { timeout: 15000 });
    await satellitePage.evaluate(() => window.close());
    await closePromise;

    // closing the window also stops the app, unblocking the console
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).toBeHidden({ timeout: 15000 });
  });

  test('window closes despite a beforeunload handler in the app', { tag: ['@desktop_only'] }, async ({
    rstudioPage: page,
  }) => {
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

  test('app stops when the window cannot report that it closed', { tag: ['@desktop_only'] }, async ({
    rstudioPage: page,
  }) => {
    const consoleActions = new ConsolePaneActions(page);
    const satellitePage = await launchShinyAppInWindow(page, consoleActions);
    await interceptCloseNotification(page, { drop: true });

    const closePromise = satellitePage.waitForEvent('close', { timeout: 15000 });
    await satellitePage.evaluate(() => window.close());
    await closePromise;

    // the window did try, so what stops the app is Electron's report
    expect(await closeNotificationCount(page)).toBeGreaterThan(0);
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).toBeHidden({ timeout: 15000 });
  });

  // Server-only: the e2e harness runs Desktop with
  // RSTUDIO_DESKTOP_IGNORE_BEFOREUNLOAD=1, so a close cannot be cancelled there.
  test('app stops when the window is closed after a cancelled close', { tag: ['@server_only'] }, async ({
    rstudioPage: page,
  }) => {
    const consoleActions = new ConsolePaneActions(page);
    const satellitePage = await launchShinyAppInWindow(page, consoleActions);
    await interceptCloseNotification(page, { drop: false });

    // an app that prompts before leaving, until told otherwise; the user
    // gesture (click) is required for Chromium to honor the handler
    const appBody = satellitePage.frameLocator(APP_FRAME).locator('body');
    await appBody.click();
    await appBody.evaluate(() => {
      const w = window as unknown as Record<string, any>;
      w.__promptBeforeLeaving = true;
      window.addEventListener('beforeunload', (e) => {
        if (!w.__promptBeforeLeaving)
          return;
        e.preventDefault();
        e.returnValue = '';
      });
    });

    // first attempt: the user chooses to stay
    const prompt = satellitePage.waitForEvent('dialog', { timeout: 15000 });
    await satellitePage.evaluate(() => {
      setTimeout(() => window.close(), 0);
    });
    await (await prompt).dismiss();

    // The window is still open, so it must not have reported a closure. A
    // window that reports one here stops the app only if the real close
    // happens to follow within the few seconds the main window keeps
    // checking on it.
    expect(satellitePage.isClosed()).toBe(false);
    expect(await closeNotificationCount(page)).toBe(0);
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).toBeVisible();

    // second attempt: close for real
    await appBody.evaluate(() => {
      (window as unknown as Record<string, any>).__promptBeforeLeaving = false;
    });
    const closePromise = satellitePage.waitForEvent('close', { timeout: 15000 });
    await satellitePage.evaluate(() => window.close());
    await closePromise;

    expect(await closeNotificationCount(page)).toBeGreaterThan(0);
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).toBeHidden({ timeout: 15000 });
  });
});

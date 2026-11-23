import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { executeCommand } from '@utils/commands';
import { restartSessionWithSentinel, waitForSessionRestart } from '@utils/project';
import { dismissBlockingModals } from '@pages/modals.page';
import { executeInConsole } from '@pages/console_pane.page';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import type { BrowserContext, Locator, Page } from 'playwright';
import { setupPositAssistantChat } from './_chat-setup';

// Regression tests for rstudio/rstudio#19695: suspend and Restart R stop the
// Posit Assistant backend, whose reconnect failures raised a "connection lost"
// banner that outlived the resume. These need a running backend, not sign-in.
// The locators search the GWT document, not the iframe's. The banner's text
// comes from Posit Assistant, so match it by RStudio's Restart button instead.
const lostBanner = (page: Page): Locator =>
  page.getByRole('button', { name: 'Restart Posit Assistant' });
const suspendedNotice = (page: Page): Locator =>
  page.getByText(/The R session is suspended\. Posit Assistant will reconnect/);
const restartingNotice = (page: Page): Locator =>
  page.getByText(/Restarting R\. Posit Assistant will reconnect/);

const CHAT_IFRAME = "iframe[title='Posit Assistant']";
const RETURN_TO_MAIN_BTN = '#rstudio_chat_return_to_main_button';

/** The URL of the document currently loaded in the chat iframe ('' if none). */
async function chatDocumentUrl(page: Page): Promise<string> {
  return page.evaluate((selector) => {
    const frame = document.querySelector(selector) as HTMLIFrameElement | null;
    return frame?.contentDocument?.location.href ?? '';
  }, CHAT_IFRAME);
}

// The Posit Assistant client reports a lost connection on its 3rd failed
// reconnect, one second apart. Watch well past that.
const RECONNECT_REPORT_WINDOW_MS = 8000;

/**
 * Fails on the first sample, over the whole window, where the banner shows.
 * expect.poll can't assert absence: it returns on the first passing sample.
 */
async function expectBannerNeverShown(page: Page, windowMs: number, when: string): Promise<void> {
  const deadline = Date.now() + windowMs;
  while (Date.now() < deadline) {
    const shown = await lostBanner(page).isVisible().catch(() => false);
    expect(shown, `"connection lost" banner shown ${when}`).toBe(false);
    await sleep(250);
  }
}

/**
 * Counts ChatPresenter's "Chat iframe error:" log lines, which it writes even
 * when the banner is suppressed. A suppression check is only meaningful if the
 * outgoing page reported a failure during it.
 */
function watchIframeErrors(page: Page): { count: () => number; stop: () => void } {
  let count = 0;
  const onConsole = (msg: { text(): string }): void => {
    if (msg.text().includes('Chat iframe error:')) count++;
  };
  page.on('console', onConsole);
  return { count: () => count, stop: () => page.off('console', onConsole) };
}

// Client state is pushed on a passive 5s timer, and that RPC resumes a
// suspended session. The per-test fixture dirties it (resetting the source
// pane), so suspending right away gets the session resumed mid-test.
const CLIENT_STATE_QUIET_MS = 6000;

/** Waits until no set_client_state request has started for CLIENT_STATE_QUIET_MS. */
async function waitForClientStateQuiet(page: Page): Promise<void> {
  let lastPushMs = Date.now();
  const onRequest = (request: { url(): string }): void => {
    if (request.url().includes('/rpc/set_client_state')) lastPushMs = Date.now();
  };
  page.on('request', onRequest);
  try {
    while (Date.now() - lastPushMs < CLIENT_STATE_QUIET_MS) {
      await sleep(250);
    }
  } finally {
    page.off('request', onRequest);
  }
}

// Any RPC to a suspended session resumes it, and the client-state push runs
// every 5s. Watch past that.
const STAYS_SUSPENDED_WINDOW_MS = 8000;

/** Fails if any page in the context starts an RPC within the window. */
async function expectNoRpcs(context: BrowserContext, windowMs: number): Promise<void> {
  const rpcs: string[] = [];
  const onRequest = (request: { url(): string }): void => {
    if (request.url().includes('/rpc/')) rpcs.push(request.url());
  };
  context.on('request', onRequest);
  try {
    await sleep(windowMs);
  } finally {
    context.off('request', onRequest);
  }
  expect(rpcs, 'RPCs sent while suspended').toEqual([]);
}

test.describe.serial('Posit Assistant across suspend and restart', {
  tag: ['@ai', '@chat', '@server_only'],
}, () => {
  let chatActions: ChatPaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    ({ chatActions } = await setupPositAssistantChat(page));
  });

  test.beforeEach(async () => {
    await chatActions.waitForChatReady();
  });

  test('suspend shows a notice instead of "connection lost"; resume clears it', async ({
    rstudioPage: page,
  }) => {
    await waitForClientStateQuiet(page);
    // Suspend cooperatively, as the inactivity timeout does. Session > Suspend
    // is forced and doesn't reliably deliver SUSPEND_SESSION (see below).
    const iframeErrors = watchIframeErrors(page);
    try {
      await executeInConsole(page, '.rs.suspendSession(force = FALSE)', { wait: false });

      await expect(suspendedNotice(page)).toBeVisible({ timeout: 15000 });

      // The session stays suspended while nothing issues an RPC, so the old
      // iframe keeps failing to reconnect for this whole window.
      await expectBannerNeverShown(page, RECONNECT_REPORT_WINDOW_MS, 'while suspended');
      await expect(suspendedNotice(page)).toBeVisible();
      expect(iframeErrors.count(), 'iframe errors reported while suspended').toBeGreaterThan(0);
    } finally {
      iframeErrors.stop();
    }

    await waitForSessionRestart(page);
    await dismissBlockingModals(page);

    // The notice hides only once the old page is replaced, so the ready check
    // that follows can't match the old page's composer
    await expect(suspendedNotice(page)).toBeHidden({ timeout: 15000 });
    await chatActions.waitForChatReady();
    await expectBannerNeverShown(page, 3000, 'after resume');
  });

  test('a slow Restart R does not leave a "connection lost" banner', async ({
    rstudioPage: page,
  }) => {
    // Hold the post-restart backend lookup until the checks below are done, so
    // the pre-restart iframe outlives the reconnect-report threshold, as it
    // does when a restart is slow.
    let held = 0;
    let release!: () => void;
    const released = new Promise<void>((resolve) => { release = resolve; });
    await page.route(/\/rpc\/chat_get_backend_status/, async (route) => {
      held++;
      await released;
      await route.continue().catch(() => undefined);
    });

    const iframeErrors = watchIframeErrors(page);
    try {
      await restartSessionWithSentinel(page);
      await dismissBlockingModals(page);
      // The held lookup keeps the pre-restart iframe, and its notice, in place
      await expect(restartingNotice(page)).toBeVisible();
      await expectBannerNeverShown(page, RECONNECT_REPORT_WINDOW_MS, 'during restart');
      expect(iframeErrors.count(), 'iframe errors reported during restart').toBeGreaterThan(0);
    } finally {
      iframeErrors.stop();
      release();
      await page.unroute(/\/rpc\/chat_get_backend_status/);
    }
    expect(held, 'chat_get_backend_status calls held after restart').toBeGreaterThan(0);

    await expect(restartingNotice(page)).toBeHidden({ timeout: 15000 });
    await chatActions.waitForChatReady();
    await expectBannerNeverShown(page, 3000, 'after restart');
  });

  // The issue's repro. A forced suspend doesn't flush pending events before
  // exiting, so SUSPEND_SESSION reaches the client only if an events poll picks
  // it up first. Blocking the polls loses it every time; the old page's banner
  // then shows while suspended, and the reload on resume must clear it.
  test('Session > Suspend without SUSPEND_SESSION: resume clears the banner', async ({
    rstudioPage: page,
  }) => {
    await waitForClientStateQuiet(page);
    await page.route(/\/events\/get_events/, (route) => route.abort());
    try {
      // The route only stops new polls. Wait for the in-flight one (held for
      // up to 50s) to finish and the next to hit the block, or it could still
      // deliver SUSPEND_SESSION.
      await page.waitForEvent('requestfailed', {
        predicate: (request) => request.url().includes('/events/get_events'),
        timeout: 60000,
      });
      await executeCommand(page, 'suspendSession');
      await expect(lostBanner(page)).toBeVisible({ timeout: 15000 });
    } finally {
      await page.unroute(/\/events\/get_events/);
    }
    await expect(suspendedNotice(page)).toBeHidden();
    const suspendedDocumentUrl = await chatDocumentUrl(page);

    await waitForSessionRestart(page);
    await dismissBlockingModals(page);

    // With no overlay up, the banner is hidden as the reload starts, while the
    // old document is still loaded; wait for the new one to finish loading
    await page.waitForFunction(({ selector, before }) => {
      const doc = (document.querySelector(selector) as HTMLIFrameElement | null)?.contentDocument;
      return !!doc && doc.location.href !== before && doc.readyState === 'complete';
    }, { selector: CHAT_IFRAME, before: suspendedDocumentUrl }, { timeout: 30000 });
    await expect(lostBanner(page)).toBeHidden();
    await chatActions.waitForChatReady();
  });

  // #19699: saving the popped-out window's geometry on suspend dirtied client
  // state, whose next push resumed the session. The window shows the notice too.
  test('popped-out chat stays suspended and shows the notice until resume', async ({
    rstudioPage: page,
  }) => {
    const satellitePromise = page.context().waitForEvent('page', { timeout: 30000 });
    await executeCommand(page, 'popOutChat');
    const satellite = await satellitePromise;
    try {
      await expect(satellite.locator(RETURN_TO_MAIN_BTN)).toBeVisible({ timeout: 15000 });
      // Popping out saves client state
      await waitForClientStateQuiet(page);

      await executeInConsole(page, '.rs.suspendSession(force = FALSE)', { wait: false });
      await expect(suspendedNotice(satellite)).toBeVisible({ timeout: 15000 });
      await expectNoRpcs(page.context(), STAYS_SUSPENDED_WINDOW_MS);
      await expect(suspendedNotice(satellite)).toBeVisible();

      await waitForSessionRestart(page);
      await dismissBlockingModals(page);
      await expect(suspendedNotice(satellite)).toBeHidden({ timeout: 15000 });
    } finally {
      // Closing the window returns chat to the main window. Under Playwright,
      // chat can already be back there after resume with this window still
      // open (manual testing in Chrome keeps chat popped out).
      await satellite.close();
    }
  });
});

import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { executeCommand } from '@utils/commands';
import { restartSessionWithSentinel, waitForSessionRestart } from '@utils/project';
import { dismissBlockingModals } from '@pages/modals.page';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import type { Locator, Page } from 'playwright';
import { setupPositAssistantChat } from './_chat-setup';

// Regression tests for rstudio/rstudio#19695: suspend and Restart R stop the
// Posit Assistant backend, whose reconnect failures raised a "connection lost"
// banner that outlived the resume. These need a running backend, not sign-in.
// The locators search the GWT document, not the iframe's.
const lostBanner = (page: Page): Locator =>
  page.getByText(/Connection to Posit Assistant was lost/);
const suspendedNotice = (page: Page): Locator =>
  page.getByText(/The R session is suspended\. Posit Assistant will reconnect/);
const restartingNotice = (page: Page): Locator =>
  page.getByText(/Restarting R\. Posit Assistant will reconnect/);

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
    await executeCommand(page, 'suspendSession');

    await expect(suspendedNotice(page)).toBeVisible({ timeout: 15000 });

    // The session stays suspended while nothing issues an RPC, so the old
    // iframe keeps failing to reconnect for this whole window.
    await expectBannerNeverShown(page, RECONNECT_REPORT_WINDOW_MS, 'while suspended');
    await expect(suspendedNotice(page)).toBeVisible();

    await waitForSessionRestart(page);
    await dismissBlockingModals(page);
    await chatActions.waitForChatReady();

    await expect(suspendedNotice(page)).toBeHidden({ timeout: 15000 });
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

    try {
      await restartSessionWithSentinel(page);
      await dismissBlockingModals(page);
      // The held lookup keeps the pre-restart iframe, and its notice, in place
      await expect(restartingNotice(page)).toBeVisible();
      await expectBannerNeverShown(page, RECONNECT_REPORT_WINDOW_MS, 'during restart');
    } finally {
      release();
      await page.unroute(/\/rpc\/chat_get_backend_status/);
    }
    expect(held, 'chat_get_backend_status calls held after restart').toBeGreaterThan(0);

    await chatActions.waitForChatReady();
    await expect(restartingNotice(page)).toBeHidden({ timeout: 15000 });
    await expectBannerNeverShown(page, 3000, 'after restart');
  });
});

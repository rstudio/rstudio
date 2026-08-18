import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { sleep, CHAT_PROVIDERS, TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { restartSessionWithSentinel } from '@utils/project';
import { executeCommand } from '@utils/commands';
import type { Page } from 'playwright';
import { createChatActions } from './_chat-setup';

const CHAT_IFRAME = "iframe[title='Posit Assistant']";
const PREFS_OK_BTN = '#rstudio_preferences_confirm';

// Posit Assistant is configured but not necessarily installed. Both tests
// just need the iframe to exist; they don't depend on the chat backend being
// up. We deliberately skip dismissSetupPrompts() so the iframe state stays
// stable across runs and we don't pay the install cost.
async function ensureChatPaneVisible(page: Page, chatActions: ChatPaneActions): Promise<void> {
  const iframe = page.locator(CHAT_IFRAME);
  if (!(await iframe.isVisible().catch(() => false))) {
    await chatActions.openChatPane();
  }
  await expect(iframe).toBeVisible({ timeout: 15000 });
  // Iframe writes content via setFrameContent() which fires multiple load
  // events (about:blank + doc.open/write/close). Let those settle before
  // attaching the counter.
  await sleep(2000);
}

test.describe.serial('Chat pane persistence', { tag: ['@ai', '@chat'] }, () => {
  requireAiCredentials(test, 'positai');

  let consoleActions: ConsolePaneActions;
  let chatActions: ChatPaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    const actions = createChatActions(page);
    consoleActions = actions.consoleActions;
    chatActions = actions.chatActions;

    await consoleActions.clearConsole();
    await actions.assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);
  });

  // Regression test for rstudio/rstudio#17223: dismissing Global Options
  // without changes was triggering a chat iframe reload.
  test('chat iframe does not reload when Global Options dismissed without changes', async ({
    rstudioPage: page,
  }) => {
    await ensureChatPaneVisible(page, chatActions);

    // Attach a load-event counter to the iframe element. Any reload --
    // setUrl() or doc.open()/write()/close() -- fires this listener.
    await page.evaluate((selector) => {
      const f = document.querySelector(selector) as
        | (HTMLIFrameElement & { _reloadCount?: number })
        | null;
      if (!f) throw new Error('chat iframe not found');
      f._reloadCount = 0;
      f.addEventListener('load', () => {
        f._reloadCount = (f._reloadCount ?? 0) + 1;
      });
    }, CHAT_IFRAME);

    // Open Global Options. expect-visible auto-waits for the dialog to render,
    // so the subsequent click() doesn't need an extra settling delay.
    await executeCommand(page, 'showOptions');
    const okBtn = page.locator(PREFS_OK_BTN);
    await expect(okBtn).toBeVisible({ timeout: 15000 });

    // Click OK without making any changes
    await okBtn.click();
    await expect(okBtn).toBeHidden({ timeout: 15000 });

    // Deliberate observation window: this is a test for the *absence* of a
    // reload event. If a spurious load was going to fire, it would have
    // started within this window. Don't shrink without a stronger signal.
    await sleep(1000);

    const reloadCount = await page.evaluate((selector) => {
      const f = document.querySelector(selector) as
        | (HTMLIFrameElement & { _reloadCount?: number })
        | null;
      return f?._reloadCount ?? -1;
    }, CHAT_IFRAME);

    expect(reloadCount, 'chat iframe reload count after Options OK').toBe(0);
  });

  // Regression test for rstudio/rstudio#17240: chat pane must remain
  // populated after an R session restart.
  test('chat pane content survives R session restart', async ({ rstudioPage: page }) => {
    await ensureChatPaneVisible(page, chatActions);

    // Tag the pre-restart iframe. On resume ChatPane double-buffers: it loads
    // the resume URL into a second (untitled, hidden) frame and only removes
    // this one once that load completes, so the tag is how we tell the
    // resumed frame from the one we started with.
    //
    // The observer records the high-water mark of titled iframes across the
    // resume (rstudio/rstudio#18585): the pending frame must stay untitled
    // until it replaces this one, or every lookup by title is ambiguous for
    // the length of the load. Callbacks run after each mutation batch, so the
    // swap's synchronous title-then-remove sequence is only ever seen in its
    // end state -- a reading of 2 means the frames genuinely coexisted.
    await page.evaluate((selector) => {
      const f = document.querySelector(selector);
      if (!f) throw new Error('chat iframe not found');
      f.setAttribute('data-pw-prerestart', '');

      const w = window as Window & {
        _maxChatFrames?: number;
        _chatFrameObserver?: MutationObserver;
      };
      const count = () => document.querySelectorAll(selector).length;
      w._maxChatFrames = count();
      w._chatFrameObserver?.disconnect();
      w._chatFrameObserver = new MutationObserver(() => {
        w._maxChatFrames = Math.max(w._maxChatFrames ?? 0, count());
      });
      w._chatFrameObserver.observe(document.body, {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ['title'],
      });
    }, CHAT_IFRAME);

    // Restart the R session (sentinel-confirmed)
    await restartSessionWithSentinel(page);

    // Re-open the chat pane (toggleSidebar may have hidden it during restart)
    await ensureChatPaneVisible(page, chatActions);

    // Best-effort wait for the swap, so the body sampled below is the
    // post-restart document rather than the preserved pre-restart one.
    // window.rstudio.ready (awaited above) only covers GWT's own init; the
    // chat resume load is independent of it. If the resumed frame stalls,
    // ChatPane reclaims it after FRAME_LOAD_TIMEOUT_MS and reloads the
    // original element instead -- the pane stays populated either way, which
    // is the property under test, so a miss here is logged, not fatal.
    const swapped = await page
      .waitForFunction(
        (selector) => {
          const f = document.querySelector(selector);
          return f !== null && !f.hasAttribute('data-pw-prerestart');
        },
        CHAT_IFRAME,
        { timeout: 20000, polling: 250 },
      )
      .then(() => true)
      .catch(() => false);

    if (!swapped) {
      console.log('resume swap did not replace the chat iframe within 20s -- ' +
        'asserting against the pre-restart frame');
    }

    const maxChatFrames = await page.evaluate(() => {
      const w = window as Window & {
        _maxChatFrames?: number;
        _chatFrameObserver?: MutationObserver;
      };
      w._chatFrameObserver?.disconnect();
      return w._maxChatFrames ?? -1;
    });

    expect(maxChatFrames, 'iframes titled "Posit Assistant" attached at once during resume').toBe(1);

    // The iframe should have rendered content -- either the chat app root
    // or the "Not Installed" page. Both share a non-empty body.
    const bodyText = await page
      .locator(CHAT_IFRAME)
      .contentFrame()
      .locator('body')
      .innerText({ timeout: TIMEOUTS.consoleReady });

    expect(bodyText.trim().length, 'chat iframe body should not be empty after restart').toBeGreaterThan(0);
  });
});

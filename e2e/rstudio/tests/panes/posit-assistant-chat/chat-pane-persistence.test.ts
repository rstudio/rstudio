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
const PRERESTART_ATTR = 'data-pw-prerestart';

// In-page state for the resume-window frame counts (see the restart test).
// maxTitled is the invariant under test; maxSiblings says whether the resume
// double-buffered at all, so a passing run can't quietly claim coverage it
// didn't get.
type ChatFrameWatch = {
  counts: { maxTitled: number; maxSiblings: number };
  observer: MutationObserver;
};
type WatchWindow = Window & { _chatFrameWatch?: ChatFrameWatch };

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

  // The restart test's observer and marker live on a page shared with every
  // later test in this worker, so drop them even when that test throws before
  // its own read. Cleanup must not mask the failure that got us here.
  test.afterEach(async ({ rstudioPage: page }) => {
    await page
      .evaluate(({ selector, attr }) => {
        const w = window as WatchWindow;
        w._chatFrameWatch?.observer.disconnect();
        delete w._chatFrameWatch;
        document.querySelector(selector)?.removeAttribute(attr);
      }, { selector: CHAT_IFRAME, attr: PRERESTART_ATTR })
      .catch(() => undefined);
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
  test('chat pane content survives R session restart', async ({ rstudioPage: page }, testInfo) => {
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
    //
    // It also counts iframes under the chat pane's panel, where the pending
    // frame is attached as a sibling of this one. That count reaching 2 is
    // what proves the double-buffer ran, which the titled count alone cannot
    // show: it starts at 1 and stays there both when the fix holds and when
    // the resume never double-buffers.
    await page.evaluate(({ selector, attr }) => {
      const f = document.querySelector(selector);
      if (!f) throw new Error('chat iframe not found');
      f.setAttribute(attr, '');

      const pane = f.parentElement;
      const titled = () => document.querySelectorAll(selector).length;
      const siblings = () => pane?.querySelectorAll(':scope > iframe').length ?? 0;
      const counts = { maxTitled: titled(), maxSiblings: siblings() };
      const observer = new MutationObserver(() => {
        counts.maxTitled = Math.max(counts.maxTitled, titled());
        counts.maxSiblings = Math.max(counts.maxSiblings, siblings());
      });

      const w = window as WatchWindow;
      w._chatFrameWatch?.observer.disconnect();
      w._chatFrameWatch = { counts, observer };
      observer.observe(document.body, {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ['title'],
      });
    }, { selector: CHAT_IFRAME, attr: PRERESTART_ATTR });

    // Restart the R session (sentinel-confirmed)
    await restartSessionWithSentinel(page);

    // Re-open the chat pane (toggleSidebar may have hidden it during restart)
    await ensureChatPaneVisible(page, chatActions);

    // Best-effort wait for the swap, so the body sampled below is the
    // post-restart document rather than the preserved pre-restart one. If the
    // resumed frame stalls, ChatPane reclaims it after FRAME_LOAD_TIMEOUT_MS
    // and reloads the original element instead -- the pane stays populated
    // either way, which is the property under test, so a miss is annotated
    // below rather than failed.
    const swapped = await page
      .waitForFunction(
        ({ selector, attr }) => {
          const f = document.querySelector(selector);
          return f !== null && !f.hasAttribute(attr);
        },
        { selector: CHAT_IFRAME, attr: PRERESTART_ATTR },
        { timeout: 20000, polling: 250 },
      )
      .then(() => true)
      .catch(() => false);

    // The iframe should have rendered content -- either the chat app root
    // or the "Not Installed" page. Both share a non-empty body.
    const bodyText = await page
      .locator(CHAT_IFRAME)
      .contentFrame()
      .locator('body')
      .innerText({ timeout: TIMEOUTS.consoleReady });

    // Read the counts only now: the resume load is independent of
    // window.rstudio.ready, so on a slow worker the pending frame can be
    // attached late, and disconnecting the observer any earlier could close
    // the window before it opens.
    const counts = await page.evaluate(() => {
      const w = window as WatchWindow;
      w._chatFrameWatch?.observer.disconnect();
      return w._chatFrameWatch?.counts ?? { maxTitled: -1, maxSiblings: -1 };
    });

    // Record what the run actually exercised. The double-buffer only runs when
    // the chat backend was loaded before the restart: with Posit Assistant not
    // installed, ChatPresenter never caches a URL, so it neither dims the pane
    // on suspend nor reloads it on resume -- it just re-polls install status.
    // That is a legitimate environment, not a failure, but it does mean the
    // #18585 guard below proved nothing, so say so rather than passing quietly.
    if (counts.maxSiblings < 2) {
      const note = 'resume did not double-buffer (no pending frame attached) -- ' +
        'the duplicate-title assertion was not exercised';
      console.log(note);
      testInfo.annotations.push({ type: 'coverage-gap', description: note });
    } else if (!swapped) {
      const note = 'resume double-buffered but did not swap within 20s -- body asserted ' +
        'against the pre-restart frame';
      console.log(note);
      testInfo.annotations.push({ type: 'coverage-gap', description: note });
    }

    expect(bodyText.trim().length, 'chat iframe body should not be empty after restart').toBeGreaterThan(0);

    expect(counts.maxTitled, 'iframes titled "Posit Assistant" attached at once during resume').toBe(1);
  });
});

import { test, expect } from '@fixtures/rstudio.fixture';
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

test.describe.serial('Chat pane persistence', { tag: ['@ai'] }, () => {
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

    // Open Global Options
    await executeCommand(page, 'showOptions');
    const okBtn = page.locator(PREFS_OK_BTN);
    await expect(okBtn).toBeVisible({ timeout: 15000 });
    await sleep(500); // let dialog finish opening

    // Click OK without making any changes
    await okBtn.click();
    await expect(okBtn).toBeHidden({ timeout: 15000 });

    // Brief pause to allow any spurious reload to start
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

    // Restart the R session (sentinel-confirmed)
    await restartSessionWithSentinel(page);

    // Re-open the chat pane (toggleSidebar may have hidden it during restart)
    await ensureChatPaneVisible(page, chatActions);

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

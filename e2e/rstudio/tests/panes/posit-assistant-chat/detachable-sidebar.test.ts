import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import type { Page } from 'playwright';
import { setupPositAssistantChat, annotateVersions } from './_chat-setup';

// Return button in satellite window (has explicit ElementIds assignment)
const RETURN_TO_MAIN_BTN = '#rstudio_chat_return_to_main_button';

test.describe.serial('Detachable Assistant Sidebar - #16937', { tag: ['@ai', '@desktop_only'] }, () => {
  requireAiCredentials(test, 'positai');

  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let consoleActions: ConsolePaneActions;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    ({ chatActions, chatPane, consoleActions, versions } = await setupPositAssistantChat(page));
  });

  test.beforeEach(async () => {
    annotateVersions(versions);
  });

  test('conversation continuity across detach and reattach', async ({ rstudioPage: page }) => {
    // --- Phase 1: Send a message in main window ---
    await chatActions.startNewConversation();

    const initialCount = await chatPane.getMessageCount();
    await chatActions.sendChatMessage('Who is the Norse god of mischief? Answer in one word.');
    const countAfterFirst = await chatActions.waitForResponse(initialCount);
    expect(countAfterFirst).toBeGreaterThan(initialCount);

    // Verify the response contains "Loki"
    const firstResponse = chatPane.messageItem.last();
    await expect(firstResponse).toContainText('Loki', { timeout: 10000 });

    // Record message count before detach
    const messagesBeforeDetach = await chatPane.getMessageCount();

    // --- Phase 2: Pop out to satellite window ---
    const context = page.context();

    // Listen for the new page (satellite window) before triggering pop-out
    const satellitePromise = context.waitForEvent('page', { timeout: 30000 });
    await page.locator("[id^='rstudio_tb_popoutchat']").click();
    const satellitePage = await satellitePromise;
    await satellitePage.waitForLoadState('domcontentloaded');

    // Create a ChatPane scoped to the satellite page and wait for it to render.
    // The satellite re-mounts the React app from scratch, so the message DOM
    // appears a beat after domcontentloaded.
    const satelliteChatPane = new ChatPane(satellitePage);
    await expect(satelliteChatPane.chatRoot).toBeVisible({ timeout: 15000 });

    // Verify conversation continuity: same message count
    await expect.poll(() => satelliteChatPane.getMessageCount(), { timeout: 10000 })
      .toBe(messagesBeforeDetach);

    // Verify the original response is still there
    const satelliteLastMessage = satelliteChatPane.messageItem.last();
    await expect(satelliteLastMessage).toContainText('Loki', { timeout: 10000 });

    // --- Phase 3: Send another message in the satellite ---
    const satelliteChatActions = new ChatPaneActions(satellitePage, consoleActions);

    const countBeforeSecond = await satelliteChatPane.getMessageCount();
    await satelliteChatActions.sendChatMessage('Who is the Norse god of thunder? Answer in one word.');
    const countAfterSecond = await satelliteChatActions.waitForResponse(countBeforeSecond);
    expect(countAfterSecond).toBeGreaterThan(countBeforeSecond);

    const secondResponse = satelliteChatPane.messageItem.last();
    await expect(secondResponse).toContainText('Thor', { timeout: 10000 });

    const messagesBeforeReturn = await satelliteChatPane.getMessageCount();

    // --- Phase 4: Return to main window ---
    await satellitePage.locator(RETURN_TO_MAIN_BTN).click();

    // Wait for the main-window chat pane to absorb the satellite's messages.
    await expect.poll(() => chatPane.getMessageCount(), { timeout: 15000 })
      .toBe(messagesBeforeReturn);
    const mainMessageCount = messagesBeforeReturn;

    // Verify both responses are present
    const allMessages = chatPane.messageItem;
    const allTexts: string[] = [];
    for (let i = 0; i < mainMessageCount; i++) {
      allTexts.push(await allMessages.nth(i).innerText());
    }
    const combinedText = allTexts.join(' ');

    expect(combinedText).toContain('Loki');
    expect(combinedText).toContain('Thor');
  });
});

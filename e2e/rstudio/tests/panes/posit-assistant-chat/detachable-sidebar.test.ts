import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import type { Page } from 'playwright';

// Return button in satellite window (has explicit ElementIds assignment)
const RETURN_TO_MAIN_BTN = '#rstudio_chat_return_to_main_button';

test.describe.serial('Detachable Assistant Sidebar - #16937', () => {
  test.skip(process.env.RSTUDIO_EDITION === 'server', 'Satellite windows are Desktop-only — Server behavior TBD');

  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let consoleActions: ConsolePaneActions;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    const assistantActions = new AssistantOptionsActions(page, consoleActions);
    chatActions = new ChatPaneActions(page, consoleActions);
    chatPane = chatActions.chatPane;

    versions = await consoleActions.getEnvironmentVersions();
    await consoleActions.clearConsole();

    await assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);

    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();
  });

  test.beforeEach(async () => {
    test.info().annotations.push(
      { type: 'R version', description: versions.r },
      { type: 'RStudio version', description: versions.rstudio },
    );
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
    console.log(`Phase 1 complete — ${countAfterFirst} messages in main window`);

    // Record message count before detach
    const messagesBeforeDetach = await chatPane.getMessageCount();

    // --- Phase 2: Pop out to satellite window ---
    const context = page.context();

    // Listen for the new page (satellite window) before triggering pop-out
    const satellitePromise = context.waitForEvent('page', { timeout: 30000 });
    await page.locator("[id^='rstudio_tb_popoutchat']").click();
    const satellitePage = await satellitePromise;
    await satellitePage.waitForLoadState('domcontentloaded');
    await sleep(3000);

    console.log(`Satellite window opened — title: "${await satellitePage.title()}"`);

    // Create a ChatPane scoped to the satellite page
    const satelliteChatPane = new ChatPane(satellitePage);

    // Verify conversation continuity: same message count
    const satelliteMessageCount = await satelliteChatPane.getMessageCount();
    expect(satelliteMessageCount).toBe(messagesBeforeDetach);
    console.log(`Phase 2 — satellite has ${satelliteMessageCount} messages (expected ${messagesBeforeDetach})`);

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
    console.log(`Phase 3 complete — ${countAfterSecond} messages in satellite`);

    const messagesBeforeReturn = await satelliteChatPane.getMessageCount();

    // --- Phase 4: Return to main window ---
    await satellitePage.locator(RETURN_TO_MAIN_BTN).click();
    await sleep(3000);

    // Verify all messages persist in the main window
    const mainMessageCount = await chatPane.getMessageCount();
    expect(mainMessageCount).toBe(messagesBeforeReturn);
    console.log(`Phase 4 — main window has ${mainMessageCount} messages (expected ${messagesBeforeReturn})`);

    // Verify both responses are present
    const allMessages = chatPane.messageItem;
    const allTexts: string[] = [];
    for (let i = 0; i < mainMessageCount; i++) {
      allTexts.push(await allMessages.nth(i).innerText());
    }
    const combinedText = allTexts.join(' ');

    expect(combinedText).toContain('Loki');
    expect(combinedText).toContain('Thor');
    console.log('Conversation continuity verified across detach/reattach cycle');
  });
});

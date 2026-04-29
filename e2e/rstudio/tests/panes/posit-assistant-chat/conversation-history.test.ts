import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';

test.describe.serial('Conversation History', () => {
  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
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

  test('conversation history preserves and restores conversations', async ({ rstudioPage: page }) => {
    // Use unique names with timestamp to avoid colliding with old test data
    const timestamp = Date.now();
    const athenaName = `Athena-${timestamp}`;
    const aphroditeName = `Aphrodite-${timestamp}`;
    const artemisName = `Artemis-${timestamp}`;

    // Create conversation Athena (Greek goddess of wisdom)
    await chatActions.startNewConversation();
    let count = await chatPane.getMessageCount();
    await chatActions.sendChatMessage('Who is the Greek goddess of wisdom?');
    await chatActions.waitForResponse(count);
    await chatActions.renameConversation(athenaName);

    // Create conversation Aphrodite (Greek goddess of love)
    await chatActions.startNewConversation();
    count = await chatPane.getMessageCount();
    await chatActions.sendChatMessage('Who is the Greek goddess of love and beauty?');
    await chatActions.waitForResponse(count);
    await chatActions.renameConversation(aphroditeName);

    // Create conversation Artemis (Greek goddess of the hunt)
    await chatActions.startNewConversation();
    count = await chatPane.getMessageCount();
    await chatActions.sendChatMessage('Who is the Greek goddess of the hunt?');
    await chatActions.waitForResponse(count);
    await chatActions.renameConversation(artemisName);

    // Close and reopen history panel to refresh the list after all the renames
    await chatActions.toggleConversationHistory();
    await chatActions.toggleConversationHistory();

    // Select Athena and verify its content is restored
    await chatPane.getConversationItemByName(athenaName).first().click();
    await chatActions.toggleConversationHistory();
    const athenaMessages = chatPane.frame.locator('[data-message-id]');
    await expect(athenaMessages.last()).toContainText('Athena', { timeout: 5000 });

    // Open history again for Aphrodite
    await chatActions.toggleConversationHistory();
    await chatPane.getConversationItemByName(aphroditeName).first().click();
    await chatActions.toggleConversationHistory();
    const aphroditeMessages = chatPane.frame.locator('[data-message-id]');
    await expect(aphroditeMessages.last()).toContainText('Aphrodite', { timeout: 5000 });

    // Open history and delete Artemis conversation
    await chatActions.toggleConversationHistory();

    // Find Artemis in the history, hover to reveal menu button
    const artemisItem = chatPane.getConversationItemByName(artemisName).first();
    await artemisItem.hover();

    // Click the three-dot menu button on Artemis
    const artemisMenuBtn = chatPane.getConversationMenuButtonByName(artemisName);
    await artemisMenuBtn.click();

    // Click Delete from the context menu
    await chatPane.getDeleteMenuItem().click();

    // Confirm deletion by clicking the red "Delete this conversation" button
    const deleteButton = chatPane.getDeleteConfirmButton();
    await expect(deleteButton).toBeVisible({ timeout: 5000 });
    await deleteButton.click();

    // Verify Artemis is gone from history
    await chatActions.toggleConversationHistory();
    await chatActions.toggleConversationHistory();
    await expect(chatPane.getConversationItemByName(artemisName)).not.toBeVisible({ timeout: 5000 });

    // Select Athena conversation, then press Escape to dismiss history
    await chatPane.getConversationItemByName(athenaName).first().click();
    await page.keyboard.press('Escape');

    // Verify history panel is closed
    await expect(chatPane.conversationList.first()).not.toBeVisible({ timeout: 5000 });
  });
});

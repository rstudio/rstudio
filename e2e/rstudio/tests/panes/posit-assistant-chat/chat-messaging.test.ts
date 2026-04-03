import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';

test.describe.serial('Chat Messaging', () => {
  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let versions: EnvironmentVersions;
  let positAssistantVersion: string;

  test.beforeAll(async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    const assistantActions = new AssistantOptionsActions(page, consoleActions);
    chatActions = new ChatPaneActions(page, consoleActions);
    chatPane = chatActions.chatPane;

    versions = await consoleActions.getEnvironmentVersions();
    console.log(`R: ${versions.r}, RStudio: ${versions.rstudio}`);
    await consoleActions.clearConsole();

    await assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);

    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();
    positAssistantVersion = 'unknown';
  });

  test.beforeEach(async () => {
    test.info().annotations.push(
      { type: 'R version', description: versions.r },
      { type: 'RStudio version', description: versions.rstudio },
      { type: 'Posit Assistant version', description: positAssistantVersion },
    );
  });

  test('chat app loads with root element', async ({ rstudioPage: page }) => {
    await expect(chatPane.chatRoot).toBeVisible({ timeout: 30000 });
  });

  test('send a message and receive a response', async ({ rstudioPage: page }) => {
    const initialCount = await chatPane.getMessageCount();
    await chatActions.sendChatMessage('Hello, can you help me?');

    const newCount = await chatActions.waitForResponse(initialCount);
    expect(newCount).toBeGreaterThan(initialCount);

    // Verify the response message has content
    const lastMessage = chatPane.messageItem.last();
    await expect(lastMessage).not.toBeEmpty();
  });

  test('multi-turn conversation', async ({ rstudioPage: page }) => {
    const turns = [
      'Let\'s have fun with math!',
      'Set the variable x to 1',
      'Add 22 to x',
      'What is the value of x?',
    ];

    for (const message of turns) {
      const countBefore = await chatPane.getMessageCount();
      await chatActions.sendChatMessage(message);
      const countAfter = await chatActions.waitForResponse(countBefore);
      expect(countAfter).toBeGreaterThan(countBefore);
      console.log(`Turn "${message}" — messages: ${countBefore} → ${countAfter}`);
    }

    // Verify the assistant tracked context: x = 1, x + 22 = 23
    let lastMessage = chatPane.messageItem.last();
    await expect(lastMessage).toContainText('23', { timeout: 5000 });

    // Verify the assistant can answer a non-math question
    const countBefore = await chatPane.getMessageCount();
    await chatActions.sendChatMessage('To whom is this referring? Answer in one word: "until Great Birnam Wood to high Dunsinane Hill shall come against him"');
    await chatActions.waitForResponse(countBefore);

    lastMessage = chatPane.messageItem.last();
    await expect(lastMessage).toContainText('Macbeth', { timeout: 5000 });

    // Verify new conversation resets the chat
    await chatActions.startNewConversation();
    const resetCount = await chatPane.getMessageCount();
    expect(resetCount).toBe(0);
    console.log(`New conversation reset — messages: ${resetCount}`);

    // Verify the new conversation works
    const newConvCount = await chatPane.getMessageCount();
    await chatActions.sendChatMessage('Who believes that nothing will come of nothing: speak again?');
    await chatActions.waitForResponse(newConvCount);

    lastMessage = chatPane.messageItem.last();
    await expect(lastMessage).toContainText('Lear', { timeout: 5000 });
    await expect(lastMessage).not.toContainText('Macbeth');
  });

});

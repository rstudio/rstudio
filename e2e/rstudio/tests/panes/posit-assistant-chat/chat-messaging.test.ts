import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import { setupPositAssistantChat, annotateVersions } from './_chat-setup';

test.describe.serial('Chat Messaging', { tag: ['@ai', '@chat'] }, () => {
  requireAiCredentials(test, 'positai');

  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let versions: EnvironmentVersions;
  let positAssistantVersion: string;

  test.beforeAll(async ({ rstudioPage: page }) => {
    ({ chatActions, chatPane, versions } = await setupPositAssistantChat(page));
    positAssistantVersion = 'unknown';
  });

  test.beforeEach(async () => {
    // Every test here is one or more full model round-trips, and the
    // multi-turn case is six of them back to back. The 120s default budget
    // leaves ~20s per turn, so a single slow turn expires the test and
    // reports an opaque "Test timeout exceeded" instead of the assertion
    // that was actually waiting.
    test.setTimeout(300000);

    annotateVersions(versions);
    test.info().annotations.push(
      { type: 'Posit Assistant version', description: positAssistantVersion },
    );
  });

  test('chat app loads with root element', async ({ rstudioPage: page }) => {
    await expect(chatPane.chatRoot).toBeVisible({ timeout: 30000 });
  });

  test('send a message and receive a response', async ({ rstudioPage: page }) => {
    const initialCount = await chatActions.sendChatMessage('Hello, can you help me?');

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
      // The baseline comes from sendChatMessage, which samples it once the
      // previous turn has actually settled. Sampling it here instead would
      // read a count the previous turn can still grow during that wait, and
      // credit this turn with the growth.
      const countBefore = await chatActions.sendChatMessage(message);
      const countAfter = await chatActions.waitForResponse(countBefore);
      expect(countAfter).toBeGreaterThan(countBefore);
    }

    // Verify the assistant tracked context: x = 1, x + 22 = 23
    let lastMessage = chatPane.messageItem.last();
    await expect(lastMessage).toContainText('23', { timeout: 5000 });

    // Verify the assistant can answer a non-math question
    const countBefore = await chatActions.sendChatMessage('To whom is this referring? Answer in one word: "until Great Birnam Wood to high Dunsinane Hill shall come against him"');
    await chatActions.waitForResponse(countBefore);

    lastMessage = chatPane.messageItem.last();
    await expect(lastMessage).toContainText('Macbeth', { timeout: 5000 });

    // Verify new conversation resets the chat
    await chatActions.startNewConversation();
    const resetCount = await chatPane.getMessageCount();
    expect(resetCount).toBe(0);

    // Verify the new conversation works
    const newConvCount = await chatActions.sendChatMessage('Who believes that nothing will come of nothing: speak again?');
    await chatActions.waitForResponse(newConvCount);

    lastMessage = chatPane.messageItem.last();
    await expect(lastMessage).toContainText('Lear', { timeout: 5000 });
    await expect(lastMessage).not.toContainText('Macbeth');
  });

});

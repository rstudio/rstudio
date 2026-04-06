import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';

// GitHub issues:
//   https://github.com/rstudio/rstudio/issues/17172
//   https://github.com/rstudio/rstudio/issues/16957

test.describe.serial('Readline Notification in Chat Pane', { tag: ['@serial'] }, () => {
  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let consoleActions: ConsolePaneActions;
  let versions: EnvironmentVersions;

  const PROMPT = 'Write R code that asks the user for their name and count how many letters there are in it. Use the readline command. Run the code.';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    const assistantActions = new AssistantOptionsActions(page, consoleActions);
    chatActions = new ChatPaneActions(page, consoleActions);
    chatPane = chatActions.chatPane;

    versions = await consoleActions.getEnvironmentVersions();
    console.log(`R: ${versions.r}, RStudio: ${versions.rstudio}`);
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

  test('notification appears when readline blocks, chat is unresponsive, and notification clears after input', async ({ rstudioPage: page }) => {
    // Start a fresh conversation
    await chatActions.startNewConversation();

    // Send the prompt that will trigger readline()
    const initialCount = await chatPane.getMessageCount();
    await chatActions.sendChatMessage(PROMPT);

    // Handle Allow dialogs until readline notification appears.
    // The assistant will generate and run code that calls readline(),
    // which blocks R waiting for console input.
    await chatActions.pollWithAllowDialogs(async () => {
      const visible = await chatPane.readlineNotification.isVisible().catch(() => false);
      if (visible) console.log('Readline notification appeared');
      return visible;
    });

    // --- Step 4: Assert notification is visible ---
    await expect(chatPane.readlineNotification).toBeVisible({ timeout: 5000 });
    console.log('Verified: "R is waiting for input in the Console." notification is visible');

    // --- Step 5: Type a message in chat and press Enter — nothing should happen ---
    const messageCountBefore = await chatPane.getMessageCount();
    await chatPane.chatTextarea.fill('This should not go through');
    await sleep(200);
    await chatPane.sendBtn.click({ timeout: 2000 }).catch(() => {
      // Send button may be disabled or unresponsive — that's expected
      console.log('Send button click did not go through (expected)');
    });
    await sleep(1000);

    const messageCountAfter = await chatPane.getMessageCount();
    expect(messageCountAfter).toBe(messageCountBefore);
    console.log(`Chat message count unchanged: ${messageCountBefore} → ${messageCountAfter}`);

    // Notification should still be visible
    await expect(chatPane.readlineNotification).toBeVisible();
    console.log('Verified: notification still visible after attempting to send chat message');

    // --- Step 6: Provide input in the Console to unblock readline ---
    await consoleActions.consolePane.consoleTab.click();
    await sleep(500);
    await consoleActions.consolePane.consoleInput.click({ force: true });
    await sleep(500);
    await consoleActions.consolePane.consoleInput.pressSequentially('Prospero');
    await sleep(300);
    await page.keyboard.press('Enter');
    console.log('Typed "Prospero" in console to respond to readline');

    // --- Step 7: Assert notification disappears ---
    await expect(chatPane.readlineNotification).toBeHidden({ timeout: 15000 });
    console.log('Verified: readline notification dismissed after providing console input');

    // --- Step 8: Verify chat resumes working after readline completes ---
    // Wait for the assistant to finish processing the readline response
    const readlineDeadline = Date.now() + 30000;
    while (Date.now() < readlineDeadline) {
      if (!(await chatPane.isStopButtonVisible())) break;
      await sleep(500);
    }

    // Ask the assistant to print a Tempest quote to the console
    await consoleActions.clearConsole();
    const countBeforeQuote = await chatPane.getMessageCount();
    await chatActions.sendChatMessage('Print "We are such stuff as dreams are made on" to the R console using cat().');
    await chatActions.waitForResponse(countBeforeQuote);
    console.log('Sent follow-up message to verify chat is functional');

    // Verify the quote appears in console output
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      'We are such stuff as dreams are made on', { timeout: 15000 }
    );
    console.log('Verified: Tempest quote printed to console — chat is fully functional after readline');
  });
});

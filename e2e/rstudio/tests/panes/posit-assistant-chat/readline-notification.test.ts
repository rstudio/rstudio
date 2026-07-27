import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import { setupPositAssistantChat, annotateVersions } from './_chat-setup';

// GitHub issues:
//   https://github.com/rstudio/rstudio/issues/17172
//   https://github.com/rstudio/rstudio/issues/16957

test.describe.serial('Readline Notification in Chat Pane', { tag: ['@ai', '@serial'] }, () => {
  requireAiCredentials(test, 'positai');

  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let consoleActions: ConsolePaneActions;
  let versions: EnvironmentVersions;

  const PROMPT = 'Write R code that asks the user for their name and count how many letters there are in it. Use the readline command. Run the code.';

  test.beforeAll(async ({ rstudioPage: page }) => {
    ({ chatActions, chatPane, consoleActions, versions } = await setupPositAssistantChat(page));
  });

  test.beforeEach(async () => {
    annotateVersions(versions);
  });

  test('notification appears when readline blocks, chat is unresponsive, and notification clears after input', async ({ rstudioPage: page }) => {
    // Start a fresh conversation
    await chatActions.startNewConversation();

    // Send the prompt that will trigger readline()
    const initialCount = await chatPane.getMessageCount();
    await chatActions.sendChatMessage(PROMPT);

    // Handle Allow dialogs until the readline notification appears.
    // The assistant will generate and run code that calls readline(),
    // which blocks R waiting for console input.
    //
    // The notification only appears if the assistant actually ran blocking
    // readline() code. If its executeCode tool fails (e.g. the r.getLogger
    // outage, assistant #1893), the code never runs, R never blocks, and the
    // assistant just finishes its turn with a normal reply. Detect that
    // terminal state and fail with an attributable message instead of hanging
    // until the 120s test timeout. The poll budget stays below the per-test
    // timeout so this assertion, not the test timeout, reports the failure.
    let finishedWithoutNotification = 0;
    await chatActions.pollWithAllowDialogs(async () => {
      if (await chatPane.readlineNotification.isVisible().catch(() => false)) {
        return true;
      }
      // Only conclude the assistant gave up once the turn has truly ended: it
      // produced a reply and the composer is editable again (idle, ready for
      // input). A bare missing Stop button is not a reliable terminal signal --
      // it is also absent right after sending and during tool-step transitions,
      // which would otherwise fail before the assistant even runs the code. The
      // composer stays disabled for the whole turn (including a real readline
      // block, per Step 5 below), so its being editable means the turn is done.
      const turnEnded =
        (await chatPane.assistantMessageItem.count()) > 0 &&
        (await chatPane.isChatInputReady());
      if (!turnEnded) {
        finishedWithoutNotification = 0;
        return false;
      }
      // Turn ended (composer editable) but no notification => readline never
      // blocked; the assistant likely couldn't execute the code. Confirm the
      // terminal state across one re-check before failing.
      finishedWithoutNotification += 1;
      if (finishedWithoutNotification >= 2) {
        throw new Error(
          'Assistant turn finished without blocking on readline -- executeCode may ' +
          'have failed ("r.getLogger is not a function", assistant #1893). ' +
          'readline notification never appeared.',
        );
      }
      return false;
    }, 90000);

    // --- Step 4: Assert notification is visible ---
    await expect(chatPane.readlineNotification).toBeVisible({ timeout: 5000 });

    // --- Step 5: Type a message in chat and press Enter -- nothing should happen ---
    const messageCountBefore = await chatPane.getMessageCount();
    await chatPane.typeMessage('This should not go through');
    await chatPane.sendBtn.click({ timeout: 2000 }).catch(() => {
      // Send button may be disabled or unresponsive -- that's expected
    });

    // Deliberate observation window: if the bug were present, the send would
    // sneak through asynchronously. There's no readiness signal we can poll
    // for absence of an event, so we wait a beat and re-check the count.
    await page.waitForTimeout(1000);

    const messageCountAfter = await chatPane.getMessageCount();
    expect(messageCountAfter).toBe(messageCountBefore);

    // Notification should still be visible
    await expect(chatPane.readlineNotification).toBeVisible();

    // --- Step 6: Provide input in the Console to unblock readline ---
    await consoleActions.typeInConsole('Prospero');
    // Wait for the typed text to actually appear in the console input before
    // pressing Enter -- typeInConsole's per-keystroke delay can race with the
    // Enter dispatch if the editor hasn't fully consumed the keys yet.
    await expect.poll(() => consoleActions.consolePane.consoleInputValue())
      .toBe('Prospero');
    await page.keyboard.press('Enter');

    // --- Step 7: Assert notification disappears ---
    await expect(chatPane.readlineNotification).toBeHidden({ timeout: 15000 });

    // --- Step 8: Verify chat resumes working after readline completes ---
    // Wait for the assistant to finish processing the readline response.
    await expect.poll(() => chatPane.isStopButtonVisible(), { timeout: 30000 })
      .toBe(false);

    // Ask the assistant to print a Tempest quote to the console
    await consoleActions.clearConsole();
    const countBeforeQuote = await chatPane.getMessageCount();
    await chatActions.sendChatMessage('Print "We are such stuff as dreams are made on" to the R console using cat().');
    await chatActions.waitForResponse(countBeforeQuote);

    // Verify the quote appears in console output
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      'We are such stuff as dreams are made on', { timeout: 15000 }
    );
  });
});

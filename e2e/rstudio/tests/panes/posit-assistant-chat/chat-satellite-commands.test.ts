import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { executeCommand } from '@utils/commands';
import { createChatActions } from './_chat-setup';

const CHAT_IFRAME = "iframe[title='Posit Assistant']";
const RETURN_TO_MAIN_BTN = '#rstudio_chat_return_to_main_button';

// Satellite pop-out on Server hits the Chrome window.open() reload path,
// which is a separate concern from the command wiring this test covers.
// detachable-sidebar.test.ts is @desktop_only for the same reason.
test.describe.serial('Chat satellite -- commands', { tag: ['@ai', '@desktop_only'] }, () => {
  requireAiCredentials(test, 'positai');

  let consoleActions: ConsolePaneActions;
  let chatActions: ChatPaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    const actions = createChatActions(page);
    consoleActions = actions.consoleActions;
    chatActions = actions.chatActions;

    await consoleActions.clearConsole();
    await actions.assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);

    await chatActions.openChatPane();
    // Wait for iframe; don't dismiss setup prompts -- command wiring works
    // regardless of whether PAI is installed.
    await expect(page.locator(CHAT_IFRAME)).toBeVisible({ timeout: 15000 });
  });

  test('popOutChat / returnChatToMain commands open and close the satellite', async ({
    rstudioPage: page,
  }) => {
    const context = page.context();

    // Pop out the chat pane via the command.
    const satellitePromise = context.waitForEvent('page', { timeout: 30000 });
    await executeCommand(page, 'popOutChat');
    const satellitePage = await satellitePromise;
    await satellitePage.waitForLoadState('domcontentloaded');

    // Verify the satellite carries the return-to-main button. The 15s
    // expect-visible covers the chat React app's mount time.
    await expect(satellitePage.locator(RETURN_TO_MAIN_BTN)).toBeVisible({
      timeout: 15000,
    });

    // Return chat to the main window via the command.
    const closePromise = satellitePage.waitForEvent('close', { timeout: 30000 });
    await executeCommand(page, 'returnChatToMain');
    await closePromise;

    // After return, the iframe should be back in the main window.
    await expect(page.locator(CHAT_IFRAME)).toBeVisible({ timeout: 15000 });
  });
});

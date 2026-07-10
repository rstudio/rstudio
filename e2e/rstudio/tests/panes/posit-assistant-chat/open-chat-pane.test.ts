import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import { executeCommand } from '@utils/commands';
import { createChatActions, annotateVersions } from './_chat-setup';

test.describe('Open Chat Pane', { tag: ['@ai'] }, () => {
  requireAiCredentials(test, 'positai');

  let consoleActions: ConsolePaneActions;
  let chatActions: ChatPaneActions;
  let chatPane: ChatPane;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    const actions = createChatActions(page);
    consoleActions = actions.consoleActions;
    chatActions = actions.chatActions;
    chatPane = actions.chatPane;

    versions = await consoleActions.getEnvironmentVersions();
    await consoleActions.clearConsole();
    await actions.assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);
  });

  test.beforeEach(async () => {
    annotateVersions(versions);
  });

  test('open chat pane with keyboard shortcut', { tag: ['@macos_only'] }, async ({ rstudioPage: page }) => {

    const chatIframe = page.locator("iframe[title='Posit Assistant']");

    // Close the sidebar if it's open to ensure the chat pane is not visible
    if (await chatIframe.isVisible().catch(() => false)) {
      await executeCommand(page, 'toggleSidebar');
    }

    // Verify the chat iframe is not visible (expect auto-waits for the toggle).
    await expect(chatIframe).not.toBeVisible();

    // Open chat pane via Ctrl+Cmd+I (macOS shortcut)
    await page.keyboard.press('Control+Meta+I');

    // Verify the chat iframe appeared
    await expect(page.locator("iframe[title='Posit Assistant']")).toBeVisible({ timeout: 15000 });
  });

  test('open chat pane with activateChat command', async ({ rstudioPage: page }) => {
    const chatIframe = page.locator("iframe[title='Posit Assistant']");

    // Close the sidebar if it's open to ensure the chat pane is not visible
    if (await chatIframe.isVisible().catch(() => false)) {
      await executeCommand(page, 'toggleSidebar');
    }

    // Verify the chat iframe is not visible (expect auto-waits for the toggle).
    await expect(chatIframe).not.toBeVisible();

    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();

    // Verify the chat app root is visible inside the iframe
    await expect(chatPane.chatRoot).toBeVisible({ timeout: 30000 });
  });
});

import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';

test.describe('Open Chat Pane', () => {
  let consoleActions: ConsolePaneActions;
  let chatActions: ChatPaneActions;
  let chatPane: ChatPane;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    const assistantActions = new AssistantOptionsActions(page, consoleActions);
    chatActions = new ChatPaneActions(page, consoleActions);
    chatPane = chatActions.chatPane;

    versions = await consoleActions.getEnvironmentVersions();
    console.log(`R: ${versions.r}, RStudio: ${versions.rstudio}`);
    await consoleActions.clearConsole();

    await assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);
  });

  test.beforeEach(async () => {
    test.info().annotations.push(
      { type: 'R version', description: versions.r },
      { type: 'RStudio version', description: versions.rstudio },
    );
  });

  test('open chat pane with keyboard shortcut', { tag: ['@macos_only'] }, async ({ rstudioPage: page }) => {

    const chatIframe = page.locator("iframe[title='Posit Assistant']");

    // Close the sidebar if it's open to ensure the chat pane is not visible
    if (await chatIframe.isVisible().catch(() => false)) {
      await consoleActions.typeInConsole(".rs.api.executeCommand('toggleSidebar')");
      await sleep(2000);
    }

    // Verify the chat iframe is not visible
    await expect(chatIframe).not.toBeVisible();

    // Open chat pane via Ctrl+Cmd+I (macOS shortcut)
    await page.keyboard.press('Control+Meta+I');
    await sleep(2000);

    // Verify the chat iframe appeared
    await expect(page.locator("iframe[title='Posit Assistant']")).toBeVisible({ timeout: 15000 });
  });

  test('open chat pane with activateChat command', async ({ rstudioPage: page }) => {
    const chatIframe = page.locator("iframe[title='Posit Assistant']");

    // Close the sidebar if it's open to ensure the chat pane is not visible
    if (await chatIframe.isVisible().catch(() => false)) {
      await consoleActions.typeInConsole(".rs.api.executeCommand('toggleSidebar')");
      await sleep(2000);
    }

    // Verify the chat iframe is not visible
    await expect(chatIframe).not.toBeVisible();

    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();

    // Verify the chat app root is visible inside the iframe
    await expect(chatPane.chatRoot).toBeVisible({ timeout: 30000 });
  });
});

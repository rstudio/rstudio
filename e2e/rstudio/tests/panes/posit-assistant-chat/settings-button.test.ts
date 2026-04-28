import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';

test.describe.serial('Settings Button', () => {
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
    await expect(chatPane.chatRoot).toBeVisible({ timeout: 30000 });
  });

  test.beforeEach(async () => {
    test.info().annotations.push(
      { type: 'R version', description: versions.r },
      { type: 'RStudio version', description: versions.rstudio },
    );
  });

  test('settings menu opens with expected items and About dialog', async ({ rstudioPage: page }) => {
    // Verify settings button is visible
    await expect(chatPane.settingsBtn).toBeVisible({ timeout: 10000 });

    // Open the dropdown menu
    await chatPane.settingsBtn.click();
    await sleep(500);

    await expect(chatPane.settingsMenu).toBeVisible({ timeout: 5000 });

    // Verify expected menu items
    await expect(chatPane.configurePositAiItem.first()).toBeVisible({ timeout: 5000 });
    await expect(chatPane.aboutItem.first()).toBeVisible({ timeout: 5000 });

    // Click About and verify dialog appears
    await chatPane.aboutItem.first().click();
    await sleep(1000);

    const aboutDialog = chatPane.frame.locator('[role="dialog"]');
    await expect(aboutDialog).toBeVisible({ timeout: 10000 });

    // Click "Copy to Clipboard" and verify it changes to "Copied"
    const copyBtn = aboutDialog.locator('button:has-text("Copy to Clipboard")');
    await expect(copyBtn).toBeVisible({ timeout: 5000 });
    await copyBtn.click();
    await expect(aboutDialog.locator('button:has-text("Copied")')).toBeVisible({ timeout: 3000 });

    // Close the dialog
    const closeBtn = aboutDialog.locator('button:has-text("Close")').first();
    await closeBtn.click();
    await sleep(500);
  });
});

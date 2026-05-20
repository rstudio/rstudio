import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import { setupPositAssistantChat, annotateVersions } from './_chat-setup';

test.describe.serial('Settings Button', { tag: ['@ai'] }, () => {
  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    ({ chatActions, chatPane, versions } = await setupPositAssistantChat(page));
    await expect(chatPane.chatRoot).toBeVisible({ timeout: 30000 });
  });

  test.beforeEach(async () => {
    annotateVersions(versions);
  });

  test('settings menu opens with expected items and About dialog', async ({ rstudioPage: page }) => {
    // Verify settings button is visible
    await expect(chatPane.moreBtn).toBeVisible({ timeout: 10000 });

    // Open the dropdown menu
    await chatPane.moreBtn.click();
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

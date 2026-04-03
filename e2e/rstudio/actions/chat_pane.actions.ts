import type { Page } from 'playwright';
import { expect } from '@playwright/test';
import { ChatPane } from '../pages/chat_pane.page';
import { ConsolePaneActions } from './console_pane.actions';
import { sleep } from '../utils/constants';

export class ChatPaneActions {
  readonly page: Page;
  readonly chatPane: ChatPane;
  readonly consolePaneActions: ConsolePaneActions;

  constructor(page: Page, consolePaneActions: ConsolePaneActions) {
    this.page = page;
    this.chatPane = new ChatPane(page);
    this.consolePaneActions = consolePaneActions;
  }

  async openChatPane(): Promise<void> {
    await this.consolePaneActions.typeInConsole(".rs.api.executeCommand('activateChat')");
    await sleep(2000);
  }

  async dismissSetupPrompts(): Promise<void> {
    // Short timeouts: if these prompts exist, they're visible almost immediately.
    // Using 1500ms instead of 5000ms saves ~10s when no prompts are present.
    try {
      await this.chatPane.installBtn.click({ timeout: 1500 });
      console.log('Clicked Install button — waiting for installation...');
      await expect(this.chatPane.chatRoot).toBeVisible({ timeout: 60000 });
      return;
    } catch {
      // No Install button
    }

    try {
      await this.chatPane.updateBtn.click({ timeout: 1500 });
      console.log('Clicked Update on update prompt — updating Posit AI');
      await expect(this.chatPane.chatRoot).toBeVisible({ timeout: 30000 });
      return;
    } catch {
      // No update prompt, or pane is stale after update via Options
    }

    // Dismiss "Trust this directory?" prompt if present
    try {
      const trustBtn = this.chatPane.frame.locator("button:has-text('Trust'), button:has-text('trust')");
      await trustBtn.first().click({ timeout: 1500 });
      console.log('Dismissed directory trust prompt');
      await sleep(1000);
    } catch {
      // No trust prompt
    }

  }

  async clickAllowOnceIfPresent(): Promise<void> {
    try {
      await sleep(1000);
      await this.chatPane.allowBtn.click({ timeout: 3000 });
      console.log('Clicked "Allow" permission button');
      await sleep(1000);
    } catch {
      // No permission dialog
    }
  }

  async allowExecuteCodeForSession(): Promise<void> {
    // Wait for the Allow dialog to appear (dropdown trigger next to Allow button)
    await expect(this.chatPane.allowDropdownTrigger).toBeVisible({ timeout: 60000 });
    await sleep(500);

    // Click the chevron to open the dropdown menu
    await this.chatPane.allowDropdownTrigger.click();
    await sleep(500);

    // Click "Allow for this session"
    await expect(this.chatPane.allowForSessionItem).toBeVisible({ timeout: 5000 });
    await this.chatPane.allowForSessionItem.click();
    console.log('Granted "Allow for this session" permission');
    await sleep(1000);
  }

  /**
   * Poll in a loop, handling Allow dialogs (session-level then fallback),
   * until the provided condition returns true or the timeout expires.
   */
  async pollWithAllowDialogs(
    isDone: () => Promise<boolean>,
    timeout: number = 120000
  ): Promise<void> {
    const deadline = Date.now() + timeout;

    while (Date.now() < deadline) {
      // Grant session-level permission if the dropdown is available
      if (await this.chatPane.isAllowDropdownVisible()) {
        await sleep(500);
        await this.chatPane.allowDropdownTrigger.click();
        await sleep(500);
        try {
          await this.chatPane.allowForSessionItem.click({ timeout: 5000 });
          console.log('Granted "Allow for this session" permission');
          await sleep(1000);
          continue;
        } catch {
          console.log('Failed to click session permission menu item');
        }
      }

      // Fallback: click Allow once
      if (await this.chatPane.isAllowButtonVisible()) {
        await sleep(500);
        await this.chatPane.allowBtn.click();
        console.log('Clicked "Allow once" as fallback');
        await sleep(1000);
        continue;
      }

      // Check caller's condition
      if (await isDone()) {
        return;
      }

      await sleep(1000);
    }

    throw new Error(`pollWithAllowDialogs timed out after ${timeout}ms`);
  }

  async sendChatMessage(text: string): Promise<void> {
    await expect(this.chatPane.chatTextarea).toBeVisible({ timeout: 15000 });
    await expect(this.chatPane.chatTextarea).toBeEnabled({ timeout: 15000 });
    await this.chatPane.chatTextarea.fill(text);
    await sleep(200);

    await expect(this.chatPane.sendBtn).toBeVisible({ timeout: 15000 });
    await this.chatPane.sendBtn.click();
    await sleep(1000);
  }

  async waitForResponse(initialCount: number, timeout: number = 60000): Promise<number> {
    const deadline = Date.now() + timeout;

    while (Date.now() < deadline) {
      if (await this.chatPane.isAllowButtonVisible()) {
        await sleep(1000);
        await this.chatPane.allowBtn.click();
        console.log('Clicked "Allow once" during response wait');
        await sleep(1000);
      }

      const currentCount = await this.chatPane.getMessageCount();
      if (currentCount > initialCount) {
        const readyDeadline = Date.now() + 30000;
        while (Date.now() < readyDeadline) {
          if (await this.chatPane.isAllowButtonVisible()) {
            await sleep(1000);
            await this.chatPane.allowBtn.click();
            console.log('Clicked "Allow" after response arrived');
            await sleep(1000);
            continue;
          }

          if (!(await this.chatPane.isStopButtonVisible())) {
            break;
          }

          await sleep(500);
        }

        const finalCount = await this.chatPane.getMessageCount();
        console.log(`Message count: ${initialCount} → ${finalCount}`);
        return finalCount;
      }

      await sleep(500);
    }

    const finalCount = await this.chatPane.getMessageCount();
    if (finalCount > initialCount) {
      console.log(`Message count: ${initialCount} → ${finalCount}`);
      return finalCount;
    }

    throw new Error(`Timed out waiting for response. Message count stuck at ${finalCount} (expected > ${initialCount})`);
  }

  async getPositAssistantVersion(): Promise<string> {
    try {
      await this.chatPane.settingsBtn.click({ timeout: 5000 });
      await sleep(500);

      await this.chatPane.aboutItem.first().click();
      await sleep(1000);

      await this.chatPane.dialogOverlay.waitFor({ state: 'visible', timeout: 5000 });
      const dialogText = await this.chatPane.dialogOverlay.innerText();

      await this.page.keyboard.press('Escape');
      await sleep(500);

      const versionMatch = dialogText.match(/(\d+\.\d+\.\d+)/);
      return versionMatch?.[1] ?? 'unknown';
    } catch {
      return 'unknown';
    }
  }

  async startNewConversation(): Promise<void> {
    const messageCount = await this.chatPane.getMessageCount();
    if (messageCount === 0) {
      return; // Already in a fresh conversation
    }
    await this.chatPane.newConversationBtn.click({ timeout: 10000 });
    await sleep(2000);
  }

  /**
   * Rename the current conversation via the UI context menu
   * @param name The new name for the conversation
   */
  async renameConversation(name: string): Promise<void> {
    // Open history panel to access conversation list
    await this.toggleConversationHistory();
    await sleep(1000);

    // Get the first (current/active) conversation item
    const activeConvItem = this.chatPane.conversationList.first();
    await expect(activeConvItem).toBeVisible({ timeout: 5000 });

    // Hover over the conversation item to reveal the menu button
    await activeConvItem.hover();
    await sleep(500);

    // Look for the menu button (three dots) within the conversation item
    const menuBtn = activeConvItem.locator('button, [role="button"]').first();
    await expect(menuBtn).toBeVisible({ timeout: 5000 });
    // force: true because GWT console DOM elements report overlapping coordinates
    // even though the panes are visually separate
    await menuBtn.click({ force: true });
    await sleep(300);

    // Click the Rename option from the context menu
    const renameItem = this.chatPane.getRenameMenuItem();
    await expect(renameItem).toBeVisible({ timeout: 5000 });
    await renameItem.click();
    await sleep(300);

    // The input field should now be visible and editable
    const nameInput = this.chatPane.getConversationNameInput();
    await expect(nameInput).toBeVisible({ timeout: 5000 });
    await expect(nameInput).toBeFocused();

    // Clear the current name and type the new name
    await nameInput.clear();
    await nameInput.fill(name);
    await sleep(200);

    // Press Enter to confirm the rename
    await nameInput.press('Enter');
    await sleep(500);

    console.log(`Renamed conversation to "${name}"`);
  }

  async toggleConversationHistory(): Promise<void> {
    await this.chatPane.historyBtn.click({ timeout: 10000 });
    await sleep(1000);
  }
}

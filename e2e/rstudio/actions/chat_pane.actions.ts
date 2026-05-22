import type { Page, Route } from 'playwright';
import { expect } from '@playwright/test';
import { ChatPane } from '../pages/chat_pane.page';
import { ConsolePaneActions } from './console_pane.actions';
import { sleep } from '../utils/constants';
import { executeCommand } from '../utils/commands';

/** Fields returned by the chatCheckForUpdates RPC. */
export interface UpdateCheckResult {
  updateAvailable: boolean;
  isDowngrade: boolean;
  noCompatibleVersion: boolean;
  unsupportedInstalledVersion: boolean;
  unsupportedProtocol: boolean;
  manifestUnavailable: boolean;
  errorMessage: string;
  currentVersion: string;
  newVersion: string;
  downloadUrl: string;
  isInitialInstall: boolean;
}

export class ChatPaneActions {
  readonly page: Page;
  readonly chatPane: ChatPane;
  readonly consolePaneActions: ConsolePaneActions;

  constructor(page: Page, consolePaneActions: ConsolePaneActions) {
    this.page = page;
    this.chatPane = new ChatPane(page);
    this.consolePaneActions = consolePaneActions;
  }

  /** The mock result returned by the intercepted RPC. Null = no interception. */
  private updateCheckMock: UpdateCheckResult | null = null;

  /** Default (non-blocking) update check result. */
  static defaultUpdateCheckResult(): UpdateCheckResult {
    return {
      updateAvailable: false,
      isDowngrade: false,
      noCompatibleVersion: false,
      unsupportedInstalledVersion: false,
      unsupportedProtocol: false,
      manifestUnavailable: false,
      errorMessage: '',
      currentVersion: '1.0.0',
      newVersion: '',
      downloadUrl: '',
      isInitialInstall: false,
    };
  }

  /**
   * Install a Playwright route handler that intercepts `chatCheckForUpdates`
   * RPC responses and replaces the result with `this.updateCheckMock`.
   * Call once per page; subsequent calls to `setUpdateCheckMock()` swap the
   * payload without re-registering the route.
   */
  async interceptUpdateCheck(): Promise<void> {
    // Use regex so query strings don't break matching
    await this.page.route(/\/rpc\/chat_check_for_updates/, async (route: Route) => {
      if (!this.updateCheckMock) {
        console.log('[interceptUpdateCheck] no mock, passing through');
        await route.continue();
        return;
      }
      console.log('[interceptUpdateCheck] fulfilling with mock');
      // Fulfill directly with our mock -- don't call route.fetch() because
      // rsession returns empty bodies when requests overlap via CDP.
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ result: this.updateCheckMock }),
      });
    });
  }

  /** Set the mock payload for subsequent intercepted RPC calls. */
  setUpdateCheckMock(overrides: Partial<UpdateCheckResult>): void {
    this.updateCheckMock = {
      ...ChatPaneActions.defaultUpdateCheckResult(),
      ...overrides,
    };
  }

  /** Disable interception so the real backend response flows through. */
  clearUpdateCheckMock(): void {
    this.updateCheckMock = null;
  }

  async openChatPane(): Promise<void> {
    await executeCommand(this.page, 'activateChat');
    // Wait for the chat iframe to be present rather than blind-sleeping.
    // The iframe element appears as soon as the pane is activated, even
    // before its contents have loaded -- which is what dismissSetupPrompts
    // and other callers actually need to begin polling for their own
    // signals (install button, chatRoot, etc.).
    await expect(this.page.locator("iframe[title='Posit Assistant']"))
      .toBeVisible({ timeout: 15000 });
  }

  async dismissSetupPrompts(): Promise<void> {
    // Short timeouts: if these prompts exist, they're visible almost immediately.
    // Using 1500ms instead of 5000ms saves ~10s when no prompts are present.
    try {
      await this.chatPane.installBtn.click({ timeout: 1500 });
      console.log('Clicked Install button — waiting for installation...');
      await expect(this.chatPane.chatRoot).toBeVisible({ timeout: 60000 });
      await expect(this.chatPane.chatTextarea).toBeVisible({ timeout: 30000 });
      return;
    } catch {
      // No Install button
    }

    try {
      await this.chatPane.updateBtn.click({ timeout: 1500 });
      console.log('Clicked Update on update prompt — updating Posit Assistant');
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
    } catch {
      // No trust prompt
    }

  }

  async clickAllowOnceIfPresent(): Promise<void> {
    try {
      // click() already polls for actionability within its own timeout, so
      // there's no need to pre-sleep waiting for the dialog to appear.
      await this.chatPane.allowBtn.click({ timeout: 3000 });
      console.log('Clicked "Allow" permission button');
    } catch {
      // No permission dialog
    }
  }

  async allowExecuteCodeForSession(): Promise<void> {
    // Wait for the Allow dialog to appear (dropdown trigger next to Allow button)
    await expect(this.chatPane.allowDropdownTrigger).toBeVisible({ timeout: 60000 });

    // Click the chevron to open the dropdown menu; the toBeVisible below
    // polls for the menu item, replacing a blind post-click sleep.
    await this.chatPane.allowDropdownTrigger.click();

    // Click "Allow for this session"
    await expect(this.chatPane.allowForSessionItem).toBeVisible({ timeout: 5000 });
    await this.chatPane.allowForSessionItem.click();
    console.log('Granted "Allow for this session" permission');
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

    await expect(this.chatPane.sendBtn).toBeVisible({ timeout: 15000 });
    await this.chatPane.sendBtn.click();
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
      await this.chatPane.moreBtn.click({ timeout: 5000 });
      // .first().click() polls for the menu item, replacing the
      // previous post-click sleep.
      await this.chatPane.aboutItem.first().click();

      await this.chatPane.dialogOverlay.waitFor({ state: 'visible', timeout: 5000 });
      const dialogText = await this.chatPane.dialogOverlay.innerText();

      await this.page.keyboard.press('Escape');

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
    // Poll for the conversation to reset (message list emptied) instead
    // of blind-sleeping after the click.
    await expect.poll(() => this.chatPane.getMessageCount(), { timeout: 10000 }).toBe(0);
  }

  /**
   * Rename the current conversation via the UI context menu
   * @param name The new name for the conversation
   */
  async renameConversation(name: string): Promise<void> {
    // Open history panel to access conversation list. toggleConversationHistory
    // polls for the list to appear, so no post-call sleep is needed here.
    await this.toggleConversationHistory();

    // Get the first (current/active) conversation item. Each toBeVisible
    // below polls, replacing what used to be a chain of blind sleeps
    // between hover, click, menu-item click, and input fill.
    const activeConvItem = this.chatPane.conversationList.first();
    await expect(activeConvItem).toBeVisible({ timeout: 5000 });

    // Hover over the conversation item to reveal the menu button
    await activeConvItem.hover();

    // Look for the menu button (three dots) within the conversation item
    const menuBtn = activeConvItem.locator('button, [role="button"]').first();
    await expect(menuBtn).toBeVisible({ timeout: 5000 });
    // force: true because GWT console DOM elements report overlapping coordinates
    // even though the panes are visually separate
    await menuBtn.click({ force: true });

    // Click the Rename option from the context menu
    const renameItem = this.chatPane.getRenameMenuItem();
    await expect(renameItem).toBeVisible({ timeout: 5000 });
    await renameItem.click();

    // The input field should now be visible and editable
    const nameInput = this.chatPane.getConversationNameInput();
    await expect(nameInput).toBeVisible({ timeout: 5000 });
    await expect(nameInput).toBeFocused();

    // Clear the current name and type the new name
    await nameInput.clear();
    await nameInput.fill(name);

    // Press Enter to confirm the rename
    await nameInput.press('Enter');

    console.log(`Renamed conversation to "${name}"`);
  }

  async toggleConversationHistory(): Promise<void> {
    await this.chatPane.historyBtn.click({ timeout: 10000 });
    // Poll for the conversation list panel to appear (replaces post-click sleep).
    await expect(this.chatPane.conversationList.first()).toBeVisible({ timeout: 10000 });
  }
}

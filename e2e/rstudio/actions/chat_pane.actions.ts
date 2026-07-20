import type { Page } from 'playwright';
import { expect } from '@playwright/test';
import { ChatPane } from '../pages/chat_pane.page';
import { ConsolePaneActions } from './console_pane.actions';
import { sleep } from '../utils/constants';
import { executeCommand } from '../utils/commands';

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
    // openChatPane() only waits for the iframe element, not its contents. The
    // setup prompts (Install / Update) and the real chat app all render
    // asynchronously after a backend install-status round-trip, so a single
    // isVisible() snapshot here races that render: when Posit Assistant is not
    // installed, the "Not Installed" view (with its Install button) has not
    // appeared yet, the snapshot reads false, and we fall through to
    // waitForChatReady() -- which then waits 30s for a composer that will never
    // exist. Poll until one terminal setup state is reached instead.
    const state = await this.waitForSetupState();

    if (state === 'install') {
      await this.chatPane.installBtn.click();
      console.log('Clicked Install button -- waiting for installation...');
      await expect(this.chatPane.chatRoot).toBeVisible({ timeout: 60000 });
      await expect(this.chatPane.chatInput).toBeVisible({ timeout: 30000 });
    } else if (state === 'update') {
      await this.chatPane.updateBtn.click();
      console.log('Clicked Update on update prompt -- updating Posit Assistant');
      await expect(this.chatPane.chatRoot).toBeVisible({ timeout: 30000 });
    }

    // The TrustOverlay and the chat input both render asynchronously after
    // the chat app loads; waitForChatReady polls for either path through the
    // workspace-trust dialog or a clean editable composer, so callers don't
    // need to handle it themselves.
    await this.waitForChatReady();
  }

  /**
   * Poll the chat iframe until it settles into a terminal setup state:
   * an Install prompt ('install'), an Update prompt ('update'), or the real
   * chat app having loaded ('ready').
   *
   * The chat app mounts at #root, which the plain-HTML "Not Installed" /
   * "Update Available" views never contain, so its presence reliably means
   * installation is complete and only trust/sign-in (handled by
   * waitForChatReady) may remain. Install is checked before update because the
   * downgrade variant of the update view also carries an "Install ..." button.
   *
   * The timeout must exceed the backend manifest-fetch bound that gates which
   * view renders: chat_check_for_updates can run a manifest download for up to
   * kManifestDeadlineSeconds (currently 30s, SessionChat.cpp) before any prompt
   * appears, so a shorter window could give up just before a slow-manifest
   * Install prompt renders. We poll past that bound with margin.
   *
   * On timeout (a genuine stall past the manifest bound), returns 'ready' so
   * the caller defers to waitForChatReady, whose sign-in vs. stalled error is
   * more actionable than anything we'd raise here.
   */
  private async waitForSetupState(
    timeout: number = 40000
  ): Promise<'install' | 'update' | 'ready'> {
    const deadline = Date.now() + timeout;
    while (Date.now() < deadline) {
      if (await this.chatPane.installBtn.isVisible().catch(() => false)) {
        return 'install';
      }
      if (await this.chatPane.updateBtn.isVisible().catch(() => false)) {
        return 'update';
      }
      if (await this.chatPane.chatRoot.isVisible().catch(() => false)) {
        return 'ready';
      }
      await sleep(500);
    }
    // Not a positive signal: waitForChatReady() (which dismissSetupPrompts always
    // calls next) is the real readiness gate and throws on a genuine stall. Keep
    // that call if this code is ever refactored, or this becomes a silent failure.
    return 'ready';
  }

  /**
   * Wait until the chat pane is actually usable -- i.e., the message composer
   * is editable. Until that point, sending a message is a guaranteed failure
   * even when chatRoot is visible, so this is the only real "ready" signal.
   *
   * Polls (a) clicking through any trust dialog that appears late and (b)
   * failing fast with an actionable message if a Sign-In button shows up,
   * since credentials are provisioned by the auth.setup project (the OAuth
   * sign-in flow when POSIT_EMAIL/POSIT_PASSWORD are set, else a copy of the
   * local token store).
   * The local Posit Assistant rotates the refresh token in its credential
   * store (~/.posit/ai/auth/data.json), so copied tokens can be invalidated
   * between auth setup and test execution; surfacing that as "sign in locally
   * and re-run" is more useful than the cryptic "input not editable after 15s"
   * downstream timeout each test would otherwise hit.
   *
   * The TrustOverlay component in databot renders as a role="dialog" with the
   * primary button; the RestrictedModeBadge also has a "Trust this workspace"
   * affordance, so we explicitly target the dialog first (it's the modal
   * blocker) and fall back to any visible match.
   */
  async waitForChatReady(timeout: number = 30000): Promise<void> {
    const deadline = Date.now() + timeout;
    const overlayTrustBtn = this.chatPane.frame.locator(
      "[role='dialog'] button:has-text('Trust this workspace')"
    );

    let iter = 0;
    while (Date.now() < deadline) {
      iter += 1;

      if (await this.chatPane.isChatInputReady()) {
        if (iter > 1) console.log(`waitForChatReady: input editable after ${iter} iterations`);
        return;
      }

      if (await overlayTrustBtn.isVisible().catch(() => false)) {
        console.log('waitForChatReady: clicking trust overlay button');
        await overlayTrustBtn.click({ timeout: 5000 });
        // Let the overlay tear down before re-polling
        await sleep(500);
        continue;
      }

      const anyTrustBtn = this.chatPane.trustWorkspaceBtn.first();
      if (await anyTrustBtn.isVisible().catch(() => false)) {
        console.log('waitForChatReady: clicking fallback trust button');
        await anyTrustBtn.click({ timeout: 5000 });
        await sleep(500);
        continue;
      }

      await sleep(500);
    }

    // Deadline expired -- pick the most actionable error message.
    // The Sign-In affordance can flash briefly during backend startup while
    // credentials are still being loaded from the token store, so we only
    // treat it as a hard failure when it's still visible at the end of the
    // polling window.
    if (await this.chatPane.signInBtn.first().isVisible().catch(() => false)) {
      throw new Error(
        'Posit Assistant requires sign-in despite auth.setup provisioning credentials. ' +
        'If the token store was copied from the local machine, sign in to Posit AI locally ' +
        '(token store ~/.posit/ai/auth/data.json) and re-run; ' +
        'if POSIT_EMAIL/POSIT_PASSWORD are set for the sign-in flow, check them and the auth-setup log.'
      );
    }

    throw new Error(
      `waitForChatReady: chat input still not editable after ${timeout}ms ` +
      `(no Sign-In button, no Trust dialog). Chat pane initialization may ` +
      `have stalled.`
    );
  }

  async clickAllowOnceIfPresent(): Promise<void> {
    if (await this.chatPane.allowBtn.isVisible()) {
      await this.chatPane.allowBtn.click();
      console.log('Clicked "Allow" permission button');
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
    await expect(this.chatPane.chatInput).toBeVisible({ timeout: 15000 });
    await expect(this.chatPane.chatInput)
      .toHaveAttribute('contenteditable', 'true', { timeout: 15000 });
    await this.chatPane.typeMessage(text);

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
    if (!(await this.chatPane.moreBtn.isVisible())) {
      return 'unknown';
    }
    try {
      await this.chatPane.moreBtn.click();
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

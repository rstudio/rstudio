import type { Page } from 'playwright';
import { chromium, expect } from '@playwright/test';
import { ChatPane } from '../pages/chat_pane.page';
import { ConsolePaneActions } from './console_pane.actions';
import { sleep } from '../utils/constants';
import { executeCommand } from '../utils/commands';
import {
  buildPositVerificationUrl,
  getPositAiAccount,
  type PositAiAccount,
} from '../utils/ai-credentials';

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
    // These prompts are visible immediately when present; absence is the common
    // case. Gate each click on isVisible() (snapshot) so we don't burn the full
    // click({ timeout }) auto-wait when the button is missing.
    if (await this.chatPane.installBtn.isVisible()) {
      await this.chatPane.installBtn.click();
      console.log('Clicked Install button -- waiting for installation...');
      await expect(this.chatPane.chatRoot).toBeVisible({ timeout: 60000 });
      await expect(this.chatPane.chatInput).toBeVisible({ timeout: 30000 });
    } else if (await this.chatPane.updateBtn.isVisible()) {
      await this.chatPane.updateBtn.click();
      console.log('Clicked Update on update prompt -- updating Posit Assistant');
      await expect(this.chatPane.chatRoot).toBeVisible({ timeout: 30000 });
    }

    // The TrustOverlay and the chat input both render asynchronously after
    // the iframe loads; waitForChatReady polls for either path through the
    // workspace-trust dialog or a clean editable composer, so callers don't
    // need to handle it themselves.
    await this.waitForChatReady();
  }

  /**
   * Wait until the chat pane is actually usable -- i.e., the message composer
   * is editable. Until that point, sending a message is a guaranteed failure
   * even when chatRoot is visible, so this is the only real "ready" signal.
   *
   * Polls (a) clicking through any trust dialog that appears late, (b)
   * driving the device-code OAuth flow when a Sign-In button shows up and
   * POSIT_AI_EMAIL/POSIT_AI_PASSWORD are available, and (c) failing with an
   * actionable error if Sign-In is still visible without creds available to
   * drive it. The host's Posit Assistant rotates the refresh token in
   * ~/.posit/assistant/store, so seeded copies can be invalidated between
   * globalSetup and test execution; surfacing that as "sign in on the host or
   * provide POSIT_AI_EMAIL/POSIT_AI_PASSWORD" is more useful than the cryptic
   * "input not editable after 15s" downstream timeout each test would
   * otherwise hit.
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
    let signInAttempted = false;
    while (Date.now() < deadline) {
      iter += 1;

      // Check Sign-In first: the pre-sign-in welcome state renders the
      // tiptap composer with contenteditable=true even though a z-40 overlay
      // intercepts pointer events. isChatInputReady would otherwise return
      // true here and short-circuit the loop before sign-in could happen.
      if (await this.chatPane.signInBtn.first().isVisible().catch(() => false)) {
        if (!signInAttempted) {
          const account = getPositAiAccount();
          if (account) {
            console.log('waitForChatReady: driving device-code sign-in with env credentials');
            signInAttempted = true;
            await this.signInWith(account);
            await sleep(500);
            continue;
          }
        }
        // No creds (or sign-in already tried and didn't clear the button).
        // Let the deadline expire and the final error message handle it.
        await sleep(500);
        continue;
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

      if (await this.chatPane.isChatInputReady()) {
        if (iter > 1) console.log(`waitForChatReady: input editable after ${iter} iterations`);
        return;
      }

      await sleep(500);
    }

    // Deadline expired -- pick the most actionable error message.
    // The Sign-In affordance can flash briefly during backend startup while
    // credentials are still being loaded from ~/.posit/assistant/store, so we only
    // treat it as a hard failure when it's still visible at the end of the
    // polling window.
    if (await this.chatPane.signInBtn.first().isVisible().catch(() => false)) {
      throw new Error(
        'Posit Assistant requires sign-in but no credentials are available. ' +
        'Either sign in on the host (~/.posit/assistant) or set ' +
        'POSIT_AI_EMAIL / POSIT_AI_PASSWORD and re-run.'
      );
    }

    throw new Error(
      `waitForChatReady: chat input still not editable after ${timeout}ms ` +
      `(no Sign-In button, no Trust dialog). Chat pane initialization may ` +
      `have stalled.`
    );
  }

  /**
   * Drive the Posit device-code OAuth flow at login.posit.cloud using the
   * supplied account credentials. Extracts the user_code displayed in the
   * chat pane, launches a separate Chromium browser (necessary because
   * Electron's CDP context doesn't support Target.createTarget, so
   * page.context().newPage() throws "Not supported" -- the IDE is attached
   * to Electron over CDP and we can't add a sibling page inside it), drives
   * the form, and authorizes the device. Returns once the post-authorize
   * "Access Authorized" page is visible; the IDE-side polling will detect
   * the new token within a few seconds and the caller's waitForChatReady
   * loop will see the composer become editable.
   *
   * Selectors are pinned to login.posit.cloud's current UI -- if Posit
   * redesigns that flow, this method needs to be updated.
   */
  async signInWith(account: PositAiAccount): Promise<void> {
    const userCode = await this.extractVerificationCode();
    const url = buildPositVerificationUrl(userCode);
    console.log(`signInWith: opening posit.cloud at user_code=${userCode}`);

    const browser = await chromium.launch();
    try {
      const context = await browser.newContext();
      const popup = await context.newPage();

      await popup.goto(url, { waitUntil: 'domcontentloaded' });

      await popup.getByRole('textbox', { name: 'Email' }).fill(account.email);
      await popup.getByRole('button', { name: 'Continue' }).click();

      await popup.getByRole('textbox', { name: 'Password' }).fill(account.password);
      await popup.getByRole('button', { name: 'Log in' }).click();

      // Some posit.cloud sessions show an intermediate "Continue" step
      // (account selection / consent) before the device-authorize page.
      // Click it only if it appears.
      const intermediateContinue = popup.getByRole('button', { name: 'Continue' });
      if (await intermediateContinue.isVisible({ timeout: 5000 }).catch(() => false)) {
        await intermediateContinue.click();
      }

      await popup.getByRole('button', { name: 'Authorize' }).click();
      await expect(popup.getByRole('heading', { name: 'Access Authorized' }))
        .toBeVisible({ timeout: 15_000 });
    } finally {
      await browser.close();
    }
  }

  /**
   * Read the XXXX-XXXX device-flow user_code displayed in the chat pane.
   * The chat pane lays out the code with extra spaces between characters
   * for readability ("N V J S - V L M N"), so we collapse whitespace and
   * search specifically next to the "authentication code:" label rather
   * than the whole pane text (the pane includes sandbox paths and other
   * incidental hyphenated tokens like USER-HOME that would otherwise
   * regex-match first).
   */
  private async extractVerificationCode(): Promise<string> {
    const text = (await this.chatPane.chatRoot.textContent()) ?? '';
    // Match "authentication code" label, then capture the next visible XXXX-XXXX
    // group (allowing intervening whitespace because the IDE spaces the code
    // characters out visually).
    const labelMatch = text.match(
      /authentication\s+code\s*:?\s*([A-Z0-9](?:\s*[A-Z0-9]){3}\s*-\s*[A-Z0-9](?:\s*[A-Z0-9]){3})/i,
    );
    if (labelMatch) {
      const code = labelMatch[1].replace(/\s+/g, '').toUpperCase();
      if (/^[A-Z0-9]{4}-[A-Z0-9]{4}$/.test(code)) {
        return code;
      }
    }
    throw new Error(
      'signInWith: could not find an "authentication code: XXXX-XXXX" pattern in the chat pane. ' +
      'Either the IDE is on a different sign-in screen, or login.posit.cloud ' +
      'changed the displayed code format.'
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

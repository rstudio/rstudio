import type { Page, Locator, FrameLocator } from 'playwright';
import { FramePageObject } from './page_object_base_classes';

// ---------------------------------------------------------------------------
// Class-based page object
// ---------------------------------------------------------------------------

const CHAT_FRAME_SELECTOR = "iframe[title='Posit Assistant']";

/** Posit Assistant chat pane — lives inside an iframe, so all locators use `this.frame` (inherited from FramePageObject). */
export class ChatPane extends FramePageObject {
  // Inside the chat iframe
  public chatRoot: Locator;
  public chatInput: Locator;
  public messageItem: Locator;
  public assistantMessageItem: Locator;
  public allowBtn: Locator;
  public allowDropdownTrigger: Locator;
  public allowForSessionItem: Locator;
  public sendBtn: Locator;
  public stopBtn: Locator;
  public installBtn: Locator;
  public updateBtn: Locator;
  public ignoreBtn: Locator;
  public signInBtn: Locator;
  public trustWorkspaceBtn: Locator;
  public moreBtn: Locator;
  public settingsMenu: Locator;
  public configurePositAiItem: Locator;
  public aboutItem: Locator;
  public newConversationBtn: Locator;
  public historyBtn: Locator;
  public conversationList: Locator;

  // Blocking state elements (inside the chat iframe)
  public retryManifestBtn: Locator;
  public copyErrorBtn: Locator;
  public errorDetail: Locator;
  public updateRequiredBtn: Locator;

  // Outside the iframe (main page — GWT wrapper)
  public dialogOverlay: Locator;
  public readlineNotification: Locator;

  constructor(page: Page) {
    super(page, CHAT_FRAME_SELECTOR);

    this.chatRoot = this.frame.locator('#root');
    // PAI 0.4.6 (#1495) rebuilt the composer on TipTap/ProseMirror: the chat
    // input is now a contenteditable <div class="tiptap-input-editor">, not a
    // <textarea>. editor.setEditable() toggles its contenteditable attribute.
    this.chatInput = this.frame.locator('.tiptap-input-editor');
    this.messageItem = this.frame.locator('[data-message-id]');
    // Assistant-role bubbles only. The message wrapper carries the role via an
    // inner .chat-message-assistant / .chat-message-user class (ChatMessage.tsx),
    // so this excludes the user's own prompt bubble -- letting callers match on
    // reply content without a fragile "not the prompt text" exclusion.
    this.assistantMessageItem = this.frame.locator('[data-message-id]:has(.chat-message-assistant)');
    this.allowBtn = this.frame.locator("button:has-text('Allow')");
    this.allowDropdownTrigger = this.frame.locator('button.rounded-l-none:has(svg.lucide-chevron-down)');
    this.allowForSessionItem = this.frame.locator('[role="menuitem"]:has-text("for this session")');
    this.sendBtn = this.frame.locator("button:has(svg.lucide-arrow-up)");
    this.stopBtn = this.frame.locator("button:has(svg.lucide-square)");
    this.installBtn = this.frame.locator("button:has-text('Install')");
    this.updateBtn = this.frame.locator("button:has-text('Update')");
    this.ignoreBtn = this.frame.locator("button:has-text('Ignore')");
    this.signInBtn = this.frame.locator("button:has-text('Sign In'), button:has-text('Sign in')");
    this.trustWorkspaceBtn = this.frame.locator("button:has-text('Trust this workspace')");
    this.moreBtn = this.frame.getByRole('button', { name: 'More' });
    this.settingsMenu = this.frame.locator("[data-slot='dropdown-menu-content']");
    this.configurePositAiItem = this.frame.locator("xpath=//span[contains(text(), 'Configure Posit AI')] | //div[contains(text(), 'Configure Posit AI')] | //*[@role='menuitem'][contains(., 'Configure Posit AI')]");
    this.aboutItem = this.frame.locator("xpath=//span[contains(text(), 'About')] | //div[contains(text(), 'About')] | //*[@role='menuitem'][contains(., 'About')]");
    this.newConversationBtn = this.frame.getByRole('button', { name: 'New conversation' });
    this.historyBtn = this.frame.getByRole('button', { name: 'Conversation history' });
    this.conversationList = this.frame.locator("[class*='conversation']");

    // Blocking state elements
    this.retryManifestBtn = this.frame.locator('#retry-manifest-btn');
    this.copyErrorBtn = this.frame.locator('#copy-error-btn');
    this.errorDetail = this.frame.locator('#error-detail');
    this.updateRequiredBtn = this.frame.locator('#update-btn');

    this.dialogOverlay = page.locator("[data-slot='dialog-overlay']");
    this.readlineNotification = page.locator('text=R is waiting for input in the Console.');
  }

  async getMessageCount(): Promise<number> {
    return await this.messageItem.count();
  }

  async isStopButtonVisible(): Promise<boolean> {
    return await this.stopBtn.isVisible().catch(() => false);
  }

  async isAllowButtonVisible(): Promise<boolean> {
    return await this.allowBtn.isVisible().catch(() => false);
  }

  async isAllowDropdownVisible(): Promise<boolean> {
    return await this.allowDropdownTrigger.isVisible().catch(() => false);
  }

  /**
   * The chat composer is ready when it's visible and editable. TipTap reflects
   * its editable state via the contenteditable attribute (toggled by
   * editor.setEditable), so we check that rather than isEnabled() -- a
   * contenteditable <div> is not a form control and always reports "enabled".
   */
  async isChatInputReady(): Promise<boolean> {
    if (!(await this.chatInput.isVisible().catch(() => false))) {
      return false;
    }
    return (await this.chatInput.getAttribute('contenteditable').catch(() => null)) === 'true';
  }

  /**
   * Type into the chat composer. Playwright's fill() is unreliable on a
   * ProseMirror contenteditable (it bypasses the editor's input handling), so
   * focus the editor and dispatch real key events instead.
   */
  async typeMessage(text: string): Promise<void> {
    await this.chatInput.click();
    await this.chatInput.pressSequentially(text);
  }

  /**
   * Get the rename option from the context menu
   */
  getRenameMenuItem(): Locator {
    return this.frame.locator('xpath=//*[@role="menuitem" and contains(., "Rename")]');
  }

  /**
   * Get the editable input field for conversation name
   */
  getConversationNameInput(): Locator {
    return this.frame.locator('.conversation-list-item-panel input[type="text"]');
  }

  /**
   * Get a conversation item by name from the history panel
   */
  getConversationItemByName(name: string): Locator {
    return this.frame.locator(`.conversation-list-item-panel span.truncate:has-text("${name}")`);
  }

  /**
   * Get the menu button (three dots) for a conversation by name
   */
  getConversationMenuButtonByName(name: string): Locator {
    return this.frame.locator(`xpath=//span[contains(text(), "${name}")]/ancestor::div[contains(@class, "conversation-list-item")]//button | //span[contains(text(), "${name}")]/ancestor::div[contains(@class, "conversation-list-item")]//*[@role="button"]`).first();
  }

  /**
   * Get the delete option from the context menu
   */
  getDeleteMenuItem(): Locator {
    return this.frame.locator('xpath=//*[@role="menuitem" and contains(., "Delete")]');
  }

  /**
   * Get the delete confirmation button
   */
  getDeleteConfirmButton(): Locator {
    return this.frame.locator('button:has-text("Delete this conversation")');
  }
}



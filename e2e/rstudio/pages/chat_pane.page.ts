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
  public chatTextarea: Locator;
  public messageItem: Locator;
  public allowBtn: Locator;
  public allowDropdownTrigger: Locator;
  public allowForSessionItem: Locator;
  public sendBtn: Locator;
  public stopBtn: Locator;
  public installBtn: Locator;
  public updateBtn: Locator;
  public ignoreBtn: Locator;
  public settingsBtn: Locator;
  public settingsMenu: Locator;
  public configurePositAiItem: Locator;
  public aboutItem: Locator;
  public newConversationBtn: Locator;
  public historyBtn: Locator;
  public conversationList: Locator;

  // Outside the iframe (main page — GWT wrapper)
  public dialogOverlay: Locator;
  public readlineNotification: Locator;

  constructor(page: Page) {
    super(page, CHAT_FRAME_SELECTOR);

    this.chatRoot = this.frame.locator('#root');
    this.chatTextarea = this.frame.locator('textarea');
    this.messageItem = this.frame.locator('[data-message-id]');
    this.allowBtn = this.frame.locator("button:has-text('Allow')");
    this.allowDropdownTrigger = this.frame.locator('button.rounded-l-none:has(svg.lucide-chevron-down)');
    this.allowForSessionItem = this.frame.locator('[role="menuitem"]:has-text("for this session")');
    this.sendBtn = this.frame.locator("button:has(svg.lucide-arrow-up)");
    this.stopBtn = this.frame.locator("button:has(svg.lucide-square)");
    this.installBtn = this.frame.locator("button:has-text('Install')");
    this.updateBtn = this.frame.locator("button:has-text('Update')");
    this.ignoreBtn = this.frame.locator("button:has-text('Ignore')");
    this.settingsBtn = this.frame.locator("xpath=//span[contains(text(), 'Settings')]/ancestor::button");
    this.settingsMenu = this.frame.locator("[data-slot='dropdown-menu-content']");
    this.configurePositAiItem = this.frame.locator("xpath=//span[contains(text(), 'Configure Posit AI')] | //div[contains(text(), 'Configure Posit AI')] | //*[@role='menuitem'][contains(., 'Configure Posit AI')]");
    this.aboutItem = this.frame.locator("xpath=//span[contains(text(), 'About')] | //div[contains(text(), 'About')] | //*[@role='menuitem'][contains(., 'About')]");
    this.newConversationBtn = this.frame.locator("button:has(svg.lucide-plus)");
    this.historyBtn = this.frame.locator("button:has(svg.lucide-history)");
    this.conversationList = this.frame.locator("[class*='conversation']");

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



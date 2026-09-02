import type { Page, Locator, FrameLocator } from 'playwright';
import { FramePageObject } from './page_object_base_classes';
import { typingTimeout } from '../utils/constants';

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
  public conversationHistoryPanel: Locator;
  public conversationListItem: Locator;
  public pendingQuestions: Locator;
  public submitAnswersBtn: Locator;

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
    // The provider-settings menu item has been relabeled twice: "Configure
    // Posit AI" -> "Configure LLM providers" -> "Configure AI providers".
    // Match any of them so the test passes against both the released assistant
    // and the pre-release builds the release gate exercises.
    this.configurePositAiItem = this.frame.getByRole('menuitem', {
      name: /Configure (AI providers|LLM providers|Posit AI)/i,
    });
    this.aboutItem = this.frame.locator("xpath=//span[contains(text(), 'About')] | //div[contains(text(), 'About')] | //*[@role='menuitem'][contains(., 'About')]");
    this.newConversationBtn = this.frame.getByRole('button', { name: 'New conversation' });
    this.historyBtn = this.frame.getByRole('button', { name: 'Conversation history' });
    // The history panel container and its per-conversation rows. The panel is
    // removed from the DOM when the history is closed, so its presence is the
    // open/closed signal that openConversationHistory / closeConversationHistory
    // key off. The old catch-all "[class*='conversation']" locator matched the
    // panel itself before any row, and matched nothing at all once the panel
    // closed -- both of which made state-blind assertions racy.
    this.conversationHistoryPanel = this.frame.locator('.conversation-list-panel');
    this.conversationListItem = this.frame.locator('.conversation-list-item-panel');
    // PAI 0.7.x AskUser elicitation: the assistant can pause mid-turn and render
    // a question form instead of acting. The turn stays active (stop button
    // showing, composer in "queue" mode) until the form is submitted or
    // cancelled, so any wait keyed on streaming completion hangs until answered.
    this.pendingQuestions = this.frame.getByRole('tablist', { name: 'Pending questions' });
    this.submitAnswersBtn = this.frame.getByRole('button', { name: 'Submit Answers' });

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

  async isPendingQuestionVisible(): Promise<boolean> {
    return await this.pendingQuestions.isVisible().catch(() => false);
  }

  /**
   * Whether the conversation's last message is an assistant reply with any
   * content beyond a thinking disclosure.
   *
   * The provider can drop a turn mid-stream: the turn ends cleanly as far as
   * the composer is concerned (stop button gone, so isTurnIdle passes) after
   * streaming only the collapsed "Thought for Xs" block -- or nothing at all,
   * leaving the user's own prompt as the last message. No tool ran and no
   * reply text exists, so waits keyed on turn completion succeed while
   * assertions about the assistant's work fail on work it never did. This
   * predicate is the discriminator: callers re-send the prompt when it
   * reports false.
   *
   * The thinking disclosure (databot's ElementThinking) is a toggle button
   * -- labeled "Thought for Xs" / "Thinking" and carrying aria-expanded --
   * plus a collapsible region whose aria-labelledby names the toggle's id;
   * stripping each matching toggle and the region it labels leaves only
   * genuine reply content. Nothing beyond that provably-linked pair is
   * removed, so an unrecognized future DOM fails toward "substantive", i.e.
   * no retry, which is the pre-detector behavior rather than a
   * retry-until-exhausted loop.
   */
  async isLastMessageSubstantive(): Promise<boolean> {
    if ((await this.getMessageCount()) === 0) {
      return false;
    }

    // A torn-down iframe (the message items gone by evaluate time) is the
    // provider blip itself, so a failed lookup reads as "not substantive"
    // rather than erroring out of the caller's retry loop.
    return await this.messageItem.last().evaluate((el) => {
      // Each message stamps data-message-id on both the ChatMessage row
      // wrapper and the nested MessageRenderer content div, so last() lands
      // on the inner div and the assistant class sits on an ancestor;
      // closest() covers that shape, querySelector() the row wrapper.
      if (!el.closest('.chat-message-assistant') && !el.querySelector('.chat-message-assistant')) {
        return false;
      }

      const clone = el.cloneNode(true) as HTMLElement;
      for (const toggle of Array.from(clone.querySelectorAll('button[aria-expanded]'))) {
        const name = toggle.getAttribute('aria-label') ?? toggle.textContent ?? '';
        if (/^\s*(Thought for|Thinking)\b/.test(name)) {
          // The collapsed thought text stays in the DOM (the region only
          // animates shut), so textContent would count it; remove the region
          // through its aria-labelledby link rather than assuming where the
          // toggle sits relative to it.
          if (toggle.id) {
            for (const region of Array.from(
              clone.querySelectorAll(`[aria-labelledby~="${CSS.escape(toggle.id)}"]`)
            )) {
              region.remove();
            }
          }
          toggle.remove();
        }
      }

      return (clone.textContent ?? '').trim().length > 0;
    }, undefined, { timeout: 5000 }).catch(() => false);
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
    await this.chatInput.pressSequentially(text, { timeout: typingTimeout(text) });
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
    // Exact match: the dialog title is also "Delete conversation"
    return this.frame.getByRole('button', { name: 'Delete conversation', exact: true });
  }
}



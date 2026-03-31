import type { Page, Locator } from 'playwright';
import { PageObject } from './page_object_base_classes';

// ---------------------------------------------------------------------------
// Class-based page object
// ---------------------------------------------------------------------------

export class AssistantOptions extends PageObject {
  public assistantTab: Locator;
  public assistantPanel: Locator;
  public codeAssistantSelect: Locator;
  public showCodeSuggestionsSelect: Locator;
  public enableNesCheckbox: Locator;
  public chatProviderSelect: Locator;
  public optionsOkButton: Locator;

  constructor(page: Page) {
    super(page);
    this.assistantTab = page.locator('#rstudio_label_assistant_options');
    this.assistantPanel = page.locator('#rstudio_label_assistant_options_panel');
    this.codeAssistantSelect = page.locator("xpath=//label[contains(text(),'Use code assistant')]/following::select[1]");
    this.showCodeSuggestionsSelect = page.locator("xpath=//label[contains(text(),'Show code suggestions')]/following::select[1]");
    this.enableNesCheckbox = page.locator("xpath=//label[contains(text(),'Enable next edit suggestions')]/../input");
    this.chatProviderSelect = page.locator("xpath=//label[contains(text(),'Chat provider:')]/following::select[1]");
    this.optionsOkButton = page.locator('#rstudio_preferences_confirm');
  }
}

// ---------------------------------------------------------------------------
// Backward-compatible exports (used by other tests)
// ---------------------------------------------------------------------------

export const ASSISTANT_TAB = '#rstudio_label_assistant_options';
export const ASSISTANT_PANEL = '#rstudio_label_assistant_options_panel';
export const CODE_ASSISTANT_SELECT = "//label[contains(text(),'Use code assistant')]/following::select[1]";
export const SHOW_CODE_SUGGESTIONS_SELECT = "//label[contains(text(),'Show code suggestions')]/following::select[1]";
export const ENABLE_NES_CHECKBOX = "//label[contains(text(),'Enable next edit suggestions')]/../input";
export const CHAT_PROVIDER_SELECT = "//label[contains(text(),'Chat provider:')]/following::select[1]";
export const OPTIONS_OK_BUTTON = '#rstudio_preferences_confirm';


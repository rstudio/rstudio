import type { Page, Locator } from 'playwright';
import { PageObject } from './page_object_base_classes';

// ---------------------------------------------------------------------------
// Class-based page object
// ---------------------------------------------------------------------------

export class SourcePane extends PageObject {
  public selectedTab: Locator;
  public ghostText: Locator;
  public contentPane: Locator;
  public nesApply: Locator;
  public nesSuggestionContent: Locator;
  public aceTextInput: Locator;
  public nesInsertionPreview: Locator;
  public nesDeletionMarker: Locator;
  public nesDiscard: Locator;
  public nesGutter: Locator;
  public publishBtn: Locator;
  public formatOptions: Locator;
  public viewerPaneOption: Locator;
  public knitOptions: Locator;
  public knitHtml: Locator;
  public previewBtn: Locator;
  public saveBtn: Locator;
  public runLineBtn: Locator;
  public footerTable: Locator;
  public visualMdToggle: Locator;
  public secondaryToolbar: Locator;
  public chunkImage: Locator;
  public statusBarCompletionReceived: Locator;
  public statusBarCompletionPending: Locator;

  constructor(page: Page) {
    super(page);
    this.selectedTab = page.locator("[class*='rstudio_source_panel'] [class*='PanelTab-selected']");
    this.ghostText = page.locator('[class*=ace_ghost_text]');
    this.contentPane = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//*[@class='ace_content' and not(ancestor::*[contains(concat(' ', normalize-space(@class), ' '), ' ace_lineWidgetContainer ')])]");
    this.nesApply = page.locator("xpath=//*[text()='Apply']");
    this.nesSuggestionContent = page.locator('.ace_lineWidgetContainer .ace_content');
    this.aceTextInput = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//textarea[contains(@class,'ace_text-input') and not(ancestor::*[contains(concat(' ', normalize-space(@class), ' '), ' ace_lineWidgetContainer ')])]");
    this.nesInsertionPreview = page.locator('.ace_insertion_preview');
    this.nesDiscard = page.locator("xpath=//*[text()='Discard']");
    this.nesDeletionMarker = page.locator('[class*=ace_next-edit-suggestion-deletion]');
    this.nesGutter = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//*[contains(@class,'ace_nes-gutter')]");
    // The toolbar locators below scope to the visible tabpanel (same pattern as
    // contentPane / aceTextInput above). Each open editor tab renders its own
    // copy of these buttons; a plain page-wide locator triggers Playwright's
    // strict-mode "multiple elements" error as soon as a second tab exists
    // (e.g. the Untitled placeholder left by resetSourcePaneState plus a test-
    // opened file). Scoping picks the one belonging to the active tab.
    this.publishBtn = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_publish_item_editor')]");
    this.formatOptions = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[@aria-label='Edit the R Markdown format options for the current file']");
    this.viewerPaneOption = page.locator('#rstudio_label_preview_in_viewer_pane_command');
    this.knitOptions = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[@aria-label='Knit options']");
    this.knitHtml = page.locator('#rstudio_label_knit_to_html_command');
    this.previewBtn = page.locator("xpath=//*[contains(@title,'Preview') and not(contains(@style, 'display: none'))]");
    this.saveBtn = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[contains(@title,'Save current doc')]");
    // Title prefix is platform-stable (the shortcut suffix differs); matching
    // it avoids depending on the hashed `run_the_current_line_or_selection_*`
    // class. Scoped to the visible tabpanel like the buttons above.
    this.runLineBtn = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[contains(@title,'Run the current line or selection')]");
    this.footerTable = page.locator("xpath=//*[contains(@class, 'rstudio_source_panel')]//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[contains(@class, 'rstudio-themes-background')]");
    this.visualMdToggle = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[contains(concat(' ', normalize-space(@class), ' '), ' rstudio_visual_md_on ')]");
    this.secondaryToolbar = page.locator('[aria-label="Markdown editing tools"]');
    this.chunkImage = page.locator("xpath=//*[@id='rstudio_source_text_editor']//*[@class='gwt-Image']");
    this.statusBarCompletionReceived = this.footerTable.locator('.gwt-Label', { hasText: 'Completion response received' });
    // Shown while a code-completion request is in flight (COMPLETION_REQUESTED).
    // Its presence means a response may still land and re-render ghost text.
    this.statusBarCompletionPending = this.footerTable.locator('.gwt-Label', { hasText: 'Waiting for completions' });
  }
}


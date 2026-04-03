import type { Page, Locator } from 'playwright';
import { PageObject } from './page_object_base_classes';

// ---------------------------------------------------------------------------
// Class-based page object
// ---------------------------------------------------------------------------

export class SourcePane extends PageObject {
  public selectedTab: Locator;
  public copilotGhostText: Locator;
  public contentPane: Locator;
  public nesApply: Locator;
  public nesSuggestionContent: Locator;
  public aceTextInput: Locator;
  public nesInsertionPreview: Locator;
  public nesDeletionMarker: Locator;
  public nesGutter: Locator;
  public publishBtn: Locator;
  public formatOptions: Locator;
  public viewerPaneOption: Locator;
  public knitOptions: Locator;
  public knitHtml: Locator;
  public previewBtn: Locator;
  public saveBtn: Locator;
  public footerTable: Locator;
  public visualMdToggle: Locator;
  public secondaryToolbar: Locator;
  public chunkImage: Locator;

  constructor(page: Page) {
    super(page);
    this.selectedTab = page.locator("[class*='rstudio_source_panel'] [class*='PanelTab-selected']");
    this.copilotGhostText = page.locator('[class*=ace_ghost_text]');
    this.contentPane = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//*[@class='ace_content']");
    this.nesApply = page.locator("xpath=//*[text()='Apply']");
    this.nesSuggestionContent = page.locator('.ace_lineWidgetContainer .ace_content');
    this.aceTextInput = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//textarea[contains(@class,'ace_text-input')]");
    this.nesInsertionPreview = page.locator('.ace_insertion_preview');
    this.nesDeletionMarker = page.locator('[class*=ace_next-edit-suggestion-deletion]');
    this.nesGutter = page.locator("xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//*[contains(@class,'ace_nes-gutter')]");
    this.publishBtn = page.locator("[id*='rstudio_publish_item_editor']");
    this.formatOptions = page.locator('[aria-label="Edit the R Markdown format options for the current file"]');
    this.viewerPaneOption = page.locator('#rstudio_label_preview_in_viewer_pane_command');
    this.knitOptions = page.locator('[aria-label="Knit options"]');
    this.knitHtml = page.locator('#rstudio_label_knit_to_html_command');
    this.previewBtn = page.locator("xpath=//*[contains(@title,'Preview') and not(contains(@style, 'display: none'))]");
    this.saveBtn = page.locator("[class*='rstudio_source_panel'] [title*='Save current doc']");
    this.footerTable = page.locator("xpath=//*[contains(@class, 'rstudio_source_panel')]//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[contains(@class, 'rstudio-themes-background')]");
    this.visualMdToggle = page.locator('.rstudio_visual_md_on');
    this.secondaryToolbar = page.locator('[aria-label="Markdown editing tools"]');
    this.chunkImage = page.locator("xpath=//*[@id='rstudio_source_text_editor']//*[@class='gwt-Image']");
  }
}


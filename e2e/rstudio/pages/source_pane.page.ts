import type { Page, Locator } from 'playwright';
import { expect } from '@playwright/test';
import { PageObject } from './page_object_base_classes';
import { TIMEOUTS, sleep } from '../utils/constants';
import { typeInConsole } from './console_pane.page';

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

// ---------------------------------------------------------------------------
// Backward-compatible exports
// ---------------------------------------------------------------------------

export const SELECTED_TAB = "[class*='rstudio_source_panel'] [class*='PanelTab-selected']";
export const COPILOT_GHOST_TEXT = '[class*=ace_ghost_text]';
export const CONTENT_PANE = "//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//*[@class='ace_content']";
export const NES_APPLY = "//*[text()='Apply']";
export const NES_SUGGESTION_CONTENT = '.ace_lineWidgetContainer .ace_content';
export const ACE_TEXT_INPUT = "//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//textarea[contains(@class,'ace_text-input')]";
export const NES_INSERTION_PREVIEW = '.ace_insertion_preview';
export const NES_DELETION_MARKER = '[class*=ace_next-edit-suggestion-deletion]';
export const NES_GUTTER = "//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//*[contains(@class,'ace_nes-gutter')]";
export const PUBLISH_BTN = "[id*='rstudio_publish_item_editor']";
export const FORMAT_OPTIONS = '[aria-label="Edit the R Markdown format options for the current file"]';
export const VIEWER_PANE_OPTION = '#rstudio_label_preview_in_viewer_pane_command';
export const KNIT_OPTIONS = '[aria-label="Knit options"]';
export const KNIT_HTML = '#rstudio_label_knit_to_html_command';
export const PREVIEW_BTN = "//*[contains(@title,'Preview') and not(contains(@style, 'display: none'))]";
export const SAVE_BTN = "[class*='rstudio_source_panel'] [title*='Save current doc']";
export const FOOTER_TABLE = "//*[contains(@class, 'rstudio_source_panel')]//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[contains(@class, 'rstudio-themes-background')]";
export const VISUAL_MD_TOGGLE = '.rstudio_visual_md_on';
export const SECONDARY_TOOLBAR = '[aria-label="Markdown editing tools"]';
export const CHUNK_IMAGE = "//*[@id='rstudio_source_text_editor']//*[@class='gwt-Image']";

export async function createAndOpenFile(page: Page, fileName: string, fileContent: string): Promise<void> {
  await typeInConsole(page, `writeLines("${fileContent}", "${fileName}")`);
  await sleep(1000);

  await typeInConsole(page, `file.edit('${fileName}')`);

  await expect(page.locator(SELECTED_TAB)).toContainText(fileName, { timeout: TIMEOUTS.fileOpen });
}

export async function closeSourceAndDeleteFile(page: Page, fileName: string): Promise<void> {
  await typeInConsole(page, ".rs.api.executeCommand('closeAllSourceDocs')");
  await sleep(1000);

  try {
    const dontSaveBtn = page.locator("button:has-text('Don\\'t Save'), button:has-text('Do not Save'), #rstudio_dlg_no");
    await dontSaveBtn.click({ timeout: 2000 });
  } catch {
    // No dialog appeared
  }

  const aceInputs = page.locator(`xpath=${ACE_TEXT_INPUT}`);
  await expect(aceInputs).toHaveCount(0, { timeout: 5000 }).catch(() => {});
  await sleep(500);

  await typeInConsole(page, `unlink("${fileName}")`);
  await sleep(500);
}

export async function sendText(page: Page, text: string): Promise<void> {
  const contentPane = page.locator(`xpath=${CONTENT_PANE}`);
  await contentPane.click();
  await sleep(300);
  await page.keyboard.type(text);
  await sleep(300);
}

export async function acceptNesRename(page: Page): Promise<string> {
  const nesApply = page.locator(`xpath=${NES_APPLY}`);
  const ghostTextLocator = page.locator(COPILOT_GHOST_TEXT);
  const insertionPreview = page.locator(NES_INSERTION_PREVIEW);
  const nesGutter = page.locator(`xpath=${NES_GUTTER}`);

  await expect(nesApply.or(ghostTextLocator).or(insertionPreview).or(nesGutter).first()).toBeVisible({ timeout: TIMEOUTS.nesApply });
  await sleep(2000);

  if (await nesApply.first().isVisible()) {
    const nesSuggestion = page.locator(NES_SUGGESTION_CONTENT).first();
    await expect(nesSuggestion).toBeVisible();
    const nesSuggestionText = await nesSuggestion.textContent();
    const nesSuggestionHtml = await nesSuggestion.innerHTML();
    console.log('  NES Apply suggestion (text): ' + nesSuggestionText);
    console.log('  NES Apply suggestion (html): ' + nesSuggestionHtml);
    await nesApply.first().click();
    await sleep(2000);
    return 'apply';
  } else if (await insertionPreview.first().isVisible()) {
    const insertionText = await insertionPreview.first().textContent();
    const count = await insertionPreview.count();
    console.log(`  NES inline diff: "${insertionText}" (${count} suggestion(s) visible)`);
    await page.keyboard.press('Control+;');
    await sleep(2000);
    return 'inline-diff';
  } else if (await ghostTextLocator.first().isVisible()) {
    const ghostTextParts = await ghostTextLocator.allTextContents();
    console.log('  NES ghost text: "' + ghostTextParts.join('') + '"');
    await page.keyboard.press('Control+;');
    await sleep(2000);
    return 'ghost-text';
  } else {
    console.log('  NES gutter icon detected — clicking to reveal suggestion...');
    await nesGutter.first().click();
    await sleep(2000);

    try {
      await expect(nesApply.or(ghostTextLocator).or(insertionPreview).first()).toBeVisible({ timeout: 15000 });
      await sleep(2000);

      if (await nesApply.first().isVisible()) {
        const nesSuggestionText = await page.locator(NES_SUGGESTION_CONTENT).first().textContent();
        console.log('  NES Apply suggestion (after gutter click): ' + nesSuggestionText);
        await nesApply.first().click();
        await sleep(2000);
      } else if (await insertionPreview.first().isVisible()) {
        const insertionText = await insertionPreview.first().textContent();
        console.log('  NES inline diff (after gutter click): "' + insertionText + '"');
        await page.keyboard.press('Control+;');
        await sleep(2000);
      } else {
        const ghostParts = await ghostTextLocator.allTextContents();
        console.log('  NES ghost text (after gutter click): "' + ghostParts.join('') + '"');
        await page.keyboard.press('Control+;');
        await sleep(2000);
      }
      return 'gutter-clicked';
    } catch {
      console.log('  WARNING: No suggestion appeared after clicking gutter icon');
      throw new Error('NES gutter icon clicked but no suggestion appeared');
    }
  }
}

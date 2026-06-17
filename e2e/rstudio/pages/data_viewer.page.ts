import type { Page, Locator, FrameLocator } from 'playwright';
import { PageObject } from './page_object_base_classes';

export class DataViewerPane extends PageObject {
  // Toolbar elements (outside iframe): the "Go to column" typeahead box
  // (shown when columns overflow the viewport) and its suggestion entries
  // (a GWT SuggestBox popup attached to the main document body).
  public gotoColumnInput: Locator;
  public gotoColumnSuggestions: Locator;

  // Iframe accessor
  public frame: FrameLocator;

  // Elements inside the iframe
  public gridInfo: Locator;
  public sortStatus: Locator;
  public clearSortButton: Locator;
  public viewport: Locator;
  public horizontalScrollbar: Locator;

  constructor(page: Page) {
    super(page);

    this.gotoColumnInput = page.locator('#data-viewer-goto-column input');
    this.gotoColumnSuggestions = page.locator('.gwt-SuggestBoxPopup .item');

    // The data grid renders inside an iframe
    this.frame = page.frameLocator('[title="Data Browser"]');
    this.gridInfo = this.frame.locator('#rsGridData_info');
    // "Sorted by: <col>" status at the right of the info bar, with its
    // clear-sort button (hidden while no sort is active).
    this.sortStatus = this.frame.locator('#rsGridData_info_sort');
    this.clearSortButton = this.frame.locator('#rsGridData_info_sort_clear');
    this.viewport = this.frame.locator('#gridViewport');
    // Custom auto-hide overlay scrollbar for the horizontal axis. Carries the
    // "visible" class while shown; the class is removed when it fades out.
    this.horizontalScrollbar = this.frame.locator('.custom-scrollbar.horizontal');
  }

  /** Get a column header locator by column number (inside iframe). */
  columnHeader(colNumber: number): Locator {
    return this.frame.locator(`[title^='column ${colNumber}:']`);
  }

  /**
   * Jump to a column via the toolbar's Go to column box: type a column
   * index or name and press Enter to accept the top suggestion. Types with
   * pressSequentially -- the GWT SuggestBox queries its oracle from key
   * events, which fill() doesn't generate.
   */
  async goToColumn(column: number | string): Promise<void> {
    await this.gotoColumnInput.click();
    await this.gotoColumnInput.fill('');
    await this.gotoColumnInput.pressSequentially(String(column));
    // Wait for a suggestion before committing -- name matches require the
    // async column-name fetch on first use.
    await this.gotoColumnSuggestions.first().waitFor({ state: 'visible' });
    await this.gotoColumnInput.press('Enter');
  }

  /** Get all number cells in the grid (inside iframe). */
  get numberCells(): Locator {
    return this.frame.locator("[class*='numberCell']");
  }
}

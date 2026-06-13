import type { Page, Locator, FrameLocator } from 'playwright';
import { PageObject } from './page_object_base_classes';

export class DataViewerPane extends PageObject {
  // Toolbar elements (outside iframe)
  public gotoColumnButton: Locator;

  // Iframe accessor
  public frame: FrameLocator;

  // The go-to-column popup's input, inside the iframe (present only while
  // the popup is open).
  public gotoColumnInput: Locator;

  // Elements inside the iframe
  public gridInfo: Locator;
  public sortStatus: Locator;
  public clearSortButton: Locator;
  public viewport: Locator;
  public horizontalScrollbar: Locator;

  constructor(page: Page) {
    super(page);

    // The "Go to Column..." toolbar button (only shown for frames wider
    // than one fetch window). Opens a typeahead popup inside the iframe.
    this.gotoColumnButton = page.locator('#data-viewer-goto-column');

    // The data grid renders inside an iframe
    this.frame = page.frameLocator('[title="Data Browser"]');
    this.gotoColumnInput = this.frame.locator('#gotoColumnInput');
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
   * Jump to a column via the Go to Column popup: open it from the toolbar
   * button, type a column index or name, and press Enter to accept the
   * first/active suggestion.
   */
  async goToColumn(column: number | string): Promise<void> {
    await this.gotoColumnButton.click();
    await this.gotoColumnInput.fill(String(column));
    // Wait for a suggestion to materialize -- Enter on an empty list is a
    // no-op (e.g. name matches require the async column-name fetch).
    await this.frame.locator('.goto-column-item').first().waitFor({ state: 'visible' });
    await this.gotoColumnInput.press('Enter');
  }

  /** Get all number cells in the grid (inside iframe). */
  get numberCells(): Locator {
    return this.frame.locator("[class*='numberCell']");
  }
}

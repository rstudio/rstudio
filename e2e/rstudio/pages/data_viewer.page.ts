import type { Page, Locator, FrameLocator } from 'playwright';
import { PageObject } from './page_object_base_classes';

export class DataViewerPane extends PageObject {
  // Toolbar elements (outside iframe)
  public columnNumberInput: Locator;

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

    // The "go to column" jump box in the RStudio toolbar (only shown for
    // frames wider than one fetch window).
    this.columnNumberInput = page.locator('#data-viewer-column-input');

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

  /** Jump to an absolute (1-based) column via the toolbar's jump box. */
  async goToColumn(column: number): Promise<void> {
    await this.columnNumberInput.fill(String(column));
    await this.columnNumberInput.press('Enter');
  }

  /** Get all number cells in the grid (inside iframe). */
  get numberCells(): Locator {
    return this.frame.locator("[class*='numberCell']");
  }
}

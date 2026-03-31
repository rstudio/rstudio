import type { Page, Locator, FrameLocator } from 'playwright';
import { PageObject } from './page_object_base_classes';

export class DataViewerPane extends PageObject {
  // Toolbar elements (outside iframe)
  public rightArrow: Locator;
  public leftArrow: Locator;
  public rightDoubleArrow: Locator;
  public leftDoubleArrow: Locator;
  public columnNumberInput: Locator;

  // Iframe accessor
  public frame: FrameLocator;

  // Elements inside the iframe
  public gridInfo: Locator;

  constructor(page: Page) {
    super(page);

    // Navigation arrows and column input are in the RStudio toolbar
    this.rightArrow = page.locator("[class*='icon-angle-right']");
    this.leftArrow = page.locator("[class*='icon-angle-left']");
    this.rightDoubleArrow = page.locator("[class*='icon-angle-double-right']");
    this.leftDoubleArrow = page.locator("[class*='icon-angle-double-left']");
    this.columnNumberInput = page.locator('#data-viewer-column-input');

    // The data grid renders inside an iframe
    this.frame = page.frameLocator('[title="Data Browser"]');
    this.gridInfo = this.frame.locator('#rsGridData_info');
  }

  /** Get a column header locator by column number (inside iframe). */
  columnHeader(colNumber: number): Locator {
    return this.frame.locator(`[title^='column ${colNumber}:'][tabindex]`);
  }

  /** Get all number cells in the grid (inside iframe). */
  get numberCells(): Locator {
    return this.frame.locator("[class*='numberCell']");
  }
}

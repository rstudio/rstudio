import type { Page, Locator } from 'playwright';
import { PageObject } from './page_object_base_classes';

export class InsertCitationModal extends PageObject {
  public fromDoiLabel: Locator;
  public crossrefLabel: Locator;
  public dataCiteLabel: Locator;
  public pubMedLabel: Locator;
  public rPackageLabel: Locator;
  public searchInput: Locator;
  public searchBtn: Locator;
  public citationItem: Locator;
  public rPackageCitationItem: Locator;
  public stagedCitationItem: Locator;
  public deleteCitationBtn: Locator;
  public insertBtn: Locator;
  public cancelBtn: Locator;

  constructor(page: Page) {
    super(page);
    this.fromDoiLabel = page.locator("[alt='From DOI']");
    this.crossrefLabel = page.locator("[alt='Crossref']");
    this.dataCiteLabel = page.locator("[alt='DataCite']");
    this.pubMedLabel = page.locator("[alt='PubMed']");
    this.rPackageLabel = page.locator("[alt='R Package']");
    this.searchInput = page.locator("[class*='citation-panel-latent-search'] input");
    this.searchBtn = page.locator("[class*='citation-panel-latent-search'] button");
    this.citationItem = page.locator("[class*='citation-source-panel-item-detailed'] button");
    this.rPackageCitationItem = page.locator("[class*='citation-source-panel-item'] button");
    this.stagedCitationItem = page.locator("[class*='input-text-edittable']");
    this.deleteCitationBtn = page.locator("[class*='input-delete-image']");
    this.insertBtn = page.locator("xpath=//button[text()='Insert']");
    this.cancelBtn = page.locator("xpath=//button[text()='Cancel']");
  }
}

import { Page, FrameLocator } from '@playwright/test';

export class PageObject {
  protected page: Page;

  constructor(page: Page) {
    this.page = page;
  }
}

export class FramePageObject {
  protected page: Page;
  public frame: FrameLocator;

  constructor(page: Page, frameSelector: string) {
    this.page = page;
    this.frame = page.frameLocator(frameSelector);
  }
}

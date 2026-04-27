import type { Page } from 'playwright';
import { expect } from '@playwright/test';
import { sleep } from '../utils/constants';

// Selectors
export const CONFIRM_BTN = '#rstudio_dlg_ok';
export const CANCEL_BTN = '#rstudio_dlg_cancel';
export const YES_BTN = '#rstudio_dlg_yes';
export const NO_BTN = '#rstudio_dlg_no';
export const RMARKDOWN_MODAL = 'div.gwt-DialogBox[aria-label="New R Markdown"]';
export const TEMPLATE_OPTION = "//span[contains(.,'From Template')]";
export const TEMPLATE_LIST = '#rstudio_new_rmd_template';
export const SPELLING_MODAL = "div.gwt-DialogBox[aria-label='Check Spelling']";

// Actions
export async function installDepIfPrompted(page: Page, detectTimeout: number = 5000, installTimeout: number = 500000): Promise<void> {
  try {
    await page.locator(YES_BTN).click({ timeout: detectTimeout });
    console.log('Clicked Yes to install dependencies');
    // Wait for interrupt button flicker to settle
    await sleep(5000);
    // Wait for interrupt button to disappear (install complete)
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).not.toBeVisible({ timeout: installTimeout });
  } catch {
    // No dependency install dialog appeared
  }
}

export async function clickConfirmIfVisible(page: Page, timeout: number = 15000): Promise<void> {
  try {
    await page.locator(CONFIRM_BTN).click({ timeout });
    console.log('Clicked OK on confirmation dialog');
    await sleep(1000);
  } catch {
    // No confirmation dialog appeared
  }
}

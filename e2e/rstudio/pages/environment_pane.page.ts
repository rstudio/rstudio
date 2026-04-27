import type { Page } from 'playwright';
import { sleep } from '../utils/constants';
import { CONFIRM_BTN } from './modals.page';

// Selectors
export const CLEAR_WORKSPACE_BTN = '#rstudio_tb_clearworkspace';

// Actions
export async function clearWorkspace(page: Page): Promise<void> {
  await page.locator(CLEAR_WORKSPACE_BTN).click();
  await sleep(500);
  await page.locator(CONFIRM_BTN).click();
  await sleep(1000);
}

import { test, expect } from '@fixtures/rstudio.fixture';
import {
  APPEARANCE_TAB,
  APPEARANCE_PREVIEW,
  CODE_TAB,
  CODE_COMPLETION_TAB,
  CODE_COMPLETION_PANEL,
  GENERAL_TAB,
  GENERAL_PANEL,
  OPTIONS_OK,
  OPTIONS_CANCEL,
  OPTIONS_APPLY,
  SPELLING_TAB,
  SPELLING_PANEL,
  closeGlobalOptions,
  openGlobalOptions,
} from '@pages/global_options.page';

test.describe('Global Options layout', () => {
  test('panes fit the dialog while an open window is resized', async ({ rstudioPage: page }) => {
    const originalSize = await page.evaluate(() => ({ width: innerWidth, height: innerHeight }));
    try {
      await page.setViewportSize({ width: 800, height: 900 });
      await openGlobalOptions(page);

      for (const height of [641, 480, 900]) {
        await page.setViewportSize({ width: 800, height });

        // Cover both a tabbed pane and a pane with its tab header hidden.
        for (const [tab, panel] of [[GENERAL_TAB, GENERAL_PANEL], [SPELLING_TAB, SPELLING_PANEL]]) {
          await page.locator(tab).click();
          await expect.poll(() => page.locator(panel).evaluate(element => {
            const container = element.parentElement!;
            return container.scrollHeight - container.clientHeight;
          })).toBeLessThanOrEqual(1);
        }

        await page.locator(APPEARANCE_TAB).click();
        await expect(page.locator(APPEARANCE_PREVIEW)).toBeVisible();
        for (const button of [OPTIONS_OK, OPTIONS_CANCEL, OPTIONS_APPLY]) {
          await expect(page.locator(button)).toBeInViewport({ ratio: 1 });
        }
      }
    } finally {
      await closeGlobalOptions(page);
      await page.setViewportSize(originalSize);
    }
  });

  test('tall tab content scrolls without moving the tab header or footer', async ({ rstudioPage: page }) => {
    const originalSize = await page.evaluate(() => ({ width: innerWidth, height: innerHeight }));
    try {
      await page.setViewportSize({ width: 800, height: 480 });
      await openGlobalOptions(page);
      await page.locator(CODE_TAB).click();
      await page.locator(CODE_COMPLETION_TAB).click();

      const lastInput = page.locator(CODE_COMPLETION_PANEL).locator('input:visible').last();
      await lastInput.scrollIntoViewIfNeeded();
      await lastInput.click();
      await expect(lastInput).toBeInViewport({ ratio: 1 });
      await expect(lastInput).toBeFocused();
      await expect(page.locator(CODE_COMPLETION_TAB)).toBeInViewport({ ratio: 1 });
      await expect(page.locator(OPTIONS_CANCEL)).toBeInViewport({ ratio: 1 });
      await expect.poll(() => lastInput.evaluate(element => {
        for (let parent = element.parentElement; parent; parent = parent.parentElement) {
          if (parent.scrollTop > 0 && /auto|scroll/.test(getComputedStyle(parent).overflowY))
            return true;
        }
        return false;
      })).toBe(true);
    } finally {
      await closeGlobalOptions(page);
      await page.setViewportSize(originalSize);
    }
  });
});

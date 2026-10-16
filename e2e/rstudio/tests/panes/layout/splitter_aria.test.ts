// Pane splitters are focusable separators, which ARIA requires to report their position
// with aria-valuenow (#19151). The column splitter comes from the GWT fork's
// SplitLayoutPanel; the row splitter from RStudio's BinarySplitLayoutPanel.

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand, resetLayoutZoom } from '@utils/commands';
import type { Locator, Page } from 'playwright';

const COLUMN_SPLITTER = '#rstudio_middle_column_splitter';
const ROW_SPLITTER = '[role="separator"][aria-label="right splitter"]';
const KEY_PRESSES = 3;

/**
 * The splitter's aria-valuenow minus its rendered position, as a percentage of the distance
 * it can travel across its panel (0 at the left or top edge). NaN when aria-valuenow is unset.
 */
async function valueNowError(splitter: Locator): Promise<number> {
  return splitter.evaluate(el => {
    const vertical = el.getAttribute('aria-orientation') === 'vertical';
    // splitter -> layout layer container -> split panel
    const panel = el.parentElement!.parentElement!;
    const r = el.getBoundingClientRect();
    const p = panel.getBoundingClientRect();
    const offset = vertical ? r.left - p.left - panel.clientLeft : r.top - p.top - panel.clientTop;
    const range = vertical ? panel.clientWidth - r.width : panel.clientHeight - r.height;
    return Number(el.getAttribute('aria-valuenow') ?? NaN) - (100 * offset) / range;
  });
}

async function expectValueNowMatchesPosition(splitter: Locator): Promise<void> {
  // Rounding to a whole percentage leaves up to half a point of difference.
  await expect.poll(async () => Math.abs(await valueNowError(splitter))).toBeLessThanOrEqual(0.5);
}

async function valueNow(splitter: Locator): Promise<number> {
  return Number(await splitter.getAttribute('aria-valuenow'));
}

async function pressKey(page: Page, splitter: Locator, key: string, count: number): Promise<void> {
  await splitter.evaluate(el => (el as HTMLElement).focus());
  for (let i = 0; i < count; i++) {
    await page.keyboard.press(key);
  }
}

test.describe('Pane splitter aria-valuenow (#19151)', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await resetLayoutZoom(page);
  });

  test('column and row splitters report their position', async ({ rstudioPage: page }) => {
    await expectValueNowMatchesPosition(page.locator(COLUMN_SPLITTER));
    await expectValueNowMatchesPosition(page.locator(ROW_SPLITTER));
  });

  test('column splitter value follows keyboard resizing', async ({ rstudioPage: page }) => {
    const splitter = page.locator(COLUMN_SPLITTER);
    await expectValueNowMatchesPosition(splitter);
    const before = await valueNow(splitter);

    await pressKey(page, splitter, 'ArrowLeft', KEY_PRESSES);
    try {
      await expect.poll(() => valueNow(splitter)).toBeLessThan(before);
      await expectValueNowMatchesPosition(splitter);
    } finally {
      await pressKey(page, splitter, 'ArrowRight', KEY_PRESSES);
    }
  });

  test('row splitter value follows keyboard resizing', async ({ rstudioPage: page }) => {
    const splitter = page.locator(ROW_SPLITTER);
    await expectValueNowMatchesPosition(splitter);
    const before = await valueNow(splitter);

    await pressKey(page, splitter, 'ArrowUp', KEY_PRESSES);
    try {
      await expect.poll(() => valueNow(splitter)).toBeLessThan(before);
      await expectValueNowMatchesPosition(splitter);
    } finally {
      await pressKey(page, splitter, 'ArrowDown', KEY_PRESSES);
    }
  });

  test('column splitter value follows column zoom', async ({ rstudioPage: page }) => {
    // Zooming sizes the columns programmatically rather than through the splitter.
    const splitter = page.locator(COLUMN_SPLITTER);
    await expectValueNowMatchesPosition(splitter);
    expect(await valueNow(splitter)).toBeLessThan(100);

    // The zoomed left column fills the panel, so the zoom animation has finished once the
    // splitter reaches the right edge.
    await executeCommand(page, 'layoutZoomLeftColumn');
    await expect.poll(() => valueNow(splitter)).toBe(100);
    await expectValueNowMatchesPosition(splitter);

    // Ends the zoom like layoutEndZoom, but resolves only once the relayout has finished.
    // Ending the zoom restores the default column widths, not the pre-zoom ones.
    await resetLayoutZoom(page);
    await expect.poll(() => valueNow(splitter)).toBeLessThan(100);
    await expectValueNowMatchesPosition(splitter);
  });
});

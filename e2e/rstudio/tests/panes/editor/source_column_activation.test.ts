import { test, expect } from '@fixtures/rstudio.fixture';
import { documentCloseAllNoSave, executeCommand } from '@utils/commands';
import type { Page } from 'playwright';

const MAIN_COLUMN = '#rstudio_Source_pane';
const EXTRA_COLUMNS = '[id^="rstudio_Source"][id$="_pane"]:not(#rstudio_Source_pane)';

// Hold the 100 ms timers scheduled while a document is constructed, including
// its outline-latch timer. Other delays and RPCs keep running so we can select
// another column or close this one before releasing the callbacks (#18736).
// Keep real timer IDs and honor cancellation; restore the browser functions
// before releasing callbacks so timers they schedule run normally.
async function holdOutlineTimers(page: Page) {
  return page.evaluateHandle(() => {
    const browser: Window = window;
    const setTimeout = browser.setTimeout;
    const clearTimeout = browser.clearTimeout;
    const pending = new Map<number, () => void>();
    browser.setTimeout = (handler: TimerHandler, delay?: number, ...args: unknown[]) => {
      if (delay !== 100 || typeof handler !== 'function')
        return setTimeout.call(window, handler, delay, ...args);

      const id = setTimeout.call(window, () => {}, 60_000);
      pending.set(id, () => handler.apply(window, args));
      return id;
    };
    browser.clearTimeout = (id?: number) => {
      if (id !== undefined)
        pending.delete(id);
      clearTimeout.call(window, id);
    };

    return {
      release() {
        browser.setTimeout = setTimeout;
        browser.clearTimeout = clearTimeout;
        const callbacks = [...pending];
        pending.clear();
        for (const [id, callback] of callbacks) {
          clearTimeout.call(window, id);
          callback();
        }
        return callbacks.length;
      },
    };
  });
}

test.describe('Source column activation (#18736)', () => {
  test('outline initialization preserves a subsequently selected column', async ({ rstudioPage: page }) => {
    const mainDocument = await page.evaluate(() => window.rstudio!.documents.active());
    expect(mainDocument).not.toBeNull();
    await expect(page.locator(EXTRA_COLUMNS)).toHaveCount(0);
    const timers = await holdOutlineTimers(page);

    try {
      await executeCommand(page, 'newSourceColumn');
      await expect(page.locator(`${EXTRA_COLUMNS} .ace_editor`)).toHaveCount(1);

      await page.locator(`${MAIN_COLUMN} .ace_content`).click();
      await expect.poll(() => page.evaluate(() => window.rstudio!.documents.active()?.id))
        .toBe(mainDocument!.id);

      expect(await timers.evaluate(t => t.release()), 'document initialization timers were held')
        .toBeGreaterThan(0);
      expect(await page.evaluate(() => window.rstudio!.documents.active()?.id))
        .toBe(mainDocument!.id);
    } finally {
      await timers.evaluate(t => t.release());
      await timers.dispose();
      await documentCloseAllNoSave(page);
    }
  });

  test('a delayed outline callback cannot revive a closed column', async ({ rstudioPage: page }) => {
    await expect(page.locator(EXTRA_COLUMNS)).toHaveCount(0);
    const timers = await holdOutlineTimers(page);

    try {
      await executeCommand(page, 'newSourceColumn');
      await expect(page.locator(`${EXTRA_COLUMNS} .ace_editor`)).toHaveCount(1);

      await documentCloseAllNoSave(page);
      await expect(page.locator(EXTRA_COLUMNS)).toHaveCount(0);
      await expect(page.locator(MAIN_COLUMN)).not.toBeVisible();

      expect(await timers.evaluate(t => t.release()), 'closed document initialization timers were held')
        .toBeGreaterThan(0);

      await executeCommand(page, 'newSourceDoc');
      await expect(page.locator(`${MAIN_COLUMN} .ace_editor`)).toBeVisible();
      await expect.poll(() => page.evaluate(() => window.rstudio!.documents.active()))
        .toMatchObject({ path: null, dirty: false });
      await expect(page.locator(EXTRA_COLUMNS)).toHaveCount(0);
    } finally {
      await timers.evaluate(t => t.release());
      await timers.dispose();
      await documentCloseAllNoSave(page);
    }
  });
});

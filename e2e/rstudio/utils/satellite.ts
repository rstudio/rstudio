import type { Page } from '@playwright/test';

/**
 * On Desktop, Electron reports a closed satellite window to the main window
 * by calling unregisterDesktopChildWindow() on it, next to the report the
 * window makes itself. Hold those calls back by `delayMs`, to stand in for a
 * report that arrives well after the window's own. Undo with
 * restoreDesktopCloseReport().
 */
export async function delayDesktopCloseReport(page: Page, delayMs: number): Promise<void> {
  await page.evaluate((delay) => {
    const w = window as unknown as Record<string, any>;
    const original = w.unregisterDesktopChildWindow;
    w.__desktopCloseReports = 0;
    w.__restoreDesktopCloseReport = () => {
      w.unregisterDesktopChildWindow = original;
    };
    w.unregisterDesktopChildWindow = function (...args: unknown[]) {
      setTimeout(() => {
        original.apply(this, args);
        w.__desktopCloseReports++;
      }, delay);
    };
  }, delayMs);
}

/**
 * Wait until a report held back by delayDesktopCloseReport() has reached the
 * main window.
 */
export async function waitForDesktopCloseReport(page: Page, timeout = 15000): Promise<void> {
  await page.waitForFunction(
    () => (window as unknown as Record<string, any>).__desktopCloseReports > 0,
    null,
    { timeout },
  );
}

export async function restoreDesktopCloseReport(page: Page): Promise<void> {
  await page.evaluate(() => {
    const w = window as unknown as Record<string, any>;
    if (w.__restoreDesktopCloseReport)
      w.__restoreDesktopCloseReport();
    delete w.__restoreDesktopCloseReport;
  });
}

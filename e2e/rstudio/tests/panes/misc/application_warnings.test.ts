import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand, getPref } from '@utils/commands';
import { executeInConsole } from '@pages/console_pane.page';
import { DARK_THEME, LIGHT_THEME, expectThemeStylesheet } from '@utils/theme';
import type { Page } from '@playwright/test';

const dismissName = /^Dismiss warning \d+$/;
const shortcutMessage = 'Type shortcuts to see if they are bound to a command. Close this message bar when done.';

function warningRow(page: Page, message: string | RegExp) {
  return page.getByRole('contentinfo', { name: 'Warning bar' }).getByRole('group').filter({
    has: page.getByText(message, { exact: true }),
  });
}

// Drive two real warning producers, including the diagnostic that continuously
// updates its message. Other startup warnings may already be present.
test('application warnings coexist and dismiss independently', async ({ rstudioPage: page }) => {
  const warnings = page.getByRole('contentinfo', { name: 'Warning bar' });
  const example = warningRow(page, 'This is a warning!');
  const shortcut = warningRow(page, /^(Type shortcuts to see if they are bound to a command\.|Shortcut not bound$|debugStep : Execute Next Line$)/);
  try {
    await executeCommand(page, 'showWarningBar');
    await executeCommand(page, 'showShortcutCommand');
    await expect(example).toHaveCount(1);
    await expect(shortcut).toHaveCount(1);
    await expect(example).toBeVisible();
    await expect(shortcut).toBeVisible();

    // Updating the shortcut diagnostic must reuse its row. Assert its visible
    // message, which is updated synchronously, rather than live-region text.
    await page.keyboard.press('F8');
    await expect(shortcut.getByText('Shortcut not bound', { exact: true })).toBeVisible();
    await expect(example).toHaveCount(1);
    await expect(shortcut).toHaveCount(1);
    await expect(example).toBeVisible();

    // Dismissing the unrelated row must leave shortcut reporting active.
    // F10 is bound to debugStep on every supported platform.
    await example.getByRole('button', { name: dismissName }).click();
    await expect(example).toHaveCount(0);
    await expect(shortcut).toHaveCount(1);
    await page.keyboard.press('F10');
    await expect(shortcut.getByText('debugStep : Execute Next Line', { exact: true })).toBeVisible();
    await expect(example).toHaveCount(0);
    await expect(shortcut).toHaveCount(1);

    await shortcut.getByRole('button', { name: dismissName }).click();
    await expect(shortcut).toHaveCount(0);
    await executeCommand(page, 'showShortcutCommand');
    await expect(shortcut).toBeVisible();
    await executeCommand(page, 'showWarningBar');
    await expect(example).toBeVisible();
    await expect(example).toHaveCount(1);
    await expect(shortcut).toHaveCount(1);
  } finally {
    for (const row of [example, shortcut]) {
      const close = row.getByRole('button', { name: dismissName });
      if (await close.isVisible())
        await close.click();
    }
  }
});


for (const [theme, href] of [[LIGHT_THEME, 'textmate'], [DARK_THEME, 'cobalt']]) {
  test(`warning rows and focus outlines fit with non-overlay scrollbars (${theme})`, async ({ rstudioPage: page }) => {
    const originalTheme = await getPref(page, 'editor_theme') as string;
    const originalSize = await page.evaluate(() => ({ width: innerWidth, height: innerHeight }));
    const warnings = page.getByRole('contentinfo', { name: 'Warning bar' });
    const example = warningRow(page, 'This is a warning!');
    const shortcut = warningRow(page, shortcutMessage);
    // Custom scrollbar dimensions reserve space even on macOS, whose default
    // overlay scrollbars would otherwise hide this regression.
    const scrollbarStyle = await page.addStyleTag({ content: `
      [role="contentinfo"][aria-label="Warning bar"] { overflow-y: scroll !important; }
      [role="contentinfo"][aria-label="Warning bar"]::-webkit-scrollbar { width: 16px; height: 16px; }
      [role="contentinfo"][aria-label="Warning bar"]::-webkit-scrollbar-thumb { background: #888; }
    ` });
    try {
      await executeInConsole(page, `.rs.api.applyTheme(${JSON.stringify(theme)})`, { wait: true });
      await expectThemeStylesheet(page, href);
      await page.setViewportSize({ width: 500, height: 700 });
      await executeCommand(page, 'showWarningBar');
      await executeCommand(page, 'showShortcutCommand');
      await expect(example).toBeVisible();
      await expect(shortcut).toBeVisible();
      await expect.poll(() => warnings.evaluate(el => (el as HTMLElement).offsetWidth - el.clientWidth)).toBeGreaterThanOrEqual(16);
      await expect.poll(() => warnings.evaluate(el => el.scrollWidth - el.clientWidth)).toBeLessThanOrEqual(0);

      for (const row of [example, shortcut]) {
        await row.scrollIntoViewIfNeeded();
        const bounds = await warnings.evaluate(el => {
          const rect = el.getBoundingClientRect();
          return { left: rect.left, top: rect.top, right: rect.left + el.clientWidth, bottom: rect.top + el.clientHeight };
        });
        for (const part of [row.locator('td').nth(1), row.getByRole('button', { name: dismissName })]) {
          const rect = await part.boundingBox();
          expect(rect).not.toBeNull();
          expect(rect!.x).toBeGreaterThanOrEqual(bounds.left);
          expect(rect!.x + rect!.width).toBeLessThanOrEqual(bounds.right);
          expect(rect!.y).toBeGreaterThanOrEqual(bounds.top);
          expect(rect!.y + rect!.height).toBeLessThanOrEqual(bounds.bottom);
        }
      }
      // Keyboard focus outlines must also fit, not just the button rectangles.
      const close = example.getByRole('button', { name: dismissName });
      await close.focus();
      await page.keyboard.press('Shift');
      const ring = await close.evaluate(el => {
        const style = getComputedStyle(el);
        const rect = el.getBoundingClientRect();
        const cell = el.closest('td')!.getBoundingClientRect();
        const outset = Math.max(0, parseFloat(style.outlineWidth) + parseFloat(style.outlineOffset));
        return { width: parseFloat(style.outlineWidth), left: rect.left - outset, right: rect.right + outset, cellLeft: cell.left, cellRight: cell.right };
      });
      expect(ring.width).toBeGreaterThan(0);
      expect(ring.left).toBeGreaterThanOrEqual(ring.cellLeft);
      expect(ring.right).toBeLessThanOrEqual(ring.cellRight);
      await executeCommand(page, 'showShortcutCommand');
      // The long diagnostic is clipped inside its message cell, not by a
      // horizontal scrollbar across the entire warning row.
      const message = shortcut.locator('td').nth(1);
      expect(await message.evaluate(el => el.scrollWidth > el.clientWidth)).toBe(true);
    } finally {
      for (const row of [example, shortcut]) {
        const close = row.getByRole('button', { name: dismissName });
        if (await close.isVisible())
          await close.click();
      }
      await scrollbarStyle.evaluate(el => el.parentNode?.removeChild(el));
      await page.setViewportSize(originalSize);
      await executeInConsole(page, `.rs.api.applyTheme(${JSON.stringify(originalTheme)})`, { wait: true });
    }
  });
}


test('warnings follow the workbench and retain keyboard focus while dismissing', async ({ rstudioPage: page }) => {
  const warnings = page.getByRole('contentinfo', { name: 'Warning bar' });
  // This fixture uses a disposable profile. Clear startup warnings to exercise
  // dismissal of the final row as well as next/previous row focus.
  while (await warnings.getByRole('button', { name: dismissName }).count())
    await warnings.getByRole('button', { name: dismissName }).last().click();
  try {
    await executeCommand(page, 'showWarningBar');
    await executeCommand(page, 'showShortcutCommand');
    const rows = warnings.getByRole('group');
    await expect(rows).toHaveCount(2);
    expect(await warnings.evaluate(el => {
      const main = document.querySelector('[role="main"][aria-label="Workbench"]')!;
      return !!(main.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING);
    })).toBe(true);

    const firstName = await rows.nth(0).getAttribute('aria-label');
    const secondName = await rows.nth(1).getAttribute('aria-label');
    expect(firstName).not.toBe(secondName);
    await rows.first().getByRole('button', { name: dismissName }).focus();
    await page.keyboard.press('Space');
    await expect(rows).toHaveCount(1);
    await expect(rows.first().getByRole('button', { name: dismissName })).toBeFocused();

    // Reopening the example appends it; removing it must focus the previous row.
    await executeCommand(page, 'showWarningBar');
    await rows.last().getByRole('button', { name: dismissName }).focus();
    await page.keyboard.press('Space');
    await expect(rows).toHaveCount(1);
    await expect(rows.first().getByRole('button', { name: dismissName })).toBeFocused();
    await page.keyboard.press('Space');
    await expect(rows).toHaveCount(0);
    await expect.poll(() => page.evaluate(() => !!document.activeElement?.closest('[role="main"]'))).toBe(true);
    await expect(page.locator('.ace_editor.ace_focus')).toHaveCount(1);
  } finally {
    while (await warnings.getByRole('button', { name: dismissName }).count())
      await warnings.getByRole('button', { name: dismissName }).last().click();
  }
});


test('clipped warning details are keyboard accessible and return focus', { tag: '@desktop_only' }, async ({ rstudioPage: page }) => {
  const originalSize = await page.evaluate(() => ({ width: innerWidth, height: innerHeight }));
  const message = 'This license warning contains important details that must remain readable when the window is narrow or the text is enlarged.';
  const row = warningRow(page, message);
  // Use the real desktop license-warning producer, which remains stable while
  // typing (unlike the shortcut diagnostic). This fixture has a disposable profile.
  const showLicenseWarning = (text: string) => page.evaluate(value => {
    const hooks = (window as typeof window & {
      desktopHooks: { updateLicenseWarningBar(message: string): void };
    }).desktopHooks;
    hooks.updateLicenseWarningBar(value);
  }, text);
  const largeText = await page.addStyleTag({ content: '[role="contentinfo"] span[title] { font-size: 200%; }' });
  try {
    await page.setViewportSize({ width: 500, height: 700 });
    await showLicenseWarning(message);
    await expect.poll(() => row.locator('td').nth(1).evaluate(el => el.scrollWidth > el.clientWidth)).toBe(true);
    const details = row.getByRole('button', { name: /^Details for warning / });
    await details.focus();
    await page.keyboard.press('Enter');
    const dialog = page.getByRole('alertdialog', { name: 'Warning details' });
    await expect(dialog).toBeVisible();
    await expect(dialog).toHaveAccessibleDescription(message);
    await expect(dialog.getByText(message, { exact: true })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0);
    await expect(details).toBeFocused();
  } finally {
    await page.keyboard.press('Escape');
    await showLicenseWarning('');
    await largeText.evaluate(el => el.parentNode?.removeChild(el));
    await page.setViewportSize(originalSize);
  }
});


test('nearby warnings share one polite announcement outside their rows', async ({ rstudioPage: page }) => {
  const example = warningRow(page, 'This is a warning!');
  const shortcut = warningRow(page, shortcutMessage);
  try {
    await executeCommand(page, 'showWarningBar');
    await executeCommand(page, 'showShortcutCommand');
    const announcement = page.getByRole('status').filter({ hasText: 'This is a warning!' });
    await expect(announcement).toHaveCount(1);
    await expect(announcement).toContainText(shortcutMessage);
    expect(await announcement.evaluate(el => el.closest('[role="contentinfo"]') === null)).toBe(true);
    for (const row of [example, shortcut]) {
      await expect(row.getByRole('alert')).toHaveCount(0);
      await expect(row.locator('span[title]')).not.toHaveAttribute('aria-hidden', 'true');
      await expect(row.getByRole('button', { name: dismissName })).toHaveAccessibleDescription(await row.locator('span[title]').innerText());
    }
  } finally {
    for (const row of [example, shortcut])
      if (await row.isVisible())
        await row.getByRole('button', { name: dismissName }).click();
  }
});

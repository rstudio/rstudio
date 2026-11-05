import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand } from '@utils/commands';

// Drive two real warning producers, including the diagnostic that continuously
// updates its message. Other startup warnings may already be present.
test('application warnings coexist and dismiss independently', async ({ rstudioPage: page }) => {
  const warnings = page.getByRole('contentinfo', { name: 'Warning bar' });
  const example = warnings.getByRole('group', { name: 'This is a warning!', exact: true });
  const shortcut = warnings.getByRole('group', {
    name: /^(Type shortcuts to see if they are bound to a command\.|Shortcut not bound$|debugStep : Execute Next Line$)/,
  });
  try {
    await executeCommand(page, 'showWarningBar');
    await executeCommand(page, 'showShortcutCommand');
    await expect(example).toHaveCount(1);
    await expect(shortcut).toHaveCount(1);
    await expect(example).toBeVisible();
    await expect(shortcut).toBeVisible();

    // Updating the shortcut diagnostic must reuse its row. Assert the group
    // name, which is updated synchronously, rather than delayed live-region text.
    await page.keyboard.press('F8');
    await expect(shortcut).toHaveAccessibleName('Shortcut not bound');
    await expect(example).toHaveCount(1);
    await expect(shortcut).toHaveCount(1);
    await expect(example).toBeVisible();

    // Dismissing the unrelated row must leave shortcut reporting active.
    // F10 is bound to debugStep on every supported platform.
    await example.getByRole('button', { name: 'Dismiss Warning Bar' }).click();
    await expect(example).toHaveCount(0);
    await expect(shortcut).toHaveCount(1);
    await page.keyboard.press('F10');
    await expect(shortcut).toHaveAccessibleName('debugStep : Execute Next Line');
    await expect(example).toHaveCount(0);
    await expect(shortcut).toHaveCount(1);

    await shortcut.getByRole('button', { name: 'Dismiss Warning Bar' }).click();
    await expect(shortcut).toHaveCount(0);
    await executeCommand(page, 'showShortcutCommand');
    await expect(shortcut).toBeVisible();
    await executeCommand(page, 'showWarningBar');
    await expect(example).toBeVisible();
    await expect(example).toHaveCount(1);
    await expect(shortcut).toHaveCount(1);
  } finally {
    for (const row of [example, shortcut]) {
      const close = row.getByRole('button', { name: 'Dismiss Warning Bar' });
      if (await close.isVisible())
        await close.click();
    }
  }
});


test('warning rows fit with non-overlay scrollbars', async ({ rstudioPage: page }) => {
  const originalSize = await page.evaluate(() => ({ width: innerWidth, height: innerHeight }));
  const warnings = page.getByRole('contentinfo', { name: 'Warning bar' });
  const example = warnings.getByRole('group', { name: 'This is a warning!', exact: true });
  const shortcut = warnings.getByRole('group', { name: /^Type shortcuts to see if they are bound to a command\./ });
  // Custom scrollbar dimensions reserve space even on macOS, whose default
  // overlay scrollbars would otherwise hide this regression.
  const scrollbarStyle = await page.addStyleTag({ content: `
    [role="contentinfo"][aria-label="Warning bar"] { overflow-y: scroll !important; }
    [role="contentinfo"][aria-label="Warning bar"]::-webkit-scrollbar { width: 16px; height: 16px; }
    [role="contentinfo"][aria-label="Warning bar"]::-webkit-scrollbar-thumb { background: #888; }
  ` });
  try {
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
      for (const part of [row.locator('td').nth(1), row.getByRole('button', { name: 'Dismiss Warning Bar' })]) {
        const rect = await part.boundingBox();
        expect(rect).not.toBeNull();
        expect(rect!.x).toBeGreaterThanOrEqual(bounds.left);
        expect(rect!.x + rect!.width).toBeLessThanOrEqual(bounds.right);
        expect(rect!.y).toBeGreaterThanOrEqual(bounds.top);
        expect(rect!.y + rect!.height).toBeLessThanOrEqual(bounds.bottom);
      }
    }
    // The long diagnostic is clipped inside its message cell, not by a
    // horizontal scrollbar across the entire warning row.
    const message = shortcut.locator('td').nth(1);
    expect(await message.evaluate(el => el.scrollWidth > el.clientWidth)).toBe(true);
  } finally {
    for (const row of [example, shortcut]) {
      const close = row.getByRole('button', { name: 'Dismiss Warning Bar' });
      if (await close.isVisible())
        await close.click();
    }
    await scrollbarStyle.evaluate(el => el.parentNode?.removeChild(el));
    await page.setViewportSize(originalSize);
  }
});

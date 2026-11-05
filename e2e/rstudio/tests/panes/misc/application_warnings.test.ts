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

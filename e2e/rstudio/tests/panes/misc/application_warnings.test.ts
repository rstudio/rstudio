import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand } from '@utils/commands';

// Drive two real warning producers, including the diagnostic that continuously
// updates its message. No production-only automation hooks are needed.
test('application warnings coexist and dismiss independently', async ({ rstudioPage: page }) => {
  const warnings = page.getByRole('contentinfo', { name: 'Warning bar' });
  const dismiss = warnings.getByRole('button', { name: 'Dismiss Warning Bar' });
  try {
    await executeCommand(page, 'showWarningBar');
    await executeCommand(page, 'showShortcutCommand');
    await expect(dismiss).toHaveCount(2);
    const example = warnings.getByRole('group', { name: 'This is a warning!', exact: true });
    await expect(example).toBeVisible();

    // Updating the shortcut diagnostic must reuse its row.
    await page.keyboard.press('F8');
    await expect(dismiss).toHaveCount(2);
    await expect(example).toBeVisible();

    // Dismissing the unrelated row must leave shortcut reporting active.
    await example.getByRole('button', { name: 'Dismiss Warning Bar' }).click();
    await expect(dismiss).toHaveCount(1);
    const previous = await warnings.innerText();
    await page.keyboard.press('Control+Shift+F10');
    await expect.poll(() => warnings.innerText()).not.toBe(previous);
    await expect(dismiss).toHaveCount(1);

    await dismiss.click();
    await expect(warnings).toBeHidden();
    await executeCommand(page, 'showShortcutCommand');
    await expect(dismiss).toHaveCount(1);
    await executeCommand(page, 'showWarningBar');
    await expect(dismiss).toHaveCount(2);
  } finally {
    while (await dismiss.count())
      await dismiss.first().click();
  }
});

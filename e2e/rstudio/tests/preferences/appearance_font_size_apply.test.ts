import { test, expect } from '@fixtures/rstudio.fixture';
import { clearPref, dismissAllModals, getPref, setPref } from '@utils/commands';
import {
  APPEARANCE_PANEL,
  APPEARANCE_TAB,
  DIALOG_BOX,
  OPTIONS_APPLY,
  OPTIONS_OK,
  openGlobalOptions,
} from '@pages/global_options.page';
import type { Page } from 'playwright';

// The Appearance pane only writes a font size when it differs from the value it
// last applied. Apply after a help font size change must record that value as
// the help baseline, or a later OK compares against the wrong baseline and
// drops the edit (#19222).

// LayoutGrid associates its label with the row rather than the input, so
// locate the numeric input immediately following the visible label.
const EDITOR_FONT_SIZE_INPUT =
  "xpath=.//*[contains(text(),'Editor font size:')]/following::input[1]";
const HELP_FONT_SIZE_INPUT =
  "xpath=.//*[contains(text(),'Help font size:')]/following::input[1]";

async function openAppearance(page: Page) {
  await openGlobalOptions(page);
  await page.locator(APPEARANCE_TAB).click();
  const panel = page.locator(APPEARANCE_PANEL);
  await expect(panel).toBeVisible();
  return panel;
}

async function applyAndWaitForSave(page: Page): Promise<void> {
  const saved = page.waitForResponse((response) => response.url().includes('set_user_prefs'));
  await page.locator(OPTIONS_APPLY).click();
  const response = await saved;
  expect(response.ok()).toBe(true);
  expect(await response.json()).not.toHaveProperty('error');
}

async function clickOkAndWaitForClose(page: Page): Promise<void> {
  await page.locator(OPTIONS_OK).click();
  await expect(page.locator(OPTIONS_OK)).toBeHidden({ timeout: 10000 });
}

test.describe('Appearance font sizes after Apply', () => {
  test.beforeEach(async ({ rstudioPage: page }) => {
    await setPref(page, 'font_size_points', 10);
    await setPref(page, 'help_font_size_points', 10);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await dismissAllModals(page);
    await page.waitForSelector(DIALOG_BOX, { state: 'detached', timeout: 10000 });
    await clearPref(page, 'font_size_points');
    await clearPref(page, 'help_font_size_points');
  });

  test('help font size reverted after Apply is saved on OK', async ({ rstudioPage: page }) => {
    const panel = await openAppearance(page);
    const helpFontSize = panel.locator(HELP_FONT_SIZE_INPUT);

    await helpFontSize.fill('12');
    await applyAndWaitForSave(page);
    expect(await getPref(page, 'help_font_size_points')).toBe(12);

    await helpFontSize.fill('10');
    await clickOkAndWaitForClose(page);
    await expect.poll(() => getPref(page, 'help_font_size_points')).toBe(10);
  });

  test('editor font size matching the applied help size is saved on OK', async ({
    rstudioPage: page,
  }) => {
    const panel = await openAppearance(page);

    await panel.locator(HELP_FONT_SIZE_INPUT).fill('12');
    await applyAndWaitForSave(page);

    await panel.locator(EDITOR_FONT_SIZE_INPUT).fill('12');
    await clickOkAndWaitForClose(page);
    await expect.poll(() => getPref(page, 'font_size_points')).toBe(12);
  });
});

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { clearPref, dismissAllModals, setPref } from '@utils/commands';
import { CONFIRM_BTN } from '@pages/modals.page';
import {
  CONSOLE_TAB,
  DIALOG_BOX,
  OPTIONS_APPLY,
  OPTIONS_OK,
  openGlobalOptions,
  closeGlobalOptions,
} from '@pages/global_options.page';

// LayoutGrid associates its label with the composite widget rather than its
// input, so locate the numeric input immediately following the visible label.
const MAX_LINES_INPUT =
  "xpath=//label[contains(text(),'Maximum lines of output to keep in the console')]/following::input[1]";

test.describe('Console scrollback limit validation', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await dismissAllModals(page);
    await page.waitForSelector(DIALOG_BOX, { state: 'detached', timeout: 10000 });
    await clearPref(page, 'console_max_lines');
  });

  for (const [buttonName, buttonSelector] of [
    ['Apply', OPTIONS_APPLY],
    ['OK', OPTIONS_OK],
  ]) {
    test(`${buttonName} rejects fewer than 10 console lines and accepts the minimum`, async ({
      rstudioPage: page,
    }) => {
      const consoleActions = new ConsolePaneActions(page);
      await setPref(page, 'console_max_lines', 5000);
      await openGlobalOptions(page);
      await page.locator(CONSOLE_TAB).click();

      const maxLines = page.locator(MAX_LINES_INPUT);
      await maxLines.fill('9');
      await page.locator(buttonSelector).click();

      // The numeric widget's bounds must block the actual apply path, not
      // merely add an HTML min attribute that the dialog ignores.
      const errorOk = page.locator(CONFIRM_BTN);
      await expect(errorOk).toBeVisible();
      // The message must name the field: the pane has three numeric inputs.
      await expect(
        page.getByText('Maximum lines of output to keep in the console: must be greater than or equal to 10.'),
      ).toBeVisible();
      await expect(page.locator(OPTIONS_OK)).toBeVisible();
      await errorOk.click();
      await closeGlobalOptions(page);

      // Read the session's preference to verify the rejected value was not
      // persisted, even if the dialog's client model still looks unchanged.
      expect(await consoleActions.evalRLogical(
        '.rs.api.readRStudioPreference("console_max_lines") == 5000',
      )).toBe(true);

      await openGlobalOptions(page);
      await page.locator(CONSOLE_TAB).click();
      await expect(maxLines).toHaveValue('5000');
      await maxLines.fill('10');
      const saved = page.waitForResponse((response) => response.url().includes('set_user_prefs'));
      await page.locator(buttonSelector).click();
      const response = await saved;
      expect(response.ok()).toBe(true);
      expect(await response.json()).not.toHaveProperty('error');
      if (buttonName === 'Apply')
        await closeGlobalOptions(page);
      else
        await expect(page.locator(OPTIONS_OK)).toBeHidden();

      expect(await consoleActions.evalRLogical(
        '.rs.api.readRStudioPreference("console_max_lines") == 10',
      )).toBe(true);
    });
  }
});

import { test, expect } from '@fixtures/rstudio.fixture';
import { dismissAllModals } from '@utils/commands';
import {
  DIALOG_BOX,
  openGlobalOptions,
  closeGlobalOptions,
  APPEARANCE_TAB,
  APPEARANCE_PANEL,
  APPEARANCE_PREVIEW,
  CODE_TAB,
  CODE_EDITING_TAB,
  CODE_EDITING_PANEL,
  CODE_DISPLAY_TAB,
  CODE_DISPLAY_PANEL,
  CODE_SAVING_TAB,
  CODE_SAVING_PANEL,
  CODE_CHANGE_ENCODING_BTN,
  CODE_CHANGE_ENCODING_MODAL,
  CODE_COMPLETION_TAB,
  CODE_COMPLETION_PANEL,
  CODE_DIAGNOSTICS_TAB,
  CODE_DIAGNOSTICS_PANEL,
  GENERAL_TAB,
  GENERAL_PANEL,
  GENERAL_ADVANCED_TAB,
  GENERAL_ADVANCED_PANEL,
  PACKAGES_TAB,
  PACKAGES_PANEL,
  PACKAGES_MANAGEMENT_TAB,
  PACKAGES_MANAGEMENT_PANEL,
  PACKAGES_DEVELOPMENT_TAB,
  PACKAGES_DEVELOPMENT_PANEL,
  PANE_LAYOUT_TAB,
  PANE_LAYOUT_PANEL,
  RMARKDOWN_TAB,
  RMARKDOWN_PANEL,
  SPELLING_TAB,
  SPELLING_PANEL,
  SWEAVE_TAB,
  SWEAVE_PANEL,
  TERMINAL_TAB,
  TERMINAL_PANEL,
  TERMINAL_GENERAL_TAB,
  TERMINAL_GENERAL_PANEL,
  TERMINAL_CLOSING_TAB,
  TERMINAL_CLOSING_PANEL,
  PYTHON_TAB,
  PYTHON_PANEL,
  PYTHON_INTERPRETER_PATH,
  PYTHON_INTERPRETER_SELECT_BTN,
  PYTHON_INTERPRETERS_MODAL,
  ASSISTANT_TAB,
  ASSISTANT_PANEL,
  ASSISTANT_LABEL,
} from '@pages/global_options.page';

test.describe('Global Options panels', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    if (await page.locator(DIALOG_BOX).count() > 0) {
      await dismissAllModals(page);
      await page.waitForSelector(DIALOG_BOX, { state: 'detached', timeout: 10000 });
    }
  });

  test('Appearance panel displays editor theme preview', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(APPEARANCE_TAB).click();
    await expect(page.locator(APPEARANCE_PANEL)).toBeVisible();
    await expect(page.locator(APPEARANCE_PREVIEW)).toBeVisible();
    await closeGlobalOptions(page);
  });

  test('Code sub-panels are all accessible', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(CODE_TAB).click();

    await expect(page.locator(CODE_EDITING_TAB)).toBeVisible();
    await expect(page.locator(CODE_DISPLAY_TAB)).toBeVisible();
    await expect(page.locator(CODE_SAVING_TAB)).toBeVisible();
    await expect(page.locator(CODE_COMPLETION_TAB)).toBeVisible();
    await expect(page.locator(CODE_DIAGNOSTICS_TAB)).toBeVisible();

    await expect(page.locator(CODE_EDITING_PANEL)).toBeVisible();
    await expect(page.getByLabel('Auto-detect code indentation')).not.toBeChecked();

    await page.locator(CODE_DISPLAY_TAB).click();
    await expect(page.locator(CODE_DISPLAY_PANEL)).toBeVisible();

    await page.locator(CODE_SAVING_TAB).click();
    await expect(page.locator(CODE_SAVING_PANEL)).toBeVisible();

    await page.locator(CODE_COMPLETION_TAB).click();
    await expect(page.locator(CODE_COMPLETION_PANEL)).toBeVisible();

    await page.locator(CODE_DIAGNOSTICS_TAB).click();
    await expect(page.locator(CODE_DIAGNOSTICS_PANEL)).toBeVisible();

    await closeGlobalOptions(page);
  });

  test('Code saving change encoding modal opens', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(CODE_TAB).click();
    await page.locator(CODE_SAVING_TAB).click();
    await expect(page.locator(CODE_CHANGE_ENCODING_BTN)).toBeVisible();
    await page.locator(CODE_CHANGE_ENCODING_BTN).click();
    await expect(page.locator(CODE_CHANGE_ENCODING_MODAL)).toBeVisible();
    await page.keyboard.press('Escape');
    await page.waitForSelector(CODE_CHANGE_ENCODING_MODAL, { state: 'detached', timeout: 10000 });
    await closeGlobalOptions(page);
  });

  test('General panel and Advanced sub-panel are accessible', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(GENERAL_TAB).click();
    await expect(page.locator(GENERAL_PANEL)).toBeVisible();
    await page.locator(GENERAL_ADVANCED_TAB).click();
    await expect(page.locator(GENERAL_ADVANCED_PANEL)).toBeVisible();
    await closeGlobalOptions(page);
  });

  test('Packages sub-panels are accessible', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(PACKAGES_TAB).click();
    await expect(page.locator(PACKAGES_PANEL)).toBeVisible();
    await page.locator(PACKAGES_MANAGEMENT_TAB).click();
    await expect(page.locator(PACKAGES_MANAGEMENT_PANEL)).toBeVisible();
    await page.locator(PACKAGES_DEVELOPMENT_TAB).click();
    await expect(page.locator(PACKAGES_DEVELOPMENT_PANEL)).toBeVisible();
    await closeGlobalOptions(page);
  });

  test('Pane Layout panel is accessible', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(PANE_LAYOUT_TAB).click();
    await expect(page.locator(PANE_LAYOUT_PANEL)).toBeVisible();
    await closeGlobalOptions(page);
  });

  test('R Markdown panel is accessible', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(RMARKDOWN_TAB).click();
    await expect(page.locator(RMARKDOWN_PANEL)).toBeVisible();
    await closeGlobalOptions(page);
  });

  test('Spelling panel is accessible', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(SPELLING_TAB).click();
    await expect(page.locator(SPELLING_PANEL)).toBeVisible();
    await closeGlobalOptions(page);
  });

  test('Sweave panel is accessible', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(SWEAVE_TAB).click();
    await expect(page.locator(SWEAVE_PANEL)).toBeVisible();
    await closeGlobalOptions(page);
  });

  test('Terminal sub-panels are accessible', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(TERMINAL_TAB).click();
    await expect(page.locator(TERMINAL_PANEL)).toBeVisible();
    await page.locator(TERMINAL_GENERAL_TAB).click();
    await expect(page.locator(TERMINAL_GENERAL_PANEL)).toBeVisible();
    await page.locator(TERMINAL_CLOSING_TAB).click();
    await expect(page.locator(TERMINAL_CLOSING_PANEL)).toBeVisible();
    await closeGlobalOptions(page);
  });

  test('Python panel and interpreter selector are accessible', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(PYTHON_TAB).click();
    await expect(page.locator(PYTHON_PANEL)).toBeVisible();
    await expect(page.locator(PYTHON_INTERPRETER_PATH)).toBeVisible();
    await expect(page.locator(PYTHON_INTERPRETER_SELECT_BTN)).toBeVisible();
    await page.locator(PYTHON_INTERPRETER_SELECT_BTN).click();
    await expect(page.locator(PYTHON_INTERPRETERS_MODAL)).toBeVisible({ timeout: 15000 });
    await page.keyboard.press('Escape');
    await page.waitForSelector(PYTHON_INTERPRETERS_MODAL, { state: 'detached', timeout: 10000 });
    await closeGlobalOptions(page);
  });

  test('Assistant panel displays code assistant configuration', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(ASSISTANT_TAB).click();
    await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();
    await expect(page.locator(ASSISTANT_PANEL).getByText(ASSISTANT_LABEL)).toBeVisible();
    await closeGlobalOptions(page);
  });
});

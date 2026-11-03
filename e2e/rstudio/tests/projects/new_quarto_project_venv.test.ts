/**
 * New Quarto Project page: with the Jupyter engine, the venv/condaenv row's
 * packages box sits beside the checkboxes, whose captions are their own
 * labels, so the box needs a name of its own (#19381). The row is shown only
 * when Quarto reports a Python able to create a venv or conda environment.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { resetSourcePaneState } from '@utils/commands';

const PALETTE_LIST = '#rstudio_command_palette_list';
const WIZARD_DIALOG = '.gwt-DialogBox[aria-label="New Project Wizard"]';
const NEW_DIRECTORY_OPTION = '#rstudio_label_new_directory_wizard_page';
const QUARTO_PROJECT_OPTION = '#rstudio_label_quarto_project_wizard_page';
const CREATE_PROJECT_BTN = '#rstudio_label_create_project_wizard_confirm';
// Prefix match: dialog button ids gain a numeric suffix when several exist
const CANCEL_BTN = "[id^='rstudio_dlg_cancel']";
const VENV_PACKAGES = '#rstudio_quarto_project_use_venv_packages';

test.describe('New Quarto Project venv packages', () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    // Pre-warm quarto, as create_projects does: its first run on a cold
    // runner can outlast the page's 30s capabilities timeout, which would
    // leave the venv row hidden.
    test.setTimeout(300000);
    await executeInConsole(
      page,
      'invisible(try(suppressWarnings(' +
        'system2("quarto", "--version", stdout = TRUE, stderr = TRUE)' +
        '), silent = TRUE))',
      { wait: true, timeout: 180000 },
    );
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    const dialog = page.locator(WIZARD_DIALOG);
    if (await dialog.isVisible()) {
      await dialog.locator(CANCEL_BTN).click();
      await expect(dialog).not.toBeVisible({ timeout: 5000 });
    }
  });

  test('the packages box is named', async ({ rstudioPage: page }) => {
    await resetSourcePaneState(page);

    // The palette, not executeCommand('newProject'), which can block the R
    // thread behind an "R session is busy" modal (see create_projects).
    await page.keyboard.press('ControlOrMeta+Shift+p');
    await expect(page.locator(PALETTE_LIST)).toBeVisible({ timeout: 5000 });
    await page.keyboard.type('Create a New Project');
    const item = page.locator(`${PALETTE_LIST} >> text=Create a New Project...`);
    await expect(item).toBeVisible({ timeout: 5000 });
    await item.click();

    // get_new_project_context probes the build toolchain before the wizard
    // shows; slow on a cold runner.
    const dialog = page.locator(WIZARD_DIALOG);
    await expect(dialog).toBeVisible({ timeout: 90000 });
    await dialog.locator(NEW_DIRECTORY_OPTION).click();

    // The page requests capabilities once its slide-in finishes. Whether the
    // venv row should show comes from this response, so a failed request
    // fails the test instead of passing for "no Python".
    const capsResponse = page.waitForResponse(
      (r) => r.url().includes('/rpc/quarto_capabilities'), { timeout: 60000 });
    await dialog.locator(QUARTO_PROJECT_OPTION).click();
    const caps = await (await capsResponse).json();
    expect(caps.error, 'quarto_capabilities returned an error').toBeUndefined();
    const python = caps.result?.python;
    test.skip(
      python?.venv !== true && python?.conda !== true,
      'Quarto reports no Python that can create a venv or conda environment',
    );

    // Visible as well as enabled: the button is hidden but enabled while the
    // page slides in, and is disabled until the page has applied the response.
    const createBtn = page.locator(CREATE_PROJECT_BTN);
    await expect(createBtn).toBeVisible({ timeout: 10000 });
    await expect(createBtn).toBeEnabled({ timeout: 10000 });

    // Every Quarto page (project, website, blog, ...) is in the DOM; only the
    // active one is visible.
    await dialog.getByLabel('Engine:', { exact: true }).filter({ visible: true })
      .selectOption({ label: 'Jupyter' });

    const venvPackages = dialog.locator(VENV_PACKAGES);
    await expect(venvPackages).toBeVisible();
    await expect(venvPackages).toHaveAccessibleName('Packages to install in the environment');
  });
});

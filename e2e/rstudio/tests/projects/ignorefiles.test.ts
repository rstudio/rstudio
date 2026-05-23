import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox, SANDBOX_DIR_PREFIX } from '@utils/sandbox';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { setPref } from '@utils/commands';
import { closeProjectIfOpen, waitForConsoleIdle } from '@utils/project';
import * as fs from 'fs';
import type { Page } from 'playwright';

const PALETTE_LIST = '#rstudio_command_palette_list';
const WIZARD_DIALOG = '.gwt-DialogBox[aria-label="New Project Wizard"]';
const NEW_DIRECTORY_OPTION = '#rstudio_label_new_directory_wizard_page';
const NEW_PROJECT_OPTION = '#rstudio_label_new_project_wizard_page';
const PROJECT_NAME_INPUT = '#rstudio_new_project_directory_name';
const GIT_CHECKBOX = '#rstudio_new_project_git_repo input';
const CREATE_PROJECT_BTN = '#rstudio_label_create_project_wizard_confirm';
const PROJECT_MENU = '#rstudio_project_menubutton_toolbar';

async function captureResult(page: Page, rExpression: string): Promise<string> {
  const marker = `__IG_${Date.now()}__`;
  await executeInConsole(page, `cat("${marker}", ${rExpression}, "${marker}")`, { wait: true });
  const pattern = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`);
  const text = await page.locator(CONSOLE_OUTPUT).innerText();
  const match = text.match(pattern);
  if (!match) throw new Error(`captureResult: markers not found for "${rExpression}"`);
  return match[1].trim();
}

test.describe('Project ignore files', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let originalDefaultProjectLocation = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);

    // Close any project from a prior crashed run so the wizard isn't blocked.
    await closeProjectIfOpen(page);

    // The wizard's parent-directory field is read-only; redirect it into the
    // sandbox by overriding the default_project_location preference.
    const current = await captureResult(
      page,
      '.rs.api.readRStudioPreference("default_project_location")',
    );
    const basename = current.split(/[/\\]/).pop() ?? '';
    originalDefaultProjectLocation = basename.startsWith(SANDBOX_DIR_PREFIX) ? '' : current;

    await setPref(page, 'default_project_location', sandbox.dir);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    try {
      await closeProjectIfOpen(page);
      await setPref(page, 'default_project_location', originalDefaultProjectLocation);
    } catch (err) {
      console.warn('ignorefiles afterAll cleanup failed:', err);
    }
  });

  // https://github.com/rstudio/rstudio/commit/89f6cef5d8
  test('.positai is added to .gitignore only after the directory exists', async ({ rstudioPage: page }) => {
    const projectName = 'PositaiIgnoreTest';
    const projectDir = `${sandbox.dir}/${projectName}`.replace(/\\/g, '/');
    const gitignorePath = `${projectDir}/.gitignore`;

    // Open New Project wizard via the command palette (executeCommand blocks
    // on an "R session is busy" modal on some platforms). Wait for the
    // palette list to render before typing -- the keystrokes get dropped if
    // the palette hasn't mounted yet.
    await page.keyboard.press('ControlOrMeta+Shift+p');
    await expect(page.locator(PALETTE_LIST)).toBeVisible({ timeout: 5000 });
    await page.keyboard.type('Create a New Project');
    const paletteItem = page.locator(`${PALETTE_LIST} >> text=Create a New Project...`);
    await expect(paletteItem).toBeVisible({ timeout: 5000 });
    await paletteItem.click();
    await expect(page.locator(WIZARD_DIALOG)).toBeVisible({ timeout: 15000 });

    // Each wizard step's click auto-waits for the next panel to render before
    // the next click is dispatched.
    await page.locator(NEW_DIRECTORY_OPTION).click();
    await page.locator(NEW_PROJECT_OPTION).click();

    const nameInput = page.locator(PROJECT_NAME_INPUT);
    await nameInput.click();
    // pressSequentially fires GWT key events that enable the Create button.
    // Wait for the input to actually hold the typed text before checking the
    // git checkbox below.
    await nameInput.pressSequentially(projectName);
    await expect(nameInput).toHaveValue(projectName, { timeout: 2000 });

    await page.locator(GIT_CHECKBOX).check();

    await page.locator(CREATE_PROJECT_BTN).click();
    await expect(page.locator(WIZARD_DIALOG)).not.toBeVisible({ timeout: 15000 });

    await expect(page.locator(PROJECT_MENU)).toContainText(projectName, {
      timeout: TIMEOUTS.sessionRestart,
    });
    await waitForConsoleIdle(page);

    // Before the fix, .positai was added unconditionally at project open. Now
    // it should appear only after the directory exists.
    await expect.poll(() => fs.existsSync(gitignorePath), {
      timeout: TIMEOUTS.consoleReady,
    }).toBe(true);
    expect(fs.readFileSync(gitignorePath, 'utf8').split('\n')).not.toContain('.positai');

    // Create .positai via rsession so the file monitor picks it up.
    await consoleActions.executeInConsole(`dir.create("${projectDir}/.positai")`);

    await expect
      .poll(() => fs.readFileSync(gitignorePath, 'utf8').split('\n').includes('.positai'), {
        timeout: TIMEOUTS.consoleReady,
      })
      .toBe(true);
  });
});

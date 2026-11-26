import { test, expect } from '@fixtures/rstudio.fixture';
import {
  clearPref,
  dismissAllModals,
  executeCommand,
  openProject,
  setChatUpdateCheckOverride,
  setPref,
} from '@utils/commands';
import { closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeInConsoleChecked } from '@pages/console_pane.page';
import { CONFIRM_BTN, YES_BTN, NO_BTN } from '@pages/modals.page';
import type { Page } from 'playwright';

// rstudio/rstudio#19803. An install of Posit Assistant is allowed only when it
// is selected as the chat provider or the code assistant, and the code
// assistant half of that answers from the project's setting before the global
// preference. "Check for Posit Assistant Updates" offers an install only when
// that holds, so with the chat provider off its dialog shows which assistant
// setting won. The check's outcome is driven by the update-check override, as
// in check-for-updates-command.test.ts; no install is started.

const CHECK_UPDATES_CMD = 'checkForPositAssistantUpdates';
const DIALOG = '.gwt-DialogBox';

const UPDATE_AVAILABLE = {
  updateAvailable: true,
  reinstallAvailable: false,
  isDowngrade: false,
  noCompatibleVersion: false,
  unsupportedInstalledVersion: false,
  unsupportedProtocol: false,
  manifestUnavailable: false,
  errorMessage: '',
  currentVersion: '1.0.0',
  newVersion: '2.0.0',
  downloadUrl: '',
  isInitialInstall: false,
};

test.describe.serial(
  'Check for Posit Assistant updates under a project assistant -- #19803',
  { tag: ['@ai', '@chat', '@serial'] },
  () => {
    const sandbox = useSuiteSandbox();

    // Create a project whose options set its code assistant. The value is
    // seeded in the scratch-path options file before the project opens, so the
    // session reads it at startup; setting it through Project Options would
    // raise a restart prompt.
    async function seedProject(page: Page, name: string, assistant: string): Promise<string> {
      const projectDir = `${sandbox.dir.replace(/\\/g, '/')}/${name}`;
      const rproj = `${projectDir}/${name}.Rproj`;
      await executeInConsoleChecked(
        page,
        `{ dir.create("${projectDir}"); ` +
          // rs_writeProjectFile persists any ProjectId the scratch path is keyed by.
          `stopifnot(.Call("rs_writeProjectFile", "${rproj}", PACKAGE = "(embedding)")); ` +
          `scratch <- .Call("rs_computeScratchPaths", "${rproj}", PACKAGE = "(embedding)")$scratch_path; ` +
          `dir.create(scratch, recursive = TRUE, showWarnings = FALSE); ` +
          `writeLines("assistant=${assistant}", file.path(scratch, "assistant_options")) }`,
      );
      return rproj;
    }

    async function runCheckExpectingDialog(page: Page): Promise<void> {
      await setChatUpdateCheckOverride(page, UPDATE_AVAILABLE);
      await Promise.all([
        page.waitForRequest('**/rpc/chat_check_for_updates', { timeout: 10000 }),
        executeCommand(page, CHECK_UPDATES_CMD),
      ]);
      await expect(page.locator(DIALOG)).toBeVisible({ timeout: 10000 });
    }

    test.afterEach(async ({ rstudioPage: page }) => {
      await setChatUpdateCheckOverride(page, null);
      await dismissAllModals(page);
    });

    test.afterAll(async ({ rstudioPage: page }) => {
      try {
        await closeProjectIfOpen(page);
      } catch (err) {
        console.warn('[check-for-updates-project-override] afterAll closeProjectIfOpen failed:', err);
      }
      for (const pref of ['assistant', 'chat_provider']) {
        await clearPref(page, pref);
      }
    });

    test('offers the install when the project selects Posit AI over a global (None)', async ({
      rstudioPage: page,
    }) => {
      const rproj = await seedProject(page, 'AssistantPosit', 'posit');
      await openProject(page, rproj);
      await setPref(page, 'chat_provider', 'none');
      await setPref(page, 'assistant', 'none');

      await runCheckExpectingDialog(page);

      const dialog = page.locator(DIALOG);
      await expect(dialog).toContainText('2.0.0');
      await expect(page.locator(YES_BTN)).toContainText('Update Posit Assistant');

      // Dismiss with Cancel -- accepting would start a real download/install.
      await page.locator(NO_BTN).click();
      await expect(dialog).toBeHidden();
    });

    test('does not offer the install when the project selects (None) over a global Posit AI', async ({
      rstudioPage: page,
    }) => {
      const rproj = await seedProject(page, 'AssistantNone', 'none');
      await openProject(page, rproj);
      await setPref(page, 'chat_provider', 'none');
      await setPref(page, 'assistant', 'posit');

      await runCheckExpectingDialog(page);

      const dialog = page.locator(DIALOG);
      await expect(dialog).toContainText('Select a chat provider');
      await expect(page.locator(YES_BTN)).toHaveCount(0);

      await page.locator(CONFIRM_BTN).click();
      await expect(dialog).toBeHidden();
    });
  },
);

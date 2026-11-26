import { test, expect } from '@fixtures/rstudio.fixture';
import {
  dismissAllModals,
  getPref,
  numModalsShowing,
  openProject,
  setPref,
} from '@utils/commands';
import { closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeInConsoleChecked } from '@pages/console_pane.page';
import {
  ASSISTANT_TAB,
  ASSISTANT_PANEL,
  ASSISTANT_CODE_ASSISTANT_SELECT,
  GLOBAL_OPTIONS_DIALOG,
  OPTIONS_OK,
  openGlobalOptions,
} from '@pages/global_options.page';

// Regression test for #19742. While the open project sets its own code
// assistant, Global Options > Assistant locks its selector to the project's
// value. OK used to save that locked value as the global `assistant`
// preference (and derive `copilot_enabled` from it), so every other project
// lost the user's global choice.
test.describe.serial('Global Assistant pane project override', () => {
  const sandbox = useSuiteSandbox();

  // The test changes these prefs. Restore what the profile had rather than
  // clearing them: `assistant` defaults to "posit", which would switch Posit
  // Assistant on for whatever runs next in the session.
  let originalAssistant: string | null = null;
  let originalCopilotEnabled: boolean | null = null;

  test.beforeAll(async ({ rstudioPage: page }) => {
    originalAssistant = (await getPref(page, 'assistant')) as string | null;
    originalCopilotEnabled = (await getPref(page, 'copilot_enabled')) as boolean | null;

    // Seed the project's assistant in its scratch-path options file before
    // opening it, so the session reads it at startup just as the pane does.
    // Setting it through Project Options would raise a restart prompt.
    const projectDir = `${sandbox.dir.replace(/\\/g, '/')}/AssistantOverride`;
    const rproj = `${projectDir}/AssistantOverride.Rproj`;
    await executeInConsoleChecked(
      page,
      `{ dir.create("${projectDir}"); ` +
        // rs_writeProjectFile persists any ProjectId the scratch path is keyed by.
        `stopifnot(.Call("rs_writeProjectFile", "${rproj}", PACKAGE = "(embedding)")); ` +
        `scratch <- .Call("rs_computeScratchPaths", "${rproj}", PACKAGE = "(embedding)")$scratch_path; ` +
        `dir.create(scratch, recursive = TRUE, showWarnings = FALSE); ` +
        `writeLines("assistant=none", file.path(scratch, "assistant_options")) }`,
    );
    await openProject(page, rproj);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    try {
      if ((await numModalsShowing(page)) > 0) {
        await dismissAllModals(page);
      }
    } catch (err) {
      console.warn('[global_assistant_project_override] afterAll dismissAllModals failed:', err);
    }
    try {
      await closeProjectIfOpen(page);
    } catch (err) {
      console.warn('[global_assistant_project_override] afterAll closeProjectIfOpen failed:', err);
    }
    if (originalAssistant !== null)
      await setPref(page, 'assistant', originalAssistant);
    if (originalCopilotEnabled !== null)
      await setPref(page, 'copilot_enabled', originalCopilotEnabled);
  });

  test('OK keeps the global assistant while a project overrides it', async ({
    rstudioPage: page,
  }) => {
    // Set here, not in beforeAll: the per-test fixture runs after beforeAll and
    // resets a non-"none" assistant for suites not tagged @ai or @chat.
    await setPref(page, 'assistant', 'copilot');
    await setPref(page, 'copilot_enabled', true);
    expect(await getPref(page, 'assistant')).toBe('copilot');

    await openGlobalOptions(page);
    await page.locator(ASSISTANT_TAB).click();
    await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();

    // The selector locks to the project's value once project options load.
    const assistantSelect = page.locator(ASSISTANT_CODE_ASSISTANT_SELECT);
    await expect(assistantSelect).toBeDisabled({ timeout: 15000 });
    await expect(assistantSelect).toHaveValue('none');

    // Showing the project's (None) must not persist anything on its own: the
    // pane used to save it eagerly, as if the user had switched off Copilot.
    expect(await getPref(page, 'assistant')).toBe('copilot');
    expect(await getPref(page, 'copilot_enabled')).toBe(true);

    await page.locator(OPTIONS_OK).click();
    await page.waitForSelector(GLOBAL_OPTIONS_DIALOG, { state: 'detached', timeout: 10000 });

    await expect.poll(() => getPref(page, 'assistant')).toBe('copilot');
    await expect.poll(() => getPref(page, 'copilot_enabled')).toBe(true);
  });
});

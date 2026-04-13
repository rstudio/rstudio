import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { typeInConsole, clearConsole, CONSOLE_INPUT, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import type { Page } from 'playwright';

/**
 * Project Trust Dialog — rstudio/rstudio#17231, PR #17211
 *
 * Tests the trust dialog that prompts before auto-executing risky startup
 * files (.Rprofile, .Renviron, .RData) in untrusted project directories.
 *
 * Requires project-trust-dialogs=1 in rsession.conf (Server/Workbench only —
 * Desktop passes --config-file none and has no Pro overlay to enable it).
 *
 * Serial because tests build on shared trust state. The test order is designed
 * to alternate trust statuses so the GWT-side lastTrustStatus_ check allows
 * the dialog to re-appear between tests.
 */

// -- Selectors ----------------------------------------------------------------

const TRUST_DIALOG = '[role="alertdialog"]';
const TRUST_BTN_UNKNOWN = 'button:has-text("Yes, I trust this project")';
const DONT_TRUST_BTN = 'button:has-text("No, I do not trust this project")';
const TRUST_BTN_RESTRICTED = 'button:has-text("Trust this project")';
const KEEP_RESTRICTED_BTN = 'button:has-text("Keep restricted")';
const LOCK_ICON = '[title^="Restricted Mode"]';

const HEADER_UNKNOWN = 'Do you trust this project?';
const HEADER_RESTRICTED = 'This project is restricted';

// -- Constants ----------------------------------------------------------------

const PROJECT_NAME = 'trust_test_project';
const RPROFILE_MARKER = 'TRUST_MARKER <- TRUE';
const PALETTE_LIST = '#rstudio_command_palette_list';

// -- Helpers ------------------------------------------------------------------

/** Run an R expression wrapped in cat() with unique markers, return the output. */
async function captureResult(page: Page, rExpression: string): Promise<string> {
  const marker = `__TRUST_${Date.now()}__`;

  // Retry up to 3 times — after Server page navigation, the console may
  // not be fully connected yet and the first attempt can silently fail.
  for (let attempt = 0; attempt < 3; attempt++) {
    await clearConsole(page);
    await typeInConsole(page, `cat("${marker}", ${rExpression}, "${marker}")`);
    await sleep(1500);
    const output = await page.locator(CONSOLE_OUTPUT).innerText();
    const re = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`);
    const match = output.match(re);
    if (match) return match[1].trim();

    // Didn't find markers — wait and retry
    await sleep(2000);
  }
  console.warn(`captureResult: markers not found after 3 attempts for: ${rExpression}`);
  return '';
}

/** Wait for the R session to restart and the console to become ready. */
async function waitForSessionRestart(page: Page): Promise<void> {
  // On Server, project switches navigate to a new session URL.
  // Wait for navigation to settle before checking for the console.
  await page.waitForLoadState('load', { timeout: 30000 }).catch(() => {});
  await sleep(3000);
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: 60000 });
  await sleep(2000);

  // Wait for GWT to fully initialize (not just the console DOM element)
  await page.waitForFunction(
    'typeof window.rstudioapi !== "undefined" || typeof window.$RStudio !== "undefined"',
    null,
    { timeout: 15000 }
  ).catch(() => {});
  await sleep(1000);

  // Confirm R is actually idle by running a trivial command with retries
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const marker = `__READY_${Date.now()}__`;
      await typeInConsole(page, `cat("${marker}")`);
      await sleep(1500);
      const output = await page.locator(CONSOLE_OUTPUT).innerText();
      if (output.includes(marker)) return;
    } catch {
      // typeInConsole may fail if console isn't ready
    }
    await sleep(2000);
  }
}

/** Restart R and wait for the console to be ready. */
async function restartR(page: Page): Promise<void> {
  await typeInConsole(page, '.rs.api.executeCommand("restartR")');
  await waitForSessionRestart(page);
}

/**
 * Restart R when a trust dialog is expected afterward.
 * Skips the idle check in waitForSessionRestart since the dialog
 * blocks console access.
 */
async function restartRExpectingDialog(page: Page): Promise<void> {
  await typeInConsole(page, '.rs.api.executeCommand("restartR")');
  await sleep(3000);
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: 30000 });
  await sleep(2000);
}

/** Check if the trust dialog is visible within the given timeout. */
async function isTrustDialogVisible(page: Page, timeout = 10000): Promise<boolean> {
  try {
    await expect(page.locator(TRUST_DIALOG)).toBeVisible({ timeout });
    return true;
  } catch {
    return false;
  }
}

/** Reset trust state for a directory (or current project). */
async function resetTrust(page: Page, dir?: string): Promise<void> {
  if (dir) {
    const escaped = dir.replace(/\\/g, '/');
    await typeInConsole(page, `.rs.trust.reset("${escaped}")`);
  } else {
    await typeInConsole(page, '.rs.trust.reset()');
  }
  await sleep(500);
}

/** Revoke trust for a directory (mark as untrusted). */
async function revokeTrust(page: Page, dir?: string): Promise<void> {
  if (dir) {
    const escaped = dir.replace(/\\/g, '/');
    await typeInConsole(page, `.rs.trust.revoke("${escaped}")`);
  } else {
    await typeInConsole(page, '.rs.trust.revoke()');
  }
  await sleep(500);
}

/** Write a .Rprofile in the given directory. */
async function writeRprofile(page: Page, dir: string, content: string): Promise<void> {
  const escaped = dir.replace(/\\/g, '/');
  await typeInConsole(page, `writeLines('${content}', file.path("${escaped}", ".Rprofile"))`);
  await sleep(500);
}

/** Get the current working directory from R. */
async function getWorkingDir(page: Page): Promise<string> {
  return captureResult(page, 'getwd()');
}

/**
 * Create a new RStudio project using the New Project wizard (UI).
 * Opens the wizard via the command palette — executeCommand("newProject")
 * blocks the R thread and triggers an "R session is busy" modal.
 */
async function createProjectViaUI(page: Page, name: string): Promise<void> {
  // Close any open source docs to prevent "unsaved changes" dialogs during project switch
  await typeInConsole(page, '.rs.api.closeAllSourceBuffersWithoutSaving()');
  await sleep(1000);

  // Open command palette and invoke "Create a New Project..."
  await page.keyboard.press('ControlOrMeta+Shift+p');
  await sleep(1000);

  await page.keyboard.type('Create a New Project');
  await sleep(500);

  const paletteItem = page.locator(`${PALETTE_LIST} >> text=Create a New Project...`);
  await expect(paletteItem).toBeVisible({ timeout: 5000 });
  await paletteItem.click();
  await sleep(2000);

  // Wait for wizard dialog
  const dialog = page.locator('.gwt-DialogBox');
  await expect(dialog).toBeVisible({ timeout: 15000 });

  // Step 1: Click "New Directory"
  const newDirItem = dialog.locator('#rstudio_label_new_directory_wizard_page');
  await expect(newDirItem).toBeVisible({ timeout: 5000 });
  await newDirItem.click();
  await sleep(1000);

  // Step 2: Click "New Project" in the project type list
  // Each wizard item gets an ID from its title: rstudio_label_{title}_wizard_page
  const newProjItem = dialog.locator('#rstudio_label_new_project_wizard_page');
  await expect(newProjItem).toBeVisible({ timeout: 5000 });
  await newProjItem.click();
  await sleep(1000);

  // Step 3: Enter directory name — use click + pressSequentially so GWT
  // detects keystrokes and enables the OK button
  const dirInput = dialog.locator('input.gwt-TextBox').first();
  await expect(dirInput).toBeVisible({ timeout: 5000 });
  await dirInput.click();
  await dirInput.pressSequentially(name);
  await sleep(500);

  // Step 4: Click "Create Project"
  // Wizard overrides the OK button ID: rstudio_label_{caption}_wizard_confirm
  const createBtn = page.locator('#rstudio_label_create_project_wizard_confirm');
  await expect(createBtn).toBeVisible({ timeout: 5000 });
  await createBtn.click();

  // Session restarts when switching to the new project
  await waitForSessionRestart(page);
}

// -- Test Suite ---------------------------------------------------------------

test.describe.serial('Project Trust Dialog (#17231)', { tag: ['@server_only', '@serial'] }, () => {
  let projectDir = '';
  let trustEnabled = true;
  let originalLoadWorkspace = 'FALSE';

  test.beforeAll(async ({ rstudioPage: page }) => {
    // Dismiss any leftover dialog/overlay from a prior failed run.
    // Try Escape multiple times to handle trust dialogs, save prompts, etc.
    for (let i = 0; i < 3; i++) {
      try {
        const overlay = page.locator('.gwt-PopupPanelGlass, [role="alertdialog"]');
        if (await overlay.first().isVisible({ timeout: 2000 })) {
          await page.keyboard.press('Escape');
          await sleep(1000);
        } else {
          break;
        }
      } catch {
        break;
      }
    }

    // Capture original load_workspace preference for restoration
    originalLoadWorkspace = await captureResult(page,
      '.rs.api.readRStudioPreference("load_workspace")');
    console.log(`Original load_workspace: ${originalLoadWorkspace}`);

    // Ensure we're in the home directory before cleanup (can't delete a dir we're in)
    await typeInConsole(page, 'setwd("~")');
    await sleep(500);

    // Clean up any leftover test project from a prior run
    await typeInConsole(page, `unlink("~/trust_test_project", recursive = TRUE)`);
    await sleep(500);

    // Reset trust for the test project directory in case a prior run left it in trust.json
    await typeInConsole(page, '.rs.trust.reset(path.expand("~/trust_test_project"))');
    await sleep(500);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    try {
      if (projectDir) {
        // Reset trust entries
        await resetTrust(page, projectDir);

        // Close the project via command palette (executeCommand blocks R)
        await page.keyboard.press('ControlOrMeta+Shift+p');
        await sleep(1000);
        await page.keyboard.type('Close Current Project');
        await sleep(500);
        const closeItem = page.locator(`${PALETTE_LIST} >> text=Close Current Project`);
        await expect(closeItem).toBeVisible({ timeout: 5000 });
        await closeItem.click();
        await waitForSessionRestart(page);

        // Delete the test project directory
        const escaped = projectDir.replace(/\\/g, '/');
        await typeInConsole(page, `unlink("${escaped}", recursive = TRUE)`);
        await sleep(1000);
      }

      // Restore original workspace preference
      await typeInConsole(page,
        `.rs.api.writeRStudioPreference("load_workspace", ${originalLoadWorkspace})`);
      await sleep(500);
    } catch {
      // Best-effort cleanup
    }
  });

  // ---------------------------------------------------------------------------
  // Test 1: No dialog without a project
  // ---------------------------------------------------------------------------
  test('No trust dialog when no project is open', async ({ rstudioPage: page }) => {
    // Ensure no project is open
    const hasProject = await captureResult(page,
      '!is.null(rstudioapi::getActiveProject())');

    if (hasProject === 'TRUE') {
      await page.keyboard.press('ControlOrMeta+Shift+p');
      await sleep(1000);
      await page.keyboard.type('Close Current Project');
      await sleep(500);
      const closeItem = page.locator(`${PALETTE_LIST} >> text=Close Current Project`);
      await expect(closeItem).toBeVisible({ timeout: 5000 });
      await closeItem.click();
      await waitForSessionRestart(page);
    }

    // Restart R session to trigger trust check
    await restartR(page);

    // No dialog should appear (no project open)
    const dialogVisible = await isTrustDialogVisible(page, 5000);
    expect(dialogVisible).toBe(false);

    // No lock icon
    await expect(page.locator(LOCK_ICON)).not.toBeVisible();
  });

  // ---------------------------------------------------------------------------
  // Test 2: Trust dialog appears when project has .Rprofile → "Don't Trust"
  // ---------------------------------------------------------------------------
  test('Trust dialog appears and "Don\'t Trust" enables restricted mode', async ({ rstudioPage: page }) => {
    // Create project via UI (command palette > New Project wizard)
    await createProjectViaUI(page, PROJECT_NAME);

    // Record the project directory
    projectDir = await getWorkingDir(page);
    console.log(`Project created at: ${projectDir}`);

    // Add a .Rprofile with a marker variable
    await writeRprofile(page, projectDir, RPROFILE_MARKER);

    // Restart R to trigger trust check — use restartRExpectingDialog since
    // the trust dialog may block console access
    await restartRExpectingDialog(page);

    // Check if trust dialog appears
    const dialogVisible = await isTrustDialogVisible(page, 15000);

    if (!dialogVisible) {
      // Trust dialogs not enabled on this server
      trustEnabled = false;
      const markerExists = await captureResult(page, 'exists("TRUST_MARKER")');
      console.log(`Trust dialog not shown. TRUST_MARKER exists: ${markerExists}`);
      console.log('Trust dialogs disabled — set project-trust-dialogs=1 in rsession.conf to enable. Skipping remaining tests.');
      test.skip(true, 'Trust dialog not visible — requires project-trust-dialogs=1 in rsession.conf');
      return;
    }

    // Verify dialog content (unknown variant)
    const dialog = page.locator(TRUST_DIALOG);
    await expect(dialog.locator(`text=${HEADER_UNKNOWN}`)).toBeVisible();
    await expect(dialog.locator('text=Files detected:')).toBeVisible();
    await expect(dialog.locator('a:has-text(".Rprofile")')).toBeVisible();

    // Verify both buttons present
    await expect(page.locator(TRUST_BTN_UNKNOWN)).toBeVisible();
    await expect(page.locator(DONT_TRUST_BTN)).toBeVisible();

    // Click "No, I do not trust this project"
    await page.locator(DONT_TRUST_BTN).click();
    await sleep(2000);

    // Verify restricted mode: lock icon visible
    await expect(page.locator(LOCK_ICON)).toBeVisible({ timeout: 5000 });

    // Verify .Rprofile was NOT sourced
    const markerExists = await captureResult(page, 'exists("TRUST_MARKER")');
    expect(markerExists).toBe('FALSE');
  });

  // ---------------------------------------------------------------------------
  // Test 3: Restricted variant after "Don't Trust"
  // The trust decision was persisted in trust.json. On restart, the dialog
  // shows the "restricted" variant with different header and button text.
  // (Status changed from "unknown" to "untrusted", so lastTrustStatus_ allows it.)
  // ---------------------------------------------------------------------------
  test('Restart shows restricted variant after "Don\'t Trust"', async ({ rstudioPage: page }) => {
    if (!trustEnabled) { test.skip(true, 'Trust dialogs not enabled'); return; }

    await restartRExpectingDialog(page);

    // Restricted variant should appear
    await expect(page.locator(TRUST_DIALOG)).toBeVisible({ timeout: 15000 });

    const dialog = page.locator(TRUST_DIALOG);
    await expect(dialog.locator(`text=${HEADER_RESTRICTED}`)).toBeVisible();

    // Verify restricted-variant buttons
    await expect(page.locator(TRUST_BTN_RESTRICTED)).toBeVisible();
    await expect(page.locator(KEEP_RESTRICTED_BTN)).toBeVisible();

    // Click "Keep restricted"
    await page.locator(KEEP_RESTRICTED_BTN).click();
    await sleep(1000);

    // Lock icon still visible
    await expect(page.locator(LOCK_ICON)).toBeVisible({ timeout: 5000 });
  });

  // ---------------------------------------------------------------------------
  // Test 4: Escape dismisses dialog without persisting
  // Reset trust so status becomes "unknown" (different from lastTrustStatus_
  // "untrusted"), which lets the dialog re-appear. Escape suppresses files
  // for the session but doesn't persist the decision.
  // ---------------------------------------------------------------------------
  test('Escape suppresses startup files without persisting', async ({ rstudioPage: page }) => {
    if (!trustEnabled) { test.skip(true, 'Trust dialogs not enabled'); return; }

    // Reset trust to get "unknown" status (different from lastTrustStatus_ "untrusted")
    await resetTrust(page, projectDir);
    await restartRExpectingDialog(page);

    // Unknown dialog should appear
    await expect(page.locator(TRUST_DIALOG)).toBeVisible({ timeout: 15000 });
    await expect(page.locator(TRUST_DIALOG).locator(`text=${HEADER_UNKNOWN}`)).toBeVisible();

    // Press Escape to dismiss
    await page.keyboard.press('Escape');
    await sleep(1000);

    // Dialog dismissed
    await expect(page.locator(TRUST_DIALOG)).not.toBeVisible();

    // Restricted mode active: lock icon visible, .Rprofile not sourced
    await expect(page.locator(LOCK_ICON)).toBeVisible({ timeout: 5000 });
    const markerExists = await captureResult(page, 'exists("TRUST_MARKER")');
    expect(markerExists).toBe('FALSE');
  });

  // ---------------------------------------------------------------------------
  // Test 5: "Trust" restarts session and loads startup files
  // Revoke trust so status becomes "untrusted" (different from lastTrustStatus_
  // "unknown"), which lets the restricted dialog appear. Clicking "Trust this
  // project" grants trust, restarts the session, and .Rprofile is sourced.
  // ---------------------------------------------------------------------------
  test('"Trust" restarts session and loads startup files', async ({ rstudioPage: page }) => {
    if (!trustEnabled) { test.skip(true, 'Trust dialogs not enabled'); return; }

    // Revoke trust to trigger restricted variant (status "untrusted" ≠ lastTrustStatus_ "unknown")
    await revokeTrust(page, projectDir);
    await restartRExpectingDialog(page);

    // Restricted dialog should appear
    await expect(page.locator(TRUST_DIALOG)).toBeVisible({ timeout: 15000 });

    // Click "Trust this project" (restricted variant's trust button)
    await page.locator(TRUST_BTN_RESTRICTED).click();

    // Granting trust triggers a session restart (with page navigation on Server)
    await waitForSessionRestart(page);

    // No lock icon (project is now trusted)
    await expect(page.locator(LOCK_ICON)).not.toBeVisible();

    // .Rprofile WAS sourced — marker variable exists
    const markerExists = await captureResult(page, 'exists("TRUST_MARKER")');
    expect(markerExists).toBe('TRUE');
  });

  // ---------------------------------------------------------------------------
  // Test 6: renv carve-out — no dialog for safe .Rprofile
  // A .Rprofile containing only source("renv/activate.R") is treated as safe
  // and doesn't trigger the trust dialog.
  // ---------------------------------------------------------------------------
  test('No dialog for renv-only .Rprofile', async ({ rstudioPage: page }) => {
    if (!trustEnabled) { test.skip(true, 'Trust dialogs not enabled'); return; }

    // Reset trust so the project has no trust decision
    await resetTrust(page, projectDir);

    // Replace .Rprofile with renv-safe content and create a dummy renv/activate.R
    // so it doesn't error when sourced (no risky files → trust not required → .Rprofile runs)
    await writeRprofile(page, projectDir, 'source("renv/activate.R")');
    const escaped = projectDir.replace(/\\/g, '/');
    await typeInConsole(page, `dir.create(file.path("${escaped}", "renv"), showWarnings = FALSE)`);
    await sleep(300);
    await typeInConsole(page, `writeLines("# dummy", file.path("${escaped}", "renv", "activate.R"))`);
    await sleep(300);

    // Restart R
    await restartR(page);

    // No dialog — .Rprofile is safe (renv carve-out), no other risky files
    const dialogVisible = await isTrustDialogVisible(page, 5000);
    expect(dialogVisible).toBe(false);

    // No lock icon
    await expect(page.locator(LOCK_ICON)).not.toBeVisible();
  });

  // ---------------------------------------------------------------------------
  // Test 7: .RData triggers dialog when workspace restore is enabled
  // ---------------------------------------------------------------------------
  test('.RData triggers dialog when workspace restore is enabled', async ({ rstudioPage: page }) => {
    if (!trustEnabled) { test.skip(true, 'Trust dialogs not enabled'); return; }

    // Reset trust
    await resetTrust(page, projectDir);

    // Enable workspace restore
    await typeInConsole(page, '.rs.api.writeRStudioPreference("load_workspace", TRUE)');
    await sleep(500);

    // Create an .RData file (empty workspace save)
    const escaped = projectDir.replace(/\\/g, '/');
    await typeInConsole(page, `save(list = character(0), file = file.path("${escaped}", ".RData"))`);
    await sleep(500);

    // Restart — dialog expected for .RData
    await restartRExpectingDialog(page);

    // Trust dialog should appear for .RData
    const dialogVisible = await isTrustDialogVisible(page, 15000);
    expect(dialogVisible).toBe(true);

    // Verify .RData is listed in detected files (.RData also appears in the
    // explanation text, so use exact match to target the file name element)
    const dialog = page.locator(TRUST_DIALOG);
    await expect(dialog.getByText('.RData', { exact: true })).toBeVisible();

    // Dismiss with Escape
    await page.keyboard.press('Escape');
    await sleep(1000);
  });

  // ---------------------------------------------------------------------------
  // Test 8: .RData does NOT trigger dialog when workspace restore is disabled
  // ---------------------------------------------------------------------------
  test('.RData does not trigger dialog when workspace restore is disabled', async ({ rstudioPage: page }) => {
    if (!trustEnabled) { test.skip(true, 'Trust dialogs not enabled'); return; }

    // Reset trust (idempotent — already reset after test 7's Escape, but explicit for clarity)
    await resetTrust(page, projectDir);

    // Disable workspace restore
    await typeInConsole(page, '.rs.api.writeRStudioPreference("load_workspace", FALSE)');
    await sleep(500);

    // .RData still exists from test 7, renv .Rprofile from test 6
    // With load_workspace=FALSE, .RData is not risky
    // renv .Rprofile is safe
    // No risky files → no dialog

    // Restart
    await restartR(page);

    // No dialog
    const dialogVisible = await isTrustDialogVisible(page, 5000);
    expect(dialogVisible).toBe(false);

    // No lock icon
    await expect(page.locator(LOCK_ICON)).not.toBeVisible();
  });
});

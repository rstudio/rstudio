import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import {
  executeCommand,
  setChatUpdateCheckOverride,
  setPref,
  getPref,
  dismissAllModals,
} from '@utils/commands';
import { CONFIRM_BTN, YES_BTN, NO_BTN } from '@pages/modals.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import { setupPositAssistantChat, annotateVersions } from './_chat-setup';
import type { Page } from 'playwright';

/**
 * "Check for Posit Assistant updates" command -- rstudio/rstudio#18253
 *
 * The command runs a forced update check and ALWAYS reports the result in a
 * modal dialog: up to date, an update/install is available (offer to install),
 * a newer Posit Assistant requires a newer RStudio (OK only), or an error.
 * Accepting an available update reuses the existing install engine, which is
 * refused unless Posit Assistant is selected as the chat provider or assistant.
 *
 * Unlike deprecate-pai-blocking.test.ts (which drives the *startup* flow and
 * asserts on the inline window.rstudio.chat.state), this command shows GWT
 * modal dialogs, so the assertions read the .gwt-DialogBox text and its
 * buttons. The check outcome is driven deterministically with the
 * chat_set_update_check_override one-shot RPC (same mechanism as the blocking
 * suite): set the override, invoke the command, and the next
 * chat_check_for_updates returns it verbatim.
 *
 * The install itself (Yes on the confirm dialog) needs a real download + inter-
 * session lock and is covered below the UI by the C++ suites, so these tests
 * assert up to and including the confirmation dialog, then dismiss with Cancel.
 */

const CHECK_UPDATES_CMD = 'checkForPositAssistantUpdates';
const DIALOG = '.gwt-DialogBox';

// Default response shape -- callers spread + override the relevant fields.
// Mirrors the field set returned by chatCheckForUpdates in SessionChat.cpp so
// rsession's override has every field the response schema expects.
const DEFAULT_CHECK_RESPONSE = {
  updateAvailable: false,
  isDowngrade: false,
  noCompatibleVersion: false,
  unsupportedInstalledVersion: false,
  unsupportedProtocol: false,
  manifestUnavailable: false,
  errorMessage: '',
  currentVersion: '1.0.0',
  newVersion: '',
  downloadUrl: '',
  isInitialInstall: false,
};

test.describe.serial('Check for Posit Assistant updates -- #18253', { tag: ['@ai', '@serial'] }, () => {
  requireAiCredentials(test, 'positai');

  let versions: EnvironmentVersions;
  // The guard test flips these prefs; capture and restore them so a shared
  // worker's later specs still see Posit Assistant selected.
  let originalChatProvider: string | null = null;
  let originalAssistant: string | null = null;

  test.beforeAll(async ({ rstudioPage: page }) => {
    ({ versions } = await setupPositAssistantChat(page));
    originalChatProvider = (await getPref(page, 'chat_provider')) as string | null;
    originalAssistant = (await getPref(page, 'assistant')) as string | null;
  });

  test.beforeEach(async () => {
    annotateVersions(versions);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // Clear the per-test override so an unrelated chat_check_for_updates
    // (session resume, a downstream test) doesn't pick it up, and dismiss any
    // dialog a failed assertion left on screen so its glass panel can't wedge
    // the next test.
    await setChatUpdateCheckOverride(page, null);
    await dismissAllModals(page);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    if (originalChatProvider !== null)
      await setPref(page, 'chat_provider', originalChatProvider);
    if (originalAssistant !== null)
      await setPref(page, 'assistant', originalAssistant);
  });

  /**
   * Install the override, invoke the command, and wait for its result dialog.
   * Waiting on the chat_check_for_updates request (rather than just the dialog)
   * fails fast and clearly if the command never dispatched the check.
   */
  async function runCheckExpectingDialog(
    page: Page,
    override: Record<string, unknown>,
  ): Promise<void> {
    await setChatUpdateCheckOverride(page, override);
    await Promise.all([
      page.waitForRequest('**/rpc/chat_check_for_updates', { timeout: 10000 }),
      executeCommand(page, CHECK_UPDATES_CMD),
    ]);
    await expect(page.locator(DIALOG)).toBeVisible({ timeout: 10000 });
  }

  // -------------------------------------------------------------------------
  // Test 1: No update available -> informational "up to date" dialog
  // -------------------------------------------------------------------------
  test('reports when Posit Assistant is up to date', async ({ rstudioPage: page }) => {
    await runCheckExpectingDialog(page, { ...DEFAULT_CHECK_RESPONSE, updateAvailable: false });

    const dialog = page.locator(DIALOG);
    await expect(dialog).toContainText('latest version of Posit Assistant');
    // Informational: OK only, no install offered.
    await expect(page.locator(YES_BTN)).toHaveCount(0);

    await page.locator(CONFIRM_BTN).click();
    await expect(dialog).toBeHidden();
  });

  // -------------------------------------------------------------------------
  // Test 2: Update available -> offer to install it
  // -------------------------------------------------------------------------
  test('offers to install an available update', async ({ rstudioPage: page }) => {
    await runCheckExpectingDialog(page, {
      ...DEFAULT_CHECK_RESPONSE,
      updateAvailable: true,
      currentVersion: '1.0.0',
      newVersion: '2.0.0',
    });

    const dialog = page.locator(DIALOG);
    await expect(dialog).toContainText('1.0.0');
    await expect(dialog).toContainText('2.0.0');
    await expect(page.locator(YES_BTN)).toContainText('Update Posit Assistant');

    // Dismiss with Cancel -- accepting would start a real download/install.
    await page.locator(NO_BTN).click();
    await expect(dialog).toBeHidden();
  });

  // -------------------------------------------------------------------------
  // Test 3: Downgrade -> presented explicitly, not as an ordinary update
  // -------------------------------------------------------------------------
  test('presents an older recommended version as a downgrade', async ({ rstudioPage: page }) => {
    await runCheckExpectingDialog(page, {
      ...DEFAULT_CHECK_RESPONSE,
      updateAvailable: true,
      isDowngrade: true,
      currentVersion: '2.0.0',
      newVersion: '1.0.0',
    });

    const dialog = page.locator(DIALOG);
    await expect(dialog).toContainText('currently recommended');
    // The confirm button names the concrete (older) target version rather than
    // a generic "Update".
    await expect(page.locator(YES_BTN)).toContainText('Install Version 1.0.0');

    await page.locator(NO_BTN).click();
    await expect(dialog).toBeHidden();
  });

  // -------------------------------------------------------------------------
  // Test 4: Newer Posit Assistant needs a newer RStudio -> OK-only dialog
  // -------------------------------------------------------------------------
  test('shows an RStudio-update-required dialog with no install action', async ({ rstudioPage: page }) => {
    await runCheckExpectingDialog(page, { ...DEFAULT_CHECK_RESPONSE, unsupportedProtocol: true });

    const dialog = page.locator(DIALOG);
    await expect(dialog).toContainText('no longer supported by Posit Assistant');
    // No install is possible: OK only.
    await expect(page.locator(YES_BTN)).toHaveCount(0);

    await page.locator(CONFIRM_BTN).click();
    await expect(dialog).toBeHidden();
  });

  // -------------------------------------------------------------------------
  // Test 5: Manifest unavailable -> error dialog surfacing the detail
  // -------------------------------------------------------------------------
  test('surfaces a manifest error with its detail', async ({ rstudioPage: page }) => {
    await runCheckExpectingDialog(page, {
      ...DEFAULT_CHECK_RESPONSE,
      manifestUnavailable: true,
      errorMessage: 'Network error: unable to download manifest',
    });

    const dialog = page.locator(DIALOG);
    await expect(dialog).toContainText('Unable to verify Posit Assistant compatibility');
    await expect(dialog).toContainText('Network error: unable to download manifest');

    await page.locator(CONFIRM_BTN).click();
    await expect(dialog).toBeHidden();
  });

  // -------------------------------------------------------------------------
  // Test 6: Guard -- do not offer an install the backend would refuse
  //
  // chat_install_update is refused unless Posit Assistant is selected as the
  // chat provider or assistant. With neither selected, an "update available"
  // result must direct the user to select it rather than offer an install that
  // would fail. Runs last because it changes prefs (restored in afterAll).
  // -------------------------------------------------------------------------
  test('does not offer install when Posit Assistant is not selected', async ({ rstudioPage: page }) => {
    await setPref(page, 'chat_provider', 'none');
    await setPref(page, 'assistant', 'none');

    await runCheckExpectingDialog(page, {
      ...DEFAULT_CHECK_RESPONSE,
      updateAvailable: true,
      currentVersion: '1.0.0',
      newVersion: '2.0.0',
    });

    const dialog = page.locator(DIALOG);
    await expect(dialog).toContainText('Select a chat provider');
    // The install prompt is suppressed -- no Yes/Update button.
    await expect(page.locator(YES_BTN)).toHaveCount(0);

    await page.locator(CONFIRM_BTN).click();
    await expect(dialog).toBeHidden();
  });
});

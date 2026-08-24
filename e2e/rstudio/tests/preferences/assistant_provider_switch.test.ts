import { test, expect } from '@fixtures/rstudio.fixture';
import { clearPref, getPref, setPref, setChatUpdateCheckOverride } from '@utils/commands';
import { NO_BTN } from '@pages/modals.page';
import {
  ASSISTANT_TAB,
  ASSISTANT_PANEL,
  ASSISTANT_CODE_ASSISTANT_SELECT,
  ASSISTANT_POSIT_OPTION,
  openGlobalOptions,
  closeGlobalOptions,
} from '@pages/global_options.page';

/**
 * Selecting Posit AI as the code assistant runs an install/update check whose
 * result decides whether the install prompt appears. This forces that check to
 * report a fresh install as available, so the prompt shows regardless of what
 * is on disk and without a manifest fetch. Consumed by the next
 * chat_check_for_updates (see s_updateCheckOverride in SessionChat.cpp), which
 * is why it is installed immediately before the selection that triggers it.
 */
const INSTALL_AVAILABLE_RESPONSE = {
  updateAvailable: true,
  isInitialInstall: true,
  isDowngrade: false,
  noCompatibleVersion: false,
  unsupportedInstalledVersion: false,
  unsupportedProtocol: false,
  manifestUnavailable: false,
  errorMessage: '',
  currentVersion: '',
  newVersion: '99.0.0',
  downloadUrl: '',
};

// @desktop_only: GitHub Copilot is offered in the code-assistant selector only
// when the copilot-enabled session option is set, which defaults on in Desktop
// mode alone (SessionOptions.cpp).
test.describe('Assistant code-assistant switch', { tag: ['@desktop_only'] }, () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await setChatUpdateCheckOverride(page, null);
    for (const pref of ['assistant', 'copilot_enabled']) {
      await clearPref(page, pref);
    }
  });

  // Regression test for #18356. Switching away from Copilot persists the new
  // assistant right away -- that write is how the Copilot agent is stopped --
  // so declining the install prompt has to undo it. It did not: the value to
  // revert to was read after the write had already replaced it with "posit",
  // which left the assistant pointing at an addon that was never installed.
  test('declining the Posit Assistant install reverts a Copilot selection', async ({ rstudioPage: page }) => {
    await setPref(page, 'assistant', 'copilot');
    await setPref(page, 'copilot_enabled', true);

    await openGlobalOptions(page);
    await page.locator(ASSISTANT_TAB).click();
    await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();

    const assistantSelect = page.locator(ASSISTANT_CODE_ASSISTANT_SELECT);
    await expect(assistantSelect).toHaveValue('copilot');

    await setChatUpdateCheckOverride(page, INSTALL_AVAILABLE_RESPONSE);
    await assistantSelect.selectOption({ label: ASSISTANT_POSIT_OPTION });

    // Decline the install prompt. It follows an assistantVerifyInstalled
    // round-trip and the forced check, so allow for both.
    const declineBtn = page.locator(NO_BTN);
    await expect(declineBtn).toBeVisible({ timeout: 30000 });
    await declineBtn.click();
    await expect(declineBtn).toBeHidden();

    // Both the selection and the preferences behind it go back to Copilot.
    // copilot_enabled matters as much as assistant here: left false, the
    // reverted selection would name an agent the backend keeps stopped.
    await expect(assistantSelect).toHaveValue('copilot');
    await expect.poll(() => getPref(page, 'assistant')).toBe('copilot');
    await expect.poll(() => getPref(page, 'copilot_enabled')).toBe(true);

    // Cancel, the dismissal that persists nothing: the reverted values above
    // must be what survives the dialog, not something onApply wrote.
    await closeGlobalOptions(page);
    await expect.poll(() => getPref(page, 'assistant')).toBe('copilot');
    await expect.poll(() => getPref(page, 'copilot_enabled')).toBe(true);
  });
});

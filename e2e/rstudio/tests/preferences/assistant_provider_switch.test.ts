import { test, expect } from '@fixtures/rstudio.fixture';
import { clearPref, dismissAllModals, getPref, setPref, setChatUpdateCheckOverride } from '@utils/commands';
import { NO_BTN } from '@pages/modals.page';
import {
  DIALOG_BOX,
  ASSISTANT_TAB,
  ASSISTANT_PANEL,
  ASSISTANT_CODE_ASSISTANT_SELECT,
  ASSISTANT_COPILOT_OPTION,
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
    // A failure mid-test can leave the options dialog, the install prompt, or
    // both up; their modal glass would then intercept the next test's clicks.
    if (await page.locator(DIALOG_BOX).count() > 0) {
      await dismissAllModals(page);
      await page.waitForSelector(DIALOG_BOX, { state: 'detached', timeout: 10000 });
    }

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
    // copilot_enabled is the deprecated mirror of the selection: left false
    // under a Copilot selection, the next switch away from Copilot would not
    // persist, since that write is gated on it.
    await expect(assistantSelect).toHaveValue('copilot');
    await expect.poll(() => getPref(page, 'assistant')).toBe('copilot');
    await expect.poll(() => getPref(page, 'copilot_enabled')).toBe(true);

    // Cancel, the dismissal that persists nothing: the reverted values above
    // must be what survives the dialog, not something onApply wrote.
    await closeGlobalOptions(page);
    await expect.poll(() => getPref(page, 'assistant')).toBe('copilot');
    await expect.poll(() => getPref(page, 'copilot_enabled')).toBe(true);
  });

  // What the revert puts back is the selection, not the preference behind it.
  // Selecting Copilot from None persists nothing (there is no Copilot agent to
  // stop), so a decline has to fall back to the selector's own prior value --
  // and must leave the preference alone, since OK was never pressed.
  test('declining reverts to an unsaved Copilot selection without persisting it', async ({ rstudioPage: page }) => {
    await setPref(page, 'assistant', 'none');
    await setPref(page, 'copilot_enabled', false);

    await openGlobalOptions(page);
    await page.locator(ASSISTANT_TAB).click();
    await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();

    const assistantSelect = page.locator(ASSISTANT_CODE_ASSISTANT_SELECT);
    await assistantSelect.selectOption({ label: ASSISTANT_COPILOT_OPTION });
    await expect.poll(() => getPref(page, 'assistant')).toBe('none');

    await setChatUpdateCheckOverride(page, INSTALL_AVAILABLE_RESPONSE);
    await assistantSelect.selectOption({ label: ASSISTANT_POSIT_OPTION });

    const declineBtn = page.locator(NO_BTN);
    await expect(declineBtn).toBeVisible({ timeout: 30000 });
    await declineBtn.click();
    await expect(declineBtn).toBeHidden();

    await expect(assistantSelect).toHaveValue('copilot');
    await expect.poll(() => getPref(page, 'assistant')).toBe('none');

    await closeGlobalOptions(page);
    await expect.poll(() => getPref(page, 'assistant')).toBe('none');
  });

  // Same shape as above, but with Posit AI already saved: the decline must not
  // write the unsaved Copilot selection over it, or cancelling the dialog would
  // no longer restore the setting the user actually had.
  test('declining does not persist an unsaved selection over a saved Posit AI', async ({ rstudioPage: page }) => {
    await setPref(page, 'assistant', 'posit');
    await setPref(page, 'copilot_enabled', false);

    await openGlobalOptions(page);
    await page.locator(ASSISTANT_TAB).click();
    await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();

    const assistantSelect = page.locator(ASSISTANT_CODE_ASSISTANT_SELECT);
    await assistantSelect.selectOption({ label: ASSISTANT_COPILOT_OPTION });

    await setChatUpdateCheckOverride(page, INSTALL_AVAILABLE_RESPONSE);
    await assistantSelect.selectOption({ label: ASSISTANT_POSIT_OPTION });

    const declineBtn = page.locator(NO_BTN);
    await expect(declineBtn).toBeVisible({ timeout: 30000 });
    await declineBtn.click();
    await expect(declineBtn).toBeHidden();

    await expect(assistantSelect).toHaveValue('copilot');
    await expect.poll(() => getPref(page, 'assistant')).toBe('posit');

    // Cancel discards the unapplied Copilot selection, leaving Posit AI saved.
    await closeGlobalOptions(page);
    await expect.poll(() => getPref(page, 'assistant')).toBe('posit');
    await expect.poll(() => getPref(page, 'copilot_enabled')).toBe(false);
  });
});

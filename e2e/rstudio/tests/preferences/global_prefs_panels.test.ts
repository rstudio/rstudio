import { test, expect } from '@fixtures/rstudio.fixture';
import { dismissAllModals, clearPref, setPref } from '@utils/commands';
import {
  DIALOG_BOX,
  OPTIONS_OK,
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
  ASSISTANT_CODE_ASSISTANT_SELECT,
  ASSISTANT_COMPLETIONS_TRIGGER_SELECT,
  ASSISTANT_COMPLETIONS_DELAY_LABEL,
  ASSISTANT_COMPLETIONS_DELAY_INPUT,
  ASSISTANT_COPILOT_OPTION,
  ASSISTANT_NONE_OPTION,
  ASSISTANT_UPDATE_CHECK_INPUT,
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
    const autoDetect = page.getByLabel('Auto-detect code indentation');
    await expect(autoDetect).toBeVisible();
    await expect(autoDetect).not.toBeChecked();

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

  // Panels that are a plain open -> click tab -> verify panel visible. Each row
  // generates its own named test so a failure still points at one panel.
  const SIMPLE_PANELS = [
    { name: 'Pane Layout', tab: PANE_LAYOUT_TAB, panel: PANE_LAYOUT_PANEL },
    { name: 'R Markdown', tab: RMARKDOWN_TAB, panel: RMARKDOWN_PANEL },
    { name: 'Spelling', tab: SPELLING_TAB, panel: SPELLING_PANEL },
    { name: 'Sweave', tab: SWEAVE_TAB, panel: SWEAVE_PANEL },
  ];

  for (const { name, tab, panel } of SIMPLE_PANELS) {
    test(`${name} panel is accessible`, async ({ rstudioPage: page }) => {
      await openGlobalOptions(page);
      await page.locator(tab).click();
      await expect(page.locator(panel)).toBeVisible();
      await closeGlobalOptions(page);
    });
  }

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

  // The interpreters modal only opens once python_find_interpreters has
  // scanned the machine, with a "Finding interpreters..." progress box up in
  // the meantime. On a cold CI runner that scan can run well past 15s (seen
  // flaking at 15s on desktop-linux while the progress box was still showing,
  // with the retry passing in under 4s) -- this, not a Server-specific
  // behavior, is also what #18064 hit, so the modal wait gets a
  // discovery-sized timeout and the test runs on Server again.
  test('Python panel and interpreter selector are accessible', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(PYTHON_TAB).click();
    await expect(page.locator(PYTHON_PANEL)).toBeVisible();
    await expect(page.locator(PYTHON_INTERPRETER_PATH)).toBeVisible();
    await expect(page.locator(PYTHON_INTERPRETER_SELECT_BTN)).toBeVisible();
    await page.locator(PYTHON_INTERPRETER_SELECT_BTN).click();
    await expect(page.locator(PYTHON_INTERPRETERS_MODAL)).toBeVisible({ timeout: 60000 });
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

  // Regression test for #17929: the completions-delay field was constructed and
  // wired for show/hide but never added to the layout, so it never rendered.
  test('Assistant completions delay field follows the trigger selector', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(ASSISTANT_TAB).click();
    await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();

    // The completions-trigger and delay controls only render once a code
    // assistant is selected; GitHub Copilot is always offered.
    await page.locator(ASSISTANT_CODE_ASSISTANT_SELECT).selectOption({ label: ASSISTANT_COPILOT_OPTION });

    const delayField = page.locator(ASSISTANT_PANEL).getByText(ASSISTANT_COMPLETIONS_DELAY_LABEL);
    const triggerSelect = page.locator(ASSISTANT_COMPLETIONS_TRIGGER_SELECT);

    // Shown for automatic completions (the default trigger)...
    await triggerSelect.selectOption({ label: 'Automatically' });
    await expect(delayField).toBeVisible();

    // ...and hidden for manual completions, where the delay does not apply.
    await triggerSelect.selectOption({ label: 'Manually' });
    await expect(delayField).toBeHidden();

    await triggerSelect.selectOption({ label: 'Automatically' });
    await expect(delayField).toBeVisible();

    await closeGlobalOptions(page);
  });

  // createCommonSettingsPanel() sets the field's visibility at layout time, not
  // via a change event. Open with the assistant and trigger already configured
  // so the field must render visible -- showing its saved value -- with no
  // selectOption interaction at all.
  test('Assistant completions delay field renders on open for automatic completions', async ({ rstudioPage: page }) => {
    await setPref(page, 'assistant', 'copilot');
    await setPref(page, 'assistant_completions_trigger', 'auto');
    await setPref(page, 'assistant_completions_delay', 1234);
    try {
      await openGlobalOptions(page);
      await page.locator(ASSISTANT_TAB).click();
      await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();

      const delayInput = page.locator(ASSISTANT_PANEL).locator(ASSISTANT_COMPLETIONS_DELAY_INPUT);
      await expect(delayInput).toBeVisible();
      await expect(delayInput).toHaveValue('1234');

      await closeGlobalOptions(page);
    } finally {
      await clearPref(page, 'assistant_completions_delay');
      await clearPref(page, 'assistant_completions_trigger');
      await clearPref(page, 'assistant');
    }
  });

  // Now that the delay field is editable, its 10-5000 bounds must be enforced
  // when applying the dialog (follow-up to the #17929 fix). The overflow case
  // guards against a digit string that passes the digit check but overflows the
  // integer parse, which must still be rejected rather than throwing.
  test('Assistant completions delay rejects out-of-range and overflowing values', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(ASSISTANT_TAB).click();
    await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();

    await page.locator(ASSISTANT_CODE_ASSISTANT_SELECT).selectOption({ label: ASSISTANT_COPILOT_OPTION });
    await page.locator(ASSISTANT_COMPLETIONS_TRIGGER_SELECT).selectOption({ label: 'Automatically' });

    const delayInput = page.locator(ASSISTANT_PANEL).locator(ASSISTANT_COMPLETIONS_DELAY_INPUT);
    const errorOk = page.locator('#rstudio_dlg_ok');

    // Each entry must block the apply with a range error (not crash): the error
    // modal appears and the options dialog stays open.
    const expectRejectedOnApply = async (entered: string) => {
      await delayInput.fill(entered);
      await page.locator(OPTIONS_OK).click();
      await expect(errorOk).toBeVisible();
      await expect(page.locator(OPTIONS_OK)).toBeVisible();
      await errorOk.click();
      await expect(errorOk).toBeHidden();
    };

    await expectRejectedOnApply('99999');                  // above the maximum
    await expectRejectedOnApply('999999999999999999999');  // overflows int -- must not throw

    await closeGlobalOptions(page);
  });

  // A field with no explicit maximum (the always-visible update-check interval)
  // must also reject a digit string that overflows int rather than throwing or
  // silently dropping it -- the effective maximum is Integer.MAX_VALUE.
  test('Assistant update check interval rejects an int-overflowing value', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(ASSISTANT_TAB).click();
    await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();

    await page.locator(ASSISTANT_PANEL).locator(ASSISTANT_UPDATE_CHECK_INPUT).fill('999999999999999999999');
    await page.locator(OPTIONS_OK).click();

    // Rejected with a range error instead of throwing; the dialog stays open.
    const errorOk = page.locator('#rstudio_dlg_ok');
    await expect(errorOk).toBeVisible();
    await expect(page.locator(OPTIONS_OK)).toBeVisible();
    await errorOk.click();

    await closeGlobalOptions(page);
  });

  // The delay field keeps its text while hidden, so an out-of-range or
  // non-numeric entry is clamped back into the supported range when the field
  // is hidden (manual trigger). This keeps the value the apply path persists in
  // range even when the hidden field is skipped by validation.
  test('Assistant completions delay clamps out-of-range and non-numeric values when hidden', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);
    await page.locator(ASSISTANT_TAB).click();
    await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();

    await page.locator(ASSISTANT_CODE_ASSISTANT_SELECT).selectOption({ label: ASSISTANT_COPILOT_OPTION });
    const triggerSelect = page.locator(ASSISTANT_COMPLETIONS_TRIGGER_SELECT);
    const delayInput = page.locator(ASSISTANT_PANEL).locator(ASSISTANT_COMPLETIONS_DELAY_INPUT);

    await triggerSelect.selectOption({ label: 'Automatically' });
    // Capture the value the field shows now; the non-numeric case below expects
    // the clamp to fall back to it.
    const savedValue = await delayInput.inputValue();

    // The clamped value is observable only while the field is shown, so each
    // case enters a value under Automatically, hides it via Manually, then
    // returns to Automatically to read it back.
    const expectClampedWhenHidden = async (entered: string, expected: string) => {
      await triggerSelect.selectOption({ label: 'Automatically' });
      await delayInput.fill(entered);
      await triggerSelect.selectOption({ label: 'Manually' });
      await triggerSelect.selectOption({ label: 'Automatically' });
      await expect(delayInput).toHaveValue(expected);
    };

    await expectClampedWhenHidden('99999', '5000');     // above the maximum
    await expectClampedWhenHidden('5', '10');           // below the minimum
    // A non-numeric entry is not a parseable int, so the clamp falls back to the
    // saved value captured above rather than throwing.
    await expectClampedWhenHidden('', savedValue);

    await closeGlobalOptions(page);
  });

  // The apply-time clamp (onApply) is the authoritative guard: switching the
  // assistant away detaches the delay field so validate() skips it, yet an
  // out-of-range value entered beforehand must not be persisted. This applies
  // the dialog, so it clears the prefs it changes afterward (revert to default).
  test('Assistant completions delay is clamped before it is persisted', async ({ rstudioPage: page }) => {
    try {
      await openGlobalOptions(page);
      await page.locator(ASSISTANT_TAB).click();
      await expect(page.locator(ASSISTANT_PANEL)).toBeVisible();

      await page.locator(ASSISTANT_CODE_ASSISTANT_SELECT).selectOption({ label: ASSISTANT_COPILOT_OPTION });
      await page.locator(ASSISTANT_COMPLETIONS_TRIGGER_SELECT).selectOption({ label: 'Automatically' });
      await page.locator(ASSISTANT_PANEL).locator(ASSISTANT_COMPLETIONS_DELAY_INPUT).fill('99999');

      // Switch the assistant back to (None) so the delay field is detached --
      // not just hidden -- which is the path validate() cannot guard. Then apply.
      await page.locator(ASSISTANT_CODE_ASSISTANT_SELECT).selectOption({ label: ASSISTANT_NONE_OPTION });
      await page.locator(OPTIONS_OK).click();
      await expect(page.locator(OPTIONS_OK)).toBeHidden({ timeout: 15000 });

      // Reopen and confirm the persisted value was clamped to the 5000 maximum,
      // not saved as 99999. (The integer pref cannot be read back through the
      // automation bridge, so the round-tripped field value is the assertion.)
      await openGlobalOptions(page);
      await page.locator(ASSISTANT_TAB).click();
      await page.locator(ASSISTANT_CODE_ASSISTANT_SELECT).selectOption({ label: ASSISTANT_COPILOT_OPTION });
      await page.locator(ASSISTANT_COMPLETIONS_TRIGGER_SELECT).selectOption({ label: 'Automatically' });
      await expect(page.locator(ASSISTANT_PANEL).locator(ASSISTANT_COMPLETIONS_DELAY_INPUT)).toHaveValue('5000');

      await closeGlobalOptions(page);
    } finally {
      // Revert the prefs this test may have changed (clear to defaults) so this
      // apply-based test leaks no state to later tests.
      for (const pref of [
        'assistant',
        'chat_provider',
        'assistant_tab_key_behavior',
        'assistant_completions_trigger',
        'assistant_nes_autoshow',
        'assistant_completions_delay',
        'copilot_enabled',
        'copilot_tab_key_behavior',
        'copilot_completions_trigger',
      ]) {
        await clearPref(page, pref);
      }
    }
  });
});

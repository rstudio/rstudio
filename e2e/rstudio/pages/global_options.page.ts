import type { Page } from 'playwright';
import { executeCommand } from '@utils/commands';

// Dialog chrome
export const GLOBAL_OPTIONS_DIALOG = '#rstudio_dialog_global_prefs';
export const DIALOG_BOX = '.gwt-DialogBox';
export const OPTIONS_OK = '#rstudio_preferences_confirm';
export const OPTIONS_CANCEL = '#rstudio_dlg_cancel';
export const OPTIONS_APPLY = '#rstudio_dlg_apply';

// Appearance
export const APPEARANCE_TAB = '#rstudio_label_appearance_options';
export const APPEARANCE_PANEL = '#rstudio_label_appearance_options_panel';
export const APPEARANCE_PREVIEW = 'iframe[title="Editor Theme Preview"]';

// Code
export const CODE_TAB = '#rstudio_label_code_options';
export const CODE_PANEL = '#rstudio_label_code_options_panel';
export const CODE_EDITING_TAB = '#rstudio_edit_editing_prefs_tab';
export const CODE_EDITING_PANEL = '#rstudio_edit_editing_prefs_panel';
export const CODE_DISPLAY_TAB = '#rstudio_edit_display_prefs_tab';
export const CODE_DISPLAY_PANEL = '#rstudio_edit_display_prefs_panel';
export const CODE_SAVING_TAB = '#rstudio_edit_saving_prefs_tab';
export const CODE_SAVING_PANEL = '#rstudio_edit_saving_prefs_panel';
export const CODE_CHANGE_ENCODING_BTN = '#rstudio_tbb_button_text_encoding';
export const CODE_CHANGE_ENCODING_MODAL = 'div.gwt-DialogBox[aria-label="Choose Encoding"]';
export const CODE_COMPLETION_TAB = '#rstudio_editing_completion_prefs_tab';
export const CODE_COMPLETION_PANEL = '#rstudio_editing_completion_prefs_panel';
export const CODE_DIAGNOSTICS_TAB = '#rstudio_editing_diagnostics_prefs_tab';
export const CODE_DIAGNOSTICS_PANEL = '#rstudio_editing_diagnostics_prefs_panel';

// General
export const GENERAL_TAB = '#rstudio_label_general_options';
export const GENERAL_PANEL = '#rstudio_label_general_options_panel';
export const GENERAL_ADVANCED_TAB = '#rstudio_general_advanced_prefs_tab';
export const GENERAL_ADVANCED_PANEL = '#rstudio_general_advanced_prefs_panel';

// Packages
export const PACKAGES_TAB = '#rstudio_label_packages_options';
export const PACKAGES_PANEL = '#rstudio_label_packages_options_panel';
export const PACKAGES_MANAGEMENT_TAB = '#rstudio_package_management_prefs_tab';
export const PACKAGES_MANAGEMENT_PANEL = '#rstudio_package_management_prefs_panel';
export const PACKAGES_DEVELOPMENT_TAB = '#rstudio_package_development_prefs_tab';
export const PACKAGES_DEVELOPMENT_PANEL = '#rstudio_package_development_prefs_panel';

// Pane Layout
export const PANE_LAYOUT_TAB = '#rstudio_label_pane_layout_options';
export const PANE_LAYOUT_PANEL = '#rstudio_label_pane_layout_options_panel';

// R Markdown
export const RMARKDOWN_TAB = '#rstudio_label_r_markdown_options';
export const RMARKDOWN_PANEL = '#rstudio_label_r_markdown_options_panel';

// Spelling
export const SPELLING_TAB = '#rstudio_label_spelling_options';
export const SPELLING_PANEL = '#rstudio_label_spelling_options_panel';

// Sweave
export const SWEAVE_TAB = '#rstudio_label_sweave_options';
export const SWEAVE_PANEL = '#rstudio_label_sweave_options_panel';

// Terminal
export const TERMINAL_TAB = '#rstudio_label_terminal_options';
export const TERMINAL_PANEL = '#rstudio_label_terminal_options_panel';
export const TERMINAL_GENERAL_TAB = '#rstudio_terminal_general_prefs_tab';
export const TERMINAL_GENERAL_PANEL = '#rstudio_terminal_general_prefs_panel';
export const TERMINAL_CLOSING_TAB = '#rstudio_terminal_closing_prefs_tab';
export const TERMINAL_CLOSING_PANEL = '#rstudio_terminal_closing_prefs_panel';

// Python
export const PYTHON_TAB = '#rstudio_label_python_options';
export const PYTHON_PANEL = '#rstudio_label_python_options_panel';
export const PYTHON_INTERPRETER_PATH = '#rstudio_tbb_text_python_path';
export const PYTHON_INTERPRETER_SELECT_BTN = '#rstudio_tbb_button_python_path';
export const PYTHON_INTERPRETERS_MODAL = '[aria-label="Python Interpreters"]';

// Assistant (code assistant / GitHub Copilot configuration)
// ASSISTANT_TAB/ASSISTANT_PANEL and OPTIONS_OK mirror selectors in
// pages/assistant_options.page.ts; keep both in sync if these IDs change.
export const ASSISTANT_TAB = '#rstudio_label_assistant_options';
export const ASSISTANT_PANEL = '#rstudio_label_assistant_options_panel';
export const ASSISTANT_LABEL = 'Use code assistant:';
// The completions-trigger selector and delay field render only once a code
// assistant (not "None") is selected.
export const ASSISTANT_CODE_ASSISTANT_SELECT =
  "xpath=//label[contains(text(),'Use code assistant')]/following::select[1]";
export const ASSISTANT_COMPLETIONS_TRIGGER_SELECT =
  "xpath=//label[contains(text(),'Show code suggestions:')]/following::select[1]";
export const ASSISTANT_COMPLETIONS_DELAY_LABEL = 'Show code suggestions after keyboard idle';
// Relative to ASSISTANT_PANEL: the delay value input that follows its label.
export const ASSISTANT_COMPLETIONS_DELAY_INPUT =
  "xpath=.//*[contains(text(),'Show code suggestions after keyboard idle')]/following::input[1]";
export const ASSISTANT_COPILOT_OPTION = 'GitHub Copilot';
export const ASSISTANT_NONE_OPTION = '(None)';
// Always-visible numeric field with no explicit maximum (Chat section).
// Relative to ASSISTANT_PANEL: the value input that follows its label.
export const ASSISTANT_UPDATE_CHECK_INPUT =
  "xpath=.//*[contains(text(),'update check interval')]/following::input[1]";

// Actions
export async function openGlobalOptions(page: Page): Promise<void> {
  await executeCommand(page, 'showOptions');
  await page.waitForSelector(OPTIONS_OK, { timeout: 15000 });
}

export async function closeGlobalOptions(page: Page): Promise<void> {
  await page.locator(OPTIONS_CANCEL).click();
  await page.waitForSelector(GLOBAL_OPTIONS_DIALOG, { state: 'detached', timeout: 10000 });
}

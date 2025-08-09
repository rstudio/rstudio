/*
 * PrefsConstants.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs;

public interface PrefsConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Verify Key...".
     *
     * @return translated "Verify Key..."
     */
    String verifyKey();

    /**
     * Translated "Unable to verify Zotero API key.\n\nYou should verify that your API key is still valid, and if necessary create a new key.".
     *
     * @return translated "Unable to verify Zotero API key.\n\nYou should verify that your API key is still valid, and if necessary create a new key."
     */
    String zoteroVerifyKeyFailedMessage();

    /**
     * Translated "Zotero Web API Key:".
     *
     * @return translated "Zotero Web API Key:"
     */
    String zoteroWebApiKey();

    /**
     * Translated "Verifying Key...".
     *
     * @return translated "Verifying Key..."
     */
    String verifyingKey();

    /**
     * Translated "Zotero".
     *
     * @return translated "Zotero"
     */
    String zotero();

    /**
     * Translated "Zotero API key successfully verified.".
     *
     * @return translated "Zotero API key successfully verified."
     */
    String zoteroKeyVerified();

    /**
     * Translated "Use libraries:".
     *
     * @return translated "Use libraries:"
     */
    String useLibraries();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String error();

    /**
     * Translated "You must select at least one Zotero library".
     *
     * @return translated "You must select at least one Zotero library"
     */
    String selectOneZoteroLibrary();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    String defaultInParentheses();

    /**
     * Translated "My Library".
     *
     * @return translated "My Library"
     */
    String myLibrary();

    /**
     * Translated "Selected Libraries".
     *
     * @return translated "Selected Libraries"
     */
    String selectedLibraries();

    /**
     * Translated "Conda Environment".
     *
     * @return translated "Conda Environment"
     */
    String condaEnvironment();

    /**
     * Translated "Virtual Environment".
     *
     * @return translated "Virtual Environment"
     */
    String virtualEnvironment();

    /**
     * Translated "Python Interpreter".
     *
     * @return translated "Python Interpreter"
     */
    String pythonInterpreter();

    /**
     * Translated "System".
     *
     * @return translated "System"
     */
    String system();

    /**
     * Translated "Virtual Environments".
     *
     * @return translated "Virtual Environments"
     */
    String virtualEnvironmentPlural();

    /**
     * Translated "Conda Environments".
     *
     * @return translated "Conda Environments"
     */
    String condaEnvironmentPlural();

    /**
     * Translated "Python Interpreters".
     *
     * @return translated "Python Interpreters"
     */
    String pythonInterpreterPlural();

    /**
     * Translated "Select".
     *
     * @return translated "Select"
     */
    String select();

    /**
     * Translated "(None available)".
     *
     * @return translated "(None available)"
     */
    String noneAvailableParentheses();

    /**
     * Translated "Editor Theme Preview".
     *
     * @return translated "Editor Theme Preview"
     */
    String editorThemePreview();

    /**
     * Translated "Spelling Prefs".
     *
     * @return translated "Spelling Prefs"
     */
    String spellingPrefsTitle();

    /**
     * Translated "The context for the user''s spelling preferences.".
     *
     * @return translated "The context for the user''s spelling preferences."
     */
    String spellingPrefsDescription();

    /**
     * Translated "SSH Public Key Filename".
     *
     * @return translated "SSH Public Key Filename"
     */
    String rsaKeyFileTitle();

    /**
     * Translated "Filename of SSH public key".
     *
     * @return translated "Filename of SSH public key"
     */
    String rsaKeyFileDescription();

    /**
     * Translated "Has SSH Key".
     *
     * @return translated "Has SSH Key"
     */
    String haveRSAKeyTitle();

    /**
     * Translated "Whether the user has an SSH key".
     *
     * @return translated "Whether the user has an SSH key"
     */
    String haveRSAKeyDescription();

    /**
     * Translated "Error Changing Setting".
     *
     * @return translated "Error Changing Setting"
     */
    String errorChangingSettingCaption();

    /**
     * Translated "The tab key moves focus setting could not be updated.".
     *
     * @return translated "The tab key moves focus setting could not be updated."
     */
    String tabKeyErrorMessage();

    /**
     * Translated "Tab key always moves focus on".
     *
     * @return translated "Tab key always moves focus on"
     */
    String tabKeyFocusOnMessage();

    /**
     * Translated "Tab key always moves focus off".
     *
     * @return translated "Tab key always moves focus off"
     */
    String tabKeyFocusOffMessage();

    /**
     * Translated "The screen reader support setting could not be changed.".
     *
     * @return translated "The screen reader support setting could not be changed."
     */
    String toggleScreenReaderErrorMessage();

    /**
     * Translated "Confirm Toggle Screen Reader Support".
     *
     * @return translated "Confirm Toggle Screen Reader Support"
     */
    String toggleScreenReaderConfirmCaption();

    /**
     * Translated "Are you sure you want to {0} screen reader support? The application will reload to apply the change.".
     *
     * @return translated "Are you sure you want to {0} screen reader support? The application will reload to apply the change."
     */
    String toggleScreenReaderMessageConfirmDialog(String value);

    /**
     * Translated "disable".
     *
     * @return translated "disable"
     */
    String disable();

    /**
     * Translated "enable".
     *
     * @return translated "enable"
     */
    String enable();

    /**
     * Translated "Warning: screen reader mode not enabled. Turn on using shortcut {0}.".
     *
     * @return translated "Warning: screen reader mode not enabled. Turn on using shortcut {0}."
     */
    String announceScreenReaderStateMessage(String shortcut);

    /**
     * Translated "{0} (enabled)".
     *
     * @return translated "{0} (enabled)"
     */
    String screenReaderStateEnabled(String screenReaderLabel);

    /**
     * Translated "{0} (disabled)".
     *
     * @return translated "{0} (disabled)"
     */
    String screenReaderStateDisabled(String screenReaderLabel);

    /**
     * Translated "Clear Preferences".
     *
     * @return translated "Clear Preferences"
     */
    String onClearUserPrefsYesLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    String cancel();

    /**
     * Translated "Restart R".
     *
     * @return translated "Restart R"
     */
    String onClearUserPrefsRestartR();

    /**
     * Translated "Preferences Cleared".
     *
     * @return translated "Preferences Cleared"
     */
    String onClearUserPrefsResponseCaption();

    /**
     * Translated "Your preferences have been cleared, and your R session will now be restarted.
     * A backup copy of your preferences can be found at: \n\n{0}".
     *
     * @return translated "Your preferences have been cleared, and your R session will now be restarted.
     * A backup copy of your preferences can be found at: \n\n{0}"
     */
    String onClearUserPrefsResponseMessage(String path);

    /**
     * Translated "Confirm Clear Preferences".
     *
     * @return translated "Confirm Clear Preferences"
     */
    String onClearUserPrefsCaption();

    /**
     * Translated "Are you sure you want to clear your preferences?
     * All RStudio settings will be restored to their defaults, and your R session will be restarted.".
     *
     * @return translated "Are you sure you want to clear your preferences?
     * All RStudio settings will be restored to their defaults, and your R session will be restarted."
     */
    String onClearUserPrefsMessage();

    /**
     * Translated "Using Zotero".
     *
     * @return translated "Using Zotero"
     */
    String usingZotero();

    /**
     * Translated "Zotero Library:".
     *
     * @return translated "Zotero Library:"
     */
    String zoteroLibrary();

    /**
     * Translated "Web".
     *
     * @return translated "Web"
     */
    String web();

    /**
     * Translated "Local".
     *
     * @return translated "Local"
     */
    String local();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    String noneParentheses();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String general();

    /**
     * Translated "Line ending conversion:".
     *
     * @return translated "Line ending conversion:"
     */
    String lineEndingConversion();

    /**
     * Translated "(Use Default)".
     *
     * @return translated "(Use Default)"
     */
    String useDefaultParentheses();

    /**
     * Translated "None".
     *
     * @return translated "None"
     */
    String none();

    /**
     * Translated "Platform Native".
     *
     * @return translated "Platform Native"
     */
    String platformNative();

    /**
     * Translated "Posix (LF)".
     *
     * @return translated "Posix (LF)"
     */
    String posixLF();

    /**
     * Translated "Windows (CR/LF)".
     *
     * @return translated "Windows (CR/LF)"
     */
    String windowsCRLF();

    /**
     * Translated "Options".
     *
     * @return translated "Options"
     */
    String options();

    /**
     * Translated "Assistive Tools".
     *
     * @return translated "Assistive Tools"
     */
    String generalHeaderPanel();

    /**
     * Translated "Screen reader support (requires restart)".
     *
     * @return translated "Screen reader support (requires restart)"
     */
    String chkScreenReaderLabel();

    /**
     * Translated "Milliseconds after typing before speaking results".
     *
     * @return translated "Milliseconds after typing before speaking results"
     */
    String typingStatusDelayLabel();

    /**
     * Translated "Maximum number of console output lines to read".
     *
     * @return translated "Maximum number of console output lines to read"
     */
    String maxOutputLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    String displayLabel();

    /**
     * Translated "Reduce user interface animations".
     *
     * @return translated "Reduce user interface animations"
     */
    String reducedMotionLabel();

    /**
     * Translated "Tab key always moves focus".
     *
     * @return translated "Tab key always moves focus"
     */
    String chkTabMovesFocusLabel();

    /**
     * Translated "Highlight focused panel".
     *
     * @return translated "Highlight focused panel"
     */
    String generalPanelLabel();

    /**
     * Translated "RStudio accessibility help".
     *
     * @return translated "RStudio accessibility help"
     */
    String helpRStudioAccessibilityLinkLabel();

    /**
     * Translated "Enable / Disable Announcements".
     *
     * @return translated "Enable / Disable Announcements"
     */
    String announcementsLabel();

    /**
     * Translated "Accessibility".
     *
     * @return translated "Accessibility"
     */
    String tabHeaderPanel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String generalPanelText();

    /**
     * Translated "Announcements".
     *
     * @return translated "Announcements"
     */
    String announcementsPanelText();

    /**
     * Translated "RStudio theme:".
     *
     * @return translated "RStudio theme:"
     */
    String appearanceRStudioThemeLabel();

    /**
     * Translated "Zoom:".
     *
     * @return translated "Zoom:"
     */
    String appearanceZoomLabelZoom();

    /**
     * Translated "Editor font (loading...):".
     *
     * @return translated "Editor font (loading...):"
     */
    String fontFaceEditorFontLabel();

    /**
     * Translated "Editor font:".
     *
     * @return translated "Editor font:"
     */
    String appearanceEditorFontLabel();

    /**
     * Translated "Font size:".
     *
     * @return translated "Font size:"
     */
    String appearanceEditorFontSizeLabel();

    /**
     * Translated "Line height (%):".
     *
     * @return translated "Line height (%):"
     */
    String appearanceEditorLineHeightLabel();
    
    /**
     * Translated "Editor theme:".
     *
     * @return translated "Editor theme:"
     */
    String appearanceEditorThemeLabel();

    /**
     * Translated "Add...".
     *
     * @return translated "Add..."
     */
    String addThemeButtonLabel();

    /**
     * Translated "Theme Files (*.tmTheme *.rstheme)".
     *
     * @return translated "Theme Files (*.tmTheme *.rstheme)"
     */
    String addThemeButtonCaption();

    /**
     * Translated "Remove".
     *
     * @return translated "Remove"
     */
    String removeThemeButtonLabel();

    /**
     * Translated "Converting a tmTheme to an rstheme".
     *
     * @return translated "Converting a tmTheme to an rstheme"
     */
    String addThemeUserActionLabel();

    /**
     * Translated "The active theme "{0}" could not be found. It''s possible it was removed outside the context of RStudio. Switching to the {1} default theme: "".
     *
     * @return translated "The active theme "{0}" could not be found. It''s possible it was removed outside the context of RStudio. Switching to the {1} default theme: ""
     */
    String setThemeWarningMessage(String name, String currentTheme);

    /**
     * Translated "dark".
     *
     * @return translated "dark"
     */
    String themeWarningMessageDarkLabel();

    /**
     * Translated "light".
     *
     * @return translated "light"
     */
    String themeWarningMessageLightLabel();

    /**
     * Translated "A theme file with the same name, ''{0}'', already exists. Adding the theme will cause the existing file to be overwritten. Would you like to add the theme anyway?".
     *
     * @return translated "A theme file with the same name, ''{0}'', already exists. Adding the theme will cause the existing file to be overwritten. Would you like to add the theme anyway?"
     */
    String showThemeExistsDialogLabel(String inputFileName);

    /**
     * Translated "Theme File Already Exists".
     *
     * @return translated "Theme File Already Exists"
     */
    String globalDisplayThemeExistsCaption();

    /**
     * Translated "Unable to add the theme ''".
     *
     * @return translated "Unable to add the theme ''"
     */
    String cantAddThemeMessage();


    /**
     * Translated "''. The following error occurred: ".
     *
     * @return translated "''. The following error occurred: "
     */
    String cantAddThemeErrorCaption();

    /**
     * Translated "Failed to Add Theme".
     *
     * @return translated "Failed to Add Theme"
     */
    String cantAddThemeGlobalMessage();

    /**
     * Translated "Unable to remove the theme ''{0}'': {1}".
     *
     * @return translated "Unable to remove the theme ''{0}'': {1}"
     */
    String showCantRemoveThemeDialogMessage(String themeName, String errorMessage);

    /**
     * Translated "Failed to Remove Theme".
     *
     * @return translated "Failed to Remove Theme"
     */
    String showCantRemoveErrorMessage();

    /**
     * Translated "The theme "{0}" cannot be removed because it is currently in use. To delete this theme, please change the active theme and retry.".
     *
     * @return translated "The theme "{0}" cannot be removed because it is currently in use. To delete this theme, please change the active theme and retry."
     */
    String showCantRemoveActiveThemeDialog(String themeName);

    /**
     * Translated "Cannot Remove Active Theme".
     *
     * @return translated "Cannot Remove Active Theme"
     */
    String showCantRemoveThemeCaption();

    /**
     * Translated "Taking this action will delete the theme "{0}" and cannot be undone. Are you sure you wish to continue?".
     *
     * @return translated "Taking this action will delete the theme "{0}" and cannot be undone. Are you sure you wish to continue?"
     */
    String showRemoveThemeWarningMessage(String themeName);

    /**
     * Translated "Remove Theme".
     *
     * @return translated "Remove Theme"
     */
    String showRemoveThemeGlobalMessage();

    /**
     * Translated "There is an existing theme with the same name as the new theme in the current location. Would you like remove the existing theme, "{0}", and add the new theme?".
     *
     * @return translated "There is an existing theme with the same name as the new theme in the current location. Would you like remove the existing theme, "{0}", and add the new theme?"
     */
    String showDuplicateThemeErrorMessage(String themeName);

    /**
     * Translated "Duplicate Theme In Same Location".
     *
     * @return translated "Duplicate Theme In Same Location"
     */
    String showDuplicateThemeDuplicateGlobalMessage();

    /**
     * Translated "There is an existing theme with the same name as the new theme, ".
     *
     * @return translated "There is an existing theme with the same name as the new theme, "
     */
    String showDuplicateThemeWarningMessage(String themeName);

    /**
     * Translated "Duplicate Theme In Another Location".
     *
     * @return translated "Duplicate Theme In Another Location"
     */
    String showDuplicateThemeGlobalMessage();

    /**
     * Translated "Appearance".
     *
     * @return translated "Appearance"
     */
    String appearanceLabel();

    /**
     * Translated "Editor font:".
     *
     * @return translated "Editor font:"
     */
    String editorFontLabel();

    /**
     * Translated "PDF Generation".
     *
     * @return translated "PDF Generation"
     */
    String headerPDFGenerationLabel();

    /**
     * Translated "NOTE: The Rnw weave and LaTeX compilation options are also set on a per-project (and optionally per-file) basis. Click the help icons above for more details.".
     *
     * @return translated "NOTE: The Rnw weave and LaTeX compilation options are also set on a per-project (and optionally per-file) basis. Click the help icons above for more details."
     */
    String perProjectNoteLabel();

    /**
     * Translated "LaTeX Editing and Compilation".
     *
     * @return translated "LaTeX Editing and Compilation"
     */
    String perProjectHeaderLabel();

    /**
     * Translated "Use tinytex when compiling .tex files".
     *
     * @return translated "Use tinytex when compiling .tex files"
     */
    String chkUseTinytexLabel();

    /**
     * Translated "Clean auxiliary output after compile".
     *
     * @return translated "Clean auxiliary output after compile"
     */
    String chkCleanTexi2DviOutputLabel();

    /**
     * Translated "Enable shell escape commands".
     *
     * @return translated "Enable shell escape commands"
     */
    String chkEnableShellEscapeLabel();

    /**
     * Translated "Insert numbered sections and subsections".
     *
     * @return translated "Insert numbered sections and subsections"
     */
    String insertNumberedLatexSectionsLabel();

    /**
     * Translated "PDF Preview".
     *
     * @return translated "PDF Preview"
     */
    String previewingOptionsHeaderLabel();

    /**
     * Translated "Always enable Rnw concordance (required for synctex)".
     *
     * @return translated "Always enable Rnw concordance (required for synctex)"
     */
    String alwaysEnableRnwConcordanceLabel();

    /**
     * Translated "Preview PDF after compile using:".
     *
     * @return translated "Preview PDF after compile using:"
     */
    String pdfPreviewSelectWidgetLabel();

    /**
     * Translated "Help on previewing PDF files".
     *
     * @return translated "Help on previewing PDF files"
     */
    String pdfPreviewHelpButtonTitle();

    /**
     * Translated "Sweave".
     *
     * @return translated "Sweave"
     */
    String preferencesPaneTitle();

    /**
     * Translated "(No Preview)".
     *
     * @return translated "(No Preview)"
     */
    String pdfNoPreviewOption();

    /**
     * Translated "(Recommended)".
     *
     * @return translated "(Recommended)"
     */
    String pdfPreviewSumatraOption();

    /**
     * Translated "RStudio Viewer".
     *
     * @return translated "RStudio Viewer"
     */
    String pdfPreviewRStudioViewerOption();

    /**
     * Translated "System Viewer".
     *
     * @return translated "System Viewer"
     */
    String pdfPreviewSystemViewerOption();

    /**
     * Translated "Execution".
     *
     * @return translated "Execution"
     */
    String consoleExecutionLabel();
    
    /**
     * Translated "Discard pending console input on error".
     *
     * @return translated "Discard pending console input on error"
     */
    String consoleDiscardPendingConsoleInputOnErrorLabel();
    
    
    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    String consoleDisplayLabel();
    
    /**
     * Translated "Highlight".
     *
     * @return translated "Highlight"
     */
    String consoleHighlightLabel();

    /**
     * Translated "Show syntax highlighting in console input".
     *
     * @return translated "Show syntax highlighting in console input"
     */
    String consoleSyntaxHighlightingLabel();

    /**
     * Translated "Different color for error or message output (requires restart)".
     *
     * @return translated "Different color for error or message output (requires restart)"
     */
    String consoleDifferentColorLabel();

    /**
     * Translated "Limit visible console output (requires restart)".
     *
     * @return translated "Limit visible console output (requires restart)"
     */
    String consoleLimitVariableLabel();

    /**
     * Translated "Truncate lines to maximum length (characters)".
     *
     * @return translated "Truncate lines to maximum length (characters)"
     */
    String consoleLimitOutputLengthLabel();

    /**
     * Translated "Number of lines to show in console history:".
     *
     * @return translated "Number of lines to show in console history:"
     */
    String consoleMaxLinesLabel();

    /**
     * Translated "ANSI Escape Codes:".
     *
     * @return translated "ANSI Escape Codes:"
     */
    String consoleANSIEscapeCodesLabel();

    /**
     * Translated "Show ANSI colors".
     *
     * @return translated "Show ANSI colors"
     */
    String consoleColorModeANSIOption();

    /**
     * Translated "Remove ANSI codes".
     *
     * @return translated "Remove ANSI codes"
     */
    String consoleColorModeRemoveANSIOption();

    /**
     * Translated "Ignore ANSI codes (1.0 behavior)".
     *
     * @return translated "Ignore ANSI codes (1.0 behavior)"
     */
    String consoleColorModeIgnoreANSIOption();

    /**
     * Translated "Console".
     *
     * @return translated "Console"
     */
    String consoleLabel();

    /**
     * Translated "Debugging".
     *
     * @return translated "Debugging"
     */
    String debuggingHeaderLabel();

    /**
     * Translated "Automatically expand tracebacks in error inspector".
     *
     * @return translated "Automatically expand tracebacks in error inspector"
     */
    String debuggingExpandTracebacksLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    String otherHeaderCaption();

    /**
     * Translated "Double-click to select words".
     *
     * @return translated "Double-click to select words"
     */
    String otherDoubleClickLabel();

    /**
     * Translated "Warn when automatic session suspension is paused".
     *
     * @return translated "Warn when automatic session suspension is paused"
     */
    String warnAutoSuspendPausedLabel();

    /**
     * Translated "Number of seconds to delay warning".
     *
     * @return translated "Number of seconds to delay warning"
     */
    String numSecondsToDelayWarningLabel();

    /**
     * Translated "R Sessions".
     *
     * @return translated "R Sessions"
     */
    String rSessionsTitle();

    /**
     * Translated "R version".
     *
     * @return translated "R version"
     */
    String rVersionTitle();

    /**
     * Translated "Change...".
     *
     * @return translated "Change..."
     */
    String rVersionChangeTitle();

    /**
     * Translated "Change R Version".
     *
     * @return translated "Change R Version"
     */
    String rChangeVersionMessage();

    /**
     * Translated "You need to quit and re-open RStudio in order for this change to take effect.".
     *
     * @return translated "You need to quit and re-open RStudio in order for this change to take effect."
     */
    String rQuitReOpenMessage();

    /**
     * Translated "Loading...".
     *
     * @return translated "Loading..."
     */
    String rVersionLoadingText();

    /**
     * Translated "Restore last used R version for projects".
     *
     * @return translated "Restore last used R version for projects"
     */
    String rRestoreLabel();

    /**
     * Translated "working directory (when not in a project):".
     *
     * @return translated "working directory (when not in a project):"
     */
    String rDefaultDirectoryTitle();

    /**
     * Translated "Restore most recently opened project at startup".
     *
     * @return translated "Restore most recently opened project at startup"
     */
    String rRestorePreviousTitle();

    /**
     * Translated "Restore previously open source documents at startup".
     *
     * @return translated "Restore previously open source documents at startup"
     */
    String rRestorePreviousOpenTitle();

    /**
     * Translated "Run Rprofile when resuming suspended session".
     *
     * @return translated "Run Rprofile when resuming suspended session"
     */
    String rRunProfileTitle();

    /**
     * Translated "Workspace".
     *
     * @return translated "Workspace"
     */
    String workspaceCaption();

    /**
     * Translated "Restore .RData into workspace at startup".
     *
     * @return translated "Restore .RData into workspace at startup"
     */
    String workspaceLabel();

    /**
     * Translated "Save workspace to .RData on exit:".
     *
     * @return translated "Save workspace to .RData on exit:"
     */
    String saveWorkSpaceLabel();

    /**
     * Translated "Always".
     *
     * @return translated "Always"
     */
    String saveWorkAlways();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    String saveWorkNever();

    /**
     * Translated "Ask".
     *
     * @return translated "Ask"
     */
    String saveWorkAsk();

    /**
     * Translated "History".
     *
     * @return translated "History"
     */
    String historyCaption();

    /**
     * Translated "Always save history (even when not saving .RData)".
     *
     * @return translated "Always save history (even when not saving .RData)"
     */
    String alwaysSaveHistoryLabel();

    /**
     * Translated "Remove duplicate entries in history".
     *
     * @return translated "Remove duplicate entries in history"
     */
    String removeDuplicatesLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    String otherCaption();

    /**
     * Translated "Wrap around when navigating to previous/next tab".
     *
     * @return translated "Wrap around when navigating to previous/next tab"
     */
    String otherWrapAroundLabel();

    /**
     * Translated "Automatically notify me of updates to RStudio".
     *
     * @return translated "Automatically notify me of updates to RStudio"
     */
    String otherNotifyMeLabel();

    /**
     * Translated "Send automated crash reports to Posit".
     *
     * @return translated "Send automated crash reports to Posit"
     */
    String otherSendReportsLabel();

    /**
     * Translated "Graphics Device".
     *
     * @return translated "Graphics Device"
     */
    String graphicsDeviceCaption();

    /**
     * Translated "Antialiasing:".
     *
     * @return translated "Antialiasing:"
     */
    String graphicsAntialiasingLabel();

    /**
     * Translated "(Default):".
     *
     * @return translated "(Default):"
     */
    String antialiasingDefaultOption();

    /**
     * Translated "None".
     *
     * @return translated "None"
     */
    String antialiasingNoneOption();

    /**
     * Translated "Gray".
     *
     * @return translated "Gray"
     */
    String antialiasingGrayOption();

    /**
     * Translated "Subpixel".
     *
     * @return translated "Subpixel"
     */
    String antialiasingSubpixelOption();

    /**
     * Translated "Show server home page:".
     *
     * @return translated "Show server home page:"
     */
    String serverHomePageLabel();

    /**
     * Translated "Multiple active sessions".
     *
     * @return translated "Multiple active sessions"
     */
    String serverHomePageActiveSessionsOption();

    /**
     * Translated "Always".
     *
     * @return translated "Always"
     */
    String serverHomePageAlwaysOption();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    String serverHomePageNeverOption();

    /**
     * Translated "Re-use idle sessions for project links".
     *
     * @return translated "Re-use idle sessions for project links"
     */
    String reUseIdleSessionLabel();

    /**
     * Translated "Home Page".
     *
     * @return translated "Home Page"
     */
    String desktopCaption();

    /**
     * Translated "Debugging".
     *
     * @return translated "Debugging"
     */
    String advancedDebuggingCaption();

    /**
     * Translated "Use debug error handler only when my code contains errors".
     *
     * @return translated "Use debug error handler only when my code contains errors"
     */
    String advancedDebuggingLabel();

    /**
     * Translated "OS Integration".
     *
     * @return translated "OS Integration"
     */
    String advancedOsIntegrationCaption();

    /**
     * Translated "Rendering engine:".
     *
     * @return translated "Rendering engine:"
     */
    String advancedRenderingEngineLabel();

    /**
     * Translated "Auto-detect (recommended)".
     *
     * @return translated "Auto-detect (recommended)"
     */
    String renderingEngineAutoDetectOption();

    /**
     * Translated "Desktop OpenGL".
     *
     * @return translated "Desktop OpenGL"
     */
    String renderingEngineDesktopOption();

    /**
     * Translated "OpenGL for Embedded Systems".
     *
     * @return translated "OpenGL for Embedded Systems"
     */
    String renderingEngineLinuxDesktopOption();

    /**
     * Translated "Software".
     *
     * @return translated "Software"
     */
    String renderingEngineSoftwareOption();

    /**
     * Translated "Use GPU exclusion list (recommended)".
     *
     * @return translated "Use GPU exclusion list (recommended)"
     */
    String useGpuExclusionListLabel();

    /**
     * Translated "Use GPU driver bug workarounds (recommended)".
     *
     * @return translated "Use GPU driver bug workarounds (recommended)"
     */
    String useGpuDriverBugWorkaroundsLabel();

    /**
     * Translated "Enable X11 clipboard monitoring".
     *
     * @return translated "Enable X11 clipboard monitoring"
     */
    String clipboardMonitoringLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    String otherLabel();

    /**
     * Translated "Experimental Features".
     *
     * @return translated "Experimental Features"
     */
    String experimentalLabel();

    /**
     * Translated "English".
     *
     * @return translated "English"
     */
    String englishLabel();

    /**
     * Translated "French (Français)".
     *
     * @return translated "French (Français)"
     */
    String frenchLabel();

    /**
     * Translated "Show .Last.value in environment listing".
     *
     * @return translated "Show .Last.value in environment listing"
     */
    String otherShowLastDotValueLabel();

    /**
     * Translated "Help font size:".
     *
     * @return translated "Help font size:"
     */
    String helpFontSizeLabel();

    /**
     * Translated "General".
     *
     * @return "General"
     */
    String generalTabListLabel();

    /**
     * Translated "Basic".
     *
     * @return "Basic"
     */
    String generalTabListBasicOption();

    /**
     * Translated "Graphics".
     *
     * @return "Graphics"
     */
    String generalTabListGraphicsOption();

    /**
     * Translated "Advanced".
     *
     * @return "Advanced"
     */
    String generalTabListAdvancedOption();

    /**
     * Translated " (Default)".
     *
     * @return " (Default)"
     */
    String graphicsBackEndDefaultOption();

    /**
     * Translated "Quartz".
     *
     * @return "Quartz"
     */
    String graphicsBackEndQuartzOption();

    /**
     * Translated "Windows".
     *
     * @return "Windows"
     */
    String graphicsBackEndWindowsOption();

    /**
     * Translated "Cairo".
     *
     * @return "Cairo"
     */
    String graphicsBackEndCairoOption();

    /**
     * Translated "Cairo PNG".
     *
     * @return "Cairo PNG"
     */
    String graphicsBackEndCairoPNGOption();

    /**
     * Translated "AGG".
     *
     * @return "AGG"
     */
    String graphicsBackEndAGGOption();

    /**
     * Translated "Backend:".
     *
     * @return "Backend:"
     */
    String graphicsBackendLabel();

    /**
     * Translated "Using the AGG renderer".
     *
     * @return "Using the AGG renderer"
     */
    String graphicsBackendUserAction();

    /**
     * Translated "Browse...".
     *
     * @return "Browse..."
     */
    String browseLabel();

    /**
     * Translated "Choose Directory".
     *
     * @return "Choose Directory"
     */
    String directoryLabel();
    /**
     * Translated "Code".
     *
     * @return translated "Code"
     */
    String codePaneLabel();

    /**
     * Translated "Package Management".
     *
     * @return translated "Package Management"
     */
    String packageManagementTitle();

    /**
     * Translated "CRAN repositories modified outside package preferences.".
     *
     * @return translated "CRAN repositories modified outside package preferences."
     */
    String packagesInfoBarText();
    
    /**
     * Translated "Repositories are being managed by a renv.lock file".
     *
     * @return translated "Repositories are being managed by a renv.lock file"
     */
    String packagesRenvInfoBarText();

    /**
     * Translated "Primary CRAN repository:".
     *
     * @return translated "Primary CRAN repository:"
     */
    String cranMirrorTextBoxTitle();

    /**
     * Translated "Change...".
     *
     * @return translated "Change..."
     */
    String cranMirrorChangeLabel();

    /**
     * Translated "Secondary repositories:".
     *
     * @return translated "Secondary repositories:"
     */
    String secondaryReposTitle();

    /**
     * Translated "Enable packages pane".
     *
     * @return translated "Enable packages pane"
     */
    String chkEnablePackagesTitle();

    /**
     * Translated "Use secure download method for HTTP".
     *
     * @return translated "Use secure download method for HTTP"
     */
    String useSecurePackageDownloadTitle();

    /**
     * Translated "Help on secure package downloads for R".
     *
     * @return translated "Help on secure package downloads for R"
     */
    String useSecurePackageTitle();

    /**
     * Translated "Use Internet Explorer library/proxy for HTTP".
     *
     * @return translated "Use Internet Explorer library/proxy for HTTP"
     */
    String useInternetTitle();

    /**
     * Translated "Managing Packages".
     *
     * @return translated "Managing Packages"
     */
    String managePackagesTitle();

    /**
     * Translated "Package Development".
     *
     * @return translated "Package Development"
     */
    String developmentTitle();

    /**
     * Translated "C/C++ Development".
     *
     * @return translated "C/C++ Development"
     */
    String cppDevelopmentTitle();

    /**
     * Translated "Use devtools package functions if available".
     *
     * @return translated "Use devtools package functions if available"
     */
    String useDevtoolsLabel();

    /**
     * Translated "Save all files prior to building packages".
     *
     * @return translated "Save all files prior to building packages"
     */
    String developmentSaveLabel();

    /**
     * Translated "Automatically navigate editor to build errors".
     *
     * @return translated "Automatically navigate editor to build errors"
     */
    String developmentNavigateLabel();

    /**
     * Translated "Hide object files in package src directory".
     *
     * @return translated "Hide object files in package src directory"
     */
    String developmentHideLabel();

    /**
     * Translated "Cleanup output after successful R CMD check".
     *
     * @return translated "Cleanup output after successful R CMD check"
     */
    String developmentCleanupLabel();

    /**
     * Translated "View Rcheck directory after failed R CMD check".
     *
     * @return translated "View Rcheck directory after failed R CMD check"
     */
    String developmentViewLabel();

    /**
     * Translated "C++ template".
     *
     * @return translated "C++ template"
     */
    String developmentCppTemplate();

    /**
     * Translated "empty".
     *
     * @return translated "empty"
     */
    String developmentEmptyLabel();

    /**
     * Translated "Always use LF line-endings in Unix Makefiles".
     *
     * @return translated "Always use LF line-endings in Unix Makefiles"
     */
    String developmentUseLFLabel();

    /**
     * Translated "Packages".
     *
     * @return translated "Packages"
     */
    String tabPackagesPanelTitle();

    /**
     * Translated "Management".
     *
     * @return translated "Management"
     */
    String managementPanelTitle();

    /**
     * Translated "Development".
     *
     * @return translated "Development"
     */
    String developmentManagementPanelTitle();

    /**
     * Translated "C / C++".
     *
     * @return translated "C / C++"
     */
    String cppPanelTitle();

    /**
     * Translated "Retrieving list of CRAN mirrors...".
     *
     * @return translated "Retrieving list of CRAN mirrors..."
     */
    String chooseMirrorDialogMessage();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String showDisconnectErrorCaption();

    /**
     * Translated "Please select a CRAN Mirror".
     *
     * @return translated "Please select a CRAN Mirror"
     */
    String showDisconnectErrorMessage();

    /**
     * Translated "Validating CRAN repository...".
     *
     * @return translated "Validating CRAN repository..."
     */
    String progressIndicatorMessage();

    /**
     * Translated "The given URL does not appear to be a valid CRAN repository.".
     *
     * @return translated "The given URL does not appear to be a valid CRAN repository."
     */
    String progressIndicatorError();

    /**
     * Translated "Custom:".
     *
     * @return translated "Custom:"
     */
    String customLabel();

    /**
     * Translated "CRAN Mirrors:".
     *
     * @return translated "CRAN Mirrors:"
     */
    String mirrorsLabel();

    /**
     * Translated "Choose Primary Repository".
     *
     * @return translated "Choose Primary Repository"
     */
    String headerLabel();

    /**
     * Translated "Add...".
     *
     * @return translated "Add..."
     */
    String buttonAddLabel();

    /**
     * Translated "Remove...".
     *
     * @return translated "Remove..."
     */
    String buttonRemoveLabel();

    /**
     * Translated "Up".
     *
     * @return translated "Up"
     */
    String buttonUpLabel();

    /**
     * Translated "Down".
     *
     * @return translated "Down"
     */
    String buttonDownLabel();

    /**
     * Translated "Developing Packages".
     *
     * @return translated "Developing Packages"
     */
    String developingPkgHelpLink();

    /**
     * Translated "Retrieving list of secondary repositories...".
     *
     * @return translated "Retrieving list of secondary repositories..."
     */
    String secondaryReposDialog();

    /**
     * Translated "Please select or input a CRAN repository".
     *
     * @return translated "Please select or input a CRAN repository"
     */
    String validateSyncLabel();

    /**
     * Translated "The repository ".
     *
     * @return translated "The repository "
     */
    String showErrorRepoMessage();

    /**
     * Translated "is already included".
     *
     * @return translated "is already included"
     */
    String alreadyIncludedMessage();

    /**
     * Translated "Validating CRAN repository...".
     *
     * @return translated "Validating CRAN repository..."
     */
    String validateAsyncProgress();

    /**
     * Translated "The given URL does not appear to be a valid CRAN repository.".
     *
     * @return translated "The given URL does not appear to be a valid CRAN repository."
     */
    String onResponseReceived();

    /**
     * Translated "Name:".
     *
     * @return translated "Name:"
     */
    String nameLabel();

    /**
     * Translated "URL:".
     *
     * @return translated "URL:"
     */
    String urlLabel();

    /**
     * Translated "Available repositories:".
     *
     * @return translated "Available repositories:"
     */
    String reposLabel();

    /**
     * Translated "Add Secondary Repository".
     *
     * @return translated "Add Secondary Repository"
     */
    String secondaryRepoLabel();

    /**
     * Translated "Choose the layout of the panels in RStudio by selecting from the controls in each panel. Add up to three additional Source Columns to the left side of the layout. When a column is removed, all saved files within the column are closed and any unsaved files are moved to the main Source Pane.".
     *
     * @return translated "Choose the layout of the panels in RStudio by selecting from the controls in each panel. Add up to three additional Source Columns to the left side of the layout. When a column is removed, all saved files within the column are closed and any unsaved files are moved to the main Source Pane."
     */
    String paneLayoutText();

    /**
     * Translated "Manage Column Display".
     *
     * @return translated "Manage Column Display"
     */
    String columnToolbarLabel();

    /**
     * Translated "Add Column".
     *
     * @return translated "Add Column"
     */
    String addButtonText();

    /**
     * Translated "Add column".
     *
     * @return translated "Add column"
     */
    String addButtonLabel();

    /**
     * Translated "Remove Column".
     *
     * @return translated "Remove Column"
     */
    String removeButtonText();

    /**
     * Translated "Remove column".
     *
     * @return translated "Remove column"
     */
    String removeButtonLabel();

    /**
     * Translated "Bad config! Falling back to a reasonable default".
     *
     * @return translated "Bad config! Falling back to a reasonable default"
     */
    String createGridLabel();

    /**
     * Translated "Additional source column".
     *
     * @return translated "Additional source column"
     */
    String createColumnLabel();

    /**
     * Translated "Pane Layout".
     *
     * @return translated "Pane Layout"
     */
    String paneLayoutLabel();

    /**
     * Translated "Publishing Accounts".
     *
     * @return translated "Publishing Accounts"
     */
    String accountListLabel();

    /**
     * Translated "Connect...".
     *
     * @return translated "Connect..."
     */
    String connectButtonLabel();

    /**
     * Translated "Reconnect...".
     *
     * @return translated "Reconnect..."
     */
    String reconnectButtonLabel();

    /**
     * Translated "Disconnect".
     *
     * @return translated "Disconnect"
     */
    String disconnectButtonLabel();

    /**
     * Translated "Account records appear to exist, but cannot be viewed because a required package is not installed.".
     *
     * @return translated "Account records appear to exist, but cannot be viewed because a required package is not installed."
     */
    String missingPkgPanelMessage();

    /**
     * Translated "Install Missing Packages".
     *
     * @return translated "Install Missing Packages"
     */
    String installPkgsMessage();

    /**
     * Translated "Viewing publish accounts".
     *
     * @return translated "Viewing publish accounts"
     */
    String withRSConnectLabel();

    /**
     * Translated "Enable publishing to Posit Connect".
     *
     * @return translated "Enable publishing to Posit Connect"
     */
    String chkEnableRSConnectLabel();

    /**
     * Translated "Information about Posit Connect".
     *
     * @return translated "Information about Posit Connect"
     */
    String checkBoxWithHelpTitle();

    /**
     * Translated "Settings".
     *
     * @return translated "Settings"
     */
    String settingsHeaderLabel();

    /**
     * Translated "Enable publishing documents, apps, and APIs".
     *
     * @return translated "Enable publishing documents, apps, and APIs"
     */
    String chkEnablePublishingLabel();

    /**
     * Translated "Show diagnostic information when publishing".
     *
     * @return translated "Show diagnostic information when publishing"
     */
    String showPublishDiagnosticsLabel();

    /**
     * Translated "SSL Certificates".
     *
     * @return translated "SSL Certificates"
     */
    String sSLCertificatesHeaderLabel();

    /**
     * Translated "Check SSL certificates when publishing".
     *
     * @return translated "Check SSL certificates when publishing"
     */
    String publishCheckCertificatesLabel();

    /**
     * Translated "Check SSL certificates when publishing".
     *
     * @return translated "Check SSL certificates when publishing"
     */
    String usePublishCaBundleLabel();

    /**
     * Translated "(none)".
     *
     * @return translated "(none)"
     */
    String caBundlePath();

    /**
     * Translated "Troubleshooting Deployments".
     *
     * @return translated "Troubleshooting Deployments"
     */
    String helpLinkTroubleshooting();

    /**
     * Translated "Publishing".
     *
     * @return translated "Publishing"
     */
    String publishingPaneHeader();

    /**
     * Translated "Error Disconnecting Account".
     *
     * @return translated "Error Disconnecting Account"
     */
    String showErrorCaption();

    /**
     * Translated "Please select an account to disconnect.".
     *
     * @return translated "Please select an account to disconnect."
     */
    String showErrorMessage();

    /**
     * Translated "Confirm Remove Account".
     *
     * @return translated "Confirm Remove Account"
     */
    String removeAccountGlobalDisplay();

    /**
     * Translated "Are you sure you want to disconnect the ''{0}'' account on ''{1}''? This won''t delete the account on the server.".
     *
     * @return translated "Are you sure you want to disconnect the ''{0}'' account on ''{1}''? This won''t delete the account on the server."
     */
    String removeAccountMessage(String name, String server);

    /**
     * Translated "Disconnect Account".
     *
     * @return translated "Disconnect Account"
     */
    String onConfirmDisconnectYesLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    String onConfirmDisconnectNoLabel();

    /**
     * Translated "Error Disconnecting Account".
     *
     * @return translated "Error Disconnecting Account"
     */
    String disconnectingErrorMessage();

    /**
     * Translated "Connecting a publishing account".
     *
     * @return translated "Connecting a publishing account"
     */
    String getAccountCountLabel();

    /**
     * Translated "(No interpreter selected)".
     *
     * @return translated "(No interpreter selected)"
     */
    String pythonPreferencesText();

    /**
     * Translated "(NOTE: This project has already been configured with its own Python interpreter. Use the Edit Project Options button to change the version of Python used in this project.)".
     *
     * @return translated "(NOTE: This project has already been configured with its own Python interpreter. Use the Edit Project Options button to change the version of Python used in this project.)"
     */
    String overrideText();


    /**
     * Translated "Python".
     *
     * @return translated "Python"
     */
    String headerPythonLabel();

    /**
     * Translated "The active Python interpreter has been changed by an R startup script.".
     *
     * @return translated "The active Python interpreter has been changed by an R startup script."
     */
    String mismatchWarningBarText();

    /**
     * Translated "Finding interpreters...".
     *
     * @return translated "Finding interpreters..."
     */
    String progressIndicatorText();

    /**
     * Translated "Finding interpreters...".
     *
     * @return translated "Finding interpreters..."
     */
    String tbPythonInterpreterText();

    /**
     * Translated "Select...".
     *
     * @return translated "Select..."
     */
    String tbPythonActionText();

    /**
     * Translated "Error finding Python interpreters: ".
     *
     * @return translated "Error finding Python interpreters: "
     */
    String onDependencyErrorMessage();

    /**
     * Translated "The selected Python interpreter appears to be invalid.".
     *
     * @return translated "The selected Python interpreter appears to be invalid."
     */
    String invalidReasonLabel();

    /**
     * Translated "Using Python in RStudio".
     *
     * @return translated "Using Python in RStudio"
     */
    String helpRnwButtonLabel();

    /**
     * Translated "Automatically activate project-local Python environments".
     *
     * @return translated "Automatically activate project-local Python environments"
     */
    String cbAutoUseProjectInterpreter();

    /**
     * Translated "When enabled, RStudio will automatically find and activate a Python environment located within the project root directory (if any).".
     *
     * @return translated "When enabled, RStudio will automatically find and activate a Python environment located within the project root directory (if any)."
     */
    String cbAutoUseProjectInterpreterMessage();


    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String tabPanelCaption();

    /**
     * Translated "Clear".
     *
     * @return translated "Clear"
     */
    String clearLabel();

    /**
     * Translated "[Unknown]".
     *
     * @return translated "[Unknown]"
     */
    String unknownType();

    /**
     * Translated "Virtual Environment".
     *
     * @return translated "Virtual Environment"
     */
    String virtualEnvironmentType();

    /**
     * Translated "Conda Environment".
     *
     * @return translated "Conda Environment"
     */
    String condaEnvironmentType();

    /**
     * Translated "System Interpreter".
     *
     * @return translated "System Interpreter"
     */
    String systemInterpreterType();

    /**
     * Get locale value for the Quarto preview label. Default value
     * is "This version of RStudio includes a preview of Quarto, a
     * new scientific and technical publishing system. "
     *
     * @return translated value for Quarto preview label
     */
    String quartoPreviewLabel();

    /**
     * Get locale value of the label for the checkbox to enable the
     * Quarto preview. Default value is "Enable Quarto preview".
     *
     * @return the translated value for the label
     */
    String enableQuartoPreviewCheckboxLabel();

    /**
     * Get locale value for the help link caption. Default value
     * is "Learn more about Quarto".
     *
     * @return the translated value for the help link
     */
    String helpLinkCaption();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    String rMarkdownHeaderLabel();

    String documentOutlineHeaderLabel();
    /**
     * Translated "Show document outline by default".
     *
     * @return translated "Show document outline by default"
     */
    String rMarkdownShowLabel();

    /**
     * Translated "Soft-wrap R Markdown files".
     *
     * @return translated "Soft-wrap R Markdown files"
     */
    String rMarkdownSoftWrapLabel();

    /**
     * Translated "Show in document outline: ".
     *
     * @return translated "Show in document outline: "
     */
    String docOutlineDisplayLabel();

    /**
     * Translated "Sections Only".
     *
     * @return translated "Sections Only"
     */
    String docOutlineSectionsOption();

    /**
     * Translated "Sections and Named Chunks".
     *
     * @return translated "Sections and Named Chunks"
     */
    String docOutlineSectionsNamedChunksOption();

    /**
     * Translated "Sections and All Chunks".
     *
     * @return translated "Sections and All Chunks"
     */
    String docOutlineSectionsAllChunksOption();

    /**
     * Translated "Show output preview in: ".
     *
     * @return translated "Show output preview in: "
     */
    String rmdViewerModeLabel();

    /**
     * Translated "Window".
     *
     * @return translated "Window"
     */
    String rmdViewerModeWindowOption();

    /**
     * Translated "Viewer Pane".
     *
     * @return translated "Viewer Pane"
     */
    String rmdViewerModeViewerPaneOption();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    String rmdViewerModeNoneOption();

    /**
     * Translated "Show output inline for all R Markdown documents".
     *
     * @return translated "Show output inline for all R Markdown documents"
     */
    String rmdInlineOutputLabel();

    /**
     * Translated "Show equation and image previews: ".
     *
     * @return translated "Show equation and image previews: "
     */
    String latexPreviewWidgetLabel();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    String latexPreviewWidgetNeverOption();

    /**
     * Translated "In a popup".
     *
     * @return translated "In a popup"
     */
    String latexPreviewWidgetPopupOption();

    /**
     * Translated "Inline".
     *
     * @return translated "Inline"
     */
    String latexPreviewWidgetInlineOption();

    /**
     * Translated "Evaluate chunks in directory: ".
     *
     * @return translated "Evaluate chunks in directory: "
     */
    String knitWorkingDirLabel();

    /**
     * Translated "Document".
     *
     * @return translated "Document"
     */
    String knitWorkingDirDocumentOption();

    /**
     * Translated "Current".
     *
     * @return translated "Current"
     */
    String knitWorkingDirCurrentOption();

    /**
     * Translated "Project".
     *
     * @return translated "Project"
     */
    String knitWorkingDirProjectOption();

    /**
     * Translated "R Notebooks".
     *
     * @return translated "R Notebooks"
     */
    String rNotebooksCaption();

    /**
     * Translated "Execute setup chunk automatically in notebooks".
     *
     * @return translated "Execute setup chunk automatically in notebooks"
     */
    String autoExecuteSetupChunkLabel();

    /**
     * Translated "Hide console automatically when executing notebook chunks".
     *
     * @return translated "Hide console automatically when executing notebook chunks"
     */
    String notebookHideConsoleLabel();

    /**
     * Translated "Using R Notebooks".
     *
     * @return translated "Using R Notebooks"
     */
    String helpRStudioLinkLabel();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    String advancedHeaderLabel();

    /**
     * Translated "Enable chunk background highlight".
     *
     * @return translated "Enable chunk background highlight"
     */
    String advancedEnableChunkLabel();

    /**
     * Translated "Show inline toolbar for R code chunks".
     *
     * @return translated "Show inline toolbar for R code chunks"
     */
    String advancedShowInlineLabel();

    /**
     * Translated "Display render command in R Markdown tab".
     *
     * @return translated "Display render command in R Markdown tab"
     */
    String advancedDisplayRender();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String visualModeGeneralCaption();

    /**
     * Translated "Use visual editor by default for new documents".
     *
     * @return translated "Use visual editor by default for new documents"
     */
    String visualModeUseVisualEditorLabel();

    /**
     * Translated "Learn more about visual editing mode".
     *
     * @return translated "Learn more about visual editing mode"
     */
    String visualModeHelpLink();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    String visualModeHeaderLabel();

    /**
     * Translated "Show document outline by default".
     *
     * @return translated "Show document outline by default"
     */
    String visualEditorShowOutlineLabel();

    /**
     * Translated "Show margin column indicator in code blocks".
     *
     * @return translated "Show margin column indicator in code blocks"
     */
    String visualEditorShowMarginLabel();

    /**
     * Translated "Editor content width (px):".
     *
     * @return translated "Editor content width (px):"
     */
    String visualModeContentWidthLabel();

    /**
     * Translated "Editor font size:".
     *
     * @return translated "Editor font size:"
     */
    String visualModeFontSizeLabel();

    /**
     * Translated "Markdown".
     *
     * @return translated "Markdown"
     */
    String visualModeOptionsMarkdownCaption();

    /**
     * Translated "Default spacing between list items: ".
     *
     * @return translated "Default spacing between list items: "
     */
    String visualModeListSpacingLabel();

    /**
     * Translated "Automatic text wrapping (line breaks): ".
     *
     * @return translated "Automatic text wrapping (line breaks): "
     */
    String visualModeWrapLabel();

    /**
     * Translated "Learn more about automatic line wrapping".
     *
     * @return translated "Learn more about automatic line wrapping"
     */
    String visualModeWrapHelpLabel();

    /**
     * Translated "Wrap at column:".
     *
     * @return translated "Wrap at column:"
     */
    String visualModeOptionsLabel();

    /**
     * Translated "Write references at end of current: ".
     *
     * @return translated "Write references at end of current: "
     */
    String visualModeReferencesLabel();

    /**
     * Translated "Write canonical visual mode markdown in source mode".
     *
     * @return translated "Write canonical visual mode markdown in source mode"
     */
    String visualModeCanonicalLabel();

    /**
     * Translated "Visual Mode Preferences".
     *
     * @return translated "Visual Mode Preferences"
     */
    String visualModeCanonicalMessageCaption();

    /**
     * Translated "Are you sure you want to write canonical markdown from source mode for all R Markdown files?\n\nThis preference should generally only be used at a project level (to prevent re-writing of markdown source that you or others don''t intend to use with visual mode).\n\nChange this preference now?".
     *
     * @return translated "Are you sure you want to write canonical markdown from source mode for all R Markdown files?\n\nThis preference should generally only be used at a project level (to prevent re-writing of markdown source that you or others don''t intend to use with visual mode).\n\nChange this preference now?"
     */
    String visualModeCanonicalPreferenceMessage();

    /**
     * Translated "Learn more about markdown writer options".
     *
     * @return translated "Learn more about markdown writer options"
     */
    String markdownPerFileOptionsHelpLink();

    /**
     * Translated "Citation features are available within visual editing mode.".
     *
     * @return translated "Citation features are available within visual editing mode."
     */
    String citationsLabel();

    /**
     * Translated "Learn more about using citations with visual editing mode".
     *
     * @return translated "Learn more about using citations with visual editing mode"
     */
    String citationsHelpLink();

    /**
     * Translated "Zotero".
     *
     * @return translated "Zotero"
     */
    String zoteroHeaderLabel();

    /**
     * Translated "Zotero Data Directory:".
     *
     * @return translated "Zotero Data Directory:"
     */
    String zoteroDataDirLabel();

    /**
     * Translated "(None Detected)".
     *
     * @return translated "(None Detected)"
     */
    String zoteroDataDirNotDectedLabel();

    /**
     * Translated "Use Better BibTeX for citation keys and BibTeX export".
     *
     * @return translated "Use Better BibTeX for citation keys and BibTeX export"
     */
    String zoteroUseBetterBibtexLabel();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    String tabPanelTitle();

    /**
     * Translated "Basic".
     *
     * @return translated "Basic"
     */
    String tabPanelBasic();

    /**
     * Translated "Advanced".
     *
     * @return translated "Advanced"
     */
    String tabPanelAdvanced();

    /**
     * Translated "Visual".
     *
     * @return translated "Visual"
     */
    String tabPanelVisual();

    /**
     * Translated "Citations".
     *
     * @return translated "Citations"
     */
    String tabPanelCitations();

    /**
     * Translated "Web".
     *
     * @return translated "Web"
     */
    String webOption();

    /**
     * Translated "Show line numbers in code blocks".
     *
     * @return translated "Show line numbers in code blocks"
     */
    String showLinkNumbersLabel();

    /**
     * Translated "Enable version control interface for RStudio projects".
     *
     * @return translated "Enable version control interface for RStudio projects"
     */
    String chkVcsEnabledLabel();

    /**
     * Translated "Enable".
     *
     * @return translated "Enable"
     */
    String globalDisplayEnable();

    /**
     * Translated "Disable".
     *
     * @return translated "Disable"
     */
    String globalDisplayDisable();

    /**
     * Translated "{0} Version Control ".
     *
     * @return translated "{0} Version Control "
     */
    String globalDisplayVC(String displayEnable);

    /**
     * Translated "You must restart RStudio for this change to take effect.".
     *
     * @return translated "You must restart RStudio for this change to take effect."
     */
    String globalDisplayVCMessage();

    /**
     * Translated "The program ''{0}'' is unlikely to be a valid git executable.\nPlease select a git executable called '''git.exe''.".
     *
     * @return translated "The program ''{0}'' is unlikely to be a valid git executable.\nPlease select a git executable called ''git.exe''."
     */
    String gitExePathMessage(String gitPath);

    /**
     * Translated "Invalid Git Executable".
     *
     * @return translated "Invalid Git Executable."
     */
    String gitGlobalDisplay();

    /**
     * Translated "Git executable:".
     *
     * @return translated "Git executable:"
     */
    String gitExePathLabel();

    /**
     * Translated "(Not Found)".
     *
     * @return translated "(Not Found)"
     */
    String gitExePathNotFoundLabel();

    /**
     * Translated "SVN executable:".
     *
     * @return translated "SVN executable:"
     */
    String svnExePathLabel();

    /**
     * Translated "Terminal executable:".
     *
     * @return translated "Terminal executable:"
     */
    String terminalPathLabel();

    /**
     * Translated "Git/SVN".
     *
     * @return translated "Git/SVN"
     */
    String gitSVNPaneHeader();

    /**
     * Translated "Dictionaries".
     *
     * @return translated "Dictionaries"
     */
    String spellingPreferencesPaneHeader();

    /**
     * Translated "Ignore".
     *
     * @return translated "Ignore"
     */
    String ignoreHeader();

    /**
     * Translated "Ignore words in UPPERCASE".
     *
     * @return translated "Ignore words in UPPERCASE"
     */
    String ignoreWordsUppercaseLabel();

    /**
     * Translated "Ignore words with numbers".
     *
     * @return translated "Ignore words with numbers"
     */
    String ignoreWordsNumbersLabel();

    /**
     * Translated "Checking".
     *
     * @return translated "Checking"
     */
    String checkingHeader();

    /**
     * Translated "Use real time spell-checking".
     *
     * @return translated "Use real time spell-checking"
     */
    String realTimeSpellcheckingCheckboxLabel();


    /**
     * Translated "User dictionary: ".
     *
     * @return translated "User dictionary: "
     */
    String kUserDictionaryLabel();

    /**
     * Translated "{0}{1} words".
     *
     * @return translated "{0}{1} words"
     */
    String kUserDictionaryWordsLabel(String kUserDictionary, String entries);

    /**
     * Translated "Edit User Dictionary...".
     *
     * @return translated "Edit User Dictionary..."
     */
    String editUserDictLabel();

    /**
     * Translated "Edit User Dictionary".
     *
     * @return translated "Edit User Dictionary"
     */
    String editUserDictCaption();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    String editUserDictSaveCaption();

    /**
     * Translated "Spelling".
     *
     * @return translated "Spelling"
     */
    String spellingPaneLabel();

    /**
     * Translated "Edit".
     *
     * @return translated "Edit"
     */
    String editDialog();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    String saveDialog();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    String cancelButton();

    /**
     * Translated "Shell".
     *
     * @return translated "Shell"
     */
    String shellHeaderLabel();

    /**
     * Translated "Initial directory:".
     *
     * @return translated "Initial directory:"
     */
    String initialDirectoryLabel();

    /**
     * Translated "Project directory".
     *
     * @return translated "Project directory"
     */
    String projectDirectoryOption();

    /**
     * Translated "Current directory".
     *
     * @return translated "Current directory"
     */
    String currentDirectoryOption();

    /**
     * Translated "Home directory".
     *
     * @return translated "Home directory"
     */
    String homeDirectoryOption();

    /**
     * Translated "New terminals open with:".
     *
     * @return translated "New terminals open with:"
     */
    String terminalShellLabel();

    /**
     * Translated "The program ''{0}'' is unlikely to be a valid shell executable.".
     *
     * @return translated "The program ''{0}'' is unlikely to be a valid shell executable."
     */
    String shellExePathMessage(String shellExePath);
    /**
     * Translated "Invalid Shell Executable".
     *
     * @return translated "Invalid Shell Executable"
     */
    String shellExeCaption();

    /**
     * Translated "Custom shell binary:".
     *
     * @return translated "Custom shell binary:"
     */
    String customShellPathLabel();

    /**
     * Translated "(Not Found)".
     *
     * @return translated "(Not Found)"
     */
    String customShellChooserEmptyLabel();

    /**
     * Translated "Custom shell command-line options:".
     *
     * @return translated "Custom shell command-line options:"
     */
    String customShellOptionsLabel();

    /**
     * Translated "Connection".
     *
     * @return translated "Connection"
     */
    String perfLabel();

    /**
     * Translated "Local terminal echo".
     *
     * @return translated "Local terminal echo"
     */
    String chkTerminalLocalEchoLabel();

    /**
     * Translated "Local echo is more responsive but may get out of sync with some line-editing modes or custom shells.".
     *
     * @return translated "Local echo is more responsive but may get out of sync with some line-editing modes or custom shells."
     */
    String chkTerminalLocalEchoTitle();

    /**
     * Translated "Connect with WebSockets".
     *
     * @return translated "Connect with WebSockets"
     */
    String chkTerminalWebsocketLabel();

    /**
     * Translated "WebSockets are generally more responsive; try turning off if terminal won''t connect.".
     *
     * @return translated "WebSockets are generally more responsive; try turning off if terminal won''t connect."
     */
    String chkTerminalWebsocketTitle();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    String displayHeaderLabel();

    /**
     * Translated "Hardware acceleration".
     *
     * @return translated "Hardware acceleration"
     */
    String chkHardwareAccelerationLabel();

    /**
     * Translated "Audible bell".
     *
     * @return translated "Audible bell"
     */
    String chkAudibleBellLabel();

    /**
     * Translated "Clickable web links".
     *
     * @return translated "Clickable web links"
     */
    String chkWebLinksLabel();

    /**
     * Translated "Using the RStudio terminal".
     *
     * @return translated "Using the RStudio terminal"
     */
    String helpLinkLabel();

    /**
     * Translated "Miscellaneous".
     *
     * @return translated "Miscellaneous"
     */
    String miscLabel();

    /**
     * Translated "When shell exits:".
     *
     * @return translated "When shell exits:"
     */
    String autoClosePrefLabel();

    /**
     * Translated "Close the pane".
     *
     * @return translated "Close the pane"
     */
    String closePaneOption();

    /**
     * Translated "Don''t close the pane".
     *
     * @return translated "Don''t close the pane"
     */
    String doNotClosePaneOption();

    /**
     * Translated "Close pane if shell exits cleanly".
     *
     * @return translated "Close pane if shell exits cleanly"
     */
    String shellExitsPaneOption();

    /**
     * Translated "Save and restore environment variables".
     *
     * @return translated "Save and restore environment variables"
     */
    String chkCaptureEnvLabel();

    /**
     * Translated "Terminal occasionally runs a hidden command to capture state of environment variables.".
     *
     * @return translated "Terminal occasionally runs a hidden command to capture state of environment variables."
     */
    String chkCaptureEnvTitle();

    /**
     * Translated "Process Termination".
     *
     * @return translated "Process Termination"
     */
    String shutdownLabel();

    /**
     * Translated "Ask before killing processes:".
     *
     * @return translated "Ask before killing processes:"
     */
    String busyModeLabel();

    /**
     * Translated "Don''t ask before killing:".
     *
     * @return translated "Don''t ask before killing:"
     */
    String busyWhitelistLabel();

    /**
     * Translated "Terminal".
     *
     * @return translated "Terminal"
     */
    String terminalPaneLabel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String tabGeneralPanelLabel();

    /**
     * Translated "Closing".
     *
     * @return translated "Closing"
     */
    String tabClosingPanelLabel();

    /**
     * Translated "Always".
     *
     * @return translated "Always"
     */
    String busyModeAlwaysOption();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    String busyModeNeverOption();

    /**
     * Translated "Always except for list".
     *
     * @return translated "Always except for list"
     */
    String busyModeListOption();

    /**
     * Translated "Enable Python integration".
     *
     * @return translated "Enable Python integration"
     */
    String chkPythonIntegration();

    /**
     * Translated "When enabled, the active version of Python will be placed on the PATH for new terminal sessions. Only bash and zsh are supported.".
     *
     * @return translated "When enabled, the active version of Python will be placed on the PATH for new terminal sessions. Only bash and zsh are supported."
     */
    String chkPythonIntegrationTitle();

    /**
     * Translated "Confirm Remove".
     *
     * @return translated "Confirm Remove"
     */
    String confirmRemoveCaption();

    /**
     * Translated "Are you sure you want to remove the {0} repository?".
     *
     * @return translated "Are you sure you want to remove the {0} repository?"
     */
    String confirmRemoveMessage(String repo);

    /**
     * Translated "Modern".
     *
     * @return translated "Modern"
     */
    String modernThemeLabel();

    /**
     * Translated "Sky".
     *
     * @return translated "Sky"
     */
    String skyThemeLabel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String generalHeaderLabel();
    
    /**
     * Translated "Code Formatting".
     *
     * @return translated "Code Formatting"
     */
    String codeFormattingHeaderLabel();
 
    /**
     * Translated "Use formatter:".
     *
     * @return translated "Use formatter:"
     */
    String useFormatterLabel();
    

    /**
     * Translated "Syntax".
     *
     * @return translated "Syntax"
     */
    String syntaxHeaderLabel();

    /**
     * Translated "Edit Snippets...".
     *
     * @return translated "Edit Snippets..."
     */
    String editSnippetsButtonLabel();

    /**
     * Translated "tight".
     *
     * @return translated "tight"
     */
    String listSpacingTight();

    /**
     * Translated "spaced".
     *
     * @return translated "spaced"
     */
    String listSpacingSpaced();

    /**
     * Translated "(none)".
     *
     * @return translated "(none)"
     */
    String editingWrapNone();

    /**
     * Translated "(column)".
     *
     * @return translated "(column)"
     */
    String editingWrapColumn();

    /**
     * Translated "(sentence)".
     *
     * @return translated "(sentence)"
     */
    String editingWrapSentence();

    /**
     * Translated "block".
     *
     * @return translated "block"
     */
    String refLocationBlock();

    /**
     * Translated "section".
     *
     * @return translated "section"
     */
    String refLocationSection();

    /**
     * Translated "document"".
     *
     * @return translated "document"
     */
    String refLocationDocument();

    /**
     * Translated "Other Languages".
     *
     * @return translated "Other Languages"
     */
    String editingDiagOtherLabel();

    /**
     * Translated "Show Diagnostics".
     *
     * @return translated "Show Diagnostics"
     */
    String editingDiagShowLabel();

    /**
     * Translated "R Diagnostics".
     *
     * @return translated "R Diagnostics"
     */
    String editingDiagnosticsPanel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String editingDisplayPanel();

    /**
     * Translated "Modify Keyboard Shortcuts...".
     *
     * @return translated "Modify Keyboard Shortcuts..."
     */
    String editingEditShortcuts();

    /**
     * Translated "Execution".
     *
     * @return translated "Execution"
     */
    String editingExecutionLabel();

    /**
     * Translated "Completion Delay".
     *
     * @return translated "Completion Delay"
     */
    String editingHeaderLabel();

    /**
     * Translated "Other Languages".
     *
     * @return translated "Other Languages"
     */
    String editingOtherLabel();

    /**
     * Translated "Keyword and text-based completions are supported for several other languages including JavaScript, HTML, CSS, Python, and SQL.".
     *
     * @return translated "Keyword and text-based completions are supported for several other languages including JavaScript, HTML, CSS, Python, and SQL."
     */
    String editingOtherTip();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String editingSavePanel();

    /**
     * Translated "Change...".
     *
     * @return translated "Change..."
     */
    String editingSavePanelAction();

    /**
     * Translated "Autosave".
     *
     * @return translated "Autosave"
     */
    String editingSavePanelAutosave();

    /**
     * Translated "Serialization".
     *
     * @return translated "Serialization"
     */
    String editingSerializationLabel();

    /**
     * Translated "Help on code snippets".
     *
     * @return translated "Help on code snippets"
     */
    String editingSnippetHelpTitle();

    /**
     * Translated "Snippets".
     *
     * @return translated "Snippets"
     */
    String editingSnippetsLabel();

    /**
     * Translated "Editing".
     *
     * @return translated "Editing"
     */
    String editingTabPanel();

    /**
     * Translated "Completion".
     *
     * @return translated "Completion"
     */
    String editingTabPanelCompletionPanel();

    /**
     * Translated "Diagnostics".
     *
     * @return translated "Diagnostics"
     */
    String editingTabPanelDiagnosticsPanel();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    String editingTabPanelDisplayPanel();

    /**
     * Translated "Formatting".
     *
     * @return translated "Formatting"
     */
    String editingTabPanelFormattingPanel();
    
    /**
     * Translated "Saving".
     *
     * @return translated "Saving"
     */
    String editingTabPanelSavePanel();

    /**
     * Translated "R and C/C++".
     *
     * @return translated "R and C/C++"
     */
    String editingCompletionPanel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    String editingHeader();

    /**
     * Translated "No bindings available".
     *
     * @return translated "No bindings available"
     */
    String editingKeyboardShortcuts();

    /**
     * Translated "Keyboard Shortcuts".
     *
     * @return translated "Keyboard Shortcuts"
     */
    String editingKeyboardText();

    /**
     * Translated "Customized".
     *
     * @return translated "Customized"
     */
    String editingRadioCustomized();

    /**
     * Translated "Filter...".
     *
     * @return translated "Filter..."
     */
    String editingFilterWidget();

    /**
     * Translated "Reset...".
     *
     * @return translated "Reset..."
     */
    String editingResetText();

    /**
     * Translated "Reset Keyboard Shortcuts".
     *
     * @return translated "Reset Keyboard Shortcuts"
     */
    String editingGlobalDisplay();

    /**
     * Translated "Are you sure you want to reset keyboard shortcuts to their default values? ".
     *
     * @return translated "Are you sure you want to reset keyboard shortcuts to their default values? "
     */
    String editingGlobalCaption();

    /**
     * Translated "This action cannot be undone.".
     *
     * @return translated "This action cannot be undone."
     */
    String editingGlobalMessage();

    /**
     * Translated "Resetting Keyboard Shortcuts...".
     *
     * @return translated "Resetting Keyboard Shortcuts..."
     */
    String editingProgressMessage();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    String editingCancelShortcuts();

    /**
     * Translated "Tab width"
     *
     * @return translated "Tab width"
     */
    String editingTabWidthLabel();

    /**
     * Translated "Editor scroll speed sensitivity:"
     *
     * @return translated "Editor scroll speed sensitivity:"
     */
    String editorScrollMultiplier();

    /**
     * Translated "Adjust the editor scroll speed sensitivity. Higher is faster."
     *
     * @return translated "Adjust the editor scroll speed sensitivity. Higher is faster."
     */
    String editorScrollMultiplierDesc();

    /**
     * Translated "Auto-detect code indentation"
     *
     * @return translated "Auto-detect code indentation"
     */
    String editingAutoDetectIndentationLabel();

    /**
     * Translated "Auto-detect code indentation"
     *
     * @return translated "When enabled, the indentation for documents not part of an RStudio project will be automatically detected."
     */
    String editingAutoDetectIndentationDesc();

    /**
     * Translated "Insert matching parens/quotes"
     *
     * @return translated "Insert matching parens/quotes"
     */
    String editingInsertMatchingLabel();

    /**
     * Translated "Use native pipe operator, |> (requires R 4.1+)"
     *
     * @return translated "Use native pipe operator, |> (requires R 4.1+)"
     */
    String editingUseNativePipeOperatorLabel();

    /**
     * Translated "NOTE: Some of these settings may be overridden by project-specific options."
     *
     * @return translated "NOTE: Some of these settings may be overridden by project-specific options."
     */
    String editingProjectOverrideInfoText();

    /**
     * Translated "Edit Project Options..."
     *
     * @return translated "Edit Project Options..."
     */
    String editProjectPreferencesButtonLabel();

    /**
     * Translated "Auto-indent code after paste"
     *
     * @return translated "Auto-indent code after paste"
     */
    String editingReindentOnPasteLabel();

    /**
     * Translated "Vertically align arguments in auto-indent"
     *
     * @return translated "Vertically align arguments in auto-indent"
     */
    String editingVerticallyAlignArgumentsIndentLabel();

    /**
     * Translated "Continue comment when inserting new line"
     *
     * @return translated "Continue comment when inserting new line"
     */
    String editingContinueCommentsOnNewlineLabel();

    /**
     * Translated "When enabled, pressing Enter will continue comments on new lines. Press Shift + Enter to exit a comment."
     *
     * @return translated "When enabled, pressing Enter will continue comments on new lines. Press Shift + Enter to exit a comment."
     */
    String editingContinueCommentsOnNewlineDesc();

    /**
     * Translated "Enable hyperlink highlighting in editor"
     *
     * @return translated "Enable hyperlink highlighting in editor"
     */
    String editingHighlightWebLinkLabel();

    /**
     * Translated "When enabled, hyperlinks in comments will be underlined and clickable."
     *
     * @return translated "When enabled, hyperlinks in comments will be underlined and clickable."
     */
    String editingHighlightWebLinkDesc();

    /**
     * Translated "Surround selection on text insertion:"
     *
     * @return translated "Surround selection on text insertion:"
     */
    String editingSurroundSelectionLabel();

    /**
     * Translated "Keybindings:"
     *
     * @return translated "Keybindings:"
     */
    String editingKeybindingsLabel();

    /**
     * Translated "Focus console after executing from source"
     *
     * @return translated "Focus console after executing from source"
     */
    String editingFocusConsoleAfterExecLabel();

    /**
     * Translated "Ctrl+Enter executes:"
     *
     * @return translated "Ctrl+Enter executes:"
     */
    String editingExecutionBehaviorLabel();

    /**
     * Translated "Highlight selected word"
     *
     * @return translated "Highlight selected word"
     */
    String displayHighlightSelectedWordLabel();

    /**
     * Translated "Highlight selected line"
     *
     * @return translated "Highlight selected line"
     */
    String displayHighlightSelectedLineLabel();

    /**
      * Translated "Show line numbers"
      *
      * @return translated "Show line numbers"
      */
    String displayShowLineNumbersLabel();

    /**
     * Translated "Relative line numbers"
     *
     * @return translated "Relative line numbers"
     */
    String displayRelativeLineNumbersLabel();

    /**
     * Translated "Show margin"
     *
     * @return translated "Show margin"
     */
    String displayShowMarginLabel();

    /**
     * Translated "Show whitespace characters"
     *
     * @return translated "Show whitespace characters"
     */
    String displayShowInvisiblesLabel();

    /**
     * Translated "Show indent guides"
     *
     * @return translated "Show indent guides"
     */
    String displayShowIndentGuidesLabel();

    /**
     * Translated "Blinking cursor"
     *
     * @return translated "Blinking cursor"
     */
    String displayBlinkingCursorLabel();

    /**
     * Translated "Allow scroll past end of document"
     *
     * @return translated "Allow scroll past end of document"
     */
    String displayScrollPastEndOfDocumentLabel();


    /**
     * Translated "Allow drag and drop of text"
     *
     * @return translated "Allow drag and drop of text"
     */
    String displayEnableTextDragLabel();

    /**
     * Translated "Fold Style:"
     *
     * @return translated "Fold Style:"
     */
    String displayFoldStyleLabel();

    /**
     * Translated "Ensure that source files end with newline"
     *
     * @return translated "Ensure that source files end with newline"
     */
    String savingAutoAppendNewLineLabel();

    /**
     * Translated "Strip trailing horizontal whitespace when saving"
     *
     * @return translated "Strip trailing horizontal whitespace when saving"
     */
    String savingStripTrailingWhitespaceLabel();

    /**
     * Translated "Restore last cursor position when opening file"
     *
     * @return translated "Restore last cursor position when opening file"
     */
    String savingRestoreSourceDocumentCursorPositionLabel();

    /**
     * Translated "Default text encoding:"
     *
     * @return translated "Default text encoding:"
     */
    String savingDefaultEncodingLabel();

    /**
     * Translated "Always save R scripts before sourcing"
     *
     * @return translated "Always save R scripts before sourcing"
     */
    String savingSaveBeforeSourcingLabel();

    /**
     * Translated "Automatically save when editor loses focus"
     *
     * @return translated "Automatically save when editor loses focus"
     */
    String savingAutoSaveOnBlurLabel();

    /**
     * Translated "When editor is idle:"
     *
     * @return translated "When editor is idle:"
     */
    String savingAutoSaveOnIdleLabel();

    /**
     * Translated "Idle period:"
     *
     * @return translated "Idle period:"
     */
    String savingAutoSaveIdleMsLabel();

    /**
     * Translated "Show code completions:"
     *
     * @return translated "Show code completions:"
     */
    String completionCodeCompletionLabel();

    /**
     * Translated "Show code completions:"
     *
     * @return translated "Show code completions:"
     */
    String completionCodeCompletionOtherLabel();

    /**
     * Translated "Allow automatic completions in console"
     *
     * @return translated "Allow automatic completions in console"
     */
    String completionConsoleCodeCompletionLabel();

    /**
     * Translated "Insert parentheses after function completions"
     *
     * @return translated "Insert parentheses after function completions"
     */
    String completionInsertParensAfterFunctionCompletion();

    /**
     * Translated "Show help tooltip after function completions"
     *
     * @return translated "Show help tooltip after function completions"
     */
    String completionShowFunctionSignatureTooltipsLabel();

    /**
     * Translated "Show help tooltip on cursor idle"
     *
     * @return translated "Show help tooltip on cursor idle"
     */
    String completionShowHelpTooltipOnIdleLabel();

    /**
     * Translated "Insert spaces around equals for argument completions"
     *
     * @return translated "Insert spaces around equals for argument completions"
     */
    String completionInsertSpacesAroundEqualsLabel();

    /**
     * Translated "Use tab for autocompletions"
     *
     * @return translated "Use tab for autocompletions"
     */
    String completionTabCompletionLabel();

    /**
     * Translated "Use tab for multiline autocompletions"
     *
     * @return translated "Use tab for multiline autocompletions"
     */
    String completionTabMultilineCompletionLabel();

    /**
     * Translated "Show completions after characters entered:"
     *
     * @return translated "Show completions after characters entered:"
     */
    String completionCodeCompletionCharactersLabel();

    /**
     * Translated "Show completions after keyboard idle (ms):"
     *
     * @return translated "Show completions after keyboard idle (ms):"
     */
    String completionCodeCompletionDelayLabel();

    /**
     * Translated "Show diagnostics for R"
     *
     * @return translated "Show diagnostics for R"
     */
    String diagnosticsShowDiagnosticsRLabel();

    /**
     * Translated "Enable diagnostics within R function calls"
     *
     * @return translated "Enable diagnostics within R function calls"
     */
    String diagnosticsInRFunctionCallsLabel();

    /**
     * Translated "Check arguments to R function calls"
     *
     * @return translated "Check arguments to R function calls"
     */
    String diagnosticsCheckArgumentsToRFunctionCallsLabel();

    /**
     * Translated "Check usage of '<-' in function call"
     *
     * @return translated "Check usage of '<-' in function call"
     */
    String diagnosticsCheckUnexpectedAssignmentInFunctionCallLabel();

    /**
     * Translated "Warn if variable used has no definition in scope"
     *
     * @return translated "Warn if variable used has no definition in scope"
     */
    String diagnosticsWarnIfNoSuchVariableInScopeLabel();

    /**
     * Translated "Warn if variable is defined but not used"
     *
     * @return translated "Warn if variable is defined but not used"
     */
    String diagnosticsWarnVariableDefinedButNotUsedLabel();

    /**
     * Translated "Provide R style diagnostics (e.g. whitespace)"
     *
     * @return translated "Provide R style diagnostics (e.g. whitespace)"
     */
    String diagnosticsStyleDiagnosticsLabel();

    /**
     * Translated "Prompt to install missing R packages discovered in R source files"
     *
     * @return translated "Prompt to install missing R packages discovered in R source files"
     */
    String diagnosticsAutoDiscoverPackageDependenciesLabel();

    /**
     * Translated "Show diagnostics for C/C++"
     *
     * @return translated "Show diagnostics for C/C++"
     */
    String diagnosticsShowDiagnosticsCppLabel();

    /**
     * Translated "Show diagnostics for YAML"
     *
     * @return translated "Show diagnostics for YAML"
     */
    String diagnosticsShowDiagnosticsYamlLabel();

    /**
     * Translated "Show diagnostics for JavaScript, HTML, and CSS"
     *
     * @return translated "Show diagnostics for JavaScript, HTML, and CSS"
     */
    String diagnosticsShowDiagnosticsOtherLabel();

    /**
     * Translated "Show diagnostics whenever source files are saved"
     *
     * @return translated "Show diagnostics whenever source files are saved"
     */
    String diagnosticsOnSaveLabel();

    /**
     * Translated "Show diagnostics after keyboard is idle for a period of time"
     *
     * @return translated "Show diagnostics after keyboard is idle for a period of time"
     */
    String diagnosticsBackgroundDiagnosticsLabel();

    /**
     * Translated "Keyboard idle time (ms):"
     *
     * @return translated "Keyboard idle time (ms):"
     */
    String diagnosticsBackgroundDiagnosticsDelayMsLabel();

    /**
     * Translated "Show full path to project in window title"
     *
     * @return translated "Show full path to project in window title"
     */
    String fullProjectPathInWindowTitleLabel();

    /**
     * Translated "Hide menu bar until Alt-key pressed"
     *
     * @return translated "Hide menu bar until Alt-key pressed"
     */
    String autohideMenubarLabel();
    
    /**
     * Translated "Text rendering:"
     *
     * @return translated "Text rendering:"
     */
    String textRenderingLabel();
    
    /**
     * Translated "Geometric Precision"
     *
     * @return translated "Geometric Precision"
     */
    String geometricPrecision();
    
    /**
     * Translated "Loading..."
     *
     * @return translated "Loading..."
     */
    String copilotLoadingMessage();
    
    /**
     * Translated "Generating diagnostic report..."
     *
     * @return translated "Generating diagnostic report..."
     */
    String copilotDiagnosticReportProgressLabel();

    /**
     * Translated "You are currently signed in as: {0}"
     *
     * @return translated "You are currently signed in as: {0}"
     */
    String copilotSignedInAsLabel(String user);

    /**
     * Translated "Show Error..."
     *
     * @return translated "Show Error..."
     */
    String copilotShowErrorLabel();

    /**
     * Translated "Sign In"
     *
     * @return translated "Sign In"
     */
    String copilotSignInLabel();

    /**
     * Translated "Sign Out"
     *
     * @return translated "Sign Out"
     */
    String copilotSignOutLabel();

    /**
     * Translated "Activate"
     *
     * @return translated "Activate"
     */
    String copilotActivateLabel();

    /**
     * Translated "Refresh"
     *
     * @return translated "Refresh"
     */
    String copilotRefreshLabel();
    
    /**
     * Translated "Diagnostics"
     *
     * @return translated "Diagnostics"
     */
    String copilotDiagnosticsLabel();
    
    /**
     * Translated "Project Options..."
     *
     * @return translated "Project Options..."
     */
    String copilotProjectOptionsLabel();

    /**
     * Translated "GitHub Copilot: Terms of Service"
     *
     * @return translated "GitHub Copilot: Terms of Service"
     */
    String copilotTermsOfServiceLinkLabel();

    /**
     * Translated "By using GitHub Copilot, you agree to abide by their terms of service."
     *
     * @return translated "By using GitHub Copilot, you agree to abide by their terms of service."
     */
    String copilotTermsOfServiceLabel();

    /**
     * Translated "GitHub Copilot"
     *
     * @return translated "GitHub Copilot"
     */
    String copilotDisplayName();

    /**
     * Translated "Copilot"
     *
     * @return translated "Copilot"
     */
    String copilotPaneName();

    /**
     * Translated "Copilot Indexing"
     *
     * @return translated "Copilot Indexing"
     */
    String copilotIndexingHeader();

    /**
     * Translated "Copilot Completions"
     *
     * @return translated "Copilot Completions"
     */
    String copilotCompletionsHeader();

    /**
     * Translated "Show code suggestions after keyboard idle (ms):"
     *
     * @return translated "Show code suggestions after keyboard idle (ms):"
     */
    String copilotCompletionsDelayLabel();

    /**
     * Translated "GitHub Copilot integration has been disabled by the administrator."
     *
     * @return translated "GitHub Copilot integration has been disabled by the administrator."
     */
    String copilotDisabledByAdmin();

    /**
     * Translated "GitHub Copilot: Status"
     *
     * @return translated "GitHub Copilot: Status"
     */
    String copilotStatusDialogCaption();

    /**
     * Translated "An unexpected error occurred while checking the status of the GitHub Copilot agent."
     *
     * @return translated "An unexpected error occurred while checking the status of the GitHub Copilot agent."
     */
    String copilotUnexpectedError();

    /**
     * Translated "An error occurred while starting the Copilot agent."
     *
     * @return translated "An error occurred while starting the Copilot agent."
     */
    String copilotStartupError();

    /**
     * Translated "GitHub Copilot has been disabled in this project."
     *
     * @return translated "GitHub Copilot has been disabled in this project."
     */
    String copilotDisabledInProject();

    /**
     * Translated "The GitHub Copilot agent is not currently running."
     *
     * @return translated "The GitHub Copilot agent is not currently running."
     */
    String copilotAgentNotRunning();

    /**
     * Translated "The GitHub Copilot agent has not been enabled."
     *
     * @return translated "The GitHub Copilot agent has not been enabled."
     */
    String copilotAgentNotEnabled();

    /**
     * Translated "You are currently signed in as {0}, but you haven''t yet activated GitHub Copilot."
     *
     * @return translated "You are currently signed in as {0}, but you haven''t yet activated GitHub Copilot."
     */
    String copilotAccountNotActivated(String name);

    /**
     * Translated "You are not currently signed in."
     *
     * @return translated "You are not currently signed in."
     */
    String copilotNotSignedIn();

    /**
     * Translated "RStudio received a Copilot response that it does not understand.\n{0}"
     *
     * @return translated "RStudio received a Copilot response that it does not understand.\n{0}"
     */
    String copilotUnknownResponse(String response);

    /**
     * Translated "Sign git commits"
     *
     * @return translated "Sign git commits"
     */
    String gitSignCommitLabel();
}

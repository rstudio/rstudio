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

    @DefaultMessage("Verify Key...")
    @Key("verifyKey")
    String verifyKey();

    @DefaultMessage("Unable to verify Zotero API key.\n\nYou should verify that your API key is still valid, and if necessary create a new key.")
    @Key("zoteroVerifyKeyFailedMessage")
    String zoteroVerifyKeyFailedMessage();

    @DefaultMessage("Zotero Web API Key:")
    @Key("zoteroWebApiKey")
    String zoteroWebApiKey();

    @DefaultMessage("Verifying Key...")
    @Key("verifyingKey")
    String verifyingKey();

    @DefaultMessage("Zotero")
    @Key("zotero")
    String zotero();

    @DefaultMessage("Zotero API key successfully verified.")
    @Key("zoteroKeyVerified")
    String zoteroKeyVerified();

    @DefaultMessage("Use libraries:")
    @Key("useLibraries")
    String useLibraries();

    @DefaultMessage("Error")
    @Key("error")
    String error();

    @DefaultMessage("You must select at least one Zotero library")
    @Key("selectOneZoteroLibrary")
    String selectOneZoteroLibrary();

    @DefaultMessage("(Default)")
    @Key("defaultInParentheses")
    String defaultInParentheses();

    @DefaultMessage("My Library")
    @Key("myLibrary")
    String myLibrary();

    @DefaultMessage("Selected Libraries")
    @Key("selectedLibraries")
    String selectedLibraries();

    @DefaultMessage("Conda Environment")
    @Key("condaEnvironment")
    String condaEnvironment();

    @DefaultMessage("Virtual Environment")
    @Key("virtualEnvironment")
    String virtualEnvironment();

    @DefaultMessage("Python Interpreter")
    @Key("pythonInterpreter")
    String pythonInterpreter();

    @DefaultMessage("System")
    @Key("system")
    String system();

    @DefaultMessage("Virtual Environments")
    @Key("virtualEnvironmentPlural")
    String virtualEnvironmentPlural();

    @DefaultMessage("Conda Environments")
    @Key("condaEnvironmentPlural")
    String condaEnvironmentPlural();

    @DefaultMessage("Python Interpreters")
    @Key("pythonInterpreterPlural")
    String pythonInterpreterPlural();

    @DefaultMessage("Select")
    @Key("select")
    String select();

    @DefaultMessage("(None available)")
    @Key("noneAvailableParentheses")
    String noneAvailableParentheses();

    @DefaultMessage("Editor Theme Preview")
    @Key("editorThemePreview")
    String editorThemePreview();

    @DefaultMessage("Spelling Prefs")
    @Key("spellingPrefsTitle")
    String spellingPrefsTitle();

    @DefaultMessage("The context for the user''s spelling preferences.")
    @Key("spellingPrefsDescription")
    String spellingPrefsDescription();

    @DefaultMessage("SSH Public Key Filename")
    @Key("rsaKeyFileTitle")
    String rsaKeyFileTitle();

    @DefaultMessage("Filename of SSH public key")
    @Key("rsaKeyFileDescription")
    String rsaKeyFileDescription();

    @DefaultMessage("Has SSH Key")
    @Key("haveRSAKeyTitle")
    String haveRSAKeyTitle();

    @DefaultMessage("Whether the user has an SSH key")
    @Key("haveRSAKeyDescription")
    String haveRSAKeyDescription();

    @DefaultMessage("Error Changing Setting")
    @Key("errorChangingSettingCaption")
    String errorChangingSettingCaption();

    @DefaultMessage("The tab key moves focus setting could not be updated.")
    @Key("tabKeyErrorMessage")
    String tabKeyErrorMessage();

    @DefaultMessage("Tab key always moves focus on")
    @Key("tabKeyFocusOnMessage")
    String tabKeyFocusOnMessage();

    @DefaultMessage("Tab key always moves focus off")
    @Key("tabKeyFocusOffMessage")
    String tabKeyFocusOffMessage();

    @DefaultMessage("The screen reader support setting could not be changed.")
    @Key("toggleScreenReaderErrorMessage")
    String toggleScreenReaderErrorMessage();

    @DefaultMessage("Confirm Toggle Screen Reader Support")
    @Key("toggleScreenReaderConfirmCaption")
    String toggleScreenReaderConfirmCaption();

    @DefaultMessage("Are you sure you want to {0} screen reader support? The application will reload to apply the change.")
    @Key("toggleScreenReaderMessageConfirmDialog")
    String toggleScreenReaderMessageConfirmDialog(String value);

    @DefaultMessage("disable")
    @Key("disable")
    String disable();

    @DefaultMessage("enable")
    @Key("enable")
    String enable();

    @DefaultMessage("Warning: screen reader mode not enabled. Turn on using shortcut {0}.")
    @Key("announceScreenReaderStateMessage")
    String announceScreenReaderStateMessage(String shortcut);

    @DefaultMessage("{0} (enabled)")
    @Key("screenReaderStateEnabled")
    String screenReaderStateEnabled(String screenReaderLabel);

    @DefaultMessage("{0} (disabled)")
    @Key("screenReaderStateDisabled")
    String screenReaderStateDisabled(String screenReaderLabel);

    @DefaultMessage("Clear Preferences")
    @Key("onClearUserPrefsYesLabel")
    String onClearUserPrefsYesLabel();

    @DefaultMessage("Cancel")
    @Key("cancel")
    String cancel();

    @DefaultMessage("Restart R")
    @Key("onClearUserPrefsRestartR")
    String onClearUserPrefsRestartR();

    @DefaultMessage("Preferences Cleared")
    @Key("onClearUserPrefsResponseCaption")
    String onClearUserPrefsResponseCaption();

    /**
     * Translated "Your preferences have been cleared, and your R session will now be restarted.
     * A backup copy of your preferences can be found at: \n\n{0}".
     *
     * @return translated "Your preferences have been cleared, and your R session will now be restarted.
     * A backup copy of your preferences can be found at: \n\n{0}"
     */
    @DefaultMessage("Your preferences have been cleared, and your R session will now be restarted. " +
            "A backup copy of your preferences can be found at: \n\n{0}")
    @Key("onClearUserPrefsResponseMessage")
    String onClearUserPrefsResponseMessage(String path);

    @DefaultMessage("Confirm Clear Preferences")
    @Key("onClearUserPrefsCaption")
    String onClearUserPrefsCaption();

    /**
     * Translated "Are you sure you want to clear your preferences?
     * All RStudio settings will be restored to their defaults, and your R session will be restarted.".
     *
     * @return translated "Are you sure you want to clear your preferences?
     * All RStudio settings will be restored to their defaults, and your R session will be restarted."
     */
    @DefaultMessage("Are you sure you want to clear your preferences? " +
            "All RStudio settings will be restored to their defaults, and your R session will be restarted.")
    @Key("onClearUserPrefsMessage")
    String onClearUserPrefsMessage();

    @DefaultMessage("Using Zotero")
    @Key("usingZotero")
    String usingZotero();

    @DefaultMessage("Zotero Library:")
    @Key("zoteroLibrary")
    String zoteroLibrary();

    @DefaultMessage("Web")
    @Key("web")
    String web();

    @DefaultMessage("Local")
    @Key("local")
    String local();

    @DefaultMessage("(None)")
    @Key("noneParentheses")
    String noneParentheses();

    @DefaultMessage("General")
    @Key("general")
    String general();

    @DefaultMessage("Line ending conversion:")
    @Key("lineEndingConversion")
    String lineEndingConversion();

    @DefaultMessage("(Use Default)")
    @Key("useDefaultParentheses")
    String useDefaultParentheses();

    @DefaultMessage("None")
    @Key("none")
    String none();

    @DefaultMessage("Platform Native")
    @Key("platformNative")
    String platformNative();

    @DefaultMessage("Posix (LF)")
    @Key("posixLF")
    String posixLF();

    @DefaultMessage("Windows (CR/LF)")
    @Key("windowsCRLF")
    String windowsCRLF();

    @DefaultMessage("Options")
    @Key("options")
    String options();

    @DefaultMessage("Assistive Tools")
    @Key("generalHeaderPanel")
    String generalHeaderPanel();

    @DefaultMessage("Screen reader support (requires restart)")
    @Key("chkScreenReaderLabel")
    String chkScreenReaderLabel();

    @DefaultMessage("Milliseconds after typing before speaking results")
    @Key("typingStatusDelayLabel")
    String typingStatusDelayLabel();

    @DefaultMessage("Maximum number of console output lines to read")
    @Key("maxOutputLabel")
    String maxOutputLabel();

    @DefaultMessage("Other")
    @Key("displayLabel")
    String displayLabel();

    @DefaultMessage("Reduce user interface animations")
    @Key("reducedMotionLabel")
    String reducedMotionLabel();

    @DefaultMessage("Tab key always moves focus")
    @Key("chkTabMovesFocusLabel")
    String chkTabMovesFocusLabel();

    @DefaultMessage("Highlight focused panel")
    @Key("generalPanelLabel")
    String generalPanelLabel();

    @DefaultMessage("RStudio accessibility help")
    @Key("helpRStudioAccessibilityLinkLabel")
    String helpRStudioAccessibilityLinkLabel();

    @DefaultMessage("Enable / Disable Announcements")
    @Key("announcementsLabel")
    String announcementsLabel();

    @DefaultMessage("Accessibility")
    @Key("tabHeaderPanel")
    String tabHeaderPanel();

    @DefaultMessage("General")
    @Key("generalPanelText")
    String generalPanelText();

    @DefaultMessage("Announcements")
    @Key("announcementsPanelText")
    String announcementsPanelText();

    @DefaultMessage("RStudio theme:")
    @Key("appearanceRStudioThemeLabel")
    String appearanceRStudioThemeLabel();

    @DefaultMessage("Zoom:")
    @Key("appearanceZoomLabelZoom")
    String appearanceZoomLabelZoom();

    @DefaultMessage("Editor font (loading...):")
    @Key("fontFaceEditorFontLabel")
    String fontFaceEditorFontLabel();

    @DefaultMessage("Editor font:")
    @Key("appearanceEditorFontLabel")
    String appearanceEditorFontLabel();

    @DefaultMessage("Font size:")
    @Key("appearanceEditorFontSizeLabel")
    String appearanceEditorFontSizeLabel();

    @DefaultMessage("Line height (%):")
    @Key("appearanceEditorLineHeightLabel")
    String appearanceEditorLineHeightLabel();
    

    @DefaultMessage("Editor theme:")
    @Key("appearanceEditorThemeLabel")
    String appearanceEditorThemeLabel();

    @DefaultMessage("Add...")
    @Key("addThemeButtonLabel")
    String addThemeButtonLabel();

    @DefaultMessage("Theme Files (*.tmTheme *.rstheme)")
    @Key("addThemeButtonCaption")
    String addThemeButtonCaption();

    @DefaultMessage("Remove")
    @Key("removeThemeButtonLabel")
    String removeThemeButtonLabel();

    @DefaultMessage("Converting a tmTheme to an rstheme")
    @Key("addThemeUserActionLabel")
    String addThemeUserActionLabel();

    @DefaultMessage("The active theme \"{0}\" could not be found. It''s possible it was removed outside the context of RStudio. Switching to the {1} default theme: \"")
    @Key("setThemeWarningMessage")
    String setThemeWarningMessage(String name, String currentTheme);

    @DefaultMessage("dark")
    @Key("themeWarningMessageDarkLabel")
    String themeWarningMessageDarkLabel();

    @DefaultMessage("light")
    @Key("themeWarningMessageLightLabel")
    String themeWarningMessageLightLabel();

    @DefaultMessage("A theme file with the same name, ''{0}'', already exists. Adding the theme will cause the existing file to be overwritten. Would you like to add the theme anyway?")
    @Key("showThemeExistsDialogLabel")
    String showThemeExistsDialogLabel(String inputFileName);

    @DefaultMessage("Theme File Already Exists")
    @Key("globalDisplayThemeExistsCaption")
    String globalDisplayThemeExistsCaption();

    @DefaultMessage("Unable to add the theme ''")
    @Key("cantAddThemeMessage")
    String cantAddThemeMessage();

    @DefaultMessage("''. The following error occurred: ")
    @Key("cantAddThemeErrorCaption")
    String cantAddThemeErrorCaption();

    @DefaultMessage("Failed to Add Theme")
    @Key("cantAddThemeGlobalMessage")
    String cantAddThemeGlobalMessage();

    @DefaultMessage("Unable to remove the theme ''{0}'': {1}")
    @Key("showCantRemoveThemeDialogMessage")
    String showCantRemoveThemeDialogMessage(String themeName, String errorMessage);

    @DefaultMessage("Failed to Remove Theme")
    @Key("showCantRemoveErrorMessage")
    String showCantRemoveErrorMessage();

    @DefaultMessage("The theme \"{0}\" cannot be removed because it is currently in use. To delete this theme, please change the active theme and retry.")
    @Key("showCantRemoveActiveThemeDialog")
    String showCantRemoveActiveThemeDialog(String themeName);

    @DefaultMessage("Cannot Remove Active Theme")
    @Key("showCantRemoveThemeCaption")
    String showCantRemoveThemeCaption();

    @DefaultMessage("Taking this action will delete the theme \"{0}\" and cannot be undone. Are you sure you wish to continue?")
    @Key("showRemoveThemeWarningMessage")
    String showRemoveThemeWarningMessage(String themeName);

    @DefaultMessage("Remove Theme")
    @Key("showRemoveThemeGlobalMessage")
    String showRemoveThemeGlobalMessage();

    @DefaultMessage("There is an existing theme with the same name as the new theme in the current location. Would you like remove the existing theme, \"{0}\", and add the new theme?")
    @Key("showDuplicateThemeErrorMessage")
    String showDuplicateThemeErrorMessage(String themeName);

    @DefaultMessage("Duplicate Theme In Same Location")
    @Key("showDuplicateThemeDuplicateGlobalMessage")
    String showDuplicateThemeDuplicateGlobalMessage();

    @DefaultMessage("There is an existing theme with the same name as the new theme, \"{0}\" in another location. The existing theme will be hidden but not removed. Removing the new theme later will un-hide the existing theme. Would you like to continue?")
    @Key("showDuplicateThemeWarningMessage")
    String showDuplicateThemeWarningMessage(String themeName);

    @DefaultMessage("Duplicate Theme In Another Location")
    @Key("showDuplicateThemeGlobalMessage")
    String showDuplicateThemeGlobalMessage();

    @DefaultMessage("Appearance")
    @Key("appearanceLabel")
    String appearanceLabel();

    @DefaultMessage("Editor font:")
    @Key("editorFontLabel")
    String editorFontLabel();

    @DefaultMessage("PDF Generation")
    @Key("headerPDFGenerationLabel")
    String headerPDFGenerationLabel();

    @DefaultMessage("NOTE: The Rnw weave and LaTeX compilation options are also set on a per-project (and optionally per-file) basis. Click the help icons above for more details.")
    @Key("perProjectNoteLabel")
    String perProjectNoteLabel();

    @DefaultMessage("LaTeX Editing and Compilation")
    @Key("perProjectHeaderLabel")
    String perProjectHeaderLabel();

    @DefaultMessage("Use tinytex when compiling .tex files")
    @Key("chkUseTinytexLabel")
    String chkUseTinytexLabel();

    @DefaultMessage("Clean auxiliary output after compile")
    @Key("chkCleanTexi2DviOutputLabel")
    String chkCleanTexi2DviOutputLabel();

    @DefaultMessage("Enable shell escape commands")
    @Key("chkEnableShellEscapeLabel")
    String chkEnableShellEscapeLabel();

    @DefaultMessage("Insert numbered sections and subsections")
    @Key("insertNumberedLatexSectionsLabel")
    String insertNumberedLatexSectionsLabel();

    @DefaultMessage("PDF Preview")
    @Key("previewingOptionsHeaderLabel")
    String previewingOptionsHeaderLabel();

    @DefaultMessage("Always enable Rnw concordance (required for synctex)")
    @Key("alwaysEnableRnwConcordanceLabel")
    String alwaysEnableRnwConcordanceLabel();

    @DefaultMessage("Preview PDF after compile using:")
    @Key("pdfPreviewSelectWidgetLabel")
    String pdfPreviewSelectWidgetLabel();

    @DefaultMessage("Help on previewing PDF files")
    @Key("pdfPreviewHelpButtonTitle")
    String pdfPreviewHelpButtonTitle();

    @DefaultMessage("Sweave")
    @Key("preferencesPaneTitle")
    String preferencesPaneTitle();

    @DefaultMessage("(No Preview)")
    @Key("pdfNoPreviewOption")
    String pdfNoPreviewOption();

    @DefaultMessage("(Recommended)")
    @Key("pdfPreviewSumatraOption")
    String pdfPreviewSumatraOption();

    @DefaultMessage("RStudio Viewer")
    @Key("pdfPreviewRStudioViewerOption")
    String pdfPreviewRStudioViewerOption();

    @DefaultMessage("System Viewer")
    @Key("pdfPreviewSystemViewerOption")
    String pdfPreviewSystemViewerOption();

    @DefaultMessage("Execution")
    @Key("consoleExecutionLabel")
    String consoleExecutionLabel();
    

    @DefaultMessage("Discard pending console input on error")
    @Key("consoleDiscardPendingConsoleInputOnErrorLabel")
    String consoleDiscardPendingConsoleInputOnErrorLabel();
    
    

    @DefaultMessage("Display")
    @Key("consoleDisplayLabel")
    String consoleDisplayLabel();
    

    @DefaultMessage("Highlight")
    @Key("consoleHighlightLabel")
    String consoleHighlightLabel();

    @DefaultMessage("Show syntax highlighting in console input")
    @Key("consoleSyntaxHighlightingLabel")
    String consoleSyntaxHighlightingLabel();

    @DefaultMessage("Different color for error or message output (requires restart)")
    @Key("consoleDifferentColorLabel")
    String consoleDifferentColorLabel();

    @DefaultMessage("Limit visible console output (requires restart)")
    @Key("consoleLimitVariableLabel")
    String consoleLimitVariableLabel();

    @DefaultMessage("Truncate lines to maximum length (characters)")
    @Key("consoleLimitOutputLengthLabel")
    String consoleLimitOutputLengthLabel();

    @DefaultMessage("Number of lines to show in console history:")
    @Key("consoleMaxLinesLabel")
    String consoleMaxLinesLabel();

    @DefaultMessage("ANSI Escape Codes:")
    @Key("consoleANSIEscapeCodesLabel")
    String consoleANSIEscapeCodesLabel();

    @DefaultMessage("Show ANSI colors")
    @Key("consoleColorModeANSIOption")
    String consoleColorModeANSIOption();

    @DefaultMessage("Remove ANSI codes")
    @Key("consoleColorModeRemoveANSIOption")
    String consoleColorModeRemoveANSIOption();

    @DefaultMessage("Ignore ANSI codes (1.0 behavior)")
    @Key("consoleColorModeIgnoreANSIOption")
    String consoleColorModeIgnoreANSIOption();

    @DefaultMessage("Console")
    @Key("consoleLabel")
    String consoleLabel();

    @DefaultMessage("Debugging")
    @Key("debuggingHeaderLabel")
    String debuggingHeaderLabel();

    @DefaultMessage("Automatically expand tracebacks in error inspector")
    @Key("debuggingExpandTracebacksLabel")
    String debuggingExpandTracebacksLabel();

    @DefaultMessage("Other")
    @Key("otherHeaderCaption")
    String otherHeaderCaption();

    @DefaultMessage("Double-click to select words")
    @Key("otherDoubleClickLabel")
    String otherDoubleClickLabel();

    @DefaultMessage("Warn when automatic session suspension is paused")
    @Key("WarnAutomaticSuspensionPaused")
    String warnAutoSuspendPausedLabel();

    @DefaultMessage("Number of seconds to delay warning")
    @Key("numberOfSecondsToDelayWarning")
    String numSecondsToDelayWarningLabel();

    @DefaultMessage("R Sessions")
    @Key("rSessionsTitle")
    String rSessionsTitle();

    @DefaultMessage("R version")
    @Key("rVersionTitle")
    String rVersionTitle();

    @DefaultMessage("Change...")
    @Key("rVersionChangeTitle")
    String rVersionChangeTitle();

    @DefaultMessage("Change R Version")
    @Key("rChangeVersionMessage")
    String rChangeVersionMessage();

    @DefaultMessage("You need to quit and re-open RStudio in order for this change to take effect.")
    @Key("rQuitReOpenMessage")
    String rQuitReOpenMessage();

    @DefaultMessage("Loading...")
    @Key("rVersionLoadingText")
    String rVersionLoadingText();

    @DefaultMessage("Restore last used R version for projects")
    @Key("rRestoreLabel")
    String rRestoreLabel();

    @DefaultMessage("Default working directory (when not in a project):")
    @Key("rDefaultDirectoryTitle")
    String rDefaultDirectoryTitle();

    @DefaultMessage("Restore most recently opened project at startup")
    @Key("rRestorePreviousTitle")
    String rRestorePreviousTitle();

    @DefaultMessage("Restore previously open source documents at startup")
    @Key("rRestorePreviousOpenTitle")
    String rRestorePreviousOpenTitle();

    @DefaultMessage("Run Rprofile when resuming suspended session")
    @Key("rRunProfileTitle")
    String rRunProfileTitle();

    @DefaultMessage("Workspace")
    @Key("workspaceCaption")
    String workspaceCaption();

    @DefaultMessage("Restore .RData into workspace at startup")
    @Key("workspaceLabel")
    String workspaceLabel();

    @DefaultMessage("Save workspace to .RData on exit:")
    @Key("saveWorkSpaceLabel")
    String saveWorkSpaceLabel();

    @DefaultMessage("Always")
    @Key("saveWorkAlways")
    String saveWorkAlways();

    @DefaultMessage("Never")
    @Key("saveWorkNever")
    String saveWorkNever();

    @DefaultMessage("Ask")
    @Key("saveWorkAsk")
    String saveWorkAsk();

    @DefaultMessage("History")
    @Key("historyCaption")
    String historyCaption();

    @DefaultMessage("Always save history (even when not saving .RData)")
    @Key("alwaysSaveHistoryLabel")
    String alwaysSaveHistoryLabel();

    @DefaultMessage("Remove duplicate entries in history")
    @Key("removeDuplicatesLabel")
    String removeDuplicatesLabel();

    @DefaultMessage("Other")
    @Key("otherCaption")
    String otherCaption();

    @DefaultMessage("Wrap around when navigating to previous/next tab")
    @Key("otherWrapAroundLabel")
    String otherWrapAroundLabel();

    @DefaultMessage("Automatically notify me of updates to RStudio")
    @Key("otherNotifyMeLabel")
    String otherNotifyMeLabel();

    @DefaultMessage("Send automated crash reports to Posit")
    @Key("otherSendReportsLabel")
    String otherSendReportsLabel();

    @DefaultMessage("Graphics Device")
    @Key("graphicsDeviceCaption")
    String graphicsDeviceCaption();

    @DefaultMessage("Antialiasing:")
    @Key("graphicsAntialiasingLabel")
    String graphicsAntialiasingLabel();

    @DefaultMessage("(Default)")
    @Key("antialiasingDefaultOption")
    String antialiasingDefaultOption();

    @DefaultMessage("None")
    @Key("antialiasingNoneOption")
    String antialiasingNoneOption();

    @DefaultMessage("Gray")
    @Key("antialiasingGrayOption")
    String antialiasingGrayOption();

    @DefaultMessage("Subpixel")
    @Key("antialiasingSubpixelOption")
    String antialiasingSubpixelOption();

    @DefaultMessage("Show server home page:")
    @Key("serverHomePageLabel")
    String serverHomePageLabel();

    @DefaultMessage("Multiple active sessions")
    @Key("serverHomePageActiveSessionsOption")
    String serverHomePageActiveSessionsOption();

    @DefaultMessage("Always")
    @Key("serverHomePageAlwaysOption")
    String serverHomePageAlwaysOption();

    @DefaultMessage("Never")
    @Key("serverHomePageNeverOption")
    String serverHomePageNeverOption();

    @DefaultMessage("Re-use idle sessions for project links")
    @Key("reUseIdleSessionLabel")
    String reUseIdleSessionLabel();

    @DefaultMessage("Home Page")
    @Key("desktopCaption")
    String desktopCaption();

    @DefaultMessage("Debugging")
    @Key("advancedDebuggingCaption")
    String advancedDebuggingCaption();

    @DefaultMessage("Use debug error handler only when my code contains errors")
    @Key("advancedDebuggingLabel")
    String advancedDebuggingLabel();

    @DefaultMessage("OS Integration")
    @Key("advancedOsIntegrationCaption")
    String advancedOsIntegrationCaption();

    @DefaultMessage("Rendering engine:")
    @Key("advancedRenderingEngineLabel")
    String advancedRenderingEngineLabel();

    @DefaultMessage("Auto-detect (recommended)")
    @Key("renderingEngineAutoDetectOption")
    String renderingEngineAutoDetectOption();

    @DefaultMessage("Desktop OpenGL")
    @Key("renderingEngineDesktopOption")
    String renderingEngineDesktopOption();

    @DefaultMessage("OpenGL for Embedded Systems")
    @Key("renderingEngineLinuxDesktopOption")
    String renderingEngineLinuxDesktopOption();

    @DefaultMessage("Software")
    @Key("renderingEngineSoftwareOption")
    String renderingEngineSoftwareOption();

    @DefaultMessage("Use GPU exclusion list (recommended)")
    @Key("useGpuExclusionListLabel")
    String useGpuExclusionListLabel();

    @DefaultMessage("Use GPU driver bug workarounds (recommended)")
    @Key("useGpuDriverBugWorkaroundsLabel")
    String useGpuDriverBugWorkaroundsLabel();

    @DefaultMessage("Enable X11 clipboard monitoring")
    @Key("clipboardMonitoringLabel")
    String clipboardMonitoringLabel();

    @DefaultMessage("Other")
    @Key("otherLabel")
    String otherLabel();

    @DefaultMessage("Experimental Features")
    @Key("experimentalLabel")
    String experimentalLabel();

    @DefaultMessage("English")
    @Key("englishLabel")
    String englishLabel();

    @DefaultMessage("French (Français)")
    @Key("frenchLabel")
    String frenchLabel();

    @DefaultMessage("Show .Last.value in environment listing")
    @Key("otherShowLastDotValueLabel")
    String otherShowLastDotValueLabel();

    @DefaultMessage("Help font size:")
    @Key("helpFontSizeLabel")
    String helpFontSizeLabel();

    /**
     * Translated "General".
     *
     * @return "General"
     */
    @DefaultMessage("General")
    @Key("generalTabListLabel")
    String generalTablistLabel();

    /**
     * Translated "Basic".
     *
     * @return "Basic"
     */
    @DefaultMessage("Basic")
    @Key("generalTabListBasicOption")
    String generalTablListBasicOption();

    /**
     * Translated "Graphics".
     *
     * @return "Graphics"
     */
    @DefaultMessage("Graphics")
    @Key("generalTabListGraphicsOption")
    String generalTablListGraphicsOption();

    /**
     * Translated "Advanced".
     *
     * @return "Advanced"
     */
    @DefaultMessage("Advanced")
    @Key("generalTabListAdvancedOption")
    String generalTabListAdvancedOption();

    /**
     * Translated " (Default)".
     *
     * @return " (Default)"
     */
    @DefaultMessage("(Default)")
    @Key("graphicsBackEndDefaultOption")
    String graphicsBackEndDefaultOption();

    /**
     * Translated "Quartz".
     *
     * @return "Quartz"
     */
    @DefaultMessage("Quartz")
    @Key("graphicsBackEndQuartzOption")
    String graphicsBackEndQuartzOption();

    /**
     * Translated "Windows".
     *
     * @return "Windows"
     */
    @DefaultMessage("Windows")
    @Key("graphicsBackEndWindowsOption")
    String graphicsBackEndWindowsOption();

    /**
     * Translated "Cairo".
     *
     * @return "Cairo"
     */
    @DefaultMessage("Cairo")
    @Key("graphicsBackEndCairoOption")
    String graphicsBackEndCairoOption();

    /**
     * Translated "Cairo PNG".
     *
     * @return "Cairo PNG"
     */
    @DefaultMessage("Cairo PNG")
    @Key("graphicsBackEndCairoPNGOption")
    String graphicsBackEndCairoPNGOption();

    /**
     * Translated "AGG".
     *
     * @return "AGG"
     */
    @DefaultMessage("AGG")
    @Key("graphicsBackEndAGGOption")
    String graphicsBackEndAGGOption();

    /**
     * Translated "Backend:".
     *
     * @return "Backend:"
     */
    @DefaultMessage("Backend:")
    @Key("graphicsBackendLabel")
    String graphicsBackendLabel();

    /**
     * Translated "Using the AGG renderer".
     *
     * @return "Using the AGG renderer"
     */
    @DefaultMessage("Using the AGG renderer")
    @Key("graphicsBackendUserAction")
    String graphicsBackendUserAction();

    /**
     * Translated "Browse...".
     *
     * @return "Browse..."
     */
    @DefaultMessage("Browse...")
    @Key("browseLabel")
    String browseLabel();

    /**
     * Translated "Choose Directory".
     *
     * @return "Choose Directory"
     */
    @DefaultMessage("Choose Directory")
    @Key("directoryLabel")
    String directoryLabel();

    @DefaultMessage("Code")
    @Key("codePaneLabel")
    String codePaneLabel();

    @DefaultMessage("Package Management")
    @Key("packageManagementTitle")
    String packageManagementTitle();

    @DefaultMessage("CRAN repositories modified outside package preferences.")
    @Key("packagesInfoBarText")
    String packagesInfoBarText();
    

    @DefaultMessage("Repositories are being managed by a renv.lock file")
    @Key("packagesRenvInfoBarText")
    String packagesRenvInfoBarText();

    @DefaultMessage("Primary CRAN repository:")
    @Key("cranMirrorTextBoxTitle")
    String cranMirrorTextBoxTitle();

    @DefaultMessage("Change...")
    @Key("cranMirrorChangeLabel")
    String cranMirrorChangeLabel();

    @DefaultMessage("Secondary repositories:")
    @Key("secondaryReposTitle")
    String secondaryReposTitle();

    @DefaultMessage("Enable packages pane")
    @Key("chkEnablePackagesTitle")
    String chkEnablePackagesTitle();

    @DefaultMessage("Use secure download method for HTTP")
    @Key("useSecurePackageDownloadTitle")
    String useSecurePackageDownloadTitle();

    @DefaultMessage("Help on secure package downloads for R")
    @Key("useSecurePackageTitle")
    String useSecurePackageTitle();

    @DefaultMessage("Use Internet Explorer library/proxy for HTTP")
    @Key("useInternetTitle")
    String useInternetTitle();

    @DefaultMessage("Managing Packages")
    @Key("managePackagesTitle")
    String managePackagesTitle();

    @DefaultMessage("Package Development")
    @Key("developmentTitle")
    String developmentTitle();

    @DefaultMessage("C/C++ Development")
    @Key("cppDevelopmentTitle")
    String cppDevelopmentTitle();

    @DefaultMessage("Use devtools package functions if available")
    @Key("useDevtoolsLabel")
    String useDevtoolsLabel();

    @DefaultMessage("Save all files prior to building packages")
    @Key("developmentSaveLabel")
    String developmentSaveLabel();

    @DefaultMessage("Automatically navigate editor to build errors")
    @Key("developmentNavigateLabel")
    String developmentNavigateLabel();

    @DefaultMessage("Hide object files in package src directory")
    @Key("developmentHideLabel")
    String developmentHideLabel();

    @DefaultMessage("Cleanup output after successful R CMD check")
    @Key("developmentCleanupLabel")
    String developmentCleanupLabel();

    @DefaultMessage("View Rcheck directory after failed R CMD check")
    @Key("developmentViewLabel")
    String developmentViewLabel();

    @DefaultMessage("C++ template")
    @Key("developmentCppTemplate")
    String developmentCppTemplate();

    @DefaultMessage("empty")
    @Key("developmentEmptyLabel")
    String developmentEmptyLabel();

    @DefaultMessage("Always use LF line-endings in Unix Makefiles")
    @Key("developmentUseLFLabel")
    String developmentUseLFLabel();

    @DefaultMessage("Packages")
    @Key("tabPackagesPanelTitle")
    String tabPackagesPanelTitle();

    @DefaultMessage("Management")
    @Key("managementPanelTitle")
    String managementPanelTitle();

    @DefaultMessage("Development")
    @Key("developmentManagementPanelTitle")
    String developmentManagementPanelTitle();

    @DefaultMessage("C / C++")
    @Key("C / C++")
    String cppPanelTitle();

    @DefaultMessage("Retrieving list of CRAN mirrors...")
    @Key("chooseMirrorDialogMessage")
    String chooseMirrorDialogMessage();

    @DefaultMessage("Error")
    @Key("showDisconnectErrorCaption")
    String showDisconnectErrorCaption();

    @DefaultMessage("Please select a CRAN Mirror")
    @Key("showDisconnectErrorMessage")
    String showDisconnectErrorMessage();

    @DefaultMessage("Validating CRAN repository...")
    @Key("progressIndicatorMessage")
    String progressIndicatorMessage();

    @DefaultMessage("The given URL does not appear to be a valid CRAN repository.")
    @Key("progressIndicatorError")
    String progressIndicatorError();

    @DefaultMessage("Custom:")
    @Key("customLabel")
    String customLabel();

    @DefaultMessage("CRAN Mirrors:")
    @Key("mirrorsLabel")
    String mirrorsLabel();

    @DefaultMessage("Choose Primary Repository")
    @Key("headerLabel")
    String headerLabel();

    @DefaultMessage("Add...")
    @Key("buttonAddLabel")
    String buttonAddLabel();

    @DefaultMessage("Remove...")
    @Key("buttonRemoveLabel")
    String buttonRemoveLabel();

    @DefaultMessage("Up")
    @Key("buttonUpLabel")
    String buttonUpLabel();

    @DefaultMessage("Down")
    @Key("buttonDownLabel")
    String buttonDownLabel();

    @DefaultMessage("Developing Packages")
    @Key("developingPkgHelpLink")
    String developingPkgHelpLink();

    @DefaultMessage("Retrieving list of secondary repositories...")
    @Key("secondaryReposDialog")
    String secondaryReposDialog();

    @DefaultMessage("Please select or input a CRAN repository")
    @Key("validateSyncLabel")
    String validateSyncLabel();

    @DefaultMessage("The repository ")
    @Key("showErrorRepoMessage")
    String showErrorRepoMessage();

    @DefaultMessage("is already included")
    @Key("alreadyIncludedMessage")
    String alreadyIncludedMessage();

    @DefaultMessage("Validating CRAN repository...")
    @Key("validateAsyncProgress")
    String validateAsyncProgress();

    @DefaultMessage("The given URL does not appear to be a valid CRAN repository.")
    @Key("onResponseReceived")
    String onResponseReceived();

    @DefaultMessage("Name:")
    @Key("nameLabel")
    String nameLabel();

    @DefaultMessage("URL:")
    @Key("urlLabel")
    String urlLabel();

    @DefaultMessage("Available repositories:")
    @Key("reposLabel")
    String reposLabel();

    @DefaultMessage("Add Secondary Repository")
    @Key("secondaryRepoLabel")
    String secondaryRepoLabel();

    @DefaultMessage("Choose the layout of the panels in RStudio by selecting from the controls in each panel. Add up to three additional Source Columns to the left side of the layout. When a column is removed, all saved files within the column are closed and any unsaved files are moved to the main Source Pane.")
    @Key("paneLayoutText")
    String paneLayoutText();

    @DefaultMessage("Manage Column Display")
    @Key("columnToolbarLabel")
    String columnToolbarLabel();

    @DefaultMessage("Add Column")
    @Key("addButtonText")
    String addButtonText();

    @DefaultMessage("Add column")
    @Key("addButtonLabel")
    String addButtonLabel();

    @DefaultMessage("Remove Column")
    @Key("removeButtonText")
    String removeButtonText();

    @DefaultMessage("Remove column")
    @Key("removeButtonLabel")
    String removeButtonLabel();

    @DefaultMessage("Columns and Panes Layout")
    @Key("createGridLabel")
    String createGridLabel();

    @DefaultMessage("Additional source column")
    @Key("createColumnLabel")
    String createColumnLabel();

    @DefaultMessage("Pane Layout")
    @Key("paneLayoutLabel")
    String paneLayoutLabel();

    @DefaultMessage("Publishing Accounts")
    @Key("accountListLabel")
    String accountListLabel();

    @DefaultMessage("Connect...")
    @Key("connectButtonLabel")
    String connectButtonLabel();

    @DefaultMessage("Reconnect...")
    @Key("reconnectButtonLabel")
    String reconnectButtonLabel();

    @DefaultMessage("Disconnect")
    @Key("disconnectButtonLabel")
    String disconnectButtonLabel();

    @DefaultMessage("Account records appear to exist, but cannot be viewed because a required package is not installed.")
    @Key("missingPkgPanelMessage")
    String missingPkgPanelMessage();

    @DefaultMessage("Install Missing Packages")
    @Key("installPkgsMessage")
    String installPkgsMessage();

    @DefaultMessage("Viewing publish accounts")
    @Key("withRSConnectLabel")
    String withRSConnectLabel();

    @DefaultMessage("Enable publishing to Posit Connect")
    @Key("chkEnableRSConnectLabel")
    String chkEnableRSConnectLabel();

    @DefaultMessage("Information about Posit Connect")
    @Key("checkBoxWithHelpTitle")
    String checkBoxWithHelpTitle();

    @DefaultMessage("Settings")
    @Key("settingsHeaderLabel")
    String settingsHeaderLabel();

    @DefaultMessage("Enable publishing documents, apps, and APIs")
    @Key("chkEnablePublishingLabel")
    String chkEnablePublishingLabel();

    @DefaultMessage("Show diagnostic information when publishing")
    @Key("showPublishDiagnosticsLabel")
    String showPublishDiagnosticsLabel();

    @DefaultMessage("SSL Certificates")
    @Key("sSLCertificatesHeaderLabel")
    String sSLCertificatesHeaderLabel();

    @DefaultMessage("Check SSL certificates when publishing")
    @Key("publishCheckCertificatesLabel")
    String publishCheckCertificatesLabel();

    @DefaultMessage("Use custom CA bundle")
    @Key("usePublishCaBundleLabel")
    String usePublishCaBundleLabel();

    @DefaultMessage("(none)")
    @Key("caBundlePath")
    String caBundlePath();

    @DefaultMessage("Troubleshooting Deployments")
    @Key("helpLinkTroubleshooting")
    String helpLinkTroubleshooting();

    @DefaultMessage("Publishing")
    @Key("publishingPaneHeader")
    String publishingPaneHeader();

    @DefaultMessage("Error Disconnecting Account")
    @Key("showErrorCaption")
    String showErrorCaption();

    @DefaultMessage("Please select an account to disconnect.")
    @Key("showErrorMessage")
    String showErrorMessage();

    @DefaultMessage("Confirm Remove Account")
    @Key("removeAccountGlobalDisplay")
    String removeAccountGlobalDisplay();

    @DefaultMessage("Are you sure you want to disconnect the ''{0}'' account on ''{1}''? This won''t delete the account on the server.")
    @Key("removeAccountMessage")
    String removeAccountMessage(String name, String server);

    @DefaultMessage("Disconnect Account")
    @Key("onConfirmDisconnectYesLabel")
    String onConfirmDisconnectYesLabel();

    @DefaultMessage("Cancel")
    @Key("onConfirmDisconnectNoLabel")
    String onConfirmDisconnectNoLabel();

    @DefaultMessage("Error Disconnecting Account")
    @Key("disconnectingErrorMessage")
    String disconnectingErrorMessage();

    @DefaultMessage("Connecting a publishing account")
    @Key("getAccountCountLabel")
    String getAccountCountLabel();

    @DefaultMessage("(No interpreter selected)")
    @Key("pythonPreferencesText")
    String pythonPreferencesText();

    @DefaultMessage("(NOTE: This project has already been configured with its own Python interpreter. Use the Edit Project Options button to change the version of Python used in this project.)")
    @Key("overrideText")
    String overrideText();

    @DefaultMessage("Python")
    @Key("headerPythonLabel")
    String headerPythonLabel();

    @DefaultMessage("The active Python interpreter has been changed by an R startup script.")
    @Key("mismatchWarningBarText")
    String mismatchWarningBarText();

    @DefaultMessage("Finding interpreters...")
    @Key("progressIndicatorText")
    String progressIndicatorText();

    @DefaultMessage("Python interpreter:")
    @Key("tbPythonInterpreterText")
    String tbPythonInterpreterText();

    @DefaultMessage("Select...")
    @Key("tbPythonActionText")
    String tbPythonActionText();

    @DefaultMessage("Error finding Python interpreters: ")
    @Key("onDependencyErrorMessage")
    String onDependencyErrorMessage();

    @DefaultMessage("The selected Python interpreter appears to be invalid.")
    @Key("invalidReasonLabel")
    String invalidReasonLabel();

    @DefaultMessage("Using Python in RStudio")
    @Key("helpRnwButtonLabel")
    String helpRnwButtonLabel();

    @DefaultMessage("Automatically activate project-local Python environments")
    @Key("cbAutoUseProjectInterpreter")
    String cbAutoUseProjectInterpreter();

    @DefaultMessage("When enabled, RStudio will automatically find and activate a Python environment located within the project root directory (if any).")
    @Key("cbAutoUseProjectInterpreterMessage")
    String cbAutoUseProjectInterpreterMessage();

    @DefaultMessage("General")
    @Key("tabPanelCaption")
    String tabPanelCaption();

    @DefaultMessage("Clear")
    @Key("clearLabel")
    String clearLabel();

    @DefaultMessage("System")
    @Key("systemTab")
    String systemTab();

    @DefaultMessage("Virtual Environments")
    @Key("virtualEnvTab")
    String virtualEnvTab();

    @DefaultMessage("Conda Environments")
    @Key("condaEnvTab")
    String condaEnvTab();

    @DefaultMessage("[Unknown]")
    @Key("unknownType")
    String unknownType();

    @DefaultMessage("Virtual Environment")
    @Key("virtualEnvironmentType")
    String virtualEnvironmentType();

    @DefaultMessage("Conda Environment")
    @Key("condaEnvironmentType")
    String condaEnvironmentType();

    @DefaultMessage("System Interpreter")
    @Key("systemInterpreterType")
    String systemInterpreterType();

    @DefaultMessage("This version of RStudio includes a preview of Quarto, a new scientific and technical publishing system. ")
    @Key("quartoPreviewLabel")
    String quartoPreviewLabel();

    /**
     * Get locale value of the label for the checkbox to enable the
     * Quarto preview. Default value is "Enable Quarto preview".
     *
     * @return the translated value for the label
     */
    @DefaultMessage("Enable Quarto preview")
    @Key("enableQuartoPreviewCheckboxLabel")
    String enableQuartoPreviewCheckboxLabel();

    /**
     * Get locale value for the help link caption. Default value
     * is "Learn more about Quarto".
     *
     * @return the translated value for the help link
     */
    @DefaultMessage("Learn more about Quarto")
    @Key("helpLinkCaption")
    String helpLinkCaption();

    @DefaultMessage("R Markdown")
    @Key("rMarkdownHeaderLabel")
    String rMarkdownHeaderLabel();

    @DefaultMessage("Document Outline")
    @Key("documentOutlineHeaderLabel")
    String documentOutlineHeaderLabel();

    @DefaultMessage("Show document outline by default")
    @Key("rMarkdownShowLabel")
    String rMarkdownShowLabel();

    @DefaultMessage("Soft-wrap R Markdown files")
    @Key("rMarkdownSoftWrapLabel")
    String rMarkdownSoftWrapLabel();

    @DefaultMessage("Show in document outline: ")
    @Key("docOutlineDisplayLabel")
    String docOutlineDisplayLabel();

    @DefaultMessage("Sections Only")
    @Key("docOutlineSectionsOption")
    String docOutlineSectionsOption();

    @DefaultMessage("Sections and Named Chunks")
    @Key("docOutlineSectionsNamedChunksOption")
    String docOutlineSectionsNamedChunksOption();

    @DefaultMessage("Sections and All Chunks")
    @Key("docOutlineSectionsAllChunksOption")
    String docOutlineSectionsAllChunksOption();

    @DefaultMessage("Show output preview in: ")
    @Key("rmdViewerModeLabel")
    String rmdViewerModeLabel();

    @DefaultMessage("Window")
    @Key("rmdViewerModeWindowOption")
    String rmdViewerModeWindowOption();

    @DefaultMessage("Viewer Pane")
    @Key("rmdViewerModeViewerPaneOption")
    String rmdViewerModeViewerPaneOption();

    @DefaultMessage("(None)")
    @Key("rmdViewerModeNoneOption")
    String rmdViewerModeNoneOption();

    @DefaultMessage("Show output inline for all R Markdown documents")
    @Key("rmdInlineOutputLabel")
    String rmdInlineOutputLabel();

    @DefaultMessage("Show equation and image previews: ")
    @Key("latexPreviewWidgetLabel")
    String latexPreviewWidgetLabel();

    @DefaultMessage("Never")
    @Key("latexPreviewWidgetNeverOption")
    String latexPreviewWidgetNeverOption();

    @DefaultMessage("In a popup")
    @Key("latexPreviewWidgetPopupOption")
    String latexPreviewWidgetPopupOption();

    @DefaultMessage("Inline")
    @Key("latexPreviewWidgetInlineOption")
    String latexPreviewWidgetInlineOption();

    @DefaultMessage("Evaluate chunks in directory: ")
    @Key("knitWorkingDirLabel")
    String knitWorkingDirLabel();

    @DefaultMessage("Document")
    @Key("knitWorkingDirDocumentOption")
    String knitWorkingDirDocumentOption();

    @DefaultMessage("Current")
    @Key("knitWorkingDirCurrentOption")
    String knitWorkingDirCurrentOption();

    @DefaultMessage("Project")
    @Key("knitWorkingDirProjectOption")
    String knitWorkingDirProjectOption();

    @DefaultMessage("R Notebooks")
    @Key("rNotebooksCaption")
    String rNotebooksCaption();

    @DefaultMessage("Execute setup chunk automatically in notebooks")
    @Key("autoExecuteSetupChunkLabel")
    String autoExecuteSetupChunkLabel();

    @DefaultMessage("Hide console automatically when executing notebook chunks")
    @Key("notebookHideConsoleLabel")
    String notebookHideConsoleLabel();

    @DefaultMessage("Using R Notebooks")
    @Key("helpRStudioLinkLabel")
    String helpRStudioLinkLabel();

    @DefaultMessage("Display")
    @Key("advancedHeaderLabel")
    String advancedHeaderLabel();

    @DefaultMessage("Enable chunk background highlight")
    @Key("advancedEnableChunkLabel")
    String advancedEnableChunkLabel();

    @DefaultMessage("Show inline toolbar for R code chunks")
    @Key("advancedShowInlineLabel")
    String advancedShowInlineLabel();

    @DefaultMessage("Display render command in R Markdown tab")
    @Key("advancedDisplayRender")
    String advancedDisplayRender();

    @DefaultMessage("General")
    @Key("visualModeGeneralCaption")
    String visualModeGeneralCaption();

    @DefaultMessage("Use visual editor by default for new documents")
    @Key("visualModeUseVisualEditorLabel")
    String visualModeUseVisualEditorLabel();

    @DefaultMessage("Learn more about visual editing mode")
    @Key("visualModeHelpLink")
    String visualModeHelpLink();

    @DefaultMessage("Display")
    @Key("visualModeHeaderLabel")
    String visualModeHeaderLabel();

    @DefaultMessage("Show document outline by default")
    @Key("visualEditorShowOutlineLabel")
    String visualEditorShowOutlineLabel();

    @DefaultMessage("Show margin column indicator in code blocks")
    @Key("visualEditorShowMarginLabel")
    String visualEditorShowMarginLabel();

    @DefaultMessage("Editor content width (px):")
    @Key("visualModeContentWidthLabel")
    String visualModeContentWidthLabel();

    @DefaultMessage("Editor font size:")
    @Key("visualModeFontSizeLabel")
    String visualModeFontSizeLabel();

    @DefaultMessage("Markdown")
    @Key("visualModeOptionsMarkdownCaption")
    String visualModeOptionsMarkdownCaption();

    @DefaultMessage("Default spacing between list items: ")
    @Key("visualModeListSpacingLabel")
    String visualModeListSpacingLabel();

    @DefaultMessage("Automatic text wrapping (line breaks): ")
    @Key("visualModeWrapLabel")
    String visualModeWrapLabel();

    @DefaultMessage("Learn more about automatic line wrapping")
    @Key("visualModeWrapHelpLabel")
    String visualModeWrapHelpLabel();

    @DefaultMessage("Wrap at column:")
    @Key("visualModeOptionsLabel")
    String visualModeOptionsLabel();

    @DefaultMessage("Write references at end of current: ")
    @Key("visualModeReferencesLabel")
    String visualModeReferencesLabel();

    @DefaultMessage("Write canonical visual mode markdown in source mode")
    @Key("visualModeCanonicalLabel")
    String visualModeCanonicalLabel();

    @DefaultMessage("Visual Mode Preferences")
    @Key("visualModeCanonicalMessageCaption")
    String visualModeCanonicalMessageCaption();

    @DefaultMessage("Are you sure you want to write canonical markdown from source mode for all R Markdown files?\n\nThis preference should generally only be used at a project level (to prevent re-writing of markdown source that you or others don''t intend to use with visual mode).\n\nChange this preference now?")
    @Key("visualModeCanonicalPreferenceMessage")
    String visualModeCanonicalPreferenceMessage();

    @DefaultMessage("Learn more about markdown writer options")
    @Key("markdownPerFileOptionsHelpLink")
    String markdownPerFileOptionsHelpLink();

    @DefaultMessage("Citation features are available within visual editing mode.")
    @Key("citationsLabel")
    String citationsLabel();

    @DefaultMessage("Learn more about using citations with visual editing mode")
    @Key("citationsHelpLink")
    String citationsHelpLink();

    @DefaultMessage("Zotero")
    @Key("zoteroHeaderLabel")
    String zoteroHeaderLabel();

    @DefaultMessage("Zotero Data Directory:")
    @Key("zoteroDataDirLabel")
    String zoteroDataDirLabel();

    @DefaultMessage("(None Detected)")
    @Key("zoteroDataDirNotDectedLabel")
    String zoteroDataDirNotDectedLabel();

    @DefaultMessage("Use Better BibTeX for citation keys and BibTeX export")
    @Key("zoteroUseBetterBibtexLabel")
    String zoteroUseBetterBibtexLabel();

    @DefaultMessage("R Markdown")
    @Key("tabPanelTitle")
    String tabPanelTitle();

    @DefaultMessage("Basic")
    @Key("tabPanelBasic")
    String tabPanelBasic();

    @DefaultMessage("Advanced")
    @Key("tabPanelAdvanced")
    String tabPanelAdvanced();

    @DefaultMessage("Visual")
    @Key("tabPanelVisual")
    String tabPanelVisual();

    @DefaultMessage("Citations")
    @Key("tabPanelCitations")
    String tabPanelCitations();

    @DefaultMessage("Web")
    @Key("webOption")
    String webOption();

    @DefaultMessage("Show line numbers in code blocks")
    @Key("showLinkNumbersLabel")
    String showLinkNumbersLabel();

    @DefaultMessage("Enable version control interface for RStudio projects")
    @Key("chkVcsEnabledLabel")
    String chkVcsEnabledLabel();

    @DefaultMessage("Enable")
    @Key("globalDisplayEnable")
    String globalDisplayEnable();

    @DefaultMessage("Disable")
    @Key("globalDisplayDisable")
    String globalDisplayDisable();

    @DefaultMessage("{0} Version Control ")
    @Key("globalDisplayVC")
    String globalDisplayVC(String displayEnable);

    @DefaultMessage("You must restart RStudio for this change to take effect.")
    @Key("globalDisplayVCMessage")
    String globalDisplayVCMessage();

    @DefaultMessage("The program ''{0}'' is unlikely to be a valid git executable.\nPlease select a git executable called ''git.exe''.")
    @Key("gitExePathMessage")
    String gitExePathMessage(String gitPath);

    @DefaultMessage("Invalid Git Executable")
    @Key("gitGlobalDisplay")
    String gitGlobalDisplay();

    @DefaultMessage("Git executable:")
    @Key("gitExePathLabel")
    String gitExePathLabel();

    @DefaultMessage("(Not Found)")
    @Key("gitExePathNotFoundLabel")
    String gitExePathNotFoundLabel();

    @DefaultMessage("SVN executable:")
    @Key("svnExePathLabel")
    String svnExePathLabel();

    @DefaultMessage("Terminal executable:")
    @Key("terminalPathLabel")
    String terminalPathLabel();

    @DefaultMessage("Git/SVN")
    @Key("gitSVNPaneHeader")
    String gitSVNPaneHeader();

    @DefaultMessage("Dictionaries")
    @Key("spellingPreferencesPaneHeader")
    String spellingPreferencesPaneHeader();

    @DefaultMessage("Ignore")
    @Key("ignoreHeader")
    String ignoreHeader();

    @DefaultMessage("Ignore words in UPPERCASE")
    @Key("ignoreWordsUppercaseLabel")
    String ignoreWordsUppercaseLabel();

    @DefaultMessage("Ignore words with numbers")
    @Key("ignoreWordsNumbersLabel")
    String ignoreWordsNumbersLabel();

    @DefaultMessage("Checking")
    @Key("checkingHeader")
    String checkingHeader();

    @DefaultMessage("Use real time spell-checking")
    @Key("realTimeSpellcheckingCheckboxLabel")
    String realTimeSpellcheckingCheckboxLabel();

    @DefaultMessage("User dictionary: ")
    @Key("kUserDictionaryLabel")
    String kUserDictionaryLabel();

    @DefaultMessage("{0}{1} words")
    @Key("kUserDictionaryWordsLabel")
    String kUserDictionaryWordsLabel(String kUserDictionary, String entries);

    @DefaultMessage("Edit User Dictionary...")
    @Key("editUserDictLabel")
    String editUserDictLabel();

    @DefaultMessage("Edit User Dictionary")
    @Key("editUserDictCaption")
    String editUserDictCaption();

    @DefaultMessage("Save")
    @Key("editUserDictSaveCaption")
    String editUserDictSaveCaption();

    @DefaultMessage("Spelling")
    @Key("spellingPaneLabel")
    String spellingPaneLabel();

    @DefaultMessage("Edit")
    @Key("editDialog")
    String editDialog();

    @DefaultMessage("Save")
    @Key("saveDialog")
    String saveDialog();

    @DefaultMessage("Cancel")
    @Key("cancelButton")
    String cancelButton();

    @DefaultMessage("Shell")
    @Key("shellHeaderLabel")
    String shellHeaderLabel();

    @DefaultMessage("Initial directory:")
    @Key("initialDirectoryLabel")
    String initialDirectoryLabel();

    @DefaultMessage("Project directory")
    @Key("projectDirectoryOption")
    String projectDirectoryOption();

    @DefaultMessage("Current directory")
    @Key("currentDirectoryOption")
    String currentDirectoryOption();

    @DefaultMessage("Home directory")
    @Key("homeDirectoryOption")
    String homeDirectoryOption();

    @DefaultMessage("New terminals open with:")
    @Key("terminalShellLabel")
    String terminalShellLabel();

    @DefaultMessage("The program ''{0}'' is unlikely to be a valid shell executable.")
    @Key("shellExePathMessage")
    String shellExePathMessage(String shellExePath);

    @DefaultMessage("Invalid Shell Executable")
    @Key("shellExeCaption")
    String shellExeCaption();

    @DefaultMessage("Custom shell binary:")
    @Key("customShellPathLabel")
    String customShellPathLabel();

    @DefaultMessage("(Not Found)")
    @Key("customShellChooserEmptyLabel")
    String customShellChooserEmptyLabel();

    @DefaultMessage("Custom shell command-line options:")
    @Key("customShellOptionsLabel")
    String customShellOptionsLabel();

    @DefaultMessage("Connection")
    @Key("perfLabel")
    String perfLabel();

    @DefaultMessage("Local terminal echo")
    @Key("chkTerminalLocalEchoLabel")
    String chkTerminalLocalEchoLabel();

    @DefaultMessage("Local echo is more responsive but may get out of sync with some line-editing modes or custom shells.")
    @Key("chkTerminalLocalEchoTitle")
    String chkTerminalLocalEchoTitle();

    @DefaultMessage("Connect with WebSockets")
    @Key("chkTerminalWebsocketLabel")
    String chkTerminalWebsocketLabel();

    @DefaultMessage("WebSockets are generally more responsive; try turning off if terminal won''t connect.")
    @Key("chkTerminalWebsocketTitle")
    String chkTerminalWebsocketTitle();

    @DefaultMessage("Display")
    @Key("displayHeaderLabel")
    String displayHeaderLabel();

    @DefaultMessage("Hardware acceleration")
    @Key("chkHardwareAccelerationLabel")
    String chkHardwareAccelerationLabel();

    @DefaultMessage("Audible bell")
    @Key("chkAudibleBellLabel")
    String chkAudibleBellLabel();

    @DefaultMessage("Clickable web links")
    @Key("chkWebLinksLabel")
    String chkWebLinksLabel();

    @DefaultMessage("Using the RStudio terminal")
    @Key("helpLinkLabel")
    String helpLinkLabel();

    @DefaultMessage("Miscellaneous")
    @Key("miscLabel")
    String miscLabel();

    @DefaultMessage("When shell exits:")
    @Key("autoClosePrefLabel")
    String autoClosePrefLabel();

    @DefaultMessage("Close the pane")
    @Key("closePaneOption")
    String closePaneOption();

    @DefaultMessage("Don''t close the pane")
    @Key("doNotClosePaneOption")
    String doNotClosePaneOption();

    @DefaultMessage("Close pane if shell exits cleanly")
    @Key("shellExitsPaneOption")
    String shellExitsPaneOption();

    @DefaultMessage("Save and restore environment variables")
    @Key("chkCaptureEnvLabel")
    String chkCaptureEnvLabel();

    @DefaultMessage("Terminal occasionally runs a hidden command to capture state of environment variables.")
    @Key("chkCaptureEnvTitle")
    String chkCaptureEnvTitle();

    @DefaultMessage("Process Termination")
    @Key("shutdownLabel")
    String shutdownLabel();

    @DefaultMessage("Ask before killing processes:")
    @Key("busyModeLabel")
    String busyModeLabel();

    @DefaultMessage("Don''t ask before killing:")
    @Key("busyWhitelistLabel")
    String busyWhitelistLabel();

    @DefaultMessage("Terminal")
    @Key("terminalPaneLabel")
    String terminalPaneLabel();

    @DefaultMessage("General")
    @Key("tabGeneralPanelLabel")
    String tabGeneralPanelLabel();

    @DefaultMessage("Closing")
    @Key("tabClosingPanelLabel")
    String tabClosingPanelLabel();

    @DefaultMessage("Always")
    @Key("busyModeAlwaysOption")
    String busyModeAlwaysOption();

    @DefaultMessage("Never")
    @Key("busyModeNeverOption")
    String busyModeNeverOption();

    @DefaultMessage("Always except for list")
    @Key("busyModeListOption")
    String busyModeListOption();

    @DefaultMessage("Enable Python integration")
    @Key("chkPythonIntegration")
    String chkPythonIntegration();

    @DefaultMessage("When enabled, the active version of Python will be placed on the PATH for new terminal sessions. Only bash and zsh are supported.")
    @Key("chkPythonIntegrationTitle")
    String chkPythonIntegrationTitle();

    @DefaultMessage("History")
    @Key("historyTab")
    String historyTab();

    @DefaultMessage("Files")
    @Key("filesTab")
    String filesTab();

    @DefaultMessage("Plots")
    @Key("plotsTab")
    String plotsTab();

    @DefaultMessage("Connections")
    @Key("connectionsTab")
    String connectionsTab();

    @DefaultMessage("Packages")
    @Key("packagesTab")
    String packagesTab();

    @DefaultMessage("Help")
    @Key("helpTab")
    String helpTab();

    @DefaultMessage("Build")
    @Key("buildTab")
    String buildTab();

    @DefaultMessage("VCS")
    @Key("vcsTab")
    String vcsTab();

    @DefaultMessage("Tutorial")
    @Key("tutorialTab")
    String tutorialTab();

    @DefaultMessage("Viewer")
    @Key("viewerTab")
    String viewerTab();

    @DefaultMessage("Presentation")
    @Key("presentationTab")
    String presentationTab();

    @DefaultMessage("Confirm Remove")
    @Key("confirmRemoveCaption")
    String confirmRemoveCaption();

    @DefaultMessage("Are you sure you want to remove the {0} repository?")
    @Key("confirmRemoveMessage")
    String confirmRemoveMessage(String repo);

    @DefaultMessage("Modern")
    @Key("modernThemeLabel")
    String modernThemeLabel();

    @DefaultMessage("Sky")
    @Key("skyThemeLabel")
    String skyThemeLabel();

    @DefaultMessage("General")
    @Key("generalHeaderLabel")
    String generalHeaderLabel();
    

    @DefaultMessage("Code Formatting")
    @Key("codeFormattingHeaderLabel")
    String codeFormattingHeaderLabel();
 

    @DefaultMessage("Use formatter:")
    @Key("useFormatterLabel")
    String useFormatterLabel();
    

    @DefaultMessage("Syntax")
    @Key("syntaxHeaderLabel")
    String syntaxHeaderLabel();

    @DefaultMessage("Edit Snippets...")
    @Key("editSnippetsButtonLabel")
    String editSnippetsButtonLabel();

    @DefaultMessage("tight")
    @Key("listSpacingTight")
    String listSpacingTight();

    @DefaultMessage("spaced")
    @Key("listSpacingSpaced")
    String listSpacingSpaced();

    @DefaultMessage("(none)")
    @Key("editingWrapNone")
    String editingWrapNone();

    @DefaultMessage("(column)")
    @Key("editingWrapColumn")
    String editingWrapColumn();

    @DefaultMessage("(sentence)")
    @Key("editingWrapSentence")
    String editingWrapSentence();

    @DefaultMessage("block")
    @Key("refLocationBlock")
    String refLocationBlock();

    @DefaultMessage("section")
    @Key("refLocationSection")
    String refLocationSection();

    @DefaultMessage("document")
    @Key("refLocationDocument")
    String refLocationDocument();

    @DefaultMessage("Other Languages")
    @Key("editingDiagOtherLabel")
    String editingDiagOtherLabel();

    @DefaultMessage("Show Diagnostics")
    @Key("editingDiagShowLabel")
    String editingDiagShowLabel();

    @DefaultMessage("R Diagnostics")
    @Key("editingDiagnosticsPanel")
    String editingDiagnosticsPanel();

    @DefaultMessage("General")
    @Key("editingDisplayPanel")
    String editingDisplayPanel();

    @DefaultMessage("Modify Keyboard Shortcuts...")
    @Key("editingEditShortcuts")
    String editingEditShortcuts();

    @DefaultMessage("Execution")
    @Key("editingExecutionLabel")
    String editingExecutionLabel();

    @DefaultMessage("Completion Delay")
    @Key("editingHeaderLabel")
    String editingHeaderLabel();

    @DefaultMessage("Other Languages")
    @Key("editingOtherLabel")
    String editingOtherLabel();

    @DefaultMessage("Keyword and text-based completions are supported for several other languages including JavaScript, HTML, CSS, Python, and SQL.")
    @Key("editingOtherTip")
    String editingOtherTip();

    @DefaultMessage("General")
    @Key("editingSavePanel")
    String editingSavePanel();

    @DefaultMessage("Change...")
    @Key("editingSavePanelAction")
    String editingSavePanelAction();

    @DefaultMessage("Autosave")
    @Key("editingSavePanelAutosave")
    String editingSavePanelAutosave();

    @DefaultMessage("Serialization")
    @Key("editingSerializationLabel")
    String editingSerializationLabel();

    @DefaultMessage("Help on code snippets")
    @Key("editingSnippetHelpTitle")
    String editingSnippetHelpTitle();

    @DefaultMessage("Snippets")
    @Key("editingSnippetsLabel")
    String editingSnippetsLabel();

    @DefaultMessage("Editing")
    @Key("editingTabPanel")
    String editingTabPanel();

    @DefaultMessage("Completion")
    @Key("editingTabPanelCompletionPanel")
    String editingTabPanelCompletionPanel();

    @DefaultMessage("Diagnostics")
    @Key("editingTabPanelDiagnosticsPanel")
    String editingTabPanelDiagnosticsPanel();

    @DefaultMessage("Display")
    @Key("editingTabPanelDisplayPanel")
    String editingTabPanelDisplayPanel();

    @DefaultMessage("Formatting")
    @Key("editingTabPanelFormattingPanel")
    String editingTabPanelFormattingPanel();
    

    @DefaultMessage("Saving")
    @Key("editingTabPanelSavePanel")
    String editingTabPanelSavePanel();

    @DefaultMessage("R and C/C++")
    @Key("editingCompletionPanel")
    String editingCompletionPanel();

    @DefaultMessage("General")
    @Key("editingHeader")
    String editingHeader();

    @DefaultMessage("No bindings available")
    @Key("editingKeyboardShortcuts")
    String editingKeyboardShortcuts();

    @DefaultMessage("Keyboard Shortcuts")
    @Key("editingKeyboardText")
    String editingKeyboardText();

    @DefaultMessage("Customized")
    @Key("editingRadioCustomized")
    String editingRadioCustomized();

    @DefaultMessage("Filter...")
    @Key("editingFilterWidget")
    String editingFilterWidget();

    @DefaultMessage("Reset...")
    @Key("editingResetText")
    String editingResetText();

    @DefaultMessage("Reset Keyboard Shortcuts")
    @Key("editingGlobalDisplay")
    String editingGlobalDisplay();

    @DefaultMessage("Are you sure you want to reset keyboard shortcuts to their default values? ")
    @Key("editingGlobalCaption")
    String editingGlobalCaption();

    @DefaultMessage("This action cannot be undone.")
    @Key("editingGlobalMessage")
    String editingGlobalMessage();

    @DefaultMessage("Resetting Keyboard Shortcuts...")
    @Key("editingProgressMessage")
    String editingProgressMessage();

    @DefaultMessage("Cancel")
    @Key("editingCancelShortcuts")
    String editingCancelShortcuts();

    @DefaultMessage("Tab width")
    @Key("editingTabWidthLabel")
    String editingTabWidthLabel();

    @DefaultMessage("Editor scroll speed sensitivity:")
    @Key("editorScrollMultiplier")
    String editorScrollMultiplier();

    @DefaultMessage("Adjust the editor scroll speed sensitivity. Higher is faster.")
    @Key("editorScrollMultiplierDesc")
    String editorScrollMultiplierDesc();

    @DefaultMessage("Auto-detect code indentation")
    @Key("editingAutoDetectIndentationLabel")
    String editingAutoDetectIndentationLabel();

    @DefaultMessage("When enabled, the indentation for documents not part of an RStudio project will be automatically detected.")
    @Key("editingAutoDetectIndentationDesc")
    String editingAutoDetectIndentationDesc();

    @DefaultMessage("Insert matching parens/quotes")
    @Key("editingInsertMatchingLabel")
    String editingInsertMatchingLabel();

    @DefaultMessage("Use native pipe operator, |> (requires R 4.1+)")
    @Key("editingUseNativePipeOperatorLabel")
    String editingUseNativePipeOperatorLabel();

    @DefaultMessage("NOTE: Some of these settings may be overridden by project-specific options.")
    @Key("editingProjectOverrideInfoText")
    String editingProjectOverrideInfoText();

    @DefaultMessage("Edit Project Options...")
    @Key("editProjectPreferencesButtonLabel")
    String editProjectPreferencesButtonLabel();

    @DefaultMessage("Auto-indent code after paste")
    @Key("editingReindentOnPasteLabel")
    String editingReindentOnPasteLabel();

    @DefaultMessage("Vertically align arguments in auto-indent")
    @Key("editingVerticallyAlignArgumentsIndentLabel")
    String editingVerticallyAlignArgumentsIndentLabel();

    @DefaultMessage("Continue comment when inserting new line")
    @Key("editingContinueCommentsOnNewlineLabel")
    String editingContinueCommentsOnNewlineLabel();

    @DefaultMessage("When enabled, pressing Enter will continue comments on new lines. Press Shift + Enter to exit a comment.")
    @Key("editingContinueCommentsOnNewlineDesc")
    String editingContinueCommentsOnNewlineDesc();

    @DefaultMessage("Enable hyperlink highlighting in editor")
    @Key("editingHighlightWebLinkLabel")
    String editingHighlightWebLinkLabel();

    @DefaultMessage("When enabled, hyperlinks in comments will be underlined and clickable.")
    @Key("editingHighlightWebLinkDesc")
    String editingHighlightWebLinkDesc();

    @DefaultMessage("Surround selection on text insertion:")
    @Key("editingSurroundSelectionLabel")
    String editingSurroundSelectionLabel();

    @DefaultMessage("Keybindings:")
    @Key("editingKeybindingsLabel")
    String editingKeybindingsLabel();

    @DefaultMessage("Focus console after executing from source")
    @Key("editingFocusConsoleAfterExecLabel")
    String editingFocusConsoleAfterExecLabel();

    @DefaultMessage("Ctrl+Enter executes:")
    @Key("editingExecutionBehaviorLabel")
    String editingExecutionBehaviorLabel();

    @DefaultMessage("Highlight selected word")
    @Key("displayHighlightSelectedWordLabel")
    String displayHighlightSelectedWordLabel();

    @DefaultMessage("Highlight selected line")
    @Key("displayHighlightSelectedLineLabel")
    String displayHighlightSelectedLineLabel();

    /**
      * Translated "Show line numbers"
      *
      * @return translated "Show line numbers"
      */
    @DefaultMessage("Show line numbers")
    @Key("displayShowLineNumbersLabel")
    String displayShowLineNumbersLabel();

    @DefaultMessage("Relative line numbers")
    @Key("displayRelativeLineNumbersLabel")
    String displayRelativeLineNumbersLabel();

    @DefaultMessage("Show margin")
    @Key("displayShowMarginLabel")
    String displayShowMarginLabel();

    @DefaultMessage("Show whitespace characters")
    @Key("displayShowInvisiblesLabel")
    String displayShowInvisiblesLabel();

    @DefaultMessage("Show indent guides")
    @Key("displayShowIndentGuidesLabel")
    String displayShowIndentGuidesLabel();

    @DefaultMessage("Blinking cursor")
    @Key("displayBlinkingCursorLabel")
    String displayBlinkingCursorLabel();

    @DefaultMessage("Allow scroll past end of document")
    @Key("displayScrollPastEndOfDocumentLabel")
    String displayScrollPastEndOfDocumentLabel();

    @DefaultMessage("Allow drag and drop of text")
    @Key("displayEnableTextDragLabel")
    String displayEnableTextDragLabel();

    @DefaultMessage("Fold Style:")
    @Key("displayFoldStyleLabel")
    String displayFoldStyleLabel();

    @DefaultMessage("Ensure that source files end with newline")
    @Key("savingAutoAppendNewLineLabel")
    String savingAutoAppendNewLineLabel();

    @DefaultMessage("Strip trailing horizontal whitespace when saving")
    @Key("savingStripTrailingWhitespaceLabel")
    String savingStripTrailingWhitespaceLabel();

    @DefaultMessage("Restore last cursor position when opening file")
    @Key("savingRestoreSourceDocumentCursorPositionLabel")
    String savingRestoreSourceDocumentCursorPositionLabel();

    @DefaultMessage("Default text encoding:")
    @Key("savingDefaultEncodingLabel")
    String savingDefaultEncodingLabel();

    @DefaultMessage("Always save R scripts before sourcing")
    @Key("savingSaveBeforeSourcingLabel")
    String savingSaveBeforeSourcingLabel();

    @DefaultMessage("Automatically save when editor loses focus")
    @Key("savingAutoSaveOnBlurLabel")
    String savingAutoSaveOnBlurLabel();

    @DefaultMessage("When editor is idle:")
    @Key("savingAutoSaveOnIdleLabel")
    String savingAutoSaveOnIdleLabel();

    @DefaultMessage("Idle period:")
    @Key("savingAutoSaveIdleMsLabel")
    String savingAutoSaveIdleMsLabel();

    @DefaultMessage("Show code completions:")
    @Key("completionCodeCompletionLabel")
    String completionCodeCompletionLabel();

    @DefaultMessage("Show code completions:")
    @Key("completionCodeCompletionOtherLabel")
    String completionCodeCompletionOtherLabel();

    @DefaultMessage("Allow automatic completions in console")
    @Key("completionConsoleCodeCompletionLabel")
    String completionConsoleCodeCompletionLabel();

    @DefaultMessage("Insert parentheses after function completions")
    @Key("completionInsertParensAfterFunctionCompletion")
    String completionInsertParensAfterFunctionCompletion();

    @DefaultMessage("Show help tooltip after function completions")
    @Key("completionShowFunctionSignatureTooltipsLabel")
    String completionShowFunctionSignatureTooltipsLabel();

    @DefaultMessage("Show help tooltip on cursor idle")
    @Key("completionShowHelpTooltipOnIdleLabel")
    String completionShowHelpTooltipOnIdleLabel();

    @DefaultMessage("Insert spaces around equals for argument completions")
    @Key("completionInsertSpacesAroundEqualsLabel")
    String completionInsertSpacesAroundEqualsLabel();

    @DefaultMessage("Use tab for autocompletions")
    @Key("completionTabCompletionLabel")
    String completionTabCompletionLabel();

    @DefaultMessage("Use tab for multiline autocompletions")
    @Key("completionTabMultilineCompletionLabel")
    String completionTabMultilineCompletionLabel();

    @DefaultMessage("Show completions after characters entered:")
    @Key("completionCodeCompletionCharactersLabel")
    String completionCodeCompletionCharactersLabel();

    @DefaultMessage("Show completions after keyboard idle (ms):")
    @Key("completionCodeCompletionDelayLabel")
    String completionCodeCompletionDelayLabel();

    @DefaultMessage("Show diagnostics for R")
    @Key("diagnosticsShowDiagnosticsRLabel")
    String diagnosticsShowDiagnosticsRLabel();

    @DefaultMessage("Enable diagnostics within R function calls")
    @Key("diagnosticsInRFunctionCallsLabel")
    String diagnosticsInRFunctionCallsLabel();

    @DefaultMessage("Check arguments to R function calls")
    @Key("diagnosticsCheckArgumentsToRFunctionCallsLabel")
    String diagnosticsCheckArgumentsToRFunctionCallsLabel();

    @DefaultMessage("Check usage of '<-' in function call")
    @Key("diagnosticsCheckUnexpectedAssignmentInFunctionCallLabel")
    String diagnosticsCheckUnexpectedAssignmentInFunctionCallLabel();

    @DefaultMessage("Warn if variable used has no definition in scope")
    @Key("diagnosticsWarnIfNoSuchVariableInScopeLabel")
    String diagnosticsWarnIfNoSuchVariableInScopeLabel();

    @DefaultMessage("Warn if variable is defined but not used")
    @Key("diagnosticsWarnVariableDefinedButNotUsedLabel")
    String diagnosticsWarnVariableDefinedButNotUsedLabel();

    @DefaultMessage("Provide R style diagnostics (e.g. whitespace)")
    @Key("diagnosticsStyleDiagnosticsLabel")
    String diagnosticsStyleDiagnosticsLabel();

    @DefaultMessage("Prompt to install missing R packages discovered in R source files")
    @Key("diagnosticsAutoDiscoverPackageDependenciesLabel")
    String diagnosticsAutoDiscoverPackageDependenciesLabel();

    @DefaultMessage("Show diagnostics for C/C++")
    @Key("diagnosticsShowDiagnosticsCppLabel")
    String diagnosticsShowDiagnosticsCppLabel();

    @DefaultMessage("Show diagnostics for YAML")
    @Key("diagnosticsShowDiagnosticsYamlLabel")
    String diagnosticsShowDiagnosticsYamlLabel();

    @DefaultMessage("Show diagnostics for JavaScript, HTML, and CSS")
    @Key("diagnosticsShowDiagnosticsOtherLabel")
    String diagnosticsShowDiagnosticsOtherLabel();

    @DefaultMessage("Show diagnostics whenever source files are saved")
    @Key("diagnosticsOnSaveLabel")
    String diagnosticsOnSaveLabel();

    @DefaultMessage("Show diagnostics after keyboard is idle for a period of time")
    @Key("diagnosticsBackgroundDiagnosticsLabel")
    String diagnosticsBackgroundDiagnosticsLabel();

    @DefaultMessage("Keyboard idle time (ms):")
    @Key("diagnosticsBackgroundDiagnosticsDelayMsLabel")
    String diagnosticsBackgroundDiagnosticsDelayMsLabel();

    @DefaultMessage("Show full path to project in window title")
    @Key("fullProjectPathInWindowTitleLabel")
    String fullProjectPathInWindowTitleLabel();

    @DefaultMessage("Hide menu bar until Alt-key pressed")
    @Key("autohideMenubarLabel")
    String autohideMenubarLabel();
    

    @DefaultMessage("Text rendering:")
    @Key("textRenderingLabel")
    String textRenderingLabel();
    

    @DefaultMessage("Geometric Precision")
    @Key("geometricPrecision")
    String geometricPrecision();
    

    @DefaultMessage("Loading...")
    @Key("copilotLoadingMessage")
    String copilotLoadingMessage();
    
    

    @DefaultMessage("Generating diagnostic report...")
    @Key("copilotDiagnosticReportProgressLabel")
    String copilotDiagnosticReportProgressLabel();

    @DefaultMessage("You are currently signed in as: {0}")
    @Key("copilotSignedInAsLabel")
    String copilotSignedInAsLabel(String user);

    @DefaultMessage("Show Error...")
    @Key("copilotShowErrorLabel")
    String copilotShowErrorLabel();

    @DefaultMessage("Sign In")
    @Key("copilotSignInLabel")
    String copilotSignInLabel();

    @DefaultMessage("Sign Out")
    @Key("copilotSignOutLabel")
    String copilotSignOutLabel();

    @DefaultMessage("Activate")
    @Key("copilotActivateLabel")
    String copilotActivateLabel();

    @DefaultMessage("Refresh")
    @Key("copilotRefreshLabel")
    String copilotRefreshLabel();
    

    @DefaultMessage("Diagnostics")
    @Key("copilotDiagnosticsLabel")
    String copilotDiagnosticsLabel();
    

    @DefaultMessage("Project Options...")
    @Key("copilotProjectOptionsLabel")
    String copilotProjectOptionsLabel();

    @DefaultMessage("GitHub Copilot: Terms of Service")
    @Key("copilotTermsOfServiceLinkLabel")
    String copilotTermsOfServiceLinkLabel();

    @DefaultMessage("By using GitHub Copilot, you agree to abide by their terms of service.")
    @Key("copilotTermsOfServiceLabel")
    String copilotTermsOfServiceLabel();

    @DefaultMessage("GitHub Copilot")
    @Key("copilotDisplayName")
    String copilotDisplayName();

    @DefaultMessage("Copilot")
    @Key("copilotPaneName")
    String copilotPaneName();

    @DefaultMessage("Copilot Indexing")
    @Key("copilotIndexingHeader")
    String copilotIndexingHeader();

    @DefaultMessage("Copilot Completions")
    @Key("copilotCompletionsHeader")
    String copilotCompletionsHeader();

    @DefaultMessage("Show code suggestions after keyboard idle (ms):")
    @Key("copilotCompletionsDelayLabel")
    String copilotCompletionsDelayLabel();

    @DefaultMessage("GitHub Copilot integration has been disabled by the administrator.")
    @Key("copilotDisabledByAdmin")
    String copilotDisabledByAdmin();

    @DefaultMessage("GitHub Copilot: Status")
    @Key("copilotStatusDialogCaption")
    String copilotStatusDialogCaption();

    @DefaultMessage("An unexpected error occurred while checking the status of the GitHub Copilot agent.")
    @Key("copilotUnexpectedError")
    String copilotUnexpectedError();

    @DefaultMessage("An error occurred while starting the Copilot agent.")
    @Key("copilotStartupError")
    String copilotStartupError();

    @DefaultMessage("GitHub Copilot has been disabled in this project.")
    @Key("copilotDisabledInProject")
    String copilotDisabledInProject();

    @DefaultMessage("The GitHub Copilot agent is not currently running.")
    @Key("copilotAgentNotRunning")
    String copilotAgentNotRunning();

    @DefaultMessage("The GitHub Copilot agent has not been enabled.")
    @Key("copilotAgentNotEnabled")
    String copilotAgentNotEnabled();

    @DefaultMessage("You are currently signed in as {0}, but you haven''t yet activated GitHub Copilot.")
    @Key("copilotAccountNotActivated")
    String copilotAccountNotActivated(String name);

    @DefaultMessage("You are not currently signed in.")
    @Key("copilotNotSignedIn")
    String copilotNotSignedIn();

    @DefaultMessage("RStudio received a Copilot response that it does not understand.\n{0}")
    @Key("copilotUnknownResponse")
    String copilotUnknownResponse(String response);

    @DefaultMessage("Sign git commits")
    @Key("gitSignCommitLabel")
    String gitSignCommitLabel();
}

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

    @Key("verifyKey")
    String verifyKey();

    @Key("zoteroVerifyKeyFailedMessage")
    String zoteroVerifyKeyFailedMessage();

    @Key("zoteroWebApiKey")
    String zoteroWebApiKey();

    @Key("verifyingKey")
    String verifyingKey();

    @Key("zotero")
    String zotero();

    @Key("zoteroKeyVerified")
    String zoteroKeyVerified();

    @Key("useLibraries")
    String useLibraries();

    @Key("error")
    String error();

    @Key("selectOneZoteroLibrary")
    String selectOneZoteroLibrary();

    @Key("defaultInParentheses")
    String defaultInParentheses();

    @Key("myLibrary")
    String myLibrary();

    @Key("selectedLibraries")
    String selectedLibraries();

    @Key("condaEnvironment")
    String condaEnvironment();

    @Key("virtualEnvironment")
    String virtualEnvironment();

    @Key("pythonInterpreter")
    String pythonInterpreter();

    @Key("system")
    String system();

    @Key("virtualEnvironmentPlural")
    String virtualEnvironmentPlural();

    @Key("condaEnvironmentPlural")
    String condaEnvironmentPlural();

    @Key("pythonInterpreterPlural")
    String pythonInterpreterPlural();

    @Key("select")
    String select();

    @Key("noneAvailableParentheses")
    String noneAvailableParentheses();

    @Key("editorThemePreview")
    String editorThemePreview();

    @Key("spellingPrefsTitle")
    String spellingPrefsTitle();

    @Key("spellingPrefsDescription")
    String spellingPrefsDescription();

    @Key("rsaKeyFileTitle")
    String rsaKeyFileTitle();

    @Key("rsaKeyFileDescription")
    String rsaKeyFileDescription();

    @Key("haveRSAKeyTitle")
    String haveRSAKeyTitle();

    @Key("haveRSAKeyDescription")
    String haveRSAKeyDescription();

    @Key("errorChangingSettingCaption")
    String errorChangingSettingCaption();

    @Key("tabKeyErrorMessage")
    String tabKeyErrorMessage();

    @Key("tabKeyFocusOnMessage")
    String tabKeyFocusOnMessage();

    @Key("tabKeyFocusOffMessage")
    String tabKeyFocusOffMessage();

    @Key("toggleScreenReaderErrorMessage")
    String toggleScreenReaderErrorMessage();

    @Key("toggleScreenReaderConfirmCaption")
    String toggleScreenReaderConfirmCaption();

    @Key("toggleScreenReaderMessageConfirmDialog")
    String toggleScreenReaderMessageConfirmDialog(String value);

    @Key("disable")
    String disable();

    @Key("enable")
    String enable();

    @Key("announceScreenReaderStateMessage")
    String announceScreenReaderStateMessage(String shortcut);

    @Key("screenReaderStateEnabled")
    String screenReaderStateEnabled(String screenReaderLabel);

    @Key("screenReaderStateDisabled")
    String screenReaderStateDisabled(String screenReaderLabel);

    @Key("onClearUserPrefsYesLabel")
    String onClearUserPrefsYesLabel();

    @Key("cancel")
    String cancel();

    @Key("onClearUserPrefsRestartR")
    String onClearUserPrefsRestartR();

    @Key("onClearUserPrefsResponseCaption")
    String onClearUserPrefsResponseCaption();

    /**
     * Translated "Your preferences have been cleared, and your R session will now be restarted.
     * A backup copy of your preferences can be found at: \n\n{0}".
     *
     * @return translated "Your preferences have been cleared, and your R session will now be restarted.
     * A backup copy of your preferences can be found at: \n\n{0}"
     */
            "A backup copy of your preferences can be found at: \n\n{0}")
    @Key("onClearUserPrefsResponseMessage")
    String onClearUserPrefsResponseMessage(String path);

    @Key("onClearUserPrefsCaption")
    String onClearUserPrefsCaption();

    /**
     * Translated "Are you sure you want to clear your preferences?
     * All RStudio settings will be restored to their defaults, and your R session will be restarted.".
     *
     * @return translated "Are you sure you want to clear your preferences?
     * All RStudio settings will be restored to their defaults, and your R session will be restarted."
     */
            "All RStudio settings will be restored to their defaults, and your R session will be restarted.")
    @Key("onClearUserPrefsMessage")
    String onClearUserPrefsMessage();

    @Key("usingZotero")
    String usingZotero();

    @Key("zoteroLibrary")
    String zoteroLibrary();

    @Key("web")
    String web();

    @Key("local")
    String local();

    @Key("noneParentheses")
    String noneParentheses();

    @Key("general")
    String general();

    @Key("lineEndingConversion")
    String lineEndingConversion();

    @Key("useDefaultParentheses")
    String useDefaultParentheses();

    @Key("none")
    String none();

    @Key("platformNative")
    String platformNative();

    @Key("posixLF")
    String posixLF();

    @Key("windowsCRLF")
    String windowsCRLF();

    @Key("options")
    String options();

    @Key("generalHeaderPanel")
    String generalHeaderPanel();

    @Key("chkScreenReaderLabel")
    String chkScreenReaderLabel();

    @Key("typingStatusDelayLabel")
    String typingStatusDelayLabel();

    @Key("maxOutputLabel")
    String maxOutputLabel();

    @Key("displayLabel")
    String displayLabel();

    @Key("reducedMotionLabel")
    String reducedMotionLabel();

    @Key("chkTabMovesFocusLabel")
    String chkTabMovesFocusLabel();

    @Key("generalPanelLabel")
    String generalPanelLabel();

    @Key("helpRStudioAccessibilityLinkLabel")
    String helpRStudioAccessibilityLinkLabel();

    @Key("announcementsLabel")
    String announcementsLabel();

    @Key("tabHeaderPanel")
    String tabHeaderPanel();

    @Key("generalPanelText")
    String generalPanelText();

    @Key("announcementsPanelText")
    String announcementsPanelText();

    @Key("appearanceRStudioThemeLabel")
    String appearanceRStudioThemeLabel();

    @Key("appearanceZoomLabelZoom")
    String appearanceZoomLabelZoom();

    @Key("fontFaceEditorFontLabel")
    String fontFaceEditorFontLabel();

    @Key("appearanceEditorFontLabel")
    String appearanceEditorFontLabel();

    @Key("appearanceEditorFontSizeLabel")
    String appearanceEditorFontSizeLabel();

    @Key("appearanceEditorLineHeightLabel")
    String appearanceEditorLineHeightLabel();
    

    @Key("appearanceEditorThemeLabel")
    String appearanceEditorThemeLabel();

    @Key("addThemeButtonLabel")
    String addThemeButtonLabel();

    @Key("addThemeButtonCaption")
    String addThemeButtonCaption();

    @Key("removeThemeButtonLabel")
    String removeThemeButtonLabel();

    @Key("addThemeUserActionLabel")
    String addThemeUserActionLabel();

    @Key("setThemeWarningMessage")
    String setThemeWarningMessage(String name, String currentTheme);

    @Key("themeWarningMessageDarkLabel")
    String themeWarningMessageDarkLabel();

    @Key("themeWarningMessageLightLabel")
    String themeWarningMessageLightLabel();

    @Key("showThemeExistsDialogLabel")
    String showThemeExistsDialogLabel(String inputFileName);

    @Key("globalDisplayThemeExistsCaption")
    String globalDisplayThemeExistsCaption();

    @Key("cantAddThemeMessage")
    String cantAddThemeMessage();

    @Key("cantAddThemeErrorCaption")
    String cantAddThemeErrorCaption();

    @Key("cantAddThemeGlobalMessage")
    String cantAddThemeGlobalMessage();

    @Key("showCantRemoveThemeDialogMessage")
    String showCantRemoveThemeDialogMessage(String themeName, String errorMessage);

    @Key("showCantRemoveErrorMessage")
    String showCantRemoveErrorMessage();

    @Key("showCantRemoveActiveThemeDialog")
    String showCantRemoveActiveThemeDialog(String themeName);

    @Key("showCantRemoveThemeCaption")
    String showCantRemoveThemeCaption();

    @Key("showRemoveThemeWarningMessage")
    String showRemoveThemeWarningMessage(String themeName);

    @Key("showRemoveThemeGlobalMessage")
    String showRemoveThemeGlobalMessage();

    @Key("showDuplicateThemeErrorMessage")
    String showDuplicateThemeErrorMessage(String themeName);

    @Key("showDuplicateThemeDuplicateGlobalMessage")
    String showDuplicateThemeDuplicateGlobalMessage();

    @Key("showDuplicateThemeWarningMessage")
    String showDuplicateThemeWarningMessage(String themeName);

    @Key("showDuplicateThemeGlobalMessage")
    String showDuplicateThemeGlobalMessage();

    @Key("appearanceLabel")
    String appearanceLabel();

    @Key("editorFontLabel")
    String editorFontLabel();

    @Key("headerPDFGenerationLabel")
    String headerPDFGenerationLabel();

    @Key("perProjectNoteLabel")
    String perProjectNoteLabel();

    @Key("perProjectHeaderLabel")
    String perProjectHeaderLabel();

    @Key("chkUseTinytexLabel")
    String chkUseTinytexLabel();

    @Key("chkCleanTexi2DviOutputLabel")
    String chkCleanTexi2DviOutputLabel();

    @Key("chkEnableShellEscapeLabel")
    String chkEnableShellEscapeLabel();

    @Key("insertNumberedLatexSectionsLabel")
    String insertNumberedLatexSectionsLabel();

    @Key("previewingOptionsHeaderLabel")
    String previewingOptionsHeaderLabel();

    @Key("alwaysEnableRnwConcordanceLabel")
    String alwaysEnableRnwConcordanceLabel();

    @Key("pdfPreviewSelectWidgetLabel")
    String pdfPreviewSelectWidgetLabel();

    @Key("pdfPreviewHelpButtonTitle")
    String pdfPreviewHelpButtonTitle();

    @Key("preferencesPaneTitle")
    String preferencesPaneTitle();

    @Key("pdfNoPreviewOption")
    String pdfNoPreviewOption();

    @Key("pdfPreviewSumatraOption")
    String pdfPreviewSumatraOption();

    @Key("pdfPreviewRStudioViewerOption")
    String pdfPreviewRStudioViewerOption();

    @Key("pdfPreviewSystemViewerOption")
    String pdfPreviewSystemViewerOption();

    @Key("consoleExecutionLabel")
    String consoleExecutionLabel();
    

    @Key("consoleDiscardPendingConsoleInputOnErrorLabel")
    String consoleDiscardPendingConsoleInputOnErrorLabel();
    
    

    @Key("consoleDisplayLabel")
    String consoleDisplayLabel();
    

    @Key("consoleHighlightLabel")
    String consoleHighlightLabel();

    @Key("consoleSyntaxHighlightingLabel")
    String consoleSyntaxHighlightingLabel();

    @Key("consoleDifferentColorLabel")
    String consoleDifferentColorLabel();

    @Key("consoleLimitVariableLabel")
    String consoleLimitVariableLabel();

    @Key("consoleLimitOutputLengthLabel")
    String consoleLimitOutputLengthLabel();

    @Key("consoleMaxLinesLabel")
    String consoleMaxLinesLabel();

    @Key("consoleANSIEscapeCodesLabel")
    String consoleANSIEscapeCodesLabel();

    @Key("consoleColorModeANSIOption")
    String consoleColorModeANSIOption();

    @Key("consoleColorModeRemoveANSIOption")
    String consoleColorModeRemoveANSIOption();

    @Key("consoleColorModeIgnoreANSIOption")
    String consoleColorModeIgnoreANSIOption();

    @Key("consoleLabel")
    String consoleLabel();

    @Key("debuggingHeaderLabel")
    String debuggingHeaderLabel();

    @Key("debuggingExpandTracebacksLabel")
    String debuggingExpandTracebacksLabel();

    @Key("otherHeaderCaption")
    String otherHeaderCaption();

    @Key("otherDoubleClickLabel")
    String otherDoubleClickLabel();

    @Key("WarnAutomaticSuspensionPaused")
    String warnAutoSuspendPausedLabel();

    @Key("numberOfSecondsToDelayWarning")
    String numSecondsToDelayWarningLabel();

    @Key("rSessionsTitle")
    String rSessionsTitle();

    @Key("rVersionTitle")
    String rVersionTitle();

    @Key("rVersionChangeTitle")
    String rVersionChangeTitle();

    @Key("rChangeVersionMessage")
    String rChangeVersionMessage();

    @Key("rQuitReOpenMessage")
    String rQuitReOpenMessage();

    @Key("rVersionLoadingText")
    String rVersionLoadingText();

    @Key("rRestoreLabel")
    String rRestoreLabel();

    @Key("rDefaultDirectoryTitle")
    String rDefaultDirectoryTitle();

    @Key("rRestorePreviousTitle")
    String rRestorePreviousTitle();

    @Key("rRestorePreviousOpenTitle")
    String rRestorePreviousOpenTitle();

    @Key("rRunProfileTitle")
    String rRunProfileTitle();

    @Key("workspaceCaption")
    String workspaceCaption();

    @Key("workspaceLabel")
    String workspaceLabel();

    @Key("saveWorkSpaceLabel")
    String saveWorkSpaceLabel();

    @Key("saveWorkAlways")
    String saveWorkAlways();

    @Key("saveWorkNever")
    String saveWorkNever();

    @Key("saveWorkAsk")
    String saveWorkAsk();

    @Key("historyCaption")
    String historyCaption();

    @Key("alwaysSaveHistoryLabel")
    String alwaysSaveHistoryLabel();

    @Key("removeDuplicatesLabel")
    String removeDuplicatesLabel();

    @Key("otherCaption")
    String otherCaption();

    @Key("otherWrapAroundLabel")
    String otherWrapAroundLabel();

    @Key("otherNotifyMeLabel")
    String otherNotifyMeLabel();

    @Key("otherSendReportsLabel")
    String otherSendReportsLabel();

    @Key("graphicsDeviceCaption")
    String graphicsDeviceCaption();

    @Key("graphicsAntialiasingLabel")
    String graphicsAntialiasingLabel();

    @Key("antialiasingDefaultOption")
    String antialiasingDefaultOption();

    @Key("antialiasingNoneOption")
    String antialiasingNoneOption();

    @Key("antialiasingGrayOption")
    String antialiasingGrayOption();

    @Key("antialiasingSubpixelOption")
    String antialiasingSubpixelOption();

    @Key("serverHomePageLabel")
    String serverHomePageLabel();

    @Key("serverHomePageActiveSessionsOption")
    String serverHomePageActiveSessionsOption();

    @Key("serverHomePageAlwaysOption")
    String serverHomePageAlwaysOption();

    @Key("serverHomePageNeverOption")
    String serverHomePageNeverOption();

    @Key("reUseIdleSessionLabel")
    String reUseIdleSessionLabel();

    @Key("desktopCaption")
    String desktopCaption();

    @Key("advancedDebuggingCaption")
    String advancedDebuggingCaption();

    @Key("advancedDebuggingLabel")
    String advancedDebuggingLabel();

    @Key("advancedOsIntegrationCaption")
    String advancedOsIntegrationCaption();

    @Key("advancedRenderingEngineLabel")
    String advancedRenderingEngineLabel();

    @Key("renderingEngineAutoDetectOption")
    String renderingEngineAutoDetectOption();

    @Key("renderingEngineDesktopOption")
    String renderingEngineDesktopOption();

    @Key("renderingEngineLinuxDesktopOption")
    String renderingEngineLinuxDesktopOption();

    @Key("renderingEngineSoftwareOption")
    String renderingEngineSoftwareOption();

    @Key("useGpuExclusionListLabel")
    String useGpuExclusionListLabel();

    @Key("useGpuDriverBugWorkaroundsLabel")
    String useGpuDriverBugWorkaroundsLabel();

    @Key("clipboardMonitoringLabel")
    String clipboardMonitoringLabel();

    @Key("otherLabel")
    String otherLabel();

    @Key("experimentalLabel")
    String experimentalLabel();

    @Key("englishLabel")
    String englishLabel();

    @Key("frenchLabel")
    String frenchLabel();

    @Key("otherShowLastDotValueLabel")
    String otherShowLastDotValueLabel();

    @Key("helpFontSizeLabel")
    String helpFontSizeLabel();

    /**
     * Translated "General".
     *
     * @return "General"
     */
    @Key("generalTabListLabel")
    String generalTablistLabel();

    /**
     * Translated "Basic".
     *
     * @return "Basic"
     */
    @Key("generalTabListBasicOption")
    String generalTablListBasicOption();

    /**
     * Translated "Graphics".
     *
     * @return "Graphics"
     */
    @Key("generalTabListGraphicsOption")
    String generalTablListGraphicsOption();

    /**
     * Translated "Advanced".
     *
     * @return "Advanced"
     */
    @Key("generalTabListAdvancedOption")
    String generalTabListAdvancedOption();

    /**
     * Translated " (Default)".
     *
     * @return " (Default)"
     */
    @Key("graphicsBackEndDefaultOption")
    String graphicsBackEndDefaultOption();

    /**
     * Translated "Quartz".
     *
     * @return "Quartz"
     */
    @Key("graphicsBackEndQuartzOption")
    String graphicsBackEndQuartzOption();

    /**
     * Translated "Windows".
     *
     * @return "Windows"
     */
    @Key("graphicsBackEndWindowsOption")
    String graphicsBackEndWindowsOption();

    /**
     * Translated "Cairo".
     *
     * @return "Cairo"
     */
    @Key("graphicsBackEndCairoOption")
    String graphicsBackEndCairoOption();

    /**
     * Translated "Cairo PNG".
     *
     * @return "Cairo PNG"
     */
    @Key("graphicsBackEndCairoPNGOption")
    String graphicsBackEndCairoPNGOption();

    /**
     * Translated "AGG".
     *
     * @return "AGG"
     */
    @Key("graphicsBackEndAGGOption")
    String graphicsBackEndAGGOption();

    /**
     * Translated "Backend:".
     *
     * @return "Backend:"
     */
    @Key("graphicsBackendLabel")
    String graphicsBackendLabel();

    /**
     * Translated "Using the AGG renderer".
     *
     * @return "Using the AGG renderer"
     */
    @Key("graphicsBackendUserAction")
    String graphicsBackendUserAction();

    /**
     * Translated "Browse...".
     *
     * @return "Browse..."
     */
    @Key("browseLabel")
    String browseLabel();

    /**
     * Translated "Choose Directory".
     *
     * @return "Choose Directory"
     */
    @Key("directoryLabel")
    String directoryLabel();

    @Key("codePaneLabel")
    String codePaneLabel();

    @Key("packageManagementTitle")
    String packageManagementTitle();

    @Key("packagesInfoBarText")
    String packagesInfoBarText();
    

    @Key("packagesRenvInfoBarText")
    String packagesRenvInfoBarText();

    @Key("cranMirrorTextBoxTitle")
    String cranMirrorTextBoxTitle();

    @Key("cranMirrorChangeLabel")
    String cranMirrorChangeLabel();

    @Key("secondaryReposTitle")
    String secondaryReposTitle();

    @Key("chkEnablePackagesTitle")
    String chkEnablePackagesTitle();

    @Key("useSecurePackageDownloadTitle")
    String useSecurePackageDownloadTitle();

    @Key("useSecurePackageTitle")
    String useSecurePackageTitle();

    @Key("useInternetTitle")
    String useInternetTitle();

    @Key("managePackagesTitle")
    String managePackagesTitle();

    @Key("developmentTitle")
    String developmentTitle();

    @Key("cppDevelopmentTitle")
    String cppDevelopmentTitle();

    @Key("useDevtoolsLabel")
    String useDevtoolsLabel();

    @Key("developmentSaveLabel")
    String developmentSaveLabel();

    @Key("developmentNavigateLabel")
    String developmentNavigateLabel();

    @Key("developmentHideLabel")
    String developmentHideLabel();

    @Key("developmentCleanupLabel")
    String developmentCleanupLabel();

    @Key("developmentViewLabel")
    String developmentViewLabel();

    @Key("developmentCppTemplate")
    String developmentCppTemplate();

    @Key("developmentEmptyLabel")
    String developmentEmptyLabel();

    @Key("developmentUseLFLabel")
    String developmentUseLFLabel();

    @Key("tabPackagesPanelTitle")
    String tabPackagesPanelTitle();

    @Key("managementPanelTitle")
    String managementPanelTitle();

    @Key("developmentManagementPanelTitle")
    String developmentManagementPanelTitle();

    @Key("C / C++")
    String cppPanelTitle();

    @Key("chooseMirrorDialogMessage")
    String chooseMirrorDialogMessage();

    @Key("showDisconnectErrorCaption")
    String showDisconnectErrorCaption();

    @Key("showDisconnectErrorMessage")
    String showDisconnectErrorMessage();

    @Key("progressIndicatorMessage")
    String progressIndicatorMessage();

    @Key("progressIndicatorError")
    String progressIndicatorError();

    @Key("customLabel")
    String customLabel();

    @Key("mirrorsLabel")
    String mirrorsLabel();

    @Key("headerLabel")
    String headerLabel();

    @Key("buttonAddLabel")
    String buttonAddLabel();

    @Key("buttonRemoveLabel")
    String buttonRemoveLabel();

    @Key("buttonUpLabel")
    String buttonUpLabel();

    @Key("buttonDownLabel")
    String buttonDownLabel();

    @Key("developingPkgHelpLink")
    String developingPkgHelpLink();

    @Key("secondaryReposDialog")
    String secondaryReposDialog();

    @Key("validateSyncLabel")
    String validateSyncLabel();

    @Key("showErrorRepoMessage")
    String showErrorRepoMessage();

    @Key("alreadyIncludedMessage")
    String alreadyIncludedMessage();

    @Key("validateAsyncProgress")
    String validateAsyncProgress();

    @Key("onResponseReceived")
    String onResponseReceived();

    @Key("nameLabel")
    String nameLabel();

    @Key("urlLabel")
    String urlLabel();

    @Key("reposLabel")
    String reposLabel();

    @Key("secondaryRepoLabel")
    String secondaryRepoLabel();

    @Key("paneLayoutText")
    String paneLayoutText();

    @Key("columnToolbarLabel")
    String columnToolbarLabel();

    @Key("addButtonText")
    String addButtonText();

    @Key("addButtonLabel")
    String addButtonLabel();

    @Key("removeButtonText")
    String removeButtonText();

    @Key("removeButtonLabel")
    String removeButtonLabel();

    @Key("createGridLabel")
    String createGridLabel();

    @Key("createColumnLabel")
    String createColumnLabel();

    @Key("paneLayoutLabel")
    String paneLayoutLabel();

    @Key("accountListLabel")
    String accountListLabel();

    @Key("connectButtonLabel")
    String connectButtonLabel();

    @Key("reconnectButtonLabel")
    String reconnectButtonLabel();

    @Key("disconnectButtonLabel")
    String disconnectButtonLabel();

    @Key("missingPkgPanelMessage")
    String missingPkgPanelMessage();

    @Key("installPkgsMessage")
    String installPkgsMessage();

    @Key("withRSConnectLabel")
    String withRSConnectLabel();

    @Key("chkEnableRSConnectLabel")
    String chkEnableRSConnectLabel();

    @Key("checkBoxWithHelpTitle")
    String checkBoxWithHelpTitle();

    @Key("settingsHeaderLabel")
    String settingsHeaderLabel();

    @Key("chkEnablePublishingLabel")
    String chkEnablePublishingLabel();

    @Key("showPublishDiagnosticsLabel")
    String showPublishDiagnosticsLabel();

    @Key("sSLCertificatesHeaderLabel")
    String sSLCertificatesHeaderLabel();

    @Key("publishCheckCertificatesLabel")
    String publishCheckCertificatesLabel();

    @Key("usePublishCaBundleLabel")
    String usePublishCaBundleLabel();

    @Key("caBundlePath")
    String caBundlePath();

    @Key("helpLinkTroubleshooting")
    String helpLinkTroubleshooting();

    @Key("publishingPaneHeader")
    String publishingPaneHeader();

    @Key("showErrorCaption")
    String showErrorCaption();

    @Key("showErrorMessage")
    String showErrorMessage();

    @Key("removeAccountGlobalDisplay")
    String removeAccountGlobalDisplay();

    @Key("removeAccountMessage")
    String removeAccountMessage(String name, String server);

    @Key("onConfirmDisconnectYesLabel")
    String onConfirmDisconnectYesLabel();

    @Key("onConfirmDisconnectNoLabel")
    String onConfirmDisconnectNoLabel();

    @Key("disconnectingErrorMessage")
    String disconnectingErrorMessage();

    @Key("getAccountCountLabel")
    String getAccountCountLabel();

    @Key("pythonPreferencesText")
    String pythonPreferencesText();

    @Key("overrideText")
    String overrideText();

    @Key("headerPythonLabel")
    String headerPythonLabel();

    @Key("mismatchWarningBarText")
    String mismatchWarningBarText();

    @Key("progressIndicatorText")
    String progressIndicatorText();

    @Key("tbPythonInterpreterText")
    String tbPythonInterpreterText();

    @Key("tbPythonActionText")
    String tbPythonActionText();

    @Key("onDependencyErrorMessage")
    String onDependencyErrorMessage();

    @Key("invalidReasonLabel")
    String invalidReasonLabel();

    @Key("helpRnwButtonLabel")
    String helpRnwButtonLabel();

    @Key("cbAutoUseProjectInterpreter")
    String cbAutoUseProjectInterpreter();

    @Key("cbAutoUseProjectInterpreterMessage")
    String cbAutoUseProjectInterpreterMessage();

    @Key("tabPanelCaption")
    String tabPanelCaption();

    @Key("clearLabel")
    String clearLabel();

    @Key("systemTab")
    String systemTab();

    @Key("virtualEnvTab")
    String virtualEnvTab();

    @Key("condaEnvTab")
    String condaEnvTab();

    @Key("unknownType")
    String unknownType();

    @Key("virtualEnvironmentType")
    String virtualEnvironmentType();

    @Key("condaEnvironmentType")
    String condaEnvironmentType();

    @Key("systemInterpreterType")
    String systemInterpreterType();

    @Key("quartoPreviewLabel")
    String quartoPreviewLabel();

    /**
     * Get locale value of the label for the checkbox to enable the
     * Quarto preview. Default value is "Enable Quarto preview".
     *
     * @return the translated value for the label
     */
    @Key("enableQuartoPreviewCheckboxLabel")
    String enableQuartoPreviewCheckboxLabel();

    /**
     * Get locale value for the help link caption. Default value
     * is "Learn more about Quarto".
     *
     * @return the translated value for the help link
     */
    @Key("helpLinkCaption")
    String helpLinkCaption();

    @Key("rMarkdownHeaderLabel")
    String rMarkdownHeaderLabel();

    @Key("documentOutlineHeaderLabel")
    String documentOutlineHeaderLabel();

    @Key("rMarkdownShowLabel")
    String rMarkdownShowLabel();

    @Key("rMarkdownSoftWrapLabel")
    String rMarkdownSoftWrapLabel();

    @Key("docOutlineDisplayLabel")
    String docOutlineDisplayLabel();

    @Key("docOutlineSectionsOption")
    String docOutlineSectionsOption();

    @Key("docOutlineSectionsNamedChunksOption")
    String docOutlineSectionsNamedChunksOption();

    @Key("docOutlineSectionsAllChunksOption")
    String docOutlineSectionsAllChunksOption();

    @Key("rmdViewerModeLabel")
    String rmdViewerModeLabel();

    @Key("rmdViewerModeWindowOption")
    String rmdViewerModeWindowOption();

    @Key("rmdViewerModeViewerPaneOption")
    String rmdViewerModeViewerPaneOption();

    @Key("rmdViewerModeNoneOption")
    String rmdViewerModeNoneOption();

    @Key("rmdInlineOutputLabel")
    String rmdInlineOutputLabel();

    @Key("latexPreviewWidgetLabel")
    String latexPreviewWidgetLabel();

    @Key("latexPreviewWidgetNeverOption")
    String latexPreviewWidgetNeverOption();

    @Key("latexPreviewWidgetPopupOption")
    String latexPreviewWidgetPopupOption();

    @Key("latexPreviewWidgetInlineOption")
    String latexPreviewWidgetInlineOption();

    @Key("knitWorkingDirLabel")
    String knitWorkingDirLabel();

    @Key("knitWorkingDirDocumentOption")
    String knitWorkingDirDocumentOption();

    @Key("knitWorkingDirCurrentOption")
    String knitWorkingDirCurrentOption();

    @Key("knitWorkingDirProjectOption")
    String knitWorkingDirProjectOption();

    @Key("rNotebooksCaption")
    String rNotebooksCaption();

    @Key("autoExecuteSetupChunkLabel")
    String autoExecuteSetupChunkLabel();

    @Key("notebookHideConsoleLabel")
    String notebookHideConsoleLabel();

    @Key("helpRStudioLinkLabel")
    String helpRStudioLinkLabel();

    @Key("advancedHeaderLabel")
    String advancedHeaderLabel();

    @Key("advancedEnableChunkLabel")
    String advancedEnableChunkLabel();

    @Key("advancedShowInlineLabel")
    String advancedShowInlineLabel();

    @Key("advancedDisplayRender")
    String advancedDisplayRender();

    @Key("visualModeGeneralCaption")
    String visualModeGeneralCaption();

    @Key("visualModeUseVisualEditorLabel")
    String visualModeUseVisualEditorLabel();

    @Key("visualModeHelpLink")
    String visualModeHelpLink();

    @Key("visualModeHeaderLabel")
    String visualModeHeaderLabel();

    @Key("visualEditorShowOutlineLabel")
    String visualEditorShowOutlineLabel();

    @Key("visualEditorShowMarginLabel")
    String visualEditorShowMarginLabel();

    @Key("visualModeContentWidthLabel")
    String visualModeContentWidthLabel();

    @Key("visualModeFontSizeLabel")
    String visualModeFontSizeLabel();

    @Key("visualModeOptionsMarkdownCaption")
    String visualModeOptionsMarkdownCaption();

    @Key("visualModeListSpacingLabel")
    String visualModeListSpacingLabel();

    @Key("visualModeWrapLabel")
    String visualModeWrapLabel();

    @Key("visualModeWrapHelpLabel")
    String visualModeWrapHelpLabel();

    @Key("visualModeOptionsLabel")
    String visualModeOptionsLabel();

    @Key("visualModeReferencesLabel")
    String visualModeReferencesLabel();

    @Key("visualModeCanonicalLabel")
    String visualModeCanonicalLabel();

    @Key("visualModeCanonicalMessageCaption")
    String visualModeCanonicalMessageCaption();

    @Key("visualModeCanonicalPreferenceMessage")
    String visualModeCanonicalPreferenceMessage();

    @Key("markdownPerFileOptionsHelpLink")
    String markdownPerFileOptionsHelpLink();

    @Key("citationsLabel")
    String citationsLabel();

    @Key("citationsHelpLink")
    String citationsHelpLink();

    @Key("zoteroHeaderLabel")
    String zoteroHeaderLabel();

    @Key("zoteroDataDirLabel")
    String zoteroDataDirLabel();

    @Key("zoteroDataDirNotDectedLabel")
    String zoteroDataDirNotDectedLabel();

    @Key("zoteroUseBetterBibtexLabel")
    String zoteroUseBetterBibtexLabel();

    @Key("tabPanelTitle")
    String tabPanelTitle();

    @Key("tabPanelBasic")
    String tabPanelBasic();

    @Key("tabPanelAdvanced")
    String tabPanelAdvanced();

    @Key("tabPanelVisual")
    String tabPanelVisual();

    @Key("tabPanelCitations")
    String tabPanelCitations();

    @Key("webOption")
    String webOption();

    @Key("showLinkNumbersLabel")
    String showLinkNumbersLabel();

    @Key("chkVcsEnabledLabel")
    String chkVcsEnabledLabel();

    @Key("globalDisplayEnable")
    String globalDisplayEnable();

    @Key("globalDisplayDisable")
    String globalDisplayDisable();

    @Key("globalDisplayVC")
    String globalDisplayVC(String displayEnable);

    @Key("globalDisplayVCMessage")
    String globalDisplayVCMessage();

    @Key("gitExePathMessage")
    String gitExePathMessage(String gitPath);

    @Key("gitGlobalDisplay")
    String gitGlobalDisplay();

    @Key("gitExePathLabel")
    String gitExePathLabel();

    @Key("gitExePathNotFoundLabel")
    String gitExePathNotFoundLabel();

    @Key("svnExePathLabel")
    String svnExePathLabel();

    @Key("terminalPathLabel")
    String terminalPathLabel();

    @Key("gitSVNPaneHeader")
    String gitSVNPaneHeader();

    @Key("spellingPreferencesPaneHeader")
    String spellingPreferencesPaneHeader();

    @Key("ignoreHeader")
    String ignoreHeader();

    @Key("ignoreWordsUppercaseLabel")
    String ignoreWordsUppercaseLabel();

    @Key("ignoreWordsNumbersLabel")
    String ignoreWordsNumbersLabel();

    @Key("checkingHeader")
    String checkingHeader();

    @Key("realTimeSpellcheckingCheckboxLabel")
    String realTimeSpellcheckingCheckboxLabel();

    @Key("kUserDictionaryLabel")
    String kUserDictionaryLabel();

    @Key("kUserDictionaryWordsLabel")
    String kUserDictionaryWordsLabel(String kUserDictionary, String entries);

    @Key("editUserDictLabel")
    String editUserDictLabel();

    @Key("editUserDictCaption")
    String editUserDictCaption();

    @Key("editUserDictSaveCaption")
    String editUserDictSaveCaption();

    @Key("spellingPaneLabel")
    String spellingPaneLabel();

    @Key("editDialog")
    String editDialog();

    @Key("saveDialog")
    String saveDialog();

    @Key("cancelButton")
    String cancelButton();

    @Key("shellHeaderLabel")
    String shellHeaderLabel();

    @Key("initialDirectoryLabel")
    String initialDirectoryLabel();

    @Key("projectDirectoryOption")
    String projectDirectoryOption();

    @Key("currentDirectoryOption")
    String currentDirectoryOption();

    @Key("homeDirectoryOption")
    String homeDirectoryOption();

    @Key("terminalShellLabel")
    String terminalShellLabel();

    @Key("shellExePathMessage")
    String shellExePathMessage(String shellExePath);

    @Key("shellExeCaption")
    String shellExeCaption();

    @Key("customShellPathLabel")
    String customShellPathLabel();

    @Key("customShellChooserEmptyLabel")
    String customShellChooserEmptyLabel();

    @Key("customShellOptionsLabel")
    String customShellOptionsLabel();

    @Key("perfLabel")
    String perfLabel();

    @Key("chkTerminalLocalEchoLabel")
    String chkTerminalLocalEchoLabel();

    @Key("chkTerminalLocalEchoTitle")
    String chkTerminalLocalEchoTitle();

    @Key("chkTerminalWebsocketLabel")
    String chkTerminalWebsocketLabel();

    @Key("chkTerminalWebsocketTitle")
    String chkTerminalWebsocketTitle();

    @Key("displayHeaderLabel")
    String displayHeaderLabel();

    @Key("chkHardwareAccelerationLabel")
    String chkHardwareAccelerationLabel();

    @Key("chkAudibleBellLabel")
    String chkAudibleBellLabel();

    @Key("chkWebLinksLabel")
    String chkWebLinksLabel();

    @Key("helpLinkLabel")
    String helpLinkLabel();

    @Key("miscLabel")
    String miscLabel();

    @Key("autoClosePrefLabel")
    String autoClosePrefLabel();

    @Key("closePaneOption")
    String closePaneOption();

    @Key("doNotClosePaneOption")
    String doNotClosePaneOption();

    @Key("shellExitsPaneOption")
    String shellExitsPaneOption();

    @Key("chkCaptureEnvLabel")
    String chkCaptureEnvLabel();

    @Key("chkCaptureEnvTitle")
    String chkCaptureEnvTitle();

    @Key("shutdownLabel")
    String shutdownLabel();

    @Key("busyModeLabel")
    String busyModeLabel();

    @Key("busyWhitelistLabel")
    String busyWhitelistLabel();

    @Key("terminalPaneLabel")
    String terminalPaneLabel();

    @Key("tabGeneralPanelLabel")
    String tabGeneralPanelLabel();

    @Key("tabClosingPanelLabel")
    String tabClosingPanelLabel();

    @Key("busyModeAlwaysOption")
    String busyModeAlwaysOption();

    @Key("busyModeNeverOption")
    String busyModeNeverOption();

    @Key("busyModeListOption")
    String busyModeListOption();

    @Key("chkPythonIntegration")
    String chkPythonIntegration();

    @Key("chkPythonIntegrationTitle")
    String chkPythonIntegrationTitle();

    @Key("historyTab")
    String historyTab();

    @Key("filesTab")
    String filesTab();

    @Key("plotsTab")
    String plotsTab();

    @Key("connectionsTab")
    String connectionsTab();

    @Key("packagesTab")
    String packagesTab();

    @Key("helpTab")
    String helpTab();

    @Key("buildTab")
    String buildTab();

    @Key("vcsTab")
    String vcsTab();

    @Key("tutorialTab")
    String tutorialTab();

    @Key("viewerTab")
    String viewerTab();

    @Key("presentationTab")
    String presentationTab();

    @Key("confirmRemoveCaption")
    String confirmRemoveCaption();

    @Key("confirmRemoveMessage")
    String confirmRemoveMessage(String repo);

    @Key("modernThemeLabel")
    String modernThemeLabel();

    @Key("skyThemeLabel")
    String skyThemeLabel();

    @Key("generalHeaderLabel")
    String generalHeaderLabel();
    

    @Key("codeFormattingHeaderLabel")
    String codeFormattingHeaderLabel();
 

    @Key("useFormatterLabel")
    String useFormatterLabel();
    

    @Key("syntaxHeaderLabel")
    String syntaxHeaderLabel();

    @Key("editSnippetsButtonLabel")
    String editSnippetsButtonLabel();

    @Key("listSpacingTight")
    String listSpacingTight();

    @Key("listSpacingSpaced")
    String listSpacingSpaced();

    @Key("editingWrapNone")
    String editingWrapNone();

    @Key("editingWrapColumn")
    String editingWrapColumn();

    @Key("editingWrapSentence")
    String editingWrapSentence();

    @Key("refLocationBlock")
    String refLocationBlock();

    @Key("refLocationSection")
    String refLocationSection();

    @Key("refLocationDocument")
    String refLocationDocument();

    @Key("editingDiagOtherLabel")
    String editingDiagOtherLabel();

    @Key("editingDiagShowLabel")
    String editingDiagShowLabel();

    @Key("editingDiagnosticsPanel")
    String editingDiagnosticsPanel();

    @Key("editingDisplayPanel")
    String editingDisplayPanel();

    @Key("editingEditShortcuts")
    String editingEditShortcuts();

    @Key("editingExecutionLabel")
    String editingExecutionLabel();

    @Key("editingHeaderLabel")
    String editingHeaderLabel();

    @Key("editingOtherLabel")
    String editingOtherLabel();

    @Key("editingOtherTip")
    String editingOtherTip();

    @Key("editingSavePanel")
    String editingSavePanel();

    @Key("editingSavePanelAction")
    String editingSavePanelAction();

    @Key("editingSavePanelAutosave")
    String editingSavePanelAutosave();

    @Key("editingSerializationLabel")
    String editingSerializationLabel();

    @Key("editingSnippetHelpTitle")
    String editingSnippetHelpTitle();

    @Key("editingSnippetsLabel")
    String editingSnippetsLabel();

    @Key("editingTabPanel")
    String editingTabPanel();

    @Key("editingTabPanelCompletionPanel")
    String editingTabPanelCompletionPanel();

    @Key("editingTabPanelDiagnosticsPanel")
    String editingTabPanelDiagnosticsPanel();

    @Key("editingTabPanelDisplayPanel")
    String editingTabPanelDisplayPanel();

    @Key("editingTabPanelFormattingPanel")
    String editingTabPanelFormattingPanel();
    

    @Key("editingTabPanelSavePanel")
    String editingTabPanelSavePanel();

    @Key("editingCompletionPanel")
    String editingCompletionPanel();

    @Key("editingHeader")
    String editingHeader();

    @Key("editingKeyboardShortcuts")
    String editingKeyboardShortcuts();

    @Key("editingKeyboardText")
    String editingKeyboardText();

    @Key("editingRadioCustomized")
    String editingRadioCustomized();

    @Key("editingFilterWidget")
    String editingFilterWidget();

    @Key("editingResetText")
    String editingResetText();

    @Key("editingGlobalDisplay")
    String editingGlobalDisplay();

    @Key("editingGlobalCaption")
    String editingGlobalCaption();

    @Key("editingGlobalMessage")
    String editingGlobalMessage();

    @Key("editingProgressMessage")
    String editingProgressMessage();

    @Key("editingCancelShortcuts")
    String editingCancelShortcuts();

    @Key("editingTabWidthLabel")
    String editingTabWidthLabel();

    @Key("editorScrollMultiplier")
    String editorScrollMultiplier();

    @Key("editorScrollMultiplierDesc")
    String editorScrollMultiplierDesc();

    @Key("editingAutoDetectIndentationLabel")
    String editingAutoDetectIndentationLabel();

    @Key("editingAutoDetectIndentationDesc")
    String editingAutoDetectIndentationDesc();

    @Key("editingInsertMatchingLabel")
    String editingInsertMatchingLabel();

    @Key("editingUseNativePipeOperatorLabel")
    String editingUseNativePipeOperatorLabel();

    @Key("editingProjectOverrideInfoText")
    String editingProjectOverrideInfoText();

    @Key("editProjectPreferencesButtonLabel")
    String editProjectPreferencesButtonLabel();

    @Key("editingReindentOnPasteLabel")
    String editingReindentOnPasteLabel();

    @Key("editingVerticallyAlignArgumentsIndentLabel")
    String editingVerticallyAlignArgumentsIndentLabel();

    @Key("editingContinueCommentsOnNewlineLabel")
    String editingContinueCommentsOnNewlineLabel();

    @Key("editingContinueCommentsOnNewlineDesc")
    String editingContinueCommentsOnNewlineDesc();

    @Key("editingHighlightWebLinkLabel")
    String editingHighlightWebLinkLabel();

    @Key("editingHighlightWebLinkDesc")
    String editingHighlightWebLinkDesc();

    @Key("editingSurroundSelectionLabel")
    String editingSurroundSelectionLabel();

    @Key("editingKeybindingsLabel")
    String editingKeybindingsLabel();

    @Key("editingFocusConsoleAfterExecLabel")
    String editingFocusConsoleAfterExecLabel();

    @Key("editingExecutionBehaviorLabel")
    String editingExecutionBehaviorLabel();

    @Key("displayHighlightSelectedWordLabel")
    String displayHighlightSelectedWordLabel();

    @Key("displayHighlightSelectedLineLabel")
    String displayHighlightSelectedLineLabel();

    /**
      * Translated "Show line numbers"
      *
      * @return translated "Show line numbers"
      */
    @Key("displayShowLineNumbersLabel")
    String displayShowLineNumbersLabel();

    @Key("displayRelativeLineNumbersLabel")
    String displayRelativeLineNumbersLabel();

    @Key("displayShowMarginLabel")
    String displayShowMarginLabel();

    @Key("displayShowInvisiblesLabel")
    String displayShowInvisiblesLabel();

    @Key("displayShowIndentGuidesLabel")
    String displayShowIndentGuidesLabel();

    @Key("displayBlinkingCursorLabel")
    String displayBlinkingCursorLabel();

    @Key("displayScrollPastEndOfDocumentLabel")
    String displayScrollPastEndOfDocumentLabel();

    @Key("displayEnableTextDragLabel")
    String displayEnableTextDragLabel();

    @Key("displayFoldStyleLabel")
    String displayFoldStyleLabel();

    @Key("savingAutoAppendNewLineLabel")
    String savingAutoAppendNewLineLabel();

    @Key("savingStripTrailingWhitespaceLabel")
    String savingStripTrailingWhitespaceLabel();

    @Key("savingRestoreSourceDocumentCursorPositionLabel")
    String savingRestoreSourceDocumentCursorPositionLabel();

    @Key("savingDefaultEncodingLabel")
    String savingDefaultEncodingLabel();

    @Key("savingSaveBeforeSourcingLabel")
    String savingSaveBeforeSourcingLabel();

    @Key("savingAutoSaveOnBlurLabel")
    String savingAutoSaveOnBlurLabel();

    @Key("savingAutoSaveOnIdleLabel")
    String savingAutoSaveOnIdleLabel();

    @Key("savingAutoSaveIdleMsLabel")
    String savingAutoSaveIdleMsLabel();

    @Key("completionCodeCompletionLabel")
    String completionCodeCompletionLabel();

    @Key("completionCodeCompletionOtherLabel")
    String completionCodeCompletionOtherLabel();

    @Key("completionConsoleCodeCompletionLabel")
    String completionConsoleCodeCompletionLabel();

    @Key("completionInsertParensAfterFunctionCompletion")
    String completionInsertParensAfterFunctionCompletion();

    @Key("completionShowFunctionSignatureTooltipsLabel")
    String completionShowFunctionSignatureTooltipsLabel();

    @Key("completionShowHelpTooltipOnIdleLabel")
    String completionShowHelpTooltipOnIdleLabel();

    @Key("completionInsertSpacesAroundEqualsLabel")
    String completionInsertSpacesAroundEqualsLabel();

    @Key("completionTabCompletionLabel")
    String completionTabCompletionLabel();

    @Key("completionTabMultilineCompletionLabel")
    String completionTabMultilineCompletionLabel();

    @Key("completionCodeCompletionCharactersLabel")
    String completionCodeCompletionCharactersLabel();

    @Key("completionCodeCompletionDelayLabel")
    String completionCodeCompletionDelayLabel();

    @Key("diagnosticsShowDiagnosticsRLabel")
    String diagnosticsShowDiagnosticsRLabel();

    @Key("diagnosticsInRFunctionCallsLabel")
    String diagnosticsInRFunctionCallsLabel();

    @Key("diagnosticsCheckArgumentsToRFunctionCallsLabel")
    String diagnosticsCheckArgumentsToRFunctionCallsLabel();

    @Key("diagnosticsCheckUnexpectedAssignmentInFunctionCallLabel")
    String diagnosticsCheckUnexpectedAssignmentInFunctionCallLabel();

    @Key("diagnosticsWarnIfNoSuchVariableInScopeLabel")
    String diagnosticsWarnIfNoSuchVariableInScopeLabel();

    @Key("diagnosticsWarnVariableDefinedButNotUsedLabel")
    String diagnosticsWarnVariableDefinedButNotUsedLabel();

    @Key("diagnosticsStyleDiagnosticsLabel")
    String diagnosticsStyleDiagnosticsLabel();

    @Key("diagnosticsAutoDiscoverPackageDependenciesLabel")
    String diagnosticsAutoDiscoverPackageDependenciesLabel();

    @Key("diagnosticsShowDiagnosticsCppLabel")
    String diagnosticsShowDiagnosticsCppLabel();

    @Key("diagnosticsShowDiagnosticsYamlLabel")
    String diagnosticsShowDiagnosticsYamlLabel();

    @Key("diagnosticsShowDiagnosticsOtherLabel")
    String diagnosticsShowDiagnosticsOtherLabel();

    @Key("diagnosticsOnSaveLabel")
    String diagnosticsOnSaveLabel();

    @Key("diagnosticsBackgroundDiagnosticsLabel")
    String diagnosticsBackgroundDiagnosticsLabel();

    @Key("diagnosticsBackgroundDiagnosticsDelayMsLabel")
    String diagnosticsBackgroundDiagnosticsDelayMsLabel();

    @Key("fullProjectPathInWindowTitleLabel")
    String fullProjectPathInWindowTitleLabel();

    @Key("autohideMenubarLabel")
    String autohideMenubarLabel();
    

    @Key("textRenderingLabel")
    String textRenderingLabel();
    

    @Key("geometricPrecision")
    String geometricPrecision();
    

    @Key("copilotLoadingMessage")
    String copilotLoadingMessage();
    
    

    @Key("copilotDiagnosticReportProgressLabel")
    String copilotDiagnosticReportProgressLabel();

    @Key("copilotSignedInAsLabel")
    String copilotSignedInAsLabel(String user);

    @Key("copilotShowErrorLabel")
    String copilotShowErrorLabel();

    @Key("copilotSignInLabel")
    String copilotSignInLabel();

    @Key("copilotSignOutLabel")
    String copilotSignOutLabel();

    @Key("copilotActivateLabel")
    String copilotActivateLabel();

    @Key("copilotRefreshLabel")
    String copilotRefreshLabel();
    

    @Key("copilotDiagnosticsLabel")
    String copilotDiagnosticsLabel();
    

    @Key("copilotProjectOptionsLabel")
    String copilotProjectOptionsLabel();

    @Key("copilotTermsOfServiceLinkLabel")
    String copilotTermsOfServiceLinkLabel();

    @Key("copilotTermsOfServiceLabel")
    String copilotTermsOfServiceLabel();

    @Key("copilotDisplayName")
    String copilotDisplayName();

    @Key("copilotPaneName")
    String copilotPaneName();

    @Key("copilotIndexingHeader")
    String copilotIndexingHeader();

    @Key("copilotCompletionsHeader")
    String copilotCompletionsHeader();

    @Key("copilotCompletionsDelayLabel")
    String copilotCompletionsDelayLabel();

    @Key("copilotDisabledByAdmin")
    String copilotDisabledByAdmin();

    @Key("copilotStatusDialogCaption")
    String copilotStatusDialogCaption();

    @Key("copilotUnexpectedError")
    String copilotUnexpectedError();

    @Key("copilotStartupError")
    String copilotStartupError();

    @Key("copilotDisabledInProject")
    String copilotDisabledInProject();

    @Key("copilotAgentNotRunning")
    String copilotAgentNotRunning();

    @Key("copilotAgentNotEnabled")
    String copilotAgentNotEnabled();

    @Key("copilotAccountNotActivated")
    String copilotAccountNotActivated(String name);

    @Key("copilotNotSignedIn")
    String copilotNotSignedIn();

    @Key("copilotUnknownResponse")
    String copilotUnknownResponse(String response);

    @Key("gitSignCommitLabel")
    String gitSignCommitLabel();
}

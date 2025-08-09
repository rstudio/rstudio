/*
 * StudioClientCommonConstants.java
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

package org.rstudio.studio.client.common;

public interface StudioClientCommonConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String errorCaption();

    /**
     * Translated "Stop".
     *
     * @return translated "Stop"
     */
    String stopTitle();

    /**
     * Translated "Output".
     *
     * @return translated "Output"
     */
    String outputLeftLabel();

    /**
     * Translated "Issues".
     *
     * @return translated "Issues"
     */
    String issuesRightLabel();

    /**
     * Translated "Clear All Breakpoints".
     *
     * @return translated "Clear All Breakpoints"
     */
    String clearAllBreakpointsCaption();

    /**
     * Translated "Hide Traceback".
     *
     * @return translated "Hide Traceback"
     */
    String hideTracebackText();

    /**
     * Translated "Show Traceback".
     *
     * @return translated "Show Traceback"
     */
    String showTracebackText();

    /**
     * Translated "Converting Theme".
     *
     * @return translated "Converting Theme"
     */
    String convertingThemeProgressCaption();

    /**
     * Translated "Preparing Import from Mongo DB".
     *
     * @return translated "Preparing Import from Mongo DB"
     */
    String withDataImportMongoProgressCaption();

    /**
     * Translated "Using testthat".
     *
     * @return translated "Using testthat"
     */
    String testthatMessage();

    /**
     * Translated "Using shinytest".
     *
     * @return translated "Using shinytest"
     */
    String shinytestMessage();

    /**
     * Translated "Preparing Tests".
     *
     * @return translated "Preparing Tests"
     */
    String preparingTestsProgressCaption();

    /**
     * Translated "Testing Tools".
     *
     * @return translated "Testing Tools"
     */
    String testingToolsContext();

    /**
     * Translated "No Files Selected".
     *
     * @return translated "No Files Selected"
     */
    String noFilesSelectedCaption();

    /**
     * Translated "Please select one or more files to export.".
     *
     * @return translated "Please select one or more files to export."
     */
    String noFilesSelectedMessage();

    /**
     * Translated "Download".
     *
     * @return translated "Download"
     */
    String downloadButtonCaption();

    /**
     * Translated "R Code Browser".
     *
     * @return translated "R Code Browser"
     */
    String rCodeBrowserLabel();

    /**
     * Translated "R Data Frame".
     *
     * @return translated "R Data Frame"
     */
    String rDataFrameLabel();

    /**
     * Translated "Public Folder".
     *
     * @return translated "Public Folder"
     */
    String publicFolderDesc();

    /**
     * Translated "Folder".
     *
     * @return translated "Folder"
     */
    String folderDesc();

    /**
     * Translated "Text file".
     *
     * @return translated "Text file"
     */
    String textFileDesc();

    /**
     * Translated "Image file".
     *
     * @return translated "Image file"
     */
    String imageFileDesc();

    /**
     * Translated "Parent folder".
     *
     * @return translated "Parent folder"
     */
    String parentFolderDesc();

    /**
     * Translated "Explore object".
     *
     * @return translated "Explore object"
     */
    String exploreObjectDesc();

    /**
     * Translated "R source viewer".
     *
     * @return translated "R source viewer"
     */
    String rSourceViewerDesc();

    /**
     * Translated "Profiler".
     *
     * @return translated "Profiler"
     */
    String profilerDesc();

    /**
     * Translated "R Script".
     *
     * @return translated "R Script"
     */
    String rScriptLabel();

    /**
     * Translated "Rd File".
     *
     * @return translated "Rd File"
     */
    String rdFile();

    /**
     * Translated "NAMESPACE".
     *
     * @return translated "NAMESPACE"
     */
    String namespaceLabel();

    /**
     * Translated "R History".
     *
     * @return translated "R History"
     */
    String rHistoryLabel();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    String rMarkdownLabel();

    /**
     * Translated "R Notebook".
     *
     * @return translated "R Notebook"
     */
    String rNotebookLabel();

    /**
     * Translated "Markdown".
     *
     * @return translated "Markdown"
     */
    String markdownLabel();

    /**
     * Translated "File Download Error".
     *
     * @return translated "File Download Error"
     */
    String fileDownloadErrorCaption();

    /**
     * Translated "Unable to show file because file downloads are restricted on this server.\n".
     *
     * @return translated "Unable to show file because file downloads are restricted on this server.\n"
     */
    String fileDownloadErrorMessage();

    /**
     * Translated "Open".
     *
     * @return translated "Open"
     */
    String openLabel();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    String saveLabel();

    /**
     * Translated "Choose".
     *
     * @return translated "Choose"
     */
    String chooseLabel();

    /**
     * Translated "and ".
     *
     * @return translated "and "
     */
    String andText();

    /**
     * Translated "Typeset LaTeX into PDF using:".
     *
     * @return translated "Typeset LaTeX into PDF using:"
     */
    String typesetLatexLabel();

    /**
     * Translated "Help on customizing LaTeX options".
     *
     * @return translated "Help on customizing LaTeX options"
     */
    String latexHelpLinkLabel();

    /**
     * Translated "Error Setting CRAN Mirror".
     *
     * @return translated "Error Setting CRAN Mirror"
     */
    String errorSettingCranMirror();

    /**
     * Translated "The CRAN mirror could not be changed.".
     *
     * @return translated "The CRAN mirror could not be changed."
     */
    String cranMirrorCannotChange();

    /**
     * Translated "Insert Roxygen Skeleton".
     *
     * @return translated "Insert Roxygen Skeleton"
     */
    String insertRoxygenSkeletonMessage();

    /**
     * Translated "Unable to insert skeleton (the cursor is not currently inside an R function definition).".
     *
     * @return translated "Unable to insert skeleton (the cursor is not currently inside an R function definition)."
     */
    String unableToInsertSkeletonMessage();

    /**
     * Translated "Cannot automatically update roxygen blocks that are not self-contained.".
     *
     * @return translated "Cannot automatically update roxygen blocks that are not self-contained."
     */
    String cannotUpdateRoxygenMessage();

    /**
     * Translated "Confirm Change".
     *
     * @return translated "Confirm Change"
     */
    String confirmChangeCaption();


    /**
     * Translated "The {0} package is required for {1} weaving, however it is not currently installed. You should ensure that {0} is installed prior to compiling a PDF.\n\nAre you sure you want to change this option?".
     *
     * @return translated "The {0} package is required for {1} weaving, however it is not currently installed. You should ensure that {0} is installed prior to compiling a PDF.\n\nAre you sure you want to change this option?"
     */
    String packageRequiredMessage(String packageName, String name);

    /**
     * Translated "Upload Error Occurred".
     *
     * @return translated "Upload Error Occurred"
     */
    String uploadErrorTitle();

    /**
     * Translated "Unable to continue ".
     *
     * @return translated "Unable to continue (another publish is currently running)"
     */
    String unableToContinueMessage();

    /**
     * Translated "Uploading document to RPubs...".
     *
     * @return translated "Uploading document to RPubs..."
     */
    String uploadingDocumentRPubsMessage();

    /**
     * Translated "Publish to RPubs".
     *
     * @return translated "Publish to RPubs"
     */
    String publishToRPubs();

    /**
     * Translated "RPubs is a free service from RStudio for sharing documents on the web. Click Publish to get started.".
     *
     * @return translated "RPubs is a free service from RStudio for sharing documents on the web. Click Publish to get started."
     */
    String rPubsServiceMessage();

    /**
     * Translated "This document has already been published on RPubs. You can ".
     *
     * @return translated "This document has already been published on RPubs. You can "
     */
    String alreadyPublishedRPubs();


    /**
     * Translated "IMPORTANT: All documents published to RPubs are publicly visible.".
     *
     * @return translated "IMPORTANT: All documents published to RPubs are publicly visible."
     */
    String importantMessage();

    /**
     * Translated "You should only publish documents that you wish to share publicly.".
     *
     * @return translated "You should only publish documents that you wish to share publicly."
     */
    String publishMessage();

    /**
     * Translated "<strong>{0}</strong> {1}".
     *
     * @return translated "<strong>{0}</strong> {1}"
     */
    String importantRPubsMessage(String importantMessage, String publishMessage);


    /**
     * Translated "Publish".
     *
     * @return translated "Publish"
     */
    String publishButtonTitle();

    /**
     * Translated "Update Existing".
     *
     * @return translated "Update Existing"
     */
    String updateExistingButtonTitle();

    /**
     * Translated "Create New".
     *
     * @return translated "Create New"
     */
    String createNewButtonTitle();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    String cancelTitle();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    String okTitle();

    /**
     * Translated "Using Keyring".
     *
     * @return translated "Using Keyring"
     */
    String usingKeyringCaption();

    /**
     * Translated "Keyring is an R package that provides access to the operating systems credential store to allow you to remember, securely, passwords and secrets. ".
     *
     * @return translated "Keyring is an R package that provides access to the operating systems credential store to allow you to remember, securely, passwords and secrets. "
     */
    String keyringDesc();

    /**
     * Translated "Would you like to install keyring?".
     *
     * @return translated "Would you like to install keyring?"
     */
    String installKeyringMessage();

    /**
     * Translated "Keyring".
     *
     * @return translated "Keyring"
     */
    String keyringCaption();

    /**
     * Translated "Install".
     *
     * @return translated "Install"
     */
    String installLabel();

    /**
     * Translated "You must enter a value.".
     *
     * @return translated "You must enter a value."
     */
    String enterValueMessage();

    /**
     * Translated "Too much console output to announce.".
     *
     * @return translated "Too much console output to announce."
     */
    String consoleOutputOverLimitMessage();

    /**
     * Translated "Error: {0}\n".
     *
     * @return translated "Error: {0}\n"
     */
    String consoleWriteError(String error);

    /**
     * Translated "Line ".
     *
     * @return translated "Line "
     */
    String lineText();

    /**
     * Translated "View error or warning within the log file".
     *
     * @return translated "View error or warning within the log file"
     */
    String viewErrorLogfile();


    /**
     * Translated "Syncing...".
     *
     * @return translated "Syncing..."
     */
    String getSyncProgressMessage();

    /**
     * Translated "; Page ".
     *
     * @return translated "; Page ."
     */
    String pdfPageText();

    /**
     * Translated "[From Click]".
     *
     * @return translated "[From Click]"
     */
    String pdfFromClickText();

    /**
     * Translated "Password".
     *
     * @return translated "Password"
     */
    String passwordTitle();

    /**
     * Translated "Personal Access Token".
     *
     * @return translated "Personal Access Token"
     */
    String patTitle();

    /**
     * Translated "Personal access token".
     *
     * @return translated "Personal access token"
     */
    String patPrompt();

    /**
     * Translated "Username".
     *
     * @return translated "Username"
     */
    String usernameTitle();

    /**
     * Translated "Press {0} to copy the key to the clipboard".
     *
     * @return translated "Press {0} to copy the key to the clipboard"
     */
    String pressLabel(String cmdText);

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    String closeButtonLabel();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    String noneLabel();

    /**
     * Translated "Getting ignored files for path...".
     *
     * @return translated "Getting ignored files for path..."
     */
    String gettingIgnoredFilesProgressMessage();

    /**
     * Translated "Setting ignored files for path...".
     *
     * @return translated "Setting ignored files for path..."
     */
    String settingIgnoredFilesProgressMessage();

    /**
     * Translated "Error: Multiple Directories".
     *
     * @return translated "Error: Multiple Directories"
     */
    String multipleDirectoriesCaption();

    /**
     * Translated "The selected files are not all within the same directory you can only ignore multiple files in one operation if they are located within the same directory).".
     *
     * @return translated "The selected files are not all within the same directory you can only ignore multiple files in one operation if they are located within the same directory)."
     */
    String selectedFilesNotInSameDirectoryMessage();

    /**
     * Translated "Directory:".
     *
     * @return translated "Directory:"
     */
    String directoryLabel();

    /**
     * Translated "Ignored files".
     *
     * @return translated "Ignored files"
     */
    String ignoredFilesLabel();

    /**
     * Translated "Ignore:".
     *
     * @return translated "Ignore:"
     */
    String ignoreCaption();

    /**
     * Translated "Specifying ignored files".
     *
     * @return translated "Specifying ignored files"
     */
    String specifyingIgnoredFilesHelpCaption();

    /**
     * Translated "Loading file contents".
     *
     * @return translated "Loading file contents"
     */
    String loadingFileContentsProgressCaption();

    /**
     * Translated "Save As".
     *
     * @return translated "Save As"
     */
    String saveAsText();

    /**
     * Translated "Save File - {0}".
     *
     * @return translated "Save File - {0}"
     */
    String saveFileCaption(String name);

    /**
     * Translated "Saving file...".
     *
     * @return translated "Saving file..."
     */
    String savingFileProgressCaption();

    /**
     * Translated "View File Tab".
     *
     * @return translated "View File Tab"
     */
    String viewFileTabLabel();

    /**
     * Translated "at".
     *
     * @return translated "at"
     */
    String atText();

    /**
     * Translated "This is a warning!".
     *
     * @return translated "This is a warning!"
     */
    String warningMessage();

    /**
     * Translated "R Presentation".
     *
     * @return translated "R Presentation"
     */
    String rPresentationLabel();

    /**
     * Translated "Source Marker Item Table".
     *
     * @return translated "Source Marker Item Table"
     */
    String sourceMarkerItemTableList();

    /**
     * Translated "Preview".
     *
     * @return translated "Preview"
     */
    String previewButtonText();

    /**
     * Translated "Generic Content".
     *
     * @return translated "Generic Content"
     */
    String genericContentLabel();

    /**
     * Translated "Object Explorer".
     *
     * @return translated "Object Explorer"
     */
    String objectExplorerLabel();

    /**
     * Translated "R Profiler".
     *
     * @return translated "R Profiler"
     */
    String rProfilerLabel();

    /**
     * Translated "Check".
     *
     * @return translated "Check"
     */
    String checkPreviewButtonText();

    /**
     * Translated "Weave Rnw files using:".
     *
     * @return translated "Weave Rnw files using:"
     */
    String weaveRnwLabel();

    /**
     * Translated "Help on weaving Rnw files".
     *
     * @return translated "Help on weaving Rnw files"
     */
    String weaveRnwHelpTitle();

    /**
     * Translated "SSH key:".
     *
     * @return translated "SSH key:"
     */
    String sshRSAKeyFormLabel();

    /**
     * Translated "View public key".
     *
     * @return translated "View public key"
     */
    String viewPublicKeyCaption();

    /**
     * Translated "Create SSH Key...".
     *
     * @return translated "Create SSH Key..."
     */
    String createRSAKeyButtonLabel();

    /**
     * Translated "Reading public key...".
     *
     * @return translated "Reading public key..."
     */
    String readingPublicKeyProgressCaption();

    /**
     * Translated "Are you sure you want to remove all the breakpoints in this project?".
     *
     * @return translated "Are you sure you want to remove all the breakpoints in this project?"
     */
    String clearAllBreakpointsMessage();

    /**
     * Translated "{0}{1}".
     *
     * @return translated "{0}{1}"
     */
    String functionNameText(String functionName, String source);

    /**
     * Translated "{0}".
     *
     * @return translated "{0}"
     */
    String showFileExportLabel(String description);

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
     * Translated "Account records appear to exist, but cannot be viewed because a ".
     *
     * @return translated "Account records appear to exist, but cannot be viewed because a "
     */
    String missingPkgPanelMessage();

    /**
     * Translated "required package is not installed.".
     *
     * @return translated "required package is not installed."
     */
    String missingPkgRequiredMessage();

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
     * Translated "Confirm Remove Account".
     *
     * @return translated "Confirm Remove Account"
     */
    String removeAccountGlobalDisplay();


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
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    String connectAccountCaption();

    /**
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    String connectAccountOkCaption();

    /**
     * Translated "Connect Publishing Account".
     *
     * @return translated "Connect Publishing Account"
     */
    String newRSConnectAccountPageTitle();

    /**
     * Translated "Pick an account".
     *
     * @return translated "Pick an account"
     */
    String newRSConnectAccountPageSubTitle();

    /**
     * Translated "Connect Publishing Account".
     *
     * @return translated "Connect Publishing Account"
     */
    String newRSConnectAccountPageCaption();

    /**
     * Translated "Choose Account Type".
     *
     * @return translated "Choose Account Type"
     */
    String wizardNavigationPageTitle();

    /**
     * Translated "Choose Account Type".
     *
     * @return translated "Choose Account Type"
     */
    String wizardNavigationPageSubTitle();

    /**
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    String wizardNavigationPageCaption();

    /**
     * Translated "Posit Connect is a server product from Posit ".
     *
     * @return translated "Posit Connect is a server product from Posit "
     */
    String serviceDescription();

    /**
     * Translated "for secure sharing of applications, reports, plots, and APIs.".
     *
     * @return translated "for secure sharing of applications, reports, plots, and APIs."
     */
    String serviceMessageDescription();

    /**
     * Translated "A cloud service run by RStudio. Publish Shiny applications ".
     *
     * @return translated "A cloud service run by RStudio. Publish Shiny applications "
     */
    String newRSConnectCloudPageSubTitle();

    /**
     * Translated "and interactive documents to the Internet.".
     *
     * @return translated "and interactive documents to the Internet."
     */
    String newRSConnectCloudPageSub();

    /**
     * Translated "Connect ShinyApps.io Account".
     *
     * @return translated "Connect ShinyApps.io Account"
     */
    String newRSConnectCloudPageCaption();

    /**
     * Translated "Converting Theme".
     *
     * @return translated "Converting Theme"
     */
    String withThemesCaption();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    String withRMarkdownCaption();

    /**
     * Translated "Install Shiny Package".
     *
     * @return translated "Install Shiny Package"
     */
    String installShinyCaption();

    /**
     * Translated "requires installation of an updated version ".
     *
     * @return translated "requires installation of an updated version "
     */
    String installShinyUserAction(String userAction);

    /**
     * Translated "Checking installed packages".
     *
     * @return translated "Checking installed packages"
     */
    String installPkgsCaption();

    /**
     * Translated "Checking installed packages".
     *
     * @return translated "Checking installed packages"
     */
    String withShinyAddinsCaption();

    /**
     * Translated "Checking installed packages".
     *
     * @return translated "Checking installed packages"
     */
    String withShinyAddinsUserAction();

    /**
     * Translated "Reformatting code...".
     *
     * @return translated "Reformatting code..."
     */
    String withStylerCaption();

    /**
     * Translated "Reformatting code with styler".
     *
     * @return translated "Reformatting code with styler"
     */
    String withStylerUserAction();

    /**
     * Translated "Preparing import from CSV".
     *
     * @return translated "Preparing import from CSV"
     */
    String withDataImportCSVCaption();

    /**
     * Translated "Preparing import from SPSS, SAS and Stata".
     *
     * @return translated "Preparing import from SPSS, SAS and Stata"
     */
    String withDataImportSAV();

    /**
     * Translated "Preparing import from Excel".
     *
     * @return translated "Preparing import from Excel"
     */
    String withDataImportXLS();

    /**
     * Translated "Preparing import from XML".
     *
     * @return translated "Preparing import from XML"
     */
    String withDataImportXML();

    /**
     * Translated "Preparing import from JSON".
     *
     * @return translated "Preparing import from JSON"
     */
    String withDataImportJSON();

    /**
     * Translated "Preparing import from JDBC".
     *
     * @return translated "Preparing import from JDBC"
     */
    String withDataImportJDBC();

    /**
     * Translated "Preparing import from ODBC".
     *
     * @return translated "Preparing import from ODBC"
     */
    String withDataImportODBC();

    /**
     * Translated "Preparing profiler".
     *
     * @return translated "Preparing profiler"
     */
    String withProfvis();

    /**
     * Translated "Preparing connection".
     *
     * @return translated "Preparing connection"
     */
    String withConnectionPackage();

    /**
     * Translated "Database Connectivity".
     *
     * @return translated "Database Connectivity"
     */
    String withConnectionPackageContext();

    /**
     * Translated "Preparing Keyring".
     *
     * @return translated "Preparing Keyring"
     */
    String withKeyring();

    /**
     * Translated "Using keyring".
     *
     * @return translated "Using keyring"
     */
    String withKeyringUserAction();

    /**
     * Translated "Preparing ".
     *
     * @return translated "Preparing "
     */
    String withOdbc();

    /**
     * Translated "Preparing ".
     *
     * @return translated "Preparing "
     */
    String withOdbcUserAction();

    /**
     * Translated "Starting tutorial".
     *
     * @return translated "Starting tutorial"
     */
    String withTutorialDependencies();

    /**
     * Translated "Starting a tutorial".
     *
     * @return translated "Starting a tutorial"
     */
    String withTutorialDependenciesUserAction();

    /**
     * Translated "Using the AGG renderer".
     *
     * @return translated "Using the AGG renderer"
     */
    String withRagg();

    /**
     * Translated "Using the Databricks Connect integration".
     *
     * @return translated "Using the Databricks Connect integration"
     */
    String withDatabricksConnect();

    /**
     * Translated " is not available\n".
     *
     * @return translated "is not available"
     */
    String unsatisfiedVersions();

    /**
     * Translated "is required but is available".
     *
     * @return translated "is required but is available"
     */
    String requiredVersion(String version);

    /**
     * Translated "Packages Not Found".
     *
     * @return translated "Packages Not Found"
     */
    String packageNotFoundUserAction();

    /**
     * Translated "Required package versions could not be found:\n\n{0}\nCheck that getOption(\"repos\") refers to a CRAN repository that contains the needed package versions.".
     *
     * @return translated "Required package versions could not be found:\n\n{0}\nCheck that getOption(\"repos\") refers to a CRAN repository that contains the needed package versions."
     */
    String packageNotFoundMessage(String unsatisfiedVersions);

    /**
     * Translated "Dependency installation failed".
     *
     * @return translated "Dependency installation failed"
     */
    String onErrorMessage();

    /**
     * Translated "Could not determine available packages".
     *
     * @return translated "Could not determine available packages"
     */
    String availablePackageErrorMessage();

    /**
     * Translated "requires an updated version of the {0} package.\n\nDo you want to install this package now?".
     *
     * @return translated "requires an updated version of the {0} package.\n\nDo you want to install this package now?"
     */
    String confirmPackageInstallation(String name);


    /**
     * Translated "requires updated versions of the following packages: {0}.\n\nDo you want to install these packages now?".
     *
     * @return translated "requires updated versions of the following packages: {0}.\n\nDo you want to install these packages now?"
     */
    String updatedVersionMessage(String dependency);

    /**
     * Translated "Install Required Packages".
     *
     * @return translated "Install Required Packages"
     */
    String installRequiredCaption();


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
     * Translated "Version Control ".
     *
     * @return translated "Version Control "
     */
    String globalDisplayVC();

    /**
     * Translated "You must restart RStudio for this change to take effect.".
     *
     * @return translated "You must restart RStudio for this change to take effect."
     */
    String globalDisplayVCMessage();


    /**
     * Translated "Terminal executable:".
     *
     * @return translated "Terminal executable:"
     */
    String terminalPathLabel();

    /**
     * Translated "Public Key".
     *
     * @return translated "Public Key"
     */
    String showPublicKeyDialogCaption();

    /**
     * Translated "Error attempting to read key ''{0}'' ({1})''".
     *
     * @return translated "Error attempting to read key ''{0}'' ({1})''"
     */
    String onSSHErrorMessage(String keyPath, String errorMessage);

    /**
     * Translated "Using Version Control with RStudio".
     *
     * @return translated "Using Version Control with RStudio"
     */
    String vCSHelpLink();

    /**
     * Translated "Create SSH Key".
     *
     * @return translated "Create SSH Key"
     */
    String createKeyDialogCaption();

    /**
     * Translated "Creating SSH Key".
     *
     * @return translated "Creating SSH Key"
     */
    String onProgressLabel();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    String setOkButtonCaption();

    /**
     * Translated "Non-Matching Passphrases".
     *
     * @return translated "Non-Matching Passphrases"
     */
    String showValidateErrorCaption();

    /**
     * Translated "The passphrase and passphrase confirmation do not match.".
     *
     * @return translated "The passphrase and passphrase confirmation do not match."
     */
    String showValidateErrorMessage();

    /**
     * Translated "The SSH key will be created at:".
     *
     * @return translated "The SSH key will be created at:"
     */
    String pathCaption();

    /**
     * Translated "SSH key management".
     *
     * @return translated "SSH key management"
     */
    String pathHelpCaption();

    /**
     * Translated "SSH key type: ".
     *
     * @return translated "SSH key type: "
     */
    String sshKeyTypeLabel();

    /**
     * Translated "RSA"
     *
     * @return translated "RSA"
     */
    String sshKeyRSAOption();

    /**
     * Translated "ED25519"
     *
     * @return translated "ED25519"
     */
    String sshKeyEd25519Option();

    /**
     * Translated "Passphrase (optional):".
     *
     * @return translated "Passphrase (optional):"
     */
    String passphraseLabel();

    /**
     * Translated "Confirm:".
     *
     * @return translated "Confirm:"
     */
    String passphraseConfirmLabel();

    /**
     * Translated "Key Already Exists".
     *
     * @return translated "Key Already Exists"
     */
    String confirmOverwriteKeyCaption();

    /**
     * Translated "An SSH key already exists at {0}. Do you want to overwrite the existing key?".
     *
     * @return translated "An SSH key already exists at {0}. Do you want to overwrite the existing key?"
     */
    String confirmOverwriteKeyMessage(String path);

    /**
     * Translated "Main dictionary language:".
     *
     * @return translated "Main dictionary language:"
     */
    String spellingLanguageSelectWidgetLabel();

    /**
     * Translated "Help on spelling dictionaries".
     *
     * @return translated "Help on spelling dictionaries"
     */
    String addHelpButtonLabel();

    /**
     * Translated "Downloading dictionaries...".
     *
     * @return translated "Downloading dictionaries..."
     */
    String progressDownloadingLabel();

    /**
     * Translated "Downloading dictionaries...".
     *
     * @return translated "Downloading dictionaries..."
     */
    String progressDownloadingLanguagesLabel();

    /**
     * Translated "Error Downloading Dictionaries".
     *
     * @return translated "Error Downloading Dictionaries"
     */
    String onErrorDownloadingCaption();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    String includeDefaultOption();

    /**
     * Translated "Update Dictionaries...".
     *
     * @return translated "Update Dictionaries..."
     */
    String allLanguagesInstalledOption();

    /**
     * Translated "Install More Languages...".
     *
     * @return translated "Install More Languages..."
     */
    String installIndexOption();

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
     * Translated "Custom dictionaries:".
     *
     * @return translated "Custom dictionaries:"
     */
    String labelWithHelpText();

    /**
     * Translated "Help on custom spelling dictionaries".
     *
     * @return translated "Help on custom spelling dictionaries"
     */
    String labelWithHelpTitle();

    /**
     * Translated "Add Custom Dictionary (*.dic)".
     *
     * @return translated "Add Custom Dictionary (*.dic)"
     */
    String fileDialogsCaption();

    /**
     * Translated "Add Custom Dictionary (*.dic)".
     *
     * @return translated "Add Custom Dictionary (*.dic)"
     */
    String fileDialogsFilter();

    /**
     * Translated "Adding dictionary...".
     *
     * @return translated "Adding dictionary..."
     */
    String onProgressAddingLabel();

    /**
     * Translated "Confirm Remove".
     *
     * @return translated "Confirm Remove"
     */
    String removeDictionaryCaption();

    /**
     * Translated "Are you sure you want to remove the {0} custom dictionary?".
     *
     * @return translated "Are you sure you want to remove the {0} custom dictionary?"
     */
    String removeDictionaryMessage(String dictionary);

    /**
     * Translated "Removing dictionary...".
     *
     * @return translated "Removing dictionary..."
     */
    String progressRemoveIndicator();

    /**
     * Translated "[Detected output overflow; buffering the next {0} lines of output]".
     *
     * @return translated "[Detected output overflow; buffering the next {0} lines of output]"
     */
    String consoleBufferedMessage(int bufferSize);

}

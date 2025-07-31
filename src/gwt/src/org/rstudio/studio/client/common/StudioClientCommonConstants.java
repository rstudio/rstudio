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

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("Stop")
    @Key("stopTitle")
    String stopTitle();

    @DefaultMessage("Output")
    @Key("outputLeftLabel")
    String outputLeftLabel();

    @DefaultMessage("Issues")
    @Key("issuesRightLabel")
    String issuesRightLabel();

    @DefaultMessage("Clear All Breakpoints")
    @Key("clearAllBreakpointsCaption")
    String clearAllBreakpointsCaption();

    @DefaultMessage("Hide Traceback")
    @Key("hideTracebackText")
    String hideTracebackText();

    @DefaultMessage("Show Traceback")
    @Key("showTracebackText")
    String showTracebackText();

    @DefaultMessage("Converting Theme")
    @Key("convertingThemeProgressCaption")
    String convertingThemeProgressCaption();

    @DefaultMessage("Preparing Import from Mongo DB")
    @Key("withDataImportMongoProgressCaption")
    String withDataImportMongoProgressCaption();

    @DefaultMessage("Using testthat")
    @Key("testthatMessage")
    String testthatMessage();

    @DefaultMessage("Using shinytest")
    @Key("shinytestMessage")
    String shinytestMessage();

    @DefaultMessage("Preparing Tests")
    @Key("preparingTestsProgressCaption")
    String preparingTestsProgressCaption();

    @DefaultMessage("Testing Tools")
    @Key("testingToolsContext")
    String testingToolsContext();

    @DefaultMessage("No Files Selected")
    @Key("noFilesSelectedCaption")
    String noFilesSelectedCaption();

    @DefaultMessage("Please select one or more files to export.")
    @Key("noFilesSelectedMessage")
    String noFilesSelectedMessage();

    @DefaultMessage("Download")
    @Key("downloadButtonCaption")
    String downloadButtonCaption();

    @DefaultMessage("R Code Browser")
    @Key("rCodeBrowserLabel")
    String rCodeBrowserLabel();

    @DefaultMessage("R Data Frame")
    @Key("rDataFrameLabel")
    String rDataFrameLabel();

    @DefaultMessage("Public Folder")
    @Key("publicFolderDesc")
    String publicFolderDesc();

    @DefaultMessage("Folder")
    @Key("folderDesc")
    String folderDesc();

    @DefaultMessage("Text file")
    @Key("textFileDesc")
    String textFileDesc();

    @DefaultMessage("Image file")
    @Key("imageFileDesc")
    String imageFileDesc();

    @DefaultMessage("Parent folder")
    @Key("parentFolderDesc")
    String parentFolderDesc();

    @DefaultMessage("Explore object")
    @Key("exploreObjectDesc")
    String exploreObjectDesc();

    @DefaultMessage("R source viewer")
    @Key("rSourceViewerDesc")
    String rSourceViewerDesc();

    @DefaultMessage("Profiler")
    @Key("profilerDesc")
    String profilerDesc();

    @DefaultMessage("R Script")
    @Key("rScriptLabel")
    String rScriptLabel();

    @DefaultMessage("Rd File")
    @Key("rdFile")
    String rdFile();

    @DefaultMessage("NAMESPACE")
    @Key("namespaceLabel")
    String namespaceLabel();

    @DefaultMessage("R History")
    @Key("rHistoryLabel")
    String rHistoryLabel();

    @DefaultMessage("R Markdown")
    @Key("rMarkdownLabel")
    String rMarkdownLabel();

    @DefaultMessage("R Notebook")
    @Key("rNotebookLabel")
    String rNotebookLabel();

    @DefaultMessage("Markdown")
    @Key("markdownLabel")
    String markdownLabel();

    @DefaultMessage("File Download Error")
    @Key("fileDownloadErrorCaption")
    String fileDownloadErrorCaption();

    @DefaultMessage("Unable to show file because file downloads are restricted on this server.\\n")
    @Key("fileDownloadErrorMessage")
    String fileDownloadErrorMessage();

    @DefaultMessage("Open")
    @Key("openLabel")
    String openLabel();

    @DefaultMessage("Save")
    @Key("saveLabel")
    String saveLabel();

    @DefaultMessage("Choose")
    @Key("chooseLabel")
    String chooseLabel();

    @DefaultMessage("and ")
    @Key("andText")
    String andText();

    @DefaultMessage("Typeset LaTeX into PDF using:")
    @Key("typesetLatexLabel")
    String typesetLatexLabel();

    @DefaultMessage("Help on customizing LaTeX options")
    @Key("latexHelpLinkLabel")
    String latexHelpLinkLabel();

    @DefaultMessage("Error Setting CRAN Mirror")
    @Key("errorSettingCranMirror")
    String errorSettingCranMirror();

    @DefaultMessage("The CRAN mirror could not be changed.")
    @Key("cranMirrorCannotChange")
    String cranMirrorCannotChange();

    @DefaultMessage("Insert Roxygen Skeleton")
    @Key("insertRoxygenSkeletonMessage")
    String insertRoxygenSkeletonMessage();

    @DefaultMessage("Unable to insert skeleton (the cursor is not currently inside an R function definition).")
    @Key("unableToInsertSkeletonMessage")
    String unableToInsertSkeletonMessage();

    @DefaultMessage("Cannot automatically update roxygen blocks that are not self-contained.")
    @Key("cannotUpdateRoxygenMessage")
    String cannotUpdateRoxygenMessage();

    @DefaultMessage("Confirm Change")
    @Key("confirmChangeCaption")
    String confirmChangeCaption();

    @DefaultMessage("The {0} package is required for {1} weaving, however it is not currently installed. You should ensure that {0} is installed prior to compiling a PDF.\\n\\nAre you sure you want to change this option?")
    @Key("packageRequiredMessage")
    String packageRequiredMessage(String packageName, String name);

    @DefaultMessage("Upload Error Occurred")
    @Key("uploadErrorTitle")
    String uploadErrorTitle();

    @DefaultMessage("Unable to continue (another publish is currently running)")
    @Key("unableToContinueMessage")
    String unableToContinueMessage();

    @DefaultMessage("Uploading document to RPubs...")
    @Key("uploadingDocumentRPubsMessage")
    String uploadingDocumentRPubsMessage();

    @DefaultMessage("Publish to RPubs")
    @Key("publishToRPubs")
    String publishToRPubs();

    @DefaultMessage("RPubs is a free service from RStudio for sharing documents on the web. Click Publish to get started.")
    @Key("rPubsServiceMessage")
    String rPubsServiceMessage();

    @DefaultMessage("This document has already been published on RPubs. You can ")
    @Key("alreadyPublishedRPubs")
    String alreadyPublishedRPubs();

    @DefaultMessage("IMPORTANT: All documents published to RPubs are publicly visible.")
    @Key("importantMessage")
    String importantMessage();

    @DefaultMessage("You should only publish documents that you wish to share publicly.")
    @Key("publishMessage")
    String publishMessage();

    @DefaultMessage("<strong>{0}</strong> {1}")
    @Key("importantRPubsMessage")
    String importantRPubsMessage(String importantMessage, String publishMessage);

    @DefaultMessage("Publish")
    @Key("publishButtonTitle")
    String publishButtonTitle();

    @DefaultMessage("Update Existing")
    @Key("updateExistingButtonTitle")
    String updateExistingButtonTitle();

    @DefaultMessage("Create New")
    @Key("createNewButtonTitle")
    String createNewButtonTitle();

    @DefaultMessage("Cancel")
    @Key("cancelTitle")
    String cancelTitle();

    @DefaultMessage("OK")
    @Key("okTitle")
    String okTitle();

    @DefaultMessage("Using Keyring")
    @Key("usingKeyringCaption")
    String usingKeyringCaption();

    @DefaultMessage("Keyring is an R package that provides access to the operating systems credential store to allow you to remember, securely, passwords and secrets. ")
    @Key("keyringDesc")
    String keyringDesc();

    @DefaultMessage("Would you like to install keyring?")
    @Key("installKeyringMessage")
    String installKeyringMessage();

    @DefaultMessage("Keyring")
    @Key("keyringCaption")
    String keyringCaption();

    @DefaultMessage("Install")
    @Key("installLabel")
    String installLabel();

    @DefaultMessage("You must enter a value.")
    @Key("enterValueMessage")
    String enterValueMessage();

    @DefaultMessage("Too much console output to announce.")
    @Key("consoleOutputOverLimitMessage")
    String consoleOutputOverLimitMessage();

    @DefaultMessage("Error: {0}\\n")
    @Key("consoleWriteError")
    String consoleWriteError(String error);

    @DefaultMessage("Line ")
    @Key("lineText")
    String lineText();

    @DefaultMessage("View error or warning within the log file")
    @Key("viewErrorLogfile")
    String viewErrorLogfile();

    @DefaultMessage("Syncing...")
    @Key("getSyncProgressMessage")
    String getSyncProgressMessage();

    @DefaultMessage("; Page ")
    @Key("pdfPageText")
    String pdfPageText();

    @DefaultMessage("[From Click]")
    @Key("pdfFromClickText")
    String pdfFromClickText();

    @DefaultMessage("Password")
    @Key("passwordTitle")
    String passwordTitle();

    @DefaultMessage("Personal Access Token")
    @Key("patTitle")
    String patTitle();

    @DefaultMessage("Personal access token")
    @Key("patPrompt")
    String patPrompt();

    @DefaultMessage("Username")
    @Key("usernameTitle")
    String usernameTitle();

    @DefaultMessage("Press {0} to copy the key to the clipboard")
    @Key("pressLabel")
    String pressLabel(String cmdText);

    @DefaultMessage("Close")
    @Key("closeButtonLabel")
    String closeButtonLabel();

    @DefaultMessage("(None)")
    @Key("noneLabel")
    String noneLabel();

    @DefaultMessage("Getting ignored files for path...")
    @Key("gettingIgnoredFilesProgressMessage")
    String gettingIgnoredFilesProgressMessage();

    @DefaultMessage("Setting ignored files for path...")
    @Key("settingIgnoredFilesProgressMessage")
    String settingIgnoredFilesProgressMessage();

    @DefaultMessage("Error: Multiple Directories")
    @Key("multipleDirectoriesCaption")
    String multipleDirectoriesCaption();

    @DefaultMessage("The selected files are not all within the same directory (you can only ignore multiple files in one operation if they are located within the same directory).")
    @Key("selectedFilesNotInSameDirectoryMessage")
    String selectedFilesNotInSameDirectoryMessage();

    @DefaultMessage("Directory:")
    @Key("directoryLabel")
    String directoryLabel();

    @DefaultMessage("Ignored files")
    @Key("ignoredFilesLabel")
    String ignoredFilesLabel();

    @DefaultMessage("Ignore:")
    @Key("ignoreCaption")
    String ignoreCaption();

    @DefaultMessage("Specifying ignored files")
    @Key("specifyingIgnoredFilesHelpCaption")
    String specifyingIgnoredFilesHelpCaption();

    @DefaultMessage("Loading file contents")
    @Key("loadingFileContentsProgressCaption")
    String loadingFileContentsProgressCaption();

    @DefaultMessage("Save As")
    @Key("saveAsText")
    String saveAsText();

    @DefaultMessage("Save File - {0}")
    @Key("saveFileCaption")
    String saveFileCaption(String name);

    @DefaultMessage("Saving file...")
    @Key("savingFileProgressCaption")
    String savingFileProgressCaption();

    @DefaultMessage("View File Tab")
    @Key("viewFileTabLabel")
    String viewFileTabLabel();

    @DefaultMessage("at")
    @Key("atText")
    String atText();

    @DefaultMessage("This is a warning!")
    @Key("warningMessage")
    String warningMessage();

    @DefaultMessage("R Presentation")
    @Key("rPresentationLabel")
    String rPresentationLabel();

    @DefaultMessage("Source Marker Item Table")
    @Key("sourceMarkerItemTableList")
    String sourceMarkerItemTableList();

    @DefaultMessage("Preview")
    @Key("previewButtonText")
    String previewButtonText();

    @DefaultMessage("Generic Content")
    @Key("genericContentLabel")
    String genericContentLabel();

    @DefaultMessage("Object Explorer")
    @Key("objectExplorerLabel")
    String objectExplorerLabel();

    @DefaultMessage("R Profiler")
    @Key("rProfilerLabel")
    String rProfilerLabel();

    @DefaultMessage("Check")
    @Key("checkPreviewButtonText")
    String checkPreviewButtonText();

    @DefaultMessage("Weave Rnw files using:")
    @Key("weaveRnwLabel")
    String weaveRnwLabel();

    @DefaultMessage("Help on weaving Rnw files")
    @Key("weaveRnwHelpTitle")
    String weaveRnwHelpTitle();

    @DefaultMessage("SSH key:")
    @Key("sshRSAKeyFormLabel")
    String sshRSAKeyFormLabel();

    @DefaultMessage("View public key")
    @Key("viewPublicKeyCaption")
    String viewPublicKeyCaption();

    @DefaultMessage("Create SSH Key...")
    @Key("createRSAKeyButtonLabel")
    String createRSAKeyButtonLabel();

    @DefaultMessage("Reading public key...")
    @Key("readingPublicKeyProgressCaption")
    String readingPublicKeyProgressCaption();

    @DefaultMessage("Are you sure you want to remove all the breakpoints in this project?")
    @Key("clearAllBreakpointsMessage")
    String clearAllBreakpointsMessage();

    @DefaultMessage("{0}{1}")
    @Key("functionNameText")
    String functionNameText(String functionName, String source);

    @DefaultMessage("{0}")
    @Key("showFileExportLabel")
    String showFileExportLabel(String description);

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

    @DefaultMessage("Account records appear to exist, but cannot be viewed because a ")
    @Key("missingPkgPanelMessage")
    String missingPkgPanelMessage();

    @DefaultMessage("required package is not installed.")
    @Key("missingPkgRequiredMessage")
    String missingPkgRequiredMessage();

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

    @DefaultMessage("Connect Account")
    @Key("connectAccountCaption")
    String connectAccountCaption();

    @DefaultMessage("Connect Account")
    @Key("connectAccountOkCaption")
    String connectAccountOkCaption();

    @DefaultMessage("Connect Publishing Account")
    @Key("newRSConnectAccountPageTitle")
    String newRSConnectAccountPageTitle();

    @DefaultMessage("Pick an account")
    @Key("newRSConnectAccountPageSubTitle")
    String newRSConnectAccountPageSubTitle();

    @DefaultMessage("Connect Publishing Account")
    @Key("newRSConnectAccountPageCaption")
    String newRSConnectAccountPageCaption();

    @DefaultMessage("Choose Account Type")
    @Key("wizardNavigationPageTitle")
    String wizardNavigationPageTitle();

    @DefaultMessage("Choose Account Type")
    @Key("wizardNavigationPageSubTitle")
    String wizardNavigationPageSubTitle();

    @DefaultMessage("Connect Account")
    @Key("wizardNavigationPageCaption")
    String wizardNavigationPageCaption();

    @DefaultMessage("Posit Connect is a server product from Posit ")
    @Key("serviceDescription")
    String serviceDescription();

    @DefaultMessage("for secure sharing of applications, reports, plots, and APIs.")
    @Key("serviceMessageDescription")
    String serviceMessageDescription();

    @DefaultMessage("A cloud service run by RStudio. Publish Shiny applications ")
    @Key("newRSConnectCloudPageSubTitle")
    String newRSConnectCloudPageSubTitle();

    @DefaultMessage("and interactive documents to the Internet.")
    @Key("newRSConnectCloudPageSub")
    String newRSConnectCloudPageSub();

    @DefaultMessage("Connect ShinyApps.io Account")
    @Key("newRSConnectCloudPageCaption")
    String newRSConnectCloudPageCaption();

    @DefaultMessage("Converting Theme")
    @Key("withThemesCaption")
    String withThemesCaption();

    @DefaultMessage("R Markdown")
    @Key("withRMarkdownCaption")
    String withRMarkdownCaption();

    @DefaultMessage("Install Shiny Package")
    @Key("installShinyCaption")
    String installShinyCaption();

    @DefaultMessage("{0} requires installation of an updated version of the shiny package.\n\nDo you want to install shiny now?")
    @Key("installShinyUserAction")
    String installShinyUserAction(String userAction);

    @DefaultMessage("Checking installed packages")
    @Key("installPkgsCaption")
    String installPkgsCaption();

    @DefaultMessage("Checking installed packages")
    @Key("withShinyAddinsCaption")
    String withShinyAddinsCaption();

    @DefaultMessage("Executing addins")
    @Key("withShinyAddinsUserAction")
    String withShinyAddinsUserAction();

    @DefaultMessage("Reformatting code...")
    @Key("withStylerCaption")
    String withStylerCaption();

    @DefaultMessage("Reformatting code with styler")
    @Key("withStylerUserAction")
    String withStylerUserAction();

    @DefaultMessage("Preparing import from CSV")
    @Key("withDataImportCSVCaption")
    String withDataImportCSVCaption();

    @DefaultMessage("Preparing import from SPSS, SAS and Stata")
    @Key("withDataImportSAV")
    String withDataImportSAV();

    @DefaultMessage("Preparing import from Excel")
    @Key("withDataImportXLS")
    String withDataImportXLS();

    @DefaultMessage("Preparing import from XML")
    @Key("withDataImportXML")
    String withDataImportXML();

    @DefaultMessage("Preparing import from JSON")
    @Key("withDataImportJSON")
    String withDataImportJSON();

    @DefaultMessage("Preparing import from JDBC")
    @Key("withDataImportJDBC")
    String withDataImportJDBC();

    @DefaultMessage("Preparing import from ODBC")
    @Key("withDataImportODBC")
    String withDataImportODBC();

    @DefaultMessage("Preparing profiler")
    @Key("withProfvis")
    String withProfvis();

    @DefaultMessage("Preparing connection")
    @Key("withConnectionPackage")
    String withConnectionPackage();

    @DefaultMessage("Database Connectivity")
    @Key("withConnectionPackageContext")
    String withConnectionPackageContext();

    @DefaultMessage("Preparing Keyring")
    @Key("withKeyring")
    String withKeyring();

    @DefaultMessage("Using keyring")
    @Key("withKeyringUserAction")
    String withKeyringUserAction();

    @DefaultMessage("Preparing ")
    @Key("withOdbc")
    String withOdbc();

    @DefaultMessage("Using ")
    @Key("withOdbcUserAction")
    String withOdbcUserAction();

    @DefaultMessage("Starting tutorial")
    @Key("withTutorialDependencies")
    String withTutorialDependencies();

    @DefaultMessage("Starting a tutorial")
    @Key("withTutorialDependenciesUserAction")
    String withTutorialDependenciesUserAction();

    @DefaultMessage("Using the AGG renderer")
    @Key("withRagg")
    String withRagg();

    @DefaultMessage("is not available")
    @Key("unsatisfiedVersions")
    String unsatisfiedVersions();

    @DefaultMessage("is required but {0} is available")
    @Key("requiredVersion")
    String requiredVersion(String version);

    @DefaultMessage("Packages Not Found")
    @Key("packageNotFoundUserAction")
    String packageNotFoundUserAction();

    @DefaultMessage("Required package versions could not be found:\n\n{0}\nCheck that getOption(\\\"repos\\\") refers to a CRAN repository that contains the needed package versions.")
    @Key("packageNotFoundMessage")
    String packageNotFoundMessage(String unsatisfiedVersions);

    @DefaultMessage("Dependency installation failed")
    @Key("onErrorMessage")
    String onErrorMessage();

    @DefaultMessage("Could not determine available packages")
    @Key("availablePackageErrorMessage")
    String availablePackageErrorMessage();

    @DefaultMessage("requires an updated version of the {0} package.\n\nDo you want to install this package now?")
    @Key("confirmPackageInstallation")
    String confirmPackageInstallation(String name);

    @DefaultMessage("requires updated versions of the following packages: {0}. \n\nDo you want to install these packages now?")
    @Key("updatedVersionMessage")
    String updatedVersionMessage(String dependency);

    @DefaultMessage("Install Required Packages")
    @Key("installRequiredCaption")
    String installRequiredCaption();

    @DefaultMessage("Enable")
    @Key("globalDisplayEnable")
    String globalDisplayEnable();

    @DefaultMessage("Disable")
    @Key("globalDisplayDisable")
    String globalDisplayDisable();

    @DefaultMessage("Version Control ")
    @Key("globalDisplayVC")
    String globalDisplayVC();

    @DefaultMessage("You must restart RStudio for this change to take effect.")
    @Key("globalDisplayVCMessage")
    String globalDisplayVCMessage();

    @DefaultMessage("Terminal executable:")
    @Key("terminalPathLabel")
    String terminalPathLabel();

    @DefaultMessage("Public Key")
    @Key("showPublicKeyDialogCaption")
    String showPublicKeyDialogCaption();

    @DefaultMessage("Error attempting to read key ''{0}'' ({1})''")
    @Key("onSSHErrorMessage")
    String onSSHErrorMessage(String keyPath, String errorMessage);

    @DefaultMessage("Using Version Control with RStudio")
    @Key("vCSHelpLink")
    String vCSHelpLink();

    @DefaultMessage("Create SSH Key")
    @Key("createKeyDialogCaption")
    String createKeyDialogCaption();

    @DefaultMessage("Creating SSH Key...")
    @Key("onProgressLabel")
    String onProgressLabel();

    @DefaultMessage("Create")
    @Key("setOkButtonCaption")
    String setOkButtonCaption();

    @DefaultMessage("Non-Matching Passphrases")
    @Key("showErrorCaption")
    String showValidateErrorCaption();

    @DefaultMessage("The passphrase and passphrase confirmation do not match.")
    @Key("showErrorMessage")
    String showValidateErrorMessage();

    @DefaultMessage("The SSH key will be created at:")
    @Key("pathCaption")
    String pathCaption();

    @DefaultMessage("SSH key management")
    @Key("pathHelpCaption")
    String pathHelpCaption();

    @DefaultMessage("SSH key type: ")
    @Key("sshKeyTypeLabel")
    String sshKeyTypeLabel();

    @DefaultMessage("RSA")
    @Key("sshKeyRSAOption")
    String sshKeyRSAOption();

    @DefaultMessage("ED25519")
    @Key("sshKeyEd25519Option")
    String sshKeyEd25519Option();

    @DefaultMessage("Passphrase (optional):")
    @Key("passphraseLabel")
    String passphraseLabel();

    @DefaultMessage("Confirm:")
    @Key("passphraseConfirmLabel")
    String passphraseConfirmLabel();

    @DefaultMessage("Key Already Exists")
    @Key("confirmOverwriteKeyCaption")
    String confirmOverwriteKeyCaption();

    @DefaultMessage("An SSH key already exists at {0}. Do you want to overwrite the existing key?")
    @Key("confirmOverwriteKeyMessage")
    String confirmOverwriteKeyMessage(String path);

    @DefaultMessage("Main dictionary language:")
    @Key("spellingLanguageSelectWidgetLabel")
    String spellingLanguageSelectWidgetLabel();

    @DefaultMessage("Help on spelling dictionaries")
    @Key("addHelpButtonLabel")
    String addHelpButtonLabel();

    @DefaultMessage("Downloading dictionaries...")
    @Key("progressDownloadingLabel")
    String progressDownloadingLabel();

    @DefaultMessage("Downloading additional languages...")
    @Key("progressDownloadingLanguagesLabel")
    String progressDownloadingLanguagesLabel();

    @DefaultMessage("Error Downloading Dictionaries")
    @Key("onErrorDownloadingCaption")
    String onErrorDownloadingCaption();

    @DefaultMessage("(Default)")
    @Key("includeDefaultOption")
    String includeDefaultOption();

    @DefaultMessage("Update Dictionaries...")
    @Key("allLanguagesInstalledOption")
    String allLanguagesInstalledOption();

    @DefaultMessage("Install More Languages...")
    @Key("installIndexOption")
    String installIndexOption();

    @DefaultMessage("Add...")
    @Key("buttonAddLabel")
    String buttonAddLabel();

    @DefaultMessage("Remove...")
    @Key("buttonRemoveLabel")
    String buttonRemoveLabel();

    @DefaultMessage("Custom dictionaries:")
    @Key("labelWithHelpText")
    String labelWithHelpText();

    @DefaultMessage("Help on custom spelling dictionaries")
    @Key("labelWithHelpTitle")
    String labelWithHelpTitle();

    @DefaultMessage("Add Custom Dictionary (*.dic)")
    @Key("fileDialogsCaption")
    String fileDialogsCaption();

    @DefaultMessage("Dictionaries (*.dic)")
    @Key("fileDialogsFilter")
    String fileDialogsFilter();

    @DefaultMessage("Adding dictionary...")
    @Key("onProgressAddingLabel")
    String onProgressAddingLabel();

    @DefaultMessage("Confirm Remove")
    @Key("removeDictionaryCaption")
    String removeDictionaryCaption();

    @DefaultMessage("Are you sure you want to remove the {0} custom dictionary?")
    @Key("removeDictionaryMessage")
    String removeDictionaryMessage(String dictionary);

    @DefaultMessage("Removing dictionary...")
    @Key("progressRemoveIndicator")
    String progressRemoveIndicator();

    @DefaultMessage("[Detected output overflow; buffering the next {0} lines of output]")
    @Key("consoleBufferedMessage")
    String consoleBufferedMessage(int bufferSize);

}

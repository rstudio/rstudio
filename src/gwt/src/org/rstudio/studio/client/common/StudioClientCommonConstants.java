/*
 * StudioClientCommonConstants.java
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
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
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "Stop".
     *
     * @return translated "Stop"
     */
    @DefaultMessage("Stop")
    @Key("stopTitle")
    String stopTitle();

    /**
     * Translated "Output".
     *
     * @return translated "Output"
     */
    @DefaultMessage("Output")
    @Key("outputLeftLabel")
    String outputLeftLabel();

    /**
     * Translated "Issues".
     *
     * @return translated "Issues"
     */
    @DefaultMessage("Issues")
    @Key("issuesRightLabel")
    String issuesRightLabel();

    /**
     * Translated "Clear All Breakpoints".
     *
     * @return translated "Clear All Breakpoints"
     */
    @DefaultMessage("Clear All Breakpoints")
    @Key("clearAllBreakpointsCaption")
    String clearAllBreakpointsCaption();

    /**
     * Translated "Hide Traceback".
     *
     * @return translated "Hide Traceback"
     */
    @DefaultMessage("Hide Traceback")
    @Key("hideTracebackText")
    String hideTracebackText();

    /**
     * Translated "Show Traceback".
     *
     * @return translated "Show Traceback"
     */
    @DefaultMessage("Show Traceback")
    @Key("showTracebackText")
    String showTracebackText();

    /**
     * Translated "Converting Theme".
     *
     * @return translated "Converting Theme"
     */
    @DefaultMessage("Converting Theme")
    @Key("convertingThemeProgressCaption")
    String convertingThemeProgressCaption();

    /**
     * Translated "Preparing Import from Mongo DB".
     *
     * @return translated "Preparing Import from Mongo DB"
     */
    @DefaultMessage("Preparing Import from Mongo DB")
    @Key("withDataImportMongoProgressCaption")
    String withDataImportMongoProgressCaption();

    /**
     * Translated "Using testthat".
     *
     * @return translated "Using testthat"
     */
    @DefaultMessage("Using testthat")
    @Key("testthatMessage")
    String testthatMessage();

    /**
     * Translated "Using shinytest".
     *
     * @return translated "Using shinytest"
     */
    @DefaultMessage("Using shinytest")
    @Key("shinytestMessage")
    String shinytestMessage();

    /**
     * Translated "Preparing Tests".
     *
     * @return translated "Preparing Tests"
     */
    @DefaultMessage("Preparing Tests")
    @Key("preparingTestsProgressCaption")
    String preparingTestsProgressCaption();

    /**
     * Translated "Testing Tools".
     *
     * @return translated "Testing Tools"
     */
    @DefaultMessage("Testing Tools")
    @Key("testingToolsContext")
    String testingToolsContext();

    /**
     * Translated "No Files Selected".
     *
     * @return translated "No Files Selected"
     */
    @DefaultMessage("No Files Selected")
    @Key("noFilesSelectedCaption")
    String noFilesSelectedCaption();

    /**
     * Translated "Please select one or more files to export.".
     *
     * @return translated "Please select one or more files to export."
     */
    @DefaultMessage("Please select one or more files to export.")
    @Key("noFilesSelectedMessage")
    String noFilesSelectedMessage();

    /**
     * Translated "Download".
     *
     * @return translated "Download"
     */
    @DefaultMessage("Download")
    @Key("downloadButtonCaption")
    String downloadButtonCaption();

    /**
     * Translated "R Code Browser".
     *
     * @return translated "R Code Browser"
     */
    @DefaultMessage("R Code Browser")
    @Key("rCodeBrowserLabel")
    String rCodeBrowserLabel();

    /**
     * Translated "R Data Frame".
     *
     * @return translated "R Data Frame"
     */
    @DefaultMessage("R Data Frame")
    @Key("rDataFrameLabel")
    String rDataFrameLabel();

    /**
     * Translated "Public Folder".
     *
     * @return translated "Public Folder"
     */
    @DefaultMessage("Public Folder")
    @Key("publicFolderDesc")
    String publicFolderDesc();

    /**
     * Translated "Folder".
     *
     * @return translated "Folder"
     */
    @DefaultMessage("Folder")
    @Key("folderDesc")
    String folderDesc();

    /**
     * Translated "Text file".
     *
     * @return translated "Text file"
     */
    @DefaultMessage("Text file")
    @Key("textFileDesc")
    String textFileDesc();

    /**
     * Translated "Image file".
     *
     * @return translated "Image file"
     */
    @DefaultMessage("Image file")
    @Key("imageFileDesc")
    String imageFileDesc();

    /**
     * Translated "Parent folder".
     *
     * @return translated "Parent folder"
     */
    @DefaultMessage("Parent folder")
    @Key("parentFolderDesc")
    String parentFolderDesc();

    /**
     * Translated "Explore object".
     *
     * @return translated "Explore object"
     */
    @DefaultMessage("Explore object")
    @Key("exploreObjectDesc")
    String exploreObjectDesc();

    /**
     * Translated "R source viewer".
     *
     * @return translated "R source viewer"
     */
    @DefaultMessage("R source viewer")
    @Key("rSourceViewerDesc")
    String rSourceViewerDesc();

    /**
     * Translated "Profiler".
     *
     * @return translated "Profiler"
     */
    @DefaultMessage("Profiler")
    @Key("profilerDesc")
    String profilerDesc();

    /**
     * Translated "R Script".
     *
     * @return translated "R Script"
     */
    @DefaultMessage("R Script")
    @Key("rScriptLabel")
    String rScriptLabel();

    /**
     * Translated "Rd File".
     *
     * @return translated "Rd File"
     */
    @DefaultMessage("Rd File")
    @Key("rdFile")
    String rdFile();

    /**
     * Translated "NAMESPACE".
     *
     * @return translated "NAMESPACE"
     */
    @DefaultMessage("NAMESPACE")
    @Key("namespaceLabel")
    String namespaceLabel();

    /**
     * Translated "R History".
     *
     * @return translated "R History"
     */
    @DefaultMessage("R History")
    @Key("rHistoryLabel")
    String rHistoryLabel();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultMessage("R Markdown")
    @Key("rMarkdownLabel")
    String rMarkdownLabel();

    /**
     * Translated "R Notebook".
     *
     * @return translated "R Notebook"
     */
    @DefaultMessage("R Notebook")
    @Key("rNotebookLabel")
    String rNotebookLabel();

    /**
     * Translated "Markdown".
     *
     * @return translated "Markdown"
     */
    @DefaultMessage("Markdown")
    @Key("markdownLabel")
    String markdownLabel();

    /**
     * Translated "File Download Error".
     *
     * @return translated "File Download Error"
     */
    @DefaultMessage("File Download Error")
    @Key("fileDownloadErrorCaption")
    String fileDownloadErrorCaption();

    /**
     * Translated "Unable to show file because file downloads are restricted on this server.\n".
     *
     * @return translated "Unable to show file because file downloads are restricted on this server.\n"
     */
    @DefaultMessage("Unable to show file because file downloads are restricted on this server.\\n")
    @Key("fileDownloadErrorMessage")
    String fileDownloadErrorMessage();

    /**
     * Translated "Open".
     *
     * @return translated "Open"
     */
    @DefaultMessage("Open")
    @Key("openLabel")
    String openLabel();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    @DefaultMessage("Save")
    @Key("saveLabel")
    String saveLabel();

    /**
     * Translated "Choose".
     *
     * @return translated "Choose"
     */
    @DefaultMessage("Choose")
    @Key("chooseLabel")
    String chooseLabel();

    /**
     * Translated "and ".
     *
     * @return translated "and "
     */
    @DefaultMessage("and ")
    @Key("andText")
    String andText();

    /**
     * Translated "Typeset LaTeX into PDF using:".
     *
     * @return translated "Typeset LaTeX into PDF using:"
     */
    @DefaultMessage("Typeset LaTeX into PDF using:")
    @Key("typesetLatexLabel")
    String typesetLatexLabel();

    /**
     * Translated "Help on customizing LaTeX options".
     *
     * @return translated "Help on customizing LaTeX options"
     */
    @DefaultMessage("Help on customizing LaTeX options")
    @Key("latexHelpLinkLabel")
    String latexHelpLinkLabel();

    /**
     * Translated "Error Setting CRAN Mirror".
     *
     * @return translated "Error Setting CRAN Mirror"
     */
    @DefaultMessage("Error Setting CRAN Mirror")
    @Key("errorSettingCranMirror")
    String errorSettingCranMirror();

    /**
     * Translated "The CRAN mirror could not be changed.".
     *
     * @return translated "The CRAN mirror could not be changed."
     */
    @DefaultMessage("The CRAN mirror could not be changed.")
    @Key("cranMirrorCannotChange")
    String cranMirrorCannotChange();

    /**
     * Translated "Insert Roxygen Skeleton".
     *
     * @return translated "Insert Roxygen Skeleton"
     */
    @DefaultMessage("Insert Roxygen Skeleton")
    @Key("insertRoxygenSkeletonMessage")
    String insertRoxygenSkeletonMessage();

    /**
     * Translated "Unable to insert skeleton (the cursor is not currently inside an R function definition).".
     *
     * @return translated "Unable to insert skeleton (the cursor is not currently inside an R function definition)."
     */
    @DefaultMessage("Unable to insert skeleton (the cursor is not currently inside an R function definition).")
    @Key("unableToInsertSkeletonMessage")
    String unableToInsertSkeletonMessage();

    /**
     * Translated "Cannot automatically update roxygen blocks that are not self-contained.".
     *
     * @return translated "Cannot automatically update roxygen blocks that are not self-contained."
     */
    @DefaultMessage("Cannot automatically update roxygen blocks that are not self-contained.")
    @Key("cannotUpdateRoxygenMessage")
    String cannotUpdateRoxygenMessage();

    /**
     * Translated "Confirm Change".
     *
     * @return translated "Confirm Change"
     */
    @DefaultMessage("Confirm Change")
    @Key("confirmChangeCaption")
    String confirmChangeCaption();


    /**
     * Translated "The {0} package is required for {1} weaving, however it is not currently installed. You should ensure that {0} is installed prior to compiling a PDF.\n\nAre you sure you want to change this option?".
     *
     * @return translated "The {0} package is required for {1} weaving, however it is not currently installed. You should ensure that {0} is installed prior to compiling a PDF.\n\nAre you sure you want to change this option?"
     */
    @DefaultMessage("The {0} package is required for {1} weaving, however it is not currently installed. You should ensure that {0} is installed prior to compiling a PDF.\\n\\nAre you sure you want to change this option?")
    @Key("packageRequiredMessage")
    String packageRequiredMessage(String packageName, String name);

    /**
     * Translated "Upload Error Occurred".
     *
     * @return translated "Upload Error Occurred"
     */
    @DefaultMessage("Upload Error Occurred")
    @Key("uploadErrorTitle")
    String uploadErrorTitle();

    /**
     * Translated "Unable to continue ".
     *
     * @return translated "Unable to continue (another publish is currently running)"
     */
    @DefaultMessage("Unable to continue (another publish is currently running)")
    @Key("unableToContinueMessage")
    String unableToContinueMessage();

    /**
     * Translated "Uploading document to RPubs...".
     *
     * @return translated "Uploading document to RPubs..."
     */
    @DefaultMessage("Uploading document to RPubs...")
    @Key("uploadingDocumentRPubsMessage")
    String uploadingDocumentRPubsMessage();

    /**
     * Translated "Publish to RPubs".
     *
     * @return translated "Publish to RPubs"
     */
    @DefaultMessage("Publish to RPubs")
    @Key("publishToRPubs")
    String publishToRPubs();

    /**
     * Translated "RPubs is a free service from RStudio for sharing documents on the web. Click Publish to get started.".
     *
     * @return translated "RPubs is a free service from RStudio for sharing documents on the web. Click Publish to get started."
     */
    @DefaultMessage("RPubs is a free service from RStudio for sharing documents on the web. Click Publish to get started.")
    @Key("rPubsServiceMessage")
    String rPubsServiceMessage();

    /**
     * Translated "This document has already been published on RPubs. You can ".
     *
     * @return translated "This document has already been published on RPubs. You can "
     */
    @DefaultMessage("This document has already been published on RPubs. You can ")
    @Key("alreadyPublishedRPubs")
    String alreadyPublishedRPubs();


    /**
     * Translated "IMPORTANT: All documents published to RPubs are publicly visible.".
     *
     * @return translated "IMPORTANT: All documents published to RPubs are publicly visible."
     */
    @DefaultMessage("IMPORTANT: All documents published to RPubs are publicly visible.")
    @Key("importantMessage")
    String importantMessage();

    /**
     * Translated "You should only publish documents that you wish to share publicly.".
     *
     * @return translated "You should only publish documents that you wish to share publicly."
     */
    @DefaultMessage("You should only publish documents that you wish to share publicly.")
    @Key("publishMessage")
    String publishMessage();

    /**
     * Translated "<strong>{0}</strong> {1}".
     *
     * @return translated "<strong>{0}</strong> {1}"
     */
    @DefaultMessage("<strong>{0}</strong> {1}")
    @Key("importantRPubsMessage")
    String importantRPubsMessage(String importantMessage, String publishMessage);


    /**
     * Translated "Publish".
     *
     * @return translated "Publish"
     */
    @DefaultMessage("Publish")
    @Key("publishButtonTitle")
    String publishButtonTitle();

    /**
     * Translated "Update Existing".
     *
     * @return translated "Update Existing"
     */
    @DefaultMessage("Update Existing")
    @Key("updateExistingButtonTitle")
    String updateExistingButtonTitle();

    /**
     * Translated "Create New".
     *
     * @return translated "Create New"
     */
    @DefaultMessage("Create New")
    @Key("createNewButtonTitle")
    String createNewButtonTitle();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("cancelTitle")
    String cancelTitle();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    @Key("okTitle")
    String okTitle();

    /**
     * Translated "Using Keyring".
     *
     * @return translated "Using Keyring"
     */
    @DefaultMessage("Using Keyring")
    @Key("usingKeyringCaption")
    String usingKeyringCaption();

    /**
     * Translated "Keyring is an R package that provides access to the operating systems credential store to allow you to remember, securely, passwords and secrets. ".
     *
     * @return translated "Keyring is an R package that provides access to the operating systems credential store to allow you to remember, securely, passwords and secrets. "
     */
    @DefaultMessage("Keyring is an R package that provides access to the operating systems credential store to allow you to remember, securely, passwords and secrets. ")
    @Key("keyringDesc")
    String keyringDesc();

    /**
     * Translated "Would you like to install keyring?".
     *
     * @return translated "Would you like to install keyring?"
     */
    @DefaultMessage("Would you like to install keyring?")
    @Key("installKeyringMessage")
    String installKeyringMessage();

    /**
     * Translated "Keyring".
     *
     * @return translated "Keyring"
     */
    @DefaultMessage("Keyring")
    @Key("keyringCaption")
    String keyringCaption();

    /**
     * Translated "Install".
     *
     * @return translated "Install"
     */
    @DefaultMessage("Install")
    @Key("installLabel")
    String installLabel();

    /**
     * Translated "You must enter a value.".
     *
     * @return translated "You must enter a value."
     */
    @DefaultMessage("You must enter a value.")
    @Key("enterValueMessage")
    String enterValueMessage();

    /**
     * Translated "Too much console output to announce.".
     *
     * @return translated "Too much console output to announce."
     */
    @DefaultMessage("Too much console output to announce.")
    @Key("consoleOutputOverLimitMessage")
    String consoleOutputOverLimitMessage();

    /**
     * Translated "Error: {0}\n".
     *
     * @return translated "Error: {0}\n"
     */
    @DefaultMessage("Error: {0}\\n")
    @Key("consoleWriteError")
    String consoleWriteError(String error);

    /**
     * Translated "Line ".
     *
     * @return translated "Line "
     */
    @DefaultMessage("Line ")
    @Key("lineText")
    String lineText();

    /**
     * Translated "View error or warning within the log file".
     *
     * @return translated "View error or warning within the log file"
     */
    @DefaultMessage("View error or warning within the log file")
    @Key("viewErrorLogfile")
    String viewErrorLogfile();


    /**
     * Translated "Syncing...".
     *
     * @return translated "Syncing..."
     */
    @DefaultMessage("Syncing...")
    @Key("getSyncProgressMessage")
    String getSyncProgressMessage();

    /**
     * Translated "; Page ".
     *
     * @return translated "; Page ."
     */
    @DefaultMessage("; Page ")
    @Key("pdfPageText")
    String pdfPageText();

    /**
     * Translated "[From Click]".
     *
     * @return translated "[From Click]"
     */
    @DefaultMessage("[From Click]")
    @Key("pdfFromClickText")
    String pdfFromClickText();

    /**
     * Translated "Password".
     *
     * @return translated "Password"
     */
    @DefaultMessage("Password")
    @Key("passwordTitle")
    String passwordTitle();

    /**
     * Translated "Username".
     *
     * @return translated "Username"
     */
    @DefaultMessage("Username")
    @Key("usernameTitle")
    String usernameTitle();

    /**
     * Translated "Press {0} to copy the key to the clipboard".
     *
     * @return translated "Press {0} to copy the key to the clipboard"
     */
    @DefaultMessage("Press {0} to copy the key to the clipboard")
    @Key("pressLabel")
    String pressLabel(String cmdText);

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    @Key("closeButtonLabel")
    String closeButtonLabel();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    @DefaultMessage("(None)")
    @Key("noneLabel")
    String noneLabel();

    /**
     * Translated "Getting ignored files for path...".
     *
     * @return translated "Getting ignored files for path..."
     */
    @DefaultMessage("Getting ignored files for path...")
    @Key("gettingIgnoredFilesProgressMessage")
    String gettingIgnoredFilesProgressMessage();

    /**
     * Translated "Setting ignored files for path...".
     *
     * @return translated "Setting ignored files for path..."
     */
    @DefaultMessage("Setting ignored files for path...")
    @Key("settingIgnoredFilesProgressMessage")
    String settingIgnoredFilesProgressMessage();

    /**
     * Translated "Error: Multiple Directories".
     *
     * @return translated "Error: Multiple Directories"
     */
    @DefaultMessage("Error: Multiple Directories")
    @Key("multipleDirectoriesCaption")
    String multipleDirectoriesCaption();

    /**
     * Translated "The selected files are not all within the same directory you can only ignore multiple files in one operation if they are located within the same directory).".
     *
     * @return translated "The selected files are not all within the same directory you can only ignore multiple files in one operation if they are located within the same directory)."
     */
    @DefaultMessage("The selected files are not all within the same directory (you can only ignore multiple files in one operation if they are located within the same directory).")
    @Key("selectedFilesNotInSameDirectoryMessage")
    String selectedFilesNotInSameDirectoryMessage();

    /**
     * Translated "Directory:".
     *
     * @return translated "Directory:"
     */
    @DefaultMessage("Directory:")
    @Key("directoryLabel")
    String directoryLabel();

    /**
     * Translated "Ignored files".
     *
     * @return translated "Ignored files"
     */
    @DefaultMessage("Ignored files")
    @Key("ignoredFilesLabel")
    String ignoredFilesLabel();

    /**
     * Translated "Ignore:".
     *
     * @return translated "Ignore:"
     */
    @DefaultMessage("Ignore:")
    @Key("ignoreCaption")
    String ignoreCaption();

    /**
     * Translated "Specifying ignored files".
     *
     * @return translated "Specifying ignored files"
     */
    @DefaultMessage("Specifying ignored files")
    @Key("specifyingIgnoredFilesHelpCaption")
    String specifyingIgnoredFilesHelpCaption();

    /**
     * Translated "Loading file contents".
     *
     * @return translated "Loading file contents"
     */
    @DefaultMessage("Loading file contents")
    @Key("loadingFileContentsProgressCaption")
    String loadingFileContentsProgressCaption();

    /**
     * Translated "Save As".
     *
     * @return translated "Save As"
     */
    @DefaultMessage("Save As")
    @Key("saveAsText")
    String saveAsText();

    /**
     * Translated "Save File - {0}".
     *
     * @return translated "Save File - {0}"
     */
    @DefaultMessage("Save File - {0}")
    @Key("saveFileCaption")
    String saveFileCaption(String name);

    /**
     * Translated "Saving file...".
     *
     * @return translated "Saving file..."
     */
    @DefaultMessage("Saving file...")
    @Key("savingFileProgressCaption")
    String savingFileProgressCaption();

    /**
     * Translated "View File Tab".
     *
     * @return translated "View File Tab"
     */
    @DefaultMessage("View File Tab")
    @Key("viewFileTabLabel")
    String viewFileTabLabel();

    /**
     * Translated "at".
     *
     * @return translated "at"
     */
    @DefaultMessage("at")
    @Key("atText")
    String atText();

    /**
     * Translated "This is a warning!".
     *
     * @return translated "This is a warning!"
     */
    @DefaultMessage("This is a warning!")
    @Key("warningMessage")
    String warningMessage();

    /**
     * Translated "R Presentation".
     *
     * @return translated "R Presentation"
     */
    @DefaultMessage("R Presentation")
    @Key("rPresentationLabel")
    String rPresentationLabel();

    /**
     * Translated "Source Marker Item Table".
     *
     * @return translated "Source Marker Item Table"
     */
    @DefaultMessage("Source Marker Item Table")
    @Key("sourceMarkerItemTableList")
    String sourceMarkerItemTableList();

    /**
     * Translated "Preview".
     *
     * @return translated "Preview"
     */
    @DefaultMessage("Preview")
    @Key("previewButtonText")
    String previewButtonText();

    /**
     * Translated "Generic Content".
     *
     * @return translated "Generic Content"
     */
    @DefaultMessage("Generic Content")
    @Key("genericContentLabel")
    String genericContentLabel();

    /**
     * Translated "Object Explorer".
     *
     * @return translated "Object Explorer"
     */
    @DefaultMessage("Object Explorer")
    @Key("objectExplorerLabel")
    String objectExplorerLabel();

    /**
     * Translated "R Profiler".
     *
     * @return translated "R Profiler"
     */
    @DefaultMessage("R Profiler")
    @Key("rProfilerLabel")
    String rProfilerLabel();

    /**
     * Translated "Check".
     *
     * @return translated "Check"
     */
    @DefaultMessage("Check")
    @Key("checkPreviewButtonText")
    String checkPreviewButtonText();

    /**
     * Translated "Weave Rnw files using:".
     *
     * @return translated "Weave Rnw files using:"
     */
    @DefaultMessage("Weave Rnw files using:")
    @Key("weaveRnwLabel")
    String weaveRnwLabel();

    /**
     * Translated "Help on weaving Rnw files".
     *
     * @return translated "Help on weaving Rnw files"
     */
    @DefaultMessage("Help on weaving Rnw files")
    @Key("weaveRnwHelpTitle")
    String weaveRnwHelpTitle();

    /**
     * Translated "SSH key:".
     *
     * @return translated "SSH key:"
     */
    @DefaultMessage("SSH key:")
    @Key("sshRSAKeyFormLabel")
    String sshRSAKeyFormLabel();

    /**
     * Translated "View public key".
     *
     * @return translated "View public key"
     */
    @DefaultMessage("View public key")
    @Key("viewPublicKeyCaption")
    String viewPublicKeyCaption();

    /**
     * Translated "Create SSH Key...".
     *
     * @return translated "Create SSH Key..."
     */
    @DefaultMessage("Create SSH Key...")
    @Key("createRSAKeyButtonLabel")
    String createRSAKeyButtonLabel();

    /**
     * Translated "Reading public key...".
     *
     * @return translated "Reading public key..."
     */
    @DefaultMessage("Reading public key...")
    @Key("readingPublicKeyProgressCaption")
    String readingPublicKeyProgressCaption();

    /**
     * Translated "Are you sure you want to remove all the breakpoints in this project?".
     *
     * @return translated "Are you sure you want to remove all the breakpoints in this project?"
     */
    @DefaultMessage("Are you sure you want to remove all the breakpoints in this project?")
    @Key("clearAllBreakpointsMessage")
    String clearAllBreakpointsMessage();

    /**
     * Translated "{0}{1}".
     *
     * @return translated "{0}{1}"
     */
    @DefaultMessage("{0}{1}")
    @Key("functionNameText")
    String functionNameText(String functionName, String source);

    /**
     * Translated "{0}".
     *
     * @return translated "{0}"
     */
    @DefaultMessage("{0}")
    @Key("showFileExportLabel")
    String showFileExportLabel(String description);

    /**
     * Translated "Publishing Accounts".
     *
     * @return translated "Publishing Accounts"
     */
    @DefaultMessage("Publishing Accounts")
    @Key("accountListLabel")
    String accountListLabel();

    /**
     * Translated "Connect...".
     *
     * @return translated "Connect..."
     */
    @DefaultMessage("Connect...")
    @Key("connectButtonLabel")
    String connectButtonLabel();

    /**
     * Translated "Reconnect...".
     *
     * @return translated "Reconnect..."
     */
    @DefaultMessage("Reconnect...")
    @Key("reconnectButtonLabel")
    String reconnectButtonLabel();

    /**
     * Translated "Disconnect".
     *
     * @return translated "Disconnect"
     */
    @DefaultMessage("Disconnect")
    @Key("disconnectButtonLabel")
    String disconnectButtonLabel();

    /**
     * Translated "Account records appear to exist, but cannot be viewed because a ".
     *
     * @return translated "Account records appear to exist, but cannot be viewed because a "
     */
    @DefaultMessage("Account records appear to exist, but cannot be viewed because a ")
    @Key("missingPkgPanelMessage")
    String missingPkgPanelMessage();

    /**
     * Translated "required package is not installed.".
     *
     * @return translated "required package is not installed."
     */
    @DefaultMessage("required package is not installed.")
    @Key("missingPkgRequiredMessage")
    String missingPkgRequiredMessage();

    /**
     * Translated "Install Missing Packages".
     *
     * @return translated "Install Missing Packages"
     */
    @DefaultMessage("Install Missing Packages")
    @Key("installPkgsMessage")
    String installPkgsMessage();

    /**
     * Translated "Viewing publish accounts".
     *
     * @return translated "Viewing publish accounts"
     */
    @DefaultMessage("Viewing publish accounts")
    @Key("withRSConnectLabel")
    String withRSConnectLabel();

    /**
     * Translated "Enable publishing to RStudio Connect".
     *
     * @return translated "Enable publishing to RStudio Connect"
     */
    @DefaultMessage("Enable publishing to RStudio Connect")
    @Key("chkEnableRSConnectLabel")
    String chkEnableRSConnectLabel();

    /**
     * Translated "Information about RStudio Connect".
     *
     * @return translated "Information about RStudio Connect"
     */
    @DefaultMessage("Information about RStudio Connect")
    @Key("checkBoxWithHelpTitle")
    String checkBoxWithHelpTitle();

    /**
     * Translated "Settings".
     *
     * @return translated "Settings"
     */
    @DefaultMessage("Settings")
    @Key("settingsHeaderLabel")
    String settingsHeaderLabel();

    /**
     * Translated "Enable publishing documents, apps, and APIs".
     *
     * @return translated "Enable publishing documents, apps, and APIs"
     */
    @DefaultMessage("Enable publishing documents, apps, and APIs")
    @Key("chkEnablePublishingLabel")
    String chkEnablePublishingLabel();

    /**
     * Translated "Show diagnostic information when publishing".
     *
     * @return translated "Show diagnostic information when publishing"
     */
    @DefaultMessage("Show diagnostic information when publishing")
    @Key("showPublishDiagnosticsLabel")
    String showPublishDiagnosticsLabel();

    /**
     * Translated "SSL Certificates".
     *
     * @return translated "SSL Certificates"
     */
    @DefaultMessage("SSL Certificates")
    @Key("sSLCertificatesHeaderLabel")
    String sSLCertificatesHeaderLabel();

    /**
     * Translated "Check SSL certificates when publishing".
     *
     * @return translated "Check SSL certificates when publishing"
     */
    @DefaultMessage("Check SSL certificates when publishing")
    @Key("publishCheckCertificatesLabel")
    String publishCheckCertificatesLabel();

    /**
     * Translated "Check SSL certificates when publishing".
     *
     * @return translated "Check SSL certificates when publishing"
     */
    @DefaultMessage("Use custom CA bundle")
    @Key("usePublishCaBundleLabel")
    String usePublishCaBundleLabel();

    /**
     * Translated "(none)".
     *
     * @return translated "(none)"
     */
    @DefaultMessage("(none)")
    @Key("caBundlePath")
    String caBundlePath();

    /**
     * Translated "Troubleshooting Deployments".
     *
     * @return translated "Troubleshooting Deployments"
     */
    @DefaultMessage("Troubleshooting Deployments")
    @Key("helpLinkTroubleshooting")
    String helpLinkTroubleshooting();

    /**
     * Translated "Publishing".
     *
     * @return translated "Publishing"
     */
    @DefaultMessage("Publishing")
    @Key("publishingPaneHeader")
    String publishingPaneHeader();

    /**
     * Translated "Error Disconnecting Account".
     *
     * @return translated "Error Disconnecting Account"
     */
    @DefaultMessage("Error Disconnecting Account")
    @Key("showErrorCaption")
    String showErrorCaption();

    /**
     * Translated "Please select an account to disconnect.".
     *
     * @return translated "Please select an account to disconnect."
     */
    @DefaultMessage("Please select an account to disconnect.")
    @Key("showErrorMessage")
    String showErrorMessage();

    /**
     * Translated "Confirm Remove Account".
     *
     * @return translated "Confirm Remove Account"
     */
    @DefaultMessage("Confirm Remove Account")
    @Key("removeAccountGlobalDisplay")
    String removeAccountGlobalDisplay();


    /**
     * Translated "Disconnect Account".
     *
     * @return translated "Disconnect Account"
     */
    @DefaultMessage("Disconnect Account")
    @Key("onConfirmDisconnectYesLabel")
    String onConfirmDisconnectYesLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("onConfirmDisconnectNoLabel")
    String onConfirmDisconnectNoLabel();

    /**
     * Translated "Error Disconnecting Account".
     *
     * @return translated "Error Disconnecting Account"
     */
    @DefaultMessage("Error Disconnecting Account")
    @Key("disconnectingErrorMessage")
    String disconnectingErrorMessage();

    /**
     * Translated "Connecting a publishing account".
     *
     * @return translated "Connecting a publishing account"
     */
    @DefaultMessage("Connecting a publishing account")
    @Key("getAccountCountLabel")
    String getAccountCountLabel();

    /**
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    @DefaultMessage("Connect Account")
    @Key("connectAccountCaption")
    String connectAccountCaption();

    /**
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    @DefaultMessage("Connect Account")
    @Key("connectAccountOkCaption")
    String connectAccountOkCaption();

    /**
     * Translated "Connect Publishing Account".
     *
     * @return translated "Connect Publishing Account"
     */
    @DefaultMessage("Connect Publishing Account")
    @Key("newRSConnectAccountPageTitle")
    String newRSConnectAccountPageTitle();

    /**
     * Translated "Pick an account".
     *
     * @return translated "Pick an account"
     */
    @DefaultMessage("Pick an account")
    @Key("newRSConnectAccountPageSubTitle")
    String newRSConnectAccountPageSubTitle();

    /**
     * Translated "Connect Publishing Account".
     *
     * @return translated "Connect Publishing Account"
     */
    @DefaultMessage("Connect Publishing Account")
    @Key("newRSConnectAccountPageCaption")
    String newRSConnectAccountPageCaption();

    /**
     * Translated "Choose Account Type".
     *
     * @return translated "Choose Account Type"
     */
    @DefaultMessage("Choose Account Type")
    @Key("wizardNavigationPageTitle")
    String wizardNavigationPageTitle();

    /**
     * Translated "Choose Account Type".
     *
     * @return translated "Choose Account Type"
     */
    @DefaultMessage("Choose Account Type")
    @Key("wizardNavigationPageSubTitle")
    String wizardNavigationPageSubTitle();

    /**
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    @DefaultMessage("Connect Account")
    @Key("wizardNavigationPageCaption")
    String wizardNavigationPageCaption();

    /**
     * Translated "RStudio Connect is a server product from RStudio ".
     *
     * @return translated "RStudio Connect is a server product from RStudio "
     */
    @DefaultMessage("RStudio Connect is a server product from RStudio ")
    @Key("serviceDescription")
    String serviceDescription();

    /**
     * Translated "for secure sharing of applications, reports, plots, and APIs.".
     *
     * @return translated "for secure sharing of applications, reports, plots, and APIs."
     */
    @DefaultMessage("for secure sharing of applications, reports, plots, and APIs.")
    @Key("serviceMessageDescription")
    String serviceMessageDescription();

    /**
     * Translated "A cloud service run by RStudio. Publish Shiny applications ".
     *
     * @return translated "A cloud service run by RStudio. Publish Shiny applications "
     */
    @DefaultMessage("A cloud service run by RStudio. Publish Shiny applications ")
    @Key("newRSConnectCloudPageSubTitle")
    String newRSConnectCloudPageSubTitle();

    /**
     * Translated "and interactive documents to the Internet.".
     *
     * @return translated "and interactive documents to the Internet."
     */
    @DefaultMessage("and interactive documents to the Internet.")
    @Key("newRSConnectCloudPageSub")
    String newRSConnectCloudPageSub();

    /**
     * Translated "Connect ShinyApps.io Account".
     *
     * @return translated "Connect ShinyApps.io Account"
     */
    @DefaultMessage("Connect ShinyApps.io Account")
    @Key("newRSConnectCloudPageCaption")
    String newRSConnectCloudPageCaption();

    /**
     * Translated "Converting Theme".
     *
     * @return translated "Converting Theme"
     */
    @DefaultMessage("Converting Theme")
    @Key("withThemesCaption")
    String withThemesCaption();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultMessage("R Markdown")
    @Key("withRMarkdownCaption")
    String withRMarkdownCaption();

    /**
     * Translated "Install Shiny Package".
     *
     * @return translated "Install Shiny Package"
     */
    @DefaultMessage("Install Shiny Package")
    @Key("installShinyCaption")
    String installShinyCaption();

    /**
     * Translated "requires installation of an updated version ".
     *
     * @return translated "requires installation of an updated version "
     */
    @DefaultMessage("{0} requires installation of an updated version of the shiny package.\n\nDo you want to install shiny now?")
    @Key("installShinyUserAction")
    String installShinyUserAction(String userAction);

    /**
     * Translated "Checking installed packages".
     *
     * @return translated "Checking installed packages"
     */
    @DefaultMessage("Checking installed packages")
    @Key("installPkgsCaption")
    String installPkgsCaption();

    /**
     * Translated "Checking installed packages".
     *
     * @return translated "Checking installed packages"
     */
    @DefaultMessage("Checking installed packages")
    @Key("withShinyAddinsCaption")
    String withShinyAddinsCaption();

    /**
     * Translated "Checking installed packages".
     *
     * @return translated "Checking installed packages"
     */
    @DefaultMessage("Executing addins")
    @Key("withShinyAddinsUserAction")
    String withShinyAddinsUserAction();

    /**
     * Translated "Preparing Import from CSV".
     *
     * @return translated "Preparing Import from CSV"
     */
    @DefaultMessage("Preparing Import from CSV")
    @Key("withDataImportCSVCaption")
    String withDataImportCSVCaption();

    /**
     * Translated "Preparing Import from SPSS, SAS and Stata".
     *
     * @return translated "Preparing Import from SPSS, SAS and Stata"
     */
    @DefaultMessage("Preparing Import from SPSS, SAS and Stata")
    @Key("withDataImportSAV")
    String withDataImportSAV();

    /**
     * Translated "Preparing Import from Excel".
     *
     * @return translated "Preparing Import from Excel"
     */
    @DefaultMessage("Preparing Import from Excel")
    @Key("withDataImportXLS")
    String withDataImportXLS();

    /**
     * Translated "Preparing Import from XML".
     *
     * @return translated "Preparing Import from XML"
     */
    @DefaultMessage("Preparing Import from XML")
    @Key("withDataImportXML")
    String withDataImportXML();

    /**
     * Translated "Preparing Import from JSON".
     *
     * @return translated "Preparing Import from JSON"
     */
    @DefaultMessage("Preparing Import from JSON")
    @Key("withDataImportJSON")
    String withDataImportJSON();

    /**
     * Translated "Preparing Import from JDBC".
     *
     * @return translated "Preparing Import from JDBC"
     */
    @DefaultMessage("Preparing Import from JDBC")
    @Key("withDataImportJDBC")
    String withDataImportJDBC();

    /**
     * Translated "Preparing Import from ODBC".
     *
     * @return translated "Preparing Import from ODBC"
     */
    @DefaultMessage("Preparing Import from ODBC")
    @Key("withDataImportODBC")
    String withDataImportODBC();

    /**
     * Translated "Preparing Profiler".
     *
     * @return translated "Preparing Profiler"
     */
    @DefaultMessage("Preparing Profiler")
    @Key("withProfvis")
    String withProfvis();

    /**
     * Translated "Preparing Connection".
     *
     * @return translated "Preparing Connection"
     */
    @DefaultMessage("Preparing Connection")
    @Key("withConnectionPackage")
    String withConnectionPackage();

    /**
     * Translated "Database Connectivity".
     *
     * @return translated "Database Connectivity"
     */
    @DefaultMessage("Database Connectivity")
    @Key("withConnectionPackageContext")
    String withConnectionPackageContext();

    /**
     * Translated "Preparing Keyring".
     *
     * @return translated "Preparing Keyring"
     */
    @DefaultMessage("Preparing Keyring")
    @Key("withKeyring")
    String withKeyring();

    /**
     * Translated "Using keyring".
     *
     * @return translated "Using keyring"
     */
    @DefaultMessage("Using keyring")
    @Key("withKeyringUserAction")
    String withKeyringUserAction();

    /**
     * Translated "Preparing ".
     *
     * @return translated "Preparing "
     */
    @DefaultMessage("Preparing ")
    @Key("withOdbc")
    String withOdbc();

    /**
     * Translated "Preparing ".
     *
     * @return translated "Preparing "
     */
    @DefaultMessage("Using ")
    @Key("withOdbcUserAction")
    String withOdbcUserAction();

    /**
     * Translated "Starting tutorial".
     *
     * @return translated "Starting tutorial"
     */
    @DefaultMessage("Starting tutorial")
    @Key("withTutorialDependencies")
    String withTutorialDependencies();

    /**
     * Translated "Starting a tutorial".
     *
     * @return translated "Starting a tutorial"
     */
    @DefaultMessage("Starting a tutorial")
    @Key("withTutorialDependenciesUserAction")
    String withTutorialDependenciesUserAction();

    /**
     * Translated "Using the AGG renderer".
     *
     * @return translated "Using the AGG renderer"
     */
    @DefaultMessage("Using the AGG renderer")
    @Key("withRagg")
    String withRagg();

    /**
     * Translated " is not available\n".
     *
     * @return translated " is not available\n"
     */
    @DefaultMessage(" is not available\n")
    @Key("unsatisfiedVersions")
    String unsatisfiedVersions();

    /**
     * Translated " is required but is available\n".
     *
     * @return translated " is required but is available\n"
     */
    @DefaultMessage(" is required but {0} is available\n")
    @Key("requiredVersion")
    String requiredVersion(String version);

    /**
     * Translated "Packages Not Found".
     *
     * @return translated "Packages Not Found"
     */
    @DefaultMessage("Packages Not Found")
    @Key("packageNotFoundUserAction")
    String packageNotFoundUserAction();

    /**
     * Translated "Required package versions could not be found:\n\n{0}\nCheck that getOption(\"repos\") refers to a CRAN repository that contains the needed package versions.".
     *
     * @return translated "Required package versions could not be found:\n\n{0}\nCheck that getOption(\"repos\") refers to a CRAN repository that contains the needed package versions."
     */
    @DefaultMessage("Required package versions could not be found:\n\n{0}\nCheck that getOption(\\\"repos\\\") refers to a CRAN repository that contains the needed package versions.")
    @Key("packageNotFoundMessage")
    String packageNotFoundMessage(String unsatisfiedVersions);

    /**
     * Translated "Dependency installation failed".
     *
     * @return translated "Dependency installation failed"
     */
    @DefaultMessage("Dependency installation failed")
    @Key("onErrorMessage")
    String onErrorMessage();

    /**
     * Translated "Could not determine available packages".
     *
     * @return translated "Could not determine available packages"
     */
    @DefaultMessage("Could not determine available packages")
    @Key("availablePackageErrorMessage")
    String availablePackageErrorMessage();

    /**
     * Translated "requires an updated version of the {0} package.\n\nDo you want to install this package now?".
     *
     * @return translated "requires an updated version of the {0} package.\n\nDo you want to install this package now?"
     */
    @DefaultMessage("requires an updated version of the {0} package.\n\nDo you want to install this package now?")
    @Key("confirmPackageInstallation")
    String confirmPackageInstallation(String name);


    /**
     * Translated "requires updated versions of the following packages: {0}.\n\nDo you want to install these packages now?".
     *
     * @return translated "requires updated versions of the following packages: {0}.\n\nDo you want to install these packages now?"
     */
    @DefaultMessage("requires updated versions of the following packages: {0}. \n\nDo you want to install these packages now?")
    @Key("updatedVersionMessage")
    String updatedVersionMessage(String dependency);

    /**
     * Translated "Install Required Packages".
     *
     * @return translated "Install Required Packages"
     */
    @DefaultMessage("Install Required Packages")
    @Key("installRequiredCaption")
    String installRequiredCaption();


    /**
     * Translated "Enable".
     *
     * @return translated "Enable"
     */
    @DefaultMessage("Enable")
    @Key("globalDisplayEnable")
    String globalDisplayEnable();

    /**
     * Translated "Disable".
     *
     * @return translated "Disable"
     */
    @DefaultMessage("Disable")
    @Key("globalDisplayDisable")
    String globalDisplayDisable();

    /**
     * Translated "Version Control ".
     *
     * @return translated "Version Control "
     */
    @DefaultMessage("Version Control ")
    @Key("globalDisplayVC")
    String globalDisplayVC();

    /**
     * Translated "You must restart RStudio for this change to take effect.".
     *
     * @return translated "You must restart RStudio for this change to take effect."
     */
    @DefaultMessage("You must restart RStudio for this change to take effect.")
    @Key("globalDisplayVCMessage")
    String globalDisplayVCMessage();


    /**
     * Translated "Terminal executable:".
     *
     * @return translated "Terminal executable:"
     */
    @DefaultMessage("Terminal executable:")
    @Key("terminalPathLabel")
    String terminalPathLabel();

    /**
     * Translated "Public Key".
     *
     * @return translated "Public Key"
     */
    @DefaultMessage("Public Key")
    @Key("showPublicKeyDialogCaption")
    String showPublicKeyDialogCaption();

    /**
     * Translated "Error attempting to read key ''{0}'' ({1})''".
     *
     * @return translated "Error attempting to read key ''{0}'' ({1})''"
     */
    @DefaultMessage("Error attempting to read key ''{0}'' ({1})''")
    @Key("onSSHErrorMessage")
    String onSSHErrorMessage(String keyPath, String errorMessage);

    /**
     * Translated "Using Version Control with RStudio".
     *
     * @return translated "Using Version Control with RStudio"
     */
    @DefaultMessage("Using Version Control with RStudio")
    @Key("vCSHelpLink")
    String vCSHelpLink();

    /**
     * Translated "Create SSH Key".
     *
     * @return translated "Create SSH Key"
     */
    @DefaultMessage("Create SSH Key")
    @Key("createKeyDialogCaption")
    String createKeyDialogCaption();

    /**
     * Translated "Creating SSH Key".
     *
     * @return translated "Creating SSH Key"
     */
    @DefaultMessage("Creating SSH Key...")
    @Key("onProgressLabel")
    String onProgressLabel();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    @DefaultMessage("Create")
    @Key("setOkButtonCaption")
    String setOkButtonCaption();

    /**
     * Translated "Non-Matching Passphrases".
     *
     * @return translated "Non-Matching Passphrases"
     */
    @DefaultMessage("Non-Matching Passphrases")
    @Key("showErrorCaption")
    String showValidateErrorCaption();

    /**
     * Translated "The passphrase and passphrase confirmation do not match.".
     *
     * @return translated "The passphrase and passphrase confirmation do not match."
     */
    @DefaultMessage("The passphrase and passphrase confirmation do not match.")
    @Key("showErrorMessage")
    String showValidateErrorMessage();

    /**
     * Translated "The SSH key will be created at:".
     *
     * @return translated "The SSH key will be created at:"
     */
    @DefaultMessage("The SSH key will be created at:")
    @Key("pathCaption")
    String pathCaption();

    /**
     * Translated "SSH key management".
     *
     * @return translated "SSH key management"
     */
    @DefaultMessage("SSH key management")
    @Key("pathHelpCaption")
    String pathHelpCaption();

    /**
     * Translated "SSH key type: ".
     *
     * @return translated "SSH key type: "
     */
    @DefaultMessage("SSH key type: ")
    @Key("sshKeyTypeLabel")
    String sshKeyTypeLabel();

    /**
     * Translated "RSA-encrypted key"
     *
     * @return translated "RSA-encrypted key"
     */
    @DefaultMessage("RSA-encrypted key")
    @Key("sshKeyRSAOption")
    String sshKeyRSAOption();

    /**
     * Translated "ED25519-encrypted key"
     *
     * @return translated "ED25519-encrypted key"
     */
    @DefaultMessage("ED25519-encrypted key")
    @Key("sshKeyEd25519Option")
    String sshKeyEd25519Option();

    /**
     * Translated "Passphrase (optional):".
     *
     * @return translated "Passphrase (optional):"
     */
    @DefaultMessage("Passphrase (optional):")
    @Key("passphraseLabel")
    String passphraseLabel();

    /**
     * Translated "Confirm:".
     *
     * @return translated "Confirm:"
     */
    @DefaultMessage("Confirm:")
    @Key("passphraseConfirmLabel")
    String passphraseConfirmLabel();

    /**
     * Translated "Key Already Exists".
     *
     * @return translated "Key Already Exists"
     */
    @DefaultMessage("Key Already Exists")
    @Key("confirmOverwriteKeyCaption")
    String confirmOverwriteKeyCaption();

    /**
     * Translated "An SSH key already exists at {0}. Do you want to overwrite the existing key?".
     *
     * @return translated "An SSH key already exists at {0}. Do you want to overwrite the existing key?"
     */
    @DefaultMessage("An SSH key already exists at {0}. Do you want to overwrite the existing key?")
    @Key("confirmOverwriteKeyMessage")
    String confirmOverwriteKeyMessage(String path);

    /**
     * Translated "Main dictionary language:".
     *
     * @return translated "Main dictionary language:"
     */
    @DefaultMessage("Main dictionary language:")
    @Key("spellingLanguageSelectWidgetLabel")
    String spellingLanguageSelectWidgetLabel();

    /**
     * Translated "Help on spelling dictionaries".
     *
     * @return translated "Help on spelling dictionaries"
     */
    @DefaultMessage("Help on spelling dictionaries")
    @Key("addHelpButtonLabel")
    String addHelpButtonLabel();

    /**
     * Translated "Downloading dictionaries...".
     *
     * @return translated "Downloading dictionaries..."
     */
    @DefaultMessage("Downloading dictionaries...")
    @Key("progressDownloadingLabel")
    String progressDownloadingLabel();

    /**
     * Translated "Downloading dictionaries...".
     *
     * @return translated "Downloading dictionaries..."
     */
    @DefaultMessage("Downloading additional languages...")
    @Key("progressDownloadingLanguagesLabel")
    String progressDownloadingLanguagesLabel();

    /**
     * Translated "Error Downloading Dictionaries".
     *
     * @return translated "Error Downloading Dictionaries"
     */
    @DefaultMessage("Error Downloading Dictionaries")
    @Key("onErrorDownloadingCaption")
    String onErrorDownloadingCaption();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    @DefaultMessage("(Default)")
    @Key("includeDefaultOption")
    String includeDefaultOption();

    /**
     * Translated "Update Dictionaries...".
     *
     * @return translated "Update Dictionaries..."
     */
    @DefaultMessage("Update Dictionaries...")
    @Key("allLanguagesInstalledOption")
    String allLanguagesInstalledOption();

    /**
     * Translated "Install More Languages...".
     *
     * @return translated "Install More Languages..."
     */
    @DefaultMessage("Install More Languages...")
    @Key("installIndexOption")
    String installIndexOption();

    /**
     * Translated "Add...".
     *
     * @return translated "Add..."
     */
    @DefaultMessage("Add...")
    @Key("buttonAddLabel")
    String buttonAddLabel();

    /**
     * Translated "Remove...".
     *
     * @return translated "Remove..."
     */
    @DefaultMessage("Remove...")
    @Key("buttonRemoveLabel")
    String buttonRemoveLabel();

    /**
     * Translated "Custom dictionaries:".
     *
     * @return translated "Custom dictionaries:"
     */
    @DefaultMessage("Custom dictionaries:")
    @Key("labelWithHelpText")
    String labelWithHelpText();

    /**
     * Translated "Help on custom spelling dictionaries".
     *
     * @return translated "Help on custom spelling dictionaries"
     */
    @DefaultMessage("Help on custom spelling dictionaries")
    @Key("labelWithHelpTitle")
    String labelWithHelpTitle();

    /**
     * Translated "Add Custom Dictionary (*.dic)".
     *
     * @return translated "Add Custom Dictionary (*.dic)"
     */
    @DefaultMessage("Add Custom Dictionary (*.dic)")
    @Key("fileDialogsCaption")
    String fileDialogsCaption();

    /**
     * Translated "Add Custom Dictionary (*.dic)".
     *
     * @return translated "Add Custom Dictionary (*.dic)"
     */
    @DefaultMessage("Dictionaries (*.dic)")
    @Key("fileDialogsFilter")
    String fileDialogsFilter();

    /**
     * Translated "Adding dictionary...".
     *
     * @return translated "Adding dictionary..."
     */
    @DefaultMessage("Adding dictionary...")
    @Key("onProgressAddingLabel")
    String onProgressAddingLabel();

    /**
     * Translated "Confirm Remove".
     *
     * @return translated "Confirm Remove"
     */
    @DefaultMessage("Confirm Remove")
    @Key("removeDictionaryCaption")
    String removeDictionaryCaption();

    /**
     * Translated "Are you sure you want to remove the {0} custom dictionary?".
     *
     * @return translated "Are you sure you want to remove the {0} custom dictionary?"
     */
    @DefaultMessage("Are you sure you want to remove the {0} custom dictionary?")
    @Key("removeDictionaryMessage")
    String removeDictionaryMessage(String dictionary);

    /**
     * Translated "Removing dictionary...".
     *
     * @return translated "Removing dictionary..."
     */
    @DefaultMessage("Removing dictionary...")
    @Key("progressRemoveIndicator")
    String progressRemoveIndicator();


}

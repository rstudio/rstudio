package org.rstudio.studio.client.common;
/*
 * StudioClientCommonConstants.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
     * Translated "SSH RSA key:".
     *
     * @return translated "SSH RSA key:"
     */
    @DefaultMessage("SSH RSA key:")
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
     * Translated "Create RSA Key...".
     *
     * @return translated "Create RSA Key..."
     */
    @DefaultMessage("Create RSA Key...")
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

}

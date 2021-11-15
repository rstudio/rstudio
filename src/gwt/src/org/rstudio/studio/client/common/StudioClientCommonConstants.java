package org.rstudio.studio.client.common;

public interface StudioClientCommonConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultStringValue("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "Stop".
     *
     * @return translated "Stop"
     */
    @DefaultStringValue("Stop")
    @Key("stopTitle")
    String stopTitle();

    /**
     * Translated "Output".
     *
     * @return translated "Output"
     */
    @DefaultStringValue("Output")
    @Key("outputLeftLabel")
    String outputLeftLabel();

    /**
     * Translated "Issues".
     *
     * @return translated "Issues"
     */
    @DefaultStringValue("Issues")
    @Key("issuesRightLabel")
    String issuesRightLabel();

    /**
     * Translated "Clear All Breakpoints".
     *
     * @return translated "Clear All Breakpoints"
     */
    @DefaultStringValue("Clear All Breakpoints")
    @Key("clearAllBreakpointsCaption")
    String clearAllBreakpointsCaption();

    /**
     * Translated "Are you sure you want to remove all the breakpoints in this ".
     *
     * @return translated "Are you sure you want to remove all the breakpoints in this "
     */
    @DefaultStringValue("Are you sure you want to remove all the breakpoints in this ")
    @Key("clearAllBreakpointsMessage")
    String clearAllBreakpointsMessage();

    /**
     * Translated "project?".
     *
     * @return translated "project?"
     */
    @DefaultStringValue("project?")
    @Key("projectText")
    String projectText();

    /**
     * Translated "Hide Traceback".
     *
     * @return translated "Hide Traceback"
     */
    @DefaultStringValue("Hide Traceback")
    @Key("hideTracebackText")
    String hideTracebackText();

    /**
     * Translated "Show Traceback".
     *
     * @return translated "Show Traceback"
     */
    @DefaultStringValue("Show Traceback")
    @Key("showTracebackText")
    String showTracebackText();

    /**
     * Translated "Converting Theme".
     *
     * @return translated "Converting Theme"
     */
    @DefaultStringValue("Converting Theme")
    @Key("convertingThemeProgressCaption")
    String convertingThemeProgressCaption();

    /**
     * Translated "No dependency record found for package '".
     *
     * @return translated "No dependency record found for package '"
     */
    @DefaultStringValue("No dependency record found for package '")
    @Key("noDependencyRecordFoundLog")
    String noDependencyRecordFoundLog();

    /**
     * Translated "' (required by feature '".
     *
     * @return translated "' (required by feature '"
     */
    @DefaultStringValue("' (required by feature '")
    @Key("requiredByFeatureLog")
    String requiredByFeatureLog();

    /**
     * Translated "Preparing Import from Mongo DB".
     *
     * @return translated "Preparing Import from Mongo DB"
     */
    @DefaultStringValue("Preparing Import from Mongo DB")
    @Key("withDataImportMongoProgressCaption")
    String withDataImportMongoProgressCaption();

    /**
     * Translated "Using testthat".
     *
     * @return translated "Using testthat"
     */
    @DefaultStringValue("Using testthat")
    @Key("testthatMessage")
    String testthatMessage();

    /**
     * Translated "Using shinytest".
     *
     * @return translated "Using shinytest"
     */
    @DefaultStringValue("Using shinytest")
    @Key("shinytestMessage")
    String shinytestMessage();

    /**
     * Translated "Preparing Tests".
     *
     * @return translated "Preparing Tests"
     */
    @DefaultStringValue("Preparing Tests")
    @Key("preparingTestsProgressCaption")
    String preparingTestsProgressCaption();

    /**
     * Translated "Testing Tools".
     *
     * @return translated "Testing Tools"
     */
    @DefaultStringValue("Testing Tools")
    @Key("testingToolsContext")
    String testingToolsContext();

    /**
     * Translated "No Files Selected".
     *
     * @return translated "No Files Selected"
     */
    @DefaultStringValue("No Files Selected")
    @Key("noFilesSelectedCaption")
    String noFilesSelectedCaption();

    /**
     * Translated "Please select one or more files to export.".
     *
     * @return translated "Please select one or more files to export."
     */
    @DefaultStringValue("Please select one or more files to export.")
    @Key("noFilesSelectedMessage")
    String noFilesSelectedMessage();

    /**
     * Translated "The ".
     *
     * @return translated "The "
     */
    @DefaultStringValue("The ")
    @Key("theText")
    String theText();

    /**
     * Translated "will be downloaded to your ".
     *
     * @return translated "will be downloaded to your "
     */
    @DefaultStringValue("will be downloaded to your ")
    @Key("downloadedLabel")
    String downloadedLabel();

    /**
     * Translated "computer. Please specify a name for the downloaded file:".
     *
     * @return translated "computer. Please specify a name for the downloaded file:"
     */
    @DefaultStringValue("computer. Please specify a name for the downloaded file:")
    @Key("specifyDownloadFileLabel")
    String specifyDownloadFileLabel();

    /**
     * Translated "Download".
     *
     * @return translated "Download"
     */
    @DefaultStringValue("Download")
    @Key("downloadButtonCaption")
    String downloadButtonCaption();

    /**
     * Translated "R Code Browser".
     *
     * @return translated "R Code Browser"
     */
    @DefaultStringValue("R Code Browser")
    @Key("rCodeBrowserLabel")
    String rCodeBrowserLabel();

    /**
     * Translated "CodeBrowserType doesn't operate on filesystem files".
     *
     * @return translated "CodeBrowserType doesn't operate on filesystem files"
     */
    @DefaultStringValue("CodeBrowserType doesn't operate on filesystem files")
    @Key("openFileCodeBrowserMessage")
    String openFileCodeBrowserMessage();

    /**
     * Translated "R Data Frame".
     *
     * @return translated "R Data Frame"
     */
    @DefaultStringValue("R Data Frame")
    @Key("rDataFrameLabel")
    String rDataFrameLabel();

    /**
     * Translated "DataFrameType doesn't operate on filesystem files".
     *
     * @return translated "DataFrameType doesn't operate on filesystem files"
     */
    @DefaultStringValue("DataFrameType doesn't operate on filesystem files")
    @Key("openFileMessage")
    String openFileMessage();

    /**
     * Translated "Public Folder".
     *
     * @return translated "Public Folder"
     */
    @DefaultStringValue("Public Folder")
    @Key("publicFolderDesc")
    String publicFolderDesc();

    /**
     * Translated "Folder".
     *
     * @return translated "Folder"
     */
    @DefaultStringValue("Folder")
    @Key("folderDesc")
    String folderDesc();

    /**
     * Translated "Text file".
     *
     * @return translated "Text file"
     */
    @DefaultStringValue("Text file")
    @Key("textFileDesc")
    String textFileDesc();

    /**
     * Translated "Image file".
     *
     * @return translated "Image file"
     */
    @DefaultStringValue("Image file")
    @Key("imageFileDesc")
    String imageFileDesc();

    /**
     * Translated "Parent folder".
     *
     * @return translated "Parent folder"
     */
    @DefaultStringValue("Parent folder")
    @Key("parentFolderDesc")
    String parentFolderDesc();

    /**
     * Translated "Explore object".
     *
     * @return translated "Explore object"
     */
    @DefaultStringValue("Explore object")
    @Key("exploreObjectDesc")
    String exploreObjectDesc();

    /**
     * Translated "R source viewer".
     *
     * @return translated "R source viewer"
     */
    @DefaultStringValue("R source viewer")
    @Key("rSourceViewerDesc")
    String rSourceViewerDesc();

    /**
     * Translated "Profiler".
     *
     * @return translated "Profiler"
     */
    @DefaultStringValue("Profiler")
    @Key("profilerDesc")
    String profilerDesc();

    /**
     * Translated "R Script".
     *
     * @return translated "R Script"
     */
    @DefaultStringValue("R Script")
    @Key("rScriptLabel")
    String rScriptLabel();

    /**
     * Translated "Rd File".
     *
     * @return translated "Rd File"
     */
    @DefaultStringValue("Rd File")
    @Key("rdFile")
    String rdFile();

    /**
     * Translated "NAMESPACE".
     *
     * @return translated "NAMESPACE"
     */
    @DefaultStringValue("NAMESPACE")
    @Key("namespaceLabel")
    String namespaceLabel();

    /**
     * Translated "R History".
     *
     * @return translated "R History"
     */
    @DefaultStringValue("R History")
    @Key("rHistoryLabel")
    String rHistoryLabel();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultStringValue("R Markdown")
    @Key("rMarkdownLabel")
    String rMarkdownLabel();

    /**
     * Translated "R Notebook".
     *
     * @return translated "R Notebook"
     */
    @DefaultStringValue("R Notebook")
    @Key("rNotebookLabel")
    String rNotebookLabel();

    /**
     * Translated "Markdown".
     *
     * @return translated "Markdown"
     */
    @DefaultStringValue("Markdown")
    @Key("markdownLabel")
    String markdownLabel();

    /**
     * Translated "File Download Error".
     *
     * @return translated "File Download Error"
     */
    @DefaultStringValue("File Download Error")
    @Key("fileDownloadErrorCaption")
    String fileDownloadErrorCaption();

    /**
     * Translated "Unable to show file because file downloads are ".
     *
     * @return translated "Unable to show file because file downloads are "
     */
    @DefaultStringValue("Unable to show file because file downloads are ")
    @Key("fileDownloadErrorMessage")
    String fileDownloadErrorMessage();

    /**
     * Translated "restricted on this server.\n".
     *
     * @return translated "restricted on this server.\n"
     */
    @DefaultStringValue("restricted on this server.\\n")
    @Key("restrictedOnServerMessage")
    String restrictedOnServerMessage();

    /**
     * Translated "Unexpected filespec format".
     *
     * @return translated "Unexpected filespec format"
     */
    @DefaultStringValue("Unexpected filespec format")
    @Key("unexpectedFormatMessage")
    String unexpectedFormatMessage();

    /**
     * Translated "Open".
     *
     * @return translated "Open"
     */
    @DefaultStringValue("Open")
    @Key("openLabel")
    String openLabel();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    @DefaultStringValue("Save")
    @Key("saveLabel")
    String saveLabel();

    /**
     * Translated "Choose".
     *
     * @return translated "Choose"
     */
    @DefaultStringValue("Choose")
    @Key("chooseLabel")
    String chooseLabel();

    /**
     * Translated "and ".
     *
     * @return translated "and "
     */
    @DefaultStringValue("and ")
    @Key("andText")
    String andText();

    /**
     * Translated "Typeset LaTeX into PDF using:".
     *
     * @return translated "Typeset LaTeX into PDF using:"
     */
    @DefaultStringValue("Typeset LaTeX into PDF using:")
    @Key("typesetLatexLabel")
    String typesetLatexLabel();

    /**
     * Translated "Help on customizing LaTeX options".
     *
     * @return translated "Help on customizing LaTeX options"
     */
    @DefaultStringValue("Help on customizing LaTeX options")
    @Key("latexHelpLinkLabel")
    String latexHelpLinkLabel();

    /**
     * Translated "Error Setting CRAN Mirror".
     *
     * @return translated "Error Setting CRAN Mirror"
     */
    @DefaultStringValue("Error Setting CRAN Mirror")
    @Key("errorSettingCranMirror")
    String errorSettingCranMirror();

    /**
     * Translated "The CRAN mirror could not be changed.".
     *
     * @return translated "The CRAN mirror could not be changed."
     */
    @DefaultStringValue("The CRAN mirror could not be changed.")
    @Key("cranMirrorCannotChange")
    String cranMirrorCannotChange();

    /**
     * Translated "Insert Roxygen Skeleton".
     *
     * @return translated "Insert Roxygen Skeleton"
     */
    @DefaultStringValue("Insert Roxygen Skeleton")
    @Key("insertRoxygenSkeletonMessage")
    String insertRoxygenSkeletonMessage();

    /**
     * Translated "Unable to insert skeleton (the cursor is not currently ".
     *
     * @return translated "Unable to insert skeleton (the cursor is not currently "
     */
    @DefaultStringValue("Unable to insert skeleton (the cursor is not currently ")
    @Key("unableToInsertSkeletonMessage")
    String unableToInsertSkeletonMessage();

    /**
     * Translated "inside an R function definition).".
     *
     * @return translated "inside an R function definition)."
     */
    @DefaultStringValue("inside an R function definition).")
    @Key("rFunctionDefinitionMessage")
    String rFunctionDefinitionMessage();

    /**
     * Translated "Cannot automatically update roxygen blocks ".
     *
     * @return translated "Cannot automatically update roxygen blocks "
     */
    @DefaultStringValue("Cannot automatically update roxygen blocks ")
    @Key("cannotUpdateRoxygenMessage")
    String cannotUpdateRoxygenMessage();

    /**
     * Translated "that are not self-contained.".
     *
     * @return translated "that are not self-contained."
     */
    @DefaultStringValue("that are not self-contained.")
    @Key("notSelfContainedMessage")
    String notSelfContainedMessage();

    /**
     * Translated "Confirm Change".
     *
     * @return translated "Confirm Change"
     */
    @DefaultStringValue("Confirm Change")
    @Key("confirmChangeCaption")
    String confirmChangeCaption();

    /**
     * Translated "The ".
     *
     * @return translated "The "
     */
    @DefaultStringValue("The ")
    @Key("theMessage")
    String theMessage();

    /**
     * Translated "package is required ".
     *
     * @return translated "package is required "
     */
    @DefaultStringValue("package is required ")
    @Key("packageRequiredMessage")
    String packageRequiredMessage();

    /**
     * Translated "for ".
     *
     * @return translated "for "
     */
    @DefaultStringValue("for ")
    @Key("forMessage")
    String forMessage();

    /**
     * Translated "weaving, ".
     *
     * @return translated "weaving, "
     */
    @DefaultStringValue("weaving, ")
    @Key("weavingMessage")
    String weavingMessage();

    /**
     * Translated "however it is not currently installed. You should ".
     *
     * @return translated "however it is not currently installed. You should "
     */
    @DefaultStringValue("however it is not currently installed. You should ")
    @Key("notCurrentlyInstalledMessage")
    String notCurrentlyInstalledMessage();

    /**
     * Translated "ensure that ".
     *
     * @return translated "ensure that "
     */
    @DefaultStringValue("ensure that ")
    @Key("ensureThatMessage")
    String ensureThatMessage();

    /**
     * Translated "is installed ".
     *
     * @return translated "is installed "
     */
    @DefaultStringValue("is installed ")
    @Key("isInstalledMessage")
    String isInstalledMessage();

    /**
     * Translated "prior to compiling a PDF.".
     *
     * @return translated "prior to compiling a PDF."
     */
    @DefaultStringValue("prior to compiling a PDF.")
    @Key("compilingPDFMessage")
    String compilingPDFMessage();

    /**
     * Translated "Are you sure you want to change this option?".
     *
     * @return translated "Are you sure you want to change this option?"
     */
    @DefaultStringValue("Are you sure you want to change this option?")
    @Key("changeOptionMessage")
    String changeOptionMessage();

    /**
     * Translated "Upload Error Occurred".
     *
     * @return translated "Upload Error Occurred"
     */
    @DefaultStringValue("Upload Error Occurred")
    @Key("uploadErrorTitle")
    String uploadErrorTitle();

    /**
     * Translated "Unable to continue ".
     *
     * @return translated "Unable to continue "
     */
    @DefaultStringValue("Unable to continue ")
    @Key("unableToContinueMessage")
    String unableToContinueMessage();

    /**
     * Translated "(another publish is currently running)".
     *
     * @return translated "(another publish is currently running)"
     */
    @DefaultStringValue("(another publish is currently running)")
    @Key("currentlyRunningMessage")
    String currentlyRunningMessage();

    /**
     * Translated "Uploading document to RPubs...".
     *
     * @return translated "Uploading document to RPubs..."
     */
    @DefaultStringValue("Uploading document to RPubs...")
    @Key("uploadingDocumentRPubsMessage")
    String uploadingDocumentRPubsMessage();

    /**
     * Translated "Publish to RPubs".
     *
     * @return translated "Publish to RPubs"
     */
    @DefaultStringValue("Publish to RPubs")
    @Key("publishToRPubs")
    String publishToRPubs();

    /**
     * Translated "RPubs is a free service from RStudio for sharing ".
     *
     * @return translated "RPubs is a free service from RStudio for sharing "
     */
    @DefaultStringValue("RPubs is a free service from RStudio for sharing ")
    @Key("rPubsServiceMessage")
    String rPubsServiceMessage();

    /**
     * Translated "documents on the web. Click Publish to get ".
     *
     * @return translated "documents on the web. Click Publish to get "
     */
    @DefaultStringValue("documents on the web. Click Publish to get ")
    @Key("clickPublishMessage")
    String clickPublishMessage();

    /**
     * Translated "started.".
     *
     * @return translated "started."
     */
    @DefaultStringValue("started.")
    @Key("startedMessage")
    String startedMessage();

    /**
     * Translated "This document has already been published on RPubs. You can ".
     *
     * @return translated "This document has already been published on RPubs. You can "
     */
    @DefaultStringValue("This document has already been published on RPubs. You can ")
    @Key("alreadyPublishedRPubs")
    String alreadyPublishedRPubs();

    /**
     * Translated "choose to either update the existing RPubs document, or ".
     *
     * @return translated "choose to either update the existing RPubs document, or "
     */
    @DefaultStringValue("choose to either update the existing RPubs document, or ")
    @Key("updateRPubsMessage")
    String updateRPubsMessage();

    /**
     * Translated "create a new one.".
     *
     * @return translated "create a new one."
     */
    @DefaultStringValue("create a new one.")
    @Key("createNewRPub")
    String createNewRPub();

    /**
     * Translated "IMPORTANT: All documents published to RPubs are ".
     *
     * @return translated "IMPORTANT: All documents published to RPubs are "
     */
    @DefaultStringValue("IMPORTANT: All documents published to RPubs are ")
    @Key("importantRPubsMessage")
    String importantRPubsMessage();

    /**
     * Translated "publicly visible.".
     *
     * @return translated "publicly visible."
     */
    @DefaultStringValue("publicly visible.")
    @Key("publiclyVisibleWarningLabel")
    String publiclyVisibleWarningLabel();

    /**
     * Translated "You should ".
     *
     * @return translated "You should "
     */
    @DefaultStringValue("You should ")
    @Key("youShouldMessage")
    String youShouldMessage();

    /**
     * Translated "only publish documents that you wish to share publicly.".
     *
     * @return translated "only publish documents that you wish to share publicly."
     */
    @DefaultStringValue("only publish documents that you wish to share publicly.")
    @Key("publishDocumentsMessage")
    String publishDocumentsMessage();

    /**
     * Translated "Publish".
     *
     * @return translated "Publish"
     */
    @DefaultStringValue("Publish")
    @Key("publishButtonTitle")
    String publishButtonTitle();

    /**
     * Translated "Update Existing".
     *
     * @return translated "Update Existing"
     */
    @DefaultStringValue("Update Existing")
    @Key("updateExistingButtonTitle")
    String updateExistingButtonTitle();

    /**
     * Translated "Create New".
     *
     * @return translated "Create New"
     */
    @DefaultStringValue("Create New")
    @Key("createNewButtonTitle")
    String createNewButtonTitle();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultStringValue("Cancel")
    @Key("cancelTitle")
    String cancelTitle();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultStringValue("OK")
    @Key("okTitle")
    String okTitle();

    /**
     * Translated "Using Keyring".
     *
     * @return translated "Using Keyring"
     */
    @DefaultStringValue("Using Keyring")
    @Key("usingKeyringCaption")
    String usingKeyringCaption();

    /**
     * Translated "Keyring is an R package that provides access to ".
     *
     * @return translated "Keyring is an R package that provides access to "
     */
    @DefaultStringValue("Keyring is an R package that provides access to ")
    @Key("keyringDesc")
    String keyringDesc();

    /**
     * Translated "the operating systems credential store to allow you ".
     *
     * @return translated "the operating systems credential store to allow you "
     */
    @DefaultStringValue("the operating systems credential store to allow you ")
    @Key("keyringCredentialStoreMessage")
    String keyringCredentialStoreMessage();

    /**
     * Translated "to remember, securely, passwords and secrets. ".
     *
     * @return translated "to remember, securely, passwords and secrets. "
     */
    @DefaultStringValue("to remember, securely, passwords and secrets. ")
    @Key("keyringSecureMessage")
    String keyringSecureMessage();

    /**
     * Translated "Would you like to install keyring?".
     *
     * @return translated "Would you like to install keyring?"
     */
    @DefaultStringValue("Would you like to install keyring?")
    @Key("installKeyringMessage")
    String installKeyringMessage();

    /**
     * Translated "Keyring".
     *
     * @return translated "Keyring"
     */
    @DefaultStringValue("Keyring")
    @Key("keyringCaption")
    String keyringCaption();

    /**
     * Translated "Install".
     *
     * @return translated "Install"
     */
    @DefaultStringValue("Install")
    @Key("installLabel")
    String installLabel();

    /**
     * Translated "You must enter a value.".
     *
     * @return translated "You must enter a value."
     */
    @DefaultStringValue("You must enter a value.")
    @Key("enterValueMessage")
    String enterValueMessage();

    /**
     * Translated "Too much console output to announce.".
     *
     * @return translated "Too much console output to announce."
     */
    @DefaultStringValue("Too much console output to announce.")
    @Key("consoleOutputOverLimitMessage")
    String consoleOutputOverLimitMessage();

    /**
     * Translated "Error: ".
     *
     * @return translated "Error: "
     */
    @DefaultStringValue("Error: ")
    @Key("consoleWriteError")
    String consoleWriteError();

    /**
     * Translated "Line ".
     *
     * @return translated "Line "
     */
    @DefaultStringValue("Line ")
    @Key("lineText")
    String lineText();

    /**
     * Translated "View error or warning within the log file".
     *
     * @return translated "View error or warning within the log file"
     */
    @DefaultStringValue("View error or warning within the log file")
    @Key("viewErrorLogfile")
    String viewErrorLogfile();


    /**
     * Translated "Syncing...".
     *
     * @return translated "Syncing..."
     */
    @DefaultStringValue("Syncing...")
    @Key("getSyncProgressMessage")
    String getSyncProgressMessage();

    /**
     * Translated "; Page ".
     *
     * @return translated "; Page ."
     */
    @DefaultStringValue("; Page ")
    @Key("pdfPageText")
    String pdfPageText();

    /**
     * Translated "[From Click]".
     *
     * @return translated "[From Click]"
     */
    @DefaultStringValue("[From Click]")
    @Key("pdfFromClickText")
    String pdfFromClickText();

    /**
     * Translated "Password".
     *
     * @return translated "Password"
     */
    @DefaultStringValue("Password")
    @Key("passwordTitle")
    String passwordTitle();

    /**
     * Translated "Username".
     *
     * @return translated "Username"
     */
    @DefaultStringValue("Username")
    @Key("usernameTitle")
    String usernameTitle();

    /**
     * Translated "Press ".
     *
     * @return translated "Press "
     */
    @DefaultStringValue("Press ")
    @Key("pressLabel")
    String pressLabel();

    /**
     * Translated "to copy the key to the clipboard".
     *
     * @return translated "to copy the key to the clipboard"
     */
    @DefaultStringValue("to copy the key to the clipboard")
    @Key("copyKeyToClipboardLabel")
    String copyKeyToClipboardLabel();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultStringValue("Close")
    @Key("closeButtonLabel")
    String closeButtonLabel();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    @DefaultStringValue("(None)")
    @Key("noneLabel")
    String noneLabel();

    /**
     * Translated "Getting ignored files for path...".
     *
     * @return translated "Getting ignored files for path..."
     */
    @DefaultStringValue("Getting ignored files for path...")
    @Key("gettingIgnoredFilesProgressMessage")
    String gettingIgnoredFilesProgressMessage();

    /**
     * Translated "Setting ignored files for path...".
     *
     * @return translated "Setting ignored files for path..."
     */
    @DefaultStringValue("Setting ignored files for path...")
    @Key("settingIgnoredFilesProgressMessage")
    String settingIgnoredFilesProgressMessage();

    /**
     * Translated "Error: Multiple Directories".
     *
     * @return translated "Error: Multiple Directories"
     */
    @DefaultStringValue("Error: Multiple Directories")
    @Key("multipleDirectoriesCaption")
    String multipleDirectoriesCaption();

    /**
     * Translated "The selected files are not all within the same directory ".
     *
     * @return translated "The selected files are not all within the same directory "
     */
    @DefaultStringValue("The selected files are not all within the same directory ")
    @Key("selectedFilesNotInSameDirectoryMessage")
    String selectedFilesNotInSameDirectoryMessage();

    /**
     * Translated "(you can only ignore multiple files in one operation if ".
     *
     * @return translated "(you can only ignore multiple files in one operation if "
     */
    @DefaultStringValue("(you can only ignore multiple files in one operation if ")
    @Key("ignoreMultipleFilesMessage")
    String ignoreMultipleFilesMessage();

    /**
     * Translated "they are located within the same directory).".
     *
     * @return translated "they are located within the same directory)."
     */
    @DefaultStringValue("they are located within the same directory).")
    @Key("locatedInSameDirectoryMessage")
    String locatedInSameDirectoryMessage();

    /**
     * Translated "Directory:".
     *
     * @return translated "Directory:"
     */
    @DefaultStringValue("Directory:")
    @Key("directoryLabel")
    String directoryLabel();

    /**
     * Translated "Ignored files".
     *
     * @return translated "Ignored files"
     */
    @DefaultStringValue("Ignored files")
    @Key("ignoredFilesLabel")
    String ignoredFilesLabel();

    /**
     * Translated "Ignore:".
     *
     * @return translated "Ignore:"
     */
    @DefaultStringValue("Ignore:")
    @Key("ignoreCaption")
    String ignoreCaption();

    /**
     * Translated "Specifying ignored files".
     *
     * @return translated "Specifying ignored files"
     */
    @DefaultStringValue("Specifying ignored files")
    @Key("specifyingIgnoredFilesHelpCaption")
    String specifyingIgnoredFilesHelpCaption();

    /**
     * Translated "Loading file contents".
     *
     * @return translated "Loading file contents"
     */
    @DefaultStringValue("Loading file contents")
    @Key("loadingFileContentsProgressCaption")
    String loadingFileContentsProgressCaption();

    /**
     * Translated "Save As".
     *
     * @return translated "Save As"
     */
    @DefaultStringValue("Save As")
    @Key("saveAsText")
    String saveAsText();

    /**
     * Translated "Save File - ".
     *
     * @return translated "Save File - "
     */
    @DefaultStringValue("Save File - ")
    @Key("saveFileCaption")
    String saveFileCaption();

    /**
     * Translated "Saving file...".
     *
     * @return translated "Saving file..."
     */
    @DefaultStringValue("Saving file...")
    @Key("savingFileProgressCaption")
    String savingFileProgressCaption();

    /**
     * Translated "View File Tab".
     *
     * @return translated "View File Tab"
     */
    @DefaultStringValue("View File Tab")
    @Key("viewFileTabLabel")
    String viewFileTabLabel();
}

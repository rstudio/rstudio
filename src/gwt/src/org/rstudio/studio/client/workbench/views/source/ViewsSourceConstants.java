/*
 * ViewsSourceConstants.java
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

package org.rstudio.studio.client.workbench.views.source;

public interface ViewsSourceConstants extends com.google.gwt.i18n.client.Messages{

    @DefaultMessage("(No documents)")
    @Key("noDocumentsParentheses")
    String noDocumentsParentheses();

    @DefaultMessage("No outline available")
    @Key("noOutlineAvailable")
    String noOutlineAvailable();

    @DefaultMessage("Title")
    @Key("title")
    String title();

    @DefaultMessage("Invalid API Name")
    @Key("invalidApiName")
    String invalidApiName();

    @DefaultMessage("Invalid application name")
    @Key("invalidApplicationName")
    String invalidApplicationName();

    @DefaultMessage("The API name must not be empty")
    @Key("apiNameMustNotBeEmpty")
    String apiNameMustNotBeEmpty();

    @DefaultMessage("Plumber APIs")
    @Key("plumberAPIs")
    String plumberAPIs();

    @DefaultMessage("Create within directory:")
    @Key("createWithinDirectoryColon")
    String createWithinDirectoryColon();

    @DefaultMessage("API name:")
    @Key("apiNameColon")
    String apiNameColon();

    @DefaultMessage("Create")
    @Key("create")
    String create();

    @DefaultMessage("Application name:")
    @Key("applicationNameColon")
    String applicationNameColon();

    @DefaultMessage("Application type:")
    @Key("applicationTypeColon")
    String applicationTypeColon();

    @DefaultMessage("Single File (app.R)")
    @Key("singleFileAppR")
    String singleFileAppR();

    @DefaultMessage("Multiple File (ui.R/server.R)")
    @Key("multipleFileUiServerR")
    String multipleFileUiServerR();

    @DefaultMessage("Shiny Web Applications")
    @Key("shinyWebApplications")
    String shinyWebApplications();

    @DefaultMessage("The application name must not be empty")
    @Key("applicationNameMustNotBeEmpty")
    String applicationNameMustNotBeEmpty();

    @DefaultMessage("Invalid Application Name")
    @Key("invalidApplicationNameCapitalized")
    String invalidApplicationNameCapitalized();

    @DefaultMessage("New Quarto {0}...")
    @Key("newQuatroProgressIndicator")
    String newQuatroProgressIndicator(String presentationType);

    @DefaultMessage("Presentation")
    @Key("presentationCapitalized")
    String presentationCapitalized();

    @DefaultMessage("Document")
    @Key("documentCapitalized")
    String documentCapitalized();

    @DefaultMessage("Error")
    @Key("errorCapitalized")
    String errorCapitalized();

    @DefaultMessage("Always save files before build")
    @Key("alwaysSaveFilesBeforeBuild")
    String alwaysSaveFilesBeforeBuild();

    @DefaultMessage("Show")
    @Key("show")
    String show();

    @DefaultMessage("Source Document Error")
    @Key("sourceDocumentError")
    String sourceDocumentError();

    @DefaultMessage("Show Profiler")
    @Key("showProfiler")
    String showProfiler();

    @DefaultMessage("R Notebook")
    @Key("rNotebook")
    String rNotebook();

    @DefaultMessage("Notebook Creation Failed")
    @Key("notebookCreationFailed")
    String notebookCreationFailed();

    @DefaultMessage("One or more packages required for R Notebook creation were not installed.")
    @Key("rNotebookCreationFailedPackagesNotInstalled")
    String rNotebookCreationFailedPackagesNotInstalled();

    @DefaultMessage("Creating Stan script")
    @Key("creatingStanScript")
    String creatingStanScript();

    @DefaultMessage("Creating Stan scripts")
    @Key("creatingStanScriptPlural")
    String creatingStanScriptPlural();

    @DefaultMessage("Creating new document...")
    @Key("creatingNewDocument")
    String creatingNewDocument();

    @DefaultMessage("Error Creating Shiny Application")
    @Key("errorCreatingShinyApplication")
    String errorCreatingShinyApplication();

    @DefaultMessage("Error Creating Plumber API")
    @Key("errorCreatingPlumberApi")
    String errorCreatingPlumberApi();

    @DefaultMessage("New Shiny Web Application")
    @Key("newShinyWebApplication")
    String newShinyWebApplication();

    @DefaultMessage("New R Presentation")
    @Key("newRPresentation")
    String newRPresentation();

    @DefaultMessage("Creating Presentation...")
    @Key("creatingPresentation")
    String creatingPresentation();

    @DefaultMessage("Document Tab Move Failed")
    @Key("documentTabMoveFailed")
    String documentTabMoveFailed();

    @DefaultMessage("Couldn''t move the tab to this window: \n{0}")
    @Key("couldntMoveTabToWindowError")
    String couldntMoveTabToWindowError(String errorMessage);

    @DefaultMessage("Close All")
    @Key("closeAll")
    String closeAll();

    @DefaultMessage("Close Other")
    @Key("closeOther")
    String closeOther();

    @DefaultMessage("Open File")
    @Key("openFile")
    String openFile();

    @DefaultMessage("New Plumber API")
    @Key("newPlumberApi")
    String newPlumberApi();

    @DefaultMessage("Close All Others")
    @Key("closeAllOthers")
    String closeAllOthers();

    @DefaultMessage("Error Creating New Document")
    @Key("errorCreatingNewDocument")
    String errorCreatingNewDocument();

    @DefaultMessage("Publish Document...")
    @Key("publishDocument")
    String publishDocument();

    @DefaultMessage("Publish Plumber API...")
    @Key("publishPlumberApi")
    String publishPlumberApi();

    @DefaultMessage("Publish Application...")
    @Key("publishApplication")
    String publishApplication();

    // TODO IF THESE ARE SHORTCUTS NEED TO DOUBLE CHECK THESE

    @DefaultMessage("_Blame \"{0}\" on GitHub")
    @Key("blameOnGithub")
    String blameOnGithub(String name);

    @DefaultMessage("_View \"{0}\" on GitHub")
    @Key("viewNameOnGithub")
    String viewNameOnGithub(String name);

    @DefaultMessage("_Revert \"{0}\"...")
    @Key("revertName")
    String revertName(String name);

    @DefaultMessage("_Log of \"{0}\"")
    @Key("logOfName")
    String logOfName(String name);

    @DefaultMessage("_Diff \"{0}\"")
    @Key("diffName")
    String diffName(String name);

    @DefaultMessage("No document tabs open")
    @Key("noDocumentTabsOpen")
    String noDocumentTabsOpen();

    @DefaultMessage("Show Object Explorer")
    @Key("showObjectExplorer")
    String showObjectExplorer();

    @DefaultMessage("Show Data Frame")
    @Key("showDataFrame")
    String showDataFrame();

    @DefaultMessage("Template Content Missing")
    @Key("templateContentMissing")
    String templateContentMissing();

    @DefaultMessage("The template at {0} is missing.")
    @Key("templateAtPathMissing")
    String templateAtPathMissing(String templatePath);

    @DefaultMessage("Error while opening file")
    @Key("errorWhileOpeningFile")
    String errorWhileOpeningFile();

    @DefaultMessage("This notebook has the same name as an R Markdown file, but doesn''t match it")
    @Key("openNotebookWarningMessage")
    String openNotebookWarningMessage();

    @DefaultMessage("Notebook Open Failed")
    @Key("notebookOpenFailed")
    String notebookOpenFailed();

    @DefaultMessage("This notebook could not be opened. If the error persists, try removing the accompanying R Markdown file. \\n\\n{0}")
    @Key("notebookOpenFailedMessage")
    String notebookOpenFailedMessage(String errorMessage);

    @DefaultMessage("This notebook could not be opened. \n\n{0}")
    @Key("notebookCouldNotBeOpenedMessage")
    String notebookCouldNotBeOpenedMessage(String errorMessage);

    @DefaultMessage("Opening file...")
    @Key("openingFile")
    String openingFile();

    @DefaultMessage("The file ''{0}'' is too large to open in the source editor (the file is {1} and the maximum file size is {2})")
    @Key("showFileTooLargeWarningMsg")
    String showFileTooLargeWarningMsg(String filename, String filelength, String sizeLimit);

    @DefaultMessage("Selected File Too Large")
    @Key("selectedFileTooLarge")
    String selectedFileTooLarge();

    @DefaultMessage("The source file ''{0}'' is large ({1}) and may take some time to open. Are you sure you want to continue opening it?")
    @Key("confirmOpenLargeFileMsg")
    String confirmOpenLargeFileMsg(String filename, String length);

    @DefaultMessage("Confirm Open")
    @Key("confirmOpen")
    String confirmOpen();

    @DefaultMessage("Source Column {0}")
    @Key("sourceColumn")
    String sourceColumn(int columnCounter);

    @DefaultMessage("Source")
    @Key("source")
    String source();

    @DefaultMessage("Switch to tab")
    @Key("switchToTab")
    String switchToTab();

    @DefaultMessage("RStudio Source Editor")
    @Key("rstudioSourceEditor")
    String rstudioSourceEditor();

    @DefaultMessage("Close Source Window")
    @Key("closeSourceWindow")
    String closeSourceWindow();

    @DefaultMessage("Cancel")
    @Key("cancel")
    String cancel();

    @DefaultMessage("Close and Discard Changes")
    @Key("closeAndDiscardChanges")
    String closeAndDiscardChanges();

    @DefaultMessage("Unsaved Changes")
    @Key("unsavedChanges")
    String unsavedChanges();

    @DefaultMessage("There are unsaved documents in this window. Are you sure you want to close it?")
    @Key("confirmCloseUnsavedDocuments")
    String confirmCloseUnsavedDocuments();

    @DefaultMessage("Your edits to the file {0} have not been saved")
    @Key("yourEditsToFileHasNotBeenSaved")
    String yourEditsToFileHasNotBeenSaved(String desc);

    @DefaultMessage("Your edits to the files {0} have not been saved")
    @Key("yourEditsToFilePluralHasNotBeenSaved")
    String yourEditsToFilePluralHasNotBeenSaved(String completeMessage);

    @DefaultMessage(", ")
    @Key("commaListSeparator")
    String commaListSeparator();

    @DefaultMessage(" and ")
    @Key("andForList")
    String andForList();

    @DefaultMessage("Can''t Move Doc")
    @Key("cantMoveDoc")
    String cantMoveDoc();

    @DefaultMessage("The document could not be moved to a different window: \n{0}")
    @Key("cantMoveDocMessage")
    String cantMoveDocMessage(String errorMessage);

    @DefaultMessage("Search tabs")
    @Key("searchTabs")
    String searchTabs();

    @DefaultMessage("Code Editor Tab")
    @Key("codeEditorTab")
    String codeEditorTab();

    @DefaultMessage("Function:")
    @Key("functionColon")
    String functionColon();

    @DefaultMessage("Method:")
    @Key("methodColon")
    String methodColon();

    @DefaultMessage("Error Reading Function Definition")
    @Key("errorReadingFunctionDefinition")
    String errorReadingFunctionDefinition();

    @DefaultMessage("Code Browser displayed")
    @Key("codeBrowserDisplayed")
    String codeBrowserDisplayed();

    @DefaultMessage("Debug location is approximate because the source is not available.")
    @Key("debugLocationIsApproximate")
    String debugLocationIsApproximate();

    @DefaultMessage("R Source Viewer")
    @Key("rSourceViewer")
    String rSourceViewer();

    @DefaultMessage("(Read-only)")
    @Key("readOnlyParentheses")
    String readOnlyParentheses();

    @DefaultMessage("Code Browser Second")
    @Key("codeBrowserSecond")
    String codeBrowserSecond();

    @DefaultMessage("Code Tools")
    @Key("codeTools")
    String codeTools();

    @DefaultMessage("Error Searching for Function")
    @Key("errorSearchingForFunction")
    String errorSearchingForFunction();

    @DefaultMessage("Searching for function definition...")
    @Key("searchingForFunctionDefinition")
    String searchingForFunctionDefinition();

    @DefaultMessage("{0} Source Viewer")
    @Key("nameSourceViewer")
    String nameSourceViewer(String name);

    @DefaultMessage("Untitled Source Viewer")
    @Key("untitledSourceViewer")
    String untitledSourceViewer();

    @DefaultMessage("Help")
    @Key("help")
    String help();

    @DefaultMessage("Data Browser displayed")
    @Key("dataBrowserDisplayed")
    String dataBrowserDisplayed();

    @DefaultMessage("Data Browser")
    @Key("dataBrowser")
    String dataBrowser();

    @DefaultMessage("{0} Data Browser")
    @Key("accessibleNameDataBrowser")
    String accessibleNameDataBrowser(String accessibleName);

    @DefaultMessage("Untitled Data Browser")
    @Key("untitledDataBrowser")
    String untitledDataBrowser();

    @DefaultMessage("Displayed {0} rows of {1}")
    @Key("dataEditingTargetWidgetLabel1")
    String dataEditingTargetWidgetLabel1(String displayedObservations, String totalObservations);

    @DefaultMessage("({0} omitted)")
    @Key("dataEditingTargetWidgetLabel2")
    String dataEditingTargetWidgetLabel2(String omittedNumber);

    @DefaultMessage("{0} Object Explorer")
    @Key("accessibleNameObjectExplorer")
    String accessibleNameObjectExplorer(String accessibleName);

    @DefaultMessage("Untitled Object Explorer")
    @Key("untitledObjectExplorer")
    String untitledObjectExplorer();

    @DefaultMessage("Object Explorer displayed")
    @Key("objectExplorerDisplayed")
    String objectExplorerDisplayed();

    @DefaultMessage("URL Viewer displayed")
    @Key("urlViewerDisplayed")
    String urlViewerDisplayed();

    @DefaultMessage("URL Browser")
    @Key("urlBrowser")
    String urlBrowser();

    @DefaultMessage("{0} URL Browser")
    @Key("accessibleNameBrowser")
    String accessibleNameBrowser(String name);

    @DefaultMessage("Untitled URL Browser")
    @Key("untitledUrlBrowser")
    String untitledUrlBrowser();

    @DefaultMessage("Source Viewer")
    @Key("sourceViewer")
    String sourceViewer();

    @DefaultMessage("Name")
    @Key("name")
    String name();

    @DefaultMessage("Type")
    @Key("type")
    String type();

    @DefaultMessage("Value")
    @Key("value")
    String value();

    @DefaultMessage("View({0})")
    @Key("viewCode")
    String viewCode(String code);

    @DefaultMessage("(No selection)")
    @Key("noSelectionParentheses")
    String noSelectionParentheses();

    @DefaultMessage("Show Attributes")
    @Key("showAttributes")
    String showAttributes();

    @DefaultMessage("Search objects")
    @Key("searchObjects")
    String searchObjects();

    @DefaultMessage("Refresh")
    @Key("refresh")
    String refresh();

    @DefaultMessage("The source file {0} does not exist.")
    @Key("sourceFileAtPathDoesNotExist")
    String sourceFileAtPathDoesNotExist(String selectedPath);

    @DefaultMessage("Error while opening profiler source")
    @Key("errorOpeningProfilerSource")
    String errorOpeningProfilerSource();

    @DefaultMessage("Failed to Save Profile")
    @Key("failedToSaveProfile")
    String failedToSaveProfile();

    @DefaultMessage("Save File - {0}")
    @Key("saveFileName")
    String saveFileName(String name);

    @DefaultMessage("Failed to Save Profile Properties")
    @Key("failedToSaveProfileProperties")
    String failedToSaveProfileProperties();

    @DefaultMessage("Code Profile results displayed")
    @Key("codeProfileResultsDisplayed")
    String codeProfileResultsDisplayed();

    @DefaultMessage("Profile")
    @Key("profileCapitalized")
    String profileCapitalized();

    @DefaultMessage("Profiler")
    @Key("profilerCapitalized")
    String profilerCapitalized();

    @DefaultMessage("R Profiler")
    @Key("rProfiler")
    String rProfiler();

    @DefaultMessage("{0} Profile View")
    @Key("titleProfileView")
    String titleProfileView(String title);

    @DefaultMessage("Failed to Open Profile")
    @Key("failedToOpenProfile")
    String failedToOpenProfile();

    @DefaultMessage("Profiler Error")
    @Key("profilerError")
    String profilerError();

    @DefaultMessage("Failed to Stop Profiler")
    @Key("failedToStopProfiler")
    String failedToStopProfiler();

    @DefaultMessage("Error navigating to file")
    @Key("errorNavigatingToFile")
    String errorNavigatingToFile();

    @DefaultMessage("No file at path ''{0}''.")
    @Key("noFileAtPath")
    String noFileAtPath(String finalUrl);

    @DefaultMessage("Open Link (Command+Click)")
    @Key("openLinkMacCommand")
    String openLinkMacCommand();

    @DefaultMessage("Open Link (Ctrl+Click)")
    @Key("openLinkNotMacCommand")
    String openLinkNotMacCommand();

    @DefaultMessage("Finding definition...")
    @Key("findingDefinition")
    String findingDefinition();

    @DefaultMessage("ignored")
    @Key("ignored")
    String ignored();

    @DefaultMessage("note")
    @Key("note")
    String note();

    @DefaultMessage("warning")
    @Key("warningLowercase")
    String warningLowercase();

    @DefaultMessage("error")
    @Key("error")
    String error();

    @DefaultMessage("fatal")
    @Key("fatal")
    String fatal();

    @DefaultMessage("(No matches)")
    @Key("noMatchesParentheses")
    String noMatchesParentheses();

    @DefaultMessage("{0} of {1}")
    @Key("pagingLabelTextOf")
    String pagingLabelTextOf(int index, int textLength);

    @DefaultMessage("{0} occurrences replaced.")
    @Key("numberOfOccurrencesReplaced")
    String numberOfOccurrencesReplaced(int occurrences);

    @DefaultMessage("Invalid search term.")
    @Key("invalidSearchTerm")
    String invalidSearchTerm();

    @DefaultMessage("No more occurrences.")
    @Key("noMoreOccurrences")
    String noMoreOccurrences();

    @DefaultMessage("Find")
    @Key("findCapitalized")
    String findCapitalized();

    @DefaultMessage("Find/Replace")
    @Key("findOrReplace")
    String findOrReplace();

    @DefaultMessage("Replace")
    @Key("replaceCapitalized")
    String replaceCapitalized();

    @DefaultMessage("All")
    @Key("allCapitalized")
    String allCapitalized();

    @DefaultMessage("Replace all occurrences")
    @Key("replaceAllOccurrences")
    String replaceAllOccurrences();

    @DefaultMessage("In selection")
    @Key("inSelection")
    String inSelection();

    @DefaultMessage("Match case")
    @Key("matchCase")
    String matchCase();

    @DefaultMessage("Whole word")
    @Key("wholeWord")
    String wholeWord();

    @DefaultMessage("Regex")
    @Key("regexCapitalized")
    String regexCapitalized();

    @DefaultMessage("Wrap")
    @Key("wrapCapitalized")
    String wrapCapitalized();

    @DefaultMessage("Close find and replace")
    @Key("closeFindAndReplace")
    String closeFindAndReplace();

    @DefaultMessage("Chunk Pending Execution")
    @Key("chunkPendingExecution")
    String chunkPendingExecution();

    @DefaultMessage("The code in this chunk is scheduled to run later, when other chunks have finished executing.")
    @Key("chunkPendingExecutionMessage")
    String chunkPendingExecutionMessage();

    @DefaultMessage("OK")
    @Key("okFullyCapitalized")
    String okFullyCapitalized();

    @DefaultMessage("Don''t Run")
    @Key("dontRun")
    String dontRun();

    @DefaultMessage("The rmarkdown package is not installed; notebook HTML file will not be generated.")
    @Key("rMarkdownNotInstalledHTMLNoGenerate")
    String rMarkdownNotInstalledHTMLNoGenerate();

    @DefaultMessage("An updated version of the rmarkdown package is required to generate notebook HTML files.")
    @Key("rMarkdownUpgradeRequired")
    String rMarkdownUpgradeRequired();

    @DefaultMessage("Error creating notebook: ")
    @Key("errorCreatingNotebookPrefix")
    String errorCreatingNotebookPrefix();

    @DefaultMessage("Creating R Notebooks")
    @Key("creatingRNotebooks")
    String creatingRNotebooks();

    @DefaultMessage("Can''t execute {0}")
    @Key("cantExecuteJobDesc")
    String cantExecuteJobDesc(String jobDesc);

    @DefaultMessage("Executing Python chunks")
    @Key("executingPythonChunks")
    String executingPythonChunks();

    @DefaultMessage("Executing chunks")
    @Key("executingChunks")
    String executingChunks();

    @DefaultMessage("Run Chunk")
    @Key("runChunk")
    String runChunk();

    @DefaultMessage("{0}: Chunks Currently Executing")
    @Key("jobChunkCurrentlyExecuting")
    String jobChunkCurrentlyExecuting(String jobDesc);

    @DefaultMessage("RStudio cannot execute ''{0}'' because this notebook is already executing code. Interrupt R, or wait for execution to complete.")
    @Key("rStudioCannotExecuteJob")
    String rStudioCannotExecuteJob(String jobDesc);

    @DefaultMessage("Run Chunks")
    @Key("runChunks")
    String runChunks();

    @DefaultMessage("Chunks Currently Running")
    @Key("chunksCurrentlyRunning")
    String chunksCurrentlyRunning();

    @DefaultMessage("Output can''t be cleared because there are still chunks running. Do you want to interrupt them?")
    @Key("outputCantBeClearedBecauseChunks")
    String outputCantBeClearedBecauseChunks();

    @DefaultMessage("Interrupt and Clear Output")
    @Key("interruptAndClearOutput")
    String interruptAndClearOutput();

    @DefaultMessage("Remove Inline Chunk Output")
    @Key("removeInlineChunkOutput")
    String removeInlineChunkOutput();

    @DefaultMessage("Do you want to clear all the existing chunk output from your notebook?")
    @Key("clearExistingChunkOutputMessage")
    String clearExistingChunkOutputMessage();

    @DefaultMessage("Remove Output")
    @Key("removeOutput")
    String removeOutput();

    @DefaultMessage("Keep Output")
    @Key("keepOutput")
    String keepOutput();

    @DefaultMessage("Unnamed chunk")
    @Key("unnamedChunk")
    String unnamedChunk();

    @DefaultMessage("Chunk Name:")
    @Key("chunkNameColon")
    String chunkNameColon();

    @DefaultMessage("Output:")
    @Key("outputColon")
    String outputColon();

    @DefaultMessage("Show warnings")
    @Key("showWarnings")
    String showWarnings();

    @DefaultMessage("Show messages")
    @Key("showMessages")
    String showMessages();

    @DefaultMessage("Cache chunk")
    @Key("cacheChunk")
    String cacheChunk();

    @DefaultMessage("Use paged tables")
    @Key("usePagedTables")
    String usePagedTables();

    @DefaultMessage("Use custom figure size")
    @Key("useCustomFigureSize")
    String useCustomFigureSize();

    @DefaultMessage("Width (inches):")
    @Key("widthInchesColon")
    String widthInchesColon();

    @DefaultMessage("Height (inches):")
    @Key("heightInchesColon")
    String heightInchesColon();

    @DefaultMessage("Engine path:")
    @Key("enginePathColon")
    String enginePathColon();

    @DefaultMessage("Select Engine")
    @Key("selectEngine")
    String selectEngine();

    @DefaultMessage("Engine options:")
    @Key("engineOptionsColon")
    String engineOptionsColon();

    @DefaultMessage("Chunk options")
    @Key("chunkOptions")
    String chunkOptions();

    @DefaultMessage("Revert")
    @Key("revertCapitalized")
    String revertCapitalized();

    @DefaultMessage("Apply")
    @Key("applyCapitalized")
    String applyCapitalized();

    @DefaultMessage("Show nothing (don''t run code)")
    @Key("showNothingDontRunCode")
    String showNothingDontRunCode();

    @DefaultMessage("Show nothing (run code)")
    @Key("showNothingRunCode")
    String showNothingRunCode();

    @DefaultMessage("Show code and output")
    @Key("showCodeAndOutput")
    String showCodeAndOutput();

    @DefaultMessage("Show output only")
    @Key("showOutputOnly")
    String showOutputOnly();

    @DefaultMessage("(Use document default)")
    @Key("useDocumentDefaultParentheses")
    String useDocumentDefaultParentheses();

    @DefaultMessage("Default Chunk Options")
    @Key("defaultChunkOptions")
    String defaultChunkOptions();

    @DefaultMessage("Check Spelling")
    @Key("checkSpelling")
    String checkSpelling();

    @DefaultMessage("An error has occurred:\n\n{0}")
    @Key("anErrorHasOccurredMessage")
    String anErrorHasOccurredMessage(String eMessage);

    @DefaultMessage("Spell check is complete.")
    @Key("spellCheckIsComplete")
    String spellCheckIsComplete();

    @DefaultMessage("Spell check in progress...")
    @Key("spellCheckInProgress")
    String spellCheckInProgress();

    @DefaultMessage("Add")
    @Key("addCapitalized")
    String addCapitalized();

    @DefaultMessage("Add word to user dictionary")
    @Key("addWordToUserDictionary")
    String addWordToUserDictionary();

    @DefaultMessage("Skip")
    @Key("skip")
    String skip();

    @DefaultMessage("Ignore All")
    @Key("ignoreAll")
    String ignoreAll();

    @DefaultMessage("Change")
    @Key("changeCapitalized")
    String changeCapitalized();

    @DefaultMessage("Change All")
    @Key("changeAll")
    String changeAll();

    @DefaultMessage("Suggestions")
    @Key("suggestionsCapitalized")
    String suggestionsCapitalized();

    @DefaultMessage("Checking...")
    @Key("checkingEllipses")
    String checkingEllipses();

    @DefaultMessage("Class")
    @Key("classCapitalized")
    String classCapitalized();

    @DefaultMessage("Namespace")
    @Key("namespaceCapitalized")
    String namespaceCapitalized();

    @DefaultMessage("Lambda")
    @Key("lambdaCapitalized")
    String lambdaCapitalized();

    @DefaultMessage("Anonymous")
    @Key("anonymousCapitalized")
    String anonymousCapitalized();

    @DefaultMessage("Function")
    @Key("functionCapitalized")
    String functionCapitalized();

    @DefaultMessage("Test")
    @Key("testCapitalized")
    String testCapitalized();

    @DefaultMessage("Chunk")
    @Key("chunkCapitalized")
    String chunkCapitalized();

    @DefaultMessage("Section")
    @Key("sectionCapitalized")
    String sectionCapitalized();

    @DefaultMessage("Slide")
    @Key("slideCapitalized")
    String slideCapitalized();

    @DefaultMessage("Tomorrow Night")
    @Key("tomorrowNight")
    String tomorrowNight();

    @DefaultMessage("Textmate (default)")
    @Key("textmateDefaultParentheses")
    String textmateDefaultParentheses();

    @DefaultMessage("The specified theme does not exist")
    @Key("specifiedThemeDoesNotExist")
    String specifiedThemeDoesNotExist();

    @DefaultMessage("The specified theme is a default RStudio theme and cannot be removed.")
    @Key("specifiedDefaultThemeCannotBeRemoved")
    String specifiedDefaultThemeCannotBeRemoved();

    @DefaultMessage("Choose Encoding")
    @Key("chooseEncoding")
    String chooseEncoding();

    @DefaultMessage("Encodings")
    @Key("encodingsCapitalized")
    String encodingsCapitalized();

    @DefaultMessage("Show all encodings")
    @Key("showAllEncodings")
    String showAllEncodings();

    @DefaultMessage("Set as default encoding for source files")
    @Key("setAsDefaultEncodingSourceFiles")
    String setAsDefaultEncodingSourceFiles();

    @DefaultMessage("{0} (System default)")
    @Key("sysEncNameDefault")
    String sysEncNameDefault(String sysEncName);

    @DefaultMessage("[Ask]")
    @Key("askSquareBrackets")
    String askSquareBrackets();

    @DefaultMessage("New R Documentation File")
    @Key("newRDocumentationFile")
    String newRDocumentationFile();

    @DefaultMessage("Name Not Specified")
    @Key("nameNotSpecified")
    String nameNotSpecified();

    @DefaultMessage("You must specify a topic name for the new Rd file.")
    @Key("mustSpecifyTopicNameForRdFile")
    String mustSpecifyTopicNameForRdFile();

    @DefaultMessage("New R Markdown")
    @Key("newRMarkdown")
    String newRMarkdown();

    @DefaultMessage("Templates")
    @Key("templatesCapitalized")
    String templatesCapitalized();

    @DefaultMessage("Create Empty Document")
    @Key("createEmptyDocument")
    String createEmptyDocument();

    @DefaultMessage("Using Shiny with R Markdown")
    @Key("usingShinyWithRMarkdown")
    String usingShinyWithRMarkdown();

    @DefaultMessage("Create an HTML document with interactive Shiny components.")
    @Key("shinyDocNameDescription")
    String shinyDocNameDescription();

    @DefaultMessage("Create an IOSlides presentation with interactive Shiny components.")
    @Key("shinyPresentationNameDescription")
    String shinyPresentationNameDescription();

    @DefaultMessage("From Template")
    @Key("fromTemplate")
    String fromTemplate();

    @DefaultMessage("Shiny")
    @Key("shinyCapitalized")
    String shinyCapitalized();

    @DefaultMessage("Shiny Document")
    @Key("shinyDocument")
    String shinyDocument();

    @DefaultMessage("Shiny Presentation")
    @Key("shinyPresentation")
    String shinyPresentation();

    @DefaultMessage("No Parameters Defined")
    @Key("noParametersDefined")
    String noParametersDefined();

    @DefaultMessage("There are no parameters defined for the current R Markdown document.")
    @Key("noParametersDefinedForCurrentRMarkdown")
    String noParametersDefinedForCurrentRMarkdown();

    @DefaultMessage("Using R Markdown Parameters")
    @Key("usingRMarkdownParameters")
    String usingRMarkdownParameters();

    @DefaultMessage("Unable to activate visual mode (unsupported front matter format or non top-level YAML block)")
    @Key("unableToActivateVisualModeYAML")
    String unableToActivateVisualModeYAML();

    @DefaultMessage("Unable to activate visual mode (error parsing code chunks out of document)")
    @Key("unableToActivateVisualModeParsingCode")
    String unableToActivateVisualModeParsingCode();

    @DefaultMessage("Unable to activate visual mode (document contains example lists which are not currently supported)")
    @Key("unableToActivateVisualModeDocumentContains")
    String unableToActivateVisualModeDocumentContains();

    @DefaultMessage("Unrecognized Pandoc token(s); {0}")
    @Key("unrecognizedPandocTokens")
    String unrecognizedPandocTokens(String tokens);

    @DefaultMessage("Invalid Pandoc format: {0}")
    @Key("invalidPandocFormat")
    String invalidPandocFormat(String format);

    @DefaultMessage("Unsupported extensions for markdown mode: {0}")
    @Key("unsupportedExtensionsForMarkdown")
    String unsupportedExtensionsForMarkdown(String format);

    @DefaultMessage("Unable to parse markdown (please report at https://github.com/rstudio/rstudio/issues/new)")
    @Key("unableToParseMarkdownPleaseReport")
    String unableToParseMarkdownPleaseReport();

    @DefaultMessage("Chunk {0}")
    @Key("chunkSequence")
    String chunkSequence(int itemSequence);

    @DefaultMessage("(Top Level)")
    @Key("topLevelParentheses")
    String topLevelParentheses();

    @DefaultMessage("You cannot enter visual mode while using realtime collaboration.")
    @Key("cantEnterVisualModeUsingRealtime")
    String cantEnterVisualModeUsingRealtime();

    @DefaultMessage("{0}, {1} line")
    @Key("visualModeChunkSummary")
    String visualModeChunkSummary(String enging, int lines);

    @DefaultMessage("{0}, {1} lines")
    @Key("visualModeChunkSummaryPlural")
    String visualModeChunkSummaryPlural(String engine, int lines);

    @DefaultMessage("images")
    @Key("images")
    String images();

    @DefaultMessage("Xaringan presentations cannot be edited in visual mode.")
    @Key("xaringanPresentationsVisualMode")
    String xaringanPresentationsVisualMode();

    @DefaultMessage("Version control conflict markers detected. Please resolve them before editing in visual mode.")
    @Key("versionControlConflict")
    String versionControlConflict();

    @DefaultMessage("Switch to Visual Mode")
    @Key("switchToVisualMode")
    String switchToVisualMode();

    @DefaultMessage("Use Visual Mode")
    @Key("useVisualMode")
    String useVisualMode();

    @DefaultMessage("Don''t show this message again")
    @Key("dontShowMessageAgain")
    String dontShowMessageAgain();

    @DefaultMessage("Line Wrapping")
    @Key("lineWrapping")
    String lineWrapping();

    @DefaultMessage("project default")
    @Key("projectDefault")
    String projectDefault();

    @DefaultMessage("global default")
    @Key("globalDefault")
    String globalDefault();

    @DefaultMessage("Line wrapping in this document differs from the {0}:")
    @Key("lineWrappingDiffersFromCurrent")
    String lineWrappingDiffersFromCurrent(String current);

    @DefaultMessage("Select how you''d like to handle line wrapping below:")
    @Key("selectHandleLineWrapping")
    String selectHandleLineWrapping();

    @DefaultMessage("Use {0}-based line wrapping for this document")
    @Key("useBasedLineWrappingForDocument")
    String useBasedLineWrappingForDocument(String detectedLineWrappingForDocument);
    

    @DefaultMessage("Use {0}-based line wrapping for this project")
    @Key("useBasedLineWrappingForProject")
    String useBasedLineWrappingForProject(String detectedLineWrappingForProject);

    @DefaultMessage("Use the current {0} default line wrapping for this document")
    @Key("useDefaultLinewrapping")
    String useDefaultLinewrapping(String projectConfigChoice);

    @DefaultMessage("project")
    @Key("project")
    String project();

    @DefaultMessage("global")
    @Key("global")
    String global();

    @DefaultMessage("Learn more about visual mode line wrapping options")
    @Key("learnAboutVisualModeLineWrapping")
    String learnAboutVisualModeLineWrapping();

    @DefaultMessage("Wrap at column:")
    @Key("wrapAtColumnColon")
    String wrapAtColumnColon();

    @DefaultMessage("Collapse")
    @Key("collapseCapitalized")
    String collapseCapitalized();

    @DefaultMessage("Expand")
    @Key("expandCapitalized")
    String expandCapitalized();

    @DefaultMessage("{0} code chunk")
    @Key("collapseOrExpandCodeChunk")
    String collapseOrExpandCodeChunk(String hintText);

    @DefaultMessage("Some of your source edits are still being backed up. If you continue, your latest changes may be lost. Do you want to continue?")
    @Key("editsStillBeingBackedUp")
    String editsStillBeingBackedUp();

    @DefaultMessage("The process cannot access the file because it is being used by another process")
    @Key("processStillBeingUsedTextEditingTarget")
    String processStillBeingUsedTextEditingTarget();

    @DefaultMessage("Error Autosaving File")
    @Key("errorAutosavingFile")
    String errorAutosavingFile();

    @DefaultMessage("RStudio was unable to autosave this file. You may need to restart RStudio.")
    @Key("rStudioUnableToAutosave")
    String rStudioUnableToAutosave();

    @DefaultMessage("Could not save {0}: {1}")
    @Key("couldNotSavePathPlusMessage")
    String couldNotSavePathPlusMessage(String path, String message);

    @DefaultMessage("Error saving {0}: {1}")
    @Key("errorSavingPathPlusMessage")
    String errorSavingPathPlusMessage(String path, String message);

    @DefaultMessage("Document Outline")
    @Key("documentOutline")
    String documentOutline();

    @DefaultMessage("<td><input type=''button'' value=''More...'' data-action=''open''></input></td>")
    @Key("moreButtonCell")
    String moreButtonCell();

    @DefaultMessage("{0} <...truncated...> )")
    @Key("truncatedEllipses")
    String truncatedEllipses(String truncated);

    @DefaultMessage("The document uses {0}-based line wrapping")
    @Key("documentUsesBasedLineWrapping")
    String documentUsesBasedLineWrapping(String detectedLineWrapping);

    @DefaultMessage("The {0} is no line wrapping")
    @Key("defaultNoLineWrapping")
    String defaultNoLineWrapping(String current);

    @DefaultMessage("The {0} is {1}-based line wrapping")
    @Key("defaultConfiguredBasedLineWrapping")
    String defaultConfiguredBasedLineWrapping(String current, String configuredLineWrapping);

    @DefaultMessage("Create R Notebook")
    @Key("createRNotebookText")
    String createRNotebookText();

    @DefaultMessage("Creating Shiny applications")
    @Key("creatingShinyApplicationsText")
    String creatingShinyApplicationsText();

    @DefaultMessage("Authoring R Presentations")
    @Key("authoringRPresentationsText")
    String authoringRPresentationsText();

    @DefaultMessage("Creating R Plumber API")
    @Key("creatingRPlumberAPIText")
    String creatingRPlumberAPIText();

    @DefaultMessage("Using R Notebooks")
    @Key("usingRNotebooksText")
    String usingRNotebooksText();

    @DefaultMessage("The profiler")
    @Key("theProfilerText")
    String theProfilerText();

    @DefaultMessage("Could not resolve {0}. Please make sure this is an R package project with a BugReports field set.")
    @Key("couldNotResolveIssue")
    String couldNotResolveIssue(String issue);

    @DefaultMessage("Unable to Edit")
    @Key("unableToEditTitle")
    String unableToEditTitle();

    @DefaultMessage("The options use syntax that is not currently supported by the editing UI. Please edit manually.")
    @Key("unableToEditMessage")
    String unableToEditMessage();
}

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
    /**
     * Translated "(No documents)".
     */
    @DefaultMessage("(No documents)")
    @Key("noDocumentsParentheses")
    String noDocumentsParentheses();

    /**
     * Translated "No outline available".
     */
    @DefaultMessage("No outline available")
    @Key("noOutlineAvailable")
    String noOutlineAvailable();

    /**
     * Translated "Title".
     */
    @DefaultMessage("Title")
    @Key("title")
    String title();

    /**
     * Translated "Invalid API Name".
     */
    @DefaultMessage("Invalid API Name")
    @Key("invalidApiName")
    String invalidApiName();

    /**
     * Translated "Invalid application name".
     */
    @DefaultMessage("Invalid application name")
    @Key("invalidApplicationName")
    String invalidApplicationName();

    /**
     * Translated "The API name must not be empty".
     */
    @DefaultMessage("The API name must not be empty")
    @Key("apiNameMustNotBeEmpty")
    String apiNameMustNotBeEmpty();

    /**
     * Translated "Plumber APIs".
     */
    @DefaultMessage("Plumber APIs")
    @Key("plumberAPIs")
    String plumberAPIs();

    /**
     * Translated "Create within directory:".
     */
    @DefaultMessage("Create within directory:")
    @Key("createWithinDirectoryColon")
    String createWithinDirectoryColon();

    /**
     * Translated "API name:".
     */
    @DefaultMessage("API name:")
    @Key("apiNameColon")
    String apiNameColon();

    /**
     * Translated "Create".
     */
    @DefaultMessage("Create")
    @Key("create")
    String create();

    /**
     * Translated "Application name:".
     */
    @DefaultMessage("Application name:")
    @Key("applicationNameColon")
    String applicationNameColon();

    /**
     * Translated "Application type:".
     */
    @DefaultMessage("Application type:")
    @Key("applicationTypeColon")
    String applicationTypeColon();

    /**
     * Translated "Single File (app.R)".
     */
    @DefaultMessage("Single File (app.R)")
    @Key("singleFileAppR")
    String singleFileAppR();

    /**
     * Translated "Multiple File (ui.R/server.R)".
     */
    @DefaultMessage("Multiple File (ui.R/server.R)")
    @Key("multipleFileUiServerR")
    String multipleFileUiServerR();

    /**
     * Translated "Shiny Web Applications".
     */
    @DefaultMessage("Shiny Web Applications")
    @Key("shinyWebApplications")
    String shinyWebApplications();

    /**
     * Translated "The application name must not be empty".
     */
    @DefaultMessage("The application name must not be empty")
    @Key("applicationNameMustNotBeEmpty")
    String applicationNameMustNotBeEmpty();

    /**
     * Translated "Invalid Application Name".
     */
    @DefaultMessage("Invalid Application Name")
    @Key("invalidApplicationNameCapitalized")
    String invalidApplicationNameCapitalized();

    /**
     * Translated "New Quarto {0}...".
     */
    @DefaultMessage("New Quarto {0}...")
    @Key("newQuatroProgressIndicator")
    String newQuatroProgressIndicator(String presentationType);

    /**
     * Translated "Presentation".
     */
    @DefaultMessage("Presentation")
    @Key("presentationCapitalized")
    String presentationCapitalized();

    /**
     * Translated "Document".
     */
    @DefaultMessage("Document")
    @Key("documentCapitalized")
    String documentCapitalized();

    /**
     * Translated "Error".
     */
    @DefaultMessage("Error")
    @Key("errorCapitalized")
    String errorCapitalized();

    /**
     * Translated "Always save files before build".
     */
    @DefaultMessage("Always save files before build")
    @Key("alwaysSaveFilesBeforeBuild")
    String alwaysSaveFilesBeforeBuild();

    /**
     * Translated "Show".
     */
    @DefaultMessage("Show")
    @Key("show")
    String show();

    /**
     * Translated "Source Document Error".
     */
    @DefaultMessage("Source Document Error")
    @Key("sourceDocumentError")
    String sourceDocumentError();

    /**
     * Translated "Show Profiler".
     */
    @DefaultMessage("Show Profiler")
    @Key("showProfiler")
    String showProfiler();

    /**
     * Translated "R Notebook".
     */
    @DefaultMessage("R Notebook")
    @Key("rNotebook")
    String rNotebook();

    /**
     * Translated "Notebook Creation Failed".
     */
    @DefaultMessage("Notebook Creation Failed")
    @Key("notebookCreationFailed")
    String notebookCreationFailed();

    /**
     * Translated "One or more packages required for R Notebook creation were not installed.".
     */
    @DefaultMessage("One or more packages required for R Notebook creation were not installed.")
    @Key("rNotebookCreationFailedPackagesNotInstalled")
    String rNotebookCreationFailedPackagesNotInstalled();

    /**
     * Translated "Creating Stan script".
     */
    @DefaultMessage("Creating Stan script")
    @Key("creatingStanScript")
    String creatingStanScript();

    /**
     * Translated "Creating Stan scripts".
     */
    @DefaultMessage("Creating Stan scripts")
    @Key("creatingStanScriptPlural")
    String creatingStanScriptPlural();

    /**
     * Translated "Creating new document...".
     */
    @DefaultMessage("Creating new document...")
    @Key("creatingNewDocument")
    String creatingNewDocument();

    /**
     * Translated "Error Creating Shiny Application".
     */
    @DefaultMessage("Error Creating Shiny Application")
    @Key("errorCreatingShinyApplication")
    String errorCreatingShinyApplication();

    /**
     * Translated "Error Creating Plumber API".
     */
    @DefaultMessage("Error Creating Plumber API")
    @Key("errorCreatingPlumberApi")
    String errorCreatingPlumberApi();

    /**
     * Translated "New Shiny Web Application".
     */
    @DefaultMessage("New Shiny Web Application")
    @Key("newShinyWebApplication")
    String newShinyWebApplication();

    /**
     * Translated "New R Presentation".
     */
    @DefaultMessage("New R Presentation")
    @Key("newRPresentation")
    String newRPresentation();

    /**
     * Translated "Creating Presentation...".
     */
    @DefaultMessage("Creating Presentation...")
    @Key("creatingPresentation")
    String creatingPresentation();

    /**
     * Translated "Document Tab Move Failed".
     */
    @DefaultMessage("Document Tab Move Failed")
    @Key("documentTabMoveFailed")
    String documentTabMoveFailed();

    /**
     * Translated "Couldn''t move the tab to this window: \n{0}".
     */
    @DefaultMessage("Couldn''t move the tab to this window: \n{0}")
    @Key("couldntMoveTabToWindowError")
    String couldntMoveTabToWindowError(String errorMessage);

    /**
     * Translated "Close All".
     */
    @DefaultMessage("Close All")
    @Key("closeAll")
    String closeAll();

    /**
     * Translated "Close Other".
     */
    @DefaultMessage("Close Other")
    @Key("closeOther")
    String closeOther();

    /**
     * Translated "Open File".
     */
    @DefaultMessage("Open File")
    @Key("openFile")
    String openFile();

    /**
     * Translated "New Plumber API".
     */
    @DefaultMessage("New Plumber API")
    @Key("newPlumberApi")
    String newPlumberApi();

    /**
     * Translated "Close All Others".
     */
    @DefaultMessage("Close All Others")
    @Key("closeAllOthers")
    String closeAllOthers();

    /**
     * Translated "Error Creating New Document".
     */
    @DefaultMessage("Error Creating New Document")
    @Key("errorCreatingNewDocument")
    String errorCreatingNewDocument();

    /**
     * Translated "Publish Document...".
     */
    @DefaultMessage("Publish Document...")
    @Key("publishDocument")
    String publishDocument();

    /**
     * Translated "Publish Plumber API...".
     */
    @DefaultMessage("Publish Plumber API...")
    @Key("publishPlumberApi")
    String publishPlumberApi();

    /**
     * Translated "Publish Application...".
     */
    @DefaultMessage("Publish Application...")
    @Key("publishApplication")
    String publishApplication();

    // TODO IF THESE ARE SHORTCUTS NEED TO DOUBLE CHECK THESE
    /**
     * Translated "_Blame \"{0}\" on GitHub".
     */
    @DefaultMessage("_Blame \"{0}\" on GitHub")
    @Key("blameOnGithub")
    String blameOnGithub(String name);

    /**
     * Translated "_View \"{0}\" on GitHub".
     */
    @DefaultMessage("_View \"{0}\" on GitHub")
    @Key("viewNameOnGithub")
    String viewNameOnGithub(String name);

    /**
     * Translated "_Revert \"{0}\"...".
     */
    @DefaultMessage("_Revert \"{0}\"...")
    @Key("revertName")
    String revertName(String name);

    /**
     * Translated "_Log of \"{0}\"".
     */
    @DefaultMessage("_Log of \"{0}\"")
    @Key("logOfName")
    String logOfName(String name);

    /**
     * Translated "_Diff \"{0}\"".
     */
    @DefaultMessage("_Diff \"{0}\"")
    @Key("diffName")
    String diffName(String name);

    /**
     * Translated "No document tabs open".
     */
    @DefaultMessage("No document tabs open")
    @Key("noDocumentTabsOpen")
    String noDocumentTabsOpen();

    /**
     * Translated "Show Object Explorer".
     */
    @DefaultMessage("Show Object Explorer")
    @Key("showObjectExplorer")
    String showObjectExplorer();

    /**
     * Translated "Show Data Frame".
     */
    @DefaultMessage("Show Data Frame")
    @Key("showDataFrame")
    String showDataFrame();

    /**
     * Translated "Template Content Missing".
     */
    @DefaultMessage("Template Content Missing")
    @Key("templateContentMissing")
    String templateContentMissing();

    /**
     * Translated "The template at {0} is missing.".
     */
    @DefaultMessage("The template at {0} is missing.")
    @Key("templateAtPathMissing")
    String templateAtPathMissing(String templatePath);

    /**
     * Translated "Error while opening file".
     */
    @DefaultMessage("Error while opening file")
    @Key("errorWhileOpeningFile")
    String errorWhileOpeningFile();

    /**
     * Translated "This notebook has the same name as an R Markdown file, but doesn''t match it".
     */
    @DefaultMessage("This notebook has the same name as an R Markdown file, but doesn''t match it")
    @Key("openNotebookWarningMessage")
    String openNotebookWarningMessage();

    /**
     * Translated "Notebook Open Failed".
     */
    @DefaultMessage("Notebook Open Failed")
    @Key("notebookOpenFailed")
    String notebookOpenFailed();

    /**
     * Translated "This notebook could not be opened. If the error persists, try removing the accompanying R Markdown file. \n\n{0}".
     */
    @DefaultMessage("This notebook could not be opened. If the error persists, try removing the accompanying R Markdown file. \\n\\n{0}")
    @Key("notebookOpenFailedMessage")
    String notebookOpenFailedMessage(String errorMessage);

    /**
     * Translated "This notebook could not be opened. \n\n{0}".
     */
    @DefaultMessage("This notebook could not be opened. \n\n{0}")
    @Key("notebookCouldNotBeOpenedMessage")
    String notebookCouldNotBeOpenedMessage(String errorMessage);

    /**
     * Translated "Opening file...".
     */
    @DefaultMessage("Opening file...")
    @Key("openingFile")
    String openingFile();

    /**
     * Translated "The file ''{0}'' is too large to open in the source editor (the file is {1} and the maximum file size is {2})".
     */
    @DefaultMessage("The file ''{0}'' is too large to open in the source editor (the file is {1} and the maximum file size is {2})")
    @Key("showFileTooLargeWarningMsg")
    String showFileTooLargeWarningMsg(String filename, String filelength, String sizeLimit);

    /**
     * Translated "Selected File Too Large".
     */
    @DefaultMessage("Selected File Too Large")
    @Key("selectedFileTooLarge")
    String selectedFileTooLarge();

    /**
     * Translated "The source file ''{0}'' is large ({1}) and may take some time to open. Are you sure you want to continue opening it?".
     */
    @DefaultMessage("The source file ''{0}'' is large ({1}) and may take some time to open. Are you sure you want to continue opening it?")
    @Key("confirmOpenLargeFileMsg")
    String confirmOpenLargeFileMsg(String filename, String length);

    /**
     * Translated "Confirm Open".
     */
    @DefaultMessage("Confirm Open")
    @Key("confirmOpen")
    String confirmOpen();

    /**
     * Translated "Source Column {0}".
     */
    @DefaultMessage("Source Column {0}")
    @Key("sourceColumn")
    String sourceColumn(int columnCounter);

    /**
     * Translated "Source".
     */
    @DefaultMessage("Source")
    @Key("source")
    String source();

    /**
     * Translated "Switch to tab".
     */
    @DefaultMessage("Switch to tab")
    @Key("switchToTab")
    String switchToTab();

    /**
     * Translated "RStudio Source Editor".
     */
    @DefaultMessage("RStudio Source Editor")
    @Key("rstudioSourceEditor")
    String rstudioSourceEditor();

    /**
     * Translated "Close Source Window".
     */
    @DefaultMessage("Close Source Window")
    @Key("closeSourceWindow")
    String closeSourceWindow();

    /**
     * Translated "Cancel".
     */
    @DefaultMessage("Cancel")
    @Key("cancel")
    String cancel();

    /**
     * Translated "Close and Discard Changes".
     */
    @DefaultMessage("Close and Discard Changes")
    @Key("closeAndDiscardChanges")
    String closeAndDiscardChanges();

    /**
     * Translated "Unsaved Changes".
     */
    @DefaultMessage("Unsaved Changes")
    @Key("unsavedChanges")
    String unsavedChanges();

    /**
     * Translated "There are unsaved documents in this window. Are you sure you want to close it?".
     */
    @DefaultMessage("There are unsaved documents in this window. Are you sure you want to close it?")
    @Key("confirmCloseUnsavedDocuments")
    String confirmCloseUnsavedDocuments();

    /**
     * Translated "Your edits to the file {0} have not been saved".
     */
    @DefaultMessage("Your edits to the file {0} have not been saved")
    @Key("yourEditsToFileHasNotBeenSaved")
    String yourEditsToFileHasNotBeenSaved(String desc);

    /**
     * Translated "Your edits to the files {0} have not been saved".
     */
    @DefaultMessage("Your edits to the files {0} have not been saved")
    @Key("yourEditsToFilePluralHasNotBeenSaved")
    String yourEditsToFilePluralHasNotBeenSaved(String completeMessage);

    /**
     * Translated ", ".
     */
    @DefaultMessage(", ")
    @Key("commaListSeparator")
    String commaListSeparator();

    /**
     * Translated " and ".
     */
    @DefaultMessage(" and ")
    @Key("andForList")
    String andForList();

    /**
     * Translated "Can''t Move Doc".
     */
    @DefaultMessage("Can''t Move Doc")
    @Key("cantMoveDoc")
    String cantMoveDoc();

    /**
     * Translated "The document could not be moved to a different window: \n{0}".
     */
    @DefaultMessage("The document could not be moved to a different window: \n{0}")
    @Key("cantMoveDocMessage")
    String cantMoveDocMessage(String errorMessage);

    /**
     * Translated "Search tabs".
     */
    @DefaultMessage("Search tabs")
    @Key("searchTabs")
    String searchTabs();

    /**
     * Translated "Code Editor Tab".
     */
    @DefaultMessage("Code Editor Tab")
    @Key("codeEditorTab")
    String codeEditorTab();

    /**
     * Translated "Function:".
     */
    @DefaultMessage("Function:")
    @Key("functionColon")
    String functionColon();

    /**
     * Translated "Method:".
     */
    @DefaultMessage("Method:")
    @Key("methodColon")
    String methodColon();

    /**
     * Translated "Error Reading Function Definition".
     */
    @DefaultMessage("Error Reading Function Definition")
    @Key("errorReadingFunctionDefinition")
    String errorReadingFunctionDefinition();

    /**
     * Translated "Code Browser displayed".
     */
    @DefaultMessage("Code Browser displayed")
    @Key("codeBrowserDisplayed")
    String codeBrowserDisplayed();

    /**
     * Translated "Debug location is approximate because the source is not available.".
     */
    @DefaultMessage("Debug location is approximate because the source is not available.")
    @Key("debugLocationIsApproximate")
    String debugLocationIsApproximate();

    /**
     * Translated "R Source Viewer".
     */
    @DefaultMessage("R Source Viewer")
    @Key("rSourceViewer")
    String rSourceViewer();

    /**
     * Translated "(Read-only)".
     */
    @DefaultMessage("(Read-only)")
    @Key("readOnlyParentheses")
    String readOnlyParentheses();

    /**
     * Translated "Code Browser Second".
     */
    @DefaultMessage("Code Browser Second")
    @Key("codeBrowserSecond")
    String codeBrowserSecond();

    /**
     * Translated "Code Tools".
     */
    @DefaultMessage("Code Tools")
    @Key("codeTools")
    String codeTools();

    /**
     * Translated "Error Searching for Function".
     */
    @DefaultMessage("Error Searching for Function")
    @Key("errorSearchingForFunction")
    String errorSearchingForFunction();

    /**
     * Translated "Searching for function definition...".
     */
    @DefaultMessage("Searching for function definition...")
    @Key("searchingForFunctionDefinition")
    String searchingForFunctionDefinition();

    /**
     * Translated "{0} Source Viewer".
     */
    @DefaultMessage("{0} Source Viewer")
    @Key("nameSourceViewer")
    String nameSourceViewer(String name);

    /**
     * Translated "Untitled Source Viewer".
     */
    @DefaultMessage("Untitled Source Viewer")
    @Key("untitledSourceViewer")
    String untitledSourceViewer();

    /**
     * Translated "Help".
     */
    @DefaultMessage("Help")
    @Key("help")
    String help();

    /**
     * Translated "Data Browser displayed".
     */
    @DefaultMessage("Data Browser displayed")
    @Key("dataBrowserDisplayed")
    String dataBrowserDisplayed();

    /**
     * Translated "Data Browser".
     */
    @DefaultMessage("Data Browser")
    @Key("dataBrowser")
    String dataBrowser();

    /**
     * Translated "{0} Data Browser".
     */
    @DefaultMessage("{0} Data Browser")
    @Key("accessibleNameDataBrowser")
    String accessibleNameDataBrowser(String accessibleName);

    /**
     * Translated "Untitled Data Browser".
     */
    @DefaultMessage("Untitled Data Browser")
    @Key("untitledDataBrowser")
    String untitledDataBrowser();

    /**
     * Translated "Displayed {0} rows of {1}".
     */
    @DefaultMessage("Displayed {0} rows of {1}")
    @Key("dataEditingTargetWidgetLabel1")
    String dataEditingTargetWidgetLabel1(String displayedObservations, String totalObservations);

    /**
     * Translated "({0} omitted)".
     */
    @DefaultMessage("({0} omitted)")
    @Key("dataEditingTargetWidgetLabel2")
    String dataEditingTargetWidgetLabel2(String omittedNumber);

    /**
     * Translated "{0} Object Explorer".
     */
    @DefaultMessage("{0} Object Explorer")
    @Key("accessibleNameObjectExplorer")
    String accessibleNameObjectExplorer(String accessibleName);

    /**
     * Translated "Untitled Object Explorer".
     */
    @DefaultMessage("Untitled Object Explorer")
    @Key("untitledObjectExplorer")
    String untitledObjectExplorer();

    /**
     * Translated "Object Explorer displayed".
     */
    @DefaultMessage("Object Explorer displayed")
    @Key("objectExplorerDisplayed")
    String objectExplorerDisplayed();

    /**
     * Translated "URL Viewer displayed".
     */
    @DefaultMessage("URL Viewer displayed")
    @Key("urlViewerDisplayed")
    String urlViewerDisplayed();

    /**
     * Translated "URL Browser".
     */
    @DefaultMessage("URL Browser")
    @Key("urlBrowser")
    String urlBrowser();

    /**
     * Translated "{0} URL Browser".
     */
    @DefaultMessage("{0} URL Browser")
    @Key("accessibleNameBrowser")
    String accessibleNameBrowser(String name);

    /**
     * Translated "Untitled URL Browser".
     */
    @DefaultMessage("Untitled URL Browser")
    @Key("untitledUrlBrowser")
    String untitledUrlBrowser();

    /**
     * Translated "Source Viewer".
     */
    @DefaultMessage("Source Viewer")
    @Key("sourceViewer")
    String sourceViewer();

    /**
     * Translated "Name".
     */
    @DefaultMessage("Name")
    @Key("name")
    String name();

    /**
     * Translated "Type".
     */
    @DefaultMessage("Type")
    @Key("type")
    String type();

    /**
     * Translated "Value".
     */
    @DefaultMessage("Value")
    @Key("value")
    String value();

    /**
     * Translated "View({0})".
     */
    @DefaultMessage("View({0})")
    @Key("viewCode")
    String viewCode(String code);

    /**
     * Translated "(No selection)".
     */
    @DefaultMessage("(No selection)")
    @Key("noSelectionParentheses")
    String noSelectionParentheses();

    /**
     * Translated "Show Attributes".
     */
    @DefaultMessage("Show Attributes")
    @Key("showAttributes")
    String showAttributes();

    /**
     * Translated "Search objects".
     */
    @DefaultMessage("Search objects")
    @Key("searchObjects")
    String searchObjects();

    /**
     * Translated "Refresh".
     */
    @DefaultMessage("Refresh")
    @Key("refresh")
    String refresh();

    /**
     * Translated "The source file {0} does not exist.".
     */
    @DefaultMessage("The source file {0} does not exist.")
    @Key("sourceFileAtPathDoesNotExist")
    String sourceFileAtPathDoesNotExist(String selectedPath);

    /**
     * Translated "Error while opening profiler source".
     */
    @DefaultMessage("Error while opening profiler source")
    @Key("errorOpeningProfilerSource")
    String errorOpeningProfilerSource();

    /**
     * Translated "Failed to Save Profile".
     */
    @DefaultMessage("Failed to Save Profile")
    @Key("failedToSaveProfile")
    String failedToSaveProfile();

    /**
     * Translated "Save File - {0}".
     */
    @DefaultMessage("Save File - {0}")
    @Key("saveFileName")
    String saveFileName(String name);

    /**
     * Translated "Failed to Save Profile Properties".
     */
    @DefaultMessage("Failed to Save Profile Properties")
    @Key("failedToSaveProfileProperties")
    String failedToSaveProfileProperties();

    /**
     * Translated "Code Profile results displayed".
     */
    @DefaultMessage("Code Profile results displayed")
    @Key("codeProfileResultsDisplayed")
    String codeProfileResultsDisplayed();

    /**
     * Translated "Profile".
     */
    @DefaultMessage("Profile")
    @Key("profileCapitalized")
    String profileCapitalized();

    /**
     * Translated "Profiler".
     */
    @DefaultMessage("Profiler")
    @Key("profilerCapitalized")
    String profilerCapitalized();

    /**
     * Translated "R Profiler".
     */
    @DefaultMessage("R Profiler")
    @Key("rProfiler")
    String rProfiler();

    /**
     * Translated "{0} Profile View".
     */
    @DefaultMessage("{0} Profile View")
    @Key("titleProfileView")
    String titleProfileView(String title);

    /**
     * Translated "Failed to Open Profile".
     */
    @DefaultMessage("Failed to Open Profile")
    @Key("failedToOpenProfile")
    String failedToOpenProfile();

    /**
     * Translated "Profiler Error".
     */
    @DefaultMessage("Profiler Error")
    @Key("profilerError")
    String profilerError();

    /**
     * Translated "Failed to Stop Profiler".
     */
    @DefaultMessage("Failed to Stop Profiler")
    @Key("failedToStopProfiler")
    String failedToStopProfiler();

    /**
     * Translated "Error navigating to file".
     */
    @DefaultMessage("Error navigating to file")
    @Key("errorNavigatingToFile")
    String errorNavigatingToFile();

    /**
     * Translated "No file at path ''{0}''.".
     */
    @DefaultMessage("No file at path ''{0}''.")
    @Key("noFileAtPath")
    String noFileAtPath(String finalUrl);

    /**
     * Translated "Open Link (Command+Click)".
     */
    @DefaultMessage("Open Link (Command+Click)")
    @Key("openLinkMacCommand")
    String openLinkMacCommand();

    /**
     * Translated "Open Link (Ctrl+Click)".
     */
    @DefaultMessage("Open Link (Ctrl+Click)")
    @Key("openLinkNotMacCommand")
    String openLinkNotMacCommand();

    /**
     * Translated "Finding definition...".
     */
    @DefaultMessage("Finding definition...")
    @Key("findingDefinition")
    String findingDefinition();

    /**
     * Translated "ignored".
     */
    @DefaultMessage("ignored")
    @Key("ignored")
    String ignored();

    /**
     * Translated "note".
     */
    @DefaultMessage("note")
    @Key("note")
    String note();

    /**
     * Translated "warning".
     */
    @DefaultMessage("warning")
    @Key("warningLowercase")
    String warningLowercase();

    /**
     * Translated "error".
     */
    @DefaultMessage("error")
    @Key("error")
    String error();

    /**
     * Translated "fatal".
     */
    @DefaultMessage("fatal")
    @Key("fatal")
    String fatal();

    /**
     * Translated "(No matches)".
     */
    @DefaultMessage("(No matches)")
    @Key("noMatchesParentheses")
    String noMatchesParentheses();

    /**
     * Translated "{0} of {1}".
     */
    @DefaultMessage("{0} of {1}")
    @Key("pagingLabelTextOf")
    String pagingLabelTextOf(int index, int textLength);

    /**
     * Translated "{0} occurrences replaced.".
     */
    @DefaultMessage("{0} occurrences replaced.")
    @Key("numberOfOccurrencesReplaced")
    String numberOfOccurrencesReplaced(int occurrences);

    /**
     * Translated "Invalid search term.".
     */
    @DefaultMessage("Invalid search term.")
    @Key("invalidSearchTerm")
    String invalidSearchTerm();

    /**
     * Translated "No more occurrences.".
     */
    @DefaultMessage("No more occurrences.")
    @Key("noMoreOccurrences")
    String noMoreOccurrences();

    /**
     * Translated "Find".
     */
    @DefaultMessage("Find")
    @Key("findCapitalized")
    String findCapitalized();

    /**
     * Translated "Find/Replace".
     */
    @DefaultMessage("Find/Replace")
    @Key("findOrReplace")
    String findOrReplace();

    /**
     * Translated "Replace".
     */
    @DefaultMessage("Replace")
    @Key("replaceCapitalized")
    String replaceCapitalized();

    /**
     * Translated "All".
     */
    @DefaultMessage("All")
    @Key("allCapitalized")
    String allCapitalized();

    /**
     * Translated "Replace all occurrences".
     */
    @DefaultMessage("Replace all occurrences")
    @Key("replaceAllOccurrences")
    String replaceAllOccurrences();

    /**
     * Translated "In selection".
     */
    @DefaultMessage("In selection")
    @Key("inSelection")
    String inSelection();

    /**
     * Translated "Match case".
     */
    @DefaultMessage("Match case")
    @Key("matchCase")
    String matchCase();

    /**
     * Translated "Whole word".
     */
    @DefaultMessage("Whole word")
    @Key("wholeWord")
    String wholeWord();

    /**
     * Translated "Regex".
     */
    @DefaultMessage("Regex")
    @Key("regexCapitalized")
    String regexCapitalized();

    /**
     * Translated "Wrap".
     */
    @DefaultMessage("Wrap")
    @Key("wrapCapitalized")
    String wrapCapitalized();

    /**
     * Translated "Close find and replace".
     */
    @DefaultMessage("Close find and replace")
    @Key("closeFindAndReplace")
    String closeFindAndReplace();

    /**
     * Translated "Chunk Pending Execution".
     */
    @DefaultMessage("Chunk Pending Execution")
    @Key("chunkPendingExecution")
    String chunkPendingExecution();

    /**
     * Translated "The code in this chunk is scheduled to run later, when other chunks have finished executing.".
     */
    @DefaultMessage("The code in this chunk is scheduled to run later, when other chunks have finished executing.")
    @Key("chunkPendingExecutionMessage")
    String chunkPendingExecutionMessage();

    /**
     * Translated "OK".
     */
    @DefaultMessage("OK")
    @Key("okFullyCapitalized")
    String okFullyCapitalized();

    /**
     * Translated "Don''t Run".
     */
    @DefaultMessage("Don''t Run")
    @Key("dontRun")
    String dontRun();

    /**
     * Translated "The rmarkdown package is not installed; notebook HTML file will not be generated.".
     */
    @DefaultMessage("The rmarkdown package is not installed; notebook HTML file will not be generated.")
    @Key("rMarkdownNotInstalledHTMLNoGenerate")
    String rMarkdownNotInstalledHTMLNoGenerate();

    /**
     * Translated "An updated version of the rmarkdown package is required to generate notebook HTML files.".
     */
    @DefaultMessage("An updated version of the rmarkdown package is required to generate notebook HTML files.")
    @Key("rMarkdownUpgradeRequired")
    String rMarkdownUpgradeRequired();

    /**
     * Translated "Error creating notebook: ".
     */
    @DefaultMessage("Error creating notebook: ")
    @Key("errorCreatingNotebookPrefix")
    String errorCreatingNotebookPrefix();

    /**
     * Translated "Creating R Notebooks".
     */
    @DefaultMessage("Creating R Notebooks")
    @Key("creatingRNotebooks")
    String creatingRNotebooks();

    /**
     * Translated "Can''t execute {0}".
     */
    @DefaultMessage("Can''t execute {0}")
    @Key("cantExecuteJobDesc")
    String cantExecuteJobDesc(String jobDesc);

    /**
     * Translated "Executing Python chunks".
     */
    @DefaultMessage("Executing Python chunks")
    @Key("executingPythonChunks")
    String executingPythonChunks();

    /**
     * Translated "Executing chunks".
     */
    @DefaultMessage("Executing chunks")
    @Key("executingChunks")
    String executingChunks();

    /**
     * Translated "Run Chunk".
     */
    @DefaultMessage("Run Chunk")
    @Key("runChunk")
    String runChunk();

    /**
     * Translated "{0}: Chunks Currently Executing".
     */
    @DefaultMessage("{0}: Chunks Currently Executing")
    @Key("jobChunkCurrentlyExecuting")
    String jobChunkCurrentlyExecuting(String jobDesc);

    /**
     * Translated "RStudio cannot execute ''{0}'' because this notebook is already executing code. Interrupt R, or wait for execution to complete.".
     */
    @DefaultMessage("RStudio cannot execute ''{0}'' because this notebook is already executing code. Interrupt R, or wait for execution to complete.")
    @Key("rStudioCannotExecuteJob")
    String rStudioCannotExecuteJob(String jobDesc);

    /**
     * Translated "Run Chunks".
     */
    @DefaultMessage("Run Chunks")
    @Key("runChunks")
    String runChunks();

    /**
     * Translated "Chunks Currently Running".
     */
    @DefaultMessage("Chunks Currently Running")
    @Key("chunksCurrentlyRunning")
    String chunksCurrentlyRunning();

    /**
     * Translated "Output can''t be cleared because there are still chunks running. Do you want to interrupt them?".
     */
    @DefaultMessage("Output can''t be cleared because there are still chunks running. Do you want to interrupt them?")
    @Key("outputCantBeClearedBecauseChunks")
    String outputCantBeClearedBecauseChunks();

    /**
     * Translated "Interrupt and Clear Output".
     */
    @DefaultMessage("Interrupt and Clear Output")
    @Key("interruptAndClearOutput")
    String interruptAndClearOutput();

    /**
     * Translated "Remove Inline Chunk Output".
     */
    @DefaultMessage("Remove Inline Chunk Output")
    @Key("removeInlineChunkOutput")
    String removeInlineChunkOutput();

    /**
     * Translated "Do you want to clear all the existing chunk output from your notebook?".
     */
    @DefaultMessage("Do you want to clear all the existing chunk output from your notebook?")
    @Key("clearExistingChunkOutputMessage")
    String clearExistingChunkOutputMessage();

    /**
     * Translated "Remove Output".
     */
    @DefaultMessage("Remove Output")
    @Key("removeOutput")
    String removeOutput();

    /**
     * Translated "Keep Output".
     */
    @DefaultMessage("Keep Output")
    @Key("keepOutput")
    String keepOutput();

    /**
     * Translated "Unnamed chunk".
     */
    @DefaultMessage("Unnamed chunk")
    @Key("unnamedChunk")
    String unnamedChunk();

    /**
     * Translated "Chunk Name:".
     */
    @DefaultMessage("Chunk Name:")
    @Key("chunkNameColon")
    String chunkNameColon();

    /**
     * Translated "Output:".
     */
    @DefaultMessage("Output:")
    @Key("outputColon")
    String outputColon();

    /**
     * Translated "Show warnings".
     */
    @DefaultMessage("Show warnings")
    @Key("showWarnings")
    String showWarnings();

    /**
     * Translated "Show messages".
     */
    @DefaultMessage("Show messages")
    @Key("showMessages")
    String showMessages();

    /**
     * Translated "Cache chunk".
     */
    @DefaultMessage("Cache chunk")
    @Key("cacheChunk")
    String cacheChunk();

    /**
     * Translated "Use paged tables".
     */
    @DefaultMessage("Use paged tables")
    @Key("usePagedTables")
    String usePagedTables();

    /**
     * Translated "Use custom figure size".
     */
    @DefaultMessage("Use custom figure size")
    @Key("useCustomFigureSize")
    String useCustomFigureSize();

    /**
     * Translated "Width (inches):".
     */
    @DefaultMessage("Width (inches):")
    @Key("widthInchesColon")
    String widthInchesColon();

    /**
     * Translated "Height (inches):".
     */
    @DefaultMessage("Height (inches):")
    @Key("heightInchesColon")
    String heightInchesColon();

    /**
     * Translated "Engine path:".
     */
    @DefaultMessage("Engine path:")
    @Key("enginePathColon")
    String enginePathColon();

    /**
     * Translated "Select Engine".
     */
    @DefaultMessage("Select Engine")
    @Key("selectEngine")
    String selectEngine();

    /**
     * Translated "Engine options:".
     */
    @DefaultMessage("Engine options:")
    @Key("engineOptionsColon")
    String engineOptionsColon();

    /**
     * Translated "Chunk options".
     */
    @DefaultMessage("Chunk options")
    @Key("chunkOptions")
    String chunkOptions();

    /**
     * Translated "Revert".
     */
    @DefaultMessage("Revert")
    @Key("revertCapitalized")
    String revertCapitalized();

    /**
     * Translated "Apply".
     */
    @DefaultMessage("Apply")
    @Key("applyCapitalized")
    String applyCapitalized();

    /**
     * Translated "Show nothing (don''t run code)".
     */
    @DefaultMessage("Show nothing (don''t run code)")
    @Key("showNothingDontRunCode")
    String showNothingDontRunCode();

    /**
     * Translated "Show nothing (run code)".
     */
    @DefaultMessage("Show nothing (run code)")
    @Key("showNothingRunCode")
    String showNothingRunCode();

    /**
     * Translated "Show code and output".
     */
    @DefaultMessage("Show code and output")
    @Key("showCodeAndOutput")
    String showCodeAndOutput();

    /**
     * Translated "Show output only".
     */
    @DefaultMessage("Show output only")
    @Key("showOutputOnly")
    String showOutputOnly();

    /**
     * Translated "(Use document default)".
     */
    @DefaultMessage("(Use document default)")
    @Key("useDocumentDefaultParentheses")
    String useDocumentDefaultParentheses();

    /**
     * Translated "Default Chunk Options".
     */
    @DefaultMessage("Default Chunk Options")
    @Key("defaultChunkOptions")
    String defaultChunkOptions();

    /**
     * Translated "Check Spelling".
     */
    @DefaultMessage("Check Spelling")
    @Key("checkSpelling")
    String checkSpelling();

    /**
     * Translated "An error has occurred:\n\n{0}".
     */
    @DefaultMessage("An error has occurred:\n\n{0}")
    @Key("anErrorHasOccurredMessage")
    String anErrorHasOccurredMessage(String eMessage);

    /**
     * Translated "Spell check is complete.".
     */
    @DefaultMessage("Spell check is complete.")
    @Key("spellCheckIsComplete")
    String spellCheckIsComplete();

    /**
     * Translated "Spell check in progress...".
     */
    @DefaultMessage("Spell check in progress...")
    @Key("spellCheckInProgress")
    String spellCheckInProgress();

    /**
     * Translated "Add".
     */
    @DefaultMessage("Add")
    @Key("addCapitalized")
    String addCapitalized();

    /**
     * Translated "Add word to user dictionary".
     */
    @DefaultMessage("Add word to user dictionary")
    @Key("addWordToUserDictionary")
    String addWordToUserDictionary();

    /**
     * Translated "Skip".
     */
    @DefaultMessage("Skip")
    @Key("skip")
    String skip();

    /**
     * Translated "Ignore All".
     */
    @DefaultMessage("Ignore All")
    @Key("ignoreAll")
    String ignoreAll();

    /**
     * Translated "Change".
     */
    @DefaultMessage("Change")
    @Key("changeCapitalized")
    String changeCapitalized();

    /**
     * Translated "Change All".
     */
    @DefaultMessage("Change All")
    @Key("changeAll")
    String changeAll();

    /**
     * Translated "Suggestions".
     */
    @DefaultMessage("Suggestions")
    @Key("suggestionsCapitalized")
    String suggestionsCapitalized();

    /**
     * Translated "Checking...".
     */
    @DefaultMessage("Checking...")
    @Key("checkingEllipses")
    String checkingEllipses();

    /**
     * Translated "Class".
     */
    @DefaultMessage("Class")
    @Key("classCapitalized")
    String classCapitalized();

    /**
     * Translated "Namespace".
     */
    @DefaultMessage("Namespace")
    @Key("namespaceCapitalized")
    String namespaceCapitalized();

    /**
     * Translated "Lambda".
     */
    @DefaultMessage("Lambda")
    @Key("lambdaCapitalized")
    String lambdaCapitalized();

    /**
     * Translated "Anonymous".
     */
    @DefaultMessage("Anonymous")
    @Key("anonymousCapitalized")
    String anonymousCapitalized();

    /**
     * Translated "Function".
     */
    @DefaultMessage("Function")
    @Key("functionCapitalized")
    String functionCapitalized();

    /**
     * Translated "Test".
     */
    @DefaultMessage("Test")
    @Key("testCapitalized")
    String testCapitalized();

    /**
     * Translated "Chunk".
     */
    @DefaultMessage("Chunk")
    @Key("chunkCapitalized")
    String chunkCapitalized();

    /**
     * Translated "Section".
     */
    @DefaultMessage("Section")
    @Key("sectionCapitalized")
    String sectionCapitalized();

    /**
     * Translated "Slide".
     */
    @DefaultMessage("Slide")
    @Key("slideCapitalized")
    String slideCapitalized();

    /**
     * Translated "Tomorrow Night".
     */
    @DefaultMessage("Tomorrow Night")
    @Key("tomorrowNight")
    String tomorrowNight();

    /**
     * Translated "Textmate (default)".
     */
    @DefaultMessage("Textmate (default)")
    @Key("textmateDefaultParentheses")
    String textmateDefaultParentheses();

    /**
     * Translated "The specified theme does not exist".
     */
    @DefaultMessage("The specified theme does not exist")
    @Key("specifiedThemeDoesNotExist")
    String specifiedThemeDoesNotExist();

    /**
     * Translated "The specified theme is a default RStudio theme and cannot be removed.".
     */
    @DefaultMessage("The specified theme is a default RStudio theme and cannot be removed.")
    @Key("specifiedDefaultThemeCannotBeRemoved")
    String specifiedDefaultThemeCannotBeRemoved();

    /**
     * Translated "Choose Encoding".
     */
    @DefaultMessage("Choose Encoding")
    @Key("chooseEncoding")
    String chooseEncoding();

    /**
     * Translated "Encodings".
     */
    @DefaultMessage("Encodings")
    @Key("encodingsCapitalized")
    String encodingsCapitalized();

    /**
     * Translated "Show all encodings".
     */
    @DefaultMessage("Show all encodings")
    @Key("showAllEncodings")
    String showAllEncodings();

    /**
     * Translated "Set as default encoding for source files".
     */
    @DefaultMessage("Set as default encoding for source files")
    @Key("setAsDefaultEncodingSourceFiles")
    String setAsDefaultEncodingSourceFiles();

    /**
     * Translated "{0} (System default)".
     */
    @DefaultMessage("{0} (System default)")
    @Key("sysEncNameDefault")
    String sysEncNameDefault(String sysEncName);

    /**
     * Translated "[Ask]".
     */
    @DefaultMessage("[Ask]")
    @Key("askSquareBrackets")
    String askSquareBrackets();

    /**
     * Translated "New R Documentation File".
     */
    @DefaultMessage("New R Documentation File")
    @Key("newRDocumentationFile")
    String newRDocumentationFile();

    /**
     * Translated "Name Not Specified".
     */
    @DefaultMessage("Name Not Specified")
    @Key("nameNotSpecified")
    String nameNotSpecified();

    /**
     * Translated "You must specify a topic name for the new Rd file.".
     */
    @DefaultMessage("You must specify a topic name for the new Rd file.")
    @Key("mustSpecifyTopicNameForRdFile")
    String mustSpecifyTopicNameForRdFile();

    /**
     * Translated "New R Markdown".
     */
    @DefaultMessage("New R Markdown")
    @Key("newRMarkdown")
    String newRMarkdown();

    /**
     * Translated "Templates".
     */
    @DefaultMessage("Templates")
    @Key("templatesCapitalized")
    String templatesCapitalized();

    /**
     * Translated "Create Empty Document".
     */
    @DefaultMessage("Create Empty Document")
    @Key("createEmptyDocument")
    String createEmptyDocument();

    /**
     * Translated "Using Shiny with R Markdown".
     */
    @DefaultMessage("Using Shiny with R Markdown")
    @Key("usingShinyWithRMarkdown")
    String usingShinyWithRMarkdown();

    /**
     * Translated "Create an HTML document with interactive Shiny components.".
     */
    @DefaultMessage("Create an HTML document with interactive Shiny components.")
    @Key("shinyDocNameDescription")
    String shinyDocNameDescription();

    /**
     * Translated "Create an IOSlides presentation with interactive Shiny components.".
     */
    @DefaultMessage("Create an IOSlides presentation with interactive Shiny components.")
    @Key("shinyPresentationNameDescription")
    String shinyPresentationNameDescription();

    /**
     * Translated "From Template".
     */
    @DefaultMessage("From Template")
    @Key("fromTemplate")
    String fromTemplate();

    /**
     * Translated "Shiny".
     */
    @DefaultMessage("Shiny")
    @Key("shinyCapitalized")
    String shinyCapitalized();

    /**
     * Translated "Shiny Document".
     */
    @DefaultMessage("Shiny Document")
    @Key("shinyDocument")
    String shinyDocument();

    /**
     * Translated "Shiny Presentation".
     */
    @DefaultMessage("Shiny Presentation")
    @Key("shinyPresentation")
    String shinyPresentation();

    /**
     * Translated "No Parameters Defined".
     */
    @DefaultMessage("No Parameters Defined")
    @Key("noParametersDefined")
    String noParametersDefined();

    /**
     * Translated "There are no parameters defined for the current R Markdown document.".
     */
    @DefaultMessage("There are no parameters defined for the current R Markdown document.")
    @Key("noParametersDefinedForCurrentRMarkdown")
    String noParametersDefinedForCurrentRMarkdown();

    /**
     * Translated "Using R Markdown Parameters".
     */
    @DefaultMessage("Using R Markdown Parameters")
    @Key("usingRMarkdownParameters")
    String usingRMarkdownParameters();

    /**
     * Translated "Unable to activate visual mode (unsupported front matter format or non top-level YAML block)".
     */
    @DefaultMessage("Unable to activate visual mode (unsupported front matter format or non top-level YAML block)")
    @Key("unableToActivateVisualModeYAML")
    String unableToActivateVisualModeYAML();

    /**
     * Translated "Unable to activate visual mode (error parsing code chunks out of document)".
     */
    @DefaultMessage("Unable to activate visual mode (error parsing code chunks out of document)")
    @Key("unableToActivateVisualModeParsingCode")
    String unableToActivateVisualModeParsingCode();

    /**
     * Translated "Unable to activate visual mode (document contains example lists which are not currently supported)".
     */
    @DefaultMessage("Unable to activate visual mode (document contains example lists which are not currently supported)")
    @Key("unableToActivateVisualModeDocumentContains")
    String unableToActivateVisualModeDocumentContains();

    /**
     * Translated "Unrecognized Pandoc token(s); {0}".
     */
    @DefaultMessage("Unrecognized Pandoc token(s); {0}")
    @Key("unrecognizedPandocTokens")
    String unrecognizedPandocTokens(String tokens);

    /**
     * Translated "Invalid Pandoc format: {0}".
     */
    @DefaultMessage("Invalid Pandoc format: {0}")
    @Key("invalidPandocFormat")
    String invalidPandocFormat(String format);

    /**
     * Translated "Unsupported extensions for markdown mode: {0}".
     */
    @DefaultMessage("Unsupported extensions for markdown mode: {0}")
    @Key("unsupportedExtensionsForMarkdown")
    String unsupportedExtensionsForMarkdown(String format);

    /**
     * Translated "Unable to parse markdown (please report at https://github.com/rstudio/rstudio/issues/new)".
     */
    @DefaultMessage("Unable to parse markdown (please report at https://github.com/rstudio/rstudio/issues/new)")
    @Key("unableToParseMarkdownPleaseReport")
    String unableToParseMarkdownPleaseReport();

    /**
     * Translated "Chunk {0}".
     */
    @DefaultMessage("Chunk {0}")
    @Key("chunkSequence")
    String chunkSequence(int itemSequence);

    /**
     * Translated "(Top Level)".
     */
    @DefaultMessage("(Top Level)")
    @Key("topLevelParentheses")
    String topLevelParentheses();

    /**
     * Translated "You cannot enter visual mode while using realtime collaboration.".
     */
    @DefaultMessage("You cannot enter visual mode while using realtime collaboration.")
    @Key("cantEnterVisualModeUsingRealtime")
    String cantEnterVisualModeUsingRealtime();

    /**
     * Translated "{0}, {1} line".
     */
    @DefaultMessage("{0}, {1} line")
    @Key("visualModeChunkSummary")
    String visualModeChunkSummary(String enging, int lines);

    /**
     * Translated "{0}, {1} lines".
     */
    @DefaultMessage("{0}, {1} lines")
    @Key("visualModeChunkSummaryPlural")
    String visualModeChunkSummaryPlural(String engine, int lines);

    /**
     * Translated "images".
     */
    @DefaultMessage("images")
    @Key("images")
    String images();

    /**
     * Translated "Xaringan presentations cannot be edited in visual mode.".
     */
    @DefaultMessage("Xaringan presentations cannot be edited in visual mode.")
    @Key("xaringanPresentationsVisualMode")
    String xaringanPresentationsVisualMode();

    /**
     * Translated "Version control conflict markers detected. Please resolve them before editing in visual mode.".
     */
    @DefaultMessage("Version control conflict markers detected. Please resolve them before editing in visual mode.")
    @Key("versionControlConflict")
    String versionControlConflict();

    /**
     * Translated "Switch to Visual Mode".
     */
    @DefaultMessage("Switch to Visual Mode")
    @Key("switchToVisualMode")
    String switchToVisualMode();

    /**
     * Translated "Use Visual Mode".
     */
    @DefaultMessage("Use Visual Mode")
    @Key("useVisualMode")
    String useVisualMode();

    /**
     * Translated "Don''t show this message again".
     */
    @DefaultMessage("Don''t show this message again")
    @Key("dontShowMessageAgain")
    String dontShowMessageAgain();

    /**
     * Translated "Line Wrapping".
     */
    @DefaultMessage("Line Wrapping")
    @Key("lineWrapping")
    String lineWrapping();

    /**
     * Translated "project default".
     */
    @DefaultMessage("project default")
    @Key("projectDefault")
    String projectDefault();

    /**
     * Translated "global default".
     */
    @DefaultMessage("global default")
    @Key("globalDefault")
    String globalDefault();

    /**
     * Translated "Line wrapping in this document differs from the {0}:".
     */
    @DefaultMessage("Line wrapping in this document differs from the {0}:")
    @Key("lineWrappingDiffersFromCurrent")
    String lineWrappingDiffersFromCurrent(String current);

    /**
     * Translated "Select how you''d like to handle line wrapping below:".
     */
    @DefaultMessage("Select how you''d like to handle line wrapping below:")
    @Key("selectHandleLineWrapping")
    String selectHandleLineWrapping();

    /**
     * Translated "Use {0}-based line wrapping for this document".
     */
    @DefaultMessage("Use {0}-based line wrapping for this document")
    @Key("useBasedLineWrappingForDocument")
    String useBasedLineWrappingForDocument(String detectedLineWrappingForDocument);
    
    /**
     * Translated "Use {0}-based line wrapping for this project".
     */
    @DefaultMessage("Use {0}-based line wrapping for this project")
    @Key("useBasedLineWrappingForProject")
    String useBasedLineWrappingForProject(String detectedLineWrappingForProject);

    /**
     * Translated "Use the current {0} default line wrapping for this document".
     */
    @DefaultMessage("Use the current {0} default line wrapping for this document")
    @Key("useDefaultLinewrapping")
    String useDefaultLinewrapping(String projectConfigChoice);

    /**
     * Translated "project".
     */
    @DefaultMessage("project")
    @Key("project")
    String project();

    /**
     * Translated "global".
     */
    @DefaultMessage("global")
    @Key("global")
    String global();

    /**
     * Translated "Learn more about visual mode line wrapping options".
     */
    @DefaultMessage("Learn more about visual mode line wrapping options")
    @Key("learnAboutVisualModeLineWrapping")
    String learnAboutVisualModeLineWrapping();

    /**
     * Translated "Wrap at column:".
     */
    @DefaultMessage("Wrap at column:")
    @Key("wrapAtColumnColon")
    String wrapAtColumnColon();

    /**
     * Translated "Collapse".
     */
    @DefaultMessage("Collapse")
    @Key("collapseCapitalized")
    String collapseCapitalized();

    /**
     * Translated "Expand".
     */
    @DefaultMessage("Expand")
    @Key("expandCapitalized")
    String expandCapitalized();

    /**
     * Translated "{0} code chunk".
     */
    @DefaultMessage("{0} code chunk")
    @Key("collapseOrExpandCodeChunk")
    String collapseOrExpandCodeChunk(String hintText);

    /**
     * Translated "Some of your source edits are still being backed up. If you continue, your latest changes may be lost. Do you want to continue?".
     */
    @DefaultMessage("Some of your source edits are still being backed up. If you continue, your latest changes may be lost. Do you want to continue?")
    @Key("editsStillBeingBackedUp")
    String editsStillBeingBackedUp();

    /**
     * Translated "The process cannot access the file because it is being used by another process".
     */
    @DefaultMessage("The process cannot access the file because it is being used by another process")
    @Key("processStillBeingUsedTextEditingTarget")
    String processStillBeingUsedTextEditingTarget();

    /**
     * Translated "Error Autosaving File".
     */
    @DefaultMessage("Error Autosaving File")
    @Key("errorAutosavingFile")
    String errorAutosavingFile();

    /**
     * Translated "RStudio was unable to autosave this file. You may need to restart RStudio.".
     */
    @DefaultMessage("RStudio was unable to autosave this file. You may need to restart RStudio.")
    @Key("rStudioUnableToAutosave")
    String rStudioUnableToAutosave();

    /**
     * Translated "Could not save {0}: {1}".
     */
    @DefaultMessage("Could not save {0}: {1}")
    @Key("couldNotSavePathPlusMessage")
    String couldNotSavePathPlusMessage(String path, String message);

    /**
     * Translated "Error saving {0}: {1}".
     */
    @DefaultMessage("Error saving {0}: {1}")
    @Key("errorSavingPathPlusMessage")
    String errorSavingPathPlusMessage(String path, String message);

    /**
     */
    @DefaultMessage("Document Outline")
    @Key("documentOutline")
    String documentOutline();

    /**
     * Translated "<td><input type=''button'' value=''More...'' data-action=''open''></input></td>".
     */
    @DefaultMessage("<td><input type=''button'' value=''More...'' data-action=''open''></input></td>")
    @Key("moreButtonCell")
    String moreButtonCell();

    /**
     * Translated "{0} <...truncated...> )".
     */
    @DefaultMessage("{0} <...truncated...> )")
    @Key("truncatedEllipses")
    String truncatedEllipses(String truncated);

    /**
     * Translated "The document uses {0}-based line wrapping".
     */
    @DefaultMessage("The document uses {0}-based line wrapping")
    @Key("documentUsesBasedLineWrapping")
    String documentUsesBasedLineWrapping(String detectedLineWrapping);

    /**
     * Translated "The {0} is no line wrapping".
     */
    @DefaultMessage("The {0} is no line wrapping")
    @Key("defaultNoLineWrapping")
    String defaultNoLineWrapping(String current);

    /**
     * Translated "The {0} is {1}-based line wrapping".
     */
    @DefaultMessage("The {0} is {1}-based line wrapping")
    @Key("defaultConfiguredBasedLineWrapping")
    String defaultConfiguredBasedLineWrapping(String current, String configuredLineWrapping);

    /**
     * Translated "Create R Notebook".
     */
    @DefaultMessage("Create R Notebook")
    @Key("createRNotebookText")
    String createRNotebookText();


    /**
     * Translated "Creating Shiny applications".
     */
    @DefaultMessage("Creating Shiny applications")
    @Key("creatingShinyApplicationsText")
    String creatingShinyApplicationsText();

    /**
     * Translated "Authoring R Presentations".
     */
    @DefaultMessage("Authoring R Presentations")
    @Key("authoringRPresentationsText")
    String authoringRPresentationsText();

    /**
     * Translated "Creating R Plumber APIs".
     */
    @DefaultMessage("Creating R Plumber API")
    @Key("creatingRPlumberAPIText")
    String creatingRPlumberAPIText();

    /**
     * Translated "Using R Notebooks".
     */
    @DefaultMessage("Using R Notebooks")
    @Key("usingRNotebooksText")
    String usingRNotebooksText();

    /**
     * Translated "The profiler".
     */
    @DefaultMessage("The profiler")
    @Key("theProfilerText")
    String theProfilerText();

    /**
     * Translated "Could not resolve {0}. Please make sure this is an R package project with a BugReports field set.".
     */
    @DefaultMessage("Could not resolve {0}. Please make sure this is an R package project with a BugReports field set.")
    @Key("couldNotResolveIssue")
    String couldNotResolveIssue(String issue);

    /**
     * Translated "Unable to Edit".
     */
    @DefaultMessage("Unable to Edit")
    @Key("unableToEditTitle")
    String unableToEditTitle();

    /**
     * Translated "The options use syntax that is not currently supported by the editing UI. Please edit manually.".
     */
    @DefaultMessage("The options use syntax that is not currently supported by the editing UI. Please edit manually.")
    @Key("unableToEditMessage")
    String unableToEditMessage();
}

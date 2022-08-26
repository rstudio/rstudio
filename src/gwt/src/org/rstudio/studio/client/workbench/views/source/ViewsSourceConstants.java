/*
 * ViewsSourceConstants.java
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

package org.rstudio.studio.client.workbench.views.source;

public interface ViewsSourceConstants extends com.google.gwt.i18n.client.Messages{
    /**
     * Translated "(No documents)".
     *
     * @return translated "(No documents)"
     */
    @DefaultMessage("(No documents)")
    @Key("noDocumentsParentheses")
    String noDocumentsParentheses();

    /**
     * Translated "No outline available".
     *
     * @return translated "No outline available"
     */
    @DefaultMessage("No outline available")
    @Key("noOutlineAvailable")
    String noOutlineAvailable();

    /**
     * Translated "Title".
     *
     * @return translated "Title"
     */
    @DefaultMessage("Title")
    @Key("title")
    String title();

    /**
     * Translated "Invalid API Name".
     *
     * @return translated "Invalid API Name"
     */
    @DefaultMessage("Invalid API Name")
    @Key("invalidApiName")
    String invalidApiName();

    /**
     * Translated "Invalid application name".
     *
     * @return translated "Invalid application name"
     */
    @DefaultMessage("Invalid application name")
    @Key("invalidApplicationName")
    String invalidApplicationName();

    /**
     * Translated "The API name must not be empty".
     *
     * @return translated "The API name must not be empty"
     */
    @DefaultMessage("The API name must not be empty")
    @Key("apiNameMustNotBeEmpty")
    String apiNameMustNotBeEmpty();

    /**
     * Translated "Plumber APIs".
     *
     * @return translated "Plumber APIs"
     */
    @DefaultMessage("Plumber APIs")
    @Key("plumberAPIs")
    String plumberAPIs();

    /**
     * Translated "Create within directory:".
     *
     * @return translated "Create within directory:"
     */
    @DefaultMessage("Create within directory:")
    @Key("createWithinDirectoryColon")
    String createWithinDirectoryColon();

    /**
     * Translated "API name:".
     *
     * @return translated "API name:"
     */
    @DefaultMessage("API name:")
    @Key("apiNameColon")
    String apiNameColon();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    @DefaultMessage("Create")
    @Key("create")
    String create();

    /**
     * Translated "Application name:".
     *
     * @return translated "Application name:"
     */
    @DefaultMessage("Application name:")
    @Key("applicationNameColon")
    String applicationNameColon();

    /**
     * Translated "Application type:".
     *
     * @return translated "Application type:"
     */
    @DefaultMessage("Application type:")
    @Key("applicationTypeColon")
    String applicationTypeColon();

    /**
     * Translated "Single File (app.R)".
     *
     * @return translated "Single File (app.R)"
     */
    @DefaultMessage("Single File (app.R)")
    @Key("singleFileAppR")
    String singleFileAppR();

    /**
     * Translated "Multiple File (ui.R/server.R)".
     *
     * @return translated "Multiple File (ui.R/server.R)"
     */
    @DefaultMessage("Multiple File (ui.R/server.R)")
    @Key("multipleFileUiServerR")
    String multipleFileUiServerR();

    /**
     * Translated "Shiny Web Applications".
     *
     * @return translated "Shiny Web Applications"
     */
    @DefaultMessage("Shiny Web Applications")
    @Key("shinyWebApplications")
    String shinyWebApplications();

    /**
     * Translated "The application name must not be empty".
     *
     * @return translated "The application name must not be empty"
     */
    @DefaultMessage("The application name must not be empty")
    @Key("applicationNameMustNotBeEmpty")
    String applicationNameMustNotBeEmpty();

    /**
     * Translated "Invalid Application Name".
     *
     * @return translated "Invalid Application Name"
     */
    @DefaultMessage("Invalid Application Name")
    @Key("invalidApplicationNameCapitalized")
    String invalidApplicationNameCapitalized();

    /**
     * Translated "New Quarto {0}...".
     *
     * @return translated "New Quarto {0}..."
     */
    @DefaultMessage("New Quarto {0}...")
    @Key("newQuatroProgressIndicator")
    String newQuatroProgressIndicator(String presentationType);

    /**
     * Translated "Presentation".
     *
     * @return translated "Presentation"
     */
    @DefaultMessage("Presentation")
    @Key("presentationCapitalized")
    String presentationCapitalized();

    /**
     * Translated "Document".
     *
     * @return translated "Document"
     */
    @DefaultMessage("Document")
    @Key("documentCapitalized")
    String documentCapitalized();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCapitalized")
    String errorCapitalized();

    /**
     * Translated "Always save files before build".
     *
     * @return translated "Always save files before build"
     */
    @DefaultMessage("Always save files before build")
    @Key("alwaysSaveFilesBeforeBuild")
    String alwaysSaveFilesBeforeBuild();

    /**
     * Translated "Show".
     *
     * @return translated "Show"
     */
    @DefaultMessage("Show")
    @Key("show")
    String show();

    /**
     * Translated "Source Document Error".
     *
     * @return translated "Source Document Error"
     */
    @DefaultMessage("Source Document Error")
    @Key("sourceDocumentError")
    String sourceDocumentError();

    /**
     * Translated "Show Profiler".
     *
     * @return translated "Show Profiler"
     */
    @DefaultMessage("Show Profiler")
    @Key("showProfiler")
    String showProfiler();

    /**
     * Translated "R Notebook".
     *
     * @return translated "R Notebook"
     */
    @DefaultMessage("R Notebook")
    @Key("rNotebook")
    String rNotebook();

    /**
     * Translated "Notebook Creation Failed".
     *
     * @return translated "Notebook Creation Failed"
     */
    @DefaultMessage("Notebook Creation Failed")
    @Key("notebookCreationFailed")
    String notebookCreationFailed();

    /**
     * Translated "One or more packages required for R Notebook creation were not installed.".
     *
     * @return translated "One or more packages required for R Notebook creation were not installed."
     */
    @DefaultMessage("One or more packages required for R Notebook creation were not installed.")
    @Key("rNotebookCreationFailedPackagesNotInstalled")
    String rNotebookCreationFailedPackagesNotInstalled();

    /**
     * Translated "Creating Stan script".
     *
     * @return translated "Creating Stan script"
     */
    @DefaultMessage("Creating Stan script")
    @Key("creatingStanScript")
    String creatingStanScript();

    /**
     * Translated "Creating Stan scripts".
     *
     * @return translated "Creating Stan scripts"
     */
    @DefaultMessage("Creating Stan scripts")
    @Key("creatingStanScriptPlural")
    String creatingStanScriptPlural();

    /**
     * Translated "Creating new document...".
     *
     * @return translated "Creating new document..."
     */
    @DefaultMessage("Creating new document...")
    @Key("creatingNewDocument")
    String creatingNewDocument();

    /**
     * Translated "Error Creating Shiny Application".
     *
     * @return translated "Error Creating Shiny Application"
     */
    @DefaultMessage("Error Creating Shiny Application")
    @Key("errorCreatingShinyApplication")
    String errorCreatingShinyApplication();

    /**
     * Translated "Error Creating Plumber API".
     *
     * @return translated "Error Creating Plumber API"
     */
    @DefaultMessage("Error Creating Plumber API")
    @Key("errorCreatingPlumberApi")
    String errorCreatingPlumberApi();

    /**
     * Translated "New Shiny Web Application".
     *
     * @return translated "New Shiny Web Application"
     */
    @DefaultMessage("New Shiny Web Application")
    @Key("newShinyWebApplication")
    String newShinyWebApplication();

    /**
     * Translated "New R Presentation".
     *
     * @return translated "New R Presentation"
     */
    @DefaultMessage("New R Presentation")
    @Key("newRPresentation")
    String newRPresentation();

    /**
     * Translated "Creating Presentation...".
     *
     * @return translated "Creating Presentation..."
     */
    @DefaultMessage("Creating Presentation...")
    @Key("creatingPresentation")
    String creatingPresentation();

    /**
     * Translated "Document Tab Move Failed".
     *
     * @return translated "Document Tab Move Failed"
     */
    @DefaultMessage("Document Tab Move Failed")
    @Key("documentTabMoveFailed")
    String documentTabMoveFailed();

    /**
     * Translated "Couldn''t move the tab to this window: \n{0}".
     *
     * @return translated "Couldn''t move the tab to this window: \n{0}"
     */
    @DefaultMessage("Couldn''t move the tab to this window: \n{0}")
    @Key("couldntMoveTabToWindowError")
    String couldntMoveTabToWindowError(String errorMessage);

    /**
     * Translated "Close All".
     *
     * @return translated "Close All"
     */
    @DefaultMessage("Close All")
    @Key("closeAll")
    String closeAll();

    /**
     * Translated "Close Other".
     *
     * @return translated "Close Other"
     */
    @DefaultMessage("Close Other")
    @Key("closeOther")
    String closeOther();

    /**
     * Translated "Open File".
     *
     * @return translated "Open File"
     */
    @DefaultMessage("Open File")
    @Key("openFile")
    String openFile();

    /**
     * Translated "New Plumber API".
     *
     * @return translated "New Plumber API"
     */
    @DefaultMessage("New Plumber API")
    @Key("newPlumberApi")
    String newPlumberApi();

    /**
     * Translated "Close All Others".
     *
     * @return translated "Close All Others"
     */
    @DefaultMessage("Close All Others")
    @Key("closeAllOthers")
    String closeAllOthers();

    /**
     * Translated "Error Creating New Document".
     *
     * @return translated "Error Creating New Document"
     */
    @DefaultMessage("Error Creating New Document")
    @Key("errorCreatingNewDocument")
    String errorCreatingNewDocument();

    /**
     * Translated "Publish Document...".
     *
     * @return translated "Publish Document..."
     */
    @DefaultMessage("Publish Document...")
    @Key("publishDocument")
    String publishDocument();

    /**
     * Translated "Publish Plumber API...".
     *
     * @return translated "Publish Plumber API..."
     */
    @DefaultMessage("Publish Plumber API...")
    @Key("publishPlumberApi")
    String publishPlumberApi();

    /**
     * Translated "Publish Application...".
     *
     * @return translated "Publish Application..."
     */
    @DefaultMessage("Publish Application...")
    @Key("publishApplication")
    String publishApplication();

    // TODO IF THESE ARE SHORTCUTS NEED TO DOUBLE CHECK THESE
    /**
     * Translated "_Blame \"{0}\" on GitHub".
     *
     * @return translated "_Blame \"{0}\" on GitHub"
     */
    @DefaultMessage("_Blame \"{0}\" on GitHub")
    @Key("blameOnGithub")
    String blameOnGithub(String name);

    /**
     * Translated "_View \"{0}\" on GitHub".
     *
     * @return translated "_View \"{0}\" on GitHub"
     */
    @DefaultMessage("_View \"{0}\" on GitHub")
    @Key("viewNameOnGithub")
    String viewNameOnGithub(String name);

    /**
     * Translated "_Revert \"{0}\"...".
     *
     * @return translated "_Revert \"{0}\"..."
     */
    @DefaultMessage("_Revert \"{0}\"...")
    @Key("revertName")
    String revertName(String name);

    /**
     * Translated "_Log of \"{0}\"".
     *
     * @return translated "_Log of \"{0}\""
     */
    @DefaultMessage("_Log of \"{0}\"")
    @Key("logOfName")
    String logOfName(String name);

    /**
     * Translated "_Diff \"{0}\"".
     *
     * @return translated "_Diff \"{0}\""
     */
    @DefaultMessage("_Diff \"{0}\"")
    @Key("diffName")
    String diffName(String name);

    /**
     * Translated "No document tabs open".
     *
     * @return translated "No document tabs open"
     */
    @DefaultMessage("No document tabs open")
    @Key("noDocumentTabsOpen")
    String noDocumentTabsOpen();

    /**
     * Translated "Show Object Explorer".
     *
     * @return translated "Show Object Explorer"
     */
    @DefaultMessage("Show Object Explorer")
    @Key("showObjectExplorer")
    String showObjectExplorer();

    /**
     * Translated "Show Data Frame".
     *
     * @return translated "Show Data Frame"
     */
    @DefaultMessage("Show Data Frame")
    @Key("showDataFrame")
    String showDataFrame();

    /**
     * Translated "Template Content Missing".
     *
     * @return translated "Template Content Missing"
     */
    @DefaultMessage("Template Content Missing")
    @Key("templateContentMissing")
    String templateContentMissing();

    /**
     * Translated "The template at {0} is missing.".
     *
     * @return translated "The template at {0} is missing."
     */
    @DefaultMessage("The template at {0} is missing.")
    @Key("templateAtPathMissing")
    String templateAtPathMissing(String templatePath);

    /**
     * Translated "Error while opening file".
     *
     * @return translated "Error while opening file"
     */
    @DefaultMessage("Error while opening file")
    @Key("errorWhileOpeningFile")
    String errorWhileOpeningFile();

    /**
     * Translated "This notebook has the same name as an R Markdown file, but doesn''t match it".
     *
     * @return translated "This notebook has the same name as an R Markdown file, but doesn''t match it"
     */
    @DefaultMessage("This notebook has the same name as an R Markdown file, but doesn''t match it")
    @Key("openNotebookWarningMessage")
    String openNotebookWarningMessage();

    /**
     * Translated "Notebook Open Failed".
     *
     * @return translated "Notebook Open Failed"
     */
    @DefaultMessage("Notebook Open Failed")
    @Key("notebookOpenFailed")
    String notebookOpenFailed();

    /**
     * Translated "This notebook could not be opened. If the error persists, try removing the accompanying R Markdown file. \n\n{0}".
     *
     * @return translated "This notebook could not be opened. If the error persists, try removing the accompanying R Markdown file. \n\n{0}"
     */
    @DefaultMessage("This notebook could not be opened. If the error persists, try removing the accompanying R Markdown file. \\n\\n{0}")
    @Key("notebookOpenFailedMessage")
    String notebookOpenFailedMessage(String errorMessage);

    /**
     * Translated "This notebook could not be opened. \n\n{0}".
     *
     * @return translated "This notebook could not be opened. \n\n{0}"
     */
    @DefaultMessage("This notebook could not be opened. \n\n{0}")
    @Key("notebookCouldNotBeOpenedMessage")
    String notebookCouldNotBeOpenedMessage(String errorMessage);

    /**
     * Translated "Opening file...".
     *
     * @return translated "Opening file..."
     */
    @DefaultMessage("Opening file...")
    @Key("openingFile")
    String openingFile();

    /**
     * Translated "The file ''{0}'' is too large to open in the source editor (the file is {1} and the maximum file size is {2})".
     *
     * @return translated "The file ''{0}'' is too large to open in the source editor (the file is {1} and the maximum file size is {2})"
     */
    @DefaultMessage("The file ''{0}'' is too large to open in the source editor (the file is {1} and the maximum file size is {2})")
    @Key("showFileTooLargeWarningMsg")
    String showFileTooLargeWarningMsg(String filename, String filelength, String sizeLimit);

    /**
     * Translated "Selected File Too Large".
     *
     * @return translated "Selected File Too Large"
     */
    @DefaultMessage("Selected File Too Large")
    @Key("selectedFileTooLarge")
    String selectedFileTooLarge();

    /**
     * Translated "The source file ''{0}'' is large ({1}) and may take some time to open. Are you sure you want to continue opening it?".
     *
     * @return translated "The source file ''{0}'' is large ({1}) and may take some time to open. Are you sure you want to continue opening it?"
     */
    @DefaultMessage("The source file ''{0}'' is large ({1}) and may take some time to open. Are you sure you want to continue opening it?")
    @Key("confirmOpenLargeFileMsg")
    String confirmOpenLargeFileMsg(String filename, String length);

    /**
     * Translated "Confirm Open".
     *
     * @return translated "Confirm Open"
     */
    @DefaultMessage("Confirm Open")
    @Key("confirmOpen")
    String confirmOpen();

    /**
     * Translated "Source Column {0}".
     *
     * @return translated "Source Column {0}"
     */
    @DefaultMessage("Source Column {0}")
    @Key("sourceColumn")
    String sourceColumn(int columnCounter);

    /**
     * Translated "Source".
     *
     * @return translated "Source"
     */
    @DefaultMessage("Source")
    @Key("source")
    String source();

    /**
     * Translated "Switch to tab".
     *
     * @return translated "Switch to tab"
     */
    @DefaultMessage("Switch to tab")
    @Key("switchToTab")
    String switchToTab();

    /**
     * Translated "RStudio Source Editor".
     *
     * @return translated "RStudio Source Editor"
     */
    @DefaultMessage("RStudio Source Editor")
    @Key("rstudioSourceEditor")
    String rstudioSourceEditor();

    /**
     * Translated "Close Source Window".
     *
     * @return translated "Close Source Window"
     */
    @DefaultMessage("Close Source Window")
    @Key("closeSourceWindow")
    String closeSourceWindow();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("cancel")
    String cancel();

    /**
     * Translated "Close and Discard Changes".
     *
     * @return translated "Close and Discard Changes"
     */
    @DefaultMessage("Close and Discard Changes")
    @Key("closeAndDiscardChanges")
    String closeAndDiscardChanges();

    /**
     * Translated "Unsaved Changes".
     *
     * @return translated "Unsaved Changes"
     */
    @DefaultMessage("Unsaved Changes")
    @Key("unsavedChanges")
    String unsavedChanges();

    /**
     * Translated "There are unsaved documents in this window. Are you sure you want to close it?".
     *
     * @return translated "There are unsaved documents in this window. Are you sure you want to close it?"
     */
    @DefaultMessage("There are unsaved documents in this window. Are you sure you want to close it?")
    @Key("confirmCloseUnsavedDocuments")
    String confirmCloseUnsavedDocuments();

    /**
     * Translated "Your edits to the file {0} have not been saved".
     *
     * @return translated "Your edits to the file {0} have not been saved"
     */
    @DefaultMessage("Your edits to the file {0} have not been saved")
    @Key("yourEditsToFileHasNotBeenSaved")
    String yourEditsToFileHasNotBeenSaved(String desc);

    /**
     * Translated "Your edits to the files {0} have not been saved".
     *
     * @return translated "Your edits to the files {0} have not been saved"
     */
    @DefaultMessage("Your edits to the files {0} have not been saved")
    @Key("yourEditsToFilePluralHasNotBeenSaved")
    String yourEditsToFilePluralHasNotBeenSaved(String completeMessage);

    /**
     * Translated ", ".
     *
     * @return translated ", "
     */
    @DefaultMessage(", ")
    @Key("commaListSeparator")
    String commaListSeparator();

    /**
     * Translated " and ".
     *
     * @return translated " and "
     */
    @DefaultMessage(" and ")
    @Key("andForList")
    String andForList();

    /**
     * Translated "Can''t Move Doc".
     *
     * @return translated "Can''t Move Doc"
     */
    @DefaultMessage("Can''t Move Doc")
    @Key("cantMoveDoc")
    String cantMoveDoc();

    /**
     * Translated "The document could not be moved to a different window: \n{0}".
     *
     * @return translated "The document could not be moved to a different window: \n{0}"
     */
    @DefaultMessage("The document could not be moved to a different window: \n{0}")
    @Key("cantMoveDocMessage")
    String cantMoveDocMessage(String errorMessage);

    /**
     * Translated "Search tabs".
     *
     * @return translated "Search tabs"
     */
    @DefaultMessage("Search tabs")
    @Key("searchTabs")
    String searchTabs();

    /**
     * Translated "Code Editor Tab".
     *
     * @return translated "Code Editor Tab"
     */
    @DefaultMessage("Code Editor Tab")
    @Key("codeEditorTab")
    String codeEditorTab();

    /**
     * Translated "Function:".
     *
     * @return translated "Function:"
     */
    @DefaultMessage("Function:")
    @Key("functionColon")
    String functionColon();

    /**
     * Translated "Method:".
     *
     * @return translated "Method:"
     */
    @DefaultMessage("Method:")
    @Key("methodColon")
    String methodColon();

    /**
     * Translated "Error Reading Function Definition".
     *
     * @return translated "Error Reading Function Definition"
     */
    @DefaultMessage("Error Reading Function Definition")
    @Key("errorReadingFunctionDefinition")
    String errorReadingFunctionDefinition();

    /**
     * Translated "Code Browser displayed".
     *
     * @return translated "Code Browser displayed"
     */
    @DefaultMessage("Code Browser displayed")
    @Key("codeBrowserDisplayed")
    String codeBrowserDisplayed();

    /**
     * Translated "Debug location is approximate because the source is not available.".
     *
     * @return translated "Debug location is approximate because the source is not available."
     */
    @DefaultMessage("Debug location is approximate because the source is not available.")
    @Key("debugLocationIsApproximate")
    String debugLocationIsApproximate();

    /**
     * Translated "R Source Viewer".
     *
     * @return translated "R Source Viewer"
     */
    @DefaultMessage("R Source Viewer")
    @Key("rSourceViewer")
    String rSourceViewer();

    /**
     * Translated "(Read-only)".
     *
     * @return translated "(Read-only)"
     */
    @DefaultMessage("(Read-only)")
    @Key("readOnlyParentheses")
    String readOnlyParentheses();

    /**
     * Translated "Code Browser Second".
     *
     * @return translated "Code Browser Second"
     */
    @DefaultMessage("Code Browser Second")
    @Key("codeBrowserSecond")
    String codeBrowserSecond();

    /**
     * Translated "Code Tools".
     *
     * @return translated "Code Tools"
     */
    @DefaultMessage("Code Tools")
    @Key("codeTools")
    String codeTools();

    /**
     * Translated "Error Searching for Function".
     *
     * @return translated "Error Searching for Function"
     */
    @DefaultMessage("Error Searching for Function")
    @Key("errorSearchingForFunction")
    String errorSearchingForFunction();

    /**
     * Translated "Searching for function definition...".
     *
     * @return translated "Searching for function definition..."
     */
    @DefaultMessage("Searching for function definition...")
    @Key("searchingForFunctionDefinition")
    String searchingForFunctionDefinition();

    /**
     * Translated "{0} Source Viewer".
     *
     * @return translated "{0} Source Viewer"
     */
    @DefaultMessage("{0} Source Viewer")
    @Key("nameSourceViewer")
    String nameSourceViewer(String name);

    /**
     * Translated "Untitled Source Viewer".
     *
     * @return translated "Untitled Source Viewer"
     */
    @DefaultMessage("Untitled Source Viewer")
    @Key("untitledSourceViewer")
    String untitledSourceViewer();

    /**
     * Translated "Help".
     *
     * @return translated "Help"
     */
    @DefaultMessage("Help")
    @Key("help")
    String help();

    /**
     * Translated "Data Browser displayed".
     *
     * @return translated "Data Browser displayed"
     */
    @DefaultMessage("Data Browser displayed")
    @Key("dataBrowserDisplayed")
    String dataBrowserDisplayed();

    /**
     * Translated "Data Browser".
     *
     * @return translated "Data Browser"
     */
    @DefaultMessage("Data Browser")
    @Key("dataBrowser")
    String dataBrowser();

    /**
     * Translated "{0} Data Browser".
     *
     * @return translated "{0} Data Browser"
     */
    @DefaultMessage("{0} Data Browser")
    @Key("accessibleNameDataBrowser")
    String accessibleNameDataBrowser(String accessibleName);

    /**
     * Translated "Untitled Data Browser".
     *
     * @return translated "Untitled Data Browser"
     */
    @DefaultMessage("Untitled Data Browser")
    @Key("untitledDataBrowser")
    String untitledDataBrowser();

    /**
     * Translated "Displayed {0} rows of {1}".
     *
     * @return translated "Displayed {0} rows of {1}"
     */
    @DefaultMessage("Displayed {0} rows of {1}")
    @Key("dataEditingTargetWidgetLabel1")
    String dataEditingTargetWidgetLabel1(String displayedObservations, String totalObservations);

    /**
     * Translated "({0} omitted)".
     *
     * @return translated "({0} omitted)"
     */
    @DefaultMessage("({0} omitted)")
    @Key("dataEditingTargetWidgetLabel2")
    String dataEditingTargetWidgetLabel2(String omittedNumber);

    /**
     * Translated "{0} Object Explorer".
     *
     * @return translated "{0} Object Explorer"
     */
    @DefaultMessage("{0} Object Explorer")
    @Key("accessibleNameObjectExplorer")
    String accessibleNameObjectExplorer(String accessibleName);

    /**
     * Translated "Untitled Object Explorer".
     *
     * @return translated "Untitled Object Explorer"
     */
    @DefaultMessage("Untitled Object Explorer")
    @Key("untitledObjectExplorer")
    String untitledObjectExplorer();

    /**
     * Translated "Object Explorer displayed".
     *
     * @return translated "Object Explorer displayed"
     */
    @DefaultMessage("Object Explorer displayed")
    @Key("objectExplorerDisplayed")
    String objectExplorerDisplayed();

    /**
     * Translated "URL Viewer displayed".
     *
     * @return translated "URL Viewer displayed"
     */
    @DefaultMessage("URL Viewer displayed")
    @Key("urlViewerDisplayed")
    String urlViewerDisplayed();

    /**
     * Translated "URL Browser".
     *
     * @return translated "URL Browser"
     */
    @DefaultMessage("URL Browser")
    @Key("urlBrowser")
    String urlBrowser();

    /**
     * Translated "{0} URL Browser".
     *
     * @return translated "{0} URL Browser"
     */
    @DefaultMessage("{0} URL Browser")
    @Key("accessibleNameBrowser")
    String accessibleNameBrowser(String name);

    /**
     * Translated "Untitled URL Browser".
     *
     * @return translated "Untitled URL Browser"
     */
    @DefaultMessage("Untitled URL Browser")
    @Key("untitledUrlBrowser")
    String untitledUrlBrowser();

    /**
     * Translated "Source Viewer".
     *
     * @return translated "Source Viewer"
     */
    @DefaultMessage("Source Viewer")
    @Key("sourceViewer")
    String sourceViewer();

    /**
     * Translated "Name".
     *
     * @return translated "Name"
     */
    @DefaultMessage("Name")
    @Key("name")
    String name();

    /**
     * Translated "Type".
     *
     * @return translated "Type"
     */
    @DefaultMessage("Type")
    @Key("type")
    String type();

    /**
     * Translated "Value".
     *
     * @return translated "Value"
     */
    @DefaultMessage("Value")
    @Key("value")
    String value();

    /**
     * Translated "View({0})".
     *
     * @return translated "View({0})"
     */
    @DefaultMessage("View({0})")
    @Key("viewCode")
    String viewCode(String code);

    /**
     * Translated "(No selection)".
     *
     * @return translated "(No selection)"
     */
    @DefaultMessage("(No selection)")
    @Key("noSelectionParentheses")
    String noSelectionParentheses();

    /**
     * Translated "Show Attributes".
     *
     * @return translated "Show Attributes"
     */
    @DefaultMessage("Show Attributes")
    @Key("showAttributes")
    String showAttributes();

    /**
     * Translated "Search objects".
     *
     * @return translated "Search objects"
     */
    @DefaultMessage("Search objects")
    @Key("searchObjects")
    String searchObjects();

    /**
     * Translated "Refresh".
     *
     * @return translated "Refresh"
     */
    @DefaultMessage("Refresh")
    @Key("refresh")
    String refresh();

    /**
     * Translated "The source file {0} does not exist.".
     *
     * @return translated "The source file {0} does not exist."
     */
    @DefaultMessage("The source file {0} does not exist.")
    @Key("sourceFileAtPathDoesNotExist")
    String sourceFileAtPathDoesNotExist(String selectedPath);

    /**
     * Translated "Error while opening profiler source".
     *
     * @return translated "Error while opening profiler source"
     */
    @DefaultMessage("Error while opening profiler source")
    @Key("errorOpeningProfilerSource")
    String errorOpeningProfilerSource();

    /**
     * Translated "Failed to Save Profile".
     *
     * @return translated "Failed to Save Profile"
     */
    @DefaultMessage("Failed to Save Profile")
    @Key("failedToSaveProfile")
    String failedToSaveProfile();

    /**
     * Translated "Save File - {0}".
     *
     * @return translated "Save File - {0}"
     */
    @DefaultMessage("Save File - {0}")
    @Key("saveFileName")
    String saveFileName(String name);

    /**
     * Translated "Failed to Save Profile Properties".
     *
     * @return translated "Failed to Save Profile Properties"
     */
    @DefaultMessage("Failed to Save Profile Properties")
    @Key("failedToSaveProfileProperties")
    String failedToSaveProfileProperties();

    /**
     * Translated "Code Profile results displayed".
     *
     * @return translated "Code Profile results displayed"
     */
    @DefaultMessage("Code Profile results displayed")
    @Key("codeProfileResultsDisplayed")
    String codeProfileResultsDisplayed();

    /**
     * Translated "Profile".
     *
     * @return translated "Profile"
     */
    @DefaultMessage("Profile")
    @Key("profileCapitalized")
    String profileCapitalized();

    /**
     * Translated "Profiler".
     *
     * @return translated "Profiler"
     */
    @DefaultMessage("Profiler")
    @Key("profilerCapitalized")
    String profilerCapitalized();

    /**
     * Translated "R Profiler".
     *
     * @return translated "R Profiler"
     */
    @DefaultMessage("R Profiler")
    @Key("rProfiler")
    String rProfiler();

    /**
     * Translated "{0} Profile View".
     *
     * @return translated "{0} Profile View"
     */
    @DefaultMessage("{0} Profile View")
    @Key("titleProfileView")
    String titleProfileView(String title);

    /**
     * Translated "Failed to Open Profile".
     *
     * @return translated "Failed to Open Profile"
     */
    @DefaultMessage("Failed to Open Profile")
    @Key("failedToOpenProfile")
    String failedToOpenProfile();

    /**
     * Translated "Profiler Error".
     *
     * @return translated "Profiler Error"
     */
    @DefaultMessage("Profiler Error")
    @Key("profilerError")
    String profilerError();

    /**
     * Translated "Failed to Stop Profiler".
     *
     * @return translated "Failed to Stop Profiler"
     */
    @DefaultMessage("Failed to Stop Profiler")
    @Key("failedToStopProfiler")
    String failedToStopProfiler();

    /**
     * Translated "Error navigating to file".
     *
     * @return translated "Error navigating to file"
     */
    @DefaultMessage("Error navigating to file")
    @Key("errorNavigatingToFile")
    String errorNavigatingToFile();

    /**
     * Translated "No file at path ''{0}''.".
     *
     * @return translated "No file at path ''{0}''."
     */
    @DefaultMessage("No file at path ''{0}''.")
    @Key("noFileAtPath")
    String noFileAtPath(String finalUrl);

    /**
     * Translated "Open Link (Command+Click)".
     *
     * @return translated "Open Link (Command+Click)"
     */
    @DefaultMessage("Open Link (Command+Click)")
    @Key("openLinkMacCommand")
    String openLinkMacCommand();

    /**
     * Translated "Open Link (Shift+Click)".
     *
     * @return translated "Open Link (Shift+Click)"
     */
    @DefaultMessage("Open Link (Shift+Click)")
    @Key("openLinkNotMacCommand")
    String openLinkNotMacCommand();

    /**
     * Translated "Finding definition...".
     *
     * @return translated "Finding definition..."
     */
    @DefaultMessage("Finding definition...")
    @Key("findingDefinition")
    String findingDefinition();

    /**
     * Translated "ignored".
     *
     * @return translated "ignored"
     */
    @DefaultMessage("ignored")
    @Key("ignored")
    String ignored();

    /**
     * Translated "note".
     *
     * @return translated "note"
     */
    @DefaultMessage("note")
    @Key("note")
    String note();

    /**
     * Translated "warning".
     *
     * @return translated "warning"
     */
    @DefaultMessage("warning")
    @Key("warningLowercase")
    String warningLowercase();

    /**
     * Translated "error".
     *
     * @return translated "error"
     */
    @DefaultMessage("error")
    @Key("error")
    String error();

    /**
     * Translated "fatal".
     *
     * @return translated "fatal"
     */
    @DefaultMessage("fatal")
    @Key("fatal")
    String fatal();

    /**
     * Translated "(No matches)".
     *
     * @return translated "(No matches)"
     */
    @DefaultMessage("(No matches)")
    @Key("noMatchesParentheses")
    String noMatchesParentheses();

    /**
     * Translated "{0} of {1}".
     *
     * @return translated "{0} of {1}"
     */
    @DefaultMessage("{0} of {1}")
    @Key("pagingLabelTextOf")
    String pagingLabelTextOf(int index, int textLength);

    /**
     * Translated "{0} occurrences replaced.".
     *
     * @return translated "{0} occurrences replaced."
     */
    @DefaultMessage("{0} occurrences replaced.")
    @Key("numberOfOccurrencesReplaced")
    String numberOfOccurrencesReplaced(int occurrences);

    /**
     * Translated "Invalid search term.".
     *
     * @return translated "Invalid search term."
     */
    @DefaultMessage("Invalid search term.")
    @Key("invalidSearchTerm")
    String invalidSearchTerm();

    /**
     * Translated "No more occurrences.".
     *
     * @return translated "No more occurrences."
     */
    @DefaultMessage("No more occurrences.")
    @Key("noMoreOccurrences")
    String noMoreOccurrences();

    /**
     * Translated "Find".
     *
     * @return translated "Find"
     */
    @DefaultMessage("Find")
    @Key("findCapitalized")
    String findCapitalized();

    /**
     * Translated "Find/Replace".
     *
     * @return translated "Find/Replace"
     */
    @DefaultMessage("Find/Replace")
    @Key("findOrReplace")
    String findOrReplace();

    /**
     * Translated "Replace".
     *
     * @return translated "Replace"
     */
    @DefaultMessage("Replace")
    @Key("replaceCapitalized")
    String replaceCapitalized();

    /**
     * Translated "All".
     *
     * @return translated "All"
     */
    @DefaultMessage("All")
    @Key("allCapitalized")
    String allCapitalized();

    /**
     * Translated "Replace all occurrences".
     *
     * @return translated "Replace all occurrences"
     */
    @DefaultMessage("Replace all occurrences")
    @Key("replaceAllOccurrences")
    String replaceAllOccurrences();

    /**
     * Translated "In selection".
     *
     * @return translated "In selection"
     */
    @DefaultMessage("In selection")
    @Key("inSelection")
    String inSelection();

    /**
     * Translated "Match case".
     *
     * @return translated "Match case"
     */
    @DefaultMessage("Match case")
    @Key("matchCase")
    String matchCase();

    /**
     * Translated "Whole word".
     *
     * @return translated "Whole word"
     */
    @DefaultMessage("Whole word")
    @Key("wholeWord")
    String wholeWord();

    /**
     * Translated "Regex".
     *
     * @return translated "Regex"
     */
    @DefaultMessage("Regex")
    @Key("regexCapitalized")
    String regexCapitalized();

    /**
     * Translated "Wrap".
     *
     * @return translated "Wrap"
     */
    @DefaultMessage("Wrap")
    @Key("wrapCapitalized")
    String wrapCapitalized();

    /**
     * Translated "Close find and replace".
     *
     * @return translated "Close find and replace"
     */
    @DefaultMessage("Close find and replace")
    @Key("closeFindAndReplace")
    String closeFindAndReplace();

    /**
     * Translated "Chunk Pending Execution".
     *
     * @return translated "Chunk Pending Execution"
     */
    @DefaultMessage("Chunk Pending Execution")
    @Key("chunkPendingExecution")
    String chunkPendingExecution();

    /**
     * Translated "The code in this chunk is scheduled to run later, when other chunks have finished executing.".
     *
     * @return translated "The code in this chunk is scheduled to run later, when other chunks have finished executing."
     */
    @DefaultMessage("The code in this chunk is scheduled to run later, when other chunks have finished executing.")
    @Key("chunkPendingExecutionMessage")
    String chunkPendingExecutionMessage();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    @Key("okFullyCapitalized")
    String okFullyCapitalized();

    /**
     * Translated "Don''t Run".
     *
     * @return translated "Don''t Run"
     */
    @DefaultMessage("Don''t Run")
    @Key("dontRun")
    String dontRun();

    /**
     * Translated "The rmarkdown package is not installed; notebook HTML file will not be generated.".
     *
     * @return translated "The rmarkdown package is not installed; notebook HTML file will not be generated."
     */
    @DefaultMessage("The rmarkdown package is not installed; notebook HTML file will not be generated.")
    @Key("rMarkdownNotInstalledHTMLNoGenerate")
    String rMarkdownNotInstalledHTMLNoGenerate();

    /**
     * Translated "An updated version of the rmarkdown package is required to generate notebook HTML files.".
     *
     * @return translated "An updated version of the rmarkdown package is required to generate notebook HTML files."
     */
    @DefaultMessage("An updated version of the rmarkdown package is required to generate notebook HTML files.")
    @Key("rMarkdownUpgradeRequired")
    String rMarkdownUpgradeRequired();

    /**
     * Translated "Error creating notebook: ".
     *
     * @return translated "Error creating notebook: "
     */
    @DefaultMessage("Error creating notebook: ")
    @Key("errorCreatingNotebookPrefix")
    String errorCreatingNotebookPrefix();

    /**
     * Translated "Creating R Notebooks".
     *
     * @return translated "Creating R Notebooks"
     */
    @DefaultMessage("Creating R Notebooks")
    @Key("creatingRNotebooks")
    String creatingRNotebooks();

    /**
     * Translated "Can''t execute {0}".
     *
     * @return translated "Can''t execute {0}"
     */
    @DefaultMessage("Can''t execute {0}")
    @Key("cantExecuteJobDesc")
    String cantExecuteJobDesc(String jobDesc);

    /**
     * Translated "Executing Python chunks".
     *
     * @return translated "Executing Python chunks"
     */
    @DefaultMessage("Executing Python chunks")
    @Key("executingPythonChunks")
    String executingPythonChunks();

    /**
     * Translated "Executing chunks".
     *
     * @return translated "Executing chunks"
     */
    @DefaultMessage("Executing chunks")
    @Key("executingChunks")
    String executingChunks();

    /**
     * Translated "Run Chunk".
     *
     * @return translated "Run Chunk"
     */
    @DefaultMessage("Run Chunk")
    @Key("runChunk")
    String runChunk();

    /**
     * Translated "{0}: Chunks Currently Executing".
     *
     * @return translated "{0}: Chunks Currently Executing"
     */
    @DefaultMessage("{0}: Chunks Currently Executing")
    @Key("jobChunkCurrentlyExecuting")
    String jobChunkCurrentlyExecuting(String jobDesc);

    /**
     * Translated "RStudio cannot execute ''{0}'' because this notebook is already executing code. Interrupt R, or wait for execution to complete.".
     *
     * @return translated "RStudio cannot execute ''{0}'' because this notebook is already executing code. Interrupt R, or wait for execution to complete."
     */
    @DefaultMessage("RStudio cannot execute ''{0}'' because this notebook is already executing code. Interrupt R, or wait for execution to complete.")
    @Key("rStudioCannotExecuteJob")
    String rStudioCannotExecuteJob(String jobDesc);

    /**
     * Translated "Run Chunks".
     *
     * @return translated "Run Chunks"
     */
    @DefaultMessage("Run Chunks")
    @Key("runChunks")
    String runChunks();

    /**
     * Translated "Chunks Currently Running".
     *
     * @return translated "Chunks Currently Running"
     */
    @DefaultMessage("Chunks Currently Running")
    @Key("chunksCurrentlyRunning")
    String chunksCurrentlyRunning();

    /**
     * Translated "Output can''t be cleared because there are still chunks running. Do you want to interrupt them?".
     *
     * @return translated "Output can''t be cleared because there are still chunks running. Do you want to interrupt them?"
     */
    @DefaultMessage("Output can''t be cleared because there are still chunks running. Do you want to interrupt them?")
    @Key("outputCantBeClearedBecauseChunks")
    String outputCantBeClearedBecauseChunks();

    /**
     * Translated "Interrupt and Clear Output".
     *
     * @return translated "Interrupt and Clear Output"
     */
    @DefaultMessage("Interrupt and Clear Output")
    @Key("interruptAndClearOutput")
    String interruptAndClearOutput();

    /**
     * Translated "Remove Inline Chunk Output".
     *
     * @return translated "Remove Inline Chunk Output"
     */
    @DefaultMessage("Remove Inline Chunk Output")
    @Key("removeInlineChunkOutput")
    String removeInlineChunkOutput();

    /**
     * Translated "Do you want to clear all the existing chunk output from your notebook?".
     *
     * @return translated "Do you want to clear all the existing chunk output from your notebook?"
     */
    @DefaultMessage("Do you want to clear all the existing chunk output from your notebook?")
    @Key("clearExistingChunkOutputMessage")
    String clearExistingChunkOutputMessage();

    /**
     * Translated "Remove Output".
     *
     * @return translated "Remove Output"
     */
    @DefaultMessage("Remove Output")
    @Key("removeOutput")
    String removeOutput();

    /**
     * Translated "Keep Output".
     *
     * @return translated "Keep Output"
     */
    @DefaultMessage("Keep Output")
    @Key("keepOutput")
    String keepOutput();

    /**
     * Translated "Unnamed chunk".
     *
     * @return translated "Unnamed chunk"
     */
    @DefaultMessage("Unnamed chunk")
    @Key("unnamedChunk")
    String unnamedChunk();

    /**
     * Translated "Chunk Name:".
     *
     * @return translated "Chunk Name:"
     */
    @DefaultMessage("Chunk Name:")
    @Key("chunkNameColon")
    String chunkNameColon();

    /**
     * Translated "Output:".
     *
     * @return translated "Output:"
     */
    @DefaultMessage("Output:")
    @Key("outputColon")
    String outputColon();

    /**
     * Translated "Show warnings".
     *
     * @return translated "Show warnings"
     */
    @DefaultMessage("Show warnings")
    @Key("showWarnings")
    String showWarnings();

    /**
     * Translated "Show messages".
     *
     * @return translated "Show messages"
     */
    @DefaultMessage("Show messages")
    @Key("showMessages")
    String showMessages();

    /**
     * Translated "Cache chunk".
     *
     * @return translated "Cache chunk"
     */
    @DefaultMessage("Cache chunk")
    @Key("cacheChunk")
    String cacheChunk();

    /**
     * Translated "Use paged tables".
     *
     * @return translated "Use paged tables"
     */
    @DefaultMessage("Use paged tables")
    @Key("usePagedTables")
    String usePagedTables();

    /**
     * Translated "Use custom figure size".
     *
     * @return translated "Use custom figure size"
     */
    @DefaultMessage("Use custom figure size")
    @Key("useCustomFigureSize")
    String useCustomFigureSize();

    /**
     * Translated "Width (inches):".
     *
     * @return translated "Width (inches):"
     */
    @DefaultMessage("Width (inches):")
    @Key("widthInchesColon")
    String widthInchesColon();

    /**
     * Translated "Height (inches):".
     *
     * @return translated "Height (inches):"
     */
    @DefaultMessage("Height (inches):")
    @Key("heightInchesColon")
    String heightInchesColon();

    /**
     * Translated "Engine path:".
     *
     * @return translated "Engine path:"
     */
    @DefaultMessage("Engine path:")
    @Key("enginePathColon")
    String enginePathColon();

    /**
     * Translated "Select Engine".
     *
     * @return translated "Select Engine"
     */
    @DefaultMessage("Select Engine")
    @Key("selectEngine")
    String selectEngine();

    /**
     * Translated "Engine options:".
     *
     * @return translated "Engine options:"
     */
    @DefaultMessage("Engine options:")
    @Key("engineOptionsColon")
    String engineOptionsColon();

    /**
     * Translated "Chunk options".
     *
     * @return translated "Chunk options"
     */
    @DefaultMessage("Chunk options")
    @Key("chunkOptions")
    String chunkOptions();

    /**
     * Translated "Revert".
     *
     * @return translated "Revert"
     */
    @DefaultMessage("Revert")
    @Key("revertCapitalized")
    String revertCapitalized();

    /**
     * Translated "Apply".
     *
     * @return translated "Apply"
     */
    @DefaultMessage("Apply")
    @Key("applyCapitalized")
    String applyCapitalized();

    /**
     * Translated "Show nothing (don''t run code)".
     *
     * @return translated "Show nothing (don''t run code)"
     */
    @DefaultMessage("Show nothing (don''t run code)")
    @Key("showNothingDontRunCode")
    String showNothingDontRunCode();

    /**
     * Translated "Show nothing (run code)".
     *
     * @return translated "Show nothing (run code)"
     */
    @DefaultMessage("Show nothing (run code)")
    @Key("showNothingRunCode")
    String showNothingRunCode();

    /**
     * Translated "Show code and output".
     *
     * @return translated "Show code and output"
     */
    @DefaultMessage("Show code and output")
    @Key("showCodeAndOutput")
    String showCodeAndOutput();

    /**
     * Translated "Show output only".
     *
     * @return translated "Show output only"
     */
    @DefaultMessage("Show output only")
    @Key("showOutputOnly")
    String showOutputOnly();

    /**
     * Translated "(Use document default)".
     *
     * @return translated "(Use document default)"
     */
    @DefaultMessage("(Use document default)")
    @Key("useDocumentDefaultParentheses")
    String useDocumentDefaultParentheses();

    /**
     * Translated "Default Chunk Options".
     *
     * @return translated "Default Chunk Options"
     */
    @DefaultMessage("Default Chunk Options")
    @Key("defaultChunkOptions")
    String defaultChunkOptions();

    /**
     * Translated "Check Spelling".
     *
     * @return translated "Check Spelling"
     */
    @DefaultMessage("Check Spelling")
    @Key("checkSpelling")
    String checkSpelling();

    /**
     * Translated "An error has occurred:\n\n{0}".
     *
     * @return translated "An error has occurred:\n\n{0}"
     */
    @DefaultMessage("An error has occurred:\n\n{0}")
    @Key("anErrorHasOccurredMessage")
    String anErrorHasOccurredMessage(String eMessage);

    /**
     * Translated "Spell check is complete.".
     *
     * @return translated "Spell check is complete."
     */
    @DefaultMessage("Spell check is complete.")
    @Key("spellCheckIsComplete")
    String spellCheckIsComplete();

    /**
     * Translated "Spell check in progress...".
     *
     * @return translated "Spell check in progress..."
     */
    @DefaultMessage("Spell check in progress...")
    @Key("spellCheckInProgress")
    String spellCheckInProgress();

    /**
     * Translated "Add".
     *
     * @return translated "Add"
     */
    @DefaultMessage("Add")
    @Key("addCapitalized")
    String addCapitalized();

    /**
     * Translated "Add word to user dictionary".
     *
     * @return translated "Add word to user dictionary"
     */
    @DefaultMessage("Add word to user dictionary")
    @Key("addWordToUserDictionary")
    String addWordToUserDictionary();

    /**
     * Translated "Skip".
     *
     * @return translated "Skip"
     */
    @DefaultMessage("Skip")
    @Key("skip")
    String skip();

    /**
     * Translated "Ignore All".
     *
     * @return translated "Ignore All"
     */
    @DefaultMessage("Ignore All")
    @Key("ignoreAll")
    String ignoreAll();

    /**
     * Translated "Change".
     *
     * @return translated "Change"
     */
    @DefaultMessage("Change")
    @Key("changeCapitalized")
    String changeCapitalized();

    /**
     * Translated "Change All".
     *
     * @return translated "Change All"
     */
    @DefaultMessage("Change All")
    @Key("changeAll")
    String changeAll();

    /**
     * Translated "Suggestions".
     *
     * @return translated "Suggestions"
     */
    @DefaultMessage("Suggestions")
    @Key("suggestionsCapitalized")
    String suggestionsCapitalized();

    /**
     * Translated "Checking...".
     *
     * @return translated "Checking..."
     */
    @DefaultMessage("Checking...")
    @Key("checkingEllipses")
    String checkingEllipses();

    /**
     * Translated "Class".
     *
     * @return translated "Class"
     */
    @DefaultMessage("Class")
    @Key("classCapitalized")
    String classCapitalized();

    /**
     * Translated "Namespace".
     *
     * @return translated "Namespace"
     */
    @DefaultMessage("Namespace")
    @Key("namespaceCapitalized")
    String namespaceCapitalized();

    /**
     * Translated "Lambda".
     *
     * @return translated "Lambda"
     */
    @DefaultMessage("Lambda")
    @Key("lambdaCapitalized")
    String lambdaCapitalized();

    /**
     * Translated "Anonymous".
     *
     * @return translated "Anonymous"
     */
    @DefaultMessage("Anonymous")
    @Key("anonymousCapitalized")
    String anonymousCapitalized();

    /**
     * Translated "Function".
     *
     * @return translated "Function"
     */
    @DefaultMessage("Function")
    @Key("functionCapitalized")
    String functionCapitalized();

    /**
     * Translated "Test".
     *
     * @return translated "Test"
     */
    @DefaultMessage("Test")
    @Key("testCapitalized")
    String testCapitalized();

    /**
     * Translated "Chunk".
     *
     * @return translated "Chunk"
     */
    @DefaultMessage("Chunk")
    @Key("chunkCapitalized")
    String chunkCapitalized();

    /**
     * Translated "Section".
     *
     * @return translated "Section"
     */
    @DefaultMessage("Section")
    @Key("sectionCapitalized")
    String sectionCapitalized();

    /**
     * Translated "Slide".
     *
     * @return translated "Slide"
     */
    @DefaultMessage("Slide")
    @Key("slideCapitalized")
    String slideCapitalized();

    /**
     * Translated "Tomorrow Night".
     *
     * @return translated "Tomorrow Night"
     */
    @DefaultMessage("Tomorrow Night")
    @Key("tomorrowNight")
    String tomorrowNight();

    /**
     * Translated "Textmate (default)".
     *
     * @return translated "Textmate (default)"
     */
    @DefaultMessage("Textmate (default)")
    @Key("textmateDefaultParentheses")
    String textmateDefaultParentheses();

    /**
     * Translated "The specified theme does not exist".
     *
     * @return translated "The specified theme does not exist"
     */
    @DefaultMessage("The specified theme does not exist")
    @Key("specifiedThemeDoesNotExist")
    String specifiedThemeDoesNotExist();

    /**
     * Translated "The specified theme is a default RStudio theme and cannot be removed.".
     *
     * @return translated "The specified theme is a default RStudio theme and cannot be removed."
     */
    @DefaultMessage("The specified theme is a default RStudio theme and cannot be removed.")
    @Key("specifiedDefaultThemeCannotBeRemoved")
    String specifiedDefaultThemeCannotBeRemoved();

    /**
     * Translated "Choose Encoding".
     *
     * @return translated "Choose Encoding"
     */
    @DefaultMessage("Choose Encoding")
    @Key("chooseEncoding")
    String chooseEncoding();

    /**
     * Translated "Encodings".
     *
     * @return translated "Encodings"
     */
    @DefaultMessage("Encodings")
    @Key("encodingsCapitalized")
    String encodingsCapitalized();

    /**
     * Translated "Show all encodings".
     *
     * @return translated "Show all encodings"
     */
    @DefaultMessage("Show all encodings")
    @Key("showAllEncodings")
    String showAllEncodings();

    /**
     * Translated "Set as default encoding for source files".
     *
     * @return translated "Set as default encoding for source files"
     */
    @DefaultMessage("Set as default encoding for source files")
    @Key("setAsDefaultEncodingSourceFiles")
    String setAsDefaultEncodingSourceFiles();

    /**
     * Translated "{0} (System default)".
     *
     * @return translated "{0} (System default)"
     */
    @DefaultMessage("{0} (System default)")
    @Key("sysEncNameDefault")
    String sysEncNameDefault(String sysEncName);

    /**
     * Translated "[Ask]".
     *
     * @return translated "[Ask]"
     */
    @DefaultMessage("[Ask]")
    @Key("askSquareBrackets")
    String askSquareBrackets();

    /**
     * Translated "New R Documentation File".
     *
     * @return translated "New R Documentation File"
     */
    @DefaultMessage("New R Documentation File")
    @Key("newRDocumentationFile")
    String newRDocumentationFile();

    /**
     * Translated "Name Not Specified".
     *
     * @return translated "Name Not Specified"
     */
    @DefaultMessage("Name Not Specified")
    @Key("nameNotSpecified")
    String nameNotSpecified();

    /**
     * Translated "You must specify a topic name for the new Rd file.".
     *
     * @return translated "You must specify a topic name for the new Rd file."
     */
    @DefaultMessage("You must specify a topic name for the new Rd file.")
    @Key("mustSpecifyTopicNameForRdFile")
    String mustSpecifyTopicNameForRdFile();

    /**
     * Translated "New R Markdown".
     *
     * @return translated "New R Markdown"
     */
    @DefaultMessage("New R Markdown")
    @Key("newRMarkdown")
    String newRMarkdown();

    /**
     * Translated "Templates".
     *
     * @return translated "Templates"
     */
    @DefaultMessage("Templates")
    @Key("templatesCapitalized")
    String templatesCapitalized();

    /**
     * Translated "Create Empty Document".
     *
     * @return translated "Create Empty Document"
     */
    @DefaultMessage("Create Empty Document")
    @Key("createEmptyDocument")
    String createEmptyDocument();

    /**
     * Translated "Using Shiny with R Markdown".
     *
     * @return translated "Using Shiny with R Markdown"
     */
    @DefaultMessage("Using Shiny with R Markdown")
    @Key("usingShinyWithRMarkdown")
    String usingShinyWithRMarkdown();

    /**
     * Translated "Create an HTML document with interactive Shiny components.".
     *
     * @return translated "Create an HTML document with interactive Shiny components."
     */
    @DefaultMessage("Create an HTML document with interactive Shiny components.")
    @Key("shinyDocNameDescription")
    String shinyDocNameDescription();

    /**
     * Translated "Create an IOSlides presentation with interactive Shiny components.".
     *
     * @return translated "Create an IOSlides presentation with interactive Shiny components."
     */
    @DefaultMessage("Create an IOSlides presentation with interactive Shiny components.")
    @Key("shinyPresentationNameDescription")
    String shinyPresentationNameDescription();

    /**
     * Translated "From Template".
     *
     * @return translated "From Template"
     */
    @DefaultMessage("From Template")
    @Key("fromTemplate")
    String fromTemplate();

    /**
     * Translated "Shiny".
     *
     * @return translated "Shiny"
     */
    @DefaultMessage("Shiny")
    @Key("shinyCapitalized")
    String shinyCapitalized();

    /**
     * Translated "Shiny Document".
     *
     * @return translated "Shiny Document"
     */
    @DefaultMessage("Shiny Document")
    @Key("shinyDocument")
    String shinyDocument();

    /**
     * Translated "Shiny Presentation".
     *
     * @return translated "Shiny Presentation"
     */
    @DefaultMessage("Shiny Presentation")
    @Key("shinyPresentation")
    String shinyPresentation();

    /**
     * Translated "No Parameters Defined".
     *
     * @return translated "No Parameters Defined"
     */
    @DefaultMessage("No Parameters Defined")
    @Key("noParametersDefined")
    String noParametersDefined();

    /**
     * Translated "There are no parameters defined for the current R Markdown document.".
     *
     * @return translated "There are no parameters defined for the current R Markdown document."
     */
    @DefaultMessage("There are no parameters defined for the current R Markdown document.")
    @Key("noParametersDefinedForCurrentRMarkdown")
    String noParametersDefinedForCurrentRMarkdown();

    /**
     * Translated "Using R Markdown Parameters".
     *
     * @return translated "Using R Markdown Parameters"
     */
    @DefaultMessage("Using R Markdown Parameters")
    @Key("usingRMarkdownParameters")
    String usingRMarkdownParameters();

    /**
     * Translated "Unable to activate visual mode (unsupported front matter format or non top-level YAML block)".
     *
     * @return translated "Unable to activate visual mode (unsupported front matter format or non top-level YAML block)"
     */
    @DefaultMessage("Unable to activate visual mode (unsupported front matter format or non top-level YAML block)")
    @Key("unableToActivateVisualModeYAML")
    String unableToActivateVisualModeYAML();

    /**
     * Translated "Unable to activate visual mode (error parsing code chunks out of document)".
     *
     * @return translated "Unable to activate visual mode (error parsing code chunks out of document)"
     */
    @DefaultMessage("Unable to activate visual mode (error parsing code chunks out of document)")
    @Key("unableToActivateVisualModeParsingCode")
    String unableToActivateVisualModeParsingCode();

    /**
     * Translated "Unable to activate visual mode (document contains example lists which are not currently supported)".
     *
     * @return translated "Unable to activate visual mode (document contains example lists which are not currently supported)"
     */
    @DefaultMessage("Unable to activate visual mode (document contains example lists which are not currently supported)")
    @Key("unableToActivateVisualModeDocumentContains")
    String unableToActivateVisualModeDocumentContains();

    /**
     * Translated "Unrecognized Pandoc token(s); {0}".
     *
     * @return translated "Unrecognized Pandoc token(s); {0}"
     */
    @DefaultMessage("Unrecognized Pandoc token(s); {0}")
    @Key("unrecognizedPandocTokens")
    String unrecognizedPandocTokens(String tokens);

    /**
     * Translated "Invalid Pandoc format: {0}".
     *
     * @return translated "Invalid Pandoc format: {0}"
     */
    @DefaultMessage("Invalid Pandoc format: {0}")
    @Key("invalidPandocFormat")
    String invalidPandocFormat(String format);

    /**
     * Translated "Unsupported extensions for markdown mode: {0}".
     *
     * @return translated "Unsupported extensions for markdown mode: {0}"
     */
    @DefaultMessage("Unsupported extensions for markdown mode: {0}")
    @Key("unsupportedExtensionsForMarkdown")
    String unsupportedExtensionsForMarkdown(String format);

    /**
     * Translated "Unable to parse markdown (please report at https://github.com/rstudio/rstudio/issues/new)".
     *
     * @return translated "Unable to parse markdown (please report at https://github.com/rstudio/rstudio/issues/new)"
     */
    @DefaultMessage("Unable to parse markdown (please report at https://github.com/rstudio/rstudio/issues/new)")
    @Key("unableToParseMarkdownPleaseReport")
    String unableToParseMarkdownPleaseReport();

    /**
     * Translated "Chunk {0}".
     *
     * @return translated "Chunk {0}"
     */
    @DefaultMessage("Chunk {0}")
    @Key("chunkSequence")
    String chunkSequence(int itemSequence);

    /**
     * Translated "(Top Level)".
     *
     * @return translated "(Top Level)"
     */
    @DefaultMessage("(Top Level)")
    @Key("topLevelParentheses")
    String topLevelParentheses();

    /**
     * Translated "You cannot enter visual mode while using realtime collaboration.".
     *
     * @return translated "You cannot enter visual mode while using realtime collaboration."
     */
    @DefaultMessage("You cannot enter visual mode while using realtime collaboration.")
    @Key("cantEnterVisualModeUsingRealtime")
    String cantEnterVisualModeUsingRealtime();

    /**
     * Translated "{0}, {1} line".
     *
     * @return translated "{0}, {1} line"
     */
    @DefaultMessage("{0}, {1} line")
    @Key("visualModeChunkSummary")
    String visualModeChunkSummary(String enging, int lines);

    /**
     * Translated "{0}, {1} lines".
     *
     * @return translated "{0}, {1} lines"
     */
    @DefaultMessage("{0}, {1} lines")
    @Key("visualModeChunkSummaryPlural")
    String visualModeChunkSummaryPlural(String engine, int lines);

    /**
     * Translated "images".
     *
     * @return translated "images"
     */
    @DefaultMessage("images")
    @Key("images")
    String images();

    /**
     * Translated "Xaringan presentations cannot be edited in visual mode.".
     * Do not localize Xaringan
     * @return translated "Xaringan presentations cannot be edited in visual mode."
     */
    @DefaultMessage("Xaringan presentations cannot be edited in visual mode.")
    @Key("xaringanPresentationsVisualMode")
    String xaringanPresentationsVisualMode();

    /**
     * Translated "Version control conflict markers detected. Please resolve them before editing in visual mode.".
     *
     * @return translated "Version control conflict markers detected. Please resolve them before editing in visual mode."
     */
    @DefaultMessage("Version control conflict markers detected. Please resolve them before editing in visual mode.")
    @Key("versionControlConflict")
    String versionControlConflict();

    /**
     * Translated "Switch to Visual Mode".
     *
     * @return translated "Switch to Visual Mode"
     */
    @DefaultMessage("Switch to Visual Mode")
    @Key("switchToVisualMode")
    String switchToVisualMode();

    /**
     * Translated "Use Visual Mode".
     *
     * @return translated "Use Visual Mode"
     */
    @DefaultMessage("Use Visual Mode")
    @Key("useVisualMode")
    String useVisualMode();

    /**
     * Translated "Don''t show this message again".
     *
     * @return translated "Don''t show this message again"
     */
    @DefaultMessage("Don''t show this message again")
    @Key("dontShowMessageAgain")
    String dontShowMessageAgain();

    /**
     * Translated "Line Wrapping".
     *
     * @return translated "Line Wrapping"
     */
    @DefaultMessage("Line Wrapping")
    @Key("lineWrapping")
    String lineWrapping();

    /**
     * Translated "project default".
     *
     * @return translated "project default"
     */
    @DefaultMessage("project default")
    @Key("projectDefault")
    String projectDefault();

    /**
     * Translated "global default".
     *
     * @return translated "global default"
     */
    @DefaultMessage("global default")
    @Key("globalDefault")
    String globalDefault();

    /**
     * Translated "Line wrapping in this document differs from the {0}:".
     *
     * @return translated "Line wrapping in this document differs from the {0}:"
     */
    @DefaultMessage("Line wrapping in this document differs from the {0}:")
    @Key("lineWrappingDiffersFromCurrent")
    String lineWrappingDiffersFromCurrent(String current);

    /**
     * Translated "Select how you''d like to handle line wrapping below:".
     *
     * @return translated "Select how you''d like to handle line wrapping below:"
     */
    @DefaultMessage("Select how you''d like to handle line wrapping below:")
    @Key("selectHandleLineWrapping")
    String selectHandleLineWrapping();

    /**
     * Translated "Use {0}-based line wrapping for this document".
     *
     * @return translated "Use {0}-based line wrapping for this document"
     */
    @DefaultMessage("Use {0}-based line wrapping for this document")
    @Key("useBasedLineWrapping")
    String useBasedLineWrapping(String detectedLineWrapping);

    /**
     * Translated "Use the current {0} default line wrapping for this document".
     *
     * @return translated "Use the current {0} default line wrapping for this document"
     */
    @DefaultMessage("Use the current {0} default line wrapping for this document")
    @Key("useDefaultLinewrapping")
    String useDefaultLinewrapping(String projectConfigChoice);

    /**
     * Translated "project".
     *
     * @return translated "project"
     */
    @DefaultMessage("project")
    @Key("project")
    String project();

    /**
     * Translated "global".
     *
     * @return translated "global"
     */
    @DefaultMessage("global")
    @Key("global")
    String global();

    /**
     * Translated "Learn more about visual mode line wrapping options".
     *
     * @return translated "Learn more about visual mode line wrapping options"
     */
    @DefaultMessage("Learn more about visual mode line wrapping options")
    @Key("learnAboutVisualModeLineWrapping")
    String learnAboutVisualModeLineWrapping();

    /**
     * Translated "Wrap at column:".
     *
     * @return translated "Wrap at column:"
     */
    @DefaultMessage("Wrap at column:")
    @Key("wrapAtColumnColon")
    String wrapAtColumnColon();

    /**
     * Translated "Collapse".
     *
     * @return translated "Collapse"
     */
    @DefaultMessage("Collapse")
    @Key("collapseCapitalized")
    String collapseCapitalized();

    /**
     * Translated "Expand".
     *
     * @return translated "Expand"
     */
    @DefaultMessage("Expand")
    @Key("expandCapitalized")
    String expandCapitalized();

    /**
     * Translated "{0} code chunk".
     *
     * @return translated "{0} code chunk"
     */
    @DefaultMessage("{0} code chunk")
    @Key("collapseOrExpandCodeChunk")
    String collapseOrExpandCodeChunk(String hintText);

    /**
     * Translated "Some of your source edits are still being backed up. If you continue, your latest changes may be lost. Do you want to continue?".
     *
     * @return translated "Some of your source edits are still being backed up. If you continue, your latest changes may be lost. Do you want to continue?"
     */
    @DefaultMessage("Some of your source edits are still being backed up. If you continue, your latest changes may be lost. Do you want to continue?")
    @Key("editsStillBeingBackedUp")
    String editsStillBeingBackedUp();

    /**
     * Translated "The process cannot access the file because it is being used by another process".
     * Must match what is in rstudio\src\gwt\src\org\rstudio\studio\client\workbench\views\source\editors\text\EditorsTextConstants.java
     * @return translated "The process cannot access the file because it is being used by another process"
     */
    @DefaultMessage("The process cannot access the file because it is being used by another process")
    @Key("processStillBeingUsedTextEditingTarget")
    String processStillBeingUsedTextEditingTarget();

    /**
     * Translated "Error Autosaving File".
     *
     * @return translated "Error Autosaving File"
     */
    @DefaultMessage("Error Autosaving File")
    @Key("errorAutosavingFile")
    String errorAutosavingFile();

    /**
     * Translated "RStudio was unable to autosave this file. You may need to restart RStudio.".
     *
     * @return translated "RStudio was unable to autosave this file. You may need to restart RStudio."
     */
    @DefaultMessage("RStudio was unable to autosave this file. You may need to restart RStudio.")
    @Key("rStudioUnableToAutosave")
    String rStudioUnableToAutosave();

    /**
     * Translated "Could not save {0}: {1}".
     *
     * @return translated "Could not save {0}: {1}"
     */
    @DefaultMessage("Could not save {0}: {1}")
    @Key("couldNotSavePathPlusMessage")
    String couldNotSavePathPlusMessage(String path, String message);

    /**
     * Translated "Error saving {0}: {1}".
     *
     * @return translated "Error saving {0}: {1}"
     */
    @DefaultMessage("Error saving {0}: {1}")
    @Key("errorSavingPathPlusMessage")
    String errorSavingPathPlusMessage(String path, String message);

    /**
     * Translated "Document Outline".
     * @return translated "Document Outline"
     */
    @DefaultMessage("Document Outline")
    @Key("documentOutline")
    String documentOutline();

    /**
     * Translated "<td><input type=''button'' value=''More...'' data-action=''open''></input></td>".
     *
     * @return translated "<td><input type=''button'' value=''More...'' data-action=''open''></input></td>"
     */
    @DefaultMessage("<td><input type=''button'' value=''More...'' data-action=''open''></input></td>")
    @Key("moreButtonCell")
    String moreButtonCell();

    /**
     * Translated "{0} <...truncated...> )".
     *
     * @return translated "{0} <...truncated...> )"
     */
    @DefaultMessage("{0} <...truncated...> )")
    @Key("truncatedEllipses")
    String truncatedEllipses(String truncated);

    /**
     * Translated "The document uses {0}-based line wrapping".
     *
     * @return translated "The document uses {0}-based line wrapping"
     */
    @DefaultMessage("The document uses {0}-based line wrapping")
    @Key("documentUsesBasedLineWrapping")
    String documentUsesBasedLineWrapping(String detectedLineWrapping);

    /**
     * Translated "The {0} is no line wrapping".
     *
     * @return translated "The {0} is no line wrapping"
     */
    @DefaultMessage("The {0} is no line wrapping")
    @Key("defaultNoLineWrapping")
    String defaultNoLineWrapping(String current);

    /**
     * Translated "The {0} is {1}-based line wrapping".
     *
     * @return translated "The {0} is {1}-based line wrapping"
     */
    @DefaultMessage("The {0} is {1}-based line wrapping")
    @Key("defaultConfiguredBasedLineWrapping")
    String defaultConfiguredBasedLineWrapping(String current, String configuredLineWrapping);

    /**
     * Translated "Create R Notebook".
     * @return translated "Create R Notebook"
     */
    @DefaultMessage("Create R Notebook")
    @Key("createRNotebookText")
    String createRNotebookText();


    /**
     * Translated "Creating Shiny applications".
     * @return translated "Creating Shiny applications"
     */
    @DefaultMessage("Creating Shiny applications")
    @Key("creatingShinyApplicationsText")
    String creatingShinyApplicationsText();

    /**
     * Translated "Authoring R Presentations".
     * @return translated "Authoring R Presentations"
     */
    @DefaultMessage("Authoring R Presentations")
    @Key("authoringRPresentationsText")
    String authoringRPresentationsText();

    /**
     * Translated "Creating R Plumber API".
     * @return translated "Creating R Plumber API"
     */
    @DefaultMessage("Creating R Plumber API")
    @Key("creatingRPlumberAPIText")
    String creatingRPlumberAPIText();

    /**
     * Translated "Using R Notebooks".
     * @return translated "Using R Notebooks"
     */
    @DefaultMessage("Using R Notebooks")
    @Key("usingRNotebooksText")
    String usingRNotebooksText();

    /**
     * Translated "The profiler".
     * @return translated "The profiler"
     */
    @DefaultMessage("The profiler")
    @Key("theProfilerText")
    String theProfilerText();

    /**
     * Translated "Could not resolve {0}. Please make sure this is an R package project with a BugReports field set.".
     *
     * @return translated "Could not resolve {0}. Please make sure this is an R package project with a BugReports field set."
     */
    @DefaultMessage("Could not resolve {0}. Please make sure this is an R package project with a BugReports field set.")
    @Key("couldNotResolveIssue")
    String couldNotResolveIssue(String issue);

}

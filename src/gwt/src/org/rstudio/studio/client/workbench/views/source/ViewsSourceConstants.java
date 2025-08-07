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
     *
     * @return translated "(No documents)"
     */
    @DefaultMessage("(No documents)")
    String noDocumentsParentheses();

    /**
     * Translated "No outline available".
     *
     * @return translated "No outline available"
     */
    @DefaultMessage("No outline available")
    String noOutlineAvailable();

    /**
     * Translated "Title".
     *
     * @return translated "Title"
     */
    @DefaultMessage("Title")
    String title();

    /**
     * Translated "Invalid API Name".
     *
     * @return translated "Invalid API Name"
     */
    @DefaultMessage("Invalid API Name")
    String invalidApiName();

    /**
     * Translated "Invalid application name".
     *
     * @return translated "Invalid application name"
     */
    @DefaultMessage("Invalid application name")
    String invalidApplicationName();

    /**
     * Translated "The API name must not be empty".
     *
     * @return translated "The API name must not be empty"
     */
    @DefaultMessage("The API name must not be empty")
    String apiNameMustNotBeEmpty();

    /**
     * Translated "Plumber APIs".
     *
     * @return translated "Plumber APIs"
     */
    @DefaultMessage("Plumber APIs")
    String plumberAPIs();

    /**
     * Translated "Create within directory:".
     *
     * @return translated "Create within directory:"
     */
    @DefaultMessage("Create within directory:")
    String createWithinDirectoryColon();

    /**
     * Translated "API name:".
     *
     * @return translated "API name:"
     */
    @DefaultMessage("API name:")
    String apiNameColon();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    @DefaultMessage("Create")
    String create();

    /**
     * Translated "Application name:".
     *
     * @return translated "Application name:"
     */
    @DefaultMessage("Application name:")
    String applicationNameColon();

    /**
     * Translated "Application type:".
     *
     * @return translated "Application type:"
     */
    @DefaultMessage("Application type:")
    String applicationTypeColon();

    /**
     * Translated "Single File (app.R)".
     *
     * @return translated "Single File (app.R)"
     */
    @DefaultMessage("Single File (app.R)")
    String singleFileAppR();

    /**
     * Translated "Multiple File (ui.R/server.R)".
     *
     * @return translated "Multiple File (ui.R/server.R)"
     */
    @DefaultMessage("Multiple File (ui.R/server.R)")
    String multipleFileUiServerR();

    /**
     * Translated "Shiny Web Applications".
     *
     * @return translated "Shiny Web Applications"
     */
    @DefaultMessage("Shiny Web Applications")
    String shinyWebApplications();

    /**
     * Translated "The application name must not be empty".
     *
     * @return translated "The application name must not be empty"
     */
    @DefaultMessage("The application name must not be empty")
    String applicationNameMustNotBeEmpty();

    /**
     * Translated "Invalid Application Name".
     *
     * @return translated "Invalid Application Name"
     */
    @DefaultMessage("Invalid Application Name")
    String invalidApplicationNameCapitalized();

    /**
     * Translated "New Quarto {0}...".
     *
     * @return translated "New Quarto {0}..."
     */
    @DefaultMessage("New Quarto {0}...")
    String newQuatroProgressIndicator(String presentationType);

    /**
     * Translated "Presentation".
     *
     * @return translated "Presentation"
     */
    @DefaultMessage("Presentation")
    String presentationCapitalized();

    /**
     * Translated "Document".
     *
     * @return translated "Document"
     */
    @DefaultMessage("Document")
    String documentCapitalized();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    String errorCapitalized();

    /**
     * Translated "Always save files before build".
     *
     * @return translated "Always save files before build"
     */
    @DefaultMessage("Always save files before build")
    String alwaysSaveFilesBeforeBuild();

    /**
     * Translated "Show".
     *
     * @return translated "Show"
     */
    @DefaultMessage("Show")
    String show();

    /**
     * Translated "Source Document Error".
     *
     * @return translated "Source Document Error"
     */
    @DefaultMessage("Source Document Error")
    String sourceDocumentError();

    /**
     * Translated "Show Profiler".
     *
     * @return translated "Show Profiler"
     */
    @DefaultMessage("Show Profiler")
    String showProfiler();

    /**
     * Translated "R Notebook".
     *
     * @return translated "R Notebook"
     */
    @DefaultMessage("R Notebook")
    String rNotebook();

    /**
     * Translated "Notebook Creation Failed".
     *
     * @return translated "Notebook Creation Failed"
     */
    @DefaultMessage("Notebook Creation Failed")
    String notebookCreationFailed();

    /**
     * Translated "One or more packages required for R Notebook creation were not installed.".
     *
     * @return translated "One or more packages required for R Notebook creation were not installed."
     */
    @DefaultMessage("One or more packages required for R Notebook creation were not installed.")
    String rNotebookCreationFailedPackagesNotInstalled();

    /**
     * Translated "Creating Stan script".
     *
     * @return translated "Creating Stan script"
     */
    @DefaultMessage("Creating Stan script")
    String creatingStanScript();

    /**
     * Translated "Creating Stan scripts".
     *
     * @return translated "Creating Stan scripts"
     */
    @DefaultMessage("Creating Stan scripts")
    String creatingStanScriptPlural();

    /**
     * Translated "Creating new document...".
     *
     * @return translated "Creating new document..."
     */
    @DefaultMessage("Creating new document...")
    String creatingNewDocument();

    /**
     * Translated "Error Creating Shiny Application".
     *
     * @return translated "Error Creating Shiny Application"
     */
    @DefaultMessage("Error Creating Shiny Application")
    String errorCreatingShinyApplication();

    /**
     * Translated "Error Creating Plumber API".
     *
     * @return translated "Error Creating Plumber API"
     */
    @DefaultMessage("Error Creating Plumber API")
    String errorCreatingPlumberApi();

    /**
     * Translated "New Shiny Web Application".
     *
     * @return translated "New Shiny Web Application"
     */
    @DefaultMessage("New Shiny Web Application")
    String newShinyWebApplication();

    /**
     * Translated "New R Presentation".
     *
     * @return translated "New R Presentation"
     */
    @DefaultMessage("New R Presentation")
    String newRPresentation();

    /**
     * Translated "Creating Presentation...".
     *
     * @return translated "Creating Presentation..."
     */
    @DefaultMessage("Creating Presentation...")
    String creatingPresentation();

    /**
     * Translated "Document Tab Move Failed".
     *
     * @return translated "Document Tab Move Failed"
     */
    @DefaultMessage("Document Tab Move Failed")
    String documentTabMoveFailed();

    /**
     * Translated "Couldn''t move the tab to this window: \n{0}".
     *
     * @return translated "Couldn''t move the tab to this window: \n{0}"
     */
    @DefaultMessage("Couldn''t move the tab to this window: \n{0}")
    String couldntMoveTabToWindowError(String errorMessage);

    /**
     * Translated "Close All".
     *
     * @return translated "Close All"
     */
    @DefaultMessage("Close All")
    String closeAll();

    /**
     * Translated "Close Other".
     *
     * @return translated "Close Other"
     */
    @DefaultMessage("Close Other")
    String closeOther();

    /**
     * Translated "Open File".
     *
     * @return translated "Open File"
     */
    @DefaultMessage("Open File")
    String openFile();

    /**
     * Translated "New Plumber API".
     *
     * @return translated "New Plumber API"
     */
    @DefaultMessage("New Plumber API")
    String newPlumberApi();

    /**
     * Translated "Close All Others".
     *
     * @return translated "Close All Others"
     */
    @DefaultMessage("Close All Others")
    String closeAllOthers();

    /**
     * Translated "Error Creating New Document".
     *
     * @return translated "Error Creating New Document"
     */
    @DefaultMessage("Error Creating New Document")
    String errorCreatingNewDocument();

    /**
     * Translated "Publish Document...".
     *
     * @return translated "Publish Document..."
     */
    @DefaultMessage("Publish Document...")
    String publishDocument();

    /**
     * Translated "Publish Plumber API...".
     *
     * @return translated "Publish Plumber API..."
     */
    @DefaultMessage("Publish Plumber API...")
    String publishPlumberApi();

    /**
     * Translated "Publish Application...".
     *
     * @return translated "Publish Application..."
     */
    @DefaultMessage("Publish Application...")
    String publishApplication();

    // TODO IF THESE ARE SHORTCUTS NEED TO DOUBLE CHECK THESE
    /**
     * Translated "_Blame \"{0}\" on GitHub".
     *
     * @return translated "_Blame \"{0}\" on GitHub"
     */
    @DefaultMessage("_Blame \"{0}\" on GitHub")
    String blameOnGithub(String name);

    /**
     * Translated "_View \"{0}\" on GitHub".
     *
     * @return translated "_View \"{0}\" on GitHub"
     */
    @DefaultMessage("_View \"{0}\" on GitHub")
    String viewNameOnGithub(String name);

    /**
     * Translated "_Revert \"{0}\"...".
     *
     * @return translated "_Revert \"{0}\"..."
     */
    @DefaultMessage("_Revert \"{0}\"...")
    String revertName(String name);

    /**
     * Translated "_Log of \"{0}\"".
     *
     * @return translated "_Log of \"{0}\""
     */
    @DefaultMessage("_Log of \"{0}\"")
    String logOfName(String name);

    /**
     * Translated "_Diff \"{0}\"".
     *
     * @return translated "_Diff \"{0}\""
     */
    @DefaultMessage("_Diff \"{0}\"")
    String diffName(String name);

    /**
     * Translated "No document tabs open".
     *
     * @return translated "No document tabs open"
     */
    @DefaultMessage("No document tabs open")
    String noDocumentTabsOpen();

    /**
     * Translated "Show Object Explorer".
     *
     * @return translated "Show Object Explorer"
     */
    @DefaultMessage("Show Object Explorer")
    String showObjectExplorer();

    /**
     * Translated "Show Data Frame".
     *
     * @return translated "Show Data Frame"
     */
    @DefaultMessage("Show Data Frame")
    String showDataFrame();

    /**
     * Translated "Template Content Missing".
     *
     * @return translated "Template Content Missing"
     */
    @DefaultMessage("Template Content Missing")
    String templateContentMissing();

    /**
     * Translated "The template at {0} is missing.".
     *
     * @return translated "The template at {0} is missing."
     */
    @DefaultMessage("The template at {0} is missing.")
    String templateAtPathMissing(String templatePath);

    /**
     * Translated "Error while opening file".
     *
     * @return translated "Error while opening file"
     */
    @DefaultMessage("Error while opening file")
    String errorWhileOpeningFile();

    /**
     * Translated "This notebook has the same name as an R Markdown file, but doesn''t match it".
     *
     * @return translated "This notebook has the same name as an R Markdown file, but doesn''t match it"
     */
    @DefaultMessage("This notebook has the same name as an R Markdown file, but doesn''t match it")
    String openNotebookWarningMessage();

    /**
     * Translated "Notebook Open Failed".
     *
     * @return translated "Notebook Open Failed"
     */
    @DefaultMessage("Notebook Open Failed")
    String notebookOpenFailed();

    /**
     * Translated "This notebook could not be opened. If the error persists, try removing the accompanying R Markdown file. \n\n{0}".
     *
     * @return translated "This notebook could not be opened. If the error persists, try removing the accompanying R Markdown file. \n\n{0}"
     */
    @DefaultMessage("This notebook could not be opened. If the error persists, try removing the accompanying R Markdown file. \\n\\n{0}")
    String notebookOpenFailedMessage(String errorMessage);

    /**
     * Translated "This notebook could not be opened. \n\n{0}".
     *
     * @return translated "This notebook could not be opened. \n\n{0}"
     */
    @DefaultMessage("This notebook could not be opened. \n\n{0}")
    String notebookCouldNotBeOpenedMessage(String errorMessage);

    /**
     * Translated "Opening file...".
     *
     * @return translated "Opening file..."
     */
    @DefaultMessage("Opening file...")
    String openingFile();

    /**
     * Translated "The file ''{0}'' is too large to open in the source editor (the file is {1} and the maximum file size is {2})".
     *
     * @return translated "The file ''{0}'' is too large to open in the source editor (the file is {1} and the maximum file size is {2})"
     */
    @DefaultMessage("The file ''{0}'' is too large to open in the source editor (the file is {1} and the maximum file size is {2})")
    String showFileTooLargeWarningMsg(String filename, String filelength, String sizeLimit);

    /**
     * Translated "Selected File Too Large".
     *
     * @return translated "Selected File Too Large"
     */
    @DefaultMessage("Selected File Too Large")
    String selectedFileTooLarge();

    /**
     * Translated "The source file ''{0}'' is large ({1}) and may take some time to open. Are you sure you want to continue opening it?".
     *
     * @return translated "The source file ''{0}'' is large ({1}) and may take some time to open. Are you sure you want to continue opening it?"
     */
    @DefaultMessage("The source file ''{0}'' is large ({1}) and may take some time to open. Are you sure you want to continue opening it?")
    String confirmOpenLargeFileMsg(String filename, String length);

    /**
     * Translated "Confirm Open".
     *
     * @return translated "Confirm Open"
     */
    @DefaultMessage("Confirm Open")
    String confirmOpen();

    /**
     * Translated "Source Column {0}".
     *
     * @return translated "Source Column {0}"
     */
    @DefaultMessage("Source Column {0}")
    String sourceColumn(int columnCounter);

    /**
     * Translated "Source".
     *
     * @return translated "Source"
     */
    @DefaultMessage("Source")
    String source();

    /**
     * Translated "Switch to tab".
     *
     * @return translated "Switch to tab"
     */
    @DefaultMessage("Switch to tab")
    String switchToTab();

    /**
     * Translated "RStudio Source Editor".
     *
     * @return translated "RStudio Source Editor"
     */
    @DefaultMessage("RStudio Source Editor")
    String rstudioSourceEditor();

    /**
     * Translated "Close Source Window".
     *
     * @return translated "Close Source Window"
     */
    @DefaultMessage("Close Source Window")
    String closeSourceWindow();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    String cancel();

    /**
     * Translated "Close and Discard Changes".
     *
     * @return translated "Close and Discard Changes"
     */
    @DefaultMessage("Close and Discard Changes")
    String closeAndDiscardChanges();

    /**
     * Translated "Unsaved Changes".
     *
     * @return translated "Unsaved Changes"
     */
    @DefaultMessage("Unsaved Changes")
    String unsavedChanges();

    /**
     * Translated "There are unsaved documents in this window. Are you sure you want to close it?".
     *
     * @return translated "There are unsaved documents in this window. Are you sure you want to close it?"
     */
    @DefaultMessage("There are unsaved documents in this window. Are you sure you want to close it?")
    String confirmCloseUnsavedDocuments();

    /**
     * Translated "Your edits to the file {0} have not been saved".
     *
     * @return translated "Your edits to the file {0} have not been saved"
     */
    @DefaultMessage("Your edits to the file {0} have not been saved")
    String yourEditsToFileHasNotBeenSaved(String desc);

    /**
     * Translated "Your edits to the files {0} have not been saved".
     *
     * @return translated "Your edits to the files {0} have not been saved"
     */
    @DefaultMessage("Your edits to the files {0} have not been saved")
    String yourEditsToFilePluralHasNotBeenSaved(String completeMessage);

    /**
     * Translated ", ".
     *
     * @return translated ", "
     */
    @DefaultMessage(", ")
    String commaListSeparator();

    /**
     * Translated " and ".
     *
     * @return translated " and "
     */
    @DefaultMessage(" and ")
    String andForList();

    /**
     * Translated "Can''t Move Doc".
     *
     * @return translated "Can''t Move Doc"
     */
    @DefaultMessage("Can''t Move Doc")
    String cantMoveDoc();

    /**
     * Translated "The document could not be moved to a different window: \n{0}".
     *
     * @return translated "The document could not be moved to a different window: \n{0}"
     */
    @DefaultMessage("The document could not be moved to a different window: \n{0}")
    String cantMoveDocMessage(String errorMessage);

    /**
     * Translated "Search tabs".
     *
     * @return translated "Search tabs"
     */
    @DefaultMessage("Search tabs")
    String searchTabs();

    /**
     * Translated "Code Editor Tab".
     *
     * @return translated "Code Editor Tab"
     */
    @DefaultMessage("Code Editor Tab")
    String codeEditorTab();

    /**
     * Translated "Function:".
     *
     * @return translated "Function:"
     */
    @DefaultMessage("Function:")
    String functionColon();

    /**
     * Translated "Method:".
     *
     * @return translated "Method:"
     */
    @DefaultMessage("Method:")
    String methodColon();

    /**
     * Translated "Error Reading Function Definition".
     *
     * @return translated "Error Reading Function Definition"
     */
    @DefaultMessage("Error Reading Function Definition")
    String errorReadingFunctionDefinition();

    /**
     * Translated "Code Browser displayed".
     *
     * @return translated "Code Browser displayed"
     */
    @DefaultMessage("Code Browser displayed")
    String codeBrowserDisplayed();

    /**
     * Translated "Debug location is approximate because the source is not available.".
     *
     * @return translated "Debug location is approximate because the source is not available."
     */
    @DefaultMessage("Debug location is approximate because the source is not available.")
    String debugLocationIsApproximate();

    /**
     * Translated "R Source Viewer".
     *
     * @return translated "R Source Viewer"
     */
    @DefaultMessage("R Source Viewer")
    String rSourceViewer();

    /**
     * Translated "(Read-only)".
     *
     * @return translated "(Read-only)"
     */
    @DefaultMessage("(Read-only)")
    String readOnlyParentheses();

    /**
     * Translated "Code Browser Second".
     *
     * @return translated "Code Browser Second"
     */
    @DefaultMessage("Code Browser Second")
    String codeBrowserSecond();

    /**
     * Translated "Code Tools".
     *
     * @return translated "Code Tools"
     */
    @DefaultMessage("Code Tools")
    String codeTools();

    /**
     * Translated "Error Searching for Function".
     *
     * @return translated "Error Searching for Function"
     */
    @DefaultMessage("Error Searching for Function")
    String errorSearchingForFunction();

    /**
     * Translated "Searching for function definition...".
     *
     * @return translated "Searching for function definition..."
     */
    @DefaultMessage("Searching for function definition...")
    String searchingForFunctionDefinition();

    /**
     * Translated "{0} Source Viewer".
     *
     * @return translated "{0} Source Viewer"
     */
    @DefaultMessage("{0} Source Viewer")
    String nameSourceViewer(String name);

    /**
     * Translated "Untitled Source Viewer".
     *
     * @return translated "Untitled Source Viewer"
     */
    @DefaultMessage("Untitled Source Viewer")
    String untitledSourceViewer();

    /**
     * Translated "Help".
     *
     * @return translated "Help"
     */
    @DefaultMessage("Help")
    String help();

    /**
     * Translated "Data Browser displayed".
     *
     * @return translated "Data Browser displayed"
     */
    @DefaultMessage("Data Browser displayed")
    String dataBrowserDisplayed();

    /**
     * Translated "Data Browser".
     *
     * @return translated "Data Browser"
     */
    @DefaultMessage("Data Browser")
    String dataBrowser();

    /**
     * Translated "{0} Data Browser".
     *
     * @return translated "{0} Data Browser"
     */
    @DefaultMessage("{0} Data Browser")
    String accessibleNameDataBrowser(String accessibleName);

    /**
     * Translated "Untitled Data Browser".
     *
     * @return translated "Untitled Data Browser"
     */
    @DefaultMessage("Untitled Data Browser")
    String untitledDataBrowser();

    /**
     * Translated "Displayed {0} rows of {1}".
     *
     * @return translated "Displayed {0} rows of {1}"
     */
    @DefaultMessage("Displayed {0} rows of {1}")
    String dataEditingTargetWidgetLabel1(String displayedObservations, String totalObservations);

    /**
     * Translated "({0} omitted)".
     *
     * @return translated "({0} omitted)"
     */
    @DefaultMessage("({0} omitted)")
    String dataEditingTargetWidgetLabel2(String omittedNumber);

    /**
     * Translated "{0} Object Explorer".
     *
     * @return translated "{0} Object Explorer"
     */
    @DefaultMessage("{0} Object Explorer")
    String accessibleNameObjectExplorer(String accessibleName);

    /**
     * Translated "Untitled Object Explorer".
     *
     * @return translated "Untitled Object Explorer"
     */
    @DefaultMessage("Untitled Object Explorer")
    String untitledObjectExplorer();

    /**
     * Translated "Object Explorer displayed".
     *
     * @return translated "Object Explorer displayed"
     */
    @DefaultMessage("Object Explorer displayed")
    String objectExplorerDisplayed();

    /**
     * Translated "URL Viewer displayed".
     *
     * @return translated "URL Viewer displayed"
     */
    @DefaultMessage("URL Viewer displayed")
    String urlViewerDisplayed();

    /**
     * Translated "URL Browser".
     *
     * @return translated "URL Browser"
     */
    @DefaultMessage("URL Browser")
    String urlBrowser();

    /**
     * Translated "{0} URL Browser".
     *
     * @return translated "{0} URL Browser"
     */
    @DefaultMessage("{0} URL Browser")
    String accessibleNameBrowser(String name);

    /**
     * Translated "Untitled URL Browser".
     *
     * @return translated "Untitled URL Browser"
     */
    @DefaultMessage("Untitled URL Browser")
    String untitledUrlBrowser();

    /**
     * Translated "Source Viewer".
     *
     * @return translated "Source Viewer"
     */
    @DefaultMessage("Source Viewer")
    String sourceViewer();

    /**
     * Translated "Name".
     *
     * @return translated "Name"
     */
    @DefaultMessage("Name")
    String name();

    /**
     * Translated "Type".
     *
     * @return translated "Type"
     */
    @DefaultMessage("Type")
    String type();

    /**
     * Translated "Value".
     *
     * @return translated "Value"
     */
    @DefaultMessage("Value")
    String value();

    /**
     * Translated "View({0})".
     *
     * @return translated "View({0})"
     */
    @DefaultMessage("View({0})")
    String viewCode(String code);

    /**
     * Translated "(No selection)".
     *
     * @return translated "(No selection)"
     */
    @DefaultMessage("(No selection)")
    String noSelectionParentheses();

    /**
     * Translated "Show Attributes".
     *
     * @return translated "Show Attributes"
     */
    @DefaultMessage("Show Attributes")
    String showAttributes();

    /**
     * Translated "Search objects".
     *
     * @return translated "Search objects"
     */
    @DefaultMessage("Search objects")
    String searchObjects();

    /**
     * Translated "Refresh".
     *
     * @return translated "Refresh"
     */
    @DefaultMessage("Refresh")
    String refresh();

    /**
     * Translated "The source file {0} does not exist.".
     *
     * @return translated "The source file {0} does not exist."
     */
    @DefaultMessage("The source file {0} does not exist.")
    String sourceFileAtPathDoesNotExist(String selectedPath);

    /**
     * Translated "Error while opening profiler source".
     *
     * @return translated "Error while opening profiler source"
     */
    @DefaultMessage("Error while opening profiler source")
    String errorOpeningProfilerSource();

    /**
     * Translated "Failed to Save Profile".
     *
     * @return translated "Failed to Save Profile"
     */
    @DefaultMessage("Failed to Save Profile")
    String failedToSaveProfile();

    /**
     * Translated "Save File - {0}".
     *
     * @return translated "Save File - {0}"
     */
    @DefaultMessage("Save File - {0}")
    String saveFileName(String name);

    /**
     * Translated "Failed to Save Profile Properties".
     *
     * @return translated "Failed to Save Profile Properties"
     */
    @DefaultMessage("Failed to Save Profile Properties")
    String failedToSaveProfileProperties();

    /**
     * Translated "Code Profile results displayed".
     *
     * @return translated "Code Profile results displayed"
     */
    @DefaultMessage("Code Profile results displayed")
    String codeProfileResultsDisplayed();

    /**
     * Translated "Profile".
     *
     * @return translated "Profile"
     */
    @DefaultMessage("Profile")
    String profileCapitalized();

    /**
     * Translated "Profiler".
     *
     * @return translated "Profiler"
     */
    @DefaultMessage("Profiler")
    String profilerCapitalized();

    /**
     * Translated "R Profiler".
     *
     * @return translated "R Profiler"
     */
    @DefaultMessage("R Profiler")
    String rProfiler();

    /**
     * Translated "{0} Profile View".
     *
     * @return translated "{0} Profile View"
     */
    @DefaultMessage("{0} Profile View")
    String titleProfileView(String title);

    /**
     * Translated "Failed to Open Profile".
     *
     * @return translated "Failed to Open Profile"
     */
    @DefaultMessage("Failed to Open Profile")
    String failedToOpenProfile();

    /**
     * Translated "Profiler Error".
     *
     * @return translated "Profiler Error"
     */
    @DefaultMessage("Profiler Error")
    String profilerError();

    /**
     * Translated "Failed to Stop Profiler".
     *
     * @return translated "Failed to Stop Profiler"
     */
    @DefaultMessage("Failed to Stop Profiler")
    String failedToStopProfiler();

    /**
     * Translated "Error navigating to file".
     *
     * @return translated "Error navigating to file"
     */
    @DefaultMessage("Error navigating to file")
    String errorNavigatingToFile();

    /**
     * Translated "No file at path ''{0}''.".
     *
     * @return translated "No file at path ''{0}''."
     */
    @DefaultMessage("No file at path ''{0}''.")
    String noFileAtPath(String finalUrl);

    /**
     * Translated "Open Link (Command+Click)".
     *
     * @return translated "Open Link (Command+Click)"
     */
    @DefaultMessage("Open Link (Command+Click)")
    String openLinkMacCommand();

    /**
     * Translated "Open Link (Ctrl+Click)".
     *
     * @return translated "Open Link (Ctrl+Click)"
     */
    @DefaultMessage("Open Link (Ctrl+Click)")
    String openLinkNotMacCommand();

    /**
     * Translated "Finding definition...".
     *
     * @return translated "Finding definition..."
     */
    @DefaultMessage("Finding definition...")
    String findingDefinition();

    /**
     * Translated "ignored".
     *
     * @return translated "ignored"
     */
    @DefaultMessage("ignored")
    String ignored();

    /**
     * Translated "note".
     *
     * @return translated "note"
     */
    @DefaultMessage("note")
    String note();

    /**
     * Translated "warning".
     *
     * @return translated "warning"
     */
    @DefaultMessage("warning")
    String warningLowercase();

    /**
     * Translated "error".
     *
     * @return translated "error"
     */
    @DefaultMessage("error")
    String error();

    /**
     * Translated "fatal".
     *
     * @return translated "fatal"
     */
    @DefaultMessage("fatal")
    String fatal();

    /**
     * Translated "(No matches)".
     *
     * @return translated "(No matches)"
     */
    @DefaultMessage("(No matches)")
    String noMatchesParentheses();

    /**
     * Translated "{0} of {1}".
     *
     * @return translated "{0} of {1}"
     */
    @DefaultMessage("{0} of {1}")
    String pagingLabelTextOf(int index, int textLength);

    /**
     * Translated "{0} occurrences replaced.".
     *
     * @return translated "{0} occurrences replaced."
     */
    @DefaultMessage("{0} occurrences replaced.")
    String numberOfOccurrencesReplaced(int occurrences);

    /**
     * Translated "Invalid search term.".
     *
     * @return translated "Invalid search term."
     */
    @DefaultMessage("Invalid search term.")
    String invalidSearchTerm();

    /**
     * Translated "No more occurrences.".
     *
     * @return translated "No more occurrences."
     */
    @DefaultMessage("No more occurrences.")
    String noMoreOccurrences();

    /**
     * Translated "Find".
     *
     * @return translated "Find"
     */
    @DefaultMessage("Find")
    String findCapitalized();

    /**
     * Translated "Find/Replace".
     *
     * @return translated "Find/Replace"
     */
    @DefaultMessage("Find/Replace")
    String findOrReplace();

    /**
     * Translated "Replace".
     *
     * @return translated "Replace"
     */
    @DefaultMessage("Replace")
    String replaceCapitalized();

    /**
     * Translated "All".
     *
     * @return translated "All"
     */
    @DefaultMessage("All")
    String allCapitalized();

    /**
     * Translated "Replace all occurrences".
     *
     * @return translated "Replace all occurrences"
     */
    @DefaultMessage("Replace all occurrences")
    String replaceAllOccurrences();

    /**
     * Translated "In selection".
     *
     * @return translated "In selection"
     */
    @DefaultMessage("In selection")
    String inSelection();

    /**
     * Translated "Match case".
     *
     * @return translated "Match case"
     */
    @DefaultMessage("Match case")
    String matchCase();

    /**
     * Translated "Whole word".
     *
     * @return translated "Whole word"
     */
    @DefaultMessage("Whole word")
    String wholeWord();

    /**
     * Translated "Regex".
     *
     * @return translated "Regex"
     */
    @DefaultMessage("Regex")
    String regexCapitalized();

    /**
     * Translated "Wrap".
     *
     * @return translated "Wrap"
     */
    @DefaultMessage("Wrap")
    String wrapCapitalized();

    /**
     * Translated "Close find and replace".
     *
     * @return translated "Close find and replace"
     */
    @DefaultMessage("Close find and replace")
    String closeFindAndReplace();

    /**
     * Translated "Chunk Pending Execution".
     *
     * @return translated "Chunk Pending Execution"
     */
    @DefaultMessage("Chunk Pending Execution")
    String chunkPendingExecution();

    /**
     * Translated "The code in this chunk is scheduled to run later, when other chunks have finished executing.".
     *
     * @return translated "The code in this chunk is scheduled to run later, when other chunks have finished executing."
     */
    @DefaultMessage("The code in this chunk is scheduled to run later, when other chunks have finished executing.")
    String chunkPendingExecutionMessage();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    String okFullyCapitalized();

    /**
     * Translated "Don''t Run".
     *
     * @return translated "Don''t Run"
     */
    @DefaultMessage("Don''t Run")
    String dontRun();

    /**
     * Translated "The rmarkdown package is not installed; notebook HTML file will not be generated.".
     *
     * @return translated "The rmarkdown package is not installed; notebook HTML file will not be generated."
     */
    @DefaultMessage("The rmarkdown package is not installed; notebook HTML file will not be generated.")
    String rMarkdownNotInstalledHTMLNoGenerate();

    /**
     * Translated "An updated version of the rmarkdown package is required to generate notebook HTML files.".
     *
     * @return translated "An updated version of the rmarkdown package is required to generate notebook HTML files."
     */
    @DefaultMessage("An updated version of the rmarkdown package is required to generate notebook HTML files.")
    String rMarkdownUpgradeRequired();

    /**
     * Translated "Error creating notebook: ".
     *
     * @return translated "Error creating notebook: "
     */
    @DefaultMessage("Error creating notebook: ")
    String errorCreatingNotebookPrefix();

    /**
     * Translated "Creating R Notebooks".
     *
     * @return translated "Creating R Notebooks"
     */
    @DefaultMessage("Creating R Notebooks")
    String creatingRNotebooks();

    /**
     * Translated "Can''t execute {0}".
     *
     * @return translated "Can''t execute {0}"
     */
    @DefaultMessage("Can''t execute {0}")
    String cantExecuteJobDesc(String jobDesc);

    /**
     * Translated "Executing Python chunks".
     *
     * @return translated "Executing Python chunks"
     */
    @DefaultMessage("Executing Python chunks")
    String executingPythonChunks();

    /**
     * Translated "Executing chunks".
     *
     * @return translated "Executing chunks"
     */
    @DefaultMessage("Executing chunks")
    String executingChunks();

    /**
     * Translated "Run Chunk".
     *
     * @return translated "Run Chunk"
     */
    @DefaultMessage("Run Chunk")
    String runChunk();

    /**
     * Translated "{0}: Chunks Currently Executing".
     *
     * @return translated "{0}: Chunks Currently Executing"
     */
    @DefaultMessage("{0}: Chunks Currently Executing")
    String jobChunkCurrentlyExecuting(String jobDesc);

    /**
     * Translated "RStudio cannot execute ''{0}'' because this notebook is already executing code. Interrupt R, or wait for execution to complete.".
     *
     * @return translated "RStudio cannot execute ''{0}'' because this notebook is already executing code. Interrupt R, or wait for execution to complete."
     */
    @DefaultMessage("RStudio cannot execute ''{0}'' because this notebook is already executing code. Interrupt R, or wait for execution to complete.")
    String rStudioCannotExecuteJob(String jobDesc);

    /**
     * Translated "Run Chunks".
     *
     * @return translated "Run Chunks"
     */
    @DefaultMessage("Run Chunks")
    String runChunks();

    /**
     * Translated "Chunks Currently Running".
     *
     * @return translated "Chunks Currently Running"
     */
    @DefaultMessage("Chunks Currently Running")
    String chunksCurrentlyRunning();

    /**
     * Translated "Output can''t be cleared because there are still chunks running. Do you want to interrupt them?".
     *
     * @return translated "Output can''t be cleared because there are still chunks running. Do you want to interrupt them?"
     */
    @DefaultMessage("Output can''t be cleared because there are still chunks running. Do you want to interrupt them?")
    String outputCantBeClearedBecauseChunks();

    /**
     * Translated "Interrupt and Clear Output".
     *
     * @return translated "Interrupt and Clear Output"
     */
    @DefaultMessage("Interrupt and Clear Output")
    String interruptAndClearOutput();

    /**
     * Translated "Remove Inline Chunk Output".
     *
     * @return translated "Remove Inline Chunk Output"
     */
    @DefaultMessage("Remove Inline Chunk Output")
    String removeInlineChunkOutput();

    /**
     * Translated "Do you want to clear all the existing chunk output from your notebook?".
     *
     * @return translated "Do you want to clear all the existing chunk output from your notebook?"
     */
    @DefaultMessage("Do you want to clear all the existing chunk output from your notebook?")
    String clearExistingChunkOutputMessage();

    /**
     * Translated "Remove Output".
     *
     * @return translated "Remove Output"
     */
    @DefaultMessage("Remove Output")
    String removeOutput();

    /**
     * Translated "Keep Output".
     *
     * @return translated "Keep Output"
     */
    @DefaultMessage("Keep Output")
    String keepOutput();

    /**
     * Translated "Unnamed chunk".
     *
     * @return translated "Unnamed chunk"
     */
    @DefaultMessage("Unnamed chunk")
    String unnamedChunk();

    /**
     * Translated "Chunk Name:".
     *
     * @return translated "Chunk Name:"
     */
    @DefaultMessage("Chunk Name:")
    String chunkNameColon();

    /**
     * Translated "Output:".
     *
     * @return translated "Output:"
     */
    @DefaultMessage("Output:")
    String outputColon();

    /**
     * Translated "Show warnings".
     *
     * @return translated "Show warnings"
     */
    @DefaultMessage("Show warnings")
    String showWarnings();

    /**
     * Translated "Show messages".
     *
     * @return translated "Show messages"
     */
    @DefaultMessage("Show messages")
    String showMessages();

    /**
     * Translated "Cache chunk".
     *
     * @return translated "Cache chunk"
     */
    @DefaultMessage("Cache chunk")
    String cacheChunk();

    /**
     * Translated "Use paged tables".
     *
     * @return translated "Use paged tables"
     */
    @DefaultMessage("Use paged tables")
    String usePagedTables();

    /**
     * Translated "Use custom figure size".
     *
     * @return translated "Use custom figure size"
     */
    @DefaultMessage("Use custom figure size")
    String useCustomFigureSize();

    /**
     * Translated "Width (inches):".
     *
     * @return translated "Width (inches):"
     */
    @DefaultMessage("Width (inches):")
    String widthInchesColon();

    /**
     * Translated "Height (inches):".
     *
     * @return translated "Height (inches):"
     */
    @DefaultMessage("Height (inches):")
    String heightInchesColon();

    /**
     * Translated "Engine path:".
     *
     * @return translated "Engine path:"
     */
    @DefaultMessage("Engine path:")
    String enginePathColon();

    /**
     * Translated "Select Engine".
     *
     * @return translated "Select Engine"
     */
    @DefaultMessage("Select Engine")
    String selectEngine();

    /**
     * Translated "Engine options:".
     *
     * @return translated "Engine options:"
     */
    @DefaultMessage("Engine options:")
    String engineOptionsColon();

    /**
     * Translated "Chunk options".
     *
     * @return translated "Chunk options"
     */
    @DefaultMessage("Chunk options")
    String chunkOptions();

    /**
     * Translated "Revert".
     *
     * @return translated "Revert"
     */
    @DefaultMessage("Revert")
    String revertCapitalized();

    /**
     * Translated "Apply".
     *
     * @return translated "Apply"
     */
    @DefaultMessage("Apply")
    String applyCapitalized();

    /**
     * Translated "Show nothing (don''t run code)".
     *
     * @return translated "Show nothing (don''t run code)"
     */
    @DefaultMessage("Show nothing (don''t run code)")
    String showNothingDontRunCode();

    /**
     * Translated "Show nothing (run code)".
     *
     * @return translated "Show nothing (run code)"
     */
    @DefaultMessage("Show nothing (run code)")
    String showNothingRunCode();

    /**
     * Translated "Show code and output".
     *
     * @return translated "Show code and output"
     */
    @DefaultMessage("Show code and output")
    String showCodeAndOutput();

    /**
     * Translated "Show output only".
     *
     * @return translated "Show output only"
     */
    @DefaultMessage("Show output only")
    String showOutputOnly();

    /**
     * Translated "(Use document default)".
     *
     * @return translated "(Use document default)"
     */
    @DefaultMessage("(Use document default)")
    String useDocumentDefaultParentheses();

    /**
     * Translated "Default Chunk Options".
     *
     * @return translated "Default Chunk Options"
     */
    @DefaultMessage("Default Chunk Options")
    String defaultChunkOptions();

    /**
     * Translated "Check Spelling".
     *
     * @return translated "Check Spelling"
     */
    @DefaultMessage("Check Spelling")
    String checkSpelling();

    /**
     * Translated "An error has occurred:\n\n{0}".
     *
     * @return translated "An error has occurred:\n\n{0}"
     */
    @DefaultMessage("An error has occurred:\n\n{0}")
    String anErrorHasOccurredMessage(String eMessage);

    /**
     * Translated "Spell check is complete.".
     *
     * @return translated "Spell check is complete."
     */
    @DefaultMessage("Spell check is complete.")
    String spellCheckIsComplete();

    /**
     * Translated "Spell check in progress...".
     *
     * @return translated "Spell check in progress..."
     */
    @DefaultMessage("Spell check in progress...")
    String spellCheckInProgress();

    /**
     * Translated "Add".
     *
     * @return translated "Add"
     */
    @DefaultMessage("Add")
    String addCapitalized();

    /**
     * Translated "Add word to user dictionary".
     *
     * @return translated "Add word to user dictionary"
     */
    @DefaultMessage("Add word to user dictionary")
    String addWordToUserDictionary();

    /**
     * Translated "Skip".
     *
     * @return translated "Skip"
     */
    @DefaultMessage("Skip")
    String skip();

    /**
     * Translated "Ignore All".
     *
     * @return translated "Ignore All"
     */
    @DefaultMessage("Ignore All")
    String ignoreAll();

    /**
     * Translated "Change".
     *
     * @return translated "Change"
     */
    @DefaultMessage("Change")
    String changeCapitalized();

    /**
     * Translated "Change All".
     *
     * @return translated "Change All"
     */
    @DefaultMessage("Change All")
    String changeAll();

    /**
     * Translated "Suggestions".
     *
     * @return translated "Suggestions"
     */
    @DefaultMessage("Suggestions")
    String suggestionsCapitalized();

    /**
     * Translated "Checking...".
     *
     * @return translated "Checking..."
     */
    @DefaultMessage("Checking...")
    String checkingEllipses();

    /**
     * Translated "Class".
     *
     * @return translated "Class"
     */
    @DefaultMessage("Class")
    String classCapitalized();

    /**
     * Translated "Namespace".
     *
     * @return translated "Namespace"
     */
    @DefaultMessage("Namespace")
    String namespaceCapitalized();

    /**
     * Translated "Lambda".
     *
     * @return translated "Lambda"
     */
    @DefaultMessage("Lambda")
    String lambdaCapitalized();

    /**
     * Translated "Anonymous".
     *
     * @return translated "Anonymous"
     */
    @DefaultMessage("Anonymous")
    String anonymousCapitalized();

    /**
     * Translated "Function".
     *
     * @return translated "Function"
     */
    @DefaultMessage("Function")
    String functionCapitalized();

    /**
     * Translated "Test".
     *
     * @return translated "Test"
     */
    @DefaultMessage("Test")
    String testCapitalized();

    /**
     * Translated "Chunk".
     *
     * @return translated "Chunk"
     */
    @DefaultMessage("Chunk")
    String chunkCapitalized();

    /**
     * Translated "Section".
     *
     * @return translated "Section"
     */
    @DefaultMessage("Section")
    String sectionCapitalized();

    /**
     * Translated "Slide".
     *
     * @return translated "Slide"
     */
    @DefaultMessage("Slide")
    String slideCapitalized();

    /**
     * Translated "Tomorrow Night".
     *
     * @return translated "Tomorrow Night"
     */
    @DefaultMessage("Tomorrow Night")
    String tomorrowNight();

    /**
     * Translated "Textmate (default)".
     *
     * @return translated "Textmate (default)"
     */
    @DefaultMessage("Textmate (default)")
    String textmateDefaultParentheses();

    /**
     * Translated "The specified theme does not exist".
     *
     * @return translated "The specified theme does not exist"
     */
    @DefaultMessage("The specified theme does not exist")
    String specifiedThemeDoesNotExist();

    /**
     * Translated "The specified theme is a default RStudio theme and cannot be removed.".
     *
     * @return translated "The specified theme is a default RStudio theme and cannot be removed."
     */
    @DefaultMessage("The specified theme is a default RStudio theme and cannot be removed.")
    String specifiedDefaultThemeCannotBeRemoved();

    /**
     * Translated "Choose Encoding".
     *
     * @return translated "Choose Encoding"
     */
    @DefaultMessage("Choose Encoding")
    String chooseEncoding();

    /**
     * Translated "Encodings".
     *
     * @return translated "Encodings"
     */
    @DefaultMessage("Encodings")
    String encodingsCapitalized();

    /**
     * Translated "Show all encodings".
     *
     * @return translated "Show all encodings"
     */
    @DefaultMessage("Show all encodings")
    String showAllEncodings();

    /**
     * Translated "Set as default encoding for source files".
     *
     * @return translated "Set as default encoding for source files"
     */
    @DefaultMessage("Set as default encoding for source files")
    String setAsDefaultEncodingSourceFiles();

    /**
     * Translated "{0} (System default)".
     *
     * @return translated "{0} (System default)"
     */
    @DefaultMessage("{0} (System default)")
    String sysEncNameDefault(String sysEncName);

    /**
     * Translated "[Ask]".
     *
     * @return translated "[Ask]"
     */
    @DefaultMessage("[Ask]")
    String askSquareBrackets();

    /**
     * Translated "New R Documentation File".
     *
     * @return translated "New R Documentation File"
     */
    @DefaultMessage("New R Documentation File")
    String newRDocumentationFile();

    /**
     * Translated "Name Not Specified".
     *
     * @return translated "Name Not Specified"
     */
    @DefaultMessage("Name Not Specified")
    String nameNotSpecified();

    /**
     * Translated "You must specify a topic name for the new Rd file.".
     *
     * @return translated "You must specify a topic name for the new Rd file."
     */
    @DefaultMessage("You must specify a topic name for the new Rd file.")
    String mustSpecifyTopicNameForRdFile();

    /**
     * Translated "New R Markdown".
     *
     * @return translated "New R Markdown"
     */
    @DefaultMessage("New R Markdown")
    String newRMarkdown();

    /**
     * Translated "Templates".
     *
     * @return translated "Templates"
     */
    @DefaultMessage("Templates")
    String templatesCapitalized();

    /**
     * Translated "Create Empty Document".
     *
     * @return translated "Create Empty Document"
     */
    @DefaultMessage("Create Empty Document")
    String createEmptyDocument();

    /**
     * Translated "Using Shiny with R Markdown".
     *
     * @return translated "Using Shiny with R Markdown"
     */
    @DefaultMessage("Using Shiny with R Markdown")
    String usingShinyWithRMarkdown();

    /**
     * Translated "Create an HTML document with interactive Shiny components.".
     *
     * @return translated "Create an HTML document with interactive Shiny components."
     */
    @DefaultMessage("Create an HTML document with interactive Shiny components.")
    String shinyDocNameDescription();

    /**
     * Translated "Create an IOSlides presentation with interactive Shiny components.".
     *
     * @return translated "Create an IOSlides presentation with interactive Shiny components."
     */
    @DefaultMessage("Create an IOSlides presentation with interactive Shiny components.")
    String shinyPresentationNameDescription();

    /**
     * Translated "From Template".
     *
     * @return translated "From Template"
     */
    @DefaultMessage("From Template")
    String fromTemplate();

    /**
     * Translated "Shiny".
     *
     * @return translated "Shiny"
     */
    @DefaultMessage("Shiny")
    String shinyCapitalized();

    /**
     * Translated "Shiny Document".
     *
     * @return translated "Shiny Document"
     */
    @DefaultMessage("Shiny Document")
    String shinyDocument();

    /**
     * Translated "Shiny Presentation".
     *
     * @return translated "Shiny Presentation"
     */
    @DefaultMessage("Shiny Presentation")
    String shinyPresentation();

    /**
     * Translated "No Parameters Defined".
     *
     * @return translated "No Parameters Defined"
     */
    @DefaultMessage("No Parameters Defined")
    String noParametersDefined();

    /**
     * Translated "There are no parameters defined for the current R Markdown document.".
     *
     * @return translated "There are no parameters defined for the current R Markdown document."
     */
    @DefaultMessage("There are no parameters defined for the current R Markdown document.")
    String noParametersDefinedForCurrentRMarkdown();

    /**
     * Translated "Using R Markdown Parameters".
     *
     * @return translated "Using R Markdown Parameters"
     */
    @DefaultMessage("Using R Markdown Parameters")
    String usingRMarkdownParameters();

    /**
     * Translated "Unable to activate visual mode (unsupported front matter format or non top-level YAML block)".
     *
     * @return translated "Unable to activate visual mode (unsupported front matter format or non top-level YAML block)"
     */
    @DefaultMessage("Unable to activate visual mode (unsupported front matter format or non top-level YAML block)")
    String unableToActivateVisualModeYAML();

    /**
     * Translated "Unable to activate visual mode (error parsing code chunks out of document)".
     *
     * @return translated "Unable to activate visual mode (error parsing code chunks out of document)"
     */
    @DefaultMessage("Unable to activate visual mode (error parsing code chunks out of document)")
    String unableToActivateVisualModeParsingCode();

    /**
     * Translated "Unable to activate visual mode (document contains example lists which are not currently supported)".
     *
     * @return translated "Unable to activate visual mode (document contains example lists which are not currently supported)"
     */
    @DefaultMessage("Unable to activate visual mode (document contains example lists which are not currently supported)")
    String unableToActivateVisualModeDocumentContains();

    /**
     * Translated "Unrecognized Pandoc token(s); {0}".
     *
     * @return translated "Unrecognized Pandoc token(s); {0}"
     */
    @DefaultMessage("Unrecognized Pandoc token(s); {0}")
    String unrecognizedPandocTokens(String tokens);

    /**
     * Translated "Invalid Pandoc format: {0}".
     *
     * @return translated "Invalid Pandoc format: {0}"
     */
    @DefaultMessage("Invalid Pandoc format: {0}")
    String invalidPandocFormat(String format);

    /**
     * Translated "Unsupported extensions for markdown mode: {0}".
     *
     * @return translated "Unsupported extensions for markdown mode: {0}"
     */
    @DefaultMessage("Unsupported extensions for markdown mode: {0}")
    String unsupportedExtensionsForMarkdown(String format);

    /**
     * Translated "Unable to parse markdown (please report at https://github.com/rstudio/rstudio/issues/new)".
     *
     * @return translated "Unable to parse markdown (please report at https://github.com/rstudio/rstudio/issues/new)"
     */
    @DefaultMessage("Unable to parse markdown (please report at https://github.com/rstudio/rstudio/issues/new)")
    String unableToParseMarkdownPleaseReport();

    /**
     * Translated "Chunk {0}".
     *
     * @return translated "Chunk {0}"
     */
    @DefaultMessage("Chunk {0}")
    String chunkSequence(int itemSequence);

    /**
     * Translated "(Top Level)".
     *
     * @return translated "(Top Level)"
     */
    @DefaultMessage("(Top Level)")
    String topLevelParentheses();

    /**
     * Translated "You cannot enter visual mode while using realtime collaboration.".
     *
     * @return translated "You cannot enter visual mode while using realtime collaboration."
     */
    @DefaultMessage("You cannot enter visual mode while using realtime collaboration.")
    String cantEnterVisualModeUsingRealtime();

    /**
     * Translated "{0}, {1} line".
     *
     * @return translated "{0}, {1} line"
     */
    @DefaultMessage("{0}, {1} line")
    String visualModeChunkSummary(String enging, int lines);

    /**
     * Translated "{0}, {1} lines".
     *
     * @return translated "{0}, {1} lines"
     */
    @DefaultMessage("{0}, {1} lines")
    String visualModeChunkSummaryPlural(String engine, int lines);

    /**
     * Translated "images".
     *
     * @return translated "images"
     */
    @DefaultMessage("images")
    String images();

    /**
     * Translated "Xaringan presentations cannot be edited in visual mode.".
     * Do not localize Xaringan
     * @return translated "Xaringan presentations cannot be edited in visual mode."
     */
    @DefaultMessage("Xaringan presentations cannot be edited in visual mode.")
    String xaringanPresentationsVisualMode();

    /**
     * Translated "Version control conflict markers detected. Please resolve them before editing in visual mode.".
     *
     * @return translated "Version control conflict markers detected. Please resolve them before editing in visual mode."
     */
    @DefaultMessage("Version control conflict markers detected. Please resolve them before editing in visual mode.")
    String versionControlConflict();

    /**
     * Translated "Switch to Visual Mode".
     *
     * @return translated "Switch to Visual Mode"
     */
    @DefaultMessage("Switch to Visual Mode")
    String switchToVisualMode();

    /**
     * Translated "Use Visual Mode".
     *
     * @return translated "Use Visual Mode"
     */
    @DefaultMessage("Use Visual Mode")
    String useVisualMode();

    /**
     * Translated "Don''t show this message again".
     *
     * @return translated "Don''t show this message again"
     */
    @DefaultMessage("Don''t show this message again")
    String dontShowMessageAgain();

    /**
     * Translated "Line Wrapping".
     *
     * @return translated "Line Wrapping"
     */
    @DefaultMessage("Line Wrapping")
    String lineWrapping();

    /**
     * Translated "project default".
     *
     * @return translated "project default"
     */
    @DefaultMessage("project default")
    String projectDefault();

    /**
     * Translated "global default".
     *
     * @return translated "global default"
     */
    @DefaultMessage("global default")
    String globalDefault();

    /**
     * Translated "Line wrapping in this document differs from the {0}:".
     *
     * @return translated "Line wrapping in this document differs from the {0}:"
     */
    @DefaultMessage("Line wrapping in this document differs from the {0}:")
    String lineWrappingDiffersFromCurrent(String current);

    /**
     * Translated "Select how you''d like to handle line wrapping below:".
     *
     * @return translated "Select how you''d like to handle line wrapping below:"
     */
    @DefaultMessage("Select how you''d like to handle line wrapping below:")
    String selectHandleLineWrapping();

    /**
     * Translated "Use {0}-based line wrapping for this document".
     *
     * @return translated "Use {0}-based line wrapping for this document"
     */
    @DefaultMessage("Use {0}-based line wrapping for this document")
    String useBasedLineWrappingForDocument(String detectedLineWrappingForDocument);
    
    /**
     * Translated "Use {0}-based line wrapping for this project".
     *
     * @return translated "Use {0}-based line wrapping for this project"
     */
    @DefaultMessage("Use {0}-based line wrapping for this project")
    String useBasedLineWrappingForProject(String detectedLineWrappingForProject);

    /**
     * Translated "Use the current {0} default line wrapping for this document".
     *
     * @return translated "Use the current {0} default line wrapping for this document"
     */
    @DefaultMessage("Use the current {0} default line wrapping for this document")
    String useDefaultLinewrapping(String projectConfigChoice);

    /**
     * Translated "project".
     *
     * @return translated "project"
     */
    @DefaultMessage("project")
    String project();

    /**
     * Translated "global".
     *
     * @return translated "global"
     */
    @DefaultMessage("global")
    String global();

    /**
     * Translated "Learn more about visual mode line wrapping options".
     *
     * @return translated "Learn more about visual mode line wrapping options"
     */
    @DefaultMessage("Learn more about visual mode line wrapping options")
    String learnAboutVisualModeLineWrapping();

    /**
     * Translated "Wrap at column:".
     *
     * @return translated "Wrap at column:"
     */
    @DefaultMessage("Wrap at column:")
    String wrapAtColumnColon();

    /**
     * Translated "Collapse".
     *
     * @return translated "Collapse"
     */
    @DefaultMessage("Collapse")
    String collapseCapitalized();

    /**
     * Translated "Expand".
     *
     * @return translated "Expand"
     */
    @DefaultMessage("Expand")
    String expandCapitalized();

    /**
     * Translated "{0} code chunk".
     *
     * @return translated "{0} code chunk"
     */
    @DefaultMessage("{0} code chunk")
    String collapseOrExpandCodeChunk(String hintText);

    /**
     * Translated "Some of your source edits are still being backed up. If you continue, your latest changes may be lost. Do you want to continue?".
     *
     * @return translated "Some of your source edits are still being backed up. If you continue, your latest changes may be lost. Do you want to continue?"
     */
    @DefaultMessage("Some of your source edits are still being backed up. If you continue, your latest changes may be lost. Do you want to continue?")
    String editsStillBeingBackedUp();

    /**
     * Translated "The process cannot access the file because it is being used by another process".
     * Must match what is in rstudio\src\gwt\src\org\rstudio\studio\client\workbench\views\source\editors\text\EditorsTextConstants.java
     * @return translated "The process cannot access the file because it is being used by another process"
     */
    @DefaultMessage("The process cannot access the file because it is being used by another process")
    String processStillBeingUsedTextEditingTarget();

    /**
     * Translated "Error Autosaving File".
     *
     * @return translated "Error Autosaving File"
     */
    @DefaultMessage("Error Autosaving File")
    String errorAutosavingFile();

    /**
     * Translated "RStudio was unable to autosave this file. You may need to restart RStudio.".
     *
     * @return translated "RStudio was unable to autosave this file. You may need to restart RStudio."
     */
    @DefaultMessage("RStudio was unable to autosave this file. You may need to restart RStudio.")
    String rStudioUnableToAutosave();

    /**
     * Translated "Could not save {0}: {1}".
     *
     * @return translated "Could not save {0}: {1}"
     */
    @DefaultMessage("Could not save {0}: {1}")
    String couldNotSavePathPlusMessage(String path, String message);

    /**
     * Translated "Error saving {0}: {1}".
     *
     * @return translated "Error saving {0}: {1}"
     */
    @DefaultMessage("Error saving {0}: {1}")
    String errorSavingPathPlusMessage(String path, String message);

    /**
     * Translated "Document Outline".
     * @return translated "Document Outline"
     */
    @DefaultMessage("Document Outline")
    String documentOutline();

    /**
     * Translated "<td><input type=''button'' value=''More...'' data-action=''open''></input></td>".
     *
     * @return translated "<td><input type=''button'' value=''More...'' data-action=''open''></input></td>"
     */
    @DefaultMessage("<td><input type=''button'' value=''More...'' data-action=''open''></input></td>")
    String moreButtonCell();

    /**
     * Translated "{0} <...truncated...> )".
     *
     * @return translated "{0} <...truncated...> )"
     */
    @DefaultMessage("{0} <...truncated...> )")
    String truncatedEllipses(String truncated);

    /**
     * Translated "The document uses {0}-based line wrapping".
     *
     * @return translated "The document uses {0}-based line wrapping"
     */
    @DefaultMessage("The document uses {0}-based line wrapping")
    String documentUsesBasedLineWrapping(String detectedLineWrapping);

    /**
     * Translated "The {0} is no line wrapping".
     *
     * @return translated "The {0} is no line wrapping"
     */
    @DefaultMessage("The {0} is no line wrapping")
    String defaultNoLineWrapping(String current);

    /**
     * Translated "The {0} is {1}-based line wrapping".
     *
     * @return translated "The {0} is {1}-based line wrapping"
     */
    @DefaultMessage("The {0} is {1}-based line wrapping")
    String defaultConfiguredBasedLineWrapping(String current, String configuredLineWrapping);

    /**
     * Translated "Create R Notebook".
     * @return translated "Create R Notebook"
     */
    @DefaultMessage("Create R Notebook")
    String createRNotebookText();


    /**
     * Translated "Creating Shiny applications".
     * @return translated "Creating Shiny applications"
     */
    @DefaultMessage("Creating Shiny applications")
    String creatingShinyApplicationsText();

    /**
     * Translated "Authoring R Presentations".
     * @return translated "Authoring R Presentations"
     */
    @DefaultMessage("Authoring R Presentations")
    String authoringRPresentationsText();

    /**
     * Translated "Creating R Plumber API".
     * @return translated "Creating R Plumber API"
     */
    @DefaultMessage("Creating R Plumber API")
    String creatingRPlumberAPIText();

    /**
     * Translated "Using R Notebooks".
     * @return translated "Using R Notebooks"
     */
    @DefaultMessage("Using R Notebooks")
    String usingRNotebooksText();

    /**
     * Translated "The profiler".
     * @return translated "The profiler"
     */
    @DefaultMessage("The profiler")
    String theProfilerText();

    /**
     * Translated "Could not resolve {0}. Please make sure this is an R package project with a BugReports field set.".
     *
     * @return translated "Could not resolve {0}. Please make sure this is an R package project with a BugReports field set."
     */
    @DefaultMessage("Could not resolve {0}. Please make sure this is an R package project with a BugReports field set.")
    String couldNotResolveIssue(String issue);

    /**
     * Translated "Unable to Edit".
     * @return translated "Unable to Edit".
     */
    @DefaultMessage("Unable to Edit")
    String unableToEditTitle();

    /**
     * Translated "The options use syntax that is not currently supported by the editing UI. Please edit manually.".
     * @return translated "The options use syntax that is not currently supported by the editing UI. Please edit manually.".
     */
    @DefaultMessage("The options use syntax that is not currently supported by the editing UI. Please edit manually.")
    String unableToEditMessage();
}

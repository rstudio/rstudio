/*
 * EditorsTextConstants.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

public interface EditorsTextConstants extends com.google.gwt.i18n.client.Messages {

    @DefaultMessage("R Console")
    @Key("rConsole")
    String rConsole();

    @DefaultMessage("Chunk Feedback")
    @Key("chunkFeedback")
    String chunkFeedback();

    @DefaultMessage("Chunk HTML Page Output Frame")
    @Key("chunkHtmlPageOutputFrame")
    String chunkHtmlPageOutputFrame();

    @DefaultMessage("Chunk HTML Output Frame")
    @Key("chunkHtmlOutputFrame")
    String chunkHtmlOutputFrame();

    @DefaultMessage("RStudio: Notebook Output")
    @Key("chunkSatelliteWindowInitTitle")
    String chunkSatelliteWindowInitTitle();

    @DefaultMessage("(No image at path {0})")
    @Key("noImageLabel")
    String noImageLabel(String path);

    @DefaultMessage("No image at path {0}")
    @Key("noImageLabelNoParentheses")
    String noImageLabelNoParentheses(String path);

    @DefaultMessage("Double-Click to Zoom")
    @Key("doubleClickToZoom")
    String doubleClickToZoom();

    @DefaultMessage("The selected code could not be parsed.\n\nAre you sure you want to continue?")
    @Key("refactorServerRequestCallbackError")
    String refactorServerRequestCallbackError();

    @DefaultMessage("Failed to check if results are available")
    @Key("onShinyCompareTestError")
    String onShinyCompareTestError();

    @DefaultMessage("No Failed Results")
    @Key("onShinyCompareTestResponseCaption")
    String onShinyCompareTestResponseCaption();

    @DefaultMessage("There are no failed tests to compare.")
    @Key("onShinyCompareTestResponseMessage")
    String onShinyCompareTestResponseMessage();

    @DefaultMessage("Failed to check for additional dependencies")
    @Key("checkTestPackageDependenciesError")
    String checkTestPackageDependenciesError();

    @DefaultMessage("The package shinytest requires additional components to run.\\n\\nInstall additional components?")
    @Key("checkTestPackageDependenciesMessage")
    String checkTestPackageDependenciesMessage();

    @DefaultMessage("Install Shinytest Dependencies")
    @Key("checkTestPackageDependenciesCaption")
    String checkTestPackageDependenciesCaption();

    @DefaultMessage("Failed to install additional dependencies")
    @Key("installShinyTestDependenciesError")
    String installShinyTestDependenciesError();

    @DefaultMessage("The file {0} has changed on disk. Do you want to reload the file from disk and discard your unsaved changes?")
    @Key("checkForExternalEditFileChangedMessage")
    String checkForExternalEditFileChangedMessage(String fileName);

    @DefaultMessage("File Changed")
    @Key("checkForExternalEditFileChangedCaption")
    String checkForExternalEditFileChangedCaption();

    @DefaultMessage("The file {0} has been deleted or moved. Do you want to close this file now?")
    @Key("checkForExternalEditFileDeletedMessage")
    String checkForExternalEditFileDeletedMessage(String pathName);

    @DefaultMessage("File Deleted")
    @Key("checkForExternalEditFileDeletedCaption")
    String checkForExternalEditFileDeletedCaption();

    @DefaultMessage("The file ''{0}'' cannot be compiled to a PDF because TeX does not understand paths with spaces. If you rename the file to remove spaces then PDF compilation will work correctly.")
    @Key("fireCompilePdfEventErrorMessage")
    String fireCompilePdfEventErrorMessage(String fileName);

    @DefaultMessage("Invalid Filename")
    @Key("fireCompilePdfEventErrorCaption")
    String fireCompilePdfEventErrorCaption();

    @DefaultMessage("This will remove all previously generated output for {0} (html, prerendered data, knitr cache, etc.).\n\nAre you sure you want to clear the output now?")
    @Key("onClearPrerenderedOutputMessage")
    String onClearPrerenderedOutputMessage(String docPath);

    @DefaultMessage("Clear Prerendered Output")
    @Key("onClearPrerenderedOutputCaption")
    String onClearPrerenderedOutputCaption();

    @DefaultMessage("Clearing the Knitr cache will delete the cache directory for {0}. \n\nAre you sure you want to clear the cache now?")
    @Key("onClearKnitrCacheMessage")
    String onClearKnitrCacheMessage(String docPath);

    @DefaultMessage("Clear Knitr Cache")
    @Key("onClearKnitrCacheCaption")
    String onClearKnitrCacheCaption();

    @DefaultMessage("Unable to Compile Report")
    @Key("generateNotebookCaption")
    String generateNotebookCaption();

    @DefaultMessage("R Presentations require the knitr package (version 1.2 or higher)")
    @Key("previewRpresentationMessage")
    String previewRpresentationMessage();

    @DefaultMessage("Unable to Preview")
    @Key("previewRpresentationCaption")
    String previewRpresentationCaption();

    @DefaultMessage("Sourcing Python scripts")
    @Key("sourcePythonUserPrompt")
    String sourcePythonUserPrompt();

    @DefaultMessage("Executing Python")
    @Key("sourcePythonProgressCaption")
    String sourcePythonProgressCaption();

    @DefaultMessage("The currently active source file is not saved so doesn''t have a directory to change into.")
    @Key("onSetWorkingDirToActiveDocMessage")
    String onSetWorkingDirToActiveDocMessage();

    @DefaultMessage("Source File Not Saved")
    @Key("onSetWorkingDirToActiveDocCaption")
    String onSetWorkingDirToActiveDocCaption();

    @DefaultMessage("Enter line number:")
    @Key("onGoToLineLabel")
    String onGoToLineLabel();

    @DefaultMessage("Go to Line")
    @Key("onGoToLineTitle")
    String onGoToLineTitle();

    @DefaultMessage("{0} File name ")
    @Key("getCurrentStatusFileName")
    String getCurrentStatusFileName(String emptyString);

    @DefaultMessage("{0} File type")
    @Key("getCurrentStatusFileType")
    String getCurrentStatusFileType(String emptyString);

    @DefaultMessage("{0} Scope")
    @Key("getCurrentStatusScope")
    String getCurrentStatusScope(String emptyString);

    @DefaultMessage("{0} Column")
    @Key("getCurrentStatusColumn")
    String getCurrentStatusColumn(String emptyString);

    @DefaultMessage("Row")
    @Key("getCurrentStatusRow")
    String getCurrentStatusRow();

    @DefaultMessage("No name")
    @Key("noName")
    String noName();

    @DefaultMessage("None")
    @Key("none")
    String none();

    @DefaultMessage("Run After")
    @Key("runAfter")
    String runAfter();

    @DefaultMessage("Run Previous")
    @Key("runPrevious")
    String runPrevious();

    @DefaultMessage("Run All")
    @Key("runAll")
    String runAll();

    @DefaultMessage("Section label:")
    @Key("onInsertSectionLabel")
    String onInsertSectionLabel();

    @DefaultMessage("Insert Section")
    @Key("onInsertSectionTitle")
    String onInsertSectionTitle();

    @DefaultMessage("Couldn''t determine the format options from the YAML front matter. Make sure the YAML defines a supported output format in its ''output'' field.")
    @Key("showFrontMatterEditorDialogMessage")
    String showFrontMatterEditorDialogMessage();

    @DefaultMessage("Edit Format Failed")
    @Key("showFrontMatterEditorDialogCaption")
    String showFrontMatterEditorDialogCaption();

    @DefaultMessage("The YAML front matter in this document could not be successfully parsed. This parse error needs to be resolved before format options can be edited.")
    @Key("showFrontMatterEditorErrMsg")
    String showFrontMatterEditorErrMsg();

    @DefaultMessage("Edit Format Failed")
    @Key("showFrontMatterEditorErrCaption")
    String showFrontMatterEditorErrCaption();

    @DefaultMessage("Can''t find the YAML front matter for this document. Make sure the front matter is enclosed by lines containing only three dashes: ---.")
    @Key("showFrontMatterEditorMessage")
    String showFrontMatterEditorMessage();

    @DefaultMessage("Function Name")
    @Key("functionNameLabel")
    String functionNameLabel();

    @DefaultMessage("Please select the code to extract into a function.")
    @Key("pleaseSelectCodeMessage")
    String pleaseSelectCodeMessage();

    @DefaultMessage("Extract Function")
    @Key("extractActiveFunctionRefactoringName")
    String extractActiveFunctionRefactoringName();

    @DefaultMessage("The {0} command is only valid for R code chunks.")
    @Key("showRModeWarningMessage")
    String showRModeWarningMessage(String command);

    @DefaultMessage("Command Not Available")
    @Key("showRModeWarningCaption")
    String showRModeWarningCaption();

    @DefaultMessage("Variable Name")
    @Key("variableName")
    String variableName();

    @DefaultMessage("Extract local variable")
    @Key("extractLocalVariableRefactoringName")
    String extractLocalVariableRefactoringName();

    @DefaultMessage("Cancel")
    @Key("cancel")
    String cancel();

    @DefaultMessage("Reopen Document")
    @Key("reopenDocument")
    String reopenDocument();

    @DefaultMessage("This document has unsaved changes. These changes will be discarded when re-opening the document.\n\nWould you like to proceed?")
    @Key("onReopenSourceDocWithEncodingMessage")
    String onReopenSourceDocWithEncodingMessage();

    @DefaultMessage("Reopen with Encoding")
    @Key("onReopenSourceDocWithEncodingCaption")
    String onReopenSourceDocWithEncodingCaption();

    @DefaultMessage("Total words: {0} {1}")
    @Key("onWordCountMessage")
    String onWordCountMessage(int totalWords, String selectedWordsText);

    @DefaultMessage("Word Count")
    @Key("wordCount")
    String wordCount();

    @DefaultMessage("\nSelected words: {0}")
    @Key("selectedWords")
    String selectedWords(int selectionWords);

    @DefaultMessage("for {0}.")
    @Key("renameInScopeSelectedItemMessage")
    String renameInScopeSelectedItemMessage(String selectedItem);

    @DefaultMessage("matches ")
    @Key("renameInScopeMatchesPlural")
    String renameInScopeMatchesPlural();

    @DefaultMessage("match ")
    @Key("renameInScopeMatch")
    String renameInScopeMatch();

    @DefaultMessage("Found {0} ")
    @Key("renameInScopeFoundMatchesMessage")
    String renameInScopeFoundMatchesMessage(int matches);

    @DefaultMessage("No matches for ''{0}''")
    @Key("renameInScopeNoMatchesMessage")
    String renameInScopeNoMatchesMessage(String selectionValue);

    @DefaultMessage("This file was created as an R script however the file extension you specified will change it into another file type that will no longer open as an R script.\\n\\nAre you sure you want to change the type of the file so that it is no longer an R script?")
    @Key("saveNewFileWithEncodingWarningMessage")
    String saveNewFileWithEncodingWarningMessage();

    @DefaultMessage("Confirm Change File Type")
    @Key("saveNewFileWithEncodingWarningCaption")
    String saveNewFileWithEncodingWarningCaption();

    @DefaultMessage("Save File - {0}")
    @Key("saveNewFileWithEncodingSaveFileCaption")
    String saveNewFileWithEncodingSaveFileCaption(String nameValue);

    @DefaultMessage("Don''t Save")
    @Key("dontSave")
    String dontSave();

    @DefaultMessage("Save")
    @Key("save")
    String save();

    @DefaultMessage("The document ''{0}'' has unsaved changes.\\n\\nDo you want to save these changes?")
    @Key("saveWithPromptMessage")
    String saveWithPromptMessage(String documentName);

    @DefaultMessage("{0} - Unsaved Changes")
    @Key("saveWithPromptCaption")
    String saveWithPromptCaption(String documentName);

    @DefaultMessage("Close Anyway")
    @Key("closeAnyway")
    String closeAnyway();

    @DefaultMessage("You''re actively following another user''s cursor in ''{0}''.\n\nIf you close this file, you won''t see their cursor until they edit another file.")
    @Key("onBeforeDismissMessage")
    String onBeforeDismissMessage(String nameValue);

    @DefaultMessage("{0} - Active Following Session")
    @Key("onBeforeDismissCaption")
    String onBeforeDismissCaption(String nameValue);

    @DefaultMessage("(No {0} defined)")
    @Key("addFunctionsToMenuText")
    String addFunctionsToMenuText(String typeText);

    @DefaultMessage("chunks")
    @Key("chunks")
    String chunks();

    @DefaultMessage("functions")
    @Key("functions")
    String functions();

    @DefaultMessage("Breakpoints will be activated when this file is sourced.")
    @Key("updateBreakpointWarningBarSourcedMessage")
    String updateBreakpointWarningBarSourcedMessage();

    @DefaultMessage("Breakpoints will be activated when an updated version of the {0} package is loaded")
    @Key("updateBreakpointWarningBarPackageMessage")
    String updateBreakpointWarningBarPackageMessage(String pendingPackageName);

    @DefaultMessage("Breakpoints will be activated when the package is built and reloaded.")
    @Key("updateBreakpointWarningBarPackageLoadMessage")
    String updateBreakpointWarningBarPackageLoadMessage();

    @DefaultMessage("Breakpoints will be activated when the file or function is finished executing.")
    @Key("updateBreakpointWarningBarFunctionMessage")
    String updateBreakpointWarningBarFunctionMessage();

    @DefaultMessage("Breakpoints cannot be set until the file is saved.")
    @Key("onBreakpointSetNewDocWarning")
    String onBreakpointSetNewDocWarning();

    @DefaultMessage("Breakpoints not supported in Plumber API files.")
    @Key("onBreakpointSetPlumberfileWarning")
    String onBreakpointSetPlumberfileWarning();

    @DefaultMessage("Error Saving Setting")
    @Key("errorSavingSetting")
    String errorSavingSetting();

    @DefaultMessage("Save File")
    @Key("saveFile")
    String saveFile();

    @DefaultMessage("Installing TinyTeX")
    @Key("installTinyTeX")
    String installTinyTeX();

    @DefaultMessage("Installing tinytex")
    @Key("installTinytexLowercase")
    String installTinytexLowercase();

    @DefaultMessage("Debug lines may not match because the file contains unsaved changes.")
    @Key("updateDebugWarningBarMessage")
    String updateDebugWarningBarMessage();

    @DefaultMessage("Work on a Copy")
    @Key("beginQueuedCollabSessionNoLabel")
    String beginQueuedCollabSessionNoLabel();

    @DefaultMessage("Discard and Join")
    @Key("beginQueuedCollabSessionYesLabel")
    String beginQueuedCollabSessionYesLabel();

    @DefaultMessage("You have unsaved changes to {0}, but another user is editing the file. Do you want to discard your changes and join their edit session, or make your own copy of the file to work on?")
    @Key("beginQueuedCollabSessionMessage")
    String beginQueuedCollabSessionMessage(String filename);

    @DefaultMessage("Join Edit Session")
    @Key("beginQueuedCollabSessionCaption")
    String beginQueuedCollabSessionCaption();

    @DefaultMessage("Breakpoints can only be set inside the body of a function. ")
    @Key("onBreakpointsSavedWarningBar")
    String onBreakpointsSavedWarningBar();

    @DefaultMessage("{0}-copy{1}")
    @Key("saveAsPathName")
    String saveAsPathName(String stem, String extension);

    @DefaultMessage("This source file is read-only so changes cannot be saved")
    @Key("onResponseReceivedMessage")
    String onResponseReceivedMessage();

    @DefaultMessage("Error Saving File")
    @Key("errorSavingFile")
    String errorSavingFile();

    @DefaultMessage("The process cannot access the file because it is being used by another process")
    @Key("onErrorMessage")
    String onErrorMessage();

    @DefaultMessage("{0} is configured to weave {1} however the {2} package is not installed.")
    @Key("checkCompilersRnWPackageNotInstalled")
    String checkCompilersRnWPackageNotInstalled(String fRnWname, String forcontext, String fRnWPackage);

    @DefaultMessage("Rnw files")
    @Key("rnwFiles")
    String rnwFiles();

    @DefaultMessage("Rnw files for this project")
    @Key("rnwFilesForProject")
    String rnwFilesForProject();

    @DefaultMessage("this file")
    @Key("thisFile")
    String thisFile();

    @DefaultMessage("This server does not have LaTeX installed. You may not be able to compile.")
    @Key("checkCompilersServerWarning")
    String checkCompilersServerWarning();

    @DefaultMessage("No LaTeX installation detected. Please install LaTeX before compiling.")
    @Key("checkCompilersDesktopWarning")
    String checkCompilersDesktopWarning();

    @DefaultMessage("Unknown Rnw weave method ''{0}'' specified (valid types are {1})")
    @Key("checkCompilersRnWWeaveTypeError")
    String checkCompilersRnWWeaveTypeError(String directiveName, String typeNames);

    @DefaultMessage("Unknown LaTeX program type ''{0}'' specified (valid types are {1}")
    @Key("checkCompilersUnknownLatexType")
    String checkCompilersUnknownLatexType(String latexProgramDirective, String typeNames);

    @DefaultMessage("Finding usages...")
    @Key("findingUsages")
    String findingUsages();

    @DefaultMessage("The Rcpp package (version 0.10.1 or higher) is not currently installed")
    @Key("checkBuildCppDependenciesRcppPackage")
    String checkBuildCppDependenciesRcppPackage();

    @DefaultMessage("The tools required to build C/C++ code for R are not currently installed")
    @Key("checkBuildCppDependenciesToolsNotInstalled")
    String checkBuildCppDependenciesToolsNotInstalled();

    @DefaultMessage("Find")
    @Key("find")
    String find();

    @DefaultMessage("Find/Replace")
    @Key("findOrReplace")
    String findOrReplace();

    @DefaultMessage("''{0}'' is not a known previewer for JavaScript files. Did you mean ''r2d3''?")
    @Key("previewJSErrorMessage")
    String previewJSErrorMessage(String functionString);

    @DefaultMessage("Error Previewing JavaScript")
    @Key("previewJSErrorCaption")
    String previewJSErrorCaption();

    @DefaultMessage("Block Quote")
    @Key("blockQuote")
    String blockQuote();

    @DefaultMessage("Verbatim")
    @Key("verbatim")
    String verbatim();

    @DefaultMessage("Description List")
    @Key("descriptionList")
    String descriptionList();

    @DefaultMessage("Numbered List")
    @Key("numberedList")
    String numberedList();

    @DefaultMessage("Bullet List")
    @Key("bulletList")
    String bulletList();

    @DefaultMessage("Quote")
    @Key("quote")
    String quote();

    @DefaultMessage("Typewriter")
    @Key("typewriter")
    String typewriter();

    @DefaultMessage("Italic")
    @Key("italic")
    String italic();

    @DefaultMessage("Bold")
    @Key("bold")
    String bold();

    @DefaultMessage("Sub-Subsection")
    @Key("subSubsection")
    String subSubsection();

    @DefaultMessage("Subsection")
    @Key("subsection")
    String subsection();

    @DefaultMessage("Section")
    @Key("section")
    String section();

    @DefaultMessage("(Untitled Slide)")
    @Key("untitledSlide")
    String untitledSlide();

    @DefaultMessage("(No Slides)")
    @Key("noSlides")
    String noSlides();

    @DefaultMessage("R Markdown")
    @Key("rMarkdown")
    String rMarkdown();

    @DefaultMessage("R Notebook")
    @Key("rNotebook")
    String rNotebook();

    @DefaultMessage("Overwrite {0}")
    @Key("createDraftFromTemplateCaption")
    String createDraftFromTemplateCaption(String fileType);

    @DefaultMessage("{0} exists. Overwrite it?")
    @Key("createDraftFromTemplateMessage")
    String createDraftFromTemplateMessage(String name);

    @DefaultMessage("Overwrite")
    @Key("overwrite")
    String overwrite();

    @DefaultMessage("Template Creation Failed")
    @Key("getTemplateContentErrorCaption")
    String getTemplateContentErrorCaption();

    @DefaultMessage("Failed to load content from the template at {0}: {1}")
    @Key("getTemplateContentErrorMessage")
    String getTemplateContentErrorMessage(String templatePath, String errorMessage);

    @DefaultMessage("R Session Busy")
    @Key("getRMarkdownParamsFileCaption")
    String getRMarkdownParamsFileCaption();

    @DefaultMessage("Unable to edit parameters (the R session is currently busy).")
    @Key("getRMarkdownParamsFileMessage")
    String getRMarkdownParamsFileMessage();

    @DefaultMessage("File Remove Failed")
    @Key("cleanAndCreateTemplateCaption")
    String cleanAndCreateTemplateCaption();

    @DefaultMessage("Couldn''t remove {0}")
    @Key("cleanAndCreateTemplateMessage")
    String cleanAndCreateTemplateMessage(String path);

    @DefaultMessage("Creating R Markdown Document...")
    @Key("createDraftFromTemplateProgressMessage")
    String createDraftFromTemplateProgressMessage();

    @DefaultMessage("Couldn''t create a template from {0} at {1}.\n\n{2}")
    @Key("createDraftFromTemplateOnError")
    String createDraftFromTemplateOnError(String templatePath, String target, String errorMessage);

    @DefaultMessage("{0} requires the knitr package (version {1} or higher)")
    @Key("showKnitrPreviewWarningBar")
    String showKnitrPreviewWarningBar(String feature, String requiredVersion);

    @DefaultMessage("Spellcheck")
    @Key("spellcheck")
    String spellcheck();

    @DefaultMessage("''{0}'' is misspelled")
    @Key("wordIsMisspelled")
    String wordIsMisspelled(String word);
    

    @DefaultMessage("Ignore word")
    @Key("ignoreWord")
    String ignoreWord();

    @DefaultMessage("Add to user dictionary")
    @Key("addToUserDictionary")
    String addToUserDictionary();

    @DefaultMessage("Error Previewing SQL")
    @Key("errorPreviewingSql")
    String errorPreviewingSql();

    @DefaultMessage("Source on Save")
    @Key("sourceOnSave")
    String sourceOnSave();

    @DefaultMessage("Text editor")
    @Key("textEditor")
    String textEditor();

    @DefaultMessage("Compare Results")
    @Key("compareResults")
    String compareResults();

    @DefaultMessage("Run Tests")
    @Key("runTests")
    String runTests();

    @DefaultMessage("Compile Report ({0})")
    @Key("compileReport")
    String compileReport(String cmdtext);

    @DefaultMessage("Shiny test options")
    @Key("shinyTestOptions")
    String shinyTestOptions();

    @DefaultMessage("Knit options")
    @Key("knitOptions")
    String knitOptions();

    @DefaultMessage("Run document options")
    @Key("runDocumentOptions")
    String runDocumentOptions();

    @DefaultMessage("Source the active document")
    @Key("sourceButtonTitle")
    String sourceButtonTitle();

    @DefaultMessage("Source")
    @Key("source")
    String source();

    @DefaultMessage("{0} (with echo)")
    @Key("sourceButtonTitleWithEcho")
    String sourceButtonTitleWithEcho(String title);

    @DefaultMessage("Source options")
    @Key("sourceOptions")
    String sourceOptions();

    @DefaultMessage("Run Setup Chunk Automatically")
    @Key("runSetupChunkAuto")
    String runSetupChunkAuto();

    @DefaultMessage("Run")
    @Key("run")
    String run();

    @DefaultMessage("Run app options")
    @Key("runAppOptions")
    String runAppOptions();

    @DefaultMessage("Run API options")
    @Key("runApiOptions")
    String runApiOptions();

    @DefaultMessage("Show ")
    @Key("show")
    String show();

    @DefaultMessage("Hide ")
    @Key("hide")
    String hide();

    @DefaultMessage("Show whitespace")
    @Key("showWhitespace")
    String showWhitespace();

    @DefaultMessage("Format")
    @Key("format")
    String format();

    @DefaultMessage("Veria")
    @Key("codeTools")
    String codeTools();

    @DefaultMessage("{0} on Save")
    @Key("actionOnSave")
    String actionOnSave(String action);

    @DefaultMessage("Untitled Text editor")
    @Key("untitledTextEditor")
    String untitledTextEditor();

    @DefaultMessage("Compile PDF")
    @Key("compilePdf")
    String compilePdf();

    @DefaultMessage("R Script")
    @Key("rScript")
    String rScript();

    @DefaultMessage("Install {0} dependencies")
    @Key("showRequiredPackagesMissingWarningCaption")
    String showRequiredPackagesMissingWarningCaption(String scriptname);

    @DefaultMessage("Preview")
    @Key("preview")
    String preview();

    @DefaultMessage("Knit to ")
    @Key("knitTo")
    String knitTo();

    @DefaultMessage("Preview Notebook")
    @Key("previewNotebook")
    String previewNotebook();

    @DefaultMessage("Document Directory")
    @Key("documentDirectory")
    String documentDirectory();

    @DefaultMessage("Project Directory")
    @Key("projectDirectory")
    String projectDirectory();

    @DefaultMessage("Current Working Directory")
    @Key("currentWorkingDirectory")
    String currentWorkingDirectory();

    @DefaultMessage("Knit Directory")
    @Key("knitDirectory")
    String knitDirectory();

    @DefaultMessage("Render {0}")
    @Key("renderFormatName")
    String renderFormatName(String formatName);

    @DefaultMessage("Presentation")
    @Key("presentation")
    String presentation();

    @DefaultMessage("Document")
    @Key("document")
    String document();

    @DefaultMessage("Run {0}")
    @Key("setIsShinyFormatKnitCommandText")
    String setIsShinyFormatKnitCommandText(String docType);

    @DefaultMessage("View the current {0} with Shiny ({1})")
    @Key("setIsShinyFormatKnitDocumentButtonTitle")
    String setIsShinyFormatKnitDocumentButtonTitle(String doctype, String htmlString);

    @DefaultMessage("Preview the notebook ({0})")
    @Key("setIsNotebookFormatButtonTitle")
    String setIsNotebookFormatButtonTitle(String htmlString);

    @DefaultMessage("Content not publishable")
    @Key("invokePublishCaption")
    String invokePublishCaption();

    @DefaultMessage("This item cannot be published.")
    @Key("invokePublishMessage")
    String invokePublishMessage();

    @DefaultMessage("Knit{0}")
    @Key("setFormatTextKnitCommandText")
    String setFormatTextKnitCommandText(String text);

    @DefaultMessage("Render")
    @Key("setFormatTextQuartoCommandText")
    String setFormatTextQuartoCommandText();

    @DefaultMessage("Preview{0}")
    @Key("setFormatTextPreviewCommandText")
    String setFormatTextPreviewCommandText(String text);

    @DefaultMessage("Source Script")
    @Key("sourceScript")
    String sourceScript();

    @DefaultMessage("Save changes and source the current script")
    @Key("setSourceButtonFromScriptStatePythonDesc")
    String setSourceButtonFromScriptStatePythonDesc();

    @DefaultMessage("Run Script")
    @Key("runScript")
    String runScript();

    @DefaultMessage("Save changes and run the current script")
    @Key("setSourceButtonFromScriptStateDesc")
    String setSourceButtonFromScriptStateDesc();

    @DefaultMessage("Save changes and preview")
    @Key("setSourceButtonFromScriptStateSavePreview")
    String setSourceButtonFromScriptStateSavePreview();

    @DefaultMessage("Reload App")
    @Key("reloadApp")
    String reloadApp();

    @DefaultMessage("Save changes and reload the Shiny application")
    @Key("saveChangesAndReload")
    String saveChangesAndReload();

    @DefaultMessage("Run App")
    @Key("runApp")
    String runApp();

    @DefaultMessage("Run the Shiny application")
    @Key("runTheShinyApp")
    String runTheShinyApp();

    @DefaultMessage("Reload API")
    @Key("reloadApi")
    String reloadApi();

    @DefaultMessage("Save changes and reload the Plumber API")
    @Key("saveChangesReloadPlumberApi")
    String saveChangesReloadPlumberApi();

    @DefaultMessage("Run API")
    @Key("runApi")
    String runApi();

    @DefaultMessage("Run the Plumber API")
    @Key("runPlumberApi")
    String runPlumberApi();

    @DefaultMessage("Preview in Viewer Pane")
    @Key("previewInViewerPane")
    String previewInViewerPane();

    @DefaultMessage("Preview in Window")
    @Key("previewInWindow")
    String previewInWindow();

    @DefaultMessage("(No Preview)")
    @Key("noPreviewParentheses")
    String noPreviewParentheses();

    @DefaultMessage("Use Visual Editor")
    @Key("useVisualEditor")
    String useVisualEditor();

    @DefaultMessage("Preview Images and Equations")
    @Key("previewImagesEquations")
    String previewImagesEquations();

    @DefaultMessage("Show Previews Inline")
    @Key("showPreviewsInline")
    String showPreviewsInline();

    @DefaultMessage("Chunk Output Inline")
    @Key("chunkOutputInline")
    String chunkOutputInline();

    @DefaultMessage("Chunk Output in Console")
    @Key("chunkOutputInConsole")
    String chunkOutputInConsole();

    @DefaultMessage("Knit")
    @Key("knit")
    String knit();

    @DefaultMessage("Render")
    @Key("render")
    String render();

    @DefaultMessage("Print Frame")
    @Key("printFrame")
    String printFrame();

    @DefaultMessage("Visual")
    @Key("Visual")
    String visual();

    @DefaultMessage("Markdown editing tools")
    @Key("markdownEditingTools")
    String markdownEditingTools();

    @DefaultMessage("Compiling C/C++ code for R")
    @Key("compilingCode")
    String compilingCode();

    @DefaultMessage("Running shiny documents")
    @Key("runningShinyDocuments")
    String runningShinyDocuments();

    @DefaultMessage("Compiling notebooks from R scripts")
    @Key("compilingNotebooks")
    String compilingNotebooks();

    @DefaultMessage("Rendering R Markdown documents")
    @Key("renderingR")
    String renderingR();

    @DefaultMessage("Specifying Knit parameters")
    @Key("specifyingKnit")
    String specifyingKnit();

    @DefaultMessage("Creating R Markdown documents")
    @Key("creatingRMarkdown")
    String creatingRMarkdown();

    @DefaultMessage("Copilot: Waiting for completions...")
    @Key("copilotWaiting")
    String copilotWaiting();

    @DefaultMessage("Copilot: No completions available.")
    @Key("copilotNoCompletions")
    String copilotNoCompletions();

    @DefaultMessage("Copilot: Completion response received.")
    @Key("copilotResponseReceived")
    String copilotResponseReceived();

    @DefaultMessage("Copilot: {0}")
    @Key("copilotResponseErrorMessage")
    String copilotResponseErrorMessage(String message);

    @DefaultMessage("Copilot: Automatic completions have been enabled.")
    @Key("copilotEnabled")
    String copilotEnabled();

    @DefaultMessage("Copilot: Automatic completions have been disabled.")
    @Key("copilotDisabled")
    String copilotDisabled();
    

    @DefaultMessage("Reformat Document on Save")
    @Key("reformatDocumentOnSave")
    String reformatDocumentOnSave();
    
}

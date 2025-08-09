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
    /**
     * Translated "R Console".
     *
     * @return translated "R Console"
     */
    String rConsole();

    /**
     * Translated "Chunk Feedback".
     *
     * @return translated "Chunk Feedback"
     */
    String chunkFeedback();

    /**
     * Translated "Chunk HTML Page Output Frame".
     *
     * @return translated "Chunk HTML Page Output Frame"
     */
    String chunkHtmlPageOutputFrame();

    /**
     * Translated "Chunk HTML Output Frame".
     *
     * @return translated "Chunk HTML Output Frame"
     */
    String chunkHtmlOutputFrame();

    /**
     * Translated "RStudio: Notebook Output".
     *
     * @return translated "RStudio: Notebook Output"
     */
    String chunkSatelliteWindowInitTitle();

    /**
     * Translated "(No image at path {0})".
     *
     * @return translated "(No image at path {0})"
     */
    String noImageLabel(String path);

    /**
     * Translated "No image at path {0}".
     *
     * @return translated "No image at path {0}"
     */
    String noImageLabelNoParentheses(String path);

    /**
     * Translated "Double-Click to Zoom".
     *
     * @return translated "Double-Click to Zoom"
     */
    String doubleClickToZoom();

    /**
     * Translated "The selected code could not be parsed.\n\nAre you sure you want to continue?".
     *
     * @return translated "The selected code could not be parsed.\n\nAre you sure you want to continue?"
     */
    String refactorServerRequestCallbackError();

    /**
     * Translated "Failed to check if results are available".
     *
     * @return translated "Failed to check if results are available"
     */
    String onShinyCompareTestError();

    /**
     * Translated "No Failed Results".
     *
     * @return translated "No Failed Results"
     */
    String onShinyCompareTestResponseCaption();

    /**
     * Translated "There are no failed tests to compare.".
     *
     * @return translated "There are no failed tests to compare."
     */
    String onShinyCompareTestResponseMessage();

    /**
     * Translated "Failed to check for additional dependencies".
     *
     * @return translated "Failed to check for additional dependencies"
     */
    String checkTestPackageDependenciesError();

    /**
     * Translated "The package shinytest requires additional components to run.\n\nInstall additional components?".
     *
     * @return translated "The package shinytest requires additional components to run.\n\nInstall additional components?"
     */
    String checkTestPackageDependenciesMessage();

    /**
     * Translated "Install Shinytest Dependencies".
     *
     * @return translated "Install Shinytest Dependencies"
     */
    String checkTestPackageDependenciesCaption();

    /**
     * Translated "Failed to install additional dependencies".
     *
     * @return translated "Failed to install additional dependencies"
     */
    String installShinyTestDependenciesError();

    /**
     * Translated "The file {0} has changed on disk. Do you want to reload the file from disk and discard your unsaved changes?".
     *
     * @return translated "The file {0} has changed on disk. Do you want to reload the file from disk and discard your unsaved changes?"
     */
    String checkForExternalEditFileChangedMessage(String fileName);

    /**
     * Translated "File Changed".
     *
     * @return translated "File Changed"
     */
    String checkForExternalEditFileChangedCaption();

    /**
     * Translated "The file {0} has been deleted or moved. Do you want to close this file now?".
     *
     * @return translated "The file {0} has been deleted or moved. Do you want to close this file now?"
     */
    String checkForExternalEditFileDeletedMessage(String pathName);

    /**
     * Translated "File Deleted".
     *
     * @return translated "File Deleted"
     */
    String checkForExternalEditFileDeletedCaption();

    /**
     * Translated "The file ''{0}'' cannot be compiled to a PDF because TeX does not understand paths with spaces. If you rename the file to remove spaces then PDF compilation will work correctly.".
     *
     * @return translated "The file ''{0}'' cannot be compiled to a PDF because TeX does not understand paths with spaces. If you rename the file to remove spaces then PDF compilation will work correctly."
     */
    String fireCompilePdfEventErrorMessage(String fileName);

    /**
     * Translated "Invalid Filename".
     *
     * @return translated "Invalid Filename"
     */
    String fireCompilePdfEventErrorCaption();

    /**
     * Translated "This will remove all previously generated output for {0} (html, prerendered data, knitr cache, etc.).\n\nAre you sure you want to clear the output now?".
     *
     * @return translated "This will remove all previously generated output for {0} (html, prerendered data, knitr cache, etc.).\n\nAre you sure you want to clear the output now?"
     */
    String onClearPrerenderedOutputMessage(String docPath);

    /**
     * Translated "Clear Prerendered Output".
     *
     * @return translated "Clear Prerendered Output"
     */
    String onClearPrerenderedOutputCaption();

    /**
     * Translated "Clearing the Knitr cache will delete the cache directory for {0}. \n\nAre you sure you want to clear the cache now?".
     *
     * @return translated "Clearing the Knitr cache will delete the cache directory for {0}. \n\nAre you sure you want to clear the cache now?"
     */
    String onClearKnitrCacheMessage(String docPath);

    /**
     * Translated "Clear Knitr Cache".
     *
     * @return translated "Clear Knitr Cache"
     */
    String onClearKnitrCacheCaption();

    /**
     * Translated "Unable to Compile Report".
     *
     * @return translated "Unable to Compile Report"
     */
    String generateNotebookCaption();

    /**
     * Translated "R Presentations require the knitr package (version 1.2 or higher)".
     *
     * @return translated "R Presentations require the knitr package (version 1.2 or higher)"
     */
    String previewRpresentationMessage();

    /**
     * Translated "Unable to Preview".
     *
     * @return translated "Unable to Preview"
     */
    String previewRpresentationCaption();

    /**
     * Translated "Sourcing Python scripts".
     *
     * @return translated "Sourcing Python scripts"
     */
    String sourcePythonUserPrompt();

    /**
     * Translated "Executing Python".
     *
     * @return translated "Executing Python"
     */
    String sourcePythonProgressCaption();

    /**
     * Translated "The currently active source file is not saved so doesn''t have a directory to change into.".
     *
     * @return translated "The currently active source file is not saved so doesn''t have a directory to change into."
     */
    String onSetWorkingDirToActiveDocMessage();

    /**
     * Translated "Source File Not Saved".
     *
     * @return translated "Source File Not Saved"
     */
    String onSetWorkingDirToActiveDocCaption();

    /**
     * Translated "Enter line number:".
     *
     * @return translated "Enter line number:"
     */
    String onGoToLineLabel();

    /**
     * Translated "Go to Line".
     *
     * @return translated "Go to Line"
     */
    String onGoToLineTitle();

    /**
     * Translated "{0} File name ".
     * Has to be empty string as an argument to include the space
     * @return translated "{0} File name "
     */
    String getCurrentStatusFileName(String emptyString);

    /**
     * Translated "{0} File type".
     * Has to be empty string as an argument to include the space
     * @return translated "{0} File type"
     */
    String getCurrentStatusFileType(String emptyString);

    /**
     * Translated "{0} Scope".
     *
     * @return translated "{0} Scope"
     */
    String getCurrentStatusScope(String emptyString);

    /**
     * Translated "{0} Column".
     *
     * @return translated "{0} Column"
     */
    String getCurrentStatusColumn(String emptyString);

    /**
     * Translated "Row".
     *
     * @return translated "Row"
     */
    String getCurrentStatusRow();

    /**
     * Translated "No name".
     *
     * @return translated "No name"
     */
    String noName();

    /**
     * Translated "None".
     *
     * @return translated "None"
     */
    String none();

    /**
     * Translated "Run After".
     *
     * @return translated "Run After"
     */
    String runAfter();

    /**
     * Translated "Run Previous".
     *
     * @return translated "Run Previous"
     */
    String runPrevious();

    /**
     * Translated "Run All".
     *
     * @return translated "Run All"
     */
    String runAll();

    /**
     * Translated "Section label:".
     *
     * @return translated "Section label:"
     */
    String onInsertSectionLabel();

    /**
     * Translated "Insert Section".
     *
     * @return translated "Insert Section"
     */
    String onInsertSectionTitle();

    /**
     * Translated "Couldn''t determine the format options from the YAML front matter. Make sure the YAML defines a supported output format in its ''output'' field.".
     *
     * @return translated "Couldn''t determine the format options from the YAML front matter. Make sure the YAML defines a supported output format in its ''output'' field."
     */
    String showFrontMatterEditorDialogMessage();

    /**
     * Translated "Edit Format Failed".
     *
     * @return translated "Edit Format Failed"
     */
    String showFrontMatterEditorDialogCaption();

    /**
     * Translated "The YAML front matter in this document could not be successfully parsed. This parse error needs to be resolved before format options can be edited.".
     *
     * @return translated "The YAML front matter in this document could not be successfully parsed. This parse error needs to be resolved before format options can be edited."
     */
    String showFrontMatterEditorErrMsg();

    /**
     * Translated "Edit Format Failed".
     *
     * @return translated "Edit Format Failed"
     */
    String showFrontMatterEditorErrCaption();

    /**
     * Translated "Can''t find the YAML front matter for this document. Make sure the front matter is enclosed by lines containing only three dashes: ---.".
     *
     * @return translated "Can''t find the YAML front matter for this document. Make sure the front matter is enclosed by lines containing only three dashes: ---."
     */
    String showFrontMatterEditorMessage();

    /**
     * Translated "Function Name".
     *
     * @return translated "Function Name"
     */
    String functionNameLabel();

    /**
     * Translated "Please select the code to extract into a function.".
     *
     * @return translated "Please select the code to extract into a function."
     */
    String pleaseSelectCodeMessage();

    /**
     * Translated "Extract Function".
     *
     * @return translated "Extract Function"
     */
    String extractActiveFunctionRefactoringName();

    /**
     * Translated "The {0} command is only valid for R code chunks.".
     *
     * @return translated "The {0} command is only valid for R code chunks."
     */
    String showRModeWarningMessage(String command);

    /**
     * Translated "Command Not Available".
     *
     * @return translated "Command Not Available"
     */
    String showRModeWarningCaption();

    /**
     * Translated "Variable Name".
     *
     * @return translated "Variable Name"
     */
    String variableName();

    /**
     * Translated "Extract local variable".
     *
     * @return translated "Extract local variable"
     */
    String extractLocalVariableRefactoringName();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    String cancel();

    /**
     * Translated "Reopen Document".
     *
     * @return translated "Reopen Document"
     */
    String reopenDocument();

    /**
     * Translated "This document has unsaved changes. These changes will be discarded when re-opening the document.\n\nWould you like to proceed?".
     *
     * @return translated "This document has unsaved changes. These changes will be discarded when re-opening the document.\n\nWould you like to proceed?"
     */
    String onReopenSourceDocWithEncodingMessage();

    /**
     * Translated "Reopen with Encoding".
     *
     * @return translated "Reopen with Encoding"
     */
    String onReopenSourceDocWithEncodingCaption();

    /**
     * Translated "Total words: {0} {1}".
     *
     * @return translated "Total words: {0} {1}"
     */
    String onWordCountMessage(int totalWords, String selectedWordsText);

    /**
     * Translated "Word Count".
     *
     * @return translated "Word Count"
     */
    String wordCount();

    /**
     * Translated "\nSelected words: {0}".
     *
     * @return translated "\nSelected words: {0}"
     */
    String selectedWords(int selectionWords);

    /**
     * Translated "for {0}.".
     *
     * @return translated "for {0}."
     */
    String renameInScopeSelectedItemMessage(String selectedItem);

    /**
     * Translated "matches ".
     *
     * @return translated "matches "
     */
    String renameInScopeMatchesPlural();

    /**
     * Translated "match ".
     *
     * @return translated "match "
     */
    String renameInScopeMatch();

    /**
     * Translated "Found {0} ".
     *
     * @return translated "Found {0} "
     */
    String renameInScopeFoundMatchesMessage(int matches);

    /**
     * Translated "No matches for ''{0}''".
     *
     * @return translated "No matches for ''{0}''"
     */
    String renameInScopeNoMatchesMessage(String selectionValue);

    /**
     * Translated "This file was created as an R script however the file extension you specified will change it into another file type that will no longer open as an R script.\n\nAre you sure you want to change the type of the file so that it is no longer an R script?".
     *
     * @return translated "This file was created as an R script however the file extension you specified will change it into another file type that will no longer open as an R script.\n\nAre you sure you want to change the type of the file so that it is no longer an R script?"
     */
    String saveNewFileWithEncodingWarningMessage();

    /**
     * Translated "Confirm Change File Type".
     *
     * @return translated "Confirm Change File Type"
     */
    String saveNewFileWithEncodingWarningCaption();

    /**
     * Translated "Save File - {0}".
     *
     * @return translated "Save File - {0}"
     */
    String saveNewFileWithEncodingSaveFileCaption(String nameValue);

    /**
     * Translated "Don''t Save".
     *
     * @return translated "Don''t Save"
     */
    String dontSave();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    String save();

    /**
     * Translated "The document ''{0}'' has unsaved changes.\n\nDo you want to save these changes?".
     *
     * @return translated "The document ''{0}'' has unsaved changes.\n\nDo you want to save these changes?"
     */
    String saveWithPromptMessage(String documentName);

    /**
     * Translated "{0} - Unsaved Changes".
     *
     * @return translated "{0} - Unsaved Changes"
     */
    String saveWithPromptCaption(String documentName);

    /**
     * Translated "Close Anyway".
     *
     * @return translated "Close Anyway"
     */
    String closeAnyway();

    /**
     * Translated "You''re actively following another user''s cursor in ''{0}''.\n\nIf you close this file, you won''t see their cursor until they edit another file.".
     *
     * @return translated "You''re actively following another user''s cursor in ''{0}''.\n\nIf you close this file, you won''t see their cursor until they edit another file."
     */
    String onBeforeDismissMessage(String nameValue);

    /**
     * Translated "{0} - Active Following Session".
     *
     * @return translated "{0} - Active Following Session"
     */
    String onBeforeDismissCaption(String nameValue);

    /**
     * Translated "(No {0} defined)".
     *
     * @return translated "(No {0} defined)"
     */
    String addFunctionsToMenuText(String typeText);

    /**
     * Translated "chunks".
     *
     * @return translated "chunks"
     */
    String chunks();

    /**
     * Translated "functions".
     *
     * @return translated "functions"
     */
    String functions();

    /**
     * Translated "Breakpoints will be activated when this file is sourced.".
     *
     * @return translated "Breakpoints will be activated when this file is sourced."
     */
    String updateBreakpointWarningBarSourcedMessage();

    /**
     * Translated "Breakpoints will be activated when an updated version of the {0} package is loaded".
     *
     * @return translated "Breakpoints will be activated when an updated version of the {0} package is loaded"
     */
    String updateBreakpointWarningBarPackageMessage(String pendingPackageName);

    /**
     * Translated "Breakpoints will be activated when the package is built and reloaded.".
     *
     * @return translated "Breakpoints will be activated when the package is built and reloaded."
     */
    String updateBreakpointWarningBarPackageLoadMessage();

    /**
     * Translated "Breakpoints will be activated when the file or function is finished executing.".
     *
     * @return translated "Breakpoints will be activated when the file or function is finished executing."
     */
    String updateBreakpointWarningBarFunctionMessage();

    /**
     * Translated "Breakpoints cannot be set until the file is saved.".
     *
     * @return translated "Breakpoints cannot be set until the file is saved."
     */
    String onBreakpointSetNewDocWarning();

    /**
     * Translated "Breakpoints not supported in Plumber API files.".
     *
     * @return translated "Breakpoints not supported in Plumber API files."
     */
    String onBreakpointSetPlumberfileWarning();

    /**
     * Translated "Error Saving Setting".
     *
     * @return translated "Error Saving Setting"
     */
    String errorSavingSetting();

    /**
     * Translated "Save File".
     *
     * @return translated "Save File"
     */
    String saveFile();

    /**
     * Translated "Installing TinyTeX".
     *
     * @return translated "Installing TinyTeX"
     */
    String installTinyTeX();

    /**
     * Translated "Installing tinytex".
     *
     * @return translated "Installing tinytex"
     */
    String installTinytexLowercase();

    /**
     * Translated "Debug lines may not match because the file contains unsaved changes.".
     *
     * @return translated "Debug lines may not match because the file contains unsaved changes."
     */
    String updateDebugWarningBarMessage();

    /**
     * Translated "Work on a Copy".
     *
     * @return translated "Work on a Copy"
     */
    String beginQueuedCollabSessionNoLabel();

    /**
     * Translated "Discard and Join".
     *
     * @return translated "Discard and Join"
     */
    String beginQueuedCollabSessionYesLabel();

    /**
     * Translated "You have unsaved changes to {0}, but another user is editing the file. Do you want to discard your changes and join their edit session, or make your own copy of the file to work on?".
     *
     * @return translated "You have unsaved changes to {0}, but another user is editing the file. Do you want to discard your changes and join their edit session, or make your own copy of the file to work on?"
     */
    String beginQueuedCollabSessionMessage(String filename);

    /**
     * Translated "Join Edit Session".
     *
     * @return translated "Join Edit Session"
     */
    String beginQueuedCollabSessionCaption();

    /**
     * Translated "Breakpoints can only be set inside the body of a function. ".
     *
     * @return translated "Breakpoints can only be set inside the body of a function. "
     */
    String onBreakpointsSavedWarningBar();

    /**
     * Translated "{0}-copy{1}".
     *
     * @return translated "{0}-copy{1}"
     */
    String saveAsPathName(String stem, String extension);

    /**
     * Translated "This source file is read-only so changes cannot be saved".
     *
     * @return translated "This source file is read-only so changes cannot be saved"
     */
    String onResponseReceivedMessage();

    /**
     * Translated "Error Saving File".
     *
     * @return translated "Error Saving File"
     */
    String errorSavingFile();

    /**
     * Translated "The process cannot access the file because it is being used by another process".
     *
     * @return translated "The process cannot access the file because it is being used by another process"
     */
    String onErrorMessage();

    /**
     * Translated "{0} is configured to weave {1} however the {2} package is not installed.".
     *
     * @return translated "{0} is configured to weave {1} however the {2} package is not installed."
     */
    String checkCompilersRnWPackageNotInstalled(String fRnWname, String forcontext, String fRnWPackage);

    /**
     * Translated "Rnw files".
     *
     * @return translated "Rnw files"
     */
    String rnwFiles();

    /**
     * Translated "Rnw files for this project".
     *
     * @return translated "Rnw files for this project"
     */
    String rnwFilesForProject();

    /**
     * Translated "this file".
     *
     * @return translated "this file"
     */
    String thisFile();

    /**
     * Translated "This server does not have LaTeX installed. You may not be able to compile.".
     *
     * @return translated "This server does not have LaTeX installed. You may not be able to compile."
     */
    String checkCompilersServerWarning();

    /**
     * Translated "No LaTeX installation detected. Please install LaTeX before compiling.".
     *
     * @return translated "No LaTeX installation detected. Please install LaTeX before compiling."
     */
    String checkCompilersDesktopWarning();

    /**
     * Translated "Unknown Rnw weave method ''{0}'' specified (valid types are {1})".
     *
     * @return translated "Unknown Rnw weave method ''{0}'' specified (valid types are {1})"
     */
    String checkCompilersRnWWeaveTypeError(String directiveName, String typeNames);

    /**
     * Translated "Unknown LaTeX program type ''{0}'' specified (valid types are {1})".
     *
     * @return translated "Unknown LaTeX program type ''{0}'' specified (valid types are {1})"
     */
    String checkCompilersUnknownLatexType(String latexProgramDirective, String typeNames);

    /**
     * Translated "Finding usages...".
     *
     * @return translated "Finding usages..."
     */
    String findingUsages();

    /**
     * Translated "The Rcpp package (version 0.10.1 or higher) is not currently installed".
     *
     * @return translated "The Rcpp package (version 0.10.1 or higher) is not currently installed"
     */
    String checkBuildCppDependenciesRcppPackage();

    /**
     * Translated "The tools required to build C/C++ code for R are not currently installed".
     *
     * @return translated "The tools required to build C/C++ code for R are not currently installed"
     */
    String checkBuildCppDependenciesToolsNotInstalled();

    /**
     * Translated "Find".
     *
     * @return translated "Find"
     */
    String find();

    /**
     * Translated "Find/Replace".
     *
     * @return translated "Find/Replace"
     */
    String findOrReplace();

    /**
     * Translated "''{0}'' is not a known previewer for JavaScript files. Did you mean ''r2d3''?".
     *
     * @return translated "''{0}'' is not a known previewer for JavaScript files. Did you mean ''r2d3''?"
     */
    String previewJSErrorMessage(String functionString);

    /**
     * Translated "Error Previewing JavaScript".
     *
     * @return translated "Error Previewing JavaScript"
     */
    String previewJSErrorCaption();

    /**
     * Translated "Block Quote".
     *
     * @return translated "Block Quote"
     */
    String blockQuote();

    /**
     * Translated "Verbatim".
     *
     * @return translated "Verbatim"
     */
    String verbatim();

    /**
     * Translated "Description List".
     *
     * @return translated "Description List"
     */
    String descriptionList();

    /**
     * Translated "Numbered List".
     *
     * @return translated "Numbered List"
     */
    String numberedList();

    /**
     * Translated "Bullet List".
     *
     * @return translated "Bullet List"
     */
    String bulletList();

    /**
     * Translated "Quote".
     *
     * @return translated "Quote"
     */
    String quote();

    /**
     * Translated "Typewriter".
     *
     * @return translated "Typewriter"
     */
    String typewriter();

    /**
     * Translated "Italic".
     *
     * @return translated "Italic"
     */
    String italic();

    /**
     * Translated "Bold".
     *
     * @return translated "Bold"
     */
    String bold();

    /**
     * Translated "Sub-Subsection".
     *
     * @return translated "Sub-Subsection"
     */
    String subSubsection();

    /**
     * Translated "Subsection".
     *
     * @return translated "Subsection"
     */
    String subsection();

    /**
     * Translated "Section".
     *
     * @return translated "Section"
     */
    String section();

    /**
     * Translated "(Untitled Slide)".
     *
     * @return translated "(Untitled Slide)"
     */
    String untitledSlide();

    /**
     * Translated "(No Slides)".
     *
     * @return translated "(No Slides)"
     */
    String noSlides();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    String rMarkdown();

    /**
     * Translated "R Notebook".
     *
     * @return translated "R Notebook"
     */
    String rNotebook();

    /**
     * Translated "Overwrite {0}".
     *
     * @return translated "Overwrite {0}"
     */
    String createDraftFromTemplateCaption(String fileType);

    /**
     * Translated "{0} exists. Overwrite it?".
     *
     * @return translated "{0} exists. Overwrite it?"
     */
    String createDraftFromTemplateMessage(String name);

    /**
     * Translated "Overwrite".
     *
     * @return translated "Overwrite"
     */
    String overwrite();

    /**
     * Translated "Template Creation Failed".
     *
     * @return translated "Template Creation Failed"
     */
    String getTemplateContentErrorCaption();

    /**
     * Translated "Failed to load content from the template at {0}: {1}".
     *
     * @return translated "Failed to load content from the template at {0}: {1}"
     */
    String getTemplateContentErrorMessage(String templatePath, String errorMessage);

    /**
     * Translated "R Session Busy".
     *
     * @return translated "R Session Busy"
     */
    String getRMarkdownParamsFileCaption();

    /**
     * Translated "Unable to edit parameters (the R session is currently busy).".
     *
     * @return translated "Unable to edit parameters (the R session is currently busy)."
     */
    String getRMarkdownParamsFileMessage();

    /**
     * Translated "File Remove Failed".
     *
     * @return translated "File Remove Failed"
     */
    String cleanAndCreateTemplateCaption();

    /**
     * Translated "Couldn''t remove {0}".
     *
     * @return translated "Couldn''t remove {0}"
     */
    String cleanAndCreateTemplateMessage(String path);

    /**
     * Translated "Creating R Markdown Document...".
     *
     * @return translated "Creating R Markdown Document..."
     */
    String createDraftFromTemplateProgressMessage();

    /**
     * Translated "Couldn''t create a template from {0} at {1}.\n\n{2}".
     *
     * @return translated "Couldn''t create a template from {0} at {1}.\n\n{2}"
     */
    String createDraftFromTemplateOnError(String templatePath, String target, String errorMessage);

    /**
     * Translated "{0} requires the knitr package (version {1} or higher)".
     *
     * @return translated "{0} requires the knitr package (version {1} or higher)"
     */
    String showKnitrPreviewWarningBar(String feature, String requiredVersion);

    /**
     * Translated "Spellcheck".
     *
     * @return translated "Spellcheck"
     */
    String spellcheck();

    /**
     * Translated "''{0}'' is misspelled".
     *
     * @return translated "''{0}'' is misspelled"
     */
    String wordIsMisspelled(String word);
    
    /**
     * Translated "Ignore word".
     *
     * @return translated "Ignore word"
     */
    String ignoreWord();

    /**
     * Translated "Add to user dictionary".
     *
     * @return translated "Add to user dictionary"
     */
    String addToUserDictionary();

    /**
     * Translated "Error Previewing SQL".
     *
     * @return translated "Error Previewing SQL"
     */
    String errorPreviewingSql();

    /**
     * Translated "Source on Save".
     *
     * @return translated "Source on Save"
     */
    String sourceOnSave();

    /**
     * Translated "Text editor".
     *
     * @return translated "Text editor"
     */
    String textEditor();

    /**
     * Translated "Compare Results".
     *
     * @return translated "Compare Results"
     */
    String compareResults();

    /**
     * Translated "Run Tests".
     *
     * @return translated "Run Tests"
     */
    String runTests();

    /**
     * Translated "Compile Report ({0})".
     *
     * @return translated "Compile Report ({0})"
     */
    String compileReport(String cmdtext);

    /**
     * Translated "Shiny test options".
     *
     * @return translated "Shiny test options"
     */
    String shinyTestOptions();

    /**
     * Translated "Knit options".
     *
     * @return translated "Knit options"
     */
    String knitOptions();

    /**
     * Translated "Run document options".
     *
     * @return translated "Run document options"
     */
    String runDocumentOptions();

    /**
     * Translated "Source the active document".
     *
     * @return translated "Source the active document"
     */
    String sourceButtonTitle();

    /**
     * Translated "Source".
     *
     * @return translated "Source"
     */
    String source();

    /**
     * Translated "{0} (with echo)".
     *
     * @return translated "{0} (with echo)"
     */
    String sourceButtonTitleWithEcho(String title);

    /**
     * Translated "Source options".
     *
     * @return translated "Source options"
     */
    String sourceOptions();

    /**
     * Translated "Run Setup Chunk Automatically".
     *
     * @return translated "Run Setup Chunk Automatically"
     */
    String runSetupChunkAuto();

    /**
     * Translated "Run".
     *
     * @return translated "Run"
     */
    String run();

    /**
     * Translated "Run app options".
     *
     * @return translated "Run app options"
     */
    String runAppOptions();

    /**
     * Translated "Run API options".
     *
     * @return translated "Run API options"
     */
    String runApiOptions();

    /**
     * Translated "Show ".
     *
     * @return translated "Show "
     */
    String show();

    /**
     * Translated "Hide ".
     *
     * @return translated "Hide "
     */
    String hide();

    /**
     * Translated "Show whitespace".
     *
     * @return translated "Show whitespace"
     */
    String showWhitespace();

    /**
     * Translated "Format".
     *
     * @return translated "Format"
     */
    String format();

    /**
     * Translated "Code Tools".
     *
     * @return translated "Code Tools"
     */
    String codeTools();

    /**
     * Translated "{0} on Save".
     *
     * @return translated "{0} on Save"
     */
    String actionOnSave(String action);

    /**
     * Translated "Untitled Text editor".
     *
     * @return translated "Untitled Text editor"
     */
    String untitledTextEditor();

    /**
     * Translated "Compile PDF".
     *
     * @return translated "Compile PDF"
     */
    String compilePdf();

    /**
     * Translated "R Script".
     *
     * @return translated "R Script"
     */
    String rScript();

    /**
     * Translated "Install {0} dependencies".
     *
     * @return translated "Install {0} dependencies"
     */
    String showRequiredPackagesMissingWarningCaption(String scriptname);

    /**
     * Translated "Preview".
     *
     * @return translated "Preview"
     */
    String preview();

    /**
     * Translated "Knit to ".
     *
     * @return translated "Knit to "
     */
    String knitTo();

    /**
     * Translated "Preview Notebook".
     *
     * @return translated "Preview Notebook"
     */
    String previewNotebook();

    /**
     * Translated "Document Directory".
     *
     * @return translated "Document Directory"
     */
    String documentDirectory();

    /**
     * Translated "Project Directory".
     *
     * @return translated "Project Directory"
     */
    String projectDirectory();

    /**
     * Translated "Current Working Directory".
     *
     * @return translated "Current Working Directory"
     */
    String currentWorkingDirectory();

    /**
     * Translated "Knit Directory".
     *
     * @return translated "Knit Directory"
     */
    String knitDirectory();

    /**
     * Translated "Render {0}".
     *
     * @return translated "Render {0}"
     */
    String renderFormatName(String formatName);

    /**
     * Translated "Presentation".
     *
     * @return translated "Presentation"
     */
    String presentation();

    /**
     * Translated "Document".
     *
     * @return translated "Document"
     */
    String document();

    /**
     * Translated "Run {0}".
     *
     * @return translated "Run {0}"
     */
    String setIsShinyFormatKnitCommandText(String docType);

    /**
     * Translated "View the current {0} with Shiny ({1})".
     *
     * @return translated "View the current {0} with Shiny ({1})"
     */
    String setIsShinyFormatKnitDocumentButtonTitle(String doctype, String htmlString);

    /**
     * Translated "Preview the notebook ({0})".
     *
     * @return translated "Preview the notebook ({0})"
     */
    String setIsNotebookFormatButtonTitle(String htmlString);

    /**
     * Translated "Content not publishable".
     *
     * @return translated "Content not publishable"
     */
    String invokePublishCaption();

    /**
     * Translated "This item cannot be published.".
     *
     * @return translated "This item cannot be published."
     */
    String invokePublishMessage();

    /**
     * Translated "Knit{0}".
     *
     * @return translated "Knit{0}"
     */
    String setFormatTextKnitCommandText(String text);

    /**
     * Translated "Render".
     *
     * @return translated "Render"
     */
    String setFormatTextQuartoCommandText();

    /**
     * Translated "Preview{0}".
     *
     * @return translated "Preview{0}"
     */
    String setFormatTextPreviewCommandText(String text);

    /**
     * Translated "Source Script".
     *
     * @return translated "Source Script"
     */
    String sourceScript();

    /**
     * Translated "Save changes and source the current script".
     *
     * @return translated "Save changes and source the current script"
     */
    String setSourceButtonFromScriptStatePythonDesc();

    /**
     * Translated "Run Script".
     *
     * @return translated "Run Script"
     */
    String runScript();

    /**
     * Translated "Save changes and run the current script".
     *
     * @return translated "Save changes and run the current script"
     */
    String setSourceButtonFromScriptStateDesc();

    /**
     * Translated "Save changes and preview".
     *
     * @return translated "Save changes and preview"
     */
    String setSourceButtonFromScriptStateSavePreview();

    /**
     * Translated "Reload App".
     *
     * @return translated "Reload App"
     */
    String reloadApp();

    /**
     * Translated "Save changes and reload the Shiny application".
     *
     * @return translated "Save changes and reload the Shiny application"
     */
    String saveChangesAndReload();

    /**
     * Translated "Run App".
     *
     * @return translated "Run App"
     */
    String runApp();

    /**
     * Translated "Run the Shiny application".
     *
     * @return translated "Run the Shiny application"
     */
    String runTheShinyApp();

    /**
     * Translated "Reload API".
     *
     * @return translated "Reload API"
     */
    String reloadApi();

    /**
     * Translated "Save changes and reload the Plumber API".
     *
     * @return translated "Save changes and reload the Plumber API"
     */
    String saveChangesReloadPlumberApi();

    /**
     * Translated "Run API".
     *
     * @return translated "Run API"
     */
    String runApi();

    /**
     * Translated "Run the Plumber API".
     *
     * @return translated "Run the Plumber API"
     */
    String runPlumberApi();

    /**
     * Translated "Preview in Viewer Pane".
     *
     * @return translated "Preview in Viewer Pane"
     */
    String previewInViewerPane();

    /**
     * Translated "Preview in Window".
     *
     * @return translated "Preview in Window"
     */
    String previewInWindow();

    /**
     * Translated "(No Preview)".
     *
     * @return translated "(No Preview)"
     */
    String noPreviewParentheses();

    /**
     * Translated "Use Visual Editor".
     *
     * @return translated "Use Visual Editor"
     */
    String useVisualEditor();

    /**
     * Translated "Preview Images and Equations".
     *
     * @return translated "Preview Images and Equations"
     */
    String previewImagesEquations();

    /**
     * Translated "Show Previews Inline".
     *
     * @return translated "Show Previews Inline"
     */
    String showPreviewsInline();

    /**
     * Translated "Chunk Output Inline".
     *
     * @return translated "Chunk Output Inline"
     */
    String chunkOutputInline();

    /**
     * Translated "Chunk Output in Console".
     *
     * @return translated "Chunk Output in Console"
     */
    String chunkOutputInConsole();

    /**
     * Translated "Knit".
     *
     * @return translated "Knit"
     */
    String knit();

    /**
     * Translated "Render".
     *
     * @return translated "Render"
     */
    String render();

    /**
     * Translated "Print Frame".
     *
     * @return translated "Print Frame"
     */
    String printFrame();

    /**
     * Translated "visual".
     *
     * @return translated "visual"
     */
    String visual();

    /**
     * Translated "Markdown editing tools".
     *
     * @return translated "Markdown editing tools"
     */
    String markdownEditingTools();

    /**
     * Translated "Compiling C/C++ code for R".
     *
     * @return translated "Compiling C/C++ code for R"
     */
    String compilingCode();

    /**
     * Translated "Running shiny documents".
     *
     * @return translated "Running shiny documents"
     */
    String runningShinyDocuments();

    /**
     * Translated "Compiling notebooks from R scripts".
     *
     * @return translated "Compiling notebooks from R scripts"
     */
    String compilingNotebooks();

    /**
     * Translated "Rendering R Markdown documents".
     *
     * @return translated "Rendering R Markdown documents"
     */
    String renderingR();

    /**
     * Translated "Specifying Knit parameters".
     *
     * @return translated "Specifying Knit parameters"
     */
    String specifyingKnit();

    /**
     * Translated "Creating R Markdown documents".
     *
     * @return translated "Creating R Markdown documents"
     */
    String creatingRMarkdown();

    /**
     * Translated "Copilot: Waiting for completions..."
     *
     * @return translated "Copilot: Waiting for completions..."
     */
    String copilotWaiting();

    /**
     * Translated "Copilot: No completions available."
     *
     * @return translated "Copilot: No completions available."
     */
    String copilotNoCompletions();

    /**
     * Translated "Copilot: Completion response received."
     *
     * @return translated "Copilot: Completion response received."
     */
    String copilotResponseReceived();

    /**
     * Translated "Copilot: {0}"
     *
     * @return translated "Copilot: {0}"
     */
    String copilotResponseErrorMessage(String message);

    /**
     * Translated "Copilot: Automatic completions have been enabled."
     *
     * @return translated "Copilot: Automatic completions have been enabled."
     */
    String copilotEnabled();

    /**
     * Translated "Copilot: Automatic completions have been disabled."
     *
     * @return translated "Copilot: Automatic completions have been disabled."
     */
    String copilotDisabled();
    
    /**
     * Translated "Reformat Document on Save"
     *
     * @return translated "Reformat Document on Save"
     */
    String reformatDocumentOnSave();
    
}

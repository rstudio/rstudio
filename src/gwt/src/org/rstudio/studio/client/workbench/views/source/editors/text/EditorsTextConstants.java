/*
 * EditorsTextConstants.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

public interface EditorsTextConstants extends com.google.gwt.i18n.client.Messages {
    /**
     * Translated "R Console".
     *
     * @return translated "R Console"
     */
    @DefaultMessage("R Console")
    @Key("rConsole")
    String rConsole();

    /**
     * Translated "Chunk Feedback".
     *
     * @return translated "Chunk Feedback"
     */
    @DefaultMessage("Chunk Feedback")
    @Key("chunkFeedback")
    String chunkFeedback();

    /**
     * Translated "Chunk HTML Page Output Frame".
     *
     * @return translated "Chunk HTML Page Output Frame"
     */
    @DefaultMessage("Chunk HTML Page Output Frame")
    @Key("chunkHtmlPageOutputFrame")
    String chunkHtmlPageOutputFrame();

    /**
     * Translated "Chunk HTML Output Frame".
     *
     * @return translated "Chunk HTML Output Frame"
     */
    @DefaultMessage("Chunk HTML Output Frame")
    @Key("chunkHtmlOutputFrame")
    String chunkHtmlOutputFrame();

    /**
     * Translated "RStudio: Notebook Output".
     *
     * @return translated "RStudio: Notebook Output"
     */
    @DefaultMessage("RStudio: Notebook Output")
    @Key("chunkSatelliteWindowInitTitle")
    String chunkSatelliteWindowInitTitle();

    /**
     * Translated "(No image at path {0})".
     *
     * @return translated "(No image at path {0})"
     */
    @DefaultMessage("(No image at path {0})")
    @Key("noImageLabel")
    String noImageLabel(String path);

    /**
     * Translated "No image at path {0}".
     *
     * @return translated "No image at path {0}"
     */
    @DefaultMessage("No image at path {0}")
    @Key("noImageLabelNoParentheses")
    String noImageLabelNoParentheses(String path);

    /**
     * Translated "Double-Click to Zoom".
     *
     * @return translated "Double-Click to Zoom"
     */
    @DefaultMessage("Double-Click to Zoom")
    @Key("doubleClickToZoom")
    String doubleClickToZoom();

    /**
     * Translated "The selected code could not be parsed.\n\nAre you sure you want to continue?".
     *
     * @return translated "The selected code could not be parsed.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("The selected code could not be parsed.\n\nAre you sure you want to continue?")
    @Key("refactorServerRequestCallbackError")
    String refactorServerRequestCallbackError();

    /**
     * Translated "Failed to check if results are available".
     *
     * @return translated "Failed to check if results are available"
     */
    @DefaultMessage("Failed to check if results are available")
    @Key("onShinyCompareTestError")
    String onShinyCompareTestError();

    /**
     * Translated "No Failed Results".
     *
     * @return translated "No Failed Results"
     */
    @DefaultMessage("No Failed Results")
    @Key("onShinyCompareTestResponseCaption")
    String onShinyCompareTestResponseCaption();

    /**
     * Translated "There are no failed tests to compare.".
     *
     * @return translated "There are no failed tests to compare."
     */
    @DefaultMessage("There are no failed tests to compare.")
    @Key("onShinyCompareTestResponseMessage")
    String onShinyCompareTestResponseMessage();

    /**
     * Translated "Failed to check for additional dependencies".
     *
     * @return translated "Failed to check for additional dependencies"
     */
    @DefaultMessage("Failed to check for additional dependencies")
    @Key("checkTestPackageDependenciesError")
    String checkTestPackageDependenciesError();

    /**
     * Translated "The package shinytest requires additional components to run.\n\nInstall additional components?".
     *
     * @return translated "The package shinytest requires additional components to run.\n\nInstall additional components?"
     */
    @DefaultMessage("The package shinytest requires additional components to run.\\n\\nInstall additional components?")
    @Key("checkTestPackageDependenciesMessage")
    String checkTestPackageDependenciesMessage();

    /**
     * Translated "Install Shinytest Dependencies".
     *
     * @return translated "Install Shinytest Dependencies"
     */
    @DefaultMessage("Install Shinytest Dependencies")
    @Key("checkTestPackageDependenciesCaption")
    String checkTestPackageDependenciesCaption();

    /**
     * Translated "Failed to install additional dependencies".
     *
     * @return translated "Failed to install additional dependencies"
     */
    @DefaultMessage("Failed to install additional dependencies")
    @Key("installShinyTestDependenciesError")
    String installShinyTestDependenciesError();

    /**
     * Translated "The file {0} has changed on disk. Do you want to reload the file from disk and discard your unsaved changes?".
     *
     * @return translated "The file {0} has changed on disk. Do you want to reload the file from disk and discard your unsaved changes?"
     */
    @DefaultMessage("The file {0} has changed on disk. Do you want to reload the file from disk and discard your unsaved changes?")
    @Key("checkForExternalEditFileChangedMessage")
    String checkForExternalEditFileChangedMessage(String fileName);

    /**
     * Translated "File Changed".
     *
     * @return translated "File Changed"
     */
    @DefaultMessage("File Changed")
    @Key("checkForExternalEditFileChangedCaption")
    String checkForExternalEditFileChangedCaption();

    /**
     * Translated "The file {0} has been deleted or moved. Do you want to close this file now?".
     *
     * @return translated "The file {0} has been deleted or moved. Do you want to close this file now?"
     */
    @DefaultMessage("The file {0} has been deleted or moved. Do you want to close this file now?")
    @Key("checkForExternalEditFileDeletedMessage")
    String checkForExternalEditFileDeletedMessage(String pathName);

    /**
     * Translated "File Deleted".
     *
     * @return translated "File Deleted"
     */
    @DefaultMessage("File Deleted")
    @Key("checkForExternalEditFileDeletedCaption")
    String checkForExternalEditFileDeletedCaption();

    /**
     * Translated "The file ''{0}'' cannot be compiled to a PDF because TeX does not understand paths with spaces. If you rename the file to remove spaces then PDF compilation will work correctly.".
     *
     * @return translated "The file ''{0}'' cannot be compiled to a PDF because TeX does not understand paths with spaces. If you rename the file to remove spaces then PDF compilation will work correctly."
     */
    @DefaultMessage("The file ''{0}'' cannot be compiled to a PDF because TeX does not understand paths with spaces. If you rename the file to remove spaces then PDF compilation will work correctly.")
    @Key("fireCompilePdfEventErrorMessage")
    String fireCompilePdfEventErrorMessage(String fileName);

    /**
     * Translated "Invalid Filename".
     *
     * @return translated "Invalid Filename"
     */
    @DefaultMessage("Invalid Filename")
    @Key("fireCompilePdfEventErrorCaption")
    String fireCompilePdfEventErrorCaption();

    /**
     * Translated "This will remove all previously generated output for {0} (html, prerendered data, knitr cache, etc.).\n\nAre you sure you want to clear the output now?".
     *
     * @return translated "This will remove all previously generated output for {0} (html, prerendered data, knitr cache, etc.).\n\nAre you sure you want to clear the output now?"
     */
    @DefaultMessage("This will remove all previously generated output for {0} (html, prerendered data, knitr cache, etc.).\n\nAre you sure you want to clear the output now?")
    @Key("onClearPrerenderedOutputMessage")
    String onClearPrerenderedOutputMessage(String docPath);

    /**
     * Translated "Clear Prerendered Output".
     *
     * @return translated "Clear Prerendered Output"
     */
    @DefaultMessage("Clear Prerendered Output")
    @Key("onClearPrerenderedOutputCaption")
    String onClearPrerenderedOutputCaption();

    /**
     * Translated "Clearing the Knitr cache will delete the cache directory for {0}. \n\nAre you sure you want to clear the cache now?".
     *
     * @return translated "Clearing the Knitr cache will delete the cache directory for {0}. \n\nAre you sure you want to clear the cache now?"
     */
    @DefaultMessage("Clearing the Knitr cache will delete the cache directory for {0}. \n\nAre you sure you want to clear the cache now?")
    @Key("onClearKnitrCacheMessage")
    String onClearKnitrCacheMessage(String docPath);

    /**
     * Translated "Clear Knitr Cache".
     *
     * @return translated "Clear Knitr Cache"
     */
    @DefaultMessage("Clear Knitr Cache")
    @Key("onClearKnitrCacheCaption")
    String onClearKnitrCacheCaption();

    /**
     * Translated "Unable to Compile Report".
     *
     * @return translated "Unable to Compile Report"
     */
    @DefaultMessage("Unable to Compile Report")
    @Key("generateNotebookCaption")
    String generateNotebookCaption();

    /**
     * Translated "R Presentations require the knitr package (version 1.2 or higher)".
     *
     * @return translated "R Presentations require the knitr package (version 1.2 or higher)"
     */
    @DefaultMessage("R Presentations require the knitr package (version 1.2 or higher)")
    @Key("previewRpresentationMessage")
    String previewRpresentationMessage();

    /**
     * Translated "Unable to Preview".
     *
     * @return translated "Unable to Preview"
     */
    @DefaultMessage("Unable to Preview")
    @Key("previewRpresentationCaption")
    String previewRpresentationCaption();

    /**
     * Translated "Sourcing Python scripts".
     *
     * @return translated "Sourcing Python scripts"
     */
    @DefaultMessage("Sourcing Python scripts")
    @Key("sourcePythonUserPrompt")
    String sourcePythonUserPrompt();

    /**
     * Translated "Executing Python".
     *
     * @return translated "Executing Python"
     */
    @DefaultMessage("Executing Python")
    @Key("sourcePythonProgressCaption")
    String sourcePythonProgressCaption();

    /**
     * Translated "The currently active source file is not saved so doesn''t have a directory to change into.".
     *
     * @return translated "The currently active source file is not saved so doesn''t have a directory to change into."
     */
    @DefaultMessage("The currently active source file is not saved so doesn''t have a directory to change into.")
    @Key("onSetWorkingDirToActiveDocMessage")
    String onSetWorkingDirToActiveDocMessage();

    /**
     * Translated "Source File Not Saved".
     *
     * @return translated "Source File Not Saved"
     */
    @DefaultMessage("Source File Not Saved")
    @Key("onSetWorkingDirToActiveDocCaption")
    String onSetWorkingDirToActiveDocCaption();

    /**
     * Translated "Enter line number:".
     *
     * @return translated "Enter line number:"
     */
    @DefaultMessage("Enter line number:")
    @Key("onGoToLineLabel")
    String onGoToLineLabel();

    /**
     * Translated "Go to Line".
     *
     * @return translated "Go to Line"
     */
    @DefaultMessage("Go to Line")
    @Key("onGoToLineTitle")
    String onGoToLineTitle();

    /**
     * Translated "{0} File name ".
     * Has to be empty string as an argument to include the space
     * @return translated "{0} File name "
     */
    @DefaultMessage("{0} File name ")
    @Key("getCurrentStatusFileName")
    String getCurrentStatusFileName(String emptyString);

    /**
     * Translated "{0} File type".
     * Has to be empty string as an argument to include the space
     * @return translated "{0} File type"
     */
    @DefaultMessage("{0} File type")
    @Key("getCurrentStatusFileType")
    String getCurrentStatusFileType(String emptyString);

    /**
     * Translated "{0} Scope".
     *
     * @return translated "{0} Scope"
     */
    @DefaultMessage("{0} Scope")
    @Key("getCurrentStatusScope")
    String getCurrentStatusScope(String emptyString);

    /**
     * Translated "{0} Column".
     *
     * @return translated "{0} Column"
     */
    @DefaultMessage("{0} Column")
    @Key("getCurrentStatusColumn")
    String getCurrentStatusColumn(String emptyString);

    /**
     * Translated "Row".
     *
     * @return translated "Row"
     */
    @DefaultMessage("Row")
    @Key("getCurrentStatusRow")
    String getCurrentStatusRow();

    /**
     * Translated "No name".
     *
     * @return translated "No name"
     */
    @DefaultMessage("No name")
    @Key("noName")
    String noName();

    /**
     * Translated "None".
     *
     * @return translated "None"
     */
    @DefaultMessage("None")
    @Key("none")
    String none();

    /**
     * Translated "Run After".
     *
     * @return translated "Run After"
     */
    @DefaultMessage("Run After")
    @Key("runAfter")
    String runAfter();

    /**
     * Translated "Run Previous".
     *
     * @return translated "Run Previous"
     */
    @DefaultMessage("Run Previous")
    @Key("runPrevious")
    String runPrevious();

    /**
     * Translated "Run All".
     *
     * @return translated "Run All"
     */
    @DefaultMessage("Run All")
    @Key("runAll")
    String runAll();

    /**
     * Translated "Section label:".
     *
     * @return translated "Section label:"
     */
    @DefaultMessage("Section label:")
    @Key("onInsertSectionLabel")
    String onInsertSectionLabel();

    /**
     * Translated "Insert Section".
     *
     * @return translated "Insert Section"
     */
    @DefaultMessage("Insert Section")
    @Key("onInsertSectionTitle")
    String onInsertSectionTitle();

    /**
     * Translated "Couldn''t determine the format options from the YAML front matter. Make sure the YAML defines a supported output format in its ''output'' field.".
     *
     * @return translated "Couldn''t determine the format options from the YAML front matter. Make sure the YAML defines a supported output format in its ''output'' field."
     */
    @DefaultMessage("Couldn''t determine the format options from the YAML front matter. Make sure the YAML defines a supported output format in its ''output'' field.")
    @Key("showFrontMatterEditorDialogMessage")
    String showFrontMatterEditorDialogMessage();

    /**
     * Translated "Edit Format Failed".
     *
     * @return translated "Edit Format Failed"
     */
    @DefaultMessage("Edit Format Failed")
    @Key("showFrontMatterEditorDialogCaption")
    String showFrontMatterEditorDialogCaption();

    /**
     * Translated "The YAML front matter in this document could not be successfully parsed. This parse error needs to be resolved before format options can be edited.".
     *
     * @return translated "The YAML front matter in this document could not be successfully parsed. This parse error needs to be resolved before format options can be edited."
     */
    @DefaultMessage("The YAML front matter in this document could not be successfully parsed. This parse error needs to be resolved before format options can be edited.")
    @Key("showFrontMatterEditorErrMsg")
    String showFrontMatterEditorErrMsg();

    /**
     * Translated "Edit Format Failed".
     *
     * @return translated "Edit Format Failed"
     */
    @DefaultMessage("Edit Format Failed")
    @Key("showFrontMatterEditorErrCaption")
    String showFrontMatterEditorErrCaption();

    /**
     * Translated "Can''t find the YAML front matter for this document. Make sure the front matter is enclosed by lines containing only three dashes: ---.".
     *
     * @return translated "Can''t find the YAML front matter for this document. Make sure the front matter is enclosed by lines containing only three dashes: ---."
     */
    @DefaultMessage("Can''t find the YAML front matter for this document. Make sure the front matter is enclosed by lines containing only three dashes: ---.")
    @Key("showFrontMatterEditorMessage")
    String showFrontMatterEditorMessage();

    /**
     * Translated "Function Name".
     *
     * @return translated "Function Name"
     */
    @DefaultMessage("Function Name")
    @Key("functionNameLabel")
    String functionNameLabel();

    /**
     * Translated "Please select the code to extract into a function.".
     *
     * @return translated "Please select the code to extract into a function."
     */
    @DefaultMessage("Please select the code to extract into a function.")
    @Key("pleaseSelectCodeMessage")
    String pleaseSelectCodeMessage();

    /**
     * Translated "Extract Function".
     *
     * @return translated "Extract Function"
     */
    @DefaultMessage("Extract Function")
    @Key("extractActiveFunctionRefactoringName")
    String extractActiveFunctionRefactoringName();

    /**
     * Translated "The {0} command is only valid for R code chunks.".
     *
     * @return translated "The {0} command is only valid for R code chunks."
     */
    @DefaultMessage("The {0} command is only valid for R code chunks.")
    @Key("showRModeWarningMessage")
    String showRModeWarningMessage(String command);

    /**
     * Translated "Command Not Available".
     *
     * @return translated "Command Not Available"
     */
    @DefaultMessage("Command Not Available")
    @Key("showRModeWarningCaption")
    String showRModeWarningCaption();

    /**
     * Translated "Variable Name".
     *
     * @return translated "Variable Name"
     */
    @DefaultMessage("Variable Name")
    @Key("variableName")
    String variableName();

    /**
     * Translated "Extract local variable".
     *
     * @return translated "Extract local variable"
     */
    @DefaultMessage("Extract local variable")
    @Key("extractLocalVariableRefactoringName")
    String extractLocalVariableRefactoringName();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("cancel")
    String cancel();

    /**
     * Translated "Reopen Document".
     *
     * @return translated "Reopen Document"
     */
    @DefaultMessage("Reopen Document")
    @Key("reopenDocument")
    String reopenDocument();

    /**
     * Translated "This document has unsaved changes. These changes will be discarded when re-opening the document.\n\nWould you like to proceed?".
     *
     * @return translated "This document has unsaved changes. These changes will be discarded when re-opening the document.\n\nWould you like to proceed?"
     */
    @DefaultMessage("This document has unsaved changes. These changes will be discarded when re-opening the document.\n\nWould you like to proceed?")
    @Key("onReopenSourceDocWithEncodingMessage")
    String onReopenSourceDocWithEncodingMessage();

    /**
     * Translated "Reopen with Encoding".
     *
     * @return translated "Reopen with Encoding"
     */
    @DefaultMessage("Reopen with Encoding")
    @Key("onReopenSourceDocWithEncodingCaption")
    String onReopenSourceDocWithEncodingCaption();

    /**
     * Translated "Total words: {0} {1}".
     *
     * @return translated "Total words: {0} {1}"
     */
    @DefaultMessage("Total words: {0} {1}")
    @Key("onWordCountMessage")
    String onWordCountMessage(int totalWords, String selectedWordsText);

    /**
     * Translated "Word Count".
     *
     * @return translated "Word Count"
     */
    @DefaultMessage("Word Count")
    @Key("wordCount")
    String wordCount();

    /**
     * Translated "\nSelected words: {0}".
     *
     * @return translated "\nSelected words: {0}"
     */
    @DefaultMessage("\nSelected words: {0}")
    @Key("selectedWords")
    String selectedWords(int selectionWords);

    /**
     * Translated "for {0}.".
     *
     * @return translated "for {0}."
     */
    @DefaultMessage("for {0}.")
    @Key("renameInScopeSelectedItemMessage")
    String renameInScopeSelectedItemMessage(String selectedItem);

    /**
     * Translated "matches ".
     *
     * @return translated "matches "
     */
    @DefaultMessage("matches ")
    @Key("renameInScopeMatchesPlural")
    String renameInScopeMatchesPlural();

    /**
     * Translated "match ".
     *
     * @return translated "match "
     */
    @DefaultMessage("match ")
    @Key("renameInScopeMatch")
    String renameInScopeMatch();

    /**
     * Translated "Found {0} ".
     *
     * @return translated "Found {0} "
     */
    @DefaultMessage("Found {0} ")
    @Key("renameInScopeFoundMatchesMessage")
    String renameInScopeFoundMatchesMessage(int matches);

    /**
     * Translated "No matches for ''{0}''".
     *
     * @return translated "No matches for ''{0}''"
     */
    @DefaultMessage("No matches for ''{0}''")
    @Key("renameInScopeNoMatchesMessage")
    String renameInScopeNoMatchesMessage(String selectionValue);

    /**
     * Translated "This file was created as an R script however the file extension you specified will change it into another file type that will no longer open as an R script.\n\nAre you sure you want to change the type of the file so that it is no longer an R script?".
     *
     * @return translated "This file was created as an R script however the file extension you specified will change it into another file type that will no longer open as an R script.\n\nAre you sure you want to change the type of the file so that it is no longer an R script?"
     */
    @DefaultMessage("This file was created as an R script however the file extension you specified will change it into another file type that will no longer open as an R script.\\n\\nAre you sure you want to change the type of the file so that it is no longer an R script?")
    @Key("saveNewFileWithEncodingWarningMessage")
    String saveNewFileWithEncodingWarningMessage();

    /**
     * Translated "Confirm Change File Type".
     *
     * @return translated "Confirm Change File Type"
     */
    @DefaultMessage("Confirm Change File Type")
    @Key("saveNewFileWithEncodingWarningCaption")
    String saveNewFileWithEncodingWarningCaption();

    /**
     * Translated "Save File - {0}".
     *
     * @return translated "Save File - {0}"
     */
    @DefaultMessage("Save File - {0}")
    @Key("saveNewFileWithEncodingSaveFileCaption")
    String saveNewFileWithEncodingSaveFileCaption(String nameValue);

    /**
     * Translated "Don''t Save".
     *
     * @return translated "Don''t Save"
     */
    @DefaultMessage("Don''t Save")
    @Key("dontSave")
    String dontSave();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    @DefaultMessage("Save")
    @Key("save")
    String save();

    /**
     * Translated "The document ''{0}'' has unsaved changes.\n\nDo you want to save these changes?".
     *
     * @return translated "The document ''{0}'' has unsaved changes.\n\nDo you want to save these changes?"
     */
    @DefaultMessage("The document ''{0}'' has unsaved changes.\\n\\nDo you want to save these changes?")
    @Key("saveWithPromptMessage")
    String saveWithPromptMessage(String documentName);

    /**
     * Translated "{0} - Unsaved Changes".
     *
     * @return translated "{0} - Unsaved Changes"
     */
    @DefaultMessage("{0} - Unsaved Changes")
    @Key("saveWithPromptCaption")
    String saveWithPromptCaption(String documentName);

    /**
     * Translated "Close Anyway".
     *
     * @return translated "Close Anyway"
     */
    @DefaultMessage("Close Anyway")
    @Key("closeAnyway")
    String closeAnyway();

    /**
     * Translated "You''re actively following another user''s cursor in ''{0}''.\n\nIf you close this file, you won''t see their cursor until they edit another file.".
     *
     * @return translated "You''re actively following another user''s cursor in ''{0}''.\n\nIf you close this file, you won''t see their cursor until they edit another file."
     */
    @DefaultMessage("You''re actively following another user''s cursor in ''{0}''.\n\nIf you close this file, you won''t see their cursor until they edit another file.")
    @Key("onBeforeDismissMessage")
    String onBeforeDismissMessage(String nameValue);

    /**
     * Translated "{0} - Active Following Session".
     *
     * @return translated "{0} - Active Following Session"
     */
    @DefaultMessage("{0} - Active Following Session")
    @Key("onBeforeDismissCaption")
    String onBeforeDismissCaption(String nameValue);

    /**
     * Translated "(No {0} defined)".
     *
     * @return translated "(No {0} defined)"
     */
    @DefaultMessage("(No {0} defined)")
    @Key("addFunctionsToMenuText")
    String addFunctionsToMenuText(String typeText);

    /**
     * Translated "chunks".
     *
     * @return translated "chunks"
     */
    @DefaultMessage("chunks")
    @Key("chunks")
    String chunks();

    /**
     * Translated "functions".
     *
     * @return translated "functions"
     */
    @DefaultMessage("functions")
    @Key("functions")
    String functions();

    /**
     * Translated "Breakpoints will be activated when this file is sourced.".
     *
     * @return translated "Breakpoints will be activated when this file is sourced."
     */
    @DefaultMessage("Breakpoints will be activated when this file is sourced.")
    @Key("updateBreakpointWarningBarSourcedMessage")
    String updateBreakpointWarningBarSourcedMessage();

    /**
     * Translated "Breakpoints will be activated when an updated version of the {0} package is loaded".
     *
     * @return translated "Breakpoints will be activated when an updated version of the {0} package is loaded"
     */
    @DefaultMessage("Breakpoints will be activated when an updated version of the {0} package is loaded")
    @Key("updateBreakpointWarningBarPackageMessage")
    String updateBreakpointWarningBarPackageMessage(String pendingPackageName);

    /**
     * Translated "Breakpoints will be activated when the package is built and reloaded.".
     *
     * @return translated "Breakpoints will be activated when the package is built and reloaded."
     */
    @DefaultMessage("Breakpoints will be activated when the package is built and reloaded.")
    @Key("updateBreakpointWarningBarPackageLoadMessage")
    String updateBreakpointWarningBarPackageLoadMessage();

    /**
     * Translated "Breakpoints will be activated when the file or function is finished executing.".
     *
     * @return translated "Breakpoints will be activated when the file or function is finished executing."
     */
    @DefaultMessage("Breakpoints will be activated when the file or function is finished executing.")
    @Key("updateBreakpointWarningBarFunctionMessage")
    String updateBreakpointWarningBarFunctionMessage();

    /**
     * Translated "Breakpoints cannot be set until the file is saved.".
     *
     * @return translated "Breakpoints cannot be set until the file is saved."
     */
    @DefaultMessage("Breakpoints cannot be set until the file is saved.")
    @Key("onBreakpointSetNewDocWarning")
    String onBreakpointSetNewDocWarning();

    /**
     * Translated "Breakpoints not supported in Plumber API files.".
     *
     * @return translated "Breakpoints not supported in Plumber API files."
     */
    @DefaultMessage("Breakpoints not supported in Plumber API files.")
    @Key("onBreakpointSetPlumberfileWarning")
    String onBreakpointSetPlumberfileWarning();

    /**
     * Translated "Error Saving Setting".
     *
     * @return translated "Error Saving Setting"
     */
    @DefaultMessage("Error Saving Setting")
    @Key("errorSavingSetting")
    String errorSavingSetting();

    /**
     * Translated "Save File".
     *
     * @return translated "Save File"
     */
    @DefaultMessage("Save File")
    @Key("saveFile")
    String saveFile();

    /**
     * Translated "Installing TinyTeX".
     *
     * @return translated "Installing TinyTeX"
     */
    @DefaultMessage("Installing TinyTeX")
    @Key("installTinyTeX")
    String installTinyTeX();

    /**
     * Translated "Installing tinytex".
     *
     * @return translated "Installing tinytex"
     */
    @DefaultMessage("Installing tinytex")
    @Key("installTinytexLowercase")
    String installTinytexLowercase();

    /**
     * Translated "Debug lines may not match because the file contains unsaved changes.".
     *
     * @return translated "Debug lines may not match because the file contains unsaved changes."
     */
    @DefaultMessage("Debug lines may not match because the file contains unsaved changes.")
    @Key("updateDebugWarningBarMessage")
    String updateDebugWarningBarMessage();

    /**
     * Translated "Work on a Copy".
     *
     * @return translated "Work on a Copy"
     */
    @DefaultMessage("Work on a Copy")
    @Key("beginQueuedCollabSessionNoLabel")
    String beginQueuedCollabSessionNoLabel();

    /**
     * Translated "Discard and Join".
     *
     * @return translated "Discard and Join"
     */
    @DefaultMessage("Discard and Join")
    @Key("beginQueuedCollabSessionYesLabel")
    String beginQueuedCollabSessionYesLabel();

    /**
     * Translated "You have unsaved changes to {0}, but another user is editing the file. Do you want to discard your changes and join their edit session, or make your own copy of the file to work on?".
     *
     * @return translated "You have unsaved changes to {0}, but another user is editing the file. Do you want to discard your changes and join their edit session, or make your own copy of the file to work on?"
     */
    @DefaultMessage("You have unsaved changes to {0}, but another user is editing the file. Do you want to discard your changes and join their edit session, or make your own copy of the file to work on?")
    @Key("beginQueuedCollabSessionMessage")
    String beginQueuedCollabSessionMessage(String filename);

    /**
     * Translated "Join Edit Session".
     *
     * @return translated "Join Edit Session"
     */
    @DefaultMessage("Join Edit Session")
    @Key("beginQueuedCollabSessionCaption")
    String beginQueuedCollabSessionCaption();

    /**
     * Translated "Breakpoints can only be set inside the body of a function. ".
     *
     * @return translated "Breakpoints can only be set inside the body of a function. "
     */
    @DefaultMessage("Breakpoints can only be set inside the body of a function. ")
    @Key("onBreakpointsSavedWarningBar")
    String onBreakpointsSavedWarningBar();

    /**
     * Translated "{0}-copy{1}".
     *
     * @return translated "{0}-copy{1}"
     */
    @DefaultMessage("{0}-copy{1}")
    @Key("saveAsPathName")
    String saveAsPathName(String stem, String extension);

    /**
     * Translated "This source file is read-only so changes cannot be saved".
     *
     * @return translated "This source file is read-only so changes cannot be saved"
     */
    @DefaultMessage("This source file is read-only so changes cannot be saved")
    @Key("onResponseReceivedMessage")
    String onResponseReceivedMessage();

    /**
     * Translated "Error Saving File".
     *
     * @return translated "Error Saving File"
     */
    @DefaultMessage("Error Saving File")
    @Key("errorSavingFile")
    String errorSavingFile();

    /**
     * Translated "The process cannot access the file because it is being used by another process".
     *
     * @return translated "The process cannot access the file because it is being used by another process"
     */
    @DefaultMessage("The process cannot access the file because it is being used by another process")
    @Key("onErrorMessage")
    String onErrorMessage();

    /**
     * Translated "{0} is configured to weave {1} however the {2} package is not installed.".
     *
     * @return translated "{0} is configured to weave {1} however the {2} package is not installed."
     */
    @DefaultMessage("{0} is configured to weave {1} however the {2} package is not installed.")
    @Key("checkCompilersRnWPackageNotInstalled")
    String checkCompilersRnWPackageNotInstalled(String fRnWname, String forcontext, String fRnWPackage);

    /**
     * Translated "Rnw files".
     *
     * @return translated "Rnw files"
     */
    @DefaultMessage("Rnw files")
    @Key("rnwFiles")
    String rnwFiles();

    /**
     * Translated "Rnw files for this project".
     *
     * @return translated "Rnw files for this project"
     */
    @DefaultMessage("Rnw files for this project")
    @Key("rnwFilesForProject")
    String rnwFilesForProject();

    /**
     * Translated "this file".
     *
     * @return translated "this file"
     */
    @DefaultMessage("this file")
    @Key("thisFile")
    String thisFile();

    /**
     * Translated "This server does not have LaTeX installed. You may not be able to compile.".
     *
     * @return translated "This server does not have LaTeX installed. You may not be able to compile."
     */
    @DefaultMessage("This server does not have LaTeX installed. You may not be able to compile.")
    @Key("checkCompilersServerWarning")
    String checkCompilersServerWarning();

    /**
     * Translated "No LaTeX installation detected. Please install LaTeX before compiling.".
     *
     * @return translated "No LaTeX installation detected. Please install LaTeX before compiling."
     */
    @DefaultMessage("No LaTeX installation detected. Please install LaTeX before compiling.")
    @Key("checkCompilersDesktopWarning")
    String checkCompilersDesktopWarning();

    /**
     * Translated "Unknown Rnw weave method ''{0}'' specified (valid types are {1})".
     *
     * @return translated "Unknown Rnw weave method ''{0}'' specified (valid types are {1})"
     */
    @DefaultMessage("Unknown Rnw weave method ''{0}'' specified (valid types are {1})")
    @Key("checkCompilersRnWWeaveTypeError")
    String checkCompilersRnWWeaveTypeError(String directiveName, String typeNames);

    /**
     * Translated "Unknown LaTeX program type ''{0}'' specified (valid types are {1})".
     *
     * @return translated "Unknown LaTeX program type ''{0}'' specified (valid types are {1})"
     */
    @DefaultMessage("Unknown LaTeX program type ''{0}'' specified (valid types are {1}")
    @Key("checkCompilersUnknownLatexType")
    String checkCompilersUnknownLatexType(String latexProgramDirective, String typeNames);

    /**
     * Translated "Finding usages...".
     *
     * @return translated "Finding usages..."
     */
    @DefaultMessage("Finding usages...")
    @Key("findingUsages")
    String findingUsages();

    /**
     * Translated "The Rcpp package (version 0.10.1 or higher) is not currently installed".
     *
     * @return translated "The Rcpp package (version 0.10.1 or higher) is not currently installed"
     */
    @DefaultMessage("The Rcpp package (version 0.10.1 or higher) is not currently installed")
    @Key("checkBuildCppDependenciesRcppPackage")
    String checkBuildCppDependenciesRcppPackage();

    /**
     * Translated "The tools required to build C/C++ code for R are not currently installed".
     *
     * @return translated "The tools required to build C/C++ code for R are not currently installed"
     */
    @DefaultMessage("The tools required to build C/C++ code for R are not currently installed")
    @Key("checkBuildCppDependenciesToolsNotInstalled")
    String checkBuildCppDependenciesToolsNotInstalled();

    /**
     * Translated "Find".
     *
     * @return translated "Find"
     */
    @DefaultMessage("Find")
    @Key("find")
    String find();

    /**
     * Translated "Find/Replace".
     *
     * @return translated "Find/Replace"
     */
    @DefaultMessage("Find/Replace")
    @Key("findOrReplace")
    String findOrReplace();

    /**
     * Translated "''{0}'' is not a known previewer for JavaScript files. Did you mean ''r2d3''?".
     *
     * @return translated "''{0}'' is not a known previewer for JavaScript files. Did you mean ''r2d3''?"
     */
    @DefaultMessage("''{0}'' is not a known previewer for JavaScript files. Did you mean ''r2d3''?")
    @Key("previewJSErrorMessage")
    String previewJSErrorMessage(String functionString);

    /**
     * Translated "Error Previewing JavaScript".
     *
     * @return translated "Error Previewing JavaScript"
     */
    @DefaultMessage("Error Previewing JavaScript")
    @Key("previewJSErrorCaption")
    String previewJSErrorCaption();

    /**
     * Translated "Block Quote".
     *
     * @return translated "Block Quote"
     */
    @DefaultMessage("Block Quote")
    @Key("blockQuote")
    String blockQuote();

    /**
     * Translated "Verbatim".
     *
     * @return translated "Verbatim"
     */
    @DefaultMessage("Verbatim")
    @Key("verbatim")
    String verbatim();

    /**
     * Translated "Description List".
     *
     * @return translated "Description List"
     */
    @DefaultMessage("Description List")
    @Key("descriptionList")
    String descriptionList();

    /**
     * Translated "Numbered List".
     *
     * @return translated "Numbered List"
     */
    @DefaultMessage("Numbered List")
    @Key("numberedList")
    String numberedList();

    /**
     * Translated "Bullet List".
     *
     * @return translated "Bullet List"
     */
    @DefaultMessage("Bullet List")
    @Key("bulletList")
    String bulletList();

    /**
     * Translated "Quote".
     *
     * @return translated "Quote"
     */
    @DefaultMessage("Quote")
    @Key("quote")
    String quote();

    /**
     * Translated "Typewriter".
     *
     * @return translated "Typewriter"
     */
    @DefaultMessage("Typewriter")
    @Key("typewriter")
    String typewriter();

    /**
     * Translated "Italic".
     *
     * @return translated "Italic"
     */
    @DefaultMessage("Italic")
    @Key("italic")
    String italic();

    /**
     * Translated "Bold".
     *
     * @return translated "Bold"
     */
    @DefaultMessage("Bold")
    @Key("bold")
    String bold();

    /**
     * Translated "Sub-Subsection".
     *
     * @return translated "Sub-Subsection"
     */
    @DefaultMessage("Sub-Subsection")
    @Key("subSubsection")
    String subSubsection();

    /**
     * Translated "Subsection".
     *
     * @return translated "Subsection"
     */
    @DefaultMessage("Subsection")
    @Key("subsection")
    String subsection();

    /**
     * Translated "Section".
     *
     * @return translated "Section"
     */
    @DefaultMessage("Section")
    @Key("section")
    String section();

    /**
     * Translated "(Untitled Slide)".
     *
     * @return translated "(Untitled Slide)"
     */
    @DefaultMessage("(Untitled Slide)")
    @Key("untitledSlide")
    String untitledSlide();

    /**
     * Translated "(No Slides)".
     *
     * @return translated "(No Slides)"
     */
    @DefaultMessage("(No Slides)")
    @Key("noSlides")
    String noSlides();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultMessage("R Markdown")
    @Key("rMarkdown")
    String rMarkdown();

    /**
     * Translated "R Notebook".
     *
     * @return translated "R Notebook"
     */
    @DefaultMessage("R Notebook")
    @Key("rNotebook")
    String rNotebook();

    /**
     * Translated "Overwrite {0}".
     *
     * @return translated "Overwrite {0}"
     */
    @DefaultMessage("Overwrite {0}")
    @Key("createDraftFromTemplateCaption")
    String createDraftFromTemplateCaption(String fileType);

    /**
     * Translated "{0} exists. Overwrite it?".
     *
     * @return translated "{0} exists. Overwrite it?"
     */
    @DefaultMessage("{0} exists. Overwrite it?")
    @Key("createDraftFromTemplateMessage")
    String createDraftFromTemplateMessage(String name);

    /**
     * Translated "Overwrite".
     *
     * @return translated "Overwrite"
     */
    @DefaultMessage("Overwrite")
    @Key("overwrite")
    String overwrite();

    /**
     * Translated "Template Creation Failed".
     *
     * @return translated "Template Creation Failed"
     */
    @DefaultMessage("Template Creation Failed")
    @Key("getTemplateContentErrorCaption")
    String getTemplateContentErrorCaption();

    /**
     * Translated "Failed to load content from the template at {0}: {1}".
     *
     * @return translated "Failed to load content from the template at {0}: {1}"
     */
    @DefaultMessage("Failed to load content from the template at {0}: {1}")
    @Key("getTemplateContentErrorMessage")
    String getTemplateContentErrorMessage(String templatePath, String errorMessage);

    /**
     * Translated "R Session Busy".
     *
     * @return translated "R Session Busy"
     */
    @DefaultMessage("R Session Busy")
    @Key("getRMarkdownParamsFileCaption")
    String getRMarkdownParamsFileCaption();

    /**
     * Translated "Unable to edit parameters (the R session is currently busy).".
     *
     * @return translated "Unable to edit parameters (the R session is currently busy)."
     */
    @DefaultMessage("Unable to edit parameters (the R session is currently busy).")
    @Key("getRMarkdownParamsFileMessage")
    String getRMarkdownParamsFileMessage();

    /**
     * Translated "File Remove Failed".
     *
     * @return translated "File Remove Failed"
     */
    @DefaultMessage("File Remove Failed")
    @Key("cleanAndCreateTemplateCaption")
    String cleanAndCreateTemplateCaption();

    /**
     * Translated "Couldn''t remove {0}".
     *
     * @return translated "Couldn''t remove {0}"
     */
    @DefaultMessage("Couldn''t remove {0}")
    @Key("cleanAndCreateTemplateMessage")
    String cleanAndCreateTemplateMessage(String path);

    /**
     * Translated "Creating R Markdown Document...".
     *
     * @return translated "Creating R Markdown Document..."
     */
    @DefaultMessage("Creating R Markdown Document...")
    @Key("createDraftFromTemplateProgressMessage")
    String createDraftFromTemplateProgressMessage();

    /**
     * Translated "Couldn''t create a template from {0} at {1}.\n\n{2}".
     *
     * @return translated "Couldn''t create a template from {0} at {1}.\n\n{2}"
     */
    @DefaultMessage("Couldn''t create a template from {0} at {1}.\n\n{2}")
    @Key("createDraftFromTemplateOnError")
    String createDraftFromTemplateOnError(String templatePath, String target, String errorMessage);

    /**
     * Translated "{0} requires the knitr package (version {1} or higher)".
     *
     * @return translated "{0} requires the knitr package (version {1} or higher)"
     */
    @DefaultMessage("{0} requires the knitr package (version {1} or higher)")
    @Key("showKnitrPreviewWarningBar")
    String showKnitrPreviewWarningBar(String feature, String requiredVersion);

    /**
     * Translated "Spellcheck".
     *
     * @return translated "Spellcheck"
     */
    @DefaultMessage("Spellcheck")
    @Key("spellcheck")
    String spellcheck();

    /**
     * Translated "Ignore word".
     *
     * @return translated "Ignore word"
     */
    @DefaultMessage("Ignore word")
    @Key("ignoreWord")
    String ignoreWord();

    /**
     * Translated "Add to user dictionary".
     *
     * @return translated "Add to user dictionary"
     */
    @DefaultMessage("Add to user dictionary")
    @Key("addToUserDictionary")
    String addToUserDictionary();

    /**
     * Translated "Error Previewing SQL".
     *
     * @return translated "Error Previewing SQL"
     */
    @DefaultMessage("Error Previewing SQL")
    @Key("errorPreviewingSql")
    String errorPreviewingSql();

    /**
     * Translated "Source on Save".
     *
     * @return translated "Source on Save"
     */
    @DefaultMessage("Source on Save")
    @Key("sourceOnSave")
    String sourceOnSave();

    /**
     * Translated "Text editor".
     *
     * @return translated "Text editor"
     */
    @DefaultMessage("Text editor")
    @Key("textEditor")
    String textEditor();

    /**
     * Translated "Compare Results".
     *
     * @return translated "Compare Results"
     */
    @DefaultMessage("Compare Results")
    @Key("compareResults")
    String compareResults();

    /**
     * Translated "Run Tests".
     *
     * @return translated "Run Tests"
     */
    @DefaultMessage("Run Tests")
    @Key("runTests")
    String runTests();

    /**
     * Translated "Compile Report ({0})".
     *
     * @return translated "Compile Report ({0})"
     */
    @DefaultMessage("Compile Report ({0})")
    @Key("compileReport")
    String compileReport(String cmdtext);

    /**
     * Translated "Shiny test options".
     *
     * @return translated "Shiny test options"
     */
    @DefaultMessage("Shiny test options")
    @Key("shinyTestOptions")
    String shinyTestOptions();

    /**
     * Translated "Knit options".
     *
     * @return translated "Knit options"
     */
    @DefaultMessage("Knit options")
    @Key("knitOptions")
    String knitOptions();

    /**
     * Translated "Run document options".
     *
     * @return translated "Run document options"
     */
    @DefaultMessage("Run document options")
    @Key("runDocumentOptions")
    String runDocumentOptions();

    /**
     * Translated "Source the active document".
     *
     * @return translated "Source the active document"
     */
    @DefaultMessage("Source the active document")
    @Key("sourceButtonTitle")
    String sourceButtonTitle();

    /**
     * Translated "Source".
     *
     * @return translated "Source"
     */
    @DefaultMessage("Source")
    @Key("source")
    String source();

    /**
     * Translated "{0} (with echo)".
     *
     * @return translated "{0} (with echo)"
     */
    @DefaultMessage("{0} (with echo)")
    @Key("sourceButtonTitleWithEcho")
    String sourceButtonTitleWithEcho(String title);

    /**
     * Translated "Source options".
     *
     * @return translated "Source options"
     */
    @DefaultMessage("Source options")
    @Key("sourceOptions")
    String sourceOptions();

    /**
     * Translated "Run Setup Chunk Automatically".
     *
     * @return translated "Run Setup Chunk Automatically"
     */
    @DefaultMessage("Run Setup Chunk Automatically")
    @Key("runSetupChunkAuto")
    String runSetupChunkAuto();

    /**
     * Translated "Run".
     *
     * @return translated "Run"
     */
    @DefaultMessage("Run")
    @Key("run")
    String run();

    /**
     * Translated "Run app options".
     *
     * @return translated "Run app options"
     */
    @DefaultMessage("Run app options")
    @Key("runAppOptions")
    String runAppOptions();

    /**
     * Translated "Run API options".
     *
     * @return translated "Run API options"
     */
    @DefaultMessage("Run API options")
    @Key("runApiOptions")
    String runApiOptions();

    /**
     * Translated "Show ".
     *
     * @return translated "Show "
     */
    @DefaultMessage("Show ")
    @Key("show")
    String show();

    /**
     * Translated "Hide ".
     *
     * @return translated "Hide "
     */
    @DefaultMessage("Hide ")
    @Key("hide")
    String hide();

    /**
     * Translated "Show whitespace".
     *
     * @return translated "Show whitespace"
     */
    @DefaultMessage("Show whitespace")
    @Key("showWhitespace")
    String showWhitespace();

    /**
     * Translated "Format".
     *
     * @return translated "Format"
     */
    @DefaultMessage("Format")
    @Key("format")
    String format();

    /**
     * Translated "Code Tools".
     *
     * @return translated "Code Tools"
     */
    @DefaultMessage("Veria")
    @Key("codeTools")
    String codeTools();

    /**
     * Translated "{0} on Save".
     *
     * @return translated "{0} on Save"
     */
    @DefaultMessage("{0} on Save")
    @Key("actionOnSave")
    String actionOnSave(String action);

    /**
     * Translated "Untitled Text editor".
     *
     * @return translated "Untitled Text editor"
     */
    @DefaultMessage("Untitled Text editor")
    @Key("untitledTextEditor")
    String untitledTextEditor();

    /**
     * Translated "Compile PDF".
     *
     * @return translated "Compile PDF"
     */
    @DefaultMessage("Compile PDF")
    @Key("compilePdf")
    String compilePdf();

    /**
     * Translated "R Script".
     *
     * @return translated "R Script"
     */
    @DefaultMessage("R Script")
    @Key("rScript")
    String rScript();

    /**
     * Translated "Install {0} dependencies".
     *
     * @return translated "Install {0} dependencies"
     */
    @DefaultMessage("Install {0} dependencies")
    @Key("showRequiredPackagesMissingWarningCaption")
    String showRequiredPackagesMissingWarningCaption(String scriptname);

    /**
     * Translated "Preview".
     *
     * @return translated "Preview"
     */
    @DefaultMessage("Preview")
    @Key("preview")
    String preview();

    /**
     * Translated "Knit to ".
     *
     * @return translated "Knit to "
     */
    @DefaultMessage("Knit to ")
    @Key("knitTo")
    String knitTo();

    /**
     * Translated "Preview Notebook".
     *
     * @return translated "Preview Notebook"
     */
    @DefaultMessage("Preview Notebook")
    @Key("previewNotebook")
    String previewNotebook();

    /**
     * Translated "Document Directory".
     *
     * @return translated "Document Directory"
     */
    @DefaultMessage("Document Directory")
    @Key("documentDirectory")
    String documentDirectory();

    /**
     * Translated "Project Directory".
     *
     * @return translated "Project Directory"
     */
    @DefaultMessage("Project Directory")
    @Key("projectDirectory")
    String projectDirectory();

    /**
     * Translated "Current Working Directory".
     *
     * @return translated "Current Working Directory"
     */
    @DefaultMessage("Current Working Directory")
    @Key("currentWorkingDirectory")
    String currentWorkingDirectory();

    /**
     * Translated "Knit Directory".
     *
     * @return translated "Knit Directory"
     */
    @DefaultMessage("Knit Directory")
    @Key("knitDirectory")
    String knitDirectory();

    /**
     * Translated "Render {0}".
     *
     * @return translated "Render {0}"
     */
    @DefaultMessage("Render {0}")
    @Key("renderFormatName")
    String renderFormatName(String formatName);

    /**
     * Translated "Presentation".
     *
     * @return translated "Presentation"
     */
    @DefaultMessage("Presentation")
    @Key("presentation")
    String presentation();

    /**
     * Translated "Document".
     *
     * @return translated "Document"
     */
    @DefaultMessage("Document")
    @Key("document")
    String document();

    /**
     * Translated "Run {0}".
     *
     * @return translated "Run {0}"
     */
    @DefaultMessage("Run {0}")
    @Key("setIsShinyFormatKnitCommandText")
    String setIsShinyFormatKnitCommandText(String docType);

    /**
     * Translated "View the current {0} with Shiny ({1})".
     *
     * @return translated "View the current {0} with Shiny ({1})"
     */
    @DefaultMessage("View the current {0} with Shiny ({1})")
    @Key("setIsShinyFormatKnitDocumentButtonTitle")
    String setIsShinyFormatKnitDocumentButtonTitle(String doctype, String htmlString);

    /**
     * Translated "Preview the notebook ({0})".
     *
     * @return translated "Preview the notebook ({0})"
     */
    @DefaultMessage("Preview the notebook ({0})")
    @Key("setIsNotebookFormatButtonTitle")
    String setIsNotebookFormatButtonTitle(String htmlString);

    /**
     * Translated "Content not publishable".
     *
     * @return translated "Content not publishable"
     */
    @DefaultMessage("Content not publishable")
    @Key("invokePublishCaption")
    String invokePublishCaption();

    /**
     * Translated "This item cannot be published.".
     *
     * @return translated "This item cannot be published."
     */
    @DefaultMessage("This item cannot be published.")
    @Key("invokePublishMessage")
    String invokePublishMessage();

    /**
     * Translated "Knit{0}".
     *
     * @return translated "Knit{0}"
     */
    @DefaultMessage("Knit{0}")
    @Key("setFormatTextKnitCommandText")
    String setFormatTextKnitCommandText(String text);

    /**
     * Translated "Render".
     *
     * @return translated "Render"
     */
    @DefaultMessage("Render")
    @Key("setFormatTextQuartoCommandText")
    String setFormatTextQuartoCommandText();

    /**
     * Translated "Preview{0}".
     *
     * @return translated "Preview{0}"
     */
    @DefaultMessage("Preview{0}")
    @Key("setFormatTextPreviewCommandText")
    String setFormatTextPreviewCommandText(String text);

    /**
     * Translated "Source Script".
     *
     * @return translated "Source Script"
     */
    @DefaultMessage("Source Script")
    @Key("sourceScript")
    String sourceScript();

    /**
     * Translated "Save changes and source the current script".
     *
     * @return translated "Save changes and source the current script"
     */
    @DefaultMessage("Save changes and source the current script")
    @Key("setSourceButtonFromScriptStatePythonDesc")
    String setSourceButtonFromScriptStatePythonDesc();

    /**
     * Translated "Run Script".
     *
     * @return translated "Run Script"
     */
    @DefaultMessage("Run Script")
    @Key("runScript")
    String runScript();

    /**
     * Translated "Save changes and run the current script".
     *
     * @return translated "Save changes and run the current script"
     */
    @DefaultMessage("Save changes and run the current script")
    @Key("setSourceButtonFromScriptStateDesc")
    String setSourceButtonFromScriptStateDesc();

    /**
     * Translated "Save changes and preview".
     *
     * @return translated "Save changes and preview"
     */
    @DefaultMessage("Save changes and preview")
    @Key("setSourceButtonFromScriptStateSavePreview")
    String setSourceButtonFromScriptStateSavePreview();

    /**
     * Translated "Reload App".
     *
     * @return translated "Reload App"
     */
    @DefaultMessage("Reload App")
    @Key("reloadApp")
    String reloadApp();

    /**
     * Translated "Save changes and reload the Shiny application".
     *
     * @return translated "Save changes and reload the Shiny application"
     */
    @DefaultMessage("Save changes and reload the Shiny application")
    @Key("saveChangesAndReload")
    String saveChangesAndReload();

    /**
     * Translated "Run App".
     *
     * @return translated "Run App"
     */
    @DefaultMessage("Run App")
    @Key("runApp")
    String runApp();

    /**
     * Translated "Run the Shiny application".
     *
     * @return translated "Run the Shiny application"
     */
    @DefaultMessage("Run the Shiny application")
    @Key("runTheShinyApp")
    String runTheShinyApp();

    /**
     * Translated "Reload API".
     *
     * @return translated "Reload API"
     */
    @DefaultMessage("Reload API")
    @Key("reloadApi")
    String reloadApi();

    /**
     * Translated "Save changes and reload the Plumber API".
     *
     * @return translated "Save changes and reload the Plumber API"
     */
    @DefaultMessage("Save changes and reload the Plumber API")
    @Key("saveChangesReloadPlumberApi")
    String saveChangesReloadPlumberApi();

    /**
     * Translated "Run API".
     *
     * @return translated "Run API"
     */
    @DefaultMessage("Run API")
    @Key("runApi")
    String runApi();

    /**
     * Translated "Run the Plumber API".
     *
     * @return translated "Run the Plumber API"
     */
    @DefaultMessage("Run the Plumber API")
    @Key("runPlumberApi")
    String runPlumberApi();

    /**
     * Translated "Preview in Viewer Pane".
     *
     * @return translated "Preview in Viewer Pane"
     */
    @DefaultMessage("Preview in Viewer Pane")
    @Key("previewInViewerPane")
    String previewInViewerPane();

    /**
     * Translated "Preview in Window".
     *
     * @return translated "Preview in Window"
     */
    @DefaultMessage("Preview in Window")
    @Key("previewInWindow")
    String previewInWindow();

    /**
     * Translated "(No Preview)".
     *
     * @return translated "(No Preview)"
     */
    @DefaultMessage("(No Preview)")
    @Key("noPreviewParentheses")
    String noPreviewParentheses();

    /**
     * Translated "Use Visual Editor".
     *
     * @return translated "Use Visual Editor"
     */
    @DefaultMessage("Use Visual Editor")
    @Key("useVisualEditor")
    String useVisualEditor();

    /**
     * Translated "Preview Images and Equations".
     *
     * @return translated "Preview Images and Equations"
     */
    @DefaultMessage("Preview Images and Equations")
    @Key("previewImagesEquations")
    String previewImagesEquations();

    /**
     * Translated "Show Previews Inline".
     *
     * @return translated "Show Previews Inline"
     */
    @DefaultMessage("Show Previews Inline")
    @Key("showPreviewsInline")
    String showPreviewsInline();

    /**
     * Translated "Chunk Output Inline".
     *
     * @return translated "Chunk Output Inline"
     */
    @DefaultMessage("Chunk Output Inline")
    @Key("chunkOutputInline")
    String chunkOutputInline();

    /**
     * Translated "Chunk Output in Console".
     *
     * @return translated "Chunk Output in Console"
     */
    @DefaultMessage("Chunk Output in Console")
    @Key("chunkOutputInConsole")
    String chunkOutputInConsole();

    /**
     * Translated "Knit".
     *
     * @return translated "Knit"
     */
    @DefaultMessage("Knit")
    @Key("knit")
    String knit();

    /**
     * Translated "Render".
     *
     * @return translated "Render"
     */
    @DefaultMessage("Render")
    @Key("render")
    String render();

    /**
     * Translated "Print Frame".
     *
     * @return translated "Print Frame"
     */
    @DefaultMessage("Print Frame")
    @Key("printFrame")
    String printFrame();

    /**
     * Translated "visual".
     *
     * @return translated "visual"
     */
    @DefaultMessage("Visual")
    @Key("Visual")
    String visual();

    /**
     * Translated "Markdown editing tools".
     *
     * @return translated "Markdown editing tools"
     */
    @DefaultMessage("Markdown editing tools")
    @Key("markdownEditingTools")
    String markdownEditingTools();

    /**
     * Translated "Compiling C/C++ code for R".
     *
     * @return translated "Compiling C/C++ code for R"
     */
    @DefaultMessage("Compiling C/C++ code for R")
    @Key("compilingCode")
    String compilingCode();

    /**
     * Translated "Running shiny documents".
     *
     * @return translated "Running shiny documents"
     */
    @DefaultMessage("Running shiny documents")
    @Key("runningShinyDocuments")
    String runningShinyDocuments();

    /**
     * Translated "Compiling notebooks from R scripts".
     *
     * @return translated "Compiling notebooks from R scripts"
     */
    @DefaultMessage("Compiling notebooks from R scripts")
    @Key("compilingNotebooks")
    String compilingNotebooks();

    /**
     * Translated "Rendering R Markdown documents".
     *
     * @return translated "Rendering R Markdown documents"
     */
    @DefaultMessage("Rendering R Markdown documents")
    @Key("renderingR")
    String renderingR();

    /**
     * Translated "Specifying Knit parameters".
     *
     * @return translated "Specifying Knit parameters"
     */
    @DefaultMessage("Specifying Knit parameters")
    @Key("specifyingKnit")
    String specifyingKnit();

    /**
     * Translated "Creating R Markdown documents".
     *
     * @return translated "Creating R Markdown documents"
     */
    @DefaultMessage("Creating R Markdown documents")
    @Key("creatingRMarkdown")
    String creatingRMarkdown();
}

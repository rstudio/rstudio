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

    @Key("rConsole")
    String rConsole();

    @Key("chunkFeedback")
    String chunkFeedback();

    @Key("chunkHtmlPageOutputFrame")
    String chunkHtmlPageOutputFrame();

    @Key("chunkHtmlOutputFrame")
    String chunkHtmlOutputFrame();

    @Key("chunkSatelliteWindowInitTitle")
    String chunkSatelliteWindowInitTitle();

    @Key("noImageLabel")
    String noImageLabel(String path);

    @Key("noImageLabelNoParentheses")
    String noImageLabelNoParentheses(String path);

    @Key("doubleClickToZoom")
    String doubleClickToZoom();

    @Key("refactorServerRequestCallbackError")
    String refactorServerRequestCallbackError();

    @Key("onShinyCompareTestError")
    String onShinyCompareTestError();

    @Key("onShinyCompareTestResponseCaption")
    String onShinyCompareTestResponseCaption();

    @Key("onShinyCompareTestResponseMessage")
    String onShinyCompareTestResponseMessage();

    @Key("checkTestPackageDependenciesError")
    String checkTestPackageDependenciesError();

    @Key("checkTestPackageDependenciesMessage")
    String checkTestPackageDependenciesMessage();

    @Key("checkTestPackageDependenciesCaption")
    String checkTestPackageDependenciesCaption();

    @Key("installShinyTestDependenciesError")
    String installShinyTestDependenciesError();

    @Key("checkForExternalEditFileChangedMessage")
    String checkForExternalEditFileChangedMessage(String fileName);

    @Key("checkForExternalEditFileChangedCaption")
    String checkForExternalEditFileChangedCaption();

    @Key("checkForExternalEditFileDeletedMessage")
    String checkForExternalEditFileDeletedMessage(String pathName);

    @Key("checkForExternalEditFileDeletedCaption")
    String checkForExternalEditFileDeletedCaption();

    @Key("fireCompilePdfEventErrorMessage")
    String fireCompilePdfEventErrorMessage(String fileName);

    @Key("fireCompilePdfEventErrorCaption")
    String fireCompilePdfEventErrorCaption();

    @Key("onClearPrerenderedOutputMessage")
    String onClearPrerenderedOutputMessage(String docPath);

    @Key("onClearPrerenderedOutputCaption")
    String onClearPrerenderedOutputCaption();

    @Key("onClearKnitrCacheMessage")
    String onClearKnitrCacheMessage(String docPath);

    @Key("onClearKnitrCacheCaption")
    String onClearKnitrCacheCaption();

    @Key("generateNotebookCaption")
    String generateNotebookCaption();

    @Key("previewRpresentationMessage")
    String previewRpresentationMessage();

    @Key("previewRpresentationCaption")
    String previewRpresentationCaption();

    @Key("sourcePythonUserPrompt")
    String sourcePythonUserPrompt();

    @Key("sourcePythonProgressCaption")
    String sourcePythonProgressCaption();

    @Key("onSetWorkingDirToActiveDocMessage")
    String onSetWorkingDirToActiveDocMessage();

    @Key("onSetWorkingDirToActiveDocCaption")
    String onSetWorkingDirToActiveDocCaption();

    @Key("onGoToLineLabel")
    String onGoToLineLabel();

    @Key("onGoToLineTitle")
    String onGoToLineTitle();

    @Key("getCurrentStatusFileName")
    String getCurrentStatusFileName(String emptyString);

    @Key("getCurrentStatusFileType")
    String getCurrentStatusFileType(String emptyString);

    @Key("getCurrentStatusScope")
    String getCurrentStatusScope(String emptyString);

    @Key("getCurrentStatusColumn")
    String getCurrentStatusColumn(String emptyString);

    @Key("getCurrentStatusRow")
    String getCurrentStatusRow();

    @Key("noName")
    String noName();

    @Key("none")
    String none();

    @Key("runAfter")
    String runAfter();

    @Key("runPrevious")
    String runPrevious();

    @Key("runAll")
    String runAll();

    @Key("onInsertSectionLabel")
    String onInsertSectionLabel();

    @Key("onInsertSectionTitle")
    String onInsertSectionTitle();

    @Key("showFrontMatterEditorDialogMessage")
    String showFrontMatterEditorDialogMessage();

    @Key("showFrontMatterEditorDialogCaption")
    String showFrontMatterEditorDialogCaption();

    @Key("showFrontMatterEditorErrMsg")
    String showFrontMatterEditorErrMsg();

    @Key("showFrontMatterEditorErrCaption")
    String showFrontMatterEditorErrCaption();

    @Key("showFrontMatterEditorMessage")
    String showFrontMatterEditorMessage();

    @Key("functionNameLabel")
    String functionNameLabel();

    @Key("pleaseSelectCodeMessage")
    String pleaseSelectCodeMessage();

    @Key("extractActiveFunctionRefactoringName")
    String extractActiveFunctionRefactoringName();

    @Key("showRModeWarningMessage")
    String showRModeWarningMessage(String command);

    @Key("showRModeWarningCaption")
    String showRModeWarningCaption();

    @Key("variableName")
    String variableName();

    @Key("extractLocalVariableRefactoringName")
    String extractLocalVariableRefactoringName();

    @Key("cancel")
    String cancel();

    @Key("reopenDocument")
    String reopenDocument();

    @Key("onReopenSourceDocWithEncodingMessage")
    String onReopenSourceDocWithEncodingMessage();

    @Key("onReopenSourceDocWithEncodingCaption")
    String onReopenSourceDocWithEncodingCaption();

    @Key("onWordCountMessage")
    String onWordCountMessage(int totalWords, String selectedWordsText);

    @Key("wordCount")
    String wordCount();

    @Key("selectedWords")
    String selectedWords(int selectionWords);

    @Key("renameInScopeSelectedItemMessage")
    String renameInScopeSelectedItemMessage(String selectedItem);

    @Key("renameInScopeMatchesPlural")
    String renameInScopeMatchesPlural();

    @Key("renameInScopeMatch")
    String renameInScopeMatch();

    @Key("renameInScopeFoundMatchesMessage")
    String renameInScopeFoundMatchesMessage(int matches);

    @Key("renameInScopeNoMatchesMessage")
    String renameInScopeNoMatchesMessage(String selectionValue);

    @Key("saveNewFileWithEncodingWarningMessage")
    String saveNewFileWithEncodingWarningMessage();

    @Key("saveNewFileWithEncodingWarningCaption")
    String saveNewFileWithEncodingWarningCaption();

    @Key("saveNewFileWithEncodingSaveFileCaption")
    String saveNewFileWithEncodingSaveFileCaption(String nameValue);

    @Key("dontSave")
    String dontSave();

    @Key("save")
    String save();

    @Key("saveWithPromptMessage")
    String saveWithPromptMessage(String documentName);

    @Key("saveWithPromptCaption")
    String saveWithPromptCaption(String documentName);

    @Key("closeAnyway")
    String closeAnyway();

    @Key("onBeforeDismissMessage")
    String onBeforeDismissMessage(String nameValue);

    @Key("onBeforeDismissCaption")
    String onBeforeDismissCaption(String nameValue);

    @Key("addFunctionsToMenuText")
    String addFunctionsToMenuText(String typeText);

    @Key("chunks")
    String chunks();

    @Key("functions")
    String functions();

    @Key("updateBreakpointWarningBarSourcedMessage")
    String updateBreakpointWarningBarSourcedMessage();

    @Key("updateBreakpointWarningBarPackageMessage")
    String updateBreakpointWarningBarPackageMessage(String pendingPackageName);

    @Key("updateBreakpointWarningBarPackageLoadMessage")
    String updateBreakpointWarningBarPackageLoadMessage();

    @Key("updateBreakpointWarningBarFunctionMessage")
    String updateBreakpointWarningBarFunctionMessage();

    @Key("onBreakpointSetNewDocWarning")
    String onBreakpointSetNewDocWarning();

    @Key("onBreakpointSetPlumberfileWarning")
    String onBreakpointSetPlumberfileWarning();

    @Key("errorSavingSetting")
    String errorSavingSetting();

    @Key("saveFile")
    String saveFile();

    @Key("installTinyTeX")
    String installTinyTeX();

    @Key("installTinytexLowercase")
    String installTinytexLowercase();

    @Key("updateDebugWarningBarMessage")
    String updateDebugWarningBarMessage();

    @Key("beginQueuedCollabSessionNoLabel")
    String beginQueuedCollabSessionNoLabel();

    @Key("beginQueuedCollabSessionYesLabel")
    String beginQueuedCollabSessionYesLabel();

    @Key("beginQueuedCollabSessionMessage")
    String beginQueuedCollabSessionMessage(String filename);

    @Key("beginQueuedCollabSessionCaption")
    String beginQueuedCollabSessionCaption();

    @Key("onBreakpointsSavedWarningBar")
    String onBreakpointsSavedWarningBar();

    @Key("saveAsPathName")
    String saveAsPathName(String stem, String extension);

    @Key("onResponseReceivedMessage")
    String onResponseReceivedMessage();

    @Key("errorSavingFile")
    String errorSavingFile();

    @Key("onErrorMessage")
    String onErrorMessage();

    @Key("checkCompilersRnWPackageNotInstalled")
    String checkCompilersRnWPackageNotInstalled(String fRnWname, String forcontext, String fRnWPackage);

    @Key("rnwFiles")
    String rnwFiles();

    @Key("rnwFilesForProject")
    String rnwFilesForProject();

    @Key("thisFile")
    String thisFile();

    @Key("checkCompilersServerWarning")
    String checkCompilersServerWarning();

    @Key("checkCompilersDesktopWarning")
    String checkCompilersDesktopWarning();

    @Key("checkCompilersRnWWeaveTypeError")
    String checkCompilersRnWWeaveTypeError(String directiveName, String typeNames);

    @Key("checkCompilersUnknownLatexType")
    String checkCompilersUnknownLatexType(String latexProgramDirective, String typeNames);

    @Key("findingUsages")
    String findingUsages();

    @Key("checkBuildCppDependenciesRcppPackage")
    String checkBuildCppDependenciesRcppPackage();

    @Key("checkBuildCppDependenciesToolsNotInstalled")
    String checkBuildCppDependenciesToolsNotInstalled();

    @Key("find")
    String find();

    @Key("findOrReplace")
    String findOrReplace();

    @Key("previewJSErrorMessage")
    String previewJSErrorMessage(String functionString);

    @Key("previewJSErrorCaption")
    String previewJSErrorCaption();

    @Key("blockQuote")
    String blockQuote();

    @Key("verbatim")
    String verbatim();

    @Key("descriptionList")
    String descriptionList();

    @Key("numberedList")
    String numberedList();

    @Key("bulletList")
    String bulletList();

    @Key("quote")
    String quote();

    @Key("typewriter")
    String typewriter();

    @Key("italic")
    String italic();

    @Key("bold")
    String bold();

    @Key("subSubsection")
    String subSubsection();

    @Key("subsection")
    String subsection();

    @Key("section")
    String section();

    @Key("untitledSlide")
    String untitledSlide();

    @Key("noSlides")
    String noSlides();

    @Key("rMarkdown")
    String rMarkdown();

    @Key("rNotebook")
    String rNotebook();

    @Key("createDraftFromTemplateCaption")
    String createDraftFromTemplateCaption(String fileType);

    @Key("createDraftFromTemplateMessage")
    String createDraftFromTemplateMessage(String name);

    @Key("overwrite")
    String overwrite();

    @Key("getTemplateContentErrorCaption")
    String getTemplateContentErrorCaption();

    @Key("getTemplateContentErrorMessage")
    String getTemplateContentErrorMessage(String templatePath, String errorMessage);

    @Key("getRMarkdownParamsFileCaption")
    String getRMarkdownParamsFileCaption();

    @Key("getRMarkdownParamsFileMessage")
    String getRMarkdownParamsFileMessage();

    @Key("cleanAndCreateTemplateCaption")
    String cleanAndCreateTemplateCaption();

    @Key("cleanAndCreateTemplateMessage")
    String cleanAndCreateTemplateMessage(String path);

    @Key("createDraftFromTemplateProgressMessage")
    String createDraftFromTemplateProgressMessage();

    @Key("createDraftFromTemplateOnError")
    String createDraftFromTemplateOnError(String templatePath, String target, String errorMessage);

    @Key("showKnitrPreviewWarningBar")
    String showKnitrPreviewWarningBar(String feature, String requiredVersion);

    @Key("spellcheck")
    String spellcheck();

    @Key("wordIsMisspelled")
    String wordIsMisspelled(String word);
    

    @Key("ignoreWord")
    String ignoreWord();

    @Key("addToUserDictionary")
    String addToUserDictionary();

    @Key("errorPreviewingSql")
    String errorPreviewingSql();

    @Key("sourceOnSave")
    String sourceOnSave();

    @Key("textEditor")
    String textEditor();

    @Key("compareResults")
    String compareResults();

    @Key("runTests")
    String runTests();

    @Key("compileReport")
    String compileReport(String cmdtext);

    @Key("shinyTestOptions")
    String shinyTestOptions();

    @Key("knitOptions")
    String knitOptions();

    @Key("runDocumentOptions")
    String runDocumentOptions();

    @Key("sourceButtonTitle")
    String sourceButtonTitle();

    @Key("source")
    String source();

    @Key("sourceButtonTitleWithEcho")
    String sourceButtonTitleWithEcho(String title);

    @Key("sourceOptions")
    String sourceOptions();

    @Key("runSetupChunkAuto")
    String runSetupChunkAuto();

    @Key("run")
    String run();

    @Key("runAppOptions")
    String runAppOptions();

    @Key("runApiOptions")
    String runApiOptions();

    @Key("show")
    String show();

    @Key("hide")
    String hide();

    @Key("showWhitespace")
    String showWhitespace();

    @Key("format")
    String format();

    @Key("codeTools")
    String codeTools();

    @Key("actionOnSave")
    String actionOnSave(String action);

    @Key("untitledTextEditor")
    String untitledTextEditor();

    @Key("compilePdf")
    String compilePdf();

    @Key("rScript")
    String rScript();

    @Key("showRequiredPackagesMissingWarningCaption")
    String showRequiredPackagesMissingWarningCaption(String scriptname);

    @Key("preview")
    String preview();

    @Key("knitTo")
    String knitTo();

    @Key("previewNotebook")
    String previewNotebook();

    @Key("documentDirectory")
    String documentDirectory();

    @Key("projectDirectory")
    String projectDirectory();

    @Key("currentWorkingDirectory")
    String currentWorkingDirectory();

    @Key("knitDirectory")
    String knitDirectory();

    @Key("renderFormatName")
    String renderFormatName(String formatName);

    @Key("presentation")
    String presentation();

    @Key("document")
    String document();

    @Key("setIsShinyFormatKnitCommandText")
    String setIsShinyFormatKnitCommandText(String docType);

    @Key("setIsShinyFormatKnitDocumentButtonTitle")
    String setIsShinyFormatKnitDocumentButtonTitle(String doctype, String htmlString);

    @Key("setIsNotebookFormatButtonTitle")
    String setIsNotebookFormatButtonTitle(String htmlString);

    @Key("invokePublishCaption")
    String invokePublishCaption();

    @Key("invokePublishMessage")
    String invokePublishMessage();

    @Key("setFormatTextKnitCommandText")
    String setFormatTextKnitCommandText(String text);

    @Key("setFormatTextQuartoCommandText")
    String setFormatTextQuartoCommandText();

    @Key("setFormatTextPreviewCommandText")
    String setFormatTextPreviewCommandText(String text);

    @Key("sourceScript")
    String sourceScript();

    @Key("setSourceButtonFromScriptStatePythonDesc")
    String setSourceButtonFromScriptStatePythonDesc();

    @Key("runScript")
    String runScript();

    @Key("setSourceButtonFromScriptStateDesc")
    String setSourceButtonFromScriptStateDesc();

    @Key("setSourceButtonFromScriptStateSavePreview")
    String setSourceButtonFromScriptStateSavePreview();

    @Key("reloadApp")
    String reloadApp();

    @Key("saveChangesAndReload")
    String saveChangesAndReload();

    @Key("runApp")
    String runApp();

    @Key("runTheShinyApp")
    String runTheShinyApp();

    @Key("reloadApi")
    String reloadApi();

    @Key("saveChangesReloadPlumberApi")
    String saveChangesReloadPlumberApi();

    @Key("runApi")
    String runApi();

    @Key("runPlumberApi")
    String runPlumberApi();

    @Key("previewInViewerPane")
    String previewInViewerPane();

    @Key("previewInWindow")
    String previewInWindow();

    @Key("noPreviewParentheses")
    String noPreviewParentheses();

    @Key("useVisualEditor")
    String useVisualEditor();

    @Key("previewImagesEquations")
    String previewImagesEquations();

    @Key("showPreviewsInline")
    String showPreviewsInline();

    @Key("chunkOutputInline")
    String chunkOutputInline();

    @Key("chunkOutputInConsole")
    String chunkOutputInConsole();

    @Key("knit")
    String knit();

    @Key("render")
    String render();

    @Key("printFrame")
    String printFrame();

    @Key("Visual")
    String visual();

    @Key("markdownEditingTools")
    String markdownEditingTools();

    @Key("compilingCode")
    String compilingCode();

    @Key("runningShinyDocuments")
    String runningShinyDocuments();

    @Key("compilingNotebooks")
    String compilingNotebooks();

    @Key("renderingR")
    String renderingR();

    @Key("specifyingKnit")
    String specifyingKnit();

    @Key("creatingRMarkdown")
    String creatingRMarkdown();

    @Key("copilotWaiting")
    String copilotWaiting();

    @Key("copilotNoCompletions")
    String copilotNoCompletions();

    @Key("copilotResponseReceived")
    String copilotResponseReceived();

    @Key("copilotResponseErrorMessage")
    String copilotResponseErrorMessage(String message);

    @Key("copilotEnabled")
    String copilotEnabled();

    @Key("copilotDisabled")
    String copilotDisabled();
    

    @Key("reformatDocumentOnSave")
    String reformatDocumentOnSave();
    
}

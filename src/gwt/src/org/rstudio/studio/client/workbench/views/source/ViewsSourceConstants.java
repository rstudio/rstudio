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

    @Key("noDocumentsParentheses")
    String noDocumentsParentheses();

    @Key("noOutlineAvailable")
    String noOutlineAvailable();

    @Key("title")
    String title();

    @Key("invalidApiName")
    String invalidApiName();

    @Key("invalidApplicationName")
    String invalidApplicationName();

    @Key("apiNameMustNotBeEmpty")
    String apiNameMustNotBeEmpty();

    @Key("plumberAPIs")
    String plumberAPIs();

    @Key("createWithinDirectoryColon")
    String createWithinDirectoryColon();

    @Key("apiNameColon")
    String apiNameColon();

    @Key("create")
    String create();

    @Key("applicationNameColon")
    String applicationNameColon();

    @Key("applicationTypeColon")
    String applicationTypeColon();

    @Key("singleFileAppR")
    String singleFileAppR();

    @Key("multipleFileUiServerR")
    String multipleFileUiServerR();

    @Key("shinyWebApplications")
    String shinyWebApplications();

    @Key("applicationNameMustNotBeEmpty")
    String applicationNameMustNotBeEmpty();

    @Key("invalidApplicationNameCapitalized")
    String invalidApplicationNameCapitalized();

    @Key("newQuatroProgressIndicator")
    String newQuatroProgressIndicator(String presentationType);

    @Key("presentationCapitalized")
    String presentationCapitalized();

    @Key("documentCapitalized")
    String documentCapitalized();

    @Key("errorCapitalized")
    String errorCapitalized();

    @Key("alwaysSaveFilesBeforeBuild")
    String alwaysSaveFilesBeforeBuild();

    @Key("show")
    String show();

    @Key("sourceDocumentError")
    String sourceDocumentError();

    @Key("showProfiler")
    String showProfiler();

    @Key("rNotebook")
    String rNotebook();

    @Key("notebookCreationFailed")
    String notebookCreationFailed();

    @Key("rNotebookCreationFailedPackagesNotInstalled")
    String rNotebookCreationFailedPackagesNotInstalled();

    @Key("creatingStanScript")
    String creatingStanScript();

    @Key("creatingStanScriptPlural")
    String creatingStanScriptPlural();

    @Key("creatingNewDocument")
    String creatingNewDocument();

    @Key("errorCreatingShinyApplication")
    String errorCreatingShinyApplication();

    @Key("errorCreatingPlumberApi")
    String errorCreatingPlumberApi();

    @Key("newShinyWebApplication")
    String newShinyWebApplication();

    @Key("newRPresentation")
    String newRPresentation();

    @Key("creatingPresentation")
    String creatingPresentation();

    @Key("documentTabMoveFailed")
    String documentTabMoveFailed();

    @Key("couldntMoveTabToWindowError")
    String couldntMoveTabToWindowError(String errorMessage);

    @Key("closeAll")
    String closeAll();

    @Key("closeOther")
    String closeOther();

    @Key("openFile")
    String openFile();

    @Key("newPlumberApi")
    String newPlumberApi();

    @Key("closeAllOthers")
    String closeAllOthers();

    @Key("errorCreatingNewDocument")
    String errorCreatingNewDocument();

    @Key("publishDocument")
    String publishDocument();

    @Key("publishPlumberApi")
    String publishPlumberApi();

    @Key("publishApplication")
    String publishApplication();

    // TODO IF THESE ARE SHORTCUTS NEED TO DOUBLE CHECK THESE

    @Key("blameOnGithub")
    String blameOnGithub(String name);

    @Key("viewNameOnGithub")
    String viewNameOnGithub(String name);

    @Key("revertName")
    String revertName(String name);

    @Key("logOfName")
    String logOfName(String name);

    @Key("diffName")
    String diffName(String name);

    @Key("noDocumentTabsOpen")
    String noDocumentTabsOpen();

    @Key("showObjectExplorer")
    String showObjectExplorer();

    @Key("showDataFrame")
    String showDataFrame();

    @Key("templateContentMissing")
    String templateContentMissing();

    @Key("templateAtPathMissing")
    String templateAtPathMissing(String templatePath);

    @Key("errorWhileOpeningFile")
    String errorWhileOpeningFile();

    @Key("openNotebookWarningMessage")
    String openNotebookWarningMessage();

    @Key("notebookOpenFailed")
    String notebookOpenFailed();

    @Key("notebookOpenFailedMessage")
    String notebookOpenFailedMessage(String errorMessage);

    @Key("notebookCouldNotBeOpenedMessage")
    String notebookCouldNotBeOpenedMessage(String errorMessage);

    @Key("openingFile")
    String openingFile();

    @Key("showFileTooLargeWarningMsg")
    String showFileTooLargeWarningMsg(String filename, String filelength, String sizeLimit);

    @Key("selectedFileTooLarge")
    String selectedFileTooLarge();

    @Key("confirmOpenLargeFileMsg")
    String confirmOpenLargeFileMsg(String filename, String length);

    @Key("confirmOpen")
    String confirmOpen();

    @Key("sourceColumn")
    String sourceColumn(int columnCounter);

    @Key("source")
    String source();

    @Key("switchToTab")
    String switchToTab();

    @Key("rstudioSourceEditor")
    String rstudioSourceEditor();

    @Key("closeSourceWindow")
    String closeSourceWindow();

    @Key("cancel")
    String cancel();

    @Key("closeAndDiscardChanges")
    String closeAndDiscardChanges();

    @Key("unsavedChanges")
    String unsavedChanges();

    @Key("confirmCloseUnsavedDocuments")
    String confirmCloseUnsavedDocuments();

    @Key("yourEditsToFileHasNotBeenSaved")
    String yourEditsToFileHasNotBeenSaved(String desc);

    @Key("yourEditsToFilePluralHasNotBeenSaved")
    String yourEditsToFilePluralHasNotBeenSaved(String completeMessage);

    @Key("commaListSeparator")
    String commaListSeparator();

    @Key("andForList")
    String andForList();

    @Key("cantMoveDoc")
    String cantMoveDoc();

    @Key("cantMoveDocMessage")
    String cantMoveDocMessage(String errorMessage);

    @Key("searchTabs")
    String searchTabs();

    @Key("codeEditorTab")
    String codeEditorTab();

    @Key("functionColon")
    String functionColon();

    @Key("methodColon")
    String methodColon();

    @Key("errorReadingFunctionDefinition")
    String errorReadingFunctionDefinition();

    @Key("codeBrowserDisplayed")
    String codeBrowserDisplayed();

    @Key("debugLocationIsApproximate")
    String debugLocationIsApproximate();

    @Key("rSourceViewer")
    String rSourceViewer();

    @Key("readOnlyParentheses")
    String readOnlyParentheses();

    @Key("codeBrowserSecond")
    String codeBrowserSecond();

    @Key("codeTools")
    String codeTools();

    @Key("errorSearchingForFunction")
    String errorSearchingForFunction();

    @Key("searchingForFunctionDefinition")
    String searchingForFunctionDefinition();

    @Key("nameSourceViewer")
    String nameSourceViewer(String name);

    @Key("untitledSourceViewer")
    String untitledSourceViewer();

    @Key("help")
    String help();

    @Key("dataBrowserDisplayed")
    String dataBrowserDisplayed();

    @Key("dataBrowser")
    String dataBrowser();

    @Key("accessibleNameDataBrowser")
    String accessibleNameDataBrowser(String accessibleName);

    @Key("untitledDataBrowser")
    String untitledDataBrowser();

    @Key("dataEditingTargetWidgetLabel1")
    String dataEditingTargetWidgetLabel1(String displayedObservations, String totalObservations);

    @Key("dataEditingTargetWidgetLabel2")
    String dataEditingTargetWidgetLabel2(String omittedNumber);

    @Key("accessibleNameObjectExplorer")
    String accessibleNameObjectExplorer(String accessibleName);

    @Key("untitledObjectExplorer")
    String untitledObjectExplorer();

    @Key("objectExplorerDisplayed")
    String objectExplorerDisplayed();

    @Key("urlViewerDisplayed")
    String urlViewerDisplayed();

    @Key("urlBrowser")
    String urlBrowser();

    @Key("accessibleNameBrowser")
    String accessibleNameBrowser(String name);

    @Key("untitledUrlBrowser")
    String untitledUrlBrowser();

    @Key("sourceViewer")
    String sourceViewer();

    @Key("name")
    String name();

    @Key("type")
    String type();

    @Key("value")
    String value();

    @Key("viewCode")
    String viewCode(String code);

    @Key("noSelectionParentheses")
    String noSelectionParentheses();

    @Key("showAttributes")
    String showAttributes();

    @Key("searchObjects")
    String searchObjects();

    @Key("refresh")
    String refresh();

    @Key("sourceFileAtPathDoesNotExist")
    String sourceFileAtPathDoesNotExist(String selectedPath);

    @Key("errorOpeningProfilerSource")
    String errorOpeningProfilerSource();

    @Key("failedToSaveProfile")
    String failedToSaveProfile();

    @Key("saveFileName")
    String saveFileName(String name);

    @Key("failedToSaveProfileProperties")
    String failedToSaveProfileProperties();

    @Key("codeProfileResultsDisplayed")
    String codeProfileResultsDisplayed();

    @Key("profileCapitalized")
    String profileCapitalized();

    @Key("profilerCapitalized")
    String profilerCapitalized();

    @Key("rProfiler")
    String rProfiler();

    @Key("titleProfileView")
    String titleProfileView(String title);

    @Key("failedToOpenProfile")
    String failedToOpenProfile();

    @Key("profilerError")
    String profilerError();

    @Key("failedToStopProfiler")
    String failedToStopProfiler();

    @Key("errorNavigatingToFile")
    String errorNavigatingToFile();

    @Key("noFileAtPath")
    String noFileAtPath(String finalUrl);

    @Key("openLinkMacCommand")
    String openLinkMacCommand();

    @Key("openLinkNotMacCommand")
    String openLinkNotMacCommand();

    @Key("findingDefinition")
    String findingDefinition();

    @Key("ignored")
    String ignored();

    @Key("note")
    String note();

    @Key("warningLowercase")
    String warningLowercase();

    @Key("error")
    String error();

    @Key("fatal")
    String fatal();

    @Key("noMatchesParentheses")
    String noMatchesParentheses();

    @Key("pagingLabelTextOf")
    String pagingLabelTextOf(int index, int textLength);

    @Key("numberOfOccurrencesReplaced")
    String numberOfOccurrencesReplaced(int occurrences);

    @Key("invalidSearchTerm")
    String invalidSearchTerm();

    @Key("noMoreOccurrences")
    String noMoreOccurrences();

    @Key("findCapitalized")
    String findCapitalized();

    @Key("findOrReplace")
    String findOrReplace();

    @Key("replaceCapitalized")
    String replaceCapitalized();

    @Key("allCapitalized")
    String allCapitalized();

    @Key("replaceAllOccurrences")
    String replaceAllOccurrences();

    @Key("inSelection")
    String inSelection();

    @Key("matchCase")
    String matchCase();

    @Key("wholeWord")
    String wholeWord();

    @Key("regexCapitalized")
    String regexCapitalized();

    @Key("wrapCapitalized")
    String wrapCapitalized();

    @Key("closeFindAndReplace")
    String closeFindAndReplace();

    @Key("chunkPendingExecution")
    String chunkPendingExecution();

    @Key("chunkPendingExecutionMessage")
    String chunkPendingExecutionMessage();

    @Key("okFullyCapitalized")
    String okFullyCapitalized();

    @Key("dontRun")
    String dontRun();

    @Key("rMarkdownNotInstalledHTMLNoGenerate")
    String rMarkdownNotInstalledHTMLNoGenerate();

    @Key("rMarkdownUpgradeRequired")
    String rMarkdownUpgradeRequired();

    @Key("errorCreatingNotebookPrefix")
    String errorCreatingNotebookPrefix();

    @Key("creatingRNotebooks")
    String creatingRNotebooks();

    @Key("cantExecuteJobDesc")
    String cantExecuteJobDesc(String jobDesc);

    @Key("executingPythonChunks")
    String executingPythonChunks();

    @Key("executingChunks")
    String executingChunks();

    @Key("runChunk")
    String runChunk();

    @Key("jobChunkCurrentlyExecuting")
    String jobChunkCurrentlyExecuting(String jobDesc);

    @Key("rStudioCannotExecuteJob")
    String rStudioCannotExecuteJob(String jobDesc);

    @Key("runChunks")
    String runChunks();

    @Key("chunksCurrentlyRunning")
    String chunksCurrentlyRunning();

    @Key("outputCantBeClearedBecauseChunks")
    String outputCantBeClearedBecauseChunks();

    @Key("interruptAndClearOutput")
    String interruptAndClearOutput();

    @Key("removeInlineChunkOutput")
    String removeInlineChunkOutput();

    @Key("clearExistingChunkOutputMessage")
    String clearExistingChunkOutputMessage();

    @Key("removeOutput")
    String removeOutput();

    @Key("keepOutput")
    String keepOutput();

    @Key("unnamedChunk")
    String unnamedChunk();

    @Key("chunkNameColon")
    String chunkNameColon();

    @Key("outputColon")
    String outputColon();

    @Key("showWarnings")
    String showWarnings();

    @Key("showMessages")
    String showMessages();

    @Key("cacheChunk")
    String cacheChunk();

    @Key("usePagedTables")
    String usePagedTables();

    @Key("useCustomFigureSize")
    String useCustomFigureSize();

    @Key("widthInchesColon")
    String widthInchesColon();

    @Key("heightInchesColon")
    String heightInchesColon();

    @Key("enginePathColon")
    String enginePathColon();

    @Key("selectEngine")
    String selectEngine();

    @Key("engineOptionsColon")
    String engineOptionsColon();

    @Key("chunkOptions")
    String chunkOptions();

    @Key("revertCapitalized")
    String revertCapitalized();

    @Key("applyCapitalized")
    String applyCapitalized();

    @Key("showNothingDontRunCode")
    String showNothingDontRunCode();

    @Key("showNothingRunCode")
    String showNothingRunCode();

    @Key("showCodeAndOutput")
    String showCodeAndOutput();

    @Key("showOutputOnly")
    String showOutputOnly();

    @Key("useDocumentDefaultParentheses")
    String useDocumentDefaultParentheses();

    @Key("defaultChunkOptions")
    String defaultChunkOptions();

    @Key("checkSpelling")
    String checkSpelling();

    @Key("anErrorHasOccurredMessage")
    String anErrorHasOccurredMessage(String eMessage);

    @Key("spellCheckIsComplete")
    String spellCheckIsComplete();

    @Key("spellCheckInProgress")
    String spellCheckInProgress();

    @Key("addCapitalized")
    String addCapitalized();

    @Key("addWordToUserDictionary")
    String addWordToUserDictionary();

    @Key("skip")
    String skip();

    @Key("ignoreAll")
    String ignoreAll();

    @Key("changeCapitalized")
    String changeCapitalized();

    @Key("changeAll")
    String changeAll();

    @Key("suggestionsCapitalized")
    String suggestionsCapitalized();

    @Key("checkingEllipses")
    String checkingEllipses();

    @Key("classCapitalized")
    String classCapitalized();

    @Key("namespaceCapitalized")
    String namespaceCapitalized();

    @Key("lambdaCapitalized")
    String lambdaCapitalized();

    @Key("anonymousCapitalized")
    String anonymousCapitalized();

    @Key("functionCapitalized")
    String functionCapitalized();

    @Key("testCapitalized")
    String testCapitalized();

    @Key("chunkCapitalized")
    String chunkCapitalized();

    @Key("sectionCapitalized")
    String sectionCapitalized();

    @Key("slideCapitalized")
    String slideCapitalized();

    @Key("tomorrowNight")
    String tomorrowNight();

    @Key("textmateDefaultParentheses")
    String textmateDefaultParentheses();

    @Key("specifiedThemeDoesNotExist")
    String specifiedThemeDoesNotExist();

    @Key("specifiedDefaultThemeCannotBeRemoved")
    String specifiedDefaultThemeCannotBeRemoved();

    @Key("chooseEncoding")
    String chooseEncoding();

    @Key("encodingsCapitalized")
    String encodingsCapitalized();

    @Key("showAllEncodings")
    String showAllEncodings();

    @Key("setAsDefaultEncodingSourceFiles")
    String setAsDefaultEncodingSourceFiles();

    @Key("sysEncNameDefault")
    String sysEncNameDefault(String sysEncName);

    @Key("askSquareBrackets")
    String askSquareBrackets();

    @Key("newRDocumentationFile")
    String newRDocumentationFile();

    @Key("nameNotSpecified")
    String nameNotSpecified();

    @Key("mustSpecifyTopicNameForRdFile")
    String mustSpecifyTopicNameForRdFile();

    @Key("newRMarkdown")
    String newRMarkdown();

    @Key("templatesCapitalized")
    String templatesCapitalized();

    @Key("createEmptyDocument")
    String createEmptyDocument();

    @Key("usingShinyWithRMarkdown")
    String usingShinyWithRMarkdown();

    @Key("shinyDocNameDescription")
    String shinyDocNameDescription();

    @Key("shinyPresentationNameDescription")
    String shinyPresentationNameDescription();

    @Key("fromTemplate")
    String fromTemplate();

    @Key("shinyCapitalized")
    String shinyCapitalized();

    @Key("shinyDocument")
    String shinyDocument();

    @Key("shinyPresentation")
    String shinyPresentation();

    @Key("noParametersDefined")
    String noParametersDefined();

    @Key("noParametersDefinedForCurrentRMarkdown")
    String noParametersDefinedForCurrentRMarkdown();

    @Key("usingRMarkdownParameters")
    String usingRMarkdownParameters();

    @Key("unableToActivateVisualModeYAML")
    String unableToActivateVisualModeYAML();

    @Key("unableToActivateVisualModeParsingCode")
    String unableToActivateVisualModeParsingCode();

    @Key("unableToActivateVisualModeDocumentContains")
    String unableToActivateVisualModeDocumentContains();

    @Key("unrecognizedPandocTokens")
    String unrecognizedPandocTokens(String tokens);

    @Key("invalidPandocFormat")
    String invalidPandocFormat(String format);

    @Key("unsupportedExtensionsForMarkdown")
    String unsupportedExtensionsForMarkdown(String format);

    @Key("unableToParseMarkdownPleaseReport")
    String unableToParseMarkdownPleaseReport();

    @Key("chunkSequence")
    String chunkSequence(int itemSequence);

    @Key("topLevelParentheses")
    String topLevelParentheses();

    @Key("cantEnterVisualModeUsingRealtime")
    String cantEnterVisualModeUsingRealtime();

    @Key("visualModeChunkSummary")
    String visualModeChunkSummary(String enging, int lines);

    @Key("visualModeChunkSummaryPlural")
    String visualModeChunkSummaryPlural(String engine, int lines);

    @Key("images")
    String images();

    @Key("xaringanPresentationsVisualMode")
    String xaringanPresentationsVisualMode();

    @Key("versionControlConflict")
    String versionControlConflict();

    @Key("switchToVisualMode")
    String switchToVisualMode();

    @Key("useVisualMode")
    String useVisualMode();

    @Key("dontShowMessageAgain")
    String dontShowMessageAgain();

    @Key("lineWrapping")
    String lineWrapping();

    @Key("projectDefault")
    String projectDefault();

    @Key("globalDefault")
    String globalDefault();

    @Key("lineWrappingDiffersFromCurrent")
    String lineWrappingDiffersFromCurrent(String current);

    @Key("selectHandleLineWrapping")
    String selectHandleLineWrapping();

    @Key("useBasedLineWrappingForDocument")
    String useBasedLineWrappingForDocument(String detectedLineWrappingForDocument);
    

    @Key("useBasedLineWrappingForProject")
    String useBasedLineWrappingForProject(String detectedLineWrappingForProject);

    @Key("useDefaultLinewrapping")
    String useDefaultLinewrapping(String projectConfigChoice);

    @Key("project")
    String project();

    @Key("global")
    String global();

    @Key("learnAboutVisualModeLineWrapping")
    String learnAboutVisualModeLineWrapping();

    @Key("wrapAtColumnColon")
    String wrapAtColumnColon();

    @Key("collapseCapitalized")
    String collapseCapitalized();

    @Key("expandCapitalized")
    String expandCapitalized();

    @Key("collapseOrExpandCodeChunk")
    String collapseOrExpandCodeChunk(String hintText);

    @Key("editsStillBeingBackedUp")
    String editsStillBeingBackedUp();

    @Key("processStillBeingUsedTextEditingTarget")
    String processStillBeingUsedTextEditingTarget();

    @Key("errorAutosavingFile")
    String errorAutosavingFile();

    @Key("rStudioUnableToAutosave")
    String rStudioUnableToAutosave();

    @Key("couldNotSavePathPlusMessage")
    String couldNotSavePathPlusMessage(String path, String message);

    @Key("errorSavingPathPlusMessage")
    String errorSavingPathPlusMessage(String path, String message);

    @Key("documentOutline")
    String documentOutline();

    @Key("moreButtonCell")
    String moreButtonCell();

    @Key("truncatedEllipses")
    String truncatedEllipses(String truncated);

    @Key("documentUsesBasedLineWrapping")
    String documentUsesBasedLineWrapping(String detectedLineWrapping);

    @Key("defaultNoLineWrapping")
    String defaultNoLineWrapping(String current);

    @Key("defaultConfiguredBasedLineWrapping")
    String defaultConfiguredBasedLineWrapping(String current, String configuredLineWrapping);

    @Key("createRNotebookText")
    String createRNotebookText();

    @Key("creatingShinyApplicationsText")
    String creatingShinyApplicationsText();

    @Key("authoringRPresentationsText")
    String authoringRPresentationsText();

    @Key("creatingRPlumberAPIText")
    String creatingRPlumberAPIText();

    @Key("usingRNotebooksText")
    String usingRNotebooksText();

    @Key("theProfilerText")
    String theProfilerText();

    @Key("couldNotResolveIssue")
    String couldNotResolveIssue(String issue);

    @Key("unableToEditTitle")
    String unableToEditTitle();

    @Key("unableToEditMessage")
    String unableToEditMessage();
}

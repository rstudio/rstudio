/*
 * ViewEnvironmentConstants.java
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

package org.rstudio.studio.client.workbench.views.environment;

public interface ViewEnvironmentConstants extends com.google.gwt.i18n.client.Messages{

    @Key("confirmRemoveObjects")
    String confirmRemoveObjects();

    @Key("yesCapitalized")
    String yesCapitalized();

    @Key("noCapitalized")
    String noCapitalized();

    @Key("allObjects")
    String allObjects();

    @Key("oneObject")
    String oneObject();

    @Key("multipleObjects")
    String multipleObjects(int numObjects);

    @Key("confirmObjectRemove")
    String confirmObjectRemove(String objects);

    @Key("includeHiddenObjects")
    String includeHiddenObjects();

    @Key("environmentCapitalized")
    String environmentCapitalized();

    @Key("environmentTab")
    String environmentTab();

    @Key("refreshNow")
    String refreshNow();

    @Key("refreshOptions")
    String refreshOptions();

    @Key("environmentTabSecond")
    String environmentTabSecond();

    @Key("viewingPythonObjectsCapitalized")
    String viewingPythonObjectsCapitalized();

    @Key("viewingPythonObjects")
    String viewingPythonObjects();

    @Key("searchEnvironment")
    String searchEnvironment();

    @Key("errorOpeningCallFrame")
    String errorOpeningCallFrame();

    @Key("importDataset")
    String importDataset();

    @Key("couldNotChangeMonitoringState")
    String couldNotChangeMonitoringState();

    @Key("importCapitalized")
    String importCapitalized();

    @Key("removingObjectsEllipses")
    String removingObjectsEllipses();

    @Key("saveWorkspaceAs")
    String saveWorkspaceAs();

    @Key("loadWorkspace")
    String loadWorkspace();

    @Key("selectFileToImport")
    String selectFileToImport();

    @Key("importFromWebURL")
    String importFromWebURL();

    @Key("pleaseEnterURLToImportDataFrom")
    String pleaseEnterURLToImportDataFrom();

    @Key("downloadingDataEllipses")
    String downloadingDataEllipses();

    @Key("loadRObject")
    String loadRObject();

    @Key("loadDataIntoAnRObject")
    String loadDataIntoAnRObject(String dataFilePath);

    @Key("confirmLoadRData")
    String confirmLoadRData();

    @Key("loadRDataFileIntoGlobalEnv")
    String loadRDataFileIntoGlobalEnv(String dataFilePath);

    @Key("errorListingObjects")
    String errorListingObjects();

    @Key("codeCreationError")
    String codeCreationError();

    @Key("dataPreview")
    String dataPreview();

    @Key("isThisAValidCSVFile")
    String isThisAValidCSVFile();

    @Key("isThisAValidSpssSasSataFile")
    String isThisAValidSpssSasSataFile();

    @Key("isThisAValidExcelFile")
    String isThisAValidExcelFile();

    @Key("copyCodePreview")
    String copyCodePreview();

    @Key("pleaseEnterFormatString")
    String pleaseEnterFormatString();

    @Key("pleaseInsertACommaSeparatedList")
    String pleaseInsertACommaSeparatedList();

    @Key("dateFormat")
    String dateFormat();

    @Key("timeFormat")
    String timeFormat();

    @Key("dateAndTimeFormat")
    String dateAndTimeFormat();

    @Key("factors")
    String factors();

    @Key("allColumnsMustHaveName")
    String allColumnsMustHaveName();

    @Key("retrievingPreviewDataEllipses")
    String retrievingPreviewDataEllipses();

    @Key("previewingFirstEntriesMultiple")
    String previewingFirstEntriesMultiple(String rows, String parsingErrors);

    @Key("previewingFirstEntriesNone")
    String previewingFirstEntriesNone(String rows);

    @Key("browseCapitalized")
    String browseCapitalized();

    @Key("updateCapitalized")
    String updateCapitalized();

    @Key("updateButtonAriaLabel")
    String updateButtonAriaLabel(String caption, String ariaLabelSuffix);

    @Key("browseButtonAriaLabel")
    String browseButtonAriaLabel(String caption, String ariaLabelSuffix);

    @Key("chooseFile")
    String chooseFile();

    @Key("readingRectangularDataUsingReadr")
    String readingRectangularDataUsingReadr();

    @Key("configureLocale")
    String configureLocale();

    @Key("characterOtherDelimiter")
    String characterOtherDelimiter(String otherDelimiter);

    @Key("incorrectDelimiter")
    String incorrectDelimiter();

    @Key("specifiedDelimiterNotValid")
    String specifiedDelimiterNotValid();

    @Key("enterSingleCharacterDelimiter")
    String enterSingleCharacterDelimiter();

    @Key("otherDelimiter")
    String otherDelimiter();

    @Key("defaultCapitalized")
    String defaultCapitalized();

    @Key("empty")
    String empty();

    @Key("notApplicableAbbreviation")
    String notApplicableAbbreviation();

    @Key("nullWord")
    String nullWord();

    @Key("noneCapitalized")
    String noneCapitalized();

    @Key("doubleQuotesParentheses")
    String doubleQuotesParentheses();

    @Key("singleQuoteParentheses")
    String singleQuoteParentheses();

    @Key("otherEllipses")
    String otherEllipses();

    @Key("whitespaceCapitalized")
    String whitespaceCapitalized();

    @Key("tabCapitalized")
    String tabCapitalized();

    @Key("semicolonCapitalized")
    String semicolonCapitalized();

    @Key("commaCapitalized")
    String commaCapitalized();

    @Key("bothCapitalized")
    String bothCapitalized();

    @Key("doubleCapitalized")
    String doubleCapitalized();

    @Key("backslashCapitalized")
    String backslashCapitalized();

    @Key("configureCapitalized")
    String configureCapitalized();

    @Key("localesInReadr")
    String localesInReadr();

    @Key("encodingIdentifier")
    String encodingIdentifier();

    @Key("enterAnEncodingIdentifier")
    String enterAnEncodingIdentifier();

    @Key("readingDataUsingHaven")
    String readingDataUsingHaven();

    @Key("readingExcelFilesUsingReadxl")
    String readingExcelFilesUsingReadxl();

    @Key("importExcelData")
    String importExcelData();

    @Key("importStatisticalData")
    String importStatisticalData();

    @Key("importTextData")
    String importTextData();

    @Key("periodCapitalized")
    String periodCapitalized();

    @Key("automaticCapitalized")
    String automaticCapitalized();

    @Key("useFirstColumn")
    String useFirstColumn();

    @Key("useNumbers")
    String useNumbers();

    @Key("updatingPreview")
    String updatingPreview();

    @Key("errorCapitalized")
    String errorCapitalized();

    @Key("detectingDataFormat")
    String detectingDataFormat();

    @Key("variableNameIsRequired")
    String variableNameIsRequired();

    @Key("pleaseProvideAVariableName")
    String pleaseProvideAVariableName();

    @Key("unknownCapitalized")
    String unknownCapitalized();

    @Key("macOsSystem")
    String macOsSystem();

    @Key("windowsSystem")
    String windowsSystem();

    @Key("shinyFunctionLabel")
    String shinyFunctionLabel(String functionLabel);

    @Key("debugSourceBrackets")
    String debugSourceBrackets();

    @Key("fileLocationAtLine")
    String fileLocationAtLine(String emptyString, String fileName, int lineNumber);

    @Key("tracebackCapitalized")
    String tracebackCapitalized();

    @Key("showInternals")
    String showInternals();

    @Key("selectAll")
    String selectAll();

    @Key("nameCapitalized")
    String nameCapitalized();

    @Key("typeCapitalized")
    String typeCapitalized();

    @Key("lengthCapitalized")
    String lengthCapitalized();

    @Key("sizeCapitalized")
    String sizeCapitalized();

    @Key("valueCapitalized")
    String valueCapitalized();

    @Key("collapseObject")
    String collapseObject();

    @Key("expandObject")
    String expandObject();

    @Key("hasTrace")
    String hasTrace();

    @Key("sizeBytes")
    String sizeBytes(int size);

    @Key("buildNameColumnTitle")
    String buildNameColumnTitle(String name, String type, String size);

    @Key("unevaluatedPromise")
    String unevaluatedPromise(String emptyString);

    @Key("dataCapitalized")
    String dataCapitalized();

    @Key("functionsCapitalized")
    String functionsCapitalized();

    @Key("valuesCapitalized")
    String valuesCapitalized();

    @Key("environmentIsEmpty")
    String environmentIsEmpty();

    @Key("pieChartDepictingMemoryInUse")
    String pieChartDepictingMemoryInUse();

    @Key("memoryInUse")
    String memoryInUse(int percentUsed, String providerName);

    @Key("memoryUsage")
    String memoryUsage();

    @Key("statisticCapitalized")
    String statisticCapitalized();

    @Key("memoryCapitalized")
    String memoryCapitalized();

    @Key("sourceCapitalized")
    String sourceCapitalized();

    @Key("usedByRObjects")
    String usedByRObjects();

    @Key("usedBySession")
    String usedBySession();

    @Key("usedBySystem")
    String usedBySystem();

    @Key("freeSystemMemory")
    String freeSystemMemory();

    @Key("swapSpaceUsed")
    String swapSpaceUsed();

    @Key("totalSystemMemory")
    String totalSystemMemory();

    @Key("okCapitalized")
    String okCapitalized();

    @Key("memoryUsageReport")
    String memoryUsageReport(int percentUsed);

    @Key("showCurrentMemoryUsage")
    String showCurrentMemoryUsage();

    @Key("kiBUsedByRSession")
    String kiBUsedByRSession(String kb, String name);

    @Key("memoryInUseNone")
    String memoryInUseNone();

    @Key("emptyPieChartNoMemoryUsage")
    String emptyPieChartNoMemoryUsage();

    @Key("preparingDataImportText")
    String preparingDataImportText();

    @Key("sessionMemoryLimit")
    String sessionMemoryLimit();

    @Key("memoryUsageLimit")
    String memoryUsageLimit(String limitMB);

    @Key("memoryUsageStatus")
    String memoryUsageStatus(String sessionMem, String limitMessage,
                             String systemMem, String totalMem, String percentFree);

    /*
     * Translated "Session memory used: {0} MiB, {1}.\nFree system memory: {2} MiB ({3}%)."
     *
     * @return "Session memory used: {0} MiB, {1}.\nFree system memory: {2} MiB ({3}%)."
     */
    @Key("multiLineMemoryStatus")
    String multiLineMemoryStatus(String sessionMem, String limitMesage, String freeMemory, String percentFree);

    @Key("unlimited")
    String unlimited();

    @Key("megabytes")
    String megabytes();

    @Key("workbenchLimit")
    String workbenchLimit();
}


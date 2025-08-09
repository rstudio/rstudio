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
    String confirmRemoveObjects();
    String yesCapitalized();
    String noCapitalized();
    String allObjects();
    String oneObject();
    String multipleObjects(int numObjects);
    String confirmObjectRemove(String objects);
    String includeHiddenObjects();
    String environmentCapitalized();
    String environmentTab();
    String refreshNow();
    String refreshOptions();
    String environmentTabSecond();
    String viewingPythonObjectsCapitalized();
    String viewingPythonObjects();
    String searchEnvironment();
    String errorOpeningCallFrame();
    String importDataset();
    String couldNotChangeMonitoringState();
    String importCapitalized();
    String removingObjectsEllipses();
    String saveWorkspaceAs();
    String loadWorkspace();
    String selectFileToImport();
    String importFromWebURL();
    String pleaseEnterURLToImportDataFrom();
    String downloadingDataEllipses();
    String loadRObject();
    String loadDataIntoAnRObject(String dataFilePath);
    String confirmLoadRData();
    String loadRDataFileIntoGlobalEnv(String dataFilePath);
    String errorListingObjects();
    String codeCreationError();
    String dataPreview();
    String isThisAValidCSVFile();
    String isThisAValidSpssSasSataFile();
    String isThisAValidExcelFile();
    String copyCodePreview();
    String pleaseEnterFormatString();
    String pleaseInsertACommaSeparatedList();
    String dateFormat();
    String timeFormat();
    String dateAndTimeFormat();
    String factors();
    String allColumnsMustHaveName();
    String retrievingPreviewDataEllipses();
    String previewingFirstEntriesMultiple(String rows, String parsingErrors);
    String previewingFirstEntriesNone(String rows);
    String browseCapitalized();
    String updateCapitalized();
    String updateButtonAriaLabel(String caption, String ariaLabelSuffix);
    String browseButtonAriaLabel(String caption, String ariaLabelSuffix);
    String chooseFile();
    String readingRectangularDataUsingReadr();
    String configureLocale();
    String characterOtherDelimiter(String otherDelimiter);
    String incorrectDelimiter();
    String specifiedDelimiterNotValid();
    String enterSingleCharacterDelimiter();
    String otherDelimiter();
    String defaultCapitalized();
    String empty();
    String notApplicableAbbreviation();
    String nullWord();
    String noneCapitalized();
    String doubleQuotesParentheses();
    String singleQuoteParentheses();
    String otherEllipses();
    String whitespaceCapitalized();
    String tabCapitalized();
    String semicolonCapitalized();
    String commaCapitalized();
    String bothCapitalized();
    String doubleCapitalized();
    String backslashCapitalized();
    String configureCapitalized();
    String localesInReadr();
    String encodingIdentifier();
    String enterAnEncodingIdentifier();
    String readingDataUsingHaven();
    String readingExcelFilesUsingReadxl();
    String importExcelData();
    String importStatisticalData();
    String importTextData();
    String periodCapitalized();
    String automaticCapitalized();
    String useFirstColumn();
    String useNumbers();
    String updatingPreview();
    String errorCapitalized();
    String detectingDataFormat();
    String variableNameIsRequired();
    String pleaseProvideAVariableName();
    String unknownCapitalized();
    String macOsSystem();
    String windowsSystem();
    String shinyFunctionLabel(String functionLabel);
    String debugSourceBrackets();
    String fileLocationAtLine(String emptyString, String fileName, int lineNumber);
    String tracebackCapitalized();
    String showInternals();
    String selectAll();
    String nameCapitalized();
    String typeCapitalized();
    String lengthCapitalized();
    String sizeCapitalized();
    String valueCapitalized();
    String collapseObject();
    String expandObject();
    String hasTrace();
    String sizeBytes(int size);
    String buildNameColumnTitle(String name, String type, String size);
    String unevaluatedPromise(String emptyString);
    String dataCapitalized();
    String functionsCapitalized();
    String valuesCapitalized();
    String environmentIsEmpty();
    String pieChartDepictingMemoryInUse();
    String memoryInUse(int percentUsed, String providerName);
    String memoryUsage();
    String statisticCapitalized();
    String memoryCapitalized();
    String sourceCapitalized();
    String usedByRObjects();
    String usedBySession();
    String usedBySystem();
    String freeSystemMemory();
    String swapSpaceUsed();
    String totalSystemMemory();
    String okCapitalized();
    String memoryUsageReport(int percentUsed);
    String showCurrentMemoryUsage();
    String kiBUsedByRSession(String kb, String name);
    String memoryInUseNone();
    String emptyPieChartNoMemoryUsage();
    String preparingDataImportText();
    String sessionMemoryLimit();
    String memoryUsageLimit(String limitMB);
    String memoryUsageStatus(String sessionMem, String limitMessage, String systemMem, String totalMem, String percentFree);
    /*
     * Translated "Session memory used: {0} MiB, {1}.\nFree system memory: {2} MiB ({3}%)."
     *
     * @return "Session memory used: {0} MiB, {1}.\nFree system memory: {2} MiB ({3}%)."
     */
    String multiLineMemoryStatus(String sessionMem, String limitMesage, String freeMemory, String percentFree);
    String unlimited();
    String megabytes();
    String workbenchLimit();
}

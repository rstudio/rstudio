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

    @DefaultMessage("Confirm Remove Objects")
    @Key("confirmRemoveObjects")
    String confirmRemoveObjects();

    @DefaultMessage("Yes")
    @Key("yesCapitalized")
    String yesCapitalized();

    @DefaultMessage("No")
    @Key("noCapitalized")
    String noCapitalized();

    @DefaultMessage("all objects")
    @Key("allObjects")
    String allObjects();

    @DefaultMessage("1 object")
    @Key("oneObject")
    String oneObject();

    @DefaultMessage("{0} objects")
    @Key("multipleObjects")
    String multipleObjects(int numObjects);

    @DefaultMessage("Are you sure you want to remove {0} from the environment? This operation cannot be undone.")
    @Key("confirmObjectRemove")
    String confirmObjectRemove(String objects);

    @DefaultMessage("Include hidden objects")
    @Key("includeHiddenObjects")
    String includeHiddenObjects();

    @DefaultMessage("Environment")
    @Key("environmentCapitalized")
    String environmentCapitalized();

    @DefaultMessage("Environment Tab")
    @Key("environmentTab")
    String environmentTab();

    @DefaultMessage("Refresh Now")
    @Key("refreshNow")
    String refreshNow();

    @DefaultMessage("Refresh options")
    @Key("refreshOptions")
    String refreshOptions();

    @DefaultMessage("Environment Tab Second")
    @Key("environmentTabSecond")
    String environmentTabSecond();

    @DefaultMessage("Viewing Python Objects")
    @Key("viewingPythonObjectsCapitalized")
    String viewingPythonObjectsCapitalized();

    @DefaultMessage("Viewing Python objects")
    @Key("viewingPythonObjects")
    String viewingPythonObjects();

    @DefaultMessage("Search environment")
    @Key("searchEnvironment")
    String searchEnvironment();

    @DefaultMessage("Error opening call frame")
    @Key("errorOpeningCallFrame")
    String errorOpeningCallFrame();

    @DefaultMessage("Import Dataset")
    @Key("importDataset")
    String importDataset();

    @DefaultMessage("Could not change monitoring state")
    @Key("couldNotChangeMonitoringState")
    String couldNotChangeMonitoringState();

    @DefaultMessage("Import")
    @Key("importCapitalized")
    String importCapitalized();

    @DefaultMessage("Removing objects...")
    @Key("removingObjectsEllipses")
    String removingObjectsEllipses();

    @DefaultMessage("Save Workspace As")
    @Key("saveWorkspaceAs")
    String saveWorkspaceAs();

    @DefaultMessage("Load Workspace")
    @Key("loadWorkspace")
    String loadWorkspace();

    @DefaultMessage("Select File to Import")
    @Key("selectFileToImport")
    String selectFileToImport();

    @DefaultMessage("Import from Web URL")
    @Key("importFromWebURL")
    String importFromWebURL();

    @DefaultMessage("Please enter the URL to import data from:")
    @Key("pleaseEnterURLToImportDataFrom")
    String pleaseEnterURLToImportDataFrom();

    @DefaultMessage("Downloading data...")
    @Key("downloadingDataEllipses")
    String downloadingDataEllipses();

    @DefaultMessage("Load R Object")
    @Key("loadRObject")
    String loadRObject();

    @DefaultMessage("Load ''{0}'' into an R object named:")
    @Key("loadDataIntoAnRObject")
    String loadDataIntoAnRObject(String dataFilePath);

    @DefaultMessage("Confirm Load RData")
    @Key("confirmLoadRData")
    String confirmLoadRData();

    @DefaultMessage("Do you want to load the R data file \"{0}\" into your global environment?")
    @Key("loadRDataFileIntoGlobalEnv")
    String loadRDataFileIntoGlobalEnv(String dataFilePath);

    @DefaultMessage("Error Listing Objects")
    @Key("errorListingObjects")
    String errorListingObjects();

    @DefaultMessage("Code Creation Error")
    @Key("codeCreationError")
    String codeCreationError();

    @DefaultMessage("Data Preview")
    @Key("dataPreview")
    String dataPreview();

    @DefaultMessage("Is this a valid CSV file?\n\n")
    @Key("isThisAValidCSVFile")
    String isThisAValidCSVFile();

    @DefaultMessage("Is this a valid SPSS, SAS or STATA file?\n\n")
    @Key("isThisAValidSpssSasSataFile")
    String isThisAValidSpssSasSataFile();

    @DefaultMessage("Is this a valid Excel file?\n\n")
    @Key("isThisAValidExcelFile")
    String isThisAValidExcelFile();

    @DefaultMessage("Copy Code Preview")
    @Key("copyCodePreview")
    String copyCodePreview();

    @DefaultMessage("Please enter the format string")
    @Key("pleaseEnterFormatString")
    String pleaseEnterFormatString();

    @DefaultMessage("Enter a comma separated list of factor levels")
    @Key("pleaseInsertACommaSeparatedList")
    String pleaseInsertACommaSeparatedList();

    @DefaultMessage("Date Format")
    @Key("dateFormat")
    String dateFormat();

    @DefaultMessage("Time Format")
    @Key("timeFormat")
    String timeFormat();

    @DefaultMessage("Date and Time Format")
    @Key("dateAndTimeFormat")
    String dateAndTimeFormat();

    @DefaultMessage("Factors")
    @Key("factors")
    String factors();

    @DefaultMessage("All columns must have names in order to perform column operations.")
    @Key("allColumnsMustHaveName")
    String allColumnsMustHaveName();

    @DefaultMessage("Retrieving preview data...")
    @Key("retrievingPreviewDataEllipses")
    String retrievingPreviewDataEllipses();

    @DefaultMessage("Previewing first {0} entries. {1} parsing errors.")
    @Key("previewingFirstEntriesMultiple")
    String previewingFirstEntriesMultiple(String rows, String parsingErrors);

    @DefaultMessage("Previewing first {0} entries. ")
    @Key("previewingFirstEntriesNone")
    String previewingFirstEntriesNone(String rows);

    @DefaultMessage("Browse")
    @Key("browseCapitalized")
    String browseCapitalized();

    @DefaultMessage("Update")
    @Key("updateCapitalized")
    String updateCapitalized();

    @DefaultMessage("{0} for {1}")
    @Key("updateButtonAriaLabel")
    String updateButtonAriaLabel(String caption, String ariaLabelSuffix);

    @DefaultMessage("{0} for {1}...")
    @Key("browseButtonAriaLabel")
    String browseButtonAriaLabel(String caption, String ariaLabelSuffix);

    @DefaultMessage("Choose File")
    @Key("chooseFile")
    String chooseFile();

    @DefaultMessage("Reading rectangular data using readr")
    @Key("readingRectangularDataUsingReadr")
    String readingRectangularDataUsingReadr();

    @DefaultMessage("Configure Locale")
    @Key("configureLocale")
    String configureLocale();

    @DefaultMessage("Character {0}")
    @Key("characterOtherDelimiter")
    String characterOtherDelimiter(String otherDelimiter);

    @DefaultMessage("Incorrect Delimiter")
    @Key("incorrectDelimiter")
    String incorrectDelimiter();

    @DefaultMessage("The specified delimiter is not valid.")
    @Key("specifiedDelimiterNotValid")
    String specifiedDelimiterNotValid();

    @DefaultMessage("Please enter a single character delimiter.")
    @Key("enterSingleCharacterDelimiter")
    String enterSingleCharacterDelimiter();

    @DefaultMessage("Other Delimiter")
    @Key("otherDelimiter")
    String otherDelimiter();

    @DefaultMessage("Default")
    @Key("defaultCapitalized")
    String defaultCapitalized();

    @DefaultMessage("empty")
    @Key("empty")
    String empty();

    @DefaultMessage("NA")
    @Key("notApplicableAbbreviation")
    String notApplicableAbbreviation();

    @DefaultMessage("null")
    @Key("nullWord")
    String nullWord();

    @DefaultMessage("None")
    @Key("noneCapitalized")
    String noneCapitalized();

    @DefaultMessage("Double (\")")
    @Key("doubleQuotesParentheses")
    String doubleQuotesParentheses();

    @DefaultMessage("Single ('')")
    @Key("singleQuoteParentheses")
    String singleQuoteParentheses();

    @DefaultMessage("Other...")
    @Key("otherEllipses")
    String otherEllipses();

    @DefaultMessage("Whitespace")
    @Key("whitespaceCapitalized")
    String whitespaceCapitalized();

    @DefaultMessage("Tab")
    @Key("tabCapitalized")
    String tabCapitalized();

    @DefaultMessage("Semicolon")
    @Key("semicolonCapitalized")
    String semicolonCapitalized();

    @DefaultMessage("Comma")
    @Key("commaCapitalized")
    String commaCapitalized();

    @DefaultMessage("Both")
    @Key("bothCapitalized")
    String bothCapitalized();

    @DefaultMessage("Double")
    @Key("doubleCapitalized")
    String doubleCapitalized();

    @DefaultMessage("Backslash")
    @Key("backslashCapitalized")
    String backslashCapitalized();

    @DefaultMessage("Configure")
    @Key("configureCapitalized")
    String configureCapitalized();

    @DefaultMessage("Locales in readr")
    @Key("localesInReadr")
    String localesInReadr();

    @DefaultMessage("Encoding Identifier")
    @Key("encodingIdentifier")
    String encodingIdentifier();

    @DefaultMessage("Please enter an encoding identifier. For a list of valid encodings run iconvlist().")
    @Key("enterAnEncodingIdentifier")
    String enterAnEncodingIdentifier();

    @DefaultMessage("Reading data using haven")
    @Key("readingDataUsingHaven")
    String readingDataUsingHaven();

    @DefaultMessage("Reading Excel files using readxl")
    @Key("readingExcelFilesUsingReadxl")
    String readingExcelFilesUsingReadxl();

    @DefaultMessage("Import Excel Data")
    @Key("importExcelData")
    String importExcelData();

    @DefaultMessage("Import Statistical Data")
    @Key("importStatisticalData")
    String importStatisticalData();

    @DefaultMessage("Import Text Data")
    @Key("importTextData")
    String importTextData();

    @DefaultMessage("Period")
    @Key("periodCapitalized")
    String periodCapitalized();

    @DefaultMessage("Automatic")
    @Key("automaticCapitalized")
    String automaticCapitalized();

    @DefaultMessage("Use first column")
    @Key("useFirstColumn")
    String useFirstColumn();

    @DefaultMessage("Use numbers")
    @Key("useNumbers")
    String useNumbers();

    @DefaultMessage("Updating preview")
    @Key("updatingPreview")
    String updatingPreview();

    @DefaultMessage("Error")
    @Key("errorCapitalized")
    String errorCapitalized();

    @DefaultMessage("Detecting data format")
    @Key("detectingDataFormat")
    String detectingDataFormat();

    @DefaultMessage("Variable Name Is Required")
    @Key("variableNameIsRequired")
    String variableNameIsRequired();

    @DefaultMessage("Please provide a variable name.")
    @Key("pleaseProvideAVariableName")
    String pleaseProvideAVariableName();

    @DefaultMessage("Unknown")
    @Key("unknownCapitalized")
    String unknownCapitalized();

    @DefaultMessage("MacOS System")
    @Key("macOsSystem")
    String macOsSystem();

    @DefaultMessage("Windows System")
    @Key("windowsSystem")
    String windowsSystem();

    @DefaultMessage("[Shiny: {0}]")
    @Key("shinyFunctionLabel")
    String shinyFunctionLabel(String functionLabel);

    @DefaultMessage("[Debug source]")
    @Key("debugSourceBrackets")
    String debugSourceBrackets();

    @DefaultMessage("{0} at {1}:{2}")
    @Key("fileLocationAtLine")
    String fileLocationAtLine(String emptyString, String fileName, int lineNumber);

    @DefaultMessage("Traceback")
    @Key("tracebackCapitalized")
    String tracebackCapitalized();

    @DefaultMessage("Show internals")
    @Key("showInternals")
    String showInternals();

    @DefaultMessage("Select all")
    @Key("selectAll")
    String selectAll();

    @DefaultMessage("Name")
    @Key("nameCapitalized")
    String nameCapitalized();

    @DefaultMessage("Type")
    @Key("typeCapitalized")
    String typeCapitalized();

    @DefaultMessage("Length")
    @Key("lengthCapitalized")
    String lengthCapitalized();

    @DefaultMessage("Size")
    @Key("sizeCapitalized")
    String sizeCapitalized();

    @DefaultMessage("Value")
    @Key("valueCapitalized")
    String valueCapitalized();

    @DefaultMessage("Collapse Object")
    @Key("collapseObject")
    String collapseObject();

    @DefaultMessage("Expand Object")
    @Key("expandObject")
    String expandObject();

    @DefaultMessage("Has Trace")
    @Key("hasTrace")
    String hasTrace();

    @DefaultMessage(", {0} bytes")
    @Key("sizeBytes")
    String sizeBytes(int size);

    @DefaultMessage("{0} ({1} {2})")
    @Key("buildNameColumnTitle")
    String buildNameColumnTitle(String name, String type, String size);

    @DefaultMessage("{0} (unevaluated promise)")
    @Key("unevaluatedPromise")
    String unevaluatedPromise(String emptyString);

    @DefaultMessage("Data")
    @Key("dataCapitalized")
    String dataCapitalized();

    @DefaultMessage("Functions")
    @Key("functionsCapitalized")
    String functionsCapitalized();

    @DefaultMessage("Values")
    @Key("valuesCapitalized")
    String valuesCapitalized();

    @DefaultMessage("Environment is empty")
    @Key("environmentIsEmpty")
    String environmentIsEmpty();

    @DefaultMessage("Pie chart depicting the percentage of total memory in use")
    @Key("pieChartDepictingMemoryInUse")
    String pieChartDepictingMemoryInUse();

    @DefaultMessage("Memory in use: {0}% (source: {1})")
    @Key("memoryInUse")
    String memoryInUse(int percentUsed, String providerName);

    @DefaultMessage("Memory Usage")
    @Key("memoryUsage")
    String memoryUsage();

    @DefaultMessage("Statistic")
    @Key("statisticCapitalized")
    String statisticCapitalized();

    @DefaultMessage("Memory")
    @Key("memoryCapitalized")
    String memoryCapitalized();

    @DefaultMessage("Source")
    @Key("sourceCapitalized")
    String sourceCapitalized();

    @DefaultMessage("Used by R objects")
    @Key("usedByRObjects")
    String usedByRObjects();

    @DefaultMessage("Used by session")
    @Key("usedBySession")
    String usedBySession();

    @DefaultMessage("Used by system")
    @Key("usedBySystem")
    String usedBySystem();

    @DefaultMessage("Free system memory")
    @Key("freeSystemMemory")
    String freeSystemMemory();

    @DefaultMessage("Swap space used")
    @Key("swapSpaceUsed")
    String swapSpaceUsed();

    @DefaultMessage("Total system memory")
    @Key("totalSystemMemory")
    String totalSystemMemory();

    @DefaultMessage("OK")
    @Key("okCapitalized")
    String okCapitalized();

    @DefaultMessage("Memory Usage Report ({0}% in use)")
    @Key("memoryUsageReport")
    String memoryUsageReport(int percentUsed);

    @DefaultMessage("Show Current Memory Usage")
    @Key("showCurrentMemoryUsage")
    String showCurrentMemoryUsage();

    @DefaultMessage("{0} KiB used by R session (source: {1})")
    @Key("kiBUsedByRSession")
    String kiBUsedByRSession(String kb, String name);

    @DefaultMessage("Memory in use: none (suspended)")
    @Key("memoryInUseNone")
    String memoryInUseNone();

    @DefaultMessage("Empty pie chart depicting no memory usage")
    @Key("emptyPieChartNoMemoryUsage")
    String emptyPieChartNoMemoryUsage();

    @DefaultMessage("Preparing data import")
    @Key("preparingDataImportText")
    String preparingDataImportText();

    @DefaultMessage("Session memory limit")
    @Key("sessionMemoryLimit")
    String sessionMemoryLimit();

    @DefaultMessage("Limit: {0} MiB")
    @Key("memoryUsageLimit")
    String memoryUsageLimit(String limitMB);

    @DefaultMessage("Session memory used: {0} MiB, {1}. System memory used: {2} out of {3} MiB ({4}% free).")
    @Key("memoryUsageStatus")
    String memoryUsageStatus(String sessionMem, String limitMessage,
                             String systemMem, String totalMem, String percentFree);

    /*
     * Translated "Session memory used: {0} MiB, {1}.\nFree system memory: {2} MiB ({3}%)."
     *
     * @return "Session memory used: {0} MiB, {1}.\nFree system memory: {2} MiB ({3}%)."
     */
    @DefaultMessage("Session memory used: {0} MiB.\n{1}.\nFree system memory: {2} MiB ({3}%).")
    @Key("multiLineMemoryStatus")
    String multiLineMemoryStatus(String sessionMem, String limitMesage, String freeMemory, String percentFree);

    @DefaultMessage("unlimited")
    @Key("unlimited")
    String unlimited();

    @DefaultMessage("MiB")
    @Key("megabytes")
    String megabytes();

    @DefaultMessage("Workbench limit")
    @Key("workbenchLimit")
    String workbenchLimit();
}


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
    /**
     * Translated "Confirm Remove Objects".
     *
     * @return translated "Confirm Remove Objects"
     */
    String confirmRemoveObjects();

    /**
     * Translated "Yes".
     *
     * @return translated "Yes"
     */
    String yesCapitalized();

    /**
     * Translated "No".
     *
     * @return translated "No"
     */
    String noCapitalized();

    /**
     * Translated "all objects".
     *
     * @return translated "all objects"
     */
    String allObjects();

    /**
     * Translated "1 object".
     *
     * @return translated "1 object"
     */
    String oneObject();

    /**
     * Translated "{0} objects".
     *
     * @return translated "{0} objects"
     */
    String multipleObjects(int numObjects);

    /**
     * Translated "Are you sure you want to remove {0} from the environment? This operation cannot be undone.".
     *
     * @return translated "Are you sure you want to remove {0} from the environment? This operation cannot be undone."
     */
    String confirmObjectRemove(String objects);

    /**
     * Translated "Include hidden objects".
     *
     * @return translated "Include hidden objects"
     */
    String includeHiddenObjects();

    /**
     * Translated "Environment".
     *
     * @return translated "Environment"
     */
    String environmentCapitalized();

    /**
     * Translated "Environment Tab".
     *
     * @return translated "Environment Tab"
     */
    String environmentTab();

    /**
     * Translated "Refresh Now".
     *
     * @return translated "Refresh Now"
     */
    String refreshNow();

    /**
     * Translated "Refresh options".
     *
     * @return translated "Refresh options"
     */
    String refreshOptions();

    /**
     * Translated "Environment Tab Second".
     *
     * @return translated "Environment Tab Second"
     */
    String environmentTabSecond();

    /**
     * Translated "Viewing Python Objects".
     *
     * @return translated "Viewing Python Objects"
     */
    String viewingPythonObjectsCapitalized();

    /**
     * Translated "Viewing Python objects".
     *
     * @return translated "Viewing Python objects"
     */
    String viewingPythonObjects();

    /**
     * Translated "Search environment".
     *
     * @return translated "Search environment"
     */
    String searchEnvironment();

    /**
     * Translated "Error opening call frame".
     *
     * @return translated "Error opening call frame"
     */
    String errorOpeningCallFrame();

    /**
     * Translated "Import Dataset".
     *
     * @return translated "Import Dataset"
     */
    String importDataset();

    /**
     * Translated "Could not change monitoring state".
     *
     * @return translated "Could not change monitoring state"
     */
    String couldNotChangeMonitoringState();

    /**
     * Translated "Import".
     *
     * @return translated "Import"
     */
    String importCapitalized();

    /**
     * Translated "Removing objects...".
     *
     * @return translated "Removing objects..."
     */
    String removingObjectsEllipses();

    /**
     * Translated "Save Workspace As".
     *
     * @return translated "Save Workspace As"
     */
    String saveWorkspaceAs();

    /**
     * Translated "Load Workspace".
     *
     * @return translated "Load Workspace"
     */
    String loadWorkspace();

    /**
     * Translated "Select File to Import".
     *
     * @return translated "Select File to Import"
     */
    String selectFileToImport();

    /**
     * Translated "Import from Web URL".
     *
     * @return translated "Import from Web URL"
     */
    String importFromWebURL();

    /**
     * Translated "Please enter the URL to import data from:".
     *
     * @return translated "Please enter the URL to import data from:"
     */
    String pleaseEnterURLToImportDataFrom();

    /**
     * Translated "Downloading data...".
     *
     * @return translated "Downloading data..."
     */
    String downloadingDataEllipses();

    /**
     * Translated "Load R Object".
     *
     * @return translated "Load R Object"
     */
    String loadRObject();

    /**
     * Translated "Load ''{0}'' into an R object named:".
     *
     * @return translated "Load ''{0}'' into an R object named:"
     */
    String loadDataIntoAnRObject(String dataFilePath);

    /**
     * Translated "Confirm Load RData".
     *
     * @return translated "Confirm Load RData"
     */
    String confirmLoadRData();

    /**
     * Translated "Do you want to load the R data file \"{0}\" into your global environment?".
     *
     * @return translated "Do you want to load the R data file \"{0}\" into your global environment?"
     */
    String loadRDataFileIntoGlobalEnv(String dataFilePath);

    /**
     * Translated "Error Listing Objects".
     *
     * @return translated "Error Listing Objects"
     */
    String errorListingObjects();

    /**
     * Translated "Code Creation Error".
     *
     * @return translated "Code Creation Error"
     */
    String codeCreationError();

    /**
     * Translated "Data Preview".
     *
     * @return translated "Data Preview"
     */
    String dataPreview();

    /**
     * Translated "Is this a valid CSV file?\n\n".
     *
     * @return translated "Is this a valid CSV file?\n\n"
     */
    String isThisAValidCSVFile();

    /**
     * Translated "Is this a valid SPSS, SAS or STATA file?\n\n".
     *
     * @return translated "Is this a valid SPSS, SAS or STATA file?\n\n"
     */
    String isThisAValidSpssSasSataFile();

    /**
     * Translated "Is this a valid Excel file?\n\n".
     *
     * @return translated "Is this a valid Excel file?\n\n"
     */
    String isThisAValidExcelFile();

    /**
     * Translated "Copy Code Preview".
     *
     * @return translated "Copy Code Preview"
     */
    String copyCodePreview();

    /**
     * Translated "Please enter the format string".
     *
     * @return translated "Please enter the format string"
     */
    String pleaseEnterFormatString();

    /**
     * Translated "Enter a comma separated list of factor levels".
     *
     * @return translated "Enter a comma separated list of factor levels"
     */
    String pleaseInsertACommaSeparatedList();

    /**
     * Translated "Date Format".
     *
     * @return translated "Date Format"
     */
    String dateFormat();

    /**
     * Translated "Time Format".
     *
     * @return translated "Time Format"
     */
    String timeFormat();

    /**
     * Translated "Date and Time Format".
     *
     * @return translated "Date and Time Format"
     */
    String dateAndTimeFormat();

    /**
     * Translated "Factors".
     *
     * @return translated "Factors"
     */
    String factors();

    /**
     * Translated "All columns must have names in order to perform column operations.".
     *
     * @return translated "All columns must have names in order to perform column operations."
     */
    String allColumnsMustHaveName();

    /**
     * Translated "Retrieving preview data...".
     *
     * @return translated "Retrieving preview data..."
     */
    String retrievingPreviewDataEllipses();

    /**
     * Translated "Previewing first {0} entries. {1} parsing errors.".
     *
     * @return translated "Previewing first {0} entries. {1} parsing errors."
     */
    String previewingFirstEntriesMultiple(String rows, String parsingErrors);

    /**
     * Translated "Previewing first {0} entries. ".
     *
     * @return translated "Previewing first {0} entries. "
     */
    String previewingFirstEntriesNone(String rows);

    /**
     * Translated "Browse".
     *
     * @return translated "Browse"
     */
    String browseCapitalized();

    /**
     * Translated "Update".
     *
     * @return translated "Update"
     */
    String updateCapitalized();

    /**
     * Translated "{0} for {1}".
     * "Update for ariaLabelSuffix"
     * @return translated "{0} for {1}"
     */
    String updateButtonAriaLabel(String caption, String ariaLabelSuffix);

    /**
     * Translated "{0} for {1}...".
     * "Browse for ariaLabelSuffix..."
     * @return translated "{0} for {1}..."
     */
    String browseButtonAriaLabel(String caption, String ariaLabelSuffix);

    /**
     * Translated "Choose File".
     *
     * @return translated "Choose File"
     */
    String chooseFile();

    /**
     * Translated "Reading rectangular data using readr".
     *
     * @return translated "Reading rectangular data using readr"
     */
    String readingRectangularDataUsingReadr();

    /**
     * Translated "Configure Locale".
     *
     * @return translated "Configure Locale"
     */
    String configureLocale();

    /**
     * Translated "Character {0}".
     *
     * @return translated "Character {0}"
     */
    String characterOtherDelimiter(String otherDelimiter);

    /**
     * Translated "Incorrect Delimiter".
     *
     * @return translated "Incorrect Delimiter"
     */
    String incorrectDelimiter();

    /**
     * Translated "The specified delimiter is not valid.".
     *
     * @return translated "The specified delimiter is not valid."
     */
    String specifiedDelimiterNotValid();

    /**
     * Translated "Please enter a single character delimiter.".
     *
     * @return translated "Please enter a single character delimiter."
     */
    String enterSingleCharacterDelimiter();

    /**
     * Translated "Other Delimiter".
     *
     * @return translated "Other Delimiter"
     */
    String otherDelimiter();

    /**
     * Translated "Default".
     *
     * @return translated "Default"
     */
    String defaultCapitalized();

    /**
     * Translated "empty".
     *
     * @return translated "empty"
     */
    String empty();

    /**
     * Translated "NA".
     *
     * @return translated "NA"
     */
    String notApplicableAbbreviation();

    /**
     * Translated "null".
     *
     * @return translated "null"
     */
    String nullWord();

    /**
     * Translated "None".
     *
     * @return translated "None"
     */
    String noneCapitalized();

    /**
     * Translated "Double (\")".
     *
     * @return translated "Double (\")"
     */
    String doubleQuotesParentheses();

    /**
     * Translated "Single ('')".
     *
     * @return translated "Single ('')"
     */
    String singleQuoteParentheses();

    /**
     * Translated "Other...".
     *
     * @return translated "Other..."
     */
    String otherEllipses();

    /**
     * Translated "Whitespace".
     *
     * @return translated "Whitespace"
     */
    String whitespaceCapitalized();

    /**
     * Translated "Tab".
     *
     * @return translated "Tab"
     */
    String tabCapitalized();

    /**
     * Translated "Semicolon".
     *
     * @return translated "Semicolon"
     */
    String semicolonCapitalized();

    /**
     * Translated "Comma".
     *
     * @return translated "Comma"
     */
    String commaCapitalized();

    /**
     * Translated "Both".
     *
     * @return translated "Both"
     */
    String bothCapitalized();

    /**
     * Translated "Double".
     *
     * @return translated "Double"
     */
    String doubleCapitalized();

    /**
     * Translated "Backslash".
     *
     * @return translated "Backslash"
     */
    String backslashCapitalized();

    /**
     * Translated "Configure".
     *
     * @return translated "Configure"
     */
    String configureCapitalized();

    /**
     * Translated "Locales in readr".
     *
     * @return translated "Locales in readr"
     */
    String localesInReadr();

    /**
     * Translated "Encoding Identifier".
     *
     * @return translated "Encoding Identifier"
     */
    String encodingIdentifier();

    /**
     * Translated "Please enter an encoding identifier. For a list of valid encodings run iconvlist().".
     *
     * @return translated "Please enter an encoding identifier. For a list of valid encodings run iconvlist()."
     */
    String enterAnEncodingIdentifier();

    /**
     * Translated "Reading data using haven".
     *
     * @return translated "Reading data using haven"
     */
    String readingDataUsingHaven();

    /**
     * Translated "Reading Excel files using readxl".
     *
     * @return translated "Reading Excel files using readxl"
     */
    String readingExcelFilesUsingReadxl();

    /**
     * Translated "Import Excel Data".
     *
     * @return translated "Import Excel Data"
     */
    String importExcelData();

    /**
     * Translated "Import Statistical Data".
     *
     * @return translated "Import Statistical Data"
     */
    String importStatisticalData();

    /**
     * Translated "Import Text Data".
     *
     * @return translated "Import Text Data"
     */
    String importTextData();

    /**
     * Translated "Period".
     *
     * @return translated "Period"
     */
    String periodCapitalized();

    /**
     * Translated "Automatic".
     *
     * @return translated "Automatic"
     */
    String automaticCapitalized();

    /**
     * Translated "Use first column".
     *
     * @return translated "Use first column"
     */
    String useFirstColumn();

    /**
     * Translated "Use numbers".
     *
     * @return translated "Use numbers"
     */
    String useNumbers();

    /**
     * Translated "Updating preview".
     *
     * @return translated "Updating preview"
     */
    String updatingPreview();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String errorCapitalized();

    /**
     * Translated "Detecting data format".
     *
     * @return translated "Detecting data format"
     */
    String detectingDataFormat();

    /**
     * Translated "Variable Name Is Required".
     *
     * @return translated "Variable Name Is Required"
     */
    String variableNameIsRequired();

    /**
     * Translated "Please provide a variable name.".
     *
     * @return translated "Please provide a variable name."
     */
    String pleaseProvideAVariableName();

    /**
     * Translated "Unknown".
     *
     * @return translated "Unknown"
     */
    String unknownCapitalized();

    /**
     * Translated "MacOS System".
     *
     * @return translated "MacOS System"
     */
    String macOsSystem();

    /**
     * Translated "Windows System".
     *
     * @return translated "Windows System"
     */
    String windowsSystem();

    /**
     * Translated "[Shiny: {0}]".
     *
     * @return translated "[Shiny: {0}]"
     */
    String shinyFunctionLabel(String functionLabel);

    /**
     * Translated "[Debug source]".
     *
     * @return translated "[Debug source]"
     */
    String debugSourceBrackets();

    /**
     * Translated "{0} at {1}:{2}".
     *
     * @return translated "{0} at {1}:{2}"
     */
    String fileLocationAtLine(String emptyString, String fileName, int lineNumber);

    /**
     * Translated "Traceback".
     *
     * @return translated "Traceback"
     */
    String tracebackCapitalized();

    /**
     * Translated "Show internals".
     *
     * @return translated "Show internals"
     */
    String showInternals();

    /**
     * Translated "Select all".
     *
     * @return translated "Select all"
     */
    String selectAll();

    /**
     * Translated "Name".
     *
     * @return translated "Name"
     */
    String nameCapitalized();

    /**
     * Translated "Type".
     *
     * @return translated "Type"
     */
    String typeCapitalized();

    /**
     * Translated "Length".
     *
     * @return translated "Length"
     */
    String lengthCapitalized();

    /**
     * Translated "Size".
     *
     * @return translated "Size"
     */
    String sizeCapitalized();

    /**
     * Translated "Value".
     *
     * @return translated "Value"
     */
    String valueCapitalized();

    /**
     * Translated "Collapse Object".
     *
     * @return translated "Collapse Object"
     */
    String collapseObject();

    /**
     * Translated "Expand Object".
     *
     * @return translated "Expand Object"
     */
    String expandObject();

    /**
     * Translated "Has Trace".
     *
     * @return translated "Has Trace"
     */
    String hasTrace();

    /**
     * Translated ", {0} bytes".
     *
     * @return translated ", {0} bytes"
     */
    String sizeBytes(int size);

    /**
     * Translated "{0} ({1} {2})".
     *
     * @return translated "{0} ({1} {2})"
     */
    String buildNameColumnTitle(String name, String type, String size);

    /**
     * Translated "{0} (unevaluated promise)".
     *
     * @return translated "{0} (unevaluated promise)"
     */
    String unevaluatedPromise(String emptyString);

    /**
     * Translated "Data".
     *
     * @return translated "Data"
     */
    String dataCapitalized();

    /**
     * Translated "Functions".
     *
     * @return translated "Functions"
     */
    String functionsCapitalized();

    /**
     * Translated "Values".
     *
     * @return translated "Values"
     */
    String valuesCapitalized();

    /**
     * Translated "Environment is empty".
     *
     * @return translated "Environment is empty"
     */
    String environmentIsEmpty();

    /**
     * Translated "Pie chart depicting the percentage of total memory in use".
     *
     * @return translated "Pie chart depicting the percentage of total memory in use"
     */
    String pieChartDepictingMemoryInUse();

    /**
     * Translated "Memory in use: {0}% (source: {1})".
     *
     * @return translated "Memory in use: {0}% (source: {1})"
     */
    String memoryInUse(int percentUsed, String providerName);

    /**
     * Translated "Memory Usage".
     *
     * @return translated "Memory Usage"
     */
    String memoryUsage();

    /**
     * Translated "Statistic".
     *
     * @return translated "Statistic"
     */
    String statisticCapitalized();

    /**
     * Translated "Memory".
     *
     * @return translated "Memory"
     */
    String memoryCapitalized();

    /**
     * Translated "Source".
     *
     * @return translated "Source"
     */
    String sourceCapitalized();

    /**
     * Translated "Used by R objects".
     *
     * @return translated "Used by R objects"
     */
    String usedByRObjects();

    /**
     * Translated "Used by session".
     *
     * @return translated "Used by session"
     */
    String usedBySession();

    /**
     * Translated "Used by system".
     *
     * @return translated "Used by system"
     */
    String usedBySystem();

    /**
     * Translated "Free system memory".
     *
     * @return translated "Free system memory"
     */
    String freeSystemMemory();

    /**
     * Translated "Swap space used".
     *
     * @return translated "Swap space used"
     */
    String swapSpaceUsed();

    /**
     * Translated "Total system memory".
     *
     * @return translated "Total system memory"
     */
    String totalSystemMemory();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    String okCapitalized();

    /**
     * Translated "Memory Usage Report ({0}% in use)".
     *
     * @return translated "Memory Usage Report ({0}% in use)"
     */
    String memoryUsageReport(int percentUsed);

    /**
     * Translated "Show Current Memory Usage".
     *
     * @return translated "Show Current Memory Usage"
     */
    String showCurrentMemoryUsage();

    /**
     * Translated "{0} KiB used by R session (source: {1})".
     *
     * @return translated "{0} KiB used by R session (source: {1})"
     */
    String kiBUsedByRSession(String kb, String name);

    /**
     * Translated "Memory in use: none (suspended)".
     *
     * @return translated "Memory in use: none (suspended)"
     */
    String memoryInUseNone();

    /**
     * Translated "Empty pie chart depicting no memory usage".
     *
     * @return translated "Empty pie chart depicting no memory usage"
     */
    String emptyPieChartNoMemoryUsage();

    /**
     * Translated "Preparing data import".
     *
     * @return translated "Preparing data import"
     */
    String preparingDataImportText();

    /**
     * Translated "Session memory limit"
     * 
     * @return translated "Session memory limit"
     */
    String sessionMemoryLimit();

    /**
     * Translated "System memory used"
     *
     * @return translated "System memory used"
     */
    String memoryUsageLimit(String limitMB);

    /**
     * Translated "Session memory used: {0} MiB, {1}. System memory used: {2} out of {3} MiB ({4}% free)."
     * 
     * @return translated "Session memory used: {0} MiB, {1}. System memory used: {2} out of {3} MiB ({4} free%)."
     */
    String memoryUsageStatus(String sessionMem, String limitMessage, String systemMem, String totalMem, String percentFree);


    /*
     * Translated "Session memory used: {0} MiB, {1}.\nFree system memory: {2} MiB ({3}%)."
     *
     * @return "Session memory used: {0} MiB, {1}.\nFree system memory: {2} MiB ({3}%)."
     */
    String multiLineMemoryStatus(String sessionMem, String limitMesage, String freeMemory, String percentFree);

    /**
     * Translated "unlimited"
     *
     * @return translated "unlimited"
     */
    String unlimited();

    /**
     * Translated "MiB"
     *
     * @return translated "MiB"
     */
    String megabytes();

    /**
     * Translated "Workbench limit"
     *
     * @return translated "Workbench limit"
     */
    String workbenchLimit();
}


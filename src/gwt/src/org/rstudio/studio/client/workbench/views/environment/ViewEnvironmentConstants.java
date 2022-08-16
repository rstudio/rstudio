/*
 * ViewEnvironmentConstants.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
    @DefaultMessage("Confirm Remove Objects")
    @Key("confirmRemoveObjects")
    String confirmRemoveObjects();

    /**
     * Translated "Yes".
     *
     * @return translated "Yes"
     */
    @DefaultMessage("Yes")
    @Key("yesCapitalized")
    String yesCapitalized();

    /**
     * Translated "No".
     *
     * @return translated "No"
     */
    @DefaultMessage("No")
    @Key("noCapitalized")
    String noCapitalized();

    /**
     * Translated "all objects".
     *
     * @return translated "all objects"
     */
    @DefaultMessage("all objects")
    @Key("allObjects")
    String allObjects();

    /**
     * Translated "1 object".
     *
     * @return translated "1 object"
     */
    @DefaultMessage("1 object")
    @Key("oneObject")
    String oneObject();

    /**
     * Translated "{0} objects".
     *
     * @return translated "{0} objects"
     */
    @DefaultMessage("{0} objects")
    @Key("multipleObjects")
    String multipleObjects(int numObjects);

    /**
     * Translated "Are you sure you want to remove {0} from the environment? This operation cannot be undone.".
     *
     * @return translated "Are you sure you want to remove {0} from the environment? This operation cannot be undone."
     */
    @DefaultMessage("Are you sure you want to remove {0} from the environment? This operation cannot be undone.")
    @Key("confirmObjectRemove")
    String confirmObjectRemove(String objects);

    /**
     * Translated "Include hidden objects".
     *
     * @return translated "Include hidden objects"
     */
    @DefaultMessage("Include hidden objects")
    @Key("includeHiddenObjects")
    String includeHiddenObjects();

    /**
     * Translated "Environment".
     *
     * @return translated "Environment"
     */
    @DefaultMessage("Environment")
    @Key("environmentCapitalized")
    String environmentCapitalized();

    /**
     * Translated "Environment Tab".
     *
     * @return translated "Environment Tab"
     */
    @DefaultMessage("Environment Tab")
    @Key("environmentTab")
    String environmentTab();

    /**
     * Translated "Refresh Now".
     *
     * @return translated "Refresh Now"
     */
    @DefaultMessage("Refresh Now")
    @Key("refreshNow")
    String refreshNow();

    /**
     * Translated "Refresh options".
     *
     * @return translated "Refresh options"
     */
    @DefaultMessage("Refresh options")
    @Key("refreshOptions")
    String refreshOptions();

    /**
     * Translated "Environment Tab Second".
     *
     * @return translated "Environment Tab Second"
     */
    @DefaultMessage("Environment Tab Second")
    @Key("environmentTabSecond")
    String environmentTabSecond();

    /**
     * Translated "Viewing Python Objects".
     *
     * @return translated "Viewing Python Objects"
     */
    @DefaultMessage("Viewing Python Objects")
    @Key("viewingPythonObjectsCapitalized")
    String viewingPythonObjectsCapitalized();

    /**
     * Translated "Viewing Python objects".
     *
     * @return translated "Viewing Python objects"
     */
    @DefaultMessage("Viewing Python objects")
    @Key("viewingPythonObjects")
    String viewingPythonObjects();

    /**
     * Translated "Search environment".
     *
     * @return translated "Search environment"
     */
    @DefaultMessage("Search environment")
    @Key("searchEnvironment")
    String searchEnvironment();

    /**
     * Translated "Error opening call frame".
     *
     * @return translated "Error opening call frame"
     */
    @DefaultMessage("Error opening call frame")
    @Key("errorOpeningCallFrame")
    String errorOpeningCallFrame();

    /**
     * Translated "Import Dataset".
     *
     * @return translated "Import Dataset"
     */
    @DefaultMessage("Import Dataset")
    @Key("importDataset")
    String importDataset();

    /**
     * Translated "Could not change monitoring state".
     *
     * @return translated "Could not change monitoring state"
     */
    @DefaultMessage("Could not change monitoring state")
    @Key("couldNotChangeMonitoringState")
    String couldNotChangeMonitoringState();

    /**
     * Translated "Import".
     *
     * @return translated "Import"
     */
    @DefaultMessage("Import")
    @Key("importCapitalized")
    String importCapitalized();

    /**
     * Translated "Removing objects...".
     *
     * @return translated "Removing objects..."
     */
    @DefaultMessage("Removing objects...")
    @Key("removingObjectsEllipses")
    String removingObjectsEllipses();

    /**
     * Translated "Save Workspace As".
     *
     * @return translated "Save Workspace As"
     */
    @DefaultMessage("Save Workspace As")
    @Key("saveWorkspaceAs")
    String saveWorkspaceAs();

    /**
     * Translated "Load Workspace".
     *
     * @return translated "Load Workspace"
     */
    @DefaultMessage("Load Workspace")
    @Key("loadWorkspace")
    String loadWorkspace();

    /**
     * Translated "Select File to Import".
     *
     * @return translated "Select File to Import"
     */
    @DefaultMessage("Select File to Import")
    @Key("selectFileToImport")
    String selectFileToImport();

    /**
     * Translated "Import from Web URL".
     *
     * @return translated "Import from Web URL"
     */
    @DefaultMessage("Import from Web URL")
    @Key("importFromWebURL")
    String importFromWebURL();

    /**
     * Translated "Please enter the URL to import data from:".
     *
     * @return translated "Please enter the URL to import data from:"
     */
    @DefaultMessage("Please enter the URL to import data from:")
    @Key("pleaseEnterURLToImportDataFrom")
    String pleaseEnterURLToImportDataFrom();

    /**
     * Translated "Downloading data...".
     *
     * @return translated "Downloading data..."
     */
    @DefaultMessage("Downloading data...")
    @Key("downloadingDataEllipses")
    String downloadingDataEllipses();

    /**
     * Translated "Load R Object".
     *
     * @return translated "Load R Object"
     */
    @DefaultMessage("Load R Object")
    @Key("loadRObject")
    String loadRObject();

    /**
     * Translated "Load ''{0}'' into an R object named:".
     *
     * @return translated "Load ''{0}'' into an R object named:"
     */
    @DefaultMessage("Load ''{0}'' into an R object named:")
    @Key("loadDataIntoAnRObject")
    String loadDataIntoAnRObject(String dataFilePath);

    /**
     * Translated "Confirm Load RData".
     *
     * @return translated "Confirm Load RData"
     */
    @DefaultMessage("Confirm Load RData")
    @Key("confirmLoadRData")
    String confirmLoadRData();

    /**
     * Translated "Do you want to load the R data file \"{0}\" into your global environment?".
     *
     * @return translated "Do you want to load the R data file \"{0}\" into your global environment?"
     */
    @DefaultMessage("Do you want to load the R data file \"{0}\" into your global environment?")
    @Key("loadRDataFileIntoGlobalEnv")
    String loadRDataFileIntoGlobalEnv(String dataFilePath);

    /**
     * Translated "Error Listing Objects".
     *
     * @return translated "Error Listing Objects"
     */
    @DefaultMessage("Error Listing Objects")
    @Key("errorListingObjects")
    String errorListingObjects();

    /**
     * Translated "Code Creation Error".
     *
     * @return translated "Code Creation Error"
     */
    @DefaultMessage("Code Creation Error")
    @Key("codeCreationError")
    String codeCreationError();

    /**
     * Translated "Data Preview".
     *
     * @return translated "Data Preview"
     */
    @DefaultMessage("Data Preview")
    @Key("dataPreview")
    String dataPreview();

    /**
     * Translated "Is this a valid CSV file?\n\n".
     *
     * @return translated "Is this a valid CSV file?\n\n"
     */
    @DefaultMessage("Is this a valid CSV file?\n\n")
    @Key("isThisAValidCSVFile")
    String isThisAValidCSVFile();

    /**
     * Translated "Is this a valid SPSS, SAS or STATA file?\n\n".
     *
     * @return translated "Is this a valid SPSS, SAS or STATA file?\n\n"
     */
    @DefaultMessage("Is this a valid SPSS, SAS or STATA file?\n\n")
    @Key("isThisAValidSpssSasSataFile")
    String isThisAValidSpssSasSataFile();

    /**
     * Translated "Is this a valid Excel file?\n\n".
     *
     * @return translated "Is this a valid Excel file?\n\n"
     */
    @DefaultMessage("Is this a valid Excel file?\n\n")
    @Key("isThisAValidExcelFile")
    String isThisAValidExcelFile();

    /**
     * Translated "Copy Code Preview".
     *
     * @return translated "Copy Code Preview"
     */
    @DefaultMessage("Copy Code Preview")
    @Key("copyCodePreview")
    String copyCodePreview();

    /**
     * Translated "Please enter the format string".
     *
     * @return translated "Please enter the format string"
     */
    @DefaultMessage("Please enter the format string")
    @Key("pleaseEnterFormatString")
    String pleaseEnterFormatString();

    /**
     * Translated "Please insert a comma separated list of factors".
     *
     * @return translated "Please insert a comma separated list of factors"
     */
    @DefaultMessage("Please insert a comma separated list of factors")
    @Key("pleaseInsertACommaSeparatedList")
    String pleaseInsertACommaSeparatedList();

    /**
     * Translated "Date Format".
     *
     * @return translated "Date Format"
     */
    @DefaultMessage("Date Format")
    @Key("dateFormat")
    String dateFormat();

    /**
     * Translated "Time Format".
     *
     * @return translated "Time Format"
     */
    @DefaultMessage("Time Format")
    @Key("timeFormat")
    String timeFormat();

    /**
     * Translated "Date and Time Format".
     *
     * @return translated "Date and Time Format"
     */
    @DefaultMessage("Date and Time Format")
    @Key("dateAndTimeFormat")
    String dateAndTimeFormat();

    /**
     * Translated "Factors".
     *
     * @return translated "Factors"
     */
    @DefaultMessage("Factors")
    @Key("factors")
    String factors();

    /**
     * Translated "All columns must have names in order to perform column operations.".
     *
     * @return translated "All columns must have names in order to perform column operations."
     */
    @DefaultMessage("All columns must have names in order to perform column operations.")
    @Key("allColumnsMustHaveName")
    String allColumnsMustHaveName();

    /**
     * Translated "Retrieving preview data...".
     *
     * @return translated "Retrieving preview data..."
     */
    @DefaultMessage("Retrieving preview data...")
    @Key("retrievingPreviewDataEllipses")
    String retrievingPreviewDataEllipses();

    /**
     * Translated "Previewing first {0} entries. {1} parsing errors.".
     *
     * @return translated "Previewing first {0} entries. {1} parsing errors."
     */
    @DefaultMessage("Previewing first {0} entries. {1} parsing errors.")
    @Key("previewingFirstEntriesMultiple")
    String previewingFirstEntriesMultiple(String rows, String parsingErrors);

    /**
     * Translated "Previewing first {0} entries. ".
     *
     * @return translated "Previewing first {0} entries. "
     */
    @DefaultMessage("Previewing first {0} entries. ")
    @Key("previewingFirstEntriesNone")
    String previewingFirstEntriesNone(String rows);

    /**
     * Translated "Browse".
     *
     * @return translated "Browse"
     */
    @DefaultMessage("Browse")
    @Key("browseCapitalized")
    String browseCapitalized();

    /**
     * Translated "Update".
     *
     * @return translated "Update"
     */
    @DefaultMessage("Update")
    @Key("updateCapitalized")
    String updateCapitalized();

    /**
     * Translated "{0} for {1}".
     * "Update for ariaLabelSuffix"
     * @return translated "{0} for {1}"
     */
    @DefaultMessage("{0} for {1}")
    @Key("updateButtonAriaLabel")
    String updateButtonAriaLabel(String caption, String ariaLabelSuffix);

    /**
     * Translated "{0} for {1}...".
     * "Browse for ariaLabelSuffix..."
     * @return translated "{0} for {1}..."
     */
    @DefaultMessage("{0} for {1}...")
    @Key("browseButtonAriaLabel")
    String browseButtonAriaLabel(String caption, String ariaLabelSuffix);

    /**
     * Translated "Choose File".
     *
     * @return translated "Choose File"
     */
    @DefaultMessage("Choose File")
    @Key("chooseFile")
    String chooseFile();

    /**
     * Translated "Reading rectangular data using readr".
     *
     * @return translated "Reading rectangular data using readr"
     */
    @DefaultMessage("Reading rectangular data using readr")
    @Key("readingRectangularDataUsingReadr")
    String readingRectangularDataUsingReadr();

    /**
     * Translated "Configure Locale".
     *
     * @return translated "Configure Locale"
     */
    @DefaultMessage("Configure Locale")
    @Key("configureLocale")
    String configureLocale();

    /**
     * Translated "Character {0}".
     *
     * @return translated "Character {0}"
     */
    @DefaultMessage("Character {0}")
    @Key("characterOtherDelimiter")
    String characterOtherDelimiter(String otherDelimiter);

    /**
     * Translated "Incorrect Delimiter".
     *
     * @return translated "Incorrect Delimiter"
     */
    @DefaultMessage("Incorrect Delimiter")
    @Key("incorrectDelimiter")
    String incorrectDelimiter();

    /**
     * Translated "The specified delimiter is not valid.".
     *
     * @return translated "The specified delimiter is not valid."
     */
    @DefaultMessage("The specified delimiter is not valid.")
    @Key("specifiedDelimiterNotValid")
    String specifiedDelimiterNotValid();

    /**
     * Translated "Please enter a single character delimiter.".
     *
     * @return translated "Please enter a single character delimiter."
     */
    @DefaultMessage("Please enter a single character delimiter.")
    @Key("enterSingleCharacterDelimiter")
    String enterSingleCharacterDelimiter();

    /**
     * Translated "Other Delimiter".
     *
     * @return translated "Other Delimiter"
     */
    @DefaultMessage("Other Delimiter")
    @Key("otherDelimiter")
    String otherDelimiter();

    /**
     * Translated "Default".
     *
     * @return translated "Default"
     */
    @DefaultMessage("Default")
    @Key("defaultCapitalized")
    String defaultCapitalized();

    /**
     * Translated "empty".
     *
     * @return translated "empty"
     */
    @DefaultMessage("empty")
    @Key("empty")
    String empty();

    /**
     * Translated "NA".
     *
     * @return translated "NA"
     */
    @DefaultMessage("NA")
    @Key("notApplicableAbbreviation")
    String notApplicableAbbreviation();

    /**
     * Translated "null".
     *
     * @return translated "null"
     */
    @DefaultMessage("null")
    @Key("nullWord")
    String nullWord();

    /**
     * Translated "None".
     *
     * @return translated "None"
     */
    @DefaultMessage("None")
    @Key("noneCapitalized")
    String noneCapitalized();

    /**
     * Translated "Double (\")".
     *
     * @return translated "Double (\")"
     */
    @DefaultMessage("Double (\")")
    @Key("doubleQuotesParentheses")
    String doubleQuotesParentheses();

    /**
     * Translated "Single ('')".
     *
     * @return translated "Single ('')"
     */
    @DefaultMessage("Single ('')")
    @Key("singleQuoteParentheses")
    String singleQuoteParentheses();

    /**
     * Translated "Other...".
     *
     * @return translated "Other..."
     */
    @DefaultMessage("Other...")
    @Key("otherEllipses")
    String otherEllipses();

    /**
     * Translated "Whitespace".
     *
     * @return translated "Whitespace"
     */
    @DefaultMessage("Whitespace")
    @Key("whitespaceCapitalized")
    String whitespaceCapitalized();

    /**
     * Translated "Tab".
     *
     * @return translated "Tab"
     */
    @DefaultMessage("Tab")
    @Key("tabCapitalized")
    String tabCapitalized();

    /**
     * Translated "Semicolon".
     *
     * @return translated "Semicolon"
     */
    @DefaultMessage("Semicolon")
    @Key("semicolonCapitalized")
    String semicolonCapitalized();

    /**
     * Translated "Comma".
     *
     * @return translated "Comma"
     */
    @DefaultMessage("Comma")
    @Key("commaCapitalized")
    String commaCapitalized();

    /**
     * Translated "Both".
     *
     * @return translated "Both"
     */
    @DefaultMessage("Both")
    @Key("bothCapitalized")
    String bothCapitalized();

    /**
     * Translated "Double".
     *
     * @return translated "Double"
     */
    @DefaultMessage("Double")
    @Key("doubleCapitalized")
    String doubleCapitalized();

    /**
     * Translated "Backslash".
     *
     * @return translated "Backslash"
     */
    @DefaultMessage("Backslash")
    @Key("backslashCapitalized")
    String backslashCapitalized();

    /**
     * Translated "Configure".
     *
     * @return translated "Configure"
     */
    @DefaultMessage("Configure")
    @Key("configureCapitalized")
    String configureCapitalized();

    /**
     * Translated "Locales in readr".
     *
     * @return translated "Locales in readr"
     */
    @DefaultMessage("Locales in readr")
    @Key("localesInReadr")
    String localesInReadr();

    /**
     * Translated "Encoding Identifier".
     *
     * @return translated "Encoding Identifier"
     */
    @DefaultMessage("Encoding Identifier")
    @Key("encodingIdentifier")
    String encodingIdentifier();

    /**
     * Translated "Please enter an encoding identifier. For a list of valid encodings run iconvlist().".
     *
     * @return translated "Please enter an encoding identifier. For a list of valid encodings run iconvlist()."
     */
    @DefaultMessage("Please enter an encoding identifier. For a list of valid encodings run iconvlist().")
    @Key("enterAnEncodingIdentifier")
    String enterAnEncodingIdentifier();

    /**
     * Translated "Reading data using haven".
     *
     * @return translated "Reading data using haven"
     */
    @DefaultMessage("Reading data using haven")
    @Key("readingDataUsingHaven")
    String readingDataUsingHaven();

    /**
     * Translated "Reading Excel files using readxl".
     *
     * @return translated "Reading Excel files using readxl"
     */
    @DefaultMessage("Reading Excel files using readxl")
    @Key("readingExcelFilesUsingReadxl")
    String readingExcelFilesUsingReadxl();

    /**
     * Translated "Import Excel Data".
     *
     * @return translated "Import Excel Data"
     */
    @DefaultMessage("Import Excel Data")
    @Key("importExcelData")
    String importExcelData();

    /**
     * Translated "Import Statistical Data".
     *
     * @return translated "Import Statistical Data"
     */
    @DefaultMessage("Import Statistical Data")
    @Key("importStatisticalData")
    String importStatisticalData();

    /**
     * Translated "Import Text Data".
     *
     * @return translated "Import Text Data"
     */
    @DefaultMessage("Import Text Data")
    @Key("importTextData")
    String importTextData();

    /**
     * Translated "Period".
     *
     * @return translated "Period"
     */
    @DefaultMessage("Period")
    @Key("periodCapitalized")
    String periodCapitalized();

    /**
     * Translated "Automatic".
     *
     * @return translated "Automatic"
     */
    @DefaultMessage("Automatic")
    @Key("automaticCapitalized")
    String automaticCapitalized();

    /**
     * Translated "Use first column".
     *
     * @return translated "Use first column"
     */
    @DefaultMessage("Use first column")
    @Key("useFirstColumn")
    String useFirstColumn();

    /**
     * Translated "Use numbers".
     *
     * @return translated "Use numbers"
     */
    @DefaultMessage("Use numbers")
    @Key("useNumbers")
    String useNumbers();

    /**
     * Translated "Updating preview".
     *
     * @return translated "Updating preview"
     */
    @DefaultMessage("Updating preview")
    @Key("updatingPreview")
    String updatingPreview();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCapitalized")
    String errorCapitalized();

    /**
     * Translated "Detecting data format".
     *
     * @return translated "Detecting data format"
     */
    @DefaultMessage("Detecting data format")
    @Key("detectingDataFormat")
    String detectingDataFormat();

    /**
     * Translated "Variable Name Is Required".
     *
     * @return translated "Variable Name Is Required"
     */
    @DefaultMessage("Variable Name Is Required")
    @Key("variableNameIsRequired")
    String variableNameIsRequired();

    /**
     * Translated "Please provide a variable name.".
     *
     * @return translated "Please provide a variable name."
     */
    @DefaultMessage("Please provide a variable name.")
    @Key("pleaseProvideAVariableName")
    String pleaseProvideAVariableName();

    /**
     * Translated "Unknown".
     *
     * @return translated "Unknown"
     */
    @DefaultMessage("Unknown")
    @Key("unknownCapitalized")
    String unknownCapitalized();

    /**
     * Translated "MacOS System".
     *
     * @return translated "MacOS System"
     */
    @DefaultMessage("MacOS System")
    @Key("macOsSystem")
    String macOsSystem();

    /**
     * Translated "Windows System".
     *
     * @return translated "Windows System"
     */
    @DefaultMessage("Windows System")
    @Key("windowsSystem")
    String windowsSystem();

    /**
     * Translated "[Shiny: {0}]".
     *
     * @return translated "[Shiny: {0}]"
     */
    @DefaultMessage("[Shiny: {0}]")
    @Key("shinyFunctionLabel")
    String shinyFunctionLabel(String functionLabel);

    /**
     * Translated "[Debug source]".
     *
     * @return translated "[Debug source]"
     */
    @DefaultMessage("[Debug source]")
    @Key("debugSourceBrackets")
    String debugSourceBrackets();

    /**
     * Translated "{0} at {1}:{2}".
     *
     * @return translated "{0} at {1}:{2}"
     */
    @DefaultMessage("{0} at {1}:{2}")
    @Key("fileLocationAtLine")
    String fileLocationAtLine(String emptyString, String fileName, int lineNumber);

    /**
     * Translated "Traceback".
     *
     * @return translated "Traceback"
     */
    @DefaultMessage("Traceback")
    @Key("tracebackCapitalized")
    String tracebackCapitalized();

    /**
     * Translated "Show internals".
     *
     * @return translated "Show internals"
     */
    @DefaultMessage("Show internals")
    @Key("showInternals")
    String showInternals();

    /**
     * Translated "Select all".
     *
     * @return translated "Select all"
     */
    @DefaultMessage("Select all")
    @Key("selectAll")
    String selectAll();

    /**
     * Translated "Name".
     *
     * @return translated "Name"
     */
    @DefaultMessage("Name")
    @Key("nameCapitalized")
    String nameCapitalized();

    /**
     * Translated "Type".
     *
     * @return translated "Type"
     */
    @DefaultMessage("Type")
    @Key("typeCapitalized")
    String typeCapitalized();

    /**
     * Translated "Length".
     *
     * @return translated "Length"
     */
    @DefaultMessage("Length")
    @Key("lengthCapitalized")
    String lengthCapitalized();

    /**
     * Translated "Size".
     *
     * @return translated "Size"
     */
    @DefaultMessage("Size")
    @Key("sizeCapitalized")
    String sizeCapitalized();

    /**
     * Translated "Value".
     *
     * @return translated "Value"
     */
    @DefaultMessage("Value")
    @Key("valueCapitalized")
    String valueCapitalized();

    /**
     * Translated "Collapse Object".
     *
     * @return translated "Collapse Object"
     */
    @DefaultMessage("Collapse Object")
    @Key("collapseObject")
    String collapseObject();

    /**
     * Translated "Expand Object".
     *
     * @return translated "Expand Object"
     */
    @DefaultMessage("Expand Object")
    @Key("expandObject")
    String expandObject();

    /**
     * Translated "Has Trace".
     *
     * @return translated "Has Trace"
     */
    @DefaultMessage("Has Trace")
    @Key("hasTrace")
    String hasTrace();

    /**
     * Translated ", {0} bytes".
     *
     * @return translated ", {0} bytes"
     */
    @DefaultMessage(", {0} bytes")
    @Key("sizeBytes")
    String sizeBytes(int size);

    /**
     * Translated "{0} ({1} {2})".
     *
     * @return translated "{0} ({1} {2})"
     */
    @DefaultMessage("{0} ({1} {2})")
    @Key("buildNameColumnTitle")
    String buildNameColumnTitle(String name, String type, String size);

    /**
     * Translated "{0} (unevaluated promise)".
     *
     * @return translated "{0} (unevaluated promise)"
     */
    @DefaultMessage("{0} (unevaluated promise)")
    @Key("unevaluatedPromise")
    String unevaluatedPromise(String emptyString);

    /**
     * Translated "Data".
     *
     * @return translated "Data"
     */
    @DefaultMessage("Data")
    @Key("dataCapitalized")
    String dataCapitalized();

    /**
     * Translated "Functions".
     *
     * @return translated "Functions"
     */
    @DefaultMessage("Functions")
    @Key("functionsCapitalized")
    String functionsCapitalized();

    /**
     * Translated "Values".
     *
     * @return translated "Values"
     */
    @DefaultMessage("Values")
    @Key("valuesCapitalized")
    String valuesCapitalized();

    /**
     * Translated "Environment is empty".
     *
     * @return translated "Environment is empty"
     */
    @DefaultMessage("Environment is empty")
    @Key("environmentIsEmpty")
    String environmentIsEmpty();

    /**
     * Translated "Pie chart depicting the percentage of total memory in use".
     *
     * @return translated "Pie chart depicting the percentage of total memory in use"
     */
    @DefaultMessage("Pie chart depicting the percentage of total memory in use")
    @Key("pieChartDepictingMemoryInUse")
    String pieChartDepictingMemoryInUse();

    /**
     * Translated "Memory in use: {0}% (source: {1})".
     *
     * @return translated "Memory in use: {0}% (source: {1})"
     */
    @DefaultMessage("Memory in use: {0}% (source: {1})")
    @Key("memoryInUse")
    String memoryInUse(int percentUsed, String providerName);

    /**
     * Translated "Memory Usage".
     *
     * @return translated "Memory Usage"
     */
    @DefaultMessage("Memory Usage")
    @Key("memoryUsage")
    String memoryUsage();

    /**
     * Translated "Statistic".
     *
     * @return translated "Statistic"
     */
    @DefaultMessage("Statistic")
    @Key("statisticCapitalized")
    String statisticCapitalized();

    /**
     * Translated "Memory".
     *
     * @return translated "Memory"
     */
    @DefaultMessage("Memory")
    @Key("memoryCapitalized")
    String memoryCapitalized();

    /**
     * Translated "Source".
     *
     * @return translated "Source"
     */
    @DefaultMessage("Source")
    @Key("sourceCapitalized")
    String sourceCapitalized();

    /**
     * Translated "Used by R objects".
     *
     * @return translated "Used by R objects"
     */
    @DefaultMessage("Used by R objects")
    @Key("usedByRObjects")
    String usedByRObjects();

    /**
     * Translated "Used by session".
     *
     * @return translated "Used by session"
     */
    @DefaultMessage("Used by session")
    @Key("usedBySession")
    String usedBySession();

    /**
     * Translated "Used by system".
     *
     * @return translated "Used by system"
     */
    @DefaultMessage("Used by system")
    @Key("usedBySystem")
    String usedBySystem();

    /**
     * Translated "Free system memory".
     *
     * @return translated "Free system memory"
     */
    @DefaultMessage("Free system memory")
    @Key("freeSystemMemory")
    String freeSystemMemory();

    /**
     * Translated "Total system memory".
     *
     * @return translated "Total system memory"
     */
    @DefaultMessage("Total system memory")
    @Key("totalSystemMemory")
    String totalSystemMemory();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    @Key("okCapitalized")
    String okCapitalized();

    /**
     * Translated "Memory Usage Report ({0}% in use)".
     *
     * @return translated "Memory Usage Report ({0}% in use)"
     */
    @DefaultMessage("Memory Usage Report ({0}% in use)")
    @Key("memoryUsageReport")
    String memoryUsageReport(int percentUsed);

    /**
     * Translated "Show Current Memory Usage".
     *
     * @return translated "Show Current Memory Usage"
     */
    @DefaultMessage("Show Current Memory Usage")
    @Key("showCurrentMemoryUsage")
    String showCurrentMemoryUsage();

    /**
     * Translated "{0} KiB used by R session (source: {1})".
     *
     * @return translated "{0} KiB used by R session (source: {1})"
     */
    @DefaultMessage("{0} KiB used by R session (source: {1})")
    @Key("kiBUsedByRSession")
    String kiBUsedByRSession(String kb, String name);

    /**
     * Translated "Memory in use: none (suspended)".
     *
     * @return translated "Memory in use: none (suspended)"
     */
    @DefaultMessage("Memory in use: none (suspended)")
    @Key("memoryInUseNone")
    String memoryInUseNone();

    /**
     * Translated "Empty pie chart depicting no memory usage".
     *
     * @return translated "Empty pie chart depicting no memory usage"
     */
    @DefaultMessage("Empty pie chart depicting no memory usage")
    @Key("emptyPieChartNoMemoryUsage")
    String emptyPieChartNoMemoryUsage();

    /**
     * Translated "include".
     *
     * @return translated "include"
     */
    @DefaultMessage("include")
    @Key("includeMenuItem")
    String includeMenuItem();

    /**
     * Translated "skip".
     *
     * @return translated "skip"
     */
    @DefaultMessage("skip")
    @Key("skipMenuItem")
    String skipMenuItem();

    /**
     * Translated "only".
     *
     * @return translated "only"
     */
    @DefaultMessage("only")
    @Key("onlyMenuItem")
    String onlyMenuItem();

    /**
     * Translated "guess".
     *
     * @return translated "guess"
     */
    @DefaultMessage("guess")
    @Key("guessMenuItem")
    String guessMenuItem();

    /**
     * Translated "character".
     *
     * @return translated "character"
     */
    @DefaultMessage("character")
    @Key("characterMenuItem")
    String characterMenuItem();

    /**
     * Translated "double".
     *
     * @return translated "double"
     */
    @DefaultMessage("double")
    @Key("doubleMenuItem")
    String doubleMenuItem();

    /**
     * Translated "integer".
     *
     * @return translated "integer"
     */
    @DefaultMessage("integer")
    @Key("integerMenuItem")
    String integerMenuItem();

    /**
     * Translated "numeric".
     *
     * @return translated "numeric"
     */
    @DefaultMessage("numeric")
    @Key("numericMenuItem")
    String numericMenuItem();

    /**
     * Translated "logical".
     *
     * @return translated "logical"
     */
    @DefaultMessage("logical")
    @Key("logicalMenuItem")
    String logicalMenuItem();

    /**
     * Translated "date".
     *
     * @return translated "date"
     */
    @DefaultMessage("date")
    @Key("dateMenuItem")
    String dateMenuItem();

    /**
     * Translated "time".
     *
     * @return translated "time"
     */
    @DefaultMessage("time")
    @Key("timeMenuItem")
    String timeMenuItem();

    /**
     * Translated "dateTime".
     *
     * @return translated "dateTime"
     */
    @DefaultMessage("dateTime")
    @Key("dateTimeMenuItem")
    String dateTimeMenuItem();

    /**
     * Translated "factor".
     *
     * @return translated "factor"
     */
    @DefaultMessage("factor")
    @Key("factorMenuItem")
    String factorMenuItem();

    /**
     * Translated "Preparing data import".
     *
     * @return translated "Preparing data import"
     */
    @DefaultMessage("Preparing data import")
    @Key("preparingDataImportText")
    String preparingDataImportText();



}


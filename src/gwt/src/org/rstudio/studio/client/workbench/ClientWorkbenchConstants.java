package org.rstudio.studio.client.workbench;
/*
 * ClientWorkbenchConstants.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
public interface ClientWorkbenchConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Addins".
     *
     * @return translated "Addins"
     */
    @DefaultMessage("Addins")
    @Key("addinCaption")
    String addinCaption();

    /**
     * Translated "Execute".
     *
     * @return translated "Execute"
     */
    @DefaultMessage("Execute")
    @Key("executeButtonLabel")
    String executeButtonLabel();

    /**
     * Translated "Using RStudio Addins".
     *
     * @return translated "Using RStudio Addins"
     */
    @DefaultMessage("Using RStudio Addins")
    @Key("rstudioAddinsCaption")
    String rstudioAddinsCaption();

    /**
     * Translated "Loading addins...".
     *
     * @return translated "Loading addins..."
     */
    @DefaultMessage("Loading addins...")
    @Key("loadingAddinsCaption")
    String loadingAddinsCaption();

    /**
     * Translated "No addins available".
     *
     * @return translated "No addins available"
     */
    @DefaultMessage("No addins available")
    @Key("noAddinsAvailableCaption")
    String noAddinsAvailableCaption();

    /**
     * Translated "Keyboard Shortcuts...".
     *
     * @return translated "Keyboard Shortcuts..."
     */
    @DefaultMessage("Keyboard Shortcuts...")
    @Key("keyboardShortcutsTitle")
    String keyboardShortcutsTitle();

    /**
     * Translated "Filter addins:".
     *
     * @return translated "Filter addins:"
     */
    @DefaultMessage("Filter addins:")
    @Key("filterAddinsText")
    String filterAddinsText();

    /**
     * Translated "Package".
     *
     * @return translated "Package"
     */
    @DefaultMessage("Package")
    @Key("packageTextHeader")
    String packageTextHeader();

    /**
     * Translated "Name".
     *
     * @return translated "Name"
     */
    @DefaultMessage("Name")
    @Key("nameTextHeader")
    String nameTextHeader();

    /**
     * Translated "Description".
     *
     * @return translated "Description"
     */
    @DefaultMessage("Description")
    @Key("descTextHeader")
    String descTextHeader();

    /**
     * Translated "Description".
     *
     * @return translated "Description"
     */
    @DefaultMessage("Description")
    @Key("foundAddinsMessage")
    String foundAddinsMessage(int size, String query);

    /**
     * Translated "You are {0} over your {1} file storage limit. Please remove files to continue working.".
     *
     * @return translated "You are {0} over your {1} file storage limit. Please remove files to continue working."
     */
    @DefaultMessage("You are {0} over your {1} file storage limit. Please remove files to continue working.")
    @Key("onQuotaMessage")
    String onQuotaMessage(String fileSize, String quota);

    /**
     * Translated "You are nearly over your {0} file storage limit.".
     *
     * @return translated "You are nearly over your {0} file storage limit."
     */
    @DefaultMessage("You are nearly over your {0} file storage limit.")
    @Key("quotaStatusMessage")
    String quotaStatusMessage(String quota);

    /**
     * Translated "Choose Working Directory".
     *
     * @return translated "Choose Working Directory"
     */
    @DefaultMessage("Choose Working Directory")
    @Key("chooseWorkingDirCaption")
    String chooseWorkingDirCaption();

    /**
     * Translated "Source File".
     *
     * @return translated "Source File"
     */
    @DefaultMessage("Source File")
    @Key("sourceFileCaption")
    String sourceFileCaption();

    /**
     * Translated "Reading RSA public key...".
     *
     * @return translated "Reading RSA public key..."
     */
    @DefaultMessage("Reading RSA public key...")
    @Key("rsaKeyProgressMessage")
    String rsaKeyProgressMessage();

    /**
     * Translated "RSA Public Key".
     *
     * @return translated "RSA Public Key"
     */
    @DefaultMessage("RSA Public Key")
    @Key("rsaPublicKeyCaption")
    String rsaPublicKeyCaption();

    /**
     * Translated "Error attempting to read key ''{0}' ({1})".
     *
     * @return translated "Error attempting to read key ''{0}' ({1})"
     */
    @DefaultMessage("Error attempting to read key ''{0}' ({1})")
    @Key("onErrorReadKey")
    String onErrorReadKey(String keyPath, String userMessage);

    /**
     * Translated "May we upload crash reports to RStudio automatically?\n\nCrash reports don't include any personal information, except for IP addresses which are used to determine how many users are affected by each crash.\n\nCrash reporting can be disabled at any time under the Global Options.".
     *
     * @return translated "May we upload crash reports to RStudio automatically?\n\nCrash reports don't include any personal information, except for IP addresses which are used to determine how many users are affected by each crash.\n\nCrash reporting can be disabled at any time under the Global Options."
     */
    @DefaultMessage("May we upload crash reports to RStudio automatically?\\n\\nCrash reports don't include any personal information, except for IP addresses which are used to determine how many users are affected by each crash.\\n\\nCrash reporting can be disabled at any time under the Global Options.")
    @Key("checkForCrashHandlerPermissionMessage")
    String checkForCrashHandlerPermissionMessage();

    /**
     * Translated "Enable Automated Crash Reporting".
     *
     * @return translated "Enable Automated Crash Reporting"
     */
    @DefaultMessage("Enable Automated Crash Reporting")
    @Key("enableCrashReportingCaption")
    String enableCrashReportingCaption();

    /**
     * Translated "No".
     *
     * @return translated "No"
     */
    @DefaultMessage("No")
    @Key("noLabel")
    String noLabel();

    /**
     * Translated "No".
     *
     * @return translated "Yes"
     */
    @DefaultMessage("Yes")
    @Key("yesLabel")
    String yesLabel();

    /**
     * Translated "Admin Notification".
     *
     * @return translated "Admin Notification"
     */
    @DefaultMessage("Admin Notification")
    @Key("adminNotificationCaption")
    String adminNotificationCaption();

    /**
     * Translated "Unable to execute {0} addin\n(R session is currently busy)".
     *
     * @return translated "Unable to execute {0} addin\n(R session is currently busy)"
     */
    @DefaultMessage("Unable to execute {0} addin\\n(R session is currently busy)")
    @Key("isServerBusyMessage")
    String isServerBusyMessage(String name);

    /**
     * Translated "Error Executing Addin".
     *
     * @return translated "Error Executing Addin"
     */
    @DefaultMessage("Error Executing Addin")
    @Key("executingAddinError")
    String executingAddinError();

    /**
     * Translated "Code Search Error".
     *
     * @return translated "Code Search Error"
     */
    @DefaultMessage("Code Search Error")
    @Key("codeSearchError")
    String codeSearchError();

    /**
     * Translated "Go to File/Function".
     *
     * @return translated "Go to File/Function"
     */
    @DefaultMessage("Go to File/Function")
    @Key("fileFunctionLabel")
    String fileFunctionLabel();

    /**
     * Translated "Filter by file or function name".
     *
     * @return translated "Filter by file or function name"
     */
    @DefaultMessage("Filter by file or function name")
    @Key("codeSearchLabel")
    String codeSearchLabel();

    /**
     * Translated "Go to file/function".
     *
     * @return translated "Go to file/function"
     */
    @DefaultMessage("Go to file/function")
    @Key("textBoxWithCue")
    String textBoxWithCue();

    /**
     * Translated "Height:".
     *
     * @return translated "Height:"
     */
    @DefaultMessage("Height:")
    @Key("heightText")
    String heightText();

    /**
     * Translated "Maintain aspect ratio".
     *
     * @return translated "Maintain aspect ratio"
     */
    @DefaultMessage("Maintain aspect ratio")
    @Key("maintainAspectRatioText")
    String maintainAspectRatioText();

    /**
     * Translated "Update Preview".
     *
     * @return translated "Update Preview"
     */
    @DefaultMessage("Update Preview")
    @Key("updatePreviewTitle")
    String updatePreviewTitle();

    /**
     * Translated "Save Plot as Image".
     *
     * @return translated "Save Plot as Image"
     */
    @DefaultMessage("Save Plot as Image")
    @Key("savePlotAsImageText")
    String savePlotAsImageText();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    @DefaultMessage("Save")
    @Key("saveTitle")
    String saveTitle();

    /**
     * Translated "View plot after saving".
     *
     * @return translated "View plot after saving"
     */
    @DefaultMessage("View plot after saving")
    @Key("viewAfterSaveCheckBoxTitle")
    String viewAfterSaveCheckBoxTitle();

    /**
     * Translated "File Name Required".
     *
     * @return translated "File Name Required"
     */
    @DefaultMessage("File Name Required")
    @Key("fileNameRequiredCaption")
    String fileNameRequiredCaption();

    /**
     * Translated "You must provide a file name for the plot image.".
     *
     * @return translated "You must provide a file name for the plot image."
     */
    @DefaultMessage("You must provide a file name for the plot image.")
    @Key("fileNameRequiredMessage")
    String fileNameRequiredMessage();

    /**
     * Translated "Image format:".
     *
     * @return translated "Image format:"
     */
    @DefaultMessage("Image format:")
    @Key("imageFormatLabel")
    String imageFormatLabel();

    /**
     * Translated "Directory...".
     *
     * @return translated "Directory..."
     */
    @DefaultMessage("Directory...")
    @Key("directoryButtonTitle")
    String directoryButtonTitle();

    /**
     * Translated "Choose Directory".
     *
     * @return translated "Choose Directory"
     */
    @DefaultMessage("Choose Directory")
    @Key("chooseDirectoryCaption")
    String chooseDirectoryCaption();

    /**
     * Translated "Selected Directory".
     *
     * @return translated "Selected Directory"
     */
    @DefaultMessage("Selected Directory")
    @Key("selectedDirectoryLabel")
    String selectedDirectoryLabel();

    /**
     * Translated "File name:".
     *
     * @return translated "File name:"
     */
    @DefaultMessage("File name:")
    @Key("fileNameText")
    String fileNameText();

    /**
     * Translated "Copy Plot to Clipboard".
     *
     * @return translated "Copy Plot to Clipboard"
     */
    @DefaultMessage("Copy Plot to Clipboard")
    @Key("copyPlotText")
    String copyPlotText();

    /**
     * Translated "Copy Plot".
     *
     * @return translated "Copy Plot"
     */
    @DefaultMessage("Copy Plot")
    @Key("copyButtonText")
    String copyButtonText();

    /**
     * Translated "Copy as:".
     *
     * @return translated "Copy as:"
     */
    @DefaultMessage("Copy as:")
    @Key("copyAsText")
    String copyAsText();

    /**
     * Translated "Format".
     *
     * @return translated "Format"
     */
    @DefaultMessage("Format")
    @Key("formatName")
    String formatName();

    /**
     * Translated "Create Session".
     *
     * @return translated "Create Session"
     */
    @DefaultMessage("Create Session")
    @Key("createSessionCaption")
    String createSessionCaption();

    /**
     * Translated "Could not allocate a new session.".
     *
     * @return translated "Could not allocate a new session."
     */
    @DefaultMessage("Could not allocate a new session.")
    @Key("createSessionMessage")
    String createSessionMessage();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    @Key("closeButtonTitle")
    String closeButtonTitle();

    /**
     * Translated "Right click on the plot image above to copy to the clipboard.".
     *
     * @return translated "Right click on the plot image above to copy to the clipboard."
     */
    @DefaultMessage("Right click on the plot image above to copy to the clipboard.")
    @Key("rightClickPlotImageText")
    String rightClickPlotImageText();

    /**
     * Translated "Edit Snippets".
     *
     * @return translated "Edit Snippets"
     */
    @DefaultMessage("Edit Snippets")
    @Key("editSnippetsText")
    String editSnippetsText();

    /**
     * Translated "Using Code Snippets".
     *
     * @return translated "Using Code Snippets"
     */
    @DefaultMessage("Using Code Snippets")
    @Key("usingCodeSnippetsText")
    String usingCodeSnippetsText();

    /**
     * Translated "Error Applying Snippets ({0})".
     *
     * @return translated "Error Applying Snippets ({0})"
     */
    @DefaultMessage("Error Applying Snippets ({0})")
    @Key("applyingSnippetsError")
    String applyingSnippetsError(String fileTypeLabel);

    /**
     * Translated "weaving Rnw files".
     *
     * @return translated "weaving Rnw files"
     */
    @DefaultMessage("weaving Rnw files")
    @Key("weavingRnwFilesText")
    String weavingRnwFilesText();

    /**
     * Translated "LaTeX typesetting".
     *
     * @return translated "LaTeX typesetting"
     */
    @DefaultMessage("LaTeX typesetting")
    @Key("latexTypesettingText")
    String latexTypesettingText();

    /**
     * Translated "Project Option Unchanged".
     *
     * @return translated "Project Option Unchanged"
     */
    @DefaultMessage("Project Option Unchanged")
    @Key("projectOptionUnchangedCaption")
    String projectOptionUnchangedCaption();

    /**
     * Translated "You changed the global option for {0} to {1}, however the current project is still configured to use {2}.\n\nDo you want to edit the options for the current project as well?".
     *
     * @return translated "You changed the global option for {0} to {1}, however the current project is still configured to use {2}.\n\nDo you want to edit the options for the current project as well?"
     */
    @DefaultMessage("You changed the global option for {0} to {1}, however the current project is still configured to use {2}.\\n\\nDo you want to edit the options for the current project as well?")
    @Key("projectOptionUnchangedMessage")
    String projectOptionUnchangedMessage(String valueName, String globalValue, String value);

    /**
     * Translated "Save Selected".
     *
     * @return translated "Save Selected"
     */
    @DefaultMessage("Save Selected")
    @Key("saveSelectedCaption")
    String saveSelectedCaption();

    /**
     * Translated "Don't Save".
     *
     * @return translated "Don't Save"
     */
    @DefaultMessage("Don't Save")
    @Key("dontSaveButtonText")
    String dontSaveButtonText();

    /**
     * Translated "The following file has unsaved changes:".
     *
     * @return translated "The following file has unsaved changes:"
     */
    @DefaultMessage("The following file has unsaved changes:")
    @Key("fileUnsavedChangesText")
    String fileUnsavedChangesText();

    /**
     * Translated "The following {0} files have unsaved changes:".
     *
     * @return translated "The following {0} files have unsaved changes:"
     */
    @DefaultMessage("The following {0} files have unsaved changes:")
    @Key("filesUnsavedChangesText")
    String filesUnsavedChangesText(int size);

    /**
     * Translated "Build All".
     *
     * @return translated "Build All"
     */
    @DefaultMessage("Build All")
    @Key("buildAllLabel")
    String buildAllLabel();

    /**
     * Translated "Build all".
     *
     * @return translated "Build all"
     */
    @DefaultMessage("Build all")
    @Key("buildAllDesc")
    String buildAllDesc();

    /**
     * Translated "Project".
     *
     * @return translated "Project"
     */
    @DefaultMessage("Project")
    @Key("projectTypeText")
    String projectTypeText();

    /**
     * Translated "Book".
     *
     * @return translated "Book"
     */
    @DefaultMessage("Book")
    @Key("bookText")
    String bookText();

    /**
     * Translated "Website".
     *
     * @return translated "Website"
     */
    @DefaultMessage("Website")
    @Key("projectWebsiteText")
    String projectWebsiteText();

    /**
     * Translated "Render ".
     *
     * @return translated "Render "
     */
    @DefaultMessage("Render ")
    @Key("renderLabel")
    String renderLabel();

    /**
     * Translated "Serve ".
     *
     * @return translated "Serve "
     */
    @DefaultMessage("Serve ")
    @Key("serveLabel")
    String serveLabel();

    /**
     * Translated "Build".
     *
     * @return translated "Build"
     */
    @DefaultMessage("Build")
    @Key("buildText")
    String buildText();

    /**
     * Translated "Build Tab".
     *
     * @return translated "Build Tab"
     */
    @DefaultMessage("Build Tab")
    @Key("buildTabLabel")
    String buildTabLabel();

    /**
     * Translated "Build Book".
     *
     * @return translated "Build Book"
     */
    @DefaultMessage("Build Book")
    @Key("buildBookText")
    String buildBookText();

    /**
     * Translated "Build Website".
     *
     * @return translated "Build Website"
     */
    @DefaultMessage("Build Website")
    @Key("buildWebsiteText")
    String buildWebsiteText();

    /**
     * Translated "Build book options".
     *
     * @return translated "Build book options"
     */
    @DefaultMessage("Build book options")
    @Key("buildBookOptionsText")
    String buildBookOptionsText();

    /**
     * Translated "Serve Book".
     *
     * @return translated "Serve Book"
     */
    @DefaultMessage("Serve Book")
    @Key("serveBookText")
    String serveBookText();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    @DefaultMessage("More")
    @Key("moreText")
    String moreText();

    /**
     * Translated "All Formats".
     *
     * @return translated "All Formats"
     */
    @DefaultMessage("All Formats")
    @Key("allFormatsLabel")
    String allFormatsLabel();

    /**
     * Translated "{0} Format".
     *
     * @return translated "{0} Format"
     */
    @DefaultMessage("{0} Format")
    @Key("formatMenuLabel")
    String formatMenuLabel(String formatName);

    /**
     * Translated "Render Website".
     *
     * @return translated "Render Website"
     */
    @DefaultMessage("Render Website")
    @Key("renderWebsiteText")
    String renderWebsiteText();

    /**
     * Translated "Render Book".
     *
     * @return translated "Render Book"
     */
    @DefaultMessage("Render Book")
    @Key("renderBookText")
    String renderBookText();

    /**
     * Translated "Render Project".
     *
     * @return translated "Render Project"
     */
    @DefaultMessage("Render Project")
    @Key("renderProjectText")
    String renderProjectText();

    /**
     * Translated "Building package documentation".
     *
     * @return translated "Building package documentation"
     */
    @DefaultMessage("Building package documentation")
    @Key("packageDocumentationProgressCaption")
    String packageDocumentationProgressCaption();

    /**
     * Translated "Building sites".
     *
     * @return translated "Building sites"
     */
    @DefaultMessage("Building sites")
    @Key("buildingSitesUserAction")
    String buildingSitesUserAction();

    /**
     * Translated "Terminal jobs will be terminated. Are you sure?".
     *
     * @return translated "Terminal jobs will be terminated. Are you sure?"
     */
    @DefaultMessage("Terminal jobs will be terminated. Are you sure?")
    @Key("terminalTerminatedQuestion")
    String terminalTerminatedQuestion();

    /**
     * Translated "Terminating Build...".
     *
     * @return translated "Terminating Build..."
     */
    @DefaultMessage("Terminating Build...")
    @Key("terminatingBuildMessage")
    String terminatingBuildMessage();

    /**
     * Translated "Error Terminating Build".
     *
     * @return translated "Error Terminating Build"
     */
    @DefaultMessage("Error Terminating Build")
    @Key("errorTerminatingBuildCaption")
    String errorTerminatingBuildCaption();

    /**
     * Translated "Unable to terminate build. Please try again.".
     *
     * @return translated "Unable to terminate build. Please try again."
     */
    @DefaultMessage("Unable to terminate build. Please try again.")
    @Key("errorTerminatingBuildMessage")
    String errorTerminatingBuildMessage();

    /**
     * Translated "Quarto Serve Error".
     *
     * @return translated "Quarto Serve Error"
     */
    @DefaultMessage("Quarto Serve Error")
    @Key("quartoServeError")
    String quartoServeError();

    /**
     * Translated "Saving...".
     *
     * @return translated "Saving..."
     */
    @DefaultMessage("Saving...")
    @Key("savingMessage")
    String savingMessage();

    /**
     * Translated "Cancelling...".
     *
     * @return translated "Cancelling..."
     */
    @DefaultMessage("Cancelling...")
    @Key("cancellingMessage")
    String cancellingMessage();

    /**
     * Translated "Choose File".
     *
     * @return translated "Choose File"
     */
    @DefaultMessage("Choose File")
    @Key("chooseFileCaption")
    String chooseFileCaption();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "The R session is currently busy. Wait for completion or interrupt the current session and retry.".
     *
     * @return translated "The R session is currently busy. Wait for completion or interrupt the current session and retry."
     */
    @DefaultMessage("The R session is currently busy. Wait for completion or interrupt the current session and retry.")
    @Key("newConnectionError")
    String newConnectionError();

    /**
     * Translated "Checking for Updates...".
     *
     * @return translated "Checking for Updates..."
     */
    @DefaultMessage("Checking for Updates...")
    @Key("checkingForUpdatesProgress")
    String checkingForUpdatesProgress();

    /**
     * Translated "Failed to check for updates".
     *
     * @return translated "Failed to check for updates"
     */
    @DefaultMessage("Failed to check for updates")
    @Key("failedToCheckUpdatesError")
    String failedToCheckUpdatesError();

    /**
     * Translated "Preparing Connections...".
     *
     * @return translated "Preparing Connections..."
     */
    @DefaultMessage("Preparing Connections...")
    @Key("preparingConnectionsProgressMessage")
    String preparingConnectionsProgressMessage();

    /**
     * Translated "Connecting".
     *
     * @return translated "Connecting"
     */
    @DefaultMessage("Connecting")
    @Key("connectingLabel")
    String connectingLabel();

    /**
     * Translated "Previewing table...".
     *
     * @return translated "Previewing table..."
     */
    @DefaultMessage("Previewing table...")
    @Key("previewTableProgressMessage")
    String previewTableProgressMessage();

    /**
     * Translated "Remove Connection".
     *
     * @return translated "Remove Connection"
     */
    @DefaultMessage("Remove Connection")
    @Key("removeConnectionCaption")
    String removeConnectionCaption();

    /**
     * Translated "Are you sure you want to remove this connection from the connection history?".
     *
     * @return translated "Are you sure you want to remove this connection from the connection history?"
     */
    @DefaultMessage("Are you sure you want to remove this connection from the connection history?")
    @Key("removeConnectionQuestion")
    String removeConnectionQuestion();

    /**
     * Translated "Are you sure you want to disconnect?".
     *
     * @return translated "Are you sure you want to disconnect?"
     */
    @DefaultMessage("Are you sure you want to disconnect?")
    @Key("disconnectQuestion")
    String disconnectQuestion();

    /**
     * Translated "Disconnect".
     *
     * @return translated "Disconnect"
     */
    @DefaultMessage("Disconnect")
    @Key("disconnectCaption")
    String disconnectCaption();

    /**
     * Translated "Connections".
     *
     * @return translated "Connections"
     */
    @DefaultMessage("Connections")
    @Key("connectionsTitle")
    String connectionsTitle();

    /**
     * Translated "Connection:".
     *
     * @return translated "Connection:"
     */
    @DefaultMessage("Connection:")
    @Key("connectionTitle")
    String connectionTitle();

    /**
     * Translated "Connect from:".
     *
     * @return translated "Connect from:"
     */
    @DefaultMessage("Connect from:")
    @Key("connectFromText")
    String connectFromText();

    /**
     * Translated "R Console".
     *
     * @return translated "R Console"
     */
    @DefaultMessage("R Console")
    @Key("rConsoleItem")
    String rConsoleItem();

    /**
     * Translated "New R Script".
     *
     * @return translated "New R Script"
     */
    @DefaultMessage("New R Script")
    @Key("newRScriptItem")
    String newRScriptItem();

    /**
     * Translated "New R Notebook".
     *
     * @return translated "New R Notebook"
     */
    @DefaultMessage("New R Notebook")
    @Key("newRNotebookItem")
    String newRNotebookItem();

    /**
     * Translated "Copy to Clipboard".
     *
     * @return translated "Copy to Clipboard"
     */
    @DefaultMessage("Copy to Clipboard")
    @Key("copyToClipboardItem")
    String copyToClipboardItem();

    /**
     * Translated "(Not connected)".
     *
     * @return translated "(Not connected)"
     */
    @DefaultMessage("(Not connected)")
    @Key("notConnectedLabel")
    String notConnectedLabel();

    /**
     * Translated "Connection".
     *
     * @return translated "Connection"
     */
    @DefaultMessage("Connection")
    @Key("connectionTextHeader")
    String connectionTextHeader();

    /**
     * Translated "Connected".
     *
     * @return translated "Connected"
     */
    @DefaultMessage("Connected")
    @Key("connectedText")
    String connectedText();

    /**
     * Translated "Status".
     *
     * @return translated "Status"
     */
    @DefaultMessage("Status")
    @Key("statusText")
    String statusText();

    /**
     * Translated "Explore connection".
     *
     * @return translated "Explore connection"
     */
    @DefaultMessage("Explore connection")
    @Key("exploreConnectionTitle")
    String exploreConnectionTitle();

    /**
     * Translated "Connections Tab".
     *
     * @return translated "Connections Tab"
     */
    @DefaultMessage("Connections Tab")
    @Key("connectionsTabLabel")
    String connectionsTabLabel();

    /**
     * Translated "Filter by connection".
     *
     * @return translated "Filter by connection"
     */
    @DefaultMessage("Filter by connection")
    @Key("filterByConnectionLabel")
    String filterByConnectionLabel();

    /**
     * Translated "Filter by object".
     *
     * @return translated "Filter by object"
     */
    @DefaultMessage("Filter by object")
    @Key("filterByObjectLabel")
    String filterByObjectLabel();

    /**
     * Translated "View all connections".
     *
     * @return translated "View all connections"
     */
    @DefaultMessage("View all connections")
    @Key("viewAllConnectionsTitle")
    String viewAllConnectionsTitle();

    /**
     * Translated "R Console".
     *
     * @return translated "R Console"
     */
    @DefaultMessage("R Console")
    @Key("rConsoleText")
    String rConsoleText();

    /**
     * Translated "New R Script".
     *
     * @return translated "New R Script"
     */
    @DefaultMessage("New R Script")
    @Key("newRScriptText")
    String newRScriptText();

    /**
     * Translated "New R Notebook".
     *
     * @return translated "New R Notebook"
     */
    @DefaultMessage("New R Notebook")
    @Key("newRNotebookText")
    String newRNotebookText();

    /**
     * Translated "Copy to Clipboard".
     *
     * @return translated "Copy to Clipboard"
     */
    @DefaultMessage("Copy to Clipboard")
    @Key("copyToClipboardText")
    String copyToClipboardText();

    /**
     * Translated "Connect".
     *
     * @return translated "Connect"
     */
    @DefaultMessage("Connect")
    @Key("connectLabel")
    String connectLabel();

    /**
     * Translated "Connections Tab Connection".
     *
     * @return translated "Connections Tab Connection"
     */
    @DefaultMessage("Connections Tab Connection")
    @Key("connectionsTab")
    String connectionsTab();

    /**
     * Translated "Installation failed".
     *
     * @return translated "Installation failed"
     */
    @DefaultMessage("Installation failed")
    @Key("installationFailedCaption")
    String installationFailedCaption();

    /**
     * Translated "The {0} driver is being installed...".
     *
     * @return translated "The {0} driver is being installed..."
     */
    @DefaultMessage("The {0} driver is being installed...")
    @Key("installOdbcDriverText")
    String installOdbcDriverText(String name);

    /**
     * Translated "Installation for the {0} driver failed with status {1}.".
     *
     * @return translated "Installation for the {0} driver failed with status {1}."
     */
    @DefaultMessage("Installation for the {0} driver failed with status {1}.")
    @Key("installationFailedMessage")
    String installationFailedMessage(String name, int exitCode);

    /**
     * Translated "The {0} driver is now installed!".
     *
     * @return translated "The {0} driver is now installed!"
     */
    @DefaultMessage("The {0} driver is now installed!")
    @Key("driverInstalledText")
    String driverInstalledText(String name);

    /**
     * Translated "{0} Installation".
     *
     * @return translated "{0} Installation"
     */
    @DefaultMessage("{0} Installation")
    @Key("installationPageCaption")
    String installationPageCaption(String name);

    /**
     * Translated "{0} Connection".
     *
     * @return translated "{0} Connection"
     */
    @DefaultMessage("{0} Connection")
    @Key("connectionNameLabel")
    String connectionNameLabel(String name);


}

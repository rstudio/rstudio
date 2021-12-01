package org.rstudio.studio.client.workbench.views;
/*
 * ViewConstants.java
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
public interface ViewConstants extends com.google.gwt.i18n.client.Messages {
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
     * Translated "Installation failed".
     *
     * @return translated "Installation failed"
     */
    @DefaultMessage("Installation failed")
    @Key("installationFailedCaption")
    String installationFailedCaption();

    /**
     * Translated "{0} Installation".
     *
     * @return translated "{0} Installation"
     */
    @DefaultMessage("{0} Installation")
    @Key("installationPageCaption")
    String installationPageCaption(String name);

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
     * Translated "(Not connected)".
     *
     * @return translated "(Not connected)"
     */
    @DefaultMessage("(Not connected)")
    @Key("notConnectedLabel")
    String notConnectedLabel();

    /**
     * Translated "Connections".
     *
     * @return translated "Connections"
     */
    @DefaultMessage("Connections")
    @Key("connectionsTitle")
    String connectionsTitle();

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
     * Translated "{0} Connection".
     *
     * @return translated "{0} Connection"
     */
    @DefaultMessage("{0} Connection")
    @Key("connectionNameLabel")
    String connectionNameLabel(String name);

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
}

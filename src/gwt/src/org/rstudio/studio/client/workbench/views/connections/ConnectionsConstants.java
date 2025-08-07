/*
 * ConnectionsConstants.java
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
package org.rstudio.studio.client.workbench.views.connections;

public interface ConnectionsConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Connections".
     *
     * @return translated "Connections"
     */
    @DefaultMessage("Connections")
    String connectionsTitle();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    String errorCaption();

    /**
     * Translated "The R session is currently busy. Wait for completion or interrupt the current session and retry.".
     *
     * @return translated "The R session is currently busy. Wait for completion or interrupt the current session and retry."
     */
    @DefaultMessage("The R session is currently busy. Wait for completion or interrupt the current session and retry.")
    String newConnectionError();

    /**
     * Translated "Checking for Updates...".
     *
     * @return translated "Checking for Updates..."
     */
    @DefaultMessage("Checking for Updates...")
    String checkingForUpdatesProgress();

    /**
     * Translated "Failed to check for updates".
     *
     * @return translated "Failed to check for updates"
     */
    @DefaultMessage("Failed to check for updates")
    String failedToCheckUpdatesError();

    /**
     * Translated "Preparing connections...".
     *
     * @return translated "Preparing connections..."
     */
    @DefaultMessage("Preparing connections...")
    String preparingConnectionsProgressMessage();

    /**
     * Translated "Connecting".
     *
     * @return translated "Connecting"
     */
    @DefaultMessage("Connecting")
    String connectingLabel();

    /**
     * Translated "Previewing table...".
     *
     * @return translated "Previewing table..."
     */
    @DefaultMessage("Previewing table...")
    String previewTableProgressMessage();

    /**
     * Translated "Remove Connection".
     *
     * @return translated "Remove Connection"
     */
    @DefaultMessage("Remove Connection")
    String removeConnectionCaption();

    /**
     * Translated "Are you sure you want to remove this connection from the connection history?".
     *
     * @return translated "Are you sure you want to remove this connection from the connection history?"
     */
    @DefaultMessage("Are you sure you want to remove this connection from the connection history?")
    String removeConnectionQuestion();

    /**
     * Translated "Are you sure you want to disconnect?".
     *
     * @return translated "Are you sure you want to disconnect?"
     */
    @DefaultMessage("Are you sure you want to disconnect?")
    String disconnectQuestion();

    /**
     * Translated "Disconnect".
     *
     * @return translated "Disconnect"
     */
    @DefaultMessage("Disconnect")
    String disconnectCaption();

    /**
     * Translated "{0} Connection".
     *
     * @return translated "{0} Connection"
     */
    @DefaultMessage("{0} Connection")
    String connectionNameLabel(String name);

    /**
     * Translated "Connection".
     *
     * @return translated "Connection"
     */
    @DefaultMessage("Connection")
    String connectionTextHeader();

    /**
     * Translated "Connected".
     *
     * @return translated "Connected"
     */
    @DefaultMessage("Connected")
    String connectedText();

    /**
     * Translated "Status".
     *
     * @return translated "Status"
     */
    @DefaultMessage("Status")
    String statusText();

    /**
     * Translated "Explore connection".
     *
     * @return translated "Explore connection"
     */
    @DefaultMessage("Explore connection")
    String exploreConnectionTitle();

    /**
     * Translated "Connections Tab".
     *
     * @return translated "Connections Tab"
     */
    @DefaultMessage("Connections Tab")
    String connectionsTabLabel();

    /**
     * Translated "Filter by connection".
     *
     * @return translated "Filter by connection"
     */
    @DefaultMessage("Filter by connection")
    String filterByConnectionLabel();

    /**
     * Translated "Filter by object".
     *
     * @return translated "Filter by object"
     */
    @DefaultMessage("Filter by object")
    String filterByObjectLabel();

    /**
     * Translated "View all connections".
     *
     * @return translated "View all connections"
     */
    @DefaultMessage("View all connections")
    String viewAllConnectionsTitle();

    /**
     * Translated "R Console".
     *
     * @return translated "R Console"
     */
    @DefaultMessage("R Console")
    String rConsoleText();

    /**
     * Translated "New R Script".
     *
     * @return translated "New R Script"
     */
    @DefaultMessage("New R Script")
    String newRScriptText();

    /**
     * Translated "New R Notebook".
     *
     * @return translated "New R Notebook"
     */
    @DefaultMessage("New R Notebook")
    String newRNotebookText();

    /**
     * Translated "Copy to Clipboard".
     *
     * @return translated "Copy to Clipboard"
     */
    @DefaultMessage("Copy to Clipboard")
    String copyToClipboardText();

    /**
     * Translated "Connect".
     *
     * @return translated "Connect"
     */
    @DefaultMessage("Connect")
    String connectLabel();

    /**
     * Translated "Connections Tab Connection".
     *
     * @return translated "Connections Tab Connection"
     */
    @DefaultMessage("Connections Tab Connection")
    String connectionsTab();

    /**
     * Translated "R Console".
     *
     * @return translated "R Console"
     */
    @DefaultMessage("R Console")
    String rConsoleItem();

    /**
     * Translated "New R Script".
     *
     * @return translated "New R Script"
     */
    @DefaultMessage("New R Script")
    String newRScriptItem();

    /**
     * Translated "New R Notebook".
     *
     * @return translated "New R Notebook"
     */
    @DefaultMessage("New R Notebook")
    String newRNotebookItem();

    /**
     * Translated "Copy to Clipboard".
     *
     * @return translated "Copy to Clipboard"
     */
    @DefaultMessage("Copy to Clipboard")
    String copyToClipboardItem();

    /**
     * Translated "Connection:".
     *
     * @return translated "Connection:"
     */
    @DefaultMessage("Connection:")
    String connectionTitle();

    /**
     * Translated "Connect from:".
     *
     * @return translated "Connect from:"
     */
    @DefaultMessage("Connect from:")
    String connectFromText();

    /**
     * Translated "(Not connected)".
     *
     * @return translated "(Not connected)"
     */
    @DefaultMessage("(Not connected)")
    String notConnectedLabel();

    /**
     * Translated "The {0} driver is being installed...".
     *
     * @return translated "The {0} driver is being installed..."
     */
    @DefaultMessage("The {0} driver is being installed...")
    String installOdbcDriverText(String name);

    /**
     * Translated "Installation for the {0} driver failed with status {1}.".
     *
     * @return translated "Installation for the {0} driver failed with status {1}."
     */
    @DefaultMessage("Installation for the {0} driver failed with status {1}.")
    String installationFailedMessage(String name, int exitCode);

    /**
     * Translated "The {0} driver is now installed!".
     *
     * @return translated "The {0} driver is now installed!"
     */
    @DefaultMessage("The {0} driver is now installed!")
    String driverInstalledText(String name);

    /**
     * Translated "Installation failed".
     *
     * @return translated "Installation failed"
     */
    @DefaultMessage("Installation failed")
    String installationFailedCaption();

    /**
     * Translated "{0} Installation".
     *
     * @return translated "{0} Installation"
     */
    @DefaultMessage("{0} Installation")
    String installationPageCaption(String name);

    /**
     * Translated "Connect to Existing Data Sources".
     *
     * @return translated "Connect to Existing Data Sources"
     */
    @DefaultMessage("Connect to Existing Data Sources")
    String connectExistingDataSourceCaption();

    /**
     * Translated "{0} via {1}".
     *
     * @return translated "{0} via {1}"
     */
    @DefaultMessage("{0} via {1}")
    String connectionInfoSubTitle(String name, String source);

    /**
     * Translated "title".
     *
     * @return translated "title"
     */
    @DefaultMessage("title")
    String titleText();

    /**
     * Translated "The {0} driver is currently not installed. ".
     *
     * @return translated "The {0} driver is currently not installed. "
     */
    @DefaultMessage("The {0} driver is currently not installed. ")
    String driverLabelText(String name);

    /**
     * Translated "{0} Installation".
     *
     * @return translated "{0} Installation"
     */
    @DefaultMessage("{0} Installation")
    String installOdbcCaption(String name);

    /**
     * Translated "Connecting to ".
     *
     * @return translated "Connecting to "
     */
    @DefaultMessage("Connecting to ")
    String connectionUserActionLabel();

    /**
     * Translated "Shiny Mini UI".
     *
     * @return translated "Shiny Mini UI"
     */
    @DefaultMessage("Shiny Mini UI")
    String shinyMiniUITitle();


    /**
     * Translated "{0} Connection".
     *
     * @return translated "{0} Connection"
     */
    @DefaultMessage("{0} Connection")
    String newConnectionPage(String name);

    /**
     * Translated "Using {0}".
     *
     * @return translated "Using {0}"
     */
    @DefaultMessage("Using {0}")
    String connectionHelpLink(String name);

    /**
     * Translated "Advanced Options".
     *
     * @return translated "Advanced Options"
     */
    @DefaultMessage("Advanced Options")
    String advancedOptionsCaption();

    /**
     * Translated "Configure".
     *
     * @return translated "Configure"
     */
    @DefaultMessage("Configure")
    String configureButtonLabel();

    /**
     * Translated "Using {0}".
     *
     * @return translated "Using {0}"
     */
    @DefaultMessage("Using {0}")
    String newConnectionInfoCaption(String name);

    /**
     * Translated "<b>Success!</b> The given parameters can be used to connect and disconnect correctly.".
     *
     * @return translated "<b>Success!</b> The given parameters can be used to connect and disconnect correctly."
     */
    @DefaultMessage("<b>Success!</b> The given parameters can be used to connect and disconnect correctly.")
    String newConnectionSuccessHTML();

    /**
     * Translated "Test Results".
     *
     * @return translated "Test Results"
     */
    @DefaultMessage("Test Results")
    String testResultsCaption();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    String okLabel();

    /**
     * Translated "<b>Failure.</b> ".
     *
     * @return translated "<b>Failure.</b> "
     */
    @DefaultMessage("<b>Failure.</b> ")
    String failureHTML();

    /**
     * Translated "Test".
     *
     * @return translated "Test"
     */
    @DefaultMessage("Test")
    String testButtonLabel();

    /**
     * Translated "Testing Connection...".
     *
     * @return translated "Testing Connection..."
     */
    @DefaultMessage("Testing Connection...")
    String testingConnectionProgressMessage();

    /**
     * Translated "Options...".
     *
     * @return translated "Options..."
     */
    @DefaultMessage("Options...")
    String optionsButtonLabel();

    /**
     * Translated "Uninstall...".
     *
     * @return translated "Uninstall..."
     */
    @DefaultMessage("Uninstall...")
    String uninstallButton();

    /**
     * Translated "Uninstall {0} Driver".
     *
     * @return translated "Uninstall {0} Driver"
     */
    @DefaultMessage("Uninstall {0} Driver")
    String uninstallDriverCaption(String name);

    /**
     * Translated "Uninstall the {0} driver by removing files and registration entries?".
     *
     * @return translated "Uninstall the {0} driver by removing files and registration entries?"
     */
    @DefaultMessage("Uninstall the {0} driver by removing files and registration entries?")
    String uninstallDriverMessage(String name);

    /**
     * Translated "Uninstallation failed".
     *
     * @return translated "Uninstallation failed"
     */
    @DefaultMessage("Uninstallation failed")
    String uninstallationFailedCaption();

    /**
     * Translated "Uninstallation complete".
     *
     * @return translated "Uninstallation complete"
     */
    @DefaultMessage("Uninstallation complete")
    String uninstallationCompleteCaption();

    /**
     * Translated "Driver {0} was successfully uninstalled.".
     *
     * @return translated "Driver {0} was successfully uninstalled."
     */
    @DefaultMessage("Driver {0} was successfully uninstalled.")
    String driverUninstalledSuccess(String name);

    /**
     * Translated "Uninstallation failed".
     *
     * @return translated "Uninstallation failed"
     */
    @DefaultMessage("Uninstallation failed")
    String uninstalledFailedMessage();

    /**
     * Translated "New Connection".
     *
     * @return translated "New Connection"
     */
    @DefaultMessage("New Connection")
    String newConnectionCaption();

    /**
     * Translated "Using RStudio Connections".
     *
     * @return translated "Using RStudio Connections"
     */
    @DefaultMessage("Using RStudio Connections")
    String rstudioConnectionsCaption();

    /**
     * Translated "Loading objects".
     *
     * @return translated "Loading objects"
     */
    @DefaultMessage("Loading objects")
    String loadingObjectsMessage();

    /**
     * Translated ""View table (up to 1,000 records)"".
     *
     * @return translated ""View table (up to 1,000 records)""
     */
    @DefaultMessage("\"View table (up to 1,000 records)\"")
    String viewTableHTML();
    
    @DefaultMessage("Installation path:")
    String installationPath();
    

}

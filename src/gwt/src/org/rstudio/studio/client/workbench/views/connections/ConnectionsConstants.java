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

    @DefaultMessage("Connections")
    @Key("connectionsTitle")
    String connectionsTitle();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("The R session is currently busy. Wait for completion or interrupt the current session and retry.")
    @Key("newConnectionError")
    String newConnectionError();

    @DefaultMessage("Checking for Updates...")
    @Key("checkingForUpdatesProgress")
    String checkingForUpdatesProgress();

    @DefaultMessage("Failed to check for updates")
    @Key("failedToCheckUpdatesError")
    String failedToCheckUpdatesError();

    @DefaultMessage("Preparing connections...")
    @Key("preparingConnectionsProgressMessage")
    String preparingConnectionsProgressMessage();

    @DefaultMessage("Connecting")
    @Key("connectingLabel")
    String connectingLabel();

    @DefaultMessage("Previewing table...")
    @Key("previewTableProgressMessage")
    String previewTableProgressMessage();

    @DefaultMessage("Remove Connection")
    @Key("removeConnectionCaption")
    String removeConnectionCaption();

    @DefaultMessage("Are you sure you want to remove this connection from the connection history?")
    @Key("removeConnectionQuestion")
    String removeConnectionQuestion();

    @DefaultMessage("Are you sure you want to disconnect?")
    @Key("disconnectQuestion")
    String disconnectQuestion();

    @DefaultMessage("Disconnect")
    @Key("disconnectCaption")
    String disconnectCaption();

    @DefaultMessage("{0} Connection")
    @Key("connectionNameLabel")
    String connectionNameLabel(String name);

    @DefaultMessage("Connection")
    @Key("connectionTextHeader")
    String connectionTextHeader();

    @DefaultMessage("Connected")
    @Key("connectedText")
    String connectedText();

    @DefaultMessage("Status")
    @Key("statusText")
    String statusText();

    @DefaultMessage("Explore connection")
    @Key("exploreConnectionTitle")
    String exploreConnectionTitle();

    @DefaultMessage("Connections Tab")
    @Key("connectionsTabLabel")
    String connectionsTabLabel();

    @DefaultMessage("Filter by connection")
    @Key("filterByConnectionLabel")
    String filterByConnectionLabel();

    @DefaultMessage("Filter by object")
    @Key("filterByObjectLabel")
    String filterByObjectLabel();

    @DefaultMessage("View all connections")
    @Key("viewAllConnectionsTitle")
    String viewAllConnectionsTitle();

    @DefaultMessage("R Console")
    @Key("rConsoleText")
    String rConsoleText();

    @DefaultMessage("New R Script")
    @Key("newRScriptText")
    String newRScriptText();

    @DefaultMessage("New R Notebook")
    @Key("newRNotebookText")
    String newRNotebookText();

    @DefaultMessage("Copy to Clipboard")
    @Key("copyToClipboardText")
    String copyToClipboardText();

    @DefaultMessage("Connect")
    @Key("connectLabel")
    String connectLabel();

    @DefaultMessage("Connections Tab Connection")
    @Key("connectionsTab")
    String connectionsTab();

    @DefaultMessage("R Console")
    @Key("rConsoleItem")
    String rConsoleItem();

    @DefaultMessage("New R Script")
    @Key("newRScriptItem")
    String newRScriptItem();

    @DefaultMessage("New R Notebook")
    @Key("newRNotebookItem")
    String newRNotebookItem();

    @DefaultMessage("Copy to Clipboard")
    @Key("copyToClipboardItem")
    String copyToClipboardItem();

    @DefaultMessage("Connection:")
    @Key("connectionTitle")
    String connectionTitle();

    @DefaultMessage("Connect from:")
    @Key("connectFromText")
    String connectFromText();

    @DefaultMessage("(Not connected)")
    @Key("notConnectedLabel")
    String notConnectedLabel();

    @DefaultMessage("The {0} driver is being installed...")
    @Key("installOdbcDriverText")
    String installOdbcDriverText(String name);

    @DefaultMessage("Installation for the {0} driver failed with status {1}.")
    @Key("installationFailedMessage")
    String installationFailedMessage(String name, int exitCode);

    @DefaultMessage("The {0} driver is now installed!")
    @Key("driverInstalledText")
    String driverInstalledText(String name);

    @DefaultMessage("Installation failed")
    @Key("installationFailedCaption")
    String installationFailedCaption();

    @DefaultMessage("{0} Installation")
    @Key("installationPageCaption")
    String installationPageCaption(String name);

    @DefaultMessage("Connect to Existing Data Sources")
    @Key("connectExistingDataSourceCaption")
    String connectExistingDataSourceCaption();

    @DefaultMessage("{0} via {1}")
    @Key("connectionInfoSubTitle")
    String connectionInfoSubTitle(String name, String source);

    @DefaultMessage("title")
    @Key("titleText")
    String titleText();

    @DefaultMessage("The {0} driver is currently not installed. ")
    @Key("driverLabelText")
    String driverLabelText(String name);

    @DefaultMessage("{0} Installation")
    @Key("installOdbcCaption")
    String installOdbcCaption(String name);

    @DefaultMessage("Connecting to ")
    @Key("connectionUserActionLabel")
    String connectionUserActionLabel();

    @DefaultMessage("Shiny Mini UI")
    @Key("shinyMiniUITitle")
    String shinyMiniUITitle();

    @DefaultMessage("{0} Connection")
    @Key("newConnectionPage")
    String newConnectionPage(String name);

    @DefaultMessage("Using {0}")
    @Key("connectionHelpLink")
    String connectionHelpLink(String name);

    @DefaultMessage("Advanced Options")
    @Key("advancedOptionsCaption")
    String advancedOptionsCaption();

    @DefaultMessage("Configure")
    @Key("configureButtonLabel")
    String configureButtonLabel();

    @DefaultMessage("Using {0}")
    @Key("newConnectionInfoCaption")
    String newConnectionInfoCaption(String name);

    @DefaultMessage("<b>Success!</b> The given parameters can be used to connect and disconnect correctly.")
    @Key("newConnectionSuccessHTML")
    String newConnectionSuccessHTML();

    @DefaultMessage("Test Results")
    @Key("testResultsCaption")
    String testResultsCaption();

    @DefaultMessage("OK")
    @Key("okLabel")
    String okLabel();

    @DefaultMessage("<b>Failure.</b> ")
    @Key("failureHTML")
    String failureHTML();

    @DefaultMessage("Test")
    @Key("testButtonLabel")
    String testButtonLabel();

    @DefaultMessage("Testing Connection...")
    @Key("testingConnectionProgressMessage")
    String testingConnectionProgressMessage();

    @DefaultMessage("Options...")
    @Key("optionsButtonLabel")
    String optionsButtonLabel();

    @DefaultMessage("Uninstall...")
    @Key("uninstallButton")
    String uninstallButton();

    @DefaultMessage("Uninstall {0} Driver")
    @Key("uninstallDriverCaption")
    String uninstallDriverCaption(String name);

    @DefaultMessage("Uninstall the {0} driver by removing files and registration entries?")
    @Key("uninstallDriverMessage")
    String uninstallDriverMessage(String name);

    @DefaultMessage("Uninstallation failed")
    @Key("uninstallationFailedCaption")
    String uninstallationFailedCaption();

    @DefaultMessage("Uninstallation complete")
    @Key("uninstallationCompleteCaption")
    String uninstallationCompleteCaption();

    @DefaultMessage("Driver {0} was successfully uninstalled.")
    @Key("driverUninstalledSuccess")
    String driverUninstalledSuccess(String name);

    @DefaultMessage("Uninstallation failed")
    @Key("uninstalledFailedMessage")
    String uninstalledFailedMessage();

    @DefaultMessage("New Connection")
    @Key("newConnectionCaption")
    String newConnectionCaption();

    @DefaultMessage("Using RStudio Connections")
    @Key("rstudioConnectionsCaption")
    String rstudioConnectionsCaption();

    @DefaultMessage("Loading objects")
    @Key("loadingObjectsMessage")
    String loadingObjectsMessage();

    @DefaultMessage("\"View table (up to 1,000 records)\"")
    @Key("viewTableHTML")
    String viewTableHTML();
    
    @DefaultMessage("Installation path:")
    @Key("installationPath")
    String installationPath();
    

}

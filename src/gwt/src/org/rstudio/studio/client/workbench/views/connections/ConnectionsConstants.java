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

    @Key("connectionsTitle")
    String connectionsTitle();

    @Key("errorCaption")
    String errorCaption();

    @Key("newConnectionError")
    String newConnectionError();

    @Key("checkingForUpdatesProgress")
    String checkingForUpdatesProgress();

    @Key("failedToCheckUpdatesError")
    String failedToCheckUpdatesError();

    @Key("preparingConnectionsProgressMessage")
    String preparingConnectionsProgressMessage();

    @Key("connectingLabel")
    String connectingLabel();

    @Key("previewTableProgressMessage")
    String previewTableProgressMessage();

    @Key("removeConnectionCaption")
    String removeConnectionCaption();

    @Key("removeConnectionQuestion")
    String removeConnectionQuestion();

    @Key("disconnectQuestion")
    String disconnectQuestion();

    @Key("disconnectCaption")
    String disconnectCaption();

    @Key("connectionNameLabel")
    String connectionNameLabel(String name);

    @Key("connectionTextHeader")
    String connectionTextHeader();

    @Key("connectedText")
    String connectedText();

    @Key("statusText")
    String statusText();

    @Key("exploreConnectionTitle")
    String exploreConnectionTitle();

    @Key("connectionsTabLabel")
    String connectionsTabLabel();

    @Key("filterByConnectionLabel")
    String filterByConnectionLabel();

    @Key("filterByObjectLabel")
    String filterByObjectLabel();

    @Key("viewAllConnectionsTitle")
    String viewAllConnectionsTitle();

    @Key("rConsoleText")
    String rConsoleText();

    @Key("newRScriptText")
    String newRScriptText();

    @Key("newRNotebookText")
    String newRNotebookText();

    @Key("copyToClipboardText")
    String copyToClipboardText();

    @Key("connectLabel")
    String connectLabel();

    @Key("connectionsTab")
    String connectionsTab();

    @Key("rConsoleItem")
    String rConsoleItem();

    @Key("newRScriptItem")
    String newRScriptItem();

    @Key("newRNotebookItem")
    String newRNotebookItem();

    @Key("copyToClipboardItem")
    String copyToClipboardItem();

    @Key("connectionTitle")
    String connectionTitle();

    @Key("connectFromText")
    String connectFromText();

    @Key("notConnectedLabel")
    String notConnectedLabel();

    @Key("installOdbcDriverText")
    String installOdbcDriverText(String name);

    @Key("installationFailedMessage")
    String installationFailedMessage(String name, int exitCode);

    @Key("driverInstalledText")
    String driverInstalledText(String name);

    @Key("installationFailedCaption")
    String installationFailedCaption();

    @Key("installationPageCaption")
    String installationPageCaption(String name);

    @Key("connectExistingDataSourceCaption")
    String connectExistingDataSourceCaption();

    @Key("connectionInfoSubTitle")
    String connectionInfoSubTitle(String name, String source);

    @Key("titleText")
    String titleText();

    @Key("driverLabelText")
    String driverLabelText(String name);

    @Key("installOdbcCaption")
    String installOdbcCaption(String name);

    @Key("connectionUserActionLabel")
    String connectionUserActionLabel();

    @Key("shinyMiniUITitle")
    String shinyMiniUITitle();

    @Key("newConnectionPage")
    String newConnectionPage(String name);

    @Key("connectionHelpLink")
    String connectionHelpLink(String name);

    @Key("advancedOptionsCaption")
    String advancedOptionsCaption();

    @Key("configureButtonLabel")
    String configureButtonLabel();

    @Key("newConnectionInfoCaption")
    String newConnectionInfoCaption(String name);

    @Key("newConnectionSuccessHTML")
    String newConnectionSuccessHTML();

    @Key("testResultsCaption")
    String testResultsCaption();

    @Key("okLabel")
    String okLabel();

    @Key("failureHTML")
    String failureHTML();

    @Key("testButtonLabel")
    String testButtonLabel();

    @Key("testingConnectionProgressMessage")
    String testingConnectionProgressMessage();

    @Key("optionsButtonLabel")
    String optionsButtonLabel();

    @Key("uninstallButton")
    String uninstallButton();

    @Key("uninstallDriverCaption")
    String uninstallDriverCaption(String name);

    @Key("uninstallDriverMessage")
    String uninstallDriverMessage(String name);

    @Key("uninstallationFailedCaption")
    String uninstallationFailedCaption();

    @Key("uninstallationCompleteCaption")
    String uninstallationCompleteCaption();

    @Key("driverUninstalledSuccess")
    String driverUninstalledSuccess(String name);

    @Key("uninstalledFailedMessage")
    String uninstalledFailedMessage();

    @Key("newConnectionCaption")
    String newConnectionCaption();

    @Key("rstudioConnectionsCaption")
    String rstudioConnectionsCaption();

    @Key("loadingObjectsMessage")
    String loadingObjectsMessage();

    @Key("viewTableHTML")
    String viewTableHTML();
    
    @Key("installationPath")
    String installationPath();
    

}

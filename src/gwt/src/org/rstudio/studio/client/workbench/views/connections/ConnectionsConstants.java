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
    String connectionsTitle();
    String errorCaption();
    String newConnectionError();
    String checkingForUpdatesProgress();
    String failedToCheckUpdatesError();
    String preparingConnectionsProgressMessage();
    String connectingLabel();
    String previewTableProgressMessage();
    String removeConnectionCaption();
    String removeConnectionQuestion();
    String disconnectQuestion();
    String disconnectCaption();
    String connectionNameLabel(String name);
    String connectionTextHeader();
    String connectedText();
    String statusText();
    String exploreConnectionTitle();
    String connectionsTabLabel();
    String filterByConnectionLabel();
    String filterByObjectLabel();
    String viewAllConnectionsTitle();
    String rConsoleText();
    String newRScriptText();
    String newRNotebookText();
    String copyToClipboardText();
    String connectLabel();
    String connectionsTab();
    String rConsoleItem();
    String newRScriptItem();
    String newRNotebookItem();
    String copyToClipboardItem();
    String connectionTitle();
    String connectFromText();
    String notConnectedLabel();
    String installOdbcDriverText(String name);
    String installationFailedMessage(String name, int exitCode);
    String driverInstalledText(String name);
    String installationFailedCaption();
    String installationPageCaption(String name);
    String connectExistingDataSourceCaption();
    String connectionInfoSubTitle(String name, String source);
    String titleText();
    String driverLabelText(String name);
    String installOdbcCaption(String name);
    String connectionUserActionLabel();
    String shinyMiniUITitle();
    String newConnectionPage(String name);
    String connectionHelpLink(String name);
    String advancedOptionsCaption();
    String configureButtonLabel();
    String newConnectionInfoCaption(String name);
    String newConnectionSuccessHTML();
    String testResultsCaption();
    String okLabel();
    String failureHTML();
    String testButtonLabel();
    String testingConnectionProgressMessage();
    String optionsButtonLabel();
    String uninstallButton();
    String uninstallDriverCaption(String name);
    String uninstallDriverMessage(String name);
    String uninstallationFailedCaption();
    String uninstallationCompleteCaption();
    String driverUninstalledSuccess(String name);
    String uninstalledFailedMessage();
    String newConnectionCaption();
    String rstudioConnectionsCaption();
    String loadingObjectsMessage();
    String viewTableHTML();
    String installationPath();
}

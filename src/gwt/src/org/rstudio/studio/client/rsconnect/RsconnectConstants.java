/*
 * RsconnectConstants.java
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
package org.rstudio.studio.client.rsconnect;

public interface RsconnectConstants extends com.google.gwt.i18n.client.Messages{
    String couldNotPublish();
    String preparingForPublish();
    String error();
    String noRunningDeploymentsFound();
    String noApplicationDeploymentsFrom(String dir);
    String errorListingApplications();
    String noApplicationDeploymentsFound(String dir);
    String noDeploymentsFound();
    String couldNotDetermineAppDeployments(String dir, String message);
    String errorConfiguringApplication();
    String errorDeployingApplication();
    String couldNotDeployApplication(String recordName, String message);
    String onlyOneDeploymentAtATime();
    String deploymentInProgress();
    String uploadingContent(String contentType);
    String uploadToRPubsErrorMessage();
    String publishDocument();
    String showUnsupportedRPubsFormatMessageError();
    String unsupportedDocumentFormat();
    String rstudioDeployingApp(String appName);
    String deploymentStarted();
    String content();
    String quartoWebsite();
    String api();
    String website();
    String presentation();
    String document();
    String html();
    String plot();
    String application();
    String onRSConnectDeploymentFailedHtml(String p1, String p2, String serverUrl, String p3, String p4, String errorCode);
    String onRSConnectDeploymentFailedHtmlP1();
    String onRSConnectDeploymentFailedHtmlP2();
    String onRSConnectDeploymentFailedHtmlP3();
    String onRSConnectDeploymentFailedHtmlP4();
    String okCapitalized();
    String publishFailed();
    String cancel();
    String stopDeployment();
    String deploymentNotCancelledMessage();
    String couldNotCancelDeployment();
    String errorStoppingDeployment();
    String onRSConnectDeploymentCancelledMessage();
    String stopDeploymentQuestion();
    String publishAnyway();
    String reviewIssues();
    String publishContentIssuesMessage();
    String publishContentIssuesFound();
    String lintFailedMessage(String errorMessage);
    String lintFailed();
    String apiNotPublishableMessage();
    String apiNotPublishable();
    String contentNotPublishableMessage();
    String contentNotPublishable();
    String publishRpubTitle(String contentTypeDesc);
    String republishDocumentMessage();
    String republishDocument();
    String currentPlot();
    String rsConnectServerInfoString(String name, String url, String version, String about);
    String titleMinimumCharacter();
    String titleContainAlphanumeric();
    String errorConnectingAccountMessage(String infoString, String errorMessage);
    String errorConnectingAccount();
    String settingUpAccount();
    String accountValidationFailedMessage(String serverInfo, String errorMessage);
    String accountValidationFailed();
    String accountNotConnectedMessage();
    String accountNotConnected();
    String serverCouldntBeValidated(String errorMessage);
    String serverValidationFailedMessage(String serverUrl, String infoMessage);
    String serverValidationFailed();
    String checkingServerConnection();
    String verifyingAccount();
    String newRSConnectCloudPageCaption();
    String newRSConnectCloudPageSubTitle();
    String rstudioConnectAccount();
    String rPubsSubtitle();
    String publishTo();
    String publish();
    String publishMultiplePageSubtitle(String directoryString);
    String publishMultiplePageTitle();
    String publishMultiplePagSingleSubtitle(String name);
    String publishMultiplePagSingleTitle();
    String publishMultiplePageCaption();
    String publishReportSourcePageStaticSubtitle();
    String publishReportSourcePageStaticTitle(String descriptor);
    String publishReportSourcePageSubTitle(String scheduledReportNumber, String descriptor);
    String publishReportNoScheduledSourcePageSubtitle(String descriptor);
    String scheduledReportsPlural();
    String scheduledReportsSingular();
    String publishFilesPageTitle(String descriptor);
    String websiteLowercase();
    String documentsLowercasePlural();
    String documentLowercase();
    String publishToRstudioConnect();
    String publishToRpubs();
    String accountConnectFailedMessage(String serverInfo, String errorMessage);
    String accountConnectFailed();
    String addingAccount();
    String errorAccountMessage(String cmdString);
    String connectingAccount();
    String errorAccountMessageSetInfo();
    String rStudioCouldNotRetrieveServerInfo();
    String cantFindServers();
    String rStudioCouldNotRetrieveForAccount();
    String serverInformationNotFound();
    String errorRetrievingAccounts();
    String noAccountsConnected();
    String rStudioConnectServiceDescription();
    String rStudioConnect();
    String connectAccount();
    String chooseAccountType();
    String connectPublishingAccount();
    String pickAnAccount();
    String confirmAccountOn(String serverName);
    String connectingShinyAppsAccount();
    String uncheckAll();
    String checkAll();
    String replace();
    String checkForExistingAppMessage(String appName, String server, String url);
    String overwriteAppName(String appName);
    String index();
    String onAddFileClickMessage(String dirPath);
    String cannotAddFile();
    String selectFile();
    String couldNotFindFilesToDeploy(String errorMessage);
    String couldNotDetermineListToDeploy();
    String itemExceedsDeploymentSize(String fileSource, String maxSize);
    String collectingFiles();
    String couldNotDetermineListToDeployReRender();
    String finishedDocumentNotFoundMessage();
    String finishedDocumentNotFound();
    String errorRetrievingAccountsWithMessage(String errorMessage);
    String errorRetrievingApplicationAppId(String appId);
    String publishFromAccount();
    String createNewAccountMessage();
    String createNewAccount();
    String deploying();
    String launchBrowser();
    String publishToServer();
    String contentPublishFailedMessage();
    String contentPublishFailed();
    String clearListMessage(String appLabel);
    String clearList();
    String thisApplication();
    String publishContent(String contentDesc);
    String otherDestination();
    String removeLocalDeploymentMessage(String appLabel);
    String republish();
    String contentNotSupportedForPublishing(String contentTypeDesc);
    String cantPublishContent(String contentTypeDesc);
    String unsavedDocumentPublishMessage();
    String unsavedDocument();
    String noHTMLGenerated();
    String hostNotRegisteredMessage(String host);
    String hostNotRegistered(String host);
    String publishOptions();
    String reconnectAccount();
    String publishingContentLabel();
    String publishWizardLabel();

    @DefaultMessage("{0,number} environment variables will be published with this {1}.")
    @AlternateMessage({"one", "1 environment variable will be published with this {1}."})
    String envVarsPublishMessage(@PluralCount int itemCount, String contentType);

    String envVarsSelectMessage(String contentType);
    String close();
    String environmentVariablesDialogTitle();
    String environmentVariablesHelpLinkLabel();
    String noEnvVarsAvailable();
}

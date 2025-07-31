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

    @Key("couldNotPublish")
    String couldNotPublish();

    @Key("preparingForPublish")
    String preparingForPublish();

    @Key("error")
    String error();

    @Key("noRunningDeploymentsFound")
    String noRunningDeploymentsFound();

    @Key("noApplicationDeploymentsFrom")
    String noApplicationDeploymentsFrom(String dir);

    @Key("errorListingApplications")
    String errorListingApplications();

    @Key("noApplicationDeploymentsFound")
    String noApplicationDeploymentsFound(String dir);

    @Key("noDeploymentsFound")
    String noDeploymentsFound();

    @Key("couldNotDetermineAppDeployments")
    String couldNotDetermineAppDeployments(String dir, String message);

    @Key("errorConfiguringApplication")
    String errorConfiguringApplication();

    @Key("errorDeployingApplication")
    String errorDeployingApplication();

    @Key("couldNotDeployApplication")
    String couldNotDeployApplication(String recordName, String message);

    @Key("onlyOneDeploymentAtATime")
    String onlyOneDeploymentAtATime();

    @Key("deploymentInProgress")
    String deploymentInProgress();

    @Key("uploadingContent")
    String uploadingContent(String contentType);

    @Key("uploadToRPubsErrorMessage")
    String uploadToRPubsErrorMessage();

    @Key("publishDocument")
    String publishDocument();

    @Key("showUnsupportedRPubsFormatMessageError")
    String showUnsupportedRPubsFormatMessageError();

    @Key("unsupportedDocumentFormat")
    String unsupportedDocumentFormat();

    @Key("rstudioDeployingApp")
    String rstudioDeployingApp(String appName);

    @Key("deploymentStarted")
    String deploymentStarted();

    @Key("content")
    String content();

    @Key("quartoWebsite")
    String quartoWebsite();

    @Key("api")
    String api();

    @Key("website")
    String website();

    @Key("presentation")
    String presentation();

    @Key("document")
    String document();

    @Key("html")
    String html();

    @Key("plot")
    String plot();

    @Key("application")
    String application();

    @Key("onRSConnectDeploymentFailedHtml")
    String onRSConnectDeploymentFailedHtml(String p1, String p2, String serverUrl, String p3, String p4, String errorCode);

    @Key("onRSConnectDeploymentFailedHtmlP1")
    String onRSConnectDeploymentFailedHtmlP1();

    @Key("onRSConnectDeploymentFailedHtmlP2")
    String onRSConnectDeploymentFailedHtmlP2();

    @Key("onRSConnectDeploymentFailedHtmlP3")
    String onRSConnectDeploymentFailedHtmlP3();

    @Key("onRSConnectDeploymentFailedHtmlP4")
    String onRSConnectDeploymentFailedHtmlP4();

    @Key("okCapitalized")
    String okCapitalized();

    @Key("publishFailed")
    String publishFailed();

    @Key("cancel")
    String cancel();

    @Key("stopDeployment")
    String stopDeployment();

    @Key("deploymentNotCancelledMessage")
    String deploymentNotCancelledMessage();

    @Key("couldNotCancelDeployment")
    String couldNotCancelDeployment();

    @Key("errorStoppingDeployment")
    String errorStoppingDeployment();

    @Key("onRSConnectDeploymentCancelledMessage")
    String onRSConnectDeploymentCancelledMessage();

    @Key("stopDeploymentQuestion")
    String stopDeploymentQuestion();

    @Key("publishAnyway")
    String publishAnyway();

    @Key("reviewIssues")
    String reviewIssues();

    @Key("publishContentIssuesMessage")
    String publishContentIssuesMessage();

    @Key("publishContentIssuesFound")
    String publishContentIssuesFound();

    @Key("lintFailedMessage")
    String lintFailedMessage(String errorMessage);

    @Key("lintFailed")
    String lintFailed();

    @Key("apiNotPublishableMessage")
    String apiNotPublishableMessage();

    @Key("apiNotPublishable")
    String apiNotPublishable();

    @Key("contentNotPublishableMessage")
    String contentNotPublishableMessage();

    @Key("contentNotPublishable")
    String contentNotPublishable();

    @Key("publishRpubTitle")
    String publishRpubTitle(String contentTypeDesc);

    @Key("republishDocumentMessage")
    String republishDocumentMessage();

    @Key("republishDocument")
    String republishDocument();

    @Key("currentPlot")
    String currentPlot();

    @Key("rsConnectServerInfoString")
    String rsConnectServerInfoString(String name, String url, String version, String about);

    @Key("titleMinimumCharacter")
    String titleMinimumCharacter();

    @Key("titleContainAlphanumeric")
    String titleContainAlphanumeric();

    @Key("errorConnectingAccountMessage")
    String errorConnectingAccountMessage(String infoString, String errorMessage);

    @Key("errorConnectingAccount")
    String errorConnectingAccount();

    @Key("settingUpAccount")
    String settingUpAccount();

    @Key("accountValidationFailedMessage")
    String accountValidationFailedMessage(String serverInfo, String errorMessage);

    @Key("accountValidationFailed")
    String accountValidationFailed();

    @Key("accountNotConnectedMessage")
    String accountNotConnectedMessage();

    @Key("accountNotConnected")
    String accountNotConnected();

    @Key("serverCouldntBeValidated")
    String serverCouldntBeValidated(String errorMessage);

    @Key("serverValidationFailedMessage")
    String serverValidationFailedMessage(String serverUrl, String infoMessage);

    @Key("serverValidationFailed")
    String serverValidationFailed();

    @Key("checkingServerConnection")
    String checkingServerConnection();

    @Key("verifyingAccount")
    String verifyingAccount();

    @Key("newRSConnectCloudPageCaption")
    String newRSConnectCloudPageCaption();

    @Key("newRSConnectCloudPageSubTitle")
    String newRSConnectCloudPageSubTitle();

    @Key("rstudioConnectAccount")
    String rstudioConnectAccount();

    @Key("rPubsSubtitle")
    String rPubsSubtitle();

    @Key("publishTo")
    String publishTo();

    @Key("publish")
    String publish();

    @Key("publishMultiplePageSubtitle")
    String publishMultiplePageSubtitle(String directoryString);

    @Key("publishMultiplePageTitle")
    String publishMultiplePageTitle();

    @Key("publishMultiplePagSingleSubtitle")
    String publishMultiplePagSingleSubtitle(String name);

    @Key("publishMultiplePagSingleTitle")
    String publishMultiplePagSingleTitle();

    @Key("publishMultiplePageCaption")
    String publishMultiplePageCaption();

    @Key("publishReportSourcePageStaticSubtitle")
    String publishReportSourcePageStaticSubtitle();

    @Key("publishReportSourcePageStaticTitle")
    String publishReportSourcePageStaticTitle(String descriptor);

    @Key("publishReportSourcePageSubTitle")
    String publishReportSourcePageSubTitle(String scheduledReportNumber, String descriptor);

    @Key("publishReportNoScheduledSourcePageSubtitle")
    String publishReportNoScheduledSourcePageSubtitle(String descriptor);

    @Key("scheduledReportsPlural")
    String scheduledReportsPlural();

    @Key("scheduledReportsSingular")
    String scheduledReportsSingular();

    @Key("publishFilesPageTitle")
    String publishFilesPageTitle(String descriptor);

    @Key("websiteLowercase")
    String websiteLowercase();

    @Key("documentsLowercasePlural")
    String documentsLowercasePlural();

    @Key("documentLowercase")
    String documentLowercase();

    @Key("publishToRstudioConnect")
    String publishToRstudioConnect();

    @Key("publishToRpubs")
    String publishToRpubs();

    @Key("accountConnectFailedMessage")
    String accountConnectFailedMessage(String serverInfo, String errorMessage);

    @Key("accountConnectFailed")
    String accountConnectFailed();

    @Key("addingAccount")
    String addingAccount();

    @Key("errorAccountMessage")
    String errorAccountMessage(String cmdString);

    @Key("connectingAccount")
    String connectingAccount();

    @Key("errorAccountMessageSetInfo")
    String errorAccountMessageSetInfo();

    @Key("rStudioCouldNotRetrieveServerInfo")
    String rStudioCouldNotRetrieveServerInfo();

    @Key("cantFindServers")
    String cantFindServers();

    @Key("rStudioCouldNotRetrieveForAccount")
    String rStudioCouldNotRetrieveForAccount();

    @Key("serverInformationNotFound")
    String serverInformationNotFound();

    @Key("errorRetrievingAccounts")
    String errorRetrievingAccounts();

    @Key("noAccountsConnected")
    String noAccountsConnected();

    @Key("rStudioConnectServiceDescription")
    String rStudioConnectServiceDescription();

    @Key("rStudioConnect")
    String rStudioConnect();

    @Key("connectAccount")
    String connectAccount();

    @Key("chooseAccountType")
    String chooseAccountType();

    @Key("connectPublishingAccount")
    String connectPublishingAccount();

    @Key("pickAnAccount")
    String pickAnAccount();

    @Key("confirmAccountOn")
    String confirmAccountOn(String serverName);

    @Key("connectingShinyAppsAccount")
    String connectingShinyAppsAccount();

    @Key("uncheckAll")
    String uncheckAll();

    @Key("checkAll")
    String checkAll();

    @Key("replace")
    String replace();

    @Key("checkForExistingAppMessage")
    String checkForExistingAppMessage(String appName, String server, String url);

    @Key("overwriteAppName")
    String overwriteAppName(String appName);

    @Key("index")
    String index();

    @Key("onAddFileClickMessage")
    String onAddFileClickMessage(String dirPath);

    @Key("cannotAddFile")
    String cannotAddFile();

    @Key("selectFile")
    String selectFile();

    @Key("couldNotFindFilesToDeploy")
    String couldNotFindFilesToDeploy(String errorMessage);

    @Key("couldNotDetermineListToDeploy")
    String couldNotDetermineListToDeploy();

    @Key("itemExceedsDeploymentSize")
    String itemExceedsDeploymentSize(String fileSource, String maxSize);

    @Key("collectingFiles")
    String collectingFiles();

    @Key("couldNotDetermineListToDeployReRender")
    String couldNotDetermineListToDeployReRender();

    @Key("finishedDocumentNotFoundMessage")
    String finishedDocumentNotFoundMessage();

    @Key("finishedDocumentNotFound")
    String finishedDocumentNotFound();

    @Key("errorRetrievingAccountsWithMessage")
    String errorRetrievingAccountsWithMessage(String errorMessage);

    @Key("errorRetrievingApplicationAppId")
    String errorRetrievingApplicationAppId(String appId);

    @Key("publishFromAccount")
    String publishFromAccount();

    @Key("createNewAccountMessage")
    String createNewAccountMessage();

    @Key("createNewAccount")
    String createNewAccount();

    @Key("deploying")
    String deploying();

    @Key("launchBrowser")
    String launchBrowser();

    @Key("publishToServer")
    String publishToServer();

    @Key("contentPublishFailedMessage")
    String contentPublishFailedMessage();

    @Key("contentPublishFailed")
    String contentPublishFailed();

    @Key("clearListMessage")
    String clearListMessage(String appLabel);

    @Key("clearList")
    String clearList();

    @Key("thisApplication")
    String thisApplication();

    @Key("publishContent")
    String publishContent(String contentDesc);

    @Key("otherDestination")
    String otherDestination();

    @Key("removeLocalDeploymentMessage")
    String removeLocalDeploymentMessage(String appLabel);

    @Key("republish")
    String republish();

    @Key("contentNotSupportedForPublishing")
    String contentNotSupportedForPublishing(String contentTypeDesc);

    @Key("cantPublishContent")
    String cantPublishContent(String contentTypeDesc);

    @Key("unsavedDocumentPublishMessage")
    String unsavedDocumentPublishMessage();

    @Key("unsavedDocument")
    String unsavedDocument();

    @Key("noHTMLGenerated")
    String noHTMLGenerated();

    @Key("hostNotRegisteredMessage")
    String hostNotRegisteredMessage(String host);

    @Key("hostNotRegistered")
    String hostNotRegistered(String host);

    @Key("publishOptions")
    String publishOptions();

    @Key("reconnectAccount")
    String reconnectAccount();

    @Key("publishingContentLabel")
    String publishingContentLabel();

    @Key("publishWizardLabel")
    String publishWizardLabel();

    @AlternateMessage({"one", "1 environment variable will be published with this {1}."})
    String envVarsPublishMessage(@PluralCount int itemCount, String contentType);
    
    String envVarsSelectMessage(String contentType);
    
    String close();
    
    String environmentVariablesDialogTitle();
    
    String environmentVariablesHelpLinkLabel();
    
    String noEnvVarsAvailable();
    
}

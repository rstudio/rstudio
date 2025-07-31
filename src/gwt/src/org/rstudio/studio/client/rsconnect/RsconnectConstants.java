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

    @DefaultMessage("Could Not Publish")
    @Key("couldNotPublish")
    String couldNotPublish();

    @DefaultMessage("Preparing for Publish...")
    @Key("preparingForPublish")
    String preparingForPublish();

    @DefaultMessage("Error")
    @Key("error")
    String error();

    @DefaultMessage("No Running Deployments Found")
    @Key("noRunningDeploymentsFound")
    String noRunningDeploymentsFound();

    @DefaultMessage("No applications deployed from ''{0}'' appear to be running.")
    @Key("noApplicationDeploymentsFrom")
    String noApplicationDeploymentsFrom(String dir);

    @DefaultMessage("Error Listing Applications")
    @Key("errorListingApplications")
    String errorListingApplications();

    @DefaultMessage("No application deployments were found for ''{0}''")
    @Key("noApplicationDeploymentsFound")
    String noApplicationDeploymentsFound(String dir);

    @DefaultMessage("No Deployments Found")
    @Key("noDeploymentsFound")
    String noDeploymentsFound();

    @DefaultMessage("Could not determine application deployments for ''{0}'':{1}")
    @Key("couldNotDetermineAppDeployments")
    String couldNotDetermineAppDeployments(String dir, String message);

    @DefaultMessage("Error Configuring Application")
    @Key("errorConfiguringApplication")
    String errorConfiguringApplication();

    @DefaultMessage("Error Deploying Application")
    @Key("errorDeployingApplication")
    String errorDeployingApplication();

    @DefaultMessage("Could not deploy application ''{0}'': {1}")
    @Key("couldNotDeployApplication")
    String couldNotDeployApplication(String recordName, String message);

    @DefaultMessage("Another deployment is currently in progress; only one deployment can be performed at a time.")
    @Key("onlyOneDeploymentAtATime")
    String onlyOneDeploymentAtATime();

    @DefaultMessage("Deployment In Progress")
    @Key("deploymentInProgress")
    String deploymentInProgress();

    @DefaultMessage("Uploading {0}")
    @Key("uploadingContent")
    String uploadingContent(String contentType);

    @DefaultMessage("Only rendered documents can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document.")
    @Key("uploadToRPubsErrorMessage")
    String uploadToRPubsErrorMessage();

    @DefaultMessage("Publish Document")
    @Key("publishDocument")
    String publishDocument();

    @DefaultMessage("Only documents rendered to HTML can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document.")
    @Key("showUnsupportedRPubsFormatMessageError")
    String showUnsupportedRPubsFormatMessageError();

    @DefaultMessage("Unsupported Document Format")
    @Key("unsupportedDocumentFormat")
    String unsupportedDocumentFormat();

    @DefaultMessage("RStudio is deploying {0}. Check the Deploy console tab in the main window for status updates. ")
    @Key("rstudioDeployingApp")
    String rstudioDeployingApp(String appName);

    @DefaultMessage("Deployment Started")
    @Key("deploymentStarted")
    String deploymentStarted();

    @DefaultMessage("Content")
    @Key("content")
    String content();

    @DefaultMessage("Quarto Website")
    @Key("quartoWebsite")
    String quartoWebsite();

    @DefaultMessage("API")
    @Key("api")
    String api();

    @DefaultMessage("Website")
    @Key("website")
    String website();

    @DefaultMessage("Presentation")
    @Key("presentation")
    String presentation();

    @DefaultMessage("Document")
    @Key("document")
    String document();

    @DefaultMessage("HTML")
    @Key("html")
    String html();

    @DefaultMessage("Plot")
    @Key("plot")
    String plot();

    @DefaultMessage("Application")
    @Key("application")
    String application();

    @DefaultMessage("<p>{0}</p><p>{1}</p><p><a href=\"{2}\">{2}</a></p><p>{3}</p><p><small>{4}{5}</small></p>")
    @Key("onRSConnectDeploymentFailedHtml")
    String onRSConnectDeploymentFailedHtml(String p1, String p2, String serverUrl, String p3, String p4, String errorCode);

    @DefaultMessage("Your content could not be published because of a problem on the server.")
    @Key("onRSConnectDeploymentFailedHtmlP1")
    String onRSConnectDeploymentFailedHtmlP1();

    @DefaultMessage("More information may be available on the server''s home page:")
    @Key("onRSConnectDeploymentFailedHtmlP2")
    String onRSConnectDeploymentFailedHtmlP2();

    @DefaultMessage("If the error persists, contact the server''s administrator.")
    @Key("onRSConnectDeploymentFailedHtmlP3")
    String onRSConnectDeploymentFailedHtmlP3();

    @DefaultMessage("Error code: ")
    @Key("onRSConnectDeploymentFailedHtmlP4")
    String onRSConnectDeploymentFailedHtmlP4();

    @DefaultMessage("OK")
    @Key("okCapitalized")
    String okCapitalized();

    @DefaultMessage("Publish Failed")
    @Key("publishFailed")
    String publishFailed();

    @DefaultMessage("Cancel")
    @Key("cancel")
    String cancel();

    @DefaultMessage("Stop deployment")
    @Key("stopDeployment")
    String stopDeployment();

    @DefaultMessage("The deployment could not be cancelled; it is not running, or termination failed.")
    @Key("deploymentNotCancelledMessage")
    String deploymentNotCancelledMessage();

    @DefaultMessage("Could not cancel deployment")
    @Key("couldNotCancelDeployment")
    String couldNotCancelDeployment();

    @DefaultMessage("Error Stopping Deployment")
    @Key("errorStoppingDeployment")
    String errorStoppingDeployment();

    @DefaultMessage("Do you want to stop the deployment process? If the server has already received the content, it will still be published.")
    @Key("onRSConnectDeploymentCancelledMessage")
    String onRSConnectDeploymentCancelledMessage();

    @DefaultMessage("Stop deployment?")
    @Key("stopDeploymentQuestion")
    String stopDeploymentQuestion();

    @DefaultMessage("Publish Anyway")
    @Key("publishAnyway")
    String publishAnyway();

    @DefaultMessage("Review Issues")
    @Key("reviewIssues")
    String reviewIssues();

    @DefaultMessage("Some issues were found in your content, which may prevent it from working correctly after publishing. Do you want to review these issues or publish anyway?")
    @Key("publishContentIssuesMessage")
    String publishContentIssuesMessage();

    @DefaultMessage("Publish Content Issues Found")
    @Key("publishContentIssuesFound")
    String publishContentIssuesFound();

    @DefaultMessage("The content you tried to publish could not be checked for errors. Do you want to proceed? \n\n{0}")
    @Key("lintFailedMessage")
    String lintFailedMessage(String errorMessage);

    @DefaultMessage("Lint Failed")
    @Key("lintFailed")
    String lintFailed();

    @DefaultMessage("Publishing to Posit Connect is disabled in the Publishing options.")
    @Key("apiNotPublishableMessage")
    String apiNotPublishableMessage();

    @DefaultMessage("API Not Publishable")
    @Key("apiNotPublishable")
    String apiNotPublishable();

    @DefaultMessage("Only self-contained documents can currently be published to RPubs.")
    @Key("contentNotPublishableMessage")
    String contentNotPublishableMessage();

    @DefaultMessage("Content Not Publishable")
    @Key("contentNotPublishable")
    String contentNotPublishable();

    @DefaultMessage("Publish {0}")
    @Key("publishRpubTitle")
    String publishRpubTitle(String contentTypeDesc);

    @DefaultMessage("Only rendered documents can be republished to RPubs. To republish this document, click Knit or Preview to render it to HTML, then click the Republish button above the rendered document.")
    @Key("republishDocumentMessage")
    String republishDocumentMessage();

    @DefaultMessage("Republish Document")
    @Key("republishDocument")
    String republishDocument();

    @DefaultMessage("Current Plot")
    @Key("currentPlot")
    String currentPlot();

    @DefaultMessage("Server: {0} ({1})\nVersion: {2}\nAbout: {3}\n")
    @Key("rsConnectServerInfoString")
    String rsConnectServerInfoString(String name, String url, String version, String about);

    @DefaultMessage("The title must contain at least 3 characters.")
    @Key("titleMinimumCharacter")
    String titleMinimumCharacter();

    @DefaultMessage("The title must contain 4 - 64 alphanumeric characters.")
    @Key("titleContainAlphanumeric")
    String titleContainAlphanumeric();

    @DefaultMessage("The server appears to be valid, but rejected the request to authorize an account.\n\n{0}\n{1}")
    @Key("errorConnectingAccountMessage")
    String errorConnectingAccountMessage(String infoString, String errorMessage);

    @DefaultMessage("Error Connecting Account")
    @Key("errorConnectingAccount")
    String errorConnectingAccount();

    @DefaultMessage("Setting up an account...")
    @Key("settingUpAccount")
    String settingUpAccount();

    @DefaultMessage("RStudio failed to determine whether the account was valid. Try again; if the error persists, contact your server administrator.\n\n{0}\n{1}")
    @Key("accountValidationFailedMessage")
    String accountValidationFailedMessage(String serverInfo, String errorMessage);

    @DefaultMessage("Account Validation Failed")
    @Key("accountValidationFailed")
    String accountValidationFailed();

    @DefaultMessage("Authentication failed. If you did not cancel authentication, try again, or contact your server administrator for assistance.")
    @Key("accountNotConnectedMessage")
    String accountNotConnectedMessage();

    @DefaultMessage("Account Not Connected")
    @Key("accountNotConnected")
    String accountNotConnected();

    @DefaultMessage("The server couldn''t be validated. {0}")
    @Key("serverCouldntBeValidated")
    String serverCouldntBeValidated(String errorMessage);

    @DefaultMessage("The URL ''{0}'' does not appear to belong to a valid server. Please double check the URL, and contact your administrator if the problem persists.\n\n{1}")
    @Key("serverValidationFailedMessage")
    String serverValidationFailedMessage(String serverUrl, String infoMessage);

    @DefaultMessage("Server Validation Failed")
    @Key("serverValidationFailed")
    String serverValidationFailed();

    @DefaultMessage("Checking server connection...")
    @Key("checkingServerConnection")
    String checkingServerConnection();

    @DefaultMessage("Verifying Account")
    @Key("verifyingAccount")
    String verifyingAccount();

    @DefaultMessage("Connect ShinyApps.io Account")
    @Key("newRSConnectCloudPageCaption")
    String newRSConnectCloudPageCaption();

    @DefaultMessage("A cloud service run by RStudio. Publish Shiny applications and interactive documents to the Internet.")
    @Key("newRSConnectCloudPageSubTitle")
    String newRSConnectCloudPageSubTitle();

    @DefaultMessage("Posit Connect Account")
    @Key("rstudioConnectAccount")
    String rstudioConnectAccount();

    @DefaultMessage("RPubs is a free service from RStudio for sharing documents on the web.")
    @Key("rPubsSubtitle")
    String rPubsSubtitle();

    @DefaultMessage("Publish To")
    @Key("publishTo")
    String publishTo();

    @DefaultMessage("Publish")
    @Key("publish")
    String publish();

    @DefaultMessage("All of the documents in the directory {0} will be published.")
    @Key("publishMultiplePageSubtitle")
    String publishMultiplePageSubtitle(String directoryString);

    @DefaultMessage("Publish all documents in the directory")
    @Key("publishMultiplePageTitle")
    String publishMultiplePageTitle();

    @DefaultMessage("Only the document {0} will be published.")
    @Key("publishMultiplePagSingleSubtitle")
    String publishMultiplePagSingleSubtitle(String name);

    @DefaultMessage("Publish just this document")
    @Key("publishMultiplePagSingleTitle")
    String publishMultiplePagSingleTitle();

    @DefaultMessage("What do you want to publish?")
    @Key("publishMultiplePageCaption")
    String publishMultiplePageCaption();

    @DefaultMessage("Choose this option to publish the content as it appears in RStudio.")
    @Key("publishReportSourcePageStaticSubtitle")
    String publishReportSourcePageStaticSubtitle();

    @DefaultMessage("Publish finished {0} only")
    @Key("publishReportSourcePageStaticTitle")
    String publishReportSourcePageStaticTitle(String descriptor);

    @DefaultMessage("Choose this option if you want to create {0} or rebuild your {1} on the server.")
    @Key("publishReportSourcePageSubTitle")
    String publishReportSourcePageSubTitle(String scheduledReportNumber, String descriptor);

    @DefaultMessage("Choose this option if you want to be able to rebuild your {0} on the server.")
    @Key("publishReportNoScheduledSourcePageSubtitle")
    String publishReportNoScheduledSourcePageSubtitle(String descriptor);

    @DefaultMessage("scheduled reports")
    @Key("scheduledReportsPlural")
    String scheduledReportsPlural();

    @DefaultMessage("a scheduled report")
    @Key("scheduledReportsSingular")
    String scheduledReportsSingular();

    @DefaultMessage("Publish {0} with source code")
    @Key("publishFilesPageTitle")
    String publishFilesPageTitle(String descriptor);

    @DefaultMessage("website")
    @Key("websiteLowercase")
    String websiteLowercase();

    @DefaultMessage("documents")
    @Key("documentsLowercasePlural")
    String documentsLowercasePlural();

    @DefaultMessage("document")
    @Key("documentLowercase")
    String documentLowercase();

    @DefaultMessage("Publish to Posit Connect")
    @Key("publishToRstudioConnect")
    String publishToRstudioConnect();

    @DefaultMessage("Publish to RPubs")
    @Key("publishToRpubs")
    String publishToRpubs();

    @DefaultMessage("Your account was authenticated successfully, but could not be connected to RStudio. Make sure your installation of the ''rsconnect'' package is correct for the server you''re connecting to.\\n\\n{0}\\n{1}")
    @Key("accountConnectFailedMessage")
    String accountConnectFailedMessage(String serverInfo, String errorMessage);

    @DefaultMessage("Account Connect Failed")
    @Key("accountConnectFailed")
    String accountConnectFailed();

    @DefaultMessage("Adding account...")
    @Key("addingAccount")
    String addingAccount();

    @DefaultMessage("The command ''{0}'' failed. You can set up an account manually by using rsconnect::setAccountInfo; type ?rsconnect::setAccountInfo at the R console for more information.")
    @Key("errorAccountMessage")
    String errorAccountMessage(String cmdString);

    @DefaultMessage("Connecting account...")
    @Key("connectingAccount")
    String connectingAccount();

    @DefaultMessage("The pasted command should start with rsconnect::setAccountInfo. If you''re having trouble, try connecting your account manually; type ?rsconnect::setAccountInfo at the R console for help.")
    @Key("errorAccountMessageSetInfo")
    String errorAccountMessageSetInfo();

    @DefaultMessage("RStudio could not retrieve server information.")
    @Key("rStudioCouldNotRetrieveServerInfo")
    String rStudioCouldNotRetrieveServerInfo();

    @DefaultMessage("Can''t Find Servers")
    @Key("cantFindServers")
    String cantFindServers();

    @DefaultMessage("RStudio could not retrieve server information for the selected account.")
    @Key("rStudioCouldNotRetrieveForAccount")
    String rStudioCouldNotRetrieveForAccount();

    @DefaultMessage("Server Information Not Found")
    @Key("serverInformationNotFound")
    String serverInformationNotFound();

    @DefaultMessage("Error retrieving accounts")
    @Key("errorRetrievingAccounts")
    String errorRetrievingAccounts();

    @DefaultMessage("No accounts connected.")
    @Key("noAccountsConnected")
    String noAccountsConnected();

    @DefaultMessage("Posit Connect is a server product from Posit for secure sharing of applications, reports, plots, and APIs.")
    @Key("rStudioConnectServiceDescription")
    String rStudioConnectServiceDescription();

    @DefaultMessage("Posit Connect")
    @Key("rStudioConnect")
    String rStudioConnect();

    @DefaultMessage("Connect Account")
    @Key("connectAccount")
    String connectAccount();

    @DefaultMessage("Choose Account Type")
    @Key("chooseAccountType")
    String chooseAccountType();

    @DefaultMessage("Connect Publishing Account")
    @Key("connectPublishingAccount")
    String connectPublishingAccount();

    @DefaultMessage("Pick an account")
    @Key("pickAnAccount")
    String pickAnAccount();

    @DefaultMessage("Confirm account on {0}")
    @Key("confirmAccountOn")
    String confirmAccountOn(String serverName);

    @DefaultMessage("Connecting your ShinyApps Account")
    @Key("connectingShinyAppsAccount")
    String connectingShinyAppsAccount();

    @DefaultMessage("Uncheck All")
    @Key("uncheckAll")
    String uncheckAll();

    @DefaultMessage("Check All")
    @Key("checkAll")
    String checkAll();

    @DefaultMessage("Replace")
    @Key("replace")
    String replace();

    @DefaultMessage("You''ve already published an application named ''{0}'' to {1} ({2}). Do you want to replace the existing application with this content?")
    @Key("checkForExistingAppMessage")
    String checkForExistingAppMessage(String appName, String server, String url);

    @DefaultMessage("Overwrite {0}?")
    @Key("overwriteAppName")
    String overwriteAppName(String appName);

    @DefaultMessage("index")
    @Key("index")
    String index();

    @DefaultMessage("Only files in the same folder as the document ({0}) or one of its sub-folders may be added.")
    @Key("onAddFileClickMessage")
    String onAddFileClickMessage(String dirPath);

    @DefaultMessage("Cannot Add File")
    @Key("cannotAddFile")
    String cannotAddFile();

    @DefaultMessage("Select File")
    @Key("selectFile")
    String selectFile();

    @DefaultMessage("Could not find files to deploy: \n\n{0}")
    @Key("couldNotFindFilesToDeploy")
    String couldNotFindFilesToDeploy(String errorMessage);

    @DefaultMessage("Could not determine the list of files to deploy.")
    @Key("couldNotDetermineListToDeploy")
    String couldNotDetermineListToDeploy();

    @DefaultMessage("The item to be deployed ({0}) exceeds the maximum deployment size, which is {1}. Consider creating a new directory containing only the content you wish to deploy.")
    @Key("itemExceedsDeploymentSize")
    String itemExceedsDeploymentSize(String fileSource, String maxSize);

    @DefaultMessage("Collecting files...")
    @Key("collectingFiles")
    String collectingFiles();

    @DefaultMessage("Could not determine the list of files to deploy. Try re-rendering and ensuring that you''re publishing to a server which supports this kind of content.")
    @Key("couldNotDetermineListToDeployReRender")
    String couldNotDetermineListToDeployReRender();

    @DefaultMessage("To publish finished document, you must first render it. Dismiss this message, click Knit to render the document, then try publishing again.")
    @Key("finishedDocumentNotFoundMessage")
    String finishedDocumentNotFoundMessage();

    @DefaultMessage("Finished Document Not Found")
    @Key("finishedDocumentNotFound")
    String finishedDocumentNotFound();

    @DefaultMessage("Error retrieving accounts:\n\n{0}")
    @Key("errorRetrievingAccountsWithMessage")
    String errorRetrievingAccountsWithMessage(String errorMessage);

    @DefaultMessage("Error retrieving application {0}.")
    @Key("errorRetrievingApplicationAppId")
    String errorRetrievingApplicationAppId(String appId);

    @DefaultMessage("Publish from Account")
    @Key("publishFromAccount")
    String publishFromAccount();

    @DefaultMessage("To publish this content to a new location, click the Publish drop-down menu and choose Other Destination.")
    @Key("createNewAccountMessage")
    String createNewAccountMessage();

    @DefaultMessage("Create New Content")
    @Key("createNewAccount")
    String createNewAccount();

    @DefaultMessage("Deploying...")
    @Key("deploying")
    String deploying();

    @DefaultMessage("Launch browser")
    @Key("launchBrowser")
    String launchBrowser();

    @DefaultMessage("Publish to Server")
    @Key("publishToServer")
    String publishToServer();

    @DefaultMessage("Unable to determine file to be published. Click Knit or Preview to render it again, then click the Publish button above the rendered document.")
    @Key("contentPublishFailedMessage")
    String contentPublishFailedMessage();

    @DefaultMessage("Content Publish Failed")
    @Key("contentPublishFailed")
    String contentPublishFailed();

    @DefaultMessage("Local deployment history for {0} successfully removed.")
    @Key("clearListMessage")
    String clearListMessage(String appLabel);

    @DefaultMessage("Clear List")
    @Key("clearList")
    String clearList();

    @DefaultMessage("this application")
    @Key("thisApplication")
    String thisApplication();

    @DefaultMessage("Publish {0}...")
    @Key("publishContent")
    String publishContent(String contentDesc);

    @DefaultMessage("Other Destination...")
    @Key("otherDestination")
    String otherDestination();

    @DefaultMessage("Are you sure you want to remove all local deployment history for {0}?")
    @Key("removeLocalDeploymentMessage")
    String removeLocalDeploymentMessage(String appLabel);

    @DefaultMessage("Republish")
    @Key("republish")
    String republish();

    @DefaultMessage("The content type ''{0}'' is not currently supported for publishing.")
    @Key("contentNotSupportedForPublishing")
    String contentNotSupportedForPublishing(String contentTypeDesc);

    @DefaultMessage("Can''t publish {0}")
    @Key("cantPublishContent")
    String cantPublishContent(String contentTypeDesc);

    @DefaultMessage("Unsaved documents cannot be published. Save the document before publishing it.")
    @Key("unsavedDocumentPublishMessage")
    String unsavedDocumentPublishMessage();

    @DefaultMessage("Unsaved Document")
    @Key("unsavedDocument")
    String unsavedDocument();

    @DefaultMessage("No HTML could be generated for the content.")
    @Key("noHTMLGenerated")
    String noHTMLGenerated();

    @DefaultMessage("This copy of the content has been published to the server ''{0}'', but you currently do not have any accounts registered on that server. \n\nConnect an account on the server ''{0}'' to update the application, or publish the content to a different server.")
    @Key("hostNotRegisteredMessage")
    String hostNotRegisteredMessage(String host);

    @DefaultMessage("{0} Not Registered")
    @Key("hostNotRegistered")
    String hostNotRegistered(String host);

    @DefaultMessage("Publish options")
    @Key("publishOptions")
    String publishOptions();

    @DefaultMessage("Reconnect Account")
    @Key("reconnectAccount")
    String reconnectAccount();

    @DefaultMessage("Publishing content")
    @Key("publishingContentLabel")
    String publishingContentLabel();

    @DefaultMessage("Publish Wizard")
    @Key("publishWizardLabel")
    String publishWizardLabel();

    @DefaultMessage("{0,number} environment variables will be published with this {1}.")
    @AlternateMessage({"one", "1 environment variable will be published with this {1}."})
    String envVarsPublishMessage(@PluralCount int itemCount, String contentType);
    
    @DefaultMessage("Select one or more environment variables to publish with this {0}.")
    String envVarsSelectMessage(String contentType);
    
    @DefaultMessage("Close")
    String close();
    
    @DefaultMessage("Environment variables")
    String environmentVariablesDialogTitle();
    
    @DefaultMessage("Environment Variables")
    String environmentVariablesHelpLinkLabel();
    
    @DefaultMessage("No environment variables are currently available.")
    String noEnvVarsAvailable();
    
}

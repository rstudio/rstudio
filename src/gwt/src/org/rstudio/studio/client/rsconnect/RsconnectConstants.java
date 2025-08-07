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
    /**
     * Translated "Could Not Publish".
     *
     * @return translated "Could Not Publish"
     */
    @DefaultMessage("Could Not Publish")
    String couldNotPublish();

    /**
     * Translated "Preparing for Publish...".
     *
     * @return translated "Preparing for Publish..."
     */
    @DefaultMessage("Preparing for Publish...")
    String preparingForPublish();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    String error();

    /**
     * Translated "No Running Deployments Found".
     *
     * @return translated "No Running Deployments Found"
     */
    @DefaultMessage("No Running Deployments Found")
    String noRunningDeploymentsFound();

    /**
     * Translated "No applications deployed from ''{0}'' appear to be running.".
     *
     * @return translated "No applications deployed from ''{0}'' appear to be running."
     */
    @DefaultMessage("No applications deployed from ''{0}'' appear to be running.")
    String noApplicationDeploymentsFrom(String dir);

    /**
     * Translated "Error Listing Applications".
     *
     * @return translated "Error Listing Applications"
     */
    @DefaultMessage("Error Listing Applications")
    String errorListingApplications();

    /**
     * Translated "No application deployments were found for ''{0}''".
     *
     * @return translated "No application deployments were found for ''{0}''"
     */
    @DefaultMessage("No application deployments were found for ''{0}''")
    String noApplicationDeploymentsFound(String dir);

    /**
     * Translated "No Deployments Found".
     *
     * @return translated "No Deployments Found"
     */
    @DefaultMessage("No Deployments Found")
    String noDeploymentsFound();

    /**
     * Translated "Could not determine application deployments for ''{0}'':{1}".
     *
     * @return translated "Could not determine application deployments for ''{0}'':{1}"
     */
    @DefaultMessage("Could not determine application deployments for ''{0}'':{1}")
    String couldNotDetermineAppDeployments(String dir, String message);

    /**
     * Translated "Error Configuring Application".
     *
     * @return translated "Error Configuring Application"
     */
    @DefaultMessage("Error Configuring Application")
    String errorConfiguringApplication();

    /**
     * Translated "Error Deploying Application".
     *
     * @return translated "Error Deploying Application"
     */
    @DefaultMessage("Error Deploying Application")
    String errorDeployingApplication();

    /**
     * Translated "Could not deploy application ''{0}'': {1}".
     *
     * @return translated "Could not deploy application ''{0}'': {1}"
     */
    @DefaultMessage("Could not deploy application ''{0}'': {1}")
    String couldNotDeployApplication(String recordName, String message);

    /**
     * Translated "Another deployment is currently in progress; only one deployment can be performed at a time.".
     *
     * @return translated "Another deployment is currently in progress; only one deployment can be performed at a time."
     */
    @DefaultMessage("Another deployment is currently in progress; only one deployment can be performed at a time.")
    String onlyOneDeploymentAtATime();

    /**
     * Translated "Deployment In Progress".
     *
     * @return translated "Deployment In Progress"
     */
    @DefaultMessage("Deployment In Progress")
    String deploymentInProgress();

    /**
     * Translated "Uploading {0}".
     *
     * @return translated "Uploading {0}"
     */
    @DefaultMessage("Uploading {0}")
    String uploadingContent(String contentType);

    /**
     * Translated "Only rendered documents can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document.".
     *
     * @return translated "Only rendered documents can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document."
     */
    @DefaultMessage("Only rendered documents can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document.")
    String uploadToRPubsErrorMessage();

    /**
     * Translated "Publish Document".
     *
     * @return translated "Publish Document"
     */
    @DefaultMessage("Publish Document")
    String publishDocument();

    /**
     * Translated "Only documents rendered to HTML can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document.".
     *
     * @return translated "Only documents rendered to HTML can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document."
     */
    @DefaultMessage("Only documents rendered to HTML can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document.")
    String showUnsupportedRPubsFormatMessageError();

    /**
     * Translated "Unsupported Document Format".
     *
     * @return translated "Unsupported Document Format"
     */
    @DefaultMessage("Unsupported Document Format")
    String unsupportedDocumentFormat();

    /**
     * Translated "RStudio is deploying {0}. Check the Deploy console tab in the main window for status updates. ".
     *
     * @return translated "RStudio is deploying {0}. Check the Deploy console tab in the main window for status updates. "
     */
    @DefaultMessage("RStudio is deploying {0}. Check the Deploy console tab in the main window for status updates. ")
    String rstudioDeployingApp(String appName);

    /**
     * Translated "Deployment Started".
     *
     * @return translated "Deployment Started"
     */
    @DefaultMessage("Deployment Started")
    String deploymentStarted();

    /**
     * Translated "Content".
     *
     * @return translated "Content"
     */
    @DefaultMessage("Content")
    String content();

    /**
     * Translated "Quarto Website".
     *
     * @return translated "Quarto Website"
     */
    @DefaultMessage("Quarto Website")
    String quartoWebsite();

    /**
     * Translated "API".
     *
     * @return translated "API"
     */
    @DefaultMessage("API")
    String api();

    /**
     * Translated "Website".
     *
     * @return translated "Website"
     */
    @DefaultMessage("Website")
    String website();

    /**
     * Translated "Presentation".
     *
     * @return translated "Presentation"
     */
    @DefaultMessage("Presentation")
    String presentation();

    /**
     * Translated "Document".
     *
     * @return translated "Document"
     */
    @DefaultMessage("Document")
    String document();

    /**
     * Translated "HTML".
     *
     * @return translated "HTML"
     */
    @DefaultMessage("HTML")
    String html();

    /**
     * Translated "Plot".
     *
     * @return translated "Plot"
     */
    @DefaultMessage("Plot")
    String plot();

    /**
     * Translated "Application".
     *
     * @return translated "Application"
     */
    @DefaultMessage("Application")
    String application();

    /**
     * Translated "<p>{0}</p><p>{1}</p><p><a href=\"{2}\">{2}</a></p><p>{3}</p><p><small>{4}{5}</small></p>".
     *
     * @return translated "<p>{0}</p><p>{1}</p><p><a href=\"{2}\">{2}</a></p><p>{3}</p><p><small>{4}{5}</small></p>"
     */
    @DefaultMessage("<p>{0}</p><p>{1}</p><p><a href=\"{2}\">{2}</a></p><p>{3}</p><p><small>{4}{5}</small></p>")
    String onRSConnectDeploymentFailedHtml(String p1, String p2, String serverUrl, String p3, String p4, String errorCode);

    /**
     * Translated "Your content could not be published because of a problem on the server.".
     *
     * @return translated "Your content could not be published because of a problem on the server."
     */
    @DefaultMessage("Your content could not be published because of a problem on the server.")
    String onRSConnectDeploymentFailedHtmlP1();

    /**
     * Translated "More information may be available on the server''s home page:".
     *
     * @return translated "More information may be available on the server''s home page:"
     */
    @DefaultMessage("More information may be available on the server''s home page:")
    String onRSConnectDeploymentFailedHtmlP2();

    /**
     * Translated "If the error persists, contact the server''s administrator.".
     *
     * @return translated "If the error persists, contact the server''s administrator."
     */
    @DefaultMessage("If the error persists, contact the server''s administrator.")
    String onRSConnectDeploymentFailedHtmlP3();

    /**
     * Translated "Error code: ".
     *
     * @return translated "Error code: "
     */
    @DefaultMessage("Error code: ")
    String onRSConnectDeploymentFailedHtmlP4();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    String okCapitalized();

    /**
     * Translated "Publish Failed".
     *
     * @return translated "Publish Failed"
     */
    @DefaultMessage("Publish Failed")
    String publishFailed();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    String cancel();

    /**
     * Translated "Stop deployment".
     *
     * @return translated "Stop deployment"
     */
    @DefaultMessage("Stop deployment")
    String stopDeployment();

    /**
     * Translated "The deployment could not be cancelled; it is not running, or termination failed.".
     *
     * @return translated "The deployment could not be cancelled; it is not running, or termination failed."
     */
    @DefaultMessage("The deployment could not be cancelled; it is not running, or termination failed.")
    String deploymentNotCancelledMessage();

    /**
     * Translated "Could not cancel deployment".
     *
     * @return translated "Could not cancel deployment"
     */
    @DefaultMessage("Could not cancel deployment")
    String couldNotCancelDeployment();

    /**
     * Translated "Error Stopping Deployment".
     *
     * @return translated "Error Stopping Deployment"
     */
    @DefaultMessage("Error Stopping Deployment")
    String errorStoppingDeployment();

    /**
     * Translated "Do you want to stop the deployment process? If the server has already received the content, it will still be published.".
     *
     * @return translated "Do you want to stop the deployment process? If the server has already received the content, it will still be published."
     */
    @DefaultMessage("Do you want to stop the deployment process? If the server has already received the content, it will still be published.")
    String onRSConnectDeploymentCancelledMessage();

    /**
     * Translated "Stop deployment?".
     *
     * @return translated "Stop deployment?"
     */
    @DefaultMessage("Stop deployment?")
    String stopDeploymentQuestion();

    /**
     * Translated "Publish Anyway".
     *
     * @return translated "Publish Anyway"
     */
    @DefaultMessage("Publish Anyway")
    String publishAnyway();

    /**
     * Translated "Review Issues".
     *
     * @return translated "Review Issues"
     */
    @DefaultMessage("Review Issues")
    String reviewIssues();

    /**
     * Translated "Some issues were found in your content, which may prevent it from working correctly after publishing. Do you want to review these issues or publish anyway?".
     *
     * @return translated "Some issues were found in your content, which may prevent it from working correctly after publishing. Do you want to review these issues or publish anyway?"
     */
    @DefaultMessage("Some issues were found in your content, which may prevent it from working correctly after publishing. Do you want to review these issues or publish anyway?")
    String publishContentIssuesMessage();

    /**
     * Translated "Publish Content Issues Found".
     *
     * @return translated "Publish Content Issues Found"
     */
    @DefaultMessage("Publish Content Issues Found")
    String publishContentIssuesFound();

    /**
     * Translated "The content you tried to publish could not be checked for errors. Do you want to proceed? \n\n{0}".
     *
     * @return translated "The content you tried to publish could not be checked for errors. Do you want to proceed? \n\n{0}"
     */
    @DefaultMessage("The content you tried to publish could not be checked for errors. Do you want to proceed? \n\n{0}")
    String lintFailedMessage(String errorMessage);

    /**
     * Translated "Lint Failed".
     *
     * @return translated "Lint Failed"
     */
    @DefaultMessage("Lint Failed")
    String lintFailed();

    /**
     * Translated "Publishing to Posit Connect is disabled in the Publishing options.".
     *
     * @return translated "Publishing to Posit Connect is disabled in the Publishing options."
     */
    @DefaultMessage("Publishing to Posit Connect is disabled in the Publishing options.")
    String apiNotPublishableMessage();

    /**
     * Translated "API Not Publishable".
     *
     * @return translated "API Not Publishable"
     */
    @DefaultMessage("API Not Publishable")
    String apiNotPublishable();

    /**
     * Translated "Only self-contained documents can currently be published to RPubs.".
     *
     * @return translated "Only self-contained documents can currently be published to RPubs."
     */
    @DefaultMessage("Only self-contained documents can currently be published to RPubs.")
    String contentNotPublishableMessage();

    /**
     * Translated "Content Not Publishable".
     *
     * @return translated "Content Not Publishable"
     */
    @DefaultMessage("Content Not Publishable")
    String contentNotPublishable();

    /**
     * Translated "Publish {0}".
     *
     * @return translated "Publish {0}"
     */
    @DefaultMessage("Publish {0}")
    String publishRpubTitle(String contentTypeDesc);

    /**
     * Translated "Only rendered documents can be republished to RPubs. To republish this document, click Knit or Preview to render it to HTML, then click the Republish button above the rendered document.".
     *
     * @return translated "Only rendered documents can be republished to RPubs. To republish this document, click Knit or Preview to render it to HTML, then click the Republish button above the rendered document."
     */
    @DefaultMessage("Only rendered documents can be republished to RPubs. To republish this document, click Knit or Preview to render it to HTML, then click the Republish button above the rendered document.")
    String republishDocumentMessage();

    /**
     * Translated "Republish Document".
     *
     * @return translated "Republish Document"
     */
    @DefaultMessage("Republish Document")
    String republishDocument();

    /**
     * Translated "Current Plot".
     *
     * @return translated "Current Plot"
     */
    @DefaultMessage("Current Plot")
    String currentPlot();

    /**
     * Translated "Server: {0} ({1})\nVersion: {2}\nAbout: {3}\n".
     *
     * @return translated "Server: {0} ({1})\nVersion: {2}\nAbout: {3}\n"
     */
    @DefaultMessage("Server: {0} ({1})\nVersion: {2}\nAbout: {3}\n")
    String rsConnectServerInfoString(String name, String url, String version, String about);

    /**
     * Translated "The title must contain at least 3 characters.".
     *
     * @return translated "The title must contain at least 3 characters."
     */
    @DefaultMessage("The title must contain at least 3 characters.")
    String titleMinimumCharacter();

    /**
     * Translated "The title must contain 4 - 64 alphanumeric characters.".
     *
     * @return translated "The title must contain 4 - 64 alphanumeric characters."
     */
    @DefaultMessage("The title must contain 4 - 64 alphanumeric characters.")
    String titleContainAlphanumeric();

    /**
     * Translated "The server appears to be valid, but rejected the request to authorize an account.\n\n{0}\n{1}".
     *
     * @return translated "The server appears to be valid, but rejected the request to authorize an account.\n\n{0}\n{1}"
     */
    @DefaultMessage("The server appears to be valid, but rejected the request to authorize an account.\n\n{0}\n{1}")
    String errorConnectingAccountMessage(String infoString, String errorMessage);

    /**
     * Translated "Error Connecting Account".
     *
     * @return translated "Error Connecting Account"
     */
    @DefaultMessage("Error Connecting Account")
    String errorConnectingAccount();

    /**
     * Translated "Setting up an account...".
     *
     * @return translated "Setting up an account..."
     */
    @DefaultMessage("Setting up an account...")
    String settingUpAccount();

    /**
     * Translated "RStudio failed to determine whether the account was valid. Try again; if the error persists, contact your server administrator.\n\n{0}\n{1}".
     *
     * @return translated "RStudio failed to determine whether the account was valid. Try again; if the error persists, contact your server administrator.\n\n{0}\n{1}"
     */
    @DefaultMessage("RStudio failed to determine whether the account was valid. Try again; if the error persists, contact your server administrator.\n\n{0}\n{1}")
    String accountValidationFailedMessage(String serverInfo, String errorMessage);

    /**
     * Translated "Account Validation Failed".
     *
     * @return translated "Account Validation Failed"
     */
    @DefaultMessage("Account Validation Failed")
    String accountValidationFailed();

    /**
     * Translated "Authentication failed. If you did not cancel authentication, try again, or contact your server administrator for assistance.".
     *
     * @return translated "Authentication failed. If you did not cancel authentication, try again, or contact your server administrator for assistance."
     */
    @DefaultMessage("Authentication failed. If you did not cancel authentication, try again, or contact your server administrator for assistance.")
    String accountNotConnectedMessage();

    /**
     * Translated "Account Not Connected".
     *
     * @return translated "Account Not Connected"
     */
    @DefaultMessage("Account Not Connected")
    String accountNotConnected();

    /**
     * Translated "The server couldn''t be validated. {0}".
     *
     * @return translated "The server couldn''t be validated. {0}"
     */
    @DefaultMessage("The server couldn''t be validated. {0}")
    String serverCouldntBeValidated(String errorMessage);

    /**
     * Translated "The URL ''{0}'' does not appear to belong to a valid server. Please double check the URL, and contact your administrator if the problem persists.\n\n{1}".
     *
     * @return translated "The URL ''{0}'' does not appear to belong to a valid server. Please double check the URL, and contact your administrator if the problem persists.\n\n{1}"
     */
    @DefaultMessage("The URL ''{0}'' does not appear to belong to a valid server. Please double check the URL, and contact your administrator if the problem persists.\n\n{1}")
    String serverValidationFailedMessage(String serverUrl, String infoMessage);

    /**
     * Translated "Server Validation Failed".
     *
     * @return translated "Server Validation Failed"
     */
    @DefaultMessage("Server Validation Failed")
    String serverValidationFailed();

    /**
     * Translated "Checking server connection...".
     *
     * @return translated "Checking server connection..."
     */
    @DefaultMessage("Checking server connection...")
    String checkingServerConnection();

    /**
     * Translated "Verifying Account".
     *
     * @return translated "Verifying Account"
     */
    @DefaultMessage("Verifying Account")
    String verifyingAccount();

    /**
     * Translated "Connect ShinyApps.io Account".
     *
     * @return translated "Connect ShinyApps.io Account"
     */
    @DefaultMessage("Connect ShinyApps.io Account")
    String newRSConnectCloudPageCaption();

    /**
     * Translated "A cloud service run by RStudio. Publish Shiny applications and interactive documents to the Internet.".
     *
     * @return translated "A cloud service run by RStudio. Publish Shiny applications and interactive documents to the Internet."
     */
    @DefaultMessage("A cloud service run by RStudio. Publish Shiny applications and interactive documents to the Internet.")
    String newRSConnectCloudPageSubTitle();

    /**
     * Translated "Posit Connect Account".
     *
     * @return translated "Posit Connect Account"
     */
    @DefaultMessage("Posit Connect Account")
    String rstudioConnectAccount();

    /**
     * Translated "RPubs is a free service from RStudio for sharing documents on the web.".
     *
     * @return translated "RPubs is a free service from RStudio for sharing documents on the web."
     */
    @DefaultMessage("RPubs is a free service from RStudio for sharing documents on the web.")
    String rPubsSubtitle();

    /**
     * Translated "Publish To".
     *
     * @return translated "Publish To"
     */
    @DefaultMessage("Publish To")
    String publishTo();

    /**
     * Translated "Publish".
     *
     * @return translated "Publish"
     */
    @DefaultMessage("Publish")
    String publish();

    /**
     * Translated "All of the documents in the directory {0} will be published.".
     *
     * @return translated "All of the documents in the directory {0} will be published."
     */
    @DefaultMessage("All of the documents in the directory {0} will be published.")
    String publishMultiplePageSubtitle(String directoryString);

    /**
     * Translated "Publish all documents in the directory".
     *
     * @return translated "Publish all documents in the directory"
     */
    @DefaultMessage("Publish all documents in the directory")
    String publishMultiplePageTitle();

    /**
     * Translated "Only the document {0} will be published.".
     *
     * @return translated "Only the document {0} will be published."
     */
    @DefaultMessage("Only the document {0} will be published.")
    String publishMultiplePagSingleSubtitle(String name);

    /**
     * Translated "Publish just this document".
     *
     * @return translated "Publish just this document"
     */
    @DefaultMessage("Publish just this document")
    String publishMultiplePagSingleTitle();

    /**
     * Translated "What do you want to publish?".
     *
     * @return translated "What do you want to publish?"
     */
    @DefaultMessage("What do you want to publish?")
    String publishMultiplePageCaption();

    /**
     * Translated "Choose this option to publish the content as it appears in RStudio.".
     *
     * @return translated "Choose this option to publish the content as it appears in RStudio."
     */
    @DefaultMessage("Choose this option to publish the content as it appears in RStudio.")
    String publishReportSourcePageStaticSubtitle();

    /**
     * Translated "Publish finished {0} only".
     *
     * @return translated "Publish finished {0} only"
     */
    @DefaultMessage("Publish finished {0} only")
    String publishReportSourcePageStaticTitle(String descriptor);

    /**
     * Translated "Choose this option if you want to create {0} or rebuild your {1} on the server.".
     *
     * @return translated "Choose this option if you want to create {0} or rebuild your {1} on the server."
     */
    @DefaultMessage("Choose this option if you want to create {0} or rebuild your {1} on the server.")
    String publishReportSourcePageSubTitle(String scheduledReportNumber, String descriptor);

    /**
     * Translated "Choose this option if you want to be able to rebuild your {0} on the server.".
     *
     * @return translated "Choose this option if you want to be able to rebuild your {0} on the server."
     */
    @DefaultMessage("Choose this option if you want to be able to rebuild your {0} on the server.")
    String publishReportNoScheduledSourcePageSubtitle(String descriptor);

    /**
     * Translated "scheduled reports".
     *
     * @return translated "scheduled reports"
     */
    @DefaultMessage("scheduled reports")
    String scheduledReportsPlural();

    /**
     * Translated "a scheduled report".
     *
     * @return translated "a scheduled report"
     */
    @DefaultMessage("a scheduled report")
    String scheduledReportsSingular();

    /**
     * Translated "Publish {0} with source code".
     *
     * @return translated "Publish {0} with source code"
     */
    @DefaultMessage("Publish {0} with source code")
    String publishFilesPageTitle(String descriptor);

    /**
     * Translated "website".
     *
     * @return translated "website"
     */
    @DefaultMessage("website")
    String websiteLowercase();

    /**
     * Translated "documents".
     *
     * @return translated "documents"
     */
    @DefaultMessage("documents")
    String documentsLowercasePlural();

    /**
     * Translated "document".
     *
     * @return translated "document"
     */
    @DefaultMessage("document")
    String documentLowercase();

    /**
     * Translated "Publish to Posit Connect".
     *
     * @return translated "Publish to Posit Connect"
     */
    @DefaultMessage("Publish to Posit Connect")
    String publishToRstudioConnect();

    /**
     * Translated "Publish to RPubs".
     *
     * @return translated "Publish to RPubs"
     */
    @DefaultMessage("Publish to RPubs")
    String publishToRpubs();

    /**
     * Translated "Your account was authenticated successfully, but could not be connected to RStudio. Make sure your installation of the ''rsconnect'' package is correct for the server you''re connecting to.\n\n{0}\n{1}".
     *
     * @return translated "Your account was authenticated successfully, but could not be connected to RStudio. Make sure your installation of the ''rsconnect'' package is correct for the server you''re connecting to.\n\n{0}\n{1}"
     */
    @DefaultMessage("Your account was authenticated successfully, but could not be connected to RStudio. Make sure your installation of the ''rsconnect'' package is correct for the server you''re connecting to.\\n\\n{0}\\n{1}")
    String accountConnectFailedMessage(String serverInfo, String errorMessage);

    /**
     * Translated "Account Connect Failed".
     *
     * @return translated "Account Connect Failed"
     */
    @DefaultMessage("Account Connect Failed")
    String accountConnectFailed();

    /**
     * Translated "Adding account...".
     *
     * @return translated "Adding account..."
     */
    @DefaultMessage("Adding account...")
    String addingAccount();

    /**
     * Translated "The command ''{0}'' failed. You can set up an account manually by using rsconnect::setAccountInfo; type ?rsconnect::setAccountInfo at the R console for more information.".
     *
     * @return translated "The command ''{0}'' failed. You can set up an account manually by using rsconnect::setAccountInfo; type ?rsconnect::setAccountInfo at the R console for more information."
     */
    @DefaultMessage("The command ''{0}'' failed. You can set up an account manually by using rsconnect::setAccountInfo; type ?rsconnect::setAccountInfo at the R console for more information.")
    String errorAccountMessage(String cmdString);

    /**
     * Translated "Connecting account...".
     *
     * @return translated "Connecting account..."
     */
    @DefaultMessage("Connecting account...")
    String connectingAccount();

    /**
     * Translated "The pasted command should start with rsconnect::setAccountInfo. If you''re having trouble, try connecting your account manually; type ?rsconnect::setAccountInfo at the R console for help.".
     *
     * @return translated "The pasted command should start with rsconnect::setAccountInfo. If you''re having trouble, try connecting your account manually; type ?rsconnect::setAccountInfo at the R console for help."
     */
    @DefaultMessage("The pasted command should start with rsconnect::setAccountInfo. If you''re having trouble, try connecting your account manually; type ?rsconnect::setAccountInfo at the R console for help.")
    String errorAccountMessageSetInfo();

    /**
     * Translated "RStudio could not retrieve server information.".
     *
     * @return translated "RStudio could not retrieve server information."
     */
    @DefaultMessage("RStudio could not retrieve server information.")
    String rStudioCouldNotRetrieveServerInfo();

    /**
     * Translated "Can''t Find Servers".
     *
     * @return translated "Can''t Find Servers"
     */
    @DefaultMessage("Can''t Find Servers")
    String cantFindServers();

    /**
     * Translated "RStudio could not retrieve server information for the selected account.".
     *
     * @return translated "RStudio could not retrieve server information for the selected account."
     */
    @DefaultMessage("RStudio could not retrieve server information for the selected account.")
    String rStudioCouldNotRetrieveForAccount();

    /**
     * Translated "Server Information Not Found".
     *
     * @return translated "Server Information Not Found"
     */
    @DefaultMessage("Server Information Not Found")
    String serverInformationNotFound();

    /**
     * Translated "Error retrieving accounts".
     *
     * @return translated "Error retrieving accounts"
     */
    @DefaultMessage("Error retrieving accounts")
    String errorRetrievingAccounts();

    /**
     * Translated "No accounts connected.".
     *
     * @return translated "No accounts connected."
     */
    @DefaultMessage("No accounts connected.")
    String noAccountsConnected();

    /**
     * Translated "Posit Connect is a server product from Posit for secure sharing of applications, reports, plots, and APIs.".
     *
     * @return translated "Posit Connect is a server product from Posit for secure sharing of applications, reports, plots, and APIs."
     */
    @DefaultMessage("Posit Connect is a server product from Posit for secure sharing of applications, reports, plots, and APIs.")
    String rStudioConnectServiceDescription();

    /**
     * Translated "Posit Connect".
     *
     * @return translated "Posit Connect"
     */
    @DefaultMessage("Posit Connect")
    String rStudioConnect();

    /**
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    @DefaultMessage("Connect Account")
    String connectAccount();

    /**
     * Translated "Choose Account Type".
     *
     * @return translated "Choose Account Type"
     */
    @DefaultMessage("Choose Account Type")
    String chooseAccountType();

    /**
     * Translated "Connect Publishing Account".
     *
     * @return translated "Connect Publishing Account"
     */
    @DefaultMessage("Connect Publishing Account")
    String connectPublishingAccount();

    /**
     * Translated "Pick an account".
     *
     * @return translated "Pick an account"
     */
    @DefaultMessage("Pick an account")
    String pickAnAccount();

    /**
     * Translated "Confirm account on {0}".
     *
     * @return translated "Confirm account on {0}"
     */
    @DefaultMessage("Confirm account on {0}")
    String confirmAccountOn(String serverName);

    /**
     * Translated "Connecting your ShinyApps Account".
     *
     * @return translated "Connecting your ShinyApps Account"
     */
    @DefaultMessage("Connecting your ShinyApps Account")
    String connectingShinyAppsAccount();

    /**
     * Translated "Uncheck All".
     *
     * @return translated "Uncheck All"
     */
    @DefaultMessage("Uncheck All")
    String uncheckAll();

    /**
     * Translated "Check All".
     *
     * @return translated "Check All"
     */
    @DefaultMessage("Check All")
    String checkAll();

    /**
     * Translated "Replace".
     *
     * @return translated "Replace"
     */
    @DefaultMessage("Replace")
    String replace();

    /**
     * Translated "You''ve already published an application named ''{0}'' to {1} ({2}). Do you want to replace the existing application with this content?".
     *
     * @return translated "You''ve already published an application named ''{0}'' to {1} ({2}). Do you want to replace the existing application with this content?"
     */
    @DefaultMessage("You''ve already published an application named ''{0}'' to {1} ({2}). Do you want to replace the existing application with this content?")
    String checkForExistingAppMessage(String appName, String server, String url);

    /**
     * Translated "Overwrite {0}?".
     *
     * @return translated "Overwrite {0}?"
     */
    @DefaultMessage("Overwrite {0}?")
    String overwriteAppName(String appName);

    /**
     * Translated "index".
     *
     * @return translated "index"
     */
    @DefaultMessage("index")
    String index();

    /**
     * Translated "Only files in the same folder as the document ({0}) or one of its sub-folders may be added.".
     *
     * @return translated "Only files in the same folder as the document ({0}) or one of its sub-folders may be added."
     */
    @DefaultMessage("Only files in the same folder as the document ({0}) or one of its sub-folders may be added.")
    String onAddFileClickMessage(String dirPath);

    /**
     * Translated "Cannot Add File".
     *
     * @return translated "Cannot Add File"
     */
    @DefaultMessage("Cannot Add File")
    String cannotAddFile();

    /**
     * Translated "Select File".
     *
     * @return translated "Select File"
     */
    @DefaultMessage("Select File")
    String selectFile();

    /**
     * Translated "Could not find files to deploy: \n\n{0}".
     *
     * @return translated "Could not find files to deploy: \n\n{0}"
     */
    @DefaultMessage("Could not find files to deploy: \n\n{0}")
    String couldNotFindFilesToDeploy(String errorMessage);

    /**
     * Translated "Could not determine the list of files to deploy.".
     *
     * @return translated "Could not determine the list of files to deploy."
     */
    @DefaultMessage("Could not determine the list of files to deploy.")
    String couldNotDetermineListToDeploy();

    /**
     * Translated "The item to be deployed ({0}) exceeds the maximum deployment size, which is {1}. Consider creating a new directory containing only the content you wish to deploy.".
     *
     * @return translated "The item to be deployed ({0}) exceeds the maximum deployment size, which is {1}. Consider creating a new directory containing only the content you wish to deploy."
     */
    @DefaultMessage("The item to be deployed ({0}) exceeds the maximum deployment size, which is {1}. Consider creating a new directory containing only the content you wish to deploy.")
    String itemExceedsDeploymentSize(String fileSource, String maxSize);

    /**
     * Translated "Collecting files...".
     *
     * @return translated "Collecting files..."
     */
    @DefaultMessage("Collecting files...")
    String collectingFiles();

    /**
     * Translated "Could not determine the list of files to deploy. Try re-rendering and ensuring that you''re publishing to a server which supports this kind of content.".
     *
     * @return translated "Could not determine the list of files to deploy. Try re-rendering and ensuring that you''re publishing to a server which supports this kind of content."
     */
    @DefaultMessage("Could not determine the list of files to deploy. Try re-rendering and ensuring that you''re publishing to a server which supports this kind of content.")
    String couldNotDetermineListToDeployReRender();

    /**
     * Translated "To publish finished document, you must first render it. Dismiss this message, click Knit to render the document, then try publishing again.".
     *
     * @return translated "To publish finished document, you must first render it. Dismiss this message, click Knit to render the document, then try publishing again."
     */
    @DefaultMessage("To publish finished document, you must first render it. Dismiss this message, click Knit to render the document, then try publishing again.")
    String finishedDocumentNotFoundMessage();

    /**
     * Translated "Finished Document Not Found".
     *
     * @return translated "Finished Document Not Found"
     */
    @DefaultMessage("Finished Document Not Found")
    String finishedDocumentNotFound();

    /**
     * Translated "Error retrieving accounts:\n\n{0}".
     *
     * @return translated "Error retrieving accounts:\n\n{0}"
     */
    @DefaultMessage("Error retrieving accounts:\n\n{0}")
    String errorRetrievingAccountsWithMessage(String errorMessage);

    /**
     * Translated "Error retrieving application {0}.".
     *
     * @return translated "Error retrieving application {0}."
     */
    @DefaultMessage("Error retrieving application {0}.")
    String errorRetrievingApplicationAppId(String appId);

    /**
     * Translated "Publish from Account".
     *
     * @return translated "Publish from Account"
     */
    @DefaultMessage("Publish from Account")
    String publishFromAccount();

    /**
     * Translated "To publish this content to a new location, click the Publish drop-down menu and choose Other Destination.".
     *
     * @return translated "To publish this content to a new location, click the Publish drop-down menu and choose Other Destination."
     */
    @DefaultMessage("To publish this content to a new location, click the Publish drop-down menu and choose Other Destination.")
    String createNewAccountMessage();

    /**
     * Translated "Create New Content".
     *
     * @return translated "Create New Content"
     */
    @DefaultMessage("Create New Content")
    String createNewAccount();

    /**
     * Translated "Deploying...".
     *
     * @return translated "Deploying..."
     */
    @DefaultMessage("Deploying...")
    String deploying();

    /**
     * Translated "Launch browser".
     *
     * @return translated "Launch browser"
     */
    @DefaultMessage("Launch browser")
    String launchBrowser();

    /**
     * Translated "Publish to Server".
     *
     * @return translated "Publish to Server"
     */
    @DefaultMessage("Publish to Server")
    String publishToServer();

    /**
     * Translated "Unable to determine file to be published. Click Knit or Preview to render it again, then click the Publish button above the rendered document.".
     *
     * @return translated "Unable to determine file to be published. Click Knit or Preview to render it again, then click the Publish button above the rendered document."
     */
    @DefaultMessage("Unable to determine file to be published. Click Knit or Preview to render it again, then click the Publish button above the rendered document.")
    String contentPublishFailedMessage();

    /**
     * Translated "Content Publish Failed".
     *
     * @return translated "Content Publish Failed"
     */
    @DefaultMessage("Content Publish Failed")
    String contentPublishFailed();

    /**
     * Translated "Local deployment history for {0} successfully removed.".
     *
     * @return translated "Local deployment history for {0} successfully removed."
     */
    @DefaultMessage("Local deployment history for {0} successfully removed.")
    String clearListMessage(String appLabel);

    /**
     * Translated "Clear List".
     *
     * @return translated "Clear List"
     */
    @DefaultMessage("Clear List")
    String clearList();

    /**
     * Translated "this application".
     *
     * @return translated "this application"
     */
    @DefaultMessage("this application")
    String thisApplication();

    /**
     * Translated "Publish {0}...".
     *
     * @return translated "Publish {0}..."
     */
    @DefaultMessage("Publish {0}...")
    String publishContent(String contentDesc);

    /**
     * Translated "Other Destination...".
     *
     * @return translated "Other Destination..."
     */
    @DefaultMessage("Other Destination...")
    String otherDestination();

    /**
     * Translated "Are you sure you want to remove all local deployment history for {0}?".
     *
     * @return translated "Are you sure you want to remove all local deployment history for {0}?"
     */
    @DefaultMessage("Are you sure you want to remove all local deployment history for {0}?")
    String removeLocalDeploymentMessage(String appLabel);

    /**
     * Translated "Republish".
     *
     * @return translated "Republish"
     */
    @DefaultMessage("Republish")
    String republish();

    /**
     * Translated "The content type ''{0}'' is not currently supported for publishing.".
     *
     * @return translated "The content type ''{0}'' is not currently supported for publishing."
     */
    @DefaultMessage("The content type ''{0}'' is not currently supported for publishing.")
    String contentNotSupportedForPublishing(String contentTypeDesc);

    /**
     * Translated "Can''t publish {0}".
     *
     * @return translated "Can''t publish {0}"
     */
    @DefaultMessage("Can''t publish {0}")
    String cantPublishContent(String contentTypeDesc);

    /**
     * Translated "Unsaved documents cannot be published. Save the document before publishing it.".
     *
     * @return translated "Unsaved documents cannot be published. Save the document before publishing it."
     */
    @DefaultMessage("Unsaved documents cannot be published. Save the document before publishing it.")
    String unsavedDocumentPublishMessage();

    /**
     * Translated "Unsaved Document".
     *
     * @return translated "Unsaved Document"
     */
    @DefaultMessage("Unsaved Document")
    String unsavedDocument();

    /**
     * Translated "No HTML could be generated for the content.".
     *
     * @return translated "No HTML could be generated for the content."
     */
    @DefaultMessage("No HTML could be generated for the content.")
    String noHTMLGenerated();

    /**
     * Translated "This copy of the content has been published to the server ''{0}'', but you currently do not have any accounts registered on that server. \n\nConnect an account on the server ''{0}'' to update the application, or publish the content to a different server.".
     *
     * @return translated "This copy of the content has been published to the server ''{0}'', but you currently do not have any accounts registered on that server. \n\nConnect an account on the server ''{0}'' to update the application, or publish the content to a different server."
     */
    @DefaultMessage("This copy of the content has been published to the server ''{0}'', but you currently do not have any accounts registered on that server. \n\nConnect an account on the server ''{0}'' to update the application, or publish the content to a different server.")
    String hostNotRegisteredMessage(String host);

    /**
     * Translated "{0} Not Registered".
     *
     * @return translated "{0} Not Registered"
     */
    @DefaultMessage("{0} Not Registered")
    String hostNotRegistered(String host);

    /**
     * Translated "Publish options".
     *
     * @return translated "Publish options"
     */
    @DefaultMessage("Publish options")
    String publishOptions();

    /**
     * Translated "Reconnect Account".
     *
     * @return translated "Reconnect Account"
     */
    @DefaultMessage("Reconnect Account")
    String reconnectAccount();

    /**
     * Translated "Publishing content".
     *
     * @return translated "Publishing content"
     */
    @DefaultMessage("Publishing content")
    String publishingContentLabel();

    /**
     * Translated "Publish Wizard".
     *
     * @return translated "Publish Wizard"
     */
    @DefaultMessage("Publish Wizard")
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

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
    @Key("couldNotPublish")
    String couldNotPublish();

    /**
     * Translated "Preparing for Publish...".
     *
     * @return translated "Preparing for Publish..."
     */
    @DefaultMessage("Preparing for Publish...")
    @Key("preparingForPublish")
    String preparingForPublish();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("error")
    String error();

    /**
     * Translated "No Running Deployments Found".
     *
     * @return translated "No Running Deployments Found"
     */
    @DefaultMessage("No Running Deployments Found")
    @Key("noRunningDeploymentsFound")
    String noRunningDeploymentsFound();

    /**
     * Translated "No applications deployed from ''{0}'' appear to be running.".
     *
     * @return translated "No applications deployed from ''{0}'' appear to be running."
     */
    @DefaultMessage("No applications deployed from ''{0}'' appear to be running.")
    @Key("noApplicationDeploymentsFrom")
    String noApplicationDeploymentsFrom(String dir);

    /**
     * Translated "Error Listing Applications".
     *
     * @return translated "Error Listing Applications"
     */
    @DefaultMessage("Error Listing Applications")
    @Key("errorListingApplications")
    String errorListingApplications();

    /**
     * Translated "No application deployments were found for ''{0}''".
     *
     * @return translated "No application deployments were found for ''{0}''"
     */
    @DefaultMessage("No application deployments were found for ''{0}''")
    @Key("noApplicationDeploymentsFound")
    String noApplicationDeploymentsFound(String dir);

    /**
     * Translated "No Deployments Found".
     *
     * @return translated "No Deployments Found"
     */
    @DefaultMessage("No Deployments Found")
    @Key("noDeploymentsFound")
    String noDeploymentsFound();

    /**
     * Translated "Could not determine application deployments for ''{0}'':{1}".
     *
     * @return translated "Could not determine application deployments for ''{0}'':{1}"
     */
    @DefaultMessage("Could not determine application deployments for ''{0}'':{1}")
    @Key("couldNotDetermineAppDeployments")
    String couldNotDetermineAppDeployments(String dir, String message);

    /**
     * Translated "Error Configuring Application".
     *
     * @return translated "Error Configuring Application"
     */
    @DefaultMessage("Error Configuring Application")
    @Key("errorConfiguringApplication")
    String errorConfiguringApplication();

    /**
     * Translated "Error Deploying Application".
     *
     * @return translated "Error Deploying Application"
     */
    @DefaultMessage("Error Deploying Application")
    @Key("errorDeployingApplication")
    String errorDeployingApplication();

    /**
     * Translated "Could not deploy application ''{0}'': {1}".
     *
     * @return translated "Could not deploy application ''{0}'': {1}"
     */
    @DefaultMessage("Could not deploy application ''{0}'': {1}")
    @Key("couldNotDeployApplication")
    String couldNotDeployApplication(String recordName, String message);

    /**
     * Translated "Another deployment is currently in progress; only one deployment can be performed at a time.".
     *
     * @return translated "Another deployment is currently in progress; only one deployment can be performed at a time."
     */
    @DefaultMessage("Another deployment is currently in progress; only one deployment can be performed at a time.")
    @Key("onlyOneDeploymentAtATime")
    String onlyOneDeploymentAtATime();

    /**
     * Translated "Deployment In Progress".
     *
     * @return translated "Deployment In Progress"
     */
    @DefaultMessage("Deployment In Progress")
    @Key("deploymentInProgress")
    String deploymentInProgress();

    /**
     * Translated "Uploading {0}".
     *
     * @return translated "Uploading {0}"
     */
    @DefaultMessage("Uploading {0}")
    @Key("uploadingContent")
    String uploadingContent(String contentType);

    /**
     * Translated "Only rendered documents can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document.".
     *
     * @return translated "Only rendered documents can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document."
     */
    @DefaultMessage("Only rendered documents can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document.")
    @Key("uploadToRPubsErrorMessage")
    String uploadToRPubsErrorMessage();

    /**
     * Translated "Publish Document".
     *
     * @return translated "Publish Document"
     */
    @DefaultMessage("Publish Document")
    @Key("publishDocument")
    String publishDocument();

    /**
     * Translated "Only documents rendered to HTML can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document.".
     *
     * @return translated "Only documents rendered to HTML can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document."
     */
    @DefaultMessage("Only documents rendered to HTML can be published to RPubs. To publish this document, click Knit or Preview to render it to HTML, then click the Publish button above the rendered document.")
    @Key("showUnsupportedRPubsFormatMessageError")
    String showUnsupportedRPubsFormatMessageError();

    /**
     * Translated "Unsupported Document Format".
     *
     * @return translated "Unsupported Document Format"
     */
    @DefaultMessage("Unsupported Document Format")
    @Key("unsupportedDocumentFormat")
    String unsupportedDocumentFormat();

    /**
     * Translated "RStudio is deploying {0}. Check the Deploy console tab in the main window for status updates. ".
     *
     * @return translated "RStudio is deploying {0}. Check the Deploy console tab in the main window for status updates. "
     */
    @DefaultMessage("RStudio is deploying {0}. Check the Deploy console tab in the main window for status updates. ")
    @Key("rstudioDeployingApp")
    String rstudioDeployingApp(String appName);

    /**
     * Translated "Deployment Started".
     *
     * @return translated "Deployment Started"
     */
    @DefaultMessage("Deployment Started")
    @Key("deploymentStarted")
    String deploymentStarted();

    /**
     * Translated "Content".
     *
     * @return translated "Content"
     */
    @DefaultMessage("Content")
    @Key("content")
    String content();

    /**
     * Translated "Quarto Website".
     *
     * @return translated "Quarto Website"
     */
    @DefaultMessage("Quarto Website")
    @Key("quartoWebsite")
    String quartoWebsite();

    /**
     * Translated "API".
     *
     * @return translated "API"
     */
    @DefaultMessage("API")
    @Key("api")
    String api();

    /**
     * Translated "Website".
     *
     * @return translated "Website"
     */
    @DefaultMessage("Website")
    @Key("website")
    String website();

    /**
     * Translated "Presentation".
     *
     * @return translated "Presentation"
     */
    @DefaultMessage("Presentation")
    @Key("presentation")
    String presentation();

    /**
     * Translated "Document".
     *
     * @return translated "Document"
     */
    @DefaultMessage("Document")
    @Key("document")
    String document();

    /**
     * Translated "HTML".
     *
     * @return translated "HTML"
     */
    @DefaultMessage("HTML")
    @Key("html")
    String html();

    /**
     * Translated "Plot".
     *
     * @return translated "Plot"
     */
    @DefaultMessage("Plot")
    @Key("plot")
    String plot();

    /**
     * Translated "Application".
     *
     * @return translated "Application"
     */
    @DefaultMessage("Application")
    @Key("application")
    String application();

    /**
     * Translated "<p>{0}</p><p>{1}</p><p><a href=\"{2}\">{2}</a></p><p>{3}</p><p><small>{4}{5}</small></p>".
     *
     * @return translated "<p>{0}</p><p>{1}</p><p><a href=\"{2}\">{2}</a></p><p>{3}</p><p><small>{4}{5}</small></p>"
     */
    @DefaultMessage("<p>{0}</p><p>{1}</p><p><a href=\"{2}\">{2}</a></p><p>{3}</p><p><small>{4}{5}</small></p>")
    @Key("onRSConnectDeploymentFailedHtml")
    String onRSConnectDeploymentFailedHtml(String p1, String p2, String serverUrl, String p3, String p4, String errorCode);

    /**
     * Translated "Your content could not be published because of a problem on the server.".
     *
     * @return translated "Your content could not be published because of a problem on the server."
     */
    @DefaultMessage("Your content could not be published because of a problem on the server.")
    @Key("onRSConnectDeploymentFailedHtmlP1")
    String onRSConnectDeploymentFailedHtmlP1();

    /**
     * Translated "More information may be available on the server''s home page:".
     *
     * @return translated "More information may be available on the server''s home page:"
     */
    @DefaultMessage("More information may be available on the server''s home page:")
    @Key("onRSConnectDeploymentFailedHtmlP2")
    String onRSConnectDeploymentFailedHtmlP2();

    /**
     * Translated "If the error persists, contact the server''s administrator.".
     *
     * @return translated "If the error persists, contact the server''s administrator."
     */
    @DefaultMessage("If the error persists, contact the server''s administrator.")
    @Key("onRSConnectDeploymentFailedHtmlP3")
    String onRSConnectDeploymentFailedHtmlP3();

    /**
     * Translated "Error code: ".
     *
     * @return translated "Error code: "
     */
    @DefaultMessage("Error code: ")
    @Key("onRSConnectDeploymentFailedHtmlP4")
    String onRSConnectDeploymentFailedHtmlP4();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    @Key("okCapitalized")
    String okCapitalized();

    /**
     * Translated "Publish Failed".
     *
     * @return translated "Publish Failed"
     */
    @DefaultMessage("Publish Failed")
    @Key("publishFailed")
    String publishFailed();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("cancel")
    String cancel();

    /**
     * Translated "Stop deployment".
     *
     * @return translated "Stop deployment"
     */
    @DefaultMessage("Stop deployment")
    @Key("stopDeployment")
    String stopDeployment();

    /**
     * Translated "The deployment could not be cancelled; it is not running, or termination failed.".
     *
     * @return translated "The deployment could not be cancelled; it is not running, or termination failed."
     */
    @DefaultMessage("The deployment could not be cancelled; it is not running, or termination failed.")
    @Key("deploymentNotCancelledMessage")
    String deploymentNotCancelledMessage();

    /**
     * Translated "Could not cancel deployment".
     *
     * @return translated "Could not cancel deployment"
     */
    @DefaultMessage("Could not cancel deployment")
    @Key("couldNotCancelDeployment")
    String couldNotCancelDeployment();

    /**
     * Translated "Error Stopping Deployment".
     *
     * @return translated "Error Stopping Deployment"
     */
    @DefaultMessage("Error Stopping Deployment")
    @Key("errorStoppingDeployment")
    String errorStoppingDeployment();

    /**
     * Translated "Do you want to stop the deployment process? If the server has already received the content, it will still be published.".
     *
     * @return translated "Do you want to stop the deployment process? If the server has already received the content, it will still be published."
     */
    @DefaultMessage("Do you want to stop the deployment process? If the server has already received the content, it will still be published.")
    @Key("onRSConnectDeploymentCancelledMessage")
    String onRSConnectDeploymentCancelledMessage();

    /**
     * Translated "Stop deployment?".
     *
     * @return translated "Stop deployment?"
     */
    @DefaultMessage("Stop deployment?")
    @Key("stopDeploymentQuestion")
    String stopDeploymentQuestion();

    /**
     * Translated "Publish Anyway".
     *
     * @return translated "Publish Anyway"
     */
    @DefaultMessage("Publish Anyway")
    @Key("publishAnyway")
    String publishAnyway();

    /**
     * Translated "Review Issues".
     *
     * @return translated "Review Issues"
     */
    @DefaultMessage("Review Issues")
    @Key("reviewIssues")
    String reviewIssues();

    /**
     * Translated "Some issues were found in your content, which may prevent it from working correctly after publishing. Do you want to review these issues or publish anyway?".
     *
     * @return translated "Some issues were found in your content, which may prevent it from working correctly after publishing. Do you want to review these issues or publish anyway?"
     */
    @DefaultMessage("Some issues were found in your content, which may prevent it from working correctly after publishing. Do you want to review these issues or publish anyway?")
    @Key("publishContentIssuesMessage")
    String publishContentIssuesMessage();

    /**
     * Translated "Publish Content Issues Found".
     *
     * @return translated "Publish Content Issues Found"
     */
    @DefaultMessage("Publish Content Issues Found")
    @Key("publishContentIssuesFound")
    String publishContentIssuesFound();

    /**
     * Translated "The content you tried to publish could not be checked for errors. Do you want to proceed? \n\n{0}".
     *
     * @return translated "The content you tried to publish could not be checked for errors. Do you want to proceed? \n\n{0}"
     */
    @DefaultMessage("The content you tried to publish could not be checked for errors. Do you want to proceed? \n\n{0}")
    @Key("lintFailedMessage")
    String lintFailedMessage(String errorMessage);

    /**
     * Translated "Lint Failed".
     *
     * @return translated "Lint Failed"
     */
    @DefaultMessage("Lint Failed")
    @Key("lintFailed")
    String lintFailed();

    /**
     * Translated "Publishing to RStudio Connect is disabled in the Publishing options.".
     *
     * @return translated "Publishing to RStudio Connect is disabled in the Publishing options."
     */
    @DefaultMessage("Publishing to RStudio Connect is disabled in the Publishing options.")
    @Key("apiNotPublishableMessage")
    String apiNotPublishableMessage();

    /**
     * Translated "API Not Publishable".
     *
     * @return translated "API Not Publishable"
     */
    @DefaultMessage("API Not Publishable")
    @Key("apiNotPublishable")
    String apiNotPublishable();

    /**
     * Translated "Only self-contained documents can currently be published to RPubs.".
     *
     * @return translated "Only self-contained documents can currently be published to RPubs."
     */
    @DefaultMessage("Only self-contained documents can currently be published to RPubs.")
    @Key("contentNotPublishableMessage")
    String contentNotPublishableMessage();

    /**
     * Translated "Content Not Publishable".
     *
     * @return translated "Content Not Publishable"
     */
    @DefaultMessage("Content Not Publishable")
    @Key("contentNotPublishable")
    String contentNotPublishable();

    /**
     * Translated "Publish {0}".
     *
     * @return translated "Publish {0}"
     */
    @DefaultMessage("Publish {0}")
    @Key("publishRpubTitle")
    String publishRpubTitle(String contentTypeDesc);

    /**
     * Translated "Only rendered documents can be republished to RPubs. To republish this document, click Knit or Preview to render it to HTML, then click the Republish button above the rendered document.".
     *
     * @return translated "Only rendered documents can be republished to RPubs. To republish this document, click Knit or Preview to render it to HTML, then click the Republish button above the rendered document."
     */
    @DefaultMessage("Only rendered documents can be republished to RPubs. To republish this document, click Knit or Preview to render it to HTML, then click the Republish button above the rendered document.")
    @Key("republishDocumentMessage")
    String republishDocumentMessage();

    /**
     * Translated "Republish Document".
     *
     * @return translated "Republish Document"
     */
    @DefaultMessage("Republish Document")
    @Key("republishDocument")
    String republishDocument();

    /**
     * Translated "Current Plot".
     *
     * @return translated "Current Plot"
     */
    @DefaultMessage("Current Plot")
    @Key("currentPlot")
    String currentPlot();

    /**
     * Translated "Server: {0} ({1})\nVersion: {2}\nAbout: {3}\n".
     *
     * @return translated "Server: {0} ({1})\nVersion: {2}\nAbout: {3}\n"
     */
    @DefaultMessage("Server: {0} ({1})\nVersion: {2}\nAbout: {3}\n")
    @Key("rsConnectServerInfoString")
    String rsConnectServerInfoString(String name, String url, String version, String about);

    /**
     * Translated "The title must contain at least 3 characters.".
     *
     * @return translated "The title must contain at least 3 characters."
     */
    @DefaultMessage("The title must contain at least 3 characters.")
    @Key("titleMinimumCharacter")
    String titleMinimumCharacter();

    /**
     * Translated "The title must contain 4 - 64 alphanumeric characters.".
     *
     * @return translated "The title must contain 4 - 64 alphanumeric characters."
     */
    @DefaultMessage("The title must contain 4 - 64 alphanumeric characters.")
    @Key("titleContainAlphanumeric")
    String titleContainAlphanumeric();

    /**
     * Translated "The server appears to be valid, but rejected the request to authorize an account.\n\n{0}\n{1}".
     *
     * @return translated "The server appears to be valid, but rejected the request to authorize an account.\n\n{0}\n{1}"
     */
    @DefaultMessage("The server appears to be valid, but rejected the request to authorize an account.\n\n{0}\n{1}")
    @Key("errorConnectingAccountMessage")
    String errorConnectingAccountMessage(String infoString, String errorMessage);

    /**
     * Translated "Error Connecting Account".
     *
     * @return translated "Error Connecting Account"
     */
    @DefaultMessage("Error Connecting Account")
    @Key("errorConnectingAccount")
    String errorConnectingAccount();

    /**
     * Translated "Setting up an account...".
     *
     * @return translated "Setting up an account..."
     */
    @DefaultMessage("Setting up an account...")
    @Key("settingUpAccount")
    String settingUpAccount();

    /**
     * Translated "RStudio failed to determine whether the account was valid. Try again; if the error persists, contact your server administrator.\n\n{0}\n{1}".
     *
     * @return translated "RStudio failed to determine whether the account was valid. Try again; if the error persists, contact your server administrator.\n\n{0}\n{1}"
     */
    @DefaultMessage("RStudio failed to determine whether the account was valid. Try again; if the error persists, contact your server administrator.\n\n{0}\n{1}")
    @Key("accountValidationFailedMessage")
    String accountValidationFailedMessage(String serverInfo, String errorMessage);

    /**
     * Translated "Account Validation Failed".
     *
     * @return translated "Account Validation Failed"
     */
    @DefaultMessage("Account Validation Failed")
    @Key("accountValidationFailed")
    String accountValidationFailed();

    /**
     * Translated "Authentication failed. If you did not cancel authentication, try again, or contact your server administrator for assistance.".
     *
     * @return translated "Authentication failed. If you did not cancel authentication, try again, or contact your server administrator for assistance."
     */
    @DefaultMessage("Authentication failed. If you did not cancel authentication, try again, or contact your server administrator for assistance.")
    @Key("accountNotConnectedMessage")
    String accountNotConnectedMessage();

    /**
     * Translated "Account Not Connected".
     *
     * @return translated "Account Not Connected"
     */
    @DefaultMessage("Account Not Connected")
    @Key("accountNotConnected")
    String accountNotConnected();

    /**
     * Translated "The server couldn''t be validated. {0}".
     *
     * @return translated "The server couldn''t be validated. {0}"
     */
    @DefaultMessage("The server couldn''t be validated. {0}")
    @Key("serverCouldntBeValidated")
    String serverCouldntBeValidated(String errorMessage);

    /**
     * Translated "The URL ''{0}'' does not appear to belong to a valid server. Please double check the URL, and contact your administrator if the problem persists.\n\n{1}".
     *
     * @return translated "The URL ''{0}'' does not appear to belong to a valid server. Please double check the URL, and contact your administrator if the problem persists.\n\n{1}"
     */
    @DefaultMessage("The URL ''{0}'' does not appear to belong to a valid server. Please double check the URL, and contact your administrator if the problem persists.\n\n{1}")
    @Key("serverValidationFailedMessage")
    String serverValidationFailedMessage(String serverUrl, String infoMessage);

    /**
     * Translated "Server Validation Failed".
     *
     * @return translated "Server Validation Failed"
     */
    @DefaultMessage("Server Validation Failed")
    @Key("serverValidationFailed")
    String serverValidationFailed();

    /**
     * Translated "Checking server connection...".
     *
     * @return translated "Checking server connection..."
     */
    @DefaultMessage("Checking server connection...")
    @Key("checkingServerConnection")
    String checkingServerConnection();

    /**
     * Translated "Verifying Account".
     *
     * @return translated "Verifying Account"
     */
    @DefaultMessage("Verifying Account")
    @Key("verifyingAccount")
    String verifyingAccount();

    /**
     * Translated "Connect ShinyApps.io Account".
     *
     * @return translated "Connect ShinyApps.io Account"
     */
    @DefaultMessage("Connect ShinyApps.io Account")
    @Key("newRSConnectCloudPageCaption")
    String newRSConnectCloudPageCaption();

    /**
     * Translated "A cloud service run by RStudio. Publish Shiny applications and interactive documents to the Internet.".
     *
     * @return translated "A cloud service run by RStudio. Publish Shiny applications and interactive documents to the Internet."
     */
    @DefaultMessage("A cloud service run by RStudio. Publish Shiny applications and interactive documents to the Internet.")
    @Key("newRSConnectCloudPageSubTitle")
    String newRSConnectCloudPageSubTitle();

    /**
     * Translated "RStudio Connect Account".
     *
     * @return translated "RStudio Connect Account"
     */
    @DefaultMessage("RStudio Connect Account")
    @Key("rstudioConnectAccount")
    String rstudioConnectAccount();

    /**
     * Translated "RPubs is a free service from RStudio for sharing documents on the web.".
     *
     * @return translated "RPubs is a free service from RStudio for sharing documents on the web."
     */
    @DefaultMessage("RPubs is a free service from RStudio for sharing documents on the web.")
    @Key("rPubsSubtitle")
    String rPubsSubtitle();

    /**
     * Translated "Posit Cloud is our online service that lets you do, share, teach and learn data science in your web browser.".
     *
     * @return translated "Posit Cloud is our online service that lets you do, share, teach and learn data science in your web browser."
     */
    @DefaultMessage("Posit Cloud is our online service that lets you do, share, teach and learn data science in your web browser.")
    @Key("cloudSubtitle")
    String cloudSubtitle();

    /**
     * Translated "Publish To".
     *
     * @return translated "Publish To"
     */
    @DefaultMessage("Publish To")
    @Key("publishTo")
    String publishTo();

    /**
     * Translated "Publish".
     *
     * @return translated "Publish"
     */
    @DefaultMessage("Publish")
    @Key("publish")
    String publish();

    /**
     * Translated "All of the documents in the directory {0} will be published.".
     *
     * @return translated "All of the documents in the directory {0} will be published."
     */
    @DefaultMessage("All of the documents in the directory {0} will be published.")
    @Key("publishMultiplePageSubtitle")
    String publishMultiplePageSubtitle(String directoryString);

    /**
     * Translated "Publish all documents in the directory".
     *
     * @return translated "Publish all documents in the directory"
     */
    @DefaultMessage("Publish all documents in the directory")
    @Key("publishMultiplePageTitle")
    String publishMultiplePageTitle();

    /**
     * Translated "Only the document {0} will be published.".
     *
     * @return translated "Only the document {0} will be published."
     */
    @DefaultMessage("Only the document {0} will be published.")
    @Key("publishMultiplePagSingleSubtitle")
    String publishMultiplePagSingleSubtitle(String name);

    /**
     * Translated "Publish just this document".
     *
     * @return translated "Publish just this document"
     */
    @DefaultMessage("Publish just this document")
    @Key("publishMultiplePagSingleTitle")
    String publishMultiplePagSingleTitle();

    /**
     * Translated "What do you want to publish?".
     *
     * @return translated "What do you want to publish?"
     */
    @DefaultMessage("What do you want to publish?")
    @Key("publishMultiplePageCaption")
    String publishMultiplePageCaption();

    /**
     * Translated "Choose this option to publish the content as it appears in RStudio.".
     *
     * @return translated "Choose this option to publish the content as it appears in RStudio."
     */
    @DefaultMessage("Choose this option to publish the content as it appears in RStudio.")
    @Key("publishReportSourcePageStaticSubtitle")
    String publishReportSourcePageStaticSubtitle();

    /**
     * Translated "Publish finished {0} only".
     *
     * @return translated "Publish finished {0} only"
     */
    @DefaultMessage("Publish finished {0} only")
    @Key("publishReportSourcePageStaticTitle")
    String publishReportSourcePageStaticTitle(String descriptor);

    /**
     * Translated "Choose this option if you want to create {0} or rebuild your {1} on the server.".
     *
     * @return translated "Choose this option if you want to create {0} or rebuild your {1} on the server."
     */
    @DefaultMessage("Choose this option if you want to create {0} or rebuild your {1} on the server.")
    @Key("publishReportSourcePageSubTitle")
    String publishReportSourcePageSubTitle(String scheduledReportNumber, String descriptor);

    /**
     * Translated "scheduled reports".
     *
     * @return translated "scheduled reports"
     */
    @DefaultMessage("scheduled reports")
    @Key("scheduledReportsPlural")
    String scheduledReportsPlural();

    /**
     * Translated "a scheduled report".
     *
     * @return translated "a scheduled report"
     */
    @DefaultMessage("a scheduled report")
    @Key("scheduledReportsSingular")
    String scheduledReportsSingular();

    /**
     * Translated "Publish {0} with source code".
     *
     * @return translated "Publish {0} with source code"
     */
    @DefaultMessage("Publish {0} with source code")
    @Key("publishFilesPageTitle")
    String publishFilesPageTitle(String descriptor);

    /**
     * Translated "website".
     *
     * @return translated "website"
     */
    @DefaultMessage("website")
    @Key("websiteLowercase")
    String websiteLowercase();

    /**
     * Translated "documents".
     *
     * @return translated "documents"
     */
    @DefaultMessage("documents")
    @Key("documentsLowercasePlural")
    String documentsLowercasePlural();

    /**
     * Translated "document".
     *
     * @return translated "document"
     */
    @DefaultMessage("document")
    @Key("documentLowercase")
    String documentLowercase();

    /**
     * Translated "Publish to RStudio Connect".
     *
     * @return translated "Publish to RStudio Connect"
     */
    @DefaultMessage("Publish to RStudio Connect")
    @Key("publishToRstudioConnect")
    String publishToRstudioConnect();

    /**
     * Translated "Publish to Posit Cloud".
     *
     * @return translated "Publish to Posit Cloud"
     */
    @DefaultMessage("Publish to Posit Cloud")
    @Key("publishToPositCloud")
    String publishToPositCloud();

    /**
     * Translated "Publish to RPubs".
     *
     * @return translated "Publish to RPubs"
     */
    @DefaultMessage("Publish to RPubs")
    @Key("publishToRpubs")
    String publishToRpubs();

    /**
     * Translated "Your account was authenticated successfully, but could not be connected to RStudio. Make sure your installation of the ''rsconnect'' package is correct for the server you''re connecting to.\n\n{0}\n{1}".
     *
     * @return translated "Your account was authenticated successfully, but could not be connected to RStudio. Make sure your installation of the ''rsconnect'' package is correct for the server you''re connecting to.\n\n{0}\n{1}"
     */
    @DefaultMessage("Your account was authenticated successfully, but could not be connected to RStudio. Make sure your installation of the ''rsconnect'' package is correct for the server you''re connecting to.\\n\\n{0}\\n{1}")
    @Key("accountConnectFailedMessage")
    String accountConnectFailedMessage(String serverInfo, String errorMessage);

    /**
     * Translated "Account Connect Failed".
     *
     * @return translated "Account Connect Failed"
     */
    @DefaultMessage("Account Connect Failed")
    @Key("accountConnectFailed")
    String accountConnectFailed();

    /**
     * Translated "Adding account...".
     *
     * @return translated "Adding account..."
     */
    @DefaultMessage("Adding account...")
    @Key("addingAccount")
    String addingAccount();

    /**
     * Translated "The command ''{0}'' failed. You can set up an account manually by using rsconnect::setAccountInfo; type ?rsconnect::setAccountInfo at the R console for more information.".
     *
     * @return translated "The command ''{0}'' failed. You can set up an account manually by using rsconnect::setAccountInfo; type ?rsconnect::setAccountInfo at the R console for more information."
     */
    @DefaultMessage("The command ''{0}'' failed. You can set up an account manually by using rsconnect::setAccountInfo; type ?rsconnect::setAccountInfo at the R console for more information.")
    @Key("errorAccountMessage")
    String errorAccountMessage(String cmdString);

    /**
     * Translated "Connecting account...".
     *
     * @return translated "Connecting account..."
     */
    @DefaultMessage("Connecting account...")
    @Key("connectingAccount")
    String connectingAccount();

    /**
     * Translated "The pasted command should start with rsconnect::setAccountInfo. If you''re having trouble, try connecting your account manually; type ?rsconnect::setAccountInfo at the R console for help.".
     *
     * @return translated "The pasted command should start with rsconnect::setAccountInfo. If you''re having trouble, try connecting your account manually; type ?rsconnect::setAccountInfo at the R console for help."
     */
    @DefaultMessage("The pasted command should start with rsconnect::setAccountInfo. If you''re having trouble, try connecting your account manually; type ?rsconnect::setAccountInfo at the R console for help.")
    @Key("errorAccountMessageSetInfo")
    String errorAccountMessageSetInfo();

    /**
     * Translated "RStudio could not retrieve server information.".
     *
     * @return translated "RStudio could not retrieve server information."
     */
    @DefaultMessage("RStudio could not retrieve server information.")
    @Key("rStudioCouldNotRetrieveServerInfo")
    String rStudioCouldNotRetrieveServerInfo();

    /**
     * Translated "Can''t Find Servers".
     *
     * @return translated "Can''t Find Servers"
     */
    @DefaultMessage("Can''t Find Servers")
    @Key("cantFindServers")
    String cantFindServers();

    /**
     * Translated "RStudio could not retrieve server information for the selected account.".
     *
     * @return translated "RStudio could not retrieve server information for the selected account."
     */
    @DefaultMessage("RStudio could not retrieve server information for the selected account.")
    @Key("rStudioCouldNotRetrieveForAccount")
    String rStudioCouldNotRetrieveForAccount();

    /**
     * Translated "Server Information Not Found".
     *
     * @return translated "Server Information Not Found"
     */
    @DefaultMessage("Server Information Not Found")
    @Key("serverInformationNotFound")
    String serverInformationNotFound();

    /**
     * Translated "Error retrieving accounts".
     *
     * @return translated "Error retrieving accounts"
     */
    @DefaultMessage("Error retrieving accounts")
    @Key("errorRetrievingAccounts")
    String errorRetrievingAccounts();

    /**
     * Translated "No accounts connected.".
     *
     * @return translated "No accounts connected."
     */
    @DefaultMessage("No accounts connected.")
    @Key("noAccountsConnected")
    String noAccountsConnected();

    /**
     * Translated "RStudio Connect is a server product from RStudio for secure sharing of applications, reports, plots, and APIs.".
     *
     * @return translated "RStudio Connect is a server product from RStudio for secure sharing of applications, reports, plots, and APIs."
     */
    @DefaultMessage("RStudio Connect is a server product from RStudio for secure sharing of applications, reports, plots, and APIs.")
    @Key("rStudioConnectServiceDescription")
    String rStudioConnectServiceDescription();

    /**
     * Translated "RStudio Connect".
     *
     * @return translated "RStudio Connect"
     */
    @DefaultMessage("RStudio Connect")
    @Key("rStudioConnect")
    String rStudioConnect();

    /**
     * Translated "Connect Account".
     *
     * @return translated "Connect Account"
     */
    @DefaultMessage("Connect Account")
    @Key("connectAccount")
    String connectAccount();

    /**
     * Translated "Choose Account Type".
     *
     * @return translated "Choose Account Type"
     */
    @DefaultMessage("Choose Account Type")
    @Key("chooseAccountType")
    String chooseAccountType();

    /**
     * Translated "Connect Publishing Account".
     *
     * @return translated "Connect Publishing Account"
     */
    @DefaultMessage("Connect Publishing Account")
    @Key("connectPublishingAccount")
    String connectPublishingAccount();

    /**
     * Translated "Pick an account".
     *
     * @return translated "Pick an account"
     */
    @DefaultMessage("Pick an account")
    @Key("pickAnAccount")
    String pickAnAccount();

    /**
     * Translated "Confirm account on {0}".
     *
     * @return translated "Confirm account on {0}"
     */
    @DefaultMessage("Confirm account on {0}")
    @Key("confirmAccountOn")
    String confirmAccountOn(String serverName);

    /**
     * Translated "Connecting your ShinyApps Account".
     *
     * @return translated "Connecting your ShinyApps Account"
     */
    @DefaultMessage("Connecting your ShinyApps Account")
    @Key("connectingShinyAppsAccount")
    String connectingShinyAppsAccount();

    /**
     * Translated "Uncheck All".
     *
     * @return translated "Uncheck All"
     */
    @DefaultMessage("Uncheck All")
    @Key("uncheckAll")
    String uncheckAll();

    /**
     * Translated "Check All".
     *
     * @return translated "Check All"
     */
    @DefaultMessage("Check All")
    @Key("checkAll")
    String checkAll();

    /**
     * Translated "Replace".
     *
     * @return translated "Replace"
     */
    @DefaultMessage("Replace")
    @Key("replace")
    String replace();

    /**
     * Translated "You''ve already published an application named ''{0}'' to {1} ({2}). Do you want to replace the existing application with this content?".
     *
     * @return translated "You''ve already published an application named ''{0}'' to {1} ({2}). Do you want to replace the existing application with this content?"
     */
    @DefaultMessage("You''ve already published an application named ''{0}'' to {1} ({2}). Do you want to replace the existing application with this content?")
    @Key("checkForExistingAppMessage")
    String checkForExistingAppMessage(String appName, String server, String url);

    /**
     * Translated "Overwrite {0}?".
     *
     * @return translated "Overwrite {0}?"
     */
    @DefaultMessage("Overwrite {0}?")
    @Key("overwriteAppName")
    String overwriteAppName(String appName);

    /**
     * Translated "index".
     *
     * @return translated "index"
     */
    @DefaultMessage("index")
    @Key("index")
    String index();

    /**
     * Translated "Only files in the same folder as the document ({0}) or one of its sub-folders may be added.".
     *
     * @return translated "Only files in the same folder as the document ({0}) or one of its sub-folders may be added."
     */
    @DefaultMessage("Only files in the same folder as the document ({0}) or one of its sub-folders may be added.")
    @Key("onAddFileClickMessage")
    String onAddFileClickMessage(String dirPath);

    /**
     * Translated "Cannot Add File".
     *
     * @return translated "Cannot Add File"
     */
    @DefaultMessage("Cannot Add File")
    @Key("cannotAddFile")
    String cannotAddFile();

    /**
     * Translated "Select File".
     *
     * @return translated "Select File"
     */
    @DefaultMessage("Select File")
    @Key("selectFile")
    String selectFile();

    /**
     * Translated "Could not find files to deploy: \n\n{0}".
     *
     * @return translated "Could not find files to deploy: \n\n{0}"
     */
    @DefaultMessage("Could not find files to deploy: \n\n{0}")
    @Key("couldNotFindFilesToDeploy")
    String couldNotFindFilesToDeploy(String errorMessage);

    /**
     * Translated "Could not determine the list of files to deploy.".
     *
     * @return translated "Could not determine the list of files to deploy."
     */
    @DefaultMessage("Could not determine the list of files to deploy.")
    @Key("couldNotDetermineListToDeploy")
    String couldNotDetermineListToDeploy();

    /**
     * Translated "The item to be deployed ({0}) exceeds the maximum deployment size, which is {1}. Consider creating a new directory containing only the content you wish to deploy.".
     *
     * @return translated "The item to be deployed ({0}) exceeds the maximum deployment size, which is {1}. Consider creating a new directory containing only the content you wish to deploy."
     */
    @DefaultMessage("The item to be deployed ({0}) exceeds the maximum deployment size, which is {1}. Consider creating a new directory containing only the content you wish to deploy.")
    @Key("itemExceedsDeploymentSize")
    String itemExceedsDeploymentSize(String fileSource, String maxSize);

    /**
     * Translated "Collecting files...".
     *
     * @return translated "Collecting files..."
     */
    @DefaultMessage("Collecting files...")
    @Key("collectingFiles")
    String collectingFiles();

    /**
     * Translated "Could not determine the list of files to deploy. Try re-rendering and ensuring that you''re publishing to a server which supports this kind of content.".
     *
     * @return translated "Could not determine the list of files to deploy. Try re-rendering and ensuring that you''re publishing to a server which supports this kind of content."
     */
    @DefaultMessage("Could not determine the list of files to deploy. Try re-rendering and ensuring that you''re publishing to a server which supports this kind of content.")
    @Key("couldNotDetermineListToDeployReRender")
    String couldNotDetermineListToDeployReRender();

    /**
     * Translated "To publish finished document to RStudio Connect, you must first render it. Dismiss this message, click Knit to render the document, then try publishing again.".
     *
     * @return translated "To publish finished document to RStudio Connect, you must first render it. Dismiss this message, click Knit to render the document, then try publishing again."
     */
    @DefaultMessage("To publish finished document to RStudio Connect, you must first render it. Dismiss this message, click Knit to render the document, then try publishing again.")
    @Key("finishedDocumentNotFoundMessage")
    String finishedDocumentNotFoundMessage();

    /**
     * Translated "Finished Document Not Found".
     *
     * @return translated "Finished Document Not Found"
     */
    @DefaultMessage("Finished Document Not Found")
    @Key("finishedDocumentNotFound")
    String finishedDocumentNotFound();

    /**
     * Translated "Error retrieving accounts:\n\n{0}".
     *
     * @return translated "Error retrieving accounts:\n\n{0}"
     */
    @DefaultMessage("Error retrieving accounts:\n\n{0}")
    @Key("errorRetrievingAccountsWithMessage")
    String errorRetrievingAccountsWithMessage(String errorMessage);

    /**
     * Translated "Error retrieving application {0}.".
     *
     * @return translated "Error retrieving application {0}."
     */
    @DefaultMessage("Error retrieving application {0}.")
    @Key("errorRetrievingApplicationAppId")
    String errorRetrievingApplicationAppId(String appId);

    /**
     * Translated "Publish from Account".
     *
     * @return translated "Publish from Account"
     */
    @DefaultMessage("Publish from Account")
    @Key("publishFromAccount")
    String publishFromAccount();

    /**
     * Translated "To publish this content to a new location, click the Publish drop-down menu and choose Other Destination.".
     *
     * @return translated "To publish this content to a new location, click the Publish drop-down menu and choose Other Destination."
     */
    @DefaultMessage("To publish this content to a new location, click the Publish drop-down menu and choose Other Destination.")
    @Key("createNewAccountMessage")
    String createNewAccountMessage();

    /**
     * Translated "Create New Content".
     *
     * @return translated "Create New Content"
     */
    @DefaultMessage("Create New Content")
    @Key("createNewAccount")
    String createNewAccount();

    /**
     * Translated "Deploying...".
     *
     * @return translated "Deploying..."
     */
    @DefaultMessage("Deploying...")
    @Key("deploying")
    String deploying();

    /**
     * Translated "Launch browser".
     *
     * @return translated "Launch browser"
     */
    @DefaultMessage("Launch browser")
    @Key("launchBrowser")
    String launchBrowser();

    /**
     * Translated "Publish to Server".
     *
     * @return translated "Publish to Server"
     */
    @DefaultMessage("Publish to Server")
    @Key("publishToServer")
    String publishToServer();

    /**
     * Translated "Unable to determine file to be published. Click Knit or Preview to render it again, then click the Publish button above the rendered document.".
     *
     * @return translated "Unable to determine file to be published. Click Knit or Preview to render it again, then click the Publish button above the rendered document."
     */
    @DefaultMessage("Unable to determine file to be published. Click Knit or Preview to render it again, then click the Publish button above the rendered document.")
    @Key("contentPublishFailedMessage")
    String contentPublishFailedMessage();

    /**
     * Translated "Content Publish Failed".
     *
     * @return translated "Content Publish Failed"
     */
    @DefaultMessage("Content Publish Failed")
    @Key("contentPublishFailed")
    String contentPublishFailed();

    /**
     * Translated "Local deployment history for {0} successfully removed.".
     *
     * @return translated "Local deployment history for {0} successfully removed."
     */
    @DefaultMessage("Local deployment history for {0} successfully removed.")
    @Key("clearListMessage")
    String clearListMessage(String appLabel);

    /**
     * Translated "Clear List".
     *
     * @return translated "Clear List"
     */
    @DefaultMessage("Clear List")
    @Key("clearList")
    String clearList();

    /**
     * Translated "this application".
     *
     * @return translated "this application"
     */
    @DefaultMessage("this application")
    @Key("thisApplication")
    String thisApplication();

    /**
     * Translated "Publish {0}...".
     *
     * @return translated "Publish {0}..."
     */
    @DefaultMessage("Publish {0}...")
    @Key("publishContent")
    String publishContent(String contentDesc);

    /**
     * Translated "Other Destination...".
     *
     * @return translated "Other Destination..."
     */
    @DefaultMessage("Other Destination...")
    @Key("otherDestination")
    String otherDestination();

    /**
     * Translated "Are you sure you want to remove all local deployment history for {0}?".
     *
     * @return translated "Are you sure you want to remove all local deployment history for {0}?"
     */
    @DefaultMessage("Are you sure you want to remove all local deployment history for {0}?")
    @Key("removeLocalDeploymentMessage")
    String removeLocalDeploymentMessage(String appLabel);

    /**
     * Translated "Republish".
     *
     * @return translated "Republish"
     */
    @DefaultMessage("Republish")
    @Key("republish")
    String republish();

    /**
     * Translated "The content type ''{0}'' is not currently supported for publishing.".
     *
     * @return translated "The content type ''{0}'' is not currently supported for publishing."
     */
    @DefaultMessage("The content type ''{0}'' is not currently supported for publishing.")
    @Key("contentNotSupportedForPublishing")
    String contentNotSupportedForPublishing(String contentTypeDesc);

    /**
     * Translated "Can''t publish {0}".
     *
     * @return translated "Can''t publish {0}"
     */
    @DefaultMessage("Can''t publish {0}")
    @Key("cantPublishContent")
    String cantPublishContent(String contentTypeDesc);

    /**
     * Translated "Unsaved documents cannot be published. Save the document before publishing it.".
     *
     * @return translated "Unsaved documents cannot be published. Save the document before publishing it."
     */
    @DefaultMessage("Unsaved documents cannot be published. Save the document before publishing it.")
    @Key("unsavedDocumentPublishMessage")
    String unsavedDocumentPublishMessage();

    /**
     * Translated "Unsaved Document".
     *
     * @return translated "Unsaved Document"
     */
    @DefaultMessage("Unsaved Document")
    @Key("unsavedDocument")
    String unsavedDocument();

    /**
     * Translated "No HTML could be generated for the content.".
     *
     * @return translated "No HTML could be generated for the content."
     */
    @DefaultMessage("No HTML could be generated for the content.")
    @Key("noHTMLGenerated")
    String noHTMLGenerated();

    /**
     * Translated "This copy of the content has been published to the server ''{0}'', but you currently do not have any accounts registered on that server. \n\nConnect an account on the server ''{0}'' to update the application, or publish the content to a different server.".
     *
     * @return translated "This copy of the content has been published to the server ''{0}'', but you currently do not have any accounts registered on that server. \n\nConnect an account on the server ''{0}'' to update the application, or publish the content to a different server."
     */
    @DefaultMessage("This copy of the content has been published to the server ''{0}'', but you currently do not have any accounts registered on that server. \n\nConnect an account on the server ''{0}'' to update the application, or publish the content to a different server.")
    @Key("hostNotRegisteredMessage")
    String hostNotRegisteredMessage(String host);

    /**
     * Translated "{0} Not Registered".
     *
     * @return translated "{0} Not Registered"
     */
    @DefaultMessage("{0} Not Registered")
    @Key("hostNotRegistered")
    String hostNotRegistered(String host);

    /**
     * Translated "Publish options".
     *
     * @return translated "Publish options"
     */
    @DefaultMessage("Publish options")
    @Key("publishOptions")
    String publishOptions();

    /**
     * Translated "Reconnect Account".
     *
     * @return translated "Reconnect Account"
     */
    @DefaultMessage("Reconnect Account")
    @Key("reconnectAccount")
    String reconnectAccount();

    /**
     * Translated "Publishing content".
     *
     * @return translated "Publishing content"
     */
    @DefaultMessage("Publishing content")
    @Key("publishingContentLabel")
    String publishingContentLabel();

    /**
     * Translated "Publish Wizard".
     *
     * @return translated "Publish Wizard"
     */
    @DefaultMessage("Publish Wizard")
    @Key("publishWizardLabel")
    String publishWizardLabel();

    /**
     * Translated "Connect Posit Cloud account".
     *
     * @return translated "Connect Posit Cloud account"
     */
    @DefaultMessage("Connect Posit Cloud account")
    @Key("newPositCloudPageCaption")
    String newPositCloudPageCaption();

    /**
     * Translated "Our online service that lets you do, share, teach and learn data science in your web browser.".
     *
     * @return translated "Our online service that lets you do, share, teach and learn data science in your web browser."
     */
    @DefaultMessage("Our online service that lets you do, share, teach and learn data science in your web browser.")
    @Key("newPositCloudPageSubTitle")
    String newPositCloudPageSubTitle();
}

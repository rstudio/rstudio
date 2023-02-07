/*
 * StudioClientApplicationConstants.java
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

package org.rstudio.studio.client.application;

public interface StudioClientApplicationConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Retry".
     *
     * @return translated "Retry"
     */
    @DefaultMessage("Retry")
    @Key("retryButtonLabel")
    String retryButtonLabel();

    /**
     * Translated "Licensing Limit Reached".
     *
     * @return translated "Licensing Limit Reached"
     */
    @DefaultMessage("Licensing Limit Reached")
    @Key("licensingLimitCaption")
    String licensingLimitCaption();

    /**
     * Translated "Please quit any unused running sessions and try again, or contact your administrator to update your license.".
     *
     * @return translated "Please quit any unused running sessions and try again, or contact your administrator to update your license."
     */
    @DefaultMessage("{0}\\n\\n Please quit any unused running sessions and try again, or contact your administrator to update your license.")
    @Key("quitRunningSessionsMessage")
    String quitRunningSessionsMessage(String userMsg);

    /**
     * Translated "Unable to connect to service".
     *
     * @return translated "Unable to connect to service"
     */
    @DefaultMessage("Unable to connect to service")
    @Key("unableToConnectMessage")
    String unableToConnectMessage();

    /**
     * Translated "Error occurred during transmission".
     *
     * @return translated "Error occurred during transmission"
     */
    @DefaultMessage("Error occurred during transmission")
    @Key("errorTransmissionMessage")
    String errorTransmissionMessage();

    /**
     * Translated "Go Home".
     *
     * @return translated "Go Home"
     */
    @DefaultMessage("Go Home")
    @Key("goHomeButtonLabel")
    String goHomeButtonLabel();

    /**
     * Translated "Cannot Connect to R Session".
     *
     * @return translated "Cannot Connect to R Session"
     */
    @DefaultMessage("Cannot Connect to R Session")
    @Key("cannotConnectRCaption")
    String cannotConnectRCaption();

    /**
     * Translated "Could not connect to the R session on RStudio Server.".
     *
     * @return translated "Could not connect to the R session on RStudio Server."
     */
    @DefaultMessage("Could not connect to the R session on RStudio Server.\n\n{0} ({1})")
    @Key("cannotConnectRMessage")
    String cannotConnectRMessage(String userMessage, int errorCode);

    /**
     * Translated "RStudio Initialization Error".
     *
     * @return translated "RStudio Initialization Error"
     */
    @DefaultMessage("RStudio Initialization Error")
    @Key("rStudioInitializationErrorCaption")
    String rStudioInitializationErrorCaption();

    /**
     * Translated "docs".
     *
     * @return translated "docs"
     */
    @DefaultMessage("docs")
    @Key("helpUsingRStudioLinkName")
    String helpUsingRStudioLinkName();

    /**
     * Translated "community-forum".
     *
     * @return translated "community-forum"
     */
    @DefaultMessage("community-forum")
    @Key("communityForumLinkName")
    String communityForumLinkName();

    /**
     * Translated "support".
     *
     * @return translated "support"
     */
    @DefaultMessage("support")
    @Key("rStudioSupportLinkName")
    String rStudioSupportLinkName();

    /**
     * Translated "Focused Element: ".
     *
     * @return translated "Focused Element: "
     */
    @DefaultMessage("Focused Element: ")
    @Key("focusedElementLabel")
    String focusedElementLabel();

    /**
     * Translated "Loading workspace".
     *
     * @return translated "Loading workspace"
     */
    @DefaultMessage("Loading workspace")
    @Key("loadingWorkspaceMessage")
    String loadingWorkspaceMessage();

    /**
     * Translated "Saving workspace image".
     *
     * @return translated "Saving workspace image"
     */
    @DefaultMessage("Saving workspace image")
    @Key("savingWorkspaceImageMessage")
    String savingWorkspaceImageMessage();

    /**
     * Translated "Backing up R session...".
     *
     * @return translated "Backing up R session..."
     */
    @DefaultMessage("Backing up R session...")
    @Key("backingUpRSessionMessage")
    String backingUpRSessionMessage();

    /**
     * Translated "Switch R Version".
     *
     * @return translated "Switch R Version"
     */
    @DefaultMessage("Switch R Version")
    @Key("switchRVersionCaption")
    String switchRVersionCaption();

    /**
     * Translated "The workspace was not restored".
     *
     * @return translated "The workspace was not restored"
     */
    @DefaultMessage("The workspace was not restored")
    @Key("workspaceNotRestoredMessage")
    String workspaceNotRestoredMessage();

    /**
     * Translated ", and startup scripts were not executed".
     *
     * @return translated ", and startup scripts were not executed"
     */
    @DefaultMessage(", and startup scripts were not executed")
    @Key("startupScriptsNotExecutedMessage")
    String startupScriptsNotExecutedMessage();

    /**
     * Translated "Startup scripts were not executed.".
     *
     * @return translated "Startup scripts were not executed."
     */
    @DefaultMessage("Startup scripts were not executed.")
    @Key("startupScriptsErrorMessage")
    String startupScriptsErrorMessage();

    /**
     * Translated "This R session was started in safe mode. ".
     *
     * @return translated "This R session was started in safe mode. "
     */
    @DefaultMessage("This R session was started in safe mode. ")
    @Key("rSessionSafeModeMessage")
    String rSessionSafeModeMessage();

    /**
     * Translated "Are you sure you want to terminate R?".
     *
     * @return translated "Are you sure you want to terminate R?"
     */
    @DefaultMessage("Are you sure you want to terminate R?")
    @Key("terminateRMessage")
    String terminateRMessage();

    /**
     * Translated "R is not responding to your request to interrupt processing so to stop the current operation you may need to terminate R entirely.\n\n{0}\n\n Do you want to terminate R now?".
     *
     * @return translated "R is not responding to your request to interrupt processing so to stop the current operation you may need to terminate R entirely.\n\n{0}\n\n Do you want to terminate R now?"
     */
    @DefaultMessage("R is not responding to your request to interrupt processing so to stop the current operation you may need to terminate R entirely.\\n\\n{0}\\n\\n Do you want to terminate R now?")
    @Key("terminationDialog")
    String terminationDialog(String terminationMsg);

    /**
     * Translated "Terminate R".
     *
     * @return translated "Terminate R"
     */
    @DefaultMessage("Terminate R")
    @Key("terminateRCaption")
    String terminateRCaption();

    /**
     * Translated "Terminating R will cause your R session to immediately abort. Active computations will be interrupted and unsaved source file changes and workspace objects will be discarded.".
     *
     * @return translated "Terminating R will cause your R session to immediately abort. Active computations will be interrupted and unsaved source file changes and workspace objects will be discarded."
     */
    @DefaultMessage("Terminating R will cause your R session to immediately abort. Active computations will be interrupted and unsaved source file changes and workspace objects will be discarded.")
    @Key("terminationConsequenceMessage")
    String terminationConsequenceMessage();

    /**
     * Translated "The R session is currently busy.".
     *
     * @return translated "The R session is currently busy."
     */
    @DefaultMessage("The R session is currently busy.")
    @Key("rSessionCurrentlyBusyMessage")
    String rSessionCurrentlyBusyMessage();

    /**
     * Translated "The R session and the terminal are currently busy.".
     *
     * @return translated "The R session and the terminal are currently busy."
     */
    @DefaultMessage("The R session and the terminal are currently busy.")
    @Key("rSessionTerminalBusyMessage")
    String rSessionTerminalBusyMessage();

    /**
     * Translated "The terminal is currently busy.".
     *
     * @return translated "The terminal is currently busy."
     */
    @DefaultMessage("The terminal is currently busy.")
    @Key("terminalCurrentlyBusyMessage")
    String terminalCurrentlyBusyMessage();

    /**
     * Translated "{0} Are you sure you want to quit?".
     *
     * @return translated "{0} Are you sure you want to quit?"
     */
    @DefaultMessage("{0} Are you sure you want to quit?")
    @Key("applicationQuitMessage")
    String applicationQuitMessage(String message);

    /**
     * Translated "Are you sure you want to quit the R session?".
     *
     * @return translated "Are you sure you want to quit the R session?"
     */
    @DefaultMessage("Are you sure you want to quit the R session?")
    @Key("quitRSessionsMessage")
    String quitRSessionsMessage();

    /**
     * Translated "Save workspace image to ".
     *
     * @return translated "Save workspace image to "
     */
    @DefaultMessage("Save workspace image to ")
    @Key("saveWorkspaceImageMessage")
    String saveWorkspaceImageMessage();

    /**
     * Translated "Quit R Session".
     *
     * @return translated "Quit R Session"
     */
    @DefaultMessage("Quit R Session")
    @Key("quitRSessionTitle")
    String quitRSessionTitle();

    /**
     * Translated "Restart R".
     *
     * @return translated "Restart R"
     */
    @DefaultMessage("Restart R")
    @Key("restartRCaption")
    String restartRCaption();

    /**
     * Translated "Terminal jobs will be terminated. Are you sure?".
     *
     * @return translated "Terminal jobs will be terminated. Are you sure?"
     */
    @DefaultMessage("Terminal jobs will be terminated. Are you sure?")
    @Key("terminalJobTerminatedQuestion")
    String terminalJobTerminatedQuestion();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("progressErrorCaption")
    String progressErrorCaption();

    /**
     * Translated "Restarting R..".
     *
     * @return translated "Restarting R.."
     */
    @DefaultMessage("Restarting R..")
    @Key("restartingRMessage")
    String restartingRMessage();

    /**
     * Translated "Quit R Session".
     *
     * @return translated "Quit R Session"
     */
    @DefaultMessage("Quit R Session")
    @Key("quitRSessionCaption")
    String quitRSessionCaption();

    /**
     * Translated "Restarting RStudio".
     *
     * @return translated "Restarting RStudio"
     */
    @DefaultMessage("Restarting RStudio")
    @Key("restartRStudio")
    String restartRStudio();

    /**
     * Translated "Workspace image (.RData)".
     *
     * @return translated "Workspace image (.RData)"
     */
    @DefaultMessage("Workspace image (.RData)")
    @Key("studioClientTitle")
    String studioClientTitle();

    /**
     * Translated "Switching to project ".
     *
     * @return translated "Switching to project "
     */
    @DefaultMessage("Switching to project ")
    @Key("switchingToProjectMessage")
    String switchingToProjectMessage();

    /**
     * Translated "Closing project".
     *
     * @return translated "Closing project"
     */
    @DefaultMessage("Closing project")
    @Key("closingProjectMessage")
    String closingProjectMessage();

    /**
     * Translated "Quitting R Session...".
     *
     * @return translated "Quitting R Session..."
     */
    @DefaultMessage("Quitting R Session...")
    @Key("quitRSessionMessage")
    String quitRSessionMessage();

    /**
     * Translated "server quitSession responded false".
     *
     * @return translated "server quitSession responded false"
     */
    @DefaultMessage("server quitSession responded false")
    @Key("serverQuitSession")
    String serverQuitSession();

    /**
     * Translated "Console cleared".
     *
     * @return translated "Console cleared"
     */
    @DefaultMessage("Console cleared")
    @Key("consoleClearedAnnouncement")
    String consoleClearedAnnouncement();

    /**
     * Translated "Console output (requires restart)".
     *
     * @return translated "Console output (requires restart)"
     */
    @DefaultMessage("Console output (requires restart)")
    @Key("consoleOutputAnnouncement")
    String consoleOutputAnnouncement();

    /**
     * Translated "Console command (requires restart)".
     *
     * @return translated "Console command (requires restart)"
     */
    @DefaultMessage("Console command (requires restart)")
    @Key("consoleCommandAnnouncement")
    String consoleCommandAnnouncement();

    /**
     * Translated "Filtered result count".
     *
     * @return translated "Filtered result count"
     */
    @DefaultMessage("Filtered result count")
    @Key("filterResultCountAnnouncement")
    String filterResultCountAnnouncement();

    /**
     * Translated "Commit message length".
     *
     * @return translated "Commit message length"
     */
    @DefaultMessage("Commit message length")
    @Key("commitMessageLengthAnnouncement")
    String commitMessageLengthAnnouncement();

    /**
     * Translated "Inaccessible feature warning".
     *
     * @return translated "Inaccessible feature warning"
     */
    @DefaultMessage("Inaccessible feature warning")
    @Key("inaccessibleWarningAnnouncement")
    String inaccessibleWarningAnnouncment();

    /**
     * Translated "Info bars".
     *
     * @return translated "Info bars"
     */
    @DefaultMessage("Info bars")
    @Key("infoBarsAnnouncement")
    String infoBarsAnnouncment();

    /**
     * Translated "Task completion".
     *
     * @return translated "Task completion"
     */
    @DefaultMessage("Task completion")
    @Key("taskCompletionAnnouncement")
    String taskCompletionAnnouncement();

    /**
     * Translated "Task progress details".
     *
     * @return translated "Task progress details"
     */
    @DefaultMessage("Task progress details")
    @Key("taskProgressAnnouncement")
    String taskProgressAnnouncement();

    /**
     * Translated "Screen reader not enabled".
     *
     * @return translated "Screen reader not enabled"
     */
    @DefaultMessage("Screen reader not enabled")
    @Key("screenReaderAnnouncement")
    String screenReaderAnnouncement();

    /**
     * Translated "Changes in session state".
     *
     * @return translated "Changes in session state"
     */
    @DefaultMessage("Changes in session state")
    @Key("sessionStateAnnouncement")
    String sessionStateAnnouncement();

    /**
     * Translated "Tab key focus mode change".
     *
     * @return translated "Tab key focus mode change"
     */
    @DefaultMessage("Tab key focus mode change")
    @Key("tabKeyFocusAnnouncement")
    String tabKeyFocusAnnouncement();

    /**
     * Translated "Toolbar visibility change".
     *
     * @return translated "Toolbar visibility change"
     */
    @DefaultMessage("Toolbar visibility change")
    @Key("toolBarVisibilityAnnouncement")
    String toolBarVisibilityAnnouncement();

    /**
     * Translated "Warning bars".
     *
     * @return translated "Warning bars"
     */
    @DefaultMessage("Warning bars")
    @Key("warningBarsAnnouncement")
    String warningBarsAnnouncement();

    /**
     * Translated "Session suspension"
     *
     * @return translated "Session suspension"
     */
    @DefaultMessage("Session suspension")
    @Key("sessionSuspendAnnouncement")
    String sessionSuspendAnnouncement();

    /**
     * Translated "Unregistered live announcement: ".
     *
     * @return translated "Unregistered live announcement: "
     */
    @DefaultMessage("Unregistered live announcement: ")
    @Key("unregisteredLiveAnnouncementMessage")
    String unregisteredLiveAnnouncementMessage();

    /**
     * Translated "Close Remote Session".
     *
     * @return translated "Close Remote Session"
     */
    @DefaultMessage("Close Remote Session")
    @Key("closeRemoteSessionCaption")
    String closeRemoteSessionCaption();

    /**
     * Translated "Do you want to close the remote session?".
     *
     * @return translated "Do you want to close the remote session?"
     */
    @DefaultMessage("Do you want to close the remote session?")
    @Key("closeRemoteSessionMessage")
    String closeRemoteSessionMessage();

    /**
     * Translated "Unable to obtain a license. Please restart RStudio to try again.".
     *
     * @return translated "Unable to obtain a license. Please restart RStudio to try again."
     */
    @DefaultMessage("Unable to obtain a license. Please restart RStudio to try again.")
    @Key("licenseLostMessage")
    String licenseLostMessage();

    /**
     * Translated "Details: ".
     *
     * @return translated "Details: "
     */
    @DefaultMessage("Details: ")
    @Key("detailsMessage")
    String detailsMessage();

    /**
     * Translated "Connection Received".
     *
     * @return translated "Connection Received"
     */
    @DefaultMessage("Connection Received")
    @Key("connectionReceivedType")
    String connectionReceivedType();

    /**
     * Translated "Connection Dequeued".
     *
     * @return translated "Connection Dequeued"
     */
    @DefaultMessage("Connection Dequeued")
    @Key("connectionDequeuedType")
    String connectionDequeuedType();

    /**
     * Translated "Connection Responded".
     *
     * @return translated "Connection Responded"
     */
    @DefaultMessage("Connection Responded")
    @Key("connectionRespondedType")
    String connectionRespondedType();

    /**
     * Translated "Connection Terminated".
     *
     * @return translated "Connection Terminated"
     */
    @DefaultMessage("Connection Terminated")
    @Key("connectionTerminatedType")
    String connectionTerminatedType();

    /**
     * Translated "Connection Error".
     *
     * @return translated "Connection Error"
     */
    @DefaultMessage("Connection Error")
    @Key("connectionErrorType")
    String connectionErrorType();

    /**
     * Translated "(Unknown).
     *
     * @return translated "(Unknown)"
     */
    @DefaultMessage("(Unknown)")
    @Key("connectionUnknownType")
    String connectionUnknownType();

    /**
     * Translated "RStudio{0}".
     *
     * @return translated "RStudio{0}"
     */
    @DefaultMessage("RStudio{0}")
    @Key("rStudioEditionName")
    String rStudioEditionName(String desktop);

    /**
     * Translated "Server".
     *
     * @return translated "Server"
     */
    @DefaultMessage("Server")
    @Key("serverLabel")
    String serverLabel();

    /**
     * Translated "Copy Version".
     *
     * @return translated "Copy Version"
     */
    @DefaultMessage("Copy Version")
    @Key("copyVersionButtonTitle")
    String copyVersionButtonTitle();

    /**
     * Translated "About {0}".
     *
     * @return translated "About {0}"
     */
    @DefaultMessage("About {0}")
    @Key("title")
    String title(String version);

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    @Key("okBtn")
    String okBtn();

    /**
     * Translated "Manage License...".
     *
     * @return translated "Manage License..."
     */
    @DefaultMessage("Manage License...")
    @Key("manageLicenseBtn")
    String manageLicenseBtn();

    /**
     * Translated "for ".
     *
     * @return translated "for "
     */
    @DefaultMessage("for ")
    @Key("forText")
    String forText();

    /**
     * Translated "Version Copied".
     *
     * @return translated "Version Copied"
     */
    @DefaultMessage("Version Copied")
    @Key("versionCopiedText")
    String versionCopiedText();

    /**
     * Translated "Version information copied to clipboard.".
     *
     * @return translated "Version information copied to clipboard."
     */
    @DefaultMessage("Version information copied to clipboard.")
    @Key("versionInformationCopiedText")
    String versionInformationCopiedText();

    /**
     * Translated "Copy Version".
     *
     * @return translated "Copy Version"
     */
    @DefaultMessage("Copy Version")
    @Key("copyVersionButton")
    String copyVersionButton();

    /**
     * Translated "Build ".
     *
     * @return translated "Build "
     */
    @DefaultMessage("Build ")
    @Key("versionBuildLabel")
    String versionBuildLabel();

    /**
     * Translated ") for ".
     *
     * @return translated ") for "
     */
    @DefaultMessage(") for ")
    @Key("buildLabelForText")
    String buildLabelForText();

    /**
     * Translated "This ".
     *
     * @return translated "This "
     */
    @DefaultMessage("This ")
    @Key("buildTypeThisText")
    String buildTypeThisText();

    /**
     * Translated "build of ".
     *
     * @return translated "build of "
     */
    @DefaultMessage("build of ")
    @Key("buildOfText")
    String buildOfText();

    /**
     * Translated "is provided by Posit Software, PBC for testing purposes only and is not an officially supported release.".
     *
     * @return translated "is provided by Posit Software, PBC for testing purposes only and is not an officially supported release."
     */
    @DefaultMessage("is provided by Posit Software, PBC for testing purposes only and is not an officially supported release.")
    @Key("supportNoticeText")
    String supportNoticeText();

    /**
     * Translated "Loading...".
     *
     * @return translated "Loading..."
     */
    @DefaultMessage("Loading...")
    @Key("licenseBoxLoadingText")
    String licenseBoxLoadingText();

    /**
     * Translated "Open Source Components".
     *
     * @return translated "Open Source Components"
     */
    @DefaultMessage("Open Source Components")
    @Key("openSourceComponentsText")
    String openSourceComponentsText();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    @Key("closeButtonText")
    String closeButtonText();

    /**
     * Translated "Open Source Component".
     *
     * @return translated "Open Source Component"
     */
    @DefaultMessage("Open Source Component")
    @Key("openSourceComponentText")
    String openSourceComponentText();

    /**
     * Translated "Main toolbar visible".
     *
     * @return translated "Main toolbar visible"
     */
    @DefaultMessage("Main toolbar visible")
    @Key("toolbarVisibleText")
    String toolbarVisibleText();

    /**
     * Translated "Main toolbar hidden".
     *
     * @return translated "Main toolbar hidden"
     */
    @DefaultMessage("Main toolbar hidden")
    @Key("toolbarHiddenText")
    String toolbarHiddenText();

    /**
     * Translated "Toolbar hidden, unable to focus.".
     *
     * @return translated "Toolbar hidden, unable to focus."
     */
    @DefaultMessage("Toolbar hidden, unable to focus.")
    @Key("focusToolbarText")
    String focusToolbarText();

    /**
     * Translated "Application Updated".
     *
     * @return translated "Application Updated"
     */
    @DefaultMessage("Application Updated")
    @Key("applicationUpdatedCaption")
    String applicationUpdatedCaption();

    /**
     * Translated "An updated version of RStudio is available. Your browser will now be refreshed with the new version. All current work and data will be preserved during the update.".
     *
     * @return translated "An updated version of RStudio is available. Your browser will now be refreshed with the new version. All current work and data will be preserved during the update."
     */
    @DefaultMessage("An updated version of RStudio is available. Your browser will now be refreshed with the new version. All current work and data will be preserved during the update.")
    @Key("applicationUpdatedMessage")
    String applicationUpdatedMessage();

    /**
     * Translated "Warning bar".
     *
     * @return translated "Warning bar"
     */
    @DefaultMessage("Warning bar")
    @Key("warningBarText")
    String warningBarText();

    /**
     * Translated "R Session Error".
     *
     * @return translated "R Session Error"
     */
    @DefaultMessage("R Session Error")
    @Key("rSessionErrorCaption")
    String rSessionErrorCaption();

    /**
     * Translated "The previous R session was abnormally terminated due to an unexpected crash.\n\nYou may have lost workspace data as a result of this crash.".
     *
     * @return translated "The previous R session was abnormally terminated due to an unexpected crash.\n\nYou may have lost workspace data as a result of this crash."
     */
    @DefaultMessage("The previous R session was abnormally terminated due to an unexpected crash.\\n\\nYou may have lost workspace data as a result of this crash.")
    @Key("previousRSessionsMessage")
    String previousRSessionsMessage();

    /**
     * Translated "Main".
     *
     * @return translated "Main"
     */
    @DefaultMessage("Main")
    @Key("mainLabel")
    String mainLabel();

    /**
     * Translated "New File".
     *
     * @return translated "New File"
     */
    @DefaultMessage("New File")
    @Key("newFileTitle")
    String newFileTitle();

    /**
     * Translated "Open recent files".
     *
     * @return translated "Open recent files"
     */
    @DefaultMessage("Open recent files")
    @Key("openRecentFilesTitle")
    String openRecentFilesTitle();

    /**
     * Translated "Version control".
     *
     * @return translated "Version control"
     */
    @DefaultMessage("Version control")
    @Key("versionControlTitle")
    String versionControlTitle();

    /**
     * Translated "Workspace Panes".
     *
     * @return translated "Workspace Panes"
     */
    @DefaultMessage("Workspace Panes")
    @Key("workspacePanesTitle")
    String workspacePanesTitle();

    /**
     * Translated "Project: (None)".
     *
     * @return translated "Project: (None)"
     */
    @DefaultMessage("Project: (None)")
    @Key("toolBarButtonText")
    String toolBarButtonText();

    /**
     * Translated "Looking for projects...".
     *
     * @return translated "Looking for projects..."
     */
    @DefaultMessage("Looking for projects...")
    @Key("popupMenuProgressMessage")
    String popupMenuProgressMessage();

    /**
     * Translated "Recent Projects".
     *
     * @return translated "Recent Projects"
     */
    @DefaultMessage("Recent Projects")
    @Key("recentProjectsLabel")
    String recentProjectsLabel();

    /**
     * Translated "Shared with Me".
     *
     * @return translated "Shared with Me"
     */
    @DefaultMessage("Shared with Me")
    @Key("sharedWithMeLabel")
    String sharedWithMeLabel();

    /**
     * Translated "Export".
     *
     * @return translated "Export"
     */
    @DefaultMessage("Export")
    @Key("exportCaption")
    String exportCaption();

    /**
     * Translated "Import".
     *
     * @return translated "Import"
     */
    @DefaultMessage("Import")
    @Key("importCaption")
    String importCaption();

    /**
     * Translated "Reloading...".
     *
     * @return translated "Reloading..."
     */
    @DefaultMessage("Reloading...")
    @Key("reloadingText")
    String reloadingText();

    /**
     * Translated "Retrying in Safe Mode...".
     *
     * @return translated "Retrying in Safe Mode..."
     */
    @DefaultMessage("Retrying in Safe Mode...")
    @Key("retryInSafeModeText")
    String retryInSafeModeText();

    /**
     * Translated "Terminating R...".
     *
     * @return translated "Terminating R..."
     */
    @DefaultMessage("Terminating R...")
    @Key("terminatingRText")
    String terminatingRText();

    /**
     * Translated "Default version of R:".
     *
     * @return translated "Default version of R:"
     */
    @DefaultMessage("Default version of R:")
    @Key("defaultVersionRCaption")
    String defaultVersionRCaption();

    /**
     * Translated "Help on R versions".
     *
     * @return translated "Help on R versions"
     */
    @DefaultMessage("Help on R versions")
    @Key("helpOnRVersionsTitle")
    String helpOnRVersionsTitle();

    /**
     * Translated "Module ".
     *
     * @return translated "Module "
     */
    @DefaultMessage("Module ")
    @Key("moduleText")
    String moduleText();

    /**
     * Translated "R version ".
     *
     * @return translated "R version "
     */
    @DefaultMessage("R version ")
    @Key("rVersionText")
    String rVersionText();

    /**
     * Translated "(Use System Default)".
     *
     * @return translated "(Use System Default)"
     */
    @DefaultMessage("(Use System Default)")
    @Key("useSystemDefaultText")
    String useSystemDefaultText();

    /**
     * Translated "User-specified...".
     *
     * @return translated "User-specified..."
     */
    @DefaultMessage("User-specified...")
    @Key("userSpecifiedText")
    String userSpecifiedText();

    /**
     * Translated "Manage License...".
     *
     * @return translated "Manage License..."
     */
    @DefaultMessage("Manage License...")
    @Key("manageLicenseText")
    String manageLicenseText();

    /**
     * Translated "Addins".
     *
     * @return translated "Addins"
     */
    @DefaultMessage("Addins")
    @Key("addinsText")
    String addinsText();

    /**
     * Translated "Search for addins".
     *
     * @return translated "Search for addins"
     */
    @DefaultMessage("Search for addins")
    @Key("searchForAddinsLabel")
    String searchForAddinsLabel();

    /**
     * Translated "No addins found".
     *
     * @return translated "No addins found"
     */
    @DefaultMessage("No addins found")
    @Key("noAddinsFound")
    String noAddinsFound();

    /**
     * Translated "R encountered a fatal error.".
     *
     * @return translated "R encountered a fatal error."
     */
    @DefaultMessage("R encountered a fatal error.")
    @Key("rFatalErrorMessage")
    String rFatalErrorMessage();

    /**
     * Translated "R encountered a fatal error.".
     *
     * @return translated "The session was terminated."
     */
    @DefaultMessage("The session was terminated.")
    @Key("sessionTerminatedMessage")
    String sessionTerminatedMessage();

    /**
     * Translated "This browser was disconnected from the R session because another browser connected (only one browser at a time may be connected to an RStudio session). You may reconnect using the button below.".
     *
     * @return translated "This browser was disconnected from the R session because another browser connected (only one browser at a time may be connected to an RStudio session). You may reconnect using the button below."
     */
    @DefaultMessage("This browser was disconnected from the R session because another browser connected (only one browser at a time may be connected to an RStudio session). You may reconnect using the button below.")
    @Key("browserDisconnectedMessage")
    String browserDisconnectedMessage();

    /**
     * Translated "RStudio is temporarily offline due to system maintenance. We apologize for the inconvenience, please try again in a few minutes.".
     *
     * @return translated "RStudio is temporarily offline due to system maintenance. We apologize for the inconvenience, please try again in a few minutes."
     */
    @DefaultMessage("RStudio is temporarily offline due to system maintenance. We apologize for the inconvenience, please try again in a few minutes.")
    @Key("rStudioOfflineMessage")
    String rStudioOfflineMessage();

    /**
     * Translated "R Session Ended".
     *
     * @return translated "R Session Ended"
     */
    @DefaultMessage("R Session Ended")
    @Key("rSessionEndedCaption")
    String rSessionEndedCaption();

    /**
     * Translated "Start New Session".
     *
     * @return translated "Start New Session"
     */
    @DefaultMessage("Start New Session")
    @Key("startNewSessionText")
    String startNewSessionText();

    /**
     * Translated "R Session Aborted".
     *
     * @return translated "R Session Aborted"
     */
    @DefaultMessage("R Session Aborted")
    @Key("rSessionAbortedCaption")
    String rSessionAbortedCaption();

    /**
     * Translated "R Session Disconnected".
     *
     * @return translated "R Session Disconnected"
     */
    @DefaultMessage("R Session Disconnected")
    @Key("rSessionDisconnectedCaption")
    String rSessionDisconnectedCaption();

    /**
     * Translated "Reconnect".
     *
     * @return translated "Reconnect"
     */
    @DefaultMessage("Reconnect")
    @Key("reconnectButtonText")
    String reconnectButtonText();

    /**
     * Translated "RStudio Temporarily Offline".
     *
     * @return translated "RStudio Temporarily Offline"
     */
    @DefaultMessage("RStudio Temporarily Offline")
    @Key("temporarilyOfflineCaption")
    String temporarilyOfflineCaption();

    /**
     * Translated "Unknown mode ".
     *
     * @return translated "Unknown mode "
     */
    @DefaultMessage("Unknown mode ")
    @Key("unknownModeText")
    String unknownModeText();

    /**
     * Translated "Sign out".
     *
     * @return translated "Sign out"
     */
    @DefaultMessage("Sign out")
    @Key("signOutButtonText")
    String signOutButtonText();

    /**
     * Translated "Error Opening Devtools".
     *
     * @return translated "Error Opening Devtools"
     */
    @DefaultMessage("Error Opening Devtools")
    @Key("errorOpeningDevToolsCaption")
    String errorOpeningDevToolsCaption();

    /**
     * Translated "The Chromium devtools server could not be activated.".
     *
     * @return translated "The Chromium devtools server could not be activated."
     */
    @DefaultMessage("The Chromium devtools server could not be activated.")
    @Key("cannotActivateDevtoolsMessage")
    String cannotActivateDevtoolsMessage();

    /**
     * Translated "Error Checking for Updates".
     *
     * @return translated "Error Checking for Updates"
     */
    @DefaultMessage("Error Checking for Updates")
    @Key("errorCheckingUpdatesMessage")
    String errorCheckingUpdatesMessage();

    /**
     * Translated "An error occurred while checking for updates: ".
     *
     * @return translated "An error occurred while checking for updates: "
     */
    @DefaultMessage("An error occurred while checking for updates: ")
    @Key("errorOccurredCheckingUpdatesMessage")
    String errorOccurredCheckingUpdatesMessage();

    /**
     * Translated "Quit and Download...".
     *
     * @return translated "Quit and Download..."
     */
    @DefaultMessage("Quit and Download...")
    @Key("quitDownloadButtonLabel")
    String quitDownloadButtonLabel();

    /**
     * Translated "Update RStudio".
     *
     * @return translated "Update RStudio"
     */
    @DefaultMessage("Update RStudio")
    @Key("updateRStudioCaption")
    String updateRStudioCaption();

    /**
     * Translated "Remind Later".
     *
     * @return translated "Remind Later"
     */
    @DefaultMessage("Remind Later")
    @Key("remindLaterButtonLabel")
    String remindLaterButtonLabel();

    /**
     * Translated "Ignore Update".
     *
     * @return translated "Ignore Update"
     */
    @DefaultMessage("Ignore Update")
    @Key("ignoreUpdateButtonLabel")
    String ignoreUpdateButtonLabel();

    /**
     * Translated "Update Available".
     *
     * @return translated "Update Available"
     */
    @DefaultMessage("Update Available")
    @Key("updateAvailableCaption")
    String updateAvailableCaption();

    /**
     * Translated "No Update Available".
     *
     * @return translated "No Update Available"
     */
    @DefaultMessage("No Update Available")
    @Key("noUpdateAvailableCaption")
    String noUpdateAvailableCaption();

    /**
     * Translated "You're using the newest version of RStudio.".
     *
     * @return translated "You're using the newest version of RStudio."
     */
    @DefaultMessage("You''re using the newest version of RStudio.")
    @Key("usingNewestVersionMessage")
    String usingNewestVersionMessage();

    /**
     * Translated "Posit Workbench".
     *
     * @return translated "Posit Workbench"
     */
    @DefaultMessage("Posit Workbench")
    @Key("rStudioServerHomeTitle")
    String rStudioServerHomeTitle();

    /**
     * Translated "Your browser does not allow access to your".
     *
     * @return translated "Your browser does not allow access to your"
     */
    @DefaultMessage("Your browser does not allow access to your")
    @Key("browserNotAllowAccessLabel")
    String browserNotAllowAccessLabel();

    /**
     * Translated "computer's clipboard. As a result you must".
     *
     * @return translated "computer's clipboard. As a result you must"
     */
    @DefaultMessage("computer''s clipboard. As a result you must")
    @Key("computerClipBoardLabel")
    String computerClipBoardLabel();

    /**
     * Translated "use keyboard shortcuts for:".
     *
     * @return translated "use keyboard shortcuts for:"
     */
    @DefaultMessage("use keyboard shortcuts for:")
    @Key("useKeyboardShortcutsLabel")
    String useKeyboardShortcutsLabel();

    /**
     * Translated "Use Keyboard Shortcut".
     *
     * @return translated "Use Keyboard Shortcut"
     */
    @DefaultMessage("Use Keyboard Shortcut")
    @Key("useKeyboardShortcutCaption")
    String useKeyboardShortcutCaption();

    /**
     * Translated "Sign out".
     *
     * @return translated "Sign out"
     */
    @DefaultMessage("Sign out")
    @Key("signOutTitle")
    String signOutTitle();

    /**
     * Translated "from ".
     *
     * @return translated "from "
     */
    @DefaultMessage("from ")
    @Key("fromText")
    String fromText();

    /**
     * Translated "to ".
     *
     * @return translated "to "
     */
    @DefaultMessage("to ")
    @Key("toText")
    String toText();

    /**
     * Translated "New Session...".
     *
     * @return translated "New Session..."
     */
    @DefaultMessage("New Session...")
    @Key("newSessionMenuLabel")
    String newSessionMenuLabel();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    @DefaultMessage("Save")
    @Key("saveYesLabel")
    String saveYesLabel();

    /**
     * Translated "Don't Save".
     *
     * @return translated "Don't Save"
     */
    @DefaultMessage("Don''t Save")
    @Key("saveNoLabel")
    String saveNoLabel();

    /**
     * Translated "(active)".
     *
     * @return translated "(active)"
     */
    @DefaultMessage("(active)")
    @Key("activeText")
    String activeText();

    /**
     * Translated "<p>Click on a request to see details. Click on the background to show these instructions again.</p><h4>Available commands:</h4><ul><li>Esc: Close</li><li>P: Play/pause</li><li>E: Export</li><li>I: Import</li><li>+/-: Zoom in/out</li></ul>".
     *
     * @return translated "<p>Click on a request to see details. Click on the background to show these instructions again.</p><h4>Available commands:</h4><ul><li>Esc: Close</li><li>P: Play/pause</li><li>E: Export</li><li>I: Import</li><li>+/-: Zoom in/out</li></ul>"
     */
    @DefaultMessage("<p>Click on a request to see details. Click on the background to show these instructions again.</p><h4>Available commands:</h4><ul><li>Esc: Close</li><li>P: Play/pause</li><li>E: Export</li><li>I: Import</li><li>+/-: Zoom in/out</li></ul>")
    @Key("requestLogVisualization")
    String requestLogVisualization();

    /**
     * Translated "Please visit https://posit.co/download/rstudio-desktop/ to check if a new version is available.".
     *
     * @return translated "Please visit https://posit.co/download/rstudio-desktop/ to check if a new version is available."
     */
    @DefaultMessage("Please visit https://posit.co/download/rstudio-desktop/ to check if a new version is available.")
    @Key("visitWebsiteForNewVersion")
    String visitWebsiteForNewVersionText();
}

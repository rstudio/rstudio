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
    String retryButtonLabel();

    /**
     * Translated "Licensing Limit Reached".
     *
     * @return translated "Licensing Limit Reached"
     */
    String licensingLimitCaption();

    /**
     * Translated "Please quit any unused running sessions and try again, or contact your administrator to update your license.".
     *
     * @return translated "Please quit any unused running sessions and try again, or contact your administrator to update your license."
     */
    String quitRunningSessionsMessage(String userMsg);

    /**
     * Translated "Unable to connect to service".
     *
     * @return translated "Unable to connect to service"
     */
    String unableToConnectMessage();

    /**
     * Translated "Error occurred during transmission".
     *
     * @return translated "Error occurred during transmission"
     */
    String errorTransmissionMessage();

    /**
     * Translated "Go Home".
     *
     * @return translated "Go Home"
     */
    String goHomeButtonLabel();

    /**
     * Translated "Cannot Connect to R Session".
     *
     * @return translated "Cannot Connect to R Session"
     */
    String cannotConnectRCaption();

    /**
     * Translated "Could not connect to the R session on RStudio Server.".
     *
     * @return translated "Could not connect to the R session on RStudio Server."
     */
    String cannotConnectRMessage(String userMessage, int errorCode);

    /**
     * Translated "RStudio Initialization Error".
     *
     * @return translated "RStudio Initialization Error"
     */
    String rStudioInitializationErrorCaption();

    /**
     * Translated "docs".
     *
     * @return translated "docs"
     */
    String helpUsingRStudioLinkName();

    /**
     * Translated "community-forum".
     *
     * @return translated "community-forum"
     */
    String communityForumLinkName();

    /**
     * Translated "support".
     *
     * @return translated "support"
     */
    String rStudioSupportLinkName();

    /**
     * Translated "Focused Element: ".
     *
     * @return translated "Focused Element: "
     */
    String focusedElementLabel();

    /**
     * Translated "Loading workspace".
     *
     * @return translated "Loading workspace"
     */
    String loadingWorkspaceMessage();

    /**
     * Translated "Saving workspace image".
     *
     * @return translated "Saving workspace image"
     */
    String savingWorkspaceImageMessage();

    /**
     * Translated "Backing up R session...".
     *
     * @return translated "Backing up R session..."
     */
    String backingUpRSessionMessage();

    /**
     * Translated "Restoring R session...".
     *
     * @return translated "Restoring R session..."
     */
    String restoringRSessionMessage();
    
    /**
     * Translated "Switch R Version".
     *
     * @return translated "Switch R Version"
     */
    String switchRVersionCaption();

    /**
     * Translated "The workspace was not restored".
     *
     * @return translated "The workspace was not restored"
     */
    String workspaceNotRestoredMessage();

    /**
     * Translated ", and startup scripts were not executed".
     *
     * @return translated ", and startup scripts were not executed"
     */
    String startupScriptsNotExecutedMessage();

    /**
     * Translated "Startup scripts were not executed.".
     *
     * @return translated "Startup scripts were not executed."
     */
    String startupScriptsErrorMessage();

    /**
     * Translated "This R session was started in safe mode. ".
     *
     * @return translated "This R session was started in safe mode. "
     */
    String rSessionSafeModeMessage();

    /**
     * Translated "Are you sure you want to terminate R?".
     *
     * @return translated "Are you sure you want to terminate R?"
     */
    String terminateRMessage();

    /**
     * Translated "R is not responding to your request to interrupt processing so to stop the current operation you may need to terminate R entirely.\n\n{0}\n\n Do you want to terminate R now?".
     *
     * @return translated "R is not responding to your request to interrupt processing so to stop the current operation you may need to terminate R entirely.\n\n{0}\n\n Do you want to terminate R now?"
     */
    String terminationDialog(String terminationMsg);

    /**
     * Translated "Terminate R".
     *
     * @return translated "Terminate R"
     */
    String terminateRCaption();

    /**
     * Translated "Terminating R will cause your R session to immediately abort. Active computations will be interrupted and unsaved source file changes and workspace objects will be discarded.".
     *
     * @return translated "Terminating R will cause your R session to immediately abort. Active computations will be interrupted and unsaved source file changes and workspace objects will be discarded."
     */
    String terminationConsequenceMessage();

    /**
     * Translated "The R session is currently busy.".
     *
     * @return translated "The R session is currently busy."
     */
    String rSessionCurrentlyBusyMessage();

    /**
     * Translated "The R session and the terminal are currently busy.".
     *
     * @return translated "The R session and the terminal are currently busy."
     */
    String rSessionTerminalBusyMessage();

    /**
     * Translated "The terminal is currently busy.".
     *
     * @return translated "The terminal is currently busy."
     */
    String terminalCurrentlyBusyMessage();

    /**
     * Translated "{0} Are you sure you want to quit?".
     *
     * @return translated "{0} Are you sure you want to quit?"
     */
    String applicationQuitMessage(String message);

    /**
     * Translated "Quit RStudio".
     *
     * @return translated "Quit RStudio"
     */
    String quitRStudio();

    /**
     * Translated "Are you sure you want to quit the R session?".
     *
     * @return translated "Are you sure you want to quit the R session?"
     */
    String quitRSessionsMessage();

    /**
     * Translated "Save workspace image to ".
     *
     * @return translated "Save workspace image to "
     */
    String saveWorkspaceImageMessage();

    /**
     * Translated "Quit R Session".
     *
     * @return translated "Quit R Session"
     */
    String quitRSessionTitle();

    /**
     * Translated "Restart R".
     *
     * @return translated "Restart R"
     */
    String restartRCaption();

    /**
     * Translated "Terminal jobs will be terminated. Are you sure?".
     *
     * @return translated "Terminal jobs will be terminated. Are you sure?"
     */
    String terminalJobTerminatedQuestion();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String progressErrorCaption();

    /**
     * Translated "Restarting R..".
     *
     * @return translated "Restarting R.."
     */
    String restartingRMessage();

    /**
     * Translated "Quit R Session".
     *
     * @return translated "Quit R Session"
     */
    String quitRSessionCaption();

    /**
     * Translated "Restarting RStudio".
     *
     * @return translated "Restarting RStudio"
     */
    String restartRStudio();

    /**
     * Translated "Workspace image (.RData)".
     *
     * @return translated "Workspace image (.RData)"
     */
    String studioClientTitle();

    /**
     * Translated "Switching to project ".
     *
     * @return translated "Switching to project "
     */
    String switchingToProjectMessage();

    /**
     * Translated "Closing project".
     *
     * @return translated "Closing project"
     */
    String closingProjectMessage();

    /**
     * Translated "Quitting R Session...".
     *
     * @return translated "Quitting R Session..."
     */
    String quitRSessionMessage();

    /**
     * Translated "server quitSession responded false".
     *
     * @return translated "server quitSession responded false"
     */
    String serverQuitSession();

    /**
     * Translated "Console cleared".
     *
     * @return translated "Console cleared"
     */
    String consoleClearedAnnouncement();

    /**
     * Translated "Console output (requires restart)".
     *
     * @return translated "Console output (requires restart)"
     */
    String consoleOutputAnnouncement();

    /**
     * Translated "Console command (requires restart)".
     *
     * @return translated "Console command (requires restart)"
     */
    String consoleCommandAnnouncement();

    /**
     * Translated "Filtered result count".
     *
     * @return translated "Filtered result count"
     */
    String filterResultCountAnnouncement();

    /**
     * Translated "Commit message length".
     *
     * @return translated "Commit message length"
     */
    String commitMessageLengthAnnouncement();

    /**
     * Translated "Inaccessible feature warning".
     *
     * @return translated "Inaccessible feature warning"
     */
    String inaccessibleWarningAnnouncement();

    /**
     * Translated "Info bars".
     *
     * @return translated "Info bars"
     */
    String infoBarsAnnouncement();

    /**
     * Translated "Task completion".
     *
     * @return translated "Task completion"
     */
    String taskCompletionAnnouncement();

    /**
     * Translated "Task progress details".
     *
     * @return translated "Task progress details"
     */
    String taskProgressAnnouncement();

    /**
     * Translated "Screen reader not enabled".
     *
     * @return translated "Screen reader not enabled"
     */
    String screenReaderAnnouncement();

    /**
     * Translated "Changes in session state".
     *
     * @return translated "Changes in session state"
     */
    String sessionStateAnnouncement();

    /**
     * Translated "Tab key focus mode change".
     *
     * @return translated "Tab key focus mode change"
     */
    String tabKeyFocusAnnouncement();

    /**
     * Translated "Toolbar visibility change".
     *
     * @return translated "Toolbar visibility change"
     */
    String toolBarVisibilityAnnouncement();

    /**
     * Translated "Warning bars".
     *
     * @return translated "Warning bars"
     */
    String warningBarsAnnouncement();

    /**
     * Translated "Session suspension"
     *
     * @return translated "Session suspension"
     */
    String sessionSuspendAnnouncement();

    /**
     * Translated "Unregistered live announcement: ".
     *
     * @return translated "Unregistered live announcement: "
     */
    String unregisteredLiveAnnouncementMessage();

    /**
     * Translated "Close Remote Session".
     *
     * @return translated "Close Remote Session"
     */
    String closeRemoteSessionCaption();

    /**
     * Translated "Do you want to close the remote session?".
     *
     * @return translated "Do you want to close the remote session?"
     */
    String closeRemoteSessionMessage();

    /**
     * Translated "Unable to obtain a license. Please restart RStudio to try again.".
     *
     * @return translated "Unable to obtain a license. Please restart RStudio to try again."
     */
    String licenseLostMessage();

    /**
     * Translated "Unable to find an active license. Please select a license file or restart RStudio to try again.".
     *
     * @return translated "Unable to find an active license. Please select a license file or restart RStudio to try again."
     */
    String unableToFindActiveLicenseMessage();

    /**
     * Translated "Active RStudio License Not Found".
     *
     * @return translated "Active RStudio License Not Found"
     */
    String activeRStudioLicenseNotFound();

    /**
     * Translated "Select License...".
     *
     * @return translated "Select License..."
     */
    String selectLicense();

    /**
     * Translated "Details: ".
     *
     * @return translated "Details: "
     */
    String detailsMessage();

    /**
     * Translated "Connection Received".
     *
     * @return translated "Connection Received"
     */
    String connectionReceivedType();

    /**
     * Translated "Connection Dequeued".
     *
     * @return translated "Connection Dequeued"
     */
    String connectionDequeuedType();

    /**
     * Translated "Connection Responded".
     *
     * @return translated "Connection Responded"
     */
    String connectionRespondedType();

    /**
     * Translated "Connection Terminated".
     *
     * @return translated "Connection Terminated"
     */
    String connectionTerminatedType();

    /**
     * Translated "Connection Error".
     *
     * @return translated "Connection Error"
     */
    String connectionErrorType();

    /**
     * Translated "(Unknown).
     *
     * @return translated "(Unknown)"
     */
    String connectionUnknownType();

    /**
     * Translated "RStudio{0}".
     *
     * @return translated "RStudio{0}"
     */
    String rStudioEditionName(String desktop);

    /**
     * Translated "Server".
     *
     * @return translated "Server"
     */
    String serverLabel();

    /**
     * Translated "Copy Version".
     *
     * @return translated "Copy Version"
     */
    String copyVersionButtonTitle();

    /**
     * Translated "About {0}".
     *
     * @return translated "About {0}"
     */
    String title(String version);

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    String okBtn();

    /**
     * Translated "Manage License...".
     *
     * @return translated "Manage License..."
     */
    String manageLicenseBtn();

    /**
     * Translated "for ".
     *
     * @return translated "for "
     */
    String forText();

    /**
     * Translated "Version Copied".
     *
     * @return translated "Version Copied"
     */
    String versionCopiedText();

    /**
     * Translated "Version information copied to clipboard.".
     *
     * @return translated "Version information copied to clipboard."
     */
    String versionInformationCopiedText();

    /**
     * Translated "Copy Version".
     *
     * @return translated "Copy Version"
     */
    String copyVersionButton();

    /**
     * Translated "Build ".
     *
     * @return translated "Build "
     */
    String versionBuildLabel();

    /**
     * Translated ") for ".
     *
     * @return translated ") for "
     */
    String buildLabelForText();

    /**
     * Translated "This ".
     *
     * @return translated "This "
     */
    String buildTypeThisText();

    /**
     * Translated "build of ".
     *
     * @return translated "build of "
     */
    String buildOfText();

    /**
     * Translated "is provided by Posit Software, PBC for testing purposes only and is not an officially supported release.".
     *
     * @return translated "is provided by Posit Software, PBC for testing purposes only and is not an officially supported release."
     */
    String supportNoticeText();

    /**
     * Translated "Loading...".
     *
     * @return translated "Loading..."
     */
    String licenseBoxLoadingText();

    /**
     * Translated "Open Source Components".
     *
     * @return translated "Open Source Components"
     */
    String openSourceComponentsText();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    String closeButtonText();

    /**
     * Translated "Open Source Component".
     *
     * @return translated "Open Source Component"
     */
    String openSourceComponentText();

    /**
     * Translated "Main toolbar visible".
     *
     * @return translated "Main toolbar visible"
     */
    String toolbarVisibleText();

    /**
     * Translated "Main toolbar hidden".
     *
     * @return translated "Main toolbar hidden"
     */
    String toolbarHiddenText();

    /**
     * Translated "Toolbar hidden, unable to focus.".
     *
     * @return translated "Toolbar hidden, unable to focus."
     */
    String focusToolbarText();

    /**
     * Translated "Application Updated".
     *
     * @return translated "Application Updated"
     */
    String applicationUpdatedCaption();

    /**
     * Translated "An updated version of RStudio is available. Your browser will now be refreshed with the new version. All current work and data will be preserved during the update.".
     *
     * @return translated "An updated version of RStudio is available. Your browser will now be refreshed with the new version. All current work and data will be preserved during the update."
     */
    String applicationUpdatedMessage();

    /**
     * Translated "Warning bar".
     *
     * @return translated "Warning bar"
     */
    String warningBarText();

    /**
     * Translated "R Session Error".
     *
     * @return translated "R Session Error"
     */
    String rSessionErrorCaption();

    /**
     * Translated "The previous R session was abnormally terminated due to an unexpected crash.\n\nYou may have lost workspace data as a result of this crash.".
     *
     * @return translated "The previous R session was abnormally terminated due to an unexpected crash.\n\nYou may have lost workspace data as a result of this crash."
     */
    String previousRSessionsMessage();

    /**
     * Translated "Main".
     *
     * @return translated "Main"
     */
    String mainLabel();

    /**
     * Translated "New File".
     *
     * @return translated "New File"
     */
    String newFileTitle();

    /**
     * Translated "Open recent files".
     *
     * @return translated "Open recent files"
     */
    String openRecentFilesTitle();

    /**
     * Translated "Version control".
     *
     * @return translated "Version control"
     */
    String versionControlTitle();

    /**
     * Translated "Workspace Panes".
     *
     * @return translated "Workspace Panes"
     */
    String workspacePanesTitle();

    /**
     * Translated "Project: (None)".
     *
     * @return translated "Project: (None)"
     */
    String toolBarButtonText();

    /**
     * Translated "Looking for projects...".
     *
     * @return translated "Looking for projects..."
     */
    String popupMenuProgressMessage();

    /**
     * Translated "Recent Projects".
     *
     * @return translated "Recent Projects"
     */
    String recentProjectsLabel();

    /**
     * Translated "Shared with Me".
     *
     * @return translated "Shared with Me"
     */
    String sharedWithMeLabel();

    /**
     * Translated "Export".
     *
     * @return translated "Export"
     */
    String exportCaption();

    /**
     * Translated "Import".
     *
     * @return translated "Import"
     */
    String importCaption();

    /**
     * Translated "Reloading...".
     *
     * @return translated "Reloading..."
     */
    String reloadingText();

    /**
     * Translated "Retrying in Safe Mode...".
     *
     * @return translated "Retrying in Safe Mode..."
     */
    String retryInSafeModeText();

    /**
     * Translated "Terminating R...".
     *
     * @return translated "Terminating R..."
     */
    String terminatingRText();

    /**
     * Translated "Default version of R:".
     *
     * @return translated "Default version of R:"
     */
    String defaultVersionRCaption();

    /**
     * Translated "Help on R versions".
     *
     * @return translated "Help on R versions"
     */
    String helpOnRVersionsTitle();

    /**
     * Translated "Module ".
     *
     * @return translated "Module "
     */
    String moduleText();

    /**
     * Translated "R version ".
     *
     * @return translated "R version "
     */
    String rVersionText();

    /**
     * Translated "(Use System Default)".
     *
     * @return translated "(Use System Default)"
     */
    String useSystemDefaultText();

    /**
     * Translated "User-specified...".
     *
     * @return translated "User-specified..."
     */
    String userSpecifiedText();

    /**
     * Translated "Manage License...".
     *
     * @return translated "Manage License..."
     */
    String manageLicenseText();

    /**
     * Translated "Addins".
     *
     * @return translated "Addins"
     */
    String addinsText();

    /**
     * Translated "Search for addins".
     *
     * @return translated "Search for addins"
     */
    String searchForAddinsLabel();

    /**
     * Translated "No addins found".
     *
     * @return translated "No addins found"
     */
    String noAddinsFound();

    /**
     * Translated "R encountered a fatal error.".
     *
     * @return translated "R encountered a fatal error."
     */
    String rFatalErrorMessage();

    /**
     * Translated "R encountered a fatal error.".
     *
     * @return translated "The session was terminated."
     */
    String sessionTerminatedMessage();

    /**
     * Translated "This browser was disconnected from the R session because another browser connected (only one browser at a time may be connected to an RStudio session). You may reconnect using the button below.".
     *
     * @return translated "This browser was disconnected from the R session because another browser connected (only one browser at a time may be connected to an RStudio session). You may reconnect using the button below."
     */
    String browserDisconnectedMessage();

    /**
     * Translated "RStudio is temporarily offline due to system maintenance. We apologize for the inconvenience, please try again in a few minutes.".
     *
     * @return translated "RStudio is temporarily offline due to system maintenance. We apologize for the inconvenience, please try again in a few minutes."
     */
    String rStudioOfflineMessage();

    /**
     * Translated "R Session Ended".
     *
     * @return translated "R Session Ended"
     */
    String rSessionEndedCaption();

    /**
     * Translated "Start New Session".
     *
     * @return translated "Start New Session"
     */
    String startNewSessionText();

    /**
     * Translated "R Session Aborted".
     *
     * @return translated "R Session Aborted"
     */
    String rSessionAbortedCaption();

    /**
     * Translated "R Session Disconnected".
     *
     * @return translated "R Session Disconnected"
     */
    String rSessionDisconnectedCaption();

    /**
     * Translated "Reconnect".
     *
     * @return translated "Reconnect"
     */
    String reconnectButtonText();

    /**
     * Translated "RStudio Temporarily Offline".
     *
     * @return translated "RStudio Temporarily Offline"
     */
    String temporarilyOfflineCaption();

    /**
     * Translated "Unknown mode ".
     *
     * @return translated "Unknown mode "
     */
    String unknownModeText();

    /**
     * Translated "Sign out".
     *
     * @return translated "Sign out"
     */
    String signOutButtonText();

    /**
     * Translated "Error Opening Devtools".
     *
     * @return translated "Error Opening Devtools"
     */
    String errorOpeningDevToolsCaption();

    /**
     * Translated "The Chromium devtools server could not be activated.".
     *
     * @return translated "The Chromium devtools server could not be activated."
     */
    String cannotActivateDevtoolsMessage();

    /**
     * Translated "Error Checking for Updates".
     *
     * @return translated "Error Checking for Updates"
     */
    String errorCheckingUpdatesMessage();

    /**
     * Translated "An error occurred while checking for updates: ".
     *
     * @return translated "An error occurred while checking for updates: "
     */
    String errorOccurredCheckingUpdatesMessage();

    /**
     * Translated "Quit and Download...".
     *
     * @return translated "Quit and Download..."
     */
    String quitDownloadButtonLabel();

    /**
     * Translated "Update RStudio".
     *
     * @return translated "Update RStudio"
     */
    String updateRStudioCaption();

    /**
     * Translated "Remind Later".
     *
     * @return translated "Remind Later"
     */
    String remindLaterButtonLabel();

    /**
     * Translated "Ignore Update".
     *
     * @return translated "Ignore Update"
     */
    String ignoreUpdateButtonLabel();

    /**
     * Translated "Update Available".
     *
     * @return translated "Update Available"
     */
    String updateAvailableCaption();

    /**
     * Translated "No Update Available".
     *
     * @return translated "No Update Available"
     */
    String noUpdateAvailableCaption();

    /**
     * Translated "You're using the newest version of RStudio.".
     *
     * @return translated "You're using the newest version of RStudio."
     */
    String usingNewestVersionMessage();

    /**
     * Translated "Posit Workbench".
     *
     * @return translated "Posit Workbench"
     */
    String rStudioServerHomeTitle();

    /**
     * Translated "Your browser does not allow access to your".
     *
     * @return translated "Your browser does not allow access to your"
     */
    String browserNotAllowAccessLabel();

    /**
     * Translated "computer's clipboard. As a result you must".
     *
     * @return translated "computer's clipboard. As a result you must"
     */
    String computerClipBoardLabel();

    /**
     * Translated "use keyboard shortcuts for:".
     *
     * @return translated "use keyboard shortcuts for:"
     */
    String useKeyboardShortcutsLabel();

    /**
     * Translated "Use Keyboard Shortcut".
     *
     * @return translated "Use Keyboard Shortcut"
     */
    String useKeyboardShortcutCaption();

    /**
     * Translated "Sign out".
     *
     * @return translated "Sign out"
     */
    String signOutTitle();

    /**
     * Translated "from ".
     *
     * @return translated "from "
     */
    String fromText();

    /**
     * Translated "to ".
     *
     * @return translated "to "
     */
    String toText();

    /**
     * Translated "New Session...".
     *
     * @return translated "New Session..."
     */
    String newSessionMenuLabel();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    String saveYesLabel();

    /**
     * Translated "Don't Save".
     *
     * @return translated "Don't Save"
     */
    String saveNoLabel();

    /**
     * Translated "(active)".
     *
     * @return translated "(active)"
     */
    String activeText();

    /**
     * Translated "<p>Click on a request to see details. Click on the background to show these instructions again.</p><h4>Available commands:</h4><ul><li>Esc: Close</li><li>P: Play/pause</li><li>E: Export</li><li>I: Import</li><li>+/-: Zoom in/out</li></ul>".
     *
     * @return translated "<p>Click on a request to see details. Click on the background to show these instructions again.</p><h4>Available commands:</h4><ul><li>Esc: Close</li><li>P: Play/pause</li><li>E: Export</li><li>I: Import</li><li>+/-: Zoom in/out</li></ul>"
     */
    String requestLogVisualization();

    /**
     * Translated "Please visit https://posit.co/download/rstudio-desktop/ to check if a new version is available.".
     *
     * @return translated "Please visit https://posit.co/download/rstudio-desktop/ to check if a new version is available."
     */
    String visitWebsiteForNewVersionText();

    /**
     * Translated "Automatic update notifications were disabled for {0}.".
     *
     * @return translated "Automatic update notifications were disabled for {0}."
     */
    String updateDisabledForVersionText(String version);

    /**
     * Translated "Stop Ignoring Updates".
     *
     * @return translated "Stop Ignoring Updates"
     */
    String stopIgnoringUpdatesButtonLabel();

    /**
     * Translated "RStudio will automatically check for updates the next time it starts.".
     *
     * @return translated "RStudio will automatically check for updates the next time it starts."
     */
    String autoUpdateReenabledMessage();

    /**
     * Translated "Update No Longer Ignored".
     *
     * @return translated "Update No Longer Ignored"
     */
    String autoUpdateReenabledCaption();

    /**
     * Translated "Danger!".
     *
     * @return translated "Danger!"
     */
    String reallyCrashCaption();

    /**
     * Translated "This will cause RStudio to immediately crash. You may lose work. Trigger crash?".
     *
     * @return translated "This will cause RStudio to immediately crash. You may lose work. Trigger crash?"
     */
    String reallyCrashMessage();

    /**
     * Translated "Session memory limit exceeded. Restart required."
     *
     * @return translated "Session memory limit exceeded. Restart required."
     */
    String memoryLimitExceededCaption();

    /**
     * Translated "Memory Limit Exceeded. Save files and restart session.\n\nSession may be aborted if system runs low on memory."
     *
     * @return translated "Memory Limit Exceeded. Save files and restart session.\n\nSession may be aborted if system runs low on memory."
     */
    String memoryLimitExceededMessage();

    /**
     * Translated "Memory limit has been exceeded. The IDE session has been terminated."
     *
     * @return translated "Memory limit has been exceeded. The IDE session has been terminated."
     */
    String memoryLimitAbortedMessage();

    /**
     * Translated "Approaching session memory limit."
     *
     * @return translated "Approaching session memory limit."
     */
    String approachingMemoryLimit();

    /**
     * Translated "Over session memory limit."
     *
     * @return translated "Over session memory limit."
     */
    String overMemoryLimit();

    /*
     * Translated "Posit Workbench Login Required".
     *
     * @return translated "Posit Workbench Login Required"
     */
    String workbenchLoginRequired();

    /**
     * Translated "RStudio Server Login Required".
     *
     * @return translated "RStudio Server Login Required"
     */
    String serverLoginRequired();

    /**
     * Translated "Login expired or signed out from another window.\nSelect 'Login' for a new login tab. Return here to resume session.".
     *
     * @return translated "Login expired or signed out from another window.\nSelect 'Login' for a new login tab. Return here to resume session."
     */
    String workbenchLoginRequiredMessage();

    /**
     * Translated "Login expired or signed out from another window.\nSelect 'Login' for a new login tab.".
     *
     * @return translated "Login expired or signed out from another window.\nSelect 'Login' for a new login tab."
     */
    String serverLoginRequiredMessage();

    /**
     * Translated "Login".
     *
     * @return translated "Login"
     */
    String loginButton();
}

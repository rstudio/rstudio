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

    @DefaultMessage("Retry")
    @Key("retryButtonLabel")
    String retryButtonLabel();

    @DefaultMessage("Licensing Limit Reached")
    @Key("licensingLimitCaption")
    String licensingLimitCaption();

    @DefaultMessage("{0}\\n\\n Please quit any unused running sessions and try again, or contact your administrator to update your license.")
    @Key("quitRunningSessionsMessage")
    String quitRunningSessionsMessage(String userMsg);

    @DefaultMessage("Unable to connect to service")
    @Key("unableToConnectMessage")
    String unableToConnectMessage();

    @DefaultMessage("Error occurred during transmission")
    @Key("errorTransmissionMessage")
    String errorTransmissionMessage();

    @DefaultMessage("Go Home")
    @Key("goHomeButtonLabel")
    String goHomeButtonLabel();

    @DefaultMessage("Cannot Connect to R Session")
    @Key("cannotConnectRCaption")
    String cannotConnectRCaption();

    @DefaultMessage("Could not connect to the R session on RStudio Server.\n\n{0} ({1})")
    @Key("cannotConnectRMessage")
    String cannotConnectRMessage(String userMessage, int errorCode);

    @DefaultMessage("RStudio Initialization Error")
    @Key("rStudioInitializationErrorCaption")
    String rStudioInitializationErrorCaption();

    @DefaultMessage("docs")
    @Key("helpUsingRStudioLinkName")
    String helpUsingRStudioLinkName();

    @DefaultMessage("community-forum")
    @Key("communityForumLinkName")
    String communityForumLinkName();

    @DefaultMessage("support")
    @Key("rStudioSupportLinkName")
    String rStudioSupportLinkName();

    @DefaultMessage("Focused Element: ")
    @Key("focusedElementLabel")
    String focusedElementLabel();

    @DefaultMessage("Loading workspace")
    @Key("loadingWorkspaceMessage")
    String loadingWorkspaceMessage();

    @DefaultMessage("Saving workspace image")
    @Key("savingWorkspaceImageMessage")
    String savingWorkspaceImageMessage();

    @DefaultMessage("Backing up R session...")
    @Key("backingUpRSessionMessage")
    String backingUpRSessionMessage();

    @DefaultMessage("Restoring R session...")
    @Key("restoringRSessionMessage")
    String restoringRSessionMessage();
    

    @DefaultMessage("Switch R Version")
    @Key("switchRVersionCaption")
    String switchRVersionCaption();

    @DefaultMessage("The workspace was not restored")
    @Key("workspaceNotRestoredMessage")
    String workspaceNotRestoredMessage();

    @DefaultMessage(", and startup scripts were not executed")
    @Key("startupScriptsNotExecutedMessage")
    String startupScriptsNotExecutedMessage();

    @DefaultMessage("Startup scripts were not executed.")
    @Key("startupScriptsErrorMessage")
    String startupScriptsErrorMessage();

    @DefaultMessage("This R session was started in safe mode. ")
    @Key("rSessionSafeModeMessage")
    String rSessionSafeModeMessage();

    @DefaultMessage("Are you sure you want to terminate R?")
    @Key("terminateRMessage")
    String terminateRMessage();

    @DefaultMessage("R is not responding to your request to interrupt processing so to stop the current operation you may need to terminate R entirely.\\n\\n{0}\\n\\n Do you want to terminate R now?")
    @Key("terminationDialog")
    String terminationDialog(String terminationMsg);

    @DefaultMessage("Terminate R")
    @Key("terminateRCaption")
    String terminateRCaption();

    @DefaultMessage("Terminating R will cause your R session to immediately abort. Active computations will be interrupted and unsaved source file changes and workspace objects will be discarded.")
    @Key("terminationConsequenceMessage")
    String terminationConsequenceMessage();

    @DefaultMessage("The R session is currently busy.")
    @Key("rSessionCurrentlyBusyMessage")
    String rSessionCurrentlyBusyMessage();

    @DefaultMessage("The R session and the terminal are currently busy.")
    @Key("rSessionTerminalBusyMessage")
    String rSessionTerminalBusyMessage();

    @DefaultMessage("The terminal is currently busy.")
    @Key("terminalCurrentlyBusyMessage")
    String terminalCurrentlyBusyMessage();

    @DefaultMessage("{0} Are you sure you want to quit?")
    @Key("applicationQuitMessage")
    String applicationQuitMessage(String message);

    @DefaultMessage("Quit RStudio")
    @Key("quitRStudio")
    String quitRStudio();

    @DefaultMessage("Are you sure you want to quit the R session?")
    @Key("quitRSessionsMessage")
    String quitRSessionsMessage();

    @DefaultMessage("Save workspace image to ")
    @Key("saveWorkspaceImageMessage")
    String saveWorkspaceImageMessage();

    @DefaultMessage("Quit R Session")
    @Key("quitRSessionTitle")
    String quitRSessionTitle();

    @DefaultMessage("Restart R")
    @Key("restartRCaption")
    String restartRCaption();

    @DefaultMessage("Terminal jobs will be terminated. Are you sure?")
    @Key("terminalJobTerminatedQuestion")
    String terminalJobTerminatedQuestion();

    @DefaultMessage("Error")
    @Key("progressErrorCaption")
    String progressErrorCaption();

    @DefaultMessage("Restarting R..")
    @Key("restartingRMessage")
    String restartingRMessage();

    @DefaultMessage("Quit R Session")
    @Key("quitRSessionCaption")
    String quitRSessionCaption();

    @DefaultMessage("Restarting RStudio")
    @Key("restartRStudio")
    String restartRStudio();

    @DefaultMessage("Workspace image (.RData)")
    @Key("studioClientTitle")
    String studioClientTitle();

    @DefaultMessage("Switching to project ")
    @Key("switchingToProjectMessage")
    String switchingToProjectMessage();

    @DefaultMessage("Closing project")
    @Key("closingProjectMessage")
    String closingProjectMessage();

    @DefaultMessage("Quitting R Session...")
    @Key("quitRSessionMessage")
    String quitRSessionMessage();

    @DefaultMessage("server quitSession responded false")
    @Key("serverQuitSession")
    String serverQuitSession();

    @DefaultMessage("Console cleared")
    @Key("consoleClearedAnnouncement")
    String consoleClearedAnnouncement();

    @DefaultMessage("Console output (requires restart)")
    @Key("consoleOutputAnnouncement")
    String consoleOutputAnnouncement();

    @DefaultMessage("Console command (requires restart)")
    @Key("consoleCommandAnnouncement")
    String consoleCommandAnnouncement();

    @DefaultMessage("Filtered result count")
    @Key("filterResultCountAnnouncement")
    String filterResultCountAnnouncement();

    @DefaultMessage("Commit message length")
    @Key("commitMessageLengthAnnouncement")
    String commitMessageLengthAnnouncement();

    @DefaultMessage("Inaccessible feature warning")
    @Key("inaccessibleWarningAnnouncement")
    String inaccessibleWarningAnnouncment();

    @DefaultMessage("Info bars")
    @Key("infoBarsAnnouncement")
    String infoBarsAnnouncment();

    @DefaultMessage("Task completion")
    @Key("taskCompletionAnnouncement")
    String taskCompletionAnnouncement();

    @DefaultMessage("Task progress details")
    @Key("taskProgressAnnouncement")
    String taskProgressAnnouncement();

    @DefaultMessage("Screen reader not enabled")
    @Key("screenReaderAnnouncement")
    String screenReaderAnnouncement();

    @DefaultMessage("Changes in session state")
    @Key("sessionStateAnnouncement")
    String sessionStateAnnouncement();

    @DefaultMessage("Tab key focus mode change")
    @Key("tabKeyFocusAnnouncement")
    String tabKeyFocusAnnouncement();

    @DefaultMessage("Toolbar visibility change")
    @Key("toolBarVisibilityAnnouncement")
    String toolBarVisibilityAnnouncement();

    @DefaultMessage("Warning bars")
    @Key("warningBarsAnnouncement")
    String warningBarsAnnouncement();

    @DefaultMessage("Session suspension")
    @Key("sessionSuspendAnnouncement")
    String sessionSuspendAnnouncement();

    @DefaultMessage("Unregistered live announcement: ")
    @Key("unregisteredLiveAnnouncementMessage")
    String unregisteredLiveAnnouncementMessage();

    @DefaultMessage("Close Remote Session")
    @Key("closeRemoteSessionCaption")
    String closeRemoteSessionCaption();

    @DefaultMessage("Do you want to close the remote session?")
    @Key("closeRemoteSessionMessage")
    String closeRemoteSessionMessage();

    @DefaultMessage("Unable to obtain a license. Please restart RStudio to try again.")
    @Key("licenseLostMessage")
    String licenseLostMessage();

    @DefaultMessage("Unable to find an active license. Please select a license file or restart RStudio to try again.")
    @Key("unableToFindActiveLicenseMessage")
    String unableToFindActiveLicenseMessage();

    @DefaultMessage("Active RStudio License Not Found")
    @Key("activeRStudioLicenseNotFound")
    String activeRStudioLicenseNotFound();

    @DefaultMessage("Select License...")
    @Key("selectLicense")
    String selectLicense();

    @DefaultMessage("Details: ")
    @Key("detailsMessage")
    String detailsMessage();

    @DefaultMessage("Connection Received")
    @Key("connectionReceivedType")
    String connectionReceivedType();

    @DefaultMessage("Connection Dequeued")
    @Key("connectionDequeuedType")
    String connectionDequeuedType();

    @DefaultMessage("Connection Responded")
    @Key("connectionRespondedType")
    String connectionRespondedType();

    @DefaultMessage("Connection Terminated")
    @Key("connectionTerminatedType")
    String connectionTerminatedType();

    @DefaultMessage("Connection Error")
    @Key("connectionErrorType")
    String connectionErrorType();

    @DefaultMessage("(Unknown)")
    @Key("connectionUnknownType")
    String connectionUnknownType();

    @DefaultMessage("RStudio{0}")
    @Key("rStudioEditionName")
    String rStudioEditionName(String desktop);

    @DefaultMessage("Server")
    @Key("serverLabel")
    String serverLabel();

    @DefaultMessage("Copy Version")
    @Key("copyVersionButtonTitle")
    String copyVersionButtonTitle();

    @DefaultMessage("About {0}")
    @Key("title")
    String title(String version);

    @DefaultMessage("OK")
    @Key("okBtn")
    String okBtn();

    @DefaultMessage("Manage License...")
    @Key("manageLicenseBtn")
    String manageLicenseBtn();

    @DefaultMessage("for ")
    @Key("forText")
    String forText();

    @DefaultMessage("Version Copied")
    @Key("versionCopiedText")
    String versionCopiedText();

    @DefaultMessage("Version information copied to clipboard.")
    @Key("versionInformationCopiedText")
    String versionInformationCopiedText();

    @DefaultMessage("Copy Version")
    @Key("copyVersionButton")
    String copyVersionButton();

    @DefaultMessage("Build ")
    @Key("versionBuildLabel")
    String versionBuildLabel();

    @DefaultMessage(") for ")
    @Key("buildLabelForText")
    String buildLabelForText();

    @DefaultMessage("This ")
    @Key("buildTypeThisText")
    String buildTypeThisText();

    @DefaultMessage("build of ")
    @Key("buildOfText")
    String buildOfText();

    @DefaultMessage("is provided by Posit Software, PBC for testing purposes only and is not an officially supported release.")
    @Key("supportNoticeText")
    String supportNoticeText();

    @DefaultMessage("Loading...")
    @Key("licenseBoxLoadingText")
    String licenseBoxLoadingText();

    @DefaultMessage("Open Source Components")
    @Key("openSourceComponentsText")
    String openSourceComponentsText();

    @DefaultMessage("Close")
    @Key("closeButtonText")
    String closeButtonText();

    @DefaultMessage("Open Source Component")
    @Key("openSourceComponentText")
    String openSourceComponentText();

    @DefaultMessage("Main toolbar visible")
    @Key("toolbarVisibleText")
    String toolbarVisibleText();

    @DefaultMessage("Main toolbar hidden")
    @Key("toolbarHiddenText")
    String toolbarHiddenText();

    @DefaultMessage("Toolbar hidden, unable to focus.")
    @Key("focusToolbarText")
    String focusToolbarText();

    @DefaultMessage("Application Updated")
    @Key("applicationUpdatedCaption")
    String applicationUpdatedCaption();

    @DefaultMessage("An updated version of RStudio is available. Your browser will now be refreshed with the new version. All current work and data will be preserved during the update.")
    @Key("applicationUpdatedMessage")
    String applicationUpdatedMessage();

    @DefaultMessage("Warning bar")
    @Key("warningBarText")
    String warningBarText();

    @DefaultMessage("R Session Error")
    @Key("rSessionErrorCaption")
    String rSessionErrorCaption();

    @DefaultMessage("The previous R session was abnormally terminated due to an unexpected crash.\\n\\nYou may have lost workspace data as a result of this crash.")
    @Key("previousRSessionsMessage")
    String previousRSessionsMessage();

    @DefaultMessage("Main")
    @Key("mainLabel")
    String mainLabel();

    @DefaultMessage("New File")
    @Key("newFileTitle")
    String newFileTitle();

    @DefaultMessage("Open recent files")
    @Key("openRecentFilesTitle")
    String openRecentFilesTitle();

    @DefaultMessage("Version control")
    @Key("versionControlTitle")
    String versionControlTitle();

    @DefaultMessage("Workspace Panes")
    @Key("workspacePanesTitle")
    String workspacePanesTitle();

    @DefaultMessage("Project: (None)")
    @Key("toolBarButtonText")
    String toolBarButtonText();

    @DefaultMessage("Looking for projects...")
    @Key("popupMenuProgressMessage")
    String popupMenuProgressMessage();

    @DefaultMessage("Recent Projects")
    @Key("recentProjectsLabel")
    String recentProjectsLabel();

    @DefaultMessage("Shared with Me")
    @Key("sharedWithMeLabel")
    String sharedWithMeLabel();

    @DefaultMessage("Export")
    @Key("exportCaption")
    String exportCaption();

    @DefaultMessage("Import")
    @Key("importCaption")
    String importCaption();

    @DefaultMessage("Reloading...")
    @Key("reloadingText")
    String reloadingText();

    @DefaultMessage("Retrying in Safe Mode...")
    @Key("retryInSafeModeText")
    String retryInSafeModeText();

    @DefaultMessage("Terminating R...")
    @Key("terminatingRText")
    String terminatingRText();

    @DefaultMessage("Default version of R:")
    @Key("defaultVersionRCaption")
    String defaultVersionRCaption();

    @DefaultMessage("Help on R versions")
    @Key("helpOnRVersionsTitle")
    String helpOnRVersionsTitle();

    @DefaultMessage("Module ")
    @Key("moduleText")
    String moduleText();

    @DefaultMessage("R version ")
    @Key("rVersionText")
    String rVersionText();

    @DefaultMessage("(Use System Default)")
    @Key("useSystemDefaultText")
    String useSystemDefaultText();

    @DefaultMessage("User-specified...")
    @Key("userSpecifiedText")
    String userSpecifiedText();

    @DefaultMessage("Manage License...")
    @Key("manageLicenseText")
    String manageLicenseText();

    @DefaultMessage("Addins")
    @Key("addinsText")
    String addinsText();

    @DefaultMessage("Search for addins")
    @Key("searchForAddinsLabel")
    String searchForAddinsLabel();

    @DefaultMessage("No addins found")
    @Key("noAddinsFound")
    String noAddinsFound();

    @DefaultMessage("R encountered a fatal error.")
    @Key("rFatalErrorMessage")
    String rFatalErrorMessage();

    @DefaultMessage("The session was terminated.")
    @Key("sessionTerminatedMessage")
    String sessionTerminatedMessage();

    @DefaultMessage("This browser was disconnected from the R session because another browser connected (only one browser at a time may be connected to an RStudio session). You may reconnect using the button below.")
    @Key("browserDisconnectedMessage")
    String browserDisconnectedMessage();

    @DefaultMessage("RStudio is temporarily offline due to system maintenance. We apologize for the inconvenience, please try again in a few minutes.")
    @Key("rStudioOfflineMessage")
    String rStudioOfflineMessage();

    @DefaultMessage("R Session Ended")
    @Key("rSessionEndedCaption")
    String rSessionEndedCaption();

    @DefaultMessage("Start New Session")
    @Key("startNewSessionText")
    String startNewSessionText();

    @DefaultMessage("R Session Aborted")
    @Key("rSessionAbortedCaption")
    String rSessionAbortedCaption();

    @DefaultMessage("R Session Disconnected")
    @Key("rSessionDisconnectedCaption")
    String rSessionDisconnectedCaption();

    @DefaultMessage("Reconnect")
    @Key("reconnectButtonText")
    String reconnectButtonText();

    @DefaultMessage("RStudio Temporarily Offline")
    @Key("temporarilyOfflineCaption")
    String temporarilyOfflineCaption();

    @DefaultMessage("Unknown mode ")
    @Key("unknownModeText")
    String unknownModeText();

    @DefaultMessage("Sign out")
    @Key("signOutButtonText")
    String signOutButtonText();

    @DefaultMessage("Error Opening Devtools")
    @Key("errorOpeningDevToolsCaption")
    String errorOpeningDevToolsCaption();

    @DefaultMessage("The Chromium devtools server could not be activated.")
    @Key("cannotActivateDevtoolsMessage")
    String cannotActivateDevtoolsMessage();

    @DefaultMessage("Error Checking for Updates")
    @Key("errorCheckingUpdatesMessage")
    String errorCheckingUpdatesMessage();

    @DefaultMessage("An error occurred while checking for updates: ")
    @Key("errorOccurredCheckingUpdatesMessage")
    String errorOccurredCheckingUpdatesMessage();

    @DefaultMessage("Quit and Download...")
    @Key("quitDownloadButtonLabel")
    String quitDownloadButtonLabel();

    @DefaultMessage("Update RStudio")
    @Key("updateRStudioCaption")
    String updateRStudioCaption();

    @DefaultMessage("Remind Later")
    @Key("remindLaterButtonLabel")
    String remindLaterButtonLabel();

    @DefaultMessage("Ignore Update")
    @Key("ignoreUpdateButtonLabel")
    String ignoreUpdateButtonLabel();

    @DefaultMessage("Update Available")
    @Key("updateAvailableCaption")
    String updateAvailableCaption();

    @DefaultMessage("No Update Available")
    @Key("noUpdateAvailableCaption")
    String noUpdateAvailableCaption();

    @DefaultMessage("You''re using the newest version of RStudio.")
    @Key("usingNewestVersionMessage")
    String usingNewestVersionMessage();

    @DefaultMessage("Posit Workbench")
    @Key("rStudioServerHomeTitle")
    String rStudioServerHomeTitle();

    @DefaultMessage("Your browser does not allow access to your")
    @Key("browserNotAllowAccessLabel")
    String browserNotAllowAccessLabel();

    @DefaultMessage("computer''s clipboard. As a result you must")
    @Key("computerClipBoardLabel")
    String computerClipBoardLabel();

    @DefaultMessage("use keyboard shortcuts for:")
    @Key("useKeyboardShortcutsLabel")
    String useKeyboardShortcutsLabel();

    @DefaultMessage("Use Keyboard Shortcut")
    @Key("useKeyboardShortcutCaption")
    String useKeyboardShortcutCaption();

    @DefaultMessage("Sign out")
    @Key("signOutTitle")
    String signOutTitle();

    @DefaultMessage("from ")
    @Key("fromText")
    String fromText();

    @DefaultMessage("to ")
    @Key("toText")
    String toText();

    @DefaultMessage("New Session...")
    @Key("newSessionMenuLabel")
    String newSessionMenuLabel();

    @DefaultMessage("Save")
    @Key("saveYesLabel")
    String saveYesLabel();

    @DefaultMessage("Don''t Save")
    @Key("saveNoLabel")
    String saveNoLabel();

    @DefaultMessage("(active)")
    @Key("activeText")
    String activeText();

    @DefaultMessage("<p>Click on a request to see details. Click on the background to show these instructions again.</p><h4>Available commands:</h4><ul><li>Esc: Close</li><li>P: Play/pause</li><li>E: Export</li><li>I: Import</li><li>+/-: Zoom in/out</li></ul>")
    @Key("requestLogVisualization")
    String requestLogVisualization();

    @DefaultMessage("Please visit https://posit.co/download/rstudio-desktop/ to check if a new version is available.")
    @Key("visitWebsiteForNewVersion")
    String visitWebsiteForNewVersionText();

    @DefaultMessage("Automatic update notifications were disabled for {0}.")
    @Key("updateDisabledForVersionText")
    String updateDisabledForVersionText(String version);

    @DefaultMessage("Stop Ignoring Updates")
    @Key("stopIgnoringUpdatesButtonLabel")
    String stopIgnoringUpdatesButtonLabel();

    @DefaultMessage("RStudio will automatically check for updates the next time it starts.")
    @Key("autoUpdateReenabledMessage")
    String autoUpdateReenabledMessage();

    @DefaultMessage("Update No Longer Ignored")
    @Key("autoUpdateReenabledCaption")
    String autoUpdateReenabledCaption();

    @DefaultMessage("Danger!")
    @Key("reallyCrashCaption")
    String reallyCrashCaption();

    @DefaultMessage("This will cause RStudio to immediately crash. You may lose work. Trigger crash?")
    @Key("reallyCrashMessage")
    String reallyCrashMessage();

    @DefaultMessage("Session memory limit exceeded. Restart required.")
    @Key("memoryLimitExceededCaption")
    String memoryLimitExceededCaption();

    @DefaultMessage("Memory Limit Exceeded. Save files and restart session.\n\nSession may be aborted if system runs low on memory.")
    @Key("memoryLimitExceededMessage")
    String memoryLimitExceededMessage();

    @DefaultMessage("Memory limit has been exceeded. The IDE session has been terminated.")
    @Key("memoryLimitAbortedMessage")
    String memoryLimitAbortedMessage();

    @DefaultMessage("Approaching session memory limit.")
    @Key("approachingMemoryLimit")
    String approachingMemoryLimit();

    @DefaultMessage("Over session memory limit.")
    @Key("overMemoryLimit")
    String overMemoryLimit();

    /*
     * Translated "Posit Workbench Login Required".
     *
     * @return translated "Posit Workbench Login Required"
     */
    @DefaultMessage("Posit Workbench Login Required")
    @Key("workbenchLoginRequired")
    String workbenchLoginRequired();

    @DefaultMessage("RStudio Server Login Required")
    @Key("serverLoginRequired")
    String serverLoginRequired();

    @DefaultMessage("Login expired or signed out from another window.\nSelect ''Login'' for a new login tab. Return here to resume session.")
    @Key("workbenchLoginRequiredMessage")
    String workbenchLoginRequiredMessage();

    @DefaultMessage("Login expired or signed out from another window.\nSelect ''Login'' for a new login tab.")
    @Key("serverLoginRequiredMessage")
    String serverLoginRequiredMessage();

    @DefaultMessage("Login")
    @Key("loginButton")
    String loginButton();
}

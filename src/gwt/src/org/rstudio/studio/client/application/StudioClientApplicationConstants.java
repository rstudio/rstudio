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

    @Key("retryButtonLabel")
    String retryButtonLabel();

    @Key("licensingLimitCaption")
    String licensingLimitCaption();

    @Key("quitRunningSessionsMessage")
    String quitRunningSessionsMessage(String userMsg);

    @Key("unableToConnectMessage")
    String unableToConnectMessage();

    @Key("errorTransmissionMessage")
    String errorTransmissionMessage();

    @Key("goHomeButtonLabel")
    String goHomeButtonLabel();

    @Key("cannotConnectRCaption")
    String cannotConnectRCaption();

    @Key("cannotConnectRMessage")
    String cannotConnectRMessage(String userMessage, int errorCode);

    @Key("rStudioInitializationErrorCaption")
    String rStudioInitializationErrorCaption();

    @Key("helpUsingRStudioLinkName")
    String helpUsingRStudioLinkName();

    @Key("communityForumLinkName")
    String communityForumLinkName();

    @Key("rStudioSupportLinkName")
    String rStudioSupportLinkName();

    @Key("focusedElementLabel")
    String focusedElementLabel();

    @Key("loadingWorkspaceMessage")
    String loadingWorkspaceMessage();

    @Key("savingWorkspaceImageMessage")
    String savingWorkspaceImageMessage();

    @Key("backingUpRSessionMessage")
    String backingUpRSessionMessage();

    @Key("restoringRSessionMessage")
    String restoringRSessionMessage();
    

    @Key("switchRVersionCaption")
    String switchRVersionCaption();

    @Key("workspaceNotRestoredMessage")
    String workspaceNotRestoredMessage();

    @Key("startupScriptsNotExecutedMessage")
    String startupScriptsNotExecutedMessage();

    @Key("startupScriptsErrorMessage")
    String startupScriptsErrorMessage();

    @Key("rSessionSafeModeMessage")
    String rSessionSafeModeMessage();

    @Key("terminateRMessage")
    String terminateRMessage();

    @Key("terminationDialog")
    String terminationDialog(String terminationMsg);

    @Key("terminateRCaption")
    String terminateRCaption();

    @Key("terminationConsequenceMessage")
    String terminationConsequenceMessage();

    @Key("rSessionCurrentlyBusyMessage")
    String rSessionCurrentlyBusyMessage();

    @Key("rSessionTerminalBusyMessage")
    String rSessionTerminalBusyMessage();

    @Key("terminalCurrentlyBusyMessage")
    String terminalCurrentlyBusyMessage();

    @Key("applicationQuitMessage")
    String applicationQuitMessage(String message);

    @Key("quitRStudio")
    String quitRStudio();

    @Key("quitRSessionsMessage")
    String quitRSessionsMessage();

    @Key("saveWorkspaceImageMessage")
    String saveWorkspaceImageMessage();

    @Key("quitRSessionTitle")
    String quitRSessionTitle();

    @Key("restartRCaption")
    String restartRCaption();

    @Key("terminalJobTerminatedQuestion")
    String terminalJobTerminatedQuestion();

    @Key("progressErrorCaption")
    String progressErrorCaption();

    @Key("restartingRMessage")
    String restartingRMessage();

    @Key("quitRSessionCaption")
    String quitRSessionCaption();

    @Key("restartRStudio")
    String restartRStudio();

    @Key("studioClientTitle")
    String studioClientTitle();

    @Key("switchingToProjectMessage")
    String switchingToProjectMessage();

    @Key("closingProjectMessage")
    String closingProjectMessage();

    @Key("quitRSessionMessage")
    String quitRSessionMessage();

    @Key("serverQuitSession")
    String serverQuitSession();

    @Key("consoleClearedAnnouncement")
    String consoleClearedAnnouncement();

    @Key("consoleOutputAnnouncement")
    String consoleOutputAnnouncement();

    @Key("consoleCommandAnnouncement")
    String consoleCommandAnnouncement();

    @Key("filterResultCountAnnouncement")
    String filterResultCountAnnouncement();

    @Key("commitMessageLengthAnnouncement")
    String commitMessageLengthAnnouncement();

    @Key("inaccessibleWarningAnnouncement")
    String inaccessibleWarningAnnouncment();

    @Key("infoBarsAnnouncement")
    String infoBarsAnnouncment();

    @Key("taskCompletionAnnouncement")
    String taskCompletionAnnouncement();

    @Key("taskProgressAnnouncement")
    String taskProgressAnnouncement();

    @Key("screenReaderAnnouncement")
    String screenReaderAnnouncement();

    @Key("sessionStateAnnouncement")
    String sessionStateAnnouncement();

    @Key("tabKeyFocusAnnouncement")
    String tabKeyFocusAnnouncement();

    @Key("toolBarVisibilityAnnouncement")
    String toolBarVisibilityAnnouncement();

    @Key("warningBarsAnnouncement")
    String warningBarsAnnouncement();

    @Key("sessionSuspendAnnouncement")
    String sessionSuspendAnnouncement();

    @Key("unregisteredLiveAnnouncementMessage")
    String unregisteredLiveAnnouncementMessage();

    @Key("closeRemoteSessionCaption")
    String closeRemoteSessionCaption();

    @Key("closeRemoteSessionMessage")
    String closeRemoteSessionMessage();

    @Key("licenseLostMessage")
    String licenseLostMessage();

    @Key("unableToFindActiveLicenseMessage")
    String unableToFindActiveLicenseMessage();

    @Key("activeRStudioLicenseNotFound")
    String activeRStudioLicenseNotFound();

    @Key("selectLicense")
    String selectLicense();

    @Key("detailsMessage")
    String detailsMessage();

    @Key("connectionReceivedType")
    String connectionReceivedType();

    @Key("connectionDequeuedType")
    String connectionDequeuedType();

    @Key("connectionRespondedType")
    String connectionRespondedType();

    @Key("connectionTerminatedType")
    String connectionTerminatedType();

    @Key("connectionErrorType")
    String connectionErrorType();

    @Key("connectionUnknownType")
    String connectionUnknownType();

    @Key("rStudioEditionName")
    String rStudioEditionName(String desktop);

    @Key("serverLabel")
    String serverLabel();

    @Key("copyVersionButtonTitle")
    String copyVersionButtonTitle();

    @Key("title")
    String title(String version);

    @Key("okBtn")
    String okBtn();

    @Key("manageLicenseBtn")
    String manageLicenseBtn();

    @Key("forText")
    String forText();

    @Key("versionCopiedText")
    String versionCopiedText();

    @Key("versionInformationCopiedText")
    String versionInformationCopiedText();

    @Key("copyVersionButton")
    String copyVersionButton();

    @Key("versionBuildLabel")
    String versionBuildLabel();

    @Key("buildLabelForText")
    String buildLabelForText();

    @Key("buildTypeThisText")
    String buildTypeThisText();

    @Key("buildOfText")
    String buildOfText();

    @Key("supportNoticeText")
    String supportNoticeText();

    @Key("licenseBoxLoadingText")
    String licenseBoxLoadingText();

    @Key("openSourceComponentsText")
    String openSourceComponentsText();

    @Key("closeButtonText")
    String closeButtonText();

    @Key("openSourceComponentText")
    String openSourceComponentText();

    @Key("toolbarVisibleText")
    String toolbarVisibleText();

    @Key("toolbarHiddenText")
    String toolbarHiddenText();

    @Key("focusToolbarText")
    String focusToolbarText();

    @Key("applicationUpdatedCaption")
    String applicationUpdatedCaption();

    @Key("applicationUpdatedMessage")
    String applicationUpdatedMessage();

    @Key("warningBarText")
    String warningBarText();

    @Key("rSessionErrorCaption")
    String rSessionErrorCaption();

    @Key("previousRSessionsMessage")
    String previousRSessionsMessage();

    @Key("mainLabel")
    String mainLabel();

    @Key("newFileTitle")
    String newFileTitle();

    @Key("openRecentFilesTitle")
    String openRecentFilesTitle();

    @Key("versionControlTitle")
    String versionControlTitle();

    @Key("workspacePanesTitle")
    String workspacePanesTitle();

    @Key("toolBarButtonText")
    String toolBarButtonText();

    @Key("popupMenuProgressMessage")
    String popupMenuProgressMessage();

    @Key("recentProjectsLabel")
    String recentProjectsLabel();

    @Key("sharedWithMeLabel")
    String sharedWithMeLabel();

    @Key("exportCaption")
    String exportCaption();

    @Key("importCaption")
    String importCaption();

    @Key("reloadingText")
    String reloadingText();

    @Key("retryInSafeModeText")
    String retryInSafeModeText();

    @Key("terminatingRText")
    String terminatingRText();

    @Key("defaultVersionRCaption")
    String defaultVersionRCaption();

    @Key("helpOnRVersionsTitle")
    String helpOnRVersionsTitle();

    @Key("moduleText")
    String moduleText();

    @Key("rVersionText")
    String rVersionText();

    @Key("useSystemDefaultText")
    String useSystemDefaultText();

    @Key("userSpecifiedText")
    String userSpecifiedText();

    @Key("manageLicenseText")
    String manageLicenseText();

    @Key("addinsText")
    String addinsText();

    @Key("searchForAddinsLabel")
    String searchForAddinsLabel();

    @Key("noAddinsFound")
    String noAddinsFound();

    @Key("rFatalErrorMessage")
    String rFatalErrorMessage();

    @Key("sessionTerminatedMessage")
    String sessionTerminatedMessage();

    @Key("browserDisconnectedMessage")
    String browserDisconnectedMessage();

    @Key("rStudioOfflineMessage")
    String rStudioOfflineMessage();

    @Key("rSessionEndedCaption")
    String rSessionEndedCaption();

    @Key("startNewSessionText")
    String startNewSessionText();

    @Key("rSessionAbortedCaption")
    String rSessionAbortedCaption();

    @Key("rSessionDisconnectedCaption")
    String rSessionDisconnectedCaption();

    @Key("reconnectButtonText")
    String reconnectButtonText();

    @Key("temporarilyOfflineCaption")
    String temporarilyOfflineCaption();

    @Key("unknownModeText")
    String unknownModeText();

    @Key("signOutButtonText")
    String signOutButtonText();

    @Key("errorOpeningDevToolsCaption")
    String errorOpeningDevToolsCaption();

    @Key("cannotActivateDevtoolsMessage")
    String cannotActivateDevtoolsMessage();

    @Key("errorCheckingUpdatesMessage")
    String errorCheckingUpdatesMessage();

    @Key("errorOccurredCheckingUpdatesMessage")
    String errorOccurredCheckingUpdatesMessage();

    @Key("quitDownloadButtonLabel")
    String quitDownloadButtonLabel();

    @Key("updateRStudioCaption")
    String updateRStudioCaption();

    @Key("remindLaterButtonLabel")
    String remindLaterButtonLabel();

    @Key("ignoreUpdateButtonLabel")
    String ignoreUpdateButtonLabel();

    @Key("updateAvailableCaption")
    String updateAvailableCaption();

    @Key("noUpdateAvailableCaption")
    String noUpdateAvailableCaption();

    @Key("usingNewestVersionMessage")
    String usingNewestVersionMessage();

    @Key("rStudioServerHomeTitle")
    String rStudioServerHomeTitle();

    @Key("browserNotAllowAccessLabel")
    String browserNotAllowAccessLabel();

    @Key("computerClipBoardLabel")
    String computerClipBoardLabel();

    @Key("useKeyboardShortcutsLabel")
    String useKeyboardShortcutsLabel();

    @Key("useKeyboardShortcutCaption")
    String useKeyboardShortcutCaption();

    @Key("signOutTitle")
    String signOutTitle();

    @Key("fromText")
    String fromText();

    @Key("toText")
    String toText();

    @Key("newSessionMenuLabel")
    String newSessionMenuLabel();

    @Key("saveYesLabel")
    String saveYesLabel();

    @Key("saveNoLabel")
    String saveNoLabel();

    @Key("activeText")
    String activeText();

    @Key("requestLogVisualization")
    String requestLogVisualization();

    @Key("visitWebsiteForNewVersion")
    String visitWebsiteForNewVersionText();

    @Key("updateDisabledForVersionText")
    String updateDisabledForVersionText(String version);

    @Key("stopIgnoringUpdatesButtonLabel")
    String stopIgnoringUpdatesButtonLabel();

    @Key("autoUpdateReenabledMessage")
    String autoUpdateReenabledMessage();

    @Key("autoUpdateReenabledCaption")
    String autoUpdateReenabledCaption();

    @Key("reallyCrashCaption")
    String reallyCrashCaption();

    @Key("reallyCrashMessage")
    String reallyCrashMessage();

    @Key("memoryLimitExceededCaption")
    String memoryLimitExceededCaption();

    @Key("memoryLimitExceededMessage")
    String memoryLimitExceededMessage();

    @Key("memoryLimitAbortedMessage")
    String memoryLimitAbortedMessage();

    @Key("approachingMemoryLimit")
    String approachingMemoryLimit();

    @Key("overMemoryLimit")
    String overMemoryLimit();

    /*
     * Translated "Posit Workbench Login Required".
     *
     * @return translated "Posit Workbench Login Required"
     */
    @Key("workbenchLoginRequired")
    String workbenchLoginRequired();

    @Key("serverLoginRequired")
    String serverLoginRequired();

    @Key("workbenchLoginRequiredMessage")
    String workbenchLoginRequiredMessage();

    @Key("serverLoginRequiredMessage")
    String serverLoginRequiredMessage();

    @Key("loginButton")
    String loginButton();
}

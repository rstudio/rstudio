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
    String retryButtonLabel();
    String licensingLimitCaption();
    String quitRunningSessionsMessage(String userMsg);
    String unableToConnectMessage();
    String errorTransmissionMessage();
    String goHomeButtonLabel();
    String cannotConnectRCaption();
    String cannotConnectRMessage(String userMessage, int errorCode);
    String rStudioInitializationErrorCaption();
    String helpUsingRStudioLinkName();
    String communityForumLinkName();
    String rStudioSupportLinkName();
    String focusedElementLabel();
    String loadingWorkspaceMessage();
    String savingWorkspaceImageMessage();
    String backingUpRSessionMessage();
    String restoringRSessionMessage();
    String switchRVersionCaption();
    String workspaceNotRestoredMessage();
    String startupScriptsNotExecutedMessage();
    String startupScriptsErrorMessage();
    String rSessionSafeModeMessage();
    String terminateRMessage();
    String terminationDialog(String terminationMsg);
    String terminateRCaption();
    String terminationConsequenceMessage();
    String rSessionCurrentlyBusyMessage();
    String rSessionTerminalBusyMessage();
    String terminalCurrentlyBusyMessage();
    String applicationQuitMessage(String message);
    String quitRStudio();
    String quitRSessionsMessage();
    String saveWorkspaceImageMessage();
    String quitRSessionTitle();
    String restartRCaption();
    String terminalJobTerminatedQuestion();
    String progressErrorCaption();
    String restartingRMessage();
    String quitRSessionCaption();
    String restartRStudio();
    String studioClientTitle();
    String switchingToProjectMessage();
    String closingProjectMessage();
    String quitRSessionMessage();
    String serverQuitSession();
    String consoleClearedAnnouncement();
    String consoleOutputAnnouncement();
    String consoleCommandAnnouncement();
    String filterResultCountAnnouncement();
    String commitMessageLengthAnnouncement();
    String inaccessibleWarningAnnouncement();
    String infoBarsAnnouncement();
    String taskCompletionAnnouncement();
    String taskProgressAnnouncement();
    String screenReaderAnnouncement();
    String sessionStateAnnouncement();
    String tabKeyFocusAnnouncement();
    String toolBarVisibilityAnnouncement();
    String warningBarsAnnouncement();
    String sessionSuspendAnnouncement();
    String unregisteredLiveAnnouncementMessage();
    String closeRemoteSessionCaption();
    String closeRemoteSessionMessage();
    String licenseLostMessage();
    String unableToFindActiveLicenseMessage();
    String activeRStudioLicenseNotFound();
    String selectLicense();
    String detailsMessage();
    String connectionReceivedType();
    String connectionDequeuedType();
    String connectionRespondedType();
    String connectionTerminatedType();
    String connectionErrorType();
    String connectionUnknownType();
    String rStudioEditionName(String desktop);
    String serverLabel();
    String copyVersionButtonTitle();
    String title(String version);
    String okBtn();
    String manageLicenseBtn();
    String forText();
    String versionCopiedText();
    String versionInformationCopiedText();
    String copyVersionButton();
    String versionBuildLabel();
    String buildLabelForText();
    String buildTypeThisText();
    String buildOfText();
    String supportNoticeText();
    String licenseBoxLoadingText();
    String openSourceComponentsText();
    String closeButtonText();
    String openSourceComponentText();
    String toolbarVisibleText();
    String toolbarHiddenText();
    String focusToolbarText();
    String applicationUpdatedCaption();
    String applicationUpdatedMessage();
    String warningBarText();
    String rSessionErrorCaption();
    String previousRSessionsMessage();
    String mainLabel();
    String newFileTitle();
    String openRecentFilesTitle();
    String versionControlTitle();
    String workspacePanesTitle();
    String showSidebarTitle();
    String hideSidebarTitle();
    String toolBarButtonText();
    String popupMenuProgressMessage();
    String recentProjectsLabel();
    String sharedWithMeLabel();
    String exportCaption();
    String importCaption();
    String reloadingText();
    String retryInSafeModeText();
    String terminatingRText();
    String defaultVersionRCaption();
    String helpOnRVersionsTitle();
    String moduleText();
    String rVersionText();
    String useSystemDefaultText();
    String userSpecifiedText();
    String manageLicenseText();
    String addinsText();
    String searchForAddinsLabel();
    String noAddinsFound();
    String rFatalErrorMessage();
    String sessionTerminatedMessage();
    String browserDisconnectedMessage();
    String rStudioOfflineMessage();
    String rSessionEndedCaption();
    String startNewSessionText();
    String rSessionAbortedCaption();
    String rSessionDisconnectedCaption();
    String reconnectButtonText();
    String temporarilyOfflineCaption();
    String unknownModeText();
    String signOutButtonText();
    String errorOpeningDevToolsCaption();
    String cannotActivateDevtoolsMessage();
    String errorCheckingUpdatesMessage();
    String errorOccurredCheckingUpdatesMessage();
    String quitDownloadButtonLabel();
    String updateRStudioCaption();
    String remindLaterButtonLabel();
    String ignoreUpdateButtonLabel();
    String updateAvailableCaption();
    String noUpdateAvailableCaption();
    String usingNewestVersionMessage();
    String rStudioServerHomeTitle();
    String browserNotAllowAccessLabel();
    String computerClipBoardLabel();
    String useKeyboardShortcutsLabel();
    String useKeyboardShortcutCaption();
    String signOutTitle();
    String fromText();
    String toText();
    String newSessionMenuLabel();
    String saveYesLabel();
    String saveNoLabel();
    String activeText();
    String requestLogVisualization();
    String visitWebsiteForNewVersionText();
    String updateDisabledForVersionText(String version);
    String stopIgnoringUpdatesButtonLabel();
    String autoUpdateReenabledMessage();
    String autoUpdateReenabledCaption();
    String reallyCrashCaption();
    String reallyCrashMessage();
    String memoryLimitExceededCaption();
    String memoryLimitExceededMessage();
    String memoryLimitAbortedMessage();
    String approachingMemoryLimit();
    String overMemoryLimit();
    /*
     * Translated "Posit Workbench Login Required".
     *
     * @return translated "Posit Workbench Login Required"
     */
    String workbenchLoginRequired();
    String serverLoginRequired();
    String workbenchLoginRequiredMessage();
    String serverLoginRequiredMessage();
    String loginButton();
}

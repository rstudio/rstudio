/*
 * TerminalConstants.java
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
package org.rstudio.studio.client.workbench.views.terminal;

public interface TerminalConstants extends com.google.gwt.i18n.client.Messages {
    String terminalBusyMessage(String question);
    String terminalDiagnosticsText();
    String globalTerminalInformationText();
    String captionText();
    String titleText();
    String colsText();
    String shellText();
    String handleText();
    String sequenceText();
    String restartedText();
    String busyText();
    String exitCodeText();
    String fullScreenText();
    String zombieText();
    String trackEnvText();
    String localEchoText();
    String workingDirText();
    String interactiveText();
    String webSocketsText();
    String systemInformationText();
    String desktopText();
    String platformText();
    String browserText();
    String connectionInformationText();
    String matchFailuresText();
    String notApplicableText();
    String closeTitle();
    String appendBufferTitle();
    String terminalBufferText();
    String terminalListCountText();
    String handleDumpText();
    String captionDumpText();
    String sessionCreatedText();
    String terminalTabLabel();
    String terminalCreationFailureCaption();
    String closeCaption(String title);
    String closeMessage(String caption);
    String terminateLabel();
    String cancelLabel();
    String renameTerminalTitle();
    String renameTerminalLabel();
    String nameAlreadyInUseCaption();
    String nameAlreadyInUseMessage();
    String loadedTerminalSessionsLabel();
    String handleLabel();
    String captionLabel();
    String terminalReconnectionErrorMessage();
    String errorCaption();
    String errorMessage();
    String terminalText();
    String busyCaption(String caption);
    String noTerminalReceivedFromServerText();
    String emptyTerminalCaptionText();
    String undeterminedTerminalSequenceText();
    String terminalFailedToConnect();
    String clearingBufferCaption();
    String interruptingChildCaption();
    String writeErrorMessage(String color, String message, String ansiCode);
    String processCompletedText();
    String zombieExitCodeText();
    String unknownText();
    String clearingFinalLineCaption();
    String timeoutConnectingMessage();
    String zombieNotReconnectingMessage();
    String connectedWithRPCMessage();
    String unableToDiscoverWebsocketMessage();
    String connectWebSocketMessage();
    String websockedClosedMessage();
    String websocketConnectedMessage();
    String websocketConnectError();
    String channelTypeNotImplementedError();
    String switchedToRPCMessage();
    String failedToSwitchRPCMessage();
    String terminalFailedToConnectMessage();
    String sendUserInputMessage();
    String permanentlyDisconnectedLabel();
    String disconnectedLabel();
    String closeAllTerminalsCaption();
    String closeAllTerminalsQuestion();
    String defaultShellLabel();
    String winGitBashShellLabel();
    String winWslBashShellLabel();
    String winCmdShellLabel();
    String winPsShellLabel();
    String psCoreShellLabel();
    String bashShellLabel();
    String customShellLabel();
    String nonShellLabel();
    String zshShellLabel();
    String unknownShellLabel();
}

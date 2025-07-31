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

    @Key("terminalBusyMessage")
    String terminalBusyMessage(String question);

    @Key("terminalDiagnosticsText")
    String terminalDiagnosticsText();

    @Key("globalTerminalInformationText")
    String globalTerminalInformationText();

    @Key("captionText")
    String captionText();

    @Key("titleText")
    String titleText();

    @Key("colsText")
    String colsText();

    @Key("shellText")
    String shellText();

    @Key("handleText")
    String handleText();

    @Key("sequenceText")
    String sequenceText();

    @Key("restartedText")
    String restartedText();

    @Key("busyText")
    String busyText();

    @Key("exitCodeText")
    String exitCodeText();

    @Key("fullScreenText")
    String fullScreenText();

    @Key("zombieText")
    String zombieText();

    @Key("trackEnvText")
    String trackEnvText();

    @Key("localEchoText")
    String localEchoText();

    @Key("workingDirText")
    String workingDirText();

    @Key("interactiveText")
    String interactiveText();

    @Key("webSocketsText")
    String webSocketsText();

    @Key("systemInformationText")
    String systemInformationText();

    @Key("desktopText")
    String desktopText();

    @Key("platformText")
    String platformText();

    @Key("browserText")
    String browserText();

    @Key("connectionInformationText")
    String connectionInformationText();

    @Key("matchFailuresText")
    String matchFailuresText();

    @Key("notApplicableText")
    String notApplicableText();

    @Key("closeTitle")
    String closeTitle();

    @Key("appendBufferTitle")
    String appendBufferTitle();

    @Key("terminalBufferText")
    String terminalBufferText();

    @Key("terminalListCountText")
    String terminalListCountText();

    @Key("handleDumpText")
    String handleDumpText();

    @Key("captionDumpText")
    String captionDumpText();

    @Key("sessionCreatedText")
    String sessionCreatedText();

    @Key("terminalTabLabel")
    String terminalTabLabel();

    @Key("terminalCreationFailureCaption")
    String terminalCreationFailureCaption();

    @Key("closeCaption")
    String closeCaption(String title);

    @Key("closeMessage")
    String closeMessage(String caption);

    @Key("terminateLabel")
    String terminateLabel();

    @Key("cancelLabel")
    String cancelLabel();

    @Key("renameTerminalTitle")
    String renameTerminalTitle();

    @Key("renameTerminalLabel")
    String renameTerminalLabel();

    @Key("nameAlreadyInUseCaption")
    String nameAlreadyInUseCaption();

    @Key("nameAlreadyInUseMessage")
    String nameAlreadyInUseMessage();

    @Key("loadedTerminalSessionsLabel")
    String loadedTerminalSessionsLabel();

    @Key("handleLabel")
    String handleLabel();

    @Key("captionLabel")
    String captionLabel();

    @Key("terminalReconnectionErrorMessage")
    String terminalReconnectionErrorMessage();

    @Key("errorCaption")
    String errorCaption();

    @Key("errorMessage")
    String errorMessage();

    @Key("terminalText")
    String terminalText();

    @Key("busyCaption")
    String busyCaption(String caption);

    @Key("noTerminalReceivedFromServerText")
    String noTerminalReceivedFromServerText();

    @Key("emptyTerminalCaptionText")
    String emptyTerminalCaptionText();

    @Key("undeterminedTerminalSequenceText")
    String undeterminedTerminalSequenceText();

    @Key("terminalFailedToConnect")
    String terminalFailedToConnect();

    @Key("clearingBufferCaption")
    String clearingBufferCaption();

    @Key("interruptingChildCaption")
    String interruptingChildCaption();

    @Key("writeErrorMessage")
    String writeErrorMessage(String color, String message, String ansiCode);

    @Key("processCompletedText")
    String processCompletedText();

    @Key("zombieExitCodeText")
    String zombieExitCodeText();

    @Key("unknownText")
    String unknownText();

    @Key("clearingFinalLineCaption")
    String clearingFinalLineCaption();

    @Key("timeoutConnectingMessage")
    String timeoutConnectingMessage();

    @Key("zombieNotReconnectingMessage")
    String zombieNotReconnectingMessage();

    @Key("connectedWithRPCMessage")
    String connectedWithRPCMessage();

    @Key("unableToDiscoverWebsocketMessage")
    String unableToDiscoverWebsocketMessage();

    @Key("connectWebSocketMessage")
    String connectWebSocketMessage();

    @Key("websockedClosedMessage")
    String websockedClosedMessage();

    @Key("websocketConnectedMessage")
    String websocketConnectedMessage();

    @Key("websocketConnectError")
    String websocketConnectError();

    @Key("channelTypeNotImplementedError")
    String channelTypeNotImplementedError();

    @Key("switchedToRPCMessage")
    String switchedToRPCMessage();

    @Key("failedToSwitchRPCMessage")
    String failedToSwitchRPCMessage();

    @Key("terminalFailedToConnectMessage")
    String terminalFailedToConnectMessage();

    @Key("sendUserInputMessage")
    String sendUserInputMessage();

    @Key("permanentlyDisconnectedLabel")
    String permanentlyDisconnectedLabel();

    @Key("disconnectedLabel")
    String disconnectedLabel();

    @Key("closeAllTerminalsCaption")
    String closeAllTerminalsCaption();

    @Key("closeAllTerminalsQuestion")
    String closeAllTerminalsQuestion();

    @Key("defaultShellLabel")
    String defaultShellLabel();

    @Key("winGitBashShellLabel")
    String winGitBashShellLabel();

    @Key("winWslBashShellLabel")
    String winWslBashShellLabel();

    @Key("winCmdShellLabel")
    String winCmdShellLabel();

    @Key("winPsShellLabel")
    String winPsShellLabel();

    @Key("psCoreShellLabel")
    String psCoreShellLabel();

    @Key("bashShellLabel")
    String bashShellLabel();

    @Key("customShellLabel")
    String customShellLabel();

    @Key("nonShellLabel")
    String nonShellLabel();

    @Key("zshShellLabel")
    String zshShellLabel();

    @Key("unknownShellLabel")
    String unknownShellLabel();
}

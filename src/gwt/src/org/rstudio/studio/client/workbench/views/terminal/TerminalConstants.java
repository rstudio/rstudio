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

    @DefaultMessage("The terminal is currently busy. {0}")
    @Key("terminalBusyMessage")
    String terminalBusyMessage(String question);

    @DefaultMessage("Terminal Diagnostics")
    @Key("terminalDiagnosticsText")
    String terminalDiagnosticsText();

    @DefaultMessage("Global Terminal Information\\n---------------------------\\n")
    @Key("globalTerminalInformationText")
    String globalTerminalInformationText();

    @DefaultMessage("Caption:     ")
    @Key("captionText")
    String captionText();

    @DefaultMessage("Title:       ''")
    @Key("titleText")
    String titleText();

    @DefaultMessage("Cols x Rows  ''")
    @Key("colsText")
    String colsText();

    @DefaultMessage("Shell:       ''")
    @Key("shellText")
    String shellText();

    @DefaultMessage("Handle:      ''")
    @Key("handleText")
    String handleText();

    @DefaultMessage("Sequence:    ''")
    @Key("sequenceText")
    String sequenceText();

    @DefaultMessage("Restarted:   ''")
    @Key("restartedText")
    String restartedText();

    @DefaultMessage("Busy:        ''")
    @Key("busyText")
    String busyText();

    @DefaultMessage("Exit Code:   ''")
    @Key("exitCodeText")
    String exitCodeText();

    @DefaultMessage("Full screen: ''client=")
    @Key("fullScreenText")
    String fullScreenText();

    @DefaultMessage("Zombie:      ''")
    @Key("zombieText")
    String zombieText();

    @DefaultMessage("Track Env    ''")
    @Key("trackEnvText")
    String trackEnvText();

    @DefaultMessage("Local-echo:  ''")
    @Key("localEchoText")
    String localEchoText();

    @DefaultMessage("Working Dir: ''")
    @Key("workingDirText")
    String workingDirText();

    @DefaultMessage("Interactive: ''")
    @Key("interactiveText")
    String interactiveText();

    @DefaultMessage("WebSockets:  ''")
    @Key("webSocketsText")
    String webSocketsText();

    @DefaultMessage("\\nSystem Information------------------\\n")
    @Key("systemInformationText")
    String systemInformationText();

    @DefaultMessage("Desktop:    ''")
    @Key("desktopText")
    String desktopText();

    @DefaultMessage("Platform:   ''")
    @Key("platformText")
    String platformText();

    @DefaultMessage("Browser:    ''")
    @Key("browserText")
    String browserText();

    @DefaultMessage("\\nConnection Information\\n----------------------\\n")
    @Key("connectionInformationText")
    String connectionInformationText();

    @DefaultMessage("\nLocal-echo Match Failures\n-------------------------\n")
    @Key("matchFailuresText")
    String matchFailuresText();

    @DefaultMessage("<Not applicable>\\n")
    @Key("notApplicableText")
    String notApplicableText();

    @DefaultMessage("Close")
    @Key("closeTitle")
    String closeTitle();

    @DefaultMessage("Append Buffer")
    @Key("appendBufferTitle")
    String appendBufferTitle();

    @DefaultMessage("\\n\\nTerminal Buffer (Server)\\n---------------\\n")
    @Key("terminalBufferText")
    String terminalBufferText();

    @DefaultMessage("Terminal List Count: ")
    @Key("terminalListCountText")
    String terminalListCountText();

    @DefaultMessage("Handle: ''")
    @Key("handleDumpText")
    String handleDumpText();

    @DefaultMessage("'' Caption: ''")
    @Key("captionDumpText")
    String captionDumpText();

    @DefaultMessage("'' Session Created: ")
    @Key("sessionCreatedText")
    String sessionCreatedText();

    @DefaultMessage("Terminal Tab")
    @Key("terminalTabLabel")
    String terminalTabLabel();

    @DefaultMessage("Terminal Creation Failure")
    @Key("terminalCreationFailureCaption")
    String terminalCreationFailureCaption();

    @DefaultMessage("Close {0}")
    @Key("closeCaption")
    String closeCaption(String title);

    @DefaultMessage("Are you sure you want to exit the terminal named \"{0}\"? Any running jobs will be terminated.")
    @Key("closeMessage")
    String closeMessage(String caption);

    @DefaultMessage("Terminate")
    @Key("terminateLabel")
    String terminateLabel();

    @DefaultMessage("Cancel")
    @Key("cancelLabel")
    String cancelLabel();

    @DefaultMessage("Rename Terminal")
    @Key("renameTerminalTitle")
    String renameTerminalTitle();

    @DefaultMessage("Please enter the new terminal name:")
    @Key("renameTerminalLabel")
    String renameTerminalLabel();

    @DefaultMessage("Name already in use")
    @Key("nameAlreadyInUseCaption")
    String nameAlreadyInUseCaption();

    @DefaultMessage("Please enter a unique name.")
    @Key("nameAlreadyInUseMessage")
    String nameAlreadyInUseMessage();

    @DefaultMessage("Loaded TerminalSessions: ")
    @Key("loadedTerminalSessionsLabel")
    String loadedTerminalSessionsLabel();

    @DefaultMessage("Handle: ''")
    @Key("handleLabel")
    String handleLabel();

    @DefaultMessage("'' Caption: ''")
    @Key("captionLabel")
    String captionLabel();

    @DefaultMessage("Terminal Reconnection Failure")
    @Key("terminalReconnectionErrorMessage")
    String terminalReconnectionErrorMessage();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("Tried to switch to unknown terminal handle.")
    @Key("errorMessage")
    String errorMessage();

    @DefaultMessage("Terminal")
    @Key("terminalText")
    String terminalText();

    @DefaultMessage("{0} (busy)")
    @Key("busyCaption")
    String busyCaption(String caption);

    @DefaultMessage("No Terminal ConsoleProcess received from server")
    @Key("noTerminalReceivedFromServerText")
    String noTerminalReceivedFromServerText();

    @DefaultMessage("Empty Terminal caption")
    @Key("emptyTerminalCaptionText")
    String emptyTerminalCaptionText();

    @DefaultMessage("Undetermined Terminal sequence")
    @Key("undeterminedTerminalSequenceText")
    String undeterminedTerminalSequenceText();

    @DefaultMessage("Terminal Failed to Connect")
    @Key("terminalFailedToConnect")
    String terminalFailedToConnect();

    @DefaultMessage("Clearing Buffer")
    @Key("clearingBufferCaption")
    String clearingBufferCaption();

    @DefaultMessage("Interrupting child")
    @Key("interruptingChildCaption")
    String interruptingChildCaption();

    @DefaultMessage("{0}Error: {1}{2}")
    @Key("writeErrorMessage")
    String writeErrorMessage(String color, String message, String ansiCode);

    @DefaultMessage("[Process completed]")
    @Key("processCompletedText")
    String processCompletedText();

    @DefaultMessage("[Exit code: ")
    @Key("zombieExitCodeText")
    String zombieExitCodeText();

    @DefaultMessage("Unknown")
    @Key("unknownText")
    String unknownText();

    @DefaultMessage("Clearing Final Line of Buffer")
    @Key("clearingFinalLineCaption")
    String clearingFinalLineCaption();

    @DefaultMessage("Timeout connecting via WebSockets, switching to RPC")
    @Key("timeoutConnectingMessage")
    String timeoutConnectingMessage();

    @DefaultMessage("Zombie, not reconnecting")
    @Key("zombieNotReconnectingMessage")
    String zombieNotReconnectingMessage();

    @DefaultMessage("Connected with RPC")
    @Key("connectedWithRPCMessage")
    String connectedWithRPCMessage();

    @DefaultMessage("Unable to discover websocket protocol")
    @Key("unableToDiscoverWebsocketMessage")
    String unableToDiscoverWebsocketMessage();

    @DefaultMessage("Connect WebSocket: ''")
    @Key("connectWebSocketMessage")
    String connectWebSocketMessage();

    @DefaultMessage("WebSocket closed")
    @Key("websockedClosedMessage")
    String websockedClosedMessage();

    @DefaultMessage("WebSocket connected")
    @Key("websocketConnectedMessage")
    String websocketConnectedMessage();

    @DefaultMessage("WebSocket connect error, switching to RPC")
    @Key("websocketConnectError")
    String websocketConnectError();

    @DefaultMessage("Channel type not implemented")
    @Key("channelTypeNotImplementedError")
    String channelTypeNotImplementedError();

    @DefaultMessage("Switched to RPC")
    @Key("switchedToRPCMessage")
    String switchedToRPCMessage();

    @DefaultMessage("Failed to switch to RPC: ")
    @Key("failedToSwitchRPCMessage")
    String failedToSwitchRPCMessage();

    @DefaultMessage("Terminal failed to connect. Please try again.")
    @Key("terminalFailedToConnectMessage")
    String terminalFailedToConnectMessage();

    @DefaultMessage("Tried to send user input over null websocket")
    @Key("sendUserInputMessage")
    String sendUserInputMessage();

    @DefaultMessage("Permanently Disconnected")
    @Key("permanentlyDisconnectedLabel")
    String permanentlyDisconnectedLabel();

    @DefaultMessage("Disconnected")
    @Key("disconnectedLabel")
    String disconnectedLabel();

    @DefaultMessage("Close All Terminals")
    @Key("closeAllTerminalsCaption")
    String closeAllTerminalsCaption();

    @DefaultMessage("Are you sure you want to close all terminals? Any running jobs will be stopped")
    @Key("closeAllTerminalsQuestion")
    String closeAllTerminalsQuestion();

    @DefaultMessage("Default")
    @Key("defaultShellLabel")
    String defaultShellLabel();

    @DefaultMessage("Git Bash")
    @Key("winGitBashShellLabel")
    String winGitBashShellLabel();

    @DefaultMessage("WSL")
    @Key("winWslBashShellLabel")
    String winWslBashShellLabel();

    @DefaultMessage("Command Prompt")
    @Key("winCmdShellLabel")
    String winCmdShellLabel();

    @DefaultMessage("PowerShell")
    @Key("winPsShellLabel")
    String winPsShellLabel();

    @DefaultMessage("PowerShell Core")
    @Key("psCoreShellLabel")
    String psCoreShellLabel();

    @DefaultMessage("Bash")
    @Key("bashShellLabel")
    String bashShellLabel();

    @DefaultMessage("Custom")
    @Key("customShellLabel")
    String customShellLabel();

    @DefaultMessage("User command")
    @Key("nonShellLabel")
    String nonShellLabel();

    @DefaultMessage("Zsh")
    @Key("zshShellLabel")
    String zshShellLabel();

    @DefaultMessage("Unknown")
    @Key("unknownShellLabel")
    String unknownShellLabel();
}

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

    /**
     * Translated "The terminal is currently busy {0}. ".
     */
    @DefaultMessage("The terminal is currently busy. {0}")
    @Key("terminalBusyMessage")
    String terminalBusyMessage(String question);

    /**
     * Translated "Terminal Diagnostics".
     */
    @DefaultMessage("Terminal Diagnostics")
    @Key("terminalDiagnosticsText")
    String terminalDiagnosticsText();

    /**
     * Translated "Global Terminal Information\n---------------------------\n".
     */
    @DefaultMessage("Global Terminal Information\\n---------------------------\\n")
    @Key("globalTerminalInformationText")
    String globalTerminalInformationText();

    /**
     * Translated "Caption:     ''".
     */
    @DefaultMessage("Caption:     ''")
    @Key("captionText")
    String captionText();

    /**
     * Translated "Title:       ''".
     */
    @DefaultMessage("Title:       ''")
    @Key("titleText")
    String titleText();

    /**
     * Translated "Cols x Rows  ''".
     */
    @DefaultMessage("Cols x Rows  ''")
    @Key("colsText")
    String colsText();

    /**
     * Translated "Shell:       ''".
     */
    @DefaultMessage("Shell:       ''")
    @Key("shellText")
    String shellText();

    /**
     * Translated "Handle:      ''".
     */
    @DefaultMessage("Handle:      ''")
    @Key("handleText")
    String handleText();

    /**
     * Translated "Sequence:    ''".
     */
    @DefaultMessage("Sequence:    ''")
    @Key("sequenceText")
    String sequenceText();

    /**
     * Translated "Restarted:   ''".
     */
    @DefaultMessage("Restarted:   ''")
    @Key("restartedText")
    String restartedText();

    /**
     * Translated "Busy:        ''".
     */
    @DefaultMessage("Busy:        ''")
    @Key("busyText")
    String busyText();

    /**
     * Translated "Exit Code:   ''".
     */
    @DefaultMessage("Exit Code:   ''")
    @Key("exitCodeText")
    String exitCodeText();

    /**
     * Translated "Full screen: ''client=".
     */
    @DefaultMessage("Full screen: ''client=")
    @Key("fullScreenText")
    String fullScreenText();

    /**
     * Translated "Zombie:      ''".
     */
    @DefaultMessage("Zombie:      ''")
    @Key("zombieText")
    String zombieText();

    /**
     * Translated "Track Env    ''".
     */
    @DefaultMessage("Track Env    ''")
    @Key("trackEnvText")
    String trackEnvText();

    /**
     * Translated "Local-echo:  ''".
     */
    @DefaultMessage("Local-echo:  ''")
    @Key("localEchoText")
    String localEchoText();

    /**
     * Translated "Working Dir: ''".
     */
    @DefaultMessage("Working Dir: ''")
    @Key("workingDirText")
    String workingDirText();

    /**
     * Translated "Interactive: ''".
     */
    @DefaultMessage("Interactive: ''")
    @Key("interactiveText")
    String interactiveText();

    /**
     * Translated "WebSockets:  ''".
     */
    @DefaultMessage("WebSockets:  ''")
    @Key("webSocketsText")
    String webSocketsText();

    /**
     * Translated "\nSystem Information------------------\n".
     */
    @DefaultMessage("\\nSystem Information------------------\\n")
    @Key("systemInformationText")
    String systemInformationText();

    /**
     * Translated "Desktop:    ''".
     */
    @DefaultMessage("Desktop:    ''")
    @Key("desktopText")
    String desktopText();

    /**
     * Translated "Platform:   ''".
     */
    @DefaultMessage("Platform:   ''")
    @Key("platformText")
    String platformText();

    /**
     * Translated "Browser:    ''".
     */
    @DefaultMessage("Browser:    ''")
    @Key("browserText")
    String browserText();

    /**
     * Translated "\nConnection Information\n----------------------\n".
     */
    @DefaultMessage("\\nConnection Information\\n----------------------\\n")
    @Key("connectionInformationText")
    String connectionInformationText();

    /**
     * Translated "\Local-echo Match Failures\n----------------------\n".
     */
    @DefaultMessage("\nLocal-echo Match Failures\n-------------------------\n")
    @Key("matchFailuresText")
    String matchFailuresText();

    /**
     * Translated "<Not applicable>\n".
     */
    @DefaultMessage("<Not applicable>\\n")
    @Key("notApplicableText")
    String notApplicableText();

    /**
     * Translated "Close".
     */
    @DefaultMessage("Close")
    @Key("closeTitle")
    String closeTitle();

    /**
     * Translated "Append Buffer".
     */
    @DefaultMessage("Append Buffer")
    @Key("appendBufferTitle")
    String appendBufferTitle();

    /**
     * Translated "\n\nTerminal Buffer (Server)\n---------------\n".
     */
    @DefaultMessage("\\n\\nTerminal Buffer (Server)\\n---------------\\n")
    @Key("terminalBufferText")
    String terminalBufferText();

    /**
     * Translated "Terminal List Count: ".
     */
    @DefaultMessage("Terminal List Count: ")
    @Key("terminalListCountText")
    String terminalListCountText();

    /**
     * Translated "Handle: ''".
     */
    @DefaultMessage("Handle: ''")
    @Key("handleDumpText")
    String handleDumpText();

    /**
     * Translated "'' Caption: ''".
     */
    @DefaultMessage("'' Caption: ''")
    @Key("captionDumpText")
    String captionDumpText();

    /**
     * Translated "'' Session Created: ".
     */
    @DefaultMessage("'' Session Created: ")
    @Key("sessionCreatedText")
    String sessionCreatedText();


    /**
     * Translated "Terminal Tab".
     */
    @DefaultMessage("Terminal Tab")
    @Key("terminalTabLabel")
    String terminalTabLabel();

    /**
     * Translated "Terminal Creation Failure".
     */
    @DefaultMessage("Terminal Creation Failure")
    @Key("terminalCreationFailureCaption")
    String terminalCreationFailureCaption();

    /**
     * Translated "Close {0}".
     */
    @DefaultMessage("Close {0}")
    @Key("closeCaption")
    String closeCaption(String title);

    /**
     * Translated "Are you sure you want to exit the terminal named "{0}"? Any running jobs will be terminated.".
     */
    @DefaultMessage("Are you sure you want to exit the terminal named \"{0}\"? Any running jobs will be terminated.")
    @Key("closeMessage")
    String closeMessage(String caption);

    /**
     * Translated "Terminate".
     */
    @DefaultMessage("Terminate")
    @Key("terminateLabel")
    String terminateLabel();

    /**
     * Translated "Cancel".
     */
    @DefaultMessage("Cancel")
    @Key("cancelLabel")
    String cancelLabel();

    /**
     * Translated "Rename Terminal".
     */
    @DefaultMessage("Rename Terminal")
    @Key("renameTerminalTitle")
    String renameTerminalTitle();

    /**
     * Translated "Please enter the new terminal name:".
     */
    @DefaultMessage("Please enter the new terminal name:")
    @Key("renameTerminalLabel")
    String renameTerminalLabel();

    /**
     * Translated "Name already in use".
     */
    @DefaultMessage("Name already in use")
    @Key("nameAlreadyInUseCaption")
    String nameAlreadyInUseCaption();

    /**
     * Translated "Please enter a unique name.".
     */
    @DefaultMessage("Please enter a unique name.")
    @Key("nameAlreadyInUseMessage")
    String nameAlreadyInUseMessage();

    /**
     * Translated "Loaded TerminalSessions: ".
     */
    @DefaultMessage("Loaded TerminalSessions: ")
    @Key("loadedTerminalSessionsLabel")
    String loadedTerminalSessionsLabel();

    /**
     * Translated "Handle: ''".
     */
    @DefaultMessage("Handle: ''")
    @Key("handleLabel")
    String handleLabel();

    /**
     * Translated "'' Caption: ''".
     */
    @DefaultMessage("'' Caption: ''")
    @Key("captionLabel")
    String captionLabel();

    /**
     * Translated "Terminal Reconnection Failure".
     */
    @DefaultMessage("Terminal Reconnection Failure")
    @Key("terminalReconnectionErrorMessage")
    String terminalReconnectionErrorMessage();

    /**
     * Translated "Error".
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "Tried to switch to unknown terminal handle.".
     */
    @DefaultMessage("Tried to switch to unknown terminal handle.")
    @Key("errorMessage")
    String errorMessage();

    /**
     * Translated "Terminal".
     */
    @DefaultMessage("Terminal")
    @Key("terminalText")
    String terminalText();

    /**
     * Translated "{0} (busy)".
     */
    @DefaultMessage("{0} (busy)")
    @Key("busyCaption")
    String busyCaption(String caption);

    /**
     * Translated "No Terminal ConsoleProcess received from server".
     */
    @DefaultMessage("No Terminal ConsoleProcess received from server")
    @Key("noTerminalReceivedFromServerText")
    String noTerminalReceivedFromServerText();

    /**
     * Translated "Empty Terminal caption".
     */
    @DefaultMessage("Empty Terminal caption")
    @Key("emptyTerminalCaptionText")
    String emptyTerminalCaptionText();

    /**
     * Translated "Undetermined Terminal sequence".
     */
    @DefaultMessage("Undetermined Terminal sequence")
    @Key("undeterminedTerminalSequenceText")
    String undeterminedTerminalSequenceText();

    /**
     * Translated "Terminal Failed to Connect".
     */
    @DefaultMessage("Terminal Failed to Connect")
    @Key("terminalFailedToConnect")
    String terminalFailedToConnect();

    /**
     * Translated "Clearing Buffer".
     */
    @DefaultMessage("Clearing Buffer")
    @Key("clearingBufferCaption")
    String clearingBufferCaption();

    /**
     * Translated "Interrupting child".
     */
    @DefaultMessage("Interrupting child")
    @Key("interruptingChildCaption")
    String interruptingChildCaption();

    /**
     * Translated "{0}Error: {1}{2}".
     */
    @DefaultMessage("{0}Error: {1}{2}")
    @Key("writeErrorMessage")
    String writeErrorMessage(String color, String message, String ansiCode);

    /**
     * Translated "[Process completed]".
     */
    @DefaultMessage("[Process completed]")
    @Key("processCompletedText")
    String processCompletedText();

    /**
     * Translated "[Exit code: ".
     */
    @DefaultMessage("[Exit code: ")
    @Key("zombieExitCodeText")
    String zombieExitCodeText();

    /**
     * Translated "Unknown".
     */
    @DefaultMessage("Unknown")
    @Key("unknownText")
    String unknownText();

    /**
     * Translated "Clearing Final Line of Buffer".
     */
    @DefaultMessage("Clearing Final Line of Buffer")
    @Key("clearingFinalLineCaption")
    String clearingFinalLineCaption();

    /**
     * Translated "Timeout connecting via WebSockets, switching to RPC".
     */
    @DefaultMessage("Timeout connecting via WebSockets, switching to RPC")
    @Key("timeoutConnectingMessage")
    String timeoutConnectingMessage();

    /**
     * Translated "Zombie, not reconnecting".
     */
    @DefaultMessage("Zombie, not reconnecting")
    @Key("zombieNotReconnectingMessage")
    String zombieNotReconnectingMessage();

    /**
     * Translated "Connected with RPC".
     */
    @DefaultMessage("Connected with RPC")
    @Key("connectedWithRPCMessage")
    String connectedWithRPCMessage();

    /**
     * Translated "Unable to discover websocket protocol".
     */
    @DefaultMessage("Unable to discover websocket protocol")
    @Key("unableToDiscoverWebsocketMessage")
    String unableToDiscoverWebsocketMessage();

    /**
     * Translated "Connect WebSocket: ''".
     */
    @DefaultMessage("Connect WebSocket: ''")
    @Key("connectWebSocketMessage")
    String connectWebSocketMessage();

    /**
     * Translated "WebSocket closed".
     */
    @DefaultMessage("WebSocket closed")
    @Key("websockedClosedMessage")
    String websockedClosedMessage();

    /**
     * Translated "WebSocket connected".
     */
    @DefaultMessage("WebSocket connected")
    @Key("websocketConnectedMessage")
    String websocketConnectedMessage();

    /**
     * Translated "WebSocket connect error, switching to RPC".
     */
    @DefaultMessage("WebSocket connect error, switching to RPC")
    @Key("websocketConnectError")
    String websocketConnectError();

    /**
     * Translated "Channel type not implemented".
     */
    @DefaultMessage("Channel type not implemented")
    @Key("channelTypeNotImplementedError")
    String channelTypeNotImplementedError();

    /**
     * Translated "Switched to RPC".
     */
    @DefaultMessage("Switched to RPC")
    @Key("switchedToRPCMessage")
    String switchedToRPCMessage();

    /**
     * Translated "Failed to switch to RPC: ".
     */
    @DefaultMessage("Failed to switch to RPC: ")
    @Key("failedToSwitchRPCMessage")
    String failedToSwitchRPCMessage();

    /**
     * Translated "Terminal failed to connect. Please try again.".
     */
    @DefaultMessage("Terminal failed to connect. Please try again.")
    @Key("terminalFailedToConnectMessage")
    String terminalFailedToConnectMessage();

    /**
     * Translated "Tried to send user input over null websocket".
     */
    @DefaultMessage("Tried to send user input over null websocket")
    @Key("sendUserInputMessage")
    String sendUserInputMessage();

    /**
     * Translated "Permanently Disconnected".
     */
    @DefaultMessage("Permanently Disconnected")
    @Key("permanentlyDisconnectedLabel")
    String permanentlyDisconnectedLabel();

    /**
     * Translated "Disconnected".
     */
    @DefaultMessage("Disconnected")
    @Key("disconnectedLabel")
    String disconnectedLabel();

    /**
     * Translated "Close All Terminals".
     */
    @DefaultMessage("Close All Terminals")
    @Key("closeAllTerminalsCaption")
    String closeAllTerminalsCaption();

    /**
     * Translated "Are you sure you want to close all terminals? Any running jobs will be stopped".
     */
    @DefaultMessage("Are you sure you want to close all terminals? Any running jobs will be stopped")
    @Key("closeAllTerminalsQuestion")
    String closeAllTerminalsQuestion();

    /**
     * Translated "Default".
     */
    @DefaultMessage("Default")
    @Key("defaultShellLabel")
    String defaultShellLabel();

    /**
     * Translated "Git Bash".
     */
    @DefaultMessage("Git Bash")
    @Key("winGitBashShellLabel")
    String winGitBashShellLabel();

    /**
     * Translated "WSL".
     */
    @DefaultMessage("WSL")
    @Key("winWslBashShellLabel")
    String winWslBashShellLabel();

    /**
     * Translated "Command Prompt".
     */
    @DefaultMessage("Command Prompt")
    @Key("winCmdShellLabel")
    String winCmdShellLabel();

    /**
     * Translated "PowerShell".
     */
    @DefaultMessage("PowerShell")
    @Key("winPsShellLabel")
    String winPsShellLabel();

    /**
     * Translated "PowerShell Core".
     */
    @DefaultMessage("PowerShell Core")
    @Key("psCoreShellLabel")
    String psCoreShellLabel();

    /**
     * Translated "Bash".
     */
    @DefaultMessage("Bash")
    @Key("bashShellLabel")
    String bashShellLabel();

    /**
     * Translated "Custom".
     */
    @DefaultMessage("Custom")
    @Key("customShellLabel")
    String customShellLabel();

    /**
     * Translated "User command".
     */
    @DefaultMessage("User command")
    @Key("nonShellLabel")
    String nonShellLabel();

    /**
     * Translated "Zsh".
     */
    @DefaultMessage("Zsh")
    @Key("zshShellLabel")
    String zshShellLabel();

    /**
     * Translated "Unknown".
     */
    @DefaultMessage("Unknown")
    @Key("unknownShellLabel")
    String unknownShellLabel();
}

/*
 * TerminalConstants.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
     *
     * @return translated "The terminal is currently busy {0}. "
     */
    @DefaultMessage("The terminal is currently busy. {0}")
    @Key("terminalBusyMessage")
    String terminalBusyMessage(String question);

    /**
     * Translated "Terminal Diagnostics".
     *
     * @return translated "Terminal Diagnostics"
     */
    @DefaultMessage("Terminal Diagnostics")
    @Key("terminalDiagnosticsText")
    String terminalDiagnosticsText();

    /**
     * Translated "Global Terminal Information\n---------------------------\n".
     *
     * @return translated "Global Terminal Information\n---------------------------\n"
     */
    @DefaultMessage("Global Terminal Information\\n---------------------------\\n")
    @Key("globalTerminalInformationText")
    String globalTerminalInformationText();

    /**
     * Translated "Caption:     ".
     *
     * @return translated "Caption:     "
     */
    @DefaultMessage("Caption:     ")
    @Key("captionText")
    String captionText();

    /**
     * Translated "Title:       ''".
     *
     * @return translated "Title:       ''"
     */
    @DefaultMessage("Title:       ''")
    @Key("titleText")
    String titleText();

    /**
     * Translated "Cols x Rows  ''".
     *
     * @return translated "Cols x Rows  ''"
     */
    @DefaultMessage("Cols x Rows  ''")
    @Key("colsText")
    String colsText();

    /**
     * Translated "Shell:       ''".
     *
     * @return translated "Shell:       ''"
     */
    @DefaultMessage("Shell:       ''")
    @Key("shellText")
    String shellText();

    /**
     * Translated "Handle:      ''".
     *
     * @return translated "Handle:      ''"
     */
    @DefaultMessage("Handle:      ''")
    @Key("handleText")
    String handleText();

    /**
     * Translated "Sequence:    ''".
     *
     * @return translated "Sequence:    ''"
     */
    @DefaultMessage("Sequence:    ''")
    @Key("sequenceText")
    String sequenceText();

    /**
     * Translated "Restarted:   ''".
     *
     * @return translated "Restarted:   ''"
     */
    @DefaultMessage("Restarted:   ''")
    @Key("restartedText")
    String restartedText();

    /**
     * Translated "Busy:        ''".
     *
     * @return translated "Busy:        ''"
     */
    @DefaultMessage("Busy:        ''")
    @Key("busyText")
    String busyText();

    /**
     * Translated "Exit Code:   ''".
     *
     * @return translated "Exit Code:   ''"
     */
    @DefaultMessage("Exit Code:   ''")
    @Key("exitCodeText")
    String exitCodeText();

    /**
     * Translated "Full screen: ''client=".
     *
     * @return translated "Full screen: ''client="
     */
    @DefaultMessage("Full screen: ''client=")
    @Key("fullScreenText")
    String fullScreenText();

    /**
     * Translated "Zombie:      ''".
     *
     * @return translated "Zombie:      ''"
     */
    @DefaultMessage("Zombie:      ''")
    @Key("zombieText")
    String zombieText();

    /**
     * Translated "Track Env    ''".
     *
     * @return translated "Track Env    ''"
     */
    @DefaultMessage("Track Env    ''")
    @Key("trackEnvText")
    String trackEnvText();

    /**
     * Translated "Local-echo:  ''".
     *
     * @return translated "Local-echo:  ''"
     */
    @DefaultMessage("Local-echo:  ''")
    @Key("localEchoText")
    String localEchoText();

    /**
     * Translated "Working Dir: ''".
     *
     * @return translated "Working Dir: ''"
     */
    @DefaultMessage("Working Dir: ''")
    @Key("workingDirText")
    String workingDirText();

    /**
     * Translated "Interactive: ''".
     *
     * @return translated "Interactive: ''"
     */
    @DefaultMessage("Interactive: ''")
    @Key("interactiveText")
    String interactiveText();

    /**
     * Translated "WebSockets:  ''".
     *
     * @return translated "WebSockets:  ''"
     */
    @DefaultMessage("WebSockets:  ''")
    @Key("webSocketsText")
    String webSocketsText();

    /**
     * Translated "\nSystem Information------------------\n".
     *
     * @return translated "\nSystem Information------------------\n"
     */
    @DefaultMessage("\\nSystem Information------------------\\n")
    @Key("systemInformationText")
    String systemInformationText();

    /**
     * Translated "Desktop:    ''".
     *
     * @return translated "Desktop:    ''"
     */
    @DefaultMessage("Desktop:    ''")
    @Key("desktopText")
    String desktopText();

    /**
     * Translated "Remote:     ''".
     *
     * @return translated "Remote:     ''"
     */
    @DefaultMessage("Remote:     ''")
    @Key("remoteText")
    String remoteText();

    /**
     * Translated "Platform:   ''".
     *
     * @return translated "Platform:   ''"
     */
    @DefaultMessage("Platform:   ''")
    @Key("platformText")
    String platformText();

    /**
     * Translated "Browser:    ''".
     *
     * @return translated "Browser:    ''"
     */
    @DefaultMessage("Browser:    ''")
    @Key("browserText")
    String browserText();

    /**
     * Translated "\nConnection Information\n----------------------\n".
     *
     * @return translated "\nConnection Information\n----------------------\n"
     */
    @DefaultMessage("\\nConnection Information\\n----------------------\\n")
    @Key("connectionInformationText")
    String connectionInformationText();

    /**
     * Translated "\Local-echo Match Failures\n----------------------\n".
     *
     * @return translated "\nLocal-echo Match Failures\n-------------------------"
     */
    @DefaultMessage("\nLocal-echo Match Failures\n-------------------------\n")
    @Key("matchFailuresText")
    String matchFailuresText();

    /**
     * Translated "<Not applicable>\n".
     *
     * @return translated "<Not applicable>\n"
     */
    @DefaultMessage("<Not applicable>\\n")
    @Key("notApplicableText")
    String notApplicableText();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    @Key("closeTitle")
    String closeTitle();

    /**
     * Translated "Append Buffer".
     *
     * @return translated "Append Buffer"
     */
    @DefaultMessage("Append Buffer")
    @Key("appendBufferTitle")
    String appendBufferTitle();

    /**
     * Translated "\n\nTerminal Buffer (Server)\n---------------\n".
     *
     * @return translated "\n\nTerminal Buffer (Server)\n---------------\n"
     */
    @DefaultMessage("\\n\\nTerminal Buffer (Server)\\n---------------\\n")
    @Key("terminalBufferText")
    String terminalBufferText();

    /**
     * Translated "Terminal List Count: ".
     *
     * @return translated "Terminal List Count: "
     */
    @DefaultMessage("Terminal List Count: ")
    @Key("terminalListCountText")
    String terminalListCountText();

    /**
     * Translated "Handle: ''".
     *
     * @return translated "Handle: ''"
     */
    @DefaultMessage("Handle: ''")
    @Key("handleDumpText")
    String handleDumpText();

    /**
     * Translated "'' Caption: ''".
     *
     * @return translated "'' Caption: ''"
     */
    @DefaultMessage("'' Caption: ''")
    @Key("captionDumpText")
    String captionDumpText();

    /**
     * Translated "'' Session Created: ".
     *
     * @return translated "'' Session Created: "
     */
    @DefaultMessage("'' Session Created: ")
    @Key("sessionCreatedText")
    String sessionCreatedText();


    /**
     * Translated "Terminal Tab".
     *
     * @return translated "Terminal Tab"
     */
    @DefaultMessage("Terminal Tab")
    @Key("terminalTabLabel")
    String terminalTabLabel();

    /**
     * Translated "Terminal Creation Failure".
     *
     * @return translated "Terminal Creation Failure"
     */
    @DefaultMessage("Terminal Creation Failure")
    @Key("terminalCreationFailureCaption")
    String terminalCreationFailureCaption();

    /**
     * Translated "Close {0}".
     *
     * @return translated "Close {0}"
     */
    @DefaultMessage("Close {0}")
    @Key("closeCaption")
    String closeCaption(String title);

    /**
     * Translated "Are you sure you want to exit the terminal named "{0}"? Any running jobs will be terminated.".
     *
     * @return translated "Are you sure you want to exit the terminal named "{0}"? Any running jobs will be terminated."
     */
    @DefaultMessage("Are you sure you want to exit the terminal named \"{0}\"? Any running jobs will be terminated.")
    @Key("closeMessage")
    String closeMessage(String caption);

    /**
     * Translated "Terminate".
     *
     * @return translated "Terminate"
     */
    @DefaultMessage("Terminate")
    @Key("terminateLabel")
    String terminateLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("cancelLabel")
    String cancelLabel();

    /**
     * Translated "Rename Terminal".
     *
     * @return translated "Rename Terminal"
     */
    @DefaultMessage("Rename Terminal")
    @Key("renameTerminalTitle")
    String renameTerminalTitle();

    /**
     * Translated "Please enter the new terminal name:".
     *
     * @return translated "Please enter the new terminal name:"
     */
    @DefaultMessage("Please enter the new terminal name:")
    @Key("renameTerminalLabel")
    String renameTerminalLabel();

    /**
     * Translated "Name already in use".
     *
     * @return translated "Name already in use"
     */
    @DefaultMessage("Name already in use")
    @Key("nameAlreadyInUseCaption")
    String nameAlreadyInUseCaption();

    /**
     * Translated "Please enter a unique name.".
     *
     * @return translated "Please enter a unique name."
     */
    @DefaultMessage("Please enter a unique name.")
    @Key("nameAlreadyInUseMessage")
    String nameAlreadyInUseMessage();

    /**
     * Translated "Loaded TerminalSessions: ".
     *
     * @return translated "Loaded TerminalSessions: "
     */
    @DefaultMessage("Loaded TerminalSessions: ")
    @Key("loadedTerminalSessionsLabel")
    String loadedTerminalSessionsLabel();

    /**
     * Translated "Handle: ''".
     *
     * @return translated "Handle: ''"
     */
    @DefaultMessage("Handle: ''")
    @Key("handleLabel")
    String handleLabel();

    /**
     * Translated "'' Caption: ''".
     *
     * @return translated "'' Caption: ''"
     */
    @DefaultMessage("'' Caption: ''")
    @Key("captionLabel")
    String captionLabel();

    /**
     * Translated "Terminal Reconnection Failure".
     *
     * @return translated "Terminal Reconnection Failure"
     */
    @DefaultMessage("Terminal Reconnection Failure")
    @Key("terminalReconnectionErrorMessage")
    String terminalReconnectionErrorMessage();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "Tried to switch to unknown terminal handle.".
     *
     * @return translated "Tried to switch to unknown terminal handle."
     */
    @DefaultMessage("Tried to switch to unknown terminal handle.")
    @Key("errorMessage")
    String errorMessage();

    /**
     * Translated "Terminal".
     *
     * @return translated "Terminal"
     */
    @DefaultMessage("Terminal")
    @Key("terminalText")
    String terminalText();

    /**
     * Translated "{0} (busy)".
     *
     * @return translated "{0} (busy)"
     */
    @DefaultMessage("{0} (busy)")
    @Key("busyCaption")
    String busyCaption(String caption);

    /**
     * Translated "No Terminal ConsoleProcess received from server".
     *
     * @return translated "No Terminal ConsoleProcess received from server"
     */
    @DefaultMessage("No Terminal ConsoleProcess received from server")
    @Key("noTerminalReceivedFromServerText")
    String noTerminalReceivedFromServerText();

    /**
     * Translated "Empty Terminal caption".
     *
     * @return translated "Empty Terminal caption"
     */
    @DefaultMessage("Empty Terminal caption")
    @Key("emptyTerminalCaptionText")
    String emptyTerminalCaptionText();

    /**
     * Translated "Undetermined Terminal sequence".
     *
     * @return translated "Undetermined Terminal sequence"
     */
    @DefaultMessage("Undetermined Terminal sequence")
    @Key("undeterminedTerminalSequenceText")
    String undeterminedTerminalSequenceText();

    /**
     * Translated "Terminal Failed to Connect".
     *
     * @return translated "Terminal Failed to Connect"
     */
    @DefaultMessage("Terminal Failed to Connect")
    @Key("terminalFailedToConnect")
    String terminalFailedToConnect();

    /**
     * Translated "Clearing Buffer".
     *
     * @return translated "Clearing Buffer"
     */
    @DefaultMessage("Clearing Buffer")
    @Key("clearingBufferCaption")
    String clearingBufferCaption();

    /**
     * Translated "Interrupting child".
     *
     * @return translated "Interrupting child"
     */
    @DefaultMessage("Interrupting child")
    @Key("interruptingChildCaption")
    String interruptingChildCaption();

    /**
     * Translated "{0}Error: {1}{2}".
     *
     * @return translated "{0}Error: {1}{2}"
     */
    @DefaultMessage("{0}Error: {1}{2}")
    @Key("writeErrorMessage")
    String writeErrorMessage(String color, String message, String ansiCode);

    /**
     * Translated "[Process completed]".
     *
     * @return translated "[Process completed]"
     */
    @DefaultMessage("[Process completed]")
    @Key("processCompletedText")
    String processCompletedText();

    /**
     * Translated "[Exit code: ".
     *
     * @return translated "[Exit code: "
     */
    @DefaultMessage("[Exit code: ")
    @Key("zombieExitCodeText")
    String zombieExitCodeText();

    /**
     * Translated "Unknown".
     *
     * @return translated "Unknown"
     */
    @DefaultMessage("Unknown")
    @Key("unknownText")
    String unknownText();

    /**
     * Translated "Clearing Final Line of Buffer".
     *
     * @return translated "Clearing Final Line of Buffer"
     */
    @DefaultMessage("Clearing Final Line of Buffer")
    @Key("clearingFinalLineCaption")
    String clearingFinalLineCaption();

    /**
     * Translated "Timeout connecting via WebSockets, switching to RPC".
     *
     * @return translated "Timeout connecting via WebSockets, switching to RPC"
     */
    @DefaultMessage("Timeout connecting via WebSockets, switching to RPC")
    @Key("timeoutConnectingMessage")
    String timeoutConnectingMessage();

    /**
     * Translated "Zombie, not reconnecting".
     *
     * @return translated "Zombie, not reconnecting"
     */
    @DefaultMessage("Zombie, not reconnecting")
    @Key("zombieNotReconnectingMessage")
    String zombieNotReconnectingMessage();

    /**
     * Translated "Connected with RPC".
     *
     * @return translated "Connected with RPC"
     */
    @DefaultMessage("Connected with RPC")
    @Key("connectedWithRPCMessage")
    String connectedWithRPCMessage();

    /**
     * Translated "Unable to discover websocket protocol".
     *
     * @return translated "Unable to discover websocket protocol"
     */
    @DefaultMessage("Unable to discover websocket protocol")
    @Key("unableToDiscoverWebsocketMessage")
    String unableToDiscoverWebsocketMessage();

    /**
     * Translated "Connect WebSocket: ''".
     *
     * @return translated "Connect WebSocket: ''"
     */
    @DefaultMessage("Connect WebSocket: ''")
    @Key("connectWebSocketMessage")
    String connectWebSocketMessage();

    /**
     * Translated "WebSocket closed".
     *
     * @return translated "WebSocket closed"
     */
    @DefaultMessage("WebSocket closed")
    @Key("websockedClosedMessage")
    String websockedClosedMessage();

    /**
     * Translated "WebSocket connected".
     *
     * @return translated "WebSocket connected"
     */
    @DefaultMessage("WebSocket connected")
    @Key("websocketConnectedMessage")
    String websocketConnectedMessage();

    /**
     * Translated "WebSocket connect error, switching to RPC".
     *
     * @return translated "WebSocket connect error, switching to RPC"
     */
    @DefaultMessage("WebSocket connect error, switching to RPC")
    @Key("websocketConnectError")
    String websocketConnectError();

    /**
     * Translated "Channel type not implemented".
     *
     * @return translated "Channel type not implemented"
     */
    @DefaultMessage("Channel type not implemented")
    @Key("channelTypeNotImplementedError")
    String channelTypeNotImplementedError();

    /**
     * Translated "Switched to RPC".
     *
     * @return translated "Switched to RPC"
     */
    @DefaultMessage("Switched to RPC")
    @Key("switchedToRPCMessage")
    String switchedToRPCMessage();

    /**
     * Translated "Failed to switch to RPC: ".
     *
     * @return translated "Failed to switch to RPC: "
     */
    @DefaultMessage("Failed to switch to RPC: ")
    @Key("failedToSwitchRPCMessage")
    String failedToSwitchRPCMessage();

    /**
     * Translated "Terminal failed to connect. Please try again.".
     *
     * @return translated "Terminal failed to connect. Please try again."
     */
    @DefaultMessage("Terminal failed to connect. Please try again.")
    @Key("terminalFailedToConnectMessage")
    String terminalFailedToConnectMessage();

    /**
     * Translated "Tried to send user input over null websocket".
     *
     * @return translated "Tried to send user input over null websocket"
     */
    @DefaultMessage("Tried to send user input over null websocket")
    @Key("sendUserInputMessage")
    String sendUserInputMessage();

    /**
     * Translated "Permanently Disconnected".
     *
     * @return translated "Permanently Disconnected"
     */
    @DefaultMessage("Permanently Disconnected")
    @Key("permanentlyDisconnectedLabel")
    String permanentlyDisconnectedLabel();

    /**
     * Translated "Disconnected".
     *
     * @return translated "Disconnected"
     */
    @DefaultMessage("Disconnected")
    @Key("disconnectedLabel")
    String disconnectedLabel();

    /**
     * Translated "Close All Terminals".
     *
     * @return translated "Close All Terminals"
     */
    @DefaultMessage("Close All Terminals")
    @Key("closeAllTerminalsCaption")
    String closeAllTerminalsCaption();

    /**
     * Translated "Are you sure you want to close all terminals? Any running jobs will be stopped".
     *
     * @return translated "Are you sure you want to close all terminals? Any running jobs will be stopped"
     */
    @DefaultMessage("Are you sure you want to close all terminals? Any running jobs will be stopped")
    @Key("closeAllTerminalsQuestion")
    String closeAllTerminalsQuestion();

    /**
     * Translated "Default".
     *
     * @return translated "Default"
     */
    @DefaultMessage("Default")
    @Key("defaultShellLabel")
    String defaultShellLabel();

    /**
     * Translated "Git Bash".
     *
     * @return translated "Git Bash"
     */
    @DefaultMessage("Git Bash")
    @Key("winGitBashShellLabel")
    String winGitBashShellLabel();

    /**
     * Translated "WSL".
     *
     * @return translated "WSL"
     */
    @DefaultMessage("WSL")
    @Key("winWslBashShellLabel")
    String winWslBashShellLabel();

    /**
     * Translated "Command Prompt".
     *
     * @return translated "Command Prompt"
     */
    @DefaultMessage("Command Prompt")
    @Key("winCmdShellLabel")
    String winCmdShellLabel();

    /**
     * Translated "PowerShell".
     *
     * @return translated "PowerShell"
     */
    @DefaultMessage("PowerShell")
    @Key("winPsShellLabel")
    String winPsShellLabel();

    /**
     * Translated "PowerShell Core".
     *
     * @return translated "PowerShell Core"
     */
    @DefaultMessage("PowerShell Core")
    @Key("psCoreShellLabel")
    String psCoreShellLabel();

    /**
     * Translated "Bash".
     *
     * @return translated "Bash"
     */
    @DefaultMessage("Bash")
    @Key("bashShellLabel")
    String bashShellLabel();

    /**
     * Translated "Custom".
     *
     * @return translated "Custom"
     */
    @DefaultMessage("Custom")
    @Key("customShellLabel")
    String customShellLabel();

    /**
     * Translated "User command".
     *
     * @return translated "User command"
     */
    @DefaultMessage("User command")
    @Key("nonShellLabel")
    String nonShellLabel();

    /**
     * Translated "Zsh".
     *
     * @return translated "Zsh"
     */
    @DefaultMessage("Zsh")
    @Key("zshShellLabel")
    String zshShellLabel();

    /**
     * Translated "Unknown".
     *
     * @return translated "Unknown"
     */
    @DefaultMessage("Unknown")
    @Key("unknownShellLabel")
    String unknownShellLabel();
}

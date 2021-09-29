package org.rstudio.studio.client.workbench.prefs.views;

public interface TerminalPreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "Shell".
     *
     * @return translated "Shell"
     */
    @DefaultStringValue("Shell")
    @Key("shellHeaderLabel")
    String shellHeaderLabel();

    /**
     * Translated "Initial directory:".
     *
     * @return translated "Initial directory:"
     */
    @DefaultStringValue("Initial directory:")
    @Key("initialDirectoryLabel")
    String initialDirectoryLabel();

    /**
     * Translated "Project directory".
     *
     * @return translated "Project directory"
     */
    @DefaultStringValue("Project directory")
    @Key("projectDirectoryOption")
    String projectDirectoryOption();

    /**
     * Translated "Current directory".
     *
     * @return translated "Current directory"
     */
    @DefaultStringValue("Current directory")
    @Key("currentDirectoryOption")
    String currentDirectoryOption();

    /**
     * Translated "Home directory".
     *
     * @return translated "Home directory"
     */
    @DefaultStringValue("Home directory")
    @Key("homeDirectoryOption")
    String homeDirectoryOption();

    /**
     * Translated "New terminals open with:".
     *
     * @return translated "New terminals open with:"
     */
    @DefaultStringValue("New terminals open with:")
    @Key("terminalShellLabel")
    String terminalShellLabel();

    /**
     * Translated "The program '".
     *
     * @return translated "The program '"
     */
    @DefaultStringValue("The program '")
    @Key("shellExePathMessage")
    String shellExePathMessage();

    /**
     * Translated "is unlikely to be a valid shell executable.".
     *
     * @return translated "is unlikely to be a valid shell executable."
     */
    @DefaultStringValue("is unlikely to be a valid shell executable.")
    @Key("shellExeMessage")
    String shellExeMessage();

    /**
     * Translated "Invalid Shell Executable".
     *
     * @return translated "Invalid Shell Executable"
     */
    @DefaultStringValue("Invalid Shell Executable")
    @Key("shellExeCaption")
    String shellExeCaption();

    /**
     * Translated "Custom shell binary:".
     *
     * @return translated "Custom shell binary:"
     */
    @DefaultStringValue("Custom shell binary:")
    @Key("customShellPathLabel")
    String customShellPathLabel();

    /**
     * Translated "(Not Found)".
     *
     * @return translated "(Not Found)"
     */
    @DefaultStringValue("(Not Found)")
    @Key("customShellChooserEmptyLabel")
    String customShellChooserEmptyLabel();

    /**
     * Translated "Custom shell command-line options:".
     *
     * @return translated "Custom shell command-line options:"
     */
    @DefaultStringValue("Custom shell command-line options:")
    @Key("customShellOptionsLabel")
    String customShellOptionsLabel();

    /**
     * Translated "Connection".
     *
     * @return translated "Connection"
     */
    @DefaultStringValue("Connection")
    @Key("perfLabel")
    String perfLabel();

    /**
     * Translated "Local terminal echo".
     *
     * @return translated "Local terminal echo"
     */
    @DefaultStringValue("Local terminal echo")
    @Key("chkTerminalLocalEchoLabel")
    String chkTerminalLocalEchoLabel();

    /**
     * Translated "Local echo is more responsive but may get out of sync with some line-editing modes or custom shells.".
     *
     * @return translated "Local echo is more responsive but may get out of sync with some line-editing modes or custom shells."
     */
    @DefaultStringValue("Local echo is more responsive but may get out of sync with some line-editing modes or custom shells.")
    @Key("chkTerminalLocalEchoTitle")
    String chkTerminalLocalEchoTitle();

    /**
     * Translated "Connect with WebSockets".
     *
     * @return translated "Connect with WebSockets"
     */
    @DefaultStringValue("Connect with WebSockets")
    @Key("chkTerminalWebsocketLabel")
    String chkTerminalWebsocketLabel();

    /**
     * Translated "WebSockets are generally more responsive; try turning off if terminal won't connect.".
     *
     * @return translated "WebSockets are generally more responsive; try turning off if terminal won't connect."
     */
    @DefaultStringValue("WebSockets are generally more responsive; try turning off if terminal won't connect.")
    @Key("chkTerminalWebsocketTitle")
    String chkTerminalWebsocketTitle();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    @DefaultStringValue("Display")
    @Key("displayHeaderLabel")
    String displayHeaderLabel();

    /**
     * Translated "Hardware acceleration".
     *
     * @return translated "Hardware acceleration"
     */
    @DefaultStringValue("Hardware acceleration")
    @Key("chkHardwareAccelerationLabel")
    String chkHardwareAccelerationLabel();

    /**
     * Translated "Audible bell".
     *
     * @return translated "Audible bell"
     */
    @DefaultStringValue("Audible bell")
    @Key("chkAudibleBellLabel")
    String chkAudibleBellLabel();

    /**
     * Translated "Clickable web links".
     *
     * @return translated "Clickable web links"
     */
    @DefaultStringValue("Clickable web links")
    @Key("chkWebLinksLabel")
    String chkWebLinksLabel();

    /**
     * Translated "Using the RStudio terminal".
     *
     * @return translated "Using the RStudio terminal"
     */
    @DefaultStringValue("Using the RStudio terminal")
    @Key("helpLinkLabel")
    String helpLinkLabel();

    /**
     * Translated "Miscellaneous".
     *
     * @return translated "Miscellaneous"
     */
    @DefaultStringValue("Miscellaneous")
    @Key("miscLabel")
    String miscLabel();

    /**
     * Translated "When shell exits:".
     *
     * @return translated "When shell exits:"
     */
    @DefaultStringValue("When shell exits:")
    @Key("autoClosePrefLabel")
    String autoClosePrefLabel();

    /**
     * Translated "Close the pane".
     *
     * @return translated "Close the pane"
     */
    @DefaultStringValue("Close the pane")
    @Key("closePaneOption")
    String closePaneOption();

    /**
     * Translated "Don't close the pane".
     *
     * @return translated "Don't close the pane"
     */
    @DefaultStringValue("Don't close the pane")
    @Key("doNotClosePaneOption")
    String doNotClosePaneOption();

    /**
     * Translated "Close pane if shell exits cleanly".
     *
     * @return translated "Close pane if shell exits cleanly"
     */
    @DefaultStringValue("Close pane if shell exits cleanly")
    @Key("shellExitsPaneOption")
    String shellExitsPaneOption();

    /**
     * Translated "Save and restore environment variables".
     *
     * @return translated "Save and restore environment variables"
     */
    @DefaultStringValue("Save and restore environment variables")
    @Key("chkCaptureEnvLabel")
    String chkCaptureEnvLabel();

    /**
     * Translated "Terminal occasionally runs a hidden command to capture state of environment variables.".
     *
     * @return translated "Terminal occasionally runs a hidden command to capture state of environment variables."
     */
    @DefaultStringValue("Terminal occasionally runs a hidden command to capture state of environment variables.")
    @Key("chkCaptureEnvTitle")
    String chkCaptureEnvTitle();

    /**
     * Translated "Process Termination".
     *
     * @return translated "Process Termination"
     */
    @DefaultStringValue("Process Termination")
    @Key("shutdownLabel")
    String shutdownLabel();

    /**
     * Translated "Ask before killing processes:".
     *
     * @return translated "Ask before killing processes:"
     */
    @DefaultStringValue("Ask before killing processes:")
    @Key("busyModeLabel")
    String busyModeLabel();

    /**
     * Translated "Don't ask before killing:".
     *
     * @return translated "Don't ask before killing:"
     */
    @DefaultStringValue("Don't ask before killing:")
    @Key("busyWhitelistLabel")
    String busyWhitelistLabel();

    /**
     * Translated "Terminal".
     *
     * @return translated "Terminal"
     */
    @DefaultStringValue("Terminal")
    @Key("terminalPaneLabel")
    String terminalPaneLabel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultStringValue("General")
    @Key("tabGeneralPanelLabel")
    String tabGeneralPanelLabel();

    /**
     * Translated "Closing".
     *
     * @return translated "Closing"
     */
    @DefaultStringValue("Closing")
    @Key("tabClosingPanelLabel")
    String tabClosingPanelLabel();

    /**
     * Translated "Always".
     *
     * @return translated "Always"
     */
    @DefaultStringValue("Always")
    @Key("busyModeAlwaysOption")
    String busyModeAlwaysOption();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    @DefaultStringValue("Never")
    @Key("busyModeNeverOption")
    String busyModeNeverOption();

    /**
     * Translated "Always except for list".
     *
     * @return translated "Always except for list"
     */
    @DefaultStringValue("Always except for list")
    @Key("busyModeListOption")
    String busyModeListOption();

    /**
     * Translated "Enable Python integration".
     *
     * @return translated "Enable Python integration"
     */
    @DefaultStringValue("Enable Python integration")
    @Key("chkPythonIntegration")
    String chkPythonIntegration();

    /**
     * Translated "When enabled, the active version of Python will be placed on the PATH for new terminal sessions. ".
     *
     * @return translated "When enabled, the active version of Python will be placed on the PATH for new terminal sessions. "
     */
    @DefaultStringValue("When enabled, the active version of Python will be placed on the PATH for new terminal sessions. ")
    @Key("chkPythonIntegrationTitle")
    String chkPythonIntegrationTitle();

    /**
     * Translated "When enabled, the active version of Python will be placed on the PATH for new terminal sessions. ".
     *
     * @return translated "When enabled, the active version of Python will be placed on the PATH for new terminal sessions. "
     */
    @DefaultStringValue("Only bash and zsh are supported.")
    @Key("chkPythonIntegrationMessage")
    String chkPythonIntegrationMessage();
}

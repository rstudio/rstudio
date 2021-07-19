package org.rstudio.studio.client.workbench.prefs.views;

public interface ConsolePreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    @DefaultStringValue("Display")
    @Key("consoleDisplayLabel")
    String consoleDisplayLabel();

    /**
     * Translated "Show syntax highlighting in console input".
     *
     * @return translated "Show syntax highlighting in console input"
     */
    @DefaultStringValue("Show syntax highlighting in console input")
    @Key("consoleSyntaxHighlightingLabel")
    String consoleSyntaxHighlightingLabel();

    /**
     * Translated "Different color for error or message output (requires restart)".
     *
     * @return translated "Different color for error or message output (requires restart)"
     */
    @DefaultStringValue("Different color for error or message output (requires restart)")
    @Key("consoleDifferentColorLabel")
    String consoleDifferentColorLabel();

    /**
     * Translated "Limit visible console output (requires restart)".
     *
     * @return translated "Limit visible console output (requires restart)"
     */
    @DefaultStringValue("Limit visible console output (requires restart)")
    @Key("consoleLimitVariableLabel")
    String consoleLimitVariableLabel();

    /**
     * Translated "Limit output line length to:".
     *
     * @return translated "Limit output line length to:"
     */
    @DefaultStringValue("Limit output line length to:")
    @Key("consoleLimitOutputLengthLabel")
    String consoleLimitOutputLengthLabel();

    /**
     * Translated "ANSI Escape Codes:".
     *
     * @return translated "ANSI Escape Codes:"
     */
    @DefaultStringValue("ANSI Escape Codes:")
    @Key("consoleANSIEscapeCodesLabel")
    String consoleANSIEscapeCodesLabel();

    /**
     * Translated "Show ANSI colors".
     *
     * @return translated "Show ANSI colors"
     */
    @DefaultStringValue("Show ANSI colors")
    @Key("consoleColorModeANSIOption")
    String consoleColorModeANSIOption();

    /**
     * Translated "Remove ANSI codes".
     *
     * @return translated "Remove ANSI codes"
     */
    @DefaultStringValue("Remove ANSI codes")
    @Key("consoleColorModeRemoveANSIOption")
    String consoleColorModeRemoveANSIOption();

    /**
     * Translated "Ignore ANSI codes (1.0 behavior)".
     *
     * @return translated "Ignore ANSI codes (1.0 behavior)"
     */
    @DefaultStringValue("Ignore ANSI codes (1.0 behavior)")
    @Key("consoleColorModeIgnoreANSIOption")
    String consoleColorModeIgnoreANSIOption();

    /**
     * Translated "Console".
     *
     * @return translated "Console"
     */
    @DefaultStringValue("Console")
    @Key("consoleLabel")
    String consoleLabel();

    /**
     * Translated "Debugging".
     *
     * @return translated "Debugging"
     */
    @DefaultStringValue("Debugging")
    @Key("debuggingHeaderLabel")
    String debuggingHeaderLabel();

    /**
     * Translated "Automatically expand tracebacks in error inspector".
     *
     * @return translated "Automatically expand tracebacks in error inspector"
     */
    @DefaultStringValue("Automatically expand tracebacks in error inspector")
    @Key("debuggingExpandTracebacksLabel")
    String debuggingExpandTracebacksLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    @DefaultStringValue("Other")
    @Key("otherHeaderCaption")
    String otherHeaderCaption();

    /**
     * Translated "Double-click to select words".
     *
     * @return translated "Double-click to select words"
     */
    @DefaultStringValue("Double-click to select words")
    @Key("otherDoubleClickLabel")
    String otherDoubleClickLabel();
}

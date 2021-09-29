package org.rstudio.studio.client.workbench.prefs.views;

public interface GeneralPreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "R Sessions".
     *
     * @return translated "R Sessions"
     */
    @DefaultStringValue("R Sessions")
    @Key("rSessionsTitle")
    String rSessionsTitle();

    /**
     * Translated "R version".
     *
     * @return translated "R version"
     */
    @DefaultStringValue("R version")
    @Key("rVersionTitle")
    String rVersionTitle();

    /**
     * Translated "Change...".
     *
     * @return translated "Change..."
     */
    @DefaultStringValue("Change...")
    @Key("rVersionChangeTitle")
    String rVersionChangeTitle();

    /**
     * Translated "Change R Version".
     *
     * @return translated "Change R Version"
     */
    @DefaultStringValue("Change R Version")
    @Key("rChangeVersionMessage")
    String rChangeVersionMessage();

    /**
     * Translated "You need to quit and re-open RStudio ".
     *
     * @return translated "You need to quit and re-open RStudio "
     */
    @DefaultStringValue("You need to quit and re-open RStudio ")
    @Key("rQuitReOpenMessage")
    String rQuitReOpenMessage();

    /**
     * Translated "in order for this change to take effect.".
     *
     * @return translated "in order for this change to take effect."
     */
    @DefaultStringValue("in order for this change to take effect.")
    @Key("rTakeEffectMessage")
    String rTakeEffectMessage();

    /**
     * Translated "Loading...".
     *
     * @return translated "Loading..."
     */
    @DefaultStringValue("Loading...")
    @Key("rVersionLoadingText")
    String rVersionLoadingText();

    /**
     * Translated "Restore last used R version for projects".
     *
     * @return translated "Restore last used R version for projects"
     */
    @DefaultStringValue("Restore last used R version for projects")
    @Key("rRestoreLabel")
    String rRestoreLabel();

    /**
     * Translated "working directory (when not in a project):".
     *
     * @return translated "working directory (when not in a project):"
     */
    @DefaultStringValue("Default working directory (when not in a project):")
    @Key("rDefaultDirectoryTitle")
    String rDefaultDirectoryTitle();

    /**
     * Translated "Restore most recently opened project at startup".
     *
     * @return translated "Restore most recently opened project at startup"
     */
    @DefaultStringValue("Restore most recently opened project at startup")
    @Key("rRestorePreviousTitle")
    String rRestorePreviousTitle();

    /**
     * Translated "Restore previously open source documents at startup".
     *
     * @return translated "Restore previously open source documents at startup"
     */
    @DefaultStringValue("Restore previously open source documents at startup")
    @Key("rRestorePreviousOpenTitle")
    String rRestorePreviousOpenTitle();

    /**
     * Translated "Run Rprofile when resuming suspended session".
     *
     * @return translated "Run Rprofile when resuming suspended session"
     */
    @DefaultStringValue("Run Rprofile when resuming suspended session")
    @Key("rRunProfileTitle")
    String rRunProfileTitle();

    /**
     * Translated "Workspace".
     *
     * @return translated "Workspace"
     */
    @DefaultStringValue("Workspace")
    @Key("workspaceCaption")
    String workspaceCaption();

    /**
     * Translated "Restore .RData into workspace at startup".
     *
     * @return translated "Restore .RData into workspace at startup"
     */
    @DefaultStringValue("Restore .RData into workspace at startup")
    @Key("workspaceLabel")
    String workspaceLabel();

    /**
     * Translated "Save workspace to .RData on exit:".
     *
     * @return translated "Save workspace to .RData on exit:"
     */
    @DefaultStringValue("Save workspace to .RData on exit:")
    @Key("saveWorkSpaceLabel")
    String saveWorkSpaceLabel();

    /**
     * Translated "Always".
     *
     * @return translated "Always"
     */
    @DefaultStringValue("Always")
    @Key("saveWorkAlways")
    String saveWorkAlways();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    @DefaultStringValue("Never")
    @Key("saveWorkNever")
    String saveWorkNever();

    /**
     * Translated "Ask".
     *
     * @return translated "Ask"
     */
    @DefaultStringValue("Ask")
    @Key("saveWorkAsk")
    String saveWorkAsk();

    /**
     * Translated "History".
     *
     * @return translated "History"
     */
    @DefaultStringValue("History")
    @Key("historyCaption")
    String historyCaption();

    /**
     * Translated "Always save history (even when not saving .RData)".
     *
     * @return translated "Always save history (even when not saving .RData)"
     */
    @DefaultStringValue("Always save history (even when not saving .RData)")
    @Key("alwaysSaveHistoryLabel")
    String alwaysSaveHistoryLabel();

    /**
     * Translated "Remove duplicate entries in history".
     *
     * @return translated "Remove duplicate entries in history"
     */
    @DefaultStringValue("Remove duplicate entries in history")
    @Key("removeDuplicatesLabel")
    String removeDuplicatesLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    @DefaultStringValue("Other")
    @Key("otherCaption")
    String otherCaption();

    /**
     * Translated "Wrap around when navigating to previous/next tab".
     *
     * @return translated "Wrap around when navigating to previous/next tab"
     */
    @DefaultStringValue("Wrap around when navigating to previous/next tab")
    @Key("otherWrapAroundLabel")
    String otherWrapAroundLabel();

    /**
     * Translated "Automatically notify me of updates to RStudio".
     *
     * @return translated "Automatically notify me of updates to RStudio"
     */
    @DefaultStringValue("Automatically notify me of updates to RStudio")
    @Key("otherNotifyMeLabel")
    String otherNotifyMeLabel();

    /**
     * Translated "Send automated crash reports to RStudio".
     *
     * @return translated "Send automated crash reports to RStudio"
     */
    @DefaultStringValue("Send automated crash reports to RStudio")
    @Key("otherSendReportsLabel")
    String otherSendReportsLabel();

    /**
     * Translated "Graphics Device".
     *
     * @return translated "Graphics Device"
     */
    @DefaultStringValue("Graphics Device")
    @Key("graphicsDeviceCaption")
    String graphicsDeviceCaption();

    /**
     * Translated "Antialiasing:".
     *
     * @return translated "Antialiasing:"
     */
    @DefaultStringValue("Antialiasing:")
    @Key("graphicsAntialiasingLabel")
    String graphicsAntialiasingLabel();

    /**
     * Translated "(Default):".
     *
     * @return translated "(Default):"
     */
    @DefaultStringValue("(Default)")
    @Key("antialiasingDefaultOption")
    String antialiasingDefaultOption();

    /**
     * Translated "None".
     *
     * @return translated "None"
     */
    @DefaultStringValue("None")
    @Key("antialiasingNoneOption")
    String antialiasingNoneOption();

    /**
     * Translated "Gray".
     *
     * @return translated "Gray"
     */
    @DefaultStringValue("Gray")
    @Key("antialiasingGrayOption")
    String antialiasingGrayOption();

    /**
     * Translated "Subpixel".
     *
     * @return translated "Subpixel"
     */
    @DefaultStringValue("Subpixel")
    @Key("antialiasingSubpixelOption")
    String antialiasingSubpixelOption();

    /**
     * Translated "Show server home page:".
     *
     * @return translated "Show server home page:"
     */
    @DefaultStringValue("Show server home page:")
    @Key("serverHomePageLabel")
    String serverHomePageLabel();

    /**
     * Translated "Multiple active sessions".
     *
     * @return translated "Multiple active sessions"
     */
    @DefaultStringValue("Multiple active sessions")
    @Key("serverHomePageActiveSessionsOption")
    String serverHomePageActiveSessionsOption();

    /**
     * Translated "Always".
     *
     * @return translated "Always"
     */
    @DefaultStringValue("Always")
    @Key("serverHomePageAlwaysOption")
    String serverHomePageAlwaysOption();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    @DefaultStringValue("Never")
    @Key("serverHomePageNeverOption")
    String serverHomePageNeverOption();

    /**
     * Translated "sessions".
     *
     * @return translated "sessions"
     */
    @DefaultStringValue("sessions")
    @Key("serverHomePageSessions")
    String serverHomePageSessions();

    /**
     * Translated "always".
     *
     * @return translated "always"
     */
    @DefaultStringValue("always")
    @Key("serverHomePageAlways")
    String serverHomePageAlways();

    /**
     * Translated "never".
     *
     * @return translated "never"
     */
    @DefaultStringValue("never")
    @Key("serverHomePageNever")
    String serverHomePageNever();

    /**
     * Translated "Re-use idle sessions for project links".
     *
     * @return translated "Re-use idle sessions for project links"
     */
    @DefaultStringValue("Re-use idle sessions for project links")
    @Key("reUseIdleSessionLabel")
    String reUseIdleSessionLabel();

    /**
     * Translated "Home Page".
     *
     * @return translated "Home Page"
     */
    @DefaultStringValue("Home Page")
    @Key("desktopCaption")
    String desktopCaption();

    /**
     * Translated "Debugging".
     *
     * @return translated "Debugging"
     */
    @DefaultStringValue("Debugging")
    @Key("advancedDebuggingCaption")
    String advancedDebuggingCaption();

    /**
     * Translated "Use debug error handler only when my code contains errors".
     *
     * @return translated "Use debug error handler only when my code contains errors"
     */
    @DefaultStringValue("Use debug error handler only when my code contains errors")
    @Key("advancedDebuggingLabel")
    String advancedDebuggingLabel();

    /**
     * Translated "OS Integration".
     *
     * @return translated "OS Integration"
     */
    @DefaultStringValue("OS Integration")
    @Key("advancedOsIntegrationCaption")
    String advancedOsIntegrationCaption();

    /**
     * Translated "Rendering engine:".
     *
     * @return translated "Rendering engine:"
     */
    @DefaultStringValue("Rendering engine:")
    @Key("advancedRenderingEngineLabel")
    String advancedRenderingEngineLabel();

    /**
     * Translated "Auto-detect (recommended)".
     *
     * @return translated "Auto-detect (recommended)"
     */
    @DefaultStringValue("Auto-detect (recommended)")
    @Key("renderingEngineAutoDetectOption")
    String renderingEngineAutoDetectOption();

    /**
     * Translated "Desktop OpenGL".
     *
     * @return translated "Desktop OpenGL"
     */
    @DefaultStringValue("Desktop OpenGL")
    @Key("renderingEngineDesktopOption")
    String renderingEngineDesktopOption();

    /**
     * Translated "OpenGL for Embedded Systems".
     *
     * @return translated "OpenGL for Embedded Systems"
     */
    @DefaultStringValue("OpenGL for Embedded Systems")
    @Key("renderingEngineLinuxDesktopOption")
    String renderingEngineLinuxDesktopOption();

    /**
     * Translated "Software".
     *
     * @return translated "Software"
     */
    @DefaultStringValue("Software")
    @Key("renderingEngineSoftwareOption")
    String renderingEngineSoftwareOption();

    /**
     * Translated "Use GPU exclusion list (recommended)".
     *
     * @return translated "Use GPU exclusion list (recommended)"
     */
    @DefaultStringValue("Use GPU exclusion list (recommended)")
    @Key("useGpuExclusionListLabel")
    String useGpuExclusionListLabel();

    /**
     * Translated "Use GPU driver bug workarounds (recommended)".
     *
     * @return translated "Use GPU driver bug workarounds (recommended)"
     */
    @DefaultStringValue("Use GPU driver bug workarounds (recommended)")
    @Key("useGpuDriverBugWorkaroundsLabel")
    String useGpuDriverBugWorkaroundsLabel();

    /**
     * Translated "Enable X11 clipboard monitoring".
     *
     * @return translated "Enable X11 clipboard monitoring"
     */
    @DefaultStringValue("Enable X11 clipboard monitoring")
    @Key("clipboardMonitoringLabel")
    String clipboardMonitoringLabel();

    /**
     * Translated "Show full path to project in window title".
     *
     * @return translated "Show full path to project in window title"
     */
    @DefaultStringValue("Show full path to project in window title")
    @Key("fullPathInTitleLabel")
    String fullPathInTitleLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    @DefaultStringValue("Other")
    @Key("otherLabel")
    String otherLabel();

    /**
     * Translated "Show .Last.value in environment listing".
     *
     * @return translated "Show .Last.value in environment listing"
     */
    @DefaultStringValue("Show .Last.value in environment listing")
    @Key("otherShowLastDotValueLabel")
    String otherShowLastDotValueLabel();

    /**
     * Translated "Help panel font size:".
     *
     * @return translated "Help panel font size:"
     */
    @DefaultStringValue("Help panel font size:")
    @Key("helpFontSizeLabel")
    String helpFontSizeLabel();

    /**
     * Translated "General".
     *
     * @return "General"
     */
    @DefaultStringValue("General")
    @Key("generalTabListLabel")
    String generalTablistLabel();

    /**
     * Translated "Basic".
     *
     * @return "Basic"
     */
    @DefaultStringValue("Basic")
    @Key("generalTabListBasicOption")
    String generalTablListBasicOption();

    /**
     * Translated "Graphics".
     *
     * @return "Graphics"
     */
    @DefaultStringValue("Graphics")
    @Key("generalTabListGraphicsOption")
    String generalTablListGraphicsOption();

    /**
     * Translated "Advanced".
     *
     * @return "Advanced"
     */
    @DefaultStringValue("Advanced")
    @Key("generalTabListAdvancedOption")
    String generalTabListAdvancedOption();

    /**
     * Translated " (Default)".
     *
     * @return " (Default)"
     */
    @DefaultStringValue("(Default)")
    @Key("graphicsBackEndDefaultOption")
    String graphicsBackEndDefaultOption();

    /**
     * Translated "Quartz".
     *
     * @return "Quartz"
     */
    @DefaultStringValue("Quartz")
    @Key("graphicsBackEndQuartzOption")
    String graphicsBackEndQuartzOption();

    /**
     * Translated "Windows".
     *
     * @return "Windows"
     */
    @DefaultStringValue("Windows")
    @Key("graphicsBackEndWindowsOption")
    String graphicsBackEndWindowsOption();

    /**
     * Translated "Cairo".
     *
     * @return "Cairo"
     */
    @DefaultStringValue("Cairo")
    @Key("graphicsBackEndCairoOption")
    String graphicsBackEndCairoOption();

    /**
     * Translated "Cairo PNG".
     *
     * @return "Cairo PNG"
     */
    @DefaultStringValue("Cairo PNG")
    @Key("graphicsBackEndCairoPNGOption")
    String graphicsBackEndCairoPNGOption();

    /**
     * Translated "AGG".
     *
     * @return "AGG"
     */
    @DefaultStringValue("AGG")
    @Key("graphicsBackEndAGGOption")
    String graphicsBackEndAGGOption();

    /**
     * Translated "Backend:".
     *
     * @return "Backend:"
     */
    @DefaultStringValue("Backend:")
    @Key("graphicsBackendLabel")
    String graphicsBackendLabel();

    /**
     * Translated "Using the AGG renderer".
     *
     * @return "Using the AGG renderer"
     */
    @DefaultStringValue("Using the AGG renderer")
    @Key("graphicsBackendUserAction")
    String graphicsBackendUserAction();

    /**
     * Translated "default".
     *
     * @return "default"
     */
    @DefaultStringValue("default")
    @Key("defaultLabel")
    String defaultLabel();

    /**
     * Translated "Browse...".
     *
     * @return "Browse..."
     */
    @DefaultStringValue("Browse...")
    @Key("browseLabel")
    String browseLabel();

    /**
     * Translated "Choose Directory".
     *
     * @return "Choose Directory"
     */
    @DefaultStringValue("Choose Directory")
    @Key("directoryLabel")
    String directoryLabel();
}

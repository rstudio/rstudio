package org.rstudio.studio.client.workbench.prefs.views;

public interface RMarkdownPreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultStringValue("R Markdown")
    @Key("rMarkdownHeaderLabel")
    String rMarkdownHeaderLabel();

    /**
     * Translated "Show document outline by default".
     *
     * @return translated "Show document outline by default"
     */
    @DefaultStringValue("Show document outline by default")
    @Key("rMarkdownShowLabel")
    String rMarkdownShowLabel();

    /**
     * Translated "Soft-wrap R Markdown files".
     *
     * @return translated "Soft-wrap R Markdown files"
     */
    @DefaultStringValue("Soft-wrap R Markdown files")
    @Key("rMarkdownSoftWrapLabel")
    String rMarkdownSoftWrapLabel();

    /**
     * Translated "Show in document outline: ".
     *
     * @return translated "Show in document outline: "
     */
    @DefaultStringValue("Show in document outline: ")
    @Key("docOutlineDisplayLabel")
    String docOutlineDisplayLabel();

    /**
     * Translated "Sections Only".
     *
     * @return translated "Sections Only"
     */
    @DefaultStringValue("Sections Only")
    @Key("docOutlineSectionsOption")
    String docOutlineSectionsOption();

    /**
     * Translated "Sections and Named Chunks".
     *
     * @return translated "Sections and Named Chunks"
     */
    @DefaultStringValue("Sections and Named Chunks")
    @Key("docOutlineSectionsNamedChunksOption")
    String docOutlineSectionsNamedChunksOption();

    /**
     * Translated "Sections and All Chunks".
     *
     * @return translated "Sections and All Chunks"
     */
    @DefaultStringValue("Sections and All Chunks")
    @Key("docOutlineSectionsAllChunksOption")
    String docOutlineSectionsAllChunksOption();

    /**
     * Translated "Show output preview in: ".
     *
     * @return translated "Show output preview in: "
     */
    @DefaultStringValue("Show output preview in: ")
    @Key("rmdViewerModeLabel")
    String rmdViewerModeLabel();

    /**
     * Translated "Window".
     *
     * @return translated "Window"
     */
    @DefaultStringValue("Window")
    @Key("rmdViewerModeWindowOption")
    String rmdViewerModeWindowOption();

    /**
     * Translated "Viewer Pane".
     *
     * @return translated "Viewer Pane"
     */
    @DefaultStringValue("Viewer Pane")
    @Key("rmdViewerModeViewerPaneOption")
    String rmdViewerModeViewerPaneOption();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    @DefaultStringValue("(None)")
    @Key("rmdViewerModeNoneOption")
    String rmdViewerModeNoneOption();

    /**
     * Translated "Show output inline for all R Markdown documents".
     *
     * @return translated "Show output inline for all R Markdown documents"
     */
    @DefaultStringValue("Show output inline for all R Markdown documents")
    @Key("rmdInlineOutputLabel")
    String rmdInlineOutputLabel();

    /**
     * Translated "Show equation and image previews: ".
     *
     * @return translated "Show equation and image previews: "
     */
    @DefaultStringValue("Show equation and image previews: ")
    @Key("latexPreviewWidgetLabel")
    String latexPreviewWidgetLabel();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    @DefaultStringValue("Never")
    @Key("latexPreviewWidgetNeverOption")
    String latexPreviewWidgetNeverOption();

    /**
     * Translated "In a popup".
     *
     * @return translated "In a popup"
     */
    @DefaultStringValue("In a popup")
    @Key("latexPreviewWidgetPopupOption")
    String latexPreviewWidgetPopupOption();

    /**
     * Translated "Inline".
     *
     * @return translated "Inline"
     */
    @DefaultStringValue("Inline")
    @Key("latexPreviewWidgetInlineOption")
    String latexPreviewWidgetInlineOption();

    /**
     * Translated "Evaluate chunks in directory: ".
     *
     * @return translated "Evaluate chunks in directory: "
     */
    @DefaultStringValue("Evaluate chunks in directory: ")
    @Key("knitWorkingDirLabel")
    String knitWorkingDirLabel();

    /**
     * Translated "Document".
     *
     * @return translated "Document"
     */
    @DefaultStringValue("Document")
    @Key("knitWorkingDirDocumentOption")
    String knitWorkingDirDocumentOption();

    /**
     * Translated "Current".
     *
     * @return translated "Current"
     */
    @DefaultStringValue("Current")
    @Key("knitWorkingDirCurrentOption")
    String knitWorkingDirCurrentOption();

    /**
     * Translated "Project".
     *
     * @return translated "Project"
     */
    @DefaultStringValue("Project")
    @Key("knitWorkingDirProjectOption")
    String knitWorkingDirProjectOption();

    /**
     * Translated "R Notebooks".
     *
     * @return translated "R Notebooks"
     */
    @DefaultStringValue("R Notebooks")
    @Key("rNotebooksCaption")
    String rNotebooksCaption();

    /**
     * Translated "Execute setup chunk automatically in notebooks".
     *
     * @return translated "Execute setup chunk automatically in notebooks"
     */
    @DefaultStringValue("Execute setup chunk automatically in notebooks")
    @Key("autoExecuteSetupChunkLabel")
    String autoExecuteSetupChunkLabel();

    /**
     * Translated "Hide console automatically when executing ".
     *
     * @return translated "Hide console automatically when executing "
     */
    @DefaultStringValue("Hide console automatically when executing ")
    @Key("notebookHideConsoleLabel")
    String notebookHideConsoleLabel();

    /**
     * Translated "notebook chunks".
     *
     * @return translated "notebook chunks"
     */
    @DefaultStringValue("notebook chunks")
    @Key("notebookHideConsoleChunksLabel")
    String notebookHideConsoleChunksLabel();

    /**
     * Translated "Using R Notebooks".
     *
     * @return translated "Using R Notebooks"
     */
    @DefaultStringValue("Using R Notebooks")
    @Key("helpLinkLabel")
    String helpLinkLabel();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    @DefaultStringValue("Display")
    @Key("advancedHeaderLabel")
    String advancedHeaderLabel();

    /**
     * Translated "Enable chunk background highlight".
     *
     * @return translated "Enable chunk background highlight"
     */
    @DefaultStringValue("Enable chunk background highlight")
    @Key("advancedEnableChunkLabel")
    String advancedEnableChunkLabel();

    /**
     * Translated "Show inline toolbar for R code chunks".
     *
     * @return translated "Show inline toolbar for R code chunks"
     */
    @DefaultStringValue("Show inline toolbar for R code chunks")
    @Key("advancedShowInlineLabel")
    String advancedShowInlineLabel();

    /**
     * Translated "Display render command in R Markdown tab".
     *
     * @return translated "Display render command in R Markdown tab"
     */
    @DefaultStringValue("Display render command in R Markdown tab")
    @Key("advancedDisplayRender")
    String advancedDisplayRender();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultStringValue("General")
    @Key("visualModeGeneralCaption")
    String visualModeGeneralCaption();

    /**
     * Translated "Use visual editor by default for new documents".
     *
     * @return translated "Use visual editor by default for new documents"
     */
    @DefaultStringValue("Use visual editor by default for new documents")
    @Key("visualModeUseVisualEditorLabel")
    String visualModeUseVisualEditorLabel();

    /**
     * Translated "Learn more about visual editing mode".
     *
     * @return translated "Learn more about visual editing mode"
     */
    @DefaultStringValue("Learn more about visual editing mode")
    @Key("visualModeHelpLink")
    String visualModeHelpLink();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    @DefaultStringValue("Display")
    @Key("visualModeHeaderLabel")
    String visualModeHeaderLabel();

    /**
     * Translated "Show document outline by default".
     *
     * @return translated "Show document outline by default"
     */
    @DefaultStringValue("Show document outline by default")
    @Key("visualEditorShowOutlineLabel")
    String visualEditorShowOutlineLabel();

    /**
     * Translated "Show margin column indicator in code blocks".
     *
     * @return translated "Show margin column indicator in code blocks"
     */
    @DefaultStringValue("Show margin column indicator in code blocks")
    @Key("visualEditorShowMarginLabel")
    String visualEditorShowMarginLabel();

    /**
     * Translated "Editor content width (px):".
     *
     * @return translated "Editor content width (px):"
     */
    @DefaultStringValue("Editor content width (px):")
    @Key("visualModeContentWidthLabel")
    String visualModeContentWidthLabel();

    /**
     * Translated "Editor font size:".
     *
     * @return translated "Editor font size:"
     */
    @DefaultStringValue("Editor font size:")
    @Key("visualModeFontSizeLabel")
    String visualModeFontSizeLabel();

    /**
     * Translated "Markdown".
     *
     * @return translated "Markdown"
     */
    @DefaultStringValue("Markdown")
    @Key("visualModeOptionsMarkdownCaption")
    String visualModeOptionsMarkdownCaption();

    /**
     * Translated "Default spacing between list items: ".
     *
     * @return translated "Default spacing between list items: "
     */
    @DefaultStringValue("Default spacing between list items: ")
    @Key("visualModeListSpacingLabel")
    String visualModeListSpacingLabel();

    /**
     * Translated "Automatic text wrapping (line breaks): ".
     *
     * @return translated "Automatic text wrapping (line breaks): "
     */
    @DefaultStringValue("Automatic text wrapping (line breaks): ")
    @Key("visualModeWrapLabel")
    String visualModeWrapLabel();

    /**
     * Translated "Learn more about automatic line wrapping".
     *
     * @return translated "Learn more about automatic line wrapping"
     */
    @DefaultStringValue("Learn more about automatic line wrapping")
    @Key("visualModeWrapHelpLabel")
    String visualModeWrapHelpLabel();

    /**
     * Translated "Wrap at column:".
     *
     * @return translated "Wrap at column:"
     */
    @DefaultStringValue("Wrap at column:")
    @Key("visualModeOptionsLabel")
    String visualModeOptionsLabel();

    /**
     * Translated "Write references at end of current: ".
     *
     * @return translated "Write references at end of current: "
     */
    @DefaultStringValue("Write references at end of current: ")
    @Key("visualModeReferencesLabel")
    String visualModeReferencesLabel();

    /**
     * Translated "Write canonical visual mode markdown in source mode".
     *
     * @return translated "Write canonical visual mode markdown in source mode"
     */
    @DefaultStringValue("Write canonical visual mode markdown in source mode")
    @Key("visualModeCanonicalLabel")
    String visualModeCanonicalLabel();

    /**
     * Translated "Visual Mode Preferences".
     *
     * @return translated "Visual Mode Preferences"
     */
    @DefaultStringValue("Visual Mode Preferences")
    @Key("visualModeCanonicalMessageCaption")
    String visualModeCanonicalMessageCaption();

    /**
     * Translated "Are you sure you want to write canonical markdown from source mode for all R Markdown files?".
     *
     * @return translated "Are you sure you want to write canonical markdown from source mode for all R Markdown files?"
     */
    @DefaultStringValue("Are you sure you want to write canonical markdown from source mode for all R Markdown files?")
    @Key("visualModeCanonicalPreferenceMessage")
    String visualModeCanonicalPreferenceMessage();

    /**
     * Translated "Are you sure you want to write canonical markdown from source mode for all R Markdown files?".
     *
     * @return translated "Are you sure you want to write canonical markdown from source mode for all R Markdown files?"
     */
    @DefaultStringValue("This preference should generally only be used at a project level (to prevent ")
    @Key("visualModeCanonicalMessageDesc")
    String visualModeCanonicalMessageDesc();

    /**
     * Translated "re-writing of markdown source that you or others don't intend to use with visual mode).".
     *
     * @return translated "re-writing of markdown source that you or others don't intend to use with visual mode)."
     */
    @DefaultStringValue("re-writing of markdown source that you or others don't intend to use with visual mode).")
    @Key("visualModeCanonicalMessageRewrite")
    String visualModeCanonicalMessageRewrite();

    /**
     * Translated "Change this preference now?".
     *
     * @return translated "Change this preference now?"
     */
    @DefaultStringValue("Change this preference now?")
    @Key("visualModeCanonicalChangeMessage")
    String visualModeCanonicalChangeMessage();

    /**
     * Translated "Learn more about markdown writer options".
     *
     * @return translated "Learn more about markdown writer options"
     */
    @DefaultStringValue("Learn more about markdown writer options")
    @Key("markdownPerFileOptionsHelpLink")
    String markdownPerFileOptionsHelpLink();

    /**
     * Translated "Citation features are available within R Markdown visual mode.".
     *
     * @return translated "Citation features are available within R Markdown visual mode."
     */
    @DefaultStringValue("Citation features are available within R Markdown visual mode.")
    @Key("citationsLabel")
    String citationsLabel();

    /**
     * Translated "Learn more about using citations with visual editing mode".
     *
     * @return translated "Learn more about using citations with visual editing mode"
     */
    @DefaultStringValue("Learn more about using citations with visual editing mode")
    @Key("citationsHelpLink")
    String citationsHelpLink();

    /**
     * Translated "Zotero".
     *
     * @return translated "Zotero"
     */
    @DefaultStringValue("Zotero")
    @Key("zoteroHeaderLabel")
    String zoteroHeaderLabel();

    /**
     * Translated "Zotero Data Directory:".
     *
     * @return translated "Zotero Data Directory:"
     */
    @DefaultStringValue("Zotero Data Directory:")
    @Key("zoteroDataDirLabel")
    String zoteroDataDirLabel();

    /**
     * Translated "(None Detected)".
     *
     * @return translated "(None Detected)"
     */
    @DefaultStringValue("(None Detected)")
    @Key("zoteroDataDirNotDectedLabel")
    String zoteroDataDirNotDectedLabel();

    /**
     * Translated "Use Better BibTeX for citation keys and BibTeX export".
     *
     * @return translated "Use Better BibTeX for citation keys and BibTeX export"
     */
    @DefaultStringValue("Use Better BibTeX for citation keys and BibTeX export")
    @Key("zoteroUseBetterBibtexLabel")
    String zoteroUseBetterBibtexLabel();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultStringValue("R Markdown")
    @Key("tabPanelTitle")
    String tabPanelTitle();

    /**
     * Translated "Basic".
     *
     * @return translated "Basic"
     */
    @DefaultStringValue("Basic")
    @Key("tabPanelBasic")
    String tabPanelBasic();

    /**
     * Translated "Advanced".
     *
     * @return translated "Advanced"
     */
    @DefaultStringValue("Advanced")
    @Key("tabPanelAdvanced")
    String tabPanelAdvanced();

    /**
     * Translated "Visual".
     *
     * @return translated "Visual"
     */
    @DefaultStringValue("Visual")
    @Key("tabPanelVisual")
    String tabPanelVisual();

    /**
     * Translated "Citations".
     *
     * @return translated "Citations"
     */
    @DefaultStringValue("Citations")
    @Key("tabPanelCitations")
    String tabPanelCitations();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    @DefaultStringValue("(None)")
    @Key("noneOption")
    String noneOption();

    /**
     * Translated "Local".
     *
     * @return translated "Local"
     */
    @DefaultStringValue("Local")
    @Key("localOption")
    String localOption();

    /**
     * Translated "Web".
     *
     * @return translated "Web"
     */
    @DefaultStringValue("Web")
    @Key("webOption")
    String webOption();

    /**
     * Translated "Zotero Library:".
     *
     * @return translated "Zotero Library:"
     */
    @DefaultStringValue("Zotero Library:")
    @Key("zoteroLibLabel")
    String zoteroLibLabel();

    /**
     * Translated "Using Zotero".
     *
     * @return translated "Using Zotero"
     */
    @DefaultStringValue("Using Zotero")
    @Key("zoteroHelpLink")
    String zoteroHelpLink();
}

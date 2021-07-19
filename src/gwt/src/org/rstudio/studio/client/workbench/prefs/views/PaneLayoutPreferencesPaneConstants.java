package org.rstudio.studio.client.workbench.prefs.views;

public interface PaneLayoutPreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "Choose the layout of the panels in RStudio by selecting from the controls in".
     *
     * @return translated "Choose the layout of the panels in RStudio by selecting from the controls in"
     */
    @DefaultStringValue("Choose the layout of the panels in RStudio by selecting from the controls in")
    @Key("paneLayoutText")
    String paneLayoutText();

    /**
     * Translated "each panel. Add up to three additional Source Columns to the left side of the layout. ".
     *
     * @return translated "each panel. Add up to three additional Source Columns to the left side of the layout. "
     */
    @DefaultStringValue("each panel. Add up to three additional Source Columns to the left side of the layout. ")
    @Key("paneLayoutAddText")
    String paneLayoutAddText();

    /**
     * Translated "When a column is removed, all saved files within the column are closed and any unsaved ".
     *
     * @return translated "When a column is removed, all saved files within the column are closed and any unsaved "
     */
    @DefaultStringValue("When a column is removed, all saved files within the column are closed and any unsaved ")
    @Key("paneLayoutRemoveText")
    String paneLayoutRemoveText();

    /**
     * Translated "files are moved to the main Source Pane.".
     *
     * @return translated "files are moved to the main Source Pane."
     */
    @DefaultStringValue("files are moved to the main Source Pane.")
    @Key("paneLayoutMoveText")
    String paneLayoutMoveText();

    /**
     * Translated "Manage Column Display".
     *
     * @return translated "Manage Column Display"
     */
    @DefaultStringValue("Manage Column Display")
    @Key("columnToolbarLabel")
    String columnToolbarLabel();

    /**
     * Translated "Add Column".
     *
     * @return translated "Add Column"
     */
    @DefaultStringValue("Add Column")
    @Key("addButtonText")
    String addButtonText();

    /**
     * Translated "Add column".
     *
     * @return translated "Add column"
     */
    @DefaultStringValue("Add column")
    @Key("addButtonLabel")
    String addButtonLabel();

    /**
     * Translated "Remove Column".
     *
     * @return translated "Remove Column"
     */
    @DefaultStringValue("Remove Column")
    @Key("removeButtonText")
    String removeButtonText();

    /**
     * Translated "Remove column".
     *
     * @return translated "Remove column"
     */
    @DefaultStringValue("Remove column")
    @Key("removeButtonLabel")
    String removeButtonLabel();

    /**
     * Translated "Top left panel".
     *
     * @return translated "Top left panel"
     */
    @DefaultStringValue("Top left panel")
    @Key("leftTopPanelText")
    String leftTopPanelText();

    /**
     * Translated "Bottom left panel".
     *
     * @return translated "Bottom left panel"
     */
    @DefaultStringValue("Bottom left panel")
    @Key("leftBottomPanelText")
    String leftBottomPanelText();

    /**
     * Translated "Top right panel".
     *
     * @return translated "Top right panel"
     */
    @DefaultStringValue("Top right panel")
    @Key("rightTopPanelText")
    String rightTopPanelText();

    /**
     * Translated "Bottom right panel".
     *
     * @return translated "Bottom right panel"
     */
    @DefaultStringValue("Bottom right panel")
    @Key("rightBottomPanelText")
    String rightBottomPanelText();

    /**
     * Translated "Bad config! Falling back to a reasonable default".
     *
     * @return translated "Bad config! Falling back to a reasonable default"
     */
    @DefaultStringValue("Bad config! Falling back to a reasonable default")
    @Key("debugLogText")
    String debugLogText();

    /**
     * Translated "Bad config! Falling back to a reasonable default".
     *
     * @return translated "Bad config! Falling back to a reasonable default"
     */
    @DefaultStringValue("Columns and Panes Layout")
    @Key("createGridLabel")
    String createGridLabel();

    /**
     * Translated "Additional source column".
     *
     * @return translated "Additional source column"
     */
    @DefaultStringValue("Additional source column")
    @Key("createColumnLabel")
    String createColumnLabel();

    /**
     * Translated "Pane Layout".
     *
     * @return translated "Pane Layout"
     */
    @DefaultStringValue("Pane Layout")
    @Key("paneLayoutLabel")
    String paneLayoutLabel();
}

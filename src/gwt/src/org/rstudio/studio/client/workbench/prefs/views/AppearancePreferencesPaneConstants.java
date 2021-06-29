package org.rstudio.studio.client.workbench.prefs.views;

public interface AppearancePreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "RStudio theme:".
     *
     * @return translated "RStudio theme:"
     */
    @DefaultStringValue("RStudio theme:")
    @Key("appearanceRStudioThemeLabel")
    String appearanceRStudioThemeLabel();

    /**
     * Translated "Zoom:".
     *
     * @return translated "Zoom:"
     */
    @DefaultStringValue("Zoom:")
    @Key("appearanceZoomLabelZoom")
    String appearanceZoomLabelZoom();

    /**
     * Translated "Editor font (loading...):".
     *
     * @return translated "Editor font (loading...):"
     */
    @DefaultStringValue("Editor font (loading...):")
    @Key("fontFaceEditorFontLabel")
    String fontFaceEditorFontLabel();

    /**
     * Translated "Editor font:".
     *
     * @return translated "Editor font:"
     */
    @DefaultStringValue("Editor font:")
    @Key("appearanceEditorFontLabel")
    String appearanceEditorFontLabel();

    /**
     * Translated "Editor font size:".
     *
     * @return translated "Editor font size:"
     */
    @DefaultStringValue("Editor font size:")
    @Key("appearanceEditorFontSizeLabel")
    String appearanceEditorFontSizeLabel();

    /**
     * Translated "Editor theme:".
     *
     * @return translated "Editor theme:"
     */
    @DefaultStringValue("Editor theme:")
    @Key("appearanceEditorThemeLabel")
    String appearanceEditorThemeLabel();

    /**
     * Translated "Add...".
     *
     * @return translated "Add..."
     */
    @DefaultStringValue("Add...")
    @Key("addThemeButtonLabel")
    String addThemeButtonLabel();

    /**
     * Translated "Theme Files (*.tmTheme *.rstheme)".
     *
     * @return translated "Theme Files (*.tmTheme *.rstheme)"
     */
    @DefaultStringValue("Theme Files (*.tmTheme *.rstheme)")
    @Key("addThemeButtonCaption")
    String addThemeButtonCaption();

    /**
     * Translated "Remove".
     *
     * @return translated "Remove"
     */
    @DefaultStringValue("Remove")
    @Key("removeThemeButtonLabel")
    String removeThemeButtonLabel();

    /**
     * Translated "Converting a tmTheme to an rstheme".
     *
     * @return translated "Converting a tmTheme to an rstheme"
     */
    @DefaultStringValue("Converting a tmTheme to an rstheme")
    @Key("addThemeUserActionLabel")
    String addThemeUserActionLabel();

    /**
     * Translated "The active theme".
     *
     * @return translated "The active theme"
     */
    @DefaultStringValue("The active theme \"")
    @Key("setThemeWarningMessage")
    String setThemeWarningMessage();

    /**
     * Translated " could not be found. It's possible it was removed outside the context of RStudio. Switching to the ".
     *
     * @return translated " could not be found. It's possible it was removed outside the context of RStudio. Switching to the "
     */
    @DefaultStringValue("\"could not be found. It's possible it was removed outside the context of RStudio. Switching to the ")
    @Key("themeWarningMessage")
    String themeWarningMessage();

    /**
     * Translated "dark ".
     *
     * @return translated "dark "
     */
    @DefaultStringValue("dark ")
    @Key("themeWarningMessageDarkLabel")
    String themeWarningMessageDarkLabel();

    /**
     * Translated "light ".
     *
     * @return translated "light "
     */
    @DefaultStringValue("light ")
    @Key("themeWarningMessageLightLabel")
    String themeWarningMessageLightLabel();

    /**
     * Translated "default theme: "".
     *
     * @return translated "default theme: ""
     */
    @DefaultStringValue("default theme: \"")
    @Key("themeWarningMessageDefaultLabel")
    String themeWarningMessageDefaultLabel();

    /**
     * Translated "The theme "".
     *
     * @return translated "The theme ""
     */
    @DefaultStringValue("The theme \"")
    @Key("updateThemeLogWarning")
    String updateThemeLogWarning();

    /**
     * Translated "" does not exist. It may have been manually deleted outside the context of RStudio.".
     *
     * @return translated "" does not exist. It may have been manually deleted outside the context of RStudio."
     */
    @DefaultStringValue("\" does not exist. It may have been manually deleted outside the context of RStudio.")
    @Key("updateThemeLogWarningLabel")
    String updateThemeLogWarningLabel();

    /**
     * Translated "A theme file with the same name, '".
     *
     * @return translated "A theme file with the same name, '"
     */
    @DefaultStringValue("A theme file with the same name, '")
    @Key("showThemeExistsDialogLabel")
    String showThemeExistsDialogLabel();

    /**
     * Translated ", already exists. Adding the theme will cause the existing file to be ".
     *
     * @return translated ", already exists. Adding the theme will cause the existing file to be "
     */
    @DefaultStringValue(", already exists. Adding the theme will cause the existing file to be ")
    @Key("showThemeExistsExistsLabel")
    String showThemeExistsExistsLabel();

    /**
     * Translated "overwritten. Would you like to add the theme anyway?".
     *
     * @return translated "overwritten. Would you like to add the theme anyway?"
     */
    @DefaultStringValue("overwritten. Would you like to add the theme anyway?")
    @Key("showThemeExistsOverWriteLabel")
    String showThemeExistsOverWriteLabel();

    /**
     * Translated "Theme File Already Exists".
     *
     * @return translated "Theme File Already Exists"
     */
    @DefaultStringValue("Theme File Already Exists")
    @Key("globalDisplayThemeExistsCaption")
    String globalDisplayThemeExistsCaption();

    /**
     * Translated "Unable to add the theme '".
     *
     * @return translated "Unable to add the theme '"
     */
    @DefaultStringValue("Unable to add the theme '")
    @Key("cantAddThemeMessage")
    String cantAddThemeMessage();


    /**
     * Translated "'. The following error occurred: ".
     *
     * @return translated "'. The following error occurred: "
     */
    @DefaultStringValue("'. The following error occurred: ")
    @Key("cantAddThemeErrorCaption")
    String cantAddThemeErrorCaption();

    /**
     * Translated "Failed to Add Theme".
     *
     * @return translated "Failed to Add Theme"
     */
    @DefaultStringValue("Failed to Add Theme")
    @Key("cantAddThemeGlobalMessage")
    String cantAddThemeGlobalMessage();

    /**
     * Translated "Unable to remove the theme ".
     *
     * @return translated "Unable to remove the theme "
     */
    @DefaultStringValue("Unable to remove the theme ")
    @Key("showCantRemoveThemeDialogMessage")
    String showCantRemoveThemeDialogMessage();

    /**
     * Translated "Failed to Remove Theme".
     *
     * @return translated "Failed to Remove Theme"
     */
    @DefaultStringValue("Failed to Remove Theme")
    @Key("showCantRemoveErrorMessage")
    String showCantRemoveErrorMessage();

    /**
     * Translated "The theme "".
     *
     * @return translated "The theme ""
     */
    @DefaultStringValue("The theme \"")
    @Key("showCantRemoveActiveThemeDialog")
    String showCantRemoveActiveThemeDialog();

    /**
     * Translated "" cannot be removed because it is currently in use. To delete this theme,".
     *
     * @return translated "" cannot be removed because it is currently in use. To delete this theme,"
     */
    @DefaultStringValue("\" cannot be removed because it is currently in use. To delete this theme,")
    @Key("showCantRemoveActiveThemeDialog")
    String showCantRemoveActiveThemeMessage();

    /**
     * Translated "please change the active theme and retry.".
     *
     * @return translated "please change the active theme and retry."
     */
    @DefaultStringValue("please change the active theme and retry.")
    @Key("showCantRemoveActiveThemeRetryMessage")
    String showCantRemoveActiveThemeRetryMessage();

    /**
     * Translated "Cannot Remove Active Theme".
     *
     * @return translated "Cannot Remove Active Theme"
     */
    @DefaultStringValue("Cannot Remove Active Theme")
    @Key("showCantRemoveThemeCaption")
    String showCantRemoveThemeCaption();

    /**
     * Translated "Taking this action will delete the theme "".
     *
     * @return translated "Taking this action will delete the theme ""
     */
    @DefaultStringValue("Taking this action will delete the theme \"")
    @Key("showRemoveThemeWarningMessage")
    String showRemoveThemeWarningMessage();

    /**
     * Translated "" and cannot be undone. Are you sure you wish to continue?".
     *
     * @return translated "" and cannot be undone. Are you sure you wish to continue?"
     */
    @DefaultStringValue("\" and cannot be undone. Are you sure you wish to continue?")
    @Key("showRemoveThemeWarningQuestionMessage")
    String showRemoveThemeWarningQuestionMessage();

    /**
     * Translated "Remove Theme".
     *
     * @return translated "Remove Theme"
     */
    @DefaultStringValue("Remove Theme")
    @Key("showRemoveThemeGlobalMessage")
    String showRemoveThemeGlobalMessage();

    /**
     * Translated "There is an existing theme with the same name as the new theme in the current".
     *
     * @return translated "There is an existing theme with the same name as the new theme in the current"
     */
    @DefaultStringValue("There is an existing theme with the same name as the new theme in the current")
    @Key("showDuplicateThemeErrorMessage")
    String showDuplicateThemeErrorMessage();

    /**
     * Translated "location. Would you like to remove the existing theme, "".
     *
     * @return translated "location. Would you like to remove the existing theme, ""
     */
    @DefaultStringValue("location. Would you like to remove the existing theme, ")
    @Key("showDuplicateThemeErrorQuestionMessage")
    String showDuplicateThemeErrorQuestionMessage();

    /**
     * Translated "", and add the new theme?".
     *
     * @return translated "", and add the new theme?"
     */
    @DefaultStringValue("\", and add the new theme?")
    @Key("showDuplicateThemeErrorAddThemeMessage")
    String showDuplicateThemeErrorAddThemeMessage();

    /**
     * Translated "Duplicate Theme In Same Location".
     *
     * @return translated "Duplicate Theme In Same Location"
     */
    @DefaultStringValue("Duplicate Theme In Same Location")
    @Key("showDuplicateThemeDuplicateGlobalMessage")
    String showDuplicateThemeDuplicateGlobalMessage();

    /**
     * Translated "There is an existing theme with the same name as the new theme, ".
     *
     * @return translated "There is an existing theme with the same name as the new theme, "
     */
    @DefaultStringValue("There is an existing theme with the same name as the new theme, ")
    @Key("showDuplicateThemeWarningMessage")
    String showDuplicateThemeWarningMessage();

    /**
     * Translated "" in another location. The existing theme will be hidden but not removed.".
     *
     * @return translated "" in another location. The existing theme will be hidden but not removed."
     */
    @DefaultStringValue("\" in another location. The existing theme will be hidden but not removed.")
    @Key("showDuplicateThemeExistingMessage")
    String showDuplicateThemeExistingMessage();

    /**
     * Translated "Removing the new theme later will un-hide the existing theme. Would you".
     *
     * @return translated "Removing the new theme later will un-hide the existing theme. Would you"
     */
    @DefaultStringValue("Removing the new theme later will un-hide the existing theme. Would you")
    @Key("showDuplicateThemeQuestionMessage")
    String showDuplicateThemeQuestionMessage();

    /**
     * Translated "like to continue?".
     *
     * @return translated "like to continue?"
     */
    @DefaultStringValue("like to continue?")
    @Key("showDuplicateContinueThemeMessage")
    String showDuplicateContinueThemeMessage();

    /**
     * Translated "Duplicate Theme In Another Location".
     *
     * @return translated "Duplicate Theme In Another Location"
     */
    @DefaultStringValue("Duplicate Theme In Another Location")
    @Key("showDuplicateThemeGlobalMessage")
    String showDuplicateThemeGlobalMessage();

    /**
     * Translated "Appearance".
     *
     * @return translated "Appearance"
     */
    @DefaultStringValue("Appearance")
    @Key("appearanceLabel")
    String appearanceLabel();

    /**
     * Translated "Editor font:".
     *
     * @return translated "Editor font:"
     */
    @DefaultStringValue("Editor font:")
    @Key("editorFontLabel")
    String editorFontLabel();
}

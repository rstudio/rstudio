package org.rstudio.studio.client.common.spelling.ui;

public interface SpellingConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "Main dictionary language:".
     *
     * @return translated "Main dictionary language:"
     */
    @DefaultStringValue("Main dictionary language:")
    @Key("spellingLanguageSelectWidgetLabel")
    String spellingLanguageSelectWidgetLabel();

    /**
     * Translated "Help on spelling dictionaries".
     *
     * @return translated "Help on spelling dictionaries"
     */
    @DefaultStringValue("Help on spelling dictionaries")
    @Key("addHelpButtonLabel")
    String addHelpButtonLabel();

    /**
     * Translated "Downloading dictionaries...".
     *
     * @return translated "Downloading dictionaries..."
     */
    @DefaultStringValue("Downloading dictionaries...")
    @Key("progressDownloadingLabel")
    String progressDownloadingLabel();

    /**
     * Translated "Downloading dictionaries...".
     *
     * @return translated "Downloading dictionaries..."
     */
    @DefaultStringValue("Downloading additional languages...")
    @Key("progressDownloadingLanguagesLabel")
    String progressDownloadingLanguagesLabel();

    /**
     * Translated "Error Downloading Dictionaries".
     *
     * @return translated "Error Downloading Dictionaries"
     */
    @DefaultStringValue("Error Downloading Dictionaries")
    @Key("onErrorDownloadingCaption")
    String onErrorDownloadingCaption();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    @DefaultStringValue("(Default)")
    @Key("includeDefaultOption")
    String includeDefaultOption();

    /**
     * Translated "Update Dictionaries...".
     *
     * @return translated "Update Dictionaries..."
     */
    @DefaultStringValue("Update Dictionaries...")
    @Key("allLanguagesInstalledOption")
    String allLanguagesInstalledOption();

    /**
     * Translated "Install More Languages...".
     *
     * @return translated "Install More Languages..."
     */
    @DefaultStringValue("Install More Languages...")
    @Key("installIndexOption")
    String installIndexOption();

    /**
     * Translated "Add...".
     *
     * @return translated "Add..."
     */
    @DefaultStringValue("Add...")
    @Key("buttonAddLabel")
    String buttonAddLabel();

    /**
     * Translated "Remove...".
     *
     * @return translated "Remove..."
     */
    @DefaultStringValue("Remove...")
    @Key("buttonRemoveLabel")
    String buttonRemoveLabel();

    /**
     * Translated "Custom dictionaries:".
     *
     * @return translated "Custom dictionaries:"
     */
    @DefaultStringValue("Custom dictionaries:")
    @Key("labelWithHelpText")
    String labelWithHelpText();

    /**
     * Translated "Help on custom spelling dictionaries".
     *
     * @return translated "Help on custom spelling dictionaries"
     */
    @DefaultStringValue("Help on custom spelling dictionaries")
    @Key("labelWithHelpTitle")
    String labelWithHelpTitle();

    /**
     * Translated "Add Custom Dictionary (*.dic)".
     *
     * @return translated "Add Custom Dictionary (*.dic)"
     */
    @DefaultStringValue("Add Custom Dictionary (*.dic)")
    @Key("fileDialogsCaption")
    String fileDialogsCaption();

    /**
     * Translated "Add Custom Dictionary (*.dic)".
     *
     * @return translated "Add Custom Dictionary (*.dic)"
     */
    @DefaultStringValue("Dictionaries (*.dic)")
    @Key("fileDialogsFilter")
    String fileDialogsFilter();

    /**
     * Translated "Adding dictionary...".
     *
     * @return translated "Adding dictionary..."
     */
    @DefaultStringValue("Adding dictionary...")
    @Key("onProgressAddingLabel")
    String onProgressAddingLabel();

    /**
     * Translated "Confirm Remove".
     *
     * @return translated "Confirm Remove"
     */
    @DefaultStringValue("Confirm Remove")
    @Key("removeDictionaryCaption")
    String removeDictionaryCaption();

    /**
     * Translated "Are you sure you want to remove the ".
     *
     * @return translated "Are you sure you want to remove the "
     */
    @DefaultStringValue("Are you sure you want to remove the ")
    @Key("removeDictionaryMessage")
    String removeDictionaryMessage();

    /**
     * Translated "custom dictionary?".
     *
     * @return translated "custom dictionary?"
     */
    @DefaultStringValue("custom dictionary?")
    @Key("removeCustomDictionaryMessage")
    String removeCustomDictionaryMessage();

    /**
     * Translated "Removing dictionary...".
     *
     * @return translated "Removing dictionary..."
     */
    @DefaultStringValue("Removing dictionary...")
    @Key("progressRemoveIndicator")
    String progressRemoveIndicator();
}

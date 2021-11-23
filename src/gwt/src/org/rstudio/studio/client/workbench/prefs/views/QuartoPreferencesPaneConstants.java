package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.i18n.client.Constants;

public interface QuartoPreferencesPaneConstants extends Constants {

    /**
     * Get locale value for name (default "Quarto").
     *
     * @return translated value for name
     */
    @DefaultStringValue("Quarto")
    @Key("name")
    String name();

    /**
     * Get locale value for the Quarto preview label. Default value
     * is "This version of RStudio includes a preview of Quarto, a
     * new scientific and technical publishing system. "
     *
     * @return translated value for Quarto preview label
     */
    @DefaultStringValue("This version of RStudio includes a preview of Quarto, a new scientific and technical publishing system. ")
    @Key("quartoPreviewLabel")
    String quartoPreviewLabel();

    /**
     * Get locale value of the label for the checkbox to enable the
     * Quarto preview. Default value is "Enable Quarto preview".
     *
     * @return the translated value for the label
     */
    @DefaultStringValue("Enable Quarto preview")
    @Key("enableQuartoPreviewCheckboxLabel")
    String enableQuartoPreviewCheckboxLabel();

    /**
     * Get locale value for the help link caption. Default value
     * is "Learn more about Quarto".
     *
     * @return the translated value for the help link
     */
    @DefaultStringValue("Learn more about Quarto")
    @Key("helpLinkCaption")
    String helpLinkCaption();
}

/*
 * DataViewerConstants.java
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

package org.rstudio.studio.client.quarto;

import com.google.gwt.i18n.client.Messages;

public interface QuartoConstants extends Messages {

    /**
     * Translate "Presentation".
     *
     * @return the translated value
     */
    @DefaultMessage("Presentation")
    @Key("presentationLabel")
    String presentationLabel();

    /**
     * Translate "Document".
     *
     * @return the translated value
     */
    @DefaultMessage("Document")
    @Key("documentLabel")
    String documentLabel();

    /**
     * Translate "Interactive".
     *
     * @return the translated value
     */
    @DefaultMessage("Interactive")
    @Key("interactiveLabel")
    String interactiveLabel();

    /**
     * Translate "New Quarto Document".
     *
     * @return the translated value
     */
    @DefaultMessage("New Quarto Document")
    @Key("newQuartoDocumentCaption")
    String newQuartoDocumentCaption();

    /**
     * Translate "Create".
     *
     * @return the translated value
     */
    @DefaultMessage("Create")
    @Key("createDocButtonCaption")
    String createDocButtonCaption();

    /**
     * Translate "(optional)".
     *
     * @return the translated value
     */
    @DefaultMessage("(optional)")
    @Key("newDocAuthorPlaceholderText")
    String newDocAuthorPlaceholderText();

    /**
     * Translate "Templates".
     *
     * @return the translated value
     */
    @DefaultMessage("Templates")
    @Key("templateAriaLabelValue")
    String templateAriaLabelValue();

    /**
     * Translate "Engine:".
     *
     * @return the translated value
     */
    @DefaultMessage("Engine:")
    @Key("engineLabelCaption")
    String engineLabelCaption();

    /**
     * Translate "(None)".
     *
     * @return the translated value
     */
    @DefaultMessage("(None)")
    @Key("engineSelectNoneLabel")
    String engineSelectNoneLabel();

    /**
     * Translate "Kernel:".
     *
     * @return the translated value
     */
    @DefaultMessage("Kernel:")
    @Key("kernelLabelCaption")
    String kernelLabelCaption();

    /**
     * Translate "Learn more about Quarto".
     *
     * @return the translated value
     */
    @DefaultMessage("Learn more about Quarto")
    @Key("learnMoreLinkCaption")
    String learnMoreLinkCaption();

    /**
     * Translate "Learn more about Quarto presentations".
     *
     * @return the translated value
     */
    @DefaultMessage("Learn more about Quarto presentations")
    @Key("learnMorePresentationsLinkCaption")
    String learnMorePresentationsLinkCaption();

    /**
     * Translate "Learn more about Quarto interactive documents".
     *
     * @return the translated value
     */
    @DefaultMessage("Learn more about Quarto interactive documents")
    @Key("learnMoreInteractiveDocsLinkCaption")
    String learnMoreInteractiveDocsLinkCaption();

    /**
     * Translate "Create Empty Document".
     *
     * @return the translated value
     */
    @DefaultMessage("Create Empty Document")
    @Key("createEmptyDocButtonTitle")
    String createEmptyDocButtonTitle();

    /**
     * Translate "Title Required".
     *
     * @return the translated value
     */
    @DefaultMessage("Title Required")
    @Key("titleRequiredErrorCaption")
    String titleRequiredErrorCaption();

    /**
     * Translate "You must provide a title for the document".
     *
     * @return the translated value
     */
    @DefaultMessage("You must provide a title for the document")
    @Key("titleRequiredErrorMessage")
    String titleRequiredErrorMessage();

    /**
     * Translate "HTML".
     *
     * @return the translated value
     */
    @DefaultMessage("HTML")
    @Key("htmlFormatText")
    String htmlFormatText();

    /**
     * Translate "Recommended format for authoring (you can switch to PDF or Word output anytime)"
     *
     * @return the translated value
     */
    @DefaultMessage("Recommended format for authoring (you can switch to PDF or Word output anytime)")
    @Key("htmlFormatDesc")
    String htmlFormatDesc();

    /**
     * Translate "PDF".
     *
     * @return the translated value
     */
    @DefaultMessage("PDF")
    @Key("pdfFormatText")
    String pdfFormatText();

    /**
     * Translate "PDF output requires a LaTeX installation (e.g. https://yihui.org/tinytex/)".
     *
     * @return the translated value
     */
    @DefaultMessage("PDF output requires a LaTeX installation (e.g. https://yihui.org/tinytex/)")
    @Key("pdfFormatDesc")
    String pdfFormatDesc();

    /**
     * Translate "Word".
     *
     * @return the translated value
     */
    @DefaultMessage("Word")
    @Key("wordFormatText")
    String wordFormatText();

    /**
     * Translate "Previewing Word documents requires an installation of MS Word (or Libre/Open Office on Linux)"
     *
     * @return the translated value
     */
    @DefaultMessage("Previewing Word documents requires an installation of MS Word (or Libre/Open Office on Linux)")
    @Key("wordFormatDesc")
    String wordFormatDesc();

    /**
     * Translate "Reveal JS".
     *
     * @return the translated value
     */
    @DefaultMessage("Reveal JS")
    @Key("jsFormatText")
    String jsFormatText();

    /**
     * Translate "HTML presentation viewable with any browser (you can also print to PDF with Chrome)"
     *
     * @return the translated value
     */
    @DefaultMessage("HTML presentation viewable with any browser (you can also print to PDF with Chrome)")
    @Key("jsFormatDesc")
    String jsFormatDesc();

    /**
     * Translate "Beamer".
     *
     * @return the translated value
     */
    @DefaultMessage("Beamer")
    @Key("beamerFormatText")
    String beamerFormatText();

    /**
     * Translate "PDF output requires a LaTeX installation (e.g. https://yihui.org/tinytex/)"
     *
     * @return the translated value
     */
    @DefaultMessage("PDF output requires a LaTeX installation (e.g. https://yihui.org/tinytex/)")
    @Key("beamerFormatDesc")
    String beamerFormatDesc();

    /**
     * Translate "PowerPoint".
     *
     * @return the translated value
     */
    @DefaultMessage("PowerPoint")
    @Key("powerPointFormatText")
    String powerPointFormatText();

    /**
     * Translate "PowerPoint previewing requires an installation of PowerPoint or OpenOffice".
     *
     * @return the translated value
     */
    @DefaultMessage("PowerPoint previewing requires an installation of PowerPoint or OpenOffice")
    @Key("powerPointFormatDesc")
    String powerPointFormatDesc();

    /**
     * Translate "Create an interactive HTML document with Shiny components.".
     *
     * @return the translated value
     */
    @DefaultMessage("Create an interactive HTML document with Shiny components")
    @Key("shinyFormatDesc")
    String shinyFormatDesc();

    /**
     * Translate "Create an interactive HTML document with Observable JS components".
     *
     * @return the translated value
     */
    @DefaultMessage("Create an interactive HTML document with Observable JS components")
    @Key("observableJSFormatDesc")
    String observableJSFormatDesc();

    /**
     * Translate "Use visual markdown editor".
     *
     * @return the translated value
     */
    @DefaultMessage("Use visual markdown editor")
    @Key("chkVisualEditorLabel")
    String chkVisualEditorLabel();

    /**
     * Translate "About the Quarto visual editor".
     *
     * @return the translated value
     */
    @DefaultMessage("About the Quarto visual editor")
    @Key("aboutHelpButtonTitle")
    String aboutHelpButtonTitle();

    /**
     * Translate "Editor:".
     *
     * @return the translated value
     */
    @DefaultMessage("Editor:")
    @Key("editorText")
    String editorText();
}

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
    String presentationLabel();

    /**
     * Translate "Document".
     *
     * @return the translated value
     */
    @DefaultMessage("Document")
    String documentLabel();

    /**
     * Translate "Interactive".
     *
     * @return the translated value
     */
    @DefaultMessage("Interactive")
    String interactiveLabel();

    /**
     * Translate "New Quarto Document".
     *
     * @return the translated value
     */
    @DefaultMessage("New Quarto Document")
    String newQuartoDocumentCaption();

    /**
     * Translate "Create".
     *
     * @return the translated value
     */
    @DefaultMessage("Create")
    String createDocButtonCaption();

    /**
     * Translate "(optional)".
     *
     * @return the translated value
     */
    @DefaultMessage("(optional)")
    String newDocAuthorPlaceholderText();

    /**
     * Translate "Templates".
     *
     * @return the translated value
     */
    @DefaultMessage("Templates")
    String templateAriaLabelValue();

    /**
     * Translate "Engine:".
     *
     * @return the translated value
     */
    @DefaultMessage("Engine:")
    String engineLabelCaption();

    /**
     * Translate "(None)".
     *
     * @return the translated value
     */
    @DefaultMessage("(None)")
    String engineSelectNoneLabel();

    /**
     * Translate "Kernel:".
     *
     * @return the translated value
     */
    @DefaultMessage("Kernel:")
    String kernelLabelCaption();

    /**
     * Translate "Learn more about Quarto".
     *
     * @return the translated value
     */
    @DefaultMessage("Learn more about Quarto")
    String learnMoreLinkCaption();

    /**
     * Translate "Learn more about Quarto presentations".
     *
     * @return the translated value
     */
    @DefaultMessage("Learn more about Quarto presentations")
    String learnMorePresentationsLinkCaption();

    /**
     * Translate "Learn more about Quarto interactive documents".
     *
     * @return the translated value
     */
    @DefaultMessage("Learn more about Quarto interactive documents")
    String learnMoreInteractiveDocsLinkCaption();

    /**
     * Translate "Create Empty Document".
     *
     * @return the translated value
     */
    @DefaultMessage("Create Empty Document")
    String createEmptyDocButtonTitle();

    /**
     * Translate "Title Required".
     *
     * @return the translated value
     */
    @DefaultMessage("Title Required")
    String titleRequiredErrorCaption();

    /**
     * Translate "You must provide a title for the document".
     *
     * @return the translated value
     */
    @DefaultMessage("You must provide a title for the document")
    String titleRequiredErrorMessage();

    /**
     * Translate "HTML".
     *
     * @return the translated value
     */
    @DefaultMessage("HTML")
    String htmlFormatText();

    /**
     * Translate "Recommended format for authoring (you can switch to PDF or Word output anytime)"
     *
     * @return the translated value
     */
    @DefaultMessage("Recommended format for authoring (you can switch to PDF or Word output anytime)")
    String htmlFormatDesc();

    /**
     * Translate "PDF".
     *
     * @return the translated value
     */
    @DefaultMessage("PDF")
    String pdfFormatText();

    /**
     * Translate "PDF output requires a LaTeX installation (e.g. https://yihui.org/tinytex/)".
     *
     * @return the translated value
     */
    @DefaultMessage("PDF output requires a LaTeX installation (e.g. https://yihui.org/tinytex/)")
    String pdfFormatDesc();

    /**
     * Translate "Word".
     *
     * @return the translated value
     */
    @DefaultMessage("Word")
    String wordFormatText();

    /**
     * Translate "Previewing Word documents requires an installation of MS Word (or Libre/Open Office on Linux)"
     *
     * @return the translated value
     */
    @DefaultMessage("Previewing Word documents requires an installation of MS Word (or Libre/Open Office on Linux)")
    String wordFormatDesc();

    /**
     * Translate "Reveal JS".
     *
     * @return the translated value
     */
    @DefaultMessage("Reveal JS")
    String jsFormatText();

    /**
     * Translate "HTML presentation viewable with any browser (you can also print to PDF with Chrome)"
     *
     * @return the translated value
     */
    @DefaultMessage("HTML presentation viewable with any browser (you can also print to PDF with Chrome)")
    String jsFormatDesc();

    /**
     * Translate "Beamer".
     *
     * @return the translated value
     */
    @DefaultMessage("Beamer")
    String beamerFormatText();

    /**
     * Translate "PDF output requires a LaTeX installation (e.g. https://yihui.org/tinytex/)"
     *
     * @return the translated value
     */
    @DefaultMessage("PDF output requires a LaTeX installation (e.g. https://yihui.org/tinytex/)")
    String beamerFormatDesc();

    /**
     * Translate "PowerPoint".
     *
     * @return the translated value
     */
    @DefaultMessage("PowerPoint")
    String powerPointFormatText();

    /**
     * Translate "PowerPoint previewing requires an installation of PowerPoint or OpenOffice".
     *
     * @return the translated value
     */
    @DefaultMessage("PowerPoint previewing requires an installation of PowerPoint or OpenOffice")
    String powerPointFormatDesc();

    /**
     * Translate "Create an interactive HTML document with Shiny components.".
     *
     * @return the translated value
     */
    @DefaultMessage("Create an interactive HTML document with Shiny components")
    String shinyFormatDesc();

    /**
     * Translate "Create an interactive HTML document with Observable JS components".
     *
     * @return the translated value
     */
    @DefaultMessage("Create an interactive HTML document with Observable JS components")
    String observableJSFormatDesc();

    /**
     * Translate "Use visual markdown editor".
     *
     * @return the translated value
     */
    @DefaultMessage("Use visual markdown editor")
    String chkVisualEditorLabel();

    /**
     * Translate "About the Quarto visual editor".
     *
     * @return the translated value
     */
    @DefaultMessage("About the Quarto visual editor")
    String aboutHelpButtonTitle();

    /**
     * Translate "Editor:".
     *
     * @return the translated value
     */
    @DefaultMessage("Editor:")
    String editorText();
}

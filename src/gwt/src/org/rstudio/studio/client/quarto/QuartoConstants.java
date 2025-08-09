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
    String presentationLabel();

    /**
     * Translate "Document".
     *
     * @return the translated value
     */
    String documentLabel();

    /**
     * Translate "Interactive".
     *
     * @return the translated value
     */
    String interactiveLabel();

    /**
     * Translate "New Quarto Document".
     *
     * @return the translated value
     */
    String newQuartoDocumentCaption();

    /**
     * Translate "Create".
     *
     * @return the translated value
     */
    String createDocButtonCaption();

    /**
     * Translate "(optional)".
     *
     * @return the translated value
     */
    String newDocAuthorPlaceholderText();

    /**
     * Translate "Templates".
     *
     * @return the translated value
     */
    String templateAriaLabelValue();

    /**
     * Translate "Engine:".
     *
     * @return the translated value
     */
    String engineLabelCaption();

    /**
     * Translate "(None)".
     *
     * @return the translated value
     */
    String engineSelectNoneLabel();

    /**
     * Translate "Kernel:".
     *
     * @return the translated value
     */
    String kernelLabelCaption();

    /**
     * Translate "Learn more about Quarto".
     *
     * @return the translated value
     */
    String learnMoreLinkCaption();

    /**
     * Translate "Learn more about Quarto presentations".
     *
     * @return the translated value
     */
    String learnMorePresentationsLinkCaption();

    /**
     * Translate "Learn more about Quarto interactive documents".
     *
     * @return the translated value
     */
    String learnMoreInteractiveDocsLinkCaption();

    /**
     * Translate "Create Empty Document".
     *
     * @return the translated value
     */
    String createEmptyDocButtonTitle();

    /**
     * Translate "Title Required".
     *
     * @return the translated value
     */
    String titleRequiredErrorCaption();

    /**
     * Translate "You must provide a title for the document".
     *
     * @return the translated value
     */
    String titleRequiredErrorMessage();

    /**
     * Translate "HTML".
     *
     * @return the translated value
     */
    String htmlFormatText();

    /**
     * Translate "Recommended format for authoring (you can switch to PDF or Word output anytime)"
     *
     * @return the translated value
     */
    String htmlFormatDesc();

    /**
     * Translate "PDF".
     *
     * @return the translated value
     */
    String pdfFormatText();

    /**
     * Translate "PDF output requires a LaTeX installation (e.g. https://yihui.org/tinytex/)".
     *
     * @return the translated value
     */
    String pdfFormatDesc();

    /**
     * Translate "Word".
     *
     * @return the translated value
     */
    String wordFormatText();

    /**
     * Translate "Previewing Word documents requires an installation of MS Word (or Libre/Open Office on Linux)"
     *
     * @return the translated value
     */
    String wordFormatDesc();

    /**
     * Translate "Reveal JS".
     *
     * @return the translated value
     */
    String jsFormatText();

    /**
     * Translate "HTML presentation viewable with any browser (you can also print to PDF with Chrome)"
     *
     * @return the translated value
     */
    String jsFormatDesc();

    /**
     * Translate "Beamer".
     *
     * @return the translated value
     */
    String beamerFormatText();

    /**
     * Translate "PDF output requires a LaTeX installation (e.g. https://yihui.org/tinytex/)"
     *
     * @return the translated value
     */
    String beamerFormatDesc();

    /**
     * Translate "PowerPoint".
     *
     * @return the translated value
     */
    String powerPointFormatText();

    /**
     * Translate "PowerPoint previewing requires an installation of PowerPoint or OpenOffice".
     *
     * @return the translated value
     */
    String powerPointFormatDesc();

    /**
     * Translate "Create an interactive HTML document with Shiny components.".
     *
     * @return the translated value
     */
    String shinyFormatDesc();

    /**
     * Translate "Create an interactive HTML document with Observable JS components".
     *
     * @return the translated value
     */
    String observableJSFormatDesc();

    /**
     * Translate "Use visual markdown editor".
     *
     * @return the translated value
     */
    String chkVisualEditorLabel();

    /**
     * Translate "About the Quarto visual editor".
     *
     * @return the translated value
     */
    String aboutHelpButtonTitle();

    /**
     * Translate "Editor:".
     *
     * @return the translated value
     */
    String editorText();
}

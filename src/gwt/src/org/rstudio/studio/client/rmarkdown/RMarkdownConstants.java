/*
 * DataViewerConstants.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.rmarkdown;

import com.google.gwt.i18n.client.Messages;

public interface RMarkdownConstants extends Messages {

    /**
     * Translate "Shiny Terminate Failed".
     *
     * @return the translated value
     */
    @DefaultMessage("Shiny Terminate Failed")
    @Key("shinyTerminalErrorCaption")
    String shinyTerminalErrorCaption();

    /**
     * Translate "The Shiny document {0} needs to be stopped before the document can be rendered.".
     *
     * @return the translated value
     */
    @DefaultMessage("The Shiny document {0} needs to be stopped before the document can be rendered.")
    @Key("shinyTerminalErrorMsg")
    String shinyTerminalErrorMsg(String file);

    /**
     * Translate "Render Completed".
     *
     * @return the translated value
     */
    @DefaultMessage("Render Completed")
    @Key("renderCompletedCaption")
    String renderCompletedCaption();

    /**
     * Translate "RStudio has finished rendering {0} to {1}.".
     *
     * @return the translated value
     */
    @DefaultMessage("RStudio has finished rendering {0} to {1}.")
    @Key("renderCompletedMsg")
    String renderCompletedMsg(String targetFile, String outputFile);

    /**
     * Translate "Download File".
     *
     * @return the translated value
     */
    @DefaultMessage("Download File")
    @Key("yesLabel")
    String yesLabel();

    /**
     * Translate "OK".
     *
     * @return the translated value
     */
    @DefaultMessage("OK")
    @Key("noLabel")
    String noLabel();

    /**
     * Translate "Rmd Output Panel".
     *
     * @return the translated value
     */
    @DefaultMessage("Rmd Output Panel")
    @Key("rmdOutputPanelTitle")
    String rmdOutputPanelTitle();

    /**
     * Translate "Open in Browser".
     *
     * @return the translated value
     */
    @DefaultMessage("Open in Browser")
    @Key("openInBrowserButtonText")
    String openInBrowserButtonText();

    /**
     * Translate "Find".
     *
     * @return the translated value
     */
    @DefaultMessage("Find")
    @Key("findTextBoxCueText")
    String findTextBoxCueText();

    /**
     * Translate "Find in Page".
     *
     * @return the translated value
     */
    @DefaultMessage("Find in Page")
    @Key("findInPageCaption")
    String findInPageCaption();

    /**
     * Translate "No occurrences found".
     *
     * @return the translated value
     */
    @DefaultMessage("No occurrences found")
    @Key("noOccurrencesFoundMsg")
    String noOccurrencesFoundMsg();

    /**
     * Translate "Template:".
     *
     * @return the translated value
     */
    @DefaultMessage("Template:")
    @Key("helpCaptionTemplateText")
    String helpCaptionTemplateText();

    /**
     * Translate "Using R Markdown Templates".
     *
     * @return the translated value
     */
    @DefaultMessage("Using R Markdown Templates")
    @Key("helpCationTemplateMsg")
    String helpCationTemplateMsg();

    /**
     * Translate "R Markdown Templates Not Found".
     *
     * @return the translated value
     */
    @DefaultMessage("R Markdown Templates Not Found")
    @Key("templatesNotFoundErrorCaption")
    String templatesNotFoundErrorCaption();

    /**
     * Translate "An error occurred while looking for R Markdown templates. {0}".
     *
     * @return the translated value
     */
    @DefaultMessage("An error occurred while looking for R Markdown templates. {0}")
    @Key("templatesNotFoundErrorMsg")
    String templatesNotFoundErrorMsg(String error);

    /**
     * Translate "Edit {0} {1} Options".
     *
     * @return the translated value
     */
    @DefaultMessage("Edit {0} {1} Options")
    @Key("editTemplateOptionsCaption")
    String editTemplateOptionsCaption(String type, String templateName);

    /**
     * Translate "R Markdown Options".
     *
     * @return the translated value
     */
    @DefaultMessage("R Markdown Options")
    @Key("rMarkdownOptionstabListLabel")
    String rMarkdownOptionstabListLabel();

    /**
     * Translate "Shiny Content".
     *
     * @return the translated value
     */
    @DefaultMessage("Shiny Content")
    @Key("warningDialogText")
    String warningDialogText();

    /**
     * Translate "Yes, Once".
     *
     * @return the translated value
     */
    @DefaultMessage("Yes, Once")
    @Key("yesOnceButtonText")
    String yesOnceButtonText();

    /**
     * Translate "Yes, Always".
     *
     * @return the translated value
     */
    @DefaultMessage("Yes, Always")
    @Key("yesAlwaysButtonText")
    String yesAlwaysButtonText();

    /**
     * Translate "No".
     *
     * @return the translated value
     */
    @DefaultMessage("No")
    @Key("noButtonText")
    String noButtonText();

    /**
     * Translate "Location:".
     *
     * @return the translated value
     */
    @DefaultMessage("Location:")
    @Key("locationLabel")
    String locationLabel();
}

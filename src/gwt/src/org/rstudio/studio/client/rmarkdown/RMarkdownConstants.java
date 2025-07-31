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

package org.rstudio.studio.client.rmarkdown;

import com.google.gwt.i18n.client.Messages;

public interface RMarkdownConstants extends Messages {

    /**
     * Translate "Shiny Terminate Failed".
     *
     * @return the translated value
     */
    @Key("shinyTerminalErrorCaption")
    String shinyTerminalErrorCaption();

    /**
     * Translate "The Shiny document {0} needs to be stopped before the document can be rendered.".
     *
     * @return the translated value
     */
    @Key("shinyTerminalErrorMsg")
    String shinyTerminalErrorMsg(String file);

    /**
     * Translate "Render Completed".
     *
     * @return the translated value
     */
    @Key("renderCompletedCaption")
    String renderCompletedCaption();

    /**
     * Translate "RStudio has finished rendering {0} to {1}.".
     *
     * @return the translated value
     */
    @Key("renderCompletedMsg")
    String renderCompletedMsg(String targetFile, String outputFile);

    /**
     * Translate "Download File".
     *
     * @return the translated value
     */
    @Key("yesLabel")
    String yesLabel();

    /**
     * Translate "OK".
     *
     * @return the translated value
     */
    @Key("noLabel")
    String noLabel();

    /**
     * Translate "Rmd Output Panel".
     *
     * @return the translated value
     */
    @Key("rmdOutputPanelTitle")
    String rmdOutputPanelTitle();

    /**
     * Translate "Open in Browser".
     *
     * @return the translated value
     */
    @Key("openInBrowserButtonText")
    String openInBrowserButtonText();

    /**
     * Translate "Find".
     *
     * @return the translated value
     */
    @Key("findTextBoxCueText")
    String findTextBoxCueText();

    /**
     * Translate "Find in Page".
     *
     * @return the translated value
     */
    @Key("findInPageCaption")
    String findInPageCaption();

    /**
     * Translate "No occurrences found".
     *
     * @return the translated value
     */
    @Key("noOccurrencesFoundMsg")
    String noOccurrencesFoundMsg();

    /**
     * Translate "Template:".
     *
     * @return the translated value
     */
    @Key("helpCaptionTemplateText")
    String helpCaptionTemplateText();

    /**
     * Translate "Using R Markdown Templates".
     *
     * @return the translated value
     */
    @Key("helpCationTemplateMsg")
    String helpCationTemplateMsg();

    /**
     * Translate "R Markdown Templates Not Found".
     *
     * @return the translated value
     */
    @Key("templatesNotFoundErrorCaption")
    String templatesNotFoundErrorCaption();

    /**
     * Translate "An error occurred while looking for R Markdown templates. {0}".
     *
     * @return the translated value
     */
    @Key("templatesNotFoundErrorMsg")
    String templatesNotFoundErrorMsg(String error);

    /**
     * Translate "Edit {0} {1} Options".
     *
     * @return the translated value
     */
    @Key("editTemplateOptionsCaption")
    String editTemplateOptionsCaption(String type, String templateName);

    /**
     * Translate "R Markdown Options".
     *
     * @return the translated value
     */
    @Key("rMarkdownOptionstabListLabel")
    String rMarkdownOptionstabListLabel();

    /**
     * Translate "Shiny Content".
     *
     * @return the translated value
     */
    @Key("warningDialogText")
    String warningDialogText();

    /**
     * Translate "Yes, Once".
     *
     * @return the translated value
     */
    @Key("yesOnceButtonText")
    String yesOnceButtonText();

    /**
     * Translate "Yes, Always".
     *
     * @return the translated value
     */
    @Key("yesAlwaysButtonText")
    String yesAlwaysButtonText();

    /**
     * Translate "No".
     *
     * @return the translated value
     */
    @Key("noButtonText")
    String noButtonText();

    /**
     * Translate "Location:".
     *
     * @return the translated value
     */
    @Key("locationLabel")
    String locationLabel();
}

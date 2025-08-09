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
    String shinyTerminalErrorCaption();
    String shinyTerminalErrorMsg(String file);
    String renderCompletedCaption();
    String renderCompletedMsg(String targetFile, String outputFile);
    String yesLabel();
    String noLabel();
    String rmdOutputPanelTitle();
    String openInBrowserButtonText();
    String findTextBoxCueText();
    String findInPageCaption();
    String noOccurrencesFoundMsg();
    String helpCaptionTemplateText();
    String helpCationTemplateMsg();
    String templatesNotFoundErrorCaption();
    String templatesNotFoundErrorMsg(String error);
    String editTemplateOptionsCaption(String type, String templateName);
    String rMarkdownOptionstabListLabel();
    String warningDialogText();
    String yesOnceButtonText();
    String yesAlwaysButtonText();
    String noButtonText();
    String locationLabel();
}

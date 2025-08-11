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
    String presentationLabel();
    String documentLabel();
    String interactiveLabel();
    String newQuartoDocumentCaption();
    String createDocButtonCaption();
    String newDocAuthorPlaceholderText();
    String templateAriaLabelValue();
    String engineLabelCaption();
    String engineSelectNoneLabel();
    String kernelLabelCaption();
    String learnMoreLinkCaption();
    String learnMorePresentationsLinkCaption();
    String learnMoreInteractiveDocsLinkCaption();
    String createEmptyDocButtonTitle();
    String titleRequiredErrorCaption();
    String titleRequiredErrorMessage();
    String htmlFormatText();
    String htmlFormatDesc();
    String pdfFormatText();
    String pdfFormatDesc();
    String wordFormatText();
    String wordFormatDesc();
    String jsFormatText();
    String jsFormatDesc();
    String beamerFormatText();
    String beamerFormatDesc();
    String powerPointFormatText();
    String powerPointFormatDesc();
    String shinyFormatDesc();
    String observableJSFormatDesc();
    String chkVisualEditorLabel();
    String aboutHelpButtonTitle();
    String editorText();
}

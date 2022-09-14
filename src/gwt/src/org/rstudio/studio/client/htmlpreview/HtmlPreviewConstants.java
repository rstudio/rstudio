/*
 * DataViewerConstants.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

package org.rstudio.studio.client.htmlpreview;

import com.google.gwt.i18n.client.Constants;

public interface HtmlPreviewConstants extends Constants {

    /**
     * Translate "Knitting...".
     *
     * @return the translated value
     */
    @DefaultStringValue("Knitting...")
    @Key("progressPreviewStartedCaption")
    String progressPreviewStartedCaption();

    /**
     * Translate "Preview failed".
     *
     * @return the translated value
     */
    @DefaultStringValue("Preview failed")
    @Key("progressPreviewFailedCaption")
    String progressPreviewFailedCaption();

    /**
     * Translate "Save File As".
     *
     * @return the translated value
     */
    @DefaultStringValue("Save File As")
    @Key("saveFileAsCaption")
    String saveFileAsCaption();

    /**
     * Translate "Saving File...".
     *
     * @return the translated value
     */
    @DefaultStringValue("Saving File...")
    @Key("savingFileCaption")
    String savingFileCaption();

    /**
     * Translate "Download to Local File".
     *
     * @return the translated value
     */
    @DefaultStringValue("Download to Local File")
    @Key("downloadToLocalFileCaption")
    String downloadToLocalFileCaption();

    /**
     * Translate "web page".
     *
     * @return the translated value
     */
    @DefaultStringValue("web page")
    @Key("downloadToLocalFileDescription")
    String downloadToLocalFileDescription();

    /**
     * Translate "Preview Tab".
     *
     * @return the translated value
     */
    @DefaultStringValue("Preview Tab")
    @Key("previewTabToolbarLabel")
    String previewTabToolbarLabel();

    /**
     * Translate "Preview: ".
     *
     * @return the translated value
     */
    @DefaultStringValue("Preview: ")
    @Key("previewToolbarLabelText")
    String previewToolbarLabelText();

    /**
     * Translate "Save As".
     *
     * @return the translated value
     */
    @DefaultStringValue("Save As")
    @Key("saveAsToolbarMenuButtonText")
    String saveAsToolbarMenuButtonText();

    /**
     * Translate "Find".
     *
     * @return the translated value
     */
    @DefaultStringValue("Find")
    @Key("findTextBoxCueText")
    String findTextBoxCueText();

    /**
     * Translate "Find in Page".
     *
     * @return the translated value
     */
    @DefaultStringValue("Find in Page")
    @Key("findInPageText")
    String findInPageText();

    /**
     * Translate "No occurrences found".
     *
     * @return the translated value
     */
    @DefaultStringValue("No occurrences found")
    @Key("noOccurrencesFoundText")
    String noOccurrencesFoundText();

    /**
     * Translate "HTML Preview Panel".
     *
     * @return the translated value
     */
    @DefaultStringValue("HTML Preview Panel")
    @Key("htmlPreviewPanelTitle")
    String htmlPreviewPanelTitle();

    /**
     * Translate "Log".
     *
     * @return the translated value
     */
    @DefaultStringValue("Log")
    @Key("showLogDialogCaption")
    String showLogDialogCaption();

    /**
     * Translate "Close".
     *
     * @return the translated value
     */
    @DefaultStringValue("Close")
    @Key("closeText")
    String closeText();


}

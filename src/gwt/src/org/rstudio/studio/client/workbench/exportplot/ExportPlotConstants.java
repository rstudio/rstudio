/*
 * ExportPlotConstants.java
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
package org.rstudio.studio.client.workbench.exportplot;

public interface ExportPlotConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Height:".
     *
     * @return translated "Height:"
     */
    String heightText();

    /**
     * Translated "Maintain aspect ratio".
     *
     * @return translated "Maintain aspect ratio"
     */
    String maintainAspectRatioText();

    /**
     * Translated "Update Preview".
     *
     * @return translated "Update Preview"
     */
    String updatePreviewTitle();


    /**
     * Translated "Save Plot as Image".
     *
     * @return translated "Save Plot as Image"
     */
    String savePlotAsImageText();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    String saveTitle();

    /**
     * Translated "View plot after saving".
     *
     * @return translated "View plot after saving"
     */
    String viewAfterSaveCheckBoxTitle();
 
    /**
     * Translated "Use device pixel ratio".
     *
     * @return translated "Use device pixel ratio"
     */
    String useDevicePixelRatioCheckBoxLabel();
    
    /**
     * Translated "When set, the plot dimensions will be scaled according to the current display's device pixel ratio.".
     *
     * @return translated "When set, the plot dimensions will be scaled according to the current display's device pixel ratio."
     */
    String useDevicePixelRatioCheckBoxTitle();

    /**
     * Translated "File Name Required".
     *
     * @return translated "File Name Required"
     */
    String fileNameRequiredCaption();

    /**
     * Translated "You must provide a file name for the plot image.".
     *
     * @return translated "You must provide a file name for the plot image."
     */
    String fileNameRequiredMessage();

    /**
     * Translated "Image format:".
     *
     * @return translated "Image format:"
     */
    String imageFormatLabel();

    /**
     * Translated "Directory...".
     *
     * @return translated "Directory..."
     */
    String directoryButtonTitle();

    /**
     * Translated "Choose Directory".
     *
     * @return translated "Choose Directory"
     */
    String chooseDirectoryCaption();

    /**
     * Translated "Selected Directory".
     *
     * @return translated "Selected Directory"
     */
    String selectedDirectoryLabel();

    /**
     * Translated "File name:".
     *
     * @return translated "File name:"
     */
    String fileNameText();

    /**
     * Translated "Copy as:".
     *
     * @return translated "Copy as:"
     */
    String copyAsText();

    /**
     * Translated "Format".
     *
     * @return translated "Format"
     */
    String formatName();

    /**
     * Translated "Copy Plot to Clipboard".
     *
     * @return translated "Copy Plot to Clipboard"
     */
    String copyPlotText();

    /**
     * Translated "Copy Plot".
     *
     * @return translated "Copy Plot"
     */
    String copyButtonText();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    String closeButtonTitle();

    /**
     * Translated "Right click on the plot image above to copy to the clipboard.".
     *
     * @return translated "Right click on the plot image above to copy to the clipboard."
     */
    String rightClickPlotImageText();

    /**
     * Translated "Width:".
     *
     * @return translated "Width:"
     */
    String widthText();

}

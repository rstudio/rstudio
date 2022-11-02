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
    @DefaultMessage("Height:")
    @Key("heightText")
    String heightText();

    /**
     * Translated "Maintain aspect ratio".
     *
     * @return translated "Maintain aspect ratio"
     */
    @DefaultMessage("Maintain aspect ratio")
    @Key("maintainAspectRatioText")
    String maintainAspectRatioText();

    /**
     * Translated "Update Preview".
     *
     * @return translated "Update Preview"
     */
    @DefaultMessage("Update Preview")
    @Key("updatePreviewTitle")
    String updatePreviewTitle();


    /**
     * Translated "Save Plot as Image".
     *
     * @return translated "Save Plot as Image"
     */
    @DefaultMessage("Save Plot as Image")
    @Key("savePlotAsImageText")
    String savePlotAsImageText();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    @DefaultMessage("Save")
    @Key("saveTitle")
    String saveTitle();

    /**
     * Translated "View plot after saving".
     *
     * @return translated "View plot after saving"
     */
    @DefaultMessage("View plot after saving")
    @Key("viewAfterSaveCheckBoxTitle")
    String viewAfterSaveCheckBoxTitle();

    /**
     * Translated "File Name Required".
     *
     * @return translated "File Name Required"
     */
    @DefaultMessage("File Name Required")
    @Key("fileNameRequiredCaption")
    String fileNameRequiredCaption();

    /**
     * Translated "You must provide a file name for the plot image.".
     *
     * @return translated "You must provide a file name for the plot image."
     */
    @DefaultMessage("You must provide a file name for the plot image.")
    @Key("fileNameRequiredMessage")
    String fileNameRequiredMessage();

    /**
     * Translated "Image format:".
     *
     * @return translated "Image format:"
     */
    @DefaultMessage("Image format:")
    @Key("imageFormatLabel")
    String imageFormatLabel();

    /**
     * Translated "Directory...".
     *
     * @return translated "Directory..."
     */
    @DefaultMessage("Directory...")
    @Key("directoryButtonTitle")
    String directoryButtonTitle();

    /**
     * Translated "Choose Directory".
     *
     * @return translated "Choose Directory"
     */
    @DefaultMessage("Choose Directory")
    @Key("chooseDirectoryCaption")
    String chooseDirectoryCaption();

    /**
     * Translated "Selected Directory".
     *
     * @return translated "Selected Directory"
     */
    @DefaultMessage("Selected Directory")
    @Key("selectedDirectoryLabel")
    String selectedDirectoryLabel();

    /**
     * Translated "File name:".
     *
     * @return translated "File name:"
     */
    @DefaultMessage("File name:")
    @Key("fileNameText")
    String fileNameText();

    /**
     * Translated "Copy as:".
     *
     * @return translated "Copy as:"
     */
    @DefaultMessage("Copy as:")
    @Key("copyAsText")
    String copyAsText();

    /**
     * Translated "Format".
     *
     * @return translated "Format"
     */
    @DefaultMessage("Format")
    @Key("formatName")
    String formatName();

    /**
     * Translated "Copy Plot to Clipboard".
     *
     * @return translated "Copy Plot to Clipboard"
     */
    @DefaultMessage("Copy Plot to Clipboard")
    @Key("copyPlotText")
    String copyPlotText();

    /**
     * Translated "Copy Plot".
     *
     * @return translated "Copy Plot"
     */
    @DefaultMessage("Copy Plot")
    @Key("copyButtonText")
    String copyButtonText();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    @Key("closeButtonTitle")
    String closeButtonTitle();

    /**
     * Translated "Right click on the plot image above to copy to the clipboard.".
     *
     * @return translated "Right click on the plot image above to copy to the clipboard."
     */
    @DefaultMessage("Right click on the plot image above to copy to the clipboard.")
    @Key("rightClickPlotImageText")
    String rightClickPlotImageText();

    /**
     * Translated "Width:".
     *
     * @return translated "Width:"
     */
    @DefaultMessage("Width:")
    @Key("widthText")
    String widthText();

}

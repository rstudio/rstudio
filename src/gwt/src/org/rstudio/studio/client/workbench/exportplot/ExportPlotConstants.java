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

    @DefaultMessage("Height:")
    @Key("heightText")
    String heightText();

    @DefaultMessage("Maintain aspect ratio")
    @Key("maintainAspectRatioText")
    String maintainAspectRatioText();

    @DefaultMessage("Update Preview")
    @Key("updatePreviewTitle")
    String updatePreviewTitle();

    @DefaultMessage("Save Plot as Image")
    @Key("savePlotAsImageText")
    String savePlotAsImageText();

    @DefaultMessage("Save")
    @Key("saveTitle")
    String saveTitle();

    @DefaultMessage("View plot after saving")
    @Key("viewAfterSaveCheckBoxTitle")
    String viewAfterSaveCheckBoxTitle();
 

    @DefaultMessage("Use device pixel ratio")
    @Key("useDevicePixelRatioCheckBoxLabel")
    String useDevicePixelRatioCheckBoxLabel();
    

    @DefaultMessage("When set, the plot dimensions will be scaled according to the current display''s device pixel ratio.")
    @Key("useDevicePixelRatioCheckBoxTitle")
    String useDevicePixelRatioCheckBoxTitle();

    @DefaultMessage("File Name Required")
    @Key("fileNameRequiredCaption")
    String fileNameRequiredCaption();

    @DefaultMessage("You must provide a file name for the plot image.")
    @Key("fileNameRequiredMessage")
    String fileNameRequiredMessage();

    @DefaultMessage("Image format:")
    @Key("imageFormatLabel")
    String imageFormatLabel();

    @DefaultMessage("Directory...")
    @Key("directoryButtonTitle")
    String directoryButtonTitle();

    @DefaultMessage("Choose Directory")
    @Key("chooseDirectoryCaption")
    String chooseDirectoryCaption();

    @DefaultMessage("Selected Directory")
    @Key("selectedDirectoryLabel")
    String selectedDirectoryLabel();

    @DefaultMessage("File name:")
    @Key("fileNameText")
    String fileNameText();

    @DefaultMessage("Copy as:")
    @Key("copyAsText")
    String copyAsText();

    @DefaultMessage("Format")
    @Key("formatName")
    String formatName();

    @DefaultMessage("Copy Plot to Clipboard")
    @Key("copyPlotText")
    String copyPlotText();

    @DefaultMessage("Copy Plot")
    @Key("copyButtonText")
    String copyButtonText();

    @DefaultMessage("Close")
    @Key("closeButtonTitle")
    String closeButtonTitle();

    @DefaultMessage("Right click on the plot image above to copy to the clipboard.")
    @Key("rightClickPlotImageText")
    String rightClickPlotImageText();

    @DefaultMessage("Width:")
    @Key("widthText")
    String widthText();

}

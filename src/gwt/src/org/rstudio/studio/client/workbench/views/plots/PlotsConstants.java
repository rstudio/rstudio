/*
 * PlotsConstants.java
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
package org.rstudio.studio.client.workbench.views.plots;

public interface PlotsConstants extends com.google.gwt.i18n.client.Messages {

    @DefaultMessage("Locator active (Esc to finish)")
    @Key("locatorActiveText")
    String locatorActiveText();

    @DefaultMessage("Finish")
    @Key("finishText")
    String finishText();

    @DefaultMessage("Remove Plot")
    @Key("removePlotCaption")
    String removePlotCaption();

    @DefaultMessage("Are you sure you want to remove the current plot?")
    @Key("removePlotMessage")
    String removePlotMessage();

    @DefaultMessage("Removing plot...")
    @Key("removingPlotText")
    String removingPlotText();

    @DefaultMessage("Clear Plots")
    @Key("clearPlotsCaption")
    String clearPlotsCaption();

    @DefaultMessage("Are you sure you want to clear all of the plots in the history?")
    @Key("clearPlotsMessage")
    String clearPlotsMessage();

    @DefaultMessage("Clearing plots...")
    @Key("clearingPlotsText")
    String clearingPlotsText();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("Preparing to export plot...")
    @Key("preparingExportPlotText")
    String preparingExportPlotText();

    @DefaultMessage("Server Error")
    @Key("serverErrorCaption")
    String serverErrorCaption();

    @DefaultMessage("Plots")
    @Key("plotsTitle")
    String plotsTitle();

    @DefaultMessage("Current Plot")
    @Key("currentPlotTitle")
    String currentPlotTitle();

    @DefaultMessage("Publishing plots")
    @Key("publishingPlotsLabel")
    String publishingPlotsLabel();

    @DefaultMessage("Plot")
    @Key("plotText")
    String plotText();

    @DefaultMessage("Plots Pane")
    @Key("plotsPaneLabel")
    String plotsPaneLabel();

    @DefaultMessage("Export")
    @Key("exportText")
    String exportText();

    @DefaultMessage("Plot Preview")
    @Key("plotPreviewTitle")
    String plotPreviewTitle();

    @DefaultMessage("Converting Plot...")
    @Key("convertingPlotText")
    String convertingPlotText();

    @DefaultMessage("Error Saving Plot")
    @Key("errorSavingPlotCaption")
    String errorSavingPlotCaption();

    @DefaultMessage("File Exists")
    @Key("fileExistsCaption")
    String fileExistsCaption();

    @DefaultMessage("The specified file name already exists. Do you want to overwrite it?")
    @Key("fileExistsMessage")
    String fileExistsMessage();

    @DefaultMessage("Save Plot as PDF")
    @Key("savePlotPDFText")
    String savePlotPDFText();

    @DefaultMessage("Save")
    @Key("saveTitle")
    String saveTitle();

    @DefaultMessage("Preview")
    @Key("previewTitle")
    String previewTitle();

    @DefaultMessage("PDF Size:")
    @Key("pdfSizeText")
    String pdfSizeText();

    @DefaultMessage("Orientation:")
    @Key("orientationText")
    String orientationText();

    @DefaultMessage("Orientation")
    @Key("orientationLabel")
    String orientationLabel();

    @DefaultMessage("Portrait")
    @Key("portraitLabel")
    String portraitLabel();

    @DefaultMessage("Landscape")
    @Key("landscapeLabel")
    String landscapeLabel();

    @DefaultMessage("Options:")
    @Key("optionsText")
    String optionsText();

    @DefaultMessage("Use cairo_pdf device")
    @Key("useCairoPdfDeviceLabel")
    String useCairoPdfDeviceLabel();

    @DefaultMessage("{0} (requires X11)")
    @Key("requiresX11Label")
    String requiresX11Label(String label);

    @DefaultMessage("Directory...")
    @Key("directoryTitle")
    String directoryTitle();

    @DefaultMessage("Choose Directory")
    @Key("chooseDirectoryCaption")
    String chooseDirectoryCaption();

    @DefaultMessage("Selected Directory")
    @Key("selectedDirectoryLabel")
    String selectedDirectoryLabel();

    @DefaultMessage("File name:")
    @Key("fileNameText")
    String fileNameText();

    @DefaultMessage("View plot after saving")
    @Key("viewPlotAfterSavingLabel")
    String viewPlotAfterSavingLabel();

    @DefaultMessage("File Name Required")
    @Key("fileNameRequiredCaption")
    String fileNameRequiredCaption();

    @DefaultMessage("You must provide a file name for the plot pdf.")
    @Key("fileNameRequiredMessage")
    String fileNameRequiredMessage();

    @DefaultMessage("Size Preset")
    @Key("sizePresetLabel")
    String sizePresetLabel();

    @DefaultMessage("(Device Size)")
    @Key("deviceSizeName")
    String deviceSizeName();

    @DefaultMessage("Width")
    @Key("widthLabel")
    String widthLabel();

    @DefaultMessage("Height")
    @Key("heightLabel")
    String heightLabel();

    @DefaultMessage("inches")
    @Key("inchesLabel")
    String inchesLabel();

    @DefaultMessage("(Custom)")
    @Key("customLabel")
    String customLabel();

    @DefaultMessage("Show plot manipulator")
    @Key("showPlotManipulatorTitle")
    String showPlotManipulatorTitle();

    @DefaultMessage("Manipulate")
    @Key("manipulateTitle")
    String manipulateTitle();

}

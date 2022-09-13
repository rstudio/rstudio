/*
 * PlotsConstants.java
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
package org.rstudio.studio.client.workbench.views.plots;

public interface PlotsConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Locator active (Esc to finish)".
     *
     * @return translated "Locator active (Esc to finish)"
     */
    @DefaultMessage("Locator active (Esc to finish)")
    @Key("locatorActiveText")
    String locatorActiveText();

    /**
     * Translated "Finish".
     *
     * @return translated "Finish"
     */
    @DefaultMessage("Finish")
    @Key("finishText")
    String finishText();

    /**
     * Translated "Remove Plot".
     *
     * @return translated "Remove Plot"
     */
    @DefaultMessage("Remove Plot")
    @Key("removePlotCaption")
    String removePlotCaption();

    /**
     * Translated "Are you sure you want to remove the current plot?".
     *
     * @return translated "Are you sure you want to remove the current plot?"
     */
    @DefaultMessage("Are you sure you want to remove the current plot?")
    @Key("removePlotMessage")
    String removePlotMessage();

    /**
     * Translated "Removing plot...".
     *
     * @return translated "Removing plot..."
     */
    @DefaultMessage("Removing plot...")
    @Key("removingPlotText")
    String removingPlotText();

    /**
     * Translated "Clear Plots".
     *
     * @return translated "Clear Plots"
     */
    @DefaultMessage("Clear Plots")
    @Key("clearPlotsCaption")
    String clearPlotsCaption();

    /**
     * Translated "Are you sure you want to clear all of the plots in the history?".
     *
     * @return translated "Are you sure you want to clear all of the plots in the history?"
     */
    @DefaultMessage("Are you sure you want to clear all of the plots in the history?")
    @Key("clearPlotsMessage")
    String clearPlotsMessage();

    /**
     * Translated "Clearing plots...".
     *
     * @return translated "Clearing plots..."
     */
    @DefaultMessage("Clearing plots...")
    @Key("clearingPlotsText")
    String clearingPlotsText();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "Preparing to export plot...".
     *
     * @return translated "Preparing to export plot..."
     */
    @DefaultMessage("Preparing to export plot...")
    @Key("preparingExportPlotText")
    String preparingExportPlotText();

    /**
     * Translated "Server Error".
     *
     * @return translated "Server Error"
     */
    @DefaultMessage("Server Error")
    @Key("serverErrorCaption")
    String serverErrorCaption();

    /**
     * Translated "Plots".
     *
     * @return translated "Plots"
     */
    @DefaultMessage("Plots")
    @Key("plotsTitle")
    String plotsTitle();

    /**
     * Translated "Current Plot".
     *
     * @return translated "Current Plot"
     */
    @DefaultMessage("Current Plot")
    @Key("currentPlotTitle")
    String currentPlotTitle();

    /**
     * Translated "Publishing plots".
     *
     * @return translated "Publishing plots"
     */
    @DefaultMessage("Publishing plots")
    @Key("publishingPlotsLabel")
    String publishingPlotsLabel();

    /**
     * Translated "Plot".
     *
     * @return translated "Plot"
     */
    @DefaultMessage("Plot")
    @Key("plotText")
    String plotText();

    /**
     * Translated "Plots Pane".
     *
     * @return translated "Plots Pane"
     */
    @DefaultMessage("Plots Pane")
    @Key("plotsPaneLabel")
    String plotsPaneLabel();

    /**
     * Translated "Export".
     *
     * @return translated "Export"
     */
    @DefaultMessage("Export")
    @Key("exportText")
    String exportText();

    /**
     * Translated "Plot Preview".
     *
     * @return translated "Plot Preview"
     */
    @DefaultMessage("Plot Preview")
    @Key("plotPreviewTitle")
    String plotPreviewTitle();

    /**
     * Translated "Converting Plot...".
     *
     * @return translated "Converting Plot..."
     */
    @DefaultMessage("Converting Plot...")
    @Key("convertingPlotText")
    String convertingPlotText();

    /**
     * Translated "Error Saving Plot".
     *
     * @return translated "Error Saving Plot"
     */
    @DefaultMessage("Error Saving Plot")
    @Key("errorSavingPlotCaption")
    String errorSavingPlotCaption();

    /**
     * Translated "File Exists".
     *
     * @return translated "File Exists"
     */
    @DefaultMessage("File Exists")
    @Key("fileExistsCaption")
    String fileExistsCaption();

    /**
     * Translated "The specified file name already exists. Do you want to overwrite it?".
     *
     * @return translated "The specified file name already exists. Do you want to overwrite it?"
     */
    @DefaultMessage("The specified file name already exists. Do you want to overwrite it?")
    @Key("fileExistsMessage")
    String fileExistsMessage();

    /**
     * Translated "Save Plot as PDF".
     *
     * @return translated "Save Plot as PDF"
     */
    @DefaultMessage("Save Plot as PDF")
    @Key("savePlotPDFText")
    String savePlotPDFText();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    @DefaultMessage("Save")
    @Key("saveTitle")
    String saveTitle();

    /**
     * Translated "Preview".
     *
     * @return translated "Preview"
     */
    @DefaultMessage("Preview")
    @Key("previewTitle")
    String previewTitle();

    /**
     * Translated "PDF Size:".
     *
     * @return translated "PDF Size:"
     */
    @DefaultMessage("PDF Size:")
    @Key("pdfSizeText")
    String pdfSizeText();

    /**
     * Translated "Orientation:".
     *
     * @return translated "Orientation:"
     */
    @DefaultMessage("Orientation:")
    @Key("orientationText")
    String orientationText();

    /**
     * Translated "Orientation".
     *
     * @return translated "Orientation"
     */
    @DefaultMessage("Orientation")
    @Key("orientationLabel")
    String orientationLabel();

    /**
     * Translated "Portrait".
     *
     * @return translated "Portrait"
     */
    @DefaultMessage("Portrait")
    @Key("portraitLabel")
    String portraitLabel();

    /**
     * Translated "Landscape".
     *
     * @return translated "Landscape"
     */
    @DefaultMessage("Landscape")
    @Key("landscapeLabel")
    String landscapeLabel();

    /**
     * Translated "Options:".
     *
     * @return translated "Options:"
     */
    @DefaultMessage("Options:")
    @Key("optionsText")
    String optionsText();

    /**
     * Translated "Use cairo_pdf device".
     *
     * @return translated "Use cairo_pdf device"
     */
    @DefaultMessage("Use cairo_pdf device")
    @Key("useCairoPdfDeviceLabel")
    String useCairoPdfDeviceLabel();

    /**
     * Translated "{0} (requires X11)".
     *
     * @return translated "{0} (requires X11)"
     */
    @DefaultMessage("{0} (requires X11)")
    @Key("requiresX11Label")
    String requiresX11Label(String label);

    /**
     * Translated "Directory...".
     *
     * @return translated "Directory..."
     */
    @DefaultMessage("Directory...")
    @Key("directoryTitle")
    String directoryTitle();

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
     * Translated "View plot after saving".
     *
     * @return translated "View plot after saving"
     */
    @DefaultMessage("View plot after saving")
    @Key("viewPlotAfterSavingLabel")
    String viewPlotAfterSavingLabel();

    /**
     * Translated "File Name Required".
     *
     * @return translated "File Name Required"
     */
    @DefaultMessage("File Name Required")
    @Key("fileNameRequiredCaption")
    String fileNameRequiredCaption();

    /**
     * Translated "You must provide a file name for the plot pdf.".
     *
     * @return translated "You must provide a file name for the plot pdf."
     */
    @DefaultMessage("You must provide a file name for the plot pdf.")
    @Key("fileNameRequiredMessage")
    String fileNameRequiredMessage();

    /**
     * Translated "Size Preset".
     *
     * @return translated "Size Preset"
     */
    @DefaultMessage("Size Preset")
    @Key("sizePresetLabel")
    String sizePresetLabel();

    /**
     * Translated "(Device Size)".
     *
     * @return translated "(Device Size)"
     */
    @DefaultMessage("(Device Size)")
    @Key("deviceSizeName")
    String deviceSizeName();

    /**
     * Translated "Width".
     *
     * @return translated "Width"
     */
    @DefaultMessage("Width")
    @Key("widthLabel")
    String widthLabel();

    /**
     * Translated "Height".
     *
     * @return translated "Height"
     */
    @DefaultMessage("Height")
    @Key("heightLabel")
    String heightLabel();

    /**
     * Translated "inches".
     *
     * @return translated "inches"
     */
    @DefaultMessage("inches")
    @Key("inchesLabel")
    String inchesLabel();

    /**
     * Translated "(Custom)".
     *
     * @return translated "(Custom)"
     */
    @DefaultMessage("(Custom)")
    @Key("customLabel")
    String customLabel();

    /**
     * Translated "Show plot manipulator".
     *
     * @return translated "Show plot manipulator"
     */
    @DefaultMessage("Show plot manipulator")
    @Key("showPlotManipulatorTitle")
    String showPlotManipulatorTitle();

    /**
     * Translated "Manipulate".
     *
     * @return translated "Manipulate"
     */
    @DefaultMessage("Manipulate")
    @Key("manipulateTitle")
    String manipulateTitle();

}

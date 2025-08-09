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

    /**
     * Translated "Locator active (Esc to finish)".
     *
     * @return translated "Locator active (Esc to finish)"
     */
    String locatorActiveText();

    /**
     * Translated "Finish".
     *
     * @return translated "Finish"
     */
    String finishText();

    /**
     * Translated "Remove Plot".
     *
     * @return translated "Remove Plot"
     */
    String removePlotCaption();

    /**
     * Translated "Are you sure you want to remove the current plot?".
     *
     * @return translated "Are you sure you want to remove the current plot?"
     */
    String removePlotMessage();

    /**
     * Translated "Removing plot...".
     *
     * @return translated "Removing plot..."
     */
    String removingPlotText();

    /**
     * Translated "Clear Plots".
     *
     * @return translated "Clear Plots"
     */
    String clearPlotsCaption();

    /**
     * Translated "Are you sure you want to clear all of the plots in the history?".
     *
     * @return translated "Are you sure you want to clear all of the plots in the history?"
     */
    String clearPlotsMessage();

    /**
     * Translated "Clearing plots...".
     *
     * @return translated "Clearing plots..."
     */
    String clearingPlotsText();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String errorCaption();

    /**
     * Translated "Preparing to export plot...".
     *
     * @return translated "Preparing to export plot..."
     */
    String preparingExportPlotText();

    /**
     * Translated "Server Error".
     *
     * @return translated "Server Error"
     */
    String serverErrorCaption();

    /**
     * Translated "Plots".
     *
     * @return translated "Plots"
     */
    String plotsTitle();

    /**
     * Translated "Current Plot".
     *
     * @return translated "Current Plot"
     */
    String currentPlotTitle();

    /**
     * Translated "Publishing plots".
     *
     * @return translated "Publishing plots"
     */
    String publishingPlotsLabel();

    /**
     * Translated "Plot".
     *
     * @return translated "Plot"
     */
    String plotText();

    /**
     * Translated "Plots Pane".
     *
     * @return translated "Plots Pane"
     */
    String plotsPaneLabel();

    /**
     * Translated "Export".
     *
     * @return translated "Export"
     */
    String exportText();

    /**
     * Translated "Plot Preview".
     *
     * @return translated "Plot Preview"
     */
    String plotPreviewTitle();

    /**
     * Translated "Converting Plot...".
     *
     * @return translated "Converting Plot..."
     */
    String convertingPlotText();

    /**
     * Translated "Error Saving Plot".
     *
     * @return translated "Error Saving Plot"
     */
    String errorSavingPlotCaption();

    /**
     * Translated "File Exists".
     *
     * @return translated "File Exists"
     */
    String fileExistsCaption();

    /**
     * Translated "The specified file name already exists. Do you want to overwrite it?".
     *
     * @return translated "The specified file name already exists. Do you want to overwrite it?"
     */
    String fileExistsMessage();

    /**
     * Translated "Save Plot as PDF".
     *
     * @return translated "Save Plot as PDF"
     */
    String savePlotPDFText();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    String saveTitle();

    /**
     * Translated "Preview".
     *
     * @return translated "Preview"
     */
    String previewTitle();

    /**
     * Translated "PDF Size:".
     *
     * @return translated "PDF Size:"
     */
    String pdfSizeText();

    /**
     * Translated "Orientation:".
     *
     * @return translated "Orientation:"
     */
    String orientationText();

    /**
     * Translated "Orientation".
     *
     * @return translated "Orientation"
     */
    String orientationLabel();

    /**
     * Translated "Portrait".
     *
     * @return translated "Portrait"
     */
    String portraitLabel();

    /**
     * Translated "Landscape".
     *
     * @return translated "Landscape"
     */
    String landscapeLabel();

    /**
     * Translated "Options:".
     *
     * @return translated "Options:"
     */
    String optionsText();

    /**
     * Translated "Use cairo_pdf device".
     *
     * @return translated "Use cairo_pdf device"
     */
    String useCairoPdfDeviceLabel();

    /**
     * Translated "{0} (requires X11)".
     *
     * @return translated "{0} (requires X11)"
     */
    String requiresX11Label(String label);

    /**
     * Translated "Directory...".
     *
     * @return translated "Directory..."
     */
    String directoryTitle();

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
     * Translated "View plot after saving".
     *
     * @return translated "View plot after saving"
     */
    String viewPlotAfterSavingLabel();

    /**
     * Translated "File Name Required".
     *
     * @return translated "File Name Required"
     */
    String fileNameRequiredCaption();

    /**
     * Translated "You must provide a file name for the plot pdf.".
     *
     * @return translated "You must provide a file name for the plot pdf."
     */
    String fileNameRequiredMessage();

    /**
     * Translated "Size Preset".
     *
     * @return translated "Size Preset"
     */
    String sizePresetLabel();

    /**
     * Translated "(Device Size)".
     *
     * @return translated "(Device Size)"
     */
    String deviceSizeName();

    /**
     * Translated "Width".
     *
     * @return translated "Width"
     */
    String widthLabel();

    /**
     * Translated "Height".
     *
     * @return translated "Height"
     */
    String heightLabel();

    /**
     * Translated "inches".
     *
     * @return translated "inches"
     */
    String inchesLabel();

    /**
     * Translated "(Custom)".
     *
     * @return translated "(Custom)"
     */
    String customLabel();

    /**
     * Translated "Show plot manipulator".
     *
     * @return translated "Show plot manipulator"
     */
    String showPlotManipulatorTitle();

    /**
     * Translated "Manipulate".
     *
     * @return translated "Manipulate"
     */
    String manipulateTitle();

}

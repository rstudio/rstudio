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
    String locatorActiveText();
    String finishText();
    String removePlotCaption();
    String removePlotMessage();
    String removingPlotText();
    String clearPlotsCaption();
    String clearPlotsMessage();
    String clearingPlotsText();
    String errorCaption();
    String preparingExportPlotText();
    String serverErrorCaption();
    String plotsTitle();
    String currentPlotTitle();
    String publishingPlotsLabel();
    String plotText();
    String plotsPaneLabel();
    String exportText();
    String plotPreviewTitle();
    String convertingPlotText();
    String errorSavingPlotCaption();
    String fileExistsCaption();
    String fileExistsMessage();
    String savePlotPDFText();
    String saveTitle();
    String previewTitle();
    String pdfSizeText();
    String orientationText();
    String orientationLabel();
    String portraitLabel();
    String landscapeLabel();
    String optionsText();
    String useCairoPdfDeviceLabel();
    String requiresX11Label(String label);
    String directoryTitle();
    String chooseDirectoryCaption();
    String selectedDirectoryLabel();
    String fileNameText();
    String viewPlotAfterSavingLabel();
    String fileNameRequiredCaption();
    String fileNameRequiredMessage();
    String sizePresetLabel();
    String deviceSizeName();
    String widthLabel();
    String heightLabel();
    String inchesLabel();
    String customLabel();
    String showPlotManipulatorTitle();
    String manipulateTitle();
}

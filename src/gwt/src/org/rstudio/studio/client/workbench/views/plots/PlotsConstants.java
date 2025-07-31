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

    @Key("locatorActiveText")
    String locatorActiveText();

    @Key("finishText")
    String finishText();

    @Key("removePlotCaption")
    String removePlotCaption();

    @Key("removePlotMessage")
    String removePlotMessage();

    @Key("removingPlotText")
    String removingPlotText();

    @Key("clearPlotsCaption")
    String clearPlotsCaption();

    @Key("clearPlotsMessage")
    String clearPlotsMessage();

    @Key("clearingPlotsText")
    String clearingPlotsText();

    @Key("errorCaption")
    String errorCaption();

    @Key("preparingExportPlotText")
    String preparingExportPlotText();

    @Key("serverErrorCaption")
    String serverErrorCaption();

    @Key("plotsTitle")
    String plotsTitle();

    @Key("currentPlotTitle")
    String currentPlotTitle();

    @Key("publishingPlotsLabel")
    String publishingPlotsLabel();

    @Key("plotText")
    String plotText();

    @Key("plotsPaneLabel")
    String plotsPaneLabel();

    @Key("exportText")
    String exportText();

    @Key("plotPreviewTitle")
    String plotPreviewTitle();

    @Key("convertingPlotText")
    String convertingPlotText();

    @Key("errorSavingPlotCaption")
    String errorSavingPlotCaption();

    @Key("fileExistsCaption")
    String fileExistsCaption();

    @Key("fileExistsMessage")
    String fileExistsMessage();

    @Key("savePlotPDFText")
    String savePlotPDFText();

    @Key("saveTitle")
    String saveTitle();

    @Key("previewTitle")
    String previewTitle();

    @Key("pdfSizeText")
    String pdfSizeText();

    @Key("orientationText")
    String orientationText();

    @Key("orientationLabel")
    String orientationLabel();

    @Key("portraitLabel")
    String portraitLabel();

    @Key("landscapeLabel")
    String landscapeLabel();

    @Key("optionsText")
    String optionsText();

    @Key("useCairoPdfDeviceLabel")
    String useCairoPdfDeviceLabel();

    @Key("requiresX11Label")
    String requiresX11Label(String label);

    @Key("directoryTitle")
    String directoryTitle();

    @Key("chooseDirectoryCaption")
    String chooseDirectoryCaption();

    @Key("selectedDirectoryLabel")
    String selectedDirectoryLabel();

    @Key("fileNameText")
    String fileNameText();

    @Key("viewPlotAfterSavingLabel")
    String viewPlotAfterSavingLabel();

    @Key("fileNameRequiredCaption")
    String fileNameRequiredCaption();

    @Key("fileNameRequiredMessage")
    String fileNameRequiredMessage();

    @Key("sizePresetLabel")
    String sizePresetLabel();

    @Key("deviceSizeName")
    String deviceSizeName();

    @Key("widthLabel")
    String widthLabel();

    @Key("heightLabel")
    String heightLabel();

    @Key("inchesLabel")
    String inchesLabel();

    @Key("customLabel")
    String customLabel();

    @Key("showPlotManipulatorTitle")
    String showPlotManipulatorTitle();

    @Key("manipulateTitle")
    String manipulateTitle();

}

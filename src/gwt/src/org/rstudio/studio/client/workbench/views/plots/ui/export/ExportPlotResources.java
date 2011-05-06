/*
 * ExportPlotDialogResources.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.ui.export;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface ExportPlotResources extends ClientBundle
{  
   public static interface Styles extends CssResource
   {
      String exportTargetLabel();
      String imageFormatListBox();
      String fileNameTextBox();
      String directoryButton();
      String directoryLabel();
      
      String imagePreview();
      String imageOptionLabel();
      String imageSizeTextBox();
      
      String verticalSizeOptions();
      String horizontalSizeOptions();
      
      String widthAndHeightEntry();
      String maintainAspectRatioCheckBox();
      String updateImageSizeButton();
      
      String rightClickCopyLabel();
      
      String copyFormatLabel();
      String copyFormatBitmap();
      String copyFormatMetafile();  
   }

  
   @Source("ExportPlot.css")
   Styles styles();
   
   ImageResource rightMouse();
   
   public static ExportPlotResources INSTANCE = 
      (ExportPlotResources)GWT.create(ExportPlotResources.class) ;
  
}
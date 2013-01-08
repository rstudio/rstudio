/*
 * CopyPlotToClipboardDesktopMetafileDialog.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.ui.export.impl;


import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlotResources;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlotSizeEditor;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;

public class CopyPlotToClipboardDesktopMetafileDialog extends CopyPlotToClipboardDesktopDialog
{

   public CopyPlotToClipboardDesktopMetafileDialog(
                                 PlotsServerOperations server,
                                 ExportPlotOptions options,
                                 OperationWithInput<ExportPlotOptions> onClose)
   {
      super(server, options, onClose);
     
      
      ExportPlotResources.Styles styles = ExportPlotResources.INSTANCE.styles();
      
      Label label = new Label();
      label.setStylePrimaryName(styles.copyFormatLabel());
      label.setText("Copy as:");
      addLeftWidget(label);
      
      copyAsBitmapRadioButton_ = new RadioButton(
                                       "Format", 
                                       SafeHtmlUtils.fromString("Bitmap"));
      copyAsBitmapRadioButton_.setStylePrimaryName(styles.copyFormatBitmap());
      addLeftWidget(copyAsBitmapRadioButton_);
      
      copyAsMetafileRadioButton_ = new RadioButton(
                                       "Format", 
                                       SafeHtmlUtils.fromString("Metafile"));
      copyAsMetafileRadioButton_.setStylePrimaryName(styles.copyFormatMetafile());
      addLeftWidget(copyAsMetafileRadioButton_);
      
      if (options.getCopyAsMetafile())
         copyAsMetafileRadioButton_.setValue(true);
      else
         copyAsBitmapRadioButton_.setValue(true);
   }
   
   
   @Override
   protected void performCopy(Operation onCompleted)
   {
      if (getCopyAsMetafile())
         copyAsMetafile(onCompleted);
      else
         copyAsBitmap(onCompleted);
   }
     
   
   @Override
   protected ExportPlotOptions getCurrentOptions(ExportPlotOptions previous)
   {
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      return ExportPlotOptions.create(sizeEditor.getImageWidth(), 
                                      sizeEditor.getImageHeight(), 
                                      sizeEditor.getKeepRatio(),
                                      previous.getFormat(),
                                      previous.getViewAfterSave(),
                                      getCopyAsMetafile());    
   }
   
   
   private boolean getCopyAsMetafile()
   {
      return copyAsMetafileRadioButton_.getValue();
   }
   
   private void copyAsMetafile(final Operation onCompleted)
   {
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      server_.copyPlotToClipboardMetafile(
            sizeEditor.getImageWidth(),
            sizeEditor.getImageHeight(),
            new SimpleRequestCallback<Void>() 
            {
               @Override
               public void onResponseReceived(Void response)
               {
                  onCompleted.execute();
               }
            });
   }
   
   private RadioButton copyAsBitmapRadioButton_;
   private RadioButton copyAsMetafileRadioButton_;
  
}

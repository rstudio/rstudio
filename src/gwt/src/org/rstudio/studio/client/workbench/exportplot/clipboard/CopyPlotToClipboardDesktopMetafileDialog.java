/*
 * CopyPlotToClipboardDesktopMetafileDialog.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.exportplot.clipboard;


import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotPreviewer;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotResources;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotSizeEditor;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;

public class CopyPlotToClipboardDesktopMetafileDialog extends CopyPlotToClipboardDesktopDialog
{

   public CopyPlotToClipboardDesktopMetafileDialog(
                                 ExportPlotPreviewer previewer,
                                 ExportPlotClipboard clipboard,
                                 ExportPlotOptions options,
                                 OperationWithInput<ExportPlotOptions> onClose)
   {
      super(previewer, clipboard, options, onClose);
     
      
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
      sizeEditor.setGripperVisible(false);
      
      // NOTE: we use a timer here just to be absolutely sure the
      // browser has re-rendered and hidden the gripper before attempting
      // to get a screenshot. note that the usual tools, e.g. scheduleDeferred(),
      // don't work as expected here
      Timers.singleShot(200, () -> doCopyAsMetafile(onCompleted));
   }
   
   private void doCopyAsMetafile(final Operation onCompleted)
   {
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      clipboard_.copyPlotToClipboardMetafile(
            sizeEditor.getImageWidth(),
            sizeEditor.getImageHeight(),
            new Command() 
            {
               @Override
               public void execute()
               {
                  sizeEditor.setGripperVisible(true);
                  onCompleted.execute();
               }
            });
   }
   
   private RadioButton copyAsBitmapRadioButton_;
   private RadioButton copyAsMetafileRadioButton_;  
}

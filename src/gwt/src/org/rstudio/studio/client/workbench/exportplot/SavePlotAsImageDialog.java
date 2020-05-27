/*
 * SavePlotAsImageDialog.java
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
package org.rstudio.studio.client.workbench.exportplot;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;

public class SavePlotAsImageDialog extends ExportPlotDialog
{
   public SavePlotAsImageDialog(
                           GlobalDisplay globalDisplay,
                           SavePlotAsImageOperation saveOperation,
                           ExportPlotPreviewer previewer,
                           SavePlotAsImageContext context, 
                           final ExportPlotOptions options,
                           final OperationWithInput<ExportPlotOptions> onClose)
   {
      super(options, previewer);
      
      setText("Save Plot as Image");
     
      globalDisplay_ = globalDisplay;
      saveOperation_ = saveOperation;
      progressIndicator_ = addProgressIndicator();
      
      ThemedButton saveButton = new ThemedButton("Save", 
                                                 new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            attemptSavePlot(false, new Operation() {
               @Override
               public void execute()
               {
                  onClose.execute(getCurrentOptions(options));
             
                  closeDialog();
               }
            });
         }
      });
      addOkButton(saveButton);
      addCancelButton();
      
      // file type and target path
      saveAsTarget_ = new SavePlotAsImageTargetEditor(options.getFormat(), 
                                                      context);
      
      // view after size
      viewAfterSaveCheckBox_ = new CheckBox("View plot after saving");
      viewAfterSaveCheckBox_.setValue(options.getViewAfterSave());
      addLeftWidget(viewAfterSaveCheckBox_);
     
   }

   @Override
   protected Widget createTopLeftWidget()
   {
      return saveAsTarget_;
   }
   
   @Override
   protected void focusInitialControl()
   {
      saveAsTarget_.focus();
   }
   
   @Override
   protected ExportPlotOptions getCurrentOptions(ExportPlotOptions previous)
   {
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      return ExportPlotOptions.create(sizeEditor.getImageWidth(), 
                                      sizeEditor.getImageHeight(), 
                                      sizeEditor.getKeepRatio(),
                                      saveAsTarget_.getFormat(),
                                      viewAfterSaveCheckBox_.getValue(),
                                      previous.getCopyAsMetafile());
   }
   
   private void attemptSavePlot(boolean overwrite,
                                final Operation onCompleted)
   {
      // get plot format 
      final String format = saveAsTarget_.getFormat();
      
      // validate path
      FileSystemItem targetPath = saveAsTarget_.getTargetPath();
      if (targetPath == null)
      {
         globalDisplay_.showErrorMessage(
            "File Name Required", 
            "You must provide a file name for the plot image.", 
            saveAsTarget_);
         return;
      }
      
      saveOperation_.attemptSave(
            progressIndicator_, 
            targetPath, 
            format, 
            getSizeEditor(), 
            overwrite, 
            viewAfterSaveCheckBox_.getValue(), 
            onCompleted);    
   }
  
   private final GlobalDisplay globalDisplay_;
   private ProgressIndicator progressIndicator_;
   private final SavePlotAsImageOperation saveOperation_;
   private SavePlotAsImageTargetEditor saveAsTarget_;
   private CheckBox viewAfterSaveCheckBox_;
   
}

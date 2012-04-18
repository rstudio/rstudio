/*
 * SavePlotAsImageDialog.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;

public class SavePlotAsImageDialog extends ExportPlotDialog
{
   public SavePlotAsImageDialog(
                           GlobalDisplay globalDisplay,
                           PlotsServerOperations server,
                           SavePlotAsImageContext context, 
                           final ExportPlotOptions options,
                           final OperationWithInput<ExportPlotOptions> onClose)
   {
      super(server, options);
      
      setText("Save Plot as Image");
     
      globalDisplay_ = globalDisplay;
      server_ = server;
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
      saveAsTarget_ = new SavePlotAsTargetEditor(options.getFormat(), 
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
   protected void onDialogShown()
   {
      super.onDialogShown();
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
      
      // create handler
      SavePlotAsHandler handler = new SavePlotAsHandler(
            globalDisplay_, 
            progressIndicator_, 
            new SavePlotAsHandler.ServerOperations()
            {
               @Override
               public void savePlot(
                     FileSystemItem targetPath, 
                     boolean overwrite,
                     ServerRequestCallback<Bool> requestCallback)
               {
                  ExportPlotSizeEditor sizeEditor = getSizeEditor();
                  server_.savePlotAs(targetPath, 
                                     format, 
                                     sizeEditor.getImageWidth(), 
                                     sizeEditor.getImageHeight(), 
                                     overwrite,
                                     requestCallback);
               }

               @Override
               public String getFileUrl(FileSystemItem path)
               {
                  return server_.getFileUrl(path);
               }
            });
      
      // invoke handler
      handler.attemptSave(targetPath, 
                          overwrite, 
                          viewAfterSaveCheckBox_.getValue(), 
                          onCompleted);                   
   }
  
   private final GlobalDisplay globalDisplay_;
   private ProgressIndicator progressIndicator_;
   private final PlotsServerOperations server_;
   private SavePlotAsTargetEditor saveAsTarget_;
   private CheckBox viewAfterSaveCheckBox_;
   
}

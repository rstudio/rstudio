package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotExportContext;
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
                           PlotExportContext context, 
                           final ExportPlotOptions options,
                           final OperationWithInput<ExportPlotOptions> onClose)
   {
      super(server, options);
      
      setText("Save Plot as Image");
     
      globalDisplay_ = globalDisplay;
      server_ = server;
      progressIndicator_ = addProgressIndicator();
      
      ThemedButton saveButton = new ThemedButton("Save Plot", 
                                                 new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            attemptSavePlot(new Operation() {
               @Override
               public void execute()
               {
                  // save user options
                  onClose.execute(getCurrentOptions(options));
            
                  // close dialog
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
   
   private void attemptSavePlot(final Operation onCompleted)
   {
      // get plot format 
      String format = saveAsTarget_.getFormat();
      
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
      
      // save plot
      progressIndicator_.onProgress("Saving plot...");
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      server_.savePlotAs(targetPath, 
                         format, 
                         sizeEditor.getImageWidth(), 
                         sizeEditor.getImageHeight(), 
                         new VoidServerRequestCallback(progressIndicator_) {
                            @Override
                            protected void onSuccess()
                            {
                               onCompleted.execute();
                            }
                         });
   }
   
   private final GlobalDisplay globalDisplay_;
   private ProgressIndicator progressIndicator_;
   private final PlotsServerOperations server_;
   private SavePlotAsTargetEditor saveAsTarget_;
   private CheckBox viewAfterSaveCheckBox_;
   
}

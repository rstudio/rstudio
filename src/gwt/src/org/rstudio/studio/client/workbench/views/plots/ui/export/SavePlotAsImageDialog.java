package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialogProgressIndicator;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotContext;
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
                           SavePlotContext context, 
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
      
      if (Desktop.isDesktop() || !viewAfterSaveCheckBox_.getValue())
         desktopSavePlotAs(format, targetPath, overwrite, onCompleted);
      else
         webSavePlotAs(format, targetPath, overwrite, onCompleted);
                         
   }
   
  
   private void desktopSavePlotAs(String format, 
                                  final FileSystemItem targetPath, 
                                  boolean overwrite,
                                  Operation onCompleted)
   {
      progressIndicator_.onProgress("Saving Plot...");
      
      savePlotAs(
        format, 
        targetPath, 
        overwrite, 
        onCompleted, 
        new PlotSaveAsUIHandler() {
           @Override
           public void onSuccess()
           {
              progressIndicator_.clearProgress();
              
              if (viewAfterSaveCheckBox_.getValue())
              {
                 RStudioGinjector.INSTANCE.getFileTypeRegistry().openFile(
                                                             targetPath);
              }
           }
           
           @Override
           public void onError(ServerError error)
           {
              progressIndicator_.onError(error.getUserMessage());
               
           }

           @Override
           public void onOverwritePrompt()
           {
              progressIndicator_.clearProgress();
           }
        });
   }
   
   private void webSavePlotAs(final String format, 
                              final FileSystemItem targetPath, 
                              final boolean overwrite,
                              final Operation onCompleted)
   {
      globalDisplay_.openProgressWindow("_rstudio_save_plot_as",
                                        "Saving Plot...", 
                                        new OperationWithInput<WindowEx>() {                                        
         public void execute(final WindowEx window)
         {
            savePlotAs(
               format, 
               targetPath, 
               overwrite, 
               onCompleted, 
               new PlotSaveAsUIHandler() {
                  @Override
                  public void onSuccess()
                  {
                     // redirect window to view file
                     String url = server_.getFileUrl(targetPath);
                     window.replaceLocationHref(url);
                  }
                  
                  @Override
                  public void onError(ServerError error)
                  {
                     window.close();
                     
                     globalDisplay_.showErrorMessage("Error Saving Plot", 
                                                     error.getUserMessage());       
                  }
       
                  @Override
                  public void onOverwritePrompt()
                  {
                     window.close();
                  }
               });
         }
      });
   }
   
   private interface PlotSaveAsUIHandler
   {
      void onSuccess();
      void onError(ServerError error);
      void onOverwritePrompt();
   }
   
   
   private void savePlotAs(String format, 
                           final FileSystemItem targetPath, 
                           boolean overwrite,
                           final Operation onCompleted,
                           final PlotSaveAsUIHandler uiHandler)
   {
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      server_.savePlotAs(
         targetPath, 
         format, 
         sizeEditor.getImageWidth(), 
         sizeEditor.getImageHeight(), 
         overwrite,
         new ServerRequestCallback<Bool>() {

            @Override
            public void onResponseReceived(Bool saved)
            {
               
               
               if (saved.getValue())
               {
                  uiHandler.onSuccess();
                  
                  // fire onCompleted
                  onCompleted.execute();
               }
               else
               { 
                  uiHandler.onOverwritePrompt();
                  
                  globalDisplay_.showYesNoMessage(
                        MessageDialog.WARNING, 
                        "File Exists", 
                        "The specified image file name already exists. " +
                        "Do you want to overwrite it?", 
                        new Operation() {
                           @Override
                           public void execute()
                           {
                              attemptSavePlot(true, onCompleted);
                           }
                        }, 
                        true);
                 
               }
            }
            
            @Override
            public void onError(ServerError error)
            {
               uiHandler.onError(error);
            }
         
      });     
   }
   
   
   
   private final GlobalDisplay globalDisplay_;
   private ModalDialogProgressIndicator progressIndicator_;
   private final PlotsServerOperations server_;
   private SavePlotAsTargetEditor saveAsTarget_;
   private CheckBox viewAfterSaveCheckBox_;
   
}

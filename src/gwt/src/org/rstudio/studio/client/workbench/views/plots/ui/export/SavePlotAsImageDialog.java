package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
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
                           PlotsServerOperations server,
                           PlotExportContext context, 
                           final ExportPlotOptions options,
                           final OperationWithInput<ExportPlotOptions> onClose)
   {
      super(server, options);
      
      setText("Save Plot as Image");
      
      ThemedButton saveButton = new ThemedButton("Save Plot", 
                                                 new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            ExportPlotSizeEditor sizeEditor = getSizeEditor();
            ExportPlotOptions options = ExportPlotOptions.create(
                                     exportTarget_.getFormat(), 
                                     sizeEditor.getImageWidth(), 
                                     sizeEditor.getImageHeight(), 
                                     sizeEditor.getKeepRatio(),
                                     viewAfterSaveCheckBox_.getValue());
            onClose.execute(options);
            closeDialog();
         }
      });
      addOkButton(saveButton);
      addCancelButton();
      
      exportTarget_ = new ExportPlotTargetEditor(options.getFormat(), 
                                                 context);
      
      viewAfterSaveCheckBox_ = new CheckBox("View plot after saving");
      viewAfterSaveCheckBox_.setValue(options.getViewAfterSave());
      addLeftWidget(viewAfterSaveCheckBox_);
     
   }

   @Override
   protected Widget createTopLeftWidget()
   {
      return exportTarget_;
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
      exportTarget_.setInitialFocus();
   }
   
   private ExportPlotTargetEditor exportTarget_;
   private CheckBox viewAfterSaveCheckBox_;
   
}

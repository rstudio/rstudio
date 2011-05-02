package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ExportPlotDialog extends ModalDialogBase
{
   public ExportPlotDialog(PlotsServerOperations server,
                           ExportPlotOptions options,
                           final OperationWithInput<ExportPlotOptions> onClose)
   { 
      server_ = server;
      options_ = options;
      
      setText("Export Plot");
     
      setButtonAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
      
      ThemedButton closeButton = new ThemedButton("Close", new ClickHandler() {
         public void onClick(ClickEvent event) {
            onClose.execute(options_);
            closeDialog();
         }
      });
      addCancelButton(closeButton); 
   }
  
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel mainPanel = new VerticalPanel();
      mainPanel.setStylePrimaryName(
                  ExportPlotDialogResources.INSTANCE.styles().mainWidget());
      
   
      imagePreview_ = new ResizableImagePreview();
      imagePreview_.setWidth("360px");
      imagePreview_.setHeight("240px");
      
      mainPanel.add(imagePreview_);
      
      
      return mainPanel;
      
   }
  
   
   @SuppressWarnings("unused")
   private final PlotsServerOperations server_;
   private ExportPlotOptions options_;
   
   private ResizableImagePreview imagePreview_;
  
}

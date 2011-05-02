package org.rstudio.studio.client.workbench.views.plots.ui.export;


import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
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
      mainPanel.setStylePrimaryName(RESOURCES.styles().mainWidget());
      
      return mainPanel;
      
   }
   
   
   public static interface Styles extends CssResource
   {
      String mainWidget();
   }

   public static interface Resources extends ClientBundle
   {
      @Source("ExportPlotDialog.css")
      Styles styles();
   }
   
   public static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   @SuppressWarnings("unused")
   private final PlotsServerOperations server_;
   private ExportPlotOptions options_;
  
}

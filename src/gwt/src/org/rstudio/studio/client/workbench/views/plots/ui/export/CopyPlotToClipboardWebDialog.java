package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class CopyPlotToClipboardWebDialog extends ExportPlotDialog
{

   public CopyPlotToClipboardWebDialog(
                            PlotsServerOperations server,
                            final ExportPlotOptions options,
                            final OperationWithInput<ExportPlotOptions> onClose)
   {
      super(server, options);
     
      setText("Copy Plot to Clipboard");
      
      ThemedButton closeButton = new ThemedButton("Close", 
            new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            ExportPlotSizeEditor sizeEditor = getSizeEditor();
            ExportPlotOptions saveOptions = ExportPlotOptions.create(
                  options.getFormat(), 
                  sizeEditor.getImageWidth(), 
                  sizeEditor.getImageHeight(), 
                  sizeEditor.getKeepRatio(),
                  options.getViewAfterSave());
            onClose.execute(saveOptions);
            closeDialog();
         }
      });
      
      addCancelButton(closeButton);
      
      ExportPlotResources resources = ExportPlotResources.INSTANCE;
      HorizontalPanel infoPanel = new HorizontalPanel();
      
      Image rightMouseImage = new Image(resources.rightMouse());
      infoPanel.add(rightMouseImage);
      
      Label label = new Label("Right click on the plot image above to " +
                              "copy to the clipboard.");
      label.setStylePrimaryName(resources.styles().rightClickCopyLabel());
      infoPanel.add(label);
      
      addLeftWidget(infoPanel);

   }

}

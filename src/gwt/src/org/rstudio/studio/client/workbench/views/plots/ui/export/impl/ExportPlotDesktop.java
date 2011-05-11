package org.rstudio.studio.client.workbench.views.plots.ui.export.impl;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlot;

public class ExportPlotDesktop extends ExportPlot
{

   @Override
   public void copyPlotToClipboard(
                              PlotsServerOperations server,
                              ExportPlotOptions options,
                              OperationWithInput<ExportPlotOptions> onClose)
   {   
      if (Desktop.getFrame().supportsClipboardMetafile())
      {
         new CopyPlotToClipboardDesktopMetafileDialog(server, 
                                                      options, 
                                                      onClose).showModal();
      }
      else
      {
         new CopyPlotToClipboardDesktopDialog(server, 
                                              options, 
                                              onClose).showModal();
      }
   }

}

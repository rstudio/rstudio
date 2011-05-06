package org.rstudio.studio.client.workbench.views.plots.ui.export.impl;

import org.rstudio.core.client.widget.OperationWithInput;
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
      new CopyPlotToClipboardDesktopDialog(server, 
                                           options, 
                                           onClose).showModal();
   }

}

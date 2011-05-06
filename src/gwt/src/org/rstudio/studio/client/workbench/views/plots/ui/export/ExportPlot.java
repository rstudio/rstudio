package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotExportContext;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

public abstract class ExportPlot
{
   public void savePlotAsImage(PlotsServerOperations server,
                               PlotExportContext context, 
                               ExportPlotOptions options,
                               OperationWithInput<ExportPlotOptions> onClose)
   {
      new SavePlotAsImageDialog(server, context, options, onClose).showModal();
   }
   
   
   public abstract void copyPlotToClipboard(
                           PlotsServerOperations server,
                           ExportPlotOptions options,
                           OperationWithInput<ExportPlotOptions> onClose);

}

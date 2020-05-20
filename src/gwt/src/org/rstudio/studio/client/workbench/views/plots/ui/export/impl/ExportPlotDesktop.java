/*
 * ExportPlotDesktop.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.ui.export.impl;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.exportplot.clipboard.CopyPlotToClipboardDesktopDialog;
import org.rstudio.studio.client.workbench.exportplot.clipboard.CopyPlotToClipboardDesktopMetafileDialog;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlot;
import org.rstudio.studio.client.workbench.views.plots.ui.export.PlotsPaneClipboard;
import org.rstudio.studio.client.workbench.views.plots.ui.export.PlotsPanePreviewer;

public class ExportPlotDesktop extends ExportPlot
{
   @Override
   public void copyPlotToClipboard(
                              final PlotsServerOperations server,
                              final ExportPlotOptions options,
                              final OperationWithInput<ExportPlotOptions> onClose)
   {
      Desktop.getFrame().supportsClipboardMetafile(supported ->
      {
         if (supported)
         {
            new CopyPlotToClipboardDesktopMetafileDialog(
                  new PlotsPanePreviewer(server, true),
                  new PlotsPaneClipboard(server),
                  options, 
                  onClose).showModal();
         }
         else
         {
            new CopyPlotToClipboardDesktopDialog(
                  new PlotsPanePreviewer(server, true),
                  new PlotsPaneClipboard(server),
                  options, 
                  onClose).showModal();
         }
      });
   }
}

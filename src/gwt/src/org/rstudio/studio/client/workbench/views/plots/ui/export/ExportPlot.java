/*
 * ExportPlot.java
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
package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.exportplot.SavePlotAsImageDialog;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;


public class ExportPlot
{
   public void savePlotAsImage(GlobalDisplay globalDisplay,
                               PlotsServerOperations server,
                               SavePlotAsImageContext context, 
                               ExportPlotOptions options,
                               OperationWithInput<ExportPlotOptions> onClose)
   {
      new SavePlotAsImageDialog(globalDisplay,
                                new PlotsPaneSaveAsImageOperation(globalDisplay,
                                                                  server), 
                                new PlotsPanePreviewer(server),
                                context, 
                                options, 
                                onClose).showModal();
   }
   
   public void savePlotAsPdf(GlobalDisplay globalDisplay,
                             PlotsServerOperations server,
                             SessionInfo sessionInfo,
                             FileSystemItem defaultDirectory,
                             String defaultPlotName,
                             final SavePlotAsPdfOptions options,
                             double plotWidth,
                             double plotHeight,
                             final OperationWithInput<SavePlotAsPdfOptions> onClose)
   {
      new SavePlotAsPdfDialog(globalDisplay,
                              server,
                              sessionInfo,
                              defaultDirectory,
                              defaultPlotName,
                              options,
                              plotWidth,
                              plotHeight,
                              onClose).showModal();
   }
   
   
   public void copyPlotToClipboard(
                           PlotsServerOperations server,
                           ExportPlotOptions options,
                           OperationWithInput<ExportPlotOptions> onClose)
   {  
   }
}

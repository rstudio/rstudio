/*
 * ExportPlotWeb.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.ui.export.impl;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.exportplot.clipboard.CopyPlotToClipboardWebDialog;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlot;
import org.rstudio.studio.client.workbench.views.plots.ui.export.PlotsPanePreviewer;

public class ExportPlotWeb extends ExportPlot
{
   @Override
   public void copyPlotToClipboard(
                              PlotsServerOperations server,
                              ExportPlotOptions options,
                              OperationWithInput<ExportPlotOptions> onClose)
   {
      new CopyPlotToClipboardWebDialog(
         options, new PlotsPanePreviewer(server, true), onClose).showModal();
   }

}

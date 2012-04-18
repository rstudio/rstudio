/*
 * ExportPlotWeb.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.ui.export.impl;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlot;

public class ExportPlotWeb extends ExportPlot
{
   @Override
   public void copyPlotToClipboard(
                              PlotsServerOperations server,
                              ExportPlotOptions options,
                              OperationWithInput<ExportPlotOptions> onClose)
   {
      new CopyPlotToClipboardWebDialog(server, options, onClose).showModal();
   }

}

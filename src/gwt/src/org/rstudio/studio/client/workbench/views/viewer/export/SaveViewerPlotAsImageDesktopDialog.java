/*
 * SaveViewerPlotAsImageDesktopDialog.java
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

package org.rstudio.studio.client.workbench.views.viewer.export;


import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.exportplot.SavePlotAsImageDialog;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;

public class SaveViewerPlotAsImageDesktopDialog extends SavePlotAsImageDialog 
{

   public SaveViewerPlotAsImageDesktopDialog(
                     GlobalDisplay globalDisplay,
                     String viewerUrl,
                     SavePlotAsImageContext context, 
                     ExportPlotOptions options,
                     OperationWithInput<ExportPlotOptions> onClose)
   {
      super(globalDisplay, 
            new ViewerPaneSaveAsImageDesktopOperation(), 
            new ViewerPanePreviewer(viewerUrl), 
            context, 
            options, 
            onClose);
   }
}

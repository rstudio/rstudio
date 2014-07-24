/*
 * CopyViewerPlotToClipboardDesktopDialog.java
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

package org.rstudio.studio.client.workbench.views.viewer.export;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.exportplot.clipboard.CopyPlotToClipboardDesktopDialogBase;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;

public class CopyViewerPlotToClipboardDesktopDialog
      extends CopyPlotToClipboardDesktopDialogBase
{

   public CopyViewerPlotToClipboardDesktopDialog(
                                 String viewerUrl,
                                 ExportPlotOptions options,
                                 OperationWithInput<ExportPlotOptions> onClose)
   {
      super(options, new ViewerPanePreviewer(viewerUrl), onClose);
   }

   @Override
   protected void copyAsBitmap(Operation onCompleted)
   {
      
      ElementEx iframe = getSizeEditor().getPreviewIFrame().<ElementEx>cast();
     
      Debug.logToConsole("top: " + iframe.getClientTop());
      Debug.logToConsole("left: " + iframe.getClientLeft());
      Debug.logToConsole("width: " + iframe.getClientWidth());
      Debug.logToConsole("height: " + iframe.getClientHeight());
      
      onCompleted.execute();
   }
   
}

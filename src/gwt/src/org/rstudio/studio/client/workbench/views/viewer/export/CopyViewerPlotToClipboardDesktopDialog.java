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

import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotSizeEditor;
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
      // get the size editor
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      
      // hide gripper
      sizeEditor.setGripperVisible(false);
      
      // get the preview iframe rect
      ElementEx iframe = sizeEditor.getPreviewIFrame().<ElementEx>cast();
      Rectangle viewerRect = new Rectangle(iframe.getClientLeft(),
                                           iframe.getClientTop(),
                                           iframe.getClientWidth(),
                                           iframe.getClientHeight());
                                    
      // inflate by -1 to eliminate surrounding border
      viewerRect = viewerRect.inflate(-1);
   
      // copy to clipboard
      Desktop.getFrame().copyPageRegionToClipboard(viewerRect.getLeft(),
                                                   viewerRect.getTop(),
                                                   viewerRect.getWidth(),
                                                   viewerRect.getHeight());
      
      // show gripper
      sizeEditor.setGripperVisible(true);
      
      // all done
      onCompleted.execute();
   }
   
}

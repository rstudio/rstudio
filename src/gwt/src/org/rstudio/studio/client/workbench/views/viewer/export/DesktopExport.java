/*
 * DesktopExport.java
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
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotSizeEditor;

public class DesktopExport
{
   public static void export(ExportPlotSizeEditor sizeEditor,
                             OperationWithInput<Rectangle> exporter)
   {
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
   
      // perform the export
      exporter.execute(viewerRect);
      
      // show gripper
      sizeEditor.setGripperVisible(true);
   }
}

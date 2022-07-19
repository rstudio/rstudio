/*
 * DesktopExport.java
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.viewer.export;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopInfo;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotSizeEditor;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Command;

public class DesktopExport
{
   public static void export(final ExportPlotSizeEditor sizeEditor,
                             final OperationWithInput<Rectangle> exporter,
                             final Operation onCompleted)
   {
      sizeEditor.prepareForExport(new Command() {

         @Override
         public void execute()
         {
            // hide gripper
            sizeEditor.setGripperVisible(false);

            if (BrowseCap.isElectron())
            {
               Desktop.getFrame().getZoomLevel(zoomLevel -> performExport(zoomLevel));
            }
            else
            {
               // get zoom level
               double zoomLevel = DesktopInfo.getZoomLevel();

               performExport(zoomLevel);
            }
         }

         private void performExport(double zoomLevel) {
            
            // get the preview iframe rect
            ElementEx iframe = sizeEditor.getPreviewIFrame().<ElementEx>cast();
            final Rectangle viewerRect = new Rectangle(
                   (int) Math.ceil(zoomLevel * ElementEx.clientLeft(iframe)),
                   (int) Math.ceil(zoomLevel * ElementEx.clientTop(iframe)),
                   (int) Math.ceil(zoomLevel * iframe.getClientWidth()),
                   (int) Math.ceil(zoomLevel * iframe.getClientHeight())).inflate(-1);

            // perform the export
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
               @Override
               public void execute()
               {
                  exporter.execute(viewerRect);

                  // show gripper
                  sizeEditor.setGripperVisible(true);

                  // call onCompleted
                  if (onCompleted != null)
                     onCompleted.execute();
               }
            });
         }
      });

   }
}
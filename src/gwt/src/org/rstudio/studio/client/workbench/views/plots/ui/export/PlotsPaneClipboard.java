/*
 * PlotsPaneClipboardOperations.java
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

import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.exportplot.clipboard.ExportPlotClipboard;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

import com.google.gwt.user.client.Command;

public class PlotsPaneClipboard implements ExportPlotClipboard
{
   public PlotsPaneClipboard(PlotsServerOperations server)
   {
      server_ = server;
   }
   
   @Override
   public void copyPlotToClipboardMetafile(int width, 
                                           int height,
                                           final Command onCompleted)
   {
      server_.copyPlotToClipboardMetafile(
            width,
            height,
            new SimpleRequestCallback<Void>() 
            {
               @Override
               public void onResponseReceived(Void response)
               {
                  onCompleted.execute();
               }
            });
      
   }


   @Override
   public void copyPlotToCocoaPasteboard(int width, 
                                         int height,
                                         final Command onCompleted)
   {
      server_.copyPlotToCocoaPasteboard(
            width,
            height,
            new SimpleRequestCallback<Void>() 
            {
               @Override
               public void onResponseReceived(Void response)
               {
                  onCompleted.execute();
               }
            });
   }
   
   final PlotsServerOperations server_;
}

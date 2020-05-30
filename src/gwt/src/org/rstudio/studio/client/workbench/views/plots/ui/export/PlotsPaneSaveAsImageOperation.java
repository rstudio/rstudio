/*
 * PlotsPanePreviewer.java
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
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotSizeEditor;
import org.rstudio.studio.client.workbench.exportplot.SavePlotAsImageOperation;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

public class PlotsPaneSaveAsImageOperation implements SavePlotAsImageOperation
{
   public PlotsPaneSaveAsImageOperation(GlobalDisplay globalDisplay,
                                        PlotsServerOperations server)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
   }
   
   @Override
   public void attemptSave(ProgressIndicator progressIndicator, 
                           FileSystemItem targetPath,
                           final String format,
                           final ExportPlotSizeEditor sizeEditor,
                           boolean overwrite, 
                           boolean viewAfterSave,
                           Operation onCompleted)
   {
      // create handler
      SavePlotAsHandler handler = new SavePlotAsHandler(
            globalDisplay_, 
            progressIndicator, 
            new SavePlotAsHandler.ServerOperations()
            {
               @Override
               public void savePlot(
                     FileSystemItem targetPath, 
                     boolean overwrite,
                     ServerRequestCallback<Bool> requestCallback)
               {
                  server_.savePlotAs(targetPath, 
                                     format, 
                                     sizeEditor.getImageWidth(), 
                                     sizeEditor.getImageHeight(), 
                                     overwrite,
                                     requestCallback);
               }

               @Override
               public String getFileUrl(FileSystemItem path)
               {
                  return server_.getFileUrl(path);
               }
            });
      
      // invoke handler
      handler.attemptSave(targetPath, 
                          overwrite, 
                          viewAfterSave, 
                          onCompleted);     
      
   }
   
   private final PlotsServerOperations server_;
   private final GlobalDisplay globalDisplay_;
}

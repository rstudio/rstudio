/*
 * ViewerPaneSaveAsImageDesktopOperation.java
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

import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotSizeEditor;
import org.rstudio.studio.client.workbench.exportplot.SavePlotAsImageOperation;

public class ViewerPaneSaveAsImageDesktopOperation implements SavePlotAsImageOperation
{
   @Override
   public void attemptSave(final ProgressIndicator progressIndicator, 
                           final FileSystemItem targetPath,
                           final String format, 
                           final ExportPlotSizeEditor sizeEditor,
                           final boolean overwrite,
                           final boolean viewAfterSave,
                           final Operation onCompleted)
   {
      DesktopExport.export(
            sizeEditor, 
            new OperationWithInput<Rectangle>() {
               @Override
               public void execute(Rectangle viewerRect)
               {
                  // perform the export
                  Desktop.getFrame().exportPageRegionToFile(
                        StringUtil.notNull(targetPath.getPath()), 
                        StringUtil.notNull(format), 
                        viewerRect.getLeft(),
                        viewerRect.getTop(),
                        viewerRect.getWidth(),
                        viewerRect.getHeight());
                    
                  if (viewAfterSave) 
                     Desktop.getFrame().showFile(StringUtil.notNull(targetPath.getPath()));
               }
            },
            onCompleted
         );
   }
}

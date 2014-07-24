/*
 * ViewerPaneSaveAsImageOperation.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.exportplot.SavePlotAsImageOperation;

public class ViewerPaneSaveAsImageOperation implements SavePlotAsImageOperation
{
   public ViewerPaneSaveAsImageOperation(GlobalDisplay globalDisplay)
   {
      globalDisplay_ = globalDisplay;
   }
   
   @Override
   public void attemptSave(ProgressIndicator progressIndicator, 
                           FileSystemItem targetPath,
                           final String format, 
                           final int width,
                           final int height,
                           boolean overwrite, 
                           boolean viewAfterSave,
                           Operation onCompleted)
   {
       
      
   }
   
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
}

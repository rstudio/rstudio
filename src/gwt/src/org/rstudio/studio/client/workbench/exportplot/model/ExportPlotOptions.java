/*
 * ExportPlotOptions.java
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
package org.rstudio.studio.client.workbench.exportplot.model;

import org.rstudio.core.client.Size;
import org.rstudio.studio.client.workbench.prefs.model.UserStateAccessor;

public class ExportPlotOptions extends UserStateAccessor.ExportPlotOptions
{
   protected ExportPlotOptions() {}
   
   public static final native ExportPlotOptions create(int width, 
                                                       int height,
                                                       boolean keepRatio,
                                                       String format,
                                                       boolean viewAfterSave,
                                                       boolean copyAsMetafile) 
   /*-{
      var options = new Object();
      options.width = width;
      options.height = height;
      options.format = format;
      options.keepRatio = keepRatio;
      options.viewAfterSave = viewAfterSave;
      options.copyAsMetafile = copyAsMetafile;
      return options;
   }-*/;
   
   public static final ExportPlotOptions adaptToSize(ExportPlotOptions options,
                                                     Size size)
   {
      return ExportPlotOptions.create(size.width,
                                      size.height,
                                      options.getKeepRatio(),
                                      options.getFormat(),
                                      options.getViewAfterSave(),
                                      options.getCopyAsMetafile());
   }

   public static native boolean areEqual(ExportPlotOptions a, ExportPlotOptions b) /*-{
      if (a === null ^ b === null)
         return false;
      if (a === null)
         return true;
      return a.format === b.format &&
             a.width === b.width &&
             a.height === b.height &&
             a.keepRatio === b.keepRatio &&
             a.viewAfterSave === b.viewAfterSave &&
             a.copyAsMetafile === b.copyAsMetafile;    
   }-*/;
}

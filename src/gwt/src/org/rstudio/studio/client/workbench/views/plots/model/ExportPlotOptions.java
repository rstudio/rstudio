/*
 * ExportPlotOptions.java
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
package org.rstudio.studio.client.workbench.views.plots.model;

import org.rstudio.core.client.Size;

import com.google.gwt.core.client.JavaScriptObject;

public class ExportPlotOptions extends JavaScriptObject
{
   protected ExportPlotOptions()
   {   
   }
   
   public static final ExportPlotOptions createDefault()
   {
      return create(550, 450, false, "PNG", false, false);
   }
   
   public static final native ExportPlotOptions create(int width, 
                                                       int height,
                                                       boolean keepRatio,
                                                       String format,
                                                       boolean viewAfterSave,
                                                       boolean copyAsMetafile) 
   /*-{
      var options = new Object();
      options.width = width ;
      options.height = height ;
      options.format = format;
      options.keepRatio = keepRatio;
      options.viewAfterSave = viewAfterSave;
      options.copyAsMetafile = copyAsMetafile;
      return options ;
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
   
   public final native String getFormat() /*-{
      return this.format;
   }-*/;
   
   public final native int getWidth() /*-{
      return this.width;
   }-*/;
   public final native int getHeight() /*-{
      return this.height;
   }-*/;
   
   public final native boolean getKeepRatio() /*-{
      if (this.keepRatio)
         return this.keepRatio;
      else
         return false;
   }-*/;
   
   public final native boolean getViewAfterSave() /*-{
      if (this.viewAfterSave)
         return this.viewAfterSave;
      else
         return false;
   }-*/;
   
   public final native boolean getCopyAsMetafile() /*-{
      if (this.copyAsMetafile)
         return this.copyAsMetafile;
      else
         return false;
   }-*/;
}

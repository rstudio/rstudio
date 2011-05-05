/*
 * ExportPlotOptions.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ExportPlotOptions extends JavaScriptObject
{
   protected ExportPlotOptions()
   {   
   }
   
   public static final native ExportPlotOptions create(String format,
                                                       int width, 
                                                       int height,
                                                       boolean keepRatio,
                                                       boolean viewAfterSave) 
   /*-{
      var options = new Object();
      options.format = format;
      options.width = width ;
      options.height = height ;
      options.keepRatio = keepRatio;
      options.viewAfterSave = viewAfterSave;
      return options ;
   }-*/;

   public static native boolean areEqual(ExportPlotOptions a, ExportPlotOptions b) /*-{
      if (a === null ^ b === null)
         return false;
      if (a === null)
         return true;
      return a.format === b.format &&
             a.width === b.width &&
             a.height === b.height &&
             a.keepRatio === b.keepRatio &&
             a.viewAfterSave === b.viewAfterSave;      
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
}

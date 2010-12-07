/*
 * ExportOptions.java
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

public class ExportOptions extends JavaScriptObject
{
   protected ExportOptions()
   {   
   }
   
   public static final String PNG_TYPE = "PNG";
   public static final String JPEG_TYPE = "JPEG";
   
   public static final native ExportOptions create(String type,
                                                   int width, 
                                                   int height) /*-{
      var options = new Object();
      options.type = type;
      options.width = width ;
      options.height = height ;
      return options ;
   }-*/;

   public static native boolean areEqual(ExportOptions a, ExportOptions b) /*-{
      if (a === null ^ b === null)
         return false;
      if (a === null)
         return true;
      return a.type === b.type &&
             a.width === b.width &&
             a.height === b.height;      
   }-*/;
   
   public final native String getType() /*-{
      return this.type;
   }-*/;
   
   public final native int getWidth() /*-{
      return this.width;
   }-*/;
   public final native int getHeight() /*-{
      return this.height;
   }-*/;
}

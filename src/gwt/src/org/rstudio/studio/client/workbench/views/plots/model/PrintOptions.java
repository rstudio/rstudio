/*
 * PrintOptions.java
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

public class PrintOptions extends JavaScriptObject
{
   protected PrintOptions()
   {   
   }
   
   public static final native PrintOptions create(double width, 
                                                  double height) /*-{
      var options = new Object();
      options.width = width ;
      options.height = height ;
      return options ;
   }-*/;
   
   // size
   public final native double getWidth() /*-{
      return this.width;
   }-*/;
   public final native double getHeight() /*-{
      return this.height;
   }-*/;
   
   

}

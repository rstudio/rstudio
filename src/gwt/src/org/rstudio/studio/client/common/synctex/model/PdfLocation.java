/*
 * PdfLocation.java
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

package org.rstudio.studio.client.common.synctex.model;

import com.google.gwt.core.client.JavaScriptObject;

public class PdfLocation extends JavaScriptObject
{
   protected PdfLocation()
   {
   }
   
   public final static native PdfLocation create(String file,
                                                 int page,
                                                 double x,
                                                 double y,
                                                 double width,
                                                 double height,
                                                 boolean fromClick) /*-{
      var location = new Object();
      location.file = file;
      location.page = page;
      location.x = x;
      location.y = y;
      location.width = width;
      location.height = height;
      location.from_click = fromClick;
      return location;
   }-*/;
   
   public final native String getFile() /*-{
      return this.file;
   }-*/;
   
   public native final int getPage() /*-{
      return this.page;
   }-*/;

   public native final double getX() /*-{
      return this.x;
   }-*/;
   
   public native final double getY() /*-{
      return this.y;
   }-*/;
   
   public native final double getWidth() /*-{
      return this.width;
   }-*/;
   
   public native final double getHeight() /*-{
      return this.height;
   }-*/;
   
   public native final boolean isFromClick() /*-{
      return this.from_click;
   }-*/;
   
   public final String toDebugString() 
   {
      StringBuilder str = new StringBuilder();
      str.append(getFile());
      str.append("; Page ");
      str.append(getPage());
      str.append(" {" + (int)getX() + ", " + (int)getY() + ", " + 
                 (int)getWidth() + ", " + (int)getHeight() + "}");
      if (isFromClick())
         str.append(" [From Click]");
      return str.toString();
      
   }
}

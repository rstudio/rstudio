/*
 * PdfLocation.java
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
                                                 double height) /*-{
      var location = new Object();
      location.file = file;
      location.page = page;
      location.x = x;
      location.y = y;
      location.width = width;
      location.height = height;
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
}

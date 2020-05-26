/*
 * SatelliteWindowGeometry.java
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
package org.rstudio.studio.client.common.satellite.model;

import org.rstudio.core.client.Point;
import org.rstudio.core.client.Size;

import com.google.gwt.core.client.JavaScriptObject;

public class SatelliteWindowGeometry extends JavaScriptObject
{
   protected SatelliteWindowGeometry() {}
   
   public final native static SatelliteWindowGeometry create(int ordinal,
         int x, int y, int width, int height) /*-{
      return {
         "ordinal": ordinal,
         "x"      : x,
         "y"      : y,
         "width"  : width,
         "height" : height
      };
   }-*/;
   
   public final native int getOrdinal() /*-{
      return this.ordinal;
   }-*/;

   public final native int getX() /*-{
      return this.x;
   }-*/;

   public final native int getY() /*-{
      return this.y;
   }-*/;

   public final native int getWidth() /*-{
      return this.width;
   }-*/;

   public final native int getHeight() /*-{
      return this.height;
   }-*/;
   
   public final boolean equals(SatelliteWindowGeometry other)
   {
      return getOrdinal() == other.getOrdinal() &&
             getX()       == other.getX() &&
             getY()       == other.getY() &&
             getWidth()   == other.getWidth() &&
             getHeight()  == other.getHeight();
   }
   
   public final Point getPosition() 
   {
      return Point.create(getX(), getY());
   }
   
   public final Size getSize()
   {
      return new Size(getWidth(), getHeight());
   }
}

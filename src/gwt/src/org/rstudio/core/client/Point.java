/*
 * Point.java
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
package org.rstudio.core.client;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class Point
{
   public int x;
   public int y;
   
   @JsOverlay public final int getX() { return x; }
   @JsOverlay public final int getY() { return y; }
   @JsOverlay public static final Point create(int x, int y)
   {
      Point point = new Point();
      point.x = x;
      point.y = y;
      return point;
   }
   
   @JsOverlay
   public final String toString()
   {
      return x + ", " + y;
   }
}

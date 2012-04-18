/*
 * Rectangle.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

public class Rectangle
{
   private final int x;
   private final int y;
   private final int width;
   private final int height;
   
   public Rectangle(int x, int y, int width, int height)
   {
      super() ;
      this.x = x ;
      this.y = y ;
      this.width = width ;
      this.height = height ;
   }

   // Eclipse auto-generated
   @Override
   public int hashCode()
   {
      final int prime = 31 ;
      int result = 1 ;
      result = prime * result + height ;
      result = prime * result + width ;
      result = prime * result + x ;
      result = prime * result + y ;
      return result ;
   }

   // Eclipse auto-generated
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true ;
      if (obj == null)
         return false ;
      if (getClass() != obj.getClass())
         return false ;
      Rectangle other = (Rectangle) obj ;
      if (height != other.height)
         return false ;
      if (width != other.width)
         return false ;
      if (x != other.x)
         return false ;
      if (y != other.y)
         return false ;
      return true ;
   }
   
   public int getLeft()
   {
      return x ;
   }
   
   public int getTop()
   {
      return y ;
   }
   
   public int getWidth()
   {
      return width ;
   }
   
   public int getHeight()
   {
      return height ;
   }
   
   public int getRight()
   {
      return x + width ;
   }
   
   public int getBottom()
   {
      return y + height ;
   }
   
   public Point getLocation()
   {
      return new Point(x, y) ;
   }
   
   public Size getSize()
   {
      return new Size(width, height) ;
   }
   
   public Point getCorner(boolean left, boolean top)
   {
      return new Point(left ? getLeft() : getRight(),
                       top ? getTop() : getBottom()) ;
   }
}

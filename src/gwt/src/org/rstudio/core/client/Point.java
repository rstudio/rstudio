/*
 * Point.java
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

public class Point
{
   public final int x;
   public final int y;
   
   public Point(int x, int y)
   {
      super() ;
      this.x = x ;
      this.y = y ;
   }
   
   public int getX()
   {
      return x ;
   }
   
   public int getY()
   {
      return y ;
   }

   @Override
   public int hashCode()
   {
      final int prime = 31 ;
      int result = 1 ;
      result = prime * result + x ;
      result = prime * result + y ;
      return result ;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true ;
      if (obj == null)
         return false ;
      if (getClass() != obj.getClass())
         return false ;
      Point other = (Point) obj ;
      if (x != other.x)
         return false ;
      if (y != other.y)
         return false ;
      return true ;
   }
   
   @Override
   public String toString()
   {
      return x + ", " + y;
   }
}

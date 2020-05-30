/*
 * Size.java
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

public class Size
{
   public final int width;
   public final int height;
   
   public Size(int width, int height)
   {
      this.width = width;
      this.height = height;
   }
   
   public int getX()
   {
      return width;
   }
   
   public int getY()
   {
      return height;
   }
   
   public boolean isEmpty()
   {
      return width == 0 && height == 0;
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + width;
      result = prime * result + height;
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Size other = (Size) obj;
      if (width != other.width)
         return false;
      return height == other.height;
   }
}

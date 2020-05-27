/*
 * MathUtil.java
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

public class MathUtil
{
   public static double clamp(double value, double low, double high)
   {
      if (value < low)
         return low;
      else if (value > high)
         return high;
      else
         return value;
   }
   
   public static int clamp(int value, int low, int high)
   {
      if (value < low)
         return low;
      else if (value > high)
         return high;
      else
         return value;
   }
   
   /**
    * Checks if an int value is in a range.
    * @param value value to check
    * @param min min value
    * @param max max value
    * @return whether value is in the range, inclusively.
    */
   public static boolean inRange(int value, int min, int max) {
       return (value <= max) && (value >= min);
   }

   public static boolean isEqual(double d1, double d2, double threshold)
   {
      return Math.abs(d1 - d2) < threshold;
   }
}

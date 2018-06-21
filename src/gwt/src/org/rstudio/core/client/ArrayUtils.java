/*
 * ArrayUtils.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import com.google.gwt.core.client.JsArrayString;

public class ArrayUtils
{
   public static boolean contains(JsArrayString array, String key)
   {
      for (int i = 0; i < array.length(); i++) {
         if (key.equals(array.get(i))) {
            return true;
         }
      }

      return false;
   }
}

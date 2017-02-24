/*
 * Functional.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import java.util.Collection;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class Functional
{
   public interface Predicate<T>
   {
      public boolean test(T t);
   }
   
   public static <T> T find(Collection<T> collection,
                            Predicate<T> predicate)
   {
      for (T t : collection)
         if (predicate.test(t))
            return t;
      return null;
   }
   
   public static <T extends JavaScriptObject> T find(JsArray<T> array,
                                                     Predicate<T> predicate)
   {
      for (int i = 0, n = array.length(); i < n; i++)
         if (predicate.test(array.get(i)))
            return array.get(i);
      return null;
   }
   
   public static String find(JsArrayString array, Predicate<String> predicate)
   {
      for (int i = 0, n = array.length(); i < n; i++)
         if (predicate.test(array.get(i)))
            return array.get(i);
      return null;
   }
}

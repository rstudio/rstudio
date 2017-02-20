/*
 * ListUtil.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class ListUtil
{
   public interface FilterPredicate<T>
   {
      public boolean test(T object);
   }
   
   public interface SearchPredicate<T>
   {
      public boolean test(T object);
   }
   
   public static <T> List<T> filter(List<T> list, FilterPredicate<T> predicate)
   {
      List<T> filtered = new ArrayList<T>();
      for (T object : list)
         if (predicate.test(object))
            filtered.add(object);
      return filtered;
   }
   
   public static <T> T find(List<T> list, SearchPredicate<T> predicate)
   {
      for (T t : list)
         if (predicate.test(t))
            return t;
      return null;
   }
   
   @SuppressWarnings("unchecked")
   public static <T> List<T> create(T... ts)
   {
      List<T> result = new ArrayList<T>(ts.length);
      for (T t : ts) result.add(t);
      return result;
   }
   
   public static List<String> create(JsArrayString array)
   {
      List<String> list = new ArrayList<String>();
      for (int i = 0, n = array.length(); i < n; i++)
         list.add(array.get(i));
      return list;
   }
   
   public static <T extends JavaScriptObject> List<T> create(JsArray<T> array)
   {
      List<T> list = new ArrayList<T>();
      for (int i = 0, n = array.length(); i < n; i++)
         list.add(array.get(i));
      return list;
   }
}

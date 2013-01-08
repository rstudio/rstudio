/*
 * JsUtil.java
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
package org.rstudio.core.client.js;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import java.util.Iterator;

public class JsUtil
{
   public static Iterable<String> asIterable(final JsArrayString array)
   {
      return new Iterable<String>()
      {
         @Override
         public Iterator<String> iterator()
         {
            return new Iterator<String>()
            {
               int i = 0;

               @Override
               public boolean hasNext()
               {
                  return array.length() > i;
               }

               @Override
               public String next()
               {
                  return array.get(i++);
               }

               @Override
               public void remove()
               {
                  throw new UnsupportedOperationException();
               }
            };
         }
      };
   }

   public static <T extends JavaScriptObject> Iterable<T> asIterable(
                                                         final JsArray<T> array)
   {
      return new Iterable<T>()
      {
         @Override
         public Iterator<T> iterator()
         {
            return new Iterator<T>()
            {
               int i = 0;

               @Override
               public boolean hasNext()
               {
                  return array.length() > i;
               }

               @Override
               public T next()
               {
                  return array.get(i++);
               }

               @Override
               public void remove()
               {
                  throw new UnsupportedOperationException();
               }
            };
         }
      };
   }

   public static boolean areEqual(JsArrayString a, JsArrayString b)
   {
      if (a == null && b == null)
         return true;
      else if (a == null && b != null)
         return false;
      else if (a != null && b == null)
         return false;
      else if (a.length() != b.length())
         return false;
      else
      {
         for (int i=0; i<a.length(); i++)
         {
            if (!a.get(i).equals(b.get(i)))
               return false; 
         }
         
         return true;
      }
   }
   
   public static String[] toStringArray(JsArrayString strings)
   {
      String[] result = new String[strings.length()];
      for (int i = 0; i < strings.length(); i++)
         result[i] = strings.get(i);
      return result;
   }

   public static JsArrayString toJsArrayString(Iterable<String> strings)
   {
      JsArrayString result = JsArrayString.createArray().cast();
      for (String s : strings)
         result.push(s);
      return result;
   }

   public native static JavaScriptObject createEmptyArray(int length) /*-{
      return new Array(length);
   }-*/;

}

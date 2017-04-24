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
import com.google.gwt.core.client.JsArrayBoolean;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;

import java.util.Collection;
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
   
   public static <T extends JavaScriptObject> Iterable<T> asReverseIterable(final JsArray<T> array)
   {
      return new Iterable<T>()
      {
         @Override
         public Iterator<T> iterator()
         {
            return new Iterator<T>()
            {
               int index_ = array.length() - 1;

               @Override
               public boolean hasNext()
               {
                  return index_ > 0;
               }

               @Override
               public T next()
               {
                  return array.get(index_--);
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
   
   public static JsArrayString toJsArrayString(String[] strings)
   {
      JsArrayString result = JsArrayString.createArray().cast();
      for (String s : strings)
         result.push(s);
      return result;
   }
   
   public static JsArrayBoolean toJsArrayBoolean(Iterable<Boolean> strings)
   {
      JsArrayBoolean result = JsArrayBoolean.createArray().cast();
      for (Boolean s : strings)
         result.push(s);
      return result;
   }
   
   public static JsArrayBoolean toJsArrayBoolean(Boolean[] strings)
   {
      JsArrayBoolean result = JsArrayBoolean.createArray().cast();
      for (Boolean s : strings)
         result.push(s);
      return result;
   }
   
   public static JsArrayInteger toJsArrayInteger(Iterable<Integer> integers)
   {
      JsArrayInteger result = JsArrayInteger.createArray().cast();
      for (Integer i : integers)
         result.push(i);
      return result;
   }
   
   public static <T extends JavaScriptObject> JsArray<T> toJsArray(Collection<T> collection)
   {
      JsArray<T> object = JavaScriptObject.createArray().cast();
      for (T t : collection)
         object.push(t);
      return object;
   }
   
   public native static JavaScriptObject createEmptyArray(int length) /*-{
      return new Array(length);
   }-*/;
   
   public native static String getObjectType(JavaScriptObject obj) /*-{
     var s = typeof obj;
     if (s === "object") 
     {
        if (obj) 
        {
           // this ugly check for array types is necessary because 
           // "instanceof Array" and friends don't work for arrays created in
           // other windows:
           // http://javascript.crockford.com/remedial.html
           if (Object.prototype.toString.call(obj) == "[object Array]") 
               s = "array";
        } 
        else 
           s = "null";
     }
     return s;
  }-*/;
}

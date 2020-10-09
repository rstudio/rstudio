/*
 * JsArrayUtil.java
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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class JsArrayUtil
{
   public static boolean jsArrayStringContains(JsArrayString haystack, 
                                               String needle)
   {
      for (int i = 0; i < haystack.length(); i++)
      {
         if (haystack.get(i) == needle)
            return true;
      }
      return false;
   }
   
   public static final native <T extends JavaScriptObject> T jsFindInCollection(
         JsArray<T> haystack, 
         String property, 
         String needle) /*-{
      for (var i = 0; i < haystack.length; i++)
      {
         if (haystack[i][property] === needle)
            return haystack[i];
      }
      return null;
   }-*/;
   
   public static <T extends JavaScriptObject> void fillList(JsArray<T> jsArray, 
         List<T> list) {
      for (int i = 0; i < jsArray.length(); ++i) {
         list.add(jsArray.get(i));
      }
   }
   
   public static <T extends JavaScriptObject> ArrayList<T> toArrayList(
         JsArray<T> jsArray)
   {
      ArrayList<T> list = new ArrayList<T>();
      fillList(jsArray, list);
      return list;
   }
   
   public static <T extends JavaScriptObject> JsArray<T> toJsArray(List<T> list)
   {
      JsArray<T> array = JsArray.createArray().cast();
      for (T item: list)
      {
         array.push(item);
      }
      return array;
   }
   
   
   public static JsArrayString toJsArrayString(List<String> in)
   {
      JsArrayString out = JavaScriptObject.createArray().cast();
      for (int i = 0; i < in.size(); i ++)
      {
         out.push(in.get(i));
      }
      return out;
   }
   
   public static ArrayList<String> fromJsArrayString(JsArrayString in)
   {
      ArrayList<String> out = new ArrayList<String>();
      for (int i = 0; i < in.length(); i++)
      {
         out.add(in.get(i));
      }
      return out;
   }
   
   public static boolean jsArrayStringEqual(JsArrayString first, 
         JsArrayString second)
   {
      if (first.length() != second.length())
         return false;
      for (int i = 0; i < first.length(); i++)
      {
         if (first.get(i) != second.get(i))
            return false;
      }
      return true;
   }
   
   public final static native void remove(JsArray<?> array, int index) /*-{
      array.splice(index, 1);
   }-*/;
   
   public static JsArrayString concat(JsArrayString a, JsArrayString b)
   {
      JsArrayString ab = JsArrayString.createArray().cast();
      for (int i = 0; i < a.length(); i++)
         ab.push(a.get(i));
      for (int i = 0; i < b.length(); i++)
         ab.push(b.get(i));
      return ab;
   }
   
   public static final native <T extends JavaScriptObject> JsArray<T> copy(JsArray<T> object) /*-{
      return object.slice();
   }-*/;

   public static JsArrayString copy(JsArrayString array)
   {
      if (array == null)
         return null;

      JsArrayString copy = JsArrayString.createArray().cast();
      for (int i = 0; i < array.length(); i++)
         copy.push(array.get(i));
      return copy;
   }
   
   public static JsArrayString createStringArray(String ...strings)
   {
      JsArrayString result = JsArray.createArray().cast();
      for (String str: strings)
      {
         result.push(str);
      }
      return result;
   }
   
   public static JsArrayString createStringArray(List<String> strings)
   {
      JsArrayString result = JsArray.createArray().cast();
      for (String str: strings)
      {
         result.push(str);
      }
      return result;
      
   }
   
   @SuppressWarnings("unchecked")
   public static final <T extends JavaScriptObject> JsArray<T> deepCopy(JsArray<T> array)
   {
      JsObject original = (JsObject) array.cast();
      JsObject clone = original.clone();
      JsArray<T> cloneArray = (JsArray<T>) clone.cast();
      return cloneArray;
   }
}

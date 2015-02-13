/*
 * JsArrayUtil.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

public class JsArrayUtil
{
   public static boolean jsArrayStringContains(JsArrayString haystack, 
                                               String needle)
   {
      for (int i = 0; i < haystack.length(); i++)
      {
         if (haystack.get(i).equals(needle))
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
}

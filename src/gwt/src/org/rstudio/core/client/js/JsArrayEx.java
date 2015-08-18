/*
 * JsArrayEx.java
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
package org.rstudio.core.client.js;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/*
 * Similar to JsArray, but doesn't presume that all elements in the array have
 * the same type.
 */
public class JsArrayEx extends JavaScriptObject
{
   protected JsArrayEx() 
   {
   }

   public final native double getDouble(int idx) /*-{
      return this[idx];
   }-*/;

   public final native String getString(int idx) /*-{
      return this[idx];
   }-*/;
   
   public final native boolean getBoolean(int idx) /*-{
      return this[idx];
   }-*/;

   public final native JavaScriptObject getArray(int idx) /*-{
      return this[idx];
   }-*/;

   public final native JsArrayEx getArrayEx(int idx) /*-{
      return this[idx];
   }-*/;

   public final native JavaScriptObject getObject(int idx) /*-{
      return this[idx];
   }-*/;
   
   public final native int length() /*-{
      return this.length;
   }-*/;
   
   // recursively creates a JSONArray from this array 
   public final JSONArray toJSONArray()
   {
      JSONArray array = new JSONArray();
      for (int i = 0; i < length(); i++)
      {
         String t = JsUtil.getObjectType(getObject(i));
         if (t == "array")
            array.set(i, getArrayEx(i).toJSONArray());
         else if (t == "number")
            array.set(i, new JSONNumber(getDouble(i)));
         else if (t == "string")
            array.set(i, new JSONString(getString(i)));
         else if (t == "boolean")
            array.set(i, JSONBoolean.getInstance(getBoolean(i)));
         else if (t == "null")
            array.set(i, JSONNull.getInstance());
         else if (t == "object")
            array.set(i, new JSONObject(getObject(i)));
      }
      return array;
   }
}

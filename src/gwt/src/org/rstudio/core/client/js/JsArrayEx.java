/*
 * JsArrayEx.java
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
package org.rstudio.core.client.js;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;

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
   
   public final native int getInt(int idx) /*-{
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
   
   // re-generate Arrays and Objects within a JavaScript object,
   // ensuring that the Array / Object prototypes belong to the
   // currently active window.
   private static final native JavaScriptObject ensureWindowOwnership(JavaScriptObject object) /*-{
      
      var isArray = Object.prototype.toString.call(object) === "[object Array]";
      var isNonNullObject = (object != null) && (!isArray) && (typeof(object) === "object");
      
      var result = object;
      
      if (isArray) {
         result = new Array(object.length);
         for (var i = 0; i < object.length; i++)
            result[i] = @org.rstudio.core.client.js.JsArrayEx::ensureWindowOwnership(Lcom/google/gwt/core/client/JavaScriptObject;)(object[i]);
      }
      
      else if (isNonNullObject) {
         result = {};
         var keys = Object.keys(object);
         for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            result[key] = @org.rstudio.core.client.js.JsArrayEx::ensureWindowOwnership(Lcom/google/gwt/core/client/JavaScriptObject;)(object[key]);
         }
      }
      
      return result;
      
   }-*/;
   
   public final JSONArray toJSONArray()
   {
      return new JSONArray(ensureWindowOwnership(this));
   }
}

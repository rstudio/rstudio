/*
 * JsObject.java
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
import com.google.gwt.core.client.JsArrayString;

public class JsObject extends JavaScriptObject
{
   public static native JsObject createJsObject() /*-{
      return {};
   }-*/;
   
   public static native JsObject fromJavaScriptObject(JavaScriptObject o) /*-{
      return o;
   }-*/;

   protected JsObject() {}

   public final native boolean hasKey(String key) /*-{
      return typeof(this[key]) != 'undefined';
   }-*/;

   public final native String getValueType(String key) /*-{
      return typeof(this[key]);
   }-*/;

   /***
    * Gets a JavaScriptObject type from the object.
    * 
    * @param key The name of the key.
    * @return The JavaScriptObject for the given key.
    */
   public final native <T extends JavaScriptObject> T getObject(String key) /*-{
      return this[key];
   }-*/;

   /***
    * Gets any element from the object, as any type. Useful primarily for
    * casting elements to JsType objects, which do not have a base type.
    * 
    * @param key The name of the key.
    * @return The element for the given key.
    */
   public final native <T> T getElement(String key) /*-{
      return this[key];
   }-*/;

   public final native void setObject(String key, JavaScriptObject value) /*-{
      this[key] = value;
   }-*/;

   public final native <T> void setElement(String key, T value) /*-{
      this[key] = value;
   }-*/;

   public final native String getString(String key) /*-{
      return this[key];
   }-*/;

   public final native String getString(String key, boolean autoconvert) /*-{
      if (autoconvert)
         return this[key] + "";
      else
         return this[key];
   }-*/;
   
   public final native String getAsString(String key) /*-{
      if (this[key] === null) {
         return "NULL";
      } else if (this[key] === true) {
         return "TRUE";
      } else if (this[key] === false) {
         return "FALSE";
      } else {
         return "" + this[key];
      }
   }-*/;

   public final native void setString(String key, String value) /*-{
      this[key] = value;
   }-*/;

   public final native void unset(String key) /*-{
      delete this[key];
   }-*/;

   public final Integer getInteger(String key)
   {
      if (!hasKey(key) || getValueType(key) != "number")
         return null;
      return _getInteger(key);
   }

   public final native int _getInteger(String key) /*-{
      return this[key];
   }-*/;

   public final void setInteger(String key, Integer value)
   {
      if (value == null)
         setObject(key, null);
      else
         _setInteger(key, value.intValue());
   }

   public final native void _setInteger(String key, int value) /*-{
      this[key] = value;
   }-*/;

   public final Double getDouble(String key)
   {
      if (!hasKey(key) || getValueType(key) != "number")
         return null;
      return _getDouble(key);
   }
   
   public final Double getDbl(String key)
   {
      return getDouble(key);
   }

   public final native double _getDouble(String key) /*-{
      return this[key];
   }-*/;

   public final void setDouble(String key, Double value)
   {
      if (value == null)
         setObject(key, null);
      else
         _setDouble(key, value.doubleValue());
   }

   public final native void _setDouble(String key, double value) /*-{
      this[key] = value;
   }-*/;

   public final Boolean getBoolean(String key)
   {
      if (!hasKey(key) || getValueType(key) != "boolean")
         return null;
      return _getBoolean(key);
   }
   
   public final Boolean getBool(String key)
   {
      return getBoolean(key);
   }

   public final native boolean _getBoolean(String key) /*-{
      return this[key];
   }-*/;

   public final void setBoolean(String key, Boolean value)
   {
      if (value == null)
         setObject(key, null);
      else
         _setBoolean(key, value.booleanValue());
   }

   public final native void _setBoolean(String key, boolean value) /*-{
      this[key] = value;
   }-*/;
   
   public final native void setJSO(String key, JavaScriptObject value) /*-{
      this[key] = value;
   }-*/;
   
   public final native void setJsArrayString(String key, JsArrayString value) /*-{
      this[key] = value;
   }-*/;

   public final native JsArrayString keys() /*-{
      return Object.keys(this);
   }-*/;
   
   public final Iterable<String> iterableKeys()
   {
      return JsUtil.asIterable(keys());
   }
   
   public final native void insert(JsObject other) /*-{
      for (var key in other) {
         this[key] = other[key];
      }
   }-*/;
   
   public final native JsObject clone() /*-{
      // recursive function to handle cloning the object
      var cloneObj = function(obj) {
         var cloned = null;
         if (obj == null || typeof obj != "object") 
            return obj;
         if (Object.prototype.toString.call(obj) == "[object Array]") {
            cloned = [];
            for (var i = 0; i < obj.length; i++) {
               cloned[i] = cloneObj(obj[i]);
            }
         } else if (Object.prototype.toString.call(obj) == "[object Object]") {
            cloned = {};
            for (var attrib in obj)
               if (obj.hasOwnProperty(attrib))
                  cloned[attrib] = cloneObj(obj[attrib]);
         } 
         return cloned;
      };
      
      // perform the clone on ourselves
      return cloneObj(this);
   }-*/;
}

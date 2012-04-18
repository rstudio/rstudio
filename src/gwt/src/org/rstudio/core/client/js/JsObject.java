/*
 * JsObject.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

   protected JsObject() {}

   public final native boolean hasKey(String key) /*-{
      return typeof(this[key]) != 'undefined';
   }-*/;

   public final native String getValueType(String key) /*-{
      return typeof(this[key]);
   }-*/;

   public final native <T extends JavaScriptObject> T getObject(String key) /*-{
      return this[key];
   }-*/;

   public final native void setObject(String key, JavaScriptObject value) /*-{
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

   public final native void setString(String key, String value) /*-{
      this[key] = value;
   }-*/;

   public final native void unset(String key) /*-{
      delete this[key];
   }-*/;

   public final Integer getInteger(String key)
   {
      if (!hasKey(key) || !getValueType(key).equals("number"))
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
      if (!hasKey(key) || !getValueType(key).equals("number"))
         return null;
      return _getDouble(key);
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
      if (!hasKey(key) || !getValueType(key).equals("boolean"))
         return null;
      return _getBoolean(key);
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

   public final native JsArrayString keys() /*-{
      var keys = [];
      for (var key in this) {
         keys.push(key);
      }
      return keys;
   }-*/;
}

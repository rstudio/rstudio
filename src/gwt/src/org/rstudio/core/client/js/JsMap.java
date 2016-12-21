/*
 * JsMap.java
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

/* Used for JsObject containers where all objects are of the same type.
 * Has a simpler interface than `JsObject` which allows objects with
 * arbitrarily typed elements.
 */

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class JsMap<T extends JavaScriptObject> extends JavaScriptObject
{
   protected JsMap() {}
   
   @SuppressWarnings("rawtypes")
   public static native final JsMap create() /*-{ return {}; }-*/;
   
   public native final T get(String key) /*-{
      return this[key];
   }-*/;
   
   public native final void set(String key, T value) /*-{
      this[key] = value;
   }-*/;
   
   public native final JsArrayString keys() /*-{
      return Object.keys(this);
   }-*/;
   
   public native final int size() /*-{
      return this.size;
   }-*/;
   
   public native final boolean isEmpty() /*-{
      return this.size === 0;
   }-*/;
}
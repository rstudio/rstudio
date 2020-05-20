/*
 * JsMapString.java
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

public class JsMapString extends JavaScriptObject
{
   protected JsMapString() {}
   
   public static native final JsMapString create() /*-{ return {}; }-*/;
   
   public native final String get(String key) /*-{
      return this[key];
   }-*/;
   
   public native final void set(String key, String value) /*-{
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

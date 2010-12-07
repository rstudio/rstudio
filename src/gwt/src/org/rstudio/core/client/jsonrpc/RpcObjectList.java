/*
 * RpcObjectList.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.jsonrpc;

import com.google.gwt.core.client.JavaScriptObject;

public class RpcObjectList<T extends JavaScriptObject> extends JavaScriptObject
{
   protected RpcObjectList()
   {
   }
   
   public final native int length() /*-{
      for (key in this)
      {
         return this[key].length;
      }
   }-*/;
   
   public final native T get(int index) /*-{
      var el = {};
      for (key in this)
      {
         el[key] = this[key][index];
      }
      return el;
   }-*/;
}

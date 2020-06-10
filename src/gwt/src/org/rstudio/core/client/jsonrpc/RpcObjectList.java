/*
 * RpcObjectList.java
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
package org.rstudio.core.client.jsonrpc;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.ArrayList;

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

   public final ArrayList<T> toArrayList()
   {
      ArrayList<T> result = new ArrayList<T>(length());
      for (int i = 0; i < length(); i++)
         result.add(get(i));
      return result;
   }
}

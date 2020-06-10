/*
 * AsyncCompletion.java
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
package org.rstudio.studio.client.server.remote;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.core.client.jsonrpc.RpcResponse;

public class AsyncCompletion extends JavaScriptObject
{
   protected AsyncCompletion()
   {
   }

   public final native String getHandle() /*-{
      return this.handle;
   }-*/;

   public final native RpcResponse getResponse() /*-{
      return this.response;
   }-*/;
}

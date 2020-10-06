/*
 * ServerRequestCallback.java
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

package org.rstudio.studio.client.server;

import org.rstudio.core.client.jsonrpc.RpcRequest;

public abstract class ServerRequestCallback<T>
{ 
   public abstract void onResponseReceived(T response);
   public abstract void onError(ServerError error);
   
   public void onRequestInitiated(RpcRequest request)
   {
      request_ = request;
   }

   public void cancel()
   {
      if (request_ != null)
         request_.cancel();
      cancelled_ = true;
   }

   public boolean cancelled() 
   {
      return cancelled_;
   }
   
   private boolean cancelled_ = false;
   private RpcRequest request_;
}


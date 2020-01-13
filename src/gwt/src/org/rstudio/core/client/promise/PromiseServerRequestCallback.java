/*
 * PromiseServerRequestCallback.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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

package org.rstudio.core.client.promise;

import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.json.client.JSONValue;

import elemental2.core.JsError;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;

public class PromiseServerRequestCallback<T> extends ServerRequestCallback<T> {

   public PromiseServerRequestCallback(ResolveCallbackFn<T> resolve, RejectCallbackFn reject) {
      this.resolve_ = resolve;
      this.reject_ = reject;
   }
   
   @Override
   public void onResponseReceived(T response)
   {
      resolve_.onInvoke(response);
   }
   
   @Override
   public void onError(ServerError error)
   {
      String errMsg = error.getUserMessage();
      JSONValue clientInfo = error.getClientInfo();
      if (clientInfo != null && clientInfo.isString() != null) 
      {
         errMsg = clientInfo.isString().stringValue();
      }
      reject_.onInvoke(new JsError(errMsg)); 
   }
   
   
   private ResolveCallbackFn<T> resolve_;
   private RejectCallbackFn reject_;
}
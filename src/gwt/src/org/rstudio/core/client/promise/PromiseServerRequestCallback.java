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
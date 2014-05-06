/*
 * RpcRequest.java
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

package org.rstudio.core.client.jsonrpc;

import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Random;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.jsonrpc.RequestLogEntry.ResponseType;

// NOTE: RpcRequest is an immutable object (all fields are marked final).
// this means that it is safe to re-submit an RpcRequest since the 
// re-submission will always be identical to the initial submission (useful
// for retries after network or authentication errors)
public class RpcRequest 
{
   public static final boolean TRACE = false ;
   
   public RpcRequest(String url, 
                     String method, 
                     JSONArray params, 
                     JSONObject kwparams,
                     boolean redactLog,
                     String sourceWindow,
                     String clientId,
                     double clientVersion)
   {
      url_ = url;
      method_ = method;
      params_ = params ;
      kwparams_ = kwparams;
      redactLog_ = redactLog;
      if (sourceWindow != null)
         sourceWindow_ = new JSONString(sourceWindow);
      else
         sourceWindow_ = null;
      if (clientId != null)
         clientId_ = new JSONString(clientId);
      else
         clientId_ = null;
      clientVersion_ = new JSONNumber(clientVersion);
   }
   
   public void send(RpcRequestCallback callback)
   {
      // final references for access from anonymous class
      final RpcRequest enclosingRequest = this ;
      final RpcRequestCallback requestCallback = callback ;
      
      // build json request object
      JSONObject request = new JSONObject() ;
      request.put("method", new JSONString(method_)) ;
      if ( params_ != null )
         request.put("params", params_);  
      if ( kwparams_ != null)
         request.put("kwparams", kwparams_);
      
      // add src window if we have it
      if (sourceWindow_ != null)
         request.put("sourceWnd", sourceWindow_);
      
      // add client id if we have it
      if (clientId_ != null)
         request.put("clientId", clientId_);
      
      // add client version
      request.put("version", clientVersion_);
      
      // configure request builder
      RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url_);
      builder.setHeader("Content-Type", "application/json") ;
      builder.setHeader("Accept", "application/json");
      String requestId = Integer.toString(Random.nextInt());
      builder.setHeader("X-RS-RID", requestId);
      
      // send request
      try
      {
         String requestString = request.toString();
         if (TRACE)
            Debug.log("Request: " + requestString) ;

         requestLogEntry_ = RequestLog.log(requestId,
                                           redactLog_ ? "[REDACTED]"
                                                      : requestString);

         request_ = builder.sendRequest(requestString, new RequestCallback() {
            
            public void onError(Request request, Throwable exception)
            {      
               requestLogEntry_.logResponse(ResponseType.Error,
                                           exception.getLocalizedMessage());
               // ERROR: Request failed
               RpcError error = RpcError.create(
                                          RpcError.TRANSMISSION_ERROR,
                                          exception.getLocalizedMessage());
               requestCallback.onError(enclosingRequest, error) ;
            }
            
            public void onResponseReceived(Request request, 
                                           Response response)
            {
               // only accept 200 responses
               int status = response.getStatusCode();
               if ( status == 200 )
               {
                  // attempt to parse the response
                  RpcResponse rpcResponse = null ;
                  try
                  {
                     String responseText = response.getText();
                     if (TRACE)
                        Debug.log("Response: " + responseText) ;
                     requestLogEntry_.logResponse(ResponseType.Normal,
                                                 responseText);
                     rpcResponse = RpcResponse.parse(responseText);
                     
                     // response received and validated, process it!
                     requestCallback.onResponseReceived(enclosingRequest, 
                                                        rpcResponse) ;
                  }
                  catch(Exception e)
                  {
                     // ERROR: Unable to parse JSON
                     RpcError error = RpcError.create(
                                                RpcError.TRANSMISSION_ERROR,
                                                e.getLocalizedMessage());
                     requestCallback.onError(enclosingRequest, error) ;
                  }
               }
               else
               {
                  // ERROR: Non-200 response from server
                  
                  // default error message
                  String message = "Status code " + 
                                   Integer.toString(status) + 
                                   " returned";
                  
                  // override error message for status code 0
                  if (status == 0)
                  {
                     message = "Unable to establish connection with R session"; 
                  }
                  
                 
                  requestLogEntry_.logResponse(ResponseType.Unknown,
                                              message);
                  RpcError error = RpcError.create(
                                             RpcError.TRANSMISSION_ERROR,
                                             message) ;
                  requestCallback.onError(enclosingRequest, error);
               }
            };
         });
      }
      catch(RequestException e)
      {
         // ERROR: general request failure
                 
         String message = e.getLocalizedMessage();
        
         if (requestLogEntry_ != null)
            requestLogEntry_.logResponse(ResponseType.Unknown, message);
         
         RpcError error = RpcError.create(RpcError.TRANSMISSION_ERROR,
                                          message);
         requestCallback.onError(enclosingRequest, error);
      }
   }
   
   public void cancel()
   {
      if (request_ != null)
      {
         request_.cancel();
         request_ = null;
      }
      
      if (requestLogEntry_ != null)
      {
         requestLogEntry_.logResponse(ResponseType.Cancelled, "Cancelled");
         requestLogEntry_ = null;
      }
   }
     
   final private String url_ ;
   final private String method_ ;
   final private JSONArray params_ ;
   final private JSONObject kwparams_;
   private final boolean redactLog_;
   final private JSONString sourceWindow_;
   final private JSONString clientId_;
   final private JSONNumber clientVersion_;
   private Request request_ = null;
   private RequestLogEntry requestLogEntry_ = null;
   
     
}

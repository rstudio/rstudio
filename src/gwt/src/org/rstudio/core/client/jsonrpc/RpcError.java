/*
 * RpcError.java
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
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

public class RpcError extends JavaScriptObject 
{ 
   public static final native RpcError create(int code, String message) /*-{
      var error = new Object();
      error.code = code;
      error.message = message;
      return error;
   }-*/;
   
   protected RpcError()
   {
   }
   
   // no error
   public final static int SUCCESS = 0;
   
   // couldn't connect to service (method not executed)
   public final static int CONNECTION_ERROR = 1;
   
   // service is currently unavailable
   public final static int UNAVAILABLE = 2;
   
   // not authorized to access service or method (method not executed)
   public final static int UNAUTHORIZED = 3;
   
   // provided client id is invalid (method not executed)
   public final static int INVALID_CLIENT_ID = 4;
   
   // protocol errors (method not executed)
   public final static int PARSE_ERROR = 5;
   public final static int INVALID_REQUEST = 6;
   public final static int METHOD_NOT_FOUND = 7;
   public final static int PARAM_MISSING = 8;
   public final static int PARAM_TYPE_MISMATCH = 9;
   public final static int PARAM_INVALID = 10;
   public final static int METHOD_UNEXEPECTED = 11;
   public final static int INVALID_CLIENT_VERSION = 12;
   public final static int SERVER_OFFLINE = 13;
   public final static int INVALID_SESSION = 14;
   public final static int MAX_SESSIONS_REACHED = 15;
   public final static int MAX_USERS_REACHED = 16;

   // this session is a launcher session and the launch parameters need to be resent to implicitly relaunch the session
   public final static int LAUNCH_PARAMETERS_MISSING = 17;

   // execution error (method was executed and returned known error state)
   public final static int EXECUTION_ERROR = 100;
     
   // transmission error (application state indeterminate)
   public final static int TRANSMISSION_ERROR = 200;
   
   public final native int getCode() /*-{
      return this.code;
   }-*/;
   
   public final native String getMessage() /*-{
      return this.message;
   }-*/;
   
   public final native String getRedirectUrl() /*-{
      return this.redirect_url;
   }-*/;
   
   public final native RpcUnderlyingError getError() /*-{
      return this.error;
   }-*/;
 
   public final JSONValue getClientInfo()
   {
      return new JSONObject(this).get("client_info");
   }

   public final String getEndUserMessage()
   {
      RpcUnderlyingError underlyingError = getError();
      if (underlyingError != null)
         return underlyingError.getMessage();
      else
         return getMessage();
   }
}

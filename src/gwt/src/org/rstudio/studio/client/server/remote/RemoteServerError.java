/*
 * RemoteServerError.java
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

import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONValue;
import org.rstudio.core.client.jsonrpc.RpcError;
import org.rstudio.core.client.jsonrpc.RpcUnderlyingError;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerErrorCause;

class RemoteServerError implements ServerError 
{
   public RemoteServerError(RpcError rpcError)
   {
      code_ = codeFromRpcErrorCode(rpcError.getCode());
      message_ = rpcError.getMessage();
      redirectUrl_ = rpcError.getRedirectUrl();
      
      RpcUnderlyingError rpcErrorCause = rpcError.getError();
      if (rpcErrorCause != null)
      {
         cause_ = new ServerErrorCause(rpcErrorCause.getCode(),
                                       rpcErrorCause.getCategory(),
                                       rpcErrorCause.getMessage());
      }
      else
      {
         cause_ = null;
      }
      clientInfo_ = rpcError.getClientInfo();
   }

   @Override
   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      sb.append(code_ + ": " + message_ + "\n");
      if (cause_ != null)
         sb.append(cause_.toString());
      return sb.toString();
   }

   public int getCode()
   {
      return code_;
   }
   
   public String getMessage() 
   {
      return message_;
   }
   
   public String getRedirectUrl()
   {
      return redirectUrl_;
   }
   
   public ServerErrorCause getCause()
   {
      return cause_;
   }
   
   public String getUserMessage()
   {
      if (cause_ != null)
         return cause_.getMessage();
      else
         return message_;
   }

   @Override
   public JSONValue getClientInfo()
   {
      return clientInfo_ == null ? JSONNull.getInstance() : clientInfo_;
   }

   private int codeFromRpcErrorCode(int code)
   {
      switch(code)
      {
      case RpcError.SUCCESS:
         return ServerError.SUCCESS;
         
      case RpcError.CONNECTION_ERROR:
         return ServerError.CONNECTION;
         
      case RpcError.UNAVAILABLE:
         return ServerError.UNAVAILABLE;
      
      case RpcError.UNAUTHORIZED:
         return ServerError.UNAUTHORIZED;
           
      case RpcError.PARSE_ERROR:
      case RpcError.INVALID_REQUEST:
      case RpcError.METHOD_NOT_FOUND:
      case RpcError.PARAM_MISSING:
      case RpcError.PARAM_TYPE_MISMATCH:
      case RpcError.PARAM_INVALID:
      case RpcError.METHOD_UNEXEPECTED:
         return ServerError.PROTOCOL;
      
      case RpcError.EXECUTION_ERROR:
         return ServerError.EXECUTION;
         
      case RpcError.TRANSMISSION_ERROR:
         return ServerError.TRANSMISSION;
         
      case RpcError.MAX_SESSIONS_REACHED:
      case RpcError.MAX_USERS_REACHED:
         return ServerError.LICENSE_USAGE_LIMIT;
               
      default:
         return ServerError.SUCCESS;
      }
   }
   
   private int code_;
   private String message_;
   private String redirectUrl_;
   private ServerErrorCause cause_;
   private JSONValue clientInfo_;
}

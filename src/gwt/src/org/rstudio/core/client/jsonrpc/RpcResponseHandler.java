/*
 * RpcResponseHandler.java
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

public abstract class RpcResponseHandler
{
   /**
    * Determine Rpc Response from the standard "result" field
    */
   public RpcResponseHandler()
   {
      resultFieldName_ = null;
   }
   
   /**
    * Determine Rpc Response from the specified field
    */
   public RpcResponseHandler(String resultFieldName)
   {
      resultFieldName_ = resultFieldName;
   }
   
   public abstract void onResponseReceived(RpcResponse response);
   
   public String getResultFieldName()
   {
      return resultFieldName_;
   }
   
   private final String resultFieldName_;
}

/*
 * RpcResponse.java
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
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

public class RpcResponse extends JavaScriptObject
{
   protected RpcResponse()
   {
      
   }
   
   public final static RpcResponse parse(String json) 
   {      
      try
      {
         // we first call parseStrict so we can use the browser
         // json parser (for performance) whenever possible)
         JSONValue val = JSONParser.parseStrict(json);
         return val.isObject().getJavaScriptObject().cast();
      }
      catch(Exception e)
      {
         try
         {
            // there are some cases where json emitted by our 
            // server isn't parsable by parseStrict (for example,
            // see bug #3025). for these situations we call 
            // parseLenient (which in turn calls eval)
            @SuppressWarnings("deprecation")
            JSONValue val = JSONParser.parseLenient(json);
            return val.isObject().getJavaScriptObject().cast();
         }
         catch(Exception e2)
         {
            return null;
         }
      }
   }
    
   public final native static RpcResponse create(RpcError error) /*-{
      var response = new Object();
      response.error = error;
      return response;
   }-*/;
   
   public final RpcError getError()
   {
      return getField("error");
   }

   public final String getAsyncHandle()
   {
      return getField("asyncHandle");
   }
   
   public final <T> T getResult()
   {
      T field = this.<T>getField("result");
      return field;
   }

   private static Boolean wrapBoolean(boolean value)
   {
      return value;
   }
   
   public final native <T> T getField(String name) /*-{
      var value = this[name];
      if (typeof(value) == 'boolean')
         return @org.rstudio.core.client.jsonrpc.RpcResponse::wrapBoolean(Z)(value);
      return value;
   }-*/;
}

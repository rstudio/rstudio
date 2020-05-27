/*
 * ServerError.java
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

import com.google.gwt.json.client.JSONValue;

public interface ServerError
{
   // method succeeded
   public static final int SUCCESS = 0;
   
   // couldn't connect (method did not execute)
   public static final int CONNECTION = 1;
   
   // unavailable (method did not execute)
   public static final int UNAVAILABLE = 2;
   
   // unauthorized (method did not execute)
   public static final int UNAUTHORIZED = 3;
   
   // protocol (method did not execute)
   public static final int PROTOCOL = 4;
   
   // error during processing (method failed in known state)
   public static final int EXECUTION = 5;
     
   // rpc transmission errors (method may have executed)
   public static final int TRANSMISSION = 6;
   
   // errors indicating the license usage limit has been reached
   public static final int LICENSE_USAGE_LIMIT = 7;
 
   // error type
   int getCode();
   
   // error message 
   String getMessage();
   
   // optional redirect url
   String getRedirectUrl();
   
   // underlying error
   ServerErrorCause getCause();
   
   // message to display to the end-user
   String getUserMessage();

   JSONValue getClientInfo();

}

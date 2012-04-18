/*
 * RetryHandler.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.server.remote;

import org.rstudio.core.client.jsonrpc.RpcError;

interface RetryHandler
{
   // perform the retry 
   void onRetry();
   
   // will be called with the original error that caused the retry attempt
   // if there is an error involving the retry mechanism itself (e.g. error
   // occurs during attempt to authenticate in response to UNAUTHORIZED)
   void onError(RpcError error);
}

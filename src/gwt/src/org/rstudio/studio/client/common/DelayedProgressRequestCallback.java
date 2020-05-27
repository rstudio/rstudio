/*
 * DelayedProgressRequestCallback.java
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

package org.rstudio.studio.client.common;

import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

public abstract class DelayedProgressRequestCallback<T> 
                                 extends ServerRequestCallback<T>
{
   public DelayedProgressRequestCallback(String progressMessage)
   {
      this(new GlobalProgressDelayer(
         RStudioGinjector.INSTANCE.getGlobalDisplay(),  
         500, 
         progressMessage).getIndicator());
   }
   
   public DelayedProgressRequestCallback(ProgressIndicator indicator)
   {
      indicator_  = indicator;
   }
   
   @Override
   public void onResponseReceived(T response)
   {
      indicator_.onCompleted();
      onSuccess(response);
   }
   
   protected abstract void onSuccess(T response);

   @Override
   public void onError(ServerError error)
   {
      indicator_.onError(error.getUserMessage());
   }
   
   private ProgressIndicator indicator_;
}

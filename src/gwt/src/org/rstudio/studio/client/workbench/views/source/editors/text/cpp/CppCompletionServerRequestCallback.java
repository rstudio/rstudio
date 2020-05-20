/*
 * CppCompletionServerRequestCallback.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

public class CppCompletionServerRequestCallback<T> 
                            extends ServerRequestCallback<T>
{
   public CppCompletionServerRequestCallback(String message)
   {
      super();
      progressDelayer_ =  new GlobalProgressDelayer(
            RStudioGinjector.INSTANCE.getGlobalDisplay(), 500, message);
   }
   
   @Override
   public void onResponseReceived(T result)
   {
      progressDelayer_.dismiss();
      onSuccess(result);
   }
   
   @Override
   public void onError(ServerError error)
   {
      progressDelayer_.dismiss();
      onFailure(error);
   }
   
   protected void onSuccess(T result)
   {
   }

   protected void onFailure(ServerError error)
   {
   }

   private final GlobalProgressDelayer progressDelayer_;
}

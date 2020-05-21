/*
 * CompletionRequestContext.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import org.rstudio.core.client.Invalidation;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

public class CompletionRequestContext extends ServerRequestCallback<Completions>
{
   public static class Data
   {
      public Data(String line,
                  boolean isTabTriggeredCompletion,
                  boolean autoAcceptSingleCompletionResult)
      {
         line_ = line;
         isTabTriggeredCompletion_ = isTabTriggeredCompletion;
         autoAcceptSingleCompletionResult_ = autoAcceptSingleCompletionResult;
      }
      
      public String getLine()
      {
         return line_;
      }
      
      public boolean isTabTriggeredCompletion()
      {
         return isTabTriggeredCompletion_;
      }
      
      public boolean autoAcceptSingleCompletionResult()
      {
         return autoAcceptSingleCompletionResult_;
      }
      
      private final String line_;
      private final boolean isTabTriggeredCompletion_;
      private final boolean autoAcceptSingleCompletionResult_;
   }
   
   public interface Host
   {
      public Invalidation.Token getInvalidationToken();
      public void onCompletionResponseReceived(Data data, Completions completions);
      public void onCompletionRequestError(String message);
   }
   
   public CompletionRequestContext(Host host, Data data)
   {
      host_ = host;
      token_ = host.getInvalidationToken();
      data_ = data;
   }

   @Override
   public void onError(ServerError error)
   {
      if (token_.isInvalid())
         return;

      host_.onCompletionRequestError(error.getUserMessage());
   }

   @Override
   public void onResponseReceived(Completions completions)
   {
      if (token_.isInvalid())
         return;
      
      host_.onCompletionResponseReceived(data_, completions);
   }

   private final Host host_;
   private final Invalidation.Token token_;
   private final Data data_;
}

/*
 * CompletionRequestContext.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
   public interface Host
   {
      public void onCompletionResponseReceived(String line, boolean canAutoAccept, Completions completions);
      public void onCompletionRequestError(String message);
   }
   
   public CompletionRequestContext(Host host,
                                   String line,
                                   boolean canAutoAccept,
                                   Invalidation.Token token)
   {
      host_ = host;
      line_ = line;
      canAutoAccept_ = canAutoAccept;
      invalidationToken_ = token;
   }

   @Override
   public void onError(ServerError error)
   {
      if (invalidationToken_.isInvalid())
         return;

      host_.onCompletionRequestError(error.getUserMessage());
   }

   @Override
   public void onResponseReceived(Completions completions)
   {
      if (invalidationToken_.isInvalid())
         return;
      
      host_.onCompletionResponseReceived(line_, canAutoAccept_, completions);
   }

   private final Host host_;
   private final String line_;
   private final boolean canAutoAccept_;
   private final Invalidation.Token invalidationToken_;
}

/*
 * HelpSearchOracle.java
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
package org.rstudio.studio.client.workbench.views.help.search;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.inject.Inject;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;

import java.util.ArrayList;

public class HelpSearchOracle extends SuggestOracle
{
   @Inject
   public HelpSearchOracle(HelpServerOperations server)
   {
      server_ = server;
   }

   @Override
   public void requestSuggestions(final Request request, 
                                  final Callback callback)
   {
      String query = request.getQuery();
      server_.suggestTopics(query,
                             new ServerRequestCallback<JsArrayString>() {
         @Override
         public void onError(ServerError error)
         {
         }

         @Override
         public void onResponseReceived(JsArrayString suggestions)
         {
            int maxCount = Math.min(suggestions.length(), request.getLimit());

            ArrayList<SearchSuggestion> results =
               new ArrayList<SearchSuggestion>();
            for (int i = 0; i< maxCount; i++)
               results.add(new SearchSuggestion(suggestions.get(i)));
            
            callback.onSuggestionsReady(request, new Response(results));
         }
      });
   }
   
   private class SearchSuggestion implements Suggestion
   {
      public SearchSuggestion(String value)
      {
         value_ = value;
      }

      public String getDisplayString()
      {
         return value_;
      }

      public String getReplacementString()
      {
         return value_;
      }
      
      private final String value_;
   }

   private final HelpServerOperations server_;
}

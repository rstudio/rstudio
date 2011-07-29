/*
 * CodeSearchOracle
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.codesearch;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.inject.Inject;

import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchResult;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;

import java.util.ArrayList;

public class CodeSearchOracle extends SuggestOracle
{
   @Inject
   public CodeSearchOracle(CodeSearchServerOperations server)
   {
      server_ = server ;
   }

   @Override
   public void requestSuggestions(final Request request, 
                                  final Callback callback)
   {
      String query = request.getQuery() ;
      
      lastQuery_ = query;
      
      server_.searchCode(
            query,
            new SimpleRequestCallback<JsArray<CodeSearchResult>>() {
         
         @Override
         public void onResponseReceived(JsArray<CodeSearchResult> results)
         {
            int maxCount = Math.min(results.length(), request.getLimit());

            ArrayList<SearchSuggestion> suggestions =
                              new ArrayList<SearchSuggestion>() ;
            for (int i = 0; i< maxCount; i++)
               suggestions.add(new SearchSuggestion(
                               results.get(i).getFunctionName())) ;
            
            callback.onSuggestionsReady(request, new Response(suggestions)) ;
         }
      }); ;
   }
   
   private class SearchSuggestion implements Suggestion
   {
      public SearchSuggestion(String value)
      {
         value_ = value ;
      }

      public String getDisplayString()
      {
         return value_ ;
      }

      public String getReplacementString()
      {
         return lastQuery_ ;
      }
      
      private final String value_ ;
   }

   private final CodeSearchServerOperations server_ ;
   
   private String lastQuery_;
}

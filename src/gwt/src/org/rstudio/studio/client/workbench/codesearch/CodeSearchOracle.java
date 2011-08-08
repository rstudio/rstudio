package org.rstudio.studio.client.workbench.codesearch;

import java.util.ArrayList;

import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.codesearch.model.CodeNavigationTarget;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchResults;
import org.rstudio.studio.client.workbench.codesearch.model.RFileItem;
import org.rstudio.studio.client.workbench.codesearch.model.RSourceItem;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.inject.Inject;

public class CodeSearchOracle extends SuggestOracle
{
   @Inject
   public CodeSearchOracle(CodeSearchServerOperations server)
   {
      server_ = server;
   }
   
   
   @Override
   public void requestSuggestions(final Request request, 
                                  final Callback callback)
   {    
      // invalidate any outstanding search
      searchInvalidation_.invalidate();
      
      // first see if we can serve the request from the cache
      for (int i=resultCache_.size() - 1; i >= 0; i--)
      {
         // get the previous result
         SearchResult res = resultCache_.get(i);
         
         // exact match of previous query
         if (request.getQuery().equals(res.getQuery()))
         {
            callback.onSuggestionsReady(request, 
                                        new Response(res.getSuggestions()));
            return;
         }
         
         // if this query is a further refinement of a non-overflowed 
         // previous query then satisfy it by filtering the previous results
         if (!res.getMoreAvailable() && 
             request.getQuery().startsWith(res.getQuery()))
         {
            Pattern pattern = null;
            String queryLower = request.getQuery().toLowerCase();
            if (queryLower.indexOf('*') != -1)
               pattern = patternForTerm(queryLower);
            
            ArrayList<CodeSearchSuggestion> suggestions =
                                       new ArrayList<CodeSearchSuggestion>();
            for (int s=0; s<res.getSuggestions().size(); s++)
            {
               CodeSearchSuggestion sugg = res.getSuggestions().get(s);
               
               String name = sugg.getMatchedString().toLowerCase();
               if (pattern != null)
               {
                  Match match = pattern.match(name, 0);
                  if (match != null && match.getIndex() == 0)
                     suggestions.add(sugg);
               }
               else
               {
                  if (name.startsWith(queryLower))
                     suggestions.add(sugg);
               }
            }

            // cache suggestions. note that this adds an item to the end
            // of the resultCache_ (which we are currently iterating over)
            // not a big deal because we are about to return out of the loop
            cacheSuggestions(request, suggestions, false);
            
            // return suggestions
            callback.onSuggestionsReady(request, new Response(suggestions));
            
            return;
         } 
      }
      
      // failed to short-circuit via the cache, hit the server
      codeSearch_.enqueRequest(request, callback); 
   }
     
   public CodeNavigationTarget navigationTargetFromSuggestion(Suggestion sugg)
   {
      return ((CodeSearchSuggestion)sugg).getNavigationTarget();
   }
   
   public void invalidateSearches()
   {
      searchInvalidation_.invalidate();
   }
   
   public void clear()
   {
      resultCache_.clear();
   }
   
   @Override
   public boolean isDisplayStringHTML()
   {
      return true;
   }
   
   private Pattern patternForTerm(String term)
   {
      // split the term on *
      StringBuilder regex = new StringBuilder();
      String[] components = term.split("\\*", -1);
      for (int i=0; i<components.length; i++)
      {
         if (i > 0)
            regex.append(".*");
         regex.append(Pattern.escape(components[i]));
      }    
      return Pattern.create(regex.toString());
   }
   
   private class CodeSearchCommand extends TimeBufferedCommand  
   {
      public CodeSearchCommand()
      {
         super(300);
      }
      
      public void enqueRequest(Request request, Callback callback)
      {
         request_ = request;
         callback_ = callback;
         invalidationToken_ = searchInvalidation_.getInvalidationToken();
         nudge();
      }

      @Override
      protected void performAction(boolean shouldSchedulePassive)
      {  
         // failed to short-circuit via the cache, hit the server
         server_.searchCode(
               request_.getQuery(),
               request_.getLimit(),
               new SimpleRequestCallback<CodeSearchResults>() {
            
            @Override
            public void onResponseReceived(CodeSearchResults response)
            {  
               ArrayList<CodeSearchSuggestion> suggestions = 
                                       new ArrayList<CodeSearchSuggestion>();
               
               // file results
               ArrayList<RFileItem> fileResults = 
                                    response.getRFileItems().toArrayList();
               for (int i = 0; i<fileResults.size(); i++) 
                  suggestions.add(new CodeSearchSuggestion(fileResults.get(i)));  
               
               
               // src results
               ArrayList<RSourceItem> srcResults = 
                                    response.getRSourceItems().toArrayList();
               for (int i = 0; i<srcResults.size(); i++) 
                  suggestions.add(new CodeSearchSuggestion(srcResults.get(i)));     
               
               // cache suggestions
               cacheSuggestions(request_, 
                                suggestions,
                                response.getMoreAvailable());
               
               // return suggestions
               if (!invalidationToken_.isInvalid())
               {
                  callback_.onSuggestionsReady(request_, 
                                               new Response(suggestions));
               }
            }
         });
         
      }
      
      private Request request_;
      private Callback callback_;
      private Invalidation.Token invalidationToken_;
   };
   
   
   private void cacheSuggestions(Request request, 
                                 ArrayList<CodeSearchSuggestion> suggestions,
                                 boolean moreAvailable)
   {
      // cache the suggestions (up to 15 active result sets cached)
      // NOTE: the cache is cleared on gain focus, lost focus, and 
      // the search term reverting back to empty)
      if (resultCache_.size() > 15)
         resultCache_.remove(0);
      
      resultCache_.add(new SearchResult(request.getQuery(), 
                                        suggestions, 
                                        moreAvailable));
   }
   
   private final Invalidation searchInvalidation_ = new Invalidation();
   
   private final CodeSearchServerOperations server_ ;
   private final CodeSearchCommand codeSearch_ = new CodeSearchCommand();
   
   private final ArrayList<SearchResult> resultCache_ = 
                                             new ArrayList<SearchResult>();
   
   private class SearchResult
   {
      public SearchResult(String query, 
                          ArrayList<CodeSearchSuggestion> suggestions,
                          boolean moreAvailable)
      {
         query_ = query;
         suggestions_ = suggestions;
         moveAvailable_ = moreAvailable;
      }
      
      public String getQuery()
      {
         return query_;
      }
      
      public ArrayList<CodeSearchSuggestion> getSuggestions()
      {
         return suggestions_;
      }
      
      public boolean getMoreAvailable()
      {
         return moveAvailable_;
      }
      
      private final String query_;
      private final ArrayList<CodeSearchSuggestion> suggestions_;
      private final boolean moveAvailable_;
   }
   
}

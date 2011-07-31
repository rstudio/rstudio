package org.rstudio.studio.client.workbench.codesearch.model;

import java.util.ArrayList;

import org.rstudio.core.client.Pair;
import org.rstudio.studio.client.common.SimpleRequestCallback;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.SuggestOracle.Callback;
import com.google.gwt.user.client.ui.SuggestOracle.Request;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.inject.Inject;

public class CodeSearchSuggestions
{
   @Inject
   public CodeSearchSuggestions(CodeSearchServerOperations server)
   {
      server_ = server;
   }
   
   public void request(final Request request, final Callback callback)
   { 
      // will be used later
      boolean isPatternMatch = false;
      
      // first see if we have a cached version of this request's results
      for (Pair<Request,ArrayList<CodeSearchSuggestion>> result : resultCache_)
      {
         if (request.getQuery().equals(result.first.getQuery()) &&
             request.getLimit() == result.first.getLimit())
         {
            callback.onSuggestionsReady(request, new Response(result.second));
            return;
         }
      }
      
      // if this query is a further refinement of a previous query then
      // satisfy the request by simply filtering the previous results (note
      // that there won't be a lastQuery_ if the results overflowed)
      if (lastQuery_ != null && request.getQuery().startsWith(lastQuery_))
      { 
         ArrayList<CodeSearchSuggestion> suggestions = 
                                          new ArrayList<CodeSearchSuggestion>();
         for (int i=0; i<lastSuggestions_.size(); i++)
         {
            CodeSearchSuggestion sugg = lastSuggestions_.get(i);
            String functionName = sugg.getResult().getFunctionName();
            
            if (functionName.toLowerCase().startsWith(request.getQuery()))
               suggestions.add(sugg);
         }
         
         // yield the suggestions
         yieldSuggestions(request, suggestions, callback);
      }
      
      // if this isn't a pattern matching query, the previous results were
      // an overlfow, and all of the displayed results are fully prefaced by
      // this query then this query does not further disambiguate the results
      else if (!isPatternMatch &&
              (lastSuggestions_ != null) &&
              (lastSuggestions_.size() >= request.getLimit()) && 
              allPrevSuggestionsPrefacedBy(request.getQuery()))
      {
         yieldSuggestions(request, lastSuggestions_, callback);
      }
      
      else
      {  
         server_.searchCode(
               request.getQuery(),
               request.getLimit(),
               new SimpleRequestCallback<JsArray<CodeSearchResult>>() {
            
            @Override
            public void onResponseReceived(JsArray<CodeSearchResult> results)
            { 
               // read the response
               ArrayList<CodeSearchSuggestion> suggestions = 
                                       new ArrayList<CodeSearchSuggestion>();
               for (int i = 0; i<results.length(); i++) 
                  suggestions.add(new CodeSearchSuggestion(results.get(i)));     
               
               // if we got less than or equal to the max results then we can 
               // safely cache this for further filtering
               if (results.length() <= request.getLimit())
               {
                  // save the query
                  lastQuery_ = request.getQuery();
               }
               else
               {
                  // overwrite any previous saved query
                  lastQuery_ = null;
               }
               
               // always save the suggestions 
               lastSuggestions_ = suggestions;
               
               // yield suggestions
               yieldSuggestions(request, suggestions, callback); 
                                         
            }
         });
      }
   }
     
   public CodeSearchResult resultFromSuggestion(Suggestion suggestion)
   {
      return ((CodeSearchSuggestion)suggestion).getResult();
   }
   
   public void clear()
   {
      lastQuery_ = null;
      lastSuggestions_ = null;
      resultCache_.clear();
   }
   
   private boolean allPrevSuggestionsPrefacedBy(String query)
   { 
      for (int i=0; i<lastSuggestions_.size(); i++)
      {
         CodeSearchSuggestion sugg = lastSuggestions_.get(i);
         String functionName = sugg.getResult().getFunctionName();

         if (!functionName.toLowerCase().startsWith(query))
            return false;
      }
      
      return true;
   }
   
   // wrapper for yielding suggestions (so we always update the cache)
   private void yieldSuggestions(Request request, 
                                 ArrayList<CodeSearchSuggestion> suggestions,
                                 Callback callback)
   {
      // always cache the suggestions (up to 25 active result sets cached)
      // NOTE: the cache is cleared at the end of the search sequence (when
      // the search box loses focus)
      if (resultCache_.size() > 25)
         resultCache_.remove(0);
      resultCache_.add(
        new Pair<Request, ArrayList<CodeSearchSuggestion>>(request, 
                                                           suggestions));
      
      // callback
      callback.onSuggestionsReady(request, 
                                  new Response(suggestions)) ;
   }
   
   private final CodeSearchServerOperations server_ ;
   
   private String lastQuery_ = null;
   private ArrayList<CodeSearchSuggestion> lastSuggestions_ = null;
   private ArrayList<Pair<Request,ArrayList<CodeSearchSuggestion>>> 
      resultCache_ = new ArrayList<Pair<Request,ArrayList<CodeSearchSuggestion>>>();
}

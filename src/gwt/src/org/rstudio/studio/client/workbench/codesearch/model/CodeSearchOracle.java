package org.rstudio.studio.client.workbench.codesearch.model;

import java.util.ArrayList;

import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.studio.client.common.SimpleRequestCallback;

import com.google.gwt.core.client.JsArray;
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
      // invalidate
      invalidation_.invalidate();
      
      // first see if we can serve the request from the cache
      for (int i=resultCache_.size() - 1; i >= 0; i--)
      {
         // get the previous result
         Pair<String,ArrayList<CodeSearchSuggestion>> res = resultCache_.get(i);
         
         // exact match of previous query
         if (request.getQuery().equals(res.first))
         {
            callback.onSuggestionsReady(request, new Response(res.second));
            return;
         }
         
         // if this query is a further refinement of a non-overflowed 
         // previous query then satisfy it by filtering the previous results
         if (res.second.size() <= request.getLimit() &&
             request.getQuery().startsWith(res.first))
         {
           
            // TODO: once we introduce pattern matching then we need to
            // reflect this in this codepath
            
            String queryLower = request.getQuery().toLowerCase();
           
            ArrayList<CodeSearchSuggestion> suggestions =
                                       new ArrayList<CodeSearchSuggestion>();
            for (int s=0; s<res.second.size(); s++)
            {
               
               CodeSearchSuggestion sugg = res.second.get(s);
               
               String functionName = sugg.getResult().getFunctionName();
              
               if (functionName.toLowerCase().startsWith(queryLower))
                  suggestions.add(sugg);
            }

            // cache suggestions. note that this adds an item to the end
            // of the resultCache_ (which we are currently iterating over)
            // not a big deal because we are about to return out of the loop
            cacheSuggestions(request, suggestions);
            
            // return suggestions
            callback.onSuggestionsReady(request, new Response(suggestions));
            
            return;
         } 
      }
      
      // failed to short-circuit via the cache, hit the server
      codeSearch_.enqueRequest(request, callback); 
   }
     
   public CodeSearchResult resultFromSuggestion(Suggestion suggestion)
   {
      return ((CodeSearchSuggestion)suggestion).getResult();
   }
   
   public void clear()
   {
      resultCache_.clear();
      invalidation_.invalidate();
   }
   
   @Override
   public boolean isDisplayStringHTML()
   {
      return true;
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
         token_ = invalidation_.getInvalidationToken();
         nudge();
      }

      @Override
      protected void performAction(boolean shouldSchedulePassive)
      {
         // failed to short-circuit via the cache, hit the server
         server_.searchCode(
               request_.getQuery(),
               request_.getLimit(),
               new SimpleRequestCallback<JsArray<CodeSearchResult>>() {
            
            @Override
            public void onResponseReceived(JsArray<CodeSearchResult> results)
            { 
               // read the response
               ArrayList<CodeSearchSuggestion> suggestions = 
                                       new ArrayList<CodeSearchSuggestion>();
               for (int i = 0; i<results.length(); i++) 
                  suggestions.add(new CodeSearchSuggestion(results.get(i)));     
               
               // cache suggestions
               cacheSuggestions(request_, suggestions);
               
               // return suggestions
               if (!token_.isInvalid())
               {
                  callback_.onSuggestionsReady(request_, 
                                               new Response(suggestions));
               }
            }
         });
         
      }
      
      private Invalidation.Token token_;
      private Request request_;
      private Callback callback_;
   };
   
   
   private void cacheSuggestions(
                     final Request request, 
                     final ArrayList<CodeSearchSuggestion> suggestions)
   {
      // cache the suggestions (up to 15 active result sets cached)
      // NOTE: the cache is cleared at the end of the search sequence 
      // (when the search box loses focus)
      if (resultCache_.size() > 15)
         resultCache_.remove(0);
      
      resultCache_.add(new Pair<String, ArrayList<CodeSearchSuggestion>>(
                                         request.getQuery(), 
                                         suggestions));
   }
   
  
   private final CodeSearchServerOperations server_ ;
   
   private CodeSearchCommand codeSearch_ = new CodeSearchCommand();
   
   private Invalidation invalidation_ = new Invalidation();
   
   private ArrayList<Pair<String, ArrayList<CodeSearchSuggestion>>> 
      resultCache_ = new ArrayList<Pair<String,ArrayList<CodeSearchSuggestion>>>();
}

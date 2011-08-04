package org.rstudio.studio.client.workbench.codesearch;

import java.util.ArrayList;

import org.rstudio.core.client.Pair;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.common.SimpleRequestCallback;
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
      // record the last request
      lastRequest_ = request;
      
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
            Pattern pattern = null;
            String queryLower = request.getQuery().toLowerCase();
            if (queryLower.indexOf('*') != -1)
               pattern = patternForTerm(queryLower);
            
            ArrayList<CodeSearchSuggestion> suggestions =
                                       new ArrayList<CodeSearchSuggestion>();
            for (int s=0; s<res.second.size(); s++)
            {
               CodeSearchSuggestion sugg = res.second.get(s);
               
               String name = sugg.getSourceItem().getFunctionName().toLowerCase();
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
            cacheSuggestions(request, suggestions);
            
            // return suggestions
            callback.onSuggestionsReady(request, new Response(suggestions));
            
            return;
         } 
      }
      
      // failed to short-circuit via the cache, hit the server
      codeSearch_.enqueRequest(request, callback); 
   }
     
   public RSourceItem sourceItemFromSuggestion(Suggestion suggestion)
   {
      return ((CodeSearchSuggestion)suggestion).getSourceItem();
   }
   
   public void clear()
   {
      resultCache_.clear();
      lastRequest_ = null;
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
         nudge();
      }

      @Override
      protected void performAction(boolean shouldSchedulePassive)
      {
         // failed to short-circuit via the cache, hit the server
         server_.searchCode(
               request_.getQuery(),
               request_.getLimit(),
               new SimpleRequestCallback<RpcObjectList<RSourceItem>>() {
            
            @Override
            public void onResponseReceived(
                                    RpcObjectList<RSourceItem> response)
            { 
               // to array
               ArrayList<RSourceItem> results = response.toArrayList();
               
               // read the response
               ArrayList<CodeSearchSuggestion> suggestions = 
                                       new ArrayList<CodeSearchSuggestion>();
               for (int i = 0; i<results.size(); i++) 
                  suggestions.add(new CodeSearchSuggestion(results.get(i)));     
               
               // cache suggestions
               cacheSuggestions(request_, suggestions);
               
               // return suggestions if this request is still active
               if (request_ == lastRequest_)
               {
                  callback_.onSuggestionsReady(request_, 
                                               new Response(suggestions));
               }
            }
         });
         
      }
      
      private Request request_;
      private Callback callback_;
   };
   
   
   private void cacheSuggestions(
                     final Request request, 
                     final ArrayList<CodeSearchSuggestion> suggestions)
   {
      // cache the suggestions (up to 15 active result sets cached)
      // NOTE: the cache is cleared on gain focus, lost focus, and 
      // the search term reverting back to empty)
      if (resultCache_.size() > 15)
         resultCache_.remove(0);
      
      resultCache_.add(new Pair<String, ArrayList<CodeSearchSuggestion>>(
                                         request.getQuery(), 
                                         suggestions));
   }
   
  
   private final CodeSearchServerOperations server_ ;
   
   private CodeSearchCommand codeSearch_ = new CodeSearchCommand();
   
   private Request lastRequest_ = null;
     
   private ArrayList<Pair<String, ArrayList<CodeSearchSuggestion>>> 
      resultCache_ = new ArrayList<Pair<String,ArrayList<CodeSearchSuggestion>>>();
}

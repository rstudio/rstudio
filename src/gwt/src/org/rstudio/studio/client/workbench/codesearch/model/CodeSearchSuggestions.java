package org.rstudio.studio.client.workbench.codesearch.model;

import java.util.ArrayList;

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
      // if this query is a further refinement of a previous query then
      // satisfy the request by simply filtering the previous results
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
         callback.onSuggestionsReady(request, new Response(suggestions));
      }
      // if all previous suggestions are prefaced by this query and the 
      // last query overflowed the limit then the new query does nothing to 
      // further disambiguate results
      else if (allPreviousSuggestionsPrefacedBy(request.getQuery()) &&
               lastSuggestions_.size() >= request.getLimit())
      {
         callback.onSuggestionsReady(request, new Response(lastSuggestions_));
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
               callback.onSuggestionsReady(request, 
                                           new Response(suggestions)) ;
            }
         });
      }
   }
   
     
   public CodeSearchResult resultFromSuggestion(Suggestion suggestion)
   {
      return ((CodeSearchSuggestion)suggestion).getResult();
   }
   
   private boolean allPreviousSuggestionsPrefacedBy(String query)
   {
      if (lastSuggestions_ == null)
         return false;
      
      for (int i=0; i<lastSuggestions_.size(); i++)
      {
         CodeSearchSuggestion sugg = lastSuggestions_.get(i);
         String functionName = sugg.getResult().getFunctionName();
         
         if (!functionName.toLowerCase().startsWith(query))
            return false;
      }
      
      return true;
   }
   
   private final CodeSearchServerOperations server_ ;
   
   private String lastQuery_ = null;
   private ArrayList<CodeSearchSuggestion> lastSuggestions_ = null;
}

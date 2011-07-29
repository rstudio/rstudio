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
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.inject.Inject;

import org.rstudio.core.client.SafeHtmlUtil;
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
      // TODO: see if we can defer the query until 2 characters (unless
      // the user has only one character for more than some time period)
      
      // if this query is a further refinement of a previous query then
      // filter those results manually
      if (lastQuery_ != null && request.getQuery().startsWith(lastQuery_))
      { 
         ArrayList<SearchSuggestion> suggestions = 
                                          new ArrayList<SearchSuggestion>();
         for (int i=0; i<lastSuggestions_.size(); i++)
         {
            SearchSuggestion sugg = lastSuggestions_.get(i);
            String functionName = sugg.getResult().getFunctionName();
            
            if (functionName.startsWith(request.getQuery()))
               suggestions.add(sugg);
         }
         
         // yield the suggestions
         callback.onSuggestionsReady(request, new Response(suggestions));
      }
      else
      {  
         server_.searchCode(
               request.getQuery(),
               new SimpleRequestCallback<JsArray<CodeSearchResult>>() {
            
            @Override
            public void onResponseReceived(JsArray<CodeSearchResult> results)
            {
               // TODO: do this on the server (in doing so returning max+1
               // indicates more values so can't cache)
               int maxCount = Math.min(results.length(), request.getLimit());
   
               // read the response
               ArrayList<SearchSuggestion> suggestions = 
                                          new ArrayList<SearchSuggestion>();
               for (int i = 0; i< maxCount; i++) 
                  suggestions.add(new SearchSuggestion(results.get(i)));     
               
               // if we got less than the max results then we can safely
               // cache this for further filtering
               if (results.length() < request.getLimit())
               {
                  // save the query
                  lastQuery_ = request.getQuery();
                  
                  // save the suggestions
                  lastSuggestions_ = suggestions;
               }
               else
               {
                  lastQuery_ = null;
                  lastSuggestions_ = null;
               }
               
            
               
               // yield suggestions
               callback.onSuggestionsReady(request, 
                                           new Response(suggestions)) ;
            }
         });
      }
   }
   
   @Override
   public boolean isDisplayStringHTML()
   {
      return true;
   }
   
   public CodeSearchResult resultFromSuggestion(Suggestion suggestion)
   {
      return ((SearchSuggestion)suggestion).getResult();
   }
   
   private class SearchSuggestion implements Suggestion
   {
      public SearchSuggestion(CodeSearchResult result)
      {
         result_ = result;
      }

      public String getDisplayString()
      {
         CodeSearchResources res = CodeSearchResources.INSTANCE;
         CodeSearchResources.Styles styles = res.styles();
         
         SafeHtmlBuilder sb = new SafeHtmlBuilder();
         appendImage(sb, res.function(), styles.functionImage());
         appendSpan(sb, result_.getFunctionName(), styles.functionName());                   
         appendSpan(sb, "(" + result_.getContext() + ")", styles.functionContext());
         return sb.toSafeHtml().asString();
      }

      public String getReplacementString()
      {
         return lastQuery_ ;
      }
      
      public CodeSearchResult getResult()
      {
         return result_;
      }
      
      private final CodeSearchResult result_ ;
   }
   
   private void appendSpan(SafeHtmlBuilder sb, 
                           String content,
                           String style)
   {
      sb.append(SafeHtmlUtil.createOpenTag("span","class", style));
      sb.appendEscaped(content);
      sb.appendHtmlConstant("</span>");   
   }
   
   private void appendImage(SafeHtmlBuilder sb, ImageResource image, String style)
   {
      sb.append(SafeHtmlUtil.createOpenTag("img","class", style,
                                                 "src", image.getURL()));
      sb.appendHtmlConstant("</img>");   
   }

   

   private final CodeSearchServerOperations server_ ;
   
   private String lastQuery_ = null;
   private ArrayList<SearchSuggestion> lastSuggestions_ = null;
}

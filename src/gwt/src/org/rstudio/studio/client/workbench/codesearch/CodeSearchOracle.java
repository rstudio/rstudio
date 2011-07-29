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
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
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
      lastSuggestions_ = new ArrayList<SearchSuggestion>() ;
   }

   @Override
   public void requestSuggestions(final Request request, 
                                  final Callback callback)
   {
      final CodeSearchResources.Styles styles = 
         CodeSearchResources.INSTANCE.styles();
      
      String query = request.getQuery() ;
      
      lastQuery_ = query;
      
      server_.searchCode(
            query,
            new SimpleRequestCallback<JsArray<CodeSearchResult>>() {
         
         @Override
         public void onResponseReceived(JsArray<CodeSearchResult> results)
         {
            int maxCount = Math.min(results.length(), request.getLimit());

          
            lastSuggestions_.clear();
            for (int i = 0; i< maxCount; i++)
            {    
               lastSuggestions_.add(new SearchSuggestion(results.get(i)));     
            }
            
            
            
            callback.onSuggestionsReady(request, new Response(lastSuggestions_)) ;
         }
      }); ;
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
   
   private String lastQuery_;
   private final ArrayList<SearchSuggestion> lastSuggestions_;
}

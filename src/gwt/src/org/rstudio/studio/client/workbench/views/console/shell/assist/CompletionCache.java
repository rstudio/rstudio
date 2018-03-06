/*
 * CompletionCache.java
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

import org.rstudio.core.client.JsVectorBoolean;
import org.rstudio.core.client.JsVectorInteger;
import org.rstudio.core.client.JsVectorString;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.container.SafeMap;
import org.rstudio.studio.client.common.codetools.Completions;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JsArrayBoolean;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;

// This class should be used to cache completions during an active completion
// session. For example, if one retrieves completions for the token 'rn',
// and the user types 'rnorm', the completion cache should be able to satisfy
// the intermediate completion requests for 'rno', 'rnor', and 'rnorm'.
public class CompletionCache
{
   public CompletionCache()
   {
      cache_ = new SafeMap<String, Completions>();
   }
   
   public boolean satisfyRequest(String token,
                                 ServerRequestCallback<Completions> requestCallback)
   {
      if (StringUtil.isNullOrEmpty(token))
         return false;
      
      for (int i = 0, n = token.length(); i < n; i++)
      {
         String subToken = token.substring(0, n - i);
         if (cache_.containsKey(subToken))
         {
            Completions completions = narrow(token, cache_.get(subToken));
            requestCallback.onResponseReceived(completions);
            return true;
         }
      }
      
      return false;
   }
   
   public void store(String token, Completions completions)
   {
      cache_.put(token, completions);
   }
   
   public void flush()
   {
      cache_.clear();
   }
   
   private Completions narrow(String token, Completions original)
   {
      // Extract the vector elements of the completion string
      JsArrayString completions = original.getCompletions();
      JsArrayString packages    = original.getPackages();
      JsArrayBoolean quote      = original.getQuote();
      JsArrayInteger type       = original.getType();
      
      // Now, generate narrowed versions of the above
      JsVectorString completionsNarrow = JsVectorString.createVector().cast();
      JsVectorString packagesNarrow    = JsVectorString.createVector().cast();
      JsVectorBoolean quoteNarrow      = JsVectorBoolean.createVector().cast();
      JsVectorInteger typeNarrow       = JsVectorInteger.createVector().cast();
      
      for (int i = 0, n = completions.length(); i < n; i++)
      {
         boolean isSubsequence = StringUtil.isSubsequence(completions.get(i), token);
         if (isSubsequence)
         {
            completionsNarrow.push(completions.get(i));
            packagesNarrow.push(packages.get(i));
            quoteNarrow.push(quote.get(i));
            typeNarrow.push(type.get(i));
         }
      }
      
      // And return the completion result
      return Completions.createCompletions(
            token,
            completionsNarrow.cast(),
            packagesNarrow.cast(),
            quoteNarrow.cast(),
            typeNarrow.cast(),
            original.getGuessedFunctionName(),
            original.getExcludeOtherCompletions(),
            original.getOverrideInsertParens(),
            original.isCacheable(),
            original.getHelpHandler(),
            original.getLanguage());
   }
   
   private final SafeMap<String, Completions> cache_;
}

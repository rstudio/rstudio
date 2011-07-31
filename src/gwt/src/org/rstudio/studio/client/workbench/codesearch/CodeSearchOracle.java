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

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.inject.Inject;

import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchResult;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchSuggestions;


public class CodeSearchOracle extends SuggestOracle
{
   @Inject
   public CodeSearchOracle(CodeSearchSuggestions suggestions)
   {
      suggestions_ = suggestions;
   }

   @Override
   public void requestSuggestions(final Request request, 
                                  final Callback callback)
   { 
      // TODO: see if we can defer the query until 2 characters (unless
      // the user has only one character for more than some time period)
      
      suggestions_.request(request, callback);
  
      
     
    
      
   }
   
   @Override
   public boolean isDisplayStringHTML()
   {
      return true;
   }
   
   public CodeSearchResult resultFromSuggestion(Suggestion suggestion)
   {
      return suggestions_.resultFromSuggestion(suggestion);
   }
   
   private CodeSearchSuggestions suggestions_;
   
  
}

/*
 * SpellingService.java
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

package org.rstudio.studio.client.common.spelling;

import java.util.HashMap;

import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SpellingService
{
   @Inject
   public SpellingService(SpellingServerOperations server,
                          UIPrefs uiPrefs)
   {
      server_ = server;
      
      uiPrefs.spellingDictionaryLanguage().addValueChangeHandler(
                                           new ValueChangeHandler<String>(){
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            previousResults_.clear();
         }
      });
      
      uiPrefs.spellingCustomDictionaries().addValueChangeHandler(
                                    new ValueChangeHandler<JsArrayString>() {
         @Override
         public void onValueChange(ValueChangeEvent<JsArrayString> event)
         {
            previousResults_.clear();
         }
      });
   }

   public void checkSpelling(final String word, 
                             final ServerRequestCallback<Boolean> callback)
   {
      // check the cache
      Boolean correct = previousResults_.get(word);
      if (correct != null)
      {
         callback.onResponseReceived(correct);
         return;
      }
      
      // hit the server
      server_.checkSpelling(word, new ServerRequestCallback<Boolean>() {

         @Override
         public void onResponseReceived(Boolean correct)
         {
            previousResults_.put(word, correct);
            callback.onResponseReceived(correct);
         }
         
         @Override
         public void onError(ServerError error)
         {
            callback.onError(error);
         }
         
      });
   }

   public void suggestionList(String word,
                              ServerRequestCallback<JsArrayString> callback)
   {
      server_.suggestionList(word, callback);
   }

   private final SpellingServerOperations server_;
   
   private HashMap<String,Boolean> previousResults_ = 
                                             new HashMap<String,Boolean>();
   
}

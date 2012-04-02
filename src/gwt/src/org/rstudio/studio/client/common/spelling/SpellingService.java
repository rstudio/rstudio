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
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SpellingService implements HasChangeHandlers
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
            onSpellingDictionariesChanged();
         }
      });
      
      uiPrefs.spellingCustomDictionaries().addValueChangeHandler(
                                    new ValueChangeHandler<JsArrayString>() {
         @Override
         public void onValueChange(ValueChangeEvent<JsArrayString> event)
         {
            onSpellingDictionariesChanged();
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
   
   @Override
   public HandlerRegistration addChangeHandler(ChangeHandler handler)
   {
      return handlerManager_.addHandler(ChangeEvent.getType(), handler);    
  }

   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlerManager_.fireEvent(event);
   }
   
   private void onSpellingDictionariesChanged()
   {
      previousResults_.clear();
      DomEvent.fireNativeEvent(Document.get().createChangeEvent(),
                               handlerManager_);
   }
   
   private final SpellingServerOperations server_;
   
   private HashMap<String,Boolean> previousResults_ = 
                                             new HashMap<String,Boolean>();
   
   HandlerManager handlerManager_ = new HandlerManager(this);
   
}

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.common.spelling.model.SpellCheckerResult;
import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.core.client.JsArrayInteger;
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

   public void checkSpelling(
                     List<String> words, 
                     final ServerRequestCallback<SpellCheckerResult> callback)
   {
      // results to return
      final SpellCheckerResult spellCheckerResult = new SpellCheckerResult();
      
      // only send words to the server that aren't in the cache
      final ArrayList<String> wordsToCheck = new ArrayList<String>();
      for (int i = 0; i<words.size(); i++)
      {
         String word = words.get(i);
         Boolean isCorrect = previousResults_.get(word);
         if (isCorrect != null)
         {
            if (isCorrect)
               spellCheckerResult.getCorrect().add(word);
            else
               spellCheckerResult.getIncorrect().add(word);
         }
         else
         {
            wordsToCheck.add(word);
         }
      }
      
      // if there are no words to check then return
      if (wordsToCheck.size() == 0)
      {
         callback.onResponseReceived(spellCheckerResult);
         return;
      }
      
      // hit the server
      server_.checkSpelling(JsUtil.toJsArrayString(wordsToCheck), 
                            new ServerRequestCallback<JsArrayInteger>() {

         @Override
         public void onResponseReceived(JsArrayInteger result)
         {
            // get misspelled indexes
            ArrayList<Integer> misspelledIndexes = new ArrayList<Integer>();
            for (int i=0; i<result.length(); i++)
               misspelledIndexes.add(result.get(i));
            
            // determine correct/incorrect status and populate result & cache
            for (int i=0; i<wordsToCheck.size(); i++)
            {
               String word = wordsToCheck.get(i);
               if (misspelledIndexes.contains(i))
               {
                  spellCheckerResult.getIncorrect().add(word);
                  previousResults_.put(word, false);
               }
               else
               {
                  spellCheckerResult.getCorrect().add(word);
                  previousResults_.put(word,  true);
               }
            }
            
            // return result
            callback.onResponseReceived(spellCheckerResult);     
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

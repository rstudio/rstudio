/*
 * SpellingService.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.spelling.model.SpellCheckerResult;
import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.model.PrefLayer;
import org.rstudio.studio.client.workbench.prefs.model.SpellingPrefsContext;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

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
                          UserPrefs uiPrefs,
                          EventBus eventBus)
   {
      server_ = server;
      uiPrefs_ = uiPrefs;
      dictionaryLanguage_ = uiPrefs_.spellingDictionaryLanguage().getValue();
      
      // Local preference values can change before the server has switched
      // dictionaries. Recheck only once the preference write is confirmed.
      eventBus.addHandler(UserPrefsChangedEvent.TYPE, (event) ->
      {
         // A complete saved layer can also remove the language preference.
         if (event.isFullLayer() ||
             event.getValues().hasKey(uiPrefs_.spellingDictionaryLanguage().getId()))
            onDictionaryLanguageChanged();
      });

      // Project Options also removes the project value when the global default
      // is selected, so its confirmed change may not contain a language key.
      eventBus.addHandler(ProjectOptionsChangedEvent.TYPE, (event) -> onDictionaryLanguageChanged());
      
      uiPrefs.spellingCustomDictionaries().addValueChangeHandler(
                                    new ValueChangeHandler<JsArrayString>() {
         @Override
         public void onValueChange(ValueChangeEvent<JsArrayString> event)
         {
            invalidateCache();
         }
      });
   }

   public void checkSpelling(
                     List<String> words, 
                     final ServerRequestCallback<SpellCheckerResult> callback)
   {
      final int dictionaryGeneration = dictionaryGeneration_;

      // results to return
      final SpellCheckerResult spellCheckerResult = new SpellCheckerResult();
      
      // only send words to the server that aren't in the cache
      final ArrayList<String> wordsToCheck = new ArrayList<>();
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
            // Retry against the current dictionaries before caching or returning
            // a response requested before the dictionaries changed.
            if (dictionaryGeneration != dictionaryGeneration_)
            {
               checkSpelling(words, callback);
               return;
            }

            // get misspelled indexes
            ArrayList<Integer> misspelledIndexes = new ArrayList<>();
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
            if (dictionaryGeneration != dictionaryGeneration_)
            {
               checkSpelling(words, callback);
               return;
            }

            callback.onError(error);
         }
      });
   }

   public void suggestionList(String word,
                              ServerRequestCallback<JsArrayString> callback)
   {
      final int dictionaryGeneration = dictionaryGeneration_;
      server_.suggestionList(word, new ServerRequestCallback<JsArrayString>()
      {
         @Override
         public void onResponseReceived(JsArrayString response)
         {
            if (dictionaryGeneration != dictionaryGeneration_)
            {
               suggestionList(word, callback);
               return;
            }

            callback.onResponseReceived(response);
         }

         @Override
         public void onError(ServerError error)
         {
            if (dictionaryGeneration != dictionaryGeneration_)
            {
               suggestionList(word, callback);
               return;
            }

            callback.onError(error);
         }
      });
   }
   
   public void addCustomDictionary(
                         String dictPath,
                         final ServerRequestCallback<JsArrayString> callback)
   {
      server_.addCustomDictionary(dictPath, new CustomDictCallback(callback));
   }

   public void removeCustomDictionary(
                              String name,
                              ServerRequestCallback<JsArrayString> callback)
   {
      server_.removeCustomDictionary(name,  new CustomDictCallback(callback));
   }
   
   public void installAllDictionaries(
                  ServerRequestCallback<SpellingPrefsContext> requestCallback)
   {
      server_.installAllDictionaries(new ServerRequestCallback<SpellingPrefsContext>()
      {
         @Override
         public void onResponseReceived(SpellingPrefsContext context)
         {
            // Newly opened language pickers read this shared context.
            uiPrefs_.spellingPrefsContext().setValue(PrefLayer.LAYER_COMPUTED, context);
            requestCallback.onResponseReceived(context);
         }

         @Override
         public void onError(ServerError error)
         {
            requestCallback.onError(error);
         }
      });
   }
   
   public void invalidateCache()
   {
      dictionaryGeneration_++;
      previousResults_.clear();
      DomEvent.fireNativeEvent(Document.get().createChangeEvent(),
                               handlerManager_);
   }

   public void onDictionaryLanguageChanged()
   {
      String language = uiPrefs_.spellingDictionaryLanguage().getValue();
      if (!StringUtil.equals(dictionaryLanguage_, language))
      {
         dictionaryLanguage_ = language;
         invalidateCache();
      }
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
   
   private class CustomDictCallback extends ServerRequestCallback<JsArrayString>
   {
      public CustomDictCallback(ServerRequestCallback<JsArrayString> callback)
      {
         clientCallback_ = callback;
      }
      
      @Override
      public void onResponseReceived(JsArrayString customDicts)
      {
         // the underlying spelling dictionaries have changed so we need
         // to update the ui-pref -- this will result in an invalidation
         // of our results cache. note that if for some reason the user
         // does not press the OK or Apply button in the dialog then cross
         // process notification of the new custom dictionary state won't 
         // occur and other IDE instances will have invalid dictionary
         // results caches until they are restarted.
         uiPrefs_.spellingCustomDictionaries().setGlobalValue(customDicts);
         
         // pass through to the caller
         clientCallback_.onResponseReceived(customDicts);
      }
      
      @Override
      public void onError(ServerError error)
      {
         clientCallback_.onError(error);
      }
     
      private final ServerRequestCallback<JsArrayString> clientCallback_;
   }
         
   
   private final SpellingServerOperations server_;
   private final UserPrefs uiPrefs_;
   
   private HashMap<String,Boolean> previousResults_ = new HashMap<>();
   private int dictionaryGeneration_;
   private String dictionaryLanguage_;
   
   HandlerManager handlerManager_ = new HandlerManager(this);
   
}

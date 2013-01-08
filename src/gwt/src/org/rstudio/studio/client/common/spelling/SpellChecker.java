/*
 * SpellChecker.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.common.spelling;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.spelling.model.SpellCheckerResult;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchList;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.ListChangedHandler;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SpellChecker
{ 
   public interface Context
   {  
      ArrayList<String> readDictionary();
      void writeDictionary(ArrayList<String> words);
      
      void invalidateAllWords();
      void invalidateMisspelledWords();
      
      void releaseOnDismiss(HandlerRegistration handler);
   }

   public SpellChecker(Context context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // save reference to context and read its dictionary
      context_ = context;
      contextDictionary_ = context_.readDictionary();
          
              
      // subscribe to spelling service changes (these occur when when the
      // spelling dictionaries are changed)
      context_.releaseOnDismiss(spellingService_.addChangeHandler(
                                                   new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            context_.invalidateAllWords();
         }
      }));
      
      // subscribe to spelling prefs changes (invalidateAll on changes)
      uiPrefs_.ignoreWordsInUppercase().addValueChangeHandler(
                                                         prefChangedHandler_);
      uiPrefs_.ignoreWordsWithNumbers().addValueChangeHandler(
                                                         prefChangedHandler_);
      
      // subscribe to user dictionary changes
      context_.releaseOnDismiss(userDictionary_.addListChangedHandler(
                                                   new ListChangedHandler() {
         @Override
         public void onListChanged(ListChangedEvent event)
         {
            // detect whether this is the first delivery of the list
            // or if it is an update
            boolean isUpdate = userDictionaryWords_ != null;
                 
            userDictionaryWords_ = event.getList();
            
            updateIgnoredWordsIndex();
            
            if (isUpdate)
               context_.invalidateMisspelledWords(); 
         }
      }));
   }
   
   @Inject
   void intialize(SpellingService spellingService,
                  WorkbenchListManager workbenchListManager,
                  UIPrefs uiPrefs)
   {
      spellingService_ = spellingService;
      userDictionary_ = workbenchListManager.getUserDictionaryList();
      uiPrefs_ = uiPrefs;
   }
   
   public void checkSpelling(
                  List<String> words, 
                  final ServerRequestCallback<SpellCheckerResult> callback)
   {
      // allocate results
      final SpellCheckerResult spellCheckerResult = new SpellCheckerResult();

      if (words.isEmpty())
      {
         callback.onResponseReceived(spellCheckerResult);
         return;
      }
      
      // only send words to the server that aren't ignored
      final ArrayList<String> wordsToCheck = new ArrayList<String>();
      for (int i = 0; i<words.size(); i++)
      {
         String word = words.get(i);
         if (isWordIgnored(word))
            spellCheckerResult.getCorrect().add(word);
         else
            wordsToCheck.add(word);
      }
      
      // call the service to check the non-ignored words
      spellingService_.checkSpelling(
         wordsToCheck,
         new ServerRequestCallback<SpellCheckerResult>() {

            @Override
            public void onResponseReceived(SpellCheckerResult result)
            {
               spellCheckerResult.getCorrect().addAll(result.getCorrect());
               spellCheckerResult.getIncorrect().addAll(result.getIncorrect());
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
      spellingService_.suggestionList(word, callback);
   }
   
   public void addToUserDictionary(final String word)
   {
      userDictionary_.append(word);  
   }
   
   public void addIgnoredWord(String word)
   {
      contextDictionary_.add(word);
      context_.writeDictionary(contextDictionary_);
      
      updateIgnoredWordsIndex();

      context_.invalidateMisspelledWords();
   }
  
   private boolean isWordIgnored(String word)
   {
      if (allIgnoredWords_.contains(word))
         return true;
      else if (ignoreUppercaseWord(word))
         return true;
      else if (ignoreWordWithNumbers(word))
         return true;
      else
         return false;
   }
   
   private boolean ignoreUppercaseWord(String word)
   {
      if (!uiPrefs_.ignoreWordsInUppercase().getValue())
         return false;
     
      for (char c: word.toCharArray())
      {
         if(!Character.isUpperCase(c))
            return false;
      }
      return true;
   }
   
   private boolean ignoreWordWithNumbers(String word)
   {
      if (!uiPrefs_.ignoreWordsWithNumbers().getValue())
         return false;
      
      for (char c: word.toCharArray())
      {
         if(Character.isDigit(c))
            return true;
      }
      return false;
   }
  
   private void updateIgnoredWordsIndex()
   {
      allIgnoredWords_.clear();
      allIgnoredWords_.addAll(userDictionaryWords_);
      allIgnoredWords_.addAll(contextDictionary_);
   }
    
   private ValueChangeHandler<Boolean> prefChangedHandler_ = 
                                       new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event)
      {
         context_.invalidateAllWords();
      }  
   };

   private final Context context_;
   
   private WorkbenchList userDictionary_;
   private ArrayList<String> userDictionaryWords_;
   private ArrayList<String> contextDictionary_ = new ArrayList<String>();
   private final HashSet<String> allIgnoredWords_ = new HashSet<String>(); 
   
   private SpellingService spellingService_;
   private UIPrefs uiPrefs_;
}

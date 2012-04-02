/*
 * SpellChecker.java
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
import java.util.HashSet;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchList;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.ListChangedHandler;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;

public class SpellChecker
{ 
   public interface Context
   {  
      ArrayList<String> readDictionary();
      void writeDictionary(ArrayList<String> words);
      
      void releaseOnDismiss(HandlerRegistration handler);
   }

   public SpellChecker(Context context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // save reference to context and read its dictionary
      context_ = context;
      contextDictionary_ = context_.readDictionary();
          
      // subscribe to user dictionary changes
      context_.releaseOnDismiss(userDictionary_.addListChangedHandler(
                                                   new ListChangedHandler() {
         @Override
         public void onListChanged(ListChangedEvent event)
         {
            userDictionaryWords_ = event.getList();
            updateIgnoredWordsIndex();
         }
      }));
   }
   
   @Inject
   void intialize(SpellingService spellingService,
                  WorkbenchListManager workbenchListManager)
   {
      spellingService_ = spellingService;
      userDictionary_ = workbenchListManager.getUserDictionaryList();
   }
   
   public void checkSpelling(String word, 
                             ServerRequestCallback<Boolean> callback)
   {
      // TODO: filter on uppercase, words with numbers, etc. note  that when
      // those prefs change we need to fire an event to the container 
      // indicating to invalidate
      
      // TODO: when spelling service dictionary changes it invalidates its
      // cache. we also need to propagate this to the container indicating
      // to invalidate
      
      if (isWordIgnored(word))
         callback.onResponseReceived(true);
      
      spellingService_.checkSpelling(word, callback);
   }
   
   public void suggestionList(String word,
                              ServerRequestCallback<JsArrayString> callback)
   {
      spellingService_.suggestionList(word, callback);
   }
   
   public void addToUserDictionary(String word)
   {
      // NOTE: we don't fire the change event dicectly b/c it will occur later 
      // within the onListChannged handler 
      userDictionary_.append(word);
   }
   
   public void addIgnoredWord(String word)
   {
      contextDictionary_.add(word);
      
      context_.writeDictionary(contextDictionary_);
      
      updateIgnoredWordsIndex();
   }
  
   private boolean isWordIgnored(String word)
   {
      return allIgnoredWords_.contains(word);
   }
   
   
   private void updateIgnoredWordsIndex()
   {
      allIgnoredWords_.clear();
      allIgnoredWords_.addAll(userDictionaryWords_);
      allIgnoredWords_.addAll(contextDictionary_);
      
      // TODO: fire event to container notifying it that rescanning
      // may be necessary
   }
   
   private final Context context_;
   
   private WorkbenchList userDictionary_;
   private ArrayList<String> userDictionaryWords_;
   private ArrayList<String> contextDictionary_ = new ArrayList<String>();
   private final HashSet<String> allIgnoredWords_ = new HashSet<String>(); 
   
   private SpellingService spellingService_;
}

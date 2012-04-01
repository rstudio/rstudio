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
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.ListChangedHandler;

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;

public class SpellChecker
{ 
   public SpellChecker(SpellingDocument spellingDocument,
                       WorkbenchList userDictionary)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // save reference to document
      spellingDocument_ = spellingDocument;
      
      // get ignored words for this document
      docDictionary_ = spellingDocument.readDictionary();
      
      // TODO: need to unsubscribe from this event when we 
      // are unloaded (also for uiprefs events)
      
      // get global user dictionary (and subscribe to changes)
      userDictionary_ = userDictionary;
      userDictionary_.addListChangedHandler(new ListChangedHandler() {
         @Override
         public void onListChanged(ListChangedEvent event)
         {
            userDictionaryWords_ = event.getList();
            updateIgnoredWordsIndex();
         }
      });
   }
   
   @Inject
   void intialize(SpellingService spellingService)
   {
      spellingService_ = spellingService;
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
      // NOTE: we don't fire the change event because this will occur later 
      // within the onListChannged handler 
      userDictionary_.append(word);
   }
   
   public void addIgnoredWord(String word)
   {
      docDictionary_.add(word);
      
      spellingDocument_.writeDictionary(docDictionary_);
      
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
      allIgnoredWords_.addAll(docDictionary_);
      
      // TODO: fire event to container notifying it that rescanning
      // may be necessary
   }
   
   private final SpellingDocument spellingDocument_;
   
   private final WorkbenchList userDictionary_;
   private ArrayList<String> userDictionaryWords_;
   private ArrayList<String> docDictionary_ = new ArrayList<String>();
   private final HashSet<String> allIgnoredWords_ = new HashSet<String>(); 
   
   private SpellingService spellingService_;
}

/*
 * SpellingIgnoredWords.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.rstudio.core.client.CsvReader;
import org.rstudio.core.client.CsvWriter;
import org.rstudio.core.client.widget.NullProgressIndicator;
import org.rstudio.studio.client.workbench.WorkbenchList;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.ListChangedHandler;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

public class SpellingIgnoredWords implements HasChangeHandlers
{
   public SpellingIgnoredWords(DocUpdateSentinel docUpdateSentinel,
                               WorkbenchList userDictionary)
   {
      // save reference to doc update sentinel
      docUpdateSentinel_ = docUpdateSentinel;
      
      // get ignored words for this document from doc update sentinel
      readIgnoredWords();
      
      // get global user dictionary (and subscribe to changes)
      userDictionary_ = userDictionary;
      userDictionary_.addListChangedHandler(new ListChangedHandler() {
         @Override
         public void onListChanged(ListChangedEvent event)
         {
            userDictionaryWords_ = event.getList();
            updateIndex();
         }
      });
   }
   
   @Override
   public HandlerRegistration addChangeHandler(ChangeHandler handler)
   {
      return handlerManager_.addHandler(ChangeEvent.getType(), handler);
   }
   
   public boolean isWordIgnored(String word)
   {
      return allIgnoredWords_.contains(word);
   }
   
   public void addToUserDictionary(String word)
   {
      // NOTE: we don't fire the change event because this will occur later 
      // within the onListChannged handler 
      userDictionary_.append(word);
   }
   
   public void addIgnoredWord(String word)
   {
      ignoredWords_.add(word);
      
      writeIgnoredWords();
      
      updateIndex();
   }
   
   private void readIgnoredWords()
   {
      ignoredWords_.clear();
      String ignored = docUpdateSentinel_.getProperty(IGNORED_WORDS);
      if (ignored != null)
      {
         Iterator<String[]> iterator = new CsvReader(ignored).iterator();
         if (iterator.hasNext())
         {
            String[] words = iterator.next();
            for (String word : words)
               ignoredWords_.add(word);
         }
      }
   }
   
   private void writeIgnoredWords()
   {
      CsvWriter csvWriter = new CsvWriter();
      for (String ignored : ignoredWords_)
         csvWriter.writeValue(ignored);
      csvWriter.endLine();
      docUpdateSentinel_.setProperty(IGNORED_WORDS, 
                                     csvWriter.getValue(), 
                                     new NullProgressIndicator());
   }
   
   private void updateIndex()
   {
      allIgnoredWords_.clear();
      allIgnoredWords_.addAll(userDictionaryWords_);
      allIgnoredWords_.addAll(ignoredWords_);
      
      fireChangedEvent();
   }
   
   private void fireChangedEvent()
   {
      DomEvent.fireNativeEvent(Document.get().createChangeEvent(), 
                               handlerManager_);
   }
   
   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlerManager_.fireEvent(event);
   }
   
   private final DocUpdateSentinel docUpdateSentinel_;
   
   private final WorkbenchList userDictionary_;
   private ArrayList<String> userDictionaryWords_;
   private final ArrayList<String> ignoredWords_ = new ArrayList<String>();
   private final HashSet<String> allIgnoredWords_ = new HashSet<String>();
   
   HandlerManager handlerManager_ = new HandlerManager(this);
   
   private final static String IGNORED_WORDS = "ignored_words";  
}

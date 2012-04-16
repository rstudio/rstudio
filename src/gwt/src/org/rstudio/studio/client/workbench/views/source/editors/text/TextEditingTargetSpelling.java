/*
 * TextEditingTargetSpelling.java
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
import java.util.Iterator;

import org.rstudio.core.client.CsvReader;
import org.rstudio.core.client.CsvWriter;
import org.rstudio.core.client.widget.NullProgressIndicator;
import org.rstudio.studio.client.common.spelling.SpellChecker;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.CheckSpelling;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.InitialProgressDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingDialog;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.event.shared.HandlerRegistration;

public class TextEditingTargetSpelling implements SpellChecker.Context
{
   public TextEditingTargetSpelling(DocDisplay docDisplay,
                                    DocUpdateSentinel docUpdateSentinel)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;
      spellChecker_ = new SpellChecker(this);
      
   }
   
   public void checkSpelling()
   {
      new CheckSpelling(spellChecker_, docDisplay_,
                        new SpellingDialog(),
                        new InitialProgressDialog());
   }

   @Override
   public void invalidateAllWords()
   {
      
   }

   @Override
   public void invalidateMisspelledWords()
   {
      
   }  
   
   @Override
   public ArrayList<String> readDictionary()
   {
      ArrayList<String> ignoredWords = new ArrayList<String>();
      String ignored = docUpdateSentinel_.getProperty(IGNORED_WORDS);
      if (ignored != null)
      {
         Iterator<String[]> iterator = new CsvReader(ignored).iterator();
         if (iterator.hasNext())
         {
            String[] words = iterator.next();
            for (String word : words)
               ignoredWords.add(word);
         }
      }
      return ignoredWords;
   }

   @Override
   public void writeDictionary(ArrayList<String> ignoredWords)
   {
      CsvWriter csvWriter = new CsvWriter();
      for (String ignored : ignoredWords)
         csvWriter.writeValue(ignored);
      csvWriter.endLine();
      docUpdateSentinel_.setProperty(IGNORED_WORDS, 
                                     csvWriter.getValue(), 
                                     new NullProgressIndicator());   
   }
   
   void onDismiss()
   {
      while (releaseOnDismiss_.size() > 0)
         releaseOnDismiss_.remove(0).removeHandler();
   }
   

   @Override
   public void releaseOnDismiss(HandlerRegistration handler)
   {
      releaseOnDismiss_.add(handler);      
   }

   private final static String IGNORED_WORDS = "ignored_words"; 
   
   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel docUpdateSentinel_;
   private final SpellChecker spellChecker_;
 
   private ArrayList<HandlerRegistration> releaseOnDismiss_ = 
                                    new ArrayList<HandlerRegistration>();
}

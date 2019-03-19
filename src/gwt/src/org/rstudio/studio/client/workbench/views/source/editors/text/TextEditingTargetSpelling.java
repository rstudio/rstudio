/*
 * TextEditingTargetSpelling.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.MenuItem;
import org.rstudio.core.client.CsvReader;
import org.rstudio.core.client.CsvWriter;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.NullProgressIndicator;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.spelling.TypoSpellChecker;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.output.lint.LintManager;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.CheckSpelling;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.InitialProgressDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingDialog;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.event.shared.HandlerRegistration;

public class TextEditingTargetSpelling implements TypoSpellChecker.Context
{
   interface Resources extends ClientBundle
   {
      @Source("../../../../commands/goToWorkingDir.png")
      ImageResource addToDictIcon();
   }

   public TextEditingTargetSpelling(DocDisplay docDisplay,
                                    DocUpdateSentinel docUpdateSentinel,
                                    LintManager lintManager)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;
      lintManager_ = lintManager;
      typoSpellChecker_ = new TypoSpellChecker(this);
      injectContextMenuHandler();
   }

   public JsArray<LintItem> getLint()
   {
      Iterable<Range> wordSource = docDisplay_.getWords(
         docDisplay_.getFileType().getTokenPredicate(),
         docDisplay_.getFileType().getCharPredicate(),
         Position.create(0, 0),
         null);

      final ArrayList<String> words = new ArrayList<>();
      final ArrayList<Range> wordRanges = new ArrayList<>();

      for (Range r : wordSource)
      {
         // Don't worry about pathologically long words
         if (r.getEnd().getColumn() - r.getStart().getColumn() > 250)
            continue;

         wordRanges.add(r);
         words.add(docDisplay_.getTextForRange(r));
      }

      JsArray<LintItem> lint = JsArray.createArray().cast();
      if (wordRanges.size() > 0)
      {
         for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (!typoSpellChecker_.checkSpelling(word)) {
               Range range = wordRanges.get(i);
               lint.push(LintItem.create(
                  range.getStart().getRow(),
                  range.getStart().getColumn(),
                  range.getEnd().getRow(),
                  range.getEnd().getColumn(),
                  "Spellcheck warning",
                  "warning"));
            }
         }
      }
      return lint;
   }

   // Legacy checkSpelling function for popup dialog
   public void checkSpelling()
   {
      if (isSpellChecking_)
         return;
      isSpellChecking_ = true;
      new CheckSpelling(typoSpellChecker_, docDisplay_,
                        new SpellingDialog(),
                        new InitialProgressDialog(1000),
                        new ResultCallback<Void, Exception>()
                        {
                           @Override
                           public void onSuccess(Void result)
                           {
                              isSpellChecking_ = false;
                           }

                           @Override
                           public void onFailure(Exception e)
                           {
                              isSpellChecking_ = false;
                           }

                           @Override
                           public void onCancelled()
                           {
                              isSpellChecking_ = false;
                           }
                        });
   }

   @Override
   public void invalidateAllWords()
   {
      lintManager_.forceRelint();
   }

   @Override
   public void invalidateMisspelledWords()
   {
      
   }  
   
   @Override
   public ArrayList<String> readDictionary()
   {
      ArrayList<String> ignoredWords = new ArrayList<>();
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

   private void injectContextMenuHandler()
   {
      docDisplay_.addContextMenuHandler((event) ->
      {
         // If we have a selection, just return as the user likely wants to cut/copy/paste
         if (docDisplay_.hasSelection())
            return;

         // Get the word under the cursor
         Position pos = docDisplay_.getCursorPosition();
         Position endOfLine = Position.create(docDisplay_.getLine(pos.getRow()).length(), pos.getColumn());
         Iterable<Range> wordSource = docDisplay_.getWords(
            docDisplay_.getFileType().getTokenPredicate(),
            docDisplay_.getFileType().getCharPredicate(),
            pos,
            endOfLine);

         Iterator<Range> wordsIterator = wordSource.iterator();

         // If there's no word, just return
         if (!wordsIterator.hasNext())
            return;

         Range wordRange = wordsIterator.next();
         String word = docDisplay_.getTextForRange(wordRange);

         if (word.length() < 2 || typoSpellChecker_.checkSpelling(word))
            return;

         final ToolbarPopupMenu menu = new ToolbarPopupMenu();
         String[] suggestions = typoSpellChecker_.suggestionList(word);

         // We now know we're going to show our menu, stop default context menu
         event.preventDefault();
         event.stopPropagation();

         for (String suggestion : suggestions)
         {
            MenuItem suggestionItem = new MenuItem(
               AppCommand.formatMenuLabel(null, suggestion, ""),
               true,
               () -> {
                  docDisplay_.replaceRange(wordRange, suggestion);
                  docDisplay_.removeMarkersAtCursorPosition();
               });

            menu.addItem(suggestionItem);
         }

         // Only add a separator if we have suggestions to separate from
         if (suggestions.length > 0)
            menu.addSeparator();

         MenuItem addToDictionaryItem = new MenuItem(
            AppCommand.formatMenuLabel(RES.addToDictIcon(), "Add to user dictionary", ""),
            true,
            () -> {
               typoSpellChecker_.addToUserDictionary(word);
               docDisplay_.removeMarkersAtCursorPosition();
            });

         menu.addItem(addToDictionaryItem);


         menu.setPopupPositionAndShow((offWidth, offHeight) -> {
            menu.setPopupPosition(event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
         });
      });
   }

   private static final Resources RES = GWT.create(Resources.class);

   private final class InputKeyDownHandler implements KeyDownHandler
   {
      public void onKeyDown(KeyDownEvent event)
      {
      }
   }

   @Override
   public void releaseOnDismiss(HandlerRegistration handler)
   {
      releaseOnDismiss_.add(handler);      
   }

   private boolean isSpellChecking_;

   private final static String IGNORED_WORDS = "ignored_words"; 
   
   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel docUpdateSentinel_;
   private final LintManager lintManager_;
   private final TypoSpellChecker typoSpellChecker_;
 
   private ArrayList<HandlerRegistration> releaseOnDismiss_ = 
                                    new ArrayList<HandlerRegistration>();
}

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
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuItem;
import org.rstudio.core.client.CsvReader;
import org.rstudio.core.client.CsvWriter;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.NullProgressIndicator;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.spelling.TypoSpellChecker;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.output.lint.LintManager;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.TokenPredicate;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.CheckSpelling;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.InitialProgressDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingDialog;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.event.shared.HandlerRegistration;

public class TextEditingTargetSpelling implements TypoSpellChecker.Context
{
   interface Resources extends ClientBundle
   {
      @Source("./goToWorkingDir.png")
      ImageResource addToDictIcon();
   }

   public TextEditingTargetSpelling(DocDisplay docDisplay,
                                    DocUpdateSentinel docUpdateSentinel,
                                    LintManager lintManager,
                                    UIPrefs prefs)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;
      lintManager_ = lintManager;
      prefs_ = prefs;
      typoSpellChecker_ = new TypoSpellChecker(this);
      injectContextMenuHandler();
   }

   public JsArray<LintItem> getLint()
   {
      TextFileType fileType = docDisplay_.getFileType();
      TokenPredicate tokenPredicate = fileType.isR() ? fileType.getCommentsTokenPredicate() : fileType.getTokenPredicate();

      // only get tokens for the visible screen
      Iterable<Range> wordSource = docDisplay_.getWords(
         tokenPredicate,
         docDisplay_.getFileType().getCharPredicate(),
         Position.create(docDisplay_.getFirstVisibleRow(), 0),
         Position.create(docDisplay_.getLastVisibleRow(), docDisplay_.getLength(docDisplay_.getLastVisibleRow())));

      final ArrayList<String> words = new ArrayList<>();
      final ArrayList<Range> wordRanges = new ArrayList<>();

      for (Range r : wordSource)
      {
         // Don't worry about pathologically long words
         if (r.getEnd().getColumn() - r.getStart().getColumn() > 250)
            continue;

         wordRanges.add(r);
         words.add(docDisplay_.getTextForRange(r));

         // only check a certain number of words at once to not overwhelm the system
         if (wordRanges.size() > prefs_.maxCheckWords().getValue())
            break;
      }

      JsArray<LintItem> lint = JsArray.createArray().cast();
      if (wordRanges.size() > 0)
      {
         ArrayList<String> prefetchWords = new ArrayList<>();
         for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (!typoSpellChecker_.checkSpelling(word)) {
               if (prefetchWords.size() < prefs_.maxPrefetchWords().getValue())
                  prefetchWords.add(word);
               Range range = wordRanges.get(i);
               lint.push(LintItem.create(
                  range.getStart().getRow(),
                  range.getStart().getColumn(),
                  range.getEnd().getRow(),
                  range.getEnd().getColumn(),
                  "Spellcheck",
                  "spelling"));
            }
         }
         typoSpellChecker_.prefetchWords(prefetchWords);
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
      invalidateMisspelledWords();
      lintManager_.relintAfterDelay(LintManager.DEFAULT_LINT_DELAY);
   }

   @Override
   public void invalidateMisspelledWords()
   {
      docDisplay_.removeMarkers((a, m) -> a != null && a.text().toLowerCase().contains("spellcheck"));
   }  

   @Override
   public void invalidateWord(String word)
   {
      docDisplay_.removeMarkersAtWord(word);
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
         TextFileType fileType = docDisplay_.getFileType();
         TokenPredicate tokenPredicate = fileType.isR() ? fileType.getCommentsTokenPredicate() : fileType.getTokenPredicate();
         Position endOfLine = Position.create(pos.getRow()+1, 0);
         Iterable<Range> wordSource = docDisplay_.getWords(
            tokenPredicate,
            docDisplay_.getFileType().getCharPredicate(),
            pos,
            endOfLine);

         Iterator<Range> wordsIterator = wordSource.iterator();

         if (!wordsIterator.hasNext())
            return;

         String word;
         Range wordRange = wordsIterator.next();

         while (!wordRange.contains(pos))
         {
            if (wordsIterator.hasNext())
               wordRange = wordsIterator.next();
            else
               break;
         }
         word = docDisplay_.getTextForRange(wordRange);

         if (word == null || typoSpellChecker_.checkSpelling(word))
            return;

         // final variables for lambdas
         final String replaceWord = word;
         final Range replaceRange = wordRange;

         final ToolbarPopupMenu menu = new ToolbarPopupMenu();

         String[] suggestions = typoSpellChecker_.suggestionList(word);

         // We now know we're going to show our menu, stop default context menu
         event.preventDefault();
         event.stopPropagation();

         int i = 0;
         for (String suggestion : suggestions)
         {
            // Only show a limited number of suggestions
            if (i >= MAX_SUGGESTIONS)
               break;

            MenuItem suggestionItem = new MenuItem(
               AppCommand.formatMenuLabel(null, suggestion, ""),
               true,
               () -> {
                  docDisplay_.removeMarkersAtCursorPosition();
                  docDisplay_.replaceRange(replaceRange, suggestion);
                  lintManager_.relintAfterDelay(LintManager.DEFAULT_LINT_DELAY);
               });

            menu.addItem(suggestionItem);
            i++;
         }

         // Only add a separator if we have suggestions to separate from
         if (suggestions.length > 0)
            menu.addSeparator();

         MenuItem ignoreItem = new MenuItem(
            AppCommand.formatMenuLabel(null, "Ignore word", ""),
            true,
            () -> {
               typoSpellChecker_.addIgnoredWord(replaceWord);
               docDisplay_.removeMarkersAtCursorPosition();
            });

         menu.addItem(ignoreItem);
         menu.addSeparator();

         MenuItem addToDictionaryItem = new MenuItem(
            AppCommand.formatMenuLabel(RES.addToDictIcon(), "Add to user dictionary", ""),
            true,
            () -> {
               typoSpellChecker_.addToUserDictionary(replaceWord);
               docDisplay_.removeMarkersAtCursorPosition();
            });

         menu.addItem(addToDictionaryItem);

         menu.setPopupPositionAndShow((offWidth, offHeight) -> {
            int clientX = event.getNativeEvent().getClientX();
            int clientY = event.getNativeEvent().getClientY();
            int menuX = Math.min(clientX, Window.getClientWidth() - offWidth);
            int menuY = Math.min(clientY, Window.getClientHeight() - offHeight);
            menu.setPopupPosition(menuX, menuY);
         });
      });

      // relint the viewport as the user scrolls around
      docDisplay_.addScrollYHandler((event) -> lintManager_.relintAfterDelay(LintManager.DEFAULT_LINT_DELAY));
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
   private final static int MAX_SUGGESTIONS = 5;

   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel docUpdateSentinel_;
   private final LintManager lintManager_;
   private final UIPrefs prefs_;
   private final TypoSpellChecker typoSpellChecker_;
 
   private ArrayList<HandlerRegistration> releaseOnDismiss_ = 
                                    new ArrayList<HandlerRegistration>();
}

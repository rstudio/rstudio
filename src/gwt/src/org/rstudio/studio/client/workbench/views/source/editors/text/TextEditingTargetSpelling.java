/*
 * TextEditingTargetSpelling.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.spelling.TypoSpellChecker;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.output.lint.LintManager;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingDoc;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;


public class TextEditingTargetSpelling extends SpellingContext
{
   interface Resources extends ClientBundle
   {
      @Source("./goToWorkingDir.png")
      ImageResource addToDictIcon();
   }

   public TextEditingTargetSpelling(DocDisplay docDisplay,
                                    DocUpdateSentinel docUpdateSentinel,
                                    LintManager lintManager,
                                    UserPrefs prefs)
   {
      super(docUpdateSentinel);
      docDisplay_ = docDisplay;
      lintManager_ = lintManager;
      prefs_ = prefs;
      injectContextMenuHandler();
   }
   

   public JsArray<LintItem> getLint()
   {
      JsArray<LintItem> lint = JsArray.createArray().cast();

      /*
         There are certain dictionaries blacklisted from realtime spellchecking, it's
         possible due to external preference funny business to get into a state where
         this function was incorrectly called. Detect that state, clean it up, and return.
       */
      final String language = prefs_.spellingDictionaryLanguage().getValue();
      if (!TypoSpellChecker.canRealtimeSpellcheckDict(language))
      {
         prefs_.realTimeSpellchecking().setGlobalValue(false);
         return lint;
      }
      
      SpellingDoc spellingDoc = docDisplay_.getSpellingDoc();
      
      // only get tokens for the visible screen
      Iterable<SpellingDoc.WordRange> wordSource = spellingDoc.getWords(
         docDisplay_.indexFromPosition(Position.create(docDisplay_.getFirstVisibleRow(), 0)),
         docDisplay_.indexFromPosition(Position.create(docDisplay_.getLastVisibleRow(), docDisplay_.getLength(docDisplay_.getLastVisibleRow()))));

      final ArrayList<SpellingDoc.WordRange> wordRanges = new ArrayList<>();
      ArrayList<String> prefetchWords = new ArrayList<>();

     
      for (SpellingDoc.WordRange wordRange : wordSource)
      {
         if (!typo().shouldCheckSpelling(spellingDoc, wordRange))
            continue;

         wordRanges.add(wordRange);

         // only check a certain number of words at once to not overwhelm the system
         if (wordRanges.size() > prefs_.maxSpellcheckWords().getValue())
            break;

         String word = spellingDoc.getText(wordRange);
         if (!typo().checkSpelling(word)) {
            if (prefetchWords.size() < prefs_.maxSpellcheckPrefetch().getValue())
               prefetchWords.add(word);

            Position wordStart = docDisplay_.positionFromIndex(wordRange.start);
            Position wordEnd = docDisplay_.positionFromIndex(wordRange.end);
            
            lint.push(LintItem.create(
               wordStart.getRow(),
               wordStart.getColumn(),
               wordEnd.getRow(),
               wordEnd.getColumn(),
               "Spellcheck",
               "spelling"));
         }
      }

      if (prefetchWords.size() > 0)
         typo().prefetchWords(prefetchWords);

      return lint;
   }

   @Override
   public void invalidateAllWords()
   {
      docDisplay_.removeMarkers((a, m) -> a != null && a.text().toLowerCase().contains("spellcheck"));
      lintManager_.relintAfterDelay(LintManager.DEFAULT_LINT_DELAY);
   }

   @Override
   public void invalidateWord(String word, boolean userDictionary)
   {
      docDisplay_.removeMarkersAtWord(word);
   }
   

   private void injectContextMenuHandler()
   {
      docDisplay_.addContextMenuHandler((event) ->
      {
         // If real time checking is off or
         // If we have a selection, just return as the user likely wants to cut/copy/paste
         if (!prefs_.realTimeSpellchecking().getValue() || docDisplay_.hasSelection())
            return;
         
         SpellingDoc spellingDoc = docDisplay_.getSpellingDoc();

         // Get the word under the cursor
         Position pos = docDisplay_.getCursorPosition();
         Position endOfLine = Position.create(pos.getRow()+1, 0);
         Iterable<SpellingDoc.WordRange> wordSource = spellingDoc.getWords(
            docDisplay_.indexFromPosition(pos),
            docDisplay_.indexFromPosition(endOfLine)
         );

         Iterator<SpellingDoc.WordRange> wordsIterator = wordSource.iterator();

         if (!wordsIterator.hasNext())
            return;

         String word;
         SpellingDoc.WordRange nextWord = wordsIterator.next();
         
         Range wordRange = Range.fromPoints(
            docDisplay_.positionFromIndex(nextWord.start), 
            docDisplay_.positionFromIndex(nextWord.end)
         );

         while (!wordRange.contains(pos))
         {
            if (wordsIterator.hasNext())
               nextWord = wordsIterator.next();
            else
               break;
         }
         word = spellingDoc.getText(nextWord);

         if (word == null ||
             !typo().shouldCheckSpelling(spellingDoc, nextWord) ||
             typo().checkSpelling(word))
         {
            return;
         }

         // final variables for lambdas
         final String replaceWord = word;
         final Range replaceRange = wordRange;

         final ToolbarPopupMenu menu = new ToolbarPopupMenu();

         String[] suggestions = typo().suggestionList(word);

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
               typo().addIgnoredWord(replaceWord);
               docDisplay_.removeMarkersAtCursorPosition();
            });

         menu.addItem(ignoreItem);
         menu.addSeparator();

         MenuItem addToDictionaryItem = new MenuItem(
            AppCommand.formatMenuLabel(RES.addToDictIcon(), "Add to user dictionary", ""),
            true,
            () -> {
               typo().addToUserDictionary(replaceWord);
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

   @SuppressWarnings("unused")
   private final class InputKeyDownHandler implements KeyDownHandler
   {
      public void onKeyDown(KeyDownEvent event)
      {
      }
   }

   private final static int MAX_SUGGESTIONS = 5;

   private final DocDisplay docDisplay_;
   private final LintManager lintManager_;
   private final UserPrefs prefs_;
  

}

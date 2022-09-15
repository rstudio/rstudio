/*
 * TextEditingTargetSpelling.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuItem;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.spelling.model.SpellCheckerResult;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
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


   public void getLint(ServerRequestCallback<JsArray<LintItem>> request)
   {
      JsArray<LintItem> lint = JsArray.createArray().cast();
      SpellingDoc spellingDoc = docDisplay_.getSpellingDoc();

      // only get tokens for the visible screen
      Iterable<SpellingDoc.WordRange> wordSource = spellingDoc.getWords
        (docDisplay_.indexFromPosition(Position.create(docDisplay_.getFirstVisibleRow(), 0)),
        docDisplay_.indexFromPosition(Position.create(docDisplay_.getLastVisibleRow(),
        docDisplay_.getLength(docDisplay_.getLastVisibleRow()))));

      HashMap<String, ArrayList<SpellingDoc.WordRange>> wordsInRanges = new HashMap<>();

      for (SpellingDoc.WordRange wordRange : wordSource)
      {
         if (!spellChecker().shouldCheckSpelling(spellingDoc, wordRange))
            continue;

         // build up the HashMap of words to wordranges
         String word = spellingDoc.getText(wordRange);
         ArrayList<SpellingDoc.WordRange> list;
         if (wordsInRanges.containsKey(word))
         {
            list = wordsInRanges.get(word);
         }
         else
         {
            list = new ArrayList<>();
         }
         list.add(wordRange);
         wordsInRanges.put(word, list);
      }

      spellChecker().checkWords(new ArrayList<>(wordsInRanges.keySet()), new ServerRequestCallback<SpellCheckerResult>()
      {
         @Override
         public void onResponseReceived(SpellCheckerResult response)
         {
            // for each incorrect word from the server
            // get the word ranges for that word and add to lint
            for (String word : response.getIncorrect())
            {
               ArrayList<SpellingDoc.WordRange> ranges = wordsInRanges.get(word);
               for (SpellingDoc.WordRange r : ranges)
               {
                  Position wordStart = docDisplay_.positionFromIndex(r.start);
                  Position wordEnd = docDisplay_.positionFromIndex(r.end);

                  // bail if this is a yaml comment
                  if (docDisplay_.getFileType().isRmd())
                  {
                     Scope scope = docDisplay_.getChunkAtPosition(wordStart);
                     if (scope != null && scope.isChunk())
                     {
                        String line = docDisplay_.getLine(wordStart.getRow());
                        if (line.trim().startsWith("#|"))
                           continue;
                     }
                  }
                  
                  lint.push(LintItem.create(wordStart.getRow(), wordStart.getColumn(), wordEnd.getRow(), wordEnd.getColumn(), constants_.spellcheck(), "spelling"));
               }
            }

            request.onResponseReceived(lint);

         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
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
                 Position endOfLine = Position.create(pos.getRow() + 1, 0);
                 Iterable<SpellingDoc.WordRange> wordSource = spellingDoc.getWords(docDisplay_.indexFromPosition(pos), docDisplay_.indexFromPosition(endOfLine));

                 Iterator<SpellingDoc.WordRange> wordsIterator = wordSource.iterator();

                 if (!wordsIterator.hasNext())
                    return;

                 String word;
                 SpellingDoc.WordRange nextWord = wordsIterator.next();

                 Range wordRange = Range.fromPoints(docDisplay_.positionFromIndex(nextWord.start), docDisplay_.positionFromIndex(nextWord.end));

                 while (!wordRange.contains(pos))
                 {
                    if (wordsIterator.hasNext())
                       nextWord = wordsIterator.next();
                    else
                       break;
                 }
                 word = spellingDoc.getText(nextWord);

                 if (word == null || !spellChecker().shouldCheckSpelling(spellingDoc, nextWord))
                    return;

                 // final variables for lambdas
                 final String replaceWord = word;
                 final Range replaceRange = wordRange;
                 final NativeEvent e = event.getNativeEvent();
                 final ToolbarPopupMenu menu = new ToolbarPopupMenu();

                 // We now know we're going to show our menu, stop default context menu
                 event.preventDefault();
                 event.stopPropagation();

                 ArrayList<String> asyncWord = new ArrayList<>();
                 asyncWord.add(word);

                 spellChecker().checkWords(asyncWord, new ServerRequestCallback<SpellCheckerResult>()
                 {
                    @Override
                    public void onResponseReceived(SpellCheckerResult response)
                    {
                       if (response.getIncorrect().size() > 0)
                       {
                          spellChecker().suggestionList(word, new ServerRequestCallback<JsArrayString>()
                          {
                             @Override
                             public void onResponseReceived(JsArrayString response)
                             {
                                for (int i = 0; i < response.length(); i++)
                                {
                                   String suggestion = response.get(i);

                                   // Only show a limited number of suggestions
                                   if (i >= MAX_SUGGESTIONS)
                                      break;

                                   MenuItem suggestionItem = new MenuItem(AppCommand.formatMenuLabel(null, suggestion, ""), true, () ->
                                   {
                                      docDisplay_.removeMarkersAtCursorPosition();
                                      docDisplay_.replaceRange(replaceRange, suggestion);
                                      lintManager_.relintAfterDelay(LintManager.DEFAULT_LINT_DELAY);
                                   });

                                   menu.addItem(suggestionItem);
                                   i++;
                                }

                                // Only add a separator if we have suggestions to separate from
                                if (response.length() > 0)
                                   menu.addSeparator();

                                MenuItem ignoreItem = new MenuItem(AppCommand.formatMenuLabel(null, constants_.ignoreWord(), ""), true, () ->
                                {
                                   spellChecker().addIgnoredWord(replaceWord);
                                   docDisplay_.removeMarkersAtCursorPosition();
                                });

                                menu.addItem(ignoreItem);
                                menu.addSeparator();

                                MenuItem addToDictionaryItem = new MenuItem(AppCommand.formatMenuLabel(RES.addToDictIcon(), constants_.addToUserDictionary(), ""), true, () ->
                                {
                                   spellChecker().addToUserDictionary(replaceWord);
                                   docDisplay_.removeMarkersAtCursorPosition();
                                });

                                menu.addItem(addToDictionaryItem);

                                menu.setPopupPositionAndShow((offWidth, offHeight) ->
                                {
                                   int clientX = e.getClientX();
                                   int clientY = e.getClientY();
                                   int menuX = Math.min(clientX, Window.getClientWidth() - offWidth);
                                   int menuY = Math.min(clientY, Window.getClientHeight() - offHeight);
                                   menu.setPopupPosition(menuX, menuY);
                                });
                             }

                             @Override
                             public void onError(ServerError error)
                             {
                                Debug.logError(error);
                             }
                          });
                       }
                    }

                    @Override
                    public void onError(ServerError error)
                    {
                       Debug.logError(error);
                    }
                 });
              });

      // relint the viewport as the user scrolls around
      docDisplay_.addScrollYHandler((event) -> lintManager_.relintAfterDelay(LintManager.DEFAULT_LINT_DELAY));
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
   private static final EditorsTextConstants constants_ = GWT.create(EditorsTextConstants.class);

}

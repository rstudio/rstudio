/*
 * CheckSpelling.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.PopupPanel;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.spelling.RealtimeSpellChecker;
import org.rstudio.studio.client.common.spelling.model.SpellCheckerResult;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;


import java.util.ArrayList;
import java.util.HashMap;

public class CheckSpelling
{
   public interface Display extends HasCloseHandlers<PopupPanel>
   {
      HasClickHandlers getAddButton();
      HasClickHandlers getIgnoreAllButton();
      HasClickHandlers getSkipButton();
      HasClickHandlers getChangeButton();
      HasClickHandlers getChangeAllButton();

      HasText getMisspelledWord();
      HasText getReplacement();
      void setSuggestions(String[] values);
      void clearSuggestions();
      HasChangeHandlers getSuggestionList();
      String getSelectedSuggestion();

      void focusReplacement();

      void showModal();
      boolean isShowing();
      void closeDialog();

      void showProgress();
      void hideProgress();

      void setEditorSelectionBounds(Rectangle bounds);
   }

   public interface ProgressDisplay
   {
      void show();
      void hide();
      boolean isShowing();
      HasClickHandlers getCancelButton();
   }

   public CheckSpelling(RealtimeSpellChecker spellChecker,
                        SpellingDoc spellingDoc,
                        Display view,
                        ProgressDisplay progressDisplay,
                        ResultCallback<Void, Exception> callback)
   {
      realtimeSpellChecker_ = spellChecker;
      spellingDoc_ = spellingDoc;
      view_ = view;
      progressDisplay_ = progressDisplay;
      callback_ = callback;

      currentPos_ = spellingDoc_.getSelectionStart();
      initialCursorPos_ = spellingDoc_.createAnchor(currentPos_);
      wrapped_ = false;

      view_.getChangeButton().addClickHandler((ClickEvent event) ->
         {
            doReplacement(view_.getReplacement().getText());
            findNextMisspelling();
      });

      view_.getChangeAllButton().addClickHandler((ClickEvent event) ->
      {
         if (!view_.getMisspelledWord().getText().equals(view_.getReplacement().getText()))
         {
            changeAll_.put(view_.getMisspelledWord().getText(),
                           view_.getReplacement().getText());
         }
         doReplacement(view_.getReplacement().getText());
         findNextMisspelling();
      });

      view_.getSkipButton().addClickHandler((ClickEvent event) ->
      {
         currentPos_ = spellingDoc_.getCursorPosition() + 1;
         findNextMisspelling();
      });

      view_.getIgnoreAllButton().addClickHandler((ClickEvent event) ->
      {
         realtimeSpellChecker_.addIgnoredWord(view_.getMisspelledWord().getText());
         currentPos_ = spellingDoc_.getCursorPosition() + 1;
         findNextMisspelling();
      });

      view_.getAddButton().addClickHandler((ClickEvent event) ->
      {
         realtimeSpellChecker_.addToUserDictionary(view_.getMisspelledWord().getText());
         currentPos_ = spellingDoc_.getCursorPosition() + 1;
         findNextMisspelling();
      });

      view_.getSuggestionList().addChangeHandler((ChangeEvent event) ->
      {
         String replacement = view_.getSelectedSuggestion();
         if (replacement != null) view_.getReplacement().setText(replacement);
      });

      view_.addCloseHandler((CloseEvent<PopupPanel> popupPanelCloseEvent) -> cancel());

      progressDisplay_.getCancelButton().addClickHandler((ClickEvent event) ->
      {
         cancel();
         progressDisplay_.hide();
      });

      progressDisplay_.show();
      findNextMisspelling();
   }

   private void cancel()
   {
      spellingDoc_.dispose();
      canceled_ = true;
      callback_.onCancelled();
   }

   private void doReplacement(String replacement)
   {
      spellingDoc_.replaceSelection(replacement);
      currentPos_ = spellingDoc_.getSelectionEnd() + 1;
   }

   private void findNextMisspelling()
   {
      try
      {
         if (checkForCancel())
            return;

         showProgress();

         Iterable<SpellingDoc.WordRange> wordSource = spellingDoc_.getWords(
               currentPos_,
               wrapped_ ? initialCursorPos_.getPosition() : -1);

         final ArrayList<String> words = new ArrayList<>();
         final ArrayList<SpellingDoc.WordRange> checkWords = new ArrayList<>();

         SpellingDoc.WordRange lastWord = null;
         for (SpellingDoc.WordRange w : wordSource)
         {
            // update last (so that whenever the loop terminates we know the location
            // of the last word that we iterated over)
            lastWord = w;
          
            if (!realtimeSpellChecker_.shouldCheckSpelling(spellingDoc_, w))
               continue;

            checkWords.add(w);
            words.add(spellingDoc_.getText(w));

            // Check a maximum of N words at a time
            if (checkWords.size() == 100)
               break;
         }

         if (checkWords.size() > 0)
         {
            final int endCheckedPos = lastWord.end;
            realtimeSpellChecker_.checkWords(words, new SimpleRequestCallback<SpellCheckerResult>()
            {
               @Override
               public void onResponseReceived(SpellCheckerResult response)
               {
                  if (checkForCancel())
                     return;

                  for (int i = 0; i < words.size(); i++)
                  {
                     if (response.getIncorrect().contains(words.get(i)))
                     {
                        handleMisspelledWord(checkWords.get(i));
                        return;
                     }
                  }

                  int initialCursorPos = initialCursorPos_.getPosition();
                  if (wrapped_ && (endCheckedPos >= initialCursorPos))
                  {
                     onSpellingComplete();
                  }
                  else
                  {
                     currentPos_ = endCheckedPos + 1;
                     Scheduler.get().scheduleDeferred(() -> findNextMisspelling());
                  }
               }
            });
         }
         else
         {
            // No misspellings
            if (wrapped_)
            {
               onSpellingComplete();
            }
            else
            {
               wrapped_ = true;
               currentPos_ = 0;
               findNextMisspelling();
            }
         }
      }
      catch (Exception e)
      {
         Debug.log(e.toString());
         close();
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               constants_.checkSpelling(),
               constants_.anErrorHasOccurredMessage(e.getMessage()));
         callback_.onFailure(e);
      }
   }
   
   private void onSpellingComplete()
   {
      close();
      RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
            GlobalDisplay.MSG_INFO,
            constants_.checkSpelling(),
            constants_.spellCheckIsComplete());
      callback_.onSuccess(Void.create());
   }

   private void close()
   {
      progressDisplay_.hide();
      view_.closeDialog();
   }

   private boolean checkForCancel()
   {
      return canceled_;
   }

   private void showProgress()
   {
      if (view_.isShowing())
         view_.showProgress();
   }

   private void showDialog(Rectangle selectedWordBounds)
   {
      if (progressDisplay_.isShowing())
         progressDisplay_.hide();

      view_.setEditorSelectionBounds(selectedWordBounds);
      if (!view_.isShowing())
         view_.showModal();
      view_.hideProgress();
   }

   private void handleMisspelledWord(SpellingDoc.WordRange misspelledWord)
   {
      try
      {
         spellingDoc_.setSelection(misspelledWord);
         spellingDoc_.moveCursorNearTop();
         view_.clearSuggestions();
         view_.getReplacement().setText("");

         final String word = spellingDoc_.getText(misspelledWord);

         if (changeAll_.containsKey(word))
         {
            doReplacement(changeAll_.get(word));
            findNextMisspelling();
            return;
         }

         view_.getMisspelledWord().setText(word);

         // This fixed delay is regrettable but necessary as it can take some
         // time for Ace's scrolling logic to actually execute (i.e. the next
         // time the renderloop runs). If we don't wait, then misspelled words
         // at the end of the document will result in misreported cursor bounds,
         // meaning we'll be avoiding a completely incorrect region.
         Scheduler.get().scheduleFixedDelay(() ->
         {
            showDialog(spellingDoc_.getCursorBounds());

            view_.focusReplacement();

            realtimeSpellChecker_.suggestionList(word, new ServerRequestCallback<JsArrayString>()
            {
               @Override
               public void onResponseReceived(
                  JsArrayString response)
               {
                  String[] suggestions = JsUtil.toStringArray(response);
                  view_.setSuggestions(suggestions);
                  if (suggestions.length > 0)
                  {
                     view_.getReplacement().setText(suggestions[0]);
                     view_.focusReplacement();
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                 Debug.logError(error);
               }
            });

            return false;
         }, 100);
     }
      catch (Exception e)
      {
         Debug.log(e.toString());
         close();
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               constants_.checkSpelling(),
               constants_.anErrorHasOccurredMessage(e.getMessage()));
         callback_.onFailure(e);
      }
   }

   private final RealtimeSpellChecker realtimeSpellChecker_;
   private final SpellingDoc spellingDoc_;
   private final Display view_;
   private final ProgressDisplay progressDisplay_;
   private final ResultCallback<org.rstudio.studio.client.server.Void, Exception> callback_;
   private final SpellingDoc.Anchor initialCursorPos_;

   private final HashMap<String, String> changeAll_ = new HashMap<>();

   private int currentPos_;

   private boolean wrapped_;
   private boolean canceled_;
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}

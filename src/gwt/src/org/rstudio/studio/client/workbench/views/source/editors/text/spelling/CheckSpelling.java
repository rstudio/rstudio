/*
 * CheckSpelling.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

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
import org.rstudio.studio.client.common.spelling.TypoSpellChecker;
import org.rstudio.studio.client.common.spelling.model.SpellCheckerResult;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

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

   public CheckSpelling(TypoSpellChecker spellChecker,
                        DocDisplay docDisplay,
                        Display view,
                        ProgressDisplay progressDisplay,
                        ResultCallback<Void, Exception> callback)
   {
      typoSpellChecker_ = spellChecker;
      docDisplay_ = docDisplay;
      view_ = view;
      progressDisplay_ = progressDisplay;
      callback_ = callback;

      currentPos_ = docDisplay_.getSelectionStart();
      initialCursorPos_ = docDisplay_.createAnchor(currentPos_);
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
         currentPos_ = docDisplay_.getCursorPosition();
         findNextMisspelling();
      });

      view_.getIgnoreAllButton().addClickHandler((ClickEvent event) ->
      {
         typoSpellChecker_.addIgnoredWord(view_.getMisspelledWord().getText());
         currentPos_ = docDisplay_.getCursorPosition();
         findNextMisspelling();
      });

      view_.getAddButton().addClickHandler((ClickEvent event) ->
      {
         typoSpellChecker_.addToUserDictionary(view_.getMisspelledWord().getText());
         currentPos_ = docDisplay_.getCursorPosition();
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
      canceled_ = true;
      callback_.onCancelled();
   }

   private void doReplacement(String replacement)
   {
      docDisplay_.replaceSelection(replacement);
      currentPos_ = docDisplay_.getSelectionEnd();
   }

   private void findNextMisspelling()
   {
      try
      {
         if (checkForCancel())
            return;

         showProgress();

         Iterable<Range> wordSource = docDisplay_.getWords(
               docDisplay_.getFileType().getSpellCheckTokenPredicate(),
               docDisplay_.getFileType().getCharPredicate(),
               currentPos_,
               wrapped_ ? initialCursorPos_.getPosition() : null);

         final ArrayList<String> words = new ArrayList<String>();
         final ArrayList<Range> wordRanges = new ArrayList<Range>();

         for (Range r : wordSource)
         {
            if (!typoSpellChecker_.shouldCheckSpelling(docDisplay_, r))
               continue;

            wordRanges.add(r);
            words.add(docDisplay_.getTextForRange(r));

            // Check a maximum of N words at a time
            if (wordRanges.size() == 100)
               break;
         }

         if (wordRanges.size() > 0)
         {
            typoSpellChecker_.checkSpelling(words, new SimpleRequestCallback<SpellCheckerResult>()
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
                        handleMisspelledWord(wordRanges.get(i));
                        return;
                     }
                  }

                  currentPos_ = wordRanges.get(wordRanges.size()-1).getEnd();
                  // Everything spelled correctly, continue
                  Scheduler.get().scheduleDeferred(() -> findNextMisspelling());
               }
            });
         }
         else
         {
            // No misspellings
            if (wrapped_)
            {
               close();
               RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
                     GlobalDisplay.MSG_INFO,
                     "Check Spelling",
                     "Spell check is complete.");
               callback_.onSuccess(Void.create());
            }
            else
            {
               wrapped_ = true;
               currentPos_ = Position.create(0, 0);
               findNextMisspelling();
            }
         }
      }
      catch (Exception e)
      {
         Debug.log(e.toString());
         close();
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               "Check Spelling",
               "An error has occurred:\n\n" + e.getMessage());
         callback_.onFailure(e);
      }
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

   private void handleMisspelledWord(Range range)
   {
      try
      {
         docDisplay_.setSelectionRange(range);
         docDisplay_.moveCursorNearTop();
         view_.clearSuggestions();
         view_.getReplacement().setText("");

         final String word = docDisplay_.getTextForRange(range);

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
            showDialog(docDisplay_.getCursorBounds());

            view_.focusReplacement();

            // If Typo isn't loaded or isn't able to load (blacklisted dictionary)
            // just defer to the async backend dictionary. Once we can load all dictionaries
            // the legacy code can be removed.
            if (TypoSpellChecker.isLoaded())
            {
               String[] suggestions = typoSpellChecker_.suggestionList(word);
               view_.setSuggestions(suggestions);
               if (suggestions.length > 0)
               {
                  view_.getReplacement().setText(suggestions[0]);
                  view_.focusReplacement();
               }
            }
            else
            {
               typoSpellChecker_.legacySuggestionList(word, new ServerRequestCallback<JsArrayString>()
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
            }

            return false;
         }, 100);
     }
      catch (Exception e)
      {
         Debug.log(e.toString());
         close();
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               "Check Spelling",
               "An error has occurred:\n\n" + e.getMessage());
         callback_.onFailure(e);
      }
   }

   private final TypoSpellChecker typoSpellChecker_;
   private final DocDisplay docDisplay_;
   private final Display view_;
   private final ProgressDisplay progressDisplay_;
   private final ResultCallback<org.rstudio.studio.client.server.Void, Exception> callback_;
   private final Anchor initialCursorPos_;

   private final HashMap<String, String> changeAll_ = new HashMap<>();

   private Position currentPos_;

   private boolean wrapped_;
   private boolean canceled_;
}

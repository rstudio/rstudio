package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.HasText;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.spelling.SpellChecker;
import org.rstudio.studio.client.common.spelling.model.SpellCheckerResult;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import java.util.ArrayList;
import java.util.HashMap;

public class CheckSpelling
{
   public interface Display
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

      void showModal();
      void hide();
      void closeDialog();
   }

   public CheckSpelling(SpellChecker spellChecker,
                        DocDisplay docDisplay,
                        Display view)
   {
      spellChecker_ = spellChecker;
      docDisplay_ = docDisplay;
      view_ = view;

      currentPos_ = docDisplay_.getCursorPosition();
      initialCursorPos_ = docDisplay_.createAnchor(currentPos_);
      wrapped_ = false;

      view_.getChangeButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            doReplacement(view_.getReplacement().getText());
            findNextMisspelling();
         }
      });

      view_.getChangeAllButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            if (!view_.getMisspelledWord().getText().equals(view_.getReplacement().getText()))
            {
               changeAll_.put(view_.getMisspelledWord().getText(),
                              view_.getReplacement().getText());
            }
            doReplacement(view_.getReplacement().getText());
            findNextMisspelling();
         }
      });

      view_.getSkipButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            currentPos_ = docDisplay_.getCursorPosition();
            findNextMisspelling();
         }
      });

      view_.getIgnoreAllButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            spellChecker_.addIgnoredWord(view_.getMisspelledWord().getText());
            currentPos_ = docDisplay_.getCursorPosition();
            findNextMisspelling();
         }
      });

      view_.getAddButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            spellChecker_.addToUserDictionary(
                  view_.getMisspelledWord().getText());
            currentPos_ = docDisplay_.getCursorPosition();
            findNextMisspelling();
         }
      });

      view_.getSuggestionList().addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            String replacement = view_.getSelectedSuggestion();
            if (replacement != null)
               view_.getReplacement().setText(replacement);
         }
      });

      findNextMisspelling();
   }

   private void doReplacement(String replacement)
   {
      docDisplay_.replaceSelection(replacement);
      // Spell check what we just replaced
      currentPos_ = docDisplay_.getSelectionStart();
   }

   private void findNextMisspelling()
   {
      view_.hide();

      Iterable<Range> wordSource = docDisplay_.getWords(
            docDisplay_.getFileType().getTokenPredicate(),
            docDisplay_.getFileType().getCharPredicate(),
            currentPos_,
            wrapped_ ? initialCursorPos_.getPosition() : null);

      final ArrayList<String> words = new ArrayList<String>();
      final ArrayList<Range> wordRanges = new ArrayList<Range>();

      for (Range r : wordSource)
      {
         // Don't worry about pathologically long words
         if (r.getEnd().getColumn() - r.getStart().getColumn() > 250)
            continue;

         wordRanges.add(r);
         words.add(docDisplay_.getTextForRange(r));

         // Check a maximum of N words at a time
         if (wordRanges.size() == 100)
            break;
      }

      if (wordRanges.size() > 0)
      {
         spellChecker_.checkSpelling(words, new SimpleRequestCallback<SpellCheckerResult>()
         {
            @Override
            public void onResponseReceived(SpellCheckerResult response)
            {
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
               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     findNextMisspelling();
                  }
               });
            }
         });
      }
      else
      {
         // No misspellings
         if (wrapped_)
         {
            view_.closeDialog();
            RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
                  GlobalDisplay.MSG_INFO,
                  "Check Spelling",
                  "Spell check is complete.");
         }
         else
         {
            wrapped_ = true;
            currentPos_ = Position.create(0, 0);
            findNextMisspelling();
         }
      }
   }

   private void handleMisspelledWord(Range range)
   {
      docDisplay_.setSelectionRange(range);
      view_.clearSuggestions();
      view_.getReplacement().setText("");

      String word = docDisplay_.getTextForRange(range);

      if (changeAll_.containsKey(word))
      {
         doReplacement(changeAll_.get(word));
         findNextMisspelling();
         return;
      }

      view_.getMisspelledWord().setText(word);
      view_.showModal();

      spellChecker_.suggestionList(word, new ServerRequestCallback<JsArrayString>()
      {
         @Override
         public void onResponseReceived(JsArrayString response)
         {
            String[] suggestions = JsUtil.toStringArray(response);
            view_.setSuggestions(suggestions);
            if (suggestions.length > 0)
               view_.getReplacement().setText(suggestions[0]);
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }

   private final SpellChecker spellChecker_;
   private final DocDisplay docDisplay_;
   private final Display view_;
   private final Anchor initialCursorPos_;

   private final HashMap<String, String> changeAll_ = new HashMap<String, String>();

   private Position currentPos_;

   private boolean wrapped_;
}

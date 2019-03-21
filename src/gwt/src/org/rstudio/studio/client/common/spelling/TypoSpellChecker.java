/*
 * SpellChecker.java
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
package org.rstudio.studio.client.common.spelling;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.spelling.model.SpellCheckerResult;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchList;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TypoSpellChecker
{
   public interface Context
   {
      ArrayList<String> readDictionary();
      void writeDictionary(ArrayList<String> words);

      void invalidateAllWords();
      void invalidateMisspelledWords();

      void releaseOnDismiss(HandlerRegistration handler);
   }

   public TypoSpellChecker(Context context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      // save reference to context and read its dictionary
      context_ = context;
      contextDictionary_ = context_.readDictionary();

      // subscribe to spelling prefs changes (invalidateAll on changes)
      uiPrefs_.realTimeSpellChecking().addValueChangeHandler(prefChangedHandler_);
      uiPrefs_.ignoreWordsInUppercase().addValueChangeHandler(prefChangedHandler_);
      uiPrefs_.ignoreWordsWithNumbers().addValueChangeHandler(prefChangedHandler_);
      uiPrefs_.spellingDictionaryLanguage().addValueChangeHandler(languageChangedHandler_);

      // subscribe to user dictionary changes
      context_.releaseOnDismiss(userDictionary_.addListChangedHandler((ListChangedEvent event) ->
         {
            // detect whether this is the first delivery of the list
            // or if it is an update
            boolean isUpdate = userDictionaryWords_ != null;

            userDictionaryWords_ = event.getList();

            updateIgnoredWordsIndex();

            if (isUpdate)
               context_.invalidateMisspelledWords();
         }
      ));
   }

   @Inject
   void initialize(SpellingService spellingService, WorkbenchListManager workbenchListManager, UIPrefs uiPrefs)
   {
      userDictionary_ = workbenchListManager.getUserDictionaryList();
      uiPrefs_ = uiPrefs;

      loadDictionary();
   }

   // Check the spelling of a single word, directly returning an
   // array of suggestions for corrections. The array is empty if the
   // word is deemed correct by the dictionary
   public boolean checkSpelling(String word)
   {
      return typoNative_.check(word);
   }

   public void checkSpelling(List<String> words, final ServerRequestCallback<SpellCheckerResult> callback)
   {
      // allocate results
      final SpellCheckerResult spellCheckerResult = new SpellCheckerResult();

      // if not checking any words, return
      if (words.isEmpty())
      {
         callback.onResponseReceived(spellCheckerResult);
         return;
      }

      for (String word : words)
      {
         if (isWordIgnored(word))
         {
            spellCheckerResult.getCorrect().add(word);
         }
         else
         {
            if (typoNative_.check(word))
            {
               spellCheckerResult.getCorrect().add(word);
            }
            else
            {
               spellCheckerResult.getIncorrect().add(word);
            }
         }
      }
      callback.onResponseReceived(spellCheckerResult);
   }

   public void addToUserDictionary(final String word)
   {
      userDictionary_.append(word);
   }

   public void addIgnoredWord(String word)
   {
      contextDictionary_.add(word);
      context_.writeDictionary(contextDictionary_);

      updateIgnoredWordsIndex();

      context_.invalidateMisspelledWords();
   }

   public String[] suggestionList(String word)
   {
      if (typoNative_ == null)
      {
         return new String[0];
      }

      return typoNative_.suggest(word);
   }
   private boolean isWordIgnored(String word)
   {
      return (allIgnoredWords_.contains(word) ||
              ignoreUppercaseWord(word) ||
              ignoreWordWithNumbers(word));
   }

   private boolean ignoreUppercaseWord(String word)
   {
      if (!uiPrefs_.ignoreWordsInUppercase().getValue())
         return false;

      for (char c: word.toCharArray())
      {
         if(!Character.isUpperCase(c))
            return false;
      }
      return true;
   }

   private boolean ignoreWordWithNumbers(String word)
   {
      if (!uiPrefs_.ignoreWordsWithNumbers().getValue())
         return false;

      for (char c: word.toCharArray())
      {
         if(Character.isDigit(c))
            return true;
      }
      return false;
   }

   private void updateIgnoredWordsIndex()
   {
      allIgnoredWords_.clear();
      allIgnoredWords_.addAll(userDictionaryWords_);
      allIgnoredWords_.addAll(contextDictionary_);
   }

   private void loadDictionary()
   {
      ExternalJavaScriptLoader.Callback loadTypoCallback = () -> {
         typoNative_ = new TypoNative(
            uiPrefs_.spellingDictionaryLanguage().getValue(),
            null,
            null,
            null
         );
      };
      new ExternalJavaScriptLoader(TypoResources.INSTANCE.typojs().getSafeUri().asString()).addCallback(loadTypoCallback);
   }

   private ValueChangeHandler<Boolean> prefChangedHandler_ = new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event)
      {
         context_.invalidateAllWords();
      }
   };

   private ValueChangeHandler<String> languageChangedHandler_ = new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event)
      {
         loadDictionary();
      }
   };

   private final Context context_;

   private TypoNative typoNative_;
   private WorkbenchList userDictionary_;
   private ArrayList<String> userDictionaryWords_;
   private ArrayList<String> contextDictionary_;
   private final HashSet<String> allIgnoredWords_ = new HashSet<>();

   private UIPrefs uiPrefs_;
}


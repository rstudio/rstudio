/*
 * SpellChecker.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.spelling;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.spelling.model.SpellCheckerResult;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchList;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingDoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class RealtimeSpellChecker
{
   public interface Context
   {
      ArrayList<String> readDictionary();
      void writeDictionary(ArrayList<String> words);

      void invalidateAllWords();
      void invalidateWord(String word, boolean userDictionary);

      void releaseOnDismiss(HandlerRegistration handler);
   }

   interface Resources extends ClientBundle
   {
      @Source("./domain_specific_words.csv")
      TextResource domainSpecificWords();
   }

   public RealtimeSpellChecker(Context context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      // save reference to context and read its dictionary
      context_ = context;
      contextDictionary_ = context_.readDictionary();

      // subscribe to spelling prefs changes (invalidateAll on changes)
      ValueChangeHandler<Boolean> prefChangedHandler = (event) -> context_.invalidateAllWords();
      ValueChangeHandler<Boolean> realtimeChangedHandler = (event) -> {};
      ValueChangeHandler<String> dictChangedHandler = (event) -> {};
      userPrefs_.ignoreUppercaseWords().addValueChangeHandler(prefChangedHandler);
      userPrefs_.ignoreWordsWithNumbers().addValueChangeHandler(prefChangedHandler);
      userPrefs_.spellingDictionaryLanguage().addValueChangeHandler(dictChangedHandler);
      userPrefs_.realTimeSpellchecking().addValueChangeHandler(realtimeChangedHandler);

      // subscribe to user dictionary changes
      context_.releaseOnDismiss(userDictionary_.addListChangedHandler((ListChangedEvent event) ->
         {
            // detect whether this is the first delivery of the list
            // or if it is an update
            userDictionaryWords_ = event.getList();
            updateIgnoredWordsIndex();
         }
      ));
   }

   @Inject
   void initialize(SpellingService spellingService, WorkbenchListManager workbenchListManager, UserPrefs uiPrefs)
   {
      spellingService_ = spellingService;
      userDictionary_ = workbenchListManager.getUserDictionaryList();
      userPrefs_ = uiPrefs;

      if (domainSpecificWords_.isEmpty())
      {
         String[] words = RES.domainSpecificWords().getText().split("[\\r\\n]+");
         for (String w : words)
         {
            if (w.length() > 0)
               domainSpecificWords_.add(w);
         }
      }
   }
   
   public boolean realtimeSpellcheckEnabled()
   {
      return userPrefs_.realTimeSpellchecking().getValue();
   }

   public void addToUserDictionary(final String word)
   {
      // append to user dictionary (this is async so the in-memory list of
      // ignored words won't update immediately)
      userDictionary_.append(word);
      
      // add the words to the ignore list (necessary for contexts that 
      // rely on the word being on the ignore list for correct handling)
      // (note that allIgnoredWords_ will soon be overwritten after the
      // userDictionary list changed handler is invoked)
      allIgnoredWords_.add(word);
      
      // invalidate the context
      context_.invalidateWord(word, true);
   }
   
   public boolean isIgnoredWord(String word)
   {
      return contextDictionary_.contains(word);
   }

   public void addIgnoredWord(String word)
   {
      contextDictionary_.add(word);
      writeContextDictionary(word);
   }
   
   public void removeIgnoredWord(String word)
   {
      contextDictionary_.remove(word);
      writeContextDictionary(word);
   }
   
   private void writeContextDictionary(String affectedWord)
   {
      context_.writeDictionary(contextDictionary_);
      updateIgnoredWordsIndex();
      context_.invalidateWord(affectedWord, false);
   }

   public SpellCheckerResult getCachedWords(ArrayList<String> words)
   {
      SpellCheckerResult result = new SpellCheckerResult();
      for (String word : words)
      {
         if (isWordIgnored(word) || correctWords.contains(word))
         {
            result.getCorrect().add(word);
         }
         else if (incorrectWords.containsKey(word))
         {
            result.getIncorrect().add(word);
         }
      }

      return result;
   }

   public void checkWords(ArrayList<String> words,
                          ServerRequestCallback<SpellCheckerResult> callback)
   {
      SpellCheckerResult knownWords = getCachedWords(words);

      // we've already cached all of the words, don't hit the server
      if (knownWords.getIncorrect().size() + knownWords.getCorrect().size() == words.size())
      {
         callback.onResponseReceived(knownWords);
      }
      else
      {
         spellingService_.checkSpelling(words, new ServerRequestCallback<SpellCheckerResult>()
         {
            @Override
            public void onResponseReceived(SpellCheckerResult response)
            {
               // cache responses so we don't have to hit the server for these words again in the session
               correctWords.addAll(response.getCorrect());
               for (String wrongWord : response.getIncorrect())
               {
                  incorrectWords.put(wrongWord, null);
               }

               response.getCorrect().addAll(knownWords.getCorrect());
               response.getIncorrect().addAll(knownWords.getIncorrect());
               callback.onResponseReceived(response);
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
      }
   }

   public void suggestionList(String word,
                              ServerRequestCallback<JsArrayString> callback)
   {
      if (incorrectWords.containsKey(word) && incorrectWords.get(word) != null)
         callback.onResponseReceived(incorrectWords.get(word));
      else
         spellingService_.suggestionList(word, new ServerRequestCallback<JsArrayString>()
         {
            @Override
            public void onResponseReceived(JsArrayString response)
            {
               if (response != null)
                  incorrectWords.put(word, response);
               else
                  incorrectWords.put(word, JavaScriptObject.createArray().cast());

               callback.onResponseReceived(incorrectWords.get(word));
            }

            @Override
            public void onError(ServerError error)
            {

            }
         });
   }

   private boolean isWordIgnored(String word)
   {
      return (domainSpecificWords_.contains(word.toLowerCase()) ||
              allIgnoredWords_.contains(word) ||
              ignoreUppercaseWord(word) ||
              ignoreWordWithNumbers(word));
   }

   private boolean ignoreUppercaseWord(String word)
   {
      if (!userPrefs_.ignoreUppercaseWords().getValue())
         return false;

      for (char c: word.toCharArray())
      {
         if (!Character.isUpperCase(c))
            return false;
      }
      return true;
   }

   private boolean ignoreWordWithNumbers(String word)
   {
      if (!userPrefs_.ignoreWordsWithNumbers().getValue())
         return false;

      for (char c: word.toCharArray())
      {
         if (Character.isDigit(c))
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

   public boolean shouldCheckSpelling(SpellingDoc spellingDoc, SpellingDoc.WordRange wordRange)
   {
      String word = spellingDoc.getText(wordRange);
      if (!shouldCheckWord(word))
         return false;
        
      // source-specific knowledge of whether to check
      return spellingDoc.shouldCheck(wordRange);
   }
   
   public boolean shouldCheckWord(String word)
   {
      // Don't worry about pathologically long words
      if (word.length() > 250)
         return false;

      if (isWordIgnored(word))
         return false;
      
      return true;
   }

   private final Context context_;
   private static final Resources RES = GWT.create(Resources.class);

   private WorkbenchList userDictionary_;
   private ArrayList<String> userDictionaryWords_;
   private ArrayList<String> contextDictionary_;
   private final HashSet<String> allIgnoredWords_ = new HashSet<>();
   private final HashSet<String> domainSpecificWords_ = new HashSet<>();

   private final HashSet<String> correctWords = new HashSet<>();
   private final HashMap<String, JsArrayString> incorrectWords = new HashMap<>();

   private SpellingService spellingService_;
   private UserPrefs userPrefs_;
}

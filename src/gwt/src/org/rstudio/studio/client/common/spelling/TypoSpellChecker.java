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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.inject.Inject;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.jsonrpc.RequestLog;
import org.rstudio.core.client.jsonrpc.RequestLogEntry;
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
      ValueChangeHandler<Boolean> prefChangedHandler = (event) -> context_.invalidateAllWords();
      ValueChangeHandler<String> dictChangedHandler = (event) -> loadDictionary();
      uiPrefs_.realTimeSpellChecking().addValueChangeHandler(prefChangedHandler);
      uiPrefs_.ignoreWordsInUppercase().addValueChangeHandler(prefChangedHandler);
      uiPrefs_.ignoreWordsWithNumbers().addValueChangeHandler(prefChangedHandler);
      uiPrefs_.spellingDictionaryLanguage().addValueChangeHandler(dictChangedHandler);

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

      if (!spellingWorkerInitialized_)
      {
         spellingWorkerInitialized_ = true;
         ExternalJavaScriptLoader.Callback loadSpellingWorker = () -> {
            spellingPrefetcherNative_ = new SpellingPrefetcherNative(TypoJsEscaped.typoJsCode);
         };
         new ExternalJavaScriptLoader(
            SpellingPrefetcherResources.INSTANCE.spellingprefetcherjs().getSafeUri().asString()
         ).addCallback(loadSpellingWorker);
      }

      loadDictionary();
   }

   // Check the spelling of a single word, directly returning an
   // array of suggestions for corrections. The array is empty if the
   // word is deemed correct by the dictionary
   public boolean checkSpelling(String word)
   {
      return allIgnoredWords_.contains(word) || typoNative_.check(word);
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
      String dictLanguage = uiPrefs_.spellingDictionaryLanguage().getValue();

      // don't load the same dictionary again
      if (typoLoaded_ && loadedDict_ == dictLanguage)
         return;

      String path = GWT.getHostPageBaseURL() + "dictionaries/" + dictLanguage + "/" + dictLanguage;

      RequestLogEntry affLogEntry = RequestLog.log(dictLanguage + "_aff_request", "");
      RequestLogEntry dicLogEntry = RequestLog.log(dictLanguage + "_dic_request", "");

      typoLoaded_ = false;
      try
      {
         new RequestBuilder(RequestBuilder.GET, path + ".aff").sendRequest("", new RequestCallback() {
            @Override
            public void onResponseReceived(Request affReq, Response affResp) {
               try
               {
                  new RequestBuilder(RequestBuilder.GET, path + ".dic").sendRequest("", new RequestCallback() {
                     @Override
                     public void onResponseReceived(Request dicReq, Response dicResp) {
                        ExternalJavaScriptLoader.Callback loadTypoCallback = () -> {
                           typoNative_ = new TypoNative(dictLanguage, affResp.getText(), dicResp.getText(), null);
                           loadedDict_ = dictLanguage;
                           typoLoaded_ = true;
                        };
                        new ExternalJavaScriptLoader(TypoResources.INSTANCE.typojs().getSafeUri().asString()).addCallback(loadTypoCallback);
                     }

                     @Override
                     public void onError(Request res, Throwable throwable) {
                        dicLogEntry.logResponse(RequestLogEntry.ResponseType.Error, throwable.getLocalizedMessage());
                     }
                  });
               }
               catch (RequestException e)
               {
                  dicLogEntry.logResponse(RequestLogEntry.ResponseType.Unknown, e.getLocalizedMessage());
               }
            }

            @Override
            public void onError(Request res, Throwable throwable) {
               affLogEntry.logResponse(RequestLogEntry.ResponseType.Error, throwable.getLocalizedMessage());
            }
         });
      }
      catch (RequestException e)
      {
         affLogEntry.logResponse(RequestLogEntry.ResponseType.Unknown, e.getLocalizedMessage());
      }
   }

   public void prefetchWords(ArrayList<String> words)
   {
      if (spellingWorkerInitialized_)
      {
         spellingPrefetcherNative_.prefetch(String.join(",", words), typoNative_);
      }
   }

   public static boolean isLoaded() { return typoLoaded_; }

   private final Context context_;

   private static SpellingPrefetcherNative spellingPrefetcherNative_;
   private static boolean spellingWorkerInitialized_ = false;

   private static String loadedDict_;
   private static boolean typoLoaded_ = false;
   private static TypoNative typoNative_;

   private WorkbenchList userDictionary_;
   private ArrayList<String> userDictionaryWords_;
   private ArrayList<String> contextDictionary_;
   private final HashSet<String> allIgnoredWords_ = new HashSet<>();

   private UIPrefs uiPrefs_;
}


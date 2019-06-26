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
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.Mutable;
import org.rstudio.core.client.StringUtil;
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
   private class TypoDictionaryRequest
   {
      public TypoDictionaryRequest(String language)
      {
         language_ = language;
      }
      
      public void send()
      {
         String path = GWT.getHostPageBaseURL() + "dictionaries/" + language_ + "/" + language_;

         final Mutable<String> aff = new Mutable<String>();
         final Mutable<String> dic = new Mutable<String>();
         final Command onReady = () -> {

            if (cancelled_ || aff.get() == null || dic.get() == null)
               return;

            typoLoader_.addCallback(() -> {
               typoNative_ = new TypoNative(language_, aff.get(), dic.get(), null);
               loadedDict_ = language_;
               typoLoaded_ = true;
               
               aff.clear();
               dic.clear();
               alive_ = false;
            });
            
         };

         alive_ = true;
         makeRequest(path, "aff", (String response) -> {
            aff.set(response);
            onReady.execute();
         });

         makeRequest(path, "dic", (String response) -> {
            dic.set(response);
            onReady.execute();
         });

      }
      
      private void makeRequest(String path,
                               String suffix,
                               final CommandWithArg<String> callback)
      {
         String logName = language_ + "_" + suffix + "_request";
         final RequestLogEntry logEntry = RequestLog.log(logName, "");

         try
         {
            RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, path + "." + suffix);
            builder.sendRequest("", new RequestCallback()
            {
               @Override
               public void onResponseReceived(Request request, Response response)
               {
                  logEntry.logResponse(RequestLogEntry.ResponseType.Normal, response.getText());
                  callback.execute(response.getText());
               }

               @Override
               public void onError(Request request, Throwable throwable)
               {
                  logEntry.logResponse(RequestLogEntry.ResponseType.Error, throwable.getLocalizedMessage());
                  alive_ = false;
               }
            });
         }
         catch (RequestException e)
         {
            logEntry.logResponse(RequestLogEntry.ResponseType.Unknown, e.getLocalizedMessage());
            alive_ = false;
         }
      }
      
      
      public String getLanguage()
      {
         return language_;
      }
      
      public void cancel()
      {
         cancelled_ = true;
      }
      
      public boolean isAlive()
      {
         return alive_;
      }
      
      private boolean cancelled_;
      private boolean alive_;
      
      private final String language_;
   }
   
   public interface Context
   {
      ArrayList<String> readDictionary();
      void writeDictionary(ArrayList<String> words);

      void invalidateAllWords();
      void invalidateMisspelledWords();
      void invalidateWord(String word);

      void releaseOnDismiss(HandlerRegistration handler);
   }

   interface Resources extends ClientBundle
   {
      @Source("./typo.min.js")
      TextResource typoJsCode();
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
            userDictionaryWords_ = event.getList();
            updateIgnoredWordsIndex();
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
            spellingPrefetcherNative_ = new SpellingPrefetcherNative(RES.typoJsCode().getText());
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
      context_.invalidateWord(word);
   }

   public void addIgnoredWord(String word)
   {
      contextDictionary_.add(word);
      context_.writeDictionary(contextDictionary_);
      updateIgnoredWordsIndex();
      context_.invalidateWord(word);
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
      // don't load the same dictionary again
      final String language = uiPrefs_.spellingDictionaryLanguage().getValue();
      if (typoLoaded_ && loadedDict_ == language)
         return;
      
      // check for an active request
      if (activeRequest_ != null && activeRequest_.isAlive())
      {
         // if we're already requesting this language's dictionary, bail
         if (StringUtil.equals(activeRequest_.getLanguage(), language))
            return;
         
         // otherwise, cancel that request and start a new one
         activeRequest_.cancel();
         activeRequest_ = null;
      }

      // create and send
      activeRequest_ = new TypoDictionaryRequest(language);
      activeRequest_.send();
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
   private static final Resources RES = GWT.create(Resources.class);

   private static SpellingPrefetcherNative spellingPrefetcherNative_;
   private static boolean spellingWorkerInitialized_ = false;

   private static String loadedDict_;
   private static boolean typoLoaded_ = false;
   private static TypoNative typoNative_;
   private static TypoDictionaryRequest activeRequest_;

   private WorkbenchList userDictionary_;
   private ArrayList<String> userDictionaryWords_;
   private ArrayList<String> contextDictionary_;
   private final HashSet<String> allIgnoredWords_ = new HashSet<>();
   private final ExternalJavaScriptLoader typoLoader_ =
         new ExternalJavaScriptLoader(TypoResources.INSTANCE.typojs().getSafeUri().asString());

   private UIPrefs uiPrefs_;
}

